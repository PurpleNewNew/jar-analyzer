/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.graph.flow.model;

public enum FlowPathMode {
    FROM_SOURCE_TO_SINK(1),
    FROM_SINK_TO_SOURCE(2),
    FROM_SOURCE_TO_ALL(3);

    private final int code;

    FlowPathMode(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    public static FlowPathMode fromCode(int code) {
        return switch (code) {
            case 2 -> FROM_SINK_TO_SOURCE;
            case 3 -> FROM_SOURCE_TO_ALL;
            default -> FROM_SOURCE_TO_SINK;
        };
    }
}
