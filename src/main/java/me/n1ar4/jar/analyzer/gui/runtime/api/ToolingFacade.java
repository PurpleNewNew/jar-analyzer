package me.n1ar4.jar.analyzer.gui.runtime.api;

import me.n1ar4.jar.analyzer.gui.runtime.model.ToolingConfigSnapshotDto;

public interface ToolingFacade {
    void openExportTool();

    void openRemoteLoadTool();

    void openProxyTool();

    void openPartitionTool();

    void openGlobalSearchTool();

    void openSystemMonitorTool();

    void openMarkdownViewer(String title, String markdownResource);

    void openCfgTool();

    void openFrameTool(boolean full);

    void openOpcodeTool();

    void openAsmTool();

    void openElSearchTool();

    void openAllStringsTool();

    void openSqlAuditTool();

    void openEncodeTool();

    void openListenerTool();

    void openSerializationTool();

    void openBcelTool();

    void openRepeaterTool();

    void openObfuscationTool();

    void openHtmlGraph();

    void openChainsDfsResult();

    void openChainsTaintResult();

    void openRemoteTomcatAnalyzer();

    void openBytecodeDebugger();

    void openJdGui();

    void openFlappyGame();

    void openPockerGame();

    void openDocs();

    void openReportBug();

    void openProjectSite();

    void openVersionInfo();

    void openChangelog();

    void openThanks();

    void setLanguageChinese();

    void setLanguageEnglish();

    void useThemeDefault();

    void useThemeDark();

    void setStripeShowNames(boolean showNames);

    void setStripeWidth(int width);

    void toggleShowInnerClass();

    void toggleFixClassPath();

    void setSortByMethod();

    void setSortByClass();

    void toggleLogAllSql();

    void toggleGroupTreeByJar();

    void toggleMergePackageRoot();

    void toggleFixMethodImpl();

    void toggleQuickMode();

    ToolingConfigSnapshotDto configSnapshot();
}
