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

import java.io.ObjectInputFilter;

public class Security {
    private static final int maxArrayLength = 100000;
    private static final int maxDepth = 20;
    private static final int maxRefs = 100000;
    private static final int maxBytes = 500000000;

    public static void setObjectInputFilter() {
        try {
            ObjectInputFilter filter = new JarAnalyzerInputFilter(maxArrayLength, maxDepth, maxRefs, maxBytes);
            ObjectInputFilter.Config.setSerialFilter(filter);
            System.out.println("[*] LOAD OBJECT INPUT FILTER SUCCESS");
        } catch (Throwable t) {
            System.out.println("[-] LOAD OBJECT INPUT FILTER FAIL: " + t);
        }
    }
}
