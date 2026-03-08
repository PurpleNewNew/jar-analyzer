/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.storage.neo4j;

import me.n1ar4.jar.analyzer.analyze.spring.SpringController;
import me.n1ar4.jar.analyzer.analyze.spring.SpringMapping;
import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.core.ProjectRuntimeSnapshot;
import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.engine.WorkspaceContext;
import me.n1ar4.jar.analyzer.engine.project.ProjectModel;
import me.n1ar4.jar.analyzer.storage.neo4j.ActiveProjectContext;
import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.jar.analyzer.entity.CallSiteEntity;
import me.n1ar4.jar.analyzer.entity.ClassFileEntity;
import me.n1ar4.jar.analyzer.entity.LocalVarEntity;
import me.n1ar4.jar.analyzer.entity.ResourceEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectMetadataSnapshotStoreTest {
    private final ProjectMetadataSnapshotStore store = ProjectMetadataSnapshotStore.getInstance();
    private final String projectKey = "snapshot-" + Long.toHexString(System.nanoTime());

    @AfterEach
    void cleanup() {
        DatabaseManager.clearAllData();
        WorkspaceContext.clear();
        ActiveProjectContext.setActiveProject(
                ActiveProjectContext.temporaryProjectKey(),
                ActiveProjectContext.temporaryProjectAlias()
        );
        Neo4jProjectStore.getInstance().deleteProjectStore(projectKey);
    }

    @Test
    void shouldRoundTripProjectRuntimeMetadata() {
        ProjectModel model = ProjectModel.artifact(
                Path.of("/tmp/jar-analyzer/app.jar"),
                Path.of("/tmp/jar-analyzer/rt.jar"),
                List.of(Path.of("/tmp/jar-analyzer/app.jar")),
                false
        );
        ClassReference classRef = new ClassReference(
                61,
                1,
                "demo/Controller",
                "java/lang/Object",
                List.of(),
                false,
                List.of(),
                Set.of(),
                "app.jar",
                1
        );
        MethodReference methodRef = new MethodReference(
                new ClassReference.Handle("demo/Controller", 1),
                "index",
                "()V",
                false,
                Set.of(),
                1,
                12,
                "app.jar",
                1
        );
        SpringController controller = new SpringController();
        controller.setRest(true);
        controller.setBasePath("/api");
        controller.setClassName(new ClassReference.Handle("demo/Controller", 1));
        controller.setClassReference(classRef);
        SpringMapping mapping = new SpringMapping();
        mapping.setController(controller);
        mapping.setRest(true);
        mapping.setMethodReference(methodRef);
        mapping.setMethodName(methodRef.getHandle());
        mapping.setPath("/index");
        mapping.setPathRestful("/api/index");
        controller.addMapping(mapping);

        ProjectRuntimeSnapshot snapshot = DatabaseManager.buildProjectRuntimeSnapshot(
                42L,
                model,
                List.of("/tmp/jar-analyzer/app.jar"),
                new LinkedHashSet<>(List.of(classFile())),
                new LinkedHashSet<>(List.of(classRef)),
                new LinkedHashSet<>(List.of(methodRef)),
                java.util.Map.of(methodRef.getHandle(), List.of("jdbc:demo")),
                java.util.Map.of(),
                List.of(resource()),
                List.of(callSite()),
                List.of(localVar()),
                List.of(controller),
                List.of("demo/Interceptor"),
                List.of("demo/Servlet"),
                List.of("demo/Filter"),
                List.of("demo/Listener")
        );

        ProjectMetadataSnapshotStoreTestHook.write(projectKey, snapshot);
        DatabaseManager.clearAllData();
        WorkspaceContext.clear();

        ProjectRuntimeSnapshot restored = store.read(projectKey);
        assertNotNull(restored);
        loadRuntimeSnapshot(projectKey, restored);
        assertEquals(projectKey, ActiveProjectContext.getPublishedActiveProjectKey());
        assertEquals(42L, DatabaseManager.getProjectBuildSeq());
        assertNotNull(DatabaseManager.getProjectModel());
        assertEquals("/tmp/jar-analyzer/app.jar", DatabaseManager.getProjectModel().primaryInputPath().toString());
        assertEquals(1, DatabaseManager.getClassFiles().size());
        assertEquals(1, DatabaseManager.getMethodReferences().size());
        assertEquals(List.of("jdbc:demo"),
                DatabaseManager.getMethodStringValues("demo/Controller", "index", "()V", 1));
        assertEquals(1, DatabaseManager.getSpringControllers().size());
        assertEquals("/api/index",
                DatabaseManager.getSpringControllers().get(0).getMappings().get(0).getPathRestful());
        assertTrue(DatabaseManager.getSpringInterceptors().contains("demo/Interceptor"));
        assertTrue(DatabaseManager.getServlets().contains("demo/Servlet"));
        assertTrue(DatabaseManager.getFilters().contains("demo/Filter"));
        assertTrue(DatabaseManager.getListeners().contains("demo/Listener"));
        assertFalse(DatabaseManager.getCallSites().isEmpty());
        assertFalse(DatabaseManager.getResources().isEmpty());
        assertEquals(model.primaryInputPath(), WorkspaceContext.primaryInputPath());
    }

    @Test
    void shouldMaterializeTemporaryRuntimeFilesIntoProjectHome() throws Exception {
        Path tempRoot = Path.of(Const.tempDir, "snapshot-store-" + Long.toHexString(System.nanoTime()))
                .toAbsolutePath()
                .normalize();
        Path classPath = tempRoot.resolve("classes/demo/TempController.class");
        Path resourcePath = tempRoot.resolve("resources/app/application.properties");
        Files.createDirectories(classPath.getParent());
        Files.createDirectories(resourcePath.getParent());
        Files.write(classPath, new byte[]{0x01, 0x23, 0x45});
        Files.writeString(resourcePath, "demo.key=value", StandardCharsets.UTF_8);

        ProjectModel model = ProjectModel.artifact(
                Path.of("/tmp/jar-analyzer/temp-app.jar"),
                null,
                List.of(Path.of("/tmp/jar-analyzer/temp-app.jar")),
                false
        );
        ClassReference classRef = new ClassReference(
                61,
                1,
                "demo/TempController",
                "java/lang/Object",
                List.of(),
                false,
                List.of(),
                Set.of(),
                "app.jar",
                1
        );
        MethodReference methodRef = new MethodReference(
                new ClassReference.Handle("demo/TempController", 1),
                "run",
                "()V",
                false,
                Set.of(),
                1,
                12,
                "app.jar",
                1
        );
        ClassFileEntity classFile = new ClassFileEntity();
        classFile.setCfId(7);
        classFile.setClassName("demo/TempController");
        classFile.setPath(classPath);
        classFile.setPathStr(classPath.toString());
        classFile.setJarName("app.jar");
        classFile.setJarId(1);

        ResourceEntity resource = new ResourceEntity();
        resource.setRid(9);
        resource.setResourcePath("application.properties");
        resource.setPathStr(resourcePath.toString());
        resource.setJarName("app.jar");
        resource.setJarId(1);
        resource.setFileSize(Files.size(resourcePath));
        resource.setIsText(1);

        ProjectRuntimeSnapshot snapshot = DatabaseManager.buildProjectRuntimeSnapshot(
                55L,
                model,
                List.of("/tmp/jar-analyzer/temp-app.jar"),
                new LinkedHashSet<>(List.of(classFile)),
                new LinkedHashSet<>(List.of(classRef)),
                new LinkedHashSet<>(List.of(methodRef)),
                java.util.Map.of(),
                java.util.Map.of(),
                List.of(resource),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );

        ProjectMetadataSnapshotStoreTestHook.write(projectKey, snapshot);
        ProjectRuntimeSnapshot persisted = store.read(projectKey);
        assertNotNull(persisted);
        String persistedClassPath = persisted.classFiles().get(0).pathStr();
        String persistedResourcePath = persisted.resources().get(0).pathStr();
        Path projectHome = Neo4jProjectStore.getInstance().resolveProjectHome(projectKey);
        assertTrue(persistedClassPath.startsWith(projectHome.toString()));
        assertTrue(persistedResourcePath.startsWith(projectHome.toString()));
        assertTrue(Files.exists(Path.of(persistedClassPath)));
        assertTrue(Files.exists(Path.of(persistedResourcePath)));

        deleteRecursively(tempRoot);
        DatabaseManager.clearAllData();
        WorkspaceContext.clear();

        loadRuntimeSnapshot(projectKey, persisted);
        assertEquals(projectKey, ActiveProjectContext.getPublishedActiveProjectKey());
        assertEquals(persistedClassPath, DatabaseManager.getClassFiles().get(0).getPathStr());
        assertEquals(persistedResourcePath, DatabaseManager.getResources().get(0).getPathStr());
        assertTrue(Files.exists(Path.of(DatabaseManager.getClassFiles().get(0).getPathStr())));
        assertTrue(Files.exists(Path.of(DatabaseManager.getResources().get(0).getPathStr())));
    }

    @Test
    void unavailableMarkerShouldKeepOldSnapshotUnreadable() {
        ProjectModel model = ProjectModel.artifact(
                Path.of("/tmp/jar-analyzer/marker.jar"),
                null,
                List.of(Path.of("/tmp/jar-analyzer/marker.jar")),
                false
        );
        ProjectRuntimeSnapshot snapshot = new ProjectRuntimeSnapshot(
                ProjectRuntimeSnapshot.CURRENT_SCHEMA_VERSION,
                77L,
                new ProjectRuntimeSnapshot.ProjectModelData(
                        model.buildMode().name(),
                        model.primaryInputPath().toString(),
                        "",
                        List.of(),
                        List.of(model.primaryInputPath().toString()),
                        false
                ),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                java.util.Map.of(),
                java.util.Map.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                java.util.Set.of(),
                java.util.Set.of(),
                java.util.Set.of(),
                java.util.Set.of()
        );

        ProjectMetadataSnapshotStoreTestHook.write(projectKey, snapshot);
        store.markUnavailable(projectKey, 78L, "build_started");

        assertTrue(store.isUnavailable(projectKey));
        assertNull(store.read(projectKey));
        assertEquals(0L, store.readBuildSeq(projectKey));
        ProjectRuntimeSnapshot.ProjectModelData modelData = store.readProjectModelRegardlessOfAvailability(projectKey);
        assertNotNull(modelData);
        assertEquals(model.primaryInputPath().toString(), modelData.primaryInputPath());
    }

    @Test
    void writeShouldInvalidateCachedSnapshotEvenWhenFileStampIsReused() throws Exception {
        ProjectRuntimeSnapshot first = snapshotFor("demo/CachedController", 11L);
        ProjectRuntimeSnapshot second = snapshotFor("demo/CachedController", 22L);

        ProjectMetadataSnapshotStoreTestHook.write(projectKey, first);
        ProjectRuntimeSnapshot initialRead = store.read(projectKey);
        assertNotNull(initialRead);
        assertEquals(11L, initialRead.buildSeq());

        Path snapshotFile = store.resolveSnapshotFile(projectKey);
        long initialSize = Files.size(snapshotFile);
        FileTime initialMtime = Files.getLastModifiedTime(snapshotFile);

        ProjectMetadataSnapshotStoreTestHook.write(projectKey, second);
        assertEquals(initialSize, Files.size(snapshotFile));
        Files.setLastModifiedTime(snapshotFile, initialMtime);

        ProjectRuntimeSnapshot refreshed = store.read(projectKey);
        assertNotNull(refreshed);
        assertEquals(22L, refreshed.buildSeq());
    }

    @Test
    void shouldReadPersistedMetadataWhenProjectOverrideTargetsNonActiveProject() {
        String overrideProjectKey = projectKey + "-override";
        try {
            MethodReference activeMethod = new MethodReference(
                    new ClassReference.Handle("demo/ActiveController", 1),
                    "run",
                    "()V",
                    false,
                    Set.of(),
                    1,
                    10,
                    "active.jar",
                    1
            );
            ClassReference activeClass = new ClassReference(
                    61,
                    1,
                    "demo/ActiveController",
                    "java/lang/Object",
                    List.of(),
                    false,
                    List.of(),
                    Set.of(),
                    "active.jar",
                    1
            );
            SpringController activeController = controller(activeClass, activeMethod, "/active", "/active/run");
            DatabaseManager.restoreProjectRuntime(DatabaseManager.buildProjectRuntimeSnapshot(
                    7L,
                    ProjectModel.artifact(
                            Path.of("/tmp/jar-analyzer/active.jar"),
                            null,
                            List.of(Path.of("/tmp/jar-analyzer/active.jar")),
                            false
                    ),
                    List.of("/tmp/jar-analyzer/active.jar"),
                    new LinkedHashSet<>(List.of(classFile("demo/ActiveController", "/tmp/jar-analyzer/demo/ActiveController.class", "active.jar", 1))),
                    new LinkedHashSet<>(List.of(activeClass)),
                    new LinkedHashSet<>(List.of(activeMethod)),
                    java.util.Map.of(activeMethod.getHandle(), List.of("active-string")),
                    java.util.Map.of(activeMethod.getHandle(), List.of("active-anno")),
                    List.of(resource("active.properties", "/tmp/jar-analyzer/active.properties", "active.jar", 1)),
                    List.of(callSite("demo/ActiveController", "run", "demo/ActiveController#run#()V|0", 21, 1)),
                    List.of(localVar("demo/ActiveController", "run", "activeValue", "Ldemo/ActiveController;", 1)),
                    List.of(activeController),
                    List.of("demo/ActiveInterceptor"),
                    List.of("demo/ActiveServlet"),
                    List.of("demo/ActiveFilter"),
                    List.of("demo/ActiveListener")
            ));

            MethodReference overrideMethod = new MethodReference(
                    new ClassReference.Handle("demo/OverrideController", 2),
                    "run",
                    "()V",
                    false,
                    Set.of(),
                    1,
                    11,
                    "override.jar",
                    2
            );
            ClassReference overrideClass = new ClassReference(
                    61,
                    1,
                    "demo/OverrideController",
                    "java/lang/Object",
                    List.of(),
                    false,
                    List.of(),
                    Set.of(),
                    "override.jar",
                    2
            );
            SpringController overrideController = controller(overrideClass, overrideMethod, "/override", "/override/run");
            ProjectMetadataSnapshotStoreTestHook.write(overrideProjectKey, DatabaseManager.buildProjectRuntimeSnapshot(
                    9L,
                    ProjectModel.artifact(
                            Path.of("/tmp/jar-analyzer/override.jar"),
                            null,
                            List.of(Path.of("/tmp/jar-analyzer/override.jar")),
                            false
                    ),
                    List.of("/tmp/jar-analyzer/override.jar"),
                    new LinkedHashSet<>(List.of(classFile("demo/OverrideController", "/tmp/jar-analyzer/demo/OverrideController.class", "override.jar", 2))),
                    new LinkedHashSet<>(List.of(overrideClass)),
                    new LinkedHashSet<>(List.of(overrideMethod)),
                    java.util.Map.of(overrideMethod.getHandle(), List.of("override-string")),
                    java.util.Map.of(overrideMethod.getHandle(), List.of("override-anno")),
                    List.of(resource("override.properties", "/tmp/jar-analyzer/override.properties", "override.jar", 2)),
                    List.of(callSite("demo/OverrideController", "run", "demo/OverrideController#run#()V|0", 31, 2)),
                    List.of(localVar("demo/OverrideController", "run", "overrideValue", "Ldemo/OverrideController;", 2)),
                    List.of(overrideController),
                    List.of("demo/OverrideInterceptor"),
                    List.of("demo/OverrideServlet"),
                    List.of("demo/OverrideFilter"),
                    List.of("demo/OverrideListener")
            ));

            assertEquals(List.of("active-string"),
                    DatabaseManager.getMethodStringValues("demo/ActiveController", "run", "()V", 1));
            assertEquals(List.of("override-string"),
                    ActiveProjectContext.withProject(overrideProjectKey,
                            () -> DatabaseManager.getMethodStringValues("demo/OverrideController", "run", "()V", 2)));
            assertEquals(List.of("override-anno"),
                    ActiveProjectContext.withProject(overrideProjectKey,
                            () -> DatabaseManager.getMethodAnnoStringValues("demo/OverrideController", "run", "()V", 2)));
            assertEquals("override.properties",
                    ActiveProjectContext.withProject(overrideProjectKey,
                            () -> DatabaseManager.getResources().get(0).getResourcePath()));
            assertEquals("demo/OverrideController#run#()V|0",
                    ActiveProjectContext.withProject(overrideProjectKey,
                            () -> DatabaseManager.getCallSitesByCaller("demo/OverrideController", "run", "()V").get(0).getCallSiteKey()));
            assertEquals("overrideValue",
                    ActiveProjectContext.withProject(overrideProjectKey,
                            () -> DatabaseManager.getLocalVarsByMethod("demo/OverrideController", "run", "()V").get(0).getVarName()));
            assertEquals("/override/run",
                    ActiveProjectContext.withProject(overrideProjectKey,
                            () -> DatabaseManager.getSpringControllers().get(0).getMappings().get(0).getPathRestful()));
            assertTrue(ActiveProjectContext.withProject(overrideProjectKey,
                    () -> DatabaseManager.getSpringInterceptors().contains("demo/OverrideInterceptor")));
            assertTrue(ActiveProjectContext.withProject(overrideProjectKey,
                    () -> DatabaseManager.getServlets().contains("demo/OverrideServlet")));
            assertTrue(ActiveProjectContext.withProject(overrideProjectKey,
                    () -> DatabaseManager.getFilters().contains("demo/OverrideFilter")));
            assertTrue(ActiveProjectContext.withProject(overrideProjectKey,
                    () -> DatabaseManager.getListeners().contains("demo/OverrideListener")));
            assertEquals("override.jar",
                    ActiveProjectContext.withProject(overrideProjectKey,
                            () -> DatabaseManager.getJarsMeta().get(0).getJarName()));
        } finally {
            Neo4jProjectStore.getInstance().deleteProjectStore(overrideProjectKey);
        }
    }

    private static ClassFileEntity classFile() {
        return classFile("demo/Controller", "/tmp/jar-analyzer/demo/Controller.class", "app.jar", 1);
    }

    private static ClassFileEntity classFile(String className, String pathStr, String jarName, int jarId) {
        ClassFileEntity entity = new ClassFileEntity();
        entity.setCfId(1);
        entity.setClassName(className);
        entity.setPath(Path.of(pathStr));
        entity.setPathStr(pathStr);
        entity.setJarName(jarName);
        entity.setJarId(jarId);
        return entity;
    }

    private static ResourceEntity resource() {
        return resource("application.properties", "/tmp/jar-analyzer/application.properties", "app.jar", 1);
    }

    private static ResourceEntity resource(String resourcePath, String pathStr, String jarName, int jarId) {
        ResourceEntity entity = new ResourceEntity();
        entity.setRid(1);
        entity.setResourcePath(resourcePath);
        entity.setPathStr(pathStr);
        entity.setJarName(jarName);
        entity.setJarId(jarId);
        entity.setFileSize(12L);
        entity.setIsText(1);
        return entity;
    }

    private static CallSiteEntity callSite() {
        return callSite("demo/Controller", "index", "demo/Controller#index#()V|0", 12, 1);
    }

    private static CallSiteEntity callSite(String callerClassName,
                                           String callerMethodName,
                                           String callSiteKey,
                                           int lineNumber,
                                           int jarId) {
        CallSiteEntity entity = new CallSiteEntity();
        entity.setCallerClassName(callerClassName);
        entity.setCallerMethodName(callerMethodName);
        entity.setCallerMethodDesc("()V");
        entity.setCalleeOwner("java/io/PrintStream");
        entity.setCalleeMethodName("println");
        entity.setCalleeMethodDesc("(Ljava/lang/String;)V");
        entity.setOpCode(182);
        entity.setLineNumber(lineNumber);
        entity.setCallIndex(0);
        entity.setReceiverType("java/io/PrintStream");
        entity.setJarId(jarId);
        entity.setCallSiteKey(callSiteKey);
        return entity;
    }

    private static LocalVarEntity localVar() {
        return localVar("demo/Controller", "index", "this", "Ldemo/Controller;", 1);
    }

    private static LocalVarEntity localVar(String className,
                                           String methodName,
                                           String varName,
                                           String varDesc,
                                           int jarId) {
        LocalVarEntity entity = new LocalVarEntity();
        entity.setClassName(className);
        entity.setMethodName(methodName);
        entity.setMethodDesc("()V");
        entity.setVarIndex(0);
        entity.setVarName(varName);
        entity.setVarDesc(varDesc);
        entity.setVarSignature(varDesc);
        entity.setStartLine(10);
        entity.setEndLine(20);
        entity.setJarId(jarId);
        return entity;
    }

    private static SpringController controller(ClassReference classRef,
                                               MethodReference methodRef,
                                               String basePath,
                                               String pathRestful) {
        SpringController controller = new SpringController();
        controller.setRest(true);
        controller.setBasePath(basePath);
        controller.setClassName(methodRef.getClassReference());
        controller.setClassReference(classRef);
        SpringMapping mapping = new SpringMapping();
        mapping.setController(controller);
        mapping.setRest(true);
        mapping.setMethodReference(methodRef);
        mapping.setMethodName(methodRef.getHandle());
        mapping.setPath("/run");
        mapping.setPathRestful(pathRestful);
        controller.addMapping(mapping);
        return controller;
    }

    private static void deleteRecursively(Path root) throws Exception {
        if (root == null || !Files.exists(root)) {
            return;
        }
        try (var walk = Files.walk(root)) {
            walk.sorted((left, right) -> right.getNameCount() - left.getNameCount())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (Exception ignored) {
                            // best effort for test cleanup
                        }
                    });
        }
    }

    private static ProjectRuntimeSnapshot snapshotFor(String className, long buildSeq) {
        String jarPath = "/tmp/jar-analyzer/" + className.replace('/', '_') + ".jar";
        ProjectModel model = ProjectModel.artifact(
                Path.of(jarPath),
                null,
                List.of(Path.of(jarPath)),
                false
        );
        ClassReference classRef = new ClassReference(
                61,
                1,
                className,
                "java/lang/Object",
                List.of(),
                false,
                List.of(),
                Set.of(),
                "app.jar",
                1
        );
        MethodReference methodRef = new MethodReference(
                new ClassReference.Handle(className, 1),
                "run",
                "()V",
                false,
                Set.of(),
                1,
                12,
                "app.jar",
                1
        );
        ClassFileEntity classFile = classFile(className, "/tmp/jar-analyzer/" + className + ".class", "app.jar", 1);
        return DatabaseManager.buildProjectRuntimeSnapshot(
                buildSeq,
                model,
                List.of(jarPath),
                new LinkedHashSet<>(List.of(classFile)),
                new LinkedHashSet<>(List.of(classRef)),
                new LinkedHashSet<>(List.of(methodRef)),
                java.util.Map.of(),
                java.util.Map.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
    }

    private static void loadRuntimeSnapshot(String projectKey, ProjectRuntimeSnapshot snapshot) {
        ActiveProjectContext.setActiveProject(projectKey, projectKey);
        DatabaseManager.restoreProjectRuntime(projectKey, snapshot);
        ProjectModel model = DatabaseManager.getProjectModel();
        WorkspaceContext.setProjectModel(model == null ? ProjectModel.empty() : model);
    }
}
