/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.storage.neo4j;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

import java.util.function.Function;

public final class Neo4jStore {
    private static final Neo4jStore INSTANCE = new Neo4jStore(new Neo4jLifecycle(Neo4jEngineConfig.defaults()));

    private final Neo4jLifecycle lifecycle;

    private Neo4jStore(Neo4jLifecycle lifecycle) {
        this.lifecycle = lifecycle;
    }

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                INSTANCE.stop();
            } catch (Exception ignored) {
            }
        }, "neo4j-store-shutdown"));
    }

    public static Neo4jStore getInstance() {
        return INSTANCE;
    }

    public void start() {
        lifecycle.start();
    }

    public void stop() {
        lifecycle.stop();
    }

    public GraphDatabaseService database() {
        return lifecycle.database();
    }

    public <T> T read(long timeoutMs, Function<Transaction, T> fn) {
        return lifecycle.read(timeoutMs, fn);
    }

    public <T> T write(long timeoutMs, Function<Transaction, T> fn) {
        return lifecycle.write(timeoutMs, fn);
    }

    public void clearAll(long timeoutMs) {
        write(timeoutMs, tx -> {
            tx.execute("MATCH (n) DETACH DELETE n");
            return null;
        });
    }
}
