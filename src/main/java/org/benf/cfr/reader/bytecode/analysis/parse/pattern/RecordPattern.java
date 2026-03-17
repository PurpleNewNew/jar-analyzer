package org.benf.cfr.reader.bytecode.analysis.parse.pattern;

import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.CloneHelper;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.EquivalenceConstraint;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueUsageCollector;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.util.StringUtils;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.output.Dumper;

import java.util.List;

public class RecordPattern implements MatchPattern {
    private final JavaTypeInstance recordType;
    private final List<LValue> components;

    public RecordPattern(JavaTypeInstance recordType, List<LValue> components) {
        this.recordType = recordType;
        this.components = ListFactory.newList(components);
    }

    public JavaTypeInstance getRecordType() {
        return recordType;
    }

    public List<LValue> getComponents() {
        return components;
    }

    @Override
    public InferredJavaType getInferredJavaType() {
        return components.isEmpty()
                ? new InferredJavaType(recordType, InferredJavaType.Source.EXPRESSION)
                : components.get(0).getInferredJavaType();
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return BytecodeLoc.NONE;
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        collector.collect(recordType);
        for (LValue component : components) {
            component.collectTypeUsages(collector);
        }
    }

    @Override
    public MatchPattern deepClone(CloneHelper cloneHelper) {
        List<LValue> cloned = ListFactory.newList();
        for (LValue component : components) {
            cloned.add(cloneHelper.replaceOrClone(component));
        }
        return new RecordPattern(recordType, cloned);
    }

    @Override
    public Dumper dump(Dumper d) {
        d.dump(recordType).separator("(");
        boolean first = true;
        for (LValue component : components) {
            first = StringUtils.comma(first, d);
            LValue.Creation.dump(d, component);
        }
        return d.separator(")");
    }

    @Override
    public MatchPattern replaceSingleUsageLValues(LValueRewriter lValueRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer) {
        return this;
    }

    @Override
    public MatchPattern applyExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        return this;
    }

    @Override
    public void collectUsedLValues(LValueUsageCollector lValueUsageCollector) {
    }

    @Override
    public List<LValue> getCreatedLValues() {
        return components;
    }

    @Override
    public boolean matches(LValue lValue) {
        return components.contains(lValue);
    }

    @Override
    public boolean equivalentUnder(MatchPattern other, EquivalenceConstraint constraint) {
        if (!(other instanceof RecordPattern)) return false;
        RecordPattern recordPattern = (RecordPattern) other;
        return constraint.equivalent(recordType, recordPattern.recordType)
                && constraint.equivalent(components, recordPattern.components);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof RecordPattern)) return false;
        RecordPattern other = (RecordPattern) o;
        return recordType.equals(other.recordType) && components.equals(other.components);
    }
}
