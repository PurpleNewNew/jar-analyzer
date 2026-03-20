package org.benf.cfr.reader.state;

import org.benf.cfr.reader.apiunreleased.ClassFileSource2;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;
import org.benf.cfr.reader.bytecode.analysis.types.ClassNameUtils;
import org.benf.cfr.reader.bytecode.analysis.types.JavaRefTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.mapping.NullMapping;
import org.benf.cfr.reader.mapping.ObfuscationMapping;
import org.benf.cfr.reader.util.CannotLoadClassException;
import org.benf.cfr.reader.util.DecompilerComment;
import org.benf.cfr.reader.util.bytestream.BaseByteData;
import org.benf.cfr.reader.util.bytestream.ByteData;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.collections.SetFactory;
import org.benf.cfr.reader.util.functors.BinaryFunction;
import org.benf.cfr.reader.util.functors.UnaryFunction;
import org.benf.cfr.reader.util.getopt.Options;

import java.io.File;
import java.util.Map;
import java.util.Set;

public class DCCommonState {
    private static final BinaryFunction<String, DCCommonState, ClassFile> DIRECT_CLASS_FILE_LOADER =
            new BinaryFunction<String, DCCommonState, ClassFile>() {
                @Override
                public ClassFile invoke(String path, DCCommonState state) {
                    return state.loadClassFileAtPath(path);
                }
            };

    private final ClassCache classCache;
    private final ClassFileSource2 classFileSource;
    private final Options options;
    private final Map<String, ClassFile> classFileCache;
    private Set<JavaTypeInstance> versionCollisions;
    private final ObfuscationMapping obfuscationMapping;
    private final OverloadMethodSetCache overloadMethodSetCache;

    public DCCommonState(Options options, ClassFileSource2 classFileSource) {
        this(options, classFileSource, DIRECT_CLASS_FILE_LOADER, SetFactory.<JavaTypeInstance>newSet(), NullMapping.INSTANCE, new OverloadMethodSetCache());
    }

    public DCCommonState(DCCommonState dcCommonState, final BinaryFunction<String, DCCommonState, ClassFile> cacheAccess) {
        this(dcCommonState.options,
                dcCommonState.classFileSource,
                cacheAccess,
                dcCommonState.versionCollisions,
                dcCommonState.obfuscationMapping,
                dcCommonState.overloadMethodSetCache);
    }

    public DCCommonState(DCCommonState dcCommonState, ObfuscationMapping mapping) {
        this(dcCommonState.options,
                dcCommonState.classFileSource,
                DIRECT_CLASS_FILE_LOADER,
                dcCommonState.versionCollisions,
                mapping,
                dcCommonState.overloadMethodSetCache);
    }

    private DCCommonState(Options options,
                          ClassFileSource2 classFileSource,
                          final BinaryFunction<String, DCCommonState, ClassFile> cacheAccess,
                          Set<JavaTypeInstance> versionCollisions,
                          ObfuscationMapping obfuscationMapping,
                          OverloadMethodSetCache overloadMethodSetCache) {
        this.options = options;
        this.classFileSource = classFileSource;
        this.classCache = new ClassCache(this);
        this.classFileCache = MapFactory.newExceptionRetainingLazyMap(new UnaryFunction<String, ClassFile>() {
            @Override
            public ClassFile invoke(String arg) {
                return cacheAccess.invoke(arg, DCCommonState.this);
            }
        });
        this.versionCollisions = versionCollisions;
        this.obfuscationMapping = obfuscationMapping;
        this.overloadMethodSetCache = overloadMethodSetCache;
    }

    public void setCollisions(Set<JavaTypeInstance> versionCollisions) {
        this.versionCollisions = versionCollisions;
    }

    public Set<JavaTypeInstance> getVersionCollisions() {
        return versionCollisions;
    }

    String getPossiblyRenamedFileFromClassFileSource(String name) {
        return ClassFileSourceSupport.getPossiblyRenamedPath(classFileSource, name);
    }

    public ClassFile loadClassFileAtPath(final String path) {
        try {
            Pair<byte[], String> content = ClassFileSourceSupport.getClassFileContent(classFileSource, path);
            ByteData data = new BaseByteData(content.getFirst());
            return new ClassFile(data, content.getSecond(), this);
        } catch (Exception e) {
            throw new CannotLoadClassException(path, e);
        }
    }

    public DecompilerComment renamedTypeComment(String typeName) {
        String originalName = classCache.getOriginalName(typeName);
        if (originalName != null) {
            return new DecompilerComment("Renamed from " + originalName);
        }
        return null;
    }

    public ClassFile getClassFile(String path) throws CannotLoadClassException {
        return classFileCache.get(path);
    }

    public JavaRefTypeInstance getClassTypeOrNull(String path) {
        try {
            ClassFile classFile = getClassFile(path);
            return (JavaRefTypeInstance) classFile.getClassType();
        } catch (CannotLoadClassException e) {
            return null;
        }
    }

    public ClassFile getClassFile(JavaTypeInstance classInfo) throws CannotLoadClassException {
        String path = classInfo.getRawName();
        path = ClassNameUtils.convertToPath(path) + ".class";
        return getClassFile(path);
    }

    public ClassFile getClassFileForAnalysis(String pathOrName) throws CannotLoadClassException {
        String path = pathOrName;
        if (!pathOrName.endsWith(".class") && !new File(pathOrName).exists()) {
            path = ClassNameUtils.convertToPath(pathOrName) + ".class";
        }
        ClassFile classFile = getClassFile(path);
        classFileSource.informAnalysisRelativePathDetail(classFile.getUsePath(), classFile.getFilePath());
        try {
            return getClassFile(classFile.getClassType());
        } catch (CannotLoadClassException ignore) {
            return classFile;
        }
    }

    public ClassFile getClassFileOrNull(JavaTypeInstance classInfo) {
        try {
            return getClassFile(classInfo);
        } catch (CannotLoadClassException ignore) {
            return null;
        }
    }


    public ClassCache getClassCache() {
        return classCache;
    }

    ClassFileSource2 getClassFileSource() {
        return classFileSource;
    }

    public Options getOptions() {
        return options;
    }

    public ObfuscationMapping getObfuscationMapping() {
        return obfuscationMapping;
    }

    public OverloadMethodSetCache getOverloadMethodSetCache() {
        return overloadMethodSetCache;
    }

    public DCCommonState forkForBatchWorker() {
        return new DCCommonState(options,
                classFileSource,
                DIRECT_CLASS_FILE_LOADER,
                versionCollisions,
                obfuscationMapping,
                new OverloadMethodSetCache());
    }
}
