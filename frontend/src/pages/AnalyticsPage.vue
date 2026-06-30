<script setup lang="ts">
import { computed } from 'vue';
import type { DashboardStats } from '../types/observability';

const props = defineProps<{
  stats: DashboardStats;
}>();

function numberValue(value: unknown) {
  const result = Number(value ?? 0);
  return Number.isFinite(result) ? result : 0;
}

function percent(value: unknown, total: unknown) {
  const current = numberValue(value);
  const denominator = numberValue(total);
  if (denominator <= 0) {
    return '0%';
  }
  return `${Math.round((current / denominator) * 100)}%`;
}

const traceCount = computed(() => numberValue(props.stats.traceCount));
const successRate = computed(() => percent(props.stats.successCount, props.stats.traceCount));
const errorRate = computed(() => percent(props.stats.errorCount, props.stats.traceCount));
const avgLatencyMs = computed(() => numberValue(props.stats.avgLatencyMs));
const totalTokens = computed(() => numberValue(props.stats.totalTokens));
</script>

<template>
  <section class="page-stack">
    <div class="metric-grid">
      <a-card><a-statistic title="Trace 总数" :value="traceCount" /></a-card>
      <a-card><a-statistic title="成功率" :value="successRate" /></a-card>
      <a-card><a-statistic title="错误率" :value="errorRate" /></a-card>
      <a-card><a-statistic title="平均延迟 ms" :value="avgLatencyMs" /></a-card>
      <a-card><a-statistic title="Token 总量" :value="totalTokens" /></a-card>
    </div>

    <a-card title="Trace Count" :bordered="false">
      <template #extra><a-tag color="green">Success</a-tag><a-tag color="red">Error</a-tag></template>
      <div class="chart-panel">
        <div class="chart-grid">
          <span v-for="item in 7" :key="item"></span>
        </div>
        <div class="chart-line success-line"></div>
        <div class="chart-line error-line"></div>
        <div class="chart-days">
          <span>Mon</span><span>Tue</span><span>Wed</span><span>Thu</span><span>Fri</span><span>Sat</span><span>Sun</span>
        </div>
      </div>
    </a-card>

    <div class="analysis-grid">
      <a-card title="Trace Latency" :bordered="false">
        <a-progress :percent="Math.min(avgLatencyMs / 20, 100)" status="active" />
        <p class="card-note">平均响应耗时，用于快速判断链路性能波动。</p>
      </a-card>
    </div>
  </section>
</template>