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
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class TaintAnalysisProfile {
    public enum Level {
        FAST,
        BALANCED,
        STRICT
    }

    public enum AdditionalStep {
        RULES,
        CONTAINER,
        BUILDER,
        GETTER_SETTER,
        REFLECT_FIELD,
        OPTIONAL,
        STREAM,
        ARRAY
    }

    private static final String PROP_PROFILE = "jar.analyzer.taint.profile";
    private static final String PROP_ADDITIONAL = "jar.analyzer.taint.additional";
    private static final TaintAnalysisProfile CURRENT = resolve();

    private final Level level;
    private final EnumSet<AdditionalStep> additionalSteps;

    private TaintAnalysisProfile(Level level, EnumSet<AdditionalStep> additionalSteps) {
        this.level = level;
        this.additionalSteps = additionalSteps == null
                ? EnumSet.noneOf(AdditionalStep.class)
                : EnumSet.copyOf(additionalSteps);
    }

    public static TaintAnalysisProfile current() {
        return CURRENT;
    }

    public Level getLevel() {
        return level;
    }

    public boolean isSemanticGateEnabled() {
        return level == Level.STRICT;
    }

    public boolean isSeedHeuristicEnabled() {
        return level != Level.STRICT;
    }

    public boolean isAdditionalEnabled(AdditionalStep step) {
        return step != null && additionalSteps.contains(step);
    }

    public Set<AdditionalStep> getAdditionalSteps() {
        return Collections.unmodifiableSet(additionalSteps);
    }

    private static TaintAnalysisProfile resolve() {
        Level level = resolveLevel(System.getProperty(PROP_PROFILE));
        EnumSet<AdditionalStep> steps = resolveAdditionalSteps(level, System.getProperty(PROP_ADDITIONAL));
        return new TaintAnalysisProfile(level, steps);
    }

    private static Level resolveLevel(String value) {
        if (value == null || value.trim().isEmpty()) {
            return Level.BALANCED;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if ("fast".equals(normalized)) {
            return Level.FAST;
        }
        if ("strict".equals(normalized)) {
            return Level.STRICT;
        }
        if ("balanced".equals(normalized) || "default".equals(normalized)) {
            return Level.BALANCED;
        }
        return Level.BALANCED;
    }

    private static EnumSet<AdditionalStep> resolveAdditionalSteps(Level level, String override) {
        EnumSet<AdditionalStep> steps = EnumSet.noneOf(AdditionalStep.class);
        if (override != null && !override.trim().isEmpty()) {
            applyAdditionalTokens(steps, override);
            return steps;
        }
        if (level == Level.FAST || level == Level.STRICT) {
            return steps;
        }
        List<String> hints = ModelRegistry.getAdditionalStepHints();
        if (hints == null || hints.isEmpty()) {
            return steps;
        }
        for (String token : hints) {
            addStepToken(steps, token);
        }
        return steps;
    }

    private static void applyAdditionalTokens(EnumSet<AdditionalStep> steps, String raw) {
        String[] parts = raw.split(",");
        for (String part : parts) {
            addStepToken(steps, part);
        }
    }

    private static void addStepToken(EnumSet<AdditionalStep> steps, String token) {
        if (token == null) {
            return;
        }
        String normalized = token.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return;
        }
        if ("all".equals(normalized)) {
            steps.addAll(EnumSet.allOf(AdditionalStep.class));
            return;
        }
        if ("none".equals(normalized) || "off".equals(normalized)) {
            steps.clear();
            return;
        }
        if ("rules".equals(normalized) || "additional".equals(normalized)) {
            steps.add(AdditionalStep.RULES);
            return;
        }
        if ("container".equals(normalized) || "collection".equals(normalized)) {
            steps.add(AdditionalStep.CONTAINER);
            return;
        }
        if ("builder".equals(normalized) || "stringbuilder".equals(normalized)) {
            steps.add(AdditionalStep.BUILDER);
            return;
        }
        if ("getter".equals(normalized) || "getter-setter".equals(normalized)
                || "setter".equals(normalized) || "bean".equals(normalized)) {
            steps.add(AdditionalStep.GETTER_SETTER);
            return;
        }
        if ("reflect".equals(normalized) || "reflection".equals(normalized)) {
            steps.add(AdditionalStep.REFLECT_FIELD);
            return;
        }
        if ("optional".equals(normalized)) {
            steps.add(AdditionalStep.OPTIONAL);
            return;
        }
        if ("stream".equals(normalized)) {
            steps.add(AdditionalStep.STREAM);
            return;
        }
        if ("array".equals(normalized)) {
            steps.add(AdditionalStep.ARRAY);
        }
    }
}
