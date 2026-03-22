package org.benf.cfr.reader.bytecode.analysis.parse.expression;

import org.benf.cfr.reader.bytecode.TypeRecoveryPasses;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.PrimitiveBoxingRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.misc.Precedence;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.rewriteinterface.BoxingProcessor;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.LocalVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.literal.LiteralFolding;
import org.benf.cfr.reader.bytecode.analysis.parse.literal.TypedLiteral;
import org.benf.cfr.reader.bytecode.analysis.parse.literal.TypedLiteral.LiteralType;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.CloneHelper;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.*;
import org.benf.cfr.reader.bytecode.analysis.types.*;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.CastAction;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.util.Troolean;
import org.benf.cfr.reader.util.output.Dumper;

import java.util.Map;

public class CastExpression extends AbstractExpression implements BoxingProcessor {
    private Expression child;
    private boolean forced;

    public CastExpression(BytecodeLoc loc, InferredJavaType knownType, Expression child) {
        super(loc, knownType);
        InferredJavaType childInferredJavaType = child.getInferredJavaType();
        if (knownType.getJavaTypeInstance() == RawJavaType.LONG &&
            childInferredJavaType.getJavaTypeInstance() == RawJavaType.BOOLEAN) {
            childInferredJavaType.forceType(RawJavaType.INT, true);
        }
        // Used to explicit cast boolean -> non bool with a ternary here, but we could have messed up enough that's not valid!
        // See BitwiseNarrowingConversionTest3.
        this.child = child;
        this.forced = false;
    }

    public CastExpression(BytecodeLoc loc, InferredJavaType knownType, Expression child, boolean forced) {
        super(loc, knownType);
        this.child = child;
        this.forced = forced;
    }

    public boolean isForced() {
        return forced;
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return BytecodeLoc.combine(this, child);
    }

    @Override
    public Expression deepClone(CloneHelper cloneHelper) {
        return new CastExpression(getLoc(), getInferredJavaType(), cloneHelper.replaceOrClone(child), forced);
    }

	@Override
	public Literal getComputedLiteral(Map<LValue, Literal> display) {
		if (!(getInferredJavaType().getJavaTypeInstance() instanceof RawJavaType))
			return null;
		Literal computedChild = child.getComputedLiteral(display);
		if (computedChild == null)
			return null;
		return LiteralFolding.foldCast(computedChild, (RawJavaType) getInferredJavaType().getJavaTypeInstance());
	}

    public boolean couldBeImplicit(GenericTypeBinder gtb) {
        if (forced) return false;
        JavaTypeInstance childType = child.getInferredJavaType().getJavaTypeInstance();
        JavaTypeInstance tgtType = getInferredJavaType().getJavaTypeInstance();
        return childType.implicitlyCastsTo(tgtType, gtb);
    }

    private boolean couldBeImplicit(JavaTypeInstance tgtType, GenericTypeBinder gtb) {
        if (forced) return false;
        JavaTypeInstance childType = child.getInferredJavaType().getJavaTypeInstance();
        return childType.implicitlyCastsTo(tgtType, gtb);
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        collector.collect(getInferredJavaType().getJavaTypeInstance());
        child.collectTypeUsages(collector);
    }

    @Override
    public Precedence getPrecedence() {
        return Precedence.UNARY_OTHER;
    }

    /*
     * Actions in here make this slightly gross.  Would be nicer to convert ahead of time.
     */
    @Override
    public Dumper dumpInner(Dumper d) {
        JavaTypeInstance castType = getInferredJavaType().getJavaTypeInstance();
        while (castType instanceof JavaWildcardTypeInstance) {
            castType = ((JavaWildcardTypeInstance) castType).getUnderlyingType();
        }
        JavaTypeInstance displayCastType = castType;
        if (child instanceof LambdaExpressionCommon && ExpressionTypeHintHelper.isObjectOnlyGenericType(castType)) {
            displayCastType = castType.getDeGenerifiedType();
        }
        if (child instanceof MemberFunctionInvokation) {
            MemberFunctionInvokation memberFunctionInvokation = (MemberFunctionInvokation) child;
            JavaTypeInstance displayType = ExpressionTypeHintHelper.getDisplayType(memberFunctionInvokation, castType);
            if (shouldPreserveReferenceFunctionCast(memberFunctionInvokation, castType, displayType)) {
                d.separator("(").dump(displayCastType).separator(")");
                child.dumpWithOuterPrecedence(d, getPrecedence(), Troolean.NEITHER);
                return d;
            }
        }
        if (forced) {
            child = simplifyForcedExpectedCastChild(castType, child);
            if (child instanceof MemberFunctionInvokation) {
                MemberFunctionInvokation memberFunctionInvokation = (MemberFunctionInvokation) child;
                memberFunctionInvokation.improveAgainstExpectedType(castType);
                DisplayTypeResolution resolution = memberFunctionInvokation.resolveDisplayReturnType(castType);
                boolean suppressExpectedMemberCast = ExpressionTypeHintHelper.shouldSuppressExpectedMemberCast(
                        memberFunctionInvokation,
                        castType,
                        resolution
                );
                boolean suppressReceiverGenericCast = canSuppressReceiverGenericCast(memberFunctionInvokation, castType);
                if (suppressExpectedMemberCast) {
                    dumpSuppressedCastChild(d, child);
                    return d;
                }
                if (suppressReceiverGenericCast) {
                    dumpSuppressedCastChild(d, child);
                    return d;
                }
            }
            if (child instanceof StaticFunctionInvokation) {
                StaticFunctionInvokation staticFunctionInvokation = (StaticFunctionInvokation) child;
                staticFunctionInvokation.improveAgainstExpectedType(castType);
                DisplayTypeResolution resolution = staticFunctionInvokation.resolveDisplayReturnType(castType);
                if (ExpressionTypeHintHelper.shouldSuppressExpectedStaticCast(
                        staticFunctionInvokation,
                        castType,
                        resolution
                )) {
                    dumpSuppressedCastChild(d, child);
                    return d;
                }
            }
            if (child instanceof ConstructorInvokationSimple) {
                JavaTypeInstance constructorType = ExpressionTypeHintHelper.getDisplayType(child, castType);
                if (constructorType != null
                        && constructorType.implicitlyCastsTo(castType, null)) {
                    dumpSuppressedCastChild(d, child);
                    return d;
                }
            }
            if (castType == RawJavaType.NULL) {
                child.dumpWithOuterPrecedence(d, getPrecedence(), Troolean.NEITHER);
            } else {
                d.separator("(").dump(displayCastType).separator(")");
                child.dumpWithOuterPrecedence(d, getPrecedence(), Troolean.NEITHER);
            }
            return d;
        }
        JavaTypeInstance originalChildType = child.getInferredJavaType().getJavaTypeInstance();
        ExpressionTypeHintHelper.improveExpressionType(child, castType, TypeRecoveryPasses.CAST_CHILD_HINT);
        JavaTypeInstance childType = child.getInferredJavaType().getJavaTypeInstance();
        boolean requiresExplicitFunctionCast = false;
        if (child instanceof MemberFunctionInvokation) {
            MemberFunctionInvokation memberFunctionInvokation = (MemberFunctionInvokation) child;
            memberFunctionInvokation.improveAgainstExpectedType(castType);
            DisplayTypeResolution resolution = memberFunctionInvokation.resolveDisplayReturnType(castType);
            JavaTypeInstance displayType = resolution.getResolvedType();
            requiresExplicitFunctionCast = resolution.requiresExplicitCast()
                    || shouldPreserveReferenceFunctionCast(memberFunctionInvokation, castType, displayType);
            if (requiresExplicitFunctionCast
                    && ExpressionTypeHintHelper.shouldSuppressExpectedMemberCast(
                    memberFunctionInvokation,
                    castType,
                    resolution
            )) {
                requiresExplicitFunctionCast = false;
            }
            boolean suppressReceiverGenericCast = canSuppressReceiverGenericCast(memberFunctionInvokation, castType);
            if (suppressReceiverGenericCast) {
                dumpSuppressedCastChild(d, child);
                return d;
            }
            if (displayType != null) {
                childType = displayType;
            }
        } else if (child instanceof StaticFunctionInvokation) {
            ((StaticFunctionInvokation) child).improveAgainstExpectedType(castType);
            childType = child.getInferredJavaType().getJavaTypeInstance();
        }
        boolean preservePrimitiveCast = castType instanceof RawJavaType
                && childType instanceof RawJavaType
                && !castType.equals(childType);
        if (shouldPreferOriginalChildType(castType, originalChildType)) {
            dumpSuppressedCastChild(d, child);
            return d;
        }
        if (childType != null
                && childType.getDeGenerifiedType().equals(castType.getDeGenerifiedType())
                && !castType.equals(childType)
                && !preservePrimitiveCast
                && !requiresExplicitFunctionCast) {
            dumpSuppressedCastChild(d, child);
            return d;
        }
        if (child instanceof AbstractFunctionInvokation
                && childType != null
                && childType.implicitlyCastsTo(castType, null)
                && !preservePrimitiveCast
                && !requiresExplicitFunctionCast) {
            dumpSuppressedCastChild(d, child);
            return d;
        }
        if (!(castType instanceof RawJavaType)
                && !(childType instanceof RawJavaType)
                && childType != null
                && childType.implicitlyCastsTo(castType, null)
                && !requiresExplicitFunctionCast) {
            dumpSuppressedCastChild(d, child);
            return d;
        }
        if (castType.getInnerClassHereInfo().isAnonymousClass()) {
            dumpSuppressedCastChild(d, child);
            return d;
        }
        if (castType == RawJavaType.NULL) {
            child.dumpWithOuterPrecedence(d, getPrecedence(), Troolean.NEITHER);
        } else {
            d.separator("(").dump(displayCastType).separator(")");
            child.dumpWithOuterPrecedence(d, getPrecedence(), Troolean.NEITHER);
        }
        return d;
    }

    private void dumpSuppressedCastChild(Dumper d, Expression expression) {
        // When we suppress a redundant cast we still need to preserve the precedence
        // boundary that the cast would have imposed. This matters for ternaries flowing
        // into arithmetic/string-concat expressions such as `a + (cond ? x : y)`.
        expression.dumpWithOuterPrecedence(d, getPrecedence(), Troolean.NEITHER);
    }

    private boolean shouldPreserveReferenceFunctionCast(MemberFunctionInvokation invokation,
                                                        JavaTypeInstance castType,
                                                        JavaTypeInstance displayType) {
        if (invokation == null
                || castType == null
                || displayType == null
                || castType instanceof RawJavaType
                || !castType.equals(displayType)
                || !(invokation.getMethodPrototype().getReturnType() instanceof JavaGenericPlaceholderTypeInstance)) {
            return false;
        }
        JavaTypeInstance receiverType = getReceiverDisplayType(invokation.getObject());
        return receiverType == null
                || !ExpressionTypeHintHelper.canDisplayTypeArguments(receiverType)
                || hasConflictingGenericReceiver(invokation.getObject());
    }

    private boolean canSuppressReceiverGenericCast(MemberFunctionInvokation invokation,
                                                   JavaTypeInstance castType) {
        if (invokation == null
                || castType == null
                || castType instanceof RawJavaType
                || !(invokation.getMethodPrototype().getReturnType() instanceof JavaGenericPlaceholderTypeInstance)) {
            return false;
        }
        Expression receiver = invokation.getObject();
        if (hasConflictingGenericReceiver(receiver)) {
            return false;
        }
        JavaTypeInstance receiverType = getReceiverDisplayType(receiver);
        if (!ExpressionTypeHintHelper.canDisplayTypeArguments(receiverType)
                || ExpressionTypeHintHelper.isObjectOnlyGenericType(receiverType)) {
            return false;
        }
        JavaTypeInstance resolvedType = invokation.getMethodPrototype().getReturnType(receiverType, invokation.getArgs());
        if (resolvedType == null) {
            return false;
        }
        JavaTypeInstance resolvedBaseType = resolvedType.getDeGenerifiedType();
        JavaTypeInstance castBaseType = castType.getDeGenerifiedType();
        if (resolvedBaseType == null
                || castBaseType == null
                || !resolvedBaseType.equals(castBaseType)) {
            return false;
        }
        return resolvedType.equals(castType)
                || resolvedType.implicitlyCastsTo(castType, null)
                || ExpressionTypeHintHelper.shouldPreferResolvedType(castType, resolvedType)
                && ExpressionTypeHintHelper.canDisplayTypeArguments(resolvedType);
    }

    private JavaTypeInstance getReceiverDisplayType(Expression receiver) {
        if (receiver == null) {
            return null;
        }
        if (receiver instanceof CastExpression) {
            JavaTypeInstance childType = getReceiverDisplayType(((CastExpression) receiver).getChild());
            if (childType != null) {
                return childType;
            }
        }
        if (receiver instanceof LValueExpression) {
            LValue lValue = ((LValueExpression) receiver).getLValue();
            if (lValue instanceof LocalVariable) {
                LocalVariable localVariable = (LocalVariable) lValue;
                if (localVariable.hasConflictingGenericDeclaration()) {
                    return localVariable.getInferredJavaType().getJavaTypeInstance();
                }
                JavaTypeInstance creatorType = localVariable.getCustomCreationJavaType();
                JavaTypeInstance inferredType = localVariable.getInferredJavaType().getJavaTypeInstance();
                if (creatorType != null) {
                    return ExpressionTypeHintHelper.preferResolvedType(creatorType, inferredType);
                }
                return inferredType;
            }
        }
        return ExpressionTypeHintHelper.getDisplayType(receiver, null);
    }

    private Expression simplifyForcedExpectedCastChild(JavaTypeInstance castType, Expression candidateChild) {
        if (!(candidateChild instanceof CastExpression) || castType == null) {
            return candidateChild;
        }
        CastExpression innerCast = (CastExpression) candidateChild;
        JavaTypeInstance innerCastType = innerCast.getInferredJavaType().getJavaTypeInstance();
        if (innerCastType == null
                || innerCastType instanceof RawJavaType && castType instanceof RawJavaType
                || !innerCastType.getDeGenerifiedType().equals(castType.getDeGenerifiedType())) {
            return candidateChild;
        }
        Expression innerChild = innerCast.getChild();
        if (innerChild instanceof MemberFunctionInvokation) {
            MemberFunctionInvokation memberFunctionInvokation = (MemberFunctionInvokation) innerChild;
            DisplayTypeResolution resolution = memberFunctionInvokation.resolveDisplayReturnType(castType);
            if (ExpressionTypeHintHelper.shouldSuppressExpectedMemberCast(memberFunctionInvokation, castType, resolution)) {
                return innerChild;
            }
        } else if (innerChild instanceof StaticFunctionInvokation) {
            StaticFunctionInvokation staticFunctionInvokation = (StaticFunctionInvokation) innerChild;
            DisplayTypeResolution resolution = staticFunctionInvokation.resolveDisplayReturnType(castType);
            if (ExpressionTypeHintHelper.shouldSuppressExpectedStaticCast(staticFunctionInvokation, castType, resolution)) {
                return innerChild;
            }
        } else if (innerChild instanceof ConstructorInvokationSimple) {
            JavaTypeInstance constructorType = ExpressionTypeHintHelper.getDisplayType(innerChild, castType);
            if (constructorType != null && constructorType.implicitlyCastsTo(castType, null)) {
                return innerChild;
            }
        }
        return candidateChild;
    }

    private boolean shouldPreferOriginalChildType(JavaTypeInstance castType, JavaTypeInstance originalChildType) {
        if (castType == null || originalChildType == null) {
            return false;
        }
        JavaTypeInstance castBaseType = castType.getDeGenerifiedType();
        JavaTypeInstance childBaseType = originalChildType.getDeGenerifiedType();
        if (castBaseType == null || !castBaseType.equals(childBaseType)) {
            return false;
        }
        return ExpressionTypeHintHelper.shouldPreferResolvedType(castType, originalChildType);
    }


    @Override
    public Expression replaceSingleUsageLValues(LValueRewriter lValueRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer) {
        child = child.replaceSingleUsageLValues(lValueRewriter, ssaIdentifiers, statementContainer);
        return this;
    }

    @Override
    public Expression applyExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        child = expressionRewriter.rewriteExpression(child, ssaIdentifiers, statementContainer, flags);
        return this;
    }

    @Override
    public Expression applyReverseExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        return applyExpressionRewriter(expressionRewriter, ssaIdentifiers, statementContainer, flags);
    }

    @Override
    public void collectUsedLValues(LValueUsageCollector lValueUsageCollector) {
        child.collectUsedLValues(lValueUsageCollector);
    }

    public Expression getChild() {
        return child;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof CastExpression)) return false;
        CastExpression other = (CastExpression) o;
        if (!getInferredJavaType().getJavaTypeInstance().equals(other.getInferredJavaType().getJavaTypeInstance())) return false;
        return child.equals(other.child);
    }

    @Override
    public boolean rewriteBoxing(PrimitiveBoxingRewriter boxingRewriter) {
        // Horrible edge case.  If we're forcibly downcasting a cast, then skip the middle one.
        if (isForced()) {
            return false;
        }
        JavaTypeInstance thisType = getInferredJavaType().getJavaTypeInstance();
        while (child instanceof CastExpression) {
            CastExpression childCast = (CastExpression) child;
            JavaTypeInstance childType = childCast.getInferredJavaType().getJavaTypeInstance();
            Expression grandChild = childCast.child;
            JavaTypeInstance grandChildType = grandChild.getInferredJavaType().getJavaTypeInstance();
//            if (thisType.implicitlyCastsTo(grandChildType)) {
//                child = childCast.child;
//            } else {
//                break;
//            }
            if (Literal.NULL.equals(grandChild) && !thisType.isObject() && childType.isObject()) {
                break;
            }
            if (grandChildType.implicitlyCastsTo(childType, null) && childType.implicitlyCastsTo(thisType, null)) {
                child = childCast.child;
            } else {
                if (grandChildType instanceof RawJavaType && childType instanceof RawJavaType && thisType instanceof RawJavaType) {
                    if (!grandChildType.implicitlyCastsTo(childType, null) && !childType.implicitlyCastsTo(thisType, null)) {
                        child = childCast.child;
                        continue;
                    }
                }
                break;
            }
        }
        Expression newchild = boxingRewriter.sugarNonParameterBoxing(child, thisType);
        JavaTypeInstance childType = child.getInferredJavaType().getJavaTypeInstance();
        JavaTypeInstance newChildType = newchild.getInferredJavaType().getJavaTypeInstance();
        if (child != newchild && newChildType.implicitlyCastsTo(childType, null)) {
            // We can do this, but only if the original cast wasn't deliberately lossy.
            if (childType.implicitlyCastsTo(thisType, null) ==
            newChildType.implicitlyCastsTo(thisType, null)) {
                child = newchild;
            }
        }
        return false;
    }

    @Override
    public void applyNonArgExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
    }

    @Override
    public boolean equivalentUnder(Object o, EquivalenceConstraint constraint) {
        if (o == null) return false;
        if (o == this) return true;
        if (getClass() != o.getClass()) return false;
        CastExpression other = (CastExpression) o;
        if (!constraint.equivalent(getInferredJavaType().getJavaTypeInstance(), other.getInferredJavaType().getJavaTypeInstance()))
            return false;
        if (!constraint.equivalent(child, other.child)) return false;
        return true;
    }

    public static Expression removeImplicit(Expression e) {
        while (e instanceof CastExpression
                && ((CastExpression) e).couldBeImplicit(null)
                && !((CastExpression) e).changesPrimitiveType()) {
            e = ((CastExpression) e).getChild();
        }
        return e;
    }

    public static Expression removeImplicitOuterType(Expression e, GenericTypeBinder gtb, boolean rawArg) {
        final JavaTypeInstance t = e.getInferredJavaType().getJavaTypeInstance();
        while (e instanceof CastExpression
                && ((CastExpression) e).couldBeImplicit(gtb)
                && ((CastExpression) e).couldBeImplicit(t, gtb)) {
            if (((CastExpression) e).changesPrimitiveType()) {
                break;
            }
            Expression newE = ((CastExpression) e).getChild();
            if (!rawArg) {
                boolean wasRaw = e.getInferredJavaType().getJavaTypeInstance() instanceof RawJavaType;
                boolean isRaw = newE.getInferredJavaType().getJavaTypeInstance() instanceof RawJavaType;
                if (wasRaw && !isRaw) {
                    break;
                }
            }
            e = newE;
        }
        return e;
    }

    public static Expression tryRemoveCast(Expression e) {
        if (e instanceof CastExpression) {
            Expression ce = ((CastExpression) e).getChild();
            if (!((CastExpression) e).changesPrimitiveType()
                    && ce.getInferredJavaType().getJavaTypeInstance().implicitlyCastsTo(e.getInferredJavaType().getJavaTypeInstance(), null)) {
                e = ce;
            }
        }
        return e;
    }

    private boolean changesPrimitiveType() {
        JavaTypeInstance castType = getInferredJavaType().getJavaTypeInstance();
        JavaTypeInstance childType = child.getInferredJavaType().getJavaTypeInstance();
        return castType instanceof RawJavaType
                && childType instanceof RawJavaType
                && !castType.equals(childType);
    }

    private boolean hasConflictingGenericReceiver(Expression receiver) {
        if (!(receiver instanceof LValueExpression)) {
            return false;
        }
        LValue lValue = ((LValueExpression) receiver).getLValue();
        return lValue instanceof LocalVariable
                && ((LocalVariable) lValue).hasConflictingGenericDeclaration();
    }

}
