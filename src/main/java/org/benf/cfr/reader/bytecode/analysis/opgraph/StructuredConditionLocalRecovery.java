package org.benf.cfr.reader.bytecode.analysis.opgraph;

import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.StructuredStatementTransformer;
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
import org.benf.cfr.reader.bytecode.analysis.structured.statement.Block;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredAssignment;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredComment;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredDefinition;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredIf;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.collections.MapFactory;

import java.util.List;
import java.util.Map;

final class StructuredConditionLocalRecovery {
    private StructuredConditionLocalRecovery() {
    }

    static void rewriteConditionLocalAliases(Op04StructuredStatement root) {
        root.transform(new ConditionLocalAliasCleaner(), new StructuredScope());
    }

    static void restoreLiftableDefinitionAssignments(Op04StructuredStatement root) {
        root.transform(new DefinitionIfAssignmentRestorer(), new StructuredScope());
    }

    static void sinkDefinitionsToFirstUse(Op04StructuredStatement root) {
        root.transform(new DefinitionFirstUseSinker(), new StructuredScope());
    }

    private static class ConditionLocalAliasCleaner implements StructuredStatementTransformer {
        @Override
        public StructuredStatement transform(StructuredStatement in, StructuredScope scope) {
            in.transformStructuredChildren(this, scope);
            if (!(in instanceof Block)) {
                return in;
            }
            List<Op04StructuredStatement> statements = ((Block) in).getBlockStatements();
            for (int x = 0; x < statements.size() - 1; ++x) {
                Op04StructuredStatement currentContainer = statements.get(x);
                Op04StructuredStatement nextContainer = statements.get(x + 1);
                StructuredStatement current = currentContainer.getStatement();
                StructuredStatement next = nextContainer.getStatement();
                if (!(current instanceof StructuredAssignment) || !(next instanceof StructuredIf)) {
                    continue;
                }
                StructuredAssignment assignment = (StructuredAssignment) current;
                StructuredIf structuredIf = (StructuredIf) next;
                if (assignment.getLvalue() instanceof LocalVariable) {
                    LocalVariable assigned = (LocalVariable) assignment.getLvalue();
                    LocalVariable alias = findEquivalentConditionLocal(assigned, structuredIf.getConditionalExpression());
                    if (alias != null) {
                        structuredIf.rewriteExpressions(new StructuredLocalVariableRecoverySupport.LocalAliasExpressionRewriter(alias, assigned));
                    }
                }
                if (!shouldInlineAssignment(assignment, structuredIf.getConditionalExpression())
                        || isUsedAfterIf(statements, x + 2, assignment.getLvalue())) {
                    continue;
                }
                structuredIf.rewriteExpressions(new StructuredLocalVariableRecoverySupport.InlineAssignmentExpressionRewriter(
                        assignment.getLvalue(),
                        new AssignmentExpression(BytecodeLoc.TODO, assignment.getLvalue(), assignment.getRvalue())
                ));
                if (assignment.isCreator(assignment.getLvalue())) {
                    statements.set(x, new Op04StructuredStatement(new StructuredDefinition(assignment.getLvalue())));
                } else {
                    bypassRemovedAssignment(currentContainer, nextContainer);
                    statements.remove(x);
                    --x;
                }
            }
            return in;
        }

        private LocalVariable findEquivalentConditionLocal(LocalVariable assigned, ConditionalExpression condition) {
            LValueUsageCollectorSimple collector = new LValueUsageCollectorSimple();
            condition.collectUsedLValues(collector);
            LocalVariable match = null;
            LocalVariable placeholderMatch = null;
            for (LValue used : collector.getUsedLValues()) {
                if (!(used instanceof LocalVariable)) {
                    continue;
                }
                LocalVariable candidate = (LocalVariable) used;
                if (candidate.equals(assigned)) {
                    continue;
                }
                if (!candidate.matchesReadableAlias(assigned)) {
                    if (!isPlaceholderConditionLocal(candidate, assigned)) {
                        continue;
                    }
                    if (placeholderMatch != null && !placeholderMatch.equals(candidate)) {
                        return null;
                    }
                    placeholderMatch = candidate;
                    continue;
                }
                if (match != null && !match.equals(candidate)) {
                    return null;
                }
                match = candidate;
            }
            return match != null ? match : placeholderMatch;
        }

        private boolean isPlaceholderConditionLocal(LocalVariable candidate, LocalVariable assigned) {
            return candidate.getIdx() == -1
                    && candidate.getOriginalRawOffset() == -1
                    && candidate.getIdent() == null
                    && assigned.getInferredJavaType().getJavaTypeInstance().equals(candidate.getInferredJavaType().getJavaTypeInstance());
        }

        private boolean shouldInlineAssignment(StructuredAssignment assignment, ConditionalExpression condition) {
            if (assignment.isCreator(assignment.getLvalue())
                    && assignment.getLvalue() instanceof LocalVariable
                    && ((LocalVariable) assignment.getLvalue()).getName().isGoodName()) {
                return false;
            }
            StructuredLocalVariableRecoverySupport.AssignmentUseCounter counter =
                    new StructuredLocalVariableRecoverySupport.AssignmentUseCounter(assignment.getLvalue());
            counter.rewriteExpression(condition, null, null, ExpressionRewriterFlags.RVALUE);
            return counter.hasSingleUse();
        }

        private boolean isUsedAfterIf(List<Op04StructuredStatement> statements, int fromIdx, LValue lValue) {
            if (statements == null || lValue == null) {
                return false;
            }
            StructuredLocalVariableRecoverySupport.AssignmentUseCounter counter =
                    new StructuredLocalVariableRecoverySupport.AssignmentUseCounter(lValue);
            for (int idx = Math.max(0, fromIdx); idx < statements.size(); ++idx) {
                Op04StructuredStatement statement = statements.get(idx);
                if (statement == null || statement.getStatement() == null) {
                    continue;
                }
                statement.getStatement().rewriteExpressions(counter);
                if (counter.hasAnyUse()) {
                    return true;
                }
            }
            return false;
        }

        private void bypassRemovedAssignment(Op04StructuredStatement assignmentContainer,
                                             Op04StructuredStatement nextContainer) {
            if (assignmentContainer == null || nextContainer == null || assignmentContainer == nextContainer) {
                return;
            }
            List<Op04StructuredStatement> incoming = ListFactory.newList(assignmentContainer.getSources());
            for (Op04StructuredStatement source : incoming) {
                source.replaceTarget(assignmentContainer, nextContainer);
                if (!nextContainer.getSources().contains(source)) {
                    nextContainer.addSource(source);
                }
            }
            while (nextContainer.getSources().remove(assignmentContainer)) {
                // Remove the detached assignment node from the fallthrough chain.
            }
            assignmentContainer.setSources(ListFactory.<Op04StructuredStatement>newList());
            assignmentContainer.setTargets(ListFactory.<Op04StructuredStatement>newList());
        }
    }

    private static class DefinitionIfAssignmentRestorer implements StructuredStatementTransformer {
        @Override
        public StructuredStatement transform(StructuredStatement in, StructuredScope scope) {
            in.transformStructuredChildren(this, scope);
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
            if (!assignment.isCreator(localVariable)) {
                return null;
            }
            return StructuredLocalVariableRecoverySupport.isNullLike(assignment.getRvalue())
                    ? new LiftablePrefixCandidate(localVariable, index)
                    : null;
        }
    }

    private static class DefinitionFirstUseSinker implements StructuredStatementTransformer {
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
                int firstUse = findFirstUseIndex(statements, idx + 1, localVariable);
                if (firstUse <= idx + 1) {
                    continue;
                }
                Op04StructuredStatement moved = statements.remove(idx);
                statements.add(firstUse - 1, moved);
            }
            return in;
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
}
