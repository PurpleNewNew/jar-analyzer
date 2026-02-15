package me.n1ar4.jar.analyzer.gui.runtime.api;

import me.n1ar4.jar.analyzer.gui.runtime.model.ApiInfoDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.McpConfigDto;

import java.util.List;

public interface ApiMcpFacade {
    ApiInfoDto apiInfo();

    McpConfigDto currentConfig();

    List<String> applyAndRestart(McpConfigDto config);

    List<String> startConfigured();

    void stopAll();

    void openApiDoc();

    void openMcpDoc();

    void openN8nDoc();

    void openReportWeb(String host, int port);
}
