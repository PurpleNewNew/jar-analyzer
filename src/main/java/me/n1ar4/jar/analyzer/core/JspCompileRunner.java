/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */
package me.n1ar4.jar.analyzer.core;

import me.n1ar4.jar.analyzer.engine.project.ProjectBuildMode;
import me.n1ar4.jar.analyzer.engine.project.ProjectModel;
import me.n1ar4.jar.analyzer.engine.project.ProjectRoot;
import me.n1ar4.jar.analyzer.engine.project.ProjectRootKind;
import me.n1ar4.jar.analyzer.entity.ClassFileEntity;
import me.n1ar4.jar.analyzer.entity.ResourceEntity;
import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.jar.analyzer.utils.BytecodeCache;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import org.apache.jasper.JspC;
import org.objectweb.asm.ClassReader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class JspCompileRunner {
    private static final Logger logger = LogManager.getLogger();
    private static final String EXTRA_CLASSPATH_PROP = "jar.analyzer.classpath.extra";

    private JspCompileRunner() {
    }

    public static List<ClassFileEntity> compile(ProjectModel projectModel,
                                                Collection<ResourceEntity> resources,
                                                Collection<String> userArchives) {
        List<CompileRoot> roots = discoverRoots(projectModel, resources);
        if (roots.isEmpty()) {
            return List.of();
        }
        String classpath = resolveClasspath(projectModel, userArchives);
        List<ClassFileEntity> out = new ArrayList<>();
        for (CompileRoot root : roots) {
            out.addAll(compileRoot(root, classpath));
        }
        if (!out.isEmpty()) {
            logger.info("jsp compile stage: roots={} classes={}", roots.size(), out.size());
        }
        return out.isEmpty() ? List.of() : List.copyOf(out);
    }

    private static List<CompileRoot> discoverRoots(ProjectModel projectModel,
                                                   Collection<ResourceEntity> resources) {
        Map<String, CompileRoot> roots = new LinkedHashMap<>();
        if (resources != null && !resources.isEmpty()) {
            for (ResourceEntity resource : resources) {
                if (resource == null || !isJspResource(resource.getResourcePath())) {
                    continue;
                }
                Path uriRoot = resolveResourceUriRoot(resource);
                if (uriRoot == null || Files.notExists(uriRoot)) {
                    continue;
                }
                int jarId = resource.getJarId() == null ? -1 : resource.getJarId();
                String jarName = safe(resource.getJarName());
                roots.putIfAbsent(rootKey(uriRoot, jarId), new CompileRoot(uriRoot, jarId, jarName));
            }
        }
        if (projectModel != null && projectModel.buildMode() == ProjectBuildMode.PROJECT) {
            for (Path root : discoverProjectRoots(projectModel)) {
                if (root == null || Files.notExists(root)) {
                    continue;
                }
                roots.putIfAbsent(rootKey(root, -1), new CompileRoot(root, -1, "project-jsp"));
            }
        }
        return roots.isEmpty() ? List.of() : List.copyOf(roots.values());
    }

    private static List<Path> discoverProjectRoots(ProjectModel projectModel) {
        LinkedHashSet<Path> out = new LinkedHashSet<>();
        if (projectModel == null) {
            return List.of();
        }
        Path contentRoot = null;
        if (projectModel.roots() != null) {
            for (ProjectRoot root : projectModel.roots()) {
                if (root == null || root.path() == null) {
                    continue;
                }
                if (root.kind() == ProjectRootKind.CONTENT_ROOT && contentRoot == null) {
                    contentRoot = root.path();
                }
                if (root.kind() == ProjectRootKind.RESOURCE_ROOT && containsJspFiles(root.path(), 8)) {
                    out.add(root.path());
                }
            }
        }
        if (contentRoot != null) {
            Path[] candidates = new Path[]{
                    contentRoot.resolve(Paths.get("src", "main", "webapp")),
                    contentRoot.resolve(Paths.get("src", "test", "webapp")),
                    contentRoot.resolve(Paths.get("src", "main", "resources")),
                    contentRoot.resolve(Paths.get("src", "test", "resources"))
            };
            for (Path candidate : candidates) {
                if (containsJspFiles(candidate, 8)) {
                    out.add(candidate.toAbsolutePath().normalize());
                }
            }
        }
        return out.isEmpty() ? List.of() : List.copyOf(out);
    }

    private static List<ClassFileEntity> compileRoot(CompileRoot root, String classpath) {
        if (root == null || root.uriRoot() == null || Files.notExists(root.uriRoot())) {
            return List.of();
        }
        Path outputDir = Paths.get(Const.tempDir, "jsp-compiled",
                Integer.toHexString((root.uriRoot().toString() + "#" + root.jarId()).hashCode()));
        try {
            Files.createDirectories(outputDir);
            JspC jspc = new JspC();
            jspc.setCompile(true);
            jspc.setClassDebugInfo(false);
            jspc.setFailOnError(false);
            jspc.setOutputDir(outputDir.toString());
            if (!classpath.isBlank()) {
                jspc.setClassPath(classpath);
            }
            jspc.setUriroot(root.uriRoot().toString());
            jspc.execute();
            return collectCompiledClasses(outputDir, root);
        } catch (Exception ex) {
            logger.warn("jsp compile failed: root={} err={}", root.uriRoot(), ex.toString());
            return List.of();
        }
    }

    private static List<ClassFileEntity> collectCompiledClasses(Path outputDir, CompileRoot root) {
        if (outputDir == null || Files.notExists(outputDir)) {
            return List.of();
        }
        List<ClassFileEntity> out = new ArrayList<>();
        try (var stream = Files.walk(outputDir)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".class"))
                    .forEach(path -> {
                        try {
                            byte[] bytes = Files.readAllBytes(path);
                            if (bytes.length == 0) {
                                return;
                            }
                            ClassReader reader = new ClassReader(bytes);
                            String internalName = reader.getClassName();
                            if (internalName == null || internalName.isBlank()) {
                                return;
                            }
                            BytecodeCache.preload(path, bytes);
                            ClassFileEntity classFile = new ClassFileEntity(internalName + ".class", path, root.jarId());
                            classFile.setJarName(root.jarName());
                            classFile.setCachedBytes(bytes);
                            out.add(classFile);
                        } catch (Exception ex) {
                            logger.debug("read compiled jsp class failed: {} ({})", path, ex.toString());
                        }
                    });
        } catch (Exception ex) {
            logger.debug("scan compiled jsp classes failed: {} ({})", outputDir, ex.toString());
        }
        return out.isEmpty() ? List.of() : List.copyOf(out);
    }

    private static String resolveClasspath(ProjectModel projectModel, Collection<String> userArchives) {
        LinkedHashSet<String> entries = new LinkedHashSet<>();
        if (projectModel != null) {
            addPath(entries, projectModel.primaryInputPath());
            addPath(entries, projectModel.runtimePath());
            if (projectModel.roots() != null) {
                for (ProjectRoot root : projectModel.roots()) {
                    if (root == null || root.path() == null) {
                        continue;
                    }
                    ProjectRootKind kind = root.kind();
                    if (kind == ProjectRootKind.GENERATED
                            || kind == ProjectRootKind.LIBRARY
                            || kind == ProjectRootKind.SDK) {
                        addPath(entries, root.path());
                    }
                }
            }
        }
        if (userArchives != null) {
            for (String archive : userArchives) {
                Path path = safePath(archive);
                if (path == null || Files.notExists(path)) {
                    continue;
                }
                if (Files.isRegularFile(path) && path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".class")) {
                    addPath(entries, path.getParent());
                } else {
                    addPath(entries, path);
                }
            }
        }
        String extra = System.getProperty(EXTRA_CLASSPATH_PROP);
        if (extra != null && !extra.isBlank()) {
            String[] parts = extra.split(java.util.regex.Pattern.quote(java.io.File.pathSeparator));
            for (String part : parts) {
                addPath(entries, safePath(part));
            }
        }
        return entries.isEmpty() ? "" : String.join(java.io.File.pathSeparator, entries);
    }

    private static Path resolveResourceUriRoot(ResourceEntity resource) {
        if (resource == null) {
            return null;
        }
        Path full = safePath(resource.getPathStr());
        String resourcePath = safe(resource.getResourcePath()).replace('\\', '/');
        if (full == null || resourcePath.isBlank()) {
            return null;
        }
        int segments = resourcePath.split("/").length;
        Path root = full;
        for (int i = 0; i < segments; i++) {
            root = root.getParent();
            if (root == null) {
                return null;
            }
        }
        return root.toAbsolutePath().normalize();
    }

    private static boolean containsJspFiles(Path root, int maxDepth) {
        if (root == null || Files.notExists(root) || !Files.isDirectory(root)) {
            return false;
        }
        try (var stream = Files.walk(root, Math.max(1, maxDepth))) {
            return stream.anyMatch(path -> Files.isRegularFile(path)
                    && isJspResource(path.getFileName() == null ? null : path.getFileName().toString()));
        } catch (Exception ex) {
            logger.debug("scan jsp files failed: {} ({})", root, ex.toString());
            return false;
        }
    }

    private static boolean isJspResource(String path) {
        String value = safe(path).toLowerCase(Locale.ROOT);
        return value.endsWith(".jsp")
                || value.endsWith(".jspx")
                || value.endsWith(".tag")
                || value.endsWith(".tagx");
    }

    private static String rootKey(Path path, int jarId) {
        Path normalized = path == null ? null : path.toAbsolutePath().normalize();
        return (normalized == null ? "" : normalized.toString()) + "#" + jarId;
    }

    private static void addPath(Set<String> entries, Path path) {
        if (entries == null || path == null || Files.notExists(path)) {
            return;
        }
        entries.add(path.toAbsolutePath().normalize().toString());
    }

    private static Path safePath(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Path.of(raw).toAbsolutePath().normalize();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private record CompileRoot(Path uriRoot, int jarId, String jarName) {
    }
}
