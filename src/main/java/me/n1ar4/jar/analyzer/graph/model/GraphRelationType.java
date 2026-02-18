/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.graph.model;

public enum GraphRelationType {
    CALLS_DIRECT,
    CALLS_DISPATCH,
    CALLS_REFLECTION,
    CALLS_CALLBACK,
    CALLS_OVERRIDE,
    CONTAINS_CALLSITE,
    CALLSITE_TO_CALLEE,
    NEXT_CALLSITE;

    public static GraphRelationType fromEdgeType(String edgeType) {
        if (edgeType == null) {
            return CALLS_DIRECT;
        }
        String type = edgeType.trim().toLowerCase();
        return switch (type) {
            case "dispatch", "virtual", "interface" -> CALLS_DISPATCH;
            case "reflection", "reflect" -> CALLS_REFLECTION;
            case "callback", "lambda" -> CALLS_CALLBACK;
            case "override", "inheritance" -> CALLS_OVERRIDE;
            default -> CALLS_DIRECT;
        };
    }
}
