package org.benf.cfr.reader.bytecode;

import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.*;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.ExpressionRewriterTransformer;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.StructuredStatementTransformer;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.BadLoopPrettifier;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.ControlFlowCleaningTransformer;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.ForwardIfGotoRewriter;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.TypedBooleanTidier;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.UnusedAnonymousBlockFlattener;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.AbstractExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.BlockIdentifier;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Triplet;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredScope;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.expression.StructuredStatementExpression;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.Block;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredBreak;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredComment;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.UnstructuredAnonymousBreak;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.UnstructuredCatch;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.UnstructuredGoto;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.UnstructuredIf;
import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.transformers.CanRemovePointlessBlock;
import org.benf.cfr.reader.util.getopt.Options;
import org.benf.cfr.reader.util.getopt.OptionsImpl;

import java.util.List;
import java.util.Set;
import java.util.Stack;

public final class StructureRecoveryTransforms {
    private StructureRecoveryTransforms() {
    }

    public static void flattenNonReferencedBlocks(Op04StructuredStatement root) {
        applyStructured(root, new UnusedAnonymousBlockFlattener());
    }

    public static void insertLabelledBlocks(Op04StructuredStatement root) {
        applyStructured(root, preOrderTransformer((statement, scope) -> {
            if (statement instanceof Block) {
                ((Block) statement).extractLabelledBlocks();
            }
            return statement;
        }));
    }

    public static void tidyEmptyCatch(Op04StructuredStatement root) {
        applyStructured(root, preOrderTransformer((statement, scope) ->
                statement instanceof UnstructuredCatch ? ((UnstructuredCatch) statement).getCatchForEmpty() : statement));
    }

    public static void tidyTryCatch(Op04StructuredStatement root) {
        applyStructured(root, preOrderTransformer((statement, scope) -> {
            if (statement instanceof Block) {
                ((Block) statement).combineTryCatch();
            }
            return statement;
        }));
    }

    public static void inlinePossibles(Op04StructuredStatement root) {
        applyStructured(root, postOrderTransformer((statement, scope) -> {
            if (statement instanceof Block) {
                ((Block) statement).combineInlineable();
            }
            return statement;
        }));
    }

    public static void convertUnstructuredIf(Op04StructuredStatement root) {
        applyStructured(root, postOrderTransformer((statement, scope) ->
                statement instanceof UnstructuredIf ? ((UnstructuredIf) statement).convertEmptyToGoto() : statement));
    }

    public static void removePointlessReturn(Op04StructuredStatement root) {
        StructuredStatement statement = root.getStatement();
        if (statement instanceof Block) {
            ((Block) statement).removeLastNVReturn();
        }
    }

    public static void removePointlessControlFlow(Op04StructuredStatement root) {
        new ControlFlowCleaningTransformer().transform(root);
    }

    public static void tidyTypedBooleans(Op04StructuredStatement root) {
        new TypedBooleanTidier().transform(root);
    }

    public static void prettifyBadLoops(Op04StructuredStatement root) {
        new BadLoopPrettifier().transform(root);
    }

    public static void removeStructuredGotos(Op04StructuredStatement root) {
        applyStructured(root, scopeDescendingTransformer((statement, targets, scope) -> {
            if (statement instanceof UnstructuredGoto || statement instanceof UnstructuredAnonymousBreak) {
                return transformStructuredGotoWithScope(scope, statement, targets);
            }
            return statement;
        }));
    }

    public static void removeUnnecessaryLabelledBreaks(Op04StructuredStatement root) {
        applyStructured(root, scopeDescendingTransformer((statement, targets, scope) ->
                statement instanceof StructuredBreak ? ((StructuredBreak) statement).maybeTightenToLocal(targets) : statement));
    }

    public static void removePointlessBlocks(Op04StructuredStatement root) {
        applyStructured(root, postOrderTransformer((statement, scope) -> {
            if (statement instanceof CanRemovePointlessBlock) {
                ((CanRemovePointlessBlock) statement).removePointlessBlocks(scope);
                return statement.getContainer().getStatement();
            }
            return statement;
        }));
    }

    public static void cleanupStructuredExpressionBodies(Op04StructuredStatement root) {
        new ExpressionRewriterTransformer(new StructuredExpressionBodyCleaner()).transform(root);
    }

    public static void rewriteForwardIfGotos(Op04StructuredStatement root) {
        new ForwardIfGotoRewriter().transform(root);
    }

    public static void removePrimitiveDeconversion(Options options, Op04StructuredStatement root) {
        if (!options.getOption(OptionsImpl.SUGAR_BOXING)) {
            return;
        }
        new ExpressionRewriterTransformer(new PrimitiveBoxingRewriter()).transform(root);
    }

    private static void applyStructured(Op04StructuredStatement root, StructuredStatementTransformer transformer) {
        root.transform(transformer, new StructuredScope());
    }

    private static StructuredStatementTransformer preOrderTransformer(StructuredStatementMutation mutation) {
        return new StructuredStatementTransformer() {
            @Override
            public StructuredStatement transform(StructuredStatement in, StructuredScope scope) {
                StructuredStatement out = mutation.apply(in, scope);
                out.transformStructuredChildren(this, scope);
                return out;
            }
        };
    }

    private static StructuredStatementTransformer postOrderTransformer(StructuredStatementMutation mutation) {
        return new StructuredStatementTransformer() {
            @Override
            public StructuredStatement transform(StructuredStatement in, StructuredScope scope) {
                in.transformStructuredChildren(this, scope);
                return mutation.apply(in, scope);
            }
        };
    }

    private static StructuredStatement transformStructuredGotoWithScope(
            StructuredScope scope,
            StructuredStatement statement,
            Stack<Triplet<StructuredStatement, BlockIdentifier, Set<Op04StructuredStatement>>> breakTargets) {
        Set<Op04StructuredStatement> nextFallThrough = scope.getNextFallThrough(statement);
        List<Op04StructuredStatement> targets = statement.getContainer().getTargets();
        Op04StructuredStatement target = targets.isEmpty() ? null : targets.get(0);
        if (nextFallThrough.contains(target)) {
            if (scope.statementIsLast(statement) || scope.getDirectFallThrough().contains(target)) {
                return StructuredComment.EMPTY_COMMENT;
            }
            return statement;
        }
        if (!breakTargets.isEmpty()) {
            Triplet<StructuredStatement, BlockIdentifier, Set<Op04StructuredStatement>> breakTarget = breakTargets.peek();
            if (breakTarget.getThird().contains(target)) {
                return new StructuredBreak(BytecodeLoc.TODO, breakTarget.getSecond(), true);
            }
        }
        return statement;
    }

    private static StructuredStatementTransformer scopeDescendingTransformer(ScopeStructuredMutation mutation) {
        return new StructuredStatementTransformer() {
            private final Stack<Triplet<StructuredStatement, BlockIdentifier, Set<Op04StructuredStatement>>> targets =
                    new Stack<Triplet<StructuredStatement, BlockIdentifier, Set<Op04StructuredStatement>>>();

            @Override
            public StructuredStatement transform(StructuredStatement in, StructuredScope scope) {
                BlockIdentifier breakableBlock = in.getBreakableBlockOrNull();
                if (breakableBlock != null) {
                    targets.push(Triplet.make(in, breakableBlock, scope.getNextFallThrough(in)));
                }
                StructuredStatement out = in;
                try {
                    out.transformStructuredChildrenInReverse(this, scope);
                    out = mutation.apply(out, targets, scope);
                    if (out instanceof StructuredBreak) {
                        out = ((StructuredBreak) out).maybeTightenToLocal(targets);
                    }
                    return out;
                } finally {
                    if (breakableBlock != null) {
                        targets.pop();
                    }
                }
            }
        };
    }

    private static final class StructuredExpressionBodyCleaner extends AbstractExpressionRewriter {
        @Override
        public Expression rewriteExpression(Expression expression,
                                            SSAIdentifiers ssaIdentifiers,
                                            StatementContainer statementContainer,
                                            ExpressionRewriterFlags flags) {
            if (expression instanceof StructuredStatementExpression) {
                ((StructuredStatementExpression) expression).cleanupContent();
            }
            return expression.applyExpressionRewriter(this, ssaIdentifiers, statementContainer, flags);
        }
    }

    @FunctionalInterface
    private interface StructuredStatementMutation {
        StructuredStatement apply(StructuredStatement statement, StructuredScope scope);
    }

    @FunctionalInterface
    private interface ScopeStructuredMutation {
        StructuredStatement apply(StructuredStatement statement,
                                  Stack<Triplet<StructuredStatement, BlockIdentifier, Set<Op04StructuredStatement>>> targets,
                                  StructuredScope scope);
    }
}
