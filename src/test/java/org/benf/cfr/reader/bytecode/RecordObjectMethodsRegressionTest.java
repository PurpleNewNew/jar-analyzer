package org.benf.cfr.reader.bytecode;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecordObjectMethodsRegressionTest {
    @Test
    void shouldResugarSimpleRecordsWithoutObjectMethodsBootstrap(@TempDir Path tempDir) throws IOException {
        Path classFile = CfrDecompilerRegressionSupport.compileFixture(
                tempDir,
                "record-object-methods",
                "SimpleRecordSample",
                "--release", "21");

        String decompiled = CfrDecompilerRegressionSupport.decompileJava(classFile);

        assertTrue(decompiled.contains("public record SimpleRecordSample(String name, int age)"), decompiled);
        assertFalse(decompiled.contains("ObjectMethods.bootstrap"), decompiled);
        assertFalse(decompiled.contains("public final String toString()"), decompiled);
        assertFalse(decompiled.contains("public final int hashCode()"), decompiled);
        assertFalse(decompiled.contains("public final boolean equals(Object object)"), decompiled);

        CfrDecompilerRegressionSupport.compileJava(tempDir, "SimpleRecordSample", decompiled, "--release", "21");
    }

    @Test
    void shouldKeepCanonicalValidationWithoutSyntheticRecordAssignments(@TempDir Path tempDir) throws IOException {
        Path classFile = CfrDecompilerRegressionSupport.compileFixture(
                tempDir,
                "record-object-methods",
                "ValidatedRecordSample",
                "--release", "21");

        String decompiled = CfrDecompilerRegressionSupport.decompileJava(classFile);

        assertTrue(decompiled.contains("public record ValidatedRecordSample(String name, int age)"), decompiled);
        assertTrue(decompiled.contains("throw new IllegalArgumentException(\"name\");"), decompiled);
        assertFalse(decompiled.contains("ObjectMethods.bootstrap"), decompiled);
        assertFalse(decompiled.contains("this.name = name;"), decompiled);
        assertFalse(decompiled.contains("this.age = age;"), decompiled);

        CfrDecompilerRegressionSupport.compileJava(tempDir, "ValidatedRecordSample", decompiled, "--release", "21");
    }

    @Test
    void shouldNotResugarRecordsWhenBackingFieldsAreNotPrivateFinal(@TempDir Path tempDir) throws IOException {
        Path classFile = CfrDecompilerRegressionSupport.compileFixture(
                tempDir,
                "record-object-methods",
                "SimpleRecordSample",
                "--release", "21");
        rewriteClass(classFile, classNode -> {
            boolean changed = false;
            for (FieldNode field : classNode.fields) {
                if ((field.access & Opcodes.ACC_STATIC) != 0) {
                    continue;
                }
                field.access &= ~Opcodes.ACC_PRIVATE;
                field.access |= Opcodes.ACC_PUBLIC;
                changed = true;
            }
            assertTrue(changed, "expected record fields to be rewritten");
        });

        String decompiled = CfrDecompilerRegressionSupport.decompileJava(classFile);

        assertFalse(decompiled.contains("public record SimpleRecordSample("), decompiled);
        assertTrue(Pattern.compile("public\\s+final\\s+String\\s+name;").matcher(decompiled).find(), decompiled);
        assertTrue(Pattern.compile("public\\s+final\\s+int\\s+age;").matcher(decompiled).find(), decompiled);
    }

    @Test
    void shouldKeepObjectMethodsBootstrapVisibleWhenBootstrapArgsDoNotMatchFields(@TempDir Path tempDir) throws IOException {
        Path classFile = CfrDecompilerRegressionSupport.compileFixture(
                tempDir,
                "record-object-methods",
                "SimpleRecordSample",
                "--release", "21");
        rewriteClass(classFile, classNode -> {
            MethodNode toStringMethod = null;
            for (MethodNode method : classNode.methods) {
                if ("toString".equals(method.name) && "()Ljava/lang/String;".equals(method.desc)) {
                    toStringMethod = method;
                    break;
                }
            }
            assertNotNull(toStringMethod, "expected record toString method");
            boolean changed = false;
            for (var insn = toStringMethod.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (!(insn instanceof InvokeDynamicInsnNode)) {
                    continue;
                }
                InvokeDynamicInsnNode indy = (InvokeDynamicInsnNode) insn;
                if (!isObjectMethodsBootstrap(indy.bsm)) {
                    continue;
                }
                Object[] args = indy.bsmArgs.clone();
                Assertions.assertTrue(args.length >= 2 && args[1] instanceof String,
                        "expected record bootstrap field-name payload");
                args[1] = "age;name";
                indy.bsmArgs = args;
                changed = true;
                break;
            }
            assertTrue(changed, "expected ObjectMethods bootstrap to be rewritten");
        });

        String decompiled = CfrDecompilerRegressionSupport.decompileJava(classFile);

        assertTrue(decompiled.contains("public final String toString()"), decompiled);
        assertTrue(decompiled.contains("ObjectMethods.bootstrap"), decompiled);
    }

    private static void rewriteClass(Path classFile, java.util.function.Consumer<ClassNode> rewriter) throws IOException {
        byte[] bytes = Files.readAllBytes(classFile);
        ClassReader reader = new ClassReader(bytes);
        ClassNode classNode = new ClassNode();
        reader.accept(classNode, 0);
        rewriter.accept(classNode);
        ClassWriter writer = new ClassWriter(0);
        classNode.accept(writer);
        Files.write(classFile, writer.toByteArray());
    }

    private static boolean isObjectMethodsBootstrap(Handle handle) {
        return handle != null
                && Objects.equals(handle.getOwner(), "java/lang/runtime/ObjectMethods")
                && Objects.equals(handle.getName(), "bootstrap");
    }
}
