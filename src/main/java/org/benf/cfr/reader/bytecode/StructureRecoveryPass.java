package org.benf.cfr.reader.bytecode;

import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;

import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

interface StructureRecoveryPass {
    StructuredPassDescriptor descriptor();

    boolean enabled(Op04StructuredStatement block, MethodAnalysisContext context);

    void apply(Op04StructuredStatement block, MethodAnalysisContext context);

    static StructureRecoveryPass alwaysEnabled(StructuredPassDescriptor descriptor,
                                               BiConsumer<Op04StructuredStatement, MethodAnalysisContext> action) {
        return when(descriptor, (block, context) -> true, action);
    }

    static StructureRecoveryPass when(StructuredPassDescriptor descriptor,
                                      BiPredicate<Op04StructuredStatement, MethodAnalysisContext> enabled,
                                      BiConsumer<Op04StructuredStatement, MethodAnalysisContext> action) {
        return new StructureRecoveryPass() {
            @Override
            public StructuredPassDescriptor descriptor() {
                return descriptor;
            }

            @Override
            public boolean enabled(Op04StructuredStatement block, MethodAnalysisContext context) {
                return enabled.test(block, context);
            }

            @Override
            public void apply(Op04StructuredStatement block, MethodAnalysisContext context) {
                action.accept(block, context);
            }
        };
    }
}
