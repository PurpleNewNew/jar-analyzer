/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.gui.runtime.api;

import me.n1ar4.jar.analyzer.config.ConfigFile;
import me.n1ar4.jar.analyzer.core.CoreRunner;
import me.n1ar4.jar.analyzer.engine.CoreEngine;
import me.n1ar4.jar.analyzer.engine.EngineContext;
import me.n1ar4.jar.analyzer.engine.ProjectRuntimeContext;
import me.n1ar4.jar.analyzer.gui.runtime.model.EditorDocumentDto;
import me.n1ar4.jar.analyzer.storage.neo4j.ActiveProjectContext;
import me.n1ar4.jar.analyzer.storage.neo4j.Neo4jProjectStore;
import me.n1ar4.jar.analyzer.storage.neo4j.ProjectRegistryService;
import me.n1ar4.support.FixtureJars;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EditorFacadeNavigationTest {
    @AfterEach
    void cleanup() {
        EngineContext.setEngine(null);
        ProjectRuntimeContext.clear();
        me.n1ar4.jar.analyzer.core.DatabaseManager.clearAllData();
        ProjectRegistryService.getInstance().cleanupTemporaryProject();
        ProjectRegistryService.getInstance().activateTemporaryProject();
    }

    @Test
    void openMethodShouldKeepNavigationContextWhenEngineMissing() {
        EngineContext.setEngine(null);
        RuntimeFacades.editor().openMethod("a.b.C", "run", "()V", 1);
        EditorDocumentDto doc = RuntimeFacades.editor().current();
        assertEquals("a.b.C", doc.className());
        assertEquals("run", doc.methodName());
        assertEquals("()V", doc.methodDesc());
        assertTrue(doc.statusText().contains("method opened"));
    }

    @Test
    void resolveUsagesShouldNotFallbackToDeclaration() throws Exception {
        bootstrapCallbackFixture();

        RuntimeFacades.editor().openMethod("me/n1ar4/cb/CallbackEntry", "dynamicProxy", "()V", null);
        EditorDocumentDto doc = RuntimeFacades.editor().current();
        int offset = findMethodTokenOffset(doc, "dynamicProxy");

        var result = RuntimeFacades.editor().resolveUsages(offset);

        assertNotNull(result);
        assertFalse(result.hasTargets());
        assertTrue(result.statusText().contains("usage target not found"));
    }

    @Test
    void resolveImplementationsShouldOnlyReturnConcreteTargets() throws Exception {
        bootstrapCallbackFixture();

        RuntimeFacades.editor().openMethod("me/n1ar4/cb/Task", "run", "()V", null);
        EditorDocumentDto doc = RuntimeFacades.editor().current();
        int offset = findMethodTokenOffset(doc, "run");

        var result = RuntimeFacades.editor().resolveImplementations(offset);

        assertNotNull(result);
        assertTrue(result.hasTargets());
        List<String> owners = result.targets().stream()
                .map(item -> item.className())
                .toList();
        assertTrue(owners.contains("me/n1ar4/cb/FastTask"));
        assertTrue(owners.contains("me/n1ar4/cb/SlowTask"));
        assertFalse(owners.contains("me/n1ar4/cb/Task"));
    }

    private static void bootstrapCallbackFixture() throws Exception {
        Path jar = FixtureJars.callbackTestJar();
        ProjectRuntimeContext.updateResolveInnerJars(false);
        CoreRunner.run(jar, null, false, null);
        ConfigFile config = new ConfigFile();
        config.setDbPath(Neo4jProjectStore.getInstance()
                .resolveProjectHome(ActiveProjectContext.getActiveProjectKey())
                .toString());
        EngineContext.setEngine(new CoreEngine(config));
    }

    private static int findMethodTokenOffset(EditorDocumentDto doc, String methodName) {
        String content = doc == null ? "" : safe(doc.content());
        String token = safe(methodName);
        if (content.isBlank() || token.isBlank()) {
            return 0;
        }
        int idx = content.indexOf(token + "(");
        if (idx >= 0) {
            return idx + Math.max(0, token.length() / 2);
        }
        idx = content.indexOf(token);
        return idx >= 0 ? idx + Math.max(0, token.length() / 2) : 0;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
