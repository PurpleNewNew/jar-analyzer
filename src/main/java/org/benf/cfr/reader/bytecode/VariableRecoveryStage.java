package org.benf.cfr.reader.bytecode;

import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.StructuredConditionLocalRecoveryPasses;
import org.benf.cfr.reader.bytecode.analysis.opgraph.StructuredCreatorOrderingRecovery;
import org.benf.cfr.reader.bytecode.analysis.opgraph.StructuredEmbeddedAssignmentRecovery;

final class VariableRecoveryStage {
    void apply(Op04StructuredStatement block, MethodAnalysisContext context) {
        if (!block.isFullyStructured()) {
            return;
        }
        runPass(block, context, VariableRecoveryPasses.MARK_LAMBDA_CAPTURED_VARIABLES, () ->
                StructuredSemanticTransforms.markLambdaCapturedVariables(block));
        runPass(block, context, VariableRecoveryPasses.RESTORE_LIFTABLE_DEFINITION_ASSIGNMENTS, () ->
                StructuredConditionLocalRecoveryPasses.restoreLiftableDefinitionAssignments(block));
        runPass(block, context, VariableRecoveryPasses.SINK_DEFINITIONS_TO_FIRST_USE, () ->
                StructuredConditionLocalRecoveryPasses.sinkDefinitionsToFirstUse(block));
        runPass(block, context, VariableRecoveryPasses.RESTORE_CREATOR_DEPENDENCY_ORDER, () ->
                StructuredCreatorOrderingRecovery.restoreCreatorDependencyOrder(block));
        runPass(block, context, VariableRecoveryPasses.RESTORE_CREATORS_BEFORE_FIRST_USE, () ->
                StructuredCreatorOrderingRecovery.restoreCreatorsBeforeFirstUse(block));
        runPass(block, context, VariableRecoveryPasses.RESTORE_TRAILING_CREATOR_ASSIGNMENTS, () ->
                StructuredEmbeddedAssignmentRecovery.restoreTrailingCreatorAssignments(block));
        runPass(block, context, VariableRecoveryPasses.RESTORE_PREFIX_EMBEDDED_ASSIGNMENTS, () ->
                StructuredEmbeddedAssignmentRecovery.restorePrefixEmbeddedAssignments(block));
        runPass(block, context, VariableRecoveryPasses.REDUCE_CLASH_DECLARATIONS, () ->
                StructuredSemanticTransforms.reduceClashDeclarations(block, context.bytecodeMeta));
        runPass(block, context, VariableRecoveryPasses.DISCOVER_LOCAL_CLASS_SCOPES, () ->
                StructuredSemanticTransforms.discoverLocalClassScopes(
                        context.method,
                        block,
                        context.variableFactory,
                        context.options
                ));
    }

    private void runPass(Op04StructuredStatement block,
                         MethodAnalysisContext context,
                         StructuredPassEntry pass,
                         Runnable action) {
        StructureRecoverySnapshot before = StructureRecoverySnapshot.capture(block);
        action.run();
        context.variableRecoveryTrace.record(pass, before, StructureRecoverySnapshot.capture(block));
    }
}
