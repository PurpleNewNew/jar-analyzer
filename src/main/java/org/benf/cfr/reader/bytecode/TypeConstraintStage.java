package org.benf.cfr.reader.bytecode;

import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.LocalVariable;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredAssignment;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ExpressionTypeHintHelper;
import org.benf.cfr.reader.util.collections.ListFactory;

import java.util.List;

final class TypeConstraintStage {
    private static final String PHASE = "type-constraint";
    private static final String INPUT_REQUIREMENT = "fully-structured";

    void apply(Op04StructuredStatement block, MethodAnalysisContext context) {
        if (!block.isFullyStructured()) {
            return;
        }
        List<StructuredStatement> statements = ListFactory.newList();
        block.linearizeStatementsInto(statements);
        for (StructuredStatement statement : statements) {
            if (!(statement instanceof StructuredAssignment)) {
                continue;
            }
            propagateAssignmentConstraint((StructuredAssignment) statement, context);
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

    static String phaseName() {
        return PHASE;
    }

    static String inputRequirement() {
        return INPUT_REQUIREMENT;
    }
}
