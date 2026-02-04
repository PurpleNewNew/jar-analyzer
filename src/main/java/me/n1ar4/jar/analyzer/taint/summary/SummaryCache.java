/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */
package me.n1ar4.jar.analyzer.taint.summary;

import me.n1ar4.jar.analyzer.core.reference.MethodReference;

import java.util.LinkedHashMap;
import java.util.Map;

public final class SummaryCache {
    private final int maxSize;
    private final Map<MethodReference.Handle, MethodSummary> cache;

    public SummaryCache(int maxSize) {
        this.maxSize = Math.max(128, maxSize);
        this.cache = new LinkedHashMap<MethodReference.Handle, MethodSummary>(this.maxSize, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<MethodReference.Handle, MethodSummary> eldest) {
                return size() > SummaryCache.this.maxSize;
            }
        };
    }

    public synchronized MethodSummary get(MethodReference.Handle key) {
        return cache.get(key);
    }

    public synchronized void put(MethodReference.Handle key, MethodSummary summary) {
        if (key == null || summary == null) {
            return;
        }
        cache.put(key, summary);
    }
}
