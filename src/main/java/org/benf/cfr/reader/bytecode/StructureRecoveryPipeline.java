package org.benf.cfr.reader.bytecode;

import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.util.MiscStatementTools;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.AbstractUnStructuredStatement;
import org.benf.cfr.reader.util.getopt.OptionsImpl;

import java.util.List;

final class StructureRecoveryPipeline {
    private static final int MAX_HEAVY_RECOVERY_PASSES = 6;

    private final PatternSemanticsRewriter patternSemanticsRewriter;

    StructureRecoveryPipeline(PatternSemanticsRewriter patternSemanticsRewriter) {
        this.patternSemanticsRewriter = patternSemanticsRewriter;
    }

    void applyInitialCleanup(Op04StructuredStatement block, MethodAnalysisContext context) {
        applyCleanupRound(block, context, false);
    }

    void recoverToFixedPoint(Op04StructuredStatement block, MethodAnalysisContext context) {
        if (!context.bytecodeMeta.has(BytecodeMeta.CodeInfoFlag.SWITCHES)
                && !context.options.getOption(OptionsImpl.LABELLED_BLOCKS)) {
            return;
        }
        StructureRecoverySnapshot previous = StructureRecoverySnapshot.capture(block);
        int stalledPasses = 0;
        for (int pass = 0; pass < MAX_HEAVY_RECOVERY_PASSES && !block.isFullyStructured(); ++pass) {
            applyCleanupRound(block, context, true);
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

    void cleanupAfterModernSemantics(Op04StructuredStatement block, MethodAnalysisContext context) {
        if (!context.bytecodeMeta.has(BytecodeMeta.CodeInfoFlag.SWITCHES)
                && !context.modernFeatures.prefersPatternOutput()) {
            return;
        }
        applyCleanupRound(block, context, true);
        Op04StructuredStatement.cleanupStructuredExpressionBodies(block);
    }

    void applyOutputPolish(Op04StructuredStatement block, MethodAnalysisContext context) {
        Op04StructuredStatement.rewriteForwardIfGotos(block);
        Op04StructuredStatement.rewriteConditionLocalAliases(block);
        Op04StructuredStatement.removePointlessBlocks(block);
        Op04StructuredStatement.removePointlessControlFlow(block);
        Op04StructuredStatement.removePrimitiveDeconversion(context.options, context.method, block);
        Op04StructuredStatement.removeUnnecessaryLabelledBreaks(block);
        Op04StructuredStatement.flattenNonReferencedBlocks(block);
    }

    private void applyCleanupRound(Op04StructuredStatement block,
                                   MethodAnalysisContext context,
                                   boolean includePatternRecovery) {
        Op04StructuredStatement.tidyEmptyCatch(block);
        Op04StructuredStatement.tidyTryCatch(block);
        Op04StructuredStatement.convertUnstructuredIf(block);
        Op04StructuredStatement.rewriteForwardIfGotos(block);
        Op04StructuredStatement.rewriteConditionLocalAliases(block);
        if (includePatternRecovery) {
            Op04StructuredStatement.prettifyBadLoops(block);
            patternSemanticsRewriter.rewrite(block, context.bytecodeMeta);
        }
        Op04StructuredStatement.inlinePossibles(block);
        Op04StructuredStatement.removeStructuredGotos(block);
        Op04StructuredStatement.removePointlessBlocks(block);
        Op04StructuredStatement.removePointlessReturn(block);
        Op04StructuredStatement.removePointlessControlFlow(block);
        Op04StructuredStatement.removePrimitiveDeconversion(context.options, context.method, block);
        if (context.options.getOption(OptionsImpl.LABELLED_BLOCKS)) {
            Op04StructuredStatement.insertLabelledBlocks(block);
            Op04StructuredStatement.rewriteForwardIfGotos(block);
        }
        Op04StructuredStatement.removeUnnecessaryLabelledBreaks(block);
        Op04StructuredStatement.flattenNonReferencedBlocks(block);
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
