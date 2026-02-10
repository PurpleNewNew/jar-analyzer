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

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Transport-level request context for a tool call.
 */
public final class McpToolCallContext {
    private final Map<String, List<String>> headers;

    public McpToolCallContext(Map<String, List<String>> headers) {
        this.headers = headers == null ? Collections.emptyMap() : headers;
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    public String getFirstHeader(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        List<String> values = headers.get(name);
        if (values != null && !values.isEmpty()) {
            return values.get(0);
        }
        // Common clients sometimes vary header casing; best-effort.
        values = headers.get(name.toLowerCase());
        if (values != null && !values.isEmpty()) {
            return values.get(0);
        }
        values = headers.get(name.toUpperCase());
        if (values != null && !values.isEmpty()) {
            return values.get(0);
        }
        return null;
    }
}

