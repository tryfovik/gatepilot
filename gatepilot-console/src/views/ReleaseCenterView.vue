<template>
  <div class="page-stack">
    <section class="content-panel">
      <div class="panel-header">
        <div>
          <h2>发布中心</h2>
          <p>按“选项目、先检查、确认发布、看结果”的顺序推进配置，回滚直接从已发布版本发起。</p>
        </div>
        <div class="panel-actions">
          <StatusBadge :label="releaseStatusLabel" :tone="releaseStatusTone" />
          <button class="ghost-button" type="button" @click="loadAll">
            <RefreshCw :size="16" />
            刷新
          </button>
        </div>
      </div>

      <div class="release-picker">
        <label>
          <span>命名空间</span>
          <select v-model="namespace" class="select-input">
            <option value="">全部命名空间</option>
            <option value="default">default</option>
            <option v-for="item in namespaceOptions" :key="item" :value="item">{{ item }}</option>
          </select>
        </label>
        <label>
          <span>待发布项目</span>
          <select v-model="selectedProjectName" class="select-input">
            <option value="">请选择项目</option>
            <option v-for="item in projects" :key="projectSelectValue(item)" :value="projectSelectValue(item)">
              {{ projectLabel(item) }}
            </option>
          </select>
        </label>
        <label>
          <span>操作人</span>
          <input v-model="operator" class="search-input" />
        </label>
      </div>

      <div class="release-steps">
        <div
          v-for="step in releaseSteps"
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

      <div v-if="selectedProject" class="release-project-summary">
        <div class="summary-block">
          <div class="summary-title">项目</div>
          <div class="summary-value">{{ selectedProject.spec?.displayName || selectedProject.metadata?.name }}</div>
          <StatusBadge label="已保存" tone="info" />
        </div>
        <div class="summary-block">
          <div class="summary-title">配置分片</div>
          <div class="summary-value">{{ selectedConfigShard || '默认分片' }}</div>
          <span class="resource-subtitle">发布会从当前保存的资源生成运行配置</span>
        </div>
        <div class="summary-block">
          <div class="summary-title">当前版本</div>
          <div class="summary-value">{{ selectedProject.status?.currentPublishedVersion || '-' }}</div>
          <span class="resource-subtitle">last published</span>
        </div>
      </div>

      <div class="release-step-actions">
        <button class="ghost-button" type="button" :disabled="busy || !selectedProject" @click="runDryRun">
          <CheckCircle2 :size="16" />
          先检查
        </button>
        <button class="primary-button" type="button" :disabled="busy || !selectedProject || dryRun?.passed !== true" @click="runCreateRelease">
          <Rocket :size="16" />
          确认发布
        </button>
      </div>

      <div v-if="error" class="state-box state-box--error state-box--compact">{{ error }}</div>
      <div v-else-if="notice" class="state-box state-box--compact">{{ notice }}</div>
    </section>

    <MetricStrip v-if="dryRun" :items="dryRunMetrics" />

    <section v-if="dryRun" class="content-panel">
      <div class="panel-header">
        <div>
          <h2>发布校验</h2>
          <p>{{ dryRun.projectName }} / {{ dryRun.version }} / {{ dryRun.configShard || '默认分片' }}</p>
        </div>
        <StatusBadge :label="dryRun.passed ? '可以发布' : '需要处理'" :tone="dryRun.passed ? 'success' : 'danger'" />
      </div>
      <div v-if="dryRun.messages.length === 0" class="state-box state-box--compact">暂无校验告警</div>
      <div v-else class="warning-list">
        <div v-for="message in dryRun.messages" :key="`${message.reason}-${message.message}`" class="warning-item">
          <AlertTriangle :size="16" />
          <span>{{ message.level }} / {{ message.reason }}：{{ message.message }}</span>
        </div>
      </div>
    </section>

    <section v-if="releaseResult" class="content-panel">
      <div class="panel-header">
        <div>
          <h2>发布请求</h2>
          <p>系统会异步生成运行配置，并推送到目标节点。</p>
        </div>
        <StatusBadge :label="releaseResult.phase || 'PENDING'" tone="info" />
      </div>
      <div class="key-value-grid">
        <span>发布 ID</span>
        <strong>{{ releaseResult.releaseId }}</strong>
        <span>版本</span>
        <strong>{{ releaseResult.version }}</strong>
        <span>分片</span>
        <strong>{{ releaseResult.configShard || '默认分片' }}</strong>
        <span>创建时间</span>
        <strong>{{ formatTime(releaseResult.createdAt) }}</strong>
      </div>
    </section>

    <section v-if="releaseResult" class="content-panel">
      <div class="panel-header">
        <div>
          <h2>节点应用结果</h2>
          <p>按目标节点展示 apply 状态、已应用版本和失败原因。</p>
        </div>
        <div class="panel-actions">
          <StatusBadge :label="nodeApplySummaryLabel" :tone="nodeApplySummaryTone" />
          <button class="ghost-button" type="button" :disabled="releaseCatalogLoading" @click="loadReleaseCatalog">
            <RefreshCw :size="16" />
            刷新结果
          </button>
        </div>
      </div>

      <div v-if="releaseCatalogLoading" class="state-box state-box--compact">正在加载节点应用结果...</div>
      <div v-else-if="releaseCatalogError" class="state-box state-box--error state-box--compact">{{ releaseCatalogError }}</div>
      <div v-else-if="!releaseCatalog || releaseCatalog.nodeApplyResults.length === 0" class="empty-state">
        <div class="empty-state-title">暂无节点应用上报</div>
        <p>controller-manager 创建发布后，agent / proxy 会陆续上报应用结果，稍后刷新即可查看。</p>
      </div>
      <table v-else class="resource-table">
        <thead>
          <tr>
            <th>节点</th>
            <th>状态</th>
            <th>已应用版本</th>
            <th>可用区</th>
            <th>原因</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="node in releaseCatalog.nodeApplyResults" :key="node.nodeId">
            <td>{{ node.nodeId || '-' }}</td>
            <td>
              <StatusBadge :label="applyStateLabel(node.state)" :tone="applyStateTone(node.state)" />
            </td>
            <td>{{ node.appliedVersion || '-' }}</td>
            <td>{{ node.zone || '-' }}</td>
            <td>
              <div class="compact-stack">
                <span>{{ node.reason || '-' }}</span>
                <span v-if="node.message" class="resource-subtitle">{{ node.message }}</span>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </section>

    <section class="content-panel">
      <div class="panel-header">
        <div>
          <h2>已发布版本</h2>
          <p>选择一个历史版本即可回滚，不需要手填版本号。</p>
        </div>
      </div>

      <div class="filterbar">
        <input v-model="versionKeyword" class="search-input" placeholder="搜索版本、项目或分片" />
      </div>

      <div v-if="loading" class="state-box">正在加载...</div>
      <div v-else-if="versions.length === 0" class="empty-state">
        <div class="empty-state-title">暂无已发布版本</div>
        <p>项目发布成功后，这里会展示可回滚的快照版本。</p>
      </div>

      <table v-else class="resource-table">
        <thead>
          <tr>
            <th>版本</th>
            <th>项目</th>
            <th>分片</th>
            <th>资源摘要</th>
            <th>发布时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in filteredVersions" :key="`${item.namespace}-${item.projectName}-${item.version}-${item.configShard || ''}`">
            <td>
              <div class="resource-name">{{ item.version || '-' }}</div>
              <div class="resource-subtitle">{{ item.releaseId || '-' }}</div>
            </td>
            <td>{{ item.projectName || '-' }}</td>
            <td>{{ item.configShard || '默认分片' }}</td>
            <td>{{ item.routeCount }} 路由 / {{ item.upstreamCount }} 上游 / {{ item.policyCount }} 策略 / {{ item.targetNodeCount }} 节点</td>
            <td>{{ formatTime(item.capturedAt) }}</td>
            <td>
              <div class="table-actions">
                <button class="table-action" type="button" @click="selectVersion(item)">选择</button>
                <button class="table-action table-action--danger" type="button" :disabled="busy || !item.rollbackAllowed" @click="runRollback(item)">
                  回滚到此版本
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue';
import { AlertTriangle, CheckCircle2, RefreshCw, Rocket } from 'lucide-vue-next';
import MetricStrip from '../components/MetricStrip.vue';
import StatusBadge from '../components/StatusBadge.vue';
import {
  ConfigSnapshotSummaryResponse,
  CreateReleaseRequest,
  CreateRollbackRequest,
  ReleaseDryRunResponse,
  ReleaseResponse,
  RouteCatalogResponse,
  createRelease,
  createRollback,
  dryRunRelease,
  getRouteCatalog,
  listConfigSnapshotSummaries,
  listResources
} from '../api/client';
import { StatusTone, applyStateLabel, applyStateTone, formatTime } from '../utils/format';
import { notifyError, notifyInfo, notifySuccess, notifyWarning } from '../utils/feedback';
import { getGlobalNamespace, onGlobalNamespaceChange, setGlobalNamespace } from '../utils/namespace';

interface GatewayProject {
  metadata?: {
    name?: string;
    namespace?: string;
  };
  spec?: {
    displayName?: string;
    ownerTeam?: string;
    environment?: string;
    configShard?: string;
  };
  status?: {
    currentPublishedVersion?: string;
  };
}

interface NamespaceResource {
  metadata?: {
    name?: string;
  };
}

const namespace = ref(getGlobalNamespace('default'));
const selectedProjectName = ref('');
const operator = ref('console');
const versionKeyword = ref('');
const loading = ref(false);
const busy = ref(false);
const error = ref('');
const notice = ref('');
const dryRun = ref<ReleaseDryRunResponse | null>(null);
const releaseResult = ref<ReleaseResponse | null>(null);
const projects = ref<GatewayProject[]>([]);
const namespaces = ref<NamespaceResource[]>([]);
const versions = ref<ConfigSnapshotSummaryResponse[]>([]);
const releaseCatalog = ref<RouteCatalogResponse | null>(null);
const releaseCatalogLoading = ref(false);
const releaseCatalogError = ref('');
const releaseProjectName = ref('');
let unsubscribeNamespace: (() => void) | null = null;

const namespaceOptions = computed(() =>
  namespaces.value
    .map((item) => item.metadata?.name)
    .filter((item): item is string => Boolean(item && item !== 'default'))
);

const selectedProject = computed(() =>
  projects.value.find((item) => projectSelectValue(item) === selectedProjectName.value) || null
);

const selectedConfigShard = computed(() => selectedProject.value?.spec?.configShard || undefined);

const filteredVersions = computed(() => {
  const value = versionKeyword.value.trim().toLowerCase();
  const rows = selectedProjectName.value
    ? versions.value.filter((item) =>
        item.projectName === selectedProject.value?.metadata?.name
        && (item.namespace || namespace.value || 'default') === effectiveProjectNamespace()
      )
    : versions.value;
  if (!value) {
    return rows;
  }
  return rows.filter((item) => [
    item.version,
    item.projectName,
    item.configShard,
    item.releaseId
  ].filter(Boolean).join(' ').toLowerCase().includes(value));
});

const releaseStatusLabel = computed(() => {
  if (releaseResult.value) {
    return releaseResult.value.phase || '已创建';
  }
  if (dryRun.value) {
    return dryRun.value.passed ? '校验通过' : '校验未通过';
  }
  return selectedProject.value ? '待校验' : '请选择项目';
});

const releaseStatusTone = computed<StatusTone>(() => {
  if (releaseResult.value) {
    return 'info';
  }
  if (dryRun.value) {
    return dryRun.value.passed ? 'success' : 'danger';
  }
  return selectedProject.value ? 'warning' : 'neutral';
});

const dryRunMetrics = computed(() => [
  { label: '路由数', value: String(dryRun.value?.routeCount ?? 0), note: 'GatewayRoute' },
  { label: '上游数', value: String(dryRun.value?.upstreamCount ?? 0), note: 'Upstream' },
  { label: '策略数', value: String(dryRun.value?.policyCount ?? 0), note: 'Policy' },
  { label: '消息数', value: String(dryRun.value?.messages.length ?? 0), note: 'dry-run' }
]);

const releaseSteps = computed(() => [
  {
    index: '1',
    title: '选择项目',
    note: selectedProject.value ? selectedProject.value.metadata?.name || '已选择' : '先选一个待发布项目',
    active: !selectedProject.value,
    done: Boolean(selectedProject.value)
  },
  {
    index: '2',
    title: '检查配置',
    note: dryRun.value ? (dryRun.value.passed ? '可以发布' : '需要处理告警') : '检查路由、上游和策略',
    active: Boolean(selectedProject.value && !dryRun.value),
    done: dryRun.value?.passed === true
  },
  {
    index: '3',
    title: '确认发布',
    note: releaseResult.value ? '发布请求已创建' : '检查通过后再发布',
    active: dryRun.value?.passed === true && !releaseResult.value,
    done: Boolean(releaseResult.value)
  },
  {
    index: '4',
    title: '查看结果',
    note: releaseResult.value ? releaseResult.value.phase || '等待节点应用' : '节点应用状态会继续上报',
    active: Boolean(releaseResult.value),
    done: false
  }
]);

const nodeApplySummaryLabel = computed(() => {
  const nodes = releaseCatalog.value?.nodeApplyResults || [];
  if (releaseCatalogLoading.value) {
    return '加载中';
  }
  if (releaseCatalogError.value) {
    return '结果不可用';
  }
  if (nodes.length === 0) {
    return '等待上报';
  }
  const failed = nodes.filter((node) => node.state === 'FAILED').length;
  const applied = nodes.filter((node) => node.state === 'APPLIED').length;
  return failed > 0 ? `失败 ${failed} 个` : `已应用 ${applied}/${nodes.length}`;
});

const nodeApplySummaryTone = computed<StatusTone>(() => {
  const nodes = releaseCatalog.value?.nodeApplyResults || [];
  if (releaseCatalogError.value) {
    return 'warning';
  }
  if (nodes.some((node) => node.state === 'FAILED')) {
    return 'danger';
  }
  if (nodes.length > 0 && nodes.every((node) => node.state === 'APPLIED')) {
    return 'success';
  }
  return nodes.length > 0 ? 'info' : 'neutral';
});

onMounted(() => {
  unsubscribeNamespace = onGlobalNamespaceChange((value) => {
    if (namespace.value !== value) {
      namespace.value = value || 'default';
    }
  });
  void loadAll();
});

onUnmounted(() => {
  unsubscribeNamespace?.();
});

watch(namespace, async () => {
  setGlobalNamespace(namespace.value);
  selectedProjectName.value = '';
  dryRun.value = null;
  releaseResult.value = null;
  await loadProjectsAndVersions();
});

watch(selectedProjectName, () => {
  dryRun.value = null;
  releaseResult.value = null;
  releaseCatalog.value = null;
  releaseCatalogError.value = '';
  releaseProjectName.value = '';
});

async function loadAll() {
  loading.value = true;
  error.value = '';
  try {
    await Promise.all([loadNamespaces(), loadProjectsAndVersions()]);
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载失败';
    notifyError('发布数据加载失败', error.value);
  } finally {
    loading.value = false;
  }
}

async function loadNamespaces() {
  const page = await listResources<NamespaceResource>('namespaces', 'system', 200);
  namespaces.value = page.items;
}

async function loadProjectsAndVersions() {
  loading.value = true;
  error.value = '';
  try {
    const [projectPage, snapshotPage] = await Promise.all([
      listResources<GatewayProject>('projects', namespace.value, 200),
      listConfigSnapshotSummaries({ namespace: namespace.value, limit: 200 })
    ]);
    projects.value = projectPage.items;
    versions.value = snapshotPage.items;
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载失败';
  } finally {
    loading.value = false;
  }
}

async function runDryRun() {
  await run(async () => {
    dryRun.value = await dryRunRelease(releaseRequest());
    releaseResult.value = null;
    notice.value = dryRun.value.passed ? '发布校验通过，可以创建发布请求' : '发布校验未通过，请先处理告警';
  });
}

async function runCreateRelease() {
  await run(async () => {
    const projectName = selectedProject.value?.metadata?.name || '';
    if (!dryRun.value?.passed) {
      dryRun.value = await dryRunRelease(releaseRequest());
    }
    if (!dryRun.value.passed) {
      throw new Error('发布校验未通过，不能创建发布请求');
    }
    releaseResult.value = await createRelease(releaseRequest());
    releaseProjectName.value = projectName;
    notice.value = `发布请求已创建：${releaseResult.value.releaseId}`;
    await loadProjectsAndVersions();
    await loadReleaseCatalog();
  });
}

async function runRollback(item: ConfigSnapshotSummaryResponse) {
  if (!item.version || !item.projectName) {
    error.value = '快照缺少版本或项目名称，不能回滚';
    return;
  }
  if (!window.confirm(`确认回滚 ${item.projectName} 到版本 ${item.version}？`)) {
    notifyWarning('已取消回滚', item.version);
    return;
  }
  selectedProjectName.value = `${item.namespace || namespace.value || 'default'}/${item.projectName}`;
  await run(async () => {
    releaseResult.value = await createRollback(rollbackRequest(item));
    releaseProjectName.value = item.projectName || '';
    notice.value = `回滚请求已创建：${releaseResult.value.releaseId}`;
    await loadProjectsAndVersions();
    await loadReleaseCatalog();
  });
}

function selectVersion(item: ConfigSnapshotSummaryResponse) {
  if (item.projectName) {
    selectedProjectName.value = `${item.namespace || namespace.value || 'default'}/${item.projectName}`;
  }
  versionKeyword.value = item.version || '';
  notifyInfo('已选择历史版本', item.version || '-');
}

async function run(task: () => Promise<void>) {
  busy.value = true;
  error.value = '';
  notice.value = '';
  try {
    await task();
    if (notice.value) {
      notifySuccess('操作完成', notice.value);
    }
  } catch (err) {
    error.value = err instanceof Error ? err.message : '操作失败';
    notifyError('操作失败', error.value);
  } finally {
    busy.value = false;
  }
}

function releaseRequest(): CreateReleaseRequest {
  if (!selectedProject.value?.metadata?.name) {
    throw new Error('请先选择待发布项目');
  }
  return {
    namespace: effectiveProjectNamespace(),
    projectName: selectedProject.value.metadata.name,
    configShard: selectedConfigShard.value,
    description: `Console 发布 ${selectedProject.value.metadata.name}`,
    createdBy: operator.value || undefined
  };
}

function rollbackRequest(item: ConfigSnapshotSummaryResponse): CreateRollbackRequest {
  return {
    namespace: item.namespace || effectiveProjectNamespace() || 'default',
    projectName: item.projectName || '',
    targetVersion: item.version || '',
    configShard: item.configShard || undefined,
    description: `Console 回滚到 ${item.version}`,
    createdBy: operator.value || undefined
  };
}

async function loadReleaseCatalog() {
  if (!releaseResult.value) {
    return;
  }
  releaseCatalogLoading.value = true;
  releaseCatalogError.value = '';
  try {
    releaseCatalog.value = await getRouteCatalog({
      namespace: effectiveProjectNamespace(),
      projectName: releaseProjectName.value || selectedProject.value?.metadata?.name || '',
      version: releaseResult.value.version,
      configShard: releaseResult.value.configShard
    });
  } catch (err) {
    releaseCatalog.value = null;
    releaseCatalogError.value = err instanceof Error ? err.message : '节点应用结果加载失败';
  } finally {
    releaseCatalogLoading.value = false;
  }
}

function projectLabel(item: GatewayProject) {
  return [
    item.metadata?.namespace || namespace.value || 'default',
    item.spec?.displayName || item.metadata?.name,
    item.spec?.environment,
    item.spec?.configShard ? `分片 ${item.spec.configShard}` : ''
  ].filter(Boolean).join(' / ');
}

function projectSelectValue(item: GatewayProject) {
  return `${item.metadata?.namespace || namespace.value || 'default'}/${item.metadata?.name || ''}`;
}

function effectiveProjectNamespace() {
  return selectedProject.value?.metadata?.namespace || namespace.value || 'default';
}
</script>
