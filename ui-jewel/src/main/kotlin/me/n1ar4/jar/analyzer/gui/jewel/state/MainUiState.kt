package me.n1ar4.jar.analyzer.gui.jewel.state

data class McpStatus(
    val auditFast: Boolean = false,
    val graphLite: Boolean = false,
    val dfsTaint: Boolean = false,
    val scaLeak: Boolean = false,
    val vulRules: Boolean = false,
    val report: Boolean = false,
    val reportWeb: Boolean = false,
) {
    fun runningCount(): Int {
        return listOf(auditFast, graphLite, dfsTaint, scaLeak, vulRules, report, reportWeb).count { it }
    }
}

data class MainUiState(
    val selectedTab: CoreTab = CoreTab.START,
    val activeRightPanel: RightToolPanel = RightToolPanel.START,
    val rightPanelExpanded: Boolean = true,
    val themeMode: JewelThemeMode = JewelThemeMode.ISLAND_LIGHT,
    val engineStatus: String = "CLOSED",
    val mcpStatus: McpStatus = McpStatus(),
    val statusText: String = "ready",
)
