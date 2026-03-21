package org.benf.cfr.reader.bytecode;

import org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.util.DecompilerComment;

import java.util.List;

final class MethodDecompilePipeline {
    private static final String INITIAL_STAGE = "initial-structuring";
    private static final String INITIAL_INPUT = "op03-ready";
    private static final String CONTROL_FLOW_STAGE = "control-flow-recovery";
    private static final String CONTROL_FLOW_INPUT = "any-structure-state";
    private static final String VARIABLE_PREPARATION_STAGE = "variable-preparation";
    private static final String MODERN_SEMANTICS_STAGE = "modern-semantics";
    private static final String VARIABLE_RECOVERY_STAGE = "variable-recovery";
    private static final String TYPE_CONSTRAINT_STAGE = "type-constraint";
    private static final String FULLY_STRUCTURED_INPUT = "fully-structured";
    private static final String OUTPUT_STAGE = "output-stage";

    private final List<PipelineStagePlan> stages;

    MethodDecompilePipeline(MethodAnalysisContext context) {
        PatternSemanticsRewriter patternSemanticsRewriter = new PatternSemanticsRewriter(context.modernFeatures);
        StructureRecoveryPipeline structureRecoveryPipeline = new StructureRecoveryPipeline(patternSemanticsRewriter);
        InitialStructuringStage initialStructuringStage = new InitialStructuringStage(structureRecoveryPipeline);
        ControlFlowRecoveryStage controlFlowRecoveryStage = new ControlFlowRecoveryStage(structureRecoveryPipeline);
        VariablePreparationStage variablePreparationStage = new VariablePreparationStage();
        ModernSemanticsStage modernSemanticsStage = new ModernSemanticsStage(structureRecoveryPipeline, patternSemanticsRewriter);
        VariableRecoveryStage variableRecoveryStage = new VariableRecoveryStage();
        TypeConstraintStage typeConstraintStage = new TypeConstraintStage();
        OutputPolishStage outputPolishStage = new OutputPolishStage(structureRecoveryPipeline);
        this.stages = List.of(
                stage(INITIAL_STAGE, INITIAL_INPUT, false, false,
                        (block, analysisContext) -> initialStructuringStage.apply(block, analysisContext)),
                stage(CONTROL_FLOW_STAGE, CONTROL_FLOW_INPUT, false, true,
                        (block, analysisContext) -> controlFlowRecoveryStage.apply(block, analysisContext)),
                stage(VARIABLE_PREPARATION_STAGE, FULLY_STRUCTURED_INPUT, true, true,
                        (block, analysisContext) -> variablePreparationStage.apply(block, analysisContext)),
                stage(MODERN_SEMANTICS_STAGE, FULLY_STRUCTURED_INPUT, true, true,
                        (block, analysisContext) -> modernSemanticsStage.apply(block, analysisContext)),
                stage(VARIABLE_RECOVERY_STAGE, FULLY_STRUCTURED_INPUT, true, true,
                        (block, analysisContext) -> variableRecoveryStage.apply(block, analysisContext)),
                stage(TYPE_CONSTRAINT_STAGE, FULLY_STRUCTURED_INPUT, true, true,
                        (block, analysisContext) -> typeConstraintStage.apply(block, analysisContext)),
                stage(OUTPUT_STAGE, FULLY_STRUCTURED_INPUT, true, false,
                        (block, analysisContext) -> outputPolishStage.apply(block, analysisContext))
        );
    }

    Op04StructuredStatement analyse(List<Op03SimpleStatement> op03SimpleParseNodes, MethodAnalysisContext context) {
        Op04StructuredStatement block = Op03SimpleStatement.createInitialStructuredBlock(op03SimpleParseNodes);
        for (int idx = 0; idx < stages.size(); ++idx) {
            PipelineStagePlan stage = stages.get(idx);
            if (stage.requiresStructuredInput() && !block.isFullyStructured()) {
                return abortRemaining(block, context, idx, false);
            }
            runStage(stage.stage(), stage.inputRequirement(), block, context,
                    () -> stage.apply(block, context));
            if (stage.abortRemainingWhenUnstructuredAfter() && !block.isFullyStructured()) {
                return abortRemaining(block, context, idx + 1, true);
            }
        }
        return block;
    }

    private Op04StructuredStatement abortRemaining(Op04StructuredStatement block,
                                                   MethodAnalysisContext context,
                                                   int startIndex,
                                                   boolean addUnableToStructureComment) {
        for (int idx = startIndex; idx < stages.size(); ++idx) {
            PipelineStagePlan remainingStage = stages.get(idx);
            skipStage(remainingStage.stage(), remainingStage.inputRequirement(), block, context, "input-requirement-not-met");
        }
        if (addUnableToStructureComment) {
            context.comments.addComment(DecompilerComment.UNABLE_TO_STRUCTURE);
        }
        return block;
    }

    private void runStage(String stage,
                          String inputRequirement,
                          Op04StructuredStatement block,
                          MethodAnalysisContext context,
                          Runnable action) {
        if (!context.capturesObservability()) {
            action.run();
            return;
        }
        StructureRecoverySnapshot before = StructureRecoverySnapshot.capture(block);
        int beforeStructurePhaseCount = context.structureRecoveryTrace.getPhases().size();
        int beforeVariablePassCount = context.variableRecoveryTrace.getPasses().size();
        int beforeTypePassCount = context.typeRecoveryTrace.getPasses().size();
        action.run();
        context.methodDecompileRecord.recordStage(
                stage,
                inputRequirement,
                before,
                StructureRecoverySnapshot.capture(block),
                beforeStructurePhaseCount,
                context.structureRecoveryTrace.getPhases().size(),
                beforeVariablePassCount,
                context.variableRecoveryTrace.getPasses().size(),
                beforeTypePassCount,
                context.typeRecoveryTrace.getPasses().size(),
                context.structureRecoveryTrace,
                context.variableRecoveryTrace,
                context.typeRecoveryTrace
        );
    }

    private void skipStage(String stage,
                           String inputRequirement,
                           Op04StructuredStatement block,
                           MethodAnalysisContext context,
                           String reason) {
        if (!context.capturesObservability()) {
            return;
        }
        context.methodDecompileRecord.recordSkippedStage(
                stage,
                inputRequirement,
                StructureRecoverySnapshot.capture(block),
                context.structureRecoveryTrace.getPhases().size(),
                context.variableRecoveryTrace.getPasses().size(),
                context.typeRecoveryTrace.getPasses().size(),
                reason
        );
    }

    private static PipelineStagePlan stage(String stage,
                                           String inputRequirement,
                                           boolean requiresStructuredInput,
                                           boolean abortRemainingWhenUnstructuredAfter,
                                           PipelineStageAction action) {
        return new PipelineStagePlan(stage, inputRequirement, requiresStructuredInput, abortRemainingWhenUnstructuredAfter, action);
    }

    @FunctionalInterface
    private interface PipelineStageAction {
        void apply(Op04StructuredStatement block, MethodAnalysisContext context);
    }

    private record PipelineStagePlan(String stage,
                                     String inputRequirement,
                                     boolean requiresStructuredInput,
                                     boolean abortRemainingWhenUnstructuredAfter,
                                     PipelineStageAction action) {
        void apply(Op04StructuredStatement block, MethodAnalysisContext context) {
            action.apply(block, context);
        }
    }
}
