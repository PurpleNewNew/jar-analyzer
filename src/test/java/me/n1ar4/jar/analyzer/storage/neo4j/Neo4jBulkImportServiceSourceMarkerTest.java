package me.n1ar4.jar.analyzer.storage.neo4j;

import me.n1ar4.jar.analyzer.core.ProjectRuntimeSnapshot;
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
        ProjectRuntimeSnapshot snapshot = new ProjectRuntimeSnapshot(
                ProjectRuntimeSnapshot.CURRENT_SCHEMA_VERSION,
                1L,
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                Map.of(),
                Map.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(new ProjectRuntimeSnapshot.SpringControllerData(
                        true,
                        "/api",
                        new ProjectRuntimeSnapshot.ClassHandleData("demo/WebController", 1),
                        List.of(new ProjectRuntimeSnapshot.SpringMappingData(
                                true,
                                new ProjectRuntimeSnapshot.ClassHandleData("demo/WebController", 1),
                                "index",
                                "()Ljava/lang/String;",
                                "/index",
                                "GET",
                                "/api/index",
                                List.of()
                        ))
                )),
                Set.of("demo/AuthInterceptor"),
                Set.of("demo/LegacyServlet"),
                Set.of("demo/AuthFilter"),
                Set.of("demo/AppListener")
        );

        Method buildSourceMarkerIndex = Neo4jBulkImportService.class
                .getDeclaredMethod("buildSourceMarkerIndex", ProjectRuntimeSnapshot.class);
        buildSourceMarkerIndex.setAccessible(true);
        Object markerIndex = buildSourceMarkerIndex.invoke(null, snapshot);
        Class<?> markerIndexType = Class.forName(
                "me.n1ar4.jar.analyzer.storage.neo4j.Neo4jBulkImportService$SourceMarkerIndex");
        Method resolveSourceFlags = Neo4jBulkImportService.class
                .getDeclaredMethod("resolveSourceFlags", MethodReference.class, markerIndexType);
        resolveSourceFlags.setAccessible(true);

        MethodReference controller = method("demo/WebController", "index", "()Ljava/lang/String;");
        MethodReference filter = method(
                "demo/AuthFilter",
                "doFilter",
                "(Ljavax/servlet/ServletRequest;Ljavax/servlet/ServletResponse;Ljavax/servlet/FilterChain;)V");
        MethodReference interceptor = method(
                "demo/AuthInterceptor",
                "preHandle",
                "(Ljavax/servlet/http/HttpServletRequest;Ljavax/servlet/http/HttpServletResponse;Ljava/lang/Object;)Z");

        int controllerFlags = (int) resolveSourceFlags.invoke(null, controller, markerIndex);
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
