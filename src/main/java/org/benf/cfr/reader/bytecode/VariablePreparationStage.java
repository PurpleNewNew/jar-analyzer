package org.benf.cfr.reader.bytecode;

import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;

final class VariablePreparationStage {
    void apply(Op04StructuredStatement block, MethodAnalysisContext context) {
        if (!block.isFullyStructured()) {
            return;
        }
        StructureRecoverySnapshot before = StructureRecoverySnapshot.capture(block);
        StructuredSemanticTransforms.discoverVariableScopes(
                context.method,
                block,
                context.variableFactory,
                context.options,
                context.classFileVersion,
                context.bytecodeMeta
        );
        context.variableRecoveryTrace.record(
                VariableRecoveryPasses.DISCOVER_VARIABLE_SCOPES,
                before,
                StructureRecoverySnapshot.capture(block)
        );
    }
}
