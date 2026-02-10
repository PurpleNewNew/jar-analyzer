/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.utils;

import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

public class SocketUtil {
    private static final Logger logger = LogManager.getLogger();

    public static boolean isPortInUse(String host, int port) {
        if (host == null || host.trim().isEmpty()) {
            return false;
        }
        if (port < 1 || port > 65535) {
            return false;
        }
        try (Socket ignored = new Socket(host, port)) {
            return true;
        } catch (UnknownHostException ex) {
            logger.debug("port check unknown host: {}:{}: {}", host, port, ex.toString());
            return false;
        } catch (IOException ex) {
            // Usually Connection refused / timed out, which simply means the port is not in use.
            logger.debug("port check failed: {}:{}: {}", host, port, ex.toString());
            return false;
        }
    }
}
