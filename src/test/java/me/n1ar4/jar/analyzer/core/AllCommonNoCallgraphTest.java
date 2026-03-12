package me.n1ar4.jar.analyzer.core;

import me.n1ar4.jar.analyzer.core.scope.AnalysisScopeRules;
import me.n1ar4.jar.analyzer.graph.store.GraphEdge;
import me.n1ar4.jar.analyzer.graph.store.GraphNode;
import me.n1ar4.jar.analyzer.graph.store.GraphSnapshot;
import me.n1ar4.jar.analyzer.graph.store.GraphStore;
import me.n1ar4.support.FixtureJars;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AllCommonNoCallgraphTest {

    @Test
    void shouldContinueWithDisabledCallgraphWhenAllArchivesAreCommon() throws Exception {
        List<String> backupCommonJar = new ArrayList<>(AnalysisScopeRules.getCommonLibraryJarPrefixes());
        try {
            List<String> commonJar = new ArrayList<>(backupCommonJar);
            commonJar.add("spring-core-test");
            AnalysisScopeRules.saveCommonLibraryJarPrefixes(commonJar);

            Path jar = Files.createTempDirectory("ja-all-common").resolve("spring-core-test.jar");
            createSimpleJar(jar, "test/common/Demo");

            CoreRunner.BuildResult result = CoreRunner.run(jar, null, false, null);

            assertEquals("disabled-no-target", result.getCallGraphEngine());
            assertEquals(0L, result.getEdgeCount());
            assertEquals(0, result.getTargetJarCount());
            assertTrue(result.getLibraryJarCount() >= 1);
        } finally {
            AnalysisScopeRules.saveCommonLibraryJarPrefixes(backupCommonJar);
        }
    }

    @Test
    void allCommonBuildShouldReplacePreviousRuntimeStateWithoutKeepingOldCallgraph() throws Exception {
        List<String> backupCommonJar = new ArrayList<>(AnalysisScopeRules.getCommonLibraryJarPrefixes());
        try {
            CoreRunner.BuildResult previous = CoreRunner.run(FixtureJars.springbootTestJar(), null, false, null);
            assertTrue(previous.getEdgeCount() > 0L);
            long previousBuildSeq = DatabaseManager.getProjectBuildSeq();
            assertTrue(previousBuildSeq > 0L);
            assertTrue(new GraphStore().loadSnapshot().getEdgeCount() > 0L);

            List<String> commonJar = new ArrayList<>(backupCommonJar);
            commonJar.add("spring-core-test");
            AnalysisScopeRules.saveCommonLibraryJarPrefixes(commonJar);

            Path jar = Files.createTempDirectory("ja-all-common-rebuild").resolve("spring-core-test.jar");
            createSimpleJar(jar, "test/common/FailDemo");

            CoreRunner.BuildResult result = CoreRunner.run(jar, null, false, null);

            assertEquals("disabled-no-target", result.getCallGraphEngine());
            assertEquals(0L, result.getEdgeCount());
            assertTrue(DatabaseManager.getProjectBuildSeq() > previousBuildSeq);
            assertEquals(0L, countCallEdges(new GraphStore().loadSnapshot()));
        } finally {
            AnalysisScopeRules.saveCommonLibraryJarPrefixes(backupCommonJar);
        }
    }

    private static long countCallEdges(GraphSnapshot snapshot) {
        if (snapshot == null) {
            return 0L;
        }
        long count = 0L;
        for (GraphNode node : snapshot.getNodesView()) {
            if (node == null) {
                continue;
            }
            for (GraphEdge edge : snapshot.getOutgoingView(node.getNodeId())) {
                if (edge != null && edge.getRelType() != null && edge.getRelType().startsWith("CALL")) {
                    count++;
                }
            }
        }
        return count;
    }

    private static void createSimpleJar(Path jarPath, String internalClassName) throws Exception {
        byte[] classBytes = generateSimpleClass(internalClassName);
        try (OutputStream out = Files.newOutputStream(jarPath);
             JarOutputStream jar = new JarOutputStream(out)) {
            JarEntry entry = new JarEntry(internalClassName + ".class");
            jar.putNextEntry(entry);
            jar.write(classBytes);
            jar.closeEntry();
        }
    }

    private static byte[] generateSimpleClass(String internalClassName) {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, internalClassName, null, "java/lang/Object", null);
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
        cw.visitEnd();
        return cw.toByteArray();
    }
}
