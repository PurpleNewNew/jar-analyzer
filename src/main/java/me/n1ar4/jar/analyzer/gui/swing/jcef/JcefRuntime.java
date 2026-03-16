/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.gui.swing.jcef;

import com.jetbrains.cef.JCefAppConfig;
import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import org.cef.CefApp;
import org.cef.CefSettings;
import org.cef.SystemBootstrap;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class JcefRuntime {
    private static final Logger logger = LogManager.getLogger();
    private static final long DEFAULT_SHUTDOWN_WAIT_MS = 1500L;
    private static final String RUNTIME_DIR = "jcef-runtime";
    private static volatile CefApp app;
    private static volatile String failureReason;
    private static final Object INIT_LOCK = new Object();

    private JcefRuntime() {
    }

    public static boolean isAvailable() {
        ensureInitialized();
        return app != null;
    }

    public static String failureReason() {
        ensureInitialized();
        return failureReason == null ? "" : failureReason;
    }

    public static CefApp requireApp() {
        ensureInitialized();
        if (app == null) {
            throw new IllegalStateException(failureReason());
        }
        return app;
    }

    public static void shutdown() {
        shutdown(DEFAULT_SHUTDOWN_WAIT_MS);
    }

    static void shutdown(long waitMs) {
        synchronized (INIT_LOCK) {
            CefApp current = app;
            if (current == null) {
                return;
            }
            try {
                current.dispose();
            } catch (Throwable ex) {
                logger.warn("JCEF runtime dispose failed: {}", ex.toString());
            }
            waitForTermination(current, waitMs);
            app = null;
            failureReason = null;
        }
    }

    private static void ensureInitialized() {
        if (app != null || failureReason != null) {
            return;
        }
        synchronized (INIT_LOCK) {
            if (app != null || failureReason != null) {
                return;
            }
            try {
                Path runtimeRoot = resolveRuntimeRoot();
                Files.createDirectories(runtimeRoot);
                String remoteFlagBefore = safe(System.getProperty("jcef.remote.enabled"));
                System.setProperty("jcef.remote.enabled", "false");
                String remoteFlagAfter = safe(System.getProperty("jcef.remote.enabled"));
                logger.debug("JCEF runtime env: java.home={}, vm={}, vendor={}, os={}, arch={}",
                        safe(System.getProperty("java.home")),
                        safe(System.getProperty("java.vm.name")),
                        safe(System.getProperty("java.vendor")),
                        safe(System.getProperty("os.name")),
                        safe(System.getProperty("os.arch")));
                logger.debug("JCEF runtime remote mode property: before={}, after={}",
                        remoteFlagBefore, remoteFlagAfter);
                ensureJbrJcefModule();
                if (isMac() && !hasQuitLifecycleBridge(CefApp.class)) {
                    logger.warn("JCEF runtime missing CefApp.handleBeforeTerminate bridge, use explicit shutdown workaround");
                }
                JcefConfig resolved = resolveConfig();
                String[] cefArgs = resolved.args();
                boolean startupOk = CefApp.startup(cefArgs);
                logger.debug("JCEF runtime startup result: {}", startupOk);
                CefSettings settings = resolved.settings();
                applyWorkbenchSettings(settings, runtimeRoot);
                app = CefApp.getInstance(cefArgs, settings);
                logger.info("JCEF runtime init success");
                boolean remoteSupported = CefApp.isRemoteSupported();
                boolean remoteEnabled = CefApp.isRemoteEnabled();
                boolean serverPresent = app.getServer() != null;
                logger.debug("JCEF runtime mode: remoteSupported={}, remoteEnabled={}, serverPresent={}",
                        remoteSupported, remoteEnabled, serverPresent);
            } catch (Throwable ex) {
                String reason = normalizeReason(ex);
                failureReason = reason;
                logger.error("JCEF runtime init failed: {}", reason);
            }
        }
    }

    static Path resolveRuntimeRoot() {
        return Path.of(Const.dbDir, RUNTIME_DIR).toAbsolutePath().normalize();
    }

    static void applyWorkbenchSettings(CefSettings settings, Path runtimeRoot) {
        Objects.requireNonNull(settings, "settings");
        Objects.requireNonNull(runtimeRoot, "runtimeRoot");
        settings.windowless_rendering_enabled = false;
        settings.log_severity = CefSettings.LogSeverity.LOGSEVERITY_WARNING;
        settings.persist_session_cookies = false;
        if (safe(settings.locale).isBlank()) {
            settings.locale = "zh-CN";
        }
        // The workbench is fully local and persists through the Java backend, not Chromium state.
        settings.cache_path = "";
        settings.log_file = runtimeRoot.resolve("jcef.log").toAbsolutePath().normalize().toString();
    }

    private static void ensureJbrJcefModule() {
        Module module = CefApp.class.getModule();
        if (module == null || !module.isNamed()) {
            throw new IllegalStateException("JBR JCEF module not found (module unnamed)");
        }
        String moduleName = module.getName();
        if (!"jcef".equals(moduleName)) {
            throw new IllegalStateException("JBR JCEF module not found (current module: " + moduleName + ")");
        }
        String vmName = safeLower(System.getProperty("java.vm.name"));
        String vendor = safeLower(System.getProperty("java.vendor"));
        if (!vmName.contains("jetbrains") && !vendor.contains("jetbrains")) {
            throw new IllegalStateException("JBR runtime required for Cypher workbench");
        }
    }

    private static String safeLower(String value) {
        return Objects.requireNonNullElse(value, "").toLowerCase(Locale.ROOT);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static String normalizeReason(Throwable throwable) {
        if (throwable == null) {
            return "JCEF initialization failed";
        }
        String message = throwable.getMessage();
        if (message == null || message.isBlank()) {
            message = throwable.getClass().getSimpleName();
        }
        return "JCEF unavailable: " + message;
    }

    static boolean hasQuitLifecycleBridge(Class<?> cefAppClass) {
        if (cefAppClass == null) {
            return false;
        }
        try {
            cefAppClass.getDeclaredMethod("handleBeforeTerminate");
            return true;
        } catch (NoSuchMethodException ex) {
            return false;
        } catch (Throwable ex) {
            logger.debug("probe JCEF quit lifecycle bridge failed: {}", ex.toString());
            return false;
        }
    }

    private static void waitForTermination(CefApp current, long waitMs) {
        if (current == null) {
            return;
        }
        long deadline = System.nanoTime() + Math.max(0L, waitMs) * 1_000_000L;
        while (System.nanoTime() < deadline) {
            try {
                if (current.isTerminated()) {
                    return;
                }
            } catch (Throwable ex) {
                logger.debug("query JCEF termination state failed: {}", ex.toString());
                return;
            }
            try {
                Thread.sleep(25L);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        try {
            if (!current.isTerminated()) {
                logger.debug("JCEF runtime still shutting down after {} ms", waitMs);
            }
        } catch (Throwable ex) {
            logger.debug("query final JCEF termination state failed: {}", ex.toString());
        }
    }

    private static JcefConfig resolveConfig() {
        JCefAppConfig config = JCefAppConfig.getInstance();
        List<String> args = new ArrayList<>();
        SystemBootstrap.Loader loader = config.getLoader();
        if (loader != null) {
            SystemBootstrap.setLoader(loader);
        }
        for (String arg : config.getAppArgs()) {
            if (!safe(arg).isBlank()) {
                args.add(arg.trim());
            }
        }
        CefSettings configSettings = config.getCefSettings();
        CefSettings settings = configSettings == null ? new CefSettings() : configSettings.clone();

        appendWorkbenchArgs(args);
        if (isMac()) {
            appendUniqueArg(args, "--use-mock-keychain");
        }
        logger.debug("JCEF runtime args: {}", String.join(" ", args));
        return new JcefConfig(args.toArray(String[]::new), settings);
    }

    static void appendWorkbenchArgs(List<String> args) {
        appendUniqueArg(args, "--disable-notifications");
        appendUniqueArg(args, "--disable-background-networking");
        appendUniqueArg(args, "--disable-component-update");
        appendUniqueArg(args, "--disable-default-apps");
        appendUniqueArg(args, "--disable-sync");
        appendUniqueArg(args, "--metrics-recording-only");
        appendUniqueArg(args, "--no-first-run");
        appendUniqueArg(args, "--no-default-browser-check");
        appendDisableFeatures(args,
                "AutofillServerCommunication",
                "CertificateTransparencyComponentUpdater",
                "MediaRouter",
                "OptimizationHints");
        if (isMac()) {
            appendDisableFeatures(args, "SpareRendererForSitePerProcess");
        }
    }

    private static void appendUniqueArg(List<String> args, String value) {
        if (args == null || value == null || value.isBlank()) {
            return;
        }
        for (String arg : args) {
            if (value.equals(arg)) {
                return;
            }
        }
        args.add(value);
    }

    private static void appendDisableFeatures(List<String> args, String... features) {
        if (args == null || features == null || features.length == 0) {
            return;
        }
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        int targetIndex = -1;
        for (int i = 0; i < args.size(); i++) {
            String arg = safe(args.get(i));
            if (!arg.startsWith("--disable-features=")) {
                continue;
            }
            if (targetIndex < 0) {
                targetIndex = i;
            }
            collectDisableFeatures(merged, arg.substring("--disable-features=".length()));
        }
        for (String feature : features) {
            if (!safe(feature).isBlank()) {
                merged.add(feature.trim());
            }
        }
        if (merged.isEmpty()) {
            return;
        }
        String mergedArg = "--disable-features=" + String.join(",", merged);
        if (targetIndex < 0) {
            args.add(mergedArg);
            return;
        }
        args.set(targetIndex, mergedArg);
        for (int i = args.size() - 1; i >= 0; i--) {
            if (i == targetIndex) {
                continue;
            }
            String arg = safe(args.get(i));
            if (arg.startsWith("--disable-features=")) {
                args.remove(i);
            }
        }
    }

    private static void collectDisableFeatures(LinkedHashSet<String> out, String value) {
        if (out == null || value == null || value.isBlank()) {
            return;
        }
        for (String feature : value.split(",")) {
            if (!safe(feature).isBlank()) {
                out.add(feature.trim());
            }
        }
    }

    private static boolean isMac() {
        return safeLower(System.getProperty("os.name")).contains("mac");
    }

    private record JcefConfig(String[] args, CefSettings settings) {
    }
}
