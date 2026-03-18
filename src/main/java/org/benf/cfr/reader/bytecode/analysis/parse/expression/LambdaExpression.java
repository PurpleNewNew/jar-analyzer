package org.benf.cfr.reader.bytecode.analysis.parse.expression;

import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.misc.Precedence;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.CloneHelper;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.EquivalenceConstraint;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueUsageCollector;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.scope.LValueScopeDiscoverer;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.expression.StructuredStatementExpression;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredExpressionStatement;
import org.benf.cfr.reader.bytecode.analysis.types.GenericTypeBinder;
import org.benf.cfr.reader.bytecode.analysis.types.JavaGenericBaseInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaRefTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaWildcardTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.util.StringUtils;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.output.Dumper;

import java.util.List;

public class LambdaExpression extends AbstractExpression implements LambdaExpressionCommon {

    private List<LValue> args;
    private List<JavaTypeInstance> explicitArgTypes;
    private Expression result;

    public LambdaExpression(BytecodeLoc loc, InferredJavaType castJavaType, List<LValue> args, List<JavaTypeInstance> explicitArgType, Expression result) {
        super(loc, castJavaType);
        this.args = args;
        this.explicitArgTypes = explicitArgType;
        this.result = result;
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return BytecodeLoc.combine(this, result);
    }

    @Override
    public Expression deepClone(CloneHelper cloneHelper) {
        return new LambdaExpression(getLoc(), getInferredJavaType(), cloneHelper.replaceOrClone(args), explicitArgTypes(), cloneHelper.replaceOrClone(result));
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        collector.collectFrom(args);
        collector.collect(explicitArgTypes);
        result.collectTypeUsages(collector);
    }

    public void setExplicitArgTypes(List<JavaTypeInstance> types) {
        if (types == null || types.size() == args.size()) {
            explicitArgTypes = types;
        }
    }

    public List<JavaTypeInstance> explicitArgTypes() {
        return explicitArgTypes;
    }

    @Override
    public Expression replaceSingleUsageLValues(LValueRewriter lValueRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Expression applyExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        for (int x = 0; x < args.size(); ++x) {
            args.set(x, expressionRewriter.rewriteExpression(args.get(x), ssaIdentifiers, statementContainer, flags));
        }
        result = expressionRewriter.rewriteExpression(result, ssaIdentifiers, statementContainer, flags);
        return this;
    }

    @Override
    public Expression applyReverseExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        result = expressionRewriter.rewriteExpression(result, ssaIdentifiers, statementContainer, flags);
        for (int x = args.size()-1; x >= 0; --x) {
            args.set(x, expressionRewriter.rewriteExpression(args.get(x), ssaIdentifiers, statementContainer, flags));
        }
        return this;
    }

    @Override
    public Precedence getPrecedence() {
        return Precedence.PAREN_SUB_MEMBER;
    }

    @Override
    public Dumper dumpInner(Dumper d) {
        if (explicitArgTypes == null) {
            improveResultType(null);
        }
        boolean multi = args.size() != 1;
        boolean first = true;
        if (explicitArgTypes != null && explicitArgTypes.size() == args.size()) {
            d.separator("(");
            for (int i=0;i<args.size();++i) {
                LValue lValue = args.get(i);
                JavaTypeInstance explicitType = explicitArgTypes.get(i);
                first = StringUtils.comma(first, d);
                if (explicitType != null) {
                    d.dump(explicitType).print(" ");
                }
                lValue.dump(d, true); // set as definition
            }
            d.separator(")");
        } else {
            if (multi) d.separator("(");
            for (LValue lValue : args) {
                first = StringUtils.comma(first, d);
                lValue.dump(d, true); // set as definition
            }
            if (multi) d.separator(")");
        }
        d.print(" -> ");
        d.informBytecodeLoc(result);
        d.dump(result);
        d.removePendingCarriageReturn();
        return d;
    }

    public void improveResultType(JavaTypeInstance expectedFunctionalType) {
        List<JavaTypeInstance> expectedArgTypes = getExpectedArgTypes(expectedFunctionalType);
        if (expectedArgTypes != null) {
            setExplicitArgTypes(expectedArgTypes);
        }
        ConstructorInvokationSimple constructor = getResultConstructor();
        if (constructor == null) {
            return;
        }
        JavaTypeInstance expectedReturnType = getExpectedReturnType(expectedFunctionalType);
        if (expectedReturnType != null) {
            constructor.improveConstructionType(expectedReturnType);
        }
    }

    private List<JavaTypeInstance> getExpectedArgTypes(JavaTypeInstance expectedFunctionalType) {
        Method sam = getSam(expectedFunctionalType);
        if (sam == null) {
            return null;
        }
        List<JavaTypeInstance> argTypes = ListFactory.newList(sam.getMethodPrototype().getArgs());
        JavaTypeInstance lambdaType = expectedFunctionalType == null ? getInferredJavaType().getJavaTypeInstance() : expectedFunctionalType;
        JavaTypeInstance rawLambdaType = lambdaType == null ? null : lambdaType.getDeGenerifiedType();
        if (rawLambdaType instanceof JavaRefTypeInstance) {
            ClassFile classFile = ((JavaRefTypeInstance) rawLambdaType).getClassFile();
            if (classFile != null) {
                JavaTypeInstance classGeneralType =
                        classFile.getClassSignature().getThisGeneralTypeClass(classFile.getClassType(), classFile.getConstantPool());
                if (classGeneralType instanceof JavaGenericBaseInstance) {
                    GenericTypeBinder binder = GenericTypeBinder
                            .extractBindings((JavaGenericBaseInstance) classGeneralType, lambdaType);
                    for (int x = 0; x < argTypes.size(); ++x) {
                        argTypes.set(x, binder.getBindingFor(argTypes.get(x)));
                    }
                }
            }
        }
        for (JavaTypeInstance argType : argTypes) {
            if (argType == null
                    || argType instanceof JavaWildcardTypeInstance
                    || !ExpressionTypeHintHelper.isSpecific(argType)) {
                return null;
            }
        }
        return argTypes;
    }

    private JavaTypeInstance getExpectedReturnType(JavaTypeInstance expectedFunctionalType) {
        JavaTypeInstance lambdaType = expectedFunctionalType == null ? getInferredJavaType().getJavaTypeInstance() : expectedFunctionalType;
        Method sam = getSam(expectedFunctionalType);
        if (sam == null || lambdaType == null) {
            return null;
        }
        JavaTypeInstance rawLambdaType = lambdaType.getDeGenerifiedType();
        if (!(rawLambdaType instanceof JavaRefTypeInstance)) {
            return null;
        }
        ClassFile classFile = ((JavaRefTypeInstance) rawLambdaType).getClassFile();
        if (classFile == null) {
            return null;
        }
        JavaTypeInstance returnType = sam.getMethodPrototype().getReturnType();
        JavaTypeInstance classGeneralType =
                classFile.getClassSignature().getThisGeneralTypeClass(classFile.getClassType(), classFile.getConstantPool());
        if (classGeneralType instanceof JavaGenericBaseInstance) {
            returnType = GenericTypeBinder
                    .extractBindings((JavaGenericBaseInstance) classGeneralType, lambdaType)
                    .getBindingFor(returnType);
        }
        return returnType;
    }

    private Method getSam(JavaTypeInstance expectedFunctionalType) {
        JavaTypeInstance lambdaType = expectedFunctionalType == null ? getInferredJavaType().getJavaTypeInstance() : expectedFunctionalType;
        if (lambdaType == null) {
            return null;
        }
        JavaTypeInstance rawLambdaType = lambdaType.getDeGenerifiedType();
        if (!(rawLambdaType instanceof JavaRefTypeInstance)) {
            return null;
        }
        ClassFile classFile = ((JavaRefTypeInstance) rawLambdaType).getClassFile();
        if (classFile == null) {
            return null;
        }
        Method sam = null;
        for (Method method : classFile.getMethods()) {
            if (method.hasCodeAttribute()) {
                continue;
            }
            if (sam != null) {
                return null;
            }
            sam = method;
        }
        return sam;
    }

    private ConstructorInvokationSimple getResultConstructor() {
        if (result instanceof ConstructorInvokationSimple) {
            return (ConstructorInvokationSimple) result;
        }
        if (!(result instanceof StructuredStatementExpression)) {
            return null;
        }
        StructuredStatement content = ((StructuredStatementExpression) result).getContent();
        if (!(content instanceof StructuredExpressionStatement)) {
            return null;
        }
        Expression expression = ((StructuredExpressionStatement) content).getExpression();
        if (expression instanceof ConstructorInvokationSimple) {
            return (ConstructorInvokationSimple) expression;
        }
        return null;
    }

    private boolean shouldTraceLambdaTyping() {
        return args.size() == 1 && "ignore".equals(String.valueOf(args.get(0)));
    }

    /*
     * The RHS of a lambda expression has already been fully processed and and does not require further gathering
     * EXCEPT.... we need to know about local class sentinels.
     *
     * So adapt the collector.
     */
    @Override
    public void collectUsedLValues(LValueUsageCollector lValueUsageCollector) {
        // Fugly.  TODO: Fix interface.
        if (lValueUsageCollector instanceof LValueScopeDiscoverer) {
            if (((LValueScopeDiscoverer) lValueUsageCollector).descendLambdas()) {
                LValueScopeDiscoverer discover = (LValueScopeDiscoverer) lValueUsageCollector;
                result.collectUsedLValues(discover);
            }
        }
    }

    public List<LValue> getArgs() {
        return args;
    }

    public Expression getResult() {
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LambdaExpression that = (LambdaExpression) o;

        if (args != null ? !args.equals(that.args) : that.args != null) return false;
        if (result != null ? !result.equals(that.result) : that.result != null) return false;

        return true;
    }

    @Override
    public final boolean equivalentUnder(Object o, EquivalenceConstraint constraint) {
        if (o == null) return false;
        if (o == this) return true;
        if (getClass() != o.getClass()) return false;
        LambdaExpression other = (LambdaExpression) o;
        if (!constraint.equivalent(args, other.args)) return false;
        if (!constraint.equivalent(result, other.result)) return false;
        return true;
    }

}
