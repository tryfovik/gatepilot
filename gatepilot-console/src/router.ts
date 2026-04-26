import { createRouter, createWebHistory } from 'vue-router';
import ConfigSnapshotsView from './views/ConfigSnapshotsView.vue';
import DiagnosticsView from './views/DiagnosticsView.vue';
import NodesView from './views/NodesView.vue';
import OverviewView from './views/OverviewView.vue';
import PolicyCenterView from './views/PolicyCenterView.vue';
import ProjectTemplateView from './views/ProjectTemplateView.vue';
import ResourceListView from './views/ResourceListView.vue';
import ResourceCenterView from './views/ResourceCenterView.vue';
import HelpDocsView from './views/HelpDocsView.vue';
import ReleaseCenterView from './views/ReleaseCenterView.vue';
import RouteCatalogView from './views/RouteCatalogView.vue';
import RuntimeAuditView from './views/RuntimeAuditView.vue';
import SettingsView from './views/SettingsView.vue';
import PlatformResourceManageView from './views/PlatformResourceManageView.vue';
import { platformResourcePages, platformResourcePath } from './config/platformResources';

const resourcePageProps = (resourceType: string, title: string, description: string) => ({
  resourceType,
  title,
  description
});

export const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      redirect: '/workbench/runtime-overview'
    },
    {
      path: '/workbench/runtime-overview',
      component: OverviewView
    },
    {
      path: '/access/project-onboarding',
      component: ProjectTemplateView
    },
    {
      path: '/platform/resources/resource-center',
      component: ResourceCenterView
    },
    {
      path: '/access/gateway-projects/project-list',
      component: ResourceListView,
      props: resourcePageProps('projects', '网关项目管理', '管理 GatewayProject，按项目隔离路由、上游、策略和发布。')
    },
    {
      path: '/traffic/routing/route-catalog',
      component: RouteCatalogView
    },
    {
      path: '/traffic/governance/policy-center',
      component: PolicyCenterView
    },
    {
      path: '/release/config-release/release-center',
      component: ReleaseCenterView
    },
    {
      path: '/release/version-snapshots/snapshot-list',
      component: ConfigSnapshotsView
    },
    {
      path: '/runtime/config-state/published-configs',
      component: ResourceListView,
      props: resourcePageProps('published-configs', '已发布运行配置', '查看 controller-manager 生成并发布给数据面的 PublishedConfig。')
    },
    {
      path: '/runtime/data-plane/node-instances',
      component: NodesView
    },
    {
      path: '/traffic/upstream/upstream-services',
      component: ResourceListView,
      props: resourcePageProps('upstreams', '上游服务管理', '管理后端服务端点、负载均衡和健康状态。')
    },
    {
      path: '/operations/route-diagnostics/request-workbench',
      component: DiagnosticsView
    },
    {
      path: '/operations/runtime-audit/audit-records',
      component: RuntimeAuditView
    },
    {
      path: '/operations/control-plane-events/event-list',
      component: ResourceListView,
      props: resourcePageProps('events', '控制面事件', '查看控制面发布、回滚、同步和异常事件。')
    },
    {
      path: '/platform/settings/overview',
      component: SettingsView
    },
    {
      path: '/help/guide/user-manual',
      component: HelpDocsView
    },
    ...platformResourcePages.map((page) => ({
      path: page.path,
      component: PlatformResourceManageView,
      props: {
        resourceType: page.resourceType
      }
    })),
    { path: '/onboarding', redirect: '/access/project-onboarding' },
    { path: '/projects', redirect: '/access/gateway-projects/project-list' },
    { path: '/routes', redirect: '/traffic/routing/route-catalog' },
    { path: '/upstreams', redirect: '/traffic/upstream/upstream-services' },
    { path: '/policies', redirect: '/traffic/governance/policy-center' },
    { path: '/releases', redirect: '/release/config-release/release-center' },
    { path: '/snapshots', redirect: '/release/version-snapshots/snapshot-list' },
    { path: '/published-configs', redirect: '/runtime/config-state/published-configs' },
    { path: '/nodes', redirect: '/runtime/data-plane/node-instances' },
    { path: '/diagnostics', redirect: '/operations/route-diagnostics/request-workbench' },
    { path: '/audits', redirect: '/operations/runtime-audit/audit-records' },
    { path: '/events', redirect: '/operations/control-plane-events/event-list' },
    { path: '/resources', redirect: '/platform/resources/resource-center' },
    { path: '/settings', redirect: '/platform/settings/overview' },
    { path: '/help', redirect: '/help/guide/user-manual' },
    {
      path: '/settings/:resourceType',
      redirect: (to) => platformResourcePath(String(to.params.resourceType || ''))
    }
  ]
});
