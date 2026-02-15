package me.n1ar4.jar.analyzer.gui.runtime.model;

import java.util.List;

public sealed interface ToolingWindowPayload
        permits ToolingWindowPayload.EmptyPayload,
        ToolingWindowPayload.FramePayload,
        ToolingWindowPayload.MarkdownPayload,
        ToolingWindowPayload.PathPayload,
        ToolingWindowPayload.TextPayload,
        ToolingWindowPayload.ChainsResultPayload {

    record EmptyPayload() implements ToolingWindowPayload {
    }

    record FramePayload(boolean full) implements ToolingWindowPayload {
    }

    record MarkdownPayload(String title, String markdownResource) implements ToolingWindowPayload {
    }

    record PathPayload(String value) implements ToolingWindowPayload {
    }

    record TextPayload(String title, String content) implements ToolingWindowPayload {
    }

    record ChainsResultPayload(
            boolean taintView,
            String title,
            String emptyHint,
            List<ChainsResultItemDto> items
    ) implements ToolingWindowPayload {
    }
}
