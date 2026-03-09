/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */
package me.n1ar4.jar.analyzer.rules;

import com.alibaba.fastjson2.annotation.JSONField;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

public class PruningPolicy {
    @JSONField
    private String sourceSelection;
    @JSONField
    private String sanitizerMode;
    @JSONField
    private Boolean allowAdditionalFlows;
    @JSONField
    private String confidenceMode;
    @JSONField
    private List<ScenarioOverride> scenarios;

    public String getSourceSelection() {
        return sourceSelection;
    }

    public void setSourceSelection(String sourceSelection) {
        this.sourceSelection = sourceSelection;
    }

    public String getSanitizerMode() {
        return sanitizerMode;
    }

    public void setSanitizerMode(String sanitizerMode) {
        this.sanitizerMode = sanitizerMode;
    }

    public Boolean getAllowAdditionalFlows() {
        return allowAdditionalFlows;
    }

    public void setAllowAdditionalFlows(Boolean allowAdditionalFlows) {
        this.allowAdditionalFlows = allowAdditionalFlows;
    }

    public String getConfidenceMode() {
        return confidenceMode;
    }

    public void setConfidenceMode(String confidenceMode) {
        this.confidenceMode = confidenceMode;
    }

    public List<ScenarioOverride> getScenarios() {
        return scenarios;
    }

    public void setScenarios(List<ScenarioOverride> scenarios) {
        this.scenarios = scenarios;
    }

    public SourceSelection sourceSelection() {
        return resolve(MatchContext.empty()).sourceSelection();
    }

    public SanitizerMode sanitizerMode() {
        return resolve(MatchContext.empty()).sanitizerMode();
    }

    public boolean allowAdditionalFlows() {
        return resolve(MatchContext.empty()).allowAdditionalFlows();
    }

    public ConfidenceMode confidenceMode() {
        return resolve(MatchContext.empty()).confidenceMode();
    }

    public boolean allowImpreciseFallback() {
        return resolve(MatchContext.empty()).allowImpreciseFallback();
    }

    public boolean allowLowConfidenceSummaryFlows() {
        return resolve(MatchContext.empty()).allowLowConfidenceSummaryFlows();
    }

    public int scenarioCount() {
        return scenarios == null ? 0 : scenarios.size();
    }

    public Resolved resolve(MatchContext context) {
        MatchContext effectiveContext = context == null ? MatchContext.empty() : context;
        String effectiveSourceSelection = sourceSelection;
        String effectiveSanitizerMode = sanitizerMode;
        Boolean effectiveAllowAdditionalFlows = allowAdditionalFlows;
        String effectiveConfidenceMode = confidenceMode;
        if (scenarios != null && !scenarios.isEmpty()) {
            for (ScenarioOverride scenario : scenarios) {
                if (scenario == null || !scenario.matches(effectiveContext)) {
                    continue;
                }
                if (!safe(scenario.getSourceSelection()).isBlank()) {
                    effectiveSourceSelection = scenario.getSourceSelection();
                }
                if (!safe(scenario.getSanitizerMode()).isBlank()) {
                    effectiveSanitizerMode = scenario.getSanitizerMode();
                }
                if (scenario.getAllowAdditionalFlows() != null) {
                    effectiveAllowAdditionalFlows = scenario.getAllowAdditionalFlows();
                }
                if (!safe(scenario.getConfidenceMode()).isBlank()) {
                    effectiveConfidenceMode = scenario.getConfidenceMode();
                }
            }
        }
        return new Resolved(
                parseSourceSelection(effectiveSourceSelection),
                parseSanitizerMode(effectiveSanitizerMode),
                effectiveAllowAdditionalFlows == null || effectiveAllowAdditionalFlows,
                parseConfidenceMode(effectiveConfidenceMode)
        );
    }

    public static PruningPolicy defaults() {
        return new PruningPolicy();
    }

    private static SourceSelection parseSourceSelection(String value) {
        String normalized = safe(value).toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "graph", "build", "persisted" -> SourceSelection.GRAPH;
            case "rules", "live" -> SourceSelection.RULES;
            default -> SourceSelection.MERGED;
        };
    }

    private static SanitizerMode parseSanitizerMode(String value) {
        return "ignore".equals(safe(value).toLowerCase(Locale.ROOT))
                ? SanitizerMode.IGNORE
                : SanitizerMode.HARD;
    }

    private static ConfidenceMode parseConfidenceMode(String value) {
        return "strict".equals(safe(value).toLowerCase(Locale.ROOT))
                ? ConfidenceMode.STRICT
                : ConfidenceMode.BALANCED;
    }

    private static MatchMode parseMatchMode(String value) {
        String normalized = safe(value).toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "source", "forward" -> MatchMode.SOURCE;
            case "sink", "backward", "reverse" -> MatchMode.SINK;
            default -> MatchMode.ANY;
        };
    }

    private static String normalizeSinkTier(String value) {
        String normalized = safe(value).toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "hard", "soft", "clue" -> normalized;
            default -> "";
        };
    }

    private static List<String> normalizeTags(List<String> raw) {
        if (raw == null || raw.isEmpty()) {
            return Collections.emptyList();
        }
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (String item : raw) {
            String normalized = safe(item).toLowerCase(Locale.ROOT);
            if (!normalized.isBlank()) {
                out.add(normalized);
            }
        }
        return out.isEmpty() ? Collections.emptyList() : List.copyOf(out);
    }

    private static String normalizeSinkKind(String value) {
        String normalized = safe(value).toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return "";
        }
        if (normalized.contains("sql")) {
            return "sql";
        }
        if (normalized.contains("ssrf")) {
            return "ssrf";
        }
        if (normalized.contains("xss")) {
            return "xss";
        }
        if (normalized.contains("file") || normalized.contains("path")) {
            return "file";
        }
        if (normalized.contains("rpc")) {
            return "rpc";
        }
        if (normalized.contains("jndi")) {
            return "jndi";
        }
        if (normalized.contains("rce")) {
            return "rce";
        }
        if (normalized.contains("xxe")) {
            return "xxe";
        }
        if (normalized.contains("deserialize")) {
            return "deserialize";
        }
        return normalized;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public static final class Resolved {
        private final SourceSelection sourceSelection;
        private final SanitizerMode sanitizerMode;
        private final boolean allowAdditionalFlows;
        private final ConfidenceMode confidenceMode;

        private Resolved(SourceSelection sourceSelection,
                         SanitizerMode sanitizerMode,
                         boolean allowAdditionalFlows,
                         ConfidenceMode confidenceMode) {
            this.sourceSelection = sourceSelection == null ? SourceSelection.MERGED : sourceSelection;
            this.sanitizerMode = sanitizerMode == null ? SanitizerMode.HARD : sanitizerMode;
            this.allowAdditionalFlows = allowAdditionalFlows;
            this.confidenceMode = confidenceMode == null ? ConfidenceMode.BALANCED : confidenceMode;
        }

        public SourceSelection sourceSelection() {
            return sourceSelection;
        }

        public SanitizerMode sanitizerMode() {
            return sanitizerMode;
        }

        public boolean allowAdditionalFlows() {
            return allowAdditionalFlows;
        }

        public ConfidenceMode confidenceMode() {
            return confidenceMode;
        }

        public boolean allowImpreciseFallback() {
            return confidenceMode != ConfidenceMode.STRICT;
        }

        public boolean allowLowConfidenceSummaryFlows() {
            return confidenceMode != ConfidenceMode.STRICT;
        }
    }

    public static final class MatchContext {
        private final String sinkKind;
        private final String sinkTier;
        private final List<String> sinkTags;
        private final MatchMode mode;
        private final boolean searchAllSources;

        private MatchContext(Builder builder) {
            this.sinkKind = normalizeSinkKind(builder.sinkKind);
            this.sinkTier = normalizeSinkTier(builder.sinkTier);
            this.sinkTags = normalizeTags(builder.sinkTags);
            this.mode = builder.mode == null ? MatchMode.ANY : builder.mode;
            this.searchAllSources = builder.searchAllSources;
        }

        public static Builder builder() {
            return new Builder();
        }

        private static MatchContext empty() {
            return builder().build();
        }

        public String sinkKind() {
            return sinkKind;
        }

        public String sinkTier() {
            return sinkTier;
        }

        public List<String> sinkTags() {
            return sinkTags;
        }

        public MatchMode mode() {
            return mode;
        }

        public boolean searchAllSources() {
            return searchAllSources;
        }

        public static final class Builder {
            private String sinkKind;
            private String sinkTier;
            private List<String> sinkTags = Collections.emptyList();
            private MatchMode mode = MatchMode.ANY;
            private boolean searchAllSources;

            public Builder sinkKind(String sinkKind) {
                this.sinkKind = sinkKind;
                return this;
            }

            public Builder mode(String mode) {
                this.mode = parseMatchMode(mode);
                return this;
            }

            public Builder sinkTier(String sinkTier) {
                this.sinkTier = sinkTier;
                return this;
            }

            public Builder sinkTags(List<String> sinkTags) {
                this.sinkTags = sinkTags == null ? Collections.emptyList() : sinkTags;
                return this;
            }

            public Builder mode(MatchMode mode) {
                this.mode = mode == null ? MatchMode.ANY : mode;
                return this;
            }

            public Builder searchAllSources(boolean searchAllSources) {
                this.searchAllSources = searchAllSources;
                return this;
            }

            public MatchContext build() {
                return new MatchContext(this);
            }
        }
    }

    public static final class ScenarioOverride {
        @JSONField
        private String name;
        @JSONField
        private String mode;
        @JSONField
        private Boolean searchAllSources;
        @JSONField
        private String sinkKind;
        @JSONField
        private List<String> sinkKinds;
        @JSONField
        private String sinkTier;
        @JSONField
        private List<String> sinkTiers;
        @JSONField
        private List<String> sinkTags;
        @JSONField
        private String sourceSelection;
        @JSONField
        private String sanitizerMode;
        @JSONField
        private Boolean allowAdditionalFlows;
        @JSONField
        private String confidenceMode;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        public Boolean getSearchAllSources() {
            return searchAllSources;
        }

        public void setSearchAllSources(Boolean searchAllSources) {
            this.searchAllSources = searchAllSources;
        }

        public String getSinkKind() {
            return sinkKind;
        }

        public void setSinkKind(String sinkKind) {
            this.sinkKind = sinkKind;
        }

        public List<String> getSinkKinds() {
            return sinkKinds;
        }

        public void setSinkKinds(List<String> sinkKinds) {
            this.sinkKinds = sinkKinds;
        }

        public String getSinkTier() {
            return sinkTier;
        }

        public void setSinkTier(String sinkTier) {
            this.sinkTier = sinkTier;
        }

        public List<String> getSinkTiers() {
            return sinkTiers;
        }

        public void setSinkTiers(List<String> sinkTiers) {
            this.sinkTiers = sinkTiers;
        }

        public List<String> getSinkTags() {
            return sinkTags;
        }

        public void setSinkTags(List<String> sinkTags) {
            this.sinkTags = sinkTags;
        }

        public String getSourceSelection() {
            return sourceSelection;
        }

        public void setSourceSelection(String sourceSelection) {
            this.sourceSelection = sourceSelection;
        }

        public String getSanitizerMode() {
            return sanitizerMode;
        }

        public void setSanitizerMode(String sanitizerMode) {
            this.sanitizerMode = sanitizerMode;
        }

        public Boolean getAllowAdditionalFlows() {
            return allowAdditionalFlows;
        }

        public void setAllowAdditionalFlows(Boolean allowAdditionalFlows) {
            this.allowAdditionalFlows = allowAdditionalFlows;
        }

        public String getConfidenceMode() {
            return confidenceMode;
        }

        public void setConfidenceMode(String confidenceMode) {
            this.confidenceMode = confidenceMode;
        }

        private boolean matches(MatchContext context) {
            MatchContext effectiveContext = context == null ? MatchContext.empty() : context;
            MatchMode expectedMode = parseMatchMode(mode);
            if (expectedMode != MatchMode.ANY && expectedMode != effectiveContext.mode()) {
                return false;
            }
            if (searchAllSources != null && searchAllSources != effectiveContext.searchAllSources()) {
                return false;
            }
            List<String> expectedKinds = expectedSinkKinds();
            if (!expectedKinds.isEmpty()) {
                String actualKind = effectiveContext.sinkKind();
                if (actualKind.isBlank()) {
                    return false;
                }
                boolean kindMatched = false;
                for (String expectedKind : expectedKinds) {
                    if (expectedKind.equals(actualKind)) {
                        kindMatched = true;
                        break;
                    }
                }
                if (!kindMatched) {
                    return false;
                }
            }
            List<String> expectedTiers = expectedSinkTiers();
            if (!expectedTiers.isEmpty()) {
                String actualTier = effectiveContext.sinkTier();
                if (actualTier.isBlank()) {
                    return false;
                }
                boolean tierMatched = false;
                for (String expectedTier : expectedTiers) {
                    if (expectedTier.equals(actualTier)) {
                        tierMatched = true;
                        break;
                    }
                }
                if (!tierMatched) {
                    return false;
                }
            }
            List<String> expectedTags = expectedSinkTags();
            if (!expectedTags.isEmpty()) {
                List<String> actualTags = effectiveContext.sinkTags();
                if (actualTags == null || actualTags.isEmpty()) {
                    return false;
                }
                boolean tagMatched = false;
                for (String expectedTag : expectedTags) {
                    if (actualTags.contains(expectedTag)) {
                        tagMatched = true;
                        break;
                    }
                }
                if (!tagMatched) {
                    return false;
                }
            }
            return true;
        }

        private List<String> expectedSinkKinds() {
            List<String> out = new ArrayList<>();
            String single = normalizeSinkKind(sinkKind);
            if (!single.isBlank()) {
                out.add(single);
            }
            if (sinkKinds != null && !sinkKinds.isEmpty()) {
                for (String item : sinkKinds) {
                    String normalized = normalizeSinkKind(item);
                    if (!normalized.isBlank()) {
                        out.add(normalized);
                    }
                }
            }
            if (out.isEmpty()) {
                return Collections.emptyList();
            }
            return out;
        }

        private List<String> expectedSinkTiers() {
            List<String> out = new ArrayList<>();
            String single = normalizeSinkTier(sinkTier);
            if (!single.isBlank()) {
                out.add(single);
            }
            if (sinkTiers != null && !sinkTiers.isEmpty()) {
                for (String item : sinkTiers) {
                    String normalized = normalizeSinkTier(item);
                    if (!normalized.isBlank()) {
                        out.add(normalized);
                    }
                }
            }
            if (out.isEmpty()) {
                return Collections.emptyList();
            }
            return out;
        }

        private List<String> expectedSinkTags() {
            return normalizeTags(sinkTags);
        }
    }

    public enum MatchMode {
        ANY,
        SOURCE,
        SINK
    }

    public enum SourceSelection {
        MERGED,
        GRAPH,
        RULES
    }

    public enum SanitizerMode {
        HARD,
        IGNORE
    }

    public enum ConfidenceMode {
        BALANCED,
        STRICT
    }
}
