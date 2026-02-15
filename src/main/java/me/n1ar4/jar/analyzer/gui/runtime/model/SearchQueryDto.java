package me.n1ar4.jar.analyzer.gui.runtime.model;

public record SearchQueryDto(
        SearchMode mode,
        SearchMatchMode matchMode,
        String className,
        String methodName,
        String keyword,
        boolean nullParamFilter
) {
}
