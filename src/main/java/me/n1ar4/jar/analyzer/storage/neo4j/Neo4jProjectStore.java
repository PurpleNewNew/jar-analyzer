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

import me.n1ar4.jar.analyzer.storage.neo4j.procedure.JaNativeRegistration;
import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import org.neo4j.configuration.GraphDatabaseSettings;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder;
import org.neo4j.graphdb.GraphDatabaseService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class Neo4jProjectStore {
    private static final Logger logger = LogManager.getLogger();
    private static final Neo4jProjectStore INSTANCE = new Neo4jProjectStore();
    private static final String TEMP_DB_DIR = "neo4j-temp";
    private static final String PROJECT_DB_DIR = "neo4j-projects";
    // Neo4j may release log/file handles slightly after shutdown on Windows.
    // Deleting project stores is part of registry rollback, so give the store
    // a longer window to quiesce instead of leaking half-created homes.
    private static final int DELETE_RETRY_COUNT = 20;
    private static final long DELETE_RETRY_DELAY_MS = 200L;

    private final Map<String, StoreRuntime> runtimes = new ConcurrentHashMap<>();
    private final Object initLock = new Object();
    private final Set<String> importLocks = new HashSet<>();

    private Neo4jProjectStore() {
        Runtime.getRuntime().addShutdownHook(Thread.ofPlatform()
                .name("neo4j-project-store-shutdown")
                .daemon(false)
                .unstarted(() -> {
                    shutdownAll();
                    cleanupCurrentSessionTemporaryStore();
                }));
    }

    public static Neo4jProjectStore getInstance() {
        return INSTANCE;
    }

    public GraphDatabaseService activeDatabase() {
        return database(ActiveProjectContext.getActiveProjectKey());
    }

    public GraphDatabaseService database(String projectKey) {
        String normalized = ActiveProjectContext.resolveRequestedOrActive(projectKey);
        StoreRuntime cached = runtimes.get(normalized);
        if (cached != null) {
            return cached.databaseService;
        }
        synchronized (initLock) {
            StoreRuntime existing = runtimes.get(normalized);
            if (existing != null) {
                return existing.databaseService;
            }
            if (importLocks.contains(normalized)) {
                throw new IllegalStateException("neo4j_project_import_in_progress");
            }
            Path home = null;
            boolean homeExisted = false;
            try {
                home = resolveProjectHome(normalized);
                homeExisted = Files.exists(home);
                Files.createDirectories(home);
                DatabaseManagementService managementService = new DatabaseManagementServiceBuilder(home).build();
                GraphDatabaseService database = managementService.database(GraphDatabaseSettings.DEFAULT_DATABASE_NAME);
                StoreRuntime runtime = new StoreRuntime(normalized, home, managementService, database);
                runtimes.put(normalized, runtime);
                JaNativeRegistration.register(database);
                ensureConstraints(database);
                logger.info("neo4j project opened: key={} home={}", normalized, home);
                return database;
            } catch (Exception ex) {
                if (!homeExisted) {
                    try {
                        deleteRecursively(home);
                    } catch (Exception cleanupEx) {
                        logger.debug("cleanup neo4j project home after open failure fail: key={} err={}",
                                normalized, cleanupEx.toString());
                    }
                }
                logger.error("open neo4j project store fail: key={} err={}", normalized, ex.toString());
                throw new IllegalStateException("graph_store_open_fail", ex);
            }
        }
    }

    public void beginProjectImport(String projectKey) {
        String normalized = ActiveProjectContext.resolveRequestedOrActive(projectKey);
        synchronized (initLock) {
            importLocks.add(normalized);
            StoreRuntime runtime = runtimes.remove(normalized);
            if (runtime == null) {
                return;
            }
            try {
                runtime.managementService.shutdown();
            } catch (Exception ex) {
                logger.warn("shutdown neo4j project for import fail: key={} err={}", normalized, ex.toString());
            }
        }
    }

    public void endProjectImport(String projectKey) {
        String normalized = ActiveProjectContext.resolveRequestedOrActive(projectKey);
        synchronized (initLock) {
            importLocks.remove(normalized);
        }
    }

    public Path resolveProjectHome(String projectKey) {
        String normalized = ActiveProjectContext.resolveRequestedOrActive(projectKey);
        if (ActiveProjectContext.isTemporaryProjectKey(normalized)) {
            String sessionId = ActiveProjectContext.extractTemporarySessionId(normalized);
            return Paths.get(Const.dbDir, TEMP_DB_DIR, sessionId).toAbsolutePath().normalize();
        }
        return Paths.get(Const.dbDir, PROJECT_DB_DIR, normalized).toAbsolutePath().normalize();
    }

    public void closeProject(String projectKey) {
        String normalized = ActiveProjectContext.resolveRequestedOrActive(projectKey);
        synchronized (initLock) {
            StoreRuntime runtime = runtimes.remove(normalized);
            if (runtime == null) {
                return;
            }
            try {
                runtime.managementService.shutdown();
            } catch (Exception ex) {
                logger.warn("shutdown neo4j project fail: key={} err={}", normalized, ex.toString());
            }
        }
    }

    public void deleteProjectStore(String projectKey) {
        String normalized = ActiveProjectContext.resolveRequestedOrActive(projectKey);
        closeProject(normalized);
        Path home = resolveProjectHome(normalized);
        Exception lastFailure = null;
        for (int attempt = 1; attempt <= DELETE_RETRY_COUNT; attempt++) {
            try {
                deleteRecursively(home);
                return;
            } catch (Exception ex) {
                lastFailure = ex;
                logger.debug("delete neo4j project store retry: key={} attempt={} err={}",
                        normalized, attempt, ex.toString());
            }
            if (attempt >= DELETE_RETRY_COUNT) {
                break;
            }
            try {
                Thread.sleep(DELETE_RETRY_DELAY_MS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                IllegalStateException failure = new IllegalStateException("graph_store_delete_fail", ex);
                if (lastFailure != null) {
                    failure.addSuppressed(lastFailure);
                }
                throw failure;
            }
        }
        throw new IllegalStateException("graph_store_delete_fail", lastFailure);
    }

    public void shutdownAll() {
        synchronized (initLock) {
            for (Map.Entry<String, StoreRuntime> entry : runtimes.entrySet()) {
                StoreRuntime runtime = entry.getValue();
                if (runtime == null) {
                    continue;
                }
                try {
                    runtime.managementService.shutdown();
                } catch (Exception ex) {
                    logger.debug("shutdown neo4j runtime fail: key={} err={}", entry.getKey(), ex.toString());
                }
            }
            runtimes.clear();
        }
    }

    private static void ensureConstraints(GraphDatabaseService database) {
        if (database == null) {
            return;
        }
        try (var tx = database.beginTx()) {
            tx.execute("CREATE CONSTRAINT ja_node_id IF NOT EXISTS FOR (n:JANode) REQUIRE n.node_id IS UNIQUE");
            tx.execute("CREATE INDEX ja_node_sig IF NOT EXISTS FOR (n:JANode) ON (n.class_name, n.method_name, n.method_desc)");
            tx.execute("CREATE INDEX ja_node_kind IF NOT EXISTS FOR (n:JANode) ON (n.kind)");
            tx.commit();
        } catch (Exception ex) {
            logger.debug("ensure neo4j constraints fail: {}", ex.toString());
        }
    }

    private static void deleteRecursively(Path root) throws Exception {
        if (root == null || !Files.exists(root)) {
            return;
        }
        IOException firstFailure = null;
        try (var walk = Files.walk(root)) {
            for (Path path : walk.sorted((a, b) -> b.getNameCount() - a.getNameCount()).toList()) {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ex) {
                    if (firstFailure == null) {
                        firstFailure = ex;
                    }
                }
            }
        }
        if (firstFailure != null) {
            throw firstFailure;
        }
        if (Files.exists(root)) {
            throw new IOException("neo4j project store still exists: " + root);
        }
    }

    private void cleanupCurrentSessionTemporaryStore() {
        try {
            deleteProjectStore(ActiveProjectContext.temporaryProjectKey());
        } catch (Exception ex) {
            logger.debug("cleanup current session temp store fail: {}", ex.toString());
        }
    }

    private record StoreRuntime(String projectKey,
                                Path home,
                                DatabaseManagementService managementService,
                                GraphDatabaseService databaseService) {
        private StoreRuntime {
            Objects.requireNonNull(projectKey, "projectKey");
            Objects.requireNonNull(home, "home");
            Objects.requireNonNull(managementService, "managementService");
            Objects.requireNonNull(databaseService, "databaseService");
        }
    }
}
