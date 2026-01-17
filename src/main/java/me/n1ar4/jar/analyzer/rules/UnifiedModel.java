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
import me.n1ar4.jar.analyzer.chains.SinkModel;
import me.n1ar4.jar.analyzer.taint.Sanitizer;
import me.n1ar4.jar.analyzer.taint.TaintModel;
import me.n1ar4.jar.analyzer.taint.TaintGuardRule;

import java.util.List;

public class UnifiedModel {
    @JSONField
    private List<SourceModel> sourceModel;
    @JSONField
    private List<String> sourceAnnotations;
    @JSONField
    private List<SinkModel> sinkModel;
    @JSONField
    private List<TaintModel> summaryModel;
    @JSONField
    private List<TaintModel> additionalTaintSteps;
    @JSONField
    private List<Sanitizer> sanitizerModel;
    @JSONField
    private List<Sanitizer> neutralModel;
    @JSONField
    private List<TaintGuardRule> guardSanitizers;

    public List<SourceModel> getSourceModel() {
        return sourceModel;
    }

    public void setSourceModel(List<SourceModel> sourceModel) {
        this.sourceModel = sourceModel;
    }

    public List<String> getSourceAnnotations() {
        return sourceAnnotations;
    }

    public void setSourceAnnotations(List<String> sourceAnnotations) {
        this.sourceAnnotations = sourceAnnotations;
    }

    public List<SinkModel> getSinkModel() {
        return sinkModel;
    }

    public void setSinkModel(List<SinkModel> sinkModel) {
        this.sinkModel = sinkModel;
    }

    public List<TaintModel> getSummaryModel() {
        return summaryModel;
    }

    public void setSummaryModel(List<TaintModel> summaryModel) {
        this.summaryModel = summaryModel;
    }

    public List<TaintModel> getAdditionalTaintSteps() {
        return additionalTaintSteps;
    }

    public void setAdditionalTaintSteps(List<TaintModel> additionalTaintSteps) {
        this.additionalTaintSteps = additionalTaintSteps;
    }

    public List<Sanitizer> getSanitizerModel() {
        return sanitizerModel;
    }

    public void setSanitizerModel(List<Sanitizer> sanitizerModel) {
        this.sanitizerModel = sanitizerModel;
    }

    public List<Sanitizer> getNeutralModel() {
        return neutralModel;
    }

    public void setNeutralModel(List<Sanitizer> neutralModel) {
        this.neutralModel = neutralModel;
    }

    public List<TaintGuardRule> getGuardSanitizers() {
        return guardSanitizers;
    }

    public void setGuardSanitizers(List<TaintGuardRule> guardSanitizers) {
        this.guardSanitizers = guardSanitizers;
    }
}
