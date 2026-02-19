package me.n1ar4.jar.analyzer.gui.runtime.model;

public record EditorDeclarationTargetDto(
        String className,
        String methodName,
        String methodDesc,
        String jarName,
        int jarId,
        int caretOffset,
        String kind,
        String source
) {
    public EditorDeclarationTargetDto {
        className = safe(className);
        methodName = safe(methodName);
        methodDesc = safe(methodDesc);
        jarName = safe(jarName);
        jarId = Math.max(0, jarId);
        caretOffset = Math.max(-1, caretOffset);
        kind = safe(kind);
        source = safe(source);
    }

    public boolean methodTarget() {
        return !methodName.isBlank() && !methodDesc.isBlank();
    }

    public boolean localTarget() {
        return caretOffset >= 0 && className.isBlank();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
