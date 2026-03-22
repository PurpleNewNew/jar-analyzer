package org.benf.cfr.reader.bytecode.analysis.parse;

import org.benf.cfr.reader.bytecode.analysis.parse.expression.ExpressionTypeHintHelper;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.DeepCloneable;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.*;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.LocalVariable;
import org.benf.cfr.reader.bytecode.analysis.types.BindingSuperContainer;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.TypeConstants;
import org.benf.cfr.reader.bytecode.analysis.types.annotated.JavaAnnotatedTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.entities.exceptions.ExceptionCheck;
import org.benf.cfr.reader.util.TypeUsageCollectable;
import org.benf.cfr.reader.util.output.DumpableWithPrecedence;
import org.benf.cfr.reader.util.output.Dumper;

public interface LValue extends DumpableWithPrecedence, DeepCloneable<LValue>, TypeUsageCollectable {
    int getNumberOfCreators();

    <T> void collectLValueAssignments(Expression assignedTo, StatementContainer<T> statementContainer, LValueAssignmentCollector<T> lValueAssigmentCollector);

    boolean doesBlackListLValueReplacement(LValue replace, Expression with);

    void collectLValueUsage(LValueUsageCollector lValueUsageCollector);

    SSAIdentifiers<LValue> collectVariableMutation(SSAIdentifierFactory<LValue, ?> ssaIdentifierFactory);

    LValue replaceSingleUsageLValues(LValueRewriter lValueRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer);

    LValue applyExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags);

    InferredJavaType getInferredJavaType();

    JavaAnnotatedTypeInstance getAnnotatedCreationType();

    boolean canThrow(ExceptionCheck caught);

    void markFinal();

    boolean isFinal();

    boolean isFakeIgnored();

    void markVar();

    boolean isVar();

    boolean validIterator();

    Dumper dump(Dumper d, boolean defines);

    class Creation {
        public static Dumper dump(Dumper d, LValue lValue) {
            return dump(d, lValue, null);
        }

        public static Dumper dump(Dumper d, LValue lValue, JavaTypeInstance overrideType) {
            if (overrideType != null) {
                d.dump(overrideType).separator(" ");
                lValue.dump(d, true);
                return d;
            }
            if (lValue instanceof LocalVariable) {
                LocalVariable localVariable = (LocalVariable) lValue;
                if (localVariable.isVar() || localVariable.getInferredJavaType().isVarConfirmed()) {
                    d.print("var").separator(" ");
                    lValue.dump(d, true);
                    return d;
                }
                if (localVariable.hasConflictingReferenceDeclaration()) {
                    d.dump(TypeConstants.OBJECT).separator(" ");
                    lValue.dump(d, true);
                    return d;
                }
                JavaTypeInstance customCreationJavaType = localVariable.getCustomCreationJavaType();
                if (customCreationJavaType != null) {
                    JavaTypeInstance inferredType = lValue.getInferredJavaType().getJavaTypeInstance();
                    if (localVariable.hasConflictingGenericDeclaration()) {
                        if (shouldPreferSpecificConflictingGenericDeclaration(inferredType, customCreationJavaType)) {
                            // Some bytecode shapes transiently expose raw/Object-only generic locals while the
                            // assignment target has already recovered the specific declaration type. In those
                            // cases, preserving the raw side reintroduces uncompilable self-decompile output.
                            d.dump(preferDeclarationType(inferredType, customCreationJavaType)).separator(" ");
                            lValue.dump(d, true);
                            return d;
                        }
                        JavaTypeInstance rawDeclarationType = resolveConflictingGenericDeclarationType(inferredType, customCreationJavaType);
                        d.dump(rawDeclarationType).separator(" ");
                        lValue.dump(d, true);
                        return d;
                    }
                    JavaTypeInstance declaredType = preferDeclarationType(inferredType, customCreationJavaType);
                    d.dump(declaredType).separator(" ");
                    lValue.dump(d, true);
                    return d;
                }
            }
            JavaAnnotatedTypeInstance annotatedCreationType = lValue.getAnnotatedCreationType();
            if (annotatedCreationType != null) {
                annotatedCreationType.dump(d);
            } else {
                if (lValue.isVar()) {
                    d.print("var");
                } else {
                    InferredJavaType inferredJavaType = lValue.getInferredJavaType();
                    JavaTypeInstance t = inferredJavaType.getJavaTypeInstance();
                    d.dump(t);
                }
            }
            d.separator(" ");
            lValue.dump(d, true);
            return d;
        }

        private static JavaTypeInstance preferDeclarationType(JavaTypeInstance inferredType, JavaTypeInstance creationType) {
            if (inferredType == null || creationType == null || inferredType.equals(creationType)) {
                return creationType;
            }
            JavaTypeInstance reboundCreationType = rebindCreationTypeToInferredBase(inferredType, creationType);
            if (reboundCreationType != null) {
                return reboundCreationType;
            }
            if (ExpressionTypeHintHelper.shouldPreferResolvedType(creationType, inferredType)) {
                return inferredType;
            }
            if (creationType.implicitlyCastsTo(inferredType, null)
                    && !inferredType.implicitlyCastsTo(creationType, null)) {
                return inferredType;
            }
            return creationType;
        }

        private static JavaTypeInstance rebindCreationTypeToInferredBase(JavaTypeInstance inferredType,
                                                                         JavaTypeInstance creationType) {
            if (inferredType == null || creationType == null) {
                return null;
            }
            if (!ExpressionTypeHintHelper.canDisplayTypeArguments(creationType)) {
                return null;
            }
            JavaTypeInstance inferredBaseType = inferredType.getDeGenerifiedType();
            if (inferredBaseType == null) {
                return null;
            }
            if (creationType.getDeGenerifiedType().equals(inferredBaseType)) {
                return creationType;
            }
            BindingSuperContainer bindingSupers = creationType.getBindingSupers();
            if (bindingSupers == null) {
                return null;
            }
            JavaTypeInstance reboundType = bindingSupers.getBoundSuperForBase(inferredBaseType);
            if (reboundType == null || !ExpressionTypeHintHelper.canDisplayTypeArguments(reboundType)) {
                return null;
            }
            return reboundType;
        }

        private static JavaTypeInstance resolveConflictingGenericDeclarationType(JavaTypeInstance inferredType,
                                                                                 JavaTypeInstance creationType) {
            if (inferredType != null
                    && creationType != null
                    && ExpressionTypeHintHelper.canDisplayTypeArguments(inferredType)
                    && ExpressionTypeHintHelper.canDisplayTypeArguments(creationType)) {
                // When metadata says "declare as List<T>" but the creator expression is "new ArrayList<T>(...)",
                // both sides are valid generic views of the same value. Keep the more source-like generic side
                // instead of collapsing all the way back to the raw base type.
                if (inferredType.implicitlyCastsTo(creationType, null)
                        && !creationType.implicitlyCastsTo(inferredType, null)) {
                    return creationType;
                }
                if (creationType.implicitlyCastsTo(inferredType, null)
                        && !inferredType.implicitlyCastsTo(creationType, null)) {
                    return inferredType;
                }
            }
            JavaTypeInstance creationBaseType = creationType == null ? null : creationType.getDeGenerifiedType();
            JavaTypeInstance inferredBaseType = inferredType == null ? null : inferredType.getDeGenerifiedType();
            if (creationBaseType != null && creationBaseType.equals(inferredBaseType)) {
                return creationBaseType;
            }
            if (creationBaseType != null) {
                return creationBaseType;
            }
            return creationType;
        }

        private static boolean shouldPreferSpecificConflictingGenericDeclaration(JavaTypeInstance inferredType,
                                                                                 JavaTypeInstance creationType) {
            if (inferredType == null || creationType == null) {
                return false;
            }
            JavaTypeInstance inferredBaseType = inferredType.getDeGenerifiedType();
            JavaTypeInstance creationBaseType = creationType.getDeGenerifiedType();
            if (inferredBaseType == null || !inferredBaseType.equals(creationBaseType)) {
                return false;
            }
            if (!ExpressionTypeHintHelper.canDisplayTypeArguments(inferredType)) {
                return false;
            }
            return !ExpressionTypeHintHelper.canDisplayTypeArguments(creationType)
                    || ExpressionTypeHintHelper.isObjectOnlyGenericType(creationType);
        }
    }
}
