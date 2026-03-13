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
import me.n1ar4.jar.analyzer.engine.ProjectRuntimeContext;
import me.n1ar4.jar.analyzer.engine.project.ProjectBuildMode;
import me.n1ar4.jar.analyzer.engine.project.ProjectModel;
import me.n1ar4.jar.analyzer.engine.project.ProjectOrigin;
import me.n1ar4.jar.analyzer.engine.project.ProjectRoot;
import me.n1ar4.jar.analyzer.engine.project.ProjectRootKind;
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
    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        ProjectRuntimeContext.clear();
        DatabaseManager.clearAllData();
    }

    @AfterEach
    void cleanup() {
        ProjectRuntimeContext.clear();
        DatabaseManager.clearAllData();
    }

    @Test
    void resolveShouldFollowWorkspaceNestedJarFlag() throws Exception {
        String className = "audit/runtime/OnlyInNested";
        Path outerJar = createOuterJarWithNestedClass(tempDir.resolve("outer.jar"), className);

        ProjectRuntimeContext.replaceProjectModel(ProjectModel.artifact(outerJar, null, List.of(), false));
        long seqBefore = RuntimeClassResolver.getRootSeq();
        assertNull(RuntimeClassResolver.resolve(className));

        ProjectRuntimeContext.updateResolveInnerJars(true);
        long seqAfter = RuntimeClassResolver.getRootSeq();
        assertTrue(seqAfter > seqBefore);

        RuntimeClassResolver.ResolvedClass resolved = RuntimeClassResolver.resolve(className);
        assertNotNull(resolved);
        assertTrue(Files.exists(resolved.getClassFile()));
        assertTrue(resolved.getJarName().endsWith("nested-only.jar"));
    }

    @Test
    void stateVersionShouldAdvanceWhenWorkspaceModelChanges() {
        long version0 = ProjectRuntimeContext.stateVersion();

        ProjectRuntimeContext.replaceProjectModel(ProjectModel.artifact(tempDir.resolve("a.jar"), null, List.of(), false));
        long version1 = ProjectRuntimeContext.stateVersion();

        ProjectRuntimeContext.prepareArtifactBuild(
                tempDir.resolve("a.jar"),
                null,
                List.of(tempDir.resolve("a.jar")),
                false,
                "core"
        );
        long version2 = ProjectRuntimeContext.stateVersion();

        ProjectRuntimeContext.updateResolveInnerJars(true);
        long version3 = ProjectRuntimeContext.stateVersion();

        assertTrue(version1 > version0);
        assertTrue(version2 > version1);
        assertTrue(version3 > version2);
    }

    @Test
    void resolveShouldSeeProjectLibraryRootsWithoutGlobalClasspathProperty() throws Exception {
        String className = "audit/runtime/ExternalOnly";
        Path inputDir = Files.createDirectories(tempDir.resolve("input"));
        Path libraryJar = createJarWithClass(tempDir.resolve("external-lib.jar"), className);
        ProjectRuntimeContext.replaceProjectModel(ProjectModel.builder()
                .buildMode(ProjectBuildMode.PROJECT)
                .primaryInputPath(inputDir)
                .addRoot(new ProjectRoot(
                        ProjectRootKind.CONTENT_ROOT,
                        ProjectOrigin.APP,
                        inputDir,
                        "",
                        false,
                        false,
                        10
                ))
                .addRoot(new ProjectRoot(
                        ProjectRootKind.LIBRARY,
                        ProjectOrigin.LIBRARY,
                        libraryJar,
                        "",
                        true,
                        false,
                        20
                ))
                .build());

        RuntimeClassResolver.ResolvedClass resolved = RuntimeClassResolver.resolve(className);

        assertNotNull(resolved);
        assertTrue(Files.exists(resolved.getClassFile()));
        assertTrue(resolved.getJarName().endsWith("external-lib.jar"));
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

    private static Path createJarWithClass(Path jarPath, String internalClassName) throws Exception {
        try (OutputStream out = Files.newOutputStream(jarPath);
             JarOutputStream jar = new JarOutputStream(out)) {
            JarEntry classEntry = new JarEntry(internalClassName + ".class");
            jar.putNextEntry(classEntry);
            jar.write(generateSimpleClass(internalClassName));
            jar.closeEntry();
        }
        return jarPath;
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
