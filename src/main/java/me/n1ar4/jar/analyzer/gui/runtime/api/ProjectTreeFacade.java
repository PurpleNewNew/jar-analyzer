package me.n1ar4.jar.analyzer.gui.runtime.api;

import me.n1ar4.jar.analyzer.gui.runtime.model.NavigationTargetDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.TreeNodeDto;

import java.util.List;

public interface ProjectTreeFacade {
    List<TreeNodeDto> snapshot();

    List<TreeNodeDto> search(String keyword);

    void openTarget(NavigationTargetDto target);

    void refresh();
}
