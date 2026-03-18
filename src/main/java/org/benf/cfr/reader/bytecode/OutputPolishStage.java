package org.benf.cfr.reader.bytecode;

import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.checker.IllegalReturnChecker;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.checker.LooseCatchChecker;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.checker.VoidVariableChecker;
import org.benf.cfr.reader.util.getopt.OptionsImpl;

final class OutputPolishStage {
    private static final String PHASE = "output-stage";
    private static final String INPUT_REQUIREMENT = "fully-structured";
    private static final StructuredPassEntry EXPRESSION_POLISH_PASS = StructuredPassEntry.of(
            "output-stage",
            PHASE,
            INPUT_REQUIREMENT,
            StructuredPassDescriptor.of(
                    "expression-polish-stage",
                    "Applies expression-only output polish after structural and semantic recovery are complete.",
                    true,
                    false
            )
    );
    private static final StructuredPassEntry VALIDATION_METADATA_PASS = StructuredPassEntry.of(
            "output-stage",
            PHASE,
            INPUT_REQUIREMENT,
            StructuredPassDescriptor.of(
                    "validation-and-metadata-stage",
                    "Runs output validation and metadata annotation without changing structure.",
                    true,
                    false
            )
    );

    private final StructureRecoveryPipeline structureRecoveryPipeline;

    OutputPolishStage(StructureRecoveryPipeline structureRecoveryPipeline) {
        this.structureRecoveryPipeline = structureRecoveryPipeline;
    }

    void apply(Op04StructuredStatement block, MethodAnalysisContext context) {
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
        runPass(roundTrace, block, EXPRESSION_POLISH_PASS, () -> applyExpressionPolish(block, context));
        runPass(roundTrace, block, VALIDATION_METADATA_PASS, () -> applyValidationAndMetadata(block, context));
        StructureRecoverySnapshot after = StructureRecoverySnapshot.capture(block);
        boolean noStructuralDelta = !before.hasStructuralDelta(after);
        roundTrace.recordInvariant("no-structural-delta", noStructuralDelta, "before=" + before + ", after=" + after);
        roundTrace.finish(after);
        phaseTrace.recordInvariant("no-structural-delta", noStructuralDelta, "before=" + before + ", after=" + after);
        phaseTrace.finish(after);
    }

    private void applyExpressionPolish(Op04StructuredStatement block, MethodAnalysisContext context) {
        if (context.options.getOption(OptionsImpl.REMOVE_BOILERPLATE) && context.method.isConstructor()) {
            StructuredOutputTransforms.removeConstructorBoilerplate(block);
        }
        StructuredOutputTransforms.removeUnnecessaryVarargArrays(block);
        StructuredOutputTransforms.rewriteBadCastChains(block);
        StructuredOutputTransforms.rewriteNarrowingAssignments(block);
        StructuredSemanticTransforms.tidyVariableNames(
                context.method,
                block,
                context.bytecodeMeta,
                context.comments,
                context.constantPool.getClassCache(),
                context.modernFeatures
        );
        StructuredOutputTransforms.tidyObfuscation(context.options, block);
        StructuredOutputTransforms.miscKeyholeTransforms(context.variableFactory, block);
        structureRecoveryPipeline.applyOutputPolish(block, context);
    }

    private void applyValidationAndMetadata(Op04StructuredStatement block, MethodAnalysisContext context) {
        StructuredOutputTransforms.applyChecker(new LooseCatchChecker(), block, context.comments);
        StructuredOutputTransforms.applyChecker(new VoidVariableChecker(), block, context.comments);
        StructuredOutputTransforms.applyChecker(new IllegalReturnChecker(), block, context.comments);
        StructuredSemanticTransforms.applyLocalVariableMetadata(
                context.originalCodeAttribute,
                block,
                context.lutByOffset,
                context.comments
        );
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
