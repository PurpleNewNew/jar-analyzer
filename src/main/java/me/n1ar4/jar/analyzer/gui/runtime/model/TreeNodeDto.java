package me.n1ar4.jar.analyzer.gui.runtime.model;

import java.util.List;

public record TreeNodeDto(
        String label,
        String value,
        boolean directory,
        java.util.List<TreeNodeDto> children,
        TreeNodePayload payload
) {
    public TreeNodeDto(String label, String value, boolean directory, List<TreeNodeDto> children) {
        this(label, value, directory, children, null);
    }
}
