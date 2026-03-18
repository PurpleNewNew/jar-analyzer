package org.benf.cfr.reader.bytecode;

import org.benf.cfr.reader.bytecode.analysis.opgraph.Op01WithProcessedDataAndByteJumps;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op02WithProcessedDataAndRefs;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op02obf.Op02Obf;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op2rewriters.GetClassTestInnerConstructor;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op2rewriters.GetClassTestLambda;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op2rewriters.Op02GetClassRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op2rewriters.Op02RedundantStoreRewriter;
import org.benf.cfr.reader.entities.exceptions.ExceptionAggregator;
import org.benf.cfr.reader.entities.exceptions.ExceptionTableEntry;
import org.benf.cfr.reader.util.DecompilerComment;
import org.benf.cfr.reader.util.Troolean;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.getopt.OptionsImpl;

import java.util.List;

final class ExceptionCfgNormalizationStage {
    void normalize(List<Op01WithProcessedDataAndByteJumps> instrs,
                   Op03PipelineState state,
                   Op03PipelineContext context) {
        List<ExceptionTableEntry> exceptionTableEntries = context.originalCodeAttribute.getExceptionTableEntries();
        if (context.options.getOption(OptionsImpl.IGNORE_EXCEPTIONS_ALWAYS)) {
            exceptionTableEntries = ListFactory.newList();
        }

        ExceptionAggregator exceptions = new ExceptionAggregator(
                exceptionTableEntries,
                state.blockIdentifierFactory,
                state.lutByOffset,
                instrs,
                context.options,
                context.constantPool,
                context.comments
        );
        if (exceptions.RemovedLoopingExceptions()) {
            context.comments.addComment(DecompilerComment.LOOPING_EXCEPTIONS);
        }

        if (context.options.getOption(OptionsImpl.FORCE_PRUNE_EXCEPTIONS) == Troolean.TRUE) {
            exceptions.aggressiveRethrowPruning();
            if (context.options.getOption(OptionsImpl.ANTI_OBF)) {
                exceptions.aggressiveImpossiblePruning();
            }
            exceptions.removeSynchronisedHandlers(state.lutByIdx);
        }

        if (context.options.getOption(OptionsImpl.REWRITE_LAMBDAS, context.classFileVersion)
                && context.bytecodeMeta.has(BytecodeMeta.CodeInfoFlag.USES_INVOKEDYNAMIC)) {
            Op02GetClassRewriter.removeInvokeGetClass(context.classFile, state.op2list, GetClassTestLambda.INSTANCE);
        }
        Op02GetClassRewriter.removeInvokeGetClass(context.classFile, state.op2list, GetClassTestInnerConstructor.INSTANCE);

        if (context.options.getOption(OptionsImpl.CONTROL_FLOW_OBF)) {
            Op02Obf.removeControlFlowExceptions(context.method, exceptions, state.op2list, state.lutByOffset);
            Op02Obf.removeNumericObf(context.method, state.op2list);
        }

        state.op2list = Op02WithProcessedDataAndRefs.insertExceptionBlocks(
                state.op2list,
                exceptions,
                state.lutByOffset,
                context.constantPool,
                context.originalCodeAttribute.getCodeLength(),
                context.options
        );

        if (context.aggressiveSizeReductions) {
            Op02RedundantStoreRewriter.rewrite(state.op2list, context.originalCodeAttribute.getMaxLocals());
        }

        DecompilerComment o2stackComment = Op02WithProcessedDataAndRefs.populateStackInfo(state.op2list, context.method);
        if (Op02WithProcessedDataAndRefs.processJSR(state.op2list)) {
            o2stackComment = Op02WithProcessedDataAndRefs.populateStackInfo(state.op2list, context.method);
        }
        if (o2stackComment != null) {
            context.comments.addComment(o2stackComment);
        }

        Op02WithProcessedDataAndRefs.unlinkUnreachable(state.op2list);
        Op02WithProcessedDataAndRefs.discoverStorageLiveness(context.method, context.comments, state.op2list, context.bytecodeMeta);
    }
}
