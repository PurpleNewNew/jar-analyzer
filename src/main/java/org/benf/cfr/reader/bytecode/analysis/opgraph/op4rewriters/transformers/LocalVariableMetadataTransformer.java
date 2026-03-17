package org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers;

import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.CastExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ConditionalExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.LocalVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.StackSSALabel;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredScope;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredAssignment;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredCatch;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.TypeAnnotationHelper;
import org.benf.cfr.reader.bytecode.analysis.types.annotated.JavaAnnotatedTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.discovery.InferredJavaType;
import org.benf.cfr.reader.entities.annotations.AnnotationTableTypeEntry;
import org.benf.cfr.reader.entities.attributes.AttributeLocalVariableTypeTable;
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

    private final List<AnnotationTableTypeEntry> variableAnnotations;
    private final SortedMap<Integer, Integer> instrsByOffset;
    private final DecompilerComments comments;
    private final ConstantPool cp;
    private final Map<Integer, TreeSet<LocalVariableTypeEntry>> localVariableTypeEntries = new HashMap<Integer, TreeSet<LocalVariableTypeEntry>>();

    public LocalVariableMetadataTransformer(AttributeLocalVariableTypeTable localVariableTypeTable,
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
        return expression.applyExpressionRewriter(this, ssaIdentifiers, statementContainer, flags);
    }

    @Override
    public ConditionalExpression rewriteExpression(ConditionalExpression expression, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        Expression res = expression.applyExpressionRewriter(this, ssaIdentifiers, statementContainer, flags);
        return (ConditionalExpression) res;
    }

    @Override
    public LValue rewriteExpression(LValue lValue, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
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

    private JavaTypeInstance getLocalVariableCreatorType(LocalVariable localVariable) {
        int offset = localVariable.getOriginalRawOffset();
        int slot = localVariable.getIdx();
        if (offset < 0 || slot < 0) return null;
        TreeSet<LocalVariableTypeEntry> entries = localVariableTypeEntries.get(slot);
        if (entries == null || entries.isEmpty()) return null;

        offset = offset > 0 ? offset + 2 : 0;
        LocalVariableTypeEntry probe = new LocalVariableTypeEntry(offset, 1, -1, -1, slot);
        LocalVariableTypeEntry entry = entries.floor(probe);
        if (entry == null) return null;
        if (offset > entry.getEndPc() && entries.ceiling(probe) != null) return null;

        String signature = cp.getUTF8Entry(entry.getSignatureIndex()).getValue();
        return ConstantPoolUtils.decodeTypeTok(signature, cp);
    }

    private void applyCreatorType(LocalVariable localVariable, StructuredStatement stm) {
        JavaTypeInstance creatorType = getLocalVariableCreatorType(localVariable);
        if (creatorType == null) return;

        localVariable.getInferredJavaType().forceType(creatorType, true);
        localVariable.setCustomCreationType(creatorType.getAnnotatedInstance());

        if (!(stm instanceof StructuredAssignment)) return;
        StructuredAssignment structuredAssignment = (StructuredAssignment) stm;
        if (!structuredAssignment.isCreator(localVariable)) return;

        Expression rvalue = structuredAssignment.getRvalue();
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

        if (stm instanceof StructuredCatch) {
            handleCatchStatement((StructuredCatch) stm);
            return;
        }

        if (variableAnnotations.isEmpty() && localVariableTypeEntries.isEmpty()) return;
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

    private void handleCatchStatement(StructuredCatch stm) {
        // Need to link our catch back to the ORIGINAL catch index.
        // TODO : NYI - we need to link up.
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
}
