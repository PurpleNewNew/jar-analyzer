/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.engine;

import me.n1ar4.jar.analyzer.utils.InterruptUtil;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import org.benf.cfr.reader.apiunreleased.ClassFileSource2;
import org.benf.cfr.reader.apiunreleased.JarContent;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;
import org.benf.cfr.reader.util.AnalysisType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class JarAnalyzerClassFileSource implements ClassFileSource2 {
    private static final Logger logger = LogManager.getLogger();

    @Override
    public void informAnalysisRelativePathDetail(String usePath, String classFilePath) {
        // No relocation needed for temp-extracted classes.
    }

    @Override
    public Collection<String> addJar(String jarPath) {
        JarContent content = addJarContent(jarPath, AnalysisType.JAR);
        return content == null ? Collections.emptyList() : content.getClassFiles();
    }

    @Override
    public String getPossiblyRenamedPath(String path) {
        return path;
    }

    @Override
    public Pair<byte[], String> getClassFileContent(String path) throws IOException {
        String p = path == null ? null : path.strip();
        if (p == null || p.isEmpty()) {
            throw new IOException("No such file " + path);
        }
        ClassLookupService.LookupResult result = ClassLookupService.find(p);
        if (result == null || result.getBytes() == null) {
            throw new IOException("No such file " + path);
        }
        return Pair.make(result.getBytes(), result.getTracePath());
    }

    @Override
    public JarContent addJarContent(String jarPath, AnalysisType analysisType) {
        Path path = safePath(jarPath);
        if (path == null || !Files.exists(path)) {
            return null;
        }
        try (ZipFile zipFile = new ZipFile(path.toFile())) {
            java.util.List<String> classes = new java.util.ArrayList<>();
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }
                String name = entry.getName();
                if (name.endsWith(".class")) {
                    if (analysisType == AnalysisType.WAR && name.startsWith("WEB-INF/classes/")) {
                        classes.add(name.substring("WEB-INF/classes/".length()));
                    } else {
                        classes.add(name);
                    }
                }
            }
            return new SimpleJarContent(classes, Collections.emptyMap(), analysisType);
        } catch (Exception ex) {
            InterruptUtil.restoreInterruptIfNeeded(ex);
            logger.debug("read jar classes failed: {}: {}", jarPath, ex.toString());
            return null;
        }
    }

    private static Path safePath(String raw) {
        String p = raw == null ? null : raw.strip();
        if (p == null || p.isEmpty()) {
            return null;
        }
        try {
            return Paths.get(p).toAbsolutePath().normalize();
        } catch (Exception ex) {
            InterruptUtil.restoreInterruptIfNeeded(ex);
            logger.debug("invalid path: {}: {}", raw, ex.toString());
            return null;
        }
    }

    private static final class SimpleJarContent implements JarContent {
        private final Collection<String> classFiles;
        private final Map<String, String> manifestEntries;
        private final AnalysisType analysisType;

        private SimpleJarContent(Collection<String> classFiles,
                                 Map<String, String> manifestEntries,
                                 AnalysisType analysisType) {
            this.classFiles = classFiles == null ? Collections.emptyList() : classFiles;
            this.manifestEntries = manifestEntries == null ? new HashMap<>() : manifestEntries;
            this.analysisType = analysisType;
        }

        @Override
        public Collection<String> getClassFiles() {
            return classFiles;
        }

        @Override
        public Map<String, String> getManifestEntries() {
            return manifestEntries;
        }

        @Override
        public AnalysisType getAnalysisType() {
            return analysisType;
        }
    }
}
