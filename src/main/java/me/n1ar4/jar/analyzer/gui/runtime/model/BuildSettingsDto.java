package me.n1ar4.jar.analyzer.gui.runtime.model;

import me.n1ar4.jar.analyzer.core.runtime.JdkArchiveResolver;

public record BuildSettingsDto(
        String inputPath,
        String sdkPath,
        boolean resolveNestedJars,
        boolean fixClassPath,
        String jdkModules
) {
    public BuildSettingsDto {
        inputPath = normalizePath(inputPath);
        sdkPath = normalizePath(sdkPath);
        jdkModules = normalizeJdkModules(jdkModules);
    }

    public BuildSettingsDto(String inputPath,
                            String sdkPath,
                            boolean resolveNestedJars,
                            boolean fixClassPath) {
        this(inputPath, sdkPath, resolveNestedJars, fixClassPath, JdkArchiveResolver.DEFAULT_MODULE_POLICY);
    }

    public String activeInputPath() {
        return inputPath;
    }

    private static String normalizePath(String value) {
        return value == null ? "" : value.trim();
    }

    private static String normalizeJdkModules(String value) {
        return JdkArchiveResolver.normalizePolicy(value);
    }
}
