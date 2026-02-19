/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.graph.flow;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class FlowOptions {
    private final boolean fromSink;
    private final boolean searchAllSources;
    private final int depth;
    private final int maxLimit;
    private final Integer maxPaths;
    private final Integer maxNodes;
    private final Integer maxEdges;
    private final Integer timeoutMs;
    private final boolean onlyFromWeb;
    private final Set<String> blacklist;
    private final String minEdgeConfidence;
    private final String sinkClass;
    private final String sinkMethod;
    private final String sinkDesc;
    private final String sourceClass;
    private final String sourceMethod;
    private final String sourceDesc;

    private FlowOptions(Builder builder) {
        this.fromSink = builder.fromSink;
        this.searchAllSources = builder.searchAllSources;
        this.depth = Math.max(1, builder.depth);
        this.maxLimit = builder.maxLimit <= 0 ? 30 : builder.maxLimit;
        this.maxPaths = positiveOrNull(builder.maxPaths);
        this.maxNodes = positiveOrNull(builder.maxNodes);
        this.maxEdges = positiveOrNull(builder.maxEdges);
        this.timeoutMs = positiveOrNull(builder.timeoutMs);
        this.onlyFromWeb = builder.onlyFromWeb;
        this.blacklist = normalizeBlacklist(builder.blacklist);
        this.minEdgeConfidence = normalizeConfidence(builder.minEdgeConfidence);
        this.sinkClass = normalizeClass(builder.sinkClass);
        this.sinkMethod = safe(builder.sinkMethod);
        this.sinkDesc = normalizeDesc(builder.sinkDesc);
        this.sourceClass = normalizeClass(builder.sourceClass);
        this.sourceMethod = safe(builder.sourceMethod);
        this.sourceDesc = normalizeDesc(builder.sourceDesc);
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isFromSink() {
        return fromSink;
    }

    public boolean isSearchAllSources() {
        return searchAllSources;
    }

    public int getDepth() {
        return depth;
    }

    public int getMaxLimit() {
        return maxLimit;
    }

    public Integer getMaxPaths() {
        return maxPaths;
    }

    public Integer getMaxNodes() {
        return maxNodes;
    }

    public Integer getMaxEdges() {
        return maxEdges;
    }

    public Integer getTimeoutMs() {
        return timeoutMs;
    }

    public boolean isOnlyFromWeb() {
        return onlyFromWeb;
    }

    public Set<String> getBlacklist() {
        return blacklist;
    }

    public String getMinEdgeConfidence() {
        return minEdgeConfidence;
    }

    public String getSinkClass() {
        return sinkClass;
    }

    public String getSinkMethod() {
        return sinkMethod;
    }

    public String getSinkDesc() {
        return sinkDesc;
    }

    public String getSourceClass() {
        return sourceClass;
    }

    public String getSourceMethod() {
        return sourceMethod;
    }

    public String getSourceDesc() {
        return sourceDesc;
    }

    public int resolvePathLimit() {
        int limit = maxLimit <= 0 ? Integer.MAX_VALUE : maxLimit;
        if (maxPaths != null && maxPaths > 0) {
            limit = Math.min(limit, maxPaths);
        }
        return limit <= 0 ? Integer.MAX_VALUE : limit;
    }

    private static Integer positiveOrNull(Integer value) {
        return value == null || value <= 0 ? null : value;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static String normalizeClass(String className) {
        return safe(className).replace('.', '/');
    }

    private static String normalizeDesc(String desc) {
        String value = safe(desc);
        if (value.isEmpty() || "null".equalsIgnoreCase(value)) {
            return "*";
        }
        return value;
    }

    private static String normalizeConfidence(String confidence) {
        String value = safe(confidence).toLowerCase();
        if ("high".equals(value) || "medium".equals(value) || "low".equals(value)) {
            return value;
        }
        return "low";
    }

    private static Set<String> normalizeBlacklist(Set<String> raw) {
        if (raw == null || raw.isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> out = new HashSet<>();
        for (String item : raw) {
            String v = safe(item).replace('.', '/');
            if (!v.isEmpty()) {
                out.add(v);
            }
        }
        return Collections.unmodifiableSet(out);
    }

    public static final class Builder {
        private boolean fromSink = true;
        private boolean searchAllSources = false;
        private int depth = 10;
        private int maxLimit = 30;
        private Integer maxPaths;
        private Integer maxNodes;
        private Integer maxEdges;
        private Integer timeoutMs;
        private boolean onlyFromWeb = false;
        private Set<String> blacklist = Collections.emptySet();
        private String minEdgeConfidence = "low";
        private String sinkClass = "";
        private String sinkMethod = "";
        private String sinkDesc = "*";
        private String sourceClass = "";
        private String sourceMethod = "";
        private String sourceDesc = "*";

        public Builder fromSink(boolean fromSink) {
            this.fromSink = fromSink;
            return this;
        }

        public Builder searchAllSources(boolean searchAllSources) {
            this.searchAllSources = searchAllSources;
            return this;
        }

        public Builder depth(int depth) {
            this.depth = depth;
            return this;
        }

        public Builder maxLimit(Integer maxLimit) {
            if (maxLimit != null) {
                this.maxLimit = maxLimit;
            }
            return this;
        }

        public Builder maxPaths(Integer maxPaths) {
            this.maxPaths = maxPaths;
            return this;
        }

        public Builder maxNodes(Integer maxNodes) {
            this.maxNodes = maxNodes;
            return this;
        }

        public Builder maxEdges(Integer maxEdges) {
            this.maxEdges = maxEdges;
            return this;
        }

        public Builder timeoutMs(Integer timeoutMs) {
            this.timeoutMs = timeoutMs;
            return this;
        }

        public Builder onlyFromWeb(boolean onlyFromWeb) {
            this.onlyFromWeb = onlyFromWeb;
            return this;
        }

        public Builder blacklist(Set<String> blacklist) {
            this.blacklist = blacklist;
            return this;
        }

        public Builder minEdgeConfidence(String minEdgeConfidence) {
            this.minEdgeConfidence = minEdgeConfidence;
            return this;
        }

        public Builder sink(String sinkClass, String sinkMethod, String sinkDesc) {
            this.sinkClass = sinkClass;
            this.sinkMethod = sinkMethod;
            this.sinkDesc = sinkDesc;
            return this;
        }

        public Builder source(String sourceClass, String sourceMethod, String sourceDesc) {
            this.sourceClass = sourceClass;
            this.sourceMethod = sourceMethod;
            this.sourceDesc = sourceDesc;
            return this;
        }

        public FlowOptions build() {
            return new FlowOptions(this);
        }
    }
}
