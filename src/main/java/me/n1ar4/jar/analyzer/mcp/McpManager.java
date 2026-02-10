/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.mcp;

import me.n1ar4.jar.analyzer.mcp.backend.JarAnalyzerApiInvoker;
import me.n1ar4.jar.analyzer.mcp.report.ReportHub;
import me.n1ar4.jar.analyzer.mcp.report.ReportMcpTools;
import me.n1ar4.jar.analyzer.mcp.report.ReportWebServer;
import me.n1ar4.jar.analyzer.mcp.transport.McpHttpServer;
import me.n1ar4.jar.analyzer.server.ServerConfig;
import me.n1ar4.jar.analyzer.utils.SocketUtil;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Lifecycle manager for embedded MCP servers (in-process).
 * <p>
 * Keep ASM + SQLite; no external processes.
 */
public final class McpManager {
    private static final Logger logger = LogManager.getLogger();

    private static final String MCP_VERSION = "1.2.1";
    private static final McpManager INSTANCE = new McpManager();

    private final Object lock = new Object();
    private final EnumMap<McpLine, McpHttpServer> servers = new EnumMap<>(McpLine.class);

    private final ReportHub reportHub = new ReportHub();
    private ReportWebServer reportWebServer;

    private McpManager() {
    }

    public static McpManager get() {
        return INSTANCE;
    }

    public boolean isRunning(McpLine line) {
        if (line == null) {
            return false;
        }
        synchronized (lock) {
            McpHttpServer s = servers.get(line);
            return s != null && s.isStarted();
        }
    }

    public boolean isReportWebRunning() {
        synchronized (lock) {
            return reportWebServer != null && reportWebServer.isStarted();
        }
    }

    public void stopAll() {
        synchronized (lock) {
            for (McpHttpServer s : servers.values()) {
                try {
                    if (s != null) {
                        s.stop();
                    }
                } catch (Exception ignored) {
                }
            }
            servers.clear();
            if (reportWebServer != null) {
                try {
                    reportWebServer.stopServer();
                } catch (Exception ignored) {
                } finally {
                    reportWebServer = null;
                }
            }
        }
    }

    public List<String> restartAll(Map<McpLine, McpServiceConfig> config,
                                   McpReportWebConfig reportWebConfig,
                                   ServerConfig apiConfig) {
        stopAll();
        return startEnabled(config, reportWebConfig, apiConfig);
    }

    public List<String> startEnabled(Map<McpLine, McpServiceConfig> config,
                                     McpReportWebConfig reportWebConfig,
                                     ServerConfig apiConfig) {
        List<String> errors = new ArrayList<>();
        if (config != null) {
            for (Map.Entry<McpLine, McpServiceConfig> e : config.entrySet()) {
                if (e == null || e.getKey() == null || e.getValue() == null) {
                    continue;
                }
                McpLine line = e.getKey();
                McpServiceConfig cfg = e.getValue();
                if (!cfg.isEnabled()) {
                    continue;
                }
                String err = startLine(line, cfg, apiConfig);
                if (err != null && !err.isEmpty()) {
                    errors.add(err);
                }
            }
        }
        if (reportWebConfig != null && reportWebConfig.isEnabled()) {
            String err = startReportWeb(reportWebConfig);
            if (err != null && !err.isEmpty()) {
                errors.add(err);
            }
        }
        return errors;
    }

    public String startLine(McpLine line, McpServiceConfig cfg, ServerConfig apiConfig) {
        if (line == null || cfg == null) {
            return "mcp start failed: invalid config";
        }
        synchronized (lock) {
            McpHttpServer existing = servers.get(line);
            if (existing != null) {
                try {
                    existing.stop();
                } catch (Exception ignored) {
                }
                servers.remove(line);
            }

            int port = cfg.getPort();
            if (port < 1 || port > 65535) {
                return "mcp start failed: invalid port for " + line.getId() + ": " + port;
            }
            if (SocketUtil.isPortInUse("localhost", port)) {
                return "mcp start failed: port " + port + " is in use (" + line.getId() + ")";
            }

            McpDispatcher dispatcher = buildDispatcher(line, cfg, apiConfig);
            McpHttpServer http = new McpHttpServer(cfg.getBind(), port, dispatcher);
            try {
                http.start();
            } catch (IOException ex) {
                return "mcp start failed: " + line.getId() + ": " + ex.getMessage();
            }
            servers.put(line, http);
            return null;
        }
    }

    public void stopLine(McpLine line) {
        if (line == null) {
            return;
        }
        synchronized (lock) {
            McpHttpServer s = servers.remove(line);
            if (s != null) {
                s.stop();
            }
        }
    }

    public String startReportWeb(McpReportWebConfig cfg) {
        if (cfg == null) {
            return "report web start failed: invalid config";
        }
        synchronized (lock) {
            if (reportWebServer != null) {
                try {
                    reportWebServer.stopServer();
                } catch (Exception ignored) {
                }
                reportWebServer = null;
            }

            int port = cfg.getPort();
            if (port < 1 || port > 65535) {
                return "report web start failed: invalid port: " + port;
            }
            if (SocketUtil.isPortInUse("localhost", port)) {
                return "report web start failed: port " + port + " is in use";
            }

            ReportWebServer server = new ReportWebServer(cfg.getHost(), port, reportHub);
            server.startServer();
            if (!server.isStarted()) {
                return "report web start failed: unable to bind " + cfg.getHost() + ":" + port;
            }
            reportWebServer = server;
            return null;
        }
    }

    public void stopReportWeb() {
        synchronized (lock) {
            if (reportWebServer != null) {
                reportWebServer.stopServer();
                reportWebServer = null;
            }
        }
    }

    private static McpDispatcher buildDispatcher(McpLine line, McpServiceConfig cfg, ServerConfig apiConfig) {
        String name = switch (line) {
            case AUDIT_FAST -> "jar-analyzer-mcp-audit-fast";
            case GRAPH_LITE -> "jar-analyzer-mcp-graph-lite";
            case DFS_TAINT -> "jar-analyzer-mcp-dfs";
            case SCA_LEAK -> "jar-analyzer-mcp-sca-leak";
            case VUL_RULES -> "jar-analyzer-mcp-vul-rules";
            case REPORT -> "jar-analyzer-report-mcp";
        };

        McpToolRegistry registry = new McpToolRegistry();
        if (line == McpLine.REPORT) {
            // No /api proxy; report tool writes to DB + pushes to websocket clients.
            ReportMcpTools.register(registry, McpManager.get().reportHub);
        } else {
            JarAnalyzerApiInvoker invoker = new JarAnalyzerApiInvoker(apiConfig);
            switch (line) {
                case AUDIT_FAST:
                    JarAnalyzerMcpTools.registerAuditFast(registry, invoker);
                    break;
                case GRAPH_LITE:
                    JarAnalyzerMcpTools.registerGraphLite(registry, invoker);
                    break;
                case DFS_TAINT:
                    JarAnalyzerMcpTools.registerDfsTaint(registry, invoker);
                    break;
                case SCA_LEAK:
                    JarAnalyzerMcpTools.registerScaLeak(registry, invoker);
                    break;
                case VUL_RULES:
                    JarAnalyzerMcpTools.registerVulRules(registry, invoker);
                    break;
                default:
                    JarAnalyzerMcpTools.registerAll(registry, invoker);
                    break;
            }
        }

        return new McpDispatcher(
                name,
                MCP_VERSION,
                registry,
                cfg.isAuth(),
                cfg.getToken()
        );
    }
}
