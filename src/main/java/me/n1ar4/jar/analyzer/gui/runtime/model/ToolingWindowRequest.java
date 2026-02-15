package me.n1ar4.jar.analyzer.gui.runtime.model;

public record ToolingWindowRequest(
        ToolingWindowAction action,
        ToolingWindowPayload payload
) {
    public ToolingWindowRequest {
        if (action == null) {
            action = ToolingWindowAction.TEXT_VIEWER;
        }
        if (payload == null) {
            payload = new ToolingWindowPayload.EmptyPayload();
        }
    }

    public static ToolingWindowRequest of(ToolingWindowAction action) {
        return new ToolingWindowRequest(action, new ToolingWindowPayload.EmptyPayload());
    }
}
