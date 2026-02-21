/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.storage.neo4j;

import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import org.neo4j.configuration.connectors.BoltConnector;
import org.neo4j.configuration.connectors.HttpConnector;
import org.neo4j.configuration.connectors.HttpsConnector;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.stream.Stream;

public final class Neo4jLifecycle {
    private static final Logger logger = LogManager.getLogger();

    private final Neo4jEngineConfig config;
    private final ReentrantLock lifecycleLock = new ReentrantLock();

    private volatile DatabaseManagementService managementService;
    private volatile GraphDatabaseService graphDatabaseService;

    public Neo4jLifecycle(Neo4jEngineConfig config) {
        this.config = config;
    }

    public void start() {
        if (managementService != null && graphDatabaseService != null) {
            return;
        }
        lifecycleLock.lock();
        try {
            if (managementService != null && graphDatabaseService != null) {
                return;
            }
            try {
                Files.createDirectories(config.homeDir());
            } catch (IOException ex) {
                throw new IllegalStateException("cannot create neo4j home dir: " + config.homeDir(), ex);
            }

            try {
                startDatabase();
            } catch (RuntimeException ex) {
                if (!isIncompatibleStore(ex)) {
                    throw ex;
                }
                logger.warn("detected incompatible neo4j store, rebuild once: home={}, reason={}",
                        config.homeDir(),
                        ex.getMessage());
                resetHomeDir(config.homeDir());
                startDatabase();
            }
            logger.info("neo4j embedded started: home={}, database={}, connectorsDisabled={}",
                    config.homeDir(),
                    config.databaseName(),
                    config.disableConnectors());
        } finally {
            lifecycleLock.unlock();
        }
    }

    public void stop() {
        lifecycleLock.lock();
        try {
            DatabaseManagementService local = managementService;
            managementService = null;
            graphDatabaseService = null;
            if (local != null) {
                local.shutdown();
                logger.info("neo4j embedded stopped");
            }
        } finally {
            lifecycleLock.unlock();
        }
    }

    public GraphDatabaseService database() {
        GraphDatabaseService local = graphDatabaseService;
        if (local != null) {
            return local;
        }
        start();
        return graphDatabaseService;
    }

    public <T> T read(long timeoutMs, Function<Transaction, T> fn) {
        return executeTx(timeoutMs, fn);
    }

    public <T> T write(long timeoutMs, Function<Transaction, T> fn) {
        return executeTx(timeoutMs, fn);
    }

    private <T> T executeTx(long timeoutMs, Function<Transaction, T> fn) {
        long txTimeout = Math.max(100L, timeoutMs);
        GraphDatabaseService db = database();
        try {
            return runInTx(db, txTimeout, fn);
        } catch (RuntimeException ex) {
            if (!isIncompatibleStore(ex)) {
                throw ex;
            }
            lifecycleLock.lock();
            try {
                if (isIncompatibleStore(ex)) {
                    logger.warn("detected incompatible neo4j store during tx, rebuild once: home={}, reason={}",
                            config.homeDir(),
                            ex.getMessage());
                    resetHomeDir(config.homeDir());
                    startDatabase();
                }
            } finally {
                lifecycleLock.unlock();
            }
            return runInTx(database(), txTimeout, fn);
        }
    }

    private DatabaseManagementService buildManagementService() {
        if (!config.disableConnectors()) {
            return new DatabaseManagementServiceBuilder(config.homeDir()).build();
        }
        try {
            DatabaseManagementServiceBuilder builder = new DatabaseManagementServiceBuilder(config.homeDir());
            builder.setConfig(BoltConnector.enabled, false);
            builder.setConfig(HttpConnector.enabled, false);
            builder.setConfig(HttpsConnector.enabled, false);
            return builder.build();
        } catch (IllegalArgumentException ex) {
            if (!isConnectorSettingUnsupported(ex)) {
                throw ex;
            }
            logger.warn("connector settings not exposed by neo4lite, continue with embedded default: {}", ex.getMessage());
            return new DatabaseManagementServiceBuilder(config.homeDir()).build();
        }
    }

    private boolean isConnectorSettingUnsupported(IllegalArgumentException ex) {
        String message = ex == null ? "" : String.valueOf(ex.getMessage());
        String normalized = message.toLowerCase(Locale.ROOT);
        return normalized.contains("server.bolt.enabled")
                || normalized.contains("server.http.enabled")
                || normalized.contains("server.https.enabled");
    }

    private void startDatabase() {
        managementService = buildManagementService();
        graphDatabaseService = managementService.database(config.databaseName());
    }

    private <T> T runInTx(GraphDatabaseService db, long txTimeout, Function<Transaction, T> fn) {
        try (Transaction tx = db.beginTx(txTimeout, TimeUnit.MILLISECONDS)) {
            T out = fn.apply(tx);
            tx.commit();
            return out;
        }
    }

    private boolean isIncompatibleStore(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            String message = String.valueOf(current.getMessage()).toLowerCase(Locale.ROOT);
            if (message.contains("index provider")
                    && (message.contains("fulltext-1.0")
                    || message.contains("vector-2.0")
                    || message.contains("provider not found"))) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private void resetHomeDir(Path homeDir) {
        DatabaseManagementService local = managementService;
        managementService = null;
        graphDatabaseService = null;
        if (local != null) {
            try {
                local.shutdown();
            } catch (Exception ignored) {
                // best effort
            }
        }
        if (!Files.exists(homeDir)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(homeDir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ioException) {
                    throw new IllegalStateException("cannot clean incompatible neo4j home: " + homeDir, ioException);
                }
            });
            Files.createDirectories(homeDir);
        } catch (IOException ex) {
            throw new IllegalStateException("cannot clean incompatible neo4j home: " + homeDir, ex);
        }
    }
}
