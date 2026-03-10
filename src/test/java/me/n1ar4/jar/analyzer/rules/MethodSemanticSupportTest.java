package me.n1ar4.jar.analyzer.rules;

import me.n1ar4.jar.analyzer.analyze.spring.SpringController;
import me.n1ar4.jar.analyzer.analyze.spring.SpringMapping;
import me.n1ar4.jar.analyzer.core.reference.AnnoReference;
import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.entity.ResourceEntity;
import me.n1ar4.jar.analyzer.graph.store.GraphNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MethodSemanticSupportTest {
    @TempDir
    Path tempDir;

    @Test
    void shouldDeriveFrameworkSemanticFlags() throws Exception {
        MethodReference springMethod = method("demo/web/UserController", "index", "()Ljava/lang/String;");
        MethodReference rpcMethod = method("demo/rpc/UserService", "list", "()V");
        MethodReference jspMethod = method(
                "org/apache/jsp/index_jsp",
                "_jspService",
                "(Ljavax/servlet/http/HttpServletRequest;Ljavax/servlet/http/HttpServletResponse;)V"
        );
        MethodReference strutsMethod = method("demo/struts/LoginAction", "execute", "()Ljava/lang/String;");
        MethodReference nettyMethod = method(
                "demo/netty/AppHandler",
                "channelRead",
                "(Lio/netty/channel/ChannelHandlerContext;Ljava/lang/Object;)V"
        );
        MethodReference mapperMethod = method("demo/mapper/UserMapper", "findUsers", "()Ljava/util/List;");

        List<ClassReference> classes = List.of(
                classRef("demo/web/UserController", "java/lang/Object", List.of(), Set.of()),
                classRef("demo/rpc/UserService", "java/lang/Object", List.of(), Set.of(new AnnoReference("Lorg/apache/dubbo/config/annotation/DubboService;"))),
                classRef("org/apache/jsp/index_jsp", "java/lang/Object", List.of(), Set.of()),
                classRef("demo/struts/LoginAction", "org/apache/struts/action/Action", List.of(), Set.of()),
                classRef("demo/netty/AppHandler", "java/lang/Object", List.of("io/netty/channel/ChannelInboundHandler"), Set.of()),
                classRef("demo/mapper/UserMapper", "java/lang/Object", List.of(), Set.of())
        );

        SpringController controller = new SpringController();
        controller.setClassName(new ClassReference.Handle("demo/web/UserController", 1));
        SpringMapping mapping = new SpringMapping();
        mapping.setController(controller);
        mapping.setMethodName(springMethod.getHandle());
        controller.addMapping(mapping);

        Path mapperXml = tempDir.resolve("UserMapper.xml");
        Files.writeString(mapperXml, """
                <mapper namespace="demo.mapper.UserMapper">
                  <select id="findUsers">
                    select * from users where name = ${name}
                  </select>
                </mapper>
                """, StandardCharsets.UTF_8);

        Map<MethodReference.Handle, Integer> flags = MethodSemanticSupport.derive(
                List.of(springMethod, rpcMethod, jspMethod, strutsMethod, nettyMethod, mapperMethod),
                classes,
                List.of(controller),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(resource("mapper/UserMapper.xml", mapperXml)),
                Map.of(
                        springMethod.getHandle(), GraphNode.SOURCE_FLAG_ANY | GraphNode.SOURCE_FLAG_WEB,
                        rpcMethod.getHandle(), GraphNode.SOURCE_FLAG_ANY | GraphNode.SOURCE_FLAG_RPC,
                        jspMethod.getHandle(), GraphNode.SOURCE_FLAG_ANY | GraphNode.SOURCE_FLAG_WEB,
                        strutsMethod.getHandle(), GraphNode.SOURCE_FLAG_ANY | GraphNode.SOURCE_FLAG_WEB,
                        nettyMethod.getHandle(), GraphNode.SOURCE_FLAG_ANY | GraphNode.SOURCE_FLAG_WEB
                )
        );

        assertHas(flags, springMethod, MethodSemanticFlags.ENTRY | MethodSemanticFlags.WEB_ENTRY | MethodSemanticFlags.SPRING_ENDPOINT);
        assertHas(flags, rpcMethod, MethodSemanticFlags.ENTRY | MethodSemanticFlags.RPC_ENTRY);
        assertHas(flags, jspMethod, MethodSemanticFlags.ENTRY | MethodSemanticFlags.WEB_ENTRY | MethodSemanticFlags.JSP_ENDPOINT);
        assertHas(flags, strutsMethod, MethodSemanticFlags.ENTRY | MethodSemanticFlags.WEB_ENTRY | MethodSemanticFlags.STRUTS_ACTION);
        assertHas(flags, nettyMethod, MethodSemanticFlags.ENTRY | MethodSemanticFlags.WEB_ENTRY | MethodSemanticFlags.NETTY_HANDLER);
        assertHas(flags, mapperMethod, MethodSemanticFlags.MYBATIS_DYNAMIC_SQL);
    }

    @Test
    void shouldDeriveGadgetSemanticFlags() {
        MethodReference readObject = method("demo/gadget/SerBean", "readObject", "(Ljava/io/ObjectInputStream;)V");
        MethodReference validateObject = method("demo/gadget/SerBean", "validateObject", "()V");
        MethodReference invoke = method(
                "demo/gadget/Handler",
                "invoke",
                "(Ljava/lang/Object;Ljava/lang/reflect/Method;[Ljava/lang/Object;)Ljava/lang/Object;"
        );
        MethodReference compare = method("demo/gadget/Sorter", "compare", "(Ljava/lang/Object;Ljava/lang/Object;)I");
        MethodReference compareTo = method("demo/gadget/NaturalSorter", "compareTo", "(Ljava/lang/Object;)I");
        MethodReference transform = method("demo/gadget/Xform", "transform", "(Ljava/lang/Object;)Ljava/lang/Object;");
        MethodReference toStringMethod = method("demo/gadget/Bean", "toString", "()Ljava/lang/String;");
        MethodReference hashCodeMethod = method("demo/gadget/Bean", "hashCode", "()I");
        MethodReference equalsMethod = method("demo/gadget/Bean", "equals", "(Ljava/lang/Object;)Z");

        List<ClassReference> classes = List.of(
                classRef("demo/gadget/SerBean", "java/lang/Object", List.of("java/io/Serializable", "java/io/ObjectInputValidation"), Set.of()),
                classRef("demo/gadget/Handler", "java/lang/Object", List.of("java/lang/reflect/InvocationHandler"), Set.of()),
                classRef("demo/gadget/Sorter", "java/lang/Object", List.of("java/util/Comparator"), Set.of()),
                classRef("demo/gadget/NaturalSorter", "java/lang/Object", List.of("java/lang/Comparable"), Set.of()),
                classRef("demo/gadget/Xform", "java/lang/Object", List.of("org/apache/commons/collections/Transformer"), Set.of()),
                classRef("demo/gadget/Bean", "java/lang/Object", List.of(), Set.of())
        );

        Map<MethodReference.Handle, Integer> flags = MethodSemanticSupport.derive(
                List.of(readObject, validateObject, invoke, compare, compareTo, transform, toStringMethod, hashCodeMethod, equalsMethod),
                classes,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                Map.of()
        );

        assertHas(flags, readObject, MethodSemanticFlags.SERIALIZABLE_OWNER | MethodSemanticFlags.DESERIALIZATION_CALLBACK);
        assertHas(flags, validateObject, MethodSemanticFlags.SERIALIZABLE_OWNER | MethodSemanticFlags.DESERIALIZATION_CALLBACK);
        assertHas(flags, invoke, MethodSemanticFlags.INVOCATION_HANDLER);
        assertHas(flags, compare, MethodSemanticFlags.COMPARATOR_CALLBACK);
        assertHas(flags, compareTo, MethodSemanticFlags.COMPARABLE_CALLBACK);
        assertHas(flags, transform, MethodSemanticFlags.TRANSFORMER_CALLBACK);
        assertHas(flags, toStringMethod, MethodSemanticFlags.TOSTRING_TRIGGER);
        assertHas(flags, hashCodeMethod, MethodSemanticFlags.HASHCODE_TRIGGER);
        assertHas(flags, equalsMethod, MethodSemanticFlags.EQUALS_TRIGGER);
        assertEquals(9, flags.size());
    }

    private static void assertHas(Map<MethodReference.Handle, Integer> flags,
                                  MethodReference method,
                                  int expectedBits) {
        int actual = flags.getOrDefault(method.getHandle(), 0);
        assertTrue((actual & expectedBits) == expectedBits,
                () -> "expected bits " + expectedBits + " on " + method.getClassReference().getName() + "#" + method.getName() + " but was " + actual);
    }

    private static MethodReference method(String owner, String name, String desc) {
        return new MethodReference(
                new ClassReference.Handle(owner, 1),
                name,
                desc,
                false,
                Set.of(),
                0x0001,
                1,
                "app.jar",
                1
        );
    }

    private static ClassReference classRef(String name,
                                           String superClass,
                                           List<String> interfaces,
                                           Set<AnnoReference> annotations) {
        return new ClassReference(
                61,
                0x0021,
                name,
                superClass,
                interfaces,
                false,
                List.of(),
                annotations,
                "app.jar",
                1
        );
    }

    private static ResourceEntity resource(String resourcePath, Path path) throws Exception {
        ResourceEntity entity = new ResourceEntity();
        entity.setResourcePath(resourcePath);
        entity.setPathStr(path.toString());
        entity.setFileSize(Files.size(path));
        entity.setJarId(1);
        entity.setJarName("app.jar");
        return entity;
    }
}
