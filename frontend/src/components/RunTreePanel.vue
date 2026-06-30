<script setup lang="ts">
import { computed } from 'vue';
import { BranchesOutlined, CheckCircleFilled, ClockCircleOutlined, RobotOutlined, ToolOutlined } from '@ant-design/icons-vue';
import type { Run, Trace } from '../types/observability';
import { runModelLabel } from '../utils/observability';

const props = defineProps<{
  trace: Trace;
  runs: Run[];
  selectedRunId?: string;
}>();

const emit = defineEmits<{
  selectRun: [run: Run];
}>();

interface RunTreeNode {
  run: Run;
  children: RunTreeNode[];
  depth: number;
}

const waterfallRows = computed(() => {
  const nodeMap = new Map<string, RunTreeNode>();
  props.runs.forEach((run) => nodeMap.set(run.id, { run, children: [], depth: 0 }));

  const roots: RunTreeNode[] = [];
  nodeMap.forEach((node) => {
    const parent = node.run.parentRunId ? nodeMap.get(String(node.run.parentRunId)) : undefined;
    if (parent) parent.children.push(node);
    else roots.push(node);
  });

  const rows: RunTreeNode[] = [];
  function visit(node: RunTreeNode, depth: number) {
    node.depth = depth;
    rows.push(node);
    node.children
      .sort((a, b) => new Date(a.run.startedAt || '').getTime() - new Date(b.run.startedAt || '').getTime())
      .forEach((child) => visit(child, depth + 1));
  }

  roots
    .sort((a, b) => new Date(a.run.startedAt || '').getTime() - new Date(b.run.startedAt || '').getTime())
    .forEach((node) => visit(node, 0));
  return rows;
});

const totalLatency = computed(() => props.trace.latencyMs || props.runs.reduce((sum, run) => sum + (run.latencyMs || 0), 0));
const summaryDuration = computed(() => formatDuration(totalLatency.value));

function iconType(run: Run) {
  const type = `${run.runType || ''} ${run.name || ''}`.toLowerCase();
  if (type.includes('llm') || type.includes('model') || type.includes('openai') || type.includes('chat')) return 'llm';
  if (type.includes('tool')) return 'tool';
  return 'chain';
}

function formatDuration(ms = 0) {
  if (ms >= 1000) return `${(ms / 1000).toFixed(2)}s`;
  return `${ms}ms`;
}

function modelLabel(run: Run) {
  return runModelLabel(run);
}
</script>

<template>
  <aside class="run-tree-panel">
    <div class="run-tree-header">
      <div>
        <strong>Runs</strong>
        <span>{{ waterfallRows.length }} nodes</span>
      </div>
      <a-tag color="blue">Waterfall</a-tag>
    </div>

    <div class="trace-summary-row">
      <CheckCircleFilled class="summary-icon" />
      <span>Summary</span>
      <span class="summary-stat">{{ summaryDuration }}</span>
    </div>

    <div class="run-node-list">
      <button
        v-for="node in waterfallRows"
        :key="node.run.id"
        class="run-node-row"
        :class="{ active: selectedRunId === node.run.id }"
        :style="{ paddingLeft: `${14 + node.depth * 24}px` }"
        type="button"
        @click="emit('selectRun', node.run)"
      >
        <span v-if="node.depth > 0" class="run-branch" />
        <span class="run-icon" :class="iconType(node.run)">
          <RobotOutlined v-if="iconType(node.run) === 'llm'" />
          <ToolOutlined v-else-if="iconType(node.run) === 'tool'" />
          <BranchesOutlined v-else />
        </span>
        <span class="run-main">
          <span class="run-name">{{ node.run.name || node.run.runType }}</span>
          <a-tag v-if="modelLabel(node.run)" class="run-model">{{ modelLabel(node.run) }}</a-tag>
        </span>
        <span class="run-duration"><ClockCircleOutlined /> {{ formatDuration(node.run.latencyMs || 0) }}</span>
      </button>
    </div>
  </aside>
</template>