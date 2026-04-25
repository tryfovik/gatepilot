export interface ApiResponse<T> {
  status: 'success' | 'fail';
  code: number;
  message: string;
  data: T;
  meta?: {
    traceId?: string;
    timestamp?: string;
    costMillis?: number;
  };
}

export interface CursorPageResponse<T> {
  items: T[];
  nextCursor?: string;
  limit: number;
  total: number;
}

export interface ConfigDiffItem {
  resourceType: string;
  name: string;
  changeType: string;
  baseHash?: string;
  targetHash?: string;
}

export interface ConfigDiffResponse {
  namespace: string;
  baseVersion: string;
  targetVersion: string;
  configShard?: string;
  changed: boolean;
  addedRoutes: number;
  removedRoutes: number;
  changedRoutes: number;
  addedUpstreams: number;
  removedUpstreams: number;
  changedUpstreams: number;
  addedPolicies: number;
  removedPolicies: number;
  changedPolicies: number;
  items: ConfigDiffItem[];
}

const apiBase = '/api/gatepilot/v1';

export async function listResources<T>(
  resourceType: string,
  namespace = 'default'
): Promise<CursorPageResponse<T>> {
  const params = new URLSearchParams();
  if (namespace) {
    params.set('namespace', namespace);
  }
  const response = await fetch(`${apiBase}/resources/${resourceType}?${params.toString()}`);
  if (!response.ok) {
    throw new Error(`请求失败：${response.status}`);
  }
  const body = (await response.json()) as ApiResponse<CursorPageResponse<T>>;
  if (body.status !== 'success') {
    throw new Error(body.message || '请求失败');
  }
  return body.data;
}

export async function diffConfigSnapshots(
  namespace: string,
  baseVersion: string,
  targetVersion: string,
  configShard?: string
): Promise<ConfigDiffResponse> {
  const params = new URLSearchParams({
    namespace,
    baseVersion,
    targetVersion
  });
  if (configShard) {
    params.set('configShard', configShard);
  }
  const response = await fetch(`${apiBase}/config-snapshots/diff?${params.toString()}`);
  if (!response.ok) {
    throw new Error(`请求失败：${response.status}`);
  }
  const body = (await response.json()) as ApiResponse<ConfigDiffResponse>;
  if (body.status !== 'success') {
    throw new Error(body.message || '请求失败');
  }
  return body.data;
}
