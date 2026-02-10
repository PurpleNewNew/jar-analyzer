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
 * Infer callback edges for common CompletableFuture/CompletionStage APIs.
 * <p>
 * Note: This is RTA-based and only links to instantiated functional interface implementations.
 */
final class CompletableFutureEdgeRule implements EdgeInferRule {
    private static final String CF_OWNER = "java/util/concurrent/CompletableFuture";
    private static final String STAGE_OWNER = "java/util/concurrent/CompletionStage";

    private static final String RUN_ASYNC = "runAsync";
    private static final String SUPPLY_ASYNC = "supplyAsync";
    private static final String THEN_RUN = "thenRun";
    private static final String THEN_RUN_ASYNC = "thenRunAsync";
    private static final String THEN_ACCEPT = "thenAccept";
    private static final String THEN_ACCEPT_ASYNC = "thenAcceptAsync";
    private static final String THEN_APPLY = "thenApply";
    private static final String THEN_APPLY_ASYNC = "thenApplyAsync";

    private static final String RUN_ASYNC_DESC_1 =
            "(Ljava/lang/Runnable;)Ljava/util/concurrent/CompletableFuture;";
    private static final String RUN_ASYNC_DESC_2 =
            "(Ljava/lang/Runnable;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;";
    private static final String SUPPLY_ASYNC_DESC_1 =
            "(Ljava/util/function/Supplier;)Ljava/util/concurrent/CompletableFuture;";
    private static final String SUPPLY_ASYNC_DESC_2 =
            "(Ljava/util/function/Supplier;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;";

    private static final String THEN_RUN_DESC =
            "(Ljava/lang/Runnable;)Ljava/util/concurrent/CompletionStage;";
    private static final String THEN_RUN_CF_DESC =
            "(Ljava/lang/Runnable;)Ljava/util/concurrent/CompletableFuture;";
    private static final String THEN_ACCEPT_DESC =
            "(Ljava/util/function/Consumer;)Ljava/util/concurrent/CompletionStage;";
    private static final String THEN_ACCEPT_CF_DESC =
            "(Ljava/util/function/Consumer;)Ljava/util/concurrent/CompletableFuture;";
    private static final String THEN_APPLY_DESC =
            "(Ljava/util/function/Function;)Ljava/util/concurrent/CompletionStage;";
    private static final String THEN_APPLY_CF_DESC =
            "(Ljava/util/function/Function;)Ljava/util/concurrent/CompletableFuture;";

    private static final String RUNNABLE_RUN_NAME = "run";
    private static final String RUNNABLE_RUN_DESC = "()V";
    private static final String SUPPLIER_GET_NAME = "get";
    private static final String SUPPLIER_GET_DESC = "()Ljava/lang/Object;";
    private static final String CONSUMER_ACCEPT_NAME = "accept";
    private static final String CONSUMER_ACCEPT_DESC = "(Ljava/lang/Object;)V";
    private static final String FUNCTION_APPLY_NAME = "apply";
    private static final String FUNCTION_APPLY_DESC = "(Ljava/lang/Object;)Ljava/lang/Object;";

    private static final String REASON_RUNNABLE = "completable_future_runnable";
    private static final String REASON_SUPPLIER = "completable_future_supplier";
    private static final String REASON_CONSUMER = "completable_future_consumer";
    private static final String REASON_FUNCTION = "completable_future_function";
    private static final int MAX_TARGETS_PER_CALL = 50;

    private static final ClassReference.Handle RUNNABLE =
            new ClassReference.Handle("java/lang/Runnable");
    private static final ClassReference.Handle SUPPLIER =
            new ClassReference.Handle("java/util/function/Supplier");
    private static final ClassReference.Handle CONSUMER =
            new ClassReference.Handle("java/util/function/Consumer");
    private static final ClassReference.Handle FUNCTION =
            new ClassReference.Handle("java/util/function/Function");

    @Override
    public String id() {
        return "completableFuture";
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
                collectTargetsByJar(ctx, inh, RUNNABLE, RUNNABLE_RUN_NAME, RUNNABLE_RUN_DESC);
        Map<Integer, List<MethodReference.Handle>> supplierByJar =
                collectTargetsByJar(ctx, inh, SUPPLIER, SUPPLIER_GET_NAME, SUPPLIER_GET_DESC);
        Map<Integer, List<MethodReference.Handle>> consumerByJar =
                collectTargetsByJar(ctx, inh, CONSUMER, CONSUMER_ACCEPT_NAME, CONSUMER_ACCEPT_DESC);
        Map<Integer, List<MethodReference.Handle>> functionByJar =
                collectTargetsByJar(ctx, inh, FUNCTION, FUNCTION_APPLY_NAME, FUNCTION_APPLY_DESC);
        if (runnableByJar.isEmpty() && supplierByJar.isEmpty()
                && consumerByJar.isEmpty() && functionByJar.isEmpty()) {
            return 0;
        }
        List<MethodReference.Handle> allRunnable = flatten(runnableByJar);
        List<MethodReference.Handle> allSupplier = flatten(supplierByJar);
        List<MethodReference.Handle> allConsumer = flatten(consumerByJar);
        List<MethodReference.Handle> allFunction = flatten(functionByJar);

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
            boolean needRunnable = callsRunnableCallback(callees);
            boolean needSupplier = callsSupplierCallback(callees);
            boolean needConsumer = callsConsumerCallback(callees);
            boolean needFunction = callsFunctionCallback(callees);
            if (!needRunnable && !needSupplier && !needConsumer && !needFunction) {
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
            if (needSupplier && (!supplierByJar.isEmpty())) {
                List<MethodReference.Handle> targets = jarId == null ? null : supplierByJar.get(jarId);
                if (targets == null || targets.isEmpty()) {
                    targets = allSupplier;
                }
                added += addCallbackTargets(ctx, caller, callees, targets, REASON_SUPPLIER);
            }
            if (needConsumer && (!consumerByJar.isEmpty())) {
                List<MethodReference.Handle> targets = jarId == null ? null : consumerByJar.get(jarId);
                if (targets == null || targets.isEmpty()) {
                    targets = allConsumer;
                }
                added += addCallbackTargets(ctx, caller, callees, targets, REASON_CONSUMER);
            }
            if (needFunction && (!functionByJar.isEmpty())) {
                List<MethodReference.Handle> targets = jarId == null ? null : functionByJar.get(jarId);
                if (targets == null || targets.isEmpty()) {
                    targets = allFunction;
                }
                added += addCallbackTargets(ctx, caller, callees, targets, REASON_FUNCTION);
            }
        }
        return added;
    }

    private static boolean callsRunnableCallback(Set<MethodReference.Handle> callees) {
        if (callees == null || callees.isEmpty()) {
            return false;
        }
        for (MethodReference.Handle callee : callees) {
            if (!isCfOrStageCallee(callee)) {
                continue;
            }
            String name = callee.getName();
            String desc = callee.getDesc();
            if (RUN_ASYNC.equals(name) && (RUN_ASYNC_DESC_1.equals(desc) || RUN_ASYNC_DESC_2.equals(desc))) {
                return true;
            }
            if ((THEN_RUN.equals(name) || THEN_RUN_ASYNC.equals(name))
                    && (THEN_RUN_DESC.equals(desc) || THEN_RUN_CF_DESC.equals(desc))) {
                return true;
            }
        }
        return false;
    }

    private static boolean callsSupplierCallback(Set<MethodReference.Handle> callees) {
        if (callees == null || callees.isEmpty()) {
            return false;
        }
        for (MethodReference.Handle callee : callees) {
            if (!isCfOrStageCallee(callee)) {
                continue;
            }
            String name = callee.getName();
            String desc = callee.getDesc();
            if (SUPPLY_ASYNC.equals(name) && (SUPPLY_ASYNC_DESC_1.equals(desc) || SUPPLY_ASYNC_DESC_2.equals(desc))) {
                return true;
            }
        }
        return false;
    }

    private static boolean callsConsumerCallback(Set<MethodReference.Handle> callees) {
        if (callees == null || callees.isEmpty()) {
            return false;
        }
        for (MethodReference.Handle callee : callees) {
            if (!isCfOrStageCallee(callee)) {
                continue;
            }
            String name = callee.getName();
            String desc = callee.getDesc();
            if ((THEN_ACCEPT.equals(name) || THEN_ACCEPT_ASYNC.equals(name))
                    && (THEN_ACCEPT_DESC.equals(desc) || THEN_ACCEPT_CF_DESC.equals(desc))) {
                return true;
            }
        }
        return false;
    }

    private static boolean callsFunctionCallback(Set<MethodReference.Handle> callees) {
        if (callees == null || callees.isEmpty()) {
            return false;
        }
        for (MethodReference.Handle callee : callees) {
            if (!isCfOrStageCallee(callee)) {
                continue;
            }
            String name = callee.getName();
            String desc = callee.getDesc();
            if ((THEN_APPLY.equals(name) || THEN_APPLY_ASYNC.equals(name))
                    && (THEN_APPLY_DESC.equals(desc) || THEN_APPLY_CF_DESC.equals(desc))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCfOrStageCallee(MethodReference.Handle callee) {
        if (callee == null || callee.getClassReference() == null) {
            return false;
        }
        String owner = callee.getClassReference().getName();
        return CF_OWNER.equals(owner) || STAGE_OWNER.equals(owner);
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

