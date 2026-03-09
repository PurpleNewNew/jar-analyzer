import './style.css'
import * as d3 from 'd3'
import { EditorState } from '@codemirror/state'
import { EditorView, keymap } from '@codemirror/view'
import { defaultKeymap, history, historyKeymap } from '@codemirror/commands'
import { sql } from '@codemirror/lang-sql'
import { bridgeCall, extractBridgeError, safeBridgeCall } from './bridge'
import { buildGraphFilterBar, renderInspector, type FrameSelection, type GraphFilter } from './inspector'
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
import { buildScriptsCountText, renderScriptList } from './scripts'
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

type FrameViewMode = 'graph' | 'table' | 'text'

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
  errorCode?: string
  errorMessage?: string
}

interface SimRef {
  simulation: d3.Simulation<GraphNodeSim, GraphEdgeSim>
}

type GraphNodeSim = GraphNodePayload & d3.SimulationNodeDatum

type GraphEdgeSim = d3.SimulationLinkDatum<GraphNodeSim> & GraphEdgePayload

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
  queryOptions: { ...DEFAULT_QUERY_OPTIONS } as QueryUiOptions
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
  scriptsCountEl.textContent = buildScriptsCountText(state.scripts.length, tr)
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
  parts.push(`${tr('策略', 'profile')}: ${state.queryOptions.profile}`)
  return parts.join(' · ')
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

async function runQuery(queryInput?: string): Promise<void> {
  const query = (queryInput ?? editor.state.doc.toString()).trim()
  if (!query) {
    return
  }
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
  const query = editor.state.doc.toString().trim()
  if (!query) {
    return
  }
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
      graphFilter: null
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
    graphFilter: null
  }
}

function normalizeGraph(graph?: GraphFramePayload): GraphFramePayload | null {
  if (!graph) {
    return null
  }
  const nodes = Array.isArray(graph.nodes)
    ? graph.nodes.map((item) => ({
        ...item,
        labels: Array.isArray(item?.labels) ? item.labels : [],
        properties: isRecord(item?.properties) ? item.properties : {}
      }))
    : []
  const edges = Array.isArray(graph.edges)
    ? graph.edges.map((item) => ({
        ...item,
        properties: isRecord(item?.properties) ? item.properties : {}
      }))
    : []
  return {
    nodes,
    edges,
    warnings: Array.isArray(graph.warnings) ? graph.warnings : [],
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

  const graphEnabled = !!frame.graph && frame.graph.nodes.length > 0
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

  if (frame.view === 'graph' && graphEnabled && frame.graph) {
    renderGraphView(graphView, frame)
  } else {
    graphView.innerHTML = `<div class="empty">${tr('该结果不可图形化', 'Graph unavailable for this result')}</div>`
  }

  renderVirtualTable(tableView, frame)
  renderTextView(textView, frame)

  content.append(graphView, tableView, textView)

  const propPanel = document.createElement('aside')
  propPanel.className = 'prop-panel'
  renderInspector(propPanel, frame, {
    tr,
    activateLegend: (kind, value) => {
      activateLegend(frame, kind, value)
    },
    activateInspectorLink: (key, value, selected) => {
      activateInspectorLink(frame, key, value, selected)
    },
    clearGraphFilter: () => {
      frame.graphFilter = null
      renderFrames()
    }
  })

  body.append(tabs, content, propPanel)
  return body
}

function renderGraphView(container: HTMLElement, frame: FrameState): void {
  if (!frame.graph) {
    container.innerHTML = `<div class="empty">${tr('无图数据', 'No graph data')}</div>`
    return
  }
  const filtered = applyGraphFilter(frame.graph, frame.graphFilter)
  const nodes = filtered.nodes
  const edges = filtered.edges
  if (nodes.length === 0) {
    container.innerHTML = `<div class="empty">${tr('无图数据', 'No graph data')}</div>`
    return
  }
  container.innerHTML = ''
  const width = Math.max(360, container.clientWidth || 720)
  const height = Math.max(260, container.clientHeight || 420)

  const svg = d3
    .select(container)
    .append('svg')
    .attr('viewBox', `0 0 ${width} ${height}`)

  const edgeLayer = svg.append('g')
  const haloLayer = svg.append('g')
  const nodeLayer = svg.append('g')
  const textLayer = svg.append('g')

  const simNodes: GraphNodeSim[] = nodes.map((node) => ({ ...node }))
  const simEdges: GraphEdgeSim[] = edges.map((edge) => ({ ...edge }))

  const simulation = d3
    .forceSimulation<GraphNodeSim>(simNodes)
    .force('charge', d3.forceManyBody().strength(-220))
    .force('center', d3.forceCenter(width / 2, height / 2))
    .force('collision', d3.forceCollide<GraphNodeSim>().radius((item) => nodeRadius(item) + 6))
    .force(
      'link',
      d3
        .forceLink<GraphNodeSim, GraphEdgeSim>(simEdges)
        .id((item) => String(item.id))
        .distance(96)
        .strength(0.45)
    )

  const links = edgeLayer
    .selectAll('line')
    .data(simEdges)
    .enter()
    .append('line')
    .attr('stroke', '#8e98a6')
    .attr('stroke-width', 1.4)
    .attr('opacity', 0.9)

  const focus = graphHighlightState(frame)
  const halos = haloLayer
    .selectAll('circle')
    .data(simNodes.filter((item) => focus.nodeIds.has(item.id)))
    .enter()
    .append('circle')
    .attr('r', (item) => nodeRadius(item) + 8)
    .attr('fill', 'rgba(47, 129, 247, 0.16)')
    .attr('stroke', 'rgba(47, 129, 247, 0.45)')
    .attr('stroke-width', 1.4)
    .attr('class', 'graph-halo')
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
    .attr('opacity', (item) => (focus.active && !focus.nodeIds.has(item.id) && !focus.edgeNodeIds.has(item.id) ? 0.42 : 1))
    .style('cursor', 'pointer')
    .on('click', (_, item) => {
      frame.selection = { type: 'node', data: item }
      frame.graphFocus = { nodeIds: [item.id], edgeIds: [] }
      renderFrames()
    })
    .call(drag(simulation))

  const labels = textLayer
    .selectAll('text')
    .data(simNodes)
    .enter()
    .append('text')
    .text((item) => item.label)
    .attr('font-size', 10)
    .attr('fill', '#1f2933')
    .attr('opacity', (item) => (focus.active && (focus.nodeIds.has(item.id) || focus.edgeNodeIds.has(item.id)) ? 1 : 0.72))
    .attr('text-anchor', 'middle')
    .attr('pointer-events', 'none')

  const edgeLabels = textLayer
    .selectAll('text.edge')
    .data(simEdges)
    .enter()
    .append('text')
    .text((item) => item.relType || '')
    .attr('font-size', 9)
    .attr('fill', '#5f6b78')
    .attr('opacity', (item) => (focus.edgeIds.has(item.id) ? 1 : 0.75))
    .attr('pointer-events', 'none')

  links.on('click', (_, item) => {
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

  simulation.on('tick', () => {
    links
      .attr('x1', (item) => (item.source as GraphNodeSim).x ?? 0)
      .attr('y1', (item) => (item.source as GraphNodeSim).y ?? 0)
      .attr('x2', (item) => (item.target as GraphNodeSim).x ?? 0)
      .attr('y2', (item) => (item.target as GraphNodeSim).y ?? 0)
      .attr('stroke', (item) => (focus.edgeIds.has(item.id) ? '#2f81f7' : '#8e98a6'))
      .attr('stroke-width', (item) => (focus.edgeIds.has(item.id) ? 2.8 : 1.4))
      .attr('opacity', (item) => (focus.active && !focus.edgeIds.has(item.id) ? 0.32 : 0.9))

    halos.attr('cx', (item) => item.x ?? 0).attr('cy', (item) => item.y ?? 0)

    nodesSel.attr('cx', (item) => item.x ?? 0).attr('cy', (item) => item.y ?? 0)

    labels.attr('x', (item) => item.x ?? 0).attr('y', (item) => (item.y ?? 0) + 3)

    edgeLabels
      .attr('x', (item) => (((item.source as GraphNodeSim).x ?? 0) + ((item.target as GraphNodeSim).x ?? 0)) / 2)
      .attr('y', (item) => (((item.source as GraphNodeSim).y ?? 0) + ((item.target as GraphNodeSim).y ?? 0)) / 2)
  })

  graphSimulations.set(frame.frameId, { simulation })
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
  const edges = graph.edges.filter((edge) => edge.relType === filter.value)
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
  if (frame.graph) {
    for (const edge of frame.graph.edges) {
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

function activateLegend(frame: FrameState, kind: 'label' | 'relType', value: string): void {
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
  const fragment = buildInspectorFragment(kind === 'label' ? 'label' : 'rel_type', nextValue)
  if (fragment) {
    insertQueryFragment(fragment)
  }
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
  const selection = resolveRowSelection(frame.graph, refs)
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

function drag(simulation: d3.Simulation<GraphNodeSim, GraphEdgeSim>) {
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
  }

  function dragEnded(event: d3.D3DragEvent<SVGCircleElement, GraphNodeSim, GraphNodeSim>, d: GraphNodeSim): void {
    if (!event.active) {
      simulation.alphaTarget(0)
    }
    d.fx = null
    d.fy = null
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

window.addEventListener('beforeunload', () => {
  stopAllSimulations()
  editor.destroy()
})
