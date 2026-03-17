package org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers;

import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.ConditionalExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifier;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockType;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.CloneHelper;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredScope;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.Block;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredAssignment;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredBreak;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredComment;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredDefinition;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredExpressionStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredIf;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.UnstructuredGoto;
import org.benf.cfr.reader.util.Optional;
import org.benf.cfr.reader.util.collections.ListFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class ForwardIfGotoRewriter implements StructuredStatementTransformer {
    private static int syntheticBlockIndex = -1;

    public void transform(Op04StructuredStatement root) {
        root.transform(this, new StructuredScope());
    }

    @Override
    public StructuredStatement transform(StructuredStatement in, StructuredScope scope) {
        in.transformStructuredChildren(this, scope);
        if (!(in instanceof Block)) {
            return in;
        }
        Block block = (Block) in;
        List<Op04StructuredStatement> statements = block.getBlockStatements();
        for (int i = 0; i < statements.size(); ++i) {
            if (rewriteDispatchBlockIntoNestedIf(statements, i)) {
                break;
            }
            if (rewriteGuardedBreakElseBlock(block, statements, i)) {
                break;
            }
            if (rewriteTerminalBlockExit(block, statements, scope)) {
                break;
            }
            if (removePointlessElseGoto(statements, i)) {
                break;
            }
            RewriteCandidate candidate = findRewriteCandidate(statements, i);
            if (candidate == null) {
                continue;
            }
            applyRewrite(statements, i, candidate);
            break;
        }
        return in;
    }

    private boolean rewriteDispatchBlockIntoNestedIf(List<Op04StructuredStatement> statements, int idx) {
        if (idx >= statements.size() - 1) {
            return false;
        }
        StructuredStatement dispatchStatement = statements.get(idx).getStatement();
        StructuredStatement trailingStatement = statements.get(idx + 1).getStatement();
        if (!(dispatchStatement instanceof Block) || !(trailingStatement instanceof StructuredIf)) {
            return false;
        }
        Block dispatchBlock = (Block) dispatchStatement;
        StructuredIf trailingIf = (StructuredIf) trailingStatement;
        if (!trailingIf.hasElseBlock()) {
            return false;
        }
        DispatchPattern pattern = findDispatchPattern(dispatchBlock, trailingIf);
        if (pattern == null) {
            return false;
        }

        Op04StructuredStatement takenClone = cloneContainer(trailingIf.getIfTaken());
        Op04StructuredStatement elseClone = cloneContainer(trailingIf.getElseBlock());
        Op04StructuredStatement laterIfClone = cloneStructuredIf(
                trailingIf.getConditionalExpression(),
                trailingIf.getIfTaken(),
                trailingIf.getElseBlock()
        );
        if (takenClone == null || elseClone == null || laterIfClone == null) {
            return false;
        }

        LinkedList<Op04StructuredStatement> selectorItems = ListFactory.newLinkedList();
        selectorItems.addAll(pattern.tailStatements);
        selectorItems.add(new Op04StructuredStatement(new StructuredIf(
                BytecodeLoc.TODO,
                pattern.selectorSuccessCondition,
                takenClone,
                elseClone
        )));

        LinkedList<Op04StructuredStatement> takenItems = ListFactory.newLinkedList();
        takenItems.add(laterIfClone);
        Op04StructuredStatement outerIf = new Op04StructuredStatement(new StructuredIf(
                BytecodeLoc.TODO,
                pattern.gateCondition,
                new Op04StructuredStatement(new Block(takenItems, true)),
                new Op04StructuredStatement(new Block(selectorItems, true))
        ));

        List<Op04StructuredStatement> replacements = ListFactory.newList();
        replacements.addAll(pattern.prefixStatements);
        replacements.add(outerIf);

        statements.remove(idx + 1);
        statements.remove(idx);
        statements.addAll(idx, replacements);
        return true;
    }

    private boolean rewriteGuardedBreakElseBlock(Block block, List<Op04StructuredStatement> statements, int idx) {
        if (idx >= statements.size() - 1) {
            return false;
        }
        BlockIdentifier breakTarget = block.getBreakableBlockOrNull();
        if (breakTarget == null) {
            return false;
        }

        StructuredStatement maybeGate = statements.get(idx).getStatement();
        if (!(maybeGate instanceof StructuredIf)) {
            return false;
        }
        StructuredIf gateIf = (StructuredIf) maybeGate;
        if (gateIf.hasElseBlock()) {
            return false;
        }
        GuardedBreakPattern pattern = findGuardedBreakPattern(statements, idx, gateIf, breakTarget);
        if (pattern == null) {
            return false;
        }

        Op04StructuredStatement elseBodyClone = cloneContainer(statements.get(pattern.elseBodyIdx));
        if (elseBodyClone == null) {
            return false;
        }

        Op04StructuredStatement rewrittenInnerIf = new Op04StructuredStatement(new StructuredIf(
                BytecodeLoc.TODO,
                cloneCondition(pattern.innerIf.getConditionalExpression()),
                new Op04StructuredStatement(new Block(pattern.innerThenClones, true)),
                elseBodyClone
        ));

        LinkedList<Op04StructuredStatement> gateTakenClones = pattern.prepClones;
        gateTakenClones.add(rewrittenInnerIf);
        Op04StructuredStatement rewrittenOuterIf = new Op04StructuredStatement(new StructuredIf(
                BytecodeLoc.TODO,
                cloneCondition(gateIf.getConditionalExpression()),
                new Op04StructuredStatement(new Block(gateTakenClones, true)),
                cloneContainer(statements.get(pattern.elseBodyIdx))
        ));
        if (((StructuredIf) rewrittenOuterIf.getStatement()).getElseBlock() == null) {
            return false;
        }

        for (int removeIdx = pattern.elseBodyIdx; removeIdx > idx; --removeIdx) {
            statements.remove(removeIdx);
        }
        statements.set(idx, rewrittenOuterIf);
        if (breakTarget.hasForeignReferences()) {
            breakTarget.releaseForeignRef();
        }
        return true;
    }

    private GuardedBreakPattern findGuardedBreakPattern(List<Op04StructuredStatement> statements,
                                                        int idx,
                                                        StructuredIf gateIf,
                                                        BlockIdentifier breakTarget) {
        GuardedBreakPattern nested = findNestedGuardedBreakPattern(statements, idx, gateIf, breakTarget);
        if (nested != null) {
            return nested;
        }
        return findFlatGuardedBreakPattern(statements, idx, gateIf, breakTarget);
    }

    private GuardedBreakPattern findNestedGuardedBreakPattern(List<Op04StructuredStatement> statements,
                                                              int idx,
                                                              StructuredIf gateIf,
                                                              BlockIdentifier breakTarget) {
        Op04StructuredStatement gateTaken = unwrapSingleStatementBlock(gateIf.getIfTaken());
        if (gateTaken == null || !(gateTaken.getStatement() instanceof Block)) {
            return null;
        }

        StructuredStatement maybeElseBody = statements.get(idx + 1).getStatement();
        if (!(maybeElseBody instanceof Block)) {
            return null;
        }

        List<Op04StructuredStatement> gateBodyStatements = ((Block) gateTaken.getStatement()).getBlockStatements();
        int innerIfIdx = lastSignificantIndex(gateBodyStatements);
        if (innerIfIdx < 0) {
            return null;
        }
        StructuredStatement maybeInner = gateBodyStatements.get(innerIfIdx).getStatement();
        if (!(maybeInner instanceof StructuredIf)) {
            return null;
        }
        StructuredIf innerIf = (StructuredIf) maybeInner;
        if (innerIf.hasElseBlock()) {
            return null;
        }

        LinkedList<Op04StructuredStatement> innerThenClones = cloneBranchWithoutTrailingBreak(innerIf.getIfTaken(), breakTarget);
        LinkedList<Op04StructuredStatement> prepClones = cloneStatementRange(gateBodyStatements, 0, innerIfIdx);
        if (innerThenClones == null || prepClones == null) {
            return null;
        }
        return new GuardedBreakPattern(innerIf, prepClones, innerThenClones, idx + 1);
    }

    private GuardedBreakPattern findFlatGuardedBreakPattern(List<Op04StructuredStatement> statements,
                                                            int idx,
                                                            StructuredIf gateIf,
                                                            BlockIdentifier breakTarget) {
        Op04StructuredStatement gatePrep = unwrapSingleStatementBlock(gateIf.getIfTaken());
        if (gatePrep == null || gatePrep.getStatement() instanceof Block) {
            return null;
        }

        int innerIfIdx = nextSignificantIndex(statements, idx + 1);
        if (innerIfIdx < 0) {
            return null;
        }
        StructuredStatement maybeInner = statements.get(innerIfIdx).getStatement();
        if (!(maybeInner instanceof StructuredIf)) {
            return null;
        }
        StructuredIf innerIf = (StructuredIf) maybeInner;
        if (innerIf.hasElseBlock()) {
            return null;
        }

        int breakIdx = nextSignificantIndex(statements, innerIfIdx + 1);
        if (breakIdx < 0) {
            return null;
        }
        StructuredStatement maybeBreak = statements.get(breakIdx).getStatement();
        if (!(maybeBreak instanceof StructuredBreak)) {
            return null;
        }
        StructuredBreak terminalBreak = (StructuredBreak) maybeBreak;
        if (!sameBlockIdentifier(terminalBreak.getBreakBlock(), breakTarget)) {
            return null;
        }

        int elseBodyIdx = nextSignificantIndex(statements, breakIdx + 1);
        if (elseBodyIdx < 0) {
            return null;
        }

        LinkedList<Op04StructuredStatement> prepClones = ListFactory.newLinkedList();
        Op04StructuredStatement prepClone = cloneContainer(gatePrep);
        if (prepClone == null) {
            return null;
        }
        prepClones.add(prepClone);

        LinkedList<Op04StructuredStatement> innerThenClones = cloneBranchWithoutTrailingBreak(innerIf.getIfTaken(), breakTarget);
        if (innerThenClones == null) {
            return null;
        }
        return new GuardedBreakPattern(innerIf, prepClones, innerThenClones, elseBodyIdx);
    }

    private boolean rewriteTerminalBlockExit(Block block, List<Op04StructuredStatement> statements, StructuredScope scope) {
        if (statements.isEmpty()) {
            return false;
        }
        BlockIdentifier breakBlock = block.getBreakableBlockOrNull();
        if (breakBlock == null) {
            return false;
        }
        Set<Op04StructuredStatement> nextFallThrough = scope.getNextFallThrough(block);
        if (nextFallThrough.isEmpty()) {
            return false;
        }

        int lastIdx = statements.size() - 1;
        Op04StructuredStatement lastContainer = statements.get(lastIdx);
        Op04StructuredStatement lastGoto = unwrapSingleStatementBlock(lastContainer);
        if (lastGoto == null || !(lastGoto.getStatement() instanceof UnstructuredGoto) || lastGoto.getTargets().size() != 1) {
            return false;
        }
        Op04StructuredStatement target = lastGoto.getTargets().get(0);
        if (!nextFallThrough.contains(target)) {
            return false;
        }

        if (lastIdx > 0) {
            Op04StructuredStatement previousContainer = statements.get(lastIdx - 1);
            StructuredStatement previous = previousContainer.getStatement();
            if (previous instanceof StructuredIf) {
                StructuredIf previousIf = (StructuredIf) previous;
                Op04StructuredStatement previousTaken = unwrapSingleStatementBlock(previousIf.getIfTaken());
                if (!previousIf.hasElseBlock()
                        && previousTaken != null
                        && previousTaken.getStatement() instanceof UnstructuredGoto
                        && previousTaken.getTargets().size() == 1
                        && previousTaken.getTargets().get(0) == target) {
                    target.getSources().remove(previousTaken);
                    statements.remove(lastIdx - 1);
                    lastIdx--;
                }
            }
        }

        target.getSources().remove(lastGoto);
        statements.set(lastIdx, new Op04StructuredStatement(new StructuredBreak(BytecodeLoc.TODO, breakBlock, false)));
        return true;
    }

    private boolean removePointlessElseGoto(List<Op04StructuredStatement> statements, int idx) {
        if (idx >= statements.size() - 1) {
            return false;
        }
        StructuredStatement statement = statements.get(idx).getStatement();
        if (!(statement instanceof StructuredIf)) {
            return false;
        }
        StructuredIf structuredIf = (StructuredIf) statement;
        Op04StructuredStatement elseBlock = unwrapSingleStatementBlock(structuredIf.getElseBlock());
        if (elseBlock == null) {
            return false;
        }
        if (!(elseBlock.getStatement() instanceof UnstructuredGoto) || elseBlock.getTargets().size() != 1) {
            return false;
        }
        if (elseBlock.getTargets().get(0) != statements.get(idx + 1)) {
            return false;
        }
        Op04StructuredStatement target = elseBlock.getTargets().get(0);
        target.getSources().remove(elseBlock);
        Op04StructuredStatement originalElseBlock = structuredIf.getElseBlock();
        if (originalElseBlock != null && originalElseBlock != elseBlock) {
            target.getSources().remove(originalElseBlock);
        }
        structuredIf.clearElseBlock();
        return true;
    }

    private RewriteCandidate findRewriteCandidate(List<Op04StructuredStatement> statements, int idx) {
        Op04StructuredStatement gateContainer = statements.get(idx);
        StructuredStatement maybeGate = gateContainer.getStatement();
        if (!(maybeGate instanceof StructuredIf)) {
            return null;
        }
        StructuredIf gate = (StructuredIf) maybeGate;
        if (gate.hasElseBlock()) {
            return null;
        }
        Op04StructuredStatement gateTaken = unwrapSingleStatementBlock(gate.getIfTaken());
        if (!(gateTaken.getStatement() instanceof UnstructuredGoto) || gateTaken.getTargets().size() != 1) {
            return null;
        }
        Op04StructuredStatement target = gateTaken.getTargets().get(0);
        for (int j = idx + 1; j < statements.size(); ++j) {
            Op04StructuredStatement laterContainer = statements.get(j);
            StructuredStatement maybeLater = laterContainer.getStatement();
            if (!(maybeLater instanceof StructuredIf)) {
                if (!isPreparatoryStatement(maybeLater)) {
                    break;
                }
                continue;
            }
            StructuredIf later = (StructuredIf) maybeLater;
            Op04StructuredStatement elseBlock = unwrapSingleStatementBlock(later.getElseBlock());
            if (elseBlock == null) {
                continue;
            }
            if (!hasOnlyLocalSources(statements, idx, j)) {
                continue;
            }
            if (target == later.getIfTaken()) {
                return new RewriteCandidate(j, later, true);
            }
            if (target == elseBlock) {
                return new RewriteCandidate(j, later, false);
            }
        }
        return null;
    }

    private boolean hasOnlyLocalSources(List<Op04StructuredStatement> statements, int startIdx, int endIdx) {
        List<Op04StructuredStatement> range = statements.subList(startIdx, endIdx + 1);
        for (int idx = startIdx + 1; idx <= endIdx; ++idx) {
            Op04StructuredStatement statement = statements.get(idx);
            for (Op04StructuredStatement source : statement.getSources()) {
                if (!range.contains(source)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isPreparatoryStatement(StructuredStatement statement) {
        return statement instanceof StructuredAssignment
                || statement instanceof StructuredDefinition
                || statement instanceof StructuredComment;
    }

    private DispatchPattern findDispatchPattern(Block dispatchBlock, StructuredIf trailingIf) {
        BlockIdentifier dispatchTarget = dispatchBlock.getBreakableBlockOrNull();
        if (dispatchTarget == null) {
            return null;
        }
        List<Op04StructuredStatement> blockStatements = dispatchBlock.getBlockStatements();
        int finalGotoIdx = lastSignificantIndex(blockStatements);
        if (finalGotoIdx < 1) {
            return null;
        }
        Op04StructuredStatement finalGoto = unwrapSingleStatementBlock(blockStatements.get(finalGotoIdx));
        if (finalGoto == null || !(finalGoto.getStatement() instanceof UnstructuredGoto) || finalGoto.getTargets().size() != 1) {
            return null;
        }

        int selectorIdx = previousSignificantIndex(blockStatements, finalGotoIdx - 1);
        if (selectorIdx < 1) {
            return null;
        }
        StructuredStatement maybeSelector = blockStatements.get(selectorIdx).getStatement();
        if (!(maybeSelector instanceof StructuredIf)) {
            return null;
        }
        StructuredIf selectorIf = (StructuredIf) maybeSelector;
        if (selectorIf.hasElseBlock()) {
            return null;
        }
        Op04StructuredStatement selectorTaken = unwrapSingleStatementBlock(selectorIf.getIfTaken());
        if (selectorTaken == null || !(selectorTaken.getStatement() instanceof UnstructuredGoto) || selectorTaken.getTargets().size() != 1) {
            return null;
        }

        int gateIdx = selectorIdx - 1;
        while (gateIdx >= 0 && isPreparatoryStatement(blockStatements.get(gateIdx).getStatement())) {
            --gateIdx;
        }
        if (gateIdx < 0) {
            return null;
        }
        StructuredStatement maybeGate = blockStatements.get(gateIdx).getStatement();
        if (!(maybeGate instanceof StructuredIf)) {
            return null;
        }
        StructuredIf gateIf = (StructuredIf) maybeGate;
        if (gateIf.hasElseBlock()) {
            return null;
        }
        Op04StructuredStatement gateTaken = unwrapSingleStatementBlock(gateIf.getIfTaken());
        if (gateTaken == null || !(gateTaken.getStatement() instanceof StructuredBreak)) {
            return null;
        }
        StructuredBreak gateBreak = (StructuredBreak) gateTaken.getStatement();
        if (gateBreak.getBreakBlock() != dispatchTarget) {
            return null;
        }

        ConditionalExpression selectorSuccessCondition = getSelectorSuccessCondition(
                selectorIf,
                selectorTaken.getTargets().get(0),
                finalGoto.getTargets().get(0),
                trailingIf
        );
        if (selectorSuccessCondition == null) {
            return null;
        }

        List<Op04StructuredStatement> prefixStatements = ListFactory.newList();
        for (int x = 0; x < gateIdx; ++x) {
            prefixStatements.add(blockStatements.get(x));
        }
        List<Op04StructuredStatement> tailStatements = ListFactory.newList();
        for (int x = gateIdx + 1; x < selectorIdx; ++x) {
            tailStatements.add(blockStatements.get(x));
        }
        return new DispatchPattern(
                prefixStatements,
                tailStatements,
                gateIf.getConditionalExpression().simplify(),
                selectorSuccessCondition
        );
    }

    private ConditionalExpression getSelectorSuccessCondition(StructuredIf selectorIf,
                                                              Op04StructuredStatement selectorTakenTarget,
                                                              Op04StructuredStatement fallthroughTarget,
                                                              StructuredIf trailingIf) {
        Op04StructuredStatement successTarget = trailingIf.getIfTaken();
        Op04StructuredStatement failureTarget = trailingIf.getElseBlock();
        if (selectorTakenTarget == successTarget && fallthroughTarget == failureTarget) {
            return selectorIf.getConditionalExpression().simplify();
        }
        if (selectorTakenTarget == failureTarget && fallthroughTarget == successTarget) {
            return selectorIf.getConditionalExpression().getNegated().simplify();
        }
        return null;
    }

    private int lastSignificantIndex(List<Op04StructuredStatement> statements) {
        return previousSignificantIndex(statements, statements.size() - 1);
    }

    private int previousSignificantIndex(List<Op04StructuredStatement> statements, int startIdx) {
        for (int x = startIdx; x >= 0; --x) {
            if (!(statements.get(x).getStatement() instanceof StructuredComment)) {
                return x;
            }
        }
        return -1;
    }

    private int nextSignificantIndex(List<Op04StructuredStatement> statements, int startIdx) {
        for (int x = startIdx; x < statements.size(); ++x) {
            if (!(statements.get(x).getStatement() instanceof StructuredComment)) {
                return x;
            }
        }
        return -1;
    }

    private Op04StructuredStatement cloneStructuredIf(ConditionalExpression conditionalExpression,
                                                      Op04StructuredStatement ifTaken,
                                                      Op04StructuredStatement elseBlock) {
        CloneHelper cloneHelper = new CloneHelper();
        Op04StructuredStatement clonedIfTaken = cloneContainer(ifTaken, cloneHelper);
        Op04StructuredStatement clonedElse = cloneContainer(elseBlock, cloneHelper);
        if (clonedIfTaken == null || clonedElse == null) {
            return null;
        }
        return new Op04StructuredStatement(new StructuredIf(
                BytecodeLoc.TODO,
                (ConditionalExpression) cloneHelper.replaceOrClone(conditionalExpression),
                clonedIfTaken,
                clonedElse
        ));
    }

    private ConditionalExpression cloneCondition(ConditionalExpression conditionalExpression) {
        return (ConditionalExpression) new CloneHelper().replaceOrClone(conditionalExpression);
    }

    private LinkedList<Op04StructuredStatement> cloneStatementRange(List<Op04StructuredStatement> statements, int startInclusive, int endExclusive) {
        LinkedList<Op04StructuredStatement> clones = ListFactory.newLinkedList();
        CloneHelper cloneHelper = new CloneHelper();
        for (int x = startInclusive; x < endExclusive; ++x) {
            Op04StructuredStatement cloned = cloneContainer(statements.get(x), cloneHelper);
            if (cloned == null) {
                return null;
            }
            clones.add(cloned);
        }
        return clones;
    }

    private LinkedList<Op04StructuredStatement> cloneBranchWithoutTrailingBreak(Op04StructuredStatement branch,
                                                                                BlockIdentifier breakTarget) {
        Op04StructuredStatement unwrapped = unwrapSingleStatementBlock(branch);
        if (unwrapped == null) {
            return null;
        }
        StructuredStatement statement = unwrapped.getStatement();
        if (!(statement instanceof Block)) {
            LinkedList<Op04StructuredStatement> single = ListFactory.newLinkedList();
            Op04StructuredStatement cloned = cloneContainer(unwrapped);
            if (cloned == null) {
                return null;
            }
            single.add(cloned);
            return single;
        }

        List<Op04StructuredStatement> branchStatements = ((Block) statement).getBlockStatements();
        int lastIdx = lastSignificantIndex(branchStatements);
        if (lastIdx >= 0) {
            StructuredStatement maybeBreak = branchStatements.get(lastIdx).getStatement();
            if (maybeBreak instanceof StructuredBreak) {
                StructuredBreak structuredBreak = (StructuredBreak) maybeBreak;
                if (sameBlockIdentifier(structuredBreak.getBreakBlock(), breakTarget)) {
                    return flattenSingleNestedBlock(cloneStatementRange(branchStatements, 0, lastIdx));
                }
            }
        }
        return flattenSingleNestedBlock(cloneStatementRange(branchStatements, 0, branchStatements.size()));
    }

    private LinkedList<Op04StructuredStatement> flattenSingleNestedBlock(LinkedList<Op04StructuredStatement> statements) {
        if (statements == null || statements.size() != 1) {
            return statements;
        }
        StructuredStatement statement = statements.getFirst().getStatement();
        if (!(statement instanceof Block)) {
            return statements;
        }
        Block block = (Block) statement;
        if (block.getBreakableBlockOrNull() != null) {
            return statements;
        }
        LinkedList<Op04StructuredStatement> flattened = ListFactory.newLinkedList();
        flattened.addAll(block.getBlockStatements());
        return flattened;
    }

    private Op04StructuredStatement cloneContainer(Op04StructuredStatement original) {
        return cloneContainer(original, new CloneHelper());
    }

    private Op04StructuredStatement cloneContainer(Op04StructuredStatement original, CloneHelper cloneHelper) {
        if (original == null) {
            return null;
        }
        StructuredStatement cloned = cloneStructuredStatement(original.getStatement(), cloneHelper);
        return cloned == null ? null : new Op04StructuredStatement(cloned);
    }

    private StructuredStatement cloneStructuredStatement(StructuredStatement statement, CloneHelper cloneHelper) {
        if (statement instanceof Block) {
            Block block = (Block) statement;
            LinkedList<Op04StructuredStatement> clonedStatements = ListFactory.newLinkedList();
            for (Op04StructuredStatement child : block.getBlockStatements()) {
                Op04StructuredStatement clonedChild = cloneContainer(child, cloneHelper);
                if (clonedChild == null) {
                    return null;
                }
                clonedStatements.add(clonedChild);
            }
            return new Block(clonedStatements, block.isIndenting());
        }
        if (statement instanceof StructuredIf) {
            StructuredIf structuredIf = (StructuredIf) statement;
            Op04StructuredStatement clonedIfTaken = cloneContainer(structuredIf.getIfTaken(), cloneHelper);
            Op04StructuredStatement clonedElse = cloneContainer(structuredIf.getElseBlock(), cloneHelper);
            if (clonedIfTaken == null || (structuredIf.hasElseBlock() && clonedElse == null)) {
                return null;
            }
            return new StructuredIf(
                    BytecodeLoc.TODO,
                    (ConditionalExpression) cloneHelper.replaceOrClone(structuredIf.getConditionalExpression()),
                    clonedIfTaken,
                    clonedElse
            );
        }
        if (statement instanceof StructuredExpressionStatement) {
            StructuredExpressionStatement expressionStatement = (StructuredExpressionStatement) statement;
            return new StructuredExpressionStatement(
                    BytecodeLoc.TODO,
                    cloneHelper.replaceOrClone(expressionStatement.getExpression()),
                    false
            );
        }
        if (statement instanceof StructuredAssignment) {
            StructuredAssignment assignment = (StructuredAssignment) statement;
            return new StructuredAssignment(
                    BytecodeLoc.TODO,
                    cloneHelper.replaceOrClone(assignment.getLvalue()),
                    cloneHelper.replaceOrClone(assignment.getRvalue()),
                    assignment.isCreator(assignment.getLvalue())
            );
        }
        if (statement instanceof StructuredDefinition) {
            StructuredDefinition definition = (StructuredDefinition) statement;
            return new StructuredDefinition(cloneHelper.replaceOrClone(definition.getLvalue()));
        }
        if (statement instanceof StructuredComment) {
            return StructuredComment.EMPTY_COMMENT;
        }
        return null;
    }

    private Op04StructuredStatement unwrapSingleStatementBlock(Op04StructuredStatement statement) {
        if (statement == null) {
            return null;
        }
        StructuredStatement inner = statement.getStatement();
        if (!(inner instanceof Block)) {
            return statement;
        }
        Optional<Op04StructuredStatement> maybeStatement = ((Block) inner).getMaybeJustOneStatement();
        return maybeStatement.isSet() ? maybeStatement.getValue() : statement;
    }

    private boolean sameBlockIdentifier(BlockIdentifier lhs, BlockIdentifier rhs) {
        if (lhs == rhs) {
            return true;
        }
        if (lhs == null || rhs == null) {
            return false;
        }
        return lhs.getIndex() == rhs.getIndex() && lhs.getBlockType() == rhs.getBlockType();
    }

    private void applyRewrite(List<Op04StructuredStatement> statements, int idx, RewriteCandidate candidate) {
        Op04StructuredStatement gateContainer = statements.get(idx);
        StructuredIf gate = (StructuredIf) gateContainer.getStatement();
        StructuredIf later = candidate.laterIf;

        Op04StructuredStatement targetBranch = candidate.targetsIfTaken ? later.getIfTaken() : later.getElseBlock();
        Op04StructuredStatement breakBranch = candidate.targetsIfTaken ? later.getElseBlock() : later.getIfTaken();
        if (targetBranch == null || breakBranch == null) {
            return;
        }

        BlockIdentifier syntheticBlock = nextSyntheticBlock();
        LinkedList<Op04StructuredStatement> breakBranchItems = ListFactory.newLinkedList();
        breakBranchItems.add(breakBranch);
        breakBranchItems.add(new Op04StructuredStatement(new StructuredBreak(BytecodeLoc.TODO, syntheticBlock, false)));
        Op04StructuredStatement escapeIf = new Op04StructuredStatement(new StructuredIf(
                BytecodeLoc.TODO,
                candidate.targetsIfTaken
                        ? later.getConditionalExpression().getNegated().simplify()
                        : later.getConditionalExpression().simplify(),
                new Op04StructuredStatement(new Block(breakBranchItems, false))
        ));

        LinkedList<Op04StructuredStatement> falsePathItems = ListFactory.newLinkedList();
        for (int x = idx + 1; x < candidate.statementIdx; ++x) {
            falsePathItems.add(statements.get(x));
        }
        falsePathItems.add(escapeIf);

        Op04StructuredStatement rewrittenGate = new Op04StructuredStatement(new StructuredIf(
                BytecodeLoc.TODO,
                gate.getConditionalExpression().getNegated().simplify(),
                new Op04StructuredStatement(new Block(falsePathItems, false))
        ));

        LinkedList<Op04StructuredStatement> rewrittenItems = ListFactory.newLinkedList();
        rewrittenItems.add(rewrittenGate);
        rewrittenItems.add(targetBranch);
        syntheticBlock.addForeignRef();
        Op04StructuredStatement replacement = new Op04StructuredStatement(
                gateContainer.getIndex(),
                gateContainer.getBlockIdentifiers(),
                new Block(rewrittenItems, true, syntheticBlock)
        );

        targetBranch.getSources().clear();
        breakBranch.getSources().clear();

        for (int x = candidate.statementIdx; x >= idx; --x) {
            statements.remove(x);
        }
        statements.add(idx, replacement);
    }

    private static synchronized BlockIdentifier nextSyntheticBlock() {
        return new BlockIdentifier(syntheticBlockIndex--, BlockType.ANONYMOUS);
    }

    private static final class RewriteCandidate {
        private final int statementIdx;
        private final StructuredIf laterIf;
        private final boolean targetsIfTaken;

        private RewriteCandidate(int statementIdx, StructuredIf laterIf, boolean targetsIfTaken) {
            this.statementIdx = statementIdx;
            this.laterIf = laterIf;
            this.targetsIfTaken = targetsIfTaken;
        }
    }

    private static final class GuardedBreakPattern {
        private final StructuredIf innerIf;
        private final LinkedList<Op04StructuredStatement> prepClones;
        private final LinkedList<Op04StructuredStatement> innerThenClones;
        private final int elseBodyIdx;

        private GuardedBreakPattern(StructuredIf innerIf,
                                    LinkedList<Op04StructuredStatement> prepClones,
                                    LinkedList<Op04StructuredStatement> innerThenClones,
                                    int elseBodyIdx) {
            this.innerIf = innerIf;
            this.prepClones = prepClones;
            this.innerThenClones = innerThenClones;
            this.elseBodyIdx = elseBodyIdx;
        }
    }

    private static final class DispatchPattern {
        private final List<Op04StructuredStatement> prefixStatements;
        private final List<Op04StructuredStatement> tailStatements;
        private final ConditionalExpression gateCondition;
        private final ConditionalExpression selectorSuccessCondition;

        private DispatchPattern(List<Op04StructuredStatement> prefixStatements,
                                List<Op04StructuredStatement> tailStatements,
                                ConditionalExpression gateCondition,
                                ConditionalExpression selectorSuccessCondition) {
            this.prefixStatements = prefixStatements;
            this.tailStatements = tailStatements;
            this.gateCondition = gateCondition;
            this.selectorSuccessCondition = selectorSuccessCondition;
        }
    }
}
