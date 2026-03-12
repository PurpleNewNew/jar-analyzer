/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.graph.proc;

import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.graph.flow.model.FlowPath;
import me.n1ar4.jar.analyzer.graph.flow.FlowOptions;
import me.n1ar4.jar.analyzer.graph.flow.GraphGadgetEngine;
import me.n1ar4.jar.analyzer.graph.flow.GraphPrunedPathEngine;
import me.n1ar4.jar.analyzer.graph.flow.SearchDirection;
import me.n1ar4.jar.analyzer.graph.flow.GraphTaintEngine;
import me.n1ar4.jar.analyzer.graph.flow.TraversalMode;
import me.n1ar4.jar.analyzer.graph.query.QueryOptions;
import me.n1ar4.jar.analyzer.graph.query.QueryResult;
import me.n1ar4.jar.analyzer.graph.store.GraphEdge;
import me.n1ar4.jar.analyzer.graph.store.GraphNode;
import me.n1ar4.jar.analyzer.graph.store.GraphSnapshot;
import me.n1ar4.jar.analyzer.graph.store.GraphTraversalRules;
import me.n1ar4.jar.analyzer.rules.MethodSemanticFlags;
import me.n1ar4.jar.analyzer.taint.TaintResult;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class ProcedureRegistry {
    private static final int MAX_FAN_OUT = 96;
    private static final List<String> DEFAULT_COLUMNS = List.of(
            "path_id",
            "hop",
            "node_ids",
            "edge_ids",
            "score",
            "confidence",
            "evidence"
    );

    public QueryResult execute(String procName,
                               List<String> argExprs,
                               Map<String, Object> params,
                               QueryOptions options,
                               GraphSnapshot snapshot) {
        QueryOptions effectiveOptions = options == null ? QueryOptions.defaults() : options;
        String name = safe(procName).toLowerCase();
        if ("ja.path.shortest".equals(name)) {
            return shortest(argExprs, params, effectiveOptions, snapshot, false);
        }
        if ("ja.path.shortest_pruned".equals(name)) {
            return shortest(argExprs, params, effectiveOptions, snapshot, true);
        }
        if ("ja.path.from_to".equals(name)) {
            return fromTo(argExprs, params, effectiveOptions, snapshot, false);
        }
        if ("ja.path.from_to_pruned".equals(name)) {
            return fromTo(argExprs, params, effectiveOptions, snapshot, true);
        }
        if ("ja.path.gadget".equals(name)) {
            return pathGadget(argExprs, params, effectiveOptions, snapshot);
        }
        if ("ja.taint.track".equals(name)) {
            return taintTrack(argExprs, params, effectiveOptions, snapshot);
        }
        if ("ja.gadget.track".equals(name)) {
            return gadgetTrack(argExprs, params, effectiveOptions, snapshot);
        }
        throw new IllegalArgumentException("cypher_feature_not_supported");
    }

    private QueryResult shortest(List<String> argExprs,
                                 Map<String, Object> params,
                                 QueryOptions options,
                                 GraphSnapshot snapshot,
                                 boolean pruned) {
        long from = resolveNodeRef(resolveArg(argExprs, params, 0), snapshot);
        long to = resolveNodeRef(resolveArg(argExprs, params, 1), snapshot);
        int maxHops = toInt(resolveArg(argExprs, params, 2), options.getMaxHops());
        TraversalMode traversalMode = TraversalMode.parse(toString(resolveArg(argExprs, params, 3)));
        SearchDirection direction = SearchDirection.parse(
                toString(resolveArg(argExprs, params, 4)),
                pruned ? SearchDirection.FORWARD : SearchDirection.BIDIRECTIONAL
        );
        QueryBudget budget = QueryBudget.of(options, maxHops, 1, null);
        if (pruned) {
            PrunedRunSelection selection = selectPrunedShortest(snapshot, from, to, maxHops, traversalMode, direction, budget);
            ensureWithinBudget(budget);
            if (selection.candidate() == null) {
                return new QueryResult(
                        DEFAULT_COLUMNS,
                        List.of(),
                        buildPrunedWarnings(selection.stats(), "path_not_found", direction),
                        false
                );
            }
            GraphPrunedPathEngine.Candidate candidate = selection.candidate();
            List<Object> row = toRow(
                    1,
                    new Path(candidate.nodeIds(), edgeIds(candidate.edges())),
                    candidate.lowConfidence() ? "low" : "high",
                    prunedEvidence("ja.path.shortest_pruned", candidate, traversalMode, direction)
            );
            return new QueryResult(DEFAULT_COLUMNS, List.of(row), buildPrunedWarnings(selection.stats(), null, direction), false);
        }
        Path path = switch (direction) {
            case FORWARD -> singleDirectionShortest(snapshot, from, to, maxHops, traversalMode, budget, false);
            case BACKWARD -> singleDirectionShortest(snapshot, from, to, maxHops, traversalMode, budget, true);
            case BIDIRECTIONAL -> bidirectionalShortest(snapshot, from, to, maxHops, traversalMode, budget);
        };
        ensureWithinBudget(budget);
        if (path == null) {
            return new QueryResult(DEFAULT_COLUMNS, List.of(), List.of("path_not_found", "path_search_direction=" + direction.displayName()), false);
        }
        budget.onPath();
        return new QueryResult(
                DEFAULT_COLUMNS,
                List.of(toRow(1, path, "high", evidenceWithTraversalAndDirection("ja.path.shortest", traversalMode, direction))),
                List.of("path_search_direction=" + direction.displayName()),
                false
        );
    }

    private QueryResult fromTo(List<String> argExprs,
                               Map<String, Object> params,
                               QueryOptions options,
                               GraphSnapshot snapshot,
                               boolean pruned) {
        long from = resolveNodeRef(resolveArg(argExprs, params, 0), snapshot);
        long to = resolveNodeRef(resolveArg(argExprs, params, 1), snapshot);
        int maxHops = toInt(resolveArg(argExprs, params, 2), options.getMaxHops());
        int maxPaths = toInt(resolveArg(argExprs, params, 3), options.getMaxPaths());
        TraversalMode traversalMode = TraversalMode.parse(toString(resolveArg(argExprs, params, 4)));
        SearchDirection direction = SearchDirection.parse(
                toString(resolveArg(argExprs, params, 5)),
                SearchDirection.FORWARD
        );
        QueryBudget budget = QueryBudget.of(options, maxHops, maxPaths, null);

        if (pruned) {
            PrunedRunSelection selection = runPrunedPaths(snapshot, from, to, maxHops, maxPaths, traversalMode, direction, budget);
            ensureWithinBudget(budget);
            if (selection.candidates().isEmpty()) {
                return new QueryResult(
                        DEFAULT_COLUMNS,
                        List.of(),
                        buildPrunedWarnings(selection.stats(), "path_not_found", direction),
                        false
                );
            }
            List<List<Object>> rows = new ArrayList<>();
            int pathId = 1;
            for (GraphPrunedPathEngine.Candidate candidate : selection.candidates()) {
                rows.add(toRow(
                        pathId++,
                        new Path(candidate.nodeIds(), edgeIds(candidate.edges())),
                        candidate.lowConfidence() ? "low" : "high",
                        prunedEvidence("ja.path.from_to_pruned", candidate, traversalMode, direction)
                ));
            }
            boolean truncated = selection.truncated();
            return new QueryResult(DEFAULT_COLUMNS, rows, buildPrunedWarnings(selection.stats(), null, direction), truncated);
        }
        PathEnumeration enumeration = enumeratePaths(snapshot, from, to, maxHops, maxPaths, traversalMode, direction, budget);

        List<List<Object>> rows = new ArrayList<>();
        int pathId = 1;
        for (Path path : enumeration.paths()) {
            rows.add(toRow(pathId++, path, "medium", evidenceWithTraversalAndDirection("ja.path.from_to", traversalMode, direction)));
        }
        List<String> warnings = rows.isEmpty()
                ? List.of("path_not_found", "path_search_direction=" + direction.displayName())
                : List.of("path_search_direction=" + direction.displayName());
        return new QueryResult(DEFAULT_COLUMNS, rows, warnings, enumeration.truncated());
    }

    private QueryResult taintTrack(List<String> argExprs,
                                   Map<String, Object> params,
                                   QueryOptions options,
                                   GraphSnapshot snapshot) {
        String sourceClass = normalizeClass(toString(resolveArg(argExprs, params, 0)));
        String sourceMethod = toString(resolveArg(argExprs, params, 1));
        String sourceDesc = normalizeDesc(toString(resolveArg(argExprs, params, 2)));
        String sinkClass = normalizeClass(toString(resolveArg(argExprs, params, 3)));
        String sinkMethod = toString(resolveArg(argExprs, params, 4));
        String sinkDesc = normalizeDesc(toString(resolveArg(argExprs, params, 5)));
        int depth = toInt(resolveArg(argExprs, params, 6), options.getMaxHops());
        Integer timeoutMs = toNullableInt(resolveArg(argExprs, params, 7));
        Integer maxPaths = toNullableInt(resolveArg(argExprs, params, 8));
        String mode = toString(resolveArg(argExprs, params, 9));
        boolean searchAllSources = toBoolean(resolveArg(argExprs, params, 10), false);
        boolean onlyFromWeb = searchAllSources && toBoolean(resolveArg(argExprs, params, 11), false);
        TraversalMode traversalMode = TraversalMode.parse(toString(resolveArg(argExprs, params, 12)));
        SearchDirection direction = resolveTaintDirection(mode, toString(resolveArg(argExprs, params, 13)));

        if (sinkClass.isEmpty() || sinkMethod.isEmpty() || sinkDesc.isEmpty()) {
            throw new IllegalArgumentException("missing_param");
        }
        if (searchAllSources && direction != SearchDirection.BACKWARD) {
            throw new IllegalArgumentException("invalid_request");
        }
        if (!searchAllSources
                && (sourceClass.isEmpty() || sourceMethod.isEmpty() || sourceDesc.isEmpty())) {
            throw new IllegalArgumentException("missing_param");
        }

        QueryBudget budget = QueryBudget.of(options, depth, maxPaths == null ? 0 : maxPaths, timeoutMs);
        int effectiveTimeoutMs = Math.min(
                options.effectiveBudgetMaxMs(depth, timeoutMs),
                budget.remainingMsOrDefault(options.effectiveBudgetMaxMs(depth, timeoutMs))
        );
        if (timeoutMs != null && timeoutMs > 0) {
            effectiveTimeoutMs = Math.min(effectiveTimeoutMs, timeoutMs);
        }
        Integer effectiveMaxPaths = null;
        if (maxPaths != null && maxPaths > 0) {
            effectiveMaxPaths = maxPaths;
        }

        FlowOptions flowOptions = FlowOptions.builder()
                .direction(direction)
                .searchAllSources(searchAllSources)
                .depth(depth)
                .timeoutMs(effectiveTimeoutMs)
                .maxLimit(effectiveMaxPaths)
                .maxPaths(effectiveMaxPaths)
                .onlyFromWeb(onlyFromWeb)
                .traversalMode(traversalMode)
                .source(sourceClass, sourceMethod, sourceDesc)
                .sink(sinkClass, sinkMethod, sinkDesc)
                .build();
        GraphTaintEngine.TaintRun taintRun = new GraphTaintEngine().track(snapshot, flowOptions, null);
        ensureWithinBudget(budget);

        List<TaintResult> taintResults = taintRun.results();

        List<List<Object>> rows = new ArrayList<>();
        Set<String> warnings = new LinkedHashSet<>();
        int unresolvedNodes = 0;
        int pathId = 1;
        for (TaintResult taintResult : taintResults) {
            checkBudget(budget);
            budget.onPath();
            FlowPath dfs = taintResult.getDfsResult();
            Path mapped = mapTaintPathToGraph(dfs, snapshot, budget);
            unresolvedNodes += mapped.unresolvedCount;
            if (mapped.nodeIds.isEmpty()) {
                continue;
            }
            rows.add(toRow(pathId++, mapped,
                    taintResult.isLowConfidence() ? "low" : "high",
                    evidenceWithTraversalAndDirection(safe(taintResult.getTaintText()), traversalMode, direction)));
            if (rows.size() >= options.getMaxRows()) {
                break;
            }
        }
        if (unresolvedNodes > 0) {
            warnings.add("taint_unresolved_method_nodes=" + unresolvedNodes);
        }
        if (taintRun.stats().getTruncation().truncated()) {
            warnings.add("taint_truncated_reason=" + safe(taintRun.stats().getTruncation().reason()));
        }
        if (taintRun.stats().getNodeCount() > 0) {
            warnings.add("taint_node_visits=" + taintRun.stats().getNodeCount());
        }
        if (taintRun.stats().getEdgeCount() > 0) {
            warnings.add("taint_expanded_edges=" + taintRun.stats().getEdgeCount());
        }
        if (rows.stream().anyMatch(row -> String.valueOf(row.get(6)).contains("search backend: graph-pruned"))) {
            warnings.add("taint_search_backend=graph-pruned");
        }
        warnings.add("taint_search_direction=" + direction.displayName());
        if (traversalMode.includesAlias()) {
            warnings.add("taint_traversal_mode=" + traversalMode.displayName());
        }
        boolean truncated = taintResults.size() > rows.size()
                || taintRun.stats().getTruncation().truncated();
        return new QueryResult(DEFAULT_COLUMNS, rows, new ArrayList<>(warnings), truncated);
    }

    private QueryResult pathGadget(List<String> argExprs,
                                   Map<String, Object> params,
                                   QueryOptions options,
                                   GraphSnapshot snapshot) {
        long from = resolveNodeRef(resolveArg(argExprs, params, 0), snapshot);
        long to = resolveNodeRef(resolveArg(argExprs, params, 1), snapshot);
        int maxHops = toInt(resolveArg(argExprs, params, 2), options.getMaxHops());
        int maxPaths = toInt(resolveArg(argExprs, params, 3), options.getMaxPaths());
        TraversalMode traversalMode = TraversalMode.parse(toString(resolveArg(argExprs, params, 4)));
        SearchDirection direction = SearchDirection.parse(toString(resolveArg(argExprs, params, 5)), SearchDirection.FORWARD);
        QueryBudget budget = QueryBudget.of(options, maxHops, maxPaths, null);

        GadgetRunSelection selection = runGadgetSearch(snapshot, from, to, maxHops, maxPaths, traversalMode, direction, budget);
        ensureWithinBudget(budget);

        if (selection.candidates().isEmpty()) {
            return new QueryResult(
                    DEFAULT_COLUMNS,
                    List.of(),
                    buildGadgetWarnings(selection.stats(), "path_not_found", traversalMode, direction, null),
                    false
            );
        }

        List<List<Object>> rows = new ArrayList<>();
        int pathId = 1;
        for (GraphGadgetEngine.Candidate candidate : selection.candidates()) {
            rows.add(toRow(
                    pathId++,
                    new Path(candidate.nodeIds(), edgeIds(candidate.edges())),
                    safe(candidate.confidence()),
                    gadgetEvidence("ja.path.gadget", candidate, traversalMode, direction)
            ));
        }
        return new QueryResult(
                DEFAULT_COLUMNS,
                rows,
                buildGadgetWarnings(selection.stats(), null, traversalMode, direction, null),
                selection.truncated()
        );
    }

    private QueryResult gadgetTrack(List<String> argExprs,
                                    Map<String, Object> params,
                                    QueryOptions options,
                                    GraphSnapshot snapshot) {
        String sourceClass = normalizeClass(toString(resolveArg(argExprs, params, 0)));
        String sourceMethod = toString(resolveArg(argExprs, params, 1));
        String sourceDesc = normalizeDesc(toString(resolveArg(argExprs, params, 2)));
        String sinkClass = normalizeClass(toString(resolveArg(argExprs, params, 3)));
        String sinkMethod = toString(resolveArg(argExprs, params, 4));
        String sinkDesc = normalizeDesc(toString(resolveArg(argExprs, params, 5)));
        int depth = toInt(resolveArg(argExprs, params, 6), options.getMaxHops());
        Integer maxPathsArg = toNullableInt(resolveArg(argExprs, params, 7));
        boolean searchAllSources = toBoolean(resolveArg(argExprs, params, 8), false);
        TraversalMode traversalMode = TraversalMode.parse(toString(resolveArg(argExprs, params, 9)));
        SearchDirection direction = SearchDirection.parse(toString(resolveArg(argExprs, params, 10)), SearchDirection.FORWARD);

        if (sinkClass.isEmpty() || sinkMethod.isEmpty() || sinkDesc.isEmpty()) {
            throw new IllegalArgumentException("missing_param");
        }
        if (!searchAllSources
                && (sourceClass.isEmpty() || sourceMethod.isEmpty() || sourceDesc.isEmpty())) {
            throw new IllegalArgumentException("missing_param");
        }

        int maxPaths = options.getMaxPaths();
        if (maxPathsArg != null && maxPathsArg > 0) {
            maxPaths = maxPathsArg;
        }
        QueryBudget budget = QueryBudget.of(options, depth, maxPaths, null);

        long sinkNodeId = resolveMethodNodeRef(snapshot, sinkClass, sinkMethod, sinkDesc);
        List<Long> sourceNodeIds;
        if (searchAllSources) {
            sourceNodeIds = resolveGadgetSourceNodes(snapshot);
        } else {
            long sourceNodeId = resolveMethodNodeRef(snapshot, sourceClass, sourceMethod, sourceDesc);
            sourceNodeIds = sourceNodeId > 0L ? List.of(sourceNodeId) : List.of();
        }

        int sourceCandidates = sourceNodeIds.size();
        if (sinkNodeId <= 0L || sourceNodeIds.isEmpty()) {
            return new QueryResult(
                    DEFAULT_COLUMNS,
                    List.of(),
                    buildGadgetWarnings(null, "path_not_found", traversalMode, direction, searchAllSources ? sourceCandidates : null),
                    false
            );
        }

        List<List<Object>> rows = new ArrayList<>();
        Set<String> emitted = new LinkedHashSet<>();
        LinkedHashSet<String> warnings = new LinkedHashSet<>();
        boolean truncated = false;
        int pathId = 1;

        GadgetRunSelection selection;
        if (searchAllSources) {
            selection = runGadgetSearchAllSources(
                    snapshot,
                    sourceNodeIds,
                    sinkNodeId,
                    depth,
                    maxPaths,
                    traversalMode,
                    budget
            );
        } else {
            long sourceNodeId = sourceNodeIds.get(0);
            selection = runGadgetSearch(
                    snapshot,
                    sourceNodeId,
                    sinkNodeId,
                    depth,
                    maxPaths,
                    traversalMode,
                    direction,
                    budget
            );
        }
        ensureWithinBudget(budget);

        for (GraphGadgetEngine.Candidate candidate : selection.candidates()) {
            String key = joinLongs(candidate.nodeIds());
            if (!emitted.add(key)) {
                continue;
            }
            rows.add(toRow(
                    pathId++,
                    new Path(candidate.nodeIds(), edgeIds(candidate.edges())),
                    safe(candidate.confidence()),
                    gadgetEvidence("ja.gadget.track", candidate, traversalMode, direction)
            ));
            if (rows.size() >= maxPaths) {
                break;
            }
        }
        truncated = selection.truncated() || rows.size() >= maxPaths && selection.candidates().size() > rows.size();

        warnings.addAll(buildGadgetWarnings(
                selection.stats(),
                rows.isEmpty() ? "path_not_found" : null,
                traversalMode,
                direction,
                searchAllSources ? sourceCandidates : null
        ));
        return new QueryResult(DEFAULT_COLUMNS, rows, new ArrayList<>(warnings), truncated);
    }

    private static PrunedRunSelection selectPrunedShortest(GraphSnapshot snapshot,
                                                           long from,
                                                           long to,
                                                           int maxHops,
                                                           TraversalMode traversalMode,
                                                           SearchDirection direction,
                                                           QueryBudget budget) {
        PrunedRunSelection selection = runPrunedPaths(snapshot, from, to, maxHops, 1, traversalMode, direction, budget);
        if (selection.candidates().isEmpty()) {
            return selection;
        }
        GraphPrunedPathEngine.Candidate best = selection.candidates().get(0);
        for (GraphPrunedPathEngine.Candidate candidate : selection.candidates()) {
            if (candidate == null) {
                continue;
            }
            if (best == null || comparePrunedCandidate(candidate, best) < 0) {
                best = candidate;
            }
        }
        return new PrunedRunSelection(best == null ? List.of() : List.of(best), selection.stats(), selection.truncated());
    }

    private static PrunedRunSelection runPrunedPaths(GraphSnapshot snapshot,
                                                     long from,
                                                     long to,
                                                     int maxHops,
                                                     int maxPaths,
                                                     TraversalMode traversalMode,
                                                     SearchDirection direction,
                                                     QueryBudget budget) {
        GraphPrunedPathEngine engine = new GraphPrunedPathEngine();
        GraphPrunedPathEngine.DistanceCache distanceCache = new GraphPrunedPathEngine.DistanceCache();
        BudgetedPrunedController controller = new BudgetedPrunedController(budget);
        if (direction == SearchDirection.BACKWARD) {
            GraphPrunedPathEngine.Run run = engine.searchBackward(
                    snapshot,
                    new GraphPrunedPathEngine.ReverseSearchRequest(
                            to,
                            from,
                            Set.of(),
                            false,
                            false,
                            maxHops,
                            maxPaths,
                            traversalMode,
                            Set.of(),
                            "low"
                    ),
                    controller,
                    distanceCache
            );
            return new PrunedRunSelection(run.candidates(), run.stats(), run.candidates().size() >= maxPaths);
        }
        if (direction == SearchDirection.BIDIRECTIONAL) {
            GraphPrunedPathEngine.Run forward = engine.search(
                    snapshot,
                    new GraphPrunedPathEngine.SearchRequest(from, to, maxHops, maxPaths, traversalMode, Set.of(), "low"),
                    controller,
                    distanceCache
            );
            ensureWithinBudget(budget);
            int remaining = Math.max(0, maxPaths - forward.candidates().size());
            if (remaining <= 0) {
                return new PrunedRunSelection(forward.candidates(), forward.stats(), true);
            }
            GraphPrunedPathEngine.Run backward = engine.searchBackward(
                    snapshot,
                    new GraphPrunedPathEngine.ReverseSearchRequest(
                            to,
                            from,
                            Set.of(),
                            false,
                            false,
                            maxHops,
                            remaining,
                            traversalMode,
                            Set.of(),
                            "low"
                    ),
                    controller,
                    distanceCache
            );
            List<GraphPrunedPathEngine.Candidate> merged = mergePrunedCandidates(forward.candidates(), backward.candidates(), maxPaths);
            return new PrunedRunSelection(
                    merged,
                    mergePrunedStats(forward.stats(), backward.stats(), merged.size()),
                    merged.size() >= maxPaths && (!forward.candidates().isEmpty() || !backward.candidates().isEmpty())
            );
        }
        GraphPrunedPathEngine.Run run = engine.search(
                snapshot,
                new GraphPrunedPathEngine.SearchRequest(from, to, maxHops, maxPaths, traversalMode, Set.of(), "low"),
                controller,
                distanceCache
        );
        return new PrunedRunSelection(run.candidates(), run.stats(), run.candidates().size() >= maxPaths);
    }

    private static GadgetRunSelection runGadgetSearchAllSources(GraphSnapshot snapshot,
                                                                List<Long> sourceNodeIds,
                                                                long sinkNodeId,
                                                                int maxHops,
                                                                int maxPaths,
                                                                TraversalMode traversalMode,
                                                                QueryBudget budget) {
        Set<Long> candidates = new LinkedHashSet<>();
        if (sourceNodeIds != null) {
            for (Long sourceNodeId : sourceNodeIds) {
                if (sourceNodeId != null && sourceNodeId > 0L) {
                    candidates.add(sourceNodeId);
                }
            }
        }
        if (sinkNodeId <= 0L || candidates.isEmpty()) {
            return new GadgetRunSelection(List.of(), null, false);
        }
        int internalMaxPaths = Math.max(maxPaths, Math.min(maxPaths * Math.max(1, candidates.size()), 64));
        BudgetedGadgetController controller = new BudgetedGadgetController(budget);
        GraphGadgetEngine.Run run = new GraphGadgetEngine().searchBackwardAllSources(
                snapshot,
                new GraphGadgetEngine.AllSourcesReverseSearchRequest(
                        sinkNodeId,
                        candidates,
                        true,
                        maxHops,
                        internalMaxPaths,
                        traversalMode,
                        Set.of(),
                        "low"
                ),
                controller
        );
        List<GraphGadgetEngine.Candidate> selected = selectBestGadgetCandidatesBySource(
                sourceNodeIds,
                run.candidates(),
                maxPaths
        );
        return new GadgetRunSelection(
                selected,
                summarizeGadgetStats(run.stats(), selected),
                run.truncated() || selected.size() < Math.min(maxPaths, candidates.size()) && run.candidates().size() > selected.size()
        );
    }

    private static PathEnumeration enumeratePaths(GraphSnapshot snapshot,
                                                  long from,
                                                  long to,
                                                  int maxHops,
                                                  int maxPaths,
                                                  TraversalMode traversalMode,
                                                  SearchDirection direction,
                                                  QueryBudget budget) {
        if (direction == SearchDirection.BACKWARD) {
            return enumerateBackwardPaths(snapshot, from, to, maxHops, maxPaths, traversalMode, budget);
        }
        if (direction == SearchDirection.BIDIRECTIONAL) {
            PathEnumeration forward = enumerateForwardPaths(snapshot, from, to, maxHops, maxPaths, traversalMode, budget);
            if (forward.paths().size() >= maxPaths) {
                return forward;
            }
            PathEnumeration backward = enumerateBackwardPaths(snapshot, from, to, maxHops, maxPaths - forward.paths().size(), traversalMode, budget);
            List<Path> merged = mergePaths(forward.paths(), backward.paths(), maxPaths);
            return new PathEnumeration(merged, forward.truncated() || backward.truncated());
        }
        return enumerateForwardPaths(snapshot, from, to, maxHops, maxPaths, traversalMode, budget);
    }

    private static PathEnumeration enumerateForwardPaths(GraphSnapshot snapshot,
                                                         long from,
                                                         long to,
                                                         int maxHops,
                                                         int maxPaths,
                                                         TraversalMode traversalMode,
                                                         QueryBudget budget) {
        Map<Long, Integer> toTarget = boundedBackwardDistance(snapshot, to, maxHops, traversalMode, budget);
        if (!toTarget.containsKey(from)) {
            return PathEnumeration.empty();
        }
        List<Path> paths = new ArrayList<>();
        List<Long> nodePath = new ArrayList<>();
        List<Long> edgePath = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        ArrayDeque<DfsFrame> stack = new ArrayDeque<>();

        nodePath.add(from);
        visited.add(from);
        List<GraphEdge> firstOutgoing = rankedOutgoing(snapshot, from, visited, to, 0, maxHops, toTarget, traversalMode, maxPaths, budget);
        stack.push(new DfsFrame(from, firstOutgoing));

        while (!stack.isEmpty() && paths.size() < maxPaths) {
            checkBudget(budget);
            DfsFrame frame = stack.peek();
            long current = frame.nodeId;
            int currentHops = nodePath.size() - 1;
            Integer tail = toTarget.get(current);
            if (tail == null || currentHops + tail > maxHops) {
                backtrackFrame(stack, nodePath, edgePath, visited);
                continue;
            }
            if (current == to) {
                budget.onPath();
                paths.add(new Path(new ArrayList<>(nodePath), new ArrayList<>(edgePath)));
                backtrackFrame(stack, nodePath, edgePath, visited);
                continue;
            }
            if (currentHops >= maxHops || frame.cursor >= frame.outgoing.size()) {
                backtrackFrame(stack, nodePath, edgePath, visited);
                continue;
            }
            GraphEdge edge = frame.outgoing.get(frame.cursor++);
            long next = edge.getDstId();
            if (visited.contains(next)) {
                continue;
            }
            int nextHops = currentHops + 1;
            Integer nextTail = toTarget.get(next);
            if (nextTail == null || nextHops + nextTail > maxHops) {
                continue;
            }
            visited.add(next);
            nodePath.add(next);
            edgePath.add(edge.getEdgeId());
            List<GraphEdge> nextOutgoing = rankedOutgoing(snapshot, next, visited, to, nextHops, maxHops, toTarget, traversalMode, maxPaths, budget);
            stack.push(new DfsFrame(next, nextOutgoing));
        }
        return new PathEnumeration(paths, paths.size() >= maxPaths && !stack.isEmpty());
    }

    private static PathEnumeration enumerateBackwardPaths(GraphSnapshot snapshot,
                                                          long from,
                                                          long to,
                                                          int maxHops,
                                                          int maxPaths,
                                                          TraversalMode traversalMode,
                                                          QueryBudget budget) {
        Map<Long, Integer> fromSource = boundedForwardDistance(snapshot, from, maxHops, traversalMode, budget);
        if (!fromSource.containsKey(to)) {
            return PathEnumeration.empty();
        }
        List<Path> paths = new ArrayList<>();
        List<Long> backwardNodePath = new ArrayList<>();
        List<Long> backwardEdgePath = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        ArrayDeque<DfsFrame> stack = new ArrayDeque<>();

        backwardNodePath.add(to);
        visited.add(to);
        List<GraphEdge> firstIncoming = rankedIncoming(snapshot, to, visited, from, 0, maxHops, fromSource, traversalMode, maxPaths, budget);
        stack.push(new DfsFrame(to, firstIncoming));

        while (!stack.isEmpty() && paths.size() < maxPaths) {
            checkBudget(budget);
            DfsFrame frame = stack.peek();
            long current = frame.nodeId;
            int currentHops = backwardNodePath.size() - 1;
            Integer tail = fromSource.get(current);
            if (tail == null || currentHops + tail > maxHops) {
                backtrackFrame(stack, backwardNodePath, backwardEdgePath, visited);
                continue;
            }
            if (current == from) {
                budget.onPath();
                paths.add(reversePath(backwardNodePath, backwardEdgePath));
                backtrackFrame(stack, backwardNodePath, backwardEdgePath, visited);
                continue;
            }
            if (currentHops >= maxHops || frame.cursor >= frame.outgoing.size()) {
                backtrackFrame(stack, backwardNodePath, backwardEdgePath, visited);
                continue;
            }
            GraphEdge edge = frame.outgoing.get(frame.cursor++);
            long next = edge.getSrcId();
            if (visited.contains(next)) {
                continue;
            }
            int nextHops = currentHops + 1;
            Integer nextTail = fromSource.get(next);
            if (nextTail == null || nextHops + nextTail > maxHops) {
                continue;
            }
            visited.add(next);
            backwardNodePath.add(next);
            backwardEdgePath.add(edge.getEdgeId());
            List<GraphEdge> nextIncoming = rankedIncoming(snapshot, next, visited, from, nextHops, maxHops, fromSource, traversalMode, maxPaths, budget);
            stack.push(new DfsFrame(next, nextIncoming));
        }
        return new PathEnumeration(paths, paths.size() >= maxPaths && !stack.isEmpty());
    }

    private static GadgetRunSelection runGadgetSearch(GraphSnapshot snapshot,
                                                      long from,
                                                      long to,
                                                      int maxHops,
                                                      int maxPaths,
                                                      TraversalMode traversalMode,
                                                      SearchDirection direction,
                                                      QueryBudget budget) {
        BudgetedGadgetController controller = new BudgetedGadgetController(budget);
        if (direction == SearchDirection.BACKWARD) {
            GraphGadgetEngine.Run run = new GraphGadgetEngine().searchBackward(
                    snapshot,
                    new GraphGadgetEngine.ReverseSearchRequest(to, from, maxHops, maxPaths, traversalMode, Set.of(), "low"),
                    controller
            );
            return new GadgetRunSelection(run.candidates(), run.stats(), run.truncated());
        }
        if (direction == SearchDirection.BIDIRECTIONAL) {
            GraphGadgetEngine.Run forward = new GraphGadgetEngine().search(
                    snapshot,
                    new GraphGadgetEngine.SearchRequest(from, to, maxHops, maxPaths, traversalMode, Set.of(), "low"),
                    controller
            );
            ensureWithinBudget(budget);
            int remaining = Math.max(0, maxPaths - forward.candidates().size());
            if (remaining <= 0) {
                return new GadgetRunSelection(forward.candidates(), forward.stats(), true);
            }
            GraphGadgetEngine.Run backward = new GraphGadgetEngine().searchBackward(
                    snapshot,
                    new GraphGadgetEngine.ReverseSearchRequest(to, from, maxHops, remaining, traversalMode, Set.of(), "low"),
                    controller
            );
            List<GraphGadgetEngine.Candidate> merged = mergeGadgetCandidates(forward.candidates(), backward.candidates(), maxPaths);
            return new GadgetRunSelection(
                    merged,
                    mergeGadgetStats(forward.stats(), backward.stats(), merged),
                    forward.truncated() || backward.truncated() || merged.size() >= maxPaths
            );
        }
        GraphGadgetEngine.Run run = new GraphGadgetEngine().search(
                snapshot,
                new GraphGadgetEngine.SearchRequest(from, to, maxHops, maxPaths, traversalMode, Set.of(), "low"),
                controller
        );
        return new GadgetRunSelection(run.candidates(), run.stats(), run.truncated());
    }

    private static Path bidirectionalShortest(GraphSnapshot snapshot,
                                              long from,
                                              long to,
                                              int maxHops,
                                              TraversalMode traversalMode,
                                              QueryBudget budget) {
        if (from <= 0 || to <= 0) {
            return null;
        }
        if (from == to) {
            return new Path(List.of(from), List.of());
        }
        int bound = Math.max(1, maxHops);
        Map<Long, Integer> fromBound = boundedForwardDistance(snapshot, from, bound, traversalMode, budget);
        if (!fromBound.containsKey(to)) {
            return null;
        }
        Map<Long, Integer> toBound = boundedBackwardDistance(snapshot, to, bound, traversalMode, budget);
        if (!toBound.containsKey(from)) {
            return null;
        }

        ArrayDeque<Long> frontQ = new ArrayDeque<>();
        ArrayDeque<Long> backQ = new ArrayDeque<>();
        Map<Long, Integer> frontDist = new HashMap<>();
        Map<Long, Integer> backDist = new HashMap<>();
        Map<Long, PrevStep> frontPrev = new HashMap<>();
        Map<Long, NextStep> backNext = new HashMap<>();

        frontQ.add(from);
        backQ.add(to);
        frontDist.put(from, 0);
        backDist.put(to, 0);
        frontPrev.put(from, new PrevStep(-1L, -1L));
        backNext.put(to, new NextStep(-1L, -1L));

        int bestDistance = Integer.MAX_VALUE;
        long meetNode = -1L;

        while (!frontQ.isEmpty() && !backQ.isEmpty()) {
            checkBudget(budget);
            MeetCandidate candidate;
            if (frontQ.size() <= backQ.size()) {
                candidate = expandForwardLayer(snapshot, frontQ, frontDist, backDist, frontPrev, toBound, bound, traversalMode, budget);
            } else {
                candidate = expandBackwardLayer(snapshot, backQ, backDist, frontDist, backNext, fromBound, bound, traversalMode, budget);
            }
            if (candidate.distance < bestDistance) {
                bestDistance = candidate.distance;
                meetNode = candidate.meetNode;
            }
            if (meetNode <= 0L || bestDistance > bound) {
                continue;
            }
            int frontDepth = frontQ.isEmpty() ? Integer.MAX_VALUE : frontDist.get(frontQ.peek());
            int backDepth = backQ.isEmpty() ? Integer.MAX_VALUE : backDist.get(backQ.peek());
            if (frontDepth == Integer.MAX_VALUE || backDepth == Integer.MAX_VALUE || frontDepth + backDepth >= bestDistance) {
                break;
            }
        }

        if (meetNode <= 0L || bestDistance > bound) {
            return null;
        }
        return rebuildPath(meetNode, frontPrev, backNext);
    }

    private static Path singleDirectionShortest(GraphSnapshot snapshot,
                                                long from,
                                                long to,
                                                int maxHops,
                                                TraversalMode traversalMode,
                                                QueryBudget budget,
                                                boolean backward) {
        if (from <= 0L || to <= 0L) {
            return null;
        }
        if (from == to) {
            return new Path(List.of(from), List.of());
        }
        Map<Long, Long> parentNode = new HashMap<>();
        Map<Long, Long> parentEdge = new HashMap<>();
        Map<Long, Integer> dist = new HashMap<>();
        ArrayDeque<Long> queue = new ArrayDeque<>();
        long start = backward ? to : from;
        long target = backward ? from : to;
        queue.add(start);
        dist.put(start, 0);
        parentNode.put(start, -1L);
        parentEdge.put(start, -1L);
        while (!queue.isEmpty()) {
            checkBudget(budget);
            long current = queue.poll();
            int hops = dist.getOrDefault(current, 0);
            if (hops >= maxHops) {
                continue;
            }
            List<GraphEdge> edges = backward ? snapshot.getIncomingView(current) : snapshot.getOutgoingView(current);
            for (GraphEdge edge : edges) {
                budget.onExpand();
                long next = backward ? edge.getSrcId() : edge.getDstId();
                if (!isTraversable(snapshot, edge, traversalMode) || dist.containsKey(next)) {
                    continue;
                }
                dist.put(next, hops + 1);
                parentNode.put(next, current);
                parentEdge.put(next, edge.getEdgeId());
                if (next == target) {
                    return backward
                            ? rebuildReverseShortest(parentNode, parentEdge, from, to)
                            : rebuildForwardShortest(parentNode, parentEdge, from, to);
                }
                queue.add(next);
            }
        }
        return null;
    }

    private static MeetCandidate expandForwardLayer(GraphSnapshot snapshot,
                                                    ArrayDeque<Long> queue,
                                                    Map<Long, Integer> frontDist,
                                                    Map<Long, Integer> backDist,
                                                    Map<Long, PrevStep> frontPrev,
                                                    Map<Long, Integer> toTargetBound,
                                                    int maxHops,
                                                    TraversalMode traversalMode,
                                                    QueryBudget budget) {
        int size = queue.size();
        MeetCandidate best = MeetCandidate.none();
        for (int i = 0; i < size; i++) {
            checkBudget(budget);
            long current = queue.poll();
            Integer currentDist = frontDist.get(current);
            if (currentDist == null || currentDist >= maxHops) {
                continue;
            }
            Integer tail = toTargetBound.get(current);
            if (tail == null || currentDist + tail > maxHops) {
                continue;
            }
            for (GraphEdge edge : snapshot.getOutgoingView(current)) {
                budget.onExpand();
                long next = edge.getDstId();
                if (!isTraversable(snapshot, edge, traversalMode)) {
                    continue;
                }
                int nextDist = currentDist + 1;
                if (nextDist > maxHops) {
                    continue;
                }
                Integer nextTail = toTargetBound.get(next);
                if (nextTail == null || nextDist + nextTail > maxHops) {
                    continue;
                }
                Integer existed = frontDist.get(next);
                if (existed != null && existed <= nextDist) {
                    continue;
                }
                frontDist.put(next, nextDist);
                frontPrev.put(next, new PrevStep(current, edge.getEdgeId()));
                queue.add(next);
                Integer back = backDist.get(next);
                if (back != null) {
                    int total = nextDist + back;
                    if (total < best.distance) {
                        best = new MeetCandidate(next, total);
                    }
                }
            }
        }
        return best;
    }

    private static MeetCandidate expandBackwardLayer(GraphSnapshot snapshot,
                                                     ArrayDeque<Long> queue,
                                                     Map<Long, Integer> backDist,
                                                     Map<Long, Integer> frontDist,
                                                     Map<Long, NextStep> backNext,
                                                     Map<Long, Integer> fromSourceBound,
                                                     int maxHops,
                                                     TraversalMode traversalMode,
                                                     QueryBudget budget) {
        int size = queue.size();
        MeetCandidate best = MeetCandidate.none();
        for (int i = 0; i < size; i++) {
            checkBudget(budget);
            long current = queue.poll();
            Integer currentDist = backDist.get(current);
            if (currentDist == null || currentDist >= maxHops) {
                continue;
            }
            Integer tail = fromSourceBound.get(current);
            if (tail == null || currentDist + tail > maxHops) {
                continue;
            }
            for (GraphEdge edge : snapshot.getIncomingView(current)) {
                budget.onExpand();
                long prev = edge.getSrcId();
                if (!isTraversable(snapshot, edge, traversalMode)) {
                    continue;
                }
                int nextDist = currentDist + 1;
                if (nextDist > maxHops) {
                    continue;
                }
                Integer nextTail = fromSourceBound.get(prev);
                if (nextTail == null || nextDist + nextTail > maxHops) {
                    continue;
                }
                Integer existed = backDist.get(prev);
                if (existed != null && existed <= nextDist) {
                    continue;
                }
                backDist.put(prev, nextDist);
                backNext.put(prev, new NextStep(current, edge.getEdgeId()));
                queue.add(prev);
                Integer front = frontDist.get(prev);
                if (front != null) {
                    int total = nextDist + front;
                    if (total < best.distance) {
                        best = new MeetCandidate(prev, total);
                    }
                }
            }
        }
        return best;
    }

    private static Path rebuildPath(long meetNode,
                                    Map<Long, PrevStep> frontPrev,
                                    Map<Long, NextStep> backNext) {
        List<Long> prefixNodes = new ArrayList<>();
        List<Long> prefixEdges = new ArrayList<>();
        long cursor = meetNode;
        while (cursor > 0L) {
            prefixNodes.add(cursor);
            PrevStep step = frontPrev.get(cursor);
            if (step == null || step.prevNode <= 0L) {
                break;
            }
            prefixEdges.add(step.viaEdgeId);
            cursor = step.prevNode;
        }
        Collections.reverse(prefixNodes);
        Collections.reverse(prefixEdges);

        List<Long> nodeIds = new ArrayList<>(prefixNodes);
        List<Long> edgeIds = new ArrayList<>(prefixEdges);
        long nextCursor = meetNode;
        while (true) {
            NextStep step = backNext.get(nextCursor);
            if (step == null || step.nextNode <= 0L) {
                break;
            }
            edgeIds.add(step.viaEdgeId);
            nodeIds.add(step.nextNode);
            nextCursor = step.nextNode;
        }
        return new Path(nodeIds, edgeIds);
    }

    private static Map<Long, Integer> boundedForwardDistance(GraphSnapshot snapshot,
                                                             long start,
                                                             int maxHops,
                                                             TraversalMode traversalMode,
                                                             QueryBudget budget) {
        if (start <= 0L || maxHops <= 0) {
            return Collections.emptyMap();
        }
        Map<Long, Integer> dist = new HashMap<>();
        ArrayDeque<Long> queue = new ArrayDeque<>();
        dist.put(start, 0);
        queue.add(start);
        while (!queue.isEmpty()) {
            checkBudget(budget);
            long current = queue.poll();
            int hops = dist.get(current);
            if (hops >= maxHops) {
                continue;
            }
            for (GraphEdge edge : snapshot.getOutgoingView(current)) {
                budget.onExpand();
                long next = edge.getDstId();
                if (!isTraversable(snapshot, edge, traversalMode) || dist.containsKey(next)) {
                    continue;
                }
                dist.put(next, hops + 1);
                queue.add(next);
            }
        }
        return dist;
    }

    private static Map<Long, Integer> boundedBackwardDistance(GraphSnapshot snapshot,
                                                              long target,
                                                              int maxHops,
                                                              TraversalMode traversalMode,
                                                              QueryBudget budget) {
        if (target <= 0L || maxHops <= 0) {
            return Collections.emptyMap();
        }
        Map<Long, Integer> dist = new HashMap<>();
        ArrayDeque<Long> queue = new ArrayDeque<>();
        dist.put(target, 0);
        queue.add(target);
        while (!queue.isEmpty()) {
            checkBudget(budget);
            long current = queue.poll();
            int hops = dist.get(current);
            if (hops >= maxHops) {
                continue;
            }
            for (GraphEdge edge : snapshot.getIncomingView(current)) {
                budget.onExpand();
                long prev = edge.getSrcId();
                if (!isTraversable(snapshot, edge, traversalMode) || dist.containsKey(prev)) {
                    continue;
                }
                dist.put(prev, hops + 1);
                queue.add(prev);
            }
        }
        return dist;
    }

    private static List<GraphEdge> rankedIncoming(GraphSnapshot snapshot,
                                                  long current,
                                                  Set<Long> visited,
                                                  long source,
                                                  int currentHops,
                                                  int maxHops,
                                                  Map<Long, Integer> fromSource,
                                                  TraversalMode traversalMode,
                                                  int maxPaths,
                                                  QueryBudget budget) {
        List<ScoredEdge> scored = new ArrayList<>();
        for (GraphEdge edge : snapshot.getIncomingView(current)) {
            budget.onExpand();
            long prev = edge.getSrcId();
            if (!isTraversable(snapshot, edge, traversalMode) || visited.contains(prev)) {
                continue;
            }
            int nextHops = currentHops + 1;
            if (nextHops > maxHops) {
                continue;
            }
            Integer tail = fromSource.get(prev);
            if (tail == null || nextHops + tail > maxHops) {
                continue;
            }
            int score = confidenceScore(edge.getConfidence()) * 1000
                    + relationScore(edge.getRelType()) * 10
                    - tail;
            if (prev == source) {
                score += 10_000;
            }
            scored.add(new ScoredEdge(edge, score));
        }
        if (scored.isEmpty()) {
            return List.of();
        }
        scored.sort(Comparator.comparingInt(ScoredEdge::score).reversed());
        int dynamicCap = Math.max(8, Math.min(MAX_FAN_OUT, Math.max(8, maxPaths / 2)));
        int limit = Math.min(dynamicCap, scored.size());
        List<GraphEdge> out = new ArrayList<>(limit);
        for (int i = 0; i < limit; i++) {
            out.add(scored.get(i).edge);
        }
        return out;
    }

    private static List<GraphEdge> rankedOutgoing(GraphSnapshot snapshot,
                                                  long current,
                                                  Set<Long> visited,
                                                  long target,
                                                  int currentHops,
                                                  int maxHops,
                                                  Map<Long, Integer> toTarget,
                                                  TraversalMode traversalMode,
                                                  int maxPaths,
                                                  QueryBudget budget) {
        List<ScoredEdge> scored = new ArrayList<>();
        for (GraphEdge edge : snapshot.getOutgoingView(current)) {
            budget.onExpand();
            long next = edge.getDstId();
            if (!isTraversable(snapshot, edge, traversalMode) || visited.contains(next)) {
                continue;
            }
            int nextHops = currentHops + 1;
            if (nextHops > maxHops) {
                continue;
            }
            Integer tail = toTarget.get(next);
            if (tail == null || nextHops + tail > maxHops) {
                continue;
            }
            int score = confidenceScore(edge.getConfidence()) * 1000
                    + relationScore(edge.getRelType()) * 10
                    - tail;
            if (next == target) {
                score += 10_000;
            }
            scored.add(new ScoredEdge(edge, score));
        }
        if (scored.isEmpty()) {
            return List.of();
        }
        scored.sort(Comparator.comparingInt(ScoredEdge::score).reversed());
        int dynamicCap = Math.max(8, Math.min(MAX_FAN_OUT, Math.max(8, maxPaths / 2)));
        int limit = Math.min(dynamicCap, scored.size());
        List<GraphEdge> out = new ArrayList<>(limit);
        for (int i = 0; i < limit; i++) {
            out.add(scored.get(i).edge);
        }
        return out;
    }

    private static void backtrackFrame(ArrayDeque<DfsFrame> stack,
                                       List<Long> nodePath,
                                       List<Long> edgePath,
                                       Set<Long> visited) {
        stack.pop();
        if (stack.isEmpty()) {
            return;
        }
        if (!nodePath.isEmpty()) {
            long removedNode = nodePath.remove(nodePath.size() - 1);
            visited.remove(removedNode);
        }
        if (!edgePath.isEmpty()) {
            edgePath.remove(edgePath.size() - 1);
        }
    }

    private static Path mapTaintPathToGraph(FlowPath dfs, GraphSnapshot snapshot, QueryBudget budget) {
        if (dfs == null || dfs.getMethodList() == null || dfs.getMethodList().isEmpty()) {
            return new Path(List.of(), List.of(), 0);
        }
        List<Long> nodeIds = new ArrayList<>();
        int unresolved = 0;
        for (MethodReference.Handle handle : dfs.getMethodList()) {
            long nodeId = resolveMethodNodeId(snapshot, handle);
            if (nodeId <= 0L) {
                unresolved++;
                continue;
            }
            if (!nodeIds.isEmpty() && nodeIds.get(nodeIds.size() - 1) == nodeId) {
                continue;
            }
            nodeIds.add(nodeId);
        }
        if (nodeIds.isEmpty()) {
            return new Path(List.of(), List.of(), unresolved);
        }
        List<Long> edgeIds = resolveEdgeChain(snapshot, nodeIds, budget);
        return new Path(nodeIds, edgeIds, unresolved);
    }

    private static List<Long> resolveEdgeChain(GraphSnapshot snapshot, List<Long> nodeIds, QueryBudget budget) {
        if (nodeIds == null || nodeIds.size() < 2) {
            return List.of();
        }
        List<Long> edgeIds = new ArrayList<>(Math.max(0, nodeIds.size() - 1));
        for (int i = 0; i + 1 < nodeIds.size(); i++) {
            long src = nodeIds.get(i);
            long dst = nodeIds.get(i + 1);
            GraphEdge best = null;
            int bestScore = Integer.MIN_VALUE;
            for (GraphEdge edge : snapshot.getOutgoingView(src)) {
                budget.onExpand();
                if (edge.getDstId() != dst) {
                    continue;
                }
                if (!GraphTraversalRules.isMethodFlowEdge(snapshot, edge, true)) {
                    continue;
                }
                int score = confidenceScore(edge.getConfidence()) * 1000 + relationScore(edge.getRelType());
                if (score > bestScore) {
                    best = edge;
                    bestScore = score;
                }
            }
            if (best != null) {
                edgeIds.add(best.getEdgeId());
            }
        }
        return edgeIds;
    }

    private static long resolveMethodNodeId(GraphSnapshot snapshot, MethodReference.Handle handle) {
        if (snapshot == null || handle == null || handle.getClassReference() == null) {
            return -1L;
        }
        return snapshot.findMethodNodeId(
                handle.getClassReference().getName(),
                handle.getName(),
                handle.getDesc(),
                handle.getJarId()
        );
    }

    private static int confidenceScore(String confidence) {
        String v = safe(confidence).toLowerCase();
        return switch (v) {
            case "high" -> 3;
            case "medium" -> 2;
            case "low" -> 1;
            default -> 0;
        };
    }

    private static int relationScore(String relation) {
        String rel = safe(relation).toUpperCase();
        return switch (rel) {
            case "CALLS_DIRECT" -> 9;
            case "CALLS_DISPATCH" -> 8;
            case "CALLS_REFLECTION" -> 7;
            case "CALLS_CALLBACK" -> 6;
            case "CALLS_OVERRIDE" -> 5;
            case "ALIAS" -> 3;
            default -> 1;
        };
    }

    private static List<String> buildPrunedWarnings(GraphPrunedPathEngine.Stats stats,
                                                    String extra,
                                                    SearchDirection direction) {
        LinkedHashSet<String> warnings = new LinkedHashSet<>();
        if (extra != null && !extra.isBlank()) {
            warnings.add(extra);
        }
        warnings.add("path_search_direction=" + direction.displayName());
        if (stats != null) {
            warnings.add("pruned_node_visits=" + stats.nodeVisits());
            warnings.add("pruned_expanded_edges=" + stats.expandedEdges());
            warnings.add("pruned_state_cuts=" + stats.prunedByState());
            warnings.add("pruned_distance_cuts=" + stats.prunedByDistance());
            warnings.add("pruned_elapsed_ms=" + stats.elapsedMs());
        }
        return new ArrayList<>(warnings);
    }

    private static List<String> buildGadgetWarnings(GraphGadgetEngine.Stats stats,
                                                    String extra,
                                                    TraversalMode traversalMode,
                                                    SearchDirection direction,
                                                    Integer sourceCandidates) {
        LinkedHashSet<String> warnings = new LinkedHashSet<>();
        if (extra != null && !extra.isBlank()) {
            warnings.add(extra);
        }
        warnings.add("gadget_search_direction=" + direction.displayName());
        if (sourceCandidates != null) {
            warnings.add("gadget_source_candidates=" + Math.max(0, sourceCandidates));
        }
        if (stats != null) {
            warnings.add("gadget_node_visits=" + stats.nodeVisits());
            warnings.add("gadget_expanded_edges=" + stats.expandedEdges());
            warnings.add("gadget_state_cuts=" + stats.prunedByState());
            warnings.add("gadget_distance_cuts=" + stats.prunedByDistance());
            warnings.add("gadget_elapsed_ms=" + stats.elapsedMs());
            for (Map.Entry<String, Integer> entry : stats.routeCounts().entrySet()) {
                if (entry.getKey() == null || entry.getKey().isBlank()) {
                    continue;
                }
                warnings.add("gadget_route_" + entry.getKey().replace('-', '_') + "=" + Math.max(0, entry.getValue()));
            }
        }
        if (traversalMode != null && traversalMode.includesAlias()) {
            warnings.add("gadget_traversal_mode=" + traversalMode.displayName());
        }
        return new ArrayList<>(warnings);
    }

    private static String prunedEvidence(String name,
                                         GraphPrunedPathEngine.Candidate candidate,
                                         TraversalMode traversalMode,
                                         SearchDirection direction) {
        StringBuilder sb = new StringBuilder(safe(name));
        if (candidate != null && candidate.proven()) {
            sb.append(" proven");
        }
        if (candidate != null && candidate.lowConfidence()) {
            sb.append(" low-confidence");
        }
        return evidenceWithTraversalAndDirection(sb.toString().trim(), traversalMode, direction);
    }

    private static String gadgetEvidence(String name,
                                         GraphGadgetEngine.Candidate candidate,
                                         TraversalMode traversalMode,
                                         SearchDirection direction) {
        StringBuilder sb = new StringBuilder(safe(name));
        if (candidate != null && !safe(candidate.route()).isBlank()) {
            sb.append(" route=").append(candidate.route());
        }
        if (candidate != null && candidate.constraints() != null && !candidate.constraints().isEmpty()) {
            sb.append(" constraints=").append(String.join(",", candidate.constraints()));
        }
        if (candidate != null && candidate.lowConfidence()) {
            sb.append(" low-confidence");
        }
        return evidenceWithTraversalAndDirection(sb.toString().trim(), traversalMode, direction);
    }

    private static String evidenceWithTraversal(String evidence, TraversalMode traversalMode) {
        String base = safe(evidence);
        if (traversalMode == null || !traversalMode.includesAlias()) {
            return base;
        }
        if (base.isEmpty()) {
            return "traversal=" + traversalMode.displayName();
        }
        if (base.contains("traversal=")) {
            return base;
        }
        return base + "\ntraversal=" + traversalMode.displayName();
    }

    private static String evidenceWithTraversalAndDirection(String evidence,
                                                            TraversalMode traversalMode,
                                                            SearchDirection direction) {
        String withTraversal = evidenceWithTraversal(evidence, traversalMode);
        if (direction == null) {
            return withTraversal;
        }
        if (withTraversal.isEmpty()) {
            return "direction=" + direction.displayName();
        }
        if (withTraversal.contains("direction=")) {
            return withTraversal;
        }
        return withTraversal + "\ndirection=" + direction.displayName();
    }

    private static List<Long> edgeIds(List<GraphEdge> edges) {
        if (edges == null || edges.isEmpty()) {
            return List.of();
        }
        List<Long> out = new ArrayList<>(edges.size());
        for (GraphEdge edge : edges) {
            if (edge != null) {
                out.add(edge.getEdgeId());
            }
        }
        return out;
    }

    private static List<Object> toRow(int pathId, Path path, String confidence, String evidence) {
        int hop = Math.max(0, path.nodeIds.size() - 1);
        double score = hop <= 0 ? 1.0 : (1.0 / hop);
        return List.of(
                pathId,
                hop,
                joinLongs(path.nodeIds),
                joinLongs(path.edgeIds),
                score,
                confidence,
                evidence
        );
    }

    private static int comparePrunedCandidate(GraphPrunedPathEngine.Candidate left,
                                              GraphPrunedPathEngine.Candidate right) {
        int leftHop = left == null || left.nodeIds() == null ? Integer.MAX_VALUE : Math.max(0, left.nodeIds().size() - 1);
        int rightHop = right == null || right.nodeIds() == null ? Integer.MAX_VALUE : Math.max(0, right.nodeIds().size() - 1);
        if (leftHop != rightHop) {
            return Integer.compare(leftHop, rightHop);
        }
        if (left != null && right != null && left.proven() != right.proven()) {
            return left.proven() ? -1 : 1;
        }
        if (left != null && right != null && left.lowConfidence() != right.lowConfidence()) {
            return left.lowConfidence() ? 1 : -1;
        }
        long leftEdge = left == null || left.edges() == null || left.edges().isEmpty() ? Long.MAX_VALUE : left.edges().get(0).getEdgeId();
        long rightEdge = right == null || right.edges() == null || right.edges().isEmpty() ? Long.MAX_VALUE : right.edges().get(0).getEdgeId();
        return Long.compare(leftEdge, rightEdge);
    }

    private static List<GraphPrunedPathEngine.Candidate> mergePrunedCandidates(List<GraphPrunedPathEngine.Candidate> first,
                                                                               List<GraphPrunedPathEngine.Candidate> second,
                                                                               int maxPaths) {
        LinkedHashMap<String, GraphPrunedPathEngine.Candidate> merged = new LinkedHashMap<>();
        appendPrunedCandidates(merged, first, maxPaths);
        appendPrunedCandidates(merged, second, maxPaths);
        return List.copyOf(merged.values());
    }

    private static void appendPrunedCandidates(Map<String, GraphPrunedPathEngine.Candidate> merged,
                                               List<GraphPrunedPathEngine.Candidate> values,
                                               int maxPaths) {
        if (values == null || values.isEmpty() || merged.size() >= maxPaths) {
            return;
        }
        for (GraphPrunedPathEngine.Candidate candidate : values) {
            if (candidate == null) {
                continue;
            }
            String key = joinLongs(candidate.nodeIds());
            GraphPrunedPathEngine.Candidate current = merged.get(key);
            if (current == null || comparePrunedCandidate(candidate, current) < 0) {
                merged.put(key, candidate);
            }
            if (merged.size() >= maxPaths) {
                return;
            }
        }
    }

    private static GraphPrunedPathEngine.Stats mergePrunedStats(GraphPrunedPathEngine.Stats first,
                                                                GraphPrunedPathEngine.Stats second,
                                                                int emittedPaths) {
        if (first == null && second == null) {
            return new GraphPrunedPathEngine.Stats(0, 0, emittedPaths, 0, 0, 0L);
        }
        if (first == null) {
            return new GraphPrunedPathEngine.Stats(
                    second.nodeVisits(),
                    second.expandedEdges(),
                    emittedPaths,
                    second.prunedByState(),
                    second.prunedByDistance(),
                    second.elapsedMs()
            );
        }
        if (second == null) {
            return new GraphPrunedPathEngine.Stats(
                    first.nodeVisits(),
                    first.expandedEdges(),
                    emittedPaths,
                    first.prunedByState(),
                    first.prunedByDistance(),
                    first.elapsedMs()
            );
        }
        return new GraphPrunedPathEngine.Stats(
                first.nodeVisits() + second.nodeVisits(),
                first.expandedEdges() + second.expandedEdges(),
                emittedPaths,
                first.prunedByState() + second.prunedByState(),
                first.prunedByDistance() + second.prunedByDistance(),
                first.elapsedMs() + second.elapsedMs()
        );
    }

    private static List<Path> mergePaths(List<Path> first, List<Path> second, int maxPaths) {
        LinkedHashMap<String, Path> merged = new LinkedHashMap<>();
        appendPaths(merged, first, maxPaths);
        appendPaths(merged, second, maxPaths);
        return List.copyOf(merged.values());
    }

    private static void appendPaths(Map<String, Path> merged, List<Path> values, int maxPaths) {
        if (values == null || values.isEmpty() || merged.size() >= maxPaths) {
            return;
        }
        for (Path path : values) {
            if (path == null) {
                continue;
            }
            merged.putIfAbsent(joinLongs(path.nodeIds), path);
            if (merged.size() >= maxPaths) {
                return;
            }
        }
    }

    private static List<GraphGadgetEngine.Candidate> mergeGadgetCandidates(List<GraphGadgetEngine.Candidate> first,
                                                                           List<GraphGadgetEngine.Candidate> second,
                                                                           int maxPaths) {
        LinkedHashMap<String, GraphGadgetEngine.Candidate> merged = new LinkedHashMap<>();
        appendGadgetCandidates(merged, first, maxPaths);
        appendGadgetCandidates(merged, second, maxPaths);
        return List.copyOf(merged.values());
    }

    private static void appendGadgetCandidates(Map<String, GraphGadgetEngine.Candidate> merged,
                                               List<GraphGadgetEngine.Candidate> values,
                                               int maxPaths) {
        if (values == null || values.isEmpty() || merged.size() >= maxPaths) {
            return;
        }
        for (GraphGadgetEngine.Candidate candidate : values) {
            if (candidate == null) {
                continue;
            }
            merged.putIfAbsent(joinLongs(candidate.nodeIds()), candidate);
            if (merged.size() >= maxPaths) {
                return;
            }
        }
    }

    private static List<GraphGadgetEngine.Candidate> selectBestGadgetCandidatesBySource(List<Long> sourceNodeIds,
                                                                                         List<GraphGadgetEngine.Candidate> candidates,
                                                                                         int maxPaths) {
        if (candidates == null || candidates.isEmpty() || maxPaths <= 0) {
            return List.of();
        }
        Map<Long, GraphGadgetEngine.Candidate> bestBySource = new LinkedHashMap<>();
        for (GraphGadgetEngine.Candidate candidate : candidates) {
            if (candidate == null || candidate.nodeIds() == null || candidate.nodeIds().isEmpty()) {
                continue;
            }
            long sourceNodeId = candidate.nodeIds().get(0);
            GraphGadgetEngine.Candidate current = bestBySource.get(sourceNodeId);
            if (current == null || compareGadgetCandidate(candidate, current) < 0) {
                bestBySource.put(sourceNodeId, candidate);
            }
        }
        if (bestBySource.isEmpty()) {
            return List.of();
        }
        List<GraphGadgetEngine.Candidate> selected = new ArrayList<>();
        Set<String> emitted = new LinkedHashSet<>();
        if (sourceNodeIds != null) {
            for (Long sourceNodeId : sourceNodeIds) {
                if (sourceNodeId == null) {
                    continue;
                }
                GraphGadgetEngine.Candidate candidate = bestBySource.get(sourceNodeId);
                if (candidate == null) {
                    continue;
                }
                String key = joinLongs(candidate.nodeIds());
                if (emitted.add(key)) {
                    selected.add(candidate);
                }
                if (selected.size() >= maxPaths) {
                    return List.copyOf(selected);
                }
            }
        }
        if (selected.size() >= maxPaths) {
            return List.copyOf(selected);
        }
        List<GraphGadgetEngine.Candidate> overflow = new ArrayList<>(bestBySource.values());
        overflow.sort(ProcedureRegistry::compareGadgetCandidate);
        for (GraphGadgetEngine.Candidate candidate : overflow) {
            String key = joinLongs(candidate.nodeIds());
            if (!emitted.add(key)) {
                continue;
            }
            selected.add(candidate);
            if (selected.size() >= maxPaths) {
                break;
            }
        }
        return List.copyOf(selected);
    }

    private static int compareGadgetCandidate(GraphGadgetEngine.Candidate left,
                                              GraphGadgetEngine.Candidate right) {
        if (left == right) {
            return 0;
        }
        if (left == null) {
            return 1;
        }
        if (right == null) {
            return -1;
        }
        int route = Integer.compare(gadgetRoutePriority(safe(left.route())), gadgetRoutePriority(safe(right.route())));
        if (route != 0) {
            return route;
        }
        int confidence = Integer.compare(gadgetConfidenceRank(safe(right.confidence())), gadgetConfidenceRank(safe(left.confidence())));
        if (confidence != 0) {
            return confidence;
        }
        int length = Integer.compare(
                left.nodeIds() == null ? Integer.MAX_VALUE : left.nodeIds().size(),
                right.nodeIds() == null ? Integer.MAX_VALUE : right.nodeIds().size()
        );
        if (length != 0) {
            return length;
        }
        return joinLongs(left.nodeIds()).compareTo(joinLongs(right.nodeIds()));
    }

    private static int gadgetRoutePriority(String route) {
        return switch (safe(route)) {
            case "container-callback" -> 0;
            case "proxy-dynamic" -> 1;
            case "container-trigger" -> 2;
            case "reflection-trigger" -> 3;
            case "proxy-trigger" -> 4;
            case "reflection-callback" -> 5;
            case "reflection-container" -> 6;
            case "deserialization-trigger" -> 7;
            default -> Integer.MAX_VALUE;
        };
    }

    private static int gadgetConfidenceRank(String confidence) {
        return switch (safe(confidence).toLowerCase(Locale.ROOT)) {
            case "high" -> 3;
            case "medium" -> 2;
            case "low" -> 1;
            default -> 0;
        };
    }

    private static GraphGadgetEngine.Stats summarizeGadgetStats(GraphGadgetEngine.Stats stats,
                                                                List<GraphGadgetEngine.Candidate> emitted) {
        LinkedHashMap<String, Integer> routes = new LinkedHashMap<>();
        if (emitted != null) {
            for (GraphGadgetEngine.Candidate candidate : emitted) {
                if (candidate == null || safe(candidate.route()).isBlank()) {
                    continue;
                }
                routes.merge(candidate.route(), 1, Integer::sum);
            }
        }
        if (stats == null) {
            return new GraphGadgetEngine.Stats(0, 0, emitted == null ? 0 : emitted.size(), 0, 0, 0L, routes);
        }
        return new GraphGadgetEngine.Stats(
                stats.nodeVisits(),
                stats.expandedEdges(),
                emitted == null ? 0 : emitted.size(),
                stats.prunedByState(),
                stats.prunedByDistance(),
                stats.elapsedMs(),
                routes
        );
    }

    private static GraphGadgetEngine.Stats mergeGadgetStats(GraphGadgetEngine.Stats first,
                                                            GraphGadgetEngine.Stats second,
                                                            List<GraphGadgetEngine.Candidate> merged) {
        LinkedHashMap<String, Integer> routes = new LinkedHashMap<>();
        if (merged != null) {
            for (GraphGadgetEngine.Candidate candidate : merged) {
                if (candidate == null || safe(candidate.route()).isBlank()) {
                    continue;
                }
                routes.merge(candidate.route(), 1, Integer::sum);
            }
        }
        int nodeVisits = (first == null ? 0 : first.nodeVisits()) + (second == null ? 0 : second.nodeVisits());
        int expandedEdges = (first == null ? 0 : first.expandedEdges()) + (second == null ? 0 : second.expandedEdges());
        int stateCuts = (first == null ? 0 : first.prunedByState()) + (second == null ? 0 : second.prunedByState());
        int distanceCuts = (first == null ? 0 : first.prunedByDistance()) + (second == null ? 0 : second.prunedByDistance());
        long elapsedMs = (first == null ? 0L : first.elapsedMs()) + (second == null ? 0L : second.elapsedMs());
        return new GraphGadgetEngine.Stats(nodeVisits, expandedEdges, merged == null ? 0 : merged.size(), stateCuts, distanceCuts, elapsedMs, routes);
    }

    private static Path reversePath(List<Long> backwardNodePath, List<Long> backwardEdgePath) {
        List<Long> nodeIds = new ArrayList<>(backwardNodePath.size());
        for (int i = backwardNodePath.size() - 1; i >= 0; i--) {
            nodeIds.add(backwardNodePath.get(i));
        }
        List<Long> edgeIds = new ArrayList<>(backwardEdgePath.size());
        for (int i = backwardEdgePath.size() - 1; i >= 0; i--) {
            edgeIds.add(backwardEdgePath.get(i));
        }
        return new Path(nodeIds, edgeIds);
    }

    private static Path rebuildForwardShortest(Map<Long, Long> parentNode,
                                               Map<Long, Long> parentEdge,
                                               long from,
                                               long to) {
        ArrayList<Long> nodeIds = new ArrayList<>();
        ArrayList<Long> edgeIds = new ArrayList<>();
        long cursor = to;
        while (cursor > 0L) {
            nodeIds.add(cursor);
            long prev = parentNode.getOrDefault(cursor, -1L);
            long via = parentEdge.getOrDefault(cursor, -1L);
            if (prev <= 0L) {
                break;
            }
            edgeIds.add(via);
            cursor = prev;
        }
        Collections.reverse(nodeIds);
        Collections.reverse(edgeIds);
        return !nodeIds.isEmpty() && nodeIds.get(0) == from ? new Path(nodeIds, edgeIds) : null;
    }

    private static Path rebuildReverseShortest(Map<Long, Long> parentNode,
                                               Map<Long, Long> parentEdge,
                                               long from,
                                               long to) {
        ArrayList<Long> nodePath = new ArrayList<>();
        ArrayList<Long> edgePath = new ArrayList<>();
        long cursor = from;
        while (cursor > 0L) {
            nodePath.add(cursor);
            long next = parentNode.getOrDefault(cursor, -1L);
            long via = parentEdge.getOrDefault(cursor, -1L);
            if (next <= 0L) {
                break;
            }
            edgePath.add(via);
            cursor = next;
        }
        if (nodePath.isEmpty() || nodePath.get(nodePath.size() - 1) != to) {
            return null;
        }
        return new Path(nodePath, edgePath);
    }

    private static String joinLongs(List<Long> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Long value : values) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(value == null ? -1 : value);
        }
        return sb.toString();
    }

    private static Object resolveArg(List<String> argExprs, Map<String, Object> params, int index) {
        if (argExprs == null || index < 0 || index >= argExprs.size()) {
            return null;
        }
        String raw = safe(argExprs.get(index));
        if (raw.startsWith("$")) {
            String key = raw.substring(1).trim();
            if (params == null) {
                return null;
            }
            return params.get(key);
        }
        if ((raw.startsWith("\"") && raw.endsWith("\"")) || (raw.startsWith("'") && raw.endsWith("'"))) {
            return raw.substring(1, raw.length() - 1);
        }
        if ("true".equalsIgnoreCase(raw) || "false".equalsIgnoreCase(raw)) {
            return Boolean.parseBoolean(raw);
        }
        try {
            return Long.parseLong(raw);
        } catch (Exception ignored) {
            return raw;
        }
    }

    private static long resolveNodeRef(Object value, GraphSnapshot snapshot) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        String raw = toString(value);
        if (raw.isEmpty()) {
            return -1L;
        }
        if (raw.startsWith("node:")) {
            try {
                return Long.parseLong(raw.substring("node:".length()));
            } catch (Exception ignored) {
                return -1L;
            }
        }
        if (raw.contains("#")) {
            String[] parts = raw.split("#", 4);
            if (parts.length >= 3) {
                String clazz = normalizeClass(parts[0]);
                String method = parts[1].trim();
                String desc = parts[2].trim();
                Integer jarId = null;
                if (parts.length == 4) {
                    jarId = toNullableInt(parts[3]);
                }
                long nodeId = snapshot.findMethodNodeId(clazz, method, desc, jarId);
                if (nodeId > 0L) {
                    return nodeId;
                }
            }
        }
        return -1L;
    }

    private static long resolveMethodNodeRef(GraphSnapshot snapshot,
                                             String className,
                                             String methodName,
                                             String methodDesc) {
        if (snapshot == null) {
            return -1L;
        }
        String clazz = normalizeClass(className);
        String name = safe(methodName);
        String desc = normalizeDesc(methodDesc);
        if (clazz.isEmpty() || name.isEmpty() || desc.isEmpty()) {
            return -1L;
        }
        if (!"*".equals(desc)) {
            return snapshot.findMethodNodeId(clazz, name, desc, null);
        }
        long matched = -1L;
        for (GraphNode node : snapshot.getNodesByClassNameView(clazz)) {
            if (!GraphTraversalRules.isMethodNode(node)) {
                continue;
            }
            if (!name.equals(node.getMethodName())) {
                continue;
            }
            if (matched > 0L) {
                return -1L;
            }
            matched = node.getNodeId();
        }
        return matched;
    }

    private static List<Long> resolveGadgetSourceNodes(GraphSnapshot snapshot) {
        if (snapshot == null) {
            return List.of();
        }
        List<Long> out = new ArrayList<>();
        for (GraphNode node : snapshot.getNodesView()) {
            if (!GraphTraversalRules.isMethodNode(node)) {
                continue;
            }
            if (!node.hasMethodSemanticFlag(MethodSemanticFlags.DESERIALIZATION_CALLBACK)) {
                continue;
            }
            out.add(node.getNodeId());
        }
        out.sort(Long::compareTo);
        return List.copyOf(out);
    }

    private static int toInt(Object value, int def) {
        if (value == null) {
            return def;
        }
        if (value instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (Exception ignored) {
            return def;
        }
    }

    private static Integer toNullableInt(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean toBoolean(Object value, boolean def) {
        if (value == null) {
            return def;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        String raw = toString(value);
        if (raw.isEmpty()) {
            return def;
        }
        if ("true".equalsIgnoreCase(raw)) {
            return true;
        }
        if ("false".equalsIgnoreCase(raw)) {
            return false;
        }
        return def;
    }

    private static String toString(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static SearchDirection resolveTaintDirection(String mode, String direction) {
        String explicit = normalizeProcedureString(direction);
        if (!explicit.isEmpty()) {
            return SearchDirection.parse(explicit, SearchDirection.FORWARD);
        }
        String value = normalizeProcedureString(mode).toLowerCase(Locale.ROOT);
        if (value.isEmpty() || "source".equals(value) || "fromsource".equals(value)) {
            return SearchDirection.FORWARD;
        }
        if ("sink".equals(value) || "fromsink".equals(value)) {
            return SearchDirection.BACKWARD;
        }
        throw new IllegalArgumentException("invalid_request");
    }

    private static String normalizeProcedureString(String value) {
        String normalized = safe(value);
        if (normalized.length() >= 2) {
            char first = normalized.charAt(0);
            char last = normalized.charAt(normalized.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return normalized.substring(1, normalized.length() - 1).trim();
            }
        }
        return normalized;
    }

    private static String normalizeClass(String value) {
        return safe(value).replace('.', '/');
    }

    private static String normalizeDesc(String value) {
        String v = safe(value);
        if (v.equalsIgnoreCase("null")) {
            return "*";
        }
        return v;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static boolean isTraversable(GraphSnapshot snapshot, GraphEdge edge, TraversalMode traversalMode) {
        return GraphTraversalRules.isMethodFlowEdge(snapshot, edge, traversalMode != null && traversalMode.includesAlias());
    }

    private static void checkBudget(QueryBudget budget) {
        if (budget != null) {
            budget.checkpoint();
        }
    }

    private static void ensureWithinBudget(QueryBudget budget) {
        if (budget != null) {
            budget.ensureWithinDeadline();
        }
    }

    private static void throwTimeout() {
        throw new IllegalArgumentException("cypher_query_timeout");
    }

    private static void throwExpandBudgetExceeded() {
        throw new IllegalArgumentException("cypher_expand_budget_exceeded");
    }

    private static void throwPathBudgetExceeded() {
        throw new IllegalArgumentException("cypher_path_budget_exceeded");
    }

    private static final class DfsFrame {
        private final long nodeId;
        private final List<GraphEdge> outgoing;
        private int cursor;

        private DfsFrame(long nodeId, List<GraphEdge> outgoing) {
            this.nodeId = nodeId;
            this.outgoing = outgoing == null ? List.of() : outgoing;
            this.cursor = 0;
        }
    }

    private static final class QueryBudget {
        private static final int DEFAULT_INTERVAL = 128;
        private final long deadlineNs;
        private final long expandBudget;
        private final long pathBudget;
        private final int checkInterval;
        private long expansions;
        private long paths;
        private int ticks;

        private QueryBudget(long deadlineNs, long expandBudget, long pathBudget, int checkInterval) {
            this.deadlineNs = deadlineNs;
            this.expandBudget = expandBudget;
            this.pathBudget = pathBudget;
            this.checkInterval = Math.max(8, checkInterval);
            this.expansions = 0L;
            this.paths = 0L;
            this.ticks = 0;
        }

        private static QueryBudget of(QueryOptions options, int maxHopsHint, int maxPathsHint, Integer timeoutMsHint) {
            QueryOptions effective = options == null ? QueryOptions.defaults() : options;
            long deadlineNs = resolveDeadlineNs(effective.effectiveBudgetMaxMs(maxHopsHint, timeoutMsHint));
            return new QueryBudget(
                    deadlineNs,
                    Math.max(1L, effective.effectiveExpandBudget(maxHopsHint)),
                    Math.max(1L, effective.effectivePathBudget(maxHopsHint, maxPathsHint)),
                    Math.max(DEFAULT_INTERVAL, effective.getTimeoutCheckInterval())
            );
        }

        private static long resolveDeadlineNs(int maxMs) {
            if (maxMs <= 0) {
                return Long.MAX_VALUE;
            }
            long now = System.nanoTime();
            long budgetNs = Math.max(1L, (long) maxMs) * 1_000_000L;
            long end = now + budgetNs;
            if (end < 0L) {
                end = Long.MAX_VALUE;
            }
            return end;
        }

        private void onExpand() {
            expansions++;
            if (expansions > expandBudget) {
                throwExpandBudgetExceeded();
            }
            if (checkInterval <= 1 || (expansions % checkInterval) == 0) {
                ensureWithinDeadline();
            }
        }

        private void onPath() {
            paths++;
            if (paths > pathBudget) {
                throwPathBudgetExceeded();
            }
            if (checkInterval <= 1 || (paths % checkInterval) == 0) {
                ensureWithinDeadline();
            }
        }

        private void checkpoint() {
            ticks++;
            if ((ticks & 0xFF) != 0) {
                return;
            }
            ensureWithinDeadline();
        }

        private int remainingMsOrDefault(int fallbackMs) {
            if (deadlineNs == Long.MAX_VALUE) {
                return fallbackMs;
            }
            long now = System.nanoTime();
            long remainNs = deadlineNs - now;
            if (remainNs <= 0L) {
                throwTimeout();
            }
            long remainMs = Math.max(1L, remainNs / 1_000_000L);
            if (remainMs > Integer.MAX_VALUE) {
                return fallbackMs;
            }
            return (int) Math.min(Math.max(1, fallbackMs), remainMs);
        }

        private void ensureWithinDeadline() {
            if (System.nanoTime() > deadlineNs) {
                throwTimeout();
            }
        }
    }

    private static final class BudgetedPrunedController implements GraphPrunedPathEngine.Controller {
        private final QueryBudget budget;

        private BudgetedPrunedController(QueryBudget budget) {
            this.budget = budget;
        }

        @Override
        public void onNode() {
        }

        @Override
        public void onExpand() {
            if (budget != null) {
                budget.onExpand();
            }
        }

        @Override
        public void onPath() {
            if (budget != null) {
                budget.onPath();
            }
        }

        @Override
        public void checkpoint() {
            if (budget != null) {
                budget.checkpoint();
            }
        }

        @Override
        public boolean shouldStop(int emittedPaths) {
            checkpoint();
            return false;
        }
    }

    private static final class BudgetedGadgetController implements GraphGadgetEngine.Controller {
        private final QueryBudget budget;

        private BudgetedGadgetController(QueryBudget budget) {
            this.budget = budget;
        }

        @Override
        public void onNode() {
        }

        @Override
        public void onExpand() {
            if (budget != null) {
                budget.onExpand();
            }
        }

        @Override
        public void onPath() {
            if (budget != null) {
                budget.onPath();
            }
        }

        @Override
        public void checkpoint() {
            if (budget != null) {
                budget.checkpoint();
            }
        }

        @Override
        public boolean shouldStop(int emittedPaths) {
            checkpoint();
            return false;
        }
    }

    private static final class Path {
        private final List<Long> nodeIds;
        private final List<Long> edgeIds;
        private final int unresolvedCount;

        private Path(List<Long> nodeIds, List<Long> edgeIds) {
            this(nodeIds, edgeIds, 0);
        }

        private Path(List<Long> nodeIds, List<Long> edgeIds, int unresolvedCount) {
            this.nodeIds = nodeIds;
            this.edgeIds = edgeIds;
            this.unresolvedCount = Math.max(0, unresolvedCount);
        }
    }

    private record PathEnumeration(List<Path> paths, boolean truncated) {
        private PathEnumeration {
            paths = paths == null ? List.of() : List.copyOf(paths);
        }

        private static PathEnumeration empty() {
            return new PathEnumeration(List.of(), false);
        }
    }

    private record PrunedRunSelection(List<GraphPrunedPathEngine.Candidate> candidates,
                                      GraphPrunedPathEngine.Stats stats,
                                      boolean truncated) {
        private PrunedRunSelection {
            candidates = candidates == null ? List.of() : List.copyOf(candidates);
            stats = stats == null ? new GraphPrunedPathEngine.Stats(0, 0, 0, 0, 0, 0L) : stats;
        }

        private GraphPrunedPathEngine.Candidate candidate() {
            return candidates.isEmpty() ? null : candidates.get(0);
        }
    }

    private record GadgetRunSelection(List<GraphGadgetEngine.Candidate> candidates,
                                      GraphGadgetEngine.Stats stats,
                                      boolean truncated) {
        private GadgetRunSelection {
            candidates = candidates == null ? List.of() : List.copyOf(candidates);
            stats = stats == null ? new GraphGadgetEngine.Stats(0, 0, 0, 0, 0, 0L, Map.of()) : stats;
        }
    }

    private record PrevStep(long prevNode, long viaEdgeId) {
    }

    private record NextStep(long nextNode, long viaEdgeId) {
    }

    private record MeetCandidate(long meetNode, int distance) {
        private static MeetCandidate none() {
            return new MeetCandidate(-1L, Integer.MAX_VALUE);
        }
    }

    private record ScoredEdge(GraphEdge edge, int score) {
    }
}
