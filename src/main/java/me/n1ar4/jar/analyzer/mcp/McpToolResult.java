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

public final class McpToolResult {
    private final String text;
    private final boolean isError;

    private McpToolResult(String text, boolean isError) {
        this.text = text == null ? "" : text;
        this.isError = isError;
    }

    public static McpToolResult ok(String text) {
        return new McpToolResult(text, false);
    }

    public static McpToolResult error(String text) {
        return new McpToolResult(text, true);
    }

    public String getText() {
        return text;
    }

    public boolean isError() {
        return isError;
    }
}

