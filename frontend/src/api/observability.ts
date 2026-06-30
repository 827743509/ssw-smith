import type { ApiResponse, DashboardStats, PageResult, Project, Trace, TraceDetail } from '../types/observability';

const jsonHeaders = { 'Content-Type': 'application/json' };

async function request<T>(url: string, init?: RequestInit): Promise<T> {
  const response = await fetch(url, init);
  const body = (await response.json()) as ApiResponse<T>;
  if (!response.ok || body.code !== 0) {
    throw new Error(body.message || '请求失败');
  }
  return body.data;
}

export function listProjects() {
  return request<Project[]>('/api/projects');
}

export function createProject(payload: Pick<Project, 'name' | 'description' | 'environment'>) {
  return request<Project>('/api/projects', {
    method: 'POST',
    headers: jsonHeaders,
    body: JSON.stringify(payload)
  });
}

export function getDashboard(projectId: string) {
  return request<DashboardStats>(`/api/traces/dashboard?projectId=${projectId}`);
}

export function listTraces(params: { projectId: string; status?: string; pageNo?: number; pageSize?: number }) {
  const query = new URLSearchParams();
  query.set('projectId', params.projectId);
  if (params.status) query.set('status', params.status);
  query.set('pageNo', String(params.pageNo ?? 1));
  query.set('pageSize', String(params.pageSize ?? 20));
  return request<PageResult<Trace>>(`/api/traces?${query.toString()}`);
}

export function getTraceDetail(traceId: string) {
  return request<TraceDetail>(`/api/traces/${traceId}`);
}