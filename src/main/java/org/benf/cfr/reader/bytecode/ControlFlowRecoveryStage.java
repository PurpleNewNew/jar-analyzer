package org.benf.cfr.reader.bytecode;

import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.SwitchEnumRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.SwitchStringRewriter;

final class ControlFlowRecoveryStage {
    private final StructureRecoveryPipeline structureRecoveryPipeline;

    ControlFlowRecoveryStage(StructureRecoveryPipeline structureRecoveryPipeline) {
        this.structureRecoveryPipeline = structureRecoveryPipeline;
    }

    void apply(Op04StructuredStatement block, MethodAnalysisContext context) {
        if (!block.isFullyStructured()) {
            structureRecoveryPipeline.recoverToFixedPoint(block, context);
        }
        if (!block.isFullyStructured()) {
            return;
        }
        Op04StructuredStatement.tidyTypedBooleans(block);
        Op04StructuredStatement.prettifyBadLoops(block);
        new SwitchStringRewriter(context.options, context.classFileVersion, context.bytecodeMeta).rewrite(block);
        new SwitchEnumRewriter(context.commonState, context.classFile, context.blockIdentifierFactory).rewrite(block);
    }
}
