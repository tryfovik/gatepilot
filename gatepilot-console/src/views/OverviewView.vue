<template>
  <div class="page-stack">
    <MetricStrip :items="metrics" />

    <section class="content-panel">
      <div class="panel-header">
        <div>
          <h2>全局概览</h2>
          <p>从项目、路由、上游、节点和发布状态判断当前网关是否能正常接入和转发。</p>
        </div>
        <button class="ghost-button" type="button" :disabled="loading" @click="loadOverview">
          <RefreshCw :size="16" />
          刷新概览
        </button>
      </div>

      <div v-if="loading" class="state-box state-box--compact">正在加载概览...</div>
      <div v-else-if="error" class="state-box state-box--error state-box--compact">{{ error }}</div>

      <div v-else class="overview-grid">
        <button class="summary-block summary-block--clickable" type="button" @click="go('/access/gateway-projects/project-list', '已进入项目明细')">
          <div class="summary-title">项目接入</div>
          <div class="summary-value">{{ totals.projects }} 个项目</div>
          <StatusBadge :label="totals.projects ? '可查看明细' : '等待接入'" :tone="totals.projects ? 'success' : 'neutral'" />
        </button>
        <button class="summary-block summary-block--clickable" type="button" @click="go('/traffic/routing/route-catalog', '已进入路由目录')">
          <div class="summary-title">路由与上游</div>
          <div class="summary-value">{{ totals.routes }} 路由 / {{ totals.upstreams }} 上游</div>
          <StatusBadge :label="totals.routes ? '已建模' : '暂无路由'" :tone="totals.routes ? 'info' : 'neutral'" />
        </button>
        <button class="summary-block summary-block--clickable" type="button" @click="go('/runtime/data-plane/node-instances', '已进入节点明细')">
          <div class="summary-title">数据面副本</div>
          <div class="summary-value">{{ readyNodeCount }} / {{ totals.nodes }}</div>
          <StatusBadge :label="nodeSummaryLabel" :tone="nodeSummaryTone" />
        </button>
      </div>
    </section>

    <section class="diagnostic-grid">
      <div class="content-panel">
        <div class="panel-header">
          <div>
            <h2>最近发布</h2>
            <p>展示最新快照和版本信息，便于从总览直接判断是否已经发布过。</p>
          </div>
          <button class="table-action" type="button" @click="go('/release/version-snapshots/snapshot-list', '已进入快照明细')">查看快照</button>
        </div>
        <div v-if="recentSnapshots.length === 0" class="empty-state">
          <div class="empty-state-title">暂无发布快照</div>
          <p>项目保存后还需要创建发布请求，controller-manager 生成快照后这里会展示版本。</p>
        </div>
        <div v-else class="timeline">
          <div v-for="snapshot in recentSnapshots" :key="snapshot.version || snapshot.releaseId" class="timeline-item">
            <StatusBadge label="已生成" tone="success" />
            <div>
              <div class="resource-name">{{ snapshot.projectName || '-' }}</div>
              <div class="resource-subtitle">{{ snapshot.version || '-' }} / {{ snapshot.configShard || 'default' }}</div>
              <div class="timeline-message">
                {{ snapshot.routeCount }} 路由 / {{ snapshot.upstreamCount }} 上游 / {{ snapshot.policyCount }} 策略，
                {{ formatTime(snapshot.capturedAt) }}
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="content-panel">
        <div class="panel-header">
          <div>
            <h2>节点健康</h2>
            <p>把副本心跳、last-good、apply 和上游健康先汇总，异常再进入节点页排查。</p>
          </div>
          <button class="table-action" type="button" @click="go('/runtime/data-plane/node-instances', '已进入节点明细')">查看节点</button>
        </div>
        <div v-if="recentNodes.length === 0" class="empty-state">
          <div class="empty-state-title">暂无节点上报</div>
          <p>agent / proxy 启动并完成节点注册后，总览会展示副本状态和最近心跳。</p>
        </div>
        <table v-else class="resource-table compact-table">
          <thead>
            <tr>
              <th>节点</th>
              <th>状态</th>
              <th>配置</th>
              <th>心跳</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="node in recentNodes" :key="node.metadata?.uid || node.spec?.nodeId || node.metadata?.name">
              <td>
                <div class="resource-name">{{ node.spec?.nodeId || node.metadata?.name || '-' }}</div>
                <div class="resource-subtitle">{{ node.spec?.role || '-' }} / {{ node.spec?.isolationGroup || '-' }}</div>
              </td>
              <td>
                <StatusBadge :label="nodePhaseLabel(node.status?.nodePhase)" :tone="nodePhaseTone(node.status?.nodePhase)" />
              </td>
              <td>
                <div class="compact-stack">
                  <span>{{ node.status?.currentConfigVersion || '-' }}</span>
                  <span class="resource-subtitle">last-good {{ node.status?.lastGoodConfigVersion || '-' }}</span>
                </div>
              </td>
              <td>{{ formatTime(node.status?.lastHeartbeatAt) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <section class="content-panel">
      <div class="panel-header">
        <div>
          <h2>最近审计</h2>
          <p>审计只做旁路观察，总览展示最近请求结果，完整筛选和分页在审计页处理。</p>
        </div>
        <button class="table-action" type="button" @click="go('/operations/runtime-audit/audit-records', '已进入审计明细')">查看审计</button>
      </div>
      <div v-if="recentAudits.length === 0" class="empty-state">
        <div class="empty-state-title">暂无审计记录</div>
        <p>业务流量通过 proxy 后，异步审计写入成功才会出现在这里。</p>
      </div>
      <table v-else class="resource-table compact-table">
        <thead>
          <tr>
            <th>请求</th>
            <th>路由</th>
            <th>状态</th>
            <th>耗时</th>
            <th>时间</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="audit in recentAudits" :key="audit.id || audit.traceId">
            <td>
              <div class="resource-name">{{ audit.method || '-' }} {{ audit.path || '-' }}</div>
              <div class="resource-subtitle">{{ audit.host || audit.clientIp || '-' }}</div>
            </td>
            <td>{{ audit.routeId || audit.upstreamName || '-' }}</td>
            <td>
              <StatusBadge :label="auditStatusLabel(audit)" :tone="auditStatusTone(audit)" />
            </td>
            <td>{{ numberText(audit.latencyMillis, 0) }} ms</td>
            <td>{{ formatTime(audit.occurredAt) }}</td>
          </tr>
        </tbody>
      </table>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { RefreshCw } from 'lucide-vue-next';
import MetricStrip from '../components/MetricStrip.vue';
import StatusBadge from '../components/StatusBadge.vue';
import {
  ConfigSnapshotSummaryResponse,
  RuntimeAuditRecord,
  listConfigSnapshotSummaries,
  listResources,
  listRuntimeAudits
} from '../api/client';
import { StatusTone, formatTime, numberText } from '../utils/format';
import { notifyError, notifyInfo } from '../utils/feedback';

interface GatewayNode {
  metadata?: {
    uid?: string;
    name?: string;
  };
  spec?: {
    nodeId?: string;
    role?: string;
    isolationGroup?: string;
  };
  status?: {
    nodePhase?: string;
    currentConfigVersion?: string;
    lastGoodConfigVersion?: string;
    lastHeartbeatAt?: string | number;
    upstreamHealth?: Array<{
      healthyEndpointCount?: number;
      unhealthyEndpointCount?: number;
    }>;
  };
}

const router = useRouter();
const loading = ref(false);
const error = ref('');
const totals = ref({
  projects: 0,
  routes: 0,
  upstreams: 0,
  nodes: 0,
  configShards: 0
});
const recentNodes = ref<GatewayNode[]>([]);
const recentSnapshots = ref<ConfigSnapshotSummaryResponse[]>([]);
const recentAudits = ref<RuntimeAuditRecord[]>([]);

const readyNodeCount = computed(() => recentNodes.value.filter((node) => node.status?.nodePhase === 'READY').length);
const nodeSummaryLabel = computed(() => {
  if (!totals.value.nodes) {
    return '暂无节点';
  }
  return readyNodeCount.value === totals.value.nodes ? '全部就绪' : '需要关注';
});
const nodeSummaryTone = computed<StatusTone>(() => {
  if (!totals.value.nodes) {
    return 'neutral';
  }
  return readyNodeCount.value === totals.value.nodes ? 'success' : 'warning';
});

const metrics = computed(() => [
  {
    label: '项目数',
    value: String(totals.value.projects),
    note: '点击查看项目明细',
    onClick: () => go('/access/gateway-projects/project-list', '已进入项目明细')
  },
  {
    label: '路由数',
    value: String(totals.value.routes),
    note: '点击查看路由目录',
    onClick: () => go('/traffic/routing/route-catalog', '已进入路由目录')
  },
  {
    label: 'Proxy 副本',
    value: String(totals.value.nodes),
    note: `${readyNodeCount.value} 个就绪`,
    onClick: () => go('/runtime/data-plane/node-instances', '已进入节点明细')
  },
  {
    label: '配置分片',
    value: String(totals.value.configShards),
    note: '点击查看平台设置',
    onClick: () => go('/platform/capacity/config-shards', '已进入配置分片管理')
  }
]);

onMounted(loadOverview);

async function loadOverview() {
  loading.value = true;
  error.value = '';
  try {
    const [projects, routes, upstreams, nodes, configShards, snapshots, audits] = await Promise.all([
      listResources<unknown>('projects', '', 1),
      listResources<unknown>('routes', '', 1),
      listResources<unknown>('upstreams', '', 1),
      listResources<GatewayNode>('nodes', '', 10),
      listResources<unknown>('config-shards', '', 1),
      listConfigSnapshotSummaries({ limit: 5 }),
      listRuntimeAudits({ limit: 5 })
    ]);
    totals.value = {
      projects: projects.total,
      routes: routes.total,
      upstreams: upstreams.total,
      nodes: nodes.total,
      configShards: configShards.total
    };
    recentNodes.value = nodes.items;
    recentSnapshots.value = snapshots.items;
    recentAudits.value = audits.items;
  } catch (err) {
    error.value = err instanceof Error ? err.message : '概览加载失败';
    notifyError('概览加载失败', error.value);
  } finally {
    loading.value = false;
  }
}

function go(path: string, message: string) {
  notifyInfo(message);
  void router.push(path);
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

function nodePhaseTone(value?: string): StatusTone {
  if (value === 'READY') return 'success';
  if (value === 'REGISTERED') return 'info';
  if (value === 'DRAINING') return 'warning';
  if (value === 'NOT_READY' || value === 'OFFLINE') return 'danger';
  return 'neutral';
}

function auditStatusLabel(audit: RuntimeAuditRecord) {
  if (audit.fallback) {
    return 'Fallback';
  }
  if (audit.status && audit.status >= 500) {
    return '服务异常';
  }
  if (audit.status && audit.status >= 400) {
    return '请求异常';
  }
  return audit.outcome || '成功';
}

function auditStatusTone(audit: RuntimeAuditRecord): StatusTone {
  if (audit.fallback || (audit.status && audit.status >= 500)) {
    return 'danger';
  }
  if (audit.status && audit.status >= 400) {
    return 'warning';
  }
  return 'success';
}
</script>
