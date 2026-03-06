/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.sca;

import me.n1ar4.jar.analyzer.sca.dto.SCAApiResult;

import java.util.List;

public final class ScaReportFormatter {
    private ScaReportFormatter() {
    }

    public static String buildConsoleReport(List<SCAApiResult> results) {
        if (results == null || results.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (SCAApiResult item : results) {
            if (item == null) {
                continue;
            }
            String hash = safe(item.getHash());
            if (hash.length() > 16) {
                hash = hash.substring(0, 16);
            }
            sb.append("CVE-ID: ").append(safe(item.getCve())).append('\n');
            sb.append("DESC  : ").append(safe(item.getDesc())).append('\n');
            sb.append("CVSS  : ").append(item.getCvss()).append('\n');
            sb.append("JAR   : ").append(safe(item.getJarPath())).append('\n');
            sb.append("CLASS : ").append(safe(item.getKeyClass())).append('\n');
            sb.append("HASH(16): ").append(hash).append("\n\n");
        }
        return sb.toString();
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
