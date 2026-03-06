/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.gui.runtime.api;

import me.n1ar4.jar.analyzer.core.scope.AnalysisScopeRules;
import me.n1ar4.jar.analyzer.engine.project.ProjectModel;
import me.n1ar4.jar.analyzer.engine.project.ProjectOrigin;
import me.n1ar4.jar.analyzer.engine.project.ProjectRoot;
import me.n1ar4.jar.analyzer.entity.JarEntity;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class ProjectJarOriginResolver {
    private static final ProjectJarOriginResolver EMPTY = new ProjectJarOriginResolver(Map.of());

    private final Map<Integer, ProjectOrigin> jarOrigins;

    private ProjectJarOriginResolver(Map<Integer, ProjectOrigin> jarOrigins) {
        this.jarOrigins = jarOrigins == null || jarOrigins.isEmpty() ? Map.of() : Map.copyOf(jarOrigins);
    }

    static ProjectJarOriginResolver empty() {
        return EMPTY;
    }

    static ProjectJarOriginResolver fromProjectModel(ProjectModel model, List<JarEntity> jars) {
        List<OriginPathRule> rules = collectOriginRules(model);
        if (rules.isEmpty() || jars == null || jars.isEmpty()) {
            return empty();
        }
        Map<Integer, ProjectOrigin> out = new HashMap<>();
        for (JarEntity jar : jars) {
            if (jar == null) {
                continue;
            }
            Path jarPath = normalizePath(jar.getJarAbsPath());
            ProjectOrigin origin = resolveOriginByJarName(jarPath);
            if (origin == ProjectOrigin.UNKNOWN) {
                origin = resolveOriginByPath(jarPath, rules);
            }
            out.put(jar.getJid(), origin);
        }
        return out.isEmpty() ? empty() : new ProjectJarOriginResolver(out);
    }

    ProjectOrigin resolve(int jarId) {
        if (jarId <= 0) {
            return ProjectOrigin.APP;
        }
        ProjectOrigin origin = jarOrigins.get(jarId);
        return origin == null ? ProjectOrigin.APP : origin;
    }

    ProjectOrigin resolve(Integer jarId) {
        if (jarId == null) {
            return ProjectOrigin.APP;
        }
        return resolve(jarId.intValue());
    }

    private static List<OriginPathRule> collectOriginRules(ProjectModel model) {
        List<OriginPathRule> out = new ArrayList<>();
        if (model == null) {
            return out;
        }
        if (model.roots() != null) {
            for (ProjectRoot root : model.roots()) {
                if (root == null || root.path() == null) {
                    continue;
                }
                ProjectOrigin origin = root.origin();
                if (origin == null || origin == ProjectOrigin.UNKNOWN) {
                    origin = root.kind() == null ? ProjectOrigin.APP : root.kind().defaultOrigin();
                }
                out.add(new OriginPathRule(root.path(), origin));
            }
        }
        if (model.analyzedArchives() != null) {
            for (Path archive : model.analyzedArchives()) {
                if (archive == null) {
                    continue;
                }
                ProjectOrigin origin = resolveOriginByJarName(archive);
                if (origin == ProjectOrigin.UNKNOWN) {
                    origin = resolveOriginByPath(archive, out);
                    if (origin == ProjectOrigin.APP
                            && model.primaryInputPath() != null
                            && !archive.startsWith(model.primaryInputPath())) {
                        if (model.runtimePath() != null && archive.startsWith(model.runtimePath())) {
                            origin = ProjectOrigin.SDK;
                        } else {
                            origin = ProjectOrigin.LIBRARY;
                        }
                    }
                }
                out.add(new OriginPathRule(archive, origin));
            }
        }
        out.sort(Comparator.comparingInt((OriginPathRule item) -> item.path().getNameCount()).reversed());
        return out;
    }

    private static ProjectOrigin resolveOriginByJarName(Path jarPath) {
        if (jarPath == null || jarPath.getFileName() == null) {
            return ProjectOrigin.UNKNOWN;
        }
        String fileName = jarPath.getFileName().toString();
        if (AnalysisScopeRules.isForceTargetJar(fileName)) {
            return ProjectOrigin.APP;
        }
        if (AnalysisScopeRules.isSdkJar(fileName)) {
            return ProjectOrigin.SDK;
        }
        if (AnalysisScopeRules.isCommonLibraryJar(fileName)) {
            return ProjectOrigin.LIBRARY;
        }
        return ProjectOrigin.UNKNOWN;
    }

    private static ProjectOrigin resolveOriginByPath(Path path, List<OriginPathRule> rules) {
        if (path == null || rules == null || rules.isEmpty()) {
            return ProjectOrigin.APP;
        }
        for (OriginPathRule rule : rules) {
            if (rule == null || rule.path() == null) {
                continue;
            }
            try {
                if (path.startsWith(rule.path())) {
                    return rule.origin();
                }
            } catch (Exception ignored) {
                // GUI scope derivation should stay best-effort.
            }
        }
        return ProjectOrigin.APP;
    }

    private static Path normalizePath(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.isBlank()) {
            return null;
        }
        try {
            return Paths.get(value).toAbsolutePath().normalize();
        } catch (Exception ex) {
            try {
                return Paths.get(value).normalize();
            } catch (Exception ignored) {
                return null;
            }
        }
    }

    private record OriginPathRule(Path path, ProjectOrigin origin) {
    }
}
