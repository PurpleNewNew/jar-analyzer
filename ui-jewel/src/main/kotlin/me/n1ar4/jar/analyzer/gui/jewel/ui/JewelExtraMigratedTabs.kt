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
import me.n1ar4.jar.analyzer.gui.jewel.service.AdvanceService
import me.n1ar4.jar.analyzer.gui.jewel.service.CallImplService
import me.n1ar4.jar.analyzer.gui.jewel.service.ChainsService
import me.n1ar4.jar.analyzer.gui.jewel.service.ChainsSettings
import me.n1ar4.jar.analyzer.gui.jewel.service.ClassNavItem
import me.n1ar4.jar.analyzer.gui.jewel.service.GadgetService
import me.n1ar4.jar.analyzer.gui.jewel.service.GadgetSettings
import me.n1ar4.jar.analyzer.gui.jewel.service.LeakRules
import me.n1ar4.jar.analyzer.gui.jewel.service.LeakService
import me.n1ar4.jar.analyzer.gui.jewel.service.MethodNavItem
import me.n1ar4.jar.analyzer.gui.jewel.service.NoteService
import me.n1ar4.jar.analyzer.gui.jewel.service.ScaOutputMode
import me.n1ar4.jar.analyzer.gui.jewel.service.ScaService
import me.n1ar4.jar.analyzer.gui.jewel.service.ScaSettings
import me.n1ar4.jar.analyzer.gui.jewel.service.WebClassBucket
import me.n1ar4.jar.analyzer.gui.jewel.service.WebSnapshot
import me.n1ar4.jar.analyzer.gui.jewel.service.WebService
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.Orientation
import org.jetbrains.jewel.ui.component.CheckboxRow
import org.jetbrains.jewel.ui.component.Divider
import org.jetbrains.jewel.ui.component.RadioButtonRow
import org.jetbrains.jewel.ui.component.SimpleTabContent
import org.jetbrains.jewel.ui.component.TabData
import org.jetbrains.jewel.ui.component.TabStrip
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.theme.defaultTabStyle
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths

@Composable
fun MigratedAdvanceTab(advanceService: AdvanceService) {
    var status by remember { mutableStateOf("ready") }
    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Advance")
        Text("Analysis")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IdeaDefaultButton(onClick = {
                advanceService.showCfg()
                status = "cfg opened"
            }) { Text("CFG") }
            IdeaDefaultButton(onClick = {
                advanceService.showHtmlGraph()
                status = "html graph opened"
            }) { Text("HTML Graph") }
            IdeaOutlinedButton(onClick = {
                advanceService.showFrame(full = false)
                status = "simple frame opened"
            }) { Text("Simple Frame") }
            IdeaOutlinedButton(onClick = {
                advanceService.showFrame(full = true)
                status = "full frame opened"
            }) { Text("Full Frame") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IdeaOutlinedButton(onClick = {
                advanceService.showOpcode()
                status = "opcode opened"
            }) { Text("Opcode") }
            IdeaOutlinedButton(onClick = {
                advanceService.showAsm()
                status = "asm opened"
            }) { Text("ASM") }
        }
        Divider(Orientation.Horizontal, Modifier.fillMaxWidth())

        Text("Plugins")
        ToolStarterRow(
            name = "SQLite Query Tool",
            desc = "Run custom query in sqlite database",
            onStart = {
                RuntimeFacades.tooling().openSqlConsoleTool()
                status = "sqlite opened"
            },
        )
        ToolStarterRow(
            name = "Encode / Decode Tool",
            desc = "Encode decode encrypt decrypt helper",
            onStart = {
                RuntimeFacades.tooling().openEncodeTool()
                status = "encode tool opened"
            },
        )
        ToolStarterRow(
            name = "Socket Listener Tool",
            desc = "Socket send/listen utility",
            onStart = {
                RuntimeFacades.tooling().openListenerTool()
                status = "listener opened"
            },
        )
        ToolStarterRow(
            name = "Spring EL Search Tool",
            desc = "Search EL expression strings",
            onStart = {
                RuntimeFacades.tooling().openElSearchTool()
                status = "el search opened"
            },
        )
        ToolStarterRow(
            name = "Serialization Tool",
            desc = "Analyze serialization payload bytes",
            onStart = {
                RuntimeFacades.tooling().openSerializationTool()
                status = "serialization opened"
            },
        )
        ToolStarterRow(
            name = "BCEL Tool",
            desc = "Parse BCEL/bytecode text",
            onStart = {
                RuntimeFacades.tooling().openBcelTool()
                status = "bcel opened"
            },
        )
        Text(status)
    }
}

@Composable
fun MigratedCallTab(callImplService: CallImplService) {
    var snapshot by remember { mutableStateOf(callImplService.snapshot()) }
    LaunchedEffect(Unit) {
        while (true) {
            snapshot = callImplService.snapshot()
            delay(900)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Call")
        Text("jar: ${snapshot.currentJar}")
        Text("class: ${snapshot.currentClass}")
        Text("method: ${snapshot.currentMethod}")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IdeaDefaultButton(onClick = { callImplService.refreshCurrentContext() }) { Text("Refresh") }
        }
        Divider(Orientation.Horizontal, Modifier.fillMaxWidth())
        Text("All Methods ${snapshot.allMethods.size}")
        MethodList(snapshot.allMethods, onOpen = { callImplService.openAllMethod(it) })
        Text("Callers ${snapshot.callers.size}")
        MethodList(snapshot.callers, onOpen = { callImplService.openCaller(it) })
        Text("Callees ${snapshot.callees.size}")
        MethodList(snapshot.callees, onOpen = { callImplService.openCallee(it) })
    }
}

@Composable
fun MigratedImplTab(callImplService: CallImplService) {
    var snapshot by remember { mutableStateOf(callImplService.snapshot()) }
    LaunchedEffect(Unit) {
        while (true) {
            snapshot = callImplService.snapshot()
            delay(950)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Impl")
        Text("class: ${snapshot.currentClass}")
        Text("method: ${snapshot.currentMethod}")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IdeaDefaultButton(onClick = { callImplService.refreshCurrentContext() }) { Text("Refresh") }
        }
        Divider(Orientation.Horizontal, Modifier.fillMaxWidth())
        Text("Impl ${snapshot.impls.size}")
        MethodList(snapshot.impls, onOpen = { callImplService.openImpl(it) })
        Text("Super Impl ${snapshot.superImpls.size}")
        MethodList(snapshot.superImpls, onOpen = { callImplService.openSuperImpl(it) })
    }
}

@Composable
fun MigratedWebTab(webService: WebService) {
    var snapshot by remember { mutableStateOf(webService.snapshot()) }
    var keyword by remember { mutableStateOf(snapshot.pathKeyword) }
    var viewTab by remember { mutableStateOf(WebViewTab.CONTROLLER) }
    var status by remember { mutableStateOf("ready") }
    LaunchedEffect(Unit) {
        while (true) {
            snapshot = webService.snapshot()
            delay(1000)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Web")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            BoundTextField(keyword, { keyword = it }, Modifier.weight(1f))
            IdeaDefaultButton(onClick = {
                webService.pathSearch(keyword)
                status = "path search submitted"
            }) { Text("Path Search") }
            IdeaOutlinedButton(onClick = {
                webService.refreshAll()
                status = "web refreshed"
            }) { Text("Refresh All") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IdeaOutlinedButton(onClick = { status = exportWebSnapshot(snapshot, "json") }) { Text("Export JSON") }
            IdeaOutlinedButton(onClick = { status = exportWebSnapshot(snapshot, "txt") }) { Text("Export TXT") }
            IdeaOutlinedButton(onClick = { status = exportWebSnapshot(snapshot, "csv") }) { Text("Export CSV") }
        }
        Divider(Orientation.Horizontal, Modifier.fillMaxWidth())

        TabStrip(
            tabs = WebViewTab.entries.map { tab ->
                TabData.Default(
                    selected = tab == viewTab,
                    content = { tabState ->
                        SimpleTabContent(
                            label = "${tab.label} (${webViewCount(snapshot, tab)})",
                            state = tabState,
                        )
                    },
                    closable = false,
                    onClose = {},
                    onClick = { viewTab = tab },
                )
            },
            style = JewelTheme.defaultTabStyle,
            modifier = Modifier.fillMaxWidth(),
        )
        when (viewTab) {
            WebViewTab.CONTROLLER -> {
                Text("Controllers ${snapshot.controllers.size}")
                ClassList(snapshot.controllers, onOpen = { webService.openClass(WebClassBucket.CONTROLLER, it) })
            }
            WebViewTab.MAPPING -> {
                Text("Mappings ${snapshot.mappings.size}")
                MethodList(snapshot.mappings, onOpen = { webService.openMapping(it) })
            }
            WebViewTab.INTERCEPTOR -> {
                Text("Interceptors ${snapshot.interceptors.size}")
                ClassList(snapshot.interceptors, onOpen = { webService.openClass(WebClassBucket.INTERCEPTOR, it) })
            }
            WebViewTab.SERVLET -> {
                Text("Servlets ${snapshot.servlets.size}")
                ClassList(snapshot.servlets, onOpen = { webService.openClass(WebClassBucket.SERVLET, it) })
            }
            WebViewTab.FILTER -> {
                Text("Filters ${snapshot.filters.size}")
                ClassList(snapshot.filters, onOpen = { webService.openClass(WebClassBucket.FILTER, it) })
            }
            WebViewTab.LISTENER -> {
                Text("Listeners ${snapshot.listeners.size}")
                ClassList(snapshot.listeners, onOpen = { webService.openClass(WebClassBucket.LISTENER, it) })
            }
        }
        Text(status)
    }
}

private enum class WebViewTab(val label: String) {
    CONTROLLER("spring controller"),
    MAPPING("spring mapping"),
    INTERCEPTOR("spring interceptor"),
    SERVLET("servlet"),
    FILTER("filter"),
    LISTENER("listener"),
}

private fun webViewCount(snapshot: WebSnapshot, tab: WebViewTab): Int {
    return when (tab) {
        WebViewTab.CONTROLLER -> snapshot.controllers.size
        WebViewTab.MAPPING -> snapshot.mappings.size
        WebViewTab.INTERCEPTOR -> snapshot.interceptors.size
        WebViewTab.SERVLET -> snapshot.servlets.size
        WebViewTab.FILTER -> snapshot.filters.size
        WebViewTab.LISTENER -> snapshot.listeners.size
    }
}

@Composable
private fun ToolStarterRow(
    name: String,
    desc: String,
    onStart: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(name, modifier = Modifier.width(210.dp))
        Text(desc, modifier = Modifier.weight(1f))
        IdeaOutlinedButton(onClick = onStart) { Text("Start") }
    }
}

private fun exportWebSnapshot(snapshot: WebSnapshot, format: String): String {
    return try {
        val now = System.currentTimeMillis()
        val ext = format.lowercase()
        val path = Paths.get("web-export-$now.$ext").toAbsolutePath()
        val text = when (ext) {
            "json" -> renderWebSnapshotJson(snapshot)

            "csv" -> buildString {
                appendLine("type,class,method,desc,jar")
                snapshot.controllers.forEach { appendLine("controller,${it.className},,,${it.jarName}") }
                snapshot.interceptors.forEach { appendLine("interceptor,${it.className},,,${it.jarName}") }
                snapshot.servlets.forEach { appendLine("servlet,${it.className},,,${it.jarName}") }
                snapshot.filters.forEach { appendLine("filter,${it.className},,,${it.jarName}") }
                snapshot.listeners.forEach { appendLine("listener,${it.className},,,${it.jarName}") }
                snapshot.mappings.forEach {
                    appendLine("mapping,${it.className},${it.methodName},${it.methodDesc},${it.jarName}")
                }
            }

            else -> buildString {
                appendLine("keyword=${snapshot.pathKeyword}")
                appendLine()
                appendLine("[controllers]")
                snapshot.controllers.forEach { appendLine("${it.className} [${it.jarName}]") }
                appendLine()
                appendLine("[mappings]")
                snapshot.mappings.forEach {
                    appendLine("${it.className}#${it.methodName}${it.methodDesc} [${it.jarName}]")
                }
                appendLine()
                appendLine("[interceptors]")
                snapshot.interceptors.forEach { appendLine("${it.className} [${it.jarName}]") }
                appendLine()
                appendLine("[servlets]")
                snapshot.servlets.forEach { appendLine("${it.className} [${it.jarName}]") }
                appendLine()
                appendLine("[filters]")
                snapshot.filters.forEach { appendLine("${it.className} [${it.jarName}]") }
                appendLine()
                appendLine("[listeners]")
                snapshot.listeners.forEach { appendLine("${it.className} [${it.jarName}]") }
            }
        }
        Files.writeString(path, text, StandardCharsets.UTF_8)
        "exported: $path"
    } catch (ex: Throwable) {
        "export failed: ${ex.message}"
    }
}

private fun renderWebSnapshotJson(snapshot: WebSnapshot): String {
    return buildString {
        appendLine("{")
        appendLine("  \"keyword\": \"${jsonEscape(snapshot.pathKeyword)}\",")
        appendLine("  \"controllers\": ${classNavListJson(snapshot.controllers)},")
        appendLine("  \"mappings\": ${methodNavListJson(snapshot.mappings)},")
        appendLine("  \"interceptors\": ${classNavListJson(snapshot.interceptors)},")
        appendLine("  \"servlets\": ${classNavListJson(snapshot.servlets)},")
        appendLine("  \"filters\": ${classNavListJson(snapshot.filters)},")
        appendLine("  \"listeners\": ${classNavListJson(snapshot.listeners)}")
        append('}')
    }
}

private fun classNavListJson(rows: List<ClassNavItem>): String {
    if (rows.isEmpty()) {
        return "[]"
    }
    return rows.joinToString(prefix = "[", postfix = "]") { row ->
        "{\"className\":\"${jsonEscape(row.className)}\",\"jarName\":\"${jsonEscape(row.jarName)}\",\"jarId\":${row.jarId}}"
    }
}

private fun methodNavListJson(rows: List<MethodNavItem>): String {
    if (rows.isEmpty()) {
        return "[]"
    }
    return rows.joinToString(prefix = "[", postfix = "]") { row ->
        "{" +
            "\"className\":\"${jsonEscape(row.className)}\"," +
            "\"methodName\":\"${jsonEscape(row.methodName)}\"," +
            "\"methodDesc\":\"${jsonEscape(row.methodDesc)}\"," +
            "\"jarName\":\"${jsonEscape(row.jarName)}\"," +
            "\"jarId\":${row.jarId}" +
            "}"
    }
}

private fun jsonEscape(value: String): String {
    val raw = value
    return buildString(raw.length + 8) {
        raw.forEach { ch ->
            when (ch) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(ch)
            }
        }
    }
}

@Composable
fun MigratedNoteTab(noteService: NoteService) {
    var snapshot by remember { mutableStateOf(noteService.snapshot()) }
    LaunchedEffect(Unit) {
        while (true) {
            snapshot = noteService.snapshot()
            delay(1000)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Note")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IdeaDefaultButton(onClick = { noteService.load() }) { Text("Load history/favorites") }
            IdeaOutlinedButton(onClick = { noteService.clearHistory() }) { Text("Clear History") }
            IdeaOutlinedButton(onClick = { noteService.clearFavorites() }) { Text("Clear Fav") }
        }
        Divider(Orientation.Horizontal, Modifier.fillMaxWidth())
        Text("History ${snapshot.history.size}")
        MethodList(snapshot.history, onOpen = { noteService.openHistory(it) })
        Text("Favorite ${snapshot.favorites.size}")
        MethodList(snapshot.favorites, onOpen = { noteService.openFavorite(it) })
    }
}

@Composable
fun MigratedScaTab(scaService: ScaService) {
    val initial = remember { scaService.snapshot() }
    var settings by remember { mutableStateOf(initial.settings) }
    var logTail by remember { mutableStateOf(initial.logTail) }

    LaunchedEffect(Unit) {
        while (true) {
            val latest = scaService.snapshot()
            logTail = latest.logTail
            if (latest.settings.outputFile.isNotBlank()) {
                settings = settings.copy(outputFile = latest.settings.outputFile)
            }
            delay(1200)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("SCA")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CheckboxRow("log4j", settings.scanLog4j, { settings = settings.copy(scanLog4j = it) })
            CheckboxRow("shiro", settings.scanShiro, { settings = settings.copy(scanShiro = it) })
            CheckboxRow("fastjson", settings.scanFastjson, { settings = settings.copy(scanFastjson = it) })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            BoundTextField(settings.inputPath, { settings = settings.copy(inputPath = it) }, Modifier.weight(1f))
            IdeaOutlinedButton(onClick = { scaService.chooseInput() }) { Text("Choose") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RadioButtonRow(
                text = "html",
                selected = settings.outputMode == ScaOutputMode.HTML,
                onClick = { settings = settings.copy(outputMode = ScaOutputMode.HTML) },
            )
            RadioButtonRow(
                text = "console",
                selected = settings.outputMode == ScaOutputMode.CONSOLE,
                onClick = { settings = settings.copy(outputMode = ScaOutputMode.CONSOLE) },
            )
        }
        BoundTextField(settings.outputFile, { settings = settings.copy(outputFile = it) }, Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IdeaDefaultButton(onClick = { scaService.apply(settings) }) { Text("Apply") }
            IdeaDefaultButton(onClick = {
                scaService.apply(settings)
                scaService.start()
            }) { Text("Start") }
            IdeaOutlinedButton(onClick = { scaService.openResult() }) { Text("Open Result") }
        }
        Divider(Orientation.Horizontal, Modifier.fillMaxWidth())
        Text(logTail)
    }
}

@Composable
fun MigratedLeakTab(leakService: LeakService) {
    val initial = remember { leakService.snapshot() }
    var rules by remember { mutableStateOf(initial.rules) }
    var results by remember { mutableStateOf(initial.results) }
    var logTail by remember { mutableStateOf(initial.logTail) }

    LaunchedEffect(Unit) {
        while (true) {
            val latest = leakService.snapshot()
            results = latest.results
            logTail = latest.logTail
            delay(1200)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Leak")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CheckboxRow("url", rules.url, { rules = rules.copy(url = it) })
            CheckboxRow("jdbc", rules.jdbc, { rules = rules.copy(jdbc = it) })
            CheckboxRow("file", rules.filePath, { rules = rules.copy(filePath = it) })
            CheckboxRow("jwt", rules.jwt, { rules = rules.copy(jwt = it) })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CheckboxRow("mac", rules.mac, { rules = rules.copy(mac = it) })
            CheckboxRow("ip", rules.ip, { rules = rules.copy(ip = it) })
            CheckboxRow("phone", rules.phone, { rules = rules.copy(phone = it) })
            CheckboxRow("id", rules.idCard, { rules = rules.copy(idCard = it) })
            CheckboxRow("email", rules.email, { rules = rules.copy(email = it) })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CheckboxRow("api", rules.apiKey, { rules = rules.copy(apiKey = it) })
            CheckboxRow("bank", rules.bankCard, { rules = rules.copy(bankCard = it) })
            CheckboxRow("ak/sk", rules.cloudAkSk, { rules = rules.copy(cloudAkSk = it) })
            CheckboxRow("crypto", rules.cryptoKey, { rules = rules.copy(cryptoKey = it) })
            CheckboxRow("ai", rules.aiKey, { rules = rules.copy(aiKey = it) })
            CheckboxRow("password", rules.password, { rules = rules.copy(password = it) })
        }
        CheckboxRow("detect base64", rules.detectBase64, { rules = rules.copy(detectBase64 = it) })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IdeaDefaultButton(onClick = { leakService.apply(rules) }) { Text("Apply") }
            IdeaDefaultButton(onClick = {
                leakService.apply(rules)
                leakService.start()
            }) { Text("Start") }
            IdeaOutlinedButton(onClick = { leakService.export() }) { Text("Export") }
            IdeaOutlinedButton(onClick = { leakService.clear() }) { Text("Clean") }
        }
        Divider(Orientation.Horizontal, Modifier.fillMaxWidth())
        Text("Result ${results.size}")
        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp, max = 360.dp)) {
            itemsIndexed(results) { index, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .ideaInteractiveClickable { leakService.openResult(index) }
                        .padding(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("${index + 1}.")
                    Text("${item.typeName}: ${item.value}", modifier = Modifier.weight(1f))
                    Text(item.className, modifier = Modifier.width(260.dp))
                }
                Divider(Orientation.Horizontal, Modifier.fillMaxWidth())
            }
        }
        Text(logTail)
    }
}

@Composable
fun MigratedGadgetTab(gadgetService: GadgetService) {
    val initial = remember { gadgetService.snapshot() }
    var settings by remember { mutableStateOf(initial.settings) }
    var rows by remember { mutableStateOf(initial.rows) }

    LaunchedEffect(Unit) {
        while (true) {
            rows = gadgetService.snapshot().rows
            delay(1200)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Gadget")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            BoundTextField(settings.inputDir, { settings = settings.copy(inputDir = it) }, Modifier.weight(1f))
            IdeaOutlinedButton(onClick = { gadgetService.chooseDir() }) { Text("Choose") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CheckboxRow("native", settings.native, { settings = settings.copy(native = it) })
            CheckboxRow("hessian", settings.hessian, { settings = settings.copy(hessian = it) })
            CheckboxRow("jdbc", settings.jdbc, { settings = settings.copy(jdbc = it) })
            CheckboxRow("fastjson", settings.fastjson, { settings = settings.copy(fastjson = it) })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IdeaDefaultButton(onClick = { gadgetService.apply(settings) }) { Text("Apply") }
            IdeaDefaultButton(onClick = {
                gadgetService.apply(settings)
                gadgetService.start()
            }) { Text("Start") }
        }
        Divider(Orientation.Horizontal, Modifier.fillMaxWidth())
        Text("Result ${rows.size}")
        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp, max = 420.dp)) {
            itemsIndexed(rows) { _, row ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(row.id, modifier = Modifier.width(36.dp))
                    Text(row.definition, modifier = Modifier.weight(1f))
                    Text(row.risk, modifier = Modifier.width(220.dp))
                }
                Divider(Orientation.Horizontal, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
fun MigratedChainsTab(chainsService: ChainsService) {
    val initial = remember { chainsService.snapshot() }
    var settings by remember { mutableStateOf(initial.settings) }
    var dfsCount by remember { mutableStateOf(initial.dfsCount) }
    var taintCount by remember { mutableStateOf(initial.taintCount) }

    LaunchedEffect(Unit) {
        while (true) {
            val latest = chainsService.snapshot()
            dfsCount = latest.dfsCount
            taintCount = latest.taintCount
            delay(1100)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Chains")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RadioButtonRow(
                text = "sink",
                selected = settings.sinkSelected,
                onClick = { settings = settings.copy(sinkSelected = true, sourceSelected = false) },
            )
            RadioButtonRow(
                text = "source",
                selected = settings.sourceSelected,
                onClick = { settings = settings.copy(sinkSelected = false, sourceSelected = true) },
            )
        }
        BoundTextField(settings.sinkClass, { settings = settings.copy(sinkClass = it) }, Modifier.fillMaxWidth())
        BoundTextField(settings.sinkMethod, { settings = settings.copy(sinkMethod = it) }, Modifier.fillMaxWidth())
        BoundTextField(settings.sinkDesc, { settings = settings.copy(sinkDesc = it) }, Modifier.fillMaxWidth())
        BoundTextField(settings.sourceClass, { settings = settings.copy(sourceClass = it) }, Modifier.fillMaxWidth())
        BoundTextField(settings.sourceMethod, { settings = settings.copy(sourceMethod = it) }, Modifier.fillMaxWidth())
        BoundTextField(settings.sourceDesc, { settings = settings.copy(sourceDesc = it) }, Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CheckboxRow("source null", settings.sourceNull, { settings = settings.copy(sourceNull = it) })
            CheckboxRow("source enabled", settings.sourceEnabled, { settings = settings.copy(sourceEnabled = it) })
            CheckboxRow("only web", settings.onlyFromWeb, { settings = settings.copy(onlyFromWeb = it) })
            CheckboxRow("taint", settings.taintEnabled, { settings = settings.copy(taintEnabled = it) })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("max depth")
            BoundTextField(
                value = settings.maxDepth.toString(),
                onValueChange = { value ->
                    val depth = value.toIntOrNull() ?: settings.maxDepth
                    settings = settings.copy(maxDepth = depth.coerceIn(1, 200))
                },
                modifier = Modifier.width(100.dp),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IdeaDefaultButton(onClick = { chainsService.apply(settings) }) { Text("Apply") }
            IdeaDefaultButton(onClick = {
                chainsService.apply(settings)
                chainsService.startDfs()
            }) { Text("Start DFS") }
            IdeaOutlinedButton(onClick = {
                chainsService.apply(settings)
                chainsService.startTaint()
            }) { Text("Start Taint") }
            IdeaOutlinedButton(onClick = { chainsService.clearResults() }) { Text("Clear") }
            IdeaOutlinedButton(onClick = { chainsService.openAdvancedBridge() }) { Text("Advanced") }
            IdeaOutlinedButton(onClick = { RuntimeFacades.tooling().openChainsDfsResult() }) { Text("View DFS") }
            IdeaOutlinedButton(onClick = { RuntimeFacades.tooling().openChainsTaintResult() }) { Text("View Taint") }
        }
        Divider(Orientation.Horizontal, Modifier.fillMaxWidth())
        Text("dfs result: $dfsCount")
        Text("taint result: $taintCount")
    }
}

@Composable
private fun MethodList(
    items: List<MethodNavItem>,
    onOpen: (Int) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 260.dp)) {
        itemsIndexed(items) { index, item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .ideaInteractiveClickable { onOpen(index) }
                    .padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("${index + 1}.")
                Text("${item.className}#${item.methodName}${item.methodDesc}", modifier = Modifier.weight(1f))
                if (item.jarName.isNotBlank()) {
                    Text(item.jarName, modifier = Modifier.width(200.dp))
                }
            }
            Divider(Orientation.Horizontal, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ClassList(
    items: List<ClassNavItem>,
    onOpen: (Int) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp, max = 220.dp)) {
        itemsIndexed(items) { index, item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .ideaInteractiveClickable { onOpen(index) }
                    .padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("${index + 1}.")
                Text(item.className, modifier = Modifier.weight(1f))
                if (item.jarName.isNotBlank()) {
                    Text(item.jarName, modifier = Modifier.width(220.dp))
                }
            }
            Divider(Orientation.Horizontal, Modifier.fillMaxWidth())
        }
    }
}
