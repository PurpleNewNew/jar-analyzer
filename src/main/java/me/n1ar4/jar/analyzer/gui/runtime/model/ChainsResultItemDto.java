package me.n1ar4.jar.analyzer.gui.runtime.model;

import java.util.List;

public record ChainsResultItemDto(
        int index,
        String sink,
        String source,
        int depth,
        int pathCount,
        int nodeCount,
        int edgeCount,
        long elapsedMs,
        boolean truncated,
        String truncateReason,
        String recommend,
        boolean taint,
        boolean taintPass,
        boolean lowConfidence,
        String taintDetail,
        String sanitizerDetail,
        List<MethodNavDto> methods
) {
}
