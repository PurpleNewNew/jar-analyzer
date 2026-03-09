/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.utils;

import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.engine.WorkspaceContext;
import me.n1ar4.jar.analyzer.engine.project.ProjectModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeClassResolverTest {
    private static final String INCLUDE_NESTED_PROP = "jar.analyzer.classpath.includeNestedLib";

    @TempDir
    Path tempDir;

    private String includeNestedBackup;

    @BeforeEach
    void setUp() {
        includeNestedBackup = System.getProperty(INCLUDE_NESTED_PROP);
        WorkspaceContext.clear();
        DatabaseManager.clearAllData();
    }

    @AfterEach
    void cleanup() {
        if (includeNestedBackup == null) {
            System.clearProperty(INCLUDE_NESTED_PROP);
        } else {
            System.setProperty(INCLUDE_NESTED_PROP, includeNestedBackup);
        }
        WorkspaceContext.clear();
        DatabaseManager.clearAllData();
    }

    @Test
    void resolveShouldFollowWorkspaceNestedJarFlag() throws Exception {
        System.setProperty(INCLUDE_NESTED_PROP, "true");
        String className = "audit/runtime/OnlyInNested";
        Path outerJar = createOuterJarWithNestedClass(tempDir.resolve("outer.jar"), className);

        WorkspaceContext.setProjectModel(ProjectModel.artifact(outerJar, null, List.of(), false));
        long seqBefore = RuntimeClassResolver.getRootSeq();
        assertNull(RuntimeClassResolver.resolve(className));

        WorkspaceContext.updateResolveInnerJars(true);
        long seqAfter = RuntimeClassResolver.getRootSeq();
        assertTrue(seqAfter > seqBefore);

        RuntimeClassResolver.ResolvedClass resolved = RuntimeClassResolver.resolve(className);
        assertNotNull(resolved);
        assertTrue(Files.exists(resolved.getClassFile()));
        assertTrue(resolved.getJarName().endsWith("nested-only.jar"));
    }

    private static Path createOuterJarWithNestedClass(Path outerJar, String internalClassName) throws Exception {
        byte[] nestedJar = createNestedJar(internalClassName);
        try (OutputStream out = Files.newOutputStream(outerJar);
             JarOutputStream jar = new JarOutputStream(out)) {
            JarEntry nestedEntry = new JarEntry("BOOT-INF/lib/nested-only.jar");
            jar.putNextEntry(nestedEntry);
            jar.write(nestedJar);
            jar.closeEntry();
        }
        return outerJar;
    }

    private static byte[] createNestedJar(String internalClassName) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (JarOutputStream jar = new JarOutputStream(out)) {
            JarEntry classEntry = new JarEntry(internalClassName + ".class");
            jar.putNextEntry(classEntry);
            jar.write(generateSimpleClass(internalClassName));
            jar.closeEntry();
        }
        return out.toByteArray();
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
