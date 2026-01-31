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

import com.github.javaparser.ast.expr.MethodCallExpr;

public final class CallPosition {
    public final MethodCallExpr call;
    public final int callIndex;
    public final boolean inLambda;
    public final int line;

    public CallPosition(MethodCallExpr call, int callIndex, boolean inLambda, int line) {
        this.call = call;
        this.callIndex = callIndex;
        this.inLambda = inLambda;
        this.line = line;
    }
}
