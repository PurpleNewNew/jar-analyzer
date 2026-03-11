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

import me.n1ar4.jar.analyzer.core.taie.TaieAnalysisRunner.AnalysisProfile;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.util.Locale;

public record CallGraphPlan(String analysisProfile,
                            String callGraphEngine,
                            String callGraphModeMeta,
                            boolean bytecodeMainline,
                            boolean selectivePta,
                            AnalysisProfile taieProfile) {
    public static final String CALL_GRAPH_PROFILE_PROP = "jar.analyzer.callgraph.profile";
    public static final String ENGINE_BYTECODE = BytecodeMainlineCallGraphRunner.ENGINE;
    public static final String ENGINE_ORACLE_TAIE = "oracle-taie";
    public static final String ENGINE_BYTECODE_PTA = "bytecode-mainline+pta-refine";

    public static final String PROFILE_FAST = "fast";
    public static final String PROFILE_BALANCED = "balanced";
    public static final String PROFILE_PRECISION = "precision";
    public static final String PROFILE_ORACLE_TAIE = "oracle-taie";

    private static final Logger logger = LogManager.getLogger();

    public static CallGraphPlan resolve(String engineSetting,
                                        String profileSetting,
                                        AnalysisProfile taieProfile) {
        AnalysisProfile resolvedTaiE = taieProfile == null ? AnalysisProfile.fromSystemProperty() : taieProfile;
        String engine = normalize(engineSetting);
        if (!engine.isBlank()) {
            return fromEngine(engine, resolvedTaiE);
        }
        String profile = normalize(profileSetting);
        if (!profile.isBlank()) {
            return fromProfile(profile, resolvedTaiE);
        }
        return balancedBytecode(resolvedTaiE);
    }

    public BytecodeMainlineCallGraphRunner.Settings bytecodeSettings() {
        if (!bytecodeMainline) {
            return BytecodeMainlineCallGraphRunner.Settings.legacySemanticV1();
        }
        return new BytecodeMainlineCallGraphRunner.Settings(callGraphModeMeta, selectivePta);
    }

    private static CallGraphPlan fromEngine(String engine, AnalysisProfile taieProfile) {
        return switch (engine) {
            case "taie" -> throw new IllegalArgumentException(
                    "call graph engine=taie was removed; use oracle-taie as the explicit oracle path"
            );
            case ENGINE_BYTECODE -> new CallGraphPlan(
                    PROFILE_BALANCED,
                    ENGINE_BYTECODE,
                    BytecodeMainlineCallGraphRunner.MODE_SEMANTIC_V1,
                    true,
                    true,
                    taieProfile
            );
            case ENGINE_BYTECODE_PTA -> new CallGraphPlan(
                    PROFILE_BALANCED,
                    ENGINE_BYTECODE_PTA,
                    BytecodeMainlineCallGraphRunner.MODE_BALANCED_V1,
                    true,
                    true,
                    taieProfile
            );
            case ENGINE_ORACLE_TAIE -> oracleTaie(taieProfile);
            default -> {
                logger.warn("unknown call graph engine setting: {} (fallback to bytecode balanced)", engine);
                yield balancedBytecode(taieProfile);
            }
        };
    }

    private static CallGraphPlan fromProfile(String profile, AnalysisProfile taieProfile) {
        return switch (profile) {
            case PROFILE_FAST -> new CallGraphPlan(
                    PROFILE_FAST,
                    ENGINE_BYTECODE,
                    BytecodeMainlineCallGraphRunner.MODE_FAST_V1,
                    true,
                    false,
                    taieProfile
            );
            case PROFILE_BALANCED -> new CallGraphPlan(
                    PROFILE_BALANCED,
                    ENGINE_BYTECODE_PTA,
                    BytecodeMainlineCallGraphRunner.MODE_BALANCED_V1,
                    true,
                    true,
                    taieProfile
            );
            case PROFILE_PRECISION -> new CallGraphPlan(
                    PROFILE_PRECISION,
                    ENGINE_BYTECODE_PTA,
                    BytecodeMainlineCallGraphRunner.MODE_PRECISION_V1,
                    true,
                    true,
                    taieProfile
            );
            case PROFILE_ORACLE_TAIE -> oracleTaie(taieProfile);
            default -> {
                logger.warn("unknown call graph profile setting: {} (fallback to bytecode balanced)", profile);
                yield balancedBytecode(taieProfile);
            }
        };
    }

    private static CallGraphPlan balancedBytecode(AnalysisProfile taieProfile) {
        return new CallGraphPlan(
                PROFILE_BALANCED,
                ENGINE_BYTECODE_PTA,
                BytecodeMainlineCallGraphRunner.MODE_BALANCED_V1,
                true,
                true,
                taieProfile
        );
    }

    private static CallGraphPlan oracleTaie(AnalysisProfile taieProfile) {
        String profile = taieProfile == null ? PROFILE_BALANCED : taieProfile.value();
        return new CallGraphPlan(PROFILE_ORACLE_TAIE, ENGINE_ORACLE_TAIE, "oracle-taie:" + profile, false, false, taieProfile);
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
