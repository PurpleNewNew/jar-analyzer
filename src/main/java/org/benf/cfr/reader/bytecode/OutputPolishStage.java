package org.benf.cfr.reader.bytecode;

import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.StructuredLocalVariableRecovery;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.checker.IllegalReturnChecker;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.checker.LooseCatchChecker;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.checker.VoidVariableChecker;
import org.benf.cfr.reader.util.getopt.OptionsImpl;

final class OutputPolishStage {
    private final StructureRecoveryPipeline structureRecoveryPipeline;

    OutputPolishStage(StructureRecoveryPipeline structureRecoveryPipeline) {
        this.structureRecoveryPipeline = structureRecoveryPipeline;
    }

    void apply(Op04StructuredStatement block, MethodAnalysisContext context) {
        if (!block.isFullyStructured()) {
            return;
        }
        applyExpressionPolish(block, context);
        applyRecoveryPolish(block, context);
        applyValidationAndMetadata(block, context);
    }

    private void applyExpressionPolish(Op04StructuredStatement block, MethodAnalysisContext context) {
        if (context.options.getOption(OptionsImpl.REMOVE_BOILERPLATE) && context.method.isConstructor()) {
            StructuredOutputTransforms.removeConstructorBoilerplate(block);
        }
        StructuredOutputTransforms.removeUnnecessaryVarargArrays(block);
        StructuredOutputTransforms.rewriteBadCastChains(block);
        StructuredOutputTransforms.rewriteNarrowingAssignments(block);
        StructuredSemanticTransforms.tidyVariableNames(
                context.method,
                block,
                context.bytecodeMeta,
                context.comments,
                context.constantPool.getClassCache(),
                context.modernFeatures
        );
        StructuredOutputTransforms.tidyObfuscation(context.options, block);
        StructuredOutputTransforms.miscKeyholeTransforms(context.variableFactory, block);
        structureRecoveryPipeline.applyOutputPolish(block, context);
    }

    private void applyRecoveryPolish(Op04StructuredStatement block, MethodAnalysisContext context) {
        StructuredSemanticTransforms.reduceClashDeclarations(block, context.bytecodeMeta);
        StructuredLocalVariableRecovery.applyOutputRecovery(block);
    }

    private void applyValidationAndMetadata(Op04StructuredStatement block, MethodAnalysisContext context) {
        StructuredOutputTransforms.applyChecker(new LooseCatchChecker(), block, context.comments);
        StructuredOutputTransforms.applyChecker(new VoidVariableChecker(), block, context.comments);
        StructuredOutputTransforms.applyChecker(new IllegalReturnChecker(), block, context.comments);
        StructureRecoveryTransforms.flattenNonReferencedBlocks(block);
        StructuredSemanticTransforms.applyLocalVariableMetadata(
                context.originalCodeAttribute,
                block,
                context.lutByOffset,
                context.comments
        );
    }
}
