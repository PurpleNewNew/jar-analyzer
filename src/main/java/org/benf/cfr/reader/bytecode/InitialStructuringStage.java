package org.benf.cfr.reader.bytecode;

import org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;

import java.util.List;

final class InitialStructuringStage {
    private final StructureRecoveryPipeline structureRecoveryPipeline;

    InitialStructuringStage(StructureRecoveryPipeline structureRecoveryPipeline) {
        this.structureRecoveryPipeline = structureRecoveryPipeline;
    }

    Op04StructuredStatement build(List<Op03SimpleStatement> op03SimpleParseNodes, MethodAnalysisContext context) {
        Op04StructuredStatement block = Op03SimpleStatement.createInitialStructuredBlock(op03SimpleParseNodes);
        structureRecoveryPipeline.applyInitialCleanup(block, context);
        return block;
    }
}
