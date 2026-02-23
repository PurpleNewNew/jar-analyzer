package me.n1ar4.jar.analyzer.gui.runtime.model;

public record MethodNavDto(
        String className,
        String methodName,
        String methodDesc,
        String jarName,
        int jarId,
        String artifactRole,
        boolean indexed,
        boolean resolvable,
        String displayHint
) {
    public MethodNavDto(String className,
                        String methodName,
                        String methodDesc,
                        String jarName,
                        int jarId) {
        this(className, methodName, methodDesc, jarName, jarId, "", true, true, "");
    }

    public MethodNavDto {
        artifactRole = artifactRole == null ? "" : artifactRole.trim();
        displayHint = displayHint == null ? "" : displayHint.trim();
    }
}
