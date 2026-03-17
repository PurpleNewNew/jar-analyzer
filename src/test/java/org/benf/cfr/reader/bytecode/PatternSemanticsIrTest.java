package org.benf.cfr.reader.bytecode;

import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.InstanceOfExpressionDefining;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.LValueExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.Literal;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.PatternCaseLabel;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.LocalVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.pattern.BindingPattern;
import org.benf.cfr.reader.bytecode.analysis.parse.pattern.GuardedPattern;
import org.benf.cfr.reader.bytecode.analysis.parse.pattern.RecordPattern;
import org.benf.cfr.reader.bytecode.analysis.types.TypeConstants;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.bytecode.analysis.types.RawJavaType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PatternSemanticsIrTest {
    @Test
    void shouldShareBindingPatternAcrossInstanceOfAndCaseLabels() {
        LocalVariable binding = new LocalVariable("s", new InferredJavaType(TypeConstants.STRING, InferredJavaType.Source.EXPRESSION));
        BindingPattern bindingPattern = new BindingPattern(TypeConstants.STRING, binding);

        InstanceOfExpressionDefining instanceOf = new InstanceOfExpressionDefining(
                BytecodeLoc.NONE,
                new InferredJavaType(RawJavaType.BOOLEAN, InferredJavaType.Source.EXPRESSION),
                new LValueExpression(binding),
                bindingPattern
        );
        PatternCaseLabel guardedCase = new PatternCaseLabel(
                BytecodeLoc.NONE,
                new GuardedPattern(bindingPattern, Literal.TRUE)
        );

        assertSame(bindingPattern, instanceOf.getPattern());
        assertSame(bindingPattern, ((GuardedPattern) guardedCase.getPattern()).getInner());
        assertEquals(List.of(binding), guardedCase.getCreatedLValues());
        assertTrue(guardedCase.matches(binding));
    }

    @Test
    void shouldExposeRecordPatternsThroughUnifiedCaseLabel() {
        LocalVariable x = new LocalVariable("x", new InferredJavaType(RawJavaType.INT, InferredJavaType.Source.EXPRESSION));
        LocalVariable y = new LocalVariable("y", new InferredJavaType(RawJavaType.INT, InferredJavaType.Source.EXPRESSION));
        RecordPattern recordPattern = new RecordPattern(
                TypeConstants.OBJECT,
                List.of(x, y)
        );
        PatternCaseLabel caseLabel = new PatternCaseLabel(BytecodeLoc.NONE, recordPattern);

        assertEquals(List.of(x, y), caseLabel.getCreatedLValues());
        assertTrue(caseLabel.matches(x));
        assertTrue(caseLabel.matches(y));
        assertEquals(TypeConstants.OBJECT, recordPattern.getRecordType());
    }
}
