package me.n1ar4.jar.analyzer.gui.runtime.model;

import java.util.Locale;

import me.n1ar4.jar.analyzer.meta.CompatibilityCode;

@CompatibilityCode(
        primary = "Build settings in project/artifact dual-mode fields",
        reason = "Type keeps legacy constructor/accessors so old runtime UI/API call sites remain compatible during migration"
)
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
        boolean quickMode
) {
    public static final String MODE_ARTIFACT = "artifact";
    public static final String MODE_PROJECT = "project";

    public BuildSettingsDto {
        buildMode = normalizeMode(buildMode);
        artifactPath = normalizePath(artifactPath);
        projectPath = normalizePath(projectPath);
        sdkPath = normalizePath(sdkPath);
    }

    /**
     * Legacy constructor kept for compatibility with old call sites.
     */
    public BuildSettingsDto(
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
        this(
                MODE_ARTIFACT,
                inputPath,
                "",
                runtimePath,
                resolveNestedJars,
                addRuntimeJar,
                autoFindRuntimeJar,
                deleteTempBeforeBuild,
                fixClassPath,
                fixMethodImpl,
                quickMode
        );
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

    /**
     * Legacy accessor for old code paths.
     */
    @CompatibilityCode(
            primary = "BuildSettingsDto#activeInputPath",
            reason = "Old call sites still read inputPath in artifact/runtime style"
    )
    public String inputPath() {
        return activeInputPath();
    }

    /**
     * Legacy accessor for old code paths.
     */
    @CompatibilityCode(
            primary = "BuildSettingsDto#sdkPath",
            reason = "Old call sites still read runtimePath while new model uses sdkPath"
    )
    public String runtimePath() {
        return sdkPath;
    }

    /**
     * Legacy accessor for old code paths.
     */
    @CompatibilityCode(
            primary = "BuildSettingsDto#autoDetectSdk",
            reason = "Old build panel API still expects autoFindRuntimeJar"
    )
    public boolean autoFindRuntimeJar() {
        return autoDetectSdk;
    }

    /**
     * Legacy accessor for old code paths.
     */
    @CompatibilityCode(
            primary = "BuildSettingsDto#includeSdk",
            reason = "Old build panel API still expects addRuntimeJar"
    )
    public boolean addRuntimeJar() {
        return includeSdk;
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
