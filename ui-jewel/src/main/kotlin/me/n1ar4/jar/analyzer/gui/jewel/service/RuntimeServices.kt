package me.n1ar4.jar.analyzer.gui.jewel.service

import me.n1ar4.jar.analyzer.gui.jewel.state.McpStatus
import me.n1ar4.jar.analyzer.gui.runtime.api.RuntimeFacades
import me.n1ar4.jar.analyzer.gui.runtime.model.ApiInfoDto
import me.n1ar4.jar.analyzer.gui.runtime.model.BuildSettingsDto
import me.n1ar4.jar.analyzer.gui.runtime.model.BuildSnapshotDto
import me.n1ar4.jar.analyzer.gui.runtime.model.CallGraphSnapshotDto
import me.n1ar4.jar.analyzer.gui.runtime.model.ChainsSettingsDto
import me.n1ar4.jar.analyzer.gui.runtime.model.ChainsSnapshotDto
import me.n1ar4.jar.analyzer.gui.runtime.model.ClassNavDto
import me.n1ar4.jar.analyzer.gui.runtime.model.EditorDocumentDto
import me.n1ar4.jar.analyzer.gui.runtime.model.GadgetRowDto
import me.n1ar4.jar.analyzer.gui.runtime.model.GadgetSettingsDto
import me.n1ar4.jar.analyzer.gui.runtime.model.GadgetSnapshotDto
import me.n1ar4.jar.analyzer.gui.runtime.model.LeakItemDto
import me.n1ar4.jar.analyzer.gui.runtime.model.LeakRulesDto
import me.n1ar4.jar.analyzer.gui.runtime.model.LeakSnapshotDto
import me.n1ar4.jar.analyzer.gui.runtime.model.McpConfigDto
import me.n1ar4.jar.analyzer.gui.runtime.model.McpLineConfigDto
import me.n1ar4.jar.analyzer.gui.runtime.model.MethodNavDto
import me.n1ar4.jar.analyzer.gui.runtime.model.NoteSnapshotDto
import me.n1ar4.jar.analyzer.gui.runtime.model.ScaSettingsDto
import me.n1ar4.jar.analyzer.gui.runtime.model.ScaSnapshotDto
import me.n1ar4.jar.analyzer.gui.runtime.model.SearchMatchMode as RuntimeSearchMatchMode
import me.n1ar4.jar.analyzer.gui.runtime.model.SearchMode as RuntimeSearchMode
import me.n1ar4.jar.analyzer.gui.runtime.model.SearchQueryDto
import me.n1ar4.jar.analyzer.gui.runtime.model.SearchResultDto
import me.n1ar4.jar.analyzer.gui.runtime.model.SearchSnapshotDto
import me.n1ar4.jar.analyzer.gui.runtime.model.TreeNodeDto
import me.n1ar4.jar.analyzer.gui.runtime.model.WebClassBucket as RuntimeWebClassBucket
import me.n1ar4.jar.analyzer.gui.runtime.model.WebSnapshotDto

class RuntimeBuildService : BuildService {
    override fun engineStatus(): String {
        return RuntimeFacades.build().snapshot().engineStatus()
    }

    override fun snapshot(): BuildSnapshot {
        return RuntimeFacades.build().snapshot().toUi()
    }

    override fun applySettings(settings: BuildSettings) {
        RuntimeFacades.build().apply(settings.toDto())
    }

    override fun startBuild() {
        RuntimeFacades.build().startBuild()
    }

    override fun clearCache() {
        RuntimeFacades.build().clearCache()
    }
}

class RuntimeProjectTreeService : ProjectTreeService {
    override fun snapshot(): List<TreeNodeItem> {
        return RuntimeFacades.projectTree().snapshot().map { it.toUi() }
    }

    override fun search(keyword: String): List<TreeNodeItem> {
        return RuntimeFacades.projectTree().search(keyword).map { it.toUi() }
    }

    override fun refresh() {
        RuntimeFacades.projectTree().refresh()
    }

    override fun openNode(value: String) {
        RuntimeFacades.projectTree().openNode(value)
    }
}

class RuntimeEditorService : EditorService {
    override fun snapshot(): EditorSnapshot {
        return RuntimeFacades.editor().current().toUi()
    }

    override fun openClass(className: String, jarId: Int?) {
        RuntimeFacades.editor().openClass(className, jarId)
    }

    override fun openMethod(className: String, methodName: String, methodDesc: String, jarId: Int?) {
        RuntimeFacades.editor().openMethod(className, methodName, methodDesc, jarId)
    }

    override fun search(keyword: String, forward: Boolean) {
        RuntimeFacades.editor().searchInCurrent(keyword, forward)
    }

    override fun applyText(content: String, caretOffset: Int) {
        RuntimeFacades.editor().applyEditorText(content, caretOffset)
    }

    override fun goPrev(): Boolean {
        return RuntimeFacades.editor().goPrev()
    }

    override fun goNext(): Boolean {
        return RuntimeFacades.editor().goNext()
    }

    override fun addCurrentToFavorites(): Boolean {
        return RuntimeFacades.editor().addCurrentToFavorites()
    }
}

class RuntimeSearchService : SearchService {
    override fun supportsJumpNavigation(): Boolean = true

    override fun snapshot(): SearchSnapshot {
        return RuntimeFacades.search().snapshot().toUi()
    }

    override fun applyQuery(query: SearchQuery) {
        RuntimeFacades.search().applyQuery(query.toDto())
    }

    override fun runSearch() {
        RuntimeFacades.search().runSearch()
    }

    override fun openResult(index: Int) {
        RuntimeFacades.search().openResult(index)
    }
}

class RuntimeCallImplService : CallImplService {
    override fun snapshot(): CallImplSnapshot {
        return RuntimeFacades.callGraph().snapshot().toUi()
    }

    override fun refreshCurrentContext() {
        RuntimeFacades.callGraph().refreshCurrentContext()
    }

    override fun openAllMethod(index: Int) {
        RuntimeFacades.callGraph().openAllMethod(index)
    }

    override fun openCaller(index: Int) {
        RuntimeFacades.callGraph().openCaller(index)
    }

    override fun openCallee(index: Int) {
        RuntimeFacades.callGraph().openCallee(index)
    }

    override fun openImpl(index: Int) {
        RuntimeFacades.callGraph().openImpl(index)
    }

    override fun openSuperImpl(index: Int) {
        RuntimeFacades.callGraph().openSuperImpl(index)
    }
}

class RuntimeWebService : WebService {
    override fun snapshot(): WebSnapshot {
        return RuntimeFacades.web().snapshot().toUi()
    }

    override fun refreshAll() {
        RuntimeFacades.web().refreshAll()
    }

    override fun pathSearch(keyword: String) {
        RuntimeFacades.web().pathSearch(keyword)
    }

    override fun openClass(bucket: WebClassBucket, index: Int) {
        RuntimeFacades.web().openClass(bucket.toRuntime(), index)
    }

    override fun openMapping(index: Int) {
        RuntimeFacades.web().openMapping(index)
    }
}

class RuntimeNoteService : NoteService {
    override fun snapshot(): NoteSnapshot {
        return RuntimeFacades.note().snapshot().toUi()
    }

    override fun load() {
        RuntimeFacades.note().load()
    }

    override fun openHistory(index: Int) {
        RuntimeFacades.note().openHistory(index)
    }

    override fun openFavorite(index: Int) {
        RuntimeFacades.note().openFavorite(index)
    }

    override fun clearHistory() {
        RuntimeFacades.note().clearHistory()
    }

    override fun clearFavorites() {
        RuntimeFacades.note().clearFavorites()
    }
}

class RuntimeScaService : ScaService {
    override fun snapshot(): ScaSnapshot {
        return RuntimeFacades.sca().snapshot().toUi()
    }

    override fun apply(settings: ScaSettings) {
        RuntimeFacades.sca().apply(settings.toDto())
    }

    override fun chooseInput() {
        RuntimeFacades.sca().chooseInput()
    }

    override fun start() {
        RuntimeFacades.sca().start()
    }

    override fun openResult() {
        RuntimeFacades.sca().openResult()
    }
}

class RuntimeLeakService : LeakService {
    override fun snapshot(): LeakSnapshot {
        return RuntimeFacades.leak().snapshot().toUi()
    }

    override fun apply(rules: LeakRules) {
        RuntimeFacades.leak().apply(rules.toDto())
    }

    override fun start() {
        RuntimeFacades.leak().start()
    }

    override fun clear() {
        RuntimeFacades.leak().clear()
    }

    override fun export() {
        RuntimeFacades.leak().export()
    }

    override fun openResult(index: Int) {
        RuntimeFacades.leak().openResult(index)
    }
}

class RuntimeGadgetService : GadgetService {
    override fun snapshot(): GadgetSnapshot {
        return RuntimeFacades.gadget().snapshot().toUi()
    }

    override fun apply(settings: GadgetSettings) {
        RuntimeFacades.gadget().apply(settings.toDto())
    }

    override fun chooseDir() {
        RuntimeFacades.gadget().chooseDir()
    }

    override fun start() {
        RuntimeFacades.gadget().start()
    }
}

class RuntimeChainsService : ChainsService {
    override fun supportsTaintValidation(): Boolean = true

    override fun snapshot(): ChainsSnapshot {
        return RuntimeFacades.chains().snapshot().toUi()
    }

    override fun apply(settings: ChainsSettings) {
        RuntimeFacades.chains().apply(settings.toDto())
    }

    override fun startDfs() {
        RuntimeFacades.chains().startDfs()
    }

    override fun startTaint() {
        RuntimeFacades.chains().startTaint()
    }

    override fun clearResults() {
        RuntimeFacades.chains().clearResults()
    }

    override fun openAdvancedBridge() {
        RuntimeFacades.chains().openAdvanceSettings()
    }
}

class RuntimeAdvanceService : AdvanceService {
    override fun showOpcode() {
        RuntimeFacades.tooling().openOpcodeTool()
    }

    override fun showAsm() {
        RuntimeFacades.tooling().openAsmTool()
    }

    override fun showCfg() {
        RuntimeFacades.tooling().openCfgTool()
    }

    override fun showFrame(full: Boolean) {
        RuntimeFacades.tooling().openFrameTool(full)
    }

    override fun showHtmlGraph() {
        RuntimeFacades.tooling().openHtmlGraph()
    }
}

class RuntimeMcpService : McpService {
    override fun apiInfo(): ApiInfo {
        return RuntimeFacades.apiMcp().apiInfo().toUi()
    }

    override fun currentConfig(): McpConfig {
        return RuntimeFacades.apiMcp().currentConfig().toUi()
    }

    override fun status(): McpStatus {
        val cfg = RuntimeFacades.apiMcp().currentConfig().toUi()
        return McpStatus(
            auditFast = cfg.lines.firstOrNull { it.key == McpLineKey.AUDIT_FAST }?.running ?: false,
            graphLite = cfg.lines.firstOrNull { it.key == McpLineKey.GRAPH_LITE }?.running ?: false,
            dfsTaint = cfg.lines.firstOrNull { it.key == McpLineKey.DFS_TAINT }?.running ?: false,
            scaLeak = cfg.lines.firstOrNull { it.key == McpLineKey.SCA_LEAK }?.running ?: false,
            vulRules = cfg.lines.firstOrNull { it.key == McpLineKey.VUL_RULES }?.running ?: false,
            report = cfg.lines.firstOrNull { it.key == McpLineKey.REPORT }?.running ?: false,
            reportWeb = cfg.reportWebRunning,
        )
    }

    override fun applyAndRestart(config: McpConfig): List<String> {
        return RuntimeFacades.apiMcp().applyAndRestart(config.toDto())
    }

    override fun startConfigured(): List<String> {
        return RuntimeFacades.apiMcp().startConfigured()
    }

    override fun stopAll() {
        RuntimeFacades.apiMcp().stopAll()
    }

    override fun openApiDoc() {
        RuntimeFacades.apiMcp().openApiDoc()
    }

    override fun openMcpDoc() {
        RuntimeFacades.apiMcp().openMcpDoc()
    }

    override fun openN8nDoc() {
        RuntimeFacades.apiMcp().openN8nDoc()
    }

    override fun openReportWeb(host: String, port: Int) {
        RuntimeFacades.apiMcp().openReportWeb(host, port)
    }
}

private fun BuildSettings.toDto(): BuildSettingsDto {
    return BuildSettingsDto(
        inputPath,
        runtimePath,
        resolveNestedJars,
        autoFindRuntimeJar,
        addRuntimeJar,
        deleteTempBeforeBuild,
        fixClassPath,
        fixMethodImpl,
        quickMode,
    )
}

private fun BuildSnapshotDto.toUi(): BuildSnapshot {
    return BuildSnapshot(
        settings = settings().toUi(),
        engineStatus = engineStatus(),
        buildProgress = buildProgress(),
        totalJar = totalJar(),
        totalClass = totalClass(),
        totalMethod = totalMethod(),
        totalEdge = totalEdge(),
        databaseSize = databaseSize(),
    )
}

private fun BuildSettingsDto.toUi(): BuildSettings {
    return BuildSettings(
        inputPath = inputPath(),
        runtimePath = runtimePath(),
        resolveNestedJars = resolveNestedJars(),
        autoFindRuntimeJar = autoFindRuntimeJar(),
        addRuntimeJar = addRuntimeJar(),
        deleteTempBeforeBuild = deleteTempBeforeBuild(),
        fixClassPath = fixClassPath(),
        fixMethodImpl = fixMethodImpl(),
        quickMode = quickMode(),
    )
}

private fun SearchQuery.toDto(): SearchQueryDto {
    return SearchQueryDto(
        mode.toRuntime(),
        matchMode.toRuntime(),
        className,
        methodName,
        keyword,
        nullParamFilter,
    )
}

private fun SearchSnapshotDto.toUi(): SearchSnapshot {
    return SearchSnapshot(
        query = query().toUi(),
        results = results().map { it.toUi() },
    )
}

private fun SearchQueryDto.toUi(): SearchQuery {
    return SearchQuery(
        mode = mode().toUi(),
        matchMode = matchMode().toUi(),
        className = className(),
        methodName = methodName(),
        keyword = keyword(),
        nullParamFilter = nullParamFilter(),
    )
}

private fun SearchResultDto.toUi(): SearchResultItem {
    return SearchResultItem(
        className = className(),
        methodName = methodName(),
        methodDesc = methodDesc(),
        jarName = jarName(),
        jarId = jarId(),
        preview = preview(),
    )
}

private fun SearchMode.toRuntime(): RuntimeSearchMode {
    return when (this) {
        SearchMode.METHOD_CALL -> RuntimeSearchMode.METHOD_CALL
        SearchMode.METHOD_DEFINITION -> RuntimeSearchMode.METHOD_DEFINITION
        SearchMode.STRING_CONTAINS -> RuntimeSearchMode.STRING_CONTAINS
        SearchMode.BINARY_CONTAINS -> RuntimeSearchMode.BINARY_CONTAINS
    }
}

private fun RuntimeSearchMode.toUi(): SearchMode {
    return when (this) {
        RuntimeSearchMode.METHOD_CALL -> SearchMode.METHOD_CALL
        RuntimeSearchMode.METHOD_DEFINITION -> SearchMode.METHOD_DEFINITION
        RuntimeSearchMode.STRING_CONTAINS -> SearchMode.STRING_CONTAINS
        RuntimeSearchMode.BINARY_CONTAINS -> SearchMode.BINARY_CONTAINS
    }
}

private fun SearchMatchMode.toRuntime(): RuntimeSearchMatchMode {
    return when (this) {
        SearchMatchMode.LIKE -> RuntimeSearchMatchMode.LIKE
        SearchMatchMode.EQUALS -> RuntimeSearchMatchMode.EQUALS
    }
}

private fun RuntimeSearchMatchMode.toUi(): SearchMatchMode {
    return when (this) {
        RuntimeSearchMatchMode.LIKE -> SearchMatchMode.LIKE
        RuntimeSearchMatchMode.EQUALS -> SearchMatchMode.EQUALS
    }
}

private fun CallGraphSnapshotDto.toUi(): CallImplSnapshot {
    return CallImplSnapshot(
        currentJar = currentJar(),
        currentClass = currentClass(),
        currentMethod = currentMethod(),
        allMethods = allMethods().map { it.toUi() },
        callers = callers().map { it.toUi() },
        callees = callees().map { it.toUi() },
        impls = impls().map { it.toUi() },
        superImpls = superImpls().map { it.toUi() },
    )
}

private fun MethodNavDto.toUi(): MethodNavItem {
    return MethodNavItem(
        className = className(),
        methodName = methodName(),
        methodDesc = methodDesc(),
        jarName = jarName(),
        jarId = jarId(),
    )
}

private fun TreeNodeDto.toUi(): TreeNodeItem {
    return TreeNodeItem(
        label = label(),
        value = value(),
        directory = directory(),
        children = children().map { it.toUi() },
    )
}

private fun EditorDocumentDto.toUi(): EditorSnapshot {
    return EditorSnapshot(
        className = className(),
        jarName = jarName(),
        methodName = methodName(),
        methodDesc = methodDesc(),
        content = content(),
        caretOffset = caretOffset(),
        statusText = statusText(),
    )
}

private fun WebSnapshotDto.toUi(): WebSnapshot {
    return WebSnapshot(
        pathKeyword = pathKeyword(),
        controllers = controllers().map { it.toUi() },
        mappings = mappings().map { it.toUi() },
        interceptors = interceptors().map { it.toUi() },
        servlets = servlets().map { it.toUi() },
        filters = filters().map { it.toUi() },
        listeners = listeners().map { it.toUi() },
    )
}

private fun ClassNavDto.toUi(): ClassNavItem {
    return ClassNavItem(
        className = className(),
        jarName = jarName(),
        jarId = jarId(),
    )
}

private fun WebClassBucket.toRuntime(): RuntimeWebClassBucket {
    return when (this) {
        WebClassBucket.CONTROLLER -> RuntimeWebClassBucket.CONTROLLER
        WebClassBucket.INTERCEPTOR -> RuntimeWebClassBucket.INTERCEPTOR
        WebClassBucket.SERVLET -> RuntimeWebClassBucket.SERVLET
        WebClassBucket.FILTER -> RuntimeWebClassBucket.FILTER
        WebClassBucket.LISTENER -> RuntimeWebClassBucket.LISTENER
    }
}

private fun NoteSnapshotDto.toUi(): NoteSnapshot {
    return NoteSnapshot(
        history = history().map { it.toUi() },
        favorites = favorites().map { it.toUi() },
    )
}

private fun ScaSettings.toDto(): ScaSettingsDto {
    return ScaSettingsDto(
        scanLog4j,
        scanShiro,
        scanFastjson,
        inputPath,
        outputMode.toRuntime(),
        outputFile,
    )
}

private fun me.n1ar4.jar.analyzer.gui.runtime.model.ScaOutputMode.toUi(): ScaOutputMode {
    return when (this) {
        me.n1ar4.jar.analyzer.gui.runtime.model.ScaOutputMode.CONSOLE -> ScaOutputMode.CONSOLE
        me.n1ar4.jar.analyzer.gui.runtime.model.ScaOutputMode.HTML -> ScaOutputMode.HTML
    }
}

private fun ScaOutputMode.toRuntime(): me.n1ar4.jar.analyzer.gui.runtime.model.ScaOutputMode {
    return when (this) {
        ScaOutputMode.CONSOLE -> me.n1ar4.jar.analyzer.gui.runtime.model.ScaOutputMode.CONSOLE
        ScaOutputMode.HTML -> me.n1ar4.jar.analyzer.gui.runtime.model.ScaOutputMode.HTML
    }
}

private fun ScaSnapshotDto.toUi(): ScaSnapshot {
    return ScaSnapshot(
        settings = ScaSettings(
            scanLog4j = settings().scanLog4j(),
            scanShiro = settings().scanShiro(),
            scanFastjson = settings().scanFastjson(),
            inputPath = settings().inputPath(),
            outputMode = settings().outputMode().toUi(),
            outputFile = settings().outputFile(),
        ),
        logTail = logTail(),
    )
}

private fun LeakRules.toDto(): LeakRulesDto {
    return LeakRulesDto(
        url,
        jdbc,
        filePath,
        jwt,
        mac,
        ip,
        phone,
        idCard,
        email,
        apiKey,
        bankCard,
        cloudAkSk,
        cryptoKey,
        aiKey,
        password,
        detectBase64,
    )
}

private fun LeakSnapshotDto.toUi(): LeakSnapshot {
    return LeakSnapshot(
        rules = LeakRules(
            url = rules().url(),
            jdbc = rules().jdbc(),
            filePath = rules().filePath(),
            jwt = rules().jwt(),
            mac = rules().mac(),
            ip = rules().ip(),
            phone = rules().phone(),
            idCard = rules().idCard(),
            email = rules().email(),
            apiKey = rules().apiKey(),
            bankCard = rules().bankCard(),
            cloudAkSk = rules().cloudAkSk(),
            cryptoKey = rules().cryptoKey(),
            aiKey = rules().aiKey(),
            password = rules().password(),
            detectBase64 = rules().detectBase64(),
        ),
        results = results().map { it.toUi() },
        logTail = logTail(),
    )
}

private fun LeakItemDto.toUi(): LeakItem {
    return LeakItem(
        className = className(),
        typeName = typeName(),
        value = value(),
        jarName = jarName(),
        jarId = jarId(),
    )
}

private fun GadgetSettings.toDto(): GadgetSettingsDto {
    return GadgetSettingsDto(
        inputDir,
        native,
        hessian,
        jdbc,
        fastjson,
    )
}

private fun GadgetSnapshotDto.toUi(): GadgetSnapshot {
    return GadgetSnapshot(
        settings = GadgetSettings(
            inputDir = settings().inputDir(),
            native = settings().nativeMode(),
            hessian = settings().hessian(),
            jdbc = settings().jdbc(),
            fastjson = settings().fastjson(),
        ),
        rows = rows().map { it.toUi() },
    )
}

private fun GadgetRowDto.toUi(): GadgetRow {
    return GadgetRow(
        id = id(),
        definition = definition(),
        risk = risk(),
    )
}

private fun ChainsSettings.toDto(): ChainsSettingsDto {
    return ChainsSettingsDto(
        sinkSelected,
        sourceSelected,
        sinkClass,
        sinkMethod,
        sinkDesc,
        sourceClass,
        sourceMethod,
        sourceDesc,
        sourceNull,
        sourceEnabled,
        maxDepth,
        onlyFromWeb,
        taintEnabled,
        blacklist,
        minEdgeConfidence,
        showEdgeMeta,
        summaryEnabled,
        taintSeedParam,
        taintSeedStrict,
        maxResultLimit,
    )
}

private fun ChainsSnapshotDto.toUi(): ChainsSnapshot {
    return ChainsSnapshot(
        settings = ChainsSettings(
            sinkSelected = settings().sinkSelected(),
            sourceSelected = settings().sourceSelected(),
            sinkClass = settings().sinkClass(),
            sinkMethod = settings().sinkMethod(),
            sinkDesc = settings().sinkDesc(),
            sourceClass = settings().sourceClass(),
            sourceMethod = settings().sourceMethod(),
            sourceDesc = settings().sourceDesc(),
            sourceNull = settings().sourceNull(),
            sourceEnabled = settings().sourceEnabled(),
            maxDepth = settings().maxDepth(),
            onlyFromWeb = settings().onlyFromWeb(),
            taintEnabled = settings().taintEnabled(),
            blacklist = settings().blacklist(),
            minEdgeConfidence = settings().minEdgeConfidence(),
            showEdgeMeta = settings().showEdgeMeta(),
            summaryEnabled = settings().summaryEnabled(),
            taintSeedParam = settings().taintSeedParam(),
            taintSeedStrict = settings().taintSeedStrict(),
            maxResultLimit = settings().maxResultLimit(),
        ),
        dfsCount = dfsCount(),
        taintCount = taintCount(),
    )
}

private fun ApiInfoDto.toUi(): ApiInfo {
    return ApiInfo(
        bind = bind(),
        authEnabled = authEnabled(),
        port = port(),
        maskedToken = maskedToken(),
    )
}

private fun McpConfigDto.toUi(): McpConfig {
    return McpConfig(
        bind = bind(),
        authEnabled = authEnabled(),
        token = token(),
        lines = lines().map { it.toUi() },
        reportWebEnabled = reportWebEnabled(),
        reportWebHost = reportWebHost(),
        reportWebPort = reportWebPort(),
        reportWebRunning = reportWebRunning(),
    )
}

private fun McpLineConfigDto.toUi(): McpLineConfig {
    return McpLineConfig(
        key = key().toUi(),
        enabled = enabled(),
        port = port(),
        running = running(),
    )
}

private fun me.n1ar4.jar.analyzer.gui.runtime.model.McpLineKey.toUi(): McpLineKey {
    return when (this) {
        me.n1ar4.jar.analyzer.gui.runtime.model.McpLineKey.AUDIT_FAST -> McpLineKey.AUDIT_FAST
        me.n1ar4.jar.analyzer.gui.runtime.model.McpLineKey.GRAPH_LITE -> McpLineKey.GRAPH_LITE
        me.n1ar4.jar.analyzer.gui.runtime.model.McpLineKey.DFS_TAINT -> McpLineKey.DFS_TAINT
        me.n1ar4.jar.analyzer.gui.runtime.model.McpLineKey.SCA_LEAK -> McpLineKey.SCA_LEAK
        me.n1ar4.jar.analyzer.gui.runtime.model.McpLineKey.VUL_RULES -> McpLineKey.VUL_RULES
        me.n1ar4.jar.analyzer.gui.runtime.model.McpLineKey.REPORT -> McpLineKey.REPORT
    }
}

private fun McpLineKey.toRuntime(): me.n1ar4.jar.analyzer.gui.runtime.model.McpLineKey {
    return when (this) {
        McpLineKey.AUDIT_FAST -> me.n1ar4.jar.analyzer.gui.runtime.model.McpLineKey.AUDIT_FAST
        McpLineKey.GRAPH_LITE -> me.n1ar4.jar.analyzer.gui.runtime.model.McpLineKey.GRAPH_LITE
        McpLineKey.DFS_TAINT -> me.n1ar4.jar.analyzer.gui.runtime.model.McpLineKey.DFS_TAINT
        McpLineKey.SCA_LEAK -> me.n1ar4.jar.analyzer.gui.runtime.model.McpLineKey.SCA_LEAK
        McpLineKey.VUL_RULES -> me.n1ar4.jar.analyzer.gui.runtime.model.McpLineKey.VUL_RULES
        McpLineKey.REPORT -> me.n1ar4.jar.analyzer.gui.runtime.model.McpLineKey.REPORT
    }
}

private fun McpConfig.toDto(): McpConfigDto {
    return McpConfigDto(
        bind,
        authEnabled,
        token,
        lines.map { it.toDto() },
        reportWebEnabled,
        reportWebHost,
        reportWebPort,
        reportWebRunning,
    )
}

private fun McpLineConfig.toDto(): me.n1ar4.jar.analyzer.gui.runtime.model.McpLineConfigDto {
    return me.n1ar4.jar.analyzer.gui.runtime.model.McpLineConfigDto(
        key.toRuntime(),
        enabled,
        port,
        running,
    )
}
