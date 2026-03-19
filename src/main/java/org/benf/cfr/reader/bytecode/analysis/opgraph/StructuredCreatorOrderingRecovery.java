package org.benf.cfr.reader.bytecode.analysis.opgraph;

import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.StructuredStatementTransformer;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.LocalVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.LValueUsageCollectorSimple;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredScope;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.Block;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredAssignment;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredComment;
import org.benf.cfr.reader.util.collections.ListFactory;

import java.util.List;

public final class StructuredCreatorOrderingRecovery {
    private StructuredCreatorOrderingRecovery() {
    }

    public static void restoreCreatorDependencyOrder(Op04StructuredStatement root) {
        root.transform(new CreatorDependencyOrderRestorer(), new StructuredScope());
    }

    public static void restoreCreatorsBeforeFirstUse(Op04StructuredStatement root) {
        root.transform(new CreatorFirstUseRestorer(), new StructuredScope());
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
                if (StructuredLocalVariableRecoverySupport.matchesLateResolvedLocal(current.localVariable, later.localVariable)) {
                    break;
                }
                if (StructuredLocalVariableRecoverySupport.expressionUsesLocal(later.rvalue, current.localVariable)) {
                    break;
                }
                if (StructuredLocalVariableRecoverySupport.expressionUsesLocal(current.rvalue, later.localVariable)) {
                    moveTo = idx;
                }
            }
            return moveTo;
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

        private int findMoveTarget(List<Op04StructuredStatement> statements,
                                   int index,
                                   LiftableCreatorAssignment creatorAssignment) {
            int moveTo = -1;
            for (int idx = index - 1; idx >= 0; --idx) {
                StructuredStatement statement = statements.get(idx).getStatement();
                if (statement instanceof StructuredComment) {
                    continue;
                }
                if (StructuredLocalVariableRecoverySupport.statementAccessesLocal(statement, creatorAssignment.localVariable)) {
                    moveTo = idx;
                }
                if (StructuredLocalVariableRecoverySupport.statementTopLevelWritesLocal(statement, creatorAssignment.localVariable)) {
                    break;
                }
                if (writesCreatorDependency(statement, creatorAssignment.rvalue)) {
                    break;
                }
            }
            return moveTo;
        }

        private boolean writesCreatorDependency(StructuredStatement statement, Expression expression) {
            if (statement == null || expression == null) {
                return false;
            }
            LValueUsageCollectorSimple collector = new LValueUsageCollectorSimple();
            expression.collectUsedLValues(collector);
            for (LValue used : collector.getUsedLValues()) {
                if (used instanceof LocalVariable
                        && StructuredLocalVariableRecoverySupport.statementWritesLocal(statement, (LocalVariable) used)) {
                    return true;
                }
            }
            return false;
        }
    }

    private static LiftableCreatorAssignment getCreatorAssignment(StructuredStatement statement) {
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

    private static final class LiftableCreatorAssignment {
        private final LocalVariable localVariable;
        private final Expression rvalue;

        private LiftableCreatorAssignment(LocalVariable localVariable, Expression rvalue) {
            this.localVariable = localVariable;
            this.rvalue = rvalue;
        }
    }
}
