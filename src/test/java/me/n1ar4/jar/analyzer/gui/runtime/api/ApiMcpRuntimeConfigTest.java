package me.n1ar4.jar.analyzer.gui.runtime.api;

import me.n1ar4.jar.analyzer.gui.runtime.model.ApiInfoDto;
import me.n1ar4.jar.analyzer.server.ServerConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiMcpRuntimeConfigTest {
    @AfterEach
    void cleanup() {
        RuntimeFacades.clearApiRuntimeConfig();
    }

    @Test
    void apiInfoShouldBeEmptyWhenApiServerIsNotRunning() {
        RuntimeFacades.clearApiRuntimeConfig();

        ApiInfoDto info = RuntimeFacades.apiMcp().apiInfo();

        assertFalse(info.running());
        assertEquals("", info.bind());
        assertEquals(0, info.port());
        assertEquals("", info.maskedToken());
    }

    @Test
    void apiInfoShouldReflectLiveRuntimeConfigOnly() {
        ServerConfig config = new ServerConfig();
        config.setBind("127.0.0.1");
        config.setPort(18080);
        config.setAuth(true);
        config.setToken("runtime-token");
        RuntimeFacades.updateApiRuntimeConfig(config);

        ApiInfoDto info = RuntimeFacades.apiMcp().apiInfo();

        assertTrue(info.running());
        assertEquals("127.0.0.1", info.bind());
        assertEquals(18080, info.port());
        assertTrue(info.authEnabled());
        assertTrue(info.maskedToken().contains("***"));
    }
}
