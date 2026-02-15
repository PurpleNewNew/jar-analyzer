package me.n1ar4.jar.analyzer.gui.runtime.model;

import java.util.List;

public record GadgetSnapshotDto(
        GadgetSettingsDto settings,
        java.util.List<GadgetRowDto> rows
) {
}
