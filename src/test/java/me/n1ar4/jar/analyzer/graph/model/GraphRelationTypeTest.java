package me.n1ar4.jar.analyzer.graph.model;

import me.n1ar4.jar.analyzer.core.MethodCallMeta;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GraphRelationTypeTest {

    @Test
    void shouldMapExtendedEdgeTypes() {
        assertEquals(GraphRelationType.CALLS_INDY, GraphRelationType.fromEdgeType(MethodCallMeta.TYPE_INDY));
        assertEquals(GraphRelationType.CALLS_METHOD_HANDLE, GraphRelationType.fromEdgeType(MethodCallMeta.TYPE_METHOD_HANDLE));
        assertEquals(GraphRelationType.CALLS_FRAMEWORK, GraphRelationType.fromEdgeType(MethodCallMeta.TYPE_FRAMEWORK));
        assertEquals(GraphRelationType.CALLS_PTA, GraphRelationType.fromEdgeType(MethodCallMeta.TYPE_PTA));
    }
}
