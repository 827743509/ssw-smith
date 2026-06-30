<script setup lang="ts">
import { computed, reactive, watch } from 'vue';
import { message } from 'ant-design-vue';
import type { TableColumnsType } from 'ant-design-vue';
import type { Project } from '../types/observability';
import { formatDate } from '../utils/observability';

const props = defineProps<{
  projects: Project[];
  creating: boolean;
}>();

const emit = defineEmits<{
  'create-project': [payload: Pick<Project, 'name' | 'description' | 'environment'>];
}>();

const projectForm = reactive({ name: '', description: '', environment: 'dev' });
const canSubmit = computed(() => projectForm.name.trim().length > 0 && !props.creating);

const projectColumns: TableColumnsType<Project> = [
  { title: '项目名称', dataIndex: 'name', key: 'name' },
  { title: '环境', dataIndex: 'environment', key: 'environment', width: 120 },
  { title: '描述', dataIndex: 'description', key: 'description', ellipsis: true },
  { title: 'API Key', dataIndex: 'apiKey', key: 'apiKey', ellipsis: true },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 160 }
];

function submitProject() {
  const name = projectForm.name.trim();
  if (!name) {
    message.warning('请输入项目名称');
    return;
  }

  emit('create-project', {
    name,
    environment: projectForm.environment.trim() || 'dev',
    description: projectForm.description.trim()
  });
}

watch(
  () => props.creating,
  (creating, previousCreating) => {
    if (!creating && previousCreating) {
      projectForm.name = '';
      projectForm.description = '';
      projectForm.environment = 'dev';
    }
  }
);
</script>

<template>
  <section class="project-page">
    <a-card title="创建项目" :bordered="false" class="project-form-card">
      <a-form :model="projectForm" layout="vertical" @submit.prevent="submitProject">
        <a-form-item label="项目名称" required>
          <a-input v-model:value="projectForm.name" placeholder="例如 agentic-rag" allow-clear />
        </a-form-item>
        <a-form-item label="环境">
          <a-input v-model:value="projectForm.environment" placeholder="dev / staging / prod" allow-clear />
        </a-form-item>
        <a-form-item label="项目描述">
          <a-textarea v-model:value="projectForm.description" :rows="4" placeholder="描述这个应用或 Agent 的用途" />
        </a-form-item>
        <a-button type="primary" :loading="creating" :disabled="!canSubmit" block @click="submitProject">
          创建项目
        </a-button>
      </a-form>
    </a-card>

    <a-card title="项目列表" :bordered="false" class="project-table-card">
      <a-table :columns="projectColumns" :data-source="projects" row-key="id" :pagination="{ pageSize: 8 }">
        <template #bodyCell="{ column, record, text }">
          <template v-if="column.key === 'environment'">
            <a-tag color="blue">{{ record.environment || 'dev' }}</a-tag>
          </template>
          <template v-else-if="column.key === 'createdAt'">
            {{ formatDate(record.createdAt) }}
          </template>
          <template v-else>{{ text || '-' }}</template>
        </template>
      </a-table>
    </a-card>
  </section>
</template>