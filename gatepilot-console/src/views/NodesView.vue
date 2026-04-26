<template>
  <section class="content-panel">
    <div class="panel-header">
      <div>
        <h2>节点</h2>
        <p>查看 agent / proxy 副本、心跳、last-good、健康摘要和上游健康。</p>
      </div>
      <button class="ghost-button" type="button" @click="refresh">
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
          <th>应用</th>
          <th>负载</th>
          <th>上游</th>
          <th>心跳</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <template v-for="item in filteredItems" :key="nodeKey(item)">
          <tr :class="{ 'resource-row--expanded': isExpanded(item) }">
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
                <span v-if="healthMessage(item)" class="resource-subtitle">{{ healthMessage(item) }}</span>
              </div>
            </td>
            <td>
              <div class="compact-stack">
                <span>当前 {{ item.status?.currentConfigVersion || '-' }}</span>
                <span class="resource-subtitle">期望 {{ item.status?.desiredConfigVersion || '-' }}</span>
                <span class="resource-subtitle">last-good {{ item.status?.lastGoodConfigVersion || '-' }}</span>
              </div>
            </td>
            <td>
              <div class="compact-stack">
                <StatusBadge :label="applyStateLabel(item.status?.applyState)" :tone="applyStateTone(item.status?.applyState)" />
                <span v-if="applyMessage(item)" class="resource-subtitle">{{ applyMessage(item) }}</span>
              </div>
            </td>
            <td>
              <div class="compact-stack">
                <span>{{ formatNumber(item.status?.metrics?.requestsPerSecond) }} RPS</span>
                <span class="resource-subtitle">
                  错误率 {{ percentText(item.status?.metrics?.errorRate) }} / P95 {{ numberText(item.status?.metrics?.p95LatencyMillis) }}ms
                </span>
                <span class="resource-subtitle">连接 {{ numberText(item.status?.metrics?.activeConnections) }}</span>
              </div>
            </td>
            <td>
              <div class="compact-stack">
                <span>{{ upstreamSummary(item) }}</span>
                <span class="resource-subtitle">加载上游 {{ item.status?.loadedUpstreamCount ?? '-' }}</span>
              </div>
            </td>
            <td>{{ heartbeatTime(item) }}</td>
            <td>
              <div class="table-actions">
                <button class="table-action" type="button" @click="toggleNodeDetail(item)">
                  {{ isExpanded(item) ? '收起' : '排查' }}
                </button>
                <button class="table-action" type="button" @click="openDetail(item)">字段</button>
              </div>
            </td>
          </tr>
          <tr v-if="isExpanded(item)" class="node-drilldown-row">
            <td colspan="9">
              <div class="node-drilldown">
                <section class="node-drilldown-card">
                  <h3>副本信息</h3>
                  <div class="key-value-grid">
                    <span>节点 ID</span>
                    <strong>{{ item.spec?.nodeId || item.metadata?.name || '-' }}</strong>
                    <span>角色</span>
                    <strong>{{ item.spec?.role || '-' }}</strong>
                    <span>可用区</span>
                    <strong>{{ item.spec?.zone || '-' }}</strong>
                    <span>隔离组</span>
                    <strong>{{ item.spec?.isolationGroup || '-' }}</strong>
                    <span>配置分片</span>
                    <strong>{{ configShardText(item) }}</strong>
                  </div>
                </section>

                <section class="node-drilldown-card">
                  <h3>应用结果</h3>
                  <div class="key-value-grid">
                    <span>应用状态</span>
                    <strong>{{ applyStateLabel(item.status?.applyState) }}</strong>
                    <span>当前版本</span>
                    <strong>{{ item.status?.currentConfigVersion || '-' }}</strong>
                    <span>期望版本</span>
                    <strong>{{ item.status?.desiredConfigVersion || '-' }}</strong>
                    <span>Last-good</span>
                    <strong>{{ item.status?.lastGoodConfigVersion || '-' }}</strong>
                    <span>失败原因</span>
                    <strong>{{ applyFailureReason(item) }}</strong>
                  </div>
                </section>

                <section class="node-drilldown-card">
                  <h3>健康摘要</h3>
                  <div class="node-health-grid">
                    <StatusBadge :label="healthPartLabel('agent', item.status?.health?.agentHealthy)" :tone="healthPartTone(item.status?.health?.agentHealthy)" />
                    <StatusBadge :label="healthPartLabel('proxy', item.status?.health?.proxyHealthy)" :tone="healthPartTone(item.status?.health?.proxyHealthy)" />
                    <StatusBadge :label="healthPartLabel('控制面', item.status?.health?.controlPlaneConnected)" :tone="healthPartTone(item.status?.health?.controlPlaneConnected)" />
                  </div>
                  <p class="resource-subtitle">{{ item.status?.health?.message || '暂无健康说明' }}</p>
                </section>

                <section class="node-drilldown-card">
                  <h3>加载摘要</h3>
                  <div class="key-value-grid">
                    <span>路由</span>
                    <strong>{{ numberText(item.status?.loadedRouteCount) }}</strong>
                    <span>上游</span>
                    <strong>{{ numberText(item.status?.loadedUpstreamCount) }}</strong>
                    <span>策略</span>
                    <strong>{{ numberText(item.status?.loadedPolicyCount) }}</strong>
                    <span>心跳</span>
                    <strong>{{ heartbeatTime(item) }}</strong>
                  </div>
                </section>

                <section class="node-drilldown-card node-drilldown-card--wide">
                  <h3>上游健康</h3>
                  <div v-if="!item.status?.upstreamHealth?.length" class="state-box state-box--compact">暂无上游健康上报</div>
                  <table v-else class="resource-table compact-table">
                    <thead>
                      <tr>
                        <th>上游</th>
                        <th>状态</th>
                        <th>健康端点</th>
                        <th>异常端点</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="upstream in item.status.upstreamHealth" :key="upstream.upstreamName">
                        <td>{{ upstream.upstreamName || '-' }}</td>
                        <td>
                          <StatusBadge :label="upstreamHealthLabel(upstream)" :tone="upstreamHealthTone(upstream)" />
                        </td>
                        <td>{{ numberText(upstream.healthyEndpointCount) }}</td>
                        <td>{{ numberText(upstream.unhealthyEndpointCount) }}</td>
                      </tr>
                    </tbody>
                  </table>
                </section>
              </div>
            </td>
          </tr>
        </template>
      </tbody>
    </table>

    <ResourceDetailDrawer
      :open="Boolean(selectedItem)"
      :title="selectedItem?.spec?.nodeId || selectedItem?.metadata?.name || '节点详情'"
      :subtitle="selectedItem?.metadata?.namespace || '-'"
      :payload="selectedItem"
      @close="selectedItem = null"
    />
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue';
import { RefreshCw } from 'lucide-vue-next';
import ResourceDetailDrawer from '../components/ResourceDetailDrawer.vue';
import StatusBadge from '../components/StatusBadge.vue';
import { listResources } from '../api/client';
import { applyStateLabel, applyStateTone, formatTime, numberText, percentText } from '../utils/format';
import { notifyError } from '../utils/feedback';
import { getGlobalNamespace, onGlobalNamespaceChange, setGlobalNamespace } from '../utils/namespace';

interface GatewayUpstreamHealth {
  upstreamName?: string;
  healthyEndpointCount?: number;
  unhealthyEndpointCount?: number;
}

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
    desiredConfigVersion?: string;
    lastGoodConfigVersion?: string;
    applyState?: string;
    lastHeartbeatAt?: string | number;
    loadedUpstreamCount?: number;
    loadedRouteCount?: number;
    loadedPolicyCount?: number;
    lastApplyResult?: {
      version?: string;
      configHash?: string;
      state?: string;
      reason?: string;
      message?: string;
      startedAt?: string | number;
      finishedAt?: string | number;
    };
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
    upstreamHealth?: GatewayUpstreamHealth[];
  };
}

const namespace = ref(getGlobalNamespace('default'));
const keyword = ref('');
const loading = ref(false);
const error = ref('');
const items = ref<GatewayNode[]>([]);
const selectedItem = ref<GatewayNode | null>(null);
const expandedNodeKey = ref('');
let unsubscribeNamespace: (() => void) | null = null;

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

async function refresh() {
  await load();
  if (error.value) {
    notifyError('节点刷新失败', error.value);
    return;
  }
}

function openDetail(item: GatewayNode) {
  selectedItem.value = item;
}

function toggleNodeDetail(item: GatewayNode) {
  const key = nodeKey(item);
  expandedNodeKey.value = expandedNodeKey.value === key ? '' : key;
}

function isExpanded(item: GatewayNode) {
  return expandedNodeKey.value === nodeKey(item);
}

function nodeKey(item: GatewayNode) {
  return item.metadata?.uid || item.spec?.nodeId || item.metadata?.name || '';
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

function configShardText(item: GatewayNode) {
  return item.spec?.configShards?.length ? item.spec.configShards.join(', ') : '-';
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
  if (!health || [health.agentHealthy, health.proxyHealthy, health.controlPlaneConnected].every((value) => value === undefined || value === null)) {
    return '未知';
  }
  if (health.agentHealthy && health.proxyHealthy && health.controlPlaneConnected) {
    return '健康';
  }
  return '异常';
}

function healthTone(item: GatewayNode): 'success' | 'warning' | 'danger' | 'info' | 'neutral' {
  const health = item.status?.health;
  if (!health || [health.agentHealthy, health.proxyHealthy, health.controlPlaneConnected].every((value) => value === undefined || value === null)) {
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
  return numberText(value);
}

function healthMessage(item: GatewayNode) {
  return item.status?.health?.message || '';
}

function heartbeatTime(item: GatewayNode) {
  return formatTime(item.status?.lastHeartbeatAt);
}

function applyMessage(item: GatewayNode) {
  const result = item.status?.lastApplyResult;
  if (!result) {
    return '';
  }
  return [result.version, result.reason, result.message].filter(Boolean).join(' / ');
}

function applyFailureReason(item: GatewayNode) {
  const result = item.status?.lastApplyResult;
  if (!result || item.status?.applyState !== 'FAILED') {
    return '-';
  }
  return [result.reason, result.message].filter(Boolean).join(' / ') || '未上报失败原因';
}

function healthPartLabel(label: string, value?: boolean) {
  if (value === undefined || value === null) {
    return `${label} 未知`;
  }
  return `${label} ${value ? '正常' : '异常'}`;
}

function healthPartTone(value?: boolean): 'success' | 'warning' | 'danger' | 'info' | 'neutral' {
  if (value === undefined || value === null) {
    return 'neutral';
  }
  return value ? 'success' : 'danger';
}

function upstreamHealthLabel(upstream: GatewayUpstreamHealth) {
  const unhealthy = upstream.unhealthyEndpointCount || 0;
  const healthy = upstream.healthyEndpointCount || 0;
  if (healthy === 0 && unhealthy === 0) {
    return '未知';
  }
  return unhealthy > 0 ? '有异常' : '健康';
}

function upstreamHealthTone(upstream: GatewayUpstreamHealth): 'success' | 'warning' | 'danger' | 'info' | 'neutral' {
  const unhealthy = upstream.unhealthyEndpointCount || 0;
  const healthy = upstream.healthyEndpointCount || 0;
  if (healthy === 0 && unhealthy === 0) {
    return 'neutral';
  }
  return unhealthy > 0 ? 'warning' : 'success';
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
watch(() => [namespace.value], load);
watch(namespace, (value) => {
  setGlobalNamespace(value);
});
</script>
