package org.benf.cfr.reader.bytecode;

import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.StructuredConditionLocalRecovery;
import org.benf.cfr.reader.bytecode.analysis.opgraph.StructuredCreatorRecovery;

import java.util.ArrayList;
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

    private static final List<VariablePassPlan> PREPARATION_SEQUENCE = List.of(
            plan(DISCOVER_VARIABLE_SCOPES,
                    (block, context) -> StructuredSemanticTransforms.discoverVariableScopes(
                            context.method,
                            block,
                            context.variableFactory,
                            context.options,
                            context.classFileVersion,
                            context.bytecodeMeta
                    ))
    );

    private static final List<VariablePassPlan> RECOVERY_SEQUENCE = List.of(
            plan(MARK_LAMBDA_CAPTURED_VARIABLES,
                    (block, context) -> StructuredSemanticTransforms.markLambdaCapturedVariables(block)),
            plan(RESTORE_LIFTABLE_DEFINITION_ASSIGNMENTS,
                    (block, context) -> StructuredConditionLocalRecovery.restoreLiftableDefinitionAssignments(block)),
            plan(SINK_DEFINITIONS_TO_FIRST_USE,
                    (block, context) -> StructuredConditionLocalRecovery.sinkDefinitionsToFirstUse(block)),
            plan(RESTORE_CREATOR_DEPENDENCY_ORDER,
                    (block, context) -> StructuredCreatorRecovery.restoreCreatorDependencyOrder(block)),
            plan(RESTORE_CREATORS_BEFORE_FIRST_USE,
                    (block, context) -> StructuredCreatorRecovery.restoreCreatorsBeforeFirstUse(block)),
            plan(RESTORE_TRAILING_CREATOR_ASSIGNMENTS,
                    (block, context) -> StructuredCreatorRecovery.restoreTrailingCreatorAssignments(block)),
            plan(RESTORE_PREFIX_EMBEDDED_ASSIGNMENTS,
                    (block, context) -> StructuredCreatorRecovery.restorePrefixEmbeddedAssignments(block)),
            plan(REDUCE_CLASH_DECLARATIONS,
                    (block, context) -> StructuredSemanticTransforms.reduceClashDeclarations(block, context.bytecodeMeta)),
            plan(DISCOVER_LOCAL_CLASS_SCOPES,
                    (block, context) -> StructuredSemanticTransforms.discoverLocalClassScopes(
                            context.method,
                            block,
                            context.variableFactory,
                            context.options
                    ))
    );

    private static final List<StructuredPassEntry> PASSES = buildPasses();

    private VariableRecoveryPasses() {
    }

    public static List<StructuredPassEntry> describePasses() {
        return PASSES;
    }

    static List<VariablePassPlan> preparationPlans() {
        return PREPARATION_SEQUENCE;
    }

    static List<VariablePassPlan> recoveryPlans() {
        return RECOVERY_SEQUENCE;
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

    private static List<StructuredPassEntry> buildPasses() {
        ArrayList<StructuredPassEntry> passes = new ArrayList<StructuredPassEntry>();
        for (VariablePassPlan preparationPlan : PREPARATION_SEQUENCE) {
            passes.add(preparationPlan.pass());
        }
        for (VariablePassPlan recoveryPlan : RECOVERY_SEQUENCE) {
            passes.add(recoveryPlan.pass());
        }
        return List.copyOf(passes);
    }

    private static VariablePassPlan plan(StructuredPassEntry pass, VariablePassAction action) {
        return new VariablePassPlan(pass, action);
    }

    @FunctionalInterface
    interface VariablePassAction {
        void apply(Op04StructuredStatement block, MethodAnalysisContext context);
    }

    record VariablePassPlan(StructuredPassEntry pass, VariablePassAction action) {
        void apply(Op04StructuredStatement block, MethodAnalysisContext context) {
            action.apply(block, context);
        }
    }
}
