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
import me.n1ar4.jar.analyzer.core.ProjectRuntimeSnapshot;
import me.n1ar4.jar.analyzer.core.ProjectStateUtil;
import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.engine.ProjectRuntimeContext;
import me.n1ar4.jar.analyzer.engine.project.ProjectModel;
import me.n1ar4.jar.analyzer.rules.ModelRegistry;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNull;

class SummaryEngineProjectIsolationTest {
    @Test
    void shouldClearInMemorySummaryCacheWhenProjectChanges() throws Exception {
        SummaryEngine engine = new SummaryEngine();
        MethodReference.Handle handle = new MethodReference.Handle(
                new ClassReference.Handle("demo/SummaryProject", 1),
                "run",
                "()V"
        );
        try {
            ProjectModel model = ProjectModel.artifact(
                    Path.of("/tmp/jar-analyzer/summary-project-a.jar"),
                    null,
                    List.of(Path.of("/tmp/jar-analyzer/summary-project-a.jar")),
                    false
            );
            DatabaseManager.restoreProjectRuntime(new ProjectRuntimeSnapshot(
                    ProjectRuntimeSnapshot.CURRENT_SCHEMA_VERSION,
                    7L,
                    new ProjectRuntimeSnapshot.ProjectModelData(
                            model.buildMode().name(),
                            model.primaryInputPath().toString(),
                            "",
                            List.of(),
                            List.of(model.primaryInputPath().toString()),
                            false
                    ),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    java.util.Map.of(),
                    java.util.Map.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    java.util.Set.of(),
                    java.util.Set.of(),
                    java.util.Set.of(),
                    java.util.Set.of()
            ));
            ProjectRuntimeContext.restoreProjectRuntime("summary-project-a", 7L, model);

            SummaryCache cache = (SummaryCache) field(SummaryEngine.class, "cache").get(engine);
            cache.put(handle, new MethodSummary());

            field(SummaryEngine.class, "ruleVersion").setLong(engine, ModelRegistry.getVersion());
            field(SummaryEngine.class, "ruleFingerprint").set(engine, ModelRegistry.getRulesFingerprint());
            field(SummaryEngine.class, "projectRuntimeKey").set(engine, ProjectStateUtil.runtimeCacheKey());
            field(SummaryEngine.class, "fingerprint").set(engine, "summary-test-fingerprint");

            ProjectRuntimeContext.restoreProjectRuntime(
                    "summary-project-b",
                    7L,
                    ProjectModel.artifact(
                            Path.of("/tmp/jar-analyzer/summary-project-b.jar"),
                            null,
                            List.of(Path.of("/tmp/jar-analyzer/summary-project-b.jar")),
                            false
                    )
            );
            Method ensureRuleContext = SummaryEngine.class.getDeclaredMethod("ensureRuleContext");
            ensureRuleContext.setAccessible(true);
            ensureRuleContext.invoke(engine);

            assertNull(cache.get(handle));
        } finally {
            ProjectRuntimeContext.clear();
            DatabaseManager.clearAllData();
        }
    }

    @Test
    void shouldClearInMemorySummaryCacheWhenRuntimeModelChangesWithoutBuildSeqChange() throws Exception {
        SummaryEngine engine = new SummaryEngine();
        MethodReference.Handle handle = new MethodReference.Handle(
                new ClassReference.Handle("demo/SummaryProject", 1),
                "run",
                "()V"
        );
        try {
            ProjectModel modelA = ProjectModel.artifact(
                    Path.of("/tmp/jar-analyzer/runtime-a.jar"),
                    null,
                    List.of(Path.of("/tmp/jar-analyzer/runtime-a.jar")),
                    false
            );
            ProjectRuntimeContext.restoreProjectRuntime("summary-project", 7L, modelA);

            SummaryCache cache = (SummaryCache) field(SummaryEngine.class, "cache").get(engine);
            cache.put(handle, new MethodSummary());

            field(SummaryEngine.class, "ruleVersion").setLong(engine, ModelRegistry.getVersion());
            field(SummaryEngine.class, "ruleFingerprint").set(engine, ModelRegistry.getRulesFingerprint());
            field(SummaryEngine.class, "projectRuntimeKey").set(engine, ProjectStateUtil.runtimeCacheKey());
            field(SummaryEngine.class, "fingerprint").set(engine, "summary-test-fingerprint");

            ProjectModel modelB = ProjectModel.artifact(
                    Path.of("/tmp/jar-analyzer/runtime-b.jar"),
                    null,
                    List.of(Path.of("/tmp/jar-analyzer/runtime-b.jar")),
                    false
            );
            ProjectRuntimeContext.restoreProjectRuntime("summary-project", 7L, modelB);

            Method ensureRuleContext = SummaryEngine.class.getDeclaredMethod("ensureRuleContext");
            ensureRuleContext.setAccessible(true);
            ensureRuleContext.invoke(engine);

            assertNull(cache.get(handle));
        } finally {
            ProjectRuntimeContext.clear();
            DatabaseManager.clearAllData();
        }
    }

    private static Field field(Class<?> type, String name) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }
}
