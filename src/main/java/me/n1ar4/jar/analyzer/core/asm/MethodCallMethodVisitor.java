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
import me.n1ar4.jar.analyzer.core.MethodCallUtils;
import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class MethodCallMethodVisitor extends MethodVisitor {
    private static final String REASON_LAMBDA = "lambda";
    private final HashSet<MethodReference.Handle> calledMethods;
    private final MethodReference.Handle caller;
    private final Map<MethodCallKey, MethodCallMeta> methodCallMeta;
    private final Map<MethodReference.Handle, MethodReference> methodMap;
    private final HashMap<MethodReference.Handle, HashSet<MethodReference.Handle>> methodCalls;

    public MethodCallMethodVisitor(final int api, final MethodVisitor mv,
                                   final String ownerClass, String name, String desc,
                                   HashMap<MethodReference.Handle,
                                           HashSet<MethodReference.Handle>> methodCalls,
                                   Map<MethodCallKey, MethodCallMeta> methodCallMeta,
                                   Map<MethodReference.Handle, MethodReference> methodMap) {
        super(api, mv);
        this.caller = new MethodReference.Handle(new ClassReference.Handle(ownerClass), name, desc);
        this.methodCallMeta = methodCallMeta;
        this.methodMap = methodMap;
        this.methodCalls = methodCalls;
        HashSet<MethodReference.Handle> existing = methodCalls.get(caller);
        if (existing == null) {
            existing = new HashSet<>();
            methodCalls.put(caller, existing);
        }
        this.calledMethods = existing;
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
        MethodReference.Handle callee = new MethodReference.Handle(
                new ClassReference.Handle(owner), opcode, name, desc);
        MethodCallUtils.addCallee(calledMethods, callee);
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
                        Opcodes.INVOKEDYNAMIC,
                        handle.getName(), handle.getDesc());
                MethodCallUtils.addCallee(calledMethods, callee);
                MethodCallMeta.record(methodCallMeta, MethodCallKey.of(caller, callee),
                        MethodCallMeta.TYPE_INDY, MethodCallMeta.CONF_MEDIUM);
            }
        }
        LambdaTarget lambda = resolveLambdaTarget(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
        if (lambda != null && lambda.implHandle != null) {
            MethodReference.Handle implHandle = lambda.implHandle;
            if (implHandle.getOpcode() == null || implHandle.getOpcode() < 0) {
                implHandle = new MethodReference.Handle(
                        implHandle.getClassReference(),
                        Opcodes.INVOKEDYNAMIC,
                        implHandle.getName(),
                        implHandle.getDesc());
            }
            MethodCallUtils.addCallee(calledMethods, implHandle);
            MethodCallMeta.record(methodCallMeta, MethodCallKey.of(caller, implHandle),
                    MethodCallMeta.TYPE_INDY, MethodCallMeta.CONF_MEDIUM, REASON_LAMBDA);
            if (lambda.samHandle != null
                    && isKnownMethod(lambda.implHandle)
                    && isKnownMethod(lambda.samHandle)) {
                HashSet<MethodReference.Handle> samCallees =
                        methodCalls.computeIfAbsent(lambda.samHandle, k -> new HashSet<>());
                MethodCallUtils.addCallee(samCallees, implHandle);
                MethodCallMeta.record(methodCallMeta, MethodCallKey.of(lambda.samHandle, implHandle),
                        MethodCallMeta.TYPE_INDY, MethodCallMeta.CONF_MEDIUM, REASON_LAMBDA);
            }
        }
        super.visitInvokeDynamicInsn(name, descriptor,
                bootstrapMethodHandle, bootstrapMethodArguments);
    }

    private static LambdaTarget resolveLambdaTarget(String indyName,
                                                    String indyDesc,
                                                    Handle bsm,
                                                    Object[] bsmArgs) {
        if (bsm == null || bsmArgs == null || bsmArgs.length < 3) {
            return null;
        }
        if (!"java/lang/invoke/LambdaMetafactory".equals(bsm.getOwner())) {
            return null;
        }
        String bsmName = bsm.getName();
        if (!"metafactory".equals(bsmName) && !"altMetafactory".equals(bsmName)) {
            return null;
        }
        Object arg0 = bsmArgs[0];
        Object arg1 = bsmArgs[1];
        Object arg2 = bsmArgs[2];
        if (!(arg0 instanceof Type) || !(arg1 instanceof Handle) || !(arg2 instanceof Type)) {
            return null;
        }
        Type samType = (Type) arg0;
        Type instantiatedType = (Type) arg2;
        if (samType.getSort() != Type.METHOD || instantiatedType.getSort() != Type.METHOD) {
            return null;
        }
        Type fiType = Type.getReturnType(indyDesc);
        if (fiType.getSort() != Type.OBJECT) {
            return null;
        }
        Handle impl = (Handle) arg1;
        MethodReference.Handle samHandle = new MethodReference.Handle(
                new ClassReference.Handle(fiType.getInternalName()),
                indyName, samType.getDescriptor());
        MethodReference.Handle implHandle = new MethodReference.Handle(
                new ClassReference.Handle(impl.getOwner()),
                impl.getName(), impl.getDesc());
        return new LambdaTarget(samHandle, implHandle);
    }

    private static final class LambdaTarget {
        private final MethodReference.Handle samHandle;
        private final MethodReference.Handle implHandle;

        private LambdaTarget(MethodReference.Handle samHandle,
                             MethodReference.Handle implHandle) {
            this.samHandle = samHandle;
            this.implHandle = implHandle;
        }
    }

    private boolean isKnownMethod(MethodReference.Handle handle) {
        if (methodMap == null || handle == null) {
            return false;
        }
        return methodMap.containsKey(handle);
    }
}
