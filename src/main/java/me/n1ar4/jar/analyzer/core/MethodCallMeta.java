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
    public static final String TYPE_INDY = "invoke_dynamic";
    public static final String TYPE_METHOD_HANDLE = "method_handle";
    public static final String TYPE_REFLECTION = "reflection";
    public static final String TYPE_UNKNOWN = "unknown";

    public static final String CONF_HIGH = "high";
    public static final String CONF_MEDIUM = "medium";
    public static final String CONF_LOW = "low";

    private final LinkedHashMap<String, String> evidence = new LinkedHashMap<>();
    private final LinkedHashMap<String, String> evidenceReason = new LinkedHashMap<>();
    private String type;
    private String confidence;

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

    public static void record(Map<MethodCallKey, MethodCallMeta> map,
                              MethodCallKey key,
                              String type,
                              String confidence) {
        record(map, key, type, confidence, null);
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
        if (TYPE_INDY.equals(type)) {
            return 3;
        }
        if (TYPE_METHOD_HANDLE.equals(type)) {
            return 2;
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
