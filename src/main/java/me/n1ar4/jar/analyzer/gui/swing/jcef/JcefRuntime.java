/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.gui.swing.jcef;

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

    private static void ensureInitialized() {
        if (app != null || failureReason != null) {
            return;
        }
        synchronized (INIT_LOCK) {
            if (app != null || failureReason != null) {
                return;
            }
            try {
                Path installRoot = Path.of(Const.tempDir, "jcef-runtime");
                Files.createDirectories(installRoot);
                String remoteFlagBefore = safe(System.getProperty("jcef.remote.enabled"));
                System.setProperty("jcef.remote.enabled", "false");
                String remoteFlagAfter = safe(System.getProperty("jcef.remote.enabled"));
                logger.info("JCEF runtime env: java.home={}, vm={}, vendor={}, os={}, arch={}",
                        safe(System.getProperty("java.home")),
                        safe(System.getProperty("java.vm.name")),
                        safe(System.getProperty("java.vendor")),
                        safe(System.getProperty("os.name")),
                        safe(System.getProperty("os.arch")));
                logger.info("JCEF runtime remote mode property: before={}, after={}",
                        remoteFlagBefore, remoteFlagAfter);
                ensureJbrJcefModule();
                JcefConfig resolved = resolveConfig();
                String[] cefArgs = resolved.args();
                boolean startupOk = invokeStartup(cefArgs);
                logger.info("JCEF runtime startup result: {}", startupOk);
                CefSettings settings = resolved.settings();
                settings.windowless_rendering_enabled = false;
                settings.log_severity = CefSettings.LogSeverity.LOGSEVERITY_WARNING;
                if (safe(settings.locale).isBlank()) {
                    settings.locale = "zh-CN";
                }
                Path cacheRoot = installRoot.resolve("cache");
                Files.createDirectories(cacheRoot);
                settings.cache_path = cacheRoot.toAbsolutePath().toString();
                settings.log_file = new File(installRoot.toFile(), "jcef.log").getAbsolutePath();
                app = CefApp.getInstance(cefArgs, settings);
                logger.info("JCEF runtime init success");
                boolean remoteSupported = invokeStaticBoolean(CefApp.class, "isRemoteSupported");
                boolean remoteEnabled = invokeStaticBoolean(CefApp.class, "isRemoteEnabled");
                boolean serverPresent = invokeNoArg(app, "getServer") != null;
                logger.info("JCEF runtime mode: remoteSupported={}, remoteEnabled={}, serverPresent={}",
                        remoteSupported, remoteEnabled, serverPresent);
            } catch (Throwable ex) {
                String reason = normalizeReason(ex);
                failureReason = reason;
                logger.error("JCEF runtime init failed: {}", reason);
            }
        }
    }

    private static void ensureJbrJcefModule() {
        Module module;
        try {
            Class<?> cefAppClass = Class.forName("org.cef.CefApp");
            module = cefAppClass.getModule();
        } catch (ClassNotFoundException ex) {
            throw new IllegalStateException("JBR JCEF module not found (CefApp class missing)");
        }
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

    private static Object invokeNoArg(Object target, String methodName) {
        if (target == null || methodName == null || methodName.isBlank()) {
            return null;
        }
        try {
            return target.getClass().getMethod(methodName).invoke(target);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean invokeStaticBoolean(Class<?> clazz, String methodName) {
        if (clazz == null || methodName == null || methodName.isBlank()) {
            return false;
        }
        try {
            Object value = clazz.getMethod(methodName).invoke(null);
            if (value instanceof Boolean b) {
                return b;
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private static Object invokeStaticNoArg(Class<?> clazz, String methodName) {
        if (clazz == null || methodName == null || methodName.isBlank()) {
            return null;
        }
        try {
            return clazz.getMethod(methodName).invoke(null);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean invokeStartup(String[] args) {
        try {
            Object result = CefApp.class.getMethod("startup", String[].class).invoke(null, (Object) args);
            if (result instanceof Boolean b) {
                return b;
            }
            return true;
        } catch (NoSuchMethodException ex) {
            return true;
        } catch (Exception ex) {
            logger.warn("JCEF runtime startup invoke failed: {}", ex.toString());
            return false;
        }
    }

    private static JcefConfig resolveConfig() {
        Object config = null;
        try {
            Class<?> configClass = Class.forName("com.jetbrains.cef.JCefAppConfig");
            config = configClass.getMethod("getInstance").invoke(null);
        } catch (Throwable ex) {
            logger.warn("resolve jcef app config failed: {}", ex.toString());
        }

        List<String> args = new ArrayList<>();
        CefSettings settings = new CefSettings();
        if (config != null) {
            try {
                Object loader = invokeNoArg(config, "getLoader");
                if (loader instanceof SystemBootstrap.Loader realLoader) {
                    SystemBootstrap.setLoader(realLoader);
                }
            } catch (Throwable ex) {
                logger.warn("set jcef loader failed: {}", ex.toString());
            }
            try {
                Object configArgs = invokeNoArg(config, "getAppArgs");
                if (configArgs instanceof String[] arr) {
                    for (String arg : arr) {
                        if (!safe(arg).isBlank()) {
                            args.add(arg.trim());
                        }
                    }
                }
            } catch (Throwable ex) {
                logger.warn("read jcef app args failed: {}", ex.toString());
            }
            try {
                Object rawSettings = invokeNoArg(config, "getCefSettings");
                CefSettings configSettings = rawSettings instanceof CefSettings cs ? cs : null;
                if (configSettings != null) {
                    settings = configSettings.clone();
                }
            } catch (Throwable ex) {
                logger.warn("read jcef settings failed: {}", ex.toString());
            }
        }

        appendUniqueArg(args, "--disable-notifications");
        if (isMac()) {
            appendUniqueArg(args, "--disable-features=SpareRendererForSitePerProcess");
            appendUniqueArg(args, "--use-mock-keychain");
        }
        logger.info("JCEF runtime args: {}", String.join(" ", args));
        return new JcefConfig(args.toArray(String[]::new), settings);
    }

    private static String[] buildLegacyCefArgs() {
        List<String> args = new ArrayList<>();
        args.add("--allow-file-access-from-files");
        args.add("--allow-universal-access-from-files");

        Path javaHome = safePath(System.getProperty("java.home"));
        if (javaHome != null) {
            Path homeParent = javaHome.getParent();
            if (homeParent != null) {
                Path framework = homeParent.resolve("Frameworks").resolve("Chromium Embedded Framework.framework");
                Path resources = framework.resolve("Resources");
                Path locales = resources.resolve("locales");
                Path helper = homeParent.resolve("Frameworks").resolve("jcef Helper.app")
                        .resolve("Contents").resolve("MacOS").resolve("jcef Helper");
                if (Files.isDirectory(framework)) {
                    args.add("--framework-dir-path=" + framework.toAbsolutePath());
                }
                if (Files.isDirectory(resources)) {
                    args.add("--resources-dir-path=" + resources.toAbsolutePath());
                }
                if (Files.isDirectory(locales)) {
                    args.add("--locales-dir-path=" + locales.toAbsolutePath());
                }
                if (Files.isRegularFile(helper)) {
                    args.add("--browser-subprocess-path=" + helper.toAbsolutePath());
                }
            }
        }

        logger.info("JCEF runtime args: {}", String.join(" ", args));
        return args.toArray(String[]::new);
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

    private static Path safePath(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Path.of(raw.trim());
        } catch (Exception ex) {
            return null;
        }
    }

    private record JcefConfig(String[] args, CefSettings settings) {
    }
}
