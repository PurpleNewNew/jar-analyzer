package org.benf.cfr.reader.bytecode;

import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;

final class InitialStructuringStage {
    private final StructureRecoveryPipeline structureRecoveryPipeline;

    InitialStructuringStage(StructureRecoveryPipeline structureRecoveryPipeline) {
        this.structureRecoveryPipeline = structureRecoveryPipeline;
    }

    void apply(Op04StructuredStatement block, MethodAnalysisContext context) {
        structureRecoveryPipeline.applyInitialCleanup(block, context);
    }
}
