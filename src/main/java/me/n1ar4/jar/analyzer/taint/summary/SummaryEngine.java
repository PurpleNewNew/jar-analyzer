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
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

public final class SummaryEngine {
    private static final Logger logger = LogManager.getLogger();
    private static final String PROP_SUMMARY_CACHE = "jar.analyzer.taint.summary.cache.size";
    private final SummaryCache cache;
    private final SummaryBuilder builder;

    public SummaryEngine() {
        int size = 2048;
        try {
            String raw = System.getProperty(PROP_SUMMARY_CACHE);
            if (raw != null && !raw.trim().isEmpty()) {
                size = Integer.parseInt(raw.trim());
            }
        } catch (NumberFormatException ex) {
            logger.debug("invalid summary cache size: {}", System.getProperty(PROP_SUMMARY_CACHE));
        }
        this.cache = new SummaryCache(size);
        this.builder = new SummaryBuilder();
    }

    public MethodSummary getSummary(MethodReference.Handle handle) {
        if (handle == null) {
            return null;
        }
        MethodSummary cached = cache.get(handle);
        if (cached != null) {
            return cached;
        }
        MethodSummary summary = builder.build(handle);
        if (summary != null) {
            cache.put(handle, summary);
        }
        return summary;
    }
}
