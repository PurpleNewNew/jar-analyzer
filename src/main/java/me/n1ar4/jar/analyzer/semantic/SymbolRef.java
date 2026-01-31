/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.semantic;

import com.github.javaparser.Range;

public final class SymbolRef {
    private final SymbolKind kind;
    private final String name;
    private final String owner;
    private final Range declarationRange;
    private final TypeRef typeRef;
    private final int argCount;

    public SymbolRef(SymbolKind kind, String name, String owner) {
        this(kind, name, owner, null, null, -1);
    }

    public SymbolRef(SymbolKind kind,
                     String name,
                     String owner,
                     Range declarationRange,
                     TypeRef typeRef) {
        this(kind, name, owner, declarationRange, typeRef, -1);
    }

    public SymbolRef(SymbolKind kind,
                     String name,
                     String owner,
                     Range declarationRange,
                     TypeRef typeRef,
                     int argCount) {
        this.kind = kind == null ? SymbolKind.UNKNOWN : kind;
        this.name = name;
        this.owner = owner;
        this.declarationRange = declarationRange;
        this.typeRef = typeRef;
        this.argCount = argCount;
    }

    public SymbolKind getKind() {
        return kind;
    }

    public String getName() {
        return name;
    }

    public String getOwner() {
        return owner;
    }

    public Range getDeclarationRange() {
        return declarationRange;
    }

    public TypeRef getTypeRef() {
        return typeRef;
    }

    public int getArgCount() {
        return argCount;
    }

    public SymbolRef withArgCount(int newArgCount) {
        return new SymbolRef(kind, name, owner, declarationRange, typeRef, newArgCount);
    }
}
