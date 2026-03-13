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

import java.util.Locale;

public record CallGraphPlan(String analysisProfile,
                            String callGraphEngine,
                            String callGraphModeMeta,
                            boolean selectivePta) {
    public static final String CALL_GRAPH_PROFILE_PROP = "jar.analyzer.callgraph.profile";
    public static final String ENGINE_BYTECODE = BytecodeMainlineCallGraphRunner.ENGINE;
    public static final String ENGINE_BYTECODE_PTA = "bytecode-mainline+pta-refine";

    public static final String PROFILE_FAST = "fast";
    public static final String PROFILE_BALANCED = "balanced";
    public static final String PROFILE_PRECISION = "precision";

    public static CallGraphPlan resolve(String profileSetting) {
        String profile = normalize(profileSetting);
        if (!profile.isBlank()) {
            return fromProfile(profile);
        }
        return balancedBytecode();
    }

    public BytecodeMainlineCallGraphRunner.Settings bytecodeSettings() {
        return new BytecodeMainlineCallGraphRunner.Settings(
                callGraphModeMeta,
                selectivePta,
                PROFILE_PRECISION.equals(analysisProfile)
        );
    }

    private static CallGraphPlan fromProfile(String profile) {
        return switch (profile) {
            case PROFILE_FAST -> new CallGraphPlan(
                    PROFILE_FAST,
                    ENGINE_BYTECODE,
                    BytecodeMainlineCallGraphRunner.MODE_FAST_V1,
                    false
            );
            case PROFILE_BALANCED -> new CallGraphPlan(
                    PROFILE_BALANCED,
                    ENGINE_BYTECODE_PTA,
                    BytecodeMainlineCallGraphRunner.MODE_BALANCED_V1,
                    true
            );
            case PROFILE_PRECISION -> new CallGraphPlan(
                    PROFILE_PRECISION,
                    ENGINE_BYTECODE_PTA,
                    BytecodeMainlineCallGraphRunner.MODE_PRECISION_V1,
                    true
            );
            default -> throw new IllegalArgumentException(
                    "invalid " + CALL_GRAPH_PROFILE_PROP + "=" + profile
                            + ", supported: fast|balanced|precision"
            );
        };
    }

    private static CallGraphPlan balancedBytecode() {
        return new CallGraphPlan(
                PROFILE_BALANCED,
                ENGINE_BYTECODE_PTA,
                BytecodeMainlineCallGraphRunner.MODE_BALANCED_V1,
                true
        );
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
