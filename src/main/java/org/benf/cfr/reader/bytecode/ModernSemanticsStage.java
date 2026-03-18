package org.benf.cfr.reader.bytecode;

import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.StructuredLocalVariableRecovery;
import org.benf.cfr.reader.util.getopt.OptionsImpl;

final class ModernSemanticsStage {
    private final StructureRecoveryPipeline structureRecoveryPipeline;
    private final PatternSemanticsRewriter patternSemanticsRewriter;

    ModernSemanticsStage(StructureRecoveryPipeline structureRecoveryPipeline,
                         PatternSemanticsRewriter patternSemanticsRewriter) {
        this.structureRecoveryPipeline = structureRecoveryPipeline;
        this.patternSemanticsRewriter = patternSemanticsRewriter;
    }

    void apply(Op04StructuredStatement block, MethodAnalysisContext context) {
        if (!block.isFullyStructured()) {
            return;
        }
        Op04StructuredStatement.rewriteExplicitTypeUsages(context.method, block, context.anonymousClassUsage, context.modernFeatures);
        Op04StructuredStatement.discoverVariableScopes(context.method, block, context.variableFactory, context.options, context.classFileVersion, context.bytecodeMeta);
        if (context.options.getOption(OptionsImpl.REWRITE_TRY_RESOURCES, context.classFileVersion)) {
            Op04StructuredStatement.removeEndResource(context.classFile, block);
        }
        patternSemanticsRewriter.rewrite(block, context.bytecodeMeta);
        if (context.modernFeatures.supportsSwitchExpressions()) {
            Op04StructuredStatement.switchExpression(context.method, block, context.comments, context.modernFeatures.shouldEmitPreviewSwitchExpressionComment());
        }
        structureRecoveryPipeline.cleanupAfterModernSemantics(block, context);
        Op04StructuredStatement.rewriteLambdas(context.commonState, context.method, block);
        Op04StructuredStatement.markLambdaCapturedVariables(block);
        StructuredLocalVariableRecovery.restoreLiftableDefinitionAssignments(block);
        Op04StructuredStatement.removeRedundantIntersectionCasts(context.commonState, context.method, block);
        Op04StructuredStatement.discoverLocalClassScopes(context.method, block, context.variableFactory, context.options);
    }
}
