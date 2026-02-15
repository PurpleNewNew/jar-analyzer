package me.n1ar4.jar.analyzer.gui.runtime.model;

import java.util.List;

public record McpConfigDto(
        String bind,
        boolean authEnabled,
        String token,
        java.util.List<McpLineConfigDto> lines,
        boolean reportWebEnabled,
        String reportWebHost,
        int reportWebPort,
        boolean reportWebRunning
) {
}
