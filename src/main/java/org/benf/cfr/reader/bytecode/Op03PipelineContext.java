package org.benf.cfr.reader.bytecode;

import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.entities.attributes.AttributeCode;
import org.benf.cfr.reader.entities.constantpool.ConstantPool;
import org.benf.cfr.reader.state.DCCommonState;
import org.benf.cfr.reader.util.ClassFileVersion;
import org.benf.cfr.reader.util.DecompilerComments;
import org.benf.cfr.reader.util.getopt.Options;

final class Op03PipelineContext {
    final DCCommonState commonState;
    final Options options;
    final Method method;
    final ClassFile classFile;
    final ClassFileVersion classFileVersion;
    final BytecodeMeta bytecodeMeta;
    final DecompilerComments comments;
    final AttributeCode originalCodeAttribute;
    final ConstantPool constantPool;
    final boolean willSort;
    final boolean aggressiveSizeReductions;
    final int passIdx;
    final ModernFeatureStrategy modernFeatures;

    Op03PipelineContext(DCCommonState commonState,
                        Options options,
                        Method method,
                        ClassFile classFile,
                        ClassFileVersion classFileVersion,
                        BytecodeMeta bytecodeMeta,
                        DecompilerComments comments,
                        AttributeCode originalCodeAttribute,
                        ConstantPool constantPool,
                        boolean willSort,
                        boolean aggressiveSizeReductions,
                        int passIdx,
                        ModernFeatureStrategy modernFeatures) {
        this.commonState = commonState;
        this.options = options;
        this.method = method;
        this.classFile = classFile;
        this.classFileVersion = classFileVersion;
        this.bytecodeMeta = bytecodeMeta;
        this.comments = comments;
        this.originalCodeAttribute = originalCodeAttribute;
        this.constantPool = constantPool;
        this.willSort = willSort;
        this.aggressiveSizeReductions = aggressiveSizeReductions;
        this.passIdx = passIdx;
        this.modernFeatures = modernFeatures;
    }
}
