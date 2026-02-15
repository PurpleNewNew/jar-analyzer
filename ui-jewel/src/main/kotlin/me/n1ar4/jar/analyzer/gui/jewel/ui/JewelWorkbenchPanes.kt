package me.n1ar4.jar.analyzer.gui.jewel.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.n1ar4.jar.analyzer.gui.jewel.service.EditorService
import me.n1ar4.jar.analyzer.gui.jewel.service.EditorSnapshot
import me.n1ar4.jar.analyzer.gui.jewel.service.ProjectTreeService
import me.n1ar4.jar.analyzer.gui.jewel.service.TreeNodeItem
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.Orientation
import org.jetbrains.jewel.ui.component.Divider
import org.jetbrains.jewel.ui.component.SimpleTabContent
import org.jetbrains.jewel.ui.component.TabData
import org.jetbrains.jewel.ui.component.TabStrip
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.theme.defaultTabStyle
import kotlin.math.max
import kotlin.math.min

@Composable
fun ProjectTreePane(projectTreeService: ProjectTreeService) {
    var fileNameSearch by remember { mutableStateOf("") }
    var nodes by remember { mutableStateOf(projectTreeService.snapshot()) }
    val expanded = remember { mutableStateMapOf<String, Boolean>() }
    var selectedNodeId by remember { mutableStateOf<String?>(null) }
    var selectedMatchCursor by remember { mutableIntStateOf(0) }
    var statusText by remember { mutableStateOf("ready") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    fun reloadTree() {
        nodes = projectTreeService.snapshot()
    }

    LaunchedEffect(Unit) {
        reloadTree()
        while (true) {
            reloadTree()
            delay(1200)
        }
    }

    val flatNodes = remember(nodes, expanded) { flattenVisibleTree(nodes, expanded) }
    val nameMatches = remember(flatNodes, fileNameSearch) {
        if (fileNameSearch.isBlank()) {
            emptyList()
        } else {
            flatNodes.mapIndexedNotNull { idx, node ->
                if (!node.directory && node.label.contains(fileNameSearch, ignoreCase = true)) idx else null
            }
        }
    }

    LaunchedEffect(nameMatches.size) {
        if (selectedMatchCursor >= nameMatches.size) {
            selectedMatchCursor = 0
        }
    }

    LaunchedEffect(selectedMatchCursor, nameMatches) {
        if (nameMatches.isEmpty()) {
            return@LaunchedEffect
        }
        val target = nameMatches[selectedMatchCursor]
        selectedNodeId = flatNodes.getOrNull(target)?.id
        scope.launch {
            listState.animateScrollToItem(max(0, target - 2))
        }
        statusText = "${selectedMatchCursor + 1}/${nameMatches.size}"
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            itemsIndexed(items = flatNodes, key = { _, item -> item.id }) { _, node ->
                TreeRow(
                    node = node,
                    selected = selectedNodeId == node.id,
                    onToggle = { id ->
                        val now = expanded[id] ?: false
                        expanded[id] = !now
                    },
                    onOpen = { id, value ->
                        selectedNodeId = id
                        projectTreeService.openNode(value)
                        statusText = "open ${node.label}"
                    },
                )
            }
        }

        Divider(orientation = Orientation.Horizontal, modifier = Modifier.fillMaxWidth())
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("File Name (press Next)")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BoundTextField(
                    value = fileNameSearch,
                    onValueChange = {
                        fileNameSearch = it
                        selectedMatchCursor = 0
                    },
                    modifier = Modifier.weight(1f),
                )
                IdeaDefaultButton(onClick = {
                    if (nameMatches.isEmpty()) {
                        statusText = "0/0"
                    } else {
                        selectedMatchCursor = (selectedMatchCursor + 1) % nameMatches.size
                    }
                }, modifier = Modifier.width(76.dp).height(28.dp)) {
                    Text("Next")
                }
            }
            Text(if (nameMatches.isEmpty()) statusText else "${selectedMatchCursor + 1}/${nameMatches.size}")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IdeaOutlinedButton(onClick = {
                    projectTreeService.refresh()
                    reloadTree()
                    statusText = "refreshed"
                }, modifier = Modifier.width(84.dp).height(28.dp)) {
                    Text("Refresh")
                }
                IdeaOutlinedButton(onClick = {
                    expandAll(nodes, expanded)
                    statusText = "expanded"
                }, modifier = Modifier.width(82.dp).height(28.dp)) {
                    Text("Expand")
                }
                IdeaOutlinedButton(onClick = {
                    expanded.clear()
                    statusText = "collapsed"
                }, modifier = Modifier.width(88.dp).height(28.dp)) {
                    Text("Collapse")
                }
            }
        }
    }
}

@Composable
fun ComposeCodeEditor(editorService: EditorService) {
    var tabs by remember { mutableStateOf(listOf<EditorTabState>()) }
    var selectedTabKey by remember { mutableStateOf<String?>(null) }
    var searchKeyword by remember { mutableStateOf("") }
    var textState by remember { mutableStateOf(TextFieldValue("")) }
    var statusText by remember { mutableStateOf("ready") }

    LaunchedEffect(Unit) {
        while (true) {
            val snapshot = editorService.snapshot()
            val merged = mergeRuntimeSnapshot(tabs, snapshot)
            tabs = merged.first
            selectedTabKey = merged.second ?: selectedTabKey
            statusText = snapshot.statusText
            delay(600)
        }
    }

    val selectedTab = remember(tabs, selectedTabKey) {
        tabs.firstOrNull { it.key == selectedTabKey }
    }

    LaunchedEffect(selectedTab?.key, selectedTab?.content, selectedTab?.caretOffset) {
        val tab = selectedTab ?: return@LaunchedEffect
        val caret = tab.caretOffset.coerceIn(0, tab.content.length)
        val needsSync = textState.text != tab.content || textState.selection.start != caret
        if (needsSync) {
            textState = TextFieldValue(tab.content, TextRange(caret))
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        if (tabs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Open class/method from tree or tabs.")
            }
            return
        }

        TabStrip(
            tabs = tabs.map { tab ->
                TabData.Default(
                    selected = tab.key == selectedTabKey,
                    content = { tabState -> SimpleTabContent(label = tab.title, state = tabState) },
                    closable = tabs.size > 1,
                    onClose = {
                        tabs = closeTab(tabs, tab.key)
                        if (tabs.none { it.key == selectedTabKey }) {
                            selectedTabKey = tabs.lastOrNull()?.key
                            tabs.lastOrNull()?.let { selectTab(it, editorService) }
                        }
                    },
                    onClick = {
                        selectedTabKey = tab.key
                        selectTab(tab, editorService)
                    },
                )
            },
            style = JewelTheme.defaultTabStyle,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
        )
        Divider(orientation = Orientation.Horizontal, modifier = Modifier.fillMaxWidth())

        val active = selectedTab
        if (active == null) {
            Text("No active tab.")
            return
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${active.className} :: ${active.methodName}${active.methodDesc}",
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            BoundTextField(
                value = searchKeyword,
                onValueChange = { searchKeyword = it },
                modifier = Modifier.width(180.dp),
            )
            IdeaOutlinedButton(
                onClick = { editorService.search(searchKeyword, false) },
                modifier = Modifier.width(52.dp).height(24.dp),
            ) {
                Text("Prev")
            }
            IdeaOutlinedButton(
                onClick = { editorService.search(searchKeyword, true) },
                modifier = Modifier.width(52.dp).height(24.dp),
            ) {
                Text("Next")
            }
            IdeaOutlinedButton(
                onClick = { selectTab(active, editorService) },
                modifier = Modifier.width(52.dp).height(24.dp),
            ) {
                Text("Sync")
            }
        }
        Divider(orientation = Orientation.Horizontal, modifier = Modifier.fillMaxWidth())
        BasicTextField(
            value = textState,
            onValueChange = { next ->
                textState = next
                tabs = updateTabText(tabs, active.key, next.text, next.selection.start)
                editorService.applyText(next.text, next.selection.start)
            },
            textStyle = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                lineHeight = 19.sp,
            ),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.Transparent)
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
        )
        Divider(orientation = Orientation.Horizontal, modifier = Modifier.fillMaxWidth())
        val caret = textState.selection.start.coerceIn(0, textState.text.length)
        val lineCol = offsetToLineCol(textState.text, caret)
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "status: $statusText",
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text("Ln ${lineCol.first}, Col ${lineCol.second}")
        }
    }
}

private data class FlatTreeNode(
    val id: String,
    val label: String,
    val value: String,
    val directory: Boolean,
    val depth: Int,
    val expanded: Boolean = false,
)

private data class EditorTabState(
    val key: String,
    val title: String,
    val className: String,
    val jarName: String,
    val methodName: String,
    val methodDesc: String,
    val content: String,
    val caretOffset: Int,
)

private fun mergeRuntimeSnapshot(
    tabs: List<EditorTabState>,
    snapshot: EditorSnapshot,
): Pair<List<EditorTabState>, String?> {
    if (snapshot.className.isBlank() && snapshot.content.isBlank()) {
        return tabs to null
    }
    val key = buildTabKey(snapshot.className, snapshot.jarName)
    val title = buildTabTitle(snapshot.className)
    val next = EditorTabState(
        key = key,
        title = title,
        className = snapshot.className,
        jarName = snapshot.jarName,
        methodName = snapshot.methodName,
        methodDesc = snapshot.methodDesc,
        content = snapshot.content,
        caretOffset = snapshot.caretOffset,
    )
    val idx = tabs.indexOfFirst { it.key == key }
    if (idx < 0) {
        return (tabs + next) to key
    }
    val updated = tabs.toMutableList()
    updated[idx] = next
    return updated to key
}

private fun buildTabKey(className: String, jarName: String): String {
    val cls = className.ifBlank { "Code" }
    val jar = jarName.ifBlank { "default" }
    return "$cls|$jar"
}

private fun buildTabTitle(className: String): String {
    if (className.isBlank()) {
        return "Code"
    }
    val slash = className.lastIndexOf('/')
    val dot = className.lastIndexOf('.')
    val idx = max(slash, dot)
    return if (idx >= 0 && idx + 1 < className.length) {
        className.substring(idx + 1)
    } else {
        className
    }
}

private fun closeTab(tabs: List<EditorTabState>, key: String): List<EditorTabState> {
    return tabs.filterNot { it.key == key }
}

private fun updateTabText(
    tabs: List<EditorTabState>,
    key: String,
    text: String,
    caretOffset: Int,
): List<EditorTabState> {
    return tabs.map {
        if (it.key != key) {
            it
        } else {
            it.copy(content = text, caretOffset = caretOffset)
        }
    }
}

private fun selectTab(tab: EditorTabState, editorService: EditorService) {
    if (tab.methodName.isNotBlank()) {
        editorService.openMethod(tab.className, tab.methodName, tab.methodDesc)
    } else {
        editorService.openClass(tab.className)
    }
}

private fun offsetToLineCol(text: String, offset: Int): Pair<Int, Int> {
    if (text.isEmpty()) {
        return 1 to 1
    }
    val safeOffset = min(max(0, offset), text.length)
    var line = 1
    var col = 1
    var i = 0
    while (i < safeOffset) {
        if (text[i] == '\n') {
            line++
            col = 1
        } else {
            col++
        }
        i++
    }
    return line to col
}

private fun flattenVisibleTree(
    nodes: List<TreeNodeItem>,
    expanded: Map<String, Boolean>,
): List<FlatTreeNode> {
    val out = mutableListOf<FlatTreeNode>()
    fun walk(items: List<TreeNodeItem>, depth: Int, parent: String) {
        items.sortedWith(compareByDescending<TreeNodeItem> { it.directory }.thenBy { it.label })
            .forEach { node ->
                val id = if (parent.isBlank()) node.label else "$parent/${node.label}"
                val isExpanded = expanded[id] ?: false
                out.add(
                    FlatTreeNode(
                        id = id,
                        label = node.label,
                        value = node.value,
                        directory = node.directory,
                        depth = depth,
                        expanded = isExpanded,
                    ),
                )
                if (node.directory && isExpanded && node.children.isNotEmpty()) {
                    walk(node.children, depth + 1, id)
                }
            }
    }
    walk(nodes, 0, "")
    return out
}

private fun expandAll(nodes: List<TreeNodeItem>, expanded: MutableMap<String, Boolean>) {
    fun walk(items: List<TreeNodeItem>, parent: String) {
        items.forEach { node ->
            val id = if (parent.isBlank()) node.label else "$parent/${node.label}"
            if (node.directory) {
                expanded[id] = true
                walk(node.children, id)
            }
        }
    }
    walk(nodes, "")
}

@Composable
private fun TreeRow(
    node: FlatTreeNode,
    selected: Boolean,
    onToggle: (String) -> Unit,
    onOpen: (String, String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .ideaInteractiveClickable(selected = selected) {
                if (node.directory) {
                    onToggle(node.id)
                } else {
                    onOpen(node.id, node.value)
                }
            }
            .padding(start = (node.depth * 14).dp + 8.dp, top = 4.dp, bottom = 4.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val prefix = when {
            node.directory && node.expanded -> "▼ "
            node.directory && !node.expanded -> "▶ "
            else -> "• "
        }
        Text(prefix + node.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
