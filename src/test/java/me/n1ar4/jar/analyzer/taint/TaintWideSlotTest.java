/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.taint;

import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.starter.Const;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.JSRInlinerAdapter;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TaintWideSlotTest {
    private static TaintPass runTaintPass(byte[] classBytes,
                                          int seedParamIndex,
                                          MethodReference.Handle cur,
                                          MethodReference.Handle next) {
        AtomicReference<TaintPass> pass = new AtomicReference<>(TaintPass.fail());
        AtomicBoolean lowConfidence = new AtomicBoolean(false);
        StringBuilder text = new StringBuilder();
        ClassVisitor cv = new ClassVisitor(Const.ASMVersion) {
            private String ownerName;

            @Override
            public void visit(int version, int access, String name, String signature,
                              String superName, String[] interfaces) {
                ownerName = name;
                super.visit(version, access, name, signature, superName, interfaces);
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String desc,
                                             String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                if (!name.equals(cur.getName()) || !desc.equals(cur.getDesc())) {
                    return mv;
                }
                TaintMethodAdapter adapter = new TaintMethodAdapter(
                        Const.ASMVersion,
                        mv,
                        ownerName,
                        access,
                        name,
                        desc,
                        seedParamIndex,
                        next,
                        pass,
                        new SanitizerRule(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        text,
                        false,
                        false,
                        false,
                        lowConfidence,
                        null,
                        null,
                        false
                );
                return new JSRInlinerAdapter(adapter, access, name, desc, signature, exceptions);
            }
        };
        new ClassReader(classBytes).accept(cv, 0);
        return pass.get();
    }

    private static byte[] genClass_CallNext_StringParam(String internalName) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, internalName, null, "java/lang/Object", null);

        // default ctor
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        // void next(String)
        mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "next", "(Ljava/lang/String;)V", null, null);
        mv.visitCode();
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        // void cur(String s) { this.next(s); }
        mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "cur", "(Ljava/lang/String;)V", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, internalName, "next", "(Ljava/lang/String;)V", false);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        cw.visitEnd();
        return cw.toByteArray();
    }

    private static byte[] genClass_CallNext_LongBeforeString(String internalName) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, internalName, null, "java/lang/Object", null);

        // default ctor
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        // void next(String)
        mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "next", "(Ljava/lang/String;)V", null, null);
        mv.visitCode();
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        // void cur(long x, String s) { this.next(s); }
        mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "cur", "(JLjava/lang/String;)V", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitVarInsn(Opcodes.ALOAD, 3);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, internalName, "next", "(Ljava/lang/String;)V", false);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        cw.visitEnd();
        return cw.toByteArray();
    }

    private static byte[] genClass_CallNext_WideArgs(String internalName) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, internalName, null, "java/lang/Object", null);

        // default ctor
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        // void next(long, String)
        mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "next", "(JLjava/lang/String;)V", null, null);
        mv.visitCode();
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        // void cur(long x, String s) { this.next(x, s); }
        mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "cur", "(JLjava/lang/String;)V", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitVarInsn(Opcodes.LLOAD, 1);
        mv.visitVarInsn(Opcodes.ALOAD, 3);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, internalName, "next", "(JLjava/lang/String;)V", false);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        cw.visitEnd();
        return cw.toByteArray();
    }

    private static byte[] genClass_NewFooThenCallNext(String internalName, String fooInternalName) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, internalName, null, "java/lang/Object", null);

        // default ctor
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        // void cur(long x, String s) { Foo f = new Foo(x, s); f.next(); }
        mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "cur", "(JLjava/lang/String;)V", null, null);
        mv.visitCode();
        mv.visitTypeInsn(Opcodes.NEW, fooInternalName);
        mv.visitInsn(Opcodes.DUP);
        mv.visitVarInsn(Opcodes.LLOAD, 1);
        mv.visitVarInsn(Opcodes.ALOAD, 3);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, fooInternalName, "<init>", "(JLjava/lang/String;)V", false);
        mv.visitVarInsn(Opcodes.ASTORE, 4);
        mv.visitVarInsn(Opcodes.ALOAD, 4);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, fooInternalName, "next", "()V", false);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        cw.visitEnd();
        return cw.toByteArray();
    }

    @Test
    public void instanceCall_argSlotsMustIncludeReceiverExactlyOnce() {
        String owner = "test/Gen1";
        byte[] bytes = genClass_CallNext_StringParam(owner);
        MethodReference.Handle cur = new MethodReference.Handle(new ClassReference.Handle(owner), "cur", "(Ljava/lang/String;)V");
        MethodReference.Handle next = new MethodReference.Handle(new ClassReference.Handle(owner), "next", "(Ljava/lang/String;)V");
        TaintPass pass = runTaintPass(bytes, 0, cur, next);
        assertNotNull(pass);
        assertEquals(Collections.singleton(0), pass.getParamIndices());
    }

    @Test
    public void seedParamIndexMustRespectWideLocals() {
        String owner = "test/Gen2";
        byte[] bytes = genClass_CallNext_LongBeforeString(owner);
        MethodReference.Handle cur = new MethodReference.Handle(new ClassReference.Handle(owner), "cur", "(JLjava/lang/String;)V");
        MethodReference.Handle next = new MethodReference.Handle(new ClassReference.Handle(owner), "next", "(Ljava/lang/String;)V");
        TaintPass pass = runTaintPass(bytes, 1, cur, next);
        assertNotNull(pass);
        assertEquals(Collections.singleton(0), pass.getParamIndices());
    }

    @Test
    public void wideArgsInSinkSignatureMustMapToCorrectParamIndex() {
        String owner = "test/Gen4";
        byte[] bytes = genClass_CallNext_WideArgs(owner);
        MethodReference.Handle cur = new MethodReference.Handle(new ClassReference.Handle(owner), "cur", "(JLjava/lang/String;)V");
        MethodReference.Handle next = new MethodReference.Handle(new ClassReference.Handle(owner), "next", "(JLjava/lang/String;)V");
        TaintPass pass = runTaintPass(bytes, 1, cur, next);
        assertNotNull(pass);
        assertEquals(Collections.singleton(1), pass.getParamIndices());
    }

    @Test
    public void ctorArgUnionMustNotLoseTaintWhenWideArgsPresent() {
        String owner = "test/Gen3";
        String foo = "test/Foo";
        byte[] bytes = genClass_NewFooThenCallNext(owner, foo);
        MethodReference.Handle cur = new MethodReference.Handle(new ClassReference.Handle(owner), "cur", "(JLjava/lang/String;)V");
        MethodReference.Handle next = new MethodReference.Handle(new ClassReference.Handle(foo), "next", "()V");
        TaintPass pass = runTaintPass(bytes, 1, cur, next);
        assertNotNull(pass);
        assertTrue(pass.getParamIndices().contains(Sanitizer.THIS_PARAM));
    }
}
