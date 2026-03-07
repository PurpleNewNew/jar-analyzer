/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.starter;

import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

public final class Version {
    private static final Logger logger = LogManager.getLogger();

    private Version() {
    }

    private static int javaMajorVersion() {
        try {
            return Runtime.version().feature();
        } catch (Throwable ignored) {
            return -1;
        }
    }

    public static void logRuntimeVersion() {
        String version = System.getProperty("java.version");
        int major = javaMajorVersion();
        if (major > 0) {
            logger.info("java runtime version: {} (feature={})", version, major);
            return;
        }
        logger.info("java runtime version: {}", version);
    }
}
