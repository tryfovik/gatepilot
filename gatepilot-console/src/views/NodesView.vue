<template>
  <section class="content-panel">
    <div class="panel-header">
      <div>
        <h2>节点</h2>
        <p>查看 agent / proxy 副本、心跳、last-good、健康摘要和上游健康。</p>
      </div>
      <button class="ghost-button" type="button" @click="load">
        <RefreshCw :size="16" />
        刷新
      </button>
    </div>

    <div class="filterbar">
      <input v-model="keyword" class="search-input" placeholder="搜索节点、版本、分片或隔离组" />
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
          <th>节点</th>
          <th>状态</th>
          <th>健康</th>
          <th>配置</th>
          <th>负载</th>
          <th>上游</th>
          <th>心跳</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="item in filteredItems" :key="item.metadata?.uid || item.metadata?.name">
          <td>
            <div class="resource-name">{{ item.spec?.nodeId || item.metadata?.name || '-' }}</div>
            <div class="resource-subtitle">{{ nodeSubtitle(item) }}</div>
          </td>
          <td>
            <StatusBadge :label="nodePhaseLabel(item.status?.nodePhase)" :tone="nodePhaseTone(item.status?.nodePhase)" />
          </td>
          <td>
            <div class="compact-stack">
              <StatusBadge :label="healthLabel(item)" :tone="healthTone(item)" />
              <span class="resource-subtitle">{{ item.status?.health?.message || '-' }}</span>
            </div>
          </td>
          <td>
            <div class="compact-stack">
              <span>{{ item.status?.currentConfigVersion || '-' }}</span>
              <span class="resource-subtitle">last-good {{ item.status?.lastGoodConfigVersion || '-' }}</span>
            </div>
          </td>
          <td>
            <div class="compact-stack">
              <span>{{ formatNumber(item.status?.metrics?.requestsPerSecond) }} RPS</span>
              <span class="resource-subtitle">
                错误率 {{ formatPercent(item.status?.metrics?.errorRate) }} / P95 {{ formatNumber(item.status?.metrics?.p95LatencyMillis) }}ms
              </span>
            </div>
          </td>
          <td>
            <div class="compact-stack">
              <span>{{ upstreamSummary(item) }}</span>
              <span class="resource-subtitle">加载上游 {{ item.status?.loadedUpstreamCount ?? '-' }}</span>
            </div>
          </td>
          <td>{{ formatTime(item.status?.lastHeartbeatAt) }}</td>
        </tr>
      </tbody>
    </table>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { RefreshCw } from 'lucide-vue-next';
import StatusBadge from '../components/StatusBadge.vue';
import { listResources } from '../api/client';

interface GatewayNode {
  metadata?: {
    uid?: string;
    name?: string;
    namespace?: string;
  };
  spec?: {
    nodeId?: string;
    role?: string;
    zone?: string;
    isolationGroup?: string;
    configShards?: string[];
  };
  status?: {
    nodePhase?: string;
    currentConfigVersion?: string;
    lastGoodConfigVersion?: string;
    lastHeartbeatAt?: string;
    loadedUpstreamCount?: number;
    health?: {
      agentHealthy?: boolean;
      proxyHealthy?: boolean;
      controlPlaneConnected?: boolean;
      message?: string;
    };
    metrics?: {
      requestsPerSecond?: number;
      errorRate?: number;
      p95LatencyMillis?: number;
      activeConnections?: number;
    };
    upstreamHealth?: Array<{
      upstreamName?: string;
      healthyEndpointCount?: number;
      unhealthyEndpointCount?: number;
    }>;
  };
}

const namespace = ref('default');
const keyword = ref('');
const loading = ref(false);
const error = ref('');
const items = ref<GatewayNode[]>([]);

const filteredItems = computed(() => {
  const value = keyword.value.trim().toLowerCase();
  if (!value) {
    return items.value;
  }
  return items.value.filter((item) => {
    const haystack = [
      item.metadata?.name,
      item.metadata?.namespace,
      item.spec?.nodeId,
      item.spec?.role,
      item.spec?.zone,
      item.spec?.isolationGroup,
      item.spec?.configShards?.join(' '),
      item.status?.currentConfigVersion,
      item.status?.lastGoodConfigVersion
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
    const page = await listResources<GatewayNode>('nodes', namespace.value);
    items.value = page.items;
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载失败';
  } finally {
    loading.value = false;
  }
}

function nodeSubtitle(item: GatewayNode) {
  const parts = [
    item.spec?.role,
    item.spec?.zone,
    item.spec?.isolationGroup,
    item.spec?.configShards?.join(',')
  ].filter(Boolean);
  return parts.length ? parts.join(' / ') : item.metadata?.uid || '未生成 UID';
}

function nodePhaseLabel(value?: string) {
  const labels: Record<string, string> = {
    REGISTERED: '已注册',
    READY: '就绪',
    NOT_READY: '未就绪',
    DRAINING: '摘流中',
    OFFLINE: '离线'
  };
  return labels[value || ''] || '未知';
}

function nodePhaseTone(value?: string): 'success' | 'warning' | 'danger' | 'info' | 'neutral' {
  if (value === 'READY') return 'success';
  if (value === 'REGISTERED') return 'info';
  if (value === 'DRAINING') return 'warning';
  if (value === 'NOT_READY' || value === 'OFFLINE') return 'danger';
  return 'neutral';
}

function healthLabel(item: GatewayNode) {
  const health = item.status?.health;
  if (!health) {
    return '未知';
  }
  if (health.agentHealthy && health.proxyHealthy && health.controlPlaneConnected) {
    return '健康';
  }
  return '异常';
}

function healthTone(item: GatewayNode): 'success' | 'warning' | 'danger' | 'info' | 'neutral' {
  const health = item.status?.health;
  if (!health) {
    return 'neutral';
  }
  if (health.agentHealthy && health.proxyHealthy && health.controlPlaneConnected) {
    return 'success';
  }
  if (health.agentHealthy || health.proxyHealthy || health.controlPlaneConnected) {
    return 'warning';
  }
  return 'danger';
}

function upstreamSummary(item: GatewayNode) {
  const upstreams = item.status?.upstreamHealth || [];
  if (upstreams.length === 0) {
    return '暂无上报';
  }
  const healthy = upstreams.reduce((total, upstream) => total + (upstream.healthyEndpointCount || 0), 0);
  const unhealthy = upstreams.reduce((total, upstream) => total + (upstream.unhealthyEndpointCount || 0), 0);
  return `健康 ${healthy} / 异常 ${unhealthy}`;
}

function formatNumber(value?: number) {
  if (value === undefined || value === null) {
    return '-';
  }
  return new Intl.NumberFormat('zh-CN', { maximumFractionDigits: 2 }).format(value);
}

function formatPercent(value?: number) {
  if (value === undefined || value === null) {
    return '-';
  }
  return new Intl.NumberFormat('zh-CN', { style: 'percent', maximumFractionDigits: 2 }).format(value);
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
watch(() => [namespace.value], load);
</script>
