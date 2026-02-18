/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.gui.runtime.api;

import me.n1ar4.jar.analyzer.gui.runtime.model.ToolingWindowAction;
import me.n1ar4.jar.analyzer.gui.runtime.model.ToolingWindowRequest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolingFacadePluginTest {
    @Test
    void toolingFacadeShouldEmitMappedActions() {
        List<ToolingWindowRequest> requests = new ArrayList<>();
        RuntimeFacades.setToolingWindowConsumer(requests::add);
        try {
            ToolingFacade tooling = RuntimeFacades.tooling();
            tooling.openExportTool();
            tooling.openRemoteLoadTool();
            tooling.openProxyTool();
            tooling.openPartitionTool();
            tooling.openGlobalSearchTool();
            tooling.openSystemMonitorTool();
            tooling.openCfgTool();
            tooling.openFrameTool(true);
            tooling.openOpcodeTool();
            tooling.openAsmTool();
            tooling.openElSearchTool();
            tooling.openSqlConsoleTool();
            tooling.openCypherConsoleTool();
            tooling.openEncodeTool();
            tooling.openListenerTool();
            tooling.openSerializationTool();
            tooling.openBcelTool();
            tooling.openRepeaterTool();
            tooling.openObfuscationTool();

            assertTrue(requests.stream().anyMatch(it -> ToolingWindowAction.EXPORT == it.action()));
            assertTrue(requests.stream().anyMatch(it -> ToolingWindowAction.REMOTE_LOAD == it.action()));
            assertTrue(requests.stream().anyMatch(it -> ToolingWindowAction.PROXY == it.action()));
            assertTrue(requests.stream().anyMatch(it -> ToolingWindowAction.PARTITION == it.action()));
            assertTrue(requests.stream().anyMatch(it -> ToolingWindowAction.GLOBAL_SEARCH == it.action()));
            assertTrue(requests.stream().anyMatch(it -> ToolingWindowAction.SYSTEM_MONITOR == it.action()));
            assertTrue(requests.stream().anyMatch(it -> ToolingWindowAction.CFG == it.action()));
            assertTrue(requests.stream().anyMatch(it -> ToolingWindowAction.FRAME == it.action()));
            assertTrue(requests.stream().anyMatch(it -> ToolingWindowAction.OPCODE == it.action()));
            assertTrue(requests.stream().anyMatch(it -> ToolingWindowAction.ASM == it.action()));
            assertTrue(requests.stream().anyMatch(it -> ToolingWindowAction.EL_SEARCH == it.action()));
            assertTrue(requests.stream().anyMatch(it -> ToolingWindowAction.SQL_CONSOLE == it.action()));
            assertTrue(requests.stream().anyMatch(it -> ToolingWindowAction.CYPHER_CONSOLE == it.action()));
            assertTrue(requests.stream().anyMatch(it -> ToolingWindowAction.ENCODE_TOOL == it.action()));
            assertTrue(requests.stream().anyMatch(it -> ToolingWindowAction.SOCKET_LISTENER == it.action()));
            assertTrue(requests.stream().anyMatch(it -> ToolingWindowAction.SERIALIZATION == it.action()));
            assertTrue(requests.stream().anyMatch(it -> ToolingWindowAction.BCEL_TOOL == it.action()));
            assertTrue(requests.stream().anyMatch(it -> ToolingWindowAction.REPEATER == it.action()));
            assertTrue(requests.stream().anyMatch(it -> ToolingWindowAction.OBFUSCATION == it.action()));
        } finally {
            RuntimeFacades.setToolingWindowConsumer(null);
        }
    }
}
