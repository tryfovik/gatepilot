<template>
  <section class="metric-strip">
    <component
      :is="item.onClick ? 'button' : 'div'"
      v-for="item in items"
      :key="item.label"
      class="metric"
      :class="{ 'metric--clickable': Boolean(item.onClick) }"
      :type="item.onClick ? 'button' : undefined"
      @click="trigger(item)"
    >
      <div class="metric-label">{{ item.label }}</div>
      <div class="metric-value">{{ item.value }}</div>
      <div class="metric-note">{{ item.note }}</div>
    </component>
  </section>
</template>

<script setup lang="ts">
interface MetricItem {
  label: string;
  value: string;
  note: string;
  onClick?: () => void;
}

defineProps<{
  items: MetricItem[];
}>();

function trigger(item: MetricItem) {
  item.onClick?.();
}
</script>
