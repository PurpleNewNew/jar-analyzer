package org.benf.cfr.reader.bytecode.analysis.opgraph;

public final class StructuredLocalVariableRecovery {
    private StructuredLocalVariableRecovery() {
    }

    public static void rewriteConditionLocalAliases(Op04StructuredStatement root) {
        StructuredConditionLocalRecovery.rewriteConditionLocalAliases(root);
    }

    public static void restoreLiftableDefinitionAssignments(Op04StructuredStatement root) {
        StructuredConditionLocalRecovery.restoreLiftableDefinitionAssignments(root);
    }

    public static void restoreCreatorDependencyOrder(Op04StructuredStatement root) {
        StructuredCreatorLocalRecovery.restoreCreatorDependencyOrder(root);
    }

    public static void restoreCreatorsBeforeFirstUse(Op04StructuredStatement root) {
        StructuredCreatorLocalRecovery.restoreCreatorsBeforeFirstUse(root);
    }

    public static void restoreTrailingCreatorAssignments(Op04StructuredStatement root) {
        StructuredCreatorLocalRecovery.restoreTrailingCreatorAssignments(root);
    }

    public static void restorePrefixEmbeddedAssignments(Op04StructuredStatement root) {
        StructuredCreatorLocalRecovery.restorePrefixEmbeddedAssignments(root);
    }

    public static void sinkDefinitionsToFirstUse(Op04StructuredStatement root) {
        StructuredConditionLocalRecovery.sinkDefinitionsToFirstUse(root);
    }

    public static void applyOutputRecovery(Op04StructuredStatement root) {
        sinkDefinitionsToFirstUse(root);
        restoreCreatorDependencyOrder(root);
        restoreCreatorsBeforeFirstUse(root);
        restoreLiftableDefinitionAssignments(root);
        restoreTrailingCreatorAssignments(root);
        restorePrefixEmbeddedAssignments(root);
    }
}
