package org.benf.cfr.reader.bytecode;

import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.checker.Op04Checker;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.BadCastChainRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.ExpressionRewriterTransformer;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.HexLiteralTidier;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.InvalidBooleanCastCleaner;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.InvalidExpressionStatementCleaner;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.LambdaCleaner;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.NakedNullCaster;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.NarrowingAssignmentRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.RedundantSuperRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.TernaryCastCleaner;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.VarArgsRewriter;
import org.benf.cfr.reader.bytecode.analysis.variables.VariableFactory;
import org.benf.cfr.reader.util.DecompilerComments;
import org.benf.cfr.reader.util.getopt.Options;
import org.benf.cfr.reader.util.getopt.OptionsImpl;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ConstantFoldingRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.LiteralRewriter;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredScope;

final class StructuredOutputTransforms {
    private StructuredOutputTransforms() {
    }

    static void removeConstructorBoilerplate(Op04StructuredStatement root) {
        new RedundantSuperRewriter().rewrite(root);
    }

    static void removeUnnecessaryVarargArrays(Op04StructuredStatement root) {
        new VarArgsRewriter().rewrite(root);
    }

    static void rewriteBadCastChains(Op04StructuredStatement root) {
        root.transform(new ExpressionRewriterTransformer(new BadCastChainRewriter()), new StructuredScope());
    }

    static void rewriteNarrowingAssignments(Op04StructuredStatement root) {
        new NarrowingAssignmentRewriter().rewrite(root);
    }

    static void tidyObfuscation(Options options, Op04StructuredStatement root) {
        if (options.getOption(OptionsImpl.CONST_OBF)) {
            new ExpressionRewriterTransformer(ConstantFoldingRewriter.INSTANCE).transform(root);
        }
    }

    static void miscKeyholeTransforms(VariableFactory variableFactory, Op04StructuredStatement root) {
        new NakedNullCaster().transform(root);
        new LambdaCleaner().transform(root);
        new TernaryCastCleaner().transform(root);
        new InvalidBooleanCastCleaner().transform(root);
        new HexLiteralTidier().transform(root);
        new ExpressionRewriterTransformer(LiteralRewriter.INSTANCE).transform(root);
        new InvalidExpressionStatementCleaner(variableFactory).transform(root);
    }

    static void applyChecker(Op04Checker checker, Op04StructuredStatement root, DecompilerComments comments) {
        root.transform(checker, new StructuredScope());
        checker.commentInto(comments);
    }
}
