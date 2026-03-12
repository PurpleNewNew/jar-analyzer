/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.graph.flow;

import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.dfs.DFSEdge;
import me.n1ar4.jar.analyzer.dfs.DFSResult;
import me.n1ar4.jar.analyzer.graph.store.GraphEdge;
import me.n1ar4.jar.analyzer.graph.store.GraphNode;
import me.n1ar4.jar.analyzer.graph.store.GraphTraversalRules;
import me.n1ar4.jar.analyzer.rules.ModelRegistry;
import me.n1ar4.jar.analyzer.rules.PruningPolicy;
import me.n1ar4.jar.analyzer.rules.SinkRuleRegistry;
import me.n1ar4.jar.analyzer.taint.Sanitizer;
import me.n1ar4.jar.analyzer.taint.SanitizerRule;
import me.n1ar4.jar.analyzer.taint.SinkKindResolver;
import me.n1ar4.jar.analyzer.taint.TaintMarkers;
import me.n1ar4.jar.analyzer.taint.summary.CallFlow;
import me.n1ar4.jar.analyzer.taint.summary.FlowPort;
import me.n1ar4.jar.analyzer.taint.summary.MethodSummary;
import me.n1ar4.jar.analyzer.taint.summary.SummaryEngine;
import org.objectweb.asm.Type;

import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

final class GraphTaintSemantics {
    private final SummaryEngine summaryEngine;
    private volatile SanitizerRule sanitizerRule = new SanitizerRule();
    private volatile PruningPolicy pruningPolicy = PruningPolicy.defaults();

    GraphTaintSemantics() {
        this(new SummaryEngine());
    }

    GraphTaintSemantics(SummaryEngine summaryEngine) {
        this.summaryEngine = summaryEngine == null ? new SummaryEngine() : summaryEngine;
        refreshRuleContext();
    }

    void refreshRuleContext() {
        SinkRuleRegistry.checkNow();
        ModelRegistry.checkNow();
        sanitizerRule = ModelRegistry.getBarrierRule();
        pruningPolicy = ModelRegistry.getPruningPolicy();
    }

    String resolveSinkKind(MethodReference.Handle sink) {
        SinkKindResolver.Result resolved = SinkKindResolver.resolve(sink);
        return safe(resolved == null ? null : resolved.getKind());
    }

    PortState initialState(MethodReference.Handle method) {
        return allInputPorts(method, false);
    }

    Transition transition(MethodReference.Handle from,
                          MethodReference.Handle to,
                          PortState state,
                          GraphEdge edge,
                          String sinkKind,
                          PruningPolicy.Resolved policy) {
        String relType = edge == null ? "" : edge.getRelType();
        String confidence = edge == null ? "" : edge.getConfidence();
        return transitionInternal(from, to, state, relType, confidence, sinkKind, effectivePolicy(policy));
    }

    Transition transition(MethodReference.Handle from,
                          MethodReference.Handle to,
                          PortState state,
                          DFSEdge edge,
                          String sinkKind,
                          PruningPolicy.Resolved policy) {
        String relType = edge == null ? "" : edge.getType();
        String confidence = edge == null ? "" : edge.getConfidence();
        return transitionInternal(from, to, state, relType, confidence, sinkKind, effectivePolicy(policy));
    }

    Transition reverseTransition(MethodReference.Handle from,
                                 MethodReference.Handle to,
                                 PortState state,
                                 GraphEdge edge,
                                 String sinkKind,
                                 PruningPolicy.Resolved policy) {
        String relType = edge == null ? "" : edge.getRelType();
        String confidence = edge == null ? "" : edge.getConfidence();
        return reverseTransitionInternal(from, to, state, relType, confidence, sinkKind, effectivePolicy(policy));
    }

    Transition reverseTransition(MethodReference.Handle from,
                                 MethodReference.Handle to,
                                 PortState state,
                                 DFSEdge edge,
                                 String sinkKind,
                                 PruningPolicy.Resolved policy) {
        String relType = edge == null ? "" : edge.getType();
        String confidence = edge == null ? "" : edge.getConfidence();
        return reverseTransitionInternal(from, to, state, relType, confidence, sinkKind, effectivePolicy(policy));
    }

    MethodReference.Handle toHandle(GraphNode node) {
        if (!GraphTraversalRules.isMethodNode(node)) {
            return null;
        }
        String clazz = safe(node.getClassName()).replace('.', '/');
        String method = safe(node.getMethodName());
        String desc = safe(node.getMethodDesc());
        if (clazz.isBlank() || method.isBlank() || desc.isBlank()) {
            return null;
        }
        return new MethodReference.Handle(new ClassReference.Handle(clazz, node.getJarId()), method, desc);
    }


    PruningPolicy pruningPolicy() {
        PruningPolicy current = pruningPolicy;
        return current == null ? PruningPolicy.defaults() : current;
    }

    PruningPolicy.Resolved resolvePolicy(String sinkKind,
                                         ModelRegistry.SinkDescriptor sinkDescriptor,
                                         int dfsMode) {
        boolean fromSink = dfsMode == DFSResult.FROM_SINK_TO_SOURCE || dfsMode == DFSResult.FROM_SOURCE_TO_ALL;
        boolean searchAllSources = dfsMode == DFSResult.FROM_SOURCE_TO_ALL;
        return resolvePolicy(sinkKind, sinkDescriptor, fromSink, searchAllSources);
    }

    PruningPolicy.Resolved resolvePolicy(String sinkKind,
                                         ModelRegistry.SinkDescriptor sinkDescriptor,
                                         boolean fromSink,
                                         boolean searchAllSources) {
        ModelRegistry.SinkDescriptor descriptor = sinkDescriptor == null
                ? ModelRegistry.SinkDescriptor.empty()
                : sinkDescriptor;
        return pruningPolicy().resolve(PruningPolicy.MatchContext.builder()
                .sinkKind(sinkKind)
                .sinkTier(descriptor.getRuleTier())
                .sinkTags(descriptor.getTags())
                .mode(fromSink ? PruningPolicy.MatchMode.SINK : PruningPolicy.MatchMode.SOURCE)
                .searchAllSources(searchAllSources)
                .build());
    }

    private Transition transitionInternal(MethodReference.Handle from,
                                          MethodReference.Handle to,
                                          PortState state,
                                          String relType,
                                          String confidence,
                                          String sinkKind,
                                          PruningPolicy.Resolved policy) {
        if (from == null || to == null || state == null) {
            return impreciseFallback(to, policy);
        }
        if (GraphTraversalRules.isAliasEdge(relType)) {
            return aliasTransition(from, state, confidence, sinkKind, policy);
        }
        if (state.anyPorts || !isPreciseEdge(relType, confidence)) {
            return impreciseFallback(to, policy);
        }
        PortState effective = applySanitizers(from, state, sinkKind, policy);
        if (effective == null || effective.isEmpty()) {
            return Transition.stop();
        }
        MethodSummary summary = summaryEngine.getSummary(from);
        if (summary == null || summary.isUnknown()) {
            return impreciseFallback(to, policy);
        }
        BitSet nextPorts = new BitSet();
        boolean matched = false;
        boolean sawLow = false;
        for (CallFlow flow : summary.getCallFlows()) {
            if (flow == null || flow.getCallee() == null || flow.getFrom() == null || flow.getTo() == null) {
                continue;
            }
            if (!sameMethod(flow.getCallee(), to)) {
                continue;
            }
            if (isAdditionalFlow(flow) && !policy.allowAdditionalFlows()) {
                continue;
            }
            if (!effective.matches(flow.getFrom())) {
                continue;
            }
            if (isLowConfidenceFlow(flow) && !policy.allowLowConfidenceSummaryFlows()) {
                continue;
            }
            matched = true;
            int bitIndex = portBitIndex(flow.getTo());
            if (bitIndex < 0) {
                return impreciseFallback(to, policy);
            }
            nextPorts.set(bitIndex);
            if (isLowConfidenceFlow(flow)) {
                sawLow = true;
            }
        }
        if (!matched || nextPorts.isEmpty()) {
            if (!effective.uncertain) {
                return Transition.stop();
            }
            return impreciseFallback(to, policy);
        }
        return new Transition(
                new PortState(nextPorts, effective.uncertain || sawLow, false),
                sawLow,
                true
        );
    }

    private Transition reverseTransitionInternal(MethodReference.Handle from,
                                                 MethodReference.Handle to,
                                                 PortState state,
                                                 String relType,
                                                 String confidence,
                                                 String sinkKind,
                                                 PruningPolicy.Resolved policy) {
        if (from == null || to == null || state == null) {
            return impreciseFallback(from, policy);
        }
        if (GraphTraversalRules.isAliasEdge(relType)) {
            return aliasTransition(from, state, confidence, sinkKind, policy);
        }
        if (state.anyPorts || !isPreciseEdge(relType, confidence)) {
            return impreciseFallback(from, policy);
        }
        MethodSummary summary = summaryEngine.getSummary(from);
        if (summary == null || summary.isUnknown()) {
            return impreciseFallback(from, policy);
        }
        BitSet nextPorts = new BitSet();
        boolean matched = false;
        boolean sawLow = false;
        for (CallFlow flow : summary.getCallFlows()) {
            if (flow == null || flow.getCallee() == null || flow.getFrom() == null || flow.getTo() == null) {
                continue;
            }
            if (!sameMethod(flow.getCallee(), to)) {
                continue;
            }
            if (isAdditionalFlow(flow) && !policy.allowAdditionalFlows()) {
                continue;
            }
            if (!state.matches(flow.getTo())) {
                continue;
            }
            if (isLowConfidenceFlow(flow) && !policy.allowLowConfidenceSummaryFlows()) {
                continue;
            }
            matched = true;
            int bitIndex = portBitIndex(flow.getFrom());
            if (bitIndex < 0) {
                return impreciseFallback(from, policy);
            }
            nextPorts.set(bitIndex);
            if (isLowConfidenceFlow(flow)) {
                sawLow = true;
            }
        }
        if (!matched || nextPorts.isEmpty()) {
            if (!state.uncertain) {
                return Transition.stop();
            }
            return impreciseFallback(from, policy);
        }
        PortState callerState = new PortState(nextPorts, state.uncertain || sawLow, false);
        PortState effective = applySanitizers(from, callerState, sinkKind, policy);
        if (effective == null || effective.isEmpty()) {
            return Transition.stop();
        }
        return new Transition(effective, sawLow, true);
    }

    private PortState applySanitizers(MethodReference.Handle method,
                                      PortState state,
                                      String sinkKind,
                                      PruningPolicy.Resolved policy) {
        if (method == null || state == null || state.anyPorts || state.isEmpty()) {
            return state;
        }
        if (policy.sanitizerMode() == PruningPolicy.SanitizerMode.IGNORE) {
            return state;
        }
        SanitizerRule currentRule = sanitizerRule;
        List<Sanitizer> rules = currentRule == null ? Collections.emptyList() : currentRule.getRules();
        if (rules == null || rules.isEmpty()) {
            return state;
        }
        BitSet remaining = state.copyPorts();
        for (Sanitizer rule : rules) {
            if (!sanitizerRuleMatches(rule, method, sinkKind)) {
                continue;
            }
            removeSanitizedPorts(remaining, method, rule);
            if (remaining.isEmpty()) {
                return null;
            }
        }
        return new PortState(remaining, state.uncertain, false);
    }

    private Transition aliasTransition(MethodReference.Handle method,
                                       PortState state,
                                       String confidence,
                                       String sinkKind,
                                       PruningPolicy.Resolved policy) {
        if (state == null) {
            return Transition.stop();
        }
        PortState effective = state.anyPorts ? state : applySanitizers(method, state, sinkKind, policy);
        if (effective == null || effective.isEmpty()) {
            return Transition.stop();
        }
        boolean lowConfidence = "low".equalsIgnoreCase(GraphTraversalRules.normalizeConfidence(confidence));
        BitSet ports = effective.anyPorts ? new BitSet() : effective.copyPorts();
        return new Transition(
                new PortState(ports, effective.uncertain || lowConfidence, effective.anyPorts),
                lowConfidence,
                true
        );
    }

    private static void removeSanitizedPorts(BitSet ports,
                                             MethodReference.Handle method,
                                             Sanitizer rule) {
        if (ports == null || method == null || rule == null) {
            return;
        }
        int paramIndex = rule.getParamIndex();
        if (paramIndex == Sanitizer.NO_PARAM) {
            return;
        }
        if (paramIndex == Sanitizer.THIS_PARAM) {
            ports.clear(0);
            return;
        }
        int paramCount = argumentCount(method);
        if (paramIndex == Sanitizer.ALL_PARAMS) {
            if (paramCount > 0) {
                ports.clear(1, paramCount + 1);
            }
            return;
        }
        if (paramIndex >= 0 && paramIndex < paramCount) {
            ports.clear(paramIndex + 1);
        }
    }

    private static boolean sanitizerRuleMatches(Sanitizer rule,
                                                MethodReference.Handle method,
                                                String sinkKind) {
        if (rule == null || method == null || method.getClassReference() == null) {
            return false;
        }
        if (!kindMatches(rule.getKind(), sinkKind)) {
            return false;
        }
        String ruleClassName = safe(rule.getClassName()).replace('.', '/');
        String methodClassName = safe(method.getClassReference().getName()).replace('.', '/');
        if (!ruleClassName.equals(methodClassName)) {
            return false;
        }
        if (!safe(rule.getMethodName()).equals(safe(method.getName()))) {
            return false;
        }
        return descMatches(rule.getMethodDesc(), method.getDesc());
    }

    private static boolean kindMatches(String ruleKind, String sinkKind) {
        String rk = normalizeKind(ruleKind);
        if (rk == null || rk.isEmpty() || "any".equals(rk) || "all".equals(rk)) {
            return true;
        }
        String sk = normalizeKind(sinkKind);
        if (sk == null || sk.isEmpty()) {
            return false;
        }
        if ("any".equals(sk) || "all".equals(sk)) {
            return true;
        }
        String[] parts = rk.split("[,|/\\s]+");
        for (String part : parts) {
            if (part != null && !part.isBlank() && part.equals(sk)) {
                return true;
            }
        }
        return false;
    }

    private static String normalizeKind(String value) {
        String normalized = safe(value).toLowerCase(Locale.ROOT);
        return normalized.isBlank() ? null : normalized;
    }

    private static boolean descMatches(String ruleDesc, String targetDesc) {
        String expected = safe(ruleDesc);
        String actual = safe(targetDesc);
        if (expected.isBlank() || "*".equals(expected) || "null".equalsIgnoreCase(expected)) {
            return true;
        }
        if (expected.startsWith("(") && expected.endsWith(")")) {
            return actual.startsWith(expected);
        }
        return expected.equals(actual);
    }

    private static int argumentCount(MethodReference.Handle method) {
        if (method == null || safe(method.getDesc()).isBlank()) {
            return 0;
        }
        try {
            return Type.getArgumentTypes(method.getDesc()).length;
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static boolean isPreciseEdge(String relType, String confidence) {
        if ("low".equalsIgnoreCase(safe(confidence))) {
            return false;
        }
        String type = safe(relType).toLowerCase(Locale.ROOT);
        return switch (type) {
            case "direct", "calls_direct",
                    "dispatch", "calls_dispatch",
                    "override", "calls_override",
                    "invoke_dynamic", "indy", "calls_indy",
                    "method_handle", "calls_method_handle",
                    "framework", "calls_framework",
                    "pta", "calls_pta" -> true;
            default -> false;
        };
    }

    private static boolean isLowConfidenceFlow(CallFlow flow) {
        return flow != null && "low".equalsIgnoreCase(safe(flow.getConfidence()));
    }

    private static boolean isAdditionalFlow(CallFlow flow) {
        if (flow == null || flow.getMarkers() == null || flow.getMarkers().isEmpty()) {
            return false;
        }
        for (String marker : flow.getMarkers()) {
            String value = safe(marker);
            if (value.isBlank()) {
                continue;
            }
            if (!TaintMarkers.TAINT.equals(value)) {
                return true;
            }
        }
        return false;
    }

    private static Transition impreciseFallback(MethodReference.Handle method, PruningPolicy.Resolved policy) {
        if (policy != null && !policy.allowImpreciseFallback()) {
            return Transition.stop();
        }
        return fallback(method);
    }

    private static PruningPolicy.Resolved effectivePolicy(PruningPolicy.Resolved policy) {
        return policy == null
                ? PruningPolicy.defaults().resolve(PruningPolicy.MatchContext.builder().build())
                : policy;
    }

    private static Transition fallback(MethodReference.Handle method) {
        return new Transition(allInputPorts(method, true), true, false);
    }

    static boolean sameMethod(MethodReference.Handle left, MethodReference.Handle right) {
        if (left == null || right == null) {
            return false;
        }
        if (left.getClassReference() == null || right.getClassReference() == null) {
            return false;
        }
        if (!safe(left.getClassReference().getName()).equals(safe(right.getClassReference().getName()))) {
            return false;
        }
        if (!safe(left.getName()).equals(safe(right.getName()))) {
            return false;
        }
        if (!safe(left.getDesc()).equals(safe(right.getDesc()))) {
            return false;
        }
        Integer leftJar = left.getJarId();
        Integer rightJar = right.getJarId();
        boolean leftUnknown = leftJar == null || leftJar < 0;
        boolean rightUnknown = rightJar == null || rightJar < 0;
        if (leftUnknown || rightUnknown) {
            return true;
        }
        return leftJar.equals(rightJar);
    }

    private static PortState allInputPorts(MethodReference.Handle method, boolean uncertain) {
        if (method == null || safe(method.getDesc()).isBlank()) {
            return PortState.any();
        }
        try {
            int paramCount = Type.getArgumentTypes(method.getDesc()).length;
            BitSet ports = new BitSet(paramCount + 1);
            ports.set(0);
            if (paramCount > 0) {
                ports.set(1, paramCount + 1);
            }
            return new PortState(ports, uncertain, false);
        } catch (Exception ignored) {
            return PortState.any();
        }
    }

    private static int portBitIndex(FlowPort port) {
        if (port == null || port.getKind() == null) {
            return -1;
        }
        return switch (port.getKind()) {
            case THIS -> 0;
            case PARAM -> port.getIndex() >= 0 ? (port.getIndex() + 1) : -1;
            default -> -1;
        };
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    static final class PortState {
        private final BitSet ports;
        private final boolean uncertain;
        private final boolean anyPorts;

        private PortState(BitSet ports, boolean uncertain, boolean anyPorts) {
            this.ports = ports == null ? new BitSet() : (BitSet) ports.clone();
            this.uncertain = uncertain;
            this.anyPorts = anyPorts;
        }

        private static PortState any() {
            return new PortState(new BitSet(), true, true);
        }

        private boolean matches(FlowPort port) {
            if (anyPorts) {
                return portBitIndex(port) >= 0;
            }
            int index = portBitIndex(port);
            return index >= 0 && ports.get(index);
        }

        boolean isEmpty() {
            return !anyPorts && ports.isEmpty();
        }

        private BitSet copyPorts() {
            return (BitSet) ports.clone();
        }

        boolean uncertain() {
            return uncertain;
        }
    }

    record Transition(PortState state, boolean lowConfidence, boolean proven) {
        private static Transition stop() {
            return new Transition(null, false, false);
        }
    }
}
