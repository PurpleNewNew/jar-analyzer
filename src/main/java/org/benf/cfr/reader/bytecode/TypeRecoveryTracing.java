package org.benf.cfr.reader.bytecode;

import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ExpressionTypeHintHelper;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;

import java.util.function.Supplier;

public final class TypeRecoveryTracing {
    private static final ThreadLocal<TypeRecoveryTrace> ACTIVE = new ThreadLocal<TypeRecoveryTrace>();

    private TypeRecoveryTracing() {
    }

    public static Scope activate(TypeRecoveryTrace trace) {
        TypeRecoveryTrace previous = ACTIVE.get();
        ACTIVE.set(trace);
        return new Scope(previous);
    }

    public static void trace(StructuredPassEntry pass,
                             Expression expression,
                             JavaTypeInstance expectedType,
                             Runnable action) {
        trace(pass, expression, expectedType, () -> null, action);
    }

    public static void trace(StructuredPassEntry pass,
                             Expression expression,
                             JavaTypeInstance expectedType,
                             java.util.function.Supplier<String> detailSupplier,
                             Runnable action) {
        TypeRecoveryTrace trace = ACTIVE.get();
        if (trace == null || pass == null || expression == null || expectedType == null) {
            action.run();
            return;
        }
        String beforeType = ExpressionTypeHintHelper.describeObservedType(expression);
        action.run();
        trace.record(
                pass,
                expression.getClass().getSimpleName(),
                ExpressionTypeHintHelper.describeType(expectedType),
                beforeType,
                ExpressionTypeHintHelper.describeObservedType(expression),
                detailSupplier.get()
        );
    }

    public static JavaTypeInstance traceObservedType(StructuredPassEntry pass,
                                                     Expression expression,
                                                     Supplier<JavaTypeInstance> observedTypeSupplier,
                                                     Runnable action) {
        return traceObservedType(pass, expression, null, observedTypeSupplier, () -> null, action);
    }

    public static JavaTypeInstance traceObservedType(StructuredPassEntry pass,
                                                     Expression expression,
                                                     JavaTypeInstance expectedType,
                                                     Supplier<JavaTypeInstance> observedTypeSupplier,
                                                     Supplier<String> detailSupplier,
                                                     Runnable action) {
        TypeRecoveryTrace trace = ACTIVE.get();
        if (trace == null || pass == null || expression == null) {
            action.run();
            return observedTypeSupplier.get();
        }
        String beforeType = ExpressionTypeHintHelper.describeObservedType(expression);
        action.run();
        JavaTypeInstance observedType = observedTypeSupplier.get();
        trace.record(
                pass,
                expression.getClass().getSimpleName(),
                ExpressionTypeHintHelper.describeType(expectedType == null ? observedType : expectedType),
                beforeType,
                ExpressionTypeHintHelper.describeObservedType(expression),
                detailSupplier.get()
        );
        return observedType;
    }

    public static final class Scope implements AutoCloseable {
        private final TypeRecoveryTrace previous;

        private Scope(TypeRecoveryTrace previous) {
            this.previous = previous;
        }

        @Override
        public void close() {
            if (previous == null) {
                ACTIVE.remove();
            } else {
                ACTIVE.set(previous);
            }
        }
    }
}
