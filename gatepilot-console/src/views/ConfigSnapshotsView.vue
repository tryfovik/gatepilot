<template>
  <div class="page-stack">
    <section class="content-panel">
      <div class="panel-header">
        <div>
          <h2>版本记录</h2>
          <p>每次发布都会留下一个版本。先选一个版本做对比起点，再点另一个版本查看变化。</p>
        </div>
        <button class="ghost-button" type="button" @click="load">
          <RefreshCw :size="16" />
          刷新
        </button>
      </div>

      <form class="filterbar diff-form" @submit.prevent="load">
        <select v-model="namespace" class="select-input">
          <option value="">全部命名空间</option>
          <option value="default">default</option>
          <option v-for="item in namespaceOptions" :key="item" :value="item">{{ item }}</option>
        </select>
        <input v-model="projectName" class="search-input" placeholder="项目名称（可选）" />
        <input v-model="configShard" class="search-input" placeholder="配置分片（可选）" />
        <button class="ghost-button" type="submit">
          <Search :size="16" />
          查询
        </button>
      </form>

      <MetricStrip :items="snapshotMetrics" />

      <div class="release-steps">
        <div
          v-for="step in snapshotSteps"
          :key="step.title"
          class="release-step-card"
          :class="{ 'release-step-card--active': step.active, 'release-step-card--done': step.done }"
        >
          <span>{{ step.index }}</span>
          <div>
            <strong>{{ step.title }}</strong>
            <small>{{ step.note }}</small>
          </div>
        </div>
      </div>

      <div v-if="selectedBase" class="selected-baseline">
        <div>
          <span>对比起点</span>
          <strong>{{ selectedBase.version }}</strong>
          <small>{{ selectedBase.projectName || '-' }} / {{ selectedBase.configShard || '默认分片' }}</small>
        </div>
        <button class="ghost-button" type="button" @click="clearBase">重新选择</button>
      </div>

      <div v-if="loading" class="state-box">正在加载...</div>
      <div v-else-if="error" class="state-box state-box--error">{{ error }}</div>
      <div v-else-if="items.length === 0" class="empty-state">
        <div class="empty-state-title">暂无配置快照</div>
        <p>发布成功后，controller-manager 会生成快照，后续可用于对比和回滚。</p>
      </div>

      <table v-else class="resource-table">
        <thead>
          <tr>
            <th>版本</th>
            <th>项目</th>
            <th>分片</th>
            <th>规模</th>
            <th>采集时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in items" :key="`${item.namespace}-${item.version}-${item.configShard || 'default'}`">
            <td>
              <div class="resource-name">{{ item.version || '-' }}</div>
              <div class="resource-subtitle">{{ item.releaseId || '未关联发布请求' }}</div>
            </td>
            <td>{{ item.projectName || '-' }}</td>
            <td>{{ item.configShard || '默认分片' }}</td>
            <td>{{ item.routeCount }} 路由 / {{ item.upstreamCount }} 上游 / {{ item.policyCount }} 策略 / {{ item.targetNodeCount }} 节点</td>
            <td>{{ formatTime(item.capturedAt) }}</td>
            <td>
              <div class="table-actions">
                <button class="table-action" type="button" @click="setBase(item)">
                  {{ isBase(item) ? '已选中' : '选作起点' }}
                </button>
                <button class="table-action" type="button" :disabled="!canCompare(item)" @click="compareWith(item)">
                  和它对比
                </button>
                <button
                  class="table-action table-action--danger"
                  type="button"
                  :disabled="item.rollbackAllowed === false"
                  @click="rollbackTo(item)"
                >
                  回滚
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>

      <div v-if="timelineItems.length" class="snapshot-timeline">
      <h3>最近版本轨迹</h3>
        <TimelineList :items="timelineItems" />
      </div>
    </section>

    <section class="content-panel">
      <div class="panel-header">
        <div>
          <h2>对比结果</h2>
          <p>{{ diffTitle }}</p>
        </div>
        <div class="panel-actions">
          <StatusBadge :label="diffStatusLabel" :tone="diffStatusTone" />
          <button v-if="diff" class="ghost-button" type="button" @click="confirmClear = true">
            <Trash2 :size="16" />
            清空
          </button>
        </div>
      </div>

      <div v-if="diffLoading" class="state-box">正在处理...</div>
      <div v-else-if="diffError" class="state-box state-box--error">{{ diffError }}</div>
      <div v-else-if="rollbackNotice" class="state-box state-box--compact">{{ rollbackNotice }}</div>
      <div v-else-if="!diff" class="state-box">先选择一个版本作为起点，再点击另一个版本的“和它对比”</div>
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

    <ConfirmDialog
      :open="confirmClear"
      title="清空对比结果"
      message="只清空当前页面上的版本差异，不会修改已发布快照。"
      confirm-text="清空"
      @close="confirmClear = false"
      @confirm="clearDiff"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue';
import { RefreshCw, Search, Trash2 } from 'lucide-vue-next';
import ConfirmDialog from '../components/ConfirmDialog.vue';
import MetricStrip from '../components/MetricStrip.vue';
import StatusBadge from '../components/StatusBadge.vue';
import TimelineList from '../components/TimelineList.vue';
import {
  ConfigDiffResponse,
  ConfigSnapshotSummaryResponse,
  createRollback,
  diffConfigSnapshots,
  listConfigSnapshotSummaries,
  listResources
} from '../api/client';
import { formatTime, shortHash } from '../utils/format';
import { notifyError, notifyInfo, notifySuccess, notifyWarning } from '../utils/feedback';
import { getGlobalNamespace, onGlobalNamespaceChange, setGlobalNamespace } from '../utils/namespace';

interface NamespaceResource {
  metadata?: {
    name?: string;
  };
}

const namespace = ref(getGlobalNamespace('default'));
const projectName = ref('');
const configShard = ref('');
const loading = ref(false);
const error = ref('');
const diffLoading = ref(false);
const diffError = ref('');
const rollbackNotice = ref('');
const confirmClear = ref(false);
const items = ref<ConfigSnapshotSummaryResponse[]>([]);
const diff = ref<ConfigDiffResponse | null>(null);
const selectedBase = ref<ConfigSnapshotSummaryResponse | null>(null);
const selectedTarget = ref<ConfigSnapshotSummaryResponse | null>(null);
const namespaceOptions = ref<string[]>([]);
let unsubscribeNamespace: (() => void) | null = null;

const snapshotMetrics = computed(() => {
  const latest = items.value[0];
  const routeCount = items.value.reduce((total, item) => total + item.routeCount, 0);
  const upstreamCount = items.value.reduce((total, item) => total + item.upstreamCount, 0);
  return [
    { label: '快照数', value: String(items.value.length), note: '当前筛选结果' },
    { label: '最新版本', value: latest?.version || '-', note: latest?.configShard || '默认分片' },
    { label: '路由规模', value: String(routeCount), note: '当前页累计' },
    { label: '上游规模', value: String(upstreamCount), note: '当前页累计' }
  ];
});

const snapshotSteps = computed(() => [
  {
    index: '1',
    title: '找到项目',
    note: projectName.value || '可按项目筛选，也可以直接看全部',
    active: items.value.length === 0,
    done: items.value.length > 0
  },
  {
    index: '2',
    title: '选择起点',
    note: selectedBase.value?.version || '点“选作起点”',
    active: items.value.length > 0 && !selectedBase.value,
    done: Boolean(selectedBase.value)
  },
  {
    index: '3',
    title: '对比变化',
    note: selectedTarget.value?.version || '再点另一个版本',
    active: Boolean(selectedBase.value && !diff.value),
    done: Boolean(diff.value)
  },
  {
    index: '4',
    title: '决定回滚',
    note: '需要时直接点回滚',
    active: false,
    done: false
  }
]);

const timelineItems = computed(() =>
  items.value.slice(0, 6).map((item) => ({
    key: `${item.namespace}-${item.version}-${item.configShard || 'default'}`,
    title: `${item.version || '-'} / ${item.projectName || '未关联项目'}`,
    meta: `${formatTime(item.capturedAt)} · ${item.configShard || '默认分片'}`,
    message: `${item.routeCount} 路由，${item.upstreamCount} 上游，${item.policyCount} 策略`,
    tone: item.rollbackAllowed === false ? ('neutral' as const) : ('info' as const)
  }))
);

const diffTitle = computed(() => {
  if (!selectedBase.value || !selectedTarget.value) {
    return '差异来自两个已发布快照，不需要手动填写版本号。';
  }
  return `${selectedBase.value.version} -> ${selectedTarget.value.version}`;
});

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
    const page = await listConfigSnapshotSummaries({
      namespace: namespace.value || undefined,
      projectName: projectName.value || undefined,
      configShard: configShard.value || undefined,
      limit: 50
    });
    items.value = page.items;
    if (selectedBase.value && !items.value.some((item) => sameSnapshot(item, selectedBase.value))) {
      selectedBase.value = null;
    }
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载失败';
    notifyError('版本记录加载失败', error.value);
  } finally {
    loading.value = false;
  }
}

async function compareWith(item: ConfigSnapshotSummaryResponse) {
  if (!selectedBase.value || !selectedBase.value.version || !item.version) {
    diffError.value = '请先选择基线快照';
    return;
  }
  const effectiveNamespace = item.namespace || selectedBase.value.namespace || namespace.value;
  if (!effectiveNamespace) {
    diffError.value = '快照缺少命名空间，不能对比';
    return;
  }
  diffLoading.value = true;
  diffError.value = '';
  rollbackNotice.value = '';
  selectedTarget.value = item;
  try {
    diff.value = await diffConfigSnapshots(
      effectiveNamespace,
      selectedBase.value.version,
      item.version,
      item.configShard || selectedBase.value.configShard || undefined
    );
    notifySuccess('对比完成', diff.value.changed ? '两个版本存在变化' : '两个版本没有变化');
  } catch (err) {
    diff.value = null;
    diffError.value = err instanceof Error ? err.message : '对比失败';
    notifyError('对比失败', diffError.value);
  } finally {
    diffLoading.value = false;
  }
}

async function rollbackTo(item: ConfigSnapshotSummaryResponse) {
  diffError.value = '';
  rollbackNotice.value = '';
  if (!item.namespace || !item.projectName || !item.version) {
    diffError.value = '快照缺少命名空间、项目或版本，不能回滚';
    return;
  }
  if (!window.confirm(`确认回滚 ${item.projectName} 到版本 ${item.version}？`)) {
    notifyWarning('已取消回滚', item.version);
    return;
  }
  diffLoading.value = true;
  try {
    const result = await createRollback({
      namespace: item.namespace,
      projectName: item.projectName,
      targetVersion: item.version,
      configShard: item.configShard || undefined,
      description: `从快照页面回滚到 ${item.version}`,
      createdBy: 'console'
    });
    rollbackNotice.value = `回滚请求已创建：${result.releaseId}`;
    notifySuccess('回滚请求已创建', result.releaseId);
  } catch (err) {
    diffError.value = err instanceof Error ? err.message : '回滚失败';
    notifyError('回滚失败', diffError.value);
  } finally {
    diffLoading.value = false;
  }
}

function setBase(item: ConfigSnapshotSummaryResponse) {
  selectedBase.value = item;
  diffError.value = '';
  rollbackNotice.value = '';
  notifyInfo('已选择对比起点', item.version || '-');
}

function clearBase() {
  selectedBase.value = null;
  clearDiff();
  notifyInfo('已清空对比起点');
}

function canCompare(item: ConfigSnapshotSummaryResponse) {
  return Boolean(selectedBase.value?.version && item.version && !isBase(item));
}

function isBase(item: ConfigSnapshotSummaryResponse) {
  return Boolean(selectedBase.value && sameSnapshot(item, selectedBase.value));
}

function sameSnapshot(left: ConfigSnapshotSummaryResponse, right: ConfigSnapshotSummaryResponse | null) {
  return Boolean(right
    && left.namespace === right.namespace
    && left.version === right.version
    && (left.configShard || '') === (right.configShard || ''));
}

function clearDiff() {
  diff.value = null;
  selectedTarget.value = null;
  diffError.value = '';
  confirmClear.value = false;
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

async function loadNamespaces() {
  try {
    const page = await listResources<NamespaceResource>('namespaces', 'system', 200);
    namespaceOptions.value = page.items
      .map((item) => item.metadata?.name)
      .filter((item): item is string => Boolean(item && item !== 'default'));
  } catch {
    namespaceOptions.value = [];
  }
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
watch(namespace, () => {
  setGlobalNamespace(namespace.value);
  void load();
});
</script>
