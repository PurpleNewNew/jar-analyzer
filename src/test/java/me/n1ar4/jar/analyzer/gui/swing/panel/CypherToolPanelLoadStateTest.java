/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.gui.swing.panel;

import org.cef.handler.CefLoadHandler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class CypherToolPanelLoadStateTest {
    @Test
    void shouldTreatErrAbortedAsAbortLikeLoadError() {
        Assertions.assertTrue(CypherToolPanel.isAbortLikeLoadError(CefLoadHandler.ErrorCode.ERR_ABORTED));
    }

    @Test
    void shouldKeepRealLoadFailuresAsHardErrors() {
        Assertions.assertFalse(CypherToolPanel.isAbortLikeLoadError(CefLoadHandler.ErrorCode.ERR_FAILED));
    }

    @Test
    void shouldIgnoreResizeObserverConsoleNoise() {
        Assertions.assertTrue(CypherToolPanel.isIgnorableWorkbenchConsoleMessage(
                "ResizeObserver loop completed with undelivered notifications."));
        Assertions.assertTrue(CypherToolPanel.isIgnorableWorkbenchConsoleMessage(
                "ResizeObserver loop limit exceeded"));
        Assertions.assertFalse(CypherToolPanel.isIgnorableWorkbenchConsoleMessage("real workbench failure"));
    }

    @Test
    void shouldOnlyPushExplicitHostResizeCallback() {
        String script = CypherToolPanel.buildHostResizeScript(1280, 720);
        Assertions.assertTrue(script.contains("window.JA_WORKBENCH.onHostResize(1280, 720);"));
        Assertions.assertFalse(script.contains("window.dispatchEvent"));
    }
}
