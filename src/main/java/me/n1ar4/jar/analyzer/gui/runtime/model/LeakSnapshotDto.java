package me.n1ar4.jar.analyzer.gui.runtime.model;

import java.util.List;

public record LeakSnapshotDto(
        LeakRulesDto rules,
        java.util.List<LeakItemDto> results,
        String logTail
) {
}
