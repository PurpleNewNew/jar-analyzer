package me.n1ar4.jar.analyzer.gui.runtime.model;

public record SearchQueryDto(
        SearchMode mode,
        SearchMatchMode matchMode,
        String className,
        String methodName,
        String keyword,
        boolean nullParamFilter,
        String scope,
        boolean contributorClass,
        boolean contributorMethod,
        boolean contributorString,
        boolean contributorResource
) {
    public SearchQueryDto {
        mode = mode == null ? SearchMode.METHOD_CALL : mode;
        matchMode = matchMode == null ? SearchMatchMode.LIKE : matchMode;
        className = safe(className);
        methodName = safe(methodName);
        keyword = safe(keyword);
        scope = normalizeScope(scope);
    }

    public SearchQueryDto(SearchMode mode,
                          SearchMatchMode matchMode,
                          String className,
                          String methodName,
                          String keyword,
                          boolean nullParamFilter) {
        this(mode, matchMode, className, methodName, keyword, nullParamFilter,
                "all", true, true, true, true);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String normalizeScope(String raw) {
        String value = raw == null ? "" : raw.trim().toLowerCase();
        return switch (value) {
            case "all", "app", "library", "sdk", "generated", "excluded" -> value;
            default -> "app";
        };
    }
}
