package org.benf.cfr.reader.bytecode.analysis.parse.expression;

import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;

final class DisplayTypeResolution {
    private final JavaTypeInstance resolvedType;
    private final boolean requiresExplicitCast;
    private final String detail;

    DisplayTypeResolution(JavaTypeInstance resolvedType, boolean requiresExplicitCast, String detail) {
        this.resolvedType = resolvedType;
        this.requiresExplicitCast = requiresExplicitCast;
        this.detail = detail;
    }

    JavaTypeInstance getResolvedType() {
        return resolvedType;
    }

    boolean requiresExplicitCast() {
        return requiresExplicitCast;
    }

    String getDetail() {
        return detail;
    }
}
