/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [https://neo4j.com]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neo4j.cypher.internal.parser.v5.ast.factory

import org.antlr.v4.runtime.ParserRuleContext
import org.neo4j.cypher.internal.parser.v5.Cypher5Parser
import org.neo4j.cypher.internal.parser.v5.Cypher5ParserListener

/**
 * No-op listener methods kept after command/DDL parser-path pruning.
 * These methods are intentionally empty because command clauses are disabled by syntax checker.
 */
trait Cypher5DisabledCommandNoOpBuilder extends Cypher5ParserListener {
  private def commandSyntaxDisabled(ctx: ParserRuleContext): Unit = {
    throw new UnsupportedOperationException(
      s"feature disabled: command syntax (${Cypher5Parser.ruleNames(ctx.getRuleIndex)})"
    )
  }

  override def exitAscToken(ctx: Cypher5Parser.AscTokenContext): Unit = {}
  override def exitDescToken(ctx: Cypher5Parser.DescTokenContext): Unit = {}
  override def exitGroupToken(ctx: Cypher5Parser.GroupTokenContext): Unit = {}
  override def exitPathToken(ctx: Cypher5Parser.PathTokenContext): Unit = {}
  override def exitCommand(ctx: Cypher5Parser.CommandContext): Unit = commandSyntaxDisabled(ctx)
  override def exitCreateCommand(ctx: Cypher5Parser.CreateCommandContext): Unit = {}
  override def exitDropCommand(ctx: Cypher5Parser.DropCommandContext): Unit = {}
  override def exitShowCommand(ctx: Cypher5Parser.ShowCommandContext): Unit = {}
  override def exitShowCommandYield(ctx: Cypher5Parser.ShowCommandYieldContext): Unit = {}
  override def exitYieldItem(ctx: Cypher5Parser.YieldItemContext): Unit = {}
  override def exitYieldSkip(ctx: Cypher5Parser.YieldSkipContext): Unit = {}
  override def exitYieldLimit(ctx: Cypher5Parser.YieldLimitContext): Unit = {}
  override def exitYieldClause(ctx: Cypher5Parser.YieldClauseContext): Unit = {}
  override def exitCommandOptions(ctx: Cypher5Parser.CommandOptionsContext): Unit = {}
  override def exitTerminateCommand(ctx: Cypher5Parser.TerminateCommandContext): Unit = {}
  override def exitComposableCommandClauses(ctx: Cypher5Parser.ComposableCommandClausesContext): Unit =
    commandSyntaxDisabled(ctx)
  override def exitComposableShowCommandClauses(ctx: Cypher5Parser.ComposableShowCommandClausesContext): Unit =
    commandSyntaxDisabled(ctx)
  override def exitShowBriefAndYield(ctx: Cypher5Parser.ShowBriefAndYieldContext): Unit = {}
  override def exitShowIndexCommand(ctx: Cypher5Parser.ShowIndexCommandContext): Unit = {}
  override def exitShowIndexesAllowBrief(ctx: Cypher5Parser.ShowIndexesAllowBriefContext): Unit = {}
  override def exitShowIndexesNoBrief(ctx: Cypher5Parser.ShowIndexesNoBriefContext): Unit = {}
  override def exitShowConstraintCommand(ctx: Cypher5Parser.ShowConstraintCommandContext): Unit = {}
  override def exitConstraintAllowYieldType(ctx: Cypher5Parser.ConstraintAllowYieldTypeContext): Unit = {}
  override def exitConstraintExistType(ctx: Cypher5Parser.ConstraintExistTypeContext): Unit = {}
  override def exitConstraintBriefAndYieldType(ctx: Cypher5Parser.ConstraintBriefAndYieldTypeContext): Unit = {}
  override def exitShowConstraintsAllowBriefAndYield(ctx: Cypher5Parser.ShowConstraintsAllowBriefAndYieldContext): Unit = {}
  override def exitShowConstraintsAllowBrief(ctx: Cypher5Parser.ShowConstraintsAllowBriefContext): Unit = {}
  override def exitShowConstraintsAllowYield(ctx: Cypher5Parser.ShowConstraintsAllowYieldContext): Unit = {}
  override def exitShowProcedures(ctx: Cypher5Parser.ShowProceduresContext): Unit = {}
  override def exitShowFunctions(ctx: Cypher5Parser.ShowFunctionsContext): Unit = {}
  override def exitFunctionToken(ctx: Cypher5Parser.FunctionTokenContext): Unit = {}
  override def exitExecutableBy(ctx: Cypher5Parser.ExecutableByContext): Unit = {}
  override def exitShowFunctionsType(ctx: Cypher5Parser.ShowFunctionsTypeContext): Unit = {}
  override def exitShowTransactions(ctx: Cypher5Parser.ShowTransactionsContext): Unit = {}
  override def exitTerminateTransactions(ctx: Cypher5Parser.TerminateTransactionsContext): Unit = {}
  override def exitShowSettings(ctx: Cypher5Parser.ShowSettingsContext): Unit = {}
  override def exitSettingToken(ctx: Cypher5Parser.SettingTokenContext): Unit = {}
  override def exitNamesAndClauses(ctx: Cypher5Parser.NamesAndClausesContext): Unit = commandSyntaxDisabled(ctx)
  override def exitStringsOrExpression(ctx: Cypher5Parser.StringsOrExpressionContext): Unit = commandSyntaxDisabled(ctx)
  override def exitCommandNodePattern(ctx: Cypher5Parser.CommandNodePatternContext): Unit = commandSyntaxDisabled(ctx)
  override def exitCommandRelPattern(ctx: Cypher5Parser.CommandRelPatternContext): Unit = commandSyntaxDisabled(ctx)
  override def exitCreateConstraint(ctx: Cypher5Parser.CreateConstraintContext): Unit = {}
  override def exitConstraintType(ctx: Cypher5Parser.ConstraintTypeContext): Unit = {}
  override def exitDropConstraint(ctx: Cypher5Parser.DropConstraintContext): Unit = {}
  override def exitCreateIndex(ctx: Cypher5Parser.CreateIndexContext): Unit = {}
  override def exitOldCreateIndex(ctx: Cypher5Parser.OldCreateIndexContext): Unit = {}
  override def exitCreateIndex_(ctx: Cypher5Parser.CreateIndex_Context): Unit = {}
  override def exitCreateFulltextIndex(ctx: Cypher5Parser.CreateFulltextIndexContext): Unit = {}
  override def exitFulltextNodePattern(ctx: Cypher5Parser.FulltextNodePatternContext): Unit = {}
  override def exitFulltextRelPattern(ctx: Cypher5Parser.FulltextRelPatternContext): Unit = {}
  override def exitCreateLookupIndex(ctx: Cypher5Parser.CreateLookupIndexContext): Unit = {}
  override def exitLookupIndexNodePattern(ctx: Cypher5Parser.LookupIndexNodePatternContext): Unit = {}
  override def exitLookupIndexRelPattern(ctx: Cypher5Parser.LookupIndexRelPatternContext): Unit = {}
  override def exitDropIndex(ctx: Cypher5Parser.DropIndexContext): Unit = {}
  override def exitPropertyList(ctx: Cypher5Parser.PropertyListContext): Unit = {}
  override def exitEnclosedPropertyList(ctx: Cypher5Parser.EnclosedPropertyListContext): Unit = {}
  override def exitAlterCommand(ctx: Cypher5Parser.AlterCommandContext): Unit = {}
  override def exitRenameCommand(ctx: Cypher5Parser.RenameCommandContext): Unit = {}
  override def exitGrantCommand(ctx: Cypher5Parser.GrantCommandContext): Unit = {}
  override def exitDenyCommand(ctx: Cypher5Parser.DenyCommandContext): Unit = {}
  override def exitRevokeCommand(ctx: Cypher5Parser.RevokeCommandContext): Unit = {}
  override def exitUserNames(ctx: Cypher5Parser.UserNamesContext): Unit = {}
  override def exitRoleNames(ctx: Cypher5Parser.RoleNamesContext): Unit = {}
  override def exitRoleToken(ctx: Cypher5Parser.RoleTokenContext): Unit = {}
  override def exitEnableServerCommand(ctx: Cypher5Parser.EnableServerCommandContext): Unit = {}
  override def exitAlterServer(ctx: Cypher5Parser.AlterServerContext): Unit = {}
  override def exitRenameServer(ctx: Cypher5Parser.RenameServerContext): Unit = {}
  override def exitDropServer(ctx: Cypher5Parser.DropServerContext): Unit = {}
  override def exitShowServers(ctx: Cypher5Parser.ShowServersContext): Unit = {}
  override def exitAllocationCommand(ctx: Cypher5Parser.AllocationCommandContext): Unit = {}
  override def exitDeallocateDatabaseFromServers(ctx: Cypher5Parser.DeallocateDatabaseFromServersContext): Unit = {}
  override def exitReallocateDatabases(ctx: Cypher5Parser.ReallocateDatabasesContext): Unit = {}
  override def exitCreateRole(ctx: Cypher5Parser.CreateRoleContext): Unit = {}
  override def exitDropRole(ctx: Cypher5Parser.DropRoleContext): Unit = {}
  override def exitRenameRole(ctx: Cypher5Parser.RenameRoleContext): Unit = {}
  override def exitShowRoles(ctx: Cypher5Parser.ShowRolesContext): Unit = {}
  override def exitGrantRole(ctx: Cypher5Parser.GrantRoleContext): Unit = {}
  override def exitRevokeRole(ctx: Cypher5Parser.RevokeRoleContext): Unit = {}
  override def exitCreateUser(ctx: Cypher5Parser.CreateUserContext): Unit = {}
  override def exitDropUser(ctx: Cypher5Parser.DropUserContext): Unit = {}
  override def exitRenameUser(ctx: Cypher5Parser.RenameUserContext): Unit = {}
  override def exitAlterCurrentUser(ctx: Cypher5Parser.AlterCurrentUserContext): Unit = {}
  override def exitAlterUser(ctx: Cypher5Parser.AlterUserContext): Unit = {}
  override def exitRemoveNamedProvider(ctx: Cypher5Parser.RemoveNamedProviderContext): Unit = {}
  override def exitPassword(ctx: Cypher5Parser.PasswordContext): Unit = {}
  override def exitPasswordOnly(ctx: Cypher5Parser.PasswordOnlyContext): Unit = {}
  override def exitPasswordExpression(ctx: Cypher5Parser.PasswordExpressionContext): Unit = {}
  override def exitPasswordChangeRequired(ctx: Cypher5Parser.PasswordChangeRequiredContext): Unit = {}
  override def exitUserStatus(ctx: Cypher5Parser.UserStatusContext): Unit = {}
  override def exitHomeDatabase(ctx: Cypher5Parser.HomeDatabaseContext): Unit = {}
  override def exitSetAuthClause(ctx: Cypher5Parser.SetAuthClauseContext): Unit = {}
  override def exitUserAuthAttribute(ctx: Cypher5Parser.UserAuthAttributeContext): Unit = {}
  override def exitShowUsers(ctx: Cypher5Parser.ShowUsersContext): Unit = {}
  override def exitShowCurrentUser(ctx: Cypher5Parser.ShowCurrentUserContext): Unit = {}
  override def exitShowSupportedPrivileges(ctx: Cypher5Parser.ShowSupportedPrivilegesContext): Unit = {}
  override def exitShowPrivileges(ctx: Cypher5Parser.ShowPrivilegesContext): Unit = {}
  override def exitShowRolePrivileges(ctx: Cypher5Parser.ShowRolePrivilegesContext): Unit = {}
  override def exitShowUserPrivileges(ctx: Cypher5Parser.ShowUserPrivilegesContext): Unit = {}
  override def exitPrivilegeAsCommand(ctx: Cypher5Parser.PrivilegeAsCommandContext): Unit = {}
  override def exitPrivilegeToken(ctx: Cypher5Parser.PrivilegeTokenContext): Unit = {}
  override def exitPrivilege(ctx: Cypher5Parser.PrivilegeContext): Unit = {}
  override def exitAllPrivilege(ctx: Cypher5Parser.AllPrivilegeContext): Unit = {}
  override def exitAllPrivilegeType(ctx: Cypher5Parser.AllPrivilegeTypeContext): Unit = {}
  override def exitAllPrivilegeTarget(ctx: Cypher5Parser.AllPrivilegeTargetContext): Unit = {}
  override def exitCreatePrivilege(ctx: Cypher5Parser.CreatePrivilegeContext): Unit = {}
  override def exitCreatePrivilegeForDatabase(ctx: Cypher5Parser.CreatePrivilegeForDatabaseContext): Unit = {}
  override def exitCreateNodePrivilegeToken(ctx: Cypher5Parser.CreateNodePrivilegeTokenContext): Unit = {}
  override def exitCreateRelPrivilegeToken(ctx: Cypher5Parser.CreateRelPrivilegeTokenContext): Unit = {}
  override def exitCreatePropertyPrivilegeToken(ctx: Cypher5Parser.CreatePropertyPrivilegeTokenContext): Unit = {}
  override def exitActionForDBMS(ctx: Cypher5Parser.ActionForDBMSContext): Unit = {}
  override def exitDropPrivilege(ctx: Cypher5Parser.DropPrivilegeContext): Unit = {}
  override def exitLoadPrivilege(ctx: Cypher5Parser.LoadPrivilegeContext): Unit = {}
  override def exitShowPrivilege(ctx: Cypher5Parser.ShowPrivilegeContext): Unit = {}
  override def exitSetPrivilege(ctx: Cypher5Parser.SetPrivilegeContext): Unit = {}
  override def exitPasswordToken(ctx: Cypher5Parser.PasswordTokenContext): Unit = {}
  override def exitRemovePrivilege(ctx: Cypher5Parser.RemovePrivilegeContext): Unit = {}
  override def exitWritePrivilege(ctx: Cypher5Parser.WritePrivilegeContext): Unit = {}
  override def exitDatabasePrivilege(ctx: Cypher5Parser.DatabasePrivilegeContext): Unit = {}
  override def exitDbmsPrivilege(ctx: Cypher5Parser.DbmsPrivilegeContext): Unit = {}
  override def exitDbmsPrivilegeExecute(ctx: Cypher5Parser.DbmsPrivilegeExecuteContext): Unit = {}
  override def exitAdminToken(ctx: Cypher5Parser.AdminTokenContext): Unit = {}
  override def exitProcedureToken(ctx: Cypher5Parser.ProcedureTokenContext): Unit = {}
  override def exitIndexToken(ctx: Cypher5Parser.IndexTokenContext): Unit = {}
  override def exitConstraintToken(ctx: Cypher5Parser.ConstraintTokenContext): Unit = {}
  override def exitTransactionToken(ctx: Cypher5Parser.TransactionTokenContext): Unit = {}
  override def exitUserQualifier(ctx: Cypher5Parser.UserQualifierContext): Unit = {}
  override def exitExecuteFunctionQualifier(ctx: Cypher5Parser.ExecuteFunctionQualifierContext): Unit = {}
  override def exitExecuteProcedureQualifier(ctx: Cypher5Parser.ExecuteProcedureQualifierContext): Unit = {}
  override def exitSettingQualifier(ctx: Cypher5Parser.SettingQualifierContext): Unit = {}
  override def exitGlobs(ctx: Cypher5Parser.GlobsContext): Unit = {}
  override def exitGlob(ctx: Cypher5Parser.GlobContext): Unit = {}
  override def exitGlobRecursive(ctx: Cypher5Parser.GlobRecursiveContext): Unit = {}
  override def exitGlobPart(ctx: Cypher5Parser.GlobPartContext): Unit = {}
  override def exitQualifiedGraphPrivilegesWithProperty(ctx: Cypher5Parser.QualifiedGraphPrivilegesWithPropertyContext): Unit = {}
  override def exitQualifiedGraphPrivileges(ctx: Cypher5Parser.QualifiedGraphPrivilegesContext): Unit = {}
  override def exitLabelsResource(ctx: Cypher5Parser.LabelsResourceContext): Unit = {}
  override def exitPropertiesResource(ctx: Cypher5Parser.PropertiesResourceContext): Unit = {}
  override def exitNonEmptyStringList(ctx: Cypher5Parser.NonEmptyStringListContext): Unit = {}
  override def exitGraphQualifier(ctx: Cypher5Parser.GraphQualifierContext): Unit = {}
  override def exitGraphQualifierToken(ctx: Cypher5Parser.GraphQualifierTokenContext): Unit = {}
  override def exitRelToken(ctx: Cypher5Parser.RelTokenContext): Unit = {}
  override def exitElementToken(ctx: Cypher5Parser.ElementTokenContext): Unit = {}
  override def exitNodeToken(ctx: Cypher5Parser.NodeTokenContext): Unit = {}
  override def exitDatabaseScope(ctx: Cypher5Parser.DatabaseScopeContext): Unit = {}
  override def exitGraphScope(ctx: Cypher5Parser.GraphScopeContext): Unit = {}
  override def exitCreateCompositeDatabase(ctx: Cypher5Parser.CreateCompositeDatabaseContext): Unit = {}
  override def exitCreateDatabase(ctx: Cypher5Parser.CreateDatabaseContext): Unit = {}
  override def exitPrimaryTopology(ctx: Cypher5Parser.PrimaryTopologyContext): Unit = {}
  override def exitPrimaryToken(ctx: Cypher5Parser.PrimaryTokenContext): Unit = {}
  override def exitSecondaryTopology(ctx: Cypher5Parser.SecondaryTopologyContext): Unit = {}
  override def exitSecondaryToken(ctx: Cypher5Parser.SecondaryTokenContext): Unit = {}
  override def exitDropDatabase(ctx: Cypher5Parser.DropDatabaseContext): Unit = {}
  override def exitAliasAction(ctx: Cypher5Parser.AliasActionContext): Unit = {}
  override def exitAlterDatabase(ctx: Cypher5Parser.AlterDatabaseContext): Unit = {}
  override def exitAlterDatabaseAccess(ctx: Cypher5Parser.AlterDatabaseAccessContext): Unit = {}
  override def exitAlterDatabaseTopology(ctx: Cypher5Parser.AlterDatabaseTopologyContext): Unit = {}
  override def exitAlterDatabaseOption(ctx: Cypher5Parser.AlterDatabaseOptionContext): Unit = {}
  override def exitStartDatabase(ctx: Cypher5Parser.StartDatabaseContext): Unit = {}
  override def exitStopDatabase(ctx: Cypher5Parser.StopDatabaseContext): Unit = {}
  override def exitWaitClause(ctx: Cypher5Parser.WaitClauseContext): Unit = {}
  override def exitSecondsToken(ctx: Cypher5Parser.SecondsTokenContext): Unit = {}
  override def exitShowDatabase(ctx: Cypher5Parser.ShowDatabaseContext): Unit = {}
  override def exitAliasName(ctx: Cypher5Parser.AliasNameContext): Unit = {}
  override def exitDatabaseName(ctx: Cypher5Parser.DatabaseNameContext): Unit = {}
  override def exitCreateAlias(ctx: Cypher5Parser.CreateAliasContext): Unit = {}
  override def exitDropAlias(ctx: Cypher5Parser.DropAliasContext): Unit = {}
  override def exitAlterAlias(ctx: Cypher5Parser.AlterAliasContext): Unit = {}
  override def exitAlterAliasTarget(ctx: Cypher5Parser.AlterAliasTargetContext): Unit = {}
  override def exitAlterAliasUser(ctx: Cypher5Parser.AlterAliasUserContext): Unit = {}
  override def exitAlterAliasPassword(ctx: Cypher5Parser.AlterAliasPasswordContext): Unit = {}
  override def exitAlterAliasDriver(ctx: Cypher5Parser.AlterAliasDriverContext): Unit = {}
  override def exitAlterAliasProperties(ctx: Cypher5Parser.AlterAliasPropertiesContext): Unit = {}
  override def exitShowAliases(ctx: Cypher5Parser.ShowAliasesContext): Unit = {}
  override def exitSymbolicNameOrStringParameter(ctx: Cypher5Parser.SymbolicNameOrStringParameterContext): Unit = {}
  override def exitCommandNameExpression(ctx: Cypher5Parser.CommandNameExpressionContext): Unit =
    commandSyntaxDisabled(ctx)
  override def exitSymbolicNameOrStringParameterList(ctx: Cypher5Parser.SymbolicNameOrStringParameterListContext): Unit = {}
  override def exitSymbolicAliasNameList(ctx: Cypher5Parser.SymbolicAliasNameListContext): Unit = {}
  override def exitSymbolicAliasNameOrParameter(ctx: Cypher5Parser.SymbolicAliasNameOrParameterContext): Unit = {}
  override def exitStringList(ctx: Cypher5Parser.StringListContext): Unit = {}
  override def exitStringOrParameterExpression(ctx: Cypher5Parser.StringOrParameterExpressionContext): Unit = {}
  override def exitStringOrParameter(ctx: Cypher5Parser.StringOrParameterContext): Unit = {}
  override def exitUIntOrIntParameter(ctx: Cypher5Parser.UIntOrIntParameterContext): Unit = {}
  override def exitMapOrParameter(ctx: Cypher5Parser.MapOrParameterContext): Unit = {}
}
