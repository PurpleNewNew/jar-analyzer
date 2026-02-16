/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.determinism;

import me.n1ar4.jar.analyzer.config.ConfigFile;
import me.n1ar4.jar.analyzer.core.CoreRunner;
import me.n1ar4.jar.analyzer.engine.CoreEngine;
import me.n1ar4.jar.analyzer.engine.EngineContext;
import me.n1ar4.jar.analyzer.engine.WorkspaceContext;
import me.n1ar4.jar.analyzer.exporter.JsonExporter;
import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.support.FixtureJars;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class ExporterDeterminismTest {
    private static final String DB_PATH = Const.dbFile;

    @Test
    @SuppressWarnings("all")
    public void testJsonExporterDeterministic() {
        Path out1 = null;
        Path out2 = null;
        try {
            Path file = FixtureJars.springbootTestJar();
            WorkspaceContext.setResolveInnerJars(false);
            CoreRunner.run(file, null, false, true, true, null, true);

            ConfigFile config = new ConfigFile();
            config.setDbPath(DB_PATH);
            CoreEngine engine = new CoreEngine(config);
            EngineContext.setEngine(engine);

            JsonExporter exporter1 = new JsonExporter();
            assertTrue(exporter1.doExport());
            out1 = Paths.get(exporter1.getFileName());
            String text1 = new String(Files.readAllBytes(out1), StandardCharsets.UTF_8);

            // Ensure different filenames even on very fast CI clocks.
            TimeUnit.MILLISECONDS.sleep(5);

            JsonExporter exporter2 = new JsonExporter();
            assertTrue(exporter2.doExport());
            out2 = Paths.get(exporter2.getFileName());
            String text2 = new String(Files.readAllBytes(out2), StandardCharsets.UTF_8);

            assertEquals(text1, text2);
        } catch (Throwable t) {
            t.printStackTrace();
            fail("exporter determinism test failed: " + t);
        } finally {
            try {
                if (out1 != null) {
                    Files.deleteIfExists(out1);
                }
            } catch (Exception ignored) {
            }
            try {
                if (out2 != null) {
                    Files.deleteIfExists(out2);
                }
            } catch (Exception ignored) {
            }
        }
    }
}
