/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.gui.swing.cypher;

import org.junit.jupiter.api.Tag;

import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.gui.swing.cypher.model.SaveScriptRequest;
import me.n1ar4.jar.analyzer.gui.swing.cypher.model.ScriptItem;
import me.n1ar4.jar.analyzer.gui.swing.cypher.model.ScriptListResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@Tag("legacy-sqlite")
class CypherScriptServiceTest {
    private final CypherScriptService service = new CypherScriptService();

    @BeforeEach
    void setUp() throws Exception {
        DatabaseManager.clearAllData();
        clearScriptTable();
    }

    @AfterEach
    void tearDown() throws Exception {
        clearScriptTable();
    }

    @Test
    void shouldCreateUpdateDeleteAndListScripts() {
        ScriptItem created = service.save(new SaveScriptRequest(
                null,
                "demo",
                "MATCH (n) RETURN n LIMIT 1",
                "audit",
                false
        ));
        Assertions.assertTrue(created.scriptId() > 0L);
        Assertions.assertEquals("demo", created.title());

        ScriptListResponse firstList = service.list();
        Assertions.assertEquals(1, firstList.items().size());

        ScriptItem updated = service.save(new SaveScriptRequest(
                created.scriptId(),
                "demo-updated",
                "MATCH (m:Method) RETURN m LIMIT 5",
                "audit,method",
                true
        ));
        Assertions.assertEquals(created.scriptId(), updated.scriptId());
        Assertions.assertEquals("demo-updated", updated.title());
        Assertions.assertTrue(updated.pinned());

        ScriptListResponse secondList = service.list();
        Assertions.assertEquals(1, secondList.items().size());
        Assertions.assertEquals("demo-updated", secondList.items().get(0).title());

        service.delete(created.scriptId());
        ScriptListResponse finalList = service.list();
        Assertions.assertTrue(finalList.items().isEmpty());
    }

    private static void clearScriptTable() {
        DatabaseManager.clearAllData();
    }
}
