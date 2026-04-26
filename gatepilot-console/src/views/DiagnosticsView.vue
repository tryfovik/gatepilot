<template>
  <div class="page-stack">
    <section class="content-panel">
      <div class="panel-header">
        <div>
          <h2>路由诊断</h2>
          <p>像调接口一样构造一次请求，查看已发布配置下的路由、认证、染色、上游和治理判断。</p>
        </div>
        <StatusBadge :label="resultStatusLabel" :tone="resultStatusTone" />
      </div>

      <form class="diagnostic-workbench" @submit.prevent="run">
        <div class="diagnostic-context">
          <label>
            <span>命名空间</span>
            <input v-model.trim="form.namespace" class="search-input" placeholder="default" />
          </label>
          <label>
            <span>项目</span>
            <input v-model.trim="form.projectName" class="search-input" placeholder="可选" />
          </label>
          <label>
            <span>Host</span>
            <input v-model.trim="form.host" class="search-input" placeholder="api.example.com" />
          </label>
          <label>
            <span>版本</span>
            <input v-model.trim="form.version" class="search-input" placeholder="默认最新发布" />
          </label>
          <label>
            <span>配置分片</span>
            <input v-model.trim="form.configShard" class="search-input" placeholder="default" />
          </label>
          <label>
            <span>客户端 IP</span>
            <input v-model.trim="form.remoteAddress" class="search-input" placeholder="127.0.0.1" />
          </label>
        </div>

        <div class="request-line">
          <div class="method-tabs" role="group" aria-label="请求方法">
            <button
              v-for="method in methods"
              :key="method"
              class="method-tab"
              :class="{ 'method-tab--active': form.method === method }"
              type="button"
              @click="form.method = method"
            >
              {{ method }}
            </button>
          </div>
          <input v-model.trim="requestTarget" class="search-input request-target" placeholder="/api/orders?id=1" />
          <button class="primary-button" type="submit" :disabled="loading">
            <Play :size="16" />
            发送诊断
          </button>
        </div>

        <div class="request-editor">
          <div class="tabbar">
            <button
              v-for="tab in paramTabs"
              :key="tab.key"
              class="tab-button"
              :class="{ 'tab-button--active': activeParamTab === tab.key }"
              type="button"
              @click="activeParamTab = tab.key"
            >
              {{ tab.label }}
              <span class="tab-count">{{ enabledRowCount(tab.key) }}</span>
            </button>
          </div>

          <table class="resource-table compact-table">
            <thead>
              <tr>
                <th>启用</th>
                <th>名称</th>
                <th>值</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in activeRows" :key="row.id">
                <td>
                  <input v-model="row.enabled" class="check-input" type="checkbox" aria-label="启用参数" />
                </td>
                <td>
                  <input v-model.trim="row.name" class="table-input" placeholder="name" />
                </td>
                <td>
                  <input v-model.trim="row.value" class="table-input" placeholder="value" />
                </td>
                <td>
                  <button class="table-action table-action--danger" type="button" @click="removeRow(row.id)">
                    <X :size="14" />
                    删除
                  </button>
                </td>
              </tr>
            </tbody>
          </table>

          <button class="ghost-button add-param-button" type="button" @click="addRow(activeParamTab)">
            <Plus :size="16" />
            新增参数
          </button>
        </div>
      </form>

      <div v-if="loading" class="state-box state-box--compact">正在诊断...</div>
      <div v-else-if="error" class="state-box state-box--error state-box--compact">{{ error }}</div>
    </section>

    <MetricStrip v-if="result" :items="resultMetrics" />

    <section v-if="result" class="content-panel">
      <div class="panel-header">
        <div>
          <h2>命中链路</h2>
          <p>{{ result.method || '-' }} {{ result.host || '-' }}{{ result.path || '-' }}</p>
        </div>
        <div class="topbar-actions">
          <StatusBadge :label="result.matched ? '已命中' : '未命中'" :tone="result.matched ? 'success' : 'danger'" />
          <span class="mono-cell">{{ shortHash(result.configHash) }}</span>
        </div>
      </div>

      <div class="diagnostic-flow">
        <div v-for="step in flowSteps" :key="step.title" class="diagnostic-step" :class="`diagnostic-step--${step.tone}`">
          <component :is="step.icon" :size="18" />
          <div>
            <div class="diagnostic-step-title">{{ step.title }}</div>
            <div class="diagnostic-step-value">{{ step.value }}</div>
            <div class="diagnostic-step-note">{{ step.note }}</div>
          </div>
        </div>
      </div>
    </section>

    <section v-if="result" class="content-panel">
      <div class="panel-header">
        <div>
          <h2>诊断详情</h2>
          <p>{{ result.version || '-' }} / {{ result.configShard || '默认分片' }}</p>
        </div>
      </div>

      <div class="diagnostic-grid">
        <div class="diagnostic-block">
          <h3>路由</h3>
          <div class="key-value-grid">
            <span>名称</span>
            <strong>{{ result.route?.name || '-' }}</strong>
            <span>路径</span>
            <strong>{{ result.route?.path || '-' }}</strong>
            <span>Host</span>
            <strong>{{ listText(result.route?.hosts, '任意 Host') }}</strong>
            <span>上游</span>
            <strong>{{ result.route?.upstreamName || '-' }}</strong>
            <span>策略</span>
            <strong>{{ listText(result.route?.policyNames, '未绑定') }}</strong>
          </div>
        </div>

        <div class="diagnostic-block">
          <h3>访问控制</h3>
          <div class="key-value-grid">
            <span>方法</span>
            <StatusBadge :label="result.access?.methodAllowed ? '允许' : '拒绝'" :tone="result.access?.methodAllowed ? 'success' : 'danger'" />
            <span>允许方法</span>
            <strong>{{ listText(result.access?.allowedMethods, '全部') }}</strong>
            <span>认证</span>
            <StatusBadge :label="authLabel" :tone="authTone" />
            <span>认证策略</span>
            <strong>{{ listText(result.access?.authPolicyNames, '未绑定') }}</strong>
          </div>
        </div>

        <div class="diagnostic-block">
          <h3>染色与发布</h3>
          <div class="key-value-grid">
            <span>颜色</span>
            <strong>{{ result.traffic?.color || '-' }}</strong>
            <span>来源</span>
            <strong>{{ colorSourceLabel(result.traffic?.source) }}</strong>
            <span>Header</span>
            <strong>{{ result.traffic?.headerName || '-' }}</strong>
            <span>发布目标</span>
            <strong>{{ releaseTargetLabel(result.traffic?.releaseTarget) }}</strong>
          </div>
        </div>

        <div class="diagnostic-block">
          <h3>上游</h3>
          <div class="key-value-grid">
            <span>名称</span>
            <StatusBadge :label="result.upstream?.name || '未配置'" :tone="result.upstream?.available ? 'success' : 'danger'" />
            <span>协议</span>
            <strong>{{ result.upstream?.protocol || '-' }}</strong>
            <span>负载均衡</span>
            <strong>{{ result.upstream?.loadBalance || '-' }}</strong>
            <span>端点</span>
            <strong>{{ result.upstream?.endpointCount ?? '-' }}</strong>
            <span>健康检查</span>
            <strong>{{ result.upstream?.healthCheckEnabled ? '已启用' : '未启用' }}</strong>
          </div>
        </div>

        <div class="diagnostic-block">
          <h3>治理策略</h3>
          <div class="key-value-grid">
            <span>重试</span>
            <strong>{{ retryText }}</strong>
            <span>限流</span>
            <strong>{{ rateLimitText }}</strong>
            <span>参数规则</span>
            <strong>{{ result.governance?.rateLimit?.paramRuleCount ?? '-' }}</strong>
            <span>熔断</span>
            <strong>{{ circuitBreakerText }}</strong>
            <span>Fallback</span>
            <strong>{{ result.governance?.circuitBreaker?.fallbackMessage || '-' }}</strong>
          </div>
        </div>

        <div class="diagnostic-block">
          <h3>提示</h3>
          <div v-if="warnings.length === 0" class="state-box state-box--compact">暂无告警</div>
          <div v-else class="warning-list">
            <div v-for="warning in warnings" :key="warning" class="warning-item">
              <AlertTriangle :size="16" />
              <span>{{ warning }}</span>
            </div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import {
  AlertTriangle,
  CheckCircle2,
  CircleSlash,
  GitBranch,
  LockKeyhole,
  Palette,
  Play,
  Plus,
  Route,
  ShieldCheck,
  SlidersHorizontal,
  X
} from 'lucide-vue-next';
import MetricStrip from '../components/MetricStrip.vue';
import StatusBadge from '../components/StatusBadge.vue';
import { RouteDiagnosticsResponse, diagnoseRoute } from '../api/client';
import { StatusTone, listText, shortHash } from '../utils/format';
import { notifyError, notifyInfo } from '../utils/feedback';
import { getGlobalNamespace, onGlobalNamespaceChange, setGlobalNamespace } from '../utils/namespace';

type ParamTab = 'headers' | 'query' | 'cookies';

interface ParamRow {
  id: number;
  name: string;
  value: string;
  enabled: boolean;
}

const methods = ['GET', 'POST', 'PUT', 'DELETE', 'PATCH'] as const;
const paramTabs: Array<{ key: ParamTab; label: string }> = [
  { key: 'headers', label: 'Headers' },
  { key: 'query', label: 'Query' },
  { key: 'cookies', label: 'Cookies' }
];

const currentRoute = useRoute();
const form = reactive({
  namespace: queryText('namespace', getGlobalNamespace('default')),
  projectName: queryText('projectName'),
  host: queryText('host'),
  version: queryText('version'),
  configShard: queryText('configShard'),
  method: queryText('method', 'GET'),
  remoteAddress: queryText('remoteAddress')
});

const requestTarget = ref(queryText('path', '/api'));
const activeParamTab = ref<ParamTab>('headers');
const paramRows = reactive<Record<ParamTab, ParamRow[]>>({
  headers: [
    { id: 1, name: 'x-gatepilot-color', value: 'green', enabled: false }
  ],
  query: [
    { id: 2, name: 'debug', value: 'true', enabled: false }
  ],
  cookies: [
    { id: 3, name: 'gatepilot_color', value: 'green', enabled: false }
  ]
});
const nextRowId = ref(4);
const loading = ref(false);
const error = ref('');
const result = ref<RouteDiagnosticsResponse | null>(null);
let unsubscribeNamespace: (() => void) | null = null;

const activeRows = computed(() => paramRows[activeParamTab.value]);
const warnings = computed(() => result.value?.warnings || []);

const resultStatusLabel = computed(() => {
  if (!result.value) {
    return '待诊断';
  }
  return result.value.matched ? '已命中' : '未命中';
});

const resultStatusTone = computed<StatusTone>(() => {
  if (!result.value) {
    return 'neutral';
  }
  return result.value.matched ? 'success' : 'danger';
});

const resultMetrics = computed(() => {
  if (!result.value) {
    return [];
  }
  return [
    { label: '命中状态', value: result.value.matched ? '命中' : '未命中', note: result.value.route?.name || '未匹配路由' },
    { label: '访问控制', value: result.value.access?.methodAllowed ? '通过' : '拒绝', note: authLabel.value },
    { label: '发布目标', value: releaseTargetLabel(result.value.traffic?.releaseTarget), note: result.value.traffic?.color || '默认流量' },
    { label: '上游状态', value: result.value.upstream?.available ? '可用' : '不可用', note: result.value.upstream?.name || '未配置' }
  ];
});

const authLabel = computed(() => {
  if (!result.value?.access?.authenticationRequired) {
    return '不需要认证';
  }
  return result.value.access.anonymousAllowed ? '允许匿名' : '需要认证';
});

const authTone = computed<StatusTone>(() => {
  if (!result.value?.access?.authenticationRequired) {
    return 'success';
  }
  return result.value.access.anonymousAllowed ? 'info' : 'warning';
});

const retryText = computed(() => {
  const retry = result.value?.governance?.retry;
  if (!retry?.enabled) {
    return '未启用';
  }
  return `${retry.maxAttempts || '-'} 次 / ${listText(retry.statuses?.map(String), '默认状态码')}`;
});

const rateLimitText = computed(() => {
  const rateLimit = result.value?.governance?.rateLimit;
  if (!rateLimit?.enabled) {
    return '未启用';
  }
  return `${rateLimit.requestsPerSecond || '-'} RPS / burst ${rateLimit.burstCapacity || '-'}`;
});

const circuitBreakerText = computed(() => {
  const circuitBreaker = result.value?.governance?.circuitBreaker;
  if (!circuitBreaker?.enabled) {
    return '未启用';
  }
  return `窗口 ${circuitBreaker.slidingWindowSize || '-'} / 失败率 ${circuitBreaker.failureRateThreshold || '-'}%`;
});

const flowSteps = computed(() => {
  const current = result.value;
  if (!current) {
    return [];
  }
  return [
    {
      title: '请求入口',
      value: `${current.method || '-'} ${current.host || '-'}${current.path || '-'}`,
      note: current.namespace || form.namespace || 'default',
      tone: 'info' as const,
      icon: GitBranch
    },
    {
      title: '路由匹配',
      value: current.route?.name || '未匹配路由',
      note: current.route?.path || '-',
      tone: current.matched ? ('success' as const) : ('danger' as const),
      icon: current.matched ? Route : CircleSlash
    },
    {
      title: '访问控制',
      value: current.access?.methodAllowed ? '方法允许' : '方法拒绝',
      note: authLabel.value,
      tone: current.access?.methodAllowed ? ('success' as const) : ('danger' as const),
      icon: current.access?.authenticationRequired ? LockKeyhole : ShieldCheck
    },
    {
      title: '染色发布',
      value: releaseTargetLabel(current.traffic?.releaseTarget),
      note: [colorSourceLabel(current.traffic?.source), current.traffic?.color].filter(Boolean).join(' / ') || '-',
      tone: 'info' as const,
      icon: Palette
    },
    {
      title: '治理上游',
      value: current.upstream?.name || '未配置上游',
      note: current.upstream?.available ? '上游可用' : '上游不可用',
      tone: current.upstream?.available ? ('success' as const) : ('danger' as const),
      icon: current.upstream?.available ? CheckCircle2 : SlidersHorizontal
    }
  ];
});

async function run() {
  loading.value = true;
  error.value = '';
  try {
    const target = parseRequestTarget(requestTarget.value);
    result.value = await diagnoseRoute({
      namespace: form.namespace || undefined,
      projectName: form.projectName || undefined,
      version: form.version || undefined,
      configShard: form.configShard || undefined,
      method: form.method,
      host: form.host || undefined,
      path: target.path,
      headers: buildMap(paramRows.headers),
      query: mergeMaps(target.query, buildMap(paramRows.query)),
      cookies: buildMap(paramRows.cookies),
      remoteAddress: form.remoteAddress || undefined
    });
  } catch (err) {
    result.value = null;
    error.value = err instanceof Error ? err.message : '诊断失败';
    notifyError('诊断失败', error.value);
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  unsubscribeNamespace = onGlobalNamespaceChange((namespace) => {
    if (form.namespace !== namespace) {
      form.namespace = namespace;
    }
  });
  if (Object.keys(currentRoute.query).length > 0) {
    notifyInfo('已带入诊断参数', '可以直接发送诊断或继续补充 Header、Query、Cookie');
  }
});

onUnmounted(() => {
  unsubscribeNamespace?.();
});

watch(() => form.namespace, (namespace) => {
  setGlobalNamespace(namespace);
});

watch(() => currentRoute.query, () => {
  applyRouteQuery();
  notifyInfo('诊断参数已更新', '来源页面带入了新的项目或路由参数');
});

function applyRouteQuery() {
  form.namespace = queryText('namespace', form.namespace || 'default');
  form.projectName = queryText('projectName', form.projectName);
  form.host = queryText('host', form.host);
  form.version = queryText('version', form.version);
  form.configShard = queryText('configShard', form.configShard);
  form.method = queryText('method', form.method);
  form.remoteAddress = queryText('remoteAddress', form.remoteAddress);
  requestTarget.value = queryText('path', requestTarget.value);
}

function queryText(key: string, fallback = '') {
  const value = currentRoute.query[key];
  return Array.isArray(value) ? value[0] || fallback : value || fallback;
}

function addRow(tab: ParamTab) {
  paramRows[tab].push({
    id: nextRowId.value,
    name: '',
    value: '',
    enabled: true
  });
  nextRowId.value += 1;
}

function removeRow(rowId: number) {
  paramRows[activeParamTab.value] = paramRows[activeParamTab.value].filter((row) => row.id !== rowId);
  if (paramRows[activeParamTab.value].length === 0) {
    addRow(activeParamTab.value);
  }
}

function enabledRowCount(tab: ParamTab) {
  return paramRows[tab].filter((row) => row.enabled && row.name).length;
}

function buildMap(rows: ParamRow[], ignoredNames: string[] = []) {
  const ignored = new Set(ignoredNames.map((item) => item.toLowerCase()));
  return rows.reduce<Record<string, string[]>>((target, row) => {
    if (!row.enabled || !row.name || ignored.has(row.name.toLowerCase())) {
      return target;
    }
    target[row.name] = [...(target[row.name] || []), row.value];
    return target;
  }, {});
}

function mergeMaps(left: Record<string, string[]>, right: Record<string, string[]>) {
  return Object.entries(right).reduce<Record<string, string[]>>((target, [key, values]) => {
    target[key] = [...(target[key] || []), ...values];
    return target;
  }, { ...left });
}

function parseRequestTarget(value: string) {
  const normalized = value.startsWith('/') ? value : `/${value}`;
  const url = new URL(normalized, 'http://gatepilot.local');
  const query: Record<string, string[]> = {};
  url.searchParams.forEach((item, key) => {
    query[key] = [...(query[key] || []), item];
  });
  return {
    path: url.pathname || '/',
    query
  };
}

function colorSourceLabel(value?: string) {
  const labels: Record<string, string> = {
    HEADER: '请求头',
    RULE: '染色规则',
    WEIGHT: '权重分流',
    DEFAULT: '默认值'
  };
  return labels[value || ''] || '未知';
}

function releaseTargetLabel(value?: string) {
  const labels: Record<string, string> = {
    STABLE: '稳定版本',
    CANDIDATE: '候选版本',
    BLUE: '蓝环境',
    GREEN: '绿环境',
    SHADOW: '影子流量'
  };
  return labels[value || ''] || value || '-';
}
</script>
