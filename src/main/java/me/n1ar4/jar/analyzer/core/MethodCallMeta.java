/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */
package me.n1ar4.jar.analyzer.core;

import java.util.LinkedHashMap;
import java.util.Map;

public final class MethodCallMeta {
    public static final String TYPE_DIRECT = "direct";
    public static final String TYPE_OVERRIDE = "override";
    public static final String TYPE_DISPATCH = "dispatch";
    public static final String TYPE_INDY = "invoke_dynamic";
    public static final String TYPE_METHOD_HANDLE = "method_handle";
    public static final String TYPE_REFLECTION = "reflection";
    public static final String TYPE_CALLBACK = "callback";
    public static final String TYPE_FRAMEWORK = "framework";
    public static final String TYPE_PTA = "pta";
    public static final String TYPE_UNKNOWN = "unknown";

    public static final String CONF_HIGH = "high";
    public static final String CONF_MEDIUM = "medium";
    public static final String CONF_LOW = "low";

    public static final int EVIDENCE_DIRECT = 1 << 0;
    public static final int EVIDENCE_DISPATCH = 1 << 1;
    public static final int EVIDENCE_TYPED = 1 << 2;
    public static final int EVIDENCE_REFLECTION = 1 << 3;
    public static final int EVIDENCE_CALLBACK = 1 << 4;
    public static final int EVIDENCE_OVERRIDE = 1 << 5;
    public static final int EVIDENCE_INDY = 1 << 6;
    public static final int EVIDENCE_METHOD_HANDLE = 1 << 7;
    public static final int EVIDENCE_FRAMEWORK = 1 << 8;
    public static final int EVIDENCE_PTA = 1 << 9;

    private final LinkedHashMap<String, String> evidence = new LinkedHashMap<>();
    private final LinkedHashMap<String, String> evidenceReason = new LinkedHashMap<>();
    private String type;
    private String confidence;
    // Best observed opcode for this edge (belongs to the caller->callee edge, not the method signature).
    private int bestOpcode = -1;
    private int evidenceBits = 0;

    public MethodCallMeta(String type, String confidence) {
        addEvidenceInternal(type, confidence, null);
        recalcPrimary();
    }

    public MethodCallMeta(String type, String confidence, String evidenceRaw) {
        parseEvidence(evidenceRaw);
        if (evidence.isEmpty()) {
            addEvidenceInternal(type, confidence, null);
        }
        recalcPrimary();
    }

    public String getType() {
        return type;
    }

    public String getConfidence() {
        return confidence;
    }

    public String getEvidence() {
        if (evidence.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : evidence.entrySet()) {
            if (sb.length() > 0) {
                sb.append("|");
            }
            sb.append(entry.getKey()).append(":").append(entry.getValue());
            String reason = evidenceReason.get(entry.getKey());
            if (reason != null && !reason.isEmpty()) {
                sb.append(":").append(reason);
            }
        }
        return sb.toString();
    }

    public int getBestOpcode() {
        return bestOpcode;
    }

    public int getEvidenceBits() {
        return evidenceBits;
    }

    public static void record(Map<MethodCallKey, MethodCallMeta> map,
                              MethodCallKey key,
                              String type,
                              String confidence) {
        record(map, key, type, confidence, (String) null);
    }

    public static void record(Map<MethodCallKey, MethodCallMeta> map,
                              MethodCallKey key,
                              String type,
                              String confidence,
                              Integer opcode) {
        record(map, key, type, confidence, null, opcode);
    }

    public static void record(Map<MethodCallKey, MethodCallMeta> map,
                              MethodCallKey key,
                              String type,
                              String confidence,
                              String reason) {
        if (map == null || key == null) {
            return;
        }
        MethodCallMeta existing = map.get(key);
        if (existing == null) {
            MethodCallMeta meta = new MethodCallMeta(type, confidence);
            if (reason != null && !reason.trim().isEmpty()) {
                meta.addEvidence(type, confidence, reason);
            }
            map.put(key, meta);
            return;
        }
        existing.addEvidence(type, confidence, reason);
    }

    public static void record(Map<MethodCallKey, MethodCallMeta> map,
                              MethodCallKey key,
                              String type,
                              String confidence,
                              String reason,
                              Integer opcode) {
        record(map, key, type, confidence, reason);
        if (map == null || key == null) {
            return;
        }
        MethodCallMeta meta = map.get(key);
        if (meta != null) {
            meta.updateBestOpcode(opcode);
        }
    }

    public void addEvidence(String type, String confidence, String reason) {
        addEvidenceInternal(type, confidence, reason);
        recalcPrimary();
    }

    public void mergeFrom(MethodCallMeta other) {
        if (other == null || other.evidence.isEmpty()) {
            return;
        }
        for (Map.Entry<String, String> entry : other.evidence.entrySet()) {
            String type = entry.getKey();
            String confidence = entry.getValue();
            String reason = other.evidenceReason.get(type);
            addEvidenceInternal(type, confidence, reason);
        }
        updateBestOpcode(other.bestOpcode);
        this.evidenceBits |= other.evidenceBits;
        recalcPrimary();
    }

    private void addEvidenceInternal(String type, String confidence, String reason) {
        String t = normalizeType(type);
        String c = normalizeConfidence(confidence);
        String existing = evidence.get(t);
        if (existing == null || confidenceScore(c) > confidenceScore(existing)) {
            evidence.put(t, c);
        }
        if (reason != null && !reason.trim().isEmpty()) {
            evidenceReason.put(t, reason.trim());
        }
        evidenceBits |= evidenceBitFor(t, reason);
    }

    public void updateBestOpcode(Integer opcode) {
        if (opcode == null) {
            return;
        }
        updateBestOpcode(opcode.intValue());
    }

    private void updateBestOpcode(int opcode) {
        if (opcode <= 0) {
            return;
        }
        int newRank = opcodeRank(opcode);
        int oldRank = opcodeRank(bestOpcode);
        if (newRank > oldRank) {
            bestOpcode = opcode;
        }
    }

    private static int opcodeRank(int opcode) {
        // INVOKEVIRTUAL/INTERFACE/SPECIAL/STATIC are the "best" for representing the callsite intent.
        if (opcode == 182 || opcode == 183 || opcode == 184 || opcode == 185) {
            return 2;
        }
        // INVOKEDYNAMIC is still a real callsite, but often less specific for static resolution.
        if (opcode == 186) {
            return 1;
        }
        return 0;
    }

    private static int evidenceBitFor(String type, String reason) {
        if (TYPE_DIRECT.equals(type)) {
            return EVIDENCE_DIRECT;
        }
        if (TYPE_OVERRIDE.equals(type)) {
            return EVIDENCE_OVERRIDE;
        }
        if (TYPE_DISPATCH.equals(type)) {
            if (reason != null) {
                String r = reason.trim();
                if (!r.isEmpty() && r.startsWith("typed:")) {
                    return EVIDENCE_DISPATCH | EVIDENCE_TYPED;
                }
            }
            return EVIDENCE_DISPATCH;
        }
        if (TYPE_INDY.equals(type)) {
            return EVIDENCE_INDY;
        }
        if (TYPE_METHOD_HANDLE.equals(type)) {
            return EVIDENCE_METHOD_HANDLE;
        }
        if (TYPE_REFLECTION.equals(type)) {
            return EVIDENCE_REFLECTION;
        }
        if (TYPE_CALLBACK.equals(type)) {
            return EVIDENCE_CALLBACK;
        }
        if (TYPE_FRAMEWORK.equals(type)) {
            return EVIDENCE_FRAMEWORK;
        }
        if (TYPE_PTA.equals(type)) {
            return EVIDENCE_PTA;
        }
        return 0;
    }

    private void parseEvidence(String raw) {
        if (raw == null) {
            return;
        }
        String value = raw.trim();
        if (value.isEmpty()) {
            return;
        }
        String[] parts = value.split("\\|");
        for (String part : parts) {
            if (part == null || part.trim().isEmpty()) {
                continue;
            }
            String[] kv = part.split(":");
            String t = kv.length > 0 ? kv[0].trim() : "";
            String c = kv.length > 1 ? kv[1].trim() : "";
            String reason = null;
            if (kv.length > 2) {
                StringBuilder sb = new StringBuilder();
                for (int i = 2; i < kv.length; i++) {
                    if (sb.length() > 0) {
                        sb.append(":");
                    }
                    sb.append(kv[i]);
                }
                reason = sb.toString().trim();
            }
            addEvidenceInternal(t, c, reason);
        }
    }

    private void recalcPrimary() {
        String bestType = TYPE_UNKNOWN;
        String bestConf = CONF_LOW;
        int bestConfScore = confidenceScore(bestConf);
        int bestTypeScore = typeScore(bestType);
        for (Map.Entry<String, String> entry : evidence.entrySet()) {
            String t = entry.getKey();
            String c = entry.getValue();
            int confScore = confidenceScore(c);
            int tScore = typeScore(t);
            if (confScore > bestConfScore || (confScore == bestConfScore && tScore > bestTypeScore)) {
                bestConfScore = confScore;
                bestTypeScore = tScore;
                bestType = t;
                bestConf = c;
            }
        }
        this.type = bestType;
        this.confidence = bestConf;
    }

    private static int confidenceScore(String confidence) {
        if (CONF_HIGH.equals(confidence)) {
            return 3;
        }
        if (CONF_MEDIUM.equals(confidence)) {
            return 2;
        }
        if (CONF_LOW.equals(confidence)) {
            return 1;
        }
        return 0;
    }

    private static int typeScore(String type) {
        if (TYPE_DIRECT.equals(type)) {
            return 5;
        }
        if (TYPE_OVERRIDE.equals(type)) {
            return 4;
        }
        if (TYPE_DISPATCH.equals(type)) {
            return 4;
        }
        if (TYPE_INDY.equals(type)) {
            return 3;
        }
        if (TYPE_METHOD_HANDLE.equals(type)) {
            return 2;
        }
        if (TYPE_CALLBACK.equals(type)) {
            return 2;
        }
        if (TYPE_FRAMEWORK.equals(type)) {
            return 2;
        }
        if (TYPE_PTA.equals(type)) {
            return 6;
        }
        if (TYPE_REFLECTION.equals(type)) {
            return 1;
        }
        return 0;
    }

    private static String normalizeType(String type) {
        if (type == null) {
            return TYPE_UNKNOWN;
        }
        String v = type.trim();
        return v.isEmpty() ? TYPE_UNKNOWN : v;
    }

    private static String normalizeConfidence(String confidence) {
        if (confidence == null) {
            return CONF_LOW;
        }
        String v = confidence.trim();
        return v.isEmpty() ? CONF_LOW : v;
    }
}
