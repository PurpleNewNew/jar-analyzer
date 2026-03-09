/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */
package me.n1ar4.jar.analyzer.rules;

import java.util.LinkedHashMap;
import java.util.Map;

public final class RuleValidationIssue {
    private final String level;
    private final String code;
    private final String message;
    private final int index;
    private final String id;
    private final String kind;
    private final boolean rejected;

    public RuleValidationIssue(String level,
                               String code,
                               String message,
                               int index,
                               String id,
                               String kind,
                               boolean rejected) {
        this.level = safe(level);
        this.code = safe(code);
        this.message = safe(message);
        this.index = index;
        this.id = safe(id);
        this.kind = safe(kind);
        this.rejected = rejected;
    }

    public String getLevel() {
        return level;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public int getIndex() {
        return index;
    }

    public String getId() {
        return id;
    }

    public String getKind() {
        return kind;
    }

    public boolean isRejected() {
        return rejected;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("level", level);
        out.put("code", code);
        out.put("message", message);
        out.put("rejected", rejected);
        if (index > 0) {
            out.put("index", index);
        }
        if (!id.isBlank()) {
            out.put("id", id);
        }
        if (!kind.isBlank()) {
            out.put("kind", kind);
        }
        return out;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
