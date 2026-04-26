import { createRouter, createWebHistory } from 'vue-router';
import ConfigSnapshotsView from './views/ConfigSnapshotsView.vue';
import DiagnosticsView from './views/DiagnosticsView.vue';
import NodesView from './views/NodesView.vue';
import OverviewView from './views/OverviewView.vue';
import PolicyCenterView from './views/PolicyCenterView.vue';
import ProjectTemplateView from './views/ProjectTemplateView.vue';
import ResourceListView from './views/ResourceListView.vue';
import ReleaseCenterView from './views/ReleaseCenterView.vue';
import RouteCatalogView from './views/RouteCatalogView.vue';
import RuntimeAuditView from './views/RuntimeAuditView.vue';

const resourcePageProps = (resourceType: string, title: string, description: string) => ({
  resourceType,
  title,
  description
});

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: OverviewView },
    {
      path: '/onboarding',
      component: ProjectTemplateView
    },
    {
      path: '/projects',
      component: ResourceListView,
      props: resourcePageProps('projects', '项目', '管理 GatewayProject，按项目隔离路由、上游、策略和发布。')
    },
    {
      path: '/routes',
      component: RouteCatalogView
    },
    {
      path: '/policies',
      component: PolicyCenterView
    },
    {
      path: '/releases',
      component: ReleaseCenterView
    },
    {
      path: '/snapshots',
      component: ConfigSnapshotsView
    },
    {
      path: '/nodes',
      component: NodesView
    },
    {
      path: '/upstreams',
      component: ResourceListView,
      props: resourcePageProps('upstreams', '上游', '管理后端服务端点、负载均衡和健康状态。')
    },
    {
      path: '/diagnostics',
      component: DiagnosticsView
    },
    {
      path: '/audits',
      component: RuntimeAuditView
    },
    {
      path: '/settings',
      component: ResourceListView,
      props: resourcePageProps('projects', '设置', '管理命名空间、隔离组、配置分片和控制面参数。')
    }
  ]
});
