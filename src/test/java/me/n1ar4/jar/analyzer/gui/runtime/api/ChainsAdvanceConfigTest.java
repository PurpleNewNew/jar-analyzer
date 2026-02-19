/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.gui.runtime.api;

import me.n1ar4.jar.analyzer.gui.runtime.model.ChainsSettingsDto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChainsAdvanceConfigTest {
    @Test
    void applyAndSnapshotShouldKeepAdvancedFields() {
        ChainsSettingsDto settings = new ChainsSettingsDto(
                true,
                false,
                "java/lang/Runtime",
                "exec",
                "(Ljava/lang/String;)Ljava/lang/Process;",
                "a/b/C",
                "m",
                "()V",
                false,
                true,
                12,
                true,
                true,
                "java.lang.Object;java.util.;",
                "high",
                true,
                true,
                77
        );
        RuntimeFacades.chains().apply(settings);
        ChainsSettingsDto snapshot = RuntimeFacades.chains().snapshot().settings();
        assertEquals("java.lang.Object;java.util.;", snapshot.blacklist());
        assertEquals("high", snapshot.minEdgeConfidence());
        assertTrue(snapshot.showEdgeMeta());
        assertTrue(snapshot.summaryEnabled());
        assertEquals(77, snapshot.maxResultLimit());
    }
}
