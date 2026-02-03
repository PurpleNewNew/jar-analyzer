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

import me.n1ar4.jar.analyzer.utils.ClassIndex;

import java.nio.file.Path;
import java.util.function.Supplier;

public final class DecompileLookupContext {
    private static final ThreadLocal<Integer> PREFERRED_JAR = new ThreadLocal<>();

    private DecompileLookupContext() {
    }

    public static Integer preferredJarId() {
        return PREFERRED_JAR.get();
    }

    public static <T> T withClassPath(Path classFilePath, Supplier<T> supplier) {
        if (supplier == null) {
            return null;
        }
        Integer prev = PREFERRED_JAR.get();
        Integer next = classFilePath == null ? null : ClassIndex.resolveJarId(classFilePath);
        if (next == null) {
            PREFERRED_JAR.remove();
        } else {
            PREFERRED_JAR.set(next);
        }
        try {
            return supplier.get();
        } finally {
            if (prev == null) {
                PREFERRED_JAR.remove();
            } else {
                PREFERRED_JAR.set(prev);
            }
        }
    }
}
