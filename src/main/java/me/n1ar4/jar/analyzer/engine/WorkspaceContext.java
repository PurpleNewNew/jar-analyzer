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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Holds workspace paths for headless components that used to read Swing text fields.
 */
public final class WorkspaceContext {
    private static final AtomicReference<Path> INPUT_PATH = new AtomicReference<>();
    private static final AtomicReference<Path> RUNTIME_JAR_PATH = new AtomicReference<>();
    private static final AtomicReference<List<Path>> ANALYZED_ARCHIVES =
            new AtomicReference<>(Collections.emptyList());
    private static final AtomicBoolean RESOLVE_INNER_JARS = new AtomicBoolean(false);

    private WorkspaceContext() {
    }

    public static void setInputPath(Path inputPath) {
        INPUT_PATH.set(inputPath);
    }

    public static Path getInputPath() {
        return INPUT_PATH.get();
    }

    public static void setRuntimeJarPath(Path rtJarPath) {
        RUNTIME_JAR_PATH.set(rtJarPath);
    }

    public static Path getRuntimeJarPath() {
        return RUNTIME_JAR_PATH.get();
    }

    public static void setAnalyzedArchives(List<Path> archives) {
        if (archives == null || archives.isEmpty()) {
            ANALYZED_ARCHIVES.set(Collections.emptyList());
            return;
        }
        ANALYZED_ARCHIVES.set(Collections.unmodifiableList(new ArrayList<>(archives)));
    }

    public static List<Path> getAnalyzedArchives() {
        List<Path> value = ANALYZED_ARCHIVES.get();
        return value == null ? Collections.emptyList() : value;
    }

    public static void setResolveInnerJars(boolean enabled) {
        RESOLVE_INNER_JARS.set(enabled);
    }

    public static boolean isResolveInnerJars() {
        return RESOLVE_INNER_JARS.get();
    }

    public static void clear() {
        INPUT_PATH.set(null);
        RUNTIME_JAR_PATH.set(null);
        ANALYZED_ARCHIVES.set(Collections.emptyList());
        RESOLVE_INNER_JARS.set(false);
    }
}
