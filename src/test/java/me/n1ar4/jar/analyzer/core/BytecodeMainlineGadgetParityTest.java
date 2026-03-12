/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.core;

import me.n1ar4.jar.analyzer.engine.ProjectRuntimeContext;
import me.n1ar4.jar.analyzer.graph.store.GraphEdge;
import me.n1ar4.jar.analyzer.graph.store.GraphNode;
import me.n1ar4.jar.analyzer.graph.store.GraphSnapshot;
import me.n1ar4.jar.analyzer.graph.store.GraphStore;
import me.n1ar4.support.FixtureJars;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BytecodeMainlineGadgetParityTest {
    @AfterEach
    void cleanup() {
        System.clearProperty("jar.analyzer.callgraph.engine");
        System.clearProperty("jar.analyzer.callgraph.profile");
        GraphStore.invalidateCache();
        DatabaseManager.clearAllData();
        ProjectRuntimeContext.clear();
    }

    @Test
    void balancedBytecodeShouldAddComparatorCallbackEdge() {
        GraphSnapshot snapshot = buildBalancedSnapshot();
        long nodeId = snapshot.findMethodNodeId(
                "fixture/gadget/cc/QueueBridge",
                "replay",
                "(Ljava/util/Comparator;Ljava/lang/Object;Ljava/lang/Object;)I",
                null
        );
        assertTrue(nodeId > 0L, "queue bridge node missing");
        assertHasEdge(
                snapshot,
                nodeId,
                "CALLS_CALLBACK",
                "org/apache/commons/beanutils/BeanComparator",
                "compare",
                "(Ljava/lang/Object;Ljava/lang/Object;)I"
        );
    }

    @Test
    void balancedBytecodeShouldResolveFieldBackedReflectionTarget() {
        GraphSnapshot snapshot = buildBalancedSnapshot();
        long nodeId = snapshot.findMethodNodeId(
                "com/sun/syndication/feed/impl/ToStringBean",
                "toString",
                "()Ljava/lang/String;",
                null
        );
        assertTrue(nodeId > 0L, "toString bean node missing");
        assertHasEdge(
                snapshot,
                nodeId,
                "CALLS_REFLECTION",
                "fixture/gadget/rome/GetterBean",
                "getOutputProperties",
                "()Ljava/lang/String;"
        );
    }

    @Test
    void balancedBytecodeShouldBridgeObjectTriggerToRomeToString() {
        GraphSnapshot snapshot = buildBalancedYsoserialSnapshot();
        long nodeId = snapshot.findMethodNodeId(
                "ysoserial/payloads/util/RomeBadAttributeValueExpException",
                "replay",
                "(Ljava/lang/Object;)Ljava/lang/String;",
                null
        );
        assertTrue(nodeId > 0L, "rome bad attribute node missing");
        assertHasEdge(
                snapshot,
                nodeId,
                "CALLS_FRAMEWORK",
                "com/sun/syndication/feed/impl/ToStringBean",
                "toString",
                "()Ljava/lang/String;"
        );
    }

    private static GraphSnapshot buildBalancedSnapshot() {
        System.setProperty("jar.analyzer.callgraph.engine", "bytecode-mainline+pta-refine");
        Path jar = FixtureJars.gadgetFamilyTestJar();
        ProjectRuntimeContext.updateResolveInnerJars(false);
        CoreRunner.run(jar, null, false, null);
        return new GraphStore().loadSnapshot();
    }

    private static GraphSnapshot buildBalancedYsoserialSnapshot() {
        System.setProperty("jar.analyzer.callgraph.engine", "bytecode-mainline+pta-refine");
        Path jar = FixtureJars.ysoserialPayloadTestJar();
        ProjectRuntimeContext.updateResolveInnerJars(false);
        CoreRunner.run(jar, null, false, null);
        return new GraphStore().loadSnapshot();
    }

    private static void assertHasEdge(GraphSnapshot snapshot,
                                      long sourceNodeId,
                                      String relType,
                                      String className,
                                      String methodName,
                                      String methodDesc) {
        List<GraphEdge> edges = snapshot.getOutgoingView(sourceNodeId);
        for (GraphEdge edge : edges) {
            if (edge == null || !relType.equals(edge.getRelType())) {
                continue;
            }
            GraphNode dst = snapshot.getNode(edge.getDstId());
            if (dst == null) {
                continue;
            }
            if (className.equals(dst.getClassName())
                    && methodName.equals(dst.getMethodName())
                    && methodDesc.equals(dst.getMethodDesc())) {
                return;
            }
        }
        assertTrue(false, "missing edge " + relType + " -> " + className + "#" + methodName + methodDesc);
    }
}
