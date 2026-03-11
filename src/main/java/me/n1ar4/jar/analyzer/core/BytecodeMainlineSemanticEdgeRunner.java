/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */
package me.n1ar4.jar.analyzer.core;

import me.n1ar4.jar.analyzer.core.build.BuildContext;
import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.entity.CallSiteEntity;
import me.n1ar4.jar.analyzer.rules.MethodSemanticFlags;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class BytecodeMainlineSemanticEdgeRunner {
    private static final String THREAD_OWNER = "java/lang/Thread";
    private static final String EXECUTOR_OWNER = "java/util/concurrent/Executor";
    private static final String EXECUTOR_SERVICE_OWNER = "java/util/concurrent/ExecutorService";
    private static final String COMPLETABLE_FUTURE_OWNER = "java/util/concurrent/CompletableFuture";
    private static final String COMPLETION_STAGE_OWNER = "java/util/concurrent/CompletionStage";
    private static final String ACCESS_CONTROLLER_OWNER = "java/security/AccessController";
    private static final String PROXY_OWNER = "java/lang/reflect/Proxy";
    private static final String OBJECT_OWNER = "java/lang/Object";
    private static final String COMPARATOR_OWNER = "java/util/Comparator";
    private static final String COMPARABLE_OWNER = "java/lang/Comparable";
    private static final String COLLECTIONS_TRANSFORMER_OWNER = "org/apache/commons/collections/Transformer";
    private static final String COLLECTIONS4_TRANSFORMER_OWNER = "org/apache/commons/collections4/Transformer";
    private static final String SPRING_CGLIB_ENHANCER = "org/springframework/cglib/proxy/Enhancer";
    private static final String NETSF_CGLIB_ENHANCER = "net/sf/cglib/proxy/Enhancer";
    private static final String SPRING_PROXY_FACTORY = "org/springframework/aop/framework/ProxyFactory";
    private static final String SPRING_PROXY_FACTORY_BEAN = "org/springframework/aop/framework/ProxyFactoryBean";
    private static final String SPRING_AUTO_PROXY_CREATOR =
            "org/springframework/aop/framework/autoproxy/AbstractAutoProxyCreator";
    private static final String BYTE_BUDDY_PREFIX = "net/bytebuddy/";

    private static final ClassReference.Handle THREAD = new ClassReference.Handle(THREAD_OWNER);
    private static final ClassReference.Handle RUNNABLE = new ClassReference.Handle("java/lang/Runnable");
    private static final ClassReference.Handle CALLABLE = new ClassReference.Handle("java/util/concurrent/Callable");
    private static final ClassReference.Handle SUPPLIER = new ClassReference.Handle("java/util/function/Supplier");
    private static final ClassReference.Handle CONSUMER = new ClassReference.Handle("java/util/function/Consumer");
    private static final ClassReference.Handle FUNCTION = new ClassReference.Handle("java/util/function/Function");
    private static final ClassReference.Handle ACTION = new ClassReference.Handle("java/security/PrivilegedAction");
    private static final ClassReference.Handle ACTION_EX =
            new ClassReference.Handle("java/security/PrivilegedExceptionAction");
    private static final ClassReference.Handle INVOCATION_HANDLER =
            new ClassReference.Handle("java/lang/reflect/InvocationHandler");
    private static final ClassReference.Handle SPRING_CGLIB_INTERCEPTOR =
            new ClassReference.Handle("org/springframework/cglib/proxy/MethodInterceptor");
    private static final ClassReference.Handle NETSF_CGLIB_INTERCEPTOR =
            new ClassReference.Handle("net/sf/cglib/proxy/MethodInterceptor");
    private static final ClassReference.Handle AOP_METHOD_INTERCEPTOR =
            new ClassReference.Handle("org/aopalliance/intercept/MethodInterceptor");

    private static final Set<String> SPRING_DISPATCHER_OWNERS = Set.of(
            "org/springframework/web/servlet/DispatcherServlet",
            "org/springframework/web/servlet/mvc/method/annotation/RequestMappingHandlerAdapter",
            "org/springframework/web/method/support/InvocableHandlerMethod"
    );
    private static final Set<String> STRUTS_DISPATCHER_OWNERS = Set.of(
            "org/apache/struts/action/RequestProcessor",
            "org/apache/struts/chain/commands/servlet/ExecuteActionCommand"
    );
    private static final Set<String> NETTY_DISPATCHER_OWNERS = Set.of(
            "io/netty/channel/SimpleChannelInboundHandler",
            "io/netty/channel/ChannelInboundHandlerAdapter"
    );

    private static final int MAX_TARGETS_PER_CALL = 96;
    private static final int MAX_FRAMEWORK_CALLERS = 48;
    private static final int MAX_FRAMEWORK_TARGETS = 160;

    private BytecodeMainlineSemanticEdgeRunner() {
    }

    static Result appendEdges(BuildContext context,
                              InheritanceMap inheritanceMap,
                              Set<ClassReference.Handle> instantiatedClasses,
                              BytecodeMainlineCallGraphRunner.MethodLookup lookup) {
        if (context == null || context.methodMap == null || context.methodMap.isEmpty()
                || lookup == null || context.callSites == null || context.callSites.isEmpty()) {
            return Result.empty();
        }
        SemanticRuleContext ruleContext = new SemanticRuleContext(
                context,
                inheritanceMap,
                instantiatedClasses == null ? Set.of() : instantiatedClasses,
                lookup
        );
        EnumMap<EdgeBucket, Integer> edgeCounts = new EnumMap<>(EdgeBucket.class);
        for (CallSiteRuleGroup group : SemanticInvokeRuleRegistry.CALLSITE_RULE_GROUPS) {
            edgeCounts.put(group.bucket(), applyCallSiteRuleGroup(ruleContext, group));
        }
        for (FrameworkRule rule : SemanticInvokeRuleRegistry.FRAMEWORK_RULES) {
            edgeCounts.merge(EdgeBucket.FRAMEWORK, applyFrameworkRule(ruleContext, rule), Integer::sum);
        }
        int callbackEdges = count(edgeCounts, EdgeBucket.THREAD)
                + count(edgeCounts, EdgeBucket.EXECUTOR)
                + count(edgeCounts, EdgeBucket.COMPLETABLE_FUTURE)
                + count(edgeCounts, EdgeBucket.DO_PRIVILEGED)
                + count(edgeCounts, EdgeBucket.DYNAMIC_PROXY)
                + count(edgeCounts, EdgeBucket.GADGET_CALLBACK);
        return new Result(
                callbackEdges,
                count(edgeCounts, EdgeBucket.FRAMEWORK),
                count(edgeCounts, EdgeBucket.THREAD),
                count(edgeCounts, EdgeBucket.EXECUTOR),
                count(edgeCounts, EdgeBucket.COMPLETABLE_FUTURE),
                count(edgeCounts, EdgeBucket.DO_PRIVILEGED),
                count(edgeCounts, EdgeBucket.DYNAMIC_PROXY),
                count(edgeCounts, EdgeBucket.TRIGGER_BRIDGE)
        );
    }

    private static int count(EnumMap<EdgeBucket, Integer> counts, EdgeBucket bucket) {
        return counts.getOrDefault(bucket, 0);
    }

    private static int applyCallSiteRuleGroup(SemanticRuleContext context, CallSiteRuleGroup group) {
        if (group == null || group.bindings().isEmpty()) {
            return 0;
        }
        ArrayList<ResolvedBinding> activeBindings = new ArrayList<>();
        for (CallSiteRuleBinding binding : group.bindings()) {
            List<MethodReference.Handle> targets = context.targets(binding.targetKey());
            if (!targets.isEmpty()) {
                activeBindings.add(new ResolvedBinding(binding, targets));
            }
        }
        if (activeBindings.isEmpty()) {
            return 0;
        }
        int added = 0;
        for (CallSiteEntity site : context.buildContext().callSites) {
            if (site == null) {
                continue;
            }
            MethodReference.Handle caller = null;
            for (ResolvedBinding resolved : activeBindings) {
                CallSiteRuleBinding binding = resolved.binding();
                if (!binding.siteMatcher().matches(context, site)) {
                    continue;
                }
                if (caller == null) {
                    caller = resolveCaller(site, context.lookup());
                }
                if (caller == null || !binding.callerMatcher().matches(context, caller, site)) {
                    continue;
                }
                added += addTargets(
                        context.buildContext(),
                        caller,
                        resolved.targets(),
                        binding.type(),
                        binding.confidence(),
                        binding.reason(),
                        binding.opcode()
                );
            }
        }
        return added;
    }

    private static int applyFrameworkRule(SemanticRuleContext context, FrameworkRule rule) {
        if (rule == null) {
            return 0;
        }
        List<MethodReference.Handle> callers = collectFrameworkCallers(
                context.buildContext(),
                rule.owners(),
                rule.methodNames()
        );
        List<MethodReference.Handle> targets = collectSemanticTargets(
                context.buildContext(),
                rule.semanticFlag()
        );
        return addFrameworkEdges(context.buildContext(), callers, targets, rule.reason());
    }

    private static int addFrameworkEdges(BuildContext context,
                                         List<MethodReference.Handle> callers,
                                         List<MethodReference.Handle> targets,
                                         String reason) {
        if (callers.isEmpty() || targets.isEmpty()) {
            return 0;
        }
        int added = 0;
        int callerLimit = Math.min(MAX_FRAMEWORK_CALLERS, callers.size());
        int targetLimit = Math.min(MAX_FRAMEWORK_TARGETS, targets.size());
        for (int i = 0; i < callerLimit; i++) {
            MethodReference.Handle caller = callers.get(i);
            for (int j = 0; j < targetLimit; j++) {
                MethodReference.Handle target = targets.get(j);
                if (target == null) {
                    continue;
                }
                MethodReference.Handle callTarget = new MethodReference.Handle(
                        target.getClassReference(),
                        Opcodes.INVOKEVIRTUAL,
                        target.getName(),
                        target.getDesc()
                );
                HashSet<MethodReference.Handle> callees =
                        context.methodCalls.computeIfAbsent(caller, ignore -> new HashSet<>());
                boolean inserted = MethodCallUtils.addCallee(callees, callTarget);
                MethodCallMeta.record(context.methodCallMeta, MethodCallKey.of(caller, callTarget),
                        MethodCallMeta.TYPE_FRAMEWORK, MethodCallMeta.CONF_MEDIUM, reason, Opcodes.INVOKEVIRTUAL);
                if (inserted) {
                    added++;
                }
            }
        }
        return added;
    }

    private static MethodReference.Handle resolveCaller(CallSiteEntity site,
                                                        BytecodeMainlineCallGraphRunner.MethodLookup lookup) {
        if (site == null || lookup == null) {
            return null;
        }
        return lookup.resolve(
                site.getCallerClassName(),
                site.getCallerMethodName(),
                site.getCallerMethodDesc(),
                site.getJarId()
        );
    }

    private static List<MethodReference.Handle> collectSemanticTargets(BuildContext context, int semanticFlag) {
        return collectSemanticTargets(context, semanticFlag, null);
    }

    private static List<MethodReference.Handle> collectSemanticTargets(BuildContext context,
                                                                       int semanticFlag,
                                                                       Set<ClassReference.Handle> scope) {
        if (context == null || context.methodMap == null || context.methodMap.isEmpty() || semanticFlag <= 0) {
            return List.of();
        }
        Set<String> scopedClassNames = scopedClassNames(scope);
        LinkedHashSet<MethodReference.Handle> out = new LinkedHashSet<>();
        for (MethodReference method : context.methodMap.values()) {
            if (method == null || method.getClassReference() == null) {
                continue;
            }
            if ((method.getSemanticFlags() & semanticFlag) == 0) {
                continue;
            }
            if (!scopedClassNames.isEmpty()
                    && !scopedClassNames.contains(safe(method.getClassReference().getName()))) {
                continue;
            }
            out.add(method.getHandle());
        }
        return sortHandles(out);
    }

    private static Set<String> scopedClassNames(Set<ClassReference.Handle> scope) {
        if (scope == null || scope.isEmpty()) {
            return Set.of();
        }
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (ClassReference.Handle handle : scope) {
            if (handle == null || handle.getName() == null || handle.getName().isBlank()) {
                continue;
            }
            out.add(handle.getName().trim());
        }
        return out.isEmpty() ? Set.of() : Set.copyOf(out);
    }

    private static List<MethodReference.Handle> collectFrameworkCallers(BuildContext context,
                                                                        Set<String> owners,
                                                                        Set<String> methodNames) {
        if (context == null || context.methodMap == null || context.methodMap.isEmpty()
                || owners == null || owners.isEmpty() || methodNames == null || methodNames.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<MethodReference.Handle> out = new LinkedHashSet<>();
        for (MethodReference method : context.methodMap.values()) {
            if (method == null || method.getClassReference() == null) {
                continue;
            }
            if (!owners.contains(safe(method.getClassReference().getName()))) {
                continue;
            }
            if (!methodNames.contains(safe(method.getName()))) {
                continue;
            }
            out.add(method.getHandle());
        }
        return sortHandles(out);
    }

    private static boolean matchesThreadStartSite(SemanticRuleContext context, CallSiteEntity site) {
        return isThreadStartSite(site, context.buildContext().classMap, context.inheritanceMap());
    }

    private static boolean isThreadStartSite(CallSiteEntity site,
                                             Map<ClassReference.Handle, ClassReference> classMap,
                                             InheritanceMap inheritanceMap) {
        if (site == null || !"start".equals(safe(site.getCalleeMethodName()))
                || !"()V".equals(safe(site.getCalleeMethodDesc()))) {
            return false;
        }
        String owner = safe(site.getCalleeOwner());
        if (THREAD_OWNER.equals(owner)) {
            return true;
        }
        if (inheritanceMap == null) {
            return false;
        }
        ClassReference.Handle ownerHandle = inheritanceMap.resolveClass(owner, site.getJarId());
        return ownerHandle != null && matchesTypeHierarchy(ownerHandle, THREAD, classMap, inheritanceMap);
    }

    private static boolean isExecutorRunnableSite(CallSiteEntity site) {
        if (site == null) {
            return false;
        }
        String owner = safe(site.getCalleeOwner());
        String name = safe(site.getCalleeMethodName());
        String desc = safe(site.getCalleeMethodDesc());
        if (EXECUTOR_OWNER.equals(owner) && "execute".equals(name)
                && "(Ljava/lang/Runnable;)V".equals(desc)) {
            return true;
        }
        if (!EXECUTOR_SERVICE_OWNER.equals(owner)) {
            return false;
        }
        return ("execute".equals(name) && "(Ljava/lang/Runnable;)V".equals(desc))
                || ("submit".equals(name) && "(Ljava/lang/Runnable;)Ljava/util/concurrent/Future;".equals(desc))
                || ("submit".equals(name) && "(Ljava/lang/Runnable;Ljava/lang/Object;)Ljava/util/concurrent/Future;"
                .equals(desc));
    }

    private static boolean isExecutorCallableSite(CallSiteEntity site) {
        return site != null
                && EXECUTOR_SERVICE_OWNER.equals(safe(site.getCalleeOwner()))
                && "submit".equals(safe(site.getCalleeMethodName()))
                && "(Ljava/util/concurrent/Callable;)Ljava/util/concurrent/Future;".equals(safe(site.getCalleeMethodDesc()));
    }

    private static boolean matchesCompletableFutureRunnableSite(SemanticRuleContext context, CallSiteEntity site) {
        if (!isCompletableFutureSite(site)) {
            return false;
        }
        String name = safe(site.getCalleeMethodName());
        String desc = safe(site.getCalleeMethodDesc());
        return ("runAsync".equals(name) && (desc.equals("(Ljava/lang/Runnable;)Ljava/util/concurrent/CompletableFuture;")
                || desc.equals("(Ljava/lang/Runnable;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;")))
                || (("thenRun".equals(name) || "thenRunAsync".equals(name))
                && (desc.equals("(Ljava/lang/Runnable;)Ljava/util/concurrent/CompletionStage;")
                || desc.equals("(Ljava/lang/Runnable;)Ljava/util/concurrent/CompletableFuture;")));
    }

    private static boolean matchesCompletableFutureSupplierSite(SemanticRuleContext context, CallSiteEntity site) {
        if (!isCompletableFutureSite(site)) {
            return false;
        }
        return "supplyAsync".equals(safe(site.getCalleeMethodName()))
                && ("(Ljava/util/function/Supplier;)Ljava/util/concurrent/CompletableFuture;"
                .equals(safe(site.getCalleeMethodDesc()))
                || "(Ljava/util/function/Supplier;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;"
                .equals(safe(site.getCalleeMethodDesc())));
    }

    private static boolean matchesCompletableFutureConsumerSite(SemanticRuleContext context, CallSiteEntity site) {
        if (!isCompletableFutureSite(site)) {
            return false;
        }
        String name = safe(site.getCalleeMethodName());
        String desc = safe(site.getCalleeMethodDesc());
        return ("thenAccept".equals(name) || "thenAcceptAsync".equals(name))
                && ("(Ljava/util/function/Consumer;)Ljava/util/concurrent/CompletionStage;".equals(desc)
                || "(Ljava/util/function/Consumer;)Ljava/util/concurrent/CompletableFuture;".equals(desc));
    }

    private static boolean matchesCompletableFutureFunctionSite(SemanticRuleContext context, CallSiteEntity site) {
        if (!isCompletableFutureSite(site)) {
            return false;
        }
        String name = safe(site.getCalleeMethodName());
        String desc = safe(site.getCalleeMethodDesc());
        return ("thenApply".equals(name) || "thenApplyAsync".equals(name))
                && ("(Ljava/util/function/Function;)Ljava/util/concurrent/CompletionStage;".equals(desc)
                || "(Ljava/util/function/Function;)Ljava/util/concurrent/CompletableFuture;".equals(desc));
    }

    private static boolean isCompletableFutureSite(CallSiteEntity site) {
        if (site == null) {
            return false;
        }
        String owner = safe(site.getCalleeOwner());
        return COMPLETABLE_FUTURE_OWNER.equals(owner) || COMPLETION_STAGE_OWNER.equals(owner);
    }

    private static boolean matchesDoPrivilegedSite(SemanticRuleContext context, CallSiteEntity site) {
        return site != null
                && ACCESS_CONTROLLER_OWNER.equals(safe(site.getCalleeOwner()))
                && "doPrivileged".equals(safe(site.getCalleeMethodName()));
    }

    private static boolean isJdkProxyFactorySite(CallSiteEntity site) {
        return site != null
                && PROXY_OWNER.equals(safe(site.getCalleeOwner()))
                && "newProxyInstance".equals(safe(site.getCalleeMethodName()))
                && "(Ljava/lang/ClassLoader;[Ljava/lang/Class;Ljava/lang/reflect/InvocationHandler;)Ljava/lang/Object;"
                .equals(safe(site.getCalleeMethodDesc()));
    }

    private static boolean isCglibFactorySite(CallSiteEntity site) {
        if (site == null) {
            return false;
        }
        String owner = safe(site.getCalleeOwner());
        return (SPRING_CGLIB_ENHANCER.equals(owner) || NETSF_CGLIB_ENHANCER.equals(owner))
                && safe(site.getCalleeMethodName()).startsWith("create");
    }

    private static boolean isSpringAopFactorySite(CallSiteEntity site) {
        if (site == null) {
            return false;
        }
        String owner = safe(site.getCalleeOwner());
        String name = safe(site.getCalleeMethodName());
        if ((SPRING_PROXY_FACTORY.equals(owner) || SPRING_PROXY_FACTORY_BEAN.equals(owner))
                && ("getProxy".equals(name) || "getObject".equals(name))) {
            return true;
        }
        return SPRING_AUTO_PROXY_CREATOR.equals(owner)
                && ("wrapIfNecessary".equals(name) || "createProxy".equals(name));
    }

    private static boolean isByteBuddyFactorySite(CallSiteEntity site) {
        if (site == null) {
            return false;
        }
        String owner = safe(site.getCalleeOwner());
        if (!owner.startsWith(BYTE_BUDDY_PREFIX)) {
            return false;
        }
        String name = safe(site.getCalleeMethodName());
        return "subclass".equals(name) || "method".equals(name)
                || "intercept".equals(name) || "make".equals(name)
                || "load".equals(name) || "build".equals(name);
    }

    private static boolean matchesComparatorCallbackSite(SemanticRuleContext context, CallSiteEntity site) {
        return site != null
                && COMPARATOR_OWNER.equals(safe(site.getCalleeOwner()))
                && "compare".equals(safe(site.getCalleeMethodName()))
                && "(Ljava/lang/Object;Ljava/lang/Object;)I".equals(safe(site.getCalleeMethodDesc()));
    }

    private static boolean matchesComparableCallbackSite(SemanticRuleContext context, CallSiteEntity site) {
        return site != null
                && COMPARABLE_OWNER.equals(safe(site.getCalleeOwner()))
                && "compareTo".equals(safe(site.getCalleeMethodName()))
                && "(Ljava/lang/Object;)I".equals(safe(site.getCalleeMethodDesc()));
    }

    private static boolean matchesTransformerCallbackSite(SemanticRuleContext context, CallSiteEntity site) {
        if (site == null) {
            return false;
        }
        String owner = safe(site.getCalleeOwner());
        return (COLLECTIONS_TRANSFORMER_OWNER.equals(owner) || COLLECTIONS4_TRANSFORMER_OWNER.equals(owner))
                && "transform".equals(safe(site.getCalleeMethodName()))
                && "(Ljava/lang/Object;)Ljava/lang/Object;".equals(safe(site.getCalleeMethodDesc()));
    }

    private static boolean matchesToStringTriggerSite(SemanticRuleContext context, CallSiteEntity site) {
        return site != null
                && OBJECT_OWNER.equals(safe(site.getCalleeOwner()))
                && "toString".equals(safe(site.getCalleeMethodName()))
                && "()Ljava/lang/String;".equals(safe(site.getCalleeMethodDesc()));
    }

    private static boolean matchesHashCodeTriggerSite(SemanticRuleContext context, CallSiteEntity site) {
        return site != null
                && OBJECT_OWNER.equals(safe(site.getCalleeOwner()))
                && "hashCode".equals(safe(site.getCalleeMethodName()))
                && "()I".equals(safe(site.getCalleeMethodDesc()));
    }

    private static boolean matchesEqualsTriggerSite(SemanticRuleContext context, CallSiteEntity site) {
        return site != null
                && OBJECT_OWNER.equals(safe(site.getCalleeOwner()))
                && "equals".equals(safe(site.getCalleeMethodName()))
                && "(Ljava/lang/Object;)Z".equals(safe(site.getCalleeMethodDesc()));
    }

    private static boolean callerIsCollectionContainer(SemanticRuleContext context,
                                                       MethodReference.Handle caller,
                                                       CallSiteEntity site) {
        return callerHasSemanticFlag(context.buildContext(), caller, MethodSemanticFlags.COLLECTION_CONTAINER);
    }

    private static List<MethodReference.Handle> collectInterfaceTargets(BuildContext context,
                                                                        InheritanceMap inheritanceMap,
                                                                        Set<ClassReference.Handle> instantiatedClasses,
                                                                        ClassReference.Handle iface,
                                                                        String methodName,
                                                                        String methodDesc,
                                                                        boolean skipBaseType) {
        if (context == null || context.methodMap == null || context.methodMap.isEmpty()
                || inheritanceMap == null || instantiatedClasses == null || instantiatedClasses.isEmpty()
                || iface == null) {
            return List.of();
        }
        LinkedHashSet<MethodReference.Handle> out = new LinkedHashSet<>();
        for (ClassReference.Handle clazz : instantiatedClasses) {
            if (clazz == null) {
                continue;
            }
            if (skipBaseType && iface.equals(clazz)) {
                continue;
            }
            if (!matchesTypeHierarchy(clazz, iface, context.classMap, inheritanceMap)) {
                continue;
            }
            MethodReference.Handle target = resolveExistingHandle(context.methodMap, clazz, methodName, methodDesc);
            if (target != null) {
                out.add(target);
            }
        }
        return sortHandles(out);
    }

    private static List<MethodReference.Handle> collectByteBuddyTargets(BuildContext context) {
        if (context == null || context.methodMap == null || context.methodMap.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<MethodReference.Handle> out = new LinkedHashSet<>();
        for (MethodReference.Handle handle : context.methodMap.keySet()) {
            if (handle == null || handle.getClassReference() == null) {
                continue;
            }
            if (!"intercept".equals(safe(handle.getName()))) {
                continue;
            }
            String desc = safe(handle.getDesc());
            if (!desc.contains("Ljava/lang/reflect/Method;") || !desc.contains("[Ljava/lang/Object;")) {
                continue;
            }
            out.add(handle);
        }
        return sortHandles(out);
    }

    private static MethodReference.Handle resolveExistingHandle(Map<MethodReference.Handle, MethodReference> methodMap,
                                                                ClassReference.Handle owner,
                                                                String methodName,
                                                                String methodDesc) {
        if (methodMap == null || methodMap.isEmpty() || owner == null
                || methodName == null || methodName.isBlank()
                || methodDesc == null || methodDesc.isBlank()) {
            return null;
        }
        MethodReference.Handle exact = new MethodReference.Handle(owner, methodName, methodDesc);
        if (methodMap.containsKey(exact)) {
            return exact;
        }
        MethodReference.Handle loose = new MethodReference.Handle(
                new ClassReference.Handle(owner.getName(), -1),
                methodName,
                methodDesc
        );
        if (methodMap.containsKey(loose)) {
            return loose;
        }
        MethodReference.Handle found = null;
        for (MethodReference.Handle handle : methodMap.keySet()) {
            if (handle == null || handle.getClassReference() == null) {
                continue;
            }
            if (!safe(owner.getName()).equals(safe(handle.getClassReference().getName()))) {
                continue;
            }
            if (!safe(methodName).equals(safe(handle.getName())) || !safe(methodDesc).equals(safe(handle.getDesc()))) {
                continue;
            }
            if (found != null && !found.equals(handle)) {
                return null;
            }
            found = handle;
        }
        return found;
    }

    private static boolean matchesTypeHierarchy(ClassReference.Handle clazz,
                                                ClassReference.Handle expected,
                                                Map<ClassReference.Handle, ClassReference> classMap,
                                                InheritanceMap inheritanceMap) {
        if (clazz == null || expected == null) {
            return false;
        }
        if (clazz.equals(expected)) {
            return true;
        }
        if (inheritanceMap != null && inheritanceMap.isSubclassOf(clazz, expected)) {
            return true;
        }
        if (classMap == null || classMap.isEmpty()) {
            return false;
        }
        String expectedName = safe(expected.getName());
        if (expectedName.isEmpty()) {
            return false;
        }
        LinkedHashSet<ClassReference.Handle> queue = new LinkedHashSet<>();
        LinkedHashSet<String> visited = new LinkedHashSet<>();
        queue.add(clazz);
        while (!queue.isEmpty()) {
            ClassReference.Handle currentHandle = queue.iterator().next();
            queue.remove(currentHandle);
            if (currentHandle == null || currentHandle.getName() == null || !visited.add(currentHandle.getName())) {
                continue;
            }
            if (expectedName.equals(safe(currentHandle.getName()))) {
                return true;
            }
            ClassReference current = classMap.get(currentHandle);
            if (current == null) {
                current = resolveClassByName(classMap, currentHandle.getName());
            }
            if (current == null) {
                continue;
            }
            if (expectedName.equals(safe(current.getSuperClass()))) {
                return true;
            }
            String superClass = safe(current.getSuperClass());
            if (!superClass.isEmpty()) {
                ClassReference.Handle superHandle = inheritanceMap == null
                        ? resolveClassByNameHandle(classMap, superClass)
                        : inheritanceMap.resolveClass(superClass, currentHandle.getJarId());
                if (superHandle == null) {
                    superHandle = resolveClassByNameHandle(classMap, superClass);
                }
                if (superHandle != null) {
                    queue.add(superHandle);
                }
            }
            List<String> interfaces = current.getInterfaces();
            if (interfaces == null || interfaces.isEmpty()) {
                continue;
            }
            for (String ifaceName : interfaces) {
                if (expectedName.equals(safe(ifaceName))) {
                    return true;
                }
                ClassReference.Handle ifaceHandle = inheritanceMap == null
                        ? resolveClassByNameHandle(classMap, ifaceName)
                        : inheritanceMap.resolveClass(ifaceName, currentHandle.getJarId());
                if (ifaceHandle == null) {
                    ifaceHandle = resolveClassByNameHandle(classMap, ifaceName);
                }
                if (ifaceHandle != null) {
                    queue.add(ifaceHandle);
                }
            }
        }
        return false;
    }

    private static ClassReference resolveClassByName(Map<ClassReference.Handle, ClassReference> classMap,
                                                     String className) {
        ClassReference.Handle handle = resolveClassByNameHandle(classMap, className);
        return handle == null ? null : classMap.get(handle);
    }

    private static ClassReference.Handle resolveClassByNameHandle(Map<ClassReference.Handle, ClassReference> classMap,
                                                                  String className) {
        if (classMap == null || classMap.isEmpty()) {
            return null;
        }
        String expected = safe(className);
        if (expected.isEmpty()) {
            return null;
        }
        ClassReference.Handle found = null;
        for (ClassReference.Handle handle : classMap.keySet()) {
            if (handle == null || !expected.equals(safe(handle.getName()))) {
                continue;
            }
            if (found != null && !found.equals(handle)) {
                return null;
            }
            found = handle;
        }
        return found;
    }

    private static int addTargets(BuildContext context,
                                  MethodReference.Handle caller,
                                  Collection<MethodReference.Handle> targets,
                                  String type,
                                  String confidence,
                                  String reason,
                                  int opcode) {
        if (context == null || caller == null || targets == null || targets.isEmpty()) {
            return 0;
        }
        HashSet<MethodReference.Handle> callees =
                context.methodCalls.computeIfAbsent(caller, ignore -> new HashSet<>());
        int added = 0;
        int emitted = 0;
        for (MethodReference.Handle target : targets) {
            if (target == null || emitted >= MAX_TARGETS_PER_CALL) {
                continue;
            }
            MethodReference.Handle callTarget = new MethodReference.Handle(
                    target.getClassReference(),
                    opcode,
                    target.getName(),
                    target.getDesc()
            );
            boolean inserted = MethodCallUtils.addCallee(callees, callTarget);
            MethodCallMeta.record(context.methodCallMeta, MethodCallKey.of(caller, callTarget),
                    type, confidence, reason, opcode);
            if (inserted) {
                added++;
            }
            emitted++;
        }
        return added;
    }

    private static boolean callerHasSemanticFlag(BuildContext context,
                                                 MethodReference.Handle caller,
                                                 int semanticFlag) {
        if (context == null || context.methodMap == null || caller == null || semanticFlag <= 0) {
            return false;
        }
        MethodReference method = context.methodMap.get(caller);
        return method != null && (method.getSemanticFlags() & semanticFlag) != 0;
    }

    private static List<MethodReference.Handle> sortHandles(Collection<MethodReference.Handle> handles) {
        if (handles == null || handles.isEmpty()) {
            return List.of();
        }
        ArrayList<MethodReference.Handle> out = new ArrayList<>(handles);
        out.sort(Comparator.comparing((MethodReference.Handle handle) ->
                        handle == null || handle.getClassReference() == null ? "" : safe(handle.getClassReference().getName()))
                .thenComparing(handle -> handle == null ? "" : safe(handle.getName()))
                .thenComparing(handle -> handle == null ? "" : safe(handle.getDesc()))
                .thenComparingInt(handle -> handle == null || handle.getJarId() == null ? -1 : handle.getJarId()));
        return Collections.unmodifiableList(out);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private enum EdgeBucket {
        THREAD,
        EXECUTOR,
        COMPLETABLE_FUTURE,
        DO_PRIVILEGED,
        DYNAMIC_PROXY,
        GADGET_CALLBACK,
        TRIGGER_BRIDGE,
        FRAMEWORK
    }

    private enum TargetKey {
        THREAD_RUN,
        RUNNABLE_RUN,
        CALLABLE_CALL,
        SUPPLIER_GET,
        CONSUMER_ACCEPT,
        FUNCTION_APPLY,
        ACTION_RUN,
        JDK_PROXY_INVOKE,
        CGLIB_INTERCEPT,
        SPRING_AOP_INVOKE,
        BYTE_BUDDY_INTERCEPT,
        COMPARATOR_CALLBACK,
        COMPARABLE_CALLBACK,
        TRANSFORMER_CALLBACK,
        TOSTRING_TRIGGER,
        HASHCODE_TRIGGER,
        EQUALS_TRIGGER
    }

    @FunctionalInterface
    private interface SiteMatcher {
        boolean matches(SemanticRuleContext context, CallSiteEntity site);
    }

    @FunctionalInterface
    private interface CallerMatcher {
        CallerMatcher ALWAYS = (context, caller, site) -> true;

        boolean matches(SemanticRuleContext context, MethodReference.Handle caller, CallSiteEntity site);
    }

    private record CallSiteRuleBinding(TargetKey targetKey,
                                       String type,
                                       String confidence,
                                       String reason,
                                       int opcode,
                                       SiteMatcher siteMatcher,
                                       CallerMatcher callerMatcher) {
    }

    private record CallSiteRuleGroup(EdgeBucket bucket, List<CallSiteRuleBinding> bindings) {
    }

    private record FrameworkRule(Set<String> owners,
                                 Set<String> methodNames,
                                 int semanticFlag,
                                 String reason) {
    }

    private record ResolvedBinding(CallSiteRuleBinding binding, List<MethodReference.Handle> targets) {
    }

    private static final class SemanticRuleContext {
        private final BuildContext buildContext;
        private final InheritanceMap inheritanceMap;
        private final Set<ClassReference.Handle> instantiatedClasses;
        private final BytecodeMainlineCallGraphRunner.MethodLookup lookup;
        private final EnumMap<TargetKey, List<MethodReference.Handle>> targetCache =
                new EnumMap<>(TargetKey.class);

        private SemanticRuleContext(BuildContext buildContext,
                                    InheritanceMap inheritanceMap,
                                    Set<ClassReference.Handle> instantiatedClasses,
                                    BytecodeMainlineCallGraphRunner.MethodLookup lookup) {
            this.buildContext = buildContext;
            this.inheritanceMap = inheritanceMap;
            this.instantiatedClasses = instantiatedClasses == null ? Set.of() : instantiatedClasses;
            this.lookup = lookup;
        }

        private BuildContext buildContext() {
            return buildContext;
        }

        private InheritanceMap inheritanceMap() {
            return inheritanceMap;
        }

        private BytecodeMainlineCallGraphRunner.MethodLookup lookup() {
            return lookup;
        }

        private List<MethodReference.Handle> targets(TargetKey key) {
            return targetCache.computeIfAbsent(key, this::computeTargets);
        }

        private List<MethodReference.Handle> computeTargets(TargetKey key) {
            if (key == null) {
                return List.of();
            }
            return switch (key) {
                case THREAD_RUN -> collectInterfaceTargets(
                        buildContext, inheritanceMap, instantiatedClasses, THREAD, "run", "()V", true
                );
                case RUNNABLE_RUN -> collectInterfaceTargets(
                        buildContext, inheritanceMap, instantiatedClasses, RUNNABLE, "run", "()V", false
                );
                case CALLABLE_CALL -> collectInterfaceTargets(
                        buildContext, inheritanceMap, instantiatedClasses, CALLABLE, "call",
                        "()Ljava/lang/Object;", false
                );
                case SUPPLIER_GET -> collectInterfaceTargets(
                        buildContext, inheritanceMap, instantiatedClasses, SUPPLIER, "get",
                        "()Ljava/lang/Object;", false
                );
                case CONSUMER_ACCEPT -> collectInterfaceTargets(
                        buildContext, inheritanceMap, instantiatedClasses, CONSUMER, "accept",
                        "(Ljava/lang/Object;)V", false
                );
                case FUNCTION_APPLY -> collectInterfaceTargets(
                        buildContext, inheritanceMap, instantiatedClasses, FUNCTION, "apply",
                        "(Ljava/lang/Object;)Ljava/lang/Object;", false
                );
                case ACTION_RUN -> {
                    ArrayList<MethodReference.Handle> targets = new ArrayList<>();
                    targets.addAll(collectInterfaceTargets(
                            buildContext, inheritanceMap, instantiatedClasses, ACTION, "run",
                            "()Ljava/lang/Object;", false
                    ));
                    targets.addAll(collectInterfaceTargets(
                            buildContext, inheritanceMap, instantiatedClasses, ACTION_EX, "run",
                            "()Ljava/lang/Object;", false
                    ));
                    yield sortHandles(targets);
                }
                case JDK_PROXY_INVOKE -> collectInterfaceTargets(
                        buildContext, inheritanceMap, instantiatedClasses, INVOCATION_HANDLER, "invoke",
                        "(Ljava/lang/Object;Ljava/lang/reflect/Method;[Ljava/lang/Object;)Ljava/lang/Object;", false
                );
                case CGLIB_INTERCEPT -> {
                    ArrayList<MethodReference.Handle> targets = new ArrayList<>();
                    targets.addAll(collectInterfaceTargets(
                            buildContext, inheritanceMap, instantiatedClasses, SPRING_CGLIB_INTERCEPTOR, "intercept",
                            "(Ljava/lang/Object;Ljava/lang/reflect/Method;[Ljava/lang/Object;Lorg/springframework/cglib/proxy/MethodProxy;)Ljava/lang/Object;",
                            false
                    ));
                    targets.addAll(collectInterfaceTargets(
                            buildContext, inheritanceMap, instantiatedClasses, NETSF_CGLIB_INTERCEPTOR, "intercept",
                            "(Ljava/lang/Object;Ljava/lang/reflect/Method;[Ljava/lang/Object;Lnet/sf/cglib/proxy/MethodProxy;)Ljava/lang/Object;",
                            false
                    ));
                    yield sortHandles(targets);
                }
                case SPRING_AOP_INVOKE -> collectInterfaceTargets(
                        buildContext, inheritanceMap, instantiatedClasses, AOP_METHOD_INTERCEPTOR, "invoke",
                        "(Lorg/aopalliance/intercept/MethodInvocation;)Ljava/lang/Object;", false
                );
                case BYTE_BUDDY_INTERCEPT -> collectByteBuddyTargets(buildContext);
                case COMPARATOR_CALLBACK -> collectSemanticTargets(
                        buildContext, MethodSemanticFlags.COMPARATOR_CALLBACK
                );
                case COMPARABLE_CALLBACK -> collectSemanticTargets(
                        buildContext, MethodSemanticFlags.COMPARABLE_CALLBACK
                );
                case TRANSFORMER_CALLBACK -> collectSemanticTargets(
                        buildContext, MethodSemanticFlags.TRANSFORMER_CALLBACK
                );
                case TOSTRING_TRIGGER -> collectSemanticTargets(
                        buildContext, MethodSemanticFlags.TOSTRING_TRIGGER, instantiatedClasses
                );
                case HASHCODE_TRIGGER -> collectSemanticTargets(
                        buildContext, MethodSemanticFlags.HASHCODE_TRIGGER, instantiatedClasses
                );
                case EQUALS_TRIGGER -> collectSemanticTargets(
                        buildContext, MethodSemanticFlags.EQUALS_TRIGGER, instantiatedClasses
                );
            };
        }
    }

    private static final class SemanticInvokeRuleRegistry {
        private static final List<CallSiteRuleGroup> CALLSITE_RULE_GROUPS = List.of(
                new CallSiteRuleGroup(EdgeBucket.THREAD, List.of(
                        binding(TargetKey.THREAD_RUN, MethodCallMeta.TYPE_CALLBACK, MethodCallMeta.CONF_LOW,
                                "thread_start", Opcodes.INVOKEVIRTUAL,
                                BytecodeMainlineSemanticEdgeRunner::matchesThreadStartSite)
                )),
                new CallSiteRuleGroup(EdgeBucket.EXECUTOR, List.of(
                        binding(TargetKey.RUNNABLE_RUN, MethodCallMeta.TYPE_CALLBACK, MethodCallMeta.CONF_LOW,
                                "executor_runnable", Opcodes.INVOKEINTERFACE,
                                (context, site) -> isExecutorRunnableSite(site)),
                        binding(TargetKey.CALLABLE_CALL, MethodCallMeta.TYPE_CALLBACK, MethodCallMeta.CONF_LOW,
                                "executor_callable", Opcodes.INVOKEINTERFACE,
                                (context, site) -> isExecutorCallableSite(site))
                )),
                new CallSiteRuleGroup(EdgeBucket.COMPLETABLE_FUTURE, List.of(
                        binding(TargetKey.RUNNABLE_RUN, MethodCallMeta.TYPE_CALLBACK, MethodCallMeta.CONF_LOW,
                                "completable_future_runnable", Opcodes.INVOKEINTERFACE,
                                BytecodeMainlineSemanticEdgeRunner::matchesCompletableFutureRunnableSite),
                        binding(TargetKey.SUPPLIER_GET, MethodCallMeta.TYPE_CALLBACK, MethodCallMeta.CONF_LOW,
                                "completable_future_supplier", Opcodes.INVOKEINTERFACE,
                                BytecodeMainlineSemanticEdgeRunner::matchesCompletableFutureSupplierSite),
                        binding(TargetKey.CONSUMER_ACCEPT, MethodCallMeta.TYPE_CALLBACK, MethodCallMeta.CONF_LOW,
                                "completable_future_consumer", Opcodes.INVOKEINTERFACE,
                                BytecodeMainlineSemanticEdgeRunner::matchesCompletableFutureConsumerSite),
                        binding(TargetKey.FUNCTION_APPLY, MethodCallMeta.TYPE_CALLBACK, MethodCallMeta.CONF_LOW,
                                "completable_future_function", Opcodes.INVOKEINTERFACE,
                                BytecodeMainlineSemanticEdgeRunner::matchesCompletableFutureFunctionSite)
                )),
                new CallSiteRuleGroup(EdgeBucket.DO_PRIVILEGED, List.of(
                        binding(TargetKey.ACTION_RUN, MethodCallMeta.TYPE_CALLBACK, MethodCallMeta.CONF_LOW,
                                "do_privileged", Opcodes.INVOKEINTERFACE,
                                BytecodeMainlineSemanticEdgeRunner::matchesDoPrivilegedSite)
                )),
                new CallSiteRuleGroup(EdgeBucket.DYNAMIC_PROXY, List.of(
                        binding(TargetKey.JDK_PROXY_INVOKE, MethodCallMeta.TYPE_CALLBACK, MethodCallMeta.CONF_MEDIUM,
                                "dynamic_proxy_jdk", Opcodes.INVOKEINTERFACE,
                                (context, site) -> isJdkProxyFactorySite(site)),
                        binding(TargetKey.CGLIB_INTERCEPT, MethodCallMeta.TYPE_CALLBACK, MethodCallMeta.CONF_MEDIUM,
                                "dynamic_proxy_cglib", Opcodes.INVOKEVIRTUAL,
                                (context, site) -> isCglibFactorySite(site)),
                        binding(TargetKey.SPRING_AOP_INVOKE, MethodCallMeta.TYPE_CALLBACK, MethodCallMeta.CONF_MEDIUM,
                                "dynamic_proxy_spring_aop", Opcodes.INVOKEINTERFACE,
                                (context, site) -> isSpringAopFactorySite(site)),
                        binding(TargetKey.BYTE_BUDDY_INTERCEPT, MethodCallMeta.TYPE_CALLBACK, MethodCallMeta.CONF_MEDIUM,
                                "dynamic_proxy_bytebuddy", Opcodes.INVOKEVIRTUAL,
                                (context, site) -> isByteBuddyFactorySite(site))
                )),
                new CallSiteRuleGroup(EdgeBucket.GADGET_CALLBACK, List.of(
                        binding(TargetKey.COMPARATOR_CALLBACK, MethodCallMeta.TYPE_CALLBACK, MethodCallMeta.CONF_HIGH,
                                "semantic_comparator_callback", Opcodes.INVOKEINTERFACE,
                                BytecodeMainlineSemanticEdgeRunner::matchesComparatorCallbackSite),
                        binding(TargetKey.COMPARABLE_CALLBACK, MethodCallMeta.TYPE_CALLBACK, MethodCallMeta.CONF_HIGH,
                                "semantic_comparable_callback", Opcodes.INVOKEINTERFACE,
                                BytecodeMainlineSemanticEdgeRunner::matchesComparableCallbackSite),
                        binding(TargetKey.TRANSFORMER_CALLBACK, MethodCallMeta.TYPE_CALLBACK, MethodCallMeta.CONF_HIGH,
                                "semantic_transformer_callback", Opcodes.INVOKEINTERFACE,
                                BytecodeMainlineSemanticEdgeRunner::matchesTransformerCallbackSite)
                )),
                new CallSiteRuleGroup(EdgeBucket.TRIGGER_BRIDGE, List.of(
                        binding(TargetKey.TOSTRING_TRIGGER, MethodCallMeta.TYPE_FRAMEWORK, MethodCallMeta.CONF_HIGH,
                                "semantic_tostring_trigger_bridge", Opcodes.INVOKEVIRTUAL,
                                BytecodeMainlineSemanticEdgeRunner::matchesToStringTriggerSite,
                                BytecodeMainlineSemanticEdgeRunner::callerIsCollectionContainer),
                        binding(TargetKey.HASHCODE_TRIGGER, MethodCallMeta.TYPE_FRAMEWORK, MethodCallMeta.CONF_HIGH,
                                "semantic_hashcode_trigger_bridge", Opcodes.INVOKEVIRTUAL,
                                BytecodeMainlineSemanticEdgeRunner::matchesHashCodeTriggerSite,
                                BytecodeMainlineSemanticEdgeRunner::callerIsCollectionContainer),
                        binding(TargetKey.EQUALS_TRIGGER, MethodCallMeta.TYPE_FRAMEWORK, MethodCallMeta.CONF_HIGH,
                                "semantic_equals_trigger_bridge", Opcodes.INVOKEVIRTUAL,
                                BytecodeMainlineSemanticEdgeRunner::matchesEqualsTriggerSite,
                                BytecodeMainlineSemanticEdgeRunner::callerIsCollectionContainer)
                ))
        );

        private static final List<FrameworkRule> FRAMEWORK_RULES = List.of(
                new FrameworkRule(
                        SPRING_DISPATCHER_OWNERS,
                        Set.of("doDispatch", "doService", "service", "invokeHandlerMethod", "invokeForRequest", "doInvoke"),
                        MethodSemanticFlags.SPRING_ENDPOINT,
                        "spring_dispatch"
                ),
                new FrameworkRule(
                        STRUTS_DISPATCHER_OWNERS,
                        Set.of("process", "processActionPerform", "execute"),
                        MethodSemanticFlags.STRUTS_ACTION,
                        "struts_dispatch"
                ),
                new FrameworkRule(
                        NETTY_DISPATCHER_OWNERS,
                        Set.of("channelRead", "invokeChannelRead"),
                        MethodSemanticFlags.NETTY_HANDLER,
                        "netty_dispatch"
                )
        );

        private static CallSiteRuleBinding binding(TargetKey targetKey,
                                                   String type,
                                                   String confidence,
                                                   String reason,
                                                   int opcode,
                                                   SiteMatcher siteMatcher) {
            return binding(targetKey, type, confidence, reason, opcode, siteMatcher, CallerMatcher.ALWAYS);
        }

        private static CallSiteRuleBinding binding(TargetKey targetKey,
                                                   String type,
                                                   String confidence,
                                                   String reason,
                                                   int opcode,
                                                   SiteMatcher siteMatcher,
                                                   CallerMatcher callerMatcher) {
            return new CallSiteRuleBinding(targetKey, type, confidence, reason, opcode, siteMatcher, callerMatcher);
        }
    }

    record Result(int callbackEdges,
                  int frameworkEdges,
                  int threadStartEdges,
                  int executorEdges,
                  int completableFutureEdges,
                  int doPrivilegedEdges,
                  int dynamicProxyEdges,
                  int triggerBridgeEdges) {
        static Result empty() {
            return new Result(0, 0, 0, 0, 0, 0, 0, 0);
        }
    }
}
