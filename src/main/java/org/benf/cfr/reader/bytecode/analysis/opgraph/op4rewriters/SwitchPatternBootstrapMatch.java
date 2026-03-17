package org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters;

import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
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
    private final List<JavaTypeInstance> caseTypes;

    private SwitchPatternBootstrapMatch(Expression selector, LValue indexLValue, List<JavaTypeInstance> caseTypes) {
        this.selector = selector;
        this.indexLValue = indexLValue;
        this.caseTypes = caseTypes;
    }

    Expression getSelector() {
        return selector;
    }

    LValue getIndexLValue() {
        return indexLValue;
    }

    List<JavaTypeInstance> getCaseTypes() {
        return caseTypes;
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
        List<JavaTypeInstance> caseTypes = extractCaseTypes((NewAnonymousArray) args.get(1));
        if (caseTypes == null) return null;
        Expression selector = CastExpression.removeImplicit(args.get(2));
        LValue indexLValue = ((LValueExpression) args.get(3)).getLValue();
        return new SwitchPatternBootstrapMatch(selector, indexLValue, caseTypes);
    }

    private static List<JavaTypeInstance> extractCaseTypes(NewAnonymousArray array) {
        List<JavaTypeInstance> res = ListFactory.newList();
        for (Expression value : array.getValues()) {
            if (!(value instanceof Literal)) return null;
            TypedLiteral literal = ((Literal) value).getValue();
            if (literal.getType() != TypedLiteral.LiteralType.Class) return null;
            res.add(literal.getClassValue());
        }
        return res;
    }
}
