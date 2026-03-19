package me.n1ar4.jar.analyzer.gui.runtime.model;

import java.util.List;

public sealed interface ToolingWindowPayload
        permits ToolingWindowPayload.EmptyPayload,
        ToolingWindowPayload.EditorContentPayload,
        ToolingWindowPayload.MarkdownPayload,
        ToolingWindowPayload.PathPayload,
        ToolingWindowPayload.TextPayload,
        ToolingWindowPayload.FlowResultPayload {

    record EmptyPayload() implements ToolingWindowPayload {
    }

    record MarkdownPayload(String title, String markdownResource) implements ToolingWindowPayload {
    }

    record PathPayload(String value) implements ToolingWindowPayload {
    }

    record EditorContentPayload(
            String tabKey,
            String title,
            String statusText,
            String content,
            String filePath,
            boolean image
    ) implements ToolingWindowPayload {
    }

    record TextPayload(String title, String content) implements ToolingWindowPayload {
    }

    record FlowResultPayload(
            boolean taintView,
            String title,
            String emptyHint,
            List<FlowResultItemDto> items
    ) implements ToolingWindowPayload {
    }
}
