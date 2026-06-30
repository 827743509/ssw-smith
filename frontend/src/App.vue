<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { message } from 'ant-design-vue';
import { createProject, getDashboard, getTraceDetail, listProjects, listTraces } from './api/observability';
import AppHeader from './components/AppHeader.vue';
import AppSidebar from './components/AppSidebar.vue';
import TraceDetailDrawer from './components/TraceDetailDrawer.vue';
import AnalyticsPage from './pages/AnalyticsPage.vue';
import ProjectsPage from './pages/ProjectsPage.vue';
import TracingPage from './pages/TracingPage.vue';
import type { DashboardStats, Project, Trace, TraceDetail } from './types/observability';
import { createEmptyStats } from './utils/observability';

const selectedMenuKeys = ref<string[]>(['tracing']);
const projects = ref<Project[]>([]);
const selectedProjectId = ref('');
const selectedStatus = ref('');
const traces = ref<Trace[]>([]);
const stats = ref<DashboardStats>(createEmptyStats());
const detail = ref<TraceDetail | null>(null);
const detailOpen = ref(false);
const loading = ref(false);
const detailLoading = ref(false);
const creatingProject = ref(false);

const currentMenu = computed(() => selectedMenuKeys.value[0] || 'tracing');
const selectedProject = computed(() => projects.value.find((project) => project.id === selectedProjectId.value));
const projectOptions = computed(() =>
  projects.value.map((project) => ({ label: `${project.name} / ${project.environment || 'dev'}`, value: project.id }))
);

function openProjectMenu() {
  selectedMenuKeys.value = ['projects'];
}

function resetObservabilityData() {
  stats.value = createEmptyStats();
  traces.value = [];
  detail.value = null;
  detailOpen.value = false;
}

async function refreshAll() {
  if (!selectedProjectId.value) {
    resetObservabilityData();
    return;
  }

  loading.value = true;
  try {
    const [dashboard, page] = await Promise.all([
      getDashboard(selectedProjectId.value),
      listTraces({ projectId: selectedProjectId.value, status: selectedStatus.value || undefined })
    ]);
    stats.value = dashboard;
    traces.value = page.records;
  } catch (error) {
    message.error(error instanceof Error ? error.message : '数据加载失败');
  } finally {
    loading.value = false;
  }
}

async function loadProjects() {
  try {
    projects.value = await listProjects();
    if (!selectedProjectId.value && projects.value.length > 0) {
      selectedProjectId.value = projects.value[0].id;
    }
  } catch (error) {
    message.error(error instanceof Error ? error.message : '项目加载失败');
  }
}

async function openTrace(record: Trace) {
  detailLoading.value = true;
  detailOpen.value = true;
  try {
    detail.value = await getTraceDetail(record.id);
  } catch (error) {
    message.error(error instanceof Error ? error.message : 'Trace 详情加载失败');
  } finally {
    detailLoading.value = false;
  }
}

async function handleCreateProject(payload: Pick<Project, 'name' | 'description' | 'environment'>) {
  creatingProject.value = true;
  try {
    const project = await createProject(payload);
    projects.value = [project, ...projects.value];
    selectedProjectId.value = project.id;
    message.success('项目创建成功');
  } catch (error) {
    message.error(error instanceof Error ? error.message : '项目创建失败');
  } finally {
    creatingProject.value = false;
  }
}

watch([selectedProjectId, selectedStatus], refreshAll);

onMounted(loadProjects);
</script>

<template>
  <a-layout class="app-layout">
    <AppSidebar v-model:selected-keys="selectedMenuKeys" />

    <a-layout>
      <AppHeader
        v-model:selected-project-id="selectedProjectId"
        :current-menu="currentMenu"
        :project-options="projectOptions"
        :projects="projects"
        @refresh="refreshAll"
        @open-projects="openProjectMenu"
      />

      <a-layout-content class="app-content">
        <TracingPage
          v-if="currentMenu === 'tracing'"
          v-model:selected-status="selectedStatus"
          :traces="traces"
          :loading="loading"
          :selected-project="selectedProject"
          @open-trace="openTrace"
        />
        <AnalyticsPage v-else-if="currentMenu === 'analytics'" :stats="stats" />
        <ProjectsPage v-else :projects="projects" :creating="creatingProject" @create-project="handleCreateProject" />
      </a-layout-content>
    </a-layout>

    <TraceDetailDrawer v-model:open="detailOpen" :loading="detailLoading" :detail="detail" />
  </a-layout>
</template>