package org.benf.cfr.reader.bytecode;

import org.benf.cfr.reader.bytecode.analysis.opgraph.Op01WithProcessedDataAndByteJumps;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op02WithProcessedDataAndRefs;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op03Blocks;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op2rewriters.TypeHintRecovery;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op2rewriters.TypeHintRecoveryImpl;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op2rewriters.TypeHintRecoveryNone;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.AnonymousArray;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.AnonymousBlocks;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.BadBoolAssignmentRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.BadNarrowingArgRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.BreakRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.Cleaner;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.ConditionalCondenser;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.ConditionalRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.DeadConditionalRemover;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.EclipseLoops;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.ExceptionRewriters;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.FinallyRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.GenericInferer;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.InlineDeAssigner;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.InstanceConstants;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.IterLoopRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.KotlinSwitchHandler;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.LValueCondense;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.LValueProp;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.LValuePropSimple;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.LoopIdentifier;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.LoopLivenessClash;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.Misc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.NarrowingTypeRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.NegativeJumps;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.NullTypedLValueRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.Op03Rewriters;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.PointlessJumps;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.PushThroughGoto;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.RedundantTries;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.RemoveDeterministicJumps;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.StaticInstanceCondenser;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.SwitchReplacer;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.SynchronizedBlocks;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExplicitTypeCallRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.StackVarToLocalRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.StringBuilderRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.XorRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.statement.IfStatement;
import org.benf.cfr.reader.bytecode.analysis.variables.VariableFactory;
import org.benf.cfr.reader.util.DecompilerComment;
import org.benf.cfr.reader.util.Troolean;
import org.benf.cfr.reader.util.getopt.OptionsImpl;

import java.util.List;

final class Op03AnalysisPipeline {
    private final BytecodeDecodeStage bytecodeDecodeStage;
    private final ExceptionCfgNormalizationStage exceptionCfgNormalizationStage;

    Op03AnalysisPipeline(Op03PipelineContext context) {
        this.bytecodeDecodeStage = new BytecodeDecodeStage(context);
        this.exceptionCfgNormalizationStage = new ExceptionCfgNormalizationStage();
    }

    Op03PipelineState analyse(List<Op01WithProcessedDataAndByteJumps> instrs, Op03PipelineContext context) {
        Op03PipelineState state = bytecodeDecodeStage.decode(instrs);
        exceptionCfgNormalizationStage.normalize(instrs, state, context);
        simplify(state, context);
        shape(state, context);
        return state;
    }

    private void simplify(Op03PipelineState state, Op03PipelineContext context) {
        initializeSimpleStatements(state, context);
        normalizeEarlyStatements(state, context);
        normalizeConstructionAndTryFlow(state, context);
        stabilizeSimpleAssignments(state, context);
    }

    private void initializeSimpleStatements(Op03PipelineState state, Op03PipelineContext context) {
        state.variableFactory = new VariableFactory(context.method, context.bytecodeMeta);
        TypeHintRecovery typeHintRecovery = context.options.optionIsSet(OptionsImpl.USE_RECOVERED_ITERATOR_TYPE_HINTS)
                ? new TypeHintRecoveryImpl(context.bytecodeMeta)
                : TypeHintRecoveryNone.INSTANCE;

        state.op03SimpleParseNodes = Op02WithProcessedDataAndRefs.convertToOp03List(
                state.op2list,
                context.method,
                state.variableFactory,
                state.blockIdentifierFactory,
                context.commonState,
                context.comments,
                typeHintRecovery
        );
        state.op03SimpleParseNodes = Cleaner.sortAndRenumber(state.op03SimpleParseNodes);
    }

    private void normalizeEarlyStatements(Op03PipelineState state, Op03PipelineContext context) {
        Misc.flattenCompoundStatements(state.op03SimpleParseNodes);
        rewriteExpressions(state.op03SimpleParseNodes, new NullTypedLValueRewriter());
        rewriteExpressions(state.op03SimpleParseNodes, new BadBoolAssignmentRewriter());
        GenericInferer.inferGenericObjectInfoFromCalls(state.op03SimpleParseNodes);

        if (context.options.getOption(OptionsImpl.RELINK_CONSTANTS)) {
            InstanceConstants.INSTANCE.rewrite(context.classFile.getRefClassType(), state.op03SimpleParseNodes, context.commonState);
        }

        state.op03SimpleParseNodes = Cleaner.sortAndRenumber(state.op03SimpleParseNodes);
        if (context.aggressiveSizeReductions) {
            state.op03SimpleParseNodes = LValuePropSimple.condenseSimpleLValues(state.op03SimpleParseNodes);
        }

        Op03Rewriters.nopIsolatedStackValues(state.op03SimpleParseNodes);
        Op03SimpleStatement.assignSSAIdentifiers(context.method, state.op03SimpleParseNodes);
        StaticInstanceCondenser.INSTANCE.rewrite(state.op03SimpleParseNodes);
        LValueProp.condenseLValues(state.op03SimpleParseNodes);

        if (context.options.getOption(OptionsImpl.REMOVE_DEAD_CONDITIONALS) == Troolean.TRUE) {
            state.op03SimpleParseNodes = DeadConditionalRemover.INSTANCE.rewrite(state.op03SimpleParseNodes);
        }
        state.op03SimpleParseNodes = Cleaner.sortAndRenumber(state.op03SimpleParseNodes);

        state.op03SimpleParseNodes = KotlinSwitchHandler.extractStringSwitches(state.op03SimpleParseNodes, context.bytecodeMeta);
        SwitchReplacer.replaceRawSwitches(context.method, state.op03SimpleParseNodes, state.blockIdentifierFactory, context.options, context.comments, context.bytecodeMeta);
        state.op03SimpleParseNodes = Cleaner.sortAndRenumber(state.op03SimpleParseNodes);
    }

    private void normalizeConstructionAndTryFlow(Op03PipelineState state, Op03PipelineContext context) {
        PointlessJumps.normalizePointlessJumps(state.op03SimpleParseNodes);
        state.op03SimpleParseNodes = Op03Rewriters.prepareCatchBlocks(state.op03SimpleParseNodes, state.blockIdentifierFactory);

        state.anonymousClassUsage = new AnonymousClassUsage();
        Op03Rewriters.normalizeConstructionPhase(
                context.commonState,
                context.options,
                context.method,
                state.op03SimpleParseNodes,
                state.anonymousClassUsage,
                context.modernFeatures
        );
        state.op03SimpleParseNodes = Cleaner.sortAndRenumber(state.op03SimpleParseNodes);
        LValueProp.condenseLValues(state.op03SimpleParseNodes);
        LValueCondense.condenseLValueChain1(state.op03SimpleParseNodes);

        state.op03SimpleParseNodes = RedundantTries.removeRedundantTries(state.op03SimpleParseNodes);
        FinallyRewriter.identifyFinally(context.options, context.method, state.op03SimpleParseNodes, state.blockIdentifierFactory);
        state.op03SimpleParseNodes = Cleaner.removeUnreachableAndSort(state.op03SimpleParseNodes, !context.willSort);

        Op03Rewriters.normalizeTryBounds(context.commonState, state.op03SimpleParseNodes);
        Op03Rewriters.removePointlessExpressionStatements(state.op03SimpleParseNodes);
        state.op03SimpleParseNodes = Cleaner.removeUnreachableCode(state.op03SimpleParseNodes, !context.willSort);
    }

    private void stabilizeSimpleAssignments(Op03PipelineState state, Op03PipelineContext context) {
        Op03Rewriters.replacePrePostChangeAssignments(state.op03SimpleParseNodes);
        Op03Rewriters.pushPreChangeBack(state.op03SimpleParseNodes);
        LValueCondense.condenseLValueChain2(state.op03SimpleParseNodes);
        Op03Rewriters.collapseAssignmentsIntoConditionals(state.op03SimpleParseNodes, context.options, context.classFileVersion);
        LValueProp.condenseLValues(state.op03SimpleParseNodes);
        state.op03SimpleParseNodes = Cleaner.sortAndRenumber(state.op03SimpleParseNodes);
    }

    private void shape(Op03PipelineState state, Op03PipelineContext context) {
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
            PointlessJumps.normalizePointlessJumps(state.op03SimpleParseNodes);
            SwitchReplacer.rebuildSwitches(state.op03SimpleParseNodes, context.options, context.comments, context.bytecodeMeta);
            Op03Rewriters.rejoinBlocks(state.op03SimpleParseNodes);
            state.op03SimpleParseNodes = Op03Rewriters.normalizeTopSortedTryBounds(context.commonState, state.op03SimpleParseNodes);
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

        state.op03SimpleParseNodes = ConditionalCondenser.condenseConditionalsUntilStable(
                state.op03SimpleParseNodes,
                context.options,
                context.classFileVersion
        );

        AnonymousArray.resugarAnonymousArrays(state.op03SimpleParseNodes);
        Op03Rewriters.simplifyConditionals(state.op03SimpleParseNodes, false, context.method);
        state.op03SimpleParseNodes = Cleaner.sortAndRenumber(state.op03SimpleParseNodes);
        NegativeJumps.normalizeNegativeJumps(state.op03SimpleParseNodes, false);
        optimiseForTypes(state.op03SimpleParseNodes);

        if (context.options.getOption(OptionsImpl.ECLIPSE)) {
            EclipseLoops.eclipseLoopPass(state.op03SimpleParseNodes);
        }

        normalizeLoopAndJumpFlow(state, context);
        stabilizeConditionalFlow(state, context);
        finalizeTypeAndOutputFlow(state, context);
    }

    private void normalizeLoopAndJumpFlow(Op03PipelineState state, Op03PipelineContext context) {
        state.op03SimpleParseNodes = Cleaner.removeUnreachableCode(state.op03SimpleParseNodes, true);
        LoopIdentifier.identifyLoops(context.method, state.op03SimpleParseNodes, state.blockIdentifierFactory);
        Op03Rewriters.rewriteBadCompares(state.variableFactory, state.op03SimpleParseNodes);
        state.op03SimpleParseNodes = PushThroughGoto.normalizeByPushingThroughGotos(state.op03SimpleParseNodes);

        if (context.options.getOption(OptionsImpl.FORCE_RETURNING_IFS) == Troolean.TRUE) {
            Op03Rewriters.replaceReturningIfs(state.op03SimpleParseNodes, false);
        }

        state.op03SimpleParseNodes = Cleaner.removeUnreachableAndSort(state.op03SimpleParseNodes, true);
        BreakRewriter.classifyLoopExits(state.op03SimpleParseNodes);
        Op03Rewriters.normalizeLoopShapes(context.options, state.op03SimpleParseNodes);
        Op03Rewriters.removeSynchronizedCatchBlocks(context.options, state.op03SimpleParseNodes);
        state.op03SimpleParseNodes = Cleaner.removeDetachedStatements(state.op03SimpleParseNodes);
        PointlessJumps.normalizePointlessJumps(state.op03SimpleParseNodes);
        Op03Rewriters.extractExceptionJumps(state.op03SimpleParseNodes);
        Op03Rewriters.extractAssertionJumps(state.op03SimpleParseNodes);
        state.op03SimpleParseNodes = Cleaner.removeUnreachableCode(state.op03SimpleParseNodes, true);
    }

    private void stabilizeConditionalFlow(Op03PipelineState state, Op03PipelineContext context) {
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

        state.op03SimpleParseNodes = Cleaner.removeDetachedStatements(state.op03SimpleParseNodes);
        PointlessJumps.normalizePointlessJumps(state.op03SimpleParseNodes);
        BreakRewriter.classifyLoopExits(state.op03SimpleParseNodes);
        ConditionalRewriter.classifyAndIdentifyNonjumpingConditionals(
                state.op03SimpleParseNodes,
                state.blockIdentifierFactory,
                context.options,
                false
        );

        if (InlineDeAssigner.extractAssignments(state.op03SimpleParseNodes)) {
            state.op03SimpleParseNodes = ConditionalCondenser.recoverConditionalsAfterDeassignment(
                    state.op03SimpleParseNodes,
                    state.blockIdentifierFactory,
                    context.options,
                    context.classFileVersion
            );
        }
    }

    private void finalizeTypeAndOutputFlow(Op03PipelineState state, Op03PipelineContext context) {
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
        rewriteExpressions(state.op03SimpleParseNodes, new StringBuilderRewriter(context.options, context.classFileVersion));
        rewriteExpressions(state.op03SimpleParseNodes, new XorRewriter());
        state.op03SimpleParseNodes = Cleaner.removeUnreachableCode(state.op03SimpleParseNodes, true);

        if (context.options.getOption(OptionsImpl.LABELLED_BLOCKS)) {
            AnonymousBlocks.labelAnonymousBlocks(state.op03SimpleParseNodes, state.blockIdentifierFactory);
        }

        Op03Rewriters.simplifyConditionals(state.op03SimpleParseNodes, true, context.method);
        Op03Rewriters.extractExceptionMiddle(state.op03SimpleParseNodes);
        PointlessJumps.normalizePointlessJumps(state.op03SimpleParseNodes);
        rewriteExpressions(state.op03SimpleParseNodes, new StackVarToLocalRewriter());
        NarrowingTypeRewriter.rewrite(context.method, state.op03SimpleParseNodes);

        if (context.options.getOption(OptionsImpl.SHOW_INFERRABLE, context.classFileVersion)) {
            rewriteExpressions(state.op03SimpleParseNodes, new ExplicitTypeCallRewriter());
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
            ConditionalRewriter.classifyAnonymousBlockGotos(state.op03SimpleParseNodes, true);
            AnonymousBlocks.labelAnonymousBlocks(state.op03SimpleParseNodes, state.blockIdentifierFactory);
        }

        rewriteExpressions(state.op03SimpleParseNodes, new BadNarrowingArgRewriter());
        Cleaner.reindexInPlace(state.op03SimpleParseNodes);
        Op03SimpleStatement.noteInterestingLifetimes(state.op03SimpleParseNodes);
    }

    private static void optimiseForTypes(List<Op03SimpleStatement> statements) {
        for (Op03SimpleStatement statement : statements) {
            if (statement.getStatement() instanceof IfStatement) {
                ((IfStatement) statement.getStatement()).optimiseForTypes();
            }
        }
    }

    private static void rewriteExpressions(List<Op03SimpleStatement> statements, ExpressionRewriter expressionRewriter) {
        for (Op03SimpleStatement statement : statements) {
            statement.rewrite(expressionRewriter);
        }
    }
}
