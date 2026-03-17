package org.benf.cfr.reader.bytecode;

import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.SwitchPatternRewriter;

final class PatternSemanticsRewriter {
    private final ModernFeatureStrategy modernFeatures;
    private final SwitchPatternRewriter switchPatternRewriter = new SwitchPatternRewriter();

    PatternSemanticsRewriter(ModernFeatureStrategy modernFeatures) {
        this.modernFeatures = modernFeatures;
    }

    void rewrite(Op04StructuredStatement block, BytecodeMeta bytecodeMeta) {
        if (modernFeatures.supportsBindingPatterns()) {
            Op04StructuredStatement.normalizeInstanceOf(block, true);
            if (bytecodeMeta.has(BytecodeMeta.CodeInfoFlag.INSTANCE_OF_MATCHES)) {
                Op04StructuredStatement.tidyInstanceMatches(block);
            }
        }
        if (modernFeatures.prefersPatternOutput()) {
            switchPatternRewriter.rewrite(block);
        }
    }
}
