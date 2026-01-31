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

import org.objectweb.asm.Type;

import java.util.List;

public final class TypeRef {
    public final String internalName;
    public final Type asmType;
    public final List<String> typeArguments;

    private TypeRef(String internalName, Type asmType, List<String> typeArguments) {
        this.internalName = internalName;
        this.asmType = asmType;
        this.typeArguments = typeArguments;
    }

    public static TypeRef fromInternalName(String internalName) {
        if (internalName == null || internalName.trim().isEmpty()) {
            return null;
        }
        return new TypeRef(internalName, Type.getObjectType(internalName), null);
    }

    public static TypeRef fromInternalName(String internalName, List<String> typeArguments) {
        if (internalName == null || internalName.trim().isEmpty()) {
            return null;
        }
        return new TypeRef(internalName, Type.getObjectType(internalName), typeArguments);
    }

    public static TypeRef fromAsmType(Type type) {
        if (type == null) {
            return null;
        }
        if (type.getSort() == Type.OBJECT) {
            return new TypeRef(type.getInternalName(), type, null);
        }
        return new TypeRef(null, type, null);
    }

    public String getInternalName() {
        return internalName;
    }

    public Type getAsmType() {
        return asmType;
    }

    public List<String> getTypeArguments() {
        return typeArguments;
    }

    public boolean isPrimitive() {
        return asmType != null && asmType.getSort() >= Type.BOOLEAN && asmType.getSort() <= Type.DOUBLE;
    }

    public boolean isArray() {
        return asmType != null && asmType.getSort() == Type.ARRAY;
    }
}
