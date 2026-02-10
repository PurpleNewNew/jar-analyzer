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

public final class McpServiceConfig {
    private boolean enabled;
    private String bind = "0.0.0.0";
    private int port;
    private boolean auth;
    private String token = "JAR-ANALYZER-MCP-TOKEN";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBind() {
        return bind;
    }

    public void setBind(String bind) {
        if (bind == null || bind.trim().isEmpty()) {
            return;
        }
        this.bind = bind.trim();
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean isAuth() {
        return auth;
    }

    public void setAuth(boolean auth) {
        this.auth = auth;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        if (token == null) {
            return;
        }
        String v = token.trim();
        if (!v.isEmpty()) {
            this.token = v;
        }
    }
}

