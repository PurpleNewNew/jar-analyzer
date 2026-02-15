package me.n1ar4.jar.analyzer.gui.runtime.model;

import java.util.List;

public record WebSnapshotDto(
        String pathKeyword,
        List<ClassNavDto> controllers,
        List<MethodNavDto> mappings,
        List<ClassNavDto> interceptors,
        List<ClassNavDto> servlets,
        List<ClassNavDto> filters,
        List<ClassNavDto> listeners
) {
}
