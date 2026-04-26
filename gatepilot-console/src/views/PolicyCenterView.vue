<template>
  <div class="page-stack">
    <section class="content-panel">
      <div class="panel-header">
        <div>
          <h2>策略中心</h2>
          <p>集中查看流量治理、蓝绿灰度发布和认证策略。</p>
        </div>
      </div>
      <div class="tabbar">
        <button
          v-for="tab in tabs"
          :key="tab.resourceType"
          class="tab-button"
          :class="{ 'tab-button--active': activeTab.resourceType === tab.resourceType }"
          type="button"
          @click="switchTab(tab)"
        >
          {{ tab.label }}
        </button>
      </div>
    </section>

    <ResourceListView
      :key="activeTab.resourceType"
      :resource-type="activeTab.resourceType"
      :title="activeTab.title"
      :description="activeTab.description"
    />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import ResourceListView from './ResourceListView.vue';

const tabs = [
  {
    label: '流量治理',
    resourceType: 'traffic-policies',
    title: '流量治理策略',
    description: '查看限流、重试、熔断和染色规则。'
  },
  {
    label: '蓝绿 / 灰度',
    resourceType: 'release-policies',
    title: '发布策略',
    description: '查看蓝绿发布、灰度发布、权重分流和候选上游。'
  },
  {
    label: '认证',
    resourceType: 'auth-policies',
    title: '认证策略',
    description: '查看 API Key、JWT、OAuth2、Basic 和双向 TLS 认证配置。'
  }
];

const activeTab = ref(tabs[0]);

function switchTab(tab: typeof tabs[number]) {
  activeTab.value = tab;
}
</script>
