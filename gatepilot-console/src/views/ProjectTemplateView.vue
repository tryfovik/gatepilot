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
        <div class="release-steps onboarding-steps">
          <button
            v-for="step in onboardingSteps"
            :key="step.key"
            class="release-step-card onboarding-step-card"
            :class="{ 'release-step-card--active': activeStep === step.key, 'release-step-card--done': step.done }"
            type="button"
            @click="activeStep = step.key"
          >
            <span>{{ step.index }}</span>
            <div>
              <strong>{{ step.title }}</strong>
              <small>{{ step.note }}</small>
            </div>
          </button>
        </div>

        <div v-show="activeStep === 'project'" class="template-section">
          <h3>项目基础</h3>
          <div class="form-subsection">
            <div class="form-section-title">
              <strong>基础信息</strong>
              <span>项目能被管理、发布和追踪所需的最小信息</span>
            </div>
            <div class="form-grid">
              <label>
                <span>命名空间 <em class="required-star" aria-label="必填">*</em> <i class="help-dot" :data-help="fieldHelp('namespace')" aria-hidden="true">i</i></span>
                <select v-model="form.namespace" class="select-input" required>
                  <option value="">请选择命名空间</option>
                  <option v-for="item in namespaceOptions" :key="item.value" :value="item.value">
                    {{ item.label }}
                  </option>
                </select>
              </label>
              <label>
                <span>项目名称 <em class="required-star" aria-label="必填">*</em> <i class="help-dot" :data-help="fieldHelp('projectName')" aria-hidden="true">i</i></span>
                <input v-model="form.projectName" class="search-input" required />
              </label>
              <label>
                <span>负责团队 <em class="required-star" aria-label="必填">*</em> <i class="help-dot" :data-help="fieldHelp('ownerTeam')" aria-hidden="true">i</i></span>
                <select v-model="form.ownerTeam" class="select-input" required>
                  <option value="">请选择团队</option>
                  <option v-for="item in teamOptions" :key="item.value" :value="item.value">
                    {{ item.label }}
                  </option>
                </select>
              </label>
              <label>
                <span>环境 <em class="required-star" aria-label="必填">*</em> <i class="help-dot" :data-help="fieldHelp('environment')" aria-hidden="true">i</i></span>
                <select v-model="form.environment" class="select-input" required>
                  <option value="">请选择环境</option>
                  <option v-for="item in environmentOptions" :key="item.value" :value="item.value" :disabled="item.enabled === false">
                    {{ item.label }}
                  </option>
                </select>
              </label>
            </div>
          </div>

          <div class="form-subsection">
            <div class="form-section-title">
              <strong>更多配置</strong>
              <span>不填时使用平台默认推荐</span>
            </div>
            <div class="form-grid form-grid--optional">
              <label>
                <span>展示名称 <i class="help-dot" :data-help="fieldHelp('displayName')" aria-hidden="true">i</i></span>
                <input v-model="form.displayName" class="search-input" />
              </label>
              <label>
                <span>配置分片 <i class="help-dot" :data-help="fieldHelp('configShard')" aria-hidden="true">i</i></span>
                <select v-model="form.configShard" class="select-input">
                  <option value="">默认分片</option>
                  <option v-for="item in configShardOptions" :key="item.value" :value="item.value" :disabled="item.enabled === false">
                    {{ item.label }}
                  </option>
                </select>
              </label>
              <label>
                <span>隔离组 <i class="help-dot" :data-help="fieldHelp('isolationGroup')" aria-hidden="true">i</i></span>
                <select v-model="form.isolationGroup" class="select-input">
                  <option value="">默认隔离组</option>
                  <option v-for="item in isolationGroupOptions" :key="item.value" :value="item.value">
                    {{ item.label }}
                  </option>
                </select>
              </label>
              <label>
                <span>流量等级 <i class="help-dot" :data-help="fieldHelp('trafficTier')" aria-hidden="true">i</i></span>
                <select v-model="form.trafficTier" class="select-input">
                  <option value="">请选择流量等级</option>
                  <option v-for="item in trafficTierOptions" :key="item.value" :value="item.value">
                    {{ item.label }}
                  </option>
                </select>
              </label>
            </div>
          </div>
        </div>

        <div v-show="activeStep === 'route'" class="template-section">
          <h3>路由与上游</h3>
          <div class="form-subsection">
            <div class="form-section-title">
              <strong>基础信息</strong>
              <span>让请求能被匹配并转发到稳定上游</span>
            </div>
            <div class="form-grid">
              <label>
                <span>入口域名 <em class="required-star" aria-label="必填">*</em> <i class="help-dot" :data-help="fieldHelp('routeHost')" aria-hidden="true">i</i></span>
                <select v-model="form.route.host" class="select-input" required>
                  <option value="">请选择入口域名</option>
                  <option v-for="item in ingressDomainOptions" :key="item.value" :value="item.value" :disabled="item.enabled === false">
                    {{ item.label }}
                  </option>
                </select>
              </label>
              <label>
                <span>路径前缀 <em class="required-star" aria-label="必填">*</em> <i class="help-dot" :data-help="fieldHelp('routePath')" aria-hidden="true">i</i></span>
                <input v-model="form.route.path" class="search-input" required />
              </label>
              <label>
                <span>实例来源 <em class="required-star" aria-label="必填">*</em> <i class="help-dot" :data-help="fieldHelp('discoveryType')" aria-hidden="true">i</i></span>
                <select v-model="form.upstream.discoveryType" class="select-input" required>
                  <option value="NACOS">Nacos 服务发现</option>
                  <option value="STATIC">固定地址</option>
                </select>
              </label>
              <template v-if="form.upstream.discoveryType === 'NACOS'">
                <label>
                  <span>注册中心 <em class="required-star" aria-label="必填">*</em> <i class="help-dot" :data-help="fieldHelp('registryCenterName')" aria-hidden="true">i</i></span>
                  <select v-model="form.upstream.registryCenterName" class="select-input" required>
                    <option value="">请选择注册中心</option>
                    <option v-for="item in registryCenterOptions" :key="item.value" :value="item.value" :disabled="item.enabled === false">
                      {{ item.label }}
                    </option>
                  </select>
                </label>
                <label>
                  <span>服务名 <em class="required-star" aria-label="必填">*</em> <i class="help-dot" :data-help="fieldHelp('serviceName')" aria-hidden="true">i</i></span>
                  <input v-model="form.upstream.serviceName" class="search-input" placeholder="例如 order-service" required />
                </label>
              </template>
              <template v-else>
                <label>
                  <span>上游地址 <em class="required-star" aria-label="必填">*</em> <i class="help-dot" :data-help="fieldHelp('upstreamHost')" aria-hidden="true">i</i></span>
                  <input v-model="form.upstream.host" class="search-input" required />
                </label>
                <label>
                  <span>上游端口 <em class="required-star" aria-label="必填">*</em> <i class="help-dot" :data-help="fieldHelp('upstreamPort')" aria-hidden="true">i</i></span>
                  <input v-model.number="form.upstream.port" class="search-input" type="number" min="1" max="65535" required />
                </label>
              </template>
              <label>
                <span>协议 <em class="required-star" aria-label="必填">*</em> <i class="help-dot" :data-help="fieldHelp('protocol')" aria-hidden="true">i</i></span>
                <select v-model="form.upstream.protocol" class="select-input" required>
                  <option v-for="item in protocolOptions" :key="item.value" :value="item.value" :disabled="!item.enabled">
                    {{ item.label }}
                  </option>
                </select>
              </label>
            </div>
          </div>

          <div class="form-subsection">
            <div class="form-section-title">
              <strong>更多配置</strong>
              <span>没有特殊转发规则时可以沿用默认值</span>
            </div>
            <div class="form-grid form-grid--optional">
              <label>
                <span>负载均衡 <i class="help-dot" :data-help="fieldHelp('loadBalance')" aria-hidden="true">i</i></span>
                <select v-model="form.upstream.loadBalance" class="select-input">
                  <option v-for="item in loadBalanceOptions" :key="item.value" :value="item.value" :disabled="!item.enabled">
                    {{ item.label }}
                  </option>
                </select>
              </label>
              <label>
                <span>健康检查路径 <i class="help-dot" :data-help="fieldHelp('healthPath')" aria-hidden="true">i</i></span>
                <input v-model="form.upstream.healthPath" class="search-input" />
              </label>
              <template v-if="form.upstream.discoveryType === 'NACOS'">
                <label>
                  <span>Nacos 分组 <i class="help-dot" :data-help="fieldHelp('discoveryGroup')" aria-hidden="true">i</i></span>
                  <input v-model="form.upstream.discoveryGroup" class="search-input" placeholder="默认使用注册中心配置" />
                </label>
                <label>
                  <span>命名空间覆盖 <i class="help-dot" :data-help="fieldHelp('discoveryNamespace')" aria-hidden="true">i</i></span>
                  <input v-model="form.upstream.discoveryNamespace" class="search-input" placeholder="默认使用注册中心配置" />
                </label>
                <label>
                  <span>集群</span>
                  <input v-model="stableClustersText" class="search-input" placeholder="多个用英文逗号分隔" />
                </label>
                <label>
                  <span>元数据筛选 <i class="help-dot" :data-help="fieldHelp('metadataSelector')" aria-hidden="true">i</i></span>
                  <input v-model="stableMetadataText" class="search-input" placeholder="version=stable,zone=hz" />
                </label>
              </template>
              <label class="check-row">
                <input v-model="form.route.stripPrefix" type="checkbox" />
                <span>转发时去除路径前缀 <i class="help-dot" :data-help="fieldHelp('stripPrefix')" aria-hidden="true">i</i></span>
              </label>
              <label class="check-row">
                <input v-model="form.upstream.healthCheckEnabled" type="checkbox" />
                <span>启用上游健康检查 <i class="help-dot" :data-help="fieldHelp('healthCheckEnabled')" aria-hidden="true">i</i></span>
              </label>
            </div>
          </div>
        </div>

        <div v-show="activeStep === 'governance'" class="template-section">
          <h3>治理与蓝绿发布</h3>
          <div class="form-subsection">
            <div class="form-section-title">
              <strong>能力开关</strong>
              <span>默认只转发稳定版本，需要治理能力时再打开</span>
            </div>
            <div class="feature-toggle-grid">
              <label class="feature-toggle">
                <input v-model="form.governance.rateLimitEnabled" type="checkbox" />
                <span>
                  <strong>限流 <i class="help-dot" :data-help="fieldHelp('rateLimitEnabled')" aria-hidden="true">i</i></strong>
                </span>
              </label>
              <label class="feature-toggle">
                <input v-model="form.governance.retryEnabled" type="checkbox" />
                <span>
                  <strong>重试 <i class="help-dot" :data-help="fieldHelp('retryEnabled')" aria-hidden="true">i</i></strong>
                </span>
              </label>
              <label class="feature-toggle">
                <input v-model="form.release.enabled" type="checkbox" />
                <span>
                  <strong>灰度 <i class="help-dot" :data-help="fieldHelp('releaseEnabled')" aria-hidden="true">i</i></strong>
                </span>
              </label>
              <label class="feature-toggle">
                <input v-model="form.candidate.enabled" type="checkbox" />
                <span>
                  <strong>候选版本 <i class="help-dot" :data-help="fieldHelp('candidateEnabled')" aria-hidden="true">i</i></strong>
                </span>
              </label>
              <label class="feature-toggle">
                <input v-model="form.auth.anonymousAllowed" type="checkbox" />
                <span>
                  <strong>匿名访问 <i class="help-dot" :data-help="fieldHelp('anonymousAllowed')" aria-hidden="true">i</i></strong>
                </span>
              </label>
            </div>
          </div>

          <div v-if="form.governance.rateLimitEnabled" class="form-subsection capability-section">
            <div class="form-section-title">
              <strong>限流参数</strong>
              <span>保护上游服务，防止突发流量把后端打满</span>
            </div>
            <div class="form-grid form-grid--optional">
              <label>
                <span>限流 QPS <i class="help-dot" :data-help="fieldHelp('requestsPerSecond')" aria-hidden="true">i</i></span>
                <input v-model.number="form.governance.requestsPerSecond" class="search-input" type="number" min="1" />
              </label>
              <label>
                <span>突发容量 <i class="help-dot" :data-help="fieldHelp('burstCapacity')" aria-hidden="true">i</i></span>
                <input v-model.number="form.governance.burstCapacity" class="search-input" type="number" min="1" />
              </label>
            </div>
          </div>

          <div v-if="form.governance.retryEnabled" class="form-subsection capability-section">
            <div class="form-section-title">
              <strong>重试参数</strong>
              <span>只建议用于幂等接口，避免放大写请求</span>
            </div>
            <div class="form-grid form-grid--optional">
              <label>
                <span>最大重试次数 <i class="help-dot" :data-help="fieldHelp('maxAttempts')" aria-hidden="true">i</i></span>
                <input v-model.number="form.governance.maxAttempts" class="search-input" type="number" min="1" />
              </label>
            </div>
          </div>

          <div v-if="form.candidate.enabled" class="form-subsection capability-section">
            <div class="form-section-title">
              <strong>候选上游</strong>
              <span>用于绿环境、灰度实例或新版本服务</span>
            </div>
            <div class="form-grid form-grid--optional">
              <label>
                <span>实例来源 <i class="help-dot" :data-help="fieldHelp('candidateDiscoveryType')" aria-hidden="true">i</i></span>
                <select v-model="form.candidate.discoveryType" class="select-input">
                  <option value="NACOS">Nacos 服务发现</option>
                  <option value="STATIC">固定地址</option>
                </select>
              </label>
              <template v-if="form.candidate.discoveryType === 'NACOS'">
                <label>
                  <span>注册中心 <i class="help-dot" :data-help="fieldHelp('registryCenterName')" aria-hidden="true">i</i></span>
                  <select v-model="form.candidate.registryCenterName" class="select-input">
                    <option value="">跟随稳定上游</option>
                    <option v-for="item in registryCenterOptions" :key="item.value" :value="item.value" :disabled="item.enabled === false">
                      {{ item.label }}
                    </option>
                  </select>
                </label>
                <label>
                  <span>候选服务名 <i class="help-dot" :data-help="fieldHelp('candidateServiceName')" aria-hidden="true">i</i></span>
                  <input v-model="form.candidate.serviceName" class="search-input" placeholder="例如 order-service-green" />
                </label>
                <label>
                  <span>Nacos 分组</span>
                  <input v-model="form.candidate.discoveryGroup" class="search-input" placeholder="默认跟随稳定上游" />
                </label>
                <label>
                  <span>元数据筛选 <i class="help-dot" :data-help="fieldHelp('metadataSelector')" aria-hidden="true">i</i></span>
                  <input v-model="candidateMetadataText" class="search-input" placeholder="version=green" />
                </label>
              </template>
              <template v-else>
                <label>
                  <span>候选上游 <i class="help-dot" :data-help="fieldHelp('candidateHost')" aria-hidden="true">i</i></span>
                  <input v-model="form.candidate.host" class="search-input" />
                </label>
                <label>
                  <span>候选端口 <i class="help-dot" :data-help="fieldHelp('candidatePort')" aria-hidden="true">i</i></span>
                  <input v-model.number="form.candidate.port" class="search-input" type="number" min="1" max="65535" />
                </label>
              </template>
            </div>
          </div>

          <div v-if="form.release.enabled" class="form-subsection capability-section">
            <div class="form-section-title">
              <strong>发布策略</strong>
              <span>控制稳定版本和候选版本怎么切流</span>
            </div>
            <div class="form-grid form-grid--optional">
              <label>
                <span>发布策略 <i class="help-dot" :data-help="fieldHelp('releaseStrategy')" aria-hidden="true">i</i></span>
                <select v-model="form.release.strategy" class="select-input">
                  <option v-for="item in releaseStrategyOptions" :key="item.value" :value="item.value" :disabled="!item.enabled">
                    {{ item.label }}
                  </option>
                </select>
              </label>
              <label>
                <span>候选权重 <i class="help-dot" :data-help="fieldHelp('candidateWeight')" aria-hidden="true">i</i></span>
                <input v-model.number="form.release.candidateWeight" class="search-input" type="number" min="0" max="100" />
              </label>
              <label>
                <span>染色 Header <i class="help-dot" :data-help="fieldHelp('colorHeader')" aria-hidden="true">i</i></span>
                <input v-model="form.release.colorHeader" class="search-input" />
              </label>
              <label>
                <span>候选染色值 <i class="help-dot" :data-help="fieldHelp('candidateColor')" aria-hidden="true">i</i></span>
                <input v-model="form.release.candidateColor" class="search-input" />
              </label>
            </div>
          </div>

          <div v-if="!form.auth.anonymousAllowed" class="form-subsection capability-section">
            <div class="form-section-title">
              <strong>访问控制</strong>
              <span>未开启匿名访问时，请选择网关认证方式</span>
            </div>
            <div class="form-grid form-grid--optional">
              <label>
                <span>认证类型 <i class="help-dot" :data-help="fieldHelp('authType')" aria-hidden="true">i</i></span>
                <select v-model="form.auth.type" class="select-input">
                  <option v-for="item in authOptions" :key="item.value" :value="item.value" :disabled="!item.enabled">
                  {{ item.label }}
                </option>
              </select>
            </label>
            </div>
          </div>
          <div class="release-policy-layout">
            <section class="diagnostic-block">
              <h3>稳定版本</h3>
              <p class="resource-subtitle">默认流量会进入稳定上游，稳定版本通常承载线上主流量</p>
              <div class="key-value-grid">
                <span>上游</span>
                <strong>{{ upstreamSummary(form.upstream) }}</strong>
                <span>协议</span>
                <strong>{{ form.upstream.protocol || '-' }}</strong>
              </div>
            </section>
            <section v-if="form.candidate.enabled" class="diagnostic-block">
              <h3>候选版本</h3>
              <p class="resource-subtitle">候选上游用于绿环境或灰度版本，关闭后不会生成候选资源</p>
              <div class="key-value-grid">
                <span>是否创建</span>
                <strong>{{ form.candidate.enabled ? '创建候选上游' : '不创建' }}</strong>
                <span>上游</span>
                <strong>{{ upstreamSummary(form.candidate) }}</strong>
              </div>
            </section>
            <section v-if="form.release.enabled" class="diagnostic-block">
              <h3>切流规则</h3>
              <p class="resource-subtitle">蓝绿通常一键切到候选版本，灰度会按权重和染色 Header 命中候选版本</p>
              <div class="key-value-grid">
                <span>策略</span>
                <strong>{{ form.release.strategy || '-' }}</strong>
                <span>权重</span>
                <strong>{{ form.release.candidateWeight ?? '-' }}%</strong>
                <span>染色</span>
                <strong>{{ form.release.colorHeader || '-' }} = {{ form.release.candidateColor || '-' }}</strong>
              </div>
            </section>
          </div>
        </div>

        <div class="form-actions onboarding-actions">
          <button class="ghost-button" type="button" :disabled="activeStepIndex <= 0" @click="previousStep">上一步</button>
          <button class="ghost-button" type="button" :disabled="activeStepIndex >= onboardingStepKeys.length - 1" @click="nextStep">下一步</button>
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
              <h2>发布摘要</h2>
              <p>{{ preview?.configShard || '默认分片' }}</p>
            </div>
          </div>
          <div v-if="releaseSummary.length" class="key-value-grid">
            <template v-for="item in releaseSummary" :key="item.label">
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </template>
          </div>
          <div v-else class="state-box state-box--compact">预览后展示发布摘要</div>
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
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue';
import { AlertTriangle, CheckCircle2, Eye, Rocket, Save } from 'lucide-vue-next';
import MetricStrip from '../components/MetricStrip.vue';
import ResourceDetailDrawer from '../components/ResourceDetailDrawer.vue';
import StatusBadge from '../components/StatusBadge.vue';
import {
  CreateReleaseRequest,
  ProjectTemplateApplyResponse,
  ProjectTemplateDefaultsResponse,
  ProjectTemplateDryRunResponse,
  ProjectTemplatePreviewResponse,
  ProjectTemplateRenderedResource,
  ProjectTemplateRenderRequest,
  ReleaseResponse,
  applyProjectTemplate,
  createRelease,
  dryRunProjectTemplate,
  getProjectTemplateDefaults,
  listResources,
  previewProjectTemplate
} from '../api/client';
import { formatTime } from '../utils/format';
import { notifyError, notifySuccess } from '../utils/feedback';
import { getGlobalNamespace, onGlobalNamespaceChange, setGlobalNamespace } from '../utils/namespace';

type StatusTone = 'success' | 'warning' | 'danger' | 'info' | 'neutral';
type TemplateAction = ProjectTemplateRenderedResource['action'];
type OptionItem = { value: string; label: string; enabled?: boolean };
type TemplateForm = ProjectTemplateRenderRequest & {
  route: NonNullable<ProjectTemplateRenderRequest['route']>;
  upstream: NonNullable<ProjectTemplateRenderRequest['upstream']>;
  candidate: NonNullable<ProjectTemplateRenderRequest['candidate']>;
  governance: NonNullable<ProjectTemplateRenderRequest['governance']>;
  release: NonNullable<ProjectTemplateRenderRequest['release']>;
  auth: NonNullable<ProjectTemplateRenderRequest['auth']>;
};

const FALLBACK_PROTOCOL_OPTIONS: OptionItem[] = [
  { value: 'HTTP', label: 'HTTP', enabled: true },
  { value: 'HTTPS', label: 'HTTPS', enabled: true }
];
const FALLBACK_LOAD_BALANCE_OPTIONS: OptionItem[] = [
  { value: 'ROUND_ROBIN', label: '轮询', enabled: true },
  { value: 'WEIGHTED_ROUND_ROBIN', label: '加权轮询', enabled: true },
  { value: 'RANDOM', label: '随机', enabled: true },
  { value: 'LEAST_CONNECTIONS', label: '最少连接', enabled: false },
  { value: 'CONSISTENT_HASH', label: '一致性哈希', enabled: false }
];
const FALLBACK_RELEASE_STRATEGY_OPTIONS: OptionItem[] = [
  { value: 'BLUE_GREEN', label: '蓝绿发布', enabled: true },
  { value: 'CANARY', label: '灰度发布', enabled: true },
  { value: 'TRAFFIC_SPLIT', label: '固定权重', enabled: true },
  { value: 'SHADOW', label: '影子流量', enabled: true }
];
const FALLBACK_AUTH_OPTIONS: OptionItem[] = [
  { value: 'NONE', label: '不启用', enabled: true },
  { value: 'API_KEY', label: 'API Key', enabled: true },
  { value: 'JWT', label: 'JWT', enabled: true },
  { value: 'OAUTH2', label: 'OAuth2', enabled: true },
  { value: 'BASIC', label: 'Basic', enabled: true },
  { value: 'MTLS', label: '双向 TLS', enabled: true }
];

interface PlatformResource {
  metadata?: {
    name?: string;
  };
  spec?: Record<string, unknown> & {
    displayName?: string;
    host?: string;
    ownerTeam?: string;
    environment?: string;
    tier?: string;
    defaultConfigShard?: string;
    defaultIsolationGroup?: string;
    acceptingProjects?: boolean;
    acceptingUpstreams?: boolean;
    recommendedConfigShard?: string;
    recommendedIsolationGroup?: string;
  };
}

const form = reactive<TemplateForm>(emptyForm());

const activeStep = ref<'project' | 'route' | 'governance'>('project');
const loadingAction = ref('');
const error = ref('');
const notice = ref('');
const defaults = ref<ProjectTemplateDefaultsResponse | null>(null);
const preview = ref<ProjectTemplatePreviewResponse | null>(null);
const dryRun = ref<ProjectTemplateDryRunResponse | null>(null);
const applyResult = ref<ProjectTemplateApplyResponse | null>(null);
const releaseResult = ref<ReleaseResponse | null>(null);
const selectedResource = ref<ProjectTemplateRenderedResource | null>(null);
const platformResources = reactive({
  namespaces: [] as PlatformResource[],
  teams: [] as PlatformResource[],
  environments: [] as PlatformResource[],
  configShards: [] as PlatformResource[],
  isolationGroups: [] as PlatformResource[],
  trafficTiers: [] as PlatformResource[],
  ingressDomains: [] as PlatformResource[],
  registryCenters: [] as PlatformResource[]
});

const busy = computed(() => Boolean(loadingAction.value));
let unsubscribeNamespace: (() => void) | null = null;

const namespaceOptions = computed(() => platformResources.namespaces.map((item) => resourceOption(item)));
const teamOptions = computed(() => platformResources.teams.map((item) => resourceOption(item)));
const environmentOptions = computed<OptionItem[]>(() => {
  const resourceOptions = platformResources.environments.map((item) => resourceOption(item));
  return resourceOptions.length ? resourceOptions : defaults.value?.environments ?? [];
});
const configShardOptions = computed(() => platformResources.configShards.map((item) => resourceOption(item)));
const isolationGroupOptions = computed(() => platformResources.isolationGroups.map((item) => resourceOption(item)));
const trafficTierOptions = computed(() => platformResources.trafficTiers.map((item) => resourceOption(item)));
const ingressDomainOptions = computed(() =>
  platformResources.ingressDomains.map((item) => ({
    value: String(item.spec?.host || item.metadata?.name || ''),
    label: item.spec?.displayName ? `${item.spec.displayName} / ${item.spec.host || item.metadata?.name}` : String(item.spec?.host || item.metadata?.name || ''),
    enabled: item.spec?.acceptingProjects !== false
  })).filter((item) => item.value)
);
const registryCenterOptions = computed(() =>
  platformResources.registryCenters.map((item) => ({
    value: String(item.metadata?.name || ''),
    label: item.spec?.displayName ? `${item.spec.displayName} / ${item.metadata?.name}` : String(item.metadata?.name || ''),
    enabled: item.spec?.acceptingUpstreams !== false
  })).filter((item) => item.value)
);
const protocolOptions = computed(() => withFallbackOptions(defaults.value?.protocols, FALLBACK_PROTOCOL_OPTIONS));
const loadBalanceOptions = computed(() => withFallbackOptions(defaults.value?.loadBalances, FALLBACK_LOAD_BALANCE_OPTIONS));
const releaseStrategyOptions = computed(() => withFallbackOptions(defaults.value?.releaseStrategies, FALLBACK_RELEASE_STRATEGY_OPTIONS));
const authOptions = computed(() => withFallbackOptions(defaults.value?.authTypes, FALLBACK_AUTH_OPTIONS));

const diffMetrics = computed(() => {
  const diff = preview.value?.diff;
  return [
    { label: '新增资源', value: String(diff?.createCount ?? 0), note: 'CREATE' },
    { label: '更新资源', value: String(diff?.updateCount ?? 0), note: 'UPDATE' },
    { label: '未变化', value: String(diff?.unchangedCount ?? 0), note: 'UNCHANGED' },
    { label: '资源总数', value: String(preview.value?.resources.length ?? 0), note: 'rendered' }
  ];
});

const releaseSummary = computed(() => {
  const request = preview.value?.releaseRequest;
  if (!request) {
    return [];
  }
  return [
    { label: '命名空间', value: request.namespace || '-' },
    { label: '项目', value: request.projectName || '-' },
    { label: '配置分片', value: request.configShard || preview.value?.configShard || '默认分片' },
    { label: '发布说明', value: request.description || '-' },
    { label: '创建人', value: request.createdBy || '-' },
    { label: '资源数量', value: String(request.resourceRefs?.length ?? preview.value?.resources.length ?? 0) },
    { label: '目标节点', value: summarizeValue(request.targetNodeSelector) }
  ];
});

const stableClustersText = computed({
  get: () => (form.upstream.clusters || []).join(','),
  set: (value: string) => {
    form.upstream.clusters = splitCsv(value);
  }
});

const stableMetadataText = computed({
  get: () => formatKeyValuePairs(form.upstream.metadataSelector),
  set: (value: string) => {
    form.upstream.metadataSelector = parseKeyValuePairs(value);
  }
});

const candidateMetadataText = computed({
  get: () => formatKeyValuePairs(form.candidate.metadataSelector),
  set: (value: string) => {
    form.candidate.metadataSelector = parseKeyValuePairs(value);
  }
});

const onboardingStepKeys = ['project', 'route', 'governance'] as const;
const activeStepIndex = computed(() => onboardingStepKeys.indexOf(activeStep.value));
const onboardingSteps = computed(() => [
  {
    key: 'project' as const,
    index: '1',
    title: '项目基础',
    note: form.projectName || '命名空间、团队、环境和容量',
    done: Boolean(form.namespace && form.projectName && form.ownerTeam && form.environment)
  },
  {
    key: 'route' as const,
    index: '2',
    title: '路由上游',
    note: form.route.path || '入口域名、路径和后端服务',
    done: Boolean(form.route.host && form.route.path && stableUpstreamReady() && form.upstream.protocol)
  },
  {
    key: 'governance' as const,
    index: '3',
    title: '治理发布',
    note: form.release.enabled ? '灰度/蓝绿已启用' : '稳定版本直连',
    done: Boolean(
      form.governance.rateLimitEnabled ||
      form.governance.retryEnabled ||
      form.release.enabled ||
      form.candidate.enabled ||
      form.auth.anonymousAllowed
    )
  }
]);

onMounted(loadInitialData);
onMounted(() => {
  unsubscribeNamespace = onGlobalNamespaceChange((namespace) => {
    if (namespace && form.namespace !== namespace) {
      form.namespace = namespace;
    }
  });
});

onUnmounted(() => {
  unsubscribeNamespace?.();
});

watch(
  () => form.namespace,
  (namespace) => {
    if (namespace) {
      setGlobalNamespace(namespace);
    }
    const resource = platformResources.namespaces.find((item) => item.metadata?.name === namespace);
    applyPlatformDefaults(resource);
  }
);

watch(
  () => form.environment,
  (environment) => {
    const resource = platformResources.environments.find((item) => item.metadata?.name === environment);
    applyPlatformDefaults(resource);
  }
);

watch(
  () => form.trafficTier,
  (trafficTier) => {
    const resource = platformResources.trafficTiers.find((item) => item.metadata?.name === trafficTier);
    if (!form.configShard && resource?.spec?.recommendedConfigShard) {
      form.configShard = resource.spec.recommendedConfigShard;
    }
    if (!form.isolationGroup && resource?.spec?.recommendedIsolationGroup) {
      form.isolationGroup = resource.spec.recommendedIsolationGroup;
    }
  }
);

watch(
  () => form.release.strategy,
  (strategy, previousStrategy) => {
    if (strategy === 'BLUE_GREEN') {
      form.release.candidateWeight = 100;
      form.release.enabled = true;
      form.candidate.enabled = true;
      return;
    }
    if (previousStrategy === 'BLUE_GREEN' && form.release.candidateWeight === 100) {
      form.release.candidateWeight = 10;
    }
  }
);

watch(
  () => form.release.enabled,
  (enabled) => {
    if (enabled) {
      form.candidate.enabled = true;
      if (!form.release.strategy) {
        form.release.strategy = 'TRAFFIC_SPLIT';
      }
    }
  }
);

watch(
  () => form.upstream.discoveryType,
  (discoveryType) => {
    if (discoveryType === 'NACOS' && !form.upstream.registryCenterName && registryCenterOptions.value.length) {
      form.upstream.registryCenterName = registryCenterOptions.value[0].value;
    }
    if (!form.candidate.discoveryType || form.candidate.discoveryType === 'STATIC') {
      form.candidate.discoveryType = discoveryType;
    }
  }
);

watch(
  () => form.upstream.registryCenterName,
  (registryCenterName) => {
    if (!form.candidate.registryCenterName) {
      form.candidate.registryCenterName = registryCenterName;
    }
  }
);

watch(
  () => form.candidate.enabled,
  (enabled) => {
    if (!enabled && form.release.enabled) {
      form.release.enabled = false;
    }
  }
);

async function loadInitialData() {
  await runAction('defaults', async () => {
    defaults.value = await getProjectTemplateDefaults();
    await loadPlatformResources();
    replaceForm(defaults.value.values);
    applyFallbackOptions();
  });
}

async function loadPlatformResources() {
  const [
    namespaces,
    teams,
    environments,
    configShards,
    isolationGroups,
    trafficTiers,
    ingressDomains,
    registryCenters
  ] = await Promise.all([
    listResources<PlatformResource>('namespaces', 'system', 200),
    listResources<PlatformResource>('teams', 'system', 200),
    listResources<PlatformResource>('environments', 'system', 200),
    listResources<PlatformResource>('config-shards', 'system', 200),
    listResources<PlatformResource>('isolation-groups', 'system', 200),
    listResources<PlatformResource>('traffic-tiers', 'system', 200),
    listResources<PlatformResource>('ingress-domains', 'system', 200),
    listResources<PlatformResource>('registry-centers', 'system', 200)
  ]);
  platformResources.namespaces = namespaces.items;
  platformResources.teams = teams.items;
  platformResources.environments = environments.items;
  platformResources.configShards = configShards.items;
  platformResources.isolationGroups = isolationGroups.items;
  platformResources.trafficTiers = trafficTiers.items;
  platformResources.ingressDomains = ingressDomains.items;
  platformResources.registryCenters = registryCenters.items;
}

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
    if (action !== 'defaults') {
      notifySuccess(actionTitle(action), notice.value || '操作已完成');
    }
  } catch (err) {
    error.value = err instanceof Error ? err.message : '操作失败';
    notifyError(`${actionTitle(action)}失败`, error.value);
  } finally {
    loadingAction.value = '';
  }
}

function actionTitle(action: string) {
  const labels: Record<string, string> = {
    preview: '预览',
    dryRun: '校验',
    apply: '保存资源',
    publish: '保存并发布',
    defaults: '加载默认配置'
  };
  return labels[action] || '操作';
}

function currentRequest(): ProjectTemplateRenderRequest {
  validateTemplateRequired();
  const request = JSON.parse(JSON.stringify(form)) as ProjectTemplateRenderRequest;
  if (request.candidate?.discoveryType === 'NACOS') {
    request.candidate.registryCenterName ||= request.upstream?.registryCenterName;
    request.candidate.serviceName ||= request.upstream?.serviceName;
    request.candidate.discoveryNamespace ||= request.upstream?.discoveryNamespace;
    request.candidate.discoveryGroup ||= request.upstream?.discoveryGroup;
    request.candidate.clusters = request.candidate.clusters?.length ? request.candidate.clusters : request.upstream?.clusters;
  }
  return request;
}

function validateTemplateRequired() {
  const missingFields = [
    !form.namespace ? '命名空间' : '',
    !form.projectName ? '项目名称' : '',
    !form.ownerTeam ? '负责团队' : '',
    !form.environment ? '环境' : '',
    !form.route.host ? '入口域名' : '',
    !form.route.path ? '路径前缀' : '',
    form.upstream.discoveryType === 'NACOS' && !form.upstream.registryCenterName ? '注册中心' : '',
    form.upstream.discoveryType === 'NACOS' && !form.upstream.serviceName ? '服务名' : '',
    form.upstream.discoveryType !== 'NACOS' && !form.upstream.host ? '上游地址' : '',
    form.upstream.discoveryType !== 'NACOS' && !form.upstream.port ? '上游端口' : '',
    form.candidate.enabled && form.candidate.discoveryType === 'NACOS' && !candidateServiceName() ? '候选服务名' : '',
    form.candidate.enabled && form.candidate.discoveryType !== 'NACOS' && !form.candidate.host ? '候选上游地址' : '',
    form.candidate.enabled && form.candidate.discoveryType !== 'NACOS' && !form.candidate.port ? '候选端口' : '',
    !form.upstream.protocol ? '协议' : ''
  ].filter(Boolean);
  if (missingFields.length) {
    throw new Error(`请先填写必填信息：${missingFields.join('、')}`);
  }
}

function stableUpstreamReady() {
  if (form.upstream.discoveryType === 'NACOS') {
    return Boolean(form.upstream.registryCenterName && form.upstream.serviceName);
  }
  return Boolean(form.upstream.host && form.upstream.port);
}

function candidateServiceName() {
  return form.candidate.serviceName || form.upstream.serviceName;
}

function upstreamSummary(value: { discoveryType?: string; registryCenterName?: string; serviceName?: string; host?: string; port?: number }) {
  if (value.discoveryType === 'NACOS') {
    return `Nacos ${value.registryCenterName || form.upstream.registryCenterName || '-'} / ${value.serviceName || form.upstream.serviceName || '-'}`;
  }
  return `${value.host || '-'}:${value.port || '-'}`;
}

function splitCsv(value: unknown) {
  return String(value || '')
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean);
}

function parseKeyValuePairs(value: unknown) {
  return splitCsv(value).reduce<Record<string, string>>((result, item) => {
    const index = item.indexOf('=');
    if (index <= 0) {
      return result;
    }
    const key = item.slice(0, index).trim();
    const val = item.slice(index + 1).trim();
    if (key && val) {
      result[key] = val;
    }
    return result;
  }, {});
}

function formatKeyValuePairs(value?: Record<string, string>) {
  if (!value) {
    return '';
  }
  return Object.entries(value)
    .map(([key, val]) => `${key}=${val}`)
    .join(',');
}

function replaceForm(values: ProjectTemplateRenderRequest) {
  Object.assign(form, normalizeForm(values));
}

function applyFallbackOptions() {
  if (!form.namespace && namespaceOptions.value.length) {
    form.namespace = namespaceOptions.value[0].value;
  }
  if (!form.ownerTeam && teamOptions.value.length) {
    form.ownerTeam = teamOptions.value[0].value;
  }
  if (!form.environment && environmentOptions.value.length) {
    form.environment = environmentOptions.value[0].value;
  }
  if (!form.configShard && configShardOptions.value.length) {
    form.configShard = configShardOptions.value[0].value;
  }
  if (!form.isolationGroup && isolationGroupOptions.value.length) {
    form.isolationGroup = isolationGroupOptions.value[0].value;
  }
  if (!form.trafficTier && trafficTierOptions.value.length) {
    form.trafficTier = trafficTierOptions.value[0].value;
  }
  if (!form.route.host && ingressDomainOptions.value.length) {
    form.route.host = ingressDomainOptions.value[0].value;
  }
  if (registryCenterOptions.value.length) {
    if (!form.upstream.discoveryType || form.upstream.discoveryType === 'STATIC') {
      form.upstream.discoveryType = 'NACOS';
    }
    if (!form.upstream.registryCenterName) {
      form.upstream.registryCenterName = registryCenterOptions.value[0].value;
    }
    if (!form.candidate.discoveryType || form.candidate.discoveryType === 'STATIC') {
      form.candidate.discoveryType = form.upstream.discoveryType;
    }
    if (!form.candidate.registryCenterName) {
      form.candidate.registryCenterName = form.upstream.registryCenterName;
    }
  }
}

function applyPlatformDefaults(resource?: PlatformResource) {
  if (!resource?.spec) {
    return;
  }
  if (!form.ownerTeam && resource.spec.ownerTeam) {
    form.ownerTeam = resource.spec.ownerTeam;
  }
  if (!form.environment && resource.spec.environment) {
    form.environment = resource.spec.environment;
  }
  if (!form.configShard && resource.spec.defaultConfigShard) {
    form.configShard = resource.spec.defaultConfigShard;
  }
  if (!form.isolationGroup && resource.spec.defaultIsolationGroup) {
    form.isolationGroup = resource.spec.defaultIsolationGroup;
  }
}

function resourceOption(item: PlatformResource): OptionItem {
  const value = String(item.metadata?.name || '');
  const label = item.spec?.displayName ? `${item.spec.displayName} / ${value}` : value;
  return {
    value,
    label,
    enabled: item.spec?.acceptingProjects !== false
  };
}

function withFallbackOptions(options: OptionItem[] | undefined, fallback: OptionItem[]) {
  return options?.length ? options : fallback;
}

function normalizeForm(values: ProjectTemplateRenderRequest = {}): TemplateForm {
  const empty = emptyForm();
  return {
    ...empty,
    ...values,
    route: { ...empty.route, ...values.route },
    upstream: { ...empty.upstream, ...values.upstream },
    candidate: { ...empty.candidate, ...values.candidate },
    governance: { ...empty.governance, ...values.governance },
    release: { ...empty.release, ...values.release },
    auth: { ...empty.auth, ...values.auth }
  };
}

function emptyForm(): TemplateForm {
  return {
    namespace: getGlobalNamespace('default'),
    projectName: '',
    displayName: '',
    ownerTeam: '',
    environment: '',
    trafficTier: '',
    configShard: '',
    isolationGroup: '',
    route: {
      host: '',
      path: '',
      stripPrefix: false,
      methods: []
    },
    upstream: {
      discoveryType: 'STATIC',
      registryCenterName: '',
      serviceName: '',
      discoveryNamespace: '',
      discoveryGroup: '',
      clusters: [],
      metadataSelector: {},
      host: '',
      protocol: 'HTTP',
      loadBalance: 'ROUND_ROBIN',
      healthCheckEnabled: false,
      healthPath: ''
    },
    candidate: {
      enabled: false,
      discoveryType: 'STATIC',
      registryCenterName: '',
      serviceName: '',
      discoveryNamespace: '',
      discoveryGroup: '',
      clusters: [],
      metadataSelector: {},
      host: ''
    },
    governance: {
      rateLimitEnabled: false,
      retryEnabled: false
    },
    release: {
      enabled: false,
      strategy: 'TRAFFIC_SPLIT',
      colorHeader: '',
      candidateColor: ''
    },
    auth: {
      type: 'NONE',
      anonymousAllowed: false
    }
  };
}

function previousStep() {
  const previous = onboardingStepKeys[activeStepIndex.value - 1];
  if (previous) {
    activeStep.value = previous;
  }
}

function nextStep() {
  const next = onboardingStepKeys[activeStepIndex.value + 1];
  if (next) {
    activeStep.value = next;
  }
}

function fieldHelp(key: string) {
  const helps: Record<string, string> = {
    namespace: '命名空间用于隔离项目、路由、策略、发布和审计数据，生产环境建议按业务域或租户拆分',
    projectName: '项目在命名空间内的唯一标识，建议使用小写字母、数字和中划线，后续路由、上游、策略都会挂到这个项目下',
    displayName: '给控制台用户看的中文名称，不参与网关运行匹配',
    ownerTeam: '负责该项目接入、发布和故障响应的团队，来自平台配置里的团队资源',
    environment: '项目运行环境，影响默认分片、隔离组和发布约束',
    configShard: '配置分片决定 PublishedConfig 如何生成和下发，大规模项目可以按分片降低单份配置体积',
    isolationGroup: '隔离组对应一组 agent / proxy 副本池，高流量或核心链路可以放到独立副本池',
    trafficTier: '流量等级用于容量推荐，例如普通、高流量、核心链路，会辅助带出推荐分片和隔离组',
    routeHost: '入口域名是客户端请求进入网关时携带的 Host，建议从已授权域名中选择',
    routePath: '路径前缀用于匹配请求路径，例如 /api/order，命中后转发到上游服务',
    discoveryType: '推荐使用 Nacos 服务发现，网关会订阅服务实例并交给 Spring LoadBalancer 转发，不需要维护一长串 IP',
    registryCenterName: '注册中心来自平台配置，统一保存 Nacos 地址和认证信息，上游只引用它和服务名',
    serviceName: 'Nacos 服务名，业务服务扩缩容后实例列表由注册中心动态维护',
    discoveryGroup: 'Nacos 分组，不填时使用注册中心默认分组',
    discoveryNamespace: '仅在需要覆盖注册中心默认命名空间时填写',
    metadataSelector: '用于按 Nacos 实例元数据筛选稳定或候选实例，例如 version=stable',
    upstreamHost: '稳定版本上游地址，可以是 IP、域名或服务名，网关会把命中的请求转发过去',
    upstreamPort: '稳定版本上游端口，必须是 1-65535',
    protocol: '网关转发到上游时使用的协议，通常 HTTP 即可，需要 TLS 时选择 HTTPS',
    loadBalance: '多个上游端点时使用的负载均衡方式，优先复用 Spring LoadBalancer 能力',
    healthPath: '健康检查路径用于判断端点是否可转发，异常端点不应继续承接业务流量',
    stripPrefix: '开启后转发给上游前会去掉路由前缀，例如 /api/order/list 转成 /list',
    healthCheckEnabled: '开启后 proxy 会依据健康检查结果判断上游端点可用性',
    rateLimitEnabled: '开启后按配置的 QPS 和突发容量保护上游服务',
    retryEnabled: '开启后对可重试请求进行失败重试，只建议用于幂等接口',
    releaseEnabled: '开启后生成灰度或蓝绿发布策略，支持权重和染色切流',
    candidateEnabled: '开启后生成候选版本上游，用于绿环境、灰度实例或新版本服务',
    candidateDiscoveryType: '候选版本可以和稳定版本共用 Nacos 服务，也可以指向单独的候选服务',
    candidateServiceName: '候选版本 Nacos 服务名，不填时跟随稳定服务名，常配合元数据筛选区分版本',
    anonymousAllowed: '开启后允许请求不带身份信息直接访问，生产环境请谨慎使用',
    requestsPerSecond: '单路由每秒允许的请求量，用于保护上游，超过后按限流策略处理',
    burstCapacity: '突发容量允许短时间请求超过稳定 QPS，适合吸收瞬时尖峰',
    maxAttempts: '重试最多尝试次数，只建议对幂等接口开启，避免放大写请求',
    candidateHost: '候选版本上游地址，通常对应绿环境、灰度实例或新版本服务',
    candidatePort: '候选版本上游端口',
    candidateWeight: '候选版本承接流量比例，0 表示不按权重进入候选，100 表示全部切到候选',
    colorHeader: '染色 Header 用于指定流量进入候选版本，例如 x-gatepilot-color',
    candidateColor: '请求头命中该值时进入候选版本，例如 green',
    authType: '认证类型决定网关是否校验 API Key、JWT、OAuth2、Basic 或 mTLS',
    releaseStrategy: '发布策略决定稳定版本和候选版本怎么切流，蓝绿偏一次性切换，灰度偏按权重逐步放量'
  };
  return helps[key] || '该字段会生成 GatePilot 声明式资源，并在发布后由数据面消费';
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

function summarizeValue(value: unknown) {
  if (value === undefined || value === null || value === '') {
    return '-';
  }
  if (typeof value !== 'object') {
    return String(value);
  }
  if (Array.isArray(value)) {
    return value.length ? `${value.length} 项` : '-';
  }
  const entries = Object.entries(value as Record<string, unknown>)
    .filter(([, item]) => item !== undefined && item !== null && item !== '')
    .map(([key, item]) => `${key}: ${Array.isArray(item) ? item.join(', ') : String(item)}`);
  return entries.length ? entries.join('，') : '-';
}
</script>
