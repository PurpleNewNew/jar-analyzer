package me.n1ar4.jar.analyzer.gui.jewel.service

import me.n1ar4.jar.analyzer.gui.jewel.state.McpStatus

data class TreeNodeItem(
    val label: String,
    val value: String,
    val directory: Boolean,
    val children: List<TreeNodeItem> = emptyList(),
)

data class EditorSnapshot(
    val className: String = "",
    val jarName: String = "",
    val methodName: String = "",
    val methodDesc: String = "",
    val content: String = "",
    val caretOffset: Int = 0,
    val statusText: String = "ready",
)

data class MethodNavItem(
    val className: String,
    val methodName: String,
    val methodDesc: String,
    val jarName: String = "",
    val jarId: Int = 0,
)

data class ClassNavItem(
    val className: String,
    val jarName: String = "",
    val jarId: Int = 0,
)

data class BuildSettings(
    val inputPath: String = "",
    val runtimePath: String = "",
    val resolveNestedJars: Boolean = false,
    val autoFindRuntimeJar: Boolean = false,
    val addRuntimeJar: Boolean = false,
    val deleteTempBeforeBuild: Boolean = true,
    val fixClassPath: Boolean = false,
    val fixMethodImpl: Boolean = true,
    val quickMode: Boolean = false,
)

data class BuildSnapshot(
    val settings: BuildSettings = BuildSettings(),
    val engineStatus: String = "CLOSED",
    val buildProgress: Int = 0,
    val totalJar: String = "0",
    val totalClass: String = "0",
    val totalMethod: String = "0",
    val totalEdge: String = "0",
    val databaseSize: String = "0",
)

enum class SearchMode {
    METHOD_CALL,
    METHOD_DEFINITION,
    STRING_CONTAINS,
    BINARY_CONTAINS,
}

enum class SearchMatchMode {
    LIKE,
    EQUALS,
}

data class SearchQuery(
    val mode: SearchMode = SearchMode.METHOD_CALL,
    val matchMode: SearchMatchMode = SearchMatchMode.LIKE,
    val className: String = "",
    val methodName: String = "",
    val keyword: String = "",
    val nullParamFilter: Boolean = false,
)

data class SearchResultItem(
    val className: String,
    val methodName: String,
    val methodDesc: String,
    val jarName: String = "",
    val jarId: Int = 0,
    val preview: String = "",
)

data class SearchSnapshot(
    val query: SearchQuery = SearchQuery(),
    val results: List<SearchResultItem> = emptyList(),
)

data class CallImplSnapshot(
    val currentJar: String = "",
    val currentClass: String = "",
    val currentMethod: String = "",
    val allMethods: List<MethodNavItem> = emptyList(),
    val callers: List<MethodNavItem> = emptyList(),
    val callees: List<MethodNavItem> = emptyList(),
    val impls: List<MethodNavItem> = emptyList(),
    val superImpls: List<MethodNavItem> = emptyList(),
)

enum class WebClassBucket {
    CONTROLLER,
    INTERCEPTOR,
    SERVLET,
    FILTER,
    LISTENER,
}

data class WebSnapshot(
    val pathKeyword: String = "",
    val controllers: List<ClassNavItem> = emptyList(),
    val mappings: List<MethodNavItem> = emptyList(),
    val interceptors: List<ClassNavItem> = emptyList(),
    val servlets: List<ClassNavItem> = emptyList(),
    val filters: List<ClassNavItem> = emptyList(),
    val listeners: List<ClassNavItem> = emptyList(),
)

data class NoteSnapshot(
    val history: List<MethodNavItem> = emptyList(),
    val favorites: List<MethodNavItem> = emptyList(),
)

enum class ScaOutputMode {
    CONSOLE,
    HTML,
}

data class ScaSettings(
    val scanLog4j: Boolean = true,
    val scanShiro: Boolean = true,
    val scanFastjson: Boolean = true,
    val inputPath: String = "",
    val outputMode: ScaOutputMode = ScaOutputMode.HTML,
    val outputFile: String = "",
)

data class ScaSnapshot(
    val settings: ScaSettings = ScaSettings(),
    val logTail: String = "",
)

data class LeakRules(
    val url: Boolean = true,
    val jdbc: Boolean = true,
    val filePath: Boolean = true,
    val jwt: Boolean = true,
    val mac: Boolean = true,
    val ip: Boolean = true,
    val phone: Boolean = true,
    val idCard: Boolean = true,
    val email: Boolean = true,
    val apiKey: Boolean = true,
    val bankCard: Boolean = true,
    val cloudAkSk: Boolean = true,
    val cryptoKey: Boolean = true,
    val aiKey: Boolean = true,
    val password: Boolean = true,
    val detectBase64: Boolean = false,
)

data class LeakItem(
    val className: String,
    val typeName: String,
    val value: String,
    val jarName: String = "",
    val jarId: Int = 0,
)

data class LeakSnapshot(
    val rules: LeakRules = LeakRules(),
    val results: List<LeakItem> = emptyList(),
    val logTail: String = "",
)

data class GadgetSettings(
    val inputDir: String = "",
    val native: Boolean = true,
    val hessian: Boolean = true,
    val jdbc: Boolean = true,
    val fastjson: Boolean = true,
)

data class GadgetRow(
    val id: String,
    val definition: String,
    val risk: String,
)

data class GadgetSnapshot(
    val settings: GadgetSettings = GadgetSettings(),
    val rows: List<GadgetRow> = emptyList(),
)

data class ChainsSettings(
    val sinkSelected: Boolean = true,
    val sourceSelected: Boolean = false,
    val sinkClass: String = "java/lang/Runtime",
    val sinkMethod: String = "exec",
    val sinkDesc: String = "(Ljava/lang/String;)Ljava/lang/Process;",
    val sourceClass: String = "",
    val sourceMethod: String = "",
    val sourceDesc: String = "",
    val sourceNull: Boolean = false,
    val sourceEnabled: Boolean = true,
    val maxDepth: Int = 10,
    val onlyFromWeb: Boolean = false,
    val taintEnabled: Boolean = false,
    val blacklist: String = "",
    val minEdgeConfidence: String = "low",
    val showEdgeMeta: Boolean = true,
    val summaryEnabled: Boolean = false,
    val taintSeedParam: Int? = null,
    val taintSeedStrict: Boolean = false,
    val maxResultLimit: Int = 30,
)

data class ChainsSnapshot(
    val settings: ChainsSettings = ChainsSettings(),
    val dfsCount: Int = 0,
    val taintCount: Int = 0,
)

enum class McpLineKey(val id: String) {
    AUDIT_FAST("audit-fast"),
    GRAPH_LITE("graph-lite"),
    DFS_TAINT("dfs-taint"),
    SCA_LEAK("sca-leak"),
    VUL_RULES("vul-rules"),
    REPORT("report"),
}

data class McpLineConfig(
    val key: McpLineKey,
    val enabled: Boolean,
    val port: Int,
    val running: Boolean = false,
)

data class McpConfig(
    val bind: String = "0.0.0.0",
    val authEnabled: Boolean = false,
    val token: String = "JAR-ANALYZER-MCP-TOKEN",
    val lines: List<McpLineConfig> = emptyList(),
    val reportWebEnabled: Boolean = false,
    val reportWebHost: String = "127.0.0.1",
    val reportWebPort: Int = 20080,
    val reportWebRunning: Boolean = false,
)

data class ApiInfo(
    val bind: String = "0.0.0.0",
    val authEnabled: Boolean = false,
    val port: Int = 10032,
    val maskedToken: String = "",
)

interface BuildService {
    fun engineStatus(): String
    fun snapshot(): BuildSnapshot
    fun applySettings(settings: BuildSettings)
    fun startBuild()
    fun clearCache()
}

interface ProjectTreeService {
    fun snapshot(): List<TreeNodeItem>
    fun search(keyword: String): List<TreeNodeItem>
    fun refresh()
    fun openNode(value: String)
}

interface EditorService {
    fun snapshot(): EditorSnapshot
    fun openClass(className: String, jarId: Int? = null)
    fun openMethod(className: String, methodName: String, methodDesc: String, jarId: Int? = null)
    fun search(keyword: String, forward: Boolean)
    fun applyText(content: String, caretOffset: Int)
    fun goPrev(): Boolean
    fun goNext(): Boolean
    fun addCurrentToFavorites(): Boolean
}

interface SearchService {
    fun supportsJumpNavigation(): Boolean
    fun snapshot(): SearchSnapshot
    fun applyQuery(query: SearchQuery)
    fun runSearch()
    fun openResult(index: Int)
}

interface CallImplService {
    fun snapshot(): CallImplSnapshot
    fun refreshCurrentContext()
    fun openAllMethod(index: Int)
    fun openCaller(index: Int)
    fun openCallee(index: Int)
    fun openImpl(index: Int)
    fun openSuperImpl(index: Int)
}

interface WebService {
    fun snapshot(): WebSnapshot
    fun refreshAll()
    fun pathSearch(keyword: String)
    fun openClass(bucket: WebClassBucket, index: Int)
    fun openMapping(index: Int)
}

interface NoteService {
    fun snapshot(): NoteSnapshot
    fun load()
    fun openHistory(index: Int)
    fun openFavorite(index: Int)
    fun clearHistory()
    fun clearFavorites()
}

interface ScaService {
    fun snapshot(): ScaSnapshot
    fun apply(settings: ScaSettings)
    fun chooseInput()
    fun start()
    fun openResult()
}

interface LeakService {
    fun snapshot(): LeakSnapshot
    fun apply(rules: LeakRules)
    fun start()
    fun clear()
    fun export()
    fun openResult(index: Int)
}

interface GadgetService {
    fun snapshot(): GadgetSnapshot
    fun apply(settings: GadgetSettings)
    fun chooseDir()
    fun start()
}

interface ChainsService {
    fun supportsTaintValidation(): Boolean
    fun snapshot(): ChainsSnapshot
    fun apply(settings: ChainsSettings)
    fun startDfs()
    fun startTaint()
    fun clearResults()
    fun openAdvancedBridge()
}

interface AdvanceService {
    fun showOpcode()
    fun showAsm()
    fun showCfg()
    fun showFrame(full: Boolean)
    fun showHtmlGraph()
}

interface McpService {
    fun apiInfo(): ApiInfo
    fun currentConfig(): McpConfig
    fun status(): McpStatus
    fun applyAndRestart(config: McpConfig): List<String>
    fun startConfigured(): List<String>
    fun stopAll()
    fun openApiDoc()
    fun openMcpDoc()
    fun openN8nDoc()
    fun openReportWeb(host: String, port: Int)
}
