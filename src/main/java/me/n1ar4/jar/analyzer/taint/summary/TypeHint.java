/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */
package me.n1ar4.jar.analyzer.taint.summary;

import me.n1ar4.jar.analyzer.core.InheritanceMap;
import me.n1ar4.jar.analyzer.core.AnalyzeEnv;

public final class TypeHint {
    public enum Kind {
        UNKNOWN,
        EXACT,
        UPPER_BOUND
    }

    private final Kind kind;
    private final String type;

    private TypeHint(Kind kind, String type) {
        this.kind = kind;
        this.type = type;
    }

    public static TypeHint unknown() {
        return new TypeHint(Kind.UNKNOWN, null);
    }

    public static TypeHint exact(String type) {
        if (type == null || type.trim().isEmpty()) {
            return unknown();
        }
        return new TypeHint(Kind.EXACT, normalize(type));
    }

    public static TypeHint upperBound(String type) {
        if (type == null || type.trim().isEmpty()) {
            return unknown();
        }
        return new TypeHint(Kind.UPPER_BOUND, normalize(type));
    }

    public Kind getKind() {
        return kind;
    }

    public String getType() {
        return type;
    }

    public boolean isCompatible(TypeHint other) {
        if (other == null || other.kind == Kind.UNKNOWN || this.kind == Kind.UNKNOWN) {
            return true;
        }
        String a = this.type;
        String b = other.type;
        if (a == null || b == null) {
            return true;
        }
        if (a.equals(b)) {
            return true;
        }
        InheritanceMap map = AnalyzeEnv.inheritanceMap;
        if (map == null) {
            return true;
        }
        return map.isSubclassOf(new me.n1ar4.jar.analyzer.core.reference.ClassReference.Handle(a),
                new me.n1ar4.jar.analyzer.core.reference.ClassReference.Handle(b))
                || map.isSubclassOf(new me.n1ar4.jar.analyzer.core.reference.ClassReference.Handle(b),
                new me.n1ar4.jar.analyzer.core.reference.ClassReference.Handle(a));
    }

    private static String normalize(String type) {
        String v = type.trim();
        if (v.startsWith("L") && v.endsWith(";")) {
            v = v.substring(1, v.length() - 1);
        }
        return v.replace('.', '/');
    }
}
