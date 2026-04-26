import { createRouter, createWebHistory } from 'vue-router';
import ConfigSnapshotsView from './views/ConfigSnapshotsView.vue';
import DiagnosticsView from './views/DiagnosticsView.vue';
import NodesView from './views/NodesView.vue';
import OverviewView from './views/OverviewView.vue';
import ProjectTemplateView from './views/ProjectTemplateView.vue';
import ResourceListView from './views/ResourceListView.vue';
import RouteCatalogView from './views/RouteCatalogView.vue';

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
      component: ResourceListView,
      props: resourcePageProps('traffic-policies', '策略', '集中查看流量治理、认证和发布策略。')
    },
    {
      path: '/releases',
      component: ResourceListView,
      props: resourcePageProps('published-configs', '发布', '查看 PublishedConfig、节点应用进度和失败原因。')
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
      component: ResourceListView,
      props: resourcePageProps('events', '审计', '查询操作审计、运行事件和关联 TraceId。')
    },
    {
      path: '/settings',
      component: ResourceListView,
      props: resourcePageProps('projects', '设置', '管理命名空间、隔离组、配置分片和控制面参数。')
    }
  ]
});
