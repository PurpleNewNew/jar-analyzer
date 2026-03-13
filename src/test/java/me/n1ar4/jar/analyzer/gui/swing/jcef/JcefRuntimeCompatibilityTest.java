package me.n1ar4.jar.analyzer.gui.swing.jcef;

import me.n1ar4.jar.analyzer.starter.Const;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

class JcefRuntimeCompatibilityTest {
    @Test
    void quitLifecycleBridgeProbeShouldMatchMethodPresence() {
        Assertions.assertTrue(JcefRuntime.hasQuitLifecycleBridge(BridgePresent.class));
        Assertions.assertFalse(JcefRuntime.hasQuitLifecycleBridge(BridgeMissing.class));
        Assertions.assertFalse(JcefRuntime.hasQuitLifecycleBridge(null));
    }

    @Test
    void runtimeRootShouldNotLiveUnderBuildTemp() {
        Path runtimeRoot = JcefRuntime.resolveRuntimeRoot();
        Assertions.assertTrue(runtimeRoot.endsWith(Path.of(Const.dbDir, "jcef-runtime")));
        Assertions.assertFalse(runtimeRoot.toString().contains(Const.tempDir));
    }

    @Test
    void profileRootShouldStayInsideRuntimeRoot() {
        Path runtimeRoot = JcefRuntime.resolveRuntimeRoot();
        Path profileRoot = JcefRuntime.resolveProfileRoot(runtimeRoot);
        Assertions.assertTrue(profileRoot.startsWith(runtimeRoot));
        Assertions.assertTrue(profileRoot.endsWith(Path.of("profile")));
    }

    private static final class BridgePresent {
        @SuppressWarnings("unused")
        private void handleBeforeTerminate() {
        }
    }

    private static final class BridgeMissing {
    }
}
