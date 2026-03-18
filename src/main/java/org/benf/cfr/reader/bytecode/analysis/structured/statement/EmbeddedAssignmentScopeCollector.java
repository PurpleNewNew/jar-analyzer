package org.benf.cfr.reader.bytecode.analysis.structured.statement;

import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.AssignmentExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.AbstractExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.scope.LValueScopeDiscoverer;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;

final class EmbeddedAssignmentScopeCollector extends AbstractExpressionRewriter {
    private final StatementContainer<StructuredStatement> container;
    private final LValueScopeDiscoverer scopeDiscoverer;

    EmbeddedAssignmentScopeCollector(StatementContainer<StructuredStatement> container,
                                     LValueScopeDiscoverer scopeDiscoverer) {
        this.container = container;
        this.scopeDiscoverer = scopeDiscoverer;
    }

    void collect(Expression expression) {
        if (expression == null || container == null || scopeDiscoverer == null) {
            return;
        }
        rewriteExpression(expression, null, container, ExpressionRewriterFlags.RVALUE);
    }

    @Override
    public Expression rewriteExpression(Expression expression,
                                        SSAIdentifiers ssaIdentifiers,
                                        StatementContainer statementContainer,
                                        ExpressionRewriterFlags flags) {
        if (!(expression instanceof AssignmentExpression)) {
            return super.rewriteExpression(expression, ssaIdentifiers, statementContainer, flags);
        }
        AssignmentExpression assignmentExpression = (AssignmentExpression) expression;
        rewriteExpression(assignmentExpression.getrValue(), ssaIdentifiers, statementContainer, ExpressionRewriterFlags.RVALUE);
        assignmentExpression.getUpdatedLValue().collectLValueAssignments(
                assignmentExpression.getrValue(),
                container,
                scopeDiscoverer
        );
        return expression;
    }
}
