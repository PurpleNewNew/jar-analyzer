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

import java.util.Objects;

public final class FlowPort {
    public enum Kind {
        THIS,
        PARAM,
        FIELD,
        STATIC_FIELD,
        RETURN
    }

    private final Kind kind;
    private final int index;
    private final String fieldOwner;
    private final String fieldName;
    private final String fieldDesc;

    private FlowPort(Kind kind, int index, String fieldOwner, String fieldName, String fieldDesc) {
        this.kind = kind;
        this.index = index;
        this.fieldOwner = fieldOwner;
        this.fieldName = fieldName;
        this.fieldDesc = fieldDesc;
    }

    public static FlowPort thisPort() {
        return new FlowPort(Kind.THIS, -1, null, null, null);
    }

    public static FlowPort param(int index) {
        return new FlowPort(Kind.PARAM, index, null, null, null);
    }

    public static FlowPort ret() {
        return new FlowPort(Kind.RETURN, -1, null, null, null);
    }

    public static FlowPort field(String owner, String name, String desc, boolean isStatic) {
        return new FlowPort(isStatic ? Kind.STATIC_FIELD : Kind.FIELD, -1, owner, name, desc);
    }

    public Kind getKind() {
        return kind;
    }

    public int getIndex() {
        return index;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FlowPort)) {
            return false;
        }
        FlowPort that = (FlowPort) o;
        return index == that.index
                && kind == that.kind
                && Objects.equals(fieldOwner, that.fieldOwner)
                && Objects.equals(fieldName, that.fieldName)
                && Objects.equals(fieldDesc, that.fieldDesc);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, index, fieldOwner, fieldName, fieldDesc);
    }
}
