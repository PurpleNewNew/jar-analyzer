package me.n1ar4.jar.analyzer.gui.runtime.model;

public record ChainsSnapshotDto(
        ChainsSettingsDto settings,
        int dfsCount,
        int taintCount
) {
}
