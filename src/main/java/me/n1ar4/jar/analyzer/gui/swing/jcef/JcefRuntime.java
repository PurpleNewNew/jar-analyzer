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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class JcefRuntime {
    private static final Logger logger = LogManager.getLogger();
    private static final long DEFAULT_SHUTDOWN_WAIT_MS = 1500L;
    private static final String RUNTIME_DIR = "jcef-runtime";
    private static final String PROFILE_DIR = "profile";
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
                settings.windowless_rendering_enabled = false;
                settings.log_severity = CefSettings.LogSeverity.LOGSEVERITY_WARNING;
                if (safe(settings.locale).isBlank()) {
                    settings.locale = "zh-CN";
                }
                Path profileRoot = resolveProfileRoot(runtimeRoot);
                Files.createDirectories(profileRoot);
                settings.cache_path = profileRoot.toAbsolutePath().toString();
                settings.log_file = new File(runtimeRoot.toFile(), "jcef.log").getAbsolutePath();
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

    static Path resolveProfileRoot(Path runtimeRoot) {
        Objects.requireNonNull(runtimeRoot, "runtimeRoot");
        return runtimeRoot.resolve(PROFILE_DIR).toAbsolutePath().normalize();
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

        appendUniqueArg(args, "--disable-notifications");
        if (isMac()) {
            appendUniqueArg(args, "--disable-features=SpareRendererForSitePerProcess");
            appendUniqueArg(args, "--use-mock-keychain");
        }
        logger.debug("JCEF runtime args: {}", String.join(" ", args));
        return new JcefConfig(args.toArray(String[]::new), settings);
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

    private static boolean isMac() {
        return safeLower(System.getProperty("os.name")).contains("mac");
    }

    private record JcefConfig(String[] args, CefSettings settings) {
    }
}
