/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.taint.summary;

import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.rules.ModelRegistry;
import me.n1ar4.jar.analyzer.storage.neo4j.ActiveProjectContext;
import me.n1ar4.support.DatabaseManagerTestHook;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertNull;

class SummaryEngineProjectIsolationTest {
    @Test
    void shouldClearInMemorySummaryCacheWhenProjectChanges() throws Exception {
        String originalProjectKey = ActiveProjectContext.getActiveProjectKey();
        String originalProjectAlias = ActiveProjectContext.getActiveProjectAlias();
        SummaryEngine engine = new SummaryEngine();
        MethodReference.Handle handle = new MethodReference.Handle(
                new ClassReference.Handle("demo/SummaryProject", 1),
                "run",
                "()V"
        );
        try {
            ActiveProjectContext.setActiveProject("summary-project-a", "summary-project-a");
            DatabaseManagerTestHook.markProjectBuildReady(7L);

            SummaryCache cache = (SummaryCache) field(SummaryEngine.class, "cache").get(engine);
            cache.put(handle, new MethodSummary());

            field(SummaryEngine.class, "ruleVersion").setLong(engine, ModelRegistry.getVersion());
            field(SummaryEngine.class, "ruleFingerprint").set(engine, ModelRegistry.getRulesFingerprint());
            field(SummaryEngine.class, "projectBuildSeq").setLong(engine, DatabaseManager.getProjectBuildSeq());
            field(SummaryEngine.class, "projectKey").set(engine, ActiveProjectContext.getActiveProjectKey());
            field(SummaryEngine.class, "fingerprint").set(engine, "summary-test-fingerprint");

            ActiveProjectContext.setActiveProject("summary-project-b", "summary-project-b");
            Method ensureRuleContext = SummaryEngine.class.getDeclaredMethod("ensureRuleContext");
            ensureRuleContext.setAccessible(true);
            ensureRuleContext.invoke(engine);

            assertNull(cache.get(handle));
        } finally {
            ActiveProjectContext.setActiveProject(originalProjectKey, originalProjectAlias);
            DatabaseManager.clearAllData();
        }
    }

    private static Field field(Class<?> type, String name) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }
}
