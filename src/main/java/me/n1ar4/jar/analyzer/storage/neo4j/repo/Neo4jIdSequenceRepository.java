/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.storage.neo4j.repo;

import me.n1ar4.jar.analyzer.storage.neo4j.Neo4jStore;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public final class Neo4jIdSequenceRepository {
    private static final int DEFAULT_BLOCK_SIZE = 4096;

    private final Neo4jStore store;
    private final ConcurrentHashMap<String, SequenceWindow> windows = new ConcurrentHashMap<>();
    private final int blockSize;

    public Neo4jIdSequenceRepository(Neo4jStore store) {
        this(store, resolveBlockSize());
    }

    Neo4jIdSequenceRepository(Neo4jStore store, int blockSize) {
        this.store = store;
        this.blockSize = Math.max(32, blockSize);
    }

    public long next(String name) {
        return reserve(name, 1);
    }

    public long reserve(String name, int size) {
        String seqName = normalizeName(name);
        int step = Math.max(1, size);
        SequenceWindow window = windows.computeIfAbsent(seqName, ignore -> new SequenceWindow());
        synchronized (window) {
            long cached = window.allocate(step);
            if (cached > 0L) {
                return cached;
            }
            int fetch = Math.max(step, blockSize);
            long start = reserveFromStore(seqName, fetch);
            if (fetch > step) {
                window.refill(start + step, start + fetch);
            } else {
                window.clear();
            }
            return start;
        }
    }

    public void clearCache() {
        windows.clear();
    }

    private static String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            return "default";
        }
        return name.trim();
    }

    private long reserveFromStore(String seqName, int step) {
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

    private static int resolveBlockSize() {
        String raw = System.getProperty("jar.analyzer.idseq.block");
        if (raw == null || raw.isBlank()) {
            return DEFAULT_BLOCK_SIZE;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (Exception ignored) {
            return DEFAULT_BLOCK_SIZE;
        }
    }

    private static final class SequenceWindow {
        private long next = 0L;
        private long endExclusive = 0L;

        private long allocate(int size) {
            if (size <= 0) {
                return -1L;
            }
            if (next <= 0L || endExclusive <= next) {
                return -1L;
            }
            long start = next;
            long candidate = start + size;
            if (candidate > endExclusive) {
                return -1L;
            }
            next = candidate;
            return start;
        }

        private void refill(long start, long endExclusive) {
            this.next = Math.max(0L, start);
            this.endExclusive = Math.max(this.next, endExclusive);
        }

        private void clear() {
            this.next = 0L;
            this.endExclusive = 0L;
        }
    }
}
