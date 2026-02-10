/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.utils;

public class StringUtil {
    public static boolean isBlank(String str) {
        return str == null || str.isBlank();
    }

    public static boolean isNull(String str) {
        return isBlank(str);
    }
}
