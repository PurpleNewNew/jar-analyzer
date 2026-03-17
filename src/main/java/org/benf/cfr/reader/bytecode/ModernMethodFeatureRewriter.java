package org.benf.cfr.reader.bytecode;

import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.SwitchPatternRewriter;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.util.ClassFileVersion;
import org.benf.cfr.reader.util.DecompilerComments;
import org.benf.cfr.reader.util.getopt.Options;
import org.benf.cfr.reader.util.getopt.OptionsImpl;

final class ModernMethodFeatureRewriter {
    private ModernMethodFeatureRewriter() {
    }

    static void rewrite(Method method,
                        Op04StructuredStatement block,
                        Options options,
                        ClassFileVersion classFileVersion,
                        BytecodeMeta bytecodeMeta,
                        DecompilerComments comments) {
        Op04StructuredStatement.normalizeInstanceOf(block, options, classFileVersion);
        if (bytecodeMeta.has(BytecodeMeta.CodeInfoFlag.INSTANCE_OF_MATCHES)) {
            Op04StructuredStatement.tidyInstanceMatches(block);
        }
        new SwitchPatternRewriter().rewrite(block);
        if (options.getOption(OptionsImpl.SWITCH_EXPRESSION, classFileVersion)) {
            Op04StructuredStatement.switchExpression(method, block, comments);
        }
    }
}
