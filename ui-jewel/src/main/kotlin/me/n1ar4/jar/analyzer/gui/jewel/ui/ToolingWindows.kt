package me.n1ar4.jar.analyzer.gui.jewel.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.n1ar4.jar.analyzer.core.DatabaseManager
import me.n1ar4.jar.analyzer.engine.index.IndexEngine
import me.n1ar4.jar.analyzer.engine.index.IndexPluginsSupport
import me.n1ar4.jar.analyzer.gui.jewel.state.JewelThemeMode
import me.n1ar4.jar.analyzer.gui.runtime.api.RuntimeFacades
import me.n1ar4.jar.analyzer.gui.runtime.model.BuildSettingsDto
import me.n1ar4.jar.analyzer.gui.runtime.model.ChainsResultItemDto
import me.n1ar4.jar.analyzer.gui.runtime.model.ChainsSettingsDto
import me.n1ar4.jar.analyzer.gui.runtime.model.GadgetSettingsDto
import me.n1ar4.jar.analyzer.gui.runtime.model.ScaSettingsDto
import me.n1ar4.jar.analyzer.gui.runtime.model.SearchMatchMode
import me.n1ar4.jar.analyzer.gui.runtime.model.SearchMode
import me.n1ar4.jar.analyzer.gui.runtime.model.SearchQueryDto
import me.n1ar4.jar.analyzer.gui.runtime.model.ToolingWindowAction
import me.n1ar4.jar.analyzer.gui.runtime.model.ToolingWindowPayload
import me.n1ar4.jar.analyzer.gui.runtime.model.ToolingWindowRequest
import me.n1ar4.jar.analyzer.starter.Const
import me.n1ar4.jar.analyzer.utils.JarUtil
import org.jetbrains.jewel.intui.standalone.theme.IntUiTheme
import org.jetbrains.jewel.ui.Orientation
import org.jetbrains.jewel.ui.component.CheckboxRow
import org.jetbrains.jewel.ui.component.Divider
import org.jetbrains.jewel.ui.component.RadioButtonRow
import org.jetbrains.jewel.ui.component.Text
import java.net.HttpURLConnection
import java.net.Socket
import java.net.URI
import java.net.URL
import java.nio.charset.StandardCharsets
import java.nio.file.DirectoryStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.sql.DriverManager
import java.util.Base64
import java.util.LinkedHashMap
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException
import kotlin.math.max
import kotlin.math.min

data class ToolWindowState(
    val id: Long,
    val title: String,
    val action: ToolingWindowAction,
    val payload: ToolingWindowPayload,
)

object ToolingWindowsState {
    private val idGen = AtomicLong(1)
    private val _windows = MutableStateFlow<List<ToolWindowState>>(emptyList())
    val windows: StateFlow<List<ToolWindowState>> = _windows.asStateFlow()

    fun open(request: ToolingWindowRequest) {
        val state = ToolWindowState(
            id = idGen.getAndIncrement(),
            title = titleOf(request),
            action = request.action(),
            payload = request.payload(),
        )
        _windows.value = _windows.value + state
    }

    fun close(id: Long) {
        _windows.value = _windows.value.filterNot { it.id == id }
    }

    private fun titleOf(request: ToolingWindowRequest): String {
        return when (request.action()) {
            ToolingWindowAction.EXPORT -> "Export Tool"
            ToolingWindowAction.REMOTE_LOAD -> "Remote Load"
            ToolingWindowAction.PROXY -> "Proxy Config"
            ToolingWindowAction.PARTITION -> "Partition Config"
            ToolingWindowAction.GLOBAL_SEARCH -> "Global Search"
            ToolingWindowAction.SYSTEM_MONITOR -> "System Monitor"
            ToolingWindowAction.MARKDOWN_VIEWER -> {
                val payload = request.payload() as? ToolingWindowPayload.MarkdownPayload
                if (payload == null || payload.title().isBlank()) "Markdown Viewer" else payload.title()
            }
            ToolingWindowAction.CFG -> "CFG Analyze"
            ToolingWindowAction.FRAME -> "Frame Analyze"
            ToolingWindowAction.OPCODE -> "Opcode Viewer"
            ToolingWindowAction.ASM -> "ASM Viewer"
            ToolingWindowAction.EL_SEARCH -> "EL Search"
            ToolingWindowAction.SQL_CONSOLE -> "SQL Console"
            ToolingWindowAction.ENCODE_TOOL -> "Encode Tool"
            ToolingWindowAction.SOCKET_LISTENER -> "Socket Listener"
            ToolingWindowAction.SERIALIZATION -> "Serialization Tool"
            ToolingWindowAction.BCEL_TOOL -> "BCEL Tool"
            ToolingWindowAction.REPEATER -> "HTTP Repeater"
            ToolingWindowAction.OBFUSCATION -> "Obfuscation Tool"
            ToolingWindowAction.CHAINS_ADVANCED -> "Chains Advanced"
            ToolingWindowAction.CHAINS_RESULT -> {
                val payload = request.payload() as? ToolingWindowPayload.ChainsResultPayload
                if (payload == null || payload.title().isBlank()) "Chains Result" else payload.title()
            }
            ToolingWindowAction.SCA_INPUT_PICKER -> "SCA Input"
            ToolingWindowAction.GADGET_DIR_PICKER -> "Gadget Input"
            ToolingWindowAction.TEXT_VIEWER -> {
                val payload = request.payload() as? ToolingWindowPayload.TextPayload
                if (payload == null || payload.title().isBlank()) "Tooling" else payload.title()
            }
        }
    }
}

@Composable
fun ToolingWindowsHost() {
    val windows by ToolingWindowsState.windows.collectAsState()
    windows.forEach { window ->
        val isDark = JewelThemeMode.fromRuntimeTheme(RuntimeFacades.tooling().configSnapshot().theme()) ==
            JewelThemeMode.ISLAND_DARK
        Window(
            title = window.title,
            state = rememberWindowState(width = 920.dp, height = 640.dp),
            onCloseRequest = { ToolingWindowsState.close(window.id) },
        ) {
            IntUiTheme(isDark = isDark, swingCompatMode = false) {
                ToolWindowContent(window)
            }
        }
    }
}

@Composable
private fun ToolWindowContent(window: ToolWindowState) {
    Column(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(window.title)
        Text(
            text = "action=${window.action}",
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Divider(orientation = Orientation.Horizontal, modifier = Modifier.fillMaxWidth())

        when (window.action) {
            ToolingWindowAction.MARKDOWN_VIEWER -> MarkdownViewerPane(window)
            ToolingWindowAction.GLOBAL_SEARCH -> GlobalSearchPane()
            ToolingWindowAction.OPCODE,
            ToolingWindowAction.ASM,
            ToolingWindowAction.CFG,
            ToolingWindowAction.FRAME,
            ToolingWindowAction.BCEL_TOOL,
            ToolingWindowAction.TEXT_VIEWER -> TextPayloadPane(window)
            ToolingWindowAction.ENCODE_TOOL -> EncodePane()
            ToolingWindowAction.PARTITION -> PartitionPane()
            ToolingWindowAction.PROXY -> ProxyPane()
            ToolingWindowAction.EXPORT -> ExportPane()
            ToolingWindowAction.REMOTE_LOAD -> RemoteLoadPane()
            ToolingWindowAction.SYSTEM_MONITOR -> SystemMonitorPane()
            ToolingWindowAction.CHAINS_ADVANCED -> ChainsAdvancedPane()
            ToolingWindowAction.CHAINS_RESULT -> ChainsResultPane(window)
            ToolingWindowAction.SQL_CONSOLE -> SqlConsolePane()
            ToolingWindowAction.EL_SEARCH -> ElSearchPane()
            ToolingWindowAction.REPEATER -> RepeaterPane()
            ToolingWindowAction.SERIALIZATION -> SerializationPane()
            ToolingWindowAction.OBFUSCATION -> ObfuscationPane()
            ToolingWindowAction.SOCKET_LISTENER -> SocketListenerPane()
            ToolingWindowAction.SCA_INPUT_PICKER -> ScaInputPickerPane(window)
            ToolingWindowAction.GADGET_DIR_PICKER -> GadgetInputPickerPane(window)
        }
    }
}

@Composable
private fun MarkdownViewerPane(window: ToolWindowState) {
    val payload = window.payload as? ToolingWindowPayload.MarkdownPayload
    val markdown = remember(payload) { readMarkdown(payload?.markdownResource() ?: "") }
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text(markdown)
    }
}

@Composable
private fun GlobalSearchPane() {
    var keyword by remember { mutableStateOf("") }
    var matchMode by remember { mutableStateOf(GlobalSearchMatchMode.CONTAINS) }
    var indexMode by remember { mutableStateOf(GlobalSearchIndexMode.PASSIVE_LUCENE) }
    var caseSensitive by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("ready") }
    var results by remember { mutableStateOf<List<GlobalSearchItem>>(emptyList()) }
    var manualTick by remember { mutableStateOf(0) }
    var classFileCount by remember { mutableStateOf(0) }
    var indexSizeText by remember { mutableStateOf(globalSearchIndexSizeText()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        classFileCount = withContext(Dispatchers.IO) { GlobalSearchCache.ensureFileSnapshot() }
        indexSizeText = globalSearchIndexSizeText()
    }

    LaunchedEffect(indexMode) {
        withContext(Dispatchers.IO) {
            // false -> enable passive lucene add-index on decompile/open flows
            IndexPluginsSupport.setUseActive(indexMode != GlobalSearchIndexMode.PASSIVE_LUCENE)
        }
    }

    LaunchedEffect(keyword, matchMode, indexMode, caseSensitive, manualTick) {
        val kw = keyword.trim()
        if (kw.isEmpty()) {
            results = emptyList()
            status = "keyword empty"
            return@LaunchedEffect
        }
        delay(180)
        status = "searching..."
        val result = withContext(Dispatchers.IO) {
            runGlobalSearch(kw, matchMode, indexMode, caseSensitive)
        }
        results = result.items
        status = result.status
        indexSizeText = globalSearchIndexSizeText()
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BoundTextField(keyword, { keyword = it }, Modifier.weight(1f))
            IdeaDefaultButton(onClick = { manualTick++ }) { Text("Search") }
            IdeaOutlinedButton(onClick = {
                keyword = ""
                results = emptyList()
                status = "cleared"
            }) { Text("Clear") }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RadioButtonRow(
                "contains",
                matchMode == GlobalSearchMatchMode.CONTAINS,
                { matchMode = GlobalSearchMatchMode.CONTAINS },
            )
            RadioButtonRow(
                "regexp",
                matchMode == GlobalSearchMatchMode.REGEX,
                { matchMode = GlobalSearchMatchMode.REGEX },
            )
            CheckboxRow("case sensitive", caseSensitive, { caseSensitive = it })
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RadioButtonRow(
                "no lucene index",
                indexMode == GlobalSearchIndexMode.NO_LUCENE,
                { indexMode = GlobalSearchIndexMode.NO_LUCENE },
            )
            RadioButtonRow(
                "passive lucene index",
                indexMode == GlobalSearchIndexMode.PASSIVE_LUCENE,
                { indexMode = GlobalSearchIndexMode.PASSIVE_LUCENE },
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(indexSizeText, modifier = Modifier.weight(1f))
            Text("Class Files: $classFileCount")
            IdeaOutlinedButton(onClick = {
                scope.launch {
                    status = "refreshing class snapshot..."
                    classFileCount = withContext(Dispatchers.IO) {
                        GlobalSearchCache.refreshFileSnapshot()
                    }
                    results = emptyList()
                    manualTick++
                }
            }) {
                Text("Refresh Files")
            }
            IdeaDefaultButton(onClick = {
                scope.launch {
                    status = "building lucene index..."
                    status = withContext(Dispatchers.IO) { buildGlobalLuceneIndex() }
                    indexSizeText = globalSearchIndexSizeText()
                    GlobalSearchCache.clearQueryCache()
                    manualTick++
                }
            }) {
                Text("Build Index")
            }
        }

        Divider(orientation = Orientation.Horizontal, modifier = Modifier.fillMaxWidth())
        Text("Result ${results.size}")

        LazyColumn(
            modifier = Modifier.fillMaxWidth().heightIn(min = 220.dp, max = 480.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            itemsIndexed(results) { index, item ->
                GlobalSearchResultRow(
                    index = index,
                    item = item,
                    onOpen = { status = openGlobalSearchResult(item, caseSensitive) },
                )
            }
        }

        Text(status)
    }
}

private enum class GlobalSearchMatchMode {
    CONTAINS,
    REGEX,
}

private enum class GlobalSearchIndexMode {
    NO_LUCENE,
    PASSIVE_LUCENE,
}

private data class GlobalSearchItem(
    val fromContent: Boolean,
    val absPath: String,
    val className: String,
    val title: String,
    val searchKey: String,
)

private data class GlobalSearchExecution(
    val items: List<GlobalSearchItem>,
    val status: String,
)

private object GlobalSearchCache {
    private const val QUERY_CACHE_SIZE = 100
    private val queryCache = object : LinkedHashMap<String, List<GlobalSearchItem>>(
        QUERY_CACHE_SIZE,
        0.75f,
        true,
    ) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<GlobalSearchItem>>): Boolean {
            return size > QUERY_CACHE_SIZE
        }
    }
    private var fileSnapshot: List<String> = emptyList()
    private var fileSnapshotReady: Boolean = false
    private var builtOnce: Boolean = false

    @Synchronized
    fun ensureFileSnapshot(): Int {
        if (!fileSnapshotReady) {
            fileSnapshot = scanTempClassFiles()
            fileSnapshotReady = true
        }
        return fileSnapshot.size
    }

    @Synchronized
    fun refreshFileSnapshot(): Int {
        fileSnapshot = scanTempClassFiles()
        fileSnapshotReady = true
        clearQueryCache()
        return fileSnapshot.size
    }

    @Synchronized
    fun files(): List<String> {
        if (!fileSnapshotReady) {
            fileSnapshot = scanTempClassFiles()
            fileSnapshotReady = true
        }
        return fileSnapshot
    }

    @Synchronized
    fun query(key: String): List<GlobalSearchItem>? {
        return queryCache[key]
    }

    @Synchronized
    fun saveQuery(key: String, items: List<GlobalSearchItem>) {
        if (key.isNotBlank()) {
            queryCache[key] = items
        }
    }

    @Synchronized
    fun clearQueryCache() {
        queryCache.clear()
    }

    @Synchronized
    fun isBuiltOnce(): Boolean {
        return builtOnce
    }

    @Synchronized
    fun markBuiltOnce() {
        builtOnce = true
    }
}

@Composable
private fun GlobalSearchResultRow(
    index: Int,
    item: GlobalSearchItem,
    onOpen: () -> Unit,
) {
    val typeLabel = if (item.fromContent) "code/content" else "class/file"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .ideaInteractiveClickable(onClick = onOpen)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("${index + 1}.")
        Text("[$typeLabel] ${item.className}", modifier = Modifier.weight(1f))
        if (item.title.isNotBlank()) {
            Text(item.title, modifier = Modifier.width(180.dp))
        }
    }
    Divider(orientation = Orientation.Horizontal, modifier = Modifier.fillMaxWidth())
}

private fun runGlobalSearch(
    keyword: String,
    matchMode: GlobalSearchMatchMode,
    indexMode: GlobalSearchIndexMode,
    caseSensitive: Boolean,
): GlobalSearchExecution {
    val cacheKey = buildGlobalSearchCacheKey(keyword, matchMode, indexMode, caseSensitive)
    val cached = GlobalSearchCache.query(cacheKey)
    if (cached != null) {
        return GlobalSearchExecution(cached, "result size: ${cached.size} (cache)")
    }

    val merged = mutableListOf<GlobalSearchItem>()
    try {
        merged.addAll(searchClassFileName(keyword, matchMode))
    } catch (ex: PatternSyntaxException) {
        return GlobalSearchExecution(emptyList(), "regex error: ${safe(ex.message)}")
    } catch (ex: Throwable) {
        return GlobalSearchExecution(emptyList(), "filename search failed: ${safe(ex.message)}")
    }

    if (indexMode == GlobalSearchIndexMode.PASSIVE_LUCENE) {
        try {
            merged.addAll(searchLuceneContent(keyword, matchMode, caseSensitive))
        } catch (ex: Throwable) {
            val fileOnly = merged.size
            return GlobalSearchExecution(merged, "file result: $fileOnly, lucene error: ${safe(ex.message)}")
        }
    }

    val filtered = merged.filter { it.absPath.endsWith(".class", ignoreCase = true) }
    GlobalSearchCache.saveQuery(cacheKey, filtered)
    return GlobalSearchExecution(filtered, "result size: ${filtered.size}")
}

private fun buildGlobalSearchCacheKey(
    keyword: String,
    matchMode: GlobalSearchMatchMode,
    indexMode: GlobalSearchIndexMode,
    caseSensitive: Boolean,
): String {
    return buildString(96) {
        append(keyword)
        append('|')
        append(matchMode.name)
        append('|')
        append(indexMode.name)
        append('|')
        append(if (caseSensitive) "CS" else "CI")
    }
}

private fun searchClassFileName(
    keyword: String,
    matchMode: GlobalSearchMatchMode,
): List<GlobalSearchItem> {
    val results = mutableListOf<GlobalSearchItem>()
    val files = GlobalSearchCache.files()
    if (files.isEmpty()) {
        return results
    }

    val pattern = if (matchMode == GlobalSearchMatchMode.REGEX) Pattern.compile(keyword) else null
    files.forEach { absPath ->
        val fileName = extractFileName(absPath)
        val hit = when (matchMode) {
            GlobalSearchMatchMode.CONTAINS -> fileName.contains(keyword)
            GlobalSearchMatchMode.REGEX -> pattern!!.matcher(fileName).find()
        }
        if (!hit) {
            return@forEach
        }
        results.add(
            GlobalSearchItem(
                fromContent = false,
                absPath = absPath,
                className = resolveClassName(absPath),
                title = fileName,
                searchKey = keyword,
            ),
        )
    }
    return results
}

private fun searchLuceneContent(
    keyword: String,
    matchMode: GlobalSearchMatchMode,
    caseSensitive: Boolean,
): List<GlobalSearchItem> {
    if (!luceneIndexValid()) {
        return emptyList()
    }
    val result = when (matchMode) {
        GlobalSearchMatchMode.CONTAINS -> IndexEngine.searchNormal(keyword, caseSensitive)
        GlobalSearchMatchMode.REGEX -> IndexEngine.searchRegex(keyword, caseSensitive)
    }
    val data = result.data ?: return emptyList()
    val results = mutableListOf<GlobalSearchItem>()
    data.forEach { entry ->
        val path = entry["path"] as? String ?: return@forEach
        if (!path.endsWith(".class", ignoreCase = true)) {
            return@forEach
        }
        val title = entry["title"] as? String ?: extractFileName(path)
        results.add(
            GlobalSearchItem(
                fromContent = true,
                absPath = path,
                className = resolveClassName(path),
                title = title,
                searchKey = keyword,
            ),
        )
    }
    return results
}

private fun resolveClassName(absPath: String): String {
    val resolved = JarUtil.resolveClassNameFromPath(absPath, true)
    return if (resolved.isNullOrBlank()) absPath else resolved
}

private fun extractFileName(absPath: String): String {
    return try {
        Paths.get(absPath).fileName?.toString() ?: absPath
    } catch (_: Throwable) {
        val slash = absPath.lastIndexOf('/')
        val backslash = absPath.lastIndexOf('\\')
        val idx = max(slash, backslash)
        if (idx < 0 || idx >= absPath.length - 1) absPath else absPath.substring(idx + 1)
    }
}

private fun openGlobalSearchResult(item: GlobalSearchItem, caseSensitive: Boolean): String {
    val className = resolveClassName(item.absPath)
    if (className.isBlank() || className == item.absPath) {
        return "cannot resolve class from path"
    }
    RuntimeFacades.editor().openClass(className, null)
    val keyword = item.searchKey.trim()
    if (keyword.isNotBlank()) {
        if (caseSensitive) {
            RuntimeFacades.editor().searchInCurrent(keyword, true)
        } else {
            val doc = RuntimeFacades.editor().current()
            val text = doc.content()
            val idx = text.lowercase(Locale.ROOT).indexOf(keyword.lowercase(Locale.ROOT))
            if (idx >= 0) {
                RuntimeFacades.editor().applyEditorText(text, idx)
            } else {
                RuntimeFacades.editor().searchInCurrent(keyword, true)
            }
        }
    }
    return "opened $className"
}

private fun buildGlobalLuceneIndex(): String {
    if (GlobalSearchCache.isBuiltOnce()) {
        return "one run only allows one full build"
    }
    return try {
        val ok = IndexPluginsSupport.initIndex()
        if (!ok) {
            "create lucene index error"
        } else {
            // Follow old Swing behavior: after manual full build, stop passive append.
            IndexPluginsSupport.setUseActive(true)
            GlobalSearchCache.markBuiltOnce()
            "create lucene index finish"
        }
    } catch (ex: Throwable) {
        "build lucene index failed: ${safe(ex.message)}"
    }
}

private fun scanTempClassFiles(): List<String> {
    val base = Paths.get(Const.tempDir)
    if (!Files.isDirectory(base)) {
        return emptyList()
    }
    val results = mutableListOf<String>()
    val stream = Files.walk(base)
    try {
        val it = stream.iterator()
        while (it.hasNext()) {
            val path = it.next()
            if (!Files.isRegularFile(path)) {
                continue
            }
            if (!path.fileName.toString().endsWith(".class", ignoreCase = true)) {
                continue
            }
            results.add(path.toAbsolutePath().toString())
        }
    } finally {
        stream.close()
    }
    return results
}

private fun luceneIndexValid(): Boolean {
    val indexPath = Paths.get(Const.indexDir)
    if (!Files.isDirectory(indexPath)) {
        return false
    }
    return try {
        Files.newDirectoryStream(indexPath).use { stream: DirectoryStream<Path> ->
            for (entry in stream) {
                if (Files.isRegularFile(entry) && Files.size(entry) > 0L) {
                    return true
                }
            }
            false
        }
    } catch (_: Throwable) {
        false
    }
}

private fun globalSearchIndexSizeText(): String {
    return "当前索引大小：${formatDataSize(calculateDirSize(Paths.get(Const.indexDir)))}"
}

private fun calculateDirSize(path: Path): Long {
    if (!Files.exists(path)) {
        return 0L
    }
    if (!Files.isDirectory(path)) {
        return try {
            Files.size(path)
        } catch (_: Throwable) {
            0L
        }
    }
    var total = 0L
    return try {
        Files.newDirectoryStream(path).use { stream ->
            for (entry in stream) {
                total += calculateDirSize(entry)
            }
        }
        total
    } catch (_: Throwable) {
        0L
    }
}

private fun formatDataSize(size: Long): String {
    if (size < 1024L) return "$size Bytes"
    val units = arrayOf("Bytes", "KB", "MB", "GB", "TB")
    val exp = min(units.lastIndex, (Math.log(size.toDouble()) / Math.log(1024.0)).toInt())
    val value = size / Math.pow(1024.0, exp.toDouble())
    return String.format(Locale.ROOT, "%.2f %s", value, units[exp])
}

private fun safe(value: String?): String {
    return value?.trim() ?: ""
}

@Composable
private fun ScaInputPickerPane(window: ToolWindowState) {
    val payload = window.payload as? ToolingWindowPayload.PathPayload
    var inputPath by remember {
        mutableStateOf(payload?.value() ?: RuntimeFacades.sca().snapshot().settings().inputPath())
    }
    var status by remember { mutableStateOf("ready") }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BoundTextField(inputPath, { inputPath = it }, Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IdeaDefaultButton(onClick = {
                val old = RuntimeFacades.sca().snapshot().settings()
                RuntimeFacades.sca().apply(
                    ScaSettingsDto(
                        old.scanLog4j(),
                        old.scanShiro(),
                        old.scanFastjson(),
                        inputPath.trim(),
                        old.outputMode(),
                        old.outputFile(),
                    ),
                )
                status = "sca input applied"
            }) {
                Text("Apply")
            }
            IdeaOutlinedButton(onClick = {
                inputPath = RuntimeFacades.build().snapshot().settings().inputPath()
                status = "copied from build input"
            }) {
                Text("Use Build Input")
            }
        }
        Text(status)
    }
}

@Composable
private fun GadgetInputPickerPane(window: ToolWindowState) {
    val payload = window.payload as? ToolingWindowPayload.PathPayload
    var inputDir by remember {
        mutableStateOf(payload?.value() ?: RuntimeFacades.gadget().snapshot().settings().inputDir())
    }
    var status by remember { mutableStateOf("ready") }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BoundTextField(inputDir, { inputDir = it }, Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IdeaDefaultButton(onClick = {
                val old = RuntimeFacades.gadget().snapshot().settings()
                RuntimeFacades.gadget().apply(
                    GadgetSettingsDto(
                        inputDir.trim(),
                        old.nativeMode(),
                        old.hessian(),
                        old.jdbc(),
                        old.fastjson(),
                    ),
                )
                status = "gadget input applied"
            }) {
                Text("Apply")
            }
            IdeaOutlinedButton(onClick = {
                inputDir = RuntimeFacades.build().snapshot().settings().inputPath()
                status = "copied from build input"
            }) {
                Text("Use Build Input")
            }
        }
        Text(status)
    }
}

@Composable
private fun TextPayloadPane(window: ToolWindowState) {
    val payload = window.payload as? ToolingWindowPayload.TextPayload
    val text = payload?.content() ?: "No output."
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text(text)
    }
}

@Composable
private fun ChainsResultPane(window: ToolWindowState) {
    val payload = window.payload as? ToolingWindowPayload.ChainsResultPayload
    if (payload == null) {
        Text("Invalid chains payload.")
        return
    }
    val items = payload.items()
    var selected by remember(payload) { mutableStateOf(0) }
    var status by remember(payload) { mutableStateOf("ready") }
    val expanded = remember(payload) { mutableStateMapOf<Int, Boolean>() }

    if (items.isEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(payload.emptyHint())
            IdeaOutlinedButton(onClick = {
                status = exportChainsPayload(payload)
            }) { Text("Export Empty Report") }
            Text(status)
        }
        return
    }

    val current = items.getOrNull(selected) ?: items.first()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IdeaDefaultButton(onClick = {
                status = exportChainsPayload(payload)
            }) { Text("Export All") }
            IdeaOutlinedButton(onClick = {
                items.forEach { expanded[it.index()] = true }
                status = "all chains expanded"
            }) { Text("Expand All") }
            IdeaOutlinedButton(onClick = {
                expanded.clear()
                status = "all chains collapsed"
            }) { Text("Collapse All") }
        }

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LazyColumn(
                modifier = Modifier.weight(0.48f).fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                itemsIndexed(items) { index, item ->
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .ideaInteractiveClickable(selected = selected == index) { selected = index },
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(if (selected == index) ">" else " ")
                            Text(chainSummary(item), modifier = Modifier.weight(1f))
                            IdeaOutlinedButton(onClick = {
                                val key = item.index()
                                expanded[key] = !(expanded[key] ?: false)
                            }) {
                                Text(if (expanded[item.index()] == true) "Fold" else "Expand")
                            }
                        }
                        if (expanded[item.index()] == true) {
                            val methods = item.methods()
                            if (methods.isEmpty()) {
                                Text("  <empty chain>")
                            } else {
                                methods.forEachIndexed { step, method ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .ideaInteractiveClickable {
                                                val jarId = method.jarId()
                                                RuntimeFacades.editor().openMethod(
                                                    method.className(),
                                                    method.methodName(),
                                                    method.methodDesc(),
                                                    if (jarId <= 0) null else jarId,
                                                )
                                                status = "jumped: ${method.className()}#${method.methodName()}"
                                            }
                                            .padding(start = 12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Text("${step + 1}.")
                                        Text(
                                            "${method.className()}#${method.methodName()}${method.methodDesc()}",
                                            modifier = Modifier.weight(1f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }
                        }
                        Divider(orientation = Orientation.Horizontal, modifier = Modifier.fillMaxWidth())
                    }
                }
            }

            Column(
                modifier = Modifier.weight(0.52f).fillMaxSize().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Selected #${current.index()}")
                Text("sink: ${current.sink()}")
                Text("source: ${current.source()}")
                Text(
                    "depth=${current.depth()} path=${current.pathCount()} " +
                        "node=${current.nodeCount()} edge=${current.edgeCount()} " +
                        "elapsedMs=${current.elapsedMs()}",
                )
                if (current.truncated()) {
                    Text("truncated: ${current.truncateReason()}")
                }
                if (current.recommend().isNotBlank()) {
                    Text("recommend: ${current.recommend()}")
                }
                if (current.taint()) {
                    Text("taint: ${if (current.taintPass()) "PASS" else "BLOCK"}")
                    if (current.lowConfidence()) {
                        Text("confidence: LOW")
                    }
                    if (current.sanitizerDetail().isNotBlank()) {
                        Text("sanitizer:")
                        Text(current.sanitizerDetail())
                    }
                    if (current.taintDetail().isNotBlank()) {
                        Text("detail:")
                        Text(current.taintDetail())
                    }
                }
            }
        }

        Text(status)
    }
}

@Composable
private fun EncodePane() {
    var input by remember { mutableStateOf("") }
    var output by remember { mutableStateOf("") }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        BoundTextField(input, { input = it }, Modifier.weight(1f))
        IdeaDefaultButton(onClick = {
            output = Base64.getEncoder().encodeToString(input.toByteArray(StandardCharsets.UTF_8))
        }) { Text("B64 Encode") }
        IdeaDefaultButton(onClick = {
            output = try {
                String(Base64.getDecoder().decode(input), StandardCharsets.UTF_8)
            } catch (_: Throwable) {
                "decode failed"
            }
        }) { Text("B64 Decode") }
        IdeaDefaultButton(onClick = {
            output = input.toByteArray(StandardCharsets.UTF_8).joinToString("") { "%02x".format(it) }
        }) { Text("HEX") }
    }
    Text(output)
}

@Composable
private fun PartitionPane() {
    var partSize by remember { mutableStateOf(DatabaseManager.PART_SIZE.toString()) }
    var status by remember { mutableStateOf("current: ${DatabaseManager.PART_SIZE}") }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("PART_SIZE")
        BoundTextField(partSize, { partSize = it }, Modifier.width(140.dp))
        IdeaDefaultButton(onClick = {
            val value = partSize.trim().toIntOrNull()
            if (value == null) {
                status = "invalid integer"
                return@IdeaDefaultButton
            }
            val bounded = min(5000, max(50, value))
            DatabaseManager.PART_SIZE = bounded
            System.setProperty("jar-analyzer.db.batch", bounded.toString())
            status = "applied: $bounded"
            partSize = bounded.toString()
        }) {
            Text("Apply")
        }
    }
    Text(status)
}

@Composable
private fun ProxyPane() {
    var httpHost by remember { mutableStateOf(System.getProperty("http.proxyHost") ?: "") }
    var httpPort by remember { mutableStateOf(System.getProperty("http.proxyPort") ?: "") }
    var socksHost by remember { mutableStateOf(System.getProperty("socksProxyHost") ?: "") }
    var socksPort by remember { mutableStateOf(System.getProperty("socksProxyPort") ?: "") }
    var status by remember { mutableStateOf("ready") }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BoundTextField(httpHost, { httpHost = it }, Modifier.fillMaxWidth())
        BoundTextField(httpPort, { httpPort = it }, Modifier.width(160.dp))
        BoundTextField(socksHost, { socksHost = it }, Modifier.fillMaxWidth())
        BoundTextField(socksPort, { socksPort = it }, Modifier.width(160.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IdeaDefaultButton(onClick = {
                applyProxy("http.proxyHost", "http.proxyPort", httpHost, httpPort)
                applyProxy("https.proxyHost", "https.proxyPort", httpHost, httpPort)
                applyProxy("socksProxyHost", "socksProxyPort", socksHost, socksPort)
                status = "proxy updated"
            }) { Text("Apply") }
            IdeaOutlinedButton(onClick = {
                clearProxy("http.proxyHost", "http.proxyPort")
                clearProxy("https.proxyHost", "https.proxyPort")
                clearProxy("socksProxyHost", "socksProxyPort")
                httpHost = ""
                httpPort = ""
                socksHost = ""
                socksPort = ""
                status = "proxy cleared"
            }) { Text("Clear") }
        }
        Text(status)
    }
}

@Composable
private fun ExportPane() {
    var outputPath by remember {
        mutableStateOf("jar-analyzer-export-${System.currentTimeMillis()}.txt")
    }
    var status by remember { mutableStateOf("ready") }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BoundTextField(outputPath, { outputPath = it }, Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IdeaDefaultButton(onClick = {
                val doc = RuntimeFacades.editor().current()
                if (doc.content().isBlank()) {
                    status = "editor has no content"
                    return@IdeaDefaultButton
                }
                try {
                    val path = Paths.get(outputPath).toAbsolutePath()
                    path.parent?.let { Files.createDirectories(it) }
                    Files.writeString(path, doc.content())
                    status = "exported: $path"
                } catch (ex: Throwable) {
                    status = "export failed: ${ex.message}"
                }
            }) { Text("Export Editor") }
            IdeaOutlinedButton(onClick = {
                status = "current input: ${RuntimeFacades.build().snapshot().settings().inputPath()}"
            }) { Text("Show Build Input") }
        }
        Text(status)
    }
}

@Composable
private fun RemoteLoadPane() {
    var url by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("ready") }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BoundTextField(url, { url = it }, Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IdeaDefaultButton(onClick = {
                val target = downloadRemote(url.trim())
                if (target == null) {
                    status = "download failed"
                    return@IdeaDefaultButton
                }
                val old = RuntimeFacades.build().snapshot().settings()
                RuntimeFacades.build().apply(
                    BuildSettingsDto(
                        target.toString(),
                        old.runtimePath(),
                        old.resolveNestedJars(),
                        old.autoFindRuntimeJar(),
                        old.addRuntimeJar(),
                        old.deleteTempBeforeBuild(),
                        old.fixClassPath(),
                        old.fixMethodImpl(),
                        old.quickMode(),
                    ),
                )
                status = "downloaded & set build input: $target"
            }) { Text("Download + Use") }
            IdeaOutlinedButton(onClick = {
                RuntimeFacades.build().startBuild()
                status = "build started"
            }) { Text("Build") }
        }
        Text(status)
    }
}

@Composable
private fun SystemMonitorPane() {
    var status by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        while (true) {
            status = systemStatusText()
            delay(1000)
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(status)
    }
}

@Composable
private fun ChainsAdvancedPane() {
    val initial = remember { RuntimeFacades.chains().snapshot().settings() }
    var blacklist by remember { mutableStateOf(initial.blacklist()) }
    var minEdgeConfidence by remember { mutableStateOf(initial.minEdgeConfidence()) }
    var showEdgeMeta by remember { mutableStateOf(initial.showEdgeMeta()) }
    var summaryEnabled by remember { mutableStateOf(initial.summaryEnabled()) }
    var taintSeedParam by remember {
        mutableStateOf(initial.taintSeedParam()?.toString() ?: "")
    }
    var taintSeedStrict by remember { mutableStateOf(initial.taintSeedStrict()) }
    var maxResultLimit by remember { mutableStateOf(initial.maxResultLimit().toString()) }
    var status by remember { mutableStateOf("ready") }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BoundTextField(blacklist, { blacklist = it }, Modifier.fillMaxWidth())
        BoundTextField(minEdgeConfidence, { minEdgeConfidence = it }, Modifier.width(180.dp))
        CheckboxRow("show edge meta", showEdgeMeta, { showEdgeMeta = it })
        CheckboxRow("summary enabled", summaryEnabled, { summaryEnabled = it })
        CheckboxRow("taint seed strict", taintSeedStrict, { taintSeedStrict = it })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("taint seed param")
            BoundTextField(taintSeedParam, { taintSeedParam = it }, Modifier.width(140.dp))
            Text("max results")
            BoundTextField(maxResultLimit, { maxResultLimit = it }, Modifier.width(140.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IdeaDefaultButton(onClick = {
                val old = RuntimeFacades.chains().snapshot().settings()
                val resultLimit = maxResultLimit.trim().toIntOrNull()?.coerceIn(1, 2000) ?: old.maxResultLimit()
                val seedParam = taintSeedParam.trim().toIntOrNull()
                RuntimeFacades.chains().apply(
                    ChainsSettingsDto(
                        old.sinkSelected(),
                        old.sourceSelected(),
                        old.sinkClass(),
                        old.sinkMethod(),
                        old.sinkDesc(),
                        old.sourceClass(),
                        old.sourceMethod(),
                        old.sourceDesc(),
                        old.sourceNull(),
                        old.sourceEnabled(),
                        old.maxDepth(),
                        old.onlyFromWeb(),
                        old.taintEnabled(),
                        blacklist,
                        minEdgeConfidence.ifBlank { "low" },
                        showEdgeMeta,
                        summaryEnabled,
                        seedParam,
                        taintSeedStrict,
                        resultLimit,
                    ),
                )
                status = "advanced settings applied"
            }) { Text("Apply") }
            IdeaOutlinedButton(onClick = {
                RuntimeFacades.chains().startDfs()
                status = "dfs started"
            }) { Text("Start DFS") }
        }
        Text(status)
    }
}

@Composable
private fun SqlConsolePane() {
    var sql by remember { mutableStateOf("select count(1) as c from class_file_table;") }
    var output by remember { mutableStateOf("ready") }
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BoundTextField(sql, { sql = it }, Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IdeaDefaultButton(onClick = { output = runSql(sql) }) { Text("Execute") }
            IdeaOutlinedButton(onClick = { sql = "select * from class_file_table limit 20;" }) { Text("Sample") }
        }
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        ) {
            Text(output)
        }
    }
}

@Composable
private fun ElSearchPane() {
    var keyword by remember { mutableStateOf("\${") }
    var status by remember { mutableStateOf("ready") }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BoundTextField(keyword, { keyword = it }, Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IdeaDefaultButton(onClick = {
                RuntimeFacades.search().applyQuery(
                    SearchQueryDto(
                        SearchMode.STRING_CONTAINS,
                        SearchMatchMode.LIKE,
                        "",
                        "",
                        keyword,
                        false,
                    ),
                )
                RuntimeFacades.search().runSearch()
                status = "search submitted"
            }) { Text("Search") }
            IdeaOutlinedButton(onClick = {
                status = "result size: ${RuntimeFacades.search().snapshot().results().size}"
            }) { Text("Refresh") }
        }
        Text(status)
    }
}

@Composable
private fun RepeaterPane() {
    var method by remember { mutableStateOf("GET") }
    var url by remember { mutableStateOf("http://127.0.0.1:10032/api/status") }
    var headers by remember { mutableStateOf("Authorization: Bearer token") }
    var body by remember { mutableStateOf("") }
    var response by remember { mutableStateOf("ready") }
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            BoundTextField(method, { method = it }, Modifier.width(100.dp))
            BoundTextField(url, { url = it }, Modifier.weight(1f))
            IdeaDefaultButton(onClick = {
                response = sendHttp(method, url, headers, body)
            }) { Text("Send") }
        }
        BoundTextField(headers, { headers = it }, Modifier.fillMaxWidth())
        BoundTextField(body, { body = it }, Modifier.fillMaxWidth().heightIn(min = 80.dp, max = 160.dp))
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Text(response)
        }
    }
}

@Composable
private fun SerializationPane() {
    var input by remember { mutableStateOf("") }
    var b64 by remember { mutableStateOf("") }
    var hex by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("ready") }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BoundTextField(input, { input = it }, Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IdeaDefaultButton(onClick = {
                val bytes = input.toByteArray(StandardCharsets.UTF_8)
                b64 = Base64.getEncoder().encodeToString(bytes)
                hex = bytes.joinToString("") { "%02x".format(it) }
                status = "encoded"
            }) { Text("Encode") }
            IdeaOutlinedButton(onClick = {
                input = try {
                    String(Base64.getDecoder().decode(b64), StandardCharsets.UTF_8)
                } catch (ex: Throwable) {
                    status = "base64 decode failed: ${ex.message}"
                    input
                }
            }) { Text("Decode B64") }
            IdeaOutlinedButton(onClick = {
                input = try {
                    String(hexToBytes(hex), StandardCharsets.UTF_8)
                } catch (ex: Throwable) {
                    status = "hex decode failed: ${ex.message}"
                    input
                }
            }) { Text("Decode HEX") }
        }
        BoundTextField(b64, { b64 = it }, Modifier.fillMaxWidth())
        BoundTextField(hex, { hex = it }, Modifier.fillMaxWidth())
        Text(status)
    }
}

@Composable
private fun ObfuscationPane() {
    var input by remember { mutableStateOf("") }
    var output by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BoundTextField(input, { input = it }, Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IdeaDefaultButton(onClick = {
                val encoded = Base64.getEncoder().encodeToString(input.toByteArray(StandardCharsets.UTF_8))
                output = "new String(java.util.Base64.getDecoder().decode(\"$encoded\"), java.nio.charset.StandardCharsets.UTF_8)"
            }) { Text("Generate") }
        }
        BoundTextField(output, { output = it }, Modifier.fillMaxWidth().heightIn(min = 80.dp, max = 180.dp))
    }
}

@Composable
private fun SocketListenerPane() {
    var host by remember { mutableStateOf("127.0.0.1") }
    var port by remember { mutableStateOf("10032") }
    var payload by remember { mutableStateOf("ping") }
    var result by remember { mutableStateOf("ready") }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            BoundTextField(host, { host = it }, Modifier.weight(1f))
            BoundTextField(port, { port = it }, Modifier.width(140.dp))
        }
        BoundTextField(payload, { payload = it }, Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IdeaDefaultButton(onClick = {
                val p = port.trim().toIntOrNull()
                if (p == null) {
                    result = "invalid port"
                    return@IdeaDefaultButton
                }
                result = socketSend(host, p, payload)
            }) { Text("Send") }
        }
        Text(result)
    }
}

@Composable
private fun BcelPane() {
    var status by remember { mutableStateOf("ready") }
    val doc = RuntimeFacades.editor().current()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("current class: ${doc.className()}")
        Text("method: ${doc.methodName()}${doc.methodDesc()}")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IdeaDefaultButton(onClick = {
                status = if (doc.content().isBlank()) {
                    "open a class first"
                } else {
                    "loaded ${doc.content().length} chars from editor"
                }
            }) { Text("Check") }
            IdeaOutlinedButton(onClick = {
                status = doc.statusText()
            }) { Text("Status") }
        }
        Text(status)
    }
}

@Composable
private fun FallbackPane(action: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("No dedicated panel yet for: $action")
        Text("The action is routed through ToolingFacade and no longer depends on Swing windows.")
    }
}

private fun chainSummary(item: ChainsResultItemDto): String {
    val taintPart = if (item.taint()) {
        if (item.taintPass()) "PASS" else "BLOCK"
    } else {
        "DFS"
    }
    return "#${item.index()} [$taintPart] depth=${item.depth()} path=${item.pathCount()} ${item.sink()}"
}

private fun exportChainsPayload(payload: ToolingWindowPayload.ChainsResultPayload): String {
    return try {
        val mode = if (payload.taintView()) "taint" else "dfs"
        val path = Paths.get("chains-$mode-${System.currentTimeMillis()}.txt").toAbsolutePath()
        val report = buildString {
            appendLine(payload.title())
            appendLine("total=${payload.items().size}")
            appendLine()
            payload.items().forEach { item ->
                appendLine(renderChainsItem(item))
                appendLine()
            }
            if (payload.items().isEmpty()) {
                appendLine(payload.emptyHint())
            }
        }
        Files.writeString(path, report, StandardCharsets.UTF_8)
        "exported: $path"
    } catch (ex: Throwable) {
        "export failed: ${ex.message}"
    }
}

private fun renderChainsItem(item: ChainsResultItemDto): String {
    return buildString {
        appendLine(chainSummary(item))
        appendLine("sink=${item.sink()}")
        appendLine("source=${item.source()}")
        appendLine(
            "depth=${item.depth()} path=${item.pathCount()} node=${item.nodeCount()} " +
                "edge=${item.edgeCount()} elapsedMs=${item.elapsedMs()}",
        )
        if (item.truncated()) {
            appendLine("truncated=${item.truncateReason()}")
        }
        if (item.recommend().isNotBlank()) {
            appendLine("recommend=${item.recommend()}")
        }
        if (item.taint()) {
            appendLine("taint=${if (item.taintPass()) "PASS" else "BLOCK"} lowConfidence=${item.lowConfidence()}")
            if (item.sanitizerDetail().isNotBlank()) {
                appendLine("sanitizer:")
                appendLine(item.sanitizerDetail())
            }
            if (item.taintDetail().isNotBlank()) {
                appendLine("detail:")
                appendLine(item.taintDetail())
            }
        }
        if (item.methods().isEmpty()) {
            appendLine("methods=<empty>")
        } else {
            appendLine("methods:")
            item.methods().forEachIndexed { index, method ->
                appendLine("${index + 1}. ${method.className()}#${method.methodName()}${method.methodDesc()}")
            }
        }
    }
}

private fun applyProxy(hostKey: String, portKey: String, host: String, port: String) {
    val safeHost = host.trim()
    val safePort = port.trim()
    if (safeHost.isEmpty() || safePort.isEmpty()) {
        System.clearProperty(hostKey)
        System.clearProperty(portKey)
        return
    }
    System.setProperty(hostKey, safeHost)
    System.setProperty(portKey, safePort)
}

private fun clearProxy(hostKey: String, portKey: String) {
    System.clearProperty(hostKey)
    System.clearProperty(portKey)
}

private fun downloadRemote(rawUrl: String): Path? {
    if (rawUrl.isBlank()) {
        return null
    }
    return try {
        val dir = Paths.get(Const.downDir).toAbsolutePath()
        Files.createDirectories(dir)
        val fileName = guessFileName(rawUrl)
        val target = dir.resolve(fileName)
        URL(rawUrl).openStream().use { input ->
            Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING)
        }
        target
    } catch (_: Throwable) {
        null
    }
}

private fun guessFileName(rawUrl: String): String {
    return try {
        val uri = URI(rawUrl)
        val path = uri.path ?: return "remote-${System.currentTimeMillis()}.jar"
        val name = path.substringAfterLast('/').ifBlank { "remote-${System.currentTimeMillis()}.jar" }
        if (name.contains('.')) {
            name
        } else {
            "$name.jar"
        }
    } catch (_: Throwable) {
        "remote-${System.currentTimeMillis()}.jar"
    }
}

private fun systemStatusText(): String {
    val rt = Runtime.getRuntime()
    val totalMb = rt.totalMemory() / (1024 * 1024)
    val freeMb = rt.freeMemory() / (1024 * 1024)
    val usedMb = totalMb - freeMb
    val maxMb = rt.maxMemory() / (1024 * 1024)
    val load = java.lang.management.ManagementFactory.getOperatingSystemMXBean().systemLoadAverage
    return "cpu-load=${"%.2f".format(load)} mem-used=${usedMb}MB total=${totalMb}MB max=${maxMb}MB"
}

private fun runSql(sql: String): String {
    val statement = sql.trim()
    if (statement.isEmpty()) {
        return "sql is empty"
    }
    return try {
        DriverManager.getConnection("jdbc:sqlite:${Const.dbFile}").use { conn ->
            conn.createStatement().use { stmt ->
                val hasResult = stmt.execute(statement)
                if (!hasResult) {
                    "updated rows: ${stmt.updateCount}"
                } else {
                    val rs = stmt.resultSet
                    rs.use {
                        val meta = it.metaData
                        val cols = meta.columnCount
                        val header = (1..cols).joinToString(" | ") { idx -> meta.getColumnLabel(idx) }
                        val sb = StringBuilder()
                        sb.append(header).append('\n')
                        var count = 0
                        while (it.next() && count < 200) {
                            val row = (1..cols).joinToString(" | ") { idx ->
                                it.getString(idx) ?: "null"
                            }
                            sb.append(row).append('\n')
                            count++
                        }
                        if (count == 200 && it.next()) {
                            sb.append("... more rows truncated ...")
                        }
                        sb.toString()
                    }
                }
            }
        }
    } catch (ex: Throwable) {
        "sql failed: ${ex.message}"
    }
}

private fun sendHttp(method: String, rawUrl: String, rawHeaders: String, body: String): String {
    if (rawUrl.isBlank()) {
        return "url is empty"
    }
    return try {
        val conn = (URL(rawUrl).openConnection() as HttpURLConnection)
        conn.requestMethod = method.trim().uppercase().ifBlank { "GET" }
        conn.connectTimeout = 8_000
        conn.readTimeout = 15_000
        parseHeaders(rawHeaders).forEach { (k, v) ->
            conn.setRequestProperty(k, v)
        }
        if (conn.requestMethod != "GET" && body.isNotEmpty()) {
            conn.doOutput = true
            conn.outputStream.use { out ->
                out.write(body.toByteArray(StandardCharsets.UTF_8))
            }
        }
        val code = conn.responseCode
        val stream = if (code >= 400) conn.errorStream else conn.inputStream
        val responseBody = stream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() } ?: ""
        val headers = conn.headerFields.entries
            .filter { it.key != null }
            .joinToString("\n") { "${it.key}: ${it.value.joinToString("; ")}" }
        "HTTP $code\n$headers\n\n$responseBody"
    } catch (ex: Throwable) {
        "http failed: ${ex.message}"
    }
}

private fun parseHeaders(raw: String): Map<String, String> {
    val out = linkedMapOf<String, String>()
    raw.lineSequence().forEach { line ->
        val idx = line.indexOf(':')
        if (idx <= 0) {
            return@forEach
        }
        val key = line.substring(0, idx).trim()
        val value = line.substring(idx + 1).trim()
        if (key.isNotEmpty()) {
            out[key] = value
        }
    }
    return out
}

private fun socketSend(host: String, port: Int, payload: String): String {
    return try {
        Socket(host, port).use { socket ->
            socket.getOutputStream().use { out ->
                out.write(payload.toByteArray(StandardCharsets.UTF_8))
                out.flush()
            }
            val response = socket.getInputStream().bufferedReader(StandardCharsets.UTF_8).readLine()
            "response: ${response ?: "<empty>"}"
        }
    } catch (ex: Throwable) {
        "socket failed: ${ex.message}"
    }
}

private fun hexToBytes(value: String): ByteArray {
    val safe = value.trim().replace(" ", "").replace("\n", "")
    require(safe.length % 2 == 0) { "hex length must be even" }
    return ByteArray(safe.length / 2) { idx ->
        safe.substring(idx * 2, idx * 2 + 2).toInt(16).toByte()
    }
}

private fun readMarkdown(path: String): String {
    val safe = path.trim()
    if (safe.isEmpty()) {
        return "No markdown path provided."
    }
    return try {
        val p = Paths.get(safe)
        if (!Files.exists(p)) {
            "Markdown file not found: $safe"
        } else {
            Files.readString(p)
        }
    } catch (ex: Throwable) {
        "Read markdown failed: ${ex.message}"
    }
}
