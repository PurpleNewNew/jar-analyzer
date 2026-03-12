package me.n1ar4.jar.analyzer.storage.neo4j;

import me.n1ar4.jar.analyzer.core.ProjectRuntimeSnapshot;
import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.engine.project.ProjectModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class Neo4jBulkImportServiceCleanupTest {
    private final String projectKey = "cleanup-" + Long.toHexString(System.nanoTime());

    @AfterEach
    void cleanup() {
        Neo4jProjectStore.getInstance().deleteProjectStore(projectKey);
        Neo4jGraphSnapshotLoader.invalidate(projectKey);
    }

    @Test
    void shouldCleanupBackupAndImportArtifactsAfterSuccessfulImport() throws Exception {
        Neo4jProjectStore.getInstance().database(projectKey);
        Path projectHome = Neo4jProjectStore.getInstance().resolveProjectHome(projectKey);
        Path parent = projectHome.getParent();
        String baseName = projectHome.getFileName() == null ? projectKey : projectHome.getFileName().toString();

        ClassReference classRef = new ClassReference(
                61,
                0x0021,
                "demo/CleanupController",
                "java/lang/Object",
                List.of(),
                false,
                List.of(),
                Set.of(),
                "cleanup.jar",
                1
        );
        MethodReference methodRef = new MethodReference(
                new ClassReference.Handle("demo/CleanupController", 1),
                "run",
                "()V",
                false,
                Set.of(),
                1,
                10,
                "cleanup.jar",
                1
        );
        Set<MethodReference> methods = new LinkedHashSet<>(List.of(methodRef));
        List<ClassReference> classes = List.of(classRef);
        ProjectRuntimeSnapshot snapshot = new ProjectRuntimeSnapshot(
                ProjectRuntimeSnapshot.CURRENT_SCHEMA_VERSION,
                3L,
                new ProjectRuntimeSnapshot.ProjectModelData(
                        ProjectModel.artifact(
                                Path.of("/tmp/jar-analyzer/cleanup.jar"),
                                null,
                                List.of(Path.of("/tmp/jar-analyzer/cleanup.jar")),
                                false
                        ).buildMode().name(),
                        "/tmp/jar-analyzer/cleanup.jar",
                        "",
                        List.of(),
                        List.of("/tmp/jar-analyzer/cleanup.jar"),
                        false
                ),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
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

        new Neo4jBulkImportService().replaceFromAnalysis(
                projectKey,
                3L,
                "bytecode:balanced-v1",
                methods,
                Map.of(),
                Map.of(),
                new Neo4jBulkImportService.GraphPayloadData(
                        List.of(),
                        classes,
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        Map.of()
                ),
                () -> snapshot,
                Map.of(),
                null
        );

        assertTrue(waitUntil(() -> {
            Path importRoot = projectHome.resolve("import-staging");
            boolean importClean = !Files.exists(importRoot) || isEmptyDirectory(importRoot);
            boolean backupClean = !hasBackupSibling(parent, baseName);
            return importClean && backupClean;
        }, 5000L), "neo4j bulk cleanup should complete asynchronously");
    }

    private static boolean hasBackupSibling(Path parent, String baseName) {
        if (parent == null || baseName == null || !Files.isDirectory(parent)) {
            return false;
        }
        try (var children = Files.list(parent)) {
            return children.anyMatch(path -> {
                String name = path.getFileName() == null ? "" : path.getFileName().toString();
                return name.startsWith(baseName + ".backup-");
            });
        } catch (Exception ex) {
            return false;
        }
    }

    private static boolean isEmptyDirectory(Path path) {
        if (path == null || !Files.isDirectory(path)) {
            return true;
        }
        try (var children = Files.list(path)) {
            return children.findAny().isEmpty();
        } catch (Exception ex) {
            return false;
        }
    }

    private static boolean waitUntil(Check check, long timeoutMs) throws Exception {
        long deadline = System.currentTimeMillis() + Math.max(100L, timeoutMs);
        while (System.currentTimeMillis() < deadline) {
            if (check.done()) {
                return true;
            }
            Thread.sleep(50L);
        }
        return check.done();
    }

    @FunctionalInterface
    private interface Check {
        boolean done() throws Exception;
    }
}
