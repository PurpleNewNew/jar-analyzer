/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */
package me.n1ar4.jar.analyzer.utils;

import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.gui.MainForm;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import javax.swing.JTextField;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class ClasspathRegistry {
    private static final Logger logger = LogManager.getLogger();
    private static volatile List<Path> cachedArchives = Collections.emptyList();
    private static volatile String cachedClasspath = "";
    private static volatile long lastRootSeq = -1;
    private static volatile long lastBuildSeq = -1;

    private ClasspathRegistry() {
    }

    public static List<Path> getClasspathArchives() {
        ensureFresh();
        return cachedArchives;
    }

    public static String getClasspathString() {
        ensureFresh();
        return cachedClasspath;
    }

    private static void ensureFresh() {
        long rootSeq = RuntimeClassResolver.getRootSeq();
        long buildSeq = DatabaseManager.getBuildSeq();
        if (rootSeq == lastRootSeq && buildSeq == lastBuildSeq && cachedArchives != null) {
            return;
        }
        synchronized (ClasspathRegistry.class) {
            if (rootSeq == lastRootSeq && buildSeq == lastBuildSeq && cachedArchives != null) {
                return;
            }
            rebuild(rootSeq, buildSeq);
        }
    }

    private static void rebuild(long rootSeq, long buildSeq) {
        List<Path> resolved = resolveArchives();
        cachedArchives = resolved.isEmpty()
                ? Collections.emptyList()
                : Collections.unmodifiableList(resolved);
        cachedClasspath = buildClasspathString(resolved);
        lastRootSeq = rootSeq;
        lastBuildSeq = buildSeq;
    }

    private static List<Path> resolveArchives() {
        Set<Path> out = new LinkedHashSet<>();
        Path root = resolveRootPath();
        if (root != null && Files.exists(root)) {
            try {
                ClasspathResolver.ClasspathGraph graph = ClasspathResolver.resolveClasspathGraph(root);
                if (graph != null) {
                    out.addAll(graph.getOrderedArchives());
                }
            } catch (Throwable t) {
                logger.debug("resolve classpath graph failed: {}", t.toString());
            }
        }
        List<Path> runtime = RuntimeClassResolver.resolveRuntimeArchivesForClasspath();
        if (runtime != null && !runtime.isEmpty()) {
            out.addAll(runtime);
        }
        return new ArrayList<>(out);
    }

    private static Path resolveRootPath() {
        try {
            JTextField field = MainForm.getInstance().getFileText();
            if (field == null) {
                return null;
            }
            String text = field.getText();
            if (text == null || text.trim().isEmpty()) {
                return null;
            }
            return Paths.get(text.trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String buildClasspathString(List<Path> archives) {
        if (archives == null || archives.isEmpty()) {
            return "";
        }
        String sep = File.pathSeparator;
        StringBuilder sb = new StringBuilder();
        for (Path path : archives) {
            if (path == null) {
                continue;
            }
            String value;
            try {
                value = path.toAbsolutePath().toString();
            } catch (Exception ex) {
                continue;
            }
            if (value == null || value.trim().isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(sep);
            }
            sb.append(value);
        }
        return sb.toString();
    }
}
