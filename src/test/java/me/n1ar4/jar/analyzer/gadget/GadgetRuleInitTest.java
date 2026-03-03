/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.gadget;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

class GadgetRuleInitTest {
    @Test
    void shouldInitializeRulesOnlyOnceUnderConcurrentAccess() throws Exception {
        int workers = 16;
        ExecutorService pool = Executors.newFixedThreadPool(workers);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<List<GadgetInfo>>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < workers; i++) {
                futures.add(pool.submit(() -> {
                    start.await();
                    return GadgetRule.getRules();
                }));
            }
            start.countDown();
            List<GadgetInfo> baseline = futures.get(0).get();
            assertFalse(baseline.isEmpty(), "gadget rules should be loaded");
            for (int i = 1; i < futures.size(); i++) {
                assertSame(baseline, futures.get(i).get(), "all threads should observe the same rule snapshot");
            }
            long uniqueIds = baseline.stream().map(GadgetInfo::getID).distinct().count();
            assertEquals(baseline.size(), uniqueIds, "rules should not contain duplicate IDs");
        } finally {
            pool.shutdownNow();
        }
    }
}
