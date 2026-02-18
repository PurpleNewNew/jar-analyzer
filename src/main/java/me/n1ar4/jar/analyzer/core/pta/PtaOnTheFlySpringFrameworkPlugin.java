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

import me.n1ar4.jar.analyzer.analyze.spring.SpringController;
import me.n1ar4.jar.analyzer.analyze.spring.SpringMapping;
import me.n1ar4.jar.analyzer.core.InheritanceMap;
import me.n1ar4.jar.analyzer.core.MethodCallMeta;
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

/**
 * Move SpringFrameworkEdgeRule to PTA event loop.
 */
final class PtaOnTheFlySpringFrameworkPlugin implements PtaPlugin {
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

    private PtaPluginBridge bridge;
    private BuildContext ctx;
    private InheritanceMap inheritance;

    private final LinkedHashSet<MethodReference.Handle> bootstrapCallers = new LinkedHashSet<>();
    private final LinkedHashSet<MethodReference.Handle> webCallers = new LinkedHashSet<>();
    private final LinkedHashSet<MethodReference.Handle> txCallers = new LinkedHashSet<>();
    private final LinkedHashSet<MethodReference.Handle> beanLookupCallers = new LinkedHashSet<>();

    private List<MethodReference.Handle> controllerTargets = Collections.emptyList();
    private List<MethodReference.Handle> lifecycleTargets = Collections.emptyList();
    private List<MethodReference.Handle> txTargets = Collections.emptyList();
    private List<MethodReference.Handle> beanTargets = Collections.emptyList();

    private boolean webFallsBackToBootstrap;
    private boolean txFallsBackToBootstrap;

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

        List<MethodReference.Handle> bootstrap = collectBootstrapCallers();
        List<MethodReference.Handle> webDispatchers = collectFrameworkWebDispatchers();
        List<MethodReference.Handle> txInterceptors = collectTransactionInterceptors();
        List<MethodReference.Handle> beanLookups = collectBeanLookupCallers();

        controllerTargets = collectControllerTargets();
        lifecycleTargets = collectLifecycleTargets();
        txTargets = collectTransactionalTargets();
        beanTargets = collectBeanCandidateMethods();

        webFallsBackToBootstrap = webDispatchers.isEmpty();
        txFallsBackToBootstrap = txInterceptors.isEmpty();

        addLimited(bootstrapCallers, bootstrap);
        addLimited(webCallers, webFallsBackToBootstrap ? bootstrap : webDispatchers);
        addLimited(txCallers, txFallsBackToBootstrap ? bootstrap : txInterceptors);
        addLimited(beanLookupCallers, beanLookups);
    }

    @Override
    public void onNewContextMethod(PtaContextMethod method) {
        if (method == null || method.getMethod() == null || bridge == null) {
            return;
        }
        MethodReference.Handle caller = canonical(method.getMethod());
        if (caller == null) {
            return;
        }

        if (webCallers.contains(caller)) {
            addTargets(method, controllerTargets, REASON_WEB, MethodCallMeta.CONF_MEDIUM);
        }
        if (bootstrapCallers.contains(caller)) {
            addTargets(method, lifecycleTargets, REASON_LIFECYCLE, MethodCallMeta.CONF_LOW);
        }
        if (txCallers.contains(caller)) {
            addTargets(method, txTargets, REASON_TX, MethodCallMeta.CONF_MEDIUM);
        }
        if (beanLookupCallers.contains(caller)) {
            addTargets(method, beanTargets, REASON_BEAN, MethodCallMeta.CONF_LOW);
        }

        if (ctx == null || ctx.methodCalls == null) {
            return;
        }
        Set<MethodReference.Handle> callees = ctx.methodCalls.get(caller);
        if (callees == null || callees.isEmpty()) {
            return;
        }
        List<MethodReference.Handle> snapshot = new ArrayList<>(callees);
        for (MethodReference.Handle callee : snapshot) {
            if (isSpringBootstrapCall(callee)) {
                applyBootstrap(caller, method);
            }
            if (isBeanLookupCall(callee)) {
                applyBeanLookup(caller, method);
            }
        }
    }

    @Override
    public void onNewCallEdge(PtaContextMethod callerContext,
                              MethodReference.Handle caller,
                              MethodReference.Handle callee,
                              String edgeType,
                              String confidence,
                              int opcode) {
        if (callerContext == null || callerContext.getMethod() == null || callee == null) {
            return;
        }
        MethodReference.Handle canonicalCaller = canonical(callerContext.getMethod());
        if (canonicalCaller == null) {
            return;
        }
        if (isSpringBootstrapCall(callee)) {
            applyBootstrap(canonicalCaller, callerContext);
        }
        if (isBeanLookupCall(callee)) {
            applyBeanLookup(canonicalCaller, callerContext);
        }
    }

    private void applyBootstrap(MethodReference.Handle caller, PtaContextMethod callerContext) {
        if (caller == null || callerContext == null) {
            return;
        }
        addCaller(bootstrapCallers, caller);
        if (webFallsBackToBootstrap) {
            addCaller(webCallers, caller);
        }
        if (txFallsBackToBootstrap) {
            addCaller(txCallers, caller);
        }
        addTargets(callerContext, controllerTargets, REASON_WEB, MethodCallMeta.CONF_MEDIUM);
        addTargets(callerContext, lifecycleTargets, REASON_LIFECYCLE, MethodCallMeta.CONF_LOW);
        addTargets(callerContext, txTargets, REASON_TX, MethodCallMeta.CONF_MEDIUM);
    }

    private void applyBeanLookup(MethodReference.Handle caller, PtaContextMethod callerContext) {
        if (caller == null || callerContext == null) {
            return;
        }
        addCaller(beanLookupCallers, caller);
        addTargets(callerContext, beanTargets, REASON_BEAN, MethodCallMeta.CONF_LOW);
    }

    private void addTargets(PtaContextMethod callerContext,
                            List<MethodReference.Handle> targets,
                            String reason,
                            String confidence) {
        if (bridge == null || callerContext == null || reason == null
                || targets == null || targets.isEmpty()) {
            return;
        }
        String fireKey = callerContext.id() + "|" + reason;
        if (!fired.add(fireKey)) {
            return;
        }
        int limit = Math.min(MAX_TARGETS_PER_CALLER, targets.size());
        for (int i = 0; i < limit; i++) {
            MethodReference.Handle target = targets.get(i);
            if (target == null) {
                continue;
            }
            bridge.addSemanticEdge(callerContext,
                    target,
                    MethodCallMeta.TYPE_FRAMEWORK,
                    confidence,
                    reason,
                    Opcodes.INVOKEVIRTUAL,
                    null,
                    "sem:" + reason);
        }
    }

    private static <T> void addLimited(LinkedHashSet<T> out, List<T> source) {
        if (out == null || source == null || source.isEmpty()) {
            return;
        }
        int limit = Math.min(MAX_CALLERS, source.size());
        for (int i = 0; i < limit; i++) {
            T value = source.get(i);
            if (value != null) {
                out.add(value);
            }
        }
    }

    private static boolean addCaller(LinkedHashSet<MethodReference.Handle> set,
                                     MethodReference.Handle caller) {
        if (set == null || caller == null) {
            return false;
        }
        if (set.contains(caller)) {
            return true;
        }
        if (set.size() >= MAX_CALLERS) {
            return false;
        }
        set.add(caller);
        return true;
    }

    private static boolean isSpringBootstrapCall(MethodReference.Handle callee) {
        if (callee == null || callee.getClassReference() == null || callee.getName() == null) {
            return false;
        }
        return SPRING_APP_OWNER.equals(callee.getClassReference().getName())
                && SPRING_APP_RUN.equals(callee.getName());
    }

    private static boolean isBeanLookupCall(MethodReference.Handle callee) {
        if (callee == null || callee.getClassReference() == null || callee.getName() == null) {
            return false;
        }
        if (!"getBean".equals(callee.getName())) {
            return false;
        }
        String owner = callee.getClassReference().getName();
        return owner != null && BEAN_FACTORY_OWNERS.contains(owner);
    }

    private List<MethodReference.Handle> collectBootstrapCallers() {
        LinkedHashSet<MethodReference.Handle> out = new LinkedHashSet<>();
        if (ctx == null || ctx.methodCalls == null || ctx.methodCalls.isEmpty()) {
            return Collections.emptyList();
        }
        for (Map.Entry<MethodReference.Handle, HashSet<MethodReference.Handle>> entry : ctx.methodCalls.entrySet()) {
            MethodReference.Handle caller = canonical(entry.getKey());
            HashSet<MethodReference.Handle> callees = entry.getValue();
            if (caller == null || callees == null || callees.isEmpty()) {
                continue;
            }
            for (MethodReference.Handle callee : callees) {
                if (isSpringBootstrapCall(callee)) {
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
                if (!hasAnyClassAnno(clazz, Collections.singleton(ANN_SPRING_BOOT_APP))) {
                    continue;
                }
                MethodReference.Handle canonical = canonical(method);
                if (canonical != null) {
                    out.add(canonical);
                }
            }
        }
        return sortMethods(out);
    }

    private List<MethodReference.Handle> collectFrameworkWebDispatchers() {
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
            MethodReference.Handle canonical = canonical(handle);
            if (canonical != null) {
                out.add(canonical);
            }
        }
        return sortMethods(out);
    }

    private List<MethodReference.Handle> collectControllerTargets() {
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
                    MethodReference.Handle canonical = canonical(mapping.getMethodName());
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
                if (!hasMethodAnnoPrefix(ref, "Lorg/springframework/web/bind/annotation/")) {
                    continue;
                }
                MethodReference.Handle canonical = canonical(handle);
                if (canonical != null) {
                    out.add(canonical);
                }
            }
        }
        return sortMethods(out);
    }

    private List<MethodReference.Handle> collectLifecycleTargets() {
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
            boolean stereotype = hasAnyClassAnno(clazz, STEREOTYPE_ANNOS);
            if (stereotype && "<init>".equals(handle.getName()) && "()V".equals(handle.getDesc())) {
                MethodReference.Handle canonical = canonical(handle);
                if (canonical != null) {
                    out.add(canonical);
                }
                continue;
            }
            if ("afterPropertiesSet".equals(handle.getName()) && "()V".equals(handle.getDesc())
                    && inheritance != null && inheritance.isSubclassOf(handle.getClassReference(), INITIALIZING_BEAN)) {
                MethodReference.Handle canonical = canonical(handle);
                if (canonical != null) {
                    out.add(canonical);
                }
                continue;
            }
            if (hasMethodAnno(ref, POST_CONSTRUCT_ANNOS) || hasMethodAnno(ref, ANN_BEAN)) {
                MethodReference.Handle canonical = canonical(handle);
                if (canonical != null) {
                    out.add(canonical);
                }
            }
        }
        return sortMethods(out);
    }

    private List<MethodReference.Handle> collectTransactionalTargets() {
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
                MethodReference.Handle canonical = canonical(handle);
                if (canonical != null) {
                    out.add(canonical);
                }
            }
        }
        return sortMethods(out);
    }

    private List<MethodReference.Handle> collectTransactionInterceptors() {
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
            MethodReference.Handle canonical = canonical(handle);
            if (canonical != null) {
                out.add(canonical);
            }
        }
        return sortMethods(out);
    }

    private List<MethodReference.Handle> collectBeanLookupCallers() {
        LinkedHashSet<MethodReference.Handle> out = new LinkedHashSet<>();
        if (ctx == null || ctx.methodCalls == null || ctx.methodCalls.isEmpty()) {
            return Collections.emptyList();
        }
        for (Map.Entry<MethodReference.Handle, HashSet<MethodReference.Handle>> entry : ctx.methodCalls.entrySet()) {
            MethodReference.Handle caller = canonical(entry.getKey());
            HashSet<MethodReference.Handle> callees = entry.getValue();
            if (caller == null || callees == null || callees.isEmpty()) {
                continue;
            }
            for (MethodReference.Handle callee : callees) {
                if (isBeanLookupCall(callee)) {
                    out.add(caller);
                    break;
                }
            }
        }
        return sortMethods(out);
    }

    private List<MethodReference.Handle> collectBeanCandidateMethods() {
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
            MethodReference.Handle canonical = canonical(handle);
            if (canonical != null) {
                out.add(canonical);
            }
        }
        return sortMethods(out);
    }

    private MethodReference.Handle canonical(MethodReference.Handle handle) {
        if (handle == null || handle.getClassReference() == null || ctx == null || ctx.methodMap == null) {
            return null;
        }
        MethodReference probe = ctx.methodMap.get(new MethodReference.Handle(
                new ClassReference.Handle(handle.getClassReference().getName()),
                handle.getName(),
                handle.getDesc()));
        int jarId = probe == null || probe.getJarId() == null ? -1 : probe.getJarId();
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
            out.sort(Comparator.comparing(PtaOnTheFlySpringFrameworkPlugin::methodKey));
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
