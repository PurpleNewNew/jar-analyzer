package org.benf.cfr.reader.bytecode.analysis.parse.expression;

import org.benf.cfr.reader.bytecode.StructuredPassEntry;
import org.benf.cfr.reader.bytecode.TypeRecoveryPasses;
import org.benf.cfr.reader.bytecode.TypeRecoveryTracing;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.types.BindingSuperContainer;
import org.benf.cfr.reader.bytecode.analysis.types.JavaGenericBaseInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaGenericPlaceholderTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaWildcardTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.RawJavaType;
import org.benf.cfr.reader.bytecode.analysis.types.TypeConstants;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;

import java.util.List;

public final class ExpressionTypeHintHelper {
    private ExpressionTypeHintHelper() {
    }

    public static void improveExpressionType(Expression expression, JavaTypeInstance expectedType) {
        improveExpressionType(expression, expectedType, TypeRecoveryPasses.NESTED_EXPRESSION_HINT);
    }

    public static void improveExpressionType(Expression expression,
                                             JavaTypeInstance expectedType,
                                             StructuredPassEntry pass) {
        if (expression == null) {
            return;
        }
        JavaTypeInstance normalizedExpectedType = normalizeExpectedType(expectedType);
        if (normalizedExpectedType == null) {
            return;
        }
        final JavaTypeInstance finalExpectedType = normalizedExpectedType;
        TypeRecoveryTracing.trace(pass, expression, finalExpectedType, () -> doImproveExpressionType(expression, finalExpectedType));
    }

    public static JavaTypeInstance getDisplayType(Expression expression) {
        return getDisplayType(expression, null);
    }

    public static JavaTypeInstance getDisplayType(Expression expression, JavaTypeInstance expectedType) {
        if (expression == null) {
            return null;
        }
        JavaTypeInstance normalizedExpectedType = normalizeExpectedType(expectedType);
        if (expression instanceof ConstructorInvokationSimple) {
            if (normalizedExpectedType != null) {
                ((ConstructorInvokationSimple) expression).improveConstructionType(normalizedExpectedType);
            }
            return ((ConstructorInvokationSimple) expression).getDisplayTypeInstance();
        }
        if (expression instanceof StaticFunctionInvokation) {
            StaticFunctionInvokation staticFunctionInvokation = (StaticFunctionInvokation) expression;
            DisplayTypeResolution resolution = staticFunctionInvokation.resolveDisplayReturnType(normalizedExpectedType);
            TypeRecoveryTracing.traceObservedType(
                    TypeRecoveryPasses.DISPLAY_TYPE_STATIC_BINDER,
                    staticFunctionInvokation,
                    normalizedExpectedType,
                    resolution::getResolvedType,
                    resolution::getDetail,
                    () -> {
                    }
            );
            return TypeRecoveryTracing.traceObservedType(
                    TypeRecoveryPasses.DISPLAY_TYPE_STATIC_RETURN,
                    staticFunctionInvokation,
                    normalizedExpectedType,
                    resolution::getResolvedType,
                    resolution::getDetail,
                    () -> staticFunctionInvokation.improveAgainstExpectedType(normalizedExpectedType)
            );
        }
        if (expression instanceof MemberFunctionInvokation) {
            MemberFunctionInvokation memberFunctionInvokation = (MemberFunctionInvokation) expression;
            DisplayTypeResolution resolution = memberFunctionInvokation.resolveDisplayReturnType(normalizedExpectedType);
            TypeRecoveryTracing.traceObservedType(
                    TypeRecoveryPasses.DISPLAY_TYPE_MEMBER_BINDER,
                    memberFunctionInvokation,
                    normalizedExpectedType,
                    resolution::getResolvedType,
                    resolution::getDetail,
                    () -> {
                    }
            );
            return TypeRecoveryTracing.traceObservedType(
                    TypeRecoveryPasses.DISPLAY_TYPE_MEMBER_RETURN,
                    memberFunctionInvokation,
                    normalizedExpectedType,
                    resolution::getResolvedType,
                    resolution::getDetail,
                    () -> memberFunctionInvokation.improveAgainstExpectedType(normalizedExpectedType)
            );
        }
        if (expression instanceof AssignmentExpression) {
            return getDisplayType(((AssignmentExpression) expression).getrValue(), normalizedExpectedType);
        }
        if (expression instanceof CastExpression) {
            CastExpression castExpression = (CastExpression) expression;
            JavaTypeInstance castType = castExpression.getInferredJavaType().getJavaTypeInstance();
            JavaTypeInstance childType = getDisplayType(castExpression.getChild(), null);
            if (shouldPreferChildCastDisplayType(castType, childType)) {
                return childType;
            }
            return castType;
        }
        return expression.getInferredJavaType().getJavaTypeInstance();
    }

    public static JavaTypeInstance sanitizeConstructorType(JavaTypeInstance type) {
        JavaTypeInstance normalizedType = normalizeExpectedType(type);
        if (normalizedType == null) {
            return null;
        }
        if (canDisplayTypeArguments(normalizedType)) {
            return normalizedType;
        }
        return normalizedType.getDeGenerifiedType();
    }

    public static boolean shouldSuppressExplicitGenerics(List<JavaTypeInstance> explicitTypes) {
        if (explicitTypes == null || explicitTypes.isEmpty()) {
            return false;
        }
        for (JavaTypeInstance explicitType : explicitTypes) {
            if (explicitType == null || explicitType == TypeConstants.OBJECT) {
                return true;
            }
            if (!canDisplayTypeArguments(explicitType)) {
                return true;
            }
            if (explicitType.equals(explicitType.getDeGenerifiedType())) {
                return true;
            }
        }
        return false;
    }

    public static boolean canDefineLocalType(JavaTypeInstance type) {
        if (type == null || type == RawJavaType.VOID) {
            return false;
        }
        return !(type instanceof JavaWildcardTypeInstance);
    }

    public static JavaTypeInstance normalizeExpectedType(JavaTypeInstance expectedType) {
        if (expectedType instanceof JavaGenericBaseInstance
                && ((JavaGenericBaseInstance) expectedType).hasL01Wildcard()) {
            return ((JavaGenericBaseInstance) expectedType).getWithoutL01Wildcard();
        }
        return expectedType;
    }

    public static boolean isSpecific(JavaTypeInstance type) {
        if (type == null) {
            return false;
        }
        if (type instanceof JavaGenericBaseInstance) {
            return !((JavaGenericBaseInstance) type).hasUnbound();
        }
        return true;
    }

    public static boolean canDisplayTypeArguments(JavaTypeInstance type) {
        if (type == null) {
            return false;
        }
        if (type instanceof JavaWildcardTypeInstance || type instanceof JavaGenericPlaceholderTypeInstance) {
            return false;
        }
        if (type instanceof JavaGenericBaseInstance) {
            JavaGenericBaseInstance genericType = (JavaGenericBaseInstance) type;
            if (genericType.hasUnbound() || genericType.hasL01Wildcard()) {
                return false;
            }
            for (JavaTypeInstance parameterType : genericType.getGenericTypes()) {
                if (!canDisplayTypeArguments(parameterType)) {
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean shouldPreferResolvedType(JavaTypeInstance currentType, JavaTypeInstance candidateType) {
        if (candidateType == null || candidateType == RawJavaType.VOID) {
            return false;
        }
        if (currentType == null) {
            return true;
        }
        if (currentType.equals(candidateType)) {
            return false;
        }
        if (currentType instanceof JavaGenericPlaceholderTypeInstance
                && !(candidateType instanceof JavaGenericPlaceholderTypeInstance)) {
            return true;
        }
        if (!(currentType instanceof JavaGenericPlaceholderTypeInstance)
                && candidateType instanceof JavaGenericPlaceholderTypeInstance) {
            return false;
        }
        if (currentType == TypeConstants.OBJECT) {
            return true;
        }
        JavaTypeInstance currentBaseType = currentType.getDeGenerifiedType();
        JavaTypeInstance candidateBaseType = candidateType.getDeGenerifiedType();
        if (currentBaseType.equals(candidateBaseType)) {
            if (isRawToObjectOnlyUpgrade(currentType, candidateType)) {
                return false;
            }
            if (isObjectOnlyGenericToRawFallback(currentType, candidateType)) {
                return true;
            }
            if (!(currentType instanceof JavaGenericBaseInstance)
                    && candidateType instanceof JavaGenericBaseInstance
                    && canDisplayTypeArguments(candidateType)) {
                return true;
            }
            if (currentType instanceof JavaGenericBaseInstance
                    && candidateType instanceof JavaGenericBaseInstance) {
                JavaGenericBaseInstance currentGeneric = (JavaGenericBaseInstance) currentType;
                JavaGenericBaseInstance candidateGeneric = (JavaGenericBaseInstance) candidateType;
                boolean currentDisplayable = canDisplayTypeArguments(currentType);
                boolean candidateDisplayable = canDisplayTypeArguments(candidateType);
                if (!currentDisplayable && candidateDisplayable) {
                    return true;
                }
                if (currentDisplayable && !candidateDisplayable) {
                    return false;
                }
                if (currentGeneric.hasUnbound() && !candidateGeneric.hasUnbound()) {
                    return true;
                }
                if (!currentGeneric.hasUnbound() && candidateGeneric.hasUnbound()) {
                    return false;
                }
            }
        }
        BindingSuperContainer candidateSupers = candidateType.getBindingSupers();
        if (candidateSupers != null && candidateSupers.containsBase(currentBaseType)) {
            return false;
        }
        BindingSuperContainer currentSupers = currentType.getBindingSupers();
        if (currentSupers != null && currentSupers.containsBase(candidateBaseType)) {
            if (candidateBaseType == TypeConstants.OBJECT) {
                return false;
            }
            if (isSpecific(currentType) && !isSpecific(candidateType)) {
                return false;
            }
            return true;
        }
        if (currentType instanceof JavaGenericBaseInstance
                && ((JavaGenericBaseInstance) currentType).hasUnbound()) {
            return !currentBaseType.equals(candidateBaseType)
                    || candidateType instanceof JavaGenericPlaceholderTypeInstance
                    || isSpecific(candidateType);
        }
        return !currentBaseType.equals(candidateBaseType)
                || (!isSpecific(currentType) && isSpecific(candidateType));
    }

    public static JavaTypeInstance preferResolvedType(JavaTypeInstance currentType, JavaTypeInstance candidateType) {
        return shouldPreferResolvedType(currentType, candidateType) ? candidateType : currentType;
    }

    public static String describeObservedType(Expression expression) {
        if (expression == null) {
            return null;
        }
        if (expression instanceof ConstructorInvokationSimple) {
            return describeType(((ConstructorInvokationSimple) expression).getDisplayTypeInstance());
        }
        if (expression instanceof StaticFunctionInvokation) {
            return describeType(((StaticFunctionInvokation) expression).getDisplayReturnType());
        }
        if (expression instanceof MemberFunctionInvokation) {
            return describeType(((MemberFunctionInvokation) expression).getDisplayReturnType(null));
        }
        return describeType(expression.getInferredJavaType().getJavaTypeInstance());
    }

    public static String describeType(JavaTypeInstance type) {
        return type == null ? null : type.toString();
    }

    public static Expression applyExpectedCastIfNeeded(Expression expression, JavaTypeInstance expectedType) {
        JavaTypeInstance normalizedExpectedType = normalizeExpectedType(expectedType);
        if (expression == null || normalizedExpectedType == null || expression instanceof CastExpression) {
            return expression;
        }
        if (expression instanceof MemberFunctionInvokation) {
            MemberFunctionInvokation memberFunctionInvokation = (MemberFunctionInvokation) expression;
            DisplayTypeResolution resolution = memberFunctionInvokation.resolveDisplayReturnType(normalizedExpectedType);
            if (resolution.requiresExplicitCast()
                    && !shouldSuppressExpectedMemberCast(memberFunctionInvokation, normalizedExpectedType, resolution)) {
                return new CastExpression(
                        BytecodeLoc.NONE,
                        new InferredJavaType(normalizedExpectedType, InferredJavaType.Source.EXPRESSION, true),
                        expression,
                        true
                );
            }
        }
        if (expression instanceof StaticFunctionInvokation) {
            StaticFunctionInvokation staticFunctionInvokation = (StaticFunctionInvokation) expression;
            DisplayTypeResolution resolution = staticFunctionInvokation.resolveDisplayReturnType(normalizedExpectedType);
            if ((resolution.requiresExplicitCast() || staticFunctionInvokation.needsExpectedReturnCast(normalizedExpectedType))
                    && !shouldSuppressExpectedStaticCast(staticFunctionInvokation, normalizedExpectedType, resolution)) {
                return new CastExpression(
                        BytecodeLoc.NONE,
                        new InferredJavaType(normalizedExpectedType, InferredJavaType.Source.EXPRESSION, true),
                        expression,
                        true
                );
            }
        }
        return expression;
    }

    static boolean shouldSuppressExpectedMemberCast(MemberFunctionInvokation invokation,
                                                    JavaTypeInstance expectedType,
                                                    DisplayTypeResolution resolution) {
        JavaTypeInstance resolvedType = resolution.getResolvedType();
        if (expectedType == null || resolvedType == null) {
            return false;
        }
        JavaTypeInstance receiverType = getDisplayType(invokation.getObject(), null);
        if (!canDisplayTypeArguments(receiverType) || isObjectOnlyGenericType(receiverType)) {
            return false;
        }
        JavaTypeInstance expectedBaseType = expectedType.getDeGenerifiedType();
        JavaTypeInstance resolvedBaseType = resolvedType.getDeGenerifiedType();
        return expectedBaseType != null
                && resolvedBaseType != null
                && expectedBaseType.equals(resolvedBaseType)
                && canDisplayTypeArguments(expectedType);
    }

    static boolean shouldSuppressExpectedStaticCast(StaticFunctionInvokation invokation,
                                                    JavaTypeInstance expectedType,
                                                    DisplayTypeResolution resolution) {
        JavaTypeInstance resolvedType = resolution.getResolvedType();
        if (expectedType == null || resolvedType == null) {
            return false;
        }
        if (!canDisplayTypeArguments(expectedType) || isObjectOnlyGenericType(expectedType)) {
            return false;
        }
        JavaTypeInstance expectedBaseType = expectedType.getDeGenerifiedType();
        JavaTypeInstance resolvedBaseType = resolvedType.getDeGenerifiedType();
        if (expectedBaseType == null || !expectedBaseType.equals(resolvedBaseType)) {
            return false;
        }
        if (invokation.needsExpectedReturnCast(expectedType)) {
            return false;
        }
        return true;
    }

    public static boolean needsExpectedCast(Expression expression, JavaTypeInstance expectedType) {
        JavaTypeInstance normalizedExpectedType = normalizeExpectedType(expectedType);
        if (expression == null || normalizedExpectedType == null) {
            return false;
        }
        if (expression instanceof MemberFunctionInvokation) {
            return ((MemberFunctionInvokation) expression)
                    .resolveDisplayReturnType(normalizedExpectedType)
                    .requiresExplicitCast();
        }
        if (expression instanceof StaticFunctionInvokation) {
            StaticFunctionInvokation staticFunctionInvokation = (StaticFunctionInvokation) expression;
            return staticFunctionInvokation.resolveDisplayReturnType(normalizedExpectedType).requiresExplicitCast()
                    || staticFunctionInvokation.needsExpectedReturnCast(normalizedExpectedType);
        }
        return false;
    }

    private static boolean isRawToObjectOnlyUpgrade(JavaTypeInstance currentType, JavaTypeInstance candidateType) {
        if (currentType instanceof JavaGenericBaseInstance) {
            return false;
        }
        if (!(candidateType instanceof JavaGenericBaseInstance)) {
            return false;
        }
        return isObjectOnlyGeneric((JavaGenericBaseInstance) candidateType);
    }

    public static boolean isObjectOnlyGenericType(JavaTypeInstance type) {
        return type instanceof JavaGenericBaseInstance && isObjectOnlyGeneric((JavaGenericBaseInstance) type);
    }

    private static boolean isObjectOnlyGeneric(JavaGenericBaseInstance genericType) {
        if (genericType == null || genericType.hasUnbound() || genericType.hasL01Wildcard()) {
            return false;
        }
        List<JavaTypeInstance> genericTypes = genericType.getGenericTypes();
        if (genericTypes == null || genericTypes.isEmpty()) {
            return false;
        }
        for (JavaTypeInstance genericParameter : genericTypes) {
            if (genericParameter == null || genericParameter != TypeConstants.OBJECT) {
                return false;
            }
        }
        return true;
    }

    private static boolean isObjectOnlyGenericToRawFallback(JavaTypeInstance currentType, JavaTypeInstance candidateType) {
        if (!(currentType instanceof JavaGenericBaseInstance)) {
            return false;
        }
        if (candidateType instanceof JavaGenericBaseInstance) {
            return false;
        }
        return isObjectOnlyGeneric((JavaGenericBaseInstance) currentType);
    }

    private static boolean shouldPreferChildCastDisplayType(JavaTypeInstance castType, JavaTypeInstance childType) {
        if (castType == null || childType == null) {
            return false;
        }
        JavaTypeInstance castBaseType = castType.getDeGenerifiedType();
        JavaTypeInstance childBaseType = childType.getDeGenerifiedType();
        if (castBaseType == null || !castBaseType.equals(childBaseType)) {
            return false;
        }
        return shouldPreferResolvedType(castType, childType);
    }

    private static void doImproveExpressionType(Expression expression, JavaTypeInstance normalizedExpectedType) {
        if (expression instanceof LambdaExpression) {
            ((LambdaExpression) expression).applyTargetTypeConstraint(normalizedExpectedType);
            return;
        }
        if (expression instanceof ConstructorInvokationSimple) {
            ((ConstructorInvokationSimple) expression).improveConstructionType(normalizedExpectedType);
            return;
        }
        if (expression instanceof AssignmentExpression) {
            AssignmentExpression assignmentExpression = (AssignmentExpression) expression;
            JavaTypeInstance assignmentType = assignmentExpression.getUpdatedLValue()
                    .getInferredJavaType()
                    .getJavaTypeInstance();
            if (!isSpecific(normalizedExpectedType) && isSpecific(assignmentType)) {
                normalizedExpectedType = assignmentType;
            }
            improveExpressionType(assignmentExpression.getrValue(), normalizedExpectedType, TypeRecoveryPasses.NESTED_EXPRESSION_HINT);
            return;
        }
        if (expression instanceof StaticFunctionInvokation) {
            ((StaticFunctionInvokation) expression).improveAgainstExpectedType(normalizedExpectedType);
            return;
        }
        if (expression instanceof MemberFunctionInvokation) {
            ((MemberFunctionInvokation) expression).improveAgainstExpectedType(normalizedExpectedType);
            return;
        }
        if (expression instanceof CastExpression) {
            improveExpressionType(((CastExpression) expression).getChild(), normalizedExpectedType, TypeRecoveryPasses.NESTED_EXPRESSION_HINT);
            return;
        }
        if (expression instanceof TernaryExpression) {
            ((TernaryExpression) expression).applyTargetTypeConstraint(normalizedExpectedType);
        }
    }
}
