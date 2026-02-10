/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.server;

import me.n1ar4.jar.analyzer.core.notify.NotifierContext;
import me.n1ar4.jar.analyzer.utils.InterruptUtil;
import me.n1ar4.jar.analyzer.utils.SocketUtil;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

public class HttpServer {
    private static final Logger logger = LogManager.getLogger();

    public static void start(ServerConfig config) {
        if (SocketUtil.isPortInUse("localhost", config.getPort())) {
            String msg = "cannot start api server: port " + config.getPort() + " is in use";
            logger.warn(msg);
            try {
                NotifierContext.get().warn("Jar Analyzer", msg);
            } catch (Throwable t) {
                InterruptUtil.restoreInterruptIfNeeded(t);
                if (t instanceof Error) {
                    throw (Error) t;
                }
                logger.debug("notifier warn failed: {}", t.toString());
            }
            return;
        }
        new JarAnalyzerServer(config);
    }
}
