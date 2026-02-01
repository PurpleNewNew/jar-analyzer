/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.core.asm;

import me.n1ar4.jar.analyzer.starter.Const;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

public class ReflectionProbeClassVisitor extends ClassVisitor {
    private static final String CLASS_OWNER = "java/lang/Class";
    private static final String METHOD_OWNER = "java/lang/reflect/Method";
    private static final String CTOR_OWNER = "java/lang/reflect/Constructor";
    private static final String MH_OWNER = "java/lang/invoke/MethodHandle";

    private boolean hasReflection;

    public ReflectionProbeClassVisitor() {
        this(null);
    }

    public ReflectionProbeClassVisitor(ClassVisitor cv) {
        super(Const.ASMVersion, cv);
    }

    public boolean hasReflection() {
        return hasReflection;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc,
                                     String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        return new MethodVisitor(api, mv) {
            @Override
            public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                if (!hasReflection && isReflectionCandidate(owner, name, desc)) {
                    hasReflection = true;
                }
                super.visitMethodInsn(opcode, owner, name, desc, itf);
            }
        };
    }

    private static boolean isReflectionCandidate(String owner, String name, String desc) {
        if (owner == null || name == null || desc == null) {
            return false;
        }
        if (METHOD_OWNER.equals(owner)
                && "invoke".equals(name)
                && "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;".equals(desc)) {
            return true;
        }
        if (CTOR_OWNER.equals(owner)
                && "newInstance".equals(name)
                && "([Ljava/lang/Object;)Ljava/lang/Object;".equals(desc)) {
            return true;
        }
        if (CLASS_OWNER.equals(owner)
                && "newInstance".equals(name)
                && "()Ljava/lang/Object;".equals(desc)) {
            return true;
        }
        return isMethodHandleInvoke(owner, name, desc);
    }

    private static boolean isMethodHandleInvoke(String owner, String name, String desc) {
        if (!MH_OWNER.equals(owner)) {
            return false;
        }
        if (!"invoke".equals(name) && !"invokeExact".equals(name)
                && !"invokeWithArguments".equals(name)) {
            return false;
        }
        return desc.startsWith("(");
    }
}
