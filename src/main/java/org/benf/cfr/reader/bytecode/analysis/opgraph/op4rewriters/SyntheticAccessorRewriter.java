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
    private final ExpressionRewriter visbilityRewriter = new VisibiliyDecreasingRewriter();

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
            expression = rewriteFunctionExpression((StaticFunctionInvokation) expression);
        }
        return expression;
    }

    @Override
    public LValue rewriteExpression(LValue lValue, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        // Ecj will bury synthetic accessors in lvalues..... (see AnonymousInnerClassTest11c2).
        return lValue.applyExpressionRewriter(this, ssaIdentifiers, statementContainer, flags);
    }

    private Expression rewriteFunctionExpression(final StaticFunctionInvokation functionInvokation) {
        Expression res = rewriteFunctionExpression2(functionInvokation);
        // Just a cheat to allow me to return null.
        if (res == null) return functionInvokation;
        return res;
    }

    private static final String RETURN_LVALUE = "returnlvalue";
    private static final String MUTATION1 = "mutation1";
    private static final String MUTATION2 = "mutation2";
    private static final String MUTATION3 = "mutation3";
    private static final String ASSIGNMENT1 = "assignment1";
    private static final String PRE_INC = "preinc";
    private static final String POST_INC = "postinc";
    private static final String PRE_DEC = "predec";
    private static final String POST_DEC = "postdec";
    private static final String SUPER_INVOKE = "superinv";
    private static final String SUPER_RETINVOKE = "superretinv";

    private static boolean validRelationship(JavaTypeInstance type1, JavaTypeInstance type2) {
        Set<JavaTypeInstance> parents1 = SetFactory.newSet();
        type1.getInnerClassHereInfo().collectTransitiveDegenericParents(parents1);
        parents1.add(type1);
        Set<JavaTypeInstance> parents2 = SetFactory.newSet();
        type2.getInnerClassHereInfo().collectTransitiveDegenericParents(parents2);
        parents2.add(type2);
        boolean res = SetUtil.hasIntersection(parents1, parents2);
        return res;
    }

    private Expression rewriteFunctionExpression2(final StaticFunctionInvokation functionInvokation) {
        JavaTypeInstance tgtType = functionInvokation.getClazz();
        // Does tgtType have an inner relationship with this?
        if (!validRelationship(thisClassType, tgtType)) return null;

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
            return visbilityRewriter.rewriteExpression(res, null, null, null);
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
        if (!matchesSingleBlock(structuredStatements, buildAccessorMatcher(wcm, methodExprs), accessorMatchCollector)) return null;
        if (accessorMatchCollector.matchType == null) return null;

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

        String matchType;
        LValue lValue;
        Expression rValue;
        ArithOp op;

        @Override
        public void collectMatches(String name, WildcardMatch wcm) {
            this.matchType = name;
            this.lValue = wcm.getLValueWildCard("lvalue").getMatch();
            this.rValue = readMatchedAccessorValue(matchType, wcm);
            this.op = readMatchedAccessorOp(matchType, wcm);
        }

        private Expression readMatchedAccessorValue(String matchType, WildcardMatch wcm) {
            switch (matchType) {
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

        private ArithOp readMatchedAccessorOp(String matchType, WildcardMatch wcm) {
            if (!MUTATION3.equals(matchType)) {
                return null;
            }
            return wcm.getArithmeticMutationWildcard("mutation").getOp().getMatch();
        }
    }

    private static final String STA_SUB1 = "ssub1";
    private static final String STA_FUN1 = "sfun1";

    private Expression tryRewriteFunctionCall(List<StructuredStatement> structuredStatements, JavaTypeInstance otherType,
                                              List<Expression> appliedArgs, List<LocalVariable> methodArgs) {
        WildcardMatch wcm = new WildcardMatch();

        FuncMatchCollector funcMatchCollector = new FuncMatchCollector();
        if (!matchesSingleBlock(structuredStatements, buildFunctionCallMatcher(wcm, otherType), funcMatchCollector)) return null;
        if (funcMatchCollector.matchType == null) return null;

        CloneHelper cloneHelper = buildCloneHelper(otherType, methodArgs, appliedArgs);
        return cloneHelper.replaceOrClone(funcMatchCollector.functionInvokation);
    }

    private Matcher<StructuredStatement> buildAccessorMatcher(WildcardMatch wcm, List<Expression> methodExprs) {
        return new MatchSequence(
                new BeginBlock(null),
                buildAccessorCases(wcm, methodExprs),
                new EndBlock(null)
        );
    }

    private Matcher<StructuredStatement> buildFunctionCallMatcher(WildcardMatch wcm, JavaTypeInstance otherType) {
        String MEM_SUB1 = "msub1";
        String MEM_FUN1 = "mfun1";
        return new MatchSequence(
                new BeginBlock(null),
                buildFunctionCallCases(wcm, otherType, MEM_SUB1, MEM_FUN1),
                new EndBlock(null)
        );
    }

    private MatchOneOf buildFunctionCallCases(WildcardMatch wcm,
                                              JavaTypeInstance otherType,
                                              String memberStatementName,
                                              String memberReturnName) {
        return new MatchOneOf(
                memberFunctionCase(wcm, memberStatementName, false),
                staticFunctionCase(wcm, STA_SUB1, otherType, false),
                memberFunctionCase(wcm, memberReturnName, true),
                staticFunctionCase(wcm, STA_FUN1, otherType, true)
        );
    }

    private MatchOneOf buildAccessorCases(WildcardMatch wcm, List<Expression> methodExprs) {
        return new MatchOneOf(
                returnLValueCase(wcm),
                assignmentAccessorCase(wcm, MUTATION1, true, false),
                assignmentAccessorCase(wcm, ASSIGNMENT1, false, false),
                assignmentAccessorCase(wcm, MUTATION2, false, true),
                arithmeticMutationCase(wcm),
                unaryMutationReturnCase(wcm, PRE_INC, ArithOp.PLUS, true),
                unaryMutationReturnCase(wcm, PRE_DEC, ArithOp.MINUS, true),
                unaryMutationReturnCase(wcm, POST_INC, ArithOp.PLUS, false),
                unaryMutationReturnCase(wcm, POST_DEC, ArithOp.MINUS, false),
                unaryMutationStatementCase(wcm, POST_INC, ArithOp.PLUS),
                stackPostIncrementCase(wcm),
                unaryMutationStatementCase(wcm, POST_DEC, ArithOp.MINUS),
                superAccessorCase(wcm, SUPER_INVOKE, methodExprs, false),
                superAccessorCase(wcm, SUPER_RETINVOKE, methodExprs, true)
        );
    }

    private Matcher<StructuredStatement> returnLValueCase(WildcardMatch wcm) {
        return new ResetAfterTest(wcm, RETURN_LVALUE,
                new StructuredReturn(BytecodeLoc.NONE, new LValueExpression(wcm.getLValueWildCard("lvalue")), null)
        );
    }

    private Matcher<StructuredStatement> assignmentAccessorCase(WildcardMatch wcm,
                                                                String name,
                                                                boolean returnLValue,
                                                                boolean returnRValue) {
        List<Matcher<StructuredStatement>> sequence = new ArrayList<Matcher<StructuredStatement>>();
        sequence.add(new StructuredAssignment(BytecodeLoc.NONE, wcm.getLValueWildCard("lvalue"), wcm.getExpressionWildCard("rvalue")));
        if (returnLValue) {
            sequence.add(new StructuredReturn(BytecodeLoc.NONE, new LValueExpression(wcm.getLValueWildCard("lvalue")), null));
        } else if (returnRValue) {
            sequence.add(new StructuredReturn(BytecodeLoc.NONE, wcm.getExpressionWildCard("rvalue"), null));
        }
        return new ResetAfterTest(wcm, name, new MatchSequence(sequence.toArray(new Matcher[0])));
    }

    private Matcher<StructuredStatement> arithmeticMutationCase(WildcardMatch wcm) {
        return new ResetAfterTest(wcm, MUTATION3,
                new StructuredReturn(BytecodeLoc.NONE, wcm.getArithmeticMutationWildcard("mutation", wcm.getLValueWildCard("lvalue"), wcm.getExpressionWildCard("rvalue")), null)
        );
    }

    private Matcher<StructuredStatement> unaryMutationReturnCase(WildcardMatch wcm,
                                                                 String name,
                                                                 ArithOp op,
                                                                 boolean pre) {
        Expression expression = pre
                ? new ArithmeticPreMutationOperation(BytecodeLoc.NONE, wcm.getLValueWildCard("lvalue"), op)
                : new ArithmeticPostMutationOperation(BytecodeLoc.NONE, wcm.getLValueWildCard("lvalue"), op);
        return new ResetAfterTest(wcm, name,
                new StructuredReturn(BytecodeLoc.NONE, expression, null)
        );
    }

    private Matcher<StructuredStatement> unaryMutationStatementCase(WildcardMatch wcm,
                                                                    String name,
                                                                    ArithOp op) {
        return new ResetAfterTest(wcm, name,
                new MatchSequence(
                        new StructuredExpressionStatement(BytecodeLoc.NONE, new ArithmeticPostMutationOperation(BytecodeLoc.NONE, wcm.getLValueWildCard("lvalue"), op), false),
                        new StructuredReturn(BytecodeLoc.NONE, new LValueExpression(wcm.getLValueWildCard("lvalue")), null)
                )
        );
    }

    private Matcher<StructuredStatement> stackPostIncrementCase(WildcardMatch wcm) {
        return new ResetAfterTest(wcm, POST_INC,
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
                                                           String name,
                                                           List<Expression> methodExprs,
                                                           boolean returnsValue) {
        Expression superCall = wcm.getSuperFunction("super", methodExprs);
        return new ResetAfterTest(
                wcm,
                name,
                returnsValue
                        ? new StructuredReturn(BytecodeLoc.NONE, superCall, null)
                        : new StructuredExpressionStatement(BytecodeLoc.NONE, superCall, false)
        );
    }

    private Matcher<StructuredStatement> memberFunctionCase(WildcardMatch wcm,
                                                            String name,
                                                            boolean returnsValue) {
        Expression function = wcm.getMemberFunction("func", null, false, new LValueExpression(wcm.getLValueWildCard("lvalue")), null);
        return new ResetAfterTest(
                wcm,
                name,
                returnsValue
                        ? new StructuredReturn(BytecodeLoc.NONE, function, null)
                        : new StructuredExpressionStatement(BytecodeLoc.NONE, function, false)
        );
    }

    private Matcher<StructuredStatement> staticFunctionCase(WildcardMatch wcm,
                                                            String name,
                                                            JavaTypeInstance otherType,
                                                            boolean returnsValue) {
        Expression function = wcm.getStaticFunction("func", otherType, null, null, (List<Expression>) null);
        return new ResetAfterTest(
                wcm,
                name,
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
            expressionReplacements.put(new LValueExpression(methodArg), getCastFriendArg(otherType, methodArg, appliedArg));
        }
        return new CloneHelper(expressionReplacements, lValueReplacements);
    }

    private Expression rewriteAccessorMatch(AccessorMatchCollector accessorMatchCollector,
                                            CloneHelper cloneHelper,
                                            JavaTypeInstance otherType) {
        switch (accessorMatchCollector.matchType) {
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

    private Expression getCastFriendArg(JavaTypeInstance otherType, LocalVariable methodArg, Expression appliedArg) {
        if (methodArg.getInferredJavaType().getJavaTypeInstance().equals(otherType)) {
            if (!appliedArg.getInferredJavaType().getJavaTypeInstance().equals(otherType)) {
                appliedArg = new CastExpression(BytecodeLoc.NONE, methodArg.getInferredJavaType(), appliedArg);
            }
        }
        return appliedArg;
    }

    private class FuncMatchCollector extends AbstractMatchResultIterator {

        String matchType;
        Expression functionInvokation;

        @Override
        public void collectMatches(String name, WildcardMatch wcm) {
            this.matchType = name;
            functionInvokation = getMatchedFunctionInvocation(name, wcm);
        }

        private Expression getMatchedFunctionInvocation(String name, WildcardMatch wcm) {
            if (STA_FUN1.equals(name) || STA_SUB1.equals(name)) {
                return wcm.getStaticFunction("func").getMatch();
            }
            return wcm.getMemberFunction("func").getMatch();
        }
    }

    private class VisibiliyDecreasingRewriter extends AbstractExpressionRewriter {
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
