package org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters;

import org.benf.cfr.reader.bytecode.BytecodeMeta;
import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.AbstractMatchResultIterator;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.CollectMatch;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.KleenePlus;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.KleeneStar;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchIterator;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchOneOf;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchSequence;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.Matcher;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.ResetAfterTest;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.util.MiscStatementTools;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.BooleanExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.CastExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.LValueExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.Literal;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.NotOperation;
import org.benf.cfr.reader.bytecode.analysis.parse.literal.TypedLiteral;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifier;
import org.benf.cfr.reader.bytecode.analysis.parse.wildcard.WildcardMatch;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.Block;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredAssignment;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredBreak;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredCase;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredExpressionStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredIf;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredSwitch;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.placeholder.BeginBlock;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.placeholder.EndBlock;
import org.benf.cfr.reader.bytecode.analysis.types.TypeConstants;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.util.ClassFileVersion;
import org.benf.cfr.reader.util.MiscConstants;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.getopt.Options;
import org.benf.cfr.reader.util.getopt.OptionsImpl;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class SwitchStringRewriter implements Op04Rewriter {
    private static final String MATCH_STRING_OBJECT_ASSIGN = "string-object-assign";
    private static final String MATCH_INTERMEDIATE_ASSIGN = "switch-index-assign";
    private static final String MATCH_HASH_CALL = "hash-call";
    private static final String MATCH_HASH_SWITCH = "hash-switch";
    private static final String MATCH_INDEX_SWITCH = "index-switch";
    private static final String MATCH_CASE_ASSIGN = "case-index-assign";
    private static final String MATCH_HASH_COLLISION_ASSIGN = "hash-collision-index-assign";

    private final Options options;
    private final ClassFileVersion classFileVersion;
    private final BytecodeMeta bytecodeMeta;

    public SwitchStringRewriter(Options options, ClassFileVersion classFileVersion, BytecodeMeta bytecodeMeta) {
        this.options = options;
        this.classFileVersion = classFileVersion;
        this.bytecodeMeta = bytecodeMeta;
    }

    @Override
    public void rewrite(Op04StructuredStatement root) {
        if (!(options.getOption(OptionsImpl.STRING_SWITCH, classFileVersion)
                || bytecodeMeta.has(BytecodeMeta.CodeInfoFlag.STRING_SWITCHES))
        ) return;

        List<StructuredStatement> structuredStatements = MiscStatementTools.linearise(root);
        if (structuredStatements == null) return;

        rewriteComplex(structuredStatements);
        rewriteEmpty(structuredStatements);
    }

    /*
     * also handle the (bizarre) situation where
     *
     * switch (foo) {
     *    default:
     *    --->
     * }
     *
     * gives us
     *
     * foo.hashcode()
     * switch(-1) {
     *    default:
     * }
     *
     * Note that this doesn't pull anything into the switch (so switch expressions will need further work).
     */
    private void rewriteEmpty(List<StructuredStatement> structuredStatements) {
        MatchIterator<StructuredStatement> mi = new MatchIterator<StructuredStatement>(structuredStatements);

        WildcardMatch wcm = new WildcardMatch();

        Matcher<StructuredStatement> m = new ResetAfterTest(wcm, "r1", new MatchSequence(
                new CollectMatch(MATCH_STRING_OBJECT_ASSIGN, new StructuredAssignment(BytecodeLoc.NONE, wcm.getLValueWildCard("stringobject"), wcm.getExpressionWildCard("originalstring"))),
                new CollectMatch(MATCH_INTERMEDIATE_ASSIGN, new StructuredAssignment(BytecodeLoc.NONE, wcm.getLValueWildCard("intermed"), Literal.MINUS_ONE)),
                new CollectMatch(MATCH_HASH_CALL, new StructuredExpressionStatement(BytecodeLoc.NONE,
                        wcm.getMemberFunction("hash", MiscConstants.HASHCODE, wcm.getExpressionWildCard("stringobjornull")), false
                )),
                new CollectMatch(MATCH_HASH_SWITCH,
                        new StructuredSwitch(BytecodeLoc.NONE, new LValueExpression(wcm.getLValueWildCard("intermed")),
                                null,
                                wcm.getBlockIdentifier("switchblock"))),
                new BeginBlock(null),
                new StructuredCase(BytecodeLoc.NONE, Collections.<Expression>emptyList(), null, null, wcm.getBlockIdentifier("case")),
                new BeginBlock(null),
                new EndBlock(null),
                new EndBlock(null)
        ));

        EmptySwitchStringMatchResultCollector matchResultCollector = new EmptySwitchStringMatchResultCollector(wcm);
        while (mi.hasNext()) {
            mi.advance();
            matchResultCollector.clear();
            if (m.match(mi, matchResultCollector)) {
                if (!matchResultCollector.matchesCapturedStringObject()) continue;
                StructuredSwitch swtch = (StructuredSwitch) matchResultCollector.getStatementByName(MATCH_HASH_SWITCH);
                if (!swtch.isSafeExpression()) continue;
                nopOutCapturedPrelude(matchResultCollector, MATCH_STRING_OBJECT_ASSIGN, MATCH_INTERMEDIATE_ASSIGN, MATCH_HASH_CALL);
                swtch.getContainer().replaceStatement(buildStringSwitch(swtch, matchResultCollector.getStringExpression()));
                mi.rewind1();
            }
        }

    }

    private void rewriteComplex(List<StructuredStatement> structuredStatements) {
        // Rather than have a non-greedy kleene star at the start, we cheat and scan for valid start points.
        // switch OB (case OB (if-testalternativevalid OB assign break CB)* if-notvalid break assign break CB)+ CB
        MatchIterator<StructuredStatement> mi = new MatchIterator<StructuredStatement>(structuredStatements);

        WildcardMatch wcm1 = new WildcardMatch();
        WildcardMatch wcm2 = new WildcardMatch();
        WildcardMatch wcm3 = new WildcardMatch();

        Matcher<StructuredStatement> m = new ResetAfterTest(wcm1, "r1", new MatchSequence(
                new CollectMatch(MATCH_STRING_OBJECT_ASSIGN, new StructuredAssignment(BytecodeLoc.NONE, wcm1.getLValueWildCard("stringobject"), wcm1.getExpressionWildCard("originalstring"))),
                new CollectMatch(MATCH_INTERMEDIATE_ASSIGN, new StructuredAssignment(BytecodeLoc.NONE, wcm1.getLValueWildCard("intermed"), wcm1.getExpressionWildCard("defaultintermed"))),
                new CollectMatch(MATCH_HASH_SWITCH,
                        new StructuredSwitch(BytecodeLoc.NONE, wcm1.getMemberFunction("switch", "hashCode", wcm1.getExpressionWildCard("stringobjornull")),
                                null,
                                wcm1.getBlockIdentifier("switchblock"))),
                new BeginBlock(null),
                new KleenePlus(
                        new ResetAfterTest(wcm2, "r2",
                                new MatchSequence(
                                        new StructuredCase(BytecodeLoc.NONE, wcm2.getList("hashvals"), null, null, wcm2.getBlockIdentifier("case")),
                                        new BeginBlock(null),
                                        new KleeneStar(
                                                new ResetAfterTest(wcm3,"r3",
                                                        new MatchSequence(
                                                                new StructuredIf(BytecodeLoc.NONE, new BooleanExpression(wcm3.getMemberFunction("collision", "equals", new LValueExpression(wcm1.getLValueWildCard("stringobject")), wcm3.getExpressionWildCard("stringvalue"))), null),
                                                                new BeginBlock(null),
                                                                new CollectMatch(MATCH_HASH_COLLISION_ASSIGN, new StructuredAssignment(BytecodeLoc.NONE, wcm1.getLValueWildCard("intermed"), wcm3.getExpressionWildCard("case2id"))),
                                                                new StructuredBreak(BytecodeLoc.NONE, wcm1.getBlockIdentifier("switchblock"), true),
                                                                new EndBlock(null)
                                                        )
                                                )
                                        ),
                                        new MatchOneOf(
                                                // Either an anticollision at the end
                                                new MatchSequence(
                                                    new StructuredIf(BytecodeLoc.NONE, new NotOperation(BytecodeLoc.NONE, new BooleanExpression(wcm2.getMemberFunction("anticollision", "equals", new LValueExpression(wcm1.getLValueWildCard("stringobject")), wcm2.getExpressionWildCard("stringvalue")))), null),
                                                    new StructuredBreak(BytecodeLoc.NONE, wcm1.getBlockIdentifier("switchblock"), true),
                                                    new CollectMatch(MATCH_CASE_ASSIGN, new StructuredAssignment(BytecodeLoc.NONE, wcm1.getLValueWildCard("intermed"), wcm2.getExpressionWildCard("case2id")))
                                                ),
                                                // or a final collision.
                                                new MatchSequence(
                                                        new StructuredIf(BytecodeLoc.NONE, new BooleanExpression(wcm2.getMemberFunction("collision", "equals", new LValueExpression(wcm1.getLValueWildCard("stringobject")), wcm2.getExpressionWildCard("stringvalue"))), null),
                                                        new BeginBlock(null),
                                                        new CollectMatch(MATCH_CASE_ASSIGN, new StructuredAssignment(BytecodeLoc.NONE, wcm1.getLValueWildCard("intermed"), wcm2.getExpressionWildCard("case2id"))),
                                                        new EndBlock(null)
                                                )
                                        ),
                                        // Strictly speaking wrong, but I want to capture a missing break at the end.
                                        new KleeneStar(new StructuredBreak(BytecodeLoc.NONE, wcm1.getBlockIdentifier("switchblock"), true)),
                                        new EndBlock(null)
                                )
                        )
                ),
                new EndBlock(null),
                // We don't actually CARE what the branches of the switch-on-intermediate are...
                // we just want to make sure that there is one.
                new CollectMatch(MATCH_INDEX_SWITCH, new StructuredSwitch(BytecodeLoc.NONE, new LValueExpression(wcm1.getLValueWildCard("intermed")), null, wcm1.getBlockIdentifier("switchblock2")))
        ));

        SwitchStringMatchResultCollector matchResultCollector = new SwitchStringMatchResultCollector(wcm1, wcm2, wcm3);
        while (mi.hasNext()) {
            mi.advance();
            matchResultCollector.clear();
            if (m.match(mi, matchResultCollector)) {
                if (!matchResultCollector.matchesCapturedStringObject()) continue;
                StructuredSwitch firstSwitch = (StructuredSwitch) matchResultCollector.getStatementByName(MATCH_HASH_SWITCH);
                StructuredSwitch secondSwitch = (StructuredSwitch) matchResultCollector.getStatementByName(MATCH_INDEX_SWITCH);
                if (!secondSwitch.isSafeExpression()) continue;

                StructuredSwitch replacement = rewriteSwitch(secondSwitch, matchResultCollector);
                secondSwitch.getContainer().replaceStatement(replacement);
                firstSwitch.getContainer().nopOut();
                nopOutCapturedPrelude(matchResultCollector, MATCH_STRING_OBJECT_ASSIGN, MATCH_INTERMEDIATE_ASSIGN);
                mi.rewind1();
            }
        }
    }

    private void nopOutCapturedPrelude(AbstractSwitchStringMatchResultCollector collector, String... statementNames) {
        for (String statementName : statementNames) {
            collector.getStatementByName(statementName).getContainer().nopOut();
        }
    }

    private StructuredSwitch buildStringSwitch(StructuredSwitch original, Expression switchOn) {
        return new StructuredSwitch(BytecodeLoc.TODO, switchOn, original.getBody(), original.getBlockIdentifier());
    }

    private StructuredSwitch rewriteSwitch(StructuredSwitch original, SwitchStringMatchResultCollector matchResultCollector) {
        Op04StructuredStatement body = original.getBody();
        BlockIdentifier blockIdentifier = original.getBlockIdentifier();

        StructuredStatement inner = body.getStatement();
        if (!(inner instanceof Block)) {
            throw new FailedRewriteException("Switch body is not a block, is a " + inner.getClass());
        }

        Block block = (Block) inner;

        Map<Integer, List<String>> replacements = matchResultCollector.getValidatedHashes();
        List<Op04StructuredStatement> caseStatements = block.getBlockStatements();
        LinkedList<Op04StructuredStatement> tgt = ListFactory.newLinkedList();

        InferredJavaType typeOfSwitch = matchResultCollector.getStringExpression().getInferredJavaType();
        for (Op04StructuredStatement op04StructuredStatement : caseStatements) {
            inner = op04StructuredStatement.getStatement();
            if (!(inner instanceof StructuredCase)) {
                throw new FailedRewriteException("Block member is not a case, it's a " + inner.getClass());
            }
            StructuredCase structuredCase = (StructuredCase) inner;
            List<Expression> values = structuredCase.getValues();
            List<Expression> transformedValues = ListFactory.newList();

            for (Expression value : values) {
                Integer i = getInt(value);
                List<String> replacementStrings = replacements.get(i);
                if (replacementStrings == null) {
                    throw new FailedRewriteException("No replacements for " + i);
                }
                for (String s : replacementStrings) {
                    transformedValues.add(new Literal(TypedLiteral.getString(s)));
                }
            }

            StructuredCase replacementStructuredCase = new StructuredCase(BytecodeLoc.TODO, transformedValues, typeOfSwitch, structuredCase.getBody(), structuredCase.getBlockIdentifier());
            tgt.add(new Op04StructuredStatement(replacementStructuredCase));
        }
        Block newBlock = new Block(tgt, true);

        Expression switchOn = matchResultCollector.getStringExpression();

        // If the literal is a naughty null, we need to expressly force it to a string.
        // Don't cast to its own type, as this might be a null type.
        if (switchOn.equals(Literal.NULL)) {
            switchOn = new CastExpression(BytecodeLoc.TODO, new InferredJavaType(TypeConstants.STRING, InferredJavaType.Source.EXPRESSION), switchOn, true);
        }

        return new StructuredSwitch(
                BytecodeLoc.TODO,
                switchOn,
                new Op04StructuredStatement(newBlock),
                blockIdentifier, false);
    }

    private static boolean isLVOk(Expression lve, LValue lv, StructuredStatement assign) {
        if (lve instanceof LValueExpression && ((LValueExpression) lve).getLValue().equals(lv)) return true;
        if (!(lve instanceof Literal)) return false;
        if (!(assign instanceof StructuredAssignment)) return false;
        Expression rv = ((StructuredAssignment) assign).getRvalue();
        return rv.equals(lve);
    }

    private static abstract class AbstractSwitchStringMatchResultCollector extends AbstractMatchResultIterator {
        private final Map<String, StructuredStatement> collectedStatements = MapFactory.newMap();
        private Expression stringExpression;
        private Expression stringObjectVerification;
        private LValue stringObjectLValue;

        @Override
        public void clear() {
            collectedStatements.clear();
            stringExpression = null;
            stringObjectVerification = null;
            stringObjectLValue = null;
            clearSpecific();
        }

        @Override
        public void collectStatement(String name, StructuredStatement statement) {
            collectedStatements.put(name, statement);
        }

        protected final void captureWholeSwitchMatch(WildcardMatch wcm) {
            stringExpression = wcm.getExpressionWildCard("originalstring").getMatch();
            stringObjectVerification = wcm.getExpressionWildCard("stringobjornull").getMatch();
            stringObjectLValue = wcm.getLValueWildCard("stringobject").getMatch();
        }

        boolean matchesCapturedStringObject() {
            return isLVOk(stringObjectVerification, stringObjectLValue, getStatementByName(MATCH_STRING_OBJECT_ASSIGN));
        }

        Expression getStringExpression() {
            return stringExpression;
        }

        StructuredStatement getStatementByName(String name) {
            StructuredStatement structuredStatement = collectedStatements.get(name);
            if (structuredStatement == null) throw new IllegalArgumentException("No collected statement " + name);
            return structuredStatement;
        }

        protected abstract void clearSpecific();
    }

    private static class EmptySwitchStringMatchResultCollector extends AbstractSwitchStringMatchResultCollector {
        private final WildcardMatch wcm;

        EmptySwitchStringMatchResultCollector(WildcardMatch wcm) {
            this.wcm = wcm;
        }

        @Override
        protected void clearSpecific() {
        }

        @Override
        public void collectMatches(String name, WildcardMatch wcm) {
            if (wcm == this.wcm) {
                captureWholeSwitchMatch(wcm);
            }
        }
    }

    private static class SwitchStringMatchResultCollector extends AbstractSwitchStringMatchResultCollector {

        private final WildcardMatch wholeBlock;
        private final WildcardMatch caseStatement;
        private final WildcardMatch hashCollision; // inner collision protection

        private final List<CaseId> pendingCaseIds = ListFactory.newList();
        private final Map<Integer, List<String>> validatedHashes = MapFactory.newMap();


        private SwitchStringMatchResultCollector(WildcardMatch wholeBlock, WildcardMatch caseStatement, WildcardMatch hashCollision) {
            this.wholeBlock = wholeBlock;
            this.caseStatement = caseStatement;
            this.hashCollision = hashCollision;
        }

        @Override
        protected void clearSpecific() {
            pendingCaseIds.clear();
            validatedHashes.clear();
        }

        @Override
        public void collectMatches(String name, WildcardMatch wcm) {
            if (wcm == wholeBlock) {
                captureWholeSwitchMatch(wcm);
            } else if (wcm == caseStatement) {
                pendingCaseIds.add(readCaseId(wcm));
                flushPendingCaseIds();
            } else if (wcm == hashCollision) {
                // Note that this will be triggered BEFORE the case statement it's in.
                pendingCaseIds.add(readCaseId(wcm));
            } else {
                throw new IllegalStateException();
            }
        }

        private CaseId readCaseId(WildcardMatch wcm) {
            Expression case2id = wcm.getExpressionWildCard("case2id").getMatch();
            Expression stringValue = wcm.getExpressionWildCard("stringvalue").getMatch();
            return new CaseId(getString(stringValue), getInt(case2id));
        }

        void flushPendingCaseIds() {
            for (CaseId caseId : pendingCaseIds) {
                validatedHashes.computeIfAbsent(caseId.caseIndex, ignored -> ListFactory.newList()).add(caseId.value);
            }
            pendingCaseIds.clear();
        }

        Map<Integer, List<String>> getValidatedHashes() {
            return validatedHashes;
        }
    }

    private static class CaseId {
        private final String value;
        private final Integer caseIndex;

        private CaseId(String value, Integer caseIndex) {
            this.value = value;
            this.caseIndex = caseIndex;
        }
    }

    private static String getString(Expression e) {
        if (!(e instanceof Literal)) {
            throw new TooOptimisticMatchException();
        }
        Literal l = (Literal) e;
        TypedLiteral typedLiteral = l.getValue();
        if (typedLiteral.getType() != TypedLiteral.LiteralType.String) {
            throw new TooOptimisticMatchException();
        }
        return (String) typedLiteral.getValue();
    }

    // TODO : Verify type
    private static Integer getInt(Expression e) {
        if (!(e instanceof Literal)) {
            throw new TooOptimisticMatchException();
        }
        Literal l = (Literal) e;
        TypedLiteral typedLiteral = l.getValue();
        if (typedLiteral.getType() != TypedLiteral.LiteralType.Integer) {
            throw new TooOptimisticMatchException();
        }
        return (Integer) typedLiteral.getValue();
    }

    private static class TooOptimisticMatchException extends IllegalStateException {
    }

    private static class FailedRewriteException extends IllegalStateException {
        FailedRewriteException(String s) {
            super(s);
        }
    }

}
