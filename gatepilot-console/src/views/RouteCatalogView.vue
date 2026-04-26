<template>
  <div class="page-stack">
    <section class="content-panel">
      <div class="panel-header">
        <div>
          <h2>路由配置</h2>
          <p>声明式路由看期望状态，运行目录看数据面当前消费的 PublishedConfig。</p>
        </div>
      </div>
      <div class="tabbar">
        <button
          v-for="tab in routeTabs"
          :key="tab.key"
          class="tab-button"
          :class="{ 'tab-button--active': activeRouteTab === tab.key }"
          type="button"
          @click="activeRouteTab = tab.key"
        >
          {{ tab.label }}
        </button>
      </div>
    </section>

    <template v-if="activeRouteTab === 'runtime'">
    <section class="content-panel">
      <div class="panel-header">
        <div>
          <h2>路由目录</h2>
          <p>基于 PublishedConfig 展示当前数据面会消费的路由、上游、策略和节点应用状态。</p>
        </div>
        <button class="ghost-button" type="button" :disabled="loading" @click="load">
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
              <th>操作</th>
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
              <td>
                <button class="table-action" type="button" @click="diagnose(route)">诊断</button>
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
    </template>

    <ResourceListView
      v-else
      resource-type="routes"
      title="声明式路由"
      description="查看 GatewayRoute 期望状态，包括入口 Host、路径匹配、默认上游、绑定策略和重写规则。"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { RefreshCw, Search } from 'lucide-vue-next';
import ResourceListView from './ResourceListView.vue';
import StatusBadge from '../components/StatusBadge.vue';
import { RouteCatalogResponse, getRouteCatalog } from '../api/client';
import { applyStateLabel, applyStateTone, formatTime, listText, shortHash } from '../utils/format';
import { notifyError, notifyInfo } from '../utils/feedback';
import { getGlobalNamespace, onGlobalNamespaceChange, setGlobalNamespace } from '../utils/namespace';

const currentRoute = useRoute();
const router = useRouter();
const routeTabs = [
  { key: 'runtime', label: '运行目录' },
  { key: 'declared', label: '声明式路由' }
] as const;
const activeRouteTab = ref<(typeof routeTabs)[number]['key']>('runtime');
const namespace = ref(queryText('namespace', getGlobalNamespace('default')));
const projectName = ref(queryText('projectName'));
const version = ref(queryText('version'));
const configShard = ref(queryText('configShard'));
const keyword = ref('');
const loading = ref(false);
const error = ref('');
const catalog = ref<RouteCatalogResponse | null>(null);
let unsubscribeNamespace: (() => void) | null = null;

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
    notifyError('路由目录加载失败', error.value);
  } finally {
    loading.value = false;
  }
}

function applyRouteQuery() {
  namespace.value = queryText('namespace', namespace.value || 'default');
  projectName.value = queryText('projectName', projectName.value);
  version.value = queryText('version', version.value);
  configShard.value = queryText('configShard', configShard.value);
}

function queryText(key: string, fallback = '') {
  const value = currentRoute.query[key];
  return Array.isArray(value) ? value[0] || fallback : value || fallback;
}

function diagnose(route: RouteCatalogResponse['routes'][number]) {
  notifyInfo('已带入路由诊断参数');
  void router.push({
    path: '/operations/route-diagnostics/request-workbench',
    query: {
      namespace: namespace.value || 'default',
      projectName: catalog.value?.projectName || projectName.value || '',
      version: catalog.value?.version || version.value || '',
      configShard: catalog.value?.configShard || configShard.value || '',
      host: route.hosts?.[0] || '',
      path: route.path || '/api'
    }
  });
}

onMounted(() => {
  unsubscribeNamespace = onGlobalNamespaceChange((value) => {
    if (namespace.value !== value) {
      namespace.value = value;
    }
  });
  void load();
});
onUnmounted(() => {
  unsubscribeNamespace?.();
});
watch(() => currentRoute.query, () => {
  applyRouteQuery();
  void load();
});
watch(namespace, (value) => {
  setGlobalNamespace(value);
});
</script>
