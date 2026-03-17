package org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters;

import org.benf.cfr.reader.bytecode.analysis.loc.BytecodeLoc;
import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.AssignmentExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.CastExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.LValueExpression;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.Literal;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.MemberFunctionInvokation;
import org.benf.cfr.reader.bytecode.analysis.parse.expression.PatternCaseLabel;
import org.benf.cfr.reader.bytecode.analysis.parse.pattern.BindingPattern;
import org.benf.cfr.reader.bytecode.analysis.parse.pattern.GuardedPattern;
import org.benf.cfr.reader.bytecode.analysis.parse.pattern.RecordPattern;
import org.benf.cfr.reader.bytecode.analysis.parse.literal.TypedLiteral;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.LocalVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.AbstractExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.CloneHelper;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.SSAIdentifiers;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.AbstractStructuredContinue;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.Block;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredAssignment;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredCase;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredCatch;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredComment;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredDefinition;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredIf;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredThrow;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredTry;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredWhile;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.util.collections.ListFactory;

import java.util.LinkedList;
import java.util.List;

final class SwitchPatternCasePlanner {
    List<CaseRewritePlan> planCases(Block switchBlock,
                                    SwitchPatternBootstrapMatch bootstrapMatch,
                                    StructuredWhile patternLoop) {
        List<CaseRewritePlan> plans = ListFactory.newList();
        for (Op04StructuredStatement statement : switchBlock.getBlockStatements()) {
            StructuredStatement structuredStatement = statement.getStatement();
            if (structuredStatement instanceof StructuredComment || structuredStatement.isEffectivelyNOP()) {
                continue;
            }
            if (!(structuredStatement instanceof StructuredCase)) {
                return null;
            }
            CaseRewritePlan plan = CaseRewritePlan.plan((StructuredCase) structuredStatement, bootstrapMatch, patternLoop);
            if (plan == null) {
                return null;
            }
            plans.add(plan);
        }
        return plans;
    }

    static final class CaseRewritePlan {
        private final StructuredCase structuredCase;
        private final Block bodyBlock;
        private final Expression rewrittenValue;
        private final LinkedList<Op04StructuredStatement> rewrittenStatements;

        private CaseRewritePlan(StructuredCase structuredCase,
                                Block bodyBlock,
                                Expression rewrittenValue,
                                LinkedList<Op04StructuredStatement> rewrittenStatements) {
            this.structuredCase = structuredCase;
            this.bodyBlock = bodyBlock;
            this.rewrittenValue = rewrittenValue;
            this.rewrittenStatements = rewrittenStatements;
        }

        void apply() {
            if (rewrittenValue != null) {
                structuredCase.setValues(ListFactory.newImmutableList(rewrittenValue));
            }
            if (bodyBlock != null && rewrittenStatements != null) {
                bodyBlock.replaceBlockStatements(rewrittenStatements);
            }
        }

        private static CaseRewritePlan plan(StructuredCase structuredCase,
                                            SwitchPatternBootstrapMatch match,
                                            StructuredWhile patternLoop) {
            List<Expression> values = structuredCase.getValues();
            if (values.isEmpty()) {
                return new CaseRewritePlan(structuredCase, null, null, null);
            }
            if (values.size() != 1) return null;
            Integer caseIndex = getCaseIndex(values.get(0));
            if (caseIndex == null) return null;
            if (caseIndex == -1) {
                return new CaseRewritePlan(structuredCase, null, Literal.NULL, null);
            }
            if (caseIndex < 0 || caseIndex >= match.getCaseTypes().size()) return null;
            if (!(structuredCase.getBody().getStatement() instanceof Block)) return null;

            Block bodyBlock = (Block) structuredCase.getBody().getStatement();
            List<Op04StructuredStatement> statements = bodyBlock.getBlockStatements();
            JavaTypeInstance expectedType = match.getCaseTypes().get(caseIndex);
            PatternBinding patternBinding = findPatternBinding(statements, match.getSelector(), expectedType);
            if (patternBinding == null) {
                ConditionalGuardPlan conditionalGuardPlan = findConditionalGuardPlan(
                        statements, match.getSelector(), expectedType, match.getIndexLValue(), patternLoop);
                if (conditionalGuardPlan == null) {
                    return null;
                }
                return new CaseRewritePlan(
                        structuredCase,
                        bodyBlock,
                        new PatternCaseLabel(
                                BytecodeLoc.NONE,
                                new GuardedPattern(
                                        new BindingPattern(conditionalGuardPlan.binding.getInferredJavaType().getJavaTypeInstance(), conditionalGuardPlan.binding),
                                        conditionalGuardPlan.guard
                                )
                        ),
                        conditionalGuardPlan.rewrittenBody
                );
            }

            GuardPlan guardPlan = findGuardPlan(statements, patternBinding.bindingIndex, match.getIndexLValue(), patternLoop);
            if (guardPlan != null) {
                return new CaseRewritePlan(
                        structuredCase,
                        bodyBlock,
                        new PatternCaseLabel(
                                BytecodeLoc.NONE,
                                new GuardedPattern(
                                        new BindingPattern(patternBinding.binding.getInferredJavaType().getJavaTypeInstance(), patternBinding.binding),
                                        guardPlan.guard
                                )
                        ),
                        guardPlan.rewrittenBody
                );
            }

            RecordPatternPlan recordPatternPlan = findRecordPatternPlan(statements, patternBinding.bindingIndex, patternBinding.binding);
            if (recordPatternPlan != null) {
                return new CaseRewritePlan(
                        structuredCase,
                        bodyBlock,
                        new PatternCaseLabel(
                                BytecodeLoc.NONE,
                                new RecordPattern(
                                        patternBinding.binding.getInferredJavaType().getJavaTypeInstance(),
                                        recordPatternPlan.components
                                )
                        ),
                        recordPatternPlan.rewrittenBody
                );
            }

            LinkedList<Op04StructuredStatement> rewrittenStatements = ListFactory.newLinkedList();
            for (int x = 0; x < statements.size(); ++x) {
                if (x == patternBinding.bindingIndex) continue;
                rewrittenStatements.add(statements.get(x));
            }
            return new CaseRewritePlan(
                    structuredCase,
                    bodyBlock,
                    new PatternCaseLabel(
                            BytecodeLoc.NONE,
                            new BindingPattern(patternBinding.binding.getInferredJavaType().getJavaTypeInstance(), patternBinding.binding)
                    ),
                    rewrittenStatements
            );
        }

        private static Integer getCaseIndex(Expression expression) {
            if (!(expression instanceof Literal)) return null;
            TypedLiteral literal = ((Literal) expression).getValue();
            if (literal.getType() != TypedLiteral.LiteralType.Integer) return null;
            return literal.getIntValue();
        }

        private static PatternBinding findPatternBinding(List<Op04StructuredStatement> statements,
                                                         Expression selector,
                                                         JavaTypeInstance expectedType) {
            for (int x = 0; x < statements.size(); ++x) {
                StructuredStatement statement = statements.get(x).getStatement();
                if (!(statement instanceof StructuredAssignment)) continue;
                StructuredAssignment assignment = (StructuredAssignment) statement;
                if (!(assignment.getLvalue() instanceof LocalVariable)) continue;
                if (!(assignment.getRvalue() instanceof CastExpression)) continue;
                CastExpression castExpression = (CastExpression) assignment.getRvalue();
                if (!expectedType.equals(castExpression.getInferredJavaType().getJavaTypeInstance())) continue;
                Expression castChild = CastExpression.removeImplicit(castExpression.getChild());
                if (!selector.equals(castChild)) continue;
                return new PatternBinding((LocalVariable) assignment.getLvalue(), x);
            }
            return null;
        }

        private static GuardPlan findGuardPlan(List<Op04StructuredStatement> statements,
                                               int bindingIndex,
                                               LValue indexLValue,
                                               StructuredWhile patternLoop) {
            if (patternLoop == null) return null;
            int ifIdx = nextMeaningfulIndex(statements, bindingIndex + 1);
            if (ifIdx == -1) return null;
            StructuredStatement statement = statements.get(ifIdx).getStatement();
            if (!(statement instanceof StructuredIf)) return null;
            StructuredIf structuredIf = (StructuredIf) statement;
            if (structuredIf.hasElseBlock()) return null;

            int assignIdx = nextMeaningfulIndex(statements, ifIdx + 1);
            int continueIdx = nextMeaningfulIndex(statements, assignIdx + 1);
            if (assignIdx == -1 || continueIdx == -1) return null;

            StructuredStatement indexAssignment = statements.get(assignIdx).getStatement();
            if (!(indexAssignment instanceof StructuredAssignment)) return null;
            StructuredAssignment structuredAssignment = (StructuredAssignment) indexAssignment;
            if (!indexLValue.equals(structuredAssignment.getLvalue())) return null;
            if (!(structuredAssignment.getRvalue() instanceof Literal)) return null;

            StructuredStatement continueStatement = statements.get(continueIdx).getStatement();
            if (!(continueStatement instanceof AbstractStructuredContinue)) return null;
            if (!patternLoop.getBlock().equals(((AbstractStructuredContinue) continueStatement).getContinueTgt())) return null;

            if (nextMeaningfulIndex(statements, continueIdx + 1) != -1) return null;

            LinkedList<Op04StructuredStatement> rewrittenStatements = ListFactory.newLinkedList();
            for (int x = 0; x < bindingIndex; ++x) {
                rewrittenStatements.add(statements.get(x));
            }
            StructuredStatement ifTaken = structuredIf.getIfTaken().getStatement();
            if (ifTaken instanceof Block) {
                rewrittenStatements.addAll(((Block) ifTaken).getBlockStatements());
            } else {
                rewrittenStatements.add(structuredIf.getIfTaken());
            }
            return new GuardPlan(structuredIf.getConditionalExpression(), rewrittenStatements);
        }

        private static ConditionalGuardPlan findConditionalGuardPlan(List<Op04StructuredStatement> statements,
                                                                     Expression selector,
                                                                     JavaTypeInstance expectedType,
                                                                     LValue indexLValue,
                                                                     StructuredWhile patternLoop) {
            if (patternLoop == null) return null;
            int ifIdx = nextLeadingGuardIndex(statements);
            if (ifIdx == -1) return null;
            StructuredStatement statement = statements.get(ifIdx).getStatement();
            if (!(statement instanceof StructuredIf)) return null;
            StructuredIf structuredIf = (StructuredIf) statement;
            if (structuredIf.hasElseBlock()) return null;

            ConditionPatternBinding conditionBinding = ConditionPatternBinding.match(structuredIf.getConditionalExpression(), selector, expectedType);
            if (conditionBinding == null) return null;

            int assignIdx = nextMeaningfulIndex(statements, ifIdx + 1);
            int continueIdx = nextMeaningfulIndex(statements, assignIdx + 1);
            if (assignIdx == -1 || continueIdx == -1) return null;

            StructuredStatement indexAssignment = statements.get(assignIdx).getStatement();
            if (!(indexAssignment instanceof StructuredAssignment)) return null;
            StructuredAssignment structuredAssignment = (StructuredAssignment) indexAssignment;
            if (!indexLValue.equals(structuredAssignment.getLvalue())) return null;
            if (!(structuredAssignment.getRvalue() instanceof Literal)) return null;

            StructuredStatement continueStatement = statements.get(continueIdx).getStatement();
            if (!(continueStatement instanceof AbstractStructuredContinue)) return null;
            if (!patternLoop.getBlock().equals(((AbstractStructuredContinue) continueStatement).getContinueTgt())) return null;

            if (nextMeaningfulIndex(statements, continueIdx + 1) != -1) return null;

            LinkedList<Op04StructuredStatement> rewrittenStatements = ListFactory.newLinkedList();
            for (int x = 0; x < ifIdx; ++x) {
                if (isBindingSetupStatement(statements.get(x).getStatement(), conditionBinding.binding)) {
                    continue;
                }
                rewrittenStatements.add(statements.get(x));
            }
            StructuredStatement ifTaken = structuredIf.getIfTaken().getStatement();
            if (ifTaken instanceof Block) {
                rewrittenStatements.addAll(((Block) ifTaken).getBlockStatements());
            } else {
                rewrittenStatements.add(structuredIf.getIfTaken());
            }
            return new ConditionalGuardPlan(conditionBinding.binding, conditionBinding.guard, rewrittenStatements);
        }

        private static int nextMeaningfulIndex(List<Op04StructuredStatement> statements, int start) {
            for (int x = start; x < statements.size(); ++x) {
                if (!statements.get(x).getStatement().isEffectivelyNOP()) {
                    return x;
                }
            }
            return -1;
        }

        private static int nextLeadingGuardIndex(List<Op04StructuredStatement> statements) {
            for (int x = 0; x < statements.size(); ++x) {
                StructuredStatement statement = statements.get(x).getStatement();
                if (statement.isEffectivelyNOP() || statement instanceof StructuredComment) {
                    continue;
                }
                if (statement instanceof StructuredIf) {
                    return x;
                }
                if (statement instanceof StructuredDefinition) {
                    continue;
                }
                return -1;
            }
            return -1;
        }

        private static boolean isBindingSetupStatement(StructuredStatement statement, LocalVariable binding) {
            if (statement instanceof StructuredDefinition) {
                return binding.equals(((StructuredDefinition) statement).getLvalue());
            }
            if (statement instanceof StructuredAssignment) {
                return binding.equals(((StructuredAssignment) statement).getLvalue());
            }
            return false;
        }

        private static RecordPatternPlan findRecordPatternPlan(List<Op04StructuredStatement> statements,
                                                               int bindingIndex,
                                                               LocalVariable binding) {
            int tryIdx = nextMeaningfulIndex(statements, bindingIndex + 1);
            if (tryIdx == -1) return null;
            StructuredStatement tryStatement = statements.get(tryIdx).getStatement();
            if (!(tryStatement instanceof StructuredTry)) return null;
            StructuredTry structuredTry = (StructuredTry) tryStatement;
            if (structuredTry.hasResources()) return null;
            if (structuredTry.getFinallyBlock() != null) return null;
            if (structuredTry.getCatchBlocks().size() != 1) return null;
            if (!(structuredTry.getTryBlock().getStatement() instanceof Block)) return null;
            if (!hasSingleThrowCatch(structuredTry)) return null;

            List<LValue> components = extractRecordComponents((Block) structuredTry.getTryBlock().getStatement(), binding);
            if (components == null || components.isEmpty()) return null;
            if (usesLValue(statements, tryIdx + 1, binding)) return null;

            LinkedList<Op04StructuredStatement> rewrittenStatements = ListFactory.newLinkedList();
            for (int x = 0; x < bindingIndex; ++x) {
                Op04StructuredStatement statement = statements.get(x);
                if (shouldDropRecordPatternPreludeStatement(statement, components)) {
                    continue;
                }
                rewrittenStatements.add(statement);
            }
            for (int x = tryIdx + 1; x < statements.size(); ++x) {
                rewrittenStatements.add(statements.get(x));
            }
            return new RecordPatternPlan(components, rewrittenStatements);
        }

        private static List<LValue> extractRecordComponents(Block tryBlock, LocalVariable binding) {
            List<LValue> components = ListFactory.newList();
            List<Op04StructuredStatement> tryStatements = tryBlock.getFilteredBlockStatements();
            for (Op04StructuredStatement tryStatement : tryStatements) {
                StructuredStatement structuredStatement = tryStatement.getStatement();
                if (structuredStatement instanceof StructuredComment) {
                    continue;
                }
                if (structuredStatement instanceof StructuredDefinition) {
                    continue;
                }
                if (!(structuredStatement instanceof StructuredAssignment)) {
                    return null;
                }
                StructuredAssignment assignment = (StructuredAssignment) structuredStatement;
                if (!(assignment.getLvalue() instanceof LocalVariable)) return null;
                if (!isRecordAccessorAssignment(assignment.getRvalue(), binding)) return null;
                components.add(assignment.getLvalue());
            }
            return components;
        }

        private static boolean shouldDropRecordPatternPreludeStatement(Op04StructuredStatement statement, List<LValue> components) {
            StructuredStatement structuredStatement = statement.getStatement();
            if (!(structuredStatement instanceof StructuredDefinition)) return false;
            return components.contains(((StructuredDefinition) structuredStatement).getLvalue());
        }

        private static boolean hasSingleThrowCatch(StructuredTry structuredTry) {
            StructuredStatement catchStatement = structuredTry.getCatchBlocks().get(0).getStatement();
            if (!(catchStatement instanceof StructuredCatch)) return false;
            StructuredCatch structuredCatch = (StructuredCatch) catchStatement;
            if (!(structuredCatch.getCatchBlock().getStatement() instanceof Block)) return false;
            List<Op04StructuredStatement> statements = ((Block) structuredCatch.getCatchBlock().getStatement()).getFilteredBlockStatements();
            return statements.size() == 1 && statements.get(0).getStatement() instanceof StructuredThrow;
        }

        private static boolean isRecordAccessorAssignment(Expression expression, LocalVariable binding) {
            Expression current = expression;
            while (current instanceof AssignmentExpression) {
                current = ((AssignmentExpression) current).getrValue();
            }
            if (!(current instanceof MemberFunctionInvokation)) return false;
            MemberFunctionInvokation memberFunctionInvokation = (MemberFunctionInvokation) current;
            if (!memberFunctionInvokation.getArgs().isEmpty()) return false;
            Expression object = CastExpression.removeImplicit(memberFunctionInvokation.getObject());
            if (!(object instanceof LValueExpression)) return false;
            return binding.equals(((LValueExpression) object).getLValue());
        }

        private static boolean usesLValue(List<Op04StructuredStatement> statements, int start, LocalVariable binding) {
            UsageCheck usageCheck = new UsageCheck(binding);
            for (int x = start; x < statements.size(); ++x) {
                statements.get(x).getStatement().rewriteExpressions(usageCheck);
                if (usageCheck.failed) {
                    return true;
                }
            }
            return false;
        }
    }

    private static final class PatternBinding {
        private final LocalVariable binding;
        private final int bindingIndex;

        private PatternBinding(LocalVariable binding, int bindingIndex) {
            this.binding = binding;
            this.bindingIndex = bindingIndex;
        }
    }

    private static final class GuardPlan {
        private final Expression guard;
        private final LinkedList<Op04StructuredStatement> rewrittenBody;

        private GuardPlan(Expression guard, LinkedList<Op04StructuredStatement> rewrittenBody) {
            this.guard = guard;
            this.rewrittenBody = rewrittenBody;
        }
    }

    private static final class ConditionPatternBinding {
        private final LocalVariable binding;
        private final Expression guard;

        private ConditionPatternBinding(LocalVariable binding, Expression guard) {
            this.binding = binding;
            this.guard = guard;
        }

        private static ConditionPatternBinding match(Expression expression,
                                                     Expression selector,
                                                     JavaTypeInstance expectedType) {
            CloneHelper cloneHelper = new CloneHelper();
            Expression cloned = cloneHelper.replaceOrClone(expression);
            ConditionPatternBindingRewriter rewriter = new ConditionPatternBindingRewriter(selector, expectedType);
            Expression rewritten = cloned.applyExpressionRewriter(rewriter, null, null, null);
            if (rewriter.binding == null) {
                return null;
            }
            return new ConditionPatternBinding(rewriter.binding, rewritten);
        }
    }

    private static final class ConditionPatternBindingRewriter extends AbstractExpressionRewriter {
        private final Expression selector;
        private final JavaTypeInstance expectedType;
        private LocalVariable binding;

        private ConditionPatternBindingRewriter(Expression selector, JavaTypeInstance expectedType) {
            this.selector = selector;
            this.expectedType = expectedType;
        }

        @Override
        public Expression rewriteExpression(Expression expression,
                                            SSAIdentifiers ssaIdentifiers,
                                            org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer statementContainer,
                                            ExpressionRewriterFlags flags) {
            if (expression instanceof AssignmentExpression) {
                AssignmentExpression assignmentExpression = (AssignmentExpression) expression;
                if (assignmentExpression.getlValue() instanceof LocalVariable
                        && assignmentExpression.getrValue() instanceof CastExpression) {
                    CastExpression castExpression = (CastExpression) assignmentExpression.getrValue();
                    if (expectedType.equals(castExpression.getInferredJavaType().getJavaTypeInstance())) {
                        Expression castChild = CastExpression.removeImplicit(castExpression.getChild());
                        if (selector.equals(castChild)) {
                            LocalVariable localVariable = (LocalVariable) assignmentExpression.getlValue();
                            if (binding == null || binding.equals(localVariable)) {
                                binding = localVariable;
                                return new LValueExpression(localVariable);
                            }
                        }
                    }
                }
            }
            return super.rewriteExpression(expression, ssaIdentifiers, statementContainer, flags);
        }
    }

    private static final class ConditionalGuardPlan {
        private final LocalVariable binding;
        private final Expression guard;
        private final LinkedList<Op04StructuredStatement> rewrittenBody;

        private ConditionalGuardPlan(LocalVariable binding, Expression guard, LinkedList<Op04StructuredStatement> rewrittenBody) {
            this.binding = binding;
            this.guard = guard;
            this.rewrittenBody = rewrittenBody;
        }
    }

    private static final class RecordPatternPlan {
        private final List<LValue> components;
        private final LinkedList<Op04StructuredStatement> rewrittenBody;

        private RecordPatternPlan(List<LValue> components, LinkedList<Op04StructuredStatement> rewrittenBody) {
            this.components = components;
            this.rewrittenBody = rewrittenBody;
        }
    }

    private static final class UsageCheck extends AbstractExpressionRewriter {
        private final LValue target;
        private boolean failed;

        private UsageCheck(LValue target) {
            this.target = target;
        }

        @Override
        public LValue rewriteExpression(LValue lValue,
                                        SSAIdentifiers ssaIdentifiers,
                                        org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer statementContainer,
                                        ExpressionRewriterFlags flags) {
            if (target.equals(lValue)) {
                failed = true;
            }
            return super.rewriteExpression(lValue, ssaIdentifiers, statementContainer, flags);
        }
    }
}
