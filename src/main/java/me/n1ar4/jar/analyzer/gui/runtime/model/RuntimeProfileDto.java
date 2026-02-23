package me.n1ar4.jar.analyzer.gui.runtime.model;

public record RuntimeProfileDto(
        String profileId,
        String profileName,
        String runtimePath,
        boolean readOnly,
        boolean valid
) {
    public RuntimeProfileDto {
        profileId = safe(profileId, "default-runtime");
        profileName = safe(profileName, profileId);
        runtimePath = runtimePath == null ? "" : runtimePath.trim();
    }

    private static String safe(String value, String def) {
        String out = value == null ? "" : value.trim();
        return out.isEmpty() ? def : out;
    }
}
