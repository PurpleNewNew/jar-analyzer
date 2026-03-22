package org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers;

import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.AssignmentExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.CastExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.LocalVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.AbstractExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.RawJavaType;
import org.benf.cfr.reader.bytecode.analysis.types.TypeConstants;

public class EmbeddedAssignmentTypeStabilizer extends AbstractExpressionRewriter {
    @Override
    public Expression rewriteExpression(Expression expression,
                                        SSAIdentifiers ssaIdentifiers,
                                        StatementContainer statementContainer,
                                        ExpressionRewriterFlags flags) {
        if (expression instanceof AssignmentExpression) {
            stabilize((AssignmentExpression) expression);
        }
        return super.rewriteExpression(expression, ssaIdentifiers, statementContainer, flags);
    }

    private void stabilize(AssignmentExpression assignmentExpression) {
        if (!(assignmentExpression.getUpdatedLValue() instanceof LocalVariable)) {
            return;
        }
        Expression rValue = assignmentExpression.getrValue();
        if (!(rValue instanceof CastExpression)) {
            return;
        }
        LocalVariable localVariable = (LocalVariable) assignmentExpression.getUpdatedLValue();
        JavaTypeInstance currentType = localVariable.getInferredJavaType().getJavaTypeInstance();
        JavaTypeInstance castType = ((CastExpression) rValue).getInferredJavaType().getJavaTypeInstance();
        JavaTypeInstance witnessedType = localVariable.getEmbeddedReferenceAssignmentType();
        JavaTypeInstance declarationType = localVariable.getCustomCreationJavaType();
        JavaTypeInstance conflictProbeType = chooseConflictProbeType(witnessedType, declarationType, currentType);
        if (castType == null
                || castType == TypeConstants.OBJECT
                || castType instanceof RawJavaType) {
            return;
        }
        if (conflictProbeType == null
                || conflictProbeType == TypeConstants.OBJECT
                || conflictProbeType instanceof RawJavaType) {
            localVariable.recordEmbeddedReferenceAssignmentType(castType);
            return;
        }
        JavaTypeInstance currentBaseType = conflictProbeType.getDeGenerifiedType();
        JavaTypeInstance castBaseType = castType.getDeGenerifiedType();
        if (currentBaseType == null
                || castBaseType == null
                || currentBaseType.equals(castBaseType)
                || conflictProbeType.implicitlyCastsTo(castType, null)
                || castType.implicitlyCastsTo(conflictProbeType, null)) {
            localVariable.recordEmbeddedReferenceAssignmentType(conflictProbeType);
            return;
        }
        // Embedded assignments inside loop/condition scaffolding can reuse one recovered local across
        // unrelated reference types. This can still happen after the inferred type has already widened
        // to Object, because creator/LVT metadata may remember the first concrete slot type. Once we
        // see an incompatible second concrete reference, keep the declaration pinned to Object.
        localVariable.getInferredJavaType().forceType(TypeConstants.OBJECT, true);
        localVariable.markConflictingReferenceDeclaration();
        localVariable.forceCustomCreationJavaType(TypeConstants.OBJECT);
        localVariable.setCustomCreationType(TypeConstants.OBJECT.getAnnotatedInstance());
    }

    private JavaTypeInstance chooseConflictProbeType(JavaTypeInstance witnessedType,
                                                     JavaTypeInstance declarationType,
                                                     JavaTypeInstance currentType) {
        if (witnessedType != null
                && witnessedType != TypeConstants.OBJECT
                && !(witnessedType instanceof RawJavaType)) {
            return witnessedType;
        }
        if (declarationType != null
                && declarationType != TypeConstants.OBJECT
                && !(declarationType instanceof RawJavaType)) {
            return declarationType;
        }
        return currentType;
    }
}
