/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.core.edge;

import me.n1ar4.jar.analyzer.core.InheritanceMap;
import me.n1ar4.jar.analyzer.core.MethodCallKey;
import me.n1ar4.jar.analyzer.core.MethodCallMeta;
import me.n1ar4.jar.analyzer.core.MethodCallUtils;
import me.n1ar4.jar.analyzer.core.build.BuildContext;
import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class DynamicProxyEdgeRule implements EdgeInferRule {
    private static final String PROXY_OWNER = "java/lang/reflect/Proxy";
    private static final String PROXY_NEW = "newProxyInstance";
    private static final String PROXY_NEW_DESC =
            "(Ljava/lang/ClassLoader;[Ljava/lang/Class;Ljava/lang/reflect/InvocationHandler;)Ljava/lang/Object;";
    private static final String HANDLER_OWNER = "java/lang/reflect/InvocationHandler";
    private static final String INVOKE_NAME = "invoke";
    private static final String INVOKE_DESC =
            "(Ljava/lang/Object;Ljava/lang/reflect/Method;[Ljava/lang/Object;)Ljava/lang/Object;";
    private static final String REASON = "dynamic_proxy";
    private static final int MAX_TARGETS_PER_CALL = 64;

    private static final ClassReference.Handle HANDLER = new ClassReference.Handle(HANDLER_OWNER);

    @Override
    public String id() {
        return "dynamicProxy";
    }

    @Override
    public int apply(BuildContext ctx) {
        if (ctx == null || ctx.methodCalls == null || ctx.methodCalls.isEmpty()) {
            return 0;
        }
        if (ctx.methodMap == null || ctx.methodMap.isEmpty()) {
            return 0;
        }
        Map<Integer, List<MethodReference.Handle>> invokeByJar = collectHandlerInvokeMethods(ctx);
        if (invokeByJar.isEmpty()) {
            return 0;
        }
        List<MethodReference.Handle> allTargets = flatten(invokeByJar);
        if (allTargets.isEmpty()) {
            return 0;
        }
        int added = 0;
        for (Map.Entry<MethodReference.Handle, HashSet<MethodReference.Handle>> entry : ctx.methodCalls.entrySet()) {
            MethodReference.Handle caller = entry.getKey();
            HashSet<MethodReference.Handle> callees = entry.getValue();
            if (caller == null || callees == null || callees.isEmpty()) {
                continue;
            }
            if (!callsProxyFactory(callees)) {
                continue;
            }
            Integer jarId = resolveJarId(ctx, caller);
            List<MethodReference.Handle> scoped = jarId == null ? null : invokeByJar.get(jarId);
            List<MethodReference.Handle> targets = (scoped == null || scoped.isEmpty()) ? allTargets : scoped;
            int limit = Math.min(MAX_TARGETS_PER_CALL, targets.size());
            for (int i = 0; i < limit; i++) {
                MethodReference.Handle target = targets.get(i);
                if (target == null) {
                    continue;
                }
                MethodReference.Handle callTarget = new MethodReference.Handle(
                        target.getClassReference(),
                        Opcodes.INVOKEINTERFACE,
                        target.getName(),
                        target.getDesc()
                );
                if (MethodCallUtils.addCallee(callees, callTarget)) {
                    added++;
                    MethodCallMeta.record(ctx.methodCallMeta, MethodCallKey.of(caller, callTarget),
                            MethodCallMeta.TYPE_CALLBACK, MethodCallMeta.CONF_MEDIUM, REASON, Opcodes.INVOKEINTERFACE);
                }
            }
        }
        return added;
    }

    private static boolean callsProxyFactory(Set<MethodReference.Handle> callees) {
        if (callees == null || callees.isEmpty()) {
            return false;
        }
        for (MethodReference.Handle callee : callees) {
            if (callee == null || callee.getClassReference() == null) {
                continue;
            }
            if (!PROXY_OWNER.equals(callee.getClassReference().getName())) {
                continue;
            }
            if (!PROXY_NEW.equals(callee.getName())) {
                continue;
            }
            if (PROXY_NEW_DESC.equals(callee.getDesc())) {
                return true;
            }
        }
        return false;
    }

    private static Integer resolveJarId(BuildContext ctx, MethodReference.Handle method) {
        if (ctx == null || ctx.methodMap == null || method == null) {
            return null;
        }
        MethodReference ref = ctx.methodMap.get(method);
        return ref == null ? null : ref.getJarId();
    }

    private static Map<Integer, List<MethodReference.Handle>> collectHandlerInvokeMethods(BuildContext ctx) {
        Map<Integer, List<MethodReference.Handle>> out = new HashMap<>();
        Map<MethodReference.Handle, MethodReference> methodMap = ctx.methodMap;
        if (methodMap == null || methodMap.isEmpty()) {
            return out;
        }
        Map<ClassReference.Handle, ClassReference> classMap = ctx.classMap;
        InheritanceMap inheritance = ctx.inheritanceMap;
        for (Map.Entry<MethodReference.Handle, MethodReference> entry : methodMap.entrySet()) {
            MethodReference.Handle handle = entry.getKey();
            MethodReference ref = entry.getValue();
            if (handle == null || ref == null) {
                continue;
            }
            if (!INVOKE_NAME.equals(handle.getName()) || !INVOKE_DESC.equals(handle.getDesc())) {
                continue;
            }
            ClassReference.Handle owner = handle.getClassReference();
            if (!isHandlerImplementer(owner, classMap, inheritance)) {
                continue;
            }
            Integer jarId = ref.getJarId();
            if (jarId == null) {
                continue;
            }
            out.computeIfAbsent(jarId, k -> new ArrayList<>()).add(handle);
        }
        return out;
    }

    private static boolean isHandlerImplementer(ClassReference.Handle owner,
                                                Map<ClassReference.Handle, ClassReference> classMap,
                                                InheritanceMap inheritance) {
        if (owner == null) {
            return false;
        }
        if (HANDLER_OWNER.equals(owner.getName())) {
            return true;
        }
        if (inheritance != null && inheritance.isSubclassOf(owner, HANDLER)) {
            return true;
        }
        if (classMap == null || classMap.isEmpty()) {
            return false;
        }
        ClassReference clazz = classMap.get(owner);
        return clazz != null
                && clazz.getInterfaces() != null
                && clazz.getInterfaces().contains(HANDLER_OWNER);
    }

    private static List<MethodReference.Handle> flatten(Map<Integer, List<MethodReference.Handle>> byJar) {
        List<MethodReference.Handle> out = new ArrayList<>();
        if (byJar == null || byJar.isEmpty()) {
            return out;
        }
        for (List<MethodReference.Handle> v : byJar.values()) {
            if (v != null && !v.isEmpty()) {
                out.addAll(v);
            }
        }
        return out;
    }
}
