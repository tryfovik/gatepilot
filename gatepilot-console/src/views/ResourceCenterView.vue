<template>
  <div class="page-stack">
    <section class="content-panel">
      <div class="panel-header">
        <div>
          <h2>声明式资源中心</h2>
          <p>按资源类型查看 GatePilot 期望状态，先看资源目录，再展开某一类资源的明细。</p>
        </div>
        <button class="ghost-button" type="button" :disabled="countsLoading" @click="refreshCounts">
          <RefreshCw :size="16" />
          刷新统计
        </button>
      </div>

      <MetricStrip :items="metrics" />

      <div class="resource-center-toolbar">
        <div class="tabbar">
          <button
            v-for="scope in scopes"
            :key="scope.value"
            class="tab-button"
            :class="{ 'tab-button--active': activeScope === scope.value }"
            type="button"
            @click="setScope(scope.value)"
          >
            {{ scope.label }}
          </button>
        </div>
        <label class="resource-search">
          <Search :size="16" />
          <input v-model.trim="keyword" class="search-input" placeholder="搜索资源类型、Kind 或说明" />
        </label>
      </div>
    </section>

    <section class="content-panel">
      <div class="panel-header">
        <div>
          <h2>资源目录</h2>
          <p>点击一行展开最近资源明细，再点一次收起。</p>
        </div>
        <StatusBadge :label="`${visibleResources.length} 类资源`" tone="info" />
      </div>

      <div v-if="visibleGroups.length === 0" class="empty-state">
        <div class="empty-state-title">没有匹配的资源类型</div>
        <p>换一个关键字，或切回全部资源。</p>
      </div>

      <div v-else class="resource-directory">
        <section v-for="group in visibleGroups" :key="group.key" class="resource-directory-group">
          <div class="resource-directory-header">
            <div>
              <h3>{{ group.label }}</h3>
              <p>{{ group.description }}</p>
            </div>
            <StatusBadge :label="`${groupTotal(group.resources)} 个资源`" tone="neutral" />
          </div>

          <table class="resource-table compact-table resource-directory-table">
            <thead>
              <tr>
                <th>资源类型</th>
                <th>数量</th>
                <th>Kind</th>
                <th>说明</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <template v-for="resource in group.resources" :key="resource.resourceType">
                <tr :class="{ 'resource-directory-row--active': activeResource?.resourceType === resource.resourceType }">
                  <td>
                    <div class="resource-name">{{ resource.label }}</div>
                    <div class="resource-subtitle">{{ resource.groupLabel }}</div>
                  </td>
                  <td>
                    <strong>{{ counts[resource.resourceType] ?? 0 }}</strong>
                  </td>
                  <td class="mono-cell">{{ resource.kind }}</td>
                  <td>{{ resource.description }}</td>
                  <td>
                    <button
                      class="table-action"
                      type="button"
                      :aria-label="`${activeResource?.resourceType === resource.resourceType ? '收起' : '查看'}${resource.label}明细`"
                      @click="toggleResource(resource)"
                    >
                      <component :is="activeResource?.resourceType === resource.resourceType ? ChevronDown : ChevronRight" :size="14" />
                      {{ activeResource?.resourceType === resource.resourceType ? '收起明细' : '查看明细' }}
                    </button>
                  </td>
                </tr>
                <tr v-if="activeResource?.resourceType === resource.resourceType" class="resource-expanded-row">
                  <td colspan="5">
                    <div class="resource-expanded-content">
                      <div class="resource-expanded-header">
                        <div>
                          <h3>{{ resource.label }}明细</h3>
                          <p>{{ resource.description }}</p>
                        </div>
                        <StatusBadge :label="previewStatusLabel" :tone="previewStatusTone" />
                      </div>

                      <div v-if="previewLoading" class="state-box state-box--compact">正在加载资源明细...</div>
                      <div v-else-if="previewError" class="state-box state-box--error state-box--compact">{{ previewError }}</div>
                      <div v-else-if="previewItems.length === 0" class="empty-state">
                        <div class="empty-state-title">暂无{{ resource.label }}</div>
                        <p>这个资源类型还没有数据，后续保存或发布后会出现在这里。</p>
                      </div>

                      <table v-else class="resource-table compact-table">
                        <thead>
                          <tr>
                            <th>名称</th>
                            <th>命名空间</th>
                            <th>状态</th>
                            <th>摘要</th>
                            <th>更新时间</th>
                            <th>操作</th>
                          </tr>
                        </thead>
                        <tbody>
                          <tr v-for="item in previewItems" :key="item.metadata?.uid || `${item.metadata?.namespace}-${item.metadata?.name}`">
                            <td>
                              <div class="resource-name">{{ item.metadata?.name || '-' }}</div>
                              <div class="resource-subtitle">{{ item.spec?.displayName || item.spec?.host || item.metadata?.uid || '-' }}</div>
                            </td>
                            <td>{{ item.metadata?.namespace || '-' }}</td>
                            <td>
                              <StatusBadge :label="previewStatus(item).label" :tone="previewStatus(item).tone" />
                            </td>
                            <td>{{ previewSummary(item) }}</td>
                            <td>{{ formatTime(item.metadata?.updatedAt || item.metadata?.createdAt) }}</td>
                            <td>
                              <button class="table-action" type="button" @click="openPreviewDetail(item)">查看</button>
                            </td>
                          </tr>
                        </tbody>
                      </table>

                      <div v-if="previewItems.length > 0" class="resource-preview-note">
                        已展示 {{ previewItems.length }} 条<span v-if="activeTotal > previewItems.length">，还有 {{ activeTotal - previewItems.length }} 条在对应管理页继续查看</span>
                      </div>
                    </div>
                  </td>
                </tr>
              </template>
            </tbody>
          </table>
        </section>
      </div>
    </section>

    <ResourceDetailDrawer
      :open="Boolean(selectedPreview)"
      :title="selectedPreview?.metadata?.name || '资源详情'"
      :subtitle="activeResource?.label || '-'"
      :payload="selectedPreview"
      @close="selectedPreview = null"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { ChevronDown, ChevronRight, RefreshCw, Search } from 'lucide-vue-next';
import MetricStrip from '../components/MetricStrip.vue';
import ResourceDetailDrawer from '../components/ResourceDetailDrawer.vue';
import StatusBadge from '../components/StatusBadge.vue';
import { listResources } from '../api/client';
import { StatusTone, applyStateLabel, applyStateTone, formatTime, listText, resourcePhaseLabel, resourcePhaseTone } from '../utils/format';

type ResourceScope = 'all' | 'platform' | 'traffic' | 'runtime';

interface ResourceDescriptor {
  label: string;
  kind: string;
  resourceType: string;
  description: string;
  group: Exclude<ResourceScope, 'all'>;
  groupLabel: string;
}

interface ResourceGroup {
  key: Exclude<ResourceScope, 'all'>;
  label: string;
  description: string;
  resources: ResourceDescriptor[];
}

interface PreviewResource {
  metadata?: {
    uid?: string;
    name?: string;
    namespace?: string;
    createdAt?: string | number;
    updatedAt?: string | number;
  };
  spec?: Record<string, unknown> & {
    displayName?: string;
    host?: string;
    ownerTeam?: string;
    owner?: string;
    contact?: string;
    environment?: string;
    trafficTier?: string;
    configShard?: string;
    defaultConfigShard?: string;
    defaultIsolationGroup?: string;
    tier?: string;
    strategy?: string;
    type?: string;
    version?: string;
    serverAddr?: string;
    namespace?: string;
    group?: string;
    projectRef?: {
      name?: string;
    };
    path?: {
      value?: string;
    };
    upstreamRef?: {
      name?: string;
    };
    loadBalance?: string;
    discovery?: {
      type?: string;
      serviceName?: string;
    };
    endpoints?: unknown[];
    routes?: unknown[];
    upstreams?: unknown[];
    policies?: unknown[];
  };
  status?: {
    phase?: string;
    nodePhase?: string;
    applyState?: string;
    currentPublishedVersion?: string;
    healthyEndpointCount?: number;
    unhealthyEndpointCount?: number;
  };
}

const resourceGroups: ResourceGroup[] = [
  {
    key: 'platform',
    label: '平台基础资源',
    description: '负责项目接入前的组织、环境、入口、容量和动态参数',
    resources: [
      { label: '命名空间', kind: 'GatewayNamespace', resourceType: 'namespaces', group: 'platform', groupLabel: '组织与入口', description: '项目、路由、策略、发布和审计的隔离边界' },
      { label: '团队', kind: 'PlatformTeam', resourceType: 'teams', group: 'platform', groupLabel: '组织与入口', description: '项目接入、发布和故障响应的负责团队' },
      { label: '环境', kind: 'PlatformEnvironment', resourceType: 'environments', group: 'platform', groupLabel: '组织与入口', description: 'dev、test、stage、prod 等运行环境' },
      { label: '入口域名', kind: 'IngressDomain', resourceType: 'ingress-domains', group: 'platform', groupLabel: '组织与入口', description: '项目接入时可选择的已授权入口域名' },
      { label: '配置分片', kind: 'ConfigShard', resourceType: 'config-shards', group: 'platform', groupLabel: '容量与隔离', description: '大规模配置生成、下发和观察的分片边界' },
      { label: '隔离组', kind: 'IsolationGroup', resourceType: 'isolation-groups', group: 'platform', groupLabel: '容量与隔离', description: '高流量项目可绑定的 agent / proxy 副本池' },
      { label: '流量等级', kind: 'TrafficTier', resourceType: 'traffic-tiers', group: 'platform', groupLabel: '容量与隔离', description: '项目容量等级，用于推荐分片和隔离组' },
      { label: '注册中心', kind: 'RegistryCenter', resourceType: 'registry-centers', group: 'platform', groupLabel: '运行参数', description: 'Nacos 等服务发现连接信息，上游只引用服务名' },
      { label: '动态参数', kind: 'ControlPlaneSetting', resourceType: 'control-plane-settings', group: 'platform', groupLabel: '运行参数', description: '控制面、agent、proxy 可热生效的运行参数' }
    ]
  },
  {
    key: 'traffic',
    label: '业务流量资源',
    description: '负责请求进入网关后的匹配、转发、治理、发布和认证',
    resources: [
      { label: '项目', kind: 'GatewayProject', resourceType: 'projects', group: 'traffic', groupLabel: '接入管理', description: '项目隔离、域名、分片和发布边界' },
      { label: '路由', kind: 'GatewayRoute', resourceType: 'routes', group: 'traffic', groupLabel: '流量配置', description: '入口 Host、路径、方法和默认上游匹配规则' },
      { label: '上游', kind: 'Upstream', resourceType: 'upstreams', group: 'traffic', groupLabel: '流量配置', description: '后端端点、负载均衡和健康检查配置' },
      { label: '流量策略', kind: 'TrafficPolicy', resourceType: 'traffic-policies', group: 'traffic', groupLabel: '治理策略', description: '限流、重试、熔断、Fallback 和染色规则' },
      { label: '发布策略', kind: 'ReleasePolicy', resourceType: 'release-policies', group: 'traffic', groupLabel: '发布策略', description: '蓝绿、灰度、权重分流和候选上游规则' },
      { label: '认证策略', kind: 'AuthPolicy', resourceType: 'auth-policies', group: 'traffic', groupLabel: '认证策略', description: 'API Key、JWT、OAuth2、Basic、mTLS 等认证规则' }
    ]
  },
  {
    key: 'runtime',
    label: '运行观测资源',
    description: '负责展示发布后的运行配置、版本快照、节点状态和控制面事件',
    resources: [
      { label: '已发布配置', kind: 'PublishedConfig', resourceType: 'published-configs', group: 'runtime', groupLabel: '运行配置', description: '数据面实际消费的已发布运行态配置' },
      { label: '配置快照', kind: 'GatewayConfigSnapshot', resourceType: 'config-snapshots', group: 'runtime', groupLabel: '版本记录', description: '发布后的版本记录、版本对比和回滚依据' },
      { label: '节点', kind: 'GatewayNode', resourceType: 'nodes', group: 'runtime', groupLabel: '数据面节点', description: 'agent / proxy 副本、心跳、last-good 和 apply 状态' },
      { label: '事件', kind: 'GatewayEvent', resourceType: 'events', group: 'runtime', groupLabel: '控制面事件', description: '发布、回滚、同步和控制面异常事件' }
    ]
  }
];

const resources = resourceGroups.flatMap((group) => group.resources);
const scopes: Array<{ label: string; value: ResourceScope }> = [
  { label: '全部资源', value: 'all' },
  { label: '平台基础', value: 'platform' },
  { label: '业务流量', value: 'traffic' },
  { label: '运行观测', value: 'runtime' }
];

const activeScope = ref<ResourceScope>('all');
const activeResource = ref<ResourceDescriptor | null>(null);
const counts = ref<Record<string, number>>({});
const countsLoading = ref(false);
const keyword = ref('');
const previewItems = ref<PreviewResource[]>([]);
const previewLoading = ref(false);
const previewError = ref('');
const selectedPreview = ref<PreviewResource | null>(null);

const metrics = computed(() => [
  {
    label: '资源类型',
    value: String(resources.length),
    note: '点击查看全部资源目录',
    onClick: () => setScope('all')
  },
  {
    label: '资源总数',
    value: String(totalResourceCount.value),
    note: '所有命名空间',
    onClick: () => setScope('all')
  },
  {
    label: '基础资源',
    value: String(groupCount('platform')),
    note: '组织 / 容量 / 参数',
    onClick: () => setScope('platform')
  },
  {
    label: '流量资源',
    value: String(groupCount('traffic')),
    note: '项目 / 路由 / 上游 / 策略',
    onClick: () => setScope('traffic')
  }
]);

const totalResourceCount = computed(() =>
  Object.values(counts.value).reduce((total, value) => total + value, 0)
);

const visibleResources = computed(() => {
  const value = keyword.value.toLowerCase();
  return resources.filter((resource) => {
    const scopeMatched = activeScope.value === 'all' || resource.group === activeScope.value;
    if (!scopeMatched) {
      return false;
    }
    if (!value) {
      return true;
    }
    return [
      resource.label,
      resource.kind,
      resource.resourceType,
      resource.groupLabel,
      resource.description
    ].join(' ').toLowerCase().includes(value);
  });
});

const visibleGroups = computed(() =>
  resourceGroups
    .map((group) => ({
      ...group,
      resources: visibleResources.value.filter((resource) => resource.group === group.key)
    }))
    .filter((group) => group.resources.length > 0)
);

const activeTotal = computed(() => activeResource.value ? counts.value[activeResource.value.resourceType] || 0 : 0);
const previewStatusLabel = computed(() => (previewError.value ? '加载失败' : `${activeTotal.value} 个资源`));
const previewStatusTone = computed<StatusTone>(() => (previewError.value ? 'danger' : activeTotal.value ? 'info' : 'neutral'));

onMounted(refreshCounts);

function setScope(scope: ResourceScope) {
  activeScope.value = scope;
  activeResource.value = null;
  previewItems.value = [];
  previewError.value = '';
}

async function toggleResource(resource: ResourceDescriptor) {
  if (activeResource.value?.resourceType === resource.resourceType) {
    activeResource.value = null;
    previewItems.value = [];
    previewError.value = '';
    return;
  }
  activeResource.value = resource;
  await loadPreview();
}

async function refreshCounts() {
  countsLoading.value = true;
  try {
    const entries = await Promise.all(
      resources.map(async (resource) => {
        try {
          const page = await listResources<unknown>(resource.resourceType, '', 1);
          return [resource.resourceType, page.total] as const;
        } catch {
          return [resource.resourceType, counts.value[resource.resourceType] ?? 0] as const;
        }
      })
    );
    counts.value = Object.fromEntries(entries);
    if (activeResource.value) {
      await loadPreview();
    }
  } finally {
    countsLoading.value = false;
  }
}

async function loadPreview() {
  if (!activeResource.value) {
    previewItems.value = [];
    return;
  }
  previewLoading.value = true;
  previewError.value = '';
  try {
    const resource = activeResource.value;
    const page = await listResources<PreviewResource>(resource.resourceType, '', 10);
    previewItems.value = page.items;
    counts.value = {
      ...counts.value,
      [resource.resourceType]: page.total
    };
  } catch (err) {
    previewItems.value = [];
    previewError.value = err instanceof Error ? err.message : '加载失败';
  } finally {
    previewLoading.value = false;
  }
}

function groupCount(group: Exclude<ResourceScope, 'all'>) {
  return resources
    .filter((resource) => resource.group === group)
    .reduce((total, resource) => total + (counts.value[resource.resourceType] || 0), 0);
}

function groupTotal(groupResources: ResourceDescriptor[]) {
  return groupResources.reduce((total, resource) => total + (counts.value[resource.resourceType] || 0), 0);
}

function previewStatus(item: PreviewResource): { label: string; tone: StatusTone } {
  if (activeResource.value?.resourceType === 'published-configs') {
    return {
      label: applyStateLabel(item.status?.applyState),
      tone: applyStateTone(item.status?.applyState)
    };
  }
  if (activeResource.value?.resourceType === 'upstreams' && (item.status?.healthyEndpointCount !== undefined || item.status?.unhealthyEndpointCount !== undefined)) {
    return {
      label: `健康 ${item.status.healthyEndpointCount ?? 0} / 异常 ${item.status.unhealthyEndpointCount ?? 0}`,
      tone: item.status.unhealthyEndpointCount ? 'warning' : 'success'
    };
  }
  const phase = item.status?.phase || item.status?.nodePhase;
  return {
    label: resourcePhaseLabel(phase, '已配置'),
    tone: phase ? resourcePhaseTone(phase) : 'info'
  };
}

function openPreviewDetail(item: PreviewResource) {
  selectedPreview.value = item;
}

function previewSummary(item: PreviewResource) {
  const spec = item.spec || {};
  if (activeResource.value?.resourceType === 'namespaces') {
    return [spec.ownerTeam, spec.environment, spec.defaultConfigShard, spec.defaultIsolationGroup].filter(Boolean).join(' / ') || '-';
  }
  if (activeResource.value?.resourceType === 'teams') {
    return [spec.owner, spec.contact].filter(Boolean).join(' / ') || '-';
  }
  if (activeResource.value?.resourceType === 'environments') {
    return [spec.tier, spec.defaultConfigShard, spec.defaultIsolationGroup].filter(Boolean).join(' / ') || '-';
  }
  if (activeResource.value?.resourceType === 'projects') {
    return [spec.ownerTeam, spec.environment, spec.trafficTier, spec.configShard].filter(Boolean).join(' / ') || '-';
  }
  if (activeResource.value?.resourceType === 'routes') {
    return [spec.path?.value, spec.upstreamRef?.name].filter(Boolean).join(' / ') || '-';
  }
  if (activeResource.value?.resourceType === 'upstreams') {
    const discovery = spec.discovery?.type === 'NACOS' ? `Nacos ${spec.discovery.serviceName || '-'}` : `${spec.endpoints?.length ?? 0} 端点`;
    return `${spec.loadBalance || '-'} / ${discovery}`;
  }
  if (activeResource.value?.resourceType === 'registry-centers') {
    return [spec.type, spec.serverAddr, spec.namespace || 'public', spec.group].filter(Boolean).join(' / ') || '-';
  }
  if (activeResource.value?.resourceType === 'published-configs') {
    return `${spec.routes?.length ?? 0} 路由 / ${spec.upstreams?.length ?? 0} 上游 / ${spec.policies?.length ?? 0} 策略`;
  }
  if (activeResource.value?.resourceType === 'ingress-domains') {
    return [spec.host, spec.ownerTeam, spec.defaultNamespace].filter(Boolean).join(' / ') || '-';
  }
  return listText([spec.displayName as string, spec.strategy as string, spec.version as string, spec.projectRef?.name].filter(Boolean), '-');
}
</script>
