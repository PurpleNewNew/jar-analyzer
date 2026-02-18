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

import me.n1ar4.jar.analyzer.analyze.spring.SpringController;
import me.n1ar4.jar.analyzer.analyze.spring.SpringMapping;
import me.n1ar4.jar.analyzer.core.InheritanceMap;
import me.n1ar4.jar.analyzer.core.MethodCallKey;
import me.n1ar4.jar.analyzer.core.MethodCallMeta;
import me.n1ar4.jar.analyzer.core.MethodCallUtils;
import me.n1ar4.jar.analyzer.core.build.BuildContext;
import me.n1ar4.jar.analyzer.core.reference.AnnoReference;
import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class SpringFrameworkEdgeRule implements EdgeInferRule {
    private static final String REASON_WEB = "spring_web_entry";
    private static final String REASON_LIFECYCLE = "spring_lifecycle";
    private static final String REASON_TX = "spring_transaction";
    private static final String REASON_BEAN = "spring_getBean";

    private static final int MAX_CALLERS = 48;
    private static final int MAX_TARGETS_PER_CALLER = 160;

    private static final String SPRING_APP_OWNER = "org/springframework/boot/SpringApplication";
    private static final String SPRING_APP_RUN = "run";

    private static final String ANN_SPRING_BOOT_APP =
            "Lorg/springframework/boot/autoconfigure/SpringBootApplication;";
    private static final Set<String> STEREOTYPE_ANNOS = new HashSet<>(Arrays.asList(
            "Lorg/springframework/stereotype/Controller;",
            "Lorg/springframework/web/bind/annotation/RestController;",
            "Lorg/springframework/stereotype/Service;",
            "Lorg/springframework/stereotype/Repository;",
            "Lorg/springframework/stereotype/Component;",
            "Lorg/springframework/context/annotation/Configuration;"
    ));
    private static final Set<String> TRANSACTIONAL_ANNOS = new HashSet<>(Arrays.asList(
            "Lorg/springframework/transaction/annotation/Transactional;",
            "Ljavax/transaction/Transactional;",
            "Ljakarta/transaction/Transactional;"
    ));
    private static final Set<String> POST_CONSTRUCT_ANNOS = new HashSet<>(Arrays.asList(
            "Ljavax/annotation/PostConstruct;",
            "Ljakarta/annotation/PostConstruct;"
    ));
    private static final String ANN_BEAN = "Lorg/springframework/context/annotation/Bean;";

    private static final ClassReference.Handle INITIALIZING_BEAN =
            new ClassReference.Handle("org/springframework/beans/factory/InitializingBean");

    private static final Set<String> WEB_DISPATCHER_OWNERS = new HashSet<>(Arrays.asList(
            "org/springframework/web/servlet/DispatcherServlet",
            "org/springframework/web/servlet/mvc/method/annotation/RequestMappingHandlerAdapter",
            "org/springframework/web/method/support/InvocableHandlerMethod"
    ));

    private static final Set<String> TX_INTERCEPTOR_OWNERS = new HashSet<>(Arrays.asList(
            "org/springframework/transaction/interceptor/TransactionInterceptor",
            "org/springframework/aop/framework/JdkDynamicAopProxy",
            "org/springframework/aop/framework/CglibAopProxy$DynamicAdvisedInterceptor"
    ));

    private static final Set<String> BEAN_FACTORY_OWNERS = new HashSet<>(Arrays.asList(
            "org/springframework/context/ApplicationContext",
            "org/springframework/beans/factory/BeanFactory",
            "org/springframework/beans/factory/ListableBeanFactory"
    ));

    @Override
    public String id() {
        return "springFramework";
    }

    @Override
    public int apply(BuildContext ctx) {
        if (ctx == null || ctx.methodMap == null || ctx.methodMap.isEmpty()) {
            return 0;
        }
        List<MethodReference.Handle> bootstrap = collectBootstrapCallers(ctx);
        List<MethodReference.Handle> webDispatchers = collectFrameworkWebDispatchers(ctx);
        List<MethodReference.Handle> controllerTargets = collectControllerTargets(ctx);
        List<MethodReference.Handle> lifecycleTargets = collectLifecycleTargets(ctx);
        List<MethodReference.Handle> txTargets = collectTransactionalTargets(ctx);
        List<MethodReference.Handle> txCallers = collectTransactionInterceptors(ctx);
        List<MethodReference.Handle> beanLookupCallers = collectBeanLookupCallers(ctx);
        List<MethodReference.Handle> beanTargets = collectBeanCandidateMethods(ctx);

        int added = 0;
        List<MethodReference.Handle> webCallers = webDispatchers.isEmpty() ? bootstrap : webDispatchers;
        added += addCrossEdges(ctx, webCallers, controllerTargets, REASON_WEB, MethodCallMeta.CONF_MEDIUM);
        added += addCrossEdges(ctx, bootstrap, lifecycleTargets, REASON_LIFECYCLE, MethodCallMeta.CONF_LOW);
        if (txCallers.isEmpty()) {
            txCallers = bootstrap;
        }
        added += addCrossEdges(ctx, txCallers, txTargets, REASON_TX, MethodCallMeta.CONF_MEDIUM);
        added += addCrossEdges(ctx, beanLookupCallers, beanTargets, REASON_BEAN, MethodCallMeta.CONF_LOW);
        return added;
    }

    private static int addCrossEdges(BuildContext ctx,
                                     List<MethodReference.Handle> callers,
                                     List<MethodReference.Handle> targets,
                                     String reason,
                                     String confidence) {
        if (ctx == null || ctx.methodCalls == null || callers == null || callers.isEmpty()
                || targets == null || targets.isEmpty()) {
            return 0;
        }
        int added = 0;
        int callerLimit = Math.min(MAX_CALLERS, callers.size());
        for (int i = 0; i < callerLimit; i++) {
            MethodReference.Handle caller = callers.get(i);
            if (caller == null) {
                continue;
            }
            HashSet<MethodReference.Handle> callees = ctx.methodCalls.computeIfAbsent(caller, k -> new HashSet<>());
            int targetLimit = Math.min(MAX_TARGETS_PER_CALLER, targets.size());
            for (int t = 0; t < targetLimit; t++) {
                MethodReference.Handle target = targets.get(t);
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
                        MethodCallMeta.TYPE_FRAMEWORK, confidence, reason, Opcodes.INVOKEVIRTUAL);
            }
        }
        return added;
    }

    private static List<MethodReference.Handle> collectBootstrapCallers(BuildContext ctx) {
        LinkedHashSet<MethodReference.Handle> out = new LinkedHashSet<>();
        if (ctx == null || ctx.methodCalls == null || ctx.methodCalls.isEmpty()) {
            return Collections.emptyList();
        }
        for (Map.Entry<MethodReference.Handle, HashSet<MethodReference.Handle>> entry : ctx.methodCalls.entrySet()) {
            MethodReference.Handle caller = canonical(entry.getKey(), ctx.methodMap);
            HashSet<MethodReference.Handle> callees = entry.getValue();
            if (caller == null || callees == null || callees.isEmpty()) {
                continue;
            }
            for (MethodReference.Handle callee : callees) {
                if (callee == null || callee.getClassReference() == null) {
                    continue;
                }
                if (SPRING_APP_OWNER.equals(callee.getClassReference().getName())
                        && SPRING_APP_RUN.equals(callee.getName())) {
                    out.add(caller);
                    break;
                }
            }
        }

        if (ctx.classMap != null && ctx.methodMap != null) {
            for (MethodReference.Handle method : ctx.methodMap.keySet()) {
                if (method == null || method.getClassReference() == null) {
                    continue;
                }
                if (!"main".equals(method.getName()) || !"([Ljava/lang/String;)V".equals(method.getDesc())) {
                    continue;
                }
                ClassReference clazz = ctx.classMap.get(method.getClassReference());
                if (hasAnyClassAnno(clazz, Collections.singleton(ANN_SPRING_BOOT_APP))) {
                    MethodReference.Handle canonical = canonical(method, ctx.methodMap);
                    if (canonical != null) {
                        out.add(canonical);
                    }
                }
            }
        }
        return sortMethods(out);
    }

    private static List<MethodReference.Handle> collectFrameworkWebDispatchers(BuildContext ctx) {
        LinkedHashSet<MethodReference.Handle> out = new LinkedHashSet<>();
        if (ctx == null || ctx.methodMap == null || ctx.methodMap.isEmpty()) {
            return Collections.emptyList();
        }
        for (MethodReference.Handle handle : ctx.methodMap.keySet()) {
            if (handle == null || handle.getClassReference() == null || handle.getName() == null) {
                continue;
            }
            String owner = handle.getClassReference().getName();
            if (owner == null || !WEB_DISPATCHER_OWNERS.contains(owner)) {
                continue;
            }
            String name = handle.getName();
            if (!"doDispatch".equals(name) && !"doService".equals(name)
                    && !"service".equals(name) && !"invokeHandlerMethod".equals(name)
                    && !"invokeForRequest".equals(name) && !"doInvoke".equals(name)) {
                continue;
            }
            MethodReference.Handle canonical = canonical(handle, ctx.methodMap);
            if (canonical != null) {
                out.add(canonical);
            }
        }
        return sortMethods(out);
    }

    private static List<MethodReference.Handle> collectControllerTargets(BuildContext ctx) {
        LinkedHashSet<MethodReference.Handle> out = new LinkedHashSet<>();
        if (ctx == null) {
            return Collections.emptyList();
        }
        if (ctx.controllers != null && !ctx.controllers.isEmpty()) {
            for (SpringController controller : ctx.controllers) {
                if (controller == null || controller.getMappings() == null) {
                    continue;
                }
                for (SpringMapping mapping : controller.getMappings()) {
                    if (mapping == null) {
                        continue;
                    }
                    MethodReference.Handle canonical = canonical(mapping.getMethodName(), ctx.methodMap);
                    if (canonical != null) {
                        out.add(canonical);
                    }
                }
            }
        }
        if (out.isEmpty() && ctx.methodMap != null) {
            for (Map.Entry<MethodReference.Handle, MethodReference> entry : ctx.methodMap.entrySet()) {
                MethodReference.Handle handle = entry.getKey();
                MethodReference ref = entry.getValue();
                if (handle == null || ref == null) {
                    continue;
                }
                if (hasMethodAnnoPrefix(ref, "Lorg/springframework/web/bind/annotation/")) {
                    MethodReference.Handle canonical = canonical(handle, ctx.methodMap);
                    if (canonical != null) {
                        out.add(canonical);
                    }
                }
            }
        }
        return sortMethods(out);
    }

    private static List<MethodReference.Handle> collectLifecycleTargets(BuildContext ctx) {
        LinkedHashSet<MethodReference.Handle> out = new LinkedHashSet<>();
        if (ctx == null || ctx.methodMap == null || ctx.methodMap.isEmpty()) {
            return Collections.emptyList();
        }
        InheritanceMap inheritance = ctx.inheritanceMap;
        for (Map.Entry<MethodReference.Handle, MethodReference> entry : ctx.methodMap.entrySet()) {
            MethodReference.Handle handle = entry.getKey();
            MethodReference ref = entry.getValue();
            if (handle == null || ref == null || handle.getClassReference() == null) {
                continue;
            }
            ClassReference clazz = ctx.classMap == null ? null : ctx.classMap.get(handle.getClassReference());
            boolean stereotype = hasAnyClassAnno(clazz, STEREOTYPE_ANNOS);
            if (stereotype && "<init>".equals(handle.getName()) && "()V".equals(handle.getDesc())) {
                MethodReference.Handle canonical = canonical(handle, ctx.methodMap);
                if (canonical != null) {
                    out.add(canonical);
                }
                continue;
            }
            if ("afterPropertiesSet".equals(handle.getName()) && "()V".equals(handle.getDesc())
                    && inheritance != null && inheritance.isSubclassOf(handle.getClassReference(), INITIALIZING_BEAN)) {
                MethodReference.Handle canonical = canonical(handle, ctx.methodMap);
                if (canonical != null) {
                    out.add(canonical);
                }
                continue;
            }
            if (hasMethodAnno(ref, POST_CONSTRUCT_ANNOS) || hasMethodAnno(ref, ANN_BEAN)) {
                MethodReference.Handle canonical = canonical(handle, ctx.methodMap);
                if (canonical != null) {
                    out.add(canonical);
                }
            }
        }
        return sortMethods(out);
    }

    private static List<MethodReference.Handle> collectTransactionalTargets(BuildContext ctx) {
        LinkedHashSet<MethodReference.Handle> out = new LinkedHashSet<>();
        if (ctx == null || ctx.methodMap == null || ctx.methodMap.isEmpty()) {
            return Collections.emptyList();
        }
        for (Map.Entry<MethodReference.Handle, MethodReference> entry : ctx.methodMap.entrySet()) {
            MethodReference.Handle handle = entry.getKey();
            MethodReference ref = entry.getValue();
            if (handle == null || ref == null || handle.getClassReference() == null) {
                continue;
            }
            ClassReference clazz = ctx.classMap == null ? null : ctx.classMap.get(handle.getClassReference());
            if (hasMethodAnno(ref, TRANSACTIONAL_ANNOS) || hasAnyClassAnno(clazz, TRANSACTIONAL_ANNOS)) {
                MethodReference.Handle canonical = canonical(handle, ctx.methodMap);
                if (canonical != null) {
                    out.add(canonical);
                }
            }
        }
        return sortMethods(out);
    }

    private static List<MethodReference.Handle> collectTransactionInterceptors(BuildContext ctx) {
        LinkedHashSet<MethodReference.Handle> out = new LinkedHashSet<>();
        if (ctx == null || ctx.methodMap == null || ctx.methodMap.isEmpty()) {
            return Collections.emptyList();
        }
        for (MethodReference.Handle handle : ctx.methodMap.keySet()) {
            if (handle == null || handle.getClassReference() == null || handle.getName() == null) {
                continue;
            }
            String owner = handle.getClassReference().getName();
            if (owner == null || !TX_INTERCEPTOR_OWNERS.contains(owner)) {
                continue;
            }
            if (!"invoke".equals(handle.getName()) && !"intercept".equals(handle.getName())) {
                continue;
            }
            MethodReference.Handle canonical = canonical(handle, ctx.methodMap);
            if (canonical != null) {
                out.add(canonical);
            }
        }
        return sortMethods(out);
    }

    private static List<MethodReference.Handle> collectBeanLookupCallers(BuildContext ctx) {
        LinkedHashSet<MethodReference.Handle> out = new LinkedHashSet<>();
        if (ctx == null || ctx.methodCalls == null || ctx.methodCalls.isEmpty()) {
            return Collections.emptyList();
        }
        for (Map.Entry<MethodReference.Handle, HashSet<MethodReference.Handle>> entry : ctx.methodCalls.entrySet()) {
            MethodReference.Handle caller = canonical(entry.getKey(), ctx.methodMap);
            HashSet<MethodReference.Handle> callees = entry.getValue();
            if (caller == null || callees == null || callees.isEmpty()) {
                continue;
            }
            for (MethodReference.Handle callee : callees) {
                if (callee == null || callee.getClassReference() == null || callee.getName() == null) {
                    continue;
                }
                if (!"getBean".equals(callee.getName())) {
                    continue;
                }
                String owner = callee.getClassReference().getName();
                if (owner != null && BEAN_FACTORY_OWNERS.contains(owner)) {
                    out.add(caller);
                    break;
                }
            }
        }
        return sortMethods(out);
    }

    private static List<MethodReference.Handle> collectBeanCandidateMethods(BuildContext ctx) {
        LinkedHashSet<MethodReference.Handle> out = new LinkedHashSet<>();
        if (ctx == null || ctx.methodMap == null || ctx.methodMap.isEmpty() || ctx.classMap == null) {
            return Collections.emptyList();
        }
        for (Map.Entry<MethodReference.Handle, MethodReference> entry : ctx.methodMap.entrySet()) {
            MethodReference.Handle handle = entry.getKey();
            MethodReference ref = entry.getValue();
            if (handle == null || ref == null || handle.getClassReference() == null || handle.getName() == null) {
                continue;
            }
            if ("<init>".equals(handle.getName()) || "<clinit>".equals(handle.getName())) {
                continue;
            }
            ClassReference clazz = ctx.classMap.get(handle.getClassReference());
            if (!hasAnyClassAnno(clazz, STEREOTYPE_ANNOS)) {
                continue;
            }
            if ((ref.getAccess() & Opcodes.ACC_PUBLIC) == 0) {
                continue;
            }
            MethodReference.Handle canonical = canonical(handle, ctx.methodMap);
            if (canonical != null) {
                out.add(canonical);
            }
        }
        return sortMethods(out);
    }

    private static MethodReference.Handle canonical(MethodReference.Handle handle,
                                                    Map<MethodReference.Handle, MethodReference> methodMap) {
        if (handle == null || handle.getClassReference() == null) {
            return null;
        }
        MethodReference ref = methodMap == null ? null : methodMap.get(
                new MethodReference.Handle(new ClassReference.Handle(handle.getClassReference().getName()),
                        handle.getName(), handle.getDesc()));
        int jarId = ref == null || ref.getJarId() == null ? -1 : ref.getJarId();
        return new MethodReference.Handle(
                new ClassReference.Handle(handle.getClassReference().getName(), jarId),
                handle.getName(),
                handle.getDesc());
    }

    private static boolean hasAnyClassAnno(ClassReference clazz, Set<String> candidates) {
        if (clazz == null || clazz.getAnnotations() == null || clazz.getAnnotations().isEmpty()
                || candidates == null || candidates.isEmpty()) {
            return false;
        }
        for (AnnoReference anno : clazz.getAnnotations()) {
            if (anno == null || anno.getAnnoName() == null) {
                continue;
            }
            if (candidates.contains(anno.getAnnoName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasMethodAnnoPrefix(MethodReference method, String annoPrefix) {
        if (method == null || method.getAnnotations() == null || method.getAnnotations().isEmpty()
                || annoPrefix == null || annoPrefix.isEmpty()) {
            return false;
        }
        for (AnnoReference anno : method.getAnnotations()) {
            if (anno == null || anno.getAnnoName() == null) {
                continue;
            }
            if (anno.getAnnoName().startsWith(annoPrefix)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasMethodAnno(MethodReference method, Set<String> annos) {
        if (method == null || method.getAnnotations() == null || method.getAnnotations().isEmpty()
                || annos == null || annos.isEmpty()) {
            return false;
        }
        for (AnnoReference anno : method.getAnnotations()) {
            if (anno == null || anno.getAnnoName() == null) {
                continue;
            }
            if (annos.contains(anno.getAnnoName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasMethodAnno(MethodReference method, String annoName) {
        if (method == null || method.getAnnotations() == null || method.getAnnotations().isEmpty()
                || annoName == null || annoName.isEmpty()) {
            return false;
        }
        for (AnnoReference anno : method.getAnnotations()) {
            if (anno == null || anno.getAnnoName() == null) {
                continue;
            }
            if (annoName.equals(anno.getAnnoName())) {
                return true;
            }
        }
        return false;
    }

    private static List<MethodReference.Handle> sortMethods(Set<MethodReference.Handle> methods) {
        if (methods == null || methods.isEmpty()) {
            return Collections.emptyList();
        }
        ArrayList<MethodReference.Handle> out = new ArrayList<>(methods);
        if (out.size() > 1) {
            out.sort(Comparator.comparing(SpringFrameworkEdgeRule::methodKey));
        }
        return out;
    }

    private static String methodKey(MethodReference.Handle method) {
        if (method == null || method.getClassReference() == null) {
            return "";
        }
        return method.getClassReference().getName() + "."
                + method.getName() + method.getDesc() + "@" + method.getJarId();
    }
}
