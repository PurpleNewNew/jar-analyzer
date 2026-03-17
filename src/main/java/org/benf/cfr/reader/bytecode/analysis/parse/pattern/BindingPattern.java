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
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.output.Dumper;

import java.util.List;

public class BindingPattern implements MatchPattern {
    private final JavaTypeInstance typeInstance;
    private final LValue binding;

    public BindingPattern(JavaTypeInstance typeInstance, LValue binding) {
        this.typeInstance = typeInstance;
        this.binding = binding;
    }

    public JavaTypeInstance getTypeInstance() {
        return typeInstance;
    }

    public LValue getBinding() {
        return binding;
    }

    @Override
    public InferredJavaType getInferredJavaType() {
        return binding.getInferredJavaType();
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return BytecodeLoc.NONE;
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        collector.collect(typeInstance);
        binding.collectTypeUsages(collector);
    }

    @Override
    public MatchPattern deepClone(CloneHelper cloneHelper) {
        return new BindingPattern(typeInstance, cloneHelper.replaceOrClone(binding));
    }

    @Override
    public Dumper dump(Dumper d) {
        d.dump(typeInstance).print(" ");
        binding.dump(d, true);
        return d;
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
        return ListFactory.newImmutableList(binding);
    }

    @Override
    public boolean matches(LValue lValue) {
        return binding.equals(lValue);
    }

    @Override
    public boolean equivalentUnder(MatchPattern other, EquivalenceConstraint constraint) {
        if (!(other instanceof BindingPattern)) return false;
        BindingPattern bindingPattern = (BindingPattern) other;
        return constraint.equivalent(typeInstance, bindingPattern.typeInstance)
                && constraint.equivalent(binding, bindingPattern.binding);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof BindingPattern)) return false;
        BindingPattern other = (BindingPattern) o;
        return typeInstance.equals(other.typeInstance) && binding.equals(other.binding);
    }
}
