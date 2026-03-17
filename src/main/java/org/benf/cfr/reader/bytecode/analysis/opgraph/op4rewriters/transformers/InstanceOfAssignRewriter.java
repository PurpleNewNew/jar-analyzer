package org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers;

import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.AssignmentExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.BoolOp;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.BooleanExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.BooleanOperation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.CastExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.CompOp;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ComparisonOperation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ConditionalExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.InstanceOfExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.InstanceOfExpressionDefining;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.LValueExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.NotOperation;
import org.benf.cfr.reader.bytecode.analysis.parse.pattern.PatternFactory;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.AbstractExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.bytecode.analysis.parse.wildcard.WildcardMatch;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.RawJavaType;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.util.collections.ListFactory;

import java.util.List;

public class InstanceOfAssignRewriter {
    private static final InferredJavaType ijtBool = new InferredJavaType(RawJavaType.BOOLEAN, InferredJavaType.Source.EXPRESSION);

    private final WildcardMatch wcm = new WildcardMatch();
    private final LValue scopedEntity;
    private final WildcardMatch.LValueWildcard objWildcard;
    private final WildcardMatch.LValueWildcard tmpWildcard;
    private final List<ConditionTest> tests;

    public static boolean hasInstanceOf(ConditionalExpression conditionalExpression) {
        InstanceOfSearch search = new InstanceOfSearch();
        search.rewriteExpression(conditionalExpression, null, null, null);
        return search.found;
    }

    private static class InstanceOfSearch extends AbstractExpressionRewriter {
        private boolean found = false;

        @Override
        public Expression rewriteExpression(Expression expression, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
            if (found) return expression;
            if (expression instanceof InstanceOfExpression) {
                found = true;
                return expression;
            }
            return super.rewriteExpression(expression, ssaIdentifiers, statementContainer, flags);
        }

        @Override
        public ConditionalExpression rewriteExpression(ConditionalExpression expression, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
            if (found) return expression;
            return super.rewriteExpression(expression, ssaIdentifiers, statementContainer, flags);
        }
    }

    private enum MatchType {
        SIMPLE,
        ASSIGN_SIMPLE
    }

    private static class GuardedMatch {
        final InstanceOfExpression instanceOf;
        final ConditionalExpression guard;

        private GuardedMatch(InstanceOfExpression instanceOf, ConditionalExpression guard) {
            this.instanceOf = instanceOf;
            this.guard = guard;
        }
    }

    private static class ConditionTest {
        final ConditionalExpression expression;
        final boolean isPositive;
        final MatchType matchType;

        ConditionTest(ConditionalExpression ct, boolean isPositive, MatchType matchType) {
            this.expression = ct;
            this.isPositive = isPositive;
            this.matchType = matchType;
        }
    }

    // a && b
    // matches (a && b) && c
    // or c && (a && b)
    // but how do we handle (c && a) && b?
    public InstanceOfAssignRewriter(LValue scopedEntity) {

        JavaTypeInstance target = scopedEntity.getInferredJavaType().getJavaTypeInstance();
        this.scopedEntity = scopedEntity;
        InferredJavaType ijtTarget = new InferredJavaType(target, InferredJavaType.Source.EXPRESSION);
        objWildcard = wcm.getLValueWildCard("obj");
        tmpWildcard = wcm.getLValueWildCard("tmp");
        LValueExpression obj = new LValueExpression(objWildcard);
        CastExpression castObj = new CastExpression(BytecodeLoc.TODO, ijtTarget, obj);

        // Simple conditional tests.
        // a instanceof Foo && (x = (Foo)a) == a
        // --> a instanceof Foo x
        tests = ListFactory.newList();
        ConditionalExpression cPos1 = new BooleanOperation(BytecodeLoc.NONE,
                new BooleanExpression(new InstanceOfExpression(BytecodeLoc.NONE, ijtBool, obj, target)),
                new ComparisonOperation(BytecodeLoc.NONE, new AssignmentExpression(BytecodeLoc.NONE, scopedEntity, castObj), castObj, CompOp.EQ),
                BoolOp.AND
        );
        ConditionalExpression cPos2 = new NotOperation(BytecodeLoc.NONE, cPos1.getDemorganApplied(true));
        tests.add(new ConditionTest(cPos1, true, MatchType.SIMPLE));
        tests.add(new ConditionTest(cPos2, true, MatchType.SIMPLE));
        tests.add(new ConditionTest(cPos1.getNegated(), false, MatchType.SIMPLE));
        tests.add(new ConditionTest(cPos2.getNegated(), false, MatchType.SIMPLE));

        // Assignment conditional tests.
        // (a = y) instanceOf Foo && (x = (Foo)a) == a
        // --> (a = y) instanceof Foo x
        // The inline assignment is ALMOST CERTAINLY pointless, but unless we can prove it's not used
        // anywhere, we need to retain it. (the existence of 'a' is an annoying JDK artifact).
        CastExpression castTmp = new CastExpression(BytecodeLoc.NONE, ijtTarget, new LValueExpression(tmpWildcard));

        ConditionalExpression dPos1 = new BooleanOperation(BytecodeLoc.NONE,
                new BooleanExpression(new InstanceOfExpression(BytecodeLoc.NONE, ijtBool, new AssignmentExpression(BytecodeLoc.NONE, tmpWildcard, obj), target)),
                new ComparisonOperation(BytecodeLoc.NONE, new AssignmentExpression(BytecodeLoc.NONE, scopedEntity, castTmp), castTmp, CompOp.EQ),
                BoolOp.AND
        );
        ConditionalExpression dPos2 = new NotOperation(BytecodeLoc.NONE, cPos1.getDemorganApplied(true));
        tests.add(new ConditionTest(dPos1, true, MatchType.ASSIGN_SIMPLE));
        tests.add(new ConditionTest(dPos2, true, MatchType.ASSIGN_SIMPLE));
        tests.add(new ConditionTest(dPos1.getNegated(), false, MatchType.ASSIGN_SIMPLE));
        tests.add(new ConditionTest(dPos2.getNegated(), false, MatchType.ASSIGN_SIMPLE));
    }

    private ConditionTest getMatchingTest(ConditionalExpression ce) {
        for (ConditionTest ct : tests) {
            wcm.reset();
            if (ct.expression.equals(ce)) {
                return ct;
            }
        }
        return null;
    }

    public boolean isMatchFor(ConditionalExpression ce) {
        RewriteFinder rewriteFinder = new RewriteFinder();
        rewriteFinder.rewriteExpression(ce, null, null, null);
        return rewriteFinder.found;
    }

    private class RewriteFinder extends AbstractExpressionRewriter {
        private boolean found = false;
        @Override
        public ConditionalExpression rewriteExpression(ConditionalExpression expression, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
            if (found) return expression;
            if (getMatchingTest(expression) != null || getGuardedMatch(expression, false) != null) {
                found = true;
                return expression;
            }
            return super.rewriteExpression(expression, ssaIdentifiers, statementContainer, flags);
        }

        @Override
        public Expression rewriteExpression(Expression expression, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
            if (found) return expression;
            return super.rewriteExpression(expression, ssaIdentifiers, statementContainer, flags);
        }
    }

    private class Rewriter extends AbstractExpressionRewriter {
        @Override
        public ConditionalExpression rewriteExpression(ConditionalExpression expression, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
            expression = rewriteInner(expression);
            return super.rewriteExpression(expression, ssaIdentifiers, statementContainer, flags);
        }
    }

    public ConditionalExpression rewriteDefining(ConditionalExpression ce) {
        return new Rewriter().rewriteExpression(ce, null, null, null);
    }

    private GuardedMatch getGuardedMatch(ConditionalExpression ce, boolean rewrite) {
        if (!(ce instanceof BooleanOperation)) {
            return null;
        }
        BooleanOperation booleanOperation = (BooleanOperation) ce;
        if (booleanOperation.getOp() != BoolOp.AND) {
            return null;
        }
        ConditionalExpression lhs = booleanOperation.getLhs();
        if (!(lhs instanceof BooleanExpression)) {
            return null;
        }
        Expression inner = ((BooleanExpression) lhs).getInner();
        if (!(inner instanceof InstanceOfExpression)) {
            return null;
        }
        InstanceOfExpression instanceOf = (InstanceOfExpression) inner;
        if (!scopedEntity.getInferredJavaType().getJavaTypeInstance().equals(instanceOf.getTypeInstance())) {
            return null;
        }
        GuardedPatternUseRewriter guardedPatternUseRewriter =
                new GuardedPatternUseRewriter(instanceOf.getLhs(), instanceOf.getTypeInstance(), rewrite);
        ConditionalExpression rewrittenGuard =
                guardedPatternUseRewriter.rewriteExpression(booleanOperation.getRhs(), null, null, null);
        if (!guardedPatternUseRewriter.isMatched()) {
            return null;
        }
        return new GuardedMatch(instanceOf, rewrittenGuard);
    }

    private ConditionalExpression rewriteInner(ConditionalExpression ce) {
        ConditionTest ct = getMatchingTest(ce);
        if (ct != null) {
            if (ct.matchType == MatchType.SIMPLE) {
                LValue obj = objWildcard.getMatch();
                ce = new BooleanExpression(PatternFactory.defining(
                        new LValueExpression(obj),
                        scopedEntity.getInferredJavaType().getJavaTypeInstance(),
                        scopedEntity
                ));
            } else {
                LValue obj = objWildcard.getMatch();
                LValue tmp = tmpWildcard.getMatch();
                ce = new BooleanExpression(PatternFactory.defining(
                        new AssignmentExpression(BytecodeLoc.TODO, tmp, new LValueExpression(BytecodeLoc.TODO, obj)),
                        scopedEntity.getInferredJavaType().getJavaTypeInstance(),
                        scopedEntity
                ));
            }
            if (!ct.isPositive) {
                ce = ce.getNegated();
            }
            return ce;
        }
        GuardedMatch guardedMatch = getGuardedMatch(ce, true);
        if (guardedMatch == null) {
            return ce;
        }
        return new BooleanOperation(ce.getLoc(),
                new BooleanExpression(PatternFactory.defining(
                        guardedMatch.instanceOf.getLhs(),
                        guardedMatch.instanceOf.getTypeInstance(),
                        scopedEntity
                )),
                guardedMatch.guard,
                BoolOp.AND);
    }

    private class GuardedPatternUseRewriter extends AbstractExpressionRewriter {
        private final Expression matchedObject;
        private final JavaTypeInstance matchedType;
        private final boolean rewrite;
        private boolean matched;

        private GuardedPatternUseRewriter(Expression matchedObject, JavaTypeInstance matchedType, boolean rewrite) {
            this.matchedObject = matchedObject;
            this.matchedType = matchedType;
            this.rewrite = rewrite;
        }

        private boolean isMatchedCast(Expression expression) {
            expression = CastExpression.removeImplicit(expression);
            if (!(expression instanceof CastExpression)) {
                return false;
            }
            CastExpression castExpression = (CastExpression) expression;
            return matchedType.equals(castExpression.getInferredJavaType().getJavaTypeInstance()) &&
                    matchedObject.equals(castExpression.getChild());
        }

        boolean isMatched() {
            return matched;
        }

        @Override
        public Expression rewriteExpression(Expression expression, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
            if (expression instanceof AssignmentExpression) {
                AssignmentExpression assignmentExpression = (AssignmentExpression) expression;
                if (scopedEntity.equals(assignmentExpression.getlValue()) && isMatchedCast(assignmentExpression.getrValue())) {
                    matched = true;
                    if (rewrite) {
                        return new LValueExpression(BytecodeLoc.TODO, scopedEntity);
                    }
                    return expression;
                }
            }
            if (isMatchedCast(expression)) {
                matched = true;
                if (rewrite) {
                    return new LValueExpression(BytecodeLoc.TODO, scopedEntity);
                }
                return expression;
            }
            return super.rewriteExpression(expression, ssaIdentifiers, statementContainer, flags);
        }
    }
}
