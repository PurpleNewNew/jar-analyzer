package org.benf.cfr.reader.bytecode;

import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifierFactory;
import org.benf.cfr.reader.bytecode.analysis.variables.VariableFactory;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.entities.attributes.AttributeCode;
import org.benf.cfr.reader.entities.constantpool.ConstantPool;
import org.benf.cfr.reader.state.DCCommonState;
import org.benf.cfr.reader.util.ClassFileVersion;
import org.benf.cfr.reader.util.DecompilerComments;
import org.benf.cfr.reader.util.getopt.Options;

import java.util.SortedMap;

final class MethodAnalysisContext {
    final DCCommonState commonState;
    final Options options;
    final Method method;
    final ClassFile classFile;
    final ClassFileVersion classFileVersion;
    final BytecodeMeta bytecodeMeta;
    final DecompilerComments comments;
    final VariableFactory variableFactory;
    final AnonymousClassUsage anonymousClassUsage;
    final AttributeCode originalCodeAttribute;
    final SortedMap<Integer, Integer> lutByOffset;
    final ConstantPool constantPool;
    final BlockIdentifierFactory blockIdentifierFactory;
    final ModernFeatureStrategy modernFeatures;

    MethodAnalysisContext(DCCommonState commonState,
                          Options options,
                          Method method,
                          ClassFile classFile,
                          ClassFileVersion classFileVersion,
                          BytecodeMeta bytecodeMeta,
                          DecompilerComments comments,
                          VariableFactory variableFactory,
                          AnonymousClassUsage anonymousClassUsage,
                          AttributeCode originalCodeAttribute,
                          SortedMap<Integer, Integer> lutByOffset,
                          ConstantPool constantPool,
                          BlockIdentifierFactory blockIdentifierFactory,
                          ModernFeatureStrategy modernFeatures) {
        this.commonState = commonState;
        this.options = options;
        this.method = method;
        this.classFile = classFile;
        this.classFileVersion = classFileVersion;
        this.bytecodeMeta = bytecodeMeta;
        this.comments = comments;
        this.variableFactory = variableFactory;
        this.anonymousClassUsage = anonymousClassUsage;
        this.originalCodeAttribute = originalCodeAttribute;
        this.lutByOffset = lutByOffset;
        this.constantPool = constantPool;
        this.blockIdentifierFactory = blockIdentifierFactory;
        this.modernFeatures = modernFeatures;
    }
}
