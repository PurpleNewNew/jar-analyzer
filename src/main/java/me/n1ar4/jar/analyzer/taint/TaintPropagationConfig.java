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

import me.n1ar4.jar.analyzer.rules.ModelRegistry;
import java.util.Collections;
import java.util.List;

public final class TaintPropagationConfig {
    private final TaintModelRule summaryRule;
    private final TaintModelRule additionalRule;
    private final SanitizerRule barrierRule;
    private final List<TaintGuardRule> guardRules;
    private final TaintAnalysisProfile profile;
    private final TaintPropagationMode propagationMode;

    private TaintPropagationConfig(TaintModelRule summaryRule,
                                   TaintModelRule additionalRule,
                                   SanitizerRule barrierRule,
                                   List<TaintGuardRule> guardRules,
                                   TaintAnalysisProfile profile,
                                   TaintPropagationMode propagationMode) {
        this.summaryRule = summaryRule == null ? new TaintModelRule() : summaryRule;
        this.additionalRule = additionalRule == null ? new TaintModelRule() : additionalRule;
        this.barrierRule = barrierRule == null ? new SanitizerRule() : barrierRule;
        this.guardRules = guardRules == null ? Collections.emptyList() : guardRules;
        this.profile = profile == null ? TaintAnalysisProfile.current() : profile;
        this.propagationMode = propagationMode == null ? TaintPropagationMode.current() : propagationMode;
    }

    public static TaintPropagationConfig resolve() {
        return new TaintPropagationConfig(
                ModelRegistry.getSummaryModelRule(),
                ModelRegistry.getAdditionalModelRule(),
                ModelRegistry.getBarrierRule(),
                ModelRegistry.getGuardRules(),
                TaintAnalysisProfile.current(),
                TaintPropagationMode.current()
        );
    }

    public TaintModelRule getSummaryRule() {
        return summaryRule;
    }

    public TaintModelRule getAdditionalRule() {
        return additionalRule;
    }

    public SanitizerRule getBarrierRule() {
        return barrierRule;
    }

    public List<TaintGuardRule> getGuardRules() {
        return guardRules;
    }

    public TaintAnalysisProfile getProfile() {
        return profile;
    }

    public TaintPropagationMode getPropagationMode() {
        return propagationMode;
    }
}
