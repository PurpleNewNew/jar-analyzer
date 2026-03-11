package me.n1ar4.jar.analyzer.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CallGraphPlanTest {
    @Test
    void defaultPlanShouldUseBalancedBytecodePipeline() {
        CallGraphPlan plan = CallGraphPlan.resolve("", "");

        assertEquals(CallGraphPlan.PROFILE_BALANCED, plan.analysisProfile());
        assertEquals(CallGraphPlan.ENGINE_BYTECODE_PTA, plan.callGraphEngine());
        assertEquals(BytecodeMainlineCallGraphRunner.MODE_BALANCED_V1, plan.callGraphModeMeta());
        assertTrue(plan.selectivePta());
    }

    @Test
    void fastProfileShouldUseBytecodeMainlineWithoutPta() {
        CallGraphPlan plan = CallGraphPlan.resolve("", CallGraphPlan.PROFILE_FAST);

        assertEquals(CallGraphPlan.PROFILE_FAST, plan.analysisProfile());
        assertEquals(CallGraphPlan.ENGINE_BYTECODE, plan.callGraphEngine());
        assertEquals(BytecodeMainlineCallGraphRunner.MODE_FAST_V1, plan.callGraphModeMeta());
        assertFalse(plan.selectivePta());
    }

    @Test
    void balancedProfileShouldUsePtaRefinePipeline() {
        CallGraphPlan plan = CallGraphPlan.resolve("", CallGraphPlan.PROFILE_BALANCED);

        assertEquals(CallGraphPlan.PROFILE_BALANCED, plan.analysisProfile());
        assertEquals(CallGraphPlan.ENGINE_BYTECODE_PTA, plan.callGraphEngine());
        assertEquals(BytecodeMainlineCallGraphRunner.MODE_BALANCED_V1, plan.callGraphModeMeta());
        assertTrue(plan.selectivePta());
        assertFalse(plan.bytecodeSettings().precisionMode());
    }

    @Test
    void precisionProfileShouldUsePrecisionBudgetOnBytecodeMainline() {
        CallGraphPlan plan = CallGraphPlan.resolve("", CallGraphPlan.PROFILE_PRECISION);

        assertEquals(CallGraphPlan.PROFILE_PRECISION, plan.analysisProfile());
        assertEquals(CallGraphPlan.ENGINE_BYTECODE_PTA, plan.callGraphEngine());
        assertEquals(BytecodeMainlineCallGraphRunner.MODE_PRECISION_V1, plan.callGraphModeMeta());
        assertTrue(plan.selectivePta());
        assertTrue(plan.bytecodeSettings().precisionMode());
        assertEquals("precision", plan.bytecodeSettings().ptaBudgetProfile());
    }

    @Test
    void retiredOracleProfileShouldFailFast() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> CallGraphPlan.resolve("", "oracle-taie")
        );
        assertTrue(ex.getMessage().contains("no longer available"));
    }

    @Test
    void explicitEngineShouldOverrideProfileSwitch() {
        CallGraphPlan plan = CallGraphPlan.resolve(
                CallGraphPlan.ENGINE_BYTECODE,
                "oracle-taie"
        );

        assertEquals(CallGraphPlan.PROFILE_BALANCED, plan.analysisProfile());
        assertEquals(CallGraphPlan.ENGINE_BYTECODE, plan.callGraphEngine());
        assertEquals(BytecodeMainlineCallGraphRunner.MODE_SEMANTIC_V1, plan.callGraphModeMeta());
        assertTrue(plan.selectivePta());
    }

    @Test
    void removedTaieEngineShouldFailFast() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> CallGraphPlan.resolve("taie", "")
        );
        assertTrue(ex.getMessage().contains("bytecode-mainline"));
    }

    @Test
    void retiredOracleEngineShouldFailFast() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> CallGraphPlan.resolve("oracle-taie", "")
        );
        assertTrue(ex.getMessage().contains("no longer available"));
    }
}
