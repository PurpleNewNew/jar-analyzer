package org.benf.cfr.reader.bytecode.analysis.opgraph;

import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.StructuredStatementTransformer;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.AssignmentExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.CastExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.LambdaExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.LocalVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.AbstractExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.DefaultEquivalenceConstraint;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredScope;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.expression.StructuredStatementExpression;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.Block;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredAssignment;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredComment;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredDefinition;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredExpressionStatement;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.TypeConstants;
import org.benf.cfr.reader.util.collections.ListFactory;

import java.util.List;
import java.util.Objects;

public final class StructuredEmbeddedAssignmentRecovery {
    private StructuredEmbeddedAssignmentRecovery() {
    }

    public static void restoreTrailingCreatorAssignments(Op04StructuredStatement root) {
        root.transform(new TrailingCreatorAssignmentRestorer(), new StructuredScope());
    }

    public static void restorePrefixEmbeddedAssignments(Op04StructuredStatement root) {
        root.transform(new PrefixEmbeddedAssignmentRestorer(), new StructuredScope());
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
                if (!StructuredLocalVariableRecoverySupport.supportsEmbeddedAssignmentLift(targetStatement)) {
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
                    targetStatement.rewriteExpressions(new StructuredLocalVariableRecoverySupport.LiftedAssignmentConditionRewriter(
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
            int assignmentIndex = StructuredLocalVariableRecoverySupport.nextMeaningfulIndex(statements, start);
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
            LocalVariable definedLocal = StructuredLocalVariableRecoverySupport.getDefinedLocal(statements.get(assignmentIndex).getStatement());
            if (definedLocal == null || !matchesTrailingAssignmentLocal(assignment.localVariable, definedLocal)) {
                return null;
            }
            int valueAssignmentIndex = StructuredLocalVariableRecoverySupport.nextMeaningfulIndex(statements, assignmentIndex + 1);
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

        private boolean canInsertCreatorBefore(List<Op04StructuredStatement> statements, int index, LocalVariable localVariable) {
            for (int idx = index - 1; idx >= 0; --idx) {
                StructuredStatement statement = statements.get(idx).getStatement();
                if (statement instanceof StructuredComment) {
                    continue;
                }
                if (StructuredLocalVariableRecoverySupport.statementAccessesLocal(statement, localVariable)
                        || StructuredLocalVariableRecoverySupport.statementTopLevelWritesLocal(statement, localVariable)) {
                    return false;
                }
            }
            return true;
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
            if (StructuredLocalVariableRecoverySupport.matchesLateResolvedLocal(embedded, trailing)) {
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
                if (!StructuredLocalVariableRecoverySupport.supportsEmbeddedAssignmentLift(targetStatement)) {
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
                    targetStatement.rewriteExpressions(new StructuredLocalVariableRecoverySupport.LiftedAssignmentConditionRewriter(
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
                if (StructuredLocalVariableRecoverySupport.statementAccessesLocal(statement, assignment.localVariable)
                        || StructuredLocalVariableRecoverySupport.statementTopLevelWritesLocal(statement, assignment.localVariable)) {
                    return null;
                }
            }
            return null;
        }

        private PrefixAssignmentCandidate getPrefixCandidate(StructuredStatement statement, int index) {
            LocalVariable defined = StructuredLocalVariableRecoverySupport.getDefinedLocal(statement);
            if (defined != null) {
                return new PrefixAssignmentCandidate(index, defined);
            }
            if (!(statement instanceof StructuredAssignment)) {
                return null;
            }
            StructuredAssignment assignment = (StructuredAssignment) statement;
            if (!(assignment.getLvalue() instanceof LocalVariable)) {
                return null;
            }
            LocalVariable localVariable = (LocalVariable) assignment.getLvalue();
            if (assignment.isCreator(localVariable)
                    && StructuredLocalVariableRecoverySupport.isNullLike(assignment.getRvalue())) {
                return new PrefixAssignmentCandidate(index, localVariable);
            }
            return null;
        }

        private boolean matchesPrefixCandidate(LocalVariable embedded, LocalVariable candidate) {
            if (StructuredLocalVariableRecoverySupport.matchesLateResolvedLocal(embedded, candidate)) {
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

    private static final class EmbeddedAssignment {
        private final LocalVariable localVariable;
        private final AssignmentExpression assignmentExpression;

        private EmbeddedAssignment(LocalVariable localVariable, AssignmentExpression assignmentExpression) {
            this.localVariable = localVariable;
            this.assignmentExpression = assignmentExpression;
        }
    }

    private static final class TrailingAssignment {
        private final LocalVariable localVariable;
        private final Expression rvalue;
        private final boolean creator;

        private TrailingAssignment(LocalVariable localVariable, Expression rvalue, boolean creator) {
            this.localVariable = localVariable;
            this.rvalue = rvalue;
            this.creator = creator;
        }
    }

    private static final class TrailingAssignmentMatch {
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

    private static final class PrefixAssignmentCandidate {
        private final int index;
        private final LocalVariable localVariable;

        private PrefixAssignmentCandidate(int index, LocalVariable localVariable) {
            this.index = index;
            this.localVariable = localVariable;
        }
    }

    private static final class EmbeddedAssignmentCollector extends AbstractExpressionRewriter {
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

    private static final class NestedStructuredExpressionTransformer extends AbstractExpressionRewriter {
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
}
