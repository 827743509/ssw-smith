<script setup lang="ts">
import type { TableColumnsType } from 'ant-design-vue';
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

const traceColumns: TableColumnsType<Trace> = [
  { title: '状态', dataIndex: 'status', key: 'status', width: 110 },
  { title: '名称', dataIndex: 'name', key: 'name', ellipsis: true },
  { title: '输入', dataIndex: 'input', key: 'input', ellipsis: true },
  { title: '延迟', dataIndex: 'latencyMs', key: 'latencyMs', width: 110 },
  { title: 'Tokens', dataIndex: 'totalTokens', key: 'totalTokens', width: 110 },
  { title: '开始时间', dataIndex: 'startedAt', key: 'startedAt', width: 160 }
];

function traceRowProps(record: Trace) {
  return { onClick: () => emit('openTrace', record) };
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
          <template v-else-if="column.key === 'totalTokens'">
            {{ record.totalTokens || 0 }}
          </template>
          <template v-else-if="column.key === 'startedAt'">
            {{ formatDate(record.startedAt) }}
          </template>
          <template v-else>{{ text || '-' }}</template>
        </template>
      </a-table>
    </a-card>
  </section>
</template>