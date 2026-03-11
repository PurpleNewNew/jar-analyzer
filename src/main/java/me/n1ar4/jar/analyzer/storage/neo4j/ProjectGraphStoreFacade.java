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

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public final class ProjectGraphStoreFacade {
    private static final ProjectGraphStoreFacade INSTANCE =
            new ProjectGraphStoreFacade(Neo4jProjectStore.getInstance());

    private final Neo4jProjectStore store;

    ProjectGraphStoreFacade(Neo4jProjectStore store) {
        this.store = Objects.requireNonNull(store, "store");
    }

    public static ProjectGraphStoreFacade getInstance() {
        return INSTANCE;
    }

    public GraphDatabaseService activeDatabase() {
        return store.activeDatabase();
    }

    public GraphDatabaseService database(String projectKey) {
        return store.database(projectKey);
    }

    public Path resolveProjectHome(String projectKey) {
        return store.resolveProjectHome(projectKey);
    }

    public void beginProjectImport(String projectKey) {
        store.beginProjectImport(projectKey);
    }

    public void endProjectImport(String projectKey) {
        store.endProjectImport(projectKey);
    }

    public void closeProject(String projectKey) {
        store.closeProject(projectKey);
    }

    public void deleteProjectStore(String projectKey) {
        store.deleteProjectStore(projectKey);
    }

    public <T> T read(String projectKey, long timeoutMs, Function<Transaction, T> reader) {
        return runInTx(database(projectKey), timeoutMs, reader);
    }

    public <T> T readActive(long timeoutMs, Function<Transaction, T> reader) {
        return runInTx(activeDatabase(), timeoutMs, reader);
    }

    public <T> T write(String projectKey, long timeoutMs, Function<Transaction, T> writer) {
        return runInTx(database(projectKey), timeoutMs, writer);
    }

    public <T> T writeActive(long timeoutMs, Function<Transaction, T> writer) {
        return runInTx(activeDatabase(), timeoutMs, writer);
    }

    private static <T> T runInTx(GraphDatabaseService database,
                                 long timeoutMs,
                                 Function<Transaction, T> fn) {
        Objects.requireNonNull(database, "database");
        Objects.requireNonNull(fn, "fn");
        long effectiveTimeoutMs = Math.max(100L, timeoutMs);
        try (Transaction tx = database.beginTx(effectiveTimeoutMs, TimeUnit.MILLISECONDS)) {
            T out = fn.apply(tx);
            tx.commit();
            return out;
        }
    }
}
