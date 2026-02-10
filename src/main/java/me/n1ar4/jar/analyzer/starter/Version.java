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
        // Prefer specification version: "1.8" (Java 8) / "9" / "17" / "21" ...
        String spec = System.getProperty("java.specification.version");
        if (spec == null || spec.trim().isEmpty()) {
            return -1;
        }
        spec = spec.trim();
        try {
            if (spec.startsWith("1.")) {
                return Integer.parseInt(spec.substring(2));
            }
            int dot = spec.indexOf('.');
            if (dot > 0) {
                return Integer.parseInt(spec.substring(0, dot));
            }
            return Integer.parseInt(spec);
        } catch (NumberFormatException ex) {
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
