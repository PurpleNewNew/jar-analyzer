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
    private static final String FULLY_STRUCTURED_INPUT = "fully-structured";
    private static final String OUTPUT_STAGE = "output-stage";

    private final InitialStructuringStage initialStructuringStage;
    private final ControlFlowRecoveryStage controlFlowRecoveryStage;
    private final VariablePreparationStage variablePreparationStage;
    private final ModernSemanticsStage modernSemanticsStage;
    private final VariableRecoveryStage variableRecoveryStage;
    private final OutputPolishStage outputPolishStage;

    MethodDecompilePipeline(MethodAnalysisContext context) {
        PatternSemanticsRewriter patternSemanticsRewriter = new PatternSemanticsRewriter(context.modernFeatures);
        StructureRecoveryPipeline structureRecoveryPipeline = new StructureRecoveryPipeline(patternSemanticsRewriter);
        this.initialStructuringStage = new InitialStructuringStage(structureRecoveryPipeline);
        this.controlFlowRecoveryStage = new ControlFlowRecoveryStage(structureRecoveryPipeline);
        this.variablePreparationStage = new VariablePreparationStage();
        this.modernSemanticsStage = new ModernSemanticsStage(structureRecoveryPipeline, patternSemanticsRewriter);
        this.variableRecoveryStage = new VariableRecoveryStage();
        this.outputPolishStage = new OutputPolishStage(structureRecoveryPipeline);
    }

    Op04StructuredStatement analyse(List<Op03SimpleStatement> op03SimpleParseNodes, MethodAnalysisContext context) {
        Op04StructuredStatement block = Op03SimpleStatement.createInitialStructuredBlock(op03SimpleParseNodes);
        runStage(INITIAL_STAGE, INITIAL_INPUT, block, context, () -> initialStructuringStage.apply(block, context));
        runStage(CONTROL_FLOW_STAGE, CONTROL_FLOW_INPUT, block, context, () -> controlFlowRecoveryStage.apply(block, context));
        if (!block.isFullyStructured()) {
            skipStage(VARIABLE_PREPARATION_STAGE, FULLY_STRUCTURED_INPUT, block, context, "input-requirement-not-met");
            skipStage(MODERN_SEMANTICS_STAGE, FULLY_STRUCTURED_INPUT, block, context, "input-requirement-not-met");
            skipStage(VariableRecoveryStage.phaseName(), VariableRecoveryStage.inputRequirement(), block, context, "input-requirement-not-met");
            skipStage(OUTPUT_STAGE, FULLY_STRUCTURED_INPUT, block, context, "input-requirement-not-met");
            context.comments.addComment(DecompilerComment.UNABLE_TO_STRUCTURE);
            return block;
        }
        runStage(VARIABLE_PREPARATION_STAGE, FULLY_STRUCTURED_INPUT, block, context, () -> variablePreparationStage.apply(block, context));
        runStage(MODERN_SEMANTICS_STAGE, FULLY_STRUCTURED_INPUT, block, context, () -> modernSemanticsStage.apply(block, context));
        if (!block.isFullyStructured()) {
            skipStage(VariableRecoveryStage.phaseName(), VariableRecoveryStage.inputRequirement(), block, context, "input-requirement-not-met");
            skipStage(OUTPUT_STAGE, FULLY_STRUCTURED_INPUT, block, context, "input-requirement-not-met");
            context.comments.addComment(DecompilerComment.UNABLE_TO_STRUCTURE);
            return block;
        }
        runStage(VariableRecoveryStage.phaseName(), VariableRecoveryStage.inputRequirement(), block, context, () -> variableRecoveryStage.apply(block, context));
        if (!block.isFullyStructured()) {
            skipStage(OUTPUT_STAGE, FULLY_STRUCTURED_INPUT, block, context, "input-requirement-not-met");
            context.comments.addComment(DecompilerComment.UNABLE_TO_STRUCTURE);
            return block;
        }
        runStage(OUTPUT_STAGE, FULLY_STRUCTURED_INPUT, block, context, () -> outputPolishStage.apply(block, context));
        return block;
    }

    private void runStage(String stage,
                          String inputRequirement,
                          Op04StructuredStatement block,
                          MethodAnalysisContext context,
                          Runnable action) {
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
}
