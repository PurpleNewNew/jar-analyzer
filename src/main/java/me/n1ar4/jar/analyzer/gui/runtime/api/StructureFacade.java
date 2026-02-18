package me.n1ar4.jar.analyzer.gui.runtime.api;

import me.n1ar4.jar.analyzer.gui.runtime.model.StructureSnapshotDto;

public interface StructureFacade {
    StructureSnapshotDto snapshot(String className, Integer jarId);
}

