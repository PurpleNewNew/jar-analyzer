/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.qa;

import me.n1ar4.jar.analyzer.core.CoreRunner;
import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.engine.ProjectRuntimeContext;
import me.n1ar4.jar.analyzer.graph.proc.ProcedureRegistry;
import me.n1ar4.jar.analyzer.graph.query.QueryOptions;
import me.n1ar4.jar.analyzer.graph.query.QueryResult;
import me.n1ar4.jar.analyzer.graph.store.GraphNode;
import me.n1ar4.jar.analyzer.graph.store.GraphSnapshot;
import me.n1ar4.jar.analyzer.graph.store.GraphStore;
import me.n1ar4.jar.analyzer.rules.MethodSemanticFlags;
import me.n1ar4.support.FixtureJars;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class RealGadgetFamilyRegressionTest {
    @AfterEach
    void cleanup() {
        GraphStore.invalidateCache();
        DatabaseManager.clearAllData();
        ProjectRuntimeContext.clear();
    }

    @Test
    @SuppressWarnings("all")
    public void gadgetFixtureShouldExposeRepresentativeSemanticFlags() {
        try {
            GraphSnapshot snapshot = buildSnapshot();

            MethodReference ccSource = requireMethod(
                    "fixture/gadget/cc/CcPayload",
                    "readObject",
                    "(Ljava/io/ObjectInputStream;)V"
            );
            MethodReference queueReplay = requireMethod(
                    "fixture/gadget/cc/QueueBridge",
                    "replay",
                    "(Ljava/util/Comparator;Ljava/lang/Object;Ljava/lang/Object;)I"
            );
            MethodReference beanComparator = requireMethod(
                    "org/apache/commons/beanutils/BeanComparator",
                    "compare",
                    "(Ljava/lang/Object;Ljava/lang/Object;)I"
            );
            MethodReference proxyHandler = requireMethod(
                    "fixture/gadget/proxy/JdkProxyHandler",
                    "invoke",
                    "(Ljava/lang/Object;Ljava/lang/reflect/Method;[Ljava/lang/Object;)Ljava/lang/Object;"
            );
            MethodReference romeTrigger = requireMethod(
                    "com/sun/syndication/feed/impl/ToStringBean",
                    "toString",
                    "()Ljava/lang/String;"
            );

            assertSemantic(ccSource, MethodSemanticFlags.SERIALIZABLE_OWNER | MethodSemanticFlags.DESERIALIZATION_CALLBACK);
            assertSemantic(queueReplay, MethodSemanticFlags.COLLECTION_CONTAINER);
            assertSemantic(beanComparator, MethodSemanticFlags.COMPARATOR_CALLBACK);
            assertSemantic(proxyHandler, MethodSemanticFlags.INVOCATION_HANDLER);
            assertSemantic(romeTrigger, MethodSemanticFlags.TOSTRING_TRIGGER);

            assertTrue(requireNode(snapshot,
                    "fixture/gadget/cc/QueueBridge",
                    "replay",
                    "(Ljava/util/Comparator;Ljava/lang/Object;Ljava/lang/Object;)I")
                    .hasMethodSemanticFlag(MethodSemanticFlags.COLLECTION_CONTAINER));
            assertTrue(requireNode(snapshot,
                    "com/sun/syndication/feed/impl/ToStringBean",
                    "toString",
                    "()Ljava/lang/String;")
                    .hasMethodSemanticFlag(MethodSemanticFlags.TOSTRING_TRIGGER));
        } catch (Exception ex) {
            fail("gadget semantic regression failed: " + ex);
        }
    }

    @Test
    @SuppressWarnings("all")
    public void gadgetFixtureShouldFindRepresentativeRoutes() {
        try {
            GraphSnapshot snapshot = buildSnapshot();
            ProcedureRegistry registry = new ProcedureRegistry();
            QueryOptions options = QueryOptions.defaults();

            QueryResult cc = registry.execute(
                    "ja.path.gadget",
                    List.of(
                            ref("fixture/gadget/cc/CcPayload", "readObject", "(Ljava/io/ObjectInputStream;)V"),
                            ref("fixture/gadget/sink/RuntimeSink", "exec", "(Ljava/lang/Object;)Ljava/lang/String;"),
                            "8",
                            "12"
                    ),
                    java.util.Map.of(),
                    options,
                    snapshot
            );
            assertRoute(cc, "container-callback");

            QueryResult proxy = registry.execute(
                    "ja.path.gadget",
                    List.of(
                            ref("fixture/gadget/proxy/ProxyPayload", "readObject", "(Ljava/io/ObjectInputStream;)V"),
                            ref("fixture/gadget/sink/RuntimeSink", "exec", "(Ljava/lang/Object;)Ljava/lang/String;"),
                            "8",
                            "12"
                    ),
                    java.util.Map.of(),
                    options,
                    snapshot
            );
            assertRoute(proxy, "proxy-dynamic");

            QueryResult rome = registry.execute(
                    "ja.path.gadget",
                    List.of(
                            ref("fixture/gadget/rome/RomePayload", "readObject", "(Ljava/io/ObjectInputStream;)V"),
                            ref("fixture/gadget/sink/RuntimeSink", "exec", "(Ljava/lang/Object;)Ljava/lang/String;"),
                            "8",
                            "12"
                    ),
                    java.util.Map.of(),
                    options,
                    snapshot
            );
            assertAnyRoute(rome, Set.of("reflection-trigger", "container-trigger"));
        } catch (Exception ex) {
            fail("gadget route regression failed: " + ex);
        }
    }

    @Test
    @SuppressWarnings("all")
    public void gadgetTrackShouldEnumerateRealFixtureSources() {
        try {
            GraphSnapshot snapshot = buildSnapshot();
            QueryResult result = new ProcedureRegistry().execute(
                    "ja.gadget.track",
                    List.of(
                            "", "", "",
                            "fixture/gadget/sink/RuntimeSink", "exec", "(Ljava/lang/Object;)Ljava/lang/String;",
                            "8", "10", "true"
                    ),
                    java.util.Map.of(),
                    QueryOptions.defaults(),
                    snapshot
            );

            assertTrue(result.getWarnings().contains("gadget_source_candidates=3"));
            Set<String> routes = extractRoutes(result);
            assertTrue(routes.contains("container-callback"));
            assertTrue(routes.contains("proxy-dynamic"));
            assertTrue(routes.contains("reflection-trigger") || routes.contains("container-trigger"));
        } catch (Exception ex) {
            fail("gadget track regression failed: " + ex);
        }
    }

    private static GraphSnapshot buildSnapshot() {
        Path jar = FixtureJars.gadgetFamilyTestJar();
        ProjectRuntimeContext.updateResolveInnerJars(false);
        CoreRunner.run(jar, null, false, false, null);
        return new GraphStore().loadSnapshot();
    }

    private static void assertSemantic(MethodReference method, int mask) {
        assertNotNull(method);
        assertTrue((method.getSemanticFlags() & mask) == mask,
                "semantic flags mismatch for " + method.getClassReference().getName()
                        + "#" + method.getName() + method.getDesc()
                        + " actual=" + method.getSemanticFlags()
                        + " expectedMask=" + mask);
    }

    private static void assertRoute(QueryResult result, String route) {
        assertAnyRoute(result, Set.of(route));
    }

    private static void assertAnyRoute(QueryResult result, Set<String> routes) {
        Set<String> actual = extractRoutes(result);
        for (String route : routes) {
            if (actual.contains(route)) {
                return;
            }
        }
        fail("expected routes " + routes + " but got " + actual + " warnings=" + result.getWarnings());
    }

    private static Set<String> extractRoutes(QueryResult result) {
        Set<String> out = new HashSet<>();
        if (result == null || result.getRows() == null) {
            return out;
        }
        for (List<Object> row : result.getRows()) {
            if (row == null || row.size() < 7) {
                continue;
            }
            String evidence = String.valueOf(row.get(6));
            int index = evidence.indexOf("route=");
            if (index < 0) {
                continue;
            }
            int start = index + 6;
            int end = evidence.length();
            for (int i = start; i < evidence.length(); i++) {
                char c = evidence.charAt(i);
                if (c == ',' || Character.isWhitespace(c)) {
                    end = i;
                    break;
                }
            }
            out.add(evidence.substring(start, end));
        }
        return out;
    }

    private static String ref(String className, String methodName, String desc) {
        return className + "#" + methodName + "#" + desc;
    }

    private static MethodReference requireMethod(String className, String methodName, String desc) {
        for (MethodReference method : DatabaseManager.getMethodReferencesByClass(className)) {
            if (method == null || method.getClassReference() == null) {
                continue;
            }
            if (className.equals(method.getClassReference().getName())
                    && methodName.equals(method.getName())
                    && desc.equals(method.getDesc())) {
                return method;
            }
        }
        fail("method not found: " + className + "#" + methodName + desc);
        return null;
    }

    private static GraphNode requireNode(GraphSnapshot snapshot, String className, String methodName, String desc) {
        long nodeId = snapshot.findMethodNodeId(className, methodName, desc, null);
        assertTrue(nodeId > 0L, "graph node not found for " + className + "#" + methodName + desc);
        return snapshot.getNode(nodeId);
    }
}
