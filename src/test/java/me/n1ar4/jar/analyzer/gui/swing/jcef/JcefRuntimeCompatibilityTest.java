package me.n1ar4.jar.analyzer.gui.swing.jcef;

import me.n1ar4.jar.analyzer.starter.Const;
import org.cef.CefSettings;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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
    void workbenchSettingsShouldDisableDiskCache() {
        Path runtimeRoot = JcefRuntime.resolveRuntimeRoot();
        CefSettings settings = new CefSettings();
        JcefRuntime.applyWorkbenchSettings(settings, runtimeRoot);
        Assertions.assertEquals("", settings.cache_path);
        Assertions.assertFalse(settings.persist_session_cookies);
        Assertions.assertTrue(Path.of(settings.log_file).normalize().startsWith(runtimeRoot));
    }

    @Test
    void workbenchArgsShouldDisableBackgroundComponents() {
        List<String> args = new ArrayList<>();
        JcefRuntime.appendWorkbenchArgs(args);
        Assertions.assertTrue(args.contains("--disable-background-networking"));
        Assertions.assertTrue(args.contains("--disable-component-update"));
        Assertions.assertTrue(args.contains("--disable-default-apps"));
        Assertions.assertTrue(args.contains("--disable-sync"));
        Assertions.assertTrue(args.contains("--metrics-recording-only"));
        Assertions.assertTrue(args.contains("--no-first-run"));
        Assertions.assertTrue(args.stream().anyMatch(arg ->
                arg.startsWith("--disable-features=")
                        && arg.contains("CertificateTransparencyComponentUpdater")
                        && arg.contains("OptimizationHints")));
    }

    private static final class BridgePresent {
        @SuppressWarnings("unused")
        private void handleBeforeTerminate() {
        }
    }

    private static final class BridgeMissing {
    }
}
