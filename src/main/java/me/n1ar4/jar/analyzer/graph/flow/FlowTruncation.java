/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.graph.flow;

public record FlowTruncation(
        boolean truncated,
        String reason,
        String recommend
) {
    public static FlowTruncation none() {
        return new FlowTruncation(false, "", "");
    }

    public static FlowTruncation of(String reason, String recommend) {
        return new FlowTruncation(true, safe(reason), safe(recommend));
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
