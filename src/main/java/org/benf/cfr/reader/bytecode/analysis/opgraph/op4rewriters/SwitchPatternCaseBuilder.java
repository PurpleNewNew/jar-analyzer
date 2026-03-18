package org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters;

import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.pattern.PatternFactory;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.Block;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredCase;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.Literal;

import java.util.LinkedList;

final class SwitchPatternCaseBuilder {
    SwitchPatternCasePlanner.CaseRewritePlan build(SwitchPatternCasePlanSpec spec) {
        StructuredCase structuredCase = spec.getStructuredCase();
        Block bodyBlock = spec.getBodyBlock();
        LinkedList<org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement> rewrittenStatements = spec.getRewrittenStatements();
        Expression rewrittenValue;
        switch (spec.getKind()) {
            case UNCHANGED:
                rewrittenValue = null;
                bodyBlock = null;
                rewrittenStatements = null;
                break;
            case NULL_LITERAL:
                rewrittenValue = Literal.NULL;
                bodyBlock = null;
                rewrittenStatements = null;
                break;
            case BINDING:
                rewrittenValue = PatternFactory.caseLabel(PatternFactory.binding(spec.getBindingType(), spec.getBinding()));
                break;
            case GUARDED_BINDING:
                rewrittenValue = PatternFactory.caseLabel(
                        PatternFactory.guarded(
                                PatternFactory.binding(spec.getBindingType(), spec.getBinding()),
                                spec.getGuard()
                        )
                );
                break;
            case RECORD:
                rewrittenValue = PatternFactory.caseLabel(
                        PatternFactory.record(spec.getBindingType(), spec.getRecordComponents())
                );
                break;
            default:
                throw new IllegalStateException("Unknown switch pattern case plan kind: " + spec.getKind());
        }
        return new SwitchPatternCasePlanner.CaseRewritePlan(
                structuredCase,
                bodyBlock,
                rewrittenValue,
                rewrittenStatements
        );
    }
}
