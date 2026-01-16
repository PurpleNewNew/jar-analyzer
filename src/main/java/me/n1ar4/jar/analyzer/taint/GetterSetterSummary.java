/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */
package me.n1ar4.jar.analyzer.taint;

public class GetterSetterSummary {
    public enum Kind {
        GETTER,
        SETTER
    }

    private final Kind kind;
    private final String fieldOwner;
    private final String fieldName;
    private final String fieldDesc;
    private final boolean isStatic;
    private final int paramIndex;

    public GetterSetterSummary(Kind kind,
                               String fieldOwner,
                               String fieldName,
                               String fieldDesc,
                               boolean isStatic,
                               int paramIndex) {
        this.kind = kind;
        this.fieldOwner = fieldOwner;
        this.fieldName = fieldName;
        this.fieldDesc = fieldDesc;
        this.isStatic = isStatic;
        this.paramIndex = paramIndex;
    }

    public Kind getKind() {
        return kind;
    }

    public boolean isGetter() {
        return kind == Kind.GETTER;
    }

    public boolean isSetter() {
        return kind == Kind.SETTER;
    }

    public String getFieldOwner() {
        return fieldOwner;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getFieldDesc() {
        return fieldDesc;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public int getParamIndex() {
        return paramIndex;
    }
}
