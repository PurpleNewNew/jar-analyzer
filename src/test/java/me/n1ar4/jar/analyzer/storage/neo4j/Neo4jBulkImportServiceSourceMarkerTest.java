package me.n1ar4.jar.analyzer.storage.neo4j;

import me.n1ar4.jar.analyzer.analyze.spring.SpringController;
import me.n1ar4.jar.analyzer.analyze.spring.SpringMapping;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.graph.store.GraphNode;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class Neo4jBulkImportServiceSourceMarkerTest {
    @Test
    void explicitWebEntriesShouldBeMarkedAsWebSources() throws Exception {
        SpringController springController = new SpringController();
        springController.setRest(true);
        springController.setBasePath("/api");
        springController.setClassName(new ClassReference.Handle("demo/WebController", 1));

        SpringMapping mapping = new SpringMapping();
        mapping.setRest(true);
        mapping.setController(springController);
        mapping.setMethodName(new MethodReference.Handle(new ClassReference.Handle("demo/WebController", 1), "index", "()Ljava/lang/String;"));
        mapping.setPath("/index");
        mapping.setRestfulType("GET");
        mapping.setPathRestful("/api/index");
        springController.addMapping(mapping);

        Method buildSourceMarkerIndex = Neo4jBulkImportService.class
                .getDeclaredMethod("buildSourceMarkerIndex", Neo4jBulkImportService.GraphPayloadData.class);
        buildSourceMarkerIndex.setAccessible(true);
        Object markerIndex = buildSourceMarkerIndex.invoke(null, new Neo4jBulkImportService.GraphPayloadData(
                List.of(),
                List.of(),
                List.of(springController),
                List.of("demo/AuthInterceptor"),
                List.of("demo/LegacyServlet"),
                List.of("demo/AuthFilter"),
                List.of("demo/AppListener"),
                Map.of()
        ));
        Class<?> markerIndexType = Class.forName(
                "me.n1ar4.jar.analyzer.storage.neo4j.Neo4jBulkImportService$SourceMarkerIndex");
        Method resolveSourceFlags = Neo4jBulkImportService.class
                .getDeclaredMethod("resolveSourceFlags", MethodReference.class, markerIndexType);
        resolveSourceFlags.setAccessible(true);

        MethodReference controllerMethod = method("demo/WebController", "index", "()Ljava/lang/String;");
        MethodReference filter = method(
                "demo/AuthFilter",
                "doFilter",
                "(Ljavax/servlet/ServletRequest;Ljavax/servlet/ServletResponse;Ljavax/servlet/FilterChain;)V");
        MethodReference interceptor = method(
                "demo/AuthInterceptor",
                "preHandle",
                "(Ljavax/servlet/http/HttpServletRequest;Ljavax/servlet/http/HttpServletResponse;Ljava/lang/Object;)Z");

        int controllerFlags = (int) resolveSourceFlags.invoke(null, controllerMethod, markerIndex);
        int filterFlags = (int) resolveSourceFlags.invoke(null, filter, markerIndex);
        int interceptorFlags = (int) resolveSourceFlags.invoke(null, interceptor, markerIndex);

        assertTrue((controllerFlags & GraphNode.SOURCE_FLAG_WEB) != 0);
        assertTrue((controllerFlags & GraphNode.SOURCE_FLAG_ANY) != 0);
        assertTrue((filterFlags & GraphNode.SOURCE_FLAG_WEB) != 0);
        assertTrue((interceptorFlags & GraphNode.SOURCE_FLAG_WEB) != 0);
    }

    private static MethodReference method(String owner, String name, String desc) {
        return new MethodReference(
                new ClassReference.Handle(owner, 1),
                name,
                desc,
                false,
                Set.of(),
                1,
                1,
                "app.jar",
                1
        );
    }
}
