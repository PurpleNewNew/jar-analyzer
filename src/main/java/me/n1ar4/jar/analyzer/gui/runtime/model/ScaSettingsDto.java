package me.n1ar4.jar.analyzer.gui.runtime.model;

public record ScaSettingsDto(
        boolean scanLog4j,
        boolean scanShiro,
        boolean scanFastjson,
        String inputPath,
        ScaOutputMode outputMode,
        String outputFile
) {
}
