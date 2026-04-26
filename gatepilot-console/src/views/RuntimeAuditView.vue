<template>
  <div class="page-stack">
    <section class="content-panel">
      <div class="panel-header">
        <div>
          <h2>运行审计</h2>
          <p>按 TraceId、节点、路由和执行结果查询 proxy 上报的业务请求审计。</p>
        </div>
        <button class="ghost-button" type="button" @click="load">
          <RefreshCw :size="16" />
          刷新
        </button>
      </div>

      <form class="filterbar audit-form" @submit.prevent="load">
        <input v-model="filters.namespace" class="search-input compact-input" placeholder="命名空间" />
        <input v-model="filters.nodeId" class="search-input compact-input" placeholder="节点 ID" />
        <input v-model="filters.routeId" class="search-input compact-input" placeholder="路由 ID" />
        <input v-model="filters.traceId" class="search-input compact-input" placeholder="TraceId" />
        <select v-model="filters.outcome" class="select-input compact-input">
          <option value="">全部结果</option>
          <option value="SUCCESS">SUCCESS</option>
          <option value="REJECTED">REJECTED</option>
          <option value="FALLBACK">FALLBACK</option>
          <option value="ERROR">ERROR</option>
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
            <th>路由 / 上游</th>
            <th>结果</th>
            <th>状态</th>
            <th>耗时</th>
            <th>染色</th>
            <th>TraceId</th>
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
                <span>{{ item.routeId || '-' }}</span>
                <span class="resource-subtitle">{{ item.upstreamName || item.upstreamUri || '-' }}</span>
              </div>
            </td>
            <td>
              <StatusBadge :label="item.outcome || 'UNKNOWN'" :tone="outcomeTone(item)" />
            </td>
            <td>{{ item.status ?? '-' }}</td>
            <td>{{ item.latencyMillis ?? '-' }}ms</td>
            <td>{{ item.trafficColor || '-' }}</td>
            <td class="mono-cell">{{ item.traceId || '-' }}</td>
            <td>{{ formatTime(item.occurredAt) }}</td>
          </tr>
        </tbody>
      </table>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { RefreshCw, Search } from 'lucide-vue-next';
import MetricStrip from '../components/MetricStrip.vue';
import StatusBadge from '../components/StatusBadge.vue';
import { RuntimeAuditRecord, listRuntimeAudits } from '../api/client';

type StatusTone = 'success' | 'warning' | 'danger' | 'info' | 'neutral';

const filters = reactive({
  namespace: 'default',
  nodeId: '',
  routeId: '',
  traceId: '',
  outcome: ''
});

const loading = ref(false);
const error = ref('');
const items = ref<RuntimeAuditRecord[]>([]);

const metrics = computed(() => {
  const errorCount = items.value.filter((item) => outcomeTone(item) === 'danger').length;
  const fallbackCount = items.value.filter((item) => item.fallback).length;
  const avgLatency = average(items.value.map((item) => item.latencyMillis).filter((value): value is number => value !== undefined));
  return [
    { label: '审计数', value: String(items.value.length), note: '当前页' },
    { label: '异常请求', value: String(errorCount), note: 'ERROR / 5xx' },
    { label: 'Fallback', value: String(fallbackCount), note: '降级次数' },
    { label: '平均耗时', value: avgLatency === null ? '-' : `${avgLatency}ms`, note: '当前页' }
  ];
});

async function load() {
  loading.value = true;
  error.value = '';
  try {
    const page = await listRuntimeAudits({
      namespace: filters.namespace || undefined,
      nodeId: filters.nodeId || undefined,
      routeId: filters.routeId || undefined,
      traceId: filters.traceId || undefined,
      outcome: filters.outcome || undefined,
      limit: 50
    });
    items.value = page.items;
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载失败';
  } finally {
    loading.value = false;
  }
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

function average(values: number[]) {
  if (values.length === 0) {
    return null;
  }
  const total = values.reduce((sum, value) => sum + value, 0);
  return Math.round(total / values.length);
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

onMounted(load);
</script>
