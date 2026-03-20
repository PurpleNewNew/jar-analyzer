package org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters;

import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.CloneHelper;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.CastExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.LValueExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.Literal;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.NewAnonymousArray;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.StaticFunctionInvokation;
import org.benf.cfr.reader.bytecode.analysis.parse.literal.TypedLiteral;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.util.collections.ListFactory;

import java.util.List;

final class SwitchPatternBootstrapMatch {
    private final Expression selector;
    private final LValue indexLValue;
    private final List<SwitchCaseLabel> caseLabels;

    private SwitchPatternBootstrapMatch(Expression selector, LValue indexLValue, List<SwitchCaseLabel> caseLabels) {
        this.selector = selector;
        this.indexLValue = indexLValue;
        this.caseLabels = caseLabels;
    }

    Expression getSelector() {
        return selector;
    }

    LValue getIndexLValue() {
        return indexLValue;
    }

    List<SwitchCaseLabel> getCaseLabels() {
        return caseLabels;
    }

    static SwitchPatternBootstrapMatch match(Expression expression) {
        if (!(expression instanceof StaticFunctionInvokation)) return null;
        StaticFunctionInvokation invocation = (StaticFunctionInvokation) expression;
        if (!"typeSwitch".equals(invocation.getName())) return null;
        if (!"java.lang.runtime.SwitchBootstraps".equals(invocation.getClazz().getRawName())) return null;
        List<Expression> args = invocation.getArgs();
        if (args.size() != 4) return null;
        if (!(args.get(1) instanceof NewAnonymousArray)) return null;
        if (!(args.get(3) instanceof LValueExpression)) return null;
        List<SwitchCaseLabel> caseLabels = extractCaseLabels((NewAnonymousArray) args.get(1));
        if (caseLabels == null) return null;
        Expression selector = CastExpression.removeImplicit(args.get(2));
        LValue indexLValue = ((LValueExpression) args.get(3)).getLValue();
        return new SwitchPatternBootstrapMatch(selector, indexLValue, caseLabels);
    }

    private static List<SwitchCaseLabel> extractCaseLabels(NewAnonymousArray array) {
        List<SwitchCaseLabel> res = ListFactory.newList();
        for (Expression value : array.getValues()) {
            if (!(value instanceof Literal)) return null;
            TypedLiteral literal = ((Literal) value).getValue();
            if (literal.getType() == TypedLiteral.LiteralType.Class) {
                res.add(SwitchCaseLabel.pattern(literal.getClassValue()));
                continue;
            }
            res.add(SwitchCaseLabel.literal(value.deepClone(new CloneHelper())));
        }
        return res;
    }

    static final class SwitchCaseLabel {
        private final Expression caseExpression;
        private final JavaTypeInstance patternType;

        private SwitchCaseLabel(Expression caseExpression, JavaTypeInstance patternType) {
            this.caseExpression = caseExpression;
            this.patternType = patternType;
        }

        static SwitchCaseLabel literal(Expression caseExpression) {
            return new SwitchCaseLabel(caseExpression, null);
        }

        static SwitchCaseLabel pattern(JavaTypeInstance patternType) {
            return new SwitchCaseLabel(null, patternType);
        }

        boolean isPattern() {
            return patternType != null;
        }

        Expression getCaseExpression() {
            return caseExpression;
        }

        JavaTypeInstance getPatternType() {
            return patternType;
        }
    }
}
