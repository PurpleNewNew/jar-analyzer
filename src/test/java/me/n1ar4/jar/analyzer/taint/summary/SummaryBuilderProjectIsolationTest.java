/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.taint.summary;

import me.n1ar4.jar.analyzer.config.ConfigFile;
import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.core.ProjectRuntimeSnapshot;
import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.engine.CoreEngine;
import me.n1ar4.jar.analyzer.engine.EngineContext;
import me.n1ar4.jar.analyzer.engine.WorkspaceContext;
import me.n1ar4.jar.analyzer.engine.project.ProjectModel;
import me.n1ar4.jar.analyzer.storage.neo4j.ActiveProjectContext;
import me.n1ar4.jar.analyzer.storage.neo4j.Neo4jProjectStore;
import me.n1ar4.jar.analyzer.storage.neo4j.ProjectMetadataSnapshotStoreTestHook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SummaryBuilderProjectIsolationTest {
    @TempDir
    Path tempDir;

    @AfterEach
    void cleanup() {
        DatabaseManager.clearAllData();
        WorkspaceContext.clear();
        EngineContext.setEngine(null);
    }

    @Test
    void shouldUseRequestedProjectBytecodeInsteadOfActiveProjectRuntime() throws Exception {
        String originalProjectKey = ActiveProjectContext.getPublishedActiveProjectKey();
        String originalProjectAlias = ActiveProjectContext.getPublishedActiveProjectAlias();
        String projectA = "summary-bytecode-a";
        String projectB = "summary-bytecode-b";
        try {
            Path classA = writeClassFile(tempDir.resolve("project-a"), "demo/Same", "run");
            Path classB = writeClassFile(tempDir.resolve("project-b"), "demo/Same", "other");

            ActiveProjectContext.setActiveProject(projectA, projectA);
            DatabaseManager.restoreProjectRuntime(projectA, snapshotFor(projectA, classA, "demo/Same", "run"));

            ConfigFile config = new ConfigFile();
            config.setDbPath("test-db");
            EngineContext.setEngine(new CoreEngine(config));

            ProjectMetadataSnapshotStoreTestHook
                    .write(projectB, snapshotFor(projectB, classB, "demo/Same", "other"));

            MethodReference.Handle handle = new MethodReference.Handle(
                    new ClassReference.Handle("demo/Same", 1),
                    "run",
                    "()V"
            );
            MethodSummary activeSummary = new SummaryBuilder().build(handle);
            assertFalse(activeSummary.isUnknown());

            MethodSummary otherSummary = ActiveProjectContext.withProject(
                    projectB,
                    () -> new SummaryBuilder().build(handle)
            );
            assertTrue(otherSummary.isUnknown());
        } finally {
            ActiveProjectContext.setActiveProject(originalProjectKey, originalProjectAlias);
            Neo4jProjectStore.getInstance().deleteProjectStore(projectB);
        }
    }

    private ProjectRuntimeSnapshot snapshotFor(String projectName,
                                               Path classFile,
                                               String className,
                                               String methodName) {
        ProjectModel model = ProjectModel.artifact(
                classFile,
                null,
                List.of(classFile),
                false
        );
        ProjectRuntimeSnapshot.ProjectModelData modelData = new ProjectRuntimeSnapshot.ProjectModelData(
                model.buildMode().name(),
                classFile.toString(),
                "",
                List.of(),
                List.of(classFile.toString()),
                false
        );
        ProjectRuntimeSnapshot.ClassHandleData handle = new ProjectRuntimeSnapshot.ClassHandleData(className, 1);
        ProjectRuntimeSnapshot.ClassReferenceData classRef = new ProjectRuntimeSnapshot.ClassReferenceData(
                61,
                Opcodes.ACC_PUBLIC,
                className,
                "java/lang/Object",
                List.of(),
                false,
                List.of(),
                List.of(),
                projectName + ".jar",
                1
        );
        List<ProjectRuntimeSnapshot.MethodReferenceData> methods = List.of(new ProjectRuntimeSnapshot.MethodReferenceData(
                handle,
                List.of(),
                methodName,
                "()V",
                Opcodes.ACC_PUBLIC,
                false,
                1,
                projectName + ".jar",
                1
        ));
        return new ProjectRuntimeSnapshot(
                ProjectRuntimeSnapshot.CURRENT_SCHEMA_VERSION,
                17L,
                modelData,
                List.of(),
                List.of(new ProjectRuntimeSnapshot.ClassFileData(
                        1,
                        className,
                        classFile.toAbsolutePath().normalize().toString(),
                        projectName + ".jar",
                        1
                )),
                List.of(classRef),
                methods,
                Map.of(),
                Map.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of()
        );
    }

    private static Path writeClassFile(Path root, String className, String methodName) throws Exception {
        Path path = root.resolve(className + ".class");
        Files.createDirectories(path.getParent());
        Files.write(path, createClassBytes(className, methodName));
        return path;
    }

    private static byte[] createClassBytes(String className, String methodName) {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, className, null, "java/lang/Object", null);

        MethodVisitor init = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        init.visitCode();
        init.visitVarInsn(Opcodes.ALOAD, 0);
        init.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        init.visitInsn(Opcodes.RETURN);
        init.visitMaxs(1, 1);
        init.visitEnd();

        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, methodName, "()V", null, null);
        mv.visitCode();
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 1);
        mv.visitEnd();

        cw.visitEnd();
        return cw.toByteArray();
    }
}
