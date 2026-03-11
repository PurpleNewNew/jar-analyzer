package me.n1ar4.jar.analyzer.core;

import me.n1ar4.jar.analyzer.core.taie.TaieAnalysisRunner.AnalysisProfile;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CallGraphPlanTest {
    @Test
    void defaultPlanShouldUseBalancedBytecodePipeline() {
        CallGraphPlan plan = CallGraphPlan.resolve("", "", AnalysisProfile.BALANCED);

        assertEquals(CallGraphPlan.PROFILE_BALANCED, plan.analysisProfile());
        assertEquals(CallGraphPlan.ENGINE_BYTECODE_PTA, plan.callGraphEngine());
        assertEquals(BytecodeMainlineCallGraphRunner.MODE_BALANCED_V1, plan.callGraphModeMeta());
        assertTrue(plan.bytecodeMainline());
        assertTrue(plan.selectivePta());
    }

    @Test
    void fastProfileShouldUseBytecodeMainlineWithoutPta() {
        CallGraphPlan plan = CallGraphPlan.resolve("", CallGraphPlan.PROFILE_FAST, AnalysisProfile.BALANCED);

        assertEquals(CallGraphPlan.PROFILE_FAST, plan.analysisProfile());
        assertEquals(CallGraphPlan.ENGINE_BYTECODE, plan.callGraphEngine());
        assertEquals(BytecodeMainlineCallGraphRunner.MODE_FAST_V1, plan.callGraphModeMeta());
        assertTrue(plan.bytecodeMainline());
        assertFalse(plan.selectivePta());
    }

    @Test
    void balancedProfileShouldUsePtaRefinePipeline() {
        CallGraphPlan plan = CallGraphPlan.resolve("", CallGraphPlan.PROFILE_BALANCED, AnalysisProfile.BALANCED);

        assertEquals(CallGraphPlan.PROFILE_BALANCED, plan.analysisProfile());
        assertEquals(CallGraphPlan.ENGINE_BYTECODE_PTA, plan.callGraphEngine());
        assertEquals(BytecodeMainlineCallGraphRunner.MODE_BALANCED_V1, plan.callGraphModeMeta());
        assertTrue(plan.bytecodeMainline());
        assertTrue(plan.selectivePta());
    }

    @Test
    void oracleProfileShouldKeepTaiEAsOracleOnly() {
        CallGraphPlan plan = CallGraphPlan.resolve("", CallGraphPlan.PROFILE_ORACLE_TAIE, AnalysisProfile.HIGH);

        assertEquals(CallGraphPlan.PROFILE_ORACLE_TAIE, plan.analysisProfile());
        assertEquals(CallGraphPlan.ENGINE_ORACLE_TAIE, plan.callGraphEngine());
        assertEquals("oracle-taie:high", plan.callGraphModeMeta());
        assertFalse(plan.bytecodeMainline());
        assertEquals(AnalysisProfile.HIGH, plan.taieProfile());
    }

    @Test
    void explicitEngineShouldOverrideProfileSwitch() {
        CallGraphPlan plan = CallGraphPlan.resolve(
                CallGraphPlan.ENGINE_BYTECODE,
                CallGraphPlan.PROFILE_ORACLE_TAIE,
                AnalysisProfile.FAST
        );

        assertEquals(CallGraphPlan.PROFILE_BALANCED, plan.analysisProfile());
        assertEquals(CallGraphPlan.ENGINE_BYTECODE, plan.callGraphEngine());
        assertEquals(BytecodeMainlineCallGraphRunner.MODE_SEMANTIC_V1, plan.callGraphModeMeta());
        assertTrue(plan.bytecodeMainline());
        assertTrue(plan.selectivePta());
    }

    @Test
    void removedTaieEngineShouldFailFast() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> CallGraphPlan.resolve("taie", "", AnalysisProfile.HIGH)
        );
        assertTrue(ex.getMessage().contains("oracle-taie"));
    }
}
