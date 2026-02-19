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

import me.n1ar4.jar.analyzer.core.CoreRunner;
import me.n1ar4.jar.analyzer.engine.WorkspaceContext;
import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.support.FixtureJars;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Optional benchmark harness for incremental Lucene global-search indexing.
 * <p>
 * Run with:
 * mvn -Pbench-search test -Dtest=GlobalSearchIncrementalBenchTest
 */
public class GlobalSearchIncrementalBenchTest {
    private static final String ENABLE_PROP = "bench.search";
    private static final String LOOP_PROP = "bench.search.loop";
    private static final String MAX_WARM_P95_PROP = "bench.search.maxWarmP95Ms";
    private static final String MAX_INCREMENTAL_PROP = "bench.search.maxIncrementalMs";

    @Test
    @SuppressWarnings({"unchecked", "all"})
    public void benchIncrementalRebuildLatency() throws Exception {
        Assumptions.assumeTrue(Boolean.getBoolean(ENABLE_PROP),
                "set -D" + ENABLE_PROP + "=true to enable global-search benchmark");

        int loops = resolveInt(LOOP_PROP, 25, 5, 200);
        long maxWarmP95 = resolveLong(MAX_WARM_P95_PROP, 80L, 5L, 3_000L);
        long maxIncremental = resolveLong(MAX_INCREMENTAL_PROP, 600L, 20L, 10_000L);

        Path jar = FixtureJars.springbootTestJar();
        WorkspaceContext.updateResolveInnerJars(false);
        CoreRunner.run(jar, null, false, false, true, null, true);

        Class<?> indexClass = Class.forName(
                "me.n1ar4.jar.analyzer.gui.swing.toolwindow.GlobalSearchDialog$GlobalSearchIndex");
        Class<Enum> categoryClass = (Class<Enum>) Class.forName(
                "me.n1ar4.jar.analyzer.gui.swing.toolwindow.GlobalSearchDialog$CategoryItem");
        Class<?> runClass = Class.forName(
                "me.n1ar4.jar.analyzer.gui.swing.toolwindow.GlobalSearchDialog$SearchRun");

        Field instanceField = indexClass.getDeclaredField("INSTANCE");
        instanceField.setAccessible(true);
        Object index = instanceField.get(null);

        Method searchMethod = indexClass.getDeclaredMethod(
                "search", String.class, categoryClass, int.class, boolean.class);
        searchMethod.setAccessible(true);

        Method buildInfoMethod = runClass.getDeclaredMethod("buildInfo");
        buildInfoMethod.setAccessible(true);
        Method hitsMethod = runClass.getDeclaredMethod("hits");
        hitsMethod.setAccessible(true);

        Enum allCategory = Enum.valueOf(categoryClass, "ALL");
        Enum stringCategory = Enum.valueOf(categoryClass, "STRING");
        String baselineKeyword = "CallbackEntry";

        long t0 = System.nanoTime();
        Object fullRun = searchMethod.invoke(index, baselineKeyword, allCategory, 256, true);
        long fullRebuildMs = (System.nanoTime() - t0) / 1_000_000L;
        String fullInfo = String.valueOf(buildInfoMethod.invoke(fullRun));

        List<Long> warmLatencies = new ArrayList<>(loops);
        for (int i = 0; i < loops; i++) {
            long start = System.nanoTime();
            searchMethod.invoke(index, baselineKeyword, allCategory, 256, false);
            warmLatencies.add((System.nanoTime() - start) / 1_000_000L);
        }
        long warmP95 = percentile(warmLatencies, 0.95);

        String marker = "benchincmarker" + Long.toUnsignedString(System.nanoTime(), 36);
        insertSyntheticStringMarker(marker);
        bumpDbMtime();
        long incStart = System.nanoTime();
        Object incrementalRun = searchMethod.invoke(index, marker, stringCategory, 64, false);
        long incrementalMs = (System.nanoTime() - incStart) / 1_000_000L;
        String incrementalInfo = String.valueOf(buildInfoMethod.invoke(incrementalRun));
        List<?> hits = (List<?>) hitsMethod.invoke(incrementalRun);
        boolean markerFound = hits != null && !hits.isEmpty();
        boolean warmPass = warmP95 <= maxWarmP95;
        boolean incrementalPass = incrementalMs <= maxIncremental;
        boolean comparePass = incrementalMs <= Math.max(maxIncremental, fullRebuildMs * 2);

        List<String> reportLines = new ArrayList<>();
        reportLines.add("## Summary");
        reportLines.add("- fullRebuildMs: `" + fullRebuildMs + "`");
        reportLines.add("- warmSearchP95Ms: `" + warmP95 + "`");
        reportLines.add("- incrementalMs: `" + incrementalMs + "`");
        reportLines.add("- warmLoops: `" + loops + "`");
        reportLines.add("- markerHitCount: `" + (hits == null ? 0 : hits.size()) + "`");
        reportLines.add("- fullBuildInfo: `" + safe(fullInfo) + "`");
        reportLines.add("- incrementalBuildInfo: `" + safe(incrementalInfo) + "`");
        reportLines.add("");
        reportLines.add("## Thresholds");
        reportLines.add("- maxWarmP95Ms: `" + maxWarmP95 + "`");
        reportLines.add("- maxIncrementalMs: `" + maxIncremental + "`");
        reportLines.add("- markerFound: `" + markerFound + "`");
        reportLines.add("- warmPass: `" + warmPass + "`");
        reportLines.add("- incrementalPass: `" + incrementalPass + "`");
        reportLines.add("- comparePass(<=max(maxIncremental,2xFull)): `" + comparePass + "`");
        BenchReportWriter.writeMarkdown("global-search-incremental.md",
                "Global Search Incremental Benchmark",
                reportLines);

        System.out.println("[search-bench] fullRebuildMs=" + fullRebuildMs + " info=" + fullInfo);
        System.out.println("[search-bench] warmP95Ms=" + warmP95 + " loops=" + loops);
        System.out.println("[search-bench] incrementalMs=" + incrementalMs
                + " info=" + incrementalInfo
                + " hitCount=" + (hits == null ? 0 : hits.size()));

        assertTrue(markerFound, "incremental search should include inserted marker");
        assertTrue(incrementalInfo.toLowerCase(Locale.ROOT).contains("updated"),
                "incremental build info should expose updated sources");
        assertTrue(warmPass,
                "warm search p95 is too high: " + warmP95 + "ms");
        assertTrue(incrementalPass,
                "incremental rebuild is too high: " + incrementalMs + "ms");
        assertTrue(comparePass,
                "incremental rebuild should not exceed 2x full rebuild");
    }

    private static void insertSyntheticStringMarker(String marker) throws Exception {
        String pickSql = "SELECT m.class_name, m.method_name, m.method_desc, m.jar_id, c.jar_name " +
                "FROM method_table m " +
                "LEFT JOIN class_table c ON c.class_name = m.class_name AND c.jar_id = m.jar_id " +
                "ORDER BY m.jar_id ASC, m.class_name ASC, m.method_name ASC, m.method_desc ASC LIMIT 1";
        String insertSql = "INSERT INTO string_table(value, access, method_desc, method_name, class_name, jar_name, jar_id) " +
                "VALUES(?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + Const.dbFile)) {
            String className = "";
            String methodName = "";
            String methodDesc = "";
            String jarName = "";
            int jarId = 0;
            try (PreparedStatement pick = conn.prepareStatement(pickSql);
                 ResultSet rs = pick.executeQuery()) {
                if (rs.next()) {
                    className = safe(rs.getString("class_name"));
                    methodName = safe(rs.getString("method_name"));
                    methodDesc = safe(rs.getString("method_desc"));
                    jarName = safe(rs.getString("jar_name"));
                    jarId = rs.getInt("jar_id");
                }
            }
            try (PreparedStatement insert = conn.prepareStatement(insertSql)) {
                insert.setString(1, marker);
                insert.setInt(2, 0);
                insert.setString(3, methodDesc);
                insert.setString(4, methodName);
                insert.setString(5, className);
                insert.setString(6, jarName);
                insert.setInt(7, jarId);
                insert.executeUpdate();
            }
        }
    }

    private static void bumpDbMtime() {
        try {
            Path dbPath = Path.of(Const.dbFile);
            if (!Files.exists(dbPath)) {
                return;
            }
            long current = Files.getLastModifiedTime(dbPath).toMillis();
            long target = Math.max(System.currentTimeMillis() + 5_000L, current + 5_000L);
            Files.setLastModifiedTime(dbPath, FileTime.fromMillis(target));
        } catch (Exception ignored) {
        }
    }

    private static int resolveInt(String prop, int def, int min, int max) {
        String raw = System.getProperty(prop);
        if (raw == null || raw.trim().isEmpty()) {
            return def;
        }
        try {
            int value = Integer.parseInt(raw.trim());
            return Math.max(min, Math.min(max, value));
        } catch (Exception ignored) {
            return def;
        }
    }

    private static long resolveLong(String prop, long def, long min, long max) {
        String raw = System.getProperty(prop);
        if (raw == null || raw.trim().isEmpty()) {
            return def;
        }
        try {
            long value = Long.parseLong(raw.trim());
            return Math.max(min, Math.min(max, value));
        } catch (Exception ignored) {
            return def;
        }
    }

    private static long percentile(List<Long> values, double p) {
        if (values == null || values.isEmpty()) {
            return 0L;
        }
        List<Long> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        int index = (int) Math.ceil(sorted.size() * p) - 1;
        if (index < 0) {
            index = 0;
        }
        if (index >= sorted.size()) {
            index = sorted.size() - 1;
        }
        return sorted.get(index);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
