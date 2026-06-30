export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}

export interface PageResult<T> {
  records: T[];
  total: number;
  size: number;
  current: number;
  pages: number;
}

export interface Project {
  id: string;
  name: string;
  description?: string;
  environment?: string;
  apiKey?: string;
  createdAt?: string;
}

export interface Trace {
  id: string;
  projectId: string;
  traceKey: string;
  name: string;
  status: 'SUCCESS' | 'ERROR' | 'RUNNING' | string;
  latencyMs?: number;
  totalTokens?: number;
  promptTokens?: number;
  completionTokens?: number;
  input?: string;
  output?: string;
  metadata?: string;
  errorMessage?: string;
  startedAt?: string;
  endedAt?: string;
}

export interface Run {
  id: string;
  traceId: string;
  parentRunId?: string;
  runKey: string;
  name: string;
  runType: string;
  status: string;
  modelName?: string;
  latencyMs?: number;
  promptTokens?: number;
  completionTokens?: number;
  totalTokens?: number;
  input?: string;
  output?: string;
  metadata?: string;
  errorMessage?: string;
  startedAt?: string;
  endedAt?: string;
}

export interface Feedback {
  id: string;
  traceId: string;
  runId?: string;
  feedbackKey: string;
  score: number;
  comment?: string;
  source?: string;
  createdAt?: string;
}

export interface DashboardStats {
  traceCount: number;
  successCount: number;
  errorCount: number;
  totalTokens: number;
  avgLatencyMs: number;
  avgFeedbackScore: number;
}

export interface TraceDetail {
  trace: Trace;
  runs: Run[];
  feedbacks: Feedback[];
}
