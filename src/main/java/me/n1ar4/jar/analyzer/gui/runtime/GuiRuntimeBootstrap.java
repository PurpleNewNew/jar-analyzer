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

import me.n1ar4.jar.analyzer.core.notify.NotifierContext;
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

    public static void start(GuiStartupOptions startupOptions) {
        GuiStartupOptions options = startupOptions == null ? GuiStartupOptions.defaults() : startupOptions;
        try {
            NotifierContext.set(new SwingNotifier());
            applyMacWindowAppearanceHint(resolveStartupTheme());

            if (!Single.canRun()) {
                System.exit(0);
                return;
            }

            System.setOut(new LoggingStream(System.out, logger));
            System.out.println("set y4-log io-streams");
            System.setErr(new LoggingStream(System.err, logger));
            System.err.println("set y4-log err-streams");

            int port = options.apiPort();
            logger.info("set server port {}", port);

            ServerConfig config = new ServerConfig();
            config.setBind(options.apiBind());
            config.setPort(port);
            config.setAuth(options.apiAuthEnabled());
            config.setToken(options.apiToken());

            RuntimeFacades.clearApiRuntimeConfig();
            Thread.ofVirtual().name("jar-analyzer-http").start(() -> {
                if (HttpServer.start(config) != null) {
                    RuntimeFacades.updateApiRuntimeConfig(config);
                } else {
                    RuntimeFacades.clearApiRuntimeConfig();
                }
            });
            Thread.setDefaultUncaughtExceptionHandler(new RuntimeExceptionHandler());

            GuiLauncher launcher = resolveLauncher(ServiceLoader.load(GuiLauncher.class));
            if (launcher == null) {
                throw new IllegalStateException("gui launcher not found");
            }
            launcher.launch();
        } catch (Exception ex) {
            logger.error("start jar analyzer error: {}", ex.toString());
            throw new IllegalStateException("start gui failed", ex);
        }
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

    private static String resolveStartupTheme() {
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
                        "如果遇到未知报错通常删除当前项目库并重新分析即可解决\n" + output
                );
                logger.error("UNCAUGHT EXCEPTION LOGGED IN JAR-ANALYZER-ERROR.txt");
            } catch (Exception ex) {
                logger.warn("handle thread error: {}", ex.toString());
            }
        }
    }
}
