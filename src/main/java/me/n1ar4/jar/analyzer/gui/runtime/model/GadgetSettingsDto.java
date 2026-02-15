package me.n1ar4.jar.analyzer.gui.runtime.model;

public record GadgetSettingsDto(
        String inputDir,
        boolean nativeMode,
        boolean hessian,
        boolean jdbc,
        boolean fastjson
) {
}
