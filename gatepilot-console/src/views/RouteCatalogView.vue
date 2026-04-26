<template>
  <div class="page-stack">
    <section class="content-panel">
      <div class="panel-header">
        <div>
          <h2>路由目录</h2>
          <p>基于 PublishedConfig 展示当前数据面会消费的路由、上游、策略和节点应用状态。</p>
        </div>
        <button class="ghost-button" type="button" @click="load">
          <RefreshCw :size="16" />
          刷新
        </button>
      </div>

      <form class="filterbar diff-form" @submit.prevent="load">
        <input v-model="namespace" class="search-input compact-input" placeholder="命名空间" />
        <input v-model="projectName" class="search-input compact-input" placeholder="项目名称" />
        <input v-model="version" class="search-input compact-input" placeholder="发布版本" />
        <input v-model="configShard" class="search-input compact-input" placeholder="配置分片" />
        <input v-model="keyword" class="search-input compact-input" placeholder="搜索路由" />
        <button class="primary-button" type="submit">
          <Search :size="16" />
          查询
        </button>
      </form>

      <div v-if="loading" class="state-box">正在加载...</div>
      <div v-else-if="error" class="state-box state-box--error">{{ error }}</div>
      <div v-else-if="!catalog" class="state-box">暂无数据</div>
      <div v-else class="page-stack">
        <div class="metric-strip">
          <div class="metric">
            <div class="metric-label">路由</div>
            <div class="metric-value">{{ catalog.routeCount }}</div>
            <div class="metric-note">{{ catalog.version || '-' }}</div>
          </div>
          <div class="metric">
            <div class="metric-label">上游</div>
            <div class="metric-value">{{ catalog.upstreamCount }}</div>
            <div class="metric-note">{{ catalog.configShard || '默认分片' }}</div>
          </div>
          <div class="metric">
            <div class="metric-label">策略</div>
            <div class="metric-value">{{ catalog.policyCount }}</div>
            <div class="metric-note">{{ shortHash(catalog.configHash) }}</div>
          </div>
          <div class="metric">
            <div class="metric-label">目标节点</div>
            <div class="metric-value">{{ catalog.targetNodeCount }}</div>
            <div class="metric-note">{{ formatTime(catalog.generatedAt) }}</div>
          </div>
        </div>

        <table class="resource-table">
          <thead>
            <tr>
              <th>路由</th>
              <th>入口</th>
              <th>方法</th>
              <th>上游</th>
              <th>策略</th>
              <th>重写</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="route in filteredRoutes" :key="route.routeId || route.name">
              <td>
                <div class="resource-name">{{ route.name || route.routeId || '-' }}</div>
                <div class="resource-subtitle">{{ route.routeId || '未生成 routeId' }}</div>
              </td>
              <td>
                <div class="compact-stack">
                  <span>{{ route.path || '-' }}</span>
                  <span class="resource-subtitle">{{ listText(route.hosts, '任意 Host') }}</span>
                </div>
              </td>
              <td>{{ listText(route.methods, '全部') }}</td>
              <td>
                <StatusBadge
                  :label="route.upstreamName || '未配置'"
                  :tone="route.upstreamAvailable ? 'success' : 'danger'"
                />
              </td>
              <td>{{ listText(route.policyNames, '未绑定') }}</td>
              <td>
                <div class="compact-stack">
                  <span>{{ route.rewritePathPrefix || '-' }}</span>
                  <span class="resource-subtitle">strip {{ route.stripPrefix ? '是' : '否' }}</span>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <section v-if="catalog" class="content-panel">
      <div class="panel-header">
        <div>
          <h2>上游与节点</h2>
          <p>快速核对路由目标、端点数量、健康检查和节点应用结果。</p>
        </div>
      </div>

      <div class="diagnostic-grid">
        <div>
          <h3>上游</h3>
          <table class="resource-table compact-table">
            <thead>
              <tr>
                <th>名称</th>
                <th>协议</th>
                <th>端点</th>
                <th>健康检查</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="upstream in catalog.upstreams" :key="upstream.name">
                <td>{{ upstream.name || '-' }}</td>
                <td>{{ upstream.protocol || '-' }}</td>
                <td>{{ upstream.endpointCount }}</td>
                <td>
                  <StatusBadge :label="upstream.healthCheckEnabled ? '已启用' : '未启用'" :tone="upstream.healthCheckEnabled ? 'success' : 'neutral'" />
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div>
          <h3>节点应用</h3>
          <div v-if="catalog.nodeApplyResults.length === 0" class="state-box state-box--compact">暂无节点上报</div>
          <div v-else class="timeline">
            <div v-for="node in catalog.nodeApplyResults" :key="node.nodeId" class="timeline-item">
              <StatusBadge :label="applyStateLabel(node.state)" :tone="applyStateTone(node.state)" />
              <div>
                <div class="resource-name">{{ node.nodeId || '-' }}</div>
                <div class="resource-subtitle">{{ node.zone || '-' }} / {{ node.appliedVersion || '-' }}</div>
                <div v-if="node.message" class="timeline-message">{{ node.message }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { RefreshCw, Search } from 'lucide-vue-next';
import StatusBadge from '../components/StatusBadge.vue';
import { RouteCatalogResponse, getRouteCatalog } from '../api/client';

const namespace = ref('default');
const projectName = ref('');
const version = ref('');
const configShard = ref('');
const keyword = ref('');
const loading = ref(false);
const error = ref('');
const catalog = ref<RouteCatalogResponse | null>(null);

const filteredRoutes = computed(() => {
  const value = keyword.value.trim().toLowerCase();
  if (!catalog.value || !value) {
    return catalog.value?.routes || [];
  }
  return catalog.value.routes.filter((route) => {
    const haystack = [
      route.name,
      route.routeId,
      route.path,
      route.upstreamName,
      route.hosts?.join(' '),
      route.policyNames?.join(' ')
    ]
      .filter(Boolean)
      .join(' ')
      .toLowerCase();
    return haystack.includes(value);
  });
});

async function load() {
  loading.value = true;
  error.value = '';
  try {
    catalog.value = await getRouteCatalog({
      namespace: namespace.value,
      projectName: projectName.value,
      version: version.value,
      configShard: configShard.value
    });
  } catch (err) {
    catalog.value = null;
    error.value = err instanceof Error ? err.message : '加载失败';
  } finally {
    loading.value = false;
  }
}

function listText(values?: string[], emptyText = '-') {
  if (!values || values.length === 0) {
    return emptyText;
  }
  return values.join(', ');
}

function shortHash(value?: string) {
  if (!value) {
    return '-';
  }
  return value.length > 12 ? `${value.slice(0, 12)}...` : value;
}

function formatTime(value?: string) {
  if (!value) {
    return '-';
  }
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(value));
}

function applyStateLabel(value?: string) {
  const labels: Record<string, string> = {
    PENDING: '等待应用',
    STAGED: '已暂存',
    APPLIED: '已应用',
    FAILED: '失败',
    ROLLED_BACK: '已回滚'
  };
  return labels[value || ''] || '未知';
}

function applyStateTone(value?: string): 'success' | 'warning' | 'danger' | 'info' | 'neutral' {
  if (value === 'APPLIED') return 'success';
  if (value === 'PENDING' || value === 'STAGED') return 'info';
  if (value === 'ROLLED_BACK') return 'warning';
  if (value === 'FAILED') return 'danger';
  return 'neutral';
}

onMounted(load);
</script>
