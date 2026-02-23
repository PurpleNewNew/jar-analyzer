package me.n1ar4.jar.analyzer.gui.runtime.model;

public record ProjectSessionSummaryDto(
        String projectId,
        String projectName,
        String inputPath,
        boolean projectMode,
        long updatedAt
) {
    public ProjectSessionSummaryDto {
        projectId = safe(projectId, "default");
        projectName = safe(projectName, projectId);
        inputPath = inputPath == null ? "" : inputPath.trim();
    }

    private static String safe(String value, String def) {
        String out = value == null ? "" : value.trim();
        return out.isEmpty() ? def : out;
    }
}
