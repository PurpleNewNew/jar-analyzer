// Generated from /Users/veritas/Documents/projects/jar-analyzer/src/main/antlr4/org/neo4j/cypher/internal/parser/v5/Cypher5Parser.g4 by ANTLR 4.13.2
package org.neo4j.cypher.internal.parser.v5;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link Cypher5Parser}.
 */
public interface Cypher5ParserListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#statements}.
	 * @param ctx the parse tree
	 */
	default void enterStatements(Cypher5Parser.StatementsContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#statements}.
	 * @param ctx the parse tree
	 */
	default void exitStatements(Cypher5Parser.StatementsContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#statement}.
	 * @param ctx the parse tree
	 */
	default void enterStatement(Cypher5Parser.StatementContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#statement}.
	 * @param ctx the parse tree
	 */
	default void exitStatement(Cypher5Parser.StatementContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#periodicCommitQueryHintFailure}.
	 * @param ctx the parse tree
	 */
	default void enterPeriodicCommitQueryHintFailure(Cypher5Parser.PeriodicCommitQueryHintFailureContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#periodicCommitQueryHintFailure}.
	 * @param ctx the parse tree
	 */
	default void exitPeriodicCommitQueryHintFailure(Cypher5Parser.PeriodicCommitQueryHintFailureContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#regularQuery}.
	 * @param ctx the parse tree
	 */
	default void enterRegularQuery(Cypher5Parser.RegularQueryContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#regularQuery}.
	 * @param ctx the parse tree
	 */
	default void exitRegularQuery(Cypher5Parser.RegularQueryContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#singleQuery}.
	 * @param ctx the parse tree
	 */
	default void enterSingleQuery(Cypher5Parser.SingleQueryContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#singleQuery}.
	 * @param ctx the parse tree
	 */
	default void exitSingleQuery(Cypher5Parser.SingleQueryContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#clause}.
	 * @param ctx the parse tree
	 */
	default void enterClause(Cypher5Parser.ClauseContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#clause}.
	 * @param ctx the parse tree
	 */
	default void exitClause(Cypher5Parser.ClauseContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#useClause}.
	 * @param ctx the parse tree
	 */
	default void enterUseClause(Cypher5Parser.UseClauseContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#useClause}.
	 * @param ctx the parse tree
	 */
	default void exitUseClause(Cypher5Parser.UseClauseContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#graphReference}.
	 * @param ctx the parse tree
	 */
	default void enterGraphReference(Cypher5Parser.GraphReferenceContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#graphReference}.
	 * @param ctx the parse tree
	 */
	default void exitGraphReference(Cypher5Parser.GraphReferenceContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#finishClause}.
	 * @param ctx the parse tree
	 */
	default void enterFinishClause(Cypher5Parser.FinishClauseContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#finishClause}.
	 * @param ctx the parse tree
	 */
	default void exitFinishClause(Cypher5Parser.FinishClauseContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#returnClause}.
	 * @param ctx the parse tree
	 */
	default void enterReturnClause(Cypher5Parser.ReturnClauseContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#returnClause}.
	 * @param ctx the parse tree
	 */
	default void exitReturnClause(Cypher5Parser.ReturnClauseContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#returnBody}.
	 * @param ctx the parse tree
	 */
	default void enterReturnBody(Cypher5Parser.ReturnBodyContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#returnBody}.
	 * @param ctx the parse tree
	 */
	default void exitReturnBody(Cypher5Parser.ReturnBodyContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#returnItem}.
	 * @param ctx the parse tree
	 */
	default void enterReturnItem(Cypher5Parser.ReturnItemContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#returnItem}.
	 * @param ctx the parse tree
	 */
	default void exitReturnItem(Cypher5Parser.ReturnItemContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#returnItems}.
	 * @param ctx the parse tree
	 */
	default void enterReturnItems(Cypher5Parser.ReturnItemsContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#returnItems}.
	 * @param ctx the parse tree
	 */
	default void exitReturnItems(Cypher5Parser.ReturnItemsContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#orderItem}.
	 * @param ctx the parse tree
	 */
	default void enterOrderItem(Cypher5Parser.OrderItemContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#orderItem}.
	 * @param ctx the parse tree
	 */
	default void exitOrderItem(Cypher5Parser.OrderItemContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#ascToken}.
	 * @param ctx the parse tree
	 */
	default void enterAscToken(Cypher5Parser.AscTokenContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#ascToken}.
	 * @param ctx the parse tree
	 */
	default void exitAscToken(Cypher5Parser.AscTokenContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#descToken}.
	 * @param ctx the parse tree
	 */
	default void enterDescToken(Cypher5Parser.DescTokenContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#descToken}.
	 * @param ctx the parse tree
	 */
	default void exitDescToken(Cypher5Parser.DescTokenContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#orderBy}.
	 * @param ctx the parse tree
	 */
	default void enterOrderBy(Cypher5Parser.OrderByContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#orderBy}.
	 * @param ctx the parse tree
	 */
	default void exitOrderBy(Cypher5Parser.OrderByContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#skip}.
	 * @param ctx the parse tree
	 */
	default void enterSkip(Cypher5Parser.SkipContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#skip}.
	 * @param ctx the parse tree
	 */
	default void exitSkip(Cypher5Parser.SkipContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#limit}.
	 * @param ctx the parse tree
	 */
	default void enterLimit(Cypher5Parser.LimitContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#limit}.
	 * @param ctx the parse tree
	 */
	default void exitLimit(Cypher5Parser.LimitContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#whereClause}.
	 * @param ctx the parse tree
	 */
	default void enterWhereClause(Cypher5Parser.WhereClauseContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#whereClause}.
	 * @param ctx the parse tree
	 */
	default void exitWhereClause(Cypher5Parser.WhereClauseContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#withClause}.
	 * @param ctx the parse tree
	 */
	default void enterWithClause(Cypher5Parser.WithClauseContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#withClause}.
	 * @param ctx the parse tree
	 */
	default void exitWithClause(Cypher5Parser.WithClauseContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#createClause}.
	 * @param ctx the parse tree
	 */
	default void enterCreateClause(Cypher5Parser.CreateClauseContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#createClause}.
	 * @param ctx the parse tree
	 */
	default void exitCreateClause(Cypher5Parser.CreateClauseContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#insertClause}.
	 * @param ctx the parse tree
	 */
	default void enterInsertClause(Cypher5Parser.InsertClauseContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#insertClause}.
	 * @param ctx the parse tree
	 */
	default void exitInsertClause(Cypher5Parser.InsertClauseContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#setClause}.
	 * @param ctx the parse tree
	 */
	default void enterSetClause(Cypher5Parser.SetClauseContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#setClause}.
	 * @param ctx the parse tree
	 */
	default void exitSetClause(Cypher5Parser.SetClauseContext ctx) { }
	/**
	 * Enter a parse tree produced by the {@code SetProp}
	 * labeled alternative in {@link Cypher5Parser#setItem}.
	 * @param ctx the parse tree
	 */
	default void enterSetProp(Cypher5Parser.SetPropContext ctx) { }
	/**
	 * Exit a parse tree produced by the {@code SetProp}
	 * labeled alternative in {@link Cypher5Parser#setItem}.
	 * @param ctx the parse tree
	 */
	default void exitSetProp(Cypher5Parser.SetPropContext ctx) { }
	/**
	 * Enter a parse tree produced by the {@code SetDynamicProp}
	 * labeled alternative in {@link Cypher5Parser#setItem}.
	 * @param ctx the parse tree
	 */
	default void enterSetDynamicProp(Cypher5Parser.SetDynamicPropContext ctx) { }
	/**
	 * Exit a parse tree produced by the {@code SetDynamicProp}
	 * labeled alternative in {@link Cypher5Parser#setItem}.
	 * @param ctx the parse tree
	 */
	default void exitSetDynamicProp(Cypher5Parser.SetDynamicPropContext ctx) { }
	/**
	 * Enter a parse tree produced by the {@code SetProps}
	 * labeled alternative in {@link Cypher5Parser#setItem}.
	 * @param ctx the parse tree
	 */
	default void enterSetProps(Cypher5Parser.SetPropsContext ctx) { }
	/**
	 * Exit a parse tree produced by the {@code SetProps}
	 * labeled alternative in {@link Cypher5Parser#setItem}.
	 * @param ctx the parse tree
	 */
	default void exitSetProps(Cypher5Parser.SetPropsContext ctx) { }
	/**
	 * Enter a parse tree produced by the {@code AddProp}
	 * labeled alternative in {@link Cypher5Parser#setItem}.
	 * @param ctx the parse tree
	 */
	default void enterAddProp(Cypher5Parser.AddPropContext ctx) { }
	/**
	 * Exit a parse tree produced by the {@code AddProp}
	 * labeled alternative in {@link Cypher5Parser#setItem}.
	 * @param ctx the parse tree
	 */
	default void exitAddProp(Cypher5Parser.AddPropContext ctx) { }
	/**
	 * Enter a parse tree produced by the {@code SetLabels}
	 * labeled alternative in {@link Cypher5Parser#setItem}.
	 * @param ctx the parse tree
	 */
	default void enterSetLabels(Cypher5Parser.SetLabelsContext ctx) { }
	/**
	 * Exit a parse tree produced by the {@code SetLabels}
	 * labeled alternative in {@link Cypher5Parser#setItem}.
	 * @param ctx the parse tree
	 */
	default void exitSetLabels(Cypher5Parser.SetLabelsContext ctx) { }
	/**
	 * Enter a parse tree produced by the {@code SetLabelsIs}
	 * labeled alternative in {@link Cypher5Parser#setItem}.
	 * @param ctx the parse tree
	 */
	default void enterSetLabelsIs(Cypher5Parser.SetLabelsIsContext ctx) { }
	/**
	 * Exit a parse tree produced by the {@code SetLabelsIs}
	 * labeled alternative in {@link Cypher5Parser#setItem}.
	 * @param ctx the parse tree
	 */
	default void exitSetLabelsIs(Cypher5Parser.SetLabelsIsContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#removeClause}.
	 * @param ctx the parse tree
	 */
	default void enterRemoveClause(Cypher5Parser.RemoveClauseContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#removeClause}.
	 * @param ctx the parse tree
	 */
	default void exitRemoveClause(Cypher5Parser.RemoveClauseContext ctx) { }
	/**
	 * Enter a parse tree produced by the {@code RemoveProp}
	 * labeled alternative in {@link Cypher5Parser#removeItem}.
	 * @param ctx the parse tree
	 */
	default void enterRemoveProp(Cypher5Parser.RemovePropContext ctx) { }
	/**
	 * Exit a parse tree produced by the {@code RemoveProp}
	 * labeled alternative in {@link Cypher5Parser#removeItem}.
	 * @param ctx the parse tree
	 */
	default void exitRemoveProp(Cypher5Parser.RemovePropContext ctx) { }
	/**
	 * Enter a parse tree produced by the {@code RemoveDynamicProp}
	 * labeled alternative in {@link Cypher5Parser#removeItem}.
	 * @param ctx the parse tree
	 */
	default void enterRemoveDynamicProp(Cypher5Parser.RemoveDynamicPropContext ctx) { }
	/**
	 * Exit a parse tree produced by the {@code RemoveDynamicProp}
	 * labeled alternative in {@link Cypher5Parser#removeItem}.
	 * @param ctx the parse tree
	 */
	default void exitRemoveDynamicProp(Cypher5Parser.RemoveDynamicPropContext ctx) { }
	/**
	 * Enter a parse tree produced by the {@code RemoveLabels}
	 * labeled alternative in {@link Cypher5Parser#removeItem}.
	 * @param ctx the parse tree
	 */
	default void enterRemoveLabels(Cypher5Parser.RemoveLabelsContext ctx) { }
	/**
	 * Exit a parse tree produced by the {@code RemoveLabels}
	 * labeled alternative in {@link Cypher5Parser#removeItem}.
	 * @param ctx the parse tree
	 */
	default void exitRemoveLabels(Cypher5Parser.RemoveLabelsContext ctx) { }
	/**
	 * Enter a parse tree produced by the {@code RemoveLabelsIs}
	 * labeled alternative in {@link Cypher5Parser#removeItem}.
	 * @param ctx the parse tree
	 */
	default void enterRemoveLabelsIs(Cypher5Parser.RemoveLabelsIsContext ctx) { }
	/**
	 * Exit a parse tree produced by the {@code RemoveLabelsIs}
	 * labeled alternative in {@link Cypher5Parser#removeItem}.
	 * @param ctx the parse tree
	 */
	default void exitRemoveLabelsIs(Cypher5Parser.RemoveLabelsIsContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#deleteClause}.
	 * @param ctx the parse tree
	 */
	default void enterDeleteClause(Cypher5Parser.DeleteClauseContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#deleteClause}.
	 * @param ctx the parse tree
	 */
	default void exitDeleteClause(Cypher5Parser.DeleteClauseContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#matchClause}.
	 * @param ctx the parse tree
	 */
	default void enterMatchClause(Cypher5Parser.MatchClauseContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#matchClause}.
	 * @param ctx the parse tree
	 */
	default void exitMatchClause(Cypher5Parser.MatchClauseContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#matchMode}.
	 * @param ctx the parse tree
	 */
	default void enterMatchMode(Cypher5Parser.MatchModeContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#matchMode}.
	 * @param ctx the parse tree
	 */
	default void exitMatchMode(Cypher5Parser.MatchModeContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#hint}.
	 * @param ctx the parse tree
	 */
	default void enterHint(Cypher5Parser.HintContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#hint}.
	 * @param ctx the parse tree
	 */
	default void exitHint(Cypher5Parser.HintContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#mergeClause}.
	 * @param ctx the parse tree
	 */
	default void enterMergeClause(Cypher5Parser.MergeClauseContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#mergeClause}.
	 * @param ctx the parse tree
	 */
	default void exitMergeClause(Cypher5Parser.MergeClauseContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#mergeAction}.
	 * @param ctx the parse tree
	 */
	default void enterMergeAction(Cypher5Parser.MergeActionContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#mergeAction}.
	 * @param ctx the parse tree
	 */
	default void exitMergeAction(Cypher5Parser.MergeActionContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#unwindClause}.
	 * @param ctx the parse tree
	 */
	default void enterUnwindClause(Cypher5Parser.UnwindClauseContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#unwindClause}.
	 * @param ctx the parse tree
	 */
	default void exitUnwindClause(Cypher5Parser.UnwindClauseContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#callClause}.
	 * @param ctx the parse tree
	 */
	default void enterCallClause(Cypher5Parser.CallClauseContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#callClause}.
	 * @param ctx the parse tree
	 */
	default void exitCallClause(Cypher5Parser.CallClauseContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#procedureName}.
	 * @param ctx the parse tree
	 */
	default void enterProcedureName(Cypher5Parser.ProcedureNameContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#procedureName}.
	 * @param ctx the parse tree
	 */
	default void exitProcedureName(Cypher5Parser.ProcedureNameContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#procedureArgument}.
	 * @param ctx the parse tree
	 */
	default void enterProcedureArgument(Cypher5Parser.ProcedureArgumentContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#procedureArgument}.
	 * @param ctx the parse tree
	 */
	default void exitProcedureArgument(Cypher5Parser.ProcedureArgumentContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#procedureResultItem}.
	 * @param ctx the parse tree
	 */
	default void enterProcedureResultItem(Cypher5Parser.ProcedureResultItemContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#procedureResultItem}.
	 * @param ctx the parse tree
	 */
	default void exitProcedureResultItem(Cypher5Parser.ProcedureResultItemContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#loadCSVClause}.
	 * @param ctx the parse tree
	 */
	default void enterLoadCSVClause(Cypher5Parser.LoadCSVClauseContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#loadCSVClause}.
	 * @param ctx the parse tree
	 */
	default void exitLoadCSVClause(Cypher5Parser.LoadCSVClauseContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#foreachClause}.
	 * @param ctx the parse tree
	 */
	default void enterForeachClause(Cypher5Parser.ForeachClauseContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#foreachClause}.
	 * @param ctx the parse tree
	 */
	default void exitForeachClause(Cypher5Parser.ForeachClauseContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#subqueryClause}.
	 * @param ctx the parse tree
	 */
	default void enterSubqueryClause(Cypher5Parser.SubqueryClauseContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#subqueryClause}.
	 * @param ctx the parse tree
	 */
	default void exitSubqueryClause(Cypher5Parser.SubqueryClauseContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#subqueryScope}.
	 * @param ctx the parse tree
	 */
	default void enterSubqueryScope(Cypher5Parser.SubqueryScopeContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#subqueryScope}.
	 * @param ctx the parse tree
	 */
	default void exitSubqueryScope(Cypher5Parser.SubqueryScopeContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#subqueryInTransactionsParameters}.
	 * @param ctx the parse tree
	 */
	default void enterSubqueryInTransactionsParameters(Cypher5Parser.SubqueryInTransactionsParametersContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#subqueryInTransactionsParameters}.
	 * @param ctx the parse tree
	 */
	default void exitSubqueryInTransactionsParameters(Cypher5Parser.SubqueryInTransactionsParametersContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#subqueryInTransactionsBatchParameters}.
	 * @param ctx the parse tree
	 */
	default void enterSubqueryInTransactionsBatchParameters(Cypher5Parser.SubqueryInTransactionsBatchParametersContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#subqueryInTransactionsBatchParameters}.
	 * @param ctx the parse tree
	 */
	default void exitSubqueryInTransactionsBatchParameters(Cypher5Parser.SubqueryInTransactionsBatchParametersContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#subqueryInTransactionsErrorParameters}.
	 * @param ctx the parse tree
	 */
	default void enterSubqueryInTransactionsErrorParameters(Cypher5Parser.SubqueryInTransactionsErrorParametersContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#subqueryInTransactionsErrorParameters}.
	 * @param ctx the parse tree
	 */
	default void exitSubqueryInTransactionsErrorParameters(Cypher5Parser.SubqueryInTransactionsErrorParametersContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#subqueryInTransactionsReportParameters}.
	 * @param ctx the parse tree
	 */
	default void enterSubqueryInTransactionsReportParameters(Cypher5Parser.SubqueryInTransactionsReportParametersContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#subqueryInTransactionsReportParameters}.
	 * @param ctx the parse tree
	 */
	default void exitSubqueryInTransactionsReportParameters(Cypher5Parser.SubqueryInTransactionsReportParametersContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#orderBySkipLimitClause}.
	 * @param ctx the parse tree
	 */
	default void enterOrderBySkipLimitClause(Cypher5Parser.OrderBySkipLimitClauseContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#orderBySkipLimitClause}.
	 * @param ctx the parse tree
	 */
	default void exitOrderBySkipLimitClause(Cypher5Parser.OrderBySkipLimitClauseContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#patternList}.
	 * @param ctx the parse tree
	 */
	default void enterPatternList(Cypher5Parser.PatternListContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#patternList}.
	 * @param ctx the parse tree
	 */
	default void exitPatternList(Cypher5Parser.PatternListContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#insertPatternList}.
	 * @param ctx the parse tree
	 */
	default void enterInsertPatternList(Cypher5Parser.InsertPatternListContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#insertPatternList}.
	 * @param ctx the parse tree
	 */
	default void exitInsertPatternList(Cypher5Parser.InsertPatternListContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#pattern}.
	 * @param ctx the parse tree
	 */
	default void enterPattern(Cypher5Parser.PatternContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#pattern}.
	 * @param ctx the parse tree
	 */
	default void exitPattern(Cypher5Parser.PatternContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#insertPattern}.
	 * @param ctx the parse tree
	 */
	default void enterInsertPattern(Cypher5Parser.InsertPatternContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#insertPattern}.
	 * @param ctx the parse tree
	 */
	default void exitInsertPattern(Cypher5Parser.InsertPatternContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#quantifier}.
	 * @param ctx the parse tree
	 */
	default void enterQuantifier(Cypher5Parser.QuantifierContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#quantifier}.
	 * @param ctx the parse tree
	 */
	default void exitQuantifier(Cypher5Parser.QuantifierContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#anonymousPattern}.
	 * @param ctx the parse tree
	 */
	default void enterAnonymousPattern(Cypher5Parser.AnonymousPatternContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#anonymousPattern}.
	 * @param ctx the parse tree
	 */
	default void exitAnonymousPattern(Cypher5Parser.AnonymousPatternContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#shortestPathPattern}.
	 * @param ctx the parse tree
	 */
	default void enterShortestPathPattern(Cypher5Parser.ShortestPathPatternContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#shortestPathPattern}.
	 * @param ctx the parse tree
	 */
	default void exitShortestPathPattern(Cypher5Parser.ShortestPathPatternContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#patternElement}.
	 * @param ctx the parse tree
	 */
	default void enterPatternElement(Cypher5Parser.PatternElementContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#patternElement}.
	 * @param ctx the parse tree
	 */
	default void exitPatternElement(Cypher5Parser.PatternElementContext ctx) { }
	/**
	 * Enter a parse tree produced by the {@code AnyShortestPath}
	 * labeled alternative in {@link Cypher5Parser#selector}.
	 * @param ctx the parse tree
	 */
	default void enterAnyShortestPath(Cypher5Parser.AnyShortestPathContext ctx) { }
	/**
	 * Exit a parse tree produced by the {@code AnyShortestPath}
	 * labeled alternative in {@link Cypher5Parser#selector}.
	 * @param ctx the parse tree
	 */
	default void exitAnyShortestPath(Cypher5Parser.AnyShortestPathContext ctx) { }
	/**
	 * Enter a parse tree produced by the {@code AllShortestPath}
	 * labeled alternative in {@link Cypher5Parser#selector}.
	 * @param ctx the parse tree
	 */
	default void enterAllShortestPath(Cypher5Parser.AllShortestPathContext ctx) { }
	/**
	 * Exit a parse tree produced by the {@code AllShortestPath}
	 * labeled alternative in {@link Cypher5Parser#selector}.
	 * @param ctx the parse tree
	 */
	default void exitAllShortestPath(Cypher5Parser.AllShortestPathContext ctx) { }
	/**
	 * Enter a parse tree produced by the {@code AnyPath}
	 * labeled alternative in {@link Cypher5Parser#selector}.
	 * @param ctx the parse tree
	 */
	default void enterAnyPath(Cypher5Parser.AnyPathContext ctx) { }
	/**
	 * Exit a parse tree produced by the {@code AnyPath}
	 * labeled alternative in {@link Cypher5Parser#selector}.
	 * @param ctx the parse tree
	 */
	default void exitAnyPath(Cypher5Parser.AnyPathContext ctx) { }
	/**
	 * Enter a parse tree produced by the {@code AllPath}
	 * labeled alternative in {@link Cypher5Parser#selector}.
	 * @param ctx the parse tree
	 */
	default void enterAllPath(Cypher5Parser.AllPathContext ctx) { }
	/**
	 * Exit a parse tree produced by the {@code AllPath}
	 * labeled alternative in {@link Cypher5Parser#selector}.
	 * @param ctx the parse tree
	 */
	default void exitAllPath(Cypher5Parser.AllPathContext ctx) { }
	/**
	 * Enter a parse tree produced by the {@code ShortestGroup}
	 * labeled alternative in {@link Cypher5Parser#selector}.
	 * @param ctx the parse tree
	 */
	default void enterShortestGroup(Cypher5Parser.ShortestGroupContext ctx) { }
	/**
	 * Exit a parse tree produced by the {@code ShortestGroup}
	 * labeled alternative in {@link Cypher5Parser#selector}.
	 * @param ctx the parse tree
	 */
	default void exitShortestGroup(Cypher5Parser.ShortestGroupContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#groupToken}.
	 * @param ctx the parse tree
	 */
	default void enterGroupToken(Cypher5Parser.GroupTokenContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#groupToken}.
	 * @param ctx the parse tree
	 */
	default void exitGroupToken(Cypher5Parser.GroupTokenContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#pathToken}.
	 * @param ctx the parse tree
	 */
	default void enterPathToken(Cypher5Parser.PathTokenContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#pathToken}.
	 * @param ctx the parse tree
	 */
	default void exitPathToken(Cypher5Parser.PathTokenContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#pathPatternNonEmpty}.
	 * @param ctx the parse tree
	 */
	default void enterPathPatternNonEmpty(Cypher5Parser.PathPatternNonEmptyContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#pathPatternNonEmpty}.
	 * @param ctx the parse tree
	 */
	default void exitPathPatternNonEmpty(Cypher5Parser.PathPatternNonEmptyContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#nodePattern}.
	 * @param ctx the parse tree
	 */
	default void enterNodePattern(Cypher5Parser.NodePatternContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#nodePattern}.
	 * @param ctx the parse tree
	 */
	default void exitNodePattern(Cypher5Parser.NodePatternContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#insertNodePattern}.
	 * @param ctx the parse tree
	 */
	default void enterInsertNodePattern(Cypher5Parser.InsertNodePatternContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#insertNodePattern}.
	 * @param ctx the parse tree
	 */
	default void exitInsertNodePattern(Cypher5Parser.InsertNodePatternContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#parenthesizedPath}.
	 * @param ctx the parse tree
	 */
	default void enterParenthesizedPath(Cypher5Parser.ParenthesizedPathContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#parenthesizedPath}.
	 * @param ctx the parse tree
	 */
	default void exitParenthesizedPath(Cypher5Parser.ParenthesizedPathContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#nodeLabels}.
	 * @param ctx the parse tree
	 */
	default void enterNodeLabels(Cypher5Parser.NodeLabelsContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#nodeLabels}.
	 * @param ctx the parse tree
	 */
	default void exitNodeLabels(Cypher5Parser.NodeLabelsContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#nodeLabelsIs}.
	 * @param ctx the parse tree
	 */
	default void enterNodeLabelsIs(Cypher5Parser.NodeLabelsIsContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#nodeLabelsIs}.
	 * @param ctx the parse tree
	 */
	default void exitNodeLabelsIs(Cypher5Parser.NodeLabelsIsContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#dynamicExpression}.
	 * @param ctx the parse tree
	 */
	default void enterDynamicExpression(Cypher5Parser.DynamicExpressionContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#dynamicExpression}.
	 * @param ctx the parse tree
	 */
	default void exitDynamicExpression(Cypher5Parser.DynamicExpressionContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#dynamicAnyAllExpression}.
	 * @param ctx the parse tree
	 */
	default void enterDynamicAnyAllExpression(Cypher5Parser.DynamicAnyAllExpressionContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#dynamicAnyAllExpression}.
	 * @param ctx the parse tree
	 */
	default void exitDynamicAnyAllExpression(Cypher5Parser.DynamicAnyAllExpressionContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#dynamicLabelType}.
	 * @param ctx the parse tree
	 */
	default void enterDynamicLabelType(Cypher5Parser.DynamicLabelTypeContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#dynamicLabelType}.
	 * @param ctx the parse tree
	 */
	default void exitDynamicLabelType(Cypher5Parser.DynamicLabelTypeContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#labelType}.
	 * @param ctx the parse tree
	 */
	default void enterLabelType(Cypher5Parser.LabelTypeContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#labelType}.
	 * @param ctx the parse tree
	 */
	default void exitLabelType(Cypher5Parser.LabelTypeContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#relType}.
	 * @param ctx the parse tree
	 */
	default void enterRelType(Cypher5Parser.RelTypeContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#relType}.
	 * @param ctx the parse tree
	 */
	default void exitRelType(Cypher5Parser.RelTypeContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#labelOrRelType}.
	 * @param ctx the parse tree
	 */
	default void enterLabelOrRelType(Cypher5Parser.LabelOrRelTypeContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#labelOrRelType}.
	 * @param ctx the parse tree
	 */
	default void exitLabelOrRelType(Cypher5Parser.LabelOrRelTypeContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#properties}.
	 * @param ctx the parse tree
	 */
	default void enterProperties(Cypher5Parser.PropertiesContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#properties}.
	 * @param ctx the parse tree
	 */
	default void exitProperties(Cypher5Parser.PropertiesContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#relationshipPattern}.
	 * @param ctx the parse tree
	 */
	default void enterRelationshipPattern(Cypher5Parser.RelationshipPatternContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#relationshipPattern}.
	 * @param ctx the parse tree
	 */
	default void exitRelationshipPattern(Cypher5Parser.RelationshipPatternContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#insertRelationshipPattern}.
	 * @param ctx the parse tree
	 */
	default void enterInsertRelationshipPattern(Cypher5Parser.InsertRelationshipPatternContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#insertRelationshipPattern}.
	 * @param ctx the parse tree
	 */
	default void exitInsertRelationshipPattern(Cypher5Parser.InsertRelationshipPatternContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#leftArrow}.
	 * @param ctx the parse tree
	 */
	default void enterLeftArrow(Cypher5Parser.LeftArrowContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#leftArrow}.
	 * @param ctx the parse tree
	 */
	default void exitLeftArrow(Cypher5Parser.LeftArrowContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#arrowLine}.
	 * @param ctx the parse tree
	 */
	default void enterArrowLine(Cypher5Parser.ArrowLineContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#arrowLine}.
	 * @param ctx the parse tree
	 */
	default void exitArrowLine(Cypher5Parser.ArrowLineContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#rightArrow}.
	 * @param ctx the parse tree
	 */
	default void enterRightArrow(Cypher5Parser.RightArrowContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#rightArrow}.
	 * @param ctx the parse tree
	 */
	default void exitRightArrow(Cypher5Parser.RightArrowContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#pathLength}.
	 * @param ctx the parse tree
	 */
	default void enterPathLength(Cypher5Parser.PathLengthContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#pathLength}.
	 * @param ctx the parse tree
	 */
	default void exitPathLength(Cypher5Parser.PathLengthContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#labelExpression}.
	 * @param ctx the parse tree
	 */
	default void enterLabelExpression(Cypher5Parser.LabelExpressionContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#labelExpression}.
	 * @param ctx the parse tree
	 */
	default void exitLabelExpression(Cypher5Parser.LabelExpressionContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#labelExpression4}.
	 * @param ctx the parse tree
	 */
	default void enterLabelExpression4(Cypher5Parser.LabelExpression4Context ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#labelExpression4}.
	 * @param ctx the parse tree
	 */
	default void exitLabelExpression4(Cypher5Parser.LabelExpression4Context ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#labelExpression4Is}.
	 * @param ctx the parse tree
	 */
	default void enterLabelExpression4Is(Cypher5Parser.LabelExpression4IsContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#labelExpression4Is}.
	 * @param ctx the parse tree
	 */
	default void exitLabelExpression4Is(Cypher5Parser.LabelExpression4IsContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#labelExpression3}.
	 * @param ctx the parse tree
	 */
	default void enterLabelExpression3(Cypher5Parser.LabelExpression3Context ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#labelExpression3}.
	 * @param ctx the parse tree
	 */
	default void exitLabelExpression3(Cypher5Parser.LabelExpression3Context ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#labelExpression3Is}.
	 * @param ctx the parse tree
	 */
	default void enterLabelExpression3Is(Cypher5Parser.LabelExpression3IsContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#labelExpression3Is}.
	 * @param ctx the parse tree
	 */
	default void exitLabelExpression3Is(Cypher5Parser.LabelExpression3IsContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#labelExpression2}.
	 * @param ctx the parse tree
	 */
	default void enterLabelExpression2(Cypher5Parser.LabelExpression2Context ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#labelExpression2}.
	 * @param ctx the parse tree
	 */
	default void exitLabelExpression2(Cypher5Parser.LabelExpression2Context ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#labelExpression2Is}.
	 * @param ctx the parse tree
	 */
	default void enterLabelExpression2Is(Cypher5Parser.LabelExpression2IsContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#labelExpression2Is}.
	 * @param ctx the parse tree
	 */
	default void exitLabelExpression2Is(Cypher5Parser.LabelExpression2IsContext ctx) { }
	/**
	 * Enter a parse tree produced by the {@code ParenthesizedLabelExpression}
	 * labeled alternative in {@link Cypher5Parser#labelExpression1}.
	 * @param ctx the parse tree
	 */
	default void enterParenthesizedLabelExpression(Cypher5Parser.ParenthesizedLabelExpressionContext ctx) { }
	/**
	 * Exit a parse tree produced by the {@code ParenthesizedLabelExpression}
	 * labeled alternative in {@link Cypher5Parser#labelExpression1}.
	 * @param ctx the parse tree
	 */
	default void exitParenthesizedLabelExpression(Cypher5Parser.ParenthesizedLabelExpressionContext ctx) { }
	/**
	 * Enter a parse tree produced by the {@code AnyLabel}
	 * labeled alternative in {@link Cypher5Parser#labelExpression1}.
	 * @param ctx the parse tree
	 */
	default void enterAnyLabel(Cypher5Parser.AnyLabelContext ctx) { }
	/**
	 * Exit a parse tree produced by the {@code AnyLabel}
	 * labeled alternative in {@link Cypher5Parser#labelExpression1}.
	 * @param ctx the parse tree
	 */
	default void exitAnyLabel(Cypher5Parser.AnyLabelContext ctx) { }
	/**
	 * Enter a parse tree produced by the {@code DynamicLabel}
	 * labeled alternative in {@link Cypher5Parser#labelExpression1}.
	 * @param ctx the parse tree
	 */
	default void enterDynamicLabel(Cypher5Parser.DynamicLabelContext ctx) { }
	/**
	 * Exit a parse tree produced by the {@code DynamicLabel}
	 * labeled alternative in {@link Cypher5Parser#labelExpression1}.
	 * @param ctx the parse tree
	 */
	default void exitDynamicLabel(Cypher5Parser.DynamicLabelContext ctx) { }
	/**
	 * Enter a parse tree produced by the {@code LabelName}
	 * labeled alternative in {@link Cypher5Parser#labelExpression1}.
	 * @param ctx the parse tree
	 */
	default void enterLabelName(Cypher5Parser.LabelNameContext ctx) { }
	/**
	 * Exit a parse tree produced by the {@code LabelName}
	 * labeled alternative in {@link Cypher5Parser#labelExpression1}.
	 * @param ctx the parse tree
	 */
	default void exitLabelName(Cypher5Parser.LabelNameContext ctx) { }
	/**
	 * Enter a parse tree produced by the {@code ParenthesizedLabelExpressionIs}
	 * labeled alternative in {@link Cypher5Parser#labelExpression1Is}.
	 * @param ctx the parse tree
	 */
	default void enterParenthesizedLabelExpressionIs(Cypher5Parser.ParenthesizedLabelExpressionIsContext ctx) { }
	/**
	 * Exit a parse tree produced by the {@code ParenthesizedLabelExpressionIs}
	 * labeled alternative in {@link Cypher5Parser#labelExpression1Is}.
	 * @param ctx the parse tree
	 */
	default void exitParenthesizedLabelExpressionIs(Cypher5Parser.ParenthesizedLabelExpressionIsContext ctx) { }
	/**
	 * Enter a parse tree produced by the {@code AnyLabelIs}
	 * labeled alternative in {@link Cypher5Parser#labelExpression1Is}.
	 * @param ctx the parse tree
	 */
	default void enterAnyLabelIs(Cypher5Parser.AnyLabelIsContext ctx) { }
	/**
	 * Exit a parse tree produced by the {@code AnyLabelIs}
	 * labeled alternative in {@link Cypher5Parser#labelExpression1Is}.
	 * @param ctx the parse tree
	 */
	default void exitAnyLabelIs(Cypher5Parser.AnyLabelIsContext ctx) { }
	/**
	 * Enter a parse tree produced by the {@code DynamicLabelIs}
	 * labeled alternative in {@link Cypher5Parser#labelExpression1Is}.
	 * @param ctx the parse tree
	 */
	default void enterDynamicLabelIs(Cypher5Parser.DynamicLabelIsContext ctx) { }
	/**
	 * Exit a parse tree produced by the {@code DynamicLabelIs}
	 * labeled alternative in {@link Cypher5Parser#labelExpression1Is}.
	 * @param ctx the parse tree
	 */
	default void exitDynamicLabelIs(Cypher5Parser.DynamicLabelIsContext ctx) { }
	/**
	 * Enter a parse tree produced by the {@code LabelNameIs}
	 * labeled alternative in {@link Cypher5Parser#labelExpression1Is}.
	 * @param ctx the parse tree
	 */
	default void enterLabelNameIs(Cypher5Parser.LabelNameIsContext ctx) { }
	/**
	 * Exit a parse tree produced by the {@code LabelNameIs}
	 * labeled alternative in {@link Cypher5Parser#labelExpression1Is}.
	 * @param ctx the parse tree
	 */
	default void exitLabelNameIs(Cypher5Parser.LabelNameIsContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#insertNodeLabelExpression}.
	 * @param ctx the parse tree
	 */
	default void enterInsertNodeLabelExpression(Cypher5Parser.InsertNodeLabelExpressionContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#insertNodeLabelExpression}.
	 * @param ctx the parse tree
	 */
	default void exitInsertNodeLabelExpression(Cypher5Parser.InsertNodeLabelExpressionContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#insertRelationshipLabelExpression}.
	 * @param ctx the parse tree
	 */
	default void enterInsertRelationshipLabelExpression(Cypher5Parser.InsertRelationshipLabelExpressionContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#insertRelationshipLabelExpression}.
	 * @param ctx the parse tree
	 */
	default void exitInsertRelationshipLabelExpression(Cypher5Parser.InsertRelationshipLabelExpressionContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#expression}.
	 * @param ctx the parse tree
	 */
	default void enterExpression(Cypher5Parser.ExpressionContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#expression}.
	 * @param ctx the parse tree
	 */
	default void exitExpression(Cypher5Parser.ExpressionContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#expression11}.
	 * @param ctx the parse tree
	 */
	default void enterExpression11(Cypher5Parser.Expression11Context ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#expression11}.
	 * @param ctx the parse tree
	 */
	default void exitExpression11(Cypher5Parser.Expression11Context ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#expression10}.
	 * @param ctx the parse tree
	 */
	default void enterExpression10(Cypher5Parser.Expression10Context ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#expression10}.
	 * @param ctx the parse tree
	 */
	default void exitExpression10(Cypher5Parser.Expression10Context ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#expression9}.
	 * @param ctx the parse tree
	 */
	default void enterExpression9(Cypher5Parser.Expression9Context ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#expression9}.
	 * @param ctx the parse tree
	 */
	default void exitExpression9(Cypher5Parser.Expression9Context ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#expression8}.
	 * @param ctx the parse tree
	 */
	default void enterExpression8(Cypher5Parser.Expression8Context ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#expression8}.
	 * @param ctx the parse tree
	 */
	default void exitExpression8(Cypher5Parser.Expression8Context ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#expression7}.
	 * @param ctx the parse tree
	 */
	default void enterExpression7(Cypher5Parser.Expression7Context ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#expression7}.
	 * @param ctx the parse tree
	 */
	default void exitExpression7(Cypher5Parser.Expression7Context ctx) { }
	/**
	 * Enter a parse tree produced by the {@code StringAndListComparison}
	 * labeled alternative in {@link Cypher5Parser#comparisonExpression6}.
	 * @param ctx the parse tree
	 */
	default void enterStringAndListComparison(Cypher5Parser.StringAndListComparisonContext ctx) { }
	/**
	 * Exit a parse tree produced by the {@code StringAndListComparison}
	 * labeled alternative in {@link Cypher5Parser#comparisonExpression6}.
	 * @param ctx the parse tree
	 */
	default void exitStringAndListComparison(Cypher5Parser.StringAndListComparisonContext ctx) { }
	/**
	 * Enter a parse tree produced by the {@code NullComparison}
	 * labeled alternative in {@link Cypher5Parser#comparisonExpression6}.
	 * @param ctx the parse tree
	 */
	default void enterNullComparison(Cypher5Parser.NullComparisonContext ctx) { }
	/**
	 * Exit a parse tree produced by the {@code NullComparison}
	 * labeled alternative in {@link Cypher5Parser#comparisonExpression6}.
	 * @param ctx the parse tree
	 */
	default void exitNullComparison(Cypher5Parser.NullComparisonContext ctx) { }
	/**
	 * Enter a parse tree produced by the {@code TypeComparison}
	 * labeled alternative in {@link Cypher5Parser#comparisonExpression6}.
	 * @param ctx the parse tree
	 */
	default void enterTypeComparison(Cypher5Parser.TypeComparisonContext ctx) { }
	/**
	 * Exit a parse tree produced by the {@code TypeComparison}
	 * labeled alternative in {@link Cypher5Parser#comparisonExpression6}.
	 * @param ctx the parse tree
	 */
	default void exitTypeComparison(Cypher5Parser.TypeComparisonContext ctx) { }
	/**
	 * Enter a parse tree produced by the {@code NormalFormComparison}
	 * labeled alternative in {@link Cypher5Parser#comparisonExpression6}.
	 * @param ctx the parse tree
	 */
	default void enterNormalFormComparison(Cypher5Parser.NormalFormComparisonContext ctx) { }
	/**
	 * Exit a parse tree produced by the {@code NormalFormComparison}
	 * labeled alternative in {@link Cypher5Parser#comparisonExpression6}.
	 * @param ctx the parse tree
	 */
	default void exitNormalFormComparison(Cypher5Parser.NormalFormComparisonContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#normalForm}.
	 * @param ctx the parse tree
	 */
	default void enterNormalForm(Cypher5Parser.NormalFormContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#normalForm}.
	 * @param ctx the parse tree
	 */
	default void exitNormalForm(Cypher5Parser.NormalFormContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#expression6}.
	 * @param ctx the parse tree
	 */
	default void enterExpression6(Cypher5Parser.Expression6Context ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#expression6}.
	 * @param ctx the parse tree
	 */
	default void exitExpression6(Cypher5Parser.Expression6Context ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#expression5}.
	 * @param ctx the parse tree
	 */
	default void enterExpression5(Cypher5Parser.Expression5Context ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#expression5}.
	 * @param ctx the parse tree
	 */
	default void exitExpression5(Cypher5Parser.Expression5Context ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#expression4}.
	 * @param ctx the parse tree
	 */
	default void enterExpression4(Cypher5Parser.Expression4Context ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#expression4}.
	 * @param ctx the parse tree
	 */
	default void exitExpression4(Cypher5Parser.Expression4Context ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#expression3}.
	 * @param ctx the parse tree
	 */
	default void enterExpression3(Cypher5Parser.Expression3Context ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#expression3}.
	 * @param ctx the parse tree
	 */
	default void exitExpression3(Cypher5Parser.Expression3Context ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#expression2}.
	 * @param ctx the parse tree
	 */
	default void enterExpression2(Cypher5Parser.Expression2Context ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#expression2}.
	 * @param ctx the parse tree
	 */
	default void exitExpression2(Cypher5Parser.Expression2Context ctx) { }
	/**
	 * Enter a parse tree produced by the {@code PropertyPostfix}
	 * labeled alternative in {@link Cypher5Parser#postFix}.
	 * @param ctx the parse tree
	 */
	default void enterPropertyPostfix(Cypher5Parser.PropertyPostfixContext ctx) { }
	/**
	 * Exit a parse tree produced by the {@code PropertyPostfix}
	 * labeled alternative in {@link Cypher5Parser#postFix}.
	 * @param ctx the parse tree
	 */
	default void exitPropertyPostfix(Cypher5Parser.PropertyPostfixContext ctx) { }
	/**
	 * Enter a parse tree produced by the {@code LabelPostfix}
	 * labeled alternative in {@link Cypher5Parser#postFix}.
	 * @param ctx the parse tree
	 */
	default void enterLabelPostfix(Cypher5Parser.LabelPostfixContext ctx) { }
	/**
	 * Exit a parse tree produced by the {@code LabelPostfix}
	 * labeled alternative in {@link Cypher5Parser#postFix}.
	 * @param ctx the parse tree
	 */
	default void exitLabelPostfix(Cypher5Parser.LabelPostfixContext ctx) { }
	/**
	 * Enter a parse tree produced by the {@code IndexPostfix}
	 * labeled alternative in {@link Cypher5Parser#postFix}.
	 * @param ctx the parse tree
	 */
	default void enterIndexPostfix(Cypher5Parser.IndexPostfixContext ctx) { }
	/**
	 * Exit a parse tree produced by the {@code IndexPostfix}
	 * labeled alternative in {@link Cypher5Parser#postFix}.
	 * @param ctx the parse tree
	 */
	default void exitIndexPostfix(Cypher5Parser.IndexPostfixContext ctx) { }
	/**
	 * Enter a parse tree produced by the {@code RangePostfix}
	 * labeled alternative in {@link Cypher5Parser#postFix}.
	 * @param ctx the parse tree
	 */
	default void enterRangePostfix(Cypher5Parser.RangePostfixContext ctx) { }
	/**
	 * Exit a parse tree produced by the {@code RangePostfix}
	 * labeled alternative in {@link Cypher5Parser#postFix}.
	 * @param ctx the parse tree
	 */
	default void exitRangePostfix(Cypher5Parser.RangePostfixContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#property}.
	 * @param ctx the parse tree
	 */
	default void enterProperty(Cypher5Parser.PropertyContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#property}.
	 * @param ctx the parse tree
	 */
	default void exitProperty(Cypher5Parser.PropertyContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#dynamicProperty}.
	 * @param ctx the parse tree
	 */
	default void enterDynamicProperty(Cypher5Parser.DynamicPropertyContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#dynamicProperty}.
	 * @param ctx the parse tree
	 */
	default void exitDynamicProperty(Cypher5Parser.DynamicPropertyContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#propertyExpression}.
	 * @param ctx the parse tree
	 */
	default void enterPropertyExpression(Cypher5Parser.PropertyExpressionContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#propertyExpression}.
	 * @param ctx the parse tree
	 */
	default void exitPropertyExpression(Cypher5Parser.PropertyExpressionContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#dynamicPropertyExpression}.
	 * @param ctx the parse tree
	 */
	default void enterDynamicPropertyExpression(Cypher5Parser.DynamicPropertyExpressionContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#dynamicPropertyExpression}.
	 * @param ctx the parse tree
	 */
	default void exitDynamicPropertyExpression(Cypher5Parser.DynamicPropertyExpressionContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#expression1}.
	 * @param ctx the parse tree
	 */
	default void enterExpression1(Cypher5Parser.Expression1Context ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#expression1}.
	 * @param ctx the parse tree
	 */
	default void exitExpression1(Cypher5Parser.Expression1Context ctx) { }
	/**
	 * Enter a parse tree produced by the {@code NummericLiteral}
	 * labeled alternative in {@link Cypher5Parser#literal}.
	 * @param ctx the parse tree
	 */
	default void enterNummericLiteral(Cypher5Parser.NummericLiteralContext ctx) { }
	/**
	 * Exit a parse tree produced by the {@code NummericLiteral}
	 * labeled alternative in {@link Cypher5Parser#literal}.
	 * @param ctx the parse tree
	 */
	default void exitNummericLiteral(Cypher5Parser.NummericLiteralContext ctx) { }
	/**
	 * Enter a parse tree produced by the {@code StringsLiteral}
	 * labeled alternative in {@link Cypher5Parser#literal}.
	 * @param ctx the parse tree
	 */
	default void enterStringsLiteral(Cypher5Parser.StringsLiteralContext ctx) { }
	/**
	 * Exit a parse tree produced by the {@code StringsLiteral}
	 * labeled alternative in {@link Cypher5Parser#literal}.
	 * @param ctx the parse tree
	 */
	default void exitStringsLiteral(Cypher5Parser.StringsLiteralContext ctx) { }
	/**
	 * Enter a parse tree produced by the {@code OtherLiteral}
	 * labeled alternative in {@link Cypher5Parser#literal}.
	 * @param ctx the parse tree
	 */
	default void enterOtherLiteral(Cypher5Parser.OtherLiteralContext ctx) { }
	/**
	 * Exit a parse tree produced by the {@code OtherLiteral}
	 * labeled alternative in {@link Cypher5Parser#literal}.
	 * @param ctx the parse tree
	 */
	default void exitOtherLiteral(Cypher5Parser.OtherLiteralContext ctx) { }
	/**
	 * Enter a parse tree produced by the {@code BooleanLiteral}
	 * labeled alternative in {@link Cypher5Parser#literal}.
	 * @param ctx the parse tree
	 */
	default void enterBooleanLiteral(Cypher5Parser.BooleanLiteralContext ctx) { }
	/**
	 * Exit a parse tree produced by the {@code BooleanLiteral}
	 * labeled alternative in {@link Cypher5Parser#literal}.
	 * @param ctx the parse tree
	 */
	default void exitBooleanLiteral(Cypher5Parser.BooleanLiteralContext ctx) { }
	/**
	 * Enter a parse tree produced by the {@code KeywordLiteral}
	 * labeled alternative in {@link Cypher5Parser#literal}.
	 * @param ctx the parse tree
	 */
	default void enterKeywordLiteral(Cypher5Parser.KeywordLiteralContext ctx) { }
	/**
	 * Exit a parse tree produced by the {@code KeywordLiteral}
	 * labeled alternative in {@link Cypher5Parser#literal}.
	 * @param ctx the parse tree
	 */
	default void exitKeywordLiteral(Cypher5Parser.KeywordLiteralContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#caseExpression}.
	 * @param ctx the parse tree
	 */
	default void enterCaseExpression(Cypher5Parser.CaseExpressionContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#caseExpression}.
	 * @param ctx the parse tree
	 */
	default void exitCaseExpression(Cypher5Parser.CaseExpressionContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#caseAlternative}.
	 * @param ctx the parse tree
	 */
	default void enterCaseAlternative(Cypher5Parser.CaseAlternativeContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#caseAlternative}.
	 * @param ctx the parse tree
	 */
	default void exitCaseAlternative(Cypher5Parser.CaseAlternativeContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#extendedCaseExpression}.
	 * @param ctx the parse tree
	 */
	default void enterExtendedCaseExpression(Cypher5Parser.ExtendedCaseExpressionContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#extendedCaseExpression}.
	 * @param ctx the parse tree
	 */
	default void exitExtendedCaseExpression(Cypher5Parser.ExtendedCaseExpressionContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#extendedCaseAlternative}.
	 * @param ctx the parse tree
	 */
	default void enterExtendedCaseAlternative(Cypher5Parser.ExtendedCaseAlternativeContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#extendedCaseAlternative}.
	 * @param ctx the parse tree
	 */
	default void exitExtendedCaseAlternative(Cypher5Parser.ExtendedCaseAlternativeContext ctx) { }
	/**
	 * Enter a parse tree produced by the {@code WhenStringOrList}
	 * labeled alternative in {@link Cypher5Parser#extendedWhen}.
	 * @param ctx the parse tree
	 */
	default void enterWhenStringOrList(Cypher5Parser.WhenStringOrListContext ctx) { }
	/**
	 * Exit a parse tree produced by the {@code WhenStringOrList}
	 * labeled alternative in {@link Cypher5Parser#extendedWhen}.
	 * @param ctx the parse tree
	 */
	default void exitWhenStringOrList(Cypher5Parser.WhenStringOrListContext ctx) { }
	/**
	 * Enter a parse tree produced by the {@code WhenNull}
	 * labeled alternative in {@link Cypher5Parser#extendedWhen}.
	 * @param ctx the parse tree
	 */
	default void enterWhenNull(Cypher5Parser.WhenNullContext ctx) { }
	/**
	 * Exit a parse tree produced by the {@code WhenNull}
	 * labeled alternative in {@link Cypher5Parser#extendedWhen}.
	 * @param ctx the parse tree
	 */
	default void exitWhenNull(Cypher5Parser.WhenNullContext ctx) { }
	/**
	 * Enter a parse tree produced by the {@code WhenType}
	 * labeled alternative in {@link Cypher5Parser#extendedWhen}.
	 * @param ctx the parse tree
	 */
	default void enterWhenType(Cypher5Parser.WhenTypeContext ctx) { }
	/**
	 * Exit a parse tree produced by the {@code WhenType}
	 * labeled alternative in {@link Cypher5Parser#extendedWhen}.
	 * @param ctx the parse tree
	 */
	default void exitWhenType(Cypher5Parser.WhenTypeContext ctx) { }
	/**
	 * Enter a parse tree produced by the {@code WhenForm}
	 * labeled alternative in {@link Cypher5Parser#extendedWhen}.
	 * @param ctx the parse tree
	 */
	default void enterWhenForm(Cypher5Parser.WhenFormContext ctx) { }
	/**
	 * Exit a parse tree produced by the {@code WhenForm}
	 * labeled alternative in {@link Cypher5Parser#extendedWhen}.
	 * @param ctx the parse tree
	 */
	default void exitWhenForm(Cypher5Parser.WhenFormContext ctx) { }
	/**
	 * Enter a parse tree produced by the {@code WhenComparator}
	 * labeled alternative in {@link Cypher5Parser#extendedWhen}.
	 * @param ctx the parse tree
	 */
	default void enterWhenComparator(Cypher5Parser.WhenComparatorContext ctx) { }
	/**
	 * Exit a parse tree produced by the {@code WhenComparator}
	 * labeled alternative in {@link Cypher5Parser#extendedWhen}.
	 * @param ctx the parse tree
	 */
	default void exitWhenComparator(Cypher5Parser.WhenComparatorContext ctx) { }
	/**
	 * Enter a parse tree produced by the {@code WhenEquals}
	 * labeled alternative in {@link Cypher5Parser#extendedWhen}.
	 * @param ctx the parse tree
	 */
	default void enterWhenEquals(Cypher5Parser.WhenEqualsContext ctx) { }
	/**
	 * Exit a parse tree produced by the {@code WhenEquals}
	 * labeled alternative in {@link Cypher5Parser#extendedWhen}.
	 * @param ctx the parse tree
	 */
	default void exitWhenEquals(Cypher5Parser.WhenEqualsContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#listComprehension}.
	 * @param ctx the parse tree
	 */
	default void enterListComprehension(Cypher5Parser.ListComprehensionContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#listComprehension}.
	 * @param ctx the parse tree
	 */
	default void exitListComprehension(Cypher5Parser.ListComprehensionContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#patternComprehension}.
	 * @param ctx the parse tree
	 */
	default void enterPatternComprehension(Cypher5Parser.PatternComprehensionContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#patternComprehension}.
	 * @param ctx the parse tree
	 */
	default void exitPatternComprehension(Cypher5Parser.PatternComprehensionContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#reduceExpression}.
	 * @param ctx the parse tree
	 */
	default void enterReduceExpression(Cypher5Parser.ReduceExpressionContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#reduceExpression}.
	 * @param ctx the parse tree
	 */
	default void exitReduceExpression(Cypher5Parser.ReduceExpressionContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#listItemsPredicate}.
	 * @param ctx the parse tree
	 */
	default void enterListItemsPredicate(Cypher5Parser.ListItemsPredicateContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#listItemsPredicate}.
	 * @param ctx the parse tree
	 */
	default void exitListItemsPredicate(Cypher5Parser.ListItemsPredicateContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#normalizeFunction}.
	 * @param ctx the parse tree
	 */
	default void enterNormalizeFunction(Cypher5Parser.NormalizeFunctionContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#normalizeFunction}.
	 * @param ctx the parse tree
	 */
	default void exitNormalizeFunction(Cypher5Parser.NormalizeFunctionContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#trimFunction}.
	 * @param ctx the parse tree
	 */
	default void enterTrimFunction(Cypher5Parser.TrimFunctionContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#trimFunction}.
	 * @param ctx the parse tree
	 */
	default void exitTrimFunction(Cypher5Parser.TrimFunctionContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#patternExpression}.
	 * @param ctx the parse tree
	 */
	default void enterPatternExpression(Cypher5Parser.PatternExpressionContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#patternExpression}.
	 * @param ctx the parse tree
	 */
	default void exitPatternExpression(Cypher5Parser.PatternExpressionContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#shortestPathExpression}.
	 * @param ctx the parse tree
	 */
	default void enterShortestPathExpression(Cypher5Parser.ShortestPathExpressionContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#shortestPathExpression}.
	 * @param ctx the parse tree
	 */
	default void exitShortestPathExpression(Cypher5Parser.ShortestPathExpressionContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#parenthesizedExpression}.
	 * @param ctx the parse tree
	 */
	default void enterParenthesizedExpression(Cypher5Parser.ParenthesizedExpressionContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#parenthesizedExpression}.
	 * @param ctx the parse tree
	 */
	default void exitParenthesizedExpression(Cypher5Parser.ParenthesizedExpressionContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#mapProjection}.
	 * @param ctx the parse tree
	 */
	default void enterMapProjection(Cypher5Parser.MapProjectionContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#mapProjection}.
	 * @param ctx the parse tree
	 */
	default void exitMapProjection(Cypher5Parser.MapProjectionContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#mapProjectionElement}.
	 * @param ctx the parse tree
	 */
	default void enterMapProjectionElement(Cypher5Parser.MapProjectionElementContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#mapProjectionElement}.
	 * @param ctx the parse tree
	 */
	default void exitMapProjectionElement(Cypher5Parser.MapProjectionElementContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#countStar}.
	 * @param ctx the parse tree
	 */
	default void enterCountStar(Cypher5Parser.CountStarContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#countStar}.
	 * @param ctx the parse tree
	 */
	default void exitCountStar(Cypher5Parser.CountStarContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#existsExpression}.
	 * @param ctx the parse tree
	 */
	default void enterExistsExpression(Cypher5Parser.ExistsExpressionContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#existsExpression}.
	 * @param ctx the parse tree
	 */
	default void exitExistsExpression(Cypher5Parser.ExistsExpressionContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#countExpression}.
	 * @param ctx the parse tree
	 */
	default void enterCountExpression(Cypher5Parser.CountExpressionContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#countExpression}.
	 * @param ctx the parse tree
	 */
	default void exitCountExpression(Cypher5Parser.CountExpressionContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#collectExpression}.
	 * @param ctx the parse tree
	 */
	default void enterCollectExpression(Cypher5Parser.CollectExpressionContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#collectExpression}.
	 * @param ctx the parse tree
	 */
	default void exitCollectExpression(Cypher5Parser.CollectExpressionContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#numberLiteral}.
	 * @param ctx the parse tree
	 */
	default void enterNumberLiteral(Cypher5Parser.NumberLiteralContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#numberLiteral}.
	 * @param ctx the parse tree
	 */
	default void exitNumberLiteral(Cypher5Parser.NumberLiteralContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#signedIntegerLiteral}.
	 * @param ctx the parse tree
	 */
	default void enterSignedIntegerLiteral(Cypher5Parser.SignedIntegerLiteralContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#signedIntegerLiteral}.
	 * @param ctx the parse tree
	 */
	default void exitSignedIntegerLiteral(Cypher5Parser.SignedIntegerLiteralContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#listLiteral}.
	 * @param ctx the parse tree
	 */
	default void enterListLiteral(Cypher5Parser.ListLiteralContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#listLiteral}.
	 * @param ctx the parse tree
	 */
	default void exitListLiteral(Cypher5Parser.ListLiteralContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#propertyKeyName}.
	 * @param ctx the parse tree
	 */
	default void enterPropertyKeyName(Cypher5Parser.PropertyKeyNameContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#propertyKeyName}.
	 * @param ctx the parse tree
	 */
	default void exitPropertyKeyName(Cypher5Parser.PropertyKeyNameContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#parameter}.
	 * @param ctx the parse tree
	 */
	default void enterParameter(Cypher5Parser.ParameterContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#parameter}.
	 * @param ctx the parse tree
	 */
	default void exitParameter(Cypher5Parser.ParameterContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#parameterName}.
	 * @param ctx the parse tree
	 */
	default void enterParameterName(Cypher5Parser.ParameterNameContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#parameterName}.
	 * @param ctx the parse tree
	 */
	default void exitParameterName(Cypher5Parser.ParameterNameContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#functionInvocation}.
	 * @param ctx the parse tree
	 */
	default void enterFunctionInvocation(Cypher5Parser.FunctionInvocationContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#functionInvocation}.
	 * @param ctx the parse tree
	 */
	default void exitFunctionInvocation(Cypher5Parser.FunctionInvocationContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#functionArgument}.
	 * @param ctx the parse tree
	 */
	default void enterFunctionArgument(Cypher5Parser.FunctionArgumentContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#functionArgument}.
	 * @param ctx the parse tree
	 */
	default void exitFunctionArgument(Cypher5Parser.FunctionArgumentContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#functionName}.
	 * @param ctx the parse tree
	 */
	default void enterFunctionName(Cypher5Parser.FunctionNameContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#functionName}.
	 * @param ctx the parse tree
	 */
	default void exitFunctionName(Cypher5Parser.FunctionNameContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#namespace}.
	 * @param ctx the parse tree
	 */
	default void enterNamespace(Cypher5Parser.NamespaceContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#namespace}.
	 * @param ctx the parse tree
	 */
	default void exitNamespace(Cypher5Parser.NamespaceContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#variable}.
	 * @param ctx the parse tree
	 */
	default void enterVariable(Cypher5Parser.VariableContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#variable}.
	 * @param ctx the parse tree
	 */
	default void exitVariable(Cypher5Parser.VariableContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#nonEmptyNameList}.
	 * @param ctx the parse tree
	 */
	default void enterNonEmptyNameList(Cypher5Parser.NonEmptyNameListContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#nonEmptyNameList}.
	 * @param ctx the parse tree
	 */
	default void exitNonEmptyNameList(Cypher5Parser.NonEmptyNameListContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#type}.
	 * @param ctx the parse tree
	 */
	default void enterType(Cypher5Parser.TypeContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#type}.
	 * @param ctx the parse tree
	 */
	default void exitType(Cypher5Parser.TypeContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#typePart}.
	 * @param ctx the parse tree
	 */
	default void enterTypePart(Cypher5Parser.TypePartContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#typePart}.
	 * @param ctx the parse tree
	 */
	default void exitTypePart(Cypher5Parser.TypePartContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#typeName}.
	 * @param ctx the parse tree
	 */
	default void enterTypeName(Cypher5Parser.TypeNameContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#typeName}.
	 * @param ctx the parse tree
	 */
	default void exitTypeName(Cypher5Parser.TypeNameContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#typeNullability}.
	 * @param ctx the parse tree
	 */
	default void enterTypeNullability(Cypher5Parser.TypeNullabilityContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#typeNullability}.
	 * @param ctx the parse tree
	 */
	default void exitTypeNullability(Cypher5Parser.TypeNullabilityContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#typeListSuffix}.
	 * @param ctx the parse tree
	 */
	default void enterTypeListSuffix(Cypher5Parser.TypeListSuffixContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#typeListSuffix}.
	 * @param ctx the parse tree
	 */
	default void exitTypeListSuffix(Cypher5Parser.TypeListSuffixContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#command}.
	 * @param ctx the parse tree
	 */
	default void enterCommand(Cypher5Parser.CommandContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#command}.
	 * @param ctx the parse tree
	 */
	default void exitCommand(Cypher5Parser.CommandContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#createCommand}.
	 * @param ctx the parse tree
	 */
	default void enterCreateCommand(Cypher5Parser.CreateCommandContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#createCommand}.
	 * @param ctx the parse tree
	 */
	default void exitCreateCommand(Cypher5Parser.CreateCommandContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#dropCommand}.
	 * @param ctx the parse tree
	 */
	default void enterDropCommand(Cypher5Parser.DropCommandContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#dropCommand}.
	 * @param ctx the parse tree
	 */
	default void exitDropCommand(Cypher5Parser.DropCommandContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#showCommand}.
	 * @param ctx the parse tree
	 */
	default void enterShowCommand(Cypher5Parser.ShowCommandContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#showCommand}.
	 * @param ctx the parse tree
	 */
	default void exitShowCommand(Cypher5Parser.ShowCommandContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#showCommandYield}.
	 * @param ctx the parse tree
	 */
	default void enterShowCommandYield(Cypher5Parser.ShowCommandYieldContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#showCommandYield}.
	 * @param ctx the parse tree
	 */
	default void exitShowCommandYield(Cypher5Parser.ShowCommandYieldContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#yieldItem}.
	 * @param ctx the parse tree
	 */
	default void enterYieldItem(Cypher5Parser.YieldItemContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#yieldItem}.
	 * @param ctx the parse tree
	 */
	default void exitYieldItem(Cypher5Parser.YieldItemContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#yieldSkip}.
	 * @param ctx the parse tree
	 */
	default void enterYieldSkip(Cypher5Parser.YieldSkipContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#yieldSkip}.
	 * @param ctx the parse tree
	 */
	default void exitYieldSkip(Cypher5Parser.YieldSkipContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#yieldLimit}.
	 * @param ctx the parse tree
	 */
	default void enterYieldLimit(Cypher5Parser.YieldLimitContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#yieldLimit}.
	 * @param ctx the parse tree
	 */
	default void exitYieldLimit(Cypher5Parser.YieldLimitContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#yieldClause}.
	 * @param ctx the parse tree
	 */
	default void enterYieldClause(Cypher5Parser.YieldClauseContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#yieldClause}.
	 * @param ctx the parse tree
	 */
	default void exitYieldClause(Cypher5Parser.YieldClauseContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#commandOptions}.
	 * @param ctx the parse tree
	 */
	default void enterCommandOptions(Cypher5Parser.CommandOptionsContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#commandOptions}.
	 * @param ctx the parse tree
	 */
	default void exitCommandOptions(Cypher5Parser.CommandOptionsContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#terminateCommand}.
	 * @param ctx the parse tree
	 */
	default void enterTerminateCommand(Cypher5Parser.TerminateCommandContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#terminateCommand}.
	 * @param ctx the parse tree
	 */
	default void exitTerminateCommand(Cypher5Parser.TerminateCommandContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#composableCommandClauses}.
	 * @param ctx the parse tree
	 */
	default void enterComposableCommandClauses(Cypher5Parser.ComposableCommandClausesContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#composableCommandClauses}.
	 * @param ctx the parse tree
	 */
	default void exitComposableCommandClauses(Cypher5Parser.ComposableCommandClausesContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#composableShowCommandClauses}.
	 * @param ctx the parse tree
	 */
	default void enterComposableShowCommandClauses(Cypher5Parser.ComposableShowCommandClausesContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#composableShowCommandClauses}.
	 * @param ctx the parse tree
	 */
	default void exitComposableShowCommandClauses(Cypher5Parser.ComposableShowCommandClausesContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#showBriefAndYield}.
	 * @param ctx the parse tree
	 */
	default void enterShowBriefAndYield(Cypher5Parser.ShowBriefAndYieldContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#showBriefAndYield}.
	 * @param ctx the parse tree
	 */
	default void exitShowBriefAndYield(Cypher5Parser.ShowBriefAndYieldContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#showIndexCommand}.
	 * @param ctx the parse tree
	 */
	default void enterShowIndexCommand(Cypher5Parser.ShowIndexCommandContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#showIndexCommand}.
	 * @param ctx the parse tree
	 */
	default void exitShowIndexCommand(Cypher5Parser.ShowIndexCommandContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#showIndexesAllowBrief}.
	 * @param ctx the parse tree
	 */
	default void enterShowIndexesAllowBrief(Cypher5Parser.ShowIndexesAllowBriefContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#showIndexesAllowBrief}.
	 * @param ctx the parse tree
	 */
	default void exitShowIndexesAllowBrief(Cypher5Parser.ShowIndexesAllowBriefContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#showIndexesNoBrief}.
	 * @param ctx the parse tree
	 */
	default void enterShowIndexesNoBrief(Cypher5Parser.ShowIndexesNoBriefContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#showIndexesNoBrief}.
	 * @param ctx the parse tree
	 */
	default void exitShowIndexesNoBrief(Cypher5Parser.ShowIndexesNoBriefContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#showConstraintCommand}.
	 * @param ctx the parse tree
	 */
	default void enterShowConstraintCommand(Cypher5Parser.ShowConstraintCommandContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#showConstraintCommand}.
	 * @param ctx the parse tree
	 */
	default void exitShowConstraintCommand(Cypher5Parser.ShowConstraintCommandContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#constraintAllowYieldType}.
	 * @param ctx the parse tree
	 */
	default void enterConstraintAllowYieldType(Cypher5Parser.ConstraintAllowYieldTypeContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#constraintAllowYieldType}.
	 * @param ctx the parse tree
	 */
	default void exitConstraintAllowYieldType(Cypher5Parser.ConstraintAllowYieldTypeContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#constraintExistType}.
	 * @param ctx the parse tree
	 */
	default void enterConstraintExistType(Cypher5Parser.ConstraintExistTypeContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#constraintExistType}.
	 * @param ctx the parse tree
	 */
	default void exitConstraintExistType(Cypher5Parser.ConstraintExistTypeContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#constraintBriefAndYieldType}.
	 * @param ctx the parse tree
	 */
	default void enterConstraintBriefAndYieldType(Cypher5Parser.ConstraintBriefAndYieldTypeContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#constraintBriefAndYieldType}.
	 * @param ctx the parse tree
	 */
	default void exitConstraintBriefAndYieldType(Cypher5Parser.ConstraintBriefAndYieldTypeContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#showConstraintsAllowBriefAndYield}.
	 * @param ctx the parse tree
	 */
	default void enterShowConstraintsAllowBriefAndYield(Cypher5Parser.ShowConstraintsAllowBriefAndYieldContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#showConstraintsAllowBriefAndYield}.
	 * @param ctx the parse tree
	 */
	default void exitShowConstraintsAllowBriefAndYield(Cypher5Parser.ShowConstraintsAllowBriefAndYieldContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#showConstraintsAllowBrief}.
	 * @param ctx the parse tree
	 */
	default void enterShowConstraintsAllowBrief(Cypher5Parser.ShowConstraintsAllowBriefContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#showConstraintsAllowBrief}.
	 * @param ctx the parse tree
	 */
	default void exitShowConstraintsAllowBrief(Cypher5Parser.ShowConstraintsAllowBriefContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#showConstraintsAllowYield}.
	 * @param ctx the parse tree
	 */
	default void enterShowConstraintsAllowYield(Cypher5Parser.ShowConstraintsAllowYieldContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#showConstraintsAllowYield}.
	 * @param ctx the parse tree
	 */
	default void exitShowConstraintsAllowYield(Cypher5Parser.ShowConstraintsAllowYieldContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#showProcedures}.
	 * @param ctx the parse tree
	 */
	default void enterShowProcedures(Cypher5Parser.ShowProceduresContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#showProcedures}.
	 * @param ctx the parse tree
	 */
	default void exitShowProcedures(Cypher5Parser.ShowProceduresContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#showFunctions}.
	 * @param ctx the parse tree
	 */
	default void enterShowFunctions(Cypher5Parser.ShowFunctionsContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#showFunctions}.
	 * @param ctx the parse tree
	 */
	default void exitShowFunctions(Cypher5Parser.ShowFunctionsContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#functionToken}.
	 * @param ctx the parse tree
	 */
	default void enterFunctionToken(Cypher5Parser.FunctionTokenContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#functionToken}.
	 * @param ctx the parse tree
	 */
	default void exitFunctionToken(Cypher5Parser.FunctionTokenContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#executableBy}.
	 * @param ctx the parse tree
	 */
	default void enterExecutableBy(Cypher5Parser.ExecutableByContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#executableBy}.
	 * @param ctx the parse tree
	 */
	default void exitExecutableBy(Cypher5Parser.ExecutableByContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#showFunctionsType}.
	 * @param ctx the parse tree
	 */
	default void enterShowFunctionsType(Cypher5Parser.ShowFunctionsTypeContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#showFunctionsType}.
	 * @param ctx the parse tree
	 */
	default void exitShowFunctionsType(Cypher5Parser.ShowFunctionsTypeContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#showTransactions}.
	 * @param ctx the parse tree
	 */
	default void enterShowTransactions(Cypher5Parser.ShowTransactionsContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#showTransactions}.
	 * @param ctx the parse tree
	 */
	default void exitShowTransactions(Cypher5Parser.ShowTransactionsContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#terminateTransactions}.
	 * @param ctx the parse tree
	 */
	default void enterTerminateTransactions(Cypher5Parser.TerminateTransactionsContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#terminateTransactions}.
	 * @param ctx the parse tree
	 */
	default void exitTerminateTransactions(Cypher5Parser.TerminateTransactionsContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#showSettings}.
	 * @param ctx the parse tree
	 */
	default void enterShowSettings(Cypher5Parser.ShowSettingsContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#showSettings}.
	 * @param ctx the parse tree
	 */
	default void exitShowSettings(Cypher5Parser.ShowSettingsContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#settingToken}.
	 * @param ctx the parse tree
	 */
	default void enterSettingToken(Cypher5Parser.SettingTokenContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#settingToken}.
	 * @param ctx the parse tree
	 */
	default void exitSettingToken(Cypher5Parser.SettingTokenContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#namesAndClauses}.
	 * @param ctx the parse tree
	 */
	default void enterNamesAndClauses(Cypher5Parser.NamesAndClausesContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#namesAndClauses}.
	 * @param ctx the parse tree
	 */
	default void exitNamesAndClauses(Cypher5Parser.NamesAndClausesContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#stringsOrExpression}.
	 * @param ctx the parse tree
	 */
	default void enterStringsOrExpression(Cypher5Parser.StringsOrExpressionContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#stringsOrExpression}.
	 * @param ctx the parse tree
	 */
	default void exitStringsOrExpression(Cypher5Parser.StringsOrExpressionContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#commandNodePattern}.
	 * @param ctx the parse tree
	 */
	default void enterCommandNodePattern(Cypher5Parser.CommandNodePatternContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#commandNodePattern}.
	 * @param ctx the parse tree
	 */
	default void exitCommandNodePattern(Cypher5Parser.CommandNodePatternContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#commandRelPattern}.
	 * @param ctx the parse tree
	 */
	default void enterCommandRelPattern(Cypher5Parser.CommandRelPatternContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#commandRelPattern}.
	 * @param ctx the parse tree
	 */
	default void exitCommandRelPattern(Cypher5Parser.CommandRelPatternContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#createConstraint}.
	 * @param ctx the parse tree
	 */
	default void enterCreateConstraint(Cypher5Parser.CreateConstraintContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#createConstraint}.
	 * @param ctx the parse tree
	 */
	default void exitCreateConstraint(Cypher5Parser.CreateConstraintContext ctx) { }
	/**
	 * Enter a parse tree produced by the {@code ConstraintExists}
	 * labeled alternative in {@link Cypher5Parser#constraintType}.
	 * @param ctx the parse tree
	 */
	default void enterConstraintExists(Cypher5Parser.ConstraintExistsContext ctx) { }
	/**
	 * Exit a parse tree produced by the {@code ConstraintExists}
	 * labeled alternative in {@link Cypher5Parser#constraintType}.
	 * @param ctx the parse tree
	 */
	default void exitConstraintExists(Cypher5Parser.ConstraintExistsContext ctx) { }
	/**
	 * Enter a parse tree produced by the {@code ConstraintTyped}
	 * labeled alternative in {@link Cypher5Parser#constraintType}.
	 * @param ctx the parse tree
	 */
	default void enterConstraintTyped(Cypher5Parser.ConstraintTypedContext ctx) { }
	/**
	 * Exit a parse tree produced by the {@code ConstraintTyped}
	 * labeled alternative in {@link Cypher5Parser#constraintType}.
	 * @param ctx the parse tree
	 */
	default void exitConstraintTyped(Cypher5Parser.ConstraintTypedContext ctx) { }
	/**
	 * Enter a parse tree produced by the {@code ConstraintIsUnique}
	 * labeled alternative in {@link Cypher5Parser#constraintType}.
	 * @param ctx the parse tree
	 */
	default void enterConstraintIsUnique(Cypher5Parser.ConstraintIsUniqueContext ctx) { }
	/**
	 * Exit a parse tree produced by the {@code ConstraintIsUnique}
	 * labeled alternative in {@link Cypher5Parser#constraintType}.
	 * @param ctx the parse tree
	 */
	default void exitConstraintIsUnique(Cypher5Parser.ConstraintIsUniqueContext ctx) { }
	/**
	 * Enter a parse tree produced by the {@code ConstraintKey}
	 * labeled alternative in {@link Cypher5Parser#constraintType}.
	 * @param ctx the parse tree
	 */
	default void enterConstraintKey(Cypher5Parser.ConstraintKeyContext ctx) { }
	/**
	 * Exit a parse tree produced by the {@code ConstraintKey}
	 * labeled alternative in {@link Cypher5Parser#constraintType}.
	 * @param ctx the parse tree
	 */
	default void exitConstraintKey(Cypher5Parser.ConstraintKeyContext ctx) { }
	/**
	 * Enter a parse tree produced by the {@code ConstraintIsNotNull}
	 * labeled alternative in {@link Cypher5Parser#constraintType}.
	 * @param ctx the parse tree
	 */
	default void enterConstraintIsNotNull(Cypher5Parser.ConstraintIsNotNullContext ctx) { }
	/**
	 * Exit a parse tree produced by the {@code ConstraintIsNotNull}
	 * labeled alternative in {@link Cypher5Parser#constraintType}.
	 * @param ctx the parse tree
	 */
	default void exitConstraintIsNotNull(Cypher5Parser.ConstraintIsNotNullContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#dropConstraint}.
	 * @param ctx the parse tree
	 */
	default void enterDropConstraint(Cypher5Parser.DropConstraintContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#dropConstraint}.
	 * @param ctx the parse tree
	 */
	default void exitDropConstraint(Cypher5Parser.DropConstraintContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#createIndex}.
	 * @param ctx the parse tree
	 */
	default void enterCreateIndex(Cypher5Parser.CreateIndexContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#createIndex}.
	 * @param ctx the parse tree
	 */
	default void exitCreateIndex(Cypher5Parser.CreateIndexContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#oldCreateIndex}.
	 * @param ctx the parse tree
	 */
	default void enterOldCreateIndex(Cypher5Parser.OldCreateIndexContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#oldCreateIndex}.
	 * @param ctx the parse tree
	 */
	default void exitOldCreateIndex(Cypher5Parser.OldCreateIndexContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#createIndex_}.
	 * @param ctx the parse tree
	 */
	default void enterCreateIndex_(Cypher5Parser.CreateIndex_Context ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#createIndex_}.
	 * @param ctx the parse tree
	 */
	default void exitCreateIndex_(Cypher5Parser.CreateIndex_Context ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#createFulltextIndex}.
	 * @param ctx the parse tree
	 */
	default void enterCreateFulltextIndex(Cypher5Parser.CreateFulltextIndexContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#createFulltextIndex}.
	 * @param ctx the parse tree
	 */
	default void exitCreateFulltextIndex(Cypher5Parser.CreateFulltextIndexContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#fulltextNodePattern}.
	 * @param ctx the parse tree
	 */
	default void enterFulltextNodePattern(Cypher5Parser.FulltextNodePatternContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#fulltextNodePattern}.
	 * @param ctx the parse tree
	 */
	default void exitFulltextNodePattern(Cypher5Parser.FulltextNodePatternContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#fulltextRelPattern}.
	 * @param ctx the parse tree
	 */
	default void enterFulltextRelPattern(Cypher5Parser.FulltextRelPatternContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#fulltextRelPattern}.
	 * @param ctx the parse tree
	 */
	default void exitFulltextRelPattern(Cypher5Parser.FulltextRelPatternContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#createLookupIndex}.
	 * @param ctx the parse tree
	 */
	default void enterCreateLookupIndex(Cypher5Parser.CreateLookupIndexContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#createLookupIndex}.
	 * @param ctx the parse tree
	 */
	default void exitCreateLookupIndex(Cypher5Parser.CreateLookupIndexContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#lookupIndexNodePattern}.
	 * @param ctx the parse tree
	 */
	default void enterLookupIndexNodePattern(Cypher5Parser.LookupIndexNodePatternContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#lookupIndexNodePattern}.
	 * @param ctx the parse tree
	 */
	default void exitLookupIndexNodePattern(Cypher5Parser.LookupIndexNodePatternContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#lookupIndexRelPattern}.
	 * @param ctx the parse tree
	 */
	default void enterLookupIndexRelPattern(Cypher5Parser.LookupIndexRelPatternContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#lookupIndexRelPattern}.
	 * @param ctx the parse tree
	 */
	default void exitLookupIndexRelPattern(Cypher5Parser.LookupIndexRelPatternContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#dropIndex}.
	 * @param ctx the parse tree
	 */
	default void enterDropIndex(Cypher5Parser.DropIndexContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#dropIndex}.
	 * @param ctx the parse tree
	 */
	default void exitDropIndex(Cypher5Parser.DropIndexContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#propertyList}.
	 * @param ctx the parse tree
	 */
	default void enterPropertyList(Cypher5Parser.PropertyListContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#propertyList}.
	 * @param ctx the parse tree
	 */
	default void exitPropertyList(Cypher5Parser.PropertyListContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#enclosedPropertyList}.
	 * @param ctx the parse tree
	 */
	default void enterEnclosedPropertyList(Cypher5Parser.EnclosedPropertyListContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#enclosedPropertyList}.
	 * @param ctx the parse tree
	 */
	default void exitEnclosedPropertyList(Cypher5Parser.EnclosedPropertyListContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#alterCommand}.
	 * @param ctx the parse tree
	 */
	default void enterAlterCommand(Cypher5Parser.AlterCommandContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#alterCommand}.
	 * @param ctx the parse tree
	 */
	default void exitAlterCommand(Cypher5Parser.AlterCommandContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#renameCommand}.
	 * @param ctx the parse tree
	 */
	default void enterRenameCommand(Cypher5Parser.RenameCommandContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#renameCommand}.
	 * @param ctx the parse tree
	 */
	default void exitRenameCommand(Cypher5Parser.RenameCommandContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#grantCommand}.
	 * @param ctx the parse tree
	 */
	default void enterGrantCommand(Cypher5Parser.GrantCommandContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#grantCommand}.
	 * @param ctx the parse tree
	 */
	default void exitGrantCommand(Cypher5Parser.GrantCommandContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#denyCommand}.
	 * @param ctx the parse tree
	 */
	default void enterDenyCommand(Cypher5Parser.DenyCommandContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#denyCommand}.
	 * @param ctx the parse tree
	 */
	default void exitDenyCommand(Cypher5Parser.DenyCommandContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#revokeCommand}.
	 * @param ctx the parse tree
	 */
	default void enterRevokeCommand(Cypher5Parser.RevokeCommandContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#revokeCommand}.
	 * @param ctx the parse tree
	 */
	default void exitRevokeCommand(Cypher5Parser.RevokeCommandContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#userNames}.
	 * @param ctx the parse tree
	 */
	default void enterUserNames(Cypher5Parser.UserNamesContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#userNames}.
	 * @param ctx the parse tree
	 */
	default void exitUserNames(Cypher5Parser.UserNamesContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#roleNames}.
	 * @param ctx the parse tree
	 */
	default void enterRoleNames(Cypher5Parser.RoleNamesContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#roleNames}.
	 * @param ctx the parse tree
	 */
	default void exitRoleNames(Cypher5Parser.RoleNamesContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#roleToken}.
	 * @param ctx the parse tree
	 */
	default void enterRoleToken(Cypher5Parser.RoleTokenContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#roleToken}.
	 * @param ctx the parse tree
	 */
	default void exitRoleToken(Cypher5Parser.RoleTokenContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#enableServerCommand}.
	 * @param ctx the parse tree
	 */
	default void enterEnableServerCommand(Cypher5Parser.EnableServerCommandContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#enableServerCommand}.
	 * @param ctx the parse tree
	 */
	default void exitEnableServerCommand(Cypher5Parser.EnableServerCommandContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#alterServer}.
	 * @param ctx the parse tree
	 */
	default void enterAlterServer(Cypher5Parser.AlterServerContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#alterServer}.
	 * @param ctx the parse tree
	 */
	default void exitAlterServer(Cypher5Parser.AlterServerContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#renameServer}.
	 * @param ctx the parse tree
	 */
	default void enterRenameServer(Cypher5Parser.RenameServerContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#renameServer}.
	 * @param ctx the parse tree
	 */
	default void exitRenameServer(Cypher5Parser.RenameServerContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#dropServer}.
	 * @param ctx the parse tree
	 */
	default void enterDropServer(Cypher5Parser.DropServerContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#dropServer}.
	 * @param ctx the parse tree
	 */
	default void exitDropServer(Cypher5Parser.DropServerContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#showServers}.
	 * @param ctx the parse tree
	 */
	default void enterShowServers(Cypher5Parser.ShowServersContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#showServers}.
	 * @param ctx the parse tree
	 */
	default void exitShowServers(Cypher5Parser.ShowServersContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#allocationCommand}.
	 * @param ctx the parse tree
	 */
	default void enterAllocationCommand(Cypher5Parser.AllocationCommandContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#allocationCommand}.
	 * @param ctx the parse tree
	 */
	default void exitAllocationCommand(Cypher5Parser.AllocationCommandContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#deallocateDatabaseFromServers}.
	 * @param ctx the parse tree
	 */
	default void enterDeallocateDatabaseFromServers(Cypher5Parser.DeallocateDatabaseFromServersContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#deallocateDatabaseFromServers}.
	 * @param ctx the parse tree
	 */
	default void exitDeallocateDatabaseFromServers(Cypher5Parser.DeallocateDatabaseFromServersContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#reallocateDatabases}.
	 * @param ctx the parse tree
	 */
	default void enterReallocateDatabases(Cypher5Parser.ReallocateDatabasesContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#reallocateDatabases}.
	 * @param ctx the parse tree
	 */
	default void exitReallocateDatabases(Cypher5Parser.ReallocateDatabasesContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#createRole}.
	 * @param ctx the parse tree
	 */
	default void enterCreateRole(Cypher5Parser.CreateRoleContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#createRole}.
	 * @param ctx the parse tree
	 */
	default void exitCreateRole(Cypher5Parser.CreateRoleContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#dropRole}.
	 * @param ctx the parse tree
	 */
	default void enterDropRole(Cypher5Parser.DropRoleContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#dropRole}.
	 * @param ctx the parse tree
	 */
	default void exitDropRole(Cypher5Parser.DropRoleContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#renameRole}.
	 * @param ctx the parse tree
	 */
	default void enterRenameRole(Cypher5Parser.RenameRoleContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#renameRole}.
	 * @param ctx the parse tree
	 */
	default void exitRenameRole(Cypher5Parser.RenameRoleContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#showRoles}.
	 * @param ctx the parse tree
	 */
	default void enterShowRoles(Cypher5Parser.ShowRolesContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#showRoles}.
	 * @param ctx the parse tree
	 */
	default void exitShowRoles(Cypher5Parser.ShowRolesContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#grantRole}.
	 * @param ctx the parse tree
	 */
	default void enterGrantRole(Cypher5Parser.GrantRoleContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#grantRole}.
	 * @param ctx the parse tree
	 */
	default void exitGrantRole(Cypher5Parser.GrantRoleContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#revokeRole}.
	 * @param ctx the parse tree
	 */
	default void enterRevokeRole(Cypher5Parser.RevokeRoleContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#revokeRole}.
	 * @param ctx the parse tree
	 */
	default void exitRevokeRole(Cypher5Parser.RevokeRoleContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#createUser}.
	 * @param ctx the parse tree
	 */
	default void enterCreateUser(Cypher5Parser.CreateUserContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#createUser}.
	 * @param ctx the parse tree
	 */
	default void exitCreateUser(Cypher5Parser.CreateUserContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#dropUser}.
	 * @param ctx the parse tree
	 */
	default void enterDropUser(Cypher5Parser.DropUserContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#dropUser}.
	 * @param ctx the parse tree
	 */
	default void exitDropUser(Cypher5Parser.DropUserContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#renameUser}.
	 * @param ctx the parse tree
	 */
	default void enterRenameUser(Cypher5Parser.RenameUserContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#renameUser}.
	 * @param ctx the parse tree
	 */
	default void exitRenameUser(Cypher5Parser.RenameUserContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#alterCurrentUser}.
	 * @param ctx the parse tree
	 */
	default void enterAlterCurrentUser(Cypher5Parser.AlterCurrentUserContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#alterCurrentUser}.
	 * @param ctx the parse tree
	 */
	default void exitAlterCurrentUser(Cypher5Parser.AlterCurrentUserContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#alterUser}.
	 * @param ctx the parse tree
	 */
	default void enterAlterUser(Cypher5Parser.AlterUserContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#alterUser}.
	 * @param ctx the parse tree
	 */
	default void exitAlterUser(Cypher5Parser.AlterUserContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#removeNamedProvider}.
	 * @param ctx the parse tree
	 */
	default void enterRemoveNamedProvider(Cypher5Parser.RemoveNamedProviderContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#removeNamedProvider}.
	 * @param ctx the parse tree
	 */
	default void exitRemoveNamedProvider(Cypher5Parser.RemoveNamedProviderContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#password}.
	 * @param ctx the parse tree
	 */
	default void enterPassword(Cypher5Parser.PasswordContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#password}.
	 * @param ctx the parse tree
	 */
	default void exitPassword(Cypher5Parser.PasswordContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#passwordOnly}.
	 * @param ctx the parse tree
	 */
	default void enterPasswordOnly(Cypher5Parser.PasswordOnlyContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#passwordOnly}.
	 * @param ctx the parse tree
	 */
	default void exitPasswordOnly(Cypher5Parser.PasswordOnlyContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#passwordExpression}.
	 * @param ctx the parse tree
	 */
	default void enterPasswordExpression(Cypher5Parser.PasswordExpressionContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#passwordExpression}.
	 * @param ctx the parse tree
	 */
	default void exitPasswordExpression(Cypher5Parser.PasswordExpressionContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#passwordChangeRequired}.
	 * @param ctx the parse tree
	 */
	default void enterPasswordChangeRequired(Cypher5Parser.PasswordChangeRequiredContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#passwordChangeRequired}.
	 * @param ctx the parse tree
	 */
	default void exitPasswordChangeRequired(Cypher5Parser.PasswordChangeRequiredContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#userStatus}.
	 * @param ctx the parse tree
	 */
	default void enterUserStatus(Cypher5Parser.UserStatusContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#userStatus}.
	 * @param ctx the parse tree
	 */
	default void exitUserStatus(Cypher5Parser.UserStatusContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#homeDatabase}.
	 * @param ctx the parse tree
	 */
	default void enterHomeDatabase(Cypher5Parser.HomeDatabaseContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#homeDatabase}.
	 * @param ctx the parse tree
	 */
	default void exitHomeDatabase(Cypher5Parser.HomeDatabaseContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#setAuthClause}.
	 * @param ctx the parse tree
	 */
	default void enterSetAuthClause(Cypher5Parser.SetAuthClauseContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#setAuthClause}.
	 * @param ctx the parse tree
	 */
	default void exitSetAuthClause(Cypher5Parser.SetAuthClauseContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#userAuthAttribute}.
	 * @param ctx the parse tree
	 */
	default void enterUserAuthAttribute(Cypher5Parser.UserAuthAttributeContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#userAuthAttribute}.
	 * @param ctx the parse tree
	 */
	default void exitUserAuthAttribute(Cypher5Parser.UserAuthAttributeContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#showUsers}.
	 * @param ctx the parse tree
	 */
	default void enterShowUsers(Cypher5Parser.ShowUsersContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#showUsers}.
	 * @param ctx the parse tree
	 */
	default void exitShowUsers(Cypher5Parser.ShowUsersContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#showCurrentUser}.
	 * @param ctx the parse tree
	 */
	default void enterShowCurrentUser(Cypher5Parser.ShowCurrentUserContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#showCurrentUser}.
	 * @param ctx the parse tree
	 */
	default void exitShowCurrentUser(Cypher5Parser.ShowCurrentUserContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#showSupportedPrivileges}.
	 * @param ctx the parse tree
	 */
	default void enterShowSupportedPrivileges(Cypher5Parser.ShowSupportedPrivilegesContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#showSupportedPrivileges}.
	 * @param ctx the parse tree
	 */
	default void exitShowSupportedPrivileges(Cypher5Parser.ShowSupportedPrivilegesContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#showPrivileges}.
	 * @param ctx the parse tree
	 */
	default void enterShowPrivileges(Cypher5Parser.ShowPrivilegesContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#showPrivileges}.
	 * @param ctx the parse tree
	 */
	default void exitShowPrivileges(Cypher5Parser.ShowPrivilegesContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#showRolePrivileges}.
	 * @param ctx the parse tree
	 */
	default void enterShowRolePrivileges(Cypher5Parser.ShowRolePrivilegesContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#showRolePrivileges}.
	 * @param ctx the parse tree
	 */
	default void exitShowRolePrivileges(Cypher5Parser.ShowRolePrivilegesContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#showUserPrivileges}.
	 * @param ctx the parse tree
	 */
	default void enterShowUserPrivileges(Cypher5Parser.ShowUserPrivilegesContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#showUserPrivileges}.
	 * @param ctx the parse tree
	 */
	default void exitShowUserPrivileges(Cypher5Parser.ShowUserPrivilegesContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#privilegeAsCommand}.
	 * @param ctx the parse tree
	 */
	default void enterPrivilegeAsCommand(Cypher5Parser.PrivilegeAsCommandContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#privilegeAsCommand}.
	 * @param ctx the parse tree
	 */
	default void exitPrivilegeAsCommand(Cypher5Parser.PrivilegeAsCommandContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#privilegeToken}.
	 * @param ctx the parse tree
	 */
	default void enterPrivilegeToken(Cypher5Parser.PrivilegeTokenContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#privilegeToken}.
	 * @param ctx the parse tree
	 */
	default void exitPrivilegeToken(Cypher5Parser.PrivilegeTokenContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#privilege}.
	 * @param ctx the parse tree
	 */
	default void enterPrivilege(Cypher5Parser.PrivilegeContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#privilege}.
	 * @param ctx the parse tree
	 */
	default void exitPrivilege(Cypher5Parser.PrivilegeContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#allPrivilege}.
	 * @param ctx the parse tree
	 */
	default void enterAllPrivilege(Cypher5Parser.AllPrivilegeContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#allPrivilege}.
	 * @param ctx the parse tree
	 */
	default void exitAllPrivilege(Cypher5Parser.AllPrivilegeContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#allPrivilegeType}.
	 * @param ctx the parse tree
	 */
	default void enterAllPrivilegeType(Cypher5Parser.AllPrivilegeTypeContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#allPrivilegeType}.
	 * @param ctx the parse tree
	 */
	default void exitAllPrivilegeType(Cypher5Parser.AllPrivilegeTypeContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#allPrivilegeTarget}.
	 * @param ctx the parse tree
	 */
	default void enterAllPrivilegeTarget(Cypher5Parser.AllPrivilegeTargetContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#allPrivilegeTarget}.
	 * @param ctx the parse tree
	 */
	default void exitAllPrivilegeTarget(Cypher5Parser.AllPrivilegeTargetContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#createPrivilege}.
	 * @param ctx the parse tree
	 */
	default void enterCreatePrivilege(Cypher5Parser.CreatePrivilegeContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#createPrivilege}.
	 * @param ctx the parse tree
	 */
	default void exitCreatePrivilege(Cypher5Parser.CreatePrivilegeContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#createPrivilegeForDatabase}.
	 * @param ctx the parse tree
	 */
	default void enterCreatePrivilegeForDatabase(Cypher5Parser.CreatePrivilegeForDatabaseContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#createPrivilegeForDatabase}.
	 * @param ctx the parse tree
	 */
	default void exitCreatePrivilegeForDatabase(Cypher5Parser.CreatePrivilegeForDatabaseContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#createNodePrivilegeToken}.
	 * @param ctx the parse tree
	 */
	default void enterCreateNodePrivilegeToken(Cypher5Parser.CreateNodePrivilegeTokenContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#createNodePrivilegeToken}.
	 * @param ctx the parse tree
	 */
	default void exitCreateNodePrivilegeToken(Cypher5Parser.CreateNodePrivilegeTokenContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#createRelPrivilegeToken}.
	 * @param ctx the parse tree
	 */
	default void enterCreateRelPrivilegeToken(Cypher5Parser.CreateRelPrivilegeTokenContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#createRelPrivilegeToken}.
	 * @param ctx the parse tree
	 */
	default void exitCreateRelPrivilegeToken(Cypher5Parser.CreateRelPrivilegeTokenContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#createPropertyPrivilegeToken}.
	 * @param ctx the parse tree
	 */
	default void enterCreatePropertyPrivilegeToken(Cypher5Parser.CreatePropertyPrivilegeTokenContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#createPropertyPrivilegeToken}.
	 * @param ctx the parse tree
	 */
	default void exitCreatePropertyPrivilegeToken(Cypher5Parser.CreatePropertyPrivilegeTokenContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#actionForDBMS}.
	 * @param ctx the parse tree
	 */
	default void enterActionForDBMS(Cypher5Parser.ActionForDBMSContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#actionForDBMS}.
	 * @param ctx the parse tree
	 */
	default void exitActionForDBMS(Cypher5Parser.ActionForDBMSContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#dropPrivilege}.
	 * @param ctx the parse tree
	 */
	default void enterDropPrivilege(Cypher5Parser.DropPrivilegeContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#dropPrivilege}.
	 * @param ctx the parse tree
	 */
	default void exitDropPrivilege(Cypher5Parser.DropPrivilegeContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#loadPrivilege}.
	 * @param ctx the parse tree
	 */
	default void enterLoadPrivilege(Cypher5Parser.LoadPrivilegeContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#loadPrivilege}.
	 * @param ctx the parse tree
	 */
	default void exitLoadPrivilege(Cypher5Parser.LoadPrivilegeContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#showPrivilege}.
	 * @param ctx the parse tree
	 */
	default void enterShowPrivilege(Cypher5Parser.ShowPrivilegeContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#showPrivilege}.
	 * @param ctx the parse tree
	 */
	default void exitShowPrivilege(Cypher5Parser.ShowPrivilegeContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#setPrivilege}.
	 * @param ctx the parse tree
	 */
	default void enterSetPrivilege(Cypher5Parser.SetPrivilegeContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#setPrivilege}.
	 * @param ctx the parse tree
	 */
	default void exitSetPrivilege(Cypher5Parser.SetPrivilegeContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#passwordToken}.
	 * @param ctx the parse tree
	 */
	default void enterPasswordToken(Cypher5Parser.PasswordTokenContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#passwordToken}.
	 * @param ctx the parse tree
	 */
	default void exitPasswordToken(Cypher5Parser.PasswordTokenContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#removePrivilege}.
	 * @param ctx the parse tree
	 */
	default void enterRemovePrivilege(Cypher5Parser.RemovePrivilegeContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#removePrivilege}.
	 * @param ctx the parse tree
	 */
	default void exitRemovePrivilege(Cypher5Parser.RemovePrivilegeContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#writePrivilege}.
	 * @param ctx the parse tree
	 */
	default void enterWritePrivilege(Cypher5Parser.WritePrivilegeContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#writePrivilege}.
	 * @param ctx the parse tree
	 */
	default void exitWritePrivilege(Cypher5Parser.WritePrivilegeContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#databasePrivilege}.
	 * @param ctx the parse tree
	 */
	default void enterDatabasePrivilege(Cypher5Parser.DatabasePrivilegeContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#databasePrivilege}.
	 * @param ctx the parse tree
	 */
	default void exitDatabasePrivilege(Cypher5Parser.DatabasePrivilegeContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#dbmsPrivilege}.
	 * @param ctx the parse tree
	 */
	default void enterDbmsPrivilege(Cypher5Parser.DbmsPrivilegeContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#dbmsPrivilege}.
	 * @param ctx the parse tree
	 */
	default void exitDbmsPrivilege(Cypher5Parser.DbmsPrivilegeContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#dbmsPrivilegeExecute}.
	 * @param ctx the parse tree
	 */
	default void enterDbmsPrivilegeExecute(Cypher5Parser.DbmsPrivilegeExecuteContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#dbmsPrivilegeExecute}.
	 * @param ctx the parse tree
	 */
	default void exitDbmsPrivilegeExecute(Cypher5Parser.DbmsPrivilegeExecuteContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#adminToken}.
	 * @param ctx the parse tree
	 */
	default void enterAdminToken(Cypher5Parser.AdminTokenContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#adminToken}.
	 * @param ctx the parse tree
	 */
	default void exitAdminToken(Cypher5Parser.AdminTokenContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#procedureToken}.
	 * @param ctx the parse tree
	 */
	default void enterProcedureToken(Cypher5Parser.ProcedureTokenContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#procedureToken}.
	 * @param ctx the parse tree
	 */
	default void exitProcedureToken(Cypher5Parser.ProcedureTokenContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#indexToken}.
	 * @param ctx the parse tree
	 */
	default void enterIndexToken(Cypher5Parser.IndexTokenContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#indexToken}.
	 * @param ctx the parse tree
	 */
	default void exitIndexToken(Cypher5Parser.IndexTokenContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#constraintToken}.
	 * @param ctx the parse tree
	 */
	default void enterConstraintToken(Cypher5Parser.ConstraintTokenContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#constraintToken}.
	 * @param ctx the parse tree
	 */
	default void exitConstraintToken(Cypher5Parser.ConstraintTokenContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#transactionToken}.
	 * @param ctx the parse tree
	 */
	default void enterTransactionToken(Cypher5Parser.TransactionTokenContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#transactionToken}.
	 * @param ctx the parse tree
	 */
	default void exitTransactionToken(Cypher5Parser.TransactionTokenContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#userQualifier}.
	 * @param ctx the parse tree
	 */
	default void enterUserQualifier(Cypher5Parser.UserQualifierContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#userQualifier}.
	 * @param ctx the parse tree
	 */
	default void exitUserQualifier(Cypher5Parser.UserQualifierContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#executeFunctionQualifier}.
	 * @param ctx the parse tree
	 */
	default void enterExecuteFunctionQualifier(Cypher5Parser.ExecuteFunctionQualifierContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#executeFunctionQualifier}.
	 * @param ctx the parse tree
	 */
	default void exitExecuteFunctionQualifier(Cypher5Parser.ExecuteFunctionQualifierContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#executeProcedureQualifier}.
	 * @param ctx the parse tree
	 */
	default void enterExecuteProcedureQualifier(Cypher5Parser.ExecuteProcedureQualifierContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#executeProcedureQualifier}.
	 * @param ctx the parse tree
	 */
	default void exitExecuteProcedureQualifier(Cypher5Parser.ExecuteProcedureQualifierContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#settingQualifier}.
	 * @param ctx the parse tree
	 */
	default void enterSettingQualifier(Cypher5Parser.SettingQualifierContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#settingQualifier}.
	 * @param ctx the parse tree
	 */
	default void exitSettingQualifier(Cypher5Parser.SettingQualifierContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#globs}.
	 * @param ctx the parse tree
	 */
	default void enterGlobs(Cypher5Parser.GlobsContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#globs}.
	 * @param ctx the parse tree
	 */
	default void exitGlobs(Cypher5Parser.GlobsContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#glob}.
	 * @param ctx the parse tree
	 */
	default void enterGlob(Cypher5Parser.GlobContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#glob}.
	 * @param ctx the parse tree
	 */
	default void exitGlob(Cypher5Parser.GlobContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#globRecursive}.
	 * @param ctx the parse tree
	 */
	default void enterGlobRecursive(Cypher5Parser.GlobRecursiveContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#globRecursive}.
	 * @param ctx the parse tree
	 */
	default void exitGlobRecursive(Cypher5Parser.GlobRecursiveContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#globPart}.
	 * @param ctx the parse tree
	 */
	default void enterGlobPart(Cypher5Parser.GlobPartContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#globPart}.
	 * @param ctx the parse tree
	 */
	default void exitGlobPart(Cypher5Parser.GlobPartContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#qualifiedGraphPrivilegesWithProperty}.
	 * @param ctx the parse tree
	 */
	default void enterQualifiedGraphPrivilegesWithProperty(Cypher5Parser.QualifiedGraphPrivilegesWithPropertyContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#qualifiedGraphPrivilegesWithProperty}.
	 * @param ctx the parse tree
	 */
	default void exitQualifiedGraphPrivilegesWithProperty(Cypher5Parser.QualifiedGraphPrivilegesWithPropertyContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#qualifiedGraphPrivileges}.
	 * @param ctx the parse tree
	 */
	default void enterQualifiedGraphPrivileges(Cypher5Parser.QualifiedGraphPrivilegesContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#qualifiedGraphPrivileges}.
	 * @param ctx the parse tree
	 */
	default void exitQualifiedGraphPrivileges(Cypher5Parser.QualifiedGraphPrivilegesContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#labelsResource}.
	 * @param ctx the parse tree
	 */
	default void enterLabelsResource(Cypher5Parser.LabelsResourceContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#labelsResource}.
	 * @param ctx the parse tree
	 */
	default void exitLabelsResource(Cypher5Parser.LabelsResourceContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#propertiesResource}.
	 * @param ctx the parse tree
	 */
	default void enterPropertiesResource(Cypher5Parser.PropertiesResourceContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#propertiesResource}.
	 * @param ctx the parse tree
	 */
	default void exitPropertiesResource(Cypher5Parser.PropertiesResourceContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#nonEmptyStringList}.
	 * @param ctx the parse tree
	 */
	default void enterNonEmptyStringList(Cypher5Parser.NonEmptyStringListContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#nonEmptyStringList}.
	 * @param ctx the parse tree
	 */
	default void exitNonEmptyStringList(Cypher5Parser.NonEmptyStringListContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#graphQualifier}.
	 * @param ctx the parse tree
	 */
	default void enterGraphQualifier(Cypher5Parser.GraphQualifierContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#graphQualifier}.
	 * @param ctx the parse tree
	 */
	default void exitGraphQualifier(Cypher5Parser.GraphQualifierContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#graphQualifierToken}.
	 * @param ctx the parse tree
	 */
	default void enterGraphQualifierToken(Cypher5Parser.GraphQualifierTokenContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#graphQualifierToken}.
	 * @param ctx the parse tree
	 */
	default void exitGraphQualifierToken(Cypher5Parser.GraphQualifierTokenContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#relToken}.
	 * @param ctx the parse tree
	 */
	default void enterRelToken(Cypher5Parser.RelTokenContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#relToken}.
	 * @param ctx the parse tree
	 */
	default void exitRelToken(Cypher5Parser.RelTokenContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#elementToken}.
	 * @param ctx the parse tree
	 */
	default void enterElementToken(Cypher5Parser.ElementTokenContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#elementToken}.
	 * @param ctx the parse tree
	 */
	default void exitElementToken(Cypher5Parser.ElementTokenContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#nodeToken}.
	 * @param ctx the parse tree
	 */
	default void enterNodeToken(Cypher5Parser.NodeTokenContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#nodeToken}.
	 * @param ctx the parse tree
	 */
	default void exitNodeToken(Cypher5Parser.NodeTokenContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#databaseScope}.
	 * @param ctx the parse tree
	 */
	default void enterDatabaseScope(Cypher5Parser.DatabaseScopeContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#databaseScope}.
	 * @param ctx the parse tree
	 */
	default void exitDatabaseScope(Cypher5Parser.DatabaseScopeContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#graphScope}.
	 * @param ctx the parse tree
	 */
	default void enterGraphScope(Cypher5Parser.GraphScopeContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#graphScope}.
	 * @param ctx the parse tree
	 */
	default void exitGraphScope(Cypher5Parser.GraphScopeContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#createCompositeDatabase}.
	 * @param ctx the parse tree
	 */
	default void enterCreateCompositeDatabase(Cypher5Parser.CreateCompositeDatabaseContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#createCompositeDatabase}.
	 * @param ctx the parse tree
	 */
	default void exitCreateCompositeDatabase(Cypher5Parser.CreateCompositeDatabaseContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#createDatabase}.
	 * @param ctx the parse tree
	 */
	default void enterCreateDatabase(Cypher5Parser.CreateDatabaseContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#createDatabase}.
	 * @param ctx the parse tree
	 */
	default void exitCreateDatabase(Cypher5Parser.CreateDatabaseContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#primaryTopology}.
	 * @param ctx the parse tree
	 */
	default void enterPrimaryTopology(Cypher5Parser.PrimaryTopologyContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#primaryTopology}.
	 * @param ctx the parse tree
	 */
	default void exitPrimaryTopology(Cypher5Parser.PrimaryTopologyContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#primaryToken}.
	 * @param ctx the parse tree
	 */
	default void enterPrimaryToken(Cypher5Parser.PrimaryTokenContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#primaryToken}.
	 * @param ctx the parse tree
	 */
	default void exitPrimaryToken(Cypher5Parser.PrimaryTokenContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#secondaryTopology}.
	 * @param ctx the parse tree
	 */
	default void enterSecondaryTopology(Cypher5Parser.SecondaryTopologyContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#secondaryTopology}.
	 * @param ctx the parse tree
	 */
	default void exitSecondaryTopology(Cypher5Parser.SecondaryTopologyContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#secondaryToken}.
	 * @param ctx the parse tree
	 */
	default void enterSecondaryToken(Cypher5Parser.SecondaryTokenContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#secondaryToken}.
	 * @param ctx the parse tree
	 */
	default void exitSecondaryToken(Cypher5Parser.SecondaryTokenContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#dropDatabase}.
	 * @param ctx the parse tree
	 */
	default void enterDropDatabase(Cypher5Parser.DropDatabaseContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#dropDatabase}.
	 * @param ctx the parse tree
	 */
	default void exitDropDatabase(Cypher5Parser.DropDatabaseContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#aliasAction}.
	 * @param ctx the parse tree
	 */
	default void enterAliasAction(Cypher5Parser.AliasActionContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#aliasAction}.
	 * @param ctx the parse tree
	 */
	default void exitAliasAction(Cypher5Parser.AliasActionContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#alterDatabase}.
	 * @param ctx the parse tree
	 */
	default void enterAlterDatabase(Cypher5Parser.AlterDatabaseContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#alterDatabase}.
	 * @param ctx the parse tree
	 */
	default void exitAlterDatabase(Cypher5Parser.AlterDatabaseContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#alterDatabaseAccess}.
	 * @param ctx the parse tree
	 */
	default void enterAlterDatabaseAccess(Cypher5Parser.AlterDatabaseAccessContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#alterDatabaseAccess}.
	 * @param ctx the parse tree
	 */
	default void exitAlterDatabaseAccess(Cypher5Parser.AlterDatabaseAccessContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#alterDatabaseTopology}.
	 * @param ctx the parse tree
	 */
	default void enterAlterDatabaseTopology(Cypher5Parser.AlterDatabaseTopologyContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#alterDatabaseTopology}.
	 * @param ctx the parse tree
	 */
	default void exitAlterDatabaseTopology(Cypher5Parser.AlterDatabaseTopologyContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#alterDatabaseOption}.
	 * @param ctx the parse tree
	 */
	default void enterAlterDatabaseOption(Cypher5Parser.AlterDatabaseOptionContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#alterDatabaseOption}.
	 * @param ctx the parse tree
	 */
	default void exitAlterDatabaseOption(Cypher5Parser.AlterDatabaseOptionContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#startDatabase}.
	 * @param ctx the parse tree
	 */
	default void enterStartDatabase(Cypher5Parser.StartDatabaseContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#startDatabase}.
	 * @param ctx the parse tree
	 */
	default void exitStartDatabase(Cypher5Parser.StartDatabaseContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#stopDatabase}.
	 * @param ctx the parse tree
	 */
	default void enterStopDatabase(Cypher5Parser.StopDatabaseContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#stopDatabase}.
	 * @param ctx the parse tree
	 */
	default void exitStopDatabase(Cypher5Parser.StopDatabaseContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#waitClause}.
	 * @param ctx the parse tree
	 */
	default void enterWaitClause(Cypher5Parser.WaitClauseContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#waitClause}.
	 * @param ctx the parse tree
	 */
	default void exitWaitClause(Cypher5Parser.WaitClauseContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#secondsToken}.
	 * @param ctx the parse tree
	 */
	default void enterSecondsToken(Cypher5Parser.SecondsTokenContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#secondsToken}.
	 * @param ctx the parse tree
	 */
	default void exitSecondsToken(Cypher5Parser.SecondsTokenContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#showDatabase}.
	 * @param ctx the parse tree
	 */
	default void enterShowDatabase(Cypher5Parser.ShowDatabaseContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#showDatabase}.
	 * @param ctx the parse tree
	 */
	default void exitShowDatabase(Cypher5Parser.ShowDatabaseContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#aliasName}.
	 * @param ctx the parse tree
	 */
	default void enterAliasName(Cypher5Parser.AliasNameContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#aliasName}.
	 * @param ctx the parse tree
	 */
	default void exitAliasName(Cypher5Parser.AliasNameContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#databaseName}.
	 * @param ctx the parse tree
	 */
	default void enterDatabaseName(Cypher5Parser.DatabaseNameContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#databaseName}.
	 * @param ctx the parse tree
	 */
	default void exitDatabaseName(Cypher5Parser.DatabaseNameContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#createAlias}.
	 * @param ctx the parse tree
	 */
	default void enterCreateAlias(Cypher5Parser.CreateAliasContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#createAlias}.
	 * @param ctx the parse tree
	 */
	default void exitCreateAlias(Cypher5Parser.CreateAliasContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#dropAlias}.
	 * @param ctx the parse tree
	 */
	default void enterDropAlias(Cypher5Parser.DropAliasContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#dropAlias}.
	 * @param ctx the parse tree
	 */
	default void exitDropAlias(Cypher5Parser.DropAliasContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#alterAlias}.
	 * @param ctx the parse tree
	 */
	default void enterAlterAlias(Cypher5Parser.AlterAliasContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#alterAlias}.
	 * @param ctx the parse tree
	 */
	default void exitAlterAlias(Cypher5Parser.AlterAliasContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#alterAliasTarget}.
	 * @param ctx the parse tree
	 */
	default void enterAlterAliasTarget(Cypher5Parser.AlterAliasTargetContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#alterAliasTarget}.
	 * @param ctx the parse tree
	 */
	default void exitAlterAliasTarget(Cypher5Parser.AlterAliasTargetContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#alterAliasUser}.
	 * @param ctx the parse tree
	 */
	default void enterAlterAliasUser(Cypher5Parser.AlterAliasUserContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#alterAliasUser}.
	 * @param ctx the parse tree
	 */
	default void exitAlterAliasUser(Cypher5Parser.AlterAliasUserContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#alterAliasPassword}.
	 * @param ctx the parse tree
	 */
	default void enterAlterAliasPassword(Cypher5Parser.AlterAliasPasswordContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#alterAliasPassword}.
	 * @param ctx the parse tree
	 */
	default void exitAlterAliasPassword(Cypher5Parser.AlterAliasPasswordContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#alterAliasDriver}.
	 * @param ctx the parse tree
	 */
	default void enterAlterAliasDriver(Cypher5Parser.AlterAliasDriverContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#alterAliasDriver}.
	 * @param ctx the parse tree
	 */
	default void exitAlterAliasDriver(Cypher5Parser.AlterAliasDriverContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#alterAliasProperties}.
	 * @param ctx the parse tree
	 */
	default void enterAlterAliasProperties(Cypher5Parser.AlterAliasPropertiesContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#alterAliasProperties}.
	 * @param ctx the parse tree
	 */
	default void exitAlterAliasProperties(Cypher5Parser.AlterAliasPropertiesContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#showAliases}.
	 * @param ctx the parse tree
	 */
	default void enterShowAliases(Cypher5Parser.ShowAliasesContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#showAliases}.
	 * @param ctx the parse tree
	 */
	default void exitShowAliases(Cypher5Parser.ShowAliasesContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#symbolicNameOrStringParameter}.
	 * @param ctx the parse tree
	 */
	default void enterSymbolicNameOrStringParameter(Cypher5Parser.SymbolicNameOrStringParameterContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#symbolicNameOrStringParameter}.
	 * @param ctx the parse tree
	 */
	default void exitSymbolicNameOrStringParameter(Cypher5Parser.SymbolicNameOrStringParameterContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#commandNameExpression}.
	 * @param ctx the parse tree
	 */
	default void enterCommandNameExpression(Cypher5Parser.CommandNameExpressionContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#commandNameExpression}.
	 * @param ctx the parse tree
	 */
	default void exitCommandNameExpression(Cypher5Parser.CommandNameExpressionContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#symbolicNameOrStringParameterList}.
	 * @param ctx the parse tree
	 */
	default void enterSymbolicNameOrStringParameterList(Cypher5Parser.SymbolicNameOrStringParameterListContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#symbolicNameOrStringParameterList}.
	 * @param ctx the parse tree
	 */
	default void exitSymbolicNameOrStringParameterList(Cypher5Parser.SymbolicNameOrStringParameterListContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#symbolicAliasNameList}.
	 * @param ctx the parse tree
	 */
	default void enterSymbolicAliasNameList(Cypher5Parser.SymbolicAliasNameListContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#symbolicAliasNameList}.
	 * @param ctx the parse tree
	 */
	default void exitSymbolicAliasNameList(Cypher5Parser.SymbolicAliasNameListContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#symbolicAliasNameOrParameter}.
	 * @param ctx the parse tree
	 */
	default void enterSymbolicAliasNameOrParameter(Cypher5Parser.SymbolicAliasNameOrParameterContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#symbolicAliasNameOrParameter}.
	 * @param ctx the parse tree
	 */
	default void exitSymbolicAliasNameOrParameter(Cypher5Parser.SymbolicAliasNameOrParameterContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#symbolicAliasName}.
	 * @param ctx the parse tree
	 */
	default void enterSymbolicAliasName(Cypher5Parser.SymbolicAliasNameContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#symbolicAliasName}.
	 * @param ctx the parse tree
	 */
	default void exitSymbolicAliasName(Cypher5Parser.SymbolicAliasNameContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#stringListLiteral}.
	 * @param ctx the parse tree
	 */
	default void enterStringListLiteral(Cypher5Parser.StringListLiteralContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#stringListLiteral}.
	 * @param ctx the parse tree
	 */
	default void exitStringListLiteral(Cypher5Parser.StringListLiteralContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#stringList}.
	 * @param ctx the parse tree
	 */
	default void enterStringList(Cypher5Parser.StringListContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#stringList}.
	 * @param ctx the parse tree
	 */
	default void exitStringList(Cypher5Parser.StringListContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#stringLiteral}.
	 * @param ctx the parse tree
	 */
	default void enterStringLiteral(Cypher5Parser.StringLiteralContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#stringLiteral}.
	 * @param ctx the parse tree
	 */
	default void exitStringLiteral(Cypher5Parser.StringLiteralContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#stringOrParameterExpression}.
	 * @param ctx the parse tree
	 */
	default void enterStringOrParameterExpression(Cypher5Parser.StringOrParameterExpressionContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#stringOrParameterExpression}.
	 * @param ctx the parse tree
	 */
	default void exitStringOrParameterExpression(Cypher5Parser.StringOrParameterExpressionContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#stringOrParameter}.
	 * @param ctx the parse tree
	 */
	default void enterStringOrParameter(Cypher5Parser.StringOrParameterContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#stringOrParameter}.
	 * @param ctx the parse tree
	 */
	default void exitStringOrParameter(Cypher5Parser.StringOrParameterContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#uIntOrIntParameter}.
	 * @param ctx the parse tree
	 */
	default void enterUIntOrIntParameter(Cypher5Parser.UIntOrIntParameterContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#uIntOrIntParameter}.
	 * @param ctx the parse tree
	 */
	default void exitUIntOrIntParameter(Cypher5Parser.UIntOrIntParameterContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#mapOrParameter}.
	 * @param ctx the parse tree
	 */
	default void enterMapOrParameter(Cypher5Parser.MapOrParameterContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#mapOrParameter}.
	 * @param ctx the parse tree
	 */
	default void exitMapOrParameter(Cypher5Parser.MapOrParameterContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#map}.
	 * @param ctx the parse tree
	 */
	default void enterMap(Cypher5Parser.MapContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#map}.
	 * @param ctx the parse tree
	 */
	default void exitMap(Cypher5Parser.MapContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#symbolicVariableNameString}.
	 * @param ctx the parse tree
	 */
	default void enterSymbolicVariableNameString(Cypher5Parser.SymbolicVariableNameStringContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#symbolicVariableNameString}.
	 * @param ctx the parse tree
	 */
	default void exitSymbolicVariableNameString(Cypher5Parser.SymbolicVariableNameStringContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#escapedSymbolicVariableNameString}.
	 * @param ctx the parse tree
	 */
	default void enterEscapedSymbolicVariableNameString(Cypher5Parser.EscapedSymbolicVariableNameStringContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#escapedSymbolicVariableNameString}.
	 * @param ctx the parse tree
	 */
	default void exitEscapedSymbolicVariableNameString(Cypher5Parser.EscapedSymbolicVariableNameStringContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#unescapedSymbolicVariableNameString}.
	 * @param ctx the parse tree
	 */
	default void enterUnescapedSymbolicVariableNameString(Cypher5Parser.UnescapedSymbolicVariableNameStringContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#unescapedSymbolicVariableNameString}.
	 * @param ctx the parse tree
	 */
	default void exitUnescapedSymbolicVariableNameString(Cypher5Parser.UnescapedSymbolicVariableNameStringContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#symbolicNameString}.
	 * @param ctx the parse tree
	 */
	default void enterSymbolicNameString(Cypher5Parser.SymbolicNameStringContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#symbolicNameString}.
	 * @param ctx the parse tree
	 */
	default void exitSymbolicNameString(Cypher5Parser.SymbolicNameStringContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#escapedSymbolicNameString}.
	 * @param ctx the parse tree
	 */
	default void enterEscapedSymbolicNameString(Cypher5Parser.EscapedSymbolicNameStringContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#escapedSymbolicNameString}.
	 * @param ctx the parse tree
	 */
	default void exitEscapedSymbolicNameString(Cypher5Parser.EscapedSymbolicNameStringContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#unescapedSymbolicNameString}.
	 * @param ctx the parse tree
	 */
	default void enterUnescapedSymbolicNameString(Cypher5Parser.UnescapedSymbolicNameStringContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#unescapedSymbolicNameString}.
	 * @param ctx the parse tree
	 */
	default void exitUnescapedSymbolicNameString(Cypher5Parser.UnescapedSymbolicNameStringContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#symbolicLabelNameString}.
	 * @param ctx the parse tree
	 */
	default void enterSymbolicLabelNameString(Cypher5Parser.SymbolicLabelNameStringContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#symbolicLabelNameString}.
	 * @param ctx the parse tree
	 */
	default void exitSymbolicLabelNameString(Cypher5Parser.SymbolicLabelNameStringContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#unescapedLabelSymbolicNameString}.
	 * @param ctx the parse tree
	 */
	default void enterUnescapedLabelSymbolicNameString(Cypher5Parser.UnescapedLabelSymbolicNameStringContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#unescapedLabelSymbolicNameString}.
	 * @param ctx the parse tree
	 */
	default void exitUnescapedLabelSymbolicNameString(Cypher5Parser.UnescapedLabelSymbolicNameStringContext ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#unescapedLabelSymbolicNameString_}.
	 * @param ctx the parse tree
	 */
	default void enterUnescapedLabelSymbolicNameString_(Cypher5Parser.UnescapedLabelSymbolicNameString_Context ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#unescapedLabelSymbolicNameString_}.
	 * @param ctx the parse tree
	 */
	default void exitUnescapedLabelSymbolicNameString_(Cypher5Parser.UnescapedLabelSymbolicNameString_Context ctx) { }
	/**
	 * Enter a parse tree produced by {@link Cypher5Parser#endOfFile}.
	 * @param ctx the parse tree
	 */
	default void enterEndOfFile(Cypher5Parser.EndOfFileContext ctx) { }
	/**
	 * Exit a parse tree produced by {@link Cypher5Parser#endOfFile}.
	 * @param ctx the parse tree
	 */
	default void exitEndOfFile(Cypher5Parser.EndOfFileContext ctx) { }

	/**
	 * Compatibility rule-level callbacks used by the optimized AST builder.
	 * The grammar currently emits alternative-specific listener methods for these rules.
	 */
	default void exitSetItem(Cypher5Parser.SetItemContext ctx) { }
	default void exitRemoveItem(Cypher5Parser.RemoveItemContext ctx) { }
	default void exitSelector(Cypher5Parser.SelectorContext ctx) { }
	default void exitComparisonExpression6(Cypher5Parser.ComparisonExpression6Context ctx) { }
	default void exitPostFix(Cypher5Parser.PostFixContext ctx) { }
	default void exitExtendedWhen(Cypher5Parser.ExtendedWhenContext ctx) { }
	default void exitLabelExpression1(Cypher5Parser.LabelExpression1Context ctx) { }
	default void exitLabelExpression1Is(Cypher5Parser.LabelExpression1IsContext ctx) { }
	default void exitLiteral(Cypher5Parser.LiteralContext ctx) { }
	default void exitConstraintType(Cypher5Parser.ConstraintTypeContext ctx) { }
}
