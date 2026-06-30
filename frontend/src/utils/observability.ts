import type { DashboardStats, Run } from '../types/observability';

export function createEmptyStats(): DashboardStats {
  return {
    traceCount: 0,
    successCount: 0,
    errorCount: 0,
    totalTokens: 0,
    avgLatencyMs: 0,
    avgFeedbackScore: 0
  };
}

export function formatDate(value?: string) {
  if (!value) return '-';
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(value));
}

export function shortText(value?: string, maxLength = 120) {
  if (!value) return '-';
  return value.length > maxLength ? `${value.slice(0, maxLength)}...` : value;
}

export function statusColor(status?: string) {
  if (status === 'SUCCESS') return 'success';
  if (status === 'ERROR') return 'error';
  if (status === 'RUNNING') return 'processing';
  return 'default';
}

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

function firstTextFromSources(sources: any[], paths: string[]) {
  for (const source of sources) {
    if (!source) continue;
    for (const path of paths) {
      const value = getByPath(source, path);
      if (typeof value === 'string' && value.trim()) return value.trim();
    }
  }
  return '';
}

export function runModelLabel(run?: Run | null) {
  if (!run) return '';
  if (run.modelName?.trim()) return run.modelName.trim();

  const metadata = parseJson(run.metadata);
  const input = parseJson(run.input);
  const output = parseJson(run.output);
  const modelFromPayload = firstTextFromSources([metadata, input, output], [
    'ls_model_name',
    'model_name',
    'model',
    'modelName',
    'metadata.ls_model_name',
    'metadata.model_name',
    'metadata.model',
    'extra.metadata.ls_model_name',
    'extra.metadata.model_name',
    'extra.metadata.model',
    'invocation_params.model_name',
    'invocation_params.model',
    'extra.invocation_params.model_name',
    'extra.invocation_params.model',
    'kwargs.model_name',
    'kwargs.model',
    'serialized.kwargs.model_name',
    'serialized.kwargs.model'
  ]);
  if (modelFromPayload) return modelFromPayload;
  return firstTextFromSources([metadata, input, output], ['serialized.name']);
}