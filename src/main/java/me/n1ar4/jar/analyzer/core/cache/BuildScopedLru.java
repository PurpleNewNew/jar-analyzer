/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.core.cache;

import me.n1ar4.jar.analyzer.utils.InterruptUtil;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * A small, thread-safe LRU cache that is automatically invalidated when the epoch changes.
 * <p>
 * Typical epoch is {@code DatabaseManager#getBuildSeq()}.
 */
public final class BuildScopedLru<K, V> {
    private static final Logger logger = LogManager.getLogger();
    private final Object lock = new Object();
    private final int capacity;
    private final Supplier<Long> epochSupplier;
    private volatile long lastEpoch;
    private final LinkedHashMap<K, V> map;

    public BuildScopedLru(int capacity, Supplier<Long> epochSupplier) {
        this.capacity = Math.max(1, capacity);
        this.epochSupplier = epochSupplier == null ? () -> 0L : epochSupplier;
        this.lastEpoch = safeEpoch();
        this.map = new LinkedHashMap<K, V>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > BuildScopedLru.this.capacity;
            }
        };
    }

    public V get(K key) {
        if (key == null) {
            return null;
        }
        ensureFresh();
        synchronized (lock) {
            return map.get(key);
        }
    }

    public void put(K key, V value) {
        if (key == null || value == null) {
            return;
        }
        ensureFresh();
        synchronized (lock) {
            map.put(key, value);
        }
    }

    public void clear() {
        synchronized (lock) {
            map.clear();
        }
    }

    public int size() {
        synchronized (lock) {
            return map.size();
        }
    }

    private void ensureFresh() {
        long epoch = safeEpoch();
        if (epoch == lastEpoch) {
            return;
        }
        synchronized (lock) {
            epoch = safeEpoch();
            if (epoch != lastEpoch) {
                map.clear();
                lastEpoch = epoch;
            }
        }
    }

    private long safeEpoch() {
        try {
            Long v = epochSupplier.get();
            return v == null ? 0L : v;
        } catch (Throwable t) {
            InterruptUtil.restoreInterruptIfNeeded(t);
            if (t instanceof Error) {
                throw (Error) t;
            }
            logger.debug("epoch supplier failed: {}", t.toString());
            return 0L;
        }
    }
}
