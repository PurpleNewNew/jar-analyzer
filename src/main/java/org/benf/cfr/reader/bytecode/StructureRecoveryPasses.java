package org.benf.cfr.reader.bytecode;

import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.util.getopt.OptionsImpl;

final class StructureRecoveryPasses {
    private StructureRecoveryPasses() {
    }

    abstract static class AlwaysEnabledPass implements StructureRecoveryPass {
        @Override
        public final boolean enabled(Op04StructuredStatement block, MethodAnalysisContext context) {
            return true;
        }
    }

    static final class TidyEmptyCatchPass extends AlwaysEnabledPass {
        @Override
        public void apply(Op04StructuredStatement block, MethodAnalysisContext context) {
            Op04StructuredStatement.tidyEmptyCatch(block);
        }
    }

    static final class TidyTryCatchPass extends AlwaysEnabledPass {
        @Override
        public void apply(Op04StructuredStatement block, MethodAnalysisContext context) {
            Op04StructuredStatement.tidyTryCatch(block);
        }
    }

    static final class ConvertUnstructuredIfPass extends AlwaysEnabledPass {
        @Override
        public void apply(Op04StructuredStatement block, MethodAnalysisContext context) {
            Op04StructuredStatement.convertUnstructuredIf(block);
        }
    }

    static final class RewriteForwardIfGotosPass extends AlwaysEnabledPass {
        @Override
        public void apply(Op04StructuredStatement block, MethodAnalysisContext context) {
            Op04StructuredStatement.rewriteForwardIfGotos(block);
        }
    }

    static final class RewriteConditionLocalAliasesPass extends AlwaysEnabledPass {
        @Override
        public void apply(Op04StructuredStatement block, MethodAnalysisContext context) {
            Op04StructuredStatement.rewriteConditionLocalAliases(block);
        }
    }

    static final class PrettifyBadLoopsPass extends AlwaysEnabledPass {
        @Override
        public void apply(Op04StructuredStatement block, MethodAnalysisContext context) {
            Op04StructuredStatement.prettifyBadLoops(block);
        }
    }

    static final class PatternSemanticsPass extends AlwaysEnabledPass {
        private final PatternSemanticsRewriter patternSemanticsRewriter;

        PatternSemanticsPass(PatternSemanticsRewriter patternSemanticsRewriter) {
            this.patternSemanticsRewriter = patternSemanticsRewriter;
        }

        @Override
        public void apply(Op04StructuredStatement block, MethodAnalysisContext context) {
            patternSemanticsRewriter.rewrite(block, context.bytecodeMeta);
        }
    }

    static final class InlinePossiblesPass extends AlwaysEnabledPass {
        @Override
        public void apply(Op04StructuredStatement block, MethodAnalysisContext context) {
            Op04StructuredStatement.inlinePossibles(block);
        }
    }

    static final class RemoveStructuredGotosPass extends AlwaysEnabledPass {
        @Override
        public void apply(Op04StructuredStatement block, MethodAnalysisContext context) {
            Op04StructuredStatement.removeStructuredGotos(block);
        }
    }

    static final class RemovePointlessBlocksPass extends AlwaysEnabledPass {
        @Override
        public void apply(Op04StructuredStatement block, MethodAnalysisContext context) {
            Op04StructuredStatement.removePointlessBlocks(block);
        }
    }

    static final class RemovePointlessReturnPass extends AlwaysEnabledPass {
        @Override
        public void apply(Op04StructuredStatement block, MethodAnalysisContext context) {
            Op04StructuredStatement.removePointlessReturn(block);
        }
    }

    static final class RemovePointlessControlFlowPass extends AlwaysEnabledPass {
        @Override
        public void apply(Op04StructuredStatement block, MethodAnalysisContext context) {
            Op04StructuredStatement.removePointlessControlFlow(block);
        }
    }

    static final class RemovePrimitiveDeconversionPass extends AlwaysEnabledPass {
        @Override
        public void apply(Op04StructuredStatement block, MethodAnalysisContext context) {
            Op04StructuredStatement.removePrimitiveDeconversion(context.options, context.method, block);
        }
    }

    static final class InsertLabelledBlocksPass implements StructureRecoveryPass {
        @Override
        public boolean enabled(Op04StructuredStatement block, MethodAnalysisContext context) {
            return context.options.getOption(OptionsImpl.LABELLED_BLOCKS);
        }

        @Override
        public void apply(Op04StructuredStatement block, MethodAnalysisContext context) {
            Op04StructuredStatement.insertLabelledBlocks(block);
            Op04StructuredStatement.rewriteForwardIfGotos(block);
        }
    }

    static final class RemoveUnnecessaryLabelledBreaksPass extends AlwaysEnabledPass {
        @Override
        public void apply(Op04StructuredStatement block, MethodAnalysisContext context) {
            Op04StructuredStatement.removeUnnecessaryLabelledBreaks(block);
        }
    }

    static final class FlattenNonReferencedBlocksPass extends AlwaysEnabledPass {
        @Override
        public void apply(Op04StructuredStatement block, MethodAnalysisContext context) {
            Op04StructuredStatement.flattenNonReferencedBlocks(block);
        }
    }

    static final class CleanupStructuredExpressionBodiesPass extends AlwaysEnabledPass {
        @Override
        public void apply(Op04StructuredStatement block, MethodAnalysisContext context) {
            Op04StructuredStatement.cleanupStructuredExpressionBodies(block);
        }
    }
}
