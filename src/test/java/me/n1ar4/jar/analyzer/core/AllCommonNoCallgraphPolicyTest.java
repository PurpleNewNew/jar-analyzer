package me.n1ar4.jar.analyzer.core;

import me.n1ar4.jar.analyzer.core.scope.AnalysisScopeRules;
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

class AllCommonNoCallgraphPolicyTest {

    @Test
    void shouldContinueWithDisabledCallgraphWhenAllArchivesAreCommon() throws Exception {
        String backupPolicy = System.getProperty("jar.analyzer.all-common.policy");
        List<String> backupCommonJar = new ArrayList<>(AnalysisScopeRules.getCommonLibraryJarPrefixes());
        try {
            List<String> commonJar = new ArrayList<>(backupCommonJar);
            commonJar.add("spring-core-test");
            AnalysisScopeRules.saveCommonLibraryJarPrefixes(commonJar);
            System.setProperty("jar.analyzer.all-common.policy", "continue-no-callgraph");

            Path jar = Files.createTempDirectory("ja-common-policy").resolve("spring-core-test.jar");
            createSimpleJar(jar, "test/common/Demo");

            CoreRunner.BuildResult result = CoreRunner.run(jar, null, false, true, true, null, true);

            assertEquals("disabled-no-target", result.getCallGraphEngine());
            assertEquals(0L, result.getEdgeCount());
            assertEquals(0, result.getTargetJarCount());
            assertTrue(result.getLibraryJarCount() >= 1);
        } finally {
            AnalysisScopeRules.saveCommonLibraryJarPrefixes(backupCommonJar);
            restoreProp("jar.analyzer.all-common.policy", backupPolicy);
        }
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

    private static void restoreProp(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }
}
