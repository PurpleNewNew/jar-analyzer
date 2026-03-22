package org.benf.cfr.reader.bytecode.analysis.parse.statement;

import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.CastExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ExpressionTypeHintHelper;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.TernaryExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.CloneHelper;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.*;
import org.benf.cfr.reader.bytecode.analysis.types.JavaGenericRefTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredReturn;
import org.benf.cfr.reader.bytecode.analysis.types.JavaGenericPlaceholderTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.entities.exceptions.ExceptionCheck;
import org.benf.cfr.reader.util.output.Dumper;

public class ReturnValueStatement extends ReturnStatement {
    private Expression rvalue;
    private final JavaTypeInstance fnReturnType;

    public ReturnValueStatement(BytecodeLoc loc, Expression rvalue, JavaTypeInstance fnReturnType) {
        super(loc);
        this.rvalue = rvalue;
        if (fnReturnType instanceof JavaGenericPlaceholderTypeInstance) {
            this.rvalue = new CastExpression(BytecodeLoc.NONE, new InferredJavaType(fnReturnType, InferredJavaType.Source.FUNCTION, true), this.rvalue);
        }
        this.fnReturnType = fnReturnType;
    }

    @Override
    public ReturnStatement deepClone(CloneHelper cloneHelper) {
        return new ReturnValueStatement(getLoc(), cloneHelper.replaceOrClone(rvalue), fnReturnType);
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return BytecodeLoc.combine(this, rvalue);
    }

    @Override
    public Dumper dump(Dumper dumper) {
        return dumper.keyword("return ").dump(rvalue).endCodeln();
    }

    public Expression getReturnValue() {
        return rvalue;
    }

    public JavaTypeInstance getFnReturnType() {
        return fnReturnType;
    }

    @Override
    public void replaceSingleUsageLValues(LValueRewriter lValueRewriter, SSAIdentifiers ssaIdentifiers) {
        this.rvalue = rvalue.replaceSingleUsageLValues(lValueRewriter, ssaIdentifiers, getContainer());
    }

    @Override
    public void rewriteExpressions(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers) {
        this.rvalue = expressionRewriter.rewriteExpression(rvalue, ssaIdentifiers, getContainer(), ExpressionRewriterFlags.RVALUE);
    }

    @Override
    public void collectLValueUsage(LValueUsageCollector lValueUsageCollector) {
        rvalue.collectUsedLValues(lValueUsageCollector);
    }

    @Override
    public StructuredStatement getStructuredStatement() {
        /*
         * Create explicit cast - a bit late in the day - but we don't always know about int types
         * until now.
         */
        Expression rvalueUse = rvalue;
        if (rvalue instanceof TernaryExpression
                && canUseTargetTypedTernaryWithoutExplicitCast((TernaryExpression) rvalue, fnReturnType)) {
            return new StructuredReturn(getLoc(), rvalue, fnReturnType);
        }
        if (requiresExplicitReturnCast(rvalue.getInferredJavaType().getJavaTypeInstance(), fnReturnType)) {
            InferredJavaType inferredJavaType = new InferredJavaType(fnReturnType, InferredJavaType.Source.FUNCTION, true);
            rvalueUse = new CastExpression(BytecodeLoc.NONE, inferredJavaType, rvalue, true);
        }
        return new StructuredReturn(getLoc(), rvalueUse, fnReturnType);
    }

    private static boolean canUseTargetTypedTernaryWithoutExplicitCast(TernaryExpression ternaryExpression,
                                                                       JavaTypeInstance targetType) {
        if (ternaryExpression == null || targetType == null) {
            return false;
        }
        JavaTypeInstance targetBaseType = targetType.getDeGenerifiedType();
        if (targetBaseType == null) {
            return false;
        }
        ternaryExpression.applyTargetTypeConstraint(targetType);
        return ternaryBranchCanUseTargetType(ternaryExpression.getLhs(), targetType, targetBaseType)
                && ternaryBranchCanUseTargetType(ternaryExpression.getRhs(), targetType, targetBaseType);
    }

    private static boolean ternaryBranchCanUseTargetType(Expression expression,
                                                         JavaTypeInstance targetType,
                                                         JavaTypeInstance targetBaseType) {
        if (expression == null || targetType == null || targetBaseType == null) {
            return false;
        }
        JavaTypeInstance expressionType = expression.getInferredJavaType().getJavaTypeInstance();
        if (expressionType != null && expressionType.implicitlyCastsTo(targetType, null)) {
            return true;
        }
        JavaTypeInstance expressionBaseType = expressionType == null ? null : expressionType.getDeGenerifiedType();
        if (targetBaseType.equals(expressionBaseType)) {
            return true;
        }
        return ExpressionTypeHintHelper.applyExpectedCastIfNeeded(expression, targetType) == expression;
    }

    private static boolean requiresExplicitReturnCast(JavaTypeInstance valueType, JavaTypeInstance targetType) {
        if (!valueType.implicitlyCastsTo(targetType, null)) {
            return true;
        }
        if (!(targetType instanceof JavaGenericRefTypeInstance)) {
            return false;
        }
        if (!valueType.getDeGenerifiedType().equals(targetType.getDeGenerifiedType())) {
            return false;
        }
        return !(valueType instanceof JavaGenericRefTypeInstance);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) return false;
        if (o == this) return true;
        if (!(o instanceof ReturnValueStatement)) return false;

        ReturnValueStatement other = (ReturnValueStatement) o;
        return rvalue.equals(other.rvalue);
    }

    @Override
    public final boolean equivalentUnder(Object o, EquivalenceConstraint constraint) {
        if (o == null) return false;
        if (o == this) return true;
        if (getClass() != o.getClass()) return false;
        ReturnValueStatement other = (ReturnValueStatement) o;
        if (!constraint.equivalent(rvalue, other.rvalue)) return false;
        return true;
    }

    @Override
    public boolean canThrow(ExceptionCheck caught) {
        return rvalue.canThrow(caught);
    }
}
