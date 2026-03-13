package me.n1ar4.jar.analyzer.gui.runtime.api;

import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.engine.CoreEngine;
import me.n1ar4.jar.analyzer.engine.project.ProjectOrigin;
import me.n1ar4.jar.analyzer.core.facts.ClassFileEntity;
import me.n1ar4.jar.analyzer.core.facts.JarEntity;
import me.n1ar4.jar.analyzer.engine.model.MethodView;
import me.n1ar4.jar.analyzer.core.facts.ResourceEntity;
import me.n1ar4.jar.analyzer.graph.query.QueryOptions;
import me.n1ar4.jar.analyzer.graph.query.QueryResult;
import me.n1ar4.jar.analyzer.graph.query.QueryServices;
import me.n1ar4.jar.analyzer.gui.runtime.model.CallGraphScope;
import me.n1ar4.jar.analyzer.gui.runtime.model.NavigationTargetDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.SearchMatchMode;
import me.n1ar4.jar.analyzer.gui.runtime.model.SearchMode;
import me.n1ar4.jar.analyzer.gui.runtime.model.SearchQueryDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.SearchResultDto;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

final class SearchWorkflowSupport {
    private static final Logger logger = LogManager.getLogger();

    private final BiFunction<String, String, String> translator;

    SearchWorkflowSupport(BiFunction<String, String, String> translator) {
        this.translator = translator == null ? (zh, en) -> safe(zh) : translator;
    }

    SearchRunResult runContributorSearch(CoreEngine engine,
                                         SearchQueryDto query,
                                         CallGraphScope scope,
                                         ProjectJarOriginResolver resolver) {
        SearchMode mode = query.mode();
        boolean hasContributor = query.contributorClass()
                || query.contributorMethod()
                || query.contributorString()
                || query.contributorResource()
                || query.contributorCypher();
        if (mode == SearchMode.GLOBAL_CONTRIBUTOR && !hasContributor) {
            return new SearchRunResult(List.of(), tr("至少启用一个 contributor", "at least one contributor is required"));
        }
        String classFilter = normalizeClass(query.className());
        String methodFilter = safe(query.methodName()).trim();
        String keyword = safe(query.keyword()).trim();
        if (mode == SearchMode.METHOD_CALL || mode == SearchMode.METHOD_DEFINITION) {
            String methodTerm = methodFilter.isBlank() ? keyword : methodFilter;
            if (methodTerm.isBlank()) {
                return new SearchRunResult(List.of(), tr("方法名不能为空", "method name is required"));
            }
        } else if ((mode == SearchMode.STRING_CONTAINS || mode == SearchMode.BINARY_CONTAINS)
                && keyword.isBlank()) {
            return new SearchRunResult(List.of(), tr("关键字不能为空", "keyword is required"));
        } else if (mode == SearchMode.GLOBAL_CONTRIBUTOR
                && classFilter.isBlank()
                && methodFilter.isBlank()
                && keyword.isBlank()) {
            return new SearchRunResult(List.of(),
                    tr("需要关键字或类/方法过滤条件", "keyword or class/method filter is required"));
        }

        final int perContributorLimit = 300;
        Map<String, SearchResultDto> merged = new LinkedHashMap<>();

        if (mode == SearchMode.METHOD_CALL) {
            for (SearchResultDto item : searchCallerContributor(engine, classFilter, methodFilter, keyword,
                    query.matchMode(), query.nullParamFilter(), scope, resolver, perContributorLimit)) {
                merged.putIfAbsent(resultKey(item), item);
            }
        } else if (mode == SearchMode.METHOD_DEFINITION) {
            for (SearchResultDto item : searchMethodContributor(engine, classFilter, methodFilter, keyword,
                    query.matchMode(), query.nullParamFilter(), scope, resolver, perContributorLimit)) {
                merged.putIfAbsent(resultKey(item), item);
            }
        } else if (mode == SearchMode.STRING_CONTAINS) {
            for (SearchResultDto item : searchStringContributor(engine, classFilter, keyword, query.matchMode(),
                    query.nullParamFilter(), scope, resolver, perContributorLimit)) {
                merged.putIfAbsent(resultKey(item), item);
            }
        } else if (mode == SearchMode.BINARY_CONTAINS) {
            for (SearchResultDto item : searchBinaryContributor(engine, keyword, perContributorLimit)) {
                merged.putIfAbsent(resultKey(item), item);
            }
        } else if (query.contributorClass()) {
            String classTerm = classFilter.isBlank() ? keyword : classFilter;
            for (SearchResultDto item : searchClassContributor(classTerm, query.matchMode(),
                    perContributorLimit, scope, resolver)) {
                merged.putIfAbsent(resultKey(item), item);
            }
            if (query.contributorMethod()) {
                for (SearchResultDto item : searchMethodContributor(engine, classFilter, methodFilter, keyword,
                        query.matchMode(), query.nullParamFilter(), scope, resolver, perContributorLimit)) {
                    merged.putIfAbsent(resultKey(item), item);
                }
            }
            if (query.contributorString()) {
                for (SearchResultDto item : searchStringContributor(engine, classFilter, keyword, query.matchMode(),
                        query.nullParamFilter(), scope, resolver, perContributorLimit)) {
                    merged.putIfAbsent(resultKey(item), item);
                }
            }
            if (query.contributorResource()) {
                String resourceTerm = keyword.isBlank() ? classFilter : keyword;
                for (SearchResultDto item : searchResourceContributor(engine, resourceTerm, scope, resolver, perContributorLimit)) {
                    merged.putIfAbsent(resultKey(item), item);
                }
            }
            if (query.contributorCypher()) {
                CypherContributorResult cypher = searchCypherContributor(keyword, query.matchMode(),
                        perContributorLimit, scope, resolver);
                for (SearchResultDto item : cypher.results()) {
                    merged.putIfAbsent(resultKey(item), item);
                }
            }
        }

        List<SearchResultDto> out = new ArrayList<>(merged.values());
        String status = tr("结果数: ", "results: ") + out.size() + tr(" (contributors)", " (contributors)");
        return new SearchRunResult(out, status);
    }

    SearchRunResult runQueryLanguageSearch(SearchQueryDto query,
                                           CallGraphScope scope,
                                           ProjectJarOriginResolver resolver) {
        String script = safe(query.keyword()).trim();
        if (script.isBlank()) {
            return new SearchRunResult(List.of(), tr("Cypher 语句不能为空", "cypher query is required"));
        }
        QueryResult queryResult;
        try {
            queryResult = QueryServices.cypher().execute(script, Map.of(), QueryOptions.defaults());
        } catch (Exception ex) {
            String msg = safe(ex.getMessage());
            if (msg.isBlank()) {
                msg = ex.toString();
            }
            return new SearchRunResult(List.of(), tr("Cypher 异常: ", "cypher error: ") + msg);
        }
        List<SearchResultDto> out = mapQueryResult(queryResult, "cypher", scope, resolver);
        String status = tr("结果数: ", "results: ") + out.size();
        if (queryResult.isTruncated()) {
            status = status + tr("（已截断）", " (truncated)");
        }
        return new SearchRunResult(out, status);
    }

    private List<SearchResultDto> mapMethodViews(List<MethodView> methods,
                                                   String contributor,
                                                   boolean nullParamFilter,
                                                   CallGraphScope scope,
                                                   ProjectJarOriginResolver resolver) {
        List<SearchResultDto> out = new ArrayList<>();
        if (methods == null || methods.isEmpty()) {
            return out;
        }
        for (MethodView method : methods) {
            if (method == null) {
                continue;
            }
            if (nullParamFilter && safe(method.getMethodDesc()).contains("()")) {
                continue;
            }
            int jarId = method.getJarId();
            if (!acceptScope(scope, resolver, jarId)) {
                continue;
            }
            String origin = resolveOrigin(resolver, jarId);
            out.add(toSearchResult(method, contributor, origin));
        }
        return out;
    }

    private List<SearchResultDto> searchClassContributor(String term,
                                                         SearchMatchMode matchMode,
                                                         int limit,
                                                         CallGraphScope scope,
                                                         ProjectJarOriginResolver resolver) {
        List<SearchResultDto> out = new ArrayList<>();
        String keyword = safe(term).trim();
        if (keyword.isBlank()) {
            return out;
        }
        String normalized = normalizeClass(keyword);
        if (normalized.isBlank()) {
            return out;
        }
        int max = Math.max(1, limit);
        Map<Integer, String> jarNames = new HashMap<>();
        for (JarEntity row : DatabaseManager.getJarsMeta()) {
            if (row == null) {
                continue;
            }
            jarNames.put(row.getJid(), safe(row.getJarName()));
        }

        LinkedHashMap<String, SearchResultDto> dedup = new LinkedHashMap<>();
        for (ClassReference ref : DatabaseManager.getClassReferences()) {
            if (ref == null) {
                continue;
            }
            String className = normalizeClass(ref.getName());
            if (className.isBlank()) {
                continue;
            }
            boolean matched = matchMode == SearchMatchMode.EQUALS
                    ? className.equals(normalized)
                    : className.contains(normalized);
            if (!matched) {
                continue;
            }
            int jarId = ref.getJarId() == null ? -1 : ref.getJarId();
            if (!acceptScope(scope, resolver, jarId)) {
                continue;
            }
            String jarName = safe(ref.getJarName());
            if (jarName.isBlank()) {
                jarName = safe(jarNames.get(jarId));
            }
            String origin = resolveOrigin(resolver, jarId);
            String rowKey = className + "|" + jarId;
            dedup.putIfAbsent(rowKey, new SearchResultDto(
                    className,
                    "",
                    "",
                    jarName,
                    jarId,
                    className + (jarName.isBlank() ? "" : " [" + jarName + "]"),
                    "class",
                    origin,
                    NavigationTargetDto.classTarget(className, jarId)
            ));
            if (dedup.size() >= max) {
                break;
            }
        }
        if (dedup.size() < max) {
            for (ClassFileEntity row : DatabaseManager.getClassFiles()) {
                if (row == null) {
                    continue;
                }
                String className = normalizeClass(row.getClassName());
                if (className.isBlank()) {
                    continue;
                }
                boolean matched = matchMode == SearchMatchMode.EQUALS
                        ? className.equals(normalized)
                        : className.contains(normalized);
                if (!matched) {
                    continue;
                }
                int jarId = row.getJarId() == null ? -1 : row.getJarId();
                if (!acceptScope(scope, resolver, jarId)) {
                    continue;
                }
                String jarName = safe(row.getJarName());
                if (jarName.isBlank()) {
                    jarName = safe(jarNames.get(jarId));
                }
                String rowKey = className + "|" + jarId;
                if (dedup.containsKey(rowKey)) {
                    continue;
                }
                String origin = resolveOrigin(resolver, jarId);
                dedup.put(rowKey, new SearchResultDto(
                        className,
                        "",
                        "",
                        jarName,
                        jarId,
                        className + (jarName.isBlank() ? "" : " [" + jarName + "]"),
                        "class",
                        origin,
                        NavigationTargetDto.classTarget(className, jarId)
                ));
                if (dedup.size() >= max) {
                    break;
                }
            }
        }
        out.addAll(dedup.values());
        return out;
    }

    private List<SearchResultDto> searchMethodContributor(CoreEngine engine,
                                                          String classFilter,
                                                          String methodFilter,
                                                          String keyword,
                                                          SearchMatchMode matchMode,
                                                          boolean nullParamFilter,
                                                          CallGraphScope scope,
                                                          ProjectJarOriginResolver resolver,
                                                          int limit) {
        String methodTerm = safe(methodFilter).trim();
        if (methodTerm.isBlank()) {
            methodTerm = safe(keyword).trim();
        }
        if (methodTerm.isBlank()) {
            return List.of();
        }
        String ownerClass = classFilter.isBlank() ? null : classFilter;
        List<MethodView> methods = matchMode == SearchMatchMode.EQUALS
                ? engine.getMethod(ownerClass, methodTerm, null)
                : engine.getMethodLike(ownerClass, methodTerm, null);
        return mapMethodViews(trimLimit(methods, limit), "method", nullParamFilter, scope, resolver);
    }

    private List<SearchResultDto> searchCallerContributor(CoreEngine engine,
                                                          String classFilter,
                                                          String methodFilter,
                                                          String keyword,
                                                          SearchMatchMode matchMode,
                                                          boolean nullParamFilter,
                                                          CallGraphScope scope,
                                                          ProjectJarOriginResolver resolver,
                                                          int limit) {
        String methodTerm = safe(methodFilter).trim();
        if (methodTerm.isBlank()) {
            methodTerm = safe(keyword).trim();
        }
        if (methodTerm.isBlank()) {
            return List.of();
        }
        String ownerClass = classFilter.isBlank() ? null : classFilter;
        List<MethodView> methods = matchMode == SearchMatchMode.EQUALS
                ? engine.getCallers(ownerClass, methodTerm, null, null)
                : engine.getCallersLike(ownerClass, methodTerm, null);
        return mapMethodViews(trimLimit(methods, limit), "caller", nullParamFilter, scope, resolver);
    }

    private List<SearchResultDto> searchStringContributor(CoreEngine engine,
                                                          String classFilter,
                                                          String keyword,
                                                          SearchMatchMode matchMode,
                                                          boolean nullParamFilter,
                                                          CallGraphScope scope,
                                                          ProjectJarOriginResolver resolver,
                                                          int limit) {
        String term = safe(keyword).trim();
        if (term.isBlank()) {
            return List.of();
        }
        List<MethodView> methods = matchMode == SearchMatchMode.EQUALS
                ? engine.getMethodsByStrEqual(term)
                : engine.getMethodsByStr(term);
        if (!classFilter.isBlank()) {
            List<MethodView> filtered = new ArrayList<>();
            for (MethodView method : methods) {
                if (method == null) {
                    continue;
                }
                String className = normalizeClass(method.getClassName());
                if (className.contains(classFilter)) {
                    filtered.add(method);
                }
            }
            methods = filtered;
        }
        return mapMethodViews(trimLimit(methods, limit), "string", nullParamFilter, scope, resolver);
    }

    private List<SearchResultDto> searchResourceContributor(CoreEngine engine,
                                                            String keyword,
                                                            CallGraphScope scope,
                                                            ProjectJarOriginResolver resolver,
                                                            int limit) {
        String term = safe(keyword).trim();
        if (term.isBlank()) {
            return List.of();
        }
        List<ResourceEntity> rows = engine.getResources(term, null, 0, Math.max(1, limit));
        List<SearchResultDto> out = new ArrayList<>();
        for (ResourceEntity row : rows) {
            if (row == null) {
                continue;
            }
            int jarId = row.getJarId() == null ? 0 : row.getJarId();
            if (!acceptScope(scope, resolver, jarId)) {
                continue;
            }
            String origin = resolveOrigin(resolver, jarId);
            String resourcePath = safe(row.getResourcePath());
            String preview = resourcePath + " (" + row.getFileSize() + " bytes)";
            out.add(new SearchResultDto(
                    resourcePath,
                    "",
                    "",
                    safe(row.getJarName()),
                    jarId,
                    preview,
                    "resource",
                    origin,
                    NavigationTargetDto.resourceTarget(row.getRid())
            ));
        }
        return out;
    }

    private List<SearchResultDto> searchBinaryContributor(CoreEngine engine,
                                                          String keyword,
                                                          int limit) {
        List<SearchResultDto> out = scanBinary(engine.getJarsPath(), safe(keyword).trim());
        return trimLimit(out, limit);
    }

    private CypherContributorResult searchCypherContributor(String keyword,
                                                            SearchMatchMode matchMode,
                                                            int limit,
                                                            CallGraphScope scope,
                                                            ProjectJarOriginResolver resolver) {
        String term = safe(keyword).trim();
        if (term.isBlank()) {
            return CypherContributorResult.empty();
        }
        try {
            QueryResult result = QueryServices.cypher().execute(
                    buildCypherKeywordQuery(term, matchMode, limit),
                    Map.of(),
                    QueryOptions.defaults()
            );
            List<SearchResultDto> mapped = mapQueryResult(result, "cypher", scope, resolver);
            return CypherContributorResult.of(trimLimit(mapped, limit));
        } catch (Exception ex) {
            logger.debug("search cypher contributor fail: {}", ex.toString());
            return CypherContributorResult.of(List.of());
        }
    }

    private String buildCypherKeywordQuery(String term, SearchMatchMode matchMode, int limit) {
        String escaped = safe(term).replace("\\", "\\\\").replace("'", "\\'");
        String predicate;
        if (matchMode == SearchMatchMode.EQUALS) {
            predicate = "n.class_name = '" + escaped + "' OR n.method_name = '" + escaped + "'";
        } else {
            predicate = "n.class_name CONTAINS '" + escaped + "' OR n.method_name CONTAINS '" + escaped + "'";
        }
        return "MATCH (n) WHERE " + predicate +
                " RETURN n.class_name AS class_name, n.method_name AS method_name, " +
                "n.method_desc AS method_desc, n.jar_id AS jar_id, n.kind AS kind LIMIT " + Math.max(1, limit);
    }

    private List<SearchResultDto> mapQueryResult(QueryResult queryResult,
                                                 String contributor,
                                                 CallGraphScope scope,
                                                 ProjectJarOriginResolver resolver) {
        if (queryResult == null || queryResult.getRows() == null || queryResult.getRows().isEmpty()) {
            return List.of();
        }
        List<String> columns = normalizeColumns(queryResult);
        Map<String, Integer> indexes = new HashMap<>();
        for (int i = 0; i < columns.size(); i++) {
            indexes.put(safe(columns.get(i)).trim().toLowerCase(Locale.ROOT), i);
        }
        int classIndex = firstIndex(indexes, "class_name", "class");
        int methodIndex = firstIndex(indexes, "method_name", "method");
        int descIndex = firstIndex(indexes, "method_desc", "desc", "descriptor");
        int jarIdIndex = firstIndex(indexes, "jar_id", "jid");
        int jarNameIndex = firstIndex(indexes, "jar_name");
        int lineIndex = firstIndex(indexes, "line_number", "line", "lineno", "ln");
        int resourceIdIndex = firstIndex(indexes, "rid", "resource_id");
        int resourcePathIndex = firstIndex(indexes, "resource_path", "path");

        List<SearchResultDto> out = new ArrayList<>();
        int rowNo = 0;
        for (List<Object> row : queryResult.getRows()) {
            rowNo++;
            String className = valueAt(row, classIndex);
            String methodName = valueAt(row, methodIndex);
            String methodDesc = valueAt(row, descIndex);
            int jarId = intValueAt(row, jarIdIndex);
            String jarName = valueAt(row, jarNameIndex);
            int lineNumber = intValueAt(row, lineIndex);
            String resourcePath = valueAt(row, resourcePathIndex);
            int resourceId = intValueAt(row, resourceIdIndex);

            if (!acceptScope(scope, resolver, jarId)) {
                continue;
            }
            String origin = resolveOrigin(resolver, jarId);
            NavigationTargetDto navigationTarget = NavigationTargetDto.none();
            if (resourceId > 0) {
                navigationTarget = NavigationTargetDto.resourceTarget(resourceId);
            } else if (!resourcePath.isBlank()) {
                navigationTarget = NavigationTargetDto.filePathTarget(resourcePath);
            } else if (!className.isBlank()) {
                navigationTarget = NavigationTargetDto.classTarget(normalizeClass(className), jarId);
            }
            if (className.isBlank()) {
                className = contributor + " row " + rowNo;
            }
            String preview = buildRowPreview(columns, row);
            out.add(new SearchResultDto(
                    className,
                    methodName,
                    methodDesc,
                    jarName,
                    jarId,
                    preview,
                    contributor,
                    origin,
                    navigationTarget,
                    lineNumber
            ));
        }
        return out;
    }

    private List<String> normalizeColumns(QueryResult queryResult) {
        List<String> columns = queryResult.getColumns();
        if (columns != null && !columns.isEmpty()) {
            return columns;
        }
        List<List<Object>> rows = queryResult.getRows();
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        List<Object> row = rows.get(0);
        if (row == null || row.isEmpty()) {
            return List.of();
        }
        List<String> generated = new ArrayList<>(row.size());
        for (int i = 0; i < row.size(); i++) {
            generated.add("col" + (i + 1));
        }
        return generated;
    }

    private String buildRowPreview(List<String> columns, List<Object> row) {
        if (columns == null || columns.isEmpty() || row == null || row.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        int size = Math.min(columns.size(), row.size());
        for (int i = 0; i < size; i++) {
            if (i > 0) {
                sb.append(" | ");
            }
            sb.append(safe(columns.get(i))).append('=').append(safe(String.valueOf(row.get(i))));
        }
        return sb.toString();
    }

    private int firstIndex(Map<String, Integer> indexes, String... names) {
        if (indexes == null || indexes.isEmpty() || names == null) {
            return -1;
        }
        for (String name : names) {
            if (name == null) {
                continue;
            }
            Integer index = indexes.get(name.toLowerCase(Locale.ROOT));
            if (index != null) {
                return index;
            }
        }
        return -1;
    }

    private String valueAt(List<Object> row, int index) {
        if (row == null || index < 0 || index >= row.size()) {
            return "";
        }
        Object val = row.get(index);
        return val == null ? "" : String.valueOf(val);
    }

    private int intValueAt(List<Object> row, int index) {
        if (row == null || index < 0 || index >= row.size()) {
            return 0;
        }
        Object val = row.get(index);
        if (val == null) {
            return 0;
        }
        if (val instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(val).trim());
        } catch (Exception ignored) {
            return 0;
        }
    }

    private boolean acceptScope(CallGraphScope scope, ProjectJarOriginResolver resolver, int jarId) {
        if (scope == null || scope == CallGraphScope.ALL) {
            return true;
        }
        ProjectOrigin origin = resolver.resolve(jarId);
        return switch (scope) {
            case APP -> origin == ProjectOrigin.APP;
            case LIBRARY -> origin == ProjectOrigin.LIBRARY;
            case SDK -> origin == ProjectOrigin.SDK;
            case GENERATED -> origin == ProjectOrigin.GENERATED;
            case EXCLUDED -> origin == ProjectOrigin.EXCLUDED;
            case ALL -> true;
        };
    }

    private String resolveOrigin(ProjectJarOriginResolver resolver, int jarId) {
        return resolver.resolve(jarId).value();
    }

    private <T> List<T> trimLimit(List<T> input, int limit) {
        if (input == null || input.isEmpty()) {
            return List.of();
        }
        if (limit <= 0 || input.size() <= limit) {
            return input;
        }
        return new ArrayList<>(input.subList(0, limit));
    }

    private String resultKey(SearchResultDto item) {
        return safe(item.contributor()) + "|" +
                safe(item.className()) + "|" +
                safe(item.methodName()) + "|" +
                safe(item.methodDesc()) + "|" +
                item.jarId() + "|" +
                item.lineNumber() + "|" +
                safe(item.navigationTarget().encode()) + "|" +
                safe(item.preview());
    }

    private List<SearchResultDto> scanBinary(List<String> jars, String keyword) {
        List<SearchResultDto> out = new ArrayList<>();
        if (keyword == null || keyword.isEmpty() || jars == null || jars.isEmpty()) {
            return out;
        }
        byte[] pattern = keyword.getBytes(StandardCharsets.UTF_8);
        Set<String> hit = new LinkedHashSet<>();
        for (String path : jars) {
            if (path == null || path.isBlank()) {
                continue;
            }
            try {
                Path p = Paths.get(path);
                if (Files.notExists(p)) {
                    continue;
                }
                byte[] data = Files.readAllBytes(p);
                if (contains(data, pattern)) {
                    hit.add(path);
                }
            } catch (Exception ignored) {
                logger.debug("scan binary contributor failed: {}", ignored.toString());
            }
        }
        for (String path : hit) {
            out.add(new SearchResultDto(
                    path,
                    "",
                    "",
                    "",
                    0,
                    path,
                    "binary",
                    "unknown",
                    NavigationTargetDto.filePathTarget(path)
            ));
        }
        return out;
    }

    private boolean contains(byte[] data, byte[] target) {
        if (data == null || target == null || target.length == 0 || data.length < target.length) {
            return false;
        }
        for (int i = 0; i <= data.length - target.length; i++) {
            boolean ok = true;
            for (int j = 0; j < target.length; j++) {
                if (data[i + j] != target[j]) {
                    ok = false;
                    break;
                }
            }
            if (ok) {
                return true;
            }
        }
        return false;
    }

    private String normalizeClass(String className) {
        String value = safe(className).trim();
        if (value.isEmpty()) {
            return "";
        }
        return value.replace('.', '/');
    }

    private SearchResultDto toSearchResult(MethodView method, String contributor, String origin) {
        String preview = safe(method.getClassName()) + "#" + safe(method.getMethodName()) + safe(method.getMethodDesc());
        return new SearchResultDto(
                safe(method.getClassName()),
                safe(method.getMethodName()),
                safe(method.getMethodDesc()),
                safe(method.getJarName()),
                method.getJarId(),
                preview,
                safe(contributor),
                safe(origin),
                NavigationTargetDto.classTarget(normalizeClass(method.getClassName()), method.getJarId()),
                method.getLineNumber()
        );
    }

    private String tr(String zh, String en) {
        String value = translator.apply(zh, en);
        return value == null ? safe(zh) : value;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    record SearchRunResult(List<SearchResultDto> results, String statusText) {
    }

    private record CypherContributorResult(List<SearchResultDto> results) {
        private static CypherContributorResult empty() {
            return new CypherContributorResult(List.of());
        }

        private static CypherContributorResult of(List<SearchResultDto> results) {
            return new CypherContributorResult(results == null ? List.of() : results);
        }
    }
}
