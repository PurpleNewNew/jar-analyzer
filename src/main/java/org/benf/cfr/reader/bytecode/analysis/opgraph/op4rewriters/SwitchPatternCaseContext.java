package org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters;

import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.Block;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredCase;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredWhile;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;

import java.util.List;

final class SwitchPatternCaseContext {
    private final StructuredCase structuredCase;
    private final Block bodyBlock;
    private final List<Op04StructuredStatement> statements;
    private final Expression selector;
    private final JavaTypeInstance expectedType;
    private final LValue indexLValue;
    private final StructuredWhile patternLoop;

    SwitchPatternCaseContext(StructuredCase structuredCase,
                             Block bodyBlock,
                             List<Op04StructuredStatement> statements,
                             Expression selector,
                             JavaTypeInstance expectedType,
                             LValue indexLValue,
                             StructuredWhile patternLoop) {
        this.structuredCase = structuredCase;
        this.bodyBlock = bodyBlock;
        this.statements = statements;
        this.selector = selector;
        this.expectedType = expectedType;
        this.indexLValue = indexLValue;
        this.patternLoop = patternLoop;
    }

    StructuredCase getStructuredCase() {
        return structuredCase;
    }

    Block getBodyBlock() {
        return bodyBlock;
    }

    List<Op04StructuredStatement> getStatements() {
        return statements;
    }

    Expression getSelector() {
        return selector;
    }

    JavaTypeInstance getExpectedType() {
        return expectedType;
    }

    LValue getIndexLValue() {
        return indexLValue;
    }

    StructuredWhile getPatternLoop() {
        return patternLoop;
    }
}
