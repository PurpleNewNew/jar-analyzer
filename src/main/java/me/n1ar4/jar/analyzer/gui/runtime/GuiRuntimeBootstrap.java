/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.gui.runtime;

import me.n1ar4.jar.analyzer.cli.StartCmd;
import me.n1ar4.jar.analyzer.core.notify.NotifierContext;
import me.n1ar4.jar.analyzer.gui.GlobalOptions;
import me.n1ar4.jar.analyzer.gui.notify.SwingNotifier;
import me.n1ar4.jar.analyzer.gui.runtime.api.RuntimeFacades;
import me.n1ar4.jar.analyzer.server.HttpServer;
import me.n1ar4.jar.analyzer.server.ServerConfig;
import me.n1ar4.jar.analyzer.starter.Single;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import me.n1ar4.log.LoggingStream;

import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.ServiceLoader;

public final class GuiRuntimeBootstrap {
    private static final Logger logger = LogManager.getLogger();

    private GuiRuntimeBootstrap() {
    }

    public static void start(StartCmd startCmd) {
        if (startCmd == null) {
            logger.warn("start gui failed: startCmd is null");
            return;
        }
        try {
            NotifierContext.set(new SwingNotifier());
            normalizeThemeArg(startCmd);
            applyMacWindowAppearanceHint(resolveStartupTheme(startCmd));

            if (!Single.canRun()) {
                System.exit(0);
                return;
            }

            System.setOut(new LoggingStream(System.out, logger));
            System.out.println("set y4-log io-streams");
            System.setErr(new LoggingStream(System.err, logger));
            System.err.println("set y4-log err-streams");

            int port = startCmd.getPort();
            if (port < 1 || port > 65535) {
                port = 10032;
            }
            logger.info("set server port {}", port);

            ServerConfig config = new ServerConfig();
            config.setBind(startCmd.getServerBind());
            config.setPort(port);
            config.setAuth(startCmd.isServerAuth());
            config.setToken(startCmd.getServerToken());

            Thread.ofVirtual().name("jar-analyzer-http").start(() -> HttpServer.start(config));
            GlobalOptions.setServerConfig(config);
            Thread.setDefaultUncaughtExceptionHandler(new RuntimeExceptionHandler());

            GuiLauncher launcher = loadLauncher();
            if (launcher == null) {
                throw new IllegalStateException("gui launcher not found");
            }
            launcher.launch(startCmd);
        } catch (Exception ex) {
            logger.error("start jar analyzer error: {}", ex.toString());
            throw new IllegalStateException("start gui failed", ex);
        }
    }

    private static GuiLauncher loadLauncher() {
        ServiceLoader<GuiLauncher> loader = ServiceLoader.load(GuiLauncher.class);
        return resolveLauncher(loader);
    }

    static GuiLauncher resolveLauncher(Iterable<GuiLauncher> launchers) {
        if (launchers == null) {
            logger.error("gui launcher not found");
            return null;
        }
        for (GuiLauncher launcher : launchers) {
            if (launcher == null) {
                continue;
            }
            logger.info("load gui launcher: {}", launcher.getClass().getName());
            return launcher;
        }
        logger.error("gui launcher not found");
        return null;
    }

    private static void normalizeThemeArg(StartCmd startCmd) {
        String raw = startCmd == null ? null : startCmd.getTheme();
        if (raw == null || raw.trim().isEmpty()) {
            return;
        }
        String normalized = normalizeTheme(raw);
        if (normalized == null) {
            logger.warn("unsupported theme [{}], fallback to config/default", raw);
            return;
        }
        logger.info("theme [{}] mapped to Swing FlatLaf style", normalized);
        try {
            switch (normalized) {
                case "dark" -> RuntimeFacades.tooling().useThemeDark();
                default -> RuntimeFacades.tooling().useThemeDefault();
            }
        } catch (Throwable ex) {
            logger.warn("apply startup theme override failed: {}", ex.toString());
        }
    }

    private static String resolveStartupTheme(StartCmd startCmd) {
        String cliTheme = normalizeTheme(startCmd == null ? null : startCmd.getTheme());
        if (cliTheme != null) {
            return cliTheme;
        }
        try {
            String runtimeTheme = RuntimeFacades.tooling().configSnapshot().theme();
            String normalizedRuntime = normalizeTheme(runtimeTheme);
            if (normalizedRuntime != null) {
                return normalizedRuntime;
            }
        } catch (Throwable ex) {
            logger.debug("resolve startup theme from runtime config failed: {}", ex.toString());
        }
        return "default";
    }

    private static void applyMacWindowAppearanceHint(String normalizedTheme) {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (!os.contains("mac")) {
            return;
        }
        if ("dark".equalsIgnoreCase(normalizedTheme)) {
            System.setProperty("apple.awt.application.appearance", "NSAppearanceNameDarkAqua");
        } else {
            System.clearProperty("apple.awt.application.appearance");
        }
    }

    private static String normalizeTheme(String theme) {
        if (theme == null || theme.trim().isEmpty()) {
            return null;
        }
        String normalized = theme.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "default", "dark" -> normalized;
            default -> null;
        };
    }

    private static final class RuntimeExceptionHandler implements Thread.UncaughtExceptionHandler {
        @Override
        public void uncaughtException(Thread t, Throwable e) {
            try {
                Class<?> edtClass = Class.forName("java.awt.EventDispatchThread");
                if (edtClass.isInstance(t) && e instanceof ArrayIndexOutOfBoundsException) {
                    return;
                }
                Path errorLogPath = Paths.get("JAR-ANALYZER-ERROR.txt");
                try (FileOutputStream fos = new FileOutputStream(errorLogPath.toFile());
                     PrintWriter ps = new PrintWriter(fos)) {
                    e.printStackTrace(ps);
                    ps.flush();
                }
                byte[] data = Files.readAllBytes(errorLogPath);
                String output = new String(data, StandardCharsets.UTF_8);
                NotifierContext.get().error(
                        "Jar Analyzer Error",
                        "如果遇到未知报错通常删除 db/neo4j-home 重新分析即可解决\n" + output
                );
                logger.error("UNCAUGHT EXCEPTION LOGGED IN JAR-ANALYZER-ERROR.txt");
            } catch (Exception ex) {
                logger.warn("handle thread error: {}", ex.toString());
            }
        }
    }
}
