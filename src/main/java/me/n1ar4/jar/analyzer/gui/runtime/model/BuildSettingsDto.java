package me.n1ar4.jar.analyzer.gui.runtime.model;

public record BuildSettingsDto(
        String inputPath,
        String sdkPath,
        boolean resolveNestedJars,
        boolean fixClassPath
) {
    public BuildSettingsDto {
        inputPath = normalizePath(inputPath);
        sdkPath = normalizePath(sdkPath);
    }

    public String activeInputPath() {
        return inputPath;
    }

    private static String normalizePath(String value) {
        return value == null ? "" : value.trim();
    }
}
