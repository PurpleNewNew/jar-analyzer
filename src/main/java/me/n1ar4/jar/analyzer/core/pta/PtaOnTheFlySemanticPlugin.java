/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.core.pta;

import me.n1ar4.jar.analyzer.core.InheritanceMap;
import me.n1ar4.jar.analyzer.core.MethodCallMeta;
import me.n1ar4.jar.analyzer.core.build.BuildContext;
import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Move high-value semantic callback rules into PTA on-the-fly loop.
 * Current wave includes:
 * 1) doPrivileged
 * 2) Thread.start
 * 3) Executor/ExecutorService callbacks
 * 4) CompletableFuture/CompletionStage callbacks
 */
final class PtaOnTheFlySemanticPlugin implements PtaPlugin {
    private static final int MAX_TARGETS_PER_CALL = 50;
    private static final int EDGE_OPCODE_VIRTUAL = Opcodes.INVOKEVIRTUAL;
    private static final int EDGE_OPCODE_INTERFACE = Opcodes.INVOKEINTERFACE;

    private static final String REASON_DO_PRIVILEGED = "doPrivileged";
    private static final String REASON_THREAD_START = "thread_start";
    private static final String REASON_EXECUTOR_RUNNABLE = "executor_runnable";
    private static final String REASON_EXECUTOR_CALLABLE = "executor_callable";
    private static final String REASON_CF_RUNNABLE = "completable_future_runnable";
    private static final String REASON_CF_SUPPLIER = "completable_future_supplier";
    private static final String REASON_CF_CONSUMER = "completable_future_consumer";
    private static final String REASON_CF_FUNCTION = "completable_future_function";

    private static final ClassReference.Handle THREAD = new ClassReference.Handle("java/lang/Thread");
    private static final ClassReference.Handle ACTION = new ClassReference.Handle("java/security/PrivilegedAction");
    private static final ClassReference.Handle ACTION_EX = new ClassReference.Handle("java/security/PrivilegedExceptionAction");
    private static final ClassReference.Handle EXECUTOR = new ClassReference.Handle("java/util/concurrent/Executor");
    private static final ClassReference.Handle EXECUTOR_SERVICE = new ClassReference.Handle("java/util/concurrent/ExecutorService");
    private static final ClassReference.Handle COMPLETABLE_FUTURE = new ClassReference.Handle("java/util/concurrent/CompletableFuture");
    private static final ClassReference.Handle COMPLETION_STAGE = new ClassReference.Handle("java/util/concurrent/CompletionStage");
    private static final ClassReference.Handle RUNNABLE = new ClassReference.Handle("java/lang/Runnable");
    private static final ClassReference.Handle CALLABLE = new ClassReference.Handle("java/util/concurrent/Callable");
    private static final ClassReference.Handle SUPPLIER = new ClassReference.Handle("java/util/function/Supplier");
    private static final ClassReference.Handle CONSUMER = new ClassReference.Handle("java/util/function/Consumer");
    private static final ClassReference.Handle FUNCTION = new ClassReference.Handle("java/util/function/Function");

    private static final String RUN = "run";
    private static final String RUN_DESC = "()V";
    private static final String RUN_DESC_OBJECT = "()Ljava/lang/Object;";
    private static final String CALL = "call";
    private static final String CALL_DESC = "()Ljava/lang/Object;";
    private static final String GET = "get";
    private static final String GET_DESC = "()Ljava/lang/Object;";
    private static final String ACCEPT = "accept";
    private static final String ACCEPT_DESC = "(Ljava/lang/Object;)V";
    private static final String APPLY = "apply";
    private static final String APPLY_DESC = "(Ljava/lang/Object;)Ljava/lang/Object;";

    private static final String OWNER_ACCESS_CONTROLLER = "java/security/AccessController";
    private static final String NAME_DO_PRIVILEGED = "doPrivileged";
    private static final String NAME_START = "start";
    private static final String DESC_START = "()V";

    private static final String NAME_EXECUTE = "execute";
    private static final String DESC_EXECUTE = "(Ljava/lang/Runnable;)V";
    private static final String NAME_SUBMIT = "submit";
    private static final String DESC_SUBMIT_RUNNABLE = "(Ljava/lang/Runnable;)Ljava/util/concurrent/Future;";
    private static final String DESC_SUBMIT_RUNNABLE_RESULT = "(Ljava/lang/Runnable;Ljava/lang/Object;)Ljava/util/concurrent/Future;";
    private static final String DESC_SUBMIT_CALLABLE = "(Ljava/util/concurrent/Callable;)Ljava/util/concurrent/Future;";

    private static final String NAME_RUN_ASYNC = "runAsync";
    private static final String NAME_SUPPLY_ASYNC = "supplyAsync";
    private static final String NAME_THEN_RUN = "thenRun";
    private static final String NAME_THEN_RUN_ASYNC = "thenRunAsync";
    private static final String NAME_THEN_ACCEPT = "thenAccept";
    private static final String NAME_THEN_ACCEPT_ASYNC = "thenAcceptAsync";
    private static final String NAME_THEN_APPLY = "thenApply";
    private static final String NAME_THEN_APPLY_ASYNC = "thenApplyAsync";

    private static final String DESC_RUN_ASYNC_1 =
            "(Ljava/lang/Runnable;)Ljava/util/concurrent/CompletableFuture;";
    private static final String DESC_RUN_ASYNC_2 =
            "(Ljava/lang/Runnable;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;";
    private static final String DESC_SUPPLY_ASYNC_1 =
            "(Ljava/util/function/Supplier;)Ljava/util/concurrent/CompletableFuture;";
    private static final String DESC_SUPPLY_ASYNC_2 =
            "(Ljava/util/function/Supplier;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;";
    private static final String DESC_THEN_RUN_STAGE =
            "(Ljava/lang/Runnable;)Ljava/util/concurrent/CompletionStage;";
    private static final String DESC_THEN_RUN_CF =
            "(Ljava/lang/Runnable;)Ljava/util/concurrent/CompletableFuture;";
    private static final String DESC_THEN_ACCEPT_STAGE =
            "(Ljava/util/function/Consumer;)Ljava/util/concurrent/CompletionStage;";
    private static final String DESC_THEN_ACCEPT_CF =
            "(Ljava/util/function/Consumer;)Ljava/util/concurrent/CompletableFuture;";
    private static final String DESC_THEN_APPLY_STAGE =
            "(Ljava/util/function/Function;)Ljava/util/concurrent/CompletionStage;";
    private static final String DESC_THEN_APPLY_CF =
            "(Ljava/util/function/Function;)Ljava/util/concurrent/CompletableFuture;";

    private PtaPluginBridge bridge;
    private BuildContext ctx;
    private InheritanceMap inheritance;

    private Map<Integer, List<MethodReference.Handle>> threadRunByJar = Collections.emptyMap();
    private List<MethodReference.Handle> threadRunAll = Collections.emptyList();

    private Map<Integer, List<MethodReference.Handle>> privilegedRunByJar = Collections.emptyMap();
    private List<MethodReference.Handle> privilegedRunAll = Collections.emptyList();

    private Map<Integer, List<MethodReference.Handle>> runnableByJar = Collections.emptyMap();
    private List<MethodReference.Handle> runnableAll = Collections.emptyList();
    private Map<Integer, List<MethodReference.Handle>> callableByJar = Collections.emptyMap();
    private List<MethodReference.Handle> callableAll = Collections.emptyList();

    private Map<Integer, List<MethodReference.Handle>> supplierByJar = Collections.emptyMap();
    private List<MethodReference.Handle> supplierAll = Collections.emptyList();
    private Map<Integer, List<MethodReference.Handle>> consumerByJar = Collections.emptyMap();
    private List<MethodReference.Handle> consumerAll = Collections.emptyList();
    private Map<Integer, List<MethodReference.Handle>> functionByJar = Collections.emptyMap();
    private List<MethodReference.Handle> functionAll = Collections.emptyList();

    @Override
    public void setBridge(PtaPluginBridge bridge) {
        this.bridge = bridge;
    }

    @Override
    public void onStart() {
        if (bridge == null) {
            return;
        }
        ctx = bridge.getBuildContext();
        inheritance = ctx == null ? null : ctx.inheritanceMap;
        if (ctx == null || ctx.methodMap == null || ctx.methodMap.isEmpty() || inheritance == null) {
            return;
        }
        threadRunByJar = collectTargetsByJar(THREAD, RUN, RUN_DESC, true);
        threadRunAll = flatten(threadRunByJar);

        privilegedRunByJar = collectPrivilegedRunsByJar();
        privilegedRunAll = flatten(privilegedRunByJar);

        runnableByJar = collectTargetsByJar(RUNNABLE, RUN, RUN_DESC, false);
        runnableAll = flatten(runnableByJar);
        callableByJar = collectTargetsByJar(CALLABLE, CALL, CALL_DESC, false);
        callableAll = flatten(callableByJar);

        supplierByJar = collectTargetsByJar(SUPPLIER, GET, GET_DESC, false);
        supplierAll = flatten(supplierByJar);
        consumerByJar = collectTargetsByJar(CONSUMER, ACCEPT, ACCEPT_DESC, false);
        consumerAll = flatten(consumerByJar);
        functionByJar = collectTargetsByJar(FUNCTION, APPLY, APPLY_DESC, false);
        functionAll = flatten(functionByJar);
    }

    @Override
    public void onNewCallEdge(PtaContextMethod callerContext,
                              MethodReference.Handle caller,
                              MethodReference.Handle callee,
                              String edgeType,
                              String confidence,
                              int opcode) {
        if (bridge == null || callerContext == null || caller == null || callee == null
                || callee.getClassReference() == null || callee.getClassReference().getName() == null) {
            return;
        }
        applyRules(callerContext, caller, callee);
    }

    @Override
    public void onNewContextMethod(PtaContextMethod method) {
        if (bridge == null || method == null || method.getMethod() == null
                || ctx == null || ctx.methodCalls == null || ctx.methodCalls.isEmpty()) {
            return;
        }
        MethodReference.Handle caller = method.getMethod();
        HashSet<MethodReference.Handle> callees = ctx.methodCalls.get(caller);
        if (callees == null || callees.isEmpty()) {
            return;
        }
        List<MethodReference.Handle> snapshot = new ArrayList<>(callees);
        for (MethodReference.Handle callee : snapshot) {
            if (callee == null) {
                continue;
            }
            applyRules(method, caller, callee);
        }
    }

    private void applyRules(PtaContextMethod callerContext,
                            MethodReference.Handle caller,
                            MethodReference.Handle callee) {
        if (isDoPrivileged(callee)) {
            addTargets(callerContext, caller, privilegedRunByJar, privilegedRunAll,
                    REASON_DO_PRIVILEGED, EDGE_OPCODE_INTERFACE);
        }
        if (isThreadStart(callee)) {
            addTargets(callerContext, caller, threadRunByJar, threadRunAll,
                    REASON_THREAD_START, EDGE_OPCODE_VIRTUAL);
        }
        if (isExecutorRunnableCallback(callee)) {
            addTargets(callerContext, caller, runnableByJar, runnableAll,
                    REASON_EXECUTOR_RUNNABLE, EDGE_OPCODE_INTERFACE);
        }
        if (isExecutorCallableCallback(callee)) {
            addTargets(callerContext, caller, callableByJar, callableAll,
                    REASON_EXECUTOR_CALLABLE, EDGE_OPCODE_INTERFACE);
        }
        if (isCompletableFutureRunnable(callee)) {
            addTargets(callerContext, caller, runnableByJar, runnableAll,
                    REASON_CF_RUNNABLE, EDGE_OPCODE_INTERFACE);
        }
        if (isCompletableFutureSupplier(callee)) {
            addTargets(callerContext, caller, supplierByJar, supplierAll,
                    REASON_CF_SUPPLIER, EDGE_OPCODE_INTERFACE);
        }
        if (isCompletableFutureConsumer(callee)) {
            addTargets(callerContext, caller, consumerByJar, consumerAll,
                    REASON_CF_CONSUMER, EDGE_OPCODE_INTERFACE);
        }
        if (isCompletableFutureFunction(callee)) {
            addTargets(callerContext, caller, functionByJar, functionAll,
                    REASON_CF_FUNCTION, EDGE_OPCODE_INTERFACE);
        }
    }

    private void addTargets(PtaContextMethod callerContext,
                            MethodReference.Handle caller,
                            Map<Integer, List<MethodReference.Handle>> byJar,
                            List<MethodReference.Handle> all,
                            String reason,
                            int opcode) {
        if (bridge == null || callerContext == null || caller == null) {
            return;
        }
        List<MethodReference.Handle> targets = null;
        Integer jarId = resolveJarId(caller);
        if (jarId != null && byJar != null) {
            targets = byJar.get(jarId);
        }
        if (targets == null || targets.isEmpty()) {
            targets = all;
        }
        if (targets == null || targets.isEmpty()) {
            return;
        }
        int limit = Math.min(MAX_TARGETS_PER_CALL, targets.size());
        for (int i = 0; i < limit; i++) {
            MethodReference.Handle target = targets.get(i);
            if (target == null) {
                continue;
            }
            bridge.addSemanticEdge(callerContext, target,
                    MethodCallMeta.TYPE_CALLBACK,
                    MethodCallMeta.CONF_LOW,
                    reason,
                    opcode,
                    null,
                    "sem:" + reason);
        }
    }

    private Integer resolveJarId(MethodReference.Handle method) {
        if (ctx == null || ctx.methodMap == null || method == null) {
            return null;
        }
        MethodReference ref = ctx.methodMap.get(method);
        return ref == null ? null : ref.getJarId();
    }

    private boolean isDoPrivileged(MethodReference.Handle callee) {
        if (callee == null || callee.getClassReference() == null || callee.getClassReference().getName() == null) {
            return false;
        }
        return OWNER_ACCESS_CONTROLLER.equals(callee.getClassReference().getName())
                && NAME_DO_PRIVILEGED.equals(callee.getName());
    }

    private boolean isThreadStart(MethodReference.Handle callee) {
        if (callee == null || callee.getClassReference() == null || callee.getClassReference().getName() == null) {
            return false;
        }
        if (!NAME_START.equals(callee.getName()) || !DESC_START.equals(callee.getDesc())) {
            return false;
        }
        ClassReference.Handle owner = callee.getClassReference();
        return isSubtypeOrSame(owner, THREAD);
    }

    private boolean isExecutorRunnableCallback(MethodReference.Handle callee) {
        if (!isSubtypeOrSame(ownerOf(callee), EXECUTOR) && !isSubtypeOrSame(ownerOf(callee), EXECUTOR_SERVICE)) {
            return false;
        }
        String name = callee.getName();
        String desc = callee.getDesc();
        if (NAME_EXECUTE.equals(name) && DESC_EXECUTE.equals(desc)) {
            return true;
        }
        return NAME_SUBMIT.equals(name) && (DESC_SUBMIT_RUNNABLE.equals(desc) || DESC_SUBMIT_RUNNABLE_RESULT.equals(desc));
    }

    private boolean isExecutorCallableCallback(MethodReference.Handle callee) {
        if (!isSubtypeOrSame(ownerOf(callee), EXECUTOR_SERVICE)) {
            return false;
        }
        return NAME_SUBMIT.equals(callee.getName()) && DESC_SUBMIT_CALLABLE.equals(callee.getDesc());
    }

    private boolean isCompletableFutureRunnable(MethodReference.Handle callee) {
        if (!isCfOrStage(callee)) {
            return false;
        }
        String name = callee.getName();
        String desc = callee.getDesc();
        if (NAME_RUN_ASYNC.equals(name) && (DESC_RUN_ASYNC_1.equals(desc) || DESC_RUN_ASYNC_2.equals(desc))) {
            return true;
        }
        return (NAME_THEN_RUN.equals(name) || NAME_THEN_RUN_ASYNC.equals(name))
                && (DESC_THEN_RUN_STAGE.equals(desc) || DESC_THEN_RUN_CF.equals(desc));
    }

    private boolean isCompletableFutureSupplier(MethodReference.Handle callee) {
        if (!isCfOrStage(callee)) {
            return false;
        }
        return NAME_SUPPLY_ASYNC.equals(callee.getName())
                && (DESC_SUPPLY_ASYNC_1.equals(callee.getDesc()) || DESC_SUPPLY_ASYNC_2.equals(callee.getDesc()));
    }

    private boolean isCompletableFutureConsumer(MethodReference.Handle callee) {
        if (!isCfOrStage(callee)) {
            return false;
        }
        String name = callee.getName();
        String desc = callee.getDesc();
        return (NAME_THEN_ACCEPT.equals(name) || NAME_THEN_ACCEPT_ASYNC.equals(name))
                && (DESC_THEN_ACCEPT_STAGE.equals(desc) || DESC_THEN_ACCEPT_CF.equals(desc));
    }

    private boolean isCompletableFutureFunction(MethodReference.Handle callee) {
        if (!isCfOrStage(callee)) {
            return false;
        }
        String name = callee.getName();
        String desc = callee.getDesc();
        return (NAME_THEN_APPLY.equals(name) || NAME_THEN_APPLY_ASYNC.equals(name))
                && (DESC_THEN_APPLY_STAGE.equals(desc) || DESC_THEN_APPLY_CF.equals(desc));
    }

    private boolean isCfOrStage(MethodReference.Handle callee) {
        ClassReference.Handle owner = ownerOf(callee);
        return isSubtypeOrSame(owner, COMPLETABLE_FUTURE)
                || isSubtypeOrSame(owner, COMPLETION_STAGE);
    }

    private ClassReference.Handle ownerOf(MethodReference.Handle method) {
        return method == null ? null : method.getClassReference();
    }

    private boolean isSubtypeOrSame(ClassReference.Handle type, ClassReference.Handle parent) {
        if (type == null || parent == null || type.getName() == null || parent.getName() == null) {
            return false;
        }
        if (type.getName().equals(parent.getName())) {
            return true;
        }
        return inheritance != null && inheritance.isSubclassOf(new ClassReference.Handle(type.getName()), parent);
    }

    private Map<Integer, List<MethodReference.Handle>> collectPrivilegedRunsByJar() {
        Map<Integer, List<MethodReference.Handle>> out = new HashMap<>();
        if (ctx == null || ctx.instantiatedClasses == null || ctx.instantiatedClasses.isEmpty()) {
            return out;
        }
        for (ClassReference.Handle clazz : ctx.instantiatedClasses) {
            if (clazz == null || clazz.getName() == null) {
                continue;
            }
            if (!isSubtypeOrSame(clazz, ACTION) && !isSubtypeOrSame(clazz, ACTION_EX)) {
                continue;
            }
            MethodReference.Handle run = new MethodReference.Handle(clazz, RUN, RUN_DESC_OBJECT);
            MethodReference ref = ctx.methodMap.get(run);
            if (ref == null || ref.getJarId() == null) {
                continue;
            }
            out.computeIfAbsent(ref.getJarId(), k -> new ArrayList<>()).add(run);
        }
        sortTargetMap(out);
        return out;
    }

    private Map<Integer, List<MethodReference.Handle>> collectTargetsByJar(ClassReference.Handle iface,
                                                                            String methodName,
                                                                            String methodDesc,
                                                                            boolean skipExactIface) {
        Map<Integer, List<MethodReference.Handle>> out = new HashMap<>();
        if (ctx == null || ctx.instantiatedClasses == null || ctx.instantiatedClasses.isEmpty()
                || ctx.methodMap == null || ctx.methodMap.isEmpty() || iface == null) {
            return out;
        }
        for (ClassReference.Handle clazz : ctx.instantiatedClasses) {
            if (clazz == null || clazz.getName() == null) {
                continue;
            }
            if (skipExactIface && iface.getName().equals(clazz.getName())) {
                continue;
            }
            if (!isSubtypeOrSame(clazz, iface)) {
                continue;
            }
            MethodReference.Handle target = new MethodReference.Handle(clazz, methodName, methodDesc);
            MethodReference ref = ctx.methodMap.get(target);
            if (ref == null || ref.getJarId() == null) {
                continue;
            }
            out.computeIfAbsent(ref.getJarId(), k -> new ArrayList<>()).add(target);
        }
        sortTargetMap(out);
        return out;
    }

    private static void sortTargetMap(Map<Integer, List<MethodReference.Handle>> map) {
        if (map == null || map.isEmpty()) {
            return;
        }
        Comparator<MethodReference.Handle> cmp = Comparator.comparing(PtaOnTheFlySemanticPlugin::ownerNameOrEmpty);
        for (List<MethodReference.Handle> list : map.values()) {
            if (list != null && list.size() > 1) {
                list.sort(cmp);
            }
        }
    }

    private static String ownerNameOrEmpty(MethodReference.Handle method) {
        if (method == null || method.getClassReference() == null || method.getClassReference().getName() == null) {
            return "";
        }
        return method.getClassReference().getName();
    }

    private static List<MethodReference.Handle> flatten(Map<Integer, List<MethodReference.Handle>> map) {
        if (map == null || map.isEmpty()) {
            return Collections.emptyList();
        }
        ArrayList<MethodReference.Handle> out = new ArrayList<>();
        for (List<MethodReference.Handle> list : map.values()) {
            if (list != null && !list.isEmpty()) {
                out.addAll(list);
            }
        }
        if (out.size() > 1) {
            out.sort(Comparator.comparing(PtaOnTheFlySemanticPlugin::ownerNameOrEmpty));
        }
        return out;
    }
}
