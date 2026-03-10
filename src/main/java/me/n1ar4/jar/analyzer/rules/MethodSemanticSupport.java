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
import me.n1ar4.jar.analyzer.core.WebEntryMethodSpec;
import me.n1ar4.jar.analyzer.core.WebEntryMethods;
import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.entity.ResourceEntity;
import me.n1ar4.jar.analyzer.graph.store.GraphNode;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import org.objectweb.asm.Opcodes;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

public final class MethodSemanticSupport {
    private static final Logger logger = LogManager.getLogger();

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
            "messageReceived",
            "decode"
    );
    private static final Set<String> NETTY_DECODER_TYPES = Set.of(
            "io/netty/handler/codec/ByteToMessageDecoder",
            "io/netty/handler/codec/MessageToMessageDecoder",
            "org/jboss/netty/handler/codec/frame/FrameDecoder"
    );
    private static final Set<String> SERIALIZABLE_TYPES = Set.of(
            "java/io/Serializable",
            "java/io/Externalizable"
    );
    private static final Set<String> INVOCATION_HANDLER_TYPES = Set.of(
            "java/lang/reflect/InvocationHandler"
    );
    private static final Set<String> COMPARATOR_TYPES = Set.of(
            "java/util/Comparator"
    );
    private static final Set<String> COMPARABLE_TYPES = Set.of(
            "java/lang/Comparable"
    );
    private static final Set<String> TRANSFORMER_TYPES = Set.of(
            "org/apache/commons/collections/Transformer",
            "org/apache/commons/collections4/Transformer"
    );
    private static final Set<String> OBJECT_INPUT_VALIDATION_TYPES = Set.of(
            "java/io/ObjectInputValidation"
    );
    private static final Set<String> COLLECTION_CONTAINER_TYPES = Set.of(
            "java/util/PriorityQueue",
            "java/util/PriorityBlockingQueue",
            "java/util/TreeMap",
            "java/util/TreeSet",
            "java/util/HashMap",
            "java/util/LinkedHashMap",
            "java/util/HashSet",
            "java/util/LinkedHashSet",
            "java/util/Hashtable",
            "java/beans/beancontext/BeanContextSupport",
            "javax/management/BadAttributeValueExpException"
    );

    private MethodSemanticSupport() {
    }

    public static Map<MethodReference.Handle, Integer> derive(Collection<MethodReference> methods,
                                                              Collection<ClassReference> classes,
                                                              Collection<SpringController> controllers,
                                                              Collection<String> springInterceptors,
                                                              Collection<String> servlets,
                                                              Collection<String> filters,
                                                              Collection<String> listeners,
                                                              Collection<ResourceEntity> resources,
                                                              Map<MethodReference.Handle, Integer> explicitSourceMethodFlags) {
        if (methods == null || methods.isEmpty()) {
            return Map.of();
        }
        Map<ClassReference.Handle, ClassReference> classIndex = new HashMap<>();
        Map<String, ClassReference> classByName = new HashMap<>();
        if (classes != null) {
            for (ClassReference classReference : classes) {
                if (classReference == null || classReference.getName() == null || classReference.getName().isBlank()) {
                    continue;
                }
                classIndex.put(classReference.getHandle(), classReference);
                classByName.putIfAbsent(normalizeClassName(classReference.getName()), classReference);
            }
        }
        BiFunction<String, Integer, ClassReference> resolver =
                (className, jarId) -> resolveClassReference(className, jarId, classIndex, classByName);
        MyBatisMapperXmlIndex.Result myBatisIndex = MyBatisMapperXmlIndex.fromResources(resources);
        Set<String> springMappings = collectSpringMappingSignatures(controllers);
        Set<String> servletCallbacks = collectWebCallbackSignatures(servlets, WebEntryMethods.SERVLET_ENTRY_METHODS);
        servletCallbacks.addAll(collectWebCallbackSignatures(filters, WebEntryMethods.FILTER_ENTRY_METHODS));
        servletCallbacks.addAll(collectWebCallbackSignatures(listeners, WebEntryMethods.LISTENER_ENTRY_METHODS));
        servletCallbacks.addAll(collectWebCallbackSignatures(springInterceptors, WebEntryMethods.INTERCEPTOR_ENTRY_METHODS));
        Map<MethodReference.Handle, Integer> explicitFlags = explicitSourceMethodFlags == null
                ? Map.of()
                : explicitSourceMethodFlags;

        Map<MethodReference.Handle, Integer> out = new LinkedHashMap<>();
        for (MethodReference method : methods) {
            if (method == null || method.getClassReference() == null) {
                continue;
            }
            String className = normalizeClassName(method.getClassReference().getName());
            String methodName = safe(method.getName());
            String methodDesc = safe(method.getDesc());
            if (className.isBlank() || methodName.isBlank() || methodDesc.isBlank()) {
                continue;
            }
            ClassReference ownerClass = resolveClassReference(className, method.getJarId(), classIndex, classByName);
            int flags = 0;
            int sourceFlags = explicitFlags.getOrDefault(method.getHandle(), 0);
            if (sourceFlags != 0) {
                flags |= MethodSemanticFlags.ENTRY;
                if ((sourceFlags & GraphNode.SOURCE_FLAG_WEB) != 0) {
                    flags |= MethodSemanticFlags.WEB_ENTRY;
                }
                if ((sourceFlags & GraphNode.SOURCE_FLAG_RPC) != 0) {
                    flags |= MethodSemanticFlags.RPC_ENTRY;
                }
            }
            String signatureKey = signatureKey(className, methodName, methodDesc);
            if (springMappings.contains(signatureKey)) {
                flags |= MethodSemanticFlags.ENTRY | MethodSemanticFlags.WEB_ENTRY | MethodSemanticFlags.SPRING_ENDPOINT;
            }
            if (servletCallbacks.contains(signatureKey)) {
                flags |= MethodSemanticFlags.ENTRY | MethodSemanticFlags.WEB_ENTRY | MethodSemanticFlags.SERVLET_CALLBACK;
            }
            if (isJspServiceEntry(methodName, methodDesc)) {
                flags |= MethodSemanticFlags.ENTRY | MethodSemanticFlags.WEB_ENTRY | MethodSemanticFlags.JSP_ENDPOINT;
            }
            if (ownerClass != null) {
                if (isRpcServiceClass(ownerClass) && isRemoteServiceMethod(method)) {
                    flags |= MethodSemanticFlags.ENTRY | MethodSemanticFlags.RPC_ENTRY;
                }
                if (STRUTS_ENTRY_METHODS.contains(methodName)
                        && matchesHierarchy(ownerClass, resolver, STRUTS_ENTRY_TYPES)) {
                    flags |= MethodSemanticFlags.ENTRY | MethodSemanticFlags.WEB_ENTRY | MethodSemanticFlags.STRUTS_ACTION;
                }
                if (NETTY_HANDLER_METHODS.contains(methodName)
                        && (matchesHierarchy(ownerClass, resolver, NETTY_HANDLER_TYPES)
                        || ("decode".equals(methodName) && matchesHierarchy(ownerClass, resolver, NETTY_DECODER_TYPES)))) {
                    flags |= MethodSemanticFlags.ENTRY | MethodSemanticFlags.WEB_ENTRY | MethodSemanticFlags.NETTY_HANDLER;
                }
                if (matchesHierarchy(ownerClass, resolver, SERIALIZABLE_TYPES)) {
                    flags |= MethodSemanticFlags.SERIALIZABLE_OWNER;
                }
                if (isCollectionContainer(ownerClass, resolver)) {
                    flags |= MethodSemanticFlags.COLLECTION_CONTAINER;
                }
                if (isDeserializationCallback(methodName, methodDesc)) {
                    flags |= MethodSemanticFlags.DESERIALIZATION_CALLBACK;
                }
                if ("validateObject".equals(methodName)
                        && "()V".equals(methodDesc)
                        && matchesHierarchy(ownerClass, resolver, OBJECT_INPUT_VALIDATION_TYPES)) {
                    flags |= MethodSemanticFlags.DESERIALIZATION_CALLBACK;
                }
                if ("invoke".equals(methodName)
                        && "(Ljava/lang/Object;Ljava/lang/reflect/Method;[Ljava/lang/Object;)Ljava/lang/Object;".equals(methodDesc)
                        && matchesHierarchy(ownerClass, resolver, INVOCATION_HANDLER_TYPES)) {
                    flags |= MethodSemanticFlags.INVOCATION_HANDLER;
                }
                if ("compare".equals(methodName)
                        && "(Ljava/lang/Object;Ljava/lang/Object;)I".equals(methodDesc)
                        && matchesHierarchy(ownerClass, resolver, COMPARATOR_TYPES)) {
                    flags |= MethodSemanticFlags.COMPARATOR_CALLBACK;
                }
                if ("transform".equals(methodName)
                        && "(Ljava/lang/Object;)Ljava/lang/Object;".equals(methodDesc)
                        && matchesHierarchy(ownerClass, resolver, TRANSFORMER_TYPES)) {
                    flags |= MethodSemanticFlags.TRANSFORMER_CALLBACK;
                }
                if ("compareTo".equals(methodName)
                        && "(Ljava/lang/Object;)I".equals(methodDesc)
                        && matchesHierarchy(ownerClass, resolver, COMPARABLE_TYPES)) {
                    flags |= MethodSemanticFlags.COMPARABLE_CALLBACK;
                }
            }
            if ("toString".equals(methodName)
                    && "()Ljava/lang/String;".equals(methodDesc)) {
                flags |= MethodSemanticFlags.TOSTRING_TRIGGER;
            }
            if ("hashCode".equals(methodName)
                    && "()I".equals(methodDesc)) {
                flags |= MethodSemanticFlags.HASHCODE_TRIGGER;
            }
            if ("equals".equals(methodName)
                    && "(Ljava/lang/Object;)Z".equals(methodDesc)) {
                flags |= MethodSemanticFlags.EQUALS_TRIGGER;
            }
            if (myBatisIndex.resolve(className, methodName, methodDesc) != null) {
                flags |= MethodSemanticFlags.MYBATIS_DYNAMIC_SQL;
            }
            if (flags != 0) {
                out.put(method.getHandle(), flags);
            }
        }
        logger.info("method semantic derive: methods={} tagged={} springMappings={} servletCallbacks={} mybatisDynamic={}",
                methods.size(),
                out.size(),
                springMappings.size(),
                servletCallbacks.size(),
                myBatisIndex.sinkPatterns().size());
        return out.isEmpty() ? Map.of() : Map.copyOf(out);
    }

    private static Set<String> collectSpringMappingSignatures(Collection<SpringController> controllers) {
        Set<String> out = new HashSet<>();
        if (controllers == null || controllers.isEmpty()) {
            return out;
        }
        for (SpringController controller : controllers) {
            if (controller == null || controller.getMappings() == null) {
                continue;
            }
            for (SpringMapping mapping : controller.getMappings()) {
                MethodReference.Handle handle = mapping == null ? null : mapping.getMethodName();
                String className = normalizeClassName(handle == null || handle.getClassReference() == null ? null : handle.getClassReference().getName());
                String methodName = safe(handle == null ? null : handle.getName());
                String methodDesc = safe(handle == null ? null : handle.getDesc());
                String key = signatureKey(className, methodName, methodDesc);
                if (!key.isBlank()) {
                    out.add(key);
                }
            }
        }
        return out;
    }

    private static Set<String> collectWebCallbackSignatures(Collection<String> classes,
                                                            Set<WebEntryMethodSpec> specs) {
        Set<String> out = new HashSet<>();
        if (classes == null || classes.isEmpty() || specs == null || specs.isEmpty()) {
            return out;
        }
        for (String rawClass : classes) {
            String className = normalizeClassName(rawClass);
            if (className.isBlank()) {
                continue;
            }
            for (WebEntryMethodSpec spec : specs) {
                if (spec == null) {
                    continue;
                }
                String key = signatureKey(className, spec.name(), spec.desc());
                if (!key.isBlank()) {
                    out.add(key);
                }
            }
        }
        return out;
    }

    private static ClassReference resolveClassReference(String className,
                                                        Integer jarId,
                                                        Map<ClassReference.Handle, ClassReference> classIndex,
                                                        Map<String, ClassReference> classByName) {
        String normalized = normalizeClassName(className);
        if (normalized.isBlank()) {
            return null;
        }
        ClassReference direct = classIndex.get(new ClassReference.Handle(normalized, jarId));
        if (direct != null) {
            return direct;
        }
        return classByName.get(normalized);
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
            String currentName = normalizeClassName(current.getName());
            if (currentName.isBlank() || !visited.add(currentName)) {
                continue;
            }
            if (targets.contains(currentName)) {
                return true;
            }
            enqueue(queue, classResolver, current.getSuperClass(), current.getJarId(), targets);
            List<String> interfaces = current.getInterfaces();
            if (interfaces == null || interfaces.isEmpty()) {
                continue;
            }
            for (String iface : interfaces) {
                enqueue(queue, classResolver, iface, current.getJarId(), targets);
            }
        }
        return false;
    }

    private static boolean isCollectionContainer(ClassReference ownerClass,
                                                 BiFunction<String, Integer, ClassReference> classResolver) {
        if (ownerClass == null) {
            return false;
        }
        String className = normalizeClassName(ownerClass.getName());
        if (COLLECTION_CONTAINER_TYPES.contains(className)) {
            return true;
        }
        return matchesHierarchy(ownerClass, classResolver, COLLECTION_CONTAINER_TYPES);
    }

    private static void enqueue(ArrayDeque<ClassReference> queue,
                                BiFunction<String, Integer, ClassReference> classResolver,
                                String rawName,
                                Integer jarId,
                                Set<String> targets) {
        String normalized = normalizeClassName(rawName);
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
        ClassReference resolved = classResolver.apply(normalized, jarId);
        if (resolved != null) {
            queue.add(resolved);
        }
    }

    private static boolean isRpcServiceClass(ClassReference ownerClass) {
        if (ownerClass == null || ownerClass.getAnnotations() == null || ownerClass.getAnnotations().isEmpty()) {
            return false;
        }
        for (var annotation : ownerClass.getAnnotations()) {
            String normalized = SourceRuleSupport.normalizeAnnotationName(annotation == null ? null : annotation.getAnnoName());
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
        return (access & Opcodes.ACC_PUBLIC) != 0
                && (access & Opcodes.ACC_STATIC) == 0;
    }

    private static boolean isJspServiceEntry(String methodName, String methodDesc) {
        return "_jspService".equals(safe(methodName))
                && ("(Ljavax/servlet/http/HttpServletRequest;Ljavax/servlet/http/HttpServletResponse;)V".equals(safe(methodDesc))
                || "(Ljakarta/servlet/http/HttpServletRequest;Ljakarta/servlet/http/HttpServletResponse;)V".equals(safe(methodDesc)));
    }

    private static boolean isDeserializationCallback(String methodName, String methodDesc) {
        String name = safe(methodName);
        String desc = safe(methodDesc);
        if ("readObject".equals(name)) {
            return "(Ljava/io/ObjectInputStream;)V".equals(desc);
        }
        if ("readExternal".equals(name)) {
            return "(Ljava/io/ObjectInput;)V".equals(desc);
        }
        if ("readResolve".equals(name)) {
            return "()Ljava/lang/Object;".equals(desc);
        }
        if ("readObjectNoData".equals(name)) {
            return "()V".equals(desc);
        }
        return false;
    }

    private static String signatureKey(String className, String methodName, String methodDesc) {
        String owner = normalizeClassName(className);
        String name = safe(methodName);
        String desc = safe(methodDesc);
        if (owner.isBlank() || name.isBlank() || desc.isBlank()) {
            return "";
        }
        return owner + "#" + name + "#" + desc;
    }

    private static String normalizeClassName(String raw) {
        return safe(raw).replace('.', '/');
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
