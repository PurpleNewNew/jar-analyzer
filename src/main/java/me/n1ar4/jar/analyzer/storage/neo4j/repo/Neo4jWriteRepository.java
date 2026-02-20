/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.storage.neo4j.repo;

import me.n1ar4.jar.analyzer.storage.neo4j.Neo4jStore;
import org.neo4j.graphdb.Transaction;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class Neo4jWriteRepository {
    private final Neo4jStore store;

    public Neo4jWriteRepository(Neo4jStore store) {
        this.store = store;
    }

    public void run(String cypher) {
        run(cypher, Collections.emptyMap(), 60_000L);
    }

    public void run(String cypher, Map<String, Object> params, long timeoutMs) {
        store.write(timeoutMs, tx -> {
            tx.execute(cypher, params == null ? Collections.emptyMap() : params);
            return null;
        });
    }

    public void runBatched(String cypher, List<Map<String, Object>> rows, String rowsParam, long timeoutMs) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        String param = (rowsParam == null || rowsParam.isBlank()) ? "rows" : rowsParam;
        run(cypher, Map.of(param, rows), timeoutMs);
    }

    public <T> T write(long timeoutMs, Function<Transaction, T> fn) {
        return store.write(timeoutMs, fn);
    }
}
