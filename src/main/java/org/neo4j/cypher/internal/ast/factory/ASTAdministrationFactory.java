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
package org.neo4j.cypher.internal.ast.factory;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.neo4j.cypher.internal.parser.common.ast.factory.AccessType;
import org.neo4j.cypher.internal.parser.common.ast.factory.ActionType;
import org.neo4j.cypher.internal.parser.common.ast.factory.ScopeType;
import org.neo4j.cypher.internal.parser.common.ast.factory.SimpleEither;

/**
 * Administration command construction surface, separated from query/core AST methods.
 */
public interface ASTAdministrationFactory<
        ADMINISTRATION_COMMAND,
        RETURN_CLAUSE,
        YIELD,
        WHERE,
        DATABASE_SCOPE,
        WAIT_CLAUSE,
        ADMINISTRATION_ACTION,
        GRAPH_SCOPE,
        PRIVILEGE_TYPE,
        PRIVILEGE_RESOURCE,
        PRIVILEGE_QUALIFIER,
        AUTH,
        AUTH_ATTRIBUTE,
        EXPRESSION,
        PARAMETER,
        VARIABLE,
        POS,
        DATABASE_NAME> {

    // Role Administration Commands

    ADMINISTRATION_COMMAND createRole(
            POS p,
            boolean replace,
            SimpleEither<ASTFactory.StringPos<POS>, PARAMETER> roleName,
            SimpleEither<ASTFactory.StringPos<POS>, PARAMETER> fromRole,
            boolean ifNotExists,
            boolean immutable);

    ADMINISTRATION_COMMAND dropRole(
            POS p, SimpleEither<ASTFactory.StringPos<POS>, PARAMETER> roleName, boolean ifExists);

    ADMINISTRATION_COMMAND renameRole(
            POS p,
            SimpleEither<ASTFactory.StringPos<POS>, PARAMETER> fromRoleName,
            SimpleEither<ASTFactory.StringPos<POS>, PARAMETER> toRoleName,
            boolean ifExists);

    ADMINISTRATION_COMMAND showRoles(
            POS p, boolean withUsers, boolean showAll, YIELD yieldExpr, RETURN_CLAUSE returnWithoutGraph, WHERE where);

    ADMINISTRATION_COMMAND grantRoles(
            POS p,
            List<SimpleEither<ASTFactory.StringPos<POS>, PARAMETER>> roles,
            List<SimpleEither<ASTFactory.StringPos<POS>, PARAMETER>> users);

    ADMINISTRATION_COMMAND revokeRoles(
            POS p,
            List<SimpleEither<ASTFactory.StringPos<POS>, PARAMETER>> roles,
            List<SimpleEither<ASTFactory.StringPos<POS>, PARAMETER>> users);

    // User Administration Commands

    ADMINISTRATION_COMMAND createUser(
            POS p,
            boolean replace,
            boolean ifNotExists,
            SimpleEither<ASTFactory.StringPos<POS>, PARAMETER> username,
            Boolean suspended,
            DATABASE_NAME homeDatabase,
            List<AUTH> auths,
            List<AUTH_ATTRIBUTE> systemAuthAttributes);

    ADMINISTRATION_COMMAND dropUser(POS p, boolean ifExists, SimpleEither<ASTFactory.StringPos<POS>, PARAMETER> username);

    ADMINISTRATION_COMMAND renameUser(
            POS p,
            SimpleEither<ASTFactory.StringPos<POS>, PARAMETER> fromUserName,
            SimpleEither<ASTFactory.StringPos<POS>, PARAMETER> toUserName,
            boolean ifExists);

    ADMINISTRATION_COMMAND setOwnPassword(POS p, EXPRESSION currentPassword, EXPRESSION newPassword);

    ADMINISTRATION_COMMAND alterUser(
            POS p,
            boolean ifExists,
            SimpleEither<ASTFactory.StringPos<POS>, PARAMETER> username,
            Boolean suspended,
            DATABASE_NAME homeDatabase,
            boolean removeHome,
            List<AUTH> auths,
            List<AUTH_ATTRIBUTE> systemAuthAttributes,
            boolean removeAllAuth,
            List<EXPRESSION> removeAuths);

    AUTH auth(String provider, List<AUTH_ATTRIBUTE> attributes, POS p);

    AUTH_ATTRIBUTE authId(POS s, EXPRESSION id);

    AUTH_ATTRIBUTE password(POS p, EXPRESSION password, boolean encrypted);

    AUTH_ATTRIBUTE passwordChangeRequired(POS p, boolean changeRequired);

    EXPRESSION passwordExpression(PARAMETER password);

    EXPRESSION passwordExpression(POS s, POS e, String password);

    ADMINISTRATION_COMMAND showUsers(
            POS p, YIELD yieldExpr, RETURN_CLAUSE returnWithoutGraph, WHERE where, boolean withAuth);

    ADMINISTRATION_COMMAND showCurrentUser(POS p, YIELD yieldExpr, RETURN_CLAUSE returnWithoutGraph, WHERE where);

    // Privilege Commands

    ADMINISTRATION_COMMAND showSupportedPrivileges(
            POS p, YIELD yieldExpr, RETURN_CLAUSE returnWithoutGraph, WHERE where);

    ADMINISTRATION_COMMAND showAllPrivileges(
            POS p, boolean asCommand, boolean asRevoke, YIELD yieldExpr, RETURN_CLAUSE returnWithoutGraph, WHERE where);

    ADMINISTRATION_COMMAND showRolePrivileges(
            POS p,
            List<SimpleEither<ASTFactory.StringPos<POS>, PARAMETER>> roles,
            boolean asCommand,
            boolean asRevoke,
            YIELD yieldExpr,
            RETURN_CLAUSE returnWithoutGraph,
            WHERE where);

    ADMINISTRATION_COMMAND showUserPrivileges(
            POS p,
            List<SimpleEither<ASTFactory.StringPos<POS>, PARAMETER>> users,
            boolean asCommand,
            boolean asRevoke,
            YIELD yieldExpr,
            RETURN_CLAUSE returnWithoutGraph,
            WHERE where);

    ADMINISTRATION_COMMAND grantPrivilege(
            POS p, List<SimpleEither<ASTFactory.StringPos<POS>, PARAMETER>> roles, PRIVILEGE_TYPE privilege);

    ADMINISTRATION_COMMAND denyPrivilege(
            POS p, List<SimpleEither<ASTFactory.StringPos<POS>, PARAMETER>> roles, PRIVILEGE_TYPE privilege);

    ADMINISTRATION_COMMAND revokePrivilege(
            POS p,
            List<SimpleEither<ASTFactory.StringPos<POS>, PARAMETER>> roles,
            PRIVILEGE_TYPE privilege,
            boolean revokeGrant,
            boolean revokeDeny);

    PRIVILEGE_TYPE databasePrivilege(
            POS p,
            ADMINISTRATION_ACTION action,
            DATABASE_SCOPE scope,
            List<PRIVILEGE_QUALIFIER> qualifier,
            boolean immutable);

    PRIVILEGE_TYPE dbmsPrivilege(
            POS p, ADMINISTRATION_ACTION action, List<PRIVILEGE_QUALIFIER> qualifier, boolean immutable);

    PRIVILEGE_TYPE loadPrivilege(
            POS p, SimpleEither<String, PARAMETER> url, SimpleEither<String, PARAMETER> cidr, boolean immutable);

    PRIVILEGE_TYPE graphPrivilege(
            POS p,
            ADMINISTRATION_ACTION action,
            GRAPH_SCOPE scope,
            PRIVILEGE_RESOURCE resource,
            List<PRIVILEGE_QUALIFIER> qualifier,
            boolean immutable);

    ADMINISTRATION_ACTION privilegeAction(ActionType action);

    // Resources

    PRIVILEGE_RESOURCE propertiesResource(POS p, List<String> property);

    PRIVILEGE_RESOURCE allPropertiesResource(POS p);

    PRIVILEGE_RESOURCE labelsResource(POS p, List<String> label);

    PRIVILEGE_RESOURCE allLabelsResource(POS p);

    PRIVILEGE_RESOURCE databaseResource(POS p);

    PRIVILEGE_RESOURCE noResource(POS p);

    PRIVILEGE_QUALIFIER labelQualifier(POS p, String label);

    PRIVILEGE_QUALIFIER allLabelsQualifier(POS p);

    PRIVILEGE_QUALIFIER relationshipQualifier(POS p, String relationshipType);

    PRIVILEGE_QUALIFIER allRelationshipsQualifier(POS p);

    PRIVILEGE_QUALIFIER elementQualifier(POS p, String name);

    PRIVILEGE_QUALIFIER allElementsQualifier(POS p);

    PRIVILEGE_QUALIFIER patternQualifier(
            List<PRIVILEGE_QUALIFIER> qualifiers, VARIABLE variable, EXPRESSION expression);

    List<PRIVILEGE_QUALIFIER> allQualifier();

    List<PRIVILEGE_QUALIFIER> allDatabasesQualifier();

    List<PRIVILEGE_QUALIFIER> userQualifier(List<SimpleEither<ASTFactory.StringPos<POS>, PARAMETER>> users);

    List<PRIVILEGE_QUALIFIER> allUsersQualifier();

    List<PRIVILEGE_QUALIFIER> functionQualifier(POS p, List<String> functions);

    List<PRIVILEGE_QUALIFIER> procedureQualifier(POS p, List<String> procedures);

    List<PRIVILEGE_QUALIFIER> settingQualifier(POS p, List<String> names);

    GRAPH_SCOPE graphScope(POS p, List<DATABASE_NAME> graphNames, ScopeType scopeType);

    DATABASE_SCOPE databasePrivilegeScope(POS p, List<DATABASE_NAME> databaseNames, ScopeType scopeType);

    // Server Administration Commands

    ADMINISTRATION_COMMAND enableServer(
            POS p,
            SimpleEither<String, PARAMETER> serverName,
            SimpleEither<Map<String, EXPRESSION>, PARAMETER> options);

    ADMINISTRATION_COMMAND alterServer(
            POS p,
            SimpleEither<String, PARAMETER> serverName,
            SimpleEither<Map<String, EXPRESSION>, PARAMETER> options);

    ADMINISTRATION_COMMAND renameServer(
            POS p, SimpleEither<String, PARAMETER> serverName, SimpleEither<String, PARAMETER> newName);

    ADMINISTRATION_COMMAND dropServer(POS p, SimpleEither<String, PARAMETER> serverName);

    ADMINISTRATION_COMMAND showServers(POS p, YIELD yieldExpr, RETURN_CLAUSE returnWithoutGraph, WHERE where);

    ADMINISTRATION_COMMAND deallocateServers(POS p, boolean dryRun, List<SimpleEither<String, PARAMETER>> serverNames);

    ADMINISTRATION_COMMAND reallocateDatabases(POS p, boolean dryRun);

    // Database Administration Commands

    ADMINISTRATION_COMMAND createDatabase(
            POS p,
            boolean replace,
            DATABASE_NAME databaseName,
            boolean ifNotExists,
            WAIT_CLAUSE waitClause,
            SimpleEither<Map<String, EXPRESSION>, PARAMETER> options,
            SimpleEither<Integer, PARAMETER> topologyPrimaries,
            SimpleEither<Integer, PARAMETER> topologySecondaries);

    ADMINISTRATION_COMMAND createCompositeDatabase(
            POS p,
            boolean replace,
            DATABASE_NAME compositeDatabaseName,
            boolean ifNotExists,
            SimpleEither<Map<String, EXPRESSION>, PARAMETER> options,
            WAIT_CLAUSE waitClause);

    ADMINISTRATION_COMMAND dropDatabase(
            POS p,
            DATABASE_NAME databaseName,
            boolean ifExists,
            boolean composite,
            boolean aliasAction,
            boolean dumpData,
            WAIT_CLAUSE wait);

    ADMINISTRATION_COMMAND alterDatabase(
            POS p,
            DATABASE_NAME databaseName,
            boolean ifExists,
            AccessType accessType,
            SimpleEither<Integer, PARAMETER> topologyPrimaries,
            SimpleEither<Integer, PARAMETER> topologySecondaries,
            Map<String, EXPRESSION> options,
            Set<String> optionsToRemove,
            WAIT_CLAUSE waitClause);

    ADMINISTRATION_COMMAND showDatabase(
            POS p, DATABASE_SCOPE scope, YIELD yieldExpr, RETURN_CLAUSE returnWithoutGraph, WHERE where);

    ADMINISTRATION_COMMAND startDatabase(POS p, DATABASE_NAME databaseName, WAIT_CLAUSE wait);

    ADMINISTRATION_COMMAND stopDatabase(POS p, DATABASE_NAME databaseName, WAIT_CLAUSE wait);

    DATABASE_SCOPE databaseScope(POS p, DATABASE_NAME databaseName, boolean isDefault, boolean isHome);

    WAIT_CLAUSE wait(boolean wait, long seconds);

    DATABASE_NAME databaseName(POS p, List<String> names);

    DATABASE_NAME databaseName(PARAMETER param);

    // Alias Administration Commands
    ADMINISTRATION_COMMAND createLocalDatabaseAlias(
            POS p,
            boolean replace,
            DATABASE_NAME aliasName,
            DATABASE_NAME targetName,
            boolean ifNotExists,
            SimpleEither<Map<String, EXPRESSION>, PARAMETER> properties);

    ADMINISTRATION_COMMAND createRemoteDatabaseAlias(
            POS p,
            boolean replace,
            DATABASE_NAME aliasName,
            DATABASE_NAME targetName,
            boolean ifNotExists,
            SimpleEither<String, PARAMETER> url,
            SimpleEither<ASTFactory.StringPos<POS>, PARAMETER> username,
            EXPRESSION password,
            SimpleEither<Map<String, EXPRESSION>, PARAMETER> driverSettings,
            SimpleEither<Map<String, EXPRESSION>, PARAMETER> properties);

    ADMINISTRATION_COMMAND alterLocalDatabaseAlias(
            POS p,
            DATABASE_NAME aliasName,
            DATABASE_NAME targetName,
            boolean ifExists,
            SimpleEither<Map<String, EXPRESSION>, PARAMETER> properties);

    ADMINISTRATION_COMMAND alterRemoteDatabaseAlias(
            POS p,
            DATABASE_NAME aliasName,
            DATABASE_NAME targetName,
            boolean ifExists,
            SimpleEither<String, PARAMETER> url,
            SimpleEither<ASTFactory.StringPos<POS>, PARAMETER> username,
            EXPRESSION password,
            SimpleEither<Map<String, EXPRESSION>, PARAMETER> driverSettings,
            SimpleEither<Map<String, EXPRESSION>, PARAMETER> properties);

    ADMINISTRATION_COMMAND dropAlias(POS p, DATABASE_NAME aliasName, boolean ifExists);

    ADMINISTRATION_COMMAND showAliases(
            POS p, DATABASE_NAME aliasName, YIELD yieldExpr, RETURN_CLAUSE returnWithoutGraph, WHERE where);
}
