package org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters;

import org.benf.cfr.reader.bytecode.ModernFeatureStrategy;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.*;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.InfiniteAssertRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.PreconditionAssertRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.StructuredStatementTransformer;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.util.MiscStatementTools;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.*;
import org.benf.cfr.reader.bytecode.analysis.parse.literal.TypedLiteral;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.StaticVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifier;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;
import org.benf.cfr.reader.bytecode.analysis.parse.wildcard.WildcardMatch;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredScope;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.expression.StructuredStatementExpression;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.*;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.placeholder.BeginBlock;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.placeholder.EndBlock;
import org.benf.cfr.reader.bytecode.analysis.types.InnerClassInfo;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.RawJavaType;
import org.benf.cfr.reader.bytecode.analysis.types.TypeConstants;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.entities.*;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.getopt.Options;

import java.util.List;
import java.util.Map;

public class AssertRewriter {
    private static final String MATCH_SWITCH_ASSERT_ENTRY = "switch-assert-entry";
    private static final String MATCH_ASSERT_GUARDED_AND = "assert-guarded-and";
    private static final String MATCH_ASSERT_GUARDED_DEMORGAN = "assert-guarded-demorgan";
    private static final String MATCH_ASSERT_GUARDED_NESTED = "assert-guarded-nested";
    private static final String MATCH_ASSERT_CONTROL_FLOW = "assert-control-flow";
    private static final String MATCH_ASSERT_CONTROL_FLOW_THROW = "assert-control-flow-throw";
    private static final String MATCH_ASSERT_DISABLED_ONLY = "assert-disabled-only";

    private final ClassFile classFile;
    private StaticVariable assertionStatic = null;
    private final boolean switchExpressions;
    private InferredJavaType boolIjt = new InferredJavaType(RawJavaType.BOOLEAN, InferredJavaType.Source.EXPRESSION);

    public AssertRewriter(ClassFile classFile, Options options) {
        this.classFile = classFile;
        this.switchExpressions = ModernFeatureStrategy.from(options, classFile.getClassFileVersion()).supportsSwitchExpressions();
    }

    public void sugarAsserts(Method staticInit) {
        /*
         * Determine if the static init has a line like
         *
         *         CLASSX.y = !(CLASSX.class.desiredAssertionStatus());
         *
         * where y is static final boolean.
         */
        if (!staticInit.hasCodeAttribute()) return;
        List<StructuredStatement> statements = MiscStatementTools.linearise(staticInit.getAnalysis());
        if (statements == null) return;
        MatchIterator<StructuredStatement> mi = new MatchIterator<StructuredStatement>(statements);
        WildcardMatch wcm1 = new WildcardMatch();

        final JavaTypeInstance topClassType = classFile.getClassType();
        InnerClassInfo innerClassInfo = topClassType.getInnerClassHereInfo();
        JavaTypeInstance classType = topClassType;
        while (innerClassInfo != InnerClassInfo.NOT) {
            JavaTypeInstance nextClass = innerClassInfo.getOuterClass();
            if (nextClass == null || nextClass.equals(classType)) break;
            classType = nextClass;
            innerClassInfo = classType.getInnerClassHereInfo();
        }

        Matcher<StructuredStatement> m = new ResetAfterTest(wcm1,
                new CollectMatch("ass1", new StructuredAssignment(BytecodeLoc.NONE,
                        wcm1.getStaticVariable("assertbool", topClassType, new InferredJavaType(RawJavaType.BOOLEAN, InferredJavaType.Source.TEST)),
                        new NotOperation(BytecodeLoc.NONE, new BooleanExpression(
                                wcm1.getMemberFunction("assertmeth", "desiredAssertionStatus",
                                        new Literal(TypedLiteral.getClass(classType))
                                )
                        ))
                ))
        );

        AssertVarCollector matchResultCollector = new AssertVarCollector(wcm1);
        while (mi.hasNext()) {
            mi.advance();
            matchResultCollector.clear();
            if (m.match(mi, matchResultCollector)) {
                // This really should only match once.  If it matches multiple times, something else
                // is being identically initialised, which is probably wrong!
                if (matchResultCollector.matched()) break;
                mi.rewind1();
            }
        }
        if (!matchResultCollector.matched()) return;
        assertionStatic = matchResultCollector.assertStatic;

        /*
         * Ok, now we know what the assertion field is.  Let's search all conditionals for it - if we find one,
         * we can transform that conditional into an assert
         *
         * if (!assertionsDisabledField && (x)) --> assert(!x))
         */
        rewriteMethods();

    }

    private class AssertVarCollector extends AbstractMatchResultIterator {

        private final WildcardMatch wcm;
        ClassFileField assertField = null;
        StaticVariable assertStatic = null;

        private AssertVarCollector(WildcardMatch wcm) {
            this.wcm = wcm;
        }

        @Override
        public void clear() {
            assertField = null;
            assertStatic = null;
        }

        @Override
        public void collectStatement(String name, StructuredStatement statement) {
            StaticVariable staticVariable = wcm.getStaticVariable("assertbool").getMatch();

            ClassFileField field;
            try {
                field = classFile.getFieldByName(staticVariable.getFieldName(), staticVariable.getInferredJavaType().getJavaTypeInstance());
            } catch (NoSuchFieldException e) {
                return;
            }
            if (!field.getField().testAccessFlag(AccessFlag.ACC_SYNTHETIC)) return;
            assertField = field;
            statement.getContainer().nopOut();
            assertField.markHidden();
            assertStatic = staticVariable;
        }

        boolean matched() {
            return (assertField != null);
        }
    }

    private void rewriteMethods() {
        List<Method> methods = classFile.getMethods();
        WildcardMatch wcm1 = new WildcardMatch();
        Matcher<StructuredStatement> standardAssertMatcher = buildStandardAssertMatcher(wcm1);
        AssertUseCollector collector = new AssertUseCollector(wcm1);

        SwitchAssertRewriter swcollector = new SwitchAssertRewriter();

        for (Method method : methods) {
            if (!method.hasCodeAttribute()) continue;

            Op04StructuredStatement top = method.getAnalysis();
            if (top == null) continue;

            handlePreConditionedAsserts(top);

            /*
             * Pre-transform a couple of particularly horrible samples.
             * (where the assertion gets moved into a while block test).
             * See InfiniteAssert tests.
             */
            handleInfiniteAsserts(top);


            List<StructuredStatement> statements = MiscStatementTools.linearise(top);

            if (statements == null) continue;

            MatchIterator<StructuredStatement> mi = new MatchIterator<StructuredStatement>(statements);

            while (mi.hasNext()) {
                mi.advance();
                if (standardAssertMatcher.match(mi, collector)) {
                    mi.rewind1();
                }
            }

            // If we're looking for switch expression assertions, things get more interesting.
            // We can't search for a simple pattern any more, but we can find possible entry points.
            if (switchExpressions) {
                top.transform(swcollector, new StructuredScope());
            }
        }
    }

    private Matcher<StructuredStatement> buildStandardAssertMatcher(WildcardMatch wcm1) {
        return new ResetAfterTest(wcm1,
                new MatchOneOf(
                        buildGuardedAndAssertMatcher(wcm1),
                        buildGuardedDemorganAssertMatcher(wcm1),
                        buildGuardedNestedAssertMatcher(wcm1),
                        buildControlFlowAssertMatcher(wcm1),
                        buildDisabledOnlyAssertMatcher(wcm1)
                )
        );
    }

    private Matcher<StructuredStatement> buildGuardedAndAssertMatcher(WildcardMatch wcm1) {
        return new CollectMatch(MATCH_ASSERT_GUARDED_AND, new MatchSequence(
                new StructuredIf(BytecodeLoc.NONE,
                        new BooleanOperation(BytecodeLoc.NONE,
                                new NotOperation(BytecodeLoc.NONE, new BooleanExpression(new LValueExpression(assertionStatic))),
                                wcm1.getConditionalExpressionWildcard("condition"),
                                BoolOp.AND), null
                ),
                new BeginBlock(null),
                new StructuredThrow(BytecodeLoc.NONE, wcm1.getConstructorSimpleWildcard("exception", TypeConstants.ASSERTION_ERROR)),
                new EndBlock(null)
        ));
    }

    private Matcher<StructuredStatement> buildGuardedDemorganAssertMatcher(WildcardMatch wcm1) {
        return new CollectMatch(MATCH_ASSERT_GUARDED_DEMORGAN, new MatchSequence(
                new StructuredIf(BytecodeLoc.NONE,
                        new NotOperation(BytecodeLoc.NONE,
                                new BooleanOperation(BytecodeLoc.NONE, new BooleanExpression(new LValueExpression(assertionStatic)),
                                        wcm1.getConditionalExpressionWildcard("condition"),
                                        BoolOp.OR)), null
                ),
                new BeginBlock(null),
                new StructuredThrow(BytecodeLoc.NONE, wcm1.getConstructorSimpleWildcard("exception", TypeConstants.ASSERTION_ERROR)),
                new EndBlock(null)
        ));
    }

    private Matcher<StructuredStatement> buildGuardedNestedAssertMatcher(WildcardMatch wcm1) {
        return new CollectMatch(MATCH_ASSERT_GUARDED_NESTED, new MatchSequence(
                new StructuredIf(BytecodeLoc.NONE, new NotOperation(BytecodeLoc.NONE, new BooleanExpression(new LValueExpression(assertionStatic))), null ),
                new BeginBlock(null),
                new StructuredIf(BytecodeLoc.NONE, wcm1.getConditionalExpressionWildcard("condition"), null ),
                new BeginBlock(null),
                new StructuredThrow(BytecodeLoc.NONE, wcm1.getConstructorSimpleWildcard("exception", TypeConstants.ASSERTION_ERROR)),
                new EndBlock(null) ,
                new EndBlock(null)
        ));
    }

    private Matcher<StructuredStatement> buildControlFlowAssertMatcher(WildcardMatch wcm1) {
        return new CollectMatch(MATCH_ASSERT_CONTROL_FLOW, new MatchSequence(
                new MatchOneOf(
                        new StructuredIf(BytecodeLoc.NONE,
                                new BooleanOperation(BytecodeLoc.NONE,
                                        new BooleanExpression(new LValueExpression(assertionStatic)),
                                        wcm1.getConditionalExpressionWildcard("condition2"),
                                        BoolOp.OR), null),
                        new StructuredIf(BytecodeLoc.NONE,
                                new BooleanExpression(new LValueExpression(assertionStatic)), null)
                ),
                new BeginBlock(wcm1.getBlockWildcard("condBlock")),
                new MatchOneOf(
                        new StructuredReturn(BytecodeLoc.NONE, null, null),
                        new StructuredReturn(BytecodeLoc.NONE, wcm1.getExpressionWildCard("retval"), null),
                        new StructuredBreak(BytecodeLoc.NONE, wcm1.getBlockIdentifier("breakblock"), false)
                ),
                new EndBlock(wcm1.getBlockWildcard("condBlock")),
                new CollectMatch(MATCH_ASSERT_CONTROL_FLOW_THROW, new StructuredThrow(BytecodeLoc.NONE, wcm1.getConstructorSimpleWildcard("exception", TypeConstants.ASSERTION_ERROR)))
        ));
    }

    private Matcher<StructuredStatement> buildDisabledOnlyAssertMatcher(WildcardMatch wcm1) {
        return new CollectMatch(MATCH_ASSERT_DISABLED_ONLY, new MatchSequence(
                new StructuredIf(BytecodeLoc.NONE,
                        new NotOperation(BytecodeLoc.NONE, new BooleanExpression(new LValueExpression(assertionStatic))), null
                ),
                new BeginBlock(null),
                new StructuredThrow(BytecodeLoc.NONE, wcm1.getConstructorSimpleWildcard("exception", TypeConstants.ASSERTION_ERROR)),
                new EndBlock(null)
        ));
    }

    /*
     * Consider AssertTest19:
     *  public static void f(Integer x) {
     *   if (x > 1) {
     *       assert (x!=3);
     *   }
     * }
     *
     * The first test happens regardless, the second inside the assert
     *
     * reasonable : if (x>1&& !assertionsDisabled && x==3) throw();
     * totally wrong : assert(x>1 && x!=3)
     *
     * but correct is as above ;)
     */
    private void handlePreConditionedAsserts(Op04StructuredStatement statements) {
        PreconditionAssertRewriter rewriter = new PreconditionAssertRewriter(assertionStatic);
        rewriter.transform(statements);
    }

    private void handleInfiniteAsserts(Op04StructuredStatement statements) {
        InfiniteAssertRewriter rewriter = new InfiniteAssertRewriter(assertionStatic);
        rewriter.transform(statements);
    }

    private class SwitchAssertRewriter implements StructuredStatementTransformer {

        @Override
        public StructuredStatement transform(StructuredStatement in, StructuredScope scope) {
            in.transformStructuredChildren(this, scope);
            if (!(in instanceof Block)) {
                return in;
            }
            SwitchAssertContext context = extractSwitchAssertContext((Block) in);
            if (context == null) {
                return in;
            }
            StructuredStatement newAssert = rewriteSwitchAssertion(context);
            if (newAssert == null) {
                return in;
            }
            if (context.replacementBlock != null) {
                releaseForeignBreakTarget(getSingleIfTakenStatement(context.ifStatement));
                List<Op04StructuredStatement> replacementStatements = ListFactory.newList();
                replacementStatements.add(new Op04StructuredStatement(newAssert));
                context.replacementBlock.replaceBlockStatements(replacementStatements);
            } else {
                context.ifContainer.replaceStatement(newAssert);
            }
            return in;
        }

        private SwitchAssertContext extractSwitchAssertContext(Block block) {
            List<Op04StructuredStatement> blockStatements = block.getFilteredBlockStatements();
            if (blockStatements.isEmpty()) {
                return null;
            }
            SwitchAssertContext context = extractNestedSwitchAssertContext(block, blockStatements);
            if (context != null) {
                return context;
            }
            return extractControlFlowSwitchAssertContext(block, blockStatements);
        }

        private SwitchAssertContext extractNestedSwitchAssertContext(Block block,
                                                                    List<Op04StructuredStatement> blockStatements) {
            if (blockStatements.size() != 1) {
                return null;
            }
            Op04StructuredStatement ifContainer = blockStatements.get(0);
            StructuredStatement ifStatement = ifContainer.getStatement();
            if (!(ifStatement instanceof StructuredIf)) {
                return null;
            }

            StructuredIf structuredIf = (StructuredIf) ifStatement;
            Op04StructuredStatement taken = structuredIf.getIfTaken();
            StructuredStatement takenBody = taken.getStatement();
            if (takenBody.getClass() != Block.class) {
                return null;
            }
            Block takenBlock = (Block) takenBody;
            List<Op04StructuredStatement> switchAndThrow = takenBlock.getFilteredBlockStatements();
            if (switchAndThrow.isEmpty()) {
                return null;
            }

            BlockIdentifier outerBlock = block.getBreakableBlockOrNull();
            StructuredStatement switchStatement = switchAndThrow.get(0).getStatement();
            if (!(switchStatement instanceof StructuredSwitch)) {
                return null;
            }
            StructuredSwitch structuredSwitch = (StructuredSwitch) switchStatement;
            Op04StructuredStatement switchBody = structuredSwitch.getBody();
            if (!(switchBody.getStatement() instanceof Block)) {
                return null;
            }
            Block switchBodyBlock = (Block) switchBody.getStatement();

            return createSwitchAssertContext(ifContainer, structuredIf, outerBlock, structuredSwitch, switchBody, switchBodyBlock, switchAndThrow, takenBlock, null);
        }

        private SwitchAssertContext extractControlFlowSwitchAssertContext(Block block,
                                                                         List<Op04StructuredStatement> blockStatements) {
            if (blockStatements.size() < 2) {
                return null;
            }
            Op04StructuredStatement ifContainer = blockStatements.get(0);
            StructuredStatement ifStatement = ifContainer.getStatement();
            if (!(ifStatement instanceof StructuredIf)) {
                return null;
            }
            StructuredIf structuredIf = (StructuredIf) ifStatement;
            BlockIdentifier guardBreakTarget = getAssertionDisableBreakTarget(structuredIf);
            if (guardBreakTarget == null) {
                return null;
            }
            StructuredStatement switchStatement = blockStatements.get(1).getStatement();
            if (!(switchStatement instanceof StructuredSwitch)) {
                return null;
            }
            StructuredSwitch structuredSwitch = (StructuredSwitch) switchStatement;
            Op04StructuredStatement switchBody = structuredSwitch.getBody();
            if (!(switchBody.getStatement() instanceof Block)) {
                return null;
            }
            Block switchBodyBlock = (Block) switchBody.getStatement();
            List<Op04StructuredStatement> switchAndThrow = ListFactory.newList();
            switchAndThrow.add(blockStatements.get(1));
            for (int idx = 2; idx < blockStatements.size(); ++idx) {
                switchAndThrow.add(blockStatements.get(idx));
            }
            return createSwitchAssertContext(ifContainer, structuredIf, guardBreakTarget, structuredSwitch, switchBody, switchBodyBlock, switchAndThrow, null, block);
        }

        private SwitchAssertContext createSwitchAssertContext(Op04StructuredStatement ifContainer,
                                                              StructuredIf structuredIf,
                                                              BlockIdentifier outerBlock,
                                                              StructuredSwitch structuredSwitch,
                                                              Op04StructuredStatement switchBody,
                                                              Block switchBodyBlock,
                                                              List<Op04StructuredStatement> switchAndThrow,
                                                              Block rewriteBlock,
                                                              Block replacementBlock) {
            if (switchAndThrow.size() > 2) {
                if (rewriteBlock == null) {
                    return null;
                }
                switchAndThrow = tryCombineSwitch(switchAndThrow, outerBlock, structuredSwitch.getBlockIdentifier(), switchBodyBlock);
                if (switchAndThrow.size() != 1) {
                    return null;
                }
                rewriteBlock.replaceBlockStatements(switchAndThrow);
            }
            return new SwitchAssertContext(ifContainer, structuredIf, outerBlock, structuredSwitch.getBlockIdentifier(), switchBody, structuredSwitch, switchBodyBlock, switchAndThrow, replacementBlock);
        }

        private BlockIdentifier getAssertionDisableBreakTarget(StructuredIf structuredIf) {
            if (structuredIf.hasElseBlock()) {
                return null;
            }
            StructuredStatement breakStatement = getSingleIfTakenStatement(structuredIf);
            if (!(breakStatement instanceof StructuredBreak)) {
                return null;
            }
            StructuredBreak structuredBreak = (StructuredBreak) breakStatement;
            return structuredBreak.isLocalBreak() ? null : structuredBreak.getBreakBlock();
        }

        private StructuredStatement getSingleIfTakenStatement(StructuredIf structuredIf) {
            StructuredStatement ifTaken = structuredIf.getIfTaken().getStatement();
            if (!(ifTaken instanceof Block)) {
                return ifTaken;
            }
            List<Op04StructuredStatement> ifTakenStatements = ((Block) ifTaken).getFilteredBlockStatements();
            if (ifTakenStatements.size() != 1) {
                return null;
            }
            return ifTakenStatements.get(0).getStatement();
        }

        private StructuredStatement rewriteSwitchAssertion(SwitchAssertContext context) {
            SwitchAssertionPlan plan = createSwitchAssertionPlan(context);
            if (plan == null) {
                return null;
            }
            List<SwitchExpression.Branch> branches = ListFactory.newList();
            if (!extractSwitchBranches(
                    plan.trueBlock,
                    plan.falseBlock,
                    context.switchBodyBlock,
                    branches,
                    plan.replacements,
                    plan.fallthroughValue)) {
                return null;
            }
            return buildSwitchAssertion(context.ifStatement, context.structuredSwitch, branches, plan.exceptArg);
        }

        private SwitchAssertionPlan createSwitchAssertionPlan(SwitchAssertContext context) {
            switch (context.switchAndThrow.size()) {
                case 1:
                    return createEmbeddedSwitchAssertionPlan(context);
                case 2:
                    return createTrailingThrowSwitchAssertionPlan(context);
                default:
                    return null;
            }
        }

        private SwitchAssertionPlan createTrailingThrowSwitchAssertionPlan(SwitchAssertContext context) {
            Pair<Boolean, Expression> exceptionTest = extractAssertionThrowArgument(context.switchAndThrow.get(1).getStatement());
            if (!exceptionTest.getFirst()) {
                return null;
            }
            return new SwitchAssertionPlan(
                    context.outerBlock,
                    context.switchBlock,
                    exceptionTest.getSecond(),
                    MapFactory.<Op04StructuredStatement, StructuredExpressionYield>newOrderedMap(),
                    Literal.FALSE
            );
        }

        private SwitchAssertionPlan createEmbeddedSwitchAssertionPlan(SwitchAssertContext context) {
            Map<Op04StructuredStatement, StructuredExpressionYield> replacements = MapFactory.newOrderedMap();
            AssertionTrackingControlFlowSwitchExpressionTransformer track =
                    new AssertionTrackingControlFlowSwitchExpressionTransformer(context.switchBlock, context.outerBlock, replacements);
            context.switchBody.transform(track, new StructuredScope());
            if (track.failed || track.assertionThrows.size() > 1) {
                return null;
            }

            Expression exceptArg = null;
            replacements.clear();
            if (track.assertionThrows.size() == 1) {
                StructuredStatement throwStatement = track.assertionThrows.get(0);
                Pair<Boolean, Expression> exceptionTest = extractAssertionThrowArgument(throwStatement);
                if (!exceptionTest.getFirst()) {
                    return null;
                }
                exceptArg = exceptionTest.getSecond();
                replacements.put(throwStatement.getContainer(), new StructuredExpressionYield(BytecodeLoc.TODO, Literal.FALSE));
            }

            return new SwitchAssertionPlan(
                    context.switchBlock,
                    context.switchBlock,
                    exceptArg,
                    replacements,
                    Literal.TRUE
            );
        }

        private final class SwitchAssertContext {
            private final Op04StructuredStatement ifContainer;
            private final StructuredIf ifStatement;
            private final BlockIdentifier outerBlock;
            private final BlockIdentifier switchBlock;
            private final Op04StructuredStatement switchBody;
            private final StructuredSwitch structuredSwitch;
            private final Block switchBodyBlock;
            private final List<Op04StructuredStatement> switchAndThrow;
            private final Block replacementBlock;

            private SwitchAssertContext(Op04StructuredStatement ifContainer,
                                        StructuredIf ifStatement,
                                        BlockIdentifier outerBlock,
                                        BlockIdentifier switchBlock,
                                        Op04StructuredStatement switchBody,
                                        StructuredSwitch structuredSwitch,
                                        Block switchBodyBlock,
                                        List<Op04StructuredStatement> switchAndThrow,
                                        Block replacementBlock) {
                this.ifContainer = ifContainer;
                this.ifStatement = ifStatement;
                this.outerBlock = outerBlock;
                this.switchBlock = switchBlock;
                this.switchBody = switchBody;
                this.structuredSwitch = structuredSwitch;
                this.switchBodyBlock = switchBodyBlock;
                this.switchAndThrow = switchAndThrow;
                this.replacementBlock = replacementBlock;
            }
        }

        private final class SwitchAssertionPlan {
            private final BlockIdentifier trueBlock;
            private final BlockIdentifier falseBlock;
            private final Expression exceptArg;
            private final Map<Op04StructuredStatement, StructuredExpressionYield> replacements;
            private final Expression fallthroughValue;

            private SwitchAssertionPlan(BlockIdentifier trueBlock,
                                        BlockIdentifier falseBlock,
                                        Expression exceptArg,
                                        Map<Op04StructuredStatement, StructuredExpressionYield> replacements,
                                        Expression fallthroughValue) {
                this.trueBlock = trueBlock;
                this.falseBlock = falseBlock;
                this.exceptArg = exceptArg;
                this.replacements = replacements;
                this.fallthroughValue = fallthroughValue;
            }
        }

        // We know the first statement is a switch - if it doesn't have any breaks in it, then we're safe to
        // roll outer statements in.
        private List<Op04StructuredStatement> tryCombineSwitch(List<Op04StructuredStatement> content, BlockIdentifier outer, BlockIdentifier swiBlockIdentifier, Block swBodyBlock) {
            Map<Op04StructuredStatement, StructuredExpressionYield> replacements = MapFactory.newOrderedMap();
            ControlFlowSwitchExpressionTransformer cfset = new ControlFlowSwitchExpressionTransformer(outer, swiBlockIdentifier, replacements);
            content.get(0).transform(cfset, new StructuredScope());

            if (cfset.failed) return content;
            if (cfset.yieldFalseCount != 0) return content;

            List<Op04StructuredStatement> cases = swBodyBlock.getFilteredBlockStatements();
            Op04StructuredStatement lastCase = cases.get(cases.size()-1);
            StructuredStatement ss = lastCase.getStatement();
            if (!(ss instanceof StructuredCase)) return content;
            Op04StructuredStatement body = ((StructuredCase) ss).getBody();
            StructuredStatement bodySS = body.getStatement();
            Block block;
            if (bodySS instanceof Block) {
                block = (Block)bodySS;
            } else {
                block = new Block(body);
            }
            block.setIndenting(true);
            block.getBlockStatements().addAll(content.subList(1, content.size()));
            body.replaceStatement(block);
            return ListFactory.newList(content.get(0));
        }

        private Pair<Boolean, Expression> extractAssertionThrowArgument(StructuredStatement throwS) {
            WildcardMatch wcm2 = new WildcardMatch();
            WildcardMatch.ConstructorInvokationSimpleWildcard constructor = wcm2.getConstructorSimpleWildcard("exception", TypeConstants.ASSERTION_ERROR);
            StructuredStatement test = new StructuredThrow(BytecodeLoc.TODO, constructor);
            if (!test.equals(throwS)) return Pair.make(false, null);;
            Expression exceptArg = null;
            List<Expression> consArg = constructor.getMatch().getArgs();
            if (consArg.size() == 1) {
                exceptArg = consArg.get(0);
            } else if (consArg.size() > 1) {
                return Pair.make(false, null);
            }
            return Pair.make(true, exceptArg);
        }

        private void applyYieldReplacements(Map<Op04StructuredStatement, StructuredExpressionYield> replacements) {
            for (Map.Entry<Op04StructuredStatement, StructuredExpressionYield> replacement : replacements.entrySet()) {
                Op04StructuredStatement statementContainer = replacement.getKey();
                releaseForeignBreakTarget(statementContainer.getStatement());
                statementContainer.replaceStatement(replacement.getValue());
            }
        }

        private void releaseForeignBreakTarget(StructuredStatement statement) {
            if (!(statement instanceof StructuredBreak)) {
                return;
            }
            StructuredBreak structuredBreak = (StructuredBreak) statement;
            if (!structuredBreak.isLocalBreak()) {
                structuredBreak.getBreakBlock().releaseForeignRef();
            }
        }

        private boolean extractSwitchBranches(BlockIdentifier outer,
                                              BlockIdentifier swiBlockIdentifier,
                                              Block swBodyBlock,
                                              List<SwitchExpression.Branch> branches,
                                              Map<Op04StructuredStatement, StructuredExpressionYield> replacements,
                                              Expression fallthroughValue) {
            for (Op04StructuredStatement statement : swBodyBlock.getBlockStatements()) {
                SwitchExpression.Branch branch = extractSwitchBranch(outer, swiBlockIdentifier, replacements, statement, fallthroughValue);
                if (branch == null) return false;
                branches.add(branch);
            }
            applyYieldReplacements(replacements);
            return true;
        }

        private SwitchExpression.Branch extractSwitchBranch(BlockIdentifier outer,
                                                            BlockIdentifier swiBlockIdentifier,
                                                            Map<Op04StructuredStatement, StructuredExpressionYield> replacements,
                                                            Op04StructuredStatement statement,
                                                            Expression fallthroughValue) {
            StructuredStatement cstm = statement.getStatement();
            if (!(cstm instanceof StructuredCase)) return null;
            StructuredCase caseStm = (StructuredCase)cstm;
            ControlFlowSwitchExpressionTransformer cfset = new ControlFlowSwitchExpressionTransformer(outer, swiBlockIdentifier, replacements);
            Op04StructuredStatement body = caseStm.getBody();
            body.transform(cfset, new StructuredScope());
            if (cfset.failed) return null;
            appendYieldIfBranchFallsThrough(body, replacements, cfset, fallthroughValue);
            Expression value = getBranchValue(body, cfset);
            return new SwitchExpression.Branch(caseStm.getValues(), value);
        }

        private void appendYieldIfBranchFallsThrough(Op04StructuredStatement body,
                                                     Map<Op04StructuredStatement, StructuredExpressionYield> replacements,
                                                     ControlFlowSwitchExpressionTransformer transformer,
                                                     Expression fallthroughValue) {
            if (fallthroughValue == null) {
                return;
            }
            StructuredStatement statement = body.getStatement();
            if (!(statement instanceof Block)) {
                return;
            }
            Block block = (Block) statement;
            Op04StructuredStatement last = block.getLast();
            StructuredStatement lastStatement = replacements.get(last);
            if (lastStatement == null) {
                lastStatement = last.getStatement();
            }
            if (lastStatement instanceof StructuredExpressionYield) {
                return;
            }
            transformer.totalStatements++;
            block.getBlockStatements().add(new Op04StructuredStatement(new StructuredExpressionYield(BytecodeLoc.TODO, fallthroughValue)));
        }

        private Expression getBranchValue(Op04StructuredStatement body, ControlFlowSwitchExpressionTransformer transformer) {
            return transformer.totalStatements == 0
                    ? transformer.singleValue
                    : new StructuredStatementExpression(boolIjt, body.getStatement());
        }

        private StructuredStatement buildSwitchAssertion(StructuredIf ifStm,
                                                        StructuredSwitch structuredSwitch,
                                                        List<SwitchExpression.Branch> branches,
                                                        Expression exceptArg) {
            SwitchExpression sw = new SwitchExpression(BytecodeLoc.TODO, boolIjt, structuredSwitch.getSwitchOn(), branches);
            return ifStm.convertToAssertion(StructuredAssert.mkStructuredAssert(BytecodeLoc.TODO, new BooleanExpression(sw), exceptArg));
        }
    }

    static class AssertionTrackingControlFlowSwitchExpressionTransformer extends ControlFlowSwitchExpressionTransformer {
        List<StructuredStatement> assertionThrows = ListFactory.newList();

        AssertionTrackingControlFlowSwitchExpressionTransformer(BlockIdentifier trueBlock, BlockIdentifier falseBlock, Map<Op04StructuredStatement, StructuredExpressionYield> replacements) {
            super(trueBlock, falseBlock, replacements);
        }

        @Override
        void observeStatement(StructuredStatement in) {
            if (in instanceof StructuredThrow) {
                if (((StructuredThrow) in).getValue().getInferredJavaType().getJavaTypeInstance().equals(TypeConstants.ASSERTION_ERROR)) {
                    assertionThrows.add(in);
                }
            }
        }
    }

    static class ControlFlowSwitchExpressionTransformer implements StructuredStatementTransformer {
        private Map<Op04StructuredStatement, StructuredExpressionYield> replacements;
        protected boolean failed;
        int totalStatements;
        Expression singleValue;
        int yieldFalseCount = 0;
        private BlockIdentifier yieldTrueBlock;
        private BlockIdentifier yieldFalseBlock;

        private ControlFlowSwitchExpressionTransformer(BlockIdentifier trueBlock, BlockIdentifier falseBlock, Map<Op04StructuredStatement, StructuredExpressionYield> replacements) {
            this.yieldTrueBlock = trueBlock;
            this.yieldFalseBlock = falseBlock;
            this.replacements = replacements;
        }

        void observeStatement(StructuredStatement in) {
        }

        @Override
        public StructuredStatement transform(StructuredStatement in, StructuredScope scope) {
            if (failed) return in;

            if (in.isEffectivelyNOP()) return in;

            if (!(in instanceof Block)) {
                StructuredExpressionYield yield = replacements.get(in.getContainer());
                if (yield == null) {
                    totalStatements++;
                } else {
                    singleValue = yield.getValue();
                }
            }

            if (in instanceof StructuredBreak) {
                BreakClassification bk = classifyBreak((StructuredBreak)in, scope);
                switch (bk) {
                    case TRUE_BLOCK:
                        totalStatements--;
                        singleValue = Literal.TRUE;
                        replacements.put(in.getContainer(), new StructuredExpressionYield(BytecodeLoc.NONE, singleValue));
                        return in;
                    case FALSE_BLOCK:
                        totalStatements--;
                        yieldFalseCount++;
                        singleValue = Literal.FALSE;
                        replacements.put(in.getContainer(), new StructuredExpressionYield(BytecodeLoc.NONE, singleValue));
                        return in;
                    case INNER:
                        break;
                    case TOO_FAR:
                        failed = true;
                        return in;
                }
            }

            if (in instanceof StructuredReturn) {
                failed = true;
                return in;
            }

            observeStatement(in);

            in.transformStructuredChildren(this, scope);
            return in;
        }

        BreakClassification classifyBreak(StructuredBreak in, StructuredScope scope) {
            BlockIdentifier breakBlock = in.getBreakBlock();
            if (breakBlock == yieldTrueBlock) return BreakClassification.TRUE_BLOCK;
            if (breakBlock == yieldFalseBlock) return BreakClassification.FALSE_BLOCK;
            for (StructuredStatement stm : scope.getAll()) {
                BlockIdentifier block = stm.getBreakableBlockOrNull();
                if (block == breakBlock) return BreakClassification.INNER;
            }
            return BreakClassification.TOO_FAR;
        }

        enum BreakClassification {
            TRUE_BLOCK,
            FALSE_BLOCK,
            TOO_FAR,
            INNER
        }
    }


    private class AssertUseCollector extends AbstractMatchResultIterator {

        private StructuredStatement ass2throw;

        private final WildcardMatch wcm;

        private AssertUseCollector(WildcardMatch wcm) {
            this.wcm = wcm;
        }

        @Override
        public void clear() {
            ass2throw = null;
        }

        @Override
        public void collectStatement(String name, StructuredStatement statement) {
            Expression arg = getAssertionArgument();
            switch (name) {
                case MATCH_ASSERT_GUARDED_AND:
                case MATCH_ASSERT_GUARDED_DEMORGAN:
                case MATCH_ASSERT_GUARDED_NESTED:
                    rewriteGuardedAssert(name, (StructuredIf) statement, arg);
                    break;
                case MATCH_ASSERT_CONTROL_FLOW:
                    rewriteControlFlowAssert((StructuredIf) statement, arg);
                    break;
                case MATCH_ASSERT_CONTROL_FLOW_THROW:
                    ass2throw = statement;
                    break;
                case MATCH_ASSERT_DISABLED_ONLY:
                    rewriteDisabledOnlyAssert((StructuredIf) statement, arg);
                    break;
                default:
                    break;
            }
        }

        private Expression getAssertionArgument() {
            WildcardMatch.ConstructorInvokationSimpleWildcard constructor = wcm.getConstructorSimpleWildcard("exception");
            List<Expression> args = constructor.getMatch().getArgs();
            Expression arg = args.size() > 0 ? args.get(0) : null;
            if (arg instanceof CastExpression && arg.getInferredJavaType().getJavaTypeInstance() == TypeConstants.OBJECT) {
                return ((CastExpression) arg).getChild();
            }
            return arg;
        }

        private void rewriteGuardedAssert(String name, StructuredIf ifStatement, Expression arg) {
            ConditionalExpression condition = wcm.getConditionalExpressionWildcard("condition").getMatch();
            if (MATCH_ASSERT_GUARDED_AND.equals(name) || MATCH_ASSERT_GUARDED_NESTED.equals(name)) {
                condition = new NotOperation(BytecodeLoc.TODO, condition);
            }
            condition = condition.simplify();
            StructuredStatement structuredAssert = ifStatement.convertToAssertion(StructuredAssert.mkStructuredAssert(BytecodeLoc.TODO, condition, arg));
            ifStatement.getContainer().replaceStatement(structuredAssert);
        }

        private void rewriteControlFlowAssert(StructuredIf ifStatement, Expression arg) {
            if (ass2throw == null) throw new IllegalStateException();
            WildcardMatch.ConditionalExpressionWildcard wcard = wcm.getConditionalExpressionWildcard("condition2");
            ConditionalExpression conditionalExpression = wcard.getMatch();
            if (conditionalExpression == null) {
                conditionalExpression = new BooleanExpression(new Literal(TypedLiteral.getBoolean(0)));
            }
            StructuredStatement structuredAssert = StructuredAssert.mkStructuredAssert(BytecodeLoc.TODO, conditionalExpression, arg);
            ifStatement.getContainer().replaceStatement(structuredAssert);
            ass2throw.getContainer().replaceStatement(ifStatement.getIfTaken().getStatement());
        }

        private void rewriteDisabledOnlyAssert(StructuredIf ifStatement, Expression arg) {
            StructuredStatement structuredAssert = ifStatement.convertToAssertion(StructuredAssert.mkStructuredAssert(BytecodeLoc.TODO, new BooleanExpression(Literal.FALSE), arg));
            ifStatement.getContainer().replaceStatement(structuredAssert);
        }
    }

}
