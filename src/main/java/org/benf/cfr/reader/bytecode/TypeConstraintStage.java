package org.benf.cfr.reader.bytecode;

import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.LocalVariable;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredAssignment;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredReturn;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ExpressionTypeHintHelper;
import org.benf.cfr.reader.util.collections.ListFactory;

import java.util.List;

final class TypeConstraintStage {
    void apply(Op04StructuredStatement block, MethodAnalysisContext context) {
        if (!block.isFullyStructured()) {
            return;
        }
        List<StructuredStatement> statements = ListFactory.newList();
        block.linearizeStatementsInto(statements);
        for (StructuredStatement statement : statements) {
            if (!(statement instanceof StructuredAssignment)) {
                if (statement instanceof StructuredReturn) {
                    ((StructuredReturn) statement).applyTypeConstraints();
                }
                continue;
            }
            StructuredAssignment assignment = (StructuredAssignment) statement;
            assignment.applyTargetExpressionConstraints();
            propagateAssignmentConstraint(assignment, context);
        }
    }

    private void propagateAssignmentConstraint(StructuredAssignment assignment, MethodAnalysisContext context) {
        if (!(assignment.getLvalue() instanceof LocalVariable)) {
            return;
        }
        LocalVariable localVariable = (LocalVariable) assignment.getLvalue();
        StructuredAssignment.TypeConstraintEffect effect = assignment.applyTypeConstraints();
        context.typeRecoveryTrace.record(
                TypeRecoveryPasses.VARIABLE_ASSIGNMENT_CONSTRAINT,
                "LocalVariable",
                ExpressionTypeHintHelper.describeType(effect.getDisplayType()),
                ExpressionTypeHintHelper.describeType(effect.getBeforeType()),
                ExpressionTypeHintHelper.describeType(effect.getAfterType()),
                "local=" + describeLocal(localVariable)
                        + ", creator=" + effect.isCreator()
                        + ", creatorTargetApplied=" + effect.isCreatorTargetApplied()
                        + ", beforeCreation=" + ExpressionTypeHintHelper.describeType(effect.getBeforeCreationType())
                        + ", afterCreation=" + ExpressionTypeHintHelper.describeType(effect.getAfterCreationType())
        );
    }

    private String describeLocal(LocalVariable localVariable) {
        return localVariable.getName().getStringName();
    }
}
