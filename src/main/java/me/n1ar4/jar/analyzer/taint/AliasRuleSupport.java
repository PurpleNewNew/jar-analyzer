/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.taint;

import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
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

public final class AliasRuleSupport {
    private AliasRuleSupport() {
    }

    public static List<AliasEdge> resolveAliasEdges(
            Map<MethodReference.Handle, ? extends Set<MethodReference.Handle>> methodCalls,
            Collection<ClassReference> classReferences) {
        if (methodCalls == null || methodCalls.isEmpty()) {
            return List.of();
        }
        TaintPropagationConfig config = TaintPropagationConfig.resolve();
        TaintModelRule summaryRule = config == null ? null : config.getSummaryRule();
        TaintModelRule additionalRule = config == null ? null : config.getAdditionalRule();
        if (isEmpty(summaryRule) && isEmpty(additionalRule)) {
            return List.of();
        }

        HierarchyIndex hierarchy = new HierarchyIndex(classReferences);
        Map<MethodReference.Handle, AliasSemantics> semanticsCache = new HashMap<>();
        List<MethodReference.Handle> callers = new ArrayList<>(methodCalls.keySet());
        callers.sort(METHOD_HANDLE_COMPARATOR);

        List<AliasEdge> out = new ArrayList<>();
        for (MethodReference.Handle caller : callers) {
            if (caller == null) {
                continue;
            }
            Collection<MethodReference.Handle> rawCallees = methodCalls.get(caller);
            if (rawCallees == null || rawCallees.isEmpty()) {
                continue;
            }
            List<MethodReference.Handle> callees = new ArrayList<>(rawCallees);
            callees.sort(METHOD_HANDLE_COMPARATOR);
            for (MethodReference.Handle callee : callees) {
                if (callee == null) {
                    continue;
                }
                AliasSemantics semantics = semanticsCache.computeIfAbsent(
                        callee,
                        handle -> resolveSemantics(handle, summaryRule, additionalRule, hierarchy)
                );
                if (semantics == null || semantics.aliasKinds().isEmpty()) {
                    continue;
                }
                out.add(new AliasEdge(caller, callee, semantics.aliasKindText(), semantics.confidence(), semantics.evidence()));
            }
        }
        return out.isEmpty() ? List.of() : List.copyOf(out);
    }

    static AliasSemantics resolveSemantics(MethodReference.Handle callee,
                                           TaintModelRule summaryRule,
                                           TaintModelRule additionalRule,
                                           Collection<ClassReference> classReferences) {
        return resolveSemantics(callee, summaryRule, additionalRule, new HierarchyIndex(classReferences));
    }

    private static AliasSemantics resolveSemantics(MethodReference.Handle callee,
                                                   TaintModelRule summaryRule,
                                                   TaintModelRule additionalRule,
                                                   HierarchyIndex hierarchy) {
        if (callee == null || callee.getClassReference() == null) {
            return AliasSemantics.empty();
        }
        String owner = safe(callee.getClassReference().getName()).replace('.', '/');
        String name = safe(callee.getName());
        String desc = safe(callee.getDesc());
        if (owner.isBlank() || name.isBlank() || desc.isBlank()) {
            return AliasSemantics.empty();
        }
        List<String> candidateClasses = hierarchy.resolveCandidateClasses(owner, callee.getJarId());
        if (candidateClasses.isEmpty()) {
            candidateClasses = List.of(owner);
        }

        LinkedHashSet<String> aliasKinds = new LinkedHashSet<>();
        LinkedHashSet<String> evidences = new LinkedHashSet<>();
        int confidenceScore = 0;

        for (String candidate : candidateClasses) {
            boolean subtypeMatch = !owner.equals(candidate);
            if (!isEmpty(summaryRule)) {
                List<TaintModel> models = summaryRule.getRules(candidate, name, desc);
                confidenceScore = Math.max(confidenceScore,
                        collectAliasKinds(aliasKinds, evidences, models, owner, candidate, false, subtypeMatch));
            }
            if (!isEmpty(additionalRule)) {
                List<TaintModel> models = additionalRule.getRules(candidate, name, desc);
                confidenceScore = Math.max(confidenceScore,
                        collectAliasKinds(aliasKinds, evidences, models, owner, candidate, true, subtypeMatch));
            }
        }
        if (aliasKinds.isEmpty()) {
            return AliasSemantics.empty();
        }
        return new AliasSemantics(
                List.copyOf(aliasKinds),
                confidenceScore >= 3 ? "high" : "medium",
                String.join("|", evidences)
        );
    }

    private static int collectAliasKinds(Set<String> aliasKinds,
                                         Set<String> evidences,
                                         List<TaintModel> models,
                                         String owner,
                                         String candidate,
                                         boolean additional,
                                         boolean subtypeMatch) {
        if (models == null || models.isEmpty()) {
            return 0;
        }
        int confidence = 0;
        for (TaintModel model : models) {
            if (!shouldApplyRule(model, owner, candidate)) {
                continue;
            }
            List<TaintFlow> flows = model == null ? null : model.getFlows();
            if (flows == null || flows.isEmpty()) {
                continue;
            }
            for (TaintFlow flow : flows) {
                FlowEndpoint from = parseEndpoint(flow == null ? null : flow.getFrom());
                FlowEndpoint to = parseEndpoint(flow == null ? null : flow.getTo());
                String kind = classifyAliasKind(from, to, additional);
                if (kind == null) {
                    continue;
                }
                aliasKinds.add(kind);
                evidences.add(buildEvidence(additional, subtypeMatch, kind));
                confidence = Math.max(confidence, additional || subtypeMatch ? 2 : 3);
            }
        }
        return confidence;
    }

    private static boolean shouldApplyRule(TaintModel rule, String owner, String candidate) {
        if (rule == null || owner == null || candidate == null) {
            return false;
        }
        if (owner.equals(candidate)) {
            return true;
        }
        return Boolean.TRUE.equals(rule.getSubtypes());
    }

    private static String classifyAliasKind(FlowEndpoint from, FlowEndpoint to, boolean additional) {
        if (from == null || to == null) {
            return null;
        }
        if (from.kind() == FlowKind.ARG && to.kind() == FlowKind.ARG) {
            return "arg_to_arg";
        }
        if (from.kind() == FlowKind.THIS && to.kind() == FlowKind.ARG) {
            return "this_to_arg";
        }
        if (from.kind() == FlowKind.ARG && to.kind() == FlowKind.THIS) {
            return "arg_to_this";
        }
        if (!additional) {
            return null;
        }
        if (from.kind() == FlowKind.ARG && to.kind() == FlowKind.RETURN) {
            return "container_wrapper";
        }
        if (from.kind() == FlowKind.THIS && to.kind() == FlowKind.RETURN) {
            return "builder";
        }
        return null;
    }

    private static String buildEvidence(boolean additional, boolean subtypeMatch, String kind) {
        String prefix = additional ? "additional_flow" : "summary_flow";
        if (subtypeMatch) {
            return prefix + ":subtypes:" + kind;
        }
        return prefix + ":" + kind;
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
            int index = extractIndex(lower);
            if (index >= 0) {
                return new FlowEndpoint(FlowKind.ARG, index);
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

    private static boolean isEmpty(TaintModelRule rule) {
        return rule == null || rule.getRules() == null || rule.getRules().isEmpty();
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public record AliasEdge(MethodReference.Handle source,
                            MethodReference.Handle target,
                            String aliasKind,
                            String confidence,
                            String evidence) {
        public AliasEdge {
            aliasKind = safe(aliasKind);
            confidence = safe(confidence);
            evidence = safe(evidence);
        }
    }

    static final class AliasSemantics {
        private static final AliasSemantics EMPTY = new AliasSemantics(List.of(), "", "");
        private final List<String> aliasKinds;
        private final String confidence;
        private final String evidence;

        private AliasSemantics(List<String> aliasKinds, String confidence, String evidence) {
            this.aliasKinds = aliasKinds == null ? List.of() : List.copyOf(aliasKinds);
            this.confidence = safe(confidence);
            this.evidence = safe(evidence);
        }

        static AliasSemantics empty() {
            return EMPTY;
        }

        List<String> aliasKinds() {
            return aliasKinds;
        }

        String aliasKindText() {
            return String.join(",", aliasKinds);
        }

        String confidence() {
            return confidence;
        }

        String evidence() {
            return evidence;
        }
    }

    private enum FlowKind {
        ARG,
        THIS,
        RETURN
    }

    private record FlowEndpoint(FlowKind kind, int index) {
    }

    private static final class HierarchyIndex {
        private final Map<ClassKey, ClassReference> exact = new LinkedHashMap<>();
        private final Map<String, ClassReference> loose = new HashMap<>();
        private final Set<String> ambiguous = new HashSet<>();

        private HierarchyIndex(Collection<ClassReference> classReferences) {
            if (classReferences == null || classReferences.isEmpty()) {
                return;
            }
            for (ClassReference classReference : classReferences) {
                if (classReference == null) {
                    continue;
                }
                ClassKey key = new ClassKey(safe(classReference.getName()), normalizeJarId(classReference.getJarId()));
                exact.putIfAbsent(key, classReference);
                registerLoose(classReference);
            }
        }

        private void registerLoose(ClassReference classReference) {
            String name = safe(classReference == null ? null : classReference.getName());
            if (name.isBlank()) {
                return;
            }
            if (ambiguous.contains(name)) {
                return;
            }
            ClassReference existing = loose.putIfAbsent(name, classReference);
            if (existing != null && existing != classReference) {
                loose.remove(name);
                ambiguous.add(name);
            }
        }

        private List<String> resolveCandidateClasses(String owner, Integer jarId) {
            String normalized = safe(owner).replace('.', '/');
            if (normalized.isBlank()) {
                return List.of();
            }
            LinkedHashSet<String> out = new LinkedHashSet<>();
            ArrayDeque<ClassReference> queue = new ArrayDeque<>();
            ClassReference root = resolve(normalized, jarId);
            out.add(normalized);
            if (root != null) {
                queue.add(root);
            }
            while (!queue.isEmpty()) {
                ClassReference current = queue.poll();
                String superClass = safe(current.getSuperClass());
                if (!superClass.isBlank() && out.add(superClass)) {
                    ClassReference resolved = resolve(superClass, current.getJarId());
                    if (resolved != null) {
                        queue.add(resolved);
                    }
                }
                List<String> interfaces = current.getInterfaces();
                if (interfaces == null || interfaces.isEmpty()) {
                    continue;
                }
                for (String interfaceName : interfaces) {
                    String value = safe(interfaceName);
                    if (value.isBlank() || !out.add(value)) {
                        continue;
                    }
                    ClassReference resolved = resolve(value, current.getJarId());
                    if (resolved != null) {
                        queue.add(resolved);
                    }
                }
            }
            return new ArrayList<>(out);
        }

        private ClassReference resolve(String className, Integer jarId) {
            String normalized = safe(className).replace('.', '/');
            if (normalized.isBlank()) {
                return null;
            }
            ClassReference exactHit = exact.get(new ClassKey(normalized, normalizeJarId(jarId)));
            if (exactHit != null) {
                return exactHit;
            }
            if (ambiguous.contains(normalized)) {
                return null;
            }
            return loose.get(normalized);
        }
    }

    private record ClassKey(String className, int jarId) {
    }

    private static int normalizeJarId(Integer jarId) {
        return jarId == null || jarId < 0 ? -1 : jarId;
    }

    private static final Comparator<MethodReference.Handle> METHOD_HANDLE_COMPARATOR = Comparator
            .comparing((MethodReference.Handle handle) -> safe(handle == null || handle.getClassReference() == null ? null : handle.getClassReference().getName()))
            .thenComparing(handle -> safe(handle == null ? null : handle.getName()))
            .thenComparing(handle -> safe(handle == null ? null : handle.getDesc()))
            .thenComparingInt(handle -> normalizeJarId(handle == null ? null : handle.getJarId()));
}
