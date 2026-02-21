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
parser grammar Cypher5Parser;

@parser::header {
import org.neo4j.cypher.internal.parser.AstRuleCtx;
}

options {
   tokenVocab = Cypher5Lexer;
   contextSuperClass = AstRuleCtx;
}
statements
   : statement (SEMICOLON statement)* SEMICOLON? EOF
   ;

statement
   : periodicCommitQueryHintFailure? regularQuery
   ;

periodicCommitQueryHintFailure
   : USING PERIODIC COMMIT UNSIGNED_DECIMAL_INTEGER?
   ;

regularQuery
   : singleQuery (UNION (ALL | DISTINCT)? singleQuery)*
   ;

singleQuery
   : clause+
   ;

clause
   : useClause
   | finishClause
   | returnClause
   | createClause
   | insertClause
   | deleteClause
   | setClause
   | removeClause
   | matchClause
   | mergeClause
   | withClause
   | unwindClause
   | callClause
   | subqueryClause
   | loadCSVClause
   | foreachClause
   | orderBySkipLimitClause
   ;

useClause
   : USE GRAPH? graphReference
   ;

graphReference
   : LPAREN graphReference RPAREN
   | functionInvocation
   | symbolicAliasName
   ;

finishClause
   : FINISH
   ;

returnClause
   : RETURN returnBody
   ;

returnBody
   : DISTINCT? returnItems orderBy? skip? limit?
   ;

returnItem
   : expression (AS variable)?
   ;

returnItems
   : (TIMES | returnItem) (COMMA returnItem)*
   ;

orderItem
   : expression (ascToken | descToken)?
   ;

ascToken
   : ASC | ASCENDING
   ;

descToken
   : DESC | DESCENDING
   ;

orderBy
   : ORDER BY orderItem (COMMA orderItem)*
   ;

skip
   : (OFFSET | SKIPROWS) expression
   ;

limit
   : LIMITROWS expression
   ;

whereClause
   : WHERE expression
   ;

withClause
   : WITH returnBody whereClause?
   ;

createClause
   : CREATE patternList
   ;

insertClause
   : INSERT insertPatternList
   ;

setClause
   : SET setItem (COMMA setItem)*
   ;

setItem
   : propertyExpression EQ expression        # SetProp
   | dynamicPropertyExpression EQ expression # SetDynamicProp
   | variable EQ expression                  # SetProps
   | variable PLUSEQUAL expression           # AddProp
   | variable nodeLabels                     # SetLabels
   | variable nodeLabelsIs                   # SetLabelsIs
   ;

removeClause
   : REMOVE removeItem (COMMA removeItem)*
   ;

removeItem
   : propertyExpression         # RemoveProp
   | dynamicPropertyExpression  # RemoveDynamicProp
   | variable nodeLabels        # RemoveLabels
   | variable nodeLabelsIs      # RemoveLabelsIs
   ;

deleteClause
   : (DETACH | NODETACH)? DELETE expression (COMMA expression)*
   ;

matchClause
   : OPTIONAL? MATCH matchMode? patternList hint* whereClause?
   ;

matchMode
   : REPEATABLE (ELEMENT BINDINGS? | ELEMENTS)
   | DIFFERENT (RELATIONSHIP BINDINGS? | RELATIONSHIPS)
   ;

hint
   : USING (((
      INDEX
      | BTREE INDEX
      | TEXT INDEX
      | RANGE INDEX
      | POINT INDEX
   ) SEEK? variable labelOrRelType LPAREN nonEmptyNameList RPAREN)
   | JOIN ON nonEmptyNameList
   | SCAN variable labelOrRelType
   )
   ;

mergeClause
   : MERGE pattern mergeAction*
   ;

mergeAction
   : ON (MATCH | CREATE) setClause
   ;

unwindClause
   : UNWIND expression AS variable
   ;

callClause
   : OPTIONAL? CALL procedureName (LPAREN (procedureArgument (COMMA procedureArgument)*)? RPAREN)? (YIELD (TIMES | procedureResultItem (COMMA procedureResultItem)* whereClause?))?
   ;

procedureName
   : namespace symbolicNameString
   ;

procedureArgument
   : expression
   ;

procedureResultItem
   : symbolicNameString (AS variable)?
   ;

loadCSVClause
   : LOAD CSV (WITH HEADERS)? FROM expression AS variable (FIELDTERMINATOR stringLiteral)?
   ;

foreachClause
   : FOREACH LPAREN variable IN expression BAR clause+ RPAREN
   ;

subqueryClause
   : OPTIONAL? CALL subqueryScope? LCURLY regularQuery RCURLY subqueryInTransactionsParameters?
   ;

subqueryScope
   : LPAREN (TIMES | variable (COMMA variable)*)? RPAREN
   ;

subqueryInTransactionsParameters
   : IN (expression? CONCURRENT)? TRANSACTIONS (subqueryInTransactionsBatchParameters | subqueryInTransactionsErrorParameters | subqueryInTransactionsReportParameters)*
   ;

subqueryInTransactionsBatchParameters
   : OF expression (ROW | ROWS)
   ;

subqueryInTransactionsErrorParameters
   : ON ERROR (CONTINUE | BREAK | FAIL)
   ;

subqueryInTransactionsReportParameters
   : REPORT STATUS AS variable
   ;

orderBySkipLimitClause
   : orderBy skip? limit?
   | skip limit?
   | limit
   ;

patternList
   : pattern (COMMA pattern)*
   ;

insertPatternList
   : insertPattern (COMMA insertPattern)*
   ;

pattern
   : (variable EQ)? selector? anonymousPattern
   ;

insertPattern
   : (symbolicNameString EQ)? insertNodePattern (insertRelationshipPattern insertNodePattern)*
   ;

quantifier
   : LCURLY UNSIGNED_DECIMAL_INTEGER RCURLY
   | LCURLY from = UNSIGNED_DECIMAL_INTEGER? COMMA to = UNSIGNED_DECIMAL_INTEGER? RCURLY
   | PLUS
   | TIMES
   ;

anonymousPattern
   : shortestPathPattern
   | patternElement
   ;

shortestPathPattern
   : (SHORTEST_PATH | ALL_SHORTEST_PATHS) LPAREN patternElement RPAREN
   ;

patternElement
   : (nodePattern (relationshipPattern quantifier? nodePattern)* | parenthesizedPath)+
   ;

selector
   : ANY SHORTEST pathToken?                                  # AnyShortestPath
   | ALL SHORTEST pathToken?                                  # AllShortestPath
   | ANY UNSIGNED_DECIMAL_INTEGER? pathToken?                 # AnyPath
   | ALL pathToken?                                           # AllPath
   | SHORTEST UNSIGNED_DECIMAL_INTEGER? pathToken? groupToken # ShortestGroup
   | SHORTEST UNSIGNED_DECIMAL_INTEGER pathToken?             # AnyShortestPath
   ;

groupToken
   : GROUP | GROUPS
   ;

pathToken
   : PATH | PATHS
   ;

pathPatternNonEmpty
   : nodePattern (relationshipPattern nodePattern)+
   ;

nodePattern
   : LPAREN variable? labelExpression? properties? (WHERE expression)? RPAREN
   ;

insertNodePattern
   : LPAREN variable? insertNodeLabelExpression? map? RPAREN
   ;

parenthesizedPath
   : LPAREN pattern (WHERE expression)? RPAREN quantifier?
   ;

nodeLabels
   : (labelType | dynamicLabelType)+
   ;

nodeLabelsIs
   : IS (symbolicNameString | dynamicExpression) (labelType | dynamicLabelType)*
   ;

dynamicExpression
   : DOLLAR LPAREN expression RPAREN
   ;

dynamicAnyAllExpression
   : DOLLAR (ALL | ANY)? LPAREN expression RPAREN
   ;

dynamicLabelType
   : COLON dynamicExpression
   ;

labelType
   : COLON symbolicNameString
   ;

relType
   : COLON symbolicNameString
   ;

labelOrRelType
   : COLON symbolicNameString
   ;

properties
   : map
   | parameter["ANY"]
   ;

relationshipPattern
   : leftArrow? arrowLine (LBRACKET variable? labelExpression? pathLength? properties? (WHERE expression)? RBRACKET)? arrowLine rightArrow?
   ;

insertRelationshipPattern
   : leftArrow? arrowLine LBRACKET variable? insertRelationshipLabelExpression map? RBRACKET arrowLine rightArrow?
   ;

leftArrow
   : LT
   | ARROW_LEFT_HEAD
   ;

arrowLine
   : ARROW_LINE
   | MINUS
   ;

rightArrow
   : GT
   | ARROW_RIGHT_HEAD
   ;

pathLength
   : TIMES (from = UNSIGNED_DECIMAL_INTEGER? DOTDOT to = UNSIGNED_DECIMAL_INTEGER? | single = UNSIGNED_DECIMAL_INTEGER)?
   ;

labelExpression
   : COLON labelExpression4
   | IS labelExpression4Is
   ;

labelExpression4
   : labelExpression3 (BAR COLON? labelExpression3)*
   ;

labelExpression4Is
   : labelExpression3Is (BAR COLON? labelExpression3Is)*
   ;

labelExpression3
   : labelExpression2 ((AMPERSAND | COLON) labelExpression2)*
   ;

labelExpression3Is
   : labelExpression2Is ((AMPERSAND | COLON) labelExpression2Is)*
   ;

labelExpression2
   : EXCLAMATION_MARK* labelExpression1
   ;

labelExpression2Is
   : EXCLAMATION_MARK* labelExpression1Is
   ;

labelExpression1
   : LPAREN labelExpression4 RPAREN #ParenthesizedLabelExpression
   | PERCENT                        #AnyLabel
   | dynamicAnyAllExpression        #DynamicLabel
   | symbolicNameString             #LabelName
   ;

labelExpression1Is
   : LPAREN labelExpression4Is RPAREN #ParenthesizedLabelExpressionIs
   | PERCENT                          #AnyLabelIs
   | dynamicAnyAllExpression          #DynamicLabelIs
   | symbolicLabelNameString          #LabelNameIs
   ;

insertNodeLabelExpression
   : (COLON | IS) symbolicNameString ((AMPERSAND | COLON) symbolicNameString)*
   ;

insertRelationshipLabelExpression
   : (COLON | IS) symbolicNameString
   ;

expression
   : expression11 (OR expression11)*
   ;

expression11
   : expression10 (XOR expression10)*
   ;

expression10
   : expression9 (AND expression9)*
   ;

expression9
   : NOT* expression8
   ;

// Making changes here? Consider looking at extendedWhen too.
expression8
   : expression7 ((
      EQ
      | INVALID_NEQ
      | NEQ
      | LE
      | GE
      | LT
      | GT
   ) expression7)*
   ;

expression7
   : expression6 comparisonExpression6?
   ;

// Making changes here? Consider looking at extendedWhen too.
comparisonExpression6
   : (
      REGEQ
      | STARTS WITH
      | ENDS WITH
      | CONTAINS
      | IN
   ) expression6                                      # StringAndListComparison
   | IS NOT? NULL                                     # NullComparison
   | (IS NOT? (TYPED | COLONCOLON) | COLONCOLON) type # TypeComparison
   | IS NOT? normalForm? NORMALIZED                   # NormalFormComparison
   ;

normalForm
   : NFC
   | NFD
   | NFKC
   | NFKD
   ;

expression6
   : expression5 ((PLUS | MINUS | DOUBLEBAR) expression5)*
   ;

expression5
   : expression4 ((TIMES | DIVIDE | PERCENT) expression4)*
   ;

expression4
   : expression3 (POW expression3)*
   ;

expression3
   : expression2
   | (PLUS | MINUS) expression2
   ;

expression2
   : expression1 postFix*
   ;

postFix
   : property                                                           # PropertyPostfix
   | labelExpression                                                    # LabelPostfix
   | LBRACKET expression RBRACKET                                       # IndexPostfix
   | LBRACKET fromExp = expression? DOTDOT toExp = expression? RBRACKET # RangePostfix
   ;

property
   : DOT propertyKeyName
   ;

dynamicProperty
   : LBRACKET expression RBRACKET
   ;

propertyExpression
   : expression1 property+
   ;

dynamicPropertyExpression
   : expression1 dynamicProperty
   ;

expression1
   : literal
   | parameter["ANY"]
   | caseExpression
   | extendedCaseExpression
   | countStar
   | existsExpression
   | countExpression
   | collectExpression
   | mapProjection
   | listComprehension
   | listLiteral
   | patternComprehension
   | reduceExpression
   | listItemsPredicate
   | normalizeFunction
   | trimFunction
   | patternExpression
   | shortestPathExpression
   | parenthesizedExpression
   | functionInvocation
   | variable
   ;

literal
   : numberLiteral # NummericLiteral
   | stringLiteral # StringsLiteral
   | map           # OtherLiteral
   | TRUE          # BooleanLiteral
   | FALSE         # BooleanLiteral
   | INF           # KeywordLiteral
   | INFINITY      # KeywordLiteral
   | NAN           # KeywordLiteral
   | NULL          # KeywordLiteral
   ;

caseExpression
   : CASE caseAlternative+ (ELSE expression)? END
   ;

caseAlternative
   : WHEN expression THEN expression
   ;

extendedCaseExpression
   : CASE expression extendedCaseAlternative+ (ELSE elseExp = expression)? END
   ;

extendedCaseAlternative
   : WHEN extendedWhen (COMMA extendedWhen)* THEN expression
   ;

// Making changes here? Consider looking at comparisonExpression6 and expression8 too.
extendedWhen
   : (REGEQ | STARTS WITH | ENDS WITH) expression6 # WhenStringOrList
   | IS NOT? NULL                                  # WhenNull
   | (IS NOT? TYPED | COLONCOLON) type             # WhenType
   | IS NOT? normalForm? NORMALIZED                # WhenForm
   | (
      EQ
      | NEQ
      | INVALID_NEQ
      | LE
      | GE
      | LT
      | GT
   ) expression7                                   # WhenComparator
   | expression                                    # WhenEquals
   ;

// Observe that this is not possible to write as:
// (WHERE whereExp = expression)? (BAR barExp = expression)? RBRACKET
// Due to an ambigouity with cases such as [node IN nodes WHERE node:A|B]
// where |B will be interpreted as part of the whereExp, rather than as the expected barExp.
listComprehension
   : LBRACKET variable IN expression ((WHERE whereExp = expression)? BAR barExp = expression | (WHERE whereExp = expression)?) RBRACKET
   ;

patternComprehension
   : LBRACKET (variable EQ)? pathPatternNonEmpty (WHERE whereExp = expression)? BAR barExp = expression RBRACKET
   ;

reduceExpression
   : REDUCE LPAREN variable EQ expression COMMA variable IN expression BAR expression RPAREN
   ;

listItemsPredicate
   : (
      ALL
      | ANY
      | NONE
      | SINGLE
   ) LPAREN variable IN inExp = expression (WHERE whereExp = expression)? RPAREN
   ;

normalizeFunction
   : NORMALIZE LPAREN expression (COMMA normalForm)? RPAREN
   ;

trimFunction
   : TRIM LPAREN ((BOTH | LEADING | TRAILING)? (trimCharacterString = expression)? FROM)? trimSource = expression RPAREN
   ;

patternExpression
   : pathPatternNonEmpty
   ;

shortestPathExpression
   : shortestPathPattern
   ;

parenthesizedExpression
   : LPAREN expression RPAREN
   ;

mapProjection
   : variable LCURLY (mapProjectionElement (COMMA mapProjectionElement)* )? RCURLY
   ;

mapProjectionElement
   : propertyKeyName COLON expression
   | property
   | variable
   | DOT TIMES
   ;

countStar
   : COUNT LPAREN TIMES RPAREN
   ;

existsExpression
   : EXISTS LCURLY (regularQuery | matchMode? patternList whereClause?) RCURLY
   ;

countExpression
   : COUNT LCURLY (regularQuery | matchMode? patternList whereClause?) RCURLY
   ;

collectExpression
   : COLLECT LCURLY regularQuery RCURLY
   ;

numberLiteral
   : MINUS? (
      DECIMAL_DOUBLE
      | UNSIGNED_DECIMAL_INTEGER
      | UNSIGNED_HEX_INTEGER
      | UNSIGNED_OCTAL_INTEGER
   )
   ;

signedIntegerLiteral
   : MINUS? UNSIGNED_DECIMAL_INTEGER
   ;

listLiteral
   : LBRACKET (expression (COMMA expression)* )? RBRACKET
   ;

propertyKeyName
   : symbolicNameString
   ;

parameter[String paramType]
   : DOLLAR parameterName[paramType]
   ;

parameterName[String paramType]
   : (symbolicNameString | UNSIGNED_DECIMAL_INTEGER)
   ;

functionInvocation
   : functionName LPAREN (DISTINCT | ALL)? (functionArgument (COMMA functionArgument)* )? RPAREN
   ;

functionArgument
   : expression
   ;

functionName
   : namespace symbolicNameString
   ;

namespace
   : (symbolicNameString DOT)*
   ;

variable
   : symbolicVariableNameString
   ;

// Returns non-list of propertyKeyNames
nonEmptyNameList
   : symbolicNameString (COMMA symbolicNameString)*
   ;

type
   : typePart (BAR typePart)*
   ;

typePart
   : typeName typeNullability? typeListSuffix*
   ;

typeName
   // Note! These are matched based on the first token. Take precaution in ExpressionBuilder.scala when modifying
   : NOTHING
   | NULL
   | BOOL
   | BOOLEAN
   | VARCHAR
   | STRING
   | INT
   | SIGNED? INTEGER
   | FLOAT
   | DATE
   | LOCAL (TIME | DATETIME)
   | ZONED (TIME | DATETIME)
   | TIME (WITHOUT | WITH) (TIMEZONE | TIME ZONE)
   | TIMESTAMP (WITHOUT | WITH) (TIMEZONE | TIME ZONE)
   | DURATION
   | POINT
   | NODE
   | VERTEX
   | RELATIONSHIP
   | EDGE
   | MAP
   | (LIST | ARRAY) LT type GT
   | PATH
   | PATHS
   | PROPERTY VALUE
   | ANY (
      NODE
      | VERTEX
      | RELATIONSHIP
      | EDGE
      | MAP
      | PROPERTY VALUE
      | VALUE? LT type GT
      | VALUE
   )?
   ;

typeNullability
   : NOT NULL
   | EXCLAMATION_MARK
   ;

typeListSuffix
   : (LIST | ARRAY) typeNullability?
   ;

// Show, terminate, schema and admin commands

command
   : useClause? (
      createCommand
      | dropCommand
      | showCommand
      | terminateCommand
      | alterCommand
      | renameCommand
      | denyCommand
      | revokeCommand
      | grantCommand
      | startDatabase
      | stopDatabase
      | enableServerCommand
      | allocationCommand
   )
   ;

createCommand
   : CREATE
   ;

dropCommand
   : DROP
   ;

showCommand
   : SHOW
   ;

showCommandYield
   : YIELD
   ;

yieldItem
   : variable
   ;

yieldSkip
   : OFFSET signedIntegerLiteral
   ;

yieldLimit
   : LIMITROWS signedIntegerLiteral
   ;

yieldClause
   : YIELD
   ;

commandOptions
   : OPTIONS mapOrParameter
   ;

// Non-admin show and terminate commands

terminateCommand
   : TERMINATE terminateTransactions
   ;

composableCommandClauses
   : terminateCommand
   | composableShowCommandClauses
   ;

composableShowCommandClauses
   : SHOW
   ;

showBriefAndYield
   : BRIEF
   | VERBOSE
   | YIELD
   | WHERE expression
   ;

showIndexCommand
   : indexToken
   ;

showIndexesAllowBrief
   : indexToken
   ;

showIndexesNoBrief
   : indexToken
   ;

showConstraintCommand
   : constraintToken
   ;

constraintAllowYieldType
   : UNIQUENESS
   ;

constraintExistType
   : EXISTENCE
   ;

constraintBriefAndYieldType
   : ALL
   ;

showConstraintsAllowBriefAndYield
   : constraintToken
   ;

showConstraintsAllowBrief
   : constraintToken
   ;

showConstraintsAllowYield
   : constraintToken
   ;

showProcedures
   : procedureToken
   ;

showFunctions
   : functionToken
   ;

functionToken
   : FUNCTION | FUNCTIONS
   ;

executableBy
   : EXECUTABLE
   ;

showFunctionsType
   : ALL
   ;

showTransactions
   : transactionToken
   ;

terminateTransactions
   : transactionToken stringsOrExpression?
   ;

showSettings
   : settingToken
   ;

settingToken
   : SETTING | SETTINGS
   ;

namesAndClauses
   : stringsOrExpression?
   ;

stringsOrExpression
   : stringList
   | expression
   ;

// Schema commands

commandNodePattern
   : LPAREN variable? RPAREN
   ;

commandRelPattern
   : LPAREN RPAREN
   ;

createConstraint
   : CONSTRAINT
   ;

constraintType
   : REQUIRE #ConstraintExists
   | REQUIRE #ConstraintTyped
   | REQUIRE #ConstraintIsUnique
   | REQUIRE #ConstraintKey
   | REQUIRE #ConstraintIsNotNull
   ;

dropConstraint
   : CONSTRAINT
   ;

createIndex
   : INDEX
   ;

oldCreateIndex
   : labelType LPAREN nonEmptyNameList RPAREN
   ;

createIndex_
   : INDEX
   ;

createFulltextIndex
   : INDEX
   ;

fulltextNodePattern
   : LPAREN variable? RPAREN
   ;

fulltextRelPattern
   : LPAREN RPAREN
   ;

createLookupIndex
   : INDEX
   ;

lookupIndexNodePattern
   : LPAREN variable? RPAREN
   ;

lookupIndexRelPattern
   : LPAREN RPAREN
   ;

dropIndex
   : INDEX
   ;

propertyList
   : variable property
   ;

enclosedPropertyList
   : variable property (COMMA variable property)*
   ;

// Admin commands

alterCommand
   : ALTER
   ;

renameCommand
   : RENAME
   ;

grantCommand
   : GRANT
   ;

denyCommand
   : DENY
   ;

revokeCommand
   : REVOKE
   ;

userNames
   : symbolicNameOrStringParameterList
   ;

roleNames
   : symbolicNameOrStringParameterList
   ;

roleToken
   : ROLES
   | ROLE
   ;

// Server commands

enableServerCommand
   : ENABLE SERVER stringOrParameter
   ;

alterServer
   : SERVER stringOrParameter
   ;

renameServer
   : SERVER stringOrParameter
   ;

dropServer
   : SERVER stringOrParameter
   ;

showServers
   : SERVER
   | SERVERS
   ;

allocationCommand
   : DEALLOCATE
   | REALLOCATE
   ;

deallocateDatabaseFromServers
   : DEALLOCATE DATABASE
   | DEALLOCATE DATABASES
   ;

reallocateDatabases
   : REALLOCATE DATABASE
   | REALLOCATE DATABASES
   ;

// Role commands

createRole
   : ROLE commandNameExpression
   ;

dropRole
   : ROLE commandNameExpression
   ;

renameRole
   : ROLE commandNameExpression TO commandNameExpression
   ;

showRoles
   : roleToken
   ;

grantRole
   : roleNames TO userNames
   ;

revokeRole
   : roleNames FROM userNames
   ;

// User commands

createUser
   : USER commandNameExpression
   ;

dropUser
   : USER commandNameExpression
   ;

renameUser
   : USER commandNameExpression TO commandNameExpression
   ;

alterCurrentUser
   : CURRENT USER
   ;

alterUser
   : USER commandNameExpression
   ;

removeNamedProvider
   : AUTH
   ;

password
   : PASSWORD passwordExpression
   ;

passwordOnly
   : PASSWORD passwordExpression
   ;

passwordExpression
   : stringLiteral
   | parameter["STRING"]
   ;

passwordChangeRequired
   : CHANGE REQUIRED
   ;

userStatus
   : STATUS ACTIVE
   | STATUS SUSPENDED
   ;

homeDatabase
   : HOME DATABASE symbolicAliasNameOrParameter
   ;

setAuthClause
   : AUTH
   ;

userAuthAttribute
   : ID stringOrParameterExpression
   ;

showUsers
   : USER
   | USERS
   ;

showCurrentUser
   : CURRENT USER
   ;

// Privilege commands

showSupportedPrivileges
   : SUPPORTED privilegeToken
   ;

showPrivileges
   : privilegeToken
   ;

showRolePrivileges
   : roleToken privilegeToken
   ;

showUserPrivileges
   : USER privilegeToken
   | USERS privilegeToken
   ;

privilegeAsCommand
   : AS COMMAND
   | AS COMMANDS
   ;

privilegeToken
   : PRIVILEGE
   | PRIVILEGES
   ;

privilege
   : privilegeToken
   ;

allPrivilege
   : ALL ON allPrivilegeTarget
   ;

allPrivilegeType
   : PRIVILEGES
   ;

allPrivilegeTarget
   : DBMS
   ;

createPrivilege
   : CREATE privilegeToken
   ;

createPrivilegeForDatabase
   : indexToken
   | constraintToken
   ;

createNodePrivilegeToken
   : NEW NODE
   ;

createRelPrivilegeToken
   : NEW RELATIONSHIP
   ;

createPropertyPrivilegeToken
   : NEW PROPERTY
   ;

actionForDBMS
   : ALIAS
   ;

dropPrivilege
   : DROP privilegeToken
   ;

loadPrivilege
   : LOAD
   ;

showPrivilege
   : SHOW privilegeToken
   ;

setPrivilege
   : SET privilegeToken
   ;

passwordToken
   : PASSWORD
   | PASSWORDS
   ;

removePrivilege
   : REMOVE privilegeToken
   ;

writePrivilege
   : WRITE
   ;

databasePrivilege
   : ACCESS
   ;

dbmsPrivilege
   : ADMIN
   ;

dbmsPrivilegeExecute
   : EXECUTE
   ;

adminToken
   : ADMIN
   | ADMINISTRATOR
   ;

procedureToken
   : PROCEDURE
   | PROCEDURES
   ;

indexToken
   : INDEX
   | INDEXES
   ;

constraintToken
   : CONSTRAINT
   | CONSTRAINTS
   ;

transactionToken
   : TRANSACTION
   | TRANSACTIONS
   ;

userQualifier
   : LPAREN TIMES RPAREN
   ;

executeFunctionQualifier
   : globs
   ;

executeProcedureQualifier
   : globs
   ;

settingQualifier
   : globs
   ;

globs
   : glob (COMMA glob)*
   ;

glob
   : escapedSymbolicNameString globRecursive?
   | globRecursive
   ;

globRecursive
   : globPart globRecursive?
   ;

globPart
   : DOT escapedSymbolicNameString?
   | QUESTION
   | TIMES
   | unescapedSymbolicNameString
   ;

qualifiedGraphPrivilegesWithProperty
   : READ
   ;

qualifiedGraphPrivileges
   : DELETE
   | MERGE
   ;

labelsResource
   : TIMES
   | nonEmptyStringList
   ;

propertiesResource
   : LCURLY (TIMES | nonEmptyStringList) RCURLY
   ;

// Returns non-empty list of strings
nonEmptyStringList
   : symbolicNameString (COMMA symbolicNameString)*
   ;

graphQualifier
   : graphQualifierToken?
   ;

graphQualifierToken
   : relToken
   | nodeToken
   | elementToken
   ;

relToken
   : RELATIONSHIP
   | RELATIONSHIPS
   ;

elementToken
   : ELEMENT
   | ELEMENTS
   ;

nodeToken
   : NODE
   | NODES
   ;

databaseScope
   : DATABASE
   | DATABASES
   ;

graphScope
   : GRAPH
   | GRAPHS
   ;

// Database commands

createCompositeDatabase
   : COMPOSITE DATABASE symbolicAliasNameOrParameter
   ;

createDatabase
   : DATABASE symbolicAliasNameOrParameter
   ;

primaryTopology
   : uIntOrIntParameter primaryToken
   ;

primaryToken
   : PRIMARY | PRIMARIES
   ;

secondaryTopology
   : uIntOrIntParameter secondaryToken
   ;

secondaryToken
   : SECONDARY | SECONDARIES
   ;

dropDatabase
   : DATABASE symbolicAliasNameOrParameter
   ;

aliasAction
   : RESTRICT
   | CASCADE ALIAS
   | CASCADE ALIASES
   ;

alterDatabase
   : DATABASE symbolicAliasNameOrParameter
   ;

alterDatabaseAccess
   : ACCESS READ ONLY
   | ACCESS READ WRITE
   ;

alterDatabaseTopology
   : TOPOLOGY (primaryTopology | secondaryTopology)+
   ;

alterDatabaseOption
   : OPTION symbolicNameString expression
   ;

startDatabase
   : START DATABASE symbolicAliasNameOrParameter
   ;

stopDatabase
   : STOP DATABASE symbolicAliasNameOrParameter
   ;

waitClause
   : WAIT (UNSIGNED_DECIMAL_INTEGER secondsToken?)?
   | NOWAIT
   ;

secondsToken
   : SEC | SECOND | SECONDS;

showDatabase
   : DATABASE
   | DATABASES
   ;

// Alias commands

aliasName
   : symbolicAliasNameOrParameter
   ;

databaseName
   : symbolicAliasNameOrParameter
   ;

createAlias
   : ALIAS aliasName FOR DATABASE databaseName
   ;

dropAlias
   : ALIAS aliasName FOR DATABASE
   ;

alterAlias
   : ALIAS aliasName SET DATABASE alterAliasTarget
   ;

alterAliasTarget
   : TARGET databaseName
   ;

alterAliasUser
   : USER commandNameExpression
   ;

alterAliasPassword
   : PASSWORD passwordExpression
   ;

alterAliasDriver
   : DRIVER mapOrParameter
   ;

alterAliasProperties
   : PROPERTIES mapOrParameter
   ;

showAliases
   : ALIAS
   | ALIASES
   ;

// Various strings, symbolic names, lists and maps

// Should return an Either[String, Parameter]
symbolicNameOrStringParameter
   : symbolicNameString
   | parameter["STRING"]
   ;

// Should return an Expression
commandNameExpression
   : symbolicNameString
   | parameter["STRING"]
   ;

symbolicNameOrStringParameterList
   : commandNameExpression (COMMA commandNameExpression)*
   ;

symbolicAliasNameList
   : symbolicAliasNameOrParameter (COMMA symbolicAliasNameOrParameter)*
   ;

symbolicAliasNameOrParameter
   : symbolicAliasName
   | parameter["STRING"]
   ;

symbolicAliasName
   : symbolicNameString (DOT symbolicNameString)*
   ;

stringListLiteral
   : LBRACKET (stringLiteral (COMMA stringLiteral)*)? RBRACKET
   ;

stringList
   : stringLiteral (COMMA stringLiteral)+
   ;

stringLiteral
   : STRING_LITERAL1
   | STRING_LITERAL2
   ;

// Should return an Expression
stringOrParameterExpression
   : stringLiteral
   | parameter["STRING"]
   ;

// Should return an Either[String, Parameter]
stringOrParameter
   : stringLiteral
   | parameter["STRING"]
   ;

// Should return an Either[Integer, Parameter]
// There is no unsigned integer Cypher Type so the parameter permits signed values.
uIntOrIntParameter
    :UNSIGNED_DECIMAL_INTEGER
    | parameter["INTEGER"]
    ;

mapOrParameter
   : map
   | parameter["MAP"]
   ;

map
   : LCURLY (propertyKeyName COLON expression (COMMA propertyKeyName COLON expression)*)? RCURLY
   ;

symbolicVariableNameString
   : escapedSymbolicVariableNameString
   | unescapedSymbolicVariableNameString
   ;

escapedSymbolicVariableNameString
   : escapedSymbolicNameString
   ;

unescapedSymbolicVariableNameString
   : unescapedSymbolicNameString
   ;

symbolicNameString
   : escapedSymbolicNameString
   | unescapedSymbolicNameString
   ;

escapedSymbolicNameString
   : ESCAPED_SYMBOLIC_NAME
   ;

unescapedSymbolicNameString
   : unescapedLabelSymbolicNameString
   | NOT
   | NULL
   | TYPED
   | NORMALIZED
   | NFC
   | NFD
   | NFKC
   | NFKD
   ;

symbolicLabelNameString
   : escapedSymbolicNameString
   | unescapedLabelSymbolicNameString
   ;

// Do not remove this, it is needed for composing the grammar
// with other ones (e.g. language support ones)
unescapedLabelSymbolicNameString
   : unescapedLabelSymbolicNameString_
   ;

unescapedLabelSymbolicNameString_
   : IDENTIFIER
   | ACCESS
   | ACTIVE
   | ADMIN
   | ADMINISTRATOR
   | ALIAS
   | ALIASES
   | ALL_SHORTEST_PATHS
   | ALL
   | ALTER
   | AND
   | ANY
   | ARRAY
   | AS
   | ASC
   | ASCENDING
   | ASSERT
   | ASSIGN
   | AT
   | AUTH
   | BINDINGS
   | BOOL
   | BOOLEAN
   | BOOSTED
   | BOTH
   | BREAK
   | BRIEF
   | BTREE
   | BUILT
   | BY
   | CALL
   | CASCADE
   | CASE
   | CHANGE
   | CIDR
   | COLLECT
   | COMMAND
   | COMMANDS
   | COMMIT
   | COMPOSITE
   | CONCURRENT
   | CONSTRAINT
   | CONSTRAINTS
   | CONTAINS
   | CONTINUE
   | COPY
   | COUNT
   | CREATE
   | CSV
   | CURRENT
   | DATA
   | DATABASE
   | DATABASES
   | DATE
   | DATETIME
   | DBMS
   | DEALLOCATE
   | DEFAULT
   | DEFINED
   | DELETE
   | DENY
   | DESC
   | DESCENDING
   | DESTROY
   | DETACH
   | DIFFERENT
   | DISTINCT
   | DRIVER
   | DROP
   | DRYRUN
   | DUMP
   | DURATION
   | EACH
   | EDGE
   | ELEMENT
   | ELEMENTS
   | ELSE
   | ENABLE
   | ENCRYPTED
   | END
   | ENDS
   | ERROR
   | EXECUTABLE
   | EXECUTE
   | EXIST
   | EXISTENCE
   | EXISTS
   | FAIL
   | FALSE
   | FIELDTERMINATOR
   | FINISH
   | FLOAT
   | FOREACH
   | FOR
   | FROM
   | FULLTEXT
   | FUNCTION
   | FUNCTIONS
   | GRANT
   | GRAPH
   | GRAPHS
   | GROUP
   | GROUPS
   | HEADERS
   | HOME
   | ID
   | IF
   | IMMUTABLE
   | IMPERSONATE
   | IN
   | INDEX
   | INDEXES
   | INF
   | INFINITY
   | INSERT
   | INT
   | INTEGER
   | IS
   | JOIN
   | KEY
   | LABEL
   | LABELS
   | LEADING
   | LIMITROWS
   | LIST
   | LOAD
   | LOCAL
   | LOOKUP
   | MATCH
   | MANAGEMENT
   | MAP
   | MERGE
   | NAME
   | NAMES
   | NAN
   | NEW
   | NODE
   | NODETACH
   | NODES
   | NONE
   | NORMALIZE
   | NOTHING
   | NOWAIT
   | OF
   | OFFSET
   | ON
   | ONLY
   | OPTIONAL
   | OPTIONS
   | OPTION
   | OR
   | ORDER
   | OUTPUT
   | PASSWORD
   | PASSWORDS
   | PATH
   | PATHS
   | PERIODIC
   | PLAINTEXT
   | POINT
   | POPULATED
   | PRIMARY
   | PRIMARIES
   | PRIVILEGE
   | PRIVILEGES
   | PROCEDURE
   | PROCEDURES
   | PROPERTIES
   | PROPERTY
   | PROVIDER
   | PROVIDERS
   | RANGE
   | READ
   | REALLOCATE
   | REDUCE
   | REL
   | RELATIONSHIP
   | RELATIONSHIPS
   | REMOVE
   | RENAME
   | REPEATABLE
   | REPLACE
   | REPORT
   | REQUIRE
   | REQUIRED
   | RESTRICT
   | RETURN
   | REVOKE
   | ROLE
   | ROLES
   | ROW
   | ROWS
   | SCAN
   | SECONDARY
   | SECONDARIES
   | SEC
   | SECOND
   | SECONDS
   | SEEK
   | SERVER
   | SERVERS
   | SET
   | SETTING
   | SETTINGS
   | SHORTEST
   | SHORTEST_PATH
   | SHOW
   | SIGNED
   | SINGLE
   | SKIPROWS
   | START
   | STARTS
   | STATUS
   | STOP
   | VARCHAR
   | STRING
   | SUPPORTED
   | SUSPENDED
   | TARGET
   | TERMINATE
   | TEXT
   | THEN
   | TIME
   | TIMESTAMP
   | TIMEZONE
   | TO
   | TOPOLOGY
   | TRAILING
   | TRANSACTION
   | TRANSACTIONS
   | TRAVERSE
   | TRIM
   | TRUE
   | TYPE
   | TYPES
   | UNION
   | UNIQUE
   | UNIQUENESS
   | UNWIND
   | URL
   | USE
   | USER
   | USERS
   | USING
   | VALUE
   | VECTOR
   | VERBOSE
   | VERTEX
   | WAIT
   | WHEN
   | WHERE
   | WITH
   | WITHOUT
   | WRITE
   | XOR
   | YIELD
   | ZONE
   | ZONED
   ;

endOfFile
   : EOF
   ;
