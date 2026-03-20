package org.benf.cfr.reader.state;

import org.benf.cfr.reader.apiunreleased.ClassFileSource2;
import org.benf.cfr.reader.apiunreleased.JarContent;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.util.AnalysisType;
import org.benf.cfr.reader.util.ConfusedCFRException;
import org.benf.cfr.reader.util.MiscConstants;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.functors.UnaryFunction;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;

public final class ClassFileSourceSupport {
    private ClassFileSourceSupport() {
    }

    static String getPossiblyRenamedPath(ClassFileSource2 classFileSource, String path) {
        String renamedPath = classFileSource.getPossiblyRenamedPath(path);
        return renamedPath == null ? path : renamedPath;
    }

    static Pair<byte[], String> getClassFileContent(ClassFileSource2 classFileSource, String path) throws IOException {
        Pair<byte[], String> content = classFileSource.getClassFileContent(path);
        if (content == null || content.getFirst() == null) {
            throw new IOException("No such file " + path);
        }
        String usePath = content.getSecond() == null ? path : content.getSecond();
        return Pair.make(content.getFirst(), usePath);
    }

    private static boolean isMultiReleaseJar(JarContent jarContent) {
        String val = jarContent.getManifestEntries().get(MiscConstants.MULTI_RELEASE_KEY);
        if (val == null) return false;
        return Boolean.parseBoolean(val);
    }

    private static JarContent getJarContent(ClassFileSource2 classFileSource, String path, AnalysisType type) {
        JarContent jarContent = classFileSource.addJarContent(path, type);
        if (jarContent == null) {
            throw new ConfusedCFRException("Failed to load jar " + path);
        }
        Collection<String> classFiles = jarContent.getClassFiles();
        if (classFiles == null) {
            throw new ConfusedCFRException("Class file source returned null class list for " + path);
        }
        Map<String, String> manifestEntries = jarContent.getManifestEntries();
        return new JarContentImpl(classFiles,
                manifestEntries == null ? Collections.<String, String>emptyMap() : manifestEntries,
                jarContent.getAnalysisType() == null ? type : jarContent.getAnalysisType());
    }

    public static TreeMap<Integer, List<JavaTypeInstance>> explicitlyLoadJar(DCCommonState dcCommonState, String path, AnalysisType type) {
        JarContent jarContent = getJarContent(dcCommonState.getClassFileSource(), path, type);
        Collection<String> classFiles = jarContent.getClassFiles();

        TreeMap<Integer, List<JavaTypeInstance>> baseRes = MapFactory.newTreeMap();
        Map<Integer, List<JavaTypeInstance>> res = MapFactory.newLazyMap(baseRes, new UnaryFunction<Integer, List<JavaTypeInstance>>() {
            @Override
            public List<JavaTypeInstance> invoke(Integer arg) {
                return ListFactory.newList();
            }
        });
        boolean isMultiReleaseJar = isMultiReleaseJar(jarContent);

        for (String classPath : classFiles) {
            int version = 0;
            if (isMultiReleaseJar) {
                Matcher matcher = MiscConstants.MULTI_RELEASE_PATH_PATTERN.matcher(classPath);
                if (matcher.matches()) {
                    try {
                        version = Integer.parseInt(matcher.group(1));
                        classPath = matcher.group(2);
                    } catch (Exception e) {
                        continue;
                    }
                }
            }
            if (classPath.toLowerCase().endsWith(".class")) {
                res.get(version).add(dcCommonState.getClassCache().getRefClassFor(classPath.substring(0, classPath.length() - 6)));
            }
        }
        return baseRes;
    }
}
