<template>
  <div class="page-stack">
    <section class="content-panel">
      <div class="panel-header">
        <div>
          <h2>平台配置总览</h2>
          <p>这里只做配置入口导航，具体资源在独立页面维护，避免把所有表单塞进一个设置页。</p>
        </div>
      </div>

      <div class="settings-overview-grid">
        <div v-for="group in platformResourceGroups" :key="group.key" class="settings-overview-group">
          <div class="settings-overview-group-header">
            <h3>{{ group.label }}</h3>
            <p>{{ group.description }}</p>
          </div>
          <div class="settings-overview-links">
            <button
              v-for="page in pagesOf(group.resourceTypes)"
              :key="page.resourceType"
              class="settings-overview-link"
              type="button"
              @click="openPage(page.path, page.label)"
            >
              <strong>{{ page.label }}</strong>
              <span>{{ page.description }}</span>
            </button>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';
import {
  platformResourceGroups,
  platformResourcePages,
  type PlatformResourcePage
} from '../config/platformResources';

const router = useRouter();

function pagesOf(resourceTypes: string[]) {
  return resourceTypes
    .map((resourceType) => platformResourcePages.find((page) => page.resourceType === resourceType))
    .filter((page): page is PlatformResourcePage => Boolean(page));
}

function openPage(path: string, label: string) {
  void label;
  void router.push(path);
}
</script>
