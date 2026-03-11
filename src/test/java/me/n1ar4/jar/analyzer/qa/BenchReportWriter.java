/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.qa;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

final class BenchReportWriter {
    private BenchReportWriter() {
    }

    static void writeMarkdown(String fileName, String title, List<String> lines) {
        Path output = outputPath(fileName);
        StringBuilder md = new StringBuilder();
        md.append("# ").append(safe(title)).append('\n');
        md.append('\n');
        md.append("- Generated: ")
                .append(OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                .append('\n');
        md.append("- Java: ").append(safe(System.getProperty("java.version"))).append('\n');
        md.append("- OS: ").append(safe(System.getProperty("os.name")))
                .append(' ')
                .append(safe(System.getProperty("os.arch")))
                .append('\n');
        md.append('\n');
        if (lines != null) {
            for (String line : lines) {
                md.append(safe(line)).append('\n');
            }
        }
        write(output, md.toString());
    }

    static void writeCsv(String fileName, List<String> rows) {
        Path output = outputPath(fileName);
        StringBuilder csv = new StringBuilder();
        if (rows != null) {
            for (String row : rows) {
                csv.append(safe(row)).append('\n');
            }
        }
        write(output, csv.toString());
    }

    static void writeText(String fileName, List<String> lines) {
        Path output = outputPath(fileName);
        StringBuilder text = new StringBuilder();
        if (lines != null) {
            for (String line : lines) {
                text.append(safe(line)).append('\n');
            }
        }
        write(output, text.toString());
    }

    private static Path outputPath(String fileName) {
        return Path.of("target", "bench").resolve(safe(fileName));
    }

    private static void write(Path output, String content) {
        try {
            Files.createDirectories(output.getParent());
            Files.writeString(
                    output,
                    safe(content),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );
            System.out.println("[bench-report] wrote " + output.toAbsolutePath());
        } catch (Exception ex) {
            System.out.println("[bench-report] write failed: " + ex.getMessage());
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
