<template>
  <div class="page-stack">
    <section class="content-panel">
      <div class="panel-header">
        <div>
          <h2>项目接入向导</h2>
          <p>填写项目、路由、上游、治理和发布参数，生成 GatePilot 声明式资源。</p>
        </div>
        <div class="panel-actions">
          <button class="ghost-button" type="button" :disabled="busy" @click="runPreview">
            <Eye :size="16" />
            预览
          </button>
          <button class="ghost-button" type="button" :disabled="busy" @click="runDryRun">
            <CheckCircle2 :size="16" />
            校验
          </button>
          <button class="primary-button" type="button" :disabled="busy" @click="runApply">
            <Save :size="16" />
            保存资源
          </button>
          <button class="primary-button" type="button" :disabled="busy" @click="runPublish">
            <Rocket :size="16" />
            保存并发布
          </button>
        </div>
      </div>

      <form class="template-form" @submit.prevent="runPreview">
        <div class="template-section">
          <h3>项目</h3>
          <div class="form-grid">
            <label>
              <span>命名空间</span>
              <input v-model="form.namespace" class="search-input" />
            </label>
            <label>
              <span>项目名称</span>
              <input v-model="form.projectName" class="search-input" />
            </label>
            <label>
              <span>展示名称</span>
              <input v-model="form.displayName" class="search-input" />
            </label>
            <label>
              <span>负责团队</span>
              <input v-model="form.ownerTeam" class="search-input" />
            </label>
            <label>
              <span>环境</span>
              <select v-model="form.environment" class="select-input">
                <option v-for="item in environmentOptions" :key="item" :value="item">{{ item }}</option>
              </select>
            </label>
            <label>
              <span>配置分片</span>
              <input v-model="form.configShard" class="search-input" />
            </label>
          </div>
        </div>

        <div class="template-section">
          <h3>路由与上游</h3>
          <div class="form-grid">
            <label>
              <span>入口域名</span>
              <input v-model="form.route.host" class="search-input" />
            </label>
            <label>
              <span>路径前缀</span>
              <input v-model="form.route.path" class="search-input" />
            </label>
            <label>
              <span>上游地址</span>
              <input v-model="form.upstream.host" class="search-input" />
            </label>
            <label>
              <span>上游端口</span>
              <input v-model.number="form.upstream.port" class="search-input" type="number" min="1" max="65535" />
            </label>
            <label>
              <span>协议</span>
              <select v-model="form.upstream.protocol" class="select-input">
                <option value="HTTP">HTTP</option>
                <option value="HTTPS">HTTPS</option>
              </select>
            </label>
            <label>
              <span>负载均衡</span>
              <select v-model="form.upstream.loadBalance" class="select-input">
                <option v-for="item in loadBalanceOptions" :key="item.value" :value="item.value">
                  {{ item.label }}
                </option>
              </select>
            </label>
            <label>
              <span>健康检查路径</span>
              <input v-model="form.upstream.healthPath" class="search-input" />
            </label>
            <label class="check-row">
              <input v-model="form.route.stripPrefix" type="checkbox" />
              <span>转发时去除路径前缀</span>
            </label>
            <label class="check-row">
              <input v-model="form.upstream.healthCheckEnabled" type="checkbox" />
              <span>启用上游健康检查</span>
            </label>
          </div>
        </div>

        <div class="template-section">
          <h3>治理与发布</h3>
          <div class="form-grid">
            <label>
              <span>限流 QPS</span>
              <input v-model.number="form.governance.requestsPerSecond" class="search-input" type="number" min="1" />
            </label>
            <label>
              <span>突发容量</span>
              <input v-model.number="form.governance.burstCapacity" class="search-input" type="number" min="1" />
            </label>
            <label>
              <span>最大重试次数</span>
              <input v-model.number="form.governance.maxAttempts" class="search-input" type="number" min="1" />
            </label>
            <label>
              <span>候选上游</span>
              <input v-model="form.candidate.host" class="search-input" />
            </label>
            <label>
              <span>候选端口</span>
              <input v-model.number="form.candidate.port" class="search-input" type="number" min="1" max="65535" />
            </label>
            <label>
              <span>候选权重</span>
              <input v-model.number="form.release.candidateWeight" class="search-input" type="number" min="0" max="100" />
            </label>
            <label>
              <span>染色 Header</span>
              <input v-model="form.release.colorHeader" class="search-input" />
            </label>
            <label>
              <span>候选染色值</span>
              <input v-model="form.release.candidateColor" class="search-input" />
            </label>
            <label>
              <span>认证类型</span>
              <select v-model="form.auth.type" class="select-input">
                <option v-for="item in authOptions" :key="item" :value="item">{{ item }}</option>
              </select>
            </label>
            <label class="check-row">
              <input v-model="form.governance.rateLimitEnabled" type="checkbox" />
              <span>启用限流</span>
            </label>
            <label class="check-row">
              <input v-model="form.governance.retryEnabled" type="checkbox" />
              <span>启用重试</span>
            </label>
            <label class="check-row">
              <input v-model="form.release.enabled" type="checkbox" />
              <span>启用灰度发布</span>
            </label>
            <label class="check-row">
              <input v-model="form.candidate.enabled" type="checkbox" />
              <span>创建候选上游</span>
            </label>
            <label class="check-row">
              <input v-model="form.auth.anonymousAllowed" type="checkbox" />
              <span>允许匿名访问</span>
            </label>
          </div>
        </div>
      </form>

      <div v-if="error" class="state-box state-box--error">{{ error }}</div>
      <div v-else-if="notice" class="state-box state-box--compact">{{ notice }}</div>
    </section>

    <MetricStrip v-if="preview" :items="diffMetrics" />

    <section v-if="dryRun" class="content-panel">
      <div class="panel-header">
        <div>
          <h2>校验结果</h2>
          <p>{{ dryRun.passed ? '模板校验通过' : '模板校验未通过' }}</p>
        </div>
        <StatusBadge :label="dryRun.passed ? '通过' : '未通过'" :tone="dryRun.passed ? 'success' : 'danger'" />
      </div>
      <div v-if="dryRun.messages.length === 0" class="state-box state-box--compact">暂无校验告警</div>
      <div v-else class="warning-list">
        <div v-for="message in dryRun.messages" :key="`${message.reason}-${message.message}`" class="warning-item">
          <AlertTriangle :size="16" />
          <span>{{ message.reason }}：{{ message.message }}</span>
        </div>
      </div>
    </section>

    <section v-if="preview" class="content-panel">
      <div class="panel-header">
        <div>
          <h2>资源预览</h2>
          <p>{{ preview.namespace }} / {{ preview.projectName }}</p>
        </div>
        <StatusBadge :label="preview.diff.changed ? '有变更' : '无变更'" :tone="preview.diff.changed ? 'warning' : 'success'" />
      </div>

      <table class="resource-table">
        <thead>
          <tr>
            <th>资源</th>
            <th>类型</th>
            <th>命名空间</th>
            <th>动作</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="resource in preview.resources" :key="`${resource.kind}-${resource.name}`">
            <td>
              <div class="resource-name">{{ resource.name }}</div>
              <div class="resource-subtitle">{{ resource.kind }}</div>
            </td>
            <td>{{ resource.resourceType }}</td>
            <td>{{ resource.namespace }}</td>
            <td>
              <StatusBadge :label="actionLabel(resource.action)" :tone="actionTone(resource.action)" />
            </td>
            <td>
              <button class="table-action" type="button" @click="selectedResource = resource">查看</button>
            </td>
          </tr>
        </tbody>
      </table>
    </section>

    <section v-if="preview || releaseResult" class="content-panel">
      <div class="template-result-grid">
        <div>
          <div class="panel-header panel-header--compact">
            <div>
              <h2>发布请求</h2>
              <p>{{ preview?.configShard || '默认分片' }}</p>
            </div>
          </div>
          <pre class="json-viewer json-viewer--inline">{{ releaseRequestJson }}</pre>
        </div>
        <div>
          <div class="panel-header panel-header--compact">
            <div>
              <h2>发布结果</h2>
              <p>{{ applyResult ? `保存 ${applyResult.savedResourceCount} 个资源` : '等待提交' }}</p>
            </div>
            <StatusBadge
              v-if="releaseResult"
              :label="releaseResult.phase || 'PENDING'"
              :tone="releaseResult.phase === 'FAILED' ? 'danger' : 'info'"
            />
          </div>
          <div v-if="releaseResult" class="key-value-grid">
            <span>发布 ID</span>
            <strong>{{ releaseResult.releaseId }}</strong>
            <span>版本</span>
            <strong>{{ releaseResult.version }}</strong>
            <span>创建时间</span>
            <strong>{{ formatTime(releaseResult.createdAt) }}</strong>
          </div>
          <div v-else class="state-box state-box--compact">暂无发布请求</div>
        </div>
      </div>
    </section>

    <ResourceDetailDrawer
      :open="Boolean(selectedResource)"
      :title="selectedResource?.name || '资源详情'"
      :subtitle="selectedResource?.resourceType || '-'"
      :payload="selectedResource?.resource"
      @close="selectedResource = null"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import { AlertTriangle, CheckCircle2, Eye, Rocket, Save } from 'lucide-vue-next';
import MetricStrip from '../components/MetricStrip.vue';
import ResourceDetailDrawer from '../components/ResourceDetailDrawer.vue';
import StatusBadge from '../components/StatusBadge.vue';
import {
  CreateReleaseRequest,
  ProjectTemplateApplyResponse,
  ProjectTemplateDryRunResponse,
  ProjectTemplatePreviewResponse,
  ProjectTemplateRenderedResource,
  ProjectTemplateRenderRequest,
  ReleaseResult,
  applyProjectTemplate,
  createRelease,
  dryRunProjectTemplate,
  previewProjectTemplate
} from '../api/client';

type StatusTone = 'success' | 'warning' | 'danger' | 'info' | 'neutral';
type TemplateAction = ProjectTemplateRenderedResource['action'];
type TemplateForm = ProjectTemplateRenderRequest & {
  route: NonNullable<ProjectTemplateRenderRequest['route']>;
  upstream: NonNullable<ProjectTemplateRenderRequest['upstream']>;
  candidate: NonNullable<ProjectTemplateRenderRequest['candidate']>;
  governance: NonNullable<ProjectTemplateRenderRequest['governance']>;
  release: NonNullable<ProjectTemplateRenderRequest['release']>;
  auth: NonNullable<ProjectTemplateRenderRequest['auth']>;
};

const environmentOptions = ['prod', 'pre', 'test', 'dev'];
const authOptions = ['NONE', 'API_KEY', 'JWT', 'OAUTH2', 'BASIC', 'MTLS'];
const loadBalanceOptions = [
  { label: '轮询', value: 'ROUND_ROBIN' },
  { label: '随机', value: 'RANDOM' }
];

const form = reactive<TemplateForm>({
  namespace: 'default',
  projectName: 'orders',
  displayName: '订单服务',
  ownerTeam: '交易团队',
  environment: 'prod',
  trafficTier: 'standard',
  configShard: 'default',
  route: {
    host: 'api.example.com',
    path: '/api/orders',
    stripPrefix: true,
    methods: ['ANY']
  },
  upstream: {
    host: 'orders.default.svc.cluster.local',
    port: 8080,
    protocol: 'HTTP',
    loadBalance: 'ROUND_ROBIN',
    healthCheckEnabled: true,
    healthPath: '/actuator/health'
  },
  candidate: {
    enabled: true,
    host: 'orders-canary.default.svc.cluster.local',
    port: 8080
  },
  governance: {
    rateLimitEnabled: true,
    requestsPerSecond: 1000,
    burstCapacity: 2000,
    retryEnabled: true,
    maxAttempts: 2
  },
  release: {
    enabled: true,
    strategy: 'TRAFFIC_SPLIT',
    candidateWeight: 10,
    colorHeader: 'X-GatePilot-Color',
    candidateColor: 'canary'
  },
  auth: {
    type: 'NONE',
    anonymousAllowed: true
  }
});

const loadingAction = ref('');
const error = ref('');
const notice = ref('');
const preview = ref<ProjectTemplatePreviewResponse | null>(null);
const dryRun = ref<ProjectTemplateDryRunResponse | null>(null);
const applyResult = ref<ProjectTemplateApplyResponse | null>(null);
const releaseResult = ref<ReleaseResult | null>(null);
const selectedResource = ref<ProjectTemplateRenderedResource | null>(null);

const busy = computed(() => Boolean(loadingAction.value));

const diffMetrics = computed(() => {
  const diff = preview.value?.diff;
  return [
    { label: '新增资源', value: String(diff?.createCount ?? 0), note: 'CREATE' },
    { label: '更新资源', value: String(diff?.updateCount ?? 0), note: 'UPDATE' },
    { label: '未变化', value: String(diff?.unchangedCount ?? 0), note: 'UNCHANGED' },
    { label: '资源总数', value: String(preview.value?.resources.length ?? 0), note: 'rendered' }
  ];
});

const releaseRequestJson = computed(() => JSON.stringify(preview.value?.releaseRequest ?? {}, null, 2));

async function runPreview() {
  await runAction('preview', async () => {
    preview.value = await previewProjectTemplate(currentRequest());
    dryRun.value = null;
    applyResult.value = null;
    releaseResult.value = null;
    notice.value = '预览已生成';
  });
}

async function runDryRun() {
  await runAction('dryRun', async () => {
    dryRun.value = await dryRunProjectTemplate(currentRequest());
    preview.value = dryRun.value.preview;
    applyResult.value = null;
    releaseResult.value = null;
    notice.value = dryRun.value.passed ? '校验通过' : '校验未通过';
  });
}

async function runApply() {
  await runAction('apply', async () => {
    applyResult.value = await applyProjectTemplate(currentRequest());
    dryRun.value = applyResult.value.dryRun;
    preview.value = applyResult.value.dryRun.preview;
    releaseResult.value = null;
    notice.value = `已保存 ${applyResult.value.savedResourceCount} 个资源`;
  });
}

async function runPublish() {
  await runAction('publish', async () => {
    applyResult.value = await applyProjectTemplate(currentRequest());
    dryRun.value = applyResult.value.dryRun;
    preview.value = applyResult.value.dryRun.preview;
    if (!dryRun.value.passed) {
      throw new Error('模板校验未通过，不能发布');
    }
    releaseResult.value = await createRelease(releaseRequest());
    notice.value = `发布请求已创建：${releaseResult.value.releaseId}`;
  });
}

async function runAction(action: string, task: () => Promise<void>) {
  loadingAction.value = action;
  error.value = '';
  notice.value = '';
  try {
    await task();
  } catch (err) {
    error.value = err instanceof Error ? err.message : '操作失败';
  } finally {
    loadingAction.value = '';
  }
}

function currentRequest(): ProjectTemplateRenderRequest {
  return JSON.parse(JSON.stringify(form)) as ProjectTemplateRenderRequest;
}

function releaseRequest(): CreateReleaseRequest {
  if (!preview.value?.releaseRequest) {
    throw new Error('缺少发布请求预览');
  }
  return preview.value.releaseRequest;
}

function actionLabel(action: TemplateAction) {
  const labels: Record<TemplateAction, string> = {
    CREATE: '新增',
    UPDATE: '更新',
    UNCHANGED: '不变'
  };
  return labels[action];
}

function actionTone(action: TemplateAction): StatusTone {
  if (action === 'CREATE') {
    return 'info';
  }
  if (action === 'UPDATE') {
    return 'warning';
  }
  return 'neutral';
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
