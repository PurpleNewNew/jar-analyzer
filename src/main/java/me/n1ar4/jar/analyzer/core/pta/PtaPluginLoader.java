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

import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

final class PtaPluginLoader {
    private static final Logger logger = LogManager.getLogger();

    private PtaPluginLoader() {
    }

    static PtaCompositePlugin load(PtaSolverConfig config) {
        if (config == null) {
            return PtaCompositePlugin.empty();
        }
        ArrayList<PtaPlugin> plugins = new ArrayList<>();
        if (config.isConstraintCheckerEnabled()) {
            plugins.add(new PtaConstraintCheckerPlugin());
        }
        List<String> classNames = config.getPluginClassNames();
        if (classNames != null) {
            for (String className : classNames) {
                PtaPlugin plugin = instantiate(className);
                if (plugin != null) {
                    plugins.add(plugin);
                }
            }
        }
        return PtaCompositePlugin.of(plugins);
    }

    private static PtaPlugin instantiate(String className) {
        if (className == null || className.trim().isEmpty()) {
            return null;
        }
        try {
            Class<?> clazz = Class.forName(className.trim());
            Constructor<?> ctor = clazz.getDeclaredConstructor();
            Object instance = ctor.newInstance();
            if (!(instance instanceof PtaPlugin)) {
                logger.warn("pta plugin ignore non-PtaPlugin class: {}", className);
                return null;
            }
            return (PtaPlugin) instance;
        } catch (Exception ex) {
            logger.warn("pta plugin load failed {}: {}", className, ex.toString());
            return null;
        }
    }
}
