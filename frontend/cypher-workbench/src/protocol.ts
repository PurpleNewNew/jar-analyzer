export const CHANNEL_QUERY_EXECUTE = 'ja.query.execute'
export const CHANNEL_SCRIPT_LIST = 'ja.script.list'
export const CHANNEL_SCRIPT_SAVE = 'ja.script.save'
export const CHANNEL_SCRIPT_DELETE = 'ja.script.delete'
export const CHANNEL_UI_CONTEXT = 'ja.ui.context'
export const CHANNEL_UI_FULLSCREEN = 'ja.ui.fullscreen'

export interface UiContext {
  language: 'zh' | 'en'
  theme: 'default' | 'dark'
}

export interface QueryFrameRequestResult {
  ok: boolean
  code: string
  message: string
  frame?: QueryFramePayload
}

export interface QueryFramePayload {
  frameId: string
  query: string
  columns: string[]
  rows: unknown[][]
  warnings: string[]
  truncated: boolean
  elapsedMs: number
  graph?: GraphFramePayload
  text: string
}

export interface GraphFramePayload {
  nodes: GraphNodePayload[]
  edges: GraphEdgePayload[]
  warnings: string[]
  truncated: boolean
}

export interface GraphNodePayload {
  id: number
  label: string
  kind: string
  jarId: number
  className: string
  methodName: string
  methodDesc: string
  labels: string[]
  properties: Record<string, unknown>
}

export interface GraphEdgePayload {
  id: number
  source: number
  target: number
  relType: string
  confidence: string
  evidence: string
  properties: Record<string, unknown>
}

export interface ScriptListResponse {
  items: ScriptItem[]
}

export interface ScriptItem {
  scriptId: number
  title: string
  body: string
  tags: string
  pinned: boolean
  createdAt: number
  updatedAt: number
}

export interface QueryUiOptions {
  maxRows: number
}

export const DEFAULT_QUERY_OPTIONS: QueryUiOptions = {
  maxRows: 500
}

export function clampInt(value: unknown, min: number, max: number, fallback: number): number {
  const parsed = Number.parseInt(String(value ?? '').trim(), 10)
  if (!Number.isFinite(parsed)) {
    return fallback
  }
  if (parsed < min) {
    return min
  }
  if (parsed > max) {
    return max
  }
  return parsed
}
