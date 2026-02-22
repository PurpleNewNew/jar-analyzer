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

import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import org.neo4j.configuration.GraphDatabaseSettings;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder;
import org.neo4j.graphdb.GraphDatabaseService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class Neo4jProjectStore {
    private static final Logger logger = LogManager.getLogger();
    private static final Neo4jProjectStore INSTANCE = new Neo4jProjectStore();
    private static final String PROJECT_DB_DIR = "neo4j-projects";

    private final Map<String, StoreRuntime> runtimes = new ConcurrentHashMap<>();
    private final Object initLock = new Object();

    private Neo4jProjectStore() {
        Runtime.getRuntime().addShutdownHook(Thread.ofPlatform()
                .name("neo4j-project-store-shutdown")
                .daemon(true)
                .unstarted(this::shutdownAll));
    }

    public static Neo4jProjectStore getInstance() {
        return INSTANCE;
    }

    public GraphDatabaseService activeDatabase() {
        return database(ActiveProjectContext.getActiveProjectKey());
    }

    public GraphDatabaseService database(String projectKey) {
        String normalized = ActiveProjectContext.normalizeProjectKey(projectKey);
        StoreRuntime cached = runtimes.get(normalized);
        if (cached != null) {
            return cached.databaseService;
        }
        synchronized (initLock) {
            StoreRuntime existing = runtimes.get(normalized);
            if (existing != null) {
                return existing.databaseService;
            }
            try {
                Path home = resolveProjectHome(normalized);
                Files.createDirectories(home);
                DatabaseManagementService managementService = new DatabaseManagementServiceBuilder(home).build();
                GraphDatabaseService database = managementService.database(GraphDatabaseSettings.DEFAULT_DATABASE_NAME);
                StoreRuntime runtime = new StoreRuntime(normalized, home, managementService, database);
                runtimes.put(normalized, runtime);
                ensureConstraints(database);
                logger.info("neo4j project opened: key={} home={}", normalized, home);
                return database;
            } catch (Exception ex) {
                logger.error("open neo4j project store fail: key={} err={}", normalized, ex.toString());
                throw new IllegalStateException("neo4j_store_open_fail", ex);
            }
        }
    }

    public Path resolveProjectHome(String projectKey) {
        String normalized = ActiveProjectContext.normalizeProjectKey(projectKey);
        return Paths.get(Const.dbDir, PROJECT_DB_DIR, normalized).toAbsolutePath().normalize();
    }

    public void closeProject(String projectKey) {
        String normalized = ActiveProjectContext.normalizeProjectKey(projectKey);
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

    public void deleteProjectStore(String projectKey) {
        String normalized = ActiveProjectContext.normalizeProjectKey(projectKey);
        closeProject(normalized);
        Path home = resolveProjectHome(normalized);
        try {
            deleteRecursively(home);
        } catch (Exception ex) {
            logger.warn("delete neo4j project store fail: key={} err={}", normalized, ex.toString());
        }
    }

    public void shutdownAll() {
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
        try (var walk = Files.walk(root)) {
            walk.sorted((a, b) -> b.getNameCount() - a.getNameCount())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (Exception ignored) {
                            // best effort
                        }
                    });
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
