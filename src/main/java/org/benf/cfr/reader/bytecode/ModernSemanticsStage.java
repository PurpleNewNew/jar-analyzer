package org.benf.cfr.reader.bytecode;

import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;

import java.util.List;

final class ModernSemanticsStage {
    private final List<StructuredSemanticTransforms.ModernSemanticPlan> plans;

    ModernSemanticsStage(StructureRecoveryPipeline structureRecoveryPipeline,
                         PatternSemanticsRewriter patternSemanticsRewriter) {
        this.plans = StructuredSemanticTransforms.modernSemanticPlans(structureRecoveryPipeline, patternSemanticsRewriter);
    }

    void apply(Op04StructuredStatement block, MethodAnalysisContext context) {
        if (!block.isFullyStructured()) {
            return;
        }
        for (StructuredSemanticTransforms.ModernSemanticPlan plan : plans) {
            if (!plan.enabled(context)) {
                continue;
            }
            plan.apply(block, context);
        }
    }
}
