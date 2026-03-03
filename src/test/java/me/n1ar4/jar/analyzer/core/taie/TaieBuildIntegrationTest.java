package me.n1ar4.jar.analyzer.core.taie;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class TaieBuildIntegrationTest {

    @Test
    void shouldUseBalancedProfileByDefault() {
        String backup = System.getProperty("jar.analyzer.analysis.profile");
        try {
            System.clearProperty("jar.analyzer.analysis.profile");
            assertEquals("balanced", TaieAnalysisRunner.resolveProfile().value());
        } finally {
            restoreProp("jar.analyzer.analysis.profile", backup);
        }
    }

    @Test
    void shouldDisableWhenNoAppArchives() {
        TaieAnalysisRunner.TaieRunResult result = TaieAnalysisRunner.run(
                List.<Path>of(),
                List.<Path>of(),
                TaieAnalysisRunner.AnalysisProfile.BALANCED
        );
        assertFalse(result.enabled());
        assertFalse(result.success());
        assertEquals("no-app-archives", result.reason());
    }

    @Test
    void shouldUseAutoInvokeDynamicModeByDefault() {
        String backup = System.getProperty("jar.analyzer.taie.invokedynamic");
        try {
            System.clearProperty("jar.analyzer.taie.invokedynamic");
            assertEquals("auto", TaieAnalysisRunner.resolveInvokeDynamicMode().value());
        } finally {
            restoreProp("jar.analyzer.taie.invokedynamic", backup);
        }
    }

    @Test
    void shouldParseInvokeDynamicModeFromProperty() {
        String backup = System.getProperty("jar.analyzer.taie.invokedynamic");
        try {
            System.setProperty("jar.analyzer.taie.invokedynamic", "off");
            assertEquals("off", TaieAnalysisRunner.resolveInvokeDynamicMode().value());
            System.setProperty("jar.analyzer.taie.invokedynamic", "on");
            assertEquals("on", TaieAnalysisRunner.resolveInvokeDynamicMode().value());
        } finally {
            restoreProp("jar.analyzer.taie.invokedynamic", backup);
        }
    }

    private static void restoreProp(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }
}
