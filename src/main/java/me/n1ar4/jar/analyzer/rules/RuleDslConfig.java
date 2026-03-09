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
import me.n1ar4.jar.analyzer.taint.TaintFlow;

import java.util.List;

public class RuleDslConfig {
    @JSONField
    private List<RuleSpec> rules;

    public List<RuleSpec> getRules() {
        return rules;
    }

    public void setRules(List<RuleSpec> rules) {
        this.rules = rules;
    }

    public static class RuleSpec {
        @JSONField
        private String id;
        @JSONField
        private String kind;
        @JSONField
        private MethodMatch match;
        @JSONField
        private List<TaintFlow> flows;
        @JSONField
        private String sourceKind;
        @JSONField
        private String annotation;
        @JSONField
        private String target;
        @JSONField
        private String sanitizerKind;
        @JSONField
        private String appliesToKind;
        @JSONField
        private String sinkCategory;
        @JSONField
        private String severity;
        @JSONField
        private String ruleTier;
        @JSONField
        private String type;
        @JSONField
        private Integer paramIndex;
        @JSONField
        private List<String> allowlist;
        @JSONField
        private Boolean enabled;
        @JSONField
        private String mode;
        @JSONField
        private Boolean requireNormalized;
        @JSONField
        private String hint;
        @JSONField
        private List<String> hints;
        @JSONField
        private List<String> tags;
        @JSONField
        private String boxName;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getKind() {
            return kind;
        }

        public void setKind(String kind) {
            this.kind = kind;
        }

        public MethodMatch getMatch() {
            return match;
        }

        public void setMatch(MethodMatch match) {
            this.match = match;
        }

        public List<TaintFlow> getFlows() {
            return flows;
        }

        public void setFlows(List<TaintFlow> flows) {
            this.flows = flows;
        }

        public String getSourceKind() {
            return sourceKind;
        }

        public void setSourceKind(String sourceKind) {
            this.sourceKind = sourceKind;
        }

        public String getAnnotation() {
            return annotation;
        }

        public void setAnnotation(String annotation) {
            this.annotation = annotation;
        }

        public String getTarget() {
            return target;
        }

        public void setTarget(String target) {
            this.target = target;
        }

        public String getSanitizerKind() {
            return sanitizerKind;
        }

        public void setSanitizerKind(String sanitizerKind) {
            this.sanitizerKind = sanitizerKind;
        }

        public String getAppliesToKind() {
            return appliesToKind;
        }

        public void setAppliesToKind(String appliesToKind) {
            this.appliesToKind = appliesToKind;
        }

        public String getSinkCategory() {
            return sinkCategory;
        }

        public void setSinkCategory(String sinkCategory) {
            this.sinkCategory = sinkCategory;
        }

        public String getSeverity() {
            return severity;
        }

        public void setSeverity(String severity) {
            this.severity = severity;
        }

        public String getRuleTier() {
            return ruleTier;
        }

        public void setRuleTier(String ruleTier) {
            this.ruleTier = ruleTier;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Integer getParamIndex() {
            return paramIndex;
        }

        public void setParamIndex(Integer paramIndex) {
            this.paramIndex = paramIndex;
        }

        public List<String> getAllowlist() {
            return allowlist;
        }

        public void setAllowlist(List<String> allowlist) {
            this.allowlist = allowlist;
        }

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        public Boolean getRequireNormalized() {
            return requireNormalized;
        }

        public void setRequireNormalized(Boolean requireNormalized) {
            this.requireNormalized = requireNormalized;
        }

        public String getHint() {
            return hint;
        }

        public void setHint(String hint) {
            this.hint = hint;
        }

        public List<String> getHints() {
            return hints;
        }

        public void setHints(List<String> hints) {
            this.hints = hints;
        }

        public List<String> getTags() {
            return tags;
        }

        public void setTags(List<String> tags) {
            this.tags = tags;
        }

        public String getBoxName() {
            return boxName;
        }

        public void setBoxName(String boxName) {
            this.boxName = boxName;
        }
    }

    public static class MethodMatch {
        @JSONField
        private String className;
        @JSONField
        private String methodName;
        @JSONField
        private String methodDesc;
        @JSONField
        private Boolean subtypes;

        public String getClassName() {
            return className;
        }

        public void setClassName(String className) {
            this.className = className;
        }

        public String getMethodName() {
            return methodName;
        }

        public void setMethodName(String methodName) {
            this.methodName = methodName;
        }

        public String getMethodDesc() {
            return methodDesc;
        }

        public void setMethodDesc(String methodDesc) {
            this.methodDesc = methodDesc;
        }

        public Boolean getSubtypes() {
            return subtypes;
        }

        public void setSubtypes(Boolean subtypes) {
            this.subtypes = subtypes;
        }
    }
}
