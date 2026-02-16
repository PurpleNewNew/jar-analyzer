package me.n1ar4.jar.analyzer.gui.jewel.state

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.n1ar4.jar.analyzer.gui.jewel.service.BuildService
import me.n1ar4.jar.analyzer.gui.jewel.service.ChainsService
import me.n1ar4.jar.analyzer.gui.jewel.service.McpService
import me.n1ar4.jar.analyzer.gui.jewel.service.SearchService
import me.n1ar4.jar.analyzer.gui.runtime.api.RuntimeFacades

class MainViewModel(
    private val buildService: BuildService,
    private val searchService: SearchService,
    private val chainsService: ChainsService,
    private val mcpService: McpService,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _state = MutableStateFlow(MainUiState())
    val state: StateFlow<MainUiState> = _state.asStateFlow()

    fun selectTab(tab: CoreTab) {
        _state.update {
            it.copy(
                selectedTab = tab,
                activeRightPanel = RightToolPanel.fromCoreTab(tab),
                rightPanelExpanded = true,
                statusText = "tab: ${tab.title}",
            )
        }
    }

    fun syncFromLegacy(index: Int) {
        val tab = CoreTab.fromIndex(index)
        _state.update {
            it.copy(
                selectedTab = tab,
                activeRightPanel = RightToolPanel.fromCoreTab(tab),
            )
        }
    }

    fun onRightPanelClick(panel: RightToolPanel) {
        _state.update { current ->
            if (current.activeRightPanel == panel) {
                current.copy(
                    rightPanelExpanded = true,
                    statusText = "panel: ${panel.title}",
                )
            } else {
                val nextTab = panel.coreTab ?: current.selectedTab
                current.copy(
                    selectedTab = nextTab,
                    activeRightPanel = panel,
                    rightPanelExpanded = true,
                    statusText = "panel: ${panel.title}",
                )
            }
        }
    }

    fun setThemeMode(mode: JewelThemeMode) {
        _state.update { it.copy(themeMode = mode) }
    }

    fun refreshRuntimeState() {
        scope.launch {
            val engineStatus = buildService.engineStatus()
            val mcpStatus = mcpService.status()
            val themeMode = JewelThemeMode.fromRuntimeTheme(RuntimeFacades.tooling().configSnapshot().theme())
            val status = buildString {
                append("engine:")
                append(engineStatus)
                append(" / mcp:")
                append(mcpStatus.runningCount())
                append("/7")
                if (searchService.supportsJumpNavigation()) {
                    append(" / search:jump")
                }
                if (chainsService.supportsTaintValidation()) {
                    append(" / chains:taint")
                }
            }
            _state.update {
                it.copy(
                    engineStatus = engineStatus,
                    mcpStatus = mcpStatus,
                    themeMode = themeMode,
                    statusText = status,
                )
            }
        }
    }

    fun startMcpConfigured() {
        scope.launch {
            val errors = mcpService.startConfigured()
            _state.update {
                if (errors.isEmpty()) {
                    it.copy(statusText = "mcp started")
                } else {
                    it.copy(statusText = errors.joinToString(" | "))
                }
            }
            refreshRuntimeState()
        }
    }

    fun stopAllMcp() {
        scope.launch {
            mcpService.stopAll()
            _state.update { it.copy(statusText = "mcp stopped") }
            refreshRuntimeState()
        }
    }

    fun dispose() {
        scope.coroutineContext.cancel()
    }
}
