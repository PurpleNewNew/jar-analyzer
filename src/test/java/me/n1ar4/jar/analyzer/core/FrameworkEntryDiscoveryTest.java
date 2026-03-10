package me.n1ar4.jar.analyzer.core;

import me.n1ar4.jar.analyzer.core.reference.AnnoReference;
import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.graph.store.GraphNode;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FrameworkEntryDiscoveryTest {
    @Test
    void shouldDiscoverRpcAndWebFrameworkEntryMethods() {
        ClassReference dubboService = classRef(
                "demo/dubbo/UserServiceImpl",
                "java/lang/Object",
                List.of(),
                Set.of("Lorg/apache/dubbo/config/annotation/DubboService;")
        );
        ClassReference strutsAction = classRef(
                "demo/struts/LoginAction",
                "com/opensymphony/xwork2/ActionSupport",
                List.of(),
                Set.of()
        );
        ClassReference nettyHandler = classRef(
                "demo/netty/AuthHandler",
                "java/lang/Object",
                List.of("io/netty/channel/ChannelInboundHandler"),
                Set.of()
        );
        ClassReference jspClass = classRef(
                "org/apache/jsp/login_jsp",
                "org/apache/jasper/runtime/HttpJspBase",
                List.of(),
                Set.of()
        );

        Map<ClassReference.Handle, ClassReference> classMap = new LinkedHashMap<>();
        classMap.put(dubboService.getHandle(), dubboService);
        classMap.put(strutsAction.getHandle(), strutsAction);
        classMap.put(nettyHandler.getHandle(), nettyHandler);
        classMap.put(jspClass.getHandle(), jspClass);

        MethodReference dubboMethod = method(dubboService.getHandle(), "sayHello", "(Ljava/lang/String;)Ljava/lang/String;");
        MethodReference strutsMethod = method(strutsAction.getHandle(), "execute", "()Ljava/lang/String;");
        MethodReference nettyMethod = method(nettyHandler.getHandle(), "channelRead", "(Lio/netty/channel/ChannelHandlerContext;Ljava/lang/Object;)V");
        MethodReference jspMethod = method(jspClass.getHandle(), "_jspService", "(Ljavax/servlet/http/HttpServletRequest;Ljavax/servlet/http/HttpServletResponse;)V");

        Map<MethodReference.Handle, MethodReference> methodMap = new LinkedHashMap<>();
        methodMap.put(dubboMethod.getHandle(), dubboMethod);
        methodMap.put(strutsMethod.getHandle(), strutsMethod);
        methodMap.put(nettyMethod.getHandle(), nettyMethod);
        methodMap.put(jspMethod.getHandle(), jspMethod);

        FrameworkEntryDiscovery.Result result = FrameworkEntryDiscovery.discover(List.of(), classMap, methodMap);

        assertEquals(4, result.explicitSourceMethodFlags().size());
        assertTrue((result.explicitSourceMethodFlags().get(dubboMethod.getHandle()) & GraphNode.SOURCE_FLAG_RPC) != 0);
        assertTrue((result.explicitSourceMethodFlags().get(strutsMethod.getHandle()) & GraphNode.SOURCE_FLAG_WEB) != 0);
        assertTrue((result.explicitSourceMethodFlags().get(nettyMethod.getHandle()) & GraphNode.SOURCE_FLAG_WEB) != 0);
        assertTrue((result.explicitSourceMethodFlags().get(jspMethod.getHandle()) & GraphNode.SOURCE_FLAG_WEB) != 0);
    }

    private static ClassReference classRef(String name,
                                           String superClass,
                                           List<String> interfaces,
                                           Set<String> annotations) {
        return new ClassReference(
                61,
                Opcodes.ACC_PUBLIC,
                name,
                superClass,
                interfaces,
                false,
                List.of(),
                annotations.stream().map(AnnoReference::new).collect(java.util.stream.Collectors.toSet()),
                "app.jar",
                1
        );
    }

    private static MethodReference method(ClassReference.Handle owner, String name, String desc) {
        return new MethodReference(
                owner,
                name,
                desc,
                false,
                Set.of(),
                Opcodes.ACC_PUBLIC,
                1,
                "app.jar",
                1
        );
    }
}
