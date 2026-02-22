package me.n1ar4.jar.analyzer.gui.runtime.model;

import java.util.Locale;

public record BuildSettingsDto(
        String buildMode,
        String artifactPath,
        String projectPath,
        String sdkPath,
        boolean resolveNestedJars,
        boolean includeSdk,
        boolean autoDetectSdk,
        boolean deleteTempBeforeBuild,
        boolean fixClassPath,
        boolean fixMethodImpl,
        boolean quickMode,
        String projectKey,
        String projectAlias
) {
    public static final String MODE_ARTIFACT = "artifact";
    public static final String MODE_PROJECT = "project";

    public BuildSettingsDto {
        buildMode = normalizeMode(buildMode);
        artifactPath = normalizePath(artifactPath);
        projectPath = normalizePath(projectPath);
        sdkPath = normalizePath(sdkPath);
        projectKey = normalizePath(projectKey);
        projectAlias = normalizePath(projectAlias);
    }

    public boolean isProjectMode() {
        return MODE_PROJECT.equals(buildMode);
    }

    public String activeInputPath() {
        if (isProjectMode()) {
            if (!projectPath.isBlank()) {
                return projectPath;
            }
            return artifactPath;
        }
        if (!artifactPath.isBlank()) {
            return artifactPath;
        }
        return projectPath;
    }

    private static String normalizeMode(String value) {
        String mode = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (MODE_PROJECT.equals(mode)) {
            return MODE_PROJECT;
        }
        return MODE_ARTIFACT;
    }

    private static String normalizePath(String value) {
        return value == null ? "" : value.trim();
    }
}
