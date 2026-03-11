/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.storage.neo4j;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectGraphStoreFacadeTest {
    private final ProjectGraphStoreFacade facade = ProjectGraphStoreFacade.getInstance();

    @Test
    void readWriteShouldRoundTripProjectData() {
        String projectKey = "facade-tx-" + Long.toHexString(System.nanoTime());
        try {
            facade.write(projectKey, 30_000L, tx -> {
                tx.execute("MERGE (m:JAMeta {key:'facade-test'}) SET m.value = 7");
                return null;
            });
            Integer value = facade.read(projectKey, 30_000L, tx -> {
                try (var rs = tx.execute("MATCH (m:JAMeta {key:'facade-test'}) RETURN m.value AS value")) {
                    if (!rs.hasNext()) {
                        return -1;
                    }
                    Object raw = rs.next().get("value");
                    return raw instanceof Number n ? n.intValue() : Integer.parseInt(String.valueOf(raw));
                }
            });
            Path home = facade.resolveProjectHome(projectKey);
            assertEquals(7, value);
            assertTrue(home.toString().contains("neo4j-projects"));
        } finally {
            facade.deleteProjectStore(projectKey);
        }
    }

    @Test
    void importLockShouldBlockDatabaseReopenUntilReleased() {
        String projectKey = "facade-lock-" + Long.toHexString(System.nanoTime());
        try {
            assertDoesNotThrow(() -> facade.database(projectKey));
            facade.beginProjectImport(projectKey);
            IllegalStateException ex = assertThrows(IllegalStateException.class, () -> facade.database(projectKey));
            assertEquals("neo4j_project_import_in_progress", ex.getMessage());
            facade.endProjectImport(projectKey);
            assertDoesNotThrow(() -> facade.database(projectKey));
        } finally {
            facade.endProjectImport(projectKey);
            facade.deleteProjectStore(projectKey);
        }
    }
}
