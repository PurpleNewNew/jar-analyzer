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

import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.starter.Const;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import me.n1ar4.jar.analyzer.core.MethodCallKey;
import me.n1ar4.jar.analyzer.core.MethodCallMeta;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class MethodCallClassVisitor extends ClassVisitor {
    private String ownerClass;

    private final HashMap<MethodReference.Handle, HashSet<MethodReference.Handle>> methodCalls;
    private final Map<MethodCallKey, MethodCallMeta> methodCallMeta;
    private final Map<MethodReference.Handle, MethodReference> methodMap;

    public MethodCallClassVisitor(HashMap<MethodReference.Handle,
            HashSet<MethodReference.Handle>> methodCalls,
            Map<MethodCallKey, MethodCallMeta> methodCallMeta,
            Map<MethodReference.Handle, MethodReference> methodMap) {
        this(methodCalls, methodCallMeta, methodMap, null);
    }

    public MethodCallClassVisitor(HashMap<MethodReference.Handle,
            HashSet<MethodReference.Handle>> methodCalls,
            Map<MethodCallKey, MethodCallMeta> methodCallMeta,
            Map<MethodReference.Handle, MethodReference> methodMap,
            ClassVisitor cv) {
        super(Const.ASMVersion, cv);
        this.methodCalls = methodCalls;
        this.methodCallMeta = methodCallMeta;
        this.methodMap = methodMap;
    }

    @Override
    public void visit(int version, int access, String ownerClass, String signature,
                      String superName, String[] interfaces) {
        super.visit(version, access, ownerClass, signature, superName, interfaces);
        this.ownerClass = ownerClass;
    }

    public MethodVisitor visitMethod(int access, String methodName, String desc,
                                     String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, methodName, desc, signature, exceptions);
        return new MethodCallMethodVisitor(api, mv, this.ownerClass, methodName, desc,
                methodCalls, methodCallMeta, methodMap);
    }
}
