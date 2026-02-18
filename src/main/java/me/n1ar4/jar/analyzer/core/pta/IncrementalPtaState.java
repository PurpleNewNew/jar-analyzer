/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.core.pta;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

final class IncrementalPtaState {
    private static final int MAX_GLOBAL_ENTRIES = 500_000;
    private static final Map<String, Integer> fingerprints = new LinkedHashMap<>();
    private final String scopePrefix;

    IncrementalPtaState(String scopeKey) {
        String key = scopeKey == null ? "" : scopeKey.trim();
        if (key.isEmpty()) {
            key = "default";
        }
        this.scopePrefix = key + "|";
    }

    synchronized boolean shouldProcess(String unitId, int fingerprint) {
        if (unitId == null || unitId.isEmpty()) {
            return true;
        }
        String scopedId = scopePrefix + unitId;
        Integer old = fingerprints.get(scopedId);
        if (old == null || old != fingerprint) {
            fingerprints.put(scopedId, fingerprint);
            trimIfNecessary();
            return true;
        }
        return false;
    }

    synchronized void cleanup(Set<String> seenUnitIds) {
        for (Iterator<String> it = fingerprints.keySet().iterator(); it.hasNext(); ) {
            String key = it.next();
            if (key == null || !key.startsWith(scopePrefix)) {
                continue;
            }
            if (seenUnitIds == null || !seenUnitIds.contains(key.substring(scopePrefix.length()))) {
                it.remove();
            }
        }
    }

    static synchronized void resetAll() {
        fingerprints.clear();
    }

    private static void trimIfNecessary() {
        if (fingerprints.size() <= MAX_GLOBAL_ENTRIES) {
            return;
        }
        int remove = Math.max(1, fingerprints.size() - MAX_GLOBAL_ENTRIES);
        Iterator<String> it = fingerprints.keySet().iterator();
        while (remove > 0 && it.hasNext()) {
            it.next();
            it.remove();
            remove--;
        }
    }
}
