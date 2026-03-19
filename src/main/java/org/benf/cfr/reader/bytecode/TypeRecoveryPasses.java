package org.benf.cfr.reader.bytecode;

import java.util.List;

public final class TypeRecoveryPasses {
    public static final StructuredPassEntry FIELD_INITIALIZER_HINT = entry(
            "field-initializer-hint",
            "type-recovery.field-init",
            "expression-available",
            "Pushes declared field type into initializer expressions before dump."
    );
    public static final StructuredPassEntry INLINE_SYNTHETIC_CREATOR_HINT = entry(
            "inline-synthetic-creator-hint",
            "type-recovery.structured-assignment",
            "fully-structured",
            "Pushes creator variable type into inlined synthetic assignment expressions."
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
    public static final StructuredPassEntry DISPLAY_TYPE_STATIC_BINDER = entry(
            "display-type-static-binder",
            "type-recovery.display-type",
            "expression-available",
            "Captures static invocation binder candidates before choosing a display return type."
    );
    public static final StructuredPassEntry DISPLAY_TYPE_STATIC_RETURN = entry(
            "display-type-static-return",
            "type-recovery.display-type",
            "expression-available",
            "Resolves static invocation display return type before assigned-local type harmonization."
    );
    public static final StructuredPassEntry DISPLAY_TYPE_MEMBER_BINDER = entry(
            "display-type-member-binder",
            "type-recovery.display-type",
            "expression-available",
            "Captures member invocation binder candidates before choosing a display return type."
    );
    public static final StructuredPassEntry DISPLAY_TYPE_MEMBER_RETURN = entry(
            "display-type-member-return",
            "type-recovery.display-type",
            "expression-available",
            "Resolves member invocation display return type before assigned-local type harmonization."
    );
    public static final StructuredPassEntry ASSIGNMENT_TARGET_CONSTRAINT = entry(
            "assignment-target-constraint",
            "type-constraint",
            "fully-structured",
            "Pushes assignment target types into right-hand expressions before output."
    );
    public static final StructuredPassEntry VARIABLE_ASSIGNMENT_CONSTRAINT = entry(
            "variable-assignment-constraint",
            "type-constraint",
            "fully-structured",
            "Propagates assignment display types into local variable declarations before output."
    );
    public static final StructuredPassEntry RETURN_TARGET_CONSTRAINT = entry(
            "return-target-constraint",
            "type-constraint",
            "fully-structured",
            "Pushes method return target types into returned expressions before output."
    );
    public static final StructuredPassEntry NESTED_EXPRESSION_HINT = entry(
            "nested-expression-hint",
            "type-recovery.expression-hints",
            "expression-available",
            "Recursively propagates type hints into nested expressions."
    );
    public static final StructuredPassEntry LAMBDA_RETURN_TARGET_CONSTRAINT = entry(
            "lambda-return-target-constraint",
            "type-constraint",
            "expression-available",
            "Pushes functional-interface return type into lambda result expressions before output."
    );
    public static final StructuredPassEntry TERNARY_BRANCH_TARGET_CONSTRAINT = entry(
            "ternary-branch-target-constraint",
            "type-constraint",
            "expression-available",
            "Pushes ternary target type into both branch expressions before output."
    );

    private static final List<StructuredPassEntry> PASSES = List.of(
            FIELD_INITIALIZER_HINT,
            INLINE_SYNTHETIC_CREATOR_HINT,
            CAST_CHILD_HINT,
            ASSIGNMENT_EXPRESSION_HINT,
            DISPLAY_TYPE_STATIC_BINDER,
            DISPLAY_TYPE_STATIC_RETURN,
            DISPLAY_TYPE_MEMBER_BINDER,
            DISPLAY_TYPE_MEMBER_RETURN,
            ASSIGNMENT_TARGET_CONSTRAINT,
            VARIABLE_ASSIGNMENT_CONSTRAINT,
            RETURN_TARGET_CONSTRAINT,
            NESTED_EXPRESSION_HINT,
            LAMBDA_RETURN_TARGET_CONSTRAINT,
            TERNARY_BRANCH_TARGET_CONSTRAINT
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
