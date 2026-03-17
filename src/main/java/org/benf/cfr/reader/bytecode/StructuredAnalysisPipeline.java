package org.benf.cfr.reader.bytecode;

import org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.util.DecompilerComment;

import java.util.List;

final class StructuredAnalysisPipeline {
    private final InitialStructuringStage initialStructuringStage;
    private final ControlFlowRecoveryStage controlFlowRecoveryStage;
    private final ModernSemanticsStage modernSemanticsStage;
    private final OutputPolishStage outputPolishStage;

    StructuredAnalysisPipeline(MethodAnalysisContext context) {
        PatternSemanticsRewriter patternSemanticsRewriter = new PatternSemanticsRewriter(context.modernFeatures);
        StructureRecoveryPipeline structureRecoveryPipeline = new StructureRecoveryPipeline(patternSemanticsRewriter);
        this.initialStructuringStage = new InitialStructuringStage(structureRecoveryPipeline);
        this.controlFlowRecoveryStage = new ControlFlowRecoveryStage(structureRecoveryPipeline);
        this.modernSemanticsStage = new ModernSemanticsStage(structureRecoveryPipeline, patternSemanticsRewriter);
        this.outputPolishStage = new OutputPolishStage(structureRecoveryPipeline);
    }

    Op04StructuredStatement analyse(List<Op03SimpleStatement> op03SimpleParseNodes, MethodAnalysisContext context) {
        Op04StructuredStatement block = initialStructuringStage.build(op03SimpleParseNodes, context);
        controlFlowRecoveryStage.apply(block, context);
        if (!block.isFullyStructured()) {
            context.comments.addComment(DecompilerComment.UNABLE_TO_STRUCTURE);
            return block;
        }
        modernSemanticsStage.apply(block, context);
        if (!block.isFullyStructured()) {
            context.comments.addComment(DecompilerComment.UNABLE_TO_STRUCTURE);
            return block;
        }
        outputPolishStage.apply(block, context);
        return block;
    }
}
