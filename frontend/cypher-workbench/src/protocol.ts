export const CHANNEL_QUERY_EXECUTE = 'ja.query.execute'
export const CHANNEL_QUERY_EXPLAIN = 'ja.query.explain'
export const CHANNEL_QUERY_CAPABILITIES = 'ja.query.capabilities'
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

export type QueryProfile = 'default' | 'long-chain'

export interface QueryUiOptions {
  profile: QueryProfile
  maxRows: number
  maxMs: number
  maxHops: number
  maxPaths: number
}

export const DEFAULT_QUERY_OPTIONS: QueryUiOptions = {
  profile: 'default',
  maxRows: 500,
  maxMs: 15000,
  maxHops: 32,
  maxPaths: 500
}

export function normalizeCapabilities(raw: unknown): Record<string, unknown> | null {
  if (!raw || typeof raw !== 'object') {
    return null
  }
  const object = raw as Record<string, unknown>
  const nested = object.capabilities
  if (nested && typeof nested === 'object' && !Array.isArray(nested)) {
    return nested as Record<string, unknown>
  }
  return object
}

export function normalizeProfile(value: unknown): QueryProfile {
  return value === 'long-chain' ? 'long-chain' : 'default'
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
