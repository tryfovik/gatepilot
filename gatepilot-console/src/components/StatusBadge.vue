<template>
  <span class="status-badge" :class="`status-badge--${tone}`">{{ label }}</span>
</template>

<script setup lang="ts">
defineProps<{
  label: string;
  tone: 'success' | 'warning' | 'danger' | 'info' | 'neutral';
}>();
</script>
