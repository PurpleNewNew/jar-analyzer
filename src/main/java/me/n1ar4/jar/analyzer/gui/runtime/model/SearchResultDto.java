package me.n1ar4.jar.analyzer.gui.runtime.model;

public record SearchResultDto(
        String className,
        String methodName,
        String methodDesc,
        String jarName,
        int jarId,
        String preview,
        String contributor,
        String origin,
        String navigateValue
) {
    public SearchResultDto {
        className = safe(className);
        methodName = safe(methodName);
        methodDesc = safe(methodDesc);
        jarName = safe(jarName);
        preview = safe(preview);
        contributor = safe(contributor);
        origin = safe(origin);
        navigateValue = safe(navigateValue);
    }

    public SearchResultDto(String className,
                           String methodName,
                           String methodDesc,
                           String jarName,
                           int jarId,
                           String preview) {
        this(className, methodName, methodDesc, jarName, jarId, preview, "method", "unknown", "");
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
