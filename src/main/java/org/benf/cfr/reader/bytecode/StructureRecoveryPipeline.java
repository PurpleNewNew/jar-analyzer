package org.benf.cfr.reader.bytecode;

import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.StructuredConditionLocalRecovery;
import org.benf.cfr.reader.util.getopt.OptionsImpl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

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
                                inlinePossiblesPass(),
                                removeStructuredGotosPass(),
                                rewriteConditionLocalAliasesPass()
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
                                prettifyBadLoopsPass(),
                                patternSemanticsPass(patternSemanticsRewriter),
                                inlinePossiblesPass(),
                                removeStructuredGotosPass()
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
                                prettifyBadLoopsPass(),
                                patternSemanticsPass(patternSemanticsRewriter),
                                inlinePossiblesPass(),
                                removeStructuredGotosPass()
                        ),
                        RecoverySubphase.of(
                                rewriteConditionLocalAliasesPass(),
                                removePointlessBlocksPass(),
                                removePointlessReturnPass(),
                                removePointlessControlFlowPass(),
                                removePrimitiveDeconversionPass(),
                                insertLabelledBlocksPass(),
                                removeUnnecessaryLabelledBreaksPass(),
                                flattenNonReferencedBlocksPass(),
                                cleanupStructuredExpressionBodiesPass()
                        )
                )
        );
        this.outputPolish = RecoveryPhase.singlePass(
                "output-polish",
                PhaseInputRequirement.FULLY_STRUCTURED,
                List.of(
                        RecoverySubphase.of(
                                removePrimitiveDeconversionPass()
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
                tidyEmptyCatchPass(),
                tidyTryCatchPass(),
                convertUnstructuredIfPass(),
                rewriteForwardIfGotosPass(),
                rewriteConditionLocalAliasesPass()
        );
    }

    private static RecoverySubphase structuralCleanupSubphase() {
        return RecoverySubphase.of(
                rewriteConditionLocalAliasesPass(),
                removePointlessBlocksPass(),
                removePointlessReturnPass(),
                removePointlessControlFlowPass(),
                removePrimitiveDeconversionPass(),
                insertLabelledBlocksPass(),
                removeUnnecessaryLabelledBreaksPass(),
                flattenNonReferencedBlocksPass()
        );
    }

    private static StructureRecoveryPass tidyEmptyCatchPass() {
        return alwaysEnabledPass(
                "tidy-empty-catch",
                "Removes empty catch residue before later control-flow recovery.",
                true,
                true,
                (block, context) -> StructureRecoveryTransforms.tidyEmptyCatch(block)
        );
    }

    private static StructureRecoveryPass tidyTryCatchPass() {
        return alwaysEnabledPass(
                "tidy-try-catch",
                "Normalizes try/catch frontiers for downstream structuring.",
                true,
                true,
                (block, context) -> StructureRecoveryTransforms.tidyTryCatch(block),
                "tidy-empty-catch"
        );
    }

    private static StructureRecoveryPass convertUnstructuredIfPass() {
        return alwaysEnabledPass(
                "convert-unstructured-if",
                "Converts raw conditional frontiers into structured if forms.",
                true,
                true,
                (block, context) -> StructureRecoveryTransforms.convertUnstructuredIf(block),
                "tidy-try-catch"
        );
    }

    private static StructureRecoveryPass rewriteForwardIfGotosPass() {
        return alwaysEnabledPass(
                "rewrite-forward-if-gotos",
                "Rewrites forward-goto guard residue into structured flow.",
                true,
                true,
                (block, context) -> StructureRecoveryTransforms.rewriteForwardIfGotos(block),
                "convert-unstructured-if"
        );
    }

    private static StructureRecoveryPass rewriteConditionLocalAliasesPass() {
        return alwaysEnabledPass(
                "rewrite-condition-local-aliases",
                "Normalizes condition-local aliases before cleanup and polish passes.",
                true,
                true,
                (block, context) -> StructuredConditionLocalRecovery.rewriteConditionLocalAliases(block),
                "rewrite-forward-if-gotos"
        );
    }

    private static StructureRecoveryPass prettifyBadLoopsPass() {
        return alwaysEnabledPass(
                "prettify-bad-loops",
                "Canonicalizes malformed loop shapes into structured loop statements.",
                true,
                true,
                (block, context) -> StructureRecoveryTransforms.prettifyBadLoops(block),
                "rewrite-forward-if-gotos"
        );
    }

    private static StructureRecoveryPass patternSemanticsPass(PatternSemanticsRewriter patternSemanticsRewriter) {
        return alwaysEnabledPass(
                "pattern-semantics",
                "Recovers pattern-matching structure before post-modern cleanup.",
                true,
                true,
                (block, context) -> patternSemanticsRewriter.rewrite(block, context.bytecodeMeta, context.structureRecoveryTrace),
                "prettify-bad-loops"
        );
    }

    private static StructureRecoveryPass inlinePossiblesPass() {
        return alwaysEnabledPass(
                "inline-possibles",
                "Inlines trivial nested blocks that would otherwise block later cleanup.",
                true,
                true,
                (block, context) -> StructureRecoveryTransforms.inlinePossibles(block)
        );
    }

    private static StructureRecoveryPass removeStructuredGotosPass() {
        return alwaysEnabledPass(
                "remove-structured-gotos",
                "Drops leftover structured goto markers once control-flow is recoverable.",
                true,
                true,
                (block, context) -> StructureRecoveryTransforms.removeStructuredGotos(block),
                "inline-possibles"
        );
    }

    private static StructureRecoveryPass removePointlessBlocksPass() {
        return alwaysEnabledPass(
                "remove-pointless-blocks",
                "Flattens redundant block wrappers without changing method semantics.",
                true,
                true,
                (block, context) -> StructureRecoveryTransforms.removePointlessBlocks(block),
                "remove-structured-gotos"
        );
    }

    private static StructureRecoveryPass removePointlessReturnPass() {
        return alwaysEnabledPass(
                "remove-pointless-return",
                "Removes redundant terminal returns after control-flow stabilizes.",
                true,
                true,
                (block, context) -> StructureRecoveryTransforms.removePointlessReturn(block),
                "remove-pointless-blocks"
        );
    }

    private static StructureRecoveryPass removePointlessControlFlowPass() {
        return alwaysEnabledPass(
                "remove-pointless-control-flow",
                "Deletes inert control-flow residue that no longer affects structure.",
                true,
                true,
                (block, context) -> StructureRecoveryTransforms.removePointlessControlFlow(block),
                "remove-pointless-return"
        );
    }

    private static StructureRecoveryPass removePrimitiveDeconversionPass() {
        return alwaysEnabledPass(
                "remove-primitive-deconversion",
                "Strips primitive boxing/deconversion residue after structure is stable.",
                true,
                false,
                (block, context) -> StructureRecoveryTransforms.removePrimitiveDeconversion(context.options, block)
        );
    }

    private static StructureRecoveryPass insertLabelledBlocksPass() {
        return conditionalPass(
                "insert-labelled-blocks",
                "Introduces labelled blocks only when labelled-block output is enabled.",
                true,
                true,
                (block, context) -> context.options.getOption(OptionsImpl.LABELLED_BLOCKS),
                (block, context) -> {
                    StructureRecoveryTransforms.insertLabelledBlocks(block);
                    StructureRecoveryTransforms.rewriteForwardIfGotos(block);
                },
                "remove-pointless-control-flow"
        );
    }

    private static StructureRecoveryPass removeUnnecessaryLabelledBreaksPass() {
        return alwaysEnabledPass(
                "remove-unnecessary-labelled-breaks",
                "Prunes labelled breaks that no longer carry control-flow meaning.",
                true,
                true,
                (block, context) -> StructureRecoveryTransforms.removeUnnecessaryLabelledBreaks(block),
                "insert-labelled-blocks"
        );
    }

    private static StructureRecoveryPass flattenNonReferencedBlocksPass() {
        return alwaysEnabledPass(
                "flatten-non-referenced-blocks",
                "Flattens unreferenced block nesting after labelled cleanup.",
                true,
                true,
                (block, context) -> StructureRecoveryTransforms.flattenNonReferencedBlocks(block),
                "remove-unnecessary-labelled-breaks"
        );
    }

    private static StructureRecoveryPass cleanupStructuredExpressionBodiesPass() {
        return alwaysEnabledPass(
                "cleanup-structured-expression-bodies",
                "Removes expression-body scaffolding once structured output is ready.",
                true,
                true,
                (block, context) -> StructureRecoveryTransforms.cleanupStructuredExpressionBodies(block),
                "flatten-non-referenced-blocks"
        );
    }

    private static StructureRecoveryPass alwaysEnabledPass(String name,
                                                           String outputPromise,
                                                           boolean idempotent,
                                                           boolean allowsStructuralChange,
                                                           BiConsumer<Op04StructuredStatement, MethodAnalysisContext> action,
                                                           String... dependencies) {
        return StructureRecoveryPass.alwaysEnabled(
                passDescriptor(name, outputPromise, idempotent, allowsStructuralChange, dependencies),
                action
        );
    }

    private static StructureRecoveryPass conditionalPass(String name,
                                                         String outputPromise,
                                                         boolean idempotent,
                                                         boolean allowsStructuralChange,
                                                         BiPredicate<Op04StructuredStatement, MethodAnalysisContext> enabled,
                                                         BiConsumer<Op04StructuredStatement, MethodAnalysisContext> action,
                                                         String... dependencies) {
        return StructureRecoveryPass.when(
                passDescriptor(name, outputPromise, idempotent, allowsStructuralChange, dependencies),
                enabled,
                action
        );
    }

    private static StructuredPassDescriptor passDescriptor(String name,
                                                           String outputPromise,
                                                           boolean idempotent,
                                                           boolean allowsStructuralChange,
                                                           String... dependencies) {
        return StructuredPassDescriptor.of(name, outputPromise, idempotent, allowsStructuralChange, dependencies);
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
            if (!context.capturesObservability()) {
                if (!inputRequirement.accepts(block)) {
                    return;
                }
                if (maxRounds == 1) {
                    for (RecoverySubphase subphase : subphases) {
                        subphase.run(block, context, phaseName, inputRequirement, null);
                    }
                    return;
                }
                runToFixedPointWithoutTracing(block, context);
                return;
            }
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

        private void runToFixedPointWithoutTracing(Op04StructuredStatement block,
                                                   MethodAnalysisContext context) {
            StructureRecoverySnapshot current = StructureRecoverySnapshot.capture(block);
            Set<StructureRecoverySnapshot> seen = new HashSet<StructureRecoverySnapshot>();
            seen.add(current);
            for (int round = 0; round < maxRounds && !current.isFullyStructured(); ++round) {
                for (RecoverySubphase subphase : subphases) {
                    subphase.run(block, context, phaseName, inputRequirement, null);
                }
                StructureRecoverySnapshot next = StructureRecoverySnapshot.capture(block);
                if (next.equals(current) || !seen.add(next)) {
                    return;
                }
                current = next;
            }
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
                if (roundTrace == null) {
                    if (pass.enabled(block, context)) {
                        pass.apply(block, context);
                    }
                    continue;
                }
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
