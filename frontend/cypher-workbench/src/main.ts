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
  edgeSubtype,
  renderInspector,
  type FrameSelection,
  type GraphFilter,
  type GraphRelationMode
} from './inspector'
import {
  CHANNEL_QUERY_CAPABILITIES,
  CHANNEL_QUERY_EXECUTE,
  CHANNEL_QUERY_EXPLAIN,
  CHANNEL_SCRIPT_DELETE,
  CHANNEL_SCRIPT_LIST,
  CHANNEL_SCRIPT_SAVE,
  CHANNEL_UI_CONTEXT,
  CHANNEL_UI_FULLSCREEN,
  clampInt,
  DEFAULT_QUERY_OPTIONS,
  normalizeCapabilities,
  normalizeProfile,
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
import { buildScriptsCountText, renderScriptList, type BuiltinScriptMode } from './scripts'
import { isRecord, toCell } from './view-helpers'

declare global {
  interface Window {
    JA_WORKBENCH?: {
      updateUiContext: (ctx: UiContext) => void
      setFullscreen: (fullscreen: boolean) => void
      onHostVisibility: (visible: boolean) => void
    }
    __JA_BOOT_ERROR?: string
  }
}

const MAX_FRAMES = 50
const TABLE_ROW_HEIGHT = 26
const QUERY_OPTIONS_STORAGE_KEY = 'ja.workbench.query-options.v1'
const TRAVERSAL_MODE_STORAGE_KEY = 'ja.workbench.traversal-mode.v1'

type FrameViewMode = 'graph' | 'table' | 'text'
type WorkbenchGraphMode = Exclude<BuiltinScriptMode, 'all'>
type TraversalMode = 'call-only' | 'call+alias'
type GraphViewport = { x: number; y: number; k: number }
type GraphNodePosition = { x: number; y: number }

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
  graphRelationMode: GraphRelationMode
  graphViewport: GraphViewport | null
  graphPinnedNodeIds: number[]
  graphNodePositions: Record<number, GraphNodePosition>
  errorCode?: string
  errorMessage?: string
}

interface SimRef {
  simulation: d3.Simulation<GraphNodeSim, GraphEdgeSim>
}

type GraphNodeSim = GraphNodePayload & d3.SimulationNodeDatum

type GraphEdgeSim = d3.SimulationLinkDatum<GraphNodeSim> & GraphEdgePayload
type EdgeRouteMeta = {
  lane: number
  isLoop: boolean
  loopIndex: number
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
  capabilities: null as Record<string, unknown> | null,
  queryOptions: { ...DEFAULT_QUERY_OPTIONS } as QueryUiOptions,
  graphMode: 'call' as WorkbenchGraphMode,
  traversalMode: 'call-only' as TraversalMode
}

const graphSimulations = new Map<string, SimRef>()

installFatalBootHandlers()

const app = document.getElementById('app')
if (!app) {
  throw new Error('app root not found')
}

app.innerHTML = `
  <div class="workbench" id="workbench-root" data-theme="default">
    <header class="wb-header">
      <div class="wb-title-wrap">
        <div class="wb-title" id="title"></div>
        <div class="wb-meta" id="runtime-meta"></div>
      </div>
      <div class="wb-actions">
        <button class="wb-btn" id="btn-save" title="Save Script">☆</button>
        <button class="wb-btn" id="btn-refresh" title="Refresh Scripts">↻</button>
        <button class="wb-btn" id="btn-explain" title="Explain">Explain</button>
        <button class="wb-btn" id="btn-fullscreen" title="Fullscreen">全屏</button>
      </div>
    </header>
    <section class="command-bar">
      <div class="prompt">$</div>
      <div class="editor-wrap"><div id="editor"></div></div>
      <div class="graph-mode-toggle">
        <span id="graph-mode-label">Mode</span>
        <div class="graph-mode-segment">
          <button class="graph-mode-btn active" id="mode-call" type="button">调用图</button>
          <button class="graph-mode-btn" id="mode-structure" type="button">结构图</button>
        </div>
      </div>
      <div class="traversal-mode-toggle">
        <span id="traversal-mode-label">Traversal</span>
        <div class="traversal-mode-segment">
          <button class="traversal-mode-btn active" id="mode-call-only" type="button">CALL</button>
          <button class="traversal-mode-btn" id="mode-call-alias" type="button">CALL + ALIAS</button>
        </div>
      </div>
      <div class="query-opts">
        <label class="opt-item">
          <span id="opt-profile-label">Profile</span>
          <select id="opt-profile">
            <option value="default">default</option>
            <option value="long-chain">long-chain</option>
          </select>
        </label>
        <label class="opt-item">
          <span id="opt-rows-label">Rows</span>
          <input id="opt-max-rows" type="number" min="1" max="10000" step="1" />
        </label>
        <label class="opt-item">
          <span id="opt-timeout-label">Timeout</span>
          <input id="opt-max-ms" type="number" min="100" max="180000" step="100" />
        </label>
      </div>
      <button class="wb-btn primary" id="btn-run">Run</button>
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
const runtimeMetaEl = getRequired<HTMLElement>('runtime-meta')
const scriptsTitleEl = getRequired<HTMLElement>('scripts-title')
const scriptsCountEl = getRequired<HTMLElement>('scripts-count')
const graphModeLabelEl = getRequired<HTMLElement>('graph-mode-label')
const traversalModeLabelEl = getRequired<HTMLElement>('traversal-mode-label')
const framesEl = getRequired<HTMLElement>('frames')
const scriptListEl = getRequired<HTMLElement>('script-list')
const runButton = getRequired<HTMLButtonElement>('btn-run')
const refreshButton = getRequired<HTMLButtonElement>('btn-refresh')
const explainButton = getRequired<HTMLButtonElement>('btn-explain')
const saveButton = getRequired<HTMLButtonElement>('btn-save')
const fullscreenButton = getRequired<HTMLButtonElement>('btn-fullscreen')
const editorHost = getRequired<HTMLElement>('editor')
const profileLabelEl = getRequired<HTMLElement>('opt-profile-label')
const rowsLabelEl = getRequired<HTMLElement>('opt-rows-label')
const timeoutLabelEl = getRequired<HTMLElement>('opt-timeout-label')
const profileSelect = getRequired<HTMLSelectElement>('opt-profile')
const maxRowsInput = getRequired<HTMLInputElement>('opt-max-rows')
const maxMsInput = getRequired<HTMLInputElement>('opt-max-ms')
const modeCallButton = getRequired<HTMLButtonElement>('mode-call')
const modeStructureButton = getRequired<HTMLButtonElement>('mode-structure')
const modeCallOnlyButton = getRequired<HTMLButtonElement>('mode-call-only')
const modeCallAliasButton = getRequired<HTMLButtonElement>('mode-call-alias')

let editor = new EditorView({
  state: EditorState.create({
    doc: 'MATCH (m:Method)-[r]->(n) RETURN m,r,n LIMIT 50',
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

explainButton.addEventListener('click', () => {
  void runExplain()
})

hydrateQueryOptions()
hydrateTraversalMode()
syncQueryOptionControls()

profileSelect.addEventListener('change', () => {
  state.queryOptions.profile = normalizeProfile(profileSelect.value)
  persistQueryOptions()
  syncQueryOptionControls()
})

maxRowsInput.addEventListener('change', () => {
  state.queryOptions.maxRows = clampInt(maxRowsInput.value, 1, 10000, state.queryOptions.maxRows)
  persistQueryOptions()
  syncQueryOptionControls()
})

maxMsInput.addEventListener('change', () => {
  state.queryOptions.maxMs = clampInt(maxMsInput.value, 100, 180000, state.queryOptions.maxMs)
  persistQueryOptions()
  syncQueryOptionControls()
})

modeCallButton.addEventListener('click', () => {
  setGraphMode('call')
})

modeStructureButton.addEventListener('click', () => {
  setGraphMode('structure')
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
  }
}

void bootstrap()

function installFatalBootHandlers(): void {
  const report = (message: string): void => {
    const text = (message || '').trim()
    if (!text) {
      return
    }
    window.__JA_BOOT_ERROR = text
    const root = document.getElementById('app')
    if (root && root.childElementCount > 0) {
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
  const rawCapabilities = await safeBridgeCall<unknown>(CHANNEL_QUERY_CAPABILITIES, {})
  const capabilities = normalizeCapabilities(rawCapabilities)
  if (capabilities) {
    state.capabilities = capabilities
    applyCapabilityProfiles(capabilities)
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
}

function refreshHeaderLabels(): void {
  titleEl.textContent = 'Graph Console'
  runtimeMetaEl.textContent = buildRuntimeMeta()
  scriptsTitleEl.textContent = tr('模板与脚本', 'Templates & Scripts')
  scriptsCountEl.textContent = buildScriptsCountText(state.scripts.length, state.graphMode, tr)
  graphModeLabelEl.textContent = tr('模式', 'Mode')
  traversalModeLabelEl.textContent = tr('遍历', 'Traversal')
  runButton.textContent = tr('运行', 'Run')
  explainButton.textContent = tr('解释', 'Explain')
  refreshButton.textContent = tr('刷新', 'Refresh')
  saveButton.textContent = tr('收藏', 'Save')
  fullscreenButton.textContent = state.fullscreen
    ? tr('退出全屏', 'Exit Fullscreen')
    : tr('全屏', 'Fullscreen')
  fullscreenButton.title = state.fullscreen ? tr('退出全屏', 'Exit Fullscreen') : tr('全屏', 'Fullscreen')
  profileLabelEl.textContent = tr('策略', 'Profile')
  rowsLabelEl.textContent = tr('行数', 'Rows')
  timeoutLabelEl.textContent = tr('超时(ms)', 'Timeout(ms)')
  profileSelect.title = tr('查询策略', 'Query profile')
  maxRowsInput.title = tr('最大返回行数', 'Max result rows')
  maxMsInput.title = tr('查询超时（毫秒）', 'Query timeout in ms')
  modeCallButton.textContent = tr('调用图', 'Call Graph')
  modeStructureButton.textContent = tr('结构图', 'Structure Graph')
  modeCallButton.classList.toggle('active', state.graphMode === 'call')
  modeStructureButton.classList.toggle('active', state.graphMode === 'structure')
  modeCallOnlyButton.textContent = 'CALL'
  modeCallAliasButton.textContent = 'CALL + ALIAS'
  modeCallOnlyButton.classList.toggle('active', state.traversalMode === 'call-only')
  modeCallAliasButton.classList.toggle('active', state.traversalMode === 'call+alias')
  syncQueryOptionControls()
}

function buildRuntimeMeta(): string {
  const parts: string[] = []
  const capabilities = state.capabilities
  if (capabilities) {
    const engine = capabilities.engine
    if (typeof engine === 'string' && engine.trim()) {
      parts.push(engine.trim())
    }
    if (capabilities.readOnly === true) {
      parts.push(tr('只读', 'read-only'))
    }
  }
  parts.push(`${tr('模式', 'mode')}: ${state.graphMode === 'structure' ? tr('结构图', 'structure') : tr('调用图', 'call')}`)
  parts.push(`${tr('遍历', 'traversal')}: ${state.traversalMode === 'call+alias' ? 'CALL + ALIAS' : 'CALL'}`)
  parts.push(`${tr('策略', 'profile')}: ${state.queryOptions.profile}`)
  return parts.join(' · ')
}

function setGraphMode(mode: WorkbenchGraphMode): void {
  if (state.graphMode === mode) {
    return
  }
  state.graphMode = mode
  renderAll()
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

function applyCapabilityProfiles(capabilities: Record<string, unknown>): void {
  const profilesRaw = capabilities.profiles
  const availableProfiles = Array.isArray(profilesRaw)
    ? profilesRaw.map((item) => normalizeProfile(item)).filter((value, index, arr) => arr.indexOf(value) === index)
    : ['default']
  const supportsLongChain = availableProfiles.includes('long-chain')
  const longChainOption = Array.from(profileSelect.options).find((item) => item.value === 'long-chain')
  if (longChainOption) {
    longChainOption.disabled = !supportsLongChain
  }
  if (!supportsLongChain && state.queryOptions.profile === 'long-chain') {
    state.queryOptions.profile = 'default'
    persistQueryOptions()
  }
  syncQueryOptionControls()
}

function syncQueryOptionControls(): void {
  profileSelect.value = normalizeProfile(state.queryOptions.profile)
  maxRowsInput.value = String(clampInt(state.queryOptions.maxRows, 1, 10000, DEFAULT_QUERY_OPTIONS.maxRows))
  maxMsInput.value = String(clampInt(state.queryOptions.maxMs, 100, 180000, DEFAULT_QUERY_OPTIONS.maxMs))
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
      profile: normalizeProfile(parsed.profile ?? state.queryOptions.profile),
      maxRows: clampInt(parsed.maxRows, 1, 10000, state.queryOptions.maxRows),
      maxMs: clampInt(parsed.maxMs, 100, 180000, state.queryOptions.maxMs),
      maxHops: clampInt(parsed.maxHops, 1, 256, state.queryOptions.maxHops),
      maxPaths: clampInt(parsed.maxPaths, 1, 5000, state.queryOptions.maxPaths)
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

async function runExplain(): Promise<void> {
  const rawQuery = editor.state.doc.toString().trim()
  if (!rawQuery) {
    return
  }
  const query = materializeQuery(rawQuery)
  try {
    const result = await bridgeCall<Record<string, unknown>>(CHANNEL_QUERY_EXPLAIN, { query })
    const text = JSON.stringify(result, null, 2)
    pushFrame({
      frameId: `explain-${Date.now()}`,
      query,
      columns: ['key', 'value'],
      rows: Object.entries(result).map(([k, v]) => [k, toCell(v)]),
      warnings: [],
      truncated: false,
      elapsedMs: 0,
      graph: null,
      text,
      collapsed: false,
      view: 'text',
      selection: null,
      tableFocus: null,
      graphFocus: null,
      graphFilter: null,
      graphRelationMode: 'group',
      graphViewport: null,
      graphPinnedNodeIds: [],
      graphNodePositions: {}
    })
  } catch (error) {
    const bridgeError = extractBridgeError(error, 'cypher_explain_error')
    pushFrame(buildErrorFrame(query, bridgeError.code, bridgeError.message))
  }
}

function pushFrame(frame: FrameState): void {
  state.frames = [frame, ...state.frames.filter((item) => item.frameId !== frame.frameId)].slice(0, MAX_FRAMES)
  if (!state.selectedFrameId || !state.frames.some((item) => item.frameId === state.selectedFrameId)) {
    state.selectedFrameId = frame.frameId
  } else {
    state.selectedFrameId = frame.frameId
  }
  renderFrames()
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
    graphRelationMode: 'group',
    graphViewport: null,
    graphPinnedNodeIds: [],
    graphNodePositions: {}
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
    graphRelationMode: 'group',
    graphViewport: null,
    graphPinnedNodeIds: [],
    graphNodePositions: {},
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
  scriptsCountEl.textContent = buildScriptsCountText(state.scripts.length, state.graphMode, tr)
  renderScriptList({
    container: scriptListEl,
    scripts: state.scripts,
    graphMode: state.graphMode,
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
  stopAllSimulations()
  framesEl.innerHTML = ''
  if (state.frames.length === 0) {
    const empty = document.createElement('div')
    empty.className = 'empty'
    empty.textContent = tr('输入 Cypher 后按 Enter 运行', 'Type Cypher and press Enter')
    framesEl.appendChild(empty)
    return
  }

  for (const frame of state.frames) {
    const card = document.createElement('section')
    card.className = `frame${frame.frameId === state.selectedFrameId ? ' selected' : ''}`
    card.addEventListener('click', () => {
      state.selectedFrameId = frame.frameId
      renderFrames()
    })

    const head = document.createElement('div')
    head.className = 'frame-head'

    const fold = createFrameButton(frame.collapsed ? '▸' : '▾', frame.collapsed ? tr('展开', 'Expand') : tr('收起', 'Collapse'))
    fold.addEventListener('click', (event) => {
      event.stopPropagation()
      frame.collapsed = !frame.collapsed
      renderFrames()
    })

    const query = document.createElement('div')
    query.className = 'frame-query'
    query.textContent = `$ ${frame.query}`

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
      state.frames = state.frames.filter((item) => item.frameId !== frame.frameId)
      if (state.selectedFrameId === frame.frameId) {
        state.selectedFrameId = state.frames[0]?.frameId || ''
      }
      renderFrames()
    })

    actions.append(run, save, full, close)
    head.append(fold, query, actions)
    card.appendChild(head)

    const status = document.createElement('div')
    status.className = 'frame-status'
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
    card.appendChild(status)

    if (frame.graphFilter) {
      card.appendChild(
        buildGraphFilterBar(frame, {
          tr,
          clearGraphFilter: () => {
            frame.graphFilter = null
            renderFrames()
          }
        })
      )
    }

    if (!frame.collapsed) {
      card.appendChild(buildFrameBody(frame))
    }

    framesEl.appendChild(card)
  }

  updateSimulationVisibility()
}

function buildFrameBody(frame: FrameState): HTMLElement {
  const body = document.createElement('div')
  body.className = 'frame-body'

  const tabs = document.createElement('div')
  tabs.className = 'view-tabs'

  const modeGraph = frame.graph ? applyWorkbenchGraphMode(frame.graph, state.graphMode) : null
  const filteredGraph = modeGraph ? applyGraphFilter(modeGraph, frame.graphFilter) : null
  const normalizedGraph = normalizeGraph(filteredGraph || undefined)
  const graphEnabled = !!normalizedGraph && normalizedGraph.nodes.length > 0
  const graphTab = createTabButton('Graph', frame.view === 'graph', !graphEnabled)
  const tableTab = createTabButton('Table', frame.view === 'table', false)
  const textTab = createTabButton('Text', frame.view === 'text', false)

  graphTab.addEventListener('click', () => {
    if (!graphEnabled) {
      return
    }
    frame.view = 'graph'
    renderFrames()
  })
  tableTab.addEventListener('click', () => {
    frame.view = 'table'
    renderFrames()
  })
  textTab.addEventListener('click', () => {
    frame.view = 'text'
    renderFrames()
  })

  tabs.append(graphTab, tableTab, textTab)

  const content = document.createElement('div')
  content.className = 'view-content'

  const graphView = document.createElement('div')
  graphView.className = `graph-view${frame.view === 'graph' ? ' active' : ''}`
  const tableView = document.createElement('div')
  tableView.className = `table-view${frame.view === 'table' ? ' active' : ''}`
  const textView = document.createElement('div')
  textView.className = `text-view${frame.view === 'text' ? ' active' : ''}`

  if (frame.view === 'graph' && graphEnabled && normalizedGraph) {
    try {
      renderGraphView(graphView, frame, normalizedGraph)
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error || 'graph render failed')
      graphView.innerHTML = `<div class="empty">${tr('图渲染失败', 'Graph render failed')}: ${escapeHtml(message)}</div>`
    }
  } else {
    const emptyReason = frame.graph && frame.graph.nodes.length > 0
      ? state.graphMode === 'structure'
        ? tr('当前结果在结构图模式下无可展示内容，切回调用图或改查类结构', 'No visible graph in structure mode. Switch back to call graph or query class structure.')
        : tr('当前结果在调用图模式下无可展示内容，切到结构图可查看类结构', 'No visible graph in call mode. Switch to structure mode to inspect class graph.')
      : tr('该结果不可图形化', 'Graph unavailable for this result')
    graphView.innerHTML = `<div class="empty">${emptyReason}</div>`
  }

  renderVirtualTable(tableView, frame)
  renderTextView(textView, frame)

  content.append(graphView, tableView, textView)

  const propPanel = document.createElement('aside')
  propPanel.className = 'prop-panel'
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
      renderFrames()
    }
  })

  body.append(tabs, content, propPanel)
  return body
}

function renderGraphView(container: HTMLElement, frame: FrameState, graph: GraphFramePayload): void {
  if (!graph) {
    container.innerHTML = `<div class="empty">${tr('无图数据', 'No graph data')}</div>`
    return
  }
  const nodes = graph.nodes
  const edges = graph.edges
  if (nodes.length === 0) {
    container.innerHTML = `<div class="empty">${tr('无图数据', 'No graph data')}</div>`
    return
  }
  container.innerHTML = ''
  const stage = document.createElement('div')
  stage.className = 'graph-stage'
  container.appendChild(stage)
  const width = Math.max(360, stage.clientWidth || container.clientWidth || 720)
  const height = Math.max(260, stage.clientHeight || container.clientHeight || 420)
  const controls = document.createElement('div')
  controls.className = 'graph-zoom-controls'
  const displayControls = document.createElement('div')
  displayControls.className = 'graph-display-controls'
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
  const groupModeButton = document.createElement('button')
  groupModeButton.className = `graph-display-btn${frame.graphRelationMode === 'group' ? ' active' : ''}`
  groupModeButton.type = 'button'
  groupModeButton.textContent = tr('聚合', 'Grouped')
  groupModeButton.title = tr('显示关系类别，如 CALL', 'Show grouped relation labels such as CALL')
  groupModeButton.addEventListener('click', () => {
    if (frame.graphRelationMode === 'group') {
      return
    }
    persistGraphNodePositions(frame, simNodes)
    frame.graphRelationMode = 'group'
    renderFrames()
  })
  const detailModeButton = document.createElement('button')
  detailModeButton.className = `graph-display-btn${frame.graphRelationMode === 'detail' ? ' active' : ''}`
  detailModeButton.type = 'button'
  detailModeButton.textContent = tr('细分', 'Detailed')
  detailModeButton.title = tr('显示关系子类，如 DIRECT / DISPATCH', 'Show relation subtypes such as DIRECT / DISPATCH')
  detailModeButton.addEventListener('click', () => {
    if (frame.graphRelationMode === 'detail') {
      return
    }
    persistGraphNodePositions(frame, simNodes)
    frame.graphRelationMode = 'detail'
    renderFrames()
  })
  displayControls.append(groupModeButton, detailModeButton)
  stage.appendChild(displayControls)

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
  const markerBase = `graph-arrow-${frame.frameId}`
  defs
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
    .attr('fill', '#8e98a6')
  defs
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
    .attr('fill', '#2f81f7')

  const simNodes: GraphNodeSim[] = nodes.map((node) => ({ ...node }))
  const simEdges: GraphEdgeSim[] = edges.map((edge) => ({ ...edge }))
  const nodeOnlyLayout = simEdges.length === 0
  const edgeRouteMeta = buildEdgeRouteMeta(simEdges)
  const pinnedNodeIds = new Set<number>(frame.graphPinnedNodeIds)
  let currentTransform = frame.graphViewport
    ? d3.zoomIdentity.translate(frame.graphViewport.x, frame.graphViewport.y).scale(frame.graphViewport.k)
    : d3.zoomIdentity
  let lastPanSample: { x: number; y: number; at: number } | null = null
  let panVelocity = { x: 0, y: 0 }
  seedGraphLayout(simNodes, width, height, nodeOnlyLayout)
  restoreGraphNodePositions(frame, simNodes)
  for (const node of simNodes) {
    if (pinnedNodeIds.has(node.id)) {
      node.fx = Number.isFinite(node.x) ? Number(node.x) : width / 2
      node.fy = Number.isFinite(node.y) ? Number(node.y) : height / 2
    }
  }

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
  const warmupTicks = nodeOnlyLayout ? Math.min(180, 54 + simNodes.length * 3) : Math.min(120, 36 + simEdges.length)
  for (let i = 0; i < warmupTicks; i++) {
    simulation.tick()
  }

  const links = edgeLayer
    .selectAll('path')
    .data(simEdges)
    .enter()
    .append('path')
    .attr('stroke', '#8e98a6')
    .attr('stroke-width', 1.4)
    .attr('stroke-linecap', 'round')
    .attr('fill', 'none')
    .attr('marker-end', `url(#${markerBase}-base)`)
    .attr('opacity', 0.9)

  const focus = graphHighlightState(frame)
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
    .attr('fill', (item) => nodeColor(item.kind))
    .attr('stroke', (item) => (focus.nodeIds.has(item.id) ? '#2f81f7' : '#2f3640'))
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
      persistGraphNodePositions(frame, simNodes)
      frame.selection = { type: 'node', data: item }
      frame.graphFocus = { nodeIds: [item.id], edgeIds: [] }
      renderFrames()
    })
    .on('dblclick', (_, item) => {
      persistGraphNodePositions(frame, simNodes)
      togglePinnedNode(frame, item.id)
      renderFrames()
    })
    .call(drag(simulation, pinnedNodeIds, () => persistGraphNodePositions(frame, simNodes)))

  nodesSel.append('title').text((item) => item.label)

  const labels = textLayer
    .selectAll('text')
    .data(simNodes)
    .enter()
    .append('text')
    .text((item) => graphNodeDisplayLabel(item, false))
    .attr('font-size', 11)
    .attr('font-weight', 600)
    .attr('fill', '#1f2933')
    .attr('stroke', 'rgba(248, 250, 252, 0.94)')
    .attr('stroke-width', 4)
    .attr('paint-order', 'stroke')
    .attr('text-anchor', 'middle')
    .attr('pointer-events', 'none')

  const edgeLabels = textLayer
    .selectAll('text.edge')
    .data(simEdges)
    .enter()
    .append('text')
    .text((item) => truncateTextByWidth(edgeDisplayLabel(item, frame.graphRelationMode), 120, '700 9px "SF Mono", "JetBrains Mono", "Consolas", monospace'))
    .attr('font-size', 9)
    .attr('font-weight', 700)
    .attr('fill', '#5f6b78')
    .attr('stroke', 'rgba(248, 250, 252, 0.94)')
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
    persistGraphNodePositions(frame, simNodes)
    frame.selection = {
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
    frame.graphFocus = {
      nodeIds: [Number((item.source as GraphNodeSim).id ?? item.source), Number((item.target as GraphNodeSim).id ?? item.target)],
      edgeIds: [item.id]
    }
    renderFrames()
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
      frame.graphViewport = { x: transform.x, y: transform.y, k: transform.k }
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
    persistGraphNodePositions(frame, simNodes)
    const edgeGeometry = new Map<number, ReturnType<typeof computeEdgeGeometry>>()
    for (const edge of simEdges) {
      edgeGeometry.set(edge.id, computeEdgeGeometry(edge, edgeRouteMeta.get(edge.id) || { lane: 0, isLoop: false, loopIndex: 0 }))
    }
    links
      .attr('d', (item) => edgeGeometry.get(item.id)?.path || '')
      .attr('stroke', (item) => (focus.edgeIds.has(item.id) ? '#2f81f7' : '#8e98a6'))
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

    updateGraphVisualState(edgeGeometry)
  }

  simulation.on('tick', applyGraphGeometry)

  if (frame.graphViewport) {
    svg.call(
      zoom.transform,
      d3.zoomIdentity.translate(frame.graphViewport.x, frame.graphViewport.y).scale(frame.graphViewport.k)
    )
  } else {
    fitGraph(false)
  }
  applyGraphGeometry()
  graphSimulations.set(frame.frameId, { simulation })

  function updateGraphVisualState(
    edgeGeometry: Map<number, ReturnType<typeof computeEdgeGeometry>> = new Map()
  ): void {
    const visibleNodeLabelIds = computeVisibleNodeLabelIds(simNodes, focus, hoveredNodeId, currentTransform.k)
    const visibleEdgeLabelIds = computeVisibleEdgeLabelIds(
      simEdges,
      edgeGeometry,
      focus,
      hoveredEdgeId,
      currentTransform.k,
      frame.graphRelationMode
    )
    links
      .attr('stroke', (item) => (focus.edgeIds.has(item.id) || hoveredEdgeId === item.id ? '#2f81f7' : '#8e98a6'))
      .attr('stroke-width', (item) => (focus.edgeIds.has(item.id) || hoveredEdgeId === item.id ? 2.8 : 1.4))
      .attr('opacity', (item) => {
        if (!focus.active && hoveredEdgeId < 0 && hoveredNodeId < 0) {
          return 0.84
        }
        return focus.edgeIds.has(item.id) || hoveredEdgeId === item.id ? 1 : 0.26
      })
    rings
      .attr('stroke', (item) => (focus.nodeIds.has(item.id) ? '#fdcc59' : hoveredNodeId === item.id ? '#6ac6ff' : '#6ac6ff'))
      .attr('opacity', (item) => (focus.nodeIds.has(item.id) ? 0.34 : hoveredNodeId === item.id ? 0.26 : 0))
    nodesSel
      .attr('stroke', (item) => (focus.nodeIds.has(item.id) ? '#fdcc59' : hoveredNodeId === item.id ? '#2f81f7' : '#2f3640'))
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
      .text((item) => truncateTextByWidth(edgeDisplayLabel(item, frame.graphRelationMode), 120, '700 9px "SF Mono", "JetBrains Mono", "Consolas", monospace'))
      .attr('opacity', (item) => {
        if (focus.edgeIds.has(item.id) || hoveredEdgeId === item.id) {
          return 0.96
        }
        return visibleEdgeLabelIds.has(item.id) ? 0.7 : 0
      })
  }
}

function renderVirtualTable(container: HTMLElement, frame: FrameState): void {
  container.innerHTML = ''
  const columns = frame.columns
  const rows = frame.rows
  const focus = frame.tableFocus
  const safeColumns = columns.length > 0 ? columns : ['value']
  const template = safeColumns.map(() => 'minmax(120px, 1fr)').join(' ')

  const header = document.createElement('div')
  header.className = 'vt-header'
  header.style.gridTemplateColumns = template
  for (const col of safeColumns) {
    const cell = document.createElement('div')
    cell.className = 'vt-cell'
    cell.textContent = col
    header.appendChild(cell)
  }

  const scroll = document.createElement('div')
  scroll.className = 'vt-scroll'
  const space = document.createElement('div')
  space.className = 'vt-space'
  space.style.height = `${rows.length * TABLE_ROW_HEIGHT}px`
  const win = document.createElement('div')
  win.className = 'vt-window'
  space.appendChild(win)
  scroll.appendChild(space)

  const renderWindow = (): void => {
    const viewportHeight = Math.max(240, scroll.clientHeight || 240)
    const start = Math.max(0, Math.floor(scroll.scrollTop / TABLE_ROW_HEIGHT) - 2)
    const count = Math.ceil(viewportHeight / TABLE_ROW_HEIGHT) + 6
    const end = Math.min(rows.length, start + count)
    win.style.transform = `translateY(${start * TABLE_ROW_HEIGHT}px)`
    win.innerHTML = ''

    for (let i = start; i < end; i++) {
      const row = rows[i] || []
      const rowEl = document.createElement('div')
      rowEl.className = `vt-row${focus && focus.rowIndex === i ? ' focus' : ''}`
      rowEl.style.gridTemplateColumns = template
      rowEl.style.height = `${TABLE_ROW_HEIGHT}px`
      rowEl.addEventListener('click', () => {
        activateTableRow(frame, i, row)
      })
      rowEl.addEventListener('dblclick', () => {
        activateTableRow(frame, i, row, true)
      })
      for (let j = 0; j < safeColumns.length; j++) {
        const cell = document.createElement('div')
        cell.className = `vt-cell${focus && focus.rowIndex === i && focus.colIndex === j ? ' focus' : ''}`
        cell.textContent = toCell(row[j])
        rowEl.appendChild(cell)
      }
      win.appendChild(rowEl)
    }
  }

  scroll.addEventListener('scroll', renderWindow)
  const observer = new ResizeObserver(() => renderWindow())
  observer.observe(scroll)
  renderWindow()
  if (focus && focus.rowIndex >= 0) {
    const target = Math.max(0, focus.rowIndex * TABLE_ROW_HEIGHT - Math.max(80, (scroll.clientHeight || 240) / 3))
    requestAnimationFrame(() => {
      scroll.scrollTop = target
      renderWindow()
    })
  }

  container.append(header, scroll)
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
  const edges = graph.edges.filter((edge) => {
    if (filter.kind === 'relGroup') {
      return edgeDisplayGroup(edge) === filter.value
    }
    return edgeSubtype(edge) === filter.value
  })
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

function applyWorkbenchGraphMode(graph: GraphFramePayload, mode: WorkbenchGraphMode): GraphFramePayload {
  if (mode === 'structure') {
    const edges = graph.edges.filter((edge) => {
      const relType = String(edge.relType || edge.properties?.rel_type || '').toUpperCase()
      return relType === 'HAS' || relType === 'EXTEND' || relType === 'INTERFACES'
    })
    if (edges.length === 0) {
      return {
        ...graph,
        nodes: graph.nodes.filter((node) => isClassNode(node)),
        edges: []
      }
    }
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
  const edges = graph.edges.filter((edge) => {
    const relGroup = edgeDisplayGroup(edge)
    const relType = String(edge.relType || edge.properties?.rel_type || '').toUpperCase()
    return relGroup === 'CALL' || relGroup === 'ALIAS' || relType === 'PATH'
  })
  if (edges.length === 0) {
    return {
      ...graph,
      nodes: graph.nodes.filter((node) => isMethodNode(node)),
      edges: []
    }
  }
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

function isClassNode(node: GraphNodePayload): boolean {
  return String(node.kind || '').toLowerCase() === 'class' || node.labels.includes('Class')
}

function isMethodNode(node: GraphNodePayload): boolean {
  return String(node.kind || '').toLowerCase() === 'method' || node.labels.includes('Method')
}

function graphHighlightState(frame: FrameState): {
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
  const activeGraph = frame.graph ? applyWorkbenchGraphMode(frame.graph, state.graphMode) : null
  if (activeGraph) {
    for (const edge of activeGraph.edges) {
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
  renderFrames()
}

function activateLegend(frame: FrameState, kind: 'label' | 'relGroup' | 'relSubtype', value: string): void {
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
  const fragment = buildInspectorFragment(
    kind === 'label' ? 'label' : kind === 'relGroup' ? 'display_rel_type' : 'rel_subtype',
    nextValue
  )
  if (fragment) {
    insertQueryFragment(fragment)
  }
  renderFrames()
}

function activateNeighborhoodFocus(frame: FrameState, node: GraphNodePayload): void {
  const safeLabel = String(node.label || `${node.kind}:${node.id}`).trim()
  if (frame.graphFilter?.kind === 'neighbors' && frame.graphFilter.nodeId === node.id) {
    frame.graphFilter = null
    frame.graphFocus = { nodeIds: [node.id], edgeIds: [] }
    frame.view = 'graph'
    renderFrames()
    return
  }
  const nextNodeIds = new Set<number>([node.id])
  const nextEdgeIds = new Set<number>()
  const activeGraph = frame.graph ? applyWorkbenchGraphMode(frame.graph, state.graphMode) : null
  for (const edge of activeGraph?.edges || []) {
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
  renderFrames()
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
  const selection = resolveRowSelection(frame.graph ? applyWorkbenchGraphMode(frame.graph, state.graphMode) : null, refs)
  if (selection) {
    frame.selection = selection
  }
  if (switchToGraph && frame.graph && refs.nodeIds.length > 0) {
    frame.view = 'graph'
  }
  renderFrames()
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

function edgeDisplayLabel(edge: GraphEdgePayload, mode: GraphRelationMode): string {
  if (mode === 'detail') {
    return displayRelationSubtypeLabel(edgeSubtype(edge))
  }
  return edgeDisplayGroup(edge)
}

function displayRelationSubtypeLabel(subtype: string): string {
  const normalized = String(subtype || '').trim()
  if (!normalized) {
    return ''
  }
  switch (normalized) {
    case 'invoke_dynamic':
      return 'INDY'
    case 'method_handle':
      return 'HANDLE'
    default:
      return normalized.replace(/-/g, '_').toUpperCase()
  }
}

function computeVisibleEdgeLabelIds(
  edges: GraphEdgeSim[],
  edgeGeometry: Map<number, ReturnType<typeof computeEdgeGeometry>>,
  focus: ReturnType<typeof graphHighlightState>,
  hoveredEdgeId: number,
  zoomScale: number,
  relationMode: GraphRelationMode
): Set<number> {
  const out = new Set<number>()
  const accepted: Array<{ left: number; right: number; top: number; bottom: number }> = []
  const relTypeCounts = new Map<string, number>()
  for (const edge of edges) {
    const key = edgeDisplayLabel(edge, relationMode) || '__unknown__'
    relTypeCounts.set(key, (relTypeCounts.get(key) || 0) + 1)
  }
  const baseLimit = zoomScale >= 1.6 ? 28 : zoomScale >= 1.25 ? 18 : zoomScale >= 0.95 ? 12 : 7
  const sorted = [...edges].sort((left, right) => {
    return edgeLabelPriority(right, relTypeCounts, focus, hoveredEdgeId, relationMode)
      - edgeLabelPriority(left, relTypeCounts, focus, hoveredEdgeId, relationMode)
  })
  for (const edge of sorted) {
    const priority = edgeLabelPriority(edge, relTypeCounts, focus, hoveredEdgeId, relationMode)
    const label = truncateTextByWidth(edgeDisplayLabel(edge, relationMode), 120, '700 9px "SF Mono", "JetBrains Mono", "Consolas", monospace')
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
  hoveredEdgeId: number,
  relationMode: GraphRelationMode
): number {
  if (focus.edgeIds.has(edge.id)) {
    return 6
  }
  if (hoveredEdgeId === edge.id) {
    return 5
  }
  const relType = edgeDisplayLabel(edge, relationMode) || '__unknown__'
  const count = relTypeCounts.get(relType) || 1
  if (count === 1) {
    return 4
  }
  if (relType === 'CALL' || relType === 'ALIAS' || relType === 'DIRECT' || relType === 'DISPATCH') {
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
  if (safeKey === 'display_rel_type' || safeKey === 'rel_group') {
    return `ja.relGroup(type(r)) = ${toCypherLiteral(textValue)}`
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
  const scripts = await safeBridgeCall<ScriptListResponse>(CHANNEL_SCRIPT_LIST, {})
  state.scripts = scripts?.items || []
  renderScripts()
}

async function saveScriptFromEditor(): Promise<void> {
  const body = editor.state.doc.toString().trim()
  if (!body) {
    return
  }
  const title = window.prompt(tr('脚本标题', 'Script Title'), deriveScriptTitle(body)) || deriveScriptTitle(body)
  const tags = window.prompt(tr('脚本标签（逗号分隔）', 'Script tags (comma separated)'), '') || ''
  await saveScript({
    scriptId: undefined,
    title,
    body,
    tags,
    pinned: false
  })
}

async function saveScriptFromFrame(frame: FrameState): Promise<void> {
  const title = window.prompt(tr('脚本标题', 'Script Title'), deriveScriptTitle(frame.query)) || deriveScriptTitle(frame.query)
  const tags = window.prompt(tr('脚本标签（逗号分隔）', 'Script tags (comma separated)'), '') || ''
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
  await bridgeCall<ScriptItem>(CHANNEL_SCRIPT_SAVE, payload)
  await refreshScripts()
}

async function deleteScript(scriptId: number): Promise<void> {
  await bridgeCall(CHANNEL_SCRIPT_DELETE, { scriptId })
  await refreshScripts()
}

function applyScriptBody(body: string, execute = false): void {
  setEditorText(body)
  if (execute) {
    void runQuery(body)
  }
}

async function requestFullscreen(fullscreen: boolean): Promise<void> {
  await bridgeCall(CHANNEL_UI_FULLSCREEN, { fullscreen })
  state.fullscreen = fullscreen
  refreshHeaderLabels()
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
  state.selectedFrameId = state.frames[next].frameId
  renderFrames()
}

function selectNextFrame(): void {
  if (state.frames.length === 0) {
    return
  }
  const index = Math.max(0, state.frames.findIndex((item) => item.frameId === state.selectedFrameId))
  const next = index >= state.frames.length - 1 ? 0 : index + 1
  state.selectedFrameId = state.frames[next].frameId
  renderFrames()
}

function updateSimulationVisibility(): void {
  for (const entry of graphSimulations.values()) {
    if (state.hostVisible) {
      entry.simulation.alpha(0.45).restart()
    } else {
      entry.simulation.stop()
    }
  }
}

function stopAllSimulations(): void {
  for (const entry of graphSimulations.values()) {
    entry.simulation.stop()
  }
  graphSimulations.clear()
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

function nodeColor(kind: string): string {
  const safe = (kind || '').toLowerCase()
  if (safe.includes('method')) {
    return '#8fc5ff'
  }
  if (safe.includes('class')) {
    return '#ffb283'
  }
  if (safe.includes('call')) {
    return '#a6e3bc'
  }
  return '#d6dae0'
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
  stopAllSimulations()
  editor.destroy()
})
