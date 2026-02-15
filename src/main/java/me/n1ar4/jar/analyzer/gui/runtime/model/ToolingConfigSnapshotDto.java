package me.n1ar4.jar.analyzer.gui.runtime.model;

public record ToolingConfigSnapshotDto(
        boolean showInnerClass,
        boolean fixClassPath,
        boolean sortByMethod,
        boolean sortByClass,
        boolean logAllSql,
        boolean groupTreeByJar,
        boolean mergePackageRoot,
        boolean fixMethodImpl,
        boolean quickMode,
        String language,
        String theme
) {
}
