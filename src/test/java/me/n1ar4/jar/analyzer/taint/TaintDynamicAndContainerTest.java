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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaintDynamicAndContainerTest {
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

    private static byte[] genContainerBridgeClass(String internalName) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, internalName, null, "java/lang/Object", null);

        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "next", "(Ljava/lang/Object;)V", null, null);
        mv.visitCode();
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "cur", "(Ljava/lang/String;)V", null, null);
        mv.visitCode();
        mv.visitTypeInsn(Opcodes.NEW, "java/util/ArrayList");
        mv.visitInsn(Opcodes.DUP);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/util/ArrayList", "<init>", "()V", false);
        mv.visitVarInsn(Opcodes.ASTORE, 2);
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/util/ArrayList", "add", "(Ljava/lang/Object;)Z", false);
        mv.visitInsn(Opcodes.POP);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, internalName, "next", "(Ljava/lang/Object;)V", false);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        cw.visitEnd();
        return cw.toByteArray();
    }

    private static byte[] genMethodHandleSetterClass(String internalName) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, internalName, null, "java/lang/Object", null);

        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "next", "(Ljava/lang/Object;)V", null, null);
        mv.visitCode();
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "cur",
                "(Ljava/lang/invoke/MethodHandle;Ljava/lang/Object;Ljava/lang/String;)V",
                null,
                new String[]{"java/lang/Throwable"});
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitVarInsn(Opcodes.ALOAD, 3);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "invokeExact",
                "(Ljava/lang/Object;Ljava/lang/String;)V", false);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, internalName, "next", "(Ljava/lang/Object;)V", false);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        cw.visitEnd();
        return cw.toByteArray();
    }

    private static byte[] genSingletonListBridgeClass(String internalName) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, internalName, null, "java/lang/Object", null);

        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "next", "(Ljava/lang/Object;)V", null, null);
        mv.visitCode();
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "cur", "(Ljava/lang/String;)V", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Collections", "singletonList",
                "(Ljava/lang/Object;)Ljava/util/List;", false);
        mv.visitVarInsn(Opcodes.ASTORE, 2);

        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitInsn(Opcodes.ICONST_0);
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/List", "get",
                "(I)Ljava/lang/Object;", true);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, internalName, "next", "(Ljava/lang/Object;)V", false);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        cw.visitEnd();
        return cw.toByteArray();
    }

    private static byte[] genInterfaceSetterClass(String internalName, String ifaceInternalName) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, internalName, null, "java/lang/Object", null);

        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "next", "(Ljava/lang/Object;)V", null, null);
        mv.visitCode();
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "cur", "(L" + ifaceInternalName + ";Ljava/lang/String;)V", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, ifaceInternalName, "setName",
                "(Ljava/lang/String;)V", true);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, internalName, "next", "(Ljava/lang/Object;)V", false);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        cw.visitEnd();
        return cw.toByteArray();
    }

    @Test
    void containerMarkerShouldNotPollutePlainObjectParam() {
        String owner = "test/GenContainer";
        byte[] bytes = genContainerBridgeClass(owner);
        MethodReference.Handle cur = new MethodReference.Handle(
                new ClassReference.Handle(owner), "cur", "(Ljava/lang/String;)V");
        MethodReference.Handle next = new MethodReference.Handle(
                new ClassReference.Handle(owner), "next", "(Ljava/lang/Object;)V");
        TaintPass pass = runTaintPass(bytes, 0, cur, next);
        assertNotNull(pass);
        assertTrue(pass.getParamIndices().isEmpty());
    }

    @Test
    void methodHandleVoidInvokeShouldPropagateToTargetObject() {
        String owner = "test/GenMethodHandle";
        byte[] bytes = genMethodHandleSetterClass(owner);
        MethodReference.Handle cur = new MethodReference.Handle(
                new ClassReference.Handle(owner), "cur",
                "(Ljava/lang/invoke/MethodHandle;Ljava/lang/Object;Ljava/lang/String;)V");
        MethodReference.Handle next = new MethodReference.Handle(
                new ClassReference.Handle(owner), "next", "(Ljava/lang/Object;)V");
        TaintPass pass = runTaintPass(bytes, 2, cur, next);
        assertNotNull(pass);
        assertTrue(pass.getParamIndices().contains(0));
    }

    @Test
    void collectionsSingletonListShouldPropagateElementTaint() {
        String owner = "test/GenSingletonList";
        byte[] bytes = genSingletonListBridgeClass(owner);
        MethodReference.Handle cur = new MethodReference.Handle(
                new ClassReference.Handle(owner), "cur", "(Ljava/lang/String;)V");
        MethodReference.Handle next = new MethodReference.Handle(
                new ClassReference.Handle(owner), "next", "(Ljava/lang/Object;)V");
        TaintPass pass = runTaintPass(bytes, 0, cur, next);
        assertNotNull(pass);
        assertTrue(pass.getParamIndices().contains(0));
    }

    @Test
    void interfaceSetterShouldPropagateToReceiver() {
        String owner = "test/GenProxyLike";
        String iface = "test/ISetter";
        byte[] bytes = genInterfaceSetterClass(owner, iface);
        MethodReference.Handle cur = new MethodReference.Handle(
                new ClassReference.Handle(owner), "cur", "(Ltest/ISetter;Ljava/lang/String;)V");
        MethodReference.Handle next = new MethodReference.Handle(
                new ClassReference.Handle(owner), "next", "(Ljava/lang/Object;)V");
        TaintPass pass = runTaintPass(bytes, 1, cur, next);
        assertNotNull(pass);
        assertTrue(pass.getParamIndices().contains(0));
    }
}
