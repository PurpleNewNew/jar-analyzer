package org.benf.cfr.reader.bytecode;

import org.benf.cfr.reader.bytecode.analysis.opgraph.Op01WithProcessedDataAndByteJumps;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredComment;
import org.benf.cfr.reader.bytecode.opcode.JVMInstr;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.entities.attributes.AttributeCode;
import org.benf.cfr.reader.entities.constantpool.ConstantPool;
import org.benf.cfr.reader.state.DCCommonState;
import org.benf.cfr.reader.util.ClassFileVersion;
import org.benf.cfr.reader.util.ConfusedCFRException;
import org.benf.cfr.reader.util.DecompilerComment;
import org.benf.cfr.reader.util.DecompilerComments;
import org.benf.cfr.reader.util.Troolean;
import org.benf.cfr.reader.util.bytestream.ByteData;
import org.benf.cfr.reader.util.bytestream.OffsettingByteData;
import org.benf.cfr.reader.util.getopt.Options;
import org.benf.cfr.reader.util.getopt.OptionsImpl;
import org.benf.cfr.reader.util.output.Dumper;

import java.util.ArrayList;
import java.util.List;

public class CodeAnalyser {
    private final AttributeCode originalCodeAttribute;
    private final ConstantPool cp;

    private Method method;

    private Op04StructuredStatement analysed;
    private static final Op04StructuredStatement POISON = new Op04StructuredStatement(new StructuredComment("Analysis utterly failed (Recursive inlining?)"));

    public CodeAnalyser(AttributeCode attributeCode) {
        this.originalCodeAttribute = attributeCode;
        this.cp = attributeCode.getConstantPool();
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    private static final RecoveryOptions recover0 = new RecoveryOptions(
            new RecoveryOption.TrooleanRO(OptionsImpl.RECOVER_TYPECLASHES, Troolean.TRUE, BytecodeMeta.hasAnyFlag(BytecodeMeta.CodeInfoFlag.LIVENESS_CLASH)),
            new RecoveryOption.TrooleanRO(OptionsImpl.USE_RECOVERED_ITERATOR_TYPE_HINTS, Troolean.TRUE, BytecodeMeta.hasAnyFlag(BytecodeMeta.CodeInfoFlag.ITERATED_TYPE_HINTS)),
            new RecoveryOption.BooleanRO(OptionsImpl.STATIC_INIT_RETURN, Boolean.FALSE)
    );

    private static final RecoveryOptions recoverExAgg = new RecoveryOptions(
            new RecoveryOption.TrooleanRO(OptionsImpl.RECOVER_TYPECLASHES, Troolean.TRUE, BytecodeMeta.hasAnyFlag(BytecodeMeta.CodeInfoFlag.LIVENESS_CLASH)),
            new RecoveryOption.TrooleanRO(OptionsImpl.USE_RECOVERED_ITERATOR_TYPE_HINTS, Troolean.TRUE, BytecodeMeta.hasAnyFlag(BytecodeMeta.CodeInfoFlag.ITERATED_TYPE_HINTS)),
            new RecoveryOption.TrooleanRO(OptionsImpl.FORCE_AGGRESSIVE_EXCEPTION_AGG, Troolean.TRUE, BytecodeMeta.hasAnyFlag(BytecodeMeta.CodeInfoFlag.USES_EXCEPTIONS), DecompilerComment.AGGRESSIVE_EXCEPTION_AGG)
    );

    private static final RecoveryOptions recover0a = new RecoveryOptions(recover0,
            new RecoveryOption.TrooleanRO(OptionsImpl.FORCE_COND_PROPAGATE, Troolean.TRUE, DecompilerComment.COND_PROPAGATE),
            new RecoveryOption.TrooleanRO(OptionsImpl.FORCE_RETURNING_IFS, Troolean.TRUE, DecompilerComment.RETURNING_IFS)
    );

    private static final RecoveryOptions recoverPre1 = new RecoveryOptions(recover0,
            new RecoveryOption.TrooleanRO(OptionsImpl.FORCE_TOPSORT, Troolean.TRUE, DecompilerComment.AGGRESSIVE_TOPOLOGICAL_SORT),
            new RecoveryOption.TrooleanRO(OptionsImpl.REDUCE_COND_SCOPE, Troolean.TRUE),
            new RecoveryOption.TrooleanRO(OptionsImpl.AGGRESSIVE_DUFF, Troolean.TRUE),
            new RecoveryOption.TrooleanRO(OptionsImpl.FOR_LOOP_CAPTURE, Troolean.TRUE),
            new RecoveryOption.BooleanRO(OptionsImpl.LENIENT, Boolean.TRUE),
            new RecoveryOption.TrooleanRO(OptionsImpl.FORCE_COND_PROPAGATE, Troolean.TRUE),
            new RecoveryOption.TrooleanRO(OptionsImpl.REMOVE_DEAD_CONDITIONALS, Troolean.TRUE),
            new RecoveryOption.TrooleanRO(OptionsImpl.FORCE_PRUNE_EXCEPTIONS, Troolean.TRUE, BytecodeMeta.hasAnyFlag(BytecodeMeta.CodeInfoFlag.USES_EXCEPTIONS), DecompilerComment.PRUNE_EXCEPTIONS),
            new RecoveryOption.TrooleanRO(OptionsImpl.FORCE_AGGRESSIVE_EXCEPTION_AGG, Troolean.TRUE, BytecodeMeta.hasAnyFlag(BytecodeMeta.CodeInfoFlag.USES_EXCEPTIONS), DecompilerComment.AGGRESSIVE_EXCEPTION_AGG)
    );

    private static final RecoveryOptions recover1 = new RecoveryOptions(recoverPre1,
            new RecoveryOption.TrooleanRO(OptionsImpl.FORCE_TOPSORT_NOPULL, Troolean.TRUE)
            );

    private static final RecoveryOptions recover2 = new RecoveryOptions(recover1,
            new RecoveryOption.TrooleanRO(OptionsImpl.FORCE_TOPSORT_EXTRA, Troolean.TRUE),
            new RecoveryOption.TrooleanRO(OptionsImpl.FORCE_AGGRESSIVE_EXCEPTION_AGG2, Troolean.TRUE, BytecodeMeta.hasAnyFlag(BytecodeMeta.CodeInfoFlag.USES_EXCEPTIONS))
    );

    private static final RecoveryOptions recover3 = new RecoveryOptions(recover1,
            new RecoveryOption.BooleanRO(OptionsImpl.COMMENT_MONITORS, Boolean.TRUE, BytecodeMeta.hasAnyFlag(BytecodeMeta.CodeInfoFlag.USES_MONITORS), DecompilerComment.COMMENT_MONITORS),
            new RecoveryOption.TrooleanRO(OptionsImpl.FORCE_RETURNING_IFS, Troolean.TRUE, DecompilerComment.RETURNING_IFS)
    );

    private static final RecoveryOptions recover3a = new RecoveryOptions(recover1,
            new RecoveryOption.IntRO(OptionsImpl.AGGRESSIVE_DO_COPY, 4),
            new RecoveryOption.TrooleanRO(OptionsImpl.AGGRESSIVE_DO_EXTENSION, Troolean.TRUE),
            new RecoveryOption.TrooleanRO(OptionsImpl.FORCE_TOPSORT_EXTRA, Troolean.TRUE),
            new RecoveryOption.TrooleanRO(OptionsImpl.FORCE_AGGRESSIVE_EXCEPTION_AGG2, Troolean.TRUE, BytecodeMeta.hasAnyFlag(BytecodeMeta.CodeInfoFlag.USES_EXCEPTIONS))
    );

    /*
     * These recoveries allow potential semantic changes, so we want to be very careful and make sure that we don't apply unless we have to,
     * and warn if we base actions off them.
     */
    private static final RecoveryOptions recoverIgnoreExceptions = new RecoveryOptions(recover3,
            new RecoveryOption.BooleanRO(OptionsImpl.IGNORE_EXCEPTIONS_ALWAYS, true, BytecodeMeta.checkParam(OptionsImpl.IGNORE_EXCEPTIONS), DecompilerComment.DROP_EXCEPTIONS)
    );

    private static final RecoveryOptions recoverMalformed2a = new RecoveryOptions(recover2,
            // Don't bother with this recovery pass unless we've detected it will make a difference.
            new RecoveryOption.TrooleanRO(OptionsImpl.ALLOW_MALFORMED_SWITCH, Troolean.TRUE, BytecodeMeta.hasAnyFlag(BytecodeMeta.CodeInfoFlag.MALFORMED_SWITCH))
    );

    private static final RecoveryOptions[] recoveryOptionsArr = new RecoveryOptions[]{recover0, recover0a, recoverPre1, recover1, recover2, recoverExAgg, recover3, recover3a, recoverIgnoreExceptions, recoverMalformed2a};

    /*
     * This method should not throw.  If it does, something serious has gone wrong.
     */
    public Op04StructuredStatement getAnalysis(DCCommonState dcCommonState) {
        if (analysed == POISON) {
            /*
             * We shouldn't get here, unless a method needs to inline a copy of itself.
             * (which can't end well!)
             *
             * Seen when decompiling scala - a lambda which (to java) looks like an
             * intermediate.
             */
            throw new ConfusedCFRException("Recursive analysis");
        }
        if (analysed != null) {
            return analysed;
        }
        analysed = POISON;

        Options options = dcCommonState.getOptions();
        List<Op01WithProcessedDataAndByteJumps> instrs = getInstrs();

        AnalysisResult res;

        /*
         * Very quick scan to check for presence of certain instructions.
         */
        BytecodeMeta bytecodeMeta = new BytecodeMeta(instrs, originalCodeAttribute, options);

        if (options.optionIsSet(OptionsImpl.FORCE_PASS)) {
            int pass = options.getOption(OptionsImpl.FORCE_PASS);
            if (pass < 0 || pass >= recoveryOptionsArr.length) {
                throw new IllegalArgumentException("Illegal recovery pass idx");
            }
            RecoveryOptions.Applied applied = recoveryOptionsArr[pass].apply(dcCommonState, options, bytecodeMeta);
            res = getAnalysisOrWrapFail(pass, instrs, dcCommonState, applied.options, applied.comments, bytecodeMeta);
        } else {

            res = getAnalysisOrWrapFail(0, instrs, dcCommonState, options, null, bytecodeMeta);

            if (res.isFailed() && options.getOption(OptionsImpl.RECOVER)) {
                int passIdx = 1;
                for (RecoveryOptions recoveryOptions : recoveryOptionsArr) {
                    RecoveryOptions.Applied applied = recoveryOptions.apply(dcCommonState, options, bytecodeMeta);
                    if (!applied.valid) continue;
                    AnalysisResult nextRes = getAnalysisOrWrapFail(passIdx++, instrs, dcCommonState, applied.options, applied.comments, bytecodeMeta);
                    if (res.isFailed() && nextRes.isFailed()) {
                        if (!nextRes.isThrown()) {
                            if (res.isThrown()) {
                                // If they both failed, only replace if the later failure is not an exception, and the earlier one was.
                                res = nextRes;
                            } else if (res.getComments().contains(DecompilerComment.UNABLE_TO_STRUCTURE) && !nextRes.getComments().contains(DecompilerComment.UNABLE_TO_STRUCTURE)) {
                                // Or if we've failed, but managed to structure.
                                res = nextRes;
                            }
                        }
                    } else {
                        res = nextRes;
                    }
                    if (res.isFailed()) continue;
                    break;
                }
            }
        }

        if (res.getComments() != null) {
            method.setComments(res.getComments());
        }

        /*
         * Take the anonymous usages from the selected result.
         */
        res.getAnonymousClassUsage().useNotes();

        analysed = res.getCode();
        return analysed;
    }

    /*
     * This list isn't going to change with recovery passes, so avoid recomputing.
     * (though we may infer additional items from it if we recover from illegal bytecode)
     */
    private List<Op01WithProcessedDataAndByteJumps> getInstrs() {
        ByteData rawCode = originalCodeAttribute.getRawData();
        long codeLength = originalCodeAttribute.getCodeLength();
        ArrayList<Op01WithProcessedDataAndByteJumps> instrs = new ArrayList<Op01WithProcessedDataAndByteJumps>();
        OffsettingByteData bdCode = rawCode.getOffsettingOffsetData(0);
        int offset = 0;

        // We insert a fake NOP right at the start, so that we always know that each operation has a valid
        // parent.  This sentinel assumption is used when inserting try { catch blocks.
        instrs.add(JVMInstr.NOP.createOperation(null, cp, -1));
        do {
            JVMInstr instr = JVMInstr.find(bdCode.getS1At(0));
            Op01WithProcessedDataAndByteJumps oc = instr.createOperation(bdCode, cp, offset);
            int length = oc.getInstructionLength();
            instrs.add(oc);
            offset += length;
            bdCode.advance(length);
        } while (offset < codeLength);
        return instrs;
    }

    private AnalysisResult getAnalysisOrWrapFail(int passIdx, List<Op01WithProcessedDataAndByteJumps> instrs, DCCommonState commonState, Options options, List<DecompilerComment> extraComments, BytecodeMeta bytecodeMeta) {
        try {
            AnalysisResult res = getAnalysisInner(instrs, commonState, options, bytecodeMeta, passIdx);
            if (extraComments != null) res.getComments().addComments(extraComments);
            return res;
        } catch (RuntimeException e) {
            return new AnalysisResultFromException(e, options.getOption(OptionsImpl.DUMP_EXCEPTION_STACK_TRACE));
        }
    }

    /*
     * Note that the options passed in here only apply to this function - don't pass around.
     *
     * passIdx is only useful for breakpointing.
     */
    private AnalysisResult getAnalysisInner(List<Op01WithProcessedDataAndByteJumps> instrs, DCCommonState dcCommonState, Options options, BytecodeMeta bytecodeMeta, int passIdx) {

        boolean willSort = options.getOption(OptionsImpl.FORCE_TOPSORT) == Troolean.TRUE;

        ClassFile classFile = method.getClassFile();
        ClassFileVersion classFileVersion = classFile.getClassFileVersion();

        DecompilerComments comments = new DecompilerComments();

        boolean aggressiveSizeReductions = options.getOption(OptionsImpl.AGGRESSIVE_SIZE_REDUCTION_THRESHOLD) < instrs.size();
        if (aggressiveSizeReductions) {
            comments.addComment("Opcode count of " + instrs.size() + " triggered aggressive code reduction.  Override with --" + OptionsImpl.AGGRESSIVE_SIZE_REDUCTION_THRESHOLD.getName() + ".");
        }

        Op03PipelineContext pipelineContext = new Op03PipelineContext(
                dcCommonState,
                options,
                method,
                classFile,
                classFileVersion,
                bytecodeMeta,
                comments,
                originalCodeAttribute,
                cp,
                willSort,
                aggressiveSizeReductions,
                passIdx
        );
        Op03PipelineState pipelineState = new Op03AnalysisPipeline(pipelineContext).analyse(instrs, pipelineContext);
        MethodAnalysisContext analysisContext = new MethodAnalysisContext(
                dcCommonState,
                options,
                method,
                classFile,
                classFileVersion,
                bytecodeMeta,
                comments,
                pipelineState.variableFactory,
                pipelineState.anonymousClassUsage,
                originalCodeAttribute,
                pipelineState.lutByOffset,
                cp,
                pipelineState.blockIdentifierFactory,
                ModernFeatureStrategy.from(options, classFileVersion)
        );
        Op04StructuredStatement block = new StructuredAnalysisPipeline(analysisContext).analyse(pipelineState.op03SimpleParseNodes, analysisContext);

        // Only check for type clashes on first pass.
        if (passIdx == 0) {
            if (Op04StructuredStatement.checkTypeClashes(block, bytecodeMeta)) {
                comments.addComment(DecompilerComment.TYPE_CLASHES);
            }
        }

        return new AnalysisResultSuccessful(comments, block, pipelineState.anonymousClassUsage);
    }

    public void dump(Dumper d) {
        d.newln();
        analysed.dump(d);
    }

    public void releaseCode() {
        analysed = null;
    }
}
