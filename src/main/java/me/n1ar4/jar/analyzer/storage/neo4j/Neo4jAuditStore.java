/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.storage.neo4j;

import me.n1ar4.jar.analyzer.storage.AuditStore;

public final class Neo4jAuditStore implements AuditStore {
    private final Neo4jStore store;

    public Neo4jAuditStore() {
        this(Neo4jStore.getInstance());
    }

    public Neo4jAuditStore(Neo4jStore store) {
        this.store = store;
    }

    @Override
    public String engineName() {
        return "neo4j-embedded-lite";
    }

    @Override
    public void start() {
        store.start();
    }

    @Override
    public void stop() {
        store.stop();
    }

    public Neo4jStore store() {
        return store;
    }
}
