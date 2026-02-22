/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.storage.neo4j;

import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

import java.nio.file.Path;
import java.util.Locale;
import java.util.function.Function;
import java.util.concurrent.locks.ReentrantLock;

public final class Neo4jStore {
    private static final Logger logger = LogManager.getLogger();
    private static final Neo4jStore INSTANCE = new Neo4jStore(new Neo4jLifecycle(Neo4jEngineConfig.defaults()));

    private final ReentrantLock switchLock = new ReentrantLock();
    private volatile Neo4jLifecycle lifecycle;
    private volatile Neo4jEngineConfig config;

    private Neo4jStore(Neo4jLifecycle lifecycle) {
        this.lifecycle = lifecycle;
        this.config = Neo4jEngineConfig.defaults();
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
        lifecycle().start();
    }

    public void stop() {
        lifecycle().stop();
    }

    public GraphDatabaseService database() {
        return lifecycle().database();
    }

    public boolean useDatabase(String databaseName) {
        return lifecycle().useDatabase(databaseName);
    }

    public String activeDatabaseName() {
        return lifecycle().activeDatabaseName();
    }

    public boolean isDatabaseSwitchingSupported() {
        return lifecycle().isDatabaseSwitchingSupported();
    }

    public Path activeHomeDir() {
        Neo4jEngineConfig cfg = config;
        return cfg == null ? Neo4jEngineConfig.defaults().homeDir() : cfg.homeDir();
    }

    public boolean switchStore(Path homeDir, String databaseName) {
        if (homeDir == null) {
            return false;
        }
        Path normalizedHome = homeDir.toAbsolutePath().normalize();
        String normalizedDb = normalizeDatabaseName(databaseName);
        Neo4jEngineConfig current = config;
        if (current != null
                && normalizedHome.equals(current.homeDir())
                && normalizedDb.equalsIgnoreCase(normalizeDatabaseName(current.databaseName()))) {
            start();
            return true;
        }
        switchLock.lock();
        try {
            current = config;
            if (current != null
                    && normalizedHome.equals(current.homeDir())
                    && normalizedDb.equalsIgnoreCase(normalizeDatabaseName(current.databaseName()))) {
                start();
                return true;
            }
            Neo4jEngineConfig nextCfg = new Neo4jEngineConfig(
                    normalizedHome,
                    normalizedDb,
                    current == null || current.disableConnectors()
            );
            Neo4jLifecycle nextLifecycle = new Neo4jLifecycle(nextCfg);
            nextLifecycle.start();
            Neo4jLifecycle oldLifecycle = lifecycle;
            lifecycle = nextLifecycle;
            config = nextCfg;
            if (oldLifecycle != null) {
                try {
                    oldLifecycle.stop();
                } catch (Exception ex) {
                    logger.debug("stop old neo4j lifecycle fail: {}", ex.toString());
                }
            }
            logger.info("neo4j store switched: home={}, database={}", normalizedHome, normalizedDb);
            return true;
        } catch (Exception ex) {
            logger.warn("neo4j store switch fail: home={}, database={}, reason={}",
                    normalizedHome, normalizedDb, ex.toString());
            return false;
        } finally {
            switchLock.unlock();
        }
    }

    public <T> T read(long timeoutMs, Function<Transaction, T> fn) {
        return lifecycle().read(timeoutMs, fn);
    }

    public <T> T write(long timeoutMs, Function<Transaction, T> fn) {
        return lifecycle().write(timeoutMs, fn);
    }

    public void clearAll(long timeoutMs) {
        write(timeoutMs, tx -> {
            tx.execute("MATCH (n) DETACH DELETE n");
            return null;
        });
    }

    private Neo4jLifecycle lifecycle() {
        Neo4jLifecycle local = lifecycle;
        if (local != null) {
            return local;
        }
        switchLock.lock();
        try {
            if (lifecycle == null) {
                Neo4jEngineConfig defaults = Neo4jEngineConfig.defaults();
                config = defaults;
                lifecycle = new Neo4jLifecycle(defaults);
            }
            return lifecycle;
        } finally {
            switchLock.unlock();
        }
    }

    private static String normalizeDatabaseName(String raw) {
        if (raw == null || raw.isBlank()) {
            return "neo4j";
        }
        return raw.trim().toLowerCase(Locale.ROOT);
    }
}
