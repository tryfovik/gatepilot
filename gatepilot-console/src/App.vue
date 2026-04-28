<template>
  <div
    class="shell"
    :class="{ 'shell--sidebar-collapsed': sidebarCollapsed }"
    @pointerdown.capture="markActionFeedback"
    @keydown.enter.capture="markActionFeedback"
    @keydown.space.capture="markActionFeedback"
  >
    <aside class="sidebar">
      <div class="brand">
        <div class="brand-mark">GP</div>
        <div class="brand-copy">
          <div class="brand-title">GatePilot</div>
          <div class="brand-subtitle">网关控制台</div>
        </div>
        <button class="icon-button sidebar-toggle" type="button" :aria-label="sidebarCollapsed ? '展开侧栏' : '收起侧栏'" @click="toggleSidebar">
          <component :is="sidebarCollapsed ? PanelLeftOpen : PanelLeftClose" :size="17" />
        </button>
      </div>
      <nav class="nav">
        <div v-for="section in navSections" :key="section.title" class="nav-section">
          <button class="nav-section-title" type="button" @click="toggleSection(section)">
            <span>{{ section.title }}</span>
            <ChevronDown :size="14" :class="{ 'nav-section-icon--closed': !sectionOpen(section) }" />
          </button>
          <RouterLink
            v-for="item in section.items"
            v-show="sectionOpen(section)"
            :key="item.path"
            :to="item.path"
            class="nav-item"
            :title="item.label"
          >
            <component :is="item.icon" :size="18" />
            <span>{{ item.label }}</span>
          </RouterLink>
        </div>
      </nav>
    </aside>

    <main class="main">
      <header class="topbar">
        <div>
          <div class="page-kicker">{{ activePage?.kicker }}</div>
          <h1>{{ activePage?.label }}</h1>
        </div>
        <div class="topbar-actions">
          <StatusBadge
            :label="controlPlaneStatus.label"
            :tone="controlPlaneStatus.tone"
            :title="controlPlaneStatus.title"
          />
          <span v-if="latestApiMeta.traceId" class="trace-chip" :title="`最近请求 tid：${latestApiMeta.traceId}`">
            tid {{ latestApiMeta.traceId }}
          </span>
          <select v-model="globalNamespace" class="select-input namespace-select" aria-label="全局命名空间">
            <option value="">全部命名空间</option>
            <option value="default">default</option>
            <option v-for="item in namespaceOptions" :key="item" :value="item">{{ item }}</option>
          </select>
          <button class="ghost-button" type="button" @click="refreshCurrentView">
            <RefreshCw :size="16" />
            刷新
          </button>
        </div>
      </header>
      <RouterView :key="viewKey" />
    </main>

    <div class="toast-stack" aria-live="polite" aria-atomic="true">
      <button
        v-for="toast in toastMessages"
        :key="toast.id"
        class="toast-message"
        :class="`toast-message--${toast.tone}`"
        type="button"
        @click="dismissToast(toast.id)"
      >
        <strong>{{ toast.title }}</strong>
        <span v-if="toast.message">{{ toast.message }}</span>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue';
import { RouterLink, RouterView, useRoute } from 'vue-router';
import {
  Activity,
  Boxes,
  ChevronDown,
  Database,
  GitBranch,
  History,
  BookOpen,
  LayoutDashboard,
  Network,
  PanelLeftClose,
  PanelLeftOpen,
  RefreshCw,
  Rocket,
  Route,
  ScrollText,
  Settings,
  ShieldCheck,
  Stethoscope,
  WandSparkles
} from 'lucide-vue-next';
import StatusBadge from './components/StatusBadge.vue';
import { controlPlaneHealth, latestApiMeta, listResources, probeControlPlane } from './api/client';
import { dismissToast, notifyInfo, toastMessages } from './utils/feedback';
import { getGlobalNamespace, setGlobalNamespace } from './utils/namespace';

const route = useRoute();
const refreshKey = ref(0);
const sidebarCollapsed = ref(false);
const compactSidebar = ref(false);
const collapsedSections = ref<Record<string, boolean>>({});
const globalNamespace = ref(getGlobalNamespace('default'));
const namespaceOptions = ref<string[]>([]);
let compactSidebarQuery: MediaQueryList | null = null;

interface NamespaceResource {
  metadata?: {
    name?: string;
  };
}

const navSections = [
  {
    title: '工作台',
    items: [
      { path: '/workbench/runtime-overview', label: '运行总览', kicker: '工作台 / 运行状态 / 总览', icon: LayoutDashboard }
    ]
  },
  {
    title: '接入管理',
    items: [
      { path: '/access/project-onboarding', label: '项目接入', kicker: '接入管理 / 项目接入 / 向导', icon: WandSparkles },
      { path: '/access/gateway-projects/project-list', label: '项目管理', kicker: '接入管理 / 网关项目 / 列表', icon: Boxes }
    ]
  },
  {
    title: '流量配置',
    items: [
      { path: '/traffic/routing/route-catalog', label: '路由目录', kicker: '流量配置 / 路由目录 / 查询', icon: Route },
      { path: '/traffic/upstream/upstream-services', label: '上游服务', kicker: '流量配置 / 上游服务 / 列表', icon: GitBranch },
      { path: '/traffic/governance/policy-center', label: '治理策略', kicker: '流量配置 / 治理策略 / 中心', icon: ShieldCheck }
    ]
  },
  {
    title: '发布管理',
    items: [
      { path: '/release/config-release/release-center', label: '配置发布', kicker: '发布管理 / 配置发布 / 中心', icon: Rocket },
      { path: '/release/version-snapshots/snapshot-list', label: '版本快照', kicker: '发布管理 / 版本快照 / 列表', icon: History }
    ]
  },
  {
    title: '运行观测',
    items: [
      { path: '/runtime/config-state/published-configs', label: '运行配置', kicker: '运行观测 / 配置状态 / 已发布', icon: Activity },
      { path: '/runtime/data-plane/node-instances', label: '节点实例', kicker: '运行观测 / 数据面节点 / 实例', icon: Network },
      { path: '/operations/route-diagnostics/request-workbench', label: '请求诊断', kicker: '运行观测 / 路由诊断 / 工作台', icon: Stethoscope },
      { path: '/operations/runtime-audit/audit-records', label: '运行审计', kicker: '运行观测 / 运行审计 / 查询', icon: ScrollText },
      { path: '/operations/control-plane-events/event-list', label: '事件查询', kicker: '运行观测 / 控制面事件 / 查询', icon: History }
    ]
  },
  {
    title: '平台配置',
    items: [
      { path: '/platform/resources/resource-center', label: '资源中心', kicker: '平台配置 / 资源中心 / 总览', icon: Database },
      { path: '/platform/organization/namespaces', label: '命名空间', kicker: '平台配置 / 组织与入口 / 命名空间', icon: Settings },
      { path: '/platform/organization/teams', label: '团队', kicker: '平台配置 / 组织与入口 / 团队', icon: Settings },
      { path: '/platform/organization/environments', label: '环境', kicker: '平台配置 / 组织与入口 / 环境', icon: Settings },
      { path: '/platform/organization/ingress-domains', label: '入口域名', kicker: '平台配置 / 组织与入口 / 域名', icon: Settings },
      { path: '/platform/capacity/config-shards', label: '配置分片', kicker: '平台配置 / 容量与隔离 / 配置分片', icon: Settings },
      { path: '/platform/capacity/isolation-groups', label: '隔离组', kicker: '平台配置 / 容量与隔离 / 隔离组', icon: Settings },
      { path: '/platform/capacity/traffic-tiers', label: '流量等级', kicker: '平台配置 / 容量与隔离 / 流量等级', icon: Settings },
      { path: '/platform/runtime/registry-centers', label: '注册中心', kicker: '平台配置 / 运行参数 / 注册中心', icon: Settings },
      { path: '/platform/runtime/control-plane-settings', label: '动态参数', kicker: '平台配置 / 运行参数 / 动态参数', icon: Settings },
      { path: '/platform/settings/overview', label: '配置总览', kicker: '平台配置 / 设置总览 / 入口', icon: Settings }
    ]
  },
  {
    title: '帮助中心',
    items: [
      { path: '/help/guide/user-manual', label: '使用手册', kicker: '帮助中心 / 使用手册 / 培训', icon: BookOpen }
    ]
  }
];

const navItems = computed(() => navSections.flatMap((section) => section.items));
const activePage = computed(() =>
  navItems.value.find((item) => route.path === item.path || route.path.startsWith(`${item.path}/`)) ?? navItems.value[0]
);
const viewKey = computed(() => `${route.fullPath}:${refreshKey.value}`);
const controlPlaneStatus = computed(() => {
  if (controlPlaneHealth.state === 'up') {
    return {
      label: '控制面正常',
      tone: 'success' as const,
      title: statusTitle()
    };
  }
  if (controlPlaneHealth.state === 'down') {
    return {
      label: '控制面不可用',
      tone: 'danger' as const,
      title: statusTitle()
    };
  }
  return {
    label: '控制面检测中',
    tone: 'info' as const,
    title: statusTitle()
  };
});

function refreshCurrentView() {
  refreshKey.value += 1;
  void probeControlPlane();
  void loadNamespaces();
  notifyInfo('已刷新当前页面');
}

function toggleSidebar() {
  sidebarCollapsed.value = !sidebarCollapsed.value;
  notifyInfo(sidebarCollapsed.value ? '侧栏已收起' : '侧栏已展开');
}

function sectionOpen(section: { title: string; items: Array<{ path: string }> }) {
  if (sidebarCollapsed.value || compactSidebar.value) {
    return true;
  }
  const manual = collapsedSections.value[section.title];
  if (manual !== undefined) {
    return !manual;
  }
  return section.items.some((item) => route.path === item.path || route.path.startsWith(`${item.path}/`));
}

function toggleSection(section: { title: string; items: Array<{ path: string }> }) {
  const currentlyOpen = sectionOpen(section);
  collapsedSections.value = {
    ...collapsedSections.value,
    [section.title]: currentlyOpen
  };
}

function markActionFeedback(event: Event) {
  const target = event.target;
  if (!(target instanceof Element)) {
    return;
  }
  const button = target.closest('button');
  if (!button || button.disabled) {
    return;
  }
  button.classList.remove('button-click-feedback');
  // 让连续点击也能重新触发动画
  window.requestAnimationFrame(() => button.classList.add('button-click-feedback'));
  window.setTimeout(() => button.classList.remove('button-click-feedback'), 420);
}

function statusTitle() {
  return `${controlPlaneHealth.message}${controlPlaneHealth.checkedAt ? ` / ${controlPlaneHealth.checkedAt}` : ''}`;
}

async function loadNamespaces() {
  try {
    const page = await listResources<NamespaceResource>('namespaces', 'system', 200);
    namespaceOptions.value = page.items
      .map((item) => item.metadata?.name)
      .filter((item): item is string => Boolean(item && item !== 'default'));
  } catch {
    namespaceOptions.value = [];
  }
}

onMounted(() => {
  void probeControlPlane();
  void loadNamespaces();
  compactSidebarQuery = window.matchMedia('(max-width: 560px)');
  compactSidebar.value = compactSidebarQuery.matches;
  compactSidebarQuery.addEventListener('change', syncCompactSidebar);
});

onUnmounted(() => {
  compactSidebarQuery?.removeEventListener('change', syncCompactSidebar);
});

function syncCompactSidebar(event: MediaQueryListEvent) {
  compactSidebar.value = event.matches;
}

watch(globalNamespace, (namespace) => {
  setGlobalNamespace(namespace);
  notifyInfo('全局命名空间已切换', namespace || '全部命名空间');
});
</script>
