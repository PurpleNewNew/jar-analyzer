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

import me.n1ar4.jar.analyzer.config.ConfigFile;
import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.storage.neo4j.ActiveProjectContext;
import me.n1ar4.jar.analyzer.storage.neo4j.Neo4jProjectStore;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Global CoreEngine holder decoupled from GUI classes.
 * <p>
 * Historically many modules referenced a Swing UI singleton to reach the engine, which tightly coupled
 * server/engine code to the desktop UI. This holder provides the same capability without that dependency.
 */
public final class EngineContext {
    private static final Logger logger = LogManager.getLogger();
    private static final AtomicReference<CoreEngine> ENGINE = new AtomicReference<>();
    private static final ConcurrentHashMap<String, CoreEngine> PROJECT_ENGINES = new ConcurrentHashMap<>();
    private static final ThreadLocal<CoreEngine> ENGINE_OVERRIDE = new ThreadLocal<>();

    private EngineContext() {
    }

    public static CoreEngine getEngine() {
        CoreEngine override = ENGINE_OVERRIDE.get();
        if (override != null) {
            return override;
        }
        String currentProjectKey = ActiveProjectContext.getActiveProjectKey();
        String publishedProjectKey = ActiveProjectContext.getPublishedActiveProjectKey();
        if (!currentProjectKey.isBlank() && !currentProjectKey.equals(publishedProjectKey)) {
            return getOrCreateProjectEngine(currentProjectKey);
        }
        return ENGINE.get();
    }

    public static void setEngine(CoreEngine engine) {
        ENGINE.set(engine);
        ENGINE_OVERRIDE.remove();
        PROJECT_ENGINES.clear();
    }

    public static <T> T withEngine(CoreEngine engine, java.util.function.Supplier<T> supplier) {
        if (supplier == null) {
            return null;
        }
        CoreEngine previous = ENGINE_OVERRIDE.get();
        if (engine == null) {
            ENGINE_OVERRIDE.remove();
        } else {
            ENGINE_OVERRIDE.set(engine);
        }
        try {
            return supplier.get();
        } finally {
            if (previous == null) {
                ENGINE_OVERRIDE.remove();
            } else {
                ENGINE_OVERRIDE.set(previous);
            }
        }
    }

    public static void withEngine(CoreEngine engine, Runnable runnable) {
        withEngine(engine, () -> {
            if (runnable != null) {
                runnable.run();
            }
            return null;
        });
    }

    private static CoreEngine getOrCreateProjectEngine(String projectKey) {
        CoreEngine fallback = ENGINE.get();
        if (projectKey == null || projectKey.isBlank()) {
            return fallback;
        }
        long buildSeq = Math.max(0L, DatabaseManager.getProjectBuildSeq(projectKey));
        String cacheKey = projectKey + "|" + buildSeq;
        CoreEngine cached = PROJECT_ENGINES.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        synchronized (EngineContext.class) {
            cached = PROJECT_ENGINES.get(cacheKey);
            if (cached != null) {
                return cached;
            }
            pruneProjectEngines(projectKey, buildSeq);
            try {
                CoreEngine created = createProjectEngine(projectKey);
                PROJECT_ENGINES.put(cacheKey, created);
                return created;
            } catch (Exception ex) {
                logger.debug("create project-scoped engine failed: key={} err={}", projectKey, ex.toString());
                return fallback;
            }
        }
    }

    private static void pruneProjectEngines(String projectKey, long buildSeq) {
        String prefix = projectKey + "|";
        Iterator<Map.Entry<String, CoreEngine>> iterator = PROJECT_ENGINES.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, CoreEngine> entry = iterator.next();
            String key = entry.getKey();
            if (key == null || !key.startsWith(prefix) || key.equals(prefix + buildSeq)) {
                continue;
            }
            iterator.remove();
        }
    }

    private static CoreEngine createProjectEngine(String projectKey) {
        ConfigFile cfg = new ConfigFile();
        cfg.setDbPath(Neo4jProjectStore.getInstance().resolveProjectHome(projectKey).toString());
        return new CoreEngine(cfg);
    }
}
