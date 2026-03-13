import type { ScriptItem } from './protocol'
import { formatTime } from './view-helpers'

export interface BuiltinScript {
  titleZh: string
  titleEn: string
  tagsZh: string
  tagsEn: string
  body: string
}

export const BUILTIN_SCRIPTS: BuiltinScript[] = [
  {
    titleZh: '浏览方法调用',
    titleEn: 'Browse Calls',
    tagsZh: 'graph,call',
    tagsEn: 'graph,call',
    body: 'MATCH (m:Method)-[r:CALL]->(n:Method) RETURN m,r,n LIMIT 50'
  },
  {
    titleZh: '浏览方法节点',
    titleEn: 'Browse Methods',
    tagsZh: 'graph,node',
    tagsEn: 'graph,node',
    body: 'MATCH (m:Method) RETURN m LIMIT 50'
  },
  {
    titleZh: '查看 Alias 关系',
    titleEn: 'Browse Alias Edges',
    tagsZh: 'graph,alias',
    tagsEn: 'graph,alias',
    body: 'MATCH (m:Method)-[r:ALIAS]->(n:Method) RETURN m,r,n LIMIT 50'
  },
  {
    titleZh: '最短路径示例',
    titleEn: 'Shortest Path Example',
    tagsZh: 'ja,path',
    tagsEn: 'ja,path',
    body: 'CALL ja.path.shortest("node:1", "node:2", 6, {{TRAVERSAL_MODE_LITERAL}}) YIELD path_id, hop, node_ids, edge_ids, score, confidence, evidence RETURN *'
  },
  {
    titleZh: '查看 Sources',
    titleEn: 'Find Sources',
    tagsZh: 'rule,source',
    tagsEn: 'rule,source',
    body: 'MATCH (n:Method) WHERE ja.isSource(n) RETURN n LIMIT 50'
  },
  {
    titleZh: '查看 Sinks',
    titleEn: 'Find Sinks',
    tagsZh: 'rule,sink',
    tagsEn: 'rule,sink',
    body: 'MATCH (n:Method) WHERE ja.isSink(n) RETURN n LIMIT 50'
  },
  {
    titleZh: '污点追踪示例',
    titleEn: 'Taint Track Example',
    tagsZh: 'ja,taint',
    tagsEn: 'ja,taint',
    body: 'CALL ja.taint.track("", "", "", "app/Sink", "sink", "()V", 8, 15000, 10, true, false, {{TRAVERSAL_MODE_LITERAL}}, "backward") YIELD path_id, hop, node_ids, edge_ids, score, confidence, evidence RETURN *'
  },
  {
    titleZh: 'Gadget 追踪示例',
    titleEn: 'Gadget Track Example',
    tagsZh: 'ja,gadget',
    tagsEn: 'ja,gadget',
    body: 'CALL ja.gadget.track("", "", "", "app/Sink", "sink", "()V", 8, 10, true, {{TRAVERSAL_MODE_LITERAL}}, "forward") YIELD path_id, hop, node_ids, edge_ids, score, confidence, evidence RETURN *'
  },
  {
    titleZh: '规则校验摘要',
    titleEn: 'Rule Validation',
    tagsZh: 'rules,validation',
    tagsEn: 'rules,validation',
    body: 'RETURN ja.ruleValidation() AS validation'
  },
  {
    titleZh: '浏览类节点',
    titleEn: 'Browse Classes',
    tagsZh: 'structure,class',
    tagsEn: 'structure,class',
    body: 'MATCH (c:Class) RETURN c LIMIT 50'
  },
  {
    titleZh: '查看类的方法',
    titleEn: 'Class Methods',
    tagsZh: 'structure,has',
    tagsEn: 'structure,has',
    body: 'MATCH (c:Class)-[r:HAS]->(m:Method) RETURN c,r,m LIMIT 50'
  },
  {
    titleZh: '查看继承关系',
    titleEn: 'Class Inheritance',
    tagsZh: 'structure,extend',
    tagsEn: 'structure,extend',
    body: 'MATCH (c1:Class)-[r:EXTEND]->(c2:Class) RETURN c1,r,c2 LIMIT 50'
  }
]

interface RenderScriptListOptions {
  container: HTMLElement
  scripts: ScriptItem[]
  tr: (zh: string, en: string) => string
  applyScriptBody: (body: string, execute?: boolean) => void
  togglePinned: (item: ScriptItem) => void
  deleteScript: (scriptId: number) => void
}

export function buildScriptsCountText(savedScriptCount: number, tr: RenderScriptListOptions['tr']): string {
  return `${tr('模板', 'Tpl')} ${BUILTIN_SCRIPTS.length} · ${tr('脚本', 'Saved')} ${savedScriptCount}`
}

export function renderScriptList(options: RenderScriptListOptions): void {
  const { container, scripts, tr } = options
  container.innerHTML = ''

  appendScriptSectionTitle(container, tr('内置模板', 'Built-in Templates'), BUILTIN_SCRIPTS.length)
  for (const item of BUILTIN_SCRIPTS) {
    container.appendChild(renderBuiltinScriptItem(item, options))
  }

  appendScriptSectionTitle(container, tr('已保存脚本', 'Saved Scripts'), scripts.length)
  if (scripts.length === 0) {
    const empty = document.createElement('div')
    empty.className = 'empty empty-inline'
    empty.textContent = tr('暂无脚本', 'No saved scripts')
    container.appendChild(empty)
    return
  }

  for (const item of scripts) {
    container.appendChild(renderSavedScriptItem(item, options))
  }
}

function renderBuiltinScriptItem(item: BuiltinScript, options: RenderScriptListOptions): HTMLElement {
  const { applyScriptBody, tr } = options
  const row = document.createElement('div')
  row.className = 'script-item script-item-template'
  const title = document.createElement('div')
  title.className = 'script-title'
  title.textContent = tr(item.titleZh, item.titleEn)
  title.title = item.body
  title.addEventListener('click', () => {
    applyScriptBody(item.body)
  })
  title.addEventListener('dblclick', () => {
    applyScriptBody(item.body, true)
  })

  const actions = document.createElement('div')
  actions.className = 'script-actions'
  const useBtn = document.createElement('button')
  useBtn.className = 'script-btn'
  useBtn.textContent = tr('用', 'Use')
  useBtn.title = tr('放入编辑器', 'Load into editor')
  useBtn.addEventListener('click', () => {
    applyScriptBody(item.body)
  })
  const runBtn = document.createElement('button')
  runBtn.className = 'script-btn'
  runBtn.textContent = '▶'
  runBtn.title = tr('直接运行', 'Run template')
  runBtn.addEventListener('click', () => {
    applyScriptBody(item.body, true)
  })
  actions.append(useBtn, runBtn)

  const meta = document.createElement('div')
  meta.className = 'script-meta'
  meta.textContent = `${tr(item.tagsZh, item.tagsEn)} · ${tr('模板', 'template')}`

  row.append(title, actions, meta)
  return row
}

function renderSavedScriptItem(item: ScriptItem, options: RenderScriptListOptions): HTMLElement {
  const { applyScriptBody, deleteScript, togglePinned, tr } = options
  const row = document.createElement('div')
  row.className = 'script-item'

  const title = document.createElement('div')
  title.className = 'script-title'
  title.textContent = item.title || tr('未命名脚本', 'Untitled Script')
  title.title = item.body
  title.addEventListener('dblclick', () => {
    applyScriptBody(item.body, true)
  })
  title.addEventListener('click', () => {
    applyScriptBody(item.body)
  })

  const actions = document.createElement('div')
  actions.className = 'script-actions'

  const pinBtn = document.createElement('button')
  pinBtn.className = 'script-btn'
  pinBtn.textContent = item.pinned ? '★' : '☆'
  pinBtn.title = item.pinned ? tr('取消置顶', 'Unpin') : tr('置顶', 'Pin')
  pinBtn.addEventListener('click', () => {
    togglePinned(item)
  })

  const delBtn = document.createElement('button')
  delBtn.className = 'script-btn'
  delBtn.textContent = '×'
  delBtn.title = tr('删除', 'Delete')
  delBtn.addEventListener('click', () => {
    deleteScript(item.scriptId)
  })

  actions.append(pinBtn, delBtn)

  const meta = document.createElement('div')
  meta.className = 'script-meta'
  meta.textContent = `${item.tags || '-'} · ${formatTime(item.updatedAt)}`

  row.append(title, actions, meta)
  return row
}

function appendScriptSectionTitle(container: HTMLElement, title: string, count: number): void {
  const section = document.createElement('div')
  section.className = 'script-section-title'
  section.textContent = `${title} · ${count}`
  container.appendChild(section)
}
