/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.core.edge;

import me.n1ar4.jar.analyzer.config.ConfigFile;
import me.n1ar4.jar.analyzer.core.CoreRunner;
import me.n1ar4.jar.analyzer.engine.CoreEngine;
import me.n1ar4.jar.analyzer.engine.EngineContext;
import me.n1ar4.jar.analyzer.engine.WorkspaceContext;
import me.n1ar4.jar.analyzer.entity.CallSiteEntity;
import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.support.FixtureJars;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CallbackEdgeInferenceTest {
    private static final String DB_PATH = Const.dbFile;

    @BeforeAll
    public void buildFixture() {
        Path jar = FixtureJars.callbackTestJar();
        WorkspaceContext.setResolveInnerJars(false);
        CoreRunner.run(jar, null, false, false, true, null, true);
        ConfigFile config = new ConfigFile();
        config.setDbPath(DB_PATH);
        EngineContext.setEngine(new CoreEngine(config));
    }

    @Test
    public void testCallbackEdgesExist() throws Exception {
        // Thread.start -> ThreadSubclass.run callback
        assertEdge(
                "me/n1ar4/cb/CallbackEntry", "threadStart", "()V",
                "me/n1ar4/cb/MyThread", "run", "()V",
                "callback",
                "thread_start"
        );

        // ExecutorService.submit(Runnable) -> Runnable.run callback
        assertEdge(
                "me/n1ar4/cb/CallbackEntry", "executorSubmit", "()V",
                "me/n1ar4/cb/MyRunnable", "run", "()V",
                "callback",
                "executor_runnable"
        );

        // AccessController.doPrivileged -> PrivilegedAction.run callback
        assertEdge(
                "me/n1ar4/cb/CallbackEntry", "doPrivileged", "()Ljava/lang/Object;",
                "me/n1ar4/cb/MyAction", "run", "()Ljava/lang/Object;",
                "callback",
                "doPrivileged"
        );

        // CompletableFuture.supplyAsync -> Supplier.get callback
        assertEdge(
                "me/n1ar4/cb/CallbackEntry", "completableSupply", "()V",
                "me/n1ar4/cb/MySupplier", "get", "()Ljava/lang/Object;",
                "callback",
                "completable_future_supplier"
        );
    }

    @Test
    public void testReflectionEdgesExist() throws Exception {
        assertEdge(
                "me/n1ar4/cb/CallbackEntry", "reflectInvoke", "()V",
                "me/n1ar4/cb/ReflectionTarget", "target", "()V",
                "reflection",
                null
        );
    }

    @Test
    public void testCallSiteQueryByEdgeWorks() {
        CoreEngine engine = EngineContext.getEngine();
        assertNotNull(engine);
        List<CallSiteEntity> sites = engine.getCallSitesByEdge(
                "me/n1ar4/cb/CallbackEntry", "threadStart", "()V",
                "me/n1ar4/cb/MyThread", "start", "()V");
        assertNotNull(sites);
        assertFalse(sites.isEmpty(), "callsite evidence should exist for direct Thread.start call");
    }

    private static void assertEdge(String callerClass,
                                   String callerMethod,
                                   String callerDesc,
                                   String calleeClass,
                                   String calleeMethod,
                                   String calleeDesc,
                                   String expectedType,
                                   String evidenceContains) throws Exception {
        EdgeRow row = selectEdge(callerClass, callerMethod, callerDesc,
                calleeClass, calleeMethod, calleeDesc);
        assertNotNull(row, "edge not found: " + callerClass + "." + callerMethod + " -> " + calleeClass + "." + calleeMethod);
        assertEquals(expectedType, row.type, "edge_type mismatch");
        if (evidenceContains != null) {
            assertNotNull(row.evidence);
            assertTrue(row.evidence.contains(evidenceContains),
                    "edge_evidence should contain: " + evidenceContains + " but was: " + row.evidence);
        }
    }

    private static EdgeRow selectEdge(String callerClass,
                                     String callerMethod,
                                     String callerDesc,
                                     String calleeClass,
                                     String calleeMethod,
                                     String calleeDesc) throws Exception {
        String sql = "SELECT edge_type, edge_confidence, edge_evidence " +
                "FROM method_call_table " +
                "WHERE caller_class_name=? AND caller_method_name=? AND caller_method_desc=? " +
                "AND callee_class_name=? AND callee_method_name=? AND callee_method_desc=? " +
                "LIMIT 1";
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + DB_PATH);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, callerClass);
            stmt.setString(2, callerMethod);
            stmt.setString(3, callerDesc);
            stmt.setString(4, calleeClass);
            stmt.setString(5, calleeMethod);
            stmt.setString(6, calleeDesc);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                EdgeRow row = new EdgeRow();
                row.type = rs.getString(1);
                row.confidence = rs.getString(2);
                row.evidence = rs.getString(3);
                return row;
            }
        }
    }

    private static final class EdgeRow {
        private String type;
        private String confidence;
        private String evidence;
    }
}
