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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Move DynamicProxyEdgeRule to PTA event loop.
 */
final class PtaOnTheFlyDynamicProxyPlugin implements PtaPlugin {
    private static final int MAX_TARGETS_PER_CALL = 96;

    private static final String PROXY_OWNER = "java/lang/reflect/Proxy";
    private static final String PROXY_NEW = "newProxyInstance";
    private static final String PROXY_NEW_DESC =
            "(Ljava/lang/ClassLoader;[Ljava/lang/Class;Ljava/lang/reflect/InvocationHandler;)Ljava/lang/Object;";

    private static final String HANDLER_OWNER = "java/lang/reflect/InvocationHandler";
    private static final String HANDLER_INVOKE_NAME = "invoke";
    private static final String HANDLER_INVOKE_DESC =
            "(Ljava/lang/Object;Ljava/lang/reflect/Method;[Ljava/lang/Object;)Ljava/lang/Object;";

    private static final String SPRING_CGLIB_INTERCEPTOR = "org/springframework/cglib/proxy/MethodInterceptor";
    private static final String NETSF_CGLIB_INTERCEPTOR = "net/sf/cglib/proxy/MethodInterceptor";
    private static final String CGLIB_INTERCEPT_NAME = "intercept";
    private static final String SPRING_CGLIB_INTERCEPT_DESC =
            "(Ljava/lang/Object;Ljava/lang/reflect/Method;[Ljava/lang/Object;Lorg/springframework/cglib/proxy/MethodProxy;)Ljava/lang/Object;";
    private static final String NETSF_CGLIB_INTERCEPT_DESC =
            "(Ljava/lang/Object;Ljava/lang/reflect/Method;[Ljava/lang/Object;Lnet/sf/cglib/proxy/MethodProxy;)Ljava/lang/Object;";

    private static final String AOP_INTERCEPTOR = "org/aopalliance/intercept/MethodInterceptor";
    private static final String AOP_INVOKE_NAME = "invoke";
    private static final String AOP_INVOKE_DESC =
            "(Lorg/aopalliance/intercept/MethodInvocation;)Ljava/lang/Object;";

    private static final String SPRING_CGLIB_ENHANCER = "org/springframework/cglib/proxy/Enhancer";
    private static final String NETSF_CGLIB_ENHANCER = "net/sf/cglib/proxy/Enhancer";

    private static final String SPRING_PROXY_FACTORY = "org/springframework/aop/framework/ProxyFactory";
    private static final String SPRING_PROXY_FACTORY_BEAN = "org/springframework/aop/framework/ProxyFactoryBean";
    private static final String SPRING_AUTO_PROXY_CREATOR =
            "org/springframework/aop/framework/autoproxy/AbstractAutoProxyCreator";

    private static final String BYTE_BUDDY_PREFIX = "net/bytebuddy/";

    private static final String REASON_JDK = "dynamic_proxy_jdk";
    private static final String REASON_CGLIB = "dynamic_proxy_cglib";
    private static final String REASON_BYTE_BUDDY = "dynamic_proxy_bytebuddy";
    private static final String REASON_SPRING_AOP = "dynamic_proxy_spring_aop";

    private static final ClassReference.Handle HANDLER = new ClassReference.Handle(HANDLER_OWNER);
    private static final ClassReference.Handle SPRING_CGLIB = new ClassReference.Handle(SPRING_CGLIB_INTERCEPTOR);
    private static final ClassReference.Handle NETSF_CGLIB = new ClassReference.Handle(NETSF_CGLIB_INTERCEPTOR);
    private static final ClassReference.Handle AOP_MI = new ClassReference.Handle(AOP_INTERCEPTOR);

    private PtaPluginBridge bridge;
    private BuildContext ctx;
    private InheritanceMap inheritance;

    private Map<Integer, List<MethodReference.Handle>> jdkInvokeByJar = Collections.emptyMap();
    private List<MethodReference.Handle> allJdkInvokes = Collections.emptyList();

    private Map<Integer, List<MethodReference.Handle>> cglibByJar = Collections.emptyMap();
    private List<MethodReference.Handle> allCglibInterceptors = Collections.emptyList();

    private Map<Integer, List<MethodReference.Handle>> springAopByJar = Collections.emptyMap();
    private List<MethodReference.Handle> allSpringAopInterceptors = Collections.emptyList();

    private Map<Integer, List<MethodReference.Handle>> byteBuddyByJar = Collections.emptyMap();
    private List<MethodReference.Handle> allByteBuddyInterceptors = Collections.emptyList();

    private final Set<String> fired = new HashSet<>();

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
        if (ctx == null || ctx.methodMap == null || ctx.methodMap.isEmpty()) {
            return;
        }

        jdkInvokeByJar = collectJdkHandlerInvokeMethods();
        allJdkInvokes = flatten(jdkInvokeByJar);

        cglibByJar = collectCglibInterceptMethods();
        allCglibInterceptors = flatten(cglibByJar);

        springAopByJar = collectSpringAopInterceptorMethods();
        allSpringAopInterceptors = flatten(springAopByJar);

        byteBuddyByJar = collectByteBuddyInterceptorMethods();
        allByteBuddyInterceptors = flatten(byteBuddyByJar);
    }

    @Override
    public void onNewContextMethod(PtaContextMethod method) {
        if (method == null || method.getMethod() == null || ctx == null || ctx.methodCalls == null) {
            return;
        }
        HashSet<MethodReference.Handle> callees = ctx.methodCalls.get(method.getMethod());
        if (callees == null || callees.isEmpty()) {
            return;
        }
        List<MethodReference.Handle> snapshot = new ArrayList<>(callees);
        for (MethodReference.Handle callee : snapshot) {
            applyRules(method, callee);
        }
    }

    @Override
    public void onNewCallEdge(PtaContextMethod callerContext,
                              MethodReference.Handle caller,
                              MethodReference.Handle callee,
                              String edgeType,
                              String confidence,
                              int opcode) {
        if (callerContext == null || callee == null) {
            return;
        }
        applyRules(callerContext, callee);
    }

    private void applyRules(PtaContextMethod callerContext, MethodReference.Handle callee) {
        if (bridge == null || callerContext == null || callerContext.getMethod() == null || callee == null) {
            return;
        }
        if (isJdkProxyFactory(callee)) {
            addTargets(callerContext,
                    REASON_JDK,
                    Opcodes.INVOKEINTERFACE,
                    jdkInvokeByJar,
                    allJdkInvokes);
        }
        if (isCglibFactory(callee)) {
            addTargets(callerContext,
                    REASON_CGLIB,
                    Opcodes.INVOKEVIRTUAL,
                    cglibByJar,
                    allCglibInterceptors);
        }
        if (isSpringAopFactory(callee)) {
            addTargets(callerContext,
                    REASON_SPRING_AOP,
                    Opcodes.INVOKEINTERFACE,
                    springAopByJar,
                    allSpringAopInterceptors);
        }
        if (isByteBuddyFactory(callee)) {
            addTargets(callerContext,
                    REASON_BYTE_BUDDY,
                    Opcodes.INVOKEVIRTUAL,
                    byteBuddyByJar,
                    allByteBuddyInterceptors);
        }
    }

    private void addTargets(PtaContextMethod callerContext,
                            String reason,
                            int opcode,
                            Map<Integer, List<MethodReference.Handle>> byJar,
                            List<MethodReference.Handle> allTargets) {
        if (bridge == null || callerContext == null || callerContext.getMethod() == null || reason == null) {
            return;
        }
        String fireKey = callerContext.id() + "|" + reason;
        if (!fired.add(fireKey)) {
            return;
        }
        List<MethodReference.Handle> targets = selectScopedTargets(callerContext.getMethod(), byJar, allTargets);
        if (targets == null || targets.isEmpty()) {
            return;
        }
        int limit = Math.min(MAX_TARGETS_PER_CALL, targets.size());
        for (int i = 0; i < limit; i++) {
            MethodReference.Handle target = targets.get(i);
            if (target == null) {
                continue;
            }
            bridge.addSemanticEdge(callerContext,
                    target,
                    MethodCallMeta.TYPE_CALLBACK,
                    MethodCallMeta.CONF_MEDIUM,
                    reason,
                    opcode,
                    null,
                    "sem:" + reason);
        }
    }

    private List<MethodReference.Handle> selectScopedTargets(MethodReference.Handle caller,
                                                             Map<Integer, List<MethodReference.Handle>> byJar,
                                                             List<MethodReference.Handle> allTargets) {
        Integer jarId = resolveJarId(caller);
        if (jarId != null && byJar != null) {
            List<MethodReference.Handle> scoped = byJar.get(jarId);
            if (scoped != null && !scoped.isEmpty()) {
                return scoped;
            }
        }
        return allTargets;
    }

    private Integer resolveJarId(MethodReference.Handle method) {
        if (ctx == null || ctx.methodMap == null || method == null) {
            return null;
        }
        MethodReference ref = ctx.methodMap.get(method);
        return ref == null ? null : ref.getJarId();
    }

    private boolean isJdkProxyFactory(MethodReference.Handle callee) {
        if (callee == null || callee.getClassReference() == null) {
            return false;
        }
        return PROXY_OWNER.equals(callee.getClassReference().getName())
                && PROXY_NEW.equals(callee.getName())
                && PROXY_NEW_DESC.equals(callee.getDesc());
    }

    private boolean isCglibFactory(MethodReference.Handle callee) {
        if (callee == null || callee.getClassReference() == null || callee.getName() == null) {
            return false;
        }
        String owner = callee.getClassReference().getName();
        if (!SPRING_CGLIB_ENHANCER.equals(owner) && !NETSF_CGLIB_ENHANCER.equals(owner)) {
            return false;
        }
        return callee.getName().startsWith("create");
    }

    private boolean isSpringAopFactory(MethodReference.Handle callee) {
        if (callee == null || callee.getClassReference() == null || callee.getName() == null) {
            return false;
        }
        String owner = callee.getClassReference().getName();
        String name = callee.getName();
        if ((SPRING_PROXY_FACTORY.equals(owner) || SPRING_PROXY_FACTORY_BEAN.equals(owner))
                && ("getProxy".equals(name) || "getObject".equals(name))) {
            return true;
        }
        return SPRING_AUTO_PROXY_CREATOR.equals(owner)
                && ("wrapIfNecessary".equals(name) || "createProxy".equals(name));
    }

    private boolean isByteBuddyFactory(MethodReference.Handle callee) {
        if (callee == null || callee.getClassReference() == null || callee.getName() == null) {
            return false;
        }
        String owner = callee.getClassReference().getName();
        if (owner == null || !owner.startsWith(BYTE_BUDDY_PREFIX)) {
            return false;
        }
        String name = callee.getName();
        return "subclass".equals(name) || "method".equals(name)
                || "intercept".equals(name) || "make".equals(name)
                || "load".equals(name) || "build".equals(name);
    }

    private Map<Integer, List<MethodReference.Handle>> collectJdkHandlerInvokeMethods() {
        return collectByInterfaceAndSignature(
                Collections.singletonList(HANDLER),
                HANDLER_INVOKE_NAME,
                Collections.singleton(HANDLER_INVOKE_DESC)
        );
    }

    private Map<Integer, List<MethodReference.Handle>> collectCglibInterceptMethods() {
        List<ClassReference.Handle> interfaces = new ArrayList<>();
        interfaces.add(SPRING_CGLIB);
        interfaces.add(NETSF_CGLIB);
        Set<String> descs = new HashSet<>();
        descs.add(SPRING_CGLIB_INTERCEPT_DESC);
        descs.add(NETSF_CGLIB_INTERCEPT_DESC);
        return collectByInterfaceAndSignature(interfaces, CGLIB_INTERCEPT_NAME, descs);
    }

    private Map<Integer, List<MethodReference.Handle>> collectSpringAopInterceptorMethods() {
        return collectByInterfaceAndSignature(
                Collections.singletonList(AOP_MI),
                AOP_INVOKE_NAME,
                Collections.singleton(AOP_INVOKE_DESC)
        );
    }

    private Map<Integer, List<MethodReference.Handle>> collectByteBuddyInterceptorMethods() {
        Map<Integer, List<MethodReference.Handle>> out = new HashMap<>();
        if (ctx == null || ctx.methodMap == null || ctx.methodMap.isEmpty()) {
            return out;
        }
        for (Map.Entry<MethodReference.Handle, MethodReference> entry : ctx.methodMap.entrySet()) {
            MethodReference.Handle handle = entry.getKey();
            MethodReference ref = entry.getValue();
            if (handle == null || ref == null || handle.getName() == null || handle.getDesc() == null) {
                continue;
            }
            if (!"intercept".equals(handle.getName())) {
                continue;
            }
            String desc = handle.getDesc();
            if (!desc.contains("Ljava/lang/reflect/Method;") || !desc.contains("[Ljava/lang/Object;")) {
                continue;
            }
            Integer jarId = ref.getJarId();
            if (jarId == null) {
                continue;
            }
            out.computeIfAbsent(jarId, k -> new ArrayList<>()).add(new MethodReference.Handle(
                    new ClassReference.Handle(handle.getClassReference().getName(), jarId),
                    handle.getName(),
                    handle.getDesc()
            ));
        }
        sortAndDedup(out);
        return out;
    }

    private Map<Integer, List<MethodReference.Handle>> collectByInterfaceAndSignature(
            List<ClassReference.Handle> interfaces,
            String methodName,
            Set<String> methodDescs) {
        Map<Integer, List<MethodReference.Handle>> out = new HashMap<>();
        if (ctx == null || ctx.methodMap == null || ctx.methodMap.isEmpty()) {
            return out;
        }
        for (Map.Entry<MethodReference.Handle, MethodReference> entry : ctx.methodMap.entrySet()) {
            MethodReference.Handle handle = entry.getKey();
            MethodReference ref = entry.getValue();
            if (handle == null || ref == null || handle.getClassReference() == null) {
                continue;
            }
            if (!methodName.equals(handle.getName())) {
                continue;
            }
            if (methodDescs != null && !methodDescs.isEmpty() && !methodDescs.contains(handle.getDesc())) {
                continue;
            }
            if (!isAnyImplementer(handle.getClassReference(), interfaces)) {
                continue;
            }
            Integer jarId = ref.getJarId();
            if (jarId == null) {
                continue;
            }
            out.computeIfAbsent(jarId, k -> new ArrayList<>()).add(new MethodReference.Handle(
                    new ClassReference.Handle(handle.getClassReference().getName(), jarId),
                    handle.getName(),
                    handle.getDesc()
            ));
        }
        sortAndDedup(out);
        return out;
    }

    private boolean isAnyImplementer(ClassReference.Handle owner,
                                     List<ClassReference.Handle> targets) {
        if (owner == null || owner.getName() == null || targets == null || targets.isEmpty()) {
            return false;
        }
        for (ClassReference.Handle target : targets) {
            if (target == null || target.getName() == null) {
                continue;
            }
            if (owner.getName().equals(target.getName())) {
                return true;
            }
            if (inheritance != null && inheritance.isSubclassOf(owner, target)) {
                return true;
            }
            if (ctx != null && ctx.classMap != null) {
                ClassReference clazz = ctx.classMap.get(owner);
                if (clazz != null && clazz.getInterfaces() != null
                        && clazz.getInterfaces().contains(target.getName())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void sortAndDedup(Map<Integer, List<MethodReference.Handle>> byJar) {
        if (byJar == null || byJar.isEmpty()) {
            return;
        }
        for (Map.Entry<Integer, List<MethodReference.Handle>> entry : byJar.entrySet()) {
            List<MethodReference.Handle> list = entry.getValue();
            if (list == null || list.isEmpty()) {
                continue;
            }
            LinkedHashSet<MethodReference.Handle> set = new LinkedHashSet<>(list);
            ArrayList<MethodReference.Handle> sorted = new ArrayList<>(set);
            if (sorted.size() > 1) {
                sorted.sort((a, b) -> methodKey(a).compareTo(methodKey(b)));
            }
            entry.setValue(sorted);
        }
    }

    private static List<MethodReference.Handle> flatten(Map<Integer, List<MethodReference.Handle>> byJar) {
        if (byJar == null || byJar.isEmpty()) {
            return Collections.emptyList();
        }
        ArrayList<MethodReference.Handle> out = new ArrayList<>();
        for (List<MethodReference.Handle> list : byJar.values()) {
            if (list != null && !list.isEmpty()) {
                out.addAll(list);
            }
        }
        if (out.size() > 1) {
            out.sort((a, b) -> methodKey(a).compareTo(methodKey(b)));
        }
        return out;
    }

    private static String methodKey(MethodReference.Handle method) {
        if (method == null || method.getClassReference() == null) {
            return "";
        }
        return method.getClassReference().getName()
                + "#" + method.getName()
                + method.getDesc();
    }
}
