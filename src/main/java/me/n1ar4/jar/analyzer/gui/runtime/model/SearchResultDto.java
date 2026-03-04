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
        String navigateValue,
        int lineNumber
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
        lineNumber = Math.max(0, lineNumber);
    }

    public SearchResultDto(String className,
                           String methodName,
                           String methodDesc,
                           String jarName,
                           int jarId,
                           String preview,
                           String contributor,
                           String origin,
                           String navigateValue) {
        this(className, methodName, methodDesc, jarName, jarId,
                preview, contributor, origin, navigateValue, 0);
    }

    public SearchResultDto(String className,
                           String methodName,
                           String methodDesc,
                           String jarName,
                           int jarId,
                           String preview) {
        this(className, methodName, methodDesc, jarName, jarId, preview, "method", "unknown", "", 0);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
