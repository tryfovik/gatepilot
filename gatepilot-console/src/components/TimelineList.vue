<template>
  <ol class="timeline-list">
    <li v-for="item in items" :key="item.key" class="timeline-entry">
      <span class="timeline-dot" :class="`timeline-dot--${item.tone || 'info'}`"></span>
      <div>
        <div class="timeline-title">{{ item.title }}</div>
        <div v-if="item.meta" class="timeline-meta">{{ item.meta }}</div>
        <div v-if="item.message" class="timeline-message">{{ item.message }}</div>
      </div>
    </li>
  </ol>
</template>

<script setup lang="ts">
defineProps<{
  items: Array<{
    key: string;
    title: string;
    meta?: string;
    message?: string;
    tone?: 'success' | 'warning' | 'danger' | 'info' | 'neutral';
  }>;
}>();
</script>
