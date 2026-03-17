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
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.output.Dumper;

import java.util.List;

public class SwitchPatternCaseLabel extends AbstractExpression implements CaseLabelExpression {
    private final LValue binding;
    private Expression guard;

    public SwitchPatternCaseLabel(BytecodeLoc loc, LValue binding, Expression guard) {
        super(loc, binding.getInferredJavaType());
        this.binding = binding;
        this.guard = guard;
    }

    public LValue getBinding() {
        return binding;
    }

    public Expression getGuard() {
        return guard;
    }

    public boolean matches(LValue lValue) {
        return binding.equals(lValue);
    }

    @Override
    public List<LValue> getCreatedLValues() {
        return ListFactory.<LValue>newImmutableList(binding);
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return BytecodeLoc.combine(this, guard);
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        binding.collectTypeUsages(collector);
        collector.collectFrom(guard);
    }

    @Override
    public Expression deepClone(CloneHelper cloneHelper) {
        return new SwitchPatternCaseLabel(getLoc(), binding, cloneHelper.replaceOrClone(guard));
    }

    @Override
    public Precedence getPrecedence() {
        return Precedence.HIGHEST;
    }

    @Override
    public Dumper dumpInner(Dumper d) {
        LValue.Creation.dump(d, binding);
        if (guard != null) {
            d.keyword(" when ").dump(guard);
        }
        return d;
    }

    @Override
    public Expression replaceSingleUsageLValues(LValueRewriter lValueRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer) {
        if (guard != null) {
            guard = guard.replaceSingleUsageLValues(lValueRewriter, ssaIdentifiers, statementContainer);
        }
        return this;
    }

    @Override
    public Expression applyExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        if (guard != null) {
            guard = expressionRewriter.rewriteExpression(guard, ssaIdentifiers, statementContainer, flags);
        }
        return this;
    }

    @Override
    public Expression applyReverseExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        return applyExpressionRewriter(expressionRewriter, ssaIdentifiers, statementContainer, flags);
    }

    @Override
    public void collectUsedLValues(LValueUsageCollector lValueUsageCollector) {
        if (guard != null) {
            guard.collectUsedLValues(lValueUsageCollector);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof SwitchPatternCaseLabel)) return false;
        SwitchPatternCaseLabel other = (SwitchPatternCaseLabel) o;
        if (!binding.equals(other.binding)) return false;
        if (guard == null) return other.guard == null;
        return guard.equals(other.guard);
    }

    @Override
    public boolean equivalentUnder(Object o, EquivalenceConstraint constraint) {
        if (o == this) return true;
        if (!(o instanceof SwitchPatternCaseLabel)) return false;
        SwitchPatternCaseLabel other = (SwitchPatternCaseLabel) o;
        if (!constraint.equivalent(binding, other.binding)) return false;
        return constraint.equivalent(guard, other.guard);
    }
}
