package me.n1ar4.jar.analyzer.core.taie;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TaieEdgePolicyTest {

    @Test
    void shouldUseReachableAppByDefault() {
        String backup = System.getProperty("jar.analyzer.taie.edge.policy");
        try {
            System.clearProperty("jar.analyzer.taie.edge.policy");
            assertEquals(TaieEdgeMapper.EdgePolicy.REACHABLE_APP, TaieEdgeMapper.resolveEdgePolicy());
        } finally {
            restoreProp("jar.analyzer.taie.edge.policy", backup);
        }
    }

    @Test
    void shouldParsePolicyAliases() {
        assertEquals(TaieEdgeMapper.EdgePolicy.APP_CALLER, TaieEdgeMapper.EdgePolicy.fromValue("app"));
        assertEquals(TaieEdgeMapper.EdgePolicy.REACHABLE_APP, TaieEdgeMapper.EdgePolicy.fromValue("default"));
        assertEquals(TaieEdgeMapper.EdgePolicy.REACHABLE_APP, TaieEdgeMapper.EdgePolicy.fromValue("unknown"));
        assertEquals(TaieEdgeMapper.EdgePolicy.NON_SDK_CALLER, TaieEdgeMapper.EdgePolicy.fromValue("non-sdk"));
        assertEquals(TaieEdgeMapper.EdgePolicy.FULL, TaieEdgeMapper.EdgePolicy.fromValue("all"));
        assertEquals(TaieEdgeMapper.EdgePolicy.REACHABLE_APP, TaieEdgeMapper.EdgePolicy.fromValue("reachable"));
    }

    private static void restoreProp(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }
}
