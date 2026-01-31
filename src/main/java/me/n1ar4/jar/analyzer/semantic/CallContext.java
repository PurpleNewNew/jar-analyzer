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

import com.github.javaparser.Position;
import com.github.javaparser.ast.CompilationUnit;

public final class CallContext {
    public final CompilationUnit cu;
    public final Position position;
    public final int line;
    public final String className;
    public final String methodName;
    public final String methodDesc;
    public final int methodArgCount;

    public CallContext(CompilationUnit cu,
                       Position position,
                       int line,
                       String className,
                       String methodName,
                       String methodDesc,
                       int methodArgCount) {
        this.cu = cu;
        this.position = position;
        this.line = line;
        this.className = className;
        this.methodName = methodName;
        this.methodDesc = methodDesc;
        this.methodArgCount = methodArgCount;
    }
}
