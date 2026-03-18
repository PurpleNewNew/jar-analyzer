package org.benf.cfr.reader.bytecode.analysis.parse.expression;

import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.types.JavaGenericBaseInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaGenericPlaceholderTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaWildcardTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.RawJavaType;
import org.benf.cfr.reader.bytecode.analysis.types.TypeConstants;

import java.util.List;

public final class ExpressionTypeHintHelper {
    private ExpressionTypeHintHelper() {
    }

    public static void improveExpressionType(Expression expression, JavaTypeInstance expectedType) {
        if (expression == null) {
            return;
        }
        JavaTypeInstance normalizedExpectedType = normalizeExpectedType(expectedType);
        if (normalizedExpectedType == null) {
            return;
        }
        if (expression instanceof LambdaExpression) {
            ((LambdaExpression) expression).improveResultType(normalizedExpectedType);
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
            improveExpressionType(assignmentExpression.getrValue(), normalizedExpectedType);
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
            improveExpressionType(((CastExpression) expression).getChild(), normalizedExpectedType);
            return;
        }
        if (expression instanceof TernaryExpression) {
            ((TernaryExpression) expression).improveBranchTypes(normalizedExpectedType);
        }
    }

    public static JavaTypeInstance getDisplayType(Expression expression) {
        if (expression == null) {
            return null;
        }
        if (expression instanceof ConstructorInvokationSimple) {
            return ((ConstructorInvokationSimple) expression).getDisplayTypeInstance();
        }
        if (expression instanceof StaticFunctionInvokation) {
            StaticFunctionInvokation staticFunctionInvokation = (StaticFunctionInvokation) expression;
            staticFunctionInvokation.improveAgainstExpectedType(null);
            return staticFunctionInvokation.getDisplayReturnType();
        }
        if (expression instanceof MemberFunctionInvokation) {
            MemberFunctionInvokation memberFunctionInvokation = (MemberFunctionInvokation) expression;
            memberFunctionInvokation.improveAgainstExpectedType(null);
            return memberFunctionInvokation.getDisplayReturnType(null);
        }
        if (expression instanceof AssignmentExpression) {
            return getDisplayType(((AssignmentExpression) expression).getrValue());
        }
        if (expression instanceof CastExpression) {
            return expression.getInferredJavaType().getJavaTypeInstance();
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
}
