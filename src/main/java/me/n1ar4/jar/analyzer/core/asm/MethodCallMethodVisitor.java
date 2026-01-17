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

import me.n1ar4.jar.analyzer.core.MethodCallKey;
import me.n1ar4.jar.analyzer.core.MethodCallMeta;
import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class MethodCallMethodVisitor extends MethodVisitor {
    private final HashSet<MethodReference.Handle> calledMethods;
    private final MethodReference.Handle caller;
    private final Map<MethodCallKey, MethodCallMeta> methodCallMeta;

    public MethodCallMethodVisitor(final int api, final MethodVisitor mv,
                                   final String ownerClass, String name, String desc,
                                   HashMap<MethodReference.Handle,
                                           HashSet<MethodReference.Handle>> methodCalls,
                                   Map<MethodCallKey, MethodCallMeta> methodCallMeta) {
        super(api, mv);
        this.calledMethods = new HashSet<>();
        this.caller = new MethodReference.Handle(new ClassReference.Handle(ownerClass), name, desc);
        this.methodCallMeta = methodCallMeta;
        methodCalls.put(caller, calledMethods);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
        MethodReference.Handle callee = new MethodReference.Handle(
                new ClassReference.Handle(owner), opcode, name, desc);
        calledMethods.add(callee);
        MethodCallMeta.record(methodCallMeta, MethodCallKey.of(caller, callee),
                MethodCallMeta.TYPE_DIRECT, MethodCallMeta.CONF_HIGH);
        super.visitMethodInsn(opcode, owner, name, desc, itf);
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String descriptor,
                                       Handle bootstrapMethodHandle,
                                       Object... bootstrapMethodArguments) {
        for (Object bsmArg : bootstrapMethodArguments) {
            if (bsmArg instanceof Handle) {
                Handle handle = (Handle) bsmArg;
                MethodReference.Handle callee = new MethodReference.Handle(
                        new ClassReference.Handle(handle.getOwner()),
                        handle.getName(), handle.getDesc());
                calledMethods.add(callee);
                MethodCallMeta.record(methodCallMeta, MethodCallKey.of(caller, callee),
                        MethodCallMeta.TYPE_INDY, MethodCallMeta.CONF_MEDIUM);
            }
        }
        super.visitInvokeDynamicInsn(name, descriptor,
                bootstrapMethodHandle, bootstrapMethodArguments);
    }
}
