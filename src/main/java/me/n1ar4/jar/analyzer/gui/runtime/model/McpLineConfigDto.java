package me.n1ar4.jar.analyzer.gui.runtime.model;

public record McpLineConfigDto(
        McpLineKey key,
        boolean enabled,
        int port,
        boolean running
) {
}
