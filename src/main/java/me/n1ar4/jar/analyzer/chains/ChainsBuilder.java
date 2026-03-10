/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.chains;

import me.n1ar4.jar.analyzer.rules.SinkRuleRegistry;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ChainsBuilder {
    private static final Logger logger = LogManager.getLogger();
    private static volatile Map<String, SinkModel> sinkData = Map.of();
    private static volatile long loadedSinkVersion = -1L;

    static {
        ensureSinkRulesFresh();
    }

    /**
     * 加载 sink 规则，统一从 sink rule registry 入口读取
     */
    private static void loadSinkRules(long sinkVersion) {
        List<SinkModel> sinkList = SinkRuleRegistry.getSinkModels();
        Map<String, SinkModel> next = new LinkedHashMap<>();
        if (sinkList != null && !sinkList.isEmpty()) {
            for (SinkModel sink : sinkList) {
                if (sink.getBoxName() != null && !sink.getBoxName().trim().isEmpty()) {
                    next.put(sink.getBoxName(), sink);
                }
            }
        } else {
            logger.warn("sink rule list is empty");
        }
        sinkData = Collections.unmodifiableMap(next);
        loadedSinkVersion = sinkVersion;
        logger.info("load {} sink rule version={}", next.size(), sinkVersion);
    }

    private static void ensureSinkRulesFresh() {
        long sinkVersion = SinkRuleRegistry.getVersion();
        if (sinkVersion == loadedSinkVersion) {
            return;
        }
        synchronized (ChainsBuilder.class) {
            long latestVersion = SinkRuleRegistry.getVersion();
            if (latestVersion == loadedSinkVersion) {
                return;
            }
            loadSinkRules(latestVersion);
        }
    }

    public static SinkModel getSinkByName(String name) {
        ensureSinkRulesFresh();
        if (name == null) {
            return null;
        }
        String key = name.trim();
        if (key.isEmpty()) {
            return null;
        }
        Map<String, SinkModel> current = sinkData;
        SinkModel direct = current.get(key);
        if (direct != null) {
            return direct;
        }
        String simpleKey = stripParams(key);
        for (SinkModel sink : current.values()) {
            if (sink == null) {
                continue;
            }
            String box = sink.getBoxName();
            if (box != null && box.equalsIgnoreCase(key)) {
                return sink;
            }
            String simple = buildSimpleName(sink);
            if (simple != null && simple.equalsIgnoreCase(simpleKey)) {
                return sink;
            }
        }
        return null;
    }

    private static String stripParams(String name) {
        int idx = name.indexOf('(');
        if (idx > 0) {
            return name.substring(0, idx);
        }
        return name;
    }

    private static String buildSimpleName(SinkModel sink) {
        if (sink == null) {
            return null;
        }
        String className = sink.getClassName();
        String methodName = sink.getMethodName();
        if (className == null || methodName == null) {
            return null;
        }
        String simple = className;
        int idx = simple.lastIndexOf('/');
        if (idx >= 0 && idx < simple.length() - 1) {
            simple = simple.substring(idx + 1);
        }
        simple = simple.replace('$', '.');
        return simple + "." + methodName;
    }
}
