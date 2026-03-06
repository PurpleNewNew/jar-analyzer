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
}
