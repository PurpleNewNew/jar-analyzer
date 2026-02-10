/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.taint.summary;

import com.alibaba.fastjson2.JSON;
import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Stable serialization for {@link MethodSummary} to store in semantic_cache_table.
 * <p>
 * We use DTOs instead of JSON-reflecting the immutable summary classes directly.
 */
public final class MethodSummarySerde {
    private static final int VERSION = 1;

    private MethodSummarySerde() {
    }

    public static String toCacheValue(MethodSummary summary) {
        if (summary == null) {
            return null;
        }
        SummaryDTO dto = new SummaryDTO();
        dto.v = VERSION;
        dto.unknown = summary.isUnknown();
        dto.hasSideEffect = summary.hasSideEffect();
        dto.edges = encodeEdges(summary.getEdges());
        dto.callFlows = encodeCallFlows(summary.getCallFlows());
        return JSON.toJSONString(dto);
    }

    public static MethodSummary fromCacheValue(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        try {
            SummaryDTO dto = JSON.parseObject(raw, SummaryDTO.class);
            if (dto == null) {
                return null;
            }
            if (dto.v != VERSION) {
                return null;
            }
            MethodSummary summary = new MethodSummary();
            summary.setUnknown(dto.unknown);
            summary.setHasSideEffect(dto.hasSideEffect);
            if (dto.edges != null) {
                for (FlowEdgeDTO edge : dto.edges) {
                    FlowEdge decoded = decodeFlowEdge(edge);
                    if (decoded != null) {
                        summary.addEdge(decoded);
                    }
                }
            }
            if (dto.callFlows != null) {
                for (CallFlowDTO flow : dto.callFlows) {
                    CallFlow decoded = decodeCallFlow(flow);
                    if (decoded != null) {
                        summary.addCallFlow(decoded);
                    }
                }
            }
            return summary;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static List<FlowEdgeDTO> encodeEdges(Set<FlowEdge> edges) {
        if (edges == null || edges.isEmpty()) {
            return Collections.emptyList();
        }
        List<FlowEdgeDTO> out = new ArrayList<>(edges.size());
        for (FlowEdge edge : edges) {
            if (edge == null) {
                continue;
            }
            FlowEdgeDTO dto = new FlowEdgeDTO();
            dto.from = encodePort(edge.getFrom());
            dto.to = encodePort(edge.getTo());
            dto.markers = new ArrayList<>(edge.getMarkers());
            dto.confidence = edge.getConfidence();
            out.add(dto);
        }
        return out;
    }

    private static List<CallFlowDTO> encodeCallFlows(Set<CallFlow> flows) {
        if (flows == null || flows.isEmpty()) {
            return Collections.emptyList();
        }
        List<CallFlowDTO> out = new ArrayList<>(flows.size());
        for (CallFlow flow : flows) {
            if (flow == null) {
                continue;
            }
            CallFlowDTO dto = new CallFlowDTO();
            dto.from = encodePort(flow.getFrom());
            MethodReference.Handle callee = flow.getCallee();
            if (callee != null) {
                dto.calleeClass = callee.getClassReference() == null ? null : callee.getClassReference().getName();
                dto.calleeName = callee.getName();
                dto.calleeDesc = callee.getDesc();
            }
            dto.to = encodePort(flow.getTo());
            dto.markers = new ArrayList<>(flow.getMarkers());
            dto.confidence = flow.getConfidence();
            out.add(dto);
        }
        return out;
    }

    private static FlowPortDTO encodePort(FlowPort port) {
        if (port == null) {
            return null;
        }
        FlowPortDTO dto = new FlowPortDTO();
        dto.kind = port.getKind() == null ? null : port.getKind().name();
        dto.index = port.getIndex();
        dto.fieldOwner = port.getFieldOwner();
        dto.fieldName = port.getFieldName();
        dto.fieldDesc = port.getFieldDesc();
        return dto;
    }

    private static FlowEdge decodeFlowEdge(FlowEdgeDTO dto) {
        if (dto == null) {
            return null;
        }
        FlowPort from = decodePort(dto.from);
        FlowPort to = decodePort(dto.to);
        if (from == null || to == null) {
            return null;
        }
        java.util.Set<String> markers = dto.markers == null ? null : new java.util.HashSet<>(dto.markers);
        return new FlowEdge(from, to, markers, dto.confidence);
    }

    private static CallFlow decodeCallFlow(CallFlowDTO dto) {
        if (dto == null) {
            return null;
        }
        FlowPort from = decodePort(dto.from);
        FlowPort to = decodePort(dto.to);
        if (from == null || to == null) {
            return null;
        }
        if (dto.calleeClass == null || dto.calleeName == null || dto.calleeDesc == null) {
            return null;
        }
        MethodReference.Handle callee = new MethodReference.Handle(
                new ClassReference.Handle(dto.calleeClass),
                dto.calleeName,
                dto.calleeDesc);
        java.util.Set<String> markers = dto.markers == null ? null : new java.util.HashSet<>(dto.markers);
        return new CallFlow(from, callee, to, markers, dto.confidence);
    }

    private static FlowPort decodePort(FlowPortDTO dto) {
        if (dto == null || dto.kind == null) {
            return null;
        }
        try {
            FlowPort.Kind kind = FlowPort.Kind.valueOf(dto.kind);
            switch (kind) {
                case THIS:
                    return FlowPort.thisPort();
                case PARAM:
                    return FlowPort.param(dto.index);
                case RETURN:
                    return FlowPort.ret();
                case FIELD:
                    return FlowPort.field(dto.fieldOwner, dto.fieldName, dto.fieldDesc, false);
                case STATIC_FIELD:
                    return FlowPort.field(dto.fieldOwner, dto.fieldName, dto.fieldDesc, true);
                default:
                    return null;
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    @SuppressWarnings("all")
    private static final class SummaryDTO {
        public int v;
        public boolean unknown;
        public boolean hasSideEffect;
        public List<FlowEdgeDTO> edges;
        public List<CallFlowDTO> callFlows;
    }

    @SuppressWarnings("all")
    private static final class FlowPortDTO {
        public String kind;
        public int index;
        public String fieldOwner;
        public String fieldName;
        public String fieldDesc;
    }

    @SuppressWarnings("all")
    private static final class FlowEdgeDTO {
        public FlowPortDTO from;
        public FlowPortDTO to;
        public List<String> markers;
        public String confidence;
    }

    @SuppressWarnings("all")
    private static final class CallFlowDTO {
        public FlowPortDTO from;
        public String calleeClass;
        public String calleeName;
        public String calleeDesc;
        public FlowPortDTO to;
        public List<String> markers;
        public String confidence;
    }
}
