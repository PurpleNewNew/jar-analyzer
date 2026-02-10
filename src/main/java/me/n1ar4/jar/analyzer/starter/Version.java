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

import me.n1ar4.jar.analyzer.core.notify.NotifierContext;
import me.n1ar4.jar.analyzer.utils.InterruptUtil;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

public class Version {
    private static final Logger logger = LogManager.getLogger();

    private static int javaMajorVersion() {
        try {
            return Runtime.version().feature();
        } catch (Throwable ignored) {
            return -1;
        }
    }

    public static void check() {
        String version = System.getProperty("java.version");
        int major = javaMajorVersion();

        // Jar Analyzer now targets JDK 21 for build/runtime. Analysis of lower-version jars is still supported via ASM.
        if (major > 0 && major < 21) {
            String msg = "Jar Analyzer requires Java 21+; current version is " + version;
            logger.warn(msg);
            try {
                NotifierContext.get().warn("Jar Analyzer", msg);
            } catch (Throwable t) {
                InterruptUtil.restoreInterruptIfNeeded(t);
                if (t instanceof Error) {
                    throw (Error) t;
                }
                logger.debug("notifier warn failed: {}", t.toString());
            }
            return;
        }

        logger.info("java runtime version: {}", version);
    }
}
