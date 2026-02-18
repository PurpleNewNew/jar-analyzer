package me.n1ar4.jar.analyzer.gui.runtime.model;

public record StructureItemDto(
        String kind,
        int level,
        String label,
        String className,
        String methodName,
        String methodDesc,
        Integer jarId,
        String jarName,
        boolean navigable
) {
    public StructureItemDto {
        kind = safe(kind);
        label = safe(label);
        className = safe(className);
        methodName = safe(methodName);
        methodDesc = safe(methodDesc);
        jarName = safe(jarName);
        level = Math.max(0, level);
        if (jarId != null && jarId <= 0) {
            jarId = null;
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}

