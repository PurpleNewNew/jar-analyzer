package me.n1ar4.jar.analyzer.gui.runtime.model;

public record FlowSnapshotDto(
        FlowSettingsDto settings,
        int dfsCount,
        int taintCount,
        String statusText
) {
    public FlowSnapshotDto(FlowSettingsDto settings, int dfsCount, int taintCount) {
        this(settings, dfsCount, taintCount, "");
    }
}
