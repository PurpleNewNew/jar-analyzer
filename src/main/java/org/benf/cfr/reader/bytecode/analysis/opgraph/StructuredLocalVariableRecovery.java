package org.benf.cfr.reader.bytecode.analysis.opgraph;

import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.StructuredStatementTransformer;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.*;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.LocalVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.AbstractExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.DefaultEquivalenceConstraint;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueUsageCollectorSimple;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredScope;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.expression.StructuredStatementExpression;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.*;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.TypeConstants;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.collections.MapFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class StructuredLocalVariableRecovery {
    private StructuredLocalVariableRecovery() {
    }

    public static void rewriteConditionLocalAliases(Op04StructuredStatement root) {
        root.transform(new ConditionLocalAliasCleaner(), new StructuredScope());
    }

    public static void restoreLiftableDefinitionAssignments(Op04StructuredStatement root) {
        root.transform(new DefinitionIfAssignmentRestorer(), new StructuredScope());
    }

    public static void restoreCreatorDependencyOrder(Op04StructuredStatement root) {
        root.transform(new CreatorDependencyOrderRestorer(), new StructuredScope());
    }

    public static void restoreCreatorsBeforeFirstUse(Op04StructuredStatement root) {
        root.transform(new CreatorFirstUseRestorer(), new StructuredScope());
    }

    public static void restoreTrailingCreatorAssignments(Op04StructuredStatement root) {
        root.transform(new TrailingCreatorAssignmentRestorer(), new StructuredScope());
    }

    public static void restorePrefixEmbeddedAssignments(Op04StructuredStatement root) {
        root.transform(new PrefixEmbeddedAssignmentRestorer(), new StructuredScope());
    }

    public static void sinkDefinitionsToFirstUse(Op04StructuredStatement root) {
        root.transform(new DefinitionFirstUseSinker(), new StructuredScope());
    }

    public static void applyOutputRecovery(Op04StructuredStatement root) {
        sinkDefinitionsToFirstUse(root);
        restoreCreatorDependencyOrder(root);
        restoreCreatorsBeforeFirstUse(root);
        restoreLiftableDefinitionAssignments(root);
        restoreTrailingCreatorAssignments(root);
        restorePrefixEmbeddedAssignments(root);
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
                        structuredIf.rewriteExpressions(new LocalAliasExpressionRewriter(alias, assigned));
                    }
                }
                if (!shouldInlineAssignment(assignment, structuredIf.getConditionalExpression())
                        || isUsedAfterIf(statements, x + 2, assignment.getLvalue())) {
                    continue;
                }
                structuredIf.rewriteExpressions(new InlineAssignmentExpressionRewriter(
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
            AssignmentUseCounter counter = new AssignmentUseCounter(assignment.getLvalue());
            counter.rewriteExpression(condition, null, null, ExpressionRewriterFlags.RVALUE);
            return counter.useCount == 1;
        }

        private boolean isUsedAfterIf(List<Op04StructuredStatement> statements, int fromIdx, LValue lValue) {
            if (statements == null || lValue == null) {
                return false;
            }
            AssignmentUseCounter counter = new AssignmentUseCounter(lValue);
            for (int idx = Math.max(0, fromIdx); idx < statements.size(); ++idx) {
                Op04StructuredStatement statement = statements.get(idx);
                if (statement == null || statement.getStatement() == null) {
                    continue;
                }
                statement.getStatement().rewriteExpressions(counter);
                if (counter.useCount > 0) {
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
                targetStatement.rewriteExpressions(new LiftedAssignmentConditionRewriter(lifted, candidate.localVariable));
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
            return isNullLike(assignment.getRvalue()) ? new LiftablePrefixCandidate(localVariable, index) : null;
        }
    }

    private static class CreatorDependencyOrderRestorer implements StructuredStatementTransformer {
        @Override
        public StructuredStatement transform(StructuredStatement in, StructuredScope scope) {
            in.transformStructuredChildren(this, scope);
            if (!(in instanceof Block)) {
                return in;
            }
            List<Op04StructuredStatement> statements = ((Block) in).getBlockStatements();
            boolean changed;
            do {
                changed = false;
                for (int idx = 0; idx < statements.size() - 1; ++idx) {
                    StructuredStatement currentStatement = statements.get(idx).getStatement();
                    if (currentStatement instanceof StructuredComment) {
                        continue;
                    }
                    LiftableCreatorAssignment current = getCreatorAssignment(currentStatement);
                    if (current == null) {
                        continue;
                    }
                    int moveTo = findMoveTarget(statements, idx, current);
                    if (moveTo < 0) {
                        continue;
                    }
                    Op04StructuredStatement moved = statements.remove(idx);
                    statements.add(moveTo, moved);
                    changed = true;
                    break;
                }
            } while (changed);
            return in;
        }

        private LiftableCreatorAssignment getCreatorAssignment(StructuredStatement statement) {
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
            return new LiftableCreatorAssignment(localVariable, assignment.getRvalue());
        }

        private int findMoveTarget(List<Op04StructuredStatement> statements,
                                   int index,
                                   LiftableCreatorAssignment current) {
            int moveTo = -1;
            for (int idx = index + 1; idx < statements.size(); ++idx) {
                StructuredStatement statement = statements.get(idx).getStatement();
                if (statement instanceof StructuredComment) {
                    continue;
                }
                LiftableCreatorAssignment later = getCreatorAssignment(statement);
                if (later == null) {
                    break;
                }
                if (matchesLateResolvedLocal(current.localVariable, later.localVariable)) {
                    break;
                }
                if (usesLocal(later.rvalue, current.localVariable)) {
                    break;
                }
                if (usesLocal(current.rvalue, later.localVariable)) {
                    moveTo = idx;
                }
            }
            return moveTo;
        }

        private boolean usesLocal(Expression expression, LocalVariable localVariable) {
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
    }

    private static class CreatorFirstUseRestorer implements StructuredStatementTransformer {
        @Override
        public StructuredStatement transform(StructuredStatement in, StructuredScope scope) {
            in.transformStructuredChildren(this, scope);
            if (!(in instanceof Block)) {
                return in;
            }
            List<Op04StructuredStatement> statements = ((Block) in).getBlockStatements();
            boolean changed;
            do {
                changed = false;
                for (int idx = 1; idx < statements.size(); ++idx) {
                    StructuredStatement currentStatement = statements.get(idx).getStatement();
                    if (currentStatement instanceof StructuredComment) {
                        continue;
                    }
                    LiftableCreatorAssignment creatorAssignment = getCreatorAssignment(currentStatement);
                    if (creatorAssignment == null) {
                        continue;
                    }
                    int moveTo = findMoveTarget(statements, idx, creatorAssignment);
                    if (moveTo < 0) {
                        continue;
                    }
                    Op04StructuredStatement moved = statements.remove(idx);
                    statements.add(moveTo, moved);
                    changed = true;
                    break;
                }
            } while (changed);
            return in;
        }

        private LiftableCreatorAssignment getCreatorAssignment(StructuredStatement statement) {
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
            return new LiftableCreatorAssignment(localVariable, assignment.getRvalue());
        }

        private int findMoveTarget(List<Op04StructuredStatement> statements,
                                   int index,
                                   LiftableCreatorAssignment creatorAssignment) {
            int moveTo = -1;
            for (int idx = index - 1; idx >= 0; --idx) {
                StructuredStatement statement = statements.get(idx).getStatement();
                if (statement instanceof StructuredComment) {
                    continue;
                }
                if (usesLocal(statement, creatorAssignment.localVariable)) {
                    moveTo = idx;
                }
                if (topLevelWritesLocal(statement, creatorAssignment.localVariable)) {
                    break;
                }
                if (writesCreatorDependency(statement, creatorAssignment.rvalue)) {
                    break;
                }
            }
            return moveTo;
        }

        private boolean topLevelWritesLocal(StructuredStatement statement, LocalVariable localVariable) {
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

        private boolean usesLocal(StructuredStatement statement, LocalVariable localVariable) {
            if (statement == null || localVariable == null) {
                return false;
            }
            LocalAccessCounter counter = new LocalAccessCounter(localVariable);
            List<StructuredStatement> linearized = ListFactory.newList();
            statement.linearizeInto(linearized);
            for (StructuredStatement part : linearized) {
                counter.recordDefinition(part);
                part.rewriteExpressions(counter);
                if (counter.readCount > 0 || counter.writeCount > 0) {
                    return true;
                }
            }
            return false;
        }

        private boolean writesLocal(StructuredStatement statement, LocalVariable localVariable) {
            if (statement == null || localVariable == null) {
                return false;
            }
            LocalAccessCounter counter = new LocalAccessCounter(localVariable);
            List<StructuredStatement> linearized = ListFactory.newList();
            statement.linearizeInto(linearized);
            for (StructuredStatement part : linearized) {
                counter.recordDefinition(part);
                part.rewriteExpressions(counter);
                if (counter.writeCount > 0) {
                    return true;
                }
            }
            return false;
        }

        private boolean writesCreatorDependency(StructuredStatement statement, Expression expression) {
            if (statement == null || expression == null) {
                return false;
            }
            LValueUsageCollectorSimple collector = new LValueUsageCollectorSimple();
            expression.collectUsedLValues(collector);
            for (LValue used : collector.getUsedLValues()) {
                if (used instanceof LocalVariable && writesLocal(statement, (LocalVariable) used)) {
                    return true;
                }
            }
            return false;
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
            return assignment.isCreator(localVariable) && isNullLike(assignment.getRvalue()) ? localVariable : null;
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
            AssignmentUseCounter counter = new AssignmentUseCounter(localVariable);
            List<StructuredStatement> linearized = ListFactory.newList();
            statement.linearizeInto(linearized);
            for (StructuredStatement part : linearized) {
                part.rewriteExpressions(counter);
                if (counter.useCount > 0) {
                    return true;
                }
            }
            return false;
        }
    }

    private static class TrailingCreatorAssignmentRestorer implements StructuredStatementTransformer {
        @Override
        public StructuredStatement transform(StructuredStatement in, StructuredScope scope) {
            in.transformStructuredChildren(this, scope);
            in.rewriteExpressions(new NestedStructuredExpressionTransformer());
            if (!(in instanceof Block)) {
                return in;
            }
            List<Op04StructuredStatement> statements = ((Block) in).getBlockStatements();
            for (int idx = 0; idx < statements.size() - 1; ++idx) {
                Op04StructuredStatement targetContainer = statements.get(idx);
                StructuredStatement targetStatement = targetContainer.getStatement();
                if (targetStatement instanceof StructuredComment) {
                    continue;
                }
                if (!supportsEmbeddedAssignmentLift(targetStatement)) {
                    continue;
                }
                EmbeddedAssignmentCollector collector = new EmbeddedAssignmentCollector();
                targetStatement.rewriteExpressions(collector);
                if (collector.assignments.isEmpty()) {
                    continue;
                }
                for (EmbeddedAssignment assignment : collector.assignments) {
                    int currentIndex = statements.indexOf(targetContainer);
                    if (currentIndex < 0) {
                        break;
                    }
                    TrailingAssignmentMatch match = findAdjacentTrailingAssignment(statements, currentIndex + 1, assignment);
                    if (match == null) {
                        continue;
                    }
                    if (!canInsertCreatorBefore(statements, currentIndex, match.assignment.localVariable)) {
                        continue;
                    }
                    Op04StructuredStatement liftedCreator = match.toLiftedCreator(assignment);
                    statements.remove(match.assignmentIndex);
                    if (match.definitionIndex >= 0) {
                        statements.remove(match.definitionIndex);
                    }
                    statements.add(currentIndex, liftedCreator);
                    targetStatement.rewriteExpressions(new LiftedAssignmentConditionRewriter(
                            assignment.assignmentExpression,
                            match.assignment.localVariable
                    ));
                }
            }
            return in;
        }

        private TrailingAssignmentMatch findAdjacentTrailingAssignment(List<Op04StructuredStatement> statements,
                                                                       int start,
                                                                       EmbeddedAssignment assignment) {
            int assignmentIndex = nextMeaningfulIndex(statements, start);
            if (assignmentIndex < 0) {
                return null;
            }
            TrailingAssignment direct = getTrailingAssignment(statements.get(assignmentIndex).getStatement());
            if (direct != null
                    && direct.creator
                    && matchesTrailingAssignmentLocal(assignment.localVariable, direct.localVariable)
                    && equivalentEmbeddedAssignmentRValue(assignment.assignmentExpression.getrValue(), direct.rvalue)) {
                return new TrailingAssignmentMatch(-1, assignmentIndex, direct);
            }
            LocalVariable definedLocal = getDefinedLocal(statements.get(assignmentIndex).getStatement());
            if (definedLocal == null || !matchesTrailingAssignmentLocal(assignment.localVariable, definedLocal)) {
                return null;
            }
            int valueAssignmentIndex = nextMeaningfulIndex(statements, assignmentIndex + 1);
            if (valueAssignmentIndex < 0) {
                return null;
            }
            TrailingAssignment valueAssignment = getTrailingAssignment(statements.get(valueAssignmentIndex).getStatement());
            if (valueAssignment == null
                    || valueAssignment.creator
                    || !matchesTrailingAssignmentLocal(definedLocal, valueAssignment.localVariable)
                    || !equivalentEmbeddedAssignmentRValue(assignment.assignmentExpression.getrValue(), valueAssignment.rvalue)) {
                return null;
            }
            return new TrailingAssignmentMatch(assignmentIndex, valueAssignmentIndex, valueAssignment);
        }

        private TrailingAssignment getTrailingAssignment(StructuredStatement statement) {
            if (statement instanceof StructuredAssignment) {
                StructuredAssignment assignment = (StructuredAssignment) statement;
                if (!(assignment.getLvalue() instanceof LocalVariable)) {
                    return null;
                }
                LocalVariable localVariable = (LocalVariable) assignment.getLvalue();
                return new TrailingAssignment(localVariable, assignment.getRvalue(), assignment.isCreator(localVariable));
            }
            if (!(statement instanceof StructuredExpressionStatement)) {
                return null;
            }
            Expression expression = ((StructuredExpressionStatement) statement).getExpression();
            if (!(expression instanceof AssignmentExpression)) {
                return null;
            }
            AssignmentExpression assignmentExpression = (AssignmentExpression) expression;
            if (!(assignmentExpression.getUpdatedLValue() instanceof LocalVariable)) {
                return null;
            }
            return new TrailingAssignment(
                    (LocalVariable) assignmentExpression.getUpdatedLValue(),
                    assignmentExpression.getrValue(),
                    false
            );
        }

        private LocalVariable getDefinedLocal(StructuredStatement statement) {
            if (!(statement instanceof StructuredDefinition)) {
                return null;
            }
            LValue defined = ((StructuredDefinition) statement).getLvalue();
            return defined instanceof LocalVariable ? (LocalVariable) defined : null;
        }

        private int nextMeaningfulIndex(List<Op04StructuredStatement> statements, int start) {
            for (int idx = start; idx < statements.size(); ++idx) {
                if (!(statements.get(idx).getStatement() instanceof StructuredComment)) {
                    return idx;
                }
            }
            return -1;
        }

        private boolean canInsertCreatorBefore(List<Op04StructuredStatement> statements, int index, LocalVariable localVariable) {
            for (int idx = index - 1; idx >= 0; --idx) {
                StructuredStatement statement = statements.get(idx).getStatement();
                if (statement instanceof StructuredComment) {
                    continue;
                }
                if (accessesLocal(statement, localVariable)
                        || topLevelWritesLocal(statement, localVariable)) {
                    return false;
                }
            }
            return true;
        }

        private boolean topLevelWritesLocal(StructuredStatement statement, LocalVariable localVariable) {
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

        private boolean accessesLocal(StructuredStatement statement, LocalVariable localVariable) {
            if (statement == null || localVariable == null) {
                return false;
            }
            LocalAccessCounter counter = new LocalAccessCounter(localVariable);
            List<StructuredStatement> linearized = ListFactory.newList();
            statement.linearizeInto(linearized);
            for (StructuredStatement part : linearized) {
                counter.recordDefinition(part);
                part.rewriteExpressions(counter);
                if (counter.readCount > 0 || counter.writeCount > 0) {
                    return true;
                }
            }
            return false;
        }

        private boolean equivalentEmbeddedAssignmentRValue(Expression embedded, Expression creator) {
            if (embedded == null || creator == null) {
                return false;
            }
            embedded = stripCasts(embedded);
            creator = stripCasts(creator);
            if (embedded.equals(creator)) {
                return true;
            }
            return embedded.equivalentUnder(creator, DefaultEquivalenceConstraint.INSTANCE);
        }

        private boolean matchesTrailingAssignmentLocal(LocalVariable embedded, LocalVariable trailing) {
            if (matchesLateResolvedLocal(embedded, trailing)) {
                return true;
            }
            if (embedded == null || trailing == null) {
                return false;
            }
            JavaTypeInstance embeddedType = embedded.getInferredJavaType().getJavaTypeInstance();
            JavaTypeInstance trailingType = trailing.getInferredJavaType().getJavaTypeInstance();
            if (!Objects.equals(embeddedType, trailingType)) {
                return false;
            }
            return trailing.getName() != null
                    && trailing.getName().isGoodName()
                    && (embedded.getIdx() < 0
                    || embedded.getOriginalRawOffset() < 0
                    || embedded.getName() == null
                    || !embedded.getName().isGoodName());
        }

        private Expression stripCasts(Expression expression) {
            while (expression instanceof CastExpression) {
                expression = ((CastExpression) expression).getChild();
            }
            return expression;
        }
    }

    private static class PrefixEmbeddedAssignmentRestorer implements StructuredStatementTransformer {
        @Override
        public StructuredStatement transform(StructuredStatement in, StructuredScope scope) {
            in.transformStructuredChildren(this, scope);
            in.rewriteExpressions(new NestedStructuredExpressionTransformer());
            if (!(in instanceof Block)) {
                return in;
            }
            List<Op04StructuredStatement> statements = ((Block) in).getBlockStatements();
            for (int idx = 0; idx < statements.size(); ++idx) {
                Op04StructuredStatement targetContainer = statements.get(idx);
                StructuredStatement targetStatement = targetContainer.getStatement();
                if (targetStatement instanceof StructuredComment) {
                    continue;
                }
                if (!supportsEmbeddedAssignmentLift(targetStatement)) {
                    continue;
                }
                EmbeddedAssignmentCollector collector = new EmbeddedAssignmentCollector();
                targetStatement.rewriteExpressions(collector);
                if (collector.assignments.isEmpty()) {
                    continue;
                }
                for (int assignmentIdx = collector.assignments.size() - 1; assignmentIdx >= 0; --assignmentIdx) {
                    EmbeddedAssignment assignment = collector.assignments.get(assignmentIdx);
                    int currentIndex = statements.indexOf(targetContainer);
                    if (currentIndex <= 0) {
                        break;
                    }
                    PrefixAssignmentCandidate candidate = findPrefixCandidate(statements, currentIndex - 1, assignment);
                    if (candidate == null) {
                        continue;
                    }
                    statements.remove(candidate.index);
                    currentIndex = statements.indexOf(targetContainer);
                    if (currentIndex < 0) {
                        break;
                    }
                    statements.add(currentIndex, new Op04StructuredStatement(new StructuredAssignment(
                            assignment.assignmentExpression.getCombinedLoc(),
                            candidate.localVariable,
                            assignment.assignmentExpression.getrValue(),
                            true
                    )));
                    targetStatement.rewriteExpressions(new LiftedAssignmentConditionRewriter(
                            assignment.assignmentExpression,
                            candidate.localVariable
                    ));
                }
            }
            return in;
        }

        private PrefixAssignmentCandidate findPrefixCandidate(List<Op04StructuredStatement> statements,
                                                              int start,
                                                              EmbeddedAssignment assignment) {
            for (int idx = start; idx >= 0; --idx) {
                StructuredStatement statement = statements.get(idx).getStatement();
                if (statement instanceof StructuredComment) {
                    continue;
                }
                PrefixAssignmentCandidate candidate = getPrefixCandidate(statement, idx);
                if (candidate != null && matchesPrefixCandidate(assignment.localVariable, candidate.localVariable)) {
                    return candidate;
                }
                if (accessesLocal(statement, assignment.localVariable)
                        || topLevelWritesLocal(statement, assignment.localVariable)) {
                    return null;
                }
            }
            return null;
        }

        private PrefixAssignmentCandidate getPrefixCandidate(StructuredStatement statement, int index) {
            if (statement instanceof StructuredDefinition) {
                LValue defined = ((StructuredDefinition) statement).getLvalue();
                if (defined instanceof LocalVariable) {
                    return new PrefixAssignmentCandidate(index, (LocalVariable) defined);
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
            if (assignment.isCreator(localVariable) && isNullLike(assignment.getRvalue())) {
                return new PrefixAssignmentCandidate(index, localVariable);
            }
            return null;
        }

        private boolean accessesLocal(StructuredStatement statement, LocalVariable localVariable) {
            if (statement == null || localVariable == null) {
                return false;
            }
            LocalAccessCounter counter = new LocalAccessCounter(localVariable);
            List<StructuredStatement> linearized = ListFactory.newList();
            statement.linearizeInto(linearized);
            for (StructuredStatement part : linearized) {
                counter.recordDefinition(part);
                part.rewriteExpressions(counter);
                if (counter.readCount > 0 || counter.writeCount > 0) {
                    return true;
                }
            }
            return false;
        }

        private boolean topLevelWritesLocal(StructuredStatement statement, LocalVariable localVariable) {
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

        private boolean matchesPrefixCandidate(LocalVariable embedded, LocalVariable candidate) {
            if (matchesLateResolvedLocal(embedded, candidate)) {
                return true;
            }
            if (embedded == null || candidate == null) {
                return false;
            }
            JavaTypeInstance embeddedType = embedded.getInferredJavaType().getJavaTypeInstance();
            JavaTypeInstance candidateType = candidate.getInferredJavaType().getJavaTypeInstance();
            if (!Objects.equals(embeddedType, candidateType)
                    && embeddedType != TypeConstants.OBJECT
                    && candidateType != TypeConstants.OBJECT) {
                return false;
            }
            return candidate.getName() != null
                    && candidate.getName().isGoodName()
                    && (embedded.getIdx() < 0
                    || embedded.getOriginalRawOffset() < 0
                    || embedded.getName() == null
                    || !embedded.getName().isGoodName());
        }
    }

    private static boolean supportsEmbeddedAssignmentLift(StructuredStatement statement) {
        return statement instanceof StructuredAssignment
                || statement instanceof StructuredExpressionStatement;
    }

    private static boolean matchesLateResolvedLocal(LocalVariable expected, LocalVariable actual) {
        if (expected == null || actual == null) {
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

    private static boolean isNullLike(Expression expression) {
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

    private static class PrefixAssignmentCandidate {
        private final int index;
        private final LocalVariable localVariable;

        private PrefixAssignmentCandidate(int index, LocalVariable localVariable) {
            this.index = index;
            this.localVariable = localVariable;
        }
    }

    private static class LiftableCreatorAssignment {
        private final LocalVariable localVariable;
        private final Expression rvalue;

        private LiftableCreatorAssignment(LocalVariable localVariable, Expression rvalue) {
            this.localVariable = localVariable;
            this.rvalue = rvalue;
        }
    }

    private static class LiftablePrefixCandidate {
        private final LocalVariable localVariable;
        private final int index;

        private LiftablePrefixCandidate(LocalVariable localVariable, int index) {
            this.localVariable = localVariable;
            this.index = index;
        }
    }

    private static class EmbeddedAssignment {
        private final LocalVariable localVariable;
        private final AssignmentExpression assignmentExpression;

        private EmbeddedAssignment(LocalVariable localVariable, AssignmentExpression assignmentExpression) {
            this.localVariable = localVariable;
            this.assignmentExpression = assignmentExpression;
        }
    }

    private static class TrailingAssignment {
        private final LocalVariable localVariable;
        private final Expression rvalue;
        private final boolean creator;

        private TrailingAssignment(LocalVariable localVariable, Expression rvalue, boolean creator) {
            this.localVariable = localVariable;
            this.rvalue = rvalue;
            this.creator = creator;
        }
    }

    private static class TrailingAssignmentMatch {
        private final int definitionIndex;
        private final int assignmentIndex;
        private final TrailingAssignment assignment;

        private TrailingAssignmentMatch(int definitionIndex, int assignmentIndex, TrailingAssignment assignment) {
            this.definitionIndex = definitionIndex;
            this.assignmentIndex = assignmentIndex;
            this.assignment = assignment;
        }

        private Op04StructuredStatement toLiftedCreator(EmbeddedAssignment embeddedAssignment) {
            return new Op04StructuredStatement(new StructuredAssignment(
                    embeddedAssignment.assignmentExpression.getCombinedLoc(),
                    assignment.localVariable,
                    assignment.rvalue,
                    true
            ));
        }
    }

    private static class ConditionAssignmentCollector extends AbstractExpressionRewriter {
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
                if (matchesLateResolvedLocal(candidate.localVariable, updated)) {
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

    private static class EmbeddedAssignmentCollector extends AbstractExpressionRewriter {
        private final List<EmbeddedAssignment> assignments = ListFactory.newList();

        @Override
        public Expression rewriteExpression(Expression expression,
                                            SSAIdentifiers ssaIdentifiers,
                                            StatementContainer statementContainer,
                                            ExpressionRewriterFlags flags) {
            if (expression instanceof LambdaExpression
                    || expression instanceof StructuredStatementExpression) {
                return expression;
            }
            if (expression instanceof AssignmentExpression) {
                AssignmentExpression assignmentExpression = (AssignmentExpression) expression;
                if (assignmentExpression.getUpdatedLValue() instanceof LocalVariable) {
                    assignments.add(new EmbeddedAssignment(
                            (LocalVariable) assignmentExpression.getUpdatedLValue(),
                            assignmentExpression
                    ));
                }
            }
            return super.rewriteExpression(expression, ssaIdentifiers, statementContainer, flags);
        }
    }

    private static class NestedStructuredExpressionTransformer extends AbstractExpressionRewriter {
        @Override
        public Expression rewriteExpression(Expression expression,
                                            SSAIdentifiers ssaIdentifiers,
                                            StatementContainer statementContainer,
                                            ExpressionRewriterFlags flags) {
            if (expression instanceof StructuredStatementExpression) {
                transformNested(((StructuredStatementExpression) expression).getContent());
                return expression;
            }
            if (expression instanceof LambdaExpression) {
                Expression result = ((LambdaExpression) expression).getResult();
                if (result instanceof StructuredStatementExpression) {
                    transformNested(((StructuredStatementExpression) result).getContent());
                }
                return expression;
            }
            return super.rewriteExpression(expression, ssaIdentifiers, statementContainer, flags);
        }

        private void transformNested(StructuredStatement content) {
            if (content == null) {
                return;
            }
            Op04StructuredStatement container = content.getContainer();
            if (container == null) {
                container = new Op04StructuredStatement(content);
            }
            restoreTrailingCreatorAssignments(container);
        }
    }

    private static class LiftedAssignmentConditionRewriter extends AbstractExpressionRewriter {
        private final AssignmentExpression target;
        private final LocalVariable replacement;

        private LiftedAssignmentConditionRewriter(AssignmentExpression target, LocalVariable replacement) {
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

    private static class LocalAliasExpressionRewriter extends AbstractExpressionRewriter {
        private final LocalVariable alias;
        private final LocalVariable replacement;

        private LocalAliasExpressionRewriter(LocalVariable alias, LocalVariable replacement) {
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

    private static class AssignmentUseCounter extends AbstractExpressionRewriter {
        private final LValue needle;
        private int useCount;

        private AssignmentUseCounter(LValue needle) {
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
    }

    private static class LocalAccessCounter extends AbstractExpressionRewriter {
        private final LocalVariable needle;
        private int readCount;
        private int writeCount;

        private LocalAccessCounter(LocalVariable needle) {
            this.needle = needle;
        }

        private void recordDefinition(StructuredStatement statement) {
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
    }

    private static class InlineAssignmentExpressionRewriter extends AbstractExpressionRewriter {
        private final LValue target;
        private final AssignmentExpression replacement;
        private boolean replaced;

        private InlineAssignmentExpressionRewriter(LValue target, AssignmentExpression replacement) {
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
