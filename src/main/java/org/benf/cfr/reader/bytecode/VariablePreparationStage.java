package org.benf.cfr.reader.bytecode;

import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;

final class VariablePreparationStage {
    void apply(Op04StructuredStatement block, MethodAnalysisContext context) {
        if (!block.isFullyStructured()) {
            return;
        }
        for (VariableRecoveryPasses.VariablePassPlan preparationPlan : VariableRecoveryPasses.preparationPlans()) {
            if (!context.capturesObservability()) {
                preparationPlan.apply(block, context);
                continue;
            }
            StructureRecoverySnapshot before = StructureRecoverySnapshot.capture(block);
            preparationPlan.apply(block, context);
            context.variableRecoveryTrace.record(
                    preparationPlan.pass(),
                    before,
                    StructureRecoverySnapshot.capture(block)
            );
        }
    }
}
