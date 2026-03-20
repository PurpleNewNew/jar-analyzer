package org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters;

import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.StructuredStatementTransformer;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.PatternCaseLabel;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredScope;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.Block;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredAssignment;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredBreak;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredCase;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredComment;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredSwitch;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredWhile;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.Literal;
import org.benf.cfr.reader.util.collections.ListFactory;

import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class SwitchPatternRewriter implements StructuredStatementTransformer {
    private final Map<Op04StructuredStatement, LValue> patternLoopIndexes = new IdentityHashMap<Op04StructuredStatement, LValue>();
    private final SwitchPatternCasePlanner casePlanner = new SwitchPatternCasePlanner();

    public void rewrite(Op04StructuredStatement root) {
        root.transform(this, new StructuredScope());
    }

    @Override
    public StructuredStatement transform(StructuredStatement in, StructuredScope scope) {
        in.transformStructuredChildren(this, scope);
        if (in instanceof StructuredSwitch) {
            rewriteSwitch((StructuredSwitch) in, scope);
        } else if (in instanceof StructuredWhile) {
            StructuredStatement collapsed = collapsePatternLoop((StructuredWhile) in, scope);
            if (collapsed != null) {
                return collapsed;
            }
        }
        return in;
    }

    private void rewriteSwitch(StructuredSwitch structuredSwitch, StructuredScope scope) {
        SwitchPatternBootstrapMatch bootstrapMatch = SwitchPatternBootstrapMatch.match(structuredSwitch.getSwitchOn());
        if (bootstrapMatch == null) return;
        if (!(structuredSwitch.getBody().getStatement() instanceof Block)) return;
        Block switchBlock = (Block) structuredSwitch.getBody().getStatement();
        StructuredWhile patternLoop = findPatternLoop(scope);

        List<SwitchPatternCasePlanner.CaseRewritePlan> plans = casePlanner.planCases(switchBlock, bootstrapMatch, patternLoop);
        if (plans == null || plans.isEmpty()) return;

        structuredSwitch.setSwitchOn(bootstrapMatch.getSelector());
        for (SwitchPatternCasePlanner.CaseRewritePlan plan : plans) {
            if (plan.getRewrittenValues() != null) {
                plan.getStructuredCase().setValues(plan.getRewrittenValues());
            }
            if (plan.getBodyBlock() != null && plan.getRewrittenStatements() != null) {
                plan.getBodyBlock().replaceBlockStatements(plan.getRewrittenStatements());
            }
        }
        if (patternLoop != null) {
            patternLoopIndexes.put(patternLoop.getContainer(), bootstrapMatch.getIndexLValue());
        }
    }

    private StructuredStatement collapsePatternLoop(StructuredWhile loop, StructuredScope scope) {
        if (!(loop.getBody().getStatement() instanceof Block)) return null;
        Block loopBody = (Block) loop.getBody().getStatement();
        List<Op04StructuredStatement> filtered = loopBody.getFilteredBlockStatements();
        if (filtered.size() != 2) return null;
        Op04StructuredStatement switchContainer = filtered.get(0);
        if (!(switchContainer.getStatement() instanceof StructuredSwitch)) return null;
        if (!containsPatternCases((StructuredSwitch) switchContainer.getStatement())) return null;
        StructuredStatement trailing = filtered.get(1).getStatement();
        if (!(trailing instanceof StructuredBreak)) return null;
        StructuredBreak structuredBreak = (StructuredBreak) trailing;
        if (!structuredBreak.getBreakBlock().equals(loop.getBlock())) return null;

        LinkedList<Op04StructuredStatement> replacement = ListFactory.newLinkedList();
        replacement.add(switchContainer);
        Block collapsed = new Block(replacement, false);

        LValue indexLValue = patternLoopIndexes.remove(loop.getContainer());
        if (indexLValue != null) {
            maybeRemoveSyntheticIndexInitialiser(loop, scope, indexLValue);
        }
        return collapsed;
    }

    private boolean containsPatternCases(StructuredSwitch structuredSwitch) {
        if (!(structuredSwitch.getBody().getStatement() instanceof Block)) return false;
        for (Op04StructuredStatement statement : ((Block) structuredSwitch.getBody().getStatement()).getBlockStatements()) {
            if (!(statement.getStatement() instanceof StructuredCase)) continue;
            for (Expression value : ((StructuredCase) statement.getStatement()).getValues()) {
                if (value instanceof PatternCaseLabel) {
                    return true;
                }
            }
        }
        return false;
    }

    private void maybeRemoveSyntheticIndexInitialiser(StructuredWhile loop, StructuredScope scope, LValue indexLValue) {
        StructuredStatement enclosing = scope.get(1);
        if (!(enclosing instanceof Block)) return;
        List<Op04StructuredStatement> statements = ((Block) enclosing).getBlockStatements();
        int loopIdx = statements.indexOf(loop.getContainer());
        if (loopIdx <= 0) return;
        for (int x = loopIdx - 1; x >= 0; --x) {
            StructuredStatement statement = statements.get(x).getStatement();
            if (statement instanceof StructuredComment || statement.isEffectivelyNOP()) {
                continue;
            }
            if (statement instanceof StructuredAssignment) {
                StructuredAssignment assignment = (StructuredAssignment) statement;
                if (indexLValue.equals(assignment.getLvalue())
                        && Literal.INT_ZERO.equals(assignment.getRvalue())) {
                    statements.get(x).nopOut();
                }
            }
            return;
        }
    }

    private static StructuredWhile findPatternLoop(StructuredScope scope) {
        for (StructuredStatement item : scope.getAll()) {
            if (item instanceof StructuredWhile) {
                return (StructuredWhile) item;
            }
        }
        return null;
    }
}
