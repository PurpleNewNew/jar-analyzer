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
    private static final String DEFAULT_DATABASE_NAME = "neo4j";

    private final Neo4jEngineConfig config;
    private final ReentrantLock lifecycleLock = new ReentrantLock();

    private volatile DatabaseManagementService managementService;
    private volatile GraphDatabaseService graphDatabaseService;
    private volatile String activeDatabaseName;
    private volatile boolean databaseSwitchingSupported = true;

    public Neo4jLifecycle(Neo4jEngineConfig config) {
        this.config = config;
        this.activeDatabaseName = normalizeDatabaseName(config.databaseName());
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
                    activeDatabaseName,
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

    public String activeDatabaseName() {
        return activeDatabaseName;
    }

    public boolean isDatabaseSwitchingSupported() {
        return databaseSwitchingSupported;
    }

    public boolean useDatabase(String databaseName) {
        String target = normalizeDatabaseName(databaseName);
        if (target.equals(activeDatabaseName) && graphDatabaseService != null) {
            return true;
        }
        lifecycleLock.lock();
        try {
            if (managementService == null || graphDatabaseService == null) {
                startDatabase();
            }
            if (target.equals(activeDatabaseName) && graphDatabaseService != null) {
                return true;
            }
            DatabaseManagementService local = managementService;
            if (local == null) {
                return false;
            }
            GraphDatabaseService next = openOrCreateDatabase(local, target);
            if (next == null) {
                databaseSwitchingSupported = false;
                logger.warn("switch neo4j database fail: {}, reason=database not found and create unsupported", target);
                return false;
            }
            graphDatabaseService = next;
            activeDatabaseName = target;
            databaseSwitchingSupported = true;
            logger.info("neo4j embedded switched active database: {}", target);
            return true;
        } catch (Exception ex) {
            logger.warn("switch neo4j database fail: {}, reason={}", target, ex.toString());
            return false;
        } finally {
            lifecycleLock.unlock();
        }
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
        GraphDatabaseService selected = openOrCreateDatabase(managementService, activeDatabaseName);
        if (selected != null) {
            graphDatabaseService = selected;
            return;
        }
        if (!DEFAULT_DATABASE_NAME.equals(activeDatabaseName)) {
            GraphDatabaseService fallback = openOrCreateDatabase(managementService, DEFAULT_DATABASE_NAME);
            if (fallback != null) {
                logger.warn(
                        "requested neo4j database not available, fallback to default: requested={}, fallback={}",
                        activeDatabaseName,
                        DEFAULT_DATABASE_NAME
                );
                graphDatabaseService = fallback;
                activeDatabaseName = DEFAULT_DATABASE_NAME;
                databaseSwitchingSupported = false;
                return;
            }
        }
        throw new IllegalStateException("cannot open any neo4j database under home: " + config.homeDir());
    }

    private GraphDatabaseService openOrCreateDatabase(DatabaseManagementService management, String databaseName) {
        try {
            return management.database(databaseName);
        } catch (Exception openEx) {
            try {
                management.createDatabase(databaseName);
            } catch (Exception createEx) {
                logger.debug("create neo4j database fail: {}, reason={}", databaseName, createEx.toString());
            }
            try {
                return management.database(databaseName);
            } catch (Exception retryEx) {
                logger.debug("open neo4j database fail: {}, reason={}", databaseName, retryEx.toString());
                return null;
            }
        }
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

    private static String normalizeDatabaseName(String raw) {
        if (raw == null || raw.isBlank()) {
            return DEFAULT_DATABASE_NAME;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        StringBuilder out = new StringBuilder(normalized.length());
        for (int i = 0; i < normalized.length(); i++) {
            char ch = normalized.charAt(i);
            if ((ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9') || ch == '-') {
                out.append(ch);
            } else {
                out.append('-');
            }
        }
        String cleaned = out.toString().replaceAll("-{2,}", "-");
        cleaned = cleaned.replaceAll("^-+", "").replaceAll("-+$", "");
        if (cleaned.isBlank()) {
            return DEFAULT_DATABASE_NAME;
        }
        if (cleaned.length() > 50) {
            cleaned = cleaned.substring(0, 50);
        }
        if (!Character.isLetter(cleaned.charAt(0))) {
            cleaned = "n-" + cleaned;
        }
        return cleaned;
    }
}
