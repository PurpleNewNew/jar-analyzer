package org.benf.cfr.reader.bytecode;

import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.util.MiscStatementTools;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.AbstractUnStructuredStatement;
import org.benf.cfr.reader.util.getopt.OptionsImpl;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

final class StructureRecoveryPipeline {
    private static final int MAX_HEAVY_RECOVERY_PASSES = 6;

    private final RecoveryPassGroup initialCleanup;
    private final RecoveryPassGroup heavyRecovery;
    private final RecoveryPassGroup postModernCleanup;
    private final RecoveryPassGroup outputPolish;

    StructureRecoveryPipeline(PatternSemanticsRewriter patternSemanticsRewriter) {
        this.initialCleanup = new RecoveryPassGroup(
                new NamedRecoveryPass((block, context) -> true, (block, context) -> Op04StructuredStatement.tidyEmptyCatch(block)),
                new NamedRecoveryPass((block, context) -> true, (block, context) -> Op04StructuredStatement.tidyTryCatch(block)),
                new NamedRecoveryPass((block, context) -> true, (block, context) -> Op04StructuredStatement.convertUnstructuredIf(block)),
                new NamedRecoveryPass((block, context) -> true, (block, context) -> Op04StructuredStatement.rewriteForwardIfGotos(block)),
                new NamedRecoveryPass((block, context) -> true, (block, context) -> Op04StructuredStatement.rewriteConditionLocalAliases(block)),
                new NamedRecoveryPass((block, context) -> true, (block, context) -> Op04StructuredStatement.inlinePossibles(block)),
                new NamedRecoveryPass((block, context) -> true, (block, context) -> Op04StructuredStatement.removeStructuredGotos(block)),
                new NamedRecoveryPass((block, context) -> true, (block, context) -> Op04StructuredStatement.removePointlessBlocks(block)),
                new NamedRecoveryPass((block, context) -> true, (block, context) -> Op04StructuredStatement.removePointlessReturn(block)),
                new NamedRecoveryPass((block, context) -> true, (block, context) -> Op04StructuredStatement.removePointlessControlFlow(block)),
                new NamedRecoveryPass((block, context) -> true, (block, context) -> Op04StructuredStatement.removePrimitiveDeconversion(context.options, context.method, block)),
                new NamedRecoveryPass((block, context) -> context.options.getOption(OptionsImpl.LABELLED_BLOCKS), (block, context) -> {
                    Op04StructuredStatement.insertLabelledBlocks(block);
                    Op04StructuredStatement.rewriteForwardIfGotos(block);
                }),
                new NamedRecoveryPass((block, context) -> true, (block, context) -> Op04StructuredStatement.removeUnnecessaryLabelledBreaks(block)),
                new NamedRecoveryPass((block, context) -> true, (block, context) -> Op04StructuredStatement.flattenNonReferencedBlocks(block))
        );
        this.heavyRecovery = new RecoveryPassGroup(
                new NamedRecoveryPass((block, context) -> true, (block, context) -> Op04StructuredStatement.tidyEmptyCatch(block)),
                new NamedRecoveryPass((block, context) -> true, (block, context) -> Op04StructuredStatement.tidyTryCatch(block)),
                new NamedRecoveryPass((block, context) -> true, (block, context) -> Op04StructuredStatement.convertUnstructuredIf(block)),
                new NamedRecoveryPass((block, context) -> true, (block, context) -> Op04StructuredStatement.rewriteForwardIfGotos(block)),
                new NamedRecoveryPass((block, context) -> true, (block, context) -> Op04StructuredStatement.rewriteConditionLocalAliases(block)),
                new NamedRecoveryPass((block, context) -> true, (block, context) -> Op04StructuredStatement.prettifyBadLoops(block)),
                new NamedRecoveryPass((block, context) -> true, (block, context) -> patternSemanticsRewriter.rewrite(block, context.bytecodeMeta)),
                new NamedRecoveryPass((block, context) -> true, (block, context) -> Op04StructuredStatement.inlinePossibles(block)),
                new NamedRecoveryPass((block, context) -> true, (block, context) -> Op04StructuredStatement.removeStructuredGotos(block)),
                new NamedRecoveryPass((block, context) -> true, (block, context) -> Op04StructuredStatement.removePointlessBlocks(block)),
                new NamedRecoveryPass((block, context) -> true, (block, context) -> Op04StructuredStatement.removePointlessReturn(block)),
                new NamedRecoveryPass((block, context) -> true, (block, context) -> Op04StructuredStatement.removePointlessControlFlow(block)),
                new NamedRecoveryPass((block, context) -> true, (block, context) -> Op04StructuredStatement.removePrimitiveDeconversion(context.options, context.method, block)),
                new NamedRecoveryPass((block, context) -> context.options.getOption(OptionsImpl.LABELLED_BLOCKS), (block, context) -> {
                    Op04StructuredStatement.insertLabelledBlocks(block);
                    Op04StructuredStatement.rewriteForwardIfGotos(block);
                }),
                new NamedRecoveryPass((block, context) -> true, (block, context) -> Op04StructuredStatement.removeUnnecessaryLabelledBreaks(block)),
                new NamedRecoveryPass((block, context) -> true, (block, context) -> Op04StructuredStatement.flattenNonReferencedBlocks(block))
        );
        this.postModernCleanup = new RecoveryPassGroup(
                new NamedRecoveryPass((block, context) -> true, (block, context) -> Op04StructuredStatement.tidyEmptyCatch(block)),
                new NamedRecoveryPass((block, context) -> true, (block, context) -> Op04StructuredStatement.tidyTryCatch(block)),
                new NamedRecoveryPass((block, context) -> true, (block, context) -> Op04StructuredStatement.convertUnstructuredIf(block)),
                new NamedRecoveryPass((block, context) -> true, (block, context) -> Op04StructuredStatement.rewriteForwardIfGotos(block)),
                new NamedRecoveryPass((block, context) -> true, (block, context) -> Op04StructuredStatement.rewriteConditionLocalAliases(block)),
                new NamedRecoveryPass((block, context) -> true, (block, context) -> Op04StructuredStatement.prettifyBadLoops(block)),
                new NamedRecoveryPass((block, context) -> true, (block, context) -> patternSemanticsRewriter.rewrite(block, context.bytecodeMeta)),
                new NamedRecoveryPass((block, context) -> true, (block, context) -> Op04StructuredStatement.inlinePossibles(block)),
                new NamedRecoveryPass((block, context) -> true, (block, context) -> Op04StructuredStatement.removeStructuredGotos(block)),
                new NamedRecoveryPass((block, context) -> true, (block, context) -> Op04StructuredStatement.removePointlessBlocks(block)),
                new NamedRecoveryPass((block, context) -> true, (block, context) -> Op04StructuredStatement.removePointlessReturn(block)),
                new NamedRecoveryPass((block, context) -> true, (block, context) -> Op04StructuredStatement.removePointlessControlFlow(block)),
                new NamedRecoveryPass((block, context) -> true, (block, context) -> Op04StructuredStatement.removePrimitiveDeconversion(context.options, context.method, block)),
                new NamedRecoveryPass((block, context) -> context.options.getOption(OptionsImpl.LABELLED_BLOCKS), (block, context) -> {
                    Op04StructuredStatement.insertLabelledBlocks(block);
                    Op04StructuredStatement.rewriteForwardIfGotos(block);
                }),
                new NamedRecoveryPass((block, context) -> true, (block, context) -> Op04StructuredStatement.removeUnnecessaryLabelledBreaks(block)),
                new NamedRecoveryPass((block, context) -> true, (block, context) -> Op04StructuredStatement.flattenNonReferencedBlocks(block)),
                new NamedRecoveryPass((block, context) -> true, (block, context) -> Op04StructuredStatement.cleanupStructuredExpressionBodies(block))
        );
        this.outputPolish = new RecoveryPassGroup(
                new NamedRecoveryPass((block, context) -> true, (block, context) -> Op04StructuredStatement.rewriteForwardIfGotos(block)),
                new NamedRecoveryPass((block, context) -> true, (block, context) -> Op04StructuredStatement.rewriteConditionLocalAliases(block)),
                new NamedRecoveryPass((block, context) -> true, (block, context) -> Op04StructuredStatement.removePointlessBlocks(block)),
                new NamedRecoveryPass((block, context) -> true, (block, context) -> Op04StructuredStatement.removePointlessControlFlow(block)),
                new NamedRecoveryPass((block, context) -> true, (block, context) -> Op04StructuredStatement.removePrimitiveDeconversion(context.options, context.method, block)),
                new NamedRecoveryPass((block, context) -> true, (block, context) -> Op04StructuredStatement.removeUnnecessaryLabelledBreaks(block)),
                new NamedRecoveryPass((block, context) -> true, (block, context) -> Op04StructuredStatement.flattenNonReferencedBlocks(block))
        );
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

    private interface RecoveryPass {
        boolean enabled(Op04StructuredStatement block, MethodAnalysisContext context);

        void apply(Op04StructuredStatement block, MethodAnalysisContext context);
    }

    private static final class NamedRecoveryPass implements RecoveryPass {
        private final BiPredicate<Op04StructuredStatement, MethodAnalysisContext> enabled;
        private final BiConsumer<Op04StructuredStatement, MethodAnalysisContext> action;

        private NamedRecoveryPass(BiPredicate<Op04StructuredStatement, MethodAnalysisContext> enabled,
                                  BiConsumer<Op04StructuredStatement, MethodAnalysisContext> action) {
            this.enabled = enabled;
            this.action = action;
        }

        @Override
        public boolean enabled(Op04StructuredStatement block, MethodAnalysisContext context) {
            return enabled.test(block, context);
        }

        @Override
        public void apply(Op04StructuredStatement block, MethodAnalysisContext context) {
            action.accept(block, context);
        }
    }

    private static final class RecoveryPassGroup {
        private final List<RecoveryPass> passes;

        private RecoveryPassGroup(RecoveryPass... passes) {
            this.passes = List.of(passes);
        }

        void run(Op04StructuredStatement block, MethodAnalysisContext context) {
            for (RecoveryPass pass : passes) {
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
