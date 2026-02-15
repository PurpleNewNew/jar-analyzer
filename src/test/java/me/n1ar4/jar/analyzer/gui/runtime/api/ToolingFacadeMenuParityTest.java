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

import me.n1ar4.jar.analyzer.gui.runtime.model.ToolingConfigSnapshotDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.ToolingWindowAction;
import me.n1ar4.jar.analyzer.gui.runtime.model.ToolingWindowRequest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolingFacadeMenuParityTest {
    @Test
    void configTogglesShouldRoundTripSnapshot() {
        ToolingFacade tooling = RuntimeFacades.tooling();
        ToolingConfigSnapshotDto before = tooling.configSnapshot();

        tooling.toggleShowInnerClass();
        tooling.toggleFixClassPath();
        tooling.toggleFixMethodImpl();
        tooling.toggleQuickMode();
        tooling.toggleGroupTreeByJar();
        tooling.toggleMergePackageRoot();
        tooling.setSortByMethod();
        tooling.setLanguageEnglish();
        tooling.useThemeDark();

        ToolingConfigSnapshotDto after = tooling.configSnapshot();
        assertEquals(!before.showInnerClass(), after.showInnerClass());
        assertEquals(!before.fixClassPath(), after.fixClassPath());
        assertEquals(!before.fixMethodImpl(), after.fixMethodImpl());
        assertEquals(!before.quickMode(), after.quickMode());
        assertEquals(!before.groupTreeByJar(), after.groupTreeByJar());
        assertEquals(!before.mergePackageRoot(), after.mergePackageRoot());
        assertEquals(true, after.sortByMethod());
        assertEquals(false, after.sortByClass());
        assertEquals("en", after.language());
        assertEquals("dark", after.theme());
    }

    @Test
    void viewerMenuActionsShouldEmitWindowRequests() {
        List<ToolingWindowRequest> requests = new ArrayList<>();
        RuntimeFacades.setToolingWindowConsumer(requests::add);
        try {
            ToolingFacade tooling = RuntimeFacades.tooling();
            tooling.openVersionInfo();
            tooling.openChangelog();
            tooling.openThanks();
            assertTrue(requests.stream().anyMatch(it -> it.action() == ToolingWindowAction.TEXT_VIEWER));
            assertTrue(requests.stream().anyMatch(it -> it.action() == ToolingWindowAction.MARKDOWN_VIEWER));
        } finally {
            RuntimeFacades.setToolingWindowConsumer(null);
        }
    }
}
