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

import fi.iki.elonen.NanoHTTPD;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

public class JarAnalyzerServer extends NanoHTTPD {
    private static final Logger logger = LogManager.getLogger();
    private final PathMatcher matcher;

    public JarAnalyzerServer(ServerConfig config) {
        super(config.getBind(), config.getPort());
        this.matcher = new PathMatcher(config);
        try {
            start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
        } catch (Exception ex) {
            throw new IllegalStateException("bind " + config.getBind() + ":" + config.getPort() + " failed", ex);
        }
        logger.info("api server started: bind={} port={} auth={}",
                config.getBind(), config.getPort(), config.isAuth());
        System.out.print("#######################################################\n");
        if (config.isAuth()) {
            System.out.print("API SERVER ENABLE AUTH TOKEN\n");
        } else {
            System.out.print("API SERVER DISABLE AUTH TOKEN\n");
        }
        System.out.printf("API SERVER BIND %s:%d\n", config.getBind(), config.getPort());
        System.out.printf("API SERVER: http://127.0.0.1:%d\n", config.getPort());
        System.out.print("#######################################################\n");
    }

    @Override
    public Response serve(IHTTPSession session) {
        return matcher.handleReq(session);
    }
}
