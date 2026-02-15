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

import me.n1ar4.jar.analyzer.core.notify.NotifierContext;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ExpHandler implements Thread.UncaughtExceptionHandler {
    private static final Logger logger = LogManager.getLogger();

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        try {
            Class<?> edtClass = Class.forName("java.awt.EventDispatchThread");
            if (edtClass.isInstance(t)) {
                if (e instanceof ArrayIndexOutOfBoundsException) {
                    // 这里是一处已知的异常
                    // java/util/Vector elementAt ArrayIndexOutOfBoundsException
                    return;
                }
            }

            Path errorLogPath = Paths.get("JAR-ANALYZER-ERROR.txt");

            // 处理下异常：不抛出异常 记录到当前目录
            FileOutputStream fos = new FileOutputStream(errorLogPath.toFile());
            PrintWriter ps = new PrintWriter(fos);
            e.printStackTrace(ps);
            ps.flush();
            ps.close();

            byte[] data = Files.readAllBytes(errorLogPath);
            String output = new String(data, StandardCharsets.UTF_8);
            NotifierContext.get().error(
                    "Jar Analyzer Error",
                    "如果遇到未知报错通常删除 jar-analyzer.db 重新分析即可解决\n" + output
            );

            logger.error("UNCAUGHT EXCEPTION LOGGED IN JAR-ANALYZER-ERROR.txt");
        } catch (Exception ex) {
            logger.warn("handle thread error: {}", ex.toString());
        }
    }
}
