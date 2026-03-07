/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.starter;

import com.beust.jcommander.JCommander;
import me.n1ar4.jar.analyzer.cli.BuildCmd;
import me.n1ar4.jar.analyzer.cli.Client;
import me.n1ar4.jar.analyzer.config.ConfigEngine;
import me.n1ar4.jar.analyzer.core.notify.NotifierContext;
import me.n1ar4.jar.analyzer.gui.notify.SwingNotifier;
import me.n1ar4.jar.analyzer.gui.runtime.GuiStartupOptions;
import me.n1ar4.jar.analyzer.gui.runtime.GuiRuntimeBootstrap;
import me.n1ar4.jar.analyzer.utils.ColorUtil;
import me.n1ar4.jar.analyzer.utils.ConsoleUtils;
import me.n1ar4.jar.analyzer.utils.InterruptUtil;
import me.n1ar4.jar.analyzer.utils.JNIUtil;
import me.n1ar4.jar.analyzer.utils.OSUtil;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import me.n1ar4.security.Security;

public class Application {
    private static final Logger logger = LogManager.getLogger();

    public static void main(String[] args) {
        Security.setObjectInputFilter();

        if (OSUtil.isWindows()) {
            boolean ok = JNIUtil.extractDllSo("console.dll", null, true);
            if (ok) {
                try {
                    ConsoleUtils.setWindowsColorSupport();
                } catch (Exception ex) {
                    InterruptUtil.restoreInterruptIfNeeded(ex);
                    logger.debug("enable windows console color support failed: {}", ex.toString());
                }
            }
        }

        Logo.print();

        if (isHelp(args)) {
            printUsage();
            return;
        }

        if (isBuildMode(args)) {
            runBuild(args);
            return;
        }

        if (args != null && args.length > 0) {
            if ("gui".equalsIgnoreCase(args[0])) {
                if (args.length > 1) {
                    logger.warn("gui cli params are removed; use GUI/API panel and config file instead");
                }
            } else {
                printUsage();
                return;
            }
        }

        NotifierContext.set(new SwingNotifier());

        System.out.println(ColorUtil.red("###############################################"));
        System.out.println(ColorUtil.green("本项目是免费开源软件，不存在任何商业版本/收费版本"));
        System.out.println(ColorUtil.green("This project is free and open-source software"));
        System.out.println(ColorUtil.green("There are no commercial or paid versions"));
        System.out.println(ColorUtil.red("###############################################"));
        System.out.println();

        Version.logRuntimeVersion();

        GuiStartupOptions startupOptions = GuiStartupOptions.fromConfig(ConfigEngine.parseConfig());
        GuiRuntimeBootstrap.start(startupOptions);
    }

    private static boolean isHelp(String[] args) {
        if (args == null || args.length == 0) {
            return false;
        }
        String first = args[0];
        if (first == null) {
            return false;
        }
        String v = first.trim().toLowerCase();
        return "-h".equals(v) || "--help".equals(v) || "help".equals(v);
    }

    private static boolean isBuildMode(String[] args) {
        return args != null && args.length > 0 && BuildCmd.CMD.equalsIgnoreCase(args[0]);
    }

    private static void runBuild(String[] args) {
        BuildCmd buildCmd = new BuildCmd();
        JCommander commander = JCommander.newBuilder()
                .addCommand(BuildCmd.CMD, buildCmd)
                .build();
        try {
            commander.parse(args);
            if (!BuildCmd.CMD.equals(commander.getParsedCommand())) {
                commander.usage();
                return;
            }
            Client.runBuild(buildCmd);
        } catch (IllegalArgumentException ex) {
            logger.error(ex.getMessage());
            commander.usage();
        } catch (Exception ex) {
            InterruptUtil.restoreInterruptIfNeeded(ex);
            logger.debug("parse build args failed: {}", ex.toString());
            commander.usage();
        }
    }

    private static void printUsage() {
        System.out.println("Usage:");
        System.out.println("  java -jar jar-analyzer.jar");
        System.out.println("  java -jar jar-analyzer.jar gui");
        System.out.println("  java -jar jar-analyzer.jar build --jar <path> [--del-cache] [--inner-jars]");
    }
}
