package me.n1ar4.jar.analyzer.gui.runtime.api;

import me.n1ar4.jar.analyzer.gui.runtime.model.TreeNodeDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.TreeNodePayload;

import java.util.List;

public interface ProjectTreeFacade {
    java.util.List<TreeNodeDto> snapshot();

    java.util.List<TreeNodeDto> search(String keyword);

    void openNode(String value);

    default void openArtifactNode(TreeNodePayload payload) {
        if (payload == null) {
            return;
        }
        openNode("");
    }

    void refresh();
}
