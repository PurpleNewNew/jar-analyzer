/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.graph.query;

import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.graph.store.GraphStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProjectReadinessGateTest {
    @AfterEach
    void cleanup() {
        DatabaseManager.setBuilding(false);
        DatabaseManager.clearAllData();
    }

    @Test
    void graphStoreShouldRejectReadsWhileBuilding() {
        DatabaseManager.setBuilding(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> new GraphStore().loadSnapshot());
        assertEquals("project_build_in_progress", ex.getMessage());
    }

    @Test
    void queryServiceShouldRejectNativeReadsWhileBuilding() {
        DatabaseManager.setBuilding(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> new Neo4jQueryService().explain("MATCH (n) RETURN n LIMIT 1"));
        assertEquals("project_build_in_progress", ex.getMessage());
    }

    @Test
    void graphStoreShouldRejectReadsWhenProjectRuntimeIsMissing() {
        DatabaseManager.clearAllData();

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> new GraphStore().loadSnapshot());
        assertEquals("project_model_missing_rebuild", ex.getMessage());
    }
}
