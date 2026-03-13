/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.gui.swing.cypher;

import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.gui.swing.cypher.model.SaveScriptRequest;
import me.n1ar4.jar.analyzer.gui.swing.cypher.model.ScriptItem;
import me.n1ar4.jar.analyzer.gui.swing.cypher.model.ScriptListResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

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

    @Test
    void shouldNotOverwriteUnreadableStoreOnSave() throws Exception {
        Path store = prepareUnreadableStore();

        IllegalStateException ex = Assertions.assertThrows(IllegalStateException.class, () -> service.save(
                new SaveScriptRequest(null, "demo", "MATCH (n) RETURN n", "audit", false)
        ));

        Assertions.assertTrue(ex.getMessage().contains("cypher_script_store_unreadable"));
        Assertions.assertEquals("not-json", Files.readString(store, StandardCharsets.UTF_8));
    }

    @Test
    void shouldRejectUnreadableStoreForListAndDeleteWithoutResettingFile() throws Exception {
        Path store = prepareUnreadableStore();

        IllegalStateException listEx = Assertions.assertThrows(IllegalStateException.class, service::list);
        IllegalStateException deleteEx = Assertions.assertThrows(IllegalStateException.class, () -> service.delete(1L));

        Assertions.assertTrue(listEx.getMessage().contains("cypher_script_store_unreadable"));
        Assertions.assertTrue(deleteEx.getMessage().contains("cypher_script_store_unreadable"));
        Assertions.assertEquals("not-json", Files.readString(store, StandardCharsets.UTF_8));
    }

    private static void clearScriptTable() throws Exception {
        if (Files.exists(CypherScriptService.storeFilePath())) {
            Files.delete(CypherScriptService.storeFilePath());
        }
    }

    private static Path prepareUnreadableStore() throws Exception {
        Path store = CypherScriptService.storeFilePath();
        Files.createDirectories(store.getParent());
        Files.writeString(store, "not-json", StandardCharsets.UTF_8);
        return store;
    }
}
