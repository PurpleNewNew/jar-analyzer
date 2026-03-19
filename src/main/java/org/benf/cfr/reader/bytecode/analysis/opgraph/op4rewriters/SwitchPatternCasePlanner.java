package org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters;

import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.Literal;
import org.benf.cfr.reader.bytecode.analysis.parse.literal.TypedLiteral;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.Block;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredCase;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredComment;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredWhile;
import org.benf.cfr.reader.util.collections.ListFactory;

import java.util.LinkedList;
import java.util.List;

final class SwitchPatternCasePlanner {
    private final SwitchPatternCaseDetector detector = new SwitchPatternCaseDetector();
    private final SwitchPatternCaseBuilder builder = new SwitchPatternCaseBuilder();

    // TODO[algorithm]: Split the remaining switch-specialized planning work into reusable pattern
    // detector/builder/emitter components so guarded, record and conditional-binding cases stop
    // living behind a switch-only planner.

    List<CaseRewritePlan> planCases(Block switchBlock,
                                    SwitchPatternBootstrapMatch bootstrapMatch,
                                    StructuredWhile patternLoop) {
        List<CaseRewritePlan> plans = ListFactory.newList();
        for (Op04StructuredStatement statement : switchBlock.getBlockStatements()) {
            StructuredStatement structuredStatement = statement.getStatement();
            if (structuredStatement instanceof StructuredComment || structuredStatement.isEffectivelyNOP()) {
                continue;
            }
            if (!(structuredStatement instanceof StructuredCase)) {
                return null;
            }
            SwitchPatternCasePlanSpec planSpec = detectPlan(
                    (StructuredCase) structuredStatement,
                    bootstrapMatch,
                    patternLoop
            );
            if (planSpec == null) {
                return null;
            }
            plans.add(builder.build(planSpec));
        }
        return plans;
    }

    private SwitchPatternCasePlanSpec detectPlan(StructuredCase structuredCase,
                                                 SwitchPatternBootstrapMatch match,
                                                 StructuredWhile patternLoop) {
        List<Expression> values = structuredCase.getValues();
        if (values.isEmpty()) {
            return SwitchPatternCasePlanSpec.unchanged(structuredCase);
        }
        if (values.size() != 1) {
            return null;
        }
        Integer caseIndex = getCaseIndex(values.get(0));
        if (caseIndex == null) {
            return null;
        }
        if (caseIndex == -1) {
            return SwitchPatternCasePlanSpec.nullLiteral(structuredCase);
        }
        if (caseIndex < 0 || caseIndex >= match.getCaseTypes().size()) {
            return null;
        }
        if (!(structuredCase.getBody().getStatement() instanceof Block)) {
            return null;
        }
        Block bodyBlock = (Block) structuredCase.getBody().getStatement();
        SwitchPatternCaseContext context = new SwitchPatternCaseContext(
                structuredCase,
                bodyBlock,
                bodyBlock.getBlockStatements(),
                match.getSelector(),
                match.getCaseTypes().get(caseIndex),
                match.getIndexLValue(),
                patternLoop
        );
        return detector.detect(context);
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
        private final Expression rewrittenValue;
        private final LinkedList<Op04StructuredStatement> rewrittenStatements;

        CaseRewritePlan(StructuredCase structuredCase,
                        Block bodyBlock,
                        Expression rewrittenValue,
                        LinkedList<Op04StructuredStatement> rewrittenStatements) {
            this.structuredCase = structuredCase;
            this.bodyBlock = bodyBlock;
            this.rewrittenValue = rewrittenValue;
            this.rewrittenStatements = rewrittenStatements;
        }

        StructuredCase getStructuredCase() {
            return structuredCase;
        }

        Block getBodyBlock() {
            return bodyBlock;
        }

        Expression getRewrittenValue() {
            return rewrittenValue;
        }

        LinkedList<Op04StructuredStatement> getRewrittenStatements() {
            return rewrittenStatements;
        }
    }
}
