package org.benf.cfr.reader.bytecode.analysis.opgraph;

import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.AssignmentExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.BoolOp;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.BooleanOperation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ConditionalExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.LocalVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.AbstractExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueUsageCollectorSimple;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredScope;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.expression.StructuredStatementExpression;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.Block;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredAssignment;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredComment;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredDefinition;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredDo;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredIf;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredWhile;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.collections.MapFactory;

import java.util.List;
import java.util.Map;

final class StructuredConditionLocalRecoveryPasses {
    private StructuredConditionLocalRecoveryPasses() {
    }

    static void restoreLiftableDefinitionAssignments(Op04StructuredStatement root) {
        root.transform(new DefinitionIfAssignmentRestorer(), new StructuredScope());
    }

    static void sinkDefinitionsToFirstUse(Op04StructuredStatement root) {
        root.transform(new DefinitionFirstUseSinker(), new StructuredScope());
    }

    private static final class DefinitionIfAssignmentRestorer implements org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.StructuredStatementTransformer {
        @Override
        public StructuredStatement transform(StructuredStatement in, StructuredScope scope) {
            in.transformStructuredChildren(this, scope);
            in.rewriteExpressions(new NestedConditionExpressionTransformer());
            if (!(in instanceof Block)) {
                return in;
            }
            List<Op04StructuredStatement> statements = ((Block) in).getBlockStatements();
            for (int idx = 0; idx < statements.size(); ++idx) {
                StructuredStatement statement = statements.get(idx).getStatement();
                if (statement instanceof StructuredComment) {
                    continue;
                }
                restoreLiftablePrefix(statement, statements, idx);
            }
            return in;
        }

        private void restoreLiftablePrefix(StructuredStatement targetStatement,
                                           List<Op04StructuredStatement> statements,
                                           int statementIndex) {
            int start = statementIndex - 1;
            while (start >= 0) {
                StructuredStatement statement = statements.get(start).getStatement();
                if (statement instanceof StructuredComment) {
                    start--;
                    continue;
                }
                if (getLiftablePrefixCandidate(statement, start) != null) {
                    start--;
                    continue;
                }
                break;
            }
            if (start == statementIndex - 1) {
                return;
            }
            List<LiftablePrefixCandidate> candidates = ListFactory.newList();
            for (int idx = start + 1; idx < statementIndex; ++idx) {
                StructuredStatement candidateStatement = statements.get(idx).getStatement();
                if (candidateStatement instanceof StructuredComment) {
                    continue;
                }
                LiftablePrefixCandidate candidate = getLiftablePrefixCandidate(candidateStatement, idx);
                if (candidate != null) {
                    candidates.add(candidate);
                }
            }
            if (candidates.isEmpty()) {
                return;
            }
            if (isConditionalStatement(targetStatement)
                    && usesAnyCandidateAfterTarget(statements, statementIndex + 1, candidates)) {
                return;
            }
            if (usesAnyCandidateInsideDeferredContext(targetStatement, candidates)) {
                return;
            }
            ConditionAssignmentCollector collector = new ConditionAssignmentCollector(candidates);
            targetStatement.rewriteExpressions(collector);
            collector.bindUnmatchedReverseDefinitions();
            if (!collector.isSafeToLift()) {
                return;
            }
            for (LiftablePrefixCandidate candidate : candidates) {
                AssignmentExpression lifted = collector.getSingleAssignment(candidate.localVariable);
                if (lifted == null) {
                    continue;
                }
                statements.set(candidate.index, new Op04StructuredStatement(
                        new StructuredAssignment(lifted.getCombinedLoc(), candidate.localVariable, lifted.getrValue(), true)
                ));
                targetStatement.rewriteExpressions(new StructuredLocalVariableRecoverySupport.LiftedAssignmentConditionRewriter(
                        lifted,
                        candidate.localVariable
                ));
            }
        }

        private boolean isConditionalStatement(StructuredStatement statement) {
            return statement instanceof StructuredIf
                    || statement instanceof StructuredWhile
                    || statement instanceof StructuredDo;
        }

        private boolean usesAnyCandidateAfterTarget(List<Op04StructuredStatement> statements,
                                                    int start,
                                                    List<LiftablePrefixCandidate> candidates) {
            for (LiftablePrefixCandidate candidate : candidates) {
                if (findFirstUseAfter(statements, start, candidate.localVariable) >= 0) {
                    return true;
                }
            }
            return false;
        }

        private int findFirstUseAfter(List<Op04StructuredStatement> statements, int start, LocalVariable localVariable) {
            for (int idx = start; idx < statements.size(); ++idx) {
                StructuredStatement statement = statements.get(idx).getStatement();
                if (statement instanceof StructuredComment) {
                    continue;
                }
                if (usesLocal(statement, localVariable)) {
                    return idx;
                }
            }
            return -1;
        }

        private boolean usesLocal(StructuredStatement statement, LocalVariable localVariable) {
            if (statement == null || localVariable == null) {
                return false;
            }
            StructuredLocalVariableRecoverySupport.AssignmentUseCounter counter =
                    new StructuredLocalVariableRecoverySupport.AssignmentUseCounter(localVariable);
            List<StructuredStatement> linearized = ListFactory.newList();
            statement.linearizeInto(linearized);
            for (StructuredStatement part : linearized) {
                part.rewriteExpressions(counter);
                if (counter.hasAnyUse()) {
                    return true;
                }
            }
            return false;
        }

        private boolean usesAnyCandidateInsideDeferredContext(StructuredStatement statement,
                                                              List<LiftablePrefixCandidate> candidates) {
            for (LiftablePrefixCandidate candidate : candidates) {
                if (usesLocalInsideDeferredContext(statement, candidate.localVariable)) {
                    return true;
                }
            }
            return false;
        }

        private boolean usesLocalInsideDeferredContext(StructuredStatement statement, LocalVariable localVariable) {
            if (statement == null || localVariable == null) {
                return false;
            }
            DeferredLocalUseCollector collector = new DeferredLocalUseCollector(localVariable);
            statement.rewriteExpressions(collector);
            return collector.hasDeferredUse();
        }

        private LiftablePrefixCandidate getLiftablePrefixCandidate(StructuredStatement statement, int index) {
            if (statement instanceof StructuredDefinition) {
                LValue defined = ((StructuredDefinition) statement).getLvalue();
                if (defined instanceof LocalVariable) {
                    return new LiftablePrefixCandidate((LocalVariable) defined, index);
                }
            }
            if (!(statement instanceof StructuredAssignment)) {
                return null;
            }
            StructuredAssignment assignment = (StructuredAssignment) statement;
            if (!(assignment.getLvalue() instanceof LocalVariable)) {
                return null;
            }
            LocalVariable localVariable = (LocalVariable) assignment.getLvalue();
            return StructuredLocalVariableRecoverySupport.isNullLike(assignment.getRvalue())
                    ? new LiftablePrefixCandidate(localVariable, index)
                    : null;
        }
    }

    private static class DefinitionFirstUseSinker implements org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.StructuredStatementTransformer {
        @Override
        public StructuredStatement transform(StructuredStatement in, StructuredScope scope) {
            in.transformStructuredChildren(this, scope);
            if (!(in instanceof Block)) {
                return in;
            }
            List<Op04StructuredStatement> statements = ((Block) in).getBlockStatements();
            for (int idx = statements.size() - 2; idx >= 0; --idx) {
                StructuredStatement statement = statements.get(idx).getStatement();
                LocalVariable localVariable = getSinkableLocal(statement);
                if (localVariable == null) {
                    continue;
                }
                if (promoteFirstWriteCreator(statements, idx, localVariable)) {
                    continue;
                }
                int firstUse = findFirstUseIndex(statements, idx + 1, localVariable);
                if (firstUse <= idx + 1) {
                    continue;
                }
                Op04StructuredStatement moved = statements.remove(idx);
                statements.add(firstUse - 1, moved);
            }
            return in;
        }

        private boolean promoteFirstWriteCreator(List<Op04StructuredStatement> statements,
                                                 int creatorIndex,
                                                 LocalVariable localVariable) {
            StructuredStatement creatorStatement = statements.get(creatorIndex).getStatement();
            if (!isPromotableDeclarationOrigin(creatorStatement, localVariable)) {
                return false;
            }
            PromotableWriteSearch promoted = findFirstPromotableWrite(statements, creatorIndex + 1, localVariable);
            if (!promoted.hasAssignment()) {
                return false;
            }
            promoted.replaceWithCreator(statements, creatorIndex, localVariable);
            return true;
        }

        private boolean isPromotableDeclarationOrigin(StructuredStatement statement, LocalVariable localVariable) {
            if (statement instanceof StructuredDefinition) {
                LValue lValue = ((StructuredDefinition) statement).getLvalue();
                return lValue instanceof LocalVariable
                        && StructuredLocalVariableRecoverySupport.matchesLateResolvedLocal(localVariable, (LocalVariable) lValue);
            }
            if (!(statement instanceof StructuredAssignment)) {
                return false;
            }
            StructuredAssignment assignment = (StructuredAssignment) statement;
            return assignment.isCreator(localVariable)
                    && StructuredLocalVariableRecoverySupport.isNullLike(assignment.getRvalue());
        }

        private PromotableWriteSearch findFirstPromotableWrite(List<Op04StructuredStatement> statements,
                                                                int start,
                                                                LocalVariable localVariable) {
            for (int idx = start; idx < statements.size(); ++idx) {
                Op04StructuredStatement statementContainer = statements.get(idx);
                StructuredStatement statement = statementContainer.getStatement();
                if (statement instanceof StructuredComment) {
                    continue;
                }
                StructuredAssignment promoted = getPromotableTopLevelWrite(statement, localVariable);
                if (promoted != null) {
                    return PromotableWriteSearch.assignment(statements, idx, promoted);
                }
                if (statement instanceof Block) {
                    PromotableWriteSearch nested = findFirstPromotableWrite(((Block) statement).getBlockStatements(), 0, localVariable);
                    if (nested.hasAssignment() || nested.isBlocked()) {
                        return nested;
                    }
                }
                if (StructuredLocalVariableRecoverySupport.statementAccessesLocal(statement, localVariable)
                        || StructuredLocalVariableRecoverySupport.statementTopLevelWritesLocal(statement, localVariable)) {
                    return PromotableWriteSearch.blocked();
                }
            }
            return PromotableWriteSearch.none();
        }

        private StructuredAssignment getPromotableTopLevelWrite(StructuredStatement statement, LocalVariable localVariable) {
            if (!(statement instanceof StructuredAssignment)) {
                return null;
            }
            StructuredAssignment assignment = (StructuredAssignment) statement;
            if (!(assignment.getLvalue() instanceof LocalVariable)) {
                return null;
            }
            LocalVariable assigned = (LocalVariable) assignment.getLvalue();
            if (!StructuredLocalVariableRecoverySupport.matchesLateResolvedLocal(localVariable, assigned)) {
                return null;
            }
            if (StructuredLocalVariableRecoverySupport.expressionUsesLocal(assignment.getRvalue(), localVariable)
                    || usesAnyLocal(assignment.getRvalue())) {
                return null;
            }
            return assignment;
        }

        private boolean usesAnyLocal(Expression expression) {
            if (expression == null) {
                return false;
            }
            LValueUsageCollectorSimple collector = new LValueUsageCollectorSimple();
            expression.collectUsedLValues(collector);
            for (LValue used : collector.getUsedLValues()) {
                if (used instanceof LocalVariable) {
                    return true;
                }
            }
            return false;
        }

        private LocalVariable getSinkableLocal(StructuredStatement statement) {
            if (statement instanceof StructuredDefinition) {
                LValue lValue = ((StructuredDefinition) statement).getLvalue();
                return lValue instanceof LocalVariable ? (LocalVariable) lValue : null;
            }
            if (!(statement instanceof StructuredAssignment)) {
                return null;
            }
            StructuredAssignment assignment = (StructuredAssignment) statement;
            if (!(assignment.getLvalue() instanceof LocalVariable)) {
                return null;
            }
            LocalVariable localVariable = (LocalVariable) assignment.getLvalue();
            return assignment.isCreator(localVariable)
                    && StructuredLocalVariableRecoverySupport.isNullLike(assignment.getRvalue())
                    ? localVariable
                    : null;
        }

        private int findFirstUseIndex(List<Op04StructuredStatement> statements, int start, LocalVariable localVariable) {
            for (int idx = start; idx < statements.size(); ++idx) {
                StructuredStatement statement = statements.get(idx).getStatement();
                if (statement instanceof StructuredComment) {
                    continue;
                }
                if (usesLocal(statement, localVariable)) {
                    return idx;
                }
            }
            return -1;
        }

        private boolean usesLocal(StructuredStatement statement, LocalVariable localVariable) {
            if (statement == null || localVariable == null) {
                return false;
            }
            StructuredLocalVariableRecoverySupport.AssignmentUseCounter counter =
                    new StructuredLocalVariableRecoverySupport.AssignmentUseCounter(localVariable);
            List<StructuredStatement> linearized = ListFactory.newList();
            statement.linearizeInto(linearized);
            for (StructuredStatement part : linearized) {
                part.rewriteExpressions(counter);
                if (counter.hasAnyUse()) {
                    return true;
                }
            }
            return false;
        }

        private static final class PromotableWriteSearch {
            private static final PromotableWriteSearch NONE = new PromotableWriteSearch(null, -1, null, false);
            private static final PromotableWriteSearch BLOCKED = new PromotableWriteSearch(null, -1, null, true);

            private final List<Op04StructuredStatement> statements;
            private final int index;
            private final StructuredAssignment assignment;
            private final boolean blocked;

            private PromotableWriteSearch(List<Op04StructuredStatement> statements,
                                          int index,
                                          StructuredAssignment assignment,
                                          boolean blocked) {
                this.statements = statements;
                this.index = index;
                this.assignment = assignment;
                this.blocked = blocked;
            }

            private static PromotableWriteSearch none() {
                return NONE;
            }

            private static PromotableWriteSearch blocked() {
                return BLOCKED;
            }

            private static PromotableWriteSearch assignment(List<Op04StructuredStatement> statements,
                                                            int index,
                                                            StructuredAssignment assignment) {
                return new PromotableWriteSearch(statements, index, assignment, false);
            }

            private boolean hasAssignment() {
                return assignment != null;
            }

            private boolean isBlocked() {
                return blocked;
            }

            private void replaceWithCreator(List<Op04StructuredStatement> ownerStatements,
                                            int ownerIndex,
                                            LocalVariable localVariable) {
                ownerStatements.set(ownerIndex, new Op04StructuredStatement(
                        new StructuredAssignment(assignment.getCombinedLoc(), localVariable, assignment.getRvalue(), true)
                ));
                statements.remove(index);
            }
        }
    }

    private static final class LiftablePrefixCandidate {
        private final LocalVariable localVariable;
        private final int index;

        private LiftablePrefixCandidate(LocalVariable localVariable, int index) {
            this.localVariable = localVariable;
            this.index = index;
        }
    }

    private static final class ConditionAssignmentCollector extends AbstractExpressionRewriter {
        private final List<LiftablePrefixCandidate> candidates;
        private final Map<LocalVariable, List<AssignmentExpression>> assignments = MapFactory.newIdentityMap();
        private final List<AssignmentExpression> unmatchedAssignments = ListFactory.newList();
        private boolean unsafeBool;
        private boolean sawUnexpectedAssignment;

        private ConditionAssignmentCollector(List<LiftablePrefixCandidate> candidates) {
            this.candidates = candidates;
        }

        @Override
        public Expression rewriteExpression(Expression expression,
                                            SSAIdentifiers ssaIdentifiers,
                                            StatementContainer statementContainer,
                                            ExpressionRewriterFlags flags) {
            if (expression instanceof BooleanOperation
                    && ((BooleanOperation) expression).getOp() != BoolOp.AND) {
                unsafeBool = true;
            }
            if (expression instanceof AssignmentExpression) {
                recordAssignment((AssignmentExpression) expression);
            }
            return super.rewriteExpression(expression, ssaIdentifiers, statementContainer, flags);
        }

        private void recordAssignment(AssignmentExpression assignmentExpression) {
            if (!(assignmentExpression.getUpdatedLValue() instanceof LocalVariable)) {
                sawUnexpectedAssignment = true;
                return;
            }
            LocalVariable updated = (LocalVariable) assignmentExpression.getUpdatedLValue();
            LocalVariable candidate = findCandidate(updated);
            if (candidate == null) {
                unmatchedAssignments.add(assignmentExpression);
                return;
            }
            List<AssignmentExpression> matches = assignments.get(candidate);
            if (matches == null) {
                matches = ListFactory.newList();
                assignments.put(candidate, matches);
            }
            matches.add(assignmentExpression);
        }

        private LocalVariable findCandidate(LocalVariable updated) {
            LocalVariable nameMatch = null;
            for (LiftablePrefixCandidate candidate : candidates) {
                if (StructuredLocalVariableRecoverySupport.matchesLateResolvedLocal(candidate.localVariable, updated)) {
                    return candidate.localVariable;
                }
                if (candidate.localVariable.getName().isGoodName()
                        && updated.getName().isGoodName()
                        && candidate.localVariable.getName().getStringName().equals(updated.getName().getStringName())) {
                    if (nameMatch != null) {
                        return null;
                    }
                    nameMatch = candidate.localVariable;
                }
            }
            return nameMatch;
        }

        private void bindUnmatchedReverseDefinitions() {
            if (unsafeBool || sawUnexpectedAssignment || unmatchedAssignments.isEmpty()) {
                return;
            }
            if (!assignments.isEmpty() || unmatchedAssignments.size() != candidates.size()) {
                return;
            }
            for (LiftablePrefixCandidate candidate : candidates) {
                if (candidate.index < 0) {
                    return;
                }
            }
            for (int idx = 0; idx < candidates.size(); ++idx) {
                LiftablePrefixCandidate candidate = candidates.get(candidates.size() - 1 - idx);
                assignments.put(candidate.localVariable, ListFactory.newImmutableList(unmatchedAssignments.get(idx)));
            }
            unmatchedAssignments.clear();
        }

        private boolean isSafeToLift() {
            if (unsafeBool || sawUnexpectedAssignment || !unmatchedAssignments.isEmpty() || assignments.isEmpty()) {
                return false;
            }
            for (Map.Entry<LocalVariable, List<AssignmentExpression>> entry : assignments.entrySet()) {
                if (entry.getValue().size() != 1) {
                    return false;
                }
            }
            return true;
        }

        private AssignmentExpression getSingleAssignment(LocalVariable localVariable) {
            List<AssignmentExpression> matches = assignments.get(localVariable);
            return matches == null || matches.size() != 1 ? null : matches.get(0);
        }
    }

    private static final class NestedConditionExpressionTransformer extends AbstractExpressionRewriter {
        @Override
        public Expression rewriteExpression(Expression expression,
                                            SSAIdentifiers ssaIdentifiers,
                                            StatementContainer statementContainer,
                                            ExpressionRewriterFlags flags) {
            if (expression instanceof StructuredStatementExpression) {
                restoreNested(((StructuredStatementExpression) expression).getContent());
                return expression;
            }
            if (expression instanceof org.benf.cfr.reader.bytecode.analysis.parse.expression.LambdaExpression) {
                Expression result = ((org.benf.cfr.reader.bytecode.analysis.parse.expression.LambdaExpression) expression).getResult();
                if (result instanceof StructuredStatementExpression) {
                    restoreNested(((StructuredStatementExpression) result).getContent());
                }
                return expression;
            }
            return super.rewriteExpression(expression, ssaIdentifiers, statementContainer, flags);
        }

        private void restoreNested(StructuredStatement content) {
            if (content == null) {
                return;
            }
            Op04StructuredStatement container = content.getContainer();
            if (container == null) {
                container = new Op04StructuredStatement(content);
            }
            restoreLiftableDefinitionAssignments(container);
        }
    }

    private static final class DeferredLocalUseCollector extends AbstractExpressionRewriter {
        private final LocalVariable localVariable;
        private boolean deferredUse;

        private DeferredLocalUseCollector(LocalVariable localVariable) {
            this.localVariable = localVariable;
        }

        private boolean hasDeferredUse() {
            return deferredUse;
        }

        @Override
        public Expression rewriteExpression(Expression expression,
                                            SSAIdentifiers ssaIdentifiers,
                                            StatementContainer statementContainer,
                                            ExpressionRewriterFlags flags) {
            if (deferredUse) {
                return expression;
            }
            if (expression instanceof StructuredStatementExpression) {
                deferredUse = structuredStatementUsesLocal(((StructuredStatementExpression) expression).getContent(), localVariable);
                return expression;
            }
            if (expression instanceof org.benf.cfr.reader.bytecode.analysis.parse.expression.LambdaExpression) {
                Expression result = ((org.benf.cfr.reader.bytecode.analysis.parse.expression.LambdaExpression) expression).getResult();
                deferredUse = expressionUsesLocal(result, localVariable);
                return expression;
            }
            return super.rewriteExpression(expression, ssaIdentifiers, statementContainer, flags);
        }

        private boolean structuredStatementUsesLocal(StructuredStatement content, LocalVariable localVariable) {
            if (content == null) {
                return false;
            }
            StructuredLocalVariableRecoverySupport.AssignmentUseCounter counter =
                    new StructuredLocalVariableRecoverySupport.AssignmentUseCounter(localVariable);
            List<StructuredStatement> linearized = ListFactory.newList();
            content.linearizeInto(linearized);
            for (StructuredStatement part : linearized) {
                part.rewriteExpressions(counter);
                if (counter.hasAnyUse()) {
                    return true;
                }
            }
            return false;
        }

        private boolean expressionUsesLocal(Expression expression, LocalVariable localVariable) {
            if (expression == null) {
                return false;
            }
            StructuredLocalVariableRecoverySupport.AssignmentUseCounter counter =
                    new StructuredLocalVariableRecoverySupport.AssignmentUseCounter(localVariable);
            expression.applyExpressionRewriter(counter, null, null, ExpressionRewriterFlags.RVALUE);
            return counter.hasAnyUse();
        }
    }
}
