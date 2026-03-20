package org.benf.cfr.reader.bytecode;

import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
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
            for (TypeRecoveryPasses.TypeConstraintPlan constraintPlan : TypeRecoveryPasses.constraintPlans()) {
                if (!constraintPlan.supports(statement)) {
                    continue;
                }
                constraintPlan.apply(statement, context);
            }
        }
    }
}
