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

import me.n1ar4.jar.analyzer.utils.IOUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ReportUtil {
    private static byte[] BT_CSS = null;
    private static byte[] BT_JS = null;
    private static byte[] JQ_JS = null;
    private static byte[] POPPER_JS = null;

    static {
        try {
            InputStream bcCssIs = ClassLoader.getSystemResourceAsStream("report/BT_CSS.css");
            BT_CSS = IOUtils.readAllBytes(bcCssIs);
            InputStream btJsIs = ClassLoader.getSystemResourceAsStream("report/BT_JS.js");
            BT_JS = IOUtils.readAllBytes(btJsIs);
            InputStream jqJsIs = ClassLoader.getSystemResourceAsStream("report/JQ_JS.js");
            JQ_JS = IOUtils.readAllBytes(jqJsIs);
            InputStream popperJsIs = ClassLoader.getSystemResourceAsStream("report/POPPER_JS.js");
            POPPER_JS = IOUtils.readAllBytes(popperJsIs);
        } catch (Exception ignored) {
        }
    }

    public static void generateHtmlReport(String vulnerabilities, String filePath) throws IOException {
        String safeInput = vulnerabilities == null ? "" : vulnerabilities.trim();
        String[] entries = safeInput.isEmpty() ? new String[0] : safeInput.split("\\r?\\n\\r?\\n");
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
        if (entries.length == 0) {
            htmlContent.append("<div class=\"alert alert-secondary\">暂无漏洞数据</div>");
        } else {
            int cardIndex = 0;
            for (String entry : entries) {
                if (entry == null || entry.trim().isEmpty()) {
                    continue;
                }
                String[] lines = entry.split("\\r?\\n");
                if (lines.length == 0) {
                    continue;
                }
                String cveLine = lines[0].trim();
                String[] cveParts = cveLine.split(":", 2);
                String cve = cveParts.length > 1 ? cveParts[1].trim() : cveLine;
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
                for (int i = 1; i < lines.length; i++) {
                    String line = lines[i].trim();
                    if (line.isEmpty()) {
                        continue;
                    }
                    int colonIndex = line.indexOf(':');
                    String title;
                    String content;
                    if (colonIndex >= 0) {
                        title = line.substring(0, colonIndex).trim();
                        content = line.substring(colonIndex + 1).trim();
                    } else {
                        title = "INFO";
                        content = line;
                    }
                    if (title.equals("DESC")) {
                        title = "描述";
                    }
                    htmlContent.append("<h5 class=\"card-title\">").append(title).append("</h5>");
                    if (title.equals("CVSS")) {
                        htmlContent.append("<p class=\"card-text\">").append(content).append("&nbsp;&nbsp;&nbsp;");
                        Double val = null;
                        if (!content.isEmpty()) {
                            try {
                                val = Double.parseDouble(content);
                            } catch (Exception ignored) {
                            }
                        }
                        if (val == null) {
                            htmlContent.append("<button type=\"button\" class=\"btn btn-secondary\">UNKNOWN</button>");
                            htmlContent.append("</p>");
                        } else if (val > 8.9) {
                            // 严重
                            htmlContent.append("<button type=\"button\" class=\"btn btn-dark\">CRITICAL</button>");
                            htmlContent.append("</p>");
                        } else if (val > 6.9) {
                            // 严重
                            htmlContent.append("<button type=\"button\" class=\"btn btn-danger\">HIGH</button>");
                            htmlContent.append("</p>");
                        } else if (val > 3.9) {
                            // 严重
                            htmlContent.append("<button type=\"button\" class=\"btn btn-warning\">MODERATE</button>");
                            htmlContent.append("</p>");
                        } else {
                            // 严重
                            htmlContent.append("<button type=\"button\" class=\"btn btn-secondary\">LOW</button>");
                            htmlContent.append("</p>");
                        }
                    } else {
                        htmlContent.append("<p class=\"card-text\">").append(content).append("</p>");
                    }
                }
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
}
