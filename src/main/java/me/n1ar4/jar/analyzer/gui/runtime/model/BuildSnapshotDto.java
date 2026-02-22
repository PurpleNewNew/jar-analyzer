package me.n1ar4.jar.analyzer.gui.runtime.model;

public record BuildSnapshotDto(
        BuildSettingsDto settings,
        String engineStatus,
        int buildProgress,
        String totalJar,
        String totalClass,
        String totalMethod,
        String totalEdge,
        String databaseSize,
        String statusText,
        String activeProjectKey,
        String activeProjectAlias,
        long activeBuildSeq
) {
}
