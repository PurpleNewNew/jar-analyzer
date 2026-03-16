import './style.css'
import * as d3 from 'd3'
import { EditorState } from '@codemirror/state'
import { EditorView, keymap } from '@codemirror/view'
import { defaultKeymap, history, historyKeymap } from '@codemirror/commands'
import { sql } from '@codemirror/lang-sql'
import { bridgeCall, extractBridgeError, safeBridgeCall } from './bridge'
import {
  buildGraphFilterBar,
  edgeDisplayGroup,
  renderInspector,
  type FrameSelection,
  type GraphFilter
} from './inspector'
import {
  CHANNEL_QUERY_EXECUTE,
  CHANNEL_SCRIPT_DELETE,
  CHANNEL_SCRIPT_LIST,
  CHANNEL_SCRIPT_SAVE,
  CHANNEL_UI_CONTEXT,
  CHANNEL_UI_FULLSCREEN,
  clampInt,
  DEFAULT_QUERY_OPTIONS,
  type GraphEdgePayload,
  type GraphFramePayload,
  type GraphNodePayload,
  type QueryFramePayload,
  type QueryFrameRequestResult,
  type QueryUiOptions,
  type ScriptItem,
  type ScriptListResponse,
  type UiContext
} from './protocol'
import { buildScriptsCountText, renderScriptList } from './scripts'
import { isRecord, toCell } from './view-helpers'

declare global {
  interface Window {
    JA_WORKBENCH?: {
      updateUiContext: (ctx: UiContext) => void
      setFullscreen: (fullscreen: boolean) => void
      onHostVisibility: (visible: boolean) => void
      onHostResize: (width: number, height: number) => void
    }
    __JA_BOOT_ERROR?: string
  }
}

const MAX_FRAMES = 50
const TABLE_ROW_HEIGHT = 26
const FRAME_PREVIEW_HEIGHT = 86
const FRAME_STACK_GAP = 8
const QUERY_OPTIONS_STORAGE_KEY = 'ja.workbench.query-options.v3'
const TRAVERSAL_MODE_STORAGE_KEY = 'ja.workbench.traversal-mode.v1'

type FrameViewMode = 'graph' | 'table' | 'text'
type TraversalMode = 'call-only' | 'call+alias'
type GraphViewport = { x: number; y: number; k: number }
type GraphNodePosition = { x: number; y: number }
type NoticeKind = 'info' | 'success' | 'error'

interface FrameState {
  frameId: string
  query: string
  columns: string[]
  rows: unknown[][]
  warnings: string[]
  truncated: boolean
  elapsedMs: number
  graph: GraphFramePayload | null
  text: string
  collapsed: boolean
  view: FrameViewMode
  selection: FrameSelection
  tableFocus: { rowIndex: number; colIndex: number } | null
  graphFocus: { nodeIds: number[]; edgeIds: number[] } | null
  graphFilter: GraphFilter
  graphViewport: GraphViewport | null
  graphPinnedNodeIds: number[]
  graphNodePositions: Record<number, GraphNodePosition>
  layoutMeasured: boolean
  errorCode?: string
  errorMessage?: string
}

interface GraphViewController {
  sync: (frame: FrameState, graph: GraphFramePayload, restartSimulation?: boolean) => void
  setVisible: (visible: boolean) => void
  requestLayoutSync: () => void
  destroy: () => void
}

interface TableViewController {
  sync: (frame: FrameState) => void
  setVisible: (visible: boolean) => void
  requestLayoutSync: () => void
  destroy: () => void
}

interface FrameBodyController {
  element: HTMLElement
  sync: (frame: FrameState, restartSimulation?: boolean) => void
  setHostVisible: (visible: boolean) => void
  requestLayoutSync: () => void
  destroy: () => void
}

interface FrameCardController {
  card: HTMLElement
  sync: (frame: FrameState, restartSimulation?: boolean) => void
  setHostVisible: (visible: boolean) => void
  requestLayoutSync: () => void
  destroy: () => void
}

type GraphNodeSim = GraphNodePayload & d3.SimulationNodeDatum

type GraphEdgeSim = d3.SimulationLinkDatum<GraphNodeSim> & GraphEdgePayload
type EdgeRouteMeta = {
  lane: number
  isLoop: boolean
  loopIndex: number
}
type GraphThemeColors = {
  accent: string
  selected: string
  edgeStroke: string
  nodeStroke: string
  nodeFillMethod: string
  nodeFillClass: string
  nodeFillCall: string
  nodeFillDefault: string
  text: string
  textOutline: string
  edgeText: string
  ring: string
}

const GRAPH_LABEL_FONT = '600 11px "SF Mono", "JetBrains Mono", "Consolas", monospace'
const textWidthCache = new Map<string, number>()
let textMeasureContext: CanvasRenderingContext2D | null = null

const state = {
  ui: {
    language: 'zh',
    theme: 'default'
  } as UiContext,
  fullscreen: false,
  hostVisible: true,
  frames: [] as FrameState[],
  selectedFrameId: '',
  scripts: [] as ScriptItem[],
  queryOptions: { ...DEFAULT_QUERY_OPTIONS } as QueryUiOptions,
  traversalMode: 'call-only' as TraversalMode,
  notice: null as { kind: NoticeKind; text: string } | null
}

const frameControllers = new Map<string, FrameCardController>()
const pendingLayoutRefresh = new Set<string>()

installFatalBootHandlers()

const app = document.getElementById('app')
if (!app) {
  throw new Error('app root not found')
}

app.innerHTML = `
  <div class="workbench" id="workbench-root" data-theme="default">
    <header class="wb-header">
      <div class="wb-header-main">
        <div class="wb-title" id="title"></div>
        <div class="wb-notice" id="notice" hidden></div>
      </div>
      <div class="wb-actions">
        <button class="wb-btn" id="btn-save" title="Save Script">☆</button>
        <button class="wb-btn" id="btn-refresh" title="Refresh Scripts">↻</button>
        <button class="wb-btn" id="btn-fullscreen" title="Fullscreen">全屏</button>
      </div>
    </header>
    <section class="command-bar">
      <div class="prompt">$</div>
      <div class="editor-wrap"><div id="editor"></div></div>
      <div class="command-controls">
        <div class="traversal-mode-segment">
          <button class="traversal-mode-btn active" id="mode-call-only" type="button">CALL</button>
          <button class="traversal-mode-btn" id="mode-call-alias" type="button">CALL + ALIAS</button>
        </div>
        <div class="query-opts">
          <label class="opt-item">
          <input id="opt-max-rows" type="number" min="1" max="10000" step="1" />
          </label>
        </div>
        <button class="wb-btn primary" id="btn-run">Run</button>
      </div>
    </section>
    <section class="main-grid">
      <aside class="scripts-pane">
        <div class="pane-head">
          <span id="scripts-title"></span>
          <span id="scripts-count"></span>
        </div>
        <div id="script-list"></div>
      </aside>
      <main class="frames-pane" id="frames"></main>
    </section>
  </div>
`

const root = getRequired<HTMLElement>('workbench-root')
const titleEl = getRequired<HTMLElement>('title')
const noticeEl = getRequired<HTMLElement>('notice')
const scriptsTitleEl = getRequired<HTMLElement>('scripts-title')
const scriptsCountEl = getRequired<HTMLElement>('scripts-count')
const framesEl = getRequired<HTMLElement>('frames')
framesEl.innerHTML = '<div class="frames-rail" id="frames-rail"></div>'
const framesRailEl = getRequired<HTMLElement>('frames-rail')
const scriptListEl = getRequired<HTMLElement>('script-list')
const runButton = getRequired<HTMLButtonElement>('btn-run')
const refreshButton = getRequired<HTMLButtonElement>('btn-refresh')
const saveButton = getRequired<HTMLButtonElement>('btn-save')
const fullscreenButton = getRequired<HTMLButtonElement>('btn-fullscreen')
const editorHost = getRequired<HTMLElement>('editor')
const maxRowsInput = getRequired<HTMLInputElement>('opt-max-rows')
const modeCallOnlyButton = getRequired<HTMLButtonElement>('mode-call-only')
const modeCallAliasButton = getRequired<HTMLButtonElement>('mode-call-alias')

let suppressFrameScrollSelection = false
let frameViewportSyncScheduled = false
let workbenchLayoutSyncScheduled = false
let lastFrameViewportHeight = -1

let editor = new EditorView({
  state: EditorState.create({
    doc: 'MATCH (m:Method)-[r:CALL]->(n:Method) RETURN m,r,n LIMIT 50',
    extensions: [
      history(),
      sql(),
      keymap.of([
        ...defaultKeymap,
        ...historyKeymap,
        {
          key: 'Enter',
          preventDefault: true,
          run: () => {
            void runQuery()
            return true
          }
        },
        {
          key: 'ArrowUp',
          preventDefault: true,
          run: () => {
            if (editor.state.doc.toString().trim().length > 0) {
              return false
            }
            selectPrevFrame()
            return true
          }
        },
        {
          key: 'ArrowDown',
          preventDefault: true,
          run: () => {
            if (editor.state.doc.toString().trim().length > 0) {
              return false
            }
            selectNextFrame()
            return true
          }
        }
      ]),
      EditorView.lineWrapping,
      EditorView.theme({
        '&': {
          fontSize: '15px'
        }
      })
    ]
  }),
  parent: editorHost
})

runButton.addEventListener('click', () => {
  void runQuery()
})

refreshButton.addEventListener('click', () => {
  void refreshScripts()
})

saveButton.addEventListener('click', () => {
  void saveScriptFromEditor()
})

fullscreenButton.addEventListener('click', () => {
  void requestFullscreen(!state.fullscreen)
})

hydrateQueryOptions()
hydrateTraversalMode()
syncQueryOptionControls()

maxRowsInput.addEventListener('change', () => {
  state.queryOptions.maxRows = clampInt(maxRowsInput.value, 1, 10000, state.queryOptions.maxRows)
  persistQueryOptions()
  syncQueryOptionControls()
})

modeCallOnlyButton.addEventListener('click', () => {
  setTraversalMode('call-only')
})

modeCallAliasButton.addEventListener('click', () => {
  setTraversalMode('call+alias')
})

window.JA_WORKBENCH = {
  updateUiContext: (ctx: UiContext) => {
    applyUiContext(ctx)
  },
  setFullscreen: (fullscreen: boolean) => {
    state.fullscreen = !!fullscreen
    refreshHeaderLabels()
  },
  onHostVisibility: (visible: boolean) => {
    state.hostVisible = !!visible
    updateSimulationVisibility()
  },
  onHostResize: (_width: number, _height: number) => {
    requestWorkbenchLayoutSync(3)
  }
}

void bootstrap()

function installFatalBootHandlers(): void {
  const report = (message: string): void => {
    const text = (message || '').trim()
    if (!text || isIgnorableRuntimeMessage(text)) {
      return
    }
    window.__JA_BOOT_ERROR = text
    const root = document.getElementById('app')
    if (root && root.childElementCount > 0) {
      setNotice('error', humanizeErrorMessage(text))
      return
    }
    const host = root || document.body
    if (!host) {
      return
    }
    host.innerHTML = ''
    const panel = document.createElement('pre')
    panel.style.margin = '12px'
    panel.style.padding = '10px'
    panel.style.border = '1px solid #c0392b'
    panel.style.background = '#fff5f4'
    panel.style.color = '#912018'
    panel.style.whiteSpace = 'pre-wrap'
    panel.style.fontFamily = 'JetBrains Mono, Consolas, monospace'
    panel.textContent = `[cypher-workbench boot error]\\n${text}`
    host.appendChild(panel)
  }

  window.addEventListener('error', (event) => {
    const msg = event?.error?.stack || event?.message || 'unknown runtime error'
    report(String(msg))
  })
  window.addEventListener('unhandledrejection', (event) => {
    const reason = (event as PromiseRejectionEvent)?.reason
    let msg = 'unhandled rejection'
    if (typeof reason === 'string') {
      msg = reason
    } else if (reason != null) {
      try {
        msg = JSON.stringify(reason)
      } catch {
        msg = String(reason)
      }
    }
    report(msg || 'unhandled rejection')
  })
}

async function bootstrap(): Promise<void> {
  const uiContext = await safeBridgeCall<UiContext>(CHANNEL_UI_CONTEXT, {})
  if (uiContext) {
    applyUiContext(uiContext)
  }
  await refreshScripts()
  renderAll()
}

function applyUiContext(ctx: UiContext): void {
  const language = ctx?.language === 'en' ? 'en' : 'zh'
  const theme = ctx?.theme === 'dark' ? 'dark' : 'default'
  state.ui.language = language
  state.ui.theme = theme
  root.setAttribute('data-theme', theme)
  refreshHeaderLabels()
  renderScripts()
  renderFrames()
  requestWorkbenchLayoutSync(2)
}

function refreshHeaderLabels(): void {
  titleEl.textContent = 'Graph Console'
  scriptsTitleEl.textContent = tr('模板与脚本', 'Templates & Scripts')
  scriptsCountEl.textContent = buildScriptsCountText(state.scripts.length, tr)
  runButton.textContent = tr('运行', 'Run')
  refreshButton.textContent = tr('刷新', 'Refresh')
  saveButton.textContent = tr('收藏', 'Save')
  fullscreenButton.textContent = state.fullscreen
    ? tr('退出全屏', 'Exit Fullscreen')
    : tr('全屏', 'Fullscreen')
  fullscreenButton.title = state.fullscreen ? tr('退出全屏', 'Exit Fullscreen') : tr('全屏', 'Fullscreen')
  maxRowsInput.title = tr('最大返回行数', 'Max result rows')
  maxRowsInput.setAttribute('aria-label', tr('最大返回行数', 'Max result rows'))
  modeCallOnlyButton.textContent = 'CALL'
  modeCallAliasButton.textContent = 'CALL + ALIAS'
  modeCallOnlyButton.title = tr('仅遍历 CALL 边', 'Traverse CALL edges only')
  modeCallAliasButton.title = tr('遍历 CALL 与 ALIAS 边', 'Traverse CALL and ALIAS edges')
  modeCallOnlyButton.classList.toggle('active', state.traversalMode === 'call-only')
  modeCallAliasButton.classList.toggle('active', state.traversalMode === 'call+alias')
  syncQueryOptionControls()
  refreshNotice()
}

function refreshNotice(): void {
  const notice = state.notice
  if (!notice || !notice.text.trim()) {
    noticeEl.hidden = true
    noticeEl.textContent = ''
    noticeEl.className = 'wb-notice'
    noticeEl.removeAttribute('title')
    return
  }
  const text = notice.text.trim()
  noticeEl.hidden = false
  noticeEl.textContent = text
  noticeEl.title = text
  noticeEl.className = `wb-notice ${notice.kind}`
}

function setNotice(kind: NoticeKind, text: string): void {
  const normalized = text.trim()
  state.notice = normalized ? { kind, text: normalized } : null
  refreshNotice()
}

function clearNotice(): void {
  if (!state.notice) {
    return
  }
  state.notice = null
  refreshNotice()
}

function cssVar(name: string, fallback: string): string {
  const value = getComputedStyle(root).getPropertyValue(name).trim()
  return value || fallback
}

function graphThemeColors(): GraphThemeColors {
  return {
    accent: cssVar('--accent', '#2f81f7'),
    selected: cssVar('--warn', '#fdcc59'),
    edgeStroke: cssVar('--graph-edge-stroke', '#8e98a6'),
    nodeStroke: cssVar('--graph-node-stroke', '#2f3640'),
    nodeFillMethod: cssVar('--graph-node-fill-method', '#8fc5ff'),
    nodeFillClass: cssVar('--graph-node-fill-class', '#ffb283'),
    nodeFillCall: cssVar('--graph-node-fill-call', '#a6e3bc'),
    nodeFillDefault: cssVar('--graph-node-fill-default', '#d6dae0'),
    text: cssVar('--graph-text', '#1f2933'),
    textOutline: cssVar('--graph-text-outline', 'rgba(248, 250, 252, 0.94)'),
    edgeText: cssVar('--graph-edge-text', '#5f6b78'),
    ring: cssVar('--graph-ring', '#6ac6ff')
  }
}

installFrameRailBehavior()

function installFrameRailBehavior(): void {
  framesEl.addEventListener('scroll', handleFrameRailScroll, { passive: true })
  window.addEventListener('resize', handleWorkbenchResize)
  scheduleFrameViewportHeightSync()
}

function updateFrameViewportHeight(): void {
  const nextHeight = Math.max(0, Math.round(framesEl.clientHeight))
  if (nextHeight === lastFrameViewportHeight) {
    return
  }
  lastFrameViewportHeight = nextHeight
  framesEl.style.setProperty('--frames-viewport-height', `${nextHeight}px`)
}

function scheduleFrameViewportHeightSync(): void {
  if (frameViewportSyncScheduled) {
    return
  }
  frameViewportSyncScheduled = true
  requestAnimationFrame(() => {
    frameViewportSyncScheduled = false
    updateFrameViewportHeight()
  })
}

function requestWorkbenchLayoutSync(passes = 1): void {
  if (workbenchLayoutSyncScheduled) {
    return
  }
  workbenchLayoutSyncScheduled = true
  let remainingPasses = Math.max(1, passes)
  const runPass = (): void => {
    requestAnimationFrame(() => {
      scheduleFrameViewportHeightSync()
      const selectedFrameId = state.selectedFrameId || state.frames.find((item) => !item.collapsed)?.frameId || ''
      if (selectedFrameId) {
        frameControllers.get(selectedFrameId)?.requestLayoutSync()
      }
      remainingPasses -= 1
      if (remainingPasses > 0) {
        runPass()
        return
      }
      workbenchLayoutSyncScheduled = false
    })
  }
  runPass()
}

function handleWorkbenchResize(): void {
  requestWorkbenchLayoutSync(2)
}

function isIgnorableRuntimeMessage(message: string): boolean {
  const text = message.trim()
  return text.includes('ResizeObserver loop completed with undelivered notifications')
    || text.includes('ResizeObserver loop limit exceeded')
}

function handleFrameRailScroll(): void {
  if (suppressFrameScrollSelection || state.frames.length <= 1) {
    return
  }
  requestAnimationFrame(() => {
    if (suppressFrameScrollSelection || state.frames.length <= 1) {
      return
    }
    const anchorY = framesEl.scrollTop + FRAME_PREVIEW_HEIGHT + FRAME_STACK_GAP + 4
    let candidateId = state.frames[0]?.frameId || ''
    for (const frame of state.frames) {
      const card = frameControllers.get(frame.frameId)?.card
      if (!card) {
        continue
      }
      if (card.offsetTop <= anchorY) {
        candidateId = frame.frameId
        continue
      }
      break
    }
    if (candidateId && candidateId !== state.selectedFrameId) {
      expandFrame(candidateId, 'preserve')
    }
  })
}

function withSuppressedFrameScrollSelection(action: () => void, timeoutMs = 280): void {
  suppressFrameScrollSelection = true
  action()
  window.setTimeout(() => {
    suppressFrameScrollSelection = false
  }, timeoutMs)
}

function normalizeTraversalMode(value: unknown): TraversalMode {
  return value === 'call+alias' ? 'call+alias' : 'call-only'
}

function setTraversalMode(mode: TraversalMode): void {
  const next = normalizeTraversalMode(mode)
  if (state.traversalMode === next) {
    return
  }
  state.traversalMode = next
  persistTraversalMode()
  refreshHeaderLabels()
}

function syncQueryOptionControls(): void {
  maxRowsInput.value = String(clampInt(state.queryOptions.maxRows, 1, 10000, DEFAULT_QUERY_OPTIONS.maxRows))
}

function persistQueryOptions(): void {
  try {
    localStorage.setItem(QUERY_OPTIONS_STORAGE_KEY, JSON.stringify(state.queryOptions))
  } catch {
    // ignore storage failures in embedded runtime
  }
}

function hydrateQueryOptions(): void {
  try {
    const raw = localStorage.getItem(QUERY_OPTIONS_STORAGE_KEY)
    if (!raw) {
      return
    }
    const parsed = JSON.parse(raw) as Partial<QueryUiOptions> | null
    if (!parsed || typeof parsed !== 'object') {
      return
    }
    state.queryOptions = {
      maxRows: clampInt(parsed.maxRows, 1, 10000, state.queryOptions.maxRows)
    }
  } catch {
    // ignore broken cache
  }
}

function persistTraversalMode(): void {
  try {
    localStorage.setItem(TRAVERSAL_MODE_STORAGE_KEY, state.traversalMode)
  } catch {
    // ignore storage failures in embedded runtime
  }
}

function hydrateTraversalMode(): void {
  try {
    const raw = localStorage.getItem(TRAVERSAL_MODE_STORAGE_KEY)
    if (!raw) {
      return
    }
    state.traversalMode = normalizeTraversalMode(raw)
  } catch {
    // ignore broken cache
  }
}

function materializeQuery(query: string): string {
  return query
    .replaceAll('{{TRAVERSAL_MODE_LITERAL}}', JSON.stringify(state.traversalMode))
    .replaceAll('{{TRAVERSAL_MODE}}', state.traversalMode)
}

async function runQuery(queryInput?: string): Promise<void> {
  const rawQuery = (queryInput ?? editor.state.doc.toString()).trim()
  if (!rawQuery) {
    return
  }
  const query = materializeQuery(rawQuery)
  runButton.disabled = true
  try {
    const options = { ...state.queryOptions }
    const response = await bridgeCall<QueryFrameRequestResult>(CHANNEL_QUERY_EXECUTE, {
      query,
      params: {},
      options
    })
    if (!response.ok || !response.frame) {
      pushFrame(buildErrorFrame(query, response.code || 'cypher_query_error', response.message || 'query failed'))
      return
    }
    pushFrame(toFrameState(response.frame))
  } catch (error) {
    const bridgeError = extractBridgeError(error, 'bridge_error')
    pushFrame(buildErrorFrame(query, bridgeError.code, bridgeError.message))
  } finally {
    runButton.disabled = false
  }
}

function pushFrame(frame: FrameState): void {
  state.frames = [
    frame,
    ...state.frames
      .filter((item) => item.frameId !== frame.frameId)
      .map((item) => {
        item.collapsed = true
        return item
      })
  ].slice(0, MAX_FRAMES)
  frame.collapsed = false
  state.selectedFrameId = frame.frameId
  renderFrames()
  requestAnimationFrame(() => {
    withSuppressedFrameScrollSelection(() => {
      framesEl.scrollTo({ top: 0, behavior: 'smooth' })
    })
  })
}

function toFrameState(frame: QueryFramePayload): FrameState {
  const graph = normalizeGraph(frame.graph)
  return {
    frameId: frame.frameId,
    query: frame.query,
    columns: frame.columns || [],
    rows: frame.rows || [],
    warnings: frame.warnings || [],
    truncated: !!frame.truncated,
    elapsedMs: frame.elapsedMs || 0,
    graph,
    text: frame.text || '',
    collapsed: false,
    view: graph && graph.nodes.length > 0 ? 'graph' : 'table',
    selection: null,
    tableFocus: null,
    graphFocus: null,
    graphFilter: null,
    graphViewport: null,
    graphPinnedNodeIds: [],
    graphNodePositions: {},
    layoutMeasured: false
  }
}

function normalizeGraph(graph?: GraphFramePayload): GraphFramePayload | null {
  if (!graph) {
    return null
  }
  const warnings = Array.isArray(graph.warnings) ? [...graph.warnings] : []
  const nodeMap = new Map<number, GraphNodePayload>()
  let droppedNodes = 0
  if (Array.isArray(graph.nodes)) {
    for (const item of graph.nodes) {
      const props = isRecord(item?.properties) ? item.properties : {}
      const nodeId = resolveGraphNodeId(item, props)
      if (nodeId <= 0) {
        droppedNodes++
        continue
      }
      nodeMap.set(nodeId, {
        ...item,
        id: nodeId,
        jarId: firstNumericField(item as Record<string, unknown>, 'jarId', 'jar_id') >= 0
          ? firstNumericField(item as Record<string, unknown>, 'jarId', 'jar_id')
          : firstNumericField(props, 'jarId', 'jar_id'),
        labels: Array.isArray(item?.labels) ? item.labels : [],
        properties: props
      })
    }
  }
  let droppedEdges = 0
  const edges: GraphEdgePayload[] = []
  if (Array.isArray(graph.edges)) {
    for (const item of graph.edges) {
      const props = isRecord(item?.properties) ? item.properties : {}
      const source = resolveGraphEdgeNodeId(item as Record<string, unknown>, props, 'source', 'src_id', 'startNodeId', 'start_node_id')
      const target = resolveGraphEdgeNodeId(item as Record<string, unknown>, props, 'target', 'dst_id', 'endNodeId', 'end_node_id')
      if (source <= 0 || target <= 0 || !nodeMap.has(source) || !nodeMap.has(target)) {
        droppedEdges++
        continue
      }
      edges.push({
        ...item,
        id: resolveGraphEdgeId(item as Record<string, unknown>, props),
        source,
        target,
        properties: props
      })
    }
  }
  if (droppedNodes > 0) {
    warnings.push(`graph_node_dropped:${droppedNodes}`)
  }
  if (droppedEdges > 0) {
    warnings.push(`graph_edge_dropped:${droppedEdges}`)
  }
  return {
    nodes: Array.from(nodeMap.values()),
    edges,
    warnings,
    truncated: !!graph.truncated
  }
}

function buildErrorFrame(query: string, code: string, message: string): FrameState {
  return {
    frameId: `error-${Date.now()}`,
    query,
    columns: ['code', 'message'],
    rows: [[code, message]],
    warnings: [message],
    truncated: false,
    elapsedMs: 0,
    graph: null,
    text: JSON.stringify({ code, message }, null, 2),
    collapsed: false,
    view: 'text',
    selection: null,
    tableFocus: null,
    graphFocus: null,
    graphFilter: null,
    graphViewport: null,
    graphPinnedNodeIds: [],
    graphNodePositions: {},
    layoutMeasured: false,
    errorCode: code,
    errorMessage: message
  }
}

function renderAll(): void {
  refreshHeaderLabels()
  renderScripts()
  renderFrames()
}

function renderScripts(): void {
  scriptsCountEl.textContent = buildScriptsCountText(state.scripts.length, tr)
  renderScriptList({
    container: scriptListEl,
    scripts: state.scripts,
    tr,
    applyScriptBody,
    togglePinned: (item) => {
      void saveScript({
        scriptId: item.scriptId,
        title: item.title,
        body: item.body,
        tags: item.tags,
        pinned: !item.pinned
      })
    },
    deleteScript: (scriptId) => {
      void deleteScript(scriptId)
    }
  })
}

function renderFrames(): void {
  if (state.frames.length === 0) {
    disposeAllFrameResources()
    framesRailEl.innerHTML = ''
    renderEmptyFrames()
    requestWorkbenchLayoutSync(1)
    return
  }

  clearEmptyFrames()
  const activeFrameIds = new Set(state.frames.map((frame) => frame.frameId))
  for (const [frameId, controller] of frameControllers.entries()) {
    if (activeFrameIds.has(frameId)) {
      continue
    }
    controller.destroy()
    frameControllers.delete(frameId)
  }

  for (const frame of state.frames) {
    let controller = frameControllers.get(frame.frameId)
    if (!controller) {
      controller = createFrameCardController(frame)
      frameControllers.set(frame.frameId, controller)
    }
    controller.sync(frame)
    scheduleFrameLayoutRefresh(frame)
  }

  let anchor: ChildNode | null = framesRailEl.firstChild
  for (const frame of state.frames) {
    const controller = frameControllers.get(frame.frameId)
    if (!controller) {
      continue
    }
    const card = controller.card
    if (card.parentElement !== framesRailEl) {
      framesRailEl.insertBefore(card, anchor)
    } else if (card !== anchor) {
      framesRailEl.insertBefore(card, anchor)
    }
    anchor = card.nextSibling
  }

  updateSelectedFrameClasses()
  updateSimulationVisibility()
  requestWorkbenchLayoutSync(2)
}

function renderEmptyFrames(): void {
  const empty = document.createElement('div')
  empty.id = 'frames-empty'
  empty.className = 'empty'
  empty.textContent = tr('输入 Cypher 后按 Enter 运行', 'Type Cypher and press Enter')
  framesRailEl.appendChild(empty)
}

function clearEmptyFrames(): void {
  const empty = document.getElementById('frames-empty')
  if (empty && empty.parentElement === framesRailEl) {
    empty.remove()
  }
}

function createFrameCardController(initialFrame: FrameState): FrameCardController {
  let frame = initialFrame
  const card = document.createElement('section')
  card.addEventListener('click', () => {
    selectFrame(frame.frameId)
  })

  const head = document.createElement('div')
  head.className = 'frame-head'

  const fold = createFrameButton(frame.collapsed ? '▸' : '▾', frame.collapsed ? tr('展开', 'Expand') : tr('收起', 'Collapse'))
  fold.addEventListener('click', (event) => {
    event.stopPropagation()
    if (frame.collapsed) {
      expandFrame(frame.frameId, 'anchor')
      return
    }
    const nextFrameId = adjacentFrameId(frame.frameId)
    if (!nextFrameId) {
      return
    }
    expandFrame(nextFrameId, 'anchor')
  })

  const query = document.createElement('div')
  query.className = 'frame-query'

  const actions = document.createElement('div')
  actions.className = 'frame-actions'

  const run = createFrameButton('▶', tr('再运行', 'Run Again'))
  run.addEventListener('click', (event) => {
    event.stopPropagation()
    void runQuery(frame.query)
  })

  const save = createFrameButton('★', tr('收藏脚本', 'Save Script'))
  save.addEventListener('click', (event) => {
    event.stopPropagation()
    void saveScriptFromFrame(frame)
  })

  const full = createFrameButton(tr('全屏', 'Full'), tr('全屏', 'Fullscreen'))
  full.addEventListener('click', (event) => {
    event.stopPropagation()
    void requestFullscreen(!state.fullscreen)
  })

  const close = createFrameButton('✕', tr('关闭', 'Close'))
  close.addEventListener('click', (event) => {
    event.stopPropagation()
    const fallbackFrameId = adjacentFrameId(frame.frameId)
    state.frames = state.frames.filter((item) => item.frameId !== frame.frameId)
    if (state.frames.length === 0) {
      state.selectedFrameId = ''
      renderFrames()
      return
    }
    if (state.selectedFrameId === frame.frameId) {
      expandFrame(fallbackFrameId || state.frames[0].frameId, 'preserve')
      return
    }
    renderFrames()
  })

  actions.append(run, save, full, close)
  head.append(fold, query, actions)
  card.appendChild(head)

  const status = document.createElement('div')
  status.className = 'frame-status'
  card.appendChild(status)

  const filterHost = document.createElement('div')
  card.appendChild(filterHost)

  const bodyController = createFrameBodyController(
    () => frame,
    (restartSimulation) => refreshFrame(frame, restartSimulation)
  )
  const bodyShell = document.createElement('div')
  bodyShell.className = 'frame-body-shell'
  bodyShell.appendChild(bodyController.element)
  card.appendChild(bodyShell)

  const sync = (nextFrame: FrameState, restartSimulation = false): void => {
    frame = nextFrame
    card.className = `frame${frame.frameId === state.selectedFrameId ? ' selected' : ''}${frame.collapsed ? ' collapsed' : ''}`
    fold.textContent = frame.collapsed ? '▸' : '▾'
    fold.title = frame.collapsed ? tr('展开', 'Expand') : tr('收起', 'Collapse')
    fold.setAttribute('aria-expanded', String(!frame.collapsed))
    query.textContent = `$ ${frame.query}`
    full.textContent = tr('全屏', 'Full')
    full.title = tr('全屏', 'Fullscreen')
    bodyShell.setAttribute('aria-hidden', String(frame.collapsed))
    status.innerHTML = ''
    status.appendChild(createBadge(`${tr('耗时', 'elapsed')} ${frame.elapsedMs}ms`, frame.errorCode ? 'err' : 'ok'))
    status.appendChild(createBadge(`${tr('行', 'rows')} ${frame.rows.length}`))
    if (frame.truncated) {
      status.appendChild(createBadge(tr('结果截断', 'truncated'), 'warn'))
    }
    if (frame.errorCode) {
      status.appendChild(createBadge(frame.errorCode, 'err'))
    }
    for (const warning of (frame.graph?.warnings || frame.warnings).slice(0, 2)) {
      status.appendChild(createBadge(warning, 'warn'))
    }
    filterHost.innerHTML = ''
    filterHost.hidden = frame.collapsed || !frame.graphFilter
    if (!filterHost.hidden && frame.graphFilter) {
      filterHost.appendChild(
        buildGraphFilterBar(frame, {
          tr,
          clearGraphFilter: () => {
            frame.graphFilter = null
            refreshFrame(frame, false)
          }
        })
      )
    }
    bodyController.sync(frame, restartSimulation)
  }

  sync(frame)
  return {
    card,
    sync,
    setHostVisible: (visible) => {
      bodyController.setHostVisible(visible)
    },
    requestLayoutSync: () => {
      bodyController.requestLayoutSync()
    },
    destroy: () => {
      bodyController.destroy()
      card.remove()
    }
  }
}

function refreshFrame(frame: FrameState, restartSimulation = true): void {
  const controller = frameControllers.get(frame.frameId)
  if (!controller) {
    renderFrames()
    return
  }
  controller.sync(frame, restartSimulation)
  updateSelectedFrameClasses()
  controller.setHostVisible(state.hostVisible)
}

function expandFrame(frameId: string, mode: 'anchor' | 'preserve' | 'none' = 'anchor'): void {
  if (!frameId) {
    return
  }
  const selected = state.frames.find((item) => item.frameId === frameId)
  if (!selected) {
    return
  }
  if (state.selectedFrameId === frameId && !selected.collapsed) {
    return
  }
  const selectedCard = frameControllers.get(frameId)?.card || null
  const previousTop = selectedCard?.offsetTop || 0
  const previewOffset = Math.max(0, FRAME_PREVIEW_HEIGHT + FRAME_STACK_GAP)
  for (const frame of state.frames) {
    frame.collapsed = frame.frameId !== frameId
  }
  state.selectedFrameId = frameId
  renderFrames()
  if (mode === 'none') {
    return
  }
  requestAnimationFrame(() => {
    const nextCard = frameControllers.get(frameId)?.card
    if (!nextCard) {
      return
    }
    if (mode === 'anchor') {
      const targetTop = Math.max(0, nextCard.offsetTop - previewOffset)
      withSuppressedFrameScrollSelection(() => {
        framesEl.scrollTo({ top: targetTop, behavior: 'smooth' })
      })
      return
    }
    const nextTop = nextCard.offsetTop
    const delta = nextTop - previousTop
    if (Math.abs(delta) < 1) {
      return
    }
    withSuppressedFrameScrollSelection(() => {
      framesEl.scrollTop += delta
    })
  })
}

function selectFrame(frameId: string): void {
  if (!frameId) {
    return
  }
  const selected = state.frames.find((item) => item.frameId === frameId)
  if (!selected) {
    return
  }
  if (state.selectedFrameId === frameId && !selected.collapsed) {
    return
  }
  expandFrame(frameId, 'anchor')
}

function updateSelectedFrameClasses(): void {
  for (const [frameId, controller] of frameControllers.entries()) {
    controller.card.classList.toggle('selected', frameId === state.selectedFrameId)
  }
}

function adjacentFrameId(frameId: string): string {
  const index = state.frames.findIndex((item) => item.frameId === frameId)
  if (index < 0) {
    return ''
  }
  return state.frames[index + 1]?.frameId || state.frames[index - 1]?.frameId || ''
}

function scheduleFrameLayoutRefresh(frame: FrameState): void {
  if (frame.collapsed) {
    return
  }
  if (!frame || frame.layoutMeasured || pendingLayoutRefresh.has(frame.frameId)) {
    return
  }
  pendingLayoutRefresh.add(frame.frameId)
  requestAnimationFrame(() => {
    pendingLayoutRefresh.delete(frame.frameId)
    if (!frameControllers.has(frame.frameId) || frame.layoutMeasured) {
      return
    }
    frame.layoutMeasured = true
    refreshFrame(frame, false)
  })
}

function disposeAllFrameResources(): void {
  for (const controller of frameControllers.values()) {
    controller.destroy()
  }
  frameControllers.clear()
}

function createFrameBodyController(
  getFrame: () => FrameState,
  requestFrameRefresh: (restartSimulation: boolean) => void
): FrameBodyController {
  let graphController: GraphViewController | null = null
  let tableController: TableViewController | null = null
  let graphKey = ''
  let textKey = ''
  let inspectorKey = ''
  let hostVisible = state.hostVisible

  const body = document.createElement('div')
  body.className = 'frame-body'

  const tabs = document.createElement('div')
  tabs.className = 'view-tabs'

  const graphTab = createTabButton('Graph', false, false)
  const tableTab = createTabButton('Table', false, false)
  const textTab = createTabButton('Text', false, false)

  graphTab.addEventListener('click', () => {
    const frame = getFrame()
    if (!frame.graph || frame.graph.nodes.length === 0) {
      return
    }
    frame.view = 'graph'
    refreshFrame(frame, true)
  })
  tableTab.addEventListener('click', () => {
    const frame = getFrame()
    frame.view = 'table'
    refreshFrame(frame, false)
  })
  textTab.addEventListener('click', () => {
    const frame = getFrame()
    frame.view = 'text'
    refreshFrame(frame, false)
  })

  tabs.append(graphTab, tableTab, textTab)

  const content = document.createElement('div')
  content.className = 'view-content'

  const graphView = document.createElement('div')
  graphView.className = 'graph-view'
  const tableView = document.createElement('div')
  tableView.className = 'table-view'
  const textView = document.createElement('div')
  textView.className = 'text-view'

  content.append(graphView, tableView, textView)

  const propPanel = document.createElement('aside')
  propPanel.className = 'prop-panel'
  body.append(tabs, content, propPanel)

  const applyGraphVisibility = (frame: FrameState): void => {
    graphController?.setVisible(hostVisible && frame.view === 'graph' && !frame.collapsed)
  }

  const applyTableVisibility = (frame: FrameState): void => {
    tableController?.setVisible(frame.view === 'table' && !frame.collapsed)
  }

  const sync = (frame: FrameState, restartSimulation = false): void => {
    if (frame.collapsed) {
      graphController?.setVisible(false)
      tableController?.setVisible(false)
      return
    }

    const filteredGraph = frame.graph ? applyGraphFilter(frame.graph, frame.graphFilter) : null
    const normalizedGraph = normalizeGraph(filteredGraph || undefined)
    const graphEnabled = !!normalizedGraph && normalizedGraph.nodes.length > 0
    const nextGraphKey = graphEnabled ? graphIdentityKey(frame, normalizedGraph) : ''

    graphTab.disabled = !graphEnabled
    graphTab.className = `tab-btn${frame.view === 'graph' ? ' active' : ''}`
    tableTab.className = `tab-btn${frame.view === 'table' ? ' active' : ''}`
    textTab.className = `tab-btn${frame.view === 'text' ? ' active' : ''}`
    graphView.className = `graph-view${frame.view === 'graph' ? ' active' : ''}`
    tableView.className = `table-view${frame.view === 'table' ? ' active' : ''}`
    textView.className = `text-view${frame.view === 'text' ? ' active' : ''}`

    if (graphEnabled && normalizedGraph) {
      if (!graphController || graphKey !== nextGraphKey) {
        graphController?.destroy()
        graphView.innerHTML = ''
        try {
          graphController = mountGraphView(graphView, frame, normalizedGraph, requestFrameRefresh)
          graphKey = nextGraphKey
        } catch (error) {
          graphController = null
          graphKey = ''
          const message = error instanceof Error ? error.message : String(error || 'graph render failed')
          graphView.innerHTML = `<div class="empty">${tr('图渲染失败', 'Graph render failed')}: ${escapeHtml(message)}</div>`
        }
      } else {
        graphController.sync(frame, normalizedGraph, restartSimulation)
      }
    } else {
      graphController?.destroy()
      graphController = null
      graphKey = ''
      graphView.innerHTML = `<div class="empty">${tr('该结果不可图形化', 'Graph unavailable for this result')}</div>`
    }

    if (!tableController) {
      tableController = mountVirtualTable(tableView, frame)
    } else {
      tableController.sync(frame)
    }

    applyGraphVisibility(frame)
    applyTableVisibility(frame)
    const nextTextKey = frameTextRenderKey(frame)
    if (textKey !== nextTextKey) {
      renderTextView(textView, frame)
      textKey = nextTextKey
    }

    const nextInspectorKey = frameInspectorRenderKey(frame, normalizedGraph)
    if (inspectorKey !== nextInspectorKey) {
      renderInspector(propPanel, {
        ...frame,
        graph: normalizedGraph
      }, {
        tr,
        activateLegend: (kind, value) => {
          activateLegend(frame, kind, value)
        },
        activateInspectorLink: (key, value, selected) => {
          activateInspectorLink(frame, key, value, selected)
        },
        activateNeighborhoodFocus: (node) => {
          activateNeighborhoodFocus(frame, node)
        },
        clearGraphFilter: () => {
          frame.graphFilter = null
          refreshFrame(frame, false)
        }
      })
      inspectorKey = nextInspectorKey
    }
  }

  const destroy = (): void => {
    graphController?.destroy()
    graphController = null
    tableController?.destroy()
    tableController = null
  }

  return {
    element: body,
    sync,
    setHostVisible: (visible) => {
      hostVisible = visible
      applyGraphVisibility(getFrame())
    },
    requestLayoutSync: () => {
      graphController?.requestLayoutSync()
      tableController?.requestLayoutSync()
    },
    destroy
  }
}

function graphIdentityKey(frame: FrameState, graph: GraphFramePayload): string {
  const nodeKey = graph.nodes.map((node) => `${node.id}:${node.kind}:${node.label || ''}`).join('|')
  const edgeKey = graph.edges.map((edge) => `${edge.id}:${edge.source}>${edge.target}:${edgeDisplayLabel(edge)}`).join('|')
  return `${frame.frameId}::${nodeKey}::${edgeKey}`
}

function mountGraphView(
  container: HTMLElement,
  initialFrame: FrameState,
  initialGraph: GraphFramePayload,
  requestFrameRefresh: (restartSimulation: boolean) => void
): GraphViewController {
  let currentFrame = initialFrame
  let currentGraph = initialGraph
  const nodes = currentGraph.nodes
  const edges = currentGraph.edges
  if (nodes.length === 0) {
    throw new Error('graph data required')
  }
  container.innerHTML = ''
  const stage = document.createElement('div')
  stage.className = 'graph-stage'
  container.appendChild(stage)
  let visible = false
  const measureStage = (): { width: number; height: number } => {
    return {
      width: Math.max(360, stage.clientWidth || container.clientWidth || 720),
      height: Math.max(260, stage.clientHeight || container.clientHeight || 420)
    }
  }
  const initialSize = measureStage()
  let width = initialSize.width
  let height = initialSize.height
  let resizeScheduled = false
  let pendingResizeRefit = false
  const controls = document.createElement('div')
  controls.className = 'graph-zoom-controls'
  const zoomReadout = document.createElement('div')
  zoomReadout.className = 'graph-zoom-readout'
  const zoomInButton = document.createElement('button')
  zoomInButton.className = 'graph-zoom-btn'
  zoomInButton.type = 'button'
  zoomInButton.textContent = '+'
  zoomInButton.title = tr('放大', 'Zoom in')
  const zoomOutButton = document.createElement('button')
  zoomOutButton.className = 'graph-zoom-btn'
  zoomOutButton.type = 'button'
  zoomOutButton.textContent = '−'
  zoomOutButton.title = tr('缩小', 'Zoom out')
  const fitButton = document.createElement('button')
  fitButton.className = 'graph-zoom-btn graph-zoom-fit'
  fitButton.type = 'button'
  fitButton.textContent = tr('适配', 'Fit')
  fitButton.title = tr('重置视图', 'Reset view')
  controls.append(zoomReadout, zoomInButton, zoomOutButton, fitButton)
  stage.appendChild(controls)

  const hint = document.createElement('div')
  hint.className = 'graph-zoom-hint'
  hint.textContent = tr('按 Ctrl / Cmd / Shift + 滚轮缩放', 'Use Ctrl / Cmd / Shift + scroll to zoom')
  stage.appendChild(hint)

  const svg = d3
    .select(stage)
    .append('svg')
    .attr('class', 'graph-canvas')
    .attr('viewBox', `0 0 ${width} ${height}`)

  const scene = svg.append('g').attr('class', 'graph-scene')
  const edgeLayer = scene.append('g')
  const haloLayer = scene.append('g')
  const nodeLayer = scene.append('g')
  const textLayer = scene.append('g')
  const defs = svg.append('defs')
  const markerBase = `graph-arrow-${currentFrame.frameId}`
  let graphTheme = graphThemeColors()
  const baseMarkerPath = defs
    .append('marker')
    .attr('id', `${markerBase}-base`)
    .attr('viewBox', '0 0 10 10')
    .attr('refX', 9)
    .attr('refY', 5)
    .attr('markerWidth', 7)
    .attr('markerHeight', 7)
    .attr('orient', 'auto')
    .append('path')
    .attr('d', 'M 0 0 L 10 5 L 0 10 z')
    .attr('fill', graphTheme.edgeStroke)
  const activeMarkerPath = defs
    .append('marker')
    .attr('id', `${markerBase}-active`)
    .attr('viewBox', '0 0 10 10')
    .attr('refX', 9)
    .attr('refY', 5)
    .attr('markerWidth', 7)
    .attr('markerHeight', 7)
    .attr('orient', 'auto')
    .append('path')
    .attr('d', 'M 0 0 L 10 5 L 0 10 z')
    .attr('fill', graphTheme.accent)

  const simNodes: GraphNodeSim[] = nodes.map((node) => ({ ...node }))
  const simEdges: GraphEdgeSim[] = edges.map((edge) => ({ ...edge }))
  const nodeOnlyLayout = simEdges.length === 0
  const edgeRouteMeta = buildEdgeRouteMeta(simEdges)
  const pinnedNodeIds = new Set<number>(currentFrame.graphPinnedNodeIds)
  let currentTransform = currentFrame.graphViewport
    ? d3.zoomIdentity.translate(currentFrame.graphViewport.x, currentFrame.graphViewport.y).scale(currentFrame.graphViewport.k)
    : d3.zoomIdentity
  let lastPanSample: { x: number; y: number; at: number } | null = null
  let panVelocity = { x: 0, y: 0 }
  let edgeGeometry = new Map<number, ReturnType<typeof computeEdgeGeometry>>()
  seedGraphLayout(simNodes, width, height, nodeOnlyLayout)
  restoreGraphNodePositions(currentFrame, simNodes)
  const hasSavedLayout = Object.keys(currentFrame.graphNodePositions || {}).length > 0
  const applyPinnedLayout = (): void => {
    pinnedNodeIds.clear()
    currentFrame.graphPinnedNodeIds.forEach((item) => pinnedNodeIds.add(item))
    for (const node of simNodes) {
      if (pinnedNodeIds.has(node.id)) {
        node.fx = Number.isFinite(node.x) ? Number(node.x) : width / 2
        node.fy = Number.isFinite(node.y) ? Number(node.y) : height / 2
        continue
      }
      node.fx = null
      node.fy = null
    }
  }
  applyPinnedLayout()

  const simulation = d3
    .forceSimulation<GraphNodeSim>(simNodes)
    .force('charge', d3.forceManyBody().strength(graphChargeStrength(simNodes.length, nodeOnlyLayout)))
    .force('center', d3.forceCenter(width / 2, height / 2))
    .force('collision', d3.forceCollide<GraphNodeSim>().radius((item) => nodeRadius(item) + 18))

  if (simEdges.length > 0) {
    simulation.force(
      'link',
      d3
        .forceLink<GraphNodeSim, GraphEdgeSim>(simEdges)
        .id((item) => item.id)
        .distance(Math.max(112, 76 + Math.min(56, simNodes.length)))
        .strength(0.52)
    )
  }
  const warmupTicks = hasSavedLayout
    ? 0
    : nodeOnlyLayout
      ? Math.min(180, 54 + simNodes.length * 3)
      : Math.min(120, 36 + simEdges.length)
  for (let i = 0; i < warmupTicks; i++) {
    simulation.tick()
  }
  simulation.stop()

  const links = edgeLayer
    .selectAll('path')
    .data(simEdges)
    .enter()
    .append('path')
    .attr('stroke', graphTheme.edgeStroke)
    .attr('stroke-width', 1.4)
    .attr('stroke-linecap', 'round')
    .attr('fill', 'none')
    .attr('marker-end', `url(#${markerBase}-base)`)
    .attr('opacity', 0.9)

  let focus = graphHighlightState(currentFrame, currentGraph)
  let hoveredNodeId = -1
  let hoveredEdgeId = -1

  const rings = haloLayer
    .selectAll('circle')
    .data(simNodes)
    .enter()
    .append('circle')
    .attr('r', (item) => nodeRadius(item) + 10)
    .attr('class', 'graph-ring')
    .attr('pointer-events', 'none')

  const nodesSel = nodeLayer
    .selectAll('circle')
    .data(simNodes)
    .enter()
    .append('circle')
    .attr('r', (item) => nodeRadius(item))
    .attr('fill', (item) => nodeColor(item.kind, graphTheme))
    .attr('stroke', (item) => (focus.nodeIds.has(item.id) ? graphTheme.selected : graphTheme.nodeStroke))
    .attr('stroke-width', (item) => (focus.nodeIds.has(item.id) ? 2.4 : 1.1))
    .attr('stroke-dasharray', (item) => (pinnedNodeIds.has(item.id) ? '4 2' : null))
    .attr('opacity', (item) => (focus.active && !focus.nodeIds.has(item.id) && !focus.edgeNodeIds.has(item.id) ? 0.42 : 1))
    .style('cursor', 'pointer')
    .on('mouseenter', (_, item) => {
      hoveredNodeId = item.id
      stage.classList.add('graph-hovering')
      updateGraphVisualState()
    })
    .on('mouseleave', () => {
      hoveredNodeId = -1
      stage.classList.remove('graph-hovering')
      updateGraphVisualState()
    })
    .on('click', (_, item) => {
      freezeLayout()
      currentFrame.selection = { type: 'node', data: item }
      currentFrame.graphFocus = { nodeIds: [item.id], edgeIds: [] }
      requestFrameRefresh(false)
    })
    .on('dblclick', (_, item) => {
      freezeLayout()
      togglePinnedNode(currentFrame, item.id)
      requestFrameRefresh(false)
    })
    .call(drag(simulation, pinnedNodeIds, () => persistGraphNodePositions(currentFrame, simNodes)))

  nodesSel.append('title').text((item) => item.label)

  const labels = textLayer
    .selectAll('text')
    .data(simNodes)
    .enter()
    .append('text')
    .text((item) => graphNodeDisplayLabel(item, false))
    .attr('font-size', 11)
    .attr('font-weight', 600)
    .attr('fill', graphTheme.text)
    .attr('stroke', graphTheme.textOutline)
    .attr('stroke-width', 4)
    .attr('paint-order', 'stroke')
    .attr('text-anchor', 'middle')
    .attr('pointer-events', 'none')

  const edgeLabels = textLayer
    .selectAll('text.edge')
    .data(simEdges)
    .enter()
    .append('text')
    .text((item) => truncateTextByWidth(edgeDisplayLabel(item), 120, '700 9px "SF Mono", "JetBrains Mono", "Consolas", monospace'))
    .attr('font-size', 9)
    .attr('font-weight', 700)
    .attr('fill', graphTheme.edgeText)
    .attr('stroke', graphTheme.textOutline)
    .attr('stroke-width', 4)
    .attr('paint-order', 'stroke')
    .attr('text-anchor', 'middle')
    .attr('dominant-baseline', 'central')
    .attr('pointer-events', 'none')

  links
    .on('mouseenter', (_, item) => {
      hoveredEdgeId = item.id
      stage.classList.add('graph-hovering')
      updateGraphVisualState()
    })
    .on('mouseleave', () => {
      hoveredEdgeId = -1
      stage.classList.remove('graph-hovering')
      updateGraphVisualState()
    })
  links.on('click', (_, item) => {
    freezeLayout()
    currentFrame.selection = {
      type: 'edge',
      data: {
        id: item.id,
        source: Number((item.source as GraphNodeSim).id ?? item.source),
        target: Number((item.target as GraphNodeSim).id ?? item.target),
        relType: item.relType,
        confidence: item.confidence,
        evidence: item.evidence,
        properties: item.properties || {}
      }
    }
    currentFrame.graphFocus = {
      nodeIds: [Number((item.source as GraphNodeSim).id ?? item.source), Number((item.target as GraphNodeSim).id ?? item.target)],
      edgeIds: [item.id]
    }
    requestFrameRefresh(false)
  })

  const zoom = d3
    .zoom<SVGSVGElement, unknown>()
    .scaleExtent([0.42, 3.2])
    .filter((event) => {
      if (event.type === 'wheel') {
        return !!(event.ctrlKey || event.metaKey || event.shiftKey)
      }
      return event.type !== 'dblclick'
    })
    .on('start', () => {
      stage.classList.add('graph-interacting')
      lastPanSample = null
      panVelocity = { x: 0, y: 0 }
    })
    .on('end', (event) => {
      stage.classList.remove('graph-interacting')
      if (Math.abs(panVelocity.x) > 0.18 || Math.abs(panVelocity.y) > 0.18) {
        const momentumX = clampFloat(panVelocity.x * 180, -140, 140)
        const momentumY = clampFloat(panVelocity.y * 180, -120, 120)
        currentTransform = d3.zoomIdentity
          .translate(currentTransform.x + momentumX, currentTransform.y + momentumY)
          .scale(currentTransform.k)
        svg
          .transition()
          .duration(220)
          .ease(d3.easeCubicOut)
          .call(zoom.transform, currentTransform)
      }
      lastPanSample = null
      panVelocity = { x: 0, y: 0 }
    })
    .on('zoom', (event) => {
      const transform = event.transform
      currentTransform = transform
      const sourceEvent = event.sourceEvent as MouseEvent | WheelEvent | undefined
      if (sourceEvent?.type === 'mousemove') {
        const now = performance.now()
        if (lastPanSample) {
          const dt = Math.max(1, now - lastPanSample.at)
          panVelocity = {
            x: (transform.x - lastPanSample.x) / dt,
            y: (transform.y - lastPanSample.y) / dt
          }
      }
      lastPanSample = { x: transform.x, y: transform.y, at: now }
    }
    scene.attr('transform', transform.toString())
    currentFrame.graphViewport = { x: transform.x, y: transform.y, k: transform.k }
    zoomReadout.textContent = `${Math.round(transform.k * 100)}%`
    hint.classList.toggle('hidden', transform.k !== 1)
    updateGraphVisualState()
  })

  svg.call(zoom)
  svg.on('dblclick.zoom', null)

  const fitGraph = (animate: boolean): void => {
    const transform = fitGraphTransform(simNodes, width, height)
    if (animate) {
      svg.transition().duration(180).call(zoom.transform, transform)
      return
    }
    svg.call(zoom.transform, transform)
  }

  const freezeLayout = (): void => {
    simulation.stop()
    persistGraphNodePositions(currentFrame, simNodes)
  }

  zoomInButton.addEventListener('click', () => {
    hint.classList.add('hidden')
    svg.transition().duration(140).call(zoom.scaleBy, 1.18)
  })
  zoomOutButton.addEventListener('click', () => {
    hint.classList.add('hidden')
    svg.transition().duration(140).call(zoom.scaleBy, 1 / 1.18)
  })
  fitButton.addEventListener('click', () => {
    hint.classList.add('hidden')
    fitGraph(true)
  })

  const applyGraphGeometry = (): void => {
    persistGraphNodePositions(currentFrame, simNodes)
    edgeGeometry = new Map<number, ReturnType<typeof computeEdgeGeometry>>()
    for (const edge of simEdges) {
      edgeGeometry.set(edge.id, computeEdgeGeometry(edge, edgeRouteMeta.get(edge.id) || { lane: 0, isLoop: false, loopIndex: 0 }))
    }
    links
      .attr('d', (item) => edgeGeometry.get(item.id)?.path || '')
      .attr('stroke', (item) => (focus.edgeIds.has(item.id) ? graphTheme.accent : graphTheme.edgeStroke))
      .attr('stroke-width', (item) => (focus.edgeIds.has(item.id) ? 2.8 : 1.4))
      .attr('opacity', (item) => (focus.active && !focus.edgeIds.has(item.id) ? 0.32 : 0.9))
      .attr('marker-end', (item) => {
        return focus.edgeIds.has(item.id) || hoveredEdgeId === item.id
          ? `url(#${markerBase}-active)`
          : `url(#${markerBase}-base)`
      })

    rings.attr('cx', (item) => item.x ?? 0).attr('cy', (item) => item.y ?? 0)

    nodesSel.attr('cx', (item) => item.x ?? 0).attr('cy', (item) => item.y ?? 0)

    labels.attr('x', (item) => item.x ?? 0).attr('y', (item) => (item.y ?? 0) - nodeRadius(item) - 11)

    edgeLabels
      .attr('x', (item) => edgeGeometry.get(item.id)?.labelX ?? 0)
      .attr('y', (item) => edgeGeometry.get(item.id)?.labelY ?? 0)
      .attr('transform', (item) => {
        const geometry = edgeGeometry.get(item.id)
        if (!geometry) {
          return ''
        }
        return `rotate(${geometry.labelAngle} ${geometry.labelX} ${geometry.labelY})`
      })

    updateGraphVisualState()
  }

  simulation.on('tick', applyGraphGeometry)

  if (currentFrame.graphViewport) {
    svg.call(
      zoom.transform,
      d3.zoomIdentity.translate(currentFrame.graphViewport.x, currentFrame.graphViewport.y).scale(currentFrame.graphViewport.k)
    )
  } else {
    fitGraph(false)
  }
  applyGraphGeometry()

  const nudgeSimulation = (alpha: number): void => {
    if (!visible) {
      return
    }
    simulation.alpha(Math.max(simulation.alpha(), alpha)).restart()
  }

  const resizeStage = (refit: boolean): void => {
    const nextSize = measureStage()
    if (nextSize.width === width && nextSize.height === height && !refit) {
      return
    }
    width = nextSize.width
    height = nextSize.height
    svg.attr('viewBox', `0 0 ${width} ${height}`)
    simulation.force('center', d3.forceCenter(width / 2, height / 2))
    applyPinnedLayout()
    if (refit || !currentFrame.graphViewport) {
      fitGraph(false)
    } else {
      scene.attr('transform', currentTransform.toString())
      zoomReadout.textContent = `${Math.round(currentTransform.k * 100)}%`
      hint.classList.toggle('hidden', currentTransform.k !== 1)
    }
    applyGraphGeometry()
    if (visible) {
      nudgeSimulation(hasSavedLayout ? 0.08 : 0.2)
    }
  }

  const scheduleResizeStage = (refit: boolean): void => {
    pendingResizeRefit = pendingResizeRefit || refit
    if (resizeScheduled) {
      return
    }
    resizeScheduled = true
    requestAnimationFrame(() => {
      resizeScheduled = false
      const nextRefit = pendingResizeRefit
      pendingResizeRefit = false
      resizeStage(nextRefit)
    })
  }

  scheduleResizeStage(true)

  function updateGraphVisualState(): void {
    graphTheme = graphThemeColors()
    baseMarkerPath.attr('fill', graphTheme.edgeStroke)
    activeMarkerPath.attr('fill', graphTheme.accent)
    const visibleNodeLabelIds = computeVisibleNodeLabelIds(simNodes, focus, hoveredNodeId, currentTransform.k)
    const visibleEdgeLabelIds = computeVisibleEdgeLabelIds(
      simEdges,
      edgeGeometry,
      focus,
      hoveredEdgeId,
      currentTransform.k
    )
    links
      .attr('stroke', (item) => (focus.edgeIds.has(item.id) || hoveredEdgeId === item.id ? graphTheme.accent : graphTheme.edgeStroke))
      .attr('stroke-width', (item) => (focus.edgeIds.has(item.id) || hoveredEdgeId === item.id ? 2.8 : 1.4))
      .attr('opacity', (item) => {
        if (!focus.active && hoveredEdgeId < 0 && hoveredNodeId < 0) {
          return 0.84
        }
        return focus.edgeIds.has(item.id) || hoveredEdgeId === item.id ? 1 : 0.26
      })
    rings
      .attr('stroke', (item) => (focus.nodeIds.has(item.id) ? graphTheme.selected : graphTheme.ring))
      .attr('opacity', (item) => (focus.nodeIds.has(item.id) ? 0.34 : hoveredNodeId === item.id ? 0.26 : 0))
    nodesSel
      .attr('fill', (item) => nodeColor(item.kind, graphTheme))
      .attr('stroke', (item) => {
        if (focus.nodeIds.has(item.id)) {
          return graphTheme.selected
        }
        if (hoveredNodeId === item.id) {
          return graphTheme.accent
        }
        return graphTheme.nodeStroke
      })
      .attr('stroke-width', (item) => (focus.nodeIds.has(item.id) ? 2.6 : hoveredNodeId === item.id ? 2 : 1.1))
      .attr('stroke-dasharray', (item) => (pinnedNodeIds.has(item.id) ? '4 2' : null))
      .attr('opacity', (item) => {
        if (!focus.active && hoveredNodeId < 0 && hoveredEdgeId < 0) {
          return 0.98
        }
        if (focus.nodeIds.has(item.id) || focus.edgeNodeIds.has(item.id) || hoveredNodeId === item.id) {
          return 1
        }
        return 0.4
      })
    labels
      .attr('fill', graphTheme.text)
      .attr('stroke', graphTheme.textOutline)
      .text((item) => graphNodeDisplayLabel(item, focus.nodeIds.has(item.id) || hoveredNodeId === item.id))
      .attr('opacity', (item) => {
        if (
          focus.nodeIds.has(item.id) ||
          focus.edgeNodeIds.has(item.id) ||
          hoveredNodeId === item.id ||
          visibleNodeLabelIds.has(item.id)
        ) {
          return 1
        }
        return 0
      })
    edgeLabels
      .attr('fill', graphTheme.edgeText)
      .attr('stroke', graphTheme.textOutline)
      .text((item) => truncateTextByWidth(edgeDisplayLabel(item), 120, '700 9px "SF Mono", "JetBrains Mono", "Consolas", monospace'))
      .attr('opacity', (item) => {
        if (focus.edgeIds.has(item.id) || hoveredEdgeId === item.id) {
          return 0.96
        }
        return visibleEdgeLabelIds.has(item.id) ? 0.7 : 0
      })
  }

  return {
    sync: (frame, graph, restartSimulation = false) => {
      currentFrame = frame
      currentGraph = graph
      focus = graphHighlightState(currentFrame, currentGraph)
      applyPinnedLayout()
      if (currentFrame.graphViewport && !sameGraphViewport(currentFrame.graphViewport, currentTransform)) {
        svg.call(
          zoom.transform,
          d3.zoomIdentity.translate(currentFrame.graphViewport.x, currentFrame.graphViewport.y).scale(currentFrame.graphViewport.k)
        )
      } else {
        updateGraphVisualState()
      }
      applyGraphGeometry()
      if (restartSimulation) {
        nudgeSimulation(hasSavedLayout ? 0.12 : 0.3)
      }
    },
    setVisible: (nextVisible) => {
      if (visible === nextVisible) {
        return
      }
      visible = nextVisible
      if (!visible) {
        simulation.stop()
        return
      }
      scheduleResizeStage(!currentFrame.graphViewport)
      nudgeSimulation(hasSavedLayout ? 0.08 : 0.22)
    },
    requestLayoutSync: () => {
      scheduleResizeStage(!currentFrame.graphViewport)
    },
    destroy: () => {
      simulation.stop()
      container.innerHTML = ''
    }
  }
}

function sameGraphViewport(viewport: GraphViewport, transform: d3.ZoomTransform): boolean {
  return Math.abs(viewport.x - transform.x) < 0.5
    && Math.abs(viewport.y - transform.y) < 0.5
    && Math.abs(viewport.k - transform.k) < 0.01
}

function mountVirtualTable(container: HTMLElement, initialFrame: FrameState): TableViewController {
  let currentFrame = initialFrame
  let visible = true
  let columnsKey = ''
  let lastFocusKey = ''
  let resizeScheduled = false
  let lastViewportHeight = -1

  container.innerHTML = ''
  const header = document.createElement('div')
  header.className = 'vt-header'
  const scroll = document.createElement('div')
  scroll.className = 'vt-scroll'
  const space = document.createElement('div')
  space.className = 'vt-space'
  const win = document.createElement('div')
  win.className = 'vt-window'
  space.appendChild(win)
  scroll.appendChild(space)
  container.append(header, scroll)

  const currentColumns = (): string[] => {
    return currentFrame.columns.length > 0 ? currentFrame.columns : ['value']
  }

  const currentTemplate = (): string => {
    return currentColumns().map(() => 'minmax(120px, 1fr)').join(' ')
  }

  const focusKey = (focus: FrameState['tableFocus']): string => {
    return focus ? `${focus.rowIndex}:${focus.colIndex}` : ''
  }

  const renderHeader = (): void => {
    const safeColumns = currentColumns()
    const nextKey = safeColumns.join('\u0001')
    header.style.gridTemplateColumns = currentTemplate()
    if (nextKey === columnsKey) {
      return
    }
    columnsKey = nextKey
    header.innerHTML = ''
    for (const col of safeColumns) {
      const cell = document.createElement('div')
      cell.className = 'vt-cell'
      cell.textContent = col
      header.appendChild(cell)
    }
  }

  const renderWindow = (): void => {
    const focus = currentFrame.tableFocus
    const rows = currentFrame.rows
    const safeColumns = currentColumns()
    const template = currentTemplate()
    const viewportHeight = Math.max(240, scroll.clientHeight || 240)
    const start = Math.max(0, Math.floor(scroll.scrollTop / TABLE_ROW_HEIGHT) - 2)
    const count = Math.ceil(viewportHeight / TABLE_ROW_HEIGHT) + 6
    const end = Math.min(rows.length, start + count)
    space.style.height = `${rows.length * TABLE_ROW_HEIGHT}px`
    win.style.transform = `translateY(${start * TABLE_ROW_HEIGHT}px)`
    win.innerHTML = ''

    for (let rowIndex = start; rowIndex < end; rowIndex++) {
      const row = rows[rowIndex] || []
      const rowEl = document.createElement('div')
      rowEl.className = `vt-row${focus && focus.rowIndex === rowIndex ? ' focus' : ''}`
      rowEl.style.gridTemplateColumns = template
      rowEl.style.height = `${TABLE_ROW_HEIGHT}px`
      rowEl.addEventListener('click', () => {
        activateTableRow(currentFrame, rowIndex, row)
      })
      rowEl.addEventListener('dblclick', () => {
        activateTableRow(currentFrame, rowIndex, row, true)
      })
      for (let columnIndex = 0; columnIndex < safeColumns.length; columnIndex++) {
        const cell = document.createElement('div')
        cell.className = `vt-cell${focus && focus.rowIndex === rowIndex && focus.colIndex === columnIndex ? ' focus' : ''}`
        cell.textContent = toCell(row[columnIndex])
        rowEl.appendChild(cell)
      }
      win.appendChild(rowEl)
    }
  }

  const alignFocus = (): void => {
    const focus = currentFrame.tableFocus
    const nextFocusKey = focusKey(focus)
    if (!focus || focus.rowIndex < 0 || nextFocusKey === lastFocusKey) {
      lastFocusKey = nextFocusKey
      return
    }
    lastFocusKey = nextFocusKey
    const target = Math.max(0, focus.rowIndex * TABLE_ROW_HEIGHT - Math.max(80, (scroll.clientHeight || 240) / 3))
    requestAnimationFrame(() => {
      scroll.scrollTop = target
      renderWindow()
    })
  }

  const render = (): void => {
    renderHeader()
    renderWindow()
    alignFocus()
  }

  const onScroll = (): void => {
    renderWindow()
  }

  const scheduleRenderWindow = (force = false): void => {
    if (resizeScheduled) {
      return
    }
    resizeScheduled = true
    requestAnimationFrame(() => {
      resizeScheduled = false
      if (!visible) {
        return
      }
      const nextViewportHeight = Math.max(0, scroll.clientHeight || 0)
      if (!force && nextViewportHeight === lastViewportHeight) {
        return
      }
      lastViewportHeight = nextViewportHeight
      renderWindow()
    })
  }

  scroll.addEventListener('scroll', onScroll)
  render()

  return {
    sync: (frame) => {
      currentFrame = frame
      render()
    },
    setVisible: (nextVisible) => {
      visible = nextVisible
      if (visible) {
        scheduleRenderWindow(true)
      }
    },
    requestLayoutSync: () => {
      scheduleRenderWindow(true)
    },
    destroy: () => {
      scroll.removeEventListener('scroll', onScroll)
      container.innerHTML = ''
    }
  }
}

function renderTextView(container: HTMLElement, frame: FrameState): void {
  container.innerHTML = ''
  const raw = frame.text || JSON.stringify({ columns: frame.columns, rows: frame.rows, warnings: frame.warnings }, null, 2)

  const sum = document.createElement('details')
  sum.className = 'text-row'
  sum.open = true
  const sumTitle = document.createElement('summary')
  sumTitle.textContent = tr('汇总文本', 'Summary Text')
  const sumPre = document.createElement('pre')
  sumPre.textContent = raw
  sum.append(sumTitle, sumPre)
  container.appendChild(sum)

  frame.rows.slice(0, 200).forEach((row, index) => {
    const details = document.createElement('details')
    details.className = 'text-row'
    const title = document.createElement('summary')
    title.textContent = `${tr('行', 'Row')} #${index + 1}`
    const pre = document.createElement('pre')
    pre.textContent = JSON.stringify(row, null, 2)
    details.append(title, pre)
    container.appendChild(details)
  })
}

function frameTextRenderKey(frame: FrameState): string {
  return [
    frame.frameId,
    frame.view,
    frame.text.length,
    frame.columns.join('\u0001'),
    frame.rows.length,
    frame.warnings.join('\u0001'),
    frame.errorCode || '',
    frame.errorMessage || ''
  ].join('::')
}

function frameInspectorRenderKey(frame: FrameState, graph: GraphFramePayload | null): string {
  return [
    frame.frameId,
    frame.query,
    frame.elapsedMs,
    frame.columns.length,
    frame.rows.length,
    frame.truncated ? '1' : '0',
    graph?.nodes.length || 0,
    graph?.edges.length || 0,
    frame.graphFilter ? JSON.stringify(frame.graphFilter) : '',
    frameSelectionKey(frame.selection),
    frame.warnings.join('\u0001'),
    graph?.warnings.join('\u0001') || ''
  ].join('::')
}

function frameSelectionKey(selection: FrameSelection): string {
  if (!selection) {
    return ''
  }
  if (selection.type === 'node') {
    return `node:${selection.data.id}`
  }
  return `edge:${selection.data.id}:${selection.data.source}:${selection.data.target}`
}

function applyGraphFilter(
  graph: GraphFramePayload,
  filter: GraphFilter
): GraphFramePayload {
  if (!filter) {
    return graph
  }
  if (filter.kind === 'neighbors') {
    const nodeIds = new Set<number>([filter.nodeId])
    const edges = graph.edges.filter((edge) => {
      const include = edge.source === filter.nodeId || edge.target === filter.nodeId
      if (include) {
        nodeIds.add(edge.source)
        nodeIds.add(edge.target)
      }
      return include
    })
    return {
      ...graph,
      nodes: graph.nodes.filter((node) => nodeIds.has(node.id)),
      edges
    }
  }
  if (filter.kind === 'label') {
    const matchedIds = new Set(
      graph.nodes.filter((node) => node.labels.includes(filter.value) || node.kind === filter.value).map((node) => node.id)
    )
    const nodes = graph.nodes.filter((node) => matchedIds.has(node.id))
    const edges = graph.edges.filter((edge) => matchedIds.has(edge.source) || matchedIds.has(edge.target))
    const connectedNodeIds = new Set<number>(nodes.map((node) => node.id))
    edges.forEach((edge) => {
      connectedNodeIds.add(edge.source)
      connectedNodeIds.add(edge.target)
    })
    return {
      ...graph,
      nodes: graph.nodes.filter((node) => connectedNodeIds.has(node.id)),
      edges
    }
  }
  const edges = graph.edges.filter((edge) => edgeDisplayGroup(edge) === filter.value)
  const nodeIds = new Set<number>()
  edges.forEach((edge) => {
    nodeIds.add(edge.source)
    nodeIds.add(edge.target)
  })
  return {
    ...graph,
    nodes: graph.nodes.filter((node) => nodeIds.has(node.id)),
    edges
  }
}

function graphHighlightState(frame: FrameState, graph: GraphFramePayload | null = frame.graph): {
  active: boolean
  nodeIds: Set<number>
  edgeIds: Set<number>
  edgeNodeIds: Set<number>
} {
  const nodeIds = new Set<number>()
  const edgeIds = new Set<number>()
  if (frame.selection?.type === 'node') {
    nodeIds.add(frame.selection.data.id)
  }
  if (frame.selection?.type === 'edge') {
    edgeIds.add(frame.selection.data.id)
    nodeIds.add(frame.selection.data.source)
    nodeIds.add(frame.selection.data.target)
  }
  if (frame.graphFocus) {
    frame.graphFocus.nodeIds.forEach((item) => nodeIds.add(item))
    frame.graphFocus.edgeIds.forEach((item) => edgeIds.add(item))
  }
  const edgeNodeIds = new Set<number>()
  if (graph) {
    for (const edge of graph.edges) {
      if (edgeIds.has(edge.id)) {
        edgeNodeIds.add(edge.source)
        edgeNodeIds.add(edge.target)
      }
    }
  }
  return {
    active: nodeIds.size > 0 || edgeIds.size > 0,
    nodeIds,
    edgeIds,
    edgeNodeIds
  }
}

function activateInspectorLink(
  frame: FrameState,
  key: string,
  value: unknown,
  selected: Exclude<FrameSelection, null>
): void {
  const focus = findTableFocus(frame, key, value, selected)
  if (focus) {
    frame.tableFocus = focus
    frame.view = 'table'
  }
  frame.graphFocus =
    selected.type === 'node'
      ? { nodeIds: [selected.data.id], edgeIds: [] }
      : {
          nodeIds: [selected.data.source, selected.data.target],
          edgeIds: [selected.data.id]
        }
  const fragment = buildInspectorFragment(key, value)
  if (fragment) {
    insertQueryFragment(fragment)
  }
  refreshFrame(frame, false)
}

function activateLegend(frame: FrameState, kind: 'label' | 'relGroup', value: string): void {
  const nextValue = String(value || '').trim()
  if (!nextValue) {
    return
  }
  if (frame.graphFilter && frame.graphFilter.kind === kind && frame.graphFilter.value === nextValue) {
    frame.graphFilter = null
  } else {
    frame.graphFilter = { kind, value: nextValue }
  }
  frame.view = 'graph'
  const fragment = buildInspectorFragment(kind === 'label' ? 'label' : 'display_rel_type', nextValue)
  if (fragment) {
    insertQueryFragment(fragment)
  }
  refreshFrame(frame, false)
}

function activateNeighborhoodFocus(frame: FrameState, node: GraphNodePayload): void {
  const safeLabel = String(node.label || `${node.kind}:${node.id}`).trim()
  if (frame.graphFilter?.kind === 'neighbors' && frame.graphFilter.nodeId === node.id) {
    frame.graphFilter = null
    frame.graphFocus = { nodeIds: [node.id], edgeIds: [] }
    frame.view = 'graph'
    refreshFrame(frame, false)
    return
  }
  const nextNodeIds = new Set<number>([node.id])
  const nextEdgeIds = new Set<number>()
  for (const edge of frame.graph?.edges || []) {
    if (edge.source === node.id || edge.target === node.id) {
      nextEdgeIds.add(edge.id)
      nextNodeIds.add(edge.source)
      nextNodeIds.add(edge.target)
    }
  }
  frame.graphFilter = {
    kind: 'neighbors',
    nodeId: node.id,
    label: safeLabel,
    hops: 1
  }
  frame.graphFocus = {
    nodeIds: Array.from(nextNodeIds),
    edgeIds: Array.from(nextEdgeIds)
  }
  frame.view = 'graph'
  refreshFrame(frame, false)
}

function activateTableRow(frame: FrameState, rowIndex: number, row: unknown[], switchToGraph = false): void {
  const refs = extractRowGraphRefs(row)
  frame.tableFocus = {
    rowIndex,
    colIndex: findGraphCellIndex(row)
  }
  frame.graphFocus = {
    nodeIds: refs.nodeIds,
    edgeIds: refs.edgeIds
  }
  const selection = resolveRowSelection(frame.graph, refs)
  if (selection) {
    frame.selection = selection
  }
  if (switchToGraph && frame.graph && refs.nodeIds.length > 0) {
    frame.view = 'graph'
  }
  refreshFrame(frame, false)
}

function findTableFocus(
  frame: FrameState,
  key: string,
  value: unknown,
  selected: Exclude<FrameSelection, null>
): { rowIndex: number; colIndex: number } | null {
  let bestScore = -1
  let bestRow = -1
  let bestCol = -1
  for (let rowIndex = 0; rowIndex < frame.rows.length; rowIndex++) {
    const row = frame.rows[rowIndex] || []
    for (let colIndex = 0; colIndex < row.length; colIndex++) {
      const score = scoreCellForFocus(row[colIndex], key, value, selected, 0)
      if (score > bestScore) {
        bestScore = score
        bestRow = rowIndex
        bestCol = colIndex
      }
    }
  }
  if (bestScore < 0 || bestRow < 0 || bestCol < 0) {
    return null
  }
  return {
    rowIndex: bestRow,
    colIndex: bestCol
  }
}

function extractRowGraphRefs(row: unknown[]): { nodeIds: number[]; edgeIds: number[] } {
  const nodeIds = new Set<number>()
  const edgeIds = new Set<number>()
  collectGraphRefs(row, nodeIds, edgeIds)
  return {
    nodeIds: Array.from(nodeIds),
    edgeIds: Array.from(edgeIds)
  }
}

function findGraphCellIndex(row: unknown[]): number {
  for (let index = 0; index < row.length; index++) {
    const refs = extractRowGraphRefs([row[index]])
    if (refs.nodeIds.length > 0 || refs.edgeIds.length > 0) {
      return index
    }
  }
  return 0
}

function collectGraphRefs(value: unknown, nodeIds: Set<number>, edgeIds: Set<number>, depth = 0): void {
  if (value == null || depth > 6) {
    return
  }
  if (Array.isArray(value)) {
    value.forEach((item) => collectGraphRefs(item, nodeIds, edgeIds, depth + 1))
    return
  }
  if (!isRecord(value)) {
    return
  }
  const nodeId = firstNumericField(value, 'node_id', 'nodeId')
  if (nodeId > 0 && (Array.isArray(value.labels) || isRecord(value.properties))) {
    nodeIds.add(nodeId)
  }
  const startNodeId = firstNumericField(value, 'startNodeId', 'start_node_id', 'source', 'src_id')
  const endNodeId = firstNumericField(value, 'endNodeId', 'end_node_id', 'target', 'dst_id')
  if (startNodeId > 0 && endNodeId > 0) {
    nodeIds.add(startNodeId)
    nodeIds.add(endNodeId)
    const edgeId = firstNumericField(value, 'edge_id', 'edgeId', 'id')
    if (edgeId > 0) {
      edgeIds.add(edgeId)
    }
  }
  Object.values(value).forEach((item) => collectGraphRefs(item, nodeIds, edgeIds, depth + 1))
}

function resolveRowSelection(
  graph: GraphFramePayload | null,
  refs: { nodeIds: number[]; edgeIds: number[] }
): Exclude<FrameSelection, null> | null {
  if (!graph) {
    return null
  }
  if (refs.edgeIds.length === 1) {
    const edge = graph.edges.find((item) => item.id === refs.edgeIds[0])
    if (edge) {
      return { type: 'edge', data: edge }
    }
  }
  if (refs.nodeIds.length === 1) {
    const node = graph.nodes.find((item) => item.id === refs.nodeIds[0])
    if (node) {
      return { type: 'node', data: node }
    }
  }
  return null
}

function scoreCellForFocus(
  cell: unknown,
  key: string,
  value: unknown,
  selected: Exclude<FrameSelection, null>,
  depth: number
): number {
  if (depth > 6 || cell == null) {
    return -1
  }
  let score = -1
  if (isRecord(cell)) {
    if (selected.type === 'node') {
      const nodeId = firstNumericField(cell, 'node_id', 'nodeId', 'id')
      if (nodeId === selected.data.id && (Array.isArray(cell.labels) || isRecord(cell.properties))) {
        score = Math.max(score, 100 - depth)
      }
      if (key === 'label' && Array.isArray(cell.labels) && cell.labels.some((item) => String(item) === String(value))) {
        score = Math.max(score, 96 - depth)
      }
    } else {
      const edgeId = firstNumericField(cell, 'edge_id', 'edgeId', 'id')
      if (
        edgeId === selected.data.id &&
        firstNumericField(cell, 'startNodeId', 'start_node_id', 'source', 'src_id') >= 0 &&
        firstNumericField(cell, 'endNodeId', 'end_node_id', 'target', 'dst_id') >= 0
      ) {
        score = Math.max(score, 100 - depth)
      }
      const relType = readField(cell, 'type', 'rel_type')
      if (key === 'rel_type' && relType != null && String(relType) === String(value)) {
        score = Math.max(score, 96 - depth)
      }
    }
    const direct = readField(cell, key)
    if (direct !== undefined && valueMatches(direct, value)) {
      score = Math.max(score, 88 - depth)
    }
    const props = isRecord(cell.properties) ? cell.properties : null
    if (props) {
      const propValue = readField(props, key)
      if (propValue !== undefined && valueMatches(propValue, value)) {
        score = Math.max(score, 92 - depth)
      }
    }
    for (const nested of Object.values(cell)) {
      score = Math.max(score, scoreCellForFocus(nested, key, value, selected, depth + 1))
    }
    return score
  }
  if (Array.isArray(cell)) {
    for (const item of cell) {
      score = Math.max(score, scoreCellForFocus(item, key, value, selected, depth + 1))
    }
    return score
  }
  if (valueMatches(cell, value)) {
    return 72 - depth
  }
  const needle = toCell(value)
  const haystack = toCell(cell)
  if (needle && haystack && haystack.includes(needle)) {
    return 18 - depth
  }
  return score
}

function readField(obj: Record<string, unknown>, ...keys: string[]): unknown {
  for (const key of keys) {
    if (Object.prototype.hasOwnProperty.call(obj, key)) {
      return obj[key]
    }
  }
  return undefined
}

function firstNumericField(obj: Record<string, unknown>, ...keys: string[]): number {
  for (const key of keys) {
    const value = readField(obj, key)
    const parsed = Number(value)
    if (Number.isFinite(parsed)) {
      return parsed
    }
  }
  return -1
}

function seedGraphLayout(nodes: GraphNodeSim[], width: number, height: number, nodeOnlyLayout: boolean): void {
  if (nodes.length === 0) {
    return
  }
  if (nodeOnlyLayout) {
    const cols = Math.max(3, Math.ceil(Math.sqrt(nodes.length)))
    const rows = Math.max(1, Math.ceil(nodes.length / cols))
    const gapX = width / (cols + 1)
    const gapY = height / (rows + 1)
    nodes.forEach((node, index) => {
      const col = index % cols
      const row = Math.floor(index / cols)
      node.x = gapX * (col + 1) + ((row % 2) * gapX) / 8
      node.y = gapY * (row + 1)
      node.vx = 0
      node.vy = 0
    })
    return
  }
  const radius = Math.max(90, Math.min(width, height) * 0.28)
  nodes.forEach((node, index) => {
    const angle = (index / Math.max(1, nodes.length)) * Math.PI * 2
    node.x = width / 2 + Math.cos(angle) * radius
    node.y = height / 2 + Math.sin(angle) * radius
    node.vx = 0
    node.vy = 0
  })
}

function graphChargeStrength(nodeCount: number, nodeOnlyLayout: boolean): number {
  const base = nodeOnlyLayout ? 420 : 260
  return -Math.min(920, base + nodeCount * 12)
}

function fitGraphTransform(nodes: GraphNodeSim[], width: number, height: number): d3.ZoomTransform {
  if (nodes.length === 0) {
    return d3.zoomIdentity.translate(width / 2, height / 2).scale(1)
  }
  let minX = Number.POSITIVE_INFINITY
  let minY = Number.POSITIVE_INFINITY
  let maxX = Number.NEGATIVE_INFINITY
  let maxY = Number.NEGATIVE_INFINITY
  for (const node of nodes) {
    const x = Number.isFinite(node.x) ? Number(node.x) : width / 2
    const y = Number.isFinite(node.y) ? Number(node.y) : height / 2
    minX = Math.min(minX, x - nodeRadius(node) - 24)
    minY = Math.min(minY, y - nodeRadius(node) - 24)
    maxX = Math.max(maxX, x + nodeRadius(node) + 24)
    maxY = Math.max(maxY, y + nodeRadius(node) + 24)
  }
  const boxWidth = Math.max(1, maxX - minX)
  const boxHeight = Math.max(1, maxY - minY)
  const padding = 34
  const scale = Math.max(0.45, Math.min(2.3, Math.min((width - padding * 2) / boxWidth, (height - padding * 2) / boxHeight)))
  const centerX = minX + boxWidth / 2
  const centerY = minY + boxHeight / 2
  return d3.zoomIdentity
    .translate(width / 2 - centerX * scale, height / 2 - centerY * scale)
    .scale(scale)
}

function buildEdgeRouteMeta(edges: GraphEdgeSim[]): Map<number, EdgeRouteMeta> {
  const out = new Map<number, EdgeRouteMeta>()
  const pairGroups = new Map<string, GraphEdgeSim[]>()
  for (const edge of edges) {
    const sourceId = Number(edge.source)
    const targetId = Number(edge.target)
    const pairKey = sourceId <= targetId ? `${sourceId}:${targetId}` : `${targetId}:${sourceId}`
    const bucket = pairGroups.get(pairKey) || []
    bucket.push(edge)
    pairGroups.set(pairKey, bucket)
  }
  for (const group of pairGroups.values()) {
    if (group.length === 1) {
      out.set(group[0].id, { lane: 0, isLoop: Number(group[0].source) === Number(group[0].target), loopIndex: 0 })
      continue
    }
    const sample = group[0]
    const isLoop = Number(sample.source) === Number(sample.target)
    if (isLoop) {
      group
        .sort((left, right) => left.id - right.id)
        .forEach((edge, index) => {
          out.set(edge.id, { lane: index, isLoop: true, loopIndex: index })
        })
      continue
    }
    const forwardSource = Math.min(...group.map((edge) => Number(edge.source)))
    const forwardTarget = Math.max(...group.map((edge) => Number(edge.target)))
    const forward = group
      .filter((edge) => Number(edge.source) === forwardSource && Number(edge.target) === forwardTarget)
      .sort((left, right) => left.id - right.id)
    const backward = group
      .filter((edge) => Number(edge.source) === forwardTarget && Number(edge.target) === forwardSource)
      .sort((left, right) => left.id - right.id)
    assignDirectionalLanes(out, forward, backward.length > 0, 1)
    assignDirectionalLanes(out, backward, forward.length > 0, -1)
  }
  return out
}

function assignDirectionalLanes(
  out: Map<number, EdgeRouteMeta>,
  edges: GraphEdgeSim[],
  hasOppositeDirection: boolean,
  direction: 1 | -1
): void {
  if (edges.length === 0) {
    return
  }
  const center = (edges.length - 1) / 2
  edges.forEach((edge, index) => {
    const centered = index - center
    const lane = edges.length === 1 && !hasOppositeDirection
      ? 0
      : direction * (centered + (hasOppositeDirection ? 0.6 : 0))
    out.set(edge.id, { lane, isLoop: false, loopIndex: 0 })
  })
}

function computeEdgeGeometry(
  edge: GraphEdgeSim,
  route: EdgeRouteMeta
): { path: string; labelX: number; labelY: number; labelAngle: number } {
  const source = edge.source as GraphNodeSim
  const target = edge.target as GraphNodeSim
  const sourceX = Number.isFinite(source.x) ? Number(source.x) : 0
  const sourceY = Number.isFinite(source.y) ? Number(source.y) : 0
  const targetX = Number.isFinite(target.x) ? Number(target.x) : 0
  const targetY = Number.isFinite(target.y) ? Number(target.y) : 0
  if (route.isLoop || edge.source === edge.target) {
    const radius = nodeRadius(source) + 18 + route.loopIndex * 14
    const startX = sourceX
    const startY = sourceY - radius
    const cp1X = sourceX + radius * 1.18
    const cp1Y = sourceY - radius * 1.42
    const cp2X = sourceX + radius * 1.18
    const cp2Y = sourceY + radius * 0.32
    const endX = sourceX
    const endY = sourceY + radius * 0.78
    const cp3X = sourceX - radius * 1.18
    const cp3Y = sourceY + radius * 0.32
    const cp4X = sourceX - radius * 1.18
    const cp4Y = sourceY - radius * 1.42
    return {
      path: `M ${startX} ${startY} C ${cp1X} ${cp1Y}, ${cp2X} ${cp2Y}, ${endX} ${endY} C ${cp3X} ${cp3Y}, ${cp4X} ${cp4Y}, ${startX} ${startY}`,
      labelX: sourceX,
      labelY: sourceY - radius * 1.48,
      labelAngle: 0
    }
  }
  const dx = targetX - sourceX
  const dy = targetY - sourceY
  const distance = Math.max(1, Math.hypot(dx, dy))
  const unitX = dx / distance
  const unitY = dy / distance
  const normalX = -unitY
  const normalY = unitX
  const sourceRadius = nodeRadius(source) + 3
  const targetRadius = nodeRadius(target) + 7
  const startX = sourceX + unitX * sourceRadius
  const startY = sourceY + unitY * sourceRadius
  const endX = targetX - unitX * targetRadius
  const endY = targetY - unitY * targetRadius
  const curve = route.lane * 34
  if (Math.abs(curve) < 0.001) {
    return {
      path: `M ${startX} ${startY} L ${endX} ${endY}`,
      labelX: (startX + endX) / 2 + normalX * 8,
      labelY: (startY + endY) / 2 + normalY * 8,
      labelAngle: normalizeReadableAngle(Math.atan2(dy, dx) * 180 / Math.PI)
    }
  }
  const midX = (startX + endX) / 2
  const midY = (startY + endY) / 2
  const controlX = midX + normalX * curve
  const controlY = midY + normalY * curve
  const labelPoint = quadraticPoint(startX, startY, controlX, controlY, endX, endY, 0.5)
  const tangent = quadraticTangent(startX, startY, controlX, controlY, endX, endY, 0.5)
  return {
    path: `M ${startX} ${startY} Q ${controlX} ${controlY} ${endX} ${endY}`,
    labelX: labelPoint.x + normalX * Math.sign(curve) * 8,
    labelY: labelPoint.y + normalY * Math.sign(curve) * 8,
    labelAngle: normalizeReadableAngle(Math.atan2(tangent.y, tangent.x) * 180 / Math.PI)
  }
}

function quadraticPoint(
  startX: number,
  startY: number,
  controlX: number,
  controlY: number,
  endX: number,
  endY: number,
  t: number
): { x: number; y: number } {
  const mt = 1 - t
  return {
    x: mt * mt * startX + 2 * mt * t * controlX + t * t * endX,
    y: mt * mt * startY + 2 * mt * t * controlY + t * t * endY
  }
}

function quadraticTangent(
  startX: number,
  startY: number,
  controlX: number,
  controlY: number,
  endX: number,
  endY: number,
  t: number
): { x: number; y: number } {
  return {
    x: 2 * (1 - t) * (controlX - startX) + 2 * t * (endX - controlX),
    y: 2 * (1 - t) * (controlY - startY) + 2 * t * (endY - controlY)
  }
}

function normalizeReadableAngle(angle: number): number {
  if (angle > 90) {
    return angle - 180
  }
  if (angle < -90) {
    return angle + 180
  }
  return angle
}

function graphNodeDisplayLabel(node: GraphNodePayload, expanded: boolean): string {
  const raw = String(node.label || `${node.kind}:${node.id}`)
  let compact = raw
  const descIndex = compact.indexOf('(')
  if (descIndex > 0) {
    compact = `${compact.slice(0, descIndex)}()`
  }
  return truncateTextByWidth(compact, expanded ? 280 : 156, GRAPH_LABEL_FONT)
}

function computeVisibleNodeLabelIds(
  nodes: GraphNodeSim[],
  focus: ReturnType<typeof graphHighlightState>,
  hoveredNodeId: number,
  zoomScale: number
): Set<number> {
  const sorted = [...nodes].sort((left, right) => {
    return graphNodeLabelPriority(right, focus, hoveredNodeId) - graphNodeLabelPriority(left, focus, hoveredNodeId)
  })
  const accepted: Array<{ left: number; right: number; top: number; bottom: number }> = []
  const out = new Set<number>()
  const baseLimit = zoomScale >= 1.4 ? 20 : zoomScale >= 1.05 ? 14 : 10
  for (const node of sorted) {
    const priority = graphNodeLabelPriority(node, focus, hoveredNodeId)
    if (priority <= 0 && out.size >= baseLimit) {
      continue
    }
    const label = graphNodeDisplayLabel(node, priority >= 2)
    if (!label) {
      continue
    }
    const box = estimateNodeLabelBox(node, label)
    const overlaps = accepted.some((item) => {
      return !(box.right < item.left || box.left > item.right || box.bottom < item.top || box.top > item.bottom)
    })
    if (overlaps && priority <= 1) {
      continue
    }
    out.add(node.id)
    accepted.push(box)
  }
  return out
}

function graphNodeLabelPriority(
  node: GraphNodePayload,
  focus: ReturnType<typeof graphHighlightState>,
  hoveredNodeId: number
): number {
  if (focus.nodeIds.has(node.id)) {
    return 4
  }
  if (focus.edgeNodeIds.has(node.id)) {
    return 3
  }
  if (hoveredNodeId === node.id) {
    return 3
  }
  return 1
}

function estimateNodeLabelBox(node: GraphNodePayload, label: string): { left: number; right: number; top: number; bottom: number } {
  const width = Math.max(24, measureTextWidth(label, GRAPH_LABEL_FONT))
  const height = 16
  const x = Number.isFinite(node.x) ? Number(node.x) : 0
  const y = Number.isFinite(node.y) ? Number(node.y) : 0
  const baseline = y - nodeRadius(node) - 11
  return {
    left: x - width / 2 - 4,
    right: x + width / 2 + 4,
    top: baseline - height + 2,
    bottom: baseline + 4
  }
}

function edgeDisplayLabel(edge: GraphEdgePayload): string {
  return edgeDisplayGroup(edge)
}

function computeVisibleEdgeLabelIds(
  edges: GraphEdgeSim[],
  edgeGeometry: Map<number, ReturnType<typeof computeEdgeGeometry>>,
  focus: ReturnType<typeof graphHighlightState>,
  hoveredEdgeId: number,
  zoomScale: number
): Set<number> {
  const out = new Set<number>()
  const accepted: Array<{ left: number; right: number; top: number; bottom: number }> = []
  const relTypeCounts = new Map<string, number>()
  for (const edge of edges) {
    const key = edgeDisplayLabel(edge) || '__unknown__'
    relTypeCounts.set(key, (relTypeCounts.get(key) || 0) + 1)
  }
  const baseLimit = zoomScale >= 1.6 ? 28 : zoomScale >= 1.25 ? 18 : zoomScale >= 0.95 ? 12 : 7
  const sorted = [...edges].sort((left, right) => {
    return edgeLabelPriority(right, relTypeCounts, focus, hoveredEdgeId)
      - edgeLabelPriority(left, relTypeCounts, focus, hoveredEdgeId)
  })
  for (const edge of sorted) {
    const priority = edgeLabelPriority(edge, relTypeCounts, focus, hoveredEdgeId)
    const label = truncateTextByWidth(edgeDisplayLabel(edge), 120, '700 9px "SF Mono", "JetBrains Mono", "Consolas", monospace')
    if (!label) {
      continue
    }
    const geometry = edgeGeometry.get(edge.id)
    if (!geometry) {
      continue
    }
    const box = estimateEdgeLabelBox(label, geometry.labelX, geometry.labelY, geometry.labelAngle)
    const overlaps = accepted.some((item) => {
      return !(box.right < item.left || box.left > item.right || box.bottom < item.top || box.top > item.bottom)
    })
    if (overlaps && priority < 3) {
      continue
    }
    if (out.size >= baseLimit && priority < 3) {
      continue
    }
    out.add(edge.id)
    accepted.push(box)
  }
  return out
}

function edgeLabelPriority(
  edge: GraphEdgeSim,
  relTypeCounts: Map<string, number>,
  focus: ReturnType<typeof graphHighlightState>,
  hoveredEdgeId: number
): number {
  if (focus.edgeIds.has(edge.id)) {
    return 6
  }
  if (hoveredEdgeId === edge.id) {
    return 5
  }
  const relType = edgeDisplayLabel(edge) || '__unknown__'
  const count = relTypeCounts.get(relType) || 1
  if (count === 1) {
    return 4
  }
  if (relType === 'CALL' || relType === 'ALIAS') {
    return 3
  }
  return 2
}

function estimateEdgeLabelBox(
  label: string,
  centerX: number,
  centerY: number,
  angle: number
): { left: number; right: number; top: number; bottom: number } {
  const width = Math.max(18, measureTextWidth(label, '700 9px "SF Mono", "JetBrains Mono", "Consolas", monospace'))
  const height = 12
  const radians = Math.abs(angle) * Math.PI / 180
  const halfWidth = width / 2
  const halfHeight = height / 2
  const projectedX = Math.abs(Math.cos(radians) * halfWidth) + Math.abs(Math.sin(radians) * halfHeight)
  const projectedY = Math.abs(Math.sin(radians) * halfWidth) + Math.abs(Math.cos(radians) * halfHeight)
  return {
    left: centerX - projectedX - 4,
    right: centerX + projectedX + 4,
    top: centerY - projectedY - 3,
    bottom: centerY + projectedY + 3
  }
}

function clampFloat(value: number, min: number, max: number): number {
  if (!Number.isFinite(value)) {
    return 0
  }
  return Math.max(min, Math.min(max, value))
}

function togglePinnedNode(frame: FrameState, nodeId: number): void {
  const next = new Set<number>(frame.graphPinnedNodeIds)
  if (next.has(nodeId)) {
    next.delete(nodeId)
  } else {
    next.add(nodeId)
  }
  frame.graphPinnedNodeIds = Array.from(next)
}

function restoreGraphNodePositions(frame: FrameState, nodes: GraphNodeSim[]): void {
  const saved = frame.graphNodePositions
  if (!saved || nodes.length === 0) {
    return
  }
  for (const node of nodes) {
    const position = saved[node.id]
    if (!position) {
      continue
    }
    if (!Number.isFinite(position.x) || !Number.isFinite(position.y)) {
      continue
    }
    node.x = position.x
    node.y = position.y
    node.vx = 0
    node.vy = 0
  }
}

function persistGraphNodePositions(frame: FrameState, nodes: GraphNodeSim[]): void {
  const next: Record<number, GraphNodePosition> = {}
  for (const node of nodes) {
    if (!Number.isFinite(node.x) || !Number.isFinite(node.y)) {
      continue
    }
    next[node.id] = {
      x: Number(node.x),
      y: Number(node.y)
    }
  }
  frame.graphNodePositions = next
}

function truncateTextByWidth(text: string, maxWidth: number, font: string): string {
  const raw = String(text || '').trim()
  if (!raw) {
    return ''
  }
  if (measureTextWidth(raw, font) <= maxWidth) {
    return raw
  }
  let end = raw.length
  while (end > 1) {
    const candidate = `${raw.slice(0, end - 1).trimEnd()}…`
    if (measureTextWidth(candidate, font) <= maxWidth) {
      return candidate
    }
    end--
  }
  return '…'
}

function measureTextWidth(text: string, font: string): number {
  const safeText = String(text || '')
  const key = `${font}::${safeText}`
  const cached = textWidthCache.get(key)
  if (cached != null) {
    return cached
  }
  if (!textMeasureContext) {
    const canvas = document.createElement('canvas')
    textMeasureContext = canvas.getContext('2d')
  }
  const context = textMeasureContext
  if (!context) {
    return safeText.length * 7
  }
  context.font = font
  const width = context.measureText(safeText).width
  if (textWidthCache.size > 10000) {
    textWidthCache.clear()
  }
  textWidthCache.set(key, width)
  return width
}

function resolveGraphNodeId(
  raw: unknown,
  properties: Record<string, unknown>
): number {
  if (isRecord(raw)) {
    const propertyNodeId = firstNumericField(properties, 'node_id', 'nodeId')
    if (propertyNodeId > 0) {
      return propertyNodeId
    }
    return firstNumericField(raw, 'node_id', 'nodeId', 'id')
  }
  return firstNumericField(properties, 'node_id', 'nodeId')
}

function resolveGraphEdgeNodeId(
  raw: Record<string, unknown>,
  properties: Record<string, unknown>,
  ...keys: string[]
): number {
  const direct = firstNumericField(raw, ...keys)
  if (direct > 0) {
    return direct
  }
  return firstNumericField(properties, ...keys)
}

function resolveGraphEdgeId(
  raw: Record<string, unknown>,
  properties: Record<string, unknown>
): number {
  const direct = firstNumericField(raw, 'edge_id', 'edgeId', 'id')
  if (direct > 0) {
    return direct
  }
  return firstNumericField(properties, 'edge_id', 'edgeId', 'id')
}

function valueMatches(left: unknown, right: unknown): boolean {
  if (left === right) {
    return true
  }
  if (Array.isArray(left)) {
    return left.some((item) => valueMatches(item, right))
  }
  if (Array.isArray(right)) {
    return right.some((item) => valueMatches(left, item))
  }
  return toCell(left) === toCell(right)
}

function buildInspectorFragment(key: string, value: unknown): string {
  if (value == null) {
    return ''
  }
  const safeKey = String(key || '').trim()
  const textValue = typeof value === 'string' ? value.trim() : value
  if (!safeKey) {
    return toCypherLiteral(textValue)
  }
  if (safeKey === 'label') {
    return `:${String(textValue).trim()}`
  }
  if (safeKey === 'rel_type') {
    return `:${String(textValue).trim()}`
  }
  if (safeKey === 'display_rel_type') {
    return `type(r) = ${toCypherLiteral(textValue)}`
  }
  if (safeKey === 'rel_subtype') {
    return `ja.relSubtype(type(r)) = ${toCypherLiteral(textValue)}`
  }
  if (safeKey === 'is_source' && textValue === true) {
    return 'ja.isSource(n)'
  }
  if (safeKey === 'is_sink' && textValue === true) {
    return 'ja.isSink(n)'
  }
  if (safeKey === 'sink_kind') {
    return `ja.sinkKind(n) = ${toCypherLiteral(textValue)}`
  }
  return `${quoteCypherIdentifier(safeKey)} = ${toCypherLiteral(textValue)}`
}

function insertQueryFragment(fragment: string): void {
  const text = (fragment || '').trim()
  if (!text) {
    return
  }
  const selection = editor.state.selection.main
  const before = editor.state.doc.sliceString(Math.max(0, selection.from - 1), selection.from)
  const after = editor.state.doc.sliceString(selection.to, Math.min(editor.state.doc.length, selection.to + 1))
  const prefix = before && !/\s|[(\[{,]/.test(before) ? ' ' : ''
  const suffix = after && !/\s|[)\]},]/.test(after) ? ' ' : ''
  const insert = `${prefix}${text}${suffix}`
  const anchor = selection.from + insert.length
  editor.dispatch({
    changes: { from: selection.from, to: selection.to, insert },
    selection: { anchor }
  })
  editor.focus()
}

function quoteCypherIdentifier(value: string): string {
  return /^[A-Za-z_][A-Za-z0-9_]*$/.test(value) ? value : `\`${value.replace(/`/g, '``')}\``
}

function toCypherLiteral(value: unknown): string {
  if (value == null) {
    return 'null'
  }
  if (typeof value === 'number' || typeof value === 'boolean') {
    return String(value)
  }
  if (Array.isArray(value)) {
    return `[${value.map((item) => toCypherLiteral(item)).join(', ')}]`
  }
  if (isRecord(value)) {
    return JSON.stringify(value)
  }
  return `'${String(value).replace(/\\/g, '\\\\').replace(/'/g, "\\'")}'`
}

async function refreshScripts(): Promise<void> {
  try {
    const scripts = await bridgeCall<ScriptListResponse>(CHANNEL_SCRIPT_LIST, {})
    state.scripts = scripts?.items || []
    clearNotice()
    renderScripts()
  } catch (error) {
    setNotice('error', `${tr('脚本刷新失败', 'Failed to refresh scripts')}: ${bridgeMessage(error, 'script_refresh_failed')}`)
  }
}

async function saveScriptFromEditor(): Promise<void> {
  const body = editor.state.doc.toString().trim()
  if (!body) {
    return
  }
  const promptedTitle = window.prompt(tr('脚本标题', 'Script Title'), deriveScriptTitle(body))
  if (promptedTitle === null) {
    return
  }
  const promptedTags = window.prompt(tr('脚本标签（逗号分隔）', 'Script tags (comma separated)'), '')
  if (promptedTags === null) {
    return
  }
  const title = promptedTitle.trim() || deriveScriptTitle(body)
  const tags = promptedTags.trim()
  await saveScript({
    scriptId: undefined,
    title,
    body,
    tags,
    pinned: false
  })
}

async function saveScriptFromFrame(frame: FrameState): Promise<void> {
  const promptedTitle = window.prompt(tr('脚本标题', 'Script Title'), deriveScriptTitle(frame.query))
  if (promptedTitle === null) {
    return
  }
  const promptedTags = window.prompt(tr('脚本标签（逗号分隔）', 'Script tags (comma separated)'), '')
  if (promptedTags === null) {
    return
  }
  const title = promptedTitle.trim() || deriveScriptTitle(frame.query)
  const tags = promptedTags.trim()
  await saveScript({
    scriptId: undefined,
    title,
    body: frame.query,
    tags,
    pinned: false
  })
}

async function saveScript(payload: {
  scriptId?: number
  title: string
  body: string
  tags: string
  pinned: boolean
}): Promise<void> {
  try {
    await bridgeCall<ScriptItem>(CHANNEL_SCRIPT_SAVE, payload)
    clearNotice()
    await refreshScripts()
  } catch (error) {
    setNotice('error', `${tr('脚本保存失败', 'Failed to save script')}: ${bridgeMessage(error, 'script_save_failed')}`)
  }
}

async function deleteScript(scriptId: number): Promise<void> {
  try {
    await bridgeCall(CHANNEL_SCRIPT_DELETE, { scriptId })
    clearNotice()
    await refreshScripts()
  } catch (error) {
    setNotice('error', `${tr('脚本删除失败', 'Failed to delete script')}: ${bridgeMessage(error, 'script_delete_failed')}`)
  }
}

function applyScriptBody(body: string, execute = false): void {
  setEditorText(body)
  if (execute) {
    void runQuery(body)
  }
}

async function requestFullscreen(fullscreen: boolean): Promise<void> {
  try {
    await bridgeCall(CHANNEL_UI_FULLSCREEN, { fullscreen })
    state.fullscreen = fullscreen
    clearNotice()
    refreshHeaderLabels()
  } catch (error) {
    setNotice('error', `${tr('全屏切换失败', 'Failed to toggle fullscreen')}: ${bridgeMessage(error, 'fullscreen_toggle_failed')}`)
  }
}

function setEditorText(text: string): void {
  const safe = text || ''
  editor.dispatch({
    changes: {
      from: 0,
      to: editor.state.doc.length,
      insert: safe
    }
  })
  editor.focus()
}

function selectPrevFrame(): void {
  if (state.frames.length === 0) {
    return
  }
  const index = Math.max(0, state.frames.findIndex((item) => item.frameId === state.selectedFrameId))
  const next = index <= 0 ? state.frames.length - 1 : index - 1
  selectFrame(state.frames[next].frameId)
}

function selectNextFrame(): void {
  if (state.frames.length === 0) {
    return
  }
  const index = Math.max(0, state.frames.findIndex((item) => item.frameId === state.selectedFrameId))
  const next = index >= state.frames.length - 1 ? 0 : index + 1
  selectFrame(state.frames[next].frameId)
}

function updateSimulationVisibility(): void {
  for (const controller of frameControllers.values()) {
    controller.setHostVisible(state.hostVisible)
  }
}

function createFrameButton(text: string, title: string): HTMLButtonElement {
  const button = document.createElement('button')
  button.className = 'frame-btn'
  button.textContent = text
  button.title = title
  return button
}

function createTabButton(text: string, active: boolean, disabled: boolean): HTMLButtonElement {
  const button = document.createElement('button')
  button.className = `tab-btn${active ? ' active' : ''}`
  button.textContent = text
  button.disabled = disabled
  return button
}

function createBadge(text: string, kind: 'ok' | 'warn' | 'err' | 'default' = 'default'): HTMLElement {
  const badge = document.createElement('span')
  badge.className = `badge${kind === 'default' ? '' : ' ' + kind}`
  badge.textContent = text
  return badge
}

function nodeRadius(node: GraphNodePayload): number {
  const kind = (node.kind || '').toLowerCase()
  if (kind.includes('method')) {
    return 17
  }
  if (kind.includes('class')) {
    return 19
  }
  return 15
}

function nodeColor(kind: string, theme: GraphThemeColors = graphThemeColors()): string {
  const safe = (kind || '').toLowerCase()
  if (safe.includes('method')) {
    return theme.nodeFillMethod
  }
  if (safe.includes('class')) {
    return theme.nodeFillClass
  }
  if (safe.includes('call')) {
    return theme.nodeFillCall
  }
  return theme.nodeFillDefault
}

function drag(
  simulation: d3.Simulation<GraphNodeSim, GraphEdgeSim>,
  pinnedNodeIds: Set<number>,
  persistPositions: () => void
) {
  function dragStarted(event: d3.D3DragEvent<SVGCircleElement, GraphNodeSim, GraphNodeSim>, d: GraphNodeSim): void {
    if (!event.active) {
      simulation.alphaTarget(0.3).restart()
    }
    d.fx = d.x
    d.fy = d.y
  }

  function dragged(event: d3.D3DragEvent<SVGCircleElement, GraphNodeSim, GraphNodeSim>, d: GraphNodeSim): void {
    d.fx = event.x
    d.fy = event.y
    d.x = event.x
    d.y = event.y
    persistPositions()
  }

  function dragEnded(event: d3.D3DragEvent<SVGCircleElement, GraphNodeSim, GraphNodeSim>, d: GraphNodeSim): void {
    if (!event.active) {
      simulation.alphaTarget(0)
    }
    if (pinnedNodeIds.has(d.id)) {
      d.fx = event.x
      d.fy = event.y
      d.x = event.x
      d.y = event.y
      persistPositions()
      return
    }
    d.fx = null
    d.fy = null
    d.x = event.x
    d.y = event.y
    persistPositions()
  }

  return d3.drag<SVGCircleElement, GraphNodeSim>().on('start', dragStarted).on('drag', dragged).on('end', dragEnded)
}

function getRequired<T extends HTMLElement>(id: string): T {
  const element = document.getElementById(id)
  if (!element) {
    throw new Error(`element not found: ${id}`)
  }
  return element as T
}

function bridgeMessage(error: unknown, fallbackCode: string): string {
  const detail = extractBridgeError(error, fallbackCode)
  return humanizeErrorMessage(detail.message)
}

function humanizeErrorMessage(raw: string): string {
  const text = (raw || '').trim()
  if (!text) {
    return tr('未知错误', 'Unknown error')
  }
  return text.replace(/^[a-z0-9_]+:\s*/i, '')
}

function tr(zh: string, en: string): string {
  return state.ui.language === 'en' ? en : zh
}

function deriveScriptTitle(text: string): string {
  const line = text.split(/\r?\n/, 1)[0]?.trim() || ''
  if (!line) {
    return tr('未命名脚本', 'Untitled Script')
  }
  if (line.length <= 80) {
    return line
  }
  return `${line.slice(0, 80)}...`
}

function escapeHtml(value: string): string {
  return String(value || '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

window.addEventListener('beforeunload', () => {
  framesEl.removeEventListener('scroll', handleFrameRailScroll)
  window.removeEventListener('resize', handleWorkbenchResize)
  disposeAllFrameResources()
  editor.destroy()
})
