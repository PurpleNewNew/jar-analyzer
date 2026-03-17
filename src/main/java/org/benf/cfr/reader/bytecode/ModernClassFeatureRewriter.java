package org.benf.cfr.reader.bytecode;

import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.RecordRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.SealedClassChecker;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.state.DCCommonState;
import org.benf.cfr.reader.util.getopt.Options;

final class ModernClassFeatureRewriter {
    private ModernClassFeatureRewriter() {
    }

    static void rewrite(ClassFile classFile, DCCommonState state) {
        Options options = state.getOptions();
        ModernFeatureStrategy modernFeatures = ModernFeatureStrategy.from(options, classFile.getClassFileVersion());
        if (modernFeatures.supportsRecordTypes()) {
            RecordRewriter.rewrite(classFile, state);
        }
        if (modernFeatures.supportsSealedTypes()) {
            SealedClassChecker.rewrite(classFile, state, modernFeatures.shouldEmitPreviewSealedComment());
        }
    }
}
