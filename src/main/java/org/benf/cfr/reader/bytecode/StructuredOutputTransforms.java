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

import java.util.List;

final class StructuredOutputTransforms {
    private StructuredOutputTransforms() {
    }

    static List<StructuredPassEntry> describePasses() {
        return List.of(
                entry("remove-constructor-boilerplate", "output-polish.expression-polish", "fully-structured", "Removes redundant constructor super boilerplate.", true, false),
                entry("remove-unnecessary-vararg-arrays", "output-polish.expression-polish", "fully-structured", "Elides synthetic vararg array wrappers.", true, false),
                entry("rewrite-bad-cast-chains", "output-polish.expression-polish", "fully-structured", "Simplifies cast chains produced by earlier rewrites.", true, false),
                entry("rewrite-narrowing-assignments", "output-polish.expression-polish", "fully-structured", "Rewrites explicit narrowing assignment scaffolding.", true, false),
                entry("tidy-obfuscation", "output-polish.expression-polish", "fully-structured", "Folds obfuscation constants when configured.", true, false),
                entry("misc-keyhole-transforms", "output-polish.expression-polish", "fully-structured", "Applies keyhole expression cleanups that preserve structure.", true, false),
                entry("apply-checker", "output-polish.validation-and-metadata", "fully-structured", "Runs output validators and records any decompiler comments.", false, false)
        );
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

    private static StructuredPassEntry entry(String name,
                                             String stage,
                                             String inputRequirement,
                                             String outputPromise,
                                             boolean idempotent,
                                             boolean allowsStructuralChange,
                                             String... dependencies) {
        return StructuredPassEntry.of(
                "output-transform",
                stage,
                inputRequirement,
                StructuredPassDescriptor.of(name, outputPromise, idempotent, allowsStructuralChange, dependencies)
        );
    }
}
