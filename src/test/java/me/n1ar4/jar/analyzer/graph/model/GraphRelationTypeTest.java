package me.n1ar4.jar.analyzer.graph.model;

import me.n1ar4.jar.analyzer.core.MethodCallMeta;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GraphRelationTypeTest {

    @Test
    void shouldMapExtendedEdgeTypes() {
        assertEquals(GraphRelationType.CALLS_INDY, GraphRelationType.fromEdgeType(MethodCallMeta.TYPE_INDY));
        assertEquals(GraphRelationType.CALLS_METHOD_HANDLE, GraphRelationType.fromEdgeType(MethodCallMeta.TYPE_METHOD_HANDLE));
        assertEquals(GraphRelationType.CALLS_FRAMEWORK, GraphRelationType.fromEdgeType(MethodCallMeta.TYPE_FRAMEWORK));
        assertEquals(GraphRelationType.CALLS_PTA, GraphRelationType.fromEdgeType(MethodCallMeta.TYPE_PTA));
    }

    @Test
    void shouldExposeDisplayGroupAndSubtype() {
        assertEquals("CALL", GraphRelationType.relationGroup("CALLS_DIRECT"));
        assertEquals("ALIAS", GraphRelationType.relationGroup("ALIAS"));
        assertEquals("HAS", GraphRelationType.relationGroup("HAS"));
        assertEquals("EXTEND", GraphRelationType.relationGroup("EXTEND"));
        assertEquals("INTERFACES", GraphRelationType.relationGroup("INTERFACES"));
        assertEquals("dispatch", GraphRelationType.relationSubtype("CALLS_DISPATCH"));
        assertEquals("alias", GraphRelationType.relationSubtype("ALIAS"));
        assertEquals("has", GraphRelationType.relationSubtype("HAS"));
        assertEquals("extend", GraphRelationType.relationSubtype("EXTEND"));
        assertEquals("interfaces", GraphRelationType.relationSubtype("INTERFACES"));
        assertEquals("PATH", GraphRelationType.relationGroup("PATH"));
        assertEquals("path", GraphRelationType.relationSubtype("PATH"));
    }

    @Test
    void shouldExpandLogicalCallRelationType() {
        assertEquals(GraphRelationType.physicalCallRelationTypes(), GraphRelationType.expandLogicalRelationType("CALL"));
        assertEquals(9, GraphRelationType.physicalCallRelationTypes().size());
        assertTrue(GraphRelationType.physicalCallRelationTypes().contains("CALLS_DIRECT"));
        assertEquals("ALIAS", GraphRelationType.expandLogicalRelationType("ALIAS").get(0));
    }
}
