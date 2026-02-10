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
import me.n1ar4.jar.analyzer.gui.MainForm;
import me.n1ar4.jar.analyzer.gui.notify.SwingNotifier;
import me.n1ar4.jar.analyzer.server.HttpServer;
import me.n1ar4.jar.analyzer.server.ServerConfig;
import me.n1ar4.jar.analyzer.starter.Single;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import me.n1ar4.log.LoggingStream;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 * GUI-only bootstrap logic extracted from {@code starter.Application} to keep the entrypoint headless-friendly.
 */
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

            ThemeHelper.process(startCmd);

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

            Thread serverThread = new Thread(() -> HttpServer.start(config), "jar-analyzer-http");
            serverThread.setDaemon(true);
            serverThread.start();

            GlobalOptions.setServerConfig(config);

            Thread.setDefaultUncaughtExceptionHandler(new ExpHandler());

            // Fix 2024/11/20: for some Linux environments, skip splash animations.
            if (startCmd.isSkipLoad()) {
                SwingUtilities.invokeLater(() -> {
                    try {
                        JFrame frame = MainForm.start();
                        frame.setVisible(true);
                    } catch (Exception ex) {
                        logger.error("start jar analyzer error: {}", ex.toString());
                    }
                });
            } else {
                SwingUtilities.invokeLater(() -> {
                    try {
                        StartUpMessage.run();
                    } catch (Exception ex) {
                        logger.error("start jar analyzer error: {}", ex.toString());
                    }
                });
            }
        } catch (Exception ex) {
            logger.error("start jar analyzer error: {}", ex.toString());
        }
    }
}
