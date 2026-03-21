package org.benf.cfr.reader.bytecode.analysis.structured.statement;

import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.PrimitiveBoxingRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchIterator;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.matchutil.MatchResultCollector;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.AssignmentExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.CastExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ConstructorInvokationSimple;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ExpressionTypeHintHelper;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.LambdaExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.MemberFunctionInvokation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.StaticFunctionInvokation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.TernaryExpression;
import org.benf.cfr.reader.bytecode.TypeRecoveryTracing;
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
import org.benf.cfr.reader.bytecode.analysis.types.JavaGenericRefTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.RawJavaType;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.bytecode.TypeRecoveryPasses;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.output.Dumper;

import java.util.List;
import java.util.Objects;

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

    public void clearCreator(LValue scopedEntity) {
        if (scopedEntity != null && this.lvalue.equals(scopedEntity)) {
            creator = false;
        }
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        lvalue.collectTypeUsages(collector);
        collector.collectFrom(rvalue);
    }

    @Override
    public Dumper dump(Dumper dumper) {
        return dumpAssignment(dumper, false);
    }

    public Dumper dumpAsResource(Dumper dumper) {
        return dumpAssignment(dumper, true);
    }

    private Dumper dumpAssignment(Dumper dumper, boolean resourceContext) {
        Expression displayRValue = rvalue;
        JavaTypeInstance creatorOverrideType = null;
        if (shouldInlineSyntheticCreator()) {
            AssignmentExpression assignmentExpression = (AssignmentExpression) rvalue;
            ExpressionTypeHintHelper.improveExpressionType(
                    rvalue,
                    assignmentExpression.getUpdatedLValue().getInferredJavaType().getJavaTypeInstance(),
                    TypeRecoveryPasses.INLINE_SYNTHETIC_CREATOR_HINT
            );
            dumper.dump(rvalue);
            if (!resourceContext) {
                dumper.endCodeln();
            }
            return dumper;
        }
        if (isEffectiveCreator()) {
            LocalVariable creatorLocalVariable = lvalue instanceof LocalVariable ? (LocalVariable) lvalue : null;
            ForcedCreatorDisplay forcedCreatorDisplay = extractForcedCreatorDisplay(creatorLocalVariable, displayRValue);
            if (forcedCreatorDisplay != null) {
                creatorOverrideType = forcedCreatorDisplay.declarationType;
                displayRValue = forcedCreatorDisplay.expression;
            }
            if (lvalue.isFinal()) dumper.print("final ");
            LValue.Creation.dump(dumper, lvalue, creatorOverrideType);
        } else {
            dumper.dump(lvalue);
        }
        dumper.operator(" = ").dump(displayRValue);
        if (!resourceContext) {
            dumper.endCodeln();
        }
        return dumper;
    }

    private ForcedCreatorDisplay extractForcedCreatorDisplay(LocalVariable localVariable, Expression expression) {
        if (!(expression instanceof CastExpression)) {
            return null;
        }
        CastExpression castExpression = (CastExpression) expression;
        JavaTypeInstance castType = castExpression.getInferredJavaType().getJavaTypeInstance();
        JavaTypeInstance declarationType = chooseForcedCreatorDeclarationType(localVariable, castType);
        if (!ExpressionTypeHintHelper.canDisplayTypeArguments(declarationType)) {
            return null;
        }
        Expression child = castExpression.getChild();
        JavaTypeInstance childType = ExpressionTypeHintHelper.getDisplayType(child, declarationType);
        if (!isRedundantCreatorCast(childType, declarationType)
                && !isSuppressibleCreatorCastChild(child, declarationType)) {
            return null;
        }
        return new ForcedCreatorDisplay(declarationType, child);
    }

    private boolean isRedundantCreatorCast(JavaTypeInstance childType, JavaTypeInstance castType) {
        if (childType == null || castType == null) {
            return false;
        }
        if (childType.implicitlyCastsTo(castType, null)) {
            return true;
        }
        BindingSuperContainer bindingSupers = childType.getBindingSupers();
        if (bindingSupers == null) {
            return false;
        }
        JavaTypeInstance castBaseType = castType.getDeGenerifiedType();
        if (castBaseType == null) {
            return false;
        }
        JavaGenericRefTypeInstance boundSuper = bindingSupers.getBoundSuperForBase(castBaseType);
        return boundSuper != null && boundSuper.equals(castType);
    }

    private JavaTypeInstance chooseForcedCreatorDeclarationType(LocalVariable localVariable, JavaTypeInstance castType) {
        if (localVariable == null || castType == null) {
            return castType;
        }
        JavaTypeInstance creatorType = localVariable.getCustomCreationJavaType();
        if (creatorType != null
                && creatorType.getDeGenerifiedType() != null
                && creatorType.getDeGenerifiedType().equals(castType.getDeGenerifiedType())) {
            return ExpressionTypeHintHelper.preferResolvedType(castType, creatorType);
        }
        JavaTypeInstance inferredType = localVariable.getInferredJavaType().getJavaTypeInstance();
        if (inferredType != null
                && inferredType.getDeGenerifiedType() != null
                && inferredType.getDeGenerifiedType().equals(castType.getDeGenerifiedType())) {
            return ExpressionTypeHintHelper.preferResolvedType(castType, inferredType);
        }
        return castType;
    }

    private boolean isSuppressibleCreatorCastChild(Expression child, JavaTypeInstance declarationType) {
        if (child instanceof ConstructorInvokationSimple) {
            return true;
        }
        if (child instanceof MemberFunctionInvokation || child instanceof StaticFunctionInvokation) {
            JavaTypeInstance childDisplayType = ExpressionTypeHintHelper.getDisplayType(child, declarationType);
            return childDisplayType != null
                    && (childDisplayType.equals(declarationType)
                    || childDisplayType.implicitlyCastsTo(declarationType, null)
                    || declarationType.implicitlyCastsTo(childDisplayType, null));
        }
        return false;
    }

    public void applyTargetExpressionConstraints() {
        JavaTypeInstance expectedType = lvalue.getInferredJavaType().getJavaTypeInstance();
        if (rvalue instanceof LambdaExpression) {
            TypeRecoveryTracing.trace(
                    TypeRecoveryPasses.ASSIGNMENT_TARGET_CONSTRAINT,
                    rvalue,
                    expectedType,
                    () -> ((LambdaExpression) rvalue).applyTargetTypeConstraint(expectedType)
            );
        } else if (rvalue instanceof TernaryExpression) {
            TypeRecoveryTracing.trace(
                    TypeRecoveryPasses.ASSIGNMENT_TARGET_CONSTRAINT,
                    rvalue,
                    expectedType,
                    () -> ((TernaryExpression) rvalue).applyTargetTypeConstraint(expectedType)
            );
        } else {
            ExpressionTypeHintHelper.improveExpressionType(
                    rvalue,
                    expectedType,
                    TypeRecoveryPasses.ASSIGNMENT_TARGET_CONSTRAINT
            );
        }
        rvalue = ExpressionTypeHintHelper.applyExpectedCastIfNeeded(rvalue, expectedType);
    }

    public TypeConstraintEffect applyTypeConstraints() {
        if (!(lvalue instanceof LocalVariable)) {
            return TypeConstraintEffect.none();
        }
        LocalVariable localVariable = (LocalVariable) lvalue;
        JavaTypeInstance beforeType = localVariable.getInferredJavaType().getJavaTypeInstance();
        JavaTypeInstance beforeCreationType = localVariable.getCustomCreationJavaType();
        JavaTypeInstance displayType = ExpressionTypeHintHelper.getDisplayType(rvalue, beforeType);
        applyDisplayTypeConstraint(localVariable, displayType);
        boolean creatorTargetApplied = harmonizeCreatorTypeFromAssignmentTarget();
        return new TypeConstraintEffect(
                isEffectiveCreator(),
                creatorTargetApplied,
                beforeType,
                localVariable.getInferredJavaType().getJavaTypeInstance(),
                beforeCreationType,
                localVariable.getCustomCreationJavaType(),
                displayType
        );
    }

    private void applyDisplayTypeConstraint(LocalVariable localVariable, JavaTypeInstance displayType) {
        if (!(lvalue instanceof LocalVariable)) {
            return;
        }
        JavaTypeInstance currentType = localVariable.getInferredJavaType().getJavaTypeInstance();
        if (!ExpressionTypeHintHelper.canDefineLocalType(displayType)) {
            return;
        }
        if (shouldPreserveFixedWidthPrimitiveDeclaration(currentType, displayType)) {
            // Display-type prettification is allowed to improve generic/raw declarations, but not to rewrite
            // fixed-width primitive locals after the storage type has already been proven by bytecode. Without
            // this guard, `double x = 0.2F;` could degrade into a `float` declaration just because the literal
            // itself is representable as float syntax.
            return;
        }
        if (localVariable.hasConflictingGenericDeclaration()
                && sharesGenericBase(currentType, displayType)
                && !shouldRecoverConflictingGenericType(currentType, displayType)) {
            return;
        }
        JavaTypeInstance commonBaseType = getConflictingGenericBaseType(currentType, displayType);
        if (commonBaseType != null) {
            localVariable.markConflictingGenericDeclaration();
            localVariable.getInferredJavaType().forceType(commonBaseType, true);
            if (creator) {
                JavaTypeInstance declarationType = preserveExistingDeclarationType(
                        localVariable.getCustomCreationJavaType(),
                        commonBaseType
                );
                localVariable.setCustomCreationJavaType(declarationType);
                if (localVariable.getAnnotatedCreationType() == null) {
                    localVariable.setCustomCreationType(declarationType.getAnnotatedInstance());
                }
            }
            return;
        }
        if (!ExpressionTypeHintHelper.shouldPreferResolvedType(currentType, displayType)) {
            return;
        }
        localVariable.getInferredJavaType().forceType(displayType, true);
        if (creator) {
            JavaTypeInstance declarationType = preserveExistingDeclarationType(
                    localVariable.getCustomCreationJavaType(),
                    displayType
            );
            localVariable.setCustomCreationJavaType(declarationType);
            if (localVariable.getAnnotatedCreationType() == null) {
                localVariable.setCustomCreationType(declarationType.getAnnotatedInstance());
            }
        }
    }

    private JavaTypeInstance getConflictingGenericBaseType(JavaTypeInstance currentType, JavaTypeInstance displayType) {
        if (currentType == null || displayType == null || currentType.equals(displayType)) {
            return null;
        }
        JavaTypeInstance currentBaseType = currentType.getDeGenerifiedType();
        JavaTypeInstance displayBaseType = displayType.getDeGenerifiedType();
        if (!currentBaseType.equals(displayBaseType)) {
            return null;
        }
        boolean displayPreferred = ExpressionTypeHintHelper.shouldPreferResolvedType(displayType, currentType);
        boolean currentPreferred = ExpressionTypeHintHelper.shouldPreferResolvedType(currentType, displayType);
        if ((displayPreferred || currentPreferred)
                && !isSymmetricGenericConflict(currentType, displayType, currentBaseType, displayPreferred, currentPreferred)) {
            return null;
        }
        boolean currentGeneric = currentType.getClass() != currentBaseType.getClass();
        boolean displayGeneric = displayType.getClass() != displayBaseType.getClass();
        if (!currentGeneric && !displayGeneric) {
            return null;
        }
        return currentBaseType;
    }

    private boolean isSymmetricGenericConflict(JavaTypeInstance currentType,
                                               JavaTypeInstance displayType,
                                               JavaTypeInstance sharedBaseType,
                                               boolean displayPreferred,
                                               boolean currentPreferred) {
        if (!displayPreferred || !currentPreferred) {
            return false;
        }
        if (sharedBaseType == null) {
            return false;
        }
        if (currentType == null || displayType == null) {
            return false;
        }
        if (currentType.implicitlyCastsTo(displayType, null)
                || displayType.implicitlyCastsTo(currentType, null)) {
            return false;
        }
        boolean currentGeneric = currentType.getClass() != sharedBaseType.getClass();
        boolean displayGeneric = displayType.getClass() != sharedBaseType.getClass();
        return currentGeneric && displayGeneric;
    }

    private boolean harmonizeCreatorTypeFromAssignmentTarget() {
        if (!isEffectiveCreator()) {
            return false;
        }
        if (!(lvalue instanceof LocalVariable)) {
            return false;
        }
        if (!(rvalue instanceof AssignmentExpression)) {
            return false;
        }
        LValue updatedLValue = ((AssignmentExpression) rvalue).getUpdatedLValue();
        if (!(updatedLValue instanceof LocalVariable)) {
            return false;
        }
        JavaTypeInstance updatedType = updatedLValue.getInferredJavaType().getJavaTypeInstance();
        if (updatedType == null) {
            return false;
        }
        LocalVariable creatorVariable = (LocalVariable) lvalue;
        JavaTypeInstance beforeType = creatorVariable.getInferredJavaType().getJavaTypeInstance();
        JavaTypeInstance beforeCreationType = creatorVariable.getCustomCreationJavaType();
        creatorVariable.getInferredJavaType().forceType(updatedType, true);
        JavaTypeInstance declarationType = preserveExistingDeclarationType(beforeCreationType, updatedType);
        creatorVariable.setCustomCreationJavaType(declarationType);
        if (creatorVariable.getAnnotatedCreationType() == null) {
            creatorVariable.setCustomCreationType(declarationType.getAnnotatedInstance());
        }
        return !updatedType.equals(beforeType)
                || beforeCreationType == null
                || !declarationType.equals(beforeCreationType);
    }

    private JavaTypeInstance preserveExistingDeclarationType(JavaTypeInstance existingDeclarationType,
                                                             JavaTypeInstance candidateType) {
        if (existingDeclarationType == null || candidateType == null || existingDeclarationType.equals(candidateType)) {
            return candidateType;
        }
        JavaTypeInstance existingBaseType = existingDeclarationType.getDeGenerifiedType();
        JavaTypeInstance candidateBaseType = candidateType.getDeGenerifiedType();
        if (existingBaseType != null
                && existingBaseType.equals(candidateBaseType)
                && ExpressionTypeHintHelper.shouldPreferResolvedType(candidateType, existingDeclarationType)) {
            return existingDeclarationType;
        }
        if (candidateType.implicitlyCastsTo(existingDeclarationType, null)
                && !existingDeclarationType.implicitlyCastsTo(candidateType, null)) {
            return existingDeclarationType;
        }
        return candidateType;
    }

    private boolean shouldPreserveFixedWidthPrimitiveDeclaration(JavaTypeInstance currentType,
                                                                 JavaTypeInstance displayType) {
        RawJavaType currentRawType = currentType == null ? null : currentType.getRawTypeOfSimpleType();
        RawJavaType displayRawType = displayType == null ? null : displayType.getRawTypeOfSimpleType();
        if (currentRawType == null || displayRawType == null || currentRawType == displayRawType) {
            return false;
        }
        return isFixedWidthPrimitive(currentRawType) || isFixedWidthPrimitive(displayRawType);
    }

    private boolean isFixedWidthPrimitive(RawJavaType rawJavaType) {
        return rawJavaType == RawJavaType.LONG
                || rawJavaType == RawJavaType.FLOAT
                || rawJavaType == RawJavaType.DOUBLE;
    }

    private boolean sharesGenericBase(JavaTypeInstance currentType, JavaTypeInstance candidateType) {
        if (currentType == null || candidateType == null) {
            return false;
        }
        JavaTypeInstance currentBaseType = currentType.getDeGenerifiedType();
        JavaTypeInstance candidateBaseType = candidateType.getDeGenerifiedType();
        return currentBaseType != null && currentBaseType.equals(candidateBaseType);
    }

    private boolean shouldRecoverConflictingGenericType(JavaTypeInstance currentType,
                                                        JavaTypeInstance displayType) {
        if (currentType == null || displayType == null) {
            return false;
        }
        if (!ExpressionTypeHintHelper.shouldPreferResolvedType(currentType, displayType)) {
            return false;
        }
        return !ExpressionTypeHintHelper.canDisplayTypeArguments(currentType)
                && ExpressionTypeHintHelper.canDisplayTypeArguments(displayType);
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

        if (scopedEntity instanceof LocalVariable && lvalue instanceof LocalVariable) {
            LocalVariable scopedLocalVariable = (LocalVariable) scopedEntity;
            LocalVariable assignmentLocalVariable = (LocalVariable) lvalue;
            if (!scopedLocalVariable.equals(assignmentLocalVariable)) {
                if (!matchesLateResolvedCreatorLocal(scopedLocalVariable, assignmentLocalVariable)) {
                    throw new IllegalArgumentException("Being asked to mark creator for wrong variable: expected="
                            + describeLocal(scopedLocalVariable) + ", actual=" + describeLocal(assignmentLocalVariable));
                }
                LocalVariable creatorLocal = preferCreatorLocal(scopedLocalVariable, assignmentLocalVariable);
                LocalVariable secondaryLocal = creatorLocal == scopedLocalVariable
                        ? assignmentLocalVariable
                        : scopedLocalVariable;
                mergeCreatorLocalState(creatorLocal, secondaryLocal);
                lvalue = creatorLocal;
            }
            creator = true;
            InferredJavaType inferredJavaType = ((LocalVariable) lvalue).getInferredJavaType();
            if (inferredJavaType.isClash()) {
                inferredJavaType.collapseTypeClash();
            }
            return;
        }

        if (scopedEntity instanceof LocalVariable) {
            LocalVariable localVariable = (LocalVariable) scopedEntity;
            if (!localVariable.equals(lvalue)) {
                throw new IllegalArgumentException("Being asked to mark creator for wrong variable: expected="
                        + describeLocal(localVariable) + ", actual=" + lvalue);
            }
            creator = true;
            InferredJavaType inferredJavaType = localVariable.getInferredJavaType();
            if (inferredJavaType.isClash()) {
                inferredJavaType.collapseTypeClash();
            }
        }
    }

    private boolean matchesLateResolvedCreatorLocal(LocalVariable expected, LocalVariable actual) {
        if (expected == null || actual == null) {
            return false;
        }
        if (hasConflictingReadableNames(expected, actual)) {
            return false;
        }
        if (expected.matchesReadableAlias(actual)) {
            return true;
        }
        JavaTypeInstance expectedType = expected.getInferredJavaType().getJavaTypeInstance();
        JavaTypeInstance actualType = actual.getInferredJavaType().getJavaTypeInstance();
        if (!Objects.equals(expectedType, actualType)) {
            return false;
        }
        if (expected.getIdx() >= 0
                && actual.getIdx() >= 0
                && expected.getIdx() == actual.getIdx()) {
            if (Objects.equals(expected.getIdent(), actual.getIdent())) {
                return true;
            }
            return expected.getOriginalRawOffset() >= 0
                    && expected.getOriginalRawOffset() == actual.getOriginalRawOffset();
        }
        return expected.getOriginalRawOffset() >= 0
                && expected.getOriginalRawOffset() == actual.getOriginalRawOffset();
    }

    private boolean hasConflictingReadableNames(LocalVariable expected, LocalVariable actual) {
        if (expected.getName() == null
                || actual.getName() == null
                || !expected.getName().isGoodName()
                || !actual.getName().isGoodName()) {
            return false;
        }
        // Creator reconciliation runs before later naming/metadata passes have fully settled, so it must be
        // conservative: two readable locals with different names are much more likely to be distinct source
        // variables than aliases of the same slot.
        String expectedName = expected.getName().getStringName();
        String actualName = actual.getName().getStringName();
        return expectedName != null
                && actualName != null
                && !expectedName.equals(actualName);
    }

    private LocalVariable preferCreatorLocal(LocalVariable scopedLocalVariable,
                                             LocalVariable assignmentLocalVariable) {
        if (hasBetterCreatorIdentity(assignmentLocalVariable, scopedLocalVariable)) {
            return assignmentLocalVariable;
        }
        return scopedLocalVariable;
    }

    private boolean hasBetterCreatorIdentity(LocalVariable preferred,
                                             LocalVariable fallback) {
        return creatorIdentityScore(preferred) > creatorIdentityScore(fallback);
    }

    private int creatorIdentityScore(LocalVariable localVariable) {
        if (localVariable == null) {
            return Integer.MIN_VALUE;
        }
        int score = 0;
        if (localVariable.getIdx() >= 0) {
            score += 2;
        }
        if (localVariable.getOriginalRawOffset() >= 0) {
            score += 3;
        }
        if (localVariable.getIdent() != null) {
            score += 2;
        }
        if (localVariable.getName() != null && localVariable.getName().isGoodName()) {
            score += 1;
        }
        return score;
    }

    private void mergeCreatorLocalState(LocalVariable target, LocalVariable source) {
        if (target == null || source == null || target == source) {
            return;
        }
        JavaTypeInstance targetType = target.getInferredJavaType().getJavaTypeInstance();
        JavaTypeInstance sourceType = source.getInferredJavaType().getJavaTypeInstance();
        if (ExpressionTypeHintHelper.shouldPreferResolvedType(targetType, sourceType)) {
            target.getInferredJavaType().forceType(sourceType, true);
        }
        JavaTypeInstance targetCreationType = target.getCustomCreationJavaType();
        JavaTypeInstance sourceCreationType = source.getCustomCreationJavaType();
        if (sourceCreationType != null
                && (targetCreationType == null
                || ExpressionTypeHintHelper.shouldPreferResolvedType(targetCreationType, sourceCreationType))) {
            target.setCustomCreationJavaType(sourceCreationType);
            if (source.getAnnotatedCreationType() != null) {
                target.setCustomCreationType(source.getAnnotatedCreationType());
            } else if (target.getAnnotatedCreationType() == null) {
                target.setCustomCreationType(sourceCreationType.getAnnotatedInstance());
            }
        }
        if (source.isCapturedByLambda()) {
            target.markCapturedByLambda();
        }
        if (source.shouldSuppressDefaultInitializer()) {
            target.suppressDefaultInitializer();
        }
        if (source.hasConflictingGenericDeclaration()) {
            target.markConflictingGenericDeclaration();
        }
        if (source.isFinal()) {
            target.markFinal();
        }
        if (source.isVar()) {
            target.markVar();
        }
    }

    private String describeLocal(LocalVariable localVariable) {
        if (localVariable == null) {
            return "<null>";
        }
        String name = localVariable.getName() == null ? "<anon>" : String.valueOf(localVariable.getName().getStringName());
        JavaTypeInstance type = localVariable.getInferredJavaType().getJavaTypeInstance();
        return name + "[idx=" + localVariable.getIdx()
                + ", ident=" + localVariable.getIdent()
                + ", raw=" + localVariable.getOriginalRawOffset()
                + ", type=" + (type == null ? "<null>" : type) + "]";
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
        if (lvalue instanceof LocalVariable) {
            LocalVariable localVariable = (LocalVariable) lvalue;
            applyDisplayTypeConstraint(
                    localVariable,
                    ExpressionTypeHintHelper.getDisplayType(rvalue, localVariable.getInferredJavaType().getJavaTypeInstance())
            );
        }
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

    public static final class TypeConstraintEffect {
        private final boolean creator;
        private final boolean creatorTargetApplied;
        private final JavaTypeInstance beforeType;
        private final JavaTypeInstance afterType;
        private final JavaTypeInstance beforeCreationType;
        private final JavaTypeInstance afterCreationType;
        private final JavaTypeInstance displayType;

        private TypeConstraintEffect(boolean creator,
                                     boolean creatorTargetApplied,
                                     JavaTypeInstance beforeType,
                                     JavaTypeInstance afterType,
                                     JavaTypeInstance beforeCreationType,
                                     JavaTypeInstance afterCreationType,
                                     JavaTypeInstance displayType) {
            this.creator = creator;
            this.creatorTargetApplied = creatorTargetApplied;
            this.beforeType = beforeType;
            this.afterType = afterType;
            this.beforeCreationType = beforeCreationType;
            this.afterCreationType = afterCreationType;
            this.displayType = displayType;
        }

        private static TypeConstraintEffect none() {
            return new TypeConstraintEffect(false, false, null, null, null, null, null);
        }

        public boolean isCreator() {
            return creator;
        }

        public boolean isCreatorTargetApplied() {
            return creatorTargetApplied;
        }

        public JavaTypeInstance getBeforeType() {
            return beforeType;
        }

        public JavaTypeInstance getAfterType() {
            return afterType;
        }

        public JavaTypeInstance getBeforeCreationType() {
            return beforeCreationType;
        }

        public JavaTypeInstance getAfterCreationType() {
            return afterCreationType;
        }

        public JavaTypeInstance getDisplayType() {
            return displayType;
        }
    }

    private record ForcedCreatorDisplay(JavaTypeInstance declarationType, Expression expression) {
    }
}
