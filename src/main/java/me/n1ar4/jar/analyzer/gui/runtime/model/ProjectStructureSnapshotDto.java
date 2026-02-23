package me.n1ar4.jar.analyzer.gui.runtime.model;

import java.util.List;

public record ProjectStructureSnapshotDto(
        String projectId,
        String selectedRuntimeProfileId,
        List<RuntimeProfileDto> runtimeProfiles
) {
    public ProjectStructureSnapshotDto {
        projectId = normalize(projectId, "default");
        selectedRuntimeProfileId = normalize(selectedRuntimeProfileId, "default-runtime");
        runtimeProfiles = runtimeProfiles == null ? List.of() : List.copyOf(runtimeProfiles);
    }

    private static String normalize(String value, String def) {
        String out = value == null ? "" : value.trim();
        return out.isEmpty() ? def : out;
    }
}
