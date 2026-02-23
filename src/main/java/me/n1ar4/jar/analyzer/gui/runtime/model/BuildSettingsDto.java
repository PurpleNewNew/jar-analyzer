package me.n1ar4.jar.analyzer.gui.runtime.model;

import java.util.Locale;

public record BuildSettingsDto(
        String projectId,
        String buildInputMode,
        String artifactPath,
        String projectPath,
        String runtimeProfileId,
        boolean resolveNestedJars,
        boolean deleteTempBeforeBuild,
        boolean fixClassPath,
        boolean fixMethodImpl,
        boolean quickMode
) {
    public static final String INPUT_FILE = "file";
    public static final String INPUT_PROJECT = "project";

    public BuildSettingsDto {
        projectId = normalizeId(projectId);
        buildInputMode = normalizeMode(buildInputMode);
        artifactPath = normalizePath(artifactPath);
        projectPath = normalizePath(projectPath);
        runtimeProfileId = normalizeId(runtimeProfileId);
    }

    public static BuildSettingsDto defaults() {
        return new BuildSettingsDto(
                "default",
                INPUT_FILE,
                "",
                "",
                "default-runtime",
                false,
                true,
                false,
                true,
                false
        );
    }

    public boolean isProjectMode() {
        return INPUT_PROJECT.equals(buildInputMode);
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

    public BuildSettingsDto withProjectId(String nextProjectId) {
        return new BuildSettingsDto(
                nextProjectId,
                buildInputMode,
                artifactPath,
                projectPath,
                runtimeProfileId,
                resolveNestedJars,
                deleteTempBeforeBuild,
                fixClassPath,
                fixMethodImpl,
                quickMode
        );
    }

    public BuildSettingsDto withRuntimeProfileId(String nextRuntimeProfileId) {
        return new BuildSettingsDto(
                projectId,
                buildInputMode,
                artifactPath,
                projectPath,
                nextRuntimeProfileId,
                resolveNestedJars,
                deleteTempBeforeBuild,
                fixClassPath,
                fixMethodImpl,
                quickMode
        );
    }

    private static String normalizeMode(String value) {
        String mode = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (INPUT_PROJECT.equals(mode)) {
            return INPUT_PROJECT;
        }
        return INPUT_FILE;
    }

    private static String normalizeId(String value) {
        String out = value == null ? "" : value.trim();
        return out.isEmpty() ? "default" : out;
    }

    private static String normalizePath(String value) {
        return value == null ? "" : value.trim();
    }
}
