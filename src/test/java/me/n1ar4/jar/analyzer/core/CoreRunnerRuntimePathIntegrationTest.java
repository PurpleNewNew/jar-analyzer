package me.n1ar4.jar.analyzer.core;

import me.n1ar4.jar.analyzer.engine.ProjectRuntimeContext;
import me.n1ar4.jar.analyzer.storage.neo4j.ProjectRegistryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoreRunnerRuntimePathIntegrationTest {
    @AfterEach
    void cleanup() {
        DatabaseManager.clearAllData();
        ProjectRuntimeContext.clear();
        ProjectRegistryService.getInstance().cleanupTemporaryProject();
        ProjectRegistryService.getInstance().activateTemporaryProject();
    }

    @Test
    void runtimeRtJarShouldFeedWorkspaceAndCallGraph() throws Exception {
        Path tempDir = Files.createTempDirectory("ja-runtime-rt");
        Path appJar = tempDir.resolve("app.jar");
        Path rtJar = tempDir.resolve("rt.jar");
        createCallerJar(appJar, "demo/AppCaller");
        createRuntimeJar(rtJar, "sdk/RuntimeApi");

        CoreRunner.BuildResult withoutRuntime = CoreRunner.run(appJar, null, false, null);
        assertTrue(DatabaseManager.getMethodReferencesByClass("sdk/RuntimeApi").isEmpty());

        CoreRunner.BuildResult withRuntime = CoreRunner.run(appJar, rtJar, false, null);

        assertTrue(withRuntime.getEdgeCount() > withoutRuntime.getEdgeCount());
        assertFalse(DatabaseManager.getMethodReferencesByClass("sdk/RuntimeApi").isEmpty());
    }

    @Test
    void runtimeJmodsShouldFeedWorkspaceAndCallGraph() throws Exception {
        Path tempDir = Files.createTempDirectory("ja-runtime-jmods");
        Path appJar = tempDir.resolve("app.jar");
        Path javaHome = tempDir.resolve("fake-jdk");
        Path jmods = javaHome.resolve("jmods");
        Files.createDirectories(jmods);
        createCallerJar(appJar, "demo/AppCaller");
        createRuntimeJmod(jmods.resolve("java.base.jmod"), "sdk/RuntimeApi");

        CoreRunner.BuildResult result = CoreRunner.run(appJar, javaHome, false, null);

        assertTrue(result.getEdgeCount() > 0L);
        assertFalse(DatabaseManager.getMethodReferencesByClass("sdk/RuntimeApi").isEmpty());
    }

    private static void createCallerJar(Path jarPath, String internalClassName) throws Exception {
        byte[] classBytes = generateCallerClass(internalClassName);
        try (OutputStream out = Files.newOutputStream(jarPath);
             JarOutputStream jar = new JarOutputStream(out)) {
            JarEntry entry = new JarEntry(internalClassName + ".class");
            jar.putNextEntry(entry);
            jar.write(classBytes);
            jar.closeEntry();
        }
    }

    private static void createRuntimeJar(Path jarPath, String internalClassName) throws Exception {
        byte[] classBytes = generateRuntimeClass(internalClassName);
        try (OutputStream out = Files.newOutputStream(jarPath);
             JarOutputStream jar = new JarOutputStream(out)) {
            JarEntry entry = new JarEntry(internalClassName + ".class");
            jar.putNextEntry(entry);
            jar.write(classBytes);
            jar.closeEntry();
        }
    }

    private static void createRuntimeJmod(Path target, String internalClassName) throws Exception {
        byte[] classBytes = generateRuntimeClass(internalClassName);
        try (OutputStream out = Files.newOutputStream(target);
             ZipOutputStream zip = new ZipOutputStream(out)) {
            zip.putNextEntry(new ZipEntry("classes/" + internalClassName + ".class"));
            zip.write(classBytes);
            zip.closeEntry();
        }
    }

    private static byte[] generateCallerClass(String internalClassName) {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, internalClassName, null, "java/lang/Object", null);
        MethodVisitor init = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        init.visitCode();
        init.visitVarInsn(Opcodes.ALOAD, 0);
        init.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        init.visitInsn(Opcodes.RETURN);
        init.visitMaxs(1, 1);
        init.visitEnd();

        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "run", "()V", null, null);
        mv.visitCode();
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "sdk/RuntimeApi", "work", "()V", false);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        cw.visitEnd();
        return cw.toByteArray();
    }

    private static byte[] generateRuntimeClass(String internalClassName) {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, internalClassName, null, "java/lang/Object", null);
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "work", "()V", null, null);
        mv.visitCode();
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        cw.visitEnd();
        return cw.toByteArray();
    }
}
