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

final class ThreadStartEdgeRule implements EdgeInferRule {
    private static final String THREAD_OWNER = "java/lang/Thread";
    private static final String START_NAME = "start";
    private static final String START_DESC = "()V";
    private static final String RUN_NAME = "run";
    private static final String RUN_DESC = "()V";
    private static final String REASON = "thread_start";
    private static final int MAX_TARGETS_PER_CALL = 50;

    private static final ClassReference.Handle THREAD = new ClassReference.Handle(THREAD_OWNER);

    @Override
    public String id() {
        return "threadStart";
    }

    @Override
    public int apply(BuildContext ctx) {
        if (ctx == null || ctx.methodCalls == null || ctx.methodCalls.isEmpty()) {
            return 0;
        }
        InheritanceMap inh = ctx.inheritanceMap;
        if (inh == null) {
            return 0;
        }
        Map<Integer, List<MethodReference.Handle>> runByJar = collectThreadRunTargetsByJar(ctx, inh);
        if (runByJar.isEmpty()) {
            return 0;
        }
        List<MethodReference.Handle> allTargets = flatten(runByJar);

        int added = 0;
        for (Map.Entry<MethodReference.Handle, HashSet<MethodReference.Handle>> entry : ctx.methodCalls.entrySet()) {
            MethodReference.Handle caller = entry.getKey();
            if (caller == null) {
                continue;
            }
            HashSet<MethodReference.Handle> callees = entry.getValue();
            if (callees == null || callees.isEmpty()) {
                continue;
            }
            if (!callsThreadStart(callees, inh)) {
                continue;
            }
            Integer jarId = resolveJarId(ctx, caller);
            List<MethodReference.Handle> targets = jarId == null ? null : runByJar.get(jarId);
            if (targets == null || targets.isEmpty()) {
                targets = allTargets;
            }
            int limit = Math.min(MAX_TARGETS_PER_CALL, targets.size());
            for (int i = 0; i < limit; i++) {
                MethodReference.Handle target = targets.get(i);
                if (target == null) {
                    continue;
                }
                MethodReference.Handle callTarget = new MethodReference.Handle(
                        target.getClassReference(),
                        Opcodes.INVOKEVIRTUAL,
                        target.getName(),
                        target.getDesc());
                if (MethodCallUtils.addCallee(callees, callTarget)) {
                    added++;
                }
                MethodCallMeta.record(ctx.methodCallMeta, MethodCallKey.of(caller, callTarget),
                        MethodCallMeta.TYPE_CALLBACK, MethodCallMeta.CONF_LOW, REASON, Opcodes.INVOKEVIRTUAL);
            }
        }
        return added;
    }

    private static boolean callsThreadStart(Set<MethodReference.Handle> callees, InheritanceMap inh) {
        for (MethodReference.Handle callee : callees) {
            if (callee == null || callee.getClassReference() == null) {
                continue;
            }
            if (!START_NAME.equals(callee.getName()) || !START_DESC.equals(callee.getDesc())) {
                continue;
            }
            String owner = callee.getClassReference().getName();
            if (THREAD_OWNER.equals(owner)) {
                return true;
            }
            // The invokevirtual owner may be a Thread subclass (e.g. new MyThread().start()).
            if (inh != null && inh.isSubclassOf(callee.getClassReference(), THREAD)) {
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

    private static Map<Integer, List<MethodReference.Handle>> collectThreadRunTargetsByJar(BuildContext ctx,
                                                                                           InheritanceMap inh) {
        Map<Integer, List<MethodReference.Handle>> out = new HashMap<>();
        if (ctx == null || ctx.instantiatedClasses == null || ctx.instantiatedClasses.isEmpty()) {
            return out;
        }
        if (ctx.methodMap == null || ctx.methodMap.isEmpty()) {
            return out;
        }
        for (ClassReference.Handle clazz : ctx.instantiatedClasses) {
            if (clazz == null || THREAD.equals(clazz)) {
                continue;
            }
            if (!inh.isSubclassOf(clazz, THREAD)) {
                continue;
            }
            MethodReference.Handle run = new MethodReference.Handle(clazz, RUN_NAME, RUN_DESC);
            MethodReference ref = ctx.methodMap.get(run);
            if (ref == null) {
                continue;
            }
            Integer jarId = ref.getJarId();
            if (jarId == null) {
                continue;
            }
            out.computeIfAbsent(jarId, k -> new ArrayList<>()).add(run);
        }
        return out;
    }

    private static List<MethodReference.Handle> flatten(Map<Integer, List<MethodReference.Handle>> map) {
        List<MethodReference.Handle> out = new ArrayList<>();
        for (List<MethodReference.Handle> v : map.values()) {
            if (v != null && !v.isEmpty()) {
                out.addAll(v);
            }
        }
        return out;
    }
}
