package org.benf.cfr.reader.bytecode;

import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.StructuredLocalVariableRecovery;
import org.benf.cfr.reader.util.getopt.OptionsImpl;

final class StructureRecoveryPasses {
    private StructureRecoveryPasses() {
    }

    abstract static class AlwaysEnabledPass implements StructureRecoveryPass {
        private final StructuredPassDescriptor descriptor;

        AlwaysEnabledPass(StructuredPassDescriptor descriptor) {
            this.descriptor = descriptor;
        }

        @Override
        public final StructuredPassDescriptor descriptor() {
            return descriptor;
        }

        @Override
        public final boolean enabled(Op04StructuredStatement block, MethodAnalysisContext context) {
            return true;
        }
    }

    static final class TidyEmptyCatchPass extends AlwaysEnabledPass {
        TidyEmptyCatchPass() {
            super(passDescriptor("tidy-empty-catch", "Removes empty catch residue before later control-flow recovery.", true, true));
        }

        @Override
        public void apply(Op04StructuredStatement block, MethodAnalysisContext context) {
            StructureRecoveryTransforms.tidyEmptyCatch(block);
        }
    }

    static final class TidyTryCatchPass extends AlwaysEnabledPass {
        TidyTryCatchPass() {
            super(passDescriptor("tidy-try-catch", "Normalizes try/catch frontiers for downstream structuring.", true, true, "tidy-empty-catch"));
        }

        @Override
        public void apply(Op04StructuredStatement block, MethodAnalysisContext context) {
            StructureRecoveryTransforms.tidyTryCatch(block);
        }
    }

    static final class ConvertUnstructuredIfPass extends AlwaysEnabledPass {
        ConvertUnstructuredIfPass() {
            super(passDescriptor("convert-unstructured-if", "Converts raw conditional frontiers into structured if forms.", true, true, "tidy-try-catch"));
        }

        @Override
        public void apply(Op04StructuredStatement block, MethodAnalysisContext context) {
            StructureRecoveryTransforms.convertUnstructuredIf(block);
        }
    }

    static final class RewriteForwardIfGotosPass extends AlwaysEnabledPass {
        RewriteForwardIfGotosPass() {
            super(passDescriptor("rewrite-forward-if-gotos", "Rewrites forward-goto guard residue into structured flow.", true, true, "convert-unstructured-if"));
        }

        @Override
        public void apply(Op04StructuredStatement block, MethodAnalysisContext context) {
            StructureRecoveryTransforms.rewriteForwardIfGotos(block);
        }
    }

    static final class RewriteConditionLocalAliasesPass extends AlwaysEnabledPass {
        RewriteConditionLocalAliasesPass() {
            super(passDescriptor("rewrite-condition-local-aliases", "Normalizes condition-local aliases before cleanup and polish passes.", true, true, "rewrite-forward-if-gotos"));
        }

        @Override
        public void apply(Op04StructuredStatement block, MethodAnalysisContext context) {
            StructuredLocalVariableRecovery.rewriteConditionLocalAliases(block);
        }
    }

    static final class PrettifyBadLoopsPass extends AlwaysEnabledPass {
        PrettifyBadLoopsPass() {
            super(passDescriptor("prettify-bad-loops", "Canonicalizes malformed loop shapes into structured loop statements.", true, true, "rewrite-forward-if-gotos"));
        }

        @Override
        public void apply(Op04StructuredStatement block, MethodAnalysisContext context) {
            StructureRecoveryTransforms.prettifyBadLoops(block);
        }
    }

    static final class PatternSemanticsPass extends AlwaysEnabledPass {
        private final PatternSemanticsRewriter patternSemanticsRewriter;

        PatternSemanticsPass(PatternSemanticsRewriter patternSemanticsRewriter) {
            super(passDescriptor("pattern-semantics", "Recovers pattern-matching structure before post-modern cleanup.", true, true, "prettify-bad-loops"));
            this.patternSemanticsRewriter = patternSemanticsRewriter;
        }

        @Override
        public void apply(Op04StructuredStatement block, MethodAnalysisContext context) {
            patternSemanticsRewriter.rewrite(block, context.bytecodeMeta);
        }
    }

    static final class InlinePossiblesPass extends AlwaysEnabledPass {
        InlinePossiblesPass() {
            super(passDescriptor("inline-possibles", "Inlines trivial nested blocks that would otherwise block later cleanup.", true, true));
        }

        @Override
        public void apply(Op04StructuredStatement block, MethodAnalysisContext context) {
            StructureRecoveryTransforms.inlinePossibles(block);
        }
    }

    static final class RemoveStructuredGotosPass extends AlwaysEnabledPass {
        RemoveStructuredGotosPass() {
            super(passDescriptor("remove-structured-gotos", "Drops leftover structured goto markers once control-flow is recoverable.", true, true, "inline-possibles"));
        }

        @Override
        public void apply(Op04StructuredStatement block, MethodAnalysisContext context) {
            StructureRecoveryTransforms.removeStructuredGotos(block);
        }
    }

    static final class RemovePointlessBlocksPass extends AlwaysEnabledPass {
        RemovePointlessBlocksPass() {
            super(passDescriptor("remove-pointless-blocks", "Flattens redundant block wrappers without changing method semantics.", true, true, "remove-structured-gotos"));
        }

        @Override
        public void apply(Op04StructuredStatement block, MethodAnalysisContext context) {
            StructureRecoveryTransforms.removePointlessBlocks(block);
        }
    }

    static final class RemovePointlessReturnPass extends AlwaysEnabledPass {
        RemovePointlessReturnPass() {
            super(passDescriptor("remove-pointless-return", "Removes redundant terminal returns after control-flow stabilizes.", true, true, "remove-pointless-blocks"));
        }

        @Override
        public void apply(Op04StructuredStatement block, MethodAnalysisContext context) {
            StructureRecoveryTransforms.removePointlessReturn(block);
        }
    }

    static final class RemovePointlessControlFlowPass extends AlwaysEnabledPass {
        RemovePointlessControlFlowPass() {
            super(passDescriptor("remove-pointless-control-flow", "Deletes inert control-flow residue that no longer affects structure.", true, true, "remove-pointless-return"));
        }

        @Override
        public void apply(Op04StructuredStatement block, MethodAnalysisContext context) {
            StructureRecoveryTransforms.removePointlessControlFlow(block);
        }
    }

    static final class RemovePrimitiveDeconversionPass extends AlwaysEnabledPass {
        RemovePrimitiveDeconversionPass() {
            super(passDescriptor("remove-primitive-deconversion", "Strips primitive boxing/deconversion residue after structure is stable.", true, false));
        }

        @Override
        public void apply(Op04StructuredStatement block, MethodAnalysisContext context) {
            StructureRecoveryTransforms.removePrimitiveDeconversion(context.options, block);
        }
    }

    static final class InsertLabelledBlocksPass implements StructureRecoveryPass {
        private static final StructuredPassDescriptor DESCRIPTOR = passDescriptor(
                "insert-labelled-blocks",
                "Introduces labelled blocks only when labelled-block output is enabled.",
                true,
                true,
                "remove-pointless-control-flow"
        );

        @Override
        public StructuredPassDescriptor descriptor() {
            return DESCRIPTOR;
        }

        @Override
        public boolean enabled(Op04StructuredStatement block, MethodAnalysisContext context) {
            return context.options.getOption(OptionsImpl.LABELLED_BLOCKS);
        }

        @Override
        public void apply(Op04StructuredStatement block, MethodAnalysisContext context) {
            StructureRecoveryTransforms.insertLabelledBlocks(block);
            StructureRecoveryTransforms.rewriteForwardIfGotos(block);
        }
    }

    static final class RemoveUnnecessaryLabelledBreaksPass extends AlwaysEnabledPass {
        RemoveUnnecessaryLabelledBreaksPass() {
            super(passDescriptor("remove-unnecessary-labelled-breaks", "Prunes labelled breaks that no longer carry control-flow meaning.", true, true, "insert-labelled-blocks"));
        }

        @Override
        public void apply(Op04StructuredStatement block, MethodAnalysisContext context) {
            StructureRecoveryTransforms.removeUnnecessaryLabelledBreaks(block);
        }
    }

    static final class FlattenNonReferencedBlocksPass extends AlwaysEnabledPass {
        FlattenNonReferencedBlocksPass() {
            super(passDescriptor("flatten-non-referenced-blocks", "Flattens unreferenced block nesting after labelled cleanup.", true, true, "remove-unnecessary-labelled-breaks"));
        }

        @Override
        public void apply(Op04StructuredStatement block, MethodAnalysisContext context) {
            StructureRecoveryTransforms.flattenNonReferencedBlocks(block);
        }
    }

    static final class CleanupStructuredExpressionBodiesPass extends AlwaysEnabledPass {
        CleanupStructuredExpressionBodiesPass() {
            super(passDescriptor("cleanup-structured-expression-bodies", "Removes expression-body scaffolding once structured output is ready.", true, true, "flatten-non-referenced-blocks"));
        }

        @Override
        public void apply(Op04StructuredStatement block, MethodAnalysisContext context) {
            StructureRecoveryTransforms.cleanupStructuredExpressionBodies(block);
        }
    }

    private static StructuredPassDescriptor passDescriptor(String name,
                                                           String outputPromise,
                                                           boolean idempotent,
                                                           boolean allowsStructuralChange,
                                                           String... dependencies) {
        return StructuredPassDescriptor.of(name, outputPromise, idempotent, allowsStructuralChange, dependencies);
    }
}
