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
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.n1ar4.jar.analyzer.core.DatabaseManager
import me.n1ar4.jar.analyzer.core.reference.ClassReference
import me.n1ar4.jar.analyzer.core.reference.MethodReference
import me.n1ar4.jar.analyzer.engine.CoreEngine
import me.n1ar4.jar.analyzer.engine.DecompileDispatcher
import me.n1ar4.jar.analyzer.engine.DecompileType
import me.n1ar4.jar.analyzer.engine.EngineContext
import me.n1ar4.jar.analyzer.engine.index.IndexEngine
import me.n1ar4.jar.analyzer.engine.index.IndexPluginsSupport
import me.n1ar4.jar.analyzer.gui.jewel.state.JewelThemeMode
import me.n1ar4.jar.analyzer.gui.runtime.api.RuntimeFacades
import me.n1ar4.jar.analyzer.gui.runtime.model.BuildSettingsDto
import me.n1ar4.jar.analyzer.gui.runtime.model.ChainsResultItemDto
import me.n1ar4.jar.analyzer.gui.runtime.model.ChainsSettingsDto
import me.n1ar4.jar.analyzer.gui.runtime.model.GadgetSettingsDto
import me.n1ar4.jar.analyzer.gui.runtime.model.ScaSettingsDto
import me.n1ar4.jar.analyzer.gui.runtime.model.SearchResultDto
import me.n1ar4.jar.analyzer.gui.runtime.model.ToolingWindowAction
import me.n1ar4.jar.analyzer.gui.runtime.model.ToolingWindowPayload
import me.n1ar4.jar.analyzer.gui.runtime.model.ToolingWindowRequest
import me.n1ar4.jar.analyzer.starter.Const
import me.n1ar4.jar.analyzer.utils.JarUtil
import org.apache.bcel.classfile.Utility
import org.jetbrains.jewel.intui.standalone.theme.IntUiTheme
import org.jetbrains.jewel.ui.Orientation
import org.jetbrains.jewel.ui.component.CheckboxRow
import org.jetbrains.jewel.ui.component.Divider
import org.jetbrains.jewel.ui.component.RadioButtonRow
import org.jetbrains.jewel.ui.component.Text
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.expression.spel.support.StandardEvaluationContext
import java.io.BufferedWriter
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.ObjectStreamClass
import java.io.ObjectStreamConstants
import java.io.ObjectStreamField
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.net.ServerSocket
import java.net.Socket
import java.net.URI
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.nio.file.DirectoryStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.sql.DriverManager
import java.util.Base64
import java.util.LinkedHashMap
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException
import javax.swing.JFileChooser
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
    private val rememberedSizes = ConcurrentHashMap<ToolingWindowAction, DpSize>()
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

    fun initialSize(action: ToolingWindowAction): DpSize {
        return rememberedSizes[action] ?: DpSize(980.dp, 700.dp)
    }

    fun rememberSize(action: ToolingWindowAction, size: DpSize) {
        if (size.width.value > 1f && size.height.value > 1f) {
            rememberedSizes[action] = size
        }
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
        val initial = remember(window.action) { ToolingWindowsState.initialSize(window.action) }
        val windowState = rememberWindowState(width = initial.width, height = initial.height)
        Window(
            title = window.title,
            state = windowState,
            onCloseRequest = { ToolingWindowsState.close(window.id) },
        ) {
            val minSize = DpSize(420.dp, 280.dp)
            LaunchedEffect(windowState.size) {
                val clampedSize = DpSize(
                    width = if (windowState.size.width < minSize.width) minSize.width else windowState.size.width,
                    height = if (windowState.size.height < minSize.height) minSize.height else windowState.size.height,
                )
                if (clampedSize != windowState.size) {
                    windowState.size = clampedSize
                }
                ToolingWindowsState.rememberSize(window.action, clampedSize)
            }
            IntUiTheme(isDark = isDark, swingCompatMode = false) {
                ToolWindowContent(window)
            }
        }
    }
}

@Composable
private fun ToolWindowContent(window: ToolWindowState) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(window.title)
        Divider(orientation = Orientation.Horizontal, modifier = Modifier.fillMaxWidth())

        when (window.action) {
            ToolingWindowAction.MARKDOWN_VIEWER -> MarkdownViewerPane(window)
            ToolingWindowAction.GLOBAL_SEARCH -> GlobalSearchPane()
            ToolingWindowAction.OPCODE,
            ToolingWindowAction.ASM,
            ToolingWindowAction.CFG,
            ToolingWindowAction.FRAME,
            ToolingWindowAction.TEXT_VIEWER -> TextPayloadPane(window)
            ToolingWindowAction.BCEL_TOOL -> BcelPane()
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
    var status by remember { mutableStateOf(i18n("就绪", "ready")) }
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(i18n("输入", "Input"))
        BoundTextField(input, { input = it }, Modifier.fillMaxWidth().heightIn(min = 90.dp, max = 200.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IdeaDefaultButton(onClick = {
                output = Base64.getEncoder().encodeToString(input.toByteArray(StandardCharsets.UTF_8))
                status = "base64 -> ok"
            }) { Text("Base64 +") }
            IdeaOutlinedButton(onClick = {
                output = try {
                    String(Base64.getDecoder().decode(input.trim()), StandardCharsets.UTF_8)
                } catch (ex: Throwable) {
                    status = "base64 - fail: ${ex.message}"
                    output
                }
            }) { Text("Base64 -") }
            IdeaDefaultButton(onClick = {
                output = URLEncoder.encode(input, StandardCharsets.UTF_8)
                status = "url -> ok"
            }) { Text("URL +") }
            IdeaOutlinedButton(onClick = {
                output = try {
                    URLDecoder.decode(input, StandardCharsets.UTF_8)
                } catch (ex: Throwable) {
                    status = "url - fail: ${ex.message}"
                    output
                }
            }) { Text("URL -") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IdeaDefaultButton(onClick = {
                output = md5Legacy(input)
                status = "md5 -> ok"
            }) { Text("MD5") }
            IdeaDefaultButton(onClick = {
                output = toBashCommand(input)
                status = "bash cmd -> ok"
            }) { Text("Bash CMD") }
            IdeaDefaultButton(onClick = {
                output = toPowerShellCommand(input)
                status = "powershell cmd -> ok"
            }) { Text("Powershell CMD") }
            IdeaDefaultButton(onClick = {
                output = toStringFromCharCode(input)
                status = "String.fromCharCode -> ok"
            }) { Text("StringCmd") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IdeaOutlinedButton(onClick = {
                output = input.toByteArray(StandardCharsets.UTF_8).joinToString("") { "%02x".format(it) }
                status = "hex -> ok"
            }) { Text("HEX +") }
            IdeaOutlinedButton(onClick = {
                output = try {
                    String(hexToBytes(input), StandardCharsets.UTF_8)
                } catch (ex: Throwable) {
                    status = "hex - fail: ${ex.message}"
                    output
                }
            }) { Text("HEX -") }
            IdeaOutlinedButton(onClick = {
                input = ""
                output = ""
                status = i18n("已清空", "cleared")
            }) { Text(i18n("清空", "Clear")) }
        }
        Text(i18n("输出", "Output"))
        BoundTextField(output, { output = it }, Modifier.fillMaxWidth().heightIn(min = 90.dp, max = 240.dp))
        Text(status)
    }
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
    var outputDir by remember { mutableStateOf("jar-analyzer-export") }
    var jarsText by remember { mutableStateOf("") }
    var useCfr by remember { mutableStateOf(false) }
    var nestedLib by remember { mutableStateOf(false) }
    var running by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf(i18n("就绪", "ready")) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val prefill = withContext(Dispatchers.IO) {
            val engine = EngineContext.getEngine()
            if (engine == null || !engine.isEnabled()) {
                ""
            } else {
                engine.getJarsPath().joinToString("\n")
            }
        }
        if (prefill.isNotBlank()) {
            jarsText = prefill
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("OUTPUT DIR")
        BoundTextField(outputDir, { outputDir = it }, Modifier.fillMaxWidth())
        Text("DECOMPILE JAR/DIR")
        BoundTextField(jarsText, { jarsText = it }, Modifier.fillMaxWidth().heightIn(min = 110.dp, max = 240.dp))
        Text(i18n("换行输入多个 jar；单个目录会递归收集 .jar", "one jar per line; a single directory is scanned recursively"))
        Text("ENGINE")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RadioButtonRow("FernFlower", !useCfr, { useCfr = false })
            RadioButtonRow("CFR", useCfr, { useCfr = true })
        }
        CheckboxRow(
            "Decompile nested lib jars (BOOT-INF/lib / WEB-INF/lib / lib)",
            nestedLib,
            { nestedLib = it },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IdeaDefaultButton(
                enabled = !running,
                onClick = {
                    val output = outputDir.trim()
                    if (output.isEmpty()) {
                        status = i18n("请填写输出目录", "please enter output directory")
                        return@IdeaDefaultButton
                    }
                    val jars = collectExportJars(jarsText)
                    if (jars.isEmpty()) {
                        status = i18n("未找到可用 jar 文件", "no jar files found")
                        return@IdeaDefaultButton
                    }
                    scope.launch {
                        running = true
                        status = i18n("导出中...", "exporting...")
                        val type = if (useCfr) DecompileType.CFR else DecompileType.FERNFLOWER
                        val result = withContext(Dispatchers.IO) {
                            try {
                                val ok = DecompileDispatcher.decompileJars(jars, output, type, nestedLib)
                                ok to null
                            } catch (ex: Throwable) {
                                false to (ex.message ?: ex.toString())
                            }
                        }
                        status = if (result.first) {
                            i18n("导出完成", "export finished")
                        } else {
                            "${i18n("导出失败", "export failed")}: ${result.second ?: i18n("请检查输入与日志", "check input and logs")}"
                        }
                        running = false
                    }
                },
            ) {
                Text(if (running) i18n("运行中", "Running") else "START EXPORT")
            }
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
    val templates = remember { elTemplates() }
    val initialTemplate = remember(templates) { templates.entries.firstOrNull() }
    var expression by remember { mutableStateOf(initialTemplate?.value ?: "#method") }
    var selectedTemplate by remember { mutableStateOf(initialTemplate?.key ?: "") }
    var showTemplates by remember { mutableStateOf(false) }
    var running by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var status by remember { mutableStateOf(i18n("就绪", "ready")) }
    var localCount by remember { mutableStateOf(0) }
    var canJump by remember { mutableStateOf(false) }
    val stopFlag = remember { AtomicBoolean(false) }
    var searchJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(i18n("内置模板", "Templates"))
            IdeaOutlinedButton(onClick = { showTemplates = !showTemplates }) {
                Text(if (selectedTemplate.isBlank()) i18n("选择模板", "Select Template") else selectedTemplate)
            }
            IdeaOutlinedButton(onClick = {
                val temp = templates[selectedTemplate]
                if (temp != null) {
                    expression = temp
                    status = i18n("模板已应用", "template applied")
                }
            }) { Text(i18n("应用", "Apply")) }
        }
        if (showTemplates) {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp, max = 180.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                templates.keys.forEach { name ->
                    Text(
                        name,
                        modifier = Modifier.fillMaxWidth().clickable {
                            selectedTemplate = name
                            showTemplates = false
                        }.padding(4.dp),
                    )
                }
            }
        }
        BoundTextField(
            expression,
            { expression = it },
            Modifier.fillMaxWidth().heightIn(min = 220.dp, max = 360.dp),
        )
        org.jetbrains.jewel.ui.component.HorizontalProgressBar(progress.coerceIn(0f, 1f), Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IdeaOutlinedButton(onClick = {
                status = validateElExpression(expression)
            }) {
                Text(i18n("校验", "Validate"))
            }
            IdeaDefaultButton(
                enabled = !running,
                onClick = {
                    if (running) {
                        return@IdeaDefaultButton
                    }
                    val validate = validateElExpression(expression)
                    if (validate.contains("error", true) || validate.contains("异常")) {
                        status = validate
                        return@IdeaDefaultButton
                    }
                    val engine = EngineContext.getEngine()
                    if (engine == null || !engine.isEnabled()) {
                        status = i18n("引擎未就绪", "engine is not ready")
                        return@IdeaDefaultButton
                    }
                    running = true
                    stopFlag.set(false)
                    progress = 0f
                    localCount = 0
                    canJump = false
                    status = i18n("开始搜索...", "search started...")
                    searchJob = scope.launch {
                        val result = runElSearch(
                            expression = expression,
                            stopFlag = stopFlag,
                            onProgress = { processed, total ->
                                val safeTotal = if (total <= 0L) 1L else total
                                progress = (processed.toFloat() / safeTotal.toFloat()).coerceIn(0f, 1f)
                                status = "${i18n("已处理", "processed")} $processed/$total"
                            },
                        )
                        running = false
                        progress = 1f
                        localCount = result.results.size
                        canJump = result.results.isNotEmpty()
                        RuntimeFacades.search().publishExternalResults(result.results, result.statusText)
                        status = result.statusText
                    }
                },
            ) {
                Text(i18n("搜索", "Search"))
            }
            IdeaOutlinedButton(
                enabled = running,
                onClick = {
                    stopFlag.set(true)
                    searchJob?.cancel()
                    status = i18n("正在停止...", "stopping...")
                },
            ) {
                Text(i18n("停止", "Stop"))
            }
            IdeaOutlinedButton(
                enabled = canJump,
                onClick = {
                    RuntimeFacades.search().openResult(0)
                    status = i18n("已跳转到首个结果", "jumped to first result")
                },
            ) {
                Text(i18n("跳转首个结果", "Jump First"))
            }
        }
        Text("${i18n("结果数", "results")}: $localCount")
        Text(status)
    }
}

@Composable
private fun RepeaterPane() {
    var targetIp by remember { mutableStateOf("127.0.0.1") }
    var targetPort by remember { mutableStateOf("80") }
    var request by remember {
        mutableStateOf(
            "GET / HTTP/1.1\r\nHost: 127.0.0.1\r\nConnection: close\r\n\r\n",
        )
    }
    var response by remember { mutableStateOf("") }
    var status by remember { mutableStateOf(i18n("就绪", "ready")) }
    var running by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(i18n("Target IP", "Target IP"))
            BoundTextField(targetIp, { targetIp = it }, Modifier.weight(1f))
            Text(i18n("Target Port", "Target Port"))
            BoundTextField(targetPort, { targetPort = it }, Modifier.width(140.dp))
            IdeaDefaultButton(
                enabled = !running,
                onClick = {
                    val port = targetPort.trim().toIntOrNull()
                    if (port == null || port !in 1..65535) {
                        status = i18n("端口无效", "invalid port")
                        return@IdeaDefaultButton
                    }
                    running = true
                    status = i18n("发送中...", "sending...")
                    scope.launch {
                        val result = withContext(Dispatchers.IO) {
                            sendRawSocketRequest(targetIp.trim(), port, request)
                        }
                        response = result.response
                        status = result.status
                        running = false
                    }
                },
            ) { Text(i18n("发送", "Send")) }
        }
        Text(i18n("Request", "Request"))
        BoundTextField(request, { request = it }, Modifier.fillMaxWidth().heightIn(min = 180.dp, max = 320.dp))
        Text(i18n("Response", "Response"))
        BoundTextField(response, { response = it }, Modifier.fillMaxWidth().heightIn(min = 180.dp, max = 360.dp))
        Text(status)
    }
}

@Composable
private fun SerializationPane() {
    var filePath by remember { mutableStateOf("") }
    var dataText by remember { mutableStateOf("") }
    var useHex by remember { mutableStateOf(true) }
    var running by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf(i18n("就绪", "ready")) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("file path")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            BoundTextField(filePath, { filePath = it }, Modifier.weight(1f))
            IdeaOutlinedButton(onClick = {
                chooseFilePath(filePath)?.let { filePath = it }
            }) { Text(i18n("选择文件", "Choose File")) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RadioButtonRow("HEX", useHex, { useHex = true })
            RadioButtonRow("BASE64", !useHex, { useHex = false })
        }
        BoundTextField(dataText, { dataText = it }, Modifier.fillMaxWidth().heightIn(min = 180.dp, max = 360.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IdeaDefaultButton(
                enabled = !running,
                onClick = {
                    running = true
                    status = i18n("分析中...", "analyzing...")
                    scope.launch {
                        val result = withContext(Dispatchers.IO) {
                            analyzeSerialization(filePath, dataText, useHex)
                        }
                        if (result.displayData != null) {
                            dataText = result.displayData
                        }
                        if (result.code != null) {
                            RuntimeFacades.editor().applyEditorText(result.code, 0)
                        }
                        status = result.status
                        running = false
                    }
                },
            ) {
                Text(i18n("分析", "Analyze"))
            }
        }
        Text(status)
    }
}

@Composable
private fun ObfuscationPane() {
    var input by remember {
        mutableStateOf(
            "rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAB3CAAAABAAAAAAeA==",
        )
    }
    var output by remember { mutableStateOf("") }
    var status by remember { mutableStateOf(i18n("就绪", "ready")) }
    var running by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BoundTextField(input, { input = it }, Modifier.fillMaxWidth().heightIn(min = 160.dp, max = 280.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IdeaOutlinedButton(onClick = {
                input = ""
                output = ""
                status = i18n("已清空", "cleared")
            }) { Text("Clear") }
            IdeaDefaultButton(
                enabled = !running,
                onClick = {
                    val raw = input.trim()
                    if (raw.isEmpty()) {
                        status = i18n("输入不能为空", "input is empty")
                        return@IdeaDefaultButton
                    }
                    running = true
                    status = i18n("处理中...", "processing...")
                    scope.launch {
                        val result = withContext(Dispatchers.IO) {
                            obfuscateSerializedPayload(raw)
                        }
                        if (result.output != null) {
                            output = result.output
                        }
                        status = result.status
                        running = false
                    }
                },
            ) { Text("Execute") }
        }
        BoundTextField(output, { output = it }, Modifier.fillMaxWidth().heightIn(min = 160.dp, max = 280.dp))
        Text(status)
    }
}

@Composable
private fun SocketListenerPane() {
    var port by remember { mutableStateOf("10032") }
    var sendText by remember { mutableStateOf("") }
    var terminal by remember { mutableStateOf("") }
    var running by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf(i18n("就绪", "ready")) }
    var listenJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var serverSocket by remember { mutableStateOf<ServerSocket?>(null) }
    var clientSocket by remember { mutableStateOf<Socket?>(null) }
    var clientWriter by remember { mutableStateOf<BufferedWriter?>(null) }
    val scope = rememberCoroutineScope()

    fun appendTerminal(line: String) {
        terminal = if (terminal.isBlank()) line else "$terminal\n$line"
    }

    fun closeAllSockets() {
        try {
            clientWriter?.close()
        } catch (_: Throwable) {
        }
        try {
            clientSocket?.close()
        } catch (_: Throwable) {
        }
        try {
            serverSocket?.close()
        } catch (_: Throwable) {
        }
        clientWriter = null
        clientSocket = null
        serverSocket = null
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("port")
            BoundTextField(port, { port = it }, Modifier.width(140.dp))
            IdeaDefaultButton(onClick = {
                if (running) {
                    listenJob?.cancel()
                    closeAllSockets()
                    running = false
                    status = i18n("监听已停止", "listener stopped")
                    appendTerminal("[stop] listener stopped")
                    return@IdeaDefaultButton
                }
                val targetPort = port.trim().toIntOrNull()
                if (targetPort == null || targetPort !in 1..65535) {
                    status = i18n("端口无效", "invalid port")
                    return@IdeaDefaultButton
                }
                running = true
                terminal = ""
                status = "${i18n("监听中", "listening")} :$targetPort"
                listenJob = scope.launch(Dispatchers.IO) {
                    try {
                        val server = ServerSocket(targetPort)
                        withContext(Dispatchers.Main) {
                            serverSocket = server
                            appendTerminal("[start] listen port: $targetPort")
                        }
                        while (isActive) {
                            val socket = server.accept()
                            val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))
                            withContext(Dispatchers.Main) {
                                clientSocket = socket
                                clientWriter = writer
                                appendTerminal("[connect] ${socket.inetAddress.hostAddress}:${socket.port}")
                                status = i18n("客户端已连接", "client connected")
                            }
                            try {
                                val reader = socket.getInputStream().bufferedReader(StandardCharsets.UTF_8)
                                while (isActive) {
                                    val line = reader.readLine() ?: break
                                    withContext(Dispatchers.Main) {
                                        appendTerminal("[recv] $line")
                                    }
                                }
                            } finally {
                                try {
                                    writer.close()
                                } catch (_: Throwable) {
                                }
                                try {
                                    socket.close()
                                } catch (_: Throwable) {
                                }
                                withContext(Dispatchers.Main) {
                                    clientWriter = null
                                    clientSocket = null
                                    appendTerminal("[disconnect] client closed")
                                }
                            }
                        }
                    } catch (ex: Throwable) {
                        if (isActive) {
                            withContext(Dispatchers.Main) {
                                appendTerminal("[error] ${ex.message}")
                                status = "${i18n("监听失败", "listen failed")}: ${ex.message}"
                            }
                        }
                    } finally {
                        withContext(Dispatchers.Main) {
                            closeAllSockets()
                            running = false
                        }
                    }
                }
            }) {
                Text(if (running) i18n("Stop Listen", "Stop Listen") else i18n("Start Listen", "Start Listen"))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            BoundTextField(sendText, { sendText = it }, Modifier.weight(1f))
            IdeaOutlinedButton(onClick = {
                scope.launch(Dispatchers.IO) {
                    val writer = clientWriter
                    if (writer == null) {
                        withContext(Dispatchers.Main) {
                            appendTerminal("[send] not connected")
                            status = "not connected"
                        }
                        return@launch
                    }
                    try {
                        writer.write(sendText)
                        writer.flush()
                        withContext(Dispatchers.Main) {
                            appendTerminal("[send] $sendText")
                            status = i18n("发送成功", "sent")
                        }
                    } catch (ex: Throwable) {
                        withContext(Dispatchers.Main) {
                            appendTerminal("[send-error] ${ex.message}")
                            status = "${i18n("发送失败", "send failed")}: ${ex.message}"
                        }
                    }
                }
            }) { Text("Send") }
        }
        Text("terminal")
        BoundTextField(
            terminal,
            { terminal = it },
            Modifier.fillMaxWidth().heightIn(min = 240.dp, max = 420.dp),
            readOnly = true,
        )
        Text(status)
    }
}

@Composable
private fun BcelPane() {
    var payload by remember { mutableStateOf(BCEL_DEFAULT_PAYLOAD) }
    var status by remember { mutableStateOf(i18n("就绪", "ready")) }
    var running by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BoundTextField(payload, { payload = it }, Modifier.fillMaxWidth().heightIn(min = 260.dp, max = 420.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IdeaOutlinedButton(onClick = {
                payload = ""
                status = i18n("已清空", "cleaned")
            }) { Text("CLEAN") }
            IdeaOutlinedButton(
                enabled = !running,
                onClick = {
                    running = true
                    scope.launch {
                        val result = withContext(Dispatchers.IO) {
                            checkBcelPayload(payload)
                        }
                        status = result
                        running = false
                    }
                },
            ) { Text("CHECK") }
            IdeaDefaultButton(
                enabled = !running,
                onClick = {
                    running = true
                    status = i18n("反编译中...", "decompiling...")
                    scope.launch {
                        val result = withContext(Dispatchers.IO) {
                            decompileBcelPayload(payload)
                        }
                        status = result.status
                        if (result.code != null) {
                            RuntimeFacades.editor().applyEditorText(result.code, 0)
                        }
                        running = false
                    }
                },
            ) { Text("DECOMPILE") }
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

private fun i18n(zh: String, en: String): String {
    return if (RuntimeFacades.tooling().configSnapshot().language().equals("zh", ignoreCase = true)) {
        zh
    } else {
        en
    }
}

private fun chooseFilePath(current: String): String? {
    return try {
        val chooser = JFileChooser()
        chooser.fileSelectionMode = JFileChooser.FILES_ONLY
        chooser.isMultiSelectionEnabled = false
        val target = current.trim()
        if (target.isNotEmpty()) {
            val base = File(target)
            chooser.currentDirectory = if (base.isDirectory) base else base.parentFile
            chooser.selectedFile = base
        } else {
            chooser.currentDirectory = File(".")
        }
        val result = chooser.showOpenDialog(null)
        if (result == JFileChooser.APPROVE_OPTION) {
            chooser.selectedFile?.absolutePath
        } else {
            null
        }
    } catch (_: Throwable) {
        null
    }
}

private fun md5Legacy(input: String): String {
    val digest = MessageDigest.getInstance("MD5").digest(input.toByteArray(StandardCharsets.UTF_8))
    return BigInteger(1, digest).toString(16)
}

private fun toPowerShellCommand(cmd: String): String {
    val temp = ArrayList<Byte>()
    cmd.forEach { c ->
        c.toString().toByteArray(StandardCharsets.UTF_8).forEach { temp.add(it) }
        temp.add(0)
    }
    val bytes = ByteArray(temp.size)
    for (idx in temp.indices) {
        bytes[idx] = temp[idx]
    }
    val data = Base64.getEncoder().encodeToString(bytes)
    return "powershell.exe -NonI -W Hidden -NoP -Exec Bypass -Enc $data"
}

private fun toBashCommand(cmd: String): String {
    val data = Base64.getEncoder().encodeToString(cmd.toByteArray(StandardCharsets.UTF_8))
    return "bash -c {echo,$data}|{base64,-d}|{bash,-i}"
}

private fun toStringFromCharCode(cmd: String): String {
    val values = ArrayList<String>(cmd.length)
    for (idx in cmd.indices) {
        values.add(Character.codePointAt(cmd, idx).toString())
    }
    return "String.fromCharCode(${values.joinToString(",")})"
}

private fun collectExportJars(raw: String): List<String> {
    val input = raw.trim()
    if (input.isEmpty()) {
        return emptyList()
    }
    val out = LinkedHashSet<String>()
    if (input.contains("\n")) {
        input.lineSequence().forEach { line ->
            val item = line.trim()
            if (!item.lowercase(Locale.ROOT).endsWith(".jar")) {
                return@forEach
            }
            val path = Paths.get(item)
            if (Files.exists(path)) {
                out.add(path.toAbsolutePath().toString())
            }
        }
        return out.toList()
    }

    val target = Paths.get(input)
    if (Files.isDirectory(target)) {
        Files.walk(target).use { stream ->
            stream
                .filter { Files.isRegularFile(it) && it.fileName.toString().lowercase(Locale.ROOT).endsWith(".jar") }
                .forEach { out.add(it.toAbsolutePath().toString()) }
        }
    } else if (input.lowercase(Locale.ROOT).endsWith(".jar") && Files.exists(target)) {
        out.add(target.toAbsolutePath().toString())
    }
    return out.toList()
}

private data class RawSocketResult(
    val status: String,
    val response: String,
)

private fun sendRawSocketRequest(host: String, port: Int, request: String): RawSocketResult {
    if (host.isBlank()) {
        return RawSocketResult(i18n("目标 IP 不能为空", "target ip is empty"), "")
    }
    return try {
        Socket(host, port).use { socket ->
            socket.soTimeout = 15_000
            val payload = ensureRawRequestFormat(request)
            val writer = OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8)
            writer.write(payload)
            writer.flush()

            val input = socket.getInputStream()
            val header = StringBuilder()
            var contentLength = 0
            while (true) {
                val line = readRawLine(input, 0)
                if (line.isEmpty()) {
                    break
                }
                if (line.startsWith("Content-Length", ignoreCase = true)) {
                    contentLength = line.substringAfter(':', "0").trim().toIntOrNull() ?: 0
                }
                header.append(line)
                if (line == "\r\n" || line == "\n") {
                    break
                }
            }
            val body = if (contentLength > 0) readRawLine(input, contentLength) else ""
            RawSocketResult(i18n("发送成功", "sent"), header.toString() + body)
        }
    } catch (ex: Throwable) {
        RawSocketResult("${i18n("发送失败", "send failed")}: ${ex.message}", "")
    }
}

private fun ensureRawRequestFormat(request: String): String {
    val base = request.trimEnd()
    return if (base.endsWith("\r\n\r\n")) {
        base
    } else {
        "$base\r\n\r\n"
    }
}

private fun readRawLine(input: InputStream, contentLength: Int): String {
    val data = ArrayList<Byte>()
    return if (contentLength > 0) {
        repeat(contentLength) {
            val b = input.read()
            if (b == -1) {
                return@repeat
            }
            data.add(b.toByte())
        }
        data.toByteArray().toString(StandardCharsets.UTF_8)
    } else {
        while (true) {
            val b = input.read()
            if (b == -1) {
                break
            }
            data.add(b.toByte())
            if (b == 10) {
                break
            }
        }
        data.toByteArray().toString(StandardCharsets.UTF_8)
    }
}

private data class SerializationAnalyzeResult(
    val status: String,
    val displayData: String? = null,
    val code: String? = null,
)

private fun analyzeSerialization(filePath: String, dataText: String, useHex: Boolean): SerializationAnalyzeResult {
    return try {
        val bytes: ByteArray
        val displayData: String?
        val path = filePath.trim()
        if (path.isNotEmpty()) {
            val p = Paths.get(path)
            if (!Files.exists(p)) {
                return SerializationAnalyzeResult(i18n("文件不存在", "file not found"))
            }
            bytes = Files.readAllBytes(p)
            displayData = if (useHex) {
                bytes.joinToString("") { "%02x".format(it) }
            } else {
                Base64.getEncoder().encodeToString(bytes)
            }
        } else {
            val raw = dataText.trim()
            if (raw.isEmpty()) {
                return SerializationAnalyzeResult(i18n("数据为空", "data is empty"))
            }
            bytes = if (useHex) {
                hexToBytes(raw)
            } else {
                Base64.getDecoder().decode(raw)
            }
            displayData = null
        }

        val classBytes = analyzeSerializedClassBytes(bytes)
            ?: return SerializationAnalyzeResult(i18n("未发现可反编译的 class 字节", "class payload not found"), displayData)
        val target = Paths.get(Const.tempDir).resolve("test-ser.class")
        Files.createDirectories(target.parent)
        Files.write(target, classBytes)
        val code = DecompileDispatcher.decompile(target, DecompileDispatcher.resolvePreferred())
            ?: return SerializationAnalyzeResult(i18n("反编译失败", "decompile failed"), displayData)
        SerializationAnalyzeResult(i18n("分析完成，结果已写入主编辑器", "analyze complete, code pushed to editor"), displayData, code)
    } catch (ex: Throwable) {
        SerializationAnalyzeResult("${i18n("分析失败", "analyze failed")}: ${ex.message}")
    }
}

private fun analyzeSerializedClassBytes(bytes: ByteArray): ByteArray? {
    for (idx in 0 until bytes.size - 4) {
        if (bytes[idx] == 0xCA.toByte() &&
            bytes[idx + 1] == 0xFE.toByte() &&
            bytes[idx + 2] == 0xBA.toByte() &&
            bytes[idx + 3] == 0xBE.toByte()
        ) {
            if (idx >= 2) {
                val len = ((bytes[idx - 2].toInt() and 0xFF) shl 8) or (bytes[idx - 1].toInt() and 0xFF)
                val end = idx + 4 + len
                if (end <= bytes.size) {
                    return bytes.copyOfRange(idx, end)
                }
            }
        }
    }
    return null
}

private data class ObfuscationResult(
    val status: String,
    val output: String? = null,
)

private fun obfuscateSerializedPayload(base64: String): ObfuscationResult {
    return try {
        val decoded = try {
            Base64.getDecoder().decode(base64)
        } catch (_: Throwable) {
            return ObfuscationResult(i18n("输入必须是合法 base64", "input must be valid base64"))
        }
        val obj = try {
            ObjectInputStream(ByteArrayInputStream(decoded)).use { it.readObject() }
        } catch (ex: Throwable) {
            return ObfuscationResult("${i18n("反序列化失败", "deserialize failed")}: ${ex.message}")
        }
        val out = ByteArrayOutputStream()
        try {
            JewelCustomObjectOutputStream(out).use { oos ->
                oos.writeObject(obj)
                oos.flush()
            }
        } catch (ex: Throwable) {
            return ObfuscationResult("${i18n("重序列化失败", "re-serialize failed")}: ${ex.message}")
        }
        ObfuscationResult(i18n("执行成功", "execute success"), Base64.getEncoder().encodeToString(out.toByteArray()))
    } catch (ex: Throwable) {
        ObfuscationResult("${i18n("执行失败", "execute failed")}: ${ex.message}")
    }
}

private class JewelCustomObjectOutputStream(out: OutputStream) : ObjectOutputStream(out) {
    override fun writeClassDescriptor(desc: ObjectStreamClass) {
        val name = desc.name
        writeShort(name.length * 2)
        name.forEach { c ->
            val pair = CUSTOM_STREAM_MAP[c]
            if (pair != null) {
                write(pair[0])
                write(pair[1])
            } else {
                write((c.code ushr 8) and 0xFF)
                write(c.code and 0xFF)
            }
        }
        writeLong(desc.serialVersionUID)
        try {
            var flags: Byte = 0
            if ((getFieldValue(desc, "externalizable") as Boolean)) {
                flags = (flags.toInt() or ObjectStreamConstants.SC_EXTERNALIZABLE.toInt()).toByte()
                val protocolField = ObjectOutputStream::class.java.getDeclaredField("protocol")
                protocolField.isAccessible = true
                val protocol = protocolField.get(this) as Int
                if (protocol != ObjectStreamConstants.PROTOCOL_VERSION_1) {
                    flags = (flags.toInt() or ObjectStreamConstants.SC_BLOCK_DATA.toInt()).toByte()
                }
            } else if ((getFieldValue(desc, "serializable") as Boolean)) {
                flags = (flags.toInt() or ObjectStreamConstants.SC_SERIALIZABLE.toInt()).toByte()
            }
            if ((getFieldValue(desc, "hasWriteObjectData") as Boolean)) {
                flags = (flags.toInt() or ObjectStreamConstants.SC_WRITE_METHOD.toInt()).toByte()
            }
            if ((getFieldValue(desc, "isEnum") as Boolean)) {
                flags = (flags.toInt() or ObjectStreamConstants.SC_ENUM.toInt()).toByte()
            }
            writeByte(flags.toInt())
            val fields = getFieldValue(desc, "fields") as Array<ObjectStreamField>
            writeShort(fields.size)
            val writeTypeString = ObjectOutputStream::class.java.getDeclaredMethod("writeTypeString", String::class.java)
            writeTypeString.isAccessible = true
            fields.forEach { field ->
                writeByte(field.typeCode.code)
                writeUTF(field.name)
                if (!field.isPrimitive) {
                    writeTypeString.invoke(this, field.typeString)
                }
            }
        } catch (ex: Throwable) {
            throw IOException(ex.message, ex)
        }
    }
}

private fun getFieldValue(target: Any, name: String): Any? {
    val field = target.javaClass.getDeclaredField(name)
    field.isAccessible = true
    return field.get(target)
}

private val CUSTOM_STREAM_MAP: Map<Char, IntArray> = mapOf(
    '.' to intArrayOf(0xc0, 0xae),
    ';' to intArrayOf(0xc0, 0xbb),
    '$' to intArrayOf(0xc0, 0xa4),
    '[' to intArrayOf(0xc1, 0x9b),
    ']' to intArrayOf(0xc1, 0x9d),
    'a' to intArrayOf(0xc1, 0xa1),
    'b' to intArrayOf(0xc1, 0xa2),
    'c' to intArrayOf(0xc1, 0xa3),
    'd' to intArrayOf(0xc1, 0xa4),
    'e' to intArrayOf(0xc1, 0xa5),
    'f' to intArrayOf(0xc1, 0xa6),
    'g' to intArrayOf(0xc1, 0xa7),
    'h' to intArrayOf(0xc1, 0xa8),
    'i' to intArrayOf(0xc1, 0xa9),
    'j' to intArrayOf(0xc1, 0xaa),
    'k' to intArrayOf(0xc1, 0xab),
    'l' to intArrayOf(0xc1, 0xac),
    'm' to intArrayOf(0xc1, 0xad),
    'n' to intArrayOf(0xc1, 0xae),
    'o' to intArrayOf(0xc1, 0xaf),
    'p' to intArrayOf(0xc1, 0xb0),
    'q' to intArrayOf(0xc1, 0xb1),
    'r' to intArrayOf(0xc1, 0xb2),
    's' to intArrayOf(0xc1, 0xb3),
    't' to intArrayOf(0xc1, 0xb4),
    'u' to intArrayOf(0xc1, 0xb5),
    'v' to intArrayOf(0xc1, 0xb6),
    'w' to intArrayOf(0xc1, 0xb7),
    'x' to intArrayOf(0xc1, 0xb8),
    'y' to intArrayOf(0xc1, 0xb9),
    'z' to intArrayOf(0xc1, 0xba),
    'A' to intArrayOf(0xc1, 0x81),
    'B' to intArrayOf(0xc1, 0x82),
    'C' to intArrayOf(0xc1, 0x83),
    'D' to intArrayOf(0xc1, 0x84),
    'E' to intArrayOf(0xc1, 0x85),
    'F' to intArrayOf(0xc1, 0x86),
    'G' to intArrayOf(0xc1, 0x87),
    'H' to intArrayOf(0xc1, 0x88),
    'I' to intArrayOf(0xc1, 0x89),
    'J' to intArrayOf(0xc1, 0x8a),
    'K' to intArrayOf(0xc1, 0x8b),
    'L' to intArrayOf(0xc1, 0x8c),
    'M' to intArrayOf(0xc1, 0x8d),
    'N' to intArrayOf(0xc1, 0x8e),
    'O' to intArrayOf(0xc1, 0x8f),
    'P' to intArrayOf(0xc1, 0x90),
    'Q' to intArrayOf(0xc1, 0x91),
    'R' to intArrayOf(0xc1, 0x92),
    'S' to intArrayOf(0xc1, 0x93),
    'T' to intArrayOf(0xc1, 0x94),
    'U' to intArrayOf(0xc1, 0x95),
    'V' to intArrayOf(0xc1, 0x96),
    'W' to intArrayOf(0xc1, 0x97),
    'X' to intArrayOf(0xc1, 0x98),
    'Y' to intArrayOf(0xc1, 0x99),
    'Z' to intArrayOf(0xc1, 0x9a),
)

private class MethodElCondition {
    var classNameContains: String? = null
    var classNameNotContains: String? = null
    var nameContains: String? = null
    var nameNotContains: String? = null
    var startWith: String? = null
    var endWith: String? = null
    var returnType: String? = null
    var subClassOf: String? = null
    var superClassOf: String? = null
    var paramsNum: Int? = null
    var staticFlag: Boolean? = null
    var publicFlag: Boolean? = null
    var methodAnno: String? = null
    var excludedMethodAnno: String? = null
    var classAnno: String? = null
    var field: String? = null
    val paramTypes: MutableMap<Int, String> = LinkedHashMap()

    fun nameContains(str: String): MethodElCondition {
        nameContains = str
        return this
    }

    fun nameNotContains(str: String): MethodElCondition {
        nameNotContains = str
        return this
    }

    fun startWith(str: String): MethodElCondition {
        startWith = str
        return this
    }

    fun endWith(str: String): MethodElCondition {
        endWith = str
        return this
    }

    fun classNameContains(str: String): MethodElCondition {
        classNameContains = str
        return this
    }

    fun classNameNotContains(str: String): MethodElCondition {
        classNameNotContains = str
        return this
    }

    fun returnType(str: String): MethodElCondition {
        returnType = str
        return this
    }

    fun paramTypeMap(index: Int, type: String): MethodElCondition {
        paramTypes[index] = type
        return this
    }

    fun paramsNum(num: Int): MethodElCondition {
        paramsNum = num
        return this
    }

    fun isStatic(flag: Boolean): MethodElCondition {
        staticFlag = flag
        return this
    }

    fun isPublic(flag: Boolean): MethodElCondition {
        publicFlag = flag
        return this
    }

    fun isSubClassOf(type: String): MethodElCondition {
        subClassOf = type
        return this
    }

    fun isSuperClassOf(type: String): MethodElCondition {
        superClassOf = type
        return this
    }

    fun hasAnno(type: String): MethodElCondition {
        methodAnno = type
        return this
    }

    fun excludeAnno(type: String): MethodElCondition {
        excludedMethodAnno = type
        return this
    }

    fun hasClassAnno(type: String): MethodElCondition {
        classAnno = type
        return this
    }

    fun hasField(type: String): MethodElCondition {
        field = type
        return this
    }
}

private data class ElSearchResult(
    val results: List<SearchResultDto>,
    val statusText: String,
)

private fun elTemplates(): LinkedHashMap<String, String> {
    return linkedMapOf(
        "默认模板" to ("// 注意：过大的 JAR 文件可能非常耗时\n" +
            "#method\n" +
            "        .startWith(\"set\")\n" +
            "        .endWith(\"value\")\n" +
            "        .nameContains(\"lookup\")\n" +
            "        .nameNotContains(\"internal\")\n" +
            "        .classNameContains(\"Context\")\n" +
            "        .classNameNotContains(\"Abstract\")\n" +
            "        .returnType(\"java.lang.Process\")\n" +
            "        .paramTypeMap(0,\"java.lang.String\")\n" +
            "        .paramsNum(1)\n" +
            "        .isStatic(false)\n" +
            "        .isPublic(true)\n" +
            "        .isSubClassOf(\"java.awt.Component\")\n" +
            "        .isSuperClassOf(\"com.test.SomeClass\")\n" +
            "        .hasClassAnno(\"Controller\")\n" +
            "        .hasAnno(\"RequestMapping\")\n" +
            "        .excludeAnno(\"Auth\")\n" +
            "        .hasField(\"context\")"),
        "单 String 参数 public 构造方法" to ("// 历史参考 PGSQL Driver RCE (CVE-2022-21724)\n" +
            "#method\n" +
            "        .nameContains(\"<init>\")\n" +
            "        .paramTypeMap(0,\"java.lang.String\")\n" +
            "        .paramsNum(1)\n" +
            "        .isStatic(false)\n" +
            "        .isPublic(true)"),
        "搜索可能符合 Swing RCE 条件的方法" to ("// JDK CVE-2023-21939\n" +
            "#method\n" +
            "        .startWith(\"set\")\n" +
            "        .paramsNum(1)\n" +
            "        .isStatic(false)\n" +
            "        .isPublic(true)\n" +
            "        .paramTypeMap(0,\"java.lang.String\")\n" +
            "        .isSubClassOf(\"java.awt.Component\")"),
    )
}

private fun removeComments(code: String): String {
    return code.replace(Regex("(?m)^\\s*//.*$"), "")
}

private fun validateElExpression(expression: String): String {
    return try {
        parseElCondition(expression)
        i18n("解析通过，表达式合法", "expression is valid")
    } catch (ex: Throwable) {
        "${i18n("解析异常", "parse error")}: ${ex.message}"
    }
}

private fun parseElCondition(expression: String): MethodElCondition {
    val code = removeComments(expression).trim()
    require(code.isNotEmpty()) { i18n("表达式为空", "expression is empty") }
    val parser = SpelExpressionParser()
    val method = MethodElCondition()
    val ctx = StandardEvaluationContext()
    ctx.setVariable("method", method)
    val value = parser.parseExpression(code).getValue(ctx)
    require(value is MethodElCondition) { i18n("表达式必须返回 #method", "expression must return #method") }
    return value
}

private suspend fun runElSearch(
    expression: String,
    stopFlag: AtomicBoolean,
    onProgress: (Long, Long) -> Unit,
): ElSearchResult {
    val engine = EngineContext.getEngine()
        ?: return ElSearchResult(emptyList(), i18n("引擎未就绪", "engine is not ready"))
    if (!engine.isEnabled()) {
        return ElSearchResult(emptyList(), i18n("引擎未就绪", "engine is not ready"))
    }
    val condition = try {
        parseElCondition(expression)
    } catch (ex: Throwable) {
        return ElSearchResult(emptyList(), "${i18n("表达式错误", "expression error")}: ${ex.message}")
    }

    val classCache = ConcurrentHashMap<String, ClassReference?>()
    val superCache = ConcurrentHashMap<String, Set<ClassReference.Handle>>()
    val subCache = ConcurrentHashMap<String, Set<ClassReference.Handle>>()
    val total = engine.getMethodsCount().toLong()
    var offset = 0
    var processed = 0L
    val results = ArrayList<SearchResultDto>()
    while (offset < total && !stopFlag.get()) {
        val batch = withContext(Dispatchers.IO) { engine.getAllMethodRef(offset) }
        if (batch.isEmpty()) {
            break
        }
        offset += batch.size
        val chunkSize = max(1, batch.size / max(1, Runtime.getRuntime().availableProcessors()))
        val partial = coroutineScope {
            batch.chunked(chunkSize).map { chunk ->
                async(Dispatchers.Default) {
                    val local = ArrayList<SearchResultDto>()
                    chunk.forEach { method ->
                        if (stopFlag.get() || !isActive) {
                            return@forEach
                        }
                        if (matchElMethod(method, condition, engine, classCache, superCache, subCache)) {
                            local.add(method.toSearchResultDto())
                        }
                    }
                    local
                }
            }.awaitAll()
        }
        partial.forEach { results.addAll(it) }
        processed += batch.size
        onProgress(processed, total)
    }
    if (stopFlag.get()) {
        return ElSearchResult(results, "${i18n("搜索已停止", "search stopped")} (${results.size})")
    }
    return ElSearchResult(results, "${i18n("EL 结果", "el results")}: ${results.size}")
}

private fun matchElMethod(
    method: MethodReference,
    condition: MethodElCondition,
    engine: CoreEngine,
    classCache: ConcurrentHashMap<String, ClassReference?>,
    superCache: ConcurrentHashMap<String, Set<ClassReference.Handle>>,
    subCache: ConcurrentHashMap<String, Set<ClassReference.Handle>>,
): Boolean {
    val className = method.classReference.name
    val methodName = method.name

    if (!condition.classNameContains.isNullOrEmpty() && !className.contains(condition.classNameContains!!)) {
        return false
    }
    if (!condition.classNameNotContains.isNullOrEmpty() && className.contains(condition.classNameNotContains!!)) {
        return false
    }
    if (!condition.nameContains.isNullOrEmpty() && !methodName.contains(condition.nameContains!!)) {
        return false
    }
    if (!condition.nameNotContains.isNullOrEmpty() && methodName.contains(condition.nameNotContains!!)) {
        return false
    }
    if (!condition.startWith.isNullOrEmpty() && !methodName.startsWith(condition.startWith!!)) {
        return false
    }
    if (!condition.endWith.isNullOrEmpty() && !methodName.endsWith(condition.endWith!!)) {
        return false
    }

    val key = "${className}#${method.jarId ?: -1}"
    val classRef = classCache.computeIfAbsent(key) { engine.getClassRef(method.classReference, method.jarId) } ?: return false
    if (!condition.classAnno.isNullOrEmpty()) {
        val found = classRef.annotations?.any { it.annoName.contains(condition.classAnno!!) } == true
        if (!found) {
            return false
        }
    }

    if (!condition.methodAnno.isNullOrEmpty()) {
        val found = method.annotations?.any { it.annoName.contains(condition.methodAnno!!) } == true
        if (!found) {
            return false
        }
    }
    if (!condition.excludedMethodAnno.isNullOrEmpty()) {
        val excluded = method.annotations?.any { it.annoName.contains(condition.excludedMethodAnno!!) } == true
        if (excluded) {
            return false
        }
    }
    if (!condition.field.isNullOrEmpty()) {
        val found = classRef.members?.any { it.name.contains(condition.field!!) } == true
        if (!found) {
            return false
        }
    }

    val methodType = Type.getMethodType(method.desc)
    val args = methodType.argumentTypes
    if (condition.paramsNum != null && condition.paramsNum != args.size) {
        return false
    }
    if (!condition.returnType.isNullOrEmpty() && Type.getReturnType(method.desc).className != condition.returnType) {
        return false
    }
    if (condition.staticFlag != null && condition.staticFlag != method.isStatic) {
        return false
    }
    if (condition.publicFlag != null) {
        val isPublic = (method.access and Opcodes.ACC_PUBLIC) != 0
        if (condition.publicFlag != isPublic) {
            return false
        }
    }

    condition.paramTypes.forEach { (index, value) ->
        if (index in args.indices && args[index].className != value) {
            return false
        }
    }

    if (!condition.subClassOf.isNullOrEmpty()) {
        val parent = condition.subClassOf!!.replace(".", "/")
        val supers = superCache.computeIfAbsent(className) { engine.getSuperClasses(method.classReference) }
        if (supers.none { it.name == parent }) {
            return false
        }
    }
    if (!condition.superClassOf.isNullOrEmpty()) {
        val child = condition.superClassOf!!.replace(".", "/")
        val subs = subCache.computeIfAbsent(className) { engine.getSubClasses(method.classReference) }
        if (subs.none { it.name == child }) {
            return false
        }
    }
    return true
}

private fun MethodReference.toSearchResultDto(): SearchResultDto {
    val className = classReference.name
    return SearchResultDto(
        className,
        name,
        desc,
        jarName ?: "",
        jarId ?: 0,
        "$className#$name$desc",
    )
}

private data class BcelResult(
    val status: String,
    val code: String? = null,
)

private const val BCEL_PREFIX = "\$\$BCEL\$\$"

private fun checkBcelPayload(raw: String): String {
    val text = raw.trim()
    if (!text.uppercase(Locale.ROOT).startsWith(BCEL_PREFIX)) {
        return i18n("格式错误：必须以 $BCEL_PREFIX 开头", "format error: payload must start with $BCEL_PREFIX")
    }
    val payload = text.substring(8)
    val decoded = decodeBcelPayload(payload)
    return if (decoded == null) {
        i18n("decode 失败", "decode failed")
    } else {
        i18n("校验通过", "check pass")
    }
}

private fun decompileBcelPayload(raw: String): BcelResult {
    val text = raw.trim()
    if (!text.uppercase(Locale.ROOT).startsWith(BCEL_PREFIX)) {
        return BcelResult(i18n("格式错误：必须以 $BCEL_PREFIX 开头", "format error: payload must start with $BCEL_PREFIX"))
    }
    val payload = text.substring(8)
    val data = decodeBcelPayload(payload) ?: return BcelResult(i18n("decode 失败", "decode failed"))
    return try {
        val path = Paths.get(Const.tempDir).resolve("test-bcel.class")
        Files.createDirectories(path.parent)
        Files.write(path, data)
        val code = DecompileDispatcher.decompile(path, DecompileDispatcher.resolvePreferred())
            ?: return BcelResult(i18n("反编译失败", "decompile failed"))
        BcelResult(i18n("反编译成功，结果已写入主编辑器", "decompile success, code pushed to editor"), code)
    } catch (ex: Throwable) {
        BcelResult("${i18n("反编译失败", "decompile failed")}: ${ex.message}")
    }
}

private fun decodeBcelPayload(payload: String): ByteArray? {
    return try {
        Utility.decode(payload, true)
    } catch (_: Throwable) {
        try {
            Utility.decode(payload, false)
        } catch (_: Throwable) {
            null
        }
    }
}

private val BCEL_DEFAULT_PAYLOAD =
    "##BCEL###l#8b#I#A#A#A#A#A#A#A#95Q#c9J#DA#Q#7dm#96I#c6hb#W#f7#r#k#84D#d0F#c8#z#e2E#Q#P#83#K#91x#ee#qm#d2#a1g#st#sJ#fc#x#3d#ux#f0#D#fc#u#b1z#U#X#f0b#j#aax#efU#bd#ee#ea#7e#7d#7b#7e#B#d0#c0#96#8b4#W#b2#u#a2#94A#d9E#F#8b#O#96#i#y3#a4#PU#a0#a2#p#86D#ad#defH#k#87#3d#c9#90#f7T#m#cf#s#7eG#9aK#d1#d1#c4#U#bd#b0#xt#5b#Ye#f1#t#99#8c#Gj#cc#c0#3d_#f2#e0#40#98#G#l#K#c3E#m#f4#f4N#g#3e#d2#93#be#K#c6#bc#d3#95#9a#9fJ#ad#c3#ab#d0#e8#5e#93#n5#b0#88#c1m#85#T#d3#95#t#ca#ba#e5#bf#5b#f6#87#e2F#e4#e0#m#e3#60#r#87U#ac1#94#e3#99#ea#ad#d5#ab#d7#a1#a9Z#5b#H#eb9l#60#93a#ef_w#60#u#d8#p#b8#WA#9f#9fw#86#b2#h#fd#a2Z#d3q#q#7dz#95pBB#c5#8b#V#V#f2#L#a3#82#a8#V#Z#v#7cZ#a3#f4#H#cd#e0#8c#y#d2#B#cd#d5#bc#l#96#R#d1#fdf#bd#8dm#a4#e8#3fl#cc#80#d9#r#vg#Jq#aa#8cjj#f7#J#ec#3e#96#5d#ca#e9#98Lb#96r#ee#a3#81#ea#i#d5#y#e6#bf#86w#a8#dbF#f6#B3#c5#c4#p#92#d6#80#c5#Gn#y#a5#a9#d5#n#s#l#h#X#de#B#deF#f8Q#j#C#A#A"
        .replace('#', '$')

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
