package org.benf.cfr.reader.bytecode;

import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLocFactory;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLocFactoryImpl;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLocFactoryStub;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op01WithProcessedDataAndByteJumps;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op02WithProcessedDataAndRefs;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifierFactory;
import org.benf.cfr.reader.bytecode.opcode.JVMInstr;
import org.benf.cfr.reader.util.ConfusedCFRException;
import org.benf.cfr.reader.util.DecompilerComment;
import org.benf.cfr.reader.util.UnverifiableJumpException;
import org.benf.cfr.reader.util.bytestream.ByteData;
import org.benf.cfr.reader.util.bytestream.OffsettingByteData;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.getopt.OptionsImpl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

final class BytecodeDecodeStage {
    private final Op03PipelineContext context;

    BytecodeDecodeStage(Op03PipelineContext context) {
        this.context = context;
    }

    Op03PipelineState decode(List<Op01WithProcessedDataAndByteJumps> instrs) {
        SortedMap<Integer, Integer> lutByOffset = new TreeMap<Integer, Integer>();
        Map<Integer, Integer> lutByIdx = new HashMap<Integer, Integer>();
        int idx = 0;
        int offset = -1;
        for (Op01WithProcessedDataAndByteJumps op : instrs) {
            lutByOffset.put(offset, idx);
            lutByIdx.put(idx, offset);
            offset += op.getInstructionLength();
            idx++;
        }
        lutByIdx.put(0, -1);
        lutByOffset.put(-1, 0);

        List<Op01WithProcessedDataAndByteJumps> op1list = ListFactory.newList();
        List<Op02WithProcessedDataAndRefs> op2list = ListFactory.newList();
        BytecodeLocFactory locFactory = context.options.getOption(OptionsImpl.TRACK_BYTECODE_LOC)
                ? BytecodeLocFactoryImpl.INSTANCE
                : BytecodeLocFactoryStub.INSTANCE;
        for (int x = 0; x < instrs.size(); ++x) {
            Op01WithProcessedDataAndByteJumps op1 = instrs.get(x);
            op1list.add(op1);
            op2list.add(op1.createOp2(context.constantPool, x, locFactory, context.method));
        }

        for (int x = 0, len = op1list.size(); x < len; ++x) {
            int offsetOfThisInstruction = lutByIdx.get(x);
            int[] targetIdxs;
            try {
                targetIdxs = op1list.get(x).getAbsoluteIndexJumps(offsetOfThisInstruction, lutByOffset);
            } catch (UnverifiableJumpException e) {
                context.comments.addComment(DecompilerComment.UNVERIFIABLE_BYTECODE_BAD_JUMP);
                generateUnverifiable(x, op1list, op2list, lutByIdx, lutByOffset, locFactory);
                try {
                    targetIdxs = op1list.get(x).getAbsoluteIndexJumps(offsetOfThisInstruction, lutByOffset);
                } catch (UnverifiableJumpException e2) {
                    throw new ConfusedCFRException("Can't recover from unverifiable jumps at " + offsetOfThisInstruction);
                }
                len = op1list.size();
            }
            Op02WithProcessedDataAndRefs source = op2list.get(x);
            for (int targetIdx : targetIdxs) {
                if (targetIdx < len) {
                    Op02WithProcessedDataAndRefs target = op2list.get(targetIdx);
                    source.addTarget(target);
                    target.addSource(source);
                }
            }
        }

        return new Op03PipelineState(lutByOffset, lutByIdx, new BlockIdentifierFactory(), op2list);
    }

    private void generateUnverifiable(int index,
                                      List<Op01WithProcessedDataAndByteJumps> op1list,
                                      List<Op02WithProcessedDataAndRefs> op2list,
                                      Map<Integer, Integer> lutByIdx,
                                      SortedMap<Integer, Integer> lutByOffset,
                                      BytecodeLocFactory locFactory) {
        Op01WithProcessedDataAndByteJumps instr = op1list.get(index);
        int thisRaw = instr.getOriginalRawOffset();
        int[] thisTargets = instr.getRawTargetOffsets();
        for (int target : thisTargets) {
            if (lutByOffset.get(target + thisRaw) == null) {
                generateUnverifiableInstr(target + thisRaw, op1list, op2list, lutByIdx, lutByOffset, locFactory);
            }
        }
    }

    private void generateUnverifiableInstr(int offset,
                                           List<Op01WithProcessedDataAndByteJumps> op1list,
                                           List<Op02WithProcessedDataAndRefs> op2list,
                                           Map<Integer, Integer> lutByIdx,
                                           SortedMap<Integer, Integer> lutByOffset,
                                           BytecodeLocFactory locFactory) {
        ByteData rawData = context.originalCodeAttribute.getRawData();
        int codeLength = context.originalCodeAttribute.getCodeLength();
        do {
            Op01WithProcessedDataAndByteJumps op01 = getSingleInstr(rawData, offset);
            int[] targets = op01.getRawTargetOffsets();
            boolean noTargets = false;
            if (targets != null) {
                if (targets.length == 0) {
                    noTargets = true;
                } else {
                    throw new ConfusedCFRException("Can't currently recover from branching unverifiable instructions.");
                }
            }
            int targetIdx = op1list.size();
            op1list.add(op01);
            lutByIdx.put(targetIdx, offset);
            lutByOffset.put(offset, targetIdx);
            op2list.add(op01.createOp2(context.constantPool, targetIdx, locFactory, context.method));
            if (noTargets) {
                return;
            }
            int nextOffset = offset + op01.getInstructionLength();
            if (lutByOffset.containsKey(nextOffset)) {
                targetIdx = op1list.size();
                int fakeOffset = -op1list.size();
                lutByIdx.put(targetIdx, fakeOffset);
                lutByOffset.put(fakeOffset, targetIdx);
                int[] rawTargets = new int[]{nextOffset - fakeOffset};
                Op01WithProcessedDataAndByteJumps fakeGoto = new Op01WithProcessedDataAndByteJumps(JVMInstr.GOTO, null, rawTargets, fakeOffset);
                op1list.add(fakeGoto);
                op2list.add(fakeGoto.createOp2(context.constantPool, targetIdx, locFactory, context.method));
                return;
            }
            offset = nextOffset;
        } while (offset < codeLength);
    }

    private Op01WithProcessedDataAndByteJumps getSingleInstr(ByteData rawCode, int offset) {
        OffsettingByteData bdCode = rawCode.getOffsettingOffsetData(offset);
        JVMInstr instr = JVMInstr.find(bdCode.getS1At(0));
        return instr.createOperation(bdCode, context.constantPool, offset);
    }
}
