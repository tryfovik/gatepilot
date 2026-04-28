export type FieldType = 'text' | 'number' | 'boolean' | 'textarea' | 'secret';

export interface FieldSchema {
  key: string;
  label: string;
  help: string;
  type: FieldType;
  required?: boolean;
  relation?: {
    resourceType: string;
    multiple?: boolean;
    valueKey?: 'name' | 'host' | 'key';
    emptyLabel?: string;
  };
  options?: Array<{
    label: string;
    value: string;
  }>;
}

export interface PlatformResourcePage {
  label: string;
  resourceType: string;
  path: string;
  group: string;
  groupKey: string;
  description: string;
  nameLabel: string;
  nameHelp: string;
  namePlaceholder: string;
  fields: FieldSchema[];
}

export interface PlatformResourceGroup {
  label: string;
  key: string;
  path: string;
  description: string;
  resourceTypes: string[];
}

export const platformNamespace = 'system';

export const platformResourceGroups: PlatformResourceGroup[] = [
  {
    label: '组织与入口',
    key: 'organization',
    path: '/platform/organization/namespaces',
    description: '命名空间、团队、环境、域名',
    resourceTypes: ['namespaces', 'teams', 'environments', 'ingress-domains']
  },
  {
    label: '容量与隔离',
    key: 'capacity',
    path: '/platform/capacity/config-shards',
    description: '配置分片、隔离组、流量等级',
    resourceTypes: ['config-shards', 'isolation-groups', 'traffic-tiers']
  },
  {
    label: '运行参数',
    key: 'runtime',
    path: '/platform/runtime/control-plane-settings',
    description: '控制面、agent、proxy 热生效参数',
    resourceTypes: ['control-plane-settings', 'registry-centers']
  }
];

export const platformResourcePages: PlatformResourcePage[] = [
  {
    label: '命名空间',
    resourceType: 'namespaces',
    path: '/platform/organization/namespaces',
    group: '组织与入口',
    groupKey: 'organization',
    description: '用于隔离项目、路由、策略、发布和审计数据',
    nameLabel: '命名空间编码',
    nameHelp: '命名空间在系统内的唯一标识，建议使用小写字母、数字和中划线，例如 game-prod',
    namePlaceholder: '例如 game-prod',
    fields: [
      { key: 'displayName', label: '展示名称', help: '页面展示名，建议使用中文业务名', type: 'text' },
      { key: 'ownerTeam', label: '负责团队', help: '该命名空间的默认负责团队', type: 'text', relation: { resourceType: 'teams', emptyLabel: '不指定团队' } },
      { key: 'environment', label: '默认环境', help: '项目接入时的默认环境，例如 dev、test、prod', type: 'text', relation: { resourceType: 'environments', emptyLabel: '不指定环境' } },
      { key: 'defaultConfigShard', label: '默认配置分片', help: '新项目未指定时使用的配置分片', type: 'text', relation: { resourceType: 'config-shards', emptyLabel: '使用系统默认分片' } },
      { key: 'defaultIsolationGroup', label: '默认隔离组', help: '新项目未指定时使用的网关副本池', type: 'text', relation: { resourceType: 'isolation-groups', emptyLabel: '使用系统默认隔离组' } },
      { key: 'enabled', label: '允许接入', help: '关闭后不允许继续接入新项目', type: 'boolean' },
      { key: 'description', label: '说明', help: '写给平台用户看的简短说明', type: 'textarea' }
    ]
  },
  {
    label: '团队',
    resourceType: 'teams',
    path: '/platform/organization/teams',
    group: '组织与入口',
    groupKey: 'organization',
    description: '用于项目接入时选择负责团队，避免每个项目重复手填',
    nameLabel: '团队编码',
    nameHelp: '团队在平台内的唯一标识，建议和组织目录或内部团队编码保持一致',
    namePlaceholder: '例如 platform-team',
    fields: [
      { key: 'displayName', label: '展示名称', help: '团队在页面上的中文名称', type: 'text', required: true },
      { key: 'owner', label: '团队负责人', help: '团队负责人或值班负责人；当前还没有账号资源，后续接入组织账号后会改为选择', type: 'text' },
      { key: 'contact', label: '联系方式', help: '团队联系入口，例如群名或内部 IM 入口；这是团队自身属性，不作为资源引用', type: 'text' },
      { key: 'acceptingProjects', label: '允许新项目', help: '关闭后接入向导不应继续选择该团队', type: 'boolean' },
      { key: 'description', label: '说明', help: '写清楚团队职责边界', type: 'textarea' }
    ]
  },
  {
    label: '环境',
    resourceType: 'environments',
    path: '/platform/organization/environments',
    group: '组织与入口',
    groupKey: 'organization',
    description: '用于项目接入时选择 dev、test、stage、prod 等运行环境',
    nameLabel: '环境编码',
    nameHelp: '环境在平台内的唯一标识，例如 dev、test、stage、prod，用于接入向导选择',
    namePlaceholder: '例如 prod',
    fields: [
      { key: 'displayName', label: '展示名称', help: '环境的页面展示名，例如生产环境', type: 'text', required: true },
      {
        key: 'tier',
        label: '环境等级',
        help: '环境等级用于区分开发、测试、预发和生产，统一选择避免各处写法不一致',
        type: 'text',
        required: true,
        options: [
          { label: '开发环境 dev', value: 'dev' },
          { label: '测试环境 test', value: 'test' },
          { label: '预发环境 stage', value: 'stage' },
          { label: '生产环境 prod', value: 'prod' }
        ]
      },
      { key: 'acceptingProjects', label: '允许新项目', help: '关闭后接入向导不应继续选择该环境', type: 'boolean' },
      { key: 'description', label: '说明', help: '写清楚环境用途和发布约束', type: 'textarea' }
    ]
  },
  {
    label: '入口域名',
    resourceType: 'ingress-domains',
    path: '/platform/organization/ingress-domains',
    group: '组织与入口',
    groupKey: 'organization',
    description: '用于接入时选择已授权域名，避免用户随意手填 Host',
    nameLabel: '域名编码',
    nameHelp: '入口域名资源的唯一标识，建议与真实域名对应，便于项目接入时选择',
    namePlaceholder: '例如 api-example-com',
    fields: [
      { key: 'displayName', label: '展示名称', help: '域名的页面展示名', type: 'text' },
      { key: 'host', label: '域名', help: '真实入口域名，例如 api.example.com；这是入口域名资源自身属性，不引用其他资源', type: 'text', required: true },
      { key: 'ownerTeam', label: '归属团队', help: '负责该域名接入和证书维护的团队', type: 'text', relation: { resourceType: 'teams', emptyLabel: '不指定团队' } },
      { key: 'defaultNamespace', label: '默认命名空间', help: '该域名默认归属的命名空间', type: 'text', relation: { resourceType: 'namespaces', emptyLabel: '不指定命名空间' } },
      { key: 'acceptingProjects', label: '允许新项目', help: '关闭后接入向导不应继续选择该域名', type: 'boolean' },
      { key: 'description', label: '说明', help: '写清楚域名用途和使用边界', type: 'textarea' }
    ]
  },
  {
    label: '配置分片',
    resourceType: 'config-shards',
    path: '/platform/capacity/config-shards',
    group: '容量与隔离',
    groupKey: 'capacity',
    description: '用于大规模项目按分片生成、下发和观察 PublishedConfig',
    nameLabel: '分片编码',
    nameHelp: '配置分片的唯一标识，项目发布后会按该分片生成和下发 PublishedConfig',
    namePlaceholder: '例如 shard-high-01',
    fields: [
      { key: 'displayName', label: '展示名称', help: '配置分片的页面展示名', type: 'text' },
      { key: 'ownerTeam', label: '负责团队', help: '负责该分片容量和发布稳定性的团队', type: 'text', relation: { resourceType: 'teams', emptyLabel: '不指定团队' } },
      { key: 'maxProjectCount', label: '建议最大项目数', help: '容量规划参考，不直接限流业务请求', type: 'number' },
      { key: 'maxRouteCount', label: '建议最大路由数', help: '容量规划参考，帮助提前拆分配置分片', type: 'number' },
      { key: 'acceptingProjects', label: '允许新项目', help: '关闭后接入向导不应再默认选择该分片', type: 'boolean' },
      { key: 'description', label: '说明', help: '写清楚分片用途和容量边界', type: 'textarea' }
    ]
  },
  {
    label: '隔离组',
    resourceType: 'isolation-groups',
    path: '/platform/capacity/isolation-groups',
    group: '容量与隔离',
    groupKey: 'capacity',
    description: '用于把高流量项目调度到独立 agent / proxy 副本池',
    nameLabel: '隔离组编码',
    nameHelp: '隔离组对应一组 agent / proxy 副本池，高流量项目可以绑定到独立隔离组',
    namePlaceholder: '例如 proxy-pool-core',
    fields: [
      { key: 'displayName', label: '展示名称', help: '隔离组的页面展示名', type: 'text' },
      { key: 'ownerTeam', label: '负责团队', help: '负责该副本池容量和稳定性的团队', type: 'text', relation: { resourceType: 'teams', emptyLabel: '不指定团队' } },
      { key: 'maxRequestsPerSecond', label: '建议最大 RPS', help: '容量规划参考，便于识别是否需要扩副本', type: 'number' },
      { key: 'maxActiveConnections', label: '建议最大连接数', help: '容量规划参考，便于发现连接压力', type: 'number' },
      { key: 'dedicated', label: '独享副本池', help: '开启后表示该隔离组只承载指定项目或分片', type: 'boolean' },
      { key: 'description', label: '说明', help: '写清楚副本池用途、适用项目和扩容规则', type: 'textarea' }
    ]
  },
  {
    label: '流量等级',
    resourceType: 'traffic-tiers',
    path: '/platform/capacity/traffic-tiers',
    group: '容量与隔离',
    groupKey: 'capacity',
    description: '用于接入时选择容量等级，辅助推荐分片和隔离组',
    nameLabel: '等级编码',
    nameHelp: '流量等级的唯一标识，用于接入时推荐容量、分片和隔离组',
    namePlaceholder: '例如 critical',
    fields: [
      { key: 'displayName', label: '展示名称', help: '例如普通流量、高流量、核心链路', type: 'text' },
      { key: 'maxRequestsPerSecond', label: '建议最大 RPS', help: '容量规划参考，便于识别是否需要独立副本池', type: 'number' },
      { key: 'maxActiveConnections', label: '建议最大连接数', help: '容量规划参考，便于发现连接压力', type: 'number' },
      { key: 'recommendedConfigShard', label: '推荐配置分片', help: '选择该等级时推荐使用的配置分片', type: 'text', relation: { resourceType: 'config-shards', emptyLabel: '不推荐分片' } },
      { key: 'recommendedIsolationGroup', label: '推荐隔离组', help: '选择该等级时推荐使用的隔离组', type: 'text', relation: { resourceType: 'isolation-groups', emptyLabel: '不推荐隔离组' } },
      { key: 'dedicatedSuggested', label: '建议独享副本池', help: '高流量或核心链路建议使用独立隔离组', type: 'boolean' },
      { key: 'description', label: '说明', help: '写清楚该等级适用的项目类型', type: 'textarea' }
    ]
  },
  {
    label: '动态参数',
    resourceType: 'control-plane-settings',
    path: '/platform/runtime/control-plane-settings',
    group: '运行参数',
    groupKey: 'runtime',
    description: '用于保存可热生效的平台参数，支持推荐默认值和高级全量配置',
    nameLabel: '参数资源名',
    nameHelp: '动态参数资源的唯一标识，建议用模块和参数键组合，便于查询和回滚',
    namePlaceholder: '例如 agent-pull-interval',
    fields: [
      {
        key: 'module',
        label: '模块',
        help: '参数影响的后端模块',
        type: 'text',
        required: true,
        options: [
          { label: 'apiserver', value: 'apiserver' },
          { label: 'controller-manager', value: 'controller-manager' },
          { label: 'agent', value: 'agent' },
          { label: 'proxy', value: 'proxy' }
        ]
      },
      { key: 'key', label: '参数键', help: '动态配置键，例如 gatepilot.agent.pull-interval-ms', type: 'text', required: true },
      {
        key: 'valueType',
        label: '值类型',
        help: '后端按该类型解析参数值',
        type: 'text',
        required: true,
        options: [
          { label: '字符串', value: 'string' },
          { label: '数字', value: 'number' },
          { label: '布尔', value: 'boolean' },
          { label: '时长', value: 'duration' }
        ]
      },
      { key: 'value', label: '参数值', help: '当前希望生效的参数值', type: 'text', required: true },
      { key: 'defaultValue', label: '默认值', help: '推荐默认值，帮助用户不必理解所有高级字段', type: 'text' },
      {
        key: 'scope',
        label: '生效范围',
        help: '动态参数的生效范围，先统一选择范围类型，具体对象后续由参数键和值表达',
        type: 'text',
        required: true,
        options: [
          { label: '全局 global', value: 'global' },
          { label: '命名空间 namespace', value: 'namespace' },
          { label: '节点 node', value: 'node' },
          { label: '分片 shard', value: 'shard' }
        ]
      },
      {
        key: 'applyMode',
        label: '生效模式',
        help: 'immediate 立即生效，watch 监听生效，scheduled 定时生效',
        type: 'text',
        required: true,
        options: [
          { label: '立即生效', value: 'immediate' },
          { label: '监听生效', value: 'watch' },
          { label: '定时生效', value: 'scheduled' }
        ]
      },
      { key: 'hotReloadable', label: '热生效', help: 'GatePilot 后端应监听该资源并动态刷新运行参数', type: 'boolean' },
      { key: 'enabled', label: '启用', help: '关闭后该参数不参与动态生效', type: 'boolean' },
      { key: 'description', label: '参数说明', help: '说明参数含义、影响范围和风险', type: 'textarea' }
    ]
  },
  {
    label: '注册中心',
    resourceType: 'registry-centers',
    path: '/platform/runtime/registry-centers',
    group: '运行参数',
    groupKey: 'runtime',
    description: '统一保存 Nacos 连接信息，上游服务只引用注册中心和服务名',
    nameLabel: '注册中心编码',
    nameHelp: '注册中心资源的唯一标识，建议按环境命名，例如 nacos-prod',
    namePlaceholder: '例如 nacos-prod',
    fields: [
      { key: 'displayName', label: '展示名称', help: '页面展示名，例如生产 Nacos', type: 'text', required: true },
      {
        key: 'type',
        label: '注册中心类型',
        help: '当前主路径是 Nacos，后续如确有必要再扩展其他类型',
        type: 'text',
        required: true,
        options: [
          { label: 'Nacos', value: 'NACOS' }
        ]
      },
      { key: 'serverAddr', label: '服务地址', help: 'Nacos serverAddr，例如 nacos-headless:8848 或 10.0.0.1:8848', type: 'text', required: true },
      { key: 'namespace', label: '默认命名空间', help: 'Nacos 命名空间 ID，留空表示 public', type: 'text' },
      { key: 'group', label: '默认分组', help: 'Nacos 分组，默认 DEFAULT_GROUP', type: 'text' },
      {
        key: 'authType',
        label: '认证方式',
        help: '注册中心认证方式，密码会作为敏感配置保存，页面只用于录入和更新',
        type: 'text',
        required: true,
        options: [
          { label: '无认证', value: 'NONE' },
          { label: '用户名密码', value: 'USERNAME_PASSWORD' },
          { label: 'AK/SK', value: 'AKSK' }
        ]
      },
      { key: 'username', label: '用户名', help: '用户名密码认证时填写', type: 'text' },
      { key: 'password', label: '密码', help: '用户名密码认证时填写；生产环境后续应接入统一密钥加密能力', type: 'secret' },
      { key: 'accessKey', label: 'AccessKey', help: 'AK/SK 认证时填写', type: 'text' },
      { key: 'secretKey', label: 'SecretKey', help: 'AK/SK 认证时填写；生产环境后续应接入统一密钥加密能力', type: 'secret' },
      { key: 'acceptingUpstreams', label: '允许新上游', help: '关闭后新上游不应再选择该注册中心', type: 'boolean' },
      { key: 'description', label: '说明', help: '写清楚注册中心归属环境、网络边界和使用范围', type: 'textarea' }
    ]
  }
];

export function findPlatformResourcePage(resourceType?: string) {
  return platformResourcePages.find((page) => page.resourceType === resourceType) || platformResourcePages[0];
}

export function platformResourcePath(resourceType?: string) {
  return platformResourcePages.find((page) => page.resourceType === resourceType)?.path || '/platform/settings/overview';
}
