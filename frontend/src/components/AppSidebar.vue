<script setup lang="ts">
import { computed } from 'vue';
import { ApartmentOutlined, BarChartOutlined, BugOutlined, FolderOpenOutlined } from '@ant-design/icons-vue';

const props = defineProps<{
  selectedKeys: string[];
}>();

const emit = defineEmits<{
  'update:selectedKeys': [keys: string[]];
}>();

const selectedMenuKeys = computed({
  get: () => props.selectedKeys,
  set: (keys: string[]) => emit('update:selectedKeys', keys)
});
</script>

<template>
  <a-layout-sider class="app-sider" :width="304" theme="light">
    <div class="brand">
      <div>
        <strong>SSW Smith</strong>
        <span>LLM Observability</span>
      </div>
      <ApartmentOutlined />
    </div>

    <a-menu v-model:selectedKeys="selectedMenuKeys" mode="inline" class="side-menu">
      <a-menu-item key="tracing">
        <template #icon><BugOutlined /></template>
        开发日志跟踪
      </a-menu-item>
      <a-menu-item key="analytics">
        <template #icon><BarChartOutlined /></template>
        分析统计
      </a-menu-item>
      <a-menu-item key="projects">
        <template #icon><FolderOpenOutlined /></template>
        项目管理
      </a-menu-item>
    </a-menu>
  </a-layout-sider>
</template>