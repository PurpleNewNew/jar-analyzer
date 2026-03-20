package org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters;

import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.AbstractMatchResultIterator;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchIterator;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchOneOf;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchSequence;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.Matcher;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.ResetAfterTest;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.util.MiscStatementTools;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ArithOp;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ArithmeticMutationOperation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ArithmeticOperation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ArithmeticPostMutationOperation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ArithmeticPreMutationOperation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.AssignmentExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.CastExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.LValueExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.Literal;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.MemberFunctionInvokation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.StackValue;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.StaticFunctionInvokation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.SuperFunctionInvokation;
import org.benf.cfr.reader.bytecode.analysis.parse.literal.TypedLiteral;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.LocalVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.StaticVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.AbstractExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.CloneHelper;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.bytecode.analysis.parse.wildcard.WildcardMatch;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredAssignment;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredExpressionStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredReturn;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.placeholder.BeginBlock;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.placeholder.EndBlock;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.MethodPrototype;
import org.benf.cfr.reader.entities.AccessFlagMethod;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.state.DCCommonState;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.collections.SetFactory;
import org.benf.cfr.reader.util.collections.SetUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SyntheticAccessorRewriter extends AbstractExpressionRewriter implements Op04Rewriter {

    private final DCCommonState state;
    private final JavaTypeInstance thisClassType;
    private final ExpressionRewriter visibilityRewriter = new VisibilityDecreasingRewriter();

    public SyntheticAccessorRewriter(DCCommonState state, JavaTypeInstance thisClassType) {
        this.state = state;
        this.thisClassType = thisClassType;
    }

    @Override
    public void rewrite(Op04StructuredStatement root) {
        List<StructuredStatement> structuredStatements = MiscStatementTools.linearise(root);
        if (structuredStatements == null) return;

        for (StructuredStatement statement : structuredStatements) {
            statement.rewriteExpressions(this);
        }
    }

    /*
     * Expression rewriter boilerplate - note that we can't expect ssaIdentifiers to be non-null.
     */
    @Override
    public Expression rewriteExpression(Expression expression, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        // TODO : In practice, the rewrites are ALWAYS done in terms of static functions.
        // TODO : should we assume this?  Seems like a good thing an obfuscator could use.
        expression = expression.applyExpressionRewriter(this, ssaIdentifiers, statementContainer, flags);
        if (expression instanceof StaticFunctionInvokation) {
            /*
             * REWRITE INSIDE OUT! First, rewrite args, THEN rewrite expression.
             */
            Expression rewritten = rewriteSyntheticAccessorInvocation((StaticFunctionInvokation) expression);
            if (rewritten != null) {
                expression = rewritten;
            }
        }
        return expression;
    }

    @Override
    public LValue rewriteExpression(LValue lValue, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        // Ecj will bury synthetic accessors in lvalues..... (see AnonymousInnerClassTest11c2).
        return lValue.applyExpressionRewriter(this, ssaIdentifiers, statementContainer, flags);
    }

    private enum AccessorMatchKind {
        RETURN_LVALUE("returnlvalue"),
        MUTATION1("mutation1"),
        MUTATION2("mutation2"),
        MUTATION3("mutation3"),
        ASSIGNMENT1("assignment1"),
        PRE_INC("preinc"),
        POST_INC("postinc"),
        PRE_DEC("predec"),
        POST_DEC("postdec"),
        SUPER_INVOKE("superinv"),
        SUPER_RETINVOKE("superretinv");

        private final String matcherName;

        AccessorMatchKind(String matcherName) {
            this.matcherName = matcherName;
        }

        private static AccessorMatchKind fromMatcherName(String matcherName) {
            for (AccessorMatchKind kind : values()) {
                if (kind.matcherName.equals(matcherName)) {
                    return kind;
                }
            }
            return null;
        }
    }

    private enum FunctionCallMatchKind {
        STATIC_STATEMENT("ssub1", true),
        STATIC_RETURN("sfun1", true),
        MEMBER_STATEMENT("msub1", false),
        MEMBER_RETURN("mfun1", false);

        private final String matcherName;
        private final boolean staticInvocation;

        FunctionCallMatchKind(String matcherName, boolean staticInvocation) {
            this.matcherName = matcherName;
            this.staticInvocation = staticInvocation;
        }

        private static FunctionCallMatchKind fromMatcherName(String matcherName) {
            for (FunctionCallMatchKind kind : values()) {
                if (kind.matcherName.equals(matcherName)) {
                    return kind;
                }
            }
            return null;
        }
    }

    private static boolean sharesInnerClassHierarchy(JavaTypeInstance type1, JavaTypeInstance type2) {
        Set<JavaTypeInstance> parents1 = SetFactory.newSet();
        type1.getInnerClassHereInfo().collectTransitiveDegenericParents(parents1);
        parents1.add(type1);
        Set<JavaTypeInstance> parents2 = SetFactory.newSet();
        type2.getInnerClassHereInfo().collectTransitiveDegenericParents(parents2);
        parents2.add(type2);
        return SetUtil.hasIntersection(parents1, parents2);
    }

    private Expression rewriteSyntheticAccessorInvocation(final StaticFunctionInvokation functionInvokation) {
        JavaTypeInstance tgtType = functionInvokation.getClazz();
        // Does tgtType have an inner relationship with this?
        if (!sharesInnerClassHierarchy(thisClassType, tgtType)) return null;

        ClassFile otherClass = state.getClassFile(tgtType);
        JavaTypeInstance otherType = otherClass.getClassType();
        MethodPrototype otherPrototype = functionInvokation.getFunction().getMethodPrototype();
        List<Expression> appliedArgs = functionInvokation.getArgs();

        /*
         * Look at the code for the referenced method.
         *
         * It will either be
         *
         *  * a static getter (0 args)
         *  * a static mutator (1 arg)
         *  * an instance getter (1 arg)
         *  * an instance mutator (2 args)
         *
         * Either way, the accessor method SHOULD be a static synthetic.
         */
        Method otherMethod;
        try {
            otherMethod = otherClass.getMethodByPrototype(otherPrototype);
        } catch (NoSuchMethodException e) {
            // Ignore and return.
            return null;
        }
        if (!otherMethod.testAccessFlag(AccessFlagMethod.ACC_STATIC)) return null;
        if (!otherMethod.testAccessFlag(AccessFlagMethod.ACC_SYNTHETIC)) return null;
//                getParameters(otherMethod.getConstructorFlag());

        /* Get the linearized, comment stripped code for the block. */
        if (!otherMethod.hasCodeAttribute()) return null;
        Op04StructuredStatement otherCode = otherMethod.getAnalysis();
        if (otherCode == null) return null;
        List<LocalVariable> methodArgs = otherMethod.getMethodPrototype().getComputedParameters();

        List<StructuredStatement> structuredStatements = MiscStatementTools.linearise(otherCode);

        if (structuredStatements == null) {
            return null;
        }
        Expression res = tryRewriteAccessor(structuredStatements, otherType, appliedArgs, methodArgs);
        if (res == null) {
            res = tryRewriteFunctionCall(structuredStatements, otherType, appliedArgs, methodArgs);
        }
        if (res != null) {
            otherMethod.hideSynthetic();
            return visibilityRewriter.rewriteExpression(res, null, null, null);
        }

        return null;
    }

    private Expression tryRewriteAccessor(List<StructuredStatement> structuredStatements, JavaTypeInstance otherType,
                                          List<Expression> appliedArgs, List<LocalVariable> methodArgs) {
        WildcardMatch wcm = new WildcardMatch();

        List<Expression> methodExprs = new ArrayList<Expression>();
        for (int x=1;x<methodArgs.size();++x) {
            methodExprs.add(new LValueExpression(methodArgs.get(x)));
        }
        AccessorMatchCollector accessorMatchCollector = new AccessorMatchCollector();
        Matcher<StructuredStatement> matcher = new MatchSequence(
                new BeginBlock(null),
                buildAccessorCases(wcm, methodExprs),
                new EndBlock(null)
        );
        if (!matchesSingleBlock(structuredStatements, matcher, accessorMatchCollector)) return null;
        if (accessorMatchCollector.matchKind == null) return null;

        boolean isStatic = (accessorMatchCollector.lValue instanceof StaticVariable);
        if (isStatic) {
            // let's be paranoid, and make sure that it's a static on the accessor class.
            StaticVariable staticVariable = (StaticVariable) accessorMatchCollector.lValue;
            if (!otherType.equals(staticVariable.getOwningClassType())) return null;
        }

        /*
         * At this point, we should validate that ONLY the passed in arguments are used in rValue.
         * But I'm a coward. ;)
         */
        CloneHelper cloneHelper = buildCloneHelper(otherType, methodArgs, appliedArgs);
        return rewriteAccessorMatch(accessorMatchCollector, cloneHelper, otherType);
    }

    private class AccessorMatchCollector extends AbstractMatchResultIterator {

        AccessorMatchKind matchKind;
        LValue lValue;
        Expression rValue;
        ArithOp op;

        @Override
        public void collectMatches(String name, WildcardMatch wcm) {
            this.matchKind = AccessorMatchKind.fromMatcherName(name);
            this.lValue = wcm.getLValueWildCard("lvalue").getMatch();
            this.rValue = readMatchedAccessorValue(matchKind, wcm);
            this.op = readMatchedAccessorOp(matchKind, wcm);
        }

        private Expression readMatchedAccessorValue(AccessorMatchKind matchKind, WildcardMatch wcm) {
            switch (matchKind) {
                case MUTATION1:
                case MUTATION2:
                case ASSIGNMENT1:
                case MUTATION3:
                    return wcm.getExpressionWildCard("rvalue").getMatch();
                case SUPER_INVOKE:
                case SUPER_RETINVOKE:
                    return wcm.getSuperFunction("super").getMatch();
                default:
                    return null;
            }
        }

        private ArithOp readMatchedAccessorOp(AccessorMatchKind matchKind, WildcardMatch wcm) {
            if (matchKind != AccessorMatchKind.MUTATION3) {
                return null;
            }
            return wcm.getArithmeticMutationWildcard("mutation").getOp().getMatch();
        }
    }

    private Expression tryRewriteFunctionCall(List<StructuredStatement> structuredStatements, JavaTypeInstance otherType,
                                              List<Expression> appliedArgs, List<LocalVariable> methodArgs) {
        WildcardMatch wcm = new WildcardMatch();

        FuncMatchCollector funcMatchCollector = new FuncMatchCollector();
        Matcher<StructuredStatement> matcher = new MatchSequence(
                new BeginBlock(null),
                buildFunctionCallCases(wcm, otherType),
                new EndBlock(null)
        );
        if (!matchesSingleBlock(structuredStatements, matcher, funcMatchCollector)) return null;
        if (funcMatchCollector.matchKind == null) return null;

        CloneHelper cloneHelper = buildCloneHelper(otherType, methodArgs, appliedArgs);
        return cloneHelper.replaceOrClone(funcMatchCollector.functionInvokation);
    }

    private MatchOneOf buildFunctionCallCases(WildcardMatch wcm, JavaTypeInstance otherType) {
        return new MatchOneOf(
                memberFunctionCase(wcm, FunctionCallMatchKind.MEMBER_STATEMENT, false),
                staticFunctionCase(wcm, FunctionCallMatchKind.STATIC_STATEMENT, otherType, false),
                memberFunctionCase(wcm, FunctionCallMatchKind.MEMBER_RETURN, true),
                staticFunctionCase(wcm, FunctionCallMatchKind.STATIC_RETURN, otherType, true)
        );
    }

    private MatchOneOf buildAccessorCases(WildcardMatch wcm, List<Expression> methodExprs) {
        return new MatchOneOf(
                returnLValueCase(wcm),
                assignmentAccessorCase(wcm, AccessorMatchKind.MUTATION1, true, false),
                assignmentAccessorCase(wcm, AccessorMatchKind.ASSIGNMENT1, false, false),
                assignmentAccessorCase(wcm, AccessorMatchKind.MUTATION2, false, true),
                arithmeticMutationCase(wcm),
                unaryMutationReturnCase(wcm, AccessorMatchKind.PRE_INC, ArithOp.PLUS, true),
                unaryMutationReturnCase(wcm, AccessorMatchKind.PRE_DEC, ArithOp.MINUS, true),
                unaryMutationReturnCase(wcm, AccessorMatchKind.POST_INC, ArithOp.PLUS, false),
                unaryMutationReturnCase(wcm, AccessorMatchKind.POST_DEC, ArithOp.MINUS, false),
                unaryMutationStatementCase(wcm, AccessorMatchKind.POST_INC, ArithOp.PLUS),
                stackPostIncrementCase(wcm),
                unaryMutationStatementCase(wcm, AccessorMatchKind.POST_DEC, ArithOp.MINUS),
                superAccessorCase(wcm, AccessorMatchKind.SUPER_INVOKE, methodExprs, false),
                superAccessorCase(wcm, AccessorMatchKind.SUPER_RETINVOKE, methodExprs, true)
        );
    }

    private Matcher<StructuredStatement> returnLValueCase(WildcardMatch wcm) {
        return new ResetAfterTest(wcm, AccessorMatchKind.RETURN_LVALUE.matcherName,
                new StructuredReturn(BytecodeLoc.NONE, new LValueExpression(wcm.getLValueWildCard("lvalue")), null)
        );
    }

    private Matcher<StructuredStatement> assignmentAccessorCase(WildcardMatch wcm,
                                                                AccessorMatchKind matchKind,
                                                                boolean returnLValue,
                                                                boolean returnRValue) {
        List<Matcher<StructuredStatement>> sequence = new ArrayList<Matcher<StructuredStatement>>();
        sequence.add(new StructuredAssignment(BytecodeLoc.NONE, wcm.getLValueWildCard("lvalue"), wcm.getExpressionWildCard("rvalue")));
        if (returnLValue) {
            sequence.add(new StructuredReturn(BytecodeLoc.NONE, new LValueExpression(wcm.getLValueWildCard("lvalue")), null));
        } else if (returnRValue) {
            sequence.add(new StructuredReturn(BytecodeLoc.NONE, wcm.getExpressionWildCard("rvalue"), null));
        }
        return new ResetAfterTest(wcm, matchKind.matcherName, new MatchSequence(sequence.toArray(new Matcher[0])));
    }

    private Matcher<StructuredStatement> arithmeticMutationCase(WildcardMatch wcm) {
        return new ResetAfterTest(wcm, AccessorMatchKind.MUTATION3.matcherName,
                new StructuredReturn(BytecodeLoc.NONE, wcm.getArithmeticMutationWildcard("mutation", wcm.getLValueWildCard("lvalue"), wcm.getExpressionWildCard("rvalue")), null)
        );
    }

    private Matcher<StructuredStatement> unaryMutationReturnCase(WildcardMatch wcm,
                                                                 AccessorMatchKind matchKind,
                                                                 ArithOp op,
                                                                 boolean pre) {
        Expression expression = pre
                ? new ArithmeticPreMutationOperation(BytecodeLoc.NONE, wcm.getLValueWildCard("lvalue"), op)
                : new ArithmeticPostMutationOperation(BytecodeLoc.NONE, wcm.getLValueWildCard("lvalue"), op);
        return new ResetAfterTest(wcm, matchKind.matcherName,
                new StructuredReturn(BytecodeLoc.NONE, expression, null)
        );
    }

    private Matcher<StructuredStatement> unaryMutationStatementCase(WildcardMatch wcm,
                                                                    AccessorMatchKind matchKind,
                                                                    ArithOp op) {
        return new ResetAfterTest(wcm, matchKind.matcherName,
                new MatchSequence(
                        new StructuredExpressionStatement(BytecodeLoc.NONE, new ArithmeticPostMutationOperation(BytecodeLoc.NONE, wcm.getLValueWildCard("lvalue"), op), false),
                        new StructuredReturn(BytecodeLoc.NONE, new LValueExpression(wcm.getLValueWildCard("lvalue")), null)
                )
        );
    }

    private Matcher<StructuredStatement> stackPostIncrementCase(WildcardMatch wcm) {
        return new ResetAfterTest(wcm, AccessorMatchKind.POST_INC.matcherName,
                new MatchSequence(
                        new StructuredAssignment(BytecodeLoc.NONE, wcm.getStackLabelWildcard("tmp"), new LValueExpression(wcm.getLValueWildCard("lvalue"))),
                        new StructuredAssignment(BytecodeLoc.NONE,
                                wcm.getLValueWildCard("lvalue"),
                                new ArithmeticOperation(BytecodeLoc.NONE, new StackValue(BytecodeLoc.NONE, wcm.getStackLabelWildcard("tmp")), new Literal(TypedLiteral.getInt(1)), ArithOp.PLUS)
                        ),
                        new StructuredReturn(BytecodeLoc.NONE, new StackValue(BytecodeLoc.NONE, wcm.getStackLabelWildcard("tmp")), null)
                )
        );
    }

    private Matcher<StructuredStatement> superAccessorCase(WildcardMatch wcm,
                                                           AccessorMatchKind matchKind,
                                                           List<Expression> methodExprs,
                                                           boolean returnsValue) {
        Expression superCall = wcm.getSuperFunction("super", methodExprs);
        return new ResetAfterTest(
                wcm,
                matchKind.matcherName,
                returnsValue
                        ? new StructuredReturn(BytecodeLoc.NONE, superCall, null)
                        : new StructuredExpressionStatement(BytecodeLoc.NONE, superCall, false)
        );
    }

    private Matcher<StructuredStatement> memberFunctionCase(WildcardMatch wcm,
                                                            FunctionCallMatchKind matchKind,
                                                            boolean returnsValue) {
        Expression function = wcm.getMemberFunction("func", null, false, new LValueExpression(wcm.getLValueWildCard("lvalue")), null);
        return new ResetAfterTest(
                wcm,
                matchKind.matcherName,
                returnsValue
                        ? new StructuredReturn(BytecodeLoc.NONE, function, null)
                        : new StructuredExpressionStatement(BytecodeLoc.NONE, function, false)
        );
    }

    private Matcher<StructuredStatement> staticFunctionCase(WildcardMatch wcm,
                                                            FunctionCallMatchKind matchKind,
                                                            JavaTypeInstance otherType,
                                                            boolean returnsValue) {
        Expression function = wcm.getStaticFunction("func", otherType, null, null, (List<Expression>) null);
        return new ResetAfterTest(
                wcm,
                matchKind.matcherName,
                returnsValue
                        ? new StructuredReturn(BytecodeLoc.NONE, function, null)
                        : new StructuredExpressionStatement(BytecodeLoc.NONE, function, false)
        );
    }

    private boolean matchesSingleBlock(List<StructuredStatement> structuredStatements,
                                       Matcher<StructuredStatement> matcher,
                                       AbstractMatchResultIterator collector) {
        MatchIterator<StructuredStatement> mi = new MatchIterator<StructuredStatement>(structuredStatements);
        mi.advance();
        return matcher.match(mi, collector);
    }

    private CloneHelper buildCloneHelper(JavaTypeInstance otherType,
                                         List<LocalVariable> methodArgs,
                                         List<Expression> appliedArgs) {
        Map<LValue, LValue> lValueReplacements = MapFactory.newMap();
        Map<Expression, Expression> expressionReplacements = MapFactory.newMap();
        for (int x = 0; x < methodArgs.size(); ++x) {
            LocalVariable methodArg = methodArgs.get(x);
            Expression appliedArg = appliedArgs.get(x);
            if (appliedArg instanceof LValueExpression) {
                lValueReplacements.put(methodArg, ((LValueExpression) appliedArg).getLValue());
            }
            if (methodArg.getInferredJavaType().getJavaTypeInstance().equals(otherType)
                    && !appliedArg.getInferredJavaType().getJavaTypeInstance().equals(otherType)) {
                appliedArg = new CastExpression(BytecodeLoc.NONE, methodArg.getInferredJavaType(), appliedArg);
            }
            expressionReplacements.put(new LValueExpression(methodArg), appliedArg);
        }
        return new CloneHelper(expressionReplacements, lValueReplacements);
    }

    private Expression rewriteAccessorMatch(AccessorMatchCollector accessorMatchCollector,
                                            CloneHelper cloneHelper,
                                            JavaTypeInstance otherType) {
        switch (accessorMatchCollector.matchKind) {
            case MUTATION1:
            case MUTATION2:
            case ASSIGNMENT1:
                return cloneHelper.replaceOrClone(
                        new AssignmentExpression(BytecodeLoc.TODO, accessorMatchCollector.lValue, accessorMatchCollector.rValue)
                );
            case MUTATION3:
                return cloneHelper.replaceOrClone(
                        new ArithmeticMutationOperation(
                                BytecodeLoc.TODO,
                                accessorMatchCollector.lValue,
                                accessorMatchCollector.rValue,
                                accessorMatchCollector.op
                        )
                );
            case RETURN_LVALUE:
                return cloneHelper.replaceOrClone(new LValueExpression(accessorMatchCollector.lValue));
            case PRE_DEC:
                return cloneHelper.replaceOrClone(
                        new ArithmeticPreMutationOperation(BytecodeLoc.TODO, accessorMatchCollector.lValue, ArithOp.MINUS)
                );
            case PRE_INC:
                return cloneHelper.replaceOrClone(
                        new ArithmeticPreMutationOperation(BytecodeLoc.TODO, accessorMatchCollector.lValue, ArithOp.PLUS)
                );
            case POST_DEC:
                return cloneHelper.replaceOrClone(
                        new ArithmeticPostMutationOperation(BytecodeLoc.TODO, accessorMatchCollector.lValue, ArithOp.MINUS)
                );
            case POST_INC:
                return cloneHelper.replaceOrClone(
                        new ArithmeticPostMutationOperation(BytecodeLoc.TODO, accessorMatchCollector.lValue, ArithOp.PLUS)
                );
            case SUPER_INVOKE:
            case SUPER_RETINVOKE:
                SuperFunctionInvokation invoke = (SuperFunctionInvokation) accessorMatchCollector.rValue;
                return ((SuperFunctionInvokation) cloneHelper.replaceOrClone(invoke)).withCustomName(otherType);
            default:
                throw new IllegalStateException();
        }
    }

    private class FuncMatchCollector extends AbstractMatchResultIterator {

        FunctionCallMatchKind matchKind;
        Expression functionInvokation;

        @Override
        public void collectMatches(String name, WildcardMatch wcm) {
            this.matchKind = FunctionCallMatchKind.fromMatcherName(name);
            functionInvokation = matchKind != null && matchKind.staticInvocation
                    ? wcm.getStaticFunction("func").getMatch()
                    : wcm.getMemberFunction("func").getMatch();
        }
    }

    private class VisibilityDecreasingRewriter extends AbstractExpressionRewriter {
        @Override
        public LValue rewriteExpression(LValue lValue, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
            if (lValue instanceof StaticVariable) {
                StaticVariable sv = (StaticVariable)lValue;
                JavaTypeInstance owning = sv.getOwningClassType();
                if (!thisClassType.getInnerClassHereInfo().isTransitiveInnerClassOf(owning)) {
                    return sv.getNonSimpleCopy();
                }
            }
            return lValue;
        }
    }
}
