<template>
  <div class="page-stack">
    <section class="content-panel">
      <div class="panel-header">
        <div>
          <h2>运行审计</h2>
          <p>按 TraceId、节点、路由和执行结果查询 proxy 上报的业务请求审计。</p>
        </div>
        <button class="ghost-button" type="button" @click="() => queryFirstPage(true)">
          <RefreshCw :size="16" />
          刷新
        </button>
      </div>

      <form class="filterbar audit-form" @submit.prevent="queryFirstPage(true)">
        <select v-model="filters.namespace" class="select-input compact-input">
          <option value="">全部命名空间</option>
          <option value="default">default</option>
          <option v-for="item in namespaceOptions" :key="item" :value="item">{{ item }}</option>
        </select>
        <select v-model="filters.projectName" class="select-input compact-input">
          <option value="">全部项目</option>
          <option v-for="item in projectOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
        </select>
        <select v-model="filters.routeId" class="select-input compact-input">
          <option value="">全部路由</option>
          <option v-for="item in routeOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
        </select>
        <select v-model="filters.nodeId" class="select-input compact-input">
          <option value="">全部节点</option>
          <option v-for="item in nodeOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
        </select>
        <input v-model="filters.traceId" class="search-input compact-input" placeholder="tid / TraceId" />
        <input v-model="filters.startedAt" class="search-input compact-input" type="datetime-local" aria-label="开始时间" />
        <input v-model="filters.endedAt" class="search-input compact-input" type="datetime-local" aria-label="结束时间" />
        <select v-model="filters.outcome" class="select-input compact-input">
          <option value="">全部结果</option>
          <option value="SUCCESS">成功</option>
          <option value="REJECTED">拒绝</option>
          <option value="FALLBACK">降级</option>
          <option value="ERROR">异常</option>
        </select>
        <select v-model.number="pageLimit" class="select-input compact-input">
          <option :value="20">20 条</option>
          <option :value="50">50 条</option>
          <option :value="100">100 条</option>
        </select>
        <button class="primary-button" type="submit">
          <Search :size="16" />
          查询
        </button>
      </form>
    </section>

    <MetricStrip :items="metrics" />

    <section class="content-panel">
      <div v-if="loading" class="state-box">正在加载...</div>
      <div v-else-if="error" class="state-box state-box--error">{{ error }}</div>
      <div v-else-if="items.length === 0" class="state-box">暂无审计记录</div>

      <table v-else class="resource-table">
        <thead>
          <tr>
            <th>请求</th>
            <th>项目 / 路由</th>
            <th>上游</th>
            <th>结果</th>
            <th>状态</th>
            <th>耗时</th>
            <th>染色</th>
            <th>TraceId</th>
            <th>操作</th>
            <th>时间</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in items" :key="item.id || `${item.traceId}-${item.occurredAt}`">
            <td>
              <div class="resource-name">{{ item.method || '-' }} {{ item.path || '-' }}</div>
              <div class="resource-subtitle">{{ item.host || item.clientIp || '-' }}</div>
            </td>
            <td>
              <div class="compact-stack">
                <span>{{ item.projectName || '-' }}</span>
                <span class="resource-subtitle">{{ item.routeId || '-' }}</span>
              </div>
            </td>
            <td>{{ item.upstreamName || item.upstreamUri || '-' }}</td>
            <td>
              <StatusBadge :label="outcomeLabel(item.outcome)" :tone="outcomeTone(item)" />
            </td>
            <td>{{ item.status ?? '-' }}</td>
            <td>{{ latencyText(item.latencyMillis) }}</td>
            <td>{{ item.trafficColor || '-' }}</td>
            <td class="mono-cell">{{ item.traceId || '-' }}</td>
            <td>
              <button class="table-action" type="button" @click="openDetail(item)">详情</button>
            </td>
            <td>{{ formatTime(item.occurredAt) }}</td>
          </tr>
        </tbody>
      </table>

      <div v-if="items.length > 0" class="pagination-bar">
        <span>第 {{ currentPage }} 页，当前 {{ items.length }} 条<span v-if="total > 0"> / 共 {{ total }} 条</span></span>
        <div class="table-actions">
          <button class="ghost-button" type="button" :disabled="!canPrev || loading" @click="prevPage">上一页</button>
          <button class="ghost-button" type="button" :disabled="!nextCursor || loading" @click="nextPage">下一页</button>
        </div>
      </div>
    </section>

    <section v-if="drilldownTitle" class="content-panel">
      <div class="panel-header">
        <div>
          <h2>{{ drilldownTitle }}</h2>
          <p>{{ drilldownItems.length }} 条记录，点击详情可以查看完整审计字段。</p>
        </div>
        <button class="ghost-button" type="button" @click="clearDrilldown">收起明细</button>
      </div>
      <div v-if="drilldownItems.length === 0" class="empty-state">
        <div class="empty-state-title">暂无匹配记录</div>
        <p>当前页没有命中该指标的记录，可以调整过滤条件或翻页继续查看。</p>
      </div>
      <table v-else class="resource-table compact-table">
        <thead>
          <tr>
            <th>请求</th>
            <th>项目</th>
            <th>路由</th>
            <th>结果</th>
            <th>状态</th>
            <th>耗时</th>
            <th>原因</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in drilldownItems" :key="`drill-${item.id || item.traceId}-${item.occurredAt}`">
            <td>
              <div class="resource-name">{{ item.method || '-' }} {{ item.path || '-' }}</div>
              <div class="resource-subtitle">{{ item.host || item.clientIp || '-' }}</div>
            </td>
            <td>{{ item.projectName || '-' }}</td>
            <td>{{ item.routeId || '-' }}</td>
            <td><StatusBadge :label="outcomeLabel(item.outcome)" :tone="outcomeTone(item)" /></td>
            <td>{{ item.status ?? '-' }}</td>
            <td>{{ latencyText(item.latencyMillis) }}</td>
            <td>{{ item.reason || item.error || '-' }}</td>
            <td>
              <button class="table-action" type="button" @click="openDetail(item)">详情</button>
            </td>
          </tr>
        </tbody>
      </table>
    </section>

    <ResourceDetailDrawer
      :open="Boolean(selectedItem)"
      :title="selectedItem?.traceId || '审计详情'"
      :subtitle="selectedItem ? `${selectedItem.method || '-'} ${selectedItem.path || '-'}` : '-'"
      :payload="selectedItem"
      @close="selectedItem = null"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue';
import { RefreshCw, Search } from 'lucide-vue-next';
import MetricStrip from '../components/MetricStrip.vue';
import ResourceDetailDrawer from '../components/ResourceDetailDrawer.vue';
import StatusBadge from '../components/StatusBadge.vue';
import { RuntimeAuditRecord, listResources, listRuntimeAudits } from '../api/client';
import { StatusTone, formatTime, numberText } from '../utils/format';
import { notifyError } from '../utils/feedback';
import { getGlobalNamespace, onGlobalNamespaceChange, setGlobalNamespace } from '../utils/namespace';

interface NamespaceResource {
  metadata?: {
    name?: string;
  };
  spec?: {
    displayName?: string;
    nodeId?: string;
    projectRef?: {
      name?: string;
    };
  };
}

const filters = reactive({
  namespace: getGlobalNamespace('default'),
  projectName: '',
  nodeId: '',
  routeId: '',
  traceId: '',
  outcome: '',
  startedAt: '',
  endedAt: ''
});

const loading = ref(false);
const error = ref('');
const items = ref<RuntimeAuditRecord[]>([]);
const namespaces = ref<NamespaceResource[]>([]);
const pageLimit = ref(50);
const nextCursor = ref('');
const currentCursor = ref('');
const cursorStack = ref<string[]>([]);
const currentPage = ref(1);
const total = ref(0);
const activeDrilldown = ref<'errors' | 'fallbacks' | 'slow' | ''>('');
const selectedItem = ref<RuntimeAuditRecord | null>(null);
const projects = ref<NamespaceResource[]>([]);
const routes = ref<NamespaceResource[]>([]);
const nodes = ref<NamespaceResource[]>([]);
let unsubscribeNamespace: (() => void) | null = null;

const namespaceOptions = computed(() =>
  namespaces.value
    .map((item) => item.metadata?.name)
    .filter((item): item is string => Boolean(item && item !== 'default'))
);

const canPrev = computed(() => cursorStack.value.length > 0);
const projectOptions = computed(() => optionList(projects.value));
const routeOptions = computed(() =>
  optionList(routes.value.filter((item) => !filters.projectName || item.spec?.projectRef?.name === filters.projectName))
);
const nodeOptions = computed(() =>
  nodes.value
    .map((item) => {
      const value = item.spec?.nodeId || item.metadata?.name || '';
      return {
        value,
        label: [item.spec?.displayName || value, item.metadata?.name && item.metadata.name !== value ? item.metadata.name : '']
          .filter(Boolean)
          .join(' / ')
      };
    })
    .filter((item) => item.value)
);

const metrics = computed(() => {
  const errorCount = items.value.filter((item) => outcomeTone(item) === 'danger').length;
  const fallbackCount = items.value.filter((item) => item.fallback).length;
  const avgLatency = average(items.value.map((item) => item.latencyMillis).filter((value): value is number => value !== undefined));
  return [
    { label: '审计数', value: String(items.value.length), note: '当前页' },
    { label: '异常请求', value: String(errorCount), note: 'ERROR / 5xx', onClick: () => showDrilldown('errors') },
    { label: 'Fallback', value: String(fallbackCount), note: '降级次数', onClick: () => showDrilldown('fallbacks') },
    { label: '慢请求', value: String(slowItems.value.length), note: '>= 1000ms', onClick: () => showDrilldown('slow') }
  ];
});

const slowItems = computed(() => items.value.filter((item) => (item.latencyMillis || 0) >= 1000));

const drilldownTitle = computed(() => {
  const labels = {
    errors: '异常请求明细',
    fallbacks: 'Fallback 明细',
    slow: '慢请求明细',
    '': ''
  };
  return labels[activeDrilldown.value];
});

const drilldownItems = computed(() => {
  if (activeDrilldown.value === 'errors') {
    return items.value.filter((item) => outcomeTone(item) === 'danger');
  }
  if (activeDrilldown.value === 'fallbacks') {
    return items.value.filter((item) => item.fallback || item.outcome === 'FALLBACK');
  }
  if (activeDrilldown.value === 'slow') {
    return slowItems.value;
  }
  return [];
});

async function load(cursor = currentCursor.value) {
  loading.value = true;
  error.value = '';
  try {
    const page = await listRuntimeAudits({
      namespace: filters.namespace || undefined,
      nodeId: filters.nodeId || undefined,
      projectName: filters.projectName || undefined,
      routeId: filters.routeId || undefined,
      traceId: filters.traceId || undefined,
      outcome: filters.outcome || undefined,
      startedAt: dateTimeValue(filters.startedAt),
      endedAt: dateTimeValue(filters.endedAt),
      cursor: cursor || undefined,
      limit: pageLimit.value
    });
    items.value = page.items;
    nextCursor.value = page.nextCursor || '';
    total.value = page.total || 0;
    currentCursor.value = cursor || '';
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载失败';
  } finally {
    loading.value = false;
  }
}

async function loadNamespaces() {
  try {
    const [page, projectPage, routePage, nodePage] = await Promise.all([
      listResources<NamespaceResource>('namespaces', 'system', 200),
      listResources<NamespaceResource>('projects', filters.namespace || 'default', 200).catch(() => ({ items: [] })),
      listResources<NamespaceResource>('routes', filters.namespace || 'default', 200).catch(() => ({ items: [] })),
      listResources<NamespaceResource>('nodes', filters.namespace || 'default', 200).catch(() => ({ items: [] }))
    ]);
    namespaces.value = page.items;
    projects.value = projectPage.items;
    routes.value = routePage.items;
    nodes.value = nodePage.items;
  } catch {
    namespaces.value = [];
    projects.value = [];
    routes.value = [];
    nodes.value = [];
  }
}

async function queryFirstPage(showFeedback = true) {
  cursorStack.value = [];
  currentCursor.value = '';
  currentPage.value = 1;
  activeDrilldown.value = '';
  await load('');
  if (!showFeedback) {
    return;
  }
  if (error.value) {
    notifyError('审计查询失败', error.value);
    return;
  }
}

async function nextPage() {
  if (!nextCursor.value) {
    return;
  }
  cursorStack.value.push(currentCursor.value);
  currentPage.value += 1;
  await load(nextCursor.value);
}

async function prevPage() {
  if (!canPrev.value) {
    return;
  }
  const previousCursor = cursorStack.value.pop() || '';
  currentPage.value = Math.max(1, currentPage.value - 1);
  await load(previousCursor);
}

function outcomeTone(item: RuntimeAuditRecord): StatusTone {
  if (item.outcome === 'SUCCESS' && (item.status || 0) < 500) {
    return 'success';
  }
  if (item.fallback || item.outcome === 'FALLBACK') {
    return 'warning';
  }
  if (item.outcome === 'ERROR' || (item.status || 0) >= 500) {
    return 'danger';
  }
  if (item.outcome === 'REJECTED') {
    return 'danger';
  }
  return 'neutral';
}

function outcomeLabel(value?: string) {
  const labels: Record<string, string> = {
    SUCCESS: '成功',
    REJECTED: '拒绝',
    FALLBACK: '降级',
    ERROR: '异常',
    BLOCKED: '拦截'
  };
  return labels[value || ''] || value || '未知';
}

function latencyText(value?: number) {
  return value === undefined || value === null ? '-' : `${numberText(value, 0)}ms`;
}

function average(values: number[]) {
  if (values.length === 0) {
    return null;
  }
  const total = values.reduce((sum, value) => sum + value, 0);
  return Math.round(total / values.length);
}

function showDrilldown(type: 'errors' | 'fallbacks' | 'slow') {
  activeDrilldown.value = activeDrilldown.value === type ? '' : type;
}

function clearDrilldown() {
  activeDrilldown.value = '';
}

function openDetail(item: RuntimeAuditRecord) {
  selectedItem.value = item;
}

function optionList(resources: NamespaceResource[]) {
  return resources
    .map((item) => ({
      value: item.metadata?.name || '',
      label: [item.spec?.displayName || item.metadata?.name, item.spec?.projectRef?.name].filter(Boolean).join(' / ')
    }))
    .filter((item) => item.value);
}

function dateTimeValue(value: string) {
  if (!value) {
    return undefined;
  }
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? undefined : date.toISOString();
}

onMounted(async () => {
  unsubscribeNamespace = onGlobalNamespaceChange((namespace) => {
    if (filters.namespace !== namespace) {
      filters.namespace = namespace;
      void queryFirstPage(false);
    }
  });
  await Promise.all([loadNamespaces(), queryFirstPage(false)]);
});

onUnmounted(() => {
  unsubscribeNamespace?.();
});

watch(() => filters.namespace, (namespace) => {
  setGlobalNamespace(namespace);
  filters.projectName = '';
  filters.routeId = '';
  filters.nodeId = '';
  void loadNamespaces();
});

watch(() => filters.projectName, () => {
  filters.routeId = '';
});
</script>
