package me.n1ar4.jar.analyzer.gui.runtime.model;

public record BuildSettingsDto(
        String inputPath,
        String runtimePath,
        boolean resolveNestedJars,
        boolean autoFindRuntimeJar,
        boolean addRuntimeJar,
        boolean deleteTempBeforeBuild,
        boolean fixClassPath,
        boolean fixMethodImpl,
        boolean quickMode
) {
}
