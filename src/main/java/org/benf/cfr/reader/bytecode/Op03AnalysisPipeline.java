package org.benf.cfr.reader.bytecode;

import org.benf.cfr.reader.bytecode.analysis.opgraph.Op01WithProcessedDataAndByteJumps;

import java.util.List;

final class Op03AnalysisPipeline {
    private final BytecodeDecodeStage bytecodeDecodeStage;
    private final ExceptionCfgNormalizationStage exceptionCfgNormalizationStage;
    private final Op03SimplificationStage op03SimplificationStage;
    private final LoopConditionalShapingStage loopConditionalShapingStage;

    Op03AnalysisPipeline(Op03PipelineContext context) {
        this.bytecodeDecodeStage = new BytecodeDecodeStage(context);
        this.exceptionCfgNormalizationStage = new ExceptionCfgNormalizationStage();
        this.op03SimplificationStage = new Op03SimplificationStage();
        this.loopConditionalShapingStage = new LoopConditionalShapingStage();
    }

    Op03PipelineState analyse(List<Op01WithProcessedDataAndByteJumps> instrs, Op03PipelineContext context) {
        Op03PipelineState state = bytecodeDecodeStage.decode(instrs);
        exceptionCfgNormalizationStage.normalize(instrs, state, context);
        op03SimplificationStage.simplify(state, context);
        loopConditionalShapingStage.shape(state, context);
        return state;
    }
}
