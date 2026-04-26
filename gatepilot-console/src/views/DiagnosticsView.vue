<template>
  <div class="page-stack">
    <section class="content-panel">
      <div class="panel-header">
        <div>
          <h2>路由诊断</h2>
          <p>输入一次模拟请求，查看已发布配置下的路由命中、染色、上游和治理策略判断。</p>
        </div>
        <StatusBadge :label="resultStatusLabel" :tone="resultStatusTone" />
      </div>

      <form class="diagnostic-form" @submit.prevent="run">
        <input v-model="form.namespace" class="search-input compact-input" placeholder="命名空间" />
        <input v-model="form.projectName" class="search-input compact-input" placeholder="项目名称" />
        <input v-model="form.version" class="search-input compact-input" placeholder="发布版本" />
        <input v-model="form.configShard" class="search-input compact-input" placeholder="配置分片" />
        <select v-model="form.method" class="select-input compact-input">
          <option value="GET">GET</option>
          <option value="POST">POST</option>
          <option value="PUT">PUT</option>
          <option value="DELETE">DELETE</option>
          <option value="PATCH">PATCH</option>
        </select>
        <input v-model="form.host" class="search-input compact-input" placeholder="Host" />
        <input v-model="form.path" class="search-input path-input" placeholder="/api/orders?id=1" />
        <input v-model="headerName" class="search-input compact-input" placeholder="Header 名" />
        <input v-model="headerValue" class="search-input compact-input" placeholder="Header 值" />
        <input v-model="form.remoteAddress" class="search-input compact-input" placeholder="远端 IP" />
        <button class="primary-button" type="submit">
          <Stethoscope :size="16" />
          诊断
        </button>
      </form>

      <div v-if="loading" class="state-box">正在诊断...</div>
      <div v-else-if="error" class="state-box state-box--error">{{ error }}</div>
    </section>

    <section v-if="result" class="content-panel">
      <div class="panel-header">
        <div>
          <h2>诊断结果</h2>
          <p>{{ result.method || '-' }} {{ result.host || '-' }}{{ result.path || '-' }}</p>
        </div>
        <div class="topbar-actions">
          <StatusBadge :label="result.matched ? '已命中' : '未命中'" :tone="result.matched ? 'success' : 'danger'" />
          <span class="mono-cell">{{ shortHash(result.configHash) }}</span>
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
            <span>上游</span>
            <strong>{{ result.route?.upstreamName || '-' }}</strong>
            <span>策略</span>
            <strong>{{ listText(result.route?.policyNames, '未绑定') }}</strong>
          </div>
        </div>

        <div class="diagnostic-block">
          <h3>访问</h3>
          <div class="key-value-grid">
            <span>方法</span>
            <StatusBadge :label="result.access?.methodAllowed ? '允许' : '拒绝'" :tone="result.access?.methodAllowed ? 'success' : 'danger'" />
            <span>白名单</span>
            <strong>{{ listText(result.access?.allowedMethods, '全部') }}</strong>
            <span>认证</span>
            <StatusBadge :label="result.access?.authenticationRequired ? '需要' : '不需要'" :tone="result.access?.authenticationRequired ? 'warning' : 'success'" />
            <span>认证策略</span>
            <strong>{{ listText(result.access?.authPolicyNames, '未绑定') }}</strong>
          </div>
        </div>

        <div class="diagnostic-block">
          <h3>染色</h3>
          <div class="key-value-grid">
            <span>颜色</span>
            <strong>{{ result.traffic?.color || '-' }}</strong>
            <span>来源</span>
            <strong>{{ colorSourceLabel(result.traffic?.source) }}</strong>
            <span>Header</span>
            <strong>{{ result.traffic?.headerName || '-' }}</strong>
            <span>发布目标</span>
            <strong>{{ result.traffic?.releaseTarget || '-' }}</strong>
          </div>
        </div>

        <div class="diagnostic-block">
          <h3>上游</h3>
          <div class="key-value-grid">
            <span>名称</span>
            <StatusBadge :label="result.upstream?.name || '未配置'" :tone="result.upstream?.available ? 'success' : 'danger'" />
            <span>协议</span>
            <strong>{{ result.upstream?.protocol || '-' }}</strong>
            <span>端点</span>
            <strong>{{ result.upstream?.endpointCount ?? '-' }}</strong>
            <span>健康检查</span>
            <strong>{{ result.upstream?.healthCheckEnabled ? '已启用' : '未启用' }}</strong>
          </div>
        </div>
      </div>
    </section>

    <section v-if="result" class="content-panel">
      <div class="panel-header">
        <div>
          <h2>治理与提示</h2>
          <p>{{ result.version || '-' }} / {{ result.configShard || '默认分片' }}</p>
        </div>
      </div>

      <div class="diagnostic-grid">
        <div class="diagnostic-block">
          <h3>治理</h3>
          <div class="key-value-grid">
            <span>重试</span>
            <strong>{{ result.governance?.retry?.enabled ? `${result.governance.retry.maxAttempts || '-'} 次` : '未启用' }}</strong>
            <span>限流</span>
            <strong>{{ result.governance?.rateLimit?.enabled ? `${result.governance.rateLimit.requestsPerSecond || '-'} RPS` : '未启用' }}</strong>
            <span>熔断</span>
            <strong>{{ result.governance?.circuitBreaker?.enabled ? `失败率 ${result.governance.circuitBreaker.failureRateThreshold || '-'}%` : '未启用' }}</strong>
            <span>Fallback</span>
            <strong>{{ result.governance?.circuitBreaker?.fallbackMessage || '-' }}</strong>
          </div>
        </div>

        <div class="diagnostic-block">
          <h3>提示</h3>
          <div v-if="result.warnings.length === 0" class="state-box state-box--compact">暂无告警</div>
          <div v-else class="warning-list">
            <div v-for="warning in result.warnings" :key="warning" class="warning-item">
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
import { computed, reactive, ref } from 'vue';
import { AlertTriangle, Stethoscope } from 'lucide-vue-next';
import StatusBadge from '../components/StatusBadge.vue';
import { RouteDiagnosticsResponse, diagnoseRoute } from '../api/client';

const form = reactive({
  namespace: 'default',
  projectName: '',
  version: '',
  configShard: '',
  method: 'GET',
  host: '',
  path: '/api',
  remoteAddress: ''
});

const headerName = ref('');
const headerValue = ref('');
const loading = ref(false);
const error = ref('');
const result = ref<RouteDiagnosticsResponse | null>(null);

const resultStatusLabel = computed(() => {
  if (!result.value) {
    return '待诊断';
  }
  return result.value.matched ? '已命中' : '未命中';
});

const resultStatusTone = computed<'success' | 'warning' | 'danger' | 'info' | 'neutral'>(() => {
  if (!result.value) {
    return 'neutral';
  }
  return result.value.matched ? 'success' : 'danger';
});

async function run() {
  loading.value = true;
  error.value = '';
  const headers: Record<string, string[]> = {};
  if (headerName.value && headerValue.value) {
    headers[headerName.value] = [headerValue.value];
  }
  try {
    result.value = await diagnoseRoute({
      ...form,
      headers
    });
  } catch (err) {
    result.value = null;
    error.value = err instanceof Error ? err.message : '诊断失败';
  } finally {
    loading.value = false;
  }
}

function listText(values?: string[], emptyText = '-') {
  if (!values || values.length === 0) {
    return emptyText;
  }
  return values.join(', ');
}

function shortHash(value?: string) {
  if (!value) {
    return '-';
  }
  return value.length > 12 ? `${value.slice(0, 12)}...` : value;
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
</script>
