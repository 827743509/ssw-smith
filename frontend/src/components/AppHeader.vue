<script setup lang="ts">
import { computed, h } from 'vue';
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons-vue';
import type { Project } from '../types/observability';

const props = defineProps<{
  currentMenu: string;
  selectedProjectId: string;
  projectOptions: Array<{ label: string; value: string }>;
  projects: Project[];
}>();

const emit = defineEmits<{
  'update:selectedProjectId': [projectId: string];
  refresh: [];
  openProjects: [];
}>();

const selectedProjectModel = computed({
  get: () => props.selectedProjectId,
  set: (value: string) => emit('update:selectedProjectId', value)
});

const title = computed(() => {
  if (props.currentMenu === 'tracing') return '开发日志跟踪';
  if (props.currentMenu === 'analytics') return '分析统计';
  return '项目管理';
});

const breadcrumb = computed(() => {
  if (props.currentMenu === 'tracing') return 'Tracing';
  if (props.currentMenu === 'analytics') return 'Monitoring';
  return 'Projects';
});
</script>

<template>
  <a-layout-header class="app-header">
    <div>
      <a-breadcrumb>
        <a-breadcrumb-item>Personal</a-breadcrumb-item>
        <a-breadcrumb-item>{{ breadcrumb }}</a-breadcrumb-item>
      </a-breadcrumb>
      <h1>{{ title }}</h1>
    </div>
    <a-space>
      <a-select
        v-model:value="selectedProjectModel"
        :options="projectOptions"
        class="project-select"
        placeholder="请选择项目"
        :disabled="projects.length === 0"
      />
      <a-button :icon="h(ReloadOutlined)" @click="emit('refresh')">刷新</a-button>
      <a-button type="primary" :icon="h(PlusOutlined)" @click="emit('openProjects')">新建项目</a-button>
    </a-space>
  </a-layout-header>
</template>