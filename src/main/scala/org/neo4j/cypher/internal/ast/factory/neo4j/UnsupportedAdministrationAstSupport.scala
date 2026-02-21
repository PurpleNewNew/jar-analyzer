package org.neo4j.cypher.internal.ast.factory.neo4j

import org.neo4j.cypher.internal.ast.AdministrationCommand
import org.neo4j.cypher.internal.ast.DatabaseName
import org.neo4j.cypher.internal.ast.Return
import org.neo4j.cypher.internal.ast.Where
import org.neo4j.cypher.internal.ast.Yield
import org.neo4j.cypher.internal.ast.factory.ASTFactory.StringPos
import org.neo4j.cypher.internal.expressions.Expression
import org.neo4j.cypher.internal.expressions.Parameter
import org.neo4j.cypher.internal.expressions.Variable
import org.neo4j.cypher.internal.parser.common.ast.factory.AccessType
import org.neo4j.cypher.internal.parser.common.ast.factory.ActionType
import org.neo4j.cypher.internal.parser.common.ast.factory.ScopeType
import org.neo4j.cypher.internal.parser.common.ast.factory.SimpleEither
import org.neo4j.cypher.internal.util.InputPosition

import java.lang
import java.util

trait UnsupportedAdministrationAstSupport { this: Neo4jASTFactory =>
  private def unsupportedAdministration[T](feature: String = "administration command"): T =
    throw new UnsupportedOperationException(s"feature disabled: $feature")

  // Role administration
  def createRole(
    p: InputPosition,
    replace: Boolean,
    roleName: SimpleEither[StringPos[InputPosition], Parameter],
    from: SimpleEither[StringPos[InputPosition], Parameter],
    ifNotExists: Boolean,
    immutable: Boolean
  ): AdministrationCommand = unsupportedAdministration("create role")

  def dropRole(
    p: InputPosition,
    roleName: SimpleEither[StringPos[InputPosition], Parameter],
    ifExists: Boolean
  ): AdministrationCommand = unsupportedAdministration("drop role")

  def renameRole(
    p: InputPosition,
    fromRoleName: SimpleEither[StringPos[InputPosition], Parameter],
    toRoleName: SimpleEither[StringPos[InputPosition], Parameter],
    ifExists: Boolean
  ): AdministrationCommand = unsupportedAdministration("rename role")

  def showRoles(
    p: InputPosition,
    WithUsers: Boolean,
    showAll: Boolean,
    yieldExpr: Yield,
    returnWithoutGraph: Return,
    where: Where
  ): AdministrationCommand = unsupportedAdministration("show roles")

  def grantRoles(
    p: InputPosition,
    roles: util.List[SimpleEither[StringPos[InputPosition], Parameter]],
    users: util.List[SimpleEither[StringPos[InputPosition], Parameter]]
  ): AdministrationCommand = unsupportedAdministration("grant roles")

  def revokeRoles(
    p: InputPosition,
    roles: util.List[SimpleEither[StringPos[InputPosition], Parameter]],
    users: util.List[SimpleEither[StringPos[InputPosition], Parameter]]
  ): AdministrationCommand = unsupportedAdministration("revoke roles")

  // User administration
  def createUser(
    p: InputPosition,
    replace: Boolean,
    ifNotExists: Boolean,
    username: SimpleEither[StringPos[InputPosition], Parameter],
    suspended: lang.Boolean,
    homeDatabase: DatabaseName,
    auths: util.List[AnyRef],
    nativeAuthAttributes: util.List[AnyRef]
  ): AdministrationCommand = unsupportedAdministration("create user")

  def dropUser(
    p: InputPosition,
    ifExists: Boolean,
    username: SimpleEither[StringPos[InputPosition], Parameter]
  ): AdministrationCommand = unsupportedAdministration("drop user")

  def renameUser(
    p: InputPosition,
    fromUserName: SimpleEither[StringPos[InputPosition], Parameter],
    toUserName: SimpleEither[StringPos[InputPosition], Parameter],
    ifExists: Boolean
  ): AdministrationCommand = unsupportedAdministration("rename user")

  def setOwnPassword(
    p: InputPosition,
    currentPassword: Expression,
    newPassword: Expression
  ): AdministrationCommand = unsupportedAdministration("set own password")

  def alterUser(
    p: InputPosition,
    ifExists: Boolean,
    username: SimpleEither[StringPos[InputPosition], Parameter],
    suspended: lang.Boolean,
    homeDatabase: DatabaseName,
    removeHome: Boolean,
    auths: util.List[AnyRef],
    nativeAuthAttributes: util.List[AnyRef],
    removeAllAuth: Boolean,
    removeAuth: util.List[Expression]
  ): AdministrationCommand = unsupportedAdministration("alter user")

  def auth(provider: String, attributes: util.List[AnyRef], p: InputPosition): AnyRef =
    unsupportedAdministration("user auth")

  def authId(p: InputPosition, id: Expression): AnyRef = unsupportedAdministration("user auth")

  def password(p: InputPosition, password: Expression, encrypted: Boolean): AnyRef =
    unsupportedAdministration("user auth")

  def passwordChangeRequired(p: InputPosition, changeRequired: Boolean): AnyRef =
    unsupportedAdministration("user auth")

  def passwordExpression(password: Parameter): Expression =
    unsupportedAdministration("user auth")

  def passwordExpression(s: InputPosition, e: InputPosition, password: String): Expression =
    unsupportedAdministration("user auth")

  def showUsers(
    p: InputPosition,
    yieldExpr: Yield,
    returnWithoutGraph: Return,
    where: Where,
    withAuth: Boolean
  ): AdministrationCommand = unsupportedAdministration("show users")

  def showCurrentUser(
    p: InputPosition,
    yieldExpr: Yield,
    returnWithoutGraph: Return,
    where: Where
  ): AdministrationCommand = unsupportedAdministration("show current user")

  // Privilege administration
  def showSupportedPrivileges(
    p: InputPosition,
    yieldExpr: Yield,
    returnWithoutGraph: Return,
    where: Where
  ): AdministrationCommand = unsupportedAdministration("show privileges")

  def showAllPrivileges(
    p: InputPosition,
    asCommand: Boolean,
    asRevoke: Boolean,
    yieldExpr: Yield,
    returnWithoutGraph: Return,
    where: Where
  ): AdministrationCommand = unsupportedAdministration("show privileges")

  def showRolePrivileges(
    p: InputPosition,
    roles: util.List[SimpleEither[StringPos[InputPosition], Parameter]],
    asCommand: Boolean,
    asRevoke: Boolean,
    yieldExpr: Yield,
    returnWithoutGraph: Return,
    where: Where
  ): AdministrationCommand = unsupportedAdministration("show role privileges")

  def showUserPrivileges(
    p: InputPosition,
    users: util.List[SimpleEither[StringPos[InputPosition], Parameter]],
    asCommand: Boolean,
    asRevoke: Boolean,
    yieldExpr: Yield,
    returnWithoutGraph: Return,
    where: Where
  ): AdministrationCommand = unsupportedAdministration("show user privileges")

  def grantPrivilege(
    p: InputPosition,
    roles: util.List[SimpleEither[StringPos[InputPosition], Parameter]],
    privilege: AnyRef
  ): AdministrationCommand = unsupportedAdministration("grant privilege")

  def denyPrivilege(
    p: InputPosition,
    roles: util.List[SimpleEither[StringPos[InputPosition], Parameter]],
    privilege: AnyRef
  ): AdministrationCommand = unsupportedAdministration("deny privilege")

  def revokePrivilege(
    p: InputPosition,
    roles: util.List[SimpleEither[StringPos[InputPosition], Parameter]],
    privilege: AnyRef,
    revokeGrant: Boolean,
    revokeDeny: Boolean
  ): AdministrationCommand = unsupportedAdministration("revoke privilege")

  def databasePrivilege(
    p: InputPosition,
    action: AnyRef,
    scope: AnyRef,
    qualifier: util.List[AnyRef],
    immutable: Boolean
  ): AnyRef = unsupportedAdministration("database privilege")

  def dbmsPrivilege(
    p: InputPosition,
    action: AnyRef,
    qualifier: util.List[AnyRef],
    immutable: Boolean
  ): AnyRef = unsupportedAdministration("dbms privilege")

  def graphPrivilege(
    p: InputPosition,
    action: AnyRef,
    scope: AnyRef,
    resource: AnyRef,
    qualifier: util.List[AnyRef],
    immutable: Boolean
  ): AnyRef = unsupportedAdministration("graph privilege")

  def loadPrivilege(
    p: InputPosition,
    url: SimpleEither[String, Parameter],
    cidr: SimpleEither[String, Parameter],
    immutable: Boolean
  ): AnyRef = unsupportedAdministration("load privilege")

  def privilegeAction(action: ActionType): AnyRef =
    unsupportedAdministration("privilege action")

  def propertiesResource(p: InputPosition, properties: util.List[String]): AnyRef =
    unsupportedAdministration("privilege resource")

  def allPropertiesResource(p: InputPosition): AnyRef =
    unsupportedAdministration("privilege resource")

  def labelsResource(p: InputPosition, labels: util.List[String]): AnyRef =
    unsupportedAdministration("privilege resource")

  def allLabelsResource(p: InputPosition): AnyRef =
    unsupportedAdministration("privilege resource")

  def databaseResource(p: InputPosition): AnyRef =
    unsupportedAdministration("privilege resource")

  def noResource(p: InputPosition): AnyRef =
    unsupportedAdministration("privilege resource")

  def labelQualifier(p: InputPosition, label: String): AnyRef =
    unsupportedAdministration("privilege qualifier")

  def allLabelsQualifier(p: InputPosition): AnyRef =
    unsupportedAdministration("privilege qualifier")

  def relationshipQualifier(p: InputPosition, relationshipType: String): AnyRef =
    unsupportedAdministration("privilege qualifier")

  def allRelationshipsQualifier(p: InputPosition): AnyRef =
    unsupportedAdministration("privilege qualifier")

  def elementQualifier(p: InputPosition, name: String): AnyRef =
    unsupportedAdministration("privilege qualifier")

  def allElementsQualifier(p: InputPosition): AnyRef =
    unsupportedAdministration("privilege qualifier")

  def patternQualifier(
    qualifiers: util.List[AnyRef],
    variable: Variable,
    expression: Expression
  ): AnyRef = unsupportedAdministration("privilege qualifier")

  def allQualifier(): util.List[AnyRef] =
    unsupportedAdministration("privilege qualifier")

  def allDatabasesQualifier(): util.List[AnyRef] =
    unsupportedAdministration("privilege qualifier")

  def userQualifier(users: util.List[SimpleEither[StringPos[InputPosition], Parameter]])
    : util.List[AnyRef] =
    unsupportedAdministration("privilege qualifier")

  def allUsersQualifier(): util.List[AnyRef] =
    unsupportedAdministration("privilege qualifier")

  def functionQualifier(p: InputPosition, functions: util.List[String]): util.List[AnyRef] =
    unsupportedAdministration("privilege qualifier")

  def procedureQualifier(p: InputPosition, procedures: util.List[String]): util.List[AnyRef] =
    unsupportedAdministration("privilege qualifier")

  def settingQualifier(p: InputPosition, names: util.List[String]): util.List[AnyRef] =
    unsupportedAdministration("privilege qualifier")

  def graphScope(
    p: InputPosition,
    graphNames: util.List[DatabaseName],
    scopeType: ScopeType
  ): AnyRef = unsupportedAdministration("graph scope")

  def databasePrivilegeScope(
    p: InputPosition,
    databaseNames: util.List[DatabaseName],
    scopeType: ScopeType
  ): AnyRef = unsupportedAdministration("database scope")

  // Server administration
  def enableServer(
    p: InputPosition,
    serverName: SimpleEither[String, Parameter],
    options: SimpleEither[util.Map[String, Expression], Parameter]
  ): AdministrationCommand = unsupportedAdministration("server command")

  def alterServer(
    p: InputPosition,
    serverName: SimpleEither[String, Parameter],
    options: SimpleEither[util.Map[String, Expression], Parameter]
  ): AdministrationCommand = unsupportedAdministration("server command")

  def renameServer(
    p: InputPosition,
    serverName: SimpleEither[String, Parameter],
    newName: SimpleEither[String, Parameter]
  ): AdministrationCommand = unsupportedAdministration("server command")

  def dropServer(p: InputPosition, serverName: SimpleEither[String, Parameter]): AdministrationCommand =
    unsupportedAdministration("server command")

  def showServers(
    p: InputPosition,
    yieldExpr: Yield,
    returnWithoutGraph: Return,
    where: Where
  ): AdministrationCommand = unsupportedAdministration("server command")

  def deallocateServers(
    p: InputPosition,
    dryRun: Boolean,
    serverNames: util.List[SimpleEither[String, Parameter]]
  ): AdministrationCommand = unsupportedAdministration("server command")

  def reallocateDatabases(p: InputPosition, dryRun: Boolean): AdministrationCommand =
    unsupportedAdministration("server command")

  // Database administration
  def createDatabase(
    p: InputPosition,
    replace: Boolean,
    databaseName: DatabaseName,
    ifNotExists: Boolean,
    wait: AnyRef,
    options: SimpleEither[util.Map[String, Expression], Parameter],
    topologyPrimaries: SimpleEither[Integer, Parameter],
    topologySecondaries: SimpleEither[Integer, Parameter]
  ): AdministrationCommand = unsupportedAdministration("database command")

  def createCompositeDatabase(
    p: InputPosition,
    replace: Boolean,
    compositeDatabaseName: DatabaseName,
    ifNotExists: Boolean,
    options: SimpleEither[util.Map[String, Expression], Parameter],
    wait: AnyRef
  ): AdministrationCommand = unsupportedAdministration("database command")

  def dropDatabase(
    p: InputPosition,
    databaseName: DatabaseName,
    ifExists: Boolean,
    composite: Boolean,
    javaAliasAction: Boolean,
    dumpData: Boolean,
    wait: AnyRef
  ): AdministrationCommand = unsupportedAdministration("database command")

  def alterDatabase(
    p: InputPosition,
    databaseName: DatabaseName,
    ifExists: Boolean,
    accessType: AccessType,
    topologyPrimaries: SimpleEither[Integer, Parameter],
    topologySecondaries: SimpleEither[Integer, Parameter],
    options: util.Map[String, Expression],
    optionsToRemove: util.Set[String],
    waitClause: AnyRef
  ): AdministrationCommand = unsupportedAdministration("database command")

  def showDatabase(
    p: InputPosition,
    scope: AnyRef,
    yieldExpr: Yield,
    returnWithoutGraph: Return,
    where: Where
  ): AdministrationCommand = unsupportedAdministration("database command")

  def databaseScope(
    p: InputPosition,
    databaseName: DatabaseName,
    isDefault: Boolean,
    isHome: Boolean
  ): AnyRef = unsupportedAdministration("database command")

  def startDatabase(
    p: InputPosition,
    databaseName: DatabaseName,
    wait: AnyRef
  ): AdministrationCommand = unsupportedAdministration("database command")

  def stopDatabase(
    p: InputPosition,
    databaseName: DatabaseName,
    wait: AnyRef
  ): AdministrationCommand = unsupportedAdministration("database command")

  def wait(wait: Boolean, seconds: Long): AnyRef =
    unsupportedAdministration("database command")

  def databaseName(p: InputPosition, names: util.List[String]): DatabaseName =
    unsupportedAdministration("database command")

  def databaseName(param: Parameter): DatabaseName =
    unsupportedAdministration("database command")

  // Alias administration
  def createLocalDatabaseAlias(
    p: InputPosition,
    replace: Boolean,
    aliasName: DatabaseName,
    targetName: DatabaseName,
    ifNotExists: Boolean,
    properties: SimpleEither[util.Map[String, Expression], Parameter]
  ): AdministrationCommand = unsupportedAdministration("alias command")

  def createRemoteDatabaseAlias(
    p: InputPosition,
    replace: Boolean,
    aliasName: DatabaseName,
    targetName: DatabaseName,
    ifNotExists: Boolean,
    url: SimpleEither[String, Parameter],
    username: SimpleEither[StringPos[InputPosition], Parameter],
    password: Expression,
    driverSettings: SimpleEither[util.Map[String, Expression], Parameter],
    properties: SimpleEither[util.Map[String, Expression], Parameter]
  ): AdministrationCommand = unsupportedAdministration("alias command")

  def alterLocalDatabaseAlias(
    p: InputPosition,
    aliasName: DatabaseName,
    targetName: DatabaseName,
    ifExists: Boolean,
    properties: SimpleEither[util.Map[String, Expression], Parameter]
  ): AdministrationCommand = unsupportedAdministration("alias command")

  def alterRemoteDatabaseAlias(
    p: InputPosition,
    aliasName: DatabaseName,
    targetName: DatabaseName,
    ifExists: Boolean,
    url: SimpleEither[String, Parameter],
    username: SimpleEither[StringPos[InputPosition], Parameter],
    password: Expression,
    driverSettings: SimpleEither[util.Map[String, Expression], Parameter],
    properties: SimpleEither[util.Map[String, Expression], Parameter]
  ): AdministrationCommand = unsupportedAdministration("alias command")

  def dropAlias(
    p: InputPosition,
    aliasName: DatabaseName,
    ifExists: Boolean
  ): AdministrationCommand = unsupportedAdministration("alias command")

  def showAliases(
    p: InputPosition,
    aliasName: DatabaseName,
    yieldExpr: Yield,
    returnWithoutGraph: Return,
    where: Where
  ): AdministrationCommand = unsupportedAdministration("alias command")
}
