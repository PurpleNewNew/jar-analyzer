package org.benf.cfr.reader.bytecode;

import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.LValueTypeClashCheck;

import java.util.List;
import java.util.Set;

final class StructuredAnalysisChecks {
    private static final String PHASE = "analysis-checks";
    private static final String INPUT_REQUIREMENT = "any-structure-state";
    private static final StructuredPassEntry TYPE_CLASH_PASS = StructuredPassEntry.of(
            "analysis-check",
            PHASE,
            INPUT_REQUIREMENT,
            StructuredPassDescriptor.of(
                    "check-type-clashes",
                    "Detects liveness/type clashes without mutating structured output.",
                    true,
                    false
            )
    );

    private StructuredAnalysisChecks() {
    }

    static List<StructuredPassEntry> describePasses() {
        return List.of(TYPE_CLASH_PASS);
    }

    static boolean checkTypeClashes(Op04StructuredStatement block,
                                    BytecodeMeta bytecodeMeta,
                                    StructureRecoveryTrace trace) {
        StructureRecoverySnapshot before = StructureRecoverySnapshot.capture(block);
        StructureRecoveryTrace.PhaseTrace phaseTrace = trace.beginPhase(PHASE, INPUT_REQUIREMENT, before);
        phaseTrace.recordInvariant("input-requirement", true, "accepted " + INPUT_REQUIREMENT + " with " + before);

        StructureRecoveryTrace.RoundTrace roundTrace = phaseTrace.beginRound(1, before);
        LValueTypeClashCheck clashCheck = new LValueTypeClashCheck();
        clashCheck.processOp04Statement(block);
        Set<Integer> clashes = clashCheck.getClashes();
        if (!clashes.isEmpty()) {
            bytecodeMeta.informLivenessClashes(clashes);
        }

        StructureRecoverySnapshot after = StructureRecoverySnapshot.capture(block);
        roundTrace.recordPass(TYPE_CLASH_PASS, true, before, after);
        boolean noStructuralDelta = !before.hasStructuralDelta(after);
        roundTrace.recordInvariant(
                "no-structural-delta",
                noStructuralDelta,
                "before=" + before + ", after=" + after
        );
        roundTrace.finish(after);
        phaseTrace.recordInvariant(
                "no-structural-delta",
                noStructuralDelta,
                "before=" + before + ", after=" + after
        );
        phaseTrace.finish(after);
        return !clashes.isEmpty();
    }
}
