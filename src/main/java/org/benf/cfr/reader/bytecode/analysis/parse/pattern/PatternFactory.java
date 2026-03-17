package org.benf.cfr.reader.bytecode.analysis.parse.pattern;

import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.InstanceOfExpressionDefining;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.PatternCaseLabel;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.RawJavaType;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;

import java.util.List;

public final class PatternFactory {
    private static final InferredJavaType BOOLEAN_TYPE =
            new InferredJavaType(RawJavaType.BOOLEAN, InferredJavaType.Source.EXPRESSION);

    private PatternFactory() {
    }

    public static BindingPattern binding(JavaTypeInstance typeInstance, LValue binding) {
        return new BindingPattern(typeInstance, binding);
    }

    public static RecordPattern record(JavaTypeInstance recordType, List<LValue> components) {
        return new RecordPattern(recordType, components);
    }

    public static GuardedPattern guarded(MatchPattern inner, Expression guard) {
        return new GuardedPattern(inner, guard);
    }

    public static PatternCaseLabel caseLabel(MatchPattern pattern) {
        return new PatternCaseLabel(BytecodeLoc.NONE, pattern);
    }

    public static InstanceOfExpressionDefining defining(Expression lhs, BindingPattern pattern) {
        return new InstanceOfExpressionDefining(BytecodeLoc.TODO, BOOLEAN_TYPE, lhs, pattern);
    }

    public static InstanceOfExpressionDefining defining(Expression lhs, JavaTypeInstance typeInstance, LValue binding) {
        return defining(lhs, binding(typeInstance, binding));
    }
}
