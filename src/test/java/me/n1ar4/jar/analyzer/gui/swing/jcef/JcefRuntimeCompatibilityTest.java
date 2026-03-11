package me.n1ar4.jar.analyzer.gui.swing.jcef;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class JcefRuntimeCompatibilityTest {
    @Test
    void quitLifecycleBridgeProbeShouldMatchMethodPresence() {
        Assertions.assertTrue(JcefRuntime.hasQuitLifecycleBridge(BridgePresent.class));
        Assertions.assertFalse(JcefRuntime.hasQuitLifecycleBridge(BridgeMissing.class));
        Assertions.assertFalse(JcefRuntime.hasQuitLifecycleBridge(null));
    }

    private static final class BridgePresent {
        @SuppressWarnings("unused")
        private void handleBeforeTerminate() {
        }
    }

    private static final class BridgeMissing {
    }
}
