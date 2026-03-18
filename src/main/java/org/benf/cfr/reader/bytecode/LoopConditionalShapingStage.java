package org.benf.cfr.reader.bytecode;

import org.benf.cfr.reader.bytecode.analysis.opgraph.Op03Blocks;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.AnonymousArray;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.BadNarrowingArgRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.Cleaner;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.ConditionalRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.ExceptionRewriters;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.FinallyRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.InlineDeAssigner;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.IterLoopRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.LValueProp;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.LoopIdentifier;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.LoopLivenessClash;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.Op03Rewriters;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.RemoveDeterministicJumps;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.SwitchReplacer;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.SynchronizedBlocks;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExplicitTypeCallRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.StringBuilderRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.XorRewriter;
import org.benf.cfr.reader.util.DecompilerComment;
import org.benf.cfr.reader.util.Troolean;
import org.benf.cfr.reader.util.getopt.OptionsImpl;

import java.util.List;

final class LoopConditionalShapingStage {
    void shape(Op03PipelineState state, Op03PipelineContext context) {
        if (context.options.getOption(OptionsImpl.FORCE_COND_PROPAGATE) == Troolean.TRUE) {
            state.op03SimpleParseNodes = RemoveDeterministicJumps.apply(context.method, state.op03SimpleParseNodes);
        }

        if (context.options.getOption(OptionsImpl.FORCE_TOPSORT) == Troolean.TRUE) {
            if (context.options.getOption(OptionsImpl.FORCE_RETURNING_IFS) == Troolean.TRUE) {
                Op03Rewriters.replaceReturningIfs(state.op03SimpleParseNodes, true);
            }
            if (context.options.getOption(OptionsImpl.FORCE_COND_PROPAGATE) == Troolean.TRUE) {
                Op03Rewriters.propagateToReturn2(state.op03SimpleParseNodes);
            }
            ExceptionRewriters.handleEmptyTries(state.op03SimpleParseNodes);
            state.op03SimpleParseNodes = Cleaner.removeUnreachableCode(state.op03SimpleParseNodes, false);
            state.op03SimpleParseNodes = Op03Blocks.topologicalSort(state.op03SimpleParseNodes, context.comments, context.options);
            Op03Rewriters.removePointlessJumps(state.op03SimpleParseNodes);
            SwitchReplacer.rebuildSwitches(state.op03SimpleParseNodes, context.options, context.comments, context.bytecodeMeta);
            Op03Rewriters.rejoinBlocks(state.op03SimpleParseNodes);
            Op03Rewriters.extendTryBlocks(context.commonState, state.op03SimpleParseNodes);
            state.op03SimpleParseNodes = Op03Blocks.combineTryBlocks(state.op03SimpleParseNodes);
            Op03Rewriters.combineTryCatchEnds(state.op03SimpleParseNodes);
            Op03Rewriters.rewriteTryBackJumps(state.op03SimpleParseNodes);
            FinallyRewriter.identifyFinally(context.options, context.method, state.op03SimpleParseNodes, state.blockIdentifierFactory);
            if (context.options.getOption(OptionsImpl.FORCE_RETURNING_IFS) == Troolean.TRUE) {
                Op03Rewriters.replaceReturningIfs(state.op03SimpleParseNodes, true);
            }
        }

        if (context.options.getOption(OptionsImpl.AGGRESSIVE_DUFF) == Troolean.TRUE
                && context.bytecodeMeta.has(BytecodeMeta.CodeInfoFlag.SWITCHES)) {
            state.op03SimpleParseNodes = Cleaner.sortAndRenumber(state.op03SimpleParseNodes);
            state.op03SimpleParseNodes = SwitchReplacer.rewriteDuff(state.op03SimpleParseNodes, state.variableFactory, context.comments, context.options);
        }
        if (context.options.getOption(OptionsImpl.FORCE_COND_PROPAGATE) == Troolean.TRUE) {
            RemoveDeterministicJumps.propagateToReturn(context.method, state.op03SimpleParseNodes);
        }

        boolean reloop;
        do {
            Op03Rewriters.rewriteNegativeJumps(state.op03SimpleParseNodes, true);
            Op03Rewriters.collapseAssignmentsIntoConditionals(state.op03SimpleParseNodes, context.options, context.classFileVersion);
            AnonymousArray.resugarAnonymousArrays(state.op03SimpleParseNodes);
            reloop = Op03Rewriters.condenseConditionals(state.op03SimpleParseNodes);
            reloop = reloop | Op03Rewriters.condenseConditionals2(state.op03SimpleParseNodes);
            reloop = reloop | Op03Rewriters.normalizeDupAssigns(state.op03SimpleParseNodes);
            if (reloop) {
                LValueProp.condenseLValues(state.op03SimpleParseNodes);
            }
            state.op03SimpleParseNodes = Cleaner.removeUnreachableCode(state.op03SimpleParseNodes, true);
        } while (reloop);

        AnonymousArray.resugarAnonymousArrays(state.op03SimpleParseNodes);
        Op03Rewriters.simplifyConditionals(state.op03SimpleParseNodes, false, context.method);
        state.op03SimpleParseNodes = Cleaner.sortAndRenumber(state.op03SimpleParseNodes);
        Op03Rewriters.rewriteNegativeJumps(state.op03SimpleParseNodes, false);
        Op03Rewriters.optimiseForTypes(state.op03SimpleParseNodes);

        if (context.options.getOption(OptionsImpl.ECLIPSE)) {
            Op03Rewriters.eclipseLoopPass(state.op03SimpleParseNodes);
        }

        state.op03SimpleParseNodes = Cleaner.removeUnreachableCode(state.op03SimpleParseNodes, true);
        LoopIdentifier.identifyLoops1(context.method, state.op03SimpleParseNodes, state.blockIdentifierFactory);
        Op03Rewriters.rewriteBadCompares(state.variableFactory, state.op03SimpleParseNodes);
        state.op03SimpleParseNodes = Op03Rewriters.pushThroughGoto(state.op03SimpleParseNodes);

        if (context.options.getOption(OptionsImpl.FORCE_RETURNING_IFS) == Troolean.TRUE) {
            Op03Rewriters.replaceReturningIfs(state.op03SimpleParseNodes, false);
        }

        state.op03SimpleParseNodes = Cleaner.sortAndRenumber(state.op03SimpleParseNodes);
        state.op03SimpleParseNodes = Cleaner.removeUnreachableCode(state.op03SimpleParseNodes, true);
        Op03Rewriters.rewriteBreakStatements(state.op03SimpleParseNodes);
        Op03Rewriters.rewriteDoWhileTruePredAsWhile(state.op03SimpleParseNodes);
        Op03Rewriters.rewriteWhilesAsFors(context.options, state.op03SimpleParseNodes);
        Op03Rewriters.removeSynchronizedCatchBlocks(context.options, state.op03SimpleParseNodes);
        state.op03SimpleParseNodes = Op03Rewriters.removeUselessNops(state.op03SimpleParseNodes);
        Op03Rewriters.removePointlessJumps(state.op03SimpleParseNodes);
        Op03Rewriters.extractExceptionJumps(state.op03SimpleParseNodes);
        Op03Rewriters.extractAssertionJumps(state.op03SimpleParseNodes);
        state.op03SimpleParseNodes = Cleaner.removeUnreachableCode(state.op03SimpleParseNodes, true);

        ConditionalRewriter.identifyNonjumpingConditionals(state.op03SimpleParseNodes, state.blockIdentifierFactory, context.options);
        if (context.options.optionIsSet(OptionsImpl.AGGRESSIVE_DO_COPY)) {
            Op03Rewriters.cloneCodeFromLoop(state.op03SimpleParseNodes, context.options, context.comments);
        }
        if (context.options.getOption(OptionsImpl.AGGRESSIVE_DO_EXTENSION) == Troolean.TRUE) {
            Op03Rewriters.moveJumpsIntoDo(state.variableFactory, state.op03SimpleParseNodes, context.options, context.comments);
        }

        LValueProp.condenseLValues(state.op03SimpleParseNodes);
        if (context.options.getOption(OptionsImpl.FORCE_COND_PROPAGATE) == Troolean.TRUE) {
            Op03Rewriters.propagateToReturn2(state.op03SimpleParseNodes);
        }

        state.op03SimpleParseNodes = Op03Rewriters.removeUselessNops(state.op03SimpleParseNodes);
        Op03Rewriters.removePointlessJumps(state.op03SimpleParseNodes);
        Op03Rewriters.rewriteBreakStatements(state.op03SimpleParseNodes);
        Op03Rewriters.classifyGotos(state.op03SimpleParseNodes);
        if (context.options.getOption(OptionsImpl.LABELLED_BLOCKS)) {
            Op03Rewriters.classifyAnonymousBlockGotos(state.op03SimpleParseNodes, false);
        }
        ConditionalRewriter.identifyNonjumpingConditionals(state.op03SimpleParseNodes, state.blockIdentifierFactory, context.options);

        if (InlineDeAssigner.extractAssignments(state.op03SimpleParseNodes)) {
            state.op03SimpleParseNodes = recoverConditionalsAfterDeassignment(state.op03SimpleParseNodes, context, state.blockIdentifierFactory);
        }

        boolean checkLoopTypeClash = false;
        if (context.options.getOption(OptionsImpl.ARRAY_ITERATOR, context.classFileVersion)) {
            IterLoopRewriter.rewriteArrayForLoops(state.op03SimpleParseNodes);
            checkLoopTypeClash = true;
        }
        if (context.options.getOption(OptionsImpl.COLLECTION_ITERATOR, context.classFileVersion)) {
            IterLoopRewriter.rewriteIteratorWhileLoops(state.op03SimpleParseNodes);
            checkLoopTypeClash = true;
        }

        SynchronizedBlocks.findSynchronizedBlocks(state.op03SimpleParseNodes);
        Op03SimpleStatement.removePointlessSwitchDefaults(state.op03SimpleParseNodes);
        state.op03SimpleParseNodes = Op03Rewriters.removeUselessNops(state.op03SimpleParseNodes);
        Op03Rewriters.rewriteWith(state.op03SimpleParseNodes, new StringBuilderRewriter(context.options, context.classFileVersion));
        Op03Rewriters.rewriteWith(state.op03SimpleParseNodes, new XorRewriter());
        state.op03SimpleParseNodes = Cleaner.removeUnreachableCode(state.op03SimpleParseNodes, true);

        if (context.options.getOption(OptionsImpl.LABELLED_BLOCKS)) {
            Op03Rewriters.labelAnonymousBlocks(state.op03SimpleParseNodes, state.blockIdentifierFactory);
        }

        Op03Rewriters.simplifyConditionals(state.op03SimpleParseNodes, true, context.method);
        Op03Rewriters.extractExceptionMiddle(state.op03SimpleParseNodes);
        Op03Rewriters.removePointlessJumps(state.op03SimpleParseNodes);
        Op03Rewriters.replaceStackVarsWithLocals(state.op03SimpleParseNodes);
        Op03Rewriters.narrowAssignmentTypes(context.method, state.op03SimpleParseNodes);

        if (context.options.getOption(OptionsImpl.SHOW_INFERRABLE, context.classFileVersion)) {
            Op03Rewriters.rewriteWith(state.op03SimpleParseNodes, new ExplicitTypeCallRewriter());
        }
        if (context.passIdx == 0 && checkLoopTypeClash) {
            if (LoopLivenessClash.detect(state.op03SimpleParseNodes, context.bytecodeMeta)) {
                context.comments.addComment(DecompilerComment.TYPE_CLASHES);
            }
            if (context.bytecodeMeta.has(BytecodeMeta.CodeInfoFlag.ITERATED_TYPE_HINTS)) {
                context.comments.addComment(DecompilerComment.ITERATED_TYPE_HINTS);
            }
        }

        if (context.options.getOption(OptionsImpl.LABELLED_BLOCKS)) {
            Op03Rewriters.classifyAnonymousBlockGotos(state.op03SimpleParseNodes, true);
            Op03Rewriters.labelAnonymousBlocks(state.op03SimpleParseNodes, state.blockIdentifierFactory);
        }

        Op03Rewriters.rewriteWith(state.op03SimpleParseNodes, new BadNarrowingArgRewriter());
        Cleaner.reindexInPlace(state.op03SimpleParseNodes);
        Op03SimpleStatement.noteInterestingLifetimes(state.op03SimpleParseNodes);
    }

    private List<Op03SimpleStatement> recoverConditionalsAfterDeassignment(List<Op03SimpleStatement> op03SimpleParseNodes,
                                                                           Op03PipelineContext context,
                                                                           org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifierFactory blockIdentifierFactory) {
        boolean reloop;
        do {
            Op03Rewriters.collapseAssignmentsIntoConditionals(op03SimpleParseNodes, context.options, context.classFileVersion);
            reloop = Op03Rewriters.condenseConditionals(op03SimpleParseNodes);
            reloop = reloop | Op03Rewriters.condenseConditionals2(op03SimpleParseNodes);
            if (reloop) {
                LValueProp.condenseLValues(op03SimpleParseNodes);
            }
            op03SimpleParseNodes = Cleaner.removeUnreachableCode(op03SimpleParseNodes, true);
        } while (reloop);

        Op03Rewriters.removePointlessJumps(op03SimpleParseNodes);
        Op03Rewriters.rewriteBreakStatements(op03SimpleParseNodes);
        Op03Rewriters.classifyGotos(op03SimpleParseNodes);
        if (context.options.getOption(OptionsImpl.LABELLED_BLOCKS)) {
            Op03Rewriters.classifyAnonymousBlockGotos(op03SimpleParseNodes, true);
        }
        ConditionalRewriter.identifyNonjumpingConditionals(op03SimpleParseNodes, blockIdentifierFactory, context.options);
        LValueProp.condenseLValues(op03SimpleParseNodes);
        return Cleaner.sortAndRenumber(op03SimpleParseNodes);
    }
}
