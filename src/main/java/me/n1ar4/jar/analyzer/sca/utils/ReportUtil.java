/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.sca.utils;

import me.n1ar4.jar.analyzer.sca.dto.SCAApiResult;
import me.n1ar4.jar.analyzer.utils.IOUtil;
import me.n1ar4.jar.analyzer.utils.InterruptUtil;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class ReportUtil {
    private static final Logger logger = LogManager.getLogger();
    private static byte[] BT_CSS = null;
    private static byte[] BT_JS = null;
    private static byte[] JQ_JS = null;
    private static byte[] POPPER_JS = null;

    static {
        BT_CSS = readResourceBytes("report/BT_CSS.css");
        BT_JS = readResourceBytes("report/BT_JS.js");
        JQ_JS = readResourceBytes("report/JQ_JS.js");
        POPPER_JS = readResourceBytes("report/POPPER_JS.js");
    }

    private static byte[] readResourceBytes(String name) {
        if (name == null || name.trim().isEmpty()) {
            return new byte[0];
        }
        try (InputStream in = ClassLoader.getSystemResourceAsStream(name)) {
            if (in == null) {
                logger.debug("report resource not found: {}", name);
                return new byte[0];
            }
            byte[] data = IOUtil.readAllBytes(in);
            return data == null ? new byte[0] : data;
        } catch (Throwable t) {
            InterruptUtil.restoreInterruptIfNeeded(t);
            if (t instanceof Error) {
                throw (Error) t;
            }
            logger.debug("read report resource failed: {}: {}", name, t.toString());
            return new byte[0];
        }
    }

    public static void generateHtmlReport(List<SCAApiResult> vulnerabilities, String filePath) throws IOException {
        List<SCAApiResult> entries = vulnerabilities == null ? List.of() : vulnerabilities;
        StringBuilder htmlContent = new StringBuilder();
        htmlContent.append("<!DOCTYPE html><html lang=\"zh-CN\"><head>")
                .append("<meta charset=\"UTF-8\"><meta name=\"viewport\" " +
                        "content=\"width=device-width, initial-scale=1, shrink-to-fit=no\">")
                .append("<title>Jar Analyzer 漏洞报告</title>")
                .append("<style>").append(new String(BT_CSS, StandardCharsets.UTF_8)).append("</style>")
                .append("<script>").append(new String(JQ_JS, StandardCharsets.UTF_8)).append("</script>")
                .append("<script>").append(new String(BT_JS, StandardCharsets.UTF_8)).append("</script>")
                .append("<script>").append(new String(POPPER_JS, StandardCharsets.UTF_8)).append("</script>")
                .append("<style>")
                .append(".card { margin-bottom: 1rem; }")
                .append(".card-header { font-weight: bold; font-size: 1.25rem; }")
                .append(".card-title { font-size: 1rem; margin-top: 0.5rem; }")
                .append("</style>")
                .append("</head><body><div class=\"container\">")
                .append("<h1 class=\"mt-5 mb-4\">Jar Analyzer 漏洞报告</h1>")
                .append("<div class=\"accordion\" id=\"accordionExample\">");
        if (entries.isEmpty()) {
            htmlContent.append("<div class=\"alert alert-secondary\">暂无漏洞数据</div>");
        } else {
            int cardIndex = 0;
            for (SCAApiResult entry : entries) {
                if (entry == null) {
                    continue;
                }
                String cve = safe(entry.getCve());
                if (cve.isEmpty()) {
                    cve = "UNKNOWN";
                }
                htmlContent.append("<div class=\"card\">")
                        .append("<div class=\"card-header\" id=\"heading").append(cardIndex).append("\">")
                        .append("<h2 class=\"mb-0\">")
                        .append("<button class=\"btn btn-link\" type=\"button\" " +
                                "data-toggle=\"collapse\" data-target=\"#collapse").append(cardIndex).append(
                                "\" aria-expanded=\"true\" aria-controls=\"collapse").append(cardIndex).append("\">")
                        .append(cve)
                        .append("</button>")
                        .append("</h2>")
                        .append("</div>")
                        .append("<div id=\"collapse").append(cardIndex).append(
                                "\" class=\"collapse\" aria-labelledby=\"heading").append(cardIndex).append(
                                "\" data-parent=\"#accordionExample\">")
                        .append("<div class=\"card-body\">");
                appendCardLine(htmlContent, "描述", safe(entry.getDesc()));
                appendCvssLine(htmlContent, entry.getCvss());
                appendCardLine(htmlContent, "JAR", safe(entry.getJarPath()));
                appendCardLine(htmlContent, "CLASS", safe(entry.getKeyClass()));
                appendCardLine(htmlContent, "HASH", shortHash(entry.getHash()));
                htmlContent.append("</div></div></div>");
                cardIndex++;
            }
            if (cardIndex == 0) {
                htmlContent.append("<div class=\"alert alert-secondary\">暂无漏洞数据</div>");
            }
        }
        htmlContent.append("</div></div></body></html>");
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(filePath), StandardCharsets.UTF_8)) {
            writer.write(htmlContent.toString());
        }
    }

    private static void appendCardLine(StringBuilder htmlContent, String title, String content) {
        htmlContent.append("<h5 class=\"card-title\">").append(title).append("</h5>")
                .append("<p class=\"card-text\">").append(content).append("</p>");
    }

    private static void appendCvssLine(StringBuilder htmlContent, float value) {
        htmlContent.append("<h5 class=\"card-title\">CVSS</h5>")
                .append("<p class=\"card-text\">").append(value).append("&nbsp;&nbsp;&nbsp;");
        if (value > 8.9f) {
            htmlContent.append("<button type=\"button\" class=\"btn btn-dark\">CRITICAL</button>");
        } else if (value > 6.9f) {
            htmlContent.append("<button type=\"button\" class=\"btn btn-danger\">HIGH</button>");
        } else if (value > 3.9f) {
            htmlContent.append("<button type=\"button\" class=\"btn btn-warning\">MODERATE</button>");
        } else if (value > 0f) {
            htmlContent.append("<button type=\"button\" class=\"btn btn-secondary\">LOW</button>");
        } else {
            htmlContent.append("<button type=\"button\" class=\"btn btn-secondary\">UNKNOWN</button>");
        }
        htmlContent.append("</p>");
    }

    private static String shortHash(String hash) {
        String safeHash = safe(hash);
        if (safeHash.length() > 16) {
            return safeHash.substring(0, 16);
        }
        return safeHash;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
