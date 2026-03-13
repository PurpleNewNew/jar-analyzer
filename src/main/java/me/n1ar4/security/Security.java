/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.security;

import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.io.ObjectInputFilter;

public class Security {
    private static final Logger logger = LogManager.getLogger();
    private static final int MAX_ARRAY_LENGTH = 100000;
    private static final int MAX_DEPTH = 20;
    private static final int MAX_REFS = 100000;
    private static final int MAX_BYTES = 500000000;

    private Security() {
    }

    public static void setObjectInputFilter() {
        try {
            ObjectInputFilter filter = new JarAnalyzerInputFilter(MAX_ARRAY_LENGTH, MAX_DEPTH, MAX_REFS, MAX_BYTES);
            ObjectInputFilter.Config.setSerialFilter(filter);
            logger.info("object input filter installed");
        } catch (Throwable ex) {
            logger.error("install object input filter failed: {}", ex.toString(), ex);
            throw new IllegalStateException("object_input_filter_install_failed", ex);
        }
    }
}
