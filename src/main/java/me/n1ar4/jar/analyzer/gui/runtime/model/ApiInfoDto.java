package me.n1ar4.jar.analyzer.gui.runtime.model;

public record ApiInfoDto(
        String bind,
        boolean authEnabled,
        int port,
        String maskedToken
) {
}
