<template>
  <div v-if="open" class="drawer-backdrop" role="presentation" @click.self="emit('close')">
    <aside class="detail-drawer" aria-label="资源详情">
      <header class="drawer-header">
        <div>
          <h2>{{ title }}</h2>
          <p>{{ subtitle }}</p>
        </div>
        <button class="icon-button" type="button" aria-label="关闭详情" @click="emit('close')">
          <X :size="18" />
        </button>
      </header>
      <pre class="json-viewer">{{ formattedPayload }}</pre>
    </aside>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { X } from 'lucide-vue-next';

const props = defineProps<{
  open: boolean;
  title: string;
  subtitle: string;
  payload?: unknown;
}>();

const emit = defineEmits<{
  close: [];
}>();

const formattedPayload = computed(() => JSON.stringify(props.payload ?? {}, null, 2));
</script>
