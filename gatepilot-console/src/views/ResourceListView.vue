<template>
  <section class="content-panel">
    <div class="panel-header">
      <div>
        <h2>{{ title }}</h2>
        <p>{{ description }}</p>
      </div>
      <button class="ghost-button" type="button" @click="load">
        <RefreshCw :size="16" />
        刷新
      </button>
    </div>

    <div class="filterbar">
      <input v-model="keyword" class="search-input" placeholder="搜索名称、命名空间或版本" />
      <select v-model="namespace" class="select-input">
        <option value="default">default</option>
        <option value="">全部命名空间</option>
      </select>
    </div>

    <div v-if="loading" class="state-box">正在加载...</div>
    <div v-else-if="error" class="state-box state-box--error">{{ error }}</div>
    <div v-else-if="filteredItems.length === 0" class="state-box">暂无数据</div>

    <table v-else class="resource-table">
      <thead>
        <tr>
          <th>名称</th>
          <th>命名空间</th>
          <th>状态</th>
          <th>摘要</th>
          <th>版本</th>
          <th>更新时间</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="item in filteredItems" :key="item.metadata?.uid || item.metadata?.name">
          <td>
            <div class="resource-name">{{ item.metadata?.name || '-' }}</div>
            <div class="resource-subtitle">{{ item.metadata?.uid || '未生成 UID' }}</div>
          </td>
          <td>{{ item.metadata?.namespace || '-' }}</td>
          <td>
            <StatusBadge :label="phaseLabel(item.status?.phase)" :tone="phaseTone(item.status?.phase)" />
          </td>
          <td>{{ summaryText(item) }}</td>
          <td>{{ item.spec?.version || item.status?.currentPublishedVersion || '-' }}</td>
          <td>{{ formatTime(item.metadata?.updatedAt) }}</td>
          <td>
            <button class="table-action" type="button" @click="selectedItem = item">查看</button>
          </td>
        </tr>
      </tbody>
    </table>

    <ResourceDetailDrawer
      :open="Boolean(selectedItem)"
      :title="selectedItem?.metadata?.name || '资源详情'"
      :subtitle="selectedItem?.metadata?.namespace || '-'"
      :payload="selectedItem"
      @close="selectedItem = null"
    />
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { RefreshCw } from 'lucide-vue-next';
import ResourceDetailDrawer from '../components/ResourceDetailDrawer.vue';
import StatusBadge from '../components/StatusBadge.vue';
import { listResources } from '../api/client';

interface ResourceItem {
  metadata?: {
    uid?: string;
    name?: string;
    namespace?: string;
    updatedAt?: string;
  };
  spec?: {
    version?: string;
    strategy?: string;
    type?: string;
    loadBalance?: string;
    hosts?: string[];
    endpoints?: unknown[];
    rateLimit?: {
      enabled?: boolean;
      requestsPerSecond?: number;
    };
    retry?: {
      enabled?: boolean;
      maxAttempts?: number;
    };
  };
  status?: {
    phase?: string;
    currentPublishedVersion?: string;
  };
}

const props = defineProps<{
  resourceType: string;
  title: string;
  description: string;
}>();

const namespace = ref('default');
const keyword = ref('');
const loading = ref(false);
const error = ref('');
const items = ref<ResourceItem[]>([]);
const selectedItem = ref<ResourceItem | null>(null);

const filteredItems = computed(() => {
  const value = keyword.value.trim().toLowerCase();
  if (!value) {
    return items.value;
  }
  return items.value.filter((item) => {
    const haystack = [
      item.metadata?.name,
      item.metadata?.namespace,
      item.metadata?.uid,
      item.spec?.version,
      item.status?.currentPublishedVersion
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
    const page = await listResources<ResourceItem>(props.resourceType, namespace.value);
    items.value = page.items;
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载失败';
  } finally {
    loading.value = false;
  }
}

function summaryText(item: ResourceItem) {
  if (item.spec?.strategy) {
    return strategyLabel(item.spec.strategy);
  }
  if (item.spec?.type) {
    return `认证 ${item.spec.type}`;
  }
  if (item.spec?.loadBalance) {
    return `${item.spec.loadBalance} / ${item.spec.endpoints?.length ?? 0} 端点`;
  }
  if (item.spec?.hosts?.length) {
    return item.spec.hosts.join(', ');
  }
  if (item.spec?.rateLimit || item.spec?.retry) {
    const parts = [];
    if (item.spec.rateLimit?.enabled) {
      parts.push(`限流 ${item.spec.rateLimit.requestsPerSecond || '-'} RPS`);
    }
    if (item.spec.retry?.enabled) {
      parts.push(`重试 ${item.spec.retry.maxAttempts || '-'} 次`);
    }
    return parts.join(' / ') || '-';
  }
  return '-';
}

function strategyLabel(value: string) {
  const labels: Record<string, string> = {
    BLUE_GREEN: '蓝绿发布',
    CANARY: '灰度发布',
    TRAFFIC_SPLIT: '权重分流',
    SHADOW: '影子流量'
  };
  return labels[value] || value;
}

function phaseLabel(phase?: string) {
  const labels: Record<string, string> = {
    ACTIVE: '正常',
    PENDING: '等待中',
    DEGRADED: '有告警',
    FAILED: '失败',
    DELETED: '已删除'
  };
  return labels[phase || ''] || '未知';
}

function phaseTone(phase?: string): 'success' | 'warning' | 'danger' | 'info' | 'neutral' {
  if (phase === 'ACTIVE') return 'success';
  if (phase === 'PENDING') return 'info';
  if (phase === 'DEGRADED') return 'warning';
  if (phase === 'FAILED') return 'danger';
  return 'neutral';
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

onMounted(load);
watch(() => [props.resourceType, namespace.value], load);
</script>
