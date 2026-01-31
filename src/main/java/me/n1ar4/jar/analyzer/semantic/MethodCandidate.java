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

import java.util.List;

public final class MethodCandidate {
    private final String className;
    private final String methodName;
    private final String methodDesc;
    private final boolean isStatic;
    private final boolean isVarargs;
    private final List<TypeRef> paramTypes;

    public MethodCandidate(String className,
                           String methodName,
                           String methodDesc,
                           boolean isStatic,
                           boolean isVarargs,
                           List<TypeRef> paramTypes) {
        this.className = className;
        this.methodName = methodName;
        this.methodDesc = methodDesc;
        this.isStatic = isStatic;
        this.isVarargs = isVarargs;
        this.paramTypes = paramTypes;
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getMethodDesc() {
        return methodDesc;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public boolean isVarargs() {
        return isVarargs;
    }

    public List<TypeRef> getParamTypes() {
        return paramTypes;
    }
}
