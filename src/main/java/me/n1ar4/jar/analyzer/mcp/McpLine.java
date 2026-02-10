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

public enum McpLine {
    AUDIT_FAST("audit-fast", 20033),
    GRAPH_LITE("graph-lite", 20034),
    DFS_TAINT("dfs-taint", 20035),
    SCA_LEAK("sca-leak", 20036),
    VUL_RULES("vul-rules", 20037),
    REPORT("report", 20081);

    private final String id;
    private final int defaultPort;

    McpLine(String id, int defaultPort) {
        this.id = id;
        this.defaultPort = defaultPort;
    }

    public String getId() {
        return id;
    }

    public int getDefaultPort() {
        return defaultPort;
    }
}

