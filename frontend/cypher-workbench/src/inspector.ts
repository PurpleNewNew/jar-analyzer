import type { GraphEdgePayload, GraphFramePayload, GraphNodePayload } from './protocol'
import { isRecord, sortCountEntries, toCell } from './view-helpers'

export type FrameSelection =
  | { type: 'node'; data: GraphNodePayload }
  | { type: 'edge'; data: GraphEdgePayload }
  | null

export type GraphFilter = { kind: 'label' | 'relType'; value: string } | null

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
  activateLegend: (kind: 'label' | 'relType', value: string) => void
  activateInspectorLink: (key: string, value: unknown, selected: Exclude<FrameSelection, null>) => void
  clearGraphFilter: () => void
}

export function renderInspector(container: HTMLElement, frame: InspectorFrame, callbacks: InspectorCallbacks): void {
  const { activateInspectorLink, activateLegend, tr } = callbacks
  container.innerHTML = ''
  container.classList.remove('panel-refresh')
  void container.offsetWidth
  container.classList.add('panel-refresh')
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
      appendProp(container, tr('标签数', 'Label Types'), String(stats.labelCounts.length))
      appendProp(container, tr('关系数', 'Rel Types'), String(stats.relTypeCounts.length))
      appendCountSection(container, tr('节点标签', 'Node Labels'), stats.labelCounts, (key) => {
        activateLegend('label', key)
      }, tr)
      appendCountSection(container, tr('关系类型', 'Relationship Types'), stats.relTypeCounts, (key) => {
        activateLegend('relType', key)
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
    appendPropSection(container, tr('摘要', 'Summary'))
    appendLinkedProp(container, selected, activateInspectorLink, 'node_id', selected.data.id, tr)
    appendLinkedProp(container, selected, activateInspectorLink, 'label', selected.data.label, tr)
    appendLinkedProp(container, selected, activateInspectorLink, 'kind', selected.data.kind, tr)
    appendLinkedProp(container, selected, activateInspectorLink, 'class_name', selected.data.className, tr)
    appendLinkedProp(container, selected, activateInspectorLink, 'method_name', selected.data.methodName, tr)
    appendLinkedProp(container, selected, activateInspectorLink, 'method_desc', selected.data.methodDesc, tr)
    appendLinkedProp(container, selected, activateInspectorLink, 'jar_id', selected.data.jarId, tr)
    appendTagSection(container, tr('标签', 'Labels'), selected.data.labels, (label) => {
      activateInspectorLink('label', label, selected)
    }, tr)
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
  return `${filter.kind === 'label' ? tr('标签', 'Label') : tr('关系', 'Rel Type')}: ${filter.value}`
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
  labelCounts: Array<[string, number]>
  relTypeCounts: Array<[string, number]>
} {
  const labelCounter = new Map<string, number>()
  const relCounter = new Map<string, number>()
  for (const node of graph.nodes || []) {
    const labels = Array.isArray(node.labels) && node.labels.length > 0 ? node.labels : [node.kind || 'node']
    labels.forEach((label) => {
      if (!label) {
        return
      }
      labelCounter.set(label, (labelCounter.get(label) || 0) + 1)
    })
  }
  for (const edge of graph.edges || []) {
    const type = edge.relType || 'REL'
    relCounter.set(type, (relCounter.get(type) || 0) + 1)
  }
  return {
    labelCounts: sortCountEntries(labelCounter),
    relTypeCounts: sortCountEntries(relCounter)
  }
}
