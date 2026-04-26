<template>
  <div class="page-stack">
    <section class="content-panel">
      <div class="panel-header">
        <div>
          <h2>使用手册</h2>
          <p>面向接入、发布、排障和值班的 GatePilot Console 培训手册。</p>
        </div>
      </div>

      <div class="doc-hero">
        <div>
          <h3>推荐操作顺序</h3>
          <p>先建平台基础资源，再接入项目，确认路由和上游，最后创建发布并观察节点应用结果。</p>
        </div>
        <StatusBadge label="中文手册" tone="info" />
      </div>
    </section>

    <section class="doc-layout">
      <aside class="content-panel doc-toc">
        <button
          v-for="section in sections"
          :key="section.key"
          class="compact-link-button"
          type="button"
          @click="activeSection = section.key"
        >
          {{ section.title }}
        </button>
      </aside>

      <section class="content-panel doc-content">
        <article v-for="section in visibleSections" :key="section.key" class="doc-section">
          <h2>{{ section.title }}</h2>
          <p class="resource-subtitle">{{ section.summary }}</p>
          <div class="doc-card-grid">
            <div v-for="card in section.cards" :key="card.title" class="doc-card">
              <h3>{{ card.title }}</h3>
              <p>{{ card.text }}</p>
            </div>
          </div>
        </article>
      </section>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import StatusBadge from '../components/StatusBadge.vue';

const activeSection = ref('all');

const sections = [
  {
    key: 'platform',
    title: '平台准备',
    summary: '命名空间、团队、环境、域名、分片、隔离组和流量等级都应先在平台配置里创建',
    cards: [
      { title: '命名空间', text: '用于隔离项目、路由、策略、发布和审计数据，生产环境建议按业务域拆分' },
      { title: '容量资源', text: '配置分片控制配置规模，隔离组对应 proxy 副本池，流量等级用于接入时推荐容量' },
      { title: '动态参数', text: '控制面、agent、proxy 的可调参数统一建模成资源，保存后由后端热生效' }
    ]
  },
  {
    key: 'onboarding',
    title: '项目接入',
    summary: '项目接入向导会生成项目、路由、上游、治理策略、发布策略和认证策略',
    cards: [
      { title: '基础信息', text: '选择命名空间、团队、环境和容量资源，避免在多个页面散落手填值' },
      { title: '路由上游', text: '填写入口域名、路径前缀和稳定上游；候选上游只有开启灰度或蓝绿时才需要' },
      { title: '保存发布', text: '先预览和校验，再保存资源或保存并发布，发布结果在发布中心和节点页继续跟踪' }
    ]
  },
  {
    key: 'release',
    title: '发布回滚',
    summary: '发布中心按选择项目、检查配置、确认发布、查看结果推进',
    cards: [
      { title: '发布', text: '选择待发布项目后先 dry-run，校验通过才允许确认发布' },
      { title: '回滚', text: '从已发布版本列表选择快照回滚，不允许临时手填目标版本' },
      { title: '节点结果', text: '发布后观察 agent / proxy 的 apply 状态、last-good 和失败原因' }
    ]
  },
  {
    key: 'diagnostics',
    title: '排障诊断',
    summary: '排障时先看总览，再查路由目录、节点实例、请求诊断和运行审计',
    cards: [
      { title: '请求诊断', text: '像调接口一样填写 Method、Host、Path、Header、Query、Cookie，查看命中链路' },
      { title: '节点实例', text: '查看副本、心跳、当前版本、last-good、上游健康和 apply 失败原因' },
      { title: '运行审计', text: '按命名空间、节点、路由、tid、结果和时间过滤，异常、Fallback、慢请求可下钻' }
    ]
  }
];

const visibleSections = computed(() =>
  activeSection.value === 'all'
    ? sections
    : sections.filter((section) => section.key === activeSection.value)
);
</script>
