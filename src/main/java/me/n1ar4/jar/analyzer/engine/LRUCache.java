/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.engine;

import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * LRU Cache
 */
public class LRUCache {
    private static final Logger logger = LogManager.getLogger();
    private final int capacity;
    private final LinkedHashMap<String, String> map;

    public LRUCache(int capacity) {
        this.capacity = Math.max(1, capacity);
        this.map = new LinkedHashMap<String, String>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
                boolean evict = size() > LRUCache.this.capacity;
                if (evict) {
                    logger.debug("LRU EVICT - capacity : {} - size : {} - key : {}",
                            LRUCache.this.capacity, size(), eldest == null ? null : eldest.getKey());
                }
                return evict;
            }
        };
    }

    /**
     * Get Cache
     *
     * @param key String KEY
     * @return String CODE
     */
    public synchronized String get(String key) {
        if (key == null) {
            return null;
        }
        return map.get(key);
    }

    /**
     * Put Cache
     *
     * @param key   String KEY
     * @param value String CODE
     */
    public synchronized void put(String key, String value) {
        if (key == null || value == null) {
            return;
        }
        map.put(key, value);
    }
}
