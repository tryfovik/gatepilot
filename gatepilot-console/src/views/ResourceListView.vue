<template>
  <section class="content-panel">
    <div class="panel-header">
      <div>
        <h2>{{ title }}</h2>
        <p>{{ description }}</p>
      </div>
      <div class="panel-actions">
        <button v-if="resourceType === 'projects' && !inlineOnly" class="primary-button" type="button" @click="router.push('/access/project-onboarding')">
          <Plus :size="16" />
          接入项目
        </button>
        <button v-if="editableResource && !inlineOnly" class="primary-button" type="button" @click="startCreate">
          <Plus :size="16" />
          新增{{ editableResource.label }}
        </button>
        <button class="ghost-button" type="button" :disabled="loading" @click="refresh">
          <RefreshCw :size="16" />
          刷新
        </button>
      </div>
    </div>

    <div class="filterbar">
      <input v-model="keyword" class="search-input" placeholder="搜索名称、命名空间或版本" />
      <select v-model="namespace" class="select-input">
        <option value="">全部命名空间</option>
        <option v-for="option in namespaceOptions" :key="option.value" :value="option.value">
          {{ option.label }}
        </option>
      </select>
    </div>

    <MetricStrip v-if="resourceType === 'upstreams' && !loading && !error && items.length > 0" :items="upstreamMetrics" />

    <div v-if="loading" class="state-box">正在加载...</div>
    <div v-else-if="error" class="state-box state-box--error">{{ error }}</div>
    <div v-else-if="filteredItems.length === 0" class="empty-state">
      <div class="empty-state-title">{{ emptyTitle }}</div>
      <p>{{ emptyMessage }}</p>
      <button v-if="resourceType === 'projects' && !inlineOnly" class="primary-button" type="button" @click="router.push('/access/project-onboarding')">
        <Plus :size="16" />
        接入第一个项目
      </button>
    </div>

    <table v-else-if="resourceType === 'upstreams'" class="resource-table">
      <thead>
        <tr>
          <th>上游服务</th>
          <th>所属项目</th>
          <th>转发方式</th>
          <th>服务端点</th>
          <th>健康</th>
          <th>更新时间</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="item in filteredItems" :key="item.metadata?.uid || item.metadata?.name">
          <td>
            <div class="resource-name">{{ item.metadata?.name || '-' }}</div>
            <div class="resource-subtitle">{{ item.metadata?.namespace || '-' }}</div>
          </td>
          <td>{{ item.spec?.projectRef?.name || '-' }}</td>
          <td>
            <div class="compact-stack">
              <span>{{ protocolLabel(item.spec?.protocol) }}</span>
              <span class="resource-subtitle">{{ loadBalanceLabel(item.spec?.loadBalance) }}</span>
            </div>
          </td>
          <td>
            <div class="compact-stack">
              <span>{{ endpointSummary(item) }}</span>
              <span class="resource-subtitle">{{ endpointPreview(item) }}</span>
            </div>
          </td>
          <td>
            <StatusBadge :label="resourceStatus(item).label" :tone="resourceStatus(item).tone" />
          </td>
          <td>{{ formatTime(resourceTime(item)) }}</td>
          <td>
            <div class="table-actions">
              <button class="table-action" type="button" @click="openDetail(item)">查看</button>
              <button v-if="editableResource" class="table-action" type="button" @click="startEdit(item)">编辑</button>
              <button
                v-for="action in rowActions(item)"
                :key="action.label"
                class="table-action"
                type="button"
                @click="action.run()"
              >
                {{ action.label }}
              </button>
            </div>
          </td>
        </tr>
      </tbody>
    </table>

    <table v-else class="resource-table">
      <thead>
        <tr>
          <th>名称</th>
          <th>命名空间</th>
          <th>状态</th>
          <th>摘要</th>
          <th>{{ versionColumnTitle }}</th>
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
            <StatusBadge :label="resourceStatus(item).label" :tone="resourceStatus(item).tone" />
          </td>
          <td>{{ summaryText(item) }}</td>
          <td>{{ versionText(item) }}</td>
          <td>{{ formatTime(resourceTime(item)) }}</td>
          <td>
            <div class="table-actions">
              <button class="table-action" type="button" @click="openDetail(item)">查看</button>
              <button v-if="editableResource" class="table-action" type="button" @click="startEdit(item)">编辑</button>
              <button
                v-for="action in rowActions(item)"
                :key="action.label"
                class="table-action"
                type="button"
                @click="action.run()"
              >
                {{ action.label }}
              </button>
            </div>
          </td>
        </tr>
      </tbody>
    </table>

    <div v-if="formOpen && editableResource" class="drawer-backdrop form-drawer-backdrop" role="presentation" @click.self="closeForm">
      <form class="detail-drawer form-drawer" aria-label="资源编辑" @submit.prevent="saveCurrent">
        <div class="panel-header">
          <div>
            <h2>{{ editingName ? '编辑' : '新增' }}{{ editableResource.label }}</h2>
            <p>{{ editableResource.description }}</p>
          </div>
          <button class="icon-button" type="button" aria-label="关闭" @click="closeForm">×</button>
        </div>
        <div class="drawer-content form-drawer-content">
          <div class="form-subsection">
            <div class="form-section-title">
              <strong>基础信息</strong>
              <span>先保存资源，发布中心会把资源生成运行配置</span>
            </div>
            <div class="form-grid">
              <label>
                <span>资源名称 <em class="required-star" aria-label="必填">*</em></span>
                <input v-model.trim="resourceForm.name" class="search-input" :disabled="Boolean(editingName)" required />
              </label>
              <label v-if="resourceType !== 'projects'">
                <span>所属项目 <em class="required-star" aria-label="必填">*</em></span>
                <select v-model="resourceForm.projectName" class="select-input" required>
                  <option value="">请选择项目</option>
                  <option v-for="option in relationOptions.projects" :key="option.value" :value="option.value">{{ option.label }}</option>
                </select>
              </label>
              <label v-if="resourceType === 'projects'">
                <span>展示名称</span>
                <input v-model.trim="resourceForm.displayName" class="search-input" />
              </label>
              <label v-if="resourceType === 'projects'">
                <span>负责团队</span>
                <select v-model="resourceForm.ownerTeam" class="select-input">
                  <option value="">不指定团队</option>
                  <option v-for="option in relationOptions.teams" :key="option.value" :value="option.value">{{ option.label }}</option>
                </select>
              </label>
              <label v-if="resourceType === 'projects'">
                <span>环境</span>
                <select v-model="resourceForm.environment" class="select-input">
                  <option value="">不指定环境</option>
                  <option v-for="option in relationOptions.environments" :key="option.value" :value="option.value">{{ option.label }}</option>
                </select>
              </label>
              <label v-if="resourceType === 'projects'">
                <span>配置分片</span>
                <select v-model="resourceForm.configShard" class="select-input">
                  <option value="">默认分片</option>
                  <option v-for="option in relationOptions.configShards" :key="option.value" :value="option.value">{{ option.label }}</option>
                </select>
              </label>
            </div>
          </div>

          <div class="form-subsection">
            <div class="form-section-title">
              <strong>{{ editableResource.optionTitle }}</strong>
              <span>{{ editableResource.optionNote }}</span>
            </div>
            <div class="form-grid">
              <template v-if="resourceType === 'projects'">
                <label>
                  <span>入口域名</span>
                  <select v-model="resourceForm.hosts" class="select-input">
                    <option value="">不指定入口域名</option>
                    <option v-for="option in relationOptions.ingressDomains" :key="option.value" :value="option.value">{{ option.label }}</option>
                  </select>
                </label>
                <label class="wide-field">
                  <span>说明</span>
                  <textarea v-model.trim="resourceForm.description" class="text-input" rows="3" />
                </label>
              </template>

              <template v-else-if="resourceType === 'routes'">
                <label>
                  <span>入口域名 <em class="required-star" aria-label="必填">*</em></span>
                  <select v-model="resourceForm.hosts" class="select-input" required>
                    <option value="">请选择入口域名</option>
                    <option v-for="option in relationOptions.ingressDomains" :key="option.value" :value="option.value">{{ option.label }}</option>
                  </select>
                </label>
                <label>
                  <span>路径前缀 <em class="required-star" aria-label="必填">*</em></span>
                  <input v-model.trim="resourceForm.path" class="search-input" required />
                </label>
                <label>
                  <span>默认上游 <em class="required-star" aria-label="必填">*</em></span>
                  <select v-model="resourceForm.upstreamName" class="select-input" required>
                    <option value="">请选择上游</option>
                    <option v-for="option in relationOptions.upstreams" :key="option.value" :value="option.value">{{ option.label }}</option>
                  </select>
                </label>
                <label>
                  <span>HTTP 方法</span>
                  <input v-model.trim="resourceForm.methods" class="search-input" placeholder="GET,POST" />
                </label>
                <label class="check-row">
                  <input v-model="resourceForm.stripPrefix" type="checkbox" />
                  <span>转发时去除路径前缀</span>
                </label>
              </template>

              <template v-else-if="resourceType === 'upstreams'">
                <label>
                  <span>协议</span>
                  <select v-model="resourceForm.protocol" class="select-input">
                    <option value="HTTP">HTTP</option>
                    <option value="HTTPS">HTTPS</option>
                  </select>
                </label>
                <label>
                  <span>负载均衡</span>
                  <select v-model="resourceForm.loadBalance" class="select-input">
                    <option value="ROUND_ROBIN">轮询</option>
                    <option value="WEIGHTED_ROUND_ROBIN">加权轮询</option>
                    <option value="RANDOM">随机</option>
                  </select>
                </label>
                <label>
                  <span>端点地址 <em class="required-star" aria-label="必填">*</em></span>
                  <input v-model.trim="resourceForm.endpointHost" class="search-input" required />
                </label>
                <label>
                  <span>端点端口 <em class="required-star" aria-label="必填">*</em></span>
                  <input v-model.number="resourceForm.endpointPort" class="search-input" type="number" min="1" max="65535" required />
                </label>
                <label>
                  <span>端点权重</span>
                  <input v-model.number="resourceForm.endpointWeight" class="search-input" type="number" min="0" />
                </label>
                <label class="check-row">
                  <input v-model="resourceForm.healthCheckEnabled" type="checkbox" />
                  <span>启用健康检查</span>
                </label>
                <label>
                  <span>健康检查路径</span>
                  <input v-model.trim="resourceForm.healthPath" class="search-input" placeholder="/actuator/health" />
                </label>
              </template>

              <template v-else-if="resourceType === 'traffic-policies'">
                <label class="check-row">
                  <input v-model="resourceForm.rateLimitEnabled" type="checkbox" />
                  <span>限流</span>
                </label>
                <label v-if="resourceForm.rateLimitEnabled">
                  <span>限流 QPS</span>
                  <input v-model.number="resourceForm.requestsPerSecond" class="search-input" type="number" min="1" />
                </label>
                <label class="check-row">
                  <input v-model="resourceForm.retryEnabled" type="checkbox" />
                  <span>重试</span>
                </label>
                <label v-if="resourceForm.retryEnabled">
                  <span>最大重试次数</span>
                  <input v-model.number="resourceForm.maxAttempts" class="search-input" type="number" min="1" />
                </label>
                <label>
                  <span>染色 Header</span>
                  <input v-model.trim="resourceForm.colorHeader" class="search-input" placeholder="x-gatepilot-color" />
                </label>
                <label>
                  <span>染色值</span>
                  <input v-model.trim="resourceForm.colorValue" class="search-input" placeholder="green" />
                </label>
              </template>

              <template v-else-if="resourceType === 'release-policies'">
                <label>
                  <span>发布策略</span>
                  <select v-model="resourceForm.strategy" class="select-input">
                    <option value="BLUE_GREEN">蓝绿发布</option>
                    <option value="CANARY">灰度发布</option>
                    <option value="TRAFFIC_SPLIT">固定权重</option>
                    <option value="SHADOW">影子流量</option>
                  </select>
                </label>
                <label>
                  <span>目标路由</span>
                  <select v-model="resourceForm.routeName" class="select-input">
                    <option value="">不指定路由</option>
                    <option v-for="option in relationOptions.routes" :key="option.value" :value="option.value">{{ option.label }}</option>
                  </select>
                </label>
                <label>
                  <span>稳定上游</span>
                  <select v-model="resourceForm.stableUpstreamName" class="select-input">
                    <option value="">请选择稳定上游</option>
                    <option v-for="option in relationOptions.upstreams" :key="option.value" :value="option.value">{{ option.label }}</option>
                  </select>
                </label>
                <label>
                  <span>候选上游</span>
                  <select v-model="resourceForm.candidateUpstreamName" class="select-input">
                    <option value="">请选择候选上游</option>
                    <option v-for="option in relationOptions.upstreams" :key="option.value" :value="option.value">{{ option.label }}</option>
                  </select>
                </label>
                <label>
                  <span>候选权重</span>
                  <input v-model.number="resourceForm.candidateWeight" class="search-input" type="number" min="0" max="100" />
                </label>
                <label>
                  <span>候选染色值</span>
                  <input v-model.trim="resourceForm.colorValue" class="search-input" placeholder="green" />
                </label>
              </template>

              <template v-else-if="resourceType === 'auth-policies'">
                <label>
                  <span>认证类型</span>
                  <select v-model="resourceForm.authType" class="select-input">
                    <option value="NONE">不启用</option>
                    <option value="API_KEY">API Key</option>
                    <option value="JWT">JWT</option>
                    <option value="OAUTH2">OAuth2</option>
                    <option value="BASIC">Basic</option>
                    <option value="MTLS">双向 TLS</option>
                  </select>
                </label>
                <label class="check-row">
                  <input v-model="resourceForm.anonymousAllowed" type="checkbox" />
                  <span>允许匿名访问</span>
                </label>
              </template>
            </div>
          </div>
          <div v-if="formError" class="state-box state-box--error state-box--compact">{{ formError }}</div>
        </div>
        <div class="form-actions settings-actions form-drawer-actions">
          <button class="ghost-button" type="button" :disabled="saving" @click="closeForm">取消</button>
          <button class="primary-button" type="submit" :disabled="saving">
            保存{{ editableResource.label }}
          </button>
        </div>
      </form>
    </div>

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
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { Plus, RefreshCw } from 'lucide-vue-next';
import MetricStrip from '../components/MetricStrip.vue';
import ResourceDetailDrawer from '../components/ResourceDetailDrawer.vue';
import StatusBadge from '../components/StatusBadge.vue';
import { listResources, saveResource } from '../api/client';
import {
  StatusTone,
  applyStateLabel,
  applyStateTone,
  formatTime,
  listText,
  releaseStrategyLabel,
  resourcePhaseLabel,
  resourcePhaseTone
} from '../utils/format';
import { notifyError, notifyInfo } from '../utils/feedback';
import { getGlobalNamespace, onGlobalNamespaceChange, setGlobalNamespace } from '../utils/namespace';

interface ResourceItem {
  metadata?: {
    uid?: string;
    name?: string;
    namespace?: string;
    labels?: Record<string, string>;
    createdAt?: string | number;
    updatedAt?: string | number;
  };
  spec?: {
    version?: string;
    strategy?: string;
    displayName?: string;
    description?: string;
    ownerTeam?: string;
    environment?: string;
    trafficTier?: string;
    configShard?: string;
    host?: string;
    domains?: string[];
    projectRef?: {
      name?: string;
    };
    type?: string;
    anonymousAllowed?: boolean;
    protocol?: string;
    loadBalance?: string;
    hosts?: string[];
    endpoints?: Array<{
      host?: string;
      port?: number;
      weight?: number;
    }>;
    path?: {
      value?: string;
      stripPrefix?: boolean;
    };
    methods?: string[];
    upstreamRef?: {
      name?: string;
    };
    healthCheck?: {
      enabled?: boolean;
      path?: string;
    };
    routeRefs?: Array<{
      name?: string;
    }>;
    stableUpstreamRef?: {
      name?: string;
    };
    candidateUpstreamRef?: {
      name?: string;
    };
    trafficSplits?: Array<{
      target?: string;
      weight?: number;
      color?: string;
      upstreamRef?: {
        name?: string;
      };
    }>;
    rateLimit?: {
      enabled?: boolean;
      requestsPerSecond?: number;
    };
    retry?: {
      enabled?: boolean;
      maxAttempts?: number;
    };
    colorRules?: Array<{
      key?: string;
      match?: string;
      color?: string;
    }>;
    routes?: unknown[];
    upstreams?: unknown[];
    policies?: unknown[];
    targetNodeRefs?: unknown[];
    configHash?: string;
    severity?: string;
    source?: string;
    reason?: string;
    message?: string;
    count?: number;
    traceId?: string;
    firstObservedAt?: string | number;
    lastObservedAt?: string | number;
    involvedObject?: {
      kind?: string;
      namespace?: string;
      name?: string;
    };
  };
  status?: {
    phase?: string;
    applyState?: string;
    nodePhase?: string;
    currentPublishedVersion?: string;
    latestReleaseVersion?: string;
    healthyEndpointCount?: number;
    unhealthyEndpointCount?: number;
    archived?: boolean;
  };
}

const props = defineProps<{
  resourceType: string;
  title: string;
  description: string;
  initialNamespace?: string;
  inlineOnly?: boolean;
}>();

const namespace = ref(props.initialNamespace ?? getGlobalNamespace('default'));
const keyword = ref('');
const loading = ref(false);
const error = ref('');
const items = ref<ResourceItem[]>([]);
const selectedItem = ref<ResourceItem | null>(null);
const namespaceOptions = ref<Array<{ label: string; value: string }>>([{ label: 'default', value: 'default' }]);
const formOpen = ref(false);
const formError = ref('');
const saving = ref(false);
const editingName = ref('');
const resourceForm = reactive<Record<string, any>>({});
const relationOptions = reactive<Record<string, Array<{ label: string; value: string }>>>({
  projects: [],
  upstreams: [],
  routes: [],
  teams: [],
  environments: [],
  configShards: [],
  ingressDomains: []
});
const router = useRouter();
let unsubscribeNamespace: (() => void) | null = null;

const editableResources: Record<string, { label: string; description: string; optionTitle: string; optionNote: string }> = {
  projects: {
    label: '项目',
    description: '保存 GatewayProject，用于隔离路由、上游、策略和发布',
    optionTitle: '项目属性',
    optionNote: '域名和说明可后续继续补充'
  },
  routes: {
    label: '路由',
    description: '保存 GatewayRoute，描述入口匹配和默认上游',
    optionTitle: '匹配与转发',
    optionNote: '路由发布后由 proxy 运行'
  },
  upstreams: {
    label: '上游',
    description: '保存 Upstream，描述后端端点、协议、负载均衡和健康检查',
    optionTitle: '端点与健康',
    optionNote: '多端点后续可继续扩展成明细表'
  },
  'traffic-policies': {
    label: '治理策略',
    description: '保存 TrafficPolicy，承载限流、重试、熔断和染色',
    optionTitle: '治理能力',
    optionNote: '只填写开启能力相关参数'
  },
  'release-policies': {
    label: '发布策略',
    description: '保存 ReleasePolicy，承载蓝绿、灰度、权重和候选上游',
    optionTitle: '切流规则',
    optionNote: '稳定上游与候选上游从已创建上游中选择'
  },
  'auth-policies': {
    label: '认证策略',
    description: '保存 AuthPolicy，描述项目或路由的认证要求',
    optionTitle: '认证方式',
    optionNote: '密钥和 OAuth 细节后续由专门凭据资源承载'
  }
};
const editableResource = computed(() => editableResources[props.resourceType]);

const versionColumnTitle = computed(() => {
  if (props.resourceType === 'published-configs') {
    return '版本';
  }
  if (props.resourceType === 'events') {
    return '来源';
  }
  return '关联';
});
const emptyTitle = computed(() => {
  if (props.resourceType === 'projects') {
    return '还没有项目接入';
  }
  if (props.resourceType === 'upstreams') {
    return '还没有上游服务';
  }
  return '暂无数据';
});
const emptyMessage = computed(() =>
  props.resourceType === 'projects'
    ? '从接入向导创建项目、路由、上游和发布策略，保存后这里会展示完整资源。'
    : props.resourceType === 'upstreams'
      ? '项目接入时填写稳定上游或候选上游后，这里会展示转发目标、端点和健康状态。'
      : '当前筛选条件下没有资源，可以调整命名空间或先完成项目接入。'
);

const upstreamMetrics = computed(() => {
  const endpointCount = items.value.reduce((total, item) => total + (item.spec?.endpoints?.length || 0), 0);
  const unhealthy = items.value.reduce((total, item) => total + (item.status?.unhealthyEndpointCount || 0), 0);
  const modes = new Set(items.value.map((item) => item.spec?.loadBalance).filter(Boolean));
  return [
    { label: '上游服务', value: String(items.value.length), note: '可被路由转发的服务' },
    { label: '服务端点', value: String(endpointCount), note: 'host:port' },
    { label: '异常端点', value: String(unhealthy), note: unhealthy ? '需要排查健康检查' : '暂无异常' },
    { label: '负载方式', value: String(modes.size || 0), note: Array.from(modes).map(loadBalanceLabel).join(', ') || '-' }
  ];
});

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
    const [page] = await Promise.all([
      listResources<ResourceItem>(props.resourceType, namespace.value),
      loadRelationOptions()
    ]);
    items.value = page.items;
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载失败';
  } finally {
    loading.value = false;
  }
}

async function loadNamespaces() {
  try {
    const page = await listResources<ResourceItem>('namespaces', 'system', 100);
    const options = page.items
      .map((item) => item.metadata?.name)
      .filter((name): name is string => Boolean(name))
      .map((name) => ({ label: name, value: name }));
    namespaceOptions.value = ensureDefaultNamespace(options);
  } catch {
    namespaceOptions.value = ensureDefaultNamespace(namespaceOptions.value);
  }
}

async function refresh() {
  await load();
  if (error.value) {
    notifyError(`${props.title}刷新失败`, error.value);
    return;
  }
}

function openDetail(item: ResourceItem) {
  selectedItem.value = item;
}

function startCreate() {
  resetResourceForm();
  formOpen.value = true;
}

function startEdit(item: ResourceItem) {
  resetResourceForm();
  editingName.value = item.metadata?.name || '';
  resourceForm.name = editingName.value;
  resourceForm.projectName = item.spec?.projectRef?.name || '';
  resourceForm.displayName = item.spec?.displayName || '';
  resourceForm.ownerTeam = item.spec?.ownerTeam || '';
  resourceForm.environment = item.spec?.environment || '';
  resourceForm.configShard = item.spec?.configShard || '';
  resourceForm.description = item.spec?.description || '';
  resourceForm.hosts = (item.spec?.hosts || item.spec?.domains || []).join(',');
  resourceForm.path = item.spec?.path?.value || '';
  resourceForm.methods = (item.spec?.methods || []).join(',');
  resourceForm.stripPrefix = Boolean(item.spec?.path?.stripPrefix);
  resourceForm.upstreamName = item.spec?.upstreamRef?.name || '';
  resourceForm.protocol = item.spec?.protocol || 'HTTP';
  resourceForm.loadBalance = item.spec?.loadBalance || 'ROUND_ROBIN';
  resourceForm.endpointHost = item.spec?.endpoints?.[0]?.host || '';
  resourceForm.endpointPort = item.spec?.endpoints?.[0]?.port || '';
  resourceForm.endpointWeight = item.spec?.endpoints?.[0]?.weight || 100;
  resourceForm.healthCheckEnabled = Boolean(item.spec?.healthCheck?.enabled);
  resourceForm.healthPath = item.spec?.healthCheck?.path || '';
  resourceForm.rateLimitEnabled = Boolean(item.spec?.rateLimit?.enabled);
  resourceForm.requestsPerSecond = item.spec?.rateLimit?.requestsPerSecond || '';
  resourceForm.retryEnabled = Boolean(item.spec?.retry?.enabled);
  resourceForm.maxAttempts = item.spec?.retry?.maxAttempts || '';
  resourceForm.colorHeader = item.spec?.colorRules?.[0]?.key || '';
  resourceForm.colorValue = item.spec?.colorRules?.[0]?.color || item.spec?.trafficSplits?.find((split) => split.color)?.color || '';
  resourceForm.strategy = item.spec?.strategy || 'CANARY';
  resourceForm.routeName = item.spec?.routeRefs?.[0]?.name || '';
  resourceForm.stableUpstreamName = item.spec?.stableUpstreamRef?.name || '';
  resourceForm.candidateUpstreamName = item.spec?.candidateUpstreamRef?.name || '';
  resourceForm.candidateWeight = item.spec?.trafficSplits?.find((split) => split.target === 'candidate')?.weight ?? '';
  resourceForm.authType = item.spec?.type || 'NONE';
  resourceForm.anonymousAllowed = Boolean(item.spec?.anonymousAllowed);
  formOpen.value = true;
}

function closeForm() {
  formOpen.value = false;
  resetResourceForm();
}

async function saveCurrent() {
  formError.value = '';
  try {
    validateResourceForm();
    saving.value = true;
    await saveResource(props.resourceType, namespace.value || 'default', String(resourceForm.name), buildResource());
    await load();
    closeForm();
    notifyInfo(`${editableResource.value?.label || '资源'}已保存`, String(resourceForm.name));
  } catch (err) {
    formError.value = err instanceof Error ? err.message : '保存失败';
  } finally {
    saving.value = false;
  }
}

function resetResourceForm() {
  Object.keys(resourceForm).forEach((key) => delete resourceForm[key]);
  Object.assign(resourceForm, {
    name: '',
    projectName: '',
    protocol: 'HTTP',
    loadBalance: 'ROUND_ROBIN',
    endpointWeight: 100,
    healthCheckEnabled: false,
    rateLimitEnabled: false,
    retryEnabled: false,
    strategy: 'CANARY',
    candidateWeight: 10,
    authType: 'NONE',
    anonymousAllowed: false
  });
  editingName.value = '';
  formError.value = '';
}

function validateResourceForm() {
  if (!String(resourceForm.name || '').trim()) {
    throw new Error('请填写资源名称');
  }
  if (props.resourceType !== 'projects' && !resourceForm.projectName) {
    throw new Error('请选择所属项目');
  }
  if (props.resourceType === 'routes' && (!resourceForm.hosts || !resourceForm.path || !resourceForm.upstreamName)) {
    throw new Error('请填写入口域名、路径前缀和默认上游');
  }
  if (props.resourceType === 'upstreams' && (!resourceForm.endpointHost || !resourceForm.endpointPort)) {
    throw new Error('请填写上游端点地址和端口');
  }
}

function buildResource() {
  return {
    metadata: {
      name: String(resourceForm.name),
      namespace: namespace.value || 'default'
    },
    spec: buildSpec()
  };
}

function buildSpec() {
  if (props.resourceType === 'projects') {
    return {
      displayName: resourceForm.displayName || undefined,
      description: resourceForm.description || undefined,
      ownerTeam: resourceForm.ownerTeam || undefined,
      environment: resourceForm.environment || undefined,
      configShard: resourceForm.configShard || undefined,
      domains: splitCsv(resourceForm.hosts)
    };
  }
  if (props.resourceType === 'routes') {
    return {
      projectRef: refOf('GATEWAY_PROJECT', resourceForm.projectName),
      protocols: ['HTTP'],
      hosts: splitCsv(resourceForm.hosts),
      path: {
        type: 'Prefix',
        value: resourceForm.path,
        stripPrefix: Boolean(resourceForm.stripPrefix)
      },
      methods: splitCsv(resourceForm.methods),
      upstreamRef: refOf('UPSTREAM', resourceForm.upstreamName)
    };
  }
  if (props.resourceType === 'upstreams') {
    return {
      projectRef: refOf('GATEWAY_PROJECT', resourceForm.projectName),
      protocol: resourceForm.protocol || 'HTTP',
      loadBalance: resourceForm.loadBalance || 'ROUND_ROBIN',
      endpoints: [{
        host: resourceForm.endpointHost,
        port: Number(resourceForm.endpointPort),
        weight: Number(resourceForm.endpointWeight || 100)
      }],
      healthCheck: {
        enabled: Boolean(resourceForm.healthCheckEnabled),
        path: resourceForm.healthPath || undefined
      }
    };
  }
  if (props.resourceType === 'traffic-policies') {
    const colorRules = resourceForm.colorHeader && resourceForm.colorValue
      ? [{
          source: 'HEADER',
          key: resourceForm.colorHeader,
          match: resourceForm.colorValue,
          color: resourceForm.colorValue,
          propagateHeaders: { 'X-Traffic-Color': resourceForm.colorValue }
        }]
      : [];
    return {
      projectRef: refOf('GATEWAY_PROJECT', resourceForm.projectName),
      retry: {
        enabled: Boolean(resourceForm.retryEnabled),
        maxAttempts: resourceForm.maxAttempts ? Number(resourceForm.maxAttempts) : undefined
      },
      rateLimit: {
        enabled: Boolean(resourceForm.rateLimitEnabled),
        requestsPerSecond: resourceForm.requestsPerSecond ? Number(resourceForm.requestsPerSecond) : undefined
      },
      colorRules
    };
  }
  if (props.resourceType === 'release-policies') {
    const stable = resourceForm.stableUpstreamName;
    const candidate = resourceForm.candidateUpstreamName;
    return {
      projectRef: refOf('GATEWAY_PROJECT', resourceForm.projectName),
      routeRefs: resourceForm.routeName ? [refOf('GATEWAY_ROUTE', resourceForm.routeName)] : [],
      strategy: resourceForm.strategy || 'CANARY',
      stableUpstreamRef: stable ? refOf('UPSTREAM', stable) : undefined,
      candidateUpstreamRef: candidate ? refOf('UPSTREAM', candidate) : undefined,
      trafficSplits: [
        stable ? { target: 'stable', upstreamRef: refOf('UPSTREAM', stable), weight: 100 - Number(resourceForm.candidateWeight || 0) } : null,
        candidate ? { target: 'candidate', upstreamRef: refOf('UPSTREAM', candidate), weight: Number(resourceForm.candidateWeight || 0), color: resourceForm.colorValue || undefined } : null
      ].filter(Boolean)
    };
  }
  if (props.resourceType === 'auth-policies') {
    return {
      projectRef: refOf('GATEWAY_PROJECT', resourceForm.projectName),
      type: resourceForm.authType || 'NONE',
      anonymousAllowed: Boolean(resourceForm.anonymousAllowed)
    };
  }
  return {};
}

function refOf(kind: string, name: string) {
  return {
    kind,
    namespace: namespace.value || 'default',
    name
  };
}

function splitCsv(value: unknown) {
  return String(value || '')
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean);
}

async function loadRelationOptions() {
  if (!editableResource.value) {
    return;
  }
  const [projectPage, upstreamPage, routePage, teamPage, environmentPage, configShardPage, ingressDomainPage] = await Promise.all([
    listResources<ResourceItem>('projects', namespace.value, 200).catch(() => ({ items: [] })),
    listResources<ResourceItem>('upstreams', namespace.value, 200).catch(() => ({ items: [] })),
    listResources<ResourceItem>('routes', namespace.value, 200).catch(() => ({ items: [] })),
    listResources<ResourceItem>('teams', 'system', 200).catch(() => ({ items: [] })),
    listResources<ResourceItem>('environments', 'system', 200).catch(() => ({ items: [] })),
    listResources<ResourceItem>('config-shards', 'system', 200).catch(() => ({ items: [] })),
    listResources<ResourceItem>('ingress-domains', 'system', 200).catch(() => ({ items: [] }))
  ]);
  relationOptions.projects = relationList(projectPage.items);
  relationOptions.upstreams = relationList(upstreamPage.items);
  relationOptions.routes = relationList(routePage.items);
  relationOptions.teams = relationList(teamPage.items);
  relationOptions.environments = relationList(environmentPage.items);
  relationOptions.configShards = relationList(configShardPage.items);
  relationOptions.ingressDomains = relationList(ingressDomainPage.items, 'host');
}

function relationList(resources: ResourceItem[], valueKey: 'name' | 'host' = 'name') {
  return resources
    .map((item) => ({
      value: valueKey === 'host' ? item.spec?.host || item.metadata?.name || '' : item.metadata?.name || '',
      label: [item.spec?.displayName || item.metadata?.name, item.spec?.host, item.spec?.projectRef?.name]
        .filter(Boolean)
        .join(' / ')
    }))
    .filter((item) => item.value);
}

function summaryText(item: ResourceItem) {
  if (props.resourceType === 'projects') {
    return [
      item.spec?.displayName,
      item.spec?.ownerTeam,
      item.spec?.environment,
      listText(item.spec?.domains, '')
    ].filter(Boolean).join(' / ') || '-';
  }
  if (props.resourceType === 'routes') {
    return [
      item.spec?.path?.value,
      item.spec?.upstreamRef?.name ? `上游 ${item.spec.upstreamRef.name}` : '',
      item.spec?.path?.stripPrefix ? '剥离前缀' : ''
    ].filter(Boolean).join(' / ') || '-';
  }
  if (props.resourceType === 'published-configs') {
    return `${item.spec?.routes?.length ?? 0} 路由 / ${item.spec?.upstreams?.length ?? 0} 上游 / ${item.spec?.policies?.length ?? 0} 策略`;
  }
  if (props.resourceType === 'release-policies') {
    const splits = item.spec?.trafficSplits
      ?.map((split) => `${split.target || split.upstreamRef?.name || '-'} ${split.weight ?? 0}%${split.color ? ` ${split.color}` : ''}`)
      .join(' / ');
    return `${releaseStrategyLabel(item.spec?.strategy)}${splits ? ` / ${splits}` : ''}`;
  }
  if (props.resourceType === 'traffic-policies') {
    const parts = [];
    if (item.spec?.rateLimit?.enabled) {
      parts.push(`限流 ${item.spec.rateLimit.requestsPerSecond || '-'} RPS`);
    }
    if (item.spec?.retry?.enabled) {
      parts.push(`重试 ${item.spec.retry.maxAttempts || '-'} 次`);
    }
    if (item.spec?.colorRules?.length) {
      parts.push(`染色 ${item.spec.colorRules.length} 条`);
    }
    return parts.join(' / ') || '治理策略';
  }
  if (props.resourceType === 'events') {
    return [
      item.spec?.reason,
      item.spec?.message,
      item.spec?.involvedObject?.name ? `关联 ${item.spec.involvedObject.name}` : ''
    ].filter(Boolean).join(' / ') || '-';
  }
  if (item.spec?.strategy) {
    return releaseStrategyLabel(item.spec.strategy);
  }
  if (item.spec?.type) {
    return `认证 ${item.spec.type}`;
  }
  if (item.spec?.loadBalance) {
    const endpoints = item.spec.endpoints?.map((endpoint) => `${endpoint.host}:${endpoint.port}`).join(', ');
    return `${item.spec.loadBalance} / ${item.spec.endpoints?.length ?? 0} 端点${endpoints ? ` / ${endpoints}` : ''}`;
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

function versionText(item: ResourceItem) {
  if (props.resourceType === 'projects') {
    return item.spec?.configShard || item.metadata?.labels?.['gatepilot.io/config-shard'] || '-';
  }
  if (props.resourceType === 'routes') {
    return item.spec?.projectRef?.name || '-';
  }
  if (props.resourceType === 'upstreams') {
    return item.spec?.projectRef?.name || '-';
  }
  if (props.resourceType === 'published-configs') {
    return item.spec?.version || '-';
  }
  if (props.resourceType === 'events') {
    return [item.spec?.source, item.spec?.count ? `${item.spec.count} 次` : ''].filter(Boolean).join(' / ') || '-';
  }
  return item.spec?.version || item.status?.currentPublishedVersion || item.status?.latestReleaseVersion || item.spec?.projectRef?.name || '-';
}

function resourceStatus(item: ResourceItem): { label: string; tone: StatusTone } {
  if (props.resourceType === 'published-configs') {
    return {
      label: applyStateLabel(item.status?.applyState),
      tone: applyStateTone(item.status?.applyState)
    };
  }
  if (props.resourceType === 'upstreams') {
    const healthy = item.status?.healthyEndpointCount;
    const unhealthy = item.status?.unhealthyEndpointCount;
    if (healthy !== undefined || unhealthy !== undefined) {
      return {
        label: `健康 ${healthy ?? 0} / 异常 ${unhealthy ?? 0}`,
        tone: unhealthy ? 'warning' : 'success'
      };
    }
  }
  if (props.resourceType === 'events') {
    const severity = item.spec?.severity;
    if (severity === 'ERROR') {
      return { label: item.status?.archived ? '错误已归档' : '错误', tone: 'danger' };
    }
    if (severity === 'WARNING') {
      return { label: item.status?.archived ? '告警已归档' : '告警', tone: 'warning' };
    }
    return { label: item.status?.archived ? '信息已归档' : '信息', tone: 'info' };
  }
  const fallback = props.resourceType === 'projects' ? '已保存' : '已配置';
  return {
    label: resourcePhaseLabel(item.status?.phase || item.status?.nodePhase, fallback),
    tone: item.status?.phase || item.status?.nodePhase ? resourcePhaseTone(item.status?.phase || item.status?.nodePhase) : 'info'
  };
}

function rowActions(item: ResourceItem) {
  if (props.inlineOnly) {
    return [];
  }
  const name = item.metadata?.name;
  const itemNamespace = item.metadata?.namespace || namespace.value || 'default';
  if (!name) {
    return [];
  }
  if (props.resourceType === 'projects') {
    return [
      {
        label: '路由',
        run: () => navigate({ path: '/traffic/routing/route-catalog', query: { namespace: itemNamespace, projectName: name } }, '已进入项目路由')
      },
      {
        label: '诊断',
        run: () =>
          navigate({
            path: '/operations/route-diagnostics/request-workbench',
            query: {
              namespace: itemNamespace,
              projectName: name,
              host: item.spec?.domains?.[0] || '',
              path: '/api'
            }
          }, '已带入项目诊断参数')
      }
    ];
  }
  if (props.resourceType === 'published-configs') {
    return [
      {
        label: '路由',
        run: () =>
          navigate({
            path: '/traffic/routing/route-catalog',
            query: {
              namespace: itemNamespace,
              projectName: item.spec?.projectRef?.name || '',
              version: item.spec?.version || '',
              configShard: item.spec?.configShard || ''
            }
          }, '已进入发布路由目录')
      }
    ];
  }
  if (props.resourceType === 'upstreams') {
    const projectName = item.spec?.projectRef?.name || '';
    return [
      {
        label: '路由',
        run: () =>
          navigate({
            path: '/traffic/routing/route-catalog',
            query: {
              namespace: itemNamespace,
              projectName
            }
          }, '已进入关联路由')
      },
      {
        label: '诊断',
        run: () =>
          navigate({
            path: '/operations/route-diagnostics/request-workbench',
            query: {
              namespace: itemNamespace,
              projectName,
              path: '/api'
            }
          }, '已带入上游诊断参数')
      }
    ];
  }
  return [];
}

function protocolLabel(value?: string) {
  const labels: Record<string, string> = {
    HTTP: 'HTTP',
    HTTPS: 'HTTPS'
  };
  return labels[value || ''] || value || 'HTTP';
}

function loadBalanceLabel(value?: string) {
  const labels: Record<string, string> = {
    ROUND_ROBIN: '轮询',
    WEIGHTED_ROUND_ROBIN: '按权重轮询',
    RANDOM: '随机',
    LEAST_REQUEST: '最少请求',
    SPRING_CLOUD_LOADBALANCER: 'Spring LoadBalancer'
  };
  return labels[value || ''] || value || '默认负载';
}

function endpointSummary(item: ResourceItem) {
  const count = item.spec?.endpoints?.length || 0;
  return count ? `${count} 个端点` : '暂无端点';
}

function endpointPreview(item: ResourceItem) {
  const endpoints = item.spec?.endpoints || [];
  if (endpoints.length === 0) {
    return '保存上游端点后可转发';
  }
  return endpoints.slice(0, 2).map((endpoint) => `${endpoint.host}:${endpoint.port}${endpoint.weight ? ` 权重 ${endpoint.weight}` : ''}`).join('，');
}

function resourceTime(item: ResourceItem) {
  if (props.resourceType === 'events') {
    return item.spec?.lastObservedAt || item.spec?.firstObservedAt || item.metadata?.updatedAt || item.metadata?.createdAt;
  }
  return item.metadata?.updatedAt || item.metadata?.createdAt;
}

function navigate(target: Parameters<typeof router.push>[0], message: string) {
  notifyInfo(message);
  void router.push(target);
}

function ensureDefaultNamespace(options: Array<{ label: string; value: string }>) {
  const values = new Set(options.map((item) => item.value));
  if (!values.has('default')) {
    return [{ label: 'default', value: 'default' }, ...options];
  }
  return options;
}

onMounted(async () => {
  unsubscribeNamespace = onGlobalNamespaceChange((value) => {
    if (namespace.value !== value) {
      namespace.value = value;
    }
  });
  await loadNamespaces();
  await load();
});
onUnmounted(() => {
  unsubscribeNamespace?.();
});
watch(() => [props.resourceType, namespace.value], load);
watch(namespace, (value) => {
  setGlobalNamespace(value);
});
</script>
