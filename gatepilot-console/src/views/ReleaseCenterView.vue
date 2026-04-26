<template>
  <div class="page-stack">
    <section class="content-panel">
      <div class="panel-header">
        <div>
          <h2>发布操作</h2>
          <p>创建发布请求、执行 dry-run 校验，或按快照版本发起回滚。</p>
        </div>
        <StatusBadge :label="releaseStatusLabel" :tone="releaseStatusTone" />
      </div>

      <form class="release-grid" @submit.prevent="runDryRun">
        <label>
          <span>命名空间</span>
          <input v-model="releaseForm.namespace" class="search-input" />
        </label>
        <label>
          <span>项目名称</span>
          <input v-model="releaseForm.projectName" class="search-input" />
        </label>
        <label>
          <span>配置分片</span>
          <input v-model="releaseForm.configShard" class="search-input" />
        </label>
        <label>
          <span>操作人</span>
          <input v-model="releaseForm.createdBy" class="search-input" />
        </label>
        <label class="wide-field">
          <span>发布说明</span>
          <input v-model="releaseForm.description" class="search-input" />
        </label>
        <div class="form-actions">
          <button class="ghost-button" type="submit" :disabled="busy">
            <CheckCircle2 :size="16" />
            dry-run
          </button>
          <button class="primary-button" type="button" :disabled="busy" @click="runCreateRelease">
            <Rocket :size="16" />
            创建发布
          </button>
        </div>
      </form>

      <div v-if="error" class="state-box state-box--error">{{ error }}</div>
      <div v-else-if="notice" class="state-box state-box--compact">{{ notice }}</div>
    </section>

    <MetricStrip v-if="dryRun" :items="dryRunMetrics" />

    <section v-if="dryRun" class="content-panel">
      <div class="panel-header">
        <div>
          <h2>发布校验</h2>
          <p>{{ dryRun.version }} / {{ dryRun.configShard || '默认分片' }}</p>
        </div>
        <StatusBadge :label="dryRun.passed ? '通过' : '未通过'" :tone="dryRun.passed ? 'success' : 'danger'" />
      </div>
      <div v-if="dryRun.messages.length === 0" class="state-box state-box--compact">暂无校验告警</div>
      <div v-else class="warning-list">
        <div v-for="message in dryRun.messages" :key="`${message.reason}-${message.message}`" class="warning-item">
          <AlertTriangle :size="16" />
          <span>{{ message.level }} / {{ message.reason }}：{{ message.message }}</span>
        </div>
      </div>
    </section>

    <section class="content-panel">
      <div class="panel-header">
        <div>
          <h2>版本回滚</h2>
          <p>回滚会创建一条新的发布请求，由 controller-manager 异步推进。</p>
        </div>
      </div>

      <form class="release-grid" @submit.prevent="runRollback">
        <label>
          <span>命名空间</span>
          <input v-model="rollbackForm.namespace" class="search-input" />
        </label>
        <label>
          <span>项目名称</span>
          <input v-model="rollbackForm.projectName" class="search-input" />
        </label>
        <label>
          <span>目标版本</span>
          <input v-model="rollbackForm.targetVersion" class="search-input" />
        </label>
        <label>
          <span>配置分片</span>
          <input v-model="rollbackForm.configShard" class="search-input" />
        </label>
        <label>
          <span>操作人</span>
          <input v-model="rollbackForm.createdBy" class="search-input" />
        </label>
        <label>
          <span>回滚说明</span>
          <input v-model="rollbackForm.description" class="search-input" />
        </label>
        <div class="form-actions">
          <button class="primary-button" type="submit" :disabled="busy">
            <RotateCcw :size="16" />
            创建回滚
          </button>
        </div>
      </form>
    </section>

    <section v-if="releaseResult" class="content-panel">
      <div class="panel-header">
        <div>
          <h2>发布结果</h2>
          <p>{{ releaseResult.version }}</p>
        </div>
        <StatusBadge :label="releaseResult.phase || 'PENDING'" tone="info" />
      </div>
      <div class="key-value-grid">
        <span>发布 ID</span>
        <strong>{{ releaseResult.releaseId }}</strong>
        <span>分片</span>
        <strong>{{ releaseResult.configShard || '默认分片' }}</strong>
        <span>创建时间</span>
        <strong>{{ formatTime(releaseResult.createdAt) }}</strong>
      </div>
    </section>

    <ResourceListView
      resource-type="published-configs"
      title="已发布配置"
      description="查看 PublishedConfig、节点应用进度和失败原因。"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import { AlertTriangle, CheckCircle2, Rocket, RotateCcw } from 'lucide-vue-next';
import MetricStrip from '../components/MetricStrip.vue';
import StatusBadge from '../components/StatusBadge.vue';
import {
  CreateReleaseRequest,
  CreateRollbackRequest,
  ReleaseDryRunResult,
  ReleaseResult,
  createRelease,
  createRollback,
  dryRunRelease
} from '../api/client';
import ResourceListView from './ResourceListView.vue';

type StatusTone = 'success' | 'warning' | 'danger' | 'info' | 'neutral';

const releaseForm = reactive<CreateReleaseRequest>({
  namespace: 'default',
  projectName: '',
  configShard: 'default',
  description: '',
  createdBy: 'console'
});

const rollbackForm = reactive<CreateRollbackRequest>({
  namespace: 'default',
  projectName: '',
  targetVersion: '',
  configShard: 'default',
  description: '',
  createdBy: 'console'
});

const busy = ref(false);
const error = ref('');
const notice = ref('');
const dryRun = ref<ReleaseDryRunResult | null>(null);
const releaseResult = ref<ReleaseResult | null>(null);

const releaseStatusLabel = computed(() => {
  if (releaseResult.value) {
    return releaseResult.value.phase || '已创建';
  }
  if (dryRun.value) {
    return dryRun.value.passed ? '校验通过' : '校验未通过';
  }
  return '待操作';
});

const releaseStatusTone = computed<StatusTone>(() => {
  if (releaseResult.value) {
    return 'info';
  }
  if (dryRun.value) {
    return dryRun.value.passed ? 'success' : 'danger';
  }
  return 'neutral';
});

const dryRunMetrics = computed(() => [
  { label: '路由数', value: String(dryRun.value?.routeCount ?? 0), note: 'GatewayRoute' },
  { label: '上游数', value: String(dryRun.value?.upstreamCount ?? 0), note: 'Upstream' },
  { label: '策略数', value: String(dryRun.value?.policyCount ?? 0), note: 'Policy' },
  { label: '消息数', value: String(dryRun.value?.messages.length ?? 0), note: 'dry-run' }
]);

async function runDryRun() {
  await run(async () => {
    dryRun.value = await dryRunRelease(cleanReleaseRequest());
    releaseResult.value = null;
    notice.value = dryRun.value.passed ? '发布校验通过' : '发布校验未通过';
  });
}

async function runCreateRelease() {
  await run(async () => {
    dryRun.value = await dryRunRelease(cleanReleaseRequest());
    if (!dryRun.value.passed) {
      throw new Error('发布校验未通过，不能创建发布请求');
    }
    releaseResult.value = await createRelease(cleanReleaseRequest());
    syncRollbackForm();
    notice.value = `发布请求已创建：${releaseResult.value.releaseId}`;
  });
}

async function runRollback() {
  await run(async () => {
    releaseResult.value = await createRollback(cleanRollbackRequest());
    notice.value = `回滚请求已创建：${releaseResult.value.releaseId}`;
  });
}

async function run(task: () => Promise<void>) {
  busy.value = true;
  error.value = '';
  notice.value = '';
  try {
    await task();
  } catch (err) {
    error.value = err instanceof Error ? err.message : '操作失败';
  } finally {
    busy.value = false;
  }
}

function cleanReleaseRequest(): CreateReleaseRequest {
  return {
    namespace: releaseForm.namespace,
    projectName: releaseForm.projectName,
    configShard: releaseForm.configShard || undefined,
    description: releaseForm.description || undefined,
    createdBy: releaseForm.createdBy || undefined
  };
}

function cleanRollbackRequest(): CreateRollbackRequest {
  return {
    namespace: rollbackForm.namespace,
    projectName: rollbackForm.projectName,
    targetVersion: rollbackForm.targetVersion,
    configShard: rollbackForm.configShard || undefined,
    description: rollbackForm.description || undefined,
    createdBy: rollbackForm.createdBy || undefined
  };
}

function syncRollbackForm() {
  rollbackForm.namespace = releaseForm.namespace;
  rollbackForm.projectName = releaseForm.projectName;
  rollbackForm.configShard = releaseForm.configShard;
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
</script>
