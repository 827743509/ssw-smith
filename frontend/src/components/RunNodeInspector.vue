<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { CopyOutlined, ExperimentOutlined, LinkOutlined, ToolOutlined } from '@ant-design/icons-vue';
import type { Run, Trace } from '../types/observability';
import JsonViewer from './JsonViewer.vue';
import { formatDate, runModelLabel, statusColor } from '../utils/observability';

const props = defineProps<{
  trace: Trace;
  run?: Run | null;
}>();

function parseJson(value?: string | null): any {
  if (!value) return null;
  try {
    return JSON.parse(value);
  } catch {
    return null;
  }
}

function getByPath(source: any, path: string) {
  return path.split('.').reduce((current, key) => {
    if (current == null) return undefined;
    return current[key];
  }, source);
}

function collectArrays(source: any, paths: string[]) {
  const arrays: any[] = [];
  paths.forEach((path) => {
    const value = getByPath(source, path);
    if (Array.isArray(value)) arrays.push(...value);
  });
  return arrays;
}

function normalizeTool(tool: any) {
  const fn = tool?.function || tool?.tool || tool;
  const name = tool?.name || fn?.name || tool?.function?.name || tool?.id || 'unnamed_tool';
  const description = tool?.description || fn?.description || tool?.function?.description || '';
  const args = tool?.args || tool?.arguments || fn?.parameters || tool?.input || null;
  return { name, description, args, raw: tool };
}

function flattenObject(source: any, prefix = ''): Array<[string, string]> {
  if (!source || typeof source !== 'object' || Array.isArray(source)) return [];
  const rows: Array<[string, string]> = [];
  Object.entries(source).forEach(([key, value]) => {
    const nextKey = prefix ? `${prefix}.${key}` : key;
    if (value && typeof value === 'object' && !Array.isArray(value)) {
      rows.push(...flattenObject(value, nextKey));
    } else {
      rows.push([nextKey, typeof value === 'string' ? value : JSON.stringify(value)]);
    }
  });
  return rows;
}

const currentRun = computed(() => props.run || null);
const inputJson = computed(() => parseJson(currentRun.value?.input));
const outputJson = computed(() => parseJson(currentRun.value?.output));
const metadataJson = computed(() => parseJson(currentRun.value?.metadata));
const modelLabel = computed(() => runModelLabel(currentRun.value));

const requestedTools = computed(() => {
  const candidates = [metadataJson.value, inputJson.value].filter(Boolean);
  const tools: any[] = [];
  candidates.forEach((source) => {
    tools.push(...collectArrays(source, [
      'tools',
      'kwargs.tools',
      'invocation_params.tools',
      'extra.invocation_params.tools',
      'extra.metadata.tools',
      'serialized.kwargs.tools',
      'metadata.tools'
    ]));
  });
  return tools.map(normalizeTool);
});

const toolCalls = computed(() => {
  const calls: any[] = [];
  [inputJson.value, outputJson.value, metadataJson.value].filter(Boolean).forEach((source) => {
    calls.push(...collectArrays(source, [
      'tool_calls',
      'additional_kwargs.tool_calls',
      'message.additional_kwargs.tool_calls',
      'outputs.tool_calls',
      'generations.0.message.additional_kwargs.tool_calls'
    ]));
  });
  return calls.map(normalizeTool);
});

const attributes = computed(() => {
  if (!currentRun.value) return [];
  const run = currentRun.value;
  return [
    ['Run ID', run.runKey],
    ['Type', run.runType],
    ['Status', run.status],
    ['Model', modelLabel.value || '-'],
    ['Latency', `${run.latencyMs || 0}ms`],
    ['Prompt Tokens', run.promptTokens ?? 0],
    ['Completion Tokens', run.completionTokens ?? 0],
    ['Total Tokens', run.totalTokens ?? 0],
    ['Started At', formatDate(run.startedAt)],
    ['Ended At', formatDate(run.endedAt)]
  ];
});

const tagList = computed(() => {
  const tags = getByPath(metadataJson.value, 'tags') || getByPath(inputJson.value, 'tags');
  if (Array.isArray(tags)) return tags.map(String);
  const dottedOrder = getByPath(metadataJson.value, 'dotted_order') || getByPath(inputJson.value, 'dotted_order');
  return dottedOrder ? [String(dottedOrder)] : [];
});

const metadataRows = computed(() => {
  const fixedRows: Array<[string, string]> = [
    ['type', currentRun.value?.runType || '-'],
    ['model', modelLabel.value || '-'],
    ['model_name', modelLabel.value || '-'],
    ['run_id', currentRun.value?.runKey || '-'],
    ['status', currentRun.value?.status || '-']
  ];
  const seen = new Set<string>();
  return [...fixedRows, ...flattenObject(metadataJson.value)].filter(([key]) => {
    if (seen.has(key)) return false;
    seen.add(key);
    return true;
  });
});

const activeRequestToolKeys = ref<string[]>([]);
const activeToolCallKeys = ref<string[]>([]);

watch(
  () => currentRun.value?.id,
  () => {
    activeRequestToolKeys.value = requestedTools.value.length ? ['0'] : [];
    activeToolCallKeys.value = toolCalls.value.length ? ['0'] : [];
  },
  { immediate: true }
);

function copyText(value?: string) {
  if (!value) return;
  navigator.clipboard?.writeText(value);
}
</script>

<template>
  <main class="run-inspector">
    <template v-if="currentRun">
      <header class="inspector-header">
        <div class="inspector-title">
          <span class="inspector-logo"><ExperimentOutlined /></span>
          <div>
            <h2>{{ currentRun.name || 'Run' }}</h2>
            <div class="inspector-meta">
              <a-tag :color="statusColor(currentRun.status)">{{ currentRun.status }}</a-tag>
              <a-tag v-if="modelLabel">{{ modelLabel }}</a-tag>
              <span>{{ currentRun.latencyMs || 0 }}ms</span>
            </div>
          </div>
        </div>
        <a-button size="small" @click="copyText(currentRun.runKey)"><LinkOutlined /> ID</a-button>
      </header>

      <div class="inspector-body-grid">
        <section class="inspector-main-column">
          <a-tabs class="inspector-tabs" default-active-key="tools">
            <a-tab-pane key="tools" tab="Tools">
              <section class="detail-section">
                <div class="section-title"><ToolOutlined /> Request tools</div>
                <a-collapse v-if="requestedTools.length" v-model:active-key="activeRequestToolKeys" class="tool-collapse" ghost>
                  <a-collapse-panel v-for="(tool, index) in requestedTools" :key="String(index)">
                    <template #header>
                      <div class="tool-panel-title">
                        <ToolOutlined />
                        <strong>{{ tool.name }}</strong>
                        <a-tag color="green">Available</a-tag>
                      </div>
                    </template>
                    <p v-if="tool.description" class="tool-description">{{ tool.description }}</p>
                    <JsonViewer v-if="tool.args" :value="JSON.stringify(tool.args)" />
                    <JsonViewer v-else :value="JSON.stringify(tool.raw)" />
                  </a-collapse-panel>
                </a-collapse>
                <a-empty v-else description="No request tools parsed" />
              </section>

              <section class="detail-section compact-section">
                <div class="section-title">Tool calls</div>
                <a-collapse v-if="toolCalls.length" v-model:active-key="activeToolCallKeys" class="tool-collapse" ghost>
                  <a-collapse-panel v-for="(tool, index) in toolCalls" :key="String(index)">
                    <template #header>
                      <div class="tool-panel-title">
                        <ToolOutlined />
                        <strong>{{ tool.name }}</strong>
                        <a-tag color="lime">Called</a-tag>
                      </div>
                    </template>
                    <JsonViewer :value="JSON.stringify(tool.raw)" />
                  </a-collapse-panel>
                </a-collapse>
                <a-empty v-else description="No tool calls" />
              </section>
            </a-tab-pane>

            <a-tab-pane key="input" tab="Input">
              <div class="json-toolbar"><span>Node input</span><a-button size="small" @click="copyText(currentRun.input)"><CopyOutlined /></a-button></div>
              <JsonViewer :value="currentRun.input" />
            </a-tab-pane>

            <a-tab-pane key="output" tab="Output">
              <div class="json-toolbar"><span>Node output</span><a-button size="small" @click="copyText(currentRun.output)"><CopyOutlined /></a-button></div>
              <JsonViewer :value="currentRun.output" />
            </a-tab-pane>
          </a-tabs>
        </section>

        <aside class="attributes-side-column">
          <div class="attributes-panel-header">Attributes</div>
          <a-collapse :default-active-key="['summary', 'tags', 'metadata']" ghost>
            <a-collapse-panel key="summary" header="Summary">
              <div class="metadata-table">
                <div v-for="item in attributes" :key="item[0]" class="metadata-row">
                  <span class="metadata-key">{{ item[0] }}</span>
                  <span class="metadata-value">{{ item[1] }}</span>
                </div>
              </div>
            </a-collapse-panel>
            <a-collapse-panel key="tags" header="Tags">
              <div v-if="tagList.length" class="tag-list">
                <a-tag v-for="tag in tagList" :key="tag">{{ tag }}</a-tag>
              </div>
              <a-empty v-else description="No tags" />
            </a-collapse-panel>
            <a-collapse-panel key="metadata" header="Metadata">
              <div class="metadata-table">
                <div v-for="row in metadataRows" :key="row[0]" class="metadata-row">
                  <span class="metadata-key">{{ row[0] }}</span>
                  <span class="metadata-value">{{ row[1] }}</span>
                </div>
              </div>
            </a-collapse-panel>
          </a-collapse>
        </aside>
      </div>
    </template>
    <a-empty v-else description="Select a node" />
  </main>
</template>