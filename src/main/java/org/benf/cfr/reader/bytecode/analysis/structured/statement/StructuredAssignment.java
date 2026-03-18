package org.benf.cfr.reader.bytecode.analysis.structured.statement;

import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.PrimitiveBoxingRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchIterator;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchResultCollector;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.AssignmentExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ExpressionTypeHintHelper;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.rewriteinterface.BoxingProcessor;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.LocalVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueUsageCollectorSimple;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.scope.LValueScopeDiscoverer;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredScope;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.StructuredStatementTransformer;
import org.benf.cfr.reader.bytecode.analysis.types.BindingSuperContainer;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaGenericBaseInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaGenericPlaceholderTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.RawJavaType;
import org.benf.cfr.reader.bytecode.analysis.types.TypeConstants;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.output.Dumper;

import java.util.List;

public class StructuredAssignment extends AbstractStructuredStatement implements BoxingProcessor {

    private LValue lvalue;
    private Expression rvalue;
    private boolean creator;

    public StructuredAssignment(BytecodeLoc loc, LValue lvalue, Expression rvalue) {
        super(loc);
        this.lvalue = lvalue;
        this.rvalue = rvalue;
        this.creator = false;
    }

    public StructuredAssignment(BytecodeLoc loc, LValue lvalue, Expression rvalue, boolean creator) {
        super(loc);
        this.lvalue = lvalue;
        this.rvalue = rvalue;
        this.creator = creator;
    }

    @Override
    public BytecodeLoc getCombinedLoc() {
        return BytecodeLoc.combine(this, rvalue);
    }

    public boolean isCreator(LValue lvalue) {
        return creator && this.lvalue.equals(lvalue) && !isSelfReferentialCreator();
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        lvalue.collectTypeUsages(collector);
        collector.collectFrom(rvalue);
    }

    @Override
    public Dumper dump(Dumper dumper) {
        harmonizeAssignedLocalTypeFromRvalue();
        harmonizeCreatorTypeFromRvalue();
        harmonizeCreatorTypeFromAssignmentTarget();
        if (shouldInlineSyntheticCreator()) {
            AssignmentExpression assignmentExpression = (AssignmentExpression) rvalue;
            ExpressionTypeHintHelper.improveExpressionType(
                    rvalue,
                    assignmentExpression.getUpdatedLValue().getInferredJavaType().getJavaTypeInstance()
            );
            dumper.dump(rvalue).endCodeln();
            return dumper;
        }
        if (isEffectiveCreator()) {
            if (lvalue.isFinal()) dumper.print("final ");
            LValue.Creation.dump(dumper, lvalue);
        } else {
            dumper.dump(lvalue);
        }
        ExpressionTypeHintHelper.improveExpressionType(rvalue, lvalue.getInferredJavaType().getJavaTypeInstance());
        dumper.operator(" = ").dump(rvalue).endCodeln();
        return dumper;
    }

    private void harmonizeCreatorTypeFromRvalue() {
        if (!isEffectiveCreator()) {
            return;
        }
        harmonizeAssignedLocalTypeFromRvalue();
    }

    private void harmonizeAssignedLocalTypeFromRvalue() {
        if (!(lvalue instanceof LocalVariable)) {
            return;
        }
        JavaTypeInstance displayType = ExpressionTypeHintHelper.getDisplayType(rvalue);
        if (!ExpressionTypeHintHelper.canDefineLocalType(displayType)) {
            return;
        }
        LocalVariable localVariable = (LocalVariable) lvalue;
        JavaTypeInstance currentType = localVariable.getInferredJavaType().getJavaTypeInstance();
        if (!shouldPreferCreatorType(currentType, displayType)) {
            return;
        }
        localVariable.getInferredJavaType().forceType(displayType, true);
        if (creator) {
            localVariable.setCustomCreationJavaType(displayType);
            if (localVariable.getAnnotatedCreationType() == null) {
                localVariable.setCustomCreationType(displayType.getAnnotatedInstance());
            }
        }
    }

    private boolean shouldPreferCreatorType(JavaTypeInstance currentType, JavaTypeInstance candidateType) {
        if (candidateType == null || candidateType == RawJavaType.VOID) {
            return false;
        }
        if (currentType == null) {
            return true;
        }
        if (currentType.equals(candidateType)) {
            return false;
        }
        if (currentType == TypeConstants.OBJECT) {
            return true;
        }
        JavaTypeInstance currentBaseType = currentType.getDeGenerifiedType();
        JavaTypeInstance candidateBaseType = candidateType.getDeGenerifiedType();
        BindingSuperContainer candidateSupers = candidateType.getBindingSupers();
        if (candidateSupers != null && candidateSupers.containsBase(currentBaseType)) {
            return false;
        }
        BindingSuperContainer currentSupers = currentType.getBindingSupers();
        if (currentSupers != null && currentSupers.containsBase(candidateBaseType)) {
            if (candidateBaseType == TypeConstants.OBJECT) {
                return false;
            }
            if (ExpressionTypeHintHelper.isSpecific(currentType)
                    && !ExpressionTypeHintHelper.isSpecific(candidateType)) {
                return false;
            }
            return true;
        }
        if (currentType instanceof JavaGenericBaseInstance
                && ((JavaGenericBaseInstance) currentType).hasUnbound()) {
            return !currentBaseType.equals(candidateBaseType)
                    || candidateType instanceof JavaGenericPlaceholderTypeInstance;
        }
        return !currentBaseType.equals(candidateBaseType);
    }

    private void harmonizeCreatorTypeFromAssignmentTarget() {
        if (!isEffectiveCreator()) {
            return;
        }
        if (!(lvalue instanceof LocalVariable)) {
            return;
        }
        if (!(rvalue instanceof AssignmentExpression)) {
            return;
        }
        LValue updatedLValue = ((AssignmentExpression) rvalue).getUpdatedLValue();
        if (!(updatedLValue instanceof LocalVariable)) {
            return;
        }
        JavaTypeInstance updatedType = updatedLValue.getInferredJavaType().getJavaTypeInstance();
        if (updatedType == null) {
            return;
        }
        LocalVariable creatorVariable = (LocalVariable) lvalue;
        creatorVariable.getInferredJavaType().forceType(updatedType, true);
        creatorVariable.setCustomCreationJavaType(updatedType);
        if (creatorVariable.getAnnotatedCreationType() == null) {
            creatorVariable.setCustomCreationType(updatedType.getAnnotatedInstance());
        }
    }

    private boolean shouldInlineSyntheticCreator() {
        if (!isEffectiveCreator()) {
            return false;
        }
        if (!(lvalue instanceof LocalVariable)) {
            return false;
        }
        if (!(rvalue instanceof AssignmentExpression)) {
            return false;
        }
        LocalVariable creatorVariable = (LocalVariable) lvalue;
        if (creatorVariable.getName().isGoodName()) {
            return false;
        }
        LValue updatedLValue = ((AssignmentExpression) rvalue).getUpdatedLValue();
        if (!(updatedLValue instanceof LocalVariable)) {
            return false;
        }
        return ((LocalVariable) updatedLValue).getName().isGoodName();
    }

    @Override
    public void transformStructuredChildren(StructuredStatementTransformer transformer, StructuredScope scope) {
    }

    @Override
    public void linearizeInto(List<StructuredStatement> out) {
        out.add(this);
    }

    @Override
    public void traceLocalVariableScope(LValueScopeDiscoverer scopeDiscoverer) {
        new EmbeddedAssignmentScopeCollector(getContainer(), scopeDiscoverer).collect(rvalue);
        rvalue.collectUsedLValues(scopeDiscoverer);
        // todo - what if rvalue is an assignment?
        lvalue.collectLValueAssignments(rvalue, getContainer(), scopeDiscoverer);
    }

    @Override
    public void markCreator(LValue scopedEntity, StatementContainer<StructuredStatement> hint) {

        if (scopedEntity instanceof LocalVariable) {
            LocalVariable localVariable = (LocalVariable) scopedEntity;
            if (!localVariable.equals(lvalue)) {
                throw new IllegalArgumentException("Being asked to mark creator for wrong variable");
            }
            creator = true;
            InferredJavaType inferredJavaType = localVariable.getInferredJavaType();
            if (inferredJavaType.isClash()) {
                inferredJavaType.collapseTypeClash();
            }
        }
    }

    @Override
    public List<LValue> findCreatedHere() {
        if (isEffectiveCreator()) {
            return ListFactory.newImmutableList(lvalue);
        } else {
            return null;
        }
    }

    private boolean isEffectiveCreator() {
        return isCreator(lvalue);
    }

    private boolean isSelfReferentialCreator() {
        if (!creator) {
            return false;
        }
        if (!(lvalue instanceof LocalVariable)) {
            return false;
        }
        LocalVariable localVariable = (LocalVariable) lvalue;
        LValueUsageCollectorSimple collector = new LValueUsageCollectorSimple();
        rvalue.collectUsedLValues(collector);
        for (LValue used : collector.getUsedLValues()) {
            if (!(used instanceof LocalVariable)) {
                continue;
            }
            LocalVariable usedLocal = (LocalVariable) used;
            if (localVariable.matchesReadableAlias(usedLocal)) {
                return true;
            }
            if (localVariable.getName() == null
                    || usedLocal.getName() == null
                    || !localVariable.getName().isGoodName()
                    || !usedLocal.getName().isGoodName()) {
                continue;
            }
            if (!localVariable.getInferredJavaType().getJavaTypeInstance()
                    .equals(usedLocal.getInferredJavaType().getJavaTypeInstance())) {
                continue;
            }
            String localName = localVariable.getName().getStringName();
            String usedName = usedLocal.getName().getStringName();
            if (localName != null && localName.equals(usedName)) {
                return true;
            }
        }
        return false;
    }

    public LValue getLvalue() {
        return lvalue;
    }

    public Expression getRvalue() {
        return rvalue;
    }

    public void setRvalue(Expression rvalue) {
        this.rvalue = rvalue;
    }

    @Override
    public boolean match(MatchIterator<StructuredStatement> matchIterator, MatchResultCollector matchResultCollector) {
        StructuredStatement o = matchIterator.getCurrent();
        if (!this.equals(o)) return false;
        matchIterator.advance();
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (o == null) return false;
        if (!(o instanceof StructuredAssignment)) return false;
        StructuredAssignment other = (StructuredAssignment) o;
        if (!lvalue.equals(other.lvalue)) return false;
        if (!rvalue.equals(other.rvalue)) return false;
//        if (isCreator != other.isCreator) return false;
        return true;
    }

    @Override
    public void rewriteExpressions(ExpressionRewriter expressionRewriter) {
        expressionRewriter.handleStatement(getContainer());
        lvalue = expressionRewriter.rewriteExpression(lvalue, null, this.getContainer(), null);
        rvalue = expressionRewriter.rewriteExpression(rvalue, null, this.getContainer(), null);
        harmonizeAssignedLocalTypeFromRvalue();
    }

    @Override
    public boolean rewriteBoxing(PrimitiveBoxingRewriter boxingRewriter) {
        rvalue = boxingRewriter.sugarNonParameterBoxing(rvalue, lvalue.getInferredJavaType().getJavaTypeInstance());
        return true;
    }

    @Override
    public void applyNonArgExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        lvalue = lvalue.applyExpressionRewriter(expressionRewriter, ssaIdentifiers, statementContainer, flags);
    }

}
