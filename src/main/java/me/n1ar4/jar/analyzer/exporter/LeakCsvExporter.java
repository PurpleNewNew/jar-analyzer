/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.exporter;

import me.n1ar4.jar.analyzer.entity.LeakResult;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class LeakCsvExporter implements Exporter {
    private static final Logger logger = LogManager.getLogger();
    private final List<LeakResult> leakResults;
    private String fileName;

    public LeakCsvExporter(List<LeakResult> leakResults) {
        this.leakResults = leakResults;
    }

    @Override
    public boolean doExport() {
        if (leakResults == null || leakResults.isEmpty()) {
            logger.warn("没有泄露检测结果可以导出");
            return false;
        }

        List<LeakResult> ordered = new ArrayList<>(leakResults);
        ordered.sort(Comparator.comparingInt((LeakResult r) -> r == null || r.getJarId() == null ? Integer.MAX_VALUE : r.getJarId())
                .thenComparing(r -> r == null || r.getTypeName() == null ? "" : r.getTypeName())
                .thenComparing(r -> r == null || r.getClassName() == null ? "" : r.getClassName())
                .thenComparing(r -> r == null || r.getValue() == null ? "" : r.getValue()));

        this.fileName = String.format("leak-results-%d.csv", System.currentTimeMillis());
        try (FileWriter out = new FileWriter(fileName);
             CSVPrinter printer = new CSVPrinter(out,
                     CSVFormat.DEFAULT.builder().setHeader("Type", "ClassName", "Value").get())) {

            for (LeakResult result : ordered) {
                printer.printRecord(
                        result.getTypeName(),
                        result.getClassName(),
                        result.getValue()
                );
            }
            return true;
        } catch (Exception ex) {
            logger.error("导出泄露检测结果到 CSV 失败: " + ex.getMessage());
            return false;
        }
    }

    @Override
    public String getFileName() {
        return this.fileName;
    }
}
