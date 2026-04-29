import { reactive } from 'vue';

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

export interface ConfigSnapshotSummaryResponse {
  namespace?: string;
  projectName?: string;
  version?: string;
  configHash?: string;
  configShard?: string;
  sequence?: number;
  releaseId?: string;
  routeCount: number;
  upstreamCount: number;
  policyCount: number;
  targetNodeCount: number;
  capturedAt?: string;
  capturedBy?: string;
  description?: string;
  rollbackAllowed?: boolean;
  lastRollbackAt?: string;
}

export interface RouteCatalogResponse {
  namespace: string;
  projectName?: string;
  version?: string;
  configShard?: string;
  configHash?: string;
  generatedAt?: string;
  routeCount: number;
  upstreamCount: number;
  policyCount: number;
  targetNodeCount: number;
  routes: RouteCatalogItem[];
  upstreams: RouteCatalogUpstream[];
  policies: RouteCatalogPolicy[];
  nodeApplyResults: RouteCatalogNodeApply[];
}

export interface RouteCatalogItem {
  routeId?: string;
  name?: string;
  hosts?: string[];
  path?: string;
  methods?: string[];
  upstreamName?: string;
  upstreamAvailable: boolean;
  policyNames?: string[];
  stripPrefix?: boolean;
  rewritePathPrefix?: string;
}

export interface RouteCatalogUpstream {
  name?: string;
  protocol?: string;
  loadBalance?: string;
  endpointCount: number;
  healthCheckEnabled?: boolean;
  endpoints?: Array<{
    host?: string;
    port?: number;
    weight?: number;
  }>;
}

export interface RouteCatalogPolicy {
  name?: string;
  type?: string;
}

export interface RouteCatalogNodeApply {
  nodeId?: string;
  zone?: string;
  state?: string;
  appliedVersion?: string;
  reason?: string;
  message?: string;
}

export interface RouteDiagnosticsRequest {
  namespace?: string;
  projectName?: string;
  version?: string;
  configShard?: string;
  method?: string;
  host?: string;
  path?: string;
  headers?: Record<string, string[]>;
  query?: Record<string, string[]>;
  cookies?: Record<string, string[]>;
  remoteAddress?: string;
}

export interface RouteDiagnosticsResponse {
  namespace: string;
  projectName?: string;
  version?: string;
  configShard?: string;
  configHash?: string;
  generatedAt?: string;
  matched: boolean;
  method?: string;
  host?: string;
  path?: string;
  route?: {
    routeId?: string;
    name?: string;
    hosts?: string[];
    path?: string;
    upstreamName?: string;
    policyNames?: string[];
  };
  access?: {
    methodAllowed: boolean;
    allowedMethods?: string[];
    authenticationRequired: boolean;
    anonymousAllowed: boolean;
    authPolicyNames?: string[];
  };
  traffic?: {
    color?: string;
    source?: string;
    headerName?: string;
    releaseTarget?: string;
  };
  upstream?: {
    name?: string;
    protocol?: string;
    loadBalance?: string;
    available: boolean;
    endpointCount: number;
    healthCheckEnabled?: boolean;
  };
  governance?: {
    retry?: {
      enabled: boolean;
      maxAttempts?: number;
      statuses?: number[];
    };
    rateLimit?: {
      enabled: boolean;
      requestsPerSecond?: number;
      burstCapacity?: number;
      paramRuleCount: number;
    };
    circuitBreaker?: {
      enabled: boolean;
      slidingWindowSize?: number;
      failureRateThreshold?: number;
      fallbackStatus?: number;
      fallbackMessage?: string;
    };
  };
  warnings: string[];
}

export interface ProjectTemplateRenderRequest {
  namespace?: string;
  projectName?: string;
  displayName?: string;
  ownerTeam?: string;
  environment?: string;
  trafficTier?: string;
  configShard?: string;
  isolationGroup?: string;
  route?: {
    host?: string;
    path?: string;
    stripPrefix?: boolean;
    methods?: string[];
  };
  upstream?: {
    discoveryType?: string;
    registryCenterName?: string;
    serviceName?: string;
    discoveryNamespace?: string;
    discoveryGroup?: string;
    clusters?: string[];
    metadataSelector?: Record<string, string>;
    host?: string;
    port?: number;
    protocol?: string;
    loadBalance?: string;
    healthCheckEnabled?: boolean;
    healthPath?: string;
  };
  candidate?: {
    enabled?: boolean;
    discoveryType?: string;
    registryCenterName?: string;
    serviceName?: string;
    discoveryNamespace?: string;
    discoveryGroup?: string;
    clusters?: string[];
    metadataSelector?: Record<string, string>;
    host?: string;
    port?: number;
  };
  governance?: {
    rateLimitEnabled?: boolean;
    requestsPerSecond?: number;
    burstCapacity?: number;
    retryEnabled?: boolean;
    maxAttempts?: number;
  };
  release?: {
    enabled?: boolean;
    strategy?: string;
    candidateWeight?: number;
    colorHeader?: string;
    candidateColor?: string;
  };
  auth?: {
    type?: string;
    anonymousAllowed?: boolean;
  };
}

export interface ProjectTemplateRenderedResource {
  resourceType: string;
  kind: string;
  namespace: string;
  name: string;
  action: 'CREATE' | 'UPDATE' | 'UNCHANGED';
  resource: unknown;
}

export interface ProjectTemplateOptionItem {
  value: string;
  label: string;
  enabled: boolean;
}

export interface ProjectTemplateDefaultsResponse {
  values: ProjectTemplateRenderRequest;
  environments: ProjectTemplateOptionItem[];
  protocols: ProjectTemplateOptionItem[];
  loadBalances: ProjectTemplateOptionItem[];
  releaseStrategies: ProjectTemplateOptionItem[];
  authTypes: ProjectTemplateOptionItem[];
}

export interface ProjectTemplatePreviewResponse {
  namespace: string;
  projectName: string;
  configShard?: string;
  resources: ProjectTemplateRenderedResource[];
  releaseRequest: CreateReleaseRequest;
  diff: {
    changed: boolean;
    createCount: number;
    updateCount: number;
    unchangedCount: number;
  };
}

export interface DryRunMessage {
  level: string;
  reason: string;
  message: string;
}

export interface ReleaseDryRunResponse {
  namespace: string;
  projectName: string;
  version: string;
  configShard?: string;
  passed: boolean;
  routeCount: number;
  upstreamCount: number;
  policyCount: number;
  checkedAt?: string;
  messages: DryRunMessage[];
}

export interface ProjectTemplateDryRunResponse {
  passed: boolean;
  preview: ProjectTemplatePreviewResponse;
  messages: DryRunMessage[];
}

export interface ProjectTemplateApplyResponse {
  savedResourceCount: number;
  dryRun: ProjectTemplateDryRunResponse;
}

export interface CreateReleaseRequest {
  namespace: string;
  projectName: string;
  configShard?: string;
  description?: string;
  createdBy?: string;
  resourceRefs?: Array<{
    kind?: string;
    namespace?: string;
    name?: string;
    uid?: string;
  }>;
  targetNodeSelector?: unknown;
}

export interface ReleaseResponse {
  releaseId: string;
  version: string;
  phase: string;
  configShard?: string;
  createdAt?: string;
}

export interface CreateRollbackRequest {
  namespace: string;
  projectName: string;
  targetVersion: string;
  configShard?: string;
  description?: string;
  createdBy?: string;
}

export interface RuntimeAuditRecord {
  id?: number;
  namespace?: string;
  nodeId?: string;
  projectName?: string;
  traceId?: string;
  clientIp?: string;
  method?: string;
  path?: string;
  host?: string;
  routeId?: string;
  upstreamName?: string;
  upstreamUri?: string;
  status?: number;
  latencyMillis?: number;
  trafficColor?: string;
  methodAllowed?: boolean;
  authenticationRequired?: boolean;
  fallback?: boolean;
  outcome?: string;
  reason?: string;
  error?: string;
  occurredAt?: string;
}

const apiBase = '/api/gatepilot/v1';

export const latestApiMeta = reactive<{
  traceId: string;
  timestamp: string;
  costMillis?: number;
}>({
  traceId: '',
  timestamp: ''
});

export const controlPlaneHealth = reactive<{
  state: 'checking' | 'up' | 'down';
  message: string;
  checkedAt: string;
}>({
  state: 'checking',
  message: '正在检测控制面',
  checkedAt: ''
});

function assertApiSuccess<T>(body: ApiResponse<T>): T {
  if (body.meta) {
    latestApiMeta.traceId = body.meta.traceId || latestApiMeta.traceId;
    latestApiMeta.timestamp = body.meta.timestamp || latestApiMeta.timestamp;
    latestApiMeta.costMillis = body.meta.costMillis;
  }
  if (body.status !== 'success') {
    markControlPlaneUp(body.message || '控制面已响应，业务请求未通过');
    throw new Error(body.message || '请求失败');
  }
  markControlPlaneUp('控制面正常');
  return body.data;
}

async function readJson<T>(response: Response): Promise<T> {
  if (!response.ok) {
    const message = await errorMessage(response);
    markControlPlaneDown(message);
    throw new Error(message);
  }
  let body: ApiResponse<T>;
  try {
    body = (await response.json()) as ApiResponse<T>;
  } catch (err) {
    markControlPlaneDown(err instanceof Error ? err.message : '响应解析失败');
    throw err;
  }
  return assertApiSuccess(body);
}

async function errorMessage(response: Response) {
  const text = await response.text().catch(() => '');
  if (text.trim()) {
    return text.trim();
  }
  if (response.status >= 500) {
    return '控制面未启动或接口代理异常';
  }
  return `请求失败：${response.status}`;
}

async function apiFetch(input: RequestInfo | URL, init?: RequestInit): Promise<Response> {
  try {
    return await fetch(input, init);
  } catch (err) {
    markControlPlaneDown(err instanceof Error ? err.message : '控制面连接失败');
    throw err;
  }
}

async function postJson<T>(path: string, payload: unknown): Promise<T> {
  const response = await apiFetch(`${apiBase}${path}`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(payload)
  });
  return readJson<T>(response);
}

async function putJson<T>(path: string, payload: unknown): Promise<T> {
  const response = await apiFetch(`${apiBase}${path}`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(payload)
  });
  return readJson<T>(response);
}

export async function listResources<T>(
  resourceType: string,
  namespace = 'default',
  limit?: number
): Promise<CursorPageResponse<T>> {
  const params = new URLSearchParams();
  if (namespace) {
    params.set('namespace', namespace);
  }
  if (limit) {
    params.set('limit', String(limit));
  }
  const response = await apiFetch(`${apiBase}/resources/${resourceType}?${params.toString()}`);
  return readJson<CursorPageResponse<T>>(response);
}

export async function probeControlPlane(): Promise<void> {
  controlPlaneHealth.state = 'checking';
  controlPlaneHealth.message = '正在检测控制面';
  const params = new URLSearchParams({
    namespace: 'system',
    limit: '1'
  });
  try {
    const response = await apiFetch(`${apiBase}/resources/namespaces?${params.toString()}`);
    await readJson<CursorPageResponse<unknown>>(response);
    markControlPlaneUp('控制面正常');
  } catch (err) {
    markControlPlaneDown(err instanceof Error ? err.message : '控制面不可用');
  }
}

export async function getResource<T>(
  resourceType: string,
  namespace: string,
  name: string
): Promise<T> {
  const response = await apiFetch(
    `${apiBase}/resources/${encodeURIComponent(resourceType)}/${encodeURIComponent(namespace)}/${encodeURIComponent(name)}`
  );
  return readJson<T>(response);
}

export async function saveResource<T>(
  resourceType: string,
  namespace: string,
  name: string,
  resource: T
): Promise<T> {
  return putJson<T>(
    `/resources/${encodeURIComponent(resourceType)}/${encodeURIComponent(namespace)}/${encodeURIComponent(name)}`,
    resource
  );
}

export async function deleteResource(
  resourceType: string,
  namespace: string,
  name: string
): Promise<void> {
  const response = await apiFetch(
    `${apiBase}/resources/${encodeURIComponent(resourceType)}/${encodeURIComponent(namespace)}/${encodeURIComponent(name)}`,
    { method: 'DELETE' }
  );
  if (!response.ok) {
    const message = await errorMessage(response);
    markControlPlaneDown(message);
    throw new Error(message);
  }
  const body = (await response.json()) as ApiResponse<unknown>;
  assertApiSuccess(body);
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
  const response = await apiFetch(`${apiBase}/config-snapshots/diff?${params.toString()}`);
  return readJson<ConfigDiffResponse>(response);
}

export async function listConfigSnapshotSummaries(params: {
  namespace?: string;
  projectName?: string;
  configShard?: string;
  cursor?: string;
  limit?: number;
}): Promise<CursorPageResponse<ConfigSnapshotSummaryResponse>> {
  const search = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== '') {
      search.set(key, String(value));
    }
  });
  const response = await apiFetch(`${apiBase}/config-snapshots?${search.toString()}`);
  return readJson<CursorPageResponse<ConfigSnapshotSummaryResponse>>(response);
}

export async function getRouteCatalog(params: {
  namespace?: string;
  projectName?: string;
  version?: string;
  configShard?: string;
}): Promise<RouteCatalogResponse> {
  const search = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value) {
      search.set(key, value);
    }
  });
  const response = await apiFetch(`${apiBase}/diagnostics/route-catalog?${search.toString()}`);
  return readJson<RouteCatalogResponse>(response);
}

export async function diagnoseRoute(request: RouteDiagnosticsRequest): Promise<RouteDiagnosticsResponse> {
  return postJson<RouteDiagnosticsResponse>('/diagnostics/route', request);
}

export async function getProjectTemplateDefaults(): Promise<ProjectTemplateDefaultsResponse> {
  const response = await apiFetch(`${apiBase}/templates/projects/defaults`);
  return readJson<ProjectTemplateDefaultsResponse>(response);
}

export async function previewProjectTemplate(
  request: ProjectTemplateRenderRequest
): Promise<ProjectTemplatePreviewResponse> {
  return postJson<ProjectTemplatePreviewResponse>('/templates/projects/preview', request);
}

export async function dryRunProjectTemplate(
  request: ProjectTemplateRenderRequest
): Promise<ProjectTemplateDryRunResponse> {
  return postJson<ProjectTemplateDryRunResponse>('/templates/projects/dry-run', request);
}

export async function applyProjectTemplate(
  request: ProjectTemplateRenderRequest
): Promise<ProjectTemplateApplyResponse> {
  return postJson<ProjectTemplateApplyResponse>('/templates/projects/apply', request);
}

export async function createRelease(request: CreateReleaseRequest): Promise<ReleaseResponse> {
  return postJson<ReleaseResponse>('/releases', request);
}

export async function dryRunRelease(request: CreateReleaseRequest): Promise<ReleaseDryRunResponse> {
  return postJson<ReleaseDryRunResponse>('/releases/dry-run', request);
}

export async function createRollback(request: CreateRollbackRequest): Promise<ReleaseResponse> {
  return postJson<ReleaseResponse>('/releases/rollback', request);
}

export async function listRuntimeAudits(params: {
  namespace?: string;
  nodeId?: string;
  projectName?: string;
  routeId?: string;
  traceId?: string;
  outcome?: string;
  startedAt?: string;
  endedAt?: string;
  cursor?: string;
  limit?: number;
}): Promise<CursorPageResponse<RuntimeAuditRecord>> {
  const search = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== '') {
      search.set(key, String(value));
    }
  });
  const response = await apiFetch(`${apiBase}/audits?${search.toString()}`);
  return readJson<CursorPageResponse<RuntimeAuditRecord>>(response);
}

function markControlPlaneUp(message: string) {
  controlPlaneHealth.state = 'up';
  controlPlaneHealth.message = message;
  controlPlaneHealth.checkedAt = new Date().toISOString();
}

function markControlPlaneDown(message: string) {
  controlPlaneHealth.state = 'down';
  controlPlaneHealth.message = message || '控制面不可用';
  controlPlaneHealth.checkedAt = new Date().toISOString();
}
