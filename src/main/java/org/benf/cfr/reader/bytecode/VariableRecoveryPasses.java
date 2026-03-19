package org.benf.cfr.reader.bytecode;

import java.util.List;

public final class VariableRecoveryPasses {
    public static final StructuredPassEntry DISCOVER_VARIABLE_SCOPES = entry(
            "discover-variable-scopes",
            "variable-preparation",
            "fully-structured",
            "Discovers local variable scopes before lifting and sinking declaration-local scaffolding.",
            false,
            false
    );
    public static final StructuredPassEntry MARK_LAMBDA_CAPTURED_VARIABLES = entry(
            "mark-lambda-captured-variables",
            "variable-recovery",
            "fully-structured",
            "Marks locals captured by lambda bodies before local-variable recovery mutates declaration order.",
            true,
            false,
            "discover-variable-scopes"
    );
    public static final StructuredPassEntry RESTORE_LIFTABLE_DEFINITION_ASSIGNMENTS = entry(
            "restore-liftable-definition-assignments",
            "variable-recovery",
            "fully-structured",
            "Lifts definition assignments back to the declaration/use boundary.",
            true,
            true,
            "mark-lambda-captured-variables"
    );
    public static final StructuredPassEntry SINK_DEFINITIONS_TO_FIRST_USE = entry(
            "sink-definitions-to-first-use",
            "variable-recovery",
            "fully-structured",
            "Sinks definition scaffolding to the first real use site during variable recovery.",
            true,
            true,
            "restore-liftable-definition-assignments"
    );
    public static final StructuredPassEntry RESTORE_CREATOR_DEPENDENCY_ORDER = entry(
            "restore-creator-dependency-order",
            "variable-recovery",
            "fully-structured",
            "Restores creator ordering when constructor dependencies were emitted out of order.",
            true,
            true,
            "sink-definitions-to-first-use"
    );
    public static final StructuredPassEntry RESTORE_CREATORS_BEFORE_FIRST_USE = entry(
            "restore-creators-before-first-use",
            "variable-recovery",
            "fully-structured",
            "Moves creator assignments back before the first semantic use.",
            true,
            true,
            "restore-creator-dependency-order"
    );
    public static final StructuredPassEntry RESTORE_TRAILING_CREATOR_ASSIGNMENTS = entry(
            "restore-trailing-creator-assignments",
            "variable-recovery",
            "fully-structured",
            "Pulls trailing creator assignments back into the declaration region.",
            true,
            true,
            "restore-creators-before-first-use"
    );
    public static final StructuredPassEntry RESTORE_PREFIX_EMBEDDED_ASSIGNMENTS = entry(
            "restore-prefix-embedded-assignments",
            "variable-recovery",
            "fully-structured",
            "Extracts embedded assignments that should be standalone prefix statements.",
            true,
            true,
            "restore-trailing-creator-assignments"
    );
    public static final StructuredPassEntry REDUCE_CLASH_DECLARATIONS = entry(
            "reduce-clash-declarations",
            "variable-recovery",
            "fully-structured",
            "Reduces declaration clashes once variable scopes and creator placements have stabilized.",
            true,
            true,
            "restore-prefix-embedded-assignments"
    );
    public static final StructuredPassEntry DISCOVER_LOCAL_CLASS_SCOPES = entry(
            "discover-local-class-scopes",
            "variable-recovery",
            "fully-structured",
            "Discovers local and anonymous class scopes after variable recovery settles declarations.",
            false,
            false,
            "reduce-clash-declarations"
    );

    private static final List<StructuredPassEntry> PASSES = List.of(
            DISCOVER_VARIABLE_SCOPES,
            MARK_LAMBDA_CAPTURED_VARIABLES,
            RESTORE_LIFTABLE_DEFINITION_ASSIGNMENTS,
            SINK_DEFINITIONS_TO_FIRST_USE,
            RESTORE_CREATOR_DEPENDENCY_ORDER,
            RESTORE_CREATORS_BEFORE_FIRST_USE,
            RESTORE_TRAILING_CREATOR_ASSIGNMENTS,
            RESTORE_PREFIX_EMBEDDED_ASSIGNMENTS,
            REDUCE_CLASH_DECLARATIONS,
            DISCOVER_LOCAL_CLASS_SCOPES
    );

    private VariableRecoveryPasses() {
    }

    public static List<StructuredPassEntry> describePasses() {
        return PASSES;
    }

    private static StructuredPassEntry entry(String name,
                                             String stage,
                                             String inputRequirement,
                                             String outputPromise,
                                             boolean idempotent,
                                             boolean allowsStructuralChange,
                                             String... dependencies) {
        return StructuredPassEntry.of(
                "variable-recovery",
                stage,
                inputRequirement,
                StructuredPassDescriptor.of(name, outputPromise, idempotent, allowsStructuralChange, dependencies)
        );
    }
}
