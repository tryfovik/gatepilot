<template>
  <div v-if="open" class="dialog-backdrop" role="presentation" @click.self="emit('close')">
    <section class="dialog-panel" role="dialog" aria-modal="true" :aria-label="title">
      <div class="dialog-icon" :class="`dialog-icon--${tone}`">
        <AlertTriangle :size="18" />
      </div>
      <div class="dialog-content">
        <h2>{{ title }}</h2>
        <p>{{ message }}</p>
        <div class="dialog-actions">
          <button class="ghost-button" type="button" @click="emit('close')">{{ cancelText }}</button>
          <button class="primary-button" type="button" @click="emit('confirm')">{{ confirmText }}</button>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { AlertTriangle } from 'lucide-vue-next';

withDefaults(
  defineProps<{
    open: boolean;
    title: string;
    message: string;
    confirmText?: string;
    cancelText?: string;
    tone?: 'warning' | 'danger' | 'info';
  }>(),
  {
    confirmText: '确认',
    cancelText: '取消',
    tone: 'warning'
  }
);

const emit = defineEmits<{
  close: [];
  confirm: [];
}>();
</script>
