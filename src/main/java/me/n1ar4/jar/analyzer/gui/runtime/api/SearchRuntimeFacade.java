package me.n1ar4.jar.analyzer.gui.runtime.api;

import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.core.ProjectStateUtil;
import me.n1ar4.jar.analyzer.engine.CoreEngine;
import me.n1ar4.jar.analyzer.gui.runtime.model.CallGraphScope;
import me.n1ar4.jar.analyzer.gui.runtime.model.SearchMatchMode;
import me.n1ar4.jar.analyzer.gui.runtime.model.SearchMode;
import me.n1ar4.jar.analyzer.gui.runtime.model.SearchQueryDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.SearchResultDto;
import me.n1ar4.jar.analyzer.gui.runtime.model.SearchSnapshotDto;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;

final class SearchRuntimeFacade implements SearchFacade {
    private static final Logger logger = LogManager.getLogger();

    private final SearchState state;
    private final SearchWorkflowSupport searchWorkflow;
    private final Supplier<CoreEngine> engineSupplier;
    private final BiFunction<String, String, String> translator;
    private volatile long scopeResolverVersion = -1L;
    private volatile ProjectJarOriginResolver scopeResolver = ProjectJarOriginResolver.empty();

    SearchRuntimeFacade(SearchState state,
                        SearchWorkflowSupport searchWorkflow,
                        Supplier<CoreEngine> engineSupplier,
                        BiFunction<String, String, String> translator) {
        this.state = state;
        this.searchWorkflow = searchWorkflow == null ? new SearchWorkflowSupport((zh, en) -> safe(zh)) : searchWorkflow;
        this.engineSupplier = engineSupplier == null ? () -> null : engineSupplier;
        this.translator = translator == null ? (zh, en) -> safe(zh) : translator;
    }

    @Override
    public SearchSnapshotDto snapshot() {
        return new SearchSnapshotDto(
                state.searchQuery(),
                immutableList(state.searchResults()),
                state.searchStatusText()
        );
    }

    @Override
    public void applyQuery(SearchQueryDto query) {
        if (query == null) {
            return;
        }
        state.setSearchQuery(query);
        state.setSearchStatusText(tr("搜索条件已更新", "search query updated"));
    }

    @Override
    public void runSearch() {
        if (!state.tryStartSearch()) {
            return;
        }
        Thread.ofVirtual().name("gui-runtime-search").start(() -> {
            try {
                doSearch();
            } finally {
                state.finishSearch();
            }
        });
    }

    @Override
    public void publishExternalResults(List<SearchResultDto> results, String statusText) {
        List<SearchResultDto> sorted = new ArrayList<>();
        if (results != null && !results.isEmpty()) {
            sorted.addAll(results);
            Comparator<SearchResultDto> comparator = Comparator
                    .comparing((SearchResultDto item) -> safe(item.contributor()));
            comparator = state.sortByMethod()
                    ? comparator.thenComparing(item -> safe(item.methodName()))
                    .thenComparing(item -> safe(item.className()))
                    : comparator.thenComparing(item -> safe(item.className()))
                    .thenComparing(item -> safe(item.methodName()));
            sorted.sort(comparator);
        }
        state.setSearchResults(immutableList(sorted));
        if (statusText == null || statusText.isBlank()) {
            state.setSearchStatusText(tr("结果数: ", "results: ") + sorted.size());
        } else {
            state.setSearchStatusText(statusText);
        }
    }

    private void doSearch() {
        CoreEngine engine = engineSupplier.get();
        if (engine == null || !engine.isEnabled()) {
            state.setSearchResults(List.of());
            state.setSearchStatusText(tr("引擎尚未就绪", "engine is not ready"));
            return;
        }
        SearchQueryDto query = state.searchQuery() == null
                ? new SearchQueryDto(SearchMode.METHOD_CALL, SearchMatchMode.LIKE, "", "", "", false)
                : state.searchQuery();
        CallGraphScope scope = CallGraphScope.fromValue(query.scope());
        ProjectJarOriginResolver resolver = scope == CallGraphScope.ALL
                ? ProjectJarOriginResolver.empty()
                : loadScopeResolver();

        SearchWorkflowSupport.SearchRunResult result;
        try {
            result = switch (query.mode()) {
                case GLOBAL_CONTRIBUTOR, METHOD_CALL, METHOD_DEFINITION, STRING_CONTAINS, BINARY_CONTAINS ->
                        searchWorkflow.runContributorSearch(engine, query, scope, resolver);
                case CYPHER_QUERY -> searchWorkflow.runQueryLanguageSearch(query, scope, resolver);
            };
        } catch (Throwable ex) {
            logger.error("runtime search failed: {}", ex.toString());
            result = new SearchWorkflowSupport.SearchRunResult(
                    List.of(),
                    tr("搜索异常: ", "search error: ") + safe(ex.getMessage())
            );
        }
        publishExternalResults(result.results(), result.statusText());
    }

    private ProjectJarOriginResolver loadScopeResolver() {
        long latestRuntimeVersion = ProjectStateUtil.runtimeSnapshot();
        if (latestRuntimeVersion <= 0) {
            return ProjectJarOriginResolver.empty();
        }
        ProjectJarOriginResolver cached = scopeResolver;
        if (latestRuntimeVersion == scopeResolverVersion && cached != null) {
            return cached;
        }
        synchronized (this) {
            if (latestRuntimeVersion == scopeResolverVersion && scopeResolver != null) {
                return scopeResolver;
            }
            ProjectJarOriginResolver reloaded = loadScopeResolverFromModel();
            scopeResolverVersion = latestRuntimeVersion;
            scopeResolver = reloaded;
            return reloaded;
        }
    }

    private ProjectJarOriginResolver loadScopeResolverFromModel() {
        try {
            return ProjectJarOriginResolver.fromProjectModel(
                    ProjectStateUtil.runtimeProjectModel(),
                    DatabaseManager.getJarsMeta());
        } catch (Exception ex) {
            logger.debug("load search scope resolver fail: {}", ex.toString());
            return ProjectJarOriginResolver.empty();
        }
    }

    private String tr(String zh, String en) {
        return safe(translator.apply(zh, en));
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static <T> List<T> immutableList(List<T> list) {
        if (list == null || list.isEmpty()) {
            return List.of();
        }
        return List.copyOf(list);
    }

    interface SearchState {
        SearchQueryDto searchQuery();

        void setSearchQuery(SearchQueryDto query);

        List<SearchResultDto> searchResults();

        void setSearchResults(List<SearchResultDto> results);

        String searchStatusText();

        void setSearchStatusText(String statusText);

        boolean sortByMethod();

        boolean tryStartSearch();

        void finishSearch();
    }
}
