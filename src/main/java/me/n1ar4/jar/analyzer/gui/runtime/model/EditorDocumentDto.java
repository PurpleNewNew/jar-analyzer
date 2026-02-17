package me.n1ar4.jar.analyzer.gui.runtime.model;

public record EditorDocumentDto(
        String className,
        String jarName,
        Integer jarId,
        String methodName,
        String methodDesc,
        String content,
        int caretOffset,
        String statusText
) {
}
