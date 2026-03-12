package me.n1ar4.jar.analyzer.core;

import me.n1ar4.jar.analyzer.engine.ProjectRuntimeContext;
import me.n1ar4.jar.analyzer.storage.neo4j.ActiveProjectContext;
import me.n1ar4.jar.analyzer.storage.neo4j.Neo4jProjectStore;
import me.n1ar4.jar.analyzer.storage.neo4j.ProjectRegistryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.neo4j.graphdb.Label;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoreRunnerExplicitEntryMetricTest {
    @AfterEach
    void cleanup() {
        DatabaseManager.clearAllData();
        ProjectRuntimeContext.clear();
        ProjectRegistryService.getInstance().cleanupTemporaryProject();
        ProjectRegistryService.getInstance().activateTemporaryProject();
    }

    @Test
    void explicitFrameworkEntriesShouldBeRecordedInMetricsAndMeta() throws Exception {
        Path jar = Files.createTempDirectory("ja-explicit-entry").resolve("jsp-entry.jar");
        createJspEntryJar(jar, "demo/JspEntry");

        CoreRunner.BuildResult result = CoreRunner.run(jar, null, false, false, null);

        CoreRunner.BuildStageMetric callGraph = result.getStageMetric("callgraph");
        assertNotNull(callGraph);
        assertEquals(1, ((Number) callGraph.getDetails().get("explicit_entries")).intValue());

        var database = Neo4jProjectStore.getInstance().database(ActiveProjectContext.getActiveProjectKey());
        try (var tx = database.beginTx();
             var it = tx.findNodes(Label.label("JAMeta"), "key", "build_meta")) {
            assertTrue(it.hasNext());
            var meta = it.next();
            assertEquals(1, ((Number) meta.getProperty("explicit_entry_method_count")).intValue());
            tx.commit();
        }
    }

    private static void createJspEntryJar(Path jarPath, String internalClassName) throws Exception {
        byte[] classBytes = generateJspEntryClass(internalClassName);
        try (OutputStream out = Files.newOutputStream(jarPath);
             JarOutputStream jar = new JarOutputStream(out)) {
            JarEntry entry = new JarEntry(internalClassName + ".class");
            jar.putNextEntry(entry);
            jar.write(classBytes);
            jar.closeEntry();
        }
    }

    private static byte[] generateJspEntryClass(String internalClassName) {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, internalClassName, null, "java/lang/Object", null);
        MethodVisitor init = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        init.visitCode();
        init.visitVarInsn(Opcodes.ALOAD, 0);
        init.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        init.visitInsn(Opcodes.RETURN);
        init.visitMaxs(1, 1);
        init.visitEnd();

        MethodVisitor jsp = cw.visitMethod(
                Opcodes.ACC_PUBLIC,
                "_jspService",
                "(Ljavax/servlet/http/HttpServletRequest;Ljavax/servlet/http/HttpServletResponse;)V",
                null,
                null
        );
        jsp.visitCode();
        jsp.visitInsn(Opcodes.RETURN);
        jsp.visitMaxs(0, 3);
        jsp.visitEnd();
        cw.visitEnd();
        return cw.toByteArray();
    }
}
