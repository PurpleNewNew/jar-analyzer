package org.benf.cfr.reader.bytecode.analysis.opgraph;

import org.benf.cfr.reader.bytecode.StructuredPassDescriptor;
import org.benf.cfr.reader.bytecode.StructuredPassEntry;
import org.benf.cfr.reader.bytecode.VariableRecoveryPasses;

import java.util.List;

public final class StructuredLocalVariableRecovery {
    private StructuredLocalVariableRecovery() {
    }

    public static List<StructuredPassEntry> describePasses() {
        return List.of(
                entry("rewrite-condition-local-aliases", "structure-recovery", "any-structure-state", "Normalizes condition-local aliases before structure cleanup.", true, true),
                VariableRecoveryPasses.RESTORE_LIFTABLE_DEFINITION_ASSIGNMENTS,
                VariableRecoveryPasses.RESTORE_CREATOR_DEPENDENCY_ORDER,
                VariableRecoveryPasses.RESTORE_CREATORS_BEFORE_FIRST_USE,
                VariableRecoveryPasses.RESTORE_TRAILING_CREATOR_ASSIGNMENTS,
                VariableRecoveryPasses.RESTORE_PREFIX_EMBEDDED_ASSIGNMENTS,
                VariableRecoveryPasses.SINK_DEFINITIONS_TO_FIRST_USE
        );
    }

    public static void rewriteConditionLocalAliases(Op04StructuredStatement root) {
        StructuredConditionLocalRecovery.rewriteConditionLocalAliases(root);
    }

    public static void restoreLiftableDefinitionAssignments(Op04StructuredStatement root) {
        StructuredConditionLocalRecoveryPasses.restoreLiftableDefinitionAssignments(root);
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
        StructuredConditionLocalRecoveryPasses.sinkDefinitionsToFirstUse(root);
    }

    private static StructuredPassEntry entry(String name,
                                             String stage,
                                             String inputRequirement,
                                             String outputPromise,
                                             boolean idempotent,
                                             boolean allowsStructuralChange,
                                             String... dependencies) {
        return StructuredPassEntry.of(
                "local-variable-recovery",
                stage,
                inputRequirement,
                StructuredPassDescriptor.of(name, outputPromise, idempotent, allowsStructuralChange, dependencies)
        );
    }
}
