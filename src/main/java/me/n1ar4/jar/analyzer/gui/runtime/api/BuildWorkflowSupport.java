package me.n1ar4.jar.analyzer.gui.runtime.api;

import me.n1ar4.jar.analyzer.config.ConfigEngine;
import me.n1ar4.jar.analyzer.config.ConfigFile;
import me.n1ar4.jar.analyzer.core.CoreRunner;
import me.n1ar4.jar.analyzer.engine.CFRDecompileEngine;
import me.n1ar4.jar.analyzer.engine.CoreEngine;
import me.n1ar4.jar.analyzer.engine.EngineContext;
import me.n1ar4.jar.analyzer.engine.project.ProjectBuildMode;
import me.n1ar4.jar.analyzer.engine.project.ProjectModel;
import me.n1ar4.jar.analyzer.engine.project.ProjectOrigin;
import me.n1ar4.jar.analyzer.engine.project.ProjectRoot;
import me.n1ar4.jar.analyzer.engine.project.ProjectRootKind;
import me.n1ar4.jar.analyzer.gui.GlobalOptions;
import me.n1ar4.jar.analyzer.gui.runtime.model.BuildSettingsDto;
import me.n1ar4.jar.analyzer.storage.neo4j.ActiveProjectContext;
import me.n1ar4.jar.analyzer.storage.neo4j.ProjectGraphStoreFacade;
import me.n1ar4.jar.analyzer.storage.neo4j.ProjectRegistryService;
import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.jar.analyzer.utils.BuildToolClasspathResolver;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.BiFunction;

final class BuildWorkflowSupport {
    private static final Logger logger = LogManager.getLogger();

    private final BiFunction<String, String, String> translator;
    private final Services services;

    BuildWorkflowSupport(BiFunction<String, String, String> translator) {
        this(translator, Services.system());
    }

    BuildWorkflowSupport(BiFunction<String, String, String> translator, Services services) {
        this.translator = translator == null ? (zh, en) -> safe(zh) : translator;
        this.services = services == null ? Services.system() : services;
    }

    BuildInputResolution resolveBuildInput(BuildSettingsDto settings) {
        if (settings == null) {
            return BuildInputResolution.error(tr("构建设置为空", "build settings is empty"));
        }
        String inputPath = safe(settings.activeInputPath()).trim();
        if (inputPath.isEmpty()) {
            return BuildInputResolution.error(tr("输入路径为空", "input path is empty"));
        }
        Path selectedInput = Paths.get(inputPath).toAbsolutePath().normalize();
        if (Files.notExists(selectedInput)) {
            return BuildInputResolution.error(tr("输入路径不存在", "input path not exists"));
        }
        if (Files.isRegularFile(selectedInput) && !isArchiveOrClassFile(selectedInput)) {
            return BuildInputResolution.error(tr(
                    "输入文件必须是 .jar/.war/.class 或目录(含字节码)",
                    "input file must be .jar/.war/.class or a bytecode directory"
            ));
        }

        Path projectRoot = resolveLikelyProjectRoot(selectedInput);
        boolean projectLayout = projectRoot != null
                && Files.isDirectory(projectRoot)
                && isLikelyProjectLayout(projectRoot);
        Path normalizedProjectRoot = projectRoot == null
                ? null
                : projectRoot.toAbsolutePath().normalize();

        List<Path> extraClasspath = collectProjectExtraClasspath(normalizedProjectRoot);
        Path analysisInput = resolveAnalyzableInput(selectedInput, normalizedProjectRoot);
        if (analysisInput == null) {
            String missing = Files.isDirectory(selectedInput)
                    ? tr("输入目录中没有可分析的字节码（.class/.jar/.war）",
                    "input directory has no analyzable bytecode (.class/.jar/.war)")
                    : tr("输入中没有可分析的字节码（.class/.jar/.war）",
                    "input has no analyzable bytecode (.class/.jar/.war)");
            return BuildInputResolution.error(missing);
        }

        return BuildInputResolution.ok(
                analysisInput.toAbsolutePath().normalize(),
                selectedInput,
                normalizedProjectRoot,
                projectLayout,
                extraClasspath
        );
    }

    SdkResolution resolveSdk(BuildSettingsDto settings) {
        if (settings == null) {
            return SdkResolution.none();
        }
        String raw = safe(settings.sdkPath()).trim();
        if (raw.isEmpty()) {
            return SdkResolution.none();
        }
        Path sdk = Paths.get(raw).toAbsolutePath().normalize();
        if (Files.notExists(sdk)) {
            return SdkResolution.error(tr("SDK 路径不存在", "sdk path not exists"));
        }
        return SdkResolution.ok(sdk);
    }

    void ensureActiveProject(BuildSettingsDto settings,
                             BuildInputResolution inputResolution,
                             Path workspaceSdkPath) {
        String inputPath = "";
        if (inputResolution != null && inputResolution.selectedInputPath() != null) {
            inputPath = inputResolution.selectedInputPath().toString();
        }
        if (inputPath.isBlank() && settings != null) {
            inputPath = settings.activeInputPath();
        }
        String runtime = workspaceSdkPath == null ? "" : workspaceSdkPath.toString();
        boolean nested = settings != null && settings.resolveNestedJars();
        ProjectRegistryService service = services.projectRegistryService();
        service.upsertActiveProjectBuildSettings("", inputPath, runtime, nested);
        if (inputResolution == null) {
            return;
        }
        if (inputResolution.projectLayout()) {
            Path projectRoot = inputResolution.projectRootPath() == null
                    ? inputResolution.selectedInputPath()
                    : inputResolution.projectRootPath();
            Path analysisInput = inputResolution.inputPath() == null
                    ? inputResolution.selectedInputPath()
                    : inputResolution.inputPath();
            service.publishActiveProjectModel(buildProjectModel(
                    settings,
                    projectRoot,
                    analysisInput,
                    workspaceSdkPath,
                    inputResolution.extraClasspath()
            ));
            return;
        }
        Path artifactInput = inputResolution.inputPath() == null
                ? inputResolution.selectedInputPath()
                : inputResolution.inputPath();
        if (artifactInput != null) {
            Path normalized = artifactInput.toAbsolutePath().normalize();
            service.publishActiveProjectModel(ProjectModel.artifact(
                    normalized,
                    workspaceSdkPath,
                    List.of(normalized),
                    nested
            ));
        }
    }

    void saveBuildConfig(String jarPath, CoreRunner.BuildResult result, int language) {
        ConfigFile cfg = ConfigEngine.parseConfig();
        if (cfg == null) {
            cfg = new ConfigFile();
        }
        cfg.setJarPath(jarPath);
        Path projectStore = services.projectStore()
                .resolveProjectHome(ActiveProjectContext.getActiveProjectKey());
        cfg.setDbPath(projectStore.toString());
        cfg.setTempPath(Const.tempDir);
        cfg.setTotalJar(String.valueOf(result.getJarCount()));
        cfg.setTotalClass(String.valueOf(result.getClassCount()));
        cfg.setTotalMethod(String.valueOf(result.getMethodCount()));
        cfg.setTotalEdge(String.valueOf(result.getEdgeCount()));
        cfg.setDbSize(result.getDbSizeLabel());
        cfg.setLang(language == GlobalOptions.ENGLISH ? "en" : "zh");
        cfg.setDecompileCacheSize(String.valueOf(CFRDecompileEngine.getCacheCapacity()));
        ConfigEngine.saveConfig(cfg);
    }

    String resolveCurrentEdgeCount() {
        try {
            CoreEngine engine = services.currentEngine();
            if (engine != null && engine.isEnabled()) {
                return String.valueOf(engine.getCallGraphCache().getEdgeCount());
            }
        } catch (Throwable ex) {
            logger.debug("resolve current edge count failed: {}", ex.toString());
        }
        return "0";
    }

    long resolveActiveStoreSize() {
        Path projectStore = services.projectStore()
                .resolveProjectHome(ActiveProjectContext.getActiveProjectKey());
        if (projectStore == null || Files.notExists(projectStore)) {
            return 0L;
        }
        try (var walk = Files.walk(projectStore)) {
            return walk
                    .filter(Files::isRegularFile)
                    .mapToLong(path -> {
                        try {
                            return Files.size(path);
                        } catch (Exception ex) {
                            return 0L;
                        }
                    })
                    .sum();
        } catch (Exception ex) {
            logger.debug("resolve active store size failed: {}", ex.toString());
            return 0L;
        }
    }

    interface Services {
        CoreEngine currentEngine();

        ProjectRegistryService projectRegistryService();

        ProjectGraphStoreFacade projectStore();

        static Services system() {
            return new Services() {
                @Override
                public CoreEngine currentEngine() {
                    return EngineContext.getEngine();
                }

                @Override
                public ProjectRegistryService projectRegistryService() {
                    return ProjectRegistryService.getInstance();
                }

                @Override
                public ProjectGraphStoreFacade projectStore() {
                    return ProjectGraphStoreFacade.getInstance();
                }
            };
        }
    }

    String formatDatabaseSize(long bytes) {
        double sizeMb = (double) bytes / (1024 * 1024);
        return String.format(Locale.ROOT, "%.2f MB", sizeMb);
    }

    private boolean hasAnalyzableBytecode(Path root) {
        if (root == null || !Files.isDirectory(root)) {
            return false;
        }
        try (var stream = Files.walk(root, 8)) {
            return stream.anyMatch(path -> Files.isRegularFile(path) && isArchiveOrClassFile(path));
        } catch (Exception ex) {
            logger.debug("scan project bytecode failed: {}", ex.toString());
            return false;
        }
    }

    private Path resolveLikelyProjectRoot(Path input) {
        if (input == null) {
            return null;
        }
        Path cursor = Files.isDirectory(input) ? input : input.getParent();
        if (cursor == null) {
            return null;
        }
        Path normalized = cursor.toAbsolutePath().normalize();
        Path current = normalized;
        for (int i = 0; i < 8 && current != null; i++) {
            if (isLikelyProjectLayout(current)) {
                return current;
            }
            current = current.getParent();
        }
        return normalized;
    }

    private boolean isLikelyProjectLayout(Path root) {
        if (root == null || !Files.isDirectory(root)) {
            return false;
        }
        return Files.exists(root.resolve("pom.xml"))
                || Files.exists(root.resolve("build.gradle"))
                || Files.exists(root.resolve("build.gradle.kts"))
                || Files.exists(root.resolve("settings.gradle"))
                || Files.exists(root.resolve("settings.gradle.kts"))
                || Files.exists(root.resolve(".idea"))
                || Files.exists(root.resolve("src"));
    }

    private Path resolveAnalyzableInput(Path selectedInput, Path projectRoot) {
        if (selectedInput == null) {
            return null;
        }
        if (Files.isRegularFile(selectedInput) && isArchiveOrClassFile(selectedInput)) {
            return selectedInput;
        }
        Path directory = selectedInput;
        if (directory != null && Files.isDirectory(directory) && hasAnalyzableBytecode(directory)) {
            return directory;
        }

        Path base = projectRoot;
        if (base == null || !Files.isDirectory(base)) {
            return null;
        }
        Path[] candidates = new Path[]{
                base.resolve(Paths.get("target", "classes")),
                base.resolve(Paths.get("target", "test-classes")),
                base.resolve(Paths.get("build", "classes")),
                base.resolve(Paths.get("build", "classes", "java", "main")),
                base.resolve(Paths.get("build", "classes", "kotlin", "main")),
                base.resolve(Paths.get("out", "production")),
                base.resolve(Paths.get("out", "classes")),
                base.resolve("bin")
        };
        for (Path candidate : candidates) {
            if (Files.isDirectory(candidate) && hasAnalyzableBytecode(candidate)) {
                return candidate.toAbsolutePath().normalize();
            }
        }
        return null;
    }

    private List<Path> collectProjectExtraClasspath(Path projectRoot) {
        LinkedHashSet<Path> out = new LinkedHashSet<>();
        if (projectRoot != null && Files.isDirectory(projectRoot)) {
            try {
                out.addAll(BuildToolClasspathResolver.resolveProjectClasspath(projectRoot));
            } catch (Exception ex) {
                logger.debug("resolve build-tool classpath failed: {}", ex.toString());
            }
            out.addAll(collectConventionalProjectClasspath(projectRoot));
        }
        return out.isEmpty() ? List.of() : List.copyOf(out);
    }

    private List<Path> collectConventionalProjectClasspath(Path projectRoot) {
        if (projectRoot == null || !Files.isDirectory(projectRoot)) {
            return List.of();
        }
        LinkedHashSet<Path> out = new LinkedHashSet<>();
        Path[] jarDirs = new Path[]{
                projectRoot.resolve("lib"),
                projectRoot.resolve("libs"),
                projectRoot.resolve(Paths.get("target", "dependency")),
                projectRoot.resolve(Paths.get("build", "libs"))
        };
        for (Path dir : jarDirs) {
            if (dir == null || !Files.isDirectory(dir)) {
                continue;
            }
            try (var stream = Files.walk(dir, 4)) {
                stream.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".jar"))
                        .map(path -> path.toAbsolutePath().normalize())
                        .forEach(out::add);
            } catch (Exception ex) {
                logger.debug("scan project jar classpath failed: {}", ex.toString());
            }
        }
        Path[] classDirs = new Path[]{
                projectRoot.resolve(Paths.get("target", "classes")),
                projectRoot.resolve(Paths.get("target", "test-classes")),
                projectRoot.resolve(Paths.get("build", "classes")),
                projectRoot.resolve(Paths.get("build", "classes", "java", "main")),
                projectRoot.resolve(Paths.get("build", "classes", "kotlin", "main")),
                projectRoot.resolve(Paths.get("out", "production")),
                projectRoot.resolve(Paths.get("out", "test")),
                projectRoot.resolve("bin")
        };
        for (Path dir : classDirs) {
            if (dir != null && Files.isDirectory(dir)) {
                out.add(dir.toAbsolutePath().normalize());
            }
        }
        return out.isEmpty() ? List.of() : List.copyOf(out);
    }

    private ProjectModel buildProjectModel(BuildSettingsDto settings,
                                           Path projectRoot,
                                           Path analysisInputPath,
                                           Path workspaceSdkPath,
                                           List<Path> extraClasspath) {
        Path normalizedProjectRoot = projectRoot == null ? null : projectRoot.toAbsolutePath().normalize();
        Path normalizedAnalysisInput = analysisInputPath == null
                ? normalizedProjectRoot
                : analysisInputPath.toAbsolutePath().normalize();
        ProjectModel.Builder builder = ProjectModel.builder()
                .buildMode(ProjectBuildMode.PROJECT)
                .primaryInputPath(normalizedAnalysisInput)
                .runtimePath(workspaceSdkPath)
                .resolveInnerJars(settings != null && settings.resolveNestedJars());
        if (normalizedProjectRoot != null) {
            builder.addRoot(new ProjectRoot(
                    ProjectRootKind.CONTENT_ROOT,
                    ProjectOrigin.APP,
                    normalizedProjectRoot,
                    "",
                    false,
                    false,
                    10
            ));
            addProjectConventionalRoots(builder, normalizedProjectRoot);
        }
        if (normalizedAnalysisInput != null
                && !Objects.equals(normalizedAnalysisInput, normalizedProjectRoot)
                && Files.exists(normalizedAnalysisInput)) {
            builder.addRoot(new ProjectRoot(
                    ProjectRootKind.GENERATED,
                    ProjectOrigin.GENERATED,
                    normalizedAnalysisInput,
                    "",
                    Files.isRegularFile(normalizedAnalysisInput),
                    false,
                    15
            ));
        }
        if (workspaceSdkPath != null && Files.exists(workspaceSdkPath)) {
            builder.addRoot(new ProjectRoot(
                    ProjectRootKind.SDK,
                    ProjectOrigin.SDK,
                    workspaceSdkPath,
                    "",
                    Files.isRegularFile(workspaceSdkPath),
                    false,
                    100
            ));
        }
        addResolvedLibraryRoots(builder, normalizedProjectRoot, extraClasspath);
        return builder.build();
    }

    private void addResolvedLibraryRoots(ProjectModel.Builder builder,
                                         Path projectRoot,
                                         List<Path> extraClasspath) {
        if (builder == null || extraClasspath == null || extraClasspath.isEmpty()) {
            return;
        }
        int priority = 110;
        for (Path entry : extraClasspath) {
            if (entry == null || Files.notExists(entry)) {
                continue;
            }
            Path normalized = entry.toAbsolutePath().normalize();
            if (projectRoot != null) {
                try {
                    if (normalized.startsWith(projectRoot)) {
                        continue;
                    }
                } catch (Exception ignored) {
                    logger.debug("skip project-root library check failed: {}", ignored.toString());
                }
            }
            addRootIfExists(
                    builder,
                    normalized,
                    ProjectRootKind.LIBRARY,
                    ProjectOrigin.LIBRARY,
                    false,
                    priority++
            );
        }
    }

    private void addProjectConventionalRoots(ProjectModel.Builder builder, Path projectRoot) {
        if (builder == null || projectRoot == null) {
            return;
        }
        addRootIfExists(builder, projectRoot.resolve(Paths.get("src", "main", "java")),
                ProjectRootKind.SOURCE_ROOT, ProjectOrigin.APP, false, 20);
        addRootIfExists(builder, projectRoot.resolve(Paths.get("src", "main", "resources")),
                ProjectRootKind.RESOURCE_ROOT, ProjectOrigin.APP, false, 25);
        addRootIfExists(builder, projectRoot.resolve(Paths.get("src", "main", "webapp")),
                ProjectRootKind.RESOURCE_ROOT, ProjectOrigin.APP, false, 26);
        addRootIfExists(builder, projectRoot.resolve(Paths.get("src", "test", "java")),
                ProjectRootKind.SOURCE_ROOT, ProjectOrigin.APP, true, 30);
        addRootIfExists(builder, projectRoot.resolve(Paths.get("src", "test", "resources")),
                ProjectRootKind.RESOURCE_ROOT, ProjectOrigin.APP, true, 35);
        addRootIfExists(builder, projectRoot.resolve(Paths.get("src", "test", "webapp")),
                ProjectRootKind.RESOURCE_ROOT, ProjectOrigin.APP, true, 36);
        addRootIfExists(builder, projectRoot.resolve(Paths.get("target", "generated-sources")),
                ProjectRootKind.GENERATED, ProjectOrigin.GENERATED, false, 40);
        addRootIfExists(builder, projectRoot.resolve(Paths.get("build", "generated")),
                ProjectRootKind.GENERATED, ProjectOrigin.GENERATED, false, 45);
        addRootIfExists(builder, projectRoot.resolve("generated"),
                ProjectRootKind.GENERATED, ProjectOrigin.GENERATED, false, 46);
        addRootIfExists(builder, projectRoot.resolve("lib"),
                ProjectRootKind.LIBRARY, ProjectOrigin.LIBRARY, false, 50);
        addRootIfExists(builder, projectRoot.resolve("libs"),
                ProjectRootKind.LIBRARY, ProjectOrigin.LIBRARY, false, 55);
        addRootIfExists(builder, projectRoot.resolve("out"),
                ProjectRootKind.GENERATED, ProjectOrigin.GENERATED, false, 60);
        addRootIfExists(builder, projectRoot.resolve("target"),
                ProjectRootKind.GENERATED, ProjectOrigin.GENERATED, false, 65);
        addRootIfExists(builder, projectRoot.resolve("build"),
                ProjectRootKind.GENERATED, ProjectOrigin.GENERATED, false, 70);
        addRootIfExists(builder, projectRoot.resolve(".git"),
                ProjectRootKind.EXCLUDED, ProjectOrigin.EXCLUDED, false, 90);
        addRootIfExists(builder, projectRoot.resolve(".idea"),
                ProjectRootKind.EXCLUDED, ProjectOrigin.EXCLUDED, false, 91);
    }

    private void addRootIfExists(ProjectModel.Builder builder,
                                 Path path,
                                 ProjectRootKind kind,
                                 ProjectOrigin origin,
                                 boolean test,
                                 int priority) {
        if (builder == null || path == null || Files.notExists(path)) {
            return;
        }
        builder.addRoot(new ProjectRoot(
                kind,
                origin,
                path,
                "",
                Files.isRegularFile(path),
                test,
                priority
        ));
    }

    private boolean isArchiveOrClassFile(Path path) {
        if (path == null) {
            return false;
        }
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".jar") || name.endsWith(".war") || name.endsWith(".class");
    }

    private String tr(String zh, String en) {
        String value = translator.apply(zh, en);
        return value == null ? safe(zh) : value;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    record BuildInputResolution(Path inputPath,
                                Path selectedInputPath,
                                Path projectRootPath,
                                boolean projectLayout,
                                List<Path> extraClasspath,
                                String error) {
        BuildInputResolution {
            extraClasspath = extraClasspath == null ? List.of() : List.copyOf(extraClasspath);
            error = safe(error);
        }

        static BuildInputResolution ok(Path inputPath,
                                       Path selectedInputPath,
                                       Path projectRootPath,
                                       boolean projectLayout,
                                       List<Path> extraClasspath) {
            return new BuildInputResolution(
                    inputPath,
                    selectedInputPath,
                    projectRootPath,
                    projectLayout,
                    extraClasspath,
                    ""
            );
        }

        static BuildInputResolution error(String error) {
            return new BuildInputResolution(
                    null,
                    null,
                    null,
                    false,
                    List.of(),
                    error
            );
        }
    }

    record SdkResolution(Path sdkPath, String error) {
        SdkResolution {
            error = safe(error);
        }

        static SdkResolution none() {
            return new SdkResolution(null, "");
        }

        static SdkResolution ok(Path sdkPath) {
            return new SdkResolution(sdkPath, "");
        }

        static SdkResolution error(String error) {
            return new SdkResolution(null, error);
        }
    }
}
