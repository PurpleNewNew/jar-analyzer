package org.benf.cfr.reader.bytecode;

import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.util.MiscStatementTools;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.AbstractUnStructuredStatement;
import org.benf.cfr.reader.util.getopt.OptionsImpl;

import java.util.ArrayList;
import java.util.List;

final class StructureRecoveryPipeline {
    private static final int MAX_HEAVY_RECOVERY_PASSES = 6;

    private final RecoveryPassGroup initialCleanup;
    private final RecoveryPassGroup heavyRecovery;
    private final RecoveryPassGroup postModernCleanup;
    private final RecoveryPassGroup outputPolish;

    StructureRecoveryPipeline(PatternSemanticsRewriter patternSemanticsRewriter) {
        this.initialCleanup = new RecoveryPassGroup(commonRecoveryPasses(false, false, patternSemanticsRewriter));
        this.heavyRecovery = new RecoveryPassGroup(commonRecoveryPasses(true, false, patternSemanticsRewriter));
        this.postModernCleanup = new RecoveryPassGroup(commonRecoveryPasses(true, true, patternSemanticsRewriter));
        this.outputPolish = new RecoveryPassGroup(outputPolishPasses());
    }

    void applyInitialCleanup(Op04StructuredStatement block, MethodAnalysisContext context) {
        initialCleanup.run(block, context);
    }

    void recoverToFixedPoint(Op04StructuredStatement block, MethodAnalysisContext context) {
        if (!context.bytecodeMeta.has(BytecodeMeta.CodeInfoFlag.SWITCHES)
                && !context.options.getOption(OptionsImpl.LABELLED_BLOCKS)) {
            return;
        }
        heavyRecovery.runToFixedPoint(block, context, MAX_HEAVY_RECOVERY_PASSES);
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

    private List<StructureRecoveryPass> commonRecoveryPasses(boolean includeLoopPrettifier,
                                                             boolean includeStructuredExpressionCleanup,
                                                             PatternSemanticsRewriter patternSemanticsRewriter) {
        List<StructureRecoveryPass> passes = new ArrayList<StructureRecoveryPass>();
        passes.add(new StructureRecoveryPasses.TidyEmptyCatchPass());
        passes.add(new StructureRecoveryPasses.TidyTryCatchPass());
        passes.add(new StructureRecoveryPasses.ConvertUnstructuredIfPass());
        passes.add(new StructureRecoveryPasses.RewriteForwardIfGotosPass());
        passes.add(new StructureRecoveryPasses.RewriteConditionLocalAliasesPass());
        if (includeLoopPrettifier) {
            passes.add(new StructureRecoveryPasses.PrettifyBadLoopsPass());
            passes.add(new StructureRecoveryPasses.PatternSemanticsPass(patternSemanticsRewriter));
        }
        passes.add(new StructureRecoveryPasses.InlinePossiblesPass());
        passes.add(new StructureRecoveryPasses.RemoveStructuredGotosPass());
        passes.add(new StructureRecoveryPasses.RemovePointlessBlocksPass());
        passes.add(new StructureRecoveryPasses.RemovePointlessReturnPass());
        passes.add(new StructureRecoveryPasses.RemovePointlessControlFlowPass());
        passes.add(new StructureRecoveryPasses.RemovePrimitiveDeconversionPass());
        passes.add(new StructureRecoveryPasses.InsertLabelledBlocksPass());
        passes.add(new StructureRecoveryPasses.RemoveUnnecessaryLabelledBreaksPass());
        passes.add(new StructureRecoveryPasses.FlattenNonReferencedBlocksPass());
        if (includeStructuredExpressionCleanup) {
            passes.add(new StructureRecoveryPasses.CleanupStructuredExpressionBodiesPass());
        }
        return passes;
    }

    private List<StructureRecoveryPass> outputPolishPasses() {
        List<StructureRecoveryPass> passes = new ArrayList<StructureRecoveryPass>();
        passes.add(new StructureRecoveryPasses.RewriteForwardIfGotosPass());
        passes.add(new StructureRecoveryPasses.RewriteConditionLocalAliasesPass());
        passes.add(new StructureRecoveryPasses.RemovePointlessBlocksPass());
        passes.add(new StructureRecoveryPasses.RemovePointlessControlFlowPass());
        passes.add(new StructureRecoveryPasses.RemovePrimitiveDeconversionPass());
        passes.add(new StructureRecoveryPasses.RemoveUnnecessaryLabelledBreaksPass());
        passes.add(new StructureRecoveryPasses.FlattenNonReferencedBlocksPass());
        return passes;
    }

    private static final class RecoveryPassGroup {
        private final List<StructureRecoveryPass> passes;

        private RecoveryPassGroup(List<StructureRecoveryPass> passes) {
            this.passes = passes;
        }

        void run(Op04StructuredStatement block, MethodAnalysisContext context) {
            for (StructureRecoveryPass pass : passes) {
                if (pass.enabled(block, context)) {
                    pass.apply(block, context);
                }
            }
        }

        void runToFixedPoint(Op04StructuredStatement block,
                             MethodAnalysisContext context,
                             int maxPasses) {
            StructureRecoverySnapshot previous = StructureRecoverySnapshot.capture(block);
            int stalledPasses = 0;
            for (int pass = 0; pass < maxPasses && !block.isFullyStructured(); ++pass) {
                run(block, context);
                StructureRecoverySnapshot current = StructureRecoverySnapshot.capture(block);
                boolean improved = current.hasImprovedOver(previous);
                if (!improved) {
                    if (current.sameShapeAs(previous) || ++stalledPasses >= 2) {
                        break;
                    }
                } else {
                    stalledPasses = 0;
                }
                previous = current;
            }
        }
    }

    private static final class StructureRecoverySnapshot {
        private final boolean fullyStructured;
        private final int totalStatements;
        private final int unstructuredStatements;
        private final int nopStatements;
        private final int shapeHash;

        private StructureRecoverySnapshot(boolean fullyStructured,
                                          int totalStatements,
                                          int unstructuredStatements,
                                          int nopStatements,
                                          int shapeHash) {
            this.fullyStructured = fullyStructured;
            this.totalStatements = totalStatements;
            this.unstructuredStatements = unstructuredStatements;
            this.nopStatements = nopStatements;
            this.shapeHash = shapeHash;
        }

        static StructureRecoverySnapshot capture(Op04StructuredStatement block) {
            List<StructuredStatement> statements = MiscStatementTools.linearise(block);
            if (statements == null) {
                return new StructureRecoverySnapshot(block.isFullyStructured(), -1, -1, -1, 0);
            }
            int unstructured = 0;
            int nops = 0;
            int shapeHash = 1;
            for (StructuredStatement statement : statements) {
                if (statement instanceof AbstractUnStructuredStatement) {
                    ++unstructured;
                }
                if (statement.isEffectivelyNOP()) {
                    ++nops;
                }
                shapeHash = 31 * shapeHash + statement.getClass().getName().hashCode();
                shapeHash = 31 * shapeHash + (statement.isEffectivelyNOP() ? 1 : 0);
            }
            return new StructureRecoverySnapshot(block.isFullyStructured(), statements.size(), unstructured, nops, shapeHash);
        }

        boolean hasImprovedOver(StructureRecoverySnapshot other) {
            if (fullyStructured && !other.fullyStructured) {
                return true;
            }
            if (unstructuredStatements >= 0 && other.unstructuredStatements >= 0 && unstructuredStatements < other.unstructuredStatements) {
                return true;
            }
            if (nopStatements >= 0 && other.nopStatements >= 0 && nopStatements < other.nopStatements) {
                return true;
            }
            return totalStatements >= 0
                    && other.totalStatements >= 0
                    && totalStatements < other.totalStatements
                    && unstructuredStatements <= other.unstructuredStatements;
        }

        boolean sameShapeAs(StructureRecoverySnapshot other) {
            return fullyStructured == other.fullyStructured
                    && totalStatements == other.totalStatements
                    && unstructuredStatements == other.unstructuredStatements
                    && nopStatements == other.nopStatements
                    && shapeHash == other.shapeHash;
        }
    }
}
