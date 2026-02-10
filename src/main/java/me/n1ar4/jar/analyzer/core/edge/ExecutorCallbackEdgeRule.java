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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Infer callback edges for common Executor/ExecutorService patterns.
 * <p>
 * This rule is intentionally conservative and RTA-based: it only links to
 * instantiated Runnable/Callable implementations from the current build.
 */
final class ExecutorCallbackEdgeRule implements EdgeInferRule {
    private static final String EXECUTOR_OWNER = "java/util/concurrent/Executor";
    private static final String EXECUTOR_SERVICE_OWNER = "java/util/concurrent/ExecutorService";
    private static final String EXECUTE_NAME = "execute";
    private static final String EXECUTE_DESC = "(Ljava/lang/Runnable;)V";
    private static final String SUBMIT_NAME = "submit";
    private static final String SUBMIT_RUNNABLE_DESC = "(Ljava/lang/Runnable;)Ljava/util/concurrent/Future;";
    private static final String SUBMIT_RUNNABLE_RESULT_DESC = "(Ljava/lang/Runnable;Ljava/lang/Object;)Ljava/util/concurrent/Future;";
    private static final String SUBMIT_CALLABLE_DESC = "(Ljava/util/concurrent/Callable;)Ljava/util/concurrent/Future;";

    private static final String RUN_NAME = "run";
    private static final String RUN_DESC = "()V";
    private static final String CALL_NAME = "call";
    private static final String CALL_DESC = "()Ljava/lang/Object;";

    private static final String REASON_RUNNABLE = "executor_runnable";
    private static final String REASON_CALLABLE = "executor_callable";
    private static final int MAX_TARGETS_PER_CALL = 50;

    private static final ClassReference.Handle RUNNABLE =
            new ClassReference.Handle("java/lang/Runnable");
    private static final ClassReference.Handle CALLABLE =
            new ClassReference.Handle("java/util/concurrent/Callable");

    @Override
    public String id() {
        return "executor";
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
        Map<Integer, List<MethodReference.Handle>> runnableByJar =
                collectTargetsByJar(ctx, inh, RUNNABLE, RUN_NAME, RUN_DESC);
        Map<Integer, List<MethodReference.Handle>> callableByJar =
                collectTargetsByJar(ctx, inh, CALLABLE, CALL_NAME, CALL_DESC);
        if (runnableByJar.isEmpty() && callableByJar.isEmpty()) {
            return 0;
        }
        List<MethodReference.Handle> allRunnable = flatten(runnableByJar);
        List<MethodReference.Handle> allCallable = flatten(callableByJar);

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
            boolean needRunnable = callsExecuteOrSubmitRunnable(callees);
            boolean needCallable = callsSubmitCallable(callees);
            if (!needRunnable && !needCallable) {
                continue;
            }
            Integer jarId = resolveJarId(ctx, caller);
            if (needRunnable && (!runnableByJar.isEmpty())) {
                List<MethodReference.Handle> targets = jarId == null ? null : runnableByJar.get(jarId);
                if (targets == null || targets.isEmpty()) {
                    targets = allRunnable;
                }
                added += addCallbackTargets(ctx, caller, callees, targets, REASON_RUNNABLE);
            }
            if (needCallable && (!callableByJar.isEmpty())) {
                List<MethodReference.Handle> targets = jarId == null ? null : callableByJar.get(jarId);
                if (targets == null || targets.isEmpty()) {
                    targets = allCallable;
                }
                added += addCallbackTargets(ctx, caller, callees, targets, REASON_CALLABLE);
            }
        }
        return added;
    }

    private static boolean callsExecuteOrSubmitRunnable(Set<MethodReference.Handle> callees) {
        if (callees == null || callees.isEmpty()) {
            return false;
        }
        for (MethodReference.Handle callee : callees) {
            if (callee == null || callee.getClassReference() == null) {
                continue;
            }
            String owner = callee.getClassReference().getName();
            String name = callee.getName();
            String desc = callee.getDesc();
            if (EXECUTOR_OWNER.equals(owner) && EXECUTE_NAME.equals(name) && EXECUTE_DESC.equals(desc)) {
                return true;
            }
            if (!EXECUTOR_SERVICE_OWNER.equals(owner)) {
                continue;
            }
            if (EXECUTE_NAME.equals(name) && EXECUTE_DESC.equals(desc)) {
                return true;
            }
            if (SUBMIT_NAME.equals(name)
                    && (SUBMIT_RUNNABLE_DESC.equals(desc) || SUBMIT_RUNNABLE_RESULT_DESC.equals(desc))) {
                return true;
            }
        }
        return false;
    }

    private static boolean callsSubmitCallable(Set<MethodReference.Handle> callees) {
        if (callees == null || callees.isEmpty()) {
            return false;
        }
        for (MethodReference.Handle callee : callees) {
            if (callee == null || callee.getClassReference() == null) {
                continue;
            }
            String owner = callee.getClassReference().getName();
            if (!EXECUTOR_SERVICE_OWNER.equals(owner)) {
                continue;
            }
            if (SUBMIT_NAME.equals(callee.getName()) && SUBMIT_CALLABLE_DESC.equals(callee.getDesc())) {
                return true;
            }
        }
        return false;
    }

    private static int addCallbackTargets(BuildContext ctx,
                                         MethodReference.Handle caller,
                                         HashSet<MethodReference.Handle> callees,
                                         List<MethodReference.Handle> targets,
                                         String reason) {
        if (targets == null || targets.isEmpty()) {
            return 0;
        }
        int added = 0;
        int limit = Math.min(MAX_TARGETS_PER_CALL, targets.size());
        for (int i = 0; i < limit; i++) {
            MethodReference.Handle target = targets.get(i);
            if (target == null) {
                continue;
            }
            if (MethodCallUtils.addCallee(callees, target)) {
                added++;
                MethodCallMeta.record(ctx.methodCallMeta, MethodCallKey.of(caller, target),
                        MethodCallMeta.TYPE_CALLBACK, MethodCallMeta.CONF_LOW, reason, null);
            }
        }
        return added;
    }

    private static Integer resolveJarId(BuildContext ctx, MethodReference.Handle method) {
        if (ctx == null || ctx.methodMap == null || method == null) {
            return null;
        }
        MethodReference ref = ctx.methodMap.get(method);
        return ref == null ? null : ref.getJarId();
    }

    private static Map<Integer, List<MethodReference.Handle>> collectTargetsByJar(BuildContext ctx,
                                                                                  InheritanceMap inh,
                                                                                  ClassReference.Handle iface,
                                                                                  String name,
                                                                                  String desc) {
        Map<Integer, List<MethodReference.Handle>> out = new HashMap<>();
        if (ctx == null || ctx.instantiatedClasses == null || ctx.instantiatedClasses.isEmpty()) {
            return out;
        }
        if (ctx.methodMap == null || ctx.methodMap.isEmpty()) {
            return out;
        }
        for (ClassReference.Handle clazz : ctx.instantiatedClasses) {
            if (clazz == null) {
                continue;
            }
            if (!inh.isSubclassOf(clazz, iface)) {
                continue;
            }
            MethodReference.Handle target = new MethodReference.Handle(clazz, name, desc);
            MethodReference ref = ctx.methodMap.get(target);
            if (ref == null) {
                continue;
            }
            Integer jarId = ref.getJarId();
            if (jarId == null) {
                continue;
            }
            out.computeIfAbsent(jarId, k -> new ArrayList<>()).add(target);
        }
        for (List<MethodReference.Handle> list : out.values()) {
            if (list != null && list.size() > 1) {
                list.sort((a, b) -> {
                    String an = a == null || a.getClassReference() == null ? "" : a.getClassReference().getName();
                    String bn = b == null || b.getClassReference() == null ? "" : b.getClassReference().getName();
                    return an.compareTo(bn);
                });
            }
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
        if (out.size() > 1) {
            out.sort((a, b) -> {
                String an = a == null || a.getClassReference() == null ? "" : a.getClassReference().getName();
                String bn = b == null || b.getClassReference() == null ? "" : b.getClassReference().getName();
                return an.compareTo(bn);
            });
        }
        return out;
    }
}

