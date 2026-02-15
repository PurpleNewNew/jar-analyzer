package me.n1ar4.jar.analyzer.gui.runtime.model;

import java.util.List;

public record CallGraphSnapshotDto(
        String currentJar,
        String currentClass,
        String currentMethod,
        List<MethodNavDto> allMethods,
        List<MethodNavDto> callers,
        List<MethodNavDto> callees,
        List<MethodNavDto> impls,
        List<MethodNavDto> superImpls
) {
}
