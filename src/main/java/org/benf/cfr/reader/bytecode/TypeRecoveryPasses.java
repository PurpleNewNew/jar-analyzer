package org.benf.cfr.reader.bytecode;

import java.util.List;

public final class TypeRecoveryPasses {
    public static final StructuredPassEntry FIELD_INITIALIZER_HINT = entry(
            "field-initializer-hint",
            "type-recovery.field-init",
            "expression-available",
            "Pushes declared field type into initializer expressions before dump."
    );
    public static final StructuredPassEntry ASSIGNMENT_RHS_HINT = entry(
            "assignment-rhs-hint",
            "type-recovery.structured-assignment",
            "fully-structured",
            "Pushes assignment target type into the right-hand expression before dump."
    );
    public static final StructuredPassEntry INLINE_SYNTHETIC_CREATOR_HINT = entry(
            "inline-synthetic-creator-hint",
            "type-recovery.structured-assignment",
            "fully-structured",
            "Pushes creator variable type into inlined synthetic assignment expressions."
    );
    public static final StructuredPassEntry RETURN_VALUE_HINT = entry(
            "return-value-hint",
            "type-recovery.structured-return",
            "fully-structured",
            "Pushes method return type into returned expressions before dump."
    );
    public static final StructuredPassEntry CAST_CHILD_HINT = entry(
            "cast-child-hint",
            "type-recovery.cast-expression",
            "expression-available",
            "Pushes cast target type into child expressions before redundant-cast cleanup."
    );
    public static final StructuredPassEntry ASSIGNMENT_EXPRESSION_HINT = entry(
            "assignment-expression-hint",
            "type-recovery.assignment-expression",
            "expression-available",
            "Pushes assignment-expression target type into its right-hand expression."
    );
    public static final StructuredPassEntry DISPLAY_TYPE_STATIC_RETURN = entry(
            "display-type-static-return",
            "type-recovery.display-type",
            "expression-available",
            "Resolves static invocation display return type before assigned-local type harmonization."
    );
    public static final StructuredPassEntry DISPLAY_TYPE_MEMBER_RETURN = entry(
            "display-type-member-return",
            "type-recovery.display-type",
            "expression-available",
            "Resolves member invocation display return type before assigned-local type harmonization."
    );
    public static final StructuredPassEntry NESTED_EXPRESSION_HINT = entry(
            "nested-expression-hint",
            "type-recovery.expression-hints",
            "expression-available",
            "Recursively propagates type hints into nested expressions."
    );
    public static final StructuredPassEntry LAMBDA_RETURN_TARGET_HINT = entry(
            "lambda-return-target-hint",
            "type-recovery.lambda",
            "expression-available",
            "Pushes functional-interface return type into lambda result constructors."
    );
    public static final StructuredPassEntry TERNARY_BRANCH_TARGET_HINT = entry(
            "ternary-branch-target-hint",
            "type-recovery.ternary",
            "expression-available",
            "Pushes ternary target type into both branch expressions."
    );

    private static final List<StructuredPassEntry> PASSES = List.of(
            FIELD_INITIALIZER_HINT,
            ASSIGNMENT_RHS_HINT,
            INLINE_SYNTHETIC_CREATOR_HINT,
            RETURN_VALUE_HINT,
            CAST_CHILD_HINT,
            ASSIGNMENT_EXPRESSION_HINT,
            DISPLAY_TYPE_STATIC_RETURN,
            DISPLAY_TYPE_MEMBER_RETURN,
            NESTED_EXPRESSION_HINT,
            LAMBDA_RETURN_TARGET_HINT,
            TERNARY_BRANCH_TARGET_HINT
    );

    private TypeRecoveryPasses() {
    }

    public static List<StructuredPassEntry> describePasses() {
        return PASSES;
    }

    private static StructuredPassEntry entry(String name,
                                             String stage,
                                             String inputRequirement,
                                             String outputPromise) {
        return StructuredPassEntry.of(
                "type-recovery",
                stage,
                inputRequirement,
                StructuredPassDescriptor.of(name, outputPromise, true, false)
        );
    }
}
