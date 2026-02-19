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
import me.n1ar4.jar.analyzer.cli.StartCmd;
import me.n1ar4.jar.analyzer.core.notify.NotifierContext;
import me.n1ar4.jar.analyzer.gui.GlobalOptions;
import me.n1ar4.jar.analyzer.gui.notify.SwingNotifier;
import me.n1ar4.jar.analyzer.gui.runtime.GuiRuntimeBootstrap;
import me.n1ar4.jar.analyzer.http.Y4Client;
import me.n1ar4.jar.analyzer.utils.*;
import me.n1ar4.log.LogLevel;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import me.n1ar4.security.Security;

public class Application {
    private static final Logger logger = LogManager.getLogger();
    @SuppressWarnings("all")
    public static final BuildCmd buildCmd = new BuildCmd();
    @SuppressWarnings("all")
    public static final StartCmd startCmd = new StartCmd();

    /**
     * Main Method
     * 　　 へ　　　　　／|
     * 　　/＼7　　　 ∠＿/
     * 　 /　│　　 ／　／
     * 　│　Z ＿,＜　／　　 /`ヽ
     * 　│　　　　　ヽ　　 /　　〉
     * 　 Y　　　　　`　 /　　/
     * 　?●　?　●　　??〈　　/
     * 　()　 へ　　　　|　＼〈
     * 　　>? ?_　 ィ　 │ ／／
     * 　 / へ　　 /　?＜| ＼＼
     * 　 ヽ_?　　(_／　 │／／
     * 　　7　　　　　　　|／
     * 　　＞―r￣￣~∠--|
     */
    public static void main(String[] args) {
        // SET OBJECT INPUT FILTER
        Security.setObjectInputFilter();

        // CHECK WINDOWS
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

        // PRINT LOGO
        Logo.print();

        JCommander commander = JCommander.newBuilder()
                .addCommand(BuildCmd.CMD, buildCmd)
                .addCommand(StartCmd.CMD, startCmd)
                .build();

        try {
            commander.parse(args);
        } catch (Exception ex) {
            InterruptUtil.restoreInterruptIfNeeded(ex);
            logger.debug("parse args failed: {}", ex.toString());
            commander.usage();
            return;
        }

        // Ensure GUI preflight warnings are delivered through the active GUI notifier.
        if (StartCmd.CMD.equals(commander.getParsedCommand())) {
            NotifierContext.set(new SwingNotifier());
        }

        GlobalOptions.setSecurity(startCmd.isSecurityMode());

        // DISABLE HTTP
        if (startCmd.isNoHttp()) {
            Y4Client.enabled = false;
        }

        // SET LOG LEVEL (debug|info|warn|error)
        String logLevelStr = startCmd.getLogLevel();
        LogLevel logLevel = (logLevelStr == null || logLevelStr.isBlank())
                ? LogLevel.INFO
                : switch (logLevelStr.trim()) {
            case "debug" -> LogLevel.DEBUG;
            case "warn" -> LogLevel.WARN;
            case "error" -> LogLevel.ERROR;
            default -> LogLevel.INFO;
        };
        LogManager.setLevel(logLevel);

        System.out.println(ColorUtil.red("###############################################"));
        System.out.println(ColorUtil.green("本项目是免费开源软件，不存在任何商业版本/收费版本"));
        System.out.println(ColorUtil.green("This project is free and open-source software"));
        System.out.println(ColorUtil.green("There are no commercial or paid versions"));
        System.out.println(ColorUtil.red("###############################################"));
        System.out.println();

        // VERSION CHECK
        Version.check();

        Client.run(commander, buildCmd);
        GuiRuntimeBootstrap.start(startCmd);
    }
}
