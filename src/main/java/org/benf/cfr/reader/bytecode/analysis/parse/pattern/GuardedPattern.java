package org.benf.cfr.reader.bytecode.analysis.parse.pattern;

import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.CloneHelper;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.EquivalenceConstraint;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueUsageCollector;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.util.output.Dumper;

import java.util.List;

public class GuardedPattern implements MatchPattern {
    private final MatchPattern inner;
    private Expression guard;

    public GuardedPattern(MatchPattern inner, Expression guard) {
        this.inner = inner;
        this.guard = guard;
    }

    public MatchPattern getInner() {
        return inner;
    }

    public Expression getGuard() {
        return guard;
    }

    @Override
    public InferredJavaType getInferredJavaType() {
        return inner.getInferredJavaType();
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return guard == null ? inner.getCombinedLoc() : BytecodeLoc.combine(guard);
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        inner.collectTypeUsages(collector);
        collector.collectFrom(guard);
    }

    @Override
    public MatchPattern deepClone(CloneHelper cloneHelper) {
        return new GuardedPattern(inner.deepClone(cloneHelper), cloneHelper.replaceOrClone(guard));
    }

    @Override
    public Dumper dump(Dumper d) {
        inner.dump(d);
        if (guard != null) {
            d.keyword(" when ").dump(guard);
        }
        return d;
    }

    @Override
    public MatchPattern replaceSingleUsageLValues(LValueRewriter lValueRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer) {
        if (guard != null) {
            guard = guard.replaceSingleUsageLValues(lValueRewriter, ssaIdentifiers, statementContainer);
        }
        inner.replaceSingleUsageLValues(lValueRewriter, ssaIdentifiers, statementContainer);
        return this;
    }

    @Override
    public MatchPattern applyExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        if (guard != null) {
            guard = expressionRewriter.rewriteExpression(guard, ssaIdentifiers, statementContainer, flags);
        }
        inner.applyExpressionRewriter(expressionRewriter, ssaIdentifiers, statementContainer, flags);
        return this;
    }

    @Override
    public void collectUsedLValues(LValueUsageCollector lValueUsageCollector) {
        inner.collectUsedLValues(lValueUsageCollector);
        if (guard != null) {
            guard.collectUsedLValues(lValueUsageCollector);
        }
    }

    @Override
    public List<LValue> getCreatedLValues() {
        return inner.getCreatedLValues();
    }

    @Override
    public boolean matches(LValue lValue) {
        return inner.matches(lValue);
    }

    @Override
    public boolean equivalentUnder(MatchPattern other, EquivalenceConstraint constraint) {
        if (!(other instanceof GuardedPattern)) return false;
        GuardedPattern guardedPattern = (GuardedPattern) other;
        return inner.equivalentUnder(guardedPattern.inner, constraint)
                && constraint.equivalent(guard, guardedPattern.guard);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof GuardedPattern)) return false;
        GuardedPattern other = (GuardedPattern) o;
        if (!inner.equals(other.inner)) return false;
        if (guard == null) return other.guard == null;
        return guard.equals(other.guard);
    }
}
