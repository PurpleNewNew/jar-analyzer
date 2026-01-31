/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.taint;

import me.n1ar4.jar.analyzer.core.AnalyzeEnv;
import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.taint.TaintModelRule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class TaintSemanticSummary {
    private static final String CACHE_TYPE_RETURN_FLOW = "TAINT_RETURN_FLOW";
    private static final String CACHE_TYPE_CALL_GATE = "TAINT_CALL_GATE";
    private static final int CALL_GATE_MEMORY_MAX = 2048;
    private static final Map<String, CallGateDecision> CALL_GATE_MEMORY =
            Collections.synchronizedMap(new LinkedHashMap<String, CallGateDecision>(CALL_GATE_MEMORY_MAX, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, CallGateDecision> eldest) {
                    return size() > CALL_GATE_MEMORY_MAX;
                }
            });

    private TaintSemanticSummary() {
    }

    public static ReturnFlowSummary resolveReturnFlow(TaintModelRule summaryRule,
                                                      String owner,
                                                      String name,
                                                      String desc) {
        return resolveReturnFlow(summaryRule, owner, name, desc, null);
    }

    public static ReturnFlowSummary resolveReturnFlow(TaintModelRule summaryRule,
                                                      String owner,
                                                      String name,
                                                      String desc,
                                                      Integer jarId) {
        if (summaryRule == null || owner == null || name == null || desc == null) {
            return ReturnFlowSummary.unknown();
        }
        String cacheKey = buildCacheKey(jarId, owner, name, desc);
        String cached = DatabaseManager.getSemanticCacheValue(cacheKey, CACHE_TYPE_RETURN_FLOW);
        if (cached != null) {
            ReturnFlowSummary parsed = ReturnFlowSummary.fromCache(cached);
            if (parsed != null) {
                return parsed;
            }
        }
        ReturnFlowSummary summary = computeReturnFlow(summaryRule, owner, name, desc);
        DatabaseManager.putSemanticCacheValue(cacheKey, CACHE_TYPE_RETURN_FLOW, summary.toCacheValue());
        return summary;
    }

    public static CallGateDecision resolveCallGate(TaintModelRule summaryRule,
                                                   String owner,
                                                   String name,
                                                   String desc,
                                                   Integer jarId,
                                                   Set<Integer> taintedParams) {
        if (summaryRule == null || owner == null || name == null || desc == null) {
            return CallGateDecision.unknown();
        }
        if (taintedParams == null || taintedParams.isEmpty()) {
            return CallGateDecision.unknown();
        }
        String paramsKey = encodeParams(taintedParams);
        String cacheKey = buildCacheKey(jarId, owner, name, desc) + "|params=" + paramsKey;
        CallGateDecision memory = CALL_GATE_MEMORY.get(cacheKey);
        if (memory != null) {
            return memory;
        }
        String cached = DatabaseManager.getSemanticCacheValue(cacheKey, CACHE_TYPE_CALL_GATE);
        if (cached != null) {
            CallGateDecision parsed = CallGateDecision.fromCache(cached);
            if (parsed != null) {
                CALL_GATE_MEMORY.put(cacheKey, parsed);
                return parsed;
            }
        }
        ReturnFlowSummary summary = resolveReturnFlow(summaryRule, owner, name, desc, jarId);
        CallGateDecision decision;
        if (!summary.isKnown()) {
            decision = CallGateDecision.unknown();
        } else if (summary.allowsAny(taintedParams)) {
            decision = CallGateDecision.allowed();
        } else {
            decision = CallGateDecision.denied();
        }
        DatabaseManager.putSemanticCacheValue(cacheKey, CACHE_TYPE_CALL_GATE, decision.toCacheValue());
        CALL_GATE_MEMORY.put(cacheKey, decision);
        return decision;
    }

    private static ReturnFlowSummary computeReturnFlow(TaintModelRule summaryRule,
                                                       String owner,
                                                       String name,
                                                       String desc) {
        boolean matched = false;
        Set<Integer> sources = new LinkedHashSet<>();
        for (String candidate : resolveCandidateClasses(owner)) {
            List<TaintModel> rules = summaryRule.getRules(candidate, name, desc);
            if (rules == null || rules.isEmpty()) {
                continue;
            }
            matched = true;
            for (TaintModel rule : rules) {
                if (rule == null || rule.getFlows() == null) {
                    continue;
                }
                if (!shouldApplyRule(rule, owner, candidate)) {
                    continue;
                }
                for (TaintFlow flow : rule.getFlows()) {
                    if (flow == null) {
                        continue;
                    }
                    FlowEndpoint from = parseEndpoint(flow.getFrom());
                    FlowEndpoint to = parseEndpoint(flow.getTo());
                    if (from == null || to == null) {
                        continue;
                    }
                    if (to.kind != FlowKind.RETURN) {
                        continue;
                    }
                    if (from.kind == FlowKind.THIS) {
                        sources.add(Sanitizer.THIS_PARAM);
                    } else if (from.kind == FlowKind.ARG && from.index >= 0) {
                        sources.add(from.index);
                    }
                }
            }
        }
        return new ReturnFlowSummary(matched, sources);
    }

    private static boolean shouldApplyRule(TaintModel rule, String owner, String candidate) {
        if (rule == null || owner == null || candidate == null) {
            return false;
        }
        if (owner.equals(candidate)) {
            return true;
        }
        Boolean subtypes = rule.getSubtypes();
        return subtypes != null && subtypes;
    }

    private static List<String> resolveCandidateClasses(String owner) {
        if (owner == null || owner.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> out = new ArrayList<>();
        out.add(owner);
        if (AnalyzeEnv.inheritanceMap != null) {
            Set<ClassReference.Handle> parents = AnalyzeEnv.inheritanceMap.getSuperClasses(new ClassReference.Handle(owner));
            if (parents != null) {
                for (ClassReference.Handle parent : parents) {
                    if (parent != null && parent.getName() != null) {
                        out.add(parent.getName());
                    }
                }
            }
        }
        return out;
    }

    private static String buildCacheKey(Integer jarId, String owner, String name, String desc) {
        StringBuilder sb = new StringBuilder();
        if (jarId != null) {
            sb.append(jarId).append(':');
        }
        sb.append(owner).append("#").append(name).append("#").append(desc);
        return sb.toString();
    }

    private static String encodeParams(Set<Integer> params) {
        if (params == null || params.isEmpty()) {
            return "";
        }
        List<Integer> list = new ArrayList<>(params);
        Collections.sort(list);
        StringBuilder sb = new StringBuilder();
        for (Integer idx : list) {
            if (idx == null) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(idx);
        }
        return sb.toString();
    }

    private static FlowEndpoint parseEndpoint(String raw) {
        if (raw == null) {
            return null;
        }
        String text = raw.trim();
        if (text.isEmpty()) {
            return null;
        }
        String base = text;
        int dot = text.indexOf('.');
        if (dot >= 0) {
            base = text.substring(0, dot);
        }
        String lower = base.trim().toLowerCase(Locale.ROOT);
        if (lower.isEmpty()) {
            return null;
        }
        if ("this".equals(lower) || "receiver".equals(lower)) {
            return new FlowEndpoint(FlowKind.THIS, -1);
        }
        if ("return".equals(lower) || "ret".equals(lower) || "returnvalue".equals(lower)) {
            return new FlowEndpoint(FlowKind.RETURN, -1);
        }
        if (lower.startsWith("arg") || lower.startsWith("param") || lower.startsWith("p")) {
            int idx = extractIndex(lower);
            if (idx >= 0) {
                return new FlowEndpoint(FlowKind.ARG, idx);
            }
        }
        return null;
    }

    private static int extractIndex(String text) {
        int i = 0;
        while (i < text.length() && !Character.isDigit(text.charAt(i))) {
            i++;
        }
        if (i >= text.length()) {
            return -1;
        }
        try {
            return Integer.parseInt(text.substring(i));
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    private enum FlowKind {
        ARG,
        THIS,
        RETURN
    }

    private static final class FlowEndpoint {
        private final FlowKind kind;
        private final int index;

        private FlowEndpoint(FlowKind kind, int index) {
            this.kind = kind;
            this.index = index;
        }
    }

    public static final class CallGateDecision {
        private final boolean known;
        private final boolean allowed;

        private CallGateDecision(boolean known, boolean allowed) {
            this.known = known;
            this.allowed = allowed;
        }

        public static CallGateDecision unknown() {
            return new CallGateDecision(false, false);
        }

        public static CallGateDecision allowed() {
            return new CallGateDecision(true, true);
        }

        public static CallGateDecision denied() {
            return new CallGateDecision(true, false);
        }

        public boolean isKnown() {
            return known;
        }

        public boolean isAllowed() {
            return known && allowed;
        }

        private String toCacheValue() {
            return (known ? "1" : "0") + ":" + (allowed ? "1" : "0");
        }

        private static CallGateDecision fromCache(String raw) {
            if (raw == null || raw.isEmpty()) {
                return null;
            }
            int sep = raw.indexOf(':');
            if (sep < 0) {
                return null;
            }
            boolean known = "1".equals(raw.substring(0, sep));
            boolean allowed = "1".equals(raw.substring(sep + 1));
            return new CallGateDecision(known, allowed);
        }
    }

    public static final class ReturnFlowSummary {
        private final boolean known;
        private final Set<Integer> sources;

        private ReturnFlowSummary(boolean known, Set<Integer> sources) {
            this.known = known;
            this.sources = sources == null ? Collections.emptySet() : Collections.unmodifiableSet(sources);
        }

        public static ReturnFlowSummary unknown() {
            return new ReturnFlowSummary(false, Collections.emptySet());
        }

        public boolean isKnown() {
            return known;
        }

        public boolean allowsAny(Set<Integer> params) {
            if (!known || params == null || params.isEmpty()) {
                return false;
            }
            for (Integer param : params) {
                if (param != null && sources.contains(param)) {
                    return true;
                }
            }
            return false;
        }

        private String toCacheValue() {
            StringBuilder sb = new StringBuilder();
            sb.append(known ? "1" : "0").append(':');
            boolean first = true;
            for (Integer idx : sources) {
                if (idx == null) {
                    continue;
                }
                if (!first) {
                    sb.append(',');
                }
                sb.append(idx);
                first = false;
            }
            return sb.toString();
        }

        private static ReturnFlowSummary fromCache(String raw) {
            if (raw == null || raw.isEmpty()) {
                return null;
            }
            int sep = raw.indexOf(':');
            if (sep < 0) {
                return null;
            }
            boolean known = "1".equals(raw.substring(0, sep));
            Set<Integer> sources = new LinkedHashSet<>();
            String body = raw.substring(sep + 1);
            if (!body.isEmpty()) {
                String[] parts = body.split(",");
                for (String part : parts) {
                    try {
                        sources.add(Integer.parseInt(part));
                    } catch (NumberFormatException ex) {
                        // ignore
                    }
                }
            }
            return new ReturnFlowSummary(known, sources);
        }
    }
}
