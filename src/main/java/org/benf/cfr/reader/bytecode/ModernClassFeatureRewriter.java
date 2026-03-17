package org.benf.cfr.reader.bytecode;

import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.RecordRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.SealedClassChecker;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.state.DCCommonState;
import org.benf.cfr.reader.util.getopt.Options;
import org.benf.cfr.reader.util.getopt.OptionsImpl;

final class ModernClassFeatureRewriter {
    private ModernClassFeatureRewriter() {
    }

    static void rewrite(ClassFile classFile, DCCommonState state) {
        Options options = state.getOptions();
        if (options.getOption(OptionsImpl.RECORD_TYPES, classFile.getClassFileVersion())) {
            RecordRewriter.rewrite(classFile, state);
        }
        if (options.getOption(OptionsImpl.SEALED, classFile.getClassFileVersion())) {
            SealedClassChecker.rewrite(classFile, state);
        }
    }
}
