export type StatusTone = 'success' | 'warning' | 'danger' | 'info' | 'neutral';

type TimeValue = string | number | null | undefined;

export function formatTime(value?: TimeValue) {
  const date = toDate(value);
  if (!date) {
    return '-';
  }
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(date);
}

export function toDate(value?: TimeValue) {
  if (value === undefined || value === null || value === '') {
    return null;
  }
  const timestamp = typeof value === 'number' ? value : Number(value);
  const date = Number.isFinite(timestamp) ? new Date(normalizeEpochMillis(timestamp)) : new Date(value);
  if (Number.isNaN(date.getTime()) || date.getFullYear() < 2000) {
    return null;
  }
  return date;
}

function normalizeEpochMillis(timestamp: number) {
  const absolute = Math.abs(timestamp);
  if (absolute < 10_000_000_000) {
    return timestamp * 1000;
  }
  if (absolute < 10_000_000_000_000) {
    return timestamp;
  }
  if (absolute < 10_000_000_000_000_000) {
    return Math.trunc(timestamp / 1000);
  }
  return Math.trunc(timestamp / 1_000_000);
}

export function shortHash(value?: string | null) {
  if (!value) {
    return '-';
  }
  return value.length > 12 ? `${value.slice(0, 12)}...` : value;
}

export function listText(values?: Array<string | undefined> | null, emptyText = '-') {
  const actual = values?.filter((value): value is string => Boolean(value)) ?? [];
  return actual.length ? actual.join(', ') : emptyText;
}

export function numberText(value?: number | null, fractionDigits = 2) {
  if (value === undefined || value === null) {
    return '-';
  }
  return new Intl.NumberFormat('zh-CN', { maximumFractionDigits: fractionDigits }).format(value);
}

export function percentText(value?: number | null) {
  if (value === undefined || value === null) {
    return '-';
  }
  return new Intl.NumberFormat('zh-CN', { style: 'percent', maximumFractionDigits: 2 }).format(value);
}

export function releaseStrategyLabel(value?: string | null) {
  const labels: Record<string, string> = {
    BLUE_GREEN: '蓝绿发布',
    CANARY: '灰度发布',
    TRAFFIC_SPLIT: '固定权重',
    SHADOW: '影子流量'
  };
  return labels[value || ''] || value || '-';
}

export function applyStateLabel(value?: string | null) {
  const labels: Record<string, string> = {
    PENDING: '等待应用',
    STAGED: '已暂存',
    APPLIED: '已应用',
    FAILED: '失败',
    ROLLED_BACK: '已回滚'
  };
  return labels[value || ''] || value || '未知';
}

export function applyStateTone(value?: string | null): StatusTone {
  if (value === 'APPLIED') return 'success';
  if (value === 'PENDING' || value === 'STAGED') return 'info';
  if (value === 'ROLLED_BACK') return 'warning';
  if (value === 'FAILED') return 'danger';
  return 'neutral';
}

export function resourcePhaseLabel(value?: string | null, fallback = '已配置') {
  const labels: Record<string, string> = {
    ACTIVE: '正常',
    READY: '就绪',
    REGISTERED: '已注册',
    PENDING: '等待中',
    DEGRADED: '有告警',
    FAILED: '失败',
    DELETED: '已删除',
    NOT_READY: '未就绪',
    OFFLINE: '离线'
  };
  return labels[value || ''] || fallback;
}

export function resourcePhaseTone(value?: string | null): StatusTone {
  if (value === 'ACTIVE' || value === 'READY') return 'success';
  if (value === 'REGISTERED' || value === 'PENDING') return 'info';
  if (value === 'DEGRADED') return 'warning';
  if (value === 'FAILED' || value === 'DELETED' || value === 'NOT_READY' || value === 'OFFLINE') return 'danger';
  return 'neutral';
}
