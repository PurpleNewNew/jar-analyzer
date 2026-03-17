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
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.util.StringUtils;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.output.Dumper;

import java.util.List;

public class RecordSwitchPatternCaseLabel extends AbstractExpression implements CaseLabelExpression {
    private final JavaTypeInstance recordType;
    private final List<LValue> components;
    private Expression guard;

    public RecordSwitchPatternCaseLabel(BytecodeLoc loc,
                                        JavaTypeInstance recordType,
                                        List<LValue> components,
                                        Expression guard) {
        super(loc, components.get(0).getInferredJavaType());
        this.recordType = recordType;
        this.components = ListFactory.newList(components);
        this.guard = guard;
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return BytecodeLoc.combine(this, guard);
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        collector.collect(recordType);
        for (LValue component : components) {
            component.collectTypeUsages(collector);
        }
        collector.collectFrom(guard);
    }

    @Override
    public Expression deepClone(CloneHelper cloneHelper) {
        return new RecordSwitchPatternCaseLabel(getLoc(), recordType, components, cloneHelper.replaceOrClone(guard));
    }

    @Override
    public Precedence getPrecedence() {
        return Precedence.HIGHEST;
    }

    @Override
    public Dumper dumpInner(Dumper d) {
        d.dump(recordType).separator("(");
        boolean first = true;
        for (LValue component : components) {
            first = StringUtils.comma(first, d);
            LValue.Creation.dump(d, component);
        }
        d.separator(")");
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
        if (!(o instanceof RecordSwitchPatternCaseLabel)) return false;
        RecordSwitchPatternCaseLabel other = (RecordSwitchPatternCaseLabel) o;
        if (!recordType.equals(other.recordType)) return false;
        if (!components.equals(other.components)) return false;
        if (guard == null) return other.guard == null;
        return guard.equals(other.guard);
    }

    @Override
    public boolean equivalentUnder(Object o, EquivalenceConstraint constraint) {
        if (o == this) return true;
        if (!(o instanceof RecordSwitchPatternCaseLabel)) return false;
        RecordSwitchPatternCaseLabel other = (RecordSwitchPatternCaseLabel) o;
        if (!constraint.equivalent(recordType, other.recordType)) return false;
        if (!constraint.equivalent(components, other.components)) return false;
        return constraint.equivalent(guard, other.guard);
    }

    @Override
    public List<LValue> getCreatedLValues() {
        return components;
    }

    @Override
    public boolean matches(LValue lValue) {
        return components.contains(lValue);
    }
}
