/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.core.scope;

import me.n1ar4.jar.analyzer.engine.project.ProjectOrigin;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ArchiveScopeClassifier {
    private ArchiveScopeClassifier() {
    }

    public static ScopeSummary classifyArchives(List<Path> archives,
                                                Path primaryInputPath,
                                                Path runtimePath) {
        Map<Path, ProjectOrigin> origins = new LinkedHashMap<>();
        int target = 0;
        int library = 0;
        int sdk = 0;
        if (archives == null || archives.isEmpty()) {
            return new ScopeSummary(origins, target, library, sdk);
        }
        for (Path archive : archives) {
            if (archive == null) {
                continue;
            }
            Path normalized = normalizePath(archive);
            ProjectOrigin origin = classifyArchive(normalized, primaryInputPath, runtimePath);
            origins.put(normalized, origin);
            if (origin == ProjectOrigin.SDK) {
                sdk++;
            } else if (origin == ProjectOrigin.LIBRARY) {
                library++;
            } else {
                target++;
            }
        }
        return new ScopeSummary(origins, target, library, sdk);
    }

    public static ProjectOrigin classifyArchive(Path archivePath,
                                                Path primaryInputPath,
                                                Path runtimePath) {
        Path archive = normalizePath(archivePath);
        String jarName = safeJarName(archive);

        // Priority 1: force target
        if (AnalysisScopeRules.isForceTargetJar(jarName)) {
            return ProjectOrigin.APP;
        }

        // Priority 2: sdk
        if (isSdkArchive(archive, jarName, runtimePath)) {
            return ProjectOrigin.SDK;
        }

        // Priority 3: common third-party libraries
        if (AnalysisScopeRules.isCommonLibraryJar(jarName)) {
            return ProjectOrigin.LIBRARY;
        }

        // Priority 4: app heuristic
        if (isUnderPrimaryInput(archive, primaryInputPath)) {
            return ProjectOrigin.APP;
        }

        return ProjectOrigin.LIBRARY;
    }

    public static boolean isTargetArchive(Path archivePath,
                                          Path primaryInputPath,
                                          Path runtimePath) {
        return classifyArchive(archivePath, primaryInputPath, runtimePath) == ProjectOrigin.APP;
    }

    public static boolean isSdkArchive(Path archivePath, String jarName, Path runtimePath) {
        if (AnalysisScopeRules.isSdkJar(jarName)) {
            return true;
        }
        Path archive = normalizePath(archivePath);
        Path runtime = normalizePath(runtimePath);
        if (archive != null && runtime != null) {
            try {
                if (archive.startsWith(runtime)) {
                    return true;
                }
                Path runtimeParent = runtime.getParent();
                if (runtimeParent != null && archive.startsWith(runtimeParent.resolve("jmods"))) {
                    return true;
                }
            } catch (Exception ignored) {
            }
        }
        if (archive == null) {
            return false;
        }
        String lower = archive.toString().replace('\\', '/').toLowerCase(Locale.ROOT);
        return lower.endsWith(".jmod") || lower.contains("/jmods/");
    }

    public static boolean isAllCommonOrSdk(ScopeSummary summary) {
        if (summary == null) {
            return true;
        }
        return summary.targetArchiveCount() <= 0;
    }

    public static List<Path> pickAppArchives(ScopeSummary summary) {
        if (summary == null || summary.originsByArchive().isEmpty()) {
            return List.of();
        }
        List<Path> out = new ArrayList<>();
        for (Map.Entry<Path, ProjectOrigin> entry : summary.originsByArchive().entrySet()) {
            if (entry == null || entry.getKey() == null) {
                continue;
            }
            if (entry.getValue() == ProjectOrigin.APP) {
                out.add(entry.getKey());
            }
        }
        return out;
    }

    public static List<Path> pickLibraryArchives(ScopeSummary summary) {
        if (summary == null || summary.originsByArchive().isEmpty()) {
            return List.of();
        }
        List<Path> out = new ArrayList<>();
        for (Map.Entry<Path, ProjectOrigin> entry : summary.originsByArchive().entrySet()) {
            if (entry == null || entry.getKey() == null) {
                continue;
            }
            if (entry.getValue() == ProjectOrigin.LIBRARY) {
                out.add(entry.getKey());
            }
        }
        return out;
    }

    public static List<Path> pickSdkArchives(ScopeSummary summary) {
        if (summary == null || summary.originsByArchive().isEmpty()) {
            return List.of();
        }
        List<Path> out = new ArrayList<>();
        for (Map.Entry<Path, ProjectOrigin> entry : summary.originsByArchive().entrySet()) {
            if (entry == null || entry.getKey() == null) {
                continue;
            }
            if (entry.getValue() == ProjectOrigin.SDK) {
                out.add(entry.getKey());
            }
        }
        return out;
    }

    private static boolean isUnderPrimaryInput(Path archive, Path primaryInputPath) {
        if (archive == null || primaryInputPath == null) {
            return false;
        }
        Path primary = normalizePath(primaryInputPath);
        if (primary == null) {
            return false;
        }
        try {
            if (Files.isRegularFile(primary)) {
                return archive.equals(primary);
            }
            return archive.startsWith(primary);
        } catch (Exception ignored) {
            return false;
        }
    }

    private static String safeJarName(Path path) {
        if (path == null || path.getFileName() == null) {
            return "";
        }
        return path.getFileName().toString().toLowerCase(Locale.ROOT);
    }

    private static Path normalizePath(Path path) {
        if (path == null) {
            return null;
        }
        try {
            return path.toAbsolutePath().normalize();
        } catch (Exception ex) {
            return path.normalize();
        }
    }

    public record ScopeSummary(Map<Path, ProjectOrigin> originsByArchive,
                               int targetArchiveCount,
                               int libraryArchiveCount,
                               int sdkArchiveCount) {
        public ScopeSummary {
            originsByArchive = originsByArchive == null ? Map.of() : Map.copyOf(originsByArchive);
            if (targetArchiveCount < 0) {
                targetArchiveCount = 0;
            }
            if (libraryArchiveCount < 0) {
                libraryArchiveCount = 0;
            }
            if (sdkArchiveCount < 0) {
                sdkArchiveCount = 0;
            }
        }
    }
}
