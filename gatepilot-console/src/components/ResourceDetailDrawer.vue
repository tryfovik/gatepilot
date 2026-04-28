<template>
  <div v-if="open" class="drawer-backdrop" role="presentation" @click.self="emit('close')">
    <aside class="detail-drawer" aria-label="资源详情">
      <header class="drawer-header">
        <div>
          <h2>{{ title }}</h2>
          <p>{{ subtitle }}</p>
        </div>
        <button class="icon-button" type="button" aria-label="关闭详情" @click="emit('close')">
          <X :size="18" />
        </button>
      </header>
      <div class="drawer-content">
        <div v-if="sections.length === 0" class="state-box state-box--compact">暂无可展示字段</div>
        <section v-for="section in sections" :key="section.title" class="detail-section">
          <h3>{{ section.title }}</h3>
          <div class="detail-field-grid">
            <div v-for="field in section.fields" :key="`${section.title}-${field.label}`" class="detail-row">
              <span>{{ field.label }}</span>
              <strong>{{ field.value }}</strong>
            </div>
          </div>
        </section>
      </div>
    </aside>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { X } from 'lucide-vue-next';
import { applyStateLabel, formatTime, releaseStrategyLabel, resourcePhaseLabel } from '../utils/format';

const props = defineProps<{
  open: boolean;
  title: string;
  subtitle: string;
  payload?: unknown;
}>();

const emit = defineEmits<{
  close: [];
}>();

interface DetailField {
  label: string;
  value: string;
}

interface DetailSection {
  title: string;
  fields: DetailField[];
}

const fieldLabels: Record<string, string> = {
  uid: 'UID',
  name: '名称',
  namespace: '命名空间',
  tenant: '租户',
  generation: '资源版本',
  labels: '标签',
  annotations: '注解',
  createdAt: '创建时间',
  updatedAt: '更新时间',
  displayName: '展示名称',
  description: '说明',
  ownerTeam: '负责团队',
  environment: '环境',
  trafficTier: '流量等级',
  configShard: '配置分片',
  defaultConfigShard: '默认配置分片',
  isolationGroup: '隔离组',
  defaultIsolationGroup: '默认隔离组',
  domains: '入口域名',
  quotas: '项目配额',
  projectRef: '所属项目',
  protocols: '协议',
  hosts: '入口域名',
  path: '路径规则',
  methods: 'HTTP 方法',
  upstreamRef: '默认上游',
  policyRefs: '绑定策略',
  rewrite: '重写规则',
  type: '类型',
  strategy: '策略',
  loadBalance: '负载均衡',
  endpoints: '端点',
  healthCheck: '健康检查',
  connectionPool: '连接池',
  rateLimit: '限流',
  retry: '重试',
  circuitBreaker: '熔断',
  colorRules: '染色规则',
  trafficSplits: '流量拆分',
  version: '版本',
  configHash: '配置哈希',
  routes: '路由',
  upstreams: '上游',
  policies: '策略',
  targetNodeRefs: '目标节点',
  targetNodeSelector: '节点选择器',
  enabled: '启用',
  acceptingProjects: '允许新项目',
  dedicated: '独享副本池',
  nodeSelector: '节点选择器',
  maxProjectCount: '最大项目数',
  maxRouteCount: '最大路由数',
  maxRequestsPerSecond: '最大 RPS',
  maxActiveConnections: '最大连接数',
  module: '模块',
  key: '参数键',
  valueType: '值类型',
  value: '参数值',
  defaultValue: '默认值',
  hotReloadable: '热生效',
  applyMode: '生效模式',
  scope: '生效范围',
  serverAddr: '服务地址',
  registryRef: '注册中心',
  registryType: '注册中心类型',
  authType: '认证方式',
  username: '用户名',
  password: '密码',
  accessKey: 'AccessKey',
  secretKey: 'SecretKey',
  serviceName: '服务名',
  discovery: '服务发现',
  metadataSelector: '元数据筛选',
  acceptingUpstreams: '允许新上游',
  nodeId: '节点 ID',
  role: '角色',
  zone: '可用区',
  configShards: '配置分片',
  address: '地址',
  agentVersion: 'Agent 版本',
  proxyVersion: 'Proxy 版本',
  capabilities: '节点能力',
  phase: '资源状态',
  nodePhase: '节点状态',
  currentConfigVersion: '当前配置',
  desiredConfigVersion: '期望配置',
  lastGoodConfigVersion: 'Last-good',
  applyState: '应用状态',
  lastApplyResult: '最近应用',
  health: '健康摘要',
  metrics: '指标摘要',
  loadedRouteCount: '已加载路由',
  loadedUpstreamCount: '已加载上游',
  loadedPolicyCount: '已加载策略',
  upstreamHealth: '上游健康',
  lastHeartbeatAt: '最近心跳',
  currentPublishedVersion: '当前发布版本',
  latestPublishedVersion: '最近发布版本',
  latestReleaseVersion: '最近发布版本',
  projectCount: '项目数',
  routeCount: '路由数',
  upstreamCount: '上游数',
  policyCount: '策略数',
  nodeCount: '节点数',
  readyNodeCount: '就绪节点',
  capacityState: '容量状态',
  lastAppliedAt: '最近生效时间',
  appliedValue: '当前生效值',
  appliedGeneration: '已生效版本',
  involvedObject: '关联资源',
  severity: '事件级别',
  source: '事件来源',
  reason: '原因码',
  message: '事件说明',
  firstObservedAt: '首次观测',
  lastObservedAt: '最近观测',
  count: '出现次数',
  traceId: 'TraceId',
  archived: '已归档',
  acknowledgedBy: '确认人',
  acknowledgedAt: '确认时间'
};

const sections = computed<DetailSection[]>(() => {
  const payload = toRecord(props.payload);
  if (!payload) {
    return [];
  }
  return [
    sectionOf('元信息', payload.metadata),
    sectionOf('期望配置', payload.spec),
    sectionOf('当前状态', payload.status),
    sectionOf('其他字段', otherFields(payload))
  ].filter((section): section is DetailSection => Boolean(section && section.fields.length));
});

function sectionOf(title: string, value: unknown): DetailSection | null {
  const record = toRecord(value);
  if (!record) {
    return null;
  }
  return {
    title,
    fields: fieldsOf(record)
  };
}

function otherFields(payload: Record<string, unknown>) {
  return Object.fromEntries(
    Object.entries(payload).filter(([key]) => !['metadata', 'spec', 'status'].includes(key))
  );
}

function fieldsOf(record: Record<string, unknown>, prefix = '', depth = 0): DetailField[] {
  return Object.entries(record)
    .filter(([, value]) => value !== undefined && value !== null && value !== '')
    .flatMap(([key, value]) => {
      const path = prefix ? `${prefix}.${key}` : key;
      const nested = toRecord(value);
      if (nested && depth < 1 && shouldExpand(key, nested)) {
        return fieldsOf(nested, path, depth + 1);
      }
      return [
        {
          label: labelOf(path, key),
          value: formatValue(path, key, value)
        }
      ];
    });
}

function shouldExpand(key: string, record: Record<string, unknown>) {
  if (['labels', 'annotations', 'attributes', 'capabilities', 'quotas'].includes(key)) {
    return false;
  }
  return !record.name && !record.host && !record.value && !record.upstreamName;
}

function formatValue(path: string, key: string, value: unknown): string {
  if (value === undefined || value === null || value === '') {
    return '-';
  }
  if (sensitiveField(path, key)) {
    return '******';
  }
  if (key.endsWith('At') || path.endsWith('At')) {
    return formatTime(value as string | number);
  }
  if (key === 'phase' || key === 'nodePhase') {
    return resourcePhaseLabel(String(value));
  }
  if (key === 'applyState' || key === 'state') {
    return applyStateLabel(String(value));
  }
  if (key === 'strategy') {
    return releaseStrategyLabel(String(value));
  }
  if (typeof value === 'boolean') {
    return value ? '是' : '否';
  }
  if (typeof value === 'number') {
    return new Intl.NumberFormat('zh-CN', { maximumFractionDigits: 2 }).format(value);
  }
  if (typeof value === 'string') {
    return value;
  }
  if (Array.isArray(value)) {
    return formatArray(value);
  }
  const record = toRecord(value);
  return record ? summarizeRecord(record) : String(value);
}

function sensitiveField(path: string, key: string) {
  return ['password', 'secretKey', 'accessKey'].includes(key) || ['password', 'secretKey', 'accessKey'].includes(path);
}

function formatArray(values: unknown[]) {
  if (values.length === 0) {
    return '-';
  }
  return values.map((item) => {
    const record = toRecord(item);
    return record ? summarizeRecord(record) : String(item);
  }).join('；');
}

function summarizeRecord(record: Record<string, unknown>): string {
  if (record.name || record.kind) {
    return [record.kind, record.namespace, record.name].filter(Boolean).join(' / ');
  }
  if (record.host || record.port) {
    return `${record.host || '-'}:${record.port || '-'}${record.weight ? `，权重 ${record.weight}` : ''}`;
  }
  if (record.upstreamName) {
    return `${record.upstreamName} 健康 ${record.healthyEndpointCount ?? 0} / 异常 ${record.unhealthyEndpointCount ?? 0}`;
  }
  if (record.upstreamRef || record.weight || record.color || record.target) {
    return [
      record.target,
      summarizeNested(record.upstreamRef),
      record.weight !== undefined ? `${record.weight}%` : '',
      record.color ? `染色 ${record.color}` : ''
    ].filter(Boolean).join(' / ');
  }
  if (record.agentHealthy !== undefined || record.proxyHealthy !== undefined || record.controlPlaneConnected !== undefined) {
    return [
      `agent ${record.agentHealthy ? '健康' : '异常'}`,
      `proxy ${record.proxyHealthy ? '健康' : '异常'}`,
      `控制面 ${record.controlPlaneConnected ? '已连接' : '未连接'}`,
      record.message
    ].filter(Boolean).join(' / ');
  }
  if (record.type || record.value || record.stripPrefix !== undefined) {
    return [
      record.type,
      record.value,
      record.stripPrefix ? '剥离前缀' : ''
    ].filter(Boolean).join(' / ');
  }
  const pairs = Object.entries(record)
    .filter(([, value]) => value !== undefined && value !== null && typeof value !== 'object')
    .slice(0, 4)
    .map(([key, value]) => `${labelOf(key, key)} ${formatValue(key, key, value)}`);
  return pairs.length ? pairs.join('，') : `${Object.keys(record).length} 个字段`;
}

function summarizeNested(value: unknown): string {
  const record = toRecord(value);
  return record ? summarizeRecord(record) : String(value ?? '');
}

function labelOf(path: string, key: string) {
  return fieldLabels[path] || fieldLabels[key] || key;
}

function toRecord(value: unknown): Record<string, unknown> | null {
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    return null;
  }
  return value as Record<string, unknown>;
}
</script>
