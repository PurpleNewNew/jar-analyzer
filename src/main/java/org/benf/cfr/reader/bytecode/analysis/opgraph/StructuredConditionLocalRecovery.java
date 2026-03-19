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
import org.benf.cfr.reader.bytecode.analysis.structured.expression.StructuredStatementExpression;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.Block;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredAssignment;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredDefinition;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredIf;
import org.benf.cfr.reader.bytecode.analysis.types.RawJavaType;
import org.benf.cfr.reader.util.collections.ListFactory;

import java.util.List;

public final class StructuredConditionLocalRecovery {
    private StructuredConditionLocalRecovery() {
    }

    public static void rewriteConditionLocalAliases(Op04StructuredStatement root) {
        root.transform(new ConditionLocalAliasCleaner(), new StructuredScope());
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
            if (assignment.getLvalue() instanceof LocalVariable
                    && ((LocalVariable) assignment.getLvalue()).getName().isGoodName()) {
                LocalVariable localVariable = (LocalVariable) assignment.getLvalue();
                if (localVariable.getInferredJavaType().getJavaTypeInstance() != RawJavaType.BOOLEAN) {
                    return false;
                }
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
}
