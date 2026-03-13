import type { GraphEdgePayload, GraphFramePayload, GraphNodePayload } from './protocol'
import { isRecord, sortCountEntries, toCell } from './view-helpers'

export type FrameSelection =
  | { type: 'node'; data: GraphNodePayload }
  | { type: 'edge'; data: GraphEdgePayload }
  | null

export type GraphFilter =
  | { kind: 'label'; value: string }
  | { kind: 'relGroup'; value: string }
  | { kind: 'neighbors'; nodeId: number; label: string; hops: 1 }
  | null

interface InspectorFrame {
  query: string
  columns: string[]
  rows: unknown[][]
  warnings: string[]
  truncated: boolean
  elapsedMs: number
  graph: GraphFramePayload | null
  selection: FrameSelection
  graphFilter: GraphFilter
}

interface InspectorCallbacks {
  tr: (zh: string, en: string) => string
  activateLegend: (kind: 'label' | 'relGroup', value: string) => void
  activateInspectorLink: (key: string, value: unknown, selected: Exclude<FrameSelection, null>) => void
  activateNeighborhoodFocus: (node: GraphNodePayload) => void
  clearGraphFilter: () => void
}

export function renderInspector(container: HTMLElement, frame: InspectorFrame, callbacks: InspectorCallbacks): void {
  const { activateInspectorLink, activateLegend, activateNeighborhoodFocus, clearGraphFilter, tr } = callbacks
  container.innerHTML = ''
  const title = document.createElement('div')
  title.className = 'prop-title'
  container.appendChild(title)
  const subtitle = document.createElement('div')
  subtitle.className = 'prop-subtitle'
  container.appendChild(subtitle)

  const selected = frame.selection
  if (!selected) {
    title.textContent = tr('结果概览', 'Overview')
    subtitle.textContent = frame.query
    appendPropSection(container, tr('执行摘要', 'Execution'))
    appendProp(container, tr('耗时', 'Elapsed'), `${frame.elapsedMs}ms`)
    appendProp(container, tr('列数', 'Columns'), String(frame.columns.length))
    appendProp(container, tr('行数', 'Rows'), String(frame.rows.length))
    appendProp(container, tr('结果截断', 'Truncated'), frame.truncated ? tr('是', 'yes') : tr('否', 'no'))
    if (frame.graph) {
      const stats = graphOverviewStats(frame.graph)
      appendPropSection(container, tr('图概览', 'Graph Overview'))
      appendProp(container, tr('图节点', 'Graph Nodes'), String(frame.graph.nodes.length))
      appendProp(container, tr('图边', 'Graph Edges'), String(frame.graph.edges.length))
      appendProp(container, tr('结构标签数', 'Structure Labels'), String(stats.structureLabelCounts.length))
      appendProp(container, tr('语义摘要数', 'Semantic Badges'), String(stats.semanticCounts.length))
      appendProp(container, tr('关系类别数', 'Rel Groups'), String(stats.relGroupCounts.length))
      appendCountSection(container, tr('结构标签', 'Structure Labels'), stats.structureLabelCounts, (key) => {
        activateLegend('label', key)
      }, tr)
      appendCountSection(container, tr('节点语义', 'Node Semantics'), stats.semanticCounts, undefined, tr)
      appendCountSection(container, tr('关系类别', 'Relationship Groups'), stats.relGroupCounts, (key) => {
        activateLegend('relGroup', key)
      }, tr)
    }
    if ((frame.graph?.warnings || frame.warnings).length > 0) {
      appendTextListSection(container, tr('提示', 'Warnings'), frame.graph?.warnings || frame.warnings)
    }
    return
  }

  if (selected.type === 'node') {
    title.textContent = tr('节点详情', 'Node Details')
    subtitle.textContent = selected.data.label
    const neighborhood = frame.graph ? graphNeighborhood(frame.graph, selected.data.id) : null
    appendPropSection(container, tr('局部探索', 'Local Explore'))
    appendActionRow(
      container,
      [
        {
          label:
            frame.graphFilter?.kind === 'neighbors' && frame.graphFilter.nodeId === selected.data.id
              ? tr('恢复全图', 'Reset Graph')
              : tr('一跳邻居聚焦', 'Focus 1-Hop'),
          title: tr('在当前结果图中仅保留该节点及一跳邻居', 'Keep only this node and its 1-hop neighbors in the current result graph'),
          onClick: () => {
            if (frame.graphFilter?.kind === 'neighbors' && frame.graphFilter.nodeId === selected.data.id) {
              clearGraphFilter()
              return
            }
            activateNeighborhoodFocus(selected.data)
          }
        }
      ]
    )
    if (neighborhood) {
      appendProp(container, tr('邻居节点', 'Neighbor Nodes'), String(neighborhood.neighborCount))
      appendProp(container, tr('出边', 'Outgoing Edges'), String(neighborhood.outgoingCount))
      appendProp(container, tr('入边', 'Incoming Edges'), String(neighborhood.incomingCount))
    }
    appendPropSection(container, tr('摘要', 'Summary'))
    appendLinkedProp(container, selected, activateInspectorLink, 'node_id', selected.data.id, tr)
    appendLinkedProp(container, selected, activateInspectorLink, 'label', selected.data.label, tr)
    appendLinkedProp(container, selected, activateInspectorLink, 'kind', selected.data.kind, tr)
    appendLinkedProp(container, selected, activateInspectorLink, 'class_name', selected.data.className, tr)
    appendLinkedProp(container, selected, activateInspectorLink, 'method_name', selected.data.methodName, tr)
    appendLinkedProp(container, selected, activateInspectorLink, 'method_desc', selected.data.methodDesc, tr)
    appendLinkedProp(container, selected, activateInspectorLink, 'jar_id', selected.data.jarId, tr)
    appendLinkedProp(container, selected, activateInspectorLink, 'source_flags', selected.data.properties?.source_flags, tr)
    appendLinkedProp(container, selected, activateInspectorLink, 'source_flags_effective', selected.data.properties?.source_flags_effective, tr)
    appendLinkedProp(container, selected, activateInspectorLink, 'is_source', selected.data.properties?.is_source, tr)
    appendLinkedProp(container, selected, activateInspectorLink, 'is_sink', selected.data.properties?.is_sink, tr)
    appendLinkedProp(container, selected, activateInspectorLink, 'sink_kind', selected.data.properties?.sink_kind, tr)
    appendTagSection(container, tr('标签', 'Labels'), selected.data.labels, (label) => {
      activateInspectorLink('label', label, selected)
    }, tr)
    appendTagSection(container, tr('当前语义', 'Current Semantics'), nodeSemanticBadges(selected.data), undefined, tr)
    appendMapSection(container, tr('属性', 'Properties'), selected.data.properties, (key, value) => {
      activateInspectorLink(key, value, selected)
    }, tr)
    appendJsonSection(container, tr('原始节点', 'Raw Node'), selected.data)
    return
  }

  title.textContent = tr('边详情', 'Edge Details')
  subtitle.textContent = `${selected.data.source} -> ${selected.data.target}`
  appendPropSection(container, tr('摘要', 'Summary'))
  appendLinkedProp(container, selected, activateInspectorLink, 'edge_id', selected.data.id, tr)
  appendLinkedProp(container, selected, activateInspectorLink, 'source', selected.data.source, tr)
  appendLinkedProp(container, selected, activateInspectorLink, 'target', selected.data.target, tr)
  appendLinkedProp(container, selected, activateInspectorLink, 'display_rel_type', edgeDisplayGroup(selected.data), tr)
  appendLinkedProp(container, selected, activateInspectorLink, 'rel_subtype', edgeSubtype(selected.data), tr)
  appendLinkedProp(container, selected, activateInspectorLink, 'alias_kind', selected.data.properties?.alias_kind, tr)
  appendLinkedProp(container, selected, activateInspectorLink, 'rel_type', selected.data.relType, tr)
  appendLinkedProp(container, selected, activateInspectorLink, 'confidence', selected.data.confidence, tr)
  appendLinkedProp(container, selected, activateInspectorLink, 'evidence', selected.data.evidence, tr)
  appendMapSection(container, tr('属性', 'Properties'), selected.data.properties, (key, value) => {
    activateInspectorLink(key, value, selected)
  }, tr)
  appendJsonSection(container, tr('原始边', 'Raw Edge'), selected.data)
}

export function buildGraphFilterBar(
  frame: Pick<InspectorFrame, 'graphFilter'>,
  callbacks: Pick<InspectorCallbacks, 'clearGraphFilter' | 'tr'>
): HTMLElement {
  const { clearGraphFilter, tr } = callbacks
  const bar = document.createElement('div')
  bar.className = 'frame-filter-bar'

  const title = document.createElement('span')
  title.className = 'frame-filter-title'
  title.textContent = tr('图筛选', 'Graph Filter')

  const chips = document.createElement('div')
  chips.className = 'filter-chip-list'
  chips.appendChild(buildGraphFilterChip(frame.graphFilter, clearGraphFilter, tr))

  bar.append(title, chips)
  return bar
}

function buildGraphFilterChip(
  filter: GraphFilter,
  clearGraphFilter: InspectorCallbacks['clearGraphFilter'],
  tr: InspectorCallbacks['tr']
): HTMLElement {
  const chip = document.createElement('span')
  chip.className = 'filter-chip'

  const label = document.createElement('span')
  label.className = 'filter-chip-label'
  label.textContent = formatGraphFilter(filter, tr)

  const clear = document.createElement('button')
  clear.className = 'filter-chip-clear'
  clear.type = 'button'
  clear.textContent = '×'
  clear.title = tr('清除筛选', 'Clear filter')
  clear.addEventListener('click', (event) => {
    event.stopPropagation()
    clearGraphFilter()
  })

  chip.append(label, clear)
  return chip
}

function formatGraphFilter(filter: GraphFilter, tr: InspectorCallbacks['tr']): string {
  if (!filter) {
    return ''
  }
  if (filter.kind === 'neighbors') {
    return `${tr('一跳邻居', '1-Hop')}: ${filter.label}`
  }
  if (filter.kind === 'label') {
    return `${tr('标签', 'Label')}: ${filter.value}`
  }
  if (filter.kind === 'relGroup') {
    return `${tr('关系类别', 'Rel Group')}: ${filter.value}`
  }
  return ''
}

function appendActionRow(
  container: HTMLElement,
  actions: Array<{ label: string; title: string; onClick: () => void }>
): void {
  if (actions.length === 0) {
    return
  }
  const row = document.createElement('div')
  row.className = 'prop-action-row'
  for (const action of actions) {
    const button = document.createElement('button')
    button.type = 'button'
    button.className = 'prop-action-btn'
    button.textContent = action.label
    button.title = action.title
    button.addEventListener('click', action.onClick)
    row.appendChild(button)
  }
  container.appendChild(row)
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

function appendLinkedProp(
  container: HTMLElement,
  selected: Exclude<FrameSelection, null>,
  activateInspectorLink: InspectorCallbacks['activateInspectorLink'],
  key: string,
  value: unknown,
  tr: InspectorCallbacks['tr']
): void {
  const text = toCell(value)
  if (!text) {
    return
  }
  const row = document.createElement('div')
  row.className = 'prop-item prop-item-link'
  row.title = tr('点击定位表格并插入查询片段', 'Click to locate table row and insert query fragment')
  row.addEventListener('click', () => {
    activateInspectorLink(key, value, selected)
  })
  const k = document.createElement('div')
  k.className = 'prop-key'
  k.textContent = key
  const v = document.createElement('div')
  v.className = 'prop-value'
  v.textContent = text
  row.append(k, v)
  container.appendChild(row)
}

function appendPropSection(container: HTMLElement, title: string): void {
  const section = document.createElement('div')
  section.className = 'prop-section-title'
  section.textContent = title
  container.appendChild(section)
}

function appendTagSection(
  container: HTMLElement,
  title: string,
  values: string[],
  onClick: ((value: string) => void) | undefined,
  tr: InspectorCallbacks['tr']
): void {
  if (!values || values.length === 0) {
    return
  }
  appendPropSection(container, title)
  const wrap = document.createElement('div')
  wrap.className = 'prop-chip-list'
  for (const value of values) {
    const chip = document.createElement('span')
    chip.className = `prop-chip${onClick ? ' prop-chip-action' : ''}`
    chip.textContent = value
    if (onClick) {
      chip.title = tr('点击定位表格并插入查询片段', 'Click to locate table row and insert query fragment')
      chip.addEventListener('click', () => onClick(value))
    }
    wrap.appendChild(chip)
  }
  container.appendChild(wrap)
}

function appendMapSection(
  container: HTMLElement,
  title: string,
  value: Record<string, unknown>,
  onClick: ((key: string, value: unknown) => void) | undefined,
  tr: InspectorCallbacks['tr']
): void {
  const entries = Object.entries(value || {}).sort(([a], [b]) => a.localeCompare(b))
  if (entries.length === 0) {
    return
  }
  appendPropSection(container, title)
  for (const [key, raw] of entries) {
    if (!onClick) {
      appendProp(container, key, toCell(raw))
      continue
    }
    const row = document.createElement('div')
    row.className = 'prop-item prop-item-link'
    row.title = tr('点击定位表格并插入查询片段', 'Click to locate table row and insert query fragment')
    row.addEventListener('click', () => onClick(key, raw))
    const k = document.createElement('div')
    k.className = 'prop-key'
    k.textContent = key
    const v = document.createElement('div')
    v.className = 'prop-value'
    v.textContent = toCell(raw) || '-'
    row.append(k, v)
    container.appendChild(row)
  }
}

function appendTextListSection(container: HTMLElement, title: string, values: string[]): void {
  if (!values || values.length === 0) {
    return
  }
  appendPropSection(container, title)
  const wrap = document.createElement('div')
  wrap.className = 'prop-list'
  values.forEach((value) => {
    const row = document.createElement('div')
    row.className = 'prop-list-item'
    row.textContent = value
    wrap.appendChild(row)
  })
  container.appendChild(wrap)
}

function appendCountSection(
  container: HTMLElement,
  title: string,
  rows: Array<[string, number]>,
  onClick: ((key: string) => void) | undefined,
  tr: InspectorCallbacks['tr']
): void {
  if (!rows || rows.length === 0) {
    return
  }
  appendPropSection(container, title)
  for (const [key, count] of rows.slice(0, 8)) {
    if (!onClick) {
      appendProp(container, key, String(count))
      continue
    }
    const row = document.createElement('div')
    row.className = 'prop-item prop-item-link'
    row.title = tr('点击定位表格并插入查询片段', 'Click to locate table row and insert query fragment')
    row.addEventListener('click', () => onClick(key))
    const k = document.createElement('div')
    k.className = 'prop-key'
    k.textContent = key
    const v = document.createElement('div')
    v.className = 'prop-value'
    v.textContent = String(count)
    row.append(k, v)
    container.appendChild(row)
  }
}

function appendJsonSection(container: HTMLElement, title: string, value: unknown): void {
  const details = document.createElement('details')
  details.className = 'prop-json'
  const summary = document.createElement('summary')
  summary.textContent = title
  const pre = document.createElement('pre')
  pre.textContent = JSON.stringify(value, null, 2)
  details.append(summary, pre)
  container.appendChild(details)
}

function graphOverviewStats(graph: GraphFramePayload): {
  structureLabelCounts: Array<[string, number]>
  semanticCounts: Array<[string, number]>
  relGroupCounts: Array<[string, number]>
} {
  const labelCounter = new Map<string, number>()
  const semanticCounter = new Map<string, number>()
  const relGroupCounter = new Map<string, number>()
  for (const node of graph.nodes || []) {
    const labels = Array.isArray(node.labels) && node.labels.length > 0 ? node.labels : [node.kind || 'node']
    labels.forEach((label) => {
      if (!label) {
        return
      }
      labelCounter.set(label, (labelCounter.get(label) || 0) + 1)
    })
    for (const badge of nodeSemanticBadges(node)) {
      semanticCounter.set(badge, (semanticCounter.get(badge) || 0) + 1)
    }
  }
  for (const edge of graph.edges || []) {
    const group = edgeDisplayGroup(edge) || 'REL'
    relGroupCounter.set(group, (relGroupCounter.get(group) || 0) + 1)
  }
  return {
    structureLabelCounts: sortCountEntries(labelCounter),
    semanticCounts: sortCountEntries(semanticCounter),
    relGroupCounts: sortCountEntries(relGroupCounter)
  }
}

function graphNeighborhood(
  graph: GraphFramePayload,
  nodeId: number
): {
  neighborCount: number
  incomingCount: number
  outgoingCount: number
} {
  const neighbors = new Set<number>()
  let incomingCount = 0
  let outgoingCount = 0
  for (const edge of graph.edges || []) {
    if (edge.source === nodeId) {
      outgoingCount++
      if (edge.target !== nodeId) {
        neighbors.add(edge.target)
      }
    }
    if (edge.target === nodeId) {
      incomingCount++
      if (edge.source !== nodeId) {
        neighbors.add(edge.source)
      }
    }
  }
  return {
    neighborCount: neighbors.size,
    incomingCount,
    outgoingCount
  }
}

function nodeSemanticBadges(node: GraphNodePayload): string[] {
  const props = isRecord(node?.properties) ? node.properties : {}
  const out = new Set<string>()
  if (props.is_source === true) {
    out.add('Source')
  }
  if (props.is_sink === true) {
    out.add('Sink')
  }
  const sourceBadges = props.source_badges
  if (Array.isArray(sourceBadges)) {
    for (const badge of sourceBadges) {
      const text = toCell(badge)
      if (text && text !== 'Source') {
        out.add(text)
      }
    }
  }
  const sinkKind = toCell(props.sink_kind)
  if (sinkKind) {
    out.add(`Sink:${sinkKind}`)
  }
  return Array.from(out)
}

export function edgeDisplayGroup(edge: GraphEdgePayload): string {
  const props = isRecord(edge?.properties) ? edge.properties : {}
  return toCell(props.display_rel_type)
}

export function edgeSubtype(edge: GraphEdgePayload): string {
  const props = isRecord(edge?.properties) ? edge.properties : {}
  return toCell(props.rel_subtype)
}
