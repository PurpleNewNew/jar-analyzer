package me.n1ar4.jar.analyzer.gui.runtime.model;

public record SearchResultDto(
        String className,
        String methodName,
        String methodDesc,
        String jarName,
        int jarId,
        String preview
) {
}
