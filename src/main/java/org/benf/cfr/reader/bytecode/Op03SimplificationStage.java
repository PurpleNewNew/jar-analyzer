package org.benf.cfr.reader.bytecode;

import org.benf.cfr.reader.bytecode.analysis.opgraph.Op02WithProcessedDataAndRefs;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op2rewriters.TypeHintRecovery;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op2rewriters.TypeHintRecoveryImpl;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op2rewriters.TypeHintRecoveryNone;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.BadBoolAssignmentRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.Cleaner;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.FinallyRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.GenericInferer;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.KotlinSwitchHandler;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.LValueProp;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.LValuePropSimple;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.Misc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.NullTypedLValueRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.Op03Rewriters;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.StaticInitReturnRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op3rewriters.SwitchReplacer;
import org.benf.cfr.reader.bytecode.analysis.variables.VariableFactory;
import org.benf.cfr.reader.util.Troolean;
import org.benf.cfr.reader.util.getopt.OptionsImpl;

final class Op03SimplificationStage {
    void simplify(Op03PipelineState state, Op03PipelineContext context) {
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

        Misc.flattenCompoundStatements(state.op03SimpleParseNodes);
        Op03Rewriters.rewriteWith(state.op03SimpleParseNodes, new NullTypedLValueRewriter());
        Op03Rewriters.rewriteWith(state.op03SimpleParseNodes, new BadBoolAssignmentRewriter());
        GenericInferer.inferGenericObjectInfoFromCalls(state.op03SimpleParseNodes);

        if (context.options.getOption(OptionsImpl.RELINK_CONSTANTS)) {
            Op03Rewriters.relinkInstanceConstants(context.classFile.getRefClassType(), state.op03SimpleParseNodes, context.commonState);
        }

        state.op03SimpleParseNodes = Cleaner.sortAndRenumber(state.op03SimpleParseNodes);
        if (context.aggressiveSizeReductions) {
            state.op03SimpleParseNodes = LValuePropSimple.condenseSimpleLValues(state.op03SimpleParseNodes);
        }

        Op03Rewriters.nopIsolatedStackValues(state.op03SimpleParseNodes);
        Op03SimpleStatement.assignSSAIdentifiers(context.method, state.op03SimpleParseNodes);
        Op03Rewriters.condenseStaticInstances(state.op03SimpleParseNodes);
        LValueProp.condenseLValues(state.op03SimpleParseNodes);

        if (context.options.getOption(OptionsImpl.REMOVE_DEAD_CONDITIONALS) == Troolean.TRUE) {
            state.op03SimpleParseNodes = Op03Rewriters.removeDeadConditionals(state.op03SimpleParseNodes);
        }
        state.op03SimpleParseNodes = Cleaner.sortAndRenumber(state.op03SimpleParseNodes);

        state.op03SimpleParseNodes = KotlinSwitchHandler.extractStringSwitches(state.op03SimpleParseNodes, context.bytecodeMeta);
        SwitchReplacer.replaceRawSwitches(context.method, state.op03SimpleParseNodes, state.blockIdentifierFactory, context.options, context.comments, context.bytecodeMeta);
        state.op03SimpleParseNodes = Cleaner.sortAndRenumber(state.op03SimpleParseNodes);

        Op03Rewriters.removePointlessJumps(state.op03SimpleParseNodes);
        state.op03SimpleParseNodes = Op03Rewriters.eliminateCatchTemporaries(state.op03SimpleParseNodes);
        Op03Rewriters.identifyCatchBlocks(state.op03SimpleParseNodes, state.blockIdentifierFactory);
        Op03Rewriters.combineTryCatchBlocks(state.op03SimpleParseNodes);

        if (context.options.getOption(OptionsImpl.COMMENT_MONITORS)) {
            Op03Rewriters.commentMonitors(state.op03SimpleParseNodes);
        }

        state.anonymousClassUsage = new AnonymousClassUsage();
        Op03Rewriters.condenseConstruction(
                context.commonState,
                context.method,
                state.op03SimpleParseNodes,
                state.anonymousClassUsage,
                context.modernFeatures
        );
        state.op03SimpleParseNodes = Cleaner.sortAndRenumber(state.op03SimpleParseNodes);
        LValueProp.condenseLValues(state.op03SimpleParseNodes);
        Op03Rewriters.condenseLValueChain1(state.op03SimpleParseNodes);

        StaticInitReturnRewriter.rewrite(context.options, context.method, state.op03SimpleParseNodes);
        state.op03SimpleParseNodes = Op03Rewriters.removeRedundantTries(state.op03SimpleParseNodes);
        FinallyRewriter.identifyFinally(context.options, context.method, state.op03SimpleParseNodes, state.blockIdentifierFactory);
        state.op03SimpleParseNodes = Cleaner.removeUnreachableCode(state.op03SimpleParseNodes, !context.willSort);
        state.op03SimpleParseNodes = Cleaner.sortAndRenumber(state.op03SimpleParseNodes);

        Op03Rewriters.extendTryBlocks(context.commonState, state.op03SimpleParseNodes);
        Op03Rewriters.combineTryCatchEnds(state.op03SimpleParseNodes);
        Op03Rewriters.removePointlessExpressionStatements(state.op03SimpleParseNodes);
        state.op03SimpleParseNodes = Cleaner.removeUnreachableCode(state.op03SimpleParseNodes, !context.willSort);

        Op03Rewriters.replacePrePostChangeAssignments(state.op03SimpleParseNodes);
        Op03Rewriters.pushPreChangeBack(state.op03SimpleParseNodes);
        Op03Rewriters.condenseLValueChain2(state.op03SimpleParseNodes);
        Op03Rewriters.collapseAssignmentsIntoConditionals(state.op03SimpleParseNodes, context.options, context.classFileVersion);
        LValueProp.condenseLValues(state.op03SimpleParseNodes);
        state.op03SimpleParseNodes = Cleaner.sortAndRenumber(state.op03SimpleParseNodes);
    }
}
