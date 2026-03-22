package org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers;

import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.CastExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ConditionalExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ConstructorInvokationSimple;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ExpressionTypeHintHelper;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.LValueExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.LambdaExpressionCommon;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.MemberFunctionInvokation;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.LocalVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.StackSSALabel;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredScope;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredAssignment;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredCatch;
import org.benf.cfr.reader.bytecode.analysis.types.BindingSuperContainer;
import org.benf.cfr.reader.bytecode.analysis.types.JavaGenericPlaceholderTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.RawJavaType;
import org.benf.cfr.reader.bytecode.analysis.types.TypeConstants;
import org.benf.cfr.reader.bytecode.analysis.types.TypeAnnotationHelper;
import org.benf.cfr.reader.bytecode.analysis.types.annotated.JavaAnnotatedTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.entities.annotations.AnnotationTableTypeEntry;
import org.benf.cfr.reader.entities.attributes.AttributeLocalVariableTypeTable;
import org.benf.cfr.reader.entities.attributes.AttributeLocalVariableTable;
import org.benf.cfr.reader.entities.attributes.LocalVariableEntry;
import org.benf.cfr.reader.entities.attributes.AttributeTypeAnnotations;
import org.benf.cfr.reader.entities.attributes.LocalVariableTypeEntry;
import org.benf.cfr.reader.entities.attributes.TypeAnnotationTargetInfo;
import org.benf.cfr.reader.entities.constantpool.ConstantPool;
import org.benf.cfr.reader.entities.constantpool.ConstantPoolUtils;
import org.benf.cfr.reader.util.DecompilerComments;
import org.benf.cfr.reader.util.collections.Functional;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.functors.Predicate;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeSet;

import static org.benf.cfr.reader.entities.attributes.TypeAnnotationEntryValue.type_localvar;
import static org.benf.cfr.reader.entities.attributes.TypeAnnotationEntryValue.type_resourcevar;

public class LocalVariableMetadataTransformer implements StructuredStatementTransformer, ExpressionRewriter {
    private static final int NEARBY_SCOPE_TOLERANCE = 8;

    private final List<AnnotationTableTypeEntry> variableAnnotations;
    private final SortedMap<Integer, Integer> instrsByOffset;
    private final DecompilerComments comments;
    private final ConstantPool cp;
    private final Map<Integer, TreeSet<LocalVariableTypeEntry>> localVariableTypeEntries = new HashMap<Integer, TreeSet<LocalVariableTypeEntry>>();
    private final Map<Integer, TreeSet<LocalVariableEntry>> localVariableEntries = new HashMap<Integer, TreeSet<LocalVariableEntry>>();
    private final List<LocalVariable> creatorHintLocals = ListFactory.newList();

    public LocalVariableMetadataTransformer(AttributeLocalVariableTable localVariableTable,
                                            AttributeLocalVariableTypeTable localVariableTypeTable,
                                            AttributeTypeAnnotations vis,
                                            AttributeTypeAnnotations invis,
                                            SortedMap<Integer, Integer> instrsByOffset,
                                            DecompilerComments comments,
                                            ConstantPool cp) {
        this.instrsByOffset = instrsByOffset;
        this.comments = comments;
        this.cp = cp;
        this.variableAnnotations = ListFactory.orEmptyList(ListFactory.combinedOptimistic(
                vis == null ? null : vis.getAnnotationsFor(type_localvar, type_resourcevar),
                invis == null ? null : invis.getAnnotationsFor(type_localvar, type_resourcevar)));
        if (localVariableTable != null) {
            for (LocalVariableEntry entry : localVariableTable.getLocalVariableEntryList()) {
                TreeSet<LocalVariableEntry> entries = localVariableEntries.get(entry.getIndex());
                if (entries == null) {
                    entries = new TreeSet<LocalVariableEntry>(new OrderLocalVariables());
                    localVariableEntries.put(entry.getIndex(), entries);
                }
                entries.add(entry);
            }
        }
        if (localVariableTypeTable != null) {
            for (LocalVariableTypeEntry entry : localVariableTypeTable.getLocalVariableTypeEntryList()) {
                TreeSet<LocalVariableTypeEntry> entries = localVariableTypeEntries.get(entry.getIndex());
                if (entries == null) {
                    entries = new TreeSet<LocalVariableTypeEntry>(new OrderLocalVariableTypes());
                    localVariableTypeEntries.put(entry.getIndex(), entries);
                }
                entries.add(entry);
            }
        }
    }

    @Override
    public StructuredStatement transform(StructuredStatement in, StructuredScope scope) {
        in.transformStructuredChildren(this, scope);
        in.rewriteExpressions(this);
        return in;
    }

    @Override
    public Expression rewriteExpression(Expression expression, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        if (expression instanceof LambdaExpressionCommon) {
            // Lambda bodies are analysed as synthetic methods with their own LVT/LVTT.
            // Re-applying the enclosing method's metadata here corrupts inline lambda locals.
            return expression;
        }
        return expression.applyExpressionRewriter(this, ssaIdentifiers, statementContainer, flags);
    }

    @Override
    public ConditionalExpression rewriteExpression(ConditionalExpression expression, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        Expression res = expression.applyExpressionRewriter(this, ssaIdentifiers, statementContainer, flags);
        return (ConditionalExpression) res;
    }

    @Override
    public LValue rewriteExpression(LValue lValue, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        if (lValue instanceof LocalVariable) {
            LocalVariable localVariable = (LocalVariable) lValue;
            if (statementContainer != null) {
                Object rawStatement = statementContainer.getStatement();
                if (rawStatement instanceof StructuredAssignment) {
                    StructuredAssignment structuredAssignment = (StructuredAssignment) rawStatement;
                    if (structuredAssignment.isCreator(localVariable)) {
                        applyCreatorType(localVariable, structuredAssignment);
                        return lValue;
                    }
                }
            }
            applyResolvedVariableType(localVariable, false);
        }
        return lValue;
    }

    @Override
    public StackSSALabel rewriteExpression(StackSSALabel lValue, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        return lValue;
    }

    private List<AnnotationTableTypeEntry> getLocalVariableAnnotations(final int offset, final int slot, final int tolerance) {
        if (variableAnnotations.isEmpty()) return Collections.emptyList();
        return Functional.filter(variableAnnotations, new Predicate<AnnotationTableTypeEntry>() {
            @Override
            public boolean test(AnnotationTableTypeEntry in) {
                TypeAnnotationTargetInfo.TypeAnnotationLocalVarTarget tgt = (TypeAnnotationTargetInfo.TypeAnnotationLocalVarTarget) in.getTargetInfo();
                return tgt.matches(offset, slot, tolerance);
            }
        });
    }

    private ResolvedCreatorType resolveLocalVariableCreatorType(LocalVariable localVariable) {
        ResolvedLocalVariableTypeEntry typeEntry = getLocalVariableTypeEntry(localVariable);
        if (typeEntry != null) {
            String signature = cp.getUTF8Entry(typeEntry.entry.getSignatureIndex()).getValue();
            return new ResolvedCreatorType(ConstantPoolUtils.decodeTypeTok(signature, cp), typeEntry.matchQuality);
        }

        ResolvedLocalVariableEntry entry = getLocalVariableEntry(localVariable);
        if (entry == null) return null;

        String descriptor = cp.getUTF8Entry(entry.entry.getDescriptorIndex()).getValue();
        return new ResolvedCreatorType(ConstantPoolUtils.decodeTypeTok(descriptor, cp), entry.matchQuality);
    }

    private ResolvedLocalVariableTypeEntry getLocalVariableTypeEntry(LocalVariable localVariable) {
        int rawOffset = localVariable.getOriginalRawOffset();
        int slot = localVariable.getIdx();
        if (slot < 0) return null;
        TreeSet<LocalVariableTypeEntry> entries = localVariableTypeEntries.get(slot);
        if (entries == null || entries.isEmpty()) return null;
        String localName = localVariable.getName() == null ? null : localVariable.getName().getStringName();
        if (rawOffset < 0) {
            LocalVariableTypeEntry uniqueNamed = findUniqueNamedTypeEntry(entries, localName);
            if (uniqueNamed != null) {
                return new ResolvedLocalVariableTypeEntry(uniqueNamed, MatchQuality.UNIQUE_NAMED);
            }
            if (entries.size() == 1) {
                return new ResolvedLocalVariableTypeEntry(entries.first(), MatchQuality.UNIQUE_SLOT);
            }
            return null;
        }

        boolean currentMethodOffset = isCurrentMethodOffset(rawOffset);
        int offset = rawOffset > 0 ? rawOffset + 2 : 0;

        LocalVariableTypeEntry bestCovered = null;
        LocalVariableTypeEntry bestCoveredNamed = null;
        LocalVariableTypeEntry bestNearbyNamed = null;
        int bestCoveredDistance = Integer.MAX_VALUE;
        int bestCoveredNamedDistance = Integer.MAX_VALUE;
        int bestNearbyNamedDistance = Integer.MAX_VALUE;

        for (LocalVariableTypeEntry entry : entries) {
            int distance = distanceFromScope(entry, offset);
            boolean nameMatches = localName != null && entry.getNameIndex() >= 0
                    && localName.equals(cp.getUTF8Entry(entry.getNameIndex()).getValue());
            if (isOffsetCovered(entry, offset)) {
                if (nameMatches && distance < bestCoveredNamedDistance) {
                    bestCoveredNamed = entry;
                    bestCoveredNamedDistance = distance;
                }
                if (distance < bestCoveredDistance) {
                    bestCovered = entry;
                    bestCoveredDistance = distance;
                }
                continue;
            }
            if (distance > NEARBY_SCOPE_TOLERANCE) {
                continue;
            }
            if (nameMatches && distance < bestNearbyNamedDistance) {
                bestNearbyNamed = entry;
                bestNearbyNamedDistance = distance;
            }
        }
        if (bestCoveredNamed != null) return new ResolvedLocalVariableTypeEntry(bestCoveredNamed, MatchQuality.COVERED_NAMED);
        if (bestCovered != null && (!hasGoodName(localVariable) || currentMethodOffset)) {
            return new ResolvedLocalVariableTypeEntry(bestCovered, MatchQuality.COVERED_SLOT);
        }
        if (bestNearbyNamed != null) {
            return new ResolvedLocalVariableTypeEntry(bestNearbyNamed, MatchQuality.NEARBY_NAMED);
        }
        LocalVariableTypeEntry uniqueNamed = findUniqueNamedTypeEntry(entries, localName);
        return uniqueNamed == null ? null : new ResolvedLocalVariableTypeEntry(uniqueNamed, MatchQuality.UNIQUE_NAMED);
    }

    private ResolvedLocalVariableEntry getLocalVariableEntry(LocalVariable localVariable) {
        int rawOffset = localVariable.getOriginalRawOffset();
        int slot = localVariable.getIdx();
        if (slot < 0) return null;
        TreeSet<LocalVariableEntry> entries = localVariableEntries.get(slot);
        if (entries == null || entries.isEmpty()) return null;
        String localName = localVariable.getName() == null ? null : localVariable.getName().getStringName();
        if (rawOffset < 0) {
            LocalVariableEntry uniqueNamed = findUniqueNamedEntry(entries, localName);
            if (uniqueNamed != null) {
                return new ResolvedLocalVariableEntry(uniqueNamed, MatchQuality.UNIQUE_NAMED);
            }
            if (entries.size() == 1) {
                return new ResolvedLocalVariableEntry(entries.first(), MatchQuality.UNIQUE_SLOT);
            }
            return null;
        }

        boolean currentMethodOffset = isCurrentMethodOffset(rawOffset);
        int offset = rawOffset > 0 ? rawOffset + 2 : 0;

        LocalVariableEntry bestCovered = null;
        LocalVariableEntry bestCoveredNamed = null;
        LocalVariableEntry bestNearbyNamed = null;
        int bestCoveredDistance = Integer.MAX_VALUE;
        int bestCoveredNamedDistance = Integer.MAX_VALUE;
        int bestNearbyNamedDistance = Integer.MAX_VALUE;

        for (LocalVariableEntry entry : entries) {
            int distance = distanceFromScope(entry, offset);
            boolean nameMatches = localName != null && entry.getNameIndex() >= 0
                    && localName.equals(cp.getUTF8Entry(entry.getNameIndex()).getValue());
            if (isOffsetCovered(entry, offset)) {
                if (nameMatches && distance < bestCoveredNamedDistance) {
                    bestCoveredNamed = entry;
                    bestCoveredNamedDistance = distance;
                }
                if (distance < bestCoveredDistance) {
                    bestCovered = entry;
                    bestCoveredDistance = distance;
                }
                continue;
            }
            if (distance > NEARBY_SCOPE_TOLERANCE) {
                continue;
            }
            if (nameMatches && distance < bestNearbyNamedDistance) {
                bestNearbyNamed = entry;
                bestNearbyNamedDistance = distance;
            }
        }
        if (bestCoveredNamed != null) return new ResolvedLocalVariableEntry(bestCoveredNamed, MatchQuality.COVERED_NAMED);
        if (bestCovered != null && (!hasGoodName(localVariable) || currentMethodOffset)) {
            return new ResolvedLocalVariableEntry(bestCovered, MatchQuality.COVERED_SLOT);
        }
        if (bestNearbyNamed != null) {
            return new ResolvedLocalVariableEntry(bestNearbyNamed, MatchQuality.NEARBY_NAMED);
        }
        LocalVariableEntry uniqueNamed = findUniqueNamedEntry(entries, localName);
        return uniqueNamed == null ? null : new ResolvedLocalVariableEntry(uniqueNamed, MatchQuality.UNIQUE_NAMED);
    }

    private boolean isCurrentMethodOffset(int rawOffset) {
        return rawOffset == 0 || instrsByOffset.containsKey(rawOffset);
    }

    private boolean hasGoodName(LocalVariable localVariable) {
        return localVariable.getName() != null && localVariable.getName().isGoodName();
    }

    private boolean isOffsetCovered(LocalVariableTypeEntry entry, int offset) {
        if (entry == null) return false;
        if (offset >= entry.getStartPc() && offset < entry.getEndPc()) {
            return true;
        }
        return offset > entry.getEndPc() && offset <= entry.getEndPc() + 2;
    }

    private boolean isOffsetCovered(LocalVariableEntry entry, int offset) {
        if (entry == null) return false;
        if (offset >= entry.getStartPc() && offset < entry.getEndPc()) {
            return true;
        }
        return offset > entry.getEndPc() && offset <= entry.getEndPc() + 2;
    }

    private int distanceFromScope(LocalVariableTypeEntry entry, int offset) {
        if (entry == null) return Integer.MAX_VALUE;
        if (isOffsetCovered(entry, offset)) {
            return Math.abs(offset - entry.getStartPc());
        }
        if (offset < entry.getStartPc()) {
            return entry.getStartPc() - offset;
        }
        return offset - entry.getEndPc();
    }

    private int distanceFromScope(LocalVariableEntry entry, int offset) {
        if (entry == null) return Integer.MAX_VALUE;
        if (isOffsetCovered(entry, offset)) {
            return Math.abs(offset - entry.getStartPc());
        }
        if (offset < entry.getStartPc()) {
            return entry.getStartPc() - offset;
        }
        return offset - entry.getEndPc();
    }

    private JavaTypeInstance applyResolvedVariableType(LocalVariable localVariable, boolean refreshCreationType) {
        ResolvedCreatorType resolvedCreatorType = resolveLocalVariableCreatorType(localVariable);
        if (resolvedCreatorType == null || resolvedCreatorType.type == null) {
            return applyAliasedCreatorHint(localVariable, refreshCreationType);
        }
        JavaTypeInstance creatorType = resolvedCreatorType.type;
        JavaTypeInstance currentType = localVariable.getInferredJavaType().getJavaTypeInstance();
        JavaTypeInstance aliasedCreatorType = findAliasedCreatorHint(localVariable);
        if (ExpressionTypeHintHelper.shouldPreferResolvedType(creatorType, aliasedCreatorType)) {
            creatorType = aliasedCreatorType;
        }
        if (localVariable.hasConflictingReferenceDeclaration()) {
            // Some reused slots are deliberately pinned to Object because later embedded assignments
            // prove that one recovered local is carrying incompatible reference types. LVT metadata is
            // too local to recover the split, so letting it narrow the declaration again reintroduces
            // uncompilable self-decompile output.
            localVariable.getInferredJavaType().forceType(TypeConstants.OBJECT, true);
            localVariable.forceCustomCreationJavaType(TypeConstants.OBJECT);
            localVariable.setCustomCreationType(TypeConstants.OBJECT.getAnnotatedInstance());
            registerCreatorHintLocal(localVariable);
            return TypeConstants.OBJECT;
        }
        if (localVariable.hasConflictingGenericDeclaration()
                && currentType != null
                && creatorType != null) {
            JavaTypeInstance currentBaseType = currentType.getDeGenerifiedType();
            JavaTypeInstance creatorBaseType = creatorType.getDeGenerifiedType();
            if (currentBaseType != null && currentBaseType.equals(creatorBaseType)) {
                localVariable.getInferredJavaType().forceType(currentType, true);
                localVariable.forceCustomCreationJavaType(currentType);
                if (localVariable.getAnnotatedCreationType() == null) {
                    localVariable.setCustomCreationType(currentType.getAnnotatedInstance());
                }
                registerCreatorHintLocal(localVariable);
                return currentType;
            }
        }
        if (resolvedCreatorType.matchQuality == MatchQuality.NEARBY_NAMED
                && shouldKeepNearbyNamedCurrentType(currentType, creatorType)) {
            recordCreatorDeclarationHint(localVariable, currentType, creatorType, refreshCreationType);
            registerCreatorHintLocal(localVariable);
            return currentType;
        }
        if (shouldPreserveSpecificCurrentType(currentType, creatorType)) {
            recordCreatorDeclarationHint(localVariable, currentType, creatorType, refreshCreationType);
            registerCreatorHintLocal(localVariable);
            return currentType;
        }
        if (shouldPreserveIncompatibleCurrentType(currentType, creatorType)) {
            return currentType;
        }
        if (shouldPreserveExistingDeclarationType(localVariable, creatorType, resolvedCreatorType.matchQuality)) {
            recordCreatorDeclarationHint(localVariable, currentType, creatorType, refreshCreationType);
            registerCreatorHintLocal(localVariable);
            return currentType;
        }

        localVariable.getInferredJavaType().forceType(creatorType, true);
        localVariable.clearConflictingGenericDeclaration();
        JavaTypeInstance previousCreationType = localVariable.getCustomCreationJavaType();
        localVariable.setCustomCreationJavaType(creatorType);
        if (refreshCreationType || shouldRefreshAnnotatedCreationType(previousCreationType, creatorType)) {
            localVariable.setCustomCreationType(creatorType.getAnnotatedInstance());
        }
        registerCreatorHintLocal(localVariable);
        return creatorType;
    }

    private JavaTypeInstance applyAliasedCreatorHint(LocalVariable localVariable, boolean refreshCreationType) {
        if (localVariable == null) {
            return null;
        }
        JavaTypeInstance creatorType = findAliasedCreatorHint(localVariable);
        if (creatorType != null) {
            JavaTypeInstance previousCreationType = localVariable.getCustomCreationJavaType();
            localVariable.setCustomCreationJavaType(creatorType);
            if (refreshCreationType || shouldRefreshAnnotatedCreationType(previousCreationType, creatorType)) {
                localVariable.setCustomCreationType(creatorType.getAnnotatedInstance());
            }
        }
        return creatorType;
    }

    private void registerCreatorHintLocal(LocalVariable localVariable) {
        if (localVariable == null
                || localVariable.getCustomCreationJavaType() == null
                || !ExpressionTypeHintHelper.canDisplayTypeArguments(localVariable.getCustomCreationJavaType())) {
            return;
        }
        for (LocalVariable creatorHintLocal : creatorHintLocals) {
            if (matchesCreatorHintLocal(localVariable, creatorHintLocal)) {
                return;
            }
        }
        creatorHintLocals.add(localVariable);
    }

    private boolean matchesCreatorHintLocal(LocalVariable lhs, LocalVariable rhs) {
        if (lhs == null || rhs == null) {
            return false;
        }
        if (hasConflictingReadableNames(lhs, rhs)) {
            return false;
        }
        if (lhs.matchesReadableAlias(rhs) || rhs.matchesReadableAlias(lhs)) {
            return true;
        }
        JavaTypeInstance lhsType = lhs.getInferredJavaType().getJavaTypeInstance();
        JavaTypeInstance rhsType = rhs.getInferredJavaType().getJavaTypeInstance();
        if (!Objects.equals(lhsType, rhsType)) {
            return false;
        }
        if (lhs.getIdx() >= 0
                && rhs.getIdx() >= 0
                && lhs.getIdx() == rhs.getIdx()) {
            if (Objects.equals(lhs.getIdent(), rhs.getIdent())) {
                return true;
            }
            if (lhs.getOriginalRawOffset() >= 0
                    && lhs.getOriginalRawOffset() == rhs.getOriginalRawOffset()) {
                return true;
            }
            if (lhs.getName() != null
                    && rhs.getName() != null
                    && lhs.getName().isGoodName()
                    && rhs.getName().isGoodName()) {
                String lhsName = lhs.getName().getStringName();
                String rhsName = rhs.getName().getStringName();
                if (lhsName != null && lhsName.equals(rhsName)) {
                    return true;
                }
            }
        }
        return lhs.getOriginalRawOffset() >= 0
                && lhs.getOriginalRawOffset() == rhs.getOriginalRawOffset();
    }

    private boolean hasConflictingReadableNames(LocalVariable lhs, LocalVariable rhs) {
        if (lhs.getName() == null
                || rhs.getName() == null
                || !lhs.getName().isGoodName()
                || !rhs.getName().isGoodName()) {
            return false;
        }
        String lhsName = lhs.getName().getStringName();
        String rhsName = rhs.getName().getStringName();
        return lhsName != null
                && rhsName != null
                && !lhsName.equals(rhsName);
    }

    private JavaTypeInstance findAliasedCreatorHint(LocalVariable localVariable) {
        if (localVariable == null) {
            return null;
        }
        JavaTypeInstance currentType = localVariable.getInferredJavaType().getJavaTypeInstance();
        if (ExpressionTypeHintHelper.canDisplayTypeArguments(currentType)
                && !ExpressionTypeHintHelper.isObjectOnlyGenericType(currentType)) {
            return null;
        }
        JavaTypeInstance currentBaseType = currentType == null ? null : currentType.getDeGenerifiedType();
        if (currentBaseType == null) {
            return null;
        }
        JavaTypeInstance bestHint = null;
        for (LocalVariable creatorHintLocal : creatorHintLocals) {
            boolean matches = matchesCreatorHintLocal(localVariable, creatorHintLocal);
            if (!matches) {
                continue;
            }
            JavaTypeInstance creatorType = creatorHintLocal.getCustomCreationJavaType();
            JavaTypeInstance creatorBaseType = creatorType == null ? null : creatorType.getDeGenerifiedType();
            if (creatorBaseType == null
                    || !currentBaseType.equals(creatorBaseType)
                    || !ExpressionTypeHintHelper.canDisplayTypeArguments(creatorType)
                    || ExpressionTypeHintHelper.isObjectOnlyGenericType(creatorType)) {
                continue;
            }
            if (ExpressionTypeHintHelper.shouldPreferResolvedType(bestHint, creatorType)) {
                bestHint = creatorType;
            }
        }
        return bestHint;
    }

    private boolean sharesCreatorHintIdentity(LocalVariable lhs, LocalVariable rhs) {
        if (lhs == null || rhs == null) {
            return false;
        }
        if (lhs.getIdx() < 0 || rhs.getIdx() < 0 || lhs.getIdx() != rhs.getIdx()) {
            return false;
        }
        if (lhs.getIdent() != null && rhs.getIdent() != null) {
            return lhs.getIdent().equals(rhs.getIdent());
        }
        return lhs.getOriginalRawOffset() >= 0
                && rhs.getOriginalRawOffset() >= 0
                && lhs.getOriginalRawOffset() == rhs.getOriginalRawOffset();
    }

    private void recordCreatorDeclarationHint(LocalVariable localVariable,
                                              JavaTypeInstance currentType,
                                              JavaTypeInstance creatorType,
                                              boolean refreshCreationType) {
        if (localVariable == null
                || creatorType == null
                || !ExpressionTypeHintHelper.canDisplayTypeArguments(creatorType)) {
            return;
        }
        if (currentType != null && !sharesDeclarationCompatibility(currentType, creatorType)) {
            return;
        }
        JavaTypeInstance previousCreationType = localVariable.getCustomCreationJavaType();
        if (previousCreationType != null
                && !ExpressionTypeHintHelper.shouldPreferResolvedType(previousCreationType, creatorType)) {
            return;
        }
        localVariable.setCustomCreationJavaType(creatorType);
        if (refreshCreationType || shouldRefreshAnnotatedCreationType(previousCreationType, creatorType)) {
            localVariable.setCustomCreationType(creatorType.getAnnotatedInstance());
        }
    }

    private boolean sharesDeclarationCompatibility(JavaTypeInstance currentType, JavaTypeInstance creatorType) {
        if (currentType == null || creatorType == null) {
            return false;
        }
        JavaTypeInstance currentBaseType = currentType.getDeGenerifiedType();
        JavaTypeInstance creatorBaseType = creatorType.getDeGenerifiedType();
        if (currentBaseType == null || creatorBaseType == null) {
            return false;
        }
        if (currentBaseType.equals(creatorBaseType)) {
            return true;
        }
        return sharesAssignableHierarchy(currentBaseType, creatorType)
                || sharesAssignableHierarchy(creatorBaseType, currentType);
    }

    private boolean shouldRefreshAnnotatedCreationType(JavaTypeInstance previousCreationType, JavaTypeInstance creatorType) {
        if (creatorType == null) {
            return false;
        }
        if (previousCreationType == null) {
            return true;
        }
        return !creatorType.equals(previousCreationType);
    }

    private boolean shouldKeepNearbyNamedCurrentType(JavaTypeInstance currentType, JavaTypeInstance creatorType) {
        if (isObjectLike(currentType)) {
            return false;
        }
        if (currentType == null || creatorType == null || currentType.equals(creatorType)) {
            return true;
        }
        if (currentType.implicitlyCastsTo(creatorType, null)
                && !creatorType.implicitlyCastsTo(currentType, null)) {
            return false;
        }
        return true;
    }

    private boolean shouldPreserveIncompatibleCurrentType(JavaTypeInstance currentType, JavaTypeInstance creatorType) {
        if (currentType == null || creatorType == null) {
            return false;
        }
        if (currentType.equals(creatorType)) {
            return false;
        }
        if (isObjectLike(currentType) || isObjectLike(creatorType)) {
            return false;
        }
        JavaTypeInstance currentBaseType = currentType.getDeGenerifiedType();
        JavaTypeInstance creatorBaseType = creatorType.getDeGenerifiedType();
        if (currentBaseType != null && currentBaseType.equals(creatorBaseType)) {
            return false;
        }
        if (sharesAssignableHierarchy(currentBaseType, creatorType)
                || sharesAssignableHierarchy(creatorBaseType, currentType)) {
            return false;
        }
        return !currentType.implicitlyCastsTo(creatorType, null)
                && !creatorType.implicitlyCastsTo(currentType, null);
    }

    private boolean shouldPreserveSpecificCurrentType(JavaTypeInstance currentType, JavaTypeInstance creatorType) {
        if (currentType == null || creatorType == null || currentType.equals(creatorType)) {
            return false;
        }
        JavaTypeInstance currentBaseType = currentType.getDeGenerifiedType();
        JavaTypeInstance creatorBaseType = creatorType.getDeGenerifiedType();
        if (currentBaseType == null || creatorBaseType == null || !currentBaseType.equals(creatorBaseType)) {
            return false;
        }
        if (!ExpressionTypeHintHelper.canDisplayTypeArguments(currentType)) {
            return false;
        }
        if (ExpressionTypeHintHelper.canDisplayTypeArguments(creatorType)) {
            return false;
        }
        return ExpressionTypeHintHelper.shouldPreferResolvedType(creatorType, currentType);
    }

    private boolean sharesAssignableHierarchy(JavaTypeInstance baseType, JavaTypeInstance candidateType) {
        if (baseType == null || candidateType == null) {
            return false;
        }
        BindingSuperContainer bindingSupers = candidateType.getBindingSupers();
        return bindingSupers != null && bindingSupers.containsBase(baseType);
    }

    private boolean isObjectLike(JavaTypeInstance type) {
        if (type == null) return true;
        if (TypeConstants.OBJECT.equals(type)) return true;
        JavaTypeInstance deGenerifiedType = type.getDeGenerifiedType();
        return deGenerifiedType != null && TypeConstants.OBJECT.equals(deGenerifiedType);
    }

    private boolean shouldPreserveExistingDeclarationType(LocalVariable localVariable,
                                                          JavaTypeInstance creatorType,
                                                          MatchQuality matchQuality) {
        if ((matchQuality != MatchQuality.NEARBY_NAMED && matchQuality != MatchQuality.UNIQUE_NAMED)
                || !hasGoodName(localVariable)) {
            return false;
        }
        JavaTypeInstance currentType = localVariable.getInferredJavaType().getJavaTypeInstance();
        if (currentType == null
                || currentType.equals(creatorType)
                || TypeConstants.OBJECT.equals(currentType)
                || TypeConstants.OBJECT.equals(currentType.getDeGenerifiedType())) {
            return false;
        }
        return creatorType.implicitlyCastsTo(currentType, null)
                && !currentType.implicitlyCastsTo(creatorType, null);
    }

    private void applyCreatorType(LocalVariable localVariable, StructuredStatement stm) {
        JavaTypeInstance creatorType = applyResolvedVariableType(localVariable, true);
        if (creatorType == null) return;

        if (!(stm instanceof StructuredAssignment)) return;
        StructuredAssignment structuredAssignment = (StructuredAssignment) stm;
        if (!structuredAssignment.isCreator(localVariable)) return;

        Expression rvalue = structuredAssignment.getRvalue();
        if (rvalue instanceof ConstructorInvokationSimple) {
            ((ConstructorInvokationSimple) rvalue).improveConstructionType(creatorType);
        }
        ExpressionTypeHintHelper.improveExpressionType(rvalue, creatorType);
        if (rvalue.getInferredJavaType().getJavaTypeInstance().implicitlyCastsTo(creatorType, null)) return;
        if (canResolveMemberGenericReturn(rvalue, creatorType)) return;
        if (rvalue instanceof CastExpression) {
            JavaTypeInstance castType = rvalue.getInferredJavaType().getJavaTypeInstance();
            if (creatorType.equals(castType)) return;
        }
        structuredAssignment.setRvalue(new CastExpression(stm.getLoc(), new InferredJavaType(creatorType, InferredJavaType.Source.EXPRESSION), rvalue, true));
    }

    private boolean canResolveMemberGenericReturn(Expression expression, JavaTypeInstance targetType) {
        if (!(expression instanceof MemberFunctionInvokation)
                || targetType == null
                || targetType instanceof RawJavaType) {
            return false;
        }
        MemberFunctionInvokation invokation = (MemberFunctionInvokation) expression;
        if (!(invokation.getMethodPrototype().getReturnType() instanceof JavaGenericPlaceholderTypeInstance)) {
            return false;
        }
        Expression receiver = invokation.getObject();
        if (!(receiver instanceof LValueExpression)) {
            return false;
        }
        LValue lValue = ((LValueExpression) receiver).getLValue();
        if (!(lValue instanceof LocalVariable)) {
            return false;
        }
        LocalVariable localVariable = (LocalVariable) lValue;
        if (localVariable.hasConflictingGenericDeclaration()) {
            return false;
        }
        JavaTypeInstance receiverType = localVariable.getCustomCreationJavaType();
        JavaTypeInstance aliasedReceiverType = findAliasedCreatorHint(localVariable);
        if (ExpressionTypeHintHelper.shouldPreferResolvedType(receiverType, aliasedReceiverType)) {
            receiverType = aliasedReceiverType;
        }
        if (!ExpressionTypeHintHelper.canDisplayTypeArguments(receiverType)) {
            ResolvedCreatorType resolvedCreatorType = resolveLocalVariableCreatorType(localVariable);
            if (resolvedCreatorType != null) {
                JavaTypeInstance resolvedType = resolvedCreatorType.type;
                if (ExpressionTypeHintHelper.shouldPreferResolvedType(receiverType, resolvedType)) {
                    receiverType = resolvedType;
                }
            }
        }
        if (!ExpressionTypeHintHelper.canDisplayTypeArguments(receiverType)) {
            receiverType = localVariable.getInferredJavaType().getJavaTypeInstance();
        }
        if (!ExpressionTypeHintHelper.canDisplayTypeArguments(receiverType)) {
            return false;
        }
        JavaTypeInstance resolvedReturnType = invokation.getMethodPrototype().getReturnType(receiverType, invokation.getArgs());
        if (resolvedReturnType == null) {
            return false;
        }
        JavaTypeInstance targetBaseType = targetType.getDeGenerifiedType();
        JavaTypeInstance resolvedBaseType = resolvedReturnType.getDeGenerifiedType();
        if (targetBaseType == null
                || resolvedBaseType == null
                || !targetBaseType.equals(resolvedBaseType)) {
            return false;
        }
        return resolvedReturnType.equals(targetType)
                || resolvedReturnType.implicitlyCastsTo(targetType, null);
    }

    @Override
    public void handleStatement(StatementContainer statementContainer) {
        Object rawStatement = statementContainer.getStatement();
        if (!(rawStatement instanceof StructuredStatement)) return;
        StructuredStatement stm = (StructuredStatement) rawStatement;

        if (variableAnnotations.isEmpty() && localVariableTypeEntries.isEmpty() && localVariableEntries.isEmpty()) return;
        List<LValue> createdHere = stm.findCreatedHere();
        if (createdHere == null || createdHere.isEmpty()) return;

        for (LValue lValue : createdHere) {
            if (!(lValue instanceof LocalVariable)) continue;
            LocalVariable localVariable = (LocalVariable) lValue;
            applyCreatorType(localVariable, stm);

            if (variableAnnotations.isEmpty()) continue;
            int offset = localVariable.getOriginalRawOffset();
            int slot = localVariable.getIdx();
            if (offset < 0 || slot < 0) continue;

            SortedMap<Integer, Integer> heapMap = instrsByOffset.headMap(offset);
            int offsetTolerance = heapMap.isEmpty() ? 1 : offset - heapMap.lastKey();
            List<AnnotationTableTypeEntry> entries = getLocalVariableAnnotations(offset, slot, offsetTolerance);
            if (entries.isEmpty()) continue;

            JavaAnnotatedTypeInstance annotatedTypeInstance = localVariable.getAnnotatedCreationType();
            if (annotatedTypeInstance == null) {
                annotatedTypeInstance = localVariable.getInferredJavaType().getJavaTypeInstance().getAnnotatedInstance();
                localVariable.setCustomCreationType(annotatedTypeInstance);
            }
            TypeAnnotationHelper.apply(annotatedTypeInstance, entries, comments);
        }
    }

    private static class OrderLocalVariableTypes implements Comparator<LocalVariableTypeEntry> {
        @Override
        public int compare(LocalVariableTypeEntry a, LocalVariableTypeEntry b) {
            int x = a.getStartPc() - b.getStartPc();
            if (x != 0) return x;
            x = a.getLength() - b.getLength();
            if (x != 0) return x;
            x = a.getSignatureIndex() - b.getSignatureIndex();
            if (x != 0) return x;
            return a.getNameIndex() - b.getNameIndex();
        }
    }

    private static class OrderLocalVariables implements Comparator<LocalVariableEntry> {
        @Override
        public int compare(LocalVariableEntry a, LocalVariableEntry b) {
            int x = a.getStartPc() - b.getStartPc();
            if (x != 0) return x;
            x = a.getLength() - b.getLength();
            if (x != 0) return x;
            x = a.getDescriptorIndex() - b.getDescriptorIndex();
            if (x != 0) return x;
            return a.getNameIndex() - b.getNameIndex();
        }
    }

    private enum MatchQuality {
        COVERED_NAMED,
        COVERED_SLOT,
        NEARBY_NAMED,
        UNIQUE_NAMED,
        UNIQUE_SLOT
    }

    private LocalVariableTypeEntry findUniqueNamedTypeEntry(TreeSet<LocalVariableTypeEntry> entries, String localName) {
        if (entries == null || entries.isEmpty() || localName == null) {
            return null;
        }
        LocalVariableTypeEntry match = null;
        for (LocalVariableTypeEntry entry : entries) {
            if (entry.getNameIndex() < 0) {
                continue;
            }
            String entryName = cp.getUTF8Entry(entry.getNameIndex()).getValue();
            if (!localName.equals(entryName)) {
                continue;
            }
            if (match != null) {
                return null;
            }
            match = entry;
        }
        return match;
    }

    private LocalVariableEntry findUniqueNamedEntry(TreeSet<LocalVariableEntry> entries, String localName) {
        if (entries == null || entries.isEmpty() || localName == null) {
            return null;
        }
        LocalVariableEntry match = null;
        for (LocalVariableEntry entry : entries) {
            if (entry.getNameIndex() < 0) {
                continue;
            }
            String entryName = cp.getUTF8Entry(entry.getNameIndex()).getValue();
            if (!localName.equals(entryName)) {
                continue;
            }
            if (match != null) {
                return null;
            }
            match = entry;
        }
        return match;
    }

    private static final class ResolvedCreatorType {
        private final JavaTypeInstance type;
        private final MatchQuality matchQuality;

        private ResolvedCreatorType(JavaTypeInstance type, MatchQuality matchQuality) {
            this.type = type;
            this.matchQuality = matchQuality;
        }
    }

    private static final class ResolvedLocalVariableTypeEntry {
        private final LocalVariableTypeEntry entry;
        private final MatchQuality matchQuality;

        private ResolvedLocalVariableTypeEntry(LocalVariableTypeEntry entry, MatchQuality matchQuality) {
            this.entry = entry;
            this.matchQuality = matchQuality;
        }
    }

    private static final class ResolvedLocalVariableEntry {
        private final LocalVariableEntry entry;
        private final MatchQuality matchQuality;

        private ResolvedLocalVariableEntry(LocalVariableEntry entry, MatchQuality matchQuality) {
            this.entry = entry;
            this.matchQuality = matchQuality;
        }
    }
}
