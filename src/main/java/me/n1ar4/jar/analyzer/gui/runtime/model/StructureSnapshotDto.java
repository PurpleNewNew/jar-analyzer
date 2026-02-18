package me.n1ar4.jar.analyzer.gui.runtime.model;

import java.util.List;

public record StructureSnapshotDto(
        String className,
        Integer jarId,
        List<StructureItemDto> items,
        String statusText
) {
    public StructureSnapshotDto {
        className = safe(className);
        statusText = safe(statusText);
        if (jarId != null && jarId <= 0) {
            jarId = null;
        }
        if (items == null) {
            items = List.of();
        } else {
            items = List.copyOf(items);
        }
    }

    public static StructureSnapshotDto empty(String className, Integer jarId, String statusText) {
        return new StructureSnapshotDto(className, jarId, List.of(), statusText);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}

