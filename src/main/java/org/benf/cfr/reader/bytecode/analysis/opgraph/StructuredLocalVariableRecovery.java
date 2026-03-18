package org.benf.cfr.reader.bytecode.analysis.opgraph;

import org.benf.cfr.reader.bytecode.StructuredPassDescriptor;
import org.benf.cfr.reader.bytecode.StructuredPassEntry;

import java.util.List;

public final class StructuredLocalVariableRecovery {
    private StructuredLocalVariableRecovery() {
    }

    public static List<StructuredPassEntry> describePasses() {
        return List.of(
                entry("rewrite-condition-local-aliases", "structure-recovery", "any-structure-state", "Normalizes condition-local aliases before structure cleanup.", true, true),
                entry("restore-liftable-definition-assignments", "modern-semantics", "fully-structured", "Lifts definition assignments back to the declaration/use boundary.", true, true),
                entry("restore-creator-dependency-order", "modern-semantics.local-recovery", "fully-structured", "Restores creator ordering when constructor dependencies were emitted out of order.", true, true),
                entry("restore-creators-before-first-use", "modern-semantics.local-recovery", "fully-structured", "Moves creator assignments back before the first semantic use.", true, true),
                entry("restore-trailing-creator-assignments", "modern-semantics.local-recovery", "fully-structured", "Pulls trailing creator assignments back into the declaration region.", true, true),
                entry("restore-prefix-embedded-assignments", "modern-semantics.local-recovery", "fully-structured", "Extracts embedded assignments that should be standalone prefix statements.", true, true),
                entry("sink-definitions-to-first-use", "modern-semantics.local-recovery", "fully-structured", "Sinks definition scaffolding to the first real use site during semantic recovery.", true, true)
        );
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
