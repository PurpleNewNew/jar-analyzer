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
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ScaReportFormatterTest {
    @Test
    void buildConsoleReportShouldUseSharedStableShape() {
        SCAApiResult row = new SCAApiResult();
        row.setCve("CVE-2024-0001");
        row.setDesc("demo vuln");
        row.setCvss(9.8f);
        row.setJarPath("/tmp/demo.jar");
        row.setKeyClass("demo/Key");
        row.setHash("1234567890abcdef9999");

        String report = ScaReportFormatter.buildConsoleReport(List.of(row));

        assertTrue(report.contains("CVE-ID: CVE-2024-0001"));
        assertTrue(report.contains("DESC  : demo vuln"));
        assertTrue(report.contains("CVSS  : 9.8"));
        assertTrue(report.contains("JAR   : /tmp/demo.jar"));
        assertTrue(report.contains("CLASS : demo/Key"));
        assertTrue(report.contains("HASH(16): 1234567890abcdef"));
    }
}
