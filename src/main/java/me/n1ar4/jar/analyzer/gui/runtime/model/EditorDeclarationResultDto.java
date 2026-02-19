package me.n1ar4.jar.analyzer.gui.runtime.model;

import java.util.List;

public record EditorDeclarationResultDto(
        String token,
        String symbolKind,
        List<EditorDeclarationTargetDto> targets,
        String statusText
) {
    public EditorDeclarationResultDto {
        token = safe(token);
        symbolKind = safe(symbolKind);
        targets = targets == null ? List.of() : List.copyOf(targets);
        statusText = safe(statusText);
    }

    public static EditorDeclarationResultDto empty(String statusText) {
        return new EditorDeclarationResultDto("", "", List.of(), statusText);
    }

    public boolean hasTargets() {
        return targets != null && !targets.isEmpty();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
