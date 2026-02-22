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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.Set;

public final class RulePathUtil {
    private RulePathUtil() {
    }

    public static Path resolveExisting(String relativePath, Class<?> anchorClass) {
        for (Path candidate : buildCandidates(relativePath, anchorClass)) {
            if (candidate != null && Files.exists(candidate)) {
                return candidate;
            }
        }
        return normalize(Paths.get(relativePath));
    }

    public static Path resolveForWrite(String relativePath, Class<?> anchorClass) {
        for (Path candidate : buildCandidates(relativePath, anchorClass)) {
            if (candidate != null && Files.exists(candidate)) {
                return candidate;
            }
        }
        for (Path candidate : buildCandidates(relativePath, anchorClass)) {
            if (candidate == null) {
                continue;
            }
            Path parent = candidate.getParent();
            if (parent != null && (Files.isDirectory(parent) || Files.exists(parent))) {
                return candidate;
            }
        }
        return normalize(Paths.get(relativePath));
    }

    private static Set<Path> buildCandidates(String relativePath, Class<?> anchorClass) {
        LinkedHashSet<Path> out = new LinkedHashSet<>();
        if (relativePath == null || relativePath.isBlank()) {
            return out;
        }

        Path rel = Paths.get(relativePath);
        out.add(normalize(rel));

        String userDir = System.getProperty("user.dir", "");
        if (!userDir.isBlank()) {
            Path cursor = normalize(Paths.get(userDir));
            for (int i = 0; i < 6 && cursor != null; i++) {
                out.add(normalize(cursor.resolve(relativePath)));
                cursor = cursor.getParent();
            }
        }

        Path appBase = resolveAppBase(anchorClass);
        if (appBase != null) {
            Path cursor = appBase;
            for (int i = 0; i < 6 && cursor != null; i++) {
                out.add(normalize(cursor.resolve(relativePath)));
                cursor = cursor.getParent();
            }
        }
        return out;
    }

    private static Path resolveAppBase(Class<?> anchorClass) {
        if (anchorClass == null) {
            return null;
        }
        try {
            Path location = Paths.get(anchorClass.getProtectionDomain().getCodeSource().getLocation().toURI());
            Path normalized = normalize(location);
            if (normalized == null) {
                return null;
            }
            return Files.isDirectory(normalized) ? normalized : normalized.getParent();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Path normalize(Path path) {
        if (path == null) {
            return null;
        }
        try {
            return path.toAbsolutePath().normalize();
        } catch (Exception ex) {
            return path.normalize();
        }
    }
}

