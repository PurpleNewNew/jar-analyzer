package me.n1ar4.jar.analyzer.gui.runtime.model;

public record ApiStartupConfigDto(
        String bind,
        boolean authEnabled,
        int port,
        String token
) {
}
