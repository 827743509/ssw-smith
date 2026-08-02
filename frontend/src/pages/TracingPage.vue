<script setup lang="ts">
import type { TableColumnsType } from 'ant-design-vue';
import { ref } from 'vue';
import type { Project, Trace } from '../types/observability';
import { formatDate, shortText, statusColor } from '../utils/observability';

const props = defineProps<{
  traces: Trace[];
  loading: boolean;
  selectedStatus: string;
  selectedProject?: Project;
}>();

const emit = defineEmits<{
  'update:selectedStatus': [status: string];
  openTrace: [trace: Trace];
}>();

const errorModalOpen = ref(false);
const selectedErrorMessage = ref('');

const traceColumns: TableColumnsType<Trace> = [
  { title: '状态', dataIndex: 'status', key: 'status', width: 110 },
  { title: '名称', dataIndex: 'name', key: 'name', ellipsis: true },
  { title: '输入', dataIndex: 'input', key: 'input', ellipsis: true },
  { title: '延迟', dataIndex: 'latencyMs', key: 'latencyMs', width: 110 },
  { title: 'Input Tokens', dataIndex: 'inputTokens', key: 'inputTokens', width: 120 },
  { title: 'Total Tokens', dataIndex: 'totalTokens', key: 'totalTokens', width: 120 },
  { title: 'Output Tokens', dataIndex: 'outputTokens', key: 'outputTokens', width: 125 },
  { title: 'Cache Read Tokens', dataIndex: 'cacheRead', key: 'cacheRead', width: 145 },
  { title: '错误信息', dataIndex: 'errorMessage', key: 'errorMessage', width: 240, ellipsis: true },
  { title: '开始时间', dataIndex: 'startedAt', key: 'startedAt', width: 160 }
];

function traceRowProps(record: Trace) {
  return { onClick: () => emit('openTrace', record) };
}

function showErrorMessage(errorMessage: string) {
  selectedErrorMessage.value = errorMessage;
  errorModalOpen.value = true;
}
</script>

<template>
  <section class="page-stack">
    <a-card class="toolbar-card" :bordered="false">
      <a-space wrap>
        <a-select :value="props.selectedStatus" class="status-select" @update:value="emit('update:selectedStatus', $event)">
          <a-select-option value="">全部状态</a-select-option>
          <a-select-option value="SUCCESS">SUCCESS</a-select-option>
          <a-select-option value="ERROR">ERROR</a-select-option>
          <a-select-option value="RUNNING">RUNNING</a-select-option>
        </a-select>
        <a-tag color="blue">{{ selectedProject?.name || '未选择项目' }}</a-tag>
        <a-tag>近实时 Trace</a-tag>
      </a-space>
    </a-card>

    <a-card title="Traces" :bordered="false">
      <a-table
        :columns="traceColumns"
        :data-source="traces"
        :loading="loading"
        :pagination="{ pageSize: 12 }"
        row-key="id"
        size="middle"
        :custom-row="traceRowProps"
      >
        <template #bodyCell="{ column, record, text }">
          <template v-if="column.key === 'status'">
            <a-tag :color="statusColor(record.status)">{{ record.status }}</a-tag>
          </template>
          <template v-else-if="column.key === 'input'">
            <span class="muted-line">{{ shortText(record.input) }}</span>
          </template>
          <template v-else-if="column.key === 'latencyMs'">
            {{ record.latencyMs || 0 }}ms
          </template>
          <template v-else-if="column.key === 'inputTokens'">
            {{ record.inputTokens || 0 }}
          </template>
          <template v-else-if="column.key === 'totalTokens'">
            {{ record.totalTokens || 0 }}
          </template>
          <template v-else-if="column.key === 'outputTokens'">
            {{ record.outputTokens || 0 }}
          </template>
          <template v-else-if="column.key === 'cacheRead'">
            {{ record.cacheRead || 0 }}
          </template>
          <template v-else-if="column.key === 'errorMessage'">
            <button
              v-if="record.errorMessage"
              class="error-message-link"
              type="button"
              @click.stop="showErrorMessage(record.errorMessage)"
            >
              {{ record.errorMessage }}
            </button>
            <span v-else>-</span>
          </template>
          <template v-else-if="column.key === 'startedAt'">
            {{ formatDate(record.startedAt) }}
          </template>
          <template v-else>{{ text || '-' }}</template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="errorModalOpen" title="错误详情" :footer="null" width="760px">
      <a-typography-paragraph class="error-detail" :copyable="{ text: selectedErrorMessage }">
        {{ selectedErrorMessage }}
      </a-typography-paragraph>
    </a-modal>
  </section>
</template>

<style scoped>
.error-message-link {
  display: block;
  width: 100%;
  padding: 0;
  overflow: hidden;
  color: #ff4d4f;
  text-align: left;
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: pointer;
  background: transparent;
  border: 0;
}

.error-message-link:hover {
  color: #cf1322;
  text-decoration: underline;
}

.error-detail {
  max-height: 60vh;
  padding: 16px;
  margin-bottom: 0;
  overflow: auto;
  color: #cf1322;
  white-space: pre-wrap;
  word-break: break-word;
  background: #fff2f0;
  border: 1px solid #ffccc7;
  border-radius: 6px;
}
</style>
