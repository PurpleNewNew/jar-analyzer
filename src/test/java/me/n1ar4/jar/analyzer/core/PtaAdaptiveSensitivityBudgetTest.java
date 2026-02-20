/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.core;

import org.junit.jupiter.api.Tag;

import me.n1ar4.jar.analyzer.core.pta.ContextSensitivePtaEngine;
import me.n1ar4.jar.analyzer.engine.WorkspaceContext;
import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.support.FixtureJars;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("legacy-sqlite")
public class PtaAdaptiveSensitivityBudgetTest {
    private static final String PROP_MODE = "jar.analyzer.callgraph.mode";
    private static final String PROP_INCREMENTAL = "jar.analyzer.pta.incremental";
    private static final String PROP_ADAPTIVE = "jar.analyzer.pta.adaptive.enable";
    private static final String PROP_MAX_CTX_METHOD = "jar.analyzer.pta.adaptive.max.contexts.per.method";
    private static final String PROP_MAX_CTX_SITE = "jar.analyzer.pta.adaptive.max.contexts.per.site";
    private static final String PROP_HUGE_DISPATCH = "jar.analyzer.pta.adaptive.huge.dispatch.threshold";
    private static final String PROP_OBJ_WIDEN = "jar.analyzer.pta.adaptive.object.widen.context.threshold";

    @Test
    public void shouldKeepCorePrecisionUnderTightAdaptiveBudgets() throws Exception {
        Map<String, String> old = snapshot(
                PROP_MODE,
                PROP_INCREMENTAL,
                PROP_ADAPTIVE,
                PROP_MAX_CTX_METHOD,
                PROP_MAX_CTX_SITE,
                PROP_HUGE_DISPATCH,
                PROP_OBJ_WIDEN
        );
        try {
            ContextSensitivePtaEngine.clearIncrementalCache();
            System.setProperty(PROP_MODE, "pta");
            System.setProperty(PROP_INCREMENTAL, "false");
            System.setProperty(PROP_ADAPTIVE, "true");
            System.setProperty(PROP_MAX_CTX_METHOD, "2");
            System.setProperty(PROP_MAX_CTX_SITE, "1");
            System.setProperty(PROP_HUGE_DISPATCH, "2");
            System.setProperty(PROP_OBJ_WIDEN, "1");

            Path jar = FixtureJars.callbackTestJar();
            WorkspaceContext.updateResolveInnerJars(false);
            CoreRunner.run(jar, null, false, false, true, null, true);

            assertTrue(exists("SELECT COUNT(*) FROM method_call_table WHERE edge_type='pta'"),
                    "pta edges should still exist under tight adaptive budget");
            assertTrue(exists("SELECT COUNT(*) FROM method_call_table WHERE edge_evidence LIKE '%pta_ctx=%'"),
                    "pta context evidence should still exist under adaptive widening");

            assertTrue(exists("SELECT COUNT(*) FROM method_call_table "
                            + "WHERE caller_class_name='me/n1ar4/cb/CallbackEntry' "
                            + "AND caller_method_name='ptaFieldSensitiveDispatch' "
                            + "AND callee_class_name='me/n1ar4/cb/FastTask' "
                            + "AND callee_method_name='run' AND callee_method_desc='()V'"),
                    "tight adaptive budget should not drop core field-sensitive dispatch edge");

            assertTrue(exists("SELECT COUNT(*) FROM method_call_table "
                            + "WHERE caller_class_name='me/n1ar4/cb/CallbackEntry' "
                            + "AND caller_method_name='reflectViaHelperFlow' "
                            + "AND callee_class_name='me/n1ar4/cb/ReflectionTarget' "
                            + "AND callee_method_name='target' AND callee_method_desc='()V' "
                            + "AND edge_type='reflection'"),
                    "tight adaptive budget should not drop helper reflection closure");

            assertTrue(exists("SELECT COUNT(*) FROM method_call_table "
                            + "WHERE caller_class_name='me/n1ar4/cb/CallbackEntry' "
                            + "AND caller_method_name='methodHandleViaHelperFlow' "
                            + "AND callee_class_name='me/n1ar4/cb/ReflectionTarget' "
                            + "AND callee_method_name='target' AND callee_method_desc='()V' "
                            + "AND edge_type='method_handle'"),
                    "tight adaptive budget should not drop helper MethodHandle closure");
        } finally {
            ContextSensitivePtaEngine.clearIncrementalCache();
            restore(old);
        }
    }

    private static Map<String, String> snapshot(String... keys) {
        HashMap<String, String> out = new HashMap<>();
        if (keys == null) {
            return out;
        }
        for (String key : keys) {
            out.put(key, System.getProperty(key));
        }
        return out;
    }

    private static void restore(Map<String, String> old) {
        if (old == null || old.isEmpty()) {
            return;
        }
        for (Map.Entry<String, String> entry : old.entrySet()) {
            if (entry.getValue() == null) {
                System.clearProperty(entry.getKey());
            } else {
                System.setProperty(entry.getKey(), entry.getValue());
            }
        }
    }

    private static boolean exists(String sql) throws Exception {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + Const.dbFile);
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() && rs.getInt(1) > 0;
        }
    }
}
