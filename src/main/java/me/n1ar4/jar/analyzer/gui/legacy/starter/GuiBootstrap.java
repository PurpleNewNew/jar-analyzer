/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.gui.legacy.starter;

import me.n1ar4.jar.analyzer.cli.StartCmd;
import me.n1ar4.jar.analyzer.core.notify.NotifierContext;
import me.n1ar4.jar.analyzer.gui.GlobalOptions;
import me.n1ar4.jar.analyzer.gui.runtime.GuiLauncher;
import me.n1ar4.jar.analyzer.meta.CompatibilityCode;
import me.n1ar4.jar.analyzer.gui.notify.SwingNotifier;
import me.n1ar4.jar.analyzer.server.HttpServer;
import me.n1ar4.jar.analyzer.server.ServerConfig;
import me.n1ar4.jar.analyzer.starter.Single;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import me.n1ar4.log.LoggingStream;

import java.util.ServiceLoader;

/**
 * GUI-only bootstrap logic extracted from {@code starter.Application} to keep the entrypoint headless-friendly.
 */
@CompatibilityCode(
        primary = "GuiLauncher runtime bootstrap",
        reason = "legacy.starter bridge is kept so old entrypoint wiring remains stable during GUI module migration"
)
public final class GuiBootstrap {
    private static final Logger logger = LogManager.getLogger();

    private GuiBootstrap() {
    }

    public static void start(StartCmd startCmd) {
        if (startCmd == null) {
            logger.warn("start gui failed: startCmd is null");
            return;
        }
        try {
            // Install notifier early so pre-GUI checks can still show dialogs.
            NotifierContext.set(new SwingNotifier());
            normalizeThemeArg(startCmd);

            if (!Single.canRun()) {
                System.exit(0);
                return;
            }

            // Redirect stdout/stderr to logger so GUI has consistent output.
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

            Thread.setDefaultUncaughtExceptionHandler(new ExpHandler());

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
        String theme = startCmd.getTheme();
        if (theme == null || theme.trim().isEmpty() || "default".equalsIgnoreCase(theme.trim())) {
            return;
        }
        logger.info("theme [{}] mapped to Swing FlatLaf style", theme);
    }
}
