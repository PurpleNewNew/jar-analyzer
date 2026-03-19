package org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers;

import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.CastExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ConditionalExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ConstructorInvokationSimple;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ExpressionTypeHintHelper;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.LambdaExpressionCommon;
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
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
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

    public void transform(Op04StructuredStatement root) {
        StructuredScope structuredScope = new StructuredScope();
        root.transform(this, structuredScope);
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
        if (resolvedCreatorType == null || resolvedCreatorType.type == null) return null;
        JavaTypeInstance creatorType = resolvedCreatorType.type;
        JavaTypeInstance currentType = localVariable.getInferredJavaType().getJavaTypeInstance();
        if (resolvedCreatorType.matchQuality == MatchQuality.NEARBY_NAMED
                && shouldKeepNearbyNamedCurrentType(currentType, creatorType)) {
            return currentType;
        }
        if (shouldPreserveSpecificCurrentType(currentType, creatorType)) {
            return currentType;
        }
        if (shouldPreserveIncompatibleCurrentType(currentType, creatorType)) {
            return currentType;
        }
        if (shouldPreserveExistingDeclarationType(localVariable, creatorType, resolvedCreatorType.matchQuality)) {
            return currentType;
        }

        localVariable.getInferredJavaType().forceType(creatorType, true);
        localVariable.clearConflictingGenericDeclaration();
        JavaTypeInstance previousCreationType = localVariable.getCustomCreationJavaType();
        localVariable.setCustomCreationJavaType(creatorType);
        if (refreshCreationType || shouldRefreshAnnotatedCreationType(previousCreationType, creatorType)) {
            localVariable.setCustomCreationType(creatorType.getAnnotatedInstance());
        }
        return creatorType;
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
        if (rvalue.getInferredJavaType().getJavaTypeInstance().implicitlyCastsTo(creatorType, null)) return;
        if (rvalue instanceof CastExpression) {
            JavaTypeInstance castType = rvalue.getInferredJavaType().getJavaTypeInstance();
            if (creatorType.equals(castType)) return;
        }
        structuredAssignment.setRvalue(new CastExpression(stm.getLoc(), new InferredJavaType(creatorType, InferredJavaType.Source.EXPRESSION), rvalue, true));
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
