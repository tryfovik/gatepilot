<template>
  <div class="shell">
    <aside class="sidebar">
      <div class="brand">
        <div class="brand-mark">GP</div>
        <div>
          <div class="brand-title">GatePilot</div>
          <div class="brand-subtitle">网关控制台</div>
        </div>
      </div>
      <nav class="nav">
        <RouterLink v-for="item in navItems" :key="item.path" :to="item.path" class="nav-item">
          <component :is="item.icon" :size="18" />
          <span>{{ item.label }}</span>
        </RouterLink>
      </nav>
    </aside>

    <main class="main">
      <header class="topbar">
        <div>
          <div class="page-kicker">{{ activePage?.kicker }}</div>
          <h1>{{ activePage?.label }}</h1>
        </div>
        <div class="topbar-actions">
          <StatusBadge label="控制面正常" tone="success" />
          <button class="ghost-button" type="button">
            <RefreshCw :size="16" />
            刷新
          </button>
        </div>
      </header>
      <RouterView />
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { RouterLink, RouterView, useRoute } from 'vue-router';
import {
  Activity,
  Boxes,
  GitBranch,
  History,
  LayoutDashboard,
  Network,
  RefreshCw,
  Rocket,
  Route,
  ScrollText,
  Settings,
  ShieldCheck,
  Stethoscope
} from 'lucide-vue-next';
import StatusBadge from './components/StatusBadge.vue';

const route = useRoute();

const navItems = [
  { path: '/', label: '总览', kicker: '运行状态', icon: LayoutDashboard },
  { path: '/projects', label: '项目', kicker: '资源管理', icon: Boxes },
  { path: '/routes', label: '路由', kicker: '流量入口', icon: Route },
  { path: '/policies', label: '策略', kicker: '治理规则', icon: ShieldCheck },
  { path: '/releases', label: '发布', kicker: '配置推进', icon: Rocket },
  { path: '/snapshots', label: '快照', kicker: '版本对比', icon: History },
  { path: '/nodes', label: '节点', kicker: '数据面副本', icon: Network },
  { path: '/upstreams', label: '上游', kicker: '后端服务', icon: GitBranch },
  { path: '/diagnostics', label: '诊断', kicker: '排障入口', icon: Stethoscope },
  { path: '/audits', label: '审计', kicker: '操作与访问', icon: ScrollText },
  { path: '/settings', label: '设置', kicker: '平台配置', icon: Activity }
];

const activePage = computed(() => navItems.find((item) => item.path === route.path) ?? navItems[0]);
</script>
