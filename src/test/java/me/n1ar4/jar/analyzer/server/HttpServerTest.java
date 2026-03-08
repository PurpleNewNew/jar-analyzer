/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.server;

import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.ServerSocket;

import static org.junit.jupiter.api.Assertions.assertNull;

class HttpServerTest {
    @Test
    void startShouldReturnNullWhenPortAlreadyBoundOnConfiguredHost() throws Exception {
        try (ServerSocket socket = new ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"))) {
            ServerConfig cfg = new ServerConfig();
            cfg.setBind("127.0.0.1");
            cfg.setPort(socket.getLocalPort());

            assertNull(HttpServer.start(cfg));
        }
    }
}
