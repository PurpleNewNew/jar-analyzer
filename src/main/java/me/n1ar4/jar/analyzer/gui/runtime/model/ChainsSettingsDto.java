package me.n1ar4.jar.analyzer.gui.runtime.model;

public record ChainsSettingsDto(
        boolean sinkSelected,
        boolean sourceSelected,
        String sinkClass,
        String sinkMethod,
        String sinkDesc,
        String sourceClass,
        String sourceMethod,
        String sourceDesc,
        boolean sourceNull,
        boolean sourceEnabled,
        int maxDepth,
        boolean onlyFromWeb,
        boolean taintEnabled,
        String blacklist,
        String minEdgeConfidence,
        boolean showEdgeMeta,
        boolean summaryEnabled,
        Integer taintSeedParam,
        boolean taintSeedStrict,
        int maxResultLimit
) {
}
