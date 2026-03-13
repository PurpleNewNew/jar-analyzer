/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */
package me.n1ar4.jar.analyzer.rules;

import me.n1ar4.jar.analyzer.analyze.spring.SpringController;
import me.n1ar4.jar.analyzer.analyze.spring.SpringMapping;
import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.core.WebEntryMethodSpec;
import me.n1ar4.jar.analyzer.core.WebEntryMethods;
import me.n1ar4.jar.analyzer.core.reference.AnnoReference;
import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.graph.store.GraphNode;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

public final class SourceRuleSupport {
    private static final Set<String> RPC_SERVICE_ANNOTATIONS = Set.of(
            "Lorg/apache/dubbo/config/annotation/DubboService;",
            "Lorg/apache/dubbo/config/annotation/Service;",
            "Lcom/alibaba/dubbo/config/annotation/Service;",
            "Lnet/devh/boot/grpc/server/service/GrpcService;",
            "Ljavax/jws/WebService;",
            "Ljakarta/jws/WebService;",
            "Ljavax/xml/ws/WebServiceProvider;",
            "Ljakarta/xml/ws/WebServiceProvider;"
    );
    private static final Set<String> STRUTS_ENTRY_TYPES = Set.of(
            "com/opensymphony/xwork2/ActionSupport",
            "com/opensymphony/xwork2/Action",
            "org/apache/struts/action/Action",
            "org/apache/struts/actions/DispatchAction"
    );
    private static final Set<String> STRUTS_ENTRY_METHODS = Set.of(
            "execute",
            "doExecute",
            "unspecified"
    );
    private static final Set<String> NETTY_HANDLER_TYPES = Set.of(
            "io/netty/channel/ChannelInboundHandler",
            "io/netty/channel/SimpleChannelInboundHandler",
            "org/jboss/netty/channel/SimpleChannelUpstreamHandler"
    );
    private static final Set<String> NETTY_HANDLER_METHODS = Set.of(
            "channelRead",
            "channelRead0",
            "messageReceived"
    );
    private static final Set<String> NETTY_DECODER_TYPES = Set.of(
            "io/netty/handler/codec/ByteToMessageDecoder",
            "io/netty/handler/codec/MessageToMessageDecoder",
            "org/jboss/netty/handler/codec/frame/FrameDecoder"
    );

    private SourceRuleSupport() {
    }

    public static RuleSnapshot snapshotCurrentRules() {
        ModelRegistry.SourceRuleSnapshot snapshot = ModelRegistry.getSourceRuleSnapshot();
        return RuleSnapshot.from(snapshot.sourceModels(), snapshot.sourceAnnotations());
    }

    public static int resolveRuleFlags(MethodReference method, RuleSnapshot snapshot) {
        if (method == null || snapshot == null || snapshot.isEmpty()) {
            return 0;
        }
        String cls = safe(method.getClassReference() == null ? null : method.getClassReference().getName()).replace('.', '/');
        String name = safe(method.getName());
        String desc = safe(method.getDesc());
        if (cls.isBlank() || name.isBlank() || desc.isBlank()) {
            return 0;
        }

        int flags = 0;
        Integer exact = snapshot.modelFlags().get(new MethodRuleKey(cls, name, desc));
        if (exact != null) {
            flags |= exact;
        }
        Integer wildcard = snapshot.modelFlags().get(new MethodRuleKey(cls, name, "*"));
        if (wildcard != null) {
            flags |= wildcard;
        }

        Set<AnnoReference> annos = method.getAnnotations();
        if (annos != null && !annos.isEmpty() && !snapshot.sourceAnnotations().isEmpty()) {
            for (AnnoReference anno : annos) {
                String normalized = normalizeAnnotationName(anno == null ? null : anno.getAnnoName());
                if (normalized.isBlank() || !snapshot.sourceAnnotations().contains(normalized)) {
                    continue;
                }
                flags |= GraphNode.SOURCE_FLAG_ANNOTATION;
                if (isWebAnnotation(normalized)) {
                    flags |= GraphNode.SOURCE_FLAG_WEB;
                }
                if (isRpcAnnotation(normalized)) {
                    flags |= GraphNode.SOURCE_FLAG_RPC;
                }
            }
        }

        if (flags != 0) {
            flags |= GraphNode.SOURCE_FLAG_ANY;
        }
        return flags;
    }

    public static int resolveCurrentSourceFlags(String className,
                                                String methodName,
                                                String methodDesc,
                                                Integer jarId) {
        return resolveCurrentSourceFlags(className, methodName, methodDesc, jarId, snapshotCurrentRules());
    }

    public static int resolveCurrentSourceFlags(String className,
                                                String methodName,
                                                String methodDesc,
                                                Integer jarId,
                                                RuleSnapshot snapshot) {
        String cls = safe(className).replace('.', '/');
        String name = safe(methodName);
        String desc = safe(methodDesc);
        int normalizedJarId = jarId == null ? -1 : jarId;
        if (cls.isBlank() || name.isBlank() || desc.isBlank()) {
            return 0;
        }

        int flags = 0;
        BiFunction<String, Integer, ClassReference> classResolver = (owner, ownerJarId) -> resolveCurrentClass(owner, ownerJarId);
        List<MethodReference> methods = DatabaseManager.getMethodReferencesByClass(cls);
        boolean matchedMethod = false;
        if (methods != null && !methods.isEmpty()) {
            for (MethodReference method : methods) {
                if (!matchesMethod(method, cls, name, desc, normalizedJarId)) {
                    continue;
                }
                matchedMethod = true;
                flags |= resolveDynamicMethodFlags(method, snapshot, classResolver, cls);
            }
        }
        if (!matchedMethod) {
            flags |= resolveRuleFlags(cls, name, desc, snapshot);
        }
        int explicitFlags = resolveExplicitProjectFlags(cls, name, desc);
        if (explicitFlags != 0) {
            flags |= explicitFlags;
        }
        if (flags != 0) {
            flags |= GraphNode.SOURCE_FLAG_ANY;
        }
        return flags;
    }

    public static int resolveCurrentSourceFlags(MethodReference method, RuleSnapshot snapshot) {
        if (method == null) {
            return 0;
        }
        String cls = safe(method.getClassReference() == null ? null : method.getClassReference().getName()).replace('.', '/');
        String name = safe(method.getName());
        String desc = safe(method.getDesc());
        if (cls.isBlank() || name.isBlank() || desc.isBlank()) {
            return 0;
        }
        BiFunction<String, Integer, ClassReference> classResolver = (owner, ownerJarId) -> resolveCurrentClass(owner, ownerJarId);
        int flags = resolveDynamicMethodFlags(method, snapshot, classResolver, cls);
        int explicitFlags = resolveExplicitProjectFlags(cls, name, desc);
        if (explicitFlags != 0) {
            flags |= explicitFlags;
        }
        if (flags != 0) {
            flags |= GraphNode.SOURCE_FLAG_ANY;
        }
        return flags;
    }

    public static List<String> describeFlags(int flags) {
        if (flags <= 0) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        if ((flags & GraphNode.SOURCE_FLAG_ANY) != 0) {
            out.add("Source");
        }
        if ((flags & GraphNode.SOURCE_FLAG_WEB) != 0) {
            out.add("Web");
        }
        if ((flags & GraphNode.SOURCE_FLAG_MODEL) != 0) {
            out.add("Model");
        }
        if ((flags & GraphNode.SOURCE_FLAG_ANNOTATION) != 0) {
            out.add("Annotation");
        }
        if ((flags & GraphNode.SOURCE_FLAG_RPC) != 0) {
            out.add("Rpc");
        }
        return List.copyOf(out);
    }

    public static String normalizeSourceModelDesc(String desc) {
        String value = safe(desc);
        if (value.isBlank() || "null".equalsIgnoreCase(value)) {
            return "*";
        }
        return value;
    }

    public static String normalizeAnnotationName(String raw) {
        String value = safe(raw);
        if (value.isBlank()) {
            return "";
        }
        if (value.charAt(0) == '@') {
            value = value.substring(1);
        }
        value = value.replace('.', '/');
        if (!value.startsWith("L")) {
            value = "L" + value;
        }
        if (!value.endsWith(";")) {
            value = value + ";";
        }
        return value;
    }

    public static boolean isWebSourceKind(String kind) {
        String value = safe(kind).toLowerCase(Locale.ROOT);
        return value.contains("web")
                || value.contains("http")
                || value.contains("rest")
                || value.contains("servlet")
                || value.contains("controller");
    }

    public static boolean isRpcSourceKind(String kind) {
        String value = safe(kind).toLowerCase(Locale.ROOT);
        return value.contains("rpc")
                || value.contains("grpc")
                || value.contains("dubbo")
                || value.contains("webservice");
    }

    public static boolean isServletEntry(String methodName, String methodDesc) {
        String name = safe(methodName);
        String desc = safe(methodDesc);
        if (name.isBlank() || desc.isBlank()) {
            return false;
        }
        if (!("doGet".equals(name) || "doPost".equals(name) || "doPut".equals(name)
                || "doDelete".equals(name) || "doHead".equals(name)
                || "doOptions".equals(name) || "doTrace".equals(name)
                || "service".equals(name))) {
            return false;
        }
        return "(Ljavax/servlet/http/HttpServletRequest;Ljavax/servlet/http/HttpServletResponse;)V".equals(desc)
                || "(Ljakarta/servlet/http/HttpServletRequest;Ljakarta/servlet/http/HttpServletResponse;)V".equals(desc)
                || "(Ljavax/servlet/ServletRequest;Ljavax/servlet/ServletResponse;)V".equals(desc)
                || "(Ljakarta/servlet/ServletRequest;Ljakarta/servlet/ServletResponse;)V".equals(desc);
    }

    private static boolean isWebAnnotation(String annotation) {
        String value = safe(annotation).toLowerCase(Locale.ROOT);
        return value.contains("/springframework/web/")
                || value.contains("/ws/rs/")
                || value.contains("/servlet/");
    }

    private static boolean isRpcAnnotation(String annotation) {
        String value = safe(annotation).toLowerCase(Locale.ROOT);
        return value.contains("/dubbo/")
                || value.contains("/grpc/")
                || value.contains("/jws/webservice;");
    }

    public static int resolveFrameworkFlags(MethodReference method,
                                            ClassReference ownerClass,
                                            BiFunction<String, Integer, ClassReference> classResolver) {
        if (method == null) {
            return 0;
        }
        String methodName = safe(method.getName());
        String methodDesc = safe(method.getDesc());
        if (methodName.isBlank() || methodDesc.isBlank()) {
            return 0;
        }
        int flags = 0;
        if (isJspServiceEntry(methodName, methodDesc)) {
            flags |= GraphNode.SOURCE_FLAG_WEB;
        }
        if (ownerClass == null) {
            return flags;
        }
        if (isRpcServiceClass(ownerClass) && isRemoteServiceMethod(method)) {
            flags |= GraphNode.SOURCE_FLAG_RPC;
        }
        if (STRUTS_ENTRY_METHODS.contains(methodName)
                && matchesHierarchy(ownerClass, classResolver, STRUTS_ENTRY_TYPES)) {
            flags |= GraphNode.SOURCE_FLAG_WEB;
        }
        if (NETTY_HANDLER_METHODS.contains(methodName)
                && matchesHierarchy(ownerClass, classResolver, NETTY_HANDLER_TYPES)) {
            flags |= GraphNode.SOURCE_FLAG_WEB;
        }
        if ("decode".equals(methodName)
                && matchesHierarchy(ownerClass, classResolver, NETTY_DECODER_TYPES)) {
            flags |= GraphNode.SOURCE_FLAG_WEB;
        }
        return flags;
    }

    private static int resolveDynamicMethodFlags(MethodReference method,
                                                 RuleSnapshot snapshot,
                                                 BiFunction<String, Integer, ClassReference> classResolver,
                                                 String fallbackClassName) {
        if (method == null) {
            return 0;
        }
        int flags = resolveRuleFlags(method, snapshot);
        String owner = method.getClassReference() == null ? fallbackClassName : method.getClassReference().getName();
        flags |= resolveFrameworkFlags(method, resolveCurrentClass(owner, method.getJarId()), classResolver);
        return flags;
    }

    private static int resolveRuleFlags(String className,
                                        String methodName,
                                        String methodDesc,
                                        RuleSnapshot snapshot) {
        if (snapshot == null || snapshot.isEmpty()) {
            return 0;
        }
        String cls = safe(className).replace('.', '/');
        String name = safe(methodName);
        String desc = safe(methodDesc);
        if (cls.isBlank() || name.isBlank() || desc.isBlank()) {
            return 0;
        }
        int flags = 0;
        Integer exact = snapshot.modelFlags().get(new MethodRuleKey(cls, name, desc));
        if (exact != null) {
            flags |= exact;
        }
        Integer wildcard = snapshot.modelFlags().get(new MethodRuleKey(cls, name, "*"));
        if (wildcard != null) {
            flags |= wildcard;
        }
        if (flags != 0) {
            flags |= GraphNode.SOURCE_FLAG_ANY;
        }
        return flags;
    }

    private static int resolveExplicitProjectFlags(String className,
                                                   String methodName,
                                                   String methodDesc) {
        int flags = 0;
        if (matchesSpringControllerMapping(className, methodName, methodDesc)) {
            flags |= GraphNode.SOURCE_FLAG_WEB;
        }
        if (matchesWebEntryClass(DatabaseManager.getServlets(), className, methodName, methodDesc, WebEntryMethods.SERVLET_ENTRY_METHODS)) {
            flags |= GraphNode.SOURCE_FLAG_WEB;
        }
        if (matchesWebEntryClass(DatabaseManager.getFilters(), className, methodName, methodDesc, WebEntryMethods.FILTER_ENTRY_METHODS)) {
            flags |= GraphNode.SOURCE_FLAG_WEB;
        }
        if (matchesWebEntryClass(DatabaseManager.getSpringInterceptors(), className, methodName, methodDesc, WebEntryMethods.INTERCEPTOR_ENTRY_METHODS)) {
            flags |= GraphNode.SOURCE_FLAG_WEB;
        }
        if (matchesWebEntryClass(DatabaseManager.getListeners(), className, methodName, methodDesc, WebEntryMethods.LISTENER_ENTRY_METHODS)) {
            flags |= GraphNode.SOURCE_FLAG_WEB;
        }
        if (isServletEntry(methodName, methodDesc)) {
            flags |= GraphNode.SOURCE_FLAG_WEB;
        }
        flags |= resolveExplicitResourceFlags(className, methodName, methodDesc);
        return flags;
    }

    private static boolean matchesMethod(MethodReference method,
                                         String className,
                                         String methodName,
                                         String methodDesc,
                                         int jarId) {
        if (method == null) {
            return false;
        }
        String owner = safe(method.getClassReference() == null ? null : method.getClassReference().getName()).replace('.', '/');
        if (!className.equals(owner)) {
            return false;
        }
        if (!methodName.equals(safe(method.getName())) || !methodDesc.equals(safe(method.getDesc()))) {
            return false;
        }
        if (jarId < 0) {
            return true;
        }
        Integer methodJarId = method.getJarId();
        if (methodJarId == null || methodJarId < 0) {
            return true;
        }
        return jarId == methodJarId;
    }

    private static boolean matchesSpringControllerMapping(String className,
                                                          String methodName,
                                                          String methodDesc) {
        List<SpringController> controllers = DatabaseManager.getSpringControllers();
        if (controllers == null || controllers.isEmpty()) {
            return false;
        }
        for (SpringController controller : controllers) {
            if (controller == null || controller.getMappings() == null) {
                continue;
            }
            for (SpringMapping mapping : controller.getMappings()) {
                MethodReference.Handle handle = mapping == null ? null : mapping.getMethodName();
                String owner = safe(handle == null || handle.getClassReference() == null ? null : handle.getClassReference().getName()).replace('.', '/');
                if (!className.equals(owner)) {
                    continue;
                }
                if (methodName.equals(safe(handle.getName())) && methodDesc.equals(safe(handle.getDesc()))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean matchesWebEntryClass(Set<String> classes,
                                                String className,
                                                String methodName,
                                                String methodDesc,
                                                Set<WebEntryMethodSpec> specs) {
        if (classes == null || classes.isEmpty() || specs == null || specs.isEmpty()) {
            return false;
        }
        boolean classMatched = false;
        for (String candidate : classes) {
            if (className.equals(safe(candidate).replace('.', '/'))) {
                classMatched = true;
                break;
            }
        }
        if (!classMatched) {
            return false;
        }
        for (WebEntryMethodSpec spec : specs) {
            if (spec == null) {
                continue;
            }
            if (methodName.equals(safe(spec.name())) && methodDesc.equals(safe(spec.desc()))) {
                return true;
            }
        }
        return false;
    }

    private static int resolveExplicitResourceFlags(String className,
                                                    String methodName,
                                                    String methodDesc) {
        FrameworkXmlSourceIndex.Result index = FrameworkXmlSourceIndex.currentProject();
        if (index == null || index.isEmpty()) {
            return 0;
        }
        int flags = 0;
        if (matchesWebEntryClass(index.servletClasses(), className, methodName, methodDesc, WebEntryMethods.SERVLET_ENTRY_METHODS)) {
            flags |= GraphNode.SOURCE_FLAG_WEB;
        }
        if (matchesWebEntryClass(index.filterClasses(), className, methodName, methodDesc, WebEntryMethods.FILTER_ENTRY_METHODS)) {
            flags |= GraphNode.SOURCE_FLAG_WEB;
        }
        if (matchesWebEntryClass(index.listenerClasses(), className, methodName, methodDesc, WebEntryMethods.LISTENER_ENTRY_METHODS)) {
            flags |= GraphNode.SOURCE_FLAG_WEB;
        }
        flags |= index.resolveFlags(className, methodName, methodDesc);
        return flags;
    }

    private static boolean isJspServiceEntry(String methodName, String methodDesc) {
        return "_jspService".equals(safe(methodName))
                && ("(Ljavax/servlet/http/HttpServletRequest;Ljavax/servlet/http/HttpServletResponse;)V".equals(safe(methodDesc))
                || "(Ljakarta/servlet/http/HttpServletRequest;Ljakarta/servlet/http/HttpServletResponse;)V".equals(safe(methodDesc)));
    }

    private static boolean isRpcServiceClass(ClassReference ownerClass) {
        Set<AnnoReference> annotations = ownerClass == null ? null : ownerClass.getAnnotations();
        if (annotations == null || annotations.isEmpty()) {
            return false;
        }
        for (AnnoReference annotation : annotations) {
            String normalized = normalizeAnnotationName(annotation == null ? null : annotation.getAnnoName());
            if (RPC_SERVICE_ANNOTATIONS.contains(normalized)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRemoteServiceMethod(MethodReference method) {
        if (method == null) {
            return false;
        }
        String name = safe(method.getName());
        if (name.isBlank() || "<init>".equals(name) || "<clinit>".equals(name)) {
            return false;
        }
        int access = method.getAccess();
        if ((access & Opcodes.ACC_PUBLIC) == 0) {
            return false;
        }
        return (access & Opcodes.ACC_STATIC) == 0;
    }

    private static boolean matchesHierarchy(ClassReference ownerClass,
                                            BiFunction<String, Integer, ClassReference> classResolver,
                                            Set<String> targets) {
        if (ownerClass == null || targets == null || targets.isEmpty()) {
            return false;
        }
        ArrayDeque<ClassReference> queue = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();
        queue.add(ownerClass);
        while (!queue.isEmpty()) {
            ClassReference current = queue.poll();
            if (current == null) {
                continue;
            }
            String currentName = safe(current.getName()).replace('.', '/');
            if (currentName.isBlank() || !visited.add(currentName)) {
                continue;
            }
            if (targets.contains(currentName)) {
                return true;
            }
            enqueueHierarchyNode(queue, classResolver, current.getSuperClass(), current.getJarId(), targets);
            List<String> interfaces = current.getInterfaces();
            if (interfaces == null || interfaces.isEmpty()) {
                continue;
            }
            for (String iface : interfaces) {
                enqueueHierarchyNode(queue, classResolver, iface, current.getJarId(), targets);
            }
        }
        return false;
    }

    private static void enqueueHierarchyNode(ArrayDeque<ClassReference> queue,
                                             BiFunction<String, Integer, ClassReference> classResolver,
                                             String rawName,
                                             Integer jarId,
                                             Set<String> targets) {
        String normalized = safe(rawName).replace('.', '/');
        if (normalized.isBlank()) {
            return;
        }
        if (targets.contains(normalized)) {
            queue.add(new ClassReference(
                    normalized,
                    "",
                    List.of(),
                    false,
                    List.of(),
                    new ArrayList<>(),
                    "",
                    jarId
            ));
            return;
        }
        if (classResolver == null) {
            return;
        }
        ClassReference next = classResolver.apply(normalized, jarId);
        if (next != null) {
            queue.add(next);
        }
    }

    private static ClassReference resolveCurrentClass(String className, Integer jarId) {
        String normalized = safe(className).replace('.', '/');
        if (normalized.isBlank()) {
            return null;
        }
        ClassReference exact = DatabaseManager.getClassReferenceByName(normalized, jarId);
        if (exact != null) {
            return exact;
        }
        List<ClassReference> candidates = DatabaseManager.getClassReferencesByName(normalized);
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        return candidates.get(0);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public record RuleSnapshot(Map<MethodRuleKey, Integer> modelFlags, Set<String> sourceAnnotations) {
        static RuleSnapshot from(List<SourceModel> sourceModels, List<String> sourceAnnotations) {
            Map<MethodRuleKey, Integer> modelFlags = new HashMap<>();
            if (sourceModels != null) {
                for (SourceModel model : sourceModels) {
                    if (model == null) {
                        continue;
                    }
                    String cls = safe(model.getClassName()).replace('.', '/');
                    String method = safe(model.getMethodName());
                    String desc = normalizeSourceModelDesc(model.getMethodDesc());
                    if (cls.isBlank() || method.isBlank()) {
                        continue;
                    }
                    int flags = GraphNode.SOURCE_FLAG_MODEL;
                    if (isWebSourceKind(model.getKind())) {
                        flags |= GraphNode.SOURCE_FLAG_WEB;
                    }
                    if (isRpcSourceKind(model.getKind())) {
                        flags |= GraphNode.SOURCE_FLAG_RPC;
                    }
                    modelFlags.merge(new MethodRuleKey(cls, method, desc), flags, (left, right) -> left | right);
                }
            }

            Set<String> normalizedAnnotations = new HashSet<>();
            if (sourceAnnotations != null) {
                for (String raw : sourceAnnotations) {
                    String normalized = normalizeAnnotationName(raw);
                    if (!normalized.isBlank()) {
                        normalizedAnnotations.add(normalized);
                    }
                }
            }
            return new RuleSnapshot(
                    modelFlags.isEmpty() ? Collections.emptyMap() : Map.copyOf(modelFlags),
                    normalizedAnnotations.isEmpty() ? Collections.emptySet() : Set.copyOf(normalizedAnnotations)
            );
        }

        public boolean isEmpty() {
            return modelFlags.isEmpty() && sourceAnnotations.isEmpty();
        }
    }

    public record MethodRuleKey(String className, String methodName, String methodDesc) {
    }

}
