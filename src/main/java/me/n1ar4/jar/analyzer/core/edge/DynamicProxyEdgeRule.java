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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class DynamicProxyEdgeRule implements EdgeInferRule {
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

    private static final int MAX_TARGETS_PER_CALL = 96;

    private static final ClassReference.Handle HANDLER = new ClassReference.Handle(HANDLER_OWNER);
    private static final ClassReference.Handle SPRING_CGLIB = new ClassReference.Handle(SPRING_CGLIB_INTERCEPTOR);
    private static final ClassReference.Handle NETSF_CGLIB = new ClassReference.Handle(NETSF_CGLIB_INTERCEPTOR);
    private static final ClassReference.Handle AOP_MI = new ClassReference.Handle(AOP_INTERCEPTOR);

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

        Map<Integer, List<MethodReference.Handle>> jdkInvokeByJar = collectJdkHandlerInvokeMethods(ctx);
        Map<Integer, List<MethodReference.Handle>> cglibByJar = collectCglibInterceptMethods(ctx);
        Map<Integer, List<MethodReference.Handle>> springAopByJar = collectSpringAopInterceptorMethods(ctx);
        Map<Integer, List<MethodReference.Handle>> byteBuddyByJar = collectByteBuddyInterceptorMethods(ctx);

        List<MethodReference.Handle> allJdk = flatten(jdkInvokeByJar);
        List<MethodReference.Handle> allCglib = flatten(cglibByJar);
        List<MethodReference.Handle> allSpringAop = flatten(springAopByJar);
        List<MethodReference.Handle> allByteBuddy = flatten(byteBuddyByJar);

        if (allJdk.isEmpty() && allCglib.isEmpty() && allSpringAop.isEmpty() && allByteBuddy.isEmpty()) {
            return 0;
        }

        int added = 0;
        for (Map.Entry<MethodReference.Handle, HashSet<MethodReference.Handle>> entry : ctx.methodCalls.entrySet()) {
            MethodReference.Handle caller = entry.getKey();
            HashSet<MethodReference.Handle> callees = entry.getValue();
            if (caller == null || callees == null || callees.isEmpty()) {
                continue;
            }
            boolean needJdk = callsJdkProxyFactory(callees);
            boolean needCglib = callsCglibFactory(callees);
            boolean needByteBuddy = callsByteBuddyFactory(callees);
            boolean needSpringAop = callsSpringAopFactory(callees);
            if (!needJdk && !needCglib && !needByteBuddy && !needSpringAop) {
                continue;
            }

            Integer jarId = resolveJarId(ctx, caller);

            if (needJdk && !allJdk.isEmpty()) {
                List<MethodReference.Handle> targets = selectScoped(jarId, jdkInvokeByJar, allJdk);
                added += addTargets(ctx, caller, callees, targets, REASON_JDK, Opcodes.INVOKEINTERFACE);
            }
            if (needCglib && !allCglib.isEmpty()) {
                List<MethodReference.Handle> targets = selectScoped(jarId, cglibByJar, allCglib);
                added += addTargets(ctx, caller, callees, targets, REASON_CGLIB, Opcodes.INVOKEVIRTUAL);
            }
            if (needByteBuddy && !allByteBuddy.isEmpty()) {
                List<MethodReference.Handle> targets = selectScoped(jarId, byteBuddyByJar, allByteBuddy);
                added += addTargets(ctx, caller, callees, targets, REASON_BYTE_BUDDY, Opcodes.INVOKEVIRTUAL);
            }
            if (needSpringAop && !allSpringAop.isEmpty()) {
                List<MethodReference.Handle> targets = selectScoped(jarId, springAopByJar, allSpringAop);
                added += addTargets(ctx, caller, callees, targets, REASON_SPRING_AOP, Opcodes.INVOKEINTERFACE);
            }
        }
        return added;
    }

    private static int addTargets(BuildContext ctx,
                                  MethodReference.Handle caller,
                                  HashSet<MethodReference.Handle> callees,
                                  List<MethodReference.Handle> targets,
                                  String reason,
                                  int opcode) {
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
            MethodReference.Handle callTarget = new MethodReference.Handle(
                    target.getClassReference(),
                    opcode,
                    target.getName(),
                    target.getDesc());
            if (MethodCallUtils.addCallee(callees, callTarget)) {
                added++;
            }
            MethodCallMeta.record(ctx.methodCallMeta, MethodCallKey.of(caller, callTarget),
                    MethodCallMeta.TYPE_CALLBACK, MethodCallMeta.CONF_MEDIUM, reason, opcode);
        }
        return added;
    }

    private static List<MethodReference.Handle> selectScoped(Integer jarId,
                                                             Map<Integer, List<MethodReference.Handle>> byJar,
                                                             List<MethodReference.Handle> fallback) {
        if (jarId != null && byJar != null) {
            List<MethodReference.Handle> scoped = byJar.get(jarId);
            if (scoped != null && !scoped.isEmpty()) {
                return scoped;
            }
        }
        return fallback;
    }

    private static boolean callsJdkProxyFactory(Set<MethodReference.Handle> callees) {
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

    private static boolean callsCglibFactory(Set<MethodReference.Handle> callees) {
        if (callees == null || callees.isEmpty()) {
            return false;
        }
        for (MethodReference.Handle callee : callees) {
            if (callee == null || callee.getClassReference() == null || callee.getName() == null) {
                continue;
            }
            String owner = callee.getClassReference().getName();
            if (!SPRING_CGLIB_ENHANCER.equals(owner) && !NETSF_CGLIB_ENHANCER.equals(owner)) {
                continue;
            }
            if (callee.getName().startsWith("create")) {
                return true;
            }
        }
        return false;
    }

    private static boolean callsSpringAopFactory(Set<MethodReference.Handle> callees) {
        if (callees == null || callees.isEmpty()) {
            return false;
        }
        for (MethodReference.Handle callee : callees) {
            if (callee == null || callee.getClassReference() == null || callee.getName() == null) {
                continue;
            }
            String owner = callee.getClassReference().getName();
            String name = callee.getName();
            if ((SPRING_PROXY_FACTORY.equals(owner) || SPRING_PROXY_FACTORY_BEAN.equals(owner))
                    && ("getProxy".equals(name) || "getObject".equals(name))) {
                return true;
            }
            if (SPRING_AUTO_PROXY_CREATOR.equals(owner)
                    && ("wrapIfNecessary".equals(name) || "createProxy".equals(name))) {
                return true;
            }
        }
        return false;
    }

    private static boolean callsByteBuddyFactory(Set<MethodReference.Handle> callees) {
        if (callees == null || callees.isEmpty()) {
            return false;
        }
        for (MethodReference.Handle callee : callees) {
            if (callee == null || callee.getClassReference() == null || callee.getName() == null) {
                continue;
            }
            String owner = callee.getClassReference().getName();
            if (owner == null || !owner.startsWith(BYTE_BUDDY_PREFIX)) {
                continue;
            }
            String name = callee.getName();
            if ("subclass".equals(name) || "method".equals(name)
                    || "intercept".equals(name) || "make".equals(name)
                    || "load".equals(name) || "build".equals(name)) {
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

    private static Map<Integer, List<MethodReference.Handle>> collectJdkHandlerInvokeMethods(BuildContext ctx) {
        return collectByInterfaceAndSignature(
                ctx,
                Collections.singletonList(HANDLER),
                HANDLER_INVOKE_NAME,
                Collections.singleton(HANDLER_INVOKE_DESC)
        );
    }

    private static Map<Integer, List<MethodReference.Handle>> collectCglibInterceptMethods(BuildContext ctx) {
        List<ClassReference.Handle> interfaces = new ArrayList<>();
        interfaces.add(SPRING_CGLIB);
        interfaces.add(NETSF_CGLIB);
        Set<String> descs = new HashSet<>();
        descs.add(SPRING_CGLIB_INTERCEPT_DESC);
        descs.add(NETSF_CGLIB_INTERCEPT_DESC);
        return collectByInterfaceAndSignature(ctx, interfaces, CGLIB_INTERCEPT_NAME, descs);
    }

    private static Map<Integer, List<MethodReference.Handle>> collectSpringAopInterceptorMethods(BuildContext ctx) {
        return collectByInterfaceAndSignature(
                ctx,
                Collections.singletonList(AOP_MI),
                AOP_INVOKE_NAME,
                Collections.singleton(AOP_INVOKE_DESC)
        );
    }

    private static Map<Integer, List<MethodReference.Handle>> collectByteBuddyInterceptorMethods(BuildContext ctx) {
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
            MethodReference.Handle canonical = new MethodReference.Handle(
                    new ClassReference.Handle(handle.getClassReference().getName(), jarId),
                    handle.getName(),
                    handle.getDesc());
            out.computeIfAbsent(jarId, k -> new ArrayList<>()).add(canonical);
        }
        sortAndDedup(out);
        return out;
    }

    private static Map<Integer, List<MethodReference.Handle>> collectByInterfaceAndSignature(
            BuildContext ctx,
            List<ClassReference.Handle> interfaces,
            String methodName,
            Set<String> methodDescs) {
        Map<Integer, List<MethodReference.Handle>> out = new HashMap<>();
        if (ctx == null || ctx.methodMap == null || ctx.methodMap.isEmpty()) {
            return out;
        }
        InheritanceMap inheritance = ctx.inheritanceMap;
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
            if (!isAnyImplementer(handle.getClassReference(), interfaces, ctx.classMap, inheritance)) {
                continue;
            }
            Integer jarId = ref.getJarId();
            if (jarId == null) {
                continue;
            }
            MethodReference.Handle canonical = new MethodReference.Handle(
                    new ClassReference.Handle(handle.getClassReference().getName(), jarId),
                    handle.getName(),
                    handle.getDesc());
            out.computeIfAbsent(jarId, k -> new ArrayList<>()).add(canonical);
        }
        sortAndDedup(out);
        return out;
    }

    private static boolean isAnyImplementer(ClassReference.Handle owner,
                                            List<ClassReference.Handle> targets,
                                            Map<ClassReference.Handle, ClassReference> classMap,
                                            InheritanceMap inheritance) {
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
            if (classMap != null && !classMap.isEmpty()) {
                ClassReference clazz = classMap.get(owner);
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
                sorted.sort((a, b) -> {
                    String an = a == null || a.getClassReference() == null ? "" : a.getClassReference().getName();
                    String bn = b == null || b.getClassReference() == null ? "" : b.getClassReference().getName();
                    int c = an.compareTo(bn);
                    if (c != 0) {
                        return c;
                    }
                    String am = a == null ? "" : a.getName();
                    String bm = b == null ? "" : b.getName();
                    c = am.compareTo(bm);
                    if (c != 0) {
                        return c;
                    }
                    String ad = a == null ? "" : a.getDesc();
                    String bd = b == null ? "" : b.getDesc();
                    return ad.compareTo(bd);
                });
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
            out.sort((a, b) -> {
                String an = a == null || a.getClassReference() == null ? "" : a.getClassReference().getName();
                String bn = b == null || b.getClassReference() == null ? "" : b.getClassReference().getName();
                int c = an.compareTo(bn);
                if (c != 0) {
                    return c;
                }
                String am = a == null ? "" : a.getName();
                String bm = b == null ? "" : b.getName();
                c = am.compareTo(bm);
                if (c != 0) {
                    return c;
                }
                String ad = a == null ? "" : a.getDesc();
                String bd = b == null ? "" : b.getDesc();
                return ad.compareTo(bd);
            });
        }
        return out;
    }
}
