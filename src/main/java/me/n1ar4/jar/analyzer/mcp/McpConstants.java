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

import java.util.Set;

/**
 * Minimal MCP constants aligned with the Go mcp-go server used previously.
 */
public final class McpConstants {
    private McpConstants() {
    }

    public static final String JSONRPC_VERSION = "2.0";

    // Protocol versions (match mcp-go ValidProtocolVersions).
    public static final String PROTOCOL_LATEST = "2025-06-18";
    public static final String PROTOCOL_2025_03_26 = "2025-03-26";
    public static final String PROTOCOL_2024_11_05 = "2024-11-05";

    public static final Set<String> VALID_PROTOCOL_VERSIONS = Set.of(
            PROTOCOL_LATEST,
            PROTOCOL_2025_03_26,
            PROTOCOL_2024_11_05
    );

    // JSON-RPC error codes.
    public static final int PARSE_ERROR = -32700;
    public static final int INVALID_REQUEST = -32600;
    public static final int METHOD_NOT_FOUND = -32601;
    public static final int INVALID_PARAMS = -32602;
    public static final int INTERNAL_ERROR = -32603;

    // HTTP header keys used by streamable-http transport.
    public static final String HEADER_SESSION_ID = "Mcp-Session-Id";
}

