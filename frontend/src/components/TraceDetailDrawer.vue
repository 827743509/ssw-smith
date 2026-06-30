<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import type { Run, TraceDetail } from '../types/observability';
import { formatDate, statusColor } from '../utils/observability';
import RunNodeInspector from './RunNodeInspector.vue';
import RunTreePanel from './RunTreePanel.vue';

const props = defineProps<{
  open: boolean;
  loading: boolean;
  detail: TraceDetail | null;
}>();

const emit = defineEmits<{
  'update:open': [open: boolean];
}>();

const selectedRunId = ref<string>();

const selectedRun = computed(() => props.detail?.runs.find((run) => run.id === selectedRunId.value) || props.detail?.runs[0] || null);

watch(
  () => props.detail,
  (detail) => {
    selectedRunId.value = detail?.runs[0]?.id;
  },
  { immediate: true }
);

function selectRun(run: Run) {
  selectedRunId.value = run.id;
}
</script>

<template>
  <a-drawer
    :open="props.open"
    title="Trace Detail"
    width="92vw"
    :destroy-on-close="false"
    class="trace-detail-drawer"
    @update:open="emit('update:open', $event)"
  >
    <a-spin :spinning="loading">
      <template v-if="detail">
        <header class="trace-detail-header">
          <div>
            <a-space>
              <a-tag :color="statusColor(detail.trace.status)">{{ detail.trace.status }}</a-tag>
              <span class="trace-key">{{ detail.trace.traceKey }}</span>
            </a-space>
            <h1>{{ detail.trace.name }}</h1>
          </div>
          <a-space>
            <a-statistic title="Latency" :value="detail.trace.latencyMs || 0" suffix="ms" />
            <a-statistic title="Tokens" :value="detail.trace.totalTokens || 0" />
            <a-statistic title="Started" :value="formatDate(detail.trace.startedAt)" />
          </a-space>
        </header>

        <section class="trace-workbench">
          <RunTreePanel :trace="detail.trace" :runs="detail.runs" :selected-run-id="selectedRun?.id" @select-run="selectRun" />
          <RunNodeInspector :trace="detail.trace" :run="selectedRun" />
        </section>
      </template>
      <a-empty v-else description="No trace detail" />
    </a-spin>
  </a-drawer>
</template>