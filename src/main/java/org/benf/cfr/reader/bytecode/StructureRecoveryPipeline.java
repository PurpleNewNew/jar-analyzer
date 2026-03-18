package org.benf.cfr.reader.bytecode;

import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.util.MiscStatementTools;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.AbstractUnStructuredStatement;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

final class StructureRecoveryPipeline {
    private static final int MAX_CONTROL_FLOW_RECOVERY_ROUNDS = 6;

    private final RecoveryPhase initialCleanup;
    private final RecoveryPhase controlFlowRecovery;
    private final RecoveryPhase postModernCleanup;
    private final RecoveryPhase outputPolish;

    StructureRecoveryPipeline(PatternSemanticsRewriter patternSemanticsRewriter) {
        this.initialCleanup = RecoveryPhase.singlePass(
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
                PhaseInputRequirement.FULLY_STRUCTURED,
                List.of(
                        RecoverySubphase.of(
                                new StructureRecoveryPasses.RewriteForwardIfGotosPass(),
                                new StructureRecoveryPasses.RemovePointlessBlocksPass(),
                                new StructureRecoveryPasses.RemovePointlessControlFlowPass(),
                                new StructureRecoveryPasses.RemovePrimitiveDeconversionPass(),
                                new StructureRecoveryPasses.RemoveUnnecessaryLabelledBreaksPass(),
                                new StructureRecoveryPasses.FlattenNonReferencedBlocksPass()
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
    }

    private static final class RecoveryPhase {
        private final PhaseInputRequirement inputRequirement;
        private final List<RecoverySubphase> subphases;
        private final int maxRounds;

        private RecoveryPhase(PhaseInputRequirement inputRequirement,
                              List<RecoverySubphase> subphases,
                              int maxRounds) {
            this.inputRequirement = inputRequirement;
            this.subphases = subphases;
            this.maxRounds = maxRounds;
        }

        static RecoveryPhase singlePass(PhaseInputRequirement inputRequirement,
                                        List<RecoverySubphase> subphases) {
            return new RecoveryPhase(inputRequirement, subphases, 1);
        }

        static RecoveryPhase toFixedPoint(PhaseInputRequirement inputRequirement,
                                          int maxRounds,
                                          List<RecoverySubphase> subphases) {
            return new RecoveryPhase(inputRequirement, subphases, maxRounds);
        }

        void run(Op04StructuredStatement block, MethodAnalysisContext context) {
            if (!inputRequirement.accepts(block)) {
                return;
            }
            if (maxRounds == 1) {
                runRound(block, context);
                return;
            }
            runToFixedPoint(block, context);
        }

        private void runToFixedPoint(Op04StructuredStatement block, MethodAnalysisContext context) {
            StructureRecoverySnapshot current = StructureRecoverySnapshot.capture(block);
            Set<StructureRecoverySnapshot> seen = new HashSet<StructureRecoverySnapshot>();
            seen.add(current);
            for (int round = 0; round < maxRounds && !current.fullyStructured; ++round) {
                runRound(block, context);
                StructureRecoverySnapshot next = StructureRecoverySnapshot.capture(block);
                // A recovery round must either expose a new state or finish structuring the block; repeating a
                // previous snapshot means later rounds in this phase will only replay the same rewrites.
                if (next.equals(current) || !seen.add(next)) {
                    return;
                }
                current = next;
            }
        }

        private void runRound(Op04StructuredStatement block, MethodAnalysisContext context) {
            for (RecoverySubphase subphase : subphases) {
                subphase.run(block, context);
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

        void run(Op04StructuredStatement block, MethodAnalysisContext context) {
            for (StructureRecoveryPass pass : passes) {
                if (pass.enabled(block, context)) {
                    pass.apply(block, context);
                }
            }
        }
    }

    private static final class StructureRecoverySnapshot {
        private final boolean fullyStructured;
        private final int totalStatements;
        private final int unstructuredStatements;
        private final int nopStatements;
        private final int contentHash;

        private StructureRecoverySnapshot(boolean fullyStructured,
                                          int totalStatements,
                                          int unstructuredStatements,
                                          int nopStatements,
                                          int contentHash) {
            this.fullyStructured = fullyStructured;
            this.totalStatements = totalStatements;
            this.unstructuredStatements = unstructuredStatements;
            this.nopStatements = nopStatements;
            this.contentHash = contentHash;
        }

        static StructureRecoverySnapshot capture(Op04StructuredStatement block) {
            List<StructuredStatement> statements = MiscStatementTools.linearise(block);
            if (statements == null) {
                return new StructureRecoverySnapshot(block.isFullyStructured(), -1, -1, -1, block.toString().hashCode());
            }
            int unstructured = 0;
            int nops = 0;
            int contentHash = 1;
            for (StructuredStatement statement : statements) {
                if (statement instanceof AbstractUnStructuredStatement) {
                    ++unstructured;
                }
                if (statement.isEffectivelyNOP()) {
                    ++nops;
                }
                contentHash = 31 * contentHash + statement.getClass().getName().hashCode();
                contentHash = 31 * contentHash + (statement.isEffectivelyNOP() ? 1 : 0);
                contentHash = 31 * contentHash + statement.toString().hashCode();
            }
            return new StructureRecoverySnapshot(block.isFullyStructured(), statements.size(), unstructured, nops, contentHash);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof StructureRecoverySnapshot)) {
                return false;
            }
            StructureRecoverySnapshot other = (StructureRecoverySnapshot) o;
            return fullyStructured == other.fullyStructured
                    && totalStatements == other.totalStatements
                    && unstructuredStatements == other.unstructuredStatements
                    && nopStatements == other.nopStatements
                    && contentHash == other.contentHash;
        }

        @Override
        public int hashCode() {
            return Objects.hash(fullyStructured, totalStatements, unstructuredStatements, nopStatements, contentHash);
        }
    }
}
