/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.gui.swing.cypher.model;

import java.util.List;

public record GraphFramePayload(
        List<Node> nodes,
        List<Edge> edges,
        List<String> warnings,
        boolean truncated
) {
    public record Node(
            long id,
            String label,
            String kind,
            int jarId,
            String className,
            String methodName,
            String methodDesc
    ) {
    }

    public record Edge(
            long id,
            long source,
            long target,
            String relType,
            String confidence,
            String evidence
    ) {
    }
}
