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

import me.n1ar4.jar.analyzer.engine.EngineContext;
import me.n1ar4.jar.analyzer.gui.runtime.model.EditorDocumentDto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EditorFacadeNavigationTest {
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
}
