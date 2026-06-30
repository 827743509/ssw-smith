<script setup lang="ts">
import { computed } from 'vue';

const props = defineProps<{
  value?: string | null;
  emptyText?: string;
}>();

function formatValue(value?: string | null) {
  if (!value) return props.emptyText || '-';
  try {
    return JSON.stringify(JSON.parse(value), null, 2);
  } catch {
    return value;
  }
}

const displayValue = computed(() => formatValue(props.value));
</script>

<template>
  <pre class="json-viewer">{{ displayValue }}</pre>
</template>