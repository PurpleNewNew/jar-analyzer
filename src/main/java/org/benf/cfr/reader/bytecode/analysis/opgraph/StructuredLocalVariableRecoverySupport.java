package org.benf.cfr.reader.bytecode.analysis.opgraph;

import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.AssignmentExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.CastExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.LValueExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.Literal;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.TernaryExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.LocalVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.AbstractExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueUsageCollectorSimple;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredAssignment;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredComment;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredDefinition;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredExpressionStatement;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.TypeConstants;
import org.benf.cfr.reader.util.collections.ListFactory;

import java.util.List;
import java.util.Objects;

final class StructuredLocalVariableRecoverySupport {
    private StructuredLocalVariableRecoverySupport() {
    }

    static boolean matchesLateResolvedLocal(LocalVariable expected, LocalVariable actual) {
        if (expected == null || actual == null) {
            return false;
        }
        if (hasConflictingReadableNames(expected, actual)) {
            return false;
        }
        if (expected.matchesReadableAlias(actual)) {
            return true;
        }
        if (!hasCompatibleLateResolvedType(expected, actual)) {
            return false;
        }
        if (expected.getIdx() >= 0
                && actual.getIdx() >= 0
                && expected.getIdx() == actual.getIdx()) {
            if (Objects.equals(expected.getIdent(), actual.getIdent())) {
                return true;
            }
            if (expected.getOriginalRawOffset() >= 0
                    && expected.getOriginalRawOffset() == actual.getOriginalRawOffset()) {
                return true;
            }
        }
        if (expected.getIdent() != null && expected.getIdent().equals(actual.getIdent())) {
            return true;
        }
        if (!expected.getName().isGoodName() || !actual.getName().isGoodName()) {
            return false;
        }
        String expectedName = expected.getName().getStringName();
        String actualName = actual.getName().getStringName();
        return expectedName != null && expectedName.equals(actualName);
    }

    private static boolean hasConflictingReadableNames(LocalVariable expected, LocalVariable actual) {
        if (expected.getName() == null
                || actual.getName() == null
                || !expected.getName().isGoodName()
                || !actual.getName().isGoodName()) {
            return false;
        }
        // Once both sides have real source-level names, a name mismatch is evidence that these are
        // different locals sharing a slot, not two views of the same late-resolved variable. Allowing
        // them to alias here caused declarations such as cFake/c to be merged only halfway.
        String expectedName = expected.getName().getStringName();
        String actualName = actual.getName().getStringName();
        return expectedName != null
                && actualName != null
                && !expectedName.equals(actualName);
    }

    static boolean isNullLike(Expression expression) {
        if (expression == null) {
            return false;
        }
        if (Literal.NULL.equals(expression)) {
            return true;
        }
        if (expression instanceof CastExpression) {
            return isNullLike(((CastExpression) expression).getChild());
        }
        if (expression instanceof TernaryExpression) {
            TernaryExpression ternary = (TernaryExpression) expression;
            return isNullLike(ternary.getLhs()) && isNullLike(ternary.getRhs());
        }
        return false;
    }

    static boolean expressionUsesLocal(Expression expression, LocalVariable localVariable) {
        if (expression == null || localVariable == null) {
            return false;
        }
        LValueUsageCollectorSimple collector = new LValueUsageCollectorSimple();
        expression.collectUsedLValues(collector);
        for (LValue used : collector.getUsedLValues()) {
            if (used instanceof LocalVariable
                    && matchesLateResolvedLocal(localVariable, (LocalVariable) used)) {
                return true;
            }
        }
        return false;
    }

    static boolean statementAccessesLocal(StructuredStatement statement, LocalVariable localVariable) {
        if (statement == null || localVariable == null) {
            return false;
        }
        LocalAccessCounter counter = new LocalAccessCounter(localVariable);
        List<StructuredStatement> linearized = ListFactory.newList();
        statement.linearizeInto(linearized);
        for (StructuredStatement part : linearized) {
            counter.recordDefinition(part);
            part.rewriteExpressions(counter);
            if (counter.hasAnyAccess()) {
                return true;
            }
        }
        return false;
    }

    static boolean statementWritesLocal(StructuredStatement statement, LocalVariable localVariable) {
        if (statement == null || localVariable == null) {
            return false;
        }
        LocalAccessCounter counter = new LocalAccessCounter(localVariable);
        List<StructuredStatement> linearized = ListFactory.newList();
        statement.linearizeInto(linearized);
        for (StructuredStatement part : linearized) {
            counter.recordDefinition(part);
            part.rewriteExpressions(counter);
            if (counter.hasAnyWrite()) {
                return true;
            }
        }
        return false;
    }

    static boolean statementTopLevelWritesLocal(StructuredStatement statement, LocalVariable localVariable) {
        if (statement == null || localVariable == null) {
            return false;
        }
        if (statement instanceof StructuredDefinition) {
            LValue defined = ((StructuredDefinition) statement).getLvalue();
            return defined instanceof LocalVariable
                    && matchesLateResolvedLocal(localVariable, (LocalVariable) defined);
        }
        if (statement instanceof StructuredAssignment) {
            LValue assigned = ((StructuredAssignment) statement).getLvalue();
            return assigned instanceof LocalVariable
                    && matchesLateResolvedLocal(localVariable, (LocalVariable) assigned);
        }
        if (statement instanceof StructuredExpressionStatement) {
            Expression expression = ((StructuredExpressionStatement) statement).getExpression();
            if (expression instanceof AssignmentExpression) {
                LValue updated = ((AssignmentExpression) expression).getUpdatedLValue();
                return updated instanceof LocalVariable
                        && matchesLateResolvedLocal(localVariable, (LocalVariable) updated);
            }
        }
        return false;
    }

    static boolean supportsEmbeddedAssignmentLift(StructuredStatement statement) {
        return statement instanceof StructuredAssignment
                || statement instanceof StructuredExpressionStatement;
    }

    static int nextMeaningfulIndex(List<Op04StructuredStatement> statements, int start) {
        for (int idx = start; idx < statements.size(); ++idx) {
            StructuredStatement statement = statements.get(idx).getStatement();
            if (!(statement instanceof StructuredComment)) {
                return idx;
            }
        }
        return -1;
    }

    static LocalVariable getDefinedLocal(StructuredStatement statement) {
        if (!(statement instanceof StructuredDefinition)) {
            return null;
        }
        LValue defined = ((StructuredDefinition) statement).getLvalue();
        return defined instanceof LocalVariable ? (LocalVariable) defined : null;
    }

    private static boolean hasCompatibleLateResolvedType(LocalVariable expected, LocalVariable actual) {
        JavaTypeInstance expectedType = expected.getInferredJavaType().getJavaTypeInstance();
        JavaTypeInstance actualType = actual.getInferredJavaType().getJavaTypeInstance();
        if (Objects.equals(expectedType, actualType)) {
            return true;
        }
        if (expectedType == null || actualType == null) {
            return true;
        }
        if ((expectedType == TypeConstants.OBJECT || actualType == TypeConstants.OBJECT)
                && (isPlaceholderLocal(expected) || isPlaceholderLocal(actual))) {
            return true;
        }
        return false;
    }

    private static boolean isPlaceholderLocal(LocalVariable localVariable) {
        if (localVariable == null) {
            return false;
        }
        if (localVariable.getIdx() < 0 || localVariable.getOriginalRawOffset() < 0) {
            return true;
        }
        return localVariable.getName() == null || !localVariable.getName().isGoodName();
    }

    static final class LiftedAssignmentConditionRewriter extends AbstractExpressionRewriter {
        private final AssignmentExpression target;
        private final LocalVariable replacement;

        LiftedAssignmentConditionRewriter(AssignmentExpression target, LocalVariable replacement) {
            this.target = target;
            this.replacement = replacement;
        }

        @Override
        public Expression rewriteExpression(Expression expression,
                                            SSAIdentifiers ssaIdentifiers,
                                            StatementContainer statementContainer,
                                            ExpressionRewriterFlags flags) {
            if (target.equals(expression)) {
                return new LValueExpression(replacement);
            }
            return super.rewriteExpression(expression, ssaIdentifiers, statementContainer, flags);
        }
    }

    static final class LocalAliasExpressionRewriter extends AbstractExpressionRewriter {
        private final LocalVariable alias;
        private final LocalVariable replacement;

        LocalAliasExpressionRewriter(LocalVariable alias, LocalVariable replacement) {
            this.alias = alias;
            this.replacement = replacement;
        }

        @Override
        public LValue rewriteExpression(LValue lValue,
                                        SSAIdentifiers ssaIdentifiers,
                                        StatementContainer statementContainer,
                                        ExpressionRewriterFlags flags) {
            if (alias.equals(lValue)) {
                return replacement;
            }
            return lValue;
        }
    }

    static final class AssignmentUseCounter extends AbstractExpressionRewriter {
        private final LValue needle;
        private int useCount;

        AssignmentUseCounter(LValue needle) {
            this.needle = needle;
        }

        @Override
        public LValue rewriteExpression(LValue lValue,
                                        SSAIdentifiers ssaIdentifiers,
                                        StatementContainer statementContainer,
                                        ExpressionRewriterFlags flags) {
            if (needle.equals(lValue)) {
                useCount++;
                return lValue;
            }
            if (needle instanceof LocalVariable
                    && lValue instanceof LocalVariable
                    && matchesLateResolvedLocal((LocalVariable) needle, (LocalVariable) lValue)) {
                useCount++;
            }
            return lValue;
        }

        boolean hasSingleUse() {
            return useCount == 1;
        }

        boolean hasAnyUse() {
            return useCount > 0;
        }
    }

    static final class LocalAccessCounter extends AbstractExpressionRewriter {
        private final LocalVariable needle;
        private int readCount;
        private int writeCount;

        LocalAccessCounter(LocalVariable needle) {
            this.needle = needle;
        }

        void recordDefinition(StructuredStatement statement) {
            if (statement == null) {
                return;
            }
            List<LValue> createdHere = statement.findCreatedHere();
            if (createdHere == null) {
                return;
            }
            for (LValue created : createdHere) {
                if (created instanceof LocalVariable
                        && matchesLateResolvedLocal(needle, (LocalVariable) created)) {
                    writeCount++;
                }
            }
        }

        @Override
        public LValue rewriteExpression(LValue lValue,
                                        SSAIdentifiers ssaIdentifiers,
                                        StatementContainer statementContainer,
                                        ExpressionRewriterFlags flags) {
            if (!(lValue instanceof LocalVariable) || !matchesLateResolvedLocal(needle, (LocalVariable) lValue)) {
                return lValue;
            }
            if (flags == ExpressionRewriterFlags.LVALUE) {
                writeCount++;
            } else {
                readCount++;
            }
            return lValue;
        }

        boolean hasAnyAccess() {
            return readCount > 0 || writeCount > 0;
        }

        boolean hasAnyWrite() {
            return writeCount > 0;
        }
    }

    static final class InlineAssignmentExpressionRewriter extends AbstractExpressionRewriter {
        private final LValue target;
        private final AssignmentExpression replacement;
        private boolean replaced;

        InlineAssignmentExpressionRewriter(LValue target, AssignmentExpression replacement) {
            this.target = target;
            this.replacement = replacement;
        }

        @Override
        public Expression rewriteExpression(Expression expression,
                                            SSAIdentifiers ssaIdentifiers,
                                            StatementContainer statementContainer,
                                            ExpressionRewriterFlags flags) {
            if (replaced) {
                return expression;
            }
            if (expression instanceof LValueExpression) {
                LValueExpression lValueExpression = (LValueExpression) expression;
                if (target.equals(lValueExpression.getLValue())) {
                    replaced = true;
                    return replacement;
                }
            }
            return super.rewriteExpression(expression, ssaIdentifiers, statementContainer, flags);
        }

        @Override
        public LValue rewriteExpression(LValue lValue,
                                        SSAIdentifiers ssaIdentifiers,
                                        StatementContainer statementContainer,
                                        ExpressionRewriterFlags flags) {
            return lValue;
        }
    }
}
