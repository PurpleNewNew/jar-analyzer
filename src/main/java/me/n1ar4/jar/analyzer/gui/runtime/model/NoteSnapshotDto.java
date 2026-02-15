package me.n1ar4.jar.analyzer.gui.runtime.model;

import java.util.List;

public record NoteSnapshotDto(
        List<MethodNavDto> history,
        List<MethodNavDto> favorites
) {
}
