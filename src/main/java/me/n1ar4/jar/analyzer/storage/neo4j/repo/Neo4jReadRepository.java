/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.storage.neo4j.repo;

import me.n1ar4.jar.analyzer.storage.neo4j.Neo4jStore;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class Neo4jReadRepository {
    private final Neo4jStore store;

    public Neo4jReadRepository(Neo4jStore store) {
        this.store = store;
    }

    public List<Map<String, Object>> list(String cypher) {
        return list(cypher, Collections.emptyMap(), 60_000L);
    }

    public List<Map<String, Object>> list(String cypher, Map<String, Object> params, long timeoutMs) {
        return store.read(timeoutMs, tx -> queryList(tx, cypher, params));
    }

    public Map<String, Object> one(String cypher, Map<String, Object> params, long timeoutMs) {
        List<Map<String, Object>> rows = list(cypher, params, timeoutMs);
        if (rows.isEmpty()) {
            return Collections.emptyMap();
        }
        return rows.get(0);
    }

    public long count(String cypher, Map<String, Object> params, String field, long timeoutMs) {
        Map<String, Object> row = one(cypher, params, timeoutMs);
        Object raw = row.get(field);
        if (raw instanceof Number n) {
            return n.longValue();
        }
        if (raw == null) {
            return 0L;
        }
        try {
            return Long.parseLong(String.valueOf(raw));
        } catch (Exception ignored) {
            return 0L;
        }
    }

    public <T> T read(long timeoutMs, Function<Transaction, T> fn) {
        return store.read(timeoutMs, fn);
    }

    private static List<Map<String, Object>> queryList(Transaction tx,
                                                       String cypher,
                                                       Map<String, Object> params) {
        List<Map<String, Object>> out = new ArrayList<>();
        try (Result rs = tx.execute(cypher, params == null ? Collections.emptyMap() : params)) {
            while (rs.hasNext()) {
                out.add(rs.next());
            }
        }
        return out;
    }
}
