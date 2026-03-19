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
    private static final String TYPE_CONSTRAINT_STAGE = "type-constraint";
    private static final String FULLY_STRUCTURED_INPUT = "fully-structured";
    private static final String OUTPUT_STAGE = "output-stage";

    private final InitialStructuringStage initialStructuringStage;
    private final ControlFlowRecoveryStage controlFlowRecoveryStage;
    private final VariablePreparationStage variablePreparationStage;
    private final ModernSemanticsStage modernSemanticsStage;
    private final VariableRecoveryStage variableRecoveryStage;
    private final TypeConstraintStage typeConstraintStage;
    private final OutputPolishStage outputPolishStage;
    private final MethodStage variablePreparationMethodStage;
    private final MethodStage modernSemanticsMethodStage;
    private final MethodStage variableRecoveryMethodStage;
    private final MethodStage typeConstraintMethodStage;
    private final MethodStage outputMethodStage;

    MethodDecompilePipeline(MethodAnalysisContext context) {
        PatternSemanticsRewriter patternSemanticsRewriter = new PatternSemanticsRewriter(context.modernFeatures);
        StructureRecoveryPipeline structureRecoveryPipeline = new StructureRecoveryPipeline(patternSemanticsRewriter);
        this.initialStructuringStage = new InitialStructuringStage(structureRecoveryPipeline);
        this.controlFlowRecoveryStage = new ControlFlowRecoveryStage(structureRecoveryPipeline);
        this.variablePreparationStage = new VariablePreparationStage();
        this.modernSemanticsStage = new ModernSemanticsStage(structureRecoveryPipeline, patternSemanticsRewriter);
        this.variableRecoveryStage = new VariableRecoveryStage();
        this.typeConstraintStage = new TypeConstraintStage();
        this.outputPolishStage = new OutputPolishStage(structureRecoveryPipeline);
        this.variablePreparationMethodStage = new MethodStage(VARIABLE_PREPARATION_STAGE, FULLY_STRUCTURED_INPUT) {
            @Override
            void apply(Op04StructuredStatement block, MethodAnalysisContext context) {
                variablePreparationStage.apply(block, context);
            }
        };
        this.modernSemanticsMethodStage = new MethodStage(MODERN_SEMANTICS_STAGE, FULLY_STRUCTURED_INPUT) {
            @Override
            void apply(Op04StructuredStatement block, MethodAnalysisContext context) {
                modernSemanticsStage.apply(block, context);
            }
        };
        this.variableRecoveryMethodStage = new MethodStage(VariableRecoveryStage.phaseName(), VariableRecoveryStage.inputRequirement()) {
            @Override
            void apply(Op04StructuredStatement block, MethodAnalysisContext context) {
                variableRecoveryStage.apply(block, context);
            }
        };
        this.typeConstraintMethodStage = new MethodStage(TYPE_CONSTRAINT_STAGE, TypeConstraintStage.inputRequirement()) {
            @Override
            void apply(Op04StructuredStatement block, MethodAnalysisContext context) {
                typeConstraintStage.apply(block, context);
            }
        };
        this.outputMethodStage = new MethodStage(OUTPUT_STAGE, FULLY_STRUCTURED_INPUT) {
            @Override
            void apply(Op04StructuredStatement block, MethodAnalysisContext context) {
                outputPolishStage.apply(block, context);
            }
        };
    }

    Op04StructuredStatement analyse(List<Op03SimpleStatement> op03SimpleParseNodes, MethodAnalysisContext context) {
        Op04StructuredStatement block = Op03SimpleStatement.createInitialStructuredBlock(op03SimpleParseNodes);
        runStage(INITIAL_STAGE, INITIAL_INPUT, block, context, () -> initialStructuringStage.apply(block, context));
        runStage(CONTROL_FLOW_STAGE, CONTROL_FLOW_INPUT, block, context, () -> controlFlowRecoveryStage.apply(block, context));
        if (!block.isFullyStructured()) {
            return abortRemaining(block, context,
                    variablePreparationMethodStage,
                    modernSemanticsMethodStage,
                    variableRecoveryMethodStage,
                    typeConstraintMethodStage,
                    outputMethodStage);
        }
        if (!runStageRequiringStructure(variablePreparationMethodStage, block, context)) {
            return abortRemaining(block, context,
                    modernSemanticsMethodStage,
                    variableRecoveryMethodStage,
                    typeConstraintMethodStage,
                    outputMethodStage);
        }
        if (!runStageRequiringStructure(modernSemanticsMethodStage, block, context)) {
            return abortRemaining(block, context,
                    variableRecoveryMethodStage,
                    typeConstraintMethodStage,
                    outputMethodStage);
        }
        if (!runStageRequiringStructure(variableRecoveryMethodStage, block, context)) {
            return abortRemaining(block, context,
                    typeConstraintMethodStage,
                    outputMethodStage);
        }
        if (!runStageRequiringStructure(typeConstraintMethodStage, block, context)) {
            return abortRemaining(block, context, outputMethodStage);
        }
        runStageRequiringStructure(outputMethodStage, block, context);
        return block;
    }

    private boolean runStageRequiringStructure(MethodStage methodStage,
                                               Op04StructuredStatement block,
                                               MethodAnalysisContext context) {
        if (!block.isFullyStructured()) {
            skipStage(methodStage.stage(), methodStage.inputRequirement(), block, context, "input-requirement-not-met");
            return false;
        }
        runStage(methodStage.stage(), methodStage.inputRequirement(), block, context, () -> methodStage.apply(block, context));
        return block.isFullyStructured();
    }

    private Op04StructuredStatement abortRemaining(Op04StructuredStatement block,
                                                   MethodAnalysisContext context,
                                                   MethodStage... remainingStages) {
        for (MethodStage remainingStage : remainingStages) {
            skipStage(remainingStage.stage(), remainingStage.inputRequirement(), block, context, "input-requirement-not-met");
        }
        context.comments.addComment(DecompilerComment.UNABLE_TO_STRUCTURE);
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

    private abstract static class MethodStage {
        private final String stage;
        private final String inputRequirement;

        private MethodStage(String stage, String inputRequirement) {
            this.stage = stage;
            this.inputRequirement = inputRequirement;
        }

        private String stage() {
            return stage;
        }

        private String inputRequirement() {
            return inputRequirement;
        }

        abstract void apply(Op04StructuredStatement block, MethodAnalysisContext context);
    }
}
