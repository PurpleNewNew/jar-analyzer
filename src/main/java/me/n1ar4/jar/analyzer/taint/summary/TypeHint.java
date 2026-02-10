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

import me.n1ar4.jar.analyzer.engine.HierarchyService;

import java.util.Set;

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
        Set<String> aSupers = HierarchyService.getSuperTypes(a);
        Set<String> bSupers = HierarchyService.getSuperTypes(b);
        // Be permissive if hierarchy information is unavailable.
        if ((aSupers == null || aSupers.isEmpty()) && (bSupers == null || bSupers.isEmpty())) {
            return true;
        }
        return (aSupers != null && aSupers.contains(b)) || (bSupers != null && bSupers.contains(a));
    }

    private static String normalize(String type) {
        String v = type.trim();
        if (v.startsWith("L") && v.endsWith(";")) {
            v = v.substring(1, v.length() - 1);
        }
        return v.replace('.', '/');
    }
}
