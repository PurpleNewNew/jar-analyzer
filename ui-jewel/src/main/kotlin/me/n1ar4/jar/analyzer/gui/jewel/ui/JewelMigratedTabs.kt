package me.n1ar4.jar.analyzer.gui.jewel.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import me.n1ar4.jar.analyzer.gui.runtime.api.RuntimeFacades
import me.n1ar4.jar.analyzer.utils.CommonBlacklistUtil
import me.n1ar4.jar.analyzer.utils.CommonFilterUtil
import me.n1ar4.jar.analyzer.utils.CommonWhitelistUtil
import me.n1ar4.jar.analyzer.utils.ListParser
import me.n1ar4.jar.analyzer.gui.jewel.service.ApiInfo
import me.n1ar4.jar.analyzer.gui.jewel.service.BuildService
import me.n1ar4.jar.analyzer.gui.jewel.service.BuildSettings
import me.n1ar4.jar.analyzer.gui.jewel.service.McpConfig
import me.n1ar4.jar.analyzer.gui.jewel.service.McpLineConfig
import me.n1ar4.jar.analyzer.gui.jewel.service.McpLineKey
import me.n1ar4.jar.analyzer.gui.jewel.service.McpService
import me.n1ar4.jar.analyzer.gui.jewel.service.SearchMatchMode
import me.n1ar4.jar.analyzer.gui.jewel.service.SearchQuery
import me.n1ar4.jar.analyzer.gui.jewel.service.SearchResultItem
import me.n1ar4.jar.analyzer.gui.jewel.service.SearchMode
import me.n1ar4.jar.analyzer.gui.jewel.service.SearchService
import org.jetbrains.jewel.ui.Orientation
import org.jetbrains.jewel.ui.component.CheckboxRow
import org.jetbrains.jewel.ui.component.Divider
import org.jetbrains.jewel.ui.component.HorizontalProgressBar
import org.jetbrains.jewel.ui.component.RadioButtonRow
import org.jetbrains.jewel.ui.component.Text
import java.io.File
import java.awt.BorderLayout
import java.awt.GridLayout
import java.util.ArrayList
import javax.swing.JFileChooser
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextArea

@Composable
fun MigratedStartTab(buildService: BuildService) {
    val initial = remember { buildService.snapshot() }
    var settings by remember { mutableStateOf(initial.settings) }
    var snapshot by remember { mutableStateOf(initial) }
    var status by remember { mutableStateOf("ready") }
    var decompiler by remember {
        mutableStateOf(
            if ((System.getProperty("jar-analyzer.decompiler") ?: "").contains("cfr", ignoreCase = true)) {
                "cfr"
            } else {
                "fernflower"
            },
        )
    }

    LaunchedEffect(Unit) {
        while (true) {
            snapshot = buildService.snapshot()
            delay(900)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Build")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            BoundTextField(
                settings.inputPath,
                { settings = settings.copy(inputPath = it) },
                Modifier.weight(1f),
            )
            IdeaOutlinedButton(onClick = {
                choosePath(settings.inputPath, JFileChooser.FILES_AND_DIRECTORIES)?.let {
                    settings = settings.copy(inputPath = it)
                    status = "input selected"
                }
            }) { Text("Choose") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            BoundTextField(
                settings.runtimePath,
                { settings = settings.copy(runtimePath = it) },
                Modifier.weight(1f),
            )
            IdeaOutlinedButton(onClick = {
                choosePath(settings.runtimePath, JFileChooser.FILES_ONLY)?.let {
                    settings = settings.copy(runtimePath = it)
                    status = "runtime selected"
                }
            }) { Text("rt.jar") }
        }

        CheckboxRow(
            "Resolve nested jars",
            settings.resolveNestedJars,
            { checked ->
                settings = settings.copy(resolveNestedJars = checked)
            },
        )
        CheckboxRow(
            "Auto find rt.jar",
            settings.autoFindRuntimeJar,
            { checked ->
                settings = settings.copy(autoFindRuntimeJar = checked)
            },
        )
        CheckboxRow(
            "Add rt.jar when analyze",
            settings.addRuntimeJar,
            { checked ->
                settings = settings.copy(addRuntimeJar = checked)
            },
        )
        CheckboxRow(
            "Delete temp before build",
            settings.deleteTempBeforeBuild,
            { checked ->
                settings = settings.copy(deleteTempBeforeBuild = checked)
            },
        )
        CheckboxRow(
            "Fix class path",
            settings.fixClassPath,
            { checked ->
                settings = settings.copy(fixClassPath = checked)
            },
        )
        CheckboxRow(
            "Fix method impl/override",
            settings.fixMethodImpl,
            { checked ->
                settings = settings.copy(fixMethodImpl = checked)
            },
        )
        CheckboxRow(
            "Quick mode",
            settings.quickMode,
            { checked ->
                settings = settings.copy(quickMode = checked)
            },
        )
        Text("Rules")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IdeaOutlinedButton(onClick = {
                status = editPrefixRules(PrefixRuleType.BUILD_BLACKLIST)
            }) {
                Text("Build Blacklist")
            }
            IdeaOutlinedButton(onClick = {
                status = editPrefixRules(PrefixRuleType.BUILD_WHITELIST)
            }) {
                Text("Build Whitelist")
            }
            IdeaOutlinedButton(onClick = {
                status = editPrefixRules(PrefixRuleType.SEARCH_FILTER)
            }) {
                Text("Search Filter")
            }
        }
        Divider(Orientation.Horizontal, Modifier.fillMaxWidth())

        Text("Decompiler")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RadioButtonRow(
                "FernFlower",
                decompiler == "fernflower",
                {
                    decompiler = "fernflower"
                    System.setProperty("jar-analyzer.decompiler", decompiler)
                    status = "decompiler: fernflower"
                },
            )
            RadioButtonRow(
                "CFR",
                decompiler == "cfr",
                {
                    decompiler = "cfr"
                    System.setProperty("jar-analyzer.decompiler", decompiler)
                    status = "decompiler: cfr"
                },
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IdeaDefaultButton(onClick = {
                buildService.applySettings(settings)
                status = "settings applied"
            }) {
                Text("Apply")
            }
            IdeaDefaultButton(onClick = {
                buildService.applySettings(settings)
                buildService.startBuild()
                status = "build started"
            }) {
                Text("Start Build")
            }
            IdeaOutlinedButton(onClick = {
                buildService.clearCache()
                status = "clean requested"
            }) {
                Text("Clean")
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IdeaOutlinedButton(onClick = { RuntimeFacades.tooling().openAllStringsTool() }) {
                Text("All Strings")
            }
            IdeaOutlinedButton(onClick = { RuntimeFacades.tooling().openElSearchTool() }) {
                Text("EL Search")
            }
            IdeaOutlinedButton(onClick = { RuntimeFacades.tooling().openJdGui() }) {
                Text("JD-GUI")
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IdeaOutlinedButton(onClick = { RuntimeFacades.tooling().openOpcodeTool() }) {
                Text("Opcode")
            }
            IdeaOutlinedButton(onClick = { RuntimeFacades.tooling().openAsmTool() }) {
                Text("ASM")
            }
            IdeaOutlinedButton(onClick = { RuntimeFacades.tooling().openCfgTool() }) {
                Text("CFG")
            }
            IdeaOutlinedButton(onClick = { RuntimeFacades.tooling().openHtmlGraph() }) {
                Text("HTML Graph")
            }
        }

        Text("Progress ${snapshot.buildProgress}% / Engine ${snapshot.engineStatus}")
        HorizontalProgressBar(snapshot.buildProgress.coerceIn(0, 100) / 100f, Modifier.fillMaxWidth())
        Divider(Orientation.Horizontal, Modifier.fillMaxWidth())

        LabelValue("jar", snapshot.totalJar)
        LabelValue("class", snapshot.totalClass)
        LabelValue("method", snapshot.totalMethod)
        LabelValue("edge", snapshot.totalEdge)
        LabelValue("database", snapshot.databaseSize)

        Text(status)
    }
}

private fun choosePath(current: String, fileSelectionMode: Int): String? {
    return try {
        val chooser = JFileChooser()
        chooser.fileSelectionMode = fileSelectionMode
        chooser.isMultiSelectionEnabled = false
        val base = current.trim()
        if (base.isNotEmpty()) {
            val file = File(base)
            chooser.currentDirectory = if (file.isDirectory) file else file.parentFile
            chooser.selectedFile = file
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

private enum class PrefixRuleType(
    val title: String,
    val okStatus: String,
) {
    BUILD_BLACKLIST("Edit Build Blacklist", "build blacklist saved"),
    BUILD_WHITELIST("Edit Build Whitelist", "build whitelist saved"),
    SEARCH_FILTER("Edit Search Filter", "search filter saved"),
}

private fun editPrefixRules(type: PrefixRuleType): String {
    return try {
        val classArea = JTextArea()
        val jarArea = JTextArea()
        when (type) {
            PrefixRuleType.BUILD_BLACKLIST -> {
                classArea.text = buildPrefixText(CommonBlacklistUtil.getClassPrefixes())
                jarArea.text = buildPrefixText(CommonBlacklistUtil.getJarPrefixes())
            }
            PrefixRuleType.BUILD_WHITELIST -> {
                classArea.text = buildPrefixText(CommonWhitelistUtil.getClassPrefixes())
                jarArea.text = buildPrefixText(CommonWhitelistUtil.getJarPrefixes())
            }
            PrefixRuleType.SEARCH_FILTER -> {
                classArea.text = buildPrefixText(CommonFilterUtil.getClassPrefixes())
                jarArea.text = buildPrefixText(CommonFilterUtil.getJarPrefixes())
            }
        }

        val center = JPanel(GridLayout(2, 1, 0, 8))
        center.add(
            JPanel(BorderLayout(0, 4)).apply {
                add(JLabel("Class / Package Prefixes"), BorderLayout.NORTH)
                add(JScrollPane(classArea), BorderLayout.CENTER)
            },
        )
        center.add(
            JPanel(BorderLayout(0, 4)).apply {
                add(JLabel("JAR Prefixes"), BorderLayout.NORTH)
                add(JScrollPane(jarArea), BorderLayout.CENTER)
            },
        )
        val panel = JPanel(BorderLayout(0, 8)).apply {
            add(center, BorderLayout.CENTER)
        }
        val result = JOptionPane.showConfirmDialog(
            null,
            panel,
            type.title,
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE,
        )
        if (result != JOptionPane.OK_OPTION) {
            return "rules edit cancelled"
        }
        val classList = ListParser.parse(classArea.text)
        val jarList = parseJarList(jarArea.text)
        when (type) {
            PrefixRuleType.BUILD_BLACKLIST -> {
                CommonBlacklistUtil.saveClassPrefixes(classList)
                CommonBlacklistUtil.saveJarPrefixes(jarList)
                CommonBlacklistUtil.reload()
            }
            PrefixRuleType.BUILD_WHITELIST -> {
                CommonWhitelistUtil.saveClassPrefixes(classList)
                CommonWhitelistUtil.saveJarPrefixes(jarList)
                CommonWhitelistUtil.reload()
            }
            PrefixRuleType.SEARCH_FILTER -> {
                CommonFilterUtil.saveClassPrefixes(classList)
                CommonFilterUtil.saveJarPrefixes(jarList)
                CommonFilterUtil.reload()
            }
        }
        type.okStatus
    } catch (ex: Throwable) {
        "save rules failed: ${ex.message}"
    }
}

private fun buildPrefixText(values: List<String>?): String {
    if (values == null || values.isEmpty()) {
        return ""
    }
    return values
        .asSequence()
        .filterNotNull()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .joinToString("\n")
}

private fun parseJarList(text: String): ArrayList<String> {
    val out = ArrayList<String>()
    val raw = text.trim()
    if (raw.isEmpty()) {
        return out
    }
    raw.split('\n').forEach { line ->
        val trimmed = line.trim().trimEnd('\r')
        if (trimmed.isEmpty()) {
            return@forEach
        }
        if (trimmed.startsWith("#") || trimmed.startsWith("//") || trimmed.startsWith("/*")) {
            return@forEach
        }
        val parts = if (trimmed.contains(';')) trimmed.split(';') else listOf(trimmed)
        parts.forEach { part ->
            var value = part.trim()
            if (value.isEmpty()) {
                return@forEach
            }
            while (value.endsWith("*")) {
                value = value.dropLast(1)
            }
            if (value.isNotEmpty()) {
                out.add(value)
            }
        }
    }
    return out
}

@Composable
fun MigratedSearchTab(searchService: SearchService) {
    val initial = remember { searchService.snapshot() }
    var query by remember { mutableStateOf(initial.query) }
    var results by remember { mutableStateOf(initial.results) }
    var status by remember { mutableStateOf("ready") }

    LaunchedEffect(Unit) {
        while (true) {
            results = searchService.snapshot().results
            delay(1000)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Search")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RadioButtonRow(
                "method call",
                query.mode == SearchMode.METHOD_CALL,
                { query = query.copy(mode = SearchMode.METHOD_CALL) },
            )
            RadioButtonRow(
                "method definition",
                query.mode == SearchMode.METHOD_DEFINITION,
                { query = query.copy(mode = SearchMode.METHOD_DEFINITION) },
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RadioButtonRow(
                "string contains",
                query.mode == SearchMode.STRING_CONTAINS,
                { query = query.copy(mode = SearchMode.STRING_CONTAINS) },
            )
            RadioButtonRow(
                "binary contains",
                query.mode == SearchMode.BINARY_CONTAINS,
                { query = query.copy(mode = SearchMode.BINARY_CONTAINS) },
            )
        }

        if (query.mode != SearchMode.BINARY_CONTAINS) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                RadioButtonRow(
                    "like",
                    query.matchMode == SearchMatchMode.LIKE,
                    { query = query.copy(matchMode = SearchMatchMode.LIKE) },
                )
                RadioButtonRow(
                    "equals",
                    query.matchMode == SearchMatchMode.EQUALS,
                    { query = query.copy(matchMode = SearchMatchMode.EQUALS) },
                )
            }
        }

        BoundTextField(
            query.className,
            { query = query.copy(className = it) },
            Modifier.fillMaxWidth(),
        )
        BoundTextField(
            query.methodName,
            { query = query.copy(methodName = it) },
            Modifier.fillMaxWidth(),
        )
        BoundTextField(
            query.keyword,
            { query = query.copy(keyword = it) },
            Modifier.fillMaxWidth(),
        )
        CheckboxRow(
            "Filter null-arg methods",
            query.nullParamFilter,
            { query = query.copy(nullParamFilter = it) },
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IdeaOutlinedButton(onClick = {
                status = editPrefixRules(PrefixRuleType.SEARCH_FILTER)
            }) {
                Text("Search Filter")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IdeaDefaultButton(onClick = {
                searchService.applyQuery(query)
                searchService.runSearch()
                status = "search started"
            }) {
                Text("Search")
            }
            IdeaOutlinedButton(onClick = {
                val refreshed = searchService.snapshot()
                results = refreshed.results
                status = "results ${results.size}"
            }) {
                Text("Refresh")
            }
        }

        Divider(Orientation.Horizontal, Modifier.fillMaxWidth())
        Text("Result ${results.size}")
        LazyColumn(
            modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp, max = 420.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            itemsIndexed(results) { index, item ->
                SearchRow(index = index, item = item, onOpen = {
                    searchService.openResult(index)
                    status = "open ${item.methodName}"
                })
            }
        }
        Text(status)
    }
}

@Composable
private fun SearchRow(
    index: Int,
    item: SearchResultItem,
    onOpen: () -> Unit,
) {
    val label = remember(item) {
        if (item.preview.isBlank()) {
            "${item.className}#${item.methodName}${item.methodDesc}"
        } else {
            item.preview
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .ideaInteractiveClickable(onClick = onOpen)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("${index + 1}.")
        Text(label, modifier = Modifier.weight(1f))
        if (item.jarName.isNotBlank()) {
            Text(item.jarName, modifier = Modifier.width(180.dp))
        }
    }
    Divider(Orientation.Horizontal, Modifier.fillMaxWidth())
}

@Composable
fun MigratedApiTab(mcpService: McpService) {
    val initialCfg = remember { mcpService.currentConfig() }
    var apiInfo by remember { mutableStateOf(mcpService.apiInfo()) }
    var baseConfig by remember { mutableStateOf(initialCfg) }
    var draft by remember { mutableStateOf(initialCfg.toDraft()) }
    var status by remember { mutableStateOf("ready") }

    LaunchedEffect(Unit) {
        while (true) {
            val latestCfg = mcpService.currentConfig()
            baseConfig = latestCfg
            apiInfo = mcpService.apiInfo()
            draft = draft.withRuntime(latestCfg)
            delay(1200)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ApiInfoSection(apiInfo = apiInfo, mcpService = mcpService)
        Divider(Orientation.Horizontal, Modifier.fillMaxWidth())

        Text("MCP")
        BoundTextField(
            draft.bind,
            { draft = draft.copy(bind = it) },
            Modifier.fillMaxWidth(),
        )
        BoundTextField(
            draft.token,
            { draft = draft.copy(token = it) },
            Modifier.fillMaxWidth(),
        )
        CheckboxRow(
            "Enable auth",
            draft.authEnabled,
            { checked ->
                draft = draft.copy(authEnabled = checked)
            },
        )

        draft.lines.forEach { line ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CheckboxRow(
                    line.key.id,
                    line.enabled,
                    { checked ->
                        draft = draft.updateLine(line.key) { it.copy(enabled = checked) }
                    },
                    modifier = Modifier.weight(1f),
                )
                BoundTextField(
                    line.portText,
                    { value ->
                        draft = draft.updateLine(line.key) { it.copy(portText = value) }
                    },
                    Modifier.width(110.dp),
                )
                Text(if (line.running) "RUNNING" else "STOPPED")
            }
        }

        Divider(Orientation.Horizontal, Modifier.fillMaxWidth())
        Text("Report Web")
        CheckboxRow(
            "Enable report web",
            draft.reportWebEnabled,
            { checked ->
                draft = draft.copy(reportWebEnabled = checked)
            },
        )
        BoundTextField(
            draft.reportWebHost,
            { draft = draft.copy(reportWebHost = it) },
            Modifier.fillMaxWidth(),
        )
        BoundTextField(
            draft.reportWebPortText,
            { draft = draft.copy(reportWebPortText = it) },
            Modifier.width(160.dp),
        )
        Text(if (draft.reportWebRunning) "Report Web RUNNING" else "Report Web STOPPED")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IdeaDefaultButton(onClick = {
                val target = draft.toConfig(baseConfig)
                val errors = mcpService.applyAndRestart(target)
                status = if (errors.isEmpty()) "mcp started" else errors.joinToString(" | ")
                val refreshed = mcpService.currentConfig()
                baseConfig = refreshed
                draft = refreshed.toDraft()
            }) {
                Text("Apply + Start")
            }
            IdeaOutlinedButton(onClick = {
                val errors = mcpService.startConfigured()
                status = if (errors.isEmpty()) "mcp started from saved config" else errors.joinToString(" | ")
            }) {
                Text("Start Saved")
            }
            IdeaOutlinedButton(onClick = {
                mcpService.stopAll()
                status = "mcp stopped"
            }) {
                Text("Stop All")
            }
            IdeaOutlinedButton(onClick = {
                mcpService.openReportWeb(draft.reportWebHost, draft.reportWebPortText.toIntOrNull() ?: 20080)
            }) {
                Text("Open Report")
            }
        }
        Text(status)
    }
}

@Composable
private fun ApiInfoSection(
    apiInfo: ApiInfo,
    mcpService: McpService,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("API")
        LabelValue("bind", apiInfo.bind)
        LabelValue("port", apiInfo.port.toString())
        LabelValue("auth", if (apiInfo.authEnabled) "TRUE" else "FALSE")
        LabelValue("token", apiInfo.maskedToken)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IdeaOutlinedButton(onClick = { mcpService.openApiDoc() }) {
                Text("API Doc")
            }
            IdeaOutlinedButton(onClick = { mcpService.openMcpDoc() }) {
                Text("MCP Doc")
            }
            IdeaOutlinedButton(onClick = { mcpService.openN8nDoc() }) {
                Text("n8n Doc")
            }
        }
    }
}

@Composable
private fun LabelValue(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.width(120.dp))
        Text(value)
    }
}

private data class McpLineDraft(
    val key: McpLineKey,
    val enabled: Boolean,
    val portText: String,
    val running: Boolean,
)

private data class McpDraft(
    val bind: String,
    val authEnabled: Boolean,
    val token: String,
    val lines: List<McpLineDraft>,
    val reportWebEnabled: Boolean,
    val reportWebHost: String,
    val reportWebPortText: String,
    val reportWebRunning: Boolean,
)

private fun McpConfig.toDraft(): McpDraft {
    return McpDraft(
        bind = bind,
        authEnabled = authEnabled,
        token = token,
        lines = lines.map { it.toDraft() },
        reportWebEnabled = reportWebEnabled,
        reportWebHost = reportWebHost,
        reportWebPortText = reportWebPort.toString(),
        reportWebRunning = reportWebRunning,
    )
}

private fun McpLineConfig.toDraft(): McpLineDraft {
    return McpLineDraft(
        key = key,
        enabled = enabled,
        portText = port.toString(),
        running = running,
    )
}

private fun McpDraft.updateLine(key: McpLineKey, transform: (McpLineDraft) -> McpLineDraft): McpDraft {
    return copy(lines = lines.map { line ->
        if (line.key == key) {
            transform(line)
        } else {
            line
        }
    })
}

private fun McpDraft.withRuntime(config: McpConfig): McpDraft {
    val runtime = config.lines.associateBy { it.key }
    val merged = lines.map { line ->
        val latest = runtime[line.key]
        if (latest == null) {
            line
        } else {
            line.copy(running = latest.running)
        }
    }
    return copy(
        lines = merged,
        reportWebRunning = config.reportWebRunning,
    )
}

private fun McpDraft.toConfig(base: McpConfig): McpConfig {
    val baseMap = base.lines.associateBy { it.key }
    val mapped = lines.map { line ->
        val baseLine = baseMap[line.key]
        val fallback = baseLine?.port ?: 1
        val parsedPort = line.portText.toIntOrNull() ?: fallback
        McpLineConfig(
            key = line.key,
            enabled = line.enabled,
            port = parsedPort.coerceIn(1, 65535),
            running = line.running,
        )
    }
    val reportWebPort = (reportWebPortText.toIntOrNull() ?: base.reportWebPort).coerceIn(1, 65535)
    return McpConfig(
        bind = bind,
        authEnabled = authEnabled,
        token = token,
        lines = mapped,
        reportWebEnabled = reportWebEnabled,
        reportWebHost = reportWebHost,
        reportWebPort = reportWebPort,
        reportWebRunning = reportWebRunning,
    )
}
