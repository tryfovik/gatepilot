<template>
  <div class="page-stack">
    <section class="content-panel">
      <div class="panel-header">
        <div>
          <h2>配置快照</h2>
          <p>查看已发布配置快照，按版本、分片对比路由、上游和策略变更。</p>
        </div>
        <button class="ghost-button" type="button" @click="load">
          <RefreshCw :size="16" />
          刷新
        </button>
      </div>

      <form class="filterbar diff-form" @submit.prevent="compare">
        <select v-model="namespace" class="select-input">
          <option value="default">default</option>
          <option value="">全部命名空间</option>
        </select>
        <input v-model="baseVersion" class="search-input" placeholder="基线版本" />
        <input v-model="targetVersion" class="search-input" placeholder="目标版本" />
        <input v-model="configShard" class="search-input" placeholder="配置分片" />
        <button class="primary-button" type="submit">
          <GitCompare :size="16" />
          对比
        </button>
      </form>

      <div v-if="loading" class="state-box">正在加载...</div>
      <div v-else-if="error" class="state-box state-box--error">{{ error }}</div>
      <table v-else class="resource-table">
        <thead>
          <tr>
            <th>版本</th>
            <th>命名空间</th>
            <th>项目</th>
            <th>分片</th>
            <th>序号</th>
            <th>哈希</th>
            <th>采集时间</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in items" :key="item.metadata?.uid || item.metadata?.name">
            <td>
              <div class="resource-name">{{ item.spec?.version || item.metadata?.name || '-' }}</div>
              <div class="resource-subtitle">{{ item.spec?.releaseId || '未关联发布请求' }}</div>
            </td>
            <td>{{ item.metadata?.namespace || '-' }}</td>
            <td>{{ item.spec?.projectRef?.name || '-' }}</td>
            <td>{{ item.spec?.configShard || '-' }}</td>
            <td>{{ item.spec?.sequence ?? '-' }}</td>
            <td class="mono-cell">{{ shortHash(item.spec?.configHash) }}</td>
            <td>{{ formatTime(item.spec?.capturedAt) }}</td>
          </tr>
        </tbody>
      </table>
    </section>

    <section class="content-panel">
      <div class="panel-header">
        <div>
          <h2>版本 Diff</h2>
          <p>对比结果按资源类型和变更类型聚合，便于发布前后排查差异。</p>
        </div>
        <StatusBadge :label="diffStatusLabel" :tone="diffStatusTone" />
      </div>

      <div v-if="diffLoading" class="state-box">正在对比...</div>
      <div v-else-if="diffError" class="state-box state-box--error">{{ diffError }}</div>
      <div v-else-if="!diff" class="state-box">暂无对比结果</div>
      <div v-else class="page-stack">
        <div class="metric-strip diff-strip">
          <div class="metric">
            <div class="metric-label">路由变更</div>
            <div class="metric-value">{{ diff.addedRoutes + diff.removedRoutes + diff.changedRoutes }}</div>
            <div class="metric-note">新增 {{ diff.addedRoutes }} / 删除 {{ diff.removedRoutes }} / 修改 {{ diff.changedRoutes }}</div>
          </div>
          <div class="metric">
            <div class="metric-label">上游变更</div>
            <div class="metric-value">{{ diff.addedUpstreams + diff.removedUpstreams + diff.changedUpstreams }}</div>
            <div class="metric-note">新增 {{ diff.addedUpstreams }} / 删除 {{ diff.removedUpstreams }} / 修改 {{ diff.changedUpstreams }}</div>
          </div>
          <div class="metric">
            <div class="metric-label">策略变更</div>
            <div class="metric-value">{{ diff.addedPolicies + diff.removedPolicies + diff.changedPolicies }}</div>
            <div class="metric-note">新增 {{ diff.addedPolicies }} / 删除 {{ diff.removedPolicies }} / 修改 {{ diff.changedPolicies }}</div>
          </div>
        </div>

        <table class="resource-table">
          <thead>
            <tr>
              <th>资源类型</th>
              <th>名称</th>
              <th>变更</th>
              <th>基线哈希</th>
              <th>目标哈希</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in diff.items" :key="`${item.resourceType}-${item.name}-${item.changeType}`">
              <td>{{ resourceTypeLabel(item.resourceType) }}</td>
              <td>{{ item.name }}</td>
              <td>
                <StatusBadge :label="changeTypeLabel(item.changeType)" :tone="changeTone(item.changeType)" />
              </td>
              <td class="mono-cell">{{ shortHash(item.baseHash) }}</td>
              <td class="mono-cell">{{ shortHash(item.targetHash) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { GitCompare, RefreshCw } from 'lucide-vue-next';
import StatusBadge from '../components/StatusBadge.vue';
import { ConfigDiffResponse, diffConfigSnapshots, listResources } from '../api/client';

interface ConfigSnapshot {
  metadata?: {
    uid?: string;
    name?: string;
    namespace?: string;
  };
  spec?: {
    projectRef?: {
      name?: string;
    };
    releaseId?: string;
    version?: string;
    configHash?: string;
    configShard?: string;
    sequence?: number;
    capturedAt?: string;
  };
}

const namespace = ref('default');
const baseVersion = ref('');
const targetVersion = ref('');
const configShard = ref('');
const loading = ref(false);
const error = ref('');
const diffLoading = ref(false);
const diffError = ref('');
const items = ref<ConfigSnapshot[]>([]);
const diff = ref<ConfigDiffResponse | null>(null);

const diffStatusLabel = computed(() => {
  if (!diff.value) {
    return '待对比';
  }
  return diff.value.changed ? '存在变更' : '无变更';
});

const diffStatusTone = computed<'success' | 'warning' | 'danger' | 'info' | 'neutral'>(() => {
  if (!diff.value) {
    return 'neutral';
  }
  return diff.value.changed ? 'warning' : 'success';
});

async function load() {
  loading.value = true;
  error.value = '';
  try {
    const page = await listResources<ConfigSnapshot>('config-snapshots', namespace.value);
    items.value = page.items;
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载失败';
  } finally {
    loading.value = false;
  }
}

async function compare() {
  diffError.value = '';
  if (!namespace.value || !baseVersion.value || !targetVersion.value) {
    diffError.value = '请选择命名空间，并填写基线版本和目标版本';
    return;
  }
  diffLoading.value = true;
  try {
    diff.value = await diffConfigSnapshots(
      namespace.value,
      baseVersion.value,
      targetVersion.value,
      configShard.value || undefined
    );
  } catch (err) {
    diff.value = null;
    diffError.value = err instanceof Error ? err.message : '对比失败';
  } finally {
    diffLoading.value = false;
  }
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

function resourceTypeLabel(value: string) {
  const labels: Record<string, string> = {
    route: '路由',
    upstream: '上游',
    policy: '策略'
  };
  return labels[value] || value;
}

function changeTypeLabel(value: string) {
  const labels: Record<string, string> = {
    ADDED: '新增',
    REMOVED: '删除',
    CHANGED: '修改'
  };
  return labels[value] || value;
}

function changeTone(value: string): 'success' | 'warning' | 'danger' | 'info' | 'neutral' {
  if (value === 'ADDED') return 'success';
  if (value === 'REMOVED') return 'danger';
  if (value === 'CHANGED') return 'warning';
  return 'neutral';
}

onMounted(load);
watch(namespace, load);
</script>
