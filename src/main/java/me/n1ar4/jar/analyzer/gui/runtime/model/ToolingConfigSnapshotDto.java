package me.n1ar4.jar.analyzer.gui.runtime.model;

public record ToolingConfigSnapshotDto(
        boolean showInnerClass,
        boolean fixClassPath,
        boolean sortByMethod,
        boolean sortByClass,
        boolean groupTreeByJar,
        boolean mergePackageRoot,
        String language,
        String theme,
        boolean stripeShowNames,
        int stripeWidth
) {
}
