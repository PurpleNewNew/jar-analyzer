/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.storage.neo4j.repo;

import me.n1ar4.jar.analyzer.storage.neo4j.Neo4jStore;

import java.util.Map;

public final class Neo4jIdSequenceRepository {
    private final Neo4jStore store;

    public Neo4jIdSequenceRepository(Neo4jStore store) {
        this.store = store;
    }

    public long next(String name) {
        return reserve(name, 1);
    }

    public long reserve(String name, int size) {
        String seqName = normalizeName(name);
        int step = Math.max(1, size);
        return store.write(30_000L, tx -> {
            Object raw = tx.execute(
                            "MERGE (s:IdSequence {name:$name}) " +
                                    "ON CREATE SET s.next = 1 " +
                                    "WITH s, coalesce(s.next, 1) AS start " +
                                    "SET s.next = start + $step " +
                                    "RETURN start AS start",
                            Map.of("name", seqName, "step", step))
                    .next()
                    .get("start");
            if (raw instanceof Number number) {
                return number.longValue();
            }
            try {
                return Long.parseLong(String.valueOf(raw));
            } catch (Exception ignored) {
                return 1L;
            }
        });
    }

    private static String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            return "default";
        }
        return name.trim();
    }
}
