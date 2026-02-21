// Generated from /Users/veritas/Documents/projects/jar-analyzer/src/main/antlr4/org/neo4j/cypher/internal/parser/v5/Cypher5Parser.g4 by ANTLR 4.13.2
package org.neo4j.cypher.internal.parser.v5;

import org.neo4j.cypher.internal.parser.AstRuleCtx;

import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast", "CheckReturnValue", "this-escape"})
public class Cypher5Parser extends Parser {
	static { RuntimeMetaData.checkVersion("4.13.2", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		SPACE=1, SINGLE_LINE_COMMENT=2, MULTI_LINE_COMMENT=3, DECIMAL_DOUBLE=4, 
		UNSIGNED_DECIMAL_INTEGER=5, UNSIGNED_HEX_INTEGER=6, UNSIGNED_OCTAL_INTEGER=7, 
		STRING_LITERAL1=8, STRING_LITERAL2=9, ESCAPED_SYMBOLIC_NAME=10, ACCESS=11, 
		ACTIVE=12, ADMIN=13, ADMINISTRATOR=14, ALIAS=15, ALIASES=16, ALL_SHORTEST_PATHS=17, 
		ALL=18, ALTER=19, AND=20, ANY=21, ARRAY=22, AS=23, ASC=24, ASCENDING=25, 
		ASSERT=26, ASSIGN=27, AT=28, AUTH=29, BAR=30, BINDINGS=31, BOOL=32, BOOLEAN=33, 
		BOOSTED=34, BOTH=35, BREAK=36, BRIEF=37, BTREE=38, BUILT=39, BY=40, CALL=41, 
		CASCADE=42, CASE=43, CHANGE=44, CIDR=45, COLLECT=46, COLON=47, COLONCOLON=48, 
		COMMA=49, COMMAND=50, COMMANDS=51, COMMIT=52, COMPOSITE=53, CONCURRENT=54, 
		CONSTRAINT=55, CONSTRAINTS=56, CONTAINS=57, COPY=58, CONTINUE=59, COUNT=60, 
		CREATE=61, CSV=62, CURRENT=63, DATA=64, DATABASE=65, DATABASES=66, DATE=67, 
		DATETIME=68, DBMS=69, DEALLOCATE=70, DEFAULT=71, DEFINED=72, DELETE=73, 
		DENY=74, DESC=75, DESCENDING=76, DESTROY=77, DETACH=78, DIFFERENT=79, 
		DOLLAR=80, DISTINCT=81, DIVIDE=82, DOT=83, DOTDOT=84, DOUBLEBAR=85, DRIVER=86, 
		DROP=87, DRYRUN=88, DUMP=89, DURATION=90, EACH=91, EDGE=92, ENABLE=93, 
		ELEMENT=94, ELEMENTS=95, ELSE=96, ENCRYPTED=97, END=98, ENDS=99, EQ=100, 
		EXECUTABLE=101, EXECUTE=102, EXIST=103, EXISTENCE=104, EXISTS=105, ERROR=106, 
		FAIL=107, FALSE=108, FIELDTERMINATOR=109, FINISH=110, FLOAT=111, FOR=112, 
		FOREACH=113, FROM=114, FULLTEXT=115, FUNCTION=116, FUNCTIONS=117, GE=118, 
		GRANT=119, GRAPH=120, GRAPHS=121, GROUP=122, GROUPS=123, GT=124, HEADERS=125, 
		HOME=126, ID=127, IF=128, IMPERSONATE=129, IMMUTABLE=130, IN=131, INDEX=132, 
		INDEXES=133, INF=134, INFINITY=135, INSERT=136, INT=137, INTEGER=138, 
		IS=139, JOIN=140, KEY=141, LABEL=142, LABELS=143, AMPERSAND=144, EXCLAMATION_MARK=145, 
		LBRACKET=146, LCURLY=147, LE=148, LEADING=149, LIMITROWS=150, LIST=151, 
		LOAD=152, LOCAL=153, LOOKUP=154, LPAREN=155, LT=156, MANAGEMENT=157, MAP=158, 
		MATCH=159, MERGE=160, MINUS=161, PERCENT=162, INVALID_NEQ=163, NEQ=164, 
		NAME=165, NAMES=166, NAN=167, NFC=168, NFD=169, NFKC=170, NFKD=171, NEW=172, 
		NODE=173, NODETACH=174, NODES=175, NONE=176, NORMALIZE=177, NORMALIZED=178, 
		NOT=179, NOTHING=180, NOWAIT=181, NULL=182, OF=183, OFFSET=184, ON=185, 
		ONLY=186, OPTIONAL=187, OPTIONS=188, OPTION=189, OR=190, ORDER=191, OUTPUT=192, 
		PASSWORD=193, PASSWORDS=194, PATH=195, PATHS=196, PERIODIC=197, PLAINTEXT=198, 
		PLUS=199, PLUSEQUAL=200, POINT=201, POPULATED=202, POW=203, PRIMARY=204, 
		PRIMARIES=205, PRIVILEGE=206, PRIVILEGES=207, PROCEDURE=208, PROCEDURES=209, 
		PROPERTIES=210, PROPERTY=211, PROVIDER=212, PROVIDERS=213, QUESTION=214, 
		RANGE=215, RBRACKET=216, RCURLY=217, READ=218, REALLOCATE=219, REDUCE=220, 
		RENAME=221, REGEQ=222, REL=223, RELATIONSHIP=224, RELATIONSHIPS=225, REMOVE=226, 
		REPEATABLE=227, REPLACE=228, REPORT=229, REQUIRE=230, REQUIRED=231, RESTRICT=232, 
		RETURN=233, REVOKE=234, ROLE=235, ROLES=236, ROW=237, ROWS=238, RPAREN=239, 
		SCAN=240, SEC=241, SECOND=242, SECONDARY=243, SECONDARIES=244, SECONDS=245, 
		SEEK=246, SEMICOLON=247, SERVER=248, SERVERS=249, SET=250, SETTING=251, 
		SETTINGS=252, SHORTEST_PATH=253, SHORTEST=254, SHOW=255, SIGNED=256, SINGLE=257, 
		SKIPROWS=258, START=259, STARTS=260, STATUS=261, STOP=262, STRING=263, 
		SUPPORTED=264, SUSPENDED=265, TARGET=266, TERMINATE=267, TEXT=268, THEN=269, 
		TIME=270, TIMES=271, TIMESTAMP=272, TIMEZONE=273, TO=274, TOPOLOGY=275, 
		TRAILING=276, TRANSACTION=277, TRANSACTIONS=278, TRAVERSE=279, TRIM=280, 
		TRUE=281, TYPE=282, TYPED=283, TYPES=284, UNION=285, UNIQUE=286, UNIQUENESS=287, 
		UNWIND=288, URL=289, USE=290, USER=291, USERS=292, USING=293, VALUE=294, 
		VARCHAR=295, VECTOR=296, VERBOSE=297, VERTEX=298, WAIT=299, WHEN=300, 
		WHERE=301, WITH=302, WITHOUT=303, WRITE=304, XOR=305, YIELD=306, ZONE=307, 
		ZONED=308, IDENTIFIER=309, ARROW_LINE=310, ARROW_LEFT_HEAD=311, ARROW_RIGHT_HEAD=312, 
		ErrorChar=313;
	public static final int
		RULE_statements = 0, RULE_statement = 1, RULE_periodicCommitQueryHintFailure = 2, 
		RULE_regularQuery = 3, RULE_singleQuery = 4, RULE_clause = 5, RULE_useClause = 6, 
		RULE_graphReference = 7, RULE_finishClause = 8, RULE_returnClause = 9, 
		RULE_returnBody = 10, RULE_returnItem = 11, RULE_returnItems = 12, RULE_orderItem = 13, 
		RULE_ascToken = 14, RULE_descToken = 15, RULE_orderBy = 16, RULE_skip = 17, 
		RULE_limit = 18, RULE_whereClause = 19, RULE_withClause = 20, RULE_createClause = 21, 
		RULE_insertClause = 22, RULE_setClause = 23, RULE_setItem = 24, RULE_removeClause = 25, 
		RULE_removeItem = 26, RULE_deleteClause = 27, RULE_matchClause = 28, RULE_matchMode = 29, 
		RULE_hint = 30, RULE_mergeClause = 31, RULE_mergeAction = 32, RULE_unwindClause = 33, 
		RULE_callClause = 34, RULE_procedureName = 35, RULE_procedureArgument = 36, 
		RULE_procedureResultItem = 37, RULE_loadCSVClause = 38, RULE_foreachClause = 39, 
		RULE_subqueryClause = 40, RULE_subqueryScope = 41, RULE_subqueryInTransactionsParameters = 42, 
		RULE_subqueryInTransactionsBatchParameters = 43, RULE_subqueryInTransactionsErrorParameters = 44, 
		RULE_subqueryInTransactionsReportParameters = 45, RULE_orderBySkipLimitClause = 46, 
		RULE_patternList = 47, RULE_insertPatternList = 48, RULE_pattern = 49, 
		RULE_insertPattern = 50, RULE_quantifier = 51, RULE_anonymousPattern = 52, 
		RULE_shortestPathPattern = 53, RULE_patternElement = 54, RULE_selector = 55, 
		RULE_groupToken = 56, RULE_pathToken = 57, RULE_pathPatternNonEmpty = 58, 
		RULE_nodePattern = 59, RULE_insertNodePattern = 60, RULE_parenthesizedPath = 61, 
		RULE_nodeLabels = 62, RULE_nodeLabelsIs = 63, RULE_dynamicExpression = 64, 
		RULE_dynamicAnyAllExpression = 65, RULE_dynamicLabelType = 66, RULE_labelType = 67, 
		RULE_relType = 68, RULE_labelOrRelType = 69, RULE_properties = 70, RULE_relationshipPattern = 71, 
		RULE_insertRelationshipPattern = 72, RULE_leftArrow = 73, RULE_arrowLine = 74, 
		RULE_rightArrow = 75, RULE_pathLength = 76, RULE_labelExpression = 77, 
		RULE_labelExpression4 = 78, RULE_labelExpression4Is = 79, RULE_labelExpression3 = 80, 
		RULE_labelExpression3Is = 81, RULE_labelExpression2 = 82, RULE_labelExpression2Is = 83, 
		RULE_labelExpression1 = 84, RULE_labelExpression1Is = 85, RULE_insertNodeLabelExpression = 86, 
		RULE_insertRelationshipLabelExpression = 87, RULE_expression = 88, RULE_expression11 = 89, 
		RULE_expression10 = 90, RULE_expression9 = 91, RULE_expression8 = 92, 
		RULE_expression7 = 93, RULE_comparisonExpression6 = 94, RULE_normalForm = 95, 
		RULE_expression6 = 96, RULE_expression5 = 97, RULE_expression4 = 98, RULE_expression3 = 99, 
		RULE_expression2 = 100, RULE_postFix = 101, RULE_property = 102, RULE_dynamicProperty = 103, 
		RULE_propertyExpression = 104, RULE_dynamicPropertyExpression = 105, RULE_expression1 = 106, 
		RULE_literal = 107, RULE_caseExpression = 108, RULE_caseAlternative = 109, 
		RULE_extendedCaseExpression = 110, RULE_extendedCaseAlternative = 111, 
		RULE_extendedWhen = 112, RULE_listComprehension = 113, RULE_patternComprehension = 114, 
		RULE_reduceExpression = 115, RULE_listItemsPredicate = 116, RULE_normalizeFunction = 117, 
		RULE_trimFunction = 118, RULE_patternExpression = 119, RULE_shortestPathExpression = 120, 
		RULE_parenthesizedExpression = 121, RULE_mapProjection = 122, RULE_mapProjectionElement = 123, 
		RULE_countStar = 124, RULE_existsExpression = 125, RULE_countExpression = 126, 
		RULE_collectExpression = 127, RULE_numberLiteral = 128, RULE_signedIntegerLiteral = 129, 
		RULE_listLiteral = 130, RULE_propertyKeyName = 131, RULE_parameter = 132, 
		RULE_parameterName = 133, RULE_functionInvocation = 134, RULE_functionArgument = 135, 
		RULE_functionName = 136, RULE_namespace = 137, RULE_variable = 138, RULE_nonEmptyNameList = 139, 
		RULE_type = 140, RULE_typePart = 141, RULE_typeName = 142, RULE_typeNullability = 143, 
		RULE_typeListSuffix = 144, RULE_command = 145, RULE_createCommand = 146, 
		RULE_dropCommand = 147, RULE_showCommand = 148, RULE_showCommandYield = 149, 
		RULE_yieldItem = 150, RULE_yieldSkip = 151, RULE_yieldLimit = 152, RULE_yieldClause = 153, 
		RULE_commandOptions = 154, RULE_terminateCommand = 155, RULE_composableCommandClauses = 156, 
		RULE_composableShowCommandClauses = 157, RULE_showBriefAndYield = 158, 
		RULE_showIndexCommand = 159, RULE_showIndexesAllowBrief = 160, RULE_showIndexesNoBrief = 161, 
		RULE_showConstraintCommand = 162, RULE_constraintAllowYieldType = 163, 
		RULE_constraintExistType = 164, RULE_constraintBriefAndYieldType = 165, 
		RULE_showConstraintsAllowBriefAndYield = 166, RULE_showConstraintsAllowBrief = 167, 
		RULE_showConstraintsAllowYield = 168, RULE_showProcedures = 169, RULE_showFunctions = 170, 
		RULE_functionToken = 171, RULE_executableBy = 172, RULE_showFunctionsType = 173, 
		RULE_showTransactions = 174, RULE_terminateTransactions = 175, RULE_showSettings = 176, 
		RULE_settingToken = 177, RULE_namesAndClauses = 178, RULE_stringsOrExpression = 179, 
		RULE_commandNodePattern = 180, RULE_commandRelPattern = 181, RULE_createConstraint = 182, 
		RULE_constraintType = 183, RULE_dropConstraint = 184, RULE_createIndex = 185, 
		RULE_oldCreateIndex = 186, RULE_createIndex_ = 187, RULE_createFulltextIndex = 188, 
		RULE_fulltextNodePattern = 189, RULE_fulltextRelPattern = 190, RULE_createLookupIndex = 191, 
		RULE_lookupIndexNodePattern = 192, RULE_lookupIndexRelPattern = 193, RULE_dropIndex = 194, 
		RULE_propertyList = 195, RULE_enclosedPropertyList = 196, RULE_alterCommand = 197, 
		RULE_renameCommand = 198, RULE_grantCommand = 199, RULE_denyCommand = 200, 
		RULE_revokeCommand = 201, RULE_userNames = 202, RULE_roleNames = 203, 
		RULE_roleToken = 204, RULE_enableServerCommand = 205, RULE_alterServer = 206, 
		RULE_renameServer = 207, RULE_dropServer = 208, RULE_showServers = 209, 
		RULE_allocationCommand = 210, RULE_deallocateDatabaseFromServers = 211, 
		RULE_reallocateDatabases = 212, RULE_createRole = 213, RULE_dropRole = 214, 
		RULE_renameRole = 215, RULE_showRoles = 216, RULE_grantRole = 217, RULE_revokeRole = 218, 
		RULE_createUser = 219, RULE_dropUser = 220, RULE_renameUser = 221, RULE_alterCurrentUser = 222, 
		RULE_alterUser = 223, RULE_removeNamedProvider = 224, RULE_password = 225, 
		RULE_passwordOnly = 226, RULE_passwordExpression = 227, RULE_passwordChangeRequired = 228, 
		RULE_userStatus = 229, RULE_homeDatabase = 230, RULE_setAuthClause = 231, 
		RULE_userAuthAttribute = 232, RULE_showUsers = 233, RULE_showCurrentUser = 234, 
		RULE_showSupportedPrivileges = 235, RULE_showPrivileges = 236, RULE_showRolePrivileges = 237, 
		RULE_showUserPrivileges = 238, RULE_privilegeAsCommand = 239, RULE_privilegeToken = 240, 
		RULE_privilege = 241, RULE_allPrivilege = 242, RULE_allPrivilegeType = 243, 
		RULE_allPrivilegeTarget = 244, RULE_createPrivilege = 245, RULE_createPrivilegeForDatabase = 246, 
		RULE_createNodePrivilegeToken = 247, RULE_createRelPrivilegeToken = 248, 
		RULE_createPropertyPrivilegeToken = 249, RULE_actionForDBMS = 250, RULE_dropPrivilege = 251, 
		RULE_loadPrivilege = 252, RULE_showPrivilege = 253, RULE_setPrivilege = 254, 
		RULE_passwordToken = 255, RULE_removePrivilege = 256, RULE_writePrivilege = 257, 
		RULE_databasePrivilege = 258, RULE_dbmsPrivilege = 259, RULE_dbmsPrivilegeExecute = 260, 
		RULE_adminToken = 261, RULE_procedureToken = 262, RULE_indexToken = 263, 
		RULE_constraintToken = 264, RULE_transactionToken = 265, RULE_userQualifier = 266, 
		RULE_executeFunctionQualifier = 267, RULE_executeProcedureQualifier = 268, 
		RULE_settingQualifier = 269, RULE_globs = 270, RULE_glob = 271, RULE_globRecursive = 272, 
		RULE_globPart = 273, RULE_qualifiedGraphPrivilegesWithProperty = 274, 
		RULE_qualifiedGraphPrivileges = 275, RULE_labelsResource = 276, RULE_propertiesResource = 277, 
		RULE_nonEmptyStringList = 278, RULE_graphQualifier = 279, RULE_graphQualifierToken = 280, 
		RULE_relToken = 281, RULE_elementToken = 282, RULE_nodeToken = 283, RULE_databaseScope = 284, 
		RULE_graphScope = 285, RULE_createCompositeDatabase = 286, RULE_createDatabase = 287, 
		RULE_primaryTopology = 288, RULE_primaryToken = 289, RULE_secondaryTopology = 290, 
		RULE_secondaryToken = 291, RULE_dropDatabase = 292, RULE_aliasAction = 293, 
		RULE_alterDatabase = 294, RULE_alterDatabaseAccess = 295, RULE_alterDatabaseTopology = 296, 
		RULE_alterDatabaseOption = 297, RULE_startDatabase = 298, RULE_stopDatabase = 299, 
		RULE_waitClause = 300, RULE_secondsToken = 301, RULE_showDatabase = 302, 
		RULE_aliasName = 303, RULE_databaseName = 304, RULE_createAlias = 305, 
		RULE_dropAlias = 306, RULE_alterAlias = 307, RULE_alterAliasTarget = 308, 
		RULE_alterAliasUser = 309, RULE_alterAliasPassword = 310, RULE_alterAliasDriver = 311, 
		RULE_alterAliasProperties = 312, RULE_showAliases = 313, RULE_symbolicNameOrStringParameter = 314, 
		RULE_commandNameExpression = 315, RULE_symbolicNameOrStringParameterList = 316, 
		RULE_symbolicAliasNameList = 317, RULE_symbolicAliasNameOrParameter = 318, 
		RULE_symbolicAliasName = 319, RULE_stringListLiteral = 320, RULE_stringList = 321, 
		RULE_stringLiteral = 322, RULE_stringOrParameterExpression = 323, RULE_stringOrParameter = 324, 
		RULE_uIntOrIntParameter = 325, RULE_mapOrParameter = 326, RULE_map = 327, 
		RULE_symbolicVariableNameString = 328, RULE_escapedSymbolicVariableNameString = 329, 
		RULE_unescapedSymbolicVariableNameString = 330, RULE_symbolicNameString = 331, 
		RULE_escapedSymbolicNameString = 332, RULE_unescapedSymbolicNameString = 333, 
		RULE_symbolicLabelNameString = 334, RULE_unescapedLabelSymbolicNameString = 335, 
		RULE_unescapedLabelSymbolicNameString_ = 336, RULE_endOfFile = 337;
	private static String[] makeRuleNames() {
		return new String[] {
			"statements", "statement", "periodicCommitQueryHintFailure", "regularQuery", 
			"singleQuery", "clause", "useClause", "graphReference", "finishClause", 
			"returnClause", "returnBody", "returnItem", "returnItems", "orderItem", 
			"ascToken", "descToken", "orderBy", "skip", "limit", "whereClause", "withClause", 
			"createClause", "insertClause", "setClause", "setItem", "removeClause", 
			"removeItem", "deleteClause", "matchClause", "matchMode", "hint", "mergeClause", 
			"mergeAction", "unwindClause", "callClause", "procedureName", "procedureArgument", 
			"procedureResultItem", "loadCSVClause", "foreachClause", "subqueryClause", 
			"subqueryScope", "subqueryInTransactionsParameters", "subqueryInTransactionsBatchParameters", 
			"subqueryInTransactionsErrorParameters", "subqueryInTransactionsReportParameters", 
			"orderBySkipLimitClause", "patternList", "insertPatternList", "pattern", 
			"insertPattern", "quantifier", "anonymousPattern", "shortestPathPattern", 
			"patternElement", "selector", "groupToken", "pathToken", "pathPatternNonEmpty", 
			"nodePattern", "insertNodePattern", "parenthesizedPath", "nodeLabels", 
			"nodeLabelsIs", "dynamicExpression", "dynamicAnyAllExpression", "dynamicLabelType", 
			"labelType", "relType", "labelOrRelType", "properties", "relationshipPattern", 
			"insertRelationshipPattern", "leftArrow", "arrowLine", "rightArrow", 
			"pathLength", "labelExpression", "labelExpression4", "labelExpression4Is", 
			"labelExpression3", "labelExpression3Is", "labelExpression2", "labelExpression2Is", 
			"labelExpression1", "labelExpression1Is", "insertNodeLabelExpression", 
			"insertRelationshipLabelExpression", "expression", "expression11", "expression10", 
			"expression9", "expression8", "expression7", "comparisonExpression6", 
			"normalForm", "expression6", "expression5", "expression4", "expression3", 
			"expression2", "postFix", "property", "dynamicProperty", "propertyExpression", 
			"dynamicPropertyExpression", "expression1", "literal", "caseExpression", 
			"caseAlternative", "extendedCaseExpression", "extendedCaseAlternative", 
			"extendedWhen", "listComprehension", "patternComprehension", "reduceExpression", 
			"listItemsPredicate", "normalizeFunction", "trimFunction", "patternExpression", 
			"shortestPathExpression", "parenthesizedExpression", "mapProjection", 
			"mapProjectionElement", "countStar", "existsExpression", "countExpression", 
			"collectExpression", "numberLiteral", "signedIntegerLiteral", "listLiteral", 
			"propertyKeyName", "parameter", "parameterName", "functionInvocation", 
			"functionArgument", "functionName", "namespace", "variable", "nonEmptyNameList", 
			"type", "typePart", "typeName", "typeNullability", "typeListSuffix", 
			"command", "createCommand", "dropCommand", "showCommand", "showCommandYield", 
			"yieldItem", "yieldSkip", "yieldLimit", "yieldClause", "commandOptions", 
			"terminateCommand", "composableCommandClauses", "composableShowCommandClauses", 
			"showBriefAndYield", "showIndexCommand", "showIndexesAllowBrief", "showIndexesNoBrief", 
			"showConstraintCommand", "constraintAllowYieldType", "constraintExistType", 
			"constraintBriefAndYieldType", "showConstraintsAllowBriefAndYield", "showConstraintsAllowBrief", 
			"showConstraintsAllowYield", "showProcedures", "showFunctions", "functionToken", 
			"executableBy", "showFunctionsType", "showTransactions", "terminateTransactions", 
			"showSettings", "settingToken", "namesAndClauses", "stringsOrExpression", 
			"commandNodePattern", "commandRelPattern", "createConstraint", "constraintType", 
			"dropConstraint", "createIndex", "oldCreateIndex", "createIndex_", "createFulltextIndex", 
			"fulltextNodePattern", "fulltextRelPattern", "createLookupIndex", "lookupIndexNodePattern", 
			"lookupIndexRelPattern", "dropIndex", "propertyList", "enclosedPropertyList", 
			"alterCommand", "renameCommand", "grantCommand", "denyCommand", "revokeCommand", 
			"userNames", "roleNames", "roleToken", "enableServerCommand", "alterServer", 
			"renameServer", "dropServer", "showServers", "allocationCommand", "deallocateDatabaseFromServers", 
			"reallocateDatabases", "createRole", "dropRole", "renameRole", "showRoles", 
			"grantRole", "revokeRole", "createUser", "dropUser", "renameUser", "alterCurrentUser", 
			"alterUser", "removeNamedProvider", "password", "passwordOnly", "passwordExpression", 
			"passwordChangeRequired", "userStatus", "homeDatabase", "setAuthClause", 
			"userAuthAttribute", "showUsers", "showCurrentUser", "showSupportedPrivileges", 
			"showPrivileges", "showRolePrivileges", "showUserPrivileges", "privilegeAsCommand", 
			"privilegeToken", "privilege", "allPrivilege", "allPrivilegeType", "allPrivilegeTarget", 
			"createPrivilege", "createPrivilegeForDatabase", "createNodePrivilegeToken", 
			"createRelPrivilegeToken", "createPropertyPrivilegeToken", "actionForDBMS", 
			"dropPrivilege", "loadPrivilege", "showPrivilege", "setPrivilege", "passwordToken", 
			"removePrivilege", "writePrivilege", "databasePrivilege", "dbmsPrivilege", 
			"dbmsPrivilegeExecute", "adminToken", "procedureToken", "indexToken", 
			"constraintToken", "transactionToken", "userQualifier", "executeFunctionQualifier", 
			"executeProcedureQualifier", "settingQualifier", "globs", "glob", "globRecursive", 
			"globPart", "qualifiedGraphPrivilegesWithProperty", "qualifiedGraphPrivileges", 
			"labelsResource", "propertiesResource", "nonEmptyStringList", "graphQualifier", 
			"graphQualifierToken", "relToken", "elementToken", "nodeToken", "databaseScope", 
			"graphScope", "createCompositeDatabase", "createDatabase", "primaryTopology", 
			"primaryToken", "secondaryTopology", "secondaryToken", "dropDatabase", 
			"aliasAction", "alterDatabase", "alterDatabaseAccess", "alterDatabaseTopology", 
			"alterDatabaseOption", "startDatabase", "stopDatabase", "waitClause", 
			"secondsToken", "showDatabase", "aliasName", "databaseName", "createAlias", 
			"dropAlias", "alterAlias", "alterAliasTarget", "alterAliasUser", "alterAliasPassword", 
			"alterAliasDriver", "alterAliasProperties", "showAliases", "symbolicNameOrStringParameter", 
			"commandNameExpression", "symbolicNameOrStringParameterList", "symbolicAliasNameList", 
			"symbolicAliasNameOrParameter", "symbolicAliasName", "stringListLiteral", 
			"stringList", "stringLiteral", "stringOrParameterExpression", "stringOrParameter", 
			"uIntOrIntParameter", "mapOrParameter", "map", "symbolicVariableNameString", 
			"escapedSymbolicVariableNameString", "unescapedSymbolicVariableNameString", 
			"symbolicNameString", "escapedSymbolicNameString", "unescapedSymbolicNameString", 
			"symbolicLabelNameString", "unescapedLabelSymbolicNameString", "unescapedLabelSymbolicNameString_", 
			"endOfFile"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, "'|'", null, null, null, null, null, 
			null, null, null, null, null, null, null, null, null, null, null, "':'", 
			"'::'", "','", null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, null, null, "'$'", null, "'/'", 
			"'.'", "'..'", "'||'", null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, "'='", null, null, null, null, null, 
			null, null, null, null, null, null, null, null, null, null, null, null, 
			"'>='", null, null, null, null, null, "'>'", null, null, null, null, 
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, "'&'", "'!'", "'['", "'{'", "'<='", null, null, null, 
			null, null, null, "'('", "'<'", null, null, null, null, "'-'", "'%'", 
			"'!='", "'<>'", null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, "'+'", "'+='", null, null, "'^'", null, null, null, null, null, 
			null, null, null, null, null, "'?'", null, "']'", "'}'", null, null, 
			null, null, "'=~'", null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, "')'", null, null, null, null, 
			null, null, null, "';'", null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, "'*'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "SPACE", "SINGLE_LINE_COMMENT", "MULTI_LINE_COMMENT", "DECIMAL_DOUBLE", 
			"UNSIGNED_DECIMAL_INTEGER", "UNSIGNED_HEX_INTEGER", "UNSIGNED_OCTAL_INTEGER", 
			"STRING_LITERAL1", "STRING_LITERAL2", "ESCAPED_SYMBOLIC_NAME", "ACCESS", 
			"ACTIVE", "ADMIN", "ADMINISTRATOR", "ALIAS", "ALIASES", "ALL_SHORTEST_PATHS", 
			"ALL", "ALTER", "AND", "ANY", "ARRAY", "AS", "ASC", "ASCENDING", "ASSERT", 
			"ASSIGN", "AT", "AUTH", "BAR", "BINDINGS", "BOOL", "BOOLEAN", "BOOSTED", 
			"BOTH", "BREAK", "BRIEF", "BTREE", "BUILT", "BY", "CALL", "CASCADE", 
			"CASE", "CHANGE", "CIDR", "COLLECT", "COLON", "COLONCOLON", "COMMA", 
			"COMMAND", "COMMANDS", "COMMIT", "COMPOSITE", "CONCURRENT", "CONSTRAINT", 
			"CONSTRAINTS", "CONTAINS", "COPY", "CONTINUE", "COUNT", "CREATE", "CSV", 
			"CURRENT", "DATA", "DATABASE", "DATABASES", "DATE", "DATETIME", "DBMS", 
			"DEALLOCATE", "DEFAULT", "DEFINED", "DELETE", "DENY", "DESC", "DESCENDING", 
			"DESTROY", "DETACH", "DIFFERENT", "DOLLAR", "DISTINCT", "DIVIDE", "DOT", 
			"DOTDOT", "DOUBLEBAR", "DRIVER", "DROP", "DRYRUN", "DUMP", "DURATION", 
			"EACH", "EDGE", "ENABLE", "ELEMENT", "ELEMENTS", "ELSE", "ENCRYPTED", 
			"END", "ENDS", "EQ", "EXECUTABLE", "EXECUTE", "EXIST", "EXISTENCE", "EXISTS", 
			"ERROR", "FAIL", "FALSE", "FIELDTERMINATOR", "FINISH", "FLOAT", "FOR", 
			"FOREACH", "FROM", "FULLTEXT", "FUNCTION", "FUNCTIONS", "GE", "GRANT", 
			"GRAPH", "GRAPHS", "GROUP", "GROUPS", "GT", "HEADERS", "HOME", "ID", 
			"IF", "IMPERSONATE", "IMMUTABLE", "IN", "INDEX", "INDEXES", "INF", "INFINITY", 
			"INSERT", "INT", "INTEGER", "IS", "JOIN", "KEY", "LABEL", "LABELS", "AMPERSAND", 
			"EXCLAMATION_MARK", "LBRACKET", "LCURLY", "LE", "LEADING", "LIMITROWS", 
			"LIST", "LOAD", "LOCAL", "LOOKUP", "LPAREN", "LT", "MANAGEMENT", "MAP", 
			"MATCH", "MERGE", "MINUS", "PERCENT", "INVALID_NEQ", "NEQ", "NAME", "NAMES", 
			"NAN", "NFC", "NFD", "NFKC", "NFKD", "NEW", "NODE", "NODETACH", "NODES", 
			"NONE", "NORMALIZE", "NORMALIZED", "NOT", "NOTHING", "NOWAIT", "NULL", 
			"OF", "OFFSET", "ON", "ONLY", "OPTIONAL", "OPTIONS", "OPTION", "OR", 
			"ORDER", "OUTPUT", "PASSWORD", "PASSWORDS", "PATH", "PATHS", "PERIODIC", 
			"PLAINTEXT", "PLUS", "PLUSEQUAL", "POINT", "POPULATED", "POW", "PRIMARY", 
			"PRIMARIES", "PRIVILEGE", "PRIVILEGES", "PROCEDURE", "PROCEDURES", "PROPERTIES", 
			"PROPERTY", "PROVIDER", "PROVIDERS", "QUESTION", "RANGE", "RBRACKET", 
			"RCURLY", "READ", "REALLOCATE", "REDUCE", "RENAME", "REGEQ", "REL", "RELATIONSHIP", 
			"RELATIONSHIPS", "REMOVE", "REPEATABLE", "REPLACE", "REPORT", "REQUIRE", 
			"REQUIRED", "RESTRICT", "RETURN", "REVOKE", "ROLE", "ROLES", "ROW", "ROWS", 
			"RPAREN", "SCAN", "SEC", "SECOND", "SECONDARY", "SECONDARIES", "SECONDS", 
			"SEEK", "SEMICOLON", "SERVER", "SERVERS", "SET", "SETTING", "SETTINGS", 
			"SHORTEST_PATH", "SHORTEST", "SHOW", "SIGNED", "SINGLE", "SKIPROWS", 
			"START", "STARTS", "STATUS", "STOP", "STRING", "SUPPORTED", "SUSPENDED", 
			"TARGET", "TERMINATE", "TEXT", "THEN", "TIME", "TIMES", "TIMESTAMP", 
			"TIMEZONE", "TO", "TOPOLOGY", "TRAILING", "TRANSACTION", "TRANSACTIONS", 
			"TRAVERSE", "TRIM", "TRUE", "TYPE", "TYPED", "TYPES", "UNION", "UNIQUE", 
			"UNIQUENESS", "UNWIND", "URL", "USE", "USER", "USERS", "USING", "VALUE", 
			"VARCHAR", "VECTOR", "VERBOSE", "VERTEX", "WAIT", "WHEN", "WHERE", "WITH", 
			"WITHOUT", "WRITE", "XOR", "YIELD", "ZONE", "ZONED", "IDENTIFIER", "ARROW_LINE", 
			"ARROW_LEFT_HEAD", "ARROW_RIGHT_HEAD", "ErrorChar"
		};
	}
	private static final String[] _SYMBOLIC_NAMES = makeSymbolicNames();
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}

	@Override
	public String getGrammarFileName() { return "Cypher5Parser.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public Cypher5Parser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@SuppressWarnings("CheckReturnValue")
	public static class StatementsContext extends AstRuleCtx {
		public List<StatementContext> statement() {
			return getRuleContexts(StatementContext.class);
		}
		public StatementContext statement(int i) {
			return getRuleContext(StatementContext.class,i);
		}
		public TerminalNode EOF() { return getToken(Cypher5Parser.EOF, 0); }
		public List<TerminalNode> SEMICOLON() { return getTokens(Cypher5Parser.SEMICOLON); }
		public TerminalNode SEMICOLON(int i) {
			return getToken(Cypher5Parser.SEMICOLON, i);
		}
		public StatementsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_statements; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterStatements(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitStatements(this);
		}
	}

	public final StatementsContext statements() throws RecognitionException {
		StatementsContext _localctx = new StatementsContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_statements);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(676);
			statement();
			setState(681);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,0,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(677);
					match(SEMICOLON);
					setState(678);
					statement();
					}
					} 
				}
				setState(683);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,0,_ctx);
			}
			setState(685);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==SEMICOLON) {
				{
				setState(684);
				match(SEMICOLON);
				}
			}

			setState(687);
			match(EOF);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class StatementContext extends AstRuleCtx {
		public RegularQueryContext regularQuery() {
			return getRuleContext(RegularQueryContext.class,0);
		}
		public PeriodicCommitQueryHintFailureContext periodicCommitQueryHintFailure() {
			return getRuleContext(PeriodicCommitQueryHintFailureContext.class,0);
		}
		public StatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_statement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitStatement(this);
		}
	}

	public final StatementContext statement() throws RecognitionException {
		StatementContext _localctx = new StatementContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_statement);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(690);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==USING) {
				{
				setState(689);
				periodicCommitQueryHintFailure();
				}
			}

			setState(692);
			regularQuery();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class PeriodicCommitQueryHintFailureContext extends AstRuleCtx {
		public TerminalNode USING() { return getToken(Cypher5Parser.USING, 0); }
		public TerminalNode PERIODIC() { return getToken(Cypher5Parser.PERIODIC, 0); }
		public TerminalNode COMMIT() { return getToken(Cypher5Parser.COMMIT, 0); }
		public TerminalNode UNSIGNED_DECIMAL_INTEGER() { return getToken(Cypher5Parser.UNSIGNED_DECIMAL_INTEGER, 0); }
		public PeriodicCommitQueryHintFailureContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_periodicCommitQueryHintFailure; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterPeriodicCommitQueryHintFailure(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitPeriodicCommitQueryHintFailure(this);
		}
	}

	public final PeriodicCommitQueryHintFailureContext periodicCommitQueryHintFailure() throws RecognitionException {
		PeriodicCommitQueryHintFailureContext _localctx = new PeriodicCommitQueryHintFailureContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_periodicCommitQueryHintFailure);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(694);
			match(USING);
			setState(695);
			match(PERIODIC);
			setState(696);
			match(COMMIT);
			setState(698);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==UNSIGNED_DECIMAL_INTEGER) {
				{
				setState(697);
				match(UNSIGNED_DECIMAL_INTEGER);
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class RegularQueryContext extends AstRuleCtx {
		public List<SingleQueryContext> singleQuery() {
			return getRuleContexts(SingleQueryContext.class);
		}
		public SingleQueryContext singleQuery(int i) {
			return getRuleContext(SingleQueryContext.class,i);
		}
		public List<TerminalNode> UNION() { return getTokens(Cypher5Parser.UNION); }
		public TerminalNode UNION(int i) {
			return getToken(Cypher5Parser.UNION, i);
		}
		public List<TerminalNode> ALL() { return getTokens(Cypher5Parser.ALL); }
		public TerminalNode ALL(int i) {
			return getToken(Cypher5Parser.ALL, i);
		}
		public List<TerminalNode> DISTINCT() { return getTokens(Cypher5Parser.DISTINCT); }
		public TerminalNode DISTINCT(int i) {
			return getToken(Cypher5Parser.DISTINCT, i);
		}
		public RegularQueryContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_regularQuery; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterRegularQuery(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitRegularQuery(this);
		}
	}

	public final RegularQueryContext regularQuery() throws RecognitionException {
		RegularQueryContext _localctx = new RegularQueryContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_regularQuery);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(700);
			singleQuery();
			setState(708);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==UNION) {
				{
				{
				setState(701);
				match(UNION);
				setState(703);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==ALL || _la==DISTINCT) {
					{
					setState(702);
					_la = _input.LA(1);
					if ( !(_la==ALL || _la==DISTINCT) ) {
					_errHandler.recoverInline(this);
					}
					else {
						if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
						_errHandler.reportMatch(this);
						consume();
					}
					}
				}

				setState(705);
				singleQuery();
				}
				}
				setState(710);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class SingleQueryContext extends AstRuleCtx {
		public List<ClauseContext> clause() {
			return getRuleContexts(ClauseContext.class);
		}
		public ClauseContext clause(int i) {
			return getRuleContext(ClauseContext.class,i);
		}
		public SingleQueryContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_singleQuery; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterSingleQuery(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitSingleQuery(this);
		}
	}

	public final SingleQueryContext singleQuery() throws RecognitionException {
		SingleQueryContext _localctx = new SingleQueryContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_singleQuery);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(712); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(711);
				clause();
				}
				}
				setState(714); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( ((((_la - 41)) & ~0x3f) == 0 && ((1L << (_la - 41)) & 141734969345L) != 0) || ((((_la - 110)) & ~0x3f) == 0 && ((1L << (_la - 110)) & 1694347485511689L) != 0) || ((((_la - 174)) & ~0x3f) == 0 && ((1L << (_la - 174)) & 580964351930934273L) != 0) || ((((_la - 250)) & ~0x3f) == 0 && ((1L << (_la - 250)) & 4504974016905473L) != 0) );
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ClauseContext extends AstRuleCtx {
		public UseClauseContext useClause() {
			return getRuleContext(UseClauseContext.class,0);
		}
		public FinishClauseContext finishClause() {
			return getRuleContext(FinishClauseContext.class,0);
		}
		public ReturnClauseContext returnClause() {
			return getRuleContext(ReturnClauseContext.class,0);
		}
		public CreateClauseContext createClause() {
			return getRuleContext(CreateClauseContext.class,0);
		}
		public InsertClauseContext insertClause() {
			return getRuleContext(InsertClauseContext.class,0);
		}
		public DeleteClauseContext deleteClause() {
			return getRuleContext(DeleteClauseContext.class,0);
		}
		public SetClauseContext setClause() {
			return getRuleContext(SetClauseContext.class,0);
		}
		public RemoveClauseContext removeClause() {
			return getRuleContext(RemoveClauseContext.class,0);
		}
		public MatchClauseContext matchClause() {
			return getRuleContext(MatchClauseContext.class,0);
		}
		public MergeClauseContext mergeClause() {
			return getRuleContext(MergeClauseContext.class,0);
		}
		public WithClauseContext withClause() {
			return getRuleContext(WithClauseContext.class,0);
		}
		public UnwindClauseContext unwindClause() {
			return getRuleContext(UnwindClauseContext.class,0);
		}
		public CallClauseContext callClause() {
			return getRuleContext(CallClauseContext.class,0);
		}
		public SubqueryClauseContext subqueryClause() {
			return getRuleContext(SubqueryClauseContext.class,0);
		}
		public LoadCSVClauseContext loadCSVClause() {
			return getRuleContext(LoadCSVClauseContext.class,0);
		}
		public ForeachClauseContext foreachClause() {
			return getRuleContext(ForeachClauseContext.class,0);
		}
		public OrderBySkipLimitClauseContext orderBySkipLimitClause() {
			return getRuleContext(OrderBySkipLimitClauseContext.class,0);
		}
		public ClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_clause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitClause(this);
		}
	}

	public final ClauseContext clause() throws RecognitionException {
		ClauseContext _localctx = new ClauseContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_clause);
		try {
			setState(733);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,7,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(716);
				useClause();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(717);
				finishClause();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(718);
				returnClause();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(719);
				createClause();
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(720);
				insertClause();
				}
				break;
			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(721);
				deleteClause();
				}
				break;
			case 7:
				enterOuterAlt(_localctx, 7);
				{
				setState(722);
				setClause();
				}
				break;
			case 8:
				enterOuterAlt(_localctx, 8);
				{
				setState(723);
				removeClause();
				}
				break;
			case 9:
				enterOuterAlt(_localctx, 9);
				{
				setState(724);
				matchClause();
				}
				break;
			case 10:
				enterOuterAlt(_localctx, 10);
				{
				setState(725);
				mergeClause();
				}
				break;
			case 11:
				enterOuterAlt(_localctx, 11);
				{
				setState(726);
				withClause();
				}
				break;
			case 12:
				enterOuterAlt(_localctx, 12);
				{
				setState(727);
				unwindClause();
				}
				break;
			case 13:
				enterOuterAlt(_localctx, 13);
				{
				setState(728);
				callClause();
				}
				break;
			case 14:
				enterOuterAlt(_localctx, 14);
				{
				setState(729);
				subqueryClause();
				}
				break;
			case 15:
				enterOuterAlt(_localctx, 15);
				{
				setState(730);
				loadCSVClause();
				}
				break;
			case 16:
				enterOuterAlt(_localctx, 16);
				{
				setState(731);
				foreachClause();
				}
				break;
			case 17:
				enterOuterAlt(_localctx, 17);
				{
				setState(732);
				orderBySkipLimitClause();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class UseClauseContext extends AstRuleCtx {
		public TerminalNode USE() { return getToken(Cypher5Parser.USE, 0); }
		public GraphReferenceContext graphReference() {
			return getRuleContext(GraphReferenceContext.class,0);
		}
		public TerminalNode GRAPH() { return getToken(Cypher5Parser.GRAPH, 0); }
		public UseClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_useClause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterUseClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitUseClause(this);
		}
	}

	public final UseClauseContext useClause() throws RecognitionException {
		UseClauseContext _localctx = new UseClauseContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_useClause);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(735);
			match(USE);
			setState(737);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,8,_ctx) ) {
			case 1:
				{
				setState(736);
				match(GRAPH);
				}
				break;
			}
			setState(739);
			graphReference();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class GraphReferenceContext extends AstRuleCtx {
		public TerminalNode LPAREN() { return getToken(Cypher5Parser.LPAREN, 0); }
		public GraphReferenceContext graphReference() {
			return getRuleContext(GraphReferenceContext.class,0);
		}
		public TerminalNode RPAREN() { return getToken(Cypher5Parser.RPAREN, 0); }
		public FunctionInvocationContext functionInvocation() {
			return getRuleContext(FunctionInvocationContext.class,0);
		}
		public SymbolicAliasNameContext symbolicAliasName() {
			return getRuleContext(SymbolicAliasNameContext.class,0);
		}
		public GraphReferenceContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_graphReference; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterGraphReference(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitGraphReference(this);
		}
	}

	public final GraphReferenceContext graphReference() throws RecognitionException {
		GraphReferenceContext _localctx = new GraphReferenceContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_graphReference);
		try {
			setState(747);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,9,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(741);
				match(LPAREN);
				setState(742);
				graphReference();
				setState(743);
				match(RPAREN);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(745);
				functionInvocation();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(746);
				symbolicAliasName();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class FinishClauseContext extends AstRuleCtx {
		public TerminalNode FINISH() { return getToken(Cypher5Parser.FINISH, 0); }
		public FinishClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_finishClause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterFinishClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitFinishClause(this);
		}
	}

	public final FinishClauseContext finishClause() throws RecognitionException {
		FinishClauseContext _localctx = new FinishClauseContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_finishClause);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(749);
			match(FINISH);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ReturnClauseContext extends AstRuleCtx {
		public TerminalNode RETURN() { return getToken(Cypher5Parser.RETURN, 0); }
		public ReturnBodyContext returnBody() {
			return getRuleContext(ReturnBodyContext.class,0);
		}
		public ReturnClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_returnClause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterReturnClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitReturnClause(this);
		}
	}

	public final ReturnClauseContext returnClause() throws RecognitionException {
		ReturnClauseContext _localctx = new ReturnClauseContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_returnClause);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(751);
			match(RETURN);
			setState(752);
			returnBody();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ReturnBodyContext extends AstRuleCtx {
		public ReturnItemsContext returnItems() {
			return getRuleContext(ReturnItemsContext.class,0);
		}
		public TerminalNode DISTINCT() { return getToken(Cypher5Parser.DISTINCT, 0); }
		public OrderByContext orderBy() {
			return getRuleContext(OrderByContext.class,0);
		}
		public SkipContext skip() {
			return getRuleContext(SkipContext.class,0);
		}
		public LimitContext limit() {
			return getRuleContext(LimitContext.class,0);
		}
		public ReturnBodyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_returnBody; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterReturnBody(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitReturnBody(this);
		}
	}

	public final ReturnBodyContext returnBody() throws RecognitionException {
		ReturnBodyContext _localctx = new ReturnBodyContext(_ctx, getState());
		enterRule(_localctx, 20, RULE_returnBody);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(755);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,10,_ctx) ) {
			case 1:
				{
				setState(754);
				match(DISTINCT);
				}
				break;
			}
			setState(757);
			returnItems();
			setState(759);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,11,_ctx) ) {
			case 1:
				{
				setState(758);
				orderBy();
				}
				break;
			}
			setState(762);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,12,_ctx) ) {
			case 1:
				{
				setState(761);
				skip();
				}
				break;
			}
			setState(765);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,13,_ctx) ) {
			case 1:
				{
				setState(764);
				limit();
				}
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ReturnItemContext extends AstRuleCtx {
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public TerminalNode AS() { return getToken(Cypher5Parser.AS, 0); }
		public VariableContext variable() {
			return getRuleContext(VariableContext.class,0);
		}
		public ReturnItemContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_returnItem; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterReturnItem(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitReturnItem(this);
		}
	}

	public final ReturnItemContext returnItem() throws RecognitionException {
		ReturnItemContext _localctx = new ReturnItemContext(_ctx, getState());
		enterRule(_localctx, 22, RULE_returnItem);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(767);
			expression();
			setState(770);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==AS) {
				{
				setState(768);
				match(AS);
				setState(769);
				variable();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ReturnItemsContext extends AstRuleCtx {
		public TerminalNode TIMES() { return getToken(Cypher5Parser.TIMES, 0); }
		public List<ReturnItemContext> returnItem() {
			return getRuleContexts(ReturnItemContext.class);
		}
		public ReturnItemContext returnItem(int i) {
			return getRuleContext(ReturnItemContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(Cypher5Parser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(Cypher5Parser.COMMA, i);
		}
		public ReturnItemsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_returnItems; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterReturnItems(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitReturnItems(this);
		}
	}

	public final ReturnItemsContext returnItems() throws RecognitionException {
		ReturnItemsContext _localctx = new ReturnItemsContext(_ctx, getState());
		enterRule(_localctx, 24, RULE_returnItems);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(774);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case TIMES:
				{
				setState(772);
				match(TIMES);
				}
				break;
			case DECIMAL_DOUBLE:
			case UNSIGNED_DECIMAL_INTEGER:
			case UNSIGNED_HEX_INTEGER:
			case UNSIGNED_OCTAL_INTEGER:
			case STRING_LITERAL1:
			case STRING_LITERAL2:
			case ESCAPED_SYMBOLIC_NAME:
			case ACCESS:
			case ACTIVE:
			case ADMIN:
			case ADMINISTRATOR:
			case ALIAS:
			case ALIASES:
			case ALL_SHORTEST_PATHS:
			case ALL:
			case ALTER:
			case AND:
			case ANY:
			case ARRAY:
			case AS:
			case ASC:
			case ASCENDING:
			case ASSERT:
			case ASSIGN:
			case AT:
			case AUTH:
			case BINDINGS:
			case BOOL:
			case BOOLEAN:
			case BOOSTED:
			case BOTH:
			case BREAK:
			case BRIEF:
			case BTREE:
			case BUILT:
			case BY:
			case CALL:
			case CASCADE:
			case CASE:
			case CHANGE:
			case CIDR:
			case COLLECT:
			case COMMAND:
			case COMMANDS:
			case COMMIT:
			case COMPOSITE:
			case CONCURRENT:
			case CONSTRAINT:
			case CONSTRAINTS:
			case CONTAINS:
			case COPY:
			case CONTINUE:
			case COUNT:
			case CREATE:
			case CSV:
			case CURRENT:
			case DATA:
			case DATABASE:
			case DATABASES:
			case DATE:
			case DATETIME:
			case DBMS:
			case DEALLOCATE:
			case DEFAULT:
			case DEFINED:
			case DELETE:
			case DENY:
			case DESC:
			case DESCENDING:
			case DESTROY:
			case DETACH:
			case DIFFERENT:
			case DOLLAR:
			case DISTINCT:
			case DRIVER:
			case DROP:
			case DRYRUN:
			case DUMP:
			case DURATION:
			case EACH:
			case EDGE:
			case ENABLE:
			case ELEMENT:
			case ELEMENTS:
			case ELSE:
			case ENCRYPTED:
			case END:
			case ENDS:
			case EXECUTABLE:
			case EXECUTE:
			case EXIST:
			case EXISTENCE:
			case EXISTS:
			case ERROR:
			case FAIL:
			case FALSE:
			case FIELDTERMINATOR:
			case FINISH:
			case FLOAT:
			case FOR:
			case FOREACH:
			case FROM:
			case FULLTEXT:
			case FUNCTION:
			case FUNCTIONS:
			case GRANT:
			case GRAPH:
			case GRAPHS:
			case GROUP:
			case GROUPS:
			case HEADERS:
			case HOME:
			case ID:
			case IF:
			case IMPERSONATE:
			case IMMUTABLE:
			case IN:
			case INDEX:
			case INDEXES:
			case INF:
			case INFINITY:
			case INSERT:
			case INT:
			case INTEGER:
			case IS:
			case JOIN:
			case KEY:
			case LABEL:
			case LABELS:
			case LBRACKET:
			case LCURLY:
			case LEADING:
			case LIMITROWS:
			case LIST:
			case LOAD:
			case LOCAL:
			case LOOKUP:
			case LPAREN:
			case MANAGEMENT:
			case MAP:
			case MATCH:
			case MERGE:
			case MINUS:
			case NAME:
			case NAMES:
			case NAN:
			case NFC:
			case NFD:
			case NFKC:
			case NFKD:
			case NEW:
			case NODE:
			case NODETACH:
			case NODES:
			case NONE:
			case NORMALIZE:
			case NORMALIZED:
			case NOT:
			case NOTHING:
			case NOWAIT:
			case NULL:
			case OF:
			case OFFSET:
			case ON:
			case ONLY:
			case OPTIONAL:
			case OPTIONS:
			case OPTION:
			case OR:
			case ORDER:
			case OUTPUT:
			case PASSWORD:
			case PASSWORDS:
			case PATH:
			case PATHS:
			case PERIODIC:
			case PLAINTEXT:
			case PLUS:
			case POINT:
			case POPULATED:
			case PRIMARY:
			case PRIMARIES:
			case PRIVILEGE:
			case PRIVILEGES:
			case PROCEDURE:
			case PROCEDURES:
			case PROPERTIES:
			case PROPERTY:
			case PROVIDER:
			case PROVIDERS:
			case RANGE:
			case READ:
			case REALLOCATE:
			case REDUCE:
			case RENAME:
			case REL:
			case RELATIONSHIP:
			case RELATIONSHIPS:
			case REMOVE:
			case REPEATABLE:
			case REPLACE:
			case REPORT:
			case REQUIRE:
			case REQUIRED:
			case RESTRICT:
			case RETURN:
			case REVOKE:
			case ROLE:
			case ROLES:
			case ROW:
			case ROWS:
			case SCAN:
			case SEC:
			case SECOND:
			case SECONDARY:
			case SECONDARIES:
			case SECONDS:
			case SEEK:
			case SERVER:
			case SERVERS:
			case SET:
			case SETTING:
			case SETTINGS:
			case SHORTEST_PATH:
			case SHORTEST:
			case SHOW:
			case SIGNED:
			case SINGLE:
			case SKIPROWS:
			case START:
			case STARTS:
			case STATUS:
			case STOP:
			case STRING:
			case SUPPORTED:
			case SUSPENDED:
			case TARGET:
			case TERMINATE:
			case TEXT:
			case THEN:
			case TIME:
			case TIMESTAMP:
			case TIMEZONE:
			case TO:
			case TOPOLOGY:
			case TRAILING:
			case TRANSACTION:
			case TRANSACTIONS:
			case TRAVERSE:
			case TRIM:
			case TRUE:
			case TYPE:
			case TYPED:
			case TYPES:
			case UNION:
			case UNIQUE:
			case UNIQUENESS:
			case UNWIND:
			case URL:
			case USE:
			case USER:
			case USERS:
			case USING:
			case VALUE:
			case VARCHAR:
			case VECTOR:
			case VERBOSE:
			case VERTEX:
			case WAIT:
			case WHEN:
			case WHERE:
			case WITH:
			case WITHOUT:
			case WRITE:
			case XOR:
			case YIELD:
			case ZONE:
			case ZONED:
			case IDENTIFIER:
				{
				setState(773);
				returnItem();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(780);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(776);
				match(COMMA);
				setState(777);
				returnItem();
				}
				}
				setState(782);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class OrderItemContext extends AstRuleCtx {
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public AscTokenContext ascToken() {
			return getRuleContext(AscTokenContext.class,0);
		}
		public DescTokenContext descToken() {
			return getRuleContext(DescTokenContext.class,0);
		}
		public OrderItemContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_orderItem; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterOrderItem(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitOrderItem(this);
		}
	}

	public final OrderItemContext orderItem() throws RecognitionException {
		OrderItemContext _localctx = new OrderItemContext(_ctx, getState());
		enterRule(_localctx, 26, RULE_orderItem);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(783);
			expression();
			setState(786);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case ASC:
			case ASCENDING:
				{
				setState(784);
				ascToken();
				}
				break;
			case DESC:
			case DESCENDING:
				{
				setState(785);
				descToken();
				}
				break;
			case EOF:
			case CALL:
			case COMMA:
			case CREATE:
			case DELETE:
			case DETACH:
			case FINISH:
			case FOREACH:
			case INSERT:
			case LIMITROWS:
			case LOAD:
			case MATCH:
			case MERGE:
			case NODETACH:
			case OFFSET:
			case OPTIONAL:
			case ORDER:
			case RCURLY:
			case REMOVE:
			case RETURN:
			case RPAREN:
			case SEMICOLON:
			case SET:
			case SKIPROWS:
			case UNION:
			case UNWIND:
			case USE:
			case WHERE:
			case WITH:
				break;
			default:
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class AscTokenContext extends AstRuleCtx {
		public TerminalNode ASC() { return getToken(Cypher5Parser.ASC, 0); }
		public TerminalNode ASCENDING() { return getToken(Cypher5Parser.ASCENDING, 0); }
		public AscTokenContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_ascToken; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterAscToken(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitAscToken(this);
		}
	}

	public final AscTokenContext ascToken() throws RecognitionException {
		AscTokenContext _localctx = new AscTokenContext(_ctx, getState());
		enterRule(_localctx, 28, RULE_ascToken);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(788);
			_la = _input.LA(1);
			if ( !(_la==ASC || _la==ASCENDING) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class DescTokenContext extends AstRuleCtx {
		public TerminalNode DESC() { return getToken(Cypher5Parser.DESC, 0); }
		public TerminalNode DESCENDING() { return getToken(Cypher5Parser.DESCENDING, 0); }
		public DescTokenContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_descToken; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterDescToken(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitDescToken(this);
		}
	}

	public final DescTokenContext descToken() throws RecognitionException {
		DescTokenContext _localctx = new DescTokenContext(_ctx, getState());
		enterRule(_localctx, 30, RULE_descToken);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(790);
			_la = _input.LA(1);
			if ( !(_la==DESC || _la==DESCENDING) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class OrderByContext extends AstRuleCtx {
		public TerminalNode ORDER() { return getToken(Cypher5Parser.ORDER, 0); }
		public TerminalNode BY() { return getToken(Cypher5Parser.BY, 0); }
		public List<OrderItemContext> orderItem() {
			return getRuleContexts(OrderItemContext.class);
		}
		public OrderItemContext orderItem(int i) {
			return getRuleContext(OrderItemContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(Cypher5Parser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(Cypher5Parser.COMMA, i);
		}
		public OrderByContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_orderBy; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterOrderBy(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitOrderBy(this);
		}
	}

	public final OrderByContext orderBy() throws RecognitionException {
		OrderByContext _localctx = new OrderByContext(_ctx, getState());
		enterRule(_localctx, 32, RULE_orderBy);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(792);
			match(ORDER);
			setState(793);
			match(BY);
			setState(794);
			orderItem();
			setState(799);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(795);
				match(COMMA);
				setState(796);
				orderItem();
				}
				}
				setState(801);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class SkipContext extends AstRuleCtx {
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public TerminalNode OFFSET() { return getToken(Cypher5Parser.OFFSET, 0); }
		public TerminalNode SKIPROWS() { return getToken(Cypher5Parser.SKIPROWS, 0); }
		public SkipContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_skip; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterSkip(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitSkip(this);
		}
	}

	public final SkipContext skip() throws RecognitionException {
		SkipContext _localctx = new SkipContext(_ctx, getState());
		enterRule(_localctx, 34, RULE_skip);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(802);
			_la = _input.LA(1);
			if ( !(_la==OFFSET || _la==SKIPROWS) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			setState(803);
			expression();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class LimitContext extends AstRuleCtx {
		public TerminalNode LIMITROWS() { return getToken(Cypher5Parser.LIMITROWS, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public LimitContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_limit; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterLimit(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitLimit(this);
		}
	}

	public final LimitContext limit() throws RecognitionException {
		LimitContext _localctx = new LimitContext(_ctx, getState());
		enterRule(_localctx, 36, RULE_limit);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(805);
			match(LIMITROWS);
			setState(806);
			expression();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class WhereClauseContext extends AstRuleCtx {
		public TerminalNode WHERE() { return getToken(Cypher5Parser.WHERE, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public WhereClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_whereClause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterWhereClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitWhereClause(this);
		}
	}

	public final WhereClauseContext whereClause() throws RecognitionException {
		WhereClauseContext _localctx = new WhereClauseContext(_ctx, getState());
		enterRule(_localctx, 38, RULE_whereClause);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(808);
			match(WHERE);
			setState(809);
			expression();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class WithClauseContext extends AstRuleCtx {
		public TerminalNode WITH() { return getToken(Cypher5Parser.WITH, 0); }
		public ReturnBodyContext returnBody() {
			return getRuleContext(ReturnBodyContext.class,0);
		}
		public WhereClauseContext whereClause() {
			return getRuleContext(WhereClauseContext.class,0);
		}
		public WithClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_withClause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterWithClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitWithClause(this);
		}
	}

	public final WithClauseContext withClause() throws RecognitionException {
		WithClauseContext _localctx = new WithClauseContext(_ctx, getState());
		enterRule(_localctx, 40, RULE_withClause);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(811);
			match(WITH);
			setState(812);
			returnBody();
			setState(814);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WHERE) {
				{
				setState(813);
				whereClause();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class CreateClauseContext extends AstRuleCtx {
		public TerminalNode CREATE() { return getToken(Cypher5Parser.CREATE, 0); }
		public PatternListContext patternList() {
			return getRuleContext(PatternListContext.class,0);
		}
		public CreateClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_createClause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterCreateClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitCreateClause(this);
		}
	}

	public final CreateClauseContext createClause() throws RecognitionException {
		CreateClauseContext _localctx = new CreateClauseContext(_ctx, getState());
		enterRule(_localctx, 42, RULE_createClause);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(816);
			match(CREATE);
			setState(817);
			patternList();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class InsertClauseContext extends AstRuleCtx {
		public TerminalNode INSERT() { return getToken(Cypher5Parser.INSERT, 0); }
		public InsertPatternListContext insertPatternList() {
			return getRuleContext(InsertPatternListContext.class,0);
		}
		public InsertClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_insertClause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterInsertClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitInsertClause(this);
		}
	}

	public final InsertClauseContext insertClause() throws RecognitionException {
		InsertClauseContext _localctx = new InsertClauseContext(_ctx, getState());
		enterRule(_localctx, 44, RULE_insertClause);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(819);
			match(INSERT);
			setState(820);
			insertPatternList();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class SetClauseContext extends AstRuleCtx {
		public TerminalNode SET() { return getToken(Cypher5Parser.SET, 0); }
		public List<SetItemContext> setItem() {
			return getRuleContexts(SetItemContext.class);
		}
		public SetItemContext setItem(int i) {
			return getRuleContext(SetItemContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(Cypher5Parser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(Cypher5Parser.COMMA, i);
		}
		public SetClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_setClause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterSetClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitSetClause(this);
		}
	}

	public final SetClauseContext setClause() throws RecognitionException {
		SetClauseContext _localctx = new SetClauseContext(_ctx, getState());
		enterRule(_localctx, 46, RULE_setClause);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(822);
			match(SET);
			setState(823);
			setItem();
			setState(828);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(824);
				match(COMMA);
				setState(825);
				setItem();
				}
				}
				setState(830);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class SetItemContext extends AstRuleCtx {
		public SetItemContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_setItem; }
	 
		public SetItemContext() { }
		public void copyFrom(SetItemContext ctx) {
			super.copyFrom(ctx);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class SetPropContext extends SetItemContext {
		public PropertyExpressionContext propertyExpression() {
			return getRuleContext(PropertyExpressionContext.class,0);
		}
		public TerminalNode EQ() { return getToken(Cypher5Parser.EQ, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public SetPropContext(SetItemContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterSetProp(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitSetProp(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class AddPropContext extends SetItemContext {
		public VariableContext variable() {
			return getRuleContext(VariableContext.class,0);
		}
		public TerminalNode PLUSEQUAL() { return getToken(Cypher5Parser.PLUSEQUAL, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public AddPropContext(SetItemContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterAddProp(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitAddProp(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class SetDynamicPropContext extends SetItemContext {
		public DynamicPropertyExpressionContext dynamicPropertyExpression() {
			return getRuleContext(DynamicPropertyExpressionContext.class,0);
		}
		public TerminalNode EQ() { return getToken(Cypher5Parser.EQ, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public SetDynamicPropContext(SetItemContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterSetDynamicProp(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitSetDynamicProp(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class SetPropsContext extends SetItemContext {
		public VariableContext variable() {
			return getRuleContext(VariableContext.class,0);
		}
		public TerminalNode EQ() { return getToken(Cypher5Parser.EQ, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public SetPropsContext(SetItemContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterSetProps(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitSetProps(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class SetLabelsContext extends SetItemContext {
		public VariableContext variable() {
			return getRuleContext(VariableContext.class,0);
		}
		public NodeLabelsContext nodeLabels() {
			return getRuleContext(NodeLabelsContext.class,0);
		}
		public SetLabelsContext(SetItemContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterSetLabels(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitSetLabels(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class SetLabelsIsContext extends SetItemContext {
		public VariableContext variable() {
			return getRuleContext(VariableContext.class,0);
		}
		public NodeLabelsIsContext nodeLabelsIs() {
			return getRuleContext(NodeLabelsIsContext.class,0);
		}
		public SetLabelsIsContext(SetItemContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterSetLabelsIs(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitSetLabelsIs(this);
		}
	}

	public final SetItemContext setItem() throws RecognitionException {
		SetItemContext _localctx = new SetItemContext(_ctx, getState());
		enterRule(_localctx, 48, RULE_setItem);
		try {
			setState(853);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,21,_ctx) ) {
			case 1:
				_localctx = new SetPropContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(831);
				propertyExpression();
				setState(832);
				match(EQ);
				setState(833);
				expression();
				}
				break;
			case 2:
				_localctx = new SetDynamicPropContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(835);
				dynamicPropertyExpression();
				setState(836);
				match(EQ);
				setState(837);
				expression();
				}
				break;
			case 3:
				_localctx = new SetPropsContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(839);
				variable();
				setState(840);
				match(EQ);
				setState(841);
				expression();
				}
				break;
			case 4:
				_localctx = new AddPropContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(843);
				variable();
				setState(844);
				match(PLUSEQUAL);
				setState(845);
				expression();
				}
				break;
			case 5:
				_localctx = new SetLabelsContext(_localctx);
				enterOuterAlt(_localctx, 5);
				{
				setState(847);
				variable();
				setState(848);
				nodeLabels();
				}
				break;
			case 6:
				_localctx = new SetLabelsIsContext(_localctx);
				enterOuterAlt(_localctx, 6);
				{
				setState(850);
				variable();
				setState(851);
				nodeLabelsIs();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class RemoveClauseContext extends AstRuleCtx {
		public TerminalNode REMOVE() { return getToken(Cypher5Parser.REMOVE, 0); }
		public List<RemoveItemContext> removeItem() {
			return getRuleContexts(RemoveItemContext.class);
		}
		public RemoveItemContext removeItem(int i) {
			return getRuleContext(RemoveItemContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(Cypher5Parser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(Cypher5Parser.COMMA, i);
		}
		public RemoveClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_removeClause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterRemoveClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitRemoveClause(this);
		}
	}

	public final RemoveClauseContext removeClause() throws RecognitionException {
		RemoveClauseContext _localctx = new RemoveClauseContext(_ctx, getState());
		enterRule(_localctx, 50, RULE_removeClause);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(855);
			match(REMOVE);
			setState(856);
			removeItem();
			setState(861);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(857);
				match(COMMA);
				setState(858);
				removeItem();
				}
				}
				setState(863);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class RemoveItemContext extends AstRuleCtx {
		public RemoveItemContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_removeItem; }
	 
		public RemoveItemContext() { }
		public void copyFrom(RemoveItemContext ctx) {
			super.copyFrom(ctx);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class RemoveLabelsIsContext extends RemoveItemContext {
		public VariableContext variable() {
			return getRuleContext(VariableContext.class,0);
		}
		public NodeLabelsIsContext nodeLabelsIs() {
			return getRuleContext(NodeLabelsIsContext.class,0);
		}
		public RemoveLabelsIsContext(RemoveItemContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterRemoveLabelsIs(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitRemoveLabelsIs(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class RemoveDynamicPropContext extends RemoveItemContext {
		public DynamicPropertyExpressionContext dynamicPropertyExpression() {
			return getRuleContext(DynamicPropertyExpressionContext.class,0);
		}
		public RemoveDynamicPropContext(RemoveItemContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterRemoveDynamicProp(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitRemoveDynamicProp(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class RemoveLabelsContext extends RemoveItemContext {
		public VariableContext variable() {
			return getRuleContext(VariableContext.class,0);
		}
		public NodeLabelsContext nodeLabels() {
			return getRuleContext(NodeLabelsContext.class,0);
		}
		public RemoveLabelsContext(RemoveItemContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterRemoveLabels(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitRemoveLabels(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class RemovePropContext extends RemoveItemContext {
		public PropertyExpressionContext propertyExpression() {
			return getRuleContext(PropertyExpressionContext.class,0);
		}
		public RemovePropContext(RemoveItemContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterRemoveProp(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitRemoveProp(this);
		}
	}

	public final RemoveItemContext removeItem() throws RecognitionException {
		RemoveItemContext _localctx = new RemoveItemContext(_ctx, getState());
		enterRule(_localctx, 52, RULE_removeItem);
		try {
			setState(872);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,23,_ctx) ) {
			case 1:
				_localctx = new RemovePropContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(864);
				propertyExpression();
				}
				break;
			case 2:
				_localctx = new RemoveDynamicPropContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(865);
				dynamicPropertyExpression();
				}
				break;
			case 3:
				_localctx = new RemoveLabelsContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(866);
				variable();
				setState(867);
				nodeLabels();
				}
				break;
			case 4:
				_localctx = new RemoveLabelsIsContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(869);
				variable();
				setState(870);
				nodeLabelsIs();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class DeleteClauseContext extends AstRuleCtx {
		public TerminalNode DELETE() { return getToken(Cypher5Parser.DELETE, 0); }
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(Cypher5Parser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(Cypher5Parser.COMMA, i);
		}
		public TerminalNode DETACH() { return getToken(Cypher5Parser.DETACH, 0); }
		public TerminalNode NODETACH() { return getToken(Cypher5Parser.NODETACH, 0); }
		public DeleteClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_deleteClause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterDeleteClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitDeleteClause(this);
		}
	}

	public final DeleteClauseContext deleteClause() throws RecognitionException {
		DeleteClauseContext _localctx = new DeleteClauseContext(_ctx, getState());
		enterRule(_localctx, 54, RULE_deleteClause);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(875);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==DETACH || _la==NODETACH) {
				{
				setState(874);
				_la = _input.LA(1);
				if ( !(_la==DETACH || _la==NODETACH) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
			}

			setState(877);
			match(DELETE);
			setState(878);
			expression();
			setState(883);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(879);
				match(COMMA);
				setState(880);
				expression();
				}
				}
				setState(885);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class MatchClauseContext extends AstRuleCtx {
		public TerminalNode MATCH() { return getToken(Cypher5Parser.MATCH, 0); }
		public PatternListContext patternList() {
			return getRuleContext(PatternListContext.class,0);
		}
		public TerminalNode OPTIONAL() { return getToken(Cypher5Parser.OPTIONAL, 0); }
		public MatchModeContext matchMode() {
			return getRuleContext(MatchModeContext.class,0);
		}
		public List<HintContext> hint() {
			return getRuleContexts(HintContext.class);
		}
		public HintContext hint(int i) {
			return getRuleContext(HintContext.class,i);
		}
		public WhereClauseContext whereClause() {
			return getRuleContext(WhereClauseContext.class,0);
		}
		public MatchClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_matchClause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterMatchClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitMatchClause(this);
		}
	}

	public final MatchClauseContext matchClause() throws RecognitionException {
		MatchClauseContext _localctx = new MatchClauseContext(_ctx, getState());
		enterRule(_localctx, 56, RULE_matchClause);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(887);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==OPTIONAL) {
				{
				setState(886);
				match(OPTIONAL);
				}
			}

			setState(889);
			match(MATCH);
			setState(891);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,27,_ctx) ) {
			case 1:
				{
				setState(890);
				matchMode();
				}
				break;
			}
			setState(893);
			patternList();
			setState(897);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==USING) {
				{
				{
				setState(894);
				hint();
				}
				}
				setState(899);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(901);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WHERE) {
				{
				setState(900);
				whereClause();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class MatchModeContext extends AstRuleCtx {
		public TerminalNode REPEATABLE() { return getToken(Cypher5Parser.REPEATABLE, 0); }
		public TerminalNode ELEMENT() { return getToken(Cypher5Parser.ELEMENT, 0); }
		public TerminalNode ELEMENTS() { return getToken(Cypher5Parser.ELEMENTS, 0); }
		public TerminalNode BINDINGS() { return getToken(Cypher5Parser.BINDINGS, 0); }
		public TerminalNode DIFFERENT() { return getToken(Cypher5Parser.DIFFERENT, 0); }
		public TerminalNode RELATIONSHIP() { return getToken(Cypher5Parser.RELATIONSHIP, 0); }
		public TerminalNode RELATIONSHIPS() { return getToken(Cypher5Parser.RELATIONSHIPS, 0); }
		public MatchModeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_matchMode; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterMatchMode(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitMatchMode(this);
		}
	}

	public final MatchModeContext matchMode() throws RecognitionException {
		MatchModeContext _localctx = new MatchModeContext(_ctx, getState());
		enterRule(_localctx, 58, RULE_matchMode);
		try {
			setState(919);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case REPEATABLE:
				enterOuterAlt(_localctx, 1);
				{
				setState(903);
				match(REPEATABLE);
				setState(909);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case ELEMENT:
					{
					setState(904);
					match(ELEMENT);
					setState(906);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,30,_ctx) ) {
					case 1:
						{
						setState(905);
						match(BINDINGS);
						}
						break;
					}
					}
					break;
				case ELEMENTS:
					{
					setState(908);
					match(ELEMENTS);
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				}
				break;
			case DIFFERENT:
				enterOuterAlt(_localctx, 2);
				{
				setState(911);
				match(DIFFERENT);
				setState(917);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case RELATIONSHIP:
					{
					setState(912);
					match(RELATIONSHIP);
					setState(914);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,32,_ctx) ) {
					case 1:
						{
						setState(913);
						match(BINDINGS);
						}
						break;
					}
					}
					break;
				case RELATIONSHIPS:
					{
					setState(916);
					match(RELATIONSHIPS);
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class HintContext extends AstRuleCtx {
		public TerminalNode USING() { return getToken(Cypher5Parser.USING, 0); }
		public TerminalNode JOIN() { return getToken(Cypher5Parser.JOIN, 0); }
		public TerminalNode ON() { return getToken(Cypher5Parser.ON, 0); }
		public NonEmptyNameListContext nonEmptyNameList() {
			return getRuleContext(NonEmptyNameListContext.class,0);
		}
		public TerminalNode SCAN() { return getToken(Cypher5Parser.SCAN, 0); }
		public VariableContext variable() {
			return getRuleContext(VariableContext.class,0);
		}
		public LabelOrRelTypeContext labelOrRelType() {
			return getRuleContext(LabelOrRelTypeContext.class,0);
		}
		public TerminalNode LPAREN() { return getToken(Cypher5Parser.LPAREN, 0); }
		public TerminalNode RPAREN() { return getToken(Cypher5Parser.RPAREN, 0); }
		public TerminalNode INDEX() { return getToken(Cypher5Parser.INDEX, 0); }
		public TerminalNode BTREE() { return getToken(Cypher5Parser.BTREE, 0); }
		public TerminalNode TEXT() { return getToken(Cypher5Parser.TEXT, 0); }
		public TerminalNode RANGE() { return getToken(Cypher5Parser.RANGE, 0); }
		public TerminalNode POINT() { return getToken(Cypher5Parser.POINT, 0); }
		public TerminalNode SEEK() { return getToken(Cypher5Parser.SEEK, 0); }
		public HintContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_hint; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterHint(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitHint(this);
		}
	}

	public final HintContext hint() throws RecognitionException {
		HintContext _localctx = new HintContext(_ctx, getState());
		enterRule(_localctx, 60, RULE_hint);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(921);
			match(USING);
			setState(949);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case BTREE:
			case INDEX:
			case POINT:
			case RANGE:
			case TEXT:
				{
				{
				setState(931);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case INDEX:
					{
					setState(922);
					match(INDEX);
					}
					break;
				case BTREE:
					{
					setState(923);
					match(BTREE);
					setState(924);
					match(INDEX);
					}
					break;
				case TEXT:
					{
					setState(925);
					match(TEXT);
					setState(926);
					match(INDEX);
					}
					break;
				case RANGE:
					{
					setState(927);
					match(RANGE);
					setState(928);
					match(INDEX);
					}
					break;
				case POINT:
					{
					setState(929);
					match(POINT);
					setState(930);
					match(INDEX);
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(934);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,36,_ctx) ) {
				case 1:
					{
					setState(933);
					match(SEEK);
					}
					break;
				}
				setState(936);
				variable();
				setState(937);
				labelOrRelType();
				setState(938);
				match(LPAREN);
				setState(939);
				nonEmptyNameList();
				setState(940);
				match(RPAREN);
				}
				}
				break;
			case JOIN:
				{
				setState(942);
				match(JOIN);
				setState(943);
				match(ON);
				setState(944);
				nonEmptyNameList();
				}
				break;
			case SCAN:
				{
				setState(945);
				match(SCAN);
				setState(946);
				variable();
				setState(947);
				labelOrRelType();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class MergeClauseContext extends AstRuleCtx {
		public TerminalNode MERGE() { return getToken(Cypher5Parser.MERGE, 0); }
		public PatternContext pattern() {
			return getRuleContext(PatternContext.class,0);
		}
		public List<MergeActionContext> mergeAction() {
			return getRuleContexts(MergeActionContext.class);
		}
		public MergeActionContext mergeAction(int i) {
			return getRuleContext(MergeActionContext.class,i);
		}
		public MergeClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_mergeClause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterMergeClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitMergeClause(this);
		}
	}

	public final MergeClauseContext mergeClause() throws RecognitionException {
		MergeClauseContext _localctx = new MergeClauseContext(_ctx, getState());
		enterRule(_localctx, 62, RULE_mergeClause);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(951);
			match(MERGE);
			setState(952);
			pattern();
			setState(956);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==ON) {
				{
				{
				setState(953);
				mergeAction();
				}
				}
				setState(958);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class MergeActionContext extends AstRuleCtx {
		public TerminalNode ON() { return getToken(Cypher5Parser.ON, 0); }
		public SetClauseContext setClause() {
			return getRuleContext(SetClauseContext.class,0);
		}
		public TerminalNode MATCH() { return getToken(Cypher5Parser.MATCH, 0); }
		public TerminalNode CREATE() { return getToken(Cypher5Parser.CREATE, 0); }
		public MergeActionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_mergeAction; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterMergeAction(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitMergeAction(this);
		}
	}

	public final MergeActionContext mergeAction() throws RecognitionException {
		MergeActionContext _localctx = new MergeActionContext(_ctx, getState());
		enterRule(_localctx, 64, RULE_mergeAction);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(959);
			match(ON);
			setState(960);
			_la = _input.LA(1);
			if ( !(_la==CREATE || _la==MATCH) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			setState(961);
			setClause();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class UnwindClauseContext extends AstRuleCtx {
		public TerminalNode UNWIND() { return getToken(Cypher5Parser.UNWIND, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public TerminalNode AS() { return getToken(Cypher5Parser.AS, 0); }
		public VariableContext variable() {
			return getRuleContext(VariableContext.class,0);
		}
		public UnwindClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_unwindClause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterUnwindClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitUnwindClause(this);
		}
	}

	public final UnwindClauseContext unwindClause() throws RecognitionException {
		UnwindClauseContext _localctx = new UnwindClauseContext(_ctx, getState());
		enterRule(_localctx, 66, RULE_unwindClause);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(963);
			match(UNWIND);
			setState(964);
			expression();
			setState(965);
			match(AS);
			setState(966);
			variable();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class CallClauseContext extends AstRuleCtx {
		public TerminalNode CALL() { return getToken(Cypher5Parser.CALL, 0); }
		public ProcedureNameContext procedureName() {
			return getRuleContext(ProcedureNameContext.class,0);
		}
		public TerminalNode OPTIONAL() { return getToken(Cypher5Parser.OPTIONAL, 0); }
		public TerminalNode LPAREN() { return getToken(Cypher5Parser.LPAREN, 0); }
		public TerminalNode RPAREN() { return getToken(Cypher5Parser.RPAREN, 0); }
		public TerminalNode YIELD() { return getToken(Cypher5Parser.YIELD, 0); }
		public TerminalNode TIMES() { return getToken(Cypher5Parser.TIMES, 0); }
		public List<ProcedureResultItemContext> procedureResultItem() {
			return getRuleContexts(ProcedureResultItemContext.class);
		}
		public ProcedureResultItemContext procedureResultItem(int i) {
			return getRuleContext(ProcedureResultItemContext.class,i);
		}
		public List<ProcedureArgumentContext> procedureArgument() {
			return getRuleContexts(ProcedureArgumentContext.class);
		}
		public ProcedureArgumentContext procedureArgument(int i) {
			return getRuleContext(ProcedureArgumentContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(Cypher5Parser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(Cypher5Parser.COMMA, i);
		}
		public WhereClauseContext whereClause() {
			return getRuleContext(WhereClauseContext.class,0);
		}
		public CallClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_callClause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterCallClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitCallClause(this);
		}
	}

	public final CallClauseContext callClause() throws RecognitionException {
		CallClauseContext _localctx = new CallClauseContext(_ctx, getState());
		enterRule(_localctx, 68, RULE_callClause);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(969);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==OPTIONAL) {
				{
				setState(968);
				match(OPTIONAL);
				}
			}

			setState(971);
			match(CALL);
			setState(972);
			procedureName();
			setState(985);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LPAREN) {
				{
				setState(973);
				match(LPAREN);
				setState(982);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & -985163492229136L) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & -1170935971839737857L) != 0) || ((((_la - 128)) & ~0x3f) == 0 && ((1L << (_la - 128)) & -120528764929L) != 0) || ((((_la - 192)) & ~0x3f) == 0 && ((1L << (_la - 192)) & -36169535635589377L) != 0) || ((((_la - 256)) & ~0x3f) == 0 && ((1L << (_la - 256)) & 18014398509449215L) != 0)) {
					{
					setState(974);
					procedureArgument();
					setState(979);
					_errHandler.sync(this);
					_la = _input.LA(1);
					while (_la==COMMA) {
						{
						{
						setState(975);
						match(COMMA);
						setState(976);
						procedureArgument();
						}
						}
						setState(981);
						_errHandler.sync(this);
						_la = _input.LA(1);
					}
					}
				}

				setState(984);
				match(RPAREN);
				}
			}

			setState(1002);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==YIELD) {
				{
				setState(987);
				match(YIELD);
				setState(1000);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case TIMES:
					{
					setState(988);
					match(TIMES);
					}
					break;
				case ESCAPED_SYMBOLIC_NAME:
				case ACCESS:
				case ACTIVE:
				case ADMIN:
				case ADMINISTRATOR:
				case ALIAS:
				case ALIASES:
				case ALL_SHORTEST_PATHS:
				case ALL:
				case ALTER:
				case AND:
				case ANY:
				case ARRAY:
				case AS:
				case ASC:
				case ASCENDING:
				case ASSERT:
				case ASSIGN:
				case AT:
				case AUTH:
				case BINDINGS:
				case BOOL:
				case BOOLEAN:
				case BOOSTED:
				case BOTH:
				case BREAK:
				case BRIEF:
				case BTREE:
				case BUILT:
				case BY:
				case CALL:
				case CASCADE:
				case CASE:
				case CHANGE:
				case CIDR:
				case COLLECT:
				case COMMAND:
				case COMMANDS:
				case COMMIT:
				case COMPOSITE:
				case CONCURRENT:
				case CONSTRAINT:
				case CONSTRAINTS:
				case CONTAINS:
				case COPY:
				case CONTINUE:
				case COUNT:
				case CREATE:
				case CSV:
				case CURRENT:
				case DATA:
				case DATABASE:
				case DATABASES:
				case DATE:
				case DATETIME:
				case DBMS:
				case DEALLOCATE:
				case DEFAULT:
				case DEFINED:
				case DELETE:
				case DENY:
				case DESC:
				case DESCENDING:
				case DESTROY:
				case DETACH:
				case DIFFERENT:
				case DISTINCT:
				case DRIVER:
				case DROP:
				case DRYRUN:
				case DUMP:
				case DURATION:
				case EACH:
				case EDGE:
				case ENABLE:
				case ELEMENT:
				case ELEMENTS:
				case ELSE:
				case ENCRYPTED:
				case END:
				case ENDS:
				case EXECUTABLE:
				case EXECUTE:
				case EXIST:
				case EXISTENCE:
				case EXISTS:
				case ERROR:
				case FAIL:
				case FALSE:
				case FIELDTERMINATOR:
				case FINISH:
				case FLOAT:
				case FOR:
				case FOREACH:
				case FROM:
				case FULLTEXT:
				case FUNCTION:
				case FUNCTIONS:
				case GRANT:
				case GRAPH:
				case GRAPHS:
				case GROUP:
				case GROUPS:
				case HEADERS:
				case HOME:
				case ID:
				case IF:
				case IMPERSONATE:
				case IMMUTABLE:
				case IN:
				case INDEX:
				case INDEXES:
				case INF:
				case INFINITY:
				case INSERT:
				case INT:
				case INTEGER:
				case IS:
				case JOIN:
				case KEY:
				case LABEL:
				case LABELS:
				case LEADING:
				case LIMITROWS:
				case LIST:
				case LOAD:
				case LOCAL:
				case LOOKUP:
				case MANAGEMENT:
				case MAP:
				case MATCH:
				case MERGE:
				case NAME:
				case NAMES:
				case NAN:
				case NFC:
				case NFD:
				case NFKC:
				case NFKD:
				case NEW:
				case NODE:
				case NODETACH:
				case NODES:
				case NONE:
				case NORMALIZE:
				case NORMALIZED:
				case NOT:
				case NOTHING:
				case NOWAIT:
				case NULL:
				case OF:
				case OFFSET:
				case ON:
				case ONLY:
				case OPTIONAL:
				case OPTIONS:
				case OPTION:
				case OR:
				case ORDER:
				case OUTPUT:
				case PASSWORD:
				case PASSWORDS:
				case PATH:
				case PATHS:
				case PERIODIC:
				case PLAINTEXT:
				case POINT:
				case POPULATED:
				case PRIMARY:
				case PRIMARIES:
				case PRIVILEGE:
				case PRIVILEGES:
				case PROCEDURE:
				case PROCEDURES:
				case PROPERTIES:
				case PROPERTY:
				case PROVIDER:
				case PROVIDERS:
				case RANGE:
				case READ:
				case REALLOCATE:
				case REDUCE:
				case RENAME:
				case REL:
				case RELATIONSHIP:
				case RELATIONSHIPS:
				case REMOVE:
				case REPEATABLE:
				case REPLACE:
				case REPORT:
				case REQUIRE:
				case REQUIRED:
				case RESTRICT:
				case RETURN:
				case REVOKE:
				case ROLE:
				case ROLES:
				case ROW:
				case ROWS:
				case SCAN:
				case SEC:
				case SECOND:
				case SECONDARY:
				case SECONDARIES:
				case SECONDS:
				case SEEK:
				case SERVER:
				case SERVERS:
				case SET:
				case SETTING:
				case SETTINGS:
				case SHORTEST_PATH:
				case SHORTEST:
				case SHOW:
				case SIGNED:
				case SINGLE:
				case SKIPROWS:
				case START:
				case STARTS:
				case STATUS:
				case STOP:
				case STRING:
				case SUPPORTED:
				case SUSPENDED:
				case TARGET:
				case TERMINATE:
				case TEXT:
				case THEN:
				case TIME:
				case TIMESTAMP:
				case TIMEZONE:
				case TO:
				case TOPOLOGY:
				case TRAILING:
				case TRANSACTION:
				case TRANSACTIONS:
				case TRAVERSE:
				case TRIM:
				case TRUE:
				case TYPE:
				case TYPED:
				case TYPES:
				case UNION:
				case UNIQUE:
				case UNIQUENESS:
				case UNWIND:
				case URL:
				case USE:
				case USER:
				case USERS:
				case USING:
				case VALUE:
				case VARCHAR:
				case VECTOR:
				case VERBOSE:
				case VERTEX:
				case WAIT:
				case WHEN:
				case WHERE:
				case WITH:
				case WITHOUT:
				case WRITE:
				case XOR:
				case YIELD:
				case ZONE:
				case ZONED:
				case IDENTIFIER:
					{
					setState(989);
					procedureResultItem();
					setState(994);
					_errHandler.sync(this);
					_la = _input.LA(1);
					while (_la==COMMA) {
						{
						{
						setState(990);
						match(COMMA);
						setState(991);
						procedureResultItem();
						}
						}
						setState(996);
						_errHandler.sync(this);
						_la = _input.LA(1);
					}
					setState(998);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==WHERE) {
						{
						setState(997);
						whereClause();
						}
					}

					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ProcedureNameContext extends AstRuleCtx {
		public NamespaceContext namespace() {
			return getRuleContext(NamespaceContext.class,0);
		}
		public SymbolicNameStringContext symbolicNameString() {
			return getRuleContext(SymbolicNameStringContext.class,0);
		}
		public ProcedureNameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_procedureName; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterProcedureName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitProcedureName(this);
		}
	}

	public final ProcedureNameContext procedureName() throws RecognitionException {
		ProcedureNameContext _localctx = new ProcedureNameContext(_ctx, getState());
		enterRule(_localctx, 70, RULE_procedureName);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1004);
			namespace();
			setState(1005);
			symbolicNameString();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ProcedureArgumentContext extends AstRuleCtx {
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public ProcedureArgumentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_procedureArgument; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterProcedureArgument(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitProcedureArgument(this);
		}
	}

	public final ProcedureArgumentContext procedureArgument() throws RecognitionException {
		ProcedureArgumentContext _localctx = new ProcedureArgumentContext(_ctx, getState());
		enterRule(_localctx, 72, RULE_procedureArgument);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1007);
			expression();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ProcedureResultItemContext extends AstRuleCtx {
		public SymbolicNameStringContext symbolicNameString() {
			return getRuleContext(SymbolicNameStringContext.class,0);
		}
		public TerminalNode AS() { return getToken(Cypher5Parser.AS, 0); }
		public VariableContext variable() {
			return getRuleContext(VariableContext.class,0);
		}
		public ProcedureResultItemContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_procedureResultItem; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterProcedureResultItem(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitProcedureResultItem(this);
		}
	}

	public final ProcedureResultItemContext procedureResultItem() throws RecognitionException {
		ProcedureResultItemContext _localctx = new ProcedureResultItemContext(_ctx, getState());
		enterRule(_localctx, 74, RULE_procedureResultItem);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1009);
			symbolicNameString();
			setState(1012);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==AS) {
				{
				setState(1010);
				match(AS);
				setState(1011);
				variable();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class LoadCSVClauseContext extends AstRuleCtx {
		public TerminalNode LOAD() { return getToken(Cypher5Parser.LOAD, 0); }
		public TerminalNode CSV() { return getToken(Cypher5Parser.CSV, 0); }
		public TerminalNode FROM() { return getToken(Cypher5Parser.FROM, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public TerminalNode AS() { return getToken(Cypher5Parser.AS, 0); }
		public VariableContext variable() {
			return getRuleContext(VariableContext.class,0);
		}
		public TerminalNode WITH() { return getToken(Cypher5Parser.WITH, 0); }
		public TerminalNode HEADERS() { return getToken(Cypher5Parser.HEADERS, 0); }
		public TerminalNode FIELDTERMINATOR() { return getToken(Cypher5Parser.FIELDTERMINATOR, 0); }
		public StringLiteralContext stringLiteral() {
			return getRuleContext(StringLiteralContext.class,0);
		}
		public LoadCSVClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_loadCSVClause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterLoadCSVClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitLoadCSVClause(this);
		}
	}

	public final LoadCSVClauseContext loadCSVClause() throws RecognitionException {
		LoadCSVClauseContext _localctx = new LoadCSVClauseContext(_ctx, getState());
		enterRule(_localctx, 76, RULE_loadCSVClause);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1014);
			match(LOAD);
			setState(1015);
			match(CSV);
			setState(1018);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WITH) {
				{
				setState(1016);
				match(WITH);
				setState(1017);
				match(HEADERS);
				}
			}

			setState(1020);
			match(FROM);
			setState(1021);
			expression();
			setState(1022);
			match(AS);
			setState(1023);
			variable();
			setState(1026);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==FIELDTERMINATOR) {
				{
				setState(1024);
				match(FIELDTERMINATOR);
				setState(1025);
				stringLiteral();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ForeachClauseContext extends AstRuleCtx {
		public TerminalNode FOREACH() { return getToken(Cypher5Parser.FOREACH, 0); }
		public TerminalNode LPAREN() { return getToken(Cypher5Parser.LPAREN, 0); }
		public VariableContext variable() {
			return getRuleContext(VariableContext.class,0);
		}
		public TerminalNode IN() { return getToken(Cypher5Parser.IN, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public TerminalNode BAR() { return getToken(Cypher5Parser.BAR, 0); }
		public TerminalNode RPAREN() { return getToken(Cypher5Parser.RPAREN, 0); }
		public List<ClauseContext> clause() {
			return getRuleContexts(ClauseContext.class);
		}
		public ClauseContext clause(int i) {
			return getRuleContext(ClauseContext.class,i);
		}
		public ForeachClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_foreachClause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterForeachClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitForeachClause(this);
		}
	}

	public final ForeachClauseContext foreachClause() throws RecognitionException {
		ForeachClauseContext _localctx = new ForeachClauseContext(_ctx, getState());
		enterRule(_localctx, 78, RULE_foreachClause);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1028);
			match(FOREACH);
			setState(1029);
			match(LPAREN);
			setState(1030);
			variable();
			setState(1031);
			match(IN);
			setState(1032);
			expression();
			setState(1033);
			match(BAR);
			setState(1035); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(1034);
				clause();
				}
				}
				setState(1037); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( ((((_la - 41)) & ~0x3f) == 0 && ((1L << (_la - 41)) & 141734969345L) != 0) || ((((_la - 110)) & ~0x3f) == 0 && ((1L << (_la - 110)) & 1694347485511689L) != 0) || ((((_la - 174)) & ~0x3f) == 0 && ((1L << (_la - 174)) & 580964351930934273L) != 0) || ((((_la - 250)) & ~0x3f) == 0 && ((1L << (_la - 250)) & 4504974016905473L) != 0) );
			setState(1039);
			match(RPAREN);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class SubqueryClauseContext extends AstRuleCtx {
		public TerminalNode CALL() { return getToken(Cypher5Parser.CALL, 0); }
		public TerminalNode LCURLY() { return getToken(Cypher5Parser.LCURLY, 0); }
		public RegularQueryContext regularQuery() {
			return getRuleContext(RegularQueryContext.class,0);
		}
		public TerminalNode RCURLY() { return getToken(Cypher5Parser.RCURLY, 0); }
		public TerminalNode OPTIONAL() { return getToken(Cypher5Parser.OPTIONAL, 0); }
		public SubqueryScopeContext subqueryScope() {
			return getRuleContext(SubqueryScopeContext.class,0);
		}
		public SubqueryInTransactionsParametersContext subqueryInTransactionsParameters() {
			return getRuleContext(SubqueryInTransactionsParametersContext.class,0);
		}
		public SubqueryClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_subqueryClause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterSubqueryClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitSubqueryClause(this);
		}
	}

	public final SubqueryClauseContext subqueryClause() throws RecognitionException {
		SubqueryClauseContext _localctx = new SubqueryClauseContext(_ctx, getState());
		enterRule(_localctx, 80, RULE_subqueryClause);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1042);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==OPTIONAL) {
				{
				setState(1041);
				match(OPTIONAL);
				}
			}

			setState(1044);
			match(CALL);
			setState(1046);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LPAREN) {
				{
				setState(1045);
				subqueryScope();
				}
			}

			setState(1048);
			match(LCURLY);
			setState(1049);
			regularQuery();
			setState(1050);
			match(RCURLY);
			setState(1052);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==IN) {
				{
				setState(1051);
				subqueryInTransactionsParameters();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class SubqueryScopeContext extends AstRuleCtx {
		public TerminalNode LPAREN() { return getToken(Cypher5Parser.LPAREN, 0); }
		public TerminalNode RPAREN() { return getToken(Cypher5Parser.RPAREN, 0); }
		public TerminalNode TIMES() { return getToken(Cypher5Parser.TIMES, 0); }
		public List<VariableContext> variable() {
			return getRuleContexts(VariableContext.class);
		}
		public VariableContext variable(int i) {
			return getRuleContext(VariableContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(Cypher5Parser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(Cypher5Parser.COMMA, i);
		}
		public SubqueryScopeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_subqueryScope; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterSubqueryScope(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitSubqueryScope(this);
		}
	}

	public final SubqueryScopeContext subqueryScope() throws RecognitionException {
		SubqueryScopeContext _localctx = new SubqueryScopeContext(_ctx, getState());
		enterRule(_localctx, 82, RULE_subqueryScope);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1054);
			match(LPAREN);
			setState(1064);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case TIMES:
				{
				setState(1055);
				match(TIMES);
				}
				break;
			case ESCAPED_SYMBOLIC_NAME:
			case ACCESS:
			case ACTIVE:
			case ADMIN:
			case ADMINISTRATOR:
			case ALIAS:
			case ALIASES:
			case ALL_SHORTEST_PATHS:
			case ALL:
			case ALTER:
			case AND:
			case ANY:
			case ARRAY:
			case AS:
			case ASC:
			case ASCENDING:
			case ASSERT:
			case ASSIGN:
			case AT:
			case AUTH:
			case BINDINGS:
			case BOOL:
			case BOOLEAN:
			case BOOSTED:
			case BOTH:
			case BREAK:
			case BRIEF:
			case BTREE:
			case BUILT:
			case BY:
			case CALL:
			case CASCADE:
			case CASE:
			case CHANGE:
			case CIDR:
			case COLLECT:
			case COMMAND:
			case COMMANDS:
			case COMMIT:
			case COMPOSITE:
			case CONCURRENT:
			case CONSTRAINT:
			case CONSTRAINTS:
			case CONTAINS:
			case COPY:
			case CONTINUE:
			case COUNT:
			case CREATE:
			case CSV:
			case CURRENT:
			case DATA:
			case DATABASE:
			case DATABASES:
			case DATE:
			case DATETIME:
			case DBMS:
			case DEALLOCATE:
			case DEFAULT:
			case DEFINED:
			case DELETE:
			case DENY:
			case DESC:
			case DESCENDING:
			case DESTROY:
			case DETACH:
			case DIFFERENT:
			case DISTINCT:
			case DRIVER:
			case DROP:
			case DRYRUN:
			case DUMP:
			case DURATION:
			case EACH:
			case EDGE:
			case ENABLE:
			case ELEMENT:
			case ELEMENTS:
			case ELSE:
			case ENCRYPTED:
			case END:
			case ENDS:
			case EXECUTABLE:
			case EXECUTE:
			case EXIST:
			case EXISTENCE:
			case EXISTS:
			case ERROR:
			case FAIL:
			case FALSE:
			case FIELDTERMINATOR:
			case FINISH:
			case FLOAT:
			case FOR:
			case FOREACH:
			case FROM:
			case FULLTEXT:
			case FUNCTION:
			case FUNCTIONS:
			case GRANT:
			case GRAPH:
			case GRAPHS:
			case GROUP:
			case GROUPS:
			case HEADERS:
			case HOME:
			case ID:
			case IF:
			case IMPERSONATE:
			case IMMUTABLE:
			case IN:
			case INDEX:
			case INDEXES:
			case INF:
			case INFINITY:
			case INSERT:
			case INT:
			case INTEGER:
			case IS:
			case JOIN:
			case KEY:
			case LABEL:
			case LABELS:
			case LEADING:
			case LIMITROWS:
			case LIST:
			case LOAD:
			case LOCAL:
			case LOOKUP:
			case MANAGEMENT:
			case MAP:
			case MATCH:
			case MERGE:
			case NAME:
			case NAMES:
			case NAN:
			case NFC:
			case NFD:
			case NFKC:
			case NFKD:
			case NEW:
			case NODE:
			case NODETACH:
			case NODES:
			case NONE:
			case NORMALIZE:
			case NORMALIZED:
			case NOT:
			case NOTHING:
			case NOWAIT:
			case NULL:
			case OF:
			case OFFSET:
			case ON:
			case ONLY:
			case OPTIONAL:
			case OPTIONS:
			case OPTION:
			case OR:
			case ORDER:
			case OUTPUT:
			case PASSWORD:
			case PASSWORDS:
			case PATH:
			case PATHS:
			case PERIODIC:
			case PLAINTEXT:
			case POINT:
			case POPULATED:
			case PRIMARY:
			case PRIMARIES:
			case PRIVILEGE:
			case PRIVILEGES:
			case PROCEDURE:
			case PROCEDURES:
			case PROPERTIES:
			case PROPERTY:
			case PROVIDER:
			case PROVIDERS:
			case RANGE:
			case READ:
			case REALLOCATE:
			case REDUCE:
			case RENAME:
			case REL:
			case RELATIONSHIP:
			case RELATIONSHIPS:
			case REMOVE:
			case REPEATABLE:
			case REPLACE:
			case REPORT:
			case REQUIRE:
			case REQUIRED:
			case RESTRICT:
			case RETURN:
			case REVOKE:
			case ROLE:
			case ROLES:
			case ROW:
			case ROWS:
			case SCAN:
			case SEC:
			case SECOND:
			case SECONDARY:
			case SECONDARIES:
			case SECONDS:
			case SEEK:
			case SERVER:
			case SERVERS:
			case SET:
			case SETTING:
			case SETTINGS:
			case SHORTEST_PATH:
			case SHORTEST:
			case SHOW:
			case SIGNED:
			case SINGLE:
			case SKIPROWS:
			case START:
			case STARTS:
			case STATUS:
			case STOP:
			case STRING:
			case SUPPORTED:
			case SUSPENDED:
			case TARGET:
			case TERMINATE:
			case TEXT:
			case THEN:
			case TIME:
			case TIMESTAMP:
			case TIMEZONE:
			case TO:
			case TOPOLOGY:
			case TRAILING:
			case TRANSACTION:
			case TRANSACTIONS:
			case TRAVERSE:
			case TRIM:
			case TRUE:
			case TYPE:
			case TYPED:
			case TYPES:
			case UNION:
			case UNIQUE:
			case UNIQUENESS:
			case UNWIND:
			case URL:
			case USE:
			case USER:
			case USERS:
			case USING:
			case VALUE:
			case VARCHAR:
			case VECTOR:
			case VERBOSE:
			case VERTEX:
			case WAIT:
			case WHEN:
			case WHERE:
			case WITH:
			case WITHOUT:
			case WRITE:
			case XOR:
			case YIELD:
			case ZONE:
			case ZONED:
			case IDENTIFIER:
				{
				setState(1056);
				variable();
				setState(1061);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(1057);
					match(COMMA);
					setState(1058);
					variable();
					}
					}
					setState(1063);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
				break;
			case RPAREN:
				break;
			default:
				break;
			}
			setState(1066);
			match(RPAREN);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class SubqueryInTransactionsParametersContext extends AstRuleCtx {
		public TerminalNode IN() { return getToken(Cypher5Parser.IN, 0); }
		public TerminalNode TRANSACTIONS() { return getToken(Cypher5Parser.TRANSACTIONS, 0); }
		public TerminalNode CONCURRENT() { return getToken(Cypher5Parser.CONCURRENT, 0); }
		public List<SubqueryInTransactionsBatchParametersContext> subqueryInTransactionsBatchParameters() {
			return getRuleContexts(SubqueryInTransactionsBatchParametersContext.class);
		}
		public SubqueryInTransactionsBatchParametersContext subqueryInTransactionsBatchParameters(int i) {
			return getRuleContext(SubqueryInTransactionsBatchParametersContext.class,i);
		}
		public List<SubqueryInTransactionsErrorParametersContext> subqueryInTransactionsErrorParameters() {
			return getRuleContexts(SubqueryInTransactionsErrorParametersContext.class);
		}
		public SubqueryInTransactionsErrorParametersContext subqueryInTransactionsErrorParameters(int i) {
			return getRuleContext(SubqueryInTransactionsErrorParametersContext.class,i);
		}
		public List<SubqueryInTransactionsReportParametersContext> subqueryInTransactionsReportParameters() {
			return getRuleContexts(SubqueryInTransactionsReportParametersContext.class);
		}
		public SubqueryInTransactionsReportParametersContext subqueryInTransactionsReportParameters(int i) {
			return getRuleContext(SubqueryInTransactionsReportParametersContext.class,i);
		}
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public SubqueryInTransactionsParametersContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_subqueryInTransactionsParameters; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterSubqueryInTransactionsParameters(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitSubqueryInTransactionsParameters(this);
		}
	}

	public final SubqueryInTransactionsParametersContext subqueryInTransactionsParameters() throws RecognitionException {
		SubqueryInTransactionsParametersContext _localctx = new SubqueryInTransactionsParametersContext(_ctx, getState());
		enterRule(_localctx, 84, RULE_subqueryInTransactionsParameters);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1068);
			match(IN);
			setState(1073);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,57,_ctx) ) {
			case 1:
				{
				setState(1070);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,56,_ctx) ) {
				case 1:
					{
					setState(1069);
					expression();
					}
					break;
				}
				setState(1072);
				match(CONCURRENT);
				}
				break;
			}
			setState(1075);
			match(TRANSACTIONS);
			setState(1081);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (((((_la - 183)) & ~0x3f) == 0 && ((1L << (_la - 183)) & 70368744177669L) != 0)) {
				{
				setState(1079);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case OF:
					{
					setState(1076);
					subqueryInTransactionsBatchParameters();
					}
					break;
				case ON:
					{
					setState(1077);
					subqueryInTransactionsErrorParameters();
					}
					break;
				case REPORT:
					{
					setState(1078);
					subqueryInTransactionsReportParameters();
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				}
				setState(1083);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class SubqueryInTransactionsBatchParametersContext extends AstRuleCtx {
		public TerminalNode OF() { return getToken(Cypher5Parser.OF, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public TerminalNode ROW() { return getToken(Cypher5Parser.ROW, 0); }
		public TerminalNode ROWS() { return getToken(Cypher5Parser.ROWS, 0); }
		public SubqueryInTransactionsBatchParametersContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_subqueryInTransactionsBatchParameters; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterSubqueryInTransactionsBatchParameters(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitSubqueryInTransactionsBatchParameters(this);
		}
	}

	public final SubqueryInTransactionsBatchParametersContext subqueryInTransactionsBatchParameters() throws RecognitionException {
		SubqueryInTransactionsBatchParametersContext _localctx = new SubqueryInTransactionsBatchParametersContext(_ctx, getState());
		enterRule(_localctx, 86, RULE_subqueryInTransactionsBatchParameters);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1084);
			match(OF);
			setState(1085);
			expression();
			setState(1086);
			_la = _input.LA(1);
			if ( !(_la==ROW || _la==ROWS) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class SubqueryInTransactionsErrorParametersContext extends AstRuleCtx {
		public TerminalNode ON() { return getToken(Cypher5Parser.ON, 0); }
		public TerminalNode ERROR() { return getToken(Cypher5Parser.ERROR, 0); }
		public TerminalNode CONTINUE() { return getToken(Cypher5Parser.CONTINUE, 0); }
		public TerminalNode BREAK() { return getToken(Cypher5Parser.BREAK, 0); }
		public TerminalNode FAIL() { return getToken(Cypher5Parser.FAIL, 0); }
		public SubqueryInTransactionsErrorParametersContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_subqueryInTransactionsErrorParameters; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterSubqueryInTransactionsErrorParameters(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitSubqueryInTransactionsErrorParameters(this);
		}
	}

	public final SubqueryInTransactionsErrorParametersContext subqueryInTransactionsErrorParameters() throws RecognitionException {
		SubqueryInTransactionsErrorParametersContext _localctx = new SubqueryInTransactionsErrorParametersContext(_ctx, getState());
		enterRule(_localctx, 88, RULE_subqueryInTransactionsErrorParameters);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1088);
			match(ON);
			setState(1089);
			match(ERROR);
			setState(1090);
			_la = _input.LA(1);
			if ( !(_la==BREAK || _la==CONTINUE || _la==FAIL) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class SubqueryInTransactionsReportParametersContext extends AstRuleCtx {
		public TerminalNode REPORT() { return getToken(Cypher5Parser.REPORT, 0); }
		public TerminalNode STATUS() { return getToken(Cypher5Parser.STATUS, 0); }
		public TerminalNode AS() { return getToken(Cypher5Parser.AS, 0); }
		public VariableContext variable() {
			return getRuleContext(VariableContext.class,0);
		}
		public SubqueryInTransactionsReportParametersContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_subqueryInTransactionsReportParameters; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterSubqueryInTransactionsReportParameters(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitSubqueryInTransactionsReportParameters(this);
		}
	}

	public final SubqueryInTransactionsReportParametersContext subqueryInTransactionsReportParameters() throws RecognitionException {
		SubqueryInTransactionsReportParametersContext _localctx = new SubqueryInTransactionsReportParametersContext(_ctx, getState());
		enterRule(_localctx, 90, RULE_subqueryInTransactionsReportParameters);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1092);
			match(REPORT);
			setState(1093);
			match(STATUS);
			setState(1094);
			match(AS);
			setState(1095);
			variable();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class OrderBySkipLimitClauseContext extends AstRuleCtx {
		public OrderByContext orderBy() {
			return getRuleContext(OrderByContext.class,0);
		}
		public SkipContext skip() {
			return getRuleContext(SkipContext.class,0);
		}
		public LimitContext limit() {
			return getRuleContext(LimitContext.class,0);
		}
		public OrderBySkipLimitClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_orderBySkipLimitClause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterOrderBySkipLimitClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitOrderBySkipLimitClause(this);
		}
	}

	public final OrderBySkipLimitClauseContext orderBySkipLimitClause() throws RecognitionException {
		OrderBySkipLimitClauseContext _localctx = new OrderBySkipLimitClauseContext(_ctx, getState());
		enterRule(_localctx, 92, RULE_orderBySkipLimitClause);
		try {
			setState(1109);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case ORDER:
				enterOuterAlt(_localctx, 1);
				{
				setState(1097);
				orderBy();
				setState(1099);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,60,_ctx) ) {
				case 1:
					{
					setState(1098);
					skip();
					}
					break;
				}
				setState(1102);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,61,_ctx) ) {
				case 1:
					{
					setState(1101);
					limit();
					}
					break;
				}
				}
				break;
			case OFFSET:
			case SKIPROWS:
				enterOuterAlt(_localctx, 2);
				{
				setState(1104);
				skip();
				setState(1106);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,62,_ctx) ) {
				case 1:
					{
					setState(1105);
					limit();
					}
					break;
				}
				}
				break;
			case LIMITROWS:
				enterOuterAlt(_localctx, 3);
				{
				setState(1108);
				limit();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class PatternListContext extends AstRuleCtx {
		public List<PatternContext> pattern() {
			return getRuleContexts(PatternContext.class);
		}
		public PatternContext pattern(int i) {
			return getRuleContext(PatternContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(Cypher5Parser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(Cypher5Parser.COMMA, i);
		}
		public PatternListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_patternList; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterPatternList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitPatternList(this);
		}
	}

	public final PatternListContext patternList() throws RecognitionException {
		PatternListContext _localctx = new PatternListContext(_ctx, getState());
		enterRule(_localctx, 94, RULE_patternList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1111);
			pattern();
			setState(1116);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(1112);
				match(COMMA);
				setState(1113);
				pattern();
				}
				}
				setState(1118);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class InsertPatternListContext extends AstRuleCtx {
		public List<InsertPatternContext> insertPattern() {
			return getRuleContexts(InsertPatternContext.class);
		}
		public InsertPatternContext insertPattern(int i) {
			return getRuleContext(InsertPatternContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(Cypher5Parser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(Cypher5Parser.COMMA, i);
		}
		public InsertPatternListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_insertPatternList; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterInsertPatternList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitInsertPatternList(this);
		}
	}

	public final InsertPatternListContext insertPatternList() throws RecognitionException {
		InsertPatternListContext _localctx = new InsertPatternListContext(_ctx, getState());
		enterRule(_localctx, 96, RULE_insertPatternList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1119);
			insertPattern();
			setState(1124);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(1120);
				match(COMMA);
				setState(1121);
				insertPattern();
				}
				}
				setState(1126);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class PatternContext extends AstRuleCtx {
		public AnonymousPatternContext anonymousPattern() {
			return getRuleContext(AnonymousPatternContext.class,0);
		}
		public VariableContext variable() {
			return getRuleContext(VariableContext.class,0);
		}
		public TerminalNode EQ() { return getToken(Cypher5Parser.EQ, 0); }
		public SelectorContext selector() {
			return getRuleContext(SelectorContext.class,0);
		}
		public PatternContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_pattern; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterPattern(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitPattern(this);
		}
	}

	public final PatternContext pattern() throws RecognitionException {
		PatternContext _localctx = new PatternContext(_ctx, getState());
		enterRule(_localctx, 98, RULE_pattern);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1130);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,66,_ctx) ) {
			case 1:
				{
				setState(1127);
				variable();
				setState(1128);
				match(EQ);
				}
				break;
			}
			setState(1133);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==ALL || _la==ANY || _la==SHORTEST) {
				{
				setState(1132);
				selector();
				}
			}

			setState(1135);
			anonymousPattern();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class InsertPatternContext extends AstRuleCtx {
		public List<InsertNodePatternContext> insertNodePattern() {
			return getRuleContexts(InsertNodePatternContext.class);
		}
		public InsertNodePatternContext insertNodePattern(int i) {
			return getRuleContext(InsertNodePatternContext.class,i);
		}
		public SymbolicNameStringContext symbolicNameString() {
			return getRuleContext(SymbolicNameStringContext.class,0);
		}
		public TerminalNode EQ() { return getToken(Cypher5Parser.EQ, 0); }
		public List<InsertRelationshipPatternContext> insertRelationshipPattern() {
			return getRuleContexts(InsertRelationshipPatternContext.class);
		}
		public InsertRelationshipPatternContext insertRelationshipPattern(int i) {
			return getRuleContext(InsertRelationshipPatternContext.class,i);
		}
		public InsertPatternContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_insertPattern; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterInsertPattern(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitInsertPattern(this);
		}
	}

	public final InsertPatternContext insertPattern() throws RecognitionException {
		InsertPatternContext _localctx = new InsertPatternContext(_ctx, getState());
		enterRule(_localctx, 100, RULE_insertPattern);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1140);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & -985163492230144L) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & -1170935971839803393L) != 0) || ((((_la - 128)) & ~0x3f) == 0 && ((1L << (_la - 128)) & -129253703681L) != 0) || ((((_la - 192)) & ~0x3f) == 0 && ((1L << (_la - 192)) & -36169535635589505L) != 0) || ((((_la - 256)) & ~0x3f) == 0 && ((1L << (_la - 256)) & 18014398509449215L) != 0)) {
				{
				setState(1137);
				symbolicNameString();
				setState(1138);
				match(EQ);
				}
			}

			setState(1142);
			insertNodePattern();
			setState(1148);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==LT || _la==MINUS || _la==ARROW_LINE || _la==ARROW_LEFT_HEAD) {
				{
				{
				setState(1143);
				insertRelationshipPattern();
				setState(1144);
				insertNodePattern();
				}
				}
				setState(1150);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class QuantifierContext extends AstRuleCtx {
		public Token from;
		public Token to;
		public TerminalNode LCURLY() { return getToken(Cypher5Parser.LCURLY, 0); }
		public List<TerminalNode> UNSIGNED_DECIMAL_INTEGER() { return getTokens(Cypher5Parser.UNSIGNED_DECIMAL_INTEGER); }
		public TerminalNode UNSIGNED_DECIMAL_INTEGER(int i) {
			return getToken(Cypher5Parser.UNSIGNED_DECIMAL_INTEGER, i);
		}
		public TerminalNode RCURLY() { return getToken(Cypher5Parser.RCURLY, 0); }
		public TerminalNode COMMA() { return getToken(Cypher5Parser.COMMA, 0); }
		public TerminalNode PLUS() { return getToken(Cypher5Parser.PLUS, 0); }
		public TerminalNode TIMES() { return getToken(Cypher5Parser.TIMES, 0); }
		public QuantifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_quantifier; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterQuantifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitQuantifier(this);
		}
	}

	public final QuantifierContext quantifier() throws RecognitionException {
		QuantifierContext _localctx = new QuantifierContext(_ctx, getState());
		enterRule(_localctx, 102, RULE_quantifier);
		int _la;
		try {
			setState(1165);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,72,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1151);
				match(LCURLY);
				setState(1152);
				match(UNSIGNED_DECIMAL_INTEGER);
				setState(1153);
				match(RCURLY);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1154);
				match(LCURLY);
				setState(1156);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==UNSIGNED_DECIMAL_INTEGER) {
					{
					setState(1155);
					((QuantifierContext)_localctx).from = match(UNSIGNED_DECIMAL_INTEGER);
					}
				}

				setState(1158);
				match(COMMA);
				setState(1160);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==UNSIGNED_DECIMAL_INTEGER) {
					{
					setState(1159);
					((QuantifierContext)_localctx).to = match(UNSIGNED_DECIMAL_INTEGER);
					}
				}

				setState(1162);
				match(RCURLY);
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(1163);
				match(PLUS);
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(1164);
				match(TIMES);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class AnonymousPatternContext extends AstRuleCtx {
		public ShortestPathPatternContext shortestPathPattern() {
			return getRuleContext(ShortestPathPatternContext.class,0);
		}
		public PatternElementContext patternElement() {
			return getRuleContext(PatternElementContext.class,0);
		}
		public AnonymousPatternContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_anonymousPattern; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterAnonymousPattern(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitAnonymousPattern(this);
		}
	}

	public final AnonymousPatternContext anonymousPattern() throws RecognitionException {
		AnonymousPatternContext _localctx = new AnonymousPatternContext(_ctx, getState());
		enterRule(_localctx, 104, RULE_anonymousPattern);
		try {
			setState(1169);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case ALL_SHORTEST_PATHS:
			case SHORTEST_PATH:
				enterOuterAlt(_localctx, 1);
				{
				setState(1167);
				shortestPathPattern();
				}
				break;
			case LPAREN:
				enterOuterAlt(_localctx, 2);
				{
				setState(1168);
				patternElement();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ShortestPathPatternContext extends AstRuleCtx {
		public TerminalNode LPAREN() { return getToken(Cypher5Parser.LPAREN, 0); }
		public PatternElementContext patternElement() {
			return getRuleContext(PatternElementContext.class,0);
		}
		public TerminalNode RPAREN() { return getToken(Cypher5Parser.RPAREN, 0); }
		public TerminalNode SHORTEST_PATH() { return getToken(Cypher5Parser.SHORTEST_PATH, 0); }
		public TerminalNode ALL_SHORTEST_PATHS() { return getToken(Cypher5Parser.ALL_SHORTEST_PATHS, 0); }
		public ShortestPathPatternContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_shortestPathPattern; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterShortestPathPattern(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitShortestPathPattern(this);
		}
	}

	public final ShortestPathPatternContext shortestPathPattern() throws RecognitionException {
		ShortestPathPatternContext _localctx = new ShortestPathPatternContext(_ctx, getState());
		enterRule(_localctx, 106, RULE_shortestPathPattern);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1171);
			_la = _input.LA(1);
			if ( !(_la==ALL_SHORTEST_PATHS || _la==SHORTEST_PATH) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			setState(1172);
			match(LPAREN);
			setState(1173);
			patternElement();
			setState(1174);
			match(RPAREN);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class PatternElementContext extends AstRuleCtx {
		public List<NodePatternContext> nodePattern() {
			return getRuleContexts(NodePatternContext.class);
		}
		public NodePatternContext nodePattern(int i) {
			return getRuleContext(NodePatternContext.class,i);
		}
		public List<ParenthesizedPathContext> parenthesizedPath() {
			return getRuleContexts(ParenthesizedPathContext.class);
		}
		public ParenthesizedPathContext parenthesizedPath(int i) {
			return getRuleContext(ParenthesizedPathContext.class,i);
		}
		public List<RelationshipPatternContext> relationshipPattern() {
			return getRuleContexts(RelationshipPatternContext.class);
		}
		public RelationshipPatternContext relationshipPattern(int i) {
			return getRuleContext(RelationshipPatternContext.class,i);
		}
		public List<QuantifierContext> quantifier() {
			return getRuleContexts(QuantifierContext.class);
		}
		public QuantifierContext quantifier(int i) {
			return getRuleContext(QuantifierContext.class,i);
		}
		public PatternElementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_patternElement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterPatternElement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitPatternElement(this);
		}
	}

	public final PatternElementContext patternElement() throws RecognitionException {
		PatternElementContext _localctx = new PatternElementContext(_ctx, getState());
		enterRule(_localctx, 108, RULE_patternElement);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1189); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				setState(1189);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,76,_ctx) ) {
				case 1:
					{
					setState(1176);
					nodePattern();
					setState(1185);
					_errHandler.sync(this);
					_la = _input.LA(1);
					while (_la==LT || _la==MINUS || _la==ARROW_LINE || _la==ARROW_LEFT_HEAD) {
						{
						{
						setState(1177);
						relationshipPattern();
						setState(1179);
						_errHandler.sync(this);
						_la = _input.LA(1);
						if (_la==LCURLY || _la==PLUS || _la==TIMES) {
							{
							setState(1178);
							quantifier();
							}
						}

						setState(1181);
						nodePattern();
						}
						}
						setState(1187);
						_errHandler.sync(this);
						_la = _input.LA(1);
					}
					}
					break;
				case 2:
					{
					setState(1188);
					parenthesizedPath();
					}
					break;
				}
				}
				setState(1191); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==LPAREN );
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class SelectorContext extends AstRuleCtx {
		public SelectorContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_selector; }
	 
		public SelectorContext() { }
		public void copyFrom(SelectorContext ctx) {
			super.copyFrom(ctx);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class AllShortestPathContext extends SelectorContext {
		public TerminalNode ALL() { return getToken(Cypher5Parser.ALL, 0); }
		public TerminalNode SHORTEST() { return getToken(Cypher5Parser.SHORTEST, 0); }
		public PathTokenContext pathToken() {
			return getRuleContext(PathTokenContext.class,0);
		}
		public AllShortestPathContext(SelectorContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterAllShortestPath(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitAllShortestPath(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class AnyPathContext extends SelectorContext {
		public TerminalNode ANY() { return getToken(Cypher5Parser.ANY, 0); }
		public TerminalNode UNSIGNED_DECIMAL_INTEGER() { return getToken(Cypher5Parser.UNSIGNED_DECIMAL_INTEGER, 0); }
		public PathTokenContext pathToken() {
			return getRuleContext(PathTokenContext.class,0);
		}
		public AnyPathContext(SelectorContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterAnyPath(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitAnyPath(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class ShortestGroupContext extends SelectorContext {
		public TerminalNode SHORTEST() { return getToken(Cypher5Parser.SHORTEST, 0); }
		public GroupTokenContext groupToken() {
			return getRuleContext(GroupTokenContext.class,0);
		}
		public TerminalNode UNSIGNED_DECIMAL_INTEGER() { return getToken(Cypher5Parser.UNSIGNED_DECIMAL_INTEGER, 0); }
		public PathTokenContext pathToken() {
			return getRuleContext(PathTokenContext.class,0);
		}
		public ShortestGroupContext(SelectorContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterShortestGroup(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitShortestGroup(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class AnyShortestPathContext extends SelectorContext {
		public TerminalNode ANY() { return getToken(Cypher5Parser.ANY, 0); }
		public TerminalNode SHORTEST() { return getToken(Cypher5Parser.SHORTEST, 0); }
		public PathTokenContext pathToken() {
			return getRuleContext(PathTokenContext.class,0);
		}
		public TerminalNode UNSIGNED_DECIMAL_INTEGER() { return getToken(Cypher5Parser.UNSIGNED_DECIMAL_INTEGER, 0); }
		public AnyShortestPathContext(SelectorContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterAnyShortestPath(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitAnyShortestPath(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class AllPathContext extends SelectorContext {
		public TerminalNode ALL() { return getToken(Cypher5Parser.ALL, 0); }
		public PathTokenContext pathToken() {
			return getRuleContext(PathTokenContext.class,0);
		}
		public AllPathContext(SelectorContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterAllPath(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitAllPath(this);
		}
	}

	public final SelectorContext selector() throws RecognitionException {
		SelectorContext _localctx = new SelectorContext(_ctx, getState());
		enterRule(_localctx, 110, RULE_selector);
		int _la;
		try {
			setState(1227);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,86,_ctx) ) {
			case 1:
				_localctx = new AnyShortestPathContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(1193);
				match(ANY);
				setState(1194);
				match(SHORTEST);
				setState(1196);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==PATH || _la==PATHS) {
					{
					setState(1195);
					pathToken();
					}
				}

				}
				break;
			case 2:
				_localctx = new AllShortestPathContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(1198);
				match(ALL);
				setState(1199);
				match(SHORTEST);
				setState(1201);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==PATH || _la==PATHS) {
					{
					setState(1200);
					pathToken();
					}
				}

				}
				break;
			case 3:
				_localctx = new AnyPathContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(1203);
				match(ANY);
				setState(1205);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==UNSIGNED_DECIMAL_INTEGER) {
					{
					setState(1204);
					match(UNSIGNED_DECIMAL_INTEGER);
					}
				}

				setState(1208);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==PATH || _la==PATHS) {
					{
					setState(1207);
					pathToken();
					}
				}

				}
				break;
			case 4:
				_localctx = new AllPathContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(1210);
				match(ALL);
				setState(1212);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==PATH || _la==PATHS) {
					{
					setState(1211);
					pathToken();
					}
				}

				}
				break;
			case 5:
				_localctx = new ShortestGroupContext(_localctx);
				enterOuterAlt(_localctx, 5);
				{
				setState(1214);
				match(SHORTEST);
				setState(1216);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==UNSIGNED_DECIMAL_INTEGER) {
					{
					setState(1215);
					match(UNSIGNED_DECIMAL_INTEGER);
					}
				}

				setState(1219);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==PATH || _la==PATHS) {
					{
					setState(1218);
					pathToken();
					}
				}

				setState(1221);
				groupToken();
				}
				break;
			case 6:
				_localctx = new AnyShortestPathContext(_localctx);
				enterOuterAlt(_localctx, 6);
				{
				setState(1222);
				match(SHORTEST);
				setState(1223);
				match(UNSIGNED_DECIMAL_INTEGER);
				setState(1225);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==PATH || _la==PATHS) {
					{
					setState(1224);
					pathToken();
					}
				}

				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class GroupTokenContext extends AstRuleCtx {
		public TerminalNode GROUP() { return getToken(Cypher5Parser.GROUP, 0); }
		public TerminalNode GROUPS() { return getToken(Cypher5Parser.GROUPS, 0); }
		public GroupTokenContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_groupToken; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterGroupToken(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitGroupToken(this);
		}
	}

	public final GroupTokenContext groupToken() throws RecognitionException {
		GroupTokenContext _localctx = new GroupTokenContext(_ctx, getState());
		enterRule(_localctx, 112, RULE_groupToken);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1229);
			_la = _input.LA(1);
			if ( !(_la==GROUP || _la==GROUPS) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class PathTokenContext extends AstRuleCtx {
		public TerminalNode PATH() { return getToken(Cypher5Parser.PATH, 0); }
		public TerminalNode PATHS() { return getToken(Cypher5Parser.PATHS, 0); }
		public PathTokenContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_pathToken; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterPathToken(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitPathToken(this);
		}
	}

	public final PathTokenContext pathToken() throws RecognitionException {
		PathTokenContext _localctx = new PathTokenContext(_ctx, getState());
		enterRule(_localctx, 114, RULE_pathToken);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1231);
			_la = _input.LA(1);
			if ( !(_la==PATH || _la==PATHS) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class PathPatternNonEmptyContext extends AstRuleCtx {
		public List<NodePatternContext> nodePattern() {
			return getRuleContexts(NodePatternContext.class);
		}
		public NodePatternContext nodePattern(int i) {
			return getRuleContext(NodePatternContext.class,i);
		}
		public List<RelationshipPatternContext> relationshipPattern() {
			return getRuleContexts(RelationshipPatternContext.class);
		}
		public RelationshipPatternContext relationshipPattern(int i) {
			return getRuleContext(RelationshipPatternContext.class,i);
		}
		public PathPatternNonEmptyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_pathPatternNonEmpty; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterPathPatternNonEmpty(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitPathPatternNonEmpty(this);
		}
	}

	public final PathPatternNonEmptyContext pathPatternNonEmpty() throws RecognitionException {
		PathPatternNonEmptyContext _localctx = new PathPatternNonEmptyContext(_ctx, getState());
		enterRule(_localctx, 116, RULE_pathPatternNonEmpty);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(1233);
			nodePattern();
			setState(1237); 
			_errHandler.sync(this);
			_alt = 1;
			do {
				switch (_alt) {
				case 1:
					{
					{
					setState(1234);
					relationshipPattern();
					setState(1235);
					nodePattern();
					}
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(1239); 
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,87,_ctx);
			} while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER );
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class NodePatternContext extends AstRuleCtx {
		public TerminalNode LPAREN() { return getToken(Cypher5Parser.LPAREN, 0); }
		public TerminalNode RPAREN() { return getToken(Cypher5Parser.RPAREN, 0); }
		public VariableContext variable() {
			return getRuleContext(VariableContext.class,0);
		}
		public LabelExpressionContext labelExpression() {
			return getRuleContext(LabelExpressionContext.class,0);
		}
		public PropertiesContext properties() {
			return getRuleContext(PropertiesContext.class,0);
		}
		public TerminalNode WHERE() { return getToken(Cypher5Parser.WHERE, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public NodePatternContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_nodePattern; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterNodePattern(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitNodePattern(this);
		}
	}

	public final NodePatternContext nodePattern() throws RecognitionException {
		NodePatternContext _localctx = new NodePatternContext(_ctx, getState());
		enterRule(_localctx, 118, RULE_nodePattern);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1241);
			match(LPAREN);
			setState(1243);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,88,_ctx) ) {
			case 1:
				{
				setState(1242);
				variable();
				}
				break;
			}
			setState(1246);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==COLON || _la==IS) {
				{
				setState(1245);
				labelExpression();
				}
			}

			setState(1249);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==DOLLAR || _la==LCURLY) {
				{
				setState(1248);
				properties();
				}
			}

			setState(1253);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WHERE) {
				{
				setState(1251);
				match(WHERE);
				setState(1252);
				expression();
				}
			}

			setState(1255);
			match(RPAREN);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class InsertNodePatternContext extends AstRuleCtx {
		public TerminalNode LPAREN() { return getToken(Cypher5Parser.LPAREN, 0); }
		public TerminalNode RPAREN() { return getToken(Cypher5Parser.RPAREN, 0); }
		public VariableContext variable() {
			return getRuleContext(VariableContext.class,0);
		}
		public InsertNodeLabelExpressionContext insertNodeLabelExpression() {
			return getRuleContext(InsertNodeLabelExpressionContext.class,0);
		}
		public MapContext map() {
			return getRuleContext(MapContext.class,0);
		}
		public InsertNodePatternContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_insertNodePattern; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterInsertNodePattern(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitInsertNodePattern(this);
		}
	}

	public final InsertNodePatternContext insertNodePattern() throws RecognitionException {
		InsertNodePatternContext _localctx = new InsertNodePatternContext(_ctx, getState());
		enterRule(_localctx, 120, RULE_insertNodePattern);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1257);
			match(LPAREN);
			setState(1259);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,92,_ctx) ) {
			case 1:
				{
				setState(1258);
				variable();
				}
				break;
			}
			setState(1262);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==COLON || _la==IS) {
				{
				setState(1261);
				insertNodeLabelExpression();
				}
			}

			setState(1265);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LCURLY) {
				{
				setState(1264);
				map();
				}
			}

			setState(1267);
			match(RPAREN);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ParenthesizedPathContext extends AstRuleCtx {
		public TerminalNode LPAREN() { return getToken(Cypher5Parser.LPAREN, 0); }
		public PatternContext pattern() {
			return getRuleContext(PatternContext.class,0);
		}
		public TerminalNode RPAREN() { return getToken(Cypher5Parser.RPAREN, 0); }
		public TerminalNode WHERE() { return getToken(Cypher5Parser.WHERE, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public QuantifierContext quantifier() {
			return getRuleContext(QuantifierContext.class,0);
		}
		public ParenthesizedPathContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_parenthesizedPath; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterParenthesizedPath(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitParenthesizedPath(this);
		}
	}

	public final ParenthesizedPathContext parenthesizedPath() throws RecognitionException {
		ParenthesizedPathContext _localctx = new ParenthesizedPathContext(_ctx, getState());
		enterRule(_localctx, 122, RULE_parenthesizedPath);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1269);
			match(LPAREN);
			setState(1270);
			pattern();
			setState(1273);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WHERE) {
				{
				setState(1271);
				match(WHERE);
				setState(1272);
				expression();
				}
			}

			setState(1275);
			match(RPAREN);
			setState(1277);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LCURLY || _la==PLUS || _la==TIMES) {
				{
				setState(1276);
				quantifier();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class NodeLabelsContext extends AstRuleCtx {
		public List<LabelTypeContext> labelType() {
			return getRuleContexts(LabelTypeContext.class);
		}
		public LabelTypeContext labelType(int i) {
			return getRuleContext(LabelTypeContext.class,i);
		}
		public List<DynamicLabelTypeContext> dynamicLabelType() {
			return getRuleContexts(DynamicLabelTypeContext.class);
		}
		public DynamicLabelTypeContext dynamicLabelType(int i) {
			return getRuleContext(DynamicLabelTypeContext.class,i);
		}
		public NodeLabelsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_nodeLabels; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterNodeLabels(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitNodeLabels(this);
		}
	}

	public final NodeLabelsContext nodeLabels() throws RecognitionException {
		NodeLabelsContext _localctx = new NodeLabelsContext(_ctx, getState());
		enterRule(_localctx, 124, RULE_nodeLabels);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1281); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				setState(1281);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,97,_ctx) ) {
				case 1:
					{
					setState(1279);
					labelType();
					}
					break;
				case 2:
					{
					setState(1280);
					dynamicLabelType();
					}
					break;
				}
				}
				setState(1283); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==COLON );
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class NodeLabelsIsContext extends AstRuleCtx {
		public TerminalNode IS() { return getToken(Cypher5Parser.IS, 0); }
		public SymbolicNameStringContext symbolicNameString() {
			return getRuleContext(SymbolicNameStringContext.class,0);
		}
		public DynamicExpressionContext dynamicExpression() {
			return getRuleContext(DynamicExpressionContext.class,0);
		}
		public List<LabelTypeContext> labelType() {
			return getRuleContexts(LabelTypeContext.class);
		}
		public LabelTypeContext labelType(int i) {
			return getRuleContext(LabelTypeContext.class,i);
		}
		public List<DynamicLabelTypeContext> dynamicLabelType() {
			return getRuleContexts(DynamicLabelTypeContext.class);
		}
		public DynamicLabelTypeContext dynamicLabelType(int i) {
			return getRuleContext(DynamicLabelTypeContext.class,i);
		}
		public NodeLabelsIsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_nodeLabelsIs; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterNodeLabelsIs(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitNodeLabelsIs(this);
		}
	}

	public final NodeLabelsIsContext nodeLabelsIs() throws RecognitionException {
		NodeLabelsIsContext _localctx = new NodeLabelsIsContext(_ctx, getState());
		enterRule(_localctx, 126, RULE_nodeLabelsIs);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1285);
			match(IS);
			setState(1288);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case ESCAPED_SYMBOLIC_NAME:
			case ACCESS:
			case ACTIVE:
			case ADMIN:
			case ADMINISTRATOR:
			case ALIAS:
			case ALIASES:
			case ALL_SHORTEST_PATHS:
			case ALL:
			case ALTER:
			case AND:
			case ANY:
			case ARRAY:
			case AS:
			case ASC:
			case ASCENDING:
			case ASSERT:
			case ASSIGN:
			case AT:
			case AUTH:
			case BINDINGS:
			case BOOL:
			case BOOLEAN:
			case BOOSTED:
			case BOTH:
			case BREAK:
			case BRIEF:
			case BTREE:
			case BUILT:
			case BY:
			case CALL:
			case CASCADE:
			case CASE:
			case CHANGE:
			case CIDR:
			case COLLECT:
			case COMMAND:
			case COMMANDS:
			case COMMIT:
			case COMPOSITE:
			case CONCURRENT:
			case CONSTRAINT:
			case CONSTRAINTS:
			case CONTAINS:
			case COPY:
			case CONTINUE:
			case COUNT:
			case CREATE:
			case CSV:
			case CURRENT:
			case DATA:
			case DATABASE:
			case DATABASES:
			case DATE:
			case DATETIME:
			case DBMS:
			case DEALLOCATE:
			case DEFAULT:
			case DEFINED:
			case DELETE:
			case DENY:
			case DESC:
			case DESCENDING:
			case DESTROY:
			case DETACH:
			case DIFFERENT:
			case DISTINCT:
			case DRIVER:
			case DROP:
			case DRYRUN:
			case DUMP:
			case DURATION:
			case EACH:
			case EDGE:
			case ENABLE:
			case ELEMENT:
			case ELEMENTS:
			case ELSE:
			case ENCRYPTED:
			case END:
			case ENDS:
			case EXECUTABLE:
			case EXECUTE:
			case EXIST:
			case EXISTENCE:
			case EXISTS:
			case ERROR:
			case FAIL:
			case FALSE:
			case FIELDTERMINATOR:
			case FINISH:
			case FLOAT:
			case FOR:
			case FOREACH:
			case FROM:
			case FULLTEXT:
			case FUNCTION:
			case FUNCTIONS:
			case GRANT:
			case GRAPH:
			case GRAPHS:
			case GROUP:
			case GROUPS:
			case HEADERS:
			case HOME:
			case ID:
			case IF:
			case IMPERSONATE:
			case IMMUTABLE:
			case IN:
			case INDEX:
			case INDEXES:
			case INF:
			case INFINITY:
			case INSERT:
			case INT:
			case INTEGER:
			case IS:
			case JOIN:
			case KEY:
			case LABEL:
			case LABELS:
			case LEADING:
			case LIMITROWS:
			case LIST:
			case LOAD:
			case LOCAL:
			case LOOKUP:
			case MANAGEMENT:
			case MAP:
			case MATCH:
			case MERGE:
			case NAME:
			case NAMES:
			case NAN:
			case NFC:
			case NFD:
			case NFKC:
			case NFKD:
			case NEW:
			case NODE:
			case NODETACH:
			case NODES:
			case NONE:
			case NORMALIZE:
			case NORMALIZED:
			case NOT:
			case NOTHING:
			case NOWAIT:
			case NULL:
			case OF:
			case OFFSET:
			case ON:
			case ONLY:
			case OPTIONAL:
			case OPTIONS:
			case OPTION:
			case OR:
			case ORDER:
			case OUTPUT:
			case PASSWORD:
			case PASSWORDS:
			case PATH:
			case PATHS:
			case PERIODIC:
			case PLAINTEXT:
			case POINT:
			case POPULATED:
			case PRIMARY:
			case PRIMARIES:
			case PRIVILEGE:
			case PRIVILEGES:
			case PROCEDURE:
			case PROCEDURES:
			case PROPERTIES:
			case PROPERTY:
			case PROVIDER:
			case PROVIDERS:
			case RANGE:
			case READ:
			case REALLOCATE:
			case REDUCE:
			case RENAME:
			case REL:
			case RELATIONSHIP:
			case RELATIONSHIPS:
			case REMOVE:
			case REPEATABLE:
			case REPLACE:
			case REPORT:
			case REQUIRE:
			case REQUIRED:
			case RESTRICT:
			case RETURN:
			case REVOKE:
			case ROLE:
			case ROLES:
			case ROW:
			case ROWS:
			case SCAN:
			case SEC:
			case SECOND:
			case SECONDARY:
			case SECONDARIES:
			case SECONDS:
			case SEEK:
			case SERVER:
			case SERVERS:
			case SET:
			case SETTING:
			case SETTINGS:
			case SHORTEST_PATH:
			case SHORTEST:
			case SHOW:
			case SIGNED:
			case SINGLE:
			case SKIPROWS:
			case START:
			case STARTS:
			case STATUS:
			case STOP:
			case STRING:
			case SUPPORTED:
			case SUSPENDED:
			case TARGET:
			case TERMINATE:
			case TEXT:
			case THEN:
			case TIME:
			case TIMESTAMP:
			case TIMEZONE:
			case TO:
			case TOPOLOGY:
			case TRAILING:
			case TRANSACTION:
			case TRANSACTIONS:
			case TRAVERSE:
			case TRIM:
			case TRUE:
			case TYPE:
			case TYPED:
			case TYPES:
			case UNION:
			case UNIQUE:
			case UNIQUENESS:
			case UNWIND:
			case URL:
			case USE:
			case USER:
			case USERS:
			case USING:
			case VALUE:
			case VARCHAR:
			case VECTOR:
			case VERBOSE:
			case VERTEX:
			case WAIT:
			case WHEN:
			case WHERE:
			case WITH:
			case WITHOUT:
			case WRITE:
			case XOR:
			case YIELD:
			case ZONE:
			case ZONED:
			case IDENTIFIER:
				{
				setState(1286);
				symbolicNameString();
				}
				break;
			case DOLLAR:
				{
				setState(1287);
				dynamicExpression();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(1294);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COLON) {
				{
				setState(1292);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,100,_ctx) ) {
				case 1:
					{
					setState(1290);
					labelType();
					}
					break;
				case 2:
					{
					setState(1291);
					dynamicLabelType();
					}
					break;
				}
				}
				setState(1296);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class DynamicExpressionContext extends AstRuleCtx {
		public TerminalNode DOLLAR() { return getToken(Cypher5Parser.DOLLAR, 0); }
		public TerminalNode LPAREN() { return getToken(Cypher5Parser.LPAREN, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public TerminalNode RPAREN() { return getToken(Cypher5Parser.RPAREN, 0); }
		public DynamicExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_dynamicExpression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterDynamicExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitDynamicExpression(this);
		}
	}

	public final DynamicExpressionContext dynamicExpression() throws RecognitionException {
		DynamicExpressionContext _localctx = new DynamicExpressionContext(_ctx, getState());
		enterRule(_localctx, 128, RULE_dynamicExpression);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1297);
			match(DOLLAR);
			setState(1298);
			match(LPAREN);
			setState(1299);
			expression();
			setState(1300);
			match(RPAREN);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class DynamicAnyAllExpressionContext extends AstRuleCtx {
		public TerminalNode DOLLAR() { return getToken(Cypher5Parser.DOLLAR, 0); }
		public TerminalNode LPAREN() { return getToken(Cypher5Parser.LPAREN, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public TerminalNode RPAREN() { return getToken(Cypher5Parser.RPAREN, 0); }
		public TerminalNode ALL() { return getToken(Cypher5Parser.ALL, 0); }
		public TerminalNode ANY() { return getToken(Cypher5Parser.ANY, 0); }
		public DynamicAnyAllExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_dynamicAnyAllExpression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterDynamicAnyAllExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitDynamicAnyAllExpression(this);
		}
	}

	public final DynamicAnyAllExpressionContext dynamicAnyAllExpression() throws RecognitionException {
		DynamicAnyAllExpressionContext _localctx = new DynamicAnyAllExpressionContext(_ctx, getState());
		enterRule(_localctx, 130, RULE_dynamicAnyAllExpression);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1302);
			match(DOLLAR);
			setState(1304);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==ALL || _la==ANY) {
				{
				setState(1303);
				_la = _input.LA(1);
				if ( !(_la==ALL || _la==ANY) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
			}

			setState(1306);
			match(LPAREN);
			setState(1307);
			expression();
			setState(1308);
			match(RPAREN);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class DynamicLabelTypeContext extends AstRuleCtx {
		public TerminalNode COLON() { return getToken(Cypher5Parser.COLON, 0); }
		public DynamicExpressionContext dynamicExpression() {
			return getRuleContext(DynamicExpressionContext.class,0);
		}
		public DynamicLabelTypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_dynamicLabelType; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterDynamicLabelType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitDynamicLabelType(this);
		}
	}

	public final DynamicLabelTypeContext dynamicLabelType() throws RecognitionException {
		DynamicLabelTypeContext _localctx = new DynamicLabelTypeContext(_ctx, getState());
		enterRule(_localctx, 132, RULE_dynamicLabelType);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1310);
			match(COLON);
			setState(1311);
			dynamicExpression();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class LabelTypeContext extends AstRuleCtx {
		public TerminalNode COLON() { return getToken(Cypher5Parser.COLON, 0); }
		public SymbolicNameStringContext symbolicNameString() {
			return getRuleContext(SymbolicNameStringContext.class,0);
		}
		public LabelTypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_labelType; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterLabelType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitLabelType(this);
		}
	}

	public final LabelTypeContext labelType() throws RecognitionException {
		LabelTypeContext _localctx = new LabelTypeContext(_ctx, getState());
		enterRule(_localctx, 134, RULE_labelType);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1313);
			match(COLON);
			setState(1314);
			symbolicNameString();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class RelTypeContext extends AstRuleCtx {
		public TerminalNode COLON() { return getToken(Cypher5Parser.COLON, 0); }
		public SymbolicNameStringContext symbolicNameString() {
			return getRuleContext(SymbolicNameStringContext.class,0);
		}
		public RelTypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_relType; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterRelType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitRelType(this);
		}
	}

	public final RelTypeContext relType() throws RecognitionException {
		RelTypeContext _localctx = new RelTypeContext(_ctx, getState());
		enterRule(_localctx, 136, RULE_relType);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1316);
			match(COLON);
			setState(1317);
			symbolicNameString();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class LabelOrRelTypeContext extends AstRuleCtx {
		public TerminalNode COLON() { return getToken(Cypher5Parser.COLON, 0); }
		public SymbolicNameStringContext symbolicNameString() {
			return getRuleContext(SymbolicNameStringContext.class,0);
		}
		public LabelOrRelTypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_labelOrRelType; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterLabelOrRelType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitLabelOrRelType(this);
		}
	}

	public final LabelOrRelTypeContext labelOrRelType() throws RecognitionException {
		LabelOrRelTypeContext _localctx = new LabelOrRelTypeContext(_ctx, getState());
		enterRule(_localctx, 138, RULE_labelOrRelType);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1319);
			match(COLON);
			setState(1320);
			symbolicNameString();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class PropertiesContext extends AstRuleCtx {
		public MapContext map() {
			return getRuleContext(MapContext.class,0);
		}
		public ParameterContext parameter() {
			return getRuleContext(ParameterContext.class,0);
		}
		public PropertiesContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_properties; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterProperties(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitProperties(this);
		}
	}

	public final PropertiesContext properties() throws RecognitionException {
		PropertiesContext _localctx = new PropertiesContext(_ctx, getState());
		enterRule(_localctx, 140, RULE_properties);
		try {
			setState(1324);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case LCURLY:
				enterOuterAlt(_localctx, 1);
				{
				setState(1322);
				map();
				}
				break;
			case DOLLAR:
				enterOuterAlt(_localctx, 2);
				{
				setState(1323);
				parameter("ANY");
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class RelationshipPatternContext extends AstRuleCtx {
		public List<ArrowLineContext> arrowLine() {
			return getRuleContexts(ArrowLineContext.class);
		}
		public ArrowLineContext arrowLine(int i) {
			return getRuleContext(ArrowLineContext.class,i);
		}
		public LeftArrowContext leftArrow() {
			return getRuleContext(LeftArrowContext.class,0);
		}
		public TerminalNode LBRACKET() { return getToken(Cypher5Parser.LBRACKET, 0); }
		public TerminalNode RBRACKET() { return getToken(Cypher5Parser.RBRACKET, 0); }
		public RightArrowContext rightArrow() {
			return getRuleContext(RightArrowContext.class,0);
		}
		public VariableContext variable() {
			return getRuleContext(VariableContext.class,0);
		}
		public LabelExpressionContext labelExpression() {
			return getRuleContext(LabelExpressionContext.class,0);
		}
		public PathLengthContext pathLength() {
			return getRuleContext(PathLengthContext.class,0);
		}
		public PropertiesContext properties() {
			return getRuleContext(PropertiesContext.class,0);
		}
		public TerminalNode WHERE() { return getToken(Cypher5Parser.WHERE, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public RelationshipPatternContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_relationshipPattern; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterRelationshipPattern(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitRelationshipPattern(this);
		}
	}

	public final RelationshipPatternContext relationshipPattern() throws RecognitionException {
		RelationshipPatternContext _localctx = new RelationshipPatternContext(_ctx, getState());
		enterRule(_localctx, 142, RULE_relationshipPattern);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1327);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LT || _la==ARROW_LEFT_HEAD) {
				{
				setState(1326);
				leftArrow();
				}
			}

			setState(1329);
			arrowLine();
			setState(1348);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LBRACKET) {
				{
				setState(1330);
				match(LBRACKET);
				setState(1332);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,105,_ctx) ) {
				case 1:
					{
					setState(1331);
					variable();
					}
					break;
				}
				setState(1335);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==COLON || _la==IS) {
					{
					setState(1334);
					labelExpression();
					}
				}

				setState(1338);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==TIMES) {
					{
					setState(1337);
					pathLength();
					}
				}

				setState(1341);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==DOLLAR || _la==LCURLY) {
					{
					setState(1340);
					properties();
					}
				}

				setState(1345);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==WHERE) {
					{
					setState(1343);
					match(WHERE);
					setState(1344);
					expression();
					}
				}

				setState(1347);
				match(RBRACKET);
				}
			}

			setState(1350);
			arrowLine();
			setState(1352);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==GT || _la==ARROW_RIGHT_HEAD) {
				{
				setState(1351);
				rightArrow();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class InsertRelationshipPatternContext extends AstRuleCtx {
		public List<ArrowLineContext> arrowLine() {
			return getRuleContexts(ArrowLineContext.class);
		}
		public ArrowLineContext arrowLine(int i) {
			return getRuleContext(ArrowLineContext.class,i);
		}
		public TerminalNode LBRACKET() { return getToken(Cypher5Parser.LBRACKET, 0); }
		public InsertRelationshipLabelExpressionContext insertRelationshipLabelExpression() {
			return getRuleContext(InsertRelationshipLabelExpressionContext.class,0);
		}
		public TerminalNode RBRACKET() { return getToken(Cypher5Parser.RBRACKET, 0); }
		public LeftArrowContext leftArrow() {
			return getRuleContext(LeftArrowContext.class,0);
		}
		public VariableContext variable() {
			return getRuleContext(VariableContext.class,0);
		}
		public MapContext map() {
			return getRuleContext(MapContext.class,0);
		}
		public RightArrowContext rightArrow() {
			return getRuleContext(RightArrowContext.class,0);
		}
		public InsertRelationshipPatternContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_insertRelationshipPattern; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterInsertRelationshipPattern(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitInsertRelationshipPattern(this);
		}
	}

	public final InsertRelationshipPatternContext insertRelationshipPattern() throws RecognitionException {
		InsertRelationshipPatternContext _localctx = new InsertRelationshipPatternContext(_ctx, getState());
		enterRule(_localctx, 144, RULE_insertRelationshipPattern);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1355);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LT || _la==ARROW_LEFT_HEAD) {
				{
				setState(1354);
				leftArrow();
				}
			}

			setState(1357);
			arrowLine();
			setState(1358);
			match(LBRACKET);
			setState(1360);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,113,_ctx) ) {
			case 1:
				{
				setState(1359);
				variable();
				}
				break;
			}
			setState(1362);
			insertRelationshipLabelExpression();
			setState(1364);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LCURLY) {
				{
				setState(1363);
				map();
				}
			}

			setState(1366);
			match(RBRACKET);
			setState(1367);
			arrowLine();
			setState(1369);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==GT || _la==ARROW_RIGHT_HEAD) {
				{
				setState(1368);
				rightArrow();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class LeftArrowContext extends AstRuleCtx {
		public TerminalNode LT() { return getToken(Cypher5Parser.LT, 0); }
		public TerminalNode ARROW_LEFT_HEAD() { return getToken(Cypher5Parser.ARROW_LEFT_HEAD, 0); }
		public LeftArrowContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_leftArrow; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterLeftArrow(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitLeftArrow(this);
		}
	}

	public final LeftArrowContext leftArrow() throws RecognitionException {
		LeftArrowContext _localctx = new LeftArrowContext(_ctx, getState());
		enterRule(_localctx, 146, RULE_leftArrow);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1371);
			_la = _input.LA(1);
			if ( !(_la==LT || _la==ARROW_LEFT_HEAD) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ArrowLineContext extends AstRuleCtx {
		public TerminalNode ARROW_LINE() { return getToken(Cypher5Parser.ARROW_LINE, 0); }
		public TerminalNode MINUS() { return getToken(Cypher5Parser.MINUS, 0); }
		public ArrowLineContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_arrowLine; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterArrowLine(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitArrowLine(this);
		}
	}

	public final ArrowLineContext arrowLine() throws RecognitionException {
		ArrowLineContext _localctx = new ArrowLineContext(_ctx, getState());
		enterRule(_localctx, 148, RULE_arrowLine);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1373);
			_la = _input.LA(1);
			if ( !(_la==MINUS || _la==ARROW_LINE) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class RightArrowContext extends AstRuleCtx {
		public TerminalNode GT() { return getToken(Cypher5Parser.GT, 0); }
		public TerminalNode ARROW_RIGHT_HEAD() { return getToken(Cypher5Parser.ARROW_RIGHT_HEAD, 0); }
		public RightArrowContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_rightArrow; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterRightArrow(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitRightArrow(this);
		}
	}

	public final RightArrowContext rightArrow() throws RecognitionException {
		RightArrowContext _localctx = new RightArrowContext(_ctx, getState());
		enterRule(_localctx, 150, RULE_rightArrow);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1375);
			_la = _input.LA(1);
			if ( !(_la==GT || _la==ARROW_RIGHT_HEAD) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class PathLengthContext extends AstRuleCtx {
		public Token from;
		public Token to;
		public Token single;
		public TerminalNode TIMES() { return getToken(Cypher5Parser.TIMES, 0); }
		public TerminalNode DOTDOT() { return getToken(Cypher5Parser.DOTDOT, 0); }
		public List<TerminalNode> UNSIGNED_DECIMAL_INTEGER() { return getTokens(Cypher5Parser.UNSIGNED_DECIMAL_INTEGER); }
		public TerminalNode UNSIGNED_DECIMAL_INTEGER(int i) {
			return getToken(Cypher5Parser.UNSIGNED_DECIMAL_INTEGER, i);
		}
		public PathLengthContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_pathLength; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterPathLength(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitPathLength(this);
		}
	}

	public final PathLengthContext pathLength() throws RecognitionException {
		PathLengthContext _localctx = new PathLengthContext(_ctx, getState());
		enterRule(_localctx, 152, RULE_pathLength);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1377);
			match(TIMES);
			setState(1386);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,118,_ctx) ) {
			case 1:
				{
				setState(1379);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==UNSIGNED_DECIMAL_INTEGER) {
					{
					setState(1378);
					((PathLengthContext)_localctx).from = match(UNSIGNED_DECIMAL_INTEGER);
					}
				}

				setState(1381);
				match(DOTDOT);
				setState(1383);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==UNSIGNED_DECIMAL_INTEGER) {
					{
					setState(1382);
					((PathLengthContext)_localctx).to = match(UNSIGNED_DECIMAL_INTEGER);
					}
				}

				}
				break;
			case 2:
				{
				setState(1385);
				((PathLengthContext)_localctx).single = match(UNSIGNED_DECIMAL_INTEGER);
				}
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class LabelExpressionContext extends AstRuleCtx {
		public TerminalNode COLON() { return getToken(Cypher5Parser.COLON, 0); }
		public LabelExpression4Context labelExpression4() {
			return getRuleContext(LabelExpression4Context.class,0);
		}
		public TerminalNode IS() { return getToken(Cypher5Parser.IS, 0); }
		public LabelExpression4IsContext labelExpression4Is() {
			return getRuleContext(LabelExpression4IsContext.class,0);
		}
		public LabelExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_labelExpression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterLabelExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitLabelExpression(this);
		}
	}

	public final LabelExpressionContext labelExpression() throws RecognitionException {
		LabelExpressionContext _localctx = new LabelExpressionContext(_ctx, getState());
		enterRule(_localctx, 154, RULE_labelExpression);
		try {
			setState(1392);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case COLON:
				enterOuterAlt(_localctx, 1);
				{
				setState(1388);
				match(COLON);
				setState(1389);
				labelExpression4();
				}
				break;
			case IS:
				enterOuterAlt(_localctx, 2);
				{
				setState(1390);
				match(IS);
				setState(1391);
				labelExpression4Is();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class LabelExpression4Context extends AstRuleCtx {
		public List<LabelExpression3Context> labelExpression3() {
			return getRuleContexts(LabelExpression3Context.class);
		}
		public LabelExpression3Context labelExpression3(int i) {
			return getRuleContext(LabelExpression3Context.class,i);
		}
		public List<TerminalNode> BAR() { return getTokens(Cypher5Parser.BAR); }
		public TerminalNode BAR(int i) {
			return getToken(Cypher5Parser.BAR, i);
		}
		public List<TerminalNode> COLON() { return getTokens(Cypher5Parser.COLON); }
		public TerminalNode COLON(int i) {
			return getToken(Cypher5Parser.COLON, i);
		}
		public LabelExpression4Context(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_labelExpression4; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterLabelExpression4(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitLabelExpression4(this);
		}
	}

	public final LabelExpression4Context labelExpression4() throws RecognitionException {
		LabelExpression4Context _localctx = new LabelExpression4Context(_ctx, getState());
		enterRule(_localctx, 156, RULE_labelExpression4);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(1394);
			labelExpression3();
			setState(1402);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,121,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(1395);
					match(BAR);
					setState(1397);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==COLON) {
						{
						setState(1396);
						match(COLON);
						}
					}

					setState(1399);
					labelExpression3();
					}
					} 
				}
				setState(1404);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,121,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class LabelExpression4IsContext extends AstRuleCtx {
		public List<LabelExpression3IsContext> labelExpression3Is() {
			return getRuleContexts(LabelExpression3IsContext.class);
		}
		public LabelExpression3IsContext labelExpression3Is(int i) {
			return getRuleContext(LabelExpression3IsContext.class,i);
		}
		public List<TerminalNode> BAR() { return getTokens(Cypher5Parser.BAR); }
		public TerminalNode BAR(int i) {
			return getToken(Cypher5Parser.BAR, i);
		}
		public List<TerminalNode> COLON() { return getTokens(Cypher5Parser.COLON); }
		public TerminalNode COLON(int i) {
			return getToken(Cypher5Parser.COLON, i);
		}
		public LabelExpression4IsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_labelExpression4Is; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterLabelExpression4Is(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitLabelExpression4Is(this);
		}
	}

	public final LabelExpression4IsContext labelExpression4Is() throws RecognitionException {
		LabelExpression4IsContext _localctx = new LabelExpression4IsContext(_ctx, getState());
		enterRule(_localctx, 158, RULE_labelExpression4Is);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(1405);
			labelExpression3Is();
			setState(1413);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,123,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(1406);
					match(BAR);
					setState(1408);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==COLON) {
						{
						setState(1407);
						match(COLON);
						}
					}

					setState(1410);
					labelExpression3Is();
					}
					} 
				}
				setState(1415);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,123,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class LabelExpression3Context extends AstRuleCtx {
		public List<LabelExpression2Context> labelExpression2() {
			return getRuleContexts(LabelExpression2Context.class);
		}
		public LabelExpression2Context labelExpression2(int i) {
			return getRuleContext(LabelExpression2Context.class,i);
		}
		public List<TerminalNode> AMPERSAND() { return getTokens(Cypher5Parser.AMPERSAND); }
		public TerminalNode AMPERSAND(int i) {
			return getToken(Cypher5Parser.AMPERSAND, i);
		}
		public List<TerminalNode> COLON() { return getTokens(Cypher5Parser.COLON); }
		public TerminalNode COLON(int i) {
			return getToken(Cypher5Parser.COLON, i);
		}
		public LabelExpression3Context(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_labelExpression3; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterLabelExpression3(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitLabelExpression3(this);
		}
	}

	public final LabelExpression3Context labelExpression3() throws RecognitionException {
		LabelExpression3Context _localctx = new LabelExpression3Context(_ctx, getState());
		enterRule(_localctx, 160, RULE_labelExpression3);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(1416);
			labelExpression2();
			setState(1421);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,124,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(1417);
					_la = _input.LA(1);
					if ( !(_la==COLON || _la==AMPERSAND) ) {
					_errHandler.recoverInline(this);
					}
					else {
						if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
						_errHandler.reportMatch(this);
						consume();
					}
					setState(1418);
					labelExpression2();
					}
					} 
				}
				setState(1423);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,124,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class LabelExpression3IsContext extends AstRuleCtx {
		public List<LabelExpression2IsContext> labelExpression2Is() {
			return getRuleContexts(LabelExpression2IsContext.class);
		}
		public LabelExpression2IsContext labelExpression2Is(int i) {
			return getRuleContext(LabelExpression2IsContext.class,i);
		}
		public List<TerminalNode> AMPERSAND() { return getTokens(Cypher5Parser.AMPERSAND); }
		public TerminalNode AMPERSAND(int i) {
			return getToken(Cypher5Parser.AMPERSAND, i);
		}
		public List<TerminalNode> COLON() { return getTokens(Cypher5Parser.COLON); }
		public TerminalNode COLON(int i) {
			return getToken(Cypher5Parser.COLON, i);
		}
		public LabelExpression3IsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_labelExpression3Is; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterLabelExpression3Is(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitLabelExpression3Is(this);
		}
	}

	public final LabelExpression3IsContext labelExpression3Is() throws RecognitionException {
		LabelExpression3IsContext _localctx = new LabelExpression3IsContext(_ctx, getState());
		enterRule(_localctx, 162, RULE_labelExpression3Is);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(1424);
			labelExpression2Is();
			setState(1429);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,125,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(1425);
					_la = _input.LA(1);
					if ( !(_la==COLON || _la==AMPERSAND) ) {
					_errHandler.recoverInline(this);
					}
					else {
						if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
						_errHandler.reportMatch(this);
						consume();
					}
					setState(1426);
					labelExpression2Is();
					}
					} 
				}
				setState(1431);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,125,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class LabelExpression2Context extends AstRuleCtx {
		public LabelExpression1Context labelExpression1() {
			return getRuleContext(LabelExpression1Context.class,0);
		}
		public List<TerminalNode> EXCLAMATION_MARK() { return getTokens(Cypher5Parser.EXCLAMATION_MARK); }
		public TerminalNode EXCLAMATION_MARK(int i) {
			return getToken(Cypher5Parser.EXCLAMATION_MARK, i);
		}
		public LabelExpression2Context(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_labelExpression2; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterLabelExpression2(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitLabelExpression2(this);
		}
	}

	public final LabelExpression2Context labelExpression2() throws RecognitionException {
		LabelExpression2Context _localctx = new LabelExpression2Context(_ctx, getState());
		enterRule(_localctx, 164, RULE_labelExpression2);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1435);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==EXCLAMATION_MARK) {
				{
				{
				setState(1432);
				match(EXCLAMATION_MARK);
				}
				}
				setState(1437);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(1438);
			labelExpression1();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class LabelExpression2IsContext extends AstRuleCtx {
		public LabelExpression1IsContext labelExpression1Is() {
			return getRuleContext(LabelExpression1IsContext.class,0);
		}
		public List<TerminalNode> EXCLAMATION_MARK() { return getTokens(Cypher5Parser.EXCLAMATION_MARK); }
		public TerminalNode EXCLAMATION_MARK(int i) {
			return getToken(Cypher5Parser.EXCLAMATION_MARK, i);
		}
		public LabelExpression2IsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_labelExpression2Is; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterLabelExpression2Is(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitLabelExpression2Is(this);
		}
	}

	public final LabelExpression2IsContext labelExpression2Is() throws RecognitionException {
		LabelExpression2IsContext _localctx = new LabelExpression2IsContext(_ctx, getState());
		enterRule(_localctx, 166, RULE_labelExpression2Is);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1443);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==EXCLAMATION_MARK) {
				{
				{
				setState(1440);
				match(EXCLAMATION_MARK);
				}
				}
				setState(1445);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(1446);
			labelExpression1Is();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class LabelExpression1Context extends AstRuleCtx {
		public LabelExpression1Context(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_labelExpression1; }
	 
		public LabelExpression1Context() { }
		public void copyFrom(LabelExpression1Context ctx) {
			super.copyFrom(ctx);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class AnyLabelContext extends LabelExpression1Context {
		public TerminalNode PERCENT() { return getToken(Cypher5Parser.PERCENT, 0); }
		public AnyLabelContext(LabelExpression1Context ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterAnyLabel(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitAnyLabel(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class DynamicLabelContext extends LabelExpression1Context {
		public DynamicAnyAllExpressionContext dynamicAnyAllExpression() {
			return getRuleContext(DynamicAnyAllExpressionContext.class,0);
		}
		public DynamicLabelContext(LabelExpression1Context ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterDynamicLabel(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitDynamicLabel(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class LabelNameContext extends LabelExpression1Context {
		public SymbolicNameStringContext symbolicNameString() {
			return getRuleContext(SymbolicNameStringContext.class,0);
		}
		public LabelNameContext(LabelExpression1Context ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterLabelName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitLabelName(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class ParenthesizedLabelExpressionContext extends LabelExpression1Context {
		public TerminalNode LPAREN() { return getToken(Cypher5Parser.LPAREN, 0); }
		public LabelExpression4Context labelExpression4() {
			return getRuleContext(LabelExpression4Context.class,0);
		}
		public TerminalNode RPAREN() { return getToken(Cypher5Parser.RPAREN, 0); }
		public ParenthesizedLabelExpressionContext(LabelExpression1Context ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterParenthesizedLabelExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitParenthesizedLabelExpression(this);
		}
	}

	public final LabelExpression1Context labelExpression1() throws RecognitionException {
		LabelExpression1Context _localctx = new LabelExpression1Context(_ctx, getState());
		enterRule(_localctx, 168, RULE_labelExpression1);
		try {
			setState(1455);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case LPAREN:
				_localctx = new ParenthesizedLabelExpressionContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(1448);
				match(LPAREN);
				setState(1449);
				labelExpression4();
				setState(1450);
				match(RPAREN);
				}
				break;
			case PERCENT:
				_localctx = new AnyLabelContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(1452);
				match(PERCENT);
				}
				break;
			case DOLLAR:
				_localctx = new DynamicLabelContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(1453);
				dynamicAnyAllExpression();
				}
				break;
			case ESCAPED_SYMBOLIC_NAME:
			case ACCESS:
			case ACTIVE:
			case ADMIN:
			case ADMINISTRATOR:
			case ALIAS:
			case ALIASES:
			case ALL_SHORTEST_PATHS:
			case ALL:
			case ALTER:
			case AND:
			case ANY:
			case ARRAY:
			case AS:
			case ASC:
			case ASCENDING:
			case ASSERT:
			case ASSIGN:
			case AT:
			case AUTH:
			case BINDINGS:
			case BOOL:
			case BOOLEAN:
			case BOOSTED:
			case BOTH:
			case BREAK:
			case BRIEF:
			case BTREE:
			case BUILT:
			case BY:
			case CALL:
			case CASCADE:
			case CASE:
			case CHANGE:
			case CIDR:
			case COLLECT:
			case COMMAND:
			case COMMANDS:
			case COMMIT:
			case COMPOSITE:
			case CONCURRENT:
			case CONSTRAINT:
			case CONSTRAINTS:
			case CONTAINS:
			case COPY:
			case CONTINUE:
			case COUNT:
			case CREATE:
			case CSV:
			case CURRENT:
			case DATA:
			case DATABASE:
			case DATABASES:
			case DATE:
			case DATETIME:
			case DBMS:
			case DEALLOCATE:
			case DEFAULT:
			case DEFINED:
			case DELETE:
			case DENY:
			case DESC:
			case DESCENDING:
			case DESTROY:
			case DETACH:
			case DIFFERENT:
			case DISTINCT:
			case DRIVER:
			case DROP:
			case DRYRUN:
			case DUMP:
			case DURATION:
			case EACH:
			case EDGE:
			case ENABLE:
			case ELEMENT:
			case ELEMENTS:
			case ELSE:
			case ENCRYPTED:
			case END:
			case ENDS:
			case EXECUTABLE:
			case EXECUTE:
			case EXIST:
			case EXISTENCE:
			case EXISTS:
			case ERROR:
			case FAIL:
			case FALSE:
			case FIELDTERMINATOR:
			case FINISH:
			case FLOAT:
			case FOR:
			case FOREACH:
			case FROM:
			case FULLTEXT:
			case FUNCTION:
			case FUNCTIONS:
			case GRANT:
			case GRAPH:
			case GRAPHS:
			case GROUP:
			case GROUPS:
			case HEADERS:
			case HOME:
			case ID:
			case IF:
			case IMPERSONATE:
			case IMMUTABLE:
			case IN:
			case INDEX:
			case INDEXES:
			case INF:
			case INFINITY:
			case INSERT:
			case INT:
			case INTEGER:
			case IS:
			case JOIN:
			case KEY:
			case LABEL:
			case LABELS:
			case LEADING:
			case LIMITROWS:
			case LIST:
			case LOAD:
			case LOCAL:
			case LOOKUP:
			case MANAGEMENT:
			case MAP:
			case MATCH:
			case MERGE:
			case NAME:
			case NAMES:
			case NAN:
			case NFC:
			case NFD:
			case NFKC:
			case NFKD:
			case NEW:
			case NODE:
			case NODETACH:
			case NODES:
			case NONE:
			case NORMALIZE:
			case NORMALIZED:
			case NOT:
			case NOTHING:
			case NOWAIT:
			case NULL:
			case OF:
			case OFFSET:
			case ON:
			case ONLY:
			case OPTIONAL:
			case OPTIONS:
			case OPTION:
			case OR:
			case ORDER:
			case OUTPUT:
			case PASSWORD:
			case PASSWORDS:
			case PATH:
			case PATHS:
			case PERIODIC:
			case PLAINTEXT:
			case POINT:
			case POPULATED:
			case PRIMARY:
			case PRIMARIES:
			case PRIVILEGE:
			case PRIVILEGES:
			case PROCEDURE:
			case PROCEDURES:
			case PROPERTIES:
			case PROPERTY:
			case PROVIDER:
			case PROVIDERS:
			case RANGE:
			case READ:
			case REALLOCATE:
			case REDUCE:
			case RENAME:
			case REL:
			case RELATIONSHIP:
			case RELATIONSHIPS:
			case REMOVE:
			case REPEATABLE:
			case REPLACE:
			case REPORT:
			case REQUIRE:
			case REQUIRED:
			case RESTRICT:
			case RETURN:
			case REVOKE:
			case ROLE:
			case ROLES:
			case ROW:
			case ROWS:
			case SCAN:
			case SEC:
			case SECOND:
			case SECONDARY:
			case SECONDARIES:
			case SECONDS:
			case SEEK:
			case SERVER:
			case SERVERS:
			case SET:
			case SETTING:
			case SETTINGS:
			case SHORTEST_PATH:
			case SHORTEST:
			case SHOW:
			case SIGNED:
			case SINGLE:
			case SKIPROWS:
			case START:
			case STARTS:
			case STATUS:
			case STOP:
			case STRING:
			case SUPPORTED:
			case SUSPENDED:
			case TARGET:
			case TERMINATE:
			case TEXT:
			case THEN:
			case TIME:
			case TIMESTAMP:
			case TIMEZONE:
			case TO:
			case TOPOLOGY:
			case TRAILING:
			case TRANSACTION:
			case TRANSACTIONS:
			case TRAVERSE:
			case TRIM:
			case TRUE:
			case TYPE:
			case TYPED:
			case TYPES:
			case UNION:
			case UNIQUE:
			case UNIQUENESS:
			case UNWIND:
			case URL:
			case USE:
			case USER:
			case USERS:
			case USING:
			case VALUE:
			case VARCHAR:
			case VECTOR:
			case VERBOSE:
			case VERTEX:
			case WAIT:
			case WHEN:
			case WHERE:
			case WITH:
			case WITHOUT:
			case WRITE:
			case XOR:
			case YIELD:
			case ZONE:
			case ZONED:
			case IDENTIFIER:
				_localctx = new LabelNameContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(1454);
				symbolicNameString();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class LabelExpression1IsContext extends AstRuleCtx {
		public LabelExpression1IsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_labelExpression1Is; }
	 
		public LabelExpression1IsContext() { }
		public void copyFrom(LabelExpression1IsContext ctx) {
			super.copyFrom(ctx);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class ParenthesizedLabelExpressionIsContext extends LabelExpression1IsContext {
		public TerminalNode LPAREN() { return getToken(Cypher5Parser.LPAREN, 0); }
		public LabelExpression4IsContext labelExpression4Is() {
			return getRuleContext(LabelExpression4IsContext.class,0);
		}
		public TerminalNode RPAREN() { return getToken(Cypher5Parser.RPAREN, 0); }
		public ParenthesizedLabelExpressionIsContext(LabelExpression1IsContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterParenthesizedLabelExpressionIs(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitParenthesizedLabelExpressionIs(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class DynamicLabelIsContext extends LabelExpression1IsContext {
		public DynamicAnyAllExpressionContext dynamicAnyAllExpression() {
			return getRuleContext(DynamicAnyAllExpressionContext.class,0);
		}
		public DynamicLabelIsContext(LabelExpression1IsContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterDynamicLabelIs(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitDynamicLabelIs(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class AnyLabelIsContext extends LabelExpression1IsContext {
		public TerminalNode PERCENT() { return getToken(Cypher5Parser.PERCENT, 0); }
		public AnyLabelIsContext(LabelExpression1IsContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterAnyLabelIs(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitAnyLabelIs(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class LabelNameIsContext extends LabelExpression1IsContext {
		public SymbolicLabelNameStringContext symbolicLabelNameString() {
			return getRuleContext(SymbolicLabelNameStringContext.class,0);
		}
		public LabelNameIsContext(LabelExpression1IsContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterLabelNameIs(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitLabelNameIs(this);
		}
	}

	public final LabelExpression1IsContext labelExpression1Is() throws RecognitionException {
		LabelExpression1IsContext _localctx = new LabelExpression1IsContext(_ctx, getState());
		enterRule(_localctx, 170, RULE_labelExpression1Is);
		try {
			setState(1464);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case LPAREN:
				_localctx = new ParenthesizedLabelExpressionIsContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(1457);
				match(LPAREN);
				setState(1458);
				labelExpression4Is();
				setState(1459);
				match(RPAREN);
				}
				break;
			case PERCENT:
				_localctx = new AnyLabelIsContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(1461);
				match(PERCENT);
				}
				break;
			case DOLLAR:
				_localctx = new DynamicLabelIsContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(1462);
				dynamicAnyAllExpression();
				}
				break;
			case ESCAPED_SYMBOLIC_NAME:
			case ACCESS:
			case ACTIVE:
			case ADMIN:
			case ADMINISTRATOR:
			case ALIAS:
			case ALIASES:
			case ALL_SHORTEST_PATHS:
			case ALL:
			case ALTER:
			case AND:
			case ANY:
			case ARRAY:
			case AS:
			case ASC:
			case ASCENDING:
			case ASSERT:
			case ASSIGN:
			case AT:
			case AUTH:
			case BINDINGS:
			case BOOL:
			case BOOLEAN:
			case BOOSTED:
			case BOTH:
			case BREAK:
			case BRIEF:
			case BTREE:
			case BUILT:
			case BY:
			case CALL:
			case CASCADE:
			case CASE:
			case CHANGE:
			case CIDR:
			case COLLECT:
			case COMMAND:
			case COMMANDS:
			case COMMIT:
			case COMPOSITE:
			case CONCURRENT:
			case CONSTRAINT:
			case CONSTRAINTS:
			case CONTAINS:
			case COPY:
			case CONTINUE:
			case COUNT:
			case CREATE:
			case CSV:
			case CURRENT:
			case DATA:
			case DATABASE:
			case DATABASES:
			case DATE:
			case DATETIME:
			case DBMS:
			case DEALLOCATE:
			case DEFAULT:
			case DEFINED:
			case DELETE:
			case DENY:
			case DESC:
			case DESCENDING:
			case DESTROY:
			case DETACH:
			case DIFFERENT:
			case DISTINCT:
			case DRIVER:
			case DROP:
			case DRYRUN:
			case DUMP:
			case DURATION:
			case EACH:
			case EDGE:
			case ENABLE:
			case ELEMENT:
			case ELEMENTS:
			case ELSE:
			case ENCRYPTED:
			case END:
			case ENDS:
			case EXECUTABLE:
			case EXECUTE:
			case EXIST:
			case EXISTENCE:
			case EXISTS:
			case ERROR:
			case FAIL:
			case FALSE:
			case FIELDTERMINATOR:
			case FINISH:
			case FLOAT:
			case FOR:
			case FOREACH:
			case FROM:
			case FULLTEXT:
			case FUNCTION:
			case FUNCTIONS:
			case GRANT:
			case GRAPH:
			case GRAPHS:
			case GROUP:
			case GROUPS:
			case HEADERS:
			case HOME:
			case ID:
			case IF:
			case IMPERSONATE:
			case IMMUTABLE:
			case IN:
			case INDEX:
			case INDEXES:
			case INF:
			case INFINITY:
			case INSERT:
			case INT:
			case INTEGER:
			case IS:
			case JOIN:
			case KEY:
			case LABEL:
			case LABELS:
			case LEADING:
			case LIMITROWS:
			case LIST:
			case LOAD:
			case LOCAL:
			case LOOKUP:
			case MANAGEMENT:
			case MAP:
			case MATCH:
			case MERGE:
			case NAME:
			case NAMES:
			case NAN:
			case NEW:
			case NODE:
			case NODETACH:
			case NODES:
			case NONE:
			case NORMALIZE:
			case NOTHING:
			case NOWAIT:
			case OF:
			case OFFSET:
			case ON:
			case ONLY:
			case OPTIONAL:
			case OPTIONS:
			case OPTION:
			case OR:
			case ORDER:
			case OUTPUT:
			case PASSWORD:
			case PASSWORDS:
			case PATH:
			case PATHS:
			case PERIODIC:
			case PLAINTEXT:
			case POINT:
			case POPULATED:
			case PRIMARY:
			case PRIMARIES:
			case PRIVILEGE:
			case PRIVILEGES:
			case PROCEDURE:
			case PROCEDURES:
			case PROPERTIES:
			case PROPERTY:
			case PROVIDER:
			case PROVIDERS:
			case RANGE:
			case READ:
			case REALLOCATE:
			case REDUCE:
			case RENAME:
			case REL:
			case RELATIONSHIP:
			case RELATIONSHIPS:
			case REMOVE:
			case REPEATABLE:
			case REPLACE:
			case REPORT:
			case REQUIRE:
			case REQUIRED:
			case RESTRICT:
			case RETURN:
			case REVOKE:
			case ROLE:
			case ROLES:
			case ROW:
			case ROWS:
			case SCAN:
			case SEC:
			case SECOND:
			case SECONDARY:
			case SECONDARIES:
			case SECONDS:
			case SEEK:
			case SERVER:
			case SERVERS:
			case SET:
			case SETTING:
			case SETTINGS:
			case SHORTEST_PATH:
			case SHORTEST:
			case SHOW:
			case SIGNED:
			case SINGLE:
			case SKIPROWS:
			case START:
			case STARTS:
			case STATUS:
			case STOP:
			case STRING:
			case SUPPORTED:
			case SUSPENDED:
			case TARGET:
			case TERMINATE:
			case TEXT:
			case THEN:
			case TIME:
			case TIMESTAMP:
			case TIMEZONE:
			case TO:
			case TOPOLOGY:
			case TRAILING:
			case TRANSACTION:
			case TRANSACTIONS:
			case TRAVERSE:
			case TRIM:
			case TRUE:
			case TYPE:
			case TYPES:
			case UNION:
			case UNIQUE:
			case UNIQUENESS:
			case UNWIND:
			case URL:
			case USE:
			case USER:
			case USERS:
			case USING:
			case VALUE:
			case VARCHAR:
			case VECTOR:
			case VERBOSE:
			case VERTEX:
			case WAIT:
			case WHEN:
			case WHERE:
			case WITH:
			case WITHOUT:
			case WRITE:
			case XOR:
			case YIELD:
			case ZONE:
			case ZONED:
			case IDENTIFIER:
				_localctx = new LabelNameIsContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(1463);
				symbolicLabelNameString();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class InsertNodeLabelExpressionContext extends AstRuleCtx {
		public List<SymbolicNameStringContext> symbolicNameString() {
			return getRuleContexts(SymbolicNameStringContext.class);
		}
		public SymbolicNameStringContext symbolicNameString(int i) {
			return getRuleContext(SymbolicNameStringContext.class,i);
		}
		public List<TerminalNode> COLON() { return getTokens(Cypher5Parser.COLON); }
		public TerminalNode COLON(int i) {
			return getToken(Cypher5Parser.COLON, i);
		}
		public TerminalNode IS() { return getToken(Cypher5Parser.IS, 0); }
		public List<TerminalNode> AMPERSAND() { return getTokens(Cypher5Parser.AMPERSAND); }
		public TerminalNode AMPERSAND(int i) {
			return getToken(Cypher5Parser.AMPERSAND, i);
		}
		public InsertNodeLabelExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_insertNodeLabelExpression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterInsertNodeLabelExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitInsertNodeLabelExpression(this);
		}
	}

	public final InsertNodeLabelExpressionContext insertNodeLabelExpression() throws RecognitionException {
		InsertNodeLabelExpressionContext _localctx = new InsertNodeLabelExpressionContext(_ctx, getState());
		enterRule(_localctx, 172, RULE_insertNodeLabelExpression);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1466);
			_la = _input.LA(1);
			if ( !(_la==COLON || _la==IS) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			setState(1467);
			symbolicNameString();
			setState(1472);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COLON || _la==AMPERSAND) {
				{
				{
				setState(1468);
				_la = _input.LA(1);
				if ( !(_la==COLON || _la==AMPERSAND) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(1469);
				symbolicNameString();
				}
				}
				setState(1474);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class InsertRelationshipLabelExpressionContext extends AstRuleCtx {
		public SymbolicNameStringContext symbolicNameString() {
			return getRuleContext(SymbolicNameStringContext.class,0);
		}
		public TerminalNode COLON() { return getToken(Cypher5Parser.COLON, 0); }
		public TerminalNode IS() { return getToken(Cypher5Parser.IS, 0); }
		public InsertRelationshipLabelExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_insertRelationshipLabelExpression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterInsertRelationshipLabelExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitInsertRelationshipLabelExpression(this);
		}
	}

	public final InsertRelationshipLabelExpressionContext insertRelationshipLabelExpression() throws RecognitionException {
		InsertRelationshipLabelExpressionContext _localctx = new InsertRelationshipLabelExpressionContext(_ctx, getState());
		enterRule(_localctx, 174, RULE_insertRelationshipLabelExpression);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1475);
			_la = _input.LA(1);
			if ( !(_la==COLON || _la==IS) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			setState(1476);
			symbolicNameString();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ExpressionContext extends AstRuleCtx {
		public List<Expression11Context> expression11() {
			return getRuleContexts(Expression11Context.class);
		}
		public Expression11Context expression11(int i) {
			return getRuleContext(Expression11Context.class,i);
		}
		public List<TerminalNode> OR() { return getTokens(Cypher5Parser.OR); }
		public TerminalNode OR(int i) {
			return getToken(Cypher5Parser.OR, i);
		}
		public ExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_expression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitExpression(this);
		}
	}

	public final ExpressionContext expression() throws RecognitionException {
		ExpressionContext _localctx = new ExpressionContext(_ctx, getState());
		enterRule(_localctx, 176, RULE_expression);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1478);
			expression11();
			setState(1483);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==OR) {
				{
				{
				setState(1479);
				match(OR);
				setState(1480);
				expression11();
				}
				}
				setState(1485);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Expression11Context extends AstRuleCtx {
		public List<Expression10Context> expression10() {
			return getRuleContexts(Expression10Context.class);
		}
		public Expression10Context expression10(int i) {
			return getRuleContext(Expression10Context.class,i);
		}
		public List<TerminalNode> XOR() { return getTokens(Cypher5Parser.XOR); }
		public TerminalNode XOR(int i) {
			return getToken(Cypher5Parser.XOR, i);
		}
		public Expression11Context(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_expression11; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterExpression11(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitExpression11(this);
		}
	}

	public final Expression11Context expression11() throws RecognitionException {
		Expression11Context _localctx = new Expression11Context(_ctx, getState());
		enterRule(_localctx, 178, RULE_expression11);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1486);
			expression10();
			setState(1491);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==XOR) {
				{
				{
				setState(1487);
				match(XOR);
				setState(1488);
				expression10();
				}
				}
				setState(1493);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Expression10Context extends AstRuleCtx {
		public List<Expression9Context> expression9() {
			return getRuleContexts(Expression9Context.class);
		}
		public Expression9Context expression9(int i) {
			return getRuleContext(Expression9Context.class,i);
		}
		public List<TerminalNode> AND() { return getTokens(Cypher5Parser.AND); }
		public TerminalNode AND(int i) {
			return getToken(Cypher5Parser.AND, i);
		}
		public Expression10Context(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_expression10; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterExpression10(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitExpression10(this);
		}
	}

	public final Expression10Context expression10() throws RecognitionException {
		Expression10Context _localctx = new Expression10Context(_ctx, getState());
		enterRule(_localctx, 180, RULE_expression10);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1494);
			expression9();
			setState(1499);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AND) {
				{
				{
				setState(1495);
				match(AND);
				setState(1496);
				expression9();
				}
				}
				setState(1501);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Expression9Context extends AstRuleCtx {
		public Expression8Context expression8() {
			return getRuleContext(Expression8Context.class,0);
		}
		public List<TerminalNode> NOT() { return getTokens(Cypher5Parser.NOT); }
		public TerminalNode NOT(int i) {
			return getToken(Cypher5Parser.NOT, i);
		}
		public Expression9Context(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_expression9; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterExpression9(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitExpression9(this);
		}
	}

	public final Expression9Context expression9() throws RecognitionException {
		Expression9Context _localctx = new Expression9Context(_ctx, getState());
		enterRule(_localctx, 182, RULE_expression9);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(1505);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,134,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(1502);
					match(NOT);
					}
					} 
				}
				setState(1507);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,134,_ctx);
			}
			setState(1508);
			expression8();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Expression8Context extends AstRuleCtx {
		public List<Expression7Context> expression7() {
			return getRuleContexts(Expression7Context.class);
		}
		public Expression7Context expression7(int i) {
			return getRuleContext(Expression7Context.class,i);
		}
		public List<TerminalNode> EQ() { return getTokens(Cypher5Parser.EQ); }
		public TerminalNode EQ(int i) {
			return getToken(Cypher5Parser.EQ, i);
		}
		public List<TerminalNode> INVALID_NEQ() { return getTokens(Cypher5Parser.INVALID_NEQ); }
		public TerminalNode INVALID_NEQ(int i) {
			return getToken(Cypher5Parser.INVALID_NEQ, i);
		}
		public List<TerminalNode> NEQ() { return getTokens(Cypher5Parser.NEQ); }
		public TerminalNode NEQ(int i) {
			return getToken(Cypher5Parser.NEQ, i);
		}
		public List<TerminalNode> LE() { return getTokens(Cypher5Parser.LE); }
		public TerminalNode LE(int i) {
			return getToken(Cypher5Parser.LE, i);
		}
		public List<TerminalNode> GE() { return getTokens(Cypher5Parser.GE); }
		public TerminalNode GE(int i) {
			return getToken(Cypher5Parser.GE, i);
		}
		public List<TerminalNode> LT() { return getTokens(Cypher5Parser.LT); }
		public TerminalNode LT(int i) {
			return getToken(Cypher5Parser.LT, i);
		}
		public List<TerminalNode> GT() { return getTokens(Cypher5Parser.GT); }
		public TerminalNode GT(int i) {
			return getToken(Cypher5Parser.GT, i);
		}
		public Expression8Context(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_expression8; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterExpression8(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitExpression8(this);
		}
	}

	public final Expression8Context expression8() throws RecognitionException {
		Expression8Context _localctx = new Expression8Context(_ctx, getState());
		enterRule(_localctx, 184, RULE_expression8);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1510);
			expression7();
			setState(1515);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (((((_la - 100)) & ~0x3f) == 0 && ((1L << (_la - 100)) & -9151032967823097855L) != 0) || _la==NEQ) {
				{
				{
				setState(1511);
				_la = _input.LA(1);
				if ( !(((((_la - 100)) & ~0x3f) == 0 && ((1L << (_la - 100)) & -9151032967823097855L) != 0) || _la==NEQ) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(1512);
				expression7();
				}
				}
				setState(1517);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Expression7Context extends AstRuleCtx {
		public Expression6Context expression6() {
			return getRuleContext(Expression6Context.class,0);
		}
		public ComparisonExpression6Context comparisonExpression6() {
			return getRuleContext(ComparisonExpression6Context.class,0);
		}
		public Expression7Context(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_expression7; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterExpression7(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitExpression7(this);
		}
	}

	public final Expression7Context expression7() throws RecognitionException {
		Expression7Context _localctx = new Expression7Context(_ctx, getState());
		enterRule(_localctx, 186, RULE_expression7);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1518);
			expression6();
			setState(1520);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==COLONCOLON || _la==CONTAINS || ((((_la - 99)) & ~0x3f) == 0 && ((1L << (_la - 99)) & 1103806595073L) != 0) || _la==REGEQ || _la==STARTS) {
				{
				setState(1519);
				comparisonExpression6();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ComparisonExpression6Context extends AstRuleCtx {
		public ComparisonExpression6Context(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_comparisonExpression6; }
	 
		public ComparisonExpression6Context() { }
		public void copyFrom(ComparisonExpression6Context ctx) {
			super.copyFrom(ctx);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class TypeComparisonContext extends ComparisonExpression6Context {
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public TerminalNode IS() { return getToken(Cypher5Parser.IS, 0); }
		public TerminalNode COLONCOLON() { return getToken(Cypher5Parser.COLONCOLON, 0); }
		public TerminalNode TYPED() { return getToken(Cypher5Parser.TYPED, 0); }
		public TerminalNode NOT() { return getToken(Cypher5Parser.NOT, 0); }
		public TypeComparisonContext(ComparisonExpression6Context ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterTypeComparison(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitTypeComparison(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class StringAndListComparisonContext extends ComparisonExpression6Context {
		public Expression6Context expression6() {
			return getRuleContext(Expression6Context.class,0);
		}
		public TerminalNode REGEQ() { return getToken(Cypher5Parser.REGEQ, 0); }
		public TerminalNode STARTS() { return getToken(Cypher5Parser.STARTS, 0); }
		public TerminalNode WITH() { return getToken(Cypher5Parser.WITH, 0); }
		public TerminalNode ENDS() { return getToken(Cypher5Parser.ENDS, 0); }
		public TerminalNode CONTAINS() { return getToken(Cypher5Parser.CONTAINS, 0); }
		public TerminalNode IN() { return getToken(Cypher5Parser.IN, 0); }
		public StringAndListComparisonContext(ComparisonExpression6Context ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterStringAndListComparison(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitStringAndListComparison(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class NormalFormComparisonContext extends ComparisonExpression6Context {
		public TerminalNode IS() { return getToken(Cypher5Parser.IS, 0); }
		public TerminalNode NORMALIZED() { return getToken(Cypher5Parser.NORMALIZED, 0); }
		public TerminalNode NOT() { return getToken(Cypher5Parser.NOT, 0); }
		public NormalFormContext normalForm() {
			return getRuleContext(NormalFormContext.class,0);
		}
		public NormalFormComparisonContext(ComparisonExpression6Context ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterNormalFormComparison(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitNormalFormComparison(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class NullComparisonContext extends ComparisonExpression6Context {
		public TerminalNode IS() { return getToken(Cypher5Parser.IS, 0); }
		public TerminalNode NULL() { return getToken(Cypher5Parser.NULL, 0); }
		public TerminalNode NOT() { return getToken(Cypher5Parser.NOT, 0); }
		public NullComparisonContext(ComparisonExpression6Context ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterNullComparison(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitNullComparison(this);
		}
	}

	public final ComparisonExpression6Context comparisonExpression6() throws RecognitionException {
		ComparisonExpression6Context _localctx = new ComparisonExpression6Context(_ctx, getState());
		enterRule(_localctx, 188, RULE_comparisonExpression6);
		int _la;
		try {
			setState(1554);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,143,_ctx) ) {
			case 1:
				_localctx = new StringAndListComparisonContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(1529);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case REGEQ:
					{
					setState(1522);
					match(REGEQ);
					}
					break;
				case STARTS:
					{
					setState(1523);
					match(STARTS);
					setState(1524);
					match(WITH);
					}
					break;
				case ENDS:
					{
					setState(1525);
					match(ENDS);
					setState(1526);
					match(WITH);
					}
					break;
				case CONTAINS:
					{
					setState(1527);
					match(CONTAINS);
					}
					break;
				case IN:
					{
					setState(1528);
					match(IN);
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(1531);
				expression6();
				}
				break;
			case 2:
				_localctx = new NullComparisonContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(1532);
				match(IS);
				setState(1534);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==NOT) {
					{
					setState(1533);
					match(NOT);
					}
				}

				setState(1536);
				match(NULL);
				}
				break;
			case 3:
				_localctx = new TypeComparisonContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(1543);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case IS:
					{
					setState(1537);
					match(IS);
					setState(1539);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==NOT) {
						{
						setState(1538);
						match(NOT);
						}
					}

					setState(1541);
					_la = _input.LA(1);
					if ( !(_la==COLONCOLON || _la==TYPED) ) {
					_errHandler.recoverInline(this);
					}
					else {
						if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
						_errHandler.reportMatch(this);
						consume();
					}
					}
					break;
				case COLONCOLON:
					{
					setState(1542);
					match(COLONCOLON);
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(1545);
				type();
				}
				break;
			case 4:
				_localctx = new NormalFormComparisonContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(1546);
				match(IS);
				setState(1548);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==NOT) {
					{
					setState(1547);
					match(NOT);
					}
				}

				setState(1551);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (((((_la - 168)) & ~0x3f) == 0 && ((1L << (_la - 168)) & 15L) != 0)) {
					{
					setState(1550);
					normalForm();
					}
				}

				setState(1553);
				match(NORMALIZED);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class NormalFormContext extends AstRuleCtx {
		public TerminalNode NFC() { return getToken(Cypher5Parser.NFC, 0); }
		public TerminalNode NFD() { return getToken(Cypher5Parser.NFD, 0); }
		public TerminalNode NFKC() { return getToken(Cypher5Parser.NFKC, 0); }
		public TerminalNode NFKD() { return getToken(Cypher5Parser.NFKD, 0); }
		public NormalFormContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_normalForm; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterNormalForm(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitNormalForm(this);
		}
	}

	public final NormalFormContext normalForm() throws RecognitionException {
		NormalFormContext _localctx = new NormalFormContext(_ctx, getState());
		enterRule(_localctx, 190, RULE_normalForm);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1556);
			_la = _input.LA(1);
			if ( !(((((_la - 168)) & ~0x3f) == 0 && ((1L << (_la - 168)) & 15L) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Expression6Context extends AstRuleCtx {
		public List<Expression5Context> expression5() {
			return getRuleContexts(Expression5Context.class);
		}
		public Expression5Context expression5(int i) {
			return getRuleContext(Expression5Context.class,i);
		}
		public List<TerminalNode> PLUS() { return getTokens(Cypher5Parser.PLUS); }
		public TerminalNode PLUS(int i) {
			return getToken(Cypher5Parser.PLUS, i);
		}
		public List<TerminalNode> MINUS() { return getTokens(Cypher5Parser.MINUS); }
		public TerminalNode MINUS(int i) {
			return getToken(Cypher5Parser.MINUS, i);
		}
		public List<TerminalNode> DOUBLEBAR() { return getTokens(Cypher5Parser.DOUBLEBAR); }
		public TerminalNode DOUBLEBAR(int i) {
			return getToken(Cypher5Parser.DOUBLEBAR, i);
		}
		public Expression6Context(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_expression6; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterExpression6(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitExpression6(this);
		}
	}

	public final Expression6Context expression6() throws RecognitionException {
		Expression6Context _localctx = new Expression6Context(_ctx, getState());
		enterRule(_localctx, 192, RULE_expression6);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1558);
			expression5();
			setState(1563);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==DOUBLEBAR || _la==MINUS || _la==PLUS) {
				{
				{
				setState(1559);
				_la = _input.LA(1);
				if ( !(_la==DOUBLEBAR || _la==MINUS || _la==PLUS) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(1560);
				expression5();
				}
				}
				setState(1565);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Expression5Context extends AstRuleCtx {
		public List<Expression4Context> expression4() {
			return getRuleContexts(Expression4Context.class);
		}
		public Expression4Context expression4(int i) {
			return getRuleContext(Expression4Context.class,i);
		}
		public List<TerminalNode> TIMES() { return getTokens(Cypher5Parser.TIMES); }
		public TerminalNode TIMES(int i) {
			return getToken(Cypher5Parser.TIMES, i);
		}
		public List<TerminalNode> DIVIDE() { return getTokens(Cypher5Parser.DIVIDE); }
		public TerminalNode DIVIDE(int i) {
			return getToken(Cypher5Parser.DIVIDE, i);
		}
		public List<TerminalNode> PERCENT() { return getTokens(Cypher5Parser.PERCENT); }
		public TerminalNode PERCENT(int i) {
			return getToken(Cypher5Parser.PERCENT, i);
		}
		public Expression5Context(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_expression5; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterExpression5(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitExpression5(this);
		}
	}

	public final Expression5Context expression5() throws RecognitionException {
		Expression5Context _localctx = new Expression5Context(_ctx, getState());
		enterRule(_localctx, 194, RULE_expression5);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1566);
			expression4();
			setState(1571);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==DIVIDE || _la==PERCENT || _la==TIMES) {
				{
				{
				setState(1567);
				_la = _input.LA(1);
				if ( !(_la==DIVIDE || _la==PERCENT || _la==TIMES) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(1568);
				expression4();
				}
				}
				setState(1573);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Expression4Context extends AstRuleCtx {
		public List<Expression3Context> expression3() {
			return getRuleContexts(Expression3Context.class);
		}
		public Expression3Context expression3(int i) {
			return getRuleContext(Expression3Context.class,i);
		}
		public List<TerminalNode> POW() { return getTokens(Cypher5Parser.POW); }
		public TerminalNode POW(int i) {
			return getToken(Cypher5Parser.POW, i);
		}
		public Expression4Context(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_expression4; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterExpression4(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitExpression4(this);
		}
	}

	public final Expression4Context expression4() throws RecognitionException {
		Expression4Context _localctx = new Expression4Context(_ctx, getState());
		enterRule(_localctx, 196, RULE_expression4);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1574);
			expression3();
			setState(1579);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==POW) {
				{
				{
				setState(1575);
				match(POW);
				setState(1576);
				expression3();
				}
				}
				setState(1581);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Expression3Context extends AstRuleCtx {
		public Expression2Context expression2() {
			return getRuleContext(Expression2Context.class,0);
		}
		public TerminalNode PLUS() { return getToken(Cypher5Parser.PLUS, 0); }
		public TerminalNode MINUS() { return getToken(Cypher5Parser.MINUS, 0); }
		public Expression3Context(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_expression3; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterExpression3(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitExpression3(this);
		}
	}

	public final Expression3Context expression3() throws RecognitionException {
		Expression3Context _localctx = new Expression3Context(_ctx, getState());
		enterRule(_localctx, 198, RULE_expression3);
		int _la;
		try {
			setState(1585);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,147,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1582);
				expression2();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1583);
				_la = _input.LA(1);
				if ( !(_la==MINUS || _la==PLUS) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(1584);
				expression2();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Expression2Context extends AstRuleCtx {
		public Expression1Context expression1() {
			return getRuleContext(Expression1Context.class,0);
		}
		public List<PostFixContext> postFix() {
			return getRuleContexts(PostFixContext.class);
		}
		public PostFixContext postFix(int i) {
			return getRuleContext(PostFixContext.class,i);
		}
		public Expression2Context(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_expression2; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterExpression2(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitExpression2(this);
		}
	}

	public final Expression2Context expression2() throws RecognitionException {
		Expression2Context _localctx = new Expression2Context(_ctx, getState());
		enterRule(_localctx, 200, RULE_expression2);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(1587);
			expression1();
			setState(1591);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,148,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(1588);
					postFix();
					}
					} 
				}
				setState(1593);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,148,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class PostFixContext extends AstRuleCtx {
		public PostFixContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_postFix; }
	 
		public PostFixContext() { }
		public void copyFrom(PostFixContext ctx) {
			super.copyFrom(ctx);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class IndexPostfixContext extends PostFixContext {
		public TerminalNode LBRACKET() { return getToken(Cypher5Parser.LBRACKET, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public TerminalNode RBRACKET() { return getToken(Cypher5Parser.RBRACKET, 0); }
		public IndexPostfixContext(PostFixContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterIndexPostfix(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitIndexPostfix(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class PropertyPostfixContext extends PostFixContext {
		public PropertyContext property() {
			return getRuleContext(PropertyContext.class,0);
		}
		public PropertyPostfixContext(PostFixContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterPropertyPostfix(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitPropertyPostfix(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class LabelPostfixContext extends PostFixContext {
		public LabelExpressionContext labelExpression() {
			return getRuleContext(LabelExpressionContext.class,0);
		}
		public LabelPostfixContext(PostFixContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterLabelPostfix(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitLabelPostfix(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class RangePostfixContext extends PostFixContext {
		public ExpressionContext fromExp;
		public ExpressionContext toExp;
		public TerminalNode LBRACKET() { return getToken(Cypher5Parser.LBRACKET, 0); }
		public TerminalNode DOTDOT() { return getToken(Cypher5Parser.DOTDOT, 0); }
		public TerminalNode RBRACKET() { return getToken(Cypher5Parser.RBRACKET, 0); }
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public RangePostfixContext(PostFixContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterRangePostfix(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitRangePostfix(this);
		}
	}

	public final PostFixContext postFix() throws RecognitionException {
		PostFixContext _localctx = new PostFixContext(_ctx, getState());
		enterRule(_localctx, 202, RULE_postFix);
		int _la;
		try {
			setState(1609);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,151,_ctx) ) {
			case 1:
				_localctx = new PropertyPostfixContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(1594);
				property();
				}
				break;
			case 2:
				_localctx = new LabelPostfixContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(1595);
				labelExpression();
				}
				break;
			case 3:
				_localctx = new IndexPostfixContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(1596);
				match(LBRACKET);
				setState(1597);
				expression();
				setState(1598);
				match(RBRACKET);
				}
				break;
			case 4:
				_localctx = new RangePostfixContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(1600);
				match(LBRACKET);
				setState(1602);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & -985163492229136L) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & -1170935971839737857L) != 0) || ((((_la - 128)) & ~0x3f) == 0 && ((1L << (_la - 128)) & -120528764929L) != 0) || ((((_la - 192)) & ~0x3f) == 0 && ((1L << (_la - 192)) & -36169535635589377L) != 0) || ((((_la - 256)) & ~0x3f) == 0 && ((1L << (_la - 256)) & 18014398509449215L) != 0)) {
					{
					setState(1601);
					((RangePostfixContext)_localctx).fromExp = expression();
					}
				}

				setState(1604);
				match(DOTDOT);
				setState(1606);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & -985163492229136L) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & -1170935971839737857L) != 0) || ((((_la - 128)) & ~0x3f) == 0 && ((1L << (_la - 128)) & -120528764929L) != 0) || ((((_la - 192)) & ~0x3f) == 0 && ((1L << (_la - 192)) & -36169535635589377L) != 0) || ((((_la - 256)) & ~0x3f) == 0 && ((1L << (_la - 256)) & 18014398509449215L) != 0)) {
					{
					setState(1605);
					((RangePostfixContext)_localctx).toExp = expression();
					}
				}

				setState(1608);
				match(RBRACKET);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class PropertyContext extends AstRuleCtx {
		public TerminalNode DOT() { return getToken(Cypher5Parser.DOT, 0); }
		public PropertyKeyNameContext propertyKeyName() {
			return getRuleContext(PropertyKeyNameContext.class,0);
		}
		public PropertyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_property; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterProperty(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitProperty(this);
		}
	}

	public final PropertyContext property() throws RecognitionException {
		PropertyContext _localctx = new PropertyContext(_ctx, getState());
		enterRule(_localctx, 204, RULE_property);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1611);
			match(DOT);
			setState(1612);
			propertyKeyName();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class DynamicPropertyContext extends AstRuleCtx {
		public TerminalNode LBRACKET() { return getToken(Cypher5Parser.LBRACKET, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public TerminalNode RBRACKET() { return getToken(Cypher5Parser.RBRACKET, 0); }
		public DynamicPropertyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_dynamicProperty; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterDynamicProperty(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitDynamicProperty(this);
		}
	}

	public final DynamicPropertyContext dynamicProperty() throws RecognitionException {
		DynamicPropertyContext _localctx = new DynamicPropertyContext(_ctx, getState());
		enterRule(_localctx, 206, RULE_dynamicProperty);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1614);
			match(LBRACKET);
			setState(1615);
			expression();
			setState(1616);
			match(RBRACKET);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class PropertyExpressionContext extends AstRuleCtx {
		public Expression1Context expression1() {
			return getRuleContext(Expression1Context.class,0);
		}
		public List<PropertyContext> property() {
			return getRuleContexts(PropertyContext.class);
		}
		public PropertyContext property(int i) {
			return getRuleContext(PropertyContext.class,i);
		}
		public PropertyExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_propertyExpression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterPropertyExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitPropertyExpression(this);
		}
	}

	public final PropertyExpressionContext propertyExpression() throws RecognitionException {
		PropertyExpressionContext _localctx = new PropertyExpressionContext(_ctx, getState());
		enterRule(_localctx, 208, RULE_propertyExpression);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1618);
			expression1();
			setState(1620); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(1619);
				property();
				}
				}
				setState(1622); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==DOT );
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class DynamicPropertyExpressionContext extends AstRuleCtx {
		public Expression1Context expression1() {
			return getRuleContext(Expression1Context.class,0);
		}
		public DynamicPropertyContext dynamicProperty() {
			return getRuleContext(DynamicPropertyContext.class,0);
		}
		public DynamicPropertyExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_dynamicPropertyExpression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterDynamicPropertyExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitDynamicPropertyExpression(this);
		}
	}

	public final DynamicPropertyExpressionContext dynamicPropertyExpression() throws RecognitionException {
		DynamicPropertyExpressionContext _localctx = new DynamicPropertyExpressionContext(_ctx, getState());
		enterRule(_localctx, 210, RULE_dynamicPropertyExpression);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1624);
			expression1();
			setState(1625);
			dynamicProperty();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Expression1Context extends AstRuleCtx {
		public LiteralContext literal() {
			return getRuleContext(LiteralContext.class,0);
		}
		public ParameterContext parameter() {
			return getRuleContext(ParameterContext.class,0);
		}
		public CaseExpressionContext caseExpression() {
			return getRuleContext(CaseExpressionContext.class,0);
		}
		public ExtendedCaseExpressionContext extendedCaseExpression() {
			return getRuleContext(ExtendedCaseExpressionContext.class,0);
		}
		public CountStarContext countStar() {
			return getRuleContext(CountStarContext.class,0);
		}
		public ExistsExpressionContext existsExpression() {
			return getRuleContext(ExistsExpressionContext.class,0);
		}
		public CountExpressionContext countExpression() {
			return getRuleContext(CountExpressionContext.class,0);
		}
		public CollectExpressionContext collectExpression() {
			return getRuleContext(CollectExpressionContext.class,0);
		}
		public MapProjectionContext mapProjection() {
			return getRuleContext(MapProjectionContext.class,0);
		}
		public ListComprehensionContext listComprehension() {
			return getRuleContext(ListComprehensionContext.class,0);
		}
		public ListLiteralContext listLiteral() {
			return getRuleContext(ListLiteralContext.class,0);
		}
		public PatternComprehensionContext patternComprehension() {
			return getRuleContext(PatternComprehensionContext.class,0);
		}
		public ReduceExpressionContext reduceExpression() {
			return getRuleContext(ReduceExpressionContext.class,0);
		}
		public ListItemsPredicateContext listItemsPredicate() {
			return getRuleContext(ListItemsPredicateContext.class,0);
		}
		public NormalizeFunctionContext normalizeFunction() {
			return getRuleContext(NormalizeFunctionContext.class,0);
		}
		public TrimFunctionContext trimFunction() {
			return getRuleContext(TrimFunctionContext.class,0);
		}
		public PatternExpressionContext patternExpression() {
			return getRuleContext(PatternExpressionContext.class,0);
		}
		public ShortestPathExpressionContext shortestPathExpression() {
			return getRuleContext(ShortestPathExpressionContext.class,0);
		}
		public ParenthesizedExpressionContext parenthesizedExpression() {
			return getRuleContext(ParenthesizedExpressionContext.class,0);
		}
		public FunctionInvocationContext functionInvocation() {
			return getRuleContext(FunctionInvocationContext.class,0);
		}
		public VariableContext variable() {
			return getRuleContext(VariableContext.class,0);
		}
		public Expression1Context(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_expression1; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterExpression1(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitExpression1(this);
		}
	}

	public final Expression1Context expression1() throws RecognitionException {
		Expression1Context _localctx = new Expression1Context(_ctx, getState());
		enterRule(_localctx, 212, RULE_expression1);
		try {
			setState(1648);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,153,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1627);
				literal();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1628);
				parameter("ANY");
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(1629);
				caseExpression();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(1630);
				extendedCaseExpression();
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(1631);
				countStar();
				}
				break;
			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(1632);
				existsExpression();
				}
				break;
			case 7:
				enterOuterAlt(_localctx, 7);
				{
				setState(1633);
				countExpression();
				}
				break;
			case 8:
				enterOuterAlt(_localctx, 8);
				{
				setState(1634);
				collectExpression();
				}
				break;
			case 9:
				enterOuterAlt(_localctx, 9);
				{
				setState(1635);
				mapProjection();
				}
				break;
			case 10:
				enterOuterAlt(_localctx, 10);
				{
				setState(1636);
				listComprehension();
				}
				break;
			case 11:
				enterOuterAlt(_localctx, 11);
				{
				setState(1637);
				listLiteral();
				}
				break;
			case 12:
				enterOuterAlt(_localctx, 12);
				{
				setState(1638);
				patternComprehension();
				}
				break;
			case 13:
				enterOuterAlt(_localctx, 13);
				{
				setState(1639);
				reduceExpression();
				}
				break;
			case 14:
				enterOuterAlt(_localctx, 14);
				{
				setState(1640);
				listItemsPredicate();
				}
				break;
			case 15:
				enterOuterAlt(_localctx, 15);
				{
				setState(1641);
				normalizeFunction();
				}
				break;
			case 16:
				enterOuterAlt(_localctx, 16);
				{
				setState(1642);
				trimFunction();
				}
				break;
			case 17:
				enterOuterAlt(_localctx, 17);
				{
				setState(1643);
				patternExpression();
				}
				break;
			case 18:
				enterOuterAlt(_localctx, 18);
				{
				setState(1644);
				shortestPathExpression();
				}
				break;
			case 19:
				enterOuterAlt(_localctx, 19);
				{
				setState(1645);
				parenthesizedExpression();
				}
				break;
			case 20:
				enterOuterAlt(_localctx, 20);
				{
				setState(1646);
				functionInvocation();
				}
				break;
			case 21:
				enterOuterAlt(_localctx, 21);
				{
				setState(1647);
				variable();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class LiteralContext extends AstRuleCtx {
		public LiteralContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_literal; }
	 
		public LiteralContext() { }
		public void copyFrom(LiteralContext ctx) {
			super.copyFrom(ctx);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class NummericLiteralContext extends LiteralContext {
		public NumberLiteralContext numberLiteral() {
			return getRuleContext(NumberLiteralContext.class,0);
		}
		public NummericLiteralContext(LiteralContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterNummericLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitNummericLiteral(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class BooleanLiteralContext extends LiteralContext {
		public TerminalNode TRUE() { return getToken(Cypher5Parser.TRUE, 0); }
		public TerminalNode FALSE() { return getToken(Cypher5Parser.FALSE, 0); }
		public BooleanLiteralContext(LiteralContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterBooleanLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitBooleanLiteral(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class KeywordLiteralContext extends LiteralContext {
		public TerminalNode INF() { return getToken(Cypher5Parser.INF, 0); }
		public TerminalNode INFINITY() { return getToken(Cypher5Parser.INFINITY, 0); }
		public TerminalNode NAN() { return getToken(Cypher5Parser.NAN, 0); }
		public TerminalNode NULL() { return getToken(Cypher5Parser.NULL, 0); }
		public KeywordLiteralContext(LiteralContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterKeywordLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitKeywordLiteral(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class OtherLiteralContext extends LiteralContext {
		public MapContext map() {
			return getRuleContext(MapContext.class,0);
		}
		public OtherLiteralContext(LiteralContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterOtherLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitOtherLiteral(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class StringsLiteralContext extends LiteralContext {
		public StringLiteralContext stringLiteral() {
			return getRuleContext(StringLiteralContext.class,0);
		}
		public StringsLiteralContext(LiteralContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterStringsLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitStringsLiteral(this);
		}
	}

	public final LiteralContext literal() throws RecognitionException {
		LiteralContext _localctx = new LiteralContext(_ctx, getState());
		enterRule(_localctx, 214, RULE_literal);
		try {
			setState(1659);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case DECIMAL_DOUBLE:
			case UNSIGNED_DECIMAL_INTEGER:
			case UNSIGNED_HEX_INTEGER:
			case UNSIGNED_OCTAL_INTEGER:
			case MINUS:
				_localctx = new NummericLiteralContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(1650);
				numberLiteral();
				}
				break;
			case STRING_LITERAL1:
			case STRING_LITERAL2:
				_localctx = new StringsLiteralContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(1651);
				stringLiteral();
				}
				break;
			case LCURLY:
				_localctx = new OtherLiteralContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(1652);
				map();
				}
				break;
			case TRUE:
				_localctx = new BooleanLiteralContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(1653);
				match(TRUE);
				}
				break;
			case FALSE:
				_localctx = new BooleanLiteralContext(_localctx);
				enterOuterAlt(_localctx, 5);
				{
				setState(1654);
				match(FALSE);
				}
				break;
			case INF:
				_localctx = new KeywordLiteralContext(_localctx);
				enterOuterAlt(_localctx, 6);
				{
				setState(1655);
				match(INF);
				}
				break;
			case INFINITY:
				_localctx = new KeywordLiteralContext(_localctx);
				enterOuterAlt(_localctx, 7);
				{
				setState(1656);
				match(INFINITY);
				}
				break;
			case NAN:
				_localctx = new KeywordLiteralContext(_localctx);
				enterOuterAlt(_localctx, 8);
				{
				setState(1657);
				match(NAN);
				}
				break;
			case NULL:
				_localctx = new KeywordLiteralContext(_localctx);
				enterOuterAlt(_localctx, 9);
				{
				setState(1658);
				match(NULL);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class CaseExpressionContext extends AstRuleCtx {
		public TerminalNode CASE() { return getToken(Cypher5Parser.CASE, 0); }
		public TerminalNode END() { return getToken(Cypher5Parser.END, 0); }
		public List<CaseAlternativeContext> caseAlternative() {
			return getRuleContexts(CaseAlternativeContext.class);
		}
		public CaseAlternativeContext caseAlternative(int i) {
			return getRuleContext(CaseAlternativeContext.class,i);
		}
		public TerminalNode ELSE() { return getToken(Cypher5Parser.ELSE, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public CaseExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_caseExpression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterCaseExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitCaseExpression(this);
		}
	}

	public final CaseExpressionContext caseExpression() throws RecognitionException {
		CaseExpressionContext _localctx = new CaseExpressionContext(_ctx, getState());
		enterRule(_localctx, 216, RULE_caseExpression);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1661);
			match(CASE);
			setState(1663); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(1662);
				caseAlternative();
				}
				}
				setState(1665); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==WHEN );
			setState(1669);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==ELSE) {
				{
				setState(1667);
				match(ELSE);
				setState(1668);
				expression();
				}
			}

			setState(1671);
			match(END);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class CaseAlternativeContext extends AstRuleCtx {
		public TerminalNode WHEN() { return getToken(Cypher5Parser.WHEN, 0); }
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public TerminalNode THEN() { return getToken(Cypher5Parser.THEN, 0); }
		public CaseAlternativeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_caseAlternative; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterCaseAlternative(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitCaseAlternative(this);
		}
	}

	public final CaseAlternativeContext caseAlternative() throws RecognitionException {
		CaseAlternativeContext _localctx = new CaseAlternativeContext(_ctx, getState());
		enterRule(_localctx, 218, RULE_caseAlternative);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1673);
			match(WHEN);
			setState(1674);
			expression();
			setState(1675);
			match(THEN);
			setState(1676);
			expression();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ExtendedCaseExpressionContext extends AstRuleCtx {
		public ExpressionContext elseExp;
		public TerminalNode CASE() { return getToken(Cypher5Parser.CASE, 0); }
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public TerminalNode END() { return getToken(Cypher5Parser.END, 0); }
		public List<ExtendedCaseAlternativeContext> extendedCaseAlternative() {
			return getRuleContexts(ExtendedCaseAlternativeContext.class);
		}
		public ExtendedCaseAlternativeContext extendedCaseAlternative(int i) {
			return getRuleContext(ExtendedCaseAlternativeContext.class,i);
		}
		public TerminalNode ELSE() { return getToken(Cypher5Parser.ELSE, 0); }
		public ExtendedCaseExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_extendedCaseExpression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterExtendedCaseExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitExtendedCaseExpression(this);
		}
	}

	public final ExtendedCaseExpressionContext extendedCaseExpression() throws RecognitionException {
		ExtendedCaseExpressionContext _localctx = new ExtendedCaseExpressionContext(_ctx, getState());
		enterRule(_localctx, 220, RULE_extendedCaseExpression);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1678);
			match(CASE);
			setState(1679);
			expression();
			setState(1681); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(1680);
				extendedCaseAlternative();
				}
				}
				setState(1683); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==WHEN );
			setState(1687);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==ELSE) {
				{
				setState(1685);
				match(ELSE);
				setState(1686);
				((ExtendedCaseExpressionContext)_localctx).elseExp = expression();
				}
			}

			setState(1689);
			match(END);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ExtendedCaseAlternativeContext extends AstRuleCtx {
		public TerminalNode WHEN() { return getToken(Cypher5Parser.WHEN, 0); }
		public List<ExtendedWhenContext> extendedWhen() {
			return getRuleContexts(ExtendedWhenContext.class);
		}
		public ExtendedWhenContext extendedWhen(int i) {
			return getRuleContext(ExtendedWhenContext.class,i);
		}
		public TerminalNode THEN() { return getToken(Cypher5Parser.THEN, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public List<TerminalNode> COMMA() { return getTokens(Cypher5Parser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(Cypher5Parser.COMMA, i);
		}
		public ExtendedCaseAlternativeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_extendedCaseAlternative; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterExtendedCaseAlternative(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitExtendedCaseAlternative(this);
		}
	}

	public final ExtendedCaseAlternativeContext extendedCaseAlternative() throws RecognitionException {
		ExtendedCaseAlternativeContext _localctx = new ExtendedCaseAlternativeContext(_ctx, getState());
		enterRule(_localctx, 222, RULE_extendedCaseAlternative);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1691);
			match(WHEN);
			setState(1692);
			extendedWhen();
			setState(1697);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(1693);
				match(COMMA);
				setState(1694);
				extendedWhen();
				}
				}
				setState(1699);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(1700);
			match(THEN);
			setState(1701);
			expression();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ExtendedWhenContext extends AstRuleCtx {
		public ExtendedWhenContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_extendedWhen; }
	 
		public ExtendedWhenContext() { }
		public void copyFrom(ExtendedWhenContext ctx) {
			super.copyFrom(ctx);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class WhenStringOrListContext extends ExtendedWhenContext {
		public Expression6Context expression6() {
			return getRuleContext(Expression6Context.class,0);
		}
		public TerminalNode REGEQ() { return getToken(Cypher5Parser.REGEQ, 0); }
		public TerminalNode STARTS() { return getToken(Cypher5Parser.STARTS, 0); }
		public TerminalNode WITH() { return getToken(Cypher5Parser.WITH, 0); }
		public TerminalNode ENDS() { return getToken(Cypher5Parser.ENDS, 0); }
		public WhenStringOrListContext(ExtendedWhenContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterWhenStringOrList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitWhenStringOrList(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class WhenTypeContext extends ExtendedWhenContext {
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public TerminalNode IS() { return getToken(Cypher5Parser.IS, 0); }
		public TerminalNode TYPED() { return getToken(Cypher5Parser.TYPED, 0); }
		public TerminalNode COLONCOLON() { return getToken(Cypher5Parser.COLONCOLON, 0); }
		public TerminalNode NOT() { return getToken(Cypher5Parser.NOT, 0); }
		public WhenTypeContext(ExtendedWhenContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterWhenType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitWhenType(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class WhenFormContext extends ExtendedWhenContext {
		public TerminalNode IS() { return getToken(Cypher5Parser.IS, 0); }
		public TerminalNode NORMALIZED() { return getToken(Cypher5Parser.NORMALIZED, 0); }
		public TerminalNode NOT() { return getToken(Cypher5Parser.NOT, 0); }
		public NormalFormContext normalForm() {
			return getRuleContext(NormalFormContext.class,0);
		}
		public WhenFormContext(ExtendedWhenContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterWhenForm(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitWhenForm(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class WhenNullContext extends ExtendedWhenContext {
		public TerminalNode IS() { return getToken(Cypher5Parser.IS, 0); }
		public TerminalNode NULL() { return getToken(Cypher5Parser.NULL, 0); }
		public TerminalNode NOT() { return getToken(Cypher5Parser.NOT, 0); }
		public WhenNullContext(ExtendedWhenContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterWhenNull(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitWhenNull(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class WhenEqualsContext extends ExtendedWhenContext {
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public WhenEqualsContext(ExtendedWhenContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterWhenEquals(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitWhenEquals(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class WhenComparatorContext extends ExtendedWhenContext {
		public Expression7Context expression7() {
			return getRuleContext(Expression7Context.class,0);
		}
		public TerminalNode EQ() { return getToken(Cypher5Parser.EQ, 0); }
		public TerminalNode NEQ() { return getToken(Cypher5Parser.NEQ, 0); }
		public TerminalNode INVALID_NEQ() { return getToken(Cypher5Parser.INVALID_NEQ, 0); }
		public TerminalNode LE() { return getToken(Cypher5Parser.LE, 0); }
		public TerminalNode GE() { return getToken(Cypher5Parser.GE, 0); }
		public TerminalNode LT() { return getToken(Cypher5Parser.LT, 0); }
		public TerminalNode GT() { return getToken(Cypher5Parser.GT, 0); }
		public WhenComparatorContext(ExtendedWhenContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterWhenComparator(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitWhenComparator(this);
		}
	}

	public final ExtendedWhenContext extendedWhen() throws RecognitionException {
		ExtendedWhenContext _localctx = new ExtendedWhenContext(_ctx, getState());
		enterRule(_localctx, 224, RULE_extendedWhen);
		int _la;
		try {
			setState(1736);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,166,_ctx) ) {
			case 1:
				_localctx = new WhenStringOrListContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(1708);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case REGEQ:
					{
					setState(1703);
					match(REGEQ);
					}
					break;
				case STARTS:
					{
					setState(1704);
					match(STARTS);
					setState(1705);
					match(WITH);
					}
					break;
				case ENDS:
					{
					setState(1706);
					match(ENDS);
					setState(1707);
					match(WITH);
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(1710);
				expression6();
				}
				break;
			case 2:
				_localctx = new WhenNullContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(1711);
				match(IS);
				setState(1713);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==NOT) {
					{
					setState(1712);
					match(NOT);
					}
				}

				setState(1715);
				match(NULL);
				}
				break;
			case 3:
				_localctx = new WhenTypeContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(1722);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case IS:
					{
					setState(1716);
					match(IS);
					setState(1718);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==NOT) {
						{
						setState(1717);
						match(NOT);
						}
					}

					setState(1720);
					match(TYPED);
					}
					break;
				case COLONCOLON:
					{
					setState(1721);
					match(COLONCOLON);
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(1724);
				type();
				}
				break;
			case 4:
				_localctx = new WhenFormContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(1725);
				match(IS);
				setState(1727);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==NOT) {
					{
					setState(1726);
					match(NOT);
					}
				}

				setState(1730);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (((((_la - 168)) & ~0x3f) == 0 && ((1L << (_la - 168)) & 15L) != 0)) {
					{
					setState(1729);
					normalForm();
					}
				}

				setState(1732);
				match(NORMALIZED);
				}
				break;
			case 5:
				_localctx = new WhenComparatorContext(_localctx);
				enterOuterAlt(_localctx, 5);
				{
				setState(1733);
				_la = _input.LA(1);
				if ( !(((((_la - 100)) & ~0x3f) == 0 && ((1L << (_la - 100)) & -9151032967823097855L) != 0) || _la==NEQ) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(1734);
				expression7();
				}
				break;
			case 6:
				_localctx = new WhenEqualsContext(_localctx);
				enterOuterAlt(_localctx, 6);
				{
				setState(1735);
				expression();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ListComprehensionContext extends AstRuleCtx {
		public ExpressionContext whereExp;
		public ExpressionContext barExp;
		public TerminalNode LBRACKET() { return getToken(Cypher5Parser.LBRACKET, 0); }
		public VariableContext variable() {
			return getRuleContext(VariableContext.class,0);
		}
		public TerminalNode IN() { return getToken(Cypher5Parser.IN, 0); }
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public TerminalNode RBRACKET() { return getToken(Cypher5Parser.RBRACKET, 0); }
		public TerminalNode BAR() { return getToken(Cypher5Parser.BAR, 0); }
		public TerminalNode WHERE() { return getToken(Cypher5Parser.WHERE, 0); }
		public ListComprehensionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_listComprehension; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterListComprehension(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitListComprehension(this);
		}
	}

	public final ListComprehensionContext listComprehension() throws RecognitionException {
		ListComprehensionContext _localctx = new ListComprehensionContext(_ctx, getState());
		enterRule(_localctx, 226, RULE_listComprehension);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1738);
			match(LBRACKET);
			setState(1739);
			variable();
			setState(1740);
			match(IN);
			setState(1741);
			expression();
			setState(1752);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,169,_ctx) ) {
			case 1:
				{
				setState(1744);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==WHERE) {
					{
					setState(1742);
					match(WHERE);
					setState(1743);
					((ListComprehensionContext)_localctx).whereExp = expression();
					}
				}

				setState(1746);
				match(BAR);
				setState(1747);
				((ListComprehensionContext)_localctx).barExp = expression();
				}
				break;
			case 2:
				{
				setState(1750);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==WHERE) {
					{
					setState(1748);
					match(WHERE);
					setState(1749);
					((ListComprehensionContext)_localctx).whereExp = expression();
					}
				}

				}
				break;
			}
			setState(1754);
			match(RBRACKET);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class PatternComprehensionContext extends AstRuleCtx {
		public ExpressionContext whereExp;
		public ExpressionContext barExp;
		public TerminalNode LBRACKET() { return getToken(Cypher5Parser.LBRACKET, 0); }
		public PathPatternNonEmptyContext pathPatternNonEmpty() {
			return getRuleContext(PathPatternNonEmptyContext.class,0);
		}
		public TerminalNode BAR() { return getToken(Cypher5Parser.BAR, 0); }
		public TerminalNode RBRACKET() { return getToken(Cypher5Parser.RBRACKET, 0); }
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public VariableContext variable() {
			return getRuleContext(VariableContext.class,0);
		}
		public TerminalNode EQ() { return getToken(Cypher5Parser.EQ, 0); }
		public TerminalNode WHERE() { return getToken(Cypher5Parser.WHERE, 0); }
		public PatternComprehensionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_patternComprehension; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterPatternComprehension(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitPatternComprehension(this);
		}
	}

	public final PatternComprehensionContext patternComprehension() throws RecognitionException {
		PatternComprehensionContext _localctx = new PatternComprehensionContext(_ctx, getState());
		enterRule(_localctx, 228, RULE_patternComprehension);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1756);
			match(LBRACKET);
			setState(1760);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & -985163492230144L) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & -1170935971839803393L) != 0) || ((((_la - 128)) & ~0x3f) == 0 && ((1L << (_la - 128)) & -129253703681L) != 0) || ((((_la - 192)) & ~0x3f) == 0 && ((1L << (_la - 192)) & -36169535635589505L) != 0) || ((((_la - 256)) & ~0x3f) == 0 && ((1L << (_la - 256)) & 18014398509449215L) != 0)) {
				{
				setState(1757);
				variable();
				setState(1758);
				match(EQ);
				}
			}

			setState(1762);
			pathPatternNonEmpty();
			setState(1765);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WHERE) {
				{
				setState(1763);
				match(WHERE);
				setState(1764);
				((PatternComprehensionContext)_localctx).whereExp = expression();
				}
			}

			setState(1767);
			match(BAR);
			setState(1768);
			((PatternComprehensionContext)_localctx).barExp = expression();
			setState(1769);
			match(RBRACKET);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ReduceExpressionContext extends AstRuleCtx {
		public TerminalNode REDUCE() { return getToken(Cypher5Parser.REDUCE, 0); }
		public TerminalNode LPAREN() { return getToken(Cypher5Parser.LPAREN, 0); }
		public List<VariableContext> variable() {
			return getRuleContexts(VariableContext.class);
		}
		public VariableContext variable(int i) {
			return getRuleContext(VariableContext.class,i);
		}
		public TerminalNode EQ() { return getToken(Cypher5Parser.EQ, 0); }
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public TerminalNode COMMA() { return getToken(Cypher5Parser.COMMA, 0); }
		public TerminalNode IN() { return getToken(Cypher5Parser.IN, 0); }
		public TerminalNode BAR() { return getToken(Cypher5Parser.BAR, 0); }
		public TerminalNode RPAREN() { return getToken(Cypher5Parser.RPAREN, 0); }
		public ReduceExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_reduceExpression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterReduceExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitReduceExpression(this);
		}
	}

	public final ReduceExpressionContext reduceExpression() throws RecognitionException {
		ReduceExpressionContext _localctx = new ReduceExpressionContext(_ctx, getState());
		enterRule(_localctx, 230, RULE_reduceExpression);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1771);
			match(REDUCE);
			setState(1772);
			match(LPAREN);
			setState(1773);
			variable();
			setState(1774);
			match(EQ);
			setState(1775);
			expression();
			setState(1776);
			match(COMMA);
			setState(1777);
			variable();
			setState(1778);
			match(IN);
			setState(1779);
			expression();
			setState(1780);
			match(BAR);
			setState(1781);
			expression();
			setState(1782);
			match(RPAREN);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ListItemsPredicateContext extends AstRuleCtx {
		public ExpressionContext inExp;
		public ExpressionContext whereExp;
		public TerminalNode LPAREN() { return getToken(Cypher5Parser.LPAREN, 0); }
		public VariableContext variable() {
			return getRuleContext(VariableContext.class,0);
		}
		public TerminalNode IN() { return getToken(Cypher5Parser.IN, 0); }
		public TerminalNode RPAREN() { return getToken(Cypher5Parser.RPAREN, 0); }
		public TerminalNode ALL() { return getToken(Cypher5Parser.ALL, 0); }
		public TerminalNode ANY() { return getToken(Cypher5Parser.ANY, 0); }
		public TerminalNode NONE() { return getToken(Cypher5Parser.NONE, 0); }
		public TerminalNode SINGLE() { return getToken(Cypher5Parser.SINGLE, 0); }
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public TerminalNode WHERE() { return getToken(Cypher5Parser.WHERE, 0); }
		public ListItemsPredicateContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_listItemsPredicate; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterListItemsPredicate(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitListItemsPredicate(this);
		}
	}

	public final ListItemsPredicateContext listItemsPredicate() throws RecognitionException {
		ListItemsPredicateContext _localctx = new ListItemsPredicateContext(_ctx, getState());
		enterRule(_localctx, 232, RULE_listItemsPredicate);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1784);
			_la = _input.LA(1);
			if ( !(_la==ALL || _la==ANY || _la==NONE || _la==SINGLE) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			setState(1785);
			match(LPAREN);
			setState(1786);
			variable();
			setState(1787);
			match(IN);
			setState(1788);
			((ListItemsPredicateContext)_localctx).inExp = expression();
			setState(1791);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WHERE) {
				{
				setState(1789);
				match(WHERE);
				setState(1790);
				((ListItemsPredicateContext)_localctx).whereExp = expression();
				}
			}

			setState(1793);
			match(RPAREN);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class NormalizeFunctionContext extends AstRuleCtx {
		public TerminalNode NORMALIZE() { return getToken(Cypher5Parser.NORMALIZE, 0); }
		public TerminalNode LPAREN() { return getToken(Cypher5Parser.LPAREN, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public TerminalNode RPAREN() { return getToken(Cypher5Parser.RPAREN, 0); }
		public TerminalNode COMMA() { return getToken(Cypher5Parser.COMMA, 0); }
		public NormalFormContext normalForm() {
			return getRuleContext(NormalFormContext.class,0);
		}
		public NormalizeFunctionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_normalizeFunction; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterNormalizeFunction(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitNormalizeFunction(this);
		}
	}

	public final NormalizeFunctionContext normalizeFunction() throws RecognitionException {
		NormalizeFunctionContext _localctx = new NormalizeFunctionContext(_ctx, getState());
		enterRule(_localctx, 234, RULE_normalizeFunction);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1795);
			match(NORMALIZE);
			setState(1796);
			match(LPAREN);
			setState(1797);
			expression();
			setState(1800);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==COMMA) {
				{
				setState(1798);
				match(COMMA);
				setState(1799);
				normalForm();
				}
			}

			setState(1802);
			match(RPAREN);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class TrimFunctionContext extends AstRuleCtx {
		public ExpressionContext trimCharacterString;
		public ExpressionContext trimSource;
		public TerminalNode TRIM() { return getToken(Cypher5Parser.TRIM, 0); }
		public TerminalNode LPAREN() { return getToken(Cypher5Parser.LPAREN, 0); }
		public TerminalNode RPAREN() { return getToken(Cypher5Parser.RPAREN, 0); }
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public TerminalNode FROM() { return getToken(Cypher5Parser.FROM, 0); }
		public TerminalNode BOTH() { return getToken(Cypher5Parser.BOTH, 0); }
		public TerminalNode LEADING() { return getToken(Cypher5Parser.LEADING, 0); }
		public TerminalNode TRAILING() { return getToken(Cypher5Parser.TRAILING, 0); }
		public TrimFunctionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_trimFunction; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterTrimFunction(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitTrimFunction(this);
		}
	}

	public final TrimFunctionContext trimFunction() throws RecognitionException {
		TrimFunctionContext _localctx = new TrimFunctionContext(_ctx, getState());
		enterRule(_localctx, 236, RULE_trimFunction);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1804);
			match(TRIM);
			setState(1805);
			match(LPAREN);
			setState(1813);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,176,_ctx) ) {
			case 1:
				{
				setState(1807);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,174,_ctx) ) {
				case 1:
					{
					setState(1806);
					_la = _input.LA(1);
					if ( !(_la==BOTH || _la==LEADING || _la==TRAILING) ) {
					_errHandler.recoverInline(this);
					}
					else {
						if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
						_errHandler.reportMatch(this);
						consume();
					}
					}
					break;
				}
				setState(1810);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,175,_ctx) ) {
				case 1:
					{
					setState(1809);
					((TrimFunctionContext)_localctx).trimCharacterString = expression();
					}
					break;
				}
				setState(1812);
				match(FROM);
				}
				break;
			}
			setState(1815);
			((TrimFunctionContext)_localctx).trimSource = expression();
			setState(1816);
			match(RPAREN);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class PatternExpressionContext extends AstRuleCtx {
		public PathPatternNonEmptyContext pathPatternNonEmpty() {
			return getRuleContext(PathPatternNonEmptyContext.class,0);
		}
		public PatternExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_patternExpression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterPatternExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitPatternExpression(this);
		}
	}

	public final PatternExpressionContext patternExpression() throws RecognitionException {
		PatternExpressionContext _localctx = new PatternExpressionContext(_ctx, getState());
		enterRule(_localctx, 238, RULE_patternExpression);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1818);
			pathPatternNonEmpty();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ShortestPathExpressionContext extends AstRuleCtx {
		public ShortestPathPatternContext shortestPathPattern() {
			return getRuleContext(ShortestPathPatternContext.class,0);
		}
		public ShortestPathExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_shortestPathExpression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterShortestPathExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitShortestPathExpression(this);
		}
	}

	public final ShortestPathExpressionContext shortestPathExpression() throws RecognitionException {
		ShortestPathExpressionContext _localctx = new ShortestPathExpressionContext(_ctx, getState());
		enterRule(_localctx, 240, RULE_shortestPathExpression);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1820);
			shortestPathPattern();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ParenthesizedExpressionContext extends AstRuleCtx {
		public TerminalNode LPAREN() { return getToken(Cypher5Parser.LPAREN, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public TerminalNode RPAREN() { return getToken(Cypher5Parser.RPAREN, 0); }
		public ParenthesizedExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_parenthesizedExpression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterParenthesizedExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitParenthesizedExpression(this);
		}
	}

	public final ParenthesizedExpressionContext parenthesizedExpression() throws RecognitionException {
		ParenthesizedExpressionContext _localctx = new ParenthesizedExpressionContext(_ctx, getState());
		enterRule(_localctx, 242, RULE_parenthesizedExpression);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1822);
			match(LPAREN);
			setState(1823);
			expression();
			setState(1824);
			match(RPAREN);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class MapProjectionContext extends AstRuleCtx {
		public VariableContext variable() {
			return getRuleContext(VariableContext.class,0);
		}
		public TerminalNode LCURLY() { return getToken(Cypher5Parser.LCURLY, 0); }
		public TerminalNode RCURLY() { return getToken(Cypher5Parser.RCURLY, 0); }
		public List<MapProjectionElementContext> mapProjectionElement() {
			return getRuleContexts(MapProjectionElementContext.class);
		}
		public MapProjectionElementContext mapProjectionElement(int i) {
			return getRuleContext(MapProjectionElementContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(Cypher5Parser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(Cypher5Parser.COMMA, i);
		}
		public MapProjectionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_mapProjection; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterMapProjection(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitMapProjection(this);
		}
	}

	public final MapProjectionContext mapProjection() throws RecognitionException {
		MapProjectionContext _localctx = new MapProjectionContext(_ctx, getState());
		enterRule(_localctx, 244, RULE_mapProjection);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1826);
			variable();
			setState(1827);
			match(LCURLY);
			setState(1836);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & -985163492230144L) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & -1170935971839279105L) != 0) || ((((_la - 128)) & ~0x3f) == 0 && ((1L << (_la - 128)) & -129253703681L) != 0) || ((((_la - 192)) & ~0x3f) == 0 && ((1L << (_la - 192)) & -36169535635589505L) != 0) || ((((_la - 256)) & ~0x3f) == 0 && ((1L << (_la - 256)) & 18014398509449215L) != 0)) {
				{
				setState(1828);
				mapProjectionElement();
				setState(1833);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(1829);
					match(COMMA);
					setState(1830);
					mapProjectionElement();
					}
					}
					setState(1835);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
			}

			setState(1838);
			match(RCURLY);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class MapProjectionElementContext extends AstRuleCtx {
		public PropertyKeyNameContext propertyKeyName() {
			return getRuleContext(PropertyKeyNameContext.class,0);
		}
		public TerminalNode COLON() { return getToken(Cypher5Parser.COLON, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public PropertyContext property() {
			return getRuleContext(PropertyContext.class,0);
		}
		public VariableContext variable() {
			return getRuleContext(VariableContext.class,0);
		}
		public TerminalNode DOT() { return getToken(Cypher5Parser.DOT, 0); }
		public TerminalNode TIMES() { return getToken(Cypher5Parser.TIMES, 0); }
		public MapProjectionElementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_mapProjectionElement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterMapProjectionElement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitMapProjectionElement(this);
		}
	}

	public final MapProjectionElementContext mapProjectionElement() throws RecognitionException {
		MapProjectionElementContext _localctx = new MapProjectionElementContext(_ctx, getState());
		enterRule(_localctx, 246, RULE_mapProjectionElement);
		try {
			setState(1848);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,179,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1840);
				propertyKeyName();
				setState(1841);
				match(COLON);
				setState(1842);
				expression();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1844);
				property();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(1845);
				variable();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(1846);
				match(DOT);
				setState(1847);
				match(TIMES);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class CountStarContext extends AstRuleCtx {
		public TerminalNode COUNT() { return getToken(Cypher5Parser.COUNT, 0); }
		public TerminalNode LPAREN() { return getToken(Cypher5Parser.LPAREN, 0); }
		public TerminalNode TIMES() { return getToken(Cypher5Parser.TIMES, 0); }
		public TerminalNode RPAREN() { return getToken(Cypher5Parser.RPAREN, 0); }
		public CountStarContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_countStar; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterCountStar(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitCountStar(this);
		}
	}

	public final CountStarContext countStar() throws RecognitionException {
		CountStarContext _localctx = new CountStarContext(_ctx, getState());
		enterRule(_localctx, 248, RULE_countStar);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1850);
			match(COUNT);
			setState(1851);
			match(LPAREN);
			setState(1852);
			match(TIMES);
			setState(1853);
			match(RPAREN);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ExistsExpressionContext extends AstRuleCtx {
		public TerminalNode EXISTS() { return getToken(Cypher5Parser.EXISTS, 0); }
		public TerminalNode LCURLY() { return getToken(Cypher5Parser.LCURLY, 0); }
		public TerminalNode RCURLY() { return getToken(Cypher5Parser.RCURLY, 0); }
		public RegularQueryContext regularQuery() {
			return getRuleContext(RegularQueryContext.class,0);
		}
		public PatternListContext patternList() {
			return getRuleContext(PatternListContext.class,0);
		}
		public MatchModeContext matchMode() {
			return getRuleContext(MatchModeContext.class,0);
		}
		public WhereClauseContext whereClause() {
			return getRuleContext(WhereClauseContext.class,0);
		}
		public ExistsExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_existsExpression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterExistsExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitExistsExpression(this);
		}
	}

	public final ExistsExpressionContext existsExpression() throws RecognitionException {
		ExistsExpressionContext _localctx = new ExistsExpressionContext(_ctx, getState());
		enterRule(_localctx, 250, RULE_existsExpression);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1855);
			match(EXISTS);
			setState(1856);
			match(LCURLY);
			setState(1865);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,182,_ctx) ) {
			case 1:
				{
				setState(1857);
				regularQuery();
				}
				break;
			case 2:
				{
				setState(1859);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,180,_ctx) ) {
				case 1:
					{
					setState(1858);
					matchMode();
					}
					break;
				}
				setState(1861);
				patternList();
				setState(1863);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==WHERE) {
					{
					setState(1862);
					whereClause();
					}
				}

				}
				break;
			}
			setState(1867);
			match(RCURLY);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class CountExpressionContext extends AstRuleCtx {
		public TerminalNode COUNT() { return getToken(Cypher5Parser.COUNT, 0); }
		public TerminalNode LCURLY() { return getToken(Cypher5Parser.LCURLY, 0); }
		public TerminalNode RCURLY() { return getToken(Cypher5Parser.RCURLY, 0); }
		public RegularQueryContext regularQuery() {
			return getRuleContext(RegularQueryContext.class,0);
		}
		public PatternListContext patternList() {
			return getRuleContext(PatternListContext.class,0);
		}
		public MatchModeContext matchMode() {
			return getRuleContext(MatchModeContext.class,0);
		}
		public WhereClauseContext whereClause() {
			return getRuleContext(WhereClauseContext.class,0);
		}
		public CountExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_countExpression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterCountExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitCountExpression(this);
		}
	}

	public final CountExpressionContext countExpression() throws RecognitionException {
		CountExpressionContext _localctx = new CountExpressionContext(_ctx, getState());
		enterRule(_localctx, 252, RULE_countExpression);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1869);
			match(COUNT);
			setState(1870);
			match(LCURLY);
			setState(1879);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,185,_ctx) ) {
			case 1:
				{
				setState(1871);
				regularQuery();
				}
				break;
			case 2:
				{
				setState(1873);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,183,_ctx) ) {
				case 1:
					{
					setState(1872);
					matchMode();
					}
					break;
				}
				setState(1875);
				patternList();
				setState(1877);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==WHERE) {
					{
					setState(1876);
					whereClause();
					}
				}

				}
				break;
			}
			setState(1881);
			match(RCURLY);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class CollectExpressionContext extends AstRuleCtx {
		public TerminalNode COLLECT() { return getToken(Cypher5Parser.COLLECT, 0); }
		public TerminalNode LCURLY() { return getToken(Cypher5Parser.LCURLY, 0); }
		public RegularQueryContext regularQuery() {
			return getRuleContext(RegularQueryContext.class,0);
		}
		public TerminalNode RCURLY() { return getToken(Cypher5Parser.RCURLY, 0); }
		public CollectExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_collectExpression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterCollectExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitCollectExpression(this);
		}
	}

	public final CollectExpressionContext collectExpression() throws RecognitionException {
		CollectExpressionContext _localctx = new CollectExpressionContext(_ctx, getState());
		enterRule(_localctx, 254, RULE_collectExpression);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1883);
			match(COLLECT);
			setState(1884);
			match(LCURLY);
			setState(1885);
			regularQuery();
			setState(1886);
			match(RCURLY);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class NumberLiteralContext extends AstRuleCtx {
		public TerminalNode DECIMAL_DOUBLE() { return getToken(Cypher5Parser.DECIMAL_DOUBLE, 0); }
		public TerminalNode UNSIGNED_DECIMAL_INTEGER() { return getToken(Cypher5Parser.UNSIGNED_DECIMAL_INTEGER, 0); }
		public TerminalNode UNSIGNED_HEX_INTEGER() { return getToken(Cypher5Parser.UNSIGNED_HEX_INTEGER, 0); }
		public TerminalNode UNSIGNED_OCTAL_INTEGER() { return getToken(Cypher5Parser.UNSIGNED_OCTAL_INTEGER, 0); }
		public TerminalNode MINUS() { return getToken(Cypher5Parser.MINUS, 0); }
		public NumberLiteralContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_numberLiteral; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterNumberLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitNumberLiteral(this);
		}
	}

	public final NumberLiteralContext numberLiteral() throws RecognitionException {
		NumberLiteralContext _localctx = new NumberLiteralContext(_ctx, getState());
		enterRule(_localctx, 256, RULE_numberLiteral);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1889);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==MINUS) {
				{
				setState(1888);
				match(MINUS);
				}
			}

			setState(1891);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 240L) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class SignedIntegerLiteralContext extends AstRuleCtx {
		public TerminalNode UNSIGNED_DECIMAL_INTEGER() { return getToken(Cypher5Parser.UNSIGNED_DECIMAL_INTEGER, 0); }
		public TerminalNode MINUS() { return getToken(Cypher5Parser.MINUS, 0); }
		public SignedIntegerLiteralContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_signedIntegerLiteral; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterSignedIntegerLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitSignedIntegerLiteral(this);
		}
	}

	public final SignedIntegerLiteralContext signedIntegerLiteral() throws RecognitionException {
		SignedIntegerLiteralContext _localctx = new SignedIntegerLiteralContext(_ctx, getState());
		enterRule(_localctx, 258, RULE_signedIntegerLiteral);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1894);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==MINUS) {
				{
				setState(1893);
				match(MINUS);
				}
			}

			setState(1896);
			match(UNSIGNED_DECIMAL_INTEGER);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ListLiteralContext extends AstRuleCtx {
		public TerminalNode LBRACKET() { return getToken(Cypher5Parser.LBRACKET, 0); }
		public TerminalNode RBRACKET() { return getToken(Cypher5Parser.RBRACKET, 0); }
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(Cypher5Parser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(Cypher5Parser.COMMA, i);
		}
		public ListLiteralContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_listLiteral; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterListLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitListLiteral(this);
		}
	}

	public final ListLiteralContext listLiteral() throws RecognitionException {
		ListLiteralContext _localctx = new ListLiteralContext(_ctx, getState());
		enterRule(_localctx, 260, RULE_listLiteral);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1898);
			match(LBRACKET);
			setState(1907);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & -985163492229136L) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & -1170935971839737857L) != 0) || ((((_la - 128)) & ~0x3f) == 0 && ((1L << (_la - 128)) & -120528764929L) != 0) || ((((_la - 192)) & ~0x3f) == 0 && ((1L << (_la - 192)) & -36169535635589377L) != 0) || ((((_la - 256)) & ~0x3f) == 0 && ((1L << (_la - 256)) & 18014398509449215L) != 0)) {
				{
				setState(1899);
				expression();
				setState(1904);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(1900);
					match(COMMA);
					setState(1901);
					expression();
					}
					}
					setState(1906);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
			}

			setState(1909);
			match(RBRACKET);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class PropertyKeyNameContext extends AstRuleCtx {
		public SymbolicNameStringContext symbolicNameString() {
			return getRuleContext(SymbolicNameStringContext.class,0);
		}
		public PropertyKeyNameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_propertyKeyName; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterPropertyKeyName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitPropertyKeyName(this);
		}
	}

	public final PropertyKeyNameContext propertyKeyName() throws RecognitionException {
		PropertyKeyNameContext _localctx = new PropertyKeyNameContext(_ctx, getState());
		enterRule(_localctx, 262, RULE_propertyKeyName);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1911);
			symbolicNameString();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ParameterContext extends AstRuleCtx {
		public String paramType;
		public TerminalNode DOLLAR() { return getToken(Cypher5Parser.DOLLAR, 0); }
		public ParameterNameContext parameterName() {
			return getRuleContext(ParameterNameContext.class,0);
		}
		public ParameterContext(ParserRuleContext parent, int invokingState) { super(parent, invokingState); }
		public ParameterContext(ParserRuleContext parent, int invokingState, String paramType) {
			super(parent, invokingState);
			this.paramType = paramType;
		}
		@Override public int getRuleIndex() { return RULE_parameter; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterParameter(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitParameter(this);
		}
	}

	public final ParameterContext parameter(String paramType) throws RecognitionException {
		ParameterContext _localctx = new ParameterContext(_ctx, getState(), paramType);
		enterRule(_localctx, 264, RULE_parameter);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1913);
			match(DOLLAR);
			setState(1914);
			parameterName(paramType);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ParameterNameContext extends AstRuleCtx {
		public String paramType;
		public SymbolicNameStringContext symbolicNameString() {
			return getRuleContext(SymbolicNameStringContext.class,0);
		}
		public TerminalNode UNSIGNED_DECIMAL_INTEGER() { return getToken(Cypher5Parser.UNSIGNED_DECIMAL_INTEGER, 0); }
		public ParameterNameContext(ParserRuleContext parent, int invokingState) { super(parent, invokingState); }
		public ParameterNameContext(ParserRuleContext parent, int invokingState, String paramType) {
			super(parent, invokingState);
			this.paramType = paramType;
		}
		@Override public int getRuleIndex() { return RULE_parameterName; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterParameterName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitParameterName(this);
		}
	}

	public final ParameterNameContext parameterName(String paramType) throws RecognitionException {
		ParameterNameContext _localctx = new ParameterNameContext(_ctx, getState(), paramType);
		enterRule(_localctx, 266, RULE_parameterName);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1918);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case ESCAPED_SYMBOLIC_NAME:
			case ACCESS:
			case ACTIVE:
			case ADMIN:
			case ADMINISTRATOR:
			case ALIAS:
			case ALIASES:
			case ALL_SHORTEST_PATHS:
			case ALL:
			case ALTER:
			case AND:
			case ANY:
			case ARRAY:
			case AS:
			case ASC:
			case ASCENDING:
			case ASSERT:
			case ASSIGN:
			case AT:
			case AUTH:
			case BINDINGS:
			case BOOL:
			case BOOLEAN:
			case BOOSTED:
			case BOTH:
			case BREAK:
			case BRIEF:
			case BTREE:
			case BUILT:
			case BY:
			case CALL:
			case CASCADE:
			case CASE:
			case CHANGE:
			case CIDR:
			case COLLECT:
			case COMMAND:
			case COMMANDS:
			case COMMIT:
			case COMPOSITE:
			case CONCURRENT:
			case CONSTRAINT:
			case CONSTRAINTS:
			case CONTAINS:
			case COPY:
			case CONTINUE:
			case COUNT:
			case CREATE:
			case CSV:
			case CURRENT:
			case DATA:
			case DATABASE:
			case DATABASES:
			case DATE:
			case DATETIME:
			case DBMS:
			case DEALLOCATE:
			case DEFAULT:
			case DEFINED:
			case DELETE:
			case DENY:
			case DESC:
			case DESCENDING:
			case DESTROY:
			case DETACH:
			case DIFFERENT:
			case DISTINCT:
			case DRIVER:
			case DROP:
			case DRYRUN:
			case DUMP:
			case DURATION:
			case EACH:
			case EDGE:
			case ENABLE:
			case ELEMENT:
			case ELEMENTS:
			case ELSE:
			case ENCRYPTED:
			case END:
			case ENDS:
			case EXECUTABLE:
			case EXECUTE:
			case EXIST:
			case EXISTENCE:
			case EXISTS:
			case ERROR:
			case FAIL:
			case FALSE:
			case FIELDTERMINATOR:
			case FINISH:
			case FLOAT:
			case FOR:
			case FOREACH:
			case FROM:
			case FULLTEXT:
			case FUNCTION:
			case FUNCTIONS:
			case GRANT:
			case GRAPH:
			case GRAPHS:
			case GROUP:
			case GROUPS:
			case HEADERS:
			case HOME:
			case ID:
			case IF:
			case IMPERSONATE:
			case IMMUTABLE:
			case IN:
			case INDEX:
			case INDEXES:
			case INF:
			case INFINITY:
			case INSERT:
			case INT:
			case INTEGER:
			case IS:
			case JOIN:
			case KEY:
			case LABEL:
			case LABELS:
			case LEADING:
			case LIMITROWS:
			case LIST:
			case LOAD:
			case LOCAL:
			case LOOKUP:
			case MANAGEMENT:
			case MAP:
			case MATCH:
			case MERGE:
			case NAME:
			case NAMES:
			case NAN:
			case NFC:
			case NFD:
			case NFKC:
			case NFKD:
			case NEW:
			case NODE:
			case NODETACH:
			case NODES:
			case NONE:
			case NORMALIZE:
			case NORMALIZED:
			case NOT:
			case NOTHING:
			case NOWAIT:
			case NULL:
			case OF:
			case OFFSET:
			case ON:
			case ONLY:
			case OPTIONAL:
			case OPTIONS:
			case OPTION:
			case OR:
			case ORDER:
			case OUTPUT:
			case PASSWORD:
			case PASSWORDS:
			case PATH:
			case PATHS:
			case PERIODIC:
			case PLAINTEXT:
			case POINT:
			case POPULATED:
			case PRIMARY:
			case PRIMARIES:
			case PRIVILEGE:
			case PRIVILEGES:
			case PROCEDURE:
			case PROCEDURES:
			case PROPERTIES:
			case PROPERTY:
			case PROVIDER:
			case PROVIDERS:
			case RANGE:
			case READ:
			case REALLOCATE:
			case REDUCE:
			case RENAME:
			case REL:
			case RELATIONSHIP:
			case RELATIONSHIPS:
			case REMOVE:
			case REPEATABLE:
			case REPLACE:
			case REPORT:
			case REQUIRE:
			case REQUIRED:
			case RESTRICT:
			case RETURN:
			case REVOKE:
			case ROLE:
			case ROLES:
			case ROW:
			case ROWS:
			case SCAN:
			case SEC:
			case SECOND:
			case SECONDARY:
			case SECONDARIES:
			case SECONDS:
			case SEEK:
			case SERVER:
			case SERVERS:
			case SET:
			case SETTING:
			case SETTINGS:
			case SHORTEST_PATH:
			case SHORTEST:
			case SHOW:
			case SIGNED:
			case SINGLE:
			case SKIPROWS:
			case START:
			case STARTS:
			case STATUS:
			case STOP:
			case STRING:
			case SUPPORTED:
			case SUSPENDED:
			case TARGET:
			case TERMINATE:
			case TEXT:
			case THEN:
			case TIME:
			case TIMESTAMP:
			case TIMEZONE:
			case TO:
			case TOPOLOGY:
			case TRAILING:
			case TRANSACTION:
			case TRANSACTIONS:
			case TRAVERSE:
			case TRIM:
			case TRUE:
			case TYPE:
			case TYPED:
			case TYPES:
			case UNION:
			case UNIQUE:
			case UNIQUENESS:
			case UNWIND:
			case URL:
			case USE:
			case USER:
			case USERS:
			case USING:
			case VALUE:
			case VARCHAR:
			case VECTOR:
			case VERBOSE:
			case VERTEX:
			case WAIT:
			case WHEN:
			case WHERE:
			case WITH:
			case WITHOUT:
			case WRITE:
			case XOR:
			case YIELD:
			case ZONE:
			case ZONED:
			case IDENTIFIER:
				{
				setState(1916);
				symbolicNameString();
				}
				break;
			case UNSIGNED_DECIMAL_INTEGER:
				{
				setState(1917);
				match(UNSIGNED_DECIMAL_INTEGER);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class FunctionInvocationContext extends AstRuleCtx {
		public FunctionNameContext functionName() {
			return getRuleContext(FunctionNameContext.class,0);
		}
		public TerminalNode LPAREN() { return getToken(Cypher5Parser.LPAREN, 0); }
		public TerminalNode RPAREN() { return getToken(Cypher5Parser.RPAREN, 0); }
		public List<FunctionArgumentContext> functionArgument() {
			return getRuleContexts(FunctionArgumentContext.class);
		}
		public FunctionArgumentContext functionArgument(int i) {
			return getRuleContext(FunctionArgumentContext.class,i);
		}
		public TerminalNode DISTINCT() { return getToken(Cypher5Parser.DISTINCT, 0); }
		public TerminalNode ALL() { return getToken(Cypher5Parser.ALL, 0); }
		public List<TerminalNode> COMMA() { return getTokens(Cypher5Parser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(Cypher5Parser.COMMA, i);
		}
		public FunctionInvocationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_functionInvocation; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterFunctionInvocation(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitFunctionInvocation(this);
		}
	}

	public final FunctionInvocationContext functionInvocation() throws RecognitionException {
		FunctionInvocationContext _localctx = new FunctionInvocationContext(_ctx, getState());
		enterRule(_localctx, 268, RULE_functionInvocation);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1920);
			functionName();
			setState(1921);
			match(LPAREN);
			setState(1923);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,191,_ctx) ) {
			case 1:
				{
				setState(1922);
				_la = _input.LA(1);
				if ( !(_la==ALL || _la==DISTINCT) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			}
			setState(1933);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & -985163492229136L) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & -1170935971839737857L) != 0) || ((((_la - 128)) & ~0x3f) == 0 && ((1L << (_la - 128)) & -120528764929L) != 0) || ((((_la - 192)) & ~0x3f) == 0 && ((1L << (_la - 192)) & -36169535635589377L) != 0) || ((((_la - 256)) & ~0x3f) == 0 && ((1L << (_la - 256)) & 18014398509449215L) != 0)) {
				{
				setState(1925);
				functionArgument();
				setState(1930);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(1926);
					match(COMMA);
					setState(1927);
					functionArgument();
					}
					}
					setState(1932);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
			}

			setState(1935);
			match(RPAREN);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class FunctionArgumentContext extends AstRuleCtx {
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public FunctionArgumentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_functionArgument; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterFunctionArgument(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitFunctionArgument(this);
		}
	}

	public final FunctionArgumentContext functionArgument() throws RecognitionException {
		FunctionArgumentContext _localctx = new FunctionArgumentContext(_ctx, getState());
		enterRule(_localctx, 270, RULE_functionArgument);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1937);
			expression();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class FunctionNameContext extends AstRuleCtx {
		public NamespaceContext namespace() {
			return getRuleContext(NamespaceContext.class,0);
		}
		public SymbolicNameStringContext symbolicNameString() {
			return getRuleContext(SymbolicNameStringContext.class,0);
		}
		public FunctionNameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_functionName; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterFunctionName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitFunctionName(this);
		}
	}

	public final FunctionNameContext functionName() throws RecognitionException {
		FunctionNameContext _localctx = new FunctionNameContext(_ctx, getState());
		enterRule(_localctx, 272, RULE_functionName);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1939);
			namespace();
			setState(1940);
			symbolicNameString();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class NamespaceContext extends AstRuleCtx {
		public List<SymbolicNameStringContext> symbolicNameString() {
			return getRuleContexts(SymbolicNameStringContext.class);
		}
		public SymbolicNameStringContext symbolicNameString(int i) {
			return getRuleContext(SymbolicNameStringContext.class,i);
		}
		public List<TerminalNode> DOT() { return getTokens(Cypher5Parser.DOT); }
		public TerminalNode DOT(int i) {
			return getToken(Cypher5Parser.DOT, i);
		}
		public NamespaceContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_namespace; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterNamespace(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitNamespace(this);
		}
	}

	public final NamespaceContext namespace() throws RecognitionException {
		NamespaceContext _localctx = new NamespaceContext(_ctx, getState());
		enterRule(_localctx, 274, RULE_namespace);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(1947);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,194,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(1942);
					symbolicNameString();
					setState(1943);
					match(DOT);
					}
					} 
				}
				setState(1949);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,194,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class VariableContext extends AstRuleCtx {
		public SymbolicVariableNameStringContext symbolicVariableNameString() {
			return getRuleContext(SymbolicVariableNameStringContext.class,0);
		}
		public VariableContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_variable; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterVariable(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitVariable(this);
		}
	}

	public final VariableContext variable() throws RecognitionException {
		VariableContext _localctx = new VariableContext(_ctx, getState());
		enterRule(_localctx, 276, RULE_variable);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1950);
			symbolicVariableNameString();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class NonEmptyNameListContext extends AstRuleCtx {
		public List<SymbolicNameStringContext> symbolicNameString() {
			return getRuleContexts(SymbolicNameStringContext.class);
		}
		public SymbolicNameStringContext symbolicNameString(int i) {
			return getRuleContext(SymbolicNameStringContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(Cypher5Parser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(Cypher5Parser.COMMA, i);
		}
		public NonEmptyNameListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_nonEmptyNameList; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterNonEmptyNameList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitNonEmptyNameList(this);
		}
	}

	public final NonEmptyNameListContext nonEmptyNameList() throws RecognitionException {
		NonEmptyNameListContext _localctx = new NonEmptyNameListContext(_ctx, getState());
		enterRule(_localctx, 278, RULE_nonEmptyNameList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1952);
			symbolicNameString();
			setState(1957);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(1953);
				match(COMMA);
				setState(1954);
				symbolicNameString();
				}
				}
				setState(1959);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class TypeContext extends AstRuleCtx {
		public List<TypePartContext> typePart() {
			return getRuleContexts(TypePartContext.class);
		}
		public TypePartContext typePart(int i) {
			return getRuleContext(TypePartContext.class,i);
		}
		public List<TerminalNode> BAR() { return getTokens(Cypher5Parser.BAR); }
		public TerminalNode BAR(int i) {
			return getToken(Cypher5Parser.BAR, i);
		}
		public TypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_type; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitType(this);
		}
	}

	public final TypeContext type() throws RecognitionException {
		TypeContext _localctx = new TypeContext(_ctx, getState());
		enterRule(_localctx, 280, RULE_type);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(1960);
			typePart();
			setState(1965);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,196,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(1961);
					match(BAR);
					setState(1962);
					typePart();
					}
					} 
				}
				setState(1967);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,196,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class TypePartContext extends AstRuleCtx {
		public TypeNameContext typeName() {
			return getRuleContext(TypeNameContext.class,0);
		}
		public TypeNullabilityContext typeNullability() {
			return getRuleContext(TypeNullabilityContext.class,0);
		}
		public List<TypeListSuffixContext> typeListSuffix() {
			return getRuleContexts(TypeListSuffixContext.class);
		}
		public TypeListSuffixContext typeListSuffix(int i) {
			return getRuleContext(TypeListSuffixContext.class,i);
		}
		public TypePartContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_typePart; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterTypePart(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitTypePart(this);
		}
	}

	public final TypePartContext typePart() throws RecognitionException {
		TypePartContext _localctx = new TypePartContext(_ctx, getState());
		enterRule(_localctx, 282, RULE_typePart);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1968);
			typeName();
			setState(1970);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==EXCLAMATION_MARK || _la==NOT) {
				{
				setState(1969);
				typeNullability();
				}
			}

			setState(1975);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==ARRAY || _la==LIST) {
				{
				{
				setState(1972);
				typeListSuffix();
				}
				}
				setState(1977);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class TypeNameContext extends AstRuleCtx {
		public TerminalNode NOTHING() { return getToken(Cypher5Parser.NOTHING, 0); }
		public TerminalNode NULL() { return getToken(Cypher5Parser.NULL, 0); }
		public TerminalNode BOOL() { return getToken(Cypher5Parser.BOOL, 0); }
		public TerminalNode BOOLEAN() { return getToken(Cypher5Parser.BOOLEAN, 0); }
		public TerminalNode VARCHAR() { return getToken(Cypher5Parser.VARCHAR, 0); }
		public TerminalNode STRING() { return getToken(Cypher5Parser.STRING, 0); }
		public TerminalNode INT() { return getToken(Cypher5Parser.INT, 0); }
		public TerminalNode INTEGER() { return getToken(Cypher5Parser.INTEGER, 0); }
		public TerminalNode SIGNED() { return getToken(Cypher5Parser.SIGNED, 0); }
		public TerminalNode FLOAT() { return getToken(Cypher5Parser.FLOAT, 0); }
		public TerminalNode DATE() { return getToken(Cypher5Parser.DATE, 0); }
		public TerminalNode LOCAL() { return getToken(Cypher5Parser.LOCAL, 0); }
		public List<TerminalNode> TIME() { return getTokens(Cypher5Parser.TIME); }
		public TerminalNode TIME(int i) {
			return getToken(Cypher5Parser.TIME, i);
		}
		public TerminalNode DATETIME() { return getToken(Cypher5Parser.DATETIME, 0); }
		public TerminalNode ZONED() { return getToken(Cypher5Parser.ZONED, 0); }
		public TerminalNode WITHOUT() { return getToken(Cypher5Parser.WITHOUT, 0); }
		public TerminalNode WITH() { return getToken(Cypher5Parser.WITH, 0); }
		public TerminalNode TIMEZONE() { return getToken(Cypher5Parser.TIMEZONE, 0); }
		public TerminalNode ZONE() { return getToken(Cypher5Parser.ZONE, 0); }
		public TerminalNode TIMESTAMP() { return getToken(Cypher5Parser.TIMESTAMP, 0); }
		public TerminalNode DURATION() { return getToken(Cypher5Parser.DURATION, 0); }
		public TerminalNode POINT() { return getToken(Cypher5Parser.POINT, 0); }
		public TerminalNode NODE() { return getToken(Cypher5Parser.NODE, 0); }
		public TerminalNode VERTEX() { return getToken(Cypher5Parser.VERTEX, 0); }
		public TerminalNode RELATIONSHIP() { return getToken(Cypher5Parser.RELATIONSHIP, 0); }
		public TerminalNode EDGE() { return getToken(Cypher5Parser.EDGE, 0); }
		public TerminalNode MAP() { return getToken(Cypher5Parser.MAP, 0); }
		public TerminalNode LT() { return getToken(Cypher5Parser.LT, 0); }
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public TerminalNode GT() { return getToken(Cypher5Parser.GT, 0); }
		public TerminalNode LIST() { return getToken(Cypher5Parser.LIST, 0); }
		public TerminalNode ARRAY() { return getToken(Cypher5Parser.ARRAY, 0); }
		public TerminalNode PATH() { return getToken(Cypher5Parser.PATH, 0); }
		public TerminalNode PATHS() { return getToken(Cypher5Parser.PATHS, 0); }
		public TerminalNode PROPERTY() { return getToken(Cypher5Parser.PROPERTY, 0); }
		public TerminalNode VALUE() { return getToken(Cypher5Parser.VALUE, 0); }
		public TerminalNode ANY() { return getToken(Cypher5Parser.ANY, 0); }
		public TypeNameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_typeName; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterTypeName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitTypeName(this);
		}
	}

	public final TypeNameContext typeName() throws RecognitionException {
		TypeNameContext _localctx = new TypeNameContext(_ctx, getState());
		enterRule(_localctx, 284, RULE_typeName);
		int _la;
		try {
			setState(2043);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case NOTHING:
				enterOuterAlt(_localctx, 1);
				{
				setState(1978);
				match(NOTHING);
				}
				break;
			case NULL:
				enterOuterAlt(_localctx, 2);
				{
				setState(1979);
				match(NULL);
				}
				break;
			case BOOL:
				enterOuterAlt(_localctx, 3);
				{
				setState(1980);
				match(BOOL);
				}
				break;
			case BOOLEAN:
				enterOuterAlt(_localctx, 4);
				{
				setState(1981);
				match(BOOLEAN);
				}
				break;
			case VARCHAR:
				enterOuterAlt(_localctx, 5);
				{
				setState(1982);
				match(VARCHAR);
				}
				break;
			case STRING:
				enterOuterAlt(_localctx, 6);
				{
				setState(1983);
				match(STRING);
				}
				break;
			case INT:
				enterOuterAlt(_localctx, 7);
				{
				setState(1984);
				match(INT);
				}
				break;
			case INTEGER:
			case SIGNED:
				enterOuterAlt(_localctx, 8);
				{
				setState(1986);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==SIGNED) {
					{
					setState(1985);
					match(SIGNED);
					}
				}

				setState(1988);
				match(INTEGER);
				}
				break;
			case FLOAT:
				enterOuterAlt(_localctx, 9);
				{
				setState(1989);
				match(FLOAT);
				}
				break;
			case DATE:
				enterOuterAlt(_localctx, 10);
				{
				setState(1990);
				match(DATE);
				}
				break;
			case LOCAL:
				enterOuterAlt(_localctx, 11);
				{
				setState(1991);
				match(LOCAL);
				setState(1992);
				_la = _input.LA(1);
				if ( !(_la==DATETIME || _la==TIME) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case ZONED:
				enterOuterAlt(_localctx, 12);
				{
				setState(1993);
				match(ZONED);
				setState(1994);
				_la = _input.LA(1);
				if ( !(_la==DATETIME || _la==TIME) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case TIME:
				enterOuterAlt(_localctx, 13);
				{
				setState(1995);
				match(TIME);
				setState(1996);
				_la = _input.LA(1);
				if ( !(_la==WITH || _la==WITHOUT) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(2000);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case TIMEZONE:
					{
					setState(1997);
					match(TIMEZONE);
					}
					break;
				case TIME:
					{
					setState(1998);
					match(TIME);
					setState(1999);
					match(ZONE);
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				}
				break;
			case TIMESTAMP:
				enterOuterAlt(_localctx, 14);
				{
				setState(2002);
				match(TIMESTAMP);
				setState(2003);
				_la = _input.LA(1);
				if ( !(_la==WITH || _la==WITHOUT) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(2007);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case TIMEZONE:
					{
					setState(2004);
					match(TIMEZONE);
					}
					break;
				case TIME:
					{
					setState(2005);
					match(TIME);
					setState(2006);
					match(ZONE);
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				}
				break;
			case DURATION:
				enterOuterAlt(_localctx, 15);
				{
				setState(2009);
				match(DURATION);
				}
				break;
			case POINT:
				enterOuterAlt(_localctx, 16);
				{
				setState(2010);
				match(POINT);
				}
				break;
			case NODE:
				enterOuterAlt(_localctx, 17);
				{
				setState(2011);
				match(NODE);
				}
				break;
			case VERTEX:
				enterOuterAlt(_localctx, 18);
				{
				setState(2012);
				match(VERTEX);
				}
				break;
			case RELATIONSHIP:
				enterOuterAlt(_localctx, 19);
				{
				setState(2013);
				match(RELATIONSHIP);
				}
				break;
			case EDGE:
				enterOuterAlt(_localctx, 20);
				{
				setState(2014);
				match(EDGE);
				}
				break;
			case MAP:
				enterOuterAlt(_localctx, 21);
				{
				setState(2015);
				match(MAP);
				}
				break;
			case ARRAY:
			case LIST:
				enterOuterAlt(_localctx, 22);
				{
				setState(2016);
				_la = _input.LA(1);
				if ( !(_la==ARRAY || _la==LIST) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(2017);
				match(LT);
				setState(2018);
				type();
				setState(2019);
				match(GT);
				}
				break;
			case PATH:
				enterOuterAlt(_localctx, 23);
				{
				setState(2021);
				match(PATH);
				}
				break;
			case PATHS:
				enterOuterAlt(_localctx, 24);
				{
				setState(2022);
				match(PATHS);
				}
				break;
			case PROPERTY:
				enterOuterAlt(_localctx, 25);
				{
				setState(2023);
				match(PROPERTY);
				setState(2024);
				match(VALUE);
				}
				break;
			case ANY:
				enterOuterAlt(_localctx, 26);
				{
				setState(2025);
				match(ANY);
				setState(2041);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,203,_ctx) ) {
				case 1:
					{
					setState(2026);
					match(NODE);
					}
					break;
				case 2:
					{
					setState(2027);
					match(VERTEX);
					}
					break;
				case 3:
					{
					setState(2028);
					match(RELATIONSHIP);
					}
					break;
				case 4:
					{
					setState(2029);
					match(EDGE);
					}
					break;
				case 5:
					{
					setState(2030);
					match(MAP);
					}
					break;
				case 6:
					{
					setState(2031);
					match(PROPERTY);
					setState(2032);
					match(VALUE);
					}
					break;
				case 7:
					{
					setState(2034);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==VALUE) {
						{
						setState(2033);
						match(VALUE);
						}
					}

					setState(2036);
					match(LT);
					setState(2037);
					type();
					setState(2038);
					match(GT);
					}
					break;
				case 8:
					{
					setState(2040);
					match(VALUE);
					}
					break;
				}
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class TypeNullabilityContext extends AstRuleCtx {
		public TerminalNode NOT() { return getToken(Cypher5Parser.NOT, 0); }
		public TerminalNode NULL() { return getToken(Cypher5Parser.NULL, 0); }
		public TerminalNode EXCLAMATION_MARK() { return getToken(Cypher5Parser.EXCLAMATION_MARK, 0); }
		public TypeNullabilityContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_typeNullability; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterTypeNullability(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitTypeNullability(this);
		}
	}

	public final TypeNullabilityContext typeNullability() throws RecognitionException {
		TypeNullabilityContext _localctx = new TypeNullabilityContext(_ctx, getState());
		enterRule(_localctx, 286, RULE_typeNullability);
		try {
			setState(2048);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case NOT:
				enterOuterAlt(_localctx, 1);
				{
				setState(2045);
				match(NOT);
				setState(2046);
				match(NULL);
				}
				break;
			case EXCLAMATION_MARK:
				enterOuterAlt(_localctx, 2);
				{
				setState(2047);
				match(EXCLAMATION_MARK);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class TypeListSuffixContext extends AstRuleCtx {
		public TerminalNode LIST() { return getToken(Cypher5Parser.LIST, 0); }
		public TerminalNode ARRAY() { return getToken(Cypher5Parser.ARRAY, 0); }
		public TypeNullabilityContext typeNullability() {
			return getRuleContext(TypeNullabilityContext.class,0);
		}
		public TypeListSuffixContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_typeListSuffix; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterTypeListSuffix(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitTypeListSuffix(this);
		}
	}

	public final TypeListSuffixContext typeListSuffix() throws RecognitionException {
		TypeListSuffixContext _localctx = new TypeListSuffixContext(_ctx, getState());
		enterRule(_localctx, 288, RULE_typeListSuffix);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2050);
			_la = _input.LA(1);
			if ( !(_la==ARRAY || _la==LIST) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			setState(2052);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==EXCLAMATION_MARK || _la==NOT) {
				{
				setState(2051);
				typeNullability();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class CommandContext extends AstRuleCtx {
		public CreateCommandContext createCommand() {
			return getRuleContext(CreateCommandContext.class,0);
		}
		public DropCommandContext dropCommand() {
			return getRuleContext(DropCommandContext.class,0);
		}
		public ShowCommandContext showCommand() {
			return getRuleContext(ShowCommandContext.class,0);
		}
		public TerminateCommandContext terminateCommand() {
			return getRuleContext(TerminateCommandContext.class,0);
		}
		public AlterCommandContext alterCommand() {
			return getRuleContext(AlterCommandContext.class,0);
		}
		public RenameCommandContext renameCommand() {
			return getRuleContext(RenameCommandContext.class,0);
		}
		public DenyCommandContext denyCommand() {
			return getRuleContext(DenyCommandContext.class,0);
		}
		public RevokeCommandContext revokeCommand() {
			return getRuleContext(RevokeCommandContext.class,0);
		}
		public GrantCommandContext grantCommand() {
			return getRuleContext(GrantCommandContext.class,0);
		}
		public StartDatabaseContext startDatabase() {
			return getRuleContext(StartDatabaseContext.class,0);
		}
		public StopDatabaseContext stopDatabase() {
			return getRuleContext(StopDatabaseContext.class,0);
		}
		public EnableServerCommandContext enableServerCommand() {
			return getRuleContext(EnableServerCommandContext.class,0);
		}
		public AllocationCommandContext allocationCommand() {
			return getRuleContext(AllocationCommandContext.class,0);
		}
		public UseClauseContext useClause() {
			return getRuleContext(UseClauseContext.class,0);
		}
		public CommandContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_command; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterCommand(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitCommand(this);
		}
	}

	public final CommandContext command() throws RecognitionException {
		CommandContext _localctx = new CommandContext(_ctx, getState());
		enterRule(_localctx, 290, RULE_command);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2055);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==USE) {
				{
				setState(2054);
				useClause();
				}
			}

			setState(2070);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case CREATE:
				{
				setState(2057);
				createCommand();
				}
				break;
			case DROP:
				{
				setState(2058);
				dropCommand();
				}
				break;
			case SHOW:
				{
				setState(2059);
				showCommand();
				}
				break;
			case TERMINATE:
				{
				setState(2060);
				terminateCommand();
				}
				break;
			case ALTER:
				{
				setState(2061);
				alterCommand();
				}
				break;
			case RENAME:
				{
				setState(2062);
				renameCommand();
				}
				break;
			case DENY:
				{
				setState(2063);
				denyCommand();
				}
				break;
			case REVOKE:
				{
				setState(2064);
				revokeCommand();
				}
				break;
			case GRANT:
				{
				setState(2065);
				grantCommand();
				}
				break;
			case START:
				{
				setState(2066);
				startDatabase();
				}
				break;
			case STOP:
				{
				setState(2067);
				stopDatabase();
				}
				break;
			case ENABLE:
				{
				setState(2068);
				enableServerCommand();
				}
				break;
			case DEALLOCATE:
			case REALLOCATE:
				{
				setState(2069);
				allocationCommand();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class CreateCommandContext extends AstRuleCtx {
		public TerminalNode CREATE() { return getToken(Cypher5Parser.CREATE, 0); }
		public CreateCommandContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_createCommand; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterCreateCommand(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitCreateCommand(this);
		}
	}

	public final CreateCommandContext createCommand() throws RecognitionException {
		CreateCommandContext _localctx = new CreateCommandContext(_ctx, getState());
		enterRule(_localctx, 292, RULE_createCommand);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2072);
			match(CREATE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class DropCommandContext extends AstRuleCtx {
		public TerminalNode DROP() { return getToken(Cypher5Parser.DROP, 0); }
		public DropCommandContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_dropCommand; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterDropCommand(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitDropCommand(this);
		}
	}

	public final DropCommandContext dropCommand() throws RecognitionException {
		DropCommandContext _localctx = new DropCommandContext(_ctx, getState());
		enterRule(_localctx, 294, RULE_dropCommand);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2074);
			match(DROP);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ShowCommandContext extends AstRuleCtx {
		public TerminalNode SHOW() { return getToken(Cypher5Parser.SHOW, 0); }
		public ShowCommandContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_showCommand; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterShowCommand(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitShowCommand(this);
		}
	}

	public final ShowCommandContext showCommand() throws RecognitionException {
		ShowCommandContext _localctx = new ShowCommandContext(_ctx, getState());
		enterRule(_localctx, 296, RULE_showCommand);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2076);
			match(SHOW);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ShowCommandYieldContext extends AstRuleCtx {
		public TerminalNode YIELD() { return getToken(Cypher5Parser.YIELD, 0); }
		public ShowCommandYieldContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_showCommandYield; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterShowCommandYield(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitShowCommandYield(this);
		}
	}

	public final ShowCommandYieldContext showCommandYield() throws RecognitionException {
		ShowCommandYieldContext _localctx = new ShowCommandYieldContext(_ctx, getState());
		enterRule(_localctx, 298, RULE_showCommandYield);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2078);
			match(YIELD);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class YieldItemContext extends AstRuleCtx {
		public VariableContext variable() {
			return getRuleContext(VariableContext.class,0);
		}
		public YieldItemContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_yieldItem; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterYieldItem(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitYieldItem(this);
		}
	}

	public final YieldItemContext yieldItem() throws RecognitionException {
		YieldItemContext _localctx = new YieldItemContext(_ctx, getState());
		enterRule(_localctx, 300, RULE_yieldItem);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2080);
			variable();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class YieldSkipContext extends AstRuleCtx {
		public TerminalNode OFFSET() { return getToken(Cypher5Parser.OFFSET, 0); }
		public SignedIntegerLiteralContext signedIntegerLiteral() {
			return getRuleContext(SignedIntegerLiteralContext.class,0);
		}
		public YieldSkipContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_yieldSkip; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterYieldSkip(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitYieldSkip(this);
		}
	}

	public final YieldSkipContext yieldSkip() throws RecognitionException {
		YieldSkipContext _localctx = new YieldSkipContext(_ctx, getState());
		enterRule(_localctx, 302, RULE_yieldSkip);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2082);
			match(OFFSET);
			setState(2083);
			signedIntegerLiteral();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class YieldLimitContext extends AstRuleCtx {
		public TerminalNode LIMITROWS() { return getToken(Cypher5Parser.LIMITROWS, 0); }
		public SignedIntegerLiteralContext signedIntegerLiteral() {
			return getRuleContext(SignedIntegerLiteralContext.class,0);
		}
		public YieldLimitContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_yieldLimit; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterYieldLimit(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitYieldLimit(this);
		}
	}

	public final YieldLimitContext yieldLimit() throws RecognitionException {
		YieldLimitContext _localctx = new YieldLimitContext(_ctx, getState());
		enterRule(_localctx, 304, RULE_yieldLimit);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2085);
			match(LIMITROWS);
			setState(2086);
			signedIntegerLiteral();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class YieldClauseContext extends AstRuleCtx {
		public TerminalNode YIELD() { return getToken(Cypher5Parser.YIELD, 0); }
		public YieldClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_yieldClause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterYieldClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitYieldClause(this);
		}
	}

	public final YieldClauseContext yieldClause() throws RecognitionException {
		YieldClauseContext _localctx = new YieldClauseContext(_ctx, getState());
		enterRule(_localctx, 306, RULE_yieldClause);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2088);
			match(YIELD);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class CommandOptionsContext extends AstRuleCtx {
		public TerminalNode OPTIONS() { return getToken(Cypher5Parser.OPTIONS, 0); }
		public MapOrParameterContext mapOrParameter() {
			return getRuleContext(MapOrParameterContext.class,0);
		}
		public CommandOptionsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_commandOptions; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterCommandOptions(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitCommandOptions(this);
		}
	}

	public final CommandOptionsContext commandOptions() throws RecognitionException {
		CommandOptionsContext _localctx = new CommandOptionsContext(_ctx, getState());
		enterRule(_localctx, 308, RULE_commandOptions);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2090);
			match(OPTIONS);
			setState(2091);
			mapOrParameter();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class TerminateCommandContext extends AstRuleCtx {
		public TerminalNode TERMINATE() { return getToken(Cypher5Parser.TERMINATE, 0); }
		public TerminateTransactionsContext terminateTransactions() {
			return getRuleContext(TerminateTransactionsContext.class,0);
		}
		public TerminateCommandContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_terminateCommand; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterTerminateCommand(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitTerminateCommand(this);
		}
	}

	public final TerminateCommandContext terminateCommand() throws RecognitionException {
		TerminateCommandContext _localctx = new TerminateCommandContext(_ctx, getState());
		enterRule(_localctx, 310, RULE_terminateCommand);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2093);
			match(TERMINATE);
			setState(2094);
			terminateTransactions();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ComposableCommandClausesContext extends AstRuleCtx {
		public TerminateCommandContext terminateCommand() {
			return getRuleContext(TerminateCommandContext.class,0);
		}
		public ComposableShowCommandClausesContext composableShowCommandClauses() {
			return getRuleContext(ComposableShowCommandClausesContext.class,0);
		}
		public ComposableCommandClausesContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_composableCommandClauses; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterComposableCommandClauses(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitComposableCommandClauses(this);
		}
	}

	public final ComposableCommandClausesContext composableCommandClauses() throws RecognitionException {
		ComposableCommandClausesContext _localctx = new ComposableCommandClausesContext(_ctx, getState());
		enterRule(_localctx, 312, RULE_composableCommandClauses);
		try {
			setState(2098);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case TERMINATE:
				enterOuterAlt(_localctx, 1);
				{
				setState(2096);
				terminateCommand();
				}
				break;
			case SHOW:
				enterOuterAlt(_localctx, 2);
				{
				setState(2097);
				composableShowCommandClauses();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ComposableShowCommandClausesContext extends AstRuleCtx {
		public TerminalNode SHOW() { return getToken(Cypher5Parser.SHOW, 0); }
		public ComposableShowCommandClausesContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_composableShowCommandClauses; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterComposableShowCommandClauses(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitComposableShowCommandClauses(this);
		}
	}

	public final ComposableShowCommandClausesContext composableShowCommandClauses() throws RecognitionException {
		ComposableShowCommandClausesContext _localctx = new ComposableShowCommandClausesContext(_ctx, getState());
		enterRule(_localctx, 314, RULE_composableShowCommandClauses);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2100);
			match(SHOW);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ShowBriefAndYieldContext extends AstRuleCtx {
		public TerminalNode BRIEF() { return getToken(Cypher5Parser.BRIEF, 0); }
		public TerminalNode VERBOSE() { return getToken(Cypher5Parser.VERBOSE, 0); }
		public TerminalNode YIELD() { return getToken(Cypher5Parser.YIELD, 0); }
		public TerminalNode WHERE() { return getToken(Cypher5Parser.WHERE, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public ShowBriefAndYieldContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_showBriefAndYield; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterShowBriefAndYield(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitShowBriefAndYield(this);
		}
	}

	public final ShowBriefAndYieldContext showBriefAndYield() throws RecognitionException {
		ShowBriefAndYieldContext _localctx = new ShowBriefAndYieldContext(_ctx, getState());
		enterRule(_localctx, 316, RULE_showBriefAndYield);
		try {
			setState(2107);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case BRIEF:
				enterOuterAlt(_localctx, 1);
				{
				setState(2102);
				match(BRIEF);
				}
				break;
			case VERBOSE:
				enterOuterAlt(_localctx, 2);
				{
				setState(2103);
				match(VERBOSE);
				}
				break;
			case YIELD:
				enterOuterAlt(_localctx, 3);
				{
				setState(2104);
				match(YIELD);
				}
				break;
			case WHERE:
				enterOuterAlt(_localctx, 4);
				{
				setState(2105);
				match(WHERE);
				setState(2106);
				expression();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ShowIndexCommandContext extends AstRuleCtx {
		public IndexTokenContext indexToken() {
			return getRuleContext(IndexTokenContext.class,0);
		}
		public ShowIndexCommandContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_showIndexCommand; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterShowIndexCommand(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitShowIndexCommand(this);
		}
	}

	public final ShowIndexCommandContext showIndexCommand() throws RecognitionException {
		ShowIndexCommandContext _localctx = new ShowIndexCommandContext(_ctx, getState());
		enterRule(_localctx, 318, RULE_showIndexCommand);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2109);
			indexToken();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ShowIndexesAllowBriefContext extends AstRuleCtx {
		public IndexTokenContext indexToken() {
			return getRuleContext(IndexTokenContext.class,0);
		}
		public ShowIndexesAllowBriefContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_showIndexesAllowBrief; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterShowIndexesAllowBrief(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitShowIndexesAllowBrief(this);
		}
	}

	public final ShowIndexesAllowBriefContext showIndexesAllowBrief() throws RecognitionException {
		ShowIndexesAllowBriefContext _localctx = new ShowIndexesAllowBriefContext(_ctx, getState());
		enterRule(_localctx, 320, RULE_showIndexesAllowBrief);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2111);
			indexToken();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ShowIndexesNoBriefContext extends AstRuleCtx {
		public IndexTokenContext indexToken() {
			return getRuleContext(IndexTokenContext.class,0);
		}
		public ShowIndexesNoBriefContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_showIndexesNoBrief; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterShowIndexesNoBrief(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitShowIndexesNoBrief(this);
		}
	}

	public final ShowIndexesNoBriefContext showIndexesNoBrief() throws RecognitionException {
		ShowIndexesNoBriefContext _localctx = new ShowIndexesNoBriefContext(_ctx, getState());
		enterRule(_localctx, 322, RULE_showIndexesNoBrief);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2113);
			indexToken();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ShowConstraintCommandContext extends AstRuleCtx {
		public ConstraintTokenContext constraintToken() {
			return getRuleContext(ConstraintTokenContext.class,0);
		}
		public ShowConstraintCommandContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_showConstraintCommand; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterShowConstraintCommand(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitShowConstraintCommand(this);
		}
	}

	public final ShowConstraintCommandContext showConstraintCommand() throws RecognitionException {
		ShowConstraintCommandContext _localctx = new ShowConstraintCommandContext(_ctx, getState());
		enterRule(_localctx, 324, RULE_showConstraintCommand);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2115);
			constraintToken();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ConstraintAllowYieldTypeContext extends AstRuleCtx {
		public TerminalNode UNIQUENESS() { return getToken(Cypher5Parser.UNIQUENESS, 0); }
		public ConstraintAllowYieldTypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_constraintAllowYieldType; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterConstraintAllowYieldType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitConstraintAllowYieldType(this);
		}
	}

	public final ConstraintAllowYieldTypeContext constraintAllowYieldType() throws RecognitionException {
		ConstraintAllowYieldTypeContext _localctx = new ConstraintAllowYieldTypeContext(_ctx, getState());
		enterRule(_localctx, 326, RULE_constraintAllowYieldType);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2117);
			match(UNIQUENESS);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ConstraintExistTypeContext extends AstRuleCtx {
		public TerminalNode EXISTENCE() { return getToken(Cypher5Parser.EXISTENCE, 0); }
		public ConstraintExistTypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_constraintExistType; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterConstraintExistType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitConstraintExistType(this);
		}
	}

	public final ConstraintExistTypeContext constraintExistType() throws RecognitionException {
		ConstraintExistTypeContext _localctx = new ConstraintExistTypeContext(_ctx, getState());
		enterRule(_localctx, 328, RULE_constraintExistType);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2119);
			match(EXISTENCE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ConstraintBriefAndYieldTypeContext extends AstRuleCtx {
		public TerminalNode ALL() { return getToken(Cypher5Parser.ALL, 0); }
		public ConstraintBriefAndYieldTypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_constraintBriefAndYieldType; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterConstraintBriefAndYieldType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitConstraintBriefAndYieldType(this);
		}
	}

	public final ConstraintBriefAndYieldTypeContext constraintBriefAndYieldType() throws RecognitionException {
		ConstraintBriefAndYieldTypeContext _localctx = new ConstraintBriefAndYieldTypeContext(_ctx, getState());
		enterRule(_localctx, 330, RULE_constraintBriefAndYieldType);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2121);
			match(ALL);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ShowConstraintsAllowBriefAndYieldContext extends AstRuleCtx {
		public ConstraintTokenContext constraintToken() {
			return getRuleContext(ConstraintTokenContext.class,0);
		}
		public ShowConstraintsAllowBriefAndYieldContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_showConstraintsAllowBriefAndYield; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterShowConstraintsAllowBriefAndYield(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitShowConstraintsAllowBriefAndYield(this);
		}
	}

	public final ShowConstraintsAllowBriefAndYieldContext showConstraintsAllowBriefAndYield() throws RecognitionException {
		ShowConstraintsAllowBriefAndYieldContext _localctx = new ShowConstraintsAllowBriefAndYieldContext(_ctx, getState());
		enterRule(_localctx, 332, RULE_showConstraintsAllowBriefAndYield);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2123);
			constraintToken();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ShowConstraintsAllowBriefContext extends AstRuleCtx {
		public ConstraintTokenContext constraintToken() {
			return getRuleContext(ConstraintTokenContext.class,0);
		}
		public ShowConstraintsAllowBriefContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_showConstraintsAllowBrief; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterShowConstraintsAllowBrief(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitShowConstraintsAllowBrief(this);
		}
	}

	public final ShowConstraintsAllowBriefContext showConstraintsAllowBrief() throws RecognitionException {
		ShowConstraintsAllowBriefContext _localctx = new ShowConstraintsAllowBriefContext(_ctx, getState());
		enterRule(_localctx, 334, RULE_showConstraintsAllowBrief);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2125);
			constraintToken();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ShowConstraintsAllowYieldContext extends AstRuleCtx {
		public ConstraintTokenContext constraintToken() {
			return getRuleContext(ConstraintTokenContext.class,0);
		}
		public ShowConstraintsAllowYieldContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_showConstraintsAllowYield; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterShowConstraintsAllowYield(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitShowConstraintsAllowYield(this);
		}
	}

	public final ShowConstraintsAllowYieldContext showConstraintsAllowYield() throws RecognitionException {
		ShowConstraintsAllowYieldContext _localctx = new ShowConstraintsAllowYieldContext(_ctx, getState());
		enterRule(_localctx, 336, RULE_showConstraintsAllowYield);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2127);
			constraintToken();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ShowProceduresContext extends AstRuleCtx {
		public ProcedureTokenContext procedureToken() {
			return getRuleContext(ProcedureTokenContext.class,0);
		}
		public ShowProceduresContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_showProcedures; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterShowProcedures(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitShowProcedures(this);
		}
	}

	public final ShowProceduresContext showProcedures() throws RecognitionException {
		ShowProceduresContext _localctx = new ShowProceduresContext(_ctx, getState());
		enterRule(_localctx, 338, RULE_showProcedures);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2129);
			procedureToken();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ShowFunctionsContext extends AstRuleCtx {
		public FunctionTokenContext functionToken() {
			return getRuleContext(FunctionTokenContext.class,0);
		}
		public ShowFunctionsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_showFunctions; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterShowFunctions(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitShowFunctions(this);
		}
	}

	public final ShowFunctionsContext showFunctions() throws RecognitionException {
		ShowFunctionsContext _localctx = new ShowFunctionsContext(_ctx, getState());
		enterRule(_localctx, 340, RULE_showFunctions);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2131);
			functionToken();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class FunctionTokenContext extends AstRuleCtx {
		public TerminalNode FUNCTION() { return getToken(Cypher5Parser.FUNCTION, 0); }
		public TerminalNode FUNCTIONS() { return getToken(Cypher5Parser.FUNCTIONS, 0); }
		public FunctionTokenContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_functionToken; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterFunctionToken(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitFunctionToken(this);
		}
	}

	public final FunctionTokenContext functionToken() throws RecognitionException {
		FunctionTokenContext _localctx = new FunctionTokenContext(_ctx, getState());
		enterRule(_localctx, 342, RULE_functionToken);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2133);
			_la = _input.LA(1);
			if ( !(_la==FUNCTION || _la==FUNCTIONS) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ExecutableByContext extends AstRuleCtx {
		public TerminalNode EXECUTABLE() { return getToken(Cypher5Parser.EXECUTABLE, 0); }
		public ExecutableByContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_executableBy; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterExecutableBy(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitExecutableBy(this);
		}
	}

	public final ExecutableByContext executableBy() throws RecognitionException {
		ExecutableByContext _localctx = new ExecutableByContext(_ctx, getState());
		enterRule(_localctx, 344, RULE_executableBy);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2135);
			match(EXECUTABLE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ShowFunctionsTypeContext extends AstRuleCtx {
		public TerminalNode ALL() { return getToken(Cypher5Parser.ALL, 0); }
		public ShowFunctionsTypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_showFunctionsType; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterShowFunctionsType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitShowFunctionsType(this);
		}
	}

	public final ShowFunctionsTypeContext showFunctionsType() throws RecognitionException {
		ShowFunctionsTypeContext _localctx = new ShowFunctionsTypeContext(_ctx, getState());
		enterRule(_localctx, 346, RULE_showFunctionsType);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2137);
			match(ALL);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ShowTransactionsContext extends AstRuleCtx {
		public TransactionTokenContext transactionToken() {
			return getRuleContext(TransactionTokenContext.class,0);
		}
		public ShowTransactionsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_showTransactions; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterShowTransactions(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitShowTransactions(this);
		}
	}

	public final ShowTransactionsContext showTransactions() throws RecognitionException {
		ShowTransactionsContext _localctx = new ShowTransactionsContext(_ctx, getState());
		enterRule(_localctx, 348, RULE_showTransactions);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2139);
			transactionToken();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class TerminateTransactionsContext extends AstRuleCtx {
		public TransactionTokenContext transactionToken() {
			return getRuleContext(TransactionTokenContext.class,0);
		}
		public StringsOrExpressionContext stringsOrExpression() {
			return getRuleContext(StringsOrExpressionContext.class,0);
		}
		public TerminateTransactionsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_terminateTransactions; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterTerminateTransactions(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitTerminateTransactions(this);
		}
	}

	public final TerminateTransactionsContext terminateTransactions() throws RecognitionException {
		TerminateTransactionsContext _localctx = new TerminateTransactionsContext(_ctx, getState());
		enterRule(_localctx, 350, RULE_terminateTransactions);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2141);
			transactionToken();
			setState(2143);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & -985163492229136L) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & -1170935971839737857L) != 0) || ((((_la - 128)) & ~0x3f) == 0 && ((1L << (_la - 128)) & -120528764929L) != 0) || ((((_la - 192)) & ~0x3f) == 0 && ((1L << (_la - 192)) & -36169535635589377L) != 0) || ((((_la - 256)) & ~0x3f) == 0 && ((1L << (_la - 256)) & 18014398509449215L) != 0)) {
				{
				setState(2142);
				stringsOrExpression();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ShowSettingsContext extends AstRuleCtx {
		public SettingTokenContext settingToken() {
			return getRuleContext(SettingTokenContext.class,0);
		}
		public ShowSettingsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_showSettings; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterShowSettings(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitShowSettings(this);
		}
	}

	public final ShowSettingsContext showSettings() throws RecognitionException {
		ShowSettingsContext _localctx = new ShowSettingsContext(_ctx, getState());
		enterRule(_localctx, 352, RULE_showSettings);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2145);
			settingToken();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class SettingTokenContext extends AstRuleCtx {
		public TerminalNode SETTING() { return getToken(Cypher5Parser.SETTING, 0); }
		public TerminalNode SETTINGS() { return getToken(Cypher5Parser.SETTINGS, 0); }
		public SettingTokenContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_settingToken; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterSettingToken(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitSettingToken(this);
		}
	}

	public final SettingTokenContext settingToken() throws RecognitionException {
		SettingTokenContext _localctx = new SettingTokenContext(_ctx, getState());
		enterRule(_localctx, 354, RULE_settingToken);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2147);
			_la = _input.LA(1);
			if ( !(_la==SETTING || _la==SETTINGS) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class NamesAndClausesContext extends AstRuleCtx {
		public StringsOrExpressionContext stringsOrExpression() {
			return getRuleContext(StringsOrExpressionContext.class,0);
		}
		public NamesAndClausesContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_namesAndClauses; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterNamesAndClauses(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitNamesAndClauses(this);
		}
	}

	public final NamesAndClausesContext namesAndClauses() throws RecognitionException {
		NamesAndClausesContext _localctx = new NamesAndClausesContext(_ctx, getState());
		enterRule(_localctx, 356, RULE_namesAndClauses);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2150);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & -985163492229136L) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & -1170935971839737857L) != 0) || ((((_la - 128)) & ~0x3f) == 0 && ((1L << (_la - 128)) & -120528764929L) != 0) || ((((_la - 192)) & ~0x3f) == 0 && ((1L << (_la - 192)) & -36169535635589377L) != 0) || ((((_la - 256)) & ~0x3f) == 0 && ((1L << (_la - 256)) & 18014398509449215L) != 0)) {
				{
				setState(2149);
				stringsOrExpression();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class StringsOrExpressionContext extends AstRuleCtx {
		public StringListContext stringList() {
			return getRuleContext(StringListContext.class,0);
		}
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public StringsOrExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_stringsOrExpression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterStringsOrExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitStringsOrExpression(this);
		}
	}

	public final StringsOrExpressionContext stringsOrExpression() throws RecognitionException {
		StringsOrExpressionContext _localctx = new StringsOrExpressionContext(_ctx, getState());
		enterRule(_localctx, 358, RULE_stringsOrExpression);
		try {
			setState(2154);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,213,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(2152);
				stringList();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(2153);
				expression();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class CommandNodePatternContext extends AstRuleCtx {
		public TerminalNode LPAREN() { return getToken(Cypher5Parser.LPAREN, 0); }
		public TerminalNode RPAREN() { return getToken(Cypher5Parser.RPAREN, 0); }
		public VariableContext variable() {
			return getRuleContext(VariableContext.class,0);
		}
		public CommandNodePatternContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_commandNodePattern; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterCommandNodePattern(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitCommandNodePattern(this);
		}
	}

	public final CommandNodePatternContext commandNodePattern() throws RecognitionException {
		CommandNodePatternContext _localctx = new CommandNodePatternContext(_ctx, getState());
		enterRule(_localctx, 360, RULE_commandNodePattern);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2156);
			match(LPAREN);
			setState(2158);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & -985163492230144L) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & -1170935971839803393L) != 0) || ((((_la - 128)) & ~0x3f) == 0 && ((1L << (_la - 128)) & -129253703681L) != 0) || ((((_la - 192)) & ~0x3f) == 0 && ((1L << (_la - 192)) & -36169535635589505L) != 0) || ((((_la - 256)) & ~0x3f) == 0 && ((1L << (_la - 256)) & 18014398509449215L) != 0)) {
				{
				setState(2157);
				variable();
				}
			}

			setState(2160);
			match(RPAREN);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class CommandRelPatternContext extends AstRuleCtx {
		public TerminalNode LPAREN() { return getToken(Cypher5Parser.LPAREN, 0); }
		public TerminalNode RPAREN() { return getToken(Cypher5Parser.RPAREN, 0); }
		public CommandRelPatternContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_commandRelPattern; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterCommandRelPattern(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitCommandRelPattern(this);
		}
	}

	public final CommandRelPatternContext commandRelPattern() throws RecognitionException {
		CommandRelPatternContext _localctx = new CommandRelPatternContext(_ctx, getState());
		enterRule(_localctx, 362, RULE_commandRelPattern);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2162);
			match(LPAREN);
			setState(2163);
			match(RPAREN);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class CreateConstraintContext extends AstRuleCtx {
		public TerminalNode CONSTRAINT() { return getToken(Cypher5Parser.CONSTRAINT, 0); }
		public CreateConstraintContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_createConstraint; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterCreateConstraint(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitCreateConstraint(this);
		}
	}

	public final CreateConstraintContext createConstraint() throws RecognitionException {
		CreateConstraintContext _localctx = new CreateConstraintContext(_ctx, getState());
		enterRule(_localctx, 364, RULE_createConstraint);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2165);
			match(CONSTRAINT);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ConstraintTypeContext extends AstRuleCtx {
		public ConstraintTypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_constraintType; }
	 
		public ConstraintTypeContext() { }
		public void copyFrom(ConstraintTypeContext ctx) {
			super.copyFrom(ctx);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class ConstraintTypedContext extends ConstraintTypeContext {
		public TerminalNode REQUIRE() { return getToken(Cypher5Parser.REQUIRE, 0); }
		public ConstraintTypedContext(ConstraintTypeContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterConstraintTyped(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitConstraintTyped(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class ConstraintExistsContext extends ConstraintTypeContext {
		public TerminalNode REQUIRE() { return getToken(Cypher5Parser.REQUIRE, 0); }
		public ConstraintExistsContext(ConstraintTypeContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterConstraintExists(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitConstraintExists(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class ConstraintKeyContext extends ConstraintTypeContext {
		public TerminalNode REQUIRE() { return getToken(Cypher5Parser.REQUIRE, 0); }
		public ConstraintKeyContext(ConstraintTypeContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterConstraintKey(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitConstraintKey(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class ConstraintIsNotNullContext extends ConstraintTypeContext {
		public TerminalNode REQUIRE() { return getToken(Cypher5Parser.REQUIRE, 0); }
		public ConstraintIsNotNullContext(ConstraintTypeContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterConstraintIsNotNull(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitConstraintIsNotNull(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class ConstraintIsUniqueContext extends ConstraintTypeContext {
		public TerminalNode REQUIRE() { return getToken(Cypher5Parser.REQUIRE, 0); }
		public ConstraintIsUniqueContext(ConstraintTypeContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterConstraintIsUnique(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitConstraintIsUnique(this);
		}
	}

	public final ConstraintTypeContext constraintType() throws RecognitionException {
		ConstraintTypeContext _localctx = new ConstraintTypeContext(_ctx, getState());
		enterRule(_localctx, 366, RULE_constraintType);
		try {
			setState(2172);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,215,_ctx) ) {
			case 1:
				_localctx = new ConstraintExistsContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(2167);
				match(REQUIRE);
				}
				break;
			case 2:
				_localctx = new ConstraintTypedContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(2168);
				match(REQUIRE);
				}
				break;
			case 3:
				_localctx = new ConstraintIsUniqueContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(2169);
				match(REQUIRE);
				}
				break;
			case 4:
				_localctx = new ConstraintKeyContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(2170);
				match(REQUIRE);
				}
				break;
			case 5:
				_localctx = new ConstraintIsNotNullContext(_localctx);
				enterOuterAlt(_localctx, 5);
				{
				setState(2171);
				match(REQUIRE);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class DropConstraintContext extends AstRuleCtx {
		public TerminalNode CONSTRAINT() { return getToken(Cypher5Parser.CONSTRAINT, 0); }
		public DropConstraintContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_dropConstraint; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterDropConstraint(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitDropConstraint(this);
		}
	}

	public final DropConstraintContext dropConstraint() throws RecognitionException {
		DropConstraintContext _localctx = new DropConstraintContext(_ctx, getState());
		enterRule(_localctx, 368, RULE_dropConstraint);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2174);
			match(CONSTRAINT);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class CreateIndexContext extends AstRuleCtx {
		public TerminalNode INDEX() { return getToken(Cypher5Parser.INDEX, 0); }
		public CreateIndexContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_createIndex; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterCreateIndex(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitCreateIndex(this);
		}
	}

	public final CreateIndexContext createIndex() throws RecognitionException {
		CreateIndexContext _localctx = new CreateIndexContext(_ctx, getState());
		enterRule(_localctx, 370, RULE_createIndex);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2176);
			match(INDEX);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class OldCreateIndexContext extends AstRuleCtx {
		public LabelTypeContext labelType() {
			return getRuleContext(LabelTypeContext.class,0);
		}
		public TerminalNode LPAREN() { return getToken(Cypher5Parser.LPAREN, 0); }
		public NonEmptyNameListContext nonEmptyNameList() {
			return getRuleContext(NonEmptyNameListContext.class,0);
		}
		public TerminalNode RPAREN() { return getToken(Cypher5Parser.RPAREN, 0); }
		public OldCreateIndexContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_oldCreateIndex; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterOldCreateIndex(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitOldCreateIndex(this);
		}
	}

	public final OldCreateIndexContext oldCreateIndex() throws RecognitionException {
		OldCreateIndexContext _localctx = new OldCreateIndexContext(_ctx, getState());
		enterRule(_localctx, 372, RULE_oldCreateIndex);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2178);
			labelType();
			setState(2179);
			match(LPAREN);
			setState(2180);
			nonEmptyNameList();
			setState(2181);
			match(RPAREN);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class CreateIndex_Context extends AstRuleCtx {
		public TerminalNode INDEX() { return getToken(Cypher5Parser.INDEX, 0); }
		public CreateIndex_Context(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_createIndex_; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterCreateIndex_(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitCreateIndex_(this);
		}
	}

	public final CreateIndex_Context createIndex_() throws RecognitionException {
		CreateIndex_Context _localctx = new CreateIndex_Context(_ctx, getState());
		enterRule(_localctx, 374, RULE_createIndex_);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2183);
			match(INDEX);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class CreateFulltextIndexContext extends AstRuleCtx {
		public TerminalNode INDEX() { return getToken(Cypher5Parser.INDEX, 0); }
		public CreateFulltextIndexContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_createFulltextIndex; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterCreateFulltextIndex(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitCreateFulltextIndex(this);
		}
	}

	public final CreateFulltextIndexContext createFulltextIndex() throws RecognitionException {
		CreateFulltextIndexContext _localctx = new CreateFulltextIndexContext(_ctx, getState());
		enterRule(_localctx, 376, RULE_createFulltextIndex);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2185);
			match(INDEX);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class FulltextNodePatternContext extends AstRuleCtx {
		public TerminalNode LPAREN() { return getToken(Cypher5Parser.LPAREN, 0); }
		public TerminalNode RPAREN() { return getToken(Cypher5Parser.RPAREN, 0); }
		public VariableContext variable() {
			return getRuleContext(VariableContext.class,0);
		}
		public FulltextNodePatternContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fulltextNodePattern; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterFulltextNodePattern(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitFulltextNodePattern(this);
		}
	}

	public final FulltextNodePatternContext fulltextNodePattern() throws RecognitionException {
		FulltextNodePatternContext _localctx = new FulltextNodePatternContext(_ctx, getState());
		enterRule(_localctx, 378, RULE_fulltextNodePattern);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2187);
			match(LPAREN);
			setState(2189);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & -985163492230144L) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & -1170935971839803393L) != 0) || ((((_la - 128)) & ~0x3f) == 0 && ((1L << (_la - 128)) & -129253703681L) != 0) || ((((_la - 192)) & ~0x3f) == 0 && ((1L << (_la - 192)) & -36169535635589505L) != 0) || ((((_la - 256)) & ~0x3f) == 0 && ((1L << (_la - 256)) & 18014398509449215L) != 0)) {
				{
				setState(2188);
				variable();
				}
			}

			setState(2191);
			match(RPAREN);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class FulltextRelPatternContext extends AstRuleCtx {
		public TerminalNode LPAREN() { return getToken(Cypher5Parser.LPAREN, 0); }
		public TerminalNode RPAREN() { return getToken(Cypher5Parser.RPAREN, 0); }
		public FulltextRelPatternContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fulltextRelPattern; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterFulltextRelPattern(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitFulltextRelPattern(this);
		}
	}

	public final FulltextRelPatternContext fulltextRelPattern() throws RecognitionException {
		FulltextRelPatternContext _localctx = new FulltextRelPatternContext(_ctx, getState());
		enterRule(_localctx, 380, RULE_fulltextRelPattern);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2193);
			match(LPAREN);
			setState(2194);
			match(RPAREN);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class CreateLookupIndexContext extends AstRuleCtx {
		public TerminalNode INDEX() { return getToken(Cypher5Parser.INDEX, 0); }
		public CreateLookupIndexContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_createLookupIndex; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterCreateLookupIndex(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitCreateLookupIndex(this);
		}
	}

	public final CreateLookupIndexContext createLookupIndex() throws RecognitionException {
		CreateLookupIndexContext _localctx = new CreateLookupIndexContext(_ctx, getState());
		enterRule(_localctx, 382, RULE_createLookupIndex);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2196);
			match(INDEX);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class LookupIndexNodePatternContext extends AstRuleCtx {
		public TerminalNode LPAREN() { return getToken(Cypher5Parser.LPAREN, 0); }
		public TerminalNode RPAREN() { return getToken(Cypher5Parser.RPAREN, 0); }
		public VariableContext variable() {
			return getRuleContext(VariableContext.class,0);
		}
		public LookupIndexNodePatternContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_lookupIndexNodePattern; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterLookupIndexNodePattern(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitLookupIndexNodePattern(this);
		}
	}

	public final LookupIndexNodePatternContext lookupIndexNodePattern() throws RecognitionException {
		LookupIndexNodePatternContext _localctx = new LookupIndexNodePatternContext(_ctx, getState());
		enterRule(_localctx, 384, RULE_lookupIndexNodePattern);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2198);
			match(LPAREN);
			setState(2200);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & -985163492230144L) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & -1170935971839803393L) != 0) || ((((_la - 128)) & ~0x3f) == 0 && ((1L << (_la - 128)) & -129253703681L) != 0) || ((((_la - 192)) & ~0x3f) == 0 && ((1L << (_la - 192)) & -36169535635589505L) != 0) || ((((_la - 256)) & ~0x3f) == 0 && ((1L << (_la - 256)) & 18014398509449215L) != 0)) {
				{
				setState(2199);
				variable();
				}
			}

			setState(2202);
			match(RPAREN);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class LookupIndexRelPatternContext extends AstRuleCtx {
		public TerminalNode LPAREN() { return getToken(Cypher5Parser.LPAREN, 0); }
		public TerminalNode RPAREN() { return getToken(Cypher5Parser.RPAREN, 0); }
		public LookupIndexRelPatternContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_lookupIndexRelPattern; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterLookupIndexRelPattern(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitLookupIndexRelPattern(this);
		}
	}

	public final LookupIndexRelPatternContext lookupIndexRelPattern() throws RecognitionException {
		LookupIndexRelPatternContext _localctx = new LookupIndexRelPatternContext(_ctx, getState());
		enterRule(_localctx, 386, RULE_lookupIndexRelPattern);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2204);
			match(LPAREN);
			setState(2205);
			match(RPAREN);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class DropIndexContext extends AstRuleCtx {
		public TerminalNode INDEX() { return getToken(Cypher5Parser.INDEX, 0); }
		public DropIndexContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_dropIndex; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterDropIndex(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitDropIndex(this);
		}
	}

	public final DropIndexContext dropIndex() throws RecognitionException {
		DropIndexContext _localctx = new DropIndexContext(_ctx, getState());
		enterRule(_localctx, 388, RULE_dropIndex);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2207);
			match(INDEX);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class PropertyListContext extends AstRuleCtx {
		public VariableContext variable() {
			return getRuleContext(VariableContext.class,0);
		}
		public PropertyContext property() {
			return getRuleContext(PropertyContext.class,0);
		}
		public PropertyListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_propertyList; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterPropertyList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitPropertyList(this);
		}
	}

	public final PropertyListContext propertyList() throws RecognitionException {
		PropertyListContext _localctx = new PropertyListContext(_ctx, getState());
		enterRule(_localctx, 390, RULE_propertyList);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2209);
			variable();
			setState(2210);
			property();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class EnclosedPropertyListContext extends AstRuleCtx {
		public List<VariableContext> variable() {
			return getRuleContexts(VariableContext.class);
		}
		public VariableContext variable(int i) {
			return getRuleContext(VariableContext.class,i);
		}
		public List<PropertyContext> property() {
			return getRuleContexts(PropertyContext.class);
		}
		public PropertyContext property(int i) {
			return getRuleContext(PropertyContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(Cypher5Parser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(Cypher5Parser.COMMA, i);
		}
		public EnclosedPropertyListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_enclosedPropertyList; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterEnclosedPropertyList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitEnclosedPropertyList(this);
		}
	}

	public final EnclosedPropertyListContext enclosedPropertyList() throws RecognitionException {
		EnclosedPropertyListContext _localctx = new EnclosedPropertyListContext(_ctx, getState());
		enterRule(_localctx, 392, RULE_enclosedPropertyList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2212);
			variable();
			setState(2213);
			property();
			setState(2220);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(2214);
				match(COMMA);
				setState(2215);
				variable();
				setState(2216);
				property();
				}
				}
				setState(2222);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class AlterCommandContext extends AstRuleCtx {
		public TerminalNode ALTER() { return getToken(Cypher5Parser.ALTER, 0); }
		public AlterCommandContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_alterCommand; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterAlterCommand(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitAlterCommand(this);
		}
	}

	public final AlterCommandContext alterCommand() throws RecognitionException {
		AlterCommandContext _localctx = new AlterCommandContext(_ctx, getState());
		enterRule(_localctx, 394, RULE_alterCommand);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2223);
			match(ALTER);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class RenameCommandContext extends AstRuleCtx {
		public TerminalNode RENAME() { return getToken(Cypher5Parser.RENAME, 0); }
		public RenameCommandContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_renameCommand; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterRenameCommand(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitRenameCommand(this);
		}
	}

	public final RenameCommandContext renameCommand() throws RecognitionException {
		RenameCommandContext _localctx = new RenameCommandContext(_ctx, getState());
		enterRule(_localctx, 396, RULE_renameCommand);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2225);
			match(RENAME);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class GrantCommandContext extends AstRuleCtx {
		public TerminalNode GRANT() { return getToken(Cypher5Parser.GRANT, 0); }
		public GrantCommandContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_grantCommand; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterGrantCommand(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitGrantCommand(this);
		}
	}

	public final GrantCommandContext grantCommand() throws RecognitionException {
		GrantCommandContext _localctx = new GrantCommandContext(_ctx, getState());
		enterRule(_localctx, 398, RULE_grantCommand);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2227);
			match(GRANT);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class DenyCommandContext extends AstRuleCtx {
		public TerminalNode DENY() { return getToken(Cypher5Parser.DENY, 0); }
		public DenyCommandContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_denyCommand; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterDenyCommand(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitDenyCommand(this);
		}
	}

	public final DenyCommandContext denyCommand() throws RecognitionException {
		DenyCommandContext _localctx = new DenyCommandContext(_ctx, getState());
		enterRule(_localctx, 400, RULE_denyCommand);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2229);
			match(DENY);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class RevokeCommandContext extends AstRuleCtx {
		public TerminalNode REVOKE() { return getToken(Cypher5Parser.REVOKE, 0); }
		public RevokeCommandContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_revokeCommand; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterRevokeCommand(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitRevokeCommand(this);
		}
	}

	public final RevokeCommandContext revokeCommand() throws RecognitionException {
		RevokeCommandContext _localctx = new RevokeCommandContext(_ctx, getState());
		enterRule(_localctx, 402, RULE_revokeCommand);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2231);
			match(REVOKE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class UserNamesContext extends AstRuleCtx {
		public SymbolicNameOrStringParameterListContext symbolicNameOrStringParameterList() {
			return getRuleContext(SymbolicNameOrStringParameterListContext.class,0);
		}
		public UserNamesContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_userNames; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterUserNames(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitUserNames(this);
		}
	}

	public final UserNamesContext userNames() throws RecognitionException {
		UserNamesContext _localctx = new UserNamesContext(_ctx, getState());
		enterRule(_localctx, 404, RULE_userNames);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2233);
			symbolicNameOrStringParameterList();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class RoleNamesContext extends AstRuleCtx {
		public SymbolicNameOrStringParameterListContext symbolicNameOrStringParameterList() {
			return getRuleContext(SymbolicNameOrStringParameterListContext.class,0);
		}
		public RoleNamesContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_roleNames; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterRoleNames(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitRoleNames(this);
		}
	}

	public final RoleNamesContext roleNames() throws RecognitionException {
		RoleNamesContext _localctx = new RoleNamesContext(_ctx, getState());
		enterRule(_localctx, 406, RULE_roleNames);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2235);
			symbolicNameOrStringParameterList();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class RoleTokenContext extends AstRuleCtx {
		public TerminalNode ROLES() { return getToken(Cypher5Parser.ROLES, 0); }
		public TerminalNode ROLE() { return getToken(Cypher5Parser.ROLE, 0); }
		public RoleTokenContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_roleToken; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterRoleToken(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitRoleToken(this);
		}
	}

	public final RoleTokenContext roleToken() throws RecognitionException {
		RoleTokenContext _localctx = new RoleTokenContext(_ctx, getState());
		enterRule(_localctx, 408, RULE_roleToken);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2237);
			_la = _input.LA(1);
			if ( !(_la==ROLE || _la==ROLES) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class EnableServerCommandContext extends AstRuleCtx {
		public TerminalNode ENABLE() { return getToken(Cypher5Parser.ENABLE, 0); }
		public TerminalNode SERVER() { return getToken(Cypher5Parser.SERVER, 0); }
		public StringOrParameterContext stringOrParameter() {
			return getRuleContext(StringOrParameterContext.class,0);
		}
		public EnableServerCommandContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_enableServerCommand; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterEnableServerCommand(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitEnableServerCommand(this);
		}
	}

	public final EnableServerCommandContext enableServerCommand() throws RecognitionException {
		EnableServerCommandContext _localctx = new EnableServerCommandContext(_ctx, getState());
		enterRule(_localctx, 410, RULE_enableServerCommand);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2239);
			match(ENABLE);
			setState(2240);
			match(SERVER);
			setState(2241);
			stringOrParameter();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class AlterServerContext extends AstRuleCtx {
		public TerminalNode SERVER() { return getToken(Cypher5Parser.SERVER, 0); }
		public StringOrParameterContext stringOrParameter() {
			return getRuleContext(StringOrParameterContext.class,0);
		}
		public AlterServerContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_alterServer; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterAlterServer(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitAlterServer(this);
		}
	}

	public final AlterServerContext alterServer() throws RecognitionException {
		AlterServerContext _localctx = new AlterServerContext(_ctx, getState());
		enterRule(_localctx, 412, RULE_alterServer);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2243);
			match(SERVER);
			setState(2244);
			stringOrParameter();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class RenameServerContext extends AstRuleCtx {
		public TerminalNode SERVER() { return getToken(Cypher5Parser.SERVER, 0); }
		public StringOrParameterContext stringOrParameter() {
			return getRuleContext(StringOrParameterContext.class,0);
		}
		public RenameServerContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_renameServer; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterRenameServer(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitRenameServer(this);
		}
	}

	public final RenameServerContext renameServer() throws RecognitionException {
		RenameServerContext _localctx = new RenameServerContext(_ctx, getState());
		enterRule(_localctx, 414, RULE_renameServer);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2246);
			match(SERVER);
			setState(2247);
			stringOrParameter();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class DropServerContext extends AstRuleCtx {
		public TerminalNode SERVER() { return getToken(Cypher5Parser.SERVER, 0); }
		public StringOrParameterContext stringOrParameter() {
			return getRuleContext(StringOrParameterContext.class,0);
		}
		public DropServerContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_dropServer; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterDropServer(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitDropServer(this);
		}
	}

	public final DropServerContext dropServer() throws RecognitionException {
		DropServerContext _localctx = new DropServerContext(_ctx, getState());
		enterRule(_localctx, 416, RULE_dropServer);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2249);
			match(SERVER);
			setState(2250);
			stringOrParameter();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ShowServersContext extends AstRuleCtx {
		public TerminalNode SERVER() { return getToken(Cypher5Parser.SERVER, 0); }
		public TerminalNode SERVERS() { return getToken(Cypher5Parser.SERVERS, 0); }
		public ShowServersContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_showServers; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterShowServers(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitShowServers(this);
		}
	}

	public final ShowServersContext showServers() throws RecognitionException {
		ShowServersContext _localctx = new ShowServersContext(_ctx, getState());
		enterRule(_localctx, 418, RULE_showServers);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2252);
			_la = _input.LA(1);
			if ( !(_la==SERVER || _la==SERVERS) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class AllocationCommandContext extends AstRuleCtx {
		public TerminalNode DEALLOCATE() { return getToken(Cypher5Parser.DEALLOCATE, 0); }
		public TerminalNode REALLOCATE() { return getToken(Cypher5Parser.REALLOCATE, 0); }
		public AllocationCommandContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_allocationCommand; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterAllocationCommand(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitAllocationCommand(this);
		}
	}

	public final AllocationCommandContext allocationCommand() throws RecognitionException {
		AllocationCommandContext _localctx = new AllocationCommandContext(_ctx, getState());
		enterRule(_localctx, 420, RULE_allocationCommand);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2254);
			_la = _input.LA(1);
			if ( !(_la==DEALLOCATE || _la==REALLOCATE) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class DeallocateDatabaseFromServersContext extends AstRuleCtx {
		public TerminalNode DEALLOCATE() { return getToken(Cypher5Parser.DEALLOCATE, 0); }
		public TerminalNode DATABASE() { return getToken(Cypher5Parser.DATABASE, 0); }
		public TerminalNode DATABASES() { return getToken(Cypher5Parser.DATABASES, 0); }
		public DeallocateDatabaseFromServersContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_deallocateDatabaseFromServers; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterDeallocateDatabaseFromServers(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitDeallocateDatabaseFromServers(this);
		}
	}

	public final DeallocateDatabaseFromServersContext deallocateDatabaseFromServers() throws RecognitionException {
		DeallocateDatabaseFromServersContext _localctx = new DeallocateDatabaseFromServersContext(_ctx, getState());
		enterRule(_localctx, 422, RULE_deallocateDatabaseFromServers);
		try {
			setState(2260);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,219,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(2256);
				match(DEALLOCATE);
				setState(2257);
				match(DATABASE);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(2258);
				match(DEALLOCATE);
				setState(2259);
				match(DATABASES);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ReallocateDatabasesContext extends AstRuleCtx {
		public TerminalNode REALLOCATE() { return getToken(Cypher5Parser.REALLOCATE, 0); }
		public TerminalNode DATABASE() { return getToken(Cypher5Parser.DATABASE, 0); }
		public TerminalNode DATABASES() { return getToken(Cypher5Parser.DATABASES, 0); }
		public ReallocateDatabasesContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_reallocateDatabases; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterReallocateDatabases(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitReallocateDatabases(this);
		}
	}

	public final ReallocateDatabasesContext reallocateDatabases() throws RecognitionException {
		ReallocateDatabasesContext _localctx = new ReallocateDatabasesContext(_ctx, getState());
		enterRule(_localctx, 424, RULE_reallocateDatabases);
		try {
			setState(2266);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,220,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(2262);
				match(REALLOCATE);
				setState(2263);
				match(DATABASE);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(2264);
				match(REALLOCATE);
				setState(2265);
				match(DATABASES);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class CreateRoleContext extends AstRuleCtx {
		public TerminalNode ROLE() { return getToken(Cypher5Parser.ROLE, 0); }
		public CommandNameExpressionContext commandNameExpression() {
			return getRuleContext(CommandNameExpressionContext.class,0);
		}
		public CreateRoleContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_createRole; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterCreateRole(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitCreateRole(this);
		}
	}

	public final CreateRoleContext createRole() throws RecognitionException {
		CreateRoleContext _localctx = new CreateRoleContext(_ctx, getState());
		enterRule(_localctx, 426, RULE_createRole);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2268);
			match(ROLE);
			setState(2269);
			commandNameExpression();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class DropRoleContext extends AstRuleCtx {
		public TerminalNode ROLE() { return getToken(Cypher5Parser.ROLE, 0); }
		public CommandNameExpressionContext commandNameExpression() {
			return getRuleContext(CommandNameExpressionContext.class,0);
		}
		public DropRoleContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_dropRole; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterDropRole(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitDropRole(this);
		}
	}

	public final DropRoleContext dropRole() throws RecognitionException {
		DropRoleContext _localctx = new DropRoleContext(_ctx, getState());
		enterRule(_localctx, 428, RULE_dropRole);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2271);
			match(ROLE);
			setState(2272);
			commandNameExpression();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class RenameRoleContext extends AstRuleCtx {
		public TerminalNode ROLE() { return getToken(Cypher5Parser.ROLE, 0); }
		public List<CommandNameExpressionContext> commandNameExpression() {
			return getRuleContexts(CommandNameExpressionContext.class);
		}
		public CommandNameExpressionContext commandNameExpression(int i) {
			return getRuleContext(CommandNameExpressionContext.class,i);
		}
		public TerminalNode TO() { return getToken(Cypher5Parser.TO, 0); }
		public RenameRoleContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_renameRole; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterRenameRole(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitRenameRole(this);
		}
	}

	public final RenameRoleContext renameRole() throws RecognitionException {
		RenameRoleContext _localctx = new RenameRoleContext(_ctx, getState());
		enterRule(_localctx, 430, RULE_renameRole);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2274);
			match(ROLE);
			setState(2275);
			commandNameExpression();
			setState(2276);
			match(TO);
			setState(2277);
			commandNameExpression();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ShowRolesContext extends AstRuleCtx {
		public RoleTokenContext roleToken() {
			return getRuleContext(RoleTokenContext.class,0);
		}
		public ShowRolesContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_showRoles; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterShowRoles(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitShowRoles(this);
		}
	}

	public final ShowRolesContext showRoles() throws RecognitionException {
		ShowRolesContext _localctx = new ShowRolesContext(_ctx, getState());
		enterRule(_localctx, 432, RULE_showRoles);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2279);
			roleToken();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class GrantRoleContext extends AstRuleCtx {
		public RoleNamesContext roleNames() {
			return getRuleContext(RoleNamesContext.class,0);
		}
		public TerminalNode TO() { return getToken(Cypher5Parser.TO, 0); }
		public UserNamesContext userNames() {
			return getRuleContext(UserNamesContext.class,0);
		}
		public GrantRoleContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_grantRole; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterGrantRole(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitGrantRole(this);
		}
	}

	public final GrantRoleContext grantRole() throws RecognitionException {
		GrantRoleContext _localctx = new GrantRoleContext(_ctx, getState());
		enterRule(_localctx, 434, RULE_grantRole);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2281);
			roleNames();
			setState(2282);
			match(TO);
			setState(2283);
			userNames();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class RevokeRoleContext extends AstRuleCtx {
		public RoleNamesContext roleNames() {
			return getRuleContext(RoleNamesContext.class,0);
		}
		public TerminalNode FROM() { return getToken(Cypher5Parser.FROM, 0); }
		public UserNamesContext userNames() {
			return getRuleContext(UserNamesContext.class,0);
		}
		public RevokeRoleContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_revokeRole; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterRevokeRole(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitRevokeRole(this);
		}
	}

	public final RevokeRoleContext revokeRole() throws RecognitionException {
		RevokeRoleContext _localctx = new RevokeRoleContext(_ctx, getState());
		enterRule(_localctx, 436, RULE_revokeRole);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2285);
			roleNames();
			setState(2286);
			match(FROM);
			setState(2287);
			userNames();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class CreateUserContext extends AstRuleCtx {
		public TerminalNode USER() { return getToken(Cypher5Parser.USER, 0); }
		public CommandNameExpressionContext commandNameExpression() {
			return getRuleContext(CommandNameExpressionContext.class,0);
		}
		public CreateUserContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_createUser; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterCreateUser(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitCreateUser(this);
		}
	}

	public final CreateUserContext createUser() throws RecognitionException {
		CreateUserContext _localctx = new CreateUserContext(_ctx, getState());
		enterRule(_localctx, 438, RULE_createUser);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2289);
			match(USER);
			setState(2290);
			commandNameExpression();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class DropUserContext extends AstRuleCtx {
		public TerminalNode USER() { return getToken(Cypher5Parser.USER, 0); }
		public CommandNameExpressionContext commandNameExpression() {
			return getRuleContext(CommandNameExpressionContext.class,0);
		}
		public DropUserContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_dropUser; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterDropUser(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitDropUser(this);
		}
	}

	public final DropUserContext dropUser() throws RecognitionException {
		DropUserContext _localctx = new DropUserContext(_ctx, getState());
		enterRule(_localctx, 440, RULE_dropUser);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2292);
			match(USER);
			setState(2293);
			commandNameExpression();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class RenameUserContext extends AstRuleCtx {
		public TerminalNode USER() { return getToken(Cypher5Parser.USER, 0); }
		public List<CommandNameExpressionContext> commandNameExpression() {
			return getRuleContexts(CommandNameExpressionContext.class);
		}
		public CommandNameExpressionContext commandNameExpression(int i) {
			return getRuleContext(CommandNameExpressionContext.class,i);
		}
		public TerminalNode TO() { return getToken(Cypher5Parser.TO, 0); }
		public RenameUserContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_renameUser; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterRenameUser(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitRenameUser(this);
		}
	}

	public final RenameUserContext renameUser() throws RecognitionException {
		RenameUserContext _localctx = new RenameUserContext(_ctx, getState());
		enterRule(_localctx, 442, RULE_renameUser);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2295);
			match(USER);
			setState(2296);
			commandNameExpression();
			setState(2297);
			match(TO);
			setState(2298);
			commandNameExpression();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class AlterCurrentUserContext extends AstRuleCtx {
		public TerminalNode CURRENT() { return getToken(Cypher5Parser.CURRENT, 0); }
		public TerminalNode USER() { return getToken(Cypher5Parser.USER, 0); }
		public AlterCurrentUserContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_alterCurrentUser; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterAlterCurrentUser(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitAlterCurrentUser(this);
		}
	}

	public final AlterCurrentUserContext alterCurrentUser() throws RecognitionException {
		AlterCurrentUserContext _localctx = new AlterCurrentUserContext(_ctx, getState());
		enterRule(_localctx, 444, RULE_alterCurrentUser);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2300);
			match(CURRENT);
			setState(2301);
			match(USER);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class AlterUserContext extends AstRuleCtx {
		public TerminalNode USER() { return getToken(Cypher5Parser.USER, 0); }
		public CommandNameExpressionContext commandNameExpression() {
			return getRuleContext(CommandNameExpressionContext.class,0);
		}
		public AlterUserContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_alterUser; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterAlterUser(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitAlterUser(this);
		}
	}

	public final AlterUserContext alterUser() throws RecognitionException {
		AlterUserContext _localctx = new AlterUserContext(_ctx, getState());
		enterRule(_localctx, 446, RULE_alterUser);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2303);
			match(USER);
			setState(2304);
			commandNameExpression();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class RemoveNamedProviderContext extends AstRuleCtx {
		public TerminalNode AUTH() { return getToken(Cypher5Parser.AUTH, 0); }
		public RemoveNamedProviderContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_removeNamedProvider; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterRemoveNamedProvider(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitRemoveNamedProvider(this);
		}
	}

	public final RemoveNamedProviderContext removeNamedProvider() throws RecognitionException {
		RemoveNamedProviderContext _localctx = new RemoveNamedProviderContext(_ctx, getState());
		enterRule(_localctx, 448, RULE_removeNamedProvider);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2306);
			match(AUTH);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class PasswordContext extends AstRuleCtx {
		public TerminalNode PASSWORD() { return getToken(Cypher5Parser.PASSWORD, 0); }
		public PasswordExpressionContext passwordExpression() {
			return getRuleContext(PasswordExpressionContext.class,0);
		}
		public PasswordContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_password; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterPassword(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitPassword(this);
		}
	}

	public final PasswordContext password() throws RecognitionException {
		PasswordContext _localctx = new PasswordContext(_ctx, getState());
		enterRule(_localctx, 450, RULE_password);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2308);
			match(PASSWORD);
			setState(2309);
			passwordExpression();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class PasswordOnlyContext extends AstRuleCtx {
		public TerminalNode PASSWORD() { return getToken(Cypher5Parser.PASSWORD, 0); }
		public PasswordExpressionContext passwordExpression() {
			return getRuleContext(PasswordExpressionContext.class,0);
		}
		public PasswordOnlyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_passwordOnly; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterPasswordOnly(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitPasswordOnly(this);
		}
	}

	public final PasswordOnlyContext passwordOnly() throws RecognitionException {
		PasswordOnlyContext _localctx = new PasswordOnlyContext(_ctx, getState());
		enterRule(_localctx, 452, RULE_passwordOnly);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2311);
			match(PASSWORD);
			setState(2312);
			passwordExpression();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class PasswordExpressionContext extends AstRuleCtx {
		public StringLiteralContext stringLiteral() {
			return getRuleContext(StringLiteralContext.class,0);
		}
		public ParameterContext parameter() {
			return getRuleContext(ParameterContext.class,0);
		}
		public PasswordExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_passwordExpression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterPasswordExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitPasswordExpression(this);
		}
	}

	public final PasswordExpressionContext passwordExpression() throws RecognitionException {
		PasswordExpressionContext _localctx = new PasswordExpressionContext(_ctx, getState());
		enterRule(_localctx, 454, RULE_passwordExpression);
		try {
			setState(2316);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case STRING_LITERAL1:
			case STRING_LITERAL2:
				enterOuterAlt(_localctx, 1);
				{
				setState(2314);
				stringLiteral();
				}
				break;
			case DOLLAR:
				enterOuterAlt(_localctx, 2);
				{
				setState(2315);
				parameter("STRING");
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class PasswordChangeRequiredContext extends AstRuleCtx {
		public TerminalNode CHANGE() { return getToken(Cypher5Parser.CHANGE, 0); }
		public TerminalNode REQUIRED() { return getToken(Cypher5Parser.REQUIRED, 0); }
		public PasswordChangeRequiredContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_passwordChangeRequired; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterPasswordChangeRequired(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitPasswordChangeRequired(this);
		}
	}

	public final PasswordChangeRequiredContext passwordChangeRequired() throws RecognitionException {
		PasswordChangeRequiredContext _localctx = new PasswordChangeRequiredContext(_ctx, getState());
		enterRule(_localctx, 456, RULE_passwordChangeRequired);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2318);
			match(CHANGE);
			setState(2319);
			match(REQUIRED);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class UserStatusContext extends AstRuleCtx {
		public TerminalNode STATUS() { return getToken(Cypher5Parser.STATUS, 0); }
		public TerminalNode ACTIVE() { return getToken(Cypher5Parser.ACTIVE, 0); }
		public TerminalNode SUSPENDED() { return getToken(Cypher5Parser.SUSPENDED, 0); }
		public UserStatusContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_userStatus; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterUserStatus(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitUserStatus(this);
		}
	}

	public final UserStatusContext userStatus() throws RecognitionException {
		UserStatusContext _localctx = new UserStatusContext(_ctx, getState());
		enterRule(_localctx, 458, RULE_userStatus);
		try {
			setState(2325);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,222,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(2321);
				match(STATUS);
				setState(2322);
				match(ACTIVE);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(2323);
				match(STATUS);
				setState(2324);
				match(SUSPENDED);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class HomeDatabaseContext extends AstRuleCtx {
		public TerminalNode HOME() { return getToken(Cypher5Parser.HOME, 0); }
		public TerminalNode DATABASE() { return getToken(Cypher5Parser.DATABASE, 0); }
		public SymbolicAliasNameOrParameterContext symbolicAliasNameOrParameter() {
			return getRuleContext(SymbolicAliasNameOrParameterContext.class,0);
		}
		public HomeDatabaseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_homeDatabase; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterHomeDatabase(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitHomeDatabase(this);
		}
	}

	public final HomeDatabaseContext homeDatabase() throws RecognitionException {
		HomeDatabaseContext _localctx = new HomeDatabaseContext(_ctx, getState());
		enterRule(_localctx, 460, RULE_homeDatabase);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2327);
			match(HOME);
			setState(2328);
			match(DATABASE);
			setState(2329);
			symbolicAliasNameOrParameter();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class SetAuthClauseContext extends AstRuleCtx {
		public TerminalNode AUTH() { return getToken(Cypher5Parser.AUTH, 0); }
		public SetAuthClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_setAuthClause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterSetAuthClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitSetAuthClause(this);
		}
	}

	public final SetAuthClauseContext setAuthClause() throws RecognitionException {
		SetAuthClauseContext _localctx = new SetAuthClauseContext(_ctx, getState());
		enterRule(_localctx, 462, RULE_setAuthClause);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2331);
			match(AUTH);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class UserAuthAttributeContext extends AstRuleCtx {
		public TerminalNode ID() { return getToken(Cypher5Parser.ID, 0); }
		public StringOrParameterExpressionContext stringOrParameterExpression() {
			return getRuleContext(StringOrParameterExpressionContext.class,0);
		}
		public UserAuthAttributeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_userAuthAttribute; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterUserAuthAttribute(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitUserAuthAttribute(this);
		}
	}

	public final UserAuthAttributeContext userAuthAttribute() throws RecognitionException {
		UserAuthAttributeContext _localctx = new UserAuthAttributeContext(_ctx, getState());
		enterRule(_localctx, 464, RULE_userAuthAttribute);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2333);
			match(ID);
			setState(2334);
			stringOrParameterExpression();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ShowUsersContext extends AstRuleCtx {
		public TerminalNode USER() { return getToken(Cypher5Parser.USER, 0); }
		public TerminalNode USERS() { return getToken(Cypher5Parser.USERS, 0); }
		public ShowUsersContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_showUsers; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterShowUsers(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitShowUsers(this);
		}
	}

	public final ShowUsersContext showUsers() throws RecognitionException {
		ShowUsersContext _localctx = new ShowUsersContext(_ctx, getState());
		enterRule(_localctx, 466, RULE_showUsers);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2336);
			_la = _input.LA(1);
			if ( !(_la==USER || _la==USERS) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ShowCurrentUserContext extends AstRuleCtx {
		public TerminalNode CURRENT() { return getToken(Cypher5Parser.CURRENT, 0); }
		public TerminalNode USER() { return getToken(Cypher5Parser.USER, 0); }
		public ShowCurrentUserContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_showCurrentUser; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterShowCurrentUser(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitShowCurrentUser(this);
		}
	}

	public final ShowCurrentUserContext showCurrentUser() throws RecognitionException {
		ShowCurrentUserContext _localctx = new ShowCurrentUserContext(_ctx, getState());
		enterRule(_localctx, 468, RULE_showCurrentUser);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2338);
			match(CURRENT);
			setState(2339);
			match(USER);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ShowSupportedPrivilegesContext extends AstRuleCtx {
		public TerminalNode SUPPORTED() { return getToken(Cypher5Parser.SUPPORTED, 0); }
		public PrivilegeTokenContext privilegeToken() {
			return getRuleContext(PrivilegeTokenContext.class,0);
		}
		public ShowSupportedPrivilegesContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_showSupportedPrivileges; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterShowSupportedPrivileges(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitShowSupportedPrivileges(this);
		}
	}

	public final ShowSupportedPrivilegesContext showSupportedPrivileges() throws RecognitionException {
		ShowSupportedPrivilegesContext _localctx = new ShowSupportedPrivilegesContext(_ctx, getState());
		enterRule(_localctx, 470, RULE_showSupportedPrivileges);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2341);
			match(SUPPORTED);
			setState(2342);
			privilegeToken();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ShowPrivilegesContext extends AstRuleCtx {
		public PrivilegeTokenContext privilegeToken() {
			return getRuleContext(PrivilegeTokenContext.class,0);
		}
		public ShowPrivilegesContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_showPrivileges; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterShowPrivileges(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitShowPrivileges(this);
		}
	}

	public final ShowPrivilegesContext showPrivileges() throws RecognitionException {
		ShowPrivilegesContext _localctx = new ShowPrivilegesContext(_ctx, getState());
		enterRule(_localctx, 472, RULE_showPrivileges);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2344);
			privilegeToken();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ShowRolePrivilegesContext extends AstRuleCtx {
		public RoleTokenContext roleToken() {
			return getRuleContext(RoleTokenContext.class,0);
		}
		public PrivilegeTokenContext privilegeToken() {
			return getRuleContext(PrivilegeTokenContext.class,0);
		}
		public ShowRolePrivilegesContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_showRolePrivileges; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterShowRolePrivileges(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitShowRolePrivileges(this);
		}
	}

	public final ShowRolePrivilegesContext showRolePrivileges() throws RecognitionException {
		ShowRolePrivilegesContext _localctx = new ShowRolePrivilegesContext(_ctx, getState());
		enterRule(_localctx, 474, RULE_showRolePrivileges);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2346);
			roleToken();
			setState(2347);
			privilegeToken();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ShowUserPrivilegesContext extends AstRuleCtx {
		public TerminalNode USER() { return getToken(Cypher5Parser.USER, 0); }
		public PrivilegeTokenContext privilegeToken() {
			return getRuleContext(PrivilegeTokenContext.class,0);
		}
		public TerminalNode USERS() { return getToken(Cypher5Parser.USERS, 0); }
		public ShowUserPrivilegesContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_showUserPrivileges; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterShowUserPrivileges(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitShowUserPrivileges(this);
		}
	}

	public final ShowUserPrivilegesContext showUserPrivileges() throws RecognitionException {
		ShowUserPrivilegesContext _localctx = new ShowUserPrivilegesContext(_ctx, getState());
		enterRule(_localctx, 476, RULE_showUserPrivileges);
		try {
			setState(2353);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case USER:
				enterOuterAlt(_localctx, 1);
				{
				setState(2349);
				match(USER);
				setState(2350);
				privilegeToken();
				}
				break;
			case USERS:
				enterOuterAlt(_localctx, 2);
				{
				setState(2351);
				match(USERS);
				setState(2352);
				privilegeToken();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class PrivilegeAsCommandContext extends AstRuleCtx {
		public TerminalNode AS() { return getToken(Cypher5Parser.AS, 0); }
		public TerminalNode COMMAND() { return getToken(Cypher5Parser.COMMAND, 0); }
		public TerminalNode COMMANDS() { return getToken(Cypher5Parser.COMMANDS, 0); }
		public PrivilegeAsCommandContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_privilegeAsCommand; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterPrivilegeAsCommand(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitPrivilegeAsCommand(this);
		}
	}

	public final PrivilegeAsCommandContext privilegeAsCommand() throws RecognitionException {
		PrivilegeAsCommandContext _localctx = new PrivilegeAsCommandContext(_ctx, getState());
		enterRule(_localctx, 478, RULE_privilegeAsCommand);
		try {
			setState(2359);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,224,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(2355);
				match(AS);
				setState(2356);
				match(COMMAND);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(2357);
				match(AS);
				setState(2358);
				match(COMMANDS);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class PrivilegeTokenContext extends AstRuleCtx {
		public TerminalNode PRIVILEGE() { return getToken(Cypher5Parser.PRIVILEGE, 0); }
		public TerminalNode PRIVILEGES() { return getToken(Cypher5Parser.PRIVILEGES, 0); }
		public PrivilegeTokenContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_privilegeToken; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterPrivilegeToken(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitPrivilegeToken(this);
		}
	}

	public final PrivilegeTokenContext privilegeToken() throws RecognitionException {
		PrivilegeTokenContext _localctx = new PrivilegeTokenContext(_ctx, getState());
		enterRule(_localctx, 480, RULE_privilegeToken);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2361);
			_la = _input.LA(1);
			if ( !(_la==PRIVILEGE || _la==PRIVILEGES) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class PrivilegeContext extends AstRuleCtx {
		public PrivilegeTokenContext privilegeToken() {
			return getRuleContext(PrivilegeTokenContext.class,0);
		}
		public PrivilegeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_privilege; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterPrivilege(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitPrivilege(this);
		}
	}

	public final PrivilegeContext privilege() throws RecognitionException {
		PrivilegeContext _localctx = new PrivilegeContext(_ctx, getState());
		enterRule(_localctx, 482, RULE_privilege);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2363);
			privilegeToken();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class AllPrivilegeContext extends AstRuleCtx {
		public TerminalNode ALL() { return getToken(Cypher5Parser.ALL, 0); }
		public TerminalNode ON() { return getToken(Cypher5Parser.ON, 0); }
		public AllPrivilegeTargetContext allPrivilegeTarget() {
			return getRuleContext(AllPrivilegeTargetContext.class,0);
		}
		public AllPrivilegeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_allPrivilege; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterAllPrivilege(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitAllPrivilege(this);
		}
	}

	public final AllPrivilegeContext allPrivilege() throws RecognitionException {
		AllPrivilegeContext _localctx = new AllPrivilegeContext(_ctx, getState());
		enterRule(_localctx, 484, RULE_allPrivilege);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2365);
			match(ALL);
			setState(2366);
			match(ON);
			setState(2367);
			allPrivilegeTarget();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class AllPrivilegeTypeContext extends AstRuleCtx {
		public TerminalNode PRIVILEGES() { return getToken(Cypher5Parser.PRIVILEGES, 0); }
		public AllPrivilegeTypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_allPrivilegeType; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterAllPrivilegeType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitAllPrivilegeType(this);
		}
	}

	public final AllPrivilegeTypeContext allPrivilegeType() throws RecognitionException {
		AllPrivilegeTypeContext _localctx = new AllPrivilegeTypeContext(_ctx, getState());
		enterRule(_localctx, 486, RULE_allPrivilegeType);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2369);
			match(PRIVILEGES);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class AllPrivilegeTargetContext extends AstRuleCtx {
		public TerminalNode DBMS() { return getToken(Cypher5Parser.DBMS, 0); }
		public AllPrivilegeTargetContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_allPrivilegeTarget; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterAllPrivilegeTarget(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitAllPrivilegeTarget(this);
		}
	}

	public final AllPrivilegeTargetContext allPrivilegeTarget() throws RecognitionException {
		AllPrivilegeTargetContext _localctx = new AllPrivilegeTargetContext(_ctx, getState());
		enterRule(_localctx, 488, RULE_allPrivilegeTarget);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2371);
			match(DBMS);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class CreatePrivilegeContext extends AstRuleCtx {
		public TerminalNode CREATE() { return getToken(Cypher5Parser.CREATE, 0); }
		public PrivilegeTokenContext privilegeToken() {
			return getRuleContext(PrivilegeTokenContext.class,0);
		}
		public CreatePrivilegeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_createPrivilege; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterCreatePrivilege(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitCreatePrivilege(this);
		}
	}

	public final CreatePrivilegeContext createPrivilege() throws RecognitionException {
		CreatePrivilegeContext _localctx = new CreatePrivilegeContext(_ctx, getState());
		enterRule(_localctx, 490, RULE_createPrivilege);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2373);
			match(CREATE);
			setState(2374);
			privilegeToken();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class CreatePrivilegeForDatabaseContext extends AstRuleCtx {
		public IndexTokenContext indexToken() {
			return getRuleContext(IndexTokenContext.class,0);
		}
		public ConstraintTokenContext constraintToken() {
			return getRuleContext(ConstraintTokenContext.class,0);
		}
		public CreatePrivilegeForDatabaseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_createPrivilegeForDatabase; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterCreatePrivilegeForDatabase(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitCreatePrivilegeForDatabase(this);
		}
	}

	public final CreatePrivilegeForDatabaseContext createPrivilegeForDatabase() throws RecognitionException {
		CreatePrivilegeForDatabaseContext _localctx = new CreatePrivilegeForDatabaseContext(_ctx, getState());
		enterRule(_localctx, 492, RULE_createPrivilegeForDatabase);
		try {
			setState(2378);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case INDEX:
			case INDEXES:
				enterOuterAlt(_localctx, 1);
				{
				setState(2376);
				indexToken();
				}
				break;
			case CONSTRAINT:
			case CONSTRAINTS:
				enterOuterAlt(_localctx, 2);
				{
				setState(2377);
				constraintToken();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class CreateNodePrivilegeTokenContext extends AstRuleCtx {
		public TerminalNode NEW() { return getToken(Cypher5Parser.NEW, 0); }
		public TerminalNode NODE() { return getToken(Cypher5Parser.NODE, 0); }
		public CreateNodePrivilegeTokenContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_createNodePrivilegeToken; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterCreateNodePrivilegeToken(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitCreateNodePrivilegeToken(this);
		}
	}

	public final CreateNodePrivilegeTokenContext createNodePrivilegeToken() throws RecognitionException {
		CreateNodePrivilegeTokenContext _localctx = new CreateNodePrivilegeTokenContext(_ctx, getState());
		enterRule(_localctx, 494, RULE_createNodePrivilegeToken);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2380);
			match(NEW);
			setState(2381);
			match(NODE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class CreateRelPrivilegeTokenContext extends AstRuleCtx {
		public TerminalNode NEW() { return getToken(Cypher5Parser.NEW, 0); }
		public TerminalNode RELATIONSHIP() { return getToken(Cypher5Parser.RELATIONSHIP, 0); }
		public CreateRelPrivilegeTokenContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_createRelPrivilegeToken; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterCreateRelPrivilegeToken(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitCreateRelPrivilegeToken(this);
		}
	}

	public final CreateRelPrivilegeTokenContext createRelPrivilegeToken() throws RecognitionException {
		CreateRelPrivilegeTokenContext _localctx = new CreateRelPrivilegeTokenContext(_ctx, getState());
		enterRule(_localctx, 496, RULE_createRelPrivilegeToken);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2383);
			match(NEW);
			setState(2384);
			match(RELATIONSHIP);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class CreatePropertyPrivilegeTokenContext extends AstRuleCtx {
		public TerminalNode NEW() { return getToken(Cypher5Parser.NEW, 0); }
		public TerminalNode PROPERTY() { return getToken(Cypher5Parser.PROPERTY, 0); }
		public CreatePropertyPrivilegeTokenContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_createPropertyPrivilegeToken; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterCreatePropertyPrivilegeToken(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitCreatePropertyPrivilegeToken(this);
		}
	}

	public final CreatePropertyPrivilegeTokenContext createPropertyPrivilegeToken() throws RecognitionException {
		CreatePropertyPrivilegeTokenContext _localctx = new CreatePropertyPrivilegeTokenContext(_ctx, getState());
		enterRule(_localctx, 498, RULE_createPropertyPrivilegeToken);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2386);
			match(NEW);
			setState(2387);
			match(PROPERTY);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ActionForDBMSContext extends AstRuleCtx {
		public TerminalNode ALIAS() { return getToken(Cypher5Parser.ALIAS, 0); }
		public ActionForDBMSContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_actionForDBMS; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterActionForDBMS(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitActionForDBMS(this);
		}
	}

	public final ActionForDBMSContext actionForDBMS() throws RecognitionException {
		ActionForDBMSContext _localctx = new ActionForDBMSContext(_ctx, getState());
		enterRule(_localctx, 500, RULE_actionForDBMS);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2389);
			match(ALIAS);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class DropPrivilegeContext extends AstRuleCtx {
		public TerminalNode DROP() { return getToken(Cypher5Parser.DROP, 0); }
		public PrivilegeTokenContext privilegeToken() {
			return getRuleContext(PrivilegeTokenContext.class,0);
		}
		public DropPrivilegeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_dropPrivilege; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterDropPrivilege(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitDropPrivilege(this);
		}
	}

	public final DropPrivilegeContext dropPrivilege() throws RecognitionException {
		DropPrivilegeContext _localctx = new DropPrivilegeContext(_ctx, getState());
		enterRule(_localctx, 502, RULE_dropPrivilege);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2391);
			match(DROP);
			setState(2392);
			privilegeToken();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class LoadPrivilegeContext extends AstRuleCtx {
		public TerminalNode LOAD() { return getToken(Cypher5Parser.LOAD, 0); }
		public LoadPrivilegeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_loadPrivilege; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterLoadPrivilege(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitLoadPrivilege(this);
		}
	}

	public final LoadPrivilegeContext loadPrivilege() throws RecognitionException {
		LoadPrivilegeContext _localctx = new LoadPrivilegeContext(_ctx, getState());
		enterRule(_localctx, 504, RULE_loadPrivilege);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2394);
			match(LOAD);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ShowPrivilegeContext extends AstRuleCtx {
		public TerminalNode SHOW() { return getToken(Cypher5Parser.SHOW, 0); }
		public PrivilegeTokenContext privilegeToken() {
			return getRuleContext(PrivilegeTokenContext.class,0);
		}
		public ShowPrivilegeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_showPrivilege; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterShowPrivilege(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitShowPrivilege(this);
		}
	}

	public final ShowPrivilegeContext showPrivilege() throws RecognitionException {
		ShowPrivilegeContext _localctx = new ShowPrivilegeContext(_ctx, getState());
		enterRule(_localctx, 506, RULE_showPrivilege);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2396);
			match(SHOW);
			setState(2397);
			privilegeToken();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class SetPrivilegeContext extends AstRuleCtx {
		public TerminalNode SET() { return getToken(Cypher5Parser.SET, 0); }
		public PrivilegeTokenContext privilegeToken() {
			return getRuleContext(PrivilegeTokenContext.class,0);
		}
		public SetPrivilegeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_setPrivilege; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterSetPrivilege(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitSetPrivilege(this);
		}
	}

	public final SetPrivilegeContext setPrivilege() throws RecognitionException {
		SetPrivilegeContext _localctx = new SetPrivilegeContext(_ctx, getState());
		enterRule(_localctx, 508, RULE_setPrivilege);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2399);
			match(SET);
			setState(2400);
			privilegeToken();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class PasswordTokenContext extends AstRuleCtx {
		public TerminalNode PASSWORD() { return getToken(Cypher5Parser.PASSWORD, 0); }
		public TerminalNode PASSWORDS() { return getToken(Cypher5Parser.PASSWORDS, 0); }
		public PasswordTokenContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_passwordToken; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterPasswordToken(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitPasswordToken(this);
		}
	}

	public final PasswordTokenContext passwordToken() throws RecognitionException {
		PasswordTokenContext _localctx = new PasswordTokenContext(_ctx, getState());
		enterRule(_localctx, 510, RULE_passwordToken);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2402);
			_la = _input.LA(1);
			if ( !(_la==PASSWORD || _la==PASSWORDS) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class RemovePrivilegeContext extends AstRuleCtx {
		public TerminalNode REMOVE() { return getToken(Cypher5Parser.REMOVE, 0); }
		public PrivilegeTokenContext privilegeToken() {
			return getRuleContext(PrivilegeTokenContext.class,0);
		}
		public RemovePrivilegeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_removePrivilege; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterRemovePrivilege(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitRemovePrivilege(this);
		}
	}

	public final RemovePrivilegeContext removePrivilege() throws RecognitionException {
		RemovePrivilegeContext _localctx = new RemovePrivilegeContext(_ctx, getState());
		enterRule(_localctx, 512, RULE_removePrivilege);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2404);
			match(REMOVE);
			setState(2405);
			privilegeToken();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class WritePrivilegeContext extends AstRuleCtx {
		public TerminalNode WRITE() { return getToken(Cypher5Parser.WRITE, 0); }
		public WritePrivilegeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_writePrivilege; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterWritePrivilege(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitWritePrivilege(this);
		}
	}

	public final WritePrivilegeContext writePrivilege() throws RecognitionException {
		WritePrivilegeContext _localctx = new WritePrivilegeContext(_ctx, getState());
		enterRule(_localctx, 514, RULE_writePrivilege);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2407);
			match(WRITE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class DatabasePrivilegeContext extends AstRuleCtx {
		public TerminalNode ACCESS() { return getToken(Cypher5Parser.ACCESS, 0); }
		public DatabasePrivilegeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_databasePrivilege; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterDatabasePrivilege(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitDatabasePrivilege(this);
		}
	}

	public final DatabasePrivilegeContext databasePrivilege() throws RecognitionException {
		DatabasePrivilegeContext _localctx = new DatabasePrivilegeContext(_ctx, getState());
		enterRule(_localctx, 516, RULE_databasePrivilege);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2409);
			match(ACCESS);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class DbmsPrivilegeContext extends AstRuleCtx {
		public TerminalNode ADMIN() { return getToken(Cypher5Parser.ADMIN, 0); }
		public DbmsPrivilegeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_dbmsPrivilege; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterDbmsPrivilege(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitDbmsPrivilege(this);
		}
	}

	public final DbmsPrivilegeContext dbmsPrivilege() throws RecognitionException {
		DbmsPrivilegeContext _localctx = new DbmsPrivilegeContext(_ctx, getState());
		enterRule(_localctx, 518, RULE_dbmsPrivilege);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2411);
			match(ADMIN);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class DbmsPrivilegeExecuteContext extends AstRuleCtx {
		public TerminalNode EXECUTE() { return getToken(Cypher5Parser.EXECUTE, 0); }
		public DbmsPrivilegeExecuteContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_dbmsPrivilegeExecute; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterDbmsPrivilegeExecute(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitDbmsPrivilegeExecute(this);
		}
	}

	public final DbmsPrivilegeExecuteContext dbmsPrivilegeExecute() throws RecognitionException {
		DbmsPrivilegeExecuteContext _localctx = new DbmsPrivilegeExecuteContext(_ctx, getState());
		enterRule(_localctx, 520, RULE_dbmsPrivilegeExecute);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2413);
			match(EXECUTE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class AdminTokenContext extends AstRuleCtx {
		public TerminalNode ADMIN() { return getToken(Cypher5Parser.ADMIN, 0); }
		public TerminalNode ADMINISTRATOR() { return getToken(Cypher5Parser.ADMINISTRATOR, 0); }
		public AdminTokenContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_adminToken; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterAdminToken(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitAdminToken(this);
		}
	}

	public final AdminTokenContext adminToken() throws RecognitionException {
		AdminTokenContext _localctx = new AdminTokenContext(_ctx, getState());
		enterRule(_localctx, 522, RULE_adminToken);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2415);
			_la = _input.LA(1);
			if ( !(_la==ADMIN || _la==ADMINISTRATOR) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ProcedureTokenContext extends AstRuleCtx {
		public TerminalNode PROCEDURE() { return getToken(Cypher5Parser.PROCEDURE, 0); }
		public TerminalNode PROCEDURES() { return getToken(Cypher5Parser.PROCEDURES, 0); }
		public ProcedureTokenContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_procedureToken; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterProcedureToken(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitProcedureToken(this);
		}
	}

	public final ProcedureTokenContext procedureToken() throws RecognitionException {
		ProcedureTokenContext _localctx = new ProcedureTokenContext(_ctx, getState());
		enterRule(_localctx, 524, RULE_procedureToken);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2417);
			_la = _input.LA(1);
			if ( !(_la==PROCEDURE || _la==PROCEDURES) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class IndexTokenContext extends AstRuleCtx {
		public TerminalNode INDEX() { return getToken(Cypher5Parser.INDEX, 0); }
		public TerminalNode INDEXES() { return getToken(Cypher5Parser.INDEXES, 0); }
		public IndexTokenContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_indexToken; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterIndexToken(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitIndexToken(this);
		}
	}

	public final IndexTokenContext indexToken() throws RecognitionException {
		IndexTokenContext _localctx = new IndexTokenContext(_ctx, getState());
		enterRule(_localctx, 526, RULE_indexToken);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2419);
			_la = _input.LA(1);
			if ( !(_la==INDEX || _la==INDEXES) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ConstraintTokenContext extends AstRuleCtx {
		public TerminalNode CONSTRAINT() { return getToken(Cypher5Parser.CONSTRAINT, 0); }
		public TerminalNode CONSTRAINTS() { return getToken(Cypher5Parser.CONSTRAINTS, 0); }
		public ConstraintTokenContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_constraintToken; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterConstraintToken(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitConstraintToken(this);
		}
	}

	public final ConstraintTokenContext constraintToken() throws RecognitionException {
		ConstraintTokenContext _localctx = new ConstraintTokenContext(_ctx, getState());
		enterRule(_localctx, 528, RULE_constraintToken);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2421);
			_la = _input.LA(1);
			if ( !(_la==CONSTRAINT || _la==CONSTRAINTS) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class TransactionTokenContext extends AstRuleCtx {
		public TerminalNode TRANSACTION() { return getToken(Cypher5Parser.TRANSACTION, 0); }
		public TerminalNode TRANSACTIONS() { return getToken(Cypher5Parser.TRANSACTIONS, 0); }
		public TransactionTokenContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_transactionToken; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterTransactionToken(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitTransactionToken(this);
		}
	}

	public final TransactionTokenContext transactionToken() throws RecognitionException {
		TransactionTokenContext _localctx = new TransactionTokenContext(_ctx, getState());
		enterRule(_localctx, 530, RULE_transactionToken);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2423);
			_la = _input.LA(1);
			if ( !(_la==TRANSACTION || _la==TRANSACTIONS) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class UserQualifierContext extends AstRuleCtx {
		public TerminalNode LPAREN() { return getToken(Cypher5Parser.LPAREN, 0); }
		public TerminalNode TIMES() { return getToken(Cypher5Parser.TIMES, 0); }
		public TerminalNode RPAREN() { return getToken(Cypher5Parser.RPAREN, 0); }
		public UserQualifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_userQualifier; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterUserQualifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitUserQualifier(this);
		}
	}

	public final UserQualifierContext userQualifier() throws RecognitionException {
		UserQualifierContext _localctx = new UserQualifierContext(_ctx, getState());
		enterRule(_localctx, 532, RULE_userQualifier);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2425);
			match(LPAREN);
			setState(2426);
			match(TIMES);
			setState(2427);
			match(RPAREN);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ExecuteFunctionQualifierContext extends AstRuleCtx {
		public GlobsContext globs() {
			return getRuleContext(GlobsContext.class,0);
		}
		public ExecuteFunctionQualifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_executeFunctionQualifier; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterExecuteFunctionQualifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitExecuteFunctionQualifier(this);
		}
	}

	public final ExecuteFunctionQualifierContext executeFunctionQualifier() throws RecognitionException {
		ExecuteFunctionQualifierContext _localctx = new ExecuteFunctionQualifierContext(_ctx, getState());
		enterRule(_localctx, 534, RULE_executeFunctionQualifier);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2429);
			globs();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ExecuteProcedureQualifierContext extends AstRuleCtx {
		public GlobsContext globs() {
			return getRuleContext(GlobsContext.class,0);
		}
		public ExecuteProcedureQualifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_executeProcedureQualifier; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterExecuteProcedureQualifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitExecuteProcedureQualifier(this);
		}
	}

	public final ExecuteProcedureQualifierContext executeProcedureQualifier() throws RecognitionException {
		ExecuteProcedureQualifierContext _localctx = new ExecuteProcedureQualifierContext(_ctx, getState());
		enterRule(_localctx, 536, RULE_executeProcedureQualifier);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2431);
			globs();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class SettingQualifierContext extends AstRuleCtx {
		public GlobsContext globs() {
			return getRuleContext(GlobsContext.class,0);
		}
		public SettingQualifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_settingQualifier; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterSettingQualifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitSettingQualifier(this);
		}
	}

	public final SettingQualifierContext settingQualifier() throws RecognitionException {
		SettingQualifierContext _localctx = new SettingQualifierContext(_ctx, getState());
		enterRule(_localctx, 538, RULE_settingQualifier);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2433);
			globs();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class GlobsContext extends AstRuleCtx {
		public List<GlobContext> glob() {
			return getRuleContexts(GlobContext.class);
		}
		public GlobContext glob(int i) {
			return getRuleContext(GlobContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(Cypher5Parser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(Cypher5Parser.COMMA, i);
		}
		public GlobsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_globs; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterGlobs(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitGlobs(this);
		}
	}

	public final GlobsContext globs() throws RecognitionException {
		GlobsContext _localctx = new GlobsContext(_ctx, getState());
		enterRule(_localctx, 540, RULE_globs);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2435);
			glob();
			setState(2440);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(2436);
				match(COMMA);
				setState(2437);
				glob();
				}
				}
				setState(2442);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class GlobContext extends AstRuleCtx {
		public EscapedSymbolicNameStringContext escapedSymbolicNameString() {
			return getRuleContext(EscapedSymbolicNameStringContext.class,0);
		}
		public GlobRecursiveContext globRecursive() {
			return getRuleContext(GlobRecursiveContext.class,0);
		}
		public GlobContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_glob; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterGlob(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitGlob(this);
		}
	}

	public final GlobContext glob() throws RecognitionException {
		GlobContext _localctx = new GlobContext(_ctx, getState());
		enterRule(_localctx, 542, RULE_glob);
		int _la;
		try {
			setState(2448);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case ESCAPED_SYMBOLIC_NAME:
				enterOuterAlt(_localctx, 1);
				{
				setState(2443);
				escapedSymbolicNameString();
				setState(2445);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & -985163492231168L) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & -1170935971839279105L) != 0) || ((((_la - 128)) & ~0x3f) == 0 && ((1L << (_la - 128)) & -129253703681L) != 0) || ((((_la - 192)) & ~0x3f) == 0 && ((1L << (_la - 192)) & -36169535631395201L) != 0) || ((((_la - 256)) & ~0x3f) == 0 && ((1L << (_la - 256)) & 18014398509481983L) != 0)) {
					{
					setState(2444);
					globRecursive();
					}
				}

				}
				break;
			case ACCESS:
			case ACTIVE:
			case ADMIN:
			case ADMINISTRATOR:
			case ALIAS:
			case ALIASES:
			case ALL_SHORTEST_PATHS:
			case ALL:
			case ALTER:
			case AND:
			case ANY:
			case ARRAY:
			case AS:
			case ASC:
			case ASCENDING:
			case ASSERT:
			case ASSIGN:
			case AT:
			case AUTH:
			case BINDINGS:
			case BOOL:
			case BOOLEAN:
			case BOOSTED:
			case BOTH:
			case BREAK:
			case BRIEF:
			case BTREE:
			case BUILT:
			case BY:
			case CALL:
			case CASCADE:
			case CASE:
			case CHANGE:
			case CIDR:
			case COLLECT:
			case COMMAND:
			case COMMANDS:
			case COMMIT:
			case COMPOSITE:
			case CONCURRENT:
			case CONSTRAINT:
			case CONSTRAINTS:
			case CONTAINS:
			case COPY:
			case CONTINUE:
			case COUNT:
			case CREATE:
			case CSV:
			case CURRENT:
			case DATA:
			case DATABASE:
			case DATABASES:
			case DATE:
			case DATETIME:
			case DBMS:
			case DEALLOCATE:
			case DEFAULT:
			case DEFINED:
			case DELETE:
			case DENY:
			case DESC:
			case DESCENDING:
			case DESTROY:
			case DETACH:
			case DIFFERENT:
			case DISTINCT:
			case DOT:
			case DRIVER:
			case DROP:
			case DRYRUN:
			case DUMP:
			case DURATION:
			case EACH:
			case EDGE:
			case ENABLE:
			case ELEMENT:
			case ELEMENTS:
			case ELSE:
			case ENCRYPTED:
			case END:
			case ENDS:
			case EXECUTABLE:
			case EXECUTE:
			case EXIST:
			case EXISTENCE:
			case EXISTS:
			case ERROR:
			case FAIL:
			case FALSE:
			case FIELDTERMINATOR:
			case FINISH:
			case FLOAT:
			case FOR:
			case FOREACH:
			case FROM:
			case FULLTEXT:
			case FUNCTION:
			case FUNCTIONS:
			case GRANT:
			case GRAPH:
			case GRAPHS:
			case GROUP:
			case GROUPS:
			case HEADERS:
			case HOME:
			case ID:
			case IF:
			case IMPERSONATE:
			case IMMUTABLE:
			case IN:
			case INDEX:
			case INDEXES:
			case INF:
			case INFINITY:
			case INSERT:
			case INT:
			case INTEGER:
			case IS:
			case JOIN:
			case KEY:
			case LABEL:
			case LABELS:
			case LEADING:
			case LIMITROWS:
			case LIST:
			case LOAD:
			case LOCAL:
			case LOOKUP:
			case MANAGEMENT:
			case MAP:
			case MATCH:
			case MERGE:
			case NAME:
			case NAMES:
			case NAN:
			case NFC:
			case NFD:
			case NFKC:
			case NFKD:
			case NEW:
			case NODE:
			case NODETACH:
			case NODES:
			case NONE:
			case NORMALIZE:
			case NORMALIZED:
			case NOT:
			case NOTHING:
			case NOWAIT:
			case NULL:
			case OF:
			case OFFSET:
			case ON:
			case ONLY:
			case OPTIONAL:
			case OPTIONS:
			case OPTION:
			case OR:
			case ORDER:
			case OUTPUT:
			case PASSWORD:
			case PASSWORDS:
			case PATH:
			case PATHS:
			case PERIODIC:
			case PLAINTEXT:
			case POINT:
			case POPULATED:
			case PRIMARY:
			case PRIMARIES:
			case PRIVILEGE:
			case PRIVILEGES:
			case PROCEDURE:
			case PROCEDURES:
			case PROPERTIES:
			case PROPERTY:
			case PROVIDER:
			case PROVIDERS:
			case QUESTION:
			case RANGE:
			case READ:
			case REALLOCATE:
			case REDUCE:
			case RENAME:
			case REL:
			case RELATIONSHIP:
			case RELATIONSHIPS:
			case REMOVE:
			case REPEATABLE:
			case REPLACE:
			case REPORT:
			case REQUIRE:
			case REQUIRED:
			case RESTRICT:
			case RETURN:
			case REVOKE:
			case ROLE:
			case ROLES:
			case ROW:
			case ROWS:
			case SCAN:
			case SEC:
			case SECOND:
			case SECONDARY:
			case SECONDARIES:
			case SECONDS:
			case SEEK:
			case SERVER:
			case SERVERS:
			case SET:
			case SETTING:
			case SETTINGS:
			case SHORTEST_PATH:
			case SHORTEST:
			case SHOW:
			case SIGNED:
			case SINGLE:
			case SKIPROWS:
			case START:
			case STARTS:
			case STATUS:
			case STOP:
			case STRING:
			case SUPPORTED:
			case SUSPENDED:
			case TARGET:
			case TERMINATE:
			case TEXT:
			case THEN:
			case TIME:
			case TIMES:
			case TIMESTAMP:
			case TIMEZONE:
			case TO:
			case TOPOLOGY:
			case TRAILING:
			case TRANSACTION:
			case TRANSACTIONS:
			case TRAVERSE:
			case TRIM:
			case TRUE:
			case TYPE:
			case TYPED:
			case TYPES:
			case UNION:
			case UNIQUE:
			case UNIQUENESS:
			case UNWIND:
			case URL:
			case USE:
			case USER:
			case USERS:
			case USING:
			case VALUE:
			case VARCHAR:
			case VECTOR:
			case VERBOSE:
			case VERTEX:
			case WAIT:
			case WHEN:
			case WHERE:
			case WITH:
			case WITHOUT:
			case WRITE:
			case XOR:
			case YIELD:
			case ZONE:
			case ZONED:
			case IDENTIFIER:
				enterOuterAlt(_localctx, 2);
				{
				setState(2447);
				globRecursive();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class GlobRecursiveContext extends AstRuleCtx {
		public GlobPartContext globPart() {
			return getRuleContext(GlobPartContext.class,0);
		}
		public GlobRecursiveContext globRecursive() {
			return getRuleContext(GlobRecursiveContext.class,0);
		}
		public GlobRecursiveContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_globRecursive; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterGlobRecursive(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitGlobRecursive(this);
		}
	}

	public final GlobRecursiveContext globRecursive() throws RecognitionException {
		GlobRecursiveContext _localctx = new GlobRecursiveContext(_ctx, getState());
		enterRule(_localctx, 544, RULE_globRecursive);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2450);
			globPart();
			setState(2452);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & -985163492231168L) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & -1170935971839279105L) != 0) || ((((_la - 128)) & ~0x3f) == 0 && ((1L << (_la - 128)) & -129253703681L) != 0) || ((((_la - 192)) & ~0x3f) == 0 && ((1L << (_la - 192)) & -36169535631395201L) != 0) || ((((_la - 256)) & ~0x3f) == 0 && ((1L << (_la - 256)) & 18014398509481983L) != 0)) {
				{
				setState(2451);
				globRecursive();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class GlobPartContext extends AstRuleCtx {
		public TerminalNode DOT() { return getToken(Cypher5Parser.DOT, 0); }
		public EscapedSymbolicNameStringContext escapedSymbolicNameString() {
			return getRuleContext(EscapedSymbolicNameStringContext.class,0);
		}
		public TerminalNode QUESTION() { return getToken(Cypher5Parser.QUESTION, 0); }
		public TerminalNode TIMES() { return getToken(Cypher5Parser.TIMES, 0); }
		public UnescapedSymbolicNameStringContext unescapedSymbolicNameString() {
			return getRuleContext(UnescapedSymbolicNameStringContext.class,0);
		}
		public GlobPartContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_globPart; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterGlobPart(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitGlobPart(this);
		}
	}

	public final GlobPartContext globPart() throws RecognitionException {
		GlobPartContext _localctx = new GlobPartContext(_ctx, getState());
		enterRule(_localctx, 546, RULE_globPart);
		int _la;
		try {
			setState(2461);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case DOT:
				enterOuterAlt(_localctx, 1);
				{
				setState(2454);
				match(DOT);
				setState(2456);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==ESCAPED_SYMBOLIC_NAME) {
					{
					setState(2455);
					escapedSymbolicNameString();
					}
				}

				}
				break;
			case QUESTION:
				enterOuterAlt(_localctx, 2);
				{
				setState(2458);
				match(QUESTION);
				}
				break;
			case TIMES:
				enterOuterAlt(_localctx, 3);
				{
				setState(2459);
				match(TIMES);
				}
				break;
			case ACCESS:
			case ACTIVE:
			case ADMIN:
			case ADMINISTRATOR:
			case ALIAS:
			case ALIASES:
			case ALL_SHORTEST_PATHS:
			case ALL:
			case ALTER:
			case AND:
			case ANY:
			case ARRAY:
			case AS:
			case ASC:
			case ASCENDING:
			case ASSERT:
			case ASSIGN:
			case AT:
			case AUTH:
			case BINDINGS:
			case BOOL:
			case BOOLEAN:
			case BOOSTED:
			case BOTH:
			case BREAK:
			case BRIEF:
			case BTREE:
			case BUILT:
			case BY:
			case CALL:
			case CASCADE:
			case CASE:
			case CHANGE:
			case CIDR:
			case COLLECT:
			case COMMAND:
			case COMMANDS:
			case COMMIT:
			case COMPOSITE:
			case CONCURRENT:
			case CONSTRAINT:
			case CONSTRAINTS:
			case CONTAINS:
			case COPY:
			case CONTINUE:
			case COUNT:
			case CREATE:
			case CSV:
			case CURRENT:
			case DATA:
			case DATABASE:
			case DATABASES:
			case DATE:
			case DATETIME:
			case DBMS:
			case DEALLOCATE:
			case DEFAULT:
			case DEFINED:
			case DELETE:
			case DENY:
			case DESC:
			case DESCENDING:
			case DESTROY:
			case DETACH:
			case DIFFERENT:
			case DISTINCT:
			case DRIVER:
			case DROP:
			case DRYRUN:
			case DUMP:
			case DURATION:
			case EACH:
			case EDGE:
			case ENABLE:
			case ELEMENT:
			case ELEMENTS:
			case ELSE:
			case ENCRYPTED:
			case END:
			case ENDS:
			case EXECUTABLE:
			case EXECUTE:
			case EXIST:
			case EXISTENCE:
			case EXISTS:
			case ERROR:
			case FAIL:
			case FALSE:
			case FIELDTERMINATOR:
			case FINISH:
			case FLOAT:
			case FOR:
			case FOREACH:
			case FROM:
			case FULLTEXT:
			case FUNCTION:
			case FUNCTIONS:
			case GRANT:
			case GRAPH:
			case GRAPHS:
			case GROUP:
			case GROUPS:
			case HEADERS:
			case HOME:
			case ID:
			case IF:
			case IMPERSONATE:
			case IMMUTABLE:
			case IN:
			case INDEX:
			case INDEXES:
			case INF:
			case INFINITY:
			case INSERT:
			case INT:
			case INTEGER:
			case IS:
			case JOIN:
			case KEY:
			case LABEL:
			case LABELS:
			case LEADING:
			case LIMITROWS:
			case LIST:
			case LOAD:
			case LOCAL:
			case LOOKUP:
			case MANAGEMENT:
			case MAP:
			case MATCH:
			case MERGE:
			case NAME:
			case NAMES:
			case NAN:
			case NFC:
			case NFD:
			case NFKC:
			case NFKD:
			case NEW:
			case NODE:
			case NODETACH:
			case NODES:
			case NONE:
			case NORMALIZE:
			case NORMALIZED:
			case NOT:
			case NOTHING:
			case NOWAIT:
			case NULL:
			case OF:
			case OFFSET:
			case ON:
			case ONLY:
			case OPTIONAL:
			case OPTIONS:
			case OPTION:
			case OR:
			case ORDER:
			case OUTPUT:
			case PASSWORD:
			case PASSWORDS:
			case PATH:
			case PATHS:
			case PERIODIC:
			case PLAINTEXT:
			case POINT:
			case POPULATED:
			case PRIMARY:
			case PRIMARIES:
			case PRIVILEGE:
			case PRIVILEGES:
			case PROCEDURE:
			case PROCEDURES:
			case PROPERTIES:
			case PROPERTY:
			case PROVIDER:
			case PROVIDERS:
			case RANGE:
			case READ:
			case REALLOCATE:
			case REDUCE:
			case RENAME:
			case REL:
			case RELATIONSHIP:
			case RELATIONSHIPS:
			case REMOVE:
			case REPEATABLE:
			case REPLACE:
			case REPORT:
			case REQUIRE:
			case REQUIRED:
			case RESTRICT:
			case RETURN:
			case REVOKE:
			case ROLE:
			case ROLES:
			case ROW:
			case ROWS:
			case SCAN:
			case SEC:
			case SECOND:
			case SECONDARY:
			case SECONDARIES:
			case SECONDS:
			case SEEK:
			case SERVER:
			case SERVERS:
			case SET:
			case SETTING:
			case SETTINGS:
			case SHORTEST_PATH:
			case SHORTEST:
			case SHOW:
			case SIGNED:
			case SINGLE:
			case SKIPROWS:
			case START:
			case STARTS:
			case STATUS:
			case STOP:
			case STRING:
			case SUPPORTED:
			case SUSPENDED:
			case TARGET:
			case TERMINATE:
			case TEXT:
			case THEN:
			case TIME:
			case TIMESTAMP:
			case TIMEZONE:
			case TO:
			case TOPOLOGY:
			case TRAILING:
			case TRANSACTION:
			case TRANSACTIONS:
			case TRAVERSE:
			case TRIM:
			case TRUE:
			case TYPE:
			case TYPED:
			case TYPES:
			case UNION:
			case UNIQUE:
			case UNIQUENESS:
			case UNWIND:
			case URL:
			case USE:
			case USER:
			case USERS:
			case USING:
			case VALUE:
			case VARCHAR:
			case VECTOR:
			case VERBOSE:
			case VERTEX:
			case WAIT:
			case WHEN:
			case WHERE:
			case WITH:
			case WITHOUT:
			case WRITE:
			case XOR:
			case YIELD:
			case ZONE:
			case ZONED:
			case IDENTIFIER:
				enterOuterAlt(_localctx, 4);
				{
				setState(2460);
				unescapedSymbolicNameString();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class QualifiedGraphPrivilegesWithPropertyContext extends AstRuleCtx {
		public TerminalNode READ() { return getToken(Cypher5Parser.READ, 0); }
		public QualifiedGraphPrivilegesWithPropertyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_qualifiedGraphPrivilegesWithProperty; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterQualifiedGraphPrivilegesWithProperty(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitQualifiedGraphPrivilegesWithProperty(this);
		}
	}

	public final QualifiedGraphPrivilegesWithPropertyContext qualifiedGraphPrivilegesWithProperty() throws RecognitionException {
		QualifiedGraphPrivilegesWithPropertyContext _localctx = new QualifiedGraphPrivilegesWithPropertyContext(_ctx, getState());
		enterRule(_localctx, 548, RULE_qualifiedGraphPrivilegesWithProperty);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2463);
			match(READ);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class QualifiedGraphPrivilegesContext extends AstRuleCtx {
		public TerminalNode DELETE() { return getToken(Cypher5Parser.DELETE, 0); }
		public TerminalNode MERGE() { return getToken(Cypher5Parser.MERGE, 0); }
		public QualifiedGraphPrivilegesContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_qualifiedGraphPrivileges; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterQualifiedGraphPrivileges(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitQualifiedGraphPrivileges(this);
		}
	}

	public final QualifiedGraphPrivilegesContext qualifiedGraphPrivileges() throws RecognitionException {
		QualifiedGraphPrivilegesContext _localctx = new QualifiedGraphPrivilegesContext(_ctx, getState());
		enterRule(_localctx, 550, RULE_qualifiedGraphPrivileges);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2465);
			_la = _input.LA(1);
			if ( !(_la==DELETE || _la==MERGE) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class LabelsResourceContext extends AstRuleCtx {
		public TerminalNode TIMES() { return getToken(Cypher5Parser.TIMES, 0); }
		public NonEmptyStringListContext nonEmptyStringList() {
			return getRuleContext(NonEmptyStringListContext.class,0);
		}
		public LabelsResourceContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_labelsResource; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterLabelsResource(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitLabelsResource(this);
		}
	}

	public final LabelsResourceContext labelsResource() throws RecognitionException {
		LabelsResourceContext _localctx = new LabelsResourceContext(_ctx, getState());
		enterRule(_localctx, 552, RULE_labelsResource);
		try {
			setState(2469);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case TIMES:
				enterOuterAlt(_localctx, 1);
				{
				setState(2467);
				match(TIMES);
				}
				break;
			case ESCAPED_SYMBOLIC_NAME:
			case ACCESS:
			case ACTIVE:
			case ADMIN:
			case ADMINISTRATOR:
			case ALIAS:
			case ALIASES:
			case ALL_SHORTEST_PATHS:
			case ALL:
			case ALTER:
			case AND:
			case ANY:
			case ARRAY:
			case AS:
			case ASC:
			case ASCENDING:
			case ASSERT:
			case ASSIGN:
			case AT:
			case AUTH:
			case BINDINGS:
			case BOOL:
			case BOOLEAN:
			case BOOSTED:
			case BOTH:
			case BREAK:
			case BRIEF:
			case BTREE:
			case BUILT:
			case BY:
			case CALL:
			case CASCADE:
			case CASE:
			case CHANGE:
			case CIDR:
			case COLLECT:
			case COMMAND:
			case COMMANDS:
			case COMMIT:
			case COMPOSITE:
			case CONCURRENT:
			case CONSTRAINT:
			case CONSTRAINTS:
			case CONTAINS:
			case COPY:
			case CONTINUE:
			case COUNT:
			case CREATE:
			case CSV:
			case CURRENT:
			case DATA:
			case DATABASE:
			case DATABASES:
			case DATE:
			case DATETIME:
			case DBMS:
			case DEALLOCATE:
			case DEFAULT:
			case DEFINED:
			case DELETE:
			case DENY:
			case DESC:
			case DESCENDING:
			case DESTROY:
			case DETACH:
			case DIFFERENT:
			case DISTINCT:
			case DRIVER:
			case DROP:
			case DRYRUN:
			case DUMP:
			case DURATION:
			case EACH:
			case EDGE:
			case ENABLE:
			case ELEMENT:
			case ELEMENTS:
			case ELSE:
			case ENCRYPTED:
			case END:
			case ENDS:
			case EXECUTABLE:
			case EXECUTE:
			case EXIST:
			case EXISTENCE:
			case EXISTS:
			case ERROR:
			case FAIL:
			case FALSE:
			case FIELDTERMINATOR:
			case FINISH:
			case FLOAT:
			case FOR:
			case FOREACH:
			case FROM:
			case FULLTEXT:
			case FUNCTION:
			case FUNCTIONS:
			case GRANT:
			case GRAPH:
			case GRAPHS:
			case GROUP:
			case GROUPS:
			case HEADERS:
			case HOME:
			case ID:
			case IF:
			case IMPERSONATE:
			case IMMUTABLE:
			case IN:
			case INDEX:
			case INDEXES:
			case INF:
			case INFINITY:
			case INSERT:
			case INT:
			case INTEGER:
			case IS:
			case JOIN:
			case KEY:
			case LABEL:
			case LABELS:
			case LEADING:
			case LIMITROWS:
			case LIST:
			case LOAD:
			case LOCAL:
			case LOOKUP:
			case MANAGEMENT:
			case MAP:
			case MATCH:
			case MERGE:
			case NAME:
			case NAMES:
			case NAN:
			case NFC:
			case NFD:
			case NFKC:
			case NFKD:
			case NEW:
			case NODE:
			case NODETACH:
			case NODES:
			case NONE:
			case NORMALIZE:
			case NORMALIZED:
			case NOT:
			case NOTHING:
			case NOWAIT:
			case NULL:
			case OF:
			case OFFSET:
			case ON:
			case ONLY:
			case OPTIONAL:
			case OPTIONS:
			case OPTION:
			case OR:
			case ORDER:
			case OUTPUT:
			case PASSWORD:
			case PASSWORDS:
			case PATH:
			case PATHS:
			case PERIODIC:
			case PLAINTEXT:
			case POINT:
			case POPULATED:
			case PRIMARY:
			case PRIMARIES:
			case PRIVILEGE:
			case PRIVILEGES:
			case PROCEDURE:
			case PROCEDURES:
			case PROPERTIES:
			case PROPERTY:
			case PROVIDER:
			case PROVIDERS:
			case RANGE:
			case READ:
			case REALLOCATE:
			case REDUCE:
			case RENAME:
			case REL:
			case RELATIONSHIP:
			case RELATIONSHIPS:
			case REMOVE:
			case REPEATABLE:
			case REPLACE:
			case REPORT:
			case REQUIRE:
			case REQUIRED:
			case RESTRICT:
			case RETURN:
			case REVOKE:
			case ROLE:
			case ROLES:
			case ROW:
			case ROWS:
			case SCAN:
			case SEC:
			case SECOND:
			case SECONDARY:
			case SECONDARIES:
			case SECONDS:
			case SEEK:
			case SERVER:
			case SERVERS:
			case SET:
			case SETTING:
			case SETTINGS:
			case SHORTEST_PATH:
			case SHORTEST:
			case SHOW:
			case SIGNED:
			case SINGLE:
			case SKIPROWS:
			case START:
			case STARTS:
			case STATUS:
			case STOP:
			case STRING:
			case SUPPORTED:
			case SUSPENDED:
			case TARGET:
			case TERMINATE:
			case TEXT:
			case THEN:
			case TIME:
			case TIMESTAMP:
			case TIMEZONE:
			case TO:
			case TOPOLOGY:
			case TRAILING:
			case TRANSACTION:
			case TRANSACTIONS:
			case TRAVERSE:
			case TRIM:
			case TRUE:
			case TYPE:
			case TYPED:
			case TYPES:
			case UNION:
			case UNIQUE:
			case UNIQUENESS:
			case UNWIND:
			case URL:
			case USE:
			case USER:
			case USERS:
			case USING:
			case VALUE:
			case VARCHAR:
			case VECTOR:
			case VERBOSE:
			case VERTEX:
			case WAIT:
			case WHEN:
			case WHERE:
			case WITH:
			case WITHOUT:
			case WRITE:
			case XOR:
			case YIELD:
			case ZONE:
			case ZONED:
			case IDENTIFIER:
				enterOuterAlt(_localctx, 2);
				{
				setState(2468);
				nonEmptyStringList();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class PropertiesResourceContext extends AstRuleCtx {
		public TerminalNode LCURLY() { return getToken(Cypher5Parser.LCURLY, 0); }
		public TerminalNode RCURLY() { return getToken(Cypher5Parser.RCURLY, 0); }
		public TerminalNode TIMES() { return getToken(Cypher5Parser.TIMES, 0); }
		public NonEmptyStringListContext nonEmptyStringList() {
			return getRuleContext(NonEmptyStringListContext.class,0);
		}
		public PropertiesResourceContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_propertiesResource; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterPropertiesResource(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitPropertiesResource(this);
		}
	}

	public final PropertiesResourceContext propertiesResource() throws RecognitionException {
		PropertiesResourceContext _localctx = new PropertiesResourceContext(_ctx, getState());
		enterRule(_localctx, 554, RULE_propertiesResource);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2471);
			match(LCURLY);
			setState(2474);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case TIMES:
				{
				setState(2472);
				match(TIMES);
				}
				break;
			case ESCAPED_SYMBOLIC_NAME:
			case ACCESS:
			case ACTIVE:
			case ADMIN:
			case ADMINISTRATOR:
			case ALIAS:
			case ALIASES:
			case ALL_SHORTEST_PATHS:
			case ALL:
			case ALTER:
			case AND:
			case ANY:
			case ARRAY:
			case AS:
			case ASC:
			case ASCENDING:
			case ASSERT:
			case ASSIGN:
			case AT:
			case AUTH:
			case BINDINGS:
			case BOOL:
			case BOOLEAN:
			case BOOSTED:
			case BOTH:
			case BREAK:
			case BRIEF:
			case BTREE:
			case BUILT:
			case BY:
			case CALL:
			case CASCADE:
			case CASE:
			case CHANGE:
			case CIDR:
			case COLLECT:
			case COMMAND:
			case COMMANDS:
			case COMMIT:
			case COMPOSITE:
			case CONCURRENT:
			case CONSTRAINT:
			case CONSTRAINTS:
			case CONTAINS:
			case COPY:
			case CONTINUE:
			case COUNT:
			case CREATE:
			case CSV:
			case CURRENT:
			case DATA:
			case DATABASE:
			case DATABASES:
			case DATE:
			case DATETIME:
			case DBMS:
			case DEALLOCATE:
			case DEFAULT:
			case DEFINED:
			case DELETE:
			case DENY:
			case DESC:
			case DESCENDING:
			case DESTROY:
			case DETACH:
			case DIFFERENT:
			case DISTINCT:
			case DRIVER:
			case DROP:
			case DRYRUN:
			case DUMP:
			case DURATION:
			case EACH:
			case EDGE:
			case ENABLE:
			case ELEMENT:
			case ELEMENTS:
			case ELSE:
			case ENCRYPTED:
			case END:
			case ENDS:
			case EXECUTABLE:
			case EXECUTE:
			case EXIST:
			case EXISTENCE:
			case EXISTS:
			case ERROR:
			case FAIL:
			case FALSE:
			case FIELDTERMINATOR:
			case FINISH:
			case FLOAT:
			case FOR:
			case FOREACH:
			case FROM:
			case FULLTEXT:
			case FUNCTION:
			case FUNCTIONS:
			case GRANT:
			case GRAPH:
			case GRAPHS:
			case GROUP:
			case GROUPS:
			case HEADERS:
			case HOME:
			case ID:
			case IF:
			case IMPERSONATE:
			case IMMUTABLE:
			case IN:
			case INDEX:
			case INDEXES:
			case INF:
			case INFINITY:
			case INSERT:
			case INT:
			case INTEGER:
			case IS:
			case JOIN:
			case KEY:
			case LABEL:
			case LABELS:
			case LEADING:
			case LIMITROWS:
			case LIST:
			case LOAD:
			case LOCAL:
			case LOOKUP:
			case MANAGEMENT:
			case MAP:
			case MATCH:
			case MERGE:
			case NAME:
			case NAMES:
			case NAN:
			case NFC:
			case NFD:
			case NFKC:
			case NFKD:
			case NEW:
			case NODE:
			case NODETACH:
			case NODES:
			case NONE:
			case NORMALIZE:
			case NORMALIZED:
			case NOT:
			case NOTHING:
			case NOWAIT:
			case NULL:
			case OF:
			case OFFSET:
			case ON:
			case ONLY:
			case OPTIONAL:
			case OPTIONS:
			case OPTION:
			case OR:
			case ORDER:
			case OUTPUT:
			case PASSWORD:
			case PASSWORDS:
			case PATH:
			case PATHS:
			case PERIODIC:
			case PLAINTEXT:
			case POINT:
			case POPULATED:
			case PRIMARY:
			case PRIMARIES:
			case PRIVILEGE:
			case PRIVILEGES:
			case PROCEDURE:
			case PROCEDURES:
			case PROPERTIES:
			case PROPERTY:
			case PROVIDER:
			case PROVIDERS:
			case RANGE:
			case READ:
			case REALLOCATE:
			case REDUCE:
			case RENAME:
			case REL:
			case RELATIONSHIP:
			case RELATIONSHIPS:
			case REMOVE:
			case REPEATABLE:
			case REPLACE:
			case REPORT:
			case REQUIRE:
			case REQUIRED:
			case RESTRICT:
			case RETURN:
			case REVOKE:
			case ROLE:
			case ROLES:
			case ROW:
			case ROWS:
			case SCAN:
			case SEC:
			case SECOND:
			case SECONDARY:
			case SECONDARIES:
			case SECONDS:
			case SEEK:
			case SERVER:
			case SERVERS:
			case SET:
			case SETTING:
			case SETTINGS:
			case SHORTEST_PATH:
			case SHORTEST:
			case SHOW:
			case SIGNED:
			case SINGLE:
			case SKIPROWS:
			case START:
			case STARTS:
			case STATUS:
			case STOP:
			case STRING:
			case SUPPORTED:
			case SUSPENDED:
			case TARGET:
			case TERMINATE:
			case TEXT:
			case THEN:
			case TIME:
			case TIMESTAMP:
			case TIMEZONE:
			case TO:
			case TOPOLOGY:
			case TRAILING:
			case TRANSACTION:
			case TRANSACTIONS:
			case TRAVERSE:
			case TRIM:
			case TRUE:
			case TYPE:
			case TYPED:
			case TYPES:
			case UNION:
			case UNIQUE:
			case UNIQUENESS:
			case UNWIND:
			case URL:
			case USE:
			case USER:
			case USERS:
			case USING:
			case VALUE:
			case VARCHAR:
			case VECTOR:
			case VERBOSE:
			case VERTEX:
			case WAIT:
			case WHEN:
			case WHERE:
			case WITH:
			case WITHOUT:
			case WRITE:
			case XOR:
			case YIELD:
			case ZONE:
			case ZONED:
			case IDENTIFIER:
				{
				setState(2473);
				nonEmptyStringList();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(2476);
			match(RCURLY);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class NonEmptyStringListContext extends AstRuleCtx {
		public List<SymbolicNameStringContext> symbolicNameString() {
			return getRuleContexts(SymbolicNameStringContext.class);
		}
		public SymbolicNameStringContext symbolicNameString(int i) {
			return getRuleContext(SymbolicNameStringContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(Cypher5Parser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(Cypher5Parser.COMMA, i);
		}
		public NonEmptyStringListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_nonEmptyStringList; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterNonEmptyStringList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitNonEmptyStringList(this);
		}
	}

	public final NonEmptyStringListContext nonEmptyStringList() throws RecognitionException {
		NonEmptyStringListContext _localctx = new NonEmptyStringListContext(_ctx, getState());
		enterRule(_localctx, 556, RULE_nonEmptyStringList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2478);
			symbolicNameString();
			setState(2483);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(2479);
				match(COMMA);
				setState(2480);
				symbolicNameString();
				}
				}
				setState(2485);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class GraphQualifierContext extends AstRuleCtx {
		public GraphQualifierTokenContext graphQualifierToken() {
			return getRuleContext(GraphQualifierTokenContext.class,0);
		}
		public GraphQualifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_graphQualifier; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterGraphQualifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitGraphQualifier(this);
		}
	}

	public final GraphQualifierContext graphQualifier() throws RecognitionException {
		GraphQualifierContext _localctx = new GraphQualifierContext(_ctx, getState());
		enterRule(_localctx, 558, RULE_graphQualifier);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2487);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==ELEMENT || _la==ELEMENTS || ((((_la - 173)) & ~0x3f) == 0 && ((1L << (_la - 173)) & 6755399441055749L) != 0)) {
				{
				setState(2486);
				graphQualifierToken();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class GraphQualifierTokenContext extends AstRuleCtx {
		public RelTokenContext relToken() {
			return getRuleContext(RelTokenContext.class,0);
		}
		public NodeTokenContext nodeToken() {
			return getRuleContext(NodeTokenContext.class,0);
		}
		public ElementTokenContext elementToken() {
			return getRuleContext(ElementTokenContext.class,0);
		}
		public GraphQualifierTokenContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_graphQualifierToken; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterGraphQualifierToken(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitGraphQualifierToken(this);
		}
	}

	public final GraphQualifierTokenContext graphQualifierToken() throws RecognitionException {
		GraphQualifierTokenContext _localctx = new GraphQualifierTokenContext(_ctx, getState());
		enterRule(_localctx, 560, RULE_graphQualifierToken);
		try {
			setState(2492);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case RELATIONSHIP:
			case RELATIONSHIPS:
				enterOuterAlt(_localctx, 1);
				{
				setState(2489);
				relToken();
				}
				break;
			case NODE:
			case NODES:
				enterOuterAlt(_localctx, 2);
				{
				setState(2490);
				nodeToken();
				}
				break;
			case ELEMENT:
			case ELEMENTS:
				enterOuterAlt(_localctx, 3);
				{
				setState(2491);
				elementToken();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class RelTokenContext extends AstRuleCtx {
		public TerminalNode RELATIONSHIP() { return getToken(Cypher5Parser.RELATIONSHIP, 0); }
		public TerminalNode RELATIONSHIPS() { return getToken(Cypher5Parser.RELATIONSHIPS, 0); }
		public RelTokenContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_relToken; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterRelToken(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitRelToken(this);
		}
	}

	public final RelTokenContext relToken() throws RecognitionException {
		RelTokenContext _localctx = new RelTokenContext(_ctx, getState());
		enterRule(_localctx, 562, RULE_relToken);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2494);
			_la = _input.LA(1);
			if ( !(_la==RELATIONSHIP || _la==RELATIONSHIPS) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ElementTokenContext extends AstRuleCtx {
		public TerminalNode ELEMENT() { return getToken(Cypher5Parser.ELEMENT, 0); }
		public TerminalNode ELEMENTS() { return getToken(Cypher5Parser.ELEMENTS, 0); }
		public ElementTokenContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_elementToken; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterElementToken(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitElementToken(this);
		}
	}

	public final ElementTokenContext elementToken() throws RecognitionException {
		ElementTokenContext _localctx = new ElementTokenContext(_ctx, getState());
		enterRule(_localctx, 564, RULE_elementToken);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2496);
			_la = _input.LA(1);
			if ( !(_la==ELEMENT || _la==ELEMENTS) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class NodeTokenContext extends AstRuleCtx {
		public TerminalNode NODE() { return getToken(Cypher5Parser.NODE, 0); }
		public TerminalNode NODES() { return getToken(Cypher5Parser.NODES, 0); }
		public NodeTokenContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_nodeToken; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterNodeToken(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitNodeToken(this);
		}
	}

	public final NodeTokenContext nodeToken() throws RecognitionException {
		NodeTokenContext _localctx = new NodeTokenContext(_ctx, getState());
		enterRule(_localctx, 566, RULE_nodeToken);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2498);
			_la = _input.LA(1);
			if ( !(_la==NODE || _la==NODES) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class DatabaseScopeContext extends AstRuleCtx {
		public TerminalNode DATABASE() { return getToken(Cypher5Parser.DATABASE, 0); }
		public TerminalNode DATABASES() { return getToken(Cypher5Parser.DATABASES, 0); }
		public DatabaseScopeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_databaseScope; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterDatabaseScope(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitDatabaseScope(this);
		}
	}

	public final DatabaseScopeContext databaseScope() throws RecognitionException {
		DatabaseScopeContext _localctx = new DatabaseScopeContext(_ctx, getState());
		enterRule(_localctx, 568, RULE_databaseScope);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2500);
			_la = _input.LA(1);
			if ( !(_la==DATABASE || _la==DATABASES) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class GraphScopeContext extends AstRuleCtx {
		public TerminalNode GRAPH() { return getToken(Cypher5Parser.GRAPH, 0); }
		public TerminalNode GRAPHS() { return getToken(Cypher5Parser.GRAPHS, 0); }
		public GraphScopeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_graphScope; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterGraphScope(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitGraphScope(this);
		}
	}

	public final GraphScopeContext graphScope() throws RecognitionException {
		GraphScopeContext _localctx = new GraphScopeContext(_ctx, getState());
		enterRule(_localctx, 570, RULE_graphScope);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2502);
			_la = _input.LA(1);
			if ( !(_la==GRAPH || _la==GRAPHS) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class CreateCompositeDatabaseContext extends AstRuleCtx {
		public TerminalNode COMPOSITE() { return getToken(Cypher5Parser.COMPOSITE, 0); }
		public TerminalNode DATABASE() { return getToken(Cypher5Parser.DATABASE, 0); }
		public SymbolicAliasNameOrParameterContext symbolicAliasNameOrParameter() {
			return getRuleContext(SymbolicAliasNameOrParameterContext.class,0);
		}
		public CreateCompositeDatabaseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_createCompositeDatabase; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterCreateCompositeDatabase(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitCreateCompositeDatabase(this);
		}
	}

	public final CreateCompositeDatabaseContext createCompositeDatabase() throws RecognitionException {
		CreateCompositeDatabaseContext _localctx = new CreateCompositeDatabaseContext(_ctx, getState());
		enterRule(_localctx, 572, RULE_createCompositeDatabase);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2504);
			match(COMPOSITE);
			setState(2505);
			match(DATABASE);
			setState(2506);
			symbolicAliasNameOrParameter();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class CreateDatabaseContext extends AstRuleCtx {
		public TerminalNode DATABASE() { return getToken(Cypher5Parser.DATABASE, 0); }
		public SymbolicAliasNameOrParameterContext symbolicAliasNameOrParameter() {
			return getRuleContext(SymbolicAliasNameOrParameterContext.class,0);
		}
		public CreateDatabaseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_createDatabase; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterCreateDatabase(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitCreateDatabase(this);
		}
	}

	public final CreateDatabaseContext createDatabase() throws RecognitionException {
		CreateDatabaseContext _localctx = new CreateDatabaseContext(_ctx, getState());
		enterRule(_localctx, 574, RULE_createDatabase);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2508);
			match(DATABASE);
			setState(2509);
			symbolicAliasNameOrParameter();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class PrimaryTopologyContext extends AstRuleCtx {
		public UIntOrIntParameterContext uIntOrIntParameter() {
			return getRuleContext(UIntOrIntParameterContext.class,0);
		}
		public PrimaryTokenContext primaryToken() {
			return getRuleContext(PrimaryTokenContext.class,0);
		}
		public PrimaryTopologyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_primaryTopology; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterPrimaryTopology(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitPrimaryTopology(this);
		}
	}

	public final PrimaryTopologyContext primaryTopology() throws RecognitionException {
		PrimaryTopologyContext _localctx = new PrimaryTopologyContext(_ctx, getState());
		enterRule(_localctx, 576, RULE_primaryTopology);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2511);
			uIntOrIntParameter();
			setState(2512);
			primaryToken();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class PrimaryTokenContext extends AstRuleCtx {
		public TerminalNode PRIMARY() { return getToken(Cypher5Parser.PRIMARY, 0); }
		public TerminalNode PRIMARIES() { return getToken(Cypher5Parser.PRIMARIES, 0); }
		public PrimaryTokenContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_primaryToken; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterPrimaryToken(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitPrimaryToken(this);
		}
	}

	public final PrimaryTokenContext primaryToken() throws RecognitionException {
		PrimaryTokenContext _localctx = new PrimaryTokenContext(_ctx, getState());
		enterRule(_localctx, 578, RULE_primaryToken);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2514);
			_la = _input.LA(1);
			if ( !(_la==PRIMARY || _la==PRIMARIES) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class SecondaryTopologyContext extends AstRuleCtx {
		public UIntOrIntParameterContext uIntOrIntParameter() {
			return getRuleContext(UIntOrIntParameterContext.class,0);
		}
		public SecondaryTokenContext secondaryToken() {
			return getRuleContext(SecondaryTokenContext.class,0);
		}
		public SecondaryTopologyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_secondaryTopology; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterSecondaryTopology(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitSecondaryTopology(this);
		}
	}

	public final SecondaryTopologyContext secondaryTopology() throws RecognitionException {
		SecondaryTopologyContext _localctx = new SecondaryTopologyContext(_ctx, getState());
		enterRule(_localctx, 580, RULE_secondaryTopology);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2516);
			uIntOrIntParameter();
			setState(2517);
			secondaryToken();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class SecondaryTokenContext extends AstRuleCtx {
		public TerminalNode SECONDARY() { return getToken(Cypher5Parser.SECONDARY, 0); }
		public TerminalNode SECONDARIES() { return getToken(Cypher5Parser.SECONDARIES, 0); }
		public SecondaryTokenContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_secondaryToken; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterSecondaryToken(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitSecondaryToken(this);
		}
	}

	public final SecondaryTokenContext secondaryToken() throws RecognitionException {
		SecondaryTokenContext _localctx = new SecondaryTokenContext(_ctx, getState());
		enterRule(_localctx, 582, RULE_secondaryToken);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2519);
			_la = _input.LA(1);
			if ( !(_la==SECONDARY || _la==SECONDARIES) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class DropDatabaseContext extends AstRuleCtx {
		public TerminalNode DATABASE() { return getToken(Cypher5Parser.DATABASE, 0); }
		public SymbolicAliasNameOrParameterContext symbolicAliasNameOrParameter() {
			return getRuleContext(SymbolicAliasNameOrParameterContext.class,0);
		}
		public DropDatabaseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_dropDatabase; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterDropDatabase(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitDropDatabase(this);
		}
	}

	public final DropDatabaseContext dropDatabase() throws RecognitionException {
		DropDatabaseContext _localctx = new DropDatabaseContext(_ctx, getState());
		enterRule(_localctx, 584, RULE_dropDatabase);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2521);
			match(DATABASE);
			setState(2522);
			symbolicAliasNameOrParameter();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class AliasActionContext extends AstRuleCtx {
		public TerminalNode RESTRICT() { return getToken(Cypher5Parser.RESTRICT, 0); }
		public TerminalNode CASCADE() { return getToken(Cypher5Parser.CASCADE, 0); }
		public TerminalNode ALIAS() { return getToken(Cypher5Parser.ALIAS, 0); }
		public TerminalNode ALIASES() { return getToken(Cypher5Parser.ALIASES, 0); }
		public AliasActionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_aliasAction; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterAliasAction(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitAliasAction(this);
		}
	}

	public final AliasActionContext aliasAction() throws RecognitionException {
		AliasActionContext _localctx = new AliasActionContext(_ctx, getState());
		enterRule(_localctx, 586, RULE_aliasAction);
		try {
			setState(2529);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,237,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(2524);
				match(RESTRICT);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(2525);
				match(CASCADE);
				setState(2526);
				match(ALIAS);
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(2527);
				match(CASCADE);
				setState(2528);
				match(ALIASES);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class AlterDatabaseContext extends AstRuleCtx {
		public TerminalNode DATABASE() { return getToken(Cypher5Parser.DATABASE, 0); }
		public SymbolicAliasNameOrParameterContext symbolicAliasNameOrParameter() {
			return getRuleContext(SymbolicAliasNameOrParameterContext.class,0);
		}
		public AlterDatabaseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_alterDatabase; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterAlterDatabase(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitAlterDatabase(this);
		}
	}

	public final AlterDatabaseContext alterDatabase() throws RecognitionException {
		AlterDatabaseContext _localctx = new AlterDatabaseContext(_ctx, getState());
		enterRule(_localctx, 588, RULE_alterDatabase);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2531);
			match(DATABASE);
			setState(2532);
			symbolicAliasNameOrParameter();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class AlterDatabaseAccessContext extends AstRuleCtx {
		public TerminalNode ACCESS() { return getToken(Cypher5Parser.ACCESS, 0); }
		public TerminalNode READ() { return getToken(Cypher5Parser.READ, 0); }
		public TerminalNode ONLY() { return getToken(Cypher5Parser.ONLY, 0); }
		public TerminalNode WRITE() { return getToken(Cypher5Parser.WRITE, 0); }
		public AlterDatabaseAccessContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_alterDatabaseAccess; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterAlterDatabaseAccess(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitAlterDatabaseAccess(this);
		}
	}

	public final AlterDatabaseAccessContext alterDatabaseAccess() throws RecognitionException {
		AlterDatabaseAccessContext _localctx = new AlterDatabaseAccessContext(_ctx, getState());
		enterRule(_localctx, 590, RULE_alterDatabaseAccess);
		try {
			setState(2540);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,238,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(2534);
				match(ACCESS);
				setState(2535);
				match(READ);
				setState(2536);
				match(ONLY);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(2537);
				match(ACCESS);
				setState(2538);
				match(READ);
				setState(2539);
				match(WRITE);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class AlterDatabaseTopologyContext extends AstRuleCtx {
		public TerminalNode TOPOLOGY() { return getToken(Cypher5Parser.TOPOLOGY, 0); }
		public List<PrimaryTopologyContext> primaryTopology() {
			return getRuleContexts(PrimaryTopologyContext.class);
		}
		public PrimaryTopologyContext primaryTopology(int i) {
			return getRuleContext(PrimaryTopologyContext.class,i);
		}
		public List<SecondaryTopologyContext> secondaryTopology() {
			return getRuleContexts(SecondaryTopologyContext.class);
		}
		public SecondaryTopologyContext secondaryTopology(int i) {
			return getRuleContext(SecondaryTopologyContext.class,i);
		}
		public AlterDatabaseTopologyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_alterDatabaseTopology; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterAlterDatabaseTopology(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitAlterDatabaseTopology(this);
		}
	}

	public final AlterDatabaseTopologyContext alterDatabaseTopology() throws RecognitionException {
		AlterDatabaseTopologyContext _localctx = new AlterDatabaseTopologyContext(_ctx, getState());
		enterRule(_localctx, 592, RULE_alterDatabaseTopology);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2542);
			match(TOPOLOGY);
			setState(2545); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				setState(2545);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,239,_ctx) ) {
				case 1:
					{
					setState(2543);
					primaryTopology();
					}
					break;
				case 2:
					{
					setState(2544);
					secondaryTopology();
					}
					break;
				}
				}
				setState(2547); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==UNSIGNED_DECIMAL_INTEGER || _la==DOLLAR );
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class AlterDatabaseOptionContext extends AstRuleCtx {
		public TerminalNode OPTION() { return getToken(Cypher5Parser.OPTION, 0); }
		public SymbolicNameStringContext symbolicNameString() {
			return getRuleContext(SymbolicNameStringContext.class,0);
		}
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public AlterDatabaseOptionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_alterDatabaseOption; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterAlterDatabaseOption(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitAlterDatabaseOption(this);
		}
	}

	public final AlterDatabaseOptionContext alterDatabaseOption() throws RecognitionException {
		AlterDatabaseOptionContext _localctx = new AlterDatabaseOptionContext(_ctx, getState());
		enterRule(_localctx, 594, RULE_alterDatabaseOption);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2549);
			match(OPTION);
			setState(2550);
			symbolicNameString();
			setState(2551);
			expression();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class StartDatabaseContext extends AstRuleCtx {
		public TerminalNode START() { return getToken(Cypher5Parser.START, 0); }
		public TerminalNode DATABASE() { return getToken(Cypher5Parser.DATABASE, 0); }
		public SymbolicAliasNameOrParameterContext symbolicAliasNameOrParameter() {
			return getRuleContext(SymbolicAliasNameOrParameterContext.class,0);
		}
		public StartDatabaseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_startDatabase; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterStartDatabase(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitStartDatabase(this);
		}
	}

	public final StartDatabaseContext startDatabase() throws RecognitionException {
		StartDatabaseContext _localctx = new StartDatabaseContext(_ctx, getState());
		enterRule(_localctx, 596, RULE_startDatabase);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2553);
			match(START);
			setState(2554);
			match(DATABASE);
			setState(2555);
			symbolicAliasNameOrParameter();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class StopDatabaseContext extends AstRuleCtx {
		public TerminalNode STOP() { return getToken(Cypher5Parser.STOP, 0); }
		public TerminalNode DATABASE() { return getToken(Cypher5Parser.DATABASE, 0); }
		public SymbolicAliasNameOrParameterContext symbolicAliasNameOrParameter() {
			return getRuleContext(SymbolicAliasNameOrParameterContext.class,0);
		}
		public StopDatabaseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_stopDatabase; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterStopDatabase(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitStopDatabase(this);
		}
	}

	public final StopDatabaseContext stopDatabase() throws RecognitionException {
		StopDatabaseContext _localctx = new StopDatabaseContext(_ctx, getState());
		enterRule(_localctx, 598, RULE_stopDatabase);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2557);
			match(STOP);
			setState(2558);
			match(DATABASE);
			setState(2559);
			symbolicAliasNameOrParameter();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class WaitClauseContext extends AstRuleCtx {
		public TerminalNode WAIT() { return getToken(Cypher5Parser.WAIT, 0); }
		public TerminalNode UNSIGNED_DECIMAL_INTEGER() { return getToken(Cypher5Parser.UNSIGNED_DECIMAL_INTEGER, 0); }
		public SecondsTokenContext secondsToken() {
			return getRuleContext(SecondsTokenContext.class,0);
		}
		public TerminalNode NOWAIT() { return getToken(Cypher5Parser.NOWAIT, 0); }
		public WaitClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_waitClause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterWaitClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitWaitClause(this);
		}
	}

	public final WaitClauseContext waitClause() throws RecognitionException {
		WaitClauseContext _localctx = new WaitClauseContext(_ctx, getState());
		enterRule(_localctx, 600, RULE_waitClause);
		int _la;
		try {
			setState(2569);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case WAIT:
				enterOuterAlt(_localctx, 1);
				{
				setState(2561);
				match(WAIT);
				setState(2566);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==UNSIGNED_DECIMAL_INTEGER) {
					{
					setState(2562);
					match(UNSIGNED_DECIMAL_INTEGER);
					setState(2564);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (((((_la - 241)) & ~0x3f) == 0 && ((1L << (_la - 241)) & 19L) != 0)) {
						{
						setState(2563);
						secondsToken();
						}
					}

					}
				}

				}
				break;
			case NOWAIT:
				enterOuterAlt(_localctx, 2);
				{
				setState(2568);
				match(NOWAIT);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class SecondsTokenContext extends AstRuleCtx {
		public TerminalNode SEC() { return getToken(Cypher5Parser.SEC, 0); }
		public TerminalNode SECOND() { return getToken(Cypher5Parser.SECOND, 0); }
		public TerminalNode SECONDS() { return getToken(Cypher5Parser.SECONDS, 0); }
		public SecondsTokenContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_secondsToken; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterSecondsToken(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitSecondsToken(this);
		}
	}

	public final SecondsTokenContext secondsToken() throws RecognitionException {
		SecondsTokenContext _localctx = new SecondsTokenContext(_ctx, getState());
		enterRule(_localctx, 602, RULE_secondsToken);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2571);
			_la = _input.LA(1);
			if ( !(((((_la - 241)) & ~0x3f) == 0 && ((1L << (_la - 241)) & 19L) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ShowDatabaseContext extends AstRuleCtx {
		public TerminalNode DATABASE() { return getToken(Cypher5Parser.DATABASE, 0); }
		public TerminalNode DATABASES() { return getToken(Cypher5Parser.DATABASES, 0); }
		public ShowDatabaseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_showDatabase; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterShowDatabase(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitShowDatabase(this);
		}
	}

	public final ShowDatabaseContext showDatabase() throws RecognitionException {
		ShowDatabaseContext _localctx = new ShowDatabaseContext(_ctx, getState());
		enterRule(_localctx, 604, RULE_showDatabase);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2573);
			_la = _input.LA(1);
			if ( !(_la==DATABASE || _la==DATABASES) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class AliasNameContext extends AstRuleCtx {
		public SymbolicAliasNameOrParameterContext symbolicAliasNameOrParameter() {
			return getRuleContext(SymbolicAliasNameOrParameterContext.class,0);
		}
		public AliasNameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_aliasName; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterAliasName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitAliasName(this);
		}
	}

	public final AliasNameContext aliasName() throws RecognitionException {
		AliasNameContext _localctx = new AliasNameContext(_ctx, getState());
		enterRule(_localctx, 606, RULE_aliasName);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2575);
			symbolicAliasNameOrParameter();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class DatabaseNameContext extends AstRuleCtx {
		public SymbolicAliasNameOrParameterContext symbolicAliasNameOrParameter() {
			return getRuleContext(SymbolicAliasNameOrParameterContext.class,0);
		}
		public DatabaseNameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_databaseName; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterDatabaseName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitDatabaseName(this);
		}
	}

	public final DatabaseNameContext databaseName() throws RecognitionException {
		DatabaseNameContext _localctx = new DatabaseNameContext(_ctx, getState());
		enterRule(_localctx, 608, RULE_databaseName);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2577);
			symbolicAliasNameOrParameter();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class CreateAliasContext extends AstRuleCtx {
		public TerminalNode ALIAS() { return getToken(Cypher5Parser.ALIAS, 0); }
		public AliasNameContext aliasName() {
			return getRuleContext(AliasNameContext.class,0);
		}
		public TerminalNode FOR() { return getToken(Cypher5Parser.FOR, 0); }
		public TerminalNode DATABASE() { return getToken(Cypher5Parser.DATABASE, 0); }
		public DatabaseNameContext databaseName() {
			return getRuleContext(DatabaseNameContext.class,0);
		}
		public CreateAliasContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_createAlias; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterCreateAlias(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitCreateAlias(this);
		}
	}

	public final CreateAliasContext createAlias() throws RecognitionException {
		CreateAliasContext _localctx = new CreateAliasContext(_ctx, getState());
		enterRule(_localctx, 610, RULE_createAlias);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2579);
			match(ALIAS);
			setState(2580);
			aliasName();
			setState(2581);
			match(FOR);
			setState(2582);
			match(DATABASE);
			setState(2583);
			databaseName();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class DropAliasContext extends AstRuleCtx {
		public TerminalNode ALIAS() { return getToken(Cypher5Parser.ALIAS, 0); }
		public AliasNameContext aliasName() {
			return getRuleContext(AliasNameContext.class,0);
		}
		public TerminalNode FOR() { return getToken(Cypher5Parser.FOR, 0); }
		public TerminalNode DATABASE() { return getToken(Cypher5Parser.DATABASE, 0); }
		public DropAliasContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_dropAlias; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterDropAlias(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitDropAlias(this);
		}
	}

	public final DropAliasContext dropAlias() throws RecognitionException {
		DropAliasContext _localctx = new DropAliasContext(_ctx, getState());
		enterRule(_localctx, 612, RULE_dropAlias);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2585);
			match(ALIAS);
			setState(2586);
			aliasName();
			setState(2587);
			match(FOR);
			setState(2588);
			match(DATABASE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class AlterAliasContext extends AstRuleCtx {
		public TerminalNode ALIAS() { return getToken(Cypher5Parser.ALIAS, 0); }
		public AliasNameContext aliasName() {
			return getRuleContext(AliasNameContext.class,0);
		}
		public TerminalNode SET() { return getToken(Cypher5Parser.SET, 0); }
		public TerminalNode DATABASE() { return getToken(Cypher5Parser.DATABASE, 0); }
		public AlterAliasTargetContext alterAliasTarget() {
			return getRuleContext(AlterAliasTargetContext.class,0);
		}
		public AlterAliasContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_alterAlias; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterAlterAlias(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitAlterAlias(this);
		}
	}

	public final AlterAliasContext alterAlias() throws RecognitionException {
		AlterAliasContext _localctx = new AlterAliasContext(_ctx, getState());
		enterRule(_localctx, 614, RULE_alterAlias);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2590);
			match(ALIAS);
			setState(2591);
			aliasName();
			setState(2592);
			match(SET);
			setState(2593);
			match(DATABASE);
			setState(2594);
			alterAliasTarget();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class AlterAliasTargetContext extends AstRuleCtx {
		public TerminalNode TARGET() { return getToken(Cypher5Parser.TARGET, 0); }
		public DatabaseNameContext databaseName() {
			return getRuleContext(DatabaseNameContext.class,0);
		}
		public AlterAliasTargetContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_alterAliasTarget; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterAlterAliasTarget(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitAlterAliasTarget(this);
		}
	}

	public final AlterAliasTargetContext alterAliasTarget() throws RecognitionException {
		AlterAliasTargetContext _localctx = new AlterAliasTargetContext(_ctx, getState());
		enterRule(_localctx, 616, RULE_alterAliasTarget);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2596);
			match(TARGET);
			setState(2597);
			databaseName();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class AlterAliasUserContext extends AstRuleCtx {
		public TerminalNode USER() { return getToken(Cypher5Parser.USER, 0); }
		public CommandNameExpressionContext commandNameExpression() {
			return getRuleContext(CommandNameExpressionContext.class,0);
		}
		public AlterAliasUserContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_alterAliasUser; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterAlterAliasUser(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitAlterAliasUser(this);
		}
	}

	public final AlterAliasUserContext alterAliasUser() throws RecognitionException {
		AlterAliasUserContext _localctx = new AlterAliasUserContext(_ctx, getState());
		enterRule(_localctx, 618, RULE_alterAliasUser);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2599);
			match(USER);
			setState(2600);
			commandNameExpression();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class AlterAliasPasswordContext extends AstRuleCtx {
		public TerminalNode PASSWORD() { return getToken(Cypher5Parser.PASSWORD, 0); }
		public PasswordExpressionContext passwordExpression() {
			return getRuleContext(PasswordExpressionContext.class,0);
		}
		public AlterAliasPasswordContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_alterAliasPassword; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterAlterAliasPassword(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitAlterAliasPassword(this);
		}
	}

	public final AlterAliasPasswordContext alterAliasPassword() throws RecognitionException {
		AlterAliasPasswordContext _localctx = new AlterAliasPasswordContext(_ctx, getState());
		enterRule(_localctx, 620, RULE_alterAliasPassword);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2602);
			match(PASSWORD);
			setState(2603);
			passwordExpression();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class AlterAliasDriverContext extends AstRuleCtx {
		public TerminalNode DRIVER() { return getToken(Cypher5Parser.DRIVER, 0); }
		public MapOrParameterContext mapOrParameter() {
			return getRuleContext(MapOrParameterContext.class,0);
		}
		public AlterAliasDriverContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_alterAliasDriver; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterAlterAliasDriver(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitAlterAliasDriver(this);
		}
	}

	public final AlterAliasDriverContext alterAliasDriver() throws RecognitionException {
		AlterAliasDriverContext _localctx = new AlterAliasDriverContext(_ctx, getState());
		enterRule(_localctx, 622, RULE_alterAliasDriver);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2605);
			match(DRIVER);
			setState(2606);
			mapOrParameter();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class AlterAliasPropertiesContext extends AstRuleCtx {
		public TerminalNode PROPERTIES() { return getToken(Cypher5Parser.PROPERTIES, 0); }
		public MapOrParameterContext mapOrParameter() {
			return getRuleContext(MapOrParameterContext.class,0);
		}
		public AlterAliasPropertiesContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_alterAliasProperties; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterAlterAliasProperties(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitAlterAliasProperties(this);
		}
	}

	public final AlterAliasPropertiesContext alterAliasProperties() throws RecognitionException {
		AlterAliasPropertiesContext _localctx = new AlterAliasPropertiesContext(_ctx, getState());
		enterRule(_localctx, 624, RULE_alterAliasProperties);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2608);
			match(PROPERTIES);
			setState(2609);
			mapOrParameter();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ShowAliasesContext extends AstRuleCtx {
		public TerminalNode ALIAS() { return getToken(Cypher5Parser.ALIAS, 0); }
		public TerminalNode ALIASES() { return getToken(Cypher5Parser.ALIASES, 0); }
		public ShowAliasesContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_showAliases; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterShowAliases(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitShowAliases(this);
		}
	}

	public final ShowAliasesContext showAliases() throws RecognitionException {
		ShowAliasesContext _localctx = new ShowAliasesContext(_ctx, getState());
		enterRule(_localctx, 626, RULE_showAliases);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2611);
			_la = _input.LA(1);
			if ( !(_la==ALIAS || _la==ALIASES) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class SymbolicNameOrStringParameterContext extends AstRuleCtx {
		public SymbolicNameStringContext symbolicNameString() {
			return getRuleContext(SymbolicNameStringContext.class,0);
		}
		public ParameterContext parameter() {
			return getRuleContext(ParameterContext.class,0);
		}
		public SymbolicNameOrStringParameterContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_symbolicNameOrStringParameter; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterSymbolicNameOrStringParameter(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitSymbolicNameOrStringParameter(this);
		}
	}

	public final SymbolicNameOrStringParameterContext symbolicNameOrStringParameter() throws RecognitionException {
		SymbolicNameOrStringParameterContext _localctx = new SymbolicNameOrStringParameterContext(_ctx, getState());
		enterRule(_localctx, 628, RULE_symbolicNameOrStringParameter);
		try {
			setState(2615);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case ESCAPED_SYMBOLIC_NAME:
			case ACCESS:
			case ACTIVE:
			case ADMIN:
			case ADMINISTRATOR:
			case ALIAS:
			case ALIASES:
			case ALL_SHORTEST_PATHS:
			case ALL:
			case ALTER:
			case AND:
			case ANY:
			case ARRAY:
			case AS:
			case ASC:
			case ASCENDING:
			case ASSERT:
			case ASSIGN:
			case AT:
			case AUTH:
			case BINDINGS:
			case BOOL:
			case BOOLEAN:
			case BOOSTED:
			case BOTH:
			case BREAK:
			case BRIEF:
			case BTREE:
			case BUILT:
			case BY:
			case CALL:
			case CASCADE:
			case CASE:
			case CHANGE:
			case CIDR:
			case COLLECT:
			case COMMAND:
			case COMMANDS:
			case COMMIT:
			case COMPOSITE:
			case CONCURRENT:
			case CONSTRAINT:
			case CONSTRAINTS:
			case CONTAINS:
			case COPY:
			case CONTINUE:
			case COUNT:
			case CREATE:
			case CSV:
			case CURRENT:
			case DATA:
			case DATABASE:
			case DATABASES:
			case DATE:
			case DATETIME:
			case DBMS:
			case DEALLOCATE:
			case DEFAULT:
			case DEFINED:
			case DELETE:
			case DENY:
			case DESC:
			case DESCENDING:
			case DESTROY:
			case DETACH:
			case DIFFERENT:
			case DISTINCT:
			case DRIVER:
			case DROP:
			case DRYRUN:
			case DUMP:
			case DURATION:
			case EACH:
			case EDGE:
			case ENABLE:
			case ELEMENT:
			case ELEMENTS:
			case ELSE:
			case ENCRYPTED:
			case END:
			case ENDS:
			case EXECUTABLE:
			case EXECUTE:
			case EXIST:
			case EXISTENCE:
			case EXISTS:
			case ERROR:
			case FAIL:
			case FALSE:
			case FIELDTERMINATOR:
			case FINISH:
			case FLOAT:
			case FOR:
			case FOREACH:
			case FROM:
			case FULLTEXT:
			case FUNCTION:
			case FUNCTIONS:
			case GRANT:
			case GRAPH:
			case GRAPHS:
			case GROUP:
			case GROUPS:
			case HEADERS:
			case HOME:
			case ID:
			case IF:
			case IMPERSONATE:
			case IMMUTABLE:
			case IN:
			case INDEX:
			case INDEXES:
			case INF:
			case INFINITY:
			case INSERT:
			case INT:
			case INTEGER:
			case IS:
			case JOIN:
			case KEY:
			case LABEL:
			case LABELS:
			case LEADING:
			case LIMITROWS:
			case LIST:
			case LOAD:
			case LOCAL:
			case LOOKUP:
			case MANAGEMENT:
			case MAP:
			case MATCH:
			case MERGE:
			case NAME:
			case NAMES:
			case NAN:
			case NFC:
			case NFD:
			case NFKC:
			case NFKD:
			case NEW:
			case NODE:
			case NODETACH:
			case NODES:
			case NONE:
			case NORMALIZE:
			case NORMALIZED:
			case NOT:
			case NOTHING:
			case NOWAIT:
			case NULL:
			case OF:
			case OFFSET:
			case ON:
			case ONLY:
			case OPTIONAL:
			case OPTIONS:
			case OPTION:
			case OR:
			case ORDER:
			case OUTPUT:
			case PASSWORD:
			case PASSWORDS:
			case PATH:
			case PATHS:
			case PERIODIC:
			case PLAINTEXT:
			case POINT:
			case POPULATED:
			case PRIMARY:
			case PRIMARIES:
			case PRIVILEGE:
			case PRIVILEGES:
			case PROCEDURE:
			case PROCEDURES:
			case PROPERTIES:
			case PROPERTY:
			case PROVIDER:
			case PROVIDERS:
			case RANGE:
			case READ:
			case REALLOCATE:
			case REDUCE:
			case RENAME:
			case REL:
			case RELATIONSHIP:
			case RELATIONSHIPS:
			case REMOVE:
			case REPEATABLE:
			case REPLACE:
			case REPORT:
			case REQUIRE:
			case REQUIRED:
			case RESTRICT:
			case RETURN:
			case REVOKE:
			case ROLE:
			case ROLES:
			case ROW:
			case ROWS:
			case SCAN:
			case SEC:
			case SECOND:
			case SECONDARY:
			case SECONDARIES:
			case SECONDS:
			case SEEK:
			case SERVER:
			case SERVERS:
			case SET:
			case SETTING:
			case SETTINGS:
			case SHORTEST_PATH:
			case SHORTEST:
			case SHOW:
			case SIGNED:
			case SINGLE:
			case SKIPROWS:
			case START:
			case STARTS:
			case STATUS:
			case STOP:
			case STRING:
			case SUPPORTED:
			case SUSPENDED:
			case TARGET:
			case TERMINATE:
			case TEXT:
			case THEN:
			case TIME:
			case TIMESTAMP:
			case TIMEZONE:
			case TO:
			case TOPOLOGY:
			case TRAILING:
			case TRANSACTION:
			case TRANSACTIONS:
			case TRAVERSE:
			case TRIM:
			case TRUE:
			case TYPE:
			case TYPED:
			case TYPES:
			case UNION:
			case UNIQUE:
			case UNIQUENESS:
			case UNWIND:
			case URL:
			case USE:
			case USER:
			case USERS:
			case USING:
			case VALUE:
			case VARCHAR:
			case VECTOR:
			case VERBOSE:
			case VERTEX:
			case WAIT:
			case WHEN:
			case WHERE:
			case WITH:
			case WITHOUT:
			case WRITE:
			case XOR:
			case YIELD:
			case ZONE:
			case ZONED:
			case IDENTIFIER:
				enterOuterAlt(_localctx, 1);
				{
				setState(2613);
				symbolicNameString();
				}
				break;
			case DOLLAR:
				enterOuterAlt(_localctx, 2);
				{
				setState(2614);
				parameter("STRING");
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class CommandNameExpressionContext extends AstRuleCtx {
		public SymbolicNameStringContext symbolicNameString() {
			return getRuleContext(SymbolicNameStringContext.class,0);
		}
		public ParameterContext parameter() {
			return getRuleContext(ParameterContext.class,0);
		}
		public CommandNameExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_commandNameExpression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterCommandNameExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitCommandNameExpression(this);
		}
	}

	public final CommandNameExpressionContext commandNameExpression() throws RecognitionException {
		CommandNameExpressionContext _localctx = new CommandNameExpressionContext(_ctx, getState());
		enterRule(_localctx, 630, RULE_commandNameExpression);
		try {
			setState(2619);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case ESCAPED_SYMBOLIC_NAME:
			case ACCESS:
			case ACTIVE:
			case ADMIN:
			case ADMINISTRATOR:
			case ALIAS:
			case ALIASES:
			case ALL_SHORTEST_PATHS:
			case ALL:
			case ALTER:
			case AND:
			case ANY:
			case ARRAY:
			case AS:
			case ASC:
			case ASCENDING:
			case ASSERT:
			case ASSIGN:
			case AT:
			case AUTH:
			case BINDINGS:
			case BOOL:
			case BOOLEAN:
			case BOOSTED:
			case BOTH:
			case BREAK:
			case BRIEF:
			case BTREE:
			case BUILT:
			case BY:
			case CALL:
			case CASCADE:
			case CASE:
			case CHANGE:
			case CIDR:
			case COLLECT:
			case COMMAND:
			case COMMANDS:
			case COMMIT:
			case COMPOSITE:
			case CONCURRENT:
			case CONSTRAINT:
			case CONSTRAINTS:
			case CONTAINS:
			case COPY:
			case CONTINUE:
			case COUNT:
			case CREATE:
			case CSV:
			case CURRENT:
			case DATA:
			case DATABASE:
			case DATABASES:
			case DATE:
			case DATETIME:
			case DBMS:
			case DEALLOCATE:
			case DEFAULT:
			case DEFINED:
			case DELETE:
			case DENY:
			case DESC:
			case DESCENDING:
			case DESTROY:
			case DETACH:
			case DIFFERENT:
			case DISTINCT:
			case DRIVER:
			case DROP:
			case DRYRUN:
			case DUMP:
			case DURATION:
			case EACH:
			case EDGE:
			case ENABLE:
			case ELEMENT:
			case ELEMENTS:
			case ELSE:
			case ENCRYPTED:
			case END:
			case ENDS:
			case EXECUTABLE:
			case EXECUTE:
			case EXIST:
			case EXISTENCE:
			case EXISTS:
			case ERROR:
			case FAIL:
			case FALSE:
			case FIELDTERMINATOR:
			case FINISH:
			case FLOAT:
			case FOR:
			case FOREACH:
			case FROM:
			case FULLTEXT:
			case FUNCTION:
			case FUNCTIONS:
			case GRANT:
			case GRAPH:
			case GRAPHS:
			case GROUP:
			case GROUPS:
			case HEADERS:
			case HOME:
			case ID:
			case IF:
			case IMPERSONATE:
			case IMMUTABLE:
			case IN:
			case INDEX:
			case INDEXES:
			case INF:
			case INFINITY:
			case INSERT:
			case INT:
			case INTEGER:
			case IS:
			case JOIN:
			case KEY:
			case LABEL:
			case LABELS:
			case LEADING:
			case LIMITROWS:
			case LIST:
			case LOAD:
			case LOCAL:
			case LOOKUP:
			case MANAGEMENT:
			case MAP:
			case MATCH:
			case MERGE:
			case NAME:
			case NAMES:
			case NAN:
			case NFC:
			case NFD:
			case NFKC:
			case NFKD:
			case NEW:
			case NODE:
			case NODETACH:
			case NODES:
			case NONE:
			case NORMALIZE:
			case NORMALIZED:
			case NOT:
			case NOTHING:
			case NOWAIT:
			case NULL:
			case OF:
			case OFFSET:
			case ON:
			case ONLY:
			case OPTIONAL:
			case OPTIONS:
			case OPTION:
			case OR:
			case ORDER:
			case OUTPUT:
			case PASSWORD:
			case PASSWORDS:
			case PATH:
			case PATHS:
			case PERIODIC:
			case PLAINTEXT:
			case POINT:
			case POPULATED:
			case PRIMARY:
			case PRIMARIES:
			case PRIVILEGE:
			case PRIVILEGES:
			case PROCEDURE:
			case PROCEDURES:
			case PROPERTIES:
			case PROPERTY:
			case PROVIDER:
			case PROVIDERS:
			case RANGE:
			case READ:
			case REALLOCATE:
			case REDUCE:
			case RENAME:
			case REL:
			case RELATIONSHIP:
			case RELATIONSHIPS:
			case REMOVE:
			case REPEATABLE:
			case REPLACE:
			case REPORT:
			case REQUIRE:
			case REQUIRED:
			case RESTRICT:
			case RETURN:
			case REVOKE:
			case ROLE:
			case ROLES:
			case ROW:
			case ROWS:
			case SCAN:
			case SEC:
			case SECOND:
			case SECONDARY:
			case SECONDARIES:
			case SECONDS:
			case SEEK:
			case SERVER:
			case SERVERS:
			case SET:
			case SETTING:
			case SETTINGS:
			case SHORTEST_PATH:
			case SHORTEST:
			case SHOW:
			case SIGNED:
			case SINGLE:
			case SKIPROWS:
			case START:
			case STARTS:
			case STATUS:
			case STOP:
			case STRING:
			case SUPPORTED:
			case SUSPENDED:
			case TARGET:
			case TERMINATE:
			case TEXT:
			case THEN:
			case TIME:
			case TIMESTAMP:
			case TIMEZONE:
			case TO:
			case TOPOLOGY:
			case TRAILING:
			case TRANSACTION:
			case TRANSACTIONS:
			case TRAVERSE:
			case TRIM:
			case TRUE:
			case TYPE:
			case TYPED:
			case TYPES:
			case UNION:
			case UNIQUE:
			case UNIQUENESS:
			case UNWIND:
			case URL:
			case USE:
			case USER:
			case USERS:
			case USING:
			case VALUE:
			case VARCHAR:
			case VECTOR:
			case VERBOSE:
			case VERTEX:
			case WAIT:
			case WHEN:
			case WHERE:
			case WITH:
			case WITHOUT:
			case WRITE:
			case XOR:
			case YIELD:
			case ZONE:
			case ZONED:
			case IDENTIFIER:
				enterOuterAlt(_localctx, 1);
				{
				setState(2617);
				symbolicNameString();
				}
				break;
			case DOLLAR:
				enterOuterAlt(_localctx, 2);
				{
				setState(2618);
				parameter("STRING");
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class SymbolicNameOrStringParameterListContext extends AstRuleCtx {
		public List<CommandNameExpressionContext> commandNameExpression() {
			return getRuleContexts(CommandNameExpressionContext.class);
		}
		public CommandNameExpressionContext commandNameExpression(int i) {
			return getRuleContext(CommandNameExpressionContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(Cypher5Parser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(Cypher5Parser.COMMA, i);
		}
		public SymbolicNameOrStringParameterListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_symbolicNameOrStringParameterList; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterSymbolicNameOrStringParameterList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitSymbolicNameOrStringParameterList(this);
		}
	}

	public final SymbolicNameOrStringParameterListContext symbolicNameOrStringParameterList() throws RecognitionException {
		SymbolicNameOrStringParameterListContext _localctx = new SymbolicNameOrStringParameterListContext(_ctx, getState());
		enterRule(_localctx, 632, RULE_symbolicNameOrStringParameterList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2621);
			commandNameExpression();
			setState(2626);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(2622);
				match(COMMA);
				setState(2623);
				commandNameExpression();
				}
				}
				setState(2628);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class SymbolicAliasNameListContext extends AstRuleCtx {
		public List<SymbolicAliasNameOrParameterContext> symbolicAliasNameOrParameter() {
			return getRuleContexts(SymbolicAliasNameOrParameterContext.class);
		}
		public SymbolicAliasNameOrParameterContext symbolicAliasNameOrParameter(int i) {
			return getRuleContext(SymbolicAliasNameOrParameterContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(Cypher5Parser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(Cypher5Parser.COMMA, i);
		}
		public SymbolicAliasNameListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_symbolicAliasNameList; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterSymbolicAliasNameList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitSymbolicAliasNameList(this);
		}
	}

	public final SymbolicAliasNameListContext symbolicAliasNameList() throws RecognitionException {
		SymbolicAliasNameListContext _localctx = new SymbolicAliasNameListContext(_ctx, getState());
		enterRule(_localctx, 634, RULE_symbolicAliasNameList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2629);
			symbolicAliasNameOrParameter();
			setState(2634);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(2630);
				match(COMMA);
				setState(2631);
				symbolicAliasNameOrParameter();
				}
				}
				setState(2636);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class SymbolicAliasNameOrParameterContext extends AstRuleCtx {
		public SymbolicAliasNameContext symbolicAliasName() {
			return getRuleContext(SymbolicAliasNameContext.class,0);
		}
		public ParameterContext parameter() {
			return getRuleContext(ParameterContext.class,0);
		}
		public SymbolicAliasNameOrParameterContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_symbolicAliasNameOrParameter; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterSymbolicAliasNameOrParameter(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitSymbolicAliasNameOrParameter(this);
		}
	}

	public final SymbolicAliasNameOrParameterContext symbolicAliasNameOrParameter() throws RecognitionException {
		SymbolicAliasNameOrParameterContext _localctx = new SymbolicAliasNameOrParameterContext(_ctx, getState());
		enterRule(_localctx, 636, RULE_symbolicAliasNameOrParameter);
		try {
			setState(2639);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case ESCAPED_SYMBOLIC_NAME:
			case ACCESS:
			case ACTIVE:
			case ADMIN:
			case ADMINISTRATOR:
			case ALIAS:
			case ALIASES:
			case ALL_SHORTEST_PATHS:
			case ALL:
			case ALTER:
			case AND:
			case ANY:
			case ARRAY:
			case AS:
			case ASC:
			case ASCENDING:
			case ASSERT:
			case ASSIGN:
			case AT:
			case AUTH:
			case BINDINGS:
			case BOOL:
			case BOOLEAN:
			case BOOSTED:
			case BOTH:
			case BREAK:
			case BRIEF:
			case BTREE:
			case BUILT:
			case BY:
			case CALL:
			case CASCADE:
			case CASE:
			case CHANGE:
			case CIDR:
			case COLLECT:
			case COMMAND:
			case COMMANDS:
			case COMMIT:
			case COMPOSITE:
			case CONCURRENT:
			case CONSTRAINT:
			case CONSTRAINTS:
			case CONTAINS:
			case COPY:
			case CONTINUE:
			case COUNT:
			case CREATE:
			case CSV:
			case CURRENT:
			case DATA:
			case DATABASE:
			case DATABASES:
			case DATE:
			case DATETIME:
			case DBMS:
			case DEALLOCATE:
			case DEFAULT:
			case DEFINED:
			case DELETE:
			case DENY:
			case DESC:
			case DESCENDING:
			case DESTROY:
			case DETACH:
			case DIFFERENT:
			case DISTINCT:
			case DRIVER:
			case DROP:
			case DRYRUN:
			case DUMP:
			case DURATION:
			case EACH:
			case EDGE:
			case ENABLE:
			case ELEMENT:
			case ELEMENTS:
			case ELSE:
			case ENCRYPTED:
			case END:
			case ENDS:
			case EXECUTABLE:
			case EXECUTE:
			case EXIST:
			case EXISTENCE:
			case EXISTS:
			case ERROR:
			case FAIL:
			case FALSE:
			case FIELDTERMINATOR:
			case FINISH:
			case FLOAT:
			case FOR:
			case FOREACH:
			case FROM:
			case FULLTEXT:
			case FUNCTION:
			case FUNCTIONS:
			case GRANT:
			case GRAPH:
			case GRAPHS:
			case GROUP:
			case GROUPS:
			case HEADERS:
			case HOME:
			case ID:
			case IF:
			case IMPERSONATE:
			case IMMUTABLE:
			case IN:
			case INDEX:
			case INDEXES:
			case INF:
			case INFINITY:
			case INSERT:
			case INT:
			case INTEGER:
			case IS:
			case JOIN:
			case KEY:
			case LABEL:
			case LABELS:
			case LEADING:
			case LIMITROWS:
			case LIST:
			case LOAD:
			case LOCAL:
			case LOOKUP:
			case MANAGEMENT:
			case MAP:
			case MATCH:
			case MERGE:
			case NAME:
			case NAMES:
			case NAN:
			case NFC:
			case NFD:
			case NFKC:
			case NFKD:
			case NEW:
			case NODE:
			case NODETACH:
			case NODES:
			case NONE:
			case NORMALIZE:
			case NORMALIZED:
			case NOT:
			case NOTHING:
			case NOWAIT:
			case NULL:
			case OF:
			case OFFSET:
			case ON:
			case ONLY:
			case OPTIONAL:
			case OPTIONS:
			case OPTION:
			case OR:
			case ORDER:
			case OUTPUT:
			case PASSWORD:
			case PASSWORDS:
			case PATH:
			case PATHS:
			case PERIODIC:
			case PLAINTEXT:
			case POINT:
			case POPULATED:
			case PRIMARY:
			case PRIMARIES:
			case PRIVILEGE:
			case PRIVILEGES:
			case PROCEDURE:
			case PROCEDURES:
			case PROPERTIES:
			case PROPERTY:
			case PROVIDER:
			case PROVIDERS:
			case RANGE:
			case READ:
			case REALLOCATE:
			case REDUCE:
			case RENAME:
			case REL:
			case RELATIONSHIP:
			case RELATIONSHIPS:
			case REMOVE:
			case REPEATABLE:
			case REPLACE:
			case REPORT:
			case REQUIRE:
			case REQUIRED:
			case RESTRICT:
			case RETURN:
			case REVOKE:
			case ROLE:
			case ROLES:
			case ROW:
			case ROWS:
			case SCAN:
			case SEC:
			case SECOND:
			case SECONDARY:
			case SECONDARIES:
			case SECONDS:
			case SEEK:
			case SERVER:
			case SERVERS:
			case SET:
			case SETTING:
			case SETTINGS:
			case SHORTEST_PATH:
			case SHORTEST:
			case SHOW:
			case SIGNED:
			case SINGLE:
			case SKIPROWS:
			case START:
			case STARTS:
			case STATUS:
			case STOP:
			case STRING:
			case SUPPORTED:
			case SUSPENDED:
			case TARGET:
			case TERMINATE:
			case TEXT:
			case THEN:
			case TIME:
			case TIMESTAMP:
			case TIMEZONE:
			case TO:
			case TOPOLOGY:
			case TRAILING:
			case TRANSACTION:
			case TRANSACTIONS:
			case TRAVERSE:
			case TRIM:
			case TRUE:
			case TYPE:
			case TYPED:
			case TYPES:
			case UNION:
			case UNIQUE:
			case UNIQUENESS:
			case UNWIND:
			case URL:
			case USE:
			case USER:
			case USERS:
			case USING:
			case VALUE:
			case VARCHAR:
			case VECTOR:
			case VERBOSE:
			case VERTEX:
			case WAIT:
			case WHEN:
			case WHERE:
			case WITH:
			case WITHOUT:
			case WRITE:
			case XOR:
			case YIELD:
			case ZONE:
			case ZONED:
			case IDENTIFIER:
				enterOuterAlt(_localctx, 1);
				{
				setState(2637);
				symbolicAliasName();
				}
				break;
			case DOLLAR:
				enterOuterAlt(_localctx, 2);
				{
				setState(2638);
				parameter("STRING");
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class SymbolicAliasNameContext extends AstRuleCtx {
		public List<SymbolicNameStringContext> symbolicNameString() {
			return getRuleContexts(SymbolicNameStringContext.class);
		}
		public SymbolicNameStringContext symbolicNameString(int i) {
			return getRuleContext(SymbolicNameStringContext.class,i);
		}
		public List<TerminalNode> DOT() { return getTokens(Cypher5Parser.DOT); }
		public TerminalNode DOT(int i) {
			return getToken(Cypher5Parser.DOT, i);
		}
		public SymbolicAliasNameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_symbolicAliasName; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterSymbolicAliasName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitSymbolicAliasName(this);
		}
	}

	public final SymbolicAliasNameContext symbolicAliasName() throws RecognitionException {
		SymbolicAliasNameContext _localctx = new SymbolicAliasNameContext(_ctx, getState());
		enterRule(_localctx, 638, RULE_symbolicAliasName);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2641);
			symbolicNameString();
			setState(2646);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==DOT) {
				{
				{
				setState(2642);
				match(DOT);
				setState(2643);
				symbolicNameString();
				}
				}
				setState(2648);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class StringListLiteralContext extends AstRuleCtx {
		public TerminalNode LBRACKET() { return getToken(Cypher5Parser.LBRACKET, 0); }
		public TerminalNode RBRACKET() { return getToken(Cypher5Parser.RBRACKET, 0); }
		public List<StringLiteralContext> stringLiteral() {
			return getRuleContexts(StringLiteralContext.class);
		}
		public StringLiteralContext stringLiteral(int i) {
			return getRuleContext(StringLiteralContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(Cypher5Parser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(Cypher5Parser.COMMA, i);
		}
		public StringListLiteralContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_stringListLiteral; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterStringListLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitStringListLiteral(this);
		}
	}

	public final StringListLiteralContext stringListLiteral() throws RecognitionException {
		StringListLiteralContext _localctx = new StringListLiteralContext(_ctx, getState());
		enterRule(_localctx, 640, RULE_stringListLiteral);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2649);
			match(LBRACKET);
			setState(2658);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==STRING_LITERAL1 || _la==STRING_LITERAL2) {
				{
				setState(2650);
				stringLiteral();
				setState(2655);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(2651);
					match(COMMA);
					setState(2652);
					stringLiteral();
					}
					}
					setState(2657);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
			}

			setState(2660);
			match(RBRACKET);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class StringListContext extends AstRuleCtx {
		public List<StringLiteralContext> stringLiteral() {
			return getRuleContexts(StringLiteralContext.class);
		}
		public StringLiteralContext stringLiteral(int i) {
			return getRuleContext(StringLiteralContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(Cypher5Parser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(Cypher5Parser.COMMA, i);
		}
		public StringListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_stringList; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterStringList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitStringList(this);
		}
	}

	public final StringListContext stringList() throws RecognitionException {
		StringListContext _localctx = new StringListContext(_ctx, getState());
		enterRule(_localctx, 642, RULE_stringList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2662);
			stringLiteral();
			setState(2665); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(2663);
				match(COMMA);
				setState(2664);
				stringLiteral();
				}
				}
				setState(2667); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==COMMA );
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class StringLiteralContext extends AstRuleCtx {
		public TerminalNode STRING_LITERAL1() { return getToken(Cypher5Parser.STRING_LITERAL1, 0); }
		public TerminalNode STRING_LITERAL2() { return getToken(Cypher5Parser.STRING_LITERAL2, 0); }
		public StringLiteralContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_stringLiteral; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterStringLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitStringLiteral(this);
		}
	}

	public final StringLiteralContext stringLiteral() throws RecognitionException {
		StringLiteralContext _localctx = new StringLiteralContext(_ctx, getState());
		enterRule(_localctx, 644, RULE_stringLiteral);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2669);
			_la = _input.LA(1);
			if ( !(_la==STRING_LITERAL1 || _la==STRING_LITERAL2) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class StringOrParameterExpressionContext extends AstRuleCtx {
		public StringLiteralContext stringLiteral() {
			return getRuleContext(StringLiteralContext.class,0);
		}
		public ParameterContext parameter() {
			return getRuleContext(ParameterContext.class,0);
		}
		public StringOrParameterExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_stringOrParameterExpression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterStringOrParameterExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitStringOrParameterExpression(this);
		}
	}

	public final StringOrParameterExpressionContext stringOrParameterExpression() throws RecognitionException {
		StringOrParameterExpressionContext _localctx = new StringOrParameterExpressionContext(_ctx, getState());
		enterRule(_localctx, 646, RULE_stringOrParameterExpression);
		try {
			setState(2673);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case STRING_LITERAL1:
			case STRING_LITERAL2:
				enterOuterAlt(_localctx, 1);
				{
				setState(2671);
				stringLiteral();
				}
				break;
			case DOLLAR:
				enterOuterAlt(_localctx, 2);
				{
				setState(2672);
				parameter("STRING");
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class StringOrParameterContext extends AstRuleCtx {
		public StringLiteralContext stringLiteral() {
			return getRuleContext(StringLiteralContext.class,0);
		}
		public ParameterContext parameter() {
			return getRuleContext(ParameterContext.class,0);
		}
		public StringOrParameterContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_stringOrParameter; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterStringOrParameter(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitStringOrParameter(this);
		}
	}

	public final StringOrParameterContext stringOrParameter() throws RecognitionException {
		StringOrParameterContext _localctx = new StringOrParameterContext(_ctx, getState());
		enterRule(_localctx, 648, RULE_stringOrParameter);
		try {
			setState(2677);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case STRING_LITERAL1:
			case STRING_LITERAL2:
				enterOuterAlt(_localctx, 1);
				{
				setState(2675);
				stringLiteral();
				}
				break;
			case DOLLAR:
				enterOuterAlt(_localctx, 2);
				{
				setState(2676);
				parameter("STRING");
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class UIntOrIntParameterContext extends AstRuleCtx {
		public TerminalNode UNSIGNED_DECIMAL_INTEGER() { return getToken(Cypher5Parser.UNSIGNED_DECIMAL_INTEGER, 0); }
		public ParameterContext parameter() {
			return getRuleContext(ParameterContext.class,0);
		}
		public UIntOrIntParameterContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_uIntOrIntParameter; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterUIntOrIntParameter(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitUIntOrIntParameter(this);
		}
	}

	public final UIntOrIntParameterContext uIntOrIntParameter() throws RecognitionException {
		UIntOrIntParameterContext _localctx = new UIntOrIntParameterContext(_ctx, getState());
		enterRule(_localctx, 650, RULE_uIntOrIntParameter);
		try {
			setState(2681);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case UNSIGNED_DECIMAL_INTEGER:
				enterOuterAlt(_localctx, 1);
				{
				setState(2679);
				match(UNSIGNED_DECIMAL_INTEGER);
				}
				break;
			case DOLLAR:
				enterOuterAlt(_localctx, 2);
				{
				setState(2680);
				parameter("INTEGER");
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class MapOrParameterContext extends AstRuleCtx {
		public MapContext map() {
			return getRuleContext(MapContext.class,0);
		}
		public ParameterContext parameter() {
			return getRuleContext(ParameterContext.class,0);
		}
		public MapOrParameterContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_mapOrParameter; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterMapOrParameter(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitMapOrParameter(this);
		}
	}

	public final MapOrParameterContext mapOrParameter() throws RecognitionException {
		MapOrParameterContext _localctx = new MapOrParameterContext(_ctx, getState());
		enterRule(_localctx, 652, RULE_mapOrParameter);
		try {
			setState(2685);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case LCURLY:
				enterOuterAlt(_localctx, 1);
				{
				setState(2683);
				map();
				}
				break;
			case DOLLAR:
				enterOuterAlt(_localctx, 2);
				{
				setState(2684);
				parameter("MAP");
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class MapContext extends AstRuleCtx {
		public TerminalNode LCURLY() { return getToken(Cypher5Parser.LCURLY, 0); }
		public TerminalNode RCURLY() { return getToken(Cypher5Parser.RCURLY, 0); }
		public List<PropertyKeyNameContext> propertyKeyName() {
			return getRuleContexts(PropertyKeyNameContext.class);
		}
		public PropertyKeyNameContext propertyKeyName(int i) {
			return getRuleContext(PropertyKeyNameContext.class,i);
		}
		public List<TerminalNode> COLON() { return getTokens(Cypher5Parser.COLON); }
		public TerminalNode COLON(int i) {
			return getToken(Cypher5Parser.COLON, i);
		}
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(Cypher5Parser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(Cypher5Parser.COMMA, i);
		}
		public MapContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_map; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterMap(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitMap(this);
		}
	}

	public final MapContext map() throws RecognitionException {
		MapContext _localctx = new MapContext(_ctx, getState());
		enterRule(_localctx, 654, RULE_map);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2687);
			match(LCURLY);
			setState(2701);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & -985163492230144L) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & -1170935971839803393L) != 0) || ((((_la - 128)) & ~0x3f) == 0 && ((1L << (_la - 128)) & -129253703681L) != 0) || ((((_la - 192)) & ~0x3f) == 0 && ((1L << (_la - 192)) & -36169535635589505L) != 0) || ((((_la - 256)) & ~0x3f) == 0 && ((1L << (_la - 256)) & 18014398509449215L) != 0)) {
				{
				setState(2688);
				propertyKeyName();
				setState(2689);
				match(COLON);
				setState(2690);
				expression();
				setState(2698);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(2691);
					match(COMMA);
					setState(2692);
					propertyKeyName();
					setState(2693);
					match(COLON);
					setState(2694);
					expression();
					}
					}
					setState(2700);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
			}

			setState(2703);
			match(RCURLY);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class SymbolicVariableNameStringContext extends AstRuleCtx {
		public EscapedSymbolicVariableNameStringContext escapedSymbolicVariableNameString() {
			return getRuleContext(EscapedSymbolicVariableNameStringContext.class,0);
		}
		public UnescapedSymbolicVariableNameStringContext unescapedSymbolicVariableNameString() {
			return getRuleContext(UnescapedSymbolicVariableNameStringContext.class,0);
		}
		public SymbolicVariableNameStringContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_symbolicVariableNameString; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterSymbolicVariableNameString(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitSymbolicVariableNameString(this);
		}
	}

	public final SymbolicVariableNameStringContext symbolicVariableNameString() throws RecognitionException {
		SymbolicVariableNameStringContext _localctx = new SymbolicVariableNameStringContext(_ctx, getState());
		enterRule(_localctx, 656, RULE_symbolicVariableNameString);
		try {
			setState(2707);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case ESCAPED_SYMBOLIC_NAME:
				enterOuterAlt(_localctx, 1);
				{
				setState(2705);
				escapedSymbolicVariableNameString();
				}
				break;
			case ACCESS:
			case ACTIVE:
			case ADMIN:
			case ADMINISTRATOR:
			case ALIAS:
			case ALIASES:
			case ALL_SHORTEST_PATHS:
			case ALL:
			case ALTER:
			case AND:
			case ANY:
			case ARRAY:
			case AS:
			case ASC:
			case ASCENDING:
			case ASSERT:
			case ASSIGN:
			case AT:
			case AUTH:
			case BINDINGS:
			case BOOL:
			case BOOLEAN:
			case BOOSTED:
			case BOTH:
			case BREAK:
			case BRIEF:
			case BTREE:
			case BUILT:
			case BY:
			case CALL:
			case CASCADE:
			case CASE:
			case CHANGE:
			case CIDR:
			case COLLECT:
			case COMMAND:
			case COMMANDS:
			case COMMIT:
			case COMPOSITE:
			case CONCURRENT:
			case CONSTRAINT:
			case CONSTRAINTS:
			case CONTAINS:
			case COPY:
			case CONTINUE:
			case COUNT:
			case CREATE:
			case CSV:
			case CURRENT:
			case DATA:
			case DATABASE:
			case DATABASES:
			case DATE:
			case DATETIME:
			case DBMS:
			case DEALLOCATE:
			case DEFAULT:
			case DEFINED:
			case DELETE:
			case DENY:
			case DESC:
			case DESCENDING:
			case DESTROY:
			case DETACH:
			case DIFFERENT:
			case DISTINCT:
			case DRIVER:
			case DROP:
			case DRYRUN:
			case DUMP:
			case DURATION:
			case EACH:
			case EDGE:
			case ENABLE:
			case ELEMENT:
			case ELEMENTS:
			case ELSE:
			case ENCRYPTED:
			case END:
			case ENDS:
			case EXECUTABLE:
			case EXECUTE:
			case EXIST:
			case EXISTENCE:
			case EXISTS:
			case ERROR:
			case FAIL:
			case FALSE:
			case FIELDTERMINATOR:
			case FINISH:
			case FLOAT:
			case FOR:
			case FOREACH:
			case FROM:
			case FULLTEXT:
			case FUNCTION:
			case FUNCTIONS:
			case GRANT:
			case GRAPH:
			case GRAPHS:
			case GROUP:
			case GROUPS:
			case HEADERS:
			case HOME:
			case ID:
			case IF:
			case IMPERSONATE:
			case IMMUTABLE:
			case IN:
			case INDEX:
			case INDEXES:
			case INF:
			case INFINITY:
			case INSERT:
			case INT:
			case INTEGER:
			case IS:
			case JOIN:
			case KEY:
			case LABEL:
			case LABELS:
			case LEADING:
			case LIMITROWS:
			case LIST:
			case LOAD:
			case LOCAL:
			case LOOKUP:
			case MANAGEMENT:
			case MAP:
			case MATCH:
			case MERGE:
			case NAME:
			case NAMES:
			case NAN:
			case NFC:
			case NFD:
			case NFKC:
			case NFKD:
			case NEW:
			case NODE:
			case NODETACH:
			case NODES:
			case NONE:
			case NORMALIZE:
			case NORMALIZED:
			case NOT:
			case NOTHING:
			case NOWAIT:
			case NULL:
			case OF:
			case OFFSET:
			case ON:
			case ONLY:
			case OPTIONAL:
			case OPTIONS:
			case OPTION:
			case OR:
			case ORDER:
			case OUTPUT:
			case PASSWORD:
			case PASSWORDS:
			case PATH:
			case PATHS:
			case PERIODIC:
			case PLAINTEXT:
			case POINT:
			case POPULATED:
			case PRIMARY:
			case PRIMARIES:
			case PRIVILEGE:
			case PRIVILEGES:
			case PROCEDURE:
			case PROCEDURES:
			case PROPERTIES:
			case PROPERTY:
			case PROVIDER:
			case PROVIDERS:
			case RANGE:
			case READ:
			case REALLOCATE:
			case REDUCE:
			case RENAME:
			case REL:
			case RELATIONSHIP:
			case RELATIONSHIPS:
			case REMOVE:
			case REPEATABLE:
			case REPLACE:
			case REPORT:
			case REQUIRE:
			case REQUIRED:
			case RESTRICT:
			case RETURN:
			case REVOKE:
			case ROLE:
			case ROLES:
			case ROW:
			case ROWS:
			case SCAN:
			case SEC:
			case SECOND:
			case SECONDARY:
			case SECONDARIES:
			case SECONDS:
			case SEEK:
			case SERVER:
			case SERVERS:
			case SET:
			case SETTING:
			case SETTINGS:
			case SHORTEST_PATH:
			case SHORTEST:
			case SHOW:
			case SIGNED:
			case SINGLE:
			case SKIPROWS:
			case START:
			case STARTS:
			case STATUS:
			case STOP:
			case STRING:
			case SUPPORTED:
			case SUSPENDED:
			case TARGET:
			case TERMINATE:
			case TEXT:
			case THEN:
			case TIME:
			case TIMESTAMP:
			case TIMEZONE:
			case TO:
			case TOPOLOGY:
			case TRAILING:
			case TRANSACTION:
			case TRANSACTIONS:
			case TRAVERSE:
			case TRIM:
			case TRUE:
			case TYPE:
			case TYPED:
			case TYPES:
			case UNION:
			case UNIQUE:
			case UNIQUENESS:
			case UNWIND:
			case URL:
			case USE:
			case USER:
			case USERS:
			case USING:
			case VALUE:
			case VARCHAR:
			case VECTOR:
			case VERBOSE:
			case VERTEX:
			case WAIT:
			case WHEN:
			case WHERE:
			case WITH:
			case WITHOUT:
			case WRITE:
			case XOR:
			case YIELD:
			case ZONE:
			case ZONED:
			case IDENTIFIER:
				enterOuterAlt(_localctx, 2);
				{
				setState(2706);
				unescapedSymbolicVariableNameString();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class EscapedSymbolicVariableNameStringContext extends AstRuleCtx {
		public EscapedSymbolicNameStringContext escapedSymbolicNameString() {
			return getRuleContext(EscapedSymbolicNameStringContext.class,0);
		}
		public EscapedSymbolicVariableNameStringContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_escapedSymbolicVariableNameString; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterEscapedSymbolicVariableNameString(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitEscapedSymbolicVariableNameString(this);
		}
	}

	public final EscapedSymbolicVariableNameStringContext escapedSymbolicVariableNameString() throws RecognitionException {
		EscapedSymbolicVariableNameStringContext _localctx = new EscapedSymbolicVariableNameStringContext(_ctx, getState());
		enterRule(_localctx, 658, RULE_escapedSymbolicVariableNameString);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2709);
			escapedSymbolicNameString();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class UnescapedSymbolicVariableNameStringContext extends AstRuleCtx {
		public UnescapedSymbolicNameStringContext unescapedSymbolicNameString() {
			return getRuleContext(UnescapedSymbolicNameStringContext.class,0);
		}
		public UnescapedSymbolicVariableNameStringContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_unescapedSymbolicVariableNameString; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterUnescapedSymbolicVariableNameString(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitUnescapedSymbolicVariableNameString(this);
		}
	}

	public final UnescapedSymbolicVariableNameStringContext unescapedSymbolicVariableNameString() throws RecognitionException {
		UnescapedSymbolicVariableNameStringContext _localctx = new UnescapedSymbolicVariableNameStringContext(_ctx, getState());
		enterRule(_localctx, 660, RULE_unescapedSymbolicVariableNameString);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2711);
			unescapedSymbolicNameString();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class SymbolicNameStringContext extends AstRuleCtx {
		public EscapedSymbolicNameStringContext escapedSymbolicNameString() {
			return getRuleContext(EscapedSymbolicNameStringContext.class,0);
		}
		public UnescapedSymbolicNameStringContext unescapedSymbolicNameString() {
			return getRuleContext(UnescapedSymbolicNameStringContext.class,0);
		}
		public SymbolicNameStringContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_symbolicNameString; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterSymbolicNameString(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitSymbolicNameString(this);
		}
	}

	public final SymbolicNameStringContext symbolicNameString() throws RecognitionException {
		SymbolicNameStringContext _localctx = new SymbolicNameStringContext(_ctx, getState());
		enterRule(_localctx, 662, RULE_symbolicNameString);
		try {
			setState(2715);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case ESCAPED_SYMBOLIC_NAME:
				enterOuterAlt(_localctx, 1);
				{
				setState(2713);
				escapedSymbolicNameString();
				}
				break;
			case ACCESS:
			case ACTIVE:
			case ADMIN:
			case ADMINISTRATOR:
			case ALIAS:
			case ALIASES:
			case ALL_SHORTEST_PATHS:
			case ALL:
			case ALTER:
			case AND:
			case ANY:
			case ARRAY:
			case AS:
			case ASC:
			case ASCENDING:
			case ASSERT:
			case ASSIGN:
			case AT:
			case AUTH:
			case BINDINGS:
			case BOOL:
			case BOOLEAN:
			case BOOSTED:
			case BOTH:
			case BREAK:
			case BRIEF:
			case BTREE:
			case BUILT:
			case BY:
			case CALL:
			case CASCADE:
			case CASE:
			case CHANGE:
			case CIDR:
			case COLLECT:
			case COMMAND:
			case COMMANDS:
			case COMMIT:
			case COMPOSITE:
			case CONCURRENT:
			case CONSTRAINT:
			case CONSTRAINTS:
			case CONTAINS:
			case COPY:
			case CONTINUE:
			case COUNT:
			case CREATE:
			case CSV:
			case CURRENT:
			case DATA:
			case DATABASE:
			case DATABASES:
			case DATE:
			case DATETIME:
			case DBMS:
			case DEALLOCATE:
			case DEFAULT:
			case DEFINED:
			case DELETE:
			case DENY:
			case DESC:
			case DESCENDING:
			case DESTROY:
			case DETACH:
			case DIFFERENT:
			case DISTINCT:
			case DRIVER:
			case DROP:
			case DRYRUN:
			case DUMP:
			case DURATION:
			case EACH:
			case EDGE:
			case ENABLE:
			case ELEMENT:
			case ELEMENTS:
			case ELSE:
			case ENCRYPTED:
			case END:
			case ENDS:
			case EXECUTABLE:
			case EXECUTE:
			case EXIST:
			case EXISTENCE:
			case EXISTS:
			case ERROR:
			case FAIL:
			case FALSE:
			case FIELDTERMINATOR:
			case FINISH:
			case FLOAT:
			case FOR:
			case FOREACH:
			case FROM:
			case FULLTEXT:
			case FUNCTION:
			case FUNCTIONS:
			case GRANT:
			case GRAPH:
			case GRAPHS:
			case GROUP:
			case GROUPS:
			case HEADERS:
			case HOME:
			case ID:
			case IF:
			case IMPERSONATE:
			case IMMUTABLE:
			case IN:
			case INDEX:
			case INDEXES:
			case INF:
			case INFINITY:
			case INSERT:
			case INT:
			case INTEGER:
			case IS:
			case JOIN:
			case KEY:
			case LABEL:
			case LABELS:
			case LEADING:
			case LIMITROWS:
			case LIST:
			case LOAD:
			case LOCAL:
			case LOOKUP:
			case MANAGEMENT:
			case MAP:
			case MATCH:
			case MERGE:
			case NAME:
			case NAMES:
			case NAN:
			case NFC:
			case NFD:
			case NFKC:
			case NFKD:
			case NEW:
			case NODE:
			case NODETACH:
			case NODES:
			case NONE:
			case NORMALIZE:
			case NORMALIZED:
			case NOT:
			case NOTHING:
			case NOWAIT:
			case NULL:
			case OF:
			case OFFSET:
			case ON:
			case ONLY:
			case OPTIONAL:
			case OPTIONS:
			case OPTION:
			case OR:
			case ORDER:
			case OUTPUT:
			case PASSWORD:
			case PASSWORDS:
			case PATH:
			case PATHS:
			case PERIODIC:
			case PLAINTEXT:
			case POINT:
			case POPULATED:
			case PRIMARY:
			case PRIMARIES:
			case PRIVILEGE:
			case PRIVILEGES:
			case PROCEDURE:
			case PROCEDURES:
			case PROPERTIES:
			case PROPERTY:
			case PROVIDER:
			case PROVIDERS:
			case RANGE:
			case READ:
			case REALLOCATE:
			case REDUCE:
			case RENAME:
			case REL:
			case RELATIONSHIP:
			case RELATIONSHIPS:
			case REMOVE:
			case REPEATABLE:
			case REPLACE:
			case REPORT:
			case REQUIRE:
			case REQUIRED:
			case RESTRICT:
			case RETURN:
			case REVOKE:
			case ROLE:
			case ROLES:
			case ROW:
			case ROWS:
			case SCAN:
			case SEC:
			case SECOND:
			case SECONDARY:
			case SECONDARIES:
			case SECONDS:
			case SEEK:
			case SERVER:
			case SERVERS:
			case SET:
			case SETTING:
			case SETTINGS:
			case SHORTEST_PATH:
			case SHORTEST:
			case SHOW:
			case SIGNED:
			case SINGLE:
			case SKIPROWS:
			case START:
			case STARTS:
			case STATUS:
			case STOP:
			case STRING:
			case SUPPORTED:
			case SUSPENDED:
			case TARGET:
			case TERMINATE:
			case TEXT:
			case THEN:
			case TIME:
			case TIMESTAMP:
			case TIMEZONE:
			case TO:
			case TOPOLOGY:
			case TRAILING:
			case TRANSACTION:
			case TRANSACTIONS:
			case TRAVERSE:
			case TRIM:
			case TRUE:
			case TYPE:
			case TYPED:
			case TYPES:
			case UNION:
			case UNIQUE:
			case UNIQUENESS:
			case UNWIND:
			case URL:
			case USE:
			case USER:
			case USERS:
			case USING:
			case VALUE:
			case VARCHAR:
			case VECTOR:
			case VERBOSE:
			case VERTEX:
			case WAIT:
			case WHEN:
			case WHERE:
			case WITH:
			case WITHOUT:
			case WRITE:
			case XOR:
			case YIELD:
			case ZONE:
			case ZONED:
			case IDENTIFIER:
				enterOuterAlt(_localctx, 2);
				{
				setState(2714);
				unescapedSymbolicNameString();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class EscapedSymbolicNameStringContext extends AstRuleCtx {
		public TerminalNode ESCAPED_SYMBOLIC_NAME() { return getToken(Cypher5Parser.ESCAPED_SYMBOLIC_NAME, 0); }
		public EscapedSymbolicNameStringContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_escapedSymbolicNameString; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterEscapedSymbolicNameString(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitEscapedSymbolicNameString(this);
		}
	}

	public final EscapedSymbolicNameStringContext escapedSymbolicNameString() throws RecognitionException {
		EscapedSymbolicNameStringContext _localctx = new EscapedSymbolicNameStringContext(_ctx, getState());
		enterRule(_localctx, 664, RULE_escapedSymbolicNameString);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2717);
			match(ESCAPED_SYMBOLIC_NAME);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class UnescapedSymbolicNameStringContext extends AstRuleCtx {
		public UnescapedLabelSymbolicNameStringContext unescapedLabelSymbolicNameString() {
			return getRuleContext(UnescapedLabelSymbolicNameStringContext.class,0);
		}
		public TerminalNode NOT() { return getToken(Cypher5Parser.NOT, 0); }
		public TerminalNode NULL() { return getToken(Cypher5Parser.NULL, 0); }
		public TerminalNode TYPED() { return getToken(Cypher5Parser.TYPED, 0); }
		public TerminalNode NORMALIZED() { return getToken(Cypher5Parser.NORMALIZED, 0); }
		public TerminalNode NFC() { return getToken(Cypher5Parser.NFC, 0); }
		public TerminalNode NFD() { return getToken(Cypher5Parser.NFD, 0); }
		public TerminalNode NFKC() { return getToken(Cypher5Parser.NFKC, 0); }
		public TerminalNode NFKD() { return getToken(Cypher5Parser.NFKD, 0); }
		public UnescapedSymbolicNameStringContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_unescapedSymbolicNameString; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterUnescapedSymbolicNameString(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitUnescapedSymbolicNameString(this);
		}
	}

	public final UnescapedSymbolicNameStringContext unescapedSymbolicNameString() throws RecognitionException {
		UnescapedSymbolicNameStringContext _localctx = new UnescapedSymbolicNameStringContext(_ctx, getState());
		enterRule(_localctx, 666, RULE_unescapedSymbolicNameString);
		try {
			setState(2728);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case ACCESS:
			case ACTIVE:
			case ADMIN:
			case ADMINISTRATOR:
			case ALIAS:
			case ALIASES:
			case ALL_SHORTEST_PATHS:
			case ALL:
			case ALTER:
			case AND:
			case ANY:
			case ARRAY:
			case AS:
			case ASC:
			case ASCENDING:
			case ASSERT:
			case ASSIGN:
			case AT:
			case AUTH:
			case BINDINGS:
			case BOOL:
			case BOOLEAN:
			case BOOSTED:
			case BOTH:
			case BREAK:
			case BRIEF:
			case BTREE:
			case BUILT:
			case BY:
			case CALL:
			case CASCADE:
			case CASE:
			case CHANGE:
			case CIDR:
			case COLLECT:
			case COMMAND:
			case COMMANDS:
			case COMMIT:
			case COMPOSITE:
			case CONCURRENT:
			case CONSTRAINT:
			case CONSTRAINTS:
			case CONTAINS:
			case COPY:
			case CONTINUE:
			case COUNT:
			case CREATE:
			case CSV:
			case CURRENT:
			case DATA:
			case DATABASE:
			case DATABASES:
			case DATE:
			case DATETIME:
			case DBMS:
			case DEALLOCATE:
			case DEFAULT:
			case DEFINED:
			case DELETE:
			case DENY:
			case DESC:
			case DESCENDING:
			case DESTROY:
			case DETACH:
			case DIFFERENT:
			case DISTINCT:
			case DRIVER:
			case DROP:
			case DRYRUN:
			case DUMP:
			case DURATION:
			case EACH:
			case EDGE:
			case ENABLE:
			case ELEMENT:
			case ELEMENTS:
			case ELSE:
			case ENCRYPTED:
			case END:
			case ENDS:
			case EXECUTABLE:
			case EXECUTE:
			case EXIST:
			case EXISTENCE:
			case EXISTS:
			case ERROR:
			case FAIL:
			case FALSE:
			case FIELDTERMINATOR:
			case FINISH:
			case FLOAT:
			case FOR:
			case FOREACH:
			case FROM:
			case FULLTEXT:
			case FUNCTION:
			case FUNCTIONS:
			case GRANT:
			case GRAPH:
			case GRAPHS:
			case GROUP:
			case GROUPS:
			case HEADERS:
			case HOME:
			case ID:
			case IF:
			case IMPERSONATE:
			case IMMUTABLE:
			case IN:
			case INDEX:
			case INDEXES:
			case INF:
			case INFINITY:
			case INSERT:
			case INT:
			case INTEGER:
			case IS:
			case JOIN:
			case KEY:
			case LABEL:
			case LABELS:
			case LEADING:
			case LIMITROWS:
			case LIST:
			case LOAD:
			case LOCAL:
			case LOOKUP:
			case MANAGEMENT:
			case MAP:
			case MATCH:
			case MERGE:
			case NAME:
			case NAMES:
			case NAN:
			case NEW:
			case NODE:
			case NODETACH:
			case NODES:
			case NONE:
			case NORMALIZE:
			case NOTHING:
			case NOWAIT:
			case OF:
			case OFFSET:
			case ON:
			case ONLY:
			case OPTIONAL:
			case OPTIONS:
			case OPTION:
			case OR:
			case ORDER:
			case OUTPUT:
			case PASSWORD:
			case PASSWORDS:
			case PATH:
			case PATHS:
			case PERIODIC:
			case PLAINTEXT:
			case POINT:
			case POPULATED:
			case PRIMARY:
			case PRIMARIES:
			case PRIVILEGE:
			case PRIVILEGES:
			case PROCEDURE:
			case PROCEDURES:
			case PROPERTIES:
			case PROPERTY:
			case PROVIDER:
			case PROVIDERS:
			case RANGE:
			case READ:
			case REALLOCATE:
			case REDUCE:
			case RENAME:
			case REL:
			case RELATIONSHIP:
			case RELATIONSHIPS:
			case REMOVE:
			case REPEATABLE:
			case REPLACE:
			case REPORT:
			case REQUIRE:
			case REQUIRED:
			case RESTRICT:
			case RETURN:
			case REVOKE:
			case ROLE:
			case ROLES:
			case ROW:
			case ROWS:
			case SCAN:
			case SEC:
			case SECOND:
			case SECONDARY:
			case SECONDARIES:
			case SECONDS:
			case SEEK:
			case SERVER:
			case SERVERS:
			case SET:
			case SETTING:
			case SETTINGS:
			case SHORTEST_PATH:
			case SHORTEST:
			case SHOW:
			case SIGNED:
			case SINGLE:
			case SKIPROWS:
			case START:
			case STARTS:
			case STATUS:
			case STOP:
			case STRING:
			case SUPPORTED:
			case SUSPENDED:
			case TARGET:
			case TERMINATE:
			case TEXT:
			case THEN:
			case TIME:
			case TIMESTAMP:
			case TIMEZONE:
			case TO:
			case TOPOLOGY:
			case TRAILING:
			case TRANSACTION:
			case TRANSACTIONS:
			case TRAVERSE:
			case TRIM:
			case TRUE:
			case TYPE:
			case TYPES:
			case UNION:
			case UNIQUE:
			case UNIQUENESS:
			case UNWIND:
			case URL:
			case USE:
			case USER:
			case USERS:
			case USING:
			case VALUE:
			case VARCHAR:
			case VECTOR:
			case VERBOSE:
			case VERTEX:
			case WAIT:
			case WHEN:
			case WHERE:
			case WITH:
			case WITHOUT:
			case WRITE:
			case XOR:
			case YIELD:
			case ZONE:
			case ZONED:
			case IDENTIFIER:
				enterOuterAlt(_localctx, 1);
				{
				setState(2719);
				unescapedLabelSymbolicNameString();
				}
				break;
			case NOT:
				enterOuterAlt(_localctx, 2);
				{
				setState(2720);
				match(NOT);
				}
				break;
			case NULL:
				enterOuterAlt(_localctx, 3);
				{
				setState(2721);
				match(NULL);
				}
				break;
			case TYPED:
				enterOuterAlt(_localctx, 4);
				{
				setState(2722);
				match(TYPED);
				}
				break;
			case NORMALIZED:
				enterOuterAlt(_localctx, 5);
				{
				setState(2723);
				match(NORMALIZED);
				}
				break;
			case NFC:
				enterOuterAlt(_localctx, 6);
				{
				setState(2724);
				match(NFC);
				}
				break;
			case NFD:
				enterOuterAlt(_localctx, 7);
				{
				setState(2725);
				match(NFD);
				}
				break;
			case NFKC:
				enterOuterAlt(_localctx, 8);
				{
				setState(2726);
				match(NFKC);
				}
				break;
			case NFKD:
				enterOuterAlt(_localctx, 9);
				{
				setState(2727);
				match(NFKD);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class SymbolicLabelNameStringContext extends AstRuleCtx {
		public EscapedSymbolicNameStringContext escapedSymbolicNameString() {
			return getRuleContext(EscapedSymbolicNameStringContext.class,0);
		}
		public UnescapedLabelSymbolicNameStringContext unescapedLabelSymbolicNameString() {
			return getRuleContext(UnescapedLabelSymbolicNameStringContext.class,0);
		}
		public SymbolicLabelNameStringContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_symbolicLabelNameString; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterSymbolicLabelNameString(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitSymbolicLabelNameString(this);
		}
	}

	public final SymbolicLabelNameStringContext symbolicLabelNameString() throws RecognitionException {
		SymbolicLabelNameStringContext _localctx = new SymbolicLabelNameStringContext(_ctx, getState());
		enterRule(_localctx, 668, RULE_symbolicLabelNameString);
		try {
			setState(2732);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case ESCAPED_SYMBOLIC_NAME:
				enterOuterAlt(_localctx, 1);
				{
				setState(2730);
				escapedSymbolicNameString();
				}
				break;
			case ACCESS:
			case ACTIVE:
			case ADMIN:
			case ADMINISTRATOR:
			case ALIAS:
			case ALIASES:
			case ALL_SHORTEST_PATHS:
			case ALL:
			case ALTER:
			case AND:
			case ANY:
			case ARRAY:
			case AS:
			case ASC:
			case ASCENDING:
			case ASSERT:
			case ASSIGN:
			case AT:
			case AUTH:
			case BINDINGS:
			case BOOL:
			case BOOLEAN:
			case BOOSTED:
			case BOTH:
			case BREAK:
			case BRIEF:
			case BTREE:
			case BUILT:
			case BY:
			case CALL:
			case CASCADE:
			case CASE:
			case CHANGE:
			case CIDR:
			case COLLECT:
			case COMMAND:
			case COMMANDS:
			case COMMIT:
			case COMPOSITE:
			case CONCURRENT:
			case CONSTRAINT:
			case CONSTRAINTS:
			case CONTAINS:
			case COPY:
			case CONTINUE:
			case COUNT:
			case CREATE:
			case CSV:
			case CURRENT:
			case DATA:
			case DATABASE:
			case DATABASES:
			case DATE:
			case DATETIME:
			case DBMS:
			case DEALLOCATE:
			case DEFAULT:
			case DEFINED:
			case DELETE:
			case DENY:
			case DESC:
			case DESCENDING:
			case DESTROY:
			case DETACH:
			case DIFFERENT:
			case DISTINCT:
			case DRIVER:
			case DROP:
			case DRYRUN:
			case DUMP:
			case DURATION:
			case EACH:
			case EDGE:
			case ENABLE:
			case ELEMENT:
			case ELEMENTS:
			case ELSE:
			case ENCRYPTED:
			case END:
			case ENDS:
			case EXECUTABLE:
			case EXECUTE:
			case EXIST:
			case EXISTENCE:
			case EXISTS:
			case ERROR:
			case FAIL:
			case FALSE:
			case FIELDTERMINATOR:
			case FINISH:
			case FLOAT:
			case FOR:
			case FOREACH:
			case FROM:
			case FULLTEXT:
			case FUNCTION:
			case FUNCTIONS:
			case GRANT:
			case GRAPH:
			case GRAPHS:
			case GROUP:
			case GROUPS:
			case HEADERS:
			case HOME:
			case ID:
			case IF:
			case IMPERSONATE:
			case IMMUTABLE:
			case IN:
			case INDEX:
			case INDEXES:
			case INF:
			case INFINITY:
			case INSERT:
			case INT:
			case INTEGER:
			case IS:
			case JOIN:
			case KEY:
			case LABEL:
			case LABELS:
			case LEADING:
			case LIMITROWS:
			case LIST:
			case LOAD:
			case LOCAL:
			case LOOKUP:
			case MANAGEMENT:
			case MAP:
			case MATCH:
			case MERGE:
			case NAME:
			case NAMES:
			case NAN:
			case NEW:
			case NODE:
			case NODETACH:
			case NODES:
			case NONE:
			case NORMALIZE:
			case NOTHING:
			case NOWAIT:
			case OF:
			case OFFSET:
			case ON:
			case ONLY:
			case OPTIONAL:
			case OPTIONS:
			case OPTION:
			case OR:
			case ORDER:
			case OUTPUT:
			case PASSWORD:
			case PASSWORDS:
			case PATH:
			case PATHS:
			case PERIODIC:
			case PLAINTEXT:
			case POINT:
			case POPULATED:
			case PRIMARY:
			case PRIMARIES:
			case PRIVILEGE:
			case PRIVILEGES:
			case PROCEDURE:
			case PROCEDURES:
			case PROPERTIES:
			case PROPERTY:
			case PROVIDER:
			case PROVIDERS:
			case RANGE:
			case READ:
			case REALLOCATE:
			case REDUCE:
			case RENAME:
			case REL:
			case RELATIONSHIP:
			case RELATIONSHIPS:
			case REMOVE:
			case REPEATABLE:
			case REPLACE:
			case REPORT:
			case REQUIRE:
			case REQUIRED:
			case RESTRICT:
			case RETURN:
			case REVOKE:
			case ROLE:
			case ROLES:
			case ROW:
			case ROWS:
			case SCAN:
			case SEC:
			case SECOND:
			case SECONDARY:
			case SECONDARIES:
			case SECONDS:
			case SEEK:
			case SERVER:
			case SERVERS:
			case SET:
			case SETTING:
			case SETTINGS:
			case SHORTEST_PATH:
			case SHORTEST:
			case SHOW:
			case SIGNED:
			case SINGLE:
			case SKIPROWS:
			case START:
			case STARTS:
			case STATUS:
			case STOP:
			case STRING:
			case SUPPORTED:
			case SUSPENDED:
			case TARGET:
			case TERMINATE:
			case TEXT:
			case THEN:
			case TIME:
			case TIMESTAMP:
			case TIMEZONE:
			case TO:
			case TOPOLOGY:
			case TRAILING:
			case TRANSACTION:
			case TRANSACTIONS:
			case TRAVERSE:
			case TRIM:
			case TRUE:
			case TYPE:
			case TYPES:
			case UNION:
			case UNIQUE:
			case UNIQUENESS:
			case UNWIND:
			case URL:
			case USE:
			case USER:
			case USERS:
			case USING:
			case VALUE:
			case VARCHAR:
			case VECTOR:
			case VERBOSE:
			case VERTEX:
			case WAIT:
			case WHEN:
			case WHERE:
			case WITH:
			case WITHOUT:
			case WRITE:
			case XOR:
			case YIELD:
			case ZONE:
			case ZONED:
			case IDENTIFIER:
				enterOuterAlt(_localctx, 2);
				{
				setState(2731);
				unescapedLabelSymbolicNameString();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class UnescapedLabelSymbolicNameStringContext extends AstRuleCtx {
		public UnescapedLabelSymbolicNameString_Context unescapedLabelSymbolicNameString_() {
			return getRuleContext(UnescapedLabelSymbolicNameString_Context.class,0);
		}
		public UnescapedLabelSymbolicNameStringContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_unescapedLabelSymbolicNameString; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterUnescapedLabelSymbolicNameString(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitUnescapedLabelSymbolicNameString(this);
		}
	}

	public final UnescapedLabelSymbolicNameStringContext unescapedLabelSymbolicNameString() throws RecognitionException {
		UnescapedLabelSymbolicNameStringContext _localctx = new UnescapedLabelSymbolicNameStringContext(_ctx, getState());
		enterRule(_localctx, 670, RULE_unescapedLabelSymbolicNameString);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2734);
			unescapedLabelSymbolicNameString_();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class UnescapedLabelSymbolicNameString_Context extends AstRuleCtx {
		public TerminalNode IDENTIFIER() { return getToken(Cypher5Parser.IDENTIFIER, 0); }
		public TerminalNode ACCESS() { return getToken(Cypher5Parser.ACCESS, 0); }
		public TerminalNode ACTIVE() { return getToken(Cypher5Parser.ACTIVE, 0); }
		public TerminalNode ADMIN() { return getToken(Cypher5Parser.ADMIN, 0); }
		public TerminalNode ADMINISTRATOR() { return getToken(Cypher5Parser.ADMINISTRATOR, 0); }
		public TerminalNode ALIAS() { return getToken(Cypher5Parser.ALIAS, 0); }
		public TerminalNode ALIASES() { return getToken(Cypher5Parser.ALIASES, 0); }
		public TerminalNode ALL_SHORTEST_PATHS() { return getToken(Cypher5Parser.ALL_SHORTEST_PATHS, 0); }
		public TerminalNode ALL() { return getToken(Cypher5Parser.ALL, 0); }
		public TerminalNode ALTER() { return getToken(Cypher5Parser.ALTER, 0); }
		public TerminalNode AND() { return getToken(Cypher5Parser.AND, 0); }
		public TerminalNode ANY() { return getToken(Cypher5Parser.ANY, 0); }
		public TerminalNode ARRAY() { return getToken(Cypher5Parser.ARRAY, 0); }
		public TerminalNode AS() { return getToken(Cypher5Parser.AS, 0); }
		public TerminalNode ASC() { return getToken(Cypher5Parser.ASC, 0); }
		public TerminalNode ASCENDING() { return getToken(Cypher5Parser.ASCENDING, 0); }
		public TerminalNode ASSERT() { return getToken(Cypher5Parser.ASSERT, 0); }
		public TerminalNode ASSIGN() { return getToken(Cypher5Parser.ASSIGN, 0); }
		public TerminalNode AT() { return getToken(Cypher5Parser.AT, 0); }
		public TerminalNode AUTH() { return getToken(Cypher5Parser.AUTH, 0); }
		public TerminalNode BINDINGS() { return getToken(Cypher5Parser.BINDINGS, 0); }
		public TerminalNode BOOL() { return getToken(Cypher5Parser.BOOL, 0); }
		public TerminalNode BOOLEAN() { return getToken(Cypher5Parser.BOOLEAN, 0); }
		public TerminalNode BOOSTED() { return getToken(Cypher5Parser.BOOSTED, 0); }
		public TerminalNode BOTH() { return getToken(Cypher5Parser.BOTH, 0); }
		public TerminalNode BREAK() { return getToken(Cypher5Parser.BREAK, 0); }
		public TerminalNode BRIEF() { return getToken(Cypher5Parser.BRIEF, 0); }
		public TerminalNode BTREE() { return getToken(Cypher5Parser.BTREE, 0); }
		public TerminalNode BUILT() { return getToken(Cypher5Parser.BUILT, 0); }
		public TerminalNode BY() { return getToken(Cypher5Parser.BY, 0); }
		public TerminalNode CALL() { return getToken(Cypher5Parser.CALL, 0); }
		public TerminalNode CASCADE() { return getToken(Cypher5Parser.CASCADE, 0); }
		public TerminalNode CASE() { return getToken(Cypher5Parser.CASE, 0); }
		public TerminalNode CHANGE() { return getToken(Cypher5Parser.CHANGE, 0); }
		public TerminalNode CIDR() { return getToken(Cypher5Parser.CIDR, 0); }
		public TerminalNode COLLECT() { return getToken(Cypher5Parser.COLLECT, 0); }
		public TerminalNode COMMAND() { return getToken(Cypher5Parser.COMMAND, 0); }
		public TerminalNode COMMANDS() { return getToken(Cypher5Parser.COMMANDS, 0); }
		public TerminalNode COMMIT() { return getToken(Cypher5Parser.COMMIT, 0); }
		public TerminalNode COMPOSITE() { return getToken(Cypher5Parser.COMPOSITE, 0); }
		public TerminalNode CONCURRENT() { return getToken(Cypher5Parser.CONCURRENT, 0); }
		public TerminalNode CONSTRAINT() { return getToken(Cypher5Parser.CONSTRAINT, 0); }
		public TerminalNode CONSTRAINTS() { return getToken(Cypher5Parser.CONSTRAINTS, 0); }
		public TerminalNode CONTAINS() { return getToken(Cypher5Parser.CONTAINS, 0); }
		public TerminalNode CONTINUE() { return getToken(Cypher5Parser.CONTINUE, 0); }
		public TerminalNode COPY() { return getToken(Cypher5Parser.COPY, 0); }
		public TerminalNode COUNT() { return getToken(Cypher5Parser.COUNT, 0); }
		public TerminalNode CREATE() { return getToken(Cypher5Parser.CREATE, 0); }
		public TerminalNode CSV() { return getToken(Cypher5Parser.CSV, 0); }
		public TerminalNode CURRENT() { return getToken(Cypher5Parser.CURRENT, 0); }
		public TerminalNode DATA() { return getToken(Cypher5Parser.DATA, 0); }
		public TerminalNode DATABASE() { return getToken(Cypher5Parser.DATABASE, 0); }
		public TerminalNode DATABASES() { return getToken(Cypher5Parser.DATABASES, 0); }
		public TerminalNode DATE() { return getToken(Cypher5Parser.DATE, 0); }
		public TerminalNode DATETIME() { return getToken(Cypher5Parser.DATETIME, 0); }
		public TerminalNode DBMS() { return getToken(Cypher5Parser.DBMS, 0); }
		public TerminalNode DEALLOCATE() { return getToken(Cypher5Parser.DEALLOCATE, 0); }
		public TerminalNode DEFAULT() { return getToken(Cypher5Parser.DEFAULT, 0); }
		public TerminalNode DEFINED() { return getToken(Cypher5Parser.DEFINED, 0); }
		public TerminalNode DELETE() { return getToken(Cypher5Parser.DELETE, 0); }
		public TerminalNode DENY() { return getToken(Cypher5Parser.DENY, 0); }
		public TerminalNode DESC() { return getToken(Cypher5Parser.DESC, 0); }
		public TerminalNode DESCENDING() { return getToken(Cypher5Parser.DESCENDING, 0); }
		public TerminalNode DESTROY() { return getToken(Cypher5Parser.DESTROY, 0); }
		public TerminalNode DETACH() { return getToken(Cypher5Parser.DETACH, 0); }
		public TerminalNode DIFFERENT() { return getToken(Cypher5Parser.DIFFERENT, 0); }
		public TerminalNode DISTINCT() { return getToken(Cypher5Parser.DISTINCT, 0); }
		public TerminalNode DRIVER() { return getToken(Cypher5Parser.DRIVER, 0); }
		public TerminalNode DROP() { return getToken(Cypher5Parser.DROP, 0); }
		public TerminalNode DRYRUN() { return getToken(Cypher5Parser.DRYRUN, 0); }
		public TerminalNode DUMP() { return getToken(Cypher5Parser.DUMP, 0); }
		public TerminalNode DURATION() { return getToken(Cypher5Parser.DURATION, 0); }
		public TerminalNode EACH() { return getToken(Cypher5Parser.EACH, 0); }
		public TerminalNode EDGE() { return getToken(Cypher5Parser.EDGE, 0); }
		public TerminalNode ELEMENT() { return getToken(Cypher5Parser.ELEMENT, 0); }
		public TerminalNode ELEMENTS() { return getToken(Cypher5Parser.ELEMENTS, 0); }
		public TerminalNode ELSE() { return getToken(Cypher5Parser.ELSE, 0); }
		public TerminalNode ENABLE() { return getToken(Cypher5Parser.ENABLE, 0); }
		public TerminalNode ENCRYPTED() { return getToken(Cypher5Parser.ENCRYPTED, 0); }
		public TerminalNode END() { return getToken(Cypher5Parser.END, 0); }
		public TerminalNode ENDS() { return getToken(Cypher5Parser.ENDS, 0); }
		public TerminalNode ERROR() { return getToken(Cypher5Parser.ERROR, 0); }
		public TerminalNode EXECUTABLE() { return getToken(Cypher5Parser.EXECUTABLE, 0); }
		public TerminalNode EXECUTE() { return getToken(Cypher5Parser.EXECUTE, 0); }
		public TerminalNode EXIST() { return getToken(Cypher5Parser.EXIST, 0); }
		public TerminalNode EXISTENCE() { return getToken(Cypher5Parser.EXISTENCE, 0); }
		public TerminalNode EXISTS() { return getToken(Cypher5Parser.EXISTS, 0); }
		public TerminalNode FAIL() { return getToken(Cypher5Parser.FAIL, 0); }
		public TerminalNode FALSE() { return getToken(Cypher5Parser.FALSE, 0); }
		public TerminalNode FIELDTERMINATOR() { return getToken(Cypher5Parser.FIELDTERMINATOR, 0); }
		public TerminalNode FINISH() { return getToken(Cypher5Parser.FINISH, 0); }
		public TerminalNode FLOAT() { return getToken(Cypher5Parser.FLOAT, 0); }
		public TerminalNode FOREACH() { return getToken(Cypher5Parser.FOREACH, 0); }
		public TerminalNode FOR() { return getToken(Cypher5Parser.FOR, 0); }
		public TerminalNode FROM() { return getToken(Cypher5Parser.FROM, 0); }
		public TerminalNode FULLTEXT() { return getToken(Cypher5Parser.FULLTEXT, 0); }
		public TerminalNode FUNCTION() { return getToken(Cypher5Parser.FUNCTION, 0); }
		public TerminalNode FUNCTIONS() { return getToken(Cypher5Parser.FUNCTIONS, 0); }
		public TerminalNode GRANT() { return getToken(Cypher5Parser.GRANT, 0); }
		public TerminalNode GRAPH() { return getToken(Cypher5Parser.GRAPH, 0); }
		public TerminalNode GRAPHS() { return getToken(Cypher5Parser.GRAPHS, 0); }
		public TerminalNode GROUP() { return getToken(Cypher5Parser.GROUP, 0); }
		public TerminalNode GROUPS() { return getToken(Cypher5Parser.GROUPS, 0); }
		public TerminalNode HEADERS() { return getToken(Cypher5Parser.HEADERS, 0); }
		public TerminalNode HOME() { return getToken(Cypher5Parser.HOME, 0); }
		public TerminalNode ID() { return getToken(Cypher5Parser.ID, 0); }
		public TerminalNode IF() { return getToken(Cypher5Parser.IF, 0); }
		public TerminalNode IMMUTABLE() { return getToken(Cypher5Parser.IMMUTABLE, 0); }
		public TerminalNode IMPERSONATE() { return getToken(Cypher5Parser.IMPERSONATE, 0); }
		public TerminalNode IN() { return getToken(Cypher5Parser.IN, 0); }
		public TerminalNode INDEX() { return getToken(Cypher5Parser.INDEX, 0); }
		public TerminalNode INDEXES() { return getToken(Cypher5Parser.INDEXES, 0); }
		public TerminalNode INF() { return getToken(Cypher5Parser.INF, 0); }
		public TerminalNode INFINITY() { return getToken(Cypher5Parser.INFINITY, 0); }
		public TerminalNode INSERT() { return getToken(Cypher5Parser.INSERT, 0); }
		public TerminalNode INT() { return getToken(Cypher5Parser.INT, 0); }
		public TerminalNode INTEGER() { return getToken(Cypher5Parser.INTEGER, 0); }
		public TerminalNode IS() { return getToken(Cypher5Parser.IS, 0); }
		public TerminalNode JOIN() { return getToken(Cypher5Parser.JOIN, 0); }
		public TerminalNode KEY() { return getToken(Cypher5Parser.KEY, 0); }
		public TerminalNode LABEL() { return getToken(Cypher5Parser.LABEL, 0); }
		public TerminalNode LABELS() { return getToken(Cypher5Parser.LABELS, 0); }
		public TerminalNode LEADING() { return getToken(Cypher5Parser.LEADING, 0); }
		public TerminalNode LIMITROWS() { return getToken(Cypher5Parser.LIMITROWS, 0); }
		public TerminalNode LIST() { return getToken(Cypher5Parser.LIST, 0); }
		public TerminalNode LOAD() { return getToken(Cypher5Parser.LOAD, 0); }
		public TerminalNode LOCAL() { return getToken(Cypher5Parser.LOCAL, 0); }
		public TerminalNode LOOKUP() { return getToken(Cypher5Parser.LOOKUP, 0); }
		public TerminalNode MATCH() { return getToken(Cypher5Parser.MATCH, 0); }
		public TerminalNode MANAGEMENT() { return getToken(Cypher5Parser.MANAGEMENT, 0); }
		public TerminalNode MAP() { return getToken(Cypher5Parser.MAP, 0); }
		public TerminalNode MERGE() { return getToken(Cypher5Parser.MERGE, 0); }
		public TerminalNode NAME() { return getToken(Cypher5Parser.NAME, 0); }
		public TerminalNode NAMES() { return getToken(Cypher5Parser.NAMES, 0); }
		public TerminalNode NAN() { return getToken(Cypher5Parser.NAN, 0); }
		public TerminalNode NEW() { return getToken(Cypher5Parser.NEW, 0); }
		public TerminalNode NODE() { return getToken(Cypher5Parser.NODE, 0); }
		public TerminalNode NODETACH() { return getToken(Cypher5Parser.NODETACH, 0); }
		public TerminalNode NODES() { return getToken(Cypher5Parser.NODES, 0); }
		public TerminalNode NONE() { return getToken(Cypher5Parser.NONE, 0); }
		public TerminalNode NORMALIZE() { return getToken(Cypher5Parser.NORMALIZE, 0); }
		public TerminalNode NOTHING() { return getToken(Cypher5Parser.NOTHING, 0); }
		public TerminalNode NOWAIT() { return getToken(Cypher5Parser.NOWAIT, 0); }
		public TerminalNode OF() { return getToken(Cypher5Parser.OF, 0); }
		public TerminalNode OFFSET() { return getToken(Cypher5Parser.OFFSET, 0); }
		public TerminalNode ON() { return getToken(Cypher5Parser.ON, 0); }
		public TerminalNode ONLY() { return getToken(Cypher5Parser.ONLY, 0); }
		public TerminalNode OPTIONAL() { return getToken(Cypher5Parser.OPTIONAL, 0); }
		public TerminalNode OPTIONS() { return getToken(Cypher5Parser.OPTIONS, 0); }
		public TerminalNode OPTION() { return getToken(Cypher5Parser.OPTION, 0); }
		public TerminalNode OR() { return getToken(Cypher5Parser.OR, 0); }
		public TerminalNode ORDER() { return getToken(Cypher5Parser.ORDER, 0); }
		public TerminalNode OUTPUT() { return getToken(Cypher5Parser.OUTPUT, 0); }
		public TerminalNode PASSWORD() { return getToken(Cypher5Parser.PASSWORD, 0); }
		public TerminalNode PASSWORDS() { return getToken(Cypher5Parser.PASSWORDS, 0); }
		public TerminalNode PATH() { return getToken(Cypher5Parser.PATH, 0); }
		public TerminalNode PATHS() { return getToken(Cypher5Parser.PATHS, 0); }
		public TerminalNode PERIODIC() { return getToken(Cypher5Parser.PERIODIC, 0); }
		public TerminalNode PLAINTEXT() { return getToken(Cypher5Parser.PLAINTEXT, 0); }
		public TerminalNode POINT() { return getToken(Cypher5Parser.POINT, 0); }
		public TerminalNode POPULATED() { return getToken(Cypher5Parser.POPULATED, 0); }
		public TerminalNode PRIMARY() { return getToken(Cypher5Parser.PRIMARY, 0); }
		public TerminalNode PRIMARIES() { return getToken(Cypher5Parser.PRIMARIES, 0); }
		public TerminalNode PRIVILEGE() { return getToken(Cypher5Parser.PRIVILEGE, 0); }
		public TerminalNode PRIVILEGES() { return getToken(Cypher5Parser.PRIVILEGES, 0); }
		public TerminalNode PROCEDURE() { return getToken(Cypher5Parser.PROCEDURE, 0); }
		public TerminalNode PROCEDURES() { return getToken(Cypher5Parser.PROCEDURES, 0); }
		public TerminalNode PROPERTIES() { return getToken(Cypher5Parser.PROPERTIES, 0); }
		public TerminalNode PROPERTY() { return getToken(Cypher5Parser.PROPERTY, 0); }
		public TerminalNode PROVIDER() { return getToken(Cypher5Parser.PROVIDER, 0); }
		public TerminalNode PROVIDERS() { return getToken(Cypher5Parser.PROVIDERS, 0); }
		public TerminalNode RANGE() { return getToken(Cypher5Parser.RANGE, 0); }
		public TerminalNode READ() { return getToken(Cypher5Parser.READ, 0); }
		public TerminalNode REALLOCATE() { return getToken(Cypher5Parser.REALLOCATE, 0); }
		public TerminalNode REDUCE() { return getToken(Cypher5Parser.REDUCE, 0); }
		public TerminalNode REL() { return getToken(Cypher5Parser.REL, 0); }
		public TerminalNode RELATIONSHIP() { return getToken(Cypher5Parser.RELATIONSHIP, 0); }
		public TerminalNode RELATIONSHIPS() { return getToken(Cypher5Parser.RELATIONSHIPS, 0); }
		public TerminalNode REMOVE() { return getToken(Cypher5Parser.REMOVE, 0); }
		public TerminalNode RENAME() { return getToken(Cypher5Parser.RENAME, 0); }
		public TerminalNode REPEATABLE() { return getToken(Cypher5Parser.REPEATABLE, 0); }
		public TerminalNode REPLACE() { return getToken(Cypher5Parser.REPLACE, 0); }
		public TerminalNode REPORT() { return getToken(Cypher5Parser.REPORT, 0); }
		public TerminalNode REQUIRE() { return getToken(Cypher5Parser.REQUIRE, 0); }
		public TerminalNode REQUIRED() { return getToken(Cypher5Parser.REQUIRED, 0); }
		public TerminalNode RESTRICT() { return getToken(Cypher5Parser.RESTRICT, 0); }
		public TerminalNode RETURN() { return getToken(Cypher5Parser.RETURN, 0); }
		public TerminalNode REVOKE() { return getToken(Cypher5Parser.REVOKE, 0); }
		public TerminalNode ROLE() { return getToken(Cypher5Parser.ROLE, 0); }
		public TerminalNode ROLES() { return getToken(Cypher5Parser.ROLES, 0); }
		public TerminalNode ROW() { return getToken(Cypher5Parser.ROW, 0); }
		public TerminalNode ROWS() { return getToken(Cypher5Parser.ROWS, 0); }
		public TerminalNode SCAN() { return getToken(Cypher5Parser.SCAN, 0); }
		public TerminalNode SECONDARY() { return getToken(Cypher5Parser.SECONDARY, 0); }
		public TerminalNode SECONDARIES() { return getToken(Cypher5Parser.SECONDARIES, 0); }
		public TerminalNode SEC() { return getToken(Cypher5Parser.SEC, 0); }
		public TerminalNode SECOND() { return getToken(Cypher5Parser.SECOND, 0); }
		public TerminalNode SECONDS() { return getToken(Cypher5Parser.SECONDS, 0); }
		public TerminalNode SEEK() { return getToken(Cypher5Parser.SEEK, 0); }
		public TerminalNode SERVER() { return getToken(Cypher5Parser.SERVER, 0); }
		public TerminalNode SERVERS() { return getToken(Cypher5Parser.SERVERS, 0); }
		public TerminalNode SET() { return getToken(Cypher5Parser.SET, 0); }
		public TerminalNode SETTING() { return getToken(Cypher5Parser.SETTING, 0); }
		public TerminalNode SETTINGS() { return getToken(Cypher5Parser.SETTINGS, 0); }
		public TerminalNode SHORTEST() { return getToken(Cypher5Parser.SHORTEST, 0); }
		public TerminalNode SHORTEST_PATH() { return getToken(Cypher5Parser.SHORTEST_PATH, 0); }
		public TerminalNode SHOW() { return getToken(Cypher5Parser.SHOW, 0); }
		public TerminalNode SIGNED() { return getToken(Cypher5Parser.SIGNED, 0); }
		public TerminalNode SINGLE() { return getToken(Cypher5Parser.SINGLE, 0); }
		public TerminalNode SKIPROWS() { return getToken(Cypher5Parser.SKIPROWS, 0); }
		public TerminalNode START() { return getToken(Cypher5Parser.START, 0); }
		public TerminalNode STARTS() { return getToken(Cypher5Parser.STARTS, 0); }
		public TerminalNode STATUS() { return getToken(Cypher5Parser.STATUS, 0); }
		public TerminalNode STOP() { return getToken(Cypher5Parser.STOP, 0); }
		public TerminalNode VARCHAR() { return getToken(Cypher5Parser.VARCHAR, 0); }
		public TerminalNode STRING() { return getToken(Cypher5Parser.STRING, 0); }
		public TerminalNode SUPPORTED() { return getToken(Cypher5Parser.SUPPORTED, 0); }
		public TerminalNode SUSPENDED() { return getToken(Cypher5Parser.SUSPENDED, 0); }
		public TerminalNode TARGET() { return getToken(Cypher5Parser.TARGET, 0); }
		public TerminalNode TERMINATE() { return getToken(Cypher5Parser.TERMINATE, 0); }
		public TerminalNode TEXT() { return getToken(Cypher5Parser.TEXT, 0); }
		public TerminalNode THEN() { return getToken(Cypher5Parser.THEN, 0); }
		public TerminalNode TIME() { return getToken(Cypher5Parser.TIME, 0); }
		public TerminalNode TIMESTAMP() { return getToken(Cypher5Parser.TIMESTAMP, 0); }
		public TerminalNode TIMEZONE() { return getToken(Cypher5Parser.TIMEZONE, 0); }
		public TerminalNode TO() { return getToken(Cypher5Parser.TO, 0); }
		public TerminalNode TOPOLOGY() { return getToken(Cypher5Parser.TOPOLOGY, 0); }
		public TerminalNode TRAILING() { return getToken(Cypher5Parser.TRAILING, 0); }
		public TerminalNode TRANSACTION() { return getToken(Cypher5Parser.TRANSACTION, 0); }
		public TerminalNode TRANSACTIONS() { return getToken(Cypher5Parser.TRANSACTIONS, 0); }
		public TerminalNode TRAVERSE() { return getToken(Cypher5Parser.TRAVERSE, 0); }
		public TerminalNode TRIM() { return getToken(Cypher5Parser.TRIM, 0); }
		public TerminalNode TRUE() { return getToken(Cypher5Parser.TRUE, 0); }
		public TerminalNode TYPE() { return getToken(Cypher5Parser.TYPE, 0); }
		public TerminalNode TYPES() { return getToken(Cypher5Parser.TYPES, 0); }
		public TerminalNode UNION() { return getToken(Cypher5Parser.UNION, 0); }
		public TerminalNode UNIQUE() { return getToken(Cypher5Parser.UNIQUE, 0); }
		public TerminalNode UNIQUENESS() { return getToken(Cypher5Parser.UNIQUENESS, 0); }
		public TerminalNode UNWIND() { return getToken(Cypher5Parser.UNWIND, 0); }
		public TerminalNode URL() { return getToken(Cypher5Parser.URL, 0); }
		public TerminalNode USE() { return getToken(Cypher5Parser.USE, 0); }
		public TerminalNode USER() { return getToken(Cypher5Parser.USER, 0); }
		public TerminalNode USERS() { return getToken(Cypher5Parser.USERS, 0); }
		public TerminalNode USING() { return getToken(Cypher5Parser.USING, 0); }
		public TerminalNode VALUE() { return getToken(Cypher5Parser.VALUE, 0); }
		public TerminalNode VECTOR() { return getToken(Cypher5Parser.VECTOR, 0); }
		public TerminalNode VERBOSE() { return getToken(Cypher5Parser.VERBOSE, 0); }
		public TerminalNode VERTEX() { return getToken(Cypher5Parser.VERTEX, 0); }
		public TerminalNode WAIT() { return getToken(Cypher5Parser.WAIT, 0); }
		public TerminalNode WHEN() { return getToken(Cypher5Parser.WHEN, 0); }
		public TerminalNode WHERE() { return getToken(Cypher5Parser.WHERE, 0); }
		public TerminalNode WITH() { return getToken(Cypher5Parser.WITH, 0); }
		public TerminalNode WITHOUT() { return getToken(Cypher5Parser.WITHOUT, 0); }
		public TerminalNode WRITE() { return getToken(Cypher5Parser.WRITE, 0); }
		public TerminalNode XOR() { return getToken(Cypher5Parser.XOR, 0); }
		public TerminalNode YIELD() { return getToken(Cypher5Parser.YIELD, 0); }
		public TerminalNode ZONE() { return getToken(Cypher5Parser.ZONE, 0); }
		public TerminalNode ZONED() { return getToken(Cypher5Parser.ZONED, 0); }
		public UnescapedLabelSymbolicNameString_Context(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_unescapedLabelSymbolicNameString_; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterUnescapedLabelSymbolicNameString_(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitUnescapedLabelSymbolicNameString_(this);
		}
	}

	public final UnescapedLabelSymbolicNameString_Context unescapedLabelSymbolicNameString_() throws RecognitionException {
		UnescapedLabelSymbolicNameString_Context _localctx = new UnescapedLabelSymbolicNameString_Context(_ctx, getState());
		enterRule(_localctx, 672, RULE_unescapedLabelSymbolicNameString_);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2736);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & -985163492231168L) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & -1170935971839803393L) != 0) || ((((_la - 128)) & ~0x3f) == 0 && ((1L << (_la - 128)) & -21408720158130177L) != 0) || ((((_la - 192)) & ~0x3f) == 0 && ((1L << (_la - 192)) & -36169535635589505L) != 0) || ((((_la - 256)) & ~0x3f) == 0 && ((1L << (_la - 256)) & 18014398375231487L) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class EndOfFileContext extends AstRuleCtx {
		public TerminalNode EOF() { return getToken(Cypher5Parser.EOF, 0); }
		public EndOfFileContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_endOfFile; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).enterEndOfFile(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cypher5ParserListener ) ((Cypher5ParserListener)listener).exitEndOfFile(this);
		}
	}

	public final EndOfFileContext endOfFile() throws RecognitionException {
		EndOfFileContext _localctx = new EndOfFileContext(_ctx, getState());
		enterRule(_localctx, 674, RULE_endOfFile);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2738);
			match(EOF);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	private static final String _serializedATNSegment0 =
		"\u0004\u0001\u0139\u0ab5\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001"+
		"\u0002\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004\u0007\u0004"+
		"\u0002\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002\u0007\u0007\u0007"+
		"\u0002\b\u0007\b\u0002\t\u0007\t\u0002\n\u0007\n\u0002\u000b\u0007\u000b"+
		"\u0002\f\u0007\f\u0002\r\u0007\r\u0002\u000e\u0007\u000e\u0002\u000f\u0007"+
		"\u000f\u0002\u0010\u0007\u0010\u0002\u0011\u0007\u0011\u0002\u0012\u0007"+
		"\u0012\u0002\u0013\u0007\u0013\u0002\u0014\u0007\u0014\u0002\u0015\u0007"+
		"\u0015\u0002\u0016\u0007\u0016\u0002\u0017\u0007\u0017\u0002\u0018\u0007"+
		"\u0018\u0002\u0019\u0007\u0019\u0002\u001a\u0007\u001a\u0002\u001b\u0007"+
		"\u001b\u0002\u001c\u0007\u001c\u0002\u001d\u0007\u001d\u0002\u001e\u0007"+
		"\u001e\u0002\u001f\u0007\u001f\u0002 \u0007 \u0002!\u0007!\u0002\"\u0007"+
		"\"\u0002#\u0007#\u0002$\u0007$\u0002%\u0007%\u0002&\u0007&\u0002\'\u0007"+
		"\'\u0002(\u0007(\u0002)\u0007)\u0002*\u0007*\u0002+\u0007+\u0002,\u0007"+
		",\u0002-\u0007-\u0002.\u0007.\u0002/\u0007/\u00020\u00070\u00021\u0007"+
		"1\u00022\u00072\u00023\u00073\u00024\u00074\u00025\u00075\u00026\u0007"+
		"6\u00027\u00077\u00028\u00078\u00029\u00079\u0002:\u0007:\u0002;\u0007"+
		";\u0002<\u0007<\u0002=\u0007=\u0002>\u0007>\u0002?\u0007?\u0002@\u0007"+
		"@\u0002A\u0007A\u0002B\u0007B\u0002C\u0007C\u0002D\u0007D\u0002E\u0007"+
		"E\u0002F\u0007F\u0002G\u0007G\u0002H\u0007H\u0002I\u0007I\u0002J\u0007"+
		"J\u0002K\u0007K\u0002L\u0007L\u0002M\u0007M\u0002N\u0007N\u0002O\u0007"+
		"O\u0002P\u0007P\u0002Q\u0007Q\u0002R\u0007R\u0002S\u0007S\u0002T\u0007"+
		"T\u0002U\u0007U\u0002V\u0007V\u0002W\u0007W\u0002X\u0007X\u0002Y\u0007"+
		"Y\u0002Z\u0007Z\u0002[\u0007[\u0002\\\u0007\\\u0002]\u0007]\u0002^\u0007"+
		"^\u0002_\u0007_\u0002`\u0007`\u0002a\u0007a\u0002b\u0007b\u0002c\u0007"+
		"c\u0002d\u0007d\u0002e\u0007e\u0002f\u0007f\u0002g\u0007g\u0002h\u0007"+
		"h\u0002i\u0007i\u0002j\u0007j\u0002k\u0007k\u0002l\u0007l\u0002m\u0007"+
		"m\u0002n\u0007n\u0002o\u0007o\u0002p\u0007p\u0002q\u0007q\u0002r\u0007"+
		"r\u0002s\u0007s\u0002t\u0007t\u0002u\u0007u\u0002v\u0007v\u0002w\u0007"+
		"w\u0002x\u0007x\u0002y\u0007y\u0002z\u0007z\u0002{\u0007{\u0002|\u0007"+
		"|\u0002}\u0007}\u0002~\u0007~\u0002\u007f\u0007\u007f\u0002\u0080\u0007"+
		"\u0080\u0002\u0081\u0007\u0081\u0002\u0082\u0007\u0082\u0002\u0083\u0007"+
		"\u0083\u0002\u0084\u0007\u0084\u0002\u0085\u0007\u0085\u0002\u0086\u0007"+
		"\u0086\u0002\u0087\u0007\u0087\u0002\u0088\u0007\u0088\u0002\u0089\u0007"+
		"\u0089\u0002\u008a\u0007\u008a\u0002\u008b\u0007\u008b\u0002\u008c\u0007"+
		"\u008c\u0002\u008d\u0007\u008d\u0002\u008e\u0007\u008e\u0002\u008f\u0007"+
		"\u008f\u0002\u0090\u0007\u0090\u0002\u0091\u0007\u0091\u0002\u0092\u0007"+
		"\u0092\u0002\u0093\u0007\u0093\u0002\u0094\u0007\u0094\u0002\u0095\u0007"+
		"\u0095\u0002\u0096\u0007\u0096\u0002\u0097\u0007\u0097\u0002\u0098\u0007"+
		"\u0098\u0002\u0099\u0007\u0099\u0002\u009a\u0007\u009a\u0002\u009b\u0007"+
		"\u009b\u0002\u009c\u0007\u009c\u0002\u009d\u0007\u009d\u0002\u009e\u0007"+
		"\u009e\u0002\u009f\u0007\u009f\u0002\u00a0\u0007\u00a0\u0002\u00a1\u0007"+
		"\u00a1\u0002\u00a2\u0007\u00a2\u0002\u00a3\u0007\u00a3\u0002\u00a4\u0007"+
		"\u00a4\u0002\u00a5\u0007\u00a5\u0002\u00a6\u0007\u00a6\u0002\u00a7\u0007"+
		"\u00a7\u0002\u00a8\u0007\u00a8\u0002\u00a9\u0007\u00a9\u0002\u00aa\u0007"+
		"\u00aa\u0002\u00ab\u0007\u00ab\u0002\u00ac\u0007\u00ac\u0002\u00ad\u0007"+
		"\u00ad\u0002\u00ae\u0007\u00ae\u0002\u00af\u0007\u00af\u0002\u00b0\u0007"+
		"\u00b0\u0002\u00b1\u0007\u00b1\u0002\u00b2\u0007\u00b2\u0002\u00b3\u0007"+
		"\u00b3\u0002\u00b4\u0007\u00b4\u0002\u00b5\u0007\u00b5\u0002\u00b6\u0007"+
		"\u00b6\u0002\u00b7\u0007\u00b7\u0002\u00b8\u0007\u00b8\u0002\u00b9\u0007"+
		"\u00b9\u0002\u00ba\u0007\u00ba\u0002\u00bb\u0007\u00bb\u0002\u00bc\u0007"+
		"\u00bc\u0002\u00bd\u0007\u00bd\u0002\u00be\u0007\u00be\u0002\u00bf\u0007"+
		"\u00bf\u0002\u00c0\u0007\u00c0\u0002\u00c1\u0007\u00c1\u0002\u00c2\u0007"+
		"\u00c2\u0002\u00c3\u0007\u00c3\u0002\u00c4\u0007\u00c4\u0002\u00c5\u0007"+
		"\u00c5\u0002\u00c6\u0007\u00c6\u0002\u00c7\u0007\u00c7\u0002\u00c8\u0007"+
		"\u00c8\u0002\u00c9\u0007\u00c9\u0002\u00ca\u0007\u00ca\u0002\u00cb\u0007"+
		"\u00cb\u0002\u00cc\u0007\u00cc\u0002\u00cd\u0007\u00cd\u0002\u00ce\u0007"+
		"\u00ce\u0002\u00cf\u0007\u00cf\u0002\u00d0\u0007\u00d0\u0002\u00d1\u0007"+
		"\u00d1\u0002\u00d2\u0007\u00d2\u0002\u00d3\u0007\u00d3\u0002\u00d4\u0007"+
		"\u00d4\u0002\u00d5\u0007\u00d5\u0002\u00d6\u0007\u00d6\u0002\u00d7\u0007"+
		"\u00d7\u0002\u00d8\u0007\u00d8\u0002\u00d9\u0007\u00d9\u0002\u00da\u0007"+
		"\u00da\u0002\u00db\u0007\u00db\u0002\u00dc\u0007\u00dc\u0002\u00dd\u0007"+
		"\u00dd\u0002\u00de\u0007\u00de\u0002\u00df\u0007\u00df\u0002\u00e0\u0007"+
		"\u00e0\u0002\u00e1\u0007\u00e1\u0002\u00e2\u0007\u00e2\u0002\u00e3\u0007"+
		"\u00e3\u0002\u00e4\u0007\u00e4\u0002\u00e5\u0007\u00e5\u0002\u00e6\u0007"+
		"\u00e6\u0002\u00e7\u0007\u00e7\u0002\u00e8\u0007\u00e8\u0002\u00e9\u0007"+
		"\u00e9\u0002\u00ea\u0007\u00ea\u0002\u00eb\u0007\u00eb\u0002\u00ec\u0007"+
		"\u00ec\u0002\u00ed\u0007\u00ed\u0002\u00ee\u0007\u00ee\u0002\u00ef\u0007"+
		"\u00ef\u0002\u00f0\u0007\u00f0\u0002\u00f1\u0007\u00f1\u0002\u00f2\u0007"+
		"\u00f2\u0002\u00f3\u0007\u00f3\u0002\u00f4\u0007\u00f4\u0002\u00f5\u0007"+
		"\u00f5\u0002\u00f6\u0007\u00f6\u0002\u00f7\u0007\u00f7\u0002\u00f8\u0007"+
		"\u00f8\u0002\u00f9\u0007\u00f9\u0002\u00fa\u0007\u00fa\u0002\u00fb\u0007"+
		"\u00fb\u0002\u00fc\u0007\u00fc\u0002\u00fd\u0007\u00fd\u0002\u00fe\u0007"+
		"\u00fe\u0002\u00ff\u0007\u00ff\u0002\u0100\u0007\u0100\u0002\u0101\u0007"+
		"\u0101\u0002\u0102\u0007\u0102\u0002\u0103\u0007\u0103\u0002\u0104\u0007"+
		"\u0104\u0002\u0105\u0007\u0105\u0002\u0106\u0007\u0106\u0002\u0107\u0007"+
		"\u0107\u0002\u0108\u0007\u0108\u0002\u0109\u0007\u0109\u0002\u010a\u0007"+
		"\u010a\u0002\u010b\u0007\u010b\u0002\u010c\u0007\u010c\u0002\u010d\u0007"+
		"\u010d\u0002\u010e\u0007\u010e\u0002\u010f\u0007\u010f\u0002\u0110\u0007"+
		"\u0110\u0002\u0111\u0007\u0111\u0002\u0112\u0007\u0112\u0002\u0113\u0007"+
		"\u0113\u0002\u0114\u0007\u0114\u0002\u0115\u0007\u0115\u0002\u0116\u0007"+
		"\u0116\u0002\u0117\u0007\u0117\u0002\u0118\u0007\u0118\u0002\u0119\u0007"+
		"\u0119\u0002\u011a\u0007\u011a\u0002\u011b\u0007\u011b\u0002\u011c\u0007"+
		"\u011c\u0002\u011d\u0007\u011d\u0002\u011e\u0007\u011e\u0002\u011f\u0007"+
		"\u011f\u0002\u0120\u0007\u0120\u0002\u0121\u0007\u0121\u0002\u0122\u0007"+
		"\u0122\u0002\u0123\u0007\u0123\u0002\u0124\u0007\u0124\u0002\u0125\u0007"+
		"\u0125\u0002\u0126\u0007\u0126\u0002\u0127\u0007\u0127\u0002\u0128\u0007"+
		"\u0128\u0002\u0129\u0007\u0129\u0002\u012a\u0007\u012a\u0002\u012b\u0007"+
		"\u012b\u0002\u012c\u0007\u012c\u0002\u012d\u0007\u012d\u0002\u012e\u0007"+
		"\u012e\u0002\u012f\u0007\u012f\u0002\u0130\u0007\u0130\u0002\u0131\u0007"+
		"\u0131\u0002\u0132\u0007\u0132\u0002\u0133\u0007\u0133\u0002\u0134\u0007"+
		"\u0134\u0002\u0135\u0007\u0135\u0002\u0136\u0007\u0136\u0002\u0137\u0007"+
		"\u0137\u0002\u0138\u0007\u0138\u0002\u0139\u0007\u0139\u0002\u013a\u0007"+
		"\u013a\u0002\u013b\u0007\u013b\u0002\u013c\u0007\u013c\u0002\u013d\u0007"+
		"\u013d\u0002\u013e\u0007\u013e\u0002\u013f\u0007\u013f\u0002\u0140\u0007"+
		"\u0140\u0002\u0141\u0007\u0141\u0002\u0142\u0007\u0142\u0002\u0143\u0007"+
		"\u0143\u0002\u0144\u0007\u0144\u0002\u0145\u0007\u0145\u0002\u0146\u0007"+
		"\u0146\u0002\u0147\u0007\u0147\u0002\u0148\u0007\u0148\u0002\u0149\u0007"+
		"\u0149\u0002\u014a\u0007\u014a\u0002\u014b\u0007\u014b\u0002\u014c\u0007"+
		"\u014c\u0002\u014d\u0007\u014d\u0002\u014e\u0007\u014e\u0002\u014f\u0007"+
		"\u014f\u0002\u0150\u0007\u0150\u0002\u0151\u0007\u0151\u0001\u0000\u0001"+
		"\u0000\u0001\u0000\u0005\u0000\u02a8\b\u0000\n\u0000\f\u0000\u02ab\t\u0000"+
		"\u0001\u0000\u0003\u0000\u02ae\b\u0000\u0001\u0000\u0001\u0000\u0001\u0001"+
		"\u0003\u0001\u02b3\b\u0001\u0001\u0001\u0001\u0001\u0001\u0002\u0001\u0002"+
		"\u0001\u0002\u0001\u0002\u0003\u0002\u02bb\b\u0002\u0001\u0003\u0001\u0003"+
		"\u0001\u0003\u0003\u0003\u02c0\b\u0003\u0001\u0003\u0005\u0003\u02c3\b"+
		"\u0003\n\u0003\f\u0003\u02c6\t\u0003\u0001\u0004\u0004\u0004\u02c9\b\u0004"+
		"\u000b\u0004\f\u0004\u02ca\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005"+
		"\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005"+
		"\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005"+
		"\u0001\u0005\u0003\u0005\u02de\b\u0005\u0001\u0006\u0001\u0006\u0003\u0006"+
		"\u02e2\b\u0006\u0001\u0006\u0001\u0006\u0001\u0007\u0001\u0007\u0001\u0007"+
		"\u0001\u0007\u0001\u0007\u0001\u0007\u0003\u0007\u02ec\b\u0007\u0001\b"+
		"\u0001\b\u0001\t\u0001\t\u0001\t\u0001\n\u0003\n\u02f4\b\n\u0001\n\u0001"+
		"\n\u0003\n\u02f8\b\n\u0001\n\u0003\n\u02fb\b\n\u0001\n\u0003\n\u02fe\b"+
		"\n\u0001\u000b\u0001\u000b\u0001\u000b\u0003\u000b\u0303\b\u000b\u0001"+
		"\f\u0001\f\u0003\f\u0307\b\f\u0001\f\u0001\f\u0005\f\u030b\b\f\n\f\f\f"+
		"\u030e\t\f\u0001\r\u0001\r\u0001\r\u0003\r\u0313\b\r\u0001\u000e\u0001"+
		"\u000e\u0001\u000f\u0001\u000f\u0001\u0010\u0001\u0010\u0001\u0010\u0001"+
		"\u0010\u0001\u0010\u0005\u0010\u031e\b\u0010\n\u0010\f\u0010\u0321\t\u0010"+
		"\u0001\u0011\u0001\u0011\u0001\u0011\u0001\u0012\u0001\u0012\u0001\u0012"+
		"\u0001\u0013\u0001\u0013\u0001\u0013\u0001\u0014\u0001\u0014\u0001\u0014"+
		"\u0003\u0014\u032f\b\u0014\u0001\u0015\u0001\u0015\u0001\u0015\u0001\u0016"+
		"\u0001\u0016\u0001\u0016\u0001\u0017\u0001\u0017\u0001\u0017\u0001\u0017"+
		"\u0005\u0017\u033b\b\u0017\n\u0017\f\u0017\u033e\t\u0017\u0001\u0018\u0001"+
		"\u0018\u0001\u0018\u0001\u0018\u0001\u0018\u0001\u0018\u0001\u0018\u0001"+
		"\u0018\u0001\u0018\u0001\u0018\u0001\u0018\u0001\u0018\u0001\u0018\u0001"+
		"\u0018\u0001\u0018\u0001\u0018\u0001\u0018\u0001\u0018\u0001\u0018\u0001"+
		"\u0018\u0001\u0018\u0001\u0018\u0003\u0018\u0356\b\u0018\u0001\u0019\u0001"+
		"\u0019\u0001\u0019\u0001\u0019\u0005\u0019\u035c\b\u0019\n\u0019\f\u0019"+
		"\u035f\t\u0019\u0001\u001a\u0001\u001a\u0001\u001a\u0001\u001a\u0001\u001a"+
		"\u0001\u001a\u0001\u001a\u0001\u001a\u0003\u001a\u0369\b\u001a\u0001\u001b"+
		"\u0003\u001b\u036c\b\u001b\u0001\u001b\u0001\u001b\u0001\u001b\u0001\u001b"+
		"\u0005\u001b\u0372\b\u001b\n\u001b\f\u001b\u0375\t\u001b\u0001\u001c\u0003"+
		"\u001c\u0378\b\u001c\u0001\u001c\u0001\u001c\u0003\u001c\u037c\b\u001c"+
		"\u0001\u001c\u0001\u001c\u0005\u001c\u0380\b\u001c\n\u001c\f\u001c\u0383"+
		"\t\u001c\u0001\u001c\u0003\u001c\u0386\b\u001c\u0001\u001d\u0001\u001d"+
		"\u0001\u001d\u0003\u001d\u038b\b\u001d\u0001\u001d\u0003\u001d\u038e\b"+
		"\u001d\u0001\u001d\u0001\u001d\u0001\u001d\u0003\u001d\u0393\b\u001d\u0001"+
		"\u001d\u0003\u001d\u0396\b\u001d\u0003\u001d\u0398\b\u001d\u0001\u001e"+
		"\u0001\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0001\u001e"+
		"\u0001\u001e\u0001\u001e\u0001\u001e\u0003\u001e\u03a4\b\u001e\u0001\u001e"+
		"\u0003\u001e\u03a7\b\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0001\u001e"+
		"\u0001\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0001\u001e"+
		"\u0001\u001e\u0001\u001e\u0001\u001e\u0003\u001e\u03b6\b\u001e\u0001\u001f"+
		"\u0001\u001f\u0001\u001f\u0005\u001f\u03bb\b\u001f\n\u001f\f\u001f\u03be"+
		"\t\u001f\u0001 \u0001 \u0001 \u0001 \u0001!\u0001!\u0001!\u0001!\u0001"+
		"!\u0001\"\u0003\"\u03ca\b\"\u0001\"\u0001\"\u0001\"\u0001\"\u0001\"\u0001"+
		"\"\u0005\"\u03d2\b\"\n\"\f\"\u03d5\t\"\u0003\"\u03d7\b\"\u0001\"\u0003"+
		"\"\u03da\b\"\u0001\"\u0001\"\u0001\"\u0001\"\u0001\"\u0005\"\u03e1\b\""+
		"\n\"\f\"\u03e4\t\"\u0001\"\u0003\"\u03e7\b\"\u0003\"\u03e9\b\"\u0003\""+
		"\u03eb\b\"\u0001#\u0001#\u0001#\u0001$\u0001$\u0001%\u0001%\u0001%\u0003"+
		"%\u03f5\b%\u0001&\u0001&\u0001&\u0001&\u0003&\u03fb\b&\u0001&\u0001&\u0001"+
		"&\u0001&\u0001&\u0001&\u0003&\u0403\b&\u0001\'\u0001\'\u0001\'\u0001\'"+
		"\u0001\'\u0001\'\u0001\'\u0004\'\u040c\b\'\u000b\'\f\'\u040d\u0001\'\u0001"+
		"\'\u0001(\u0003(\u0413\b(\u0001(\u0001(\u0003(\u0417\b(\u0001(\u0001("+
		"\u0001(\u0001(\u0003(\u041d\b(\u0001)\u0001)\u0001)\u0001)\u0001)\u0005"+
		")\u0424\b)\n)\f)\u0427\t)\u0003)\u0429\b)\u0001)\u0001)\u0001*\u0001*"+
		"\u0003*\u042f\b*\u0001*\u0003*\u0432\b*\u0001*\u0001*\u0001*\u0001*\u0005"+
		"*\u0438\b*\n*\f*\u043b\t*\u0001+\u0001+\u0001+\u0001+\u0001,\u0001,\u0001"+
		",\u0001,\u0001-\u0001-\u0001-\u0001-\u0001-\u0001.\u0001.\u0003.\u044c"+
		"\b.\u0001.\u0003.\u044f\b.\u0001.\u0001.\u0003.\u0453\b.\u0001.\u0003"+
		".\u0456\b.\u0001/\u0001/\u0001/\u0005/\u045b\b/\n/\f/\u045e\t/\u00010"+
		"\u00010\u00010\u00050\u0463\b0\n0\f0\u0466\t0\u00011\u00011\u00011\u0003"+
		"1\u046b\b1\u00011\u00031\u046e\b1\u00011\u00011\u00012\u00012\u00012\u0003"+
		"2\u0475\b2\u00012\u00012\u00012\u00012\u00052\u047b\b2\n2\f2\u047e\t2"+
		"\u00013\u00013\u00013\u00013\u00013\u00033\u0485\b3\u00013\u00013\u0003"+
		"3\u0489\b3\u00013\u00013\u00013\u00033\u048e\b3\u00014\u00014\u00034\u0492"+
		"\b4\u00015\u00015\u00015\u00015\u00015\u00016\u00016\u00016\u00036\u049c"+
		"\b6\u00016\u00016\u00056\u04a0\b6\n6\f6\u04a3\t6\u00016\u00046\u04a6\b"+
		"6\u000b6\f6\u04a7\u00017\u00017\u00017\u00037\u04ad\b7\u00017\u00017\u0001"+
		"7\u00037\u04b2\b7\u00017\u00017\u00037\u04b6\b7\u00017\u00037\u04b9\b"+
		"7\u00017\u00017\u00037\u04bd\b7\u00017\u00017\u00037\u04c1\b7\u00017\u0003"+
		"7\u04c4\b7\u00017\u00017\u00017\u00017\u00037\u04ca\b7\u00037\u04cc\b"+
		"7\u00018\u00018\u00019\u00019\u0001:\u0001:\u0001:\u0001:\u0004:\u04d6"+
		"\b:\u000b:\f:\u04d7\u0001;\u0001;\u0003;\u04dc\b;\u0001;\u0003;\u04df"+
		"\b;\u0001;\u0003;\u04e2\b;\u0001;\u0001;\u0003;\u04e6\b;\u0001;\u0001"+
		";\u0001<\u0001<\u0003<\u04ec\b<\u0001<\u0003<\u04ef\b<\u0001<\u0003<\u04f2"+
		"\b<\u0001<\u0001<\u0001=\u0001=\u0001=\u0001=\u0003=\u04fa\b=\u0001=\u0001"+
		"=\u0003=\u04fe\b=\u0001>\u0001>\u0004>\u0502\b>\u000b>\f>\u0503\u0001"+
		"?\u0001?\u0001?\u0003?\u0509\b?\u0001?\u0001?\u0005?\u050d\b?\n?\f?\u0510"+
		"\t?\u0001@\u0001@\u0001@\u0001@\u0001@\u0001A\u0001A\u0003A\u0519\bA\u0001"+
		"A\u0001A\u0001A\u0001A\u0001B\u0001B\u0001B\u0001C\u0001C\u0001C\u0001"+
		"D\u0001D\u0001D\u0001E\u0001E\u0001E\u0001F\u0001F\u0003F\u052d\bF\u0001"+
		"G\u0003G\u0530\bG\u0001G\u0001G\u0001G\u0003G\u0535\bG\u0001G\u0003G\u0538"+
		"\bG\u0001G\u0003G\u053b\bG\u0001G\u0003G\u053e\bG\u0001G\u0001G\u0003"+
		"G\u0542\bG\u0001G\u0003G\u0545\bG\u0001G\u0001G\u0003G\u0549\bG\u0001"+
		"H\u0003H\u054c\bH\u0001H\u0001H\u0001H\u0003H\u0551\bH\u0001H\u0001H\u0003"+
		"H\u0555\bH\u0001H\u0001H\u0001H\u0003H\u055a\bH\u0001I\u0001I\u0001J\u0001"+
		"J\u0001K\u0001K\u0001L\u0001L\u0003L\u0564\bL\u0001L\u0001L\u0003L\u0568"+
		"\bL\u0001L\u0003L\u056b\bL\u0001M\u0001M\u0001M\u0001M\u0003M\u0571\b"+
		"M\u0001N\u0001N\u0001N\u0003N\u0576\bN\u0001N\u0005N\u0579\bN\nN\fN\u057c"+
		"\tN\u0001O\u0001O\u0001O\u0003O\u0581\bO\u0001O\u0005O\u0584\bO\nO\fO"+
		"\u0587\tO\u0001P\u0001P\u0001P\u0005P\u058c\bP\nP\fP\u058f\tP\u0001Q\u0001"+
		"Q\u0001Q\u0005Q\u0594\bQ\nQ\fQ\u0597\tQ\u0001R\u0005R\u059a\bR\nR\fR\u059d"+
		"\tR\u0001R\u0001R\u0001S\u0005S\u05a2\bS\nS\fS\u05a5\tS\u0001S\u0001S"+
		"\u0001T\u0001T\u0001T\u0001T\u0001T\u0001T\u0001T\u0003T\u05b0\bT\u0001"+
		"U\u0001U\u0001U\u0001U\u0001U\u0001U\u0001U\u0003U\u05b9\bU\u0001V\u0001"+
		"V\u0001V\u0001V\u0005V\u05bf\bV\nV\fV\u05c2\tV\u0001W\u0001W\u0001W\u0001"+
		"X\u0001X\u0001X\u0005X\u05ca\bX\nX\fX\u05cd\tX\u0001Y\u0001Y\u0001Y\u0005"+
		"Y\u05d2\bY\nY\fY\u05d5\tY\u0001Z\u0001Z\u0001Z\u0005Z\u05da\bZ\nZ\fZ\u05dd"+
		"\tZ\u0001[\u0005[\u05e0\b[\n[\f[\u05e3\t[\u0001[\u0001[\u0001\\\u0001"+
		"\\\u0001\\\u0005\\\u05ea\b\\\n\\\f\\\u05ed\t\\\u0001]\u0001]\u0003]\u05f1"+
		"\b]\u0001^\u0001^\u0001^\u0001^\u0001^\u0001^\u0001^\u0003^\u05fa\b^\u0001"+
		"^\u0001^\u0001^\u0003^\u05ff\b^\u0001^\u0001^\u0001^\u0003^\u0604\b^\u0001"+
		"^\u0001^\u0003^\u0608\b^\u0001^\u0001^\u0001^\u0003^\u060d\b^\u0001^\u0003"+
		"^\u0610\b^\u0001^\u0003^\u0613\b^\u0001_\u0001_\u0001`\u0001`\u0001`\u0005"+
		"`\u061a\b`\n`\f`\u061d\t`\u0001a\u0001a\u0001a\u0005a\u0622\ba\na\fa\u0625"+
		"\ta\u0001b\u0001b\u0001b\u0005b\u062a\bb\nb\fb\u062d\tb\u0001c\u0001c"+
		"\u0001c\u0003c\u0632\bc\u0001d\u0001d\u0005d\u0636\bd\nd\fd\u0639\td\u0001"+
		"e\u0001e\u0001e\u0001e\u0001e\u0001e\u0001e\u0001e\u0003e\u0643\be\u0001"+
		"e\u0001e\u0003e\u0647\be\u0001e\u0003e\u064a\be\u0001f\u0001f\u0001f\u0001"+
		"g\u0001g\u0001g\u0001g\u0001h\u0001h\u0004h\u0655\bh\u000bh\fh\u0656\u0001"+
		"i\u0001i\u0001i\u0001j\u0001j\u0001j\u0001j\u0001j\u0001j\u0001j\u0001"+
		"j\u0001j\u0001j\u0001j\u0001j\u0001j\u0001j\u0001j\u0001j\u0001j\u0001"+
		"j\u0001j\u0001j\u0001j\u0003j\u0671\bj\u0001k\u0001k\u0001k\u0001k\u0001"+
		"k\u0001k\u0001k\u0001k\u0001k\u0003k\u067c\bk\u0001l\u0001l\u0004l\u0680"+
		"\bl\u000bl\fl\u0681\u0001l\u0001l\u0003l\u0686\bl\u0001l\u0001l\u0001"+
		"m\u0001m\u0001m\u0001m\u0001m\u0001n\u0001n\u0001n\u0004n\u0692\bn\u000b"+
		"n\fn\u0693\u0001n\u0001n\u0003n\u0698\bn\u0001n\u0001n\u0001o\u0001o\u0001"+
		"o\u0001o\u0005o\u06a0\bo\no\fo\u06a3\to\u0001o\u0001o\u0001o\u0001p\u0001"+
		"p\u0001p\u0001p\u0001p\u0003p\u06ad\bp\u0001p\u0001p\u0001p\u0003p\u06b2"+
		"\bp\u0001p\u0001p\u0001p\u0003p\u06b7\bp\u0001p\u0001p\u0003p\u06bb\b"+
		"p\u0001p\u0001p\u0001p\u0003p\u06c0\bp\u0001p\u0003p\u06c3\bp\u0001p\u0001"+
		"p\u0001p\u0001p\u0003p\u06c9\bp\u0001q\u0001q\u0001q\u0001q\u0001q\u0001"+
		"q\u0003q\u06d1\bq\u0001q\u0001q\u0001q\u0001q\u0003q\u06d7\bq\u0003q\u06d9"+
		"\bq\u0001q\u0001q\u0001r\u0001r\u0001r\u0001r\u0003r\u06e1\br\u0001r\u0001"+
		"r\u0001r\u0003r\u06e6\br\u0001r\u0001r\u0001r\u0001r\u0001s\u0001s\u0001"+
		"s\u0001s\u0001s\u0001s\u0001s\u0001s\u0001s\u0001s\u0001s\u0001s\u0001"+
		"s\u0001t\u0001t\u0001t\u0001t\u0001t\u0001t\u0001t\u0003t\u0700\bt\u0001"+
		"t\u0001t\u0001u\u0001u\u0001u\u0001u\u0001u\u0003u\u0709\bu\u0001u\u0001"+
		"u\u0001v\u0001v\u0001v\u0003v\u0710\bv\u0001v\u0003v\u0713\bv\u0001v\u0003"+
		"v\u0716\bv\u0001v\u0001v\u0001v\u0001w\u0001w\u0001x\u0001x\u0001y\u0001"+
		"y\u0001y\u0001y\u0001z\u0001z\u0001z\u0001z\u0001z\u0005z\u0728\bz\nz"+
		"\fz\u072b\tz\u0003z\u072d\bz\u0001z\u0001z\u0001{\u0001{\u0001{\u0001"+
		"{\u0001{\u0001{\u0001{\u0001{\u0003{\u0739\b{\u0001|\u0001|\u0001|\u0001"+
		"|\u0001|\u0001}\u0001}\u0001}\u0001}\u0003}\u0744\b}\u0001}\u0001}\u0003"+
		"}\u0748\b}\u0003}\u074a\b}\u0001}\u0001}\u0001~\u0001~\u0001~\u0001~\u0003"+
		"~\u0752\b~\u0001~\u0001~\u0003~\u0756\b~\u0003~\u0758\b~\u0001~\u0001"+
		"~\u0001\u007f\u0001\u007f\u0001\u007f\u0001\u007f\u0001\u007f\u0001\u0080"+
		"\u0003\u0080\u0762\b\u0080\u0001\u0080\u0001\u0080\u0001\u0081\u0003\u0081"+
		"\u0767\b\u0081\u0001\u0081\u0001\u0081\u0001\u0082\u0001\u0082\u0001\u0082"+
		"\u0001\u0082\u0005\u0082\u076f\b\u0082\n\u0082\f\u0082\u0772\t\u0082\u0003"+
		"\u0082\u0774\b\u0082\u0001\u0082\u0001\u0082\u0001\u0083\u0001\u0083\u0001"+
		"\u0084\u0001\u0084\u0001\u0084\u0001\u0085\u0001\u0085\u0003\u0085\u077f"+
		"\b\u0085\u0001\u0086\u0001\u0086\u0001\u0086\u0003\u0086\u0784\b\u0086"+
		"\u0001\u0086\u0001\u0086\u0001\u0086\u0005\u0086\u0789\b\u0086\n\u0086"+
		"\f\u0086\u078c\t\u0086\u0003\u0086\u078e\b\u0086\u0001\u0086\u0001\u0086"+
		"\u0001\u0087\u0001\u0087\u0001\u0088\u0001\u0088\u0001\u0088\u0001\u0089"+
		"\u0001\u0089\u0001\u0089\u0005\u0089\u079a\b\u0089\n\u0089\f\u0089\u079d"+
		"\t\u0089\u0001\u008a\u0001\u008a\u0001\u008b\u0001\u008b\u0001\u008b\u0005"+
		"\u008b\u07a4\b\u008b\n\u008b\f\u008b\u07a7\t\u008b\u0001\u008c\u0001\u008c"+
		"\u0001\u008c\u0005\u008c\u07ac\b\u008c\n\u008c\f\u008c\u07af\t\u008c\u0001"+
		"\u008d\u0001\u008d\u0003\u008d\u07b3\b\u008d\u0001\u008d\u0005\u008d\u07b6"+
		"\b\u008d\n\u008d\f\u008d\u07b9\t\u008d\u0001\u008e\u0001\u008e\u0001\u008e"+
		"\u0001\u008e\u0001\u008e\u0001\u008e\u0001\u008e\u0001\u008e\u0003\u008e"+
		"\u07c3\b\u008e\u0001\u008e\u0001\u008e\u0001\u008e\u0001\u008e\u0001\u008e"+
		"\u0001\u008e\u0001\u008e\u0001\u008e\u0001\u008e\u0001\u008e\u0001\u008e"+
		"\u0001\u008e\u0003\u008e\u07d1\b\u008e\u0001\u008e\u0001\u008e\u0001\u008e"+
		"\u0001\u008e\u0001\u008e\u0003\u008e\u07d8\b\u008e\u0001\u008e\u0001\u008e"+
		"\u0001\u008e\u0001\u008e\u0001\u008e\u0001\u008e\u0001\u008e\u0001\u008e"+
		"\u0001\u008e\u0001\u008e\u0001\u008e\u0001\u008e\u0001\u008e\u0001\u008e"+
		"\u0001\u008e\u0001\u008e\u0001\u008e\u0001\u008e\u0001\u008e\u0001\u008e"+
		"\u0001\u008e\u0001\u008e\u0001\u008e\u0001\u008e\u0001\u008e\u0003\u008e"+
		"\u07f3\b\u008e\u0001\u008e\u0001\u008e\u0001\u008e\u0001\u008e\u0001\u008e"+
		"\u0003\u008e\u07fa\b\u008e\u0003\u008e\u07fc\b\u008e\u0001\u008f\u0001"+
		"\u008f\u0001\u008f\u0003\u008f\u0801\b\u008f\u0001\u0090\u0001\u0090\u0003"+
		"\u0090\u0805\b\u0090\u0001\u0091\u0003\u0091\u0808\b\u0091\u0001\u0091"+
		"\u0001\u0091\u0001\u0091\u0001\u0091\u0001\u0091\u0001\u0091\u0001\u0091"+
		"\u0001\u0091\u0001\u0091\u0001\u0091\u0001\u0091\u0001\u0091\u0001\u0091"+
		"\u0003\u0091\u0817\b\u0091\u0001\u0092\u0001\u0092\u0001\u0093\u0001\u0093"+
		"\u0001\u0094\u0001\u0094\u0001\u0095\u0001\u0095\u0001\u0096\u0001\u0096"+
		"\u0001\u0097\u0001\u0097\u0001\u0097\u0001\u0098\u0001\u0098\u0001\u0098"+
		"\u0001\u0099\u0001\u0099\u0001\u009a\u0001\u009a\u0001\u009a\u0001\u009b"+
		"\u0001\u009b\u0001\u009b\u0001\u009c\u0001\u009c\u0003\u009c\u0833\b\u009c"+
		"\u0001\u009d\u0001\u009d\u0001\u009e\u0001\u009e\u0001\u009e\u0001\u009e"+
		"\u0001\u009e\u0003\u009e\u083c\b\u009e\u0001\u009f\u0001\u009f\u0001\u00a0"+
		"\u0001\u00a0\u0001\u00a1\u0001\u00a1\u0001\u00a2\u0001\u00a2\u0001\u00a3"+
		"\u0001\u00a3\u0001\u00a4\u0001\u00a4\u0001\u00a5\u0001\u00a5\u0001\u00a6"+
		"\u0001\u00a6\u0001\u00a7\u0001\u00a7\u0001\u00a8\u0001\u00a8\u0001\u00a9"+
		"\u0001\u00a9\u0001\u00aa\u0001\u00aa\u0001\u00ab\u0001\u00ab\u0001\u00ac"+
		"\u0001\u00ac\u0001\u00ad\u0001\u00ad\u0001\u00ae\u0001\u00ae\u0001\u00af"+
		"\u0001\u00af\u0003\u00af\u0860\b\u00af\u0001\u00b0\u0001\u00b0\u0001\u00b1"+
		"\u0001\u00b1\u0001\u00b2\u0003\u00b2\u0867\b\u00b2\u0001\u00b3\u0001\u00b3"+
		"\u0003\u00b3\u086b\b\u00b3\u0001\u00b4\u0001\u00b4\u0003\u00b4\u086f\b"+
		"\u00b4\u0001\u00b4\u0001\u00b4\u0001\u00b5\u0001\u00b5\u0001\u00b5\u0001"+
		"\u00b6\u0001\u00b6\u0001\u00b7\u0001\u00b7\u0001\u00b7\u0001\u00b7\u0001"+
		"\u00b7\u0003\u00b7\u087d\b\u00b7\u0001\u00b8\u0001\u00b8\u0001\u00b9\u0001"+
		"\u00b9\u0001\u00ba\u0001\u00ba\u0001\u00ba\u0001\u00ba\u0001\u00ba\u0001"+
		"\u00bb\u0001\u00bb\u0001\u00bc\u0001\u00bc\u0001\u00bd\u0001\u00bd\u0003"+
		"\u00bd\u088e\b\u00bd\u0001\u00bd\u0001\u00bd\u0001\u00be\u0001\u00be\u0001"+
		"\u00be\u0001\u00bf\u0001\u00bf\u0001\u00c0\u0001\u00c0\u0003\u00c0\u0899"+
		"\b\u00c0\u0001\u00c0\u0001\u00c0\u0001\u00c1\u0001\u00c1\u0001\u00c1\u0001"+
		"\u00c2\u0001\u00c2\u0001\u00c3\u0001\u00c3\u0001\u00c3\u0001\u00c4\u0001"+
		"\u00c4\u0001\u00c4\u0001\u00c4\u0001\u00c4\u0001\u00c4\u0005\u00c4\u08ab"+
		"\b\u00c4\n\u00c4\f\u00c4\u08ae\t\u00c4\u0001\u00c5\u0001\u00c5\u0001\u00c6"+
		"\u0001\u00c6\u0001\u00c7\u0001\u00c7\u0001\u00c8\u0001\u00c8\u0001\u00c9"+
		"\u0001\u00c9\u0001\u00ca\u0001\u00ca\u0001\u00cb\u0001\u00cb\u0001\u00cc"+
		"\u0001\u00cc\u0001\u00cd\u0001\u00cd\u0001\u00cd\u0001\u00cd\u0001\u00ce"+
		"\u0001\u00ce\u0001\u00ce\u0001\u00cf\u0001\u00cf\u0001\u00cf\u0001\u00d0"+
		"\u0001\u00d0\u0001\u00d0\u0001\u00d1\u0001\u00d1\u0001\u00d2\u0001\u00d2"+
		"\u0001\u00d3\u0001\u00d3\u0001\u00d3\u0001\u00d3\u0003\u00d3\u08d5\b\u00d3"+
		"\u0001\u00d4\u0001\u00d4\u0001\u00d4\u0001\u00d4\u0003\u00d4\u08db\b\u00d4"+
		"\u0001\u00d5\u0001\u00d5\u0001\u00d5\u0001\u00d6\u0001\u00d6\u0001\u00d6"+
		"\u0001\u00d7\u0001\u00d7\u0001\u00d7\u0001\u00d7\u0001\u00d7\u0001\u00d8"+
		"\u0001\u00d8\u0001\u00d9\u0001\u00d9\u0001\u00d9\u0001\u00d9\u0001\u00da"+
		"\u0001\u00da\u0001\u00da\u0001\u00da\u0001\u00db\u0001\u00db\u0001\u00db"+
		"\u0001\u00dc\u0001\u00dc\u0001\u00dc\u0001\u00dd\u0001\u00dd\u0001\u00dd"+
		"\u0001\u00dd\u0001\u00dd\u0001\u00de\u0001\u00de\u0001\u00de\u0001\u00df"+
		"\u0001\u00df\u0001\u00df\u0001\u00e0\u0001\u00e0\u0001\u00e1\u0001\u00e1"+
		"\u0001\u00e1\u0001\u00e2\u0001\u00e2\u0001\u00e2\u0001\u00e3\u0001\u00e3"+
		"\u0003\u00e3\u090d\b\u00e3\u0001\u00e4\u0001\u00e4\u0001\u00e4\u0001\u00e5"+
		"\u0001\u00e5\u0001\u00e5\u0001\u00e5\u0003\u00e5\u0916\b\u00e5\u0001\u00e6"+
		"\u0001\u00e6\u0001\u00e6\u0001\u00e6\u0001\u00e7\u0001\u00e7\u0001\u00e8"+
		"\u0001\u00e8\u0001\u00e8\u0001\u00e9\u0001\u00e9\u0001\u00ea\u0001\u00ea"+
		"\u0001\u00ea\u0001\u00eb\u0001\u00eb\u0001\u00eb\u0001\u00ec\u0001\u00ec"+
		"\u0001\u00ed\u0001\u00ed\u0001\u00ed\u0001\u00ee\u0001\u00ee\u0001\u00ee"+
		"\u0001\u00ee\u0003\u00ee\u0932\b\u00ee\u0001\u00ef\u0001\u00ef\u0001\u00ef"+
		"\u0001\u00ef\u0003\u00ef\u0938\b\u00ef\u0001\u00f0\u0001\u00f0\u0001\u00f1"+
		"\u0001\u00f1\u0001\u00f2\u0001\u00f2\u0001\u00f2\u0001\u00f2\u0001\u00f3"+
		"\u0001\u00f3\u0001\u00f4\u0001\u00f4\u0001\u00f5\u0001\u00f5\u0001\u00f5"+
		"\u0001\u00f6\u0001\u00f6\u0003\u00f6\u094b\b\u00f6\u0001\u00f7\u0001\u00f7"+
		"\u0001\u00f7\u0001\u00f8\u0001\u00f8\u0001\u00f8\u0001\u00f9\u0001\u00f9"+
		"\u0001\u00f9\u0001\u00fa\u0001\u00fa\u0001\u00fb\u0001\u00fb\u0001\u00fb"+
		"\u0001\u00fc\u0001\u00fc\u0001\u00fd\u0001\u00fd\u0001\u00fd\u0001\u00fe"+
		"\u0001\u00fe\u0001\u00fe\u0001\u00ff\u0001\u00ff\u0001\u0100\u0001\u0100"+
		"\u0001\u0100\u0001\u0101\u0001\u0101\u0001\u0102\u0001\u0102\u0001\u0103"+
		"\u0001\u0103\u0001\u0104\u0001\u0104\u0001\u0105\u0001\u0105\u0001\u0106"+
		"\u0001\u0106\u0001\u0107\u0001\u0107\u0001\u0108\u0001\u0108\u0001\u0109"+
		"\u0001\u0109\u0001\u010a\u0001\u010a\u0001\u010a\u0001\u010a\u0001\u010b"+
		"\u0001\u010b\u0001\u010c\u0001\u010c\u0001\u010d\u0001\u010d\u0001\u010e"+
		"\u0001\u010e\u0001\u010e\u0005\u010e\u0987\b\u010e\n\u010e\f\u010e\u098a"+
		"\t\u010e\u0001\u010f\u0001\u010f\u0003\u010f\u098e\b\u010f\u0001\u010f"+
		"\u0003\u010f\u0991\b\u010f\u0001\u0110\u0001\u0110\u0003\u0110\u0995\b"+
		"\u0110\u0001\u0111\u0001\u0111\u0003\u0111\u0999\b\u0111\u0001\u0111\u0001"+
		"\u0111\u0001\u0111\u0003\u0111\u099e\b\u0111\u0001\u0112\u0001\u0112\u0001"+
		"\u0113\u0001\u0113\u0001\u0114\u0001\u0114\u0003\u0114\u09a6\b\u0114\u0001"+
		"\u0115\u0001\u0115\u0001\u0115\u0003\u0115\u09ab\b\u0115\u0001\u0115\u0001"+
		"\u0115\u0001\u0116\u0001\u0116\u0001\u0116\u0005\u0116\u09b2\b\u0116\n"+
		"\u0116\f\u0116\u09b5\t\u0116\u0001\u0117\u0003\u0117\u09b8\b\u0117\u0001"+
		"\u0118\u0001\u0118\u0001\u0118\u0003\u0118\u09bd\b\u0118\u0001\u0119\u0001"+
		"\u0119\u0001\u011a\u0001\u011a\u0001\u011b\u0001\u011b\u0001\u011c\u0001"+
		"\u011c\u0001\u011d\u0001\u011d\u0001\u011e\u0001\u011e\u0001\u011e\u0001"+
		"\u011e\u0001\u011f\u0001\u011f\u0001\u011f\u0001\u0120\u0001\u0120\u0001"+
		"\u0120\u0001\u0121\u0001\u0121\u0001\u0122\u0001\u0122\u0001\u0122\u0001"+
		"\u0123\u0001\u0123\u0001\u0124\u0001\u0124\u0001\u0124\u0001\u0125\u0001"+
		"\u0125\u0001\u0125\u0001\u0125\u0001\u0125\u0003\u0125\u09e2\b\u0125\u0001"+
		"\u0126\u0001\u0126\u0001\u0126\u0001\u0127\u0001\u0127\u0001\u0127\u0001"+
		"\u0127\u0001\u0127\u0001\u0127\u0003\u0127\u09ed\b\u0127\u0001\u0128\u0001"+
		"\u0128\u0001\u0128\u0004\u0128\u09f2\b\u0128\u000b\u0128\f\u0128\u09f3"+
		"\u0001\u0129\u0001\u0129\u0001\u0129\u0001\u0129\u0001\u012a\u0001\u012a"+
		"\u0001\u012a\u0001\u012a\u0001\u012b\u0001\u012b\u0001\u012b\u0001\u012b"+
		"\u0001\u012c\u0001\u012c\u0001\u012c\u0003\u012c\u0a05\b\u012c\u0003\u012c"+
		"\u0a07\b\u012c\u0001\u012c\u0003\u012c\u0a0a\b\u012c\u0001\u012d\u0001"+
		"\u012d\u0001\u012e\u0001\u012e\u0001\u012f\u0001\u012f\u0001\u0130\u0001"+
		"\u0130\u0001\u0131\u0001\u0131\u0001\u0131\u0001\u0131\u0001\u0131\u0001"+
		"\u0131\u0001\u0132\u0001\u0132\u0001\u0132\u0001\u0132\u0001\u0132\u0001"+
		"\u0133\u0001\u0133\u0001\u0133\u0001\u0133\u0001\u0133\u0001\u0133\u0001"+
		"\u0134\u0001\u0134\u0001\u0134\u0001\u0135\u0001\u0135\u0001\u0135\u0001"+
		"\u0136\u0001\u0136\u0001\u0136\u0001\u0137\u0001\u0137\u0001\u0137\u0001"+
		"\u0138\u0001\u0138\u0001\u0138\u0001\u0139\u0001\u0139\u0001\u013a\u0001"+
		"\u013a\u0003\u013a\u0a38\b\u013a\u0001\u013b\u0001\u013b\u0003\u013b\u0a3c"+
		"\b\u013b\u0001\u013c\u0001\u013c\u0001\u013c\u0005\u013c\u0a41\b\u013c"+
		"\n\u013c\f\u013c\u0a44\t\u013c\u0001\u013d\u0001\u013d\u0001\u013d\u0005"+
		"\u013d\u0a49\b\u013d\n\u013d\f\u013d\u0a4c\t\u013d\u0001\u013e\u0001\u013e"+
		"\u0003\u013e\u0a50\b\u013e\u0001\u013f\u0001\u013f\u0001\u013f\u0005\u013f"+
		"\u0a55\b\u013f\n\u013f\f\u013f\u0a58\t\u013f\u0001\u0140\u0001\u0140\u0001"+
		"\u0140\u0001\u0140\u0005\u0140\u0a5e\b\u0140\n\u0140\f\u0140\u0a61\t\u0140"+
		"\u0003\u0140\u0a63\b\u0140\u0001\u0140\u0001\u0140\u0001\u0141\u0001\u0141"+
		"\u0001\u0141\u0004\u0141\u0a6a\b\u0141\u000b\u0141\f\u0141\u0a6b\u0001"+
		"\u0142\u0001\u0142\u0001\u0143\u0001\u0143\u0003\u0143\u0a72\b\u0143\u0001"+
		"\u0144\u0001\u0144\u0003\u0144\u0a76\b\u0144\u0001\u0145\u0001\u0145\u0003"+
		"\u0145\u0a7a\b\u0145\u0001\u0146\u0001\u0146\u0003\u0146\u0a7e\b\u0146"+
		"\u0001\u0147\u0001\u0147\u0001\u0147\u0001\u0147\u0001\u0147\u0001\u0147"+
		"\u0001\u0147\u0001\u0147\u0001\u0147\u0005\u0147\u0a89\b\u0147\n\u0147"+
		"\f\u0147\u0a8c\t\u0147\u0003\u0147\u0a8e\b\u0147\u0001\u0147\u0001\u0147"+
		"\u0001\u0148\u0001\u0148\u0003\u0148\u0a94\b\u0148\u0001\u0149\u0001\u0149"+
		"\u0001\u014a\u0001\u014a\u0001\u014b\u0001\u014b\u0003\u014b\u0a9c\b\u014b"+
		"\u0001\u014c\u0001\u014c\u0001\u014d\u0001\u014d\u0001\u014d\u0001\u014d"+
		"\u0001\u014d\u0001\u014d\u0001\u014d\u0001\u014d\u0001\u014d\u0003\u014d"+
		"\u0aa9\b\u014d\u0001\u014e\u0001\u014e\u0003\u014e\u0aad\b\u014e\u0001"+
		"\u014f\u0001\u014f\u0001\u0150\u0001\u0150\u0001\u0151\u0001\u0151\u0001"+
		"\u0151\u0000\u0000\u0152\u0000\u0002\u0004\u0006\b\n\f\u000e\u0010\u0012"+
		"\u0014\u0016\u0018\u001a\u001c\u001e \"$&(*,.02468:<>@BDFHJLNPRTVXZ\\"+
		"^`bdfhjlnprtvxz|~\u0080\u0082\u0084\u0086\u0088\u008a\u008c\u008e\u0090"+
		"\u0092\u0094\u0096\u0098\u009a\u009c\u009e\u00a0\u00a2\u00a4\u00a6\u00a8"+
		"\u00aa\u00ac\u00ae\u00b0\u00b2\u00b4\u00b6\u00b8\u00ba\u00bc\u00be\u00c0"+
		"\u00c2\u00c4\u00c6\u00c8\u00ca\u00cc\u00ce\u00d0\u00d2\u00d4\u00d6\u00d8"+
		"\u00da\u00dc\u00de\u00e0\u00e2\u00e4\u00e6\u00e8\u00ea\u00ec\u00ee\u00f0"+
		"\u00f2\u00f4\u00f6\u00f8\u00fa\u00fc\u00fe\u0100\u0102\u0104\u0106\u0108"+
		"\u010a\u010c\u010e\u0110\u0112\u0114\u0116\u0118\u011a\u011c\u011e\u0120"+
		"\u0122\u0124\u0126\u0128\u012a\u012c\u012e\u0130\u0132\u0134\u0136\u0138"+
		"\u013a\u013c\u013e\u0140\u0142\u0144\u0146\u0148\u014a\u014c\u014e\u0150"+
		"\u0152\u0154\u0156\u0158\u015a\u015c\u015e\u0160\u0162\u0164\u0166\u0168"+
		"\u016a\u016c\u016e\u0170\u0172\u0174\u0176\u0178\u017a\u017c\u017e\u0180"+
		"\u0182\u0184\u0186\u0188\u018a\u018c\u018e\u0190\u0192\u0194\u0196\u0198"+
		"\u019a\u019c\u019e\u01a0\u01a2\u01a4\u01a6\u01a8\u01aa\u01ac\u01ae\u01b0"+
		"\u01b2\u01b4\u01b6\u01b8\u01ba\u01bc\u01be\u01c0\u01c2\u01c4\u01c6\u01c8"+
		"\u01ca\u01cc\u01ce\u01d0\u01d2\u01d4\u01d6\u01d8\u01da\u01dc\u01de\u01e0"+
		"\u01e2\u01e4\u01e6\u01e8\u01ea\u01ec\u01ee\u01f0\u01f2\u01f4\u01f6\u01f8"+
		"\u01fa\u01fc\u01fe\u0200\u0202\u0204\u0206\u0208\u020a\u020c\u020e\u0210"+
		"\u0212\u0214\u0216\u0218\u021a\u021c\u021e\u0220\u0222\u0224\u0226\u0228"+
		"\u022a\u022c\u022e\u0230\u0232\u0234\u0236\u0238\u023a\u023c\u023e\u0240"+
		"\u0242\u0244\u0246\u0248\u024a\u024c\u024e\u0250\u0252\u0254\u0256\u0258"+
		"\u025a\u025c\u025e\u0260\u0262\u0264\u0266\u0268\u026a\u026c\u026e\u0270"+
		"\u0272\u0274\u0276\u0278\u027a\u027c\u027e\u0280\u0282\u0284\u0286\u0288"+
		"\u028a\u028c\u028e\u0290\u0292\u0294\u0296\u0298\u029a\u029c\u029e\u02a0"+
		"\u02a2\u00006\u0002\u0000\u0012\u0012QQ\u0001\u0000\u0018\u0019\u0001"+
		"\u0000KL\u0002\u0000\u00b8\u00b8\u0102\u0102\u0002\u0000NN\u00ae\u00ae"+
		"\u0002\u0000==\u009f\u009f\u0001\u0000\u00ed\u00ee\u0003\u0000$$;;kk\u0002"+
		"\u0000\u0011\u0011\u00fd\u00fd\u0001\u0000z{\u0001\u0000\u00c3\u00c4\u0002"+
		"\u0000\u0012\u0012\u0015\u0015\u0002\u0000\u009c\u009c\u0137\u0137\u0002"+
		"\u0000\u00a1\u00a1\u0136\u0136\u0002\u0000||\u0138\u0138\u0002\u0000/"+
		"/\u0090\u0090\u0002\u0000//\u008b\u008b\u0006\u0000ddvv||\u0094\u0094"+
		"\u009c\u009c\u00a3\u00a4\u0002\u000000\u011b\u011b\u0001\u0000\u00a8\u00ab"+
		"\u0003\u0000UU\u00a1\u00a1\u00c7\u00c7\u0003\u0000RR\u00a2\u00a2\u010f"+
		"\u010f\u0002\u0000\u00a1\u00a1\u00c7\u00c7\u0004\u0000\u0012\u0012\u0015"+
		"\u0015\u00b0\u00b0\u0101\u0101\u0003\u0000##\u0095\u0095\u0114\u0114\u0001"+
		"\u0000\u0004\u0007\u0002\u0000DD\u010e\u010e\u0001\u0000\u012e\u012f\u0002"+
		"\u0000\u0016\u0016\u0097\u0097\u0001\u0000tu\u0001\u0000\u00fb\u00fc\u0001"+
		"\u0000\u00eb\u00ec\u0001\u0000\u00f8\u00f9\u0002\u0000FF\u00db\u00db\u0001"+
		"\u0000\u0123\u0124\u0001\u0000\u00ce\u00cf\u0001\u0000\u00c1\u00c2\u0001"+
		"\u0000\r\u000e\u0001\u0000\u00d0\u00d1\u0001\u0000\u0084\u0085\u0001\u0000"+
		"78\u0001\u0000\u0115\u0116\u0002\u0000II\u00a0\u00a0\u0001\u0000\u00e0"+
		"\u00e1\u0001\u0000^_\u0002\u0000\u00ad\u00ad\u00af\u00af\u0001\u0000A"+
		"B\u0001\u0000xy\u0001\u0000\u00cc\u00cd\u0001\u0000\u00f3\u00f4\u0002"+
		"\u0000\u00f1\u00f2\u00f5\u00f5\u0001\u0000\u000f\u0010\u0001\u0000\b\t"+
		"\u0017\u0000\u000b\u001d\u001f.2OQQVceuw{}\u008f\u0095\u009a\u009d\u00a0"+
		"\u00a5\u00a7\u00ac\u00b1\u00b4\u00b5\u00b7\u00c6\u00c9\u00ca\u00cc\u00d5"+
		"\u00d7\u00d7\u00da\u00dd\u00df\u00ee\u00f0\u00f6\u00f8\u010e\u0110\u011a"+
		"\u011c\u0135\u0af4\u0000\u02a4\u0001\u0000\u0000\u0000\u0002\u02b2\u0001"+
		"\u0000\u0000\u0000\u0004\u02b6\u0001\u0000\u0000\u0000\u0006\u02bc\u0001"+
		"\u0000\u0000\u0000\b\u02c8\u0001\u0000\u0000\u0000\n\u02dd\u0001\u0000"+
		"\u0000\u0000\f\u02df\u0001\u0000\u0000\u0000\u000e\u02eb\u0001\u0000\u0000"+
		"\u0000\u0010\u02ed\u0001\u0000\u0000\u0000\u0012\u02ef\u0001\u0000\u0000"+
		"\u0000\u0014\u02f3\u0001\u0000\u0000\u0000\u0016\u02ff\u0001\u0000\u0000"+
		"\u0000\u0018\u0306\u0001\u0000\u0000\u0000\u001a\u030f\u0001\u0000\u0000"+
		"\u0000\u001c\u0314\u0001\u0000\u0000\u0000\u001e\u0316\u0001\u0000\u0000"+
		"\u0000 \u0318\u0001\u0000\u0000\u0000\"\u0322\u0001\u0000\u0000\u0000"+
		"$\u0325\u0001\u0000\u0000\u0000&\u0328\u0001\u0000\u0000\u0000(\u032b"+
		"\u0001\u0000\u0000\u0000*\u0330\u0001\u0000\u0000\u0000,\u0333\u0001\u0000"+
		"\u0000\u0000.\u0336\u0001\u0000\u0000\u00000\u0355\u0001\u0000\u0000\u0000"+
		"2\u0357\u0001\u0000\u0000\u00004\u0368\u0001\u0000\u0000\u00006\u036b"+
		"\u0001\u0000\u0000\u00008\u0377\u0001\u0000\u0000\u0000:\u0397\u0001\u0000"+
		"\u0000\u0000<\u0399\u0001\u0000\u0000\u0000>\u03b7\u0001\u0000\u0000\u0000"+
		"@\u03bf\u0001\u0000\u0000\u0000B\u03c3\u0001\u0000\u0000\u0000D\u03c9"+
		"\u0001\u0000\u0000\u0000F\u03ec\u0001\u0000\u0000\u0000H\u03ef\u0001\u0000"+
		"\u0000\u0000J\u03f1\u0001\u0000\u0000\u0000L\u03f6\u0001\u0000\u0000\u0000"+
		"N\u0404\u0001\u0000\u0000\u0000P\u0412\u0001\u0000\u0000\u0000R\u041e"+
		"\u0001\u0000\u0000\u0000T\u042c\u0001\u0000\u0000\u0000V\u043c\u0001\u0000"+
		"\u0000\u0000X\u0440\u0001\u0000\u0000\u0000Z\u0444\u0001\u0000\u0000\u0000"+
		"\\\u0455\u0001\u0000\u0000\u0000^\u0457\u0001\u0000\u0000\u0000`\u045f"+
		"\u0001\u0000\u0000\u0000b\u046a\u0001\u0000\u0000\u0000d\u0474\u0001\u0000"+
		"\u0000\u0000f\u048d\u0001\u0000\u0000\u0000h\u0491\u0001\u0000\u0000\u0000"+
		"j\u0493\u0001\u0000\u0000\u0000l\u04a5\u0001\u0000\u0000\u0000n\u04cb"+
		"\u0001\u0000\u0000\u0000p\u04cd\u0001\u0000\u0000\u0000r\u04cf\u0001\u0000"+
		"\u0000\u0000t\u04d1\u0001\u0000\u0000\u0000v\u04d9\u0001\u0000\u0000\u0000"+
		"x\u04e9\u0001\u0000\u0000\u0000z\u04f5\u0001\u0000\u0000\u0000|\u0501"+
		"\u0001\u0000\u0000\u0000~\u0505\u0001\u0000\u0000\u0000\u0080\u0511\u0001"+
		"\u0000\u0000\u0000\u0082\u0516\u0001\u0000\u0000\u0000\u0084\u051e\u0001"+
		"\u0000\u0000\u0000\u0086\u0521\u0001\u0000\u0000\u0000\u0088\u0524\u0001"+
		"\u0000\u0000\u0000\u008a\u0527\u0001\u0000\u0000\u0000\u008c\u052c\u0001"+
		"\u0000\u0000\u0000\u008e\u052f\u0001\u0000\u0000\u0000\u0090\u054b\u0001"+
		"\u0000\u0000\u0000\u0092\u055b\u0001\u0000\u0000\u0000\u0094\u055d\u0001"+
		"\u0000\u0000\u0000\u0096\u055f\u0001\u0000\u0000\u0000\u0098\u0561\u0001"+
		"\u0000\u0000\u0000\u009a\u0570\u0001\u0000\u0000\u0000\u009c\u0572\u0001"+
		"\u0000\u0000\u0000\u009e\u057d\u0001\u0000\u0000\u0000\u00a0\u0588\u0001"+
		"\u0000\u0000\u0000\u00a2\u0590\u0001\u0000\u0000\u0000\u00a4\u059b\u0001"+
		"\u0000\u0000\u0000\u00a6\u05a3\u0001\u0000\u0000\u0000\u00a8\u05af\u0001"+
		"\u0000\u0000\u0000\u00aa\u05b8\u0001\u0000\u0000\u0000\u00ac\u05ba\u0001"+
		"\u0000\u0000\u0000\u00ae\u05c3\u0001\u0000\u0000\u0000\u00b0\u05c6\u0001"+
		"\u0000\u0000\u0000\u00b2\u05ce\u0001\u0000\u0000\u0000\u00b4\u05d6\u0001"+
		"\u0000\u0000\u0000\u00b6\u05e1\u0001\u0000\u0000\u0000\u00b8\u05e6\u0001"+
		"\u0000\u0000\u0000\u00ba\u05ee\u0001\u0000\u0000\u0000\u00bc\u0612\u0001"+
		"\u0000\u0000\u0000\u00be\u0614\u0001\u0000\u0000\u0000\u00c0\u0616\u0001"+
		"\u0000\u0000\u0000\u00c2\u061e\u0001\u0000\u0000\u0000\u00c4\u0626\u0001"+
		"\u0000\u0000\u0000\u00c6\u0631\u0001\u0000\u0000\u0000\u00c8\u0633\u0001"+
		"\u0000\u0000\u0000\u00ca\u0649\u0001\u0000\u0000\u0000\u00cc\u064b\u0001"+
		"\u0000\u0000\u0000\u00ce\u064e\u0001\u0000\u0000\u0000\u00d0\u0652\u0001"+
		"\u0000\u0000\u0000\u00d2\u0658\u0001\u0000\u0000\u0000\u00d4\u0670\u0001"+
		"\u0000\u0000\u0000\u00d6\u067b\u0001\u0000\u0000\u0000\u00d8\u067d\u0001"+
		"\u0000\u0000\u0000\u00da\u0689\u0001\u0000\u0000\u0000\u00dc\u068e\u0001"+
		"\u0000\u0000\u0000\u00de\u069b\u0001\u0000\u0000\u0000\u00e0\u06c8\u0001"+
		"\u0000\u0000\u0000\u00e2\u06ca\u0001\u0000\u0000\u0000\u00e4\u06dc\u0001"+
		"\u0000\u0000\u0000\u00e6\u06eb\u0001\u0000\u0000\u0000\u00e8\u06f8\u0001"+
		"\u0000\u0000\u0000\u00ea\u0703\u0001\u0000\u0000\u0000\u00ec\u070c\u0001"+
		"\u0000\u0000\u0000\u00ee\u071a\u0001\u0000\u0000\u0000\u00f0\u071c\u0001"+
		"\u0000\u0000\u0000\u00f2\u071e\u0001\u0000\u0000\u0000\u00f4\u0722\u0001"+
		"\u0000\u0000\u0000\u00f6\u0738\u0001\u0000\u0000\u0000\u00f8\u073a\u0001"+
		"\u0000\u0000\u0000\u00fa\u073f\u0001\u0000\u0000\u0000\u00fc\u074d\u0001"+
		"\u0000\u0000\u0000\u00fe\u075b\u0001\u0000\u0000\u0000\u0100\u0761\u0001"+
		"\u0000\u0000\u0000\u0102\u0766\u0001\u0000\u0000\u0000\u0104\u076a\u0001"+
		"\u0000\u0000\u0000\u0106\u0777\u0001\u0000\u0000\u0000\u0108\u0779\u0001"+
		"\u0000\u0000\u0000\u010a\u077e\u0001\u0000\u0000\u0000\u010c\u0780\u0001"+
		"\u0000\u0000\u0000\u010e\u0791\u0001\u0000\u0000\u0000\u0110\u0793\u0001"+
		"\u0000\u0000\u0000\u0112\u079b\u0001\u0000\u0000\u0000\u0114\u079e\u0001"+
		"\u0000\u0000\u0000\u0116\u07a0\u0001\u0000\u0000\u0000\u0118\u07a8\u0001"+
		"\u0000\u0000\u0000\u011a\u07b0\u0001\u0000\u0000\u0000\u011c\u07fb\u0001"+
		"\u0000\u0000\u0000\u011e\u0800\u0001\u0000\u0000\u0000\u0120\u0802\u0001"+
		"\u0000\u0000\u0000\u0122\u0807\u0001\u0000\u0000\u0000\u0124\u0818\u0001"+
		"\u0000\u0000\u0000\u0126\u081a\u0001\u0000\u0000\u0000\u0128\u081c\u0001"+
		"\u0000\u0000\u0000\u012a\u081e\u0001\u0000\u0000\u0000\u012c\u0820\u0001"+
		"\u0000\u0000\u0000\u012e\u0822\u0001\u0000\u0000\u0000\u0130\u0825\u0001"+
		"\u0000\u0000\u0000\u0132\u0828\u0001\u0000\u0000\u0000\u0134\u082a\u0001"+
		"\u0000\u0000\u0000\u0136\u082d\u0001\u0000\u0000\u0000\u0138\u0832\u0001"+
		"\u0000\u0000\u0000\u013a\u0834\u0001\u0000\u0000\u0000\u013c\u083b\u0001"+
		"\u0000\u0000\u0000\u013e\u083d\u0001\u0000\u0000\u0000\u0140\u083f\u0001"+
		"\u0000\u0000\u0000\u0142\u0841\u0001\u0000\u0000\u0000\u0144\u0843\u0001"+
		"\u0000\u0000\u0000\u0146\u0845\u0001\u0000\u0000\u0000\u0148\u0847\u0001"+
		"\u0000\u0000\u0000\u014a\u0849\u0001\u0000\u0000\u0000\u014c\u084b\u0001"+
		"\u0000\u0000\u0000\u014e\u084d\u0001\u0000\u0000\u0000\u0150\u084f\u0001"+
		"\u0000\u0000\u0000\u0152\u0851\u0001\u0000\u0000\u0000\u0154\u0853\u0001"+
		"\u0000\u0000\u0000\u0156\u0855\u0001\u0000\u0000\u0000\u0158\u0857\u0001"+
		"\u0000\u0000\u0000\u015a\u0859\u0001\u0000\u0000\u0000\u015c\u085b\u0001"+
		"\u0000\u0000\u0000\u015e\u085d\u0001\u0000\u0000\u0000\u0160\u0861\u0001"+
		"\u0000\u0000\u0000\u0162\u0863\u0001\u0000\u0000\u0000\u0164\u0866\u0001"+
		"\u0000\u0000\u0000\u0166\u086a\u0001\u0000\u0000\u0000\u0168\u086c\u0001"+
		"\u0000\u0000\u0000\u016a\u0872\u0001\u0000\u0000\u0000\u016c\u0875\u0001"+
		"\u0000\u0000\u0000\u016e\u087c\u0001\u0000\u0000\u0000\u0170\u087e\u0001"+
		"\u0000\u0000\u0000\u0172\u0880\u0001\u0000\u0000\u0000\u0174\u0882\u0001"+
		"\u0000\u0000\u0000\u0176\u0887\u0001\u0000\u0000\u0000\u0178\u0889\u0001"+
		"\u0000\u0000\u0000\u017a\u088b\u0001\u0000\u0000\u0000\u017c\u0891\u0001"+
		"\u0000\u0000\u0000\u017e\u0894\u0001\u0000\u0000\u0000\u0180\u0896\u0001"+
		"\u0000\u0000\u0000\u0182\u089c\u0001\u0000\u0000\u0000\u0184\u089f\u0001"+
		"\u0000\u0000\u0000\u0186\u08a1\u0001\u0000\u0000\u0000\u0188\u08a4\u0001"+
		"\u0000\u0000\u0000\u018a\u08af\u0001\u0000\u0000\u0000\u018c\u08b1\u0001"+
		"\u0000\u0000\u0000\u018e\u08b3\u0001\u0000\u0000\u0000\u0190\u08b5\u0001"+
		"\u0000\u0000\u0000\u0192\u08b7\u0001\u0000\u0000\u0000\u0194\u08b9\u0001"+
		"\u0000\u0000\u0000\u0196\u08bb\u0001\u0000\u0000\u0000\u0198\u08bd\u0001"+
		"\u0000\u0000\u0000\u019a\u08bf\u0001\u0000\u0000\u0000\u019c\u08c3\u0001"+
		"\u0000\u0000\u0000\u019e\u08c6\u0001\u0000\u0000\u0000\u01a0\u08c9\u0001"+
		"\u0000\u0000\u0000\u01a2\u08cc\u0001\u0000\u0000\u0000\u01a4\u08ce\u0001"+
		"\u0000\u0000\u0000\u01a6\u08d4\u0001\u0000\u0000\u0000\u01a8\u08da\u0001"+
		"\u0000\u0000\u0000\u01aa\u08dc\u0001\u0000\u0000\u0000\u01ac\u08df\u0001"+
		"\u0000\u0000\u0000\u01ae\u08e2\u0001\u0000\u0000\u0000\u01b0\u08e7\u0001"+
		"\u0000\u0000\u0000\u01b2\u08e9\u0001\u0000\u0000\u0000\u01b4\u08ed\u0001"+
		"\u0000\u0000\u0000\u01b6\u08f1\u0001\u0000\u0000\u0000\u01b8\u08f4\u0001"+
		"\u0000\u0000\u0000\u01ba\u08f7\u0001\u0000\u0000\u0000\u01bc\u08fc\u0001"+
		"\u0000\u0000\u0000\u01be\u08ff\u0001\u0000\u0000\u0000\u01c0\u0902\u0001"+
		"\u0000\u0000\u0000\u01c2\u0904\u0001\u0000\u0000\u0000\u01c4\u0907\u0001"+
		"\u0000\u0000\u0000\u01c6\u090c\u0001\u0000\u0000\u0000\u01c8\u090e\u0001"+
		"\u0000\u0000\u0000\u01ca\u0915\u0001\u0000\u0000\u0000\u01cc\u0917\u0001"+
		"\u0000\u0000\u0000\u01ce\u091b\u0001\u0000\u0000\u0000\u01d0\u091d\u0001"+
		"\u0000\u0000\u0000\u01d2\u0920\u0001\u0000\u0000\u0000\u01d4\u0922\u0001"+
		"\u0000\u0000\u0000\u01d6\u0925\u0001\u0000\u0000\u0000\u01d8\u0928\u0001"+
		"\u0000\u0000\u0000\u01da\u092a\u0001\u0000\u0000\u0000\u01dc\u0931\u0001"+
		"\u0000\u0000\u0000\u01de\u0937\u0001\u0000\u0000\u0000\u01e0\u0939\u0001"+
		"\u0000\u0000\u0000\u01e2\u093b\u0001\u0000\u0000\u0000\u01e4\u093d\u0001"+
		"\u0000\u0000\u0000\u01e6\u0941\u0001\u0000\u0000\u0000\u01e8\u0943\u0001"+
		"\u0000\u0000\u0000\u01ea\u0945\u0001\u0000\u0000\u0000\u01ec\u094a\u0001"+
		"\u0000\u0000\u0000\u01ee\u094c\u0001\u0000\u0000\u0000\u01f0\u094f\u0001"+
		"\u0000\u0000\u0000\u01f2\u0952\u0001\u0000\u0000\u0000\u01f4\u0955\u0001"+
		"\u0000\u0000\u0000\u01f6\u0957\u0001\u0000\u0000\u0000\u01f8\u095a\u0001"+
		"\u0000\u0000\u0000\u01fa\u095c\u0001\u0000\u0000\u0000\u01fc\u095f\u0001"+
		"\u0000\u0000\u0000\u01fe\u0962\u0001\u0000\u0000\u0000\u0200\u0964\u0001"+
		"\u0000\u0000\u0000\u0202\u0967\u0001\u0000\u0000\u0000\u0204\u0969\u0001"+
		"\u0000\u0000\u0000\u0206\u096b\u0001\u0000\u0000\u0000\u0208\u096d\u0001"+
		"\u0000\u0000\u0000\u020a\u096f\u0001\u0000\u0000\u0000\u020c\u0971\u0001"+
		"\u0000\u0000\u0000\u020e\u0973\u0001\u0000\u0000\u0000\u0210\u0975\u0001"+
		"\u0000\u0000\u0000\u0212\u0977\u0001\u0000\u0000\u0000\u0214\u0979\u0001"+
		"\u0000\u0000\u0000\u0216\u097d\u0001\u0000\u0000\u0000\u0218\u097f\u0001"+
		"\u0000\u0000\u0000\u021a\u0981\u0001\u0000\u0000\u0000\u021c\u0983\u0001"+
		"\u0000\u0000\u0000\u021e\u0990\u0001\u0000\u0000\u0000\u0220\u0992\u0001"+
		"\u0000\u0000\u0000\u0222\u099d\u0001\u0000\u0000\u0000\u0224\u099f\u0001"+
		"\u0000\u0000\u0000\u0226\u09a1\u0001\u0000\u0000\u0000\u0228\u09a5\u0001"+
		"\u0000\u0000\u0000\u022a\u09a7\u0001\u0000\u0000\u0000\u022c\u09ae\u0001"+
		"\u0000\u0000\u0000\u022e\u09b7\u0001\u0000\u0000\u0000\u0230\u09bc\u0001"+
		"\u0000\u0000\u0000\u0232\u09be\u0001\u0000\u0000\u0000\u0234\u09c0\u0001"+
		"\u0000\u0000\u0000\u0236\u09c2\u0001\u0000\u0000\u0000\u0238\u09c4\u0001"+
		"\u0000\u0000\u0000\u023a\u09c6\u0001\u0000\u0000\u0000\u023c\u09c8\u0001"+
		"\u0000\u0000\u0000\u023e\u09cc\u0001\u0000\u0000\u0000\u0240\u09cf\u0001"+
		"\u0000\u0000\u0000\u0242\u09d2\u0001\u0000\u0000\u0000\u0244\u09d4\u0001"+
		"\u0000\u0000\u0000\u0246\u09d7\u0001\u0000\u0000\u0000\u0248\u09d9\u0001"+
		"\u0000\u0000\u0000\u024a\u09e1\u0001\u0000\u0000\u0000\u024c\u09e3\u0001"+
		"\u0000\u0000\u0000\u024e\u09ec\u0001\u0000\u0000\u0000\u0250\u09ee\u0001"+
		"\u0000\u0000\u0000\u0252\u09f5\u0001\u0000\u0000\u0000\u0254\u09f9\u0001"+
		"\u0000\u0000\u0000\u0256\u09fd\u0001\u0000\u0000\u0000\u0258\u0a09\u0001"+
		"\u0000\u0000\u0000\u025a\u0a0b\u0001\u0000\u0000\u0000\u025c\u0a0d\u0001"+
		"\u0000\u0000\u0000\u025e\u0a0f\u0001\u0000\u0000\u0000\u0260\u0a11\u0001"+
		"\u0000\u0000\u0000\u0262\u0a13\u0001\u0000\u0000\u0000\u0264\u0a19\u0001"+
		"\u0000\u0000\u0000\u0266\u0a1e\u0001\u0000\u0000\u0000\u0268\u0a24\u0001"+
		"\u0000\u0000\u0000\u026a\u0a27\u0001\u0000\u0000\u0000\u026c\u0a2a\u0001"+
		"\u0000\u0000\u0000\u026e\u0a2d\u0001\u0000\u0000\u0000\u0270\u0a30\u0001"+
		"\u0000\u0000\u0000\u0272\u0a33\u0001\u0000\u0000\u0000\u0274\u0a37\u0001"+
		"\u0000\u0000\u0000\u0276\u0a3b\u0001\u0000\u0000\u0000\u0278\u0a3d\u0001"+
		"\u0000\u0000\u0000\u027a\u0a45\u0001\u0000\u0000\u0000\u027c\u0a4f\u0001"+
		"\u0000\u0000\u0000\u027e\u0a51\u0001\u0000\u0000\u0000\u0280\u0a59\u0001"+
		"\u0000\u0000\u0000\u0282\u0a66\u0001\u0000\u0000\u0000\u0284\u0a6d\u0001"+
		"\u0000\u0000\u0000\u0286\u0a71\u0001\u0000\u0000\u0000\u0288\u0a75\u0001"+
		"\u0000\u0000\u0000\u028a\u0a79\u0001\u0000\u0000\u0000\u028c\u0a7d\u0001"+
		"\u0000\u0000\u0000\u028e\u0a7f\u0001\u0000\u0000\u0000\u0290\u0a93\u0001"+
		"\u0000\u0000\u0000\u0292\u0a95\u0001\u0000\u0000\u0000\u0294\u0a97\u0001"+
		"\u0000\u0000\u0000\u0296\u0a9b\u0001\u0000\u0000\u0000\u0298\u0a9d\u0001"+
		"\u0000\u0000\u0000\u029a\u0aa8\u0001\u0000\u0000\u0000\u029c\u0aac\u0001"+
		"\u0000\u0000\u0000\u029e\u0aae\u0001\u0000\u0000\u0000\u02a0\u0ab0\u0001"+
		"\u0000\u0000\u0000\u02a2\u0ab2\u0001\u0000\u0000\u0000\u02a4\u02a9\u0003"+
		"\u0002\u0001\u0000\u02a5\u02a6\u0005\u00f7\u0000\u0000\u02a6\u02a8\u0003"+
		"\u0002\u0001\u0000\u02a7\u02a5\u0001\u0000\u0000\u0000\u02a8\u02ab\u0001"+
		"\u0000\u0000\u0000\u02a9\u02a7\u0001\u0000\u0000\u0000\u02a9\u02aa\u0001"+
		"\u0000\u0000\u0000\u02aa\u02ad\u0001\u0000\u0000\u0000\u02ab\u02a9\u0001"+
		"\u0000\u0000\u0000\u02ac\u02ae\u0005\u00f7\u0000\u0000\u02ad\u02ac\u0001"+
		"\u0000\u0000\u0000\u02ad\u02ae\u0001\u0000\u0000\u0000\u02ae\u02af\u0001"+
		"\u0000\u0000\u0000\u02af\u02b0\u0005\u0000\u0000\u0001\u02b0\u0001\u0001"+
		"\u0000\u0000\u0000\u02b1\u02b3\u0003\u0004\u0002\u0000\u02b2\u02b1\u0001"+
		"\u0000\u0000\u0000\u02b2\u02b3\u0001\u0000\u0000\u0000\u02b3\u02b4\u0001"+
		"\u0000\u0000\u0000\u02b4\u02b5\u0003\u0006\u0003\u0000\u02b5\u0003\u0001"+
		"\u0000\u0000\u0000\u02b6\u02b7\u0005\u0125\u0000\u0000\u02b7\u02b8\u0005"+
		"\u00c5\u0000\u0000\u02b8\u02ba\u00054\u0000\u0000\u02b9\u02bb\u0005\u0005"+
		"\u0000\u0000\u02ba\u02b9\u0001\u0000\u0000\u0000\u02ba\u02bb\u0001\u0000"+
		"\u0000\u0000\u02bb\u0005\u0001\u0000\u0000\u0000\u02bc\u02c4\u0003\b\u0004"+
		"\u0000\u02bd\u02bf\u0005\u011d\u0000\u0000\u02be\u02c0\u0007\u0000\u0000"+
		"\u0000\u02bf\u02be\u0001\u0000\u0000\u0000\u02bf\u02c0\u0001\u0000\u0000"+
		"\u0000\u02c0\u02c1\u0001\u0000\u0000\u0000\u02c1\u02c3\u0003\b\u0004\u0000"+
		"\u02c2\u02bd\u0001\u0000\u0000\u0000\u02c3\u02c6\u0001\u0000\u0000\u0000"+
		"\u02c4\u02c2\u0001\u0000\u0000\u0000\u02c4\u02c5\u0001\u0000\u0000\u0000"+
		"\u02c5\u0007\u0001\u0000\u0000\u0000\u02c6\u02c4\u0001\u0000\u0000\u0000"+
		"\u02c7\u02c9\u0003\n\u0005\u0000\u02c8\u02c7\u0001\u0000\u0000\u0000\u02c9"+
		"\u02ca\u0001\u0000\u0000\u0000\u02ca\u02c8\u0001\u0000\u0000\u0000\u02ca"+
		"\u02cb\u0001\u0000\u0000\u0000\u02cb\t\u0001\u0000\u0000\u0000\u02cc\u02de"+
		"\u0003\f\u0006\u0000\u02cd\u02de\u0003\u0010\b\u0000\u02ce\u02de\u0003"+
		"\u0012\t\u0000\u02cf\u02de\u0003*\u0015\u0000\u02d0\u02de\u0003,\u0016"+
		"\u0000\u02d1\u02de\u00036\u001b\u0000\u02d2\u02de\u0003.\u0017\u0000\u02d3"+
		"\u02de\u00032\u0019\u0000\u02d4\u02de\u00038\u001c\u0000\u02d5\u02de\u0003"+
		">\u001f\u0000\u02d6\u02de\u0003(\u0014\u0000\u02d7\u02de\u0003B!\u0000"+
		"\u02d8\u02de\u0003D\"\u0000\u02d9\u02de\u0003P(\u0000\u02da\u02de\u0003"+
		"L&\u0000\u02db\u02de\u0003N\'\u0000\u02dc\u02de\u0003\\.\u0000\u02dd\u02cc"+
		"\u0001\u0000\u0000\u0000\u02dd\u02cd\u0001\u0000\u0000\u0000\u02dd\u02ce"+
		"\u0001\u0000\u0000\u0000\u02dd\u02cf\u0001\u0000\u0000\u0000\u02dd\u02d0"+
		"\u0001\u0000\u0000\u0000\u02dd\u02d1\u0001\u0000\u0000\u0000\u02dd\u02d2"+
		"\u0001\u0000\u0000\u0000\u02dd\u02d3\u0001\u0000\u0000\u0000\u02dd\u02d4"+
		"\u0001\u0000\u0000\u0000\u02dd\u02d5\u0001\u0000\u0000\u0000\u02dd\u02d6"+
		"\u0001\u0000\u0000\u0000\u02dd\u02d7\u0001\u0000\u0000\u0000\u02dd\u02d8"+
		"\u0001\u0000\u0000\u0000\u02dd\u02d9\u0001\u0000\u0000\u0000\u02dd\u02da"+
		"\u0001\u0000\u0000\u0000\u02dd\u02db\u0001\u0000\u0000\u0000\u02dd\u02dc"+
		"\u0001\u0000\u0000\u0000\u02de\u000b\u0001\u0000\u0000\u0000\u02df\u02e1"+
		"\u0005\u0122\u0000\u0000\u02e0\u02e2\u0005x\u0000\u0000\u02e1\u02e0\u0001"+
		"\u0000\u0000\u0000\u02e1\u02e2\u0001\u0000\u0000\u0000\u02e2\u02e3\u0001"+
		"\u0000\u0000\u0000\u02e3\u02e4\u0003\u000e\u0007\u0000\u02e4\r\u0001\u0000"+
		"\u0000\u0000\u02e5\u02e6\u0005\u009b\u0000\u0000\u02e6\u02e7\u0003\u000e"+
		"\u0007\u0000\u02e7\u02e8\u0005\u00ef\u0000\u0000\u02e8\u02ec\u0001\u0000"+
		"\u0000\u0000\u02e9\u02ec\u0003\u010c\u0086\u0000\u02ea\u02ec\u0003\u027e"+
		"\u013f\u0000\u02eb\u02e5\u0001\u0000\u0000\u0000\u02eb\u02e9\u0001\u0000"+
		"\u0000\u0000\u02eb\u02ea\u0001\u0000\u0000\u0000\u02ec\u000f\u0001\u0000"+
		"\u0000\u0000\u02ed\u02ee\u0005n\u0000\u0000\u02ee\u0011\u0001\u0000\u0000"+
		"\u0000\u02ef\u02f0\u0005\u00e9\u0000\u0000\u02f0\u02f1\u0003\u0014\n\u0000"+
		"\u02f1\u0013\u0001\u0000\u0000\u0000\u02f2\u02f4\u0005Q\u0000\u0000\u02f3"+
		"\u02f2\u0001\u0000\u0000\u0000\u02f3\u02f4\u0001\u0000\u0000\u0000\u02f4"+
		"\u02f5\u0001\u0000\u0000\u0000\u02f5\u02f7\u0003\u0018\f\u0000\u02f6\u02f8"+
		"\u0003 \u0010\u0000\u02f7\u02f6\u0001\u0000\u0000\u0000\u02f7\u02f8\u0001"+
		"\u0000\u0000\u0000\u02f8\u02fa\u0001\u0000\u0000\u0000\u02f9\u02fb\u0003"+
		"\"\u0011\u0000\u02fa\u02f9\u0001\u0000\u0000\u0000\u02fa\u02fb\u0001\u0000"+
		"\u0000\u0000\u02fb\u02fd\u0001\u0000\u0000\u0000\u02fc\u02fe\u0003$\u0012"+
		"\u0000\u02fd\u02fc\u0001\u0000\u0000\u0000\u02fd\u02fe\u0001\u0000\u0000"+
		"\u0000\u02fe\u0015\u0001\u0000\u0000\u0000\u02ff\u0302\u0003\u00b0X\u0000"+
		"\u0300\u0301\u0005\u0017\u0000\u0000\u0301\u0303\u0003\u0114\u008a\u0000"+
		"\u0302\u0300\u0001\u0000\u0000\u0000\u0302\u0303\u0001\u0000\u0000\u0000"+
		"\u0303\u0017\u0001\u0000\u0000\u0000\u0304\u0307\u0005\u010f\u0000\u0000"+
		"\u0305\u0307\u0003\u0016\u000b\u0000\u0306\u0304\u0001\u0000\u0000\u0000"+
		"\u0306\u0305\u0001\u0000\u0000\u0000\u0307\u030c\u0001\u0000\u0000\u0000"+
		"\u0308\u0309\u00051\u0000\u0000\u0309\u030b\u0003\u0016\u000b\u0000\u030a"+
		"\u0308\u0001\u0000\u0000\u0000\u030b\u030e\u0001\u0000\u0000\u0000\u030c"+
		"\u030a\u0001\u0000\u0000\u0000\u030c\u030d\u0001\u0000\u0000\u0000\u030d"+
		"\u0019\u0001\u0000\u0000\u0000\u030e\u030c\u0001\u0000\u0000\u0000\u030f"+
		"\u0312\u0003\u00b0X\u0000\u0310\u0313\u0003\u001c\u000e\u0000\u0311\u0313"+
		"\u0003\u001e\u000f\u0000\u0312\u0310\u0001\u0000\u0000\u0000\u0312\u0311"+
		"\u0001\u0000\u0000\u0000\u0312\u0313\u0001\u0000\u0000\u0000\u0313\u001b"+
		"\u0001\u0000\u0000\u0000\u0314\u0315\u0007\u0001\u0000\u0000\u0315\u001d"+
		"\u0001\u0000\u0000\u0000\u0316\u0317\u0007\u0002\u0000\u0000\u0317\u001f"+
		"\u0001\u0000\u0000\u0000\u0318\u0319\u0005\u00bf\u0000\u0000\u0319\u031a"+
		"\u0005(\u0000\u0000\u031a\u031f\u0003\u001a\r\u0000\u031b\u031c\u0005"+
		"1\u0000\u0000\u031c\u031e\u0003\u001a\r\u0000\u031d\u031b\u0001\u0000"+
		"\u0000\u0000\u031e\u0321\u0001\u0000\u0000\u0000\u031f\u031d\u0001\u0000"+
		"\u0000\u0000\u031f\u0320\u0001\u0000\u0000\u0000\u0320!\u0001\u0000\u0000"+
		"\u0000\u0321\u031f\u0001\u0000\u0000\u0000\u0322\u0323\u0007\u0003\u0000"+
		"\u0000\u0323\u0324\u0003\u00b0X\u0000\u0324#\u0001\u0000\u0000\u0000\u0325"+
		"\u0326\u0005\u0096\u0000\u0000\u0326\u0327\u0003\u00b0X\u0000\u0327%\u0001"+
		"\u0000\u0000\u0000\u0328\u0329\u0005\u012d\u0000\u0000\u0329\u032a\u0003"+
		"\u00b0X\u0000\u032a\'\u0001\u0000\u0000\u0000\u032b\u032c\u0005\u012e"+
		"\u0000\u0000\u032c\u032e\u0003\u0014\n\u0000\u032d\u032f\u0003&\u0013"+
		"\u0000\u032e\u032d\u0001\u0000\u0000\u0000\u032e\u032f\u0001\u0000\u0000"+
		"\u0000\u032f)\u0001\u0000\u0000\u0000\u0330\u0331\u0005=\u0000\u0000\u0331"+
		"\u0332\u0003^/\u0000\u0332+\u0001\u0000\u0000\u0000\u0333\u0334\u0005"+
		"\u0088\u0000\u0000\u0334\u0335\u0003`0\u0000\u0335-\u0001\u0000\u0000"+
		"\u0000\u0336\u0337\u0005\u00fa\u0000\u0000\u0337\u033c\u00030\u0018\u0000"+
		"\u0338\u0339\u00051\u0000\u0000\u0339\u033b\u00030\u0018\u0000\u033a\u0338"+
		"\u0001\u0000\u0000\u0000\u033b\u033e\u0001\u0000\u0000\u0000\u033c\u033a"+
		"\u0001\u0000\u0000\u0000\u033c\u033d\u0001\u0000\u0000\u0000\u033d/\u0001"+
		"\u0000\u0000\u0000\u033e\u033c\u0001\u0000\u0000\u0000\u033f\u0340\u0003"+
		"\u00d0h\u0000\u0340\u0341\u0005d\u0000\u0000\u0341\u0342\u0003\u00b0X"+
		"\u0000\u0342\u0356\u0001\u0000\u0000\u0000\u0343\u0344\u0003\u00d2i\u0000"+
		"\u0344\u0345\u0005d\u0000\u0000\u0345\u0346\u0003\u00b0X\u0000\u0346\u0356"+
		"\u0001\u0000\u0000\u0000\u0347\u0348\u0003\u0114\u008a\u0000\u0348\u0349"+
		"\u0005d\u0000\u0000\u0349\u034a\u0003\u00b0X\u0000\u034a\u0356\u0001\u0000"+
		"\u0000\u0000\u034b\u034c\u0003\u0114\u008a\u0000\u034c\u034d\u0005\u00c8"+
		"\u0000\u0000\u034d\u034e\u0003\u00b0X\u0000\u034e\u0356\u0001\u0000\u0000"+
		"\u0000\u034f\u0350\u0003\u0114\u008a\u0000\u0350\u0351\u0003|>\u0000\u0351"+
		"\u0356\u0001\u0000\u0000\u0000\u0352\u0353\u0003\u0114\u008a\u0000\u0353"+
		"\u0354\u0003~?\u0000\u0354\u0356\u0001\u0000\u0000\u0000\u0355\u033f\u0001"+
		"\u0000\u0000\u0000\u0355\u0343\u0001\u0000\u0000\u0000\u0355\u0347\u0001"+
		"\u0000\u0000\u0000\u0355\u034b\u0001\u0000\u0000\u0000\u0355\u034f\u0001"+
		"\u0000\u0000\u0000\u0355\u0352\u0001\u0000\u0000\u0000\u03561\u0001\u0000"+
		"\u0000\u0000\u0357\u0358\u0005\u00e2\u0000\u0000\u0358\u035d\u00034\u001a"+
		"\u0000\u0359\u035a\u00051\u0000\u0000\u035a\u035c\u00034\u001a\u0000\u035b"+
		"\u0359\u0001\u0000\u0000\u0000\u035c\u035f\u0001\u0000\u0000\u0000\u035d"+
		"\u035b\u0001\u0000\u0000\u0000\u035d\u035e\u0001\u0000\u0000\u0000\u035e"+
		"3\u0001\u0000\u0000\u0000\u035f\u035d\u0001\u0000\u0000\u0000\u0360\u0369"+
		"\u0003\u00d0h\u0000\u0361\u0369\u0003\u00d2i\u0000\u0362\u0363\u0003\u0114"+
		"\u008a\u0000\u0363\u0364\u0003|>\u0000\u0364\u0369\u0001\u0000\u0000\u0000"+
		"\u0365\u0366\u0003\u0114\u008a\u0000\u0366\u0367\u0003~?\u0000\u0367\u0369"+
		"\u0001\u0000\u0000\u0000\u0368\u0360\u0001\u0000\u0000\u0000\u0368\u0361"+
		"\u0001\u0000\u0000\u0000\u0368\u0362\u0001\u0000\u0000\u0000\u0368\u0365"+
		"\u0001\u0000\u0000\u0000\u03695\u0001\u0000\u0000\u0000\u036a\u036c\u0007"+
		"\u0004\u0000\u0000\u036b\u036a\u0001\u0000\u0000\u0000\u036b\u036c\u0001"+
		"\u0000\u0000\u0000\u036c\u036d\u0001\u0000\u0000\u0000\u036d\u036e\u0005"+
		"I\u0000\u0000\u036e\u0373\u0003\u00b0X\u0000\u036f\u0370\u00051\u0000"+
		"\u0000\u0370\u0372\u0003\u00b0X\u0000\u0371\u036f\u0001\u0000\u0000\u0000"+
		"\u0372\u0375\u0001\u0000\u0000\u0000\u0373\u0371\u0001\u0000\u0000\u0000"+
		"\u0373\u0374\u0001\u0000\u0000\u0000\u03747\u0001\u0000\u0000\u0000\u0375"+
		"\u0373\u0001\u0000\u0000\u0000\u0376\u0378\u0005\u00bb\u0000\u0000\u0377"+
		"\u0376\u0001\u0000\u0000\u0000\u0377\u0378\u0001\u0000\u0000\u0000\u0378"+
		"\u0379\u0001\u0000\u0000\u0000\u0379\u037b\u0005\u009f\u0000\u0000\u037a"+
		"\u037c\u0003:\u001d\u0000\u037b\u037a\u0001\u0000\u0000\u0000\u037b\u037c"+
		"\u0001\u0000\u0000\u0000\u037c\u037d\u0001\u0000\u0000\u0000\u037d\u0381"+
		"\u0003^/\u0000\u037e\u0380\u0003<\u001e\u0000\u037f\u037e\u0001\u0000"+
		"\u0000\u0000\u0380\u0383\u0001\u0000\u0000\u0000\u0381\u037f\u0001\u0000"+
		"\u0000\u0000\u0381\u0382\u0001\u0000\u0000\u0000\u0382\u0385\u0001\u0000"+
		"\u0000\u0000\u0383\u0381\u0001\u0000\u0000\u0000\u0384\u0386\u0003&\u0013"+
		"\u0000\u0385\u0384\u0001\u0000\u0000\u0000\u0385\u0386\u0001\u0000\u0000"+
		"\u0000\u03869\u0001\u0000\u0000\u0000\u0387\u038d\u0005\u00e3\u0000\u0000"+
		"\u0388\u038a\u0005^\u0000\u0000\u0389\u038b\u0005\u001f\u0000\u0000\u038a"+
		"\u0389\u0001\u0000\u0000\u0000\u038a\u038b\u0001\u0000\u0000\u0000\u038b"+
		"\u038e\u0001\u0000\u0000\u0000\u038c\u038e\u0005_\u0000\u0000\u038d\u0388"+
		"\u0001\u0000\u0000\u0000\u038d\u038c\u0001\u0000\u0000\u0000\u038e\u0398"+
		"\u0001\u0000\u0000\u0000\u038f\u0395\u0005O\u0000\u0000\u0390\u0392\u0005"+
		"\u00e0\u0000\u0000\u0391\u0393\u0005\u001f\u0000\u0000\u0392\u0391\u0001"+
		"\u0000\u0000\u0000\u0392\u0393\u0001\u0000\u0000\u0000\u0393\u0396\u0001"+
		"\u0000\u0000\u0000\u0394\u0396\u0005\u00e1\u0000\u0000\u0395\u0390\u0001"+
		"\u0000\u0000\u0000\u0395\u0394\u0001\u0000\u0000\u0000\u0396\u0398\u0001"+
		"\u0000\u0000\u0000\u0397\u0387\u0001\u0000\u0000\u0000\u0397\u038f\u0001"+
		"\u0000\u0000\u0000\u0398;\u0001\u0000\u0000\u0000\u0399\u03b5\u0005\u0125"+
		"\u0000\u0000\u039a\u03a4\u0005\u0084\u0000\u0000\u039b\u039c\u0005&\u0000"+
		"\u0000\u039c\u03a4\u0005\u0084\u0000\u0000\u039d\u039e\u0005\u010c\u0000"+
		"\u0000\u039e\u03a4\u0005\u0084\u0000\u0000\u039f\u03a0\u0005\u00d7\u0000"+
		"\u0000\u03a0\u03a4\u0005\u0084\u0000\u0000\u03a1\u03a2\u0005\u00c9\u0000"+
		"\u0000\u03a2\u03a4\u0005\u0084\u0000\u0000\u03a3\u039a\u0001\u0000\u0000"+
		"\u0000\u03a3\u039b\u0001\u0000\u0000\u0000\u03a3\u039d\u0001\u0000\u0000"+
		"\u0000\u03a3\u039f\u0001\u0000\u0000\u0000\u03a3\u03a1\u0001\u0000\u0000"+
		"\u0000\u03a4\u03a6\u0001\u0000\u0000\u0000\u03a5\u03a7\u0005\u00f6\u0000"+
		"\u0000\u03a6\u03a5\u0001\u0000\u0000\u0000\u03a6\u03a7\u0001\u0000\u0000"+
		"\u0000\u03a7\u03a8\u0001\u0000\u0000\u0000\u03a8\u03a9\u0003\u0114\u008a"+
		"\u0000\u03a9\u03aa\u0003\u008aE\u0000\u03aa\u03ab\u0005\u009b\u0000\u0000"+
		"\u03ab\u03ac\u0003\u0116\u008b\u0000\u03ac\u03ad\u0005\u00ef\u0000\u0000"+
		"\u03ad\u03b6\u0001\u0000\u0000\u0000\u03ae\u03af\u0005\u008c\u0000\u0000"+
		"\u03af\u03b0\u0005\u00b9\u0000\u0000\u03b0\u03b6\u0003\u0116\u008b\u0000"+
		"\u03b1\u03b2\u0005\u00f0\u0000\u0000\u03b2\u03b3\u0003\u0114\u008a\u0000"+
		"\u03b3\u03b4\u0003\u008aE\u0000\u03b4\u03b6\u0001\u0000\u0000\u0000\u03b5"+
		"\u03a3\u0001\u0000\u0000\u0000\u03b5\u03ae\u0001\u0000\u0000\u0000\u03b5"+
		"\u03b1\u0001\u0000\u0000\u0000\u03b6=\u0001\u0000\u0000\u0000\u03b7\u03b8"+
		"\u0005\u00a0\u0000\u0000\u03b8\u03bc\u0003b1\u0000\u03b9\u03bb\u0003@"+
		" \u0000\u03ba\u03b9\u0001\u0000\u0000\u0000\u03bb\u03be\u0001\u0000\u0000"+
		"\u0000\u03bc\u03ba\u0001\u0000\u0000\u0000\u03bc\u03bd\u0001\u0000\u0000"+
		"\u0000\u03bd?\u0001\u0000\u0000\u0000\u03be\u03bc\u0001\u0000\u0000\u0000"+
		"\u03bf\u03c0\u0005\u00b9\u0000\u0000\u03c0\u03c1\u0007\u0005\u0000\u0000"+
		"\u03c1\u03c2\u0003.\u0017\u0000\u03c2A\u0001\u0000\u0000\u0000\u03c3\u03c4"+
		"\u0005\u0120\u0000\u0000\u03c4\u03c5\u0003\u00b0X\u0000\u03c5\u03c6\u0005"+
		"\u0017\u0000\u0000\u03c6\u03c7\u0003\u0114\u008a\u0000\u03c7C\u0001\u0000"+
		"\u0000\u0000\u03c8\u03ca\u0005\u00bb\u0000\u0000\u03c9\u03c8\u0001\u0000"+
		"\u0000\u0000\u03c9\u03ca\u0001\u0000\u0000\u0000\u03ca\u03cb\u0001\u0000"+
		"\u0000\u0000\u03cb\u03cc\u0005)\u0000\u0000\u03cc\u03d9\u0003F#\u0000"+
		"\u03cd\u03d6\u0005\u009b\u0000\u0000\u03ce\u03d3\u0003H$\u0000\u03cf\u03d0"+
		"\u00051\u0000\u0000\u03d0\u03d2\u0003H$\u0000\u03d1\u03cf\u0001\u0000"+
		"\u0000\u0000\u03d2\u03d5\u0001\u0000\u0000\u0000\u03d3\u03d1\u0001\u0000"+
		"\u0000\u0000\u03d3\u03d4\u0001\u0000\u0000\u0000\u03d4\u03d7\u0001\u0000"+
		"\u0000\u0000\u03d5\u03d3\u0001\u0000\u0000\u0000\u03d6\u03ce\u0001\u0000"+
		"\u0000\u0000\u03d6\u03d7\u0001\u0000\u0000\u0000\u03d7\u03d8\u0001\u0000"+
		"\u0000\u0000\u03d8\u03da\u0005\u00ef\u0000\u0000\u03d9\u03cd\u0001\u0000"+
		"\u0000\u0000\u03d9\u03da\u0001\u0000\u0000\u0000\u03da\u03ea\u0001\u0000"+
		"\u0000\u0000\u03db\u03e8\u0005\u0132\u0000\u0000\u03dc\u03e9\u0005\u010f"+
		"\u0000\u0000\u03dd\u03e2\u0003J%\u0000\u03de\u03df\u00051\u0000\u0000"+
		"\u03df\u03e1\u0003J%\u0000\u03e0\u03de\u0001\u0000\u0000\u0000\u03e1\u03e4"+
		"\u0001\u0000\u0000\u0000\u03e2\u03e0\u0001\u0000\u0000\u0000\u03e2\u03e3"+
		"\u0001\u0000\u0000\u0000\u03e3\u03e6\u0001\u0000\u0000\u0000\u03e4\u03e2"+
		"\u0001\u0000\u0000\u0000\u03e5\u03e7\u0003&\u0013\u0000\u03e6\u03e5\u0001"+
		"\u0000\u0000\u0000\u03e6\u03e7\u0001\u0000\u0000\u0000\u03e7\u03e9\u0001"+
		"\u0000\u0000\u0000\u03e8\u03dc\u0001\u0000\u0000\u0000\u03e8\u03dd\u0001"+
		"\u0000\u0000\u0000\u03e9\u03eb\u0001\u0000\u0000\u0000\u03ea\u03db\u0001"+
		"\u0000\u0000\u0000\u03ea\u03eb\u0001\u0000\u0000\u0000\u03ebE\u0001\u0000"+
		"\u0000\u0000\u03ec\u03ed\u0003\u0112\u0089\u0000\u03ed\u03ee\u0003\u0296"+
		"\u014b\u0000\u03eeG\u0001\u0000\u0000\u0000\u03ef\u03f0\u0003\u00b0X\u0000"+
		"\u03f0I\u0001\u0000\u0000\u0000\u03f1\u03f4\u0003\u0296\u014b\u0000\u03f2"+
		"\u03f3\u0005\u0017\u0000\u0000\u03f3\u03f5\u0003\u0114\u008a\u0000\u03f4"+
		"\u03f2\u0001\u0000\u0000\u0000\u03f4\u03f5\u0001\u0000\u0000\u0000\u03f5"+
		"K\u0001\u0000\u0000\u0000\u03f6\u03f7\u0005\u0098\u0000\u0000\u03f7\u03fa"+
		"\u0005>\u0000\u0000\u03f8\u03f9\u0005\u012e\u0000\u0000\u03f9\u03fb\u0005"+
		"}\u0000\u0000\u03fa\u03f8\u0001\u0000\u0000\u0000\u03fa\u03fb\u0001\u0000"+
		"\u0000\u0000\u03fb\u03fc\u0001\u0000\u0000\u0000\u03fc\u03fd\u0005r\u0000"+
		"\u0000\u03fd\u03fe\u0003\u00b0X\u0000\u03fe\u03ff\u0005\u0017\u0000\u0000"+
		"\u03ff\u0402\u0003\u0114\u008a\u0000\u0400\u0401\u0005m\u0000\u0000\u0401"+
		"\u0403\u0003\u0284\u0142\u0000\u0402\u0400\u0001\u0000\u0000\u0000\u0402"+
		"\u0403\u0001\u0000\u0000\u0000\u0403M\u0001\u0000\u0000\u0000\u0404\u0405"+
		"\u0005q\u0000\u0000\u0405\u0406\u0005\u009b\u0000\u0000\u0406\u0407\u0003"+
		"\u0114\u008a\u0000\u0407\u0408\u0005\u0083\u0000\u0000\u0408\u0409\u0003"+
		"\u00b0X\u0000\u0409\u040b\u0005\u001e\u0000\u0000\u040a\u040c\u0003\n"+
		"\u0005\u0000\u040b\u040a\u0001\u0000\u0000\u0000\u040c\u040d\u0001\u0000"+
		"\u0000\u0000\u040d\u040b\u0001\u0000\u0000\u0000\u040d\u040e\u0001\u0000"+
		"\u0000\u0000\u040e\u040f\u0001\u0000\u0000\u0000\u040f\u0410\u0005\u00ef"+
		"\u0000\u0000\u0410O\u0001\u0000\u0000\u0000\u0411\u0413\u0005\u00bb\u0000"+
		"\u0000\u0412\u0411\u0001\u0000\u0000\u0000\u0412\u0413\u0001\u0000\u0000"+
		"\u0000\u0413\u0414\u0001\u0000\u0000\u0000\u0414\u0416\u0005)\u0000\u0000"+
		"\u0415\u0417\u0003R)\u0000\u0416\u0415\u0001\u0000\u0000\u0000\u0416\u0417"+
		"\u0001\u0000\u0000\u0000\u0417\u0418\u0001\u0000\u0000\u0000\u0418\u0419"+
		"\u0005\u0093\u0000\u0000\u0419\u041a\u0003\u0006\u0003\u0000\u041a\u041c"+
		"\u0005\u00d9\u0000\u0000\u041b\u041d\u0003T*\u0000\u041c\u041b\u0001\u0000"+
		"\u0000\u0000\u041c\u041d\u0001\u0000\u0000\u0000\u041dQ\u0001\u0000\u0000"+
		"\u0000\u041e\u0428\u0005\u009b\u0000\u0000\u041f\u0429\u0005\u010f\u0000"+
		"\u0000\u0420\u0425\u0003\u0114\u008a\u0000\u0421\u0422\u00051\u0000\u0000"+
		"\u0422\u0424\u0003\u0114\u008a\u0000\u0423\u0421\u0001\u0000\u0000\u0000"+
		"\u0424\u0427\u0001\u0000\u0000\u0000\u0425\u0423\u0001\u0000\u0000\u0000"+
		"\u0425\u0426\u0001\u0000\u0000\u0000\u0426\u0429\u0001\u0000\u0000\u0000"+
		"\u0427\u0425\u0001\u0000\u0000\u0000\u0428\u041f\u0001\u0000\u0000\u0000"+
		"\u0428\u0420\u0001\u0000\u0000\u0000\u0428\u0429\u0001\u0000\u0000\u0000"+
		"\u0429\u042a\u0001\u0000\u0000\u0000\u042a\u042b\u0005\u00ef\u0000\u0000"+
		"\u042bS\u0001\u0000\u0000\u0000\u042c\u0431\u0005\u0083\u0000\u0000\u042d"+
		"\u042f\u0003\u00b0X\u0000\u042e\u042d\u0001\u0000\u0000\u0000\u042e\u042f"+
		"\u0001\u0000\u0000\u0000\u042f\u0430\u0001\u0000\u0000\u0000\u0430\u0432"+
		"\u00056\u0000\u0000\u0431\u042e\u0001\u0000\u0000\u0000\u0431\u0432\u0001"+
		"\u0000\u0000\u0000\u0432\u0433\u0001\u0000\u0000\u0000\u0433\u0439\u0005"+
		"\u0116\u0000\u0000\u0434\u0438\u0003V+\u0000\u0435\u0438\u0003X,\u0000"+
		"\u0436\u0438\u0003Z-\u0000\u0437\u0434\u0001\u0000\u0000\u0000\u0437\u0435"+
		"\u0001\u0000\u0000\u0000\u0437\u0436\u0001\u0000\u0000\u0000\u0438\u043b"+
		"\u0001\u0000\u0000\u0000\u0439\u0437\u0001\u0000\u0000\u0000\u0439\u043a"+
		"\u0001\u0000\u0000\u0000\u043aU\u0001\u0000\u0000\u0000\u043b\u0439\u0001"+
		"\u0000\u0000\u0000\u043c\u043d\u0005\u00b7\u0000\u0000\u043d\u043e\u0003"+
		"\u00b0X\u0000\u043e\u043f\u0007\u0006\u0000\u0000\u043fW\u0001\u0000\u0000"+
		"\u0000\u0440\u0441\u0005\u00b9\u0000\u0000\u0441\u0442\u0005j\u0000\u0000"+
		"\u0442\u0443\u0007\u0007\u0000\u0000\u0443Y\u0001\u0000\u0000\u0000\u0444"+
		"\u0445\u0005\u00e5\u0000\u0000\u0445\u0446\u0005\u0105\u0000\u0000\u0446"+
		"\u0447\u0005\u0017\u0000\u0000\u0447\u0448\u0003\u0114\u008a\u0000\u0448"+
		"[\u0001\u0000\u0000\u0000\u0449\u044b\u0003 \u0010\u0000\u044a\u044c\u0003"+
		"\"\u0011\u0000\u044b\u044a\u0001\u0000\u0000\u0000\u044b\u044c\u0001\u0000"+
		"\u0000\u0000\u044c\u044e\u0001\u0000\u0000\u0000\u044d\u044f\u0003$\u0012"+
		"\u0000\u044e\u044d\u0001\u0000\u0000\u0000\u044e\u044f\u0001\u0000\u0000"+
		"\u0000\u044f\u0456\u0001\u0000\u0000\u0000\u0450\u0452\u0003\"\u0011\u0000"+
		"\u0451\u0453\u0003$\u0012\u0000\u0452\u0451\u0001\u0000\u0000\u0000\u0452"+
		"\u0453\u0001\u0000\u0000\u0000\u0453\u0456\u0001\u0000\u0000\u0000\u0454"+
		"\u0456\u0003$\u0012\u0000\u0455\u0449\u0001\u0000\u0000\u0000\u0455\u0450"+
		"\u0001\u0000\u0000\u0000\u0455\u0454\u0001\u0000\u0000\u0000\u0456]\u0001"+
		"\u0000\u0000\u0000\u0457\u045c\u0003b1\u0000\u0458\u0459\u00051\u0000"+
		"\u0000\u0459\u045b\u0003b1\u0000\u045a\u0458\u0001\u0000\u0000\u0000\u045b"+
		"\u045e\u0001\u0000\u0000\u0000\u045c\u045a\u0001\u0000\u0000\u0000\u045c"+
		"\u045d\u0001\u0000\u0000\u0000\u045d_\u0001\u0000\u0000\u0000\u045e\u045c"+
		"\u0001\u0000\u0000\u0000\u045f\u0464\u0003d2\u0000\u0460\u0461\u00051"+
		"\u0000\u0000\u0461\u0463\u0003d2\u0000\u0462\u0460\u0001\u0000\u0000\u0000"+
		"\u0463\u0466\u0001\u0000\u0000\u0000\u0464\u0462\u0001\u0000\u0000\u0000"+
		"\u0464\u0465\u0001\u0000\u0000\u0000\u0465a\u0001\u0000\u0000\u0000\u0466"+
		"\u0464\u0001\u0000\u0000\u0000\u0467\u0468\u0003\u0114\u008a\u0000\u0468"+
		"\u0469\u0005d\u0000\u0000\u0469\u046b\u0001\u0000\u0000\u0000\u046a\u0467"+
		"\u0001\u0000\u0000\u0000\u046a\u046b\u0001\u0000\u0000\u0000\u046b\u046d"+
		"\u0001\u0000\u0000\u0000\u046c\u046e\u0003n7\u0000\u046d\u046c\u0001\u0000"+
		"\u0000\u0000\u046d\u046e\u0001\u0000\u0000\u0000\u046e\u046f\u0001\u0000"+
		"\u0000\u0000\u046f\u0470\u0003h4\u0000\u0470c\u0001\u0000\u0000\u0000"+
		"\u0471\u0472\u0003\u0296\u014b\u0000\u0472\u0473\u0005d\u0000\u0000\u0473"+
		"\u0475\u0001\u0000\u0000\u0000\u0474\u0471\u0001\u0000\u0000\u0000\u0474"+
		"\u0475\u0001\u0000\u0000\u0000\u0475\u0476\u0001\u0000\u0000\u0000\u0476"+
		"\u047c\u0003x<\u0000\u0477\u0478\u0003\u0090H\u0000\u0478\u0479\u0003"+
		"x<\u0000\u0479\u047b\u0001\u0000\u0000\u0000\u047a\u0477\u0001\u0000\u0000"+
		"\u0000\u047b\u047e\u0001\u0000\u0000\u0000\u047c\u047a\u0001\u0000\u0000"+
		"\u0000\u047c\u047d\u0001\u0000\u0000\u0000\u047de\u0001\u0000\u0000\u0000"+
		"\u047e\u047c\u0001\u0000\u0000\u0000\u047f\u0480\u0005\u0093\u0000\u0000"+
		"\u0480\u0481\u0005\u0005\u0000\u0000\u0481\u048e\u0005\u00d9\u0000\u0000"+
		"\u0482\u0484\u0005\u0093\u0000\u0000\u0483\u0485\u0005\u0005\u0000\u0000"+
		"\u0484\u0483\u0001\u0000\u0000\u0000\u0484\u0485\u0001\u0000\u0000\u0000"+
		"\u0485\u0486\u0001\u0000\u0000\u0000\u0486\u0488\u00051\u0000\u0000\u0487"+
		"\u0489\u0005\u0005\u0000\u0000\u0488\u0487\u0001\u0000\u0000\u0000\u0488"+
		"\u0489\u0001\u0000\u0000\u0000\u0489\u048a\u0001\u0000\u0000\u0000\u048a"+
		"\u048e\u0005\u00d9\u0000\u0000\u048b\u048e\u0005\u00c7\u0000\u0000\u048c"+
		"\u048e\u0005\u010f\u0000\u0000\u048d\u047f\u0001\u0000\u0000\u0000\u048d"+
		"\u0482\u0001\u0000\u0000\u0000\u048d\u048b\u0001\u0000\u0000\u0000\u048d"+
		"\u048c\u0001\u0000\u0000\u0000\u048eg\u0001\u0000\u0000\u0000\u048f\u0492"+
		"\u0003j5\u0000\u0490\u0492\u0003l6\u0000\u0491\u048f\u0001\u0000\u0000"+
		"\u0000\u0491\u0490\u0001\u0000\u0000\u0000\u0492i\u0001\u0000\u0000\u0000"+
		"\u0493\u0494\u0007\b\u0000\u0000\u0494\u0495\u0005\u009b\u0000\u0000\u0495"+
		"\u0496\u0003l6\u0000\u0496\u0497\u0005\u00ef\u0000\u0000\u0497k\u0001"+
		"\u0000\u0000\u0000\u0498\u04a1\u0003v;\u0000\u0499\u049b\u0003\u008eG"+
		"\u0000\u049a\u049c\u0003f3\u0000\u049b\u049a\u0001\u0000\u0000\u0000\u049b"+
		"\u049c\u0001\u0000\u0000\u0000\u049c\u049d\u0001\u0000\u0000\u0000\u049d"+
		"\u049e\u0003v;\u0000\u049e\u04a0\u0001\u0000\u0000\u0000\u049f\u0499\u0001"+
		"\u0000\u0000\u0000\u04a0\u04a3\u0001\u0000\u0000\u0000\u04a1\u049f\u0001"+
		"\u0000\u0000\u0000\u04a1\u04a2\u0001\u0000\u0000\u0000\u04a2\u04a6\u0001"+
		"\u0000\u0000\u0000\u04a3\u04a1\u0001\u0000\u0000\u0000\u04a4\u04a6\u0003"+
		"z=\u0000\u04a5\u0498\u0001\u0000\u0000\u0000\u04a5\u04a4\u0001\u0000\u0000"+
		"\u0000\u04a6\u04a7\u0001\u0000\u0000\u0000\u04a7\u04a5\u0001\u0000\u0000"+
		"\u0000\u04a7\u04a8\u0001\u0000\u0000\u0000\u04a8m\u0001\u0000\u0000\u0000"+
		"\u04a9\u04aa\u0005\u0015\u0000\u0000\u04aa\u04ac\u0005\u00fe\u0000\u0000"+
		"\u04ab\u04ad\u0003r9\u0000\u04ac\u04ab\u0001\u0000\u0000\u0000\u04ac\u04ad"+
		"\u0001\u0000\u0000\u0000\u04ad\u04cc\u0001\u0000\u0000\u0000\u04ae\u04af"+
		"\u0005\u0012\u0000\u0000\u04af\u04b1\u0005\u00fe\u0000\u0000\u04b0\u04b2"+
		"\u0003r9\u0000\u04b1\u04b0\u0001\u0000\u0000\u0000\u04b1\u04b2\u0001\u0000"+
		"\u0000\u0000\u04b2\u04cc\u0001\u0000\u0000\u0000\u04b3\u04b5\u0005\u0015"+
		"\u0000\u0000\u04b4\u04b6\u0005\u0005\u0000\u0000\u04b5\u04b4\u0001\u0000"+
		"\u0000\u0000\u04b5\u04b6\u0001\u0000\u0000\u0000\u04b6\u04b8\u0001\u0000"+
		"\u0000\u0000\u04b7\u04b9\u0003r9\u0000\u04b8\u04b7\u0001\u0000\u0000\u0000"+
		"\u04b8\u04b9\u0001\u0000\u0000\u0000\u04b9\u04cc\u0001\u0000\u0000\u0000"+
		"\u04ba\u04bc\u0005\u0012\u0000\u0000\u04bb\u04bd\u0003r9\u0000\u04bc\u04bb"+
		"\u0001\u0000\u0000\u0000\u04bc\u04bd\u0001\u0000\u0000\u0000\u04bd\u04cc"+
		"\u0001\u0000\u0000\u0000\u04be\u04c0\u0005\u00fe\u0000\u0000\u04bf\u04c1"+
		"\u0005\u0005\u0000\u0000\u04c0\u04bf\u0001\u0000\u0000\u0000\u04c0\u04c1"+
		"\u0001\u0000\u0000\u0000\u04c1\u04c3\u0001\u0000\u0000\u0000\u04c2\u04c4"+
		"\u0003r9\u0000\u04c3\u04c2\u0001\u0000\u0000\u0000\u04c3\u04c4\u0001\u0000"+
		"\u0000\u0000\u04c4\u04c5\u0001\u0000\u0000\u0000\u04c5\u04cc\u0003p8\u0000"+
		"\u04c6\u04c7\u0005\u00fe\u0000\u0000\u04c7\u04c9\u0005\u0005\u0000\u0000"+
		"\u04c8\u04ca\u0003r9\u0000\u04c9\u04c8\u0001\u0000\u0000\u0000\u04c9\u04ca"+
		"\u0001\u0000\u0000\u0000\u04ca\u04cc\u0001\u0000\u0000\u0000\u04cb\u04a9"+
		"\u0001\u0000\u0000\u0000\u04cb\u04ae\u0001\u0000\u0000\u0000\u04cb\u04b3"+
		"\u0001\u0000\u0000\u0000\u04cb\u04ba\u0001\u0000\u0000\u0000\u04cb\u04be"+
		"\u0001\u0000\u0000\u0000\u04cb\u04c6\u0001\u0000\u0000\u0000\u04cco\u0001"+
		"\u0000\u0000\u0000\u04cd\u04ce\u0007\t\u0000\u0000\u04ceq\u0001\u0000"+
		"\u0000\u0000\u04cf\u04d0\u0007\n\u0000\u0000\u04d0s\u0001\u0000\u0000"+
		"\u0000\u04d1\u04d5\u0003v;\u0000\u04d2\u04d3\u0003\u008eG\u0000\u04d3"+
		"\u04d4\u0003v;\u0000\u04d4\u04d6\u0001\u0000\u0000\u0000\u04d5\u04d2\u0001"+
		"\u0000\u0000\u0000\u04d6\u04d7\u0001\u0000\u0000\u0000\u04d7\u04d5\u0001"+
		"\u0000\u0000\u0000\u04d7\u04d8\u0001\u0000\u0000\u0000\u04d8u\u0001\u0000"+
		"\u0000\u0000\u04d9\u04db\u0005\u009b\u0000\u0000\u04da\u04dc\u0003\u0114"+
		"\u008a\u0000\u04db\u04da\u0001\u0000\u0000\u0000\u04db\u04dc\u0001\u0000"+
		"\u0000\u0000\u04dc\u04de\u0001\u0000\u0000\u0000\u04dd\u04df\u0003\u009a"+
		"M\u0000\u04de\u04dd\u0001\u0000\u0000\u0000\u04de\u04df\u0001\u0000\u0000"+
		"\u0000\u04df\u04e1\u0001\u0000\u0000\u0000\u04e0\u04e2\u0003\u008cF\u0000"+
		"\u04e1\u04e0\u0001\u0000\u0000\u0000\u04e1\u04e2\u0001\u0000\u0000\u0000"+
		"\u04e2\u04e5\u0001\u0000\u0000\u0000\u04e3\u04e4\u0005\u012d\u0000\u0000"+
		"\u04e4\u04e6\u0003\u00b0X\u0000\u04e5\u04e3\u0001\u0000\u0000\u0000\u04e5"+
		"\u04e6\u0001\u0000\u0000\u0000\u04e6\u04e7\u0001\u0000\u0000\u0000\u04e7"+
		"\u04e8\u0005\u00ef\u0000\u0000\u04e8w\u0001\u0000\u0000\u0000\u04e9\u04eb"+
		"\u0005\u009b\u0000\u0000\u04ea\u04ec\u0003\u0114\u008a\u0000\u04eb\u04ea"+
		"\u0001\u0000\u0000\u0000\u04eb\u04ec\u0001\u0000\u0000\u0000\u04ec\u04ee"+
		"\u0001\u0000\u0000\u0000\u04ed\u04ef\u0003\u00acV\u0000\u04ee\u04ed\u0001"+
		"\u0000\u0000\u0000\u04ee\u04ef\u0001\u0000\u0000\u0000\u04ef\u04f1\u0001"+
		"\u0000\u0000\u0000\u04f0\u04f2\u0003\u028e\u0147\u0000\u04f1\u04f0\u0001"+
		"\u0000\u0000\u0000\u04f1\u04f2\u0001\u0000\u0000\u0000\u04f2\u04f3\u0001"+
		"\u0000\u0000\u0000\u04f3\u04f4\u0005\u00ef\u0000\u0000\u04f4y\u0001\u0000"+
		"\u0000\u0000\u04f5\u04f6\u0005\u009b\u0000\u0000\u04f6\u04f9\u0003b1\u0000"+
		"\u04f7\u04f8\u0005\u012d\u0000\u0000\u04f8\u04fa\u0003\u00b0X\u0000\u04f9"+
		"\u04f7\u0001\u0000\u0000\u0000\u04f9\u04fa\u0001\u0000\u0000\u0000\u04fa"+
		"\u04fb\u0001\u0000\u0000\u0000\u04fb\u04fd\u0005\u00ef\u0000\u0000\u04fc"+
		"\u04fe\u0003f3\u0000\u04fd\u04fc\u0001\u0000\u0000\u0000\u04fd\u04fe\u0001"+
		"\u0000\u0000\u0000\u04fe{\u0001\u0000\u0000\u0000\u04ff\u0502\u0003\u0086"+
		"C\u0000\u0500\u0502\u0003\u0084B\u0000\u0501\u04ff\u0001\u0000\u0000\u0000"+
		"\u0501\u0500\u0001\u0000\u0000\u0000\u0502\u0503\u0001\u0000\u0000\u0000"+
		"\u0503\u0501\u0001\u0000\u0000\u0000\u0503\u0504\u0001\u0000\u0000\u0000"+
		"\u0504}\u0001\u0000\u0000\u0000\u0505\u0508\u0005\u008b\u0000\u0000\u0506"+
		"\u0509\u0003\u0296\u014b\u0000\u0507\u0509\u0003\u0080@\u0000\u0508\u0506"+
		"\u0001\u0000\u0000\u0000\u0508\u0507\u0001\u0000\u0000\u0000\u0509\u050e"+
		"\u0001\u0000\u0000\u0000\u050a\u050d\u0003\u0086C\u0000\u050b\u050d\u0003"+
		"\u0084B\u0000\u050c\u050a\u0001\u0000\u0000\u0000\u050c\u050b\u0001\u0000"+
		"\u0000\u0000\u050d\u0510\u0001\u0000\u0000\u0000\u050e\u050c\u0001\u0000"+
		"\u0000\u0000\u050e\u050f\u0001\u0000\u0000\u0000\u050f\u007f\u0001\u0000"+
		"\u0000\u0000\u0510\u050e\u0001\u0000\u0000\u0000\u0511\u0512\u0005P\u0000"+
		"\u0000\u0512\u0513\u0005\u009b\u0000\u0000\u0513\u0514\u0003\u00b0X\u0000"+
		"\u0514\u0515\u0005\u00ef\u0000\u0000\u0515\u0081\u0001\u0000\u0000\u0000"+
		"\u0516\u0518\u0005P\u0000\u0000\u0517\u0519\u0007\u000b\u0000\u0000\u0518"+
		"\u0517\u0001\u0000\u0000\u0000\u0518\u0519\u0001\u0000\u0000\u0000\u0519"+
		"\u051a\u0001\u0000\u0000\u0000\u051a\u051b\u0005\u009b\u0000\u0000\u051b"+
		"\u051c\u0003\u00b0X\u0000\u051c\u051d\u0005\u00ef\u0000\u0000\u051d\u0083"+
		"\u0001\u0000\u0000\u0000\u051e\u051f\u0005/\u0000\u0000\u051f\u0520\u0003"+
		"\u0080@\u0000\u0520\u0085\u0001\u0000\u0000\u0000\u0521\u0522\u0005/\u0000"+
		"\u0000\u0522\u0523\u0003\u0296\u014b\u0000\u0523\u0087\u0001\u0000\u0000"+
		"\u0000\u0524\u0525\u0005/\u0000\u0000\u0525\u0526\u0003\u0296\u014b\u0000"+
		"\u0526\u0089\u0001\u0000\u0000\u0000\u0527\u0528\u0005/\u0000\u0000\u0528"+
		"\u0529\u0003\u0296\u014b\u0000\u0529\u008b\u0001\u0000\u0000\u0000\u052a"+
		"\u052d\u0003\u028e\u0147\u0000\u052b\u052d\u0003\u0108\u0084\u0000\u052c"+
		"\u052a\u0001\u0000\u0000\u0000\u052c\u052b\u0001\u0000\u0000\u0000\u052d"+
		"\u008d\u0001\u0000\u0000\u0000\u052e\u0530\u0003\u0092I\u0000\u052f\u052e"+
		"\u0001\u0000\u0000\u0000\u052f\u0530\u0001\u0000\u0000\u0000\u0530\u0531"+
		"\u0001\u0000\u0000\u0000\u0531\u0544\u0003\u0094J\u0000\u0532\u0534\u0005"+
		"\u0092\u0000\u0000\u0533\u0535\u0003\u0114\u008a\u0000\u0534\u0533\u0001"+
		"\u0000\u0000\u0000\u0534\u0535\u0001\u0000\u0000\u0000\u0535\u0537\u0001"+
		"\u0000\u0000\u0000\u0536\u0538\u0003\u009aM\u0000\u0537\u0536\u0001\u0000"+
		"\u0000\u0000\u0537\u0538\u0001\u0000\u0000\u0000\u0538\u053a\u0001\u0000"+
		"\u0000\u0000\u0539\u053b\u0003\u0098L\u0000\u053a\u0539\u0001\u0000\u0000"+
		"\u0000\u053a\u053b\u0001\u0000\u0000\u0000\u053b\u053d\u0001\u0000\u0000"+
		"\u0000\u053c\u053e\u0003\u008cF\u0000\u053d\u053c\u0001\u0000\u0000\u0000"+
		"\u053d\u053e\u0001\u0000\u0000\u0000\u053e\u0541\u0001\u0000\u0000\u0000"+
		"\u053f\u0540\u0005\u012d\u0000\u0000\u0540\u0542\u0003\u00b0X\u0000\u0541"+
		"\u053f\u0001\u0000\u0000\u0000\u0541\u0542\u0001\u0000\u0000\u0000\u0542"+
		"\u0543\u0001\u0000\u0000\u0000\u0543\u0545\u0005\u00d8\u0000\u0000\u0544"+
		"\u0532\u0001\u0000\u0000\u0000\u0544\u0545\u0001\u0000\u0000\u0000\u0545"+
		"\u0546\u0001\u0000\u0000\u0000\u0546\u0548\u0003\u0094J\u0000\u0547\u0549"+
		"\u0003\u0096K\u0000\u0548\u0547\u0001\u0000\u0000\u0000\u0548\u0549\u0001"+
		"\u0000\u0000\u0000\u0549\u008f\u0001\u0000\u0000\u0000\u054a\u054c\u0003"+
		"\u0092I\u0000\u054b\u054a\u0001\u0000\u0000\u0000\u054b\u054c\u0001\u0000"+
		"\u0000\u0000\u054c\u054d\u0001\u0000\u0000\u0000\u054d\u054e\u0003\u0094"+
		"J\u0000\u054e\u0550\u0005\u0092\u0000\u0000\u054f\u0551\u0003\u0114\u008a"+
		"\u0000\u0550\u054f\u0001\u0000\u0000\u0000\u0550\u0551\u0001\u0000\u0000"+
		"\u0000\u0551\u0552\u0001\u0000\u0000\u0000\u0552\u0554\u0003\u00aeW\u0000"+
		"\u0553\u0555\u0003\u028e\u0147\u0000\u0554\u0553\u0001\u0000\u0000\u0000"+
		"\u0554\u0555\u0001\u0000\u0000\u0000\u0555\u0556\u0001\u0000\u0000\u0000"+
		"\u0556\u0557\u0005\u00d8\u0000\u0000\u0557\u0559\u0003\u0094J\u0000\u0558"+
		"\u055a\u0003\u0096K\u0000\u0559\u0558\u0001\u0000\u0000\u0000\u0559\u055a"+
		"\u0001\u0000\u0000\u0000\u055a\u0091\u0001\u0000\u0000\u0000\u055b\u055c"+
		"\u0007\f\u0000\u0000\u055c\u0093\u0001\u0000\u0000\u0000\u055d\u055e\u0007"+
		"\r\u0000\u0000\u055e\u0095\u0001\u0000\u0000\u0000\u055f\u0560\u0007\u000e"+
		"\u0000\u0000\u0560\u0097\u0001\u0000\u0000\u0000\u0561\u056a\u0005\u010f"+
		"\u0000\u0000\u0562\u0564\u0005\u0005\u0000\u0000\u0563\u0562\u0001\u0000"+
		"\u0000\u0000\u0563\u0564\u0001\u0000\u0000\u0000\u0564\u0565\u0001\u0000"+
		"\u0000\u0000\u0565\u0567\u0005T\u0000\u0000\u0566\u0568\u0005\u0005\u0000"+
		"\u0000\u0567\u0566\u0001\u0000\u0000\u0000\u0567\u0568\u0001\u0000\u0000"+
		"\u0000\u0568\u056b\u0001\u0000\u0000\u0000\u0569\u056b\u0005\u0005\u0000"+
		"\u0000\u056a\u0563\u0001\u0000\u0000\u0000\u056a\u0569\u0001\u0000\u0000"+
		"\u0000\u056a\u056b\u0001\u0000\u0000\u0000\u056b\u0099\u0001\u0000\u0000"+
		"\u0000\u056c\u056d\u0005/\u0000\u0000\u056d\u0571\u0003\u009cN\u0000\u056e"+
		"\u056f\u0005\u008b\u0000\u0000\u056f\u0571\u0003\u009eO\u0000\u0570\u056c"+
		"\u0001\u0000\u0000\u0000\u0570\u056e\u0001\u0000\u0000\u0000\u0571\u009b"+
		"\u0001\u0000\u0000\u0000\u0572\u057a\u0003\u00a0P\u0000\u0573\u0575\u0005"+
		"\u001e\u0000\u0000\u0574\u0576\u0005/\u0000\u0000\u0575\u0574\u0001\u0000"+
		"\u0000\u0000\u0575\u0576\u0001\u0000\u0000\u0000\u0576\u0577\u0001\u0000"+
		"\u0000\u0000\u0577\u0579\u0003\u00a0P\u0000\u0578\u0573\u0001\u0000\u0000"+
		"\u0000\u0579\u057c\u0001\u0000\u0000\u0000\u057a\u0578\u0001\u0000\u0000"+
		"\u0000\u057a\u057b\u0001\u0000\u0000\u0000\u057b\u009d\u0001\u0000\u0000"+
		"\u0000\u057c\u057a\u0001\u0000\u0000\u0000\u057d\u0585\u0003\u00a2Q\u0000"+
		"\u057e\u0580\u0005\u001e\u0000\u0000\u057f\u0581\u0005/\u0000\u0000\u0580"+
		"\u057f\u0001\u0000\u0000\u0000\u0580\u0581\u0001\u0000\u0000\u0000\u0581"+
		"\u0582\u0001\u0000\u0000\u0000\u0582\u0584\u0003\u00a2Q\u0000\u0583\u057e"+
		"\u0001\u0000\u0000\u0000\u0584\u0587\u0001\u0000\u0000\u0000\u0585\u0583"+
		"\u0001\u0000\u0000\u0000\u0585\u0586\u0001\u0000\u0000\u0000\u0586\u009f"+
		"\u0001\u0000\u0000\u0000\u0587\u0585\u0001\u0000\u0000\u0000\u0588\u058d"+
		"\u0003\u00a4R\u0000\u0589\u058a\u0007\u000f\u0000\u0000\u058a\u058c\u0003"+
		"\u00a4R\u0000\u058b\u0589\u0001\u0000\u0000\u0000\u058c\u058f\u0001\u0000"+
		"\u0000\u0000\u058d\u058b\u0001\u0000\u0000\u0000\u058d\u058e\u0001\u0000"+
		"\u0000\u0000\u058e\u00a1\u0001\u0000\u0000\u0000\u058f\u058d\u0001\u0000"+
		"\u0000\u0000\u0590\u0595\u0003\u00a6S\u0000\u0591\u0592\u0007\u000f\u0000"+
		"\u0000\u0592\u0594\u0003\u00a6S\u0000\u0593\u0591\u0001\u0000\u0000\u0000"+
		"\u0594\u0597\u0001\u0000\u0000\u0000\u0595\u0593\u0001\u0000\u0000\u0000"+
		"\u0595\u0596\u0001\u0000\u0000\u0000\u0596\u00a3\u0001\u0000\u0000\u0000"+
		"\u0597\u0595\u0001\u0000\u0000\u0000\u0598\u059a\u0005\u0091\u0000\u0000"+
		"\u0599\u0598\u0001\u0000\u0000\u0000\u059a\u059d\u0001\u0000\u0000\u0000"+
		"\u059b\u0599\u0001\u0000\u0000\u0000\u059b\u059c\u0001\u0000\u0000\u0000"+
		"\u059c\u059e\u0001\u0000\u0000\u0000\u059d\u059b\u0001\u0000\u0000\u0000"+
		"\u059e\u059f\u0003\u00a8T\u0000\u059f\u00a5\u0001\u0000\u0000\u0000\u05a0"+
		"\u05a2\u0005\u0091\u0000\u0000\u05a1\u05a0\u0001\u0000\u0000\u0000\u05a2"+
		"\u05a5\u0001\u0000\u0000\u0000\u05a3\u05a1\u0001\u0000\u0000\u0000\u05a3"+
		"\u05a4\u0001\u0000\u0000\u0000\u05a4\u05a6\u0001\u0000\u0000\u0000\u05a5"+
		"\u05a3\u0001\u0000\u0000\u0000\u05a6\u05a7\u0003\u00aaU\u0000\u05a7\u00a7"+
		"\u0001\u0000\u0000\u0000\u05a8\u05a9\u0005\u009b\u0000\u0000\u05a9\u05aa"+
		"\u0003\u009cN\u0000\u05aa\u05ab\u0005\u00ef\u0000\u0000\u05ab\u05b0\u0001"+
		"\u0000\u0000\u0000\u05ac\u05b0\u0005\u00a2\u0000\u0000\u05ad\u05b0\u0003"+
		"\u0082A\u0000\u05ae\u05b0\u0003\u0296\u014b\u0000\u05af\u05a8\u0001\u0000"+
		"\u0000\u0000\u05af\u05ac\u0001\u0000\u0000\u0000\u05af\u05ad\u0001\u0000"+
		"\u0000\u0000\u05af\u05ae\u0001\u0000\u0000\u0000\u05b0\u00a9\u0001\u0000"+
		"\u0000\u0000\u05b1\u05b2\u0005\u009b\u0000\u0000\u05b2\u05b3\u0003\u009e"+
		"O\u0000\u05b3\u05b4\u0005\u00ef\u0000\u0000\u05b4\u05b9\u0001\u0000\u0000"+
		"\u0000\u05b5\u05b9\u0005\u00a2\u0000\u0000\u05b6\u05b9\u0003\u0082A\u0000"+
		"\u05b7\u05b9\u0003\u029c\u014e\u0000\u05b8\u05b1\u0001\u0000\u0000\u0000"+
		"\u05b8\u05b5\u0001\u0000\u0000\u0000\u05b8\u05b6\u0001\u0000\u0000\u0000"+
		"\u05b8\u05b7\u0001\u0000\u0000\u0000\u05b9\u00ab\u0001\u0000\u0000\u0000"+
		"\u05ba\u05bb\u0007\u0010\u0000\u0000\u05bb\u05c0\u0003\u0296\u014b\u0000"+
		"\u05bc\u05bd\u0007\u000f\u0000\u0000\u05bd\u05bf\u0003\u0296\u014b\u0000"+
		"\u05be\u05bc\u0001\u0000\u0000\u0000\u05bf\u05c2\u0001\u0000\u0000\u0000"+
		"\u05c0\u05be\u0001\u0000\u0000\u0000\u05c0\u05c1\u0001\u0000\u0000\u0000"+
		"\u05c1\u00ad\u0001\u0000\u0000\u0000\u05c2\u05c0\u0001\u0000\u0000\u0000"+
		"\u05c3\u05c4\u0007\u0010\u0000\u0000\u05c4\u05c5\u0003\u0296\u014b\u0000"+
		"\u05c5\u00af\u0001\u0000\u0000\u0000\u05c6\u05cb\u0003\u00b2Y\u0000\u05c7"+
		"\u05c8\u0005\u00be\u0000\u0000\u05c8\u05ca\u0003\u00b2Y\u0000\u05c9\u05c7"+
		"\u0001\u0000\u0000\u0000\u05ca\u05cd\u0001\u0000\u0000\u0000\u05cb\u05c9"+
		"\u0001\u0000\u0000\u0000\u05cb\u05cc\u0001\u0000\u0000\u0000\u05cc\u00b1"+
		"\u0001\u0000\u0000\u0000\u05cd\u05cb\u0001\u0000\u0000\u0000\u05ce\u05d3"+
		"\u0003\u00b4Z\u0000\u05cf\u05d0\u0005\u0131\u0000\u0000\u05d0\u05d2\u0003"+
		"\u00b4Z\u0000\u05d1\u05cf\u0001\u0000\u0000\u0000\u05d2\u05d5\u0001\u0000"+
		"\u0000\u0000\u05d3\u05d1\u0001\u0000\u0000\u0000\u05d3\u05d4\u0001\u0000"+
		"\u0000\u0000\u05d4\u00b3\u0001\u0000\u0000\u0000\u05d5\u05d3\u0001\u0000"+
		"\u0000\u0000\u05d6\u05db\u0003\u00b6[\u0000\u05d7\u05d8\u0005\u0014\u0000"+
		"\u0000\u05d8\u05da\u0003\u00b6[\u0000\u05d9\u05d7\u0001\u0000\u0000\u0000"+
		"\u05da\u05dd\u0001\u0000\u0000\u0000\u05db\u05d9\u0001\u0000\u0000\u0000"+
		"\u05db\u05dc\u0001\u0000\u0000\u0000\u05dc\u00b5\u0001\u0000\u0000\u0000"+
		"\u05dd\u05db\u0001\u0000\u0000\u0000\u05de\u05e0\u0005\u00b3\u0000\u0000"+
		"\u05df\u05de\u0001\u0000\u0000\u0000\u05e0\u05e3\u0001\u0000\u0000\u0000"+
		"\u05e1\u05df\u0001\u0000\u0000\u0000\u05e1\u05e2\u0001\u0000\u0000\u0000"+
		"\u05e2\u05e4\u0001\u0000\u0000\u0000\u05e3\u05e1\u0001\u0000\u0000\u0000"+
		"\u05e4\u05e5\u0003\u00b8\\\u0000\u05e5\u00b7\u0001\u0000\u0000\u0000\u05e6"+
		"\u05eb\u0003\u00ba]\u0000\u05e7\u05e8\u0007\u0011\u0000\u0000\u05e8\u05ea"+
		"\u0003\u00ba]\u0000\u05e9\u05e7\u0001\u0000\u0000\u0000\u05ea\u05ed\u0001"+
		"\u0000\u0000\u0000\u05eb\u05e9\u0001\u0000\u0000\u0000\u05eb\u05ec\u0001"+
		"\u0000\u0000\u0000\u05ec\u00b9\u0001\u0000\u0000\u0000\u05ed\u05eb\u0001"+
		"\u0000\u0000\u0000\u05ee\u05f0\u0003\u00c0`\u0000\u05ef\u05f1\u0003\u00bc"+
		"^\u0000\u05f0\u05ef\u0001\u0000\u0000\u0000\u05f0\u05f1\u0001\u0000\u0000"+
		"\u0000\u05f1\u00bb\u0001\u0000\u0000\u0000\u05f2\u05fa\u0005\u00de\u0000"+
		"\u0000\u05f3\u05f4\u0005\u0104\u0000\u0000\u05f4\u05fa\u0005\u012e\u0000"+
		"\u0000\u05f5\u05f6\u0005c\u0000\u0000\u05f6\u05fa\u0005\u012e\u0000\u0000"+
		"\u05f7\u05fa\u00059\u0000\u0000\u05f8\u05fa\u0005\u0083\u0000\u0000\u05f9"+
		"\u05f2\u0001\u0000\u0000\u0000\u05f9\u05f3\u0001\u0000\u0000\u0000\u05f9"+
		"\u05f5\u0001\u0000\u0000\u0000\u05f9\u05f7\u0001\u0000\u0000\u0000\u05f9"+
		"\u05f8\u0001\u0000\u0000\u0000\u05fa\u05fb\u0001\u0000\u0000\u0000\u05fb"+
		"\u0613\u0003\u00c0`\u0000\u05fc\u05fe\u0005\u008b\u0000\u0000\u05fd\u05ff"+
		"\u0005\u00b3\u0000\u0000\u05fe\u05fd\u0001\u0000\u0000\u0000\u05fe\u05ff"+
		"\u0001\u0000\u0000\u0000\u05ff\u0600\u0001\u0000\u0000\u0000\u0600\u0613"+
		"\u0005\u00b6\u0000\u0000\u0601\u0603\u0005\u008b\u0000\u0000\u0602\u0604"+
		"\u0005\u00b3\u0000\u0000\u0603\u0602\u0001\u0000\u0000\u0000\u0603\u0604"+
		"\u0001\u0000\u0000\u0000\u0604\u0605\u0001\u0000\u0000\u0000\u0605\u0608"+
		"\u0007\u0012\u0000\u0000\u0606\u0608\u00050\u0000\u0000\u0607\u0601\u0001"+
		"\u0000\u0000\u0000\u0607\u0606\u0001\u0000\u0000\u0000\u0608\u0609\u0001"+
		"\u0000\u0000\u0000\u0609\u0613\u0003\u0118\u008c\u0000\u060a\u060c\u0005"+
		"\u008b\u0000\u0000\u060b\u060d\u0005\u00b3\u0000\u0000\u060c\u060b\u0001"+
		"\u0000\u0000\u0000\u060c\u060d\u0001\u0000\u0000\u0000\u060d\u060f\u0001"+
		"\u0000\u0000\u0000\u060e\u0610\u0003\u00be_\u0000\u060f\u060e\u0001\u0000"+
		"\u0000\u0000\u060f\u0610\u0001\u0000\u0000\u0000\u0610\u0611\u0001\u0000"+
		"\u0000\u0000\u0611\u0613\u0005\u00b2\u0000\u0000\u0612\u05f9\u0001\u0000"+
		"\u0000\u0000\u0612\u05fc\u0001\u0000\u0000\u0000\u0612\u0607\u0001\u0000"+
		"\u0000\u0000\u0612\u060a\u0001\u0000\u0000\u0000\u0613\u00bd\u0001\u0000"+
		"\u0000\u0000\u0614\u0615\u0007\u0013\u0000\u0000\u0615\u00bf\u0001\u0000"+
		"\u0000\u0000\u0616\u061b\u0003\u00c2a\u0000\u0617\u0618\u0007\u0014\u0000"+
		"\u0000\u0618\u061a\u0003\u00c2a\u0000\u0619\u0617\u0001\u0000\u0000\u0000"+
		"\u061a\u061d\u0001\u0000\u0000\u0000\u061b\u0619\u0001\u0000\u0000\u0000"+
		"\u061b\u061c\u0001\u0000\u0000\u0000\u061c\u00c1\u0001\u0000\u0000\u0000"+
		"\u061d\u061b\u0001\u0000\u0000\u0000\u061e\u0623\u0003\u00c4b\u0000\u061f"+
		"\u0620\u0007\u0015\u0000\u0000\u0620\u0622\u0003\u00c4b\u0000\u0621\u061f"+
		"\u0001\u0000\u0000\u0000\u0622\u0625\u0001\u0000\u0000\u0000\u0623\u0621"+
		"\u0001\u0000\u0000\u0000\u0623\u0624\u0001\u0000\u0000\u0000\u0624\u00c3"+
		"\u0001\u0000\u0000\u0000\u0625\u0623\u0001\u0000\u0000\u0000\u0626\u062b"+
		"\u0003\u00c6c\u0000\u0627\u0628\u0005\u00cb\u0000\u0000\u0628\u062a\u0003"+
		"\u00c6c\u0000\u0629\u0627\u0001\u0000\u0000\u0000\u062a\u062d\u0001\u0000"+
		"\u0000\u0000\u062b\u0629\u0001\u0000\u0000\u0000\u062b\u062c\u0001\u0000"+
		"\u0000\u0000\u062c\u00c5\u0001\u0000\u0000\u0000\u062d\u062b\u0001\u0000"+
		"\u0000\u0000\u062e\u0632\u0003\u00c8d\u0000\u062f\u0630\u0007\u0016\u0000"+
		"\u0000\u0630\u0632\u0003\u00c8d\u0000\u0631\u062e\u0001\u0000\u0000\u0000"+
		"\u0631\u062f\u0001\u0000\u0000\u0000\u0632\u00c7\u0001\u0000\u0000\u0000"+
		"\u0633\u0637\u0003\u00d4j\u0000\u0634\u0636\u0003\u00cae\u0000\u0635\u0634"+
		"\u0001\u0000\u0000\u0000\u0636\u0639\u0001\u0000\u0000\u0000\u0637\u0635"+
		"\u0001\u0000\u0000\u0000\u0637\u0638\u0001\u0000\u0000\u0000\u0638\u00c9"+
		"\u0001\u0000\u0000\u0000\u0639\u0637\u0001\u0000\u0000\u0000\u063a\u064a"+
		"\u0003\u00ccf\u0000\u063b\u064a\u0003\u009aM\u0000\u063c\u063d\u0005\u0092"+
		"\u0000\u0000\u063d\u063e\u0003\u00b0X\u0000\u063e\u063f\u0005\u00d8\u0000"+
		"\u0000\u063f\u064a\u0001\u0000\u0000\u0000\u0640\u0642\u0005\u0092\u0000"+
		"\u0000\u0641\u0643\u0003\u00b0X\u0000\u0642\u0641\u0001\u0000\u0000\u0000"+
		"\u0642\u0643\u0001\u0000\u0000\u0000\u0643\u0644\u0001\u0000\u0000\u0000"+
		"\u0644\u0646\u0005T\u0000\u0000\u0645\u0647\u0003\u00b0X\u0000\u0646\u0645"+
		"\u0001\u0000\u0000\u0000\u0646\u0647\u0001\u0000\u0000\u0000\u0647\u0648"+
		"\u0001\u0000\u0000\u0000\u0648\u064a\u0005\u00d8\u0000\u0000\u0649\u063a"+
		"\u0001\u0000\u0000\u0000\u0649\u063b\u0001\u0000\u0000\u0000\u0649\u063c"+
		"\u0001\u0000\u0000\u0000\u0649\u0640\u0001\u0000\u0000\u0000\u064a\u00cb"+
		"\u0001\u0000\u0000\u0000\u064b\u064c\u0005S\u0000\u0000\u064c\u064d\u0003"+
		"\u0106\u0083\u0000\u064d\u00cd\u0001\u0000\u0000\u0000\u064e\u064f\u0005"+
		"\u0092\u0000\u0000\u064f\u0650\u0003\u00b0X\u0000\u0650\u0651\u0005\u00d8"+
		"\u0000\u0000\u0651\u00cf\u0001\u0000\u0000\u0000\u0652\u0654\u0003\u00d4"+
		"j\u0000\u0653\u0655\u0003\u00ccf\u0000\u0654\u0653\u0001\u0000\u0000\u0000"+
		"\u0655\u0656\u0001\u0000\u0000\u0000\u0656\u0654\u0001\u0000\u0000\u0000"+
		"\u0656\u0657\u0001\u0000\u0000\u0000\u0657\u00d1\u0001\u0000\u0000\u0000"+
		"\u0658\u0659\u0003\u00d4j\u0000\u0659\u065a\u0003\u00ceg\u0000\u065a\u00d3"+
		"\u0001\u0000\u0000\u0000\u065b\u0671\u0003\u00d6k\u0000\u065c\u0671\u0003"+
		"\u0108\u0084\u0000\u065d\u0671\u0003\u00d8l\u0000\u065e\u0671\u0003\u00dc"+
		"n\u0000\u065f\u0671\u0003\u00f8|\u0000\u0660\u0671\u0003\u00fa}\u0000"+
		"\u0661\u0671\u0003\u00fc~\u0000\u0662\u0671\u0003\u00fe\u007f\u0000\u0663"+
		"\u0671\u0003\u00f4z\u0000\u0664\u0671\u0003\u00e2q\u0000\u0665\u0671\u0003"+
		"\u0104\u0082\u0000\u0666\u0671\u0003\u00e4r\u0000\u0667\u0671\u0003\u00e6"+
		"s\u0000\u0668\u0671\u0003\u00e8t\u0000\u0669\u0671\u0003\u00eau\u0000"+
		"\u066a\u0671\u0003\u00ecv\u0000\u066b\u0671\u0003\u00eew\u0000\u066c\u0671"+
		"\u0003\u00f0x\u0000\u066d\u0671\u0003\u00f2y\u0000\u066e\u0671\u0003\u010c"+
		"\u0086\u0000\u066f\u0671\u0003\u0114\u008a\u0000\u0670\u065b\u0001\u0000"+
		"\u0000\u0000\u0670\u065c\u0001\u0000\u0000\u0000\u0670\u065d\u0001\u0000"+
		"\u0000\u0000\u0670\u065e\u0001\u0000\u0000\u0000\u0670\u065f\u0001\u0000"+
		"\u0000\u0000\u0670\u0660\u0001\u0000\u0000\u0000\u0670\u0661\u0001\u0000"+
		"\u0000\u0000\u0670\u0662\u0001\u0000\u0000\u0000\u0670\u0663\u0001\u0000"+
		"\u0000\u0000\u0670\u0664\u0001\u0000\u0000\u0000\u0670\u0665\u0001\u0000"+
		"\u0000\u0000\u0670\u0666\u0001\u0000\u0000\u0000\u0670\u0667\u0001\u0000"+
		"\u0000\u0000\u0670\u0668\u0001\u0000\u0000\u0000\u0670\u0669\u0001\u0000"+
		"\u0000\u0000\u0670\u066a\u0001\u0000\u0000\u0000\u0670\u066b\u0001\u0000"+
		"\u0000\u0000\u0670\u066c\u0001\u0000\u0000\u0000\u0670\u066d\u0001\u0000"+
		"\u0000\u0000\u0670\u066e\u0001\u0000\u0000\u0000\u0670\u066f\u0001\u0000"+
		"\u0000\u0000\u0671\u00d5\u0001\u0000\u0000\u0000\u0672\u067c\u0003\u0100"+
		"\u0080\u0000\u0673\u067c\u0003\u0284\u0142\u0000\u0674\u067c\u0003\u028e"+
		"\u0147\u0000\u0675\u067c\u0005\u0119\u0000\u0000\u0676\u067c\u0005l\u0000"+
		"\u0000\u0677\u067c\u0005\u0086\u0000\u0000\u0678\u067c\u0005\u0087\u0000"+
		"\u0000\u0679\u067c\u0005\u00a7\u0000\u0000\u067a\u067c\u0005\u00b6\u0000"+
		"\u0000\u067b\u0672\u0001\u0000\u0000\u0000\u067b\u0673\u0001\u0000\u0000"+
		"\u0000\u067b\u0674\u0001\u0000\u0000\u0000\u067b\u0675\u0001\u0000\u0000"+
		"\u0000\u067b\u0676\u0001\u0000\u0000\u0000\u067b\u0677\u0001\u0000\u0000"+
		"\u0000\u067b\u0678\u0001\u0000\u0000\u0000\u067b\u0679\u0001\u0000\u0000"+
		"\u0000\u067b\u067a\u0001\u0000\u0000\u0000\u067c\u00d7\u0001\u0000\u0000"+
		"\u0000\u067d\u067f\u0005+\u0000\u0000\u067e\u0680\u0003\u00dam\u0000\u067f"+
		"\u067e\u0001\u0000\u0000\u0000\u0680\u0681\u0001\u0000\u0000\u0000\u0681"+
		"\u067f\u0001\u0000\u0000\u0000\u0681\u0682\u0001\u0000\u0000\u0000\u0682"+
		"\u0685\u0001\u0000\u0000\u0000\u0683\u0684\u0005`\u0000\u0000\u0684\u0686"+
		"\u0003\u00b0X\u0000\u0685\u0683\u0001\u0000\u0000\u0000\u0685\u0686\u0001"+
		"\u0000\u0000\u0000\u0686\u0687\u0001\u0000\u0000\u0000\u0687\u0688\u0005"+
		"b\u0000\u0000\u0688\u00d9\u0001\u0000\u0000\u0000\u0689\u068a\u0005\u012c"+
		"\u0000\u0000\u068a\u068b\u0003\u00b0X\u0000\u068b\u068c\u0005\u010d\u0000"+
		"\u0000\u068c\u068d\u0003\u00b0X\u0000\u068d\u00db\u0001\u0000\u0000\u0000"+
		"\u068e\u068f\u0005+\u0000\u0000\u068f\u0691\u0003\u00b0X\u0000\u0690\u0692"+
		"\u0003\u00deo\u0000\u0691\u0690\u0001\u0000\u0000\u0000\u0692\u0693\u0001"+
		"\u0000\u0000\u0000\u0693\u0691\u0001\u0000\u0000\u0000\u0693\u0694\u0001"+
		"\u0000\u0000\u0000\u0694\u0697\u0001\u0000\u0000\u0000\u0695\u0696\u0005"+
		"`\u0000\u0000\u0696\u0698\u0003\u00b0X\u0000\u0697\u0695\u0001\u0000\u0000"+
		"\u0000\u0697\u0698\u0001\u0000\u0000\u0000\u0698\u0699\u0001\u0000\u0000"+
		"\u0000\u0699\u069a\u0005b\u0000\u0000\u069a\u00dd\u0001\u0000\u0000\u0000"+
		"\u069b\u069c\u0005\u012c\u0000\u0000\u069c\u06a1\u0003\u00e0p\u0000\u069d"+
		"\u069e\u00051\u0000\u0000\u069e\u06a0\u0003\u00e0p\u0000\u069f\u069d\u0001"+
		"\u0000\u0000\u0000\u06a0\u06a3\u0001\u0000\u0000\u0000\u06a1\u069f\u0001"+
		"\u0000\u0000\u0000\u06a1\u06a2\u0001\u0000\u0000\u0000\u06a2\u06a4\u0001"+
		"\u0000\u0000\u0000\u06a3\u06a1\u0001\u0000\u0000\u0000\u06a4\u06a5\u0005"+
		"\u010d\u0000\u0000\u06a5\u06a6\u0003\u00b0X\u0000\u06a6\u00df\u0001\u0000"+
		"\u0000\u0000\u06a7\u06ad\u0005\u00de\u0000\u0000\u06a8\u06a9\u0005\u0104"+
		"\u0000\u0000\u06a9\u06ad\u0005\u012e\u0000\u0000\u06aa\u06ab\u0005c\u0000"+
		"\u0000\u06ab\u06ad\u0005\u012e\u0000\u0000\u06ac\u06a7\u0001\u0000\u0000"+
		"\u0000\u06ac\u06a8\u0001\u0000\u0000\u0000\u06ac\u06aa\u0001\u0000\u0000"+
		"\u0000\u06ad\u06ae\u0001\u0000\u0000\u0000\u06ae\u06c9\u0003\u00c0`\u0000"+
		"\u06af\u06b1\u0005\u008b\u0000\u0000\u06b0\u06b2\u0005\u00b3\u0000\u0000"+
		"\u06b1\u06b0\u0001\u0000\u0000\u0000\u06b1\u06b2\u0001\u0000\u0000\u0000"+
		"\u06b2\u06b3\u0001\u0000\u0000\u0000\u06b3\u06c9\u0005\u00b6\u0000\u0000"+
		"\u06b4\u06b6\u0005\u008b\u0000\u0000\u06b5\u06b7\u0005\u00b3\u0000\u0000"+
		"\u06b6\u06b5\u0001\u0000\u0000\u0000\u06b6\u06b7\u0001\u0000\u0000\u0000"+
		"\u06b7\u06b8\u0001\u0000\u0000\u0000\u06b8\u06bb\u0005\u011b\u0000\u0000"+
		"\u06b9\u06bb\u00050\u0000\u0000\u06ba\u06b4\u0001\u0000\u0000\u0000\u06ba"+
		"\u06b9\u0001\u0000\u0000\u0000\u06bb\u06bc\u0001\u0000\u0000\u0000\u06bc"+
		"\u06c9\u0003\u0118\u008c\u0000\u06bd\u06bf\u0005\u008b\u0000\u0000\u06be"+
		"\u06c0\u0005\u00b3\u0000\u0000\u06bf\u06be\u0001\u0000\u0000\u0000\u06bf"+
		"\u06c0\u0001\u0000\u0000\u0000\u06c0\u06c2\u0001\u0000\u0000\u0000\u06c1"+
		"\u06c3\u0003\u00be_\u0000\u06c2\u06c1\u0001\u0000\u0000\u0000\u06c2\u06c3"+
		"\u0001\u0000\u0000\u0000\u06c3\u06c4\u0001\u0000\u0000\u0000\u06c4\u06c9"+
		"\u0005\u00b2\u0000\u0000\u06c5\u06c6\u0007\u0011\u0000\u0000\u06c6\u06c9"+
		"\u0003\u00ba]\u0000\u06c7\u06c9\u0003\u00b0X\u0000\u06c8\u06ac\u0001\u0000"+
		"\u0000\u0000\u06c8\u06af\u0001\u0000\u0000\u0000\u06c8\u06ba\u0001\u0000"+
		"\u0000\u0000\u06c8\u06bd\u0001\u0000\u0000\u0000\u06c8\u06c5\u0001\u0000"+
		"\u0000\u0000\u06c8\u06c7\u0001\u0000\u0000\u0000\u06c9\u00e1\u0001\u0000"+
		"\u0000\u0000\u06ca\u06cb\u0005\u0092\u0000\u0000\u06cb\u06cc\u0003\u0114"+
		"\u008a\u0000\u06cc\u06cd\u0005\u0083\u0000\u0000\u06cd\u06d8\u0003\u00b0"+
		"X\u0000\u06ce\u06cf\u0005\u012d\u0000\u0000\u06cf\u06d1\u0003\u00b0X\u0000"+
		"\u06d0\u06ce\u0001\u0000\u0000\u0000\u06d0\u06d1\u0001\u0000\u0000\u0000"+
		"\u06d1\u06d2\u0001\u0000\u0000\u0000\u06d2\u06d3\u0005\u001e\u0000\u0000"+
		"\u06d3\u06d9\u0003\u00b0X\u0000\u06d4\u06d5\u0005\u012d\u0000\u0000\u06d5"+
		"\u06d7\u0003\u00b0X\u0000\u06d6\u06d4\u0001\u0000\u0000\u0000\u06d6\u06d7"+
		"\u0001\u0000\u0000\u0000\u06d7\u06d9\u0001\u0000\u0000\u0000\u06d8\u06d0"+
		"\u0001\u0000\u0000\u0000\u06d8\u06d6\u0001\u0000\u0000\u0000\u06d9\u06da"+
		"\u0001\u0000\u0000\u0000\u06da\u06db\u0005\u00d8\u0000\u0000\u06db\u00e3"+
		"\u0001\u0000\u0000\u0000\u06dc\u06e0\u0005\u0092\u0000\u0000\u06dd\u06de"+
		"\u0003\u0114\u008a\u0000\u06de\u06df\u0005d\u0000\u0000\u06df\u06e1\u0001"+
		"\u0000\u0000\u0000\u06e0\u06dd\u0001\u0000\u0000\u0000\u06e0\u06e1\u0001"+
		"\u0000\u0000\u0000\u06e1\u06e2\u0001\u0000\u0000\u0000\u06e2\u06e5\u0003"+
		"t:\u0000\u06e3\u06e4\u0005\u012d\u0000\u0000\u06e4\u06e6\u0003\u00b0X"+
		"\u0000\u06e5\u06e3\u0001\u0000\u0000\u0000\u06e5\u06e6\u0001\u0000\u0000"+
		"\u0000\u06e6\u06e7\u0001\u0000\u0000\u0000\u06e7\u06e8\u0005\u001e\u0000"+
		"\u0000\u06e8\u06e9\u0003\u00b0X\u0000\u06e9\u06ea\u0005\u00d8\u0000\u0000"+
		"\u06ea\u00e5\u0001\u0000\u0000\u0000\u06eb\u06ec\u0005\u00dc\u0000\u0000"+
		"\u06ec\u06ed\u0005\u009b\u0000\u0000\u06ed\u06ee\u0003\u0114\u008a\u0000"+
		"\u06ee\u06ef\u0005d\u0000\u0000\u06ef\u06f0\u0003\u00b0X\u0000\u06f0\u06f1"+
		"\u00051\u0000\u0000\u06f1\u06f2\u0003\u0114\u008a\u0000\u06f2\u06f3\u0005"+
		"\u0083\u0000\u0000\u06f3\u06f4\u0003\u00b0X\u0000\u06f4\u06f5\u0005\u001e"+
		"\u0000\u0000\u06f5\u06f6\u0003\u00b0X\u0000\u06f6\u06f7\u0005\u00ef\u0000"+
		"\u0000\u06f7\u00e7\u0001\u0000\u0000\u0000\u06f8\u06f9\u0007\u0017\u0000"+
		"\u0000\u06f9\u06fa\u0005\u009b\u0000\u0000\u06fa\u06fb\u0003\u0114\u008a"+
		"\u0000\u06fb\u06fc\u0005\u0083\u0000\u0000\u06fc\u06ff\u0003\u00b0X\u0000"+
		"\u06fd\u06fe\u0005\u012d\u0000\u0000\u06fe\u0700\u0003\u00b0X\u0000\u06ff"+
		"\u06fd\u0001\u0000\u0000\u0000\u06ff\u0700\u0001\u0000\u0000\u0000\u0700"+
		"\u0701\u0001\u0000\u0000\u0000\u0701\u0702\u0005\u00ef\u0000\u0000\u0702"+
		"\u00e9\u0001\u0000\u0000\u0000\u0703\u0704\u0005\u00b1\u0000\u0000\u0704"+
		"\u0705\u0005\u009b\u0000\u0000\u0705\u0708\u0003\u00b0X\u0000\u0706\u0707"+
		"\u00051\u0000\u0000\u0707\u0709\u0003\u00be_\u0000\u0708\u0706\u0001\u0000"+
		"\u0000\u0000\u0708\u0709\u0001\u0000\u0000\u0000\u0709\u070a\u0001\u0000"+
		"\u0000\u0000\u070a\u070b\u0005\u00ef\u0000\u0000\u070b\u00eb\u0001\u0000"+
		"\u0000\u0000\u070c\u070d\u0005\u0118\u0000\u0000\u070d\u0715\u0005\u009b"+
		"\u0000\u0000\u070e\u0710\u0007\u0018\u0000\u0000\u070f\u070e\u0001\u0000"+
		"\u0000\u0000\u070f\u0710\u0001\u0000\u0000\u0000\u0710\u0712\u0001\u0000"+
		"\u0000\u0000\u0711\u0713\u0003\u00b0X\u0000\u0712\u0711\u0001\u0000\u0000"+
		"\u0000\u0712\u0713\u0001\u0000\u0000\u0000\u0713\u0714\u0001\u0000\u0000"+
		"\u0000\u0714\u0716\u0005r\u0000\u0000\u0715\u070f\u0001\u0000\u0000\u0000"+
		"\u0715\u0716\u0001\u0000\u0000\u0000\u0716\u0717\u0001\u0000\u0000\u0000"+
		"\u0717\u0718\u0003\u00b0X\u0000\u0718\u0719\u0005\u00ef\u0000\u0000\u0719"+
		"\u00ed\u0001\u0000\u0000\u0000\u071a\u071b\u0003t:\u0000\u071b\u00ef\u0001"+
		"\u0000\u0000\u0000\u071c\u071d\u0003j5\u0000\u071d\u00f1\u0001\u0000\u0000"+
		"\u0000\u071e\u071f\u0005\u009b\u0000\u0000\u071f\u0720\u0003\u00b0X\u0000"+
		"\u0720\u0721\u0005\u00ef\u0000\u0000\u0721\u00f3\u0001\u0000\u0000\u0000"+
		"\u0722\u0723\u0003\u0114\u008a\u0000\u0723\u072c\u0005\u0093\u0000\u0000"+
		"\u0724\u0729\u0003\u00f6{\u0000\u0725\u0726\u00051\u0000\u0000\u0726\u0728"+
		"\u0003\u00f6{\u0000\u0727\u0725\u0001\u0000\u0000\u0000\u0728\u072b\u0001"+
		"\u0000\u0000\u0000\u0729\u0727\u0001\u0000\u0000\u0000\u0729\u072a\u0001"+
		"\u0000\u0000\u0000\u072a\u072d\u0001\u0000\u0000\u0000\u072b\u0729\u0001"+
		"\u0000\u0000\u0000\u072c\u0724\u0001\u0000\u0000\u0000\u072c\u072d\u0001"+
		"\u0000\u0000\u0000\u072d\u072e\u0001\u0000\u0000\u0000\u072e\u072f\u0005"+
		"\u00d9\u0000\u0000\u072f\u00f5\u0001\u0000\u0000\u0000\u0730\u0731\u0003"+
		"\u0106\u0083\u0000\u0731\u0732\u0005/\u0000\u0000\u0732\u0733\u0003\u00b0"+
		"X\u0000\u0733\u0739\u0001\u0000\u0000\u0000\u0734\u0739\u0003\u00ccf\u0000"+
		"\u0735\u0739\u0003\u0114\u008a\u0000\u0736\u0737\u0005S\u0000\u0000\u0737"+
		"\u0739\u0005\u010f\u0000\u0000\u0738\u0730\u0001\u0000\u0000\u0000\u0738"+
		"\u0734\u0001\u0000\u0000\u0000\u0738\u0735\u0001\u0000\u0000\u0000\u0738"+
		"\u0736\u0001\u0000\u0000\u0000\u0739\u00f7\u0001\u0000\u0000\u0000\u073a"+
		"\u073b\u0005<\u0000\u0000\u073b\u073c\u0005\u009b\u0000\u0000\u073c\u073d"+
		"\u0005\u010f\u0000\u0000\u073d\u073e\u0005\u00ef\u0000\u0000\u073e\u00f9"+
		"\u0001\u0000\u0000\u0000\u073f\u0740\u0005i\u0000\u0000\u0740\u0749\u0005"+
		"\u0093\u0000\u0000\u0741\u074a\u0003\u0006\u0003\u0000\u0742\u0744\u0003"+
		":\u001d\u0000\u0743\u0742\u0001\u0000\u0000\u0000\u0743\u0744\u0001\u0000"+
		"\u0000\u0000\u0744\u0745\u0001\u0000\u0000\u0000\u0745\u0747\u0003^/\u0000"+
		"\u0746\u0748\u0003&\u0013\u0000\u0747\u0746\u0001\u0000\u0000\u0000\u0747"+
		"\u0748\u0001\u0000\u0000\u0000\u0748\u074a\u0001\u0000\u0000\u0000\u0749"+
		"\u0741\u0001\u0000\u0000\u0000\u0749\u0743\u0001\u0000\u0000\u0000\u074a"+
		"\u074b\u0001\u0000\u0000\u0000\u074b\u074c\u0005\u00d9\u0000\u0000\u074c"+
		"\u00fb\u0001\u0000\u0000\u0000\u074d\u074e\u0005<\u0000\u0000\u074e\u0757"+
		"\u0005\u0093\u0000\u0000\u074f\u0758\u0003\u0006\u0003\u0000\u0750\u0752"+
		"\u0003:\u001d\u0000\u0751\u0750\u0001\u0000\u0000\u0000\u0751\u0752\u0001"+
		"\u0000\u0000\u0000\u0752\u0753\u0001\u0000\u0000\u0000\u0753\u0755\u0003"+
		"^/\u0000\u0754\u0756\u0003&\u0013\u0000\u0755\u0754\u0001\u0000\u0000"+
		"\u0000\u0755\u0756\u0001\u0000\u0000\u0000\u0756\u0758\u0001\u0000\u0000"+
		"\u0000\u0757\u074f\u0001\u0000\u0000\u0000\u0757\u0751\u0001\u0000\u0000"+
		"\u0000\u0758\u0759\u0001\u0000\u0000\u0000\u0759\u075a\u0005\u00d9\u0000"+
		"\u0000\u075a\u00fd\u0001\u0000\u0000\u0000\u075b\u075c\u0005.\u0000\u0000"+
		"\u075c\u075d\u0005\u0093\u0000\u0000\u075d\u075e\u0003\u0006\u0003\u0000"+
		"\u075e\u075f\u0005\u00d9\u0000\u0000\u075f\u00ff\u0001\u0000\u0000\u0000"+
		"\u0760\u0762\u0005\u00a1\u0000\u0000\u0761\u0760\u0001\u0000\u0000\u0000"+
		"\u0761\u0762\u0001\u0000\u0000\u0000\u0762\u0763\u0001\u0000\u0000\u0000"+
		"\u0763\u0764\u0007\u0019\u0000\u0000\u0764\u0101\u0001\u0000\u0000\u0000"+
		"\u0765\u0767\u0005\u00a1\u0000\u0000\u0766\u0765\u0001\u0000\u0000\u0000"+
		"\u0766\u0767\u0001\u0000\u0000\u0000\u0767\u0768\u0001\u0000\u0000\u0000"+
		"\u0768\u0769\u0005\u0005\u0000\u0000\u0769\u0103\u0001\u0000\u0000\u0000"+
		"\u076a\u0773\u0005\u0092\u0000\u0000\u076b\u0770\u0003\u00b0X\u0000\u076c"+
		"\u076d\u00051\u0000\u0000\u076d\u076f\u0003\u00b0X\u0000\u076e\u076c\u0001"+
		"\u0000\u0000\u0000\u076f\u0772\u0001\u0000\u0000\u0000\u0770\u076e\u0001"+
		"\u0000\u0000\u0000\u0770\u0771\u0001\u0000\u0000\u0000\u0771\u0774\u0001"+
		"\u0000\u0000\u0000\u0772\u0770\u0001\u0000\u0000\u0000\u0773\u076b\u0001"+
		"\u0000\u0000\u0000\u0773\u0774\u0001\u0000\u0000\u0000\u0774\u0775\u0001"+
		"\u0000\u0000\u0000\u0775\u0776\u0005\u00d8\u0000\u0000\u0776\u0105\u0001"+
		"\u0000\u0000\u0000\u0777\u0778\u0003\u0296\u014b\u0000\u0778\u0107\u0001"+
		"\u0000\u0000\u0000\u0779\u077a\u0005P\u0000\u0000\u077a\u077b\u0003\u010a"+
		"\u0085\u0000\u077b\u0109\u0001\u0000\u0000\u0000\u077c\u077f\u0003\u0296"+
		"\u014b\u0000\u077d\u077f\u0005\u0005\u0000\u0000\u077e\u077c\u0001\u0000"+
		"\u0000\u0000\u077e\u077d\u0001\u0000\u0000\u0000\u077f\u010b\u0001\u0000"+
		"\u0000\u0000\u0780\u0781\u0003\u0110\u0088\u0000\u0781\u0783\u0005\u009b"+
		"\u0000\u0000\u0782\u0784\u0007\u0000\u0000\u0000\u0783\u0782\u0001\u0000"+
		"\u0000\u0000\u0783\u0784\u0001\u0000\u0000\u0000\u0784\u078d\u0001\u0000"+
		"\u0000\u0000\u0785\u078a\u0003\u010e\u0087\u0000\u0786\u0787\u00051\u0000"+
		"\u0000\u0787\u0789\u0003\u010e\u0087\u0000\u0788\u0786\u0001\u0000\u0000"+
		"\u0000\u0789\u078c\u0001\u0000\u0000\u0000\u078a\u0788\u0001\u0000\u0000"+
		"\u0000\u078a\u078b\u0001\u0000\u0000\u0000\u078b\u078e\u0001\u0000\u0000"+
		"\u0000\u078c\u078a\u0001\u0000\u0000\u0000\u078d\u0785\u0001\u0000\u0000"+
		"\u0000\u078d\u078e\u0001\u0000\u0000\u0000\u078e\u078f\u0001\u0000\u0000"+
		"\u0000\u078f\u0790\u0005\u00ef\u0000\u0000\u0790\u010d\u0001\u0000\u0000"+
		"\u0000\u0791\u0792\u0003\u00b0X\u0000\u0792\u010f\u0001\u0000\u0000\u0000"+
		"\u0793\u0794\u0003\u0112\u0089\u0000\u0794\u0795\u0003\u0296\u014b\u0000"+
		"\u0795\u0111\u0001\u0000\u0000\u0000\u0796\u0797\u0003\u0296\u014b\u0000"+
		"\u0797\u0798\u0005S\u0000\u0000\u0798\u079a\u0001\u0000\u0000\u0000\u0799"+
		"\u0796\u0001\u0000\u0000\u0000\u079a\u079d\u0001\u0000\u0000\u0000\u079b"+
		"\u0799\u0001\u0000\u0000\u0000\u079b\u079c\u0001\u0000\u0000\u0000\u079c"+
		"\u0113\u0001\u0000\u0000\u0000\u079d\u079b\u0001\u0000\u0000\u0000\u079e"+
		"\u079f\u0003\u0290\u0148\u0000\u079f\u0115\u0001\u0000\u0000\u0000\u07a0"+
		"\u07a5\u0003\u0296\u014b\u0000\u07a1\u07a2\u00051\u0000\u0000\u07a2\u07a4"+
		"\u0003\u0296\u014b\u0000\u07a3\u07a1\u0001\u0000\u0000\u0000\u07a4\u07a7"+
		"\u0001\u0000\u0000\u0000\u07a5\u07a3\u0001\u0000\u0000\u0000\u07a5\u07a6"+
		"\u0001\u0000\u0000\u0000\u07a6\u0117\u0001\u0000\u0000\u0000\u07a7\u07a5"+
		"\u0001\u0000\u0000\u0000\u07a8\u07ad\u0003\u011a\u008d\u0000\u07a9\u07aa"+
		"\u0005\u001e\u0000\u0000\u07aa\u07ac\u0003\u011a\u008d\u0000\u07ab\u07a9"+
		"\u0001\u0000\u0000\u0000\u07ac\u07af\u0001\u0000\u0000\u0000\u07ad\u07ab"+
		"\u0001\u0000\u0000\u0000\u07ad\u07ae\u0001\u0000\u0000\u0000\u07ae\u0119"+
		"\u0001\u0000\u0000\u0000\u07af\u07ad\u0001\u0000\u0000\u0000\u07b0\u07b2"+
		"\u0003\u011c\u008e\u0000\u07b1\u07b3\u0003\u011e\u008f\u0000\u07b2\u07b1"+
		"\u0001\u0000\u0000\u0000\u07b2\u07b3\u0001\u0000\u0000\u0000\u07b3\u07b7"+
		"\u0001\u0000\u0000\u0000\u07b4\u07b6\u0003\u0120\u0090\u0000\u07b5\u07b4"+
		"\u0001\u0000\u0000\u0000\u07b6\u07b9\u0001\u0000\u0000\u0000\u07b7\u07b5"+
		"\u0001\u0000\u0000\u0000\u07b7\u07b8\u0001\u0000\u0000\u0000\u07b8\u011b"+
		"\u0001\u0000\u0000\u0000\u07b9\u07b7\u0001\u0000\u0000\u0000\u07ba\u07fc"+
		"\u0005\u00b4\u0000\u0000\u07bb\u07fc\u0005\u00b6\u0000\u0000\u07bc\u07fc"+
		"\u0005 \u0000\u0000\u07bd\u07fc\u0005!\u0000\u0000\u07be\u07fc\u0005\u0127"+
		"\u0000\u0000\u07bf\u07fc\u0005\u0107\u0000\u0000\u07c0\u07fc\u0005\u0089"+
		"\u0000\u0000\u07c1\u07c3\u0005\u0100\u0000\u0000\u07c2\u07c1\u0001\u0000"+
		"\u0000\u0000\u07c2\u07c3\u0001\u0000\u0000\u0000\u07c3\u07c4\u0001\u0000"+
		"\u0000\u0000\u07c4\u07fc\u0005\u008a\u0000\u0000\u07c5\u07fc\u0005o\u0000"+
		"\u0000\u07c6\u07fc\u0005C\u0000\u0000\u07c7\u07c8\u0005\u0099\u0000\u0000"+
		"\u07c8\u07fc\u0007\u001a\u0000\u0000\u07c9\u07ca\u0005\u0134\u0000\u0000"+
		"\u07ca\u07fc\u0007\u001a\u0000\u0000\u07cb\u07cc\u0005\u010e\u0000\u0000"+
		"\u07cc\u07d0\u0007\u001b\u0000\u0000\u07cd\u07d1\u0005\u0111\u0000\u0000"+
		"\u07ce\u07cf\u0005\u010e\u0000\u0000\u07cf\u07d1\u0005\u0133\u0000\u0000"+
		"\u07d0\u07cd\u0001\u0000\u0000\u0000\u07d0\u07ce\u0001\u0000\u0000\u0000"+
		"\u07d1\u07fc\u0001\u0000\u0000\u0000\u07d2\u07d3\u0005\u0110\u0000\u0000"+
		"\u07d3\u07d7\u0007\u001b\u0000\u0000\u07d4\u07d8\u0005\u0111\u0000\u0000"+
		"\u07d5\u07d6\u0005\u010e\u0000\u0000\u07d6\u07d8\u0005\u0133\u0000\u0000"+
		"\u07d7\u07d4\u0001\u0000\u0000\u0000\u07d7\u07d5\u0001\u0000\u0000\u0000"+
		"\u07d8\u07fc\u0001\u0000\u0000\u0000\u07d9\u07fc\u0005Z\u0000\u0000\u07da"+
		"\u07fc\u0005\u00c9\u0000\u0000\u07db\u07fc\u0005\u00ad\u0000\u0000\u07dc"+
		"\u07fc\u0005\u012a\u0000\u0000\u07dd\u07fc\u0005\u00e0\u0000\u0000\u07de"+
		"\u07fc\u0005\\\u0000\u0000\u07df\u07fc\u0005\u009e\u0000\u0000\u07e0\u07e1"+
		"\u0007\u001c\u0000\u0000\u07e1\u07e2\u0005\u009c\u0000\u0000\u07e2\u07e3"+
		"\u0003\u0118\u008c\u0000\u07e3\u07e4\u0005|\u0000\u0000\u07e4\u07fc\u0001"+
		"\u0000\u0000\u0000\u07e5\u07fc\u0005\u00c3\u0000\u0000\u07e6\u07fc\u0005"+
		"\u00c4\u0000\u0000\u07e7\u07e8\u0005\u00d3\u0000\u0000\u07e8\u07fc\u0005"+
		"\u0126\u0000\u0000\u07e9\u07f9\u0005\u0015\u0000\u0000\u07ea\u07fa\u0005"+
		"\u00ad\u0000\u0000\u07eb\u07fa\u0005\u012a\u0000\u0000\u07ec\u07fa\u0005"+
		"\u00e0\u0000\u0000\u07ed\u07fa\u0005\\\u0000\u0000\u07ee\u07fa\u0005\u009e"+
		"\u0000\u0000\u07ef\u07f0\u0005\u00d3\u0000\u0000\u07f0\u07fa\u0005\u0126"+
		"\u0000\u0000\u07f1\u07f3\u0005\u0126\u0000\u0000\u07f2\u07f1\u0001\u0000"+
		"\u0000\u0000\u07f2\u07f3\u0001\u0000\u0000\u0000\u07f3\u07f4\u0001\u0000"+
		"\u0000\u0000\u07f4\u07f5\u0005\u009c\u0000\u0000\u07f5\u07f6\u0003\u0118"+
		"\u008c\u0000\u07f6\u07f7\u0005|\u0000\u0000\u07f7\u07fa\u0001\u0000\u0000"+
		"\u0000\u07f8\u07fa\u0005\u0126\u0000\u0000\u07f9\u07ea\u0001\u0000\u0000"+
		"\u0000\u07f9\u07eb\u0001\u0000\u0000\u0000\u07f9\u07ec\u0001\u0000\u0000"+
		"\u0000\u07f9\u07ed\u0001\u0000\u0000\u0000\u07f9\u07ee\u0001\u0000\u0000"+
		"\u0000\u07f9\u07ef\u0001\u0000\u0000\u0000\u07f9\u07f2\u0001\u0000\u0000"+
		"\u0000\u07f9\u07f8\u0001\u0000\u0000\u0000\u07f9\u07fa\u0001\u0000\u0000"+
		"\u0000\u07fa\u07fc\u0001\u0000\u0000\u0000\u07fb\u07ba\u0001\u0000\u0000"+
		"\u0000\u07fb\u07bb\u0001\u0000\u0000\u0000\u07fb\u07bc\u0001\u0000\u0000"+
		"\u0000\u07fb\u07bd\u0001\u0000\u0000\u0000\u07fb\u07be\u0001\u0000\u0000"+
		"\u0000\u07fb\u07bf\u0001\u0000\u0000\u0000\u07fb\u07c0\u0001\u0000\u0000"+
		"\u0000\u07fb\u07c2\u0001\u0000\u0000\u0000\u07fb\u07c5\u0001\u0000\u0000"+
		"\u0000\u07fb\u07c6\u0001\u0000\u0000\u0000\u07fb\u07c7\u0001\u0000\u0000"+
		"\u0000\u07fb\u07c9\u0001\u0000\u0000\u0000\u07fb\u07cb\u0001\u0000\u0000"+
		"\u0000\u07fb\u07d2\u0001\u0000\u0000\u0000\u07fb\u07d9\u0001\u0000\u0000"+
		"\u0000\u07fb\u07da\u0001\u0000\u0000\u0000\u07fb\u07db\u0001\u0000\u0000"+
		"\u0000\u07fb\u07dc\u0001\u0000\u0000\u0000\u07fb\u07dd\u0001\u0000\u0000"+
		"\u0000\u07fb\u07de\u0001\u0000\u0000\u0000\u07fb\u07df\u0001\u0000\u0000"+
		"\u0000\u07fb\u07e0\u0001\u0000\u0000\u0000\u07fb\u07e5\u0001\u0000\u0000"+
		"\u0000\u07fb\u07e6\u0001\u0000\u0000\u0000\u07fb\u07e7\u0001\u0000\u0000"+
		"\u0000\u07fb\u07e9\u0001\u0000\u0000\u0000\u07fc\u011d\u0001\u0000\u0000"+
		"\u0000\u07fd\u07fe\u0005\u00b3\u0000\u0000\u07fe\u0801\u0005\u00b6\u0000"+
		"\u0000\u07ff\u0801\u0005\u0091\u0000\u0000\u0800\u07fd\u0001\u0000\u0000"+
		"\u0000\u0800\u07ff\u0001\u0000\u0000\u0000\u0801\u011f\u0001\u0000\u0000"+
		"\u0000\u0802\u0804\u0007\u001c\u0000\u0000\u0803\u0805\u0003\u011e\u008f"+
		"\u0000\u0804\u0803\u0001\u0000\u0000\u0000\u0804\u0805\u0001\u0000\u0000"+
		"\u0000\u0805\u0121\u0001\u0000\u0000\u0000\u0806\u0808\u0003\f\u0006\u0000"+
		"\u0807\u0806\u0001\u0000\u0000\u0000\u0807\u0808\u0001\u0000\u0000\u0000"+
		"\u0808\u0816\u0001\u0000\u0000\u0000\u0809\u0817\u0003\u0124\u0092\u0000"+
		"\u080a\u0817\u0003\u0126\u0093\u0000\u080b\u0817\u0003\u0128\u0094\u0000"+
		"\u080c\u0817\u0003\u0136\u009b\u0000\u080d\u0817\u0003\u018a\u00c5\u0000"+
		"\u080e\u0817\u0003\u018c\u00c6\u0000\u080f\u0817\u0003\u0190\u00c8\u0000"+
		"\u0810\u0817\u0003\u0192\u00c9\u0000\u0811\u0817\u0003\u018e\u00c7\u0000"+
		"\u0812\u0817\u0003\u0254\u012a\u0000\u0813\u0817\u0003\u0256\u012b\u0000"+
		"\u0814\u0817\u0003\u019a\u00cd\u0000\u0815\u0817\u0003\u01a4\u00d2\u0000"+
		"\u0816\u0809\u0001\u0000\u0000\u0000\u0816\u080a\u0001\u0000\u0000\u0000"+
		"\u0816\u080b\u0001\u0000\u0000\u0000\u0816\u080c\u0001\u0000\u0000\u0000"+
		"\u0816\u080d\u0001\u0000\u0000\u0000\u0816\u080e\u0001\u0000\u0000\u0000"+
		"\u0816\u080f\u0001\u0000\u0000\u0000\u0816\u0810\u0001\u0000\u0000\u0000"+
		"\u0816\u0811\u0001\u0000\u0000\u0000\u0816\u0812\u0001\u0000\u0000\u0000"+
		"\u0816\u0813\u0001\u0000\u0000\u0000\u0816\u0814\u0001\u0000\u0000\u0000"+
		"\u0816\u0815\u0001\u0000\u0000\u0000\u0817\u0123\u0001\u0000\u0000\u0000"+
		"\u0818\u0819\u0005=\u0000\u0000\u0819\u0125\u0001\u0000\u0000\u0000\u081a"+
		"\u081b\u0005W\u0000\u0000\u081b\u0127\u0001\u0000\u0000\u0000\u081c\u081d"+
		"\u0005\u00ff\u0000\u0000\u081d\u0129\u0001\u0000\u0000\u0000\u081e\u081f"+
		"\u0005\u0132\u0000\u0000\u081f\u012b\u0001\u0000\u0000\u0000\u0820\u0821"+
		"\u0003\u0114\u008a\u0000\u0821\u012d\u0001\u0000\u0000\u0000\u0822\u0823"+
		"\u0005\u00b8\u0000\u0000\u0823\u0824\u0003\u0102\u0081\u0000\u0824\u012f"+
		"\u0001\u0000\u0000\u0000\u0825\u0826\u0005\u0096\u0000\u0000\u0826\u0827"+
		"\u0003\u0102\u0081\u0000\u0827\u0131\u0001\u0000\u0000\u0000\u0828\u0829"+
		"\u0005\u0132\u0000\u0000\u0829\u0133\u0001\u0000\u0000\u0000\u082a\u082b"+
		"\u0005\u00bc\u0000\u0000\u082b\u082c\u0003\u028c\u0146\u0000\u082c\u0135"+
		"\u0001\u0000\u0000\u0000\u082d\u082e\u0005\u010b\u0000\u0000\u082e\u082f"+
		"\u0003\u015e\u00af\u0000\u082f\u0137\u0001\u0000\u0000\u0000\u0830\u0833"+
		"\u0003\u0136\u009b\u0000\u0831\u0833\u0003\u013a\u009d\u0000\u0832\u0830"+
		"\u0001\u0000\u0000\u0000\u0832\u0831\u0001\u0000\u0000\u0000\u0833\u0139"+
		"\u0001\u0000\u0000\u0000\u0834\u0835\u0005\u00ff\u0000\u0000\u0835\u013b"+
		"\u0001\u0000\u0000\u0000\u0836\u083c\u0005%\u0000\u0000\u0837\u083c\u0005"+
		"\u0129\u0000\u0000\u0838\u083c\u0005\u0132\u0000\u0000\u0839\u083a\u0005"+
		"\u012d\u0000\u0000\u083a\u083c\u0003\u00b0X\u0000\u083b\u0836\u0001\u0000"+
		"\u0000\u0000\u083b\u0837\u0001\u0000\u0000\u0000\u083b\u0838\u0001\u0000"+
		"\u0000\u0000\u083b\u0839\u0001\u0000\u0000\u0000\u083c\u013d\u0001\u0000"+
		"\u0000\u0000\u083d\u083e\u0003\u020e\u0107\u0000\u083e\u013f\u0001\u0000"+
		"\u0000\u0000\u083f\u0840\u0003\u020e\u0107\u0000\u0840\u0141\u0001\u0000"+
		"\u0000\u0000\u0841\u0842\u0003\u020e\u0107\u0000\u0842\u0143\u0001\u0000"+
		"\u0000\u0000\u0843\u0844\u0003\u0210\u0108\u0000\u0844\u0145\u0001\u0000"+
		"\u0000\u0000\u0845\u0846\u0005\u011f\u0000\u0000\u0846\u0147\u0001\u0000"+
		"\u0000\u0000\u0847\u0848\u0005h\u0000\u0000\u0848\u0149\u0001\u0000\u0000"+
		"\u0000\u0849\u084a\u0005\u0012\u0000\u0000\u084a\u014b\u0001\u0000\u0000"+
		"\u0000\u084b\u084c\u0003\u0210\u0108\u0000\u084c\u014d\u0001\u0000\u0000"+
		"\u0000\u084d\u084e\u0003\u0210\u0108\u0000\u084e\u014f\u0001\u0000\u0000"+
		"\u0000\u084f\u0850\u0003\u0210\u0108\u0000\u0850\u0151\u0001\u0000\u0000"+
		"\u0000\u0851\u0852\u0003\u020c\u0106\u0000\u0852\u0153\u0001\u0000\u0000"+
		"\u0000\u0853\u0854\u0003\u0156\u00ab\u0000\u0854\u0155\u0001\u0000\u0000"+
		"\u0000\u0855\u0856\u0007\u001d\u0000\u0000\u0856\u0157\u0001\u0000\u0000"+
		"\u0000\u0857\u0858\u0005e\u0000\u0000\u0858\u0159\u0001\u0000\u0000\u0000"+
		"\u0859\u085a\u0005\u0012\u0000\u0000\u085a\u015b\u0001\u0000\u0000\u0000"+
		"\u085b\u085c\u0003\u0212\u0109\u0000\u085c\u015d\u0001\u0000\u0000\u0000"+
		"\u085d\u085f\u0003\u0212\u0109\u0000\u085e\u0860\u0003\u0166\u00b3\u0000"+
		"\u085f\u085e\u0001\u0000\u0000\u0000\u085f\u0860\u0001\u0000\u0000\u0000"+
		"\u0860\u015f\u0001\u0000\u0000\u0000\u0861\u0862\u0003\u0162\u00b1\u0000"+
		"\u0862\u0161\u0001\u0000\u0000\u0000\u0863\u0864\u0007\u001e\u0000\u0000"+
		"\u0864\u0163\u0001\u0000\u0000\u0000\u0865\u0867\u0003\u0166\u00b3\u0000"+
		"\u0866\u0865\u0001\u0000\u0000\u0000\u0866\u0867\u0001\u0000\u0000\u0000"+
		"\u0867\u0165\u0001\u0000\u0000\u0000\u0868\u086b\u0003\u0282\u0141\u0000"+
		"\u0869\u086b\u0003\u00b0X\u0000\u086a\u0868\u0001\u0000\u0000\u0000\u086a"+
		"\u0869\u0001\u0000\u0000\u0000\u086b\u0167\u0001\u0000\u0000\u0000\u086c"+
		"\u086e\u0005\u009b\u0000\u0000\u086d\u086f\u0003\u0114\u008a\u0000\u086e"+
		"\u086d\u0001\u0000\u0000\u0000\u086e\u086f\u0001\u0000\u0000\u0000\u086f"+
		"\u0870\u0001\u0000\u0000\u0000\u0870\u0871\u0005\u00ef\u0000\u0000\u0871"+
		"\u0169\u0001\u0000\u0000\u0000\u0872\u0873\u0005\u009b\u0000\u0000\u0873"+
		"\u0874\u0005\u00ef\u0000\u0000\u0874\u016b\u0001\u0000\u0000\u0000\u0875"+
		"\u0876\u00057\u0000\u0000\u0876\u016d\u0001\u0000\u0000\u0000\u0877\u087d"+
		"\u0005\u00e6\u0000\u0000\u0878\u087d\u0005\u00e6\u0000\u0000\u0879\u087d"+
		"\u0005\u00e6\u0000\u0000\u087a\u087d\u0005\u00e6\u0000\u0000\u087b\u087d"+
		"\u0005\u00e6\u0000\u0000\u087c\u0877\u0001\u0000\u0000\u0000\u087c\u0878"+
		"\u0001\u0000\u0000\u0000\u087c\u0879\u0001\u0000\u0000\u0000\u087c\u087a"+
		"\u0001\u0000\u0000\u0000\u087c\u087b\u0001\u0000\u0000\u0000\u087d\u016f"+
		"\u0001\u0000\u0000\u0000\u087e\u087f\u00057\u0000\u0000\u087f\u0171\u0001"+
		"\u0000\u0000\u0000\u0880\u0881\u0005\u0084\u0000\u0000\u0881\u0173\u0001"+
		"\u0000\u0000\u0000\u0882\u0883\u0003\u0086C\u0000\u0883\u0884\u0005\u009b"+
		"\u0000\u0000\u0884\u0885\u0003\u0116\u008b\u0000\u0885\u0886\u0005\u00ef"+
		"\u0000\u0000\u0886\u0175\u0001\u0000\u0000\u0000\u0887\u0888\u0005\u0084"+
		"\u0000\u0000\u0888\u0177\u0001\u0000\u0000\u0000\u0889\u088a\u0005\u0084"+
		"\u0000\u0000\u088a\u0179\u0001\u0000\u0000\u0000\u088b\u088d\u0005\u009b"+
		"\u0000\u0000\u088c\u088e\u0003\u0114\u008a\u0000\u088d\u088c\u0001\u0000"+
		"\u0000\u0000\u088d\u088e\u0001\u0000\u0000\u0000\u088e\u088f\u0001\u0000"+
		"\u0000\u0000\u088f\u0890\u0005\u00ef\u0000\u0000\u0890\u017b\u0001\u0000"+
		"\u0000\u0000\u0891\u0892\u0005\u009b\u0000\u0000\u0892\u0893\u0005\u00ef"+
		"\u0000\u0000\u0893\u017d\u0001\u0000\u0000\u0000\u0894\u0895\u0005\u0084"+
		"\u0000\u0000\u0895\u017f\u0001\u0000\u0000\u0000\u0896\u0898\u0005\u009b"+
		"\u0000\u0000\u0897\u0899\u0003\u0114\u008a\u0000\u0898\u0897\u0001\u0000"+
		"\u0000\u0000\u0898\u0899\u0001\u0000\u0000\u0000\u0899\u089a\u0001\u0000"+
		"\u0000\u0000\u089a\u089b\u0005\u00ef\u0000\u0000\u089b\u0181\u0001\u0000"+
		"\u0000\u0000\u089c\u089d\u0005\u009b\u0000\u0000\u089d\u089e\u0005\u00ef"+
		"\u0000\u0000\u089e\u0183\u0001\u0000\u0000\u0000\u089f\u08a0\u0005\u0084"+
		"\u0000\u0000\u08a0\u0185\u0001\u0000\u0000\u0000\u08a1\u08a2\u0003\u0114"+
		"\u008a\u0000\u08a2\u08a3\u0003\u00ccf\u0000\u08a3\u0187\u0001\u0000\u0000"+
		"\u0000\u08a4\u08a5\u0003\u0114\u008a\u0000\u08a5\u08ac\u0003\u00ccf\u0000"+
		"\u08a6\u08a7\u00051\u0000\u0000\u08a7\u08a8\u0003\u0114\u008a\u0000\u08a8"+
		"\u08a9\u0003\u00ccf\u0000\u08a9\u08ab\u0001\u0000\u0000\u0000\u08aa\u08a6"+
		"\u0001\u0000\u0000\u0000\u08ab\u08ae\u0001\u0000\u0000\u0000\u08ac\u08aa"+
		"\u0001\u0000\u0000\u0000\u08ac\u08ad\u0001\u0000\u0000\u0000\u08ad\u0189"+
		"\u0001\u0000\u0000\u0000\u08ae\u08ac\u0001\u0000\u0000\u0000\u08af\u08b0"+
		"\u0005\u0013\u0000\u0000\u08b0\u018b\u0001\u0000\u0000\u0000\u08b1\u08b2"+
		"\u0005\u00dd\u0000\u0000\u08b2\u018d\u0001\u0000\u0000\u0000\u08b3\u08b4"+
		"\u0005w\u0000\u0000\u08b4\u018f\u0001\u0000\u0000\u0000\u08b5\u08b6\u0005"+
		"J\u0000\u0000\u08b6\u0191\u0001\u0000\u0000\u0000\u08b7\u08b8\u0005\u00ea"+
		"\u0000\u0000\u08b8\u0193\u0001\u0000\u0000\u0000\u08b9\u08ba\u0003\u0278"+
		"\u013c\u0000\u08ba\u0195\u0001\u0000\u0000\u0000\u08bb\u08bc\u0003\u0278"+
		"\u013c\u0000\u08bc\u0197\u0001\u0000\u0000\u0000\u08bd\u08be\u0007\u001f"+
		"\u0000\u0000\u08be\u0199\u0001\u0000\u0000\u0000\u08bf\u08c0\u0005]\u0000"+
		"\u0000\u08c0\u08c1\u0005\u00f8\u0000\u0000\u08c1\u08c2\u0003\u0288\u0144"+
		"\u0000\u08c2\u019b\u0001\u0000\u0000\u0000\u08c3\u08c4\u0005\u00f8\u0000"+
		"\u0000\u08c4\u08c5\u0003\u0288\u0144\u0000\u08c5\u019d\u0001\u0000\u0000"+
		"\u0000\u08c6\u08c7\u0005\u00f8\u0000\u0000\u08c7\u08c8\u0003\u0288\u0144"+
		"\u0000\u08c8\u019f\u0001\u0000\u0000\u0000\u08c9\u08ca\u0005\u00f8\u0000"+
		"\u0000\u08ca\u08cb\u0003\u0288\u0144\u0000\u08cb\u01a1\u0001\u0000\u0000"+
		"\u0000\u08cc\u08cd\u0007 \u0000\u0000\u08cd\u01a3\u0001\u0000\u0000\u0000"+
		"\u08ce\u08cf\u0007!\u0000\u0000\u08cf\u01a5\u0001\u0000\u0000\u0000\u08d0"+
		"\u08d1\u0005F\u0000\u0000\u08d1\u08d5\u0005A\u0000\u0000\u08d2\u08d3\u0005"+
		"F\u0000\u0000\u08d3\u08d5\u0005B\u0000\u0000\u08d4\u08d0\u0001\u0000\u0000"+
		"\u0000\u08d4\u08d2\u0001\u0000\u0000\u0000\u08d5\u01a7\u0001\u0000\u0000"+
		"\u0000\u08d6\u08d7\u0005\u00db\u0000\u0000\u08d7\u08db\u0005A\u0000\u0000"+
		"\u08d8\u08d9\u0005\u00db\u0000\u0000\u08d9\u08db\u0005B\u0000\u0000\u08da"+
		"\u08d6\u0001\u0000\u0000\u0000\u08da\u08d8\u0001\u0000\u0000\u0000\u08db"+
		"\u01a9\u0001\u0000\u0000\u0000\u08dc\u08dd\u0005\u00eb\u0000\u0000\u08dd"+
		"\u08de\u0003\u0276\u013b\u0000\u08de\u01ab\u0001\u0000\u0000\u0000\u08df"+
		"\u08e0\u0005\u00eb\u0000\u0000\u08e0\u08e1\u0003\u0276\u013b\u0000\u08e1"+
		"\u01ad\u0001\u0000\u0000\u0000\u08e2\u08e3\u0005\u00eb\u0000\u0000\u08e3"+
		"\u08e4\u0003\u0276\u013b\u0000\u08e4\u08e5\u0005\u0112\u0000\u0000\u08e5"+
		"\u08e6\u0003\u0276\u013b\u0000\u08e6\u01af\u0001\u0000\u0000\u0000\u08e7"+
		"\u08e8\u0003\u0198\u00cc\u0000\u08e8\u01b1\u0001\u0000\u0000\u0000\u08e9"+
		"\u08ea\u0003\u0196\u00cb\u0000\u08ea\u08eb\u0005\u0112\u0000\u0000\u08eb"+
		"\u08ec\u0003\u0194\u00ca\u0000\u08ec\u01b3\u0001\u0000\u0000\u0000\u08ed"+
		"\u08ee\u0003\u0196\u00cb\u0000\u08ee\u08ef\u0005r\u0000\u0000\u08ef\u08f0"+
		"\u0003\u0194\u00ca\u0000\u08f0\u01b5\u0001\u0000\u0000\u0000\u08f1\u08f2"+
		"\u0005\u0123\u0000\u0000\u08f2\u08f3\u0003\u0276\u013b\u0000\u08f3\u01b7"+
		"\u0001\u0000\u0000\u0000\u08f4\u08f5\u0005\u0123\u0000\u0000\u08f5\u08f6"+
		"\u0003\u0276\u013b\u0000\u08f6\u01b9\u0001\u0000\u0000\u0000\u08f7\u08f8"+
		"\u0005\u0123\u0000\u0000\u08f8\u08f9\u0003\u0276\u013b\u0000\u08f9\u08fa"+
		"\u0005\u0112\u0000\u0000\u08fa\u08fb\u0003\u0276\u013b\u0000\u08fb\u01bb"+
		"\u0001\u0000\u0000\u0000\u08fc\u08fd\u0005?\u0000\u0000\u08fd\u08fe\u0005"+
		"\u0123\u0000\u0000\u08fe\u01bd\u0001\u0000\u0000\u0000\u08ff\u0900\u0005"+
		"\u0123\u0000\u0000\u0900\u0901\u0003\u0276\u013b\u0000\u0901\u01bf\u0001"+
		"\u0000\u0000\u0000\u0902\u0903\u0005\u001d\u0000\u0000\u0903\u01c1\u0001"+
		"\u0000\u0000\u0000\u0904\u0905\u0005\u00c1\u0000\u0000\u0905\u0906\u0003"+
		"\u01c6\u00e3\u0000\u0906\u01c3\u0001\u0000\u0000\u0000\u0907\u0908\u0005"+
		"\u00c1\u0000\u0000\u0908\u0909\u0003\u01c6\u00e3\u0000\u0909\u01c5\u0001"+
		"\u0000\u0000\u0000\u090a\u090d\u0003\u0284\u0142\u0000\u090b\u090d\u0003"+
		"\u0108\u0084\u0000\u090c\u090a\u0001\u0000\u0000\u0000\u090c\u090b\u0001"+
		"\u0000\u0000\u0000\u090d\u01c7\u0001\u0000\u0000\u0000\u090e\u090f\u0005"+
		",\u0000\u0000\u090f\u0910\u0005\u00e7\u0000\u0000\u0910\u01c9\u0001\u0000"+
		"\u0000\u0000\u0911\u0912\u0005\u0105\u0000\u0000\u0912\u0916\u0005\f\u0000"+
		"\u0000\u0913\u0914\u0005\u0105\u0000\u0000\u0914\u0916\u0005\u0109\u0000"+
		"\u0000\u0915\u0911\u0001\u0000\u0000\u0000\u0915\u0913\u0001\u0000\u0000"+
		"\u0000\u0916\u01cb\u0001\u0000\u0000\u0000\u0917\u0918\u0005~\u0000\u0000"+
		"\u0918\u0919\u0005A\u0000\u0000\u0919\u091a\u0003\u027c\u013e\u0000\u091a"+
		"\u01cd\u0001\u0000\u0000\u0000\u091b\u091c\u0005\u001d\u0000\u0000\u091c"+
		"\u01cf\u0001\u0000\u0000\u0000\u091d\u091e\u0005\u007f\u0000\u0000\u091e"+
		"\u091f\u0003\u0286\u0143\u0000\u091f\u01d1\u0001\u0000\u0000\u0000\u0920"+
		"\u0921\u0007\"\u0000\u0000\u0921\u01d3\u0001\u0000\u0000\u0000\u0922\u0923"+
		"\u0005?\u0000\u0000\u0923\u0924\u0005\u0123\u0000\u0000\u0924\u01d5\u0001"+
		"\u0000\u0000\u0000\u0925\u0926\u0005\u0108\u0000\u0000\u0926\u0927\u0003"+
		"\u01e0\u00f0\u0000\u0927\u01d7\u0001\u0000\u0000\u0000\u0928\u0929\u0003"+
		"\u01e0\u00f0\u0000\u0929\u01d9\u0001\u0000\u0000\u0000\u092a\u092b\u0003"+
		"\u0198\u00cc\u0000\u092b\u092c\u0003\u01e0\u00f0\u0000\u092c\u01db\u0001"+
		"\u0000\u0000\u0000\u092d\u092e\u0005\u0123\u0000\u0000\u092e\u0932\u0003"+
		"\u01e0\u00f0\u0000\u092f\u0930\u0005\u0124\u0000\u0000\u0930\u0932\u0003"+
		"\u01e0\u00f0\u0000\u0931\u092d\u0001\u0000\u0000\u0000\u0931\u092f\u0001"+
		"\u0000\u0000\u0000\u0932\u01dd\u0001\u0000\u0000\u0000\u0933\u0934\u0005"+
		"\u0017\u0000\u0000\u0934\u0938\u00052\u0000\u0000\u0935\u0936\u0005\u0017"+
		"\u0000\u0000\u0936\u0938\u00053\u0000\u0000\u0937\u0933\u0001\u0000\u0000"+
		"\u0000\u0937\u0935\u0001\u0000\u0000\u0000\u0938\u01df\u0001\u0000\u0000"+
		"\u0000\u0939\u093a\u0007#\u0000\u0000\u093a\u01e1\u0001\u0000\u0000\u0000"+
		"\u093b\u093c\u0003\u01e0\u00f0\u0000\u093c\u01e3\u0001\u0000\u0000\u0000"+
		"\u093d\u093e\u0005\u0012\u0000\u0000\u093e\u093f\u0005\u00b9\u0000\u0000"+
		"\u093f\u0940\u0003\u01e8\u00f4\u0000\u0940\u01e5\u0001\u0000\u0000\u0000"+
		"\u0941\u0942\u0005\u00cf\u0000\u0000\u0942\u01e7\u0001\u0000\u0000\u0000"+
		"\u0943\u0944\u0005E\u0000\u0000\u0944\u01e9\u0001\u0000\u0000\u0000\u0945"+
		"\u0946\u0005=\u0000\u0000\u0946\u0947\u0003\u01e0\u00f0\u0000\u0947\u01eb"+
		"\u0001\u0000\u0000\u0000\u0948\u094b\u0003\u020e\u0107\u0000\u0949\u094b"+
		"\u0003\u0210\u0108\u0000\u094a\u0948\u0001\u0000\u0000\u0000\u094a\u0949"+
		"\u0001\u0000\u0000\u0000\u094b\u01ed\u0001\u0000\u0000\u0000\u094c\u094d"+
		"\u0005\u00ac\u0000\u0000\u094d\u094e\u0005\u00ad\u0000\u0000\u094e\u01ef"+
		"\u0001\u0000\u0000\u0000\u094f\u0950\u0005\u00ac\u0000\u0000\u0950\u0951"+
		"\u0005\u00e0\u0000\u0000\u0951\u01f1\u0001\u0000\u0000\u0000\u0952\u0953"+
		"\u0005\u00ac\u0000\u0000\u0953\u0954\u0005\u00d3\u0000\u0000\u0954\u01f3"+
		"\u0001\u0000\u0000\u0000\u0955\u0956\u0005\u000f\u0000\u0000\u0956\u01f5"+
		"\u0001\u0000\u0000\u0000\u0957\u0958\u0005W\u0000\u0000\u0958\u0959\u0003"+
		"\u01e0\u00f0\u0000\u0959\u01f7\u0001\u0000\u0000\u0000\u095a\u095b\u0005"+
		"\u0098\u0000\u0000\u095b\u01f9\u0001\u0000\u0000\u0000\u095c\u095d\u0005"+
		"\u00ff\u0000\u0000\u095d\u095e\u0003\u01e0\u00f0\u0000\u095e\u01fb\u0001"+
		"\u0000\u0000\u0000\u095f\u0960\u0005\u00fa\u0000\u0000\u0960\u0961\u0003"+
		"\u01e0\u00f0\u0000\u0961\u01fd\u0001\u0000\u0000\u0000\u0962\u0963\u0007"+
		"$\u0000\u0000\u0963\u01ff\u0001\u0000\u0000\u0000\u0964\u0965\u0005\u00e2"+
		"\u0000\u0000\u0965\u0966\u0003\u01e0\u00f0\u0000\u0966\u0201\u0001\u0000"+
		"\u0000\u0000\u0967\u0968\u0005\u0130\u0000\u0000\u0968\u0203\u0001\u0000"+
		"\u0000\u0000\u0969\u096a\u0005\u000b\u0000\u0000\u096a\u0205\u0001\u0000"+
		"\u0000\u0000\u096b\u096c\u0005\r\u0000\u0000\u096c\u0207\u0001\u0000\u0000"+
		"\u0000\u096d\u096e\u0005f\u0000\u0000\u096e\u0209\u0001\u0000\u0000\u0000"+
		"\u096f\u0970\u0007%\u0000\u0000\u0970\u020b\u0001\u0000\u0000\u0000\u0971"+
		"\u0972\u0007&\u0000\u0000\u0972\u020d\u0001\u0000\u0000\u0000\u0973\u0974"+
		"\u0007\'\u0000\u0000\u0974\u020f\u0001\u0000\u0000\u0000\u0975\u0976\u0007"+
		"(\u0000\u0000\u0976\u0211\u0001\u0000\u0000\u0000\u0977\u0978\u0007)\u0000"+
		"\u0000\u0978\u0213\u0001\u0000\u0000\u0000\u0979\u097a\u0005\u009b\u0000"+
		"\u0000\u097a\u097b\u0005\u010f\u0000\u0000\u097b\u097c\u0005\u00ef\u0000"+
		"\u0000\u097c\u0215\u0001\u0000\u0000\u0000\u097d\u097e\u0003\u021c\u010e"+
		"\u0000\u097e\u0217\u0001\u0000\u0000\u0000\u097f\u0980\u0003\u021c\u010e"+
		"\u0000\u0980\u0219\u0001\u0000\u0000\u0000\u0981\u0982\u0003\u021c\u010e"+
		"\u0000\u0982\u021b\u0001\u0000\u0000\u0000\u0983\u0988\u0003\u021e\u010f"+
		"\u0000\u0984\u0985\u00051\u0000\u0000\u0985\u0987\u0003\u021e\u010f\u0000"+
		"\u0986\u0984\u0001\u0000\u0000\u0000\u0987\u098a\u0001\u0000\u0000\u0000"+
		"\u0988\u0986\u0001\u0000\u0000\u0000\u0988\u0989\u0001\u0000\u0000\u0000"+
		"\u0989\u021d\u0001\u0000\u0000\u0000\u098a\u0988\u0001\u0000\u0000\u0000"+
		"\u098b\u098d\u0003\u0298\u014c\u0000\u098c\u098e\u0003\u0220\u0110\u0000"+
		"\u098d\u098c\u0001\u0000\u0000\u0000\u098d\u098e\u0001\u0000\u0000\u0000"+
		"\u098e\u0991\u0001\u0000\u0000\u0000\u098f\u0991\u0003\u0220\u0110\u0000"+
		"\u0990\u098b\u0001\u0000\u0000\u0000\u0990\u098f\u0001\u0000\u0000\u0000"+
		"\u0991\u021f\u0001\u0000\u0000\u0000\u0992\u0994\u0003\u0222\u0111\u0000"+
		"\u0993\u0995\u0003\u0220\u0110\u0000\u0994\u0993\u0001\u0000\u0000\u0000"+
		"\u0994\u0995\u0001\u0000\u0000\u0000\u0995\u0221\u0001\u0000\u0000\u0000"+
		"\u0996\u0998\u0005S\u0000\u0000\u0997\u0999\u0003\u0298\u014c\u0000\u0998"+
		"\u0997\u0001\u0000\u0000\u0000\u0998\u0999\u0001\u0000\u0000\u0000\u0999"+
		"\u099e\u0001\u0000\u0000\u0000\u099a\u099e\u0005\u00d6\u0000\u0000\u099b"+
		"\u099e\u0005\u010f\u0000\u0000\u099c\u099e\u0003\u029a\u014d\u0000\u099d"+
		"\u0996\u0001\u0000\u0000\u0000\u099d\u099a\u0001\u0000\u0000\u0000\u099d"+
		"\u099b\u0001\u0000\u0000\u0000\u099d\u099c\u0001\u0000\u0000\u0000\u099e"+
		"\u0223\u0001\u0000\u0000\u0000\u099f\u09a0\u0005\u00da\u0000\u0000\u09a0"+
		"\u0225\u0001\u0000\u0000\u0000\u09a1\u09a2\u0007*\u0000\u0000\u09a2\u0227"+
		"\u0001\u0000\u0000\u0000\u09a3\u09a6\u0005\u010f\u0000\u0000\u09a4\u09a6"+
		"\u0003\u022c\u0116\u0000\u09a5\u09a3\u0001\u0000\u0000\u0000\u09a5\u09a4"+
		"\u0001\u0000\u0000\u0000\u09a6\u0229\u0001\u0000\u0000\u0000\u09a7\u09aa"+
		"\u0005\u0093\u0000\u0000\u09a8\u09ab\u0005\u010f\u0000\u0000\u09a9\u09ab"+
		"\u0003\u022c\u0116\u0000\u09aa\u09a8\u0001\u0000\u0000\u0000\u09aa\u09a9"+
		"\u0001\u0000\u0000\u0000\u09ab\u09ac\u0001\u0000\u0000\u0000\u09ac\u09ad"+
		"\u0005\u00d9\u0000\u0000\u09ad\u022b\u0001\u0000\u0000\u0000\u09ae\u09b3"+
		"\u0003\u0296\u014b\u0000\u09af\u09b0\u00051\u0000\u0000\u09b0\u09b2\u0003"+
		"\u0296\u014b\u0000\u09b1\u09af\u0001\u0000\u0000\u0000\u09b2\u09b5\u0001"+
		"\u0000\u0000\u0000\u09b3\u09b1\u0001\u0000\u0000\u0000\u09b3\u09b4\u0001"+
		"\u0000\u0000\u0000\u09b4\u022d\u0001\u0000\u0000\u0000\u09b5\u09b3\u0001"+
		"\u0000\u0000\u0000\u09b6\u09b8\u0003\u0230\u0118\u0000\u09b7\u09b6\u0001"+
		"\u0000\u0000\u0000\u09b7\u09b8\u0001\u0000\u0000\u0000\u09b8\u022f\u0001"+
		"\u0000\u0000\u0000\u09b9\u09bd\u0003\u0232\u0119\u0000\u09ba\u09bd\u0003"+
		"\u0236\u011b\u0000\u09bb\u09bd\u0003\u0234\u011a\u0000\u09bc\u09b9\u0001"+
		"\u0000\u0000\u0000\u09bc\u09ba\u0001\u0000\u0000\u0000\u09bc\u09bb\u0001"+
		"\u0000\u0000\u0000\u09bd\u0231\u0001\u0000\u0000\u0000\u09be\u09bf\u0007"+
		"+\u0000\u0000\u09bf\u0233\u0001\u0000\u0000\u0000\u09c0\u09c1\u0007,\u0000"+
		"\u0000\u09c1\u0235\u0001\u0000\u0000\u0000\u09c2\u09c3\u0007-\u0000\u0000"+
		"\u09c3\u0237\u0001\u0000\u0000\u0000\u09c4\u09c5\u0007.\u0000\u0000\u09c5"+
		"\u0239\u0001\u0000\u0000\u0000\u09c6\u09c7\u0007/\u0000\u0000\u09c7\u023b"+
		"\u0001\u0000\u0000\u0000\u09c8\u09c9\u00055\u0000\u0000\u09c9\u09ca\u0005"+
		"A\u0000\u0000\u09ca\u09cb\u0003\u027c\u013e\u0000\u09cb\u023d\u0001\u0000"+
		"\u0000\u0000\u09cc\u09cd\u0005A\u0000\u0000\u09cd\u09ce\u0003\u027c\u013e"+
		"\u0000\u09ce\u023f\u0001\u0000\u0000\u0000\u09cf\u09d0\u0003\u028a\u0145"+
		"\u0000\u09d0\u09d1\u0003\u0242\u0121\u0000\u09d1\u0241\u0001\u0000\u0000"+
		"\u0000\u09d2\u09d3\u00070\u0000\u0000\u09d3\u0243\u0001\u0000\u0000\u0000"+
		"\u09d4\u09d5\u0003\u028a\u0145\u0000\u09d5\u09d6\u0003\u0246\u0123\u0000"+
		"\u09d6\u0245\u0001\u0000\u0000\u0000\u09d7\u09d8\u00071\u0000\u0000\u09d8"+
		"\u0247\u0001\u0000\u0000\u0000\u09d9\u09da\u0005A\u0000\u0000\u09da\u09db"+
		"\u0003\u027c\u013e\u0000\u09db\u0249\u0001\u0000\u0000\u0000\u09dc\u09e2"+
		"\u0005\u00e8\u0000\u0000\u09dd\u09de\u0005*\u0000\u0000\u09de\u09e2\u0005"+
		"\u000f\u0000\u0000\u09df\u09e0\u0005*\u0000\u0000\u09e0\u09e2\u0005\u0010"+
		"\u0000\u0000\u09e1\u09dc\u0001\u0000\u0000\u0000\u09e1\u09dd\u0001\u0000"+
		"\u0000\u0000\u09e1";
	private static final String _serializedATNSegment1 =
		"\u09df\u0001\u0000\u0000\u0000\u09e2\u024b\u0001\u0000\u0000\u0000\u09e3"+
		"\u09e4\u0005A\u0000\u0000\u09e4\u09e5\u0003\u027c\u013e\u0000\u09e5\u024d"+
		"\u0001\u0000\u0000\u0000\u09e6\u09e7\u0005\u000b\u0000\u0000\u09e7\u09e8"+
		"\u0005\u00da\u0000\u0000\u09e8\u09ed\u0005\u00ba\u0000\u0000\u09e9\u09ea"+
		"\u0005\u000b\u0000\u0000\u09ea\u09eb\u0005\u00da\u0000\u0000\u09eb\u09ed"+
		"\u0005\u0130\u0000\u0000\u09ec\u09e6\u0001\u0000\u0000\u0000\u09ec\u09e9"+
		"\u0001\u0000\u0000\u0000\u09ed\u024f\u0001\u0000\u0000\u0000\u09ee\u09f1"+
		"\u0005\u0113\u0000\u0000\u09ef\u09f2\u0003\u0240\u0120\u0000\u09f0\u09f2"+
		"\u0003\u0244\u0122\u0000\u09f1\u09ef\u0001\u0000\u0000\u0000\u09f1\u09f0"+
		"\u0001\u0000\u0000\u0000\u09f2\u09f3\u0001\u0000\u0000\u0000\u09f3\u09f1"+
		"\u0001\u0000\u0000\u0000\u09f3\u09f4\u0001\u0000\u0000\u0000\u09f4\u0251"+
		"\u0001\u0000\u0000\u0000\u09f5\u09f6\u0005\u00bd\u0000\u0000\u09f6\u09f7"+
		"\u0003\u0296\u014b\u0000\u09f7\u09f8\u0003\u00b0X\u0000\u09f8\u0253\u0001"+
		"\u0000\u0000\u0000\u09f9\u09fa\u0005\u0103\u0000\u0000\u09fa\u09fb\u0005"+
		"A\u0000\u0000\u09fb\u09fc\u0003\u027c\u013e\u0000\u09fc\u0255\u0001\u0000"+
		"\u0000\u0000\u09fd\u09fe\u0005\u0106\u0000\u0000\u09fe\u09ff\u0005A\u0000"+
		"\u0000\u09ff\u0a00\u0003\u027c\u013e\u0000\u0a00\u0257\u0001\u0000\u0000"+
		"\u0000\u0a01\u0a06\u0005\u012b\u0000\u0000\u0a02\u0a04\u0005\u0005\u0000"+
		"\u0000\u0a03\u0a05\u0003\u025a\u012d\u0000\u0a04\u0a03\u0001\u0000\u0000"+
		"\u0000\u0a04\u0a05\u0001\u0000\u0000\u0000\u0a05\u0a07\u0001\u0000\u0000"+
		"\u0000\u0a06\u0a02\u0001\u0000\u0000\u0000\u0a06\u0a07\u0001\u0000\u0000"+
		"\u0000\u0a07\u0a0a\u0001\u0000\u0000\u0000\u0a08\u0a0a\u0005\u00b5\u0000"+
		"\u0000\u0a09\u0a01\u0001\u0000\u0000\u0000\u0a09\u0a08\u0001\u0000\u0000"+
		"\u0000\u0a0a\u0259\u0001\u0000\u0000\u0000\u0a0b\u0a0c\u00072\u0000\u0000"+
		"\u0a0c\u025b\u0001\u0000\u0000\u0000\u0a0d\u0a0e\u0007.\u0000\u0000\u0a0e"+
		"\u025d\u0001\u0000\u0000\u0000\u0a0f\u0a10\u0003\u027c\u013e\u0000\u0a10"+
		"\u025f\u0001\u0000\u0000\u0000\u0a11\u0a12\u0003\u027c\u013e\u0000\u0a12"+
		"\u0261\u0001\u0000\u0000\u0000\u0a13\u0a14\u0005\u000f\u0000\u0000\u0a14"+
		"\u0a15\u0003\u025e\u012f\u0000\u0a15\u0a16\u0005p\u0000\u0000\u0a16\u0a17"+
		"\u0005A\u0000\u0000\u0a17\u0a18\u0003\u0260\u0130\u0000\u0a18\u0263\u0001"+
		"\u0000\u0000\u0000\u0a19\u0a1a\u0005\u000f\u0000\u0000\u0a1a\u0a1b\u0003"+
		"\u025e\u012f\u0000\u0a1b\u0a1c\u0005p\u0000\u0000\u0a1c\u0a1d\u0005A\u0000"+
		"\u0000\u0a1d\u0265\u0001\u0000\u0000\u0000\u0a1e\u0a1f\u0005\u000f\u0000"+
		"\u0000\u0a1f\u0a20\u0003\u025e\u012f\u0000\u0a20\u0a21\u0005\u00fa\u0000"+
		"\u0000\u0a21\u0a22\u0005A\u0000\u0000\u0a22\u0a23\u0003\u0268\u0134\u0000"+
		"\u0a23\u0267\u0001\u0000\u0000\u0000\u0a24\u0a25\u0005\u010a\u0000\u0000"+
		"\u0a25\u0a26\u0003\u0260\u0130\u0000\u0a26\u0269\u0001\u0000\u0000\u0000"+
		"\u0a27\u0a28\u0005\u0123\u0000\u0000\u0a28\u0a29\u0003\u0276\u013b\u0000"+
		"\u0a29\u026b\u0001\u0000\u0000\u0000\u0a2a\u0a2b\u0005\u00c1\u0000\u0000"+
		"\u0a2b\u0a2c\u0003\u01c6\u00e3\u0000\u0a2c\u026d\u0001\u0000\u0000\u0000"+
		"\u0a2d\u0a2e\u0005V\u0000\u0000\u0a2e\u0a2f\u0003\u028c\u0146\u0000\u0a2f"+
		"\u026f\u0001\u0000\u0000\u0000\u0a30\u0a31\u0005\u00d2\u0000\u0000\u0a31"+
		"\u0a32\u0003\u028c\u0146\u0000\u0a32\u0271\u0001\u0000\u0000\u0000\u0a33"+
		"\u0a34\u00073\u0000\u0000\u0a34\u0273\u0001\u0000\u0000\u0000\u0a35\u0a38"+
		"\u0003\u0296\u014b\u0000\u0a36\u0a38\u0003\u0108\u0084\u0000\u0a37\u0a35"+
		"\u0001\u0000\u0000\u0000\u0a37\u0a36\u0001\u0000\u0000\u0000\u0a38\u0275"+
		"\u0001\u0000\u0000\u0000\u0a39\u0a3c\u0003\u0296\u014b\u0000\u0a3a\u0a3c"+
		"\u0003\u0108\u0084\u0000\u0a3b\u0a39\u0001\u0000\u0000\u0000\u0a3b\u0a3a"+
		"\u0001\u0000\u0000\u0000\u0a3c\u0277\u0001\u0000\u0000\u0000\u0a3d\u0a42"+
		"\u0003\u0276\u013b\u0000\u0a3e\u0a3f\u00051\u0000\u0000\u0a3f\u0a41\u0003"+
		"\u0276\u013b\u0000\u0a40\u0a3e\u0001\u0000\u0000\u0000\u0a41\u0a44\u0001"+
		"\u0000\u0000\u0000\u0a42\u0a40\u0001\u0000\u0000\u0000\u0a42\u0a43\u0001"+
		"\u0000\u0000\u0000\u0a43\u0279\u0001\u0000\u0000\u0000\u0a44\u0a42\u0001"+
		"\u0000\u0000\u0000\u0a45\u0a4a\u0003\u027c\u013e\u0000\u0a46\u0a47\u0005"+
		"1\u0000\u0000\u0a47\u0a49\u0003\u027c\u013e\u0000\u0a48\u0a46\u0001\u0000"+
		"\u0000\u0000\u0a49\u0a4c\u0001\u0000\u0000\u0000\u0a4a\u0a48\u0001\u0000"+
		"\u0000\u0000\u0a4a\u0a4b\u0001\u0000\u0000\u0000\u0a4b\u027b\u0001\u0000"+
		"\u0000\u0000\u0a4c\u0a4a\u0001\u0000\u0000\u0000\u0a4d\u0a50\u0003\u027e"+
		"\u013f\u0000\u0a4e\u0a50\u0003\u0108\u0084\u0000\u0a4f\u0a4d\u0001\u0000"+
		"\u0000\u0000\u0a4f\u0a4e\u0001\u0000\u0000\u0000\u0a50\u027d\u0001\u0000"+
		"\u0000\u0000\u0a51\u0a56\u0003\u0296\u014b\u0000\u0a52\u0a53\u0005S\u0000"+
		"\u0000\u0a53\u0a55\u0003\u0296\u014b\u0000\u0a54\u0a52\u0001\u0000\u0000"+
		"\u0000\u0a55\u0a58\u0001\u0000\u0000\u0000\u0a56\u0a54\u0001\u0000\u0000"+
		"\u0000\u0a56\u0a57\u0001\u0000\u0000\u0000\u0a57\u027f\u0001\u0000\u0000"+
		"\u0000\u0a58\u0a56\u0001\u0000\u0000\u0000\u0a59\u0a62\u0005\u0092\u0000"+
		"\u0000\u0a5a\u0a5f\u0003\u0284\u0142\u0000\u0a5b\u0a5c\u00051\u0000\u0000"+
		"\u0a5c\u0a5e\u0003\u0284\u0142\u0000\u0a5d\u0a5b\u0001\u0000\u0000\u0000"+
		"\u0a5e\u0a61\u0001\u0000\u0000\u0000\u0a5f\u0a5d\u0001\u0000\u0000\u0000"+
		"\u0a5f\u0a60\u0001\u0000\u0000\u0000\u0a60\u0a63\u0001\u0000\u0000\u0000"+
		"\u0a61\u0a5f\u0001\u0000\u0000\u0000\u0a62\u0a5a\u0001\u0000\u0000\u0000"+
		"\u0a62\u0a63\u0001\u0000\u0000\u0000\u0a63\u0a64\u0001\u0000\u0000\u0000"+
		"\u0a64\u0a65\u0005\u00d8\u0000\u0000\u0a65\u0281\u0001\u0000\u0000\u0000"+
		"\u0a66\u0a69\u0003\u0284\u0142\u0000\u0a67\u0a68\u00051\u0000\u0000\u0a68"+
		"\u0a6a\u0003\u0284\u0142\u0000\u0a69\u0a67\u0001\u0000\u0000\u0000\u0a6a"+
		"\u0a6b\u0001\u0000\u0000\u0000\u0a6b\u0a69\u0001\u0000\u0000\u0000\u0a6b"+
		"\u0a6c\u0001\u0000\u0000\u0000\u0a6c\u0283\u0001\u0000\u0000\u0000\u0a6d"+
		"\u0a6e\u00074\u0000\u0000\u0a6e\u0285\u0001\u0000\u0000\u0000\u0a6f\u0a72"+
		"\u0003\u0284\u0142\u0000\u0a70\u0a72\u0003\u0108\u0084\u0000\u0a71\u0a6f"+
		"\u0001\u0000\u0000\u0000\u0a71\u0a70\u0001\u0000\u0000\u0000\u0a72\u0287"+
		"\u0001\u0000\u0000\u0000\u0a73\u0a76\u0003\u0284\u0142\u0000\u0a74\u0a76"+
		"\u0003\u0108\u0084\u0000\u0a75\u0a73\u0001\u0000\u0000\u0000\u0a75\u0a74"+
		"\u0001\u0000\u0000\u0000\u0a76\u0289\u0001\u0000\u0000\u0000\u0a77\u0a7a"+
		"\u0005\u0005\u0000\u0000\u0a78\u0a7a\u0003\u0108\u0084\u0000\u0a79\u0a77"+
		"\u0001\u0000\u0000\u0000\u0a79\u0a78\u0001\u0000\u0000\u0000\u0a7a\u028b"+
		"\u0001\u0000\u0000\u0000\u0a7b\u0a7e\u0003\u028e\u0147\u0000\u0a7c\u0a7e"+
		"\u0003\u0108\u0084\u0000\u0a7d\u0a7b\u0001\u0000\u0000\u0000\u0a7d\u0a7c"+
		"\u0001\u0000\u0000\u0000\u0a7e\u028d\u0001\u0000\u0000\u0000\u0a7f\u0a8d"+
		"\u0005\u0093\u0000\u0000\u0a80\u0a81\u0003\u0106\u0083\u0000\u0a81\u0a82"+
		"\u0005/\u0000\u0000\u0a82\u0a8a\u0003\u00b0X\u0000\u0a83\u0a84\u00051"+
		"\u0000\u0000\u0a84\u0a85\u0003\u0106\u0083\u0000\u0a85\u0a86\u0005/\u0000"+
		"\u0000\u0a86\u0a87\u0003\u00b0X\u0000\u0a87\u0a89\u0001\u0000\u0000\u0000"+
		"\u0a88\u0a83\u0001\u0000\u0000\u0000\u0a89\u0a8c\u0001\u0000\u0000\u0000"+
		"\u0a8a\u0a88\u0001\u0000\u0000\u0000\u0a8a\u0a8b\u0001\u0000\u0000\u0000"+
		"\u0a8b\u0a8e\u0001\u0000\u0000\u0000\u0a8c\u0a8a\u0001\u0000\u0000\u0000"+
		"\u0a8d\u0a80\u0001\u0000\u0000\u0000\u0a8d\u0a8e\u0001\u0000\u0000\u0000"+
		"\u0a8e\u0a8f\u0001\u0000\u0000\u0000\u0a8f\u0a90\u0005\u00d9\u0000\u0000"+
		"\u0a90\u028f\u0001\u0000\u0000\u0000\u0a91\u0a94\u0003\u0292\u0149\u0000"+
		"\u0a92\u0a94\u0003\u0294\u014a\u0000\u0a93\u0a91\u0001\u0000\u0000\u0000"+
		"\u0a93\u0a92\u0001\u0000\u0000\u0000\u0a94\u0291\u0001\u0000\u0000\u0000"+
		"\u0a95\u0a96\u0003\u0298\u014c\u0000\u0a96\u0293\u0001\u0000\u0000\u0000"+
		"\u0a97\u0a98\u0003\u029a\u014d\u0000\u0a98\u0295\u0001\u0000\u0000\u0000"+
		"\u0a99\u0a9c\u0003\u0298\u014c\u0000\u0a9a\u0a9c\u0003\u029a\u014d\u0000"+
		"\u0a9b\u0a99\u0001\u0000\u0000\u0000\u0a9b\u0a9a\u0001\u0000\u0000\u0000"+
		"\u0a9c\u0297\u0001\u0000\u0000\u0000\u0a9d\u0a9e\u0005\n\u0000\u0000\u0a9e"+
		"\u0299\u0001\u0000\u0000\u0000\u0a9f\u0aa9\u0003\u029e\u014f\u0000\u0aa0"+
		"\u0aa9\u0005\u00b3\u0000\u0000\u0aa1\u0aa9\u0005\u00b6\u0000\u0000\u0aa2"+
		"\u0aa9\u0005\u011b\u0000\u0000\u0aa3\u0aa9\u0005\u00b2\u0000\u0000\u0aa4"+
		"\u0aa9\u0005\u00a8\u0000\u0000\u0aa5\u0aa9\u0005\u00a9\u0000\u0000\u0aa6"+
		"\u0aa9\u0005\u00aa\u0000\u0000\u0aa7\u0aa9\u0005\u00ab\u0000\u0000\u0aa8"+
		"\u0a9f\u0001\u0000\u0000\u0000\u0aa8\u0aa0\u0001\u0000\u0000\u0000\u0aa8"+
		"\u0aa1\u0001\u0000\u0000\u0000\u0aa8\u0aa2\u0001\u0000\u0000\u0000\u0aa8"+
		"\u0aa3\u0001\u0000\u0000\u0000\u0aa8\u0aa4\u0001\u0000\u0000\u0000\u0aa8"+
		"\u0aa5\u0001\u0000\u0000\u0000\u0aa8\u0aa6\u0001\u0000\u0000\u0000\u0aa8"+
		"\u0aa7\u0001\u0000\u0000\u0000\u0aa9\u029b\u0001\u0000\u0000\u0000\u0aaa"+
		"\u0aad\u0003\u0298\u014c\u0000\u0aab\u0aad\u0003\u029e\u014f\u0000\u0aac"+
		"\u0aaa\u0001\u0000\u0000\u0000\u0aac\u0aab\u0001\u0000\u0000\u0000\u0aad"+
		"\u029d\u0001\u0000\u0000\u0000\u0aae\u0aaf\u0003\u02a0\u0150\u0000\u0aaf"+
		"\u029f\u0001\u0000\u0000\u0000\u0ab0\u0ab1\u00075\u0000\u0000\u0ab1\u02a1"+
		"\u0001\u0000\u0000\u0000\u0ab2\u0ab3\u0005\u0000\u0000\u0001\u0ab3\u02a3"+
		"\u0001\u0000\u0000\u0000\u0107\u02a9\u02ad\u02b2\u02ba\u02bf\u02c4\u02ca"+
		"\u02dd\u02e1\u02eb\u02f3\u02f7\u02fa\u02fd\u0302\u0306\u030c\u0312\u031f"+
		"\u032e\u033c\u0355\u035d\u0368\u036b\u0373\u0377\u037b\u0381\u0385\u038a"+
		"\u038d\u0392\u0395\u0397\u03a3\u03a6\u03b5\u03bc\u03c9\u03d3\u03d6\u03d9"+
		"\u03e2\u03e6\u03e8\u03ea\u03f4\u03fa\u0402\u040d\u0412\u0416\u041c\u0425"+
		"\u0428\u042e\u0431\u0437\u0439\u044b\u044e\u0452\u0455\u045c\u0464\u046a"+
		"\u046d\u0474\u047c\u0484\u0488\u048d\u0491\u049b\u04a1\u04a5\u04a7\u04ac"+
		"\u04b1\u04b5\u04b8\u04bc\u04c0\u04c3\u04c9\u04cb\u04d7\u04db\u04de\u04e1"+
		"\u04e5\u04eb\u04ee\u04f1\u04f9\u04fd\u0501\u0503\u0508\u050c\u050e\u0518"+
		"\u052c\u052f\u0534\u0537\u053a\u053d\u0541\u0544\u0548\u054b\u0550\u0554"+
		"\u0559\u0563\u0567\u056a\u0570\u0575\u057a\u0580\u0585\u058d\u0595\u059b"+
		"\u05a3\u05af\u05b8\u05c0\u05cb\u05d3\u05db\u05e1\u05eb\u05f0\u05f9\u05fe"+
		"\u0603\u0607\u060c\u060f\u0612\u061b\u0623\u062b\u0631\u0637\u0642\u0646"+
		"\u0649\u0656\u0670\u067b\u0681\u0685\u0693\u0697\u06a1\u06ac\u06b1\u06b6"+
		"\u06ba\u06bf\u06c2\u06c8\u06d0\u06d6\u06d8\u06e0\u06e5\u06ff\u0708\u070f"+
		"\u0712\u0715\u0729\u072c\u0738\u0743\u0747\u0749\u0751\u0755\u0757\u0761"+
		"\u0766\u0770\u0773\u077e\u0783\u078a\u078d\u079b\u07a5\u07ad\u07b2\u07b7"+
		"\u07c2\u07d0\u07d7\u07f2\u07f9\u07fb\u0800\u0804\u0807\u0816\u0832\u083b"+
		"\u085f\u0866\u086a\u086e\u087c\u088d\u0898\u08ac\u08d4\u08da\u090c\u0915"+
		"\u0931\u0937\u094a\u0988\u098d\u0990\u0994\u0998\u099d\u09a5\u09aa\u09b3"+
		"\u09b7\u09bc\u09e1\u09ec\u09f1\u09f3\u0a04\u0a06\u0a09\u0a37\u0a3b\u0a42"+
		"\u0a4a\u0a4f\u0a56\u0a5f\u0a62\u0a6b\u0a71\u0a75\u0a79\u0a7d\u0a8a\u0a8d"+
		"\u0a93\u0a9b\u0aa8\u0aac";
	public static final String _serializedATN = Utils.join(
		new String[] {
			_serializedATNSegment0,
			_serializedATNSegment1
		},
		""
	);
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}