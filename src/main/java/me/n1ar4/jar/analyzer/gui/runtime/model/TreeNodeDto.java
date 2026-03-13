package me.n1ar4.jar.analyzer.gui.runtime.model;

import java.util.List;

public record TreeNodeDto(
        String label,
        String nodeKey,
        boolean directory,
        NavigationTargetDto navigationTarget,
        java.util.List<TreeNodeDto> children
) {
    public TreeNodeDto {
        label = safe(label);
        nodeKey = safe(nodeKey);
        navigationTarget = navigationTarget == null ? NavigationTargetDto.none() : navigationTarget;
        children = children == null ? java.util.List.of() : java.util.List.copyOf(children);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
