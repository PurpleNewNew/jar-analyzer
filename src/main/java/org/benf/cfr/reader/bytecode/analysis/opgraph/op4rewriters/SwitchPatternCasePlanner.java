package org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters;

import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.Literal;
import org.benf.cfr.reader.bytecode.analysis.parse.literal.TypedLiteral;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.LocalVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.pattern.PatternFactory;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.Block;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredCase;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredComment;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredWhile;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.util.collections.ListFactory;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

final class SwitchPatternCasePlanner {
    private final SwitchPatternCaseDetector detector = new SwitchPatternCaseDetector();

    // TODO[algorithm]: Split the remaining switch-specialized planning work into reusable pattern
    // detector/builder/emitter components so guarded, record and conditional-binding cases stop
    // living behind a switch-only planner.

    List<CaseRewritePlan> planCases(Block switchBlock,
                                    SwitchPatternBootstrapMatch bootstrapMatch,
                                    StructuredWhile patternLoop) {
        List<StructuredCase> cases = ListFactory.newList();
        for (Op04StructuredStatement statement : switchBlock.getBlockStatements()) {
            StructuredStatement structuredStatement = statement.getStatement();
            if (structuredStatement instanceof StructuredComment || structuredStatement.isEffectivelyNOP()) {
                continue;
            }
            if (!(structuredStatement instanceof StructuredCase)) {
                return null;
            }
            cases.add((StructuredCase) structuredStatement);
        }
        Set<Integer> unmatchedLabelIndexes = collectUnmatchedLabelIndexes(cases, bootstrapMatch);
        if (unmatchedLabelIndexes == null) {
            return null;
        }
        List<CaseRewritePlan> plans = ListFactory.newList();
        for (StructuredCase structuredCase : cases) {
            PlanSpec planSpec = detectPlan(
                    structuredCase,
                    bootstrapMatch,
                    unmatchedLabelIndexes,
                    patternLoop
            );
            if (planSpec == null) {
                return null;
            }
            plans.add(buildPlan(planSpec));
        }
        return plans;
    }

    private PlanSpec detectPlan(StructuredCase structuredCase,
                                SwitchPatternBootstrapMatch match,
                                Set<Integer> unmatchedLabelIndexes,
                                StructuredWhile patternLoop) {
        List<Expression> values = structuredCase.getValues();
        if (values.isEmpty() && unmatchedLabelIndexes.isEmpty()) {
            return PlanSpec.unchanged(structuredCase);
        }
        ResolvedLabels resolvedLabels = resolveLabels(values, match, unmatchedLabelIndexes, structuredCase.isDefault());
        if (resolvedLabels == null) {
            return null;
        }
        if (resolvedLabels.patternTypes.isEmpty()) {
            if (resolvedLabels.explicitValues.size() == 1
                    && resolvedLabels.explicitValues.get(0) == Literal.NULL) {
                return PlanSpec.nullLiteral(structuredCase);
            }
            return PlanSpec.explicitValues(structuredCase, resolvedLabels.explicitValues);
        }
        if (resolvedLabels.patternTypes.size() != 1 || !resolvedLabels.explicitValues.isEmpty()) {
            return null;
        }
        if (!(structuredCase.getBody().getStatement() instanceof Block)) {
            return null;
        }
        Block bodyBlock = (Block) structuredCase.getBody().getStatement();
        DetectionContext context = new DetectionContext(
                structuredCase,
                bodyBlock,
                bodyBlock.getBlockStatements(),
                match.getSelector(),
                resolvedLabels.patternTypes.get(0),
                match.getIndexLValue(),
                patternLoop
        );
        return detector.detect(context);
    }

    private CaseRewritePlan buildPlan(PlanSpec spec) {
        StructuredCase structuredCase = spec.getStructuredCase();
        Block bodyBlock = spec.getBodyBlock();
        LinkedList<Op04StructuredStatement> rewrittenStatements = spec.getRewrittenStatements();
        List<Expression> rewrittenValues;
        switch (spec.getKind()) {
            case UNCHANGED:
                rewrittenValues = null;
                bodyBlock = null;
                rewrittenStatements = null;
                break;
            case NULL_LITERAL:
                rewrittenValues = ListFactory.<Expression>newImmutableList(Literal.NULL);
                bodyBlock = null;
                rewrittenStatements = null;
                break;
            case EXPLICIT_VALUES:
                rewrittenValues = spec.getExplicitValues();
                bodyBlock = null;
                rewrittenStatements = null;
                break;
            case BINDING:
                rewrittenValues = ListFactory.<Expression>newImmutableList(
                        PatternFactory.caseLabel(PatternFactory.binding(spec.getBindingType(), spec.getBinding()))
                );
                break;
            case GUARDED_BINDING:
                rewrittenValues = ListFactory.<Expression>newImmutableList(
                        PatternFactory.caseLabel(
                                PatternFactory.guarded(
                                        PatternFactory.binding(spec.getBindingType(), spec.getBinding()),
                                        spec.getGuard()
                                )
                        )
                );
                break;
            case RECORD:
                rewrittenValues = ListFactory.<Expression>newImmutableList(
                        PatternFactory.caseLabel(
                                PatternFactory.record(spec.getBindingType(), spec.getRecordComponents())
                        )
                );
                break;
            default:
                throw new IllegalStateException("Unknown switch pattern case plan kind: " + spec.getKind());
        }
        return new CaseRewritePlan(structuredCase, bodyBlock, rewrittenValues, rewrittenStatements);
    }

    private Set<Integer> collectUnmatchedLabelIndexes(List<StructuredCase> structuredCases,
                                                      SwitchPatternBootstrapMatch match) {
        LinkedHashSet<Integer> unmatched = new LinkedHashSet<Integer>();
        for (int x = 0; x < match.getCaseLabels().size(); ++x) {
            unmatched.add(x);
        }
        for (StructuredCase structuredCase : structuredCases) {
            for (Expression value : structuredCase.getValues()) {
                Integer caseIndex = getCaseIndex(value);
                if (caseIndex == null) {
                    return null;
                }
                if (caseIndex >= 0) {
                    if (caseIndex >= match.getCaseLabels().size()) {
                        return null;
                    }
                    unmatched.remove(caseIndex);
                }
            }
        }
        return unmatched;
    }

    private ResolvedLabels resolveLabels(List<Expression> values,
                                         SwitchPatternBootstrapMatch match,
                                         Set<Integer> unmatchedLabelIndexes,
                                         boolean defaultCase) {
        List<Expression> explicitValues = ListFactory.newList();
        List<org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance> patternTypes = ListFactory.newList();
        if (values.isEmpty()) {
            if (!defaultCase || unmatchedLabelIndexes.size() != 1) {
                return null;
            }
            Integer only = unmatchedLabelIndexes.iterator().next();
            appendResolvedLabel(match.getCaseLabels().get(only), explicitValues, patternTypes);
            return new ResolvedLabels(explicitValues, patternTypes);
        }
        for (Expression value : values) {
            Integer caseIndex = getCaseIndex(value);
            if (caseIndex == null) {
                return null;
            }
            if (caseIndex == -1) {
                explicitValues.add(Literal.NULL);
                continue;
            }
            if (caseIndex < 0 || caseIndex >= match.getCaseLabels().size()) {
                return null;
            }
            appendResolvedLabel(match.getCaseLabels().get(caseIndex), explicitValues, patternTypes);
        }
        return new ResolvedLabels(explicitValues, patternTypes);
    }

    private void appendResolvedLabel(SwitchPatternBootstrapMatch.SwitchCaseLabel label,
                                     List<Expression> explicitValues,
                                     List<org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance> patternTypes) {
        if (label.isPattern()) {
            patternTypes.add(label.getPatternType());
        } else {
            explicitValues.add(label.getCaseExpression());
        }
    }

    private Integer getCaseIndex(Expression expression) {
        if (!(expression instanceof Literal)) {
            return null;
        }
        TypedLiteral literal = ((Literal) expression).getValue();
        if (literal.getType() != TypedLiteral.LiteralType.Integer) {
            return null;
        }
        return literal.getIntValue();
    }

    static final class CaseRewritePlan {
        private final StructuredCase structuredCase;
        private final Block bodyBlock;
        private final List<Expression> rewrittenValues;
        private final LinkedList<Op04StructuredStatement> rewrittenStatements;

        CaseRewritePlan(StructuredCase structuredCase,
                        Block bodyBlock,
                        List<Expression> rewrittenValues,
                        LinkedList<Op04StructuredStatement> rewrittenStatements) {
            this.structuredCase = structuredCase;
            this.bodyBlock = bodyBlock;
            this.rewrittenValues = rewrittenValues;
            this.rewrittenStatements = rewrittenStatements;
        }

        StructuredCase getStructuredCase() {
            return structuredCase;
        }

        Block getBodyBlock() {
            return bodyBlock;
        }

        List<Expression> getRewrittenValues() {
            return rewrittenValues;
        }

        LinkedList<Op04StructuredStatement> getRewrittenStatements() {
            return rewrittenStatements;
        }
    }

    static final class DetectionContext {
        private final StructuredCase structuredCase;
        private final Block bodyBlock;
        private final List<Op04StructuredStatement> statements;
        private final Expression selector;
        private final JavaTypeInstance expectedPatternType;
        private final LValue indexLValue;
        private final StructuredWhile patternLoop;

        private DetectionContext(StructuredCase structuredCase,
                                 Block bodyBlock,
                                 List<Op04StructuredStatement> statements,
                                 Expression selector,
                                 JavaTypeInstance expectedPatternType,
                                 LValue indexLValue,
                                 StructuredWhile patternLoop) {
            this.structuredCase = structuredCase;
            this.bodyBlock = bodyBlock;
            this.statements = statements;
            this.selector = selector;
            this.expectedPatternType = expectedPatternType;
            this.indexLValue = indexLValue;
            this.patternLoop = patternLoop;
        }

        StructuredCase getStructuredCase() {
            return structuredCase;
        }

        Block getBodyBlock() {
            return bodyBlock;
        }

        List<Op04StructuredStatement> getStatements() {
            return statements;
        }

        Expression getSelector() {
            return selector;
        }

        JavaTypeInstance getExpectedPatternType() {
            return expectedPatternType;
        }

        LValue getIndexLValue() {
            return indexLValue;
        }

        StructuredWhile getPatternLoop() {
            return patternLoop;
        }
    }

    enum PlanKind {
        UNCHANGED,
        NULL_LITERAL,
        EXPLICIT_VALUES,
        BINDING,
        GUARDED_BINDING,
        RECORD
    }

    static final class PlanSpec {
        private final PlanKind kind;
        private final StructuredCase structuredCase;
        private final Block bodyBlock;
        private final LinkedList<Op04StructuredStatement> rewrittenStatements;
        private final List<Expression> explicitValues;
        private final JavaTypeInstance bindingType;
        private final LocalVariable binding;
        private final Expression guard;
        private final List<LValue> recordComponents;

        private PlanSpec(PlanKind kind,
                         StructuredCase structuredCase,
                         Block bodyBlock,
                         LinkedList<Op04StructuredStatement> rewrittenStatements,
                         List<Expression> explicitValues,
                         JavaTypeInstance bindingType,
                         LocalVariable binding,
                         Expression guard,
                         List<LValue> recordComponents) {
            this.kind = kind;
            this.structuredCase = structuredCase;
            this.bodyBlock = bodyBlock;
            this.rewrittenStatements = rewrittenStatements;
            this.explicitValues = explicitValues;
            this.bindingType = bindingType;
            this.binding = binding;
            this.guard = guard;
            this.recordComponents = recordComponents;
        }

        static PlanSpec unchanged(StructuredCase structuredCase) {
            return new PlanSpec(PlanKind.UNCHANGED, structuredCase, null, null, null, null, null, null, null);
        }

        static PlanSpec nullLiteral(StructuredCase structuredCase) {
            return new PlanSpec(PlanKind.NULL_LITERAL, structuredCase, null, null, null, null, null, null, null);
        }

        static PlanSpec explicitValues(StructuredCase structuredCase, List<Expression> explicitValues) {
            return new PlanSpec(PlanKind.EXPLICIT_VALUES, structuredCase, null, null, explicitValues, null, null, null, null);
        }

        static PlanSpec binding(DetectionContext context,
                                LocalVariable binding,
                                LinkedList<Op04StructuredStatement> rewrittenStatements) {
            return new PlanSpec(
                    PlanKind.BINDING,
                    context.getStructuredCase(),
                    context.getBodyBlock(),
                    rewrittenStatements,
                    null,
                    binding.getInferredJavaType().getJavaTypeInstance(),
                    binding,
                    null,
                    null
            );
        }

        static PlanSpec guardedBinding(DetectionContext context,
                                       LocalVariable binding,
                                       Expression guard,
                                       LinkedList<Op04StructuredStatement> rewrittenStatements) {
            return new PlanSpec(
                    PlanKind.GUARDED_BINDING,
                    context.getStructuredCase(),
                    context.getBodyBlock(),
                    rewrittenStatements,
                    null,
                    binding.getInferredJavaType().getJavaTypeInstance(),
                    binding,
                    guard,
                    null
            );
        }

        static PlanSpec record(DetectionContext context,
                               LocalVariable binding,
                               List<LValue> components,
                               LinkedList<Op04StructuredStatement> rewrittenStatements) {
            return new PlanSpec(
                    PlanKind.RECORD,
                    context.getStructuredCase(),
                    context.getBodyBlock(),
                    rewrittenStatements,
                    null,
                    binding.getInferredJavaType().getJavaTypeInstance(),
                    binding,
                    null,
                    components
            );
        }

        PlanKind getKind() {
            return kind;
        }

        StructuredCase getStructuredCase() {
            return structuredCase;
        }

        Block getBodyBlock() {
            return bodyBlock;
        }

        LinkedList<Op04StructuredStatement> getRewrittenStatements() {
            return rewrittenStatements;
        }

        List<Expression> getExplicitValues() {
            return explicitValues;
        }

        JavaTypeInstance getBindingType() {
            return bindingType;
        }

        LocalVariable getBinding() {
            return binding;
        }

        Expression getGuard() {
            return guard;
        }

        List<LValue> getRecordComponents() {
            return recordComponents;
        }
    }

    private static final class ResolvedLabels {
        private final List<Expression> explicitValues;
        private final List<JavaTypeInstance> patternTypes;

        private ResolvedLabels(List<Expression> explicitValues,
                               List<JavaTypeInstance> patternTypes) {
            this.explicitValues = explicitValues;
            this.patternTypes = patternTypes;
        }
    }
}
