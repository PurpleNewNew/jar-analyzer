package me.n1ar4.jar.analyzer.gui.jewel

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.jetbrains.JBR
import me.n1ar4.jar.analyzer.cli.StartCmd
import me.n1ar4.jar.analyzer.core.notify.NotifierContext
import me.n1ar4.jar.analyzer.gui.jewel.service.RuntimeAdvanceService
import me.n1ar4.jar.analyzer.gui.jewel.service.RuntimeBuildService
import me.n1ar4.jar.analyzer.gui.jewel.service.RuntimeCallImplService
import me.n1ar4.jar.analyzer.gui.jewel.service.RuntimeChainsService
import me.n1ar4.jar.analyzer.gui.jewel.service.RuntimeEditorService
import me.n1ar4.jar.analyzer.gui.jewel.service.RuntimeGadgetService
import me.n1ar4.jar.analyzer.gui.jewel.service.RuntimeLeakService
import me.n1ar4.jar.analyzer.gui.jewel.service.RuntimeMcpService
import me.n1ar4.jar.analyzer.gui.jewel.service.RuntimeNoteService
import me.n1ar4.jar.analyzer.gui.jewel.service.RuntimeProjectTreeService
import me.n1ar4.jar.analyzer.gui.jewel.service.RuntimeScaService
import me.n1ar4.jar.analyzer.gui.jewel.service.RuntimeSearchService
import me.n1ar4.jar.analyzer.gui.jewel.service.RuntimeWebService
import me.n1ar4.jar.analyzer.gui.jewel.state.JewelThemeMode
import me.n1ar4.jar.analyzer.gui.jewel.state.MainViewModel
import me.n1ar4.jar.analyzer.gui.jewel.state.RightToolPanel
import me.n1ar4.jar.analyzer.gui.jewel.ui.JewelDecoratedTitleBar
import me.n1ar4.jar.analyzer.gui.jewel.ui.JewelMainView
import me.n1ar4.jar.analyzer.gui.jewel.ui.JewelWindowMenuBar
import me.n1ar4.jar.analyzer.gui.jewel.ui.ToolingWindowsHost
import me.n1ar4.jar.analyzer.gui.jewel.ui.ToolingWindowsState
import me.n1ar4.jar.analyzer.gui.notify.JewelNotifier
import me.n1ar4.jar.analyzer.gui.runtime.GuiLauncher
import me.n1ar4.jar.analyzer.gui.runtime.api.RuntimeFacades
import me.n1ar4.jar.analyzer.mcp.McpManager
import me.n1ar4.jar.analyzer.starter.Const
import me.n1ar4.log.LogManager
import me.n1ar4.log.Logger
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.intui.standalone.theme.IntUiTheme
import org.jetbrains.jewel.intui.standalone.theme.createDefaultTextStyle
import org.jetbrains.jewel.intui.standalone.theme.createEditorTextStyle
import org.jetbrains.jewel.intui.standalone.theme.darkThemeDefinition
import org.jetbrains.jewel.intui.standalone.theme.default
import org.jetbrains.jewel.intui.standalone.theme.lightThemeDefinition
import org.jetbrains.jewel.intui.window.decoratedWindow
import org.jetbrains.jewel.intui.window.styling.dark
import org.jetbrains.jewel.intui.window.styling.light
import org.jetbrains.jewel.intui.window.styling.lightWithLightHeader
import org.jetbrains.jewel.ui.ComponentStyling
import org.jetbrains.jewel.window.DecoratedWindow
import org.jetbrains.jewel.window.styling.DecoratedWindowStyle
import org.jetbrains.jewel.window.styling.TitleBarStyle
import java.nio.file.Paths

class JewelGuiLauncher : GuiLauncher {
    private val logger: Logger = LogManager.getLogger()

    override fun launch(startCmd: StartCmd) {
        launchApp(startCmd)
    }

    private fun launchApp(@Suppress("UNUSED_PARAMETER") startCmd: StartCmd) {
        NotifierContext.set(JewelNotifier())

        val buildService = RuntimeBuildService()
        val projectTreeService = RuntimeProjectTreeService()
        val editorService = RuntimeEditorService()
        val searchService = RuntimeSearchService()
        val callImplService = RuntimeCallImplService()
        val webService = RuntimeWebService()
        val noteService = RuntimeNoteService()
        val scaService = RuntimeScaService()
        val leakService = RuntimeLeakService()
        val gadgetService = RuntimeGadgetService()
        val chainsService = RuntimeChainsService()
        val advanceService = RuntimeAdvanceService()
        val mcpService = RuntimeMcpService()

        val viewModel = MainViewModel(
            buildService = buildService,
            searchService = searchService,
            chainsService = chainsService,
            mcpService = mcpService,
        )

        viewModel.setThemeMode(JewelThemeMode.fromRuntimeTheme(RuntimeFacades.tooling().configSnapshot().theme()))
        viewModel.refreshRuntimeState()
        RuntimeFacades.setToolingWindowConsumer { request ->
            ToolingWindowsState.open(request)
        }
        val supportsDecoratedWindow = runCatching { JBR.isAvailable() }.getOrDefault(false)
        if (!supportsDecoratedWindow) {
            logger.warn("JBR unavailable, fallback to standard Window decorations")
        }

        application {
            val state by viewModel.state.collectAsState()
            val projectName = runCatching { Paths.get("").toAbsolutePath().fileName.toString() }.getOrDefault("jar-analyzer")
            val closeApp = {
                try {
                    McpManager.get().stopAll()
                } catch (_: Throwable) {
                }
                RuntimeFacades.setToolingWindowConsumer(null)
                viewModel.dispose()
                exitApplication()
            }
            val defaultTextStyle = JewelTheme.createDefaultTextStyle()
            val editorTextStyle = JewelTheme.createEditorTextStyle()
            val themeDefinition = if (state.themeMode == JewelThemeMode.ISLAND_DARK) {
                JewelTheme.darkThemeDefinition(
                    defaultTextStyle = defaultTextStyle,
                    editorTextStyle = editorTextStyle,
                )
            } else {
                JewelTheme.lightThemeDefinition(
                    defaultTextStyle = defaultTextStyle,
                    editorTextStyle = editorTextStyle,
                )
            }

            if (supportsDecoratedWindow) {
                val decoratedStyling = if (state.themeMode == JewelThemeMode.ISLAND_DARK) {
                    ComponentStyling.default().decoratedWindow(
                        DecoratedWindowStyle.dark(),
                        TitleBarStyle.dark(),
                    )
                } else {
                    ComponentStyling.default().decoratedWindow(
                        DecoratedWindowStyle.light(),
                        TitleBarStyle.lightWithLightHeader(),
                    )
                }
                IntUiTheme(
                    theme = themeDefinition,
                    styling = decoratedStyling,
                    swingCompatMode = false,
                ) {
                    DecoratedWindow(
                        title = Const.app,
                        onCloseRequest = closeApp,
                        state = rememberWindowState(
                            width = 1280.dp,
                            height = 820.dp,
                        ),
                    ) {
                        JewelWindowMenuBar()
                        JewelDecoratedTitleBar(
                            themeMode = state.themeMode,
                            projectName = projectName,
                            branchName = "dev",
                            onOpenProject = { viewModel.onRightPanelClick(RightToolPanel.START) },
                            onOpenSearch = { viewModel.onRightPanelClick(RightToolPanel.SEARCH) },
                            onRun = {
                                RuntimeFacades.build().startBuild()
                                viewModel.onRightPanelClick(RightToolPanel.START)
                            },
                            onToggleTheme = {
                                if (state.themeMode == JewelThemeMode.ISLAND_DARK) {
                                    RuntimeFacades.tooling().useThemeDefault()
                                    viewModel.setThemeMode(JewelThemeMode.ISLAND_LIGHT)
                                } else {
                                    RuntimeFacades.tooling().useThemeDark()
                                    viewModel.setThemeMode(JewelThemeMode.ISLAND_DARK)
                                }
                            },
                            onOpenNotifications = { RuntimeFacades.tooling().openChangelog() },
                        )
                        JewelMainView(
                            viewModel = viewModel,
                            stateFlow = viewModel.state,
                            onRightPanelClick = { panel -> viewModel.onRightPanelClick(panel) },
                            buildService = buildService,
                            projectTreeService = projectTreeService,
                            editorService = editorService,
                            searchService = searchService,
                            callImplService = callImplService,
                            webService = webService,
                            noteService = noteService,
                            scaService = scaService,
                            leakService = leakService,
                            gadgetService = gadgetService,
                            chainsService = chainsService,
                            advanceService = advanceService,
                            mcpService = mcpService,
                            embeddedTopBar = false,
                        )
                    }
                }
            } else {
                IntUiTheme(
                    theme = themeDefinition,
                    styling = ComponentStyling.default(),
                    swingCompatMode = false,
                ) {
                    Window(
                        title = Const.app,
                        onCloseRequest = closeApp,
                        state = rememberWindowState(
                            width = 1280.dp,
                            height = 820.dp,
                        ),
                    ) {
                        JewelWindowMenuBar()
                        JewelMainView(
                            viewModel = viewModel,
                            stateFlow = viewModel.state,
                            onRightPanelClick = { panel -> viewModel.onRightPanelClick(panel) },
                            buildService = buildService,
                            projectTreeService = projectTreeService,
                            editorService = editorService,
                            searchService = searchService,
                            callImplService = callImplService,
                            webService = webService,
                            noteService = noteService,
                            scaService = scaService,
                            leakService = leakService,
                            gadgetService = gadgetService,
                            chainsService = chainsService,
                            advanceService = advanceService,
                            mcpService = mcpService,
                            embeddedTopBar = true,
                        )
                    }
                }
            }
            ToolingWindowsHost()
        }
        logger.info("launch gui with jewel workbench")
    }
}
