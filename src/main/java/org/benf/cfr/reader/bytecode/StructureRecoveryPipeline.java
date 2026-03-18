package org.benf.cfr.reader.bytecode;

import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class StructureRecoveryPipeline {
    private static final int MAX_CONTROL_FLOW_RECOVERY_ROUNDS = 6;

    private final RecoveryPhase initialCleanup;
    private final RecoveryPhase controlFlowRecovery;
    private final RecoveryPhase postModernCleanup;
    private final RecoveryPhase outputPolish;

    StructureRecoveryPipeline(PatternSemanticsRewriter patternSemanticsRewriter) {
        this.initialCleanup = RecoveryPhase.singlePass(
                "initial-cleanup",
                PhaseInputRequirement.ANY,
                List.of(
                        frontierNormalizationSubphase(),
                        RecoverySubphase.of(
                                new StructureRecoveryPasses.InlinePossiblesPass(),
                                new StructureRecoveryPasses.RemoveStructuredGotosPass(),
                                new StructureRecoveryPasses.RewriteConditionLocalAliasesPass()
                        ),
                        structuralCleanupSubphase()
                )
        );
        this.controlFlowRecovery = RecoveryPhase.toFixedPoint(
                "control-flow-recovery",
                PhaseInputRequirement.ANY,
                MAX_CONTROL_FLOW_RECOVERY_ROUNDS,
                List.of(
                        frontierNormalizationSubphase(),
                        RecoverySubphase.of(
                                new StructureRecoveryPasses.PrettifyBadLoopsPass(),
                                new StructureRecoveryPasses.PatternSemanticsPass(patternSemanticsRewriter),
                                new StructureRecoveryPasses.InlinePossiblesPass(),
                                new StructureRecoveryPasses.RemoveStructuredGotosPass()
                        ),
                        structuralCleanupSubphase()
                )
        );
        this.postModernCleanup = RecoveryPhase.singlePass(
                "post-modern-cleanup",
                PhaseInputRequirement.FULLY_STRUCTURED,
                List.of(
                        frontierNormalizationSubphase(),
                        RecoverySubphase.of(
                                new StructureRecoveryPasses.PrettifyBadLoopsPass(),
                                new StructureRecoveryPasses.PatternSemanticsPass(patternSemanticsRewriter),
                                new StructureRecoveryPasses.InlinePossiblesPass(),
                                new StructureRecoveryPasses.RemoveStructuredGotosPass()
                        ),
                        RecoverySubphase.of(
                                new StructureRecoveryPasses.RewriteConditionLocalAliasesPass(),
                                new StructureRecoveryPasses.RemovePointlessBlocksPass(),
                                new StructureRecoveryPasses.RemovePointlessReturnPass(),
                                new StructureRecoveryPasses.RemovePointlessControlFlowPass(),
                                new StructureRecoveryPasses.RemovePrimitiveDeconversionPass(),
                                new StructureRecoveryPasses.InsertLabelledBlocksPass(),
                                new StructureRecoveryPasses.RemoveUnnecessaryLabelledBreaksPass(),
                                new StructureRecoveryPasses.FlattenNonReferencedBlocksPass(),
                                new StructureRecoveryPasses.CleanupStructuredExpressionBodiesPass()
                        )
                )
        );
        this.outputPolish = RecoveryPhase.singlePass(
                "output-polish",
                PhaseInputRequirement.FULLY_STRUCTURED,
                List.of(
                        RecoverySubphase.of(
                                new StructureRecoveryPasses.RemovePrimitiveDeconversionPass()
                        )
                )
        );
    }

    void applyInitialCleanup(Op04StructuredStatement block, MethodAnalysisContext context) {
        initialCleanup.run(block, context);
    }

    void recoverToFixedPoint(Op04StructuredStatement block, MethodAnalysisContext context) {
        controlFlowRecovery.run(block, context);
    }

    void cleanupAfterModernSemantics(Op04StructuredStatement block, MethodAnalysisContext context) {
        if (!context.bytecodeMeta.has(BytecodeMeta.CodeInfoFlag.SWITCHES)
                && !context.modernFeatures.prefersPatternOutput()) {
            return;
        }
        postModernCleanup.run(block, context);
    }

    void applyOutputPolish(Op04StructuredStatement block, MethodAnalysisContext context) {
        outputPolish.run(block, context);
    }

    List<StructuredPassEntry> describePasses() {
        java.util.ArrayList<StructuredPassEntry> entries = new java.util.ArrayList<StructuredPassEntry>();
        entries.addAll(initialCleanup.describePasses());
        entries.addAll(controlFlowRecovery.describePasses());
        entries.addAll(postModernCleanup.describePasses());
        entries.addAll(outputPolish.describePasses());
        return List.copyOf(entries);
    }

    private static RecoverySubphase frontierNormalizationSubphase() {
        // Condition/if frontier cleanup is the one piece shared by all recovery stages: later passes assume
        // they see normalized guards instead of raw forward-goto residue.
        return RecoverySubphase.of(
                new StructureRecoveryPasses.TidyEmptyCatchPass(),
                new StructureRecoveryPasses.TidyTryCatchPass(),
                new StructureRecoveryPasses.ConvertUnstructuredIfPass(),
                new StructureRecoveryPasses.RewriteForwardIfGotosPass(),
                new StructureRecoveryPasses.RewriteConditionLocalAliasesPass()
        );
    }

    private static RecoverySubphase structuralCleanupSubphase() {
        return RecoverySubphase.of(
                new StructureRecoveryPasses.RewriteConditionLocalAliasesPass(),
                new StructureRecoveryPasses.RemovePointlessBlocksPass(),
                new StructureRecoveryPasses.RemovePointlessReturnPass(),
                new StructureRecoveryPasses.RemovePointlessControlFlowPass(),
                new StructureRecoveryPasses.RemovePrimitiveDeconversionPass(),
                new StructureRecoveryPasses.InsertLabelledBlocksPass(),
                new StructureRecoveryPasses.RemoveUnnecessaryLabelledBreaksPass(),
                new StructureRecoveryPasses.FlattenNonReferencedBlocksPass()
        );
    }

    private enum PhaseInputRequirement {
        ANY {
            @Override
            boolean accepts(Op04StructuredStatement block) {
                return true;
            }
        },
        FULLY_STRUCTURED {
            @Override
            boolean accepts(Op04StructuredStatement block) {
                return block.isFullyStructured();
            }
        };

        abstract boolean accepts(Op04StructuredStatement block);

        String describe() {
            return this == ANY ? "any-structure-state" : "fully-structured";
        }
    }

    private static final class RecoveryPhase {
        private final String phaseName;
        private final PhaseInputRequirement inputRequirement;
        private final List<RecoverySubphase> subphases;
        private final int maxRounds;

        private RecoveryPhase(String phaseName,
                              PhaseInputRequirement inputRequirement,
                              List<RecoverySubphase> subphases,
                              int maxRounds) {
            this.phaseName = phaseName;
            this.inputRequirement = inputRequirement;
            this.subphases = subphases;
            this.maxRounds = maxRounds;
        }

        static RecoveryPhase singlePass(String phaseName,
                                        PhaseInputRequirement inputRequirement,
                                        List<RecoverySubphase> subphases) {
            return new RecoveryPhase(phaseName, inputRequirement, subphases, 1);
        }

        static RecoveryPhase toFixedPoint(String phaseName,
                                          PhaseInputRequirement inputRequirement,
                                          int maxRounds,
                                          List<RecoverySubphase> subphases) {
            return new RecoveryPhase(phaseName, inputRequirement, subphases, maxRounds);
        }

        void run(Op04StructuredStatement block, MethodAnalysisContext context) {
            StructureRecoverySnapshot before = StructureRecoverySnapshot.capture(block);
            if (!inputRequirement.accepts(block)) {
                context.structureRecoveryTrace.recordSkippedPhase(
                        phaseName,
                        inputRequirement.describe(),
                        before,
                        "input-requirement-not-met"
                );
                return;
            }
            StructureRecoveryTrace.PhaseTrace phaseTrace = context.structureRecoveryTrace.beginPhase(
                    phaseName,
                    inputRequirement.describe(),
                    before
            );
            phaseTrace.recordInvariant(
                    "input-requirement",
                    true,
                    "accepted " + inputRequirement.describe() + " with " + before
            );
            if (maxRounds == 1) {
                runRound(block, context, phaseTrace, 1);
                StructureRecoverySnapshot after = StructureRecoverySnapshot.capture(block);
                verifyPhaseInvariants(phaseTrace, before, after);
                phaseTrace.finish(after);
                return;
            }
            runToFixedPoint(block, context, phaseTrace);
        }

        List<StructuredPassEntry> describePasses() {
            java.util.ArrayList<StructuredPassEntry> entries = new java.util.ArrayList<StructuredPassEntry>();
            for (RecoverySubphase subphase : subphases) {
                for (StructureRecoveryPass pass : subphase.passes) {
                    entries.add(StructuredPassEntry.of(
                            "structure-recovery",
                            phaseName,
                            inputRequirement.describe(),
                            pass.descriptor()
                    ));
                }
            }
            return List.copyOf(entries);
        }

        private void runToFixedPoint(Op04StructuredStatement block,
                                     MethodAnalysisContext context,
                                     StructureRecoveryTrace.PhaseTrace phaseTrace) {
            StructureRecoverySnapshot current = StructureRecoverySnapshot.capture(block);
            Set<StructureRecoverySnapshot> seen = new HashSet<StructureRecoverySnapshot>();
            seen.add(current);
            for (int round = 0; round < maxRounds && !current.isFullyStructured(); ++round) {
                runRound(block, context, phaseTrace, round + 1);
                StructureRecoverySnapshot next = StructureRecoverySnapshot.capture(block);
                // A recovery round must either expose a new state or finish structuring the block; repeating a
                // previous snapshot means later rounds in this phase will only replay the same rewrites.
                if (next.equals(current) || !seen.add(next)) {
                    verifyPhaseInvariants(phaseTrace, phaseTrace.getBefore(), next);
                    phaseTrace.finish(next);
                    return;
                }
                current = next;
            }
            verifyPhaseInvariants(phaseTrace, phaseTrace.getBefore(), current);
            phaseTrace.finish(current);
        }

        private void runRound(Op04StructuredStatement block,
                              MethodAnalysisContext context,
                              StructureRecoveryTrace.PhaseTrace phaseTrace,
                              int roundNumber) {
            StructureRecoverySnapshot roundBefore = StructureRecoverySnapshot.capture(block);
            StructureRecoveryTrace.RoundTrace roundTrace = phaseTrace.beginRound(roundNumber, roundBefore);
            for (RecoverySubphase subphase : subphases) {
                subphase.run(block, context, phaseName, inputRequirement, roundTrace);
            }
            StructureRecoverySnapshot roundAfter = StructureRecoverySnapshot.capture(block);
            verifyRoundInvariants(roundTrace, roundBefore, roundAfter);
            roundTrace.finish(roundAfter);
        }

        private void verifyPhaseInvariants(StructureRecoveryTrace.PhaseTrace phaseTrace,
                                           StructureRecoverySnapshot before,
                                           StructureRecoverySnapshot after) {
            if ("output-polish".equals(phaseName)) {
                boolean passed = !before.hasStructuralDelta(after);
                phaseTrace.recordInvariant(
                        "no-structural-delta",
                        passed,
                        "before=" + before + ", after=" + after
                );
            }
        }

        private void verifyRoundInvariants(StructureRecoveryTrace.RoundTrace roundTrace,
                                           StructureRecoverySnapshot before,
                                           StructureRecoverySnapshot after) {
            if ("control-flow-recovery".equals(phaseName)) {
                boolean passed = after.getUnstructuredStatements() <= before.getUnstructuredStatements();
                roundTrace.recordInvariant(
                        "unstructured-non-increasing",
                        passed,
                        "before=" + before.getUnstructuredStatements() + ", after=" + after.getUnstructuredStatements()
                );
            }
        }
    }

    private static final class RecoverySubphase {
        private final List<StructureRecoveryPass> passes;

        private RecoverySubphase(List<StructureRecoveryPass> passes) {
            this.passes = passes;
        }

        static RecoverySubphase of(StructureRecoveryPass... passes) {
            return new RecoverySubphase(List.of(passes));
        }

        void run(Op04StructuredStatement block,
                 MethodAnalysisContext context,
                 String phaseName,
                 PhaseInputRequirement inputRequirement,
                 StructureRecoveryTrace.RoundTrace roundTrace) {
            for (StructureRecoveryPass pass : passes) {
                StructuredPassEntry passEntry = StructuredPassEntry.of(
                        "structure-recovery",
                        phaseName,
                        inputRequirement.describe(),
                        pass.descriptor()
                );
                StructureRecoverySnapshot before = StructureRecoverySnapshot.capture(block);
                boolean enabled = pass.enabled(block, context);
                if (enabled) {
                    pass.apply(block, context);
                }
                roundTrace.recordPass(passEntry, enabled, before, StructureRecoverySnapshot.capture(block));
            }
        }
    }
}
