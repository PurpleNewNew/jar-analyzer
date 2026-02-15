package me.n1ar4.jar.analyzer.gui.runtime.model;

public record LeakItemDto(
        String className,
        String typeName,
        String value,
        String jarName,
        int jarId
) {
}
