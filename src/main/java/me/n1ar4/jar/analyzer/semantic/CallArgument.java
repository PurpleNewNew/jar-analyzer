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

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.MethodReferenceExpr;

final class CallArgument {
    enum Kind {
        NORMAL,
        LAMBDA,
        METHOD_REF,
        UNKNOWN
    }

    final Expression expression;
    final TypeRef type;
    final Kind kind;
    final int lambdaParamCount;

    private CallArgument(Expression expression, TypeRef type, Kind kind, int lambdaParamCount) {
        this.expression = expression;
        this.type = type;
        this.kind = kind;
        this.lambdaParamCount = lambdaParamCount;
    }

    static CallArgument from(Expression expr, TypeRef type) {
        if (expr == null) {
            return new CallArgument(null, type, Kind.UNKNOWN, -1);
        }
        if (expr instanceof LambdaExpr) {
            LambdaExpr lambda = (LambdaExpr) expr;
            int count = lambda.getParameters() == null ? -1 : lambda.getParameters().size();
            return new CallArgument(expr, type, Kind.LAMBDA, count);
        }
        if (expr instanceof MethodReferenceExpr) {
            return new CallArgument(expr, type, Kind.METHOD_REF, -1);
        }
        return new CallArgument(expr, type, Kind.NORMAL, -1);
    }
}
