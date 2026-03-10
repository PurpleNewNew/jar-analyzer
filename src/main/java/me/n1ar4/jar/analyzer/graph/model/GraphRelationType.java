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

import java.util.List;

public enum GraphRelationType {
    ALIAS,
    HAS,
    EXTEND,
    INTERFACES,
    CALLS_DIRECT,
    CALLS_DISPATCH,
    CALLS_REFLECTION,
    CALLS_CALLBACK,
    CALLS_OVERRIDE,
    CALLS_INDY,
    CALLS_METHOD_HANDLE,
    CALLS_FRAMEWORK,
    CALLS_PTA;

    private static final List<String> PHYSICAL_CALL_RELATION_TYPES = List.of(
            CALLS_DIRECT.name(),
            CALLS_DISPATCH.name(),
            CALLS_REFLECTION.name(),
            CALLS_CALLBACK.name(),
            CALLS_OVERRIDE.name(),
            CALLS_INDY.name(),
            CALLS_METHOD_HANDLE.name(),
            CALLS_FRAMEWORK.name(),
            CALLS_PTA.name()
    );

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
            case "invoke_dynamic", "indy", "dynamic" -> CALLS_INDY;
            case "method_handle", "methodhandle", "mh" -> CALLS_METHOD_HANDLE;
            case "framework" -> CALLS_FRAMEWORK;
            case "pta" -> CALLS_PTA;
            default -> CALLS_DIRECT;
        };
    }

    public static GraphRelationType tryParse(String relationType) {
        if (relationType == null) {
            return null;
        }
        String normalized = relationType.trim();
        if (normalized.isBlank()) {
            return null;
        }
        try {
            return GraphRelationType.valueOf(normalized.toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public static String relationGroup(String relationType) {
        GraphRelationType parsed = tryParse(relationType);
        if (parsed != null) {
            return parsed.displayGroup();
        }
        String normalized = relationType == null ? "" : relationType.trim();
        if (normalized.isBlank()) {
            return "";
        }
        return normalized.toUpperCase();
    }

    public static String relationSubtype(String relationType) {
        GraphRelationType parsed = tryParse(relationType);
        if (parsed != null) {
            return parsed.displaySubtype();
        }
        String normalized = relationType == null ? "" : relationType.trim();
        if (normalized.isBlank()) {
            return "";
        }
        return normalized.toLowerCase();
    }

    public static List<String> physicalCallRelationTypes() {
        return PHYSICAL_CALL_RELATION_TYPES;
    }

    public static List<String> expandLogicalRelationType(String relationType) {
        String normalized = relationType == null ? "" : relationType.trim().toUpperCase();
        return switch (normalized) {
            case "CALL" -> PHYSICAL_CALL_RELATION_TYPES;
            case "ALIAS", "HAS", "EXTEND", "INTERFACES" -> List.of(normalized);
            default -> normalized.isBlank() ? List.of() : List.of(relationType);
        };
    }

    public String displayGroup() {
        return switch (this) {
            case ALIAS -> "ALIAS";
            case HAS -> "HAS";
            case EXTEND -> "EXTEND";
            case INTERFACES -> "INTERFACES";
            default -> "CALL";
        };
    }

    public String displaySubtype() {
        return switch (this) {
            case ALIAS -> "alias";
            case HAS -> "has";
            case EXTEND -> "extend";
            case INTERFACES -> "interfaces";
            case CALLS_DIRECT -> "direct";
            case CALLS_DISPATCH -> "dispatch";
            case CALLS_REFLECTION -> "reflection";
            case CALLS_CALLBACK -> "callback";
            case CALLS_OVERRIDE -> "override";
            case CALLS_INDY -> "invoke_dynamic";
            case CALLS_METHOD_HANDLE -> "method_handle";
            case CALLS_FRAMEWORK -> "framework";
            case CALLS_PTA -> "pta";
        };
    }
}
