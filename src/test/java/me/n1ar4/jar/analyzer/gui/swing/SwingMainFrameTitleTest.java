package me.n1ar4.jar.analyzer.gui.swing;

import me.n1ar4.jar.analyzer.gui.runtime.model.BuildSnapshotDto;
import me.n1ar4.jar.analyzer.storage.neo4j.ActiveProjectContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SwingMainFrameTitleTest {
    @Test
    void windowTitleShouldUseProjectAlias() {
        BuildSnapshotDto snapshot = new BuildSnapshotDto(
                null, "", 0, "", "", "", "", "", "",
                "demo-project", "springboot-test", 0L
        );
        Assertions.assertEquals("springboot-test - Jar Analyzer", SwingMainFrame.windowTitle(snapshot));
    }

    @Test
    void windowTitleShouldUseTemporaryProjectLabel() {
        BuildSnapshotDto snapshot = new BuildSnapshotDto(
                null, "", 0, "", "", "", "", "", "",
                ActiveProjectContext.temporaryProjectKey(), "", 0L
        );
        Assertions.assertEquals("临时项目 - Jar Analyzer", SwingMainFrame.windowTitle(snapshot));
    }
}
