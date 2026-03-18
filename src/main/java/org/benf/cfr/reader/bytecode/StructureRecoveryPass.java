package org.benf.cfr.reader.bytecode;

import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;

interface StructureRecoveryPass {
    StructuredPassDescriptor descriptor();

    boolean enabled(Op04StructuredStatement block, MethodAnalysisContext context);

    void apply(Op04StructuredStatement block, MethodAnalysisContext context);
}
