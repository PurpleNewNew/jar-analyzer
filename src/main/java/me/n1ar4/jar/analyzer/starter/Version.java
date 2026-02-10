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

    public static boolean isJava8() {
        String version = System.getProperty("java.version");
        return version.startsWith("1.8");
    }

    public static void check() {
        String version = System.getProperty("java.version");
        if (version.startsWith("1.8")) {
            String[] versionComponents = version.split("_");
            if (versionComponents.length > 1) {
                try {
                    int updateVersion = Integer.parseInt(versionComponents[1]);
                    if (updateVersion <= 191) {
                        logger.warn("risk - java version is lower than 191");
                        String msg = "vulnerability in versions lower than Java 8u191; please use a higher version";
                        try {
                            NotifierContext.get().warn("Jar Analyzer", msg);
                        } catch (Throwable t) {
                            InterruptUtil.restoreInterruptIfNeeded(t);
                            if (t instanceof Error) {
                                throw (Error) t;
                            }
                            logger.debug("notifier warn failed: {}", t.toString());
                        }
                    } else {
                        logger.debug("safe - java version is higher than 191");
                    }
                } catch (NumberFormatException e) {
                    logger.warn("error java update version {}", versionComponents[1]);
                }
            } else {
                logger.warn("error java version {}", version);
            }
        } else {
            logger.warn("please use java 8 version");
            String msg = "java 8 is recommended; your version is " + version;
            try {
                NotifierContext.get().warn("Jar Analyzer", msg);
            } catch (Throwable t) {
                InterruptUtil.restoreInterruptIfNeeded(t);
                if (t instanceof Error) {
                    throw (Error) t;
                }
                logger.debug("notifier warn failed: {}", t.toString());
            }
        }
    }
}
