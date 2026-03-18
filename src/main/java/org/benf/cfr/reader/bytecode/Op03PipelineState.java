package org.benf.cfr.reader.bytecode;

import org.benf.cfr.reader.bytecode.analysis.opgraph.Op02WithProcessedDataAndRefs;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifierFactory;
import org.benf.cfr.reader.bytecode.analysis.variables.VariableFactory;

import java.util.List;
import java.util.Map;
import java.util.SortedMap;

final class Op03PipelineState {
    final SortedMap<Integer, Integer> lutByOffset;
    final Map<Integer, Integer> lutByIdx;
    final BlockIdentifierFactory blockIdentifierFactory;
    List<Op02WithProcessedDataAndRefs> op2list;
    VariableFactory variableFactory;
    List<Op03SimpleStatement> op03SimpleParseNodes;
    AnonymousClassUsage anonymousClassUsage;

    Op03PipelineState(SortedMap<Integer, Integer> lutByOffset,
                      Map<Integer, Integer> lutByIdx,
                      BlockIdentifierFactory blockIdentifierFactory,
                      List<Op02WithProcessedDataAndRefs> op2list) {
        this.lutByOffset = lutByOffset;
        this.lutByIdx = lutByIdx;
        this.blockIdentifierFactory = blockIdentifierFactory;
        this.op2list = op2list;
    }
}
