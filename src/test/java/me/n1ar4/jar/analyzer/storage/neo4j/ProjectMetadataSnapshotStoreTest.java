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
import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.jar.analyzer.entity.CallSiteEntity;
import me.n1ar4.jar.analyzer.entity.ClassFileEntity;
import me.n1ar4.jar.analyzer.entity.LocalVarEntity;
import me.n1ar4.jar.analyzer.entity.ResourceEntity;
import me.n1ar4.support.DatabaseManagerTestHook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectMetadataSnapshotStoreTest {
    private final ProjectMetadataSnapshotStore store = ProjectMetadataSnapshotStore.getInstance();
    private final String projectKey = "snapshot-" + Long.toHexString(System.nanoTime());

    @AfterEach
    void cleanup() {
        DatabaseManager.clearAllData();
        WorkspaceContext.clear();
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

        DatabaseManager.runAtomicUpdate(() -> {
            DatabaseManager.saveProjectModel(model);
            DatabaseManager.replaceJars(List.of("/tmp/jar-analyzer/app.jar"));
            DatabaseManager.saveClassFiles(new LinkedHashSet<>(List.of(classFile())));
            DatabaseManager.saveClassInfo(new LinkedHashSet<>(List.of(classRef)));
            DatabaseManager.saveMethods(new LinkedHashSet<>(List.of(methodRef)));
            DatabaseManager.saveStrMap(
                    java.util.Map.of(methodRef.getHandle(), List.of("jdbc:demo")),
                    java.util.Map.of()
            );
            DatabaseManager.saveResources(List.of(resource()));
            DatabaseManager.saveCallSites(List.of(callSite()));
            DatabaseManager.saveLocalVars(List.of(localVar()));
            DatabaseManager.saveSpringController(new ArrayList<>(List.of(controller)));
            DatabaseManager.saveSpringInterceptor(new ArrayList<>(List.of("demo/Interceptor")));
            DatabaseManager.saveServlets(new ArrayList<>(List.of("demo/Servlet")));
            DatabaseManager.saveFilters(new ArrayList<>(List.of("demo/Filter")));
            DatabaseManager.saveListeners(new ArrayList<>(List.of("demo/Listener")));
            DatabaseManagerTestHook.markProjectBuildReady(42L);
        });

        store.write(projectKey, DatabaseManager.snapshotProjectRuntime());
        DatabaseManager.clearAllData();
        WorkspaceContext.clear();

        assertTrue(store.restoreIntoRuntime(projectKey));
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

        DatabaseManager.runAtomicUpdate(() -> {
            DatabaseManager.saveProjectModel(model);
            DatabaseManager.replaceJars(List.of("/tmp/jar-analyzer/temp-app.jar"));
            DatabaseManager.saveClassFiles(new LinkedHashSet<>(List.of(classFile)));
            DatabaseManager.saveClassInfo(new LinkedHashSet<>(List.of(classRef)));
            DatabaseManager.saveMethods(new LinkedHashSet<>(List.of(methodRef)));
            DatabaseManager.saveResources(List.of(resource));
            DatabaseManagerTestHook.markProjectBuildReady(55L);
        });

        store.write(projectKey, DatabaseManager.snapshotProjectRuntime());
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

        assertTrue(store.restoreIntoRuntime(projectKey));
        assertEquals(persistedClassPath, DatabaseManager.getClassFiles().get(0).getPathStr());
        assertEquals(persistedResourcePath, DatabaseManager.getResources().get(0).getPathStr());
        assertTrue(Files.exists(Path.of(DatabaseManager.getClassFiles().get(0).getPathStr())));
        assertTrue(Files.exists(Path.of(DatabaseManager.getResources().get(0).getPathStr())));
    }

    private static ClassFileEntity classFile() {
        ClassFileEntity entity = new ClassFileEntity();
        entity.setCfId(1);
        entity.setClassName("demo/Controller");
        entity.setPath(Path.of("/tmp/jar-analyzer/demo/Controller.class"));
        entity.setPathStr("/tmp/jar-analyzer/demo/Controller.class");
        entity.setJarName("app.jar");
        entity.setJarId(1);
        return entity;
    }

    private static ResourceEntity resource() {
        ResourceEntity entity = new ResourceEntity();
        entity.setRid(1);
        entity.setResourcePath("application.properties");
        entity.setPathStr("/tmp/jar-analyzer/application.properties");
        entity.setJarName("app.jar");
        entity.setJarId(1);
        entity.setFileSize(12L);
        entity.setIsText(1);
        return entity;
    }

    private static CallSiteEntity callSite() {
        CallSiteEntity entity = new CallSiteEntity();
        entity.setCallerClassName("demo/Controller");
        entity.setCallerMethodName("index");
        entity.setCallerMethodDesc("()V");
        entity.setCalleeOwner("java/io/PrintStream");
        entity.setCalleeMethodName("println");
        entity.setCalleeMethodDesc("(Ljava/lang/String;)V");
        entity.setOpCode(182);
        entity.setLineNumber(12);
        entity.setCallIndex(0);
        entity.setReceiverType("java/io/PrintStream");
        entity.setJarId(1);
        entity.setCallSiteKey("demo/Controller#index#()V|0");
        return entity;
    }

    private static LocalVarEntity localVar() {
        LocalVarEntity entity = new LocalVarEntity();
        entity.setClassName("demo/Controller");
        entity.setMethodName("index");
        entity.setMethodDesc("()V");
        entity.setVarIndex(0);
        entity.setVarName("this");
        entity.setVarDesc("Ldemo/Controller;");
        entity.setVarSignature("Ldemo/Controller;");
        entity.setStartLine(10);
        entity.setEndLine(20);
        entity.setJarId(1);
        return entity;
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
}
