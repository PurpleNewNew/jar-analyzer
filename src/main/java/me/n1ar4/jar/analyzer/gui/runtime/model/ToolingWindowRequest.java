package me.n1ar4.jar.analyzer.gui.runtime.model;

import java.util.Objects;

public record ToolingWindowRequest(
        ToolingWindowAction action,
        ToolingWindowPayload payload
) {
    public ToolingWindowRequest {
        action = Objects.requireNonNull(action, "action");
        if (payload == null) {
            payload = new ToolingWindowPayload.EmptyPayload();
        }
    }

    public static ToolingWindowRequest of(ToolingWindowAction action) {
        return new ToolingWindowRequest(action, new ToolingWindowPayload.EmptyPayload());
    }
}
