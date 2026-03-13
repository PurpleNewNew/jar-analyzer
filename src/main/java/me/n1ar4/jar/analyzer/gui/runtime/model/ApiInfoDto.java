package me.n1ar4.jar.analyzer.gui.runtime.model;

public record ApiInfoDto(
        boolean running,
        String bind,
        boolean authEnabled,
        int port,
        String maskedToken
) {
}
