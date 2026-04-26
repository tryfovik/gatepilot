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
    host?: string;
    port?: number;
    protocol?: string;
    loadBalance?: string;
    healthCheckEnabled?: boolean;
    healthPath?: string;
  };
  candidate?: {
    enabled?: boolean;
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

export interface ReleaseResult {
  releaseId: string;
  version: string;
  phase: string;
  configShard?: string;
  createdAt?: string;
}

const apiBase = '/api/gatepilot/v1';

async function postJson<T>(path: string, payload: unknown): Promise<T> {
  const response = await fetch(`${apiBase}${path}`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(payload)
  });
  if (!response.ok) {
    throw new Error(`请求失败：${response.status}`);
  }
  const body = (await response.json()) as ApiResponse<T>;
  if (body.status !== 'success') {
    throw new Error(body.message || '请求失败');
  }
  return body.data;
}

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
  const response = await fetch(`${apiBase}/config-snapshots?${search.toString()}`);
  if (!response.ok) {
    throw new Error(`请求失败：${response.status}`);
  }
  const body = (await response.json()) as ApiResponse<CursorPageResponse<ConfigSnapshotSummaryResponse>>;
  if (body.status !== 'success') {
    throw new Error(body.message || '请求失败');
  }
  return body.data;
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
  const response = await fetch(`${apiBase}/diagnostics/route-catalog?${search.toString()}`);
  if (!response.ok) {
    throw new Error(`请求失败：${response.status}`);
  }
  const body = (await response.json()) as ApiResponse<RouteCatalogResponse>;
  if (body.status !== 'success') {
    throw new Error(body.message || '请求失败');
  }
  return body.data;
}

export async function diagnoseRoute(request: RouteDiagnosticsRequest): Promise<RouteDiagnosticsResponse> {
  return postJson<RouteDiagnosticsResponse>('/diagnostics/route', request);
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

export async function createRelease(request: CreateReleaseRequest): Promise<ReleaseResult> {
  return postJson<ReleaseResult>('/releases', request);
}
