package org.benf.cfr.reader.bytecode;

import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;

final class VariableRecoveryStage {
    void apply(Op04StructuredStatement block, MethodAnalysisContext context) {
        if (!block.isFullyStructured()) {
            return;
        }
        for (VariableRecoveryPasses.VariablePassPlan recoveryPlan : VariableRecoveryPasses.recoveryPlans()) {
            runPass(block, context, recoveryPlan.pass(), () -> recoveryPlan.apply(block, context));
        }
    }

    private void runPass(Op04StructuredStatement block,
                         MethodAnalysisContext context,
                         StructuredPassEntry pass,
                         Runnable action) {
        StructureRecoverySnapshot before = StructureRecoverySnapshot.capture(block);
        action.run();
        context.variableRecoveryTrace.record(pass, before, StructureRecoverySnapshot.capture(block));
    }
}
