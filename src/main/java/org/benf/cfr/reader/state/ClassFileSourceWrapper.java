package org.benf.cfr.reader.state;

import org.benf.cfr.reader.api.ClassFileSource;
import org.benf.cfr.reader.apiunreleased.ClassFileSource2;
import org.benf.cfr.reader.apiunreleased.JarContent;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;
import org.benf.cfr.reader.util.AnalysisType;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

/*
 * Compatibility for old class file source.
 *
 * I guess I picked the wrong day to commit to an API.
 */
public class ClassFileSourceWrapper implements ClassFileSource2 {
    private final ClassFileSource classFileSource;

    public static ClassFileSource2 wrap(ClassFileSource classFileSource) {
        return classFileSource instanceof ClassFileSource2 ? (ClassFileSource2) classFileSource : new ClassFileSourceWrapper(classFileSource);
    }

    public ClassFileSourceWrapper(ClassFileSource classFileSource) {
        this.classFileSource = classFileSource;
    }

    @Override
    public JarContent addJarContent(String jarPath, AnalysisType type) {
        if (jarPath == null) {
            return null;
        }
        Collection<String> classFiles = classFileSource.addJar(jarPath);
        if (classFiles == null) {
            return null;
        }
        return new JarContentImpl(classFiles, Collections.<String, String>emptyMap(), type);
    }

    @Override
    public void informAnalysisRelativePathDetail(String usePath, String classFilePath) {
        classFileSource.informAnalysisRelativePathDetail(usePath, classFilePath);
    }

    @Override
    public Collection<String> addJar(String jarPath) {
        if (jarPath == null) {
            return null;
        }
        return classFileSource.addJar(jarPath);
    }

    @Override
    public String getPossiblyRenamedPath(String path) {
        String renamedPath = classFileSource.getPossiblyRenamedPath(path);
        return renamedPath == null ? path : renamedPath;
    }

    @Override
    public Pair<byte[], String> getClassFileContent(String path) throws IOException {
        return classFileSource.getClassFileContent(path);
    }
}
