import './style.css'
import * as d3 from 'd3'
import { EditorState } from '@codemirror/state'
import { EditorView, keymap } from '@codemirror/view'
import { defaultKeymap, history, historyKeymap } from '@codemirror/commands'
import { sql } from '@codemirror/lang-sql'

declare global {
  interface Window {
    cefQuery?: (args: {
      request: string
      persistent?: boolean
      onSuccess: (resp: string) => void
      onFailure: (code: number, message: string) => void
    }) => void
    JA_WORKBENCH?: {
      updateUiContext: (ctx: UiContext) => void
      setFullscreen: (fullscreen: boolean) => void
      onHostVisibility: (visible: boolean) => void
    }
    __JA_BOOT_ERROR?: string
  }
}

const CHANNEL_QUERY_EXECUTE = 'ja.query.execute'
const CHANNEL_QUERY_EXPLAIN = 'ja.query.explain'
const CHANNEL_QUERY_CAPABILITIES = 'ja.query.capabilities'
const CHANNEL_GRAPH_PROJECT = 'ja.graph.project'
const CHANNEL_SCRIPT_LIST = 'ja.script.list'
const CHANNEL_SCRIPT_SAVE = 'ja.script.save'
const CHANNEL_SCRIPT_DELETE = 'ja.script.delete'
const CHANNEL_UI_CONTEXT = 'ja.ui.context'
const CHANNEL_UI_FULLSCREEN = 'ja.ui.fullscreen'

const MAX_FRAMES = 50
const TABLE_ROW_HEIGHT = 26

interface UiContext {
  language: 'zh' | 'en'
  theme: 'default' | 'dark'
}

interface BridgeEnvelope<T> {
  ok: boolean
  channel: string
  data?: T
  code?: string
  message?: string
}

interface QueryFrameRequestResult {
  ok: boolean
  code: string
  message: string
  frame?: QueryFramePayload
}

interface QueryFramePayload {
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

interface GraphFramePayload {
  nodes: GraphNodePayload[]
  edges: GraphEdgePayload[]
  warnings: string[]
  truncated: boolean
}

interface GraphNodePayload {
  id: number
  label: string
  kind: string
  jarId: number
  className: string
  methodName: string
  methodDesc: string
}

interface GraphEdgePayload {
  id: number
  source: number
  target: number
  relType: string
  confidence: string
  evidence: string
}

interface ScriptListResponse {
  items: ScriptItem[]
}

interface ScriptItem {
  scriptId: number
  title: string
  body: string
  tags: string
  pinned: boolean
  createdAt: number
  updatedAt: number
}

type FrameViewMode = 'graph' | 'table' | 'text'

type FrameSelection =
  | { type: 'node'; data: GraphNodePayload }
  | { type: 'edge'; data: GraphEdgePayload }
  | null

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
  capabilities: null as Record<string, unknown> | null
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
      <div class="wb-title" id="title"></div>
      <div class="wb-actions">
        <button class="wb-btn" id="btn-save" title="Save Script">☆</button>
        <button class="wb-btn" id="btn-refresh" title="Refresh Scripts">↻</button>
        <button class="wb-btn" id="btn-explain" title="Explain">Explain</button>
        <button class="wb-btn" id="btn-fullscreen" title="Fullscreen">⤢</button>
      </div>
    </header>
    <section class="command-bar">
      <div class="prompt">$</div>
      <div class="editor-wrap"><div id="editor"></div></div>
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
  const capabilities = await safeBridgeCall<Record<string, unknown>>(CHANNEL_QUERY_CAPABILITIES, {})
  if (capabilities) {
    state.capabilities = capabilities
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
  renderFrames()
}

function refreshHeaderLabels(): void {
  titleEl.textContent = 'Graph Console'
  scriptsTitleEl.textContent = tr('脚本收藏', 'Scripts')
  scriptsCountEl.textContent = `${state.scripts.length}`
  runButton.textContent = tr('运行', 'Run')
  explainButton.textContent = tr('解释', 'Explain')
  refreshButton.textContent = tr('刷新', 'Refresh')
  saveButton.textContent = tr('收藏', 'Save')
  fullscreenButton.textContent = state.fullscreen ? '⤡' : '⤢'
  fullscreenButton.title = state.fullscreen ? tr('退出全屏', 'Exit Fullscreen') : tr('全屏', 'Fullscreen')
}

async function runQuery(queryInput?: string): Promise<void> {
  const query = (queryInput ?? editor.state.doc.toString()).trim()
  if (!query) {
    return
  }
  runButton.disabled = true
  try {
    const response = await bridgeCall<QueryFrameRequestResult>(CHANNEL_QUERY_EXECUTE, {
      query,
      params: {},
      options: {
        maxRows: 500,
        maxMs: 15000,
        maxHops: 32,
        maxPaths: 500,
        profile: 'default'
      }
    })
    if (!response.ok || !response.frame) {
      pushFrame(buildErrorFrame(query, response.code || 'cypher_query_error', response.message || 'query failed'))
      return
    }
    pushFrame(toFrameState(response.frame))
  } catch (error) {
    pushFrame(buildErrorFrame(query, 'bridge_error', toErrorMessage(error)))
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
      selection: null
    })
  } catch (error) {
    pushFrame(buildErrorFrame(query, 'cypher_explain_error', toErrorMessage(error)))
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
    view: graph && graph.nodes.length > 0 && graph.edges.length > 0 ? 'graph' : 'table',
    selection: null
  }
}

function normalizeGraph(graph?: GraphFramePayload): GraphFramePayload | null {
  if (!graph) {
    return null
  }
  const nodes = Array.isArray(graph.nodes) ? graph.nodes : []
  const edges = Array.isArray(graph.edges) ? graph.edges : []
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
  scriptListEl.innerHTML = ''
  scriptsCountEl.textContent = `${state.scripts.length}`
  if (state.scripts.length === 0) {
    const empty = document.createElement('div')
    empty.className = 'empty'
    empty.textContent = tr('暂无脚本', 'No scripts')
    scriptListEl.appendChild(empty)
    return
  }

  for (const item of state.scripts) {
    const row = document.createElement('div')
    row.className = 'script-item'

    const title = document.createElement('div')
    title.className = 'script-title'
    title.textContent = item.title || tr('未命名脚本', 'Untitled Script')
    title.title = item.body
    title.addEventListener('dblclick', () => {
      setEditorText(item.body)
      void runQuery(item.body)
    })
    title.addEventListener('click', () => {
      setEditorText(item.body)
    })

    const actions = document.createElement('div')
    actions.className = 'script-actions'

    const pinBtn = document.createElement('button')
    pinBtn.className = 'script-btn'
    pinBtn.textContent = item.pinned ? '★' : '☆'
    pinBtn.title = item.pinned ? tr('取消置顶', 'Unpin') : tr('置顶', 'Pin')
    pinBtn.addEventListener('click', () => {
      void saveScript({
        scriptId: item.scriptId,
        title: item.title,
        body: item.body,
        tags: item.tags,
        pinned: !item.pinned
      })
    })

    const delBtn = document.createElement('button')
    delBtn.className = 'script-btn'
    delBtn.textContent = '×'
    delBtn.title = tr('删除', 'Delete')
    delBtn.addEventListener('click', () => {
      void deleteScript(item.scriptId)
    })

    actions.appendChild(pinBtn)
    actions.appendChild(delBtn)

    const meta = document.createElement('div')
    meta.className = 'script-meta'
    meta.textContent = `${item.tags || '-'} · ${formatTime(item.updatedAt)}`

    row.appendChild(title)
    row.appendChild(actions)
    row.appendChild(meta)
    scriptListEl.appendChild(row)
  }
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

    const full = createFrameButton('⤢', tr('全屏', 'Fullscreen'))
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

  const graphEnabled = !!frame.graph && frame.graph.nodes.length > 0 && frame.graph.edges.length > 0
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

  renderVirtualTable(tableView, frame.columns, frame.rows)
  renderTextView(textView, frame)

  content.append(graphView, tableView, textView)

  const propPanel = document.createElement('aside')
  propPanel.className = 'prop-panel'
  renderProperties(propPanel, frame)

  body.append(tabs, content, propPanel)
  return body
}

function renderGraphView(container: HTMLElement, frame: FrameState): void {
  if (!frame.graph) {
    container.innerHTML = `<div class="empty">${tr('无图数据', 'No graph data')}</div>`
    return
  }
  const { nodes, edges } = frame.graph
  if (nodes.length === 0 || edges.length === 0) {
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

  const nodesSel = nodeLayer
    .selectAll('circle')
    .data(simNodes)
    .enter()
    .append('circle')
    .attr('r', (item) => nodeRadius(item))
    .attr('fill', (item) => nodeColor(item.kind))
    .attr('stroke', '#2f3640')
    .attr('stroke-width', 1.1)
    .style('cursor', 'pointer')
    .on('click', (_, item) => {
      frame.selection = { type: 'node', data: item }
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
        evidence: item.evidence
      }
    }
    renderFrames()
  })

  simulation.on('tick', () => {
    links
      .attr('x1', (item) => (item.source as GraphNodeSim).x ?? 0)
      .attr('y1', (item) => (item.source as GraphNodeSim).y ?? 0)
      .attr('x2', (item) => (item.target as GraphNodeSim).x ?? 0)
      .attr('y2', (item) => (item.target as GraphNodeSim).y ?? 0)

    nodesSel.attr('cx', (item) => item.x ?? 0).attr('cy', (item) => item.y ?? 0)

    labels.attr('x', (item) => item.x ?? 0).attr('y', (item) => (item.y ?? 0) + 3)

    edgeLabels
      .attr('x', (item) => (((item.source as GraphNodeSim).x ?? 0) + ((item.target as GraphNodeSim).x ?? 0)) / 2)
      .attr('y', (item) => (((item.source as GraphNodeSim).y ?? 0) + ((item.target as GraphNodeSim).y ?? 0)) / 2)
  })

  graphSimulations.set(frame.frameId, { simulation })
}

function renderVirtualTable(container: HTMLElement, columns: string[], rows: unknown[][]): void {
  container.innerHTML = ''
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
      rowEl.className = 'vt-row'
      rowEl.style.gridTemplateColumns = template
      rowEl.style.height = `${TABLE_ROW_HEIGHT}px`
      for (let j = 0; j < safeColumns.length; j++) {
        const cell = document.createElement('div')
        cell.className = 'vt-cell'
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

function renderProperties(container: HTMLElement, frame: FrameState): void {
  container.innerHTML = ''
  const title = document.createElement('div')
  title.className = 'prop-title'
  container.appendChild(title)

  const selected = frame.selection
  if (!selected) {
    title.textContent = tr('结果概览', 'Overview')
    appendProp(container, tr('查询', 'Query'), frame.query)
    appendProp(container, tr('列数', 'Columns'), String(frame.columns.length))
    appendProp(container, tr('行数', 'Rows'), String(frame.rows.length))
    if (frame.graph) {
      appendProp(container, tr('图节点', 'Graph Nodes'), String(frame.graph.nodes.length))
      appendProp(container, tr('图边', 'Graph Edges'), String(frame.graph.edges.length))
    }
    appendProp(container, tr('耗时', 'Elapsed'), `${frame.elapsedMs}ms`)
    return
  }

  if (selected.type === 'node') {
    title.textContent = tr('节点属性', 'Node Properties')
    appendProp(container, 'id', String(selected.data.id))
    appendProp(container, 'label', selected.data.label)
    appendProp(container, 'kind', selected.data.kind)
    appendProp(container, 'class', selected.data.className)
    appendProp(container, 'method', selected.data.methodName)
    appendProp(container, 'desc', selected.data.methodDesc)
    appendProp(container, 'jar', String(selected.data.jarId))
    return
  }

  title.textContent = tr('边属性', 'Edge Properties')
  appendProp(container, 'id', String(selected.data.id))
  appendProp(container, 'source', String(selected.data.source))
  appendProp(container, 'target', String(selected.data.target))
  appendProp(container, 'rel_type', selected.data.relType)
  appendProp(container, 'confidence', selected.data.confidence)
  appendProp(container, 'evidence', selected.data.evidence)
}

function appendProp(container: HTMLElement, key: string, value: string): void {
  const row = document.createElement('div')
  row.className = 'prop-item'
  const k = document.createElement('div')
  k.className = 'prop-key'
  k.textContent = key
  const v = document.createElement('div')
  v.className = 'prop-value'
  v.textContent = value || '-'
  row.append(k, v)
  container.appendChild(row)
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

async function bridgeCall<T = unknown>(channel: string, payload: unknown): Promise<T> {
  if (typeof window.cefQuery !== 'function') {
    throw new Error('jcef bridge unavailable')
  }
  const request = JSON.stringify({ channel, payload })
  return new Promise<T>((resolve, reject) => {
    window.cefQuery?.({
      request,
      persistent: false,
      onSuccess: (resp) => {
        try {
          const envelope = JSON.parse(resp) as BridgeEnvelope<T>
          if (!envelope.ok) {
            reject(new Error(`${envelope.code || 'bridge_error'}: ${envelope.message || ''}`))
            return
          }
          resolve(envelope.data as T)
        } catch (error) {
          reject(error)
        }
      },
      onFailure: (code, message) => {
        reject(new Error(`bridge_failure_${code}: ${message}`))
      }
    })
  })
}

async function safeBridgeCall<T>(channel: string, payload: unknown): Promise<T | null> {
  try {
    return await bridgeCall<T>(channel, payload)
  } catch {
    return null
  }
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

function formatTime(seconds: number): string {
  if (!seconds || seconds <= 0) {
    return '-'
  }
  const date = new Date(seconds * 1000)
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  const hh = String(date.getHours()).padStart(2, '0')
  const mm = String(date.getMinutes()).padStart(2, '0')
  return `${y}-${m}-${d} ${hh}:${mm}`
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

function toCell(value: unknown): string {
  if (value == null) {
    return ''
  }
  if (typeof value === 'string') {
    return value
  }
  if (typeof value === 'number' || typeof value === 'boolean') {
    return String(value)
  }
  try {
    return JSON.stringify(value)
  } catch {
    return String(value)
  }
}

function toErrorMessage(error: unknown): string {
  if (error instanceof Error && error.message) {
    return error.message
  }
  return String(error)
}

window.addEventListener('beforeunload', () => {
  stopAllSimulations()
  editor.destroy()
})

// keep channels referenced in build artifacts for protocol visibility
void CHANNEL_GRAPH_PROJECT
