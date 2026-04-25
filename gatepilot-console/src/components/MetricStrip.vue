<template>
  <section class="metric-strip">
    <div v-for="item in items" :key="item.label" class="metric">
      <div class="metric-label">{{ item.label }}</div>
      <div class="metric-value">{{ item.value }}</div>
      <div class="metric-note">{{ item.note }}</div>
    </div>
  </section>
</template>

<script setup lang="ts">
defineProps<{
  items: Array<{
    label: string;
    value: string;
    note: string;
  }>;
}>();
</script>
