package org.benf.cfr.reader.bytecode;

import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;

import java.util.List;

final class OutputPolishStage {
    private static final String PHASE = "output-stage";
    private static final String INPUT_REQUIREMENT = "fully-structured";
    private final List<StructuredSemanticTransforms.OutputPolishPlan> plans;

    OutputPolishStage(StructureRecoveryPipeline structureRecoveryPipeline) {
        this.plans = StructuredSemanticTransforms.outputPolishPlans(structureRecoveryPipeline);
    }

    void apply(Op04StructuredStatement block, MethodAnalysisContext context) {
        if (!context.capturesObservability()) {
            if (!block.isFullyStructured()) {
                return;
            }
            for (StructuredSemanticTransforms.OutputPolishPlan plan : plans) {
                plan.apply(block, context);
            }
            return;
        }
        StructureRecoverySnapshot before = StructureRecoverySnapshot.capture(block);
        if (!block.isFullyStructured()) {
            context.structureRecoveryTrace.recordSkippedPhase(
                    PHASE,
                    INPUT_REQUIREMENT,
                    before,
                    "input-requirement-not-met"
            );
            return;
        }
        StructureRecoveryTrace.PhaseTrace phaseTrace = context.structureRecoveryTrace.beginPhase(PHASE, INPUT_REQUIREMENT, before);
        phaseTrace.recordInvariant("input-requirement", true, "accepted " + INPUT_REQUIREMENT + " with " + before);
        StructureRecoveryTrace.RoundTrace roundTrace = phaseTrace.beginRound(1, before);
        for (StructuredSemanticTransforms.OutputPolishPlan plan : plans) {
            runPass(roundTrace, block, plan.pass(), () -> plan.apply(block, context));
        }
        StructureRecoverySnapshot after = StructureRecoverySnapshot.capture(block);
        boolean noStructuralDelta = !before.hasStructuralDelta(after);
        roundTrace.recordInvariant("no-structural-delta", noStructuralDelta, "before=" + before + ", after=" + after);
        roundTrace.finish(after);
        phaseTrace.recordInvariant("no-structural-delta", noStructuralDelta, "before=" + before + ", after=" + after);
        phaseTrace.finish(after);
    }

    private void runPass(StructureRecoveryTrace.RoundTrace roundTrace,
                         Op04StructuredStatement block,
                         StructuredPassEntry pass,
                         Runnable action) {
        StructureRecoverySnapshot passBefore = StructureRecoverySnapshot.capture(block);
        action.run();
        roundTrace.recordPass(pass, true, passBefore, StructureRecoverySnapshot.capture(block));
    }
}
