// Generated from src/main/antlr4/org/neo4j/cypher/internal/parser/v5/Cypher5Parser.g4 by ANTLR 4.13.2
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
		RULE_typeListSuffix = 144, RULE_symbolicNameOrStringParameter = 145, RULE_commandNameExpression = 146, 
		RULE_symbolicNameOrStringParameterList = 147, RULE_symbolicAliasNameList = 148, 
		RULE_symbolicAliasNameOrParameter = 149, RULE_symbolicAliasName = 150, 
		RULE_stringListLiteral = 151, RULE_stringList = 152, RULE_stringLiteral = 153, 
		RULE_stringOrParameterExpression = 154, RULE_stringOrParameter = 155, 
		RULE_uIntOrIntParameter = 156, RULE_mapOrParameter = 157, RULE_map = 158, 
		RULE_symbolicVariableNameString = 159, RULE_escapedSymbolicVariableNameString = 160, 
		RULE_unescapedSymbolicVariableNameString = 161, RULE_symbolicNameString = 162, 
		RULE_escapedSymbolicNameString = 163, RULE_unescapedSymbolicNameString = 164, 
		RULE_symbolicLabelNameString = 165, RULE_unescapedLabelSymbolicNameString = 166, 
		RULE_unescapedLabelSymbolicNameString_ = 167, RULE_endOfFile = 168;
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
			"symbolicNameOrStringParameter", "commandNameExpression", "symbolicNameOrStringParameterList", 
			"symbolicAliasNameList", "symbolicAliasNameOrParameter", "symbolicAliasName", 
			"stringListLiteral", "stringList", "stringLiteral", "stringOrParameterExpression", 
			"stringOrParameter", "uIntOrIntParameter", "mapOrParameter", "map", "symbolicVariableNameString", 
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
			setState(338);
			statement();
			setState(343);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,0,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(339);
					match(SEMICOLON);
					setState(340);
					statement();
					}
					} 
				}
				setState(345);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,0,_ctx);
			}
			setState(347);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==SEMICOLON) {
				{
				setState(346);
				match(SEMICOLON);
				}
			}

			setState(349);
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
			setState(352);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==USING) {
				{
				setState(351);
				periodicCommitQueryHintFailure();
				}
			}

			setState(354);
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
			setState(356);
			match(USING);
			setState(357);
			match(PERIODIC);
			setState(358);
			match(COMMIT);
			setState(360);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==UNSIGNED_DECIMAL_INTEGER) {
				{
				setState(359);
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
			setState(362);
			singleQuery();
			setState(370);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==UNION) {
				{
				{
				setState(363);
				match(UNION);
				setState(365);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==ALL || _la==DISTINCT) {
					{
					setState(364);
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

				setState(367);
				singleQuery();
				}
				}
				setState(372);
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
			setState(374); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(373);
				clause();
				}
				}
				setState(376); 
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
			setState(395);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,7,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(378);
				useClause();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(379);
				finishClause();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(380);
				returnClause();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(381);
				createClause();
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(382);
				insertClause();
				}
				break;
			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(383);
				deleteClause();
				}
				break;
			case 7:
				enterOuterAlt(_localctx, 7);
				{
				setState(384);
				setClause();
				}
				break;
			case 8:
				enterOuterAlt(_localctx, 8);
				{
				setState(385);
				removeClause();
				}
				break;
			case 9:
				enterOuterAlt(_localctx, 9);
				{
				setState(386);
				matchClause();
				}
				break;
			case 10:
				enterOuterAlt(_localctx, 10);
				{
				setState(387);
				mergeClause();
				}
				break;
			case 11:
				enterOuterAlt(_localctx, 11);
				{
				setState(388);
				withClause();
				}
				break;
			case 12:
				enterOuterAlt(_localctx, 12);
				{
				setState(389);
				unwindClause();
				}
				break;
			case 13:
				enterOuterAlt(_localctx, 13);
				{
				setState(390);
				callClause();
				}
				break;
			case 14:
				enterOuterAlt(_localctx, 14);
				{
				setState(391);
				subqueryClause();
				}
				break;
			case 15:
				enterOuterAlt(_localctx, 15);
				{
				setState(392);
				loadCSVClause();
				}
				break;
			case 16:
				enterOuterAlt(_localctx, 16);
				{
				setState(393);
				foreachClause();
				}
				break;
			case 17:
				enterOuterAlt(_localctx, 17);
				{
				setState(394);
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
			setState(397);
			match(USE);
			setState(399);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,8,_ctx) ) {
			case 1:
				{
				setState(398);
				match(GRAPH);
				}
				break;
			}
			setState(401);
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
			setState(409);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,9,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(403);
				match(LPAREN);
				setState(404);
				graphReference();
				setState(405);
				match(RPAREN);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(407);
				functionInvocation();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(408);
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
			setState(411);
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
			setState(413);
			match(RETURN);
			setState(414);
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
			setState(417);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,10,_ctx) ) {
			case 1:
				{
				setState(416);
				match(DISTINCT);
				}
				break;
			}
			setState(419);
			returnItems();
			setState(421);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,11,_ctx) ) {
			case 1:
				{
				setState(420);
				orderBy();
				}
				break;
			}
			setState(424);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,12,_ctx) ) {
			case 1:
				{
				setState(423);
				skip();
				}
				break;
			}
			setState(427);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,13,_ctx) ) {
			case 1:
				{
				setState(426);
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
			setState(429);
			expression();
			setState(432);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==AS) {
				{
				setState(430);
				match(AS);
				setState(431);
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
			setState(436);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case TIMES:
				{
				setState(434);
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
				setState(435);
				returnItem();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(442);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(438);
				match(COMMA);
				setState(439);
				returnItem();
				}
				}
				setState(444);
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
			setState(445);
			expression();
			setState(448);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case ASC:
			case ASCENDING:
				{
				setState(446);
				ascToken();
				}
				break;
			case DESC:
			case DESCENDING:
				{
				setState(447);
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
			setState(450);
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
			setState(452);
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
			setState(454);
			match(ORDER);
			setState(455);
			match(BY);
			setState(456);
			orderItem();
			setState(461);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(457);
				match(COMMA);
				setState(458);
				orderItem();
				}
				}
				setState(463);
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
			setState(464);
			_la = _input.LA(1);
			if ( !(_la==OFFSET || _la==SKIPROWS) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			setState(465);
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
			setState(467);
			match(LIMITROWS);
			setState(468);
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
			setState(470);
			match(WHERE);
			setState(471);
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
			setState(473);
			match(WITH);
			setState(474);
			returnBody();
			setState(476);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WHERE) {
				{
				setState(475);
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
			setState(478);
			match(CREATE);
			setState(479);
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
			setState(481);
			match(INSERT);
			setState(482);
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
			setState(484);
			match(SET);
			setState(485);
			setItem();
			setState(490);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(486);
				match(COMMA);
				setState(487);
				setItem();
				}
				}
				setState(492);
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
			setState(515);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,21,_ctx) ) {
			case 1:
				_localctx = new SetPropContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(493);
				propertyExpression();
				setState(494);
				match(EQ);
				setState(495);
				expression();
				}
				break;
			case 2:
				_localctx = new SetDynamicPropContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(497);
				dynamicPropertyExpression();
				setState(498);
				match(EQ);
				setState(499);
				expression();
				}
				break;
			case 3:
				_localctx = new SetPropsContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(501);
				variable();
				setState(502);
				match(EQ);
				setState(503);
				expression();
				}
				break;
			case 4:
				_localctx = new AddPropContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(505);
				variable();
				setState(506);
				match(PLUSEQUAL);
				setState(507);
				expression();
				}
				break;
			case 5:
				_localctx = new SetLabelsContext(_localctx);
				enterOuterAlt(_localctx, 5);
				{
				setState(509);
				variable();
				setState(510);
				nodeLabels();
				}
				break;
			case 6:
				_localctx = new SetLabelsIsContext(_localctx);
				enterOuterAlt(_localctx, 6);
				{
				setState(512);
				variable();
				setState(513);
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
			setState(517);
			match(REMOVE);
			setState(518);
			removeItem();
			setState(523);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(519);
				match(COMMA);
				setState(520);
				removeItem();
				}
				}
				setState(525);
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
			setState(534);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,23,_ctx) ) {
			case 1:
				_localctx = new RemovePropContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(526);
				propertyExpression();
				}
				break;
			case 2:
				_localctx = new RemoveDynamicPropContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(527);
				dynamicPropertyExpression();
				}
				break;
			case 3:
				_localctx = new RemoveLabelsContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(528);
				variable();
				setState(529);
				nodeLabels();
				}
				break;
			case 4:
				_localctx = new RemoveLabelsIsContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(531);
				variable();
				setState(532);
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
			setState(537);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==DETACH || _la==NODETACH) {
				{
				setState(536);
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

			setState(539);
			match(DELETE);
			setState(540);
			expression();
			setState(545);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(541);
				match(COMMA);
				setState(542);
				expression();
				}
				}
				setState(547);
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
			setState(549);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==OPTIONAL) {
				{
				setState(548);
				match(OPTIONAL);
				}
			}

			setState(551);
			match(MATCH);
			setState(553);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,27,_ctx) ) {
			case 1:
				{
				setState(552);
				matchMode();
				}
				break;
			}
			setState(555);
			patternList();
			setState(559);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==USING) {
				{
				{
				setState(556);
				hint();
				}
				}
				setState(561);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(563);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WHERE) {
				{
				setState(562);
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
			setState(581);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case REPEATABLE:
				enterOuterAlt(_localctx, 1);
				{
				setState(565);
				match(REPEATABLE);
				setState(571);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case ELEMENT:
					{
					setState(566);
					match(ELEMENT);
					setState(568);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,30,_ctx) ) {
					case 1:
						{
						setState(567);
						match(BINDINGS);
						}
						break;
					}
					}
					break;
				case ELEMENTS:
					{
					setState(570);
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
				setState(573);
				match(DIFFERENT);
				setState(579);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case RELATIONSHIP:
					{
					setState(574);
					match(RELATIONSHIP);
					setState(576);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,32,_ctx) ) {
					case 1:
						{
						setState(575);
						match(BINDINGS);
						}
						break;
					}
					}
					break;
				case RELATIONSHIPS:
					{
					setState(578);
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
			setState(583);
			match(USING);
			setState(611);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case BTREE:
			case INDEX:
			case POINT:
			case RANGE:
			case TEXT:
				{
				{
				setState(593);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case INDEX:
					{
					setState(584);
					match(INDEX);
					}
					break;
				case BTREE:
					{
					setState(585);
					match(BTREE);
					setState(586);
					match(INDEX);
					}
					break;
				case TEXT:
					{
					setState(587);
					match(TEXT);
					setState(588);
					match(INDEX);
					}
					break;
				case RANGE:
					{
					setState(589);
					match(RANGE);
					setState(590);
					match(INDEX);
					}
					break;
				case POINT:
					{
					setState(591);
					match(POINT);
					setState(592);
					match(INDEX);
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(596);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,36,_ctx) ) {
				case 1:
					{
					setState(595);
					match(SEEK);
					}
					break;
				}
				setState(598);
				variable();
				setState(599);
				labelOrRelType();
				setState(600);
				match(LPAREN);
				setState(601);
				nonEmptyNameList();
				setState(602);
				match(RPAREN);
				}
				}
				break;
			case JOIN:
				{
				setState(604);
				match(JOIN);
				setState(605);
				match(ON);
				setState(606);
				nonEmptyNameList();
				}
				break;
			case SCAN:
				{
				setState(607);
				match(SCAN);
				setState(608);
				variable();
				setState(609);
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
			setState(613);
			match(MERGE);
			setState(614);
			pattern();
			setState(618);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==ON) {
				{
				{
				setState(615);
				mergeAction();
				}
				}
				setState(620);
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
			setState(621);
			match(ON);
			setState(622);
			_la = _input.LA(1);
			if ( !(_la==CREATE || _la==MATCH) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			setState(623);
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
			setState(625);
			match(UNWIND);
			setState(626);
			expression();
			setState(627);
			match(AS);
			setState(628);
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
			setState(631);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==OPTIONAL) {
				{
				setState(630);
				match(OPTIONAL);
				}
			}

			setState(633);
			match(CALL);
			setState(634);
			procedureName();
			setState(647);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LPAREN) {
				{
				setState(635);
				match(LPAREN);
				setState(644);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & -985163492229136L) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & -1170935971839737857L) != 0) || ((((_la - 128)) & ~0x3f) == 0 && ((1L << (_la - 128)) & -120528764929L) != 0) || ((((_la - 192)) & ~0x3f) == 0 && ((1L << (_la - 192)) & -36169535635589377L) != 0) || ((((_la - 256)) & ~0x3f) == 0 && ((1L << (_la - 256)) & 18014398509449215L) != 0)) {
					{
					setState(636);
					procedureArgument();
					setState(641);
					_errHandler.sync(this);
					_la = _input.LA(1);
					while (_la==COMMA) {
						{
						{
						setState(637);
						match(COMMA);
						setState(638);
						procedureArgument();
						}
						}
						setState(643);
						_errHandler.sync(this);
						_la = _input.LA(1);
					}
					}
				}

				setState(646);
				match(RPAREN);
				}
			}

			setState(664);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==YIELD) {
				{
				setState(649);
				match(YIELD);
				setState(662);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case TIMES:
					{
					setState(650);
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
					setState(651);
					procedureResultItem();
					setState(656);
					_errHandler.sync(this);
					_la = _input.LA(1);
					while (_la==COMMA) {
						{
						{
						setState(652);
						match(COMMA);
						setState(653);
						procedureResultItem();
						}
						}
						setState(658);
						_errHandler.sync(this);
						_la = _input.LA(1);
					}
					setState(660);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==WHERE) {
						{
						setState(659);
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
			setState(666);
			namespace();
			setState(667);
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
			setState(669);
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
			setState(671);
			symbolicNameString();
			setState(674);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==AS) {
				{
				setState(672);
				match(AS);
				setState(673);
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
			setState(676);
			match(LOAD);
			setState(677);
			match(CSV);
			setState(680);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WITH) {
				{
				setState(678);
				match(WITH);
				setState(679);
				match(HEADERS);
				}
			}

			setState(682);
			match(FROM);
			setState(683);
			expression();
			setState(684);
			match(AS);
			setState(685);
			variable();
			setState(688);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==FIELDTERMINATOR) {
				{
				setState(686);
				match(FIELDTERMINATOR);
				setState(687);
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
			setState(690);
			match(FOREACH);
			setState(691);
			match(LPAREN);
			setState(692);
			variable();
			setState(693);
			match(IN);
			setState(694);
			expression();
			setState(695);
			match(BAR);
			setState(697); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(696);
				clause();
				}
				}
				setState(699); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( ((((_la - 41)) & ~0x3f) == 0 && ((1L << (_la - 41)) & 141734969345L) != 0) || ((((_la - 110)) & ~0x3f) == 0 && ((1L << (_la - 110)) & 1694347485511689L) != 0) || ((((_la - 174)) & ~0x3f) == 0 && ((1L << (_la - 174)) & 580964351930934273L) != 0) || ((((_la - 250)) & ~0x3f) == 0 && ((1L << (_la - 250)) & 4504974016905473L) != 0) );
			setState(701);
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
			setState(704);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==OPTIONAL) {
				{
				setState(703);
				match(OPTIONAL);
				}
			}

			setState(706);
			match(CALL);
			setState(708);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LPAREN) {
				{
				setState(707);
				subqueryScope();
				}
			}

			setState(710);
			match(LCURLY);
			setState(711);
			regularQuery();
			setState(712);
			match(RCURLY);
			setState(714);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==IN) {
				{
				setState(713);
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
			setState(716);
			match(LPAREN);
			setState(726);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case TIMES:
				{
				setState(717);
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
				setState(718);
				variable();
				setState(723);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(719);
					match(COMMA);
					setState(720);
					variable();
					}
					}
					setState(725);
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
			setState(728);
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
			setState(730);
			match(IN);
			setState(735);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,57,_ctx) ) {
			case 1:
				{
				setState(732);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,56,_ctx) ) {
				case 1:
					{
					setState(731);
					expression();
					}
					break;
				}
				setState(734);
				match(CONCURRENT);
				}
				break;
			}
			setState(737);
			match(TRANSACTIONS);
			setState(743);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (((((_la - 183)) & ~0x3f) == 0 && ((1L << (_la - 183)) & 70368744177669L) != 0)) {
				{
				setState(741);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case OF:
					{
					setState(738);
					subqueryInTransactionsBatchParameters();
					}
					break;
				case ON:
					{
					setState(739);
					subqueryInTransactionsErrorParameters();
					}
					break;
				case REPORT:
					{
					setState(740);
					subqueryInTransactionsReportParameters();
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				}
				setState(745);
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
			setState(746);
			match(OF);
			setState(747);
			expression();
			setState(748);
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
			setState(750);
			match(ON);
			setState(751);
			match(ERROR);
			setState(752);
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
			setState(754);
			match(REPORT);
			setState(755);
			match(STATUS);
			setState(756);
			match(AS);
			setState(757);
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
			setState(771);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case ORDER:
				enterOuterAlt(_localctx, 1);
				{
				setState(759);
				orderBy();
				setState(761);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,60,_ctx) ) {
				case 1:
					{
					setState(760);
					skip();
					}
					break;
				}
				setState(764);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,61,_ctx) ) {
				case 1:
					{
					setState(763);
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
				setState(766);
				skip();
				setState(768);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,62,_ctx) ) {
				case 1:
					{
					setState(767);
					limit();
					}
					break;
				}
				}
				break;
			case LIMITROWS:
				enterOuterAlt(_localctx, 3);
				{
				setState(770);
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
			setState(773);
			pattern();
			setState(778);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(774);
				match(COMMA);
				setState(775);
				pattern();
				}
				}
				setState(780);
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
			setState(781);
			insertPattern();
			setState(786);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(782);
				match(COMMA);
				setState(783);
				insertPattern();
				}
				}
				setState(788);
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
			setState(792);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,66,_ctx) ) {
			case 1:
				{
				setState(789);
				variable();
				setState(790);
				match(EQ);
				}
				break;
			}
			setState(795);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==ALL || _la==ANY || _la==SHORTEST) {
				{
				setState(794);
				selector();
				}
			}

			setState(797);
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
			setState(802);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & -985163492230144L) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & -1170935971839803393L) != 0) || ((((_la - 128)) & ~0x3f) == 0 && ((1L << (_la - 128)) & -129253703681L) != 0) || ((((_la - 192)) & ~0x3f) == 0 && ((1L << (_la - 192)) & -36169535635589505L) != 0) || ((((_la - 256)) & ~0x3f) == 0 && ((1L << (_la - 256)) & 18014398509449215L) != 0)) {
				{
				setState(799);
				symbolicNameString();
				setState(800);
				match(EQ);
				}
			}

			setState(804);
			insertNodePattern();
			setState(810);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==LT || _la==MINUS || _la==ARROW_LINE || _la==ARROW_LEFT_HEAD) {
				{
				{
				setState(805);
				insertRelationshipPattern();
				setState(806);
				insertNodePattern();
				}
				}
				setState(812);
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
			setState(827);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,72,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(813);
				match(LCURLY);
				setState(814);
				match(UNSIGNED_DECIMAL_INTEGER);
				setState(815);
				match(RCURLY);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(816);
				match(LCURLY);
				setState(818);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==UNSIGNED_DECIMAL_INTEGER) {
					{
					setState(817);
					((QuantifierContext)_localctx).from = match(UNSIGNED_DECIMAL_INTEGER);
					}
				}

				setState(820);
				match(COMMA);
				setState(822);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==UNSIGNED_DECIMAL_INTEGER) {
					{
					setState(821);
					((QuantifierContext)_localctx).to = match(UNSIGNED_DECIMAL_INTEGER);
					}
				}

				setState(824);
				match(RCURLY);
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(825);
				match(PLUS);
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(826);
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
			setState(831);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case ALL_SHORTEST_PATHS:
			case SHORTEST_PATH:
				enterOuterAlt(_localctx, 1);
				{
				setState(829);
				shortestPathPattern();
				}
				break;
			case LPAREN:
				enterOuterAlt(_localctx, 2);
				{
				setState(830);
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
			setState(833);
			_la = _input.LA(1);
			if ( !(_la==ALL_SHORTEST_PATHS || _la==SHORTEST_PATH) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			setState(834);
			match(LPAREN);
			setState(835);
			patternElement();
			setState(836);
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
			setState(851); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				setState(851);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,76,_ctx) ) {
				case 1:
					{
					setState(838);
					nodePattern();
					setState(847);
					_errHandler.sync(this);
					_la = _input.LA(1);
					while (_la==LT || _la==MINUS || _la==ARROW_LINE || _la==ARROW_LEFT_HEAD) {
						{
						{
						setState(839);
						relationshipPattern();
						setState(841);
						_errHandler.sync(this);
						_la = _input.LA(1);
						if (_la==LCURLY || _la==PLUS || _la==TIMES) {
							{
							setState(840);
							quantifier();
							}
						}

						setState(843);
						nodePattern();
						}
						}
						setState(849);
						_errHandler.sync(this);
						_la = _input.LA(1);
					}
					}
					break;
				case 2:
					{
					setState(850);
					parenthesizedPath();
					}
					break;
				}
				}
				setState(853); 
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
			setState(889);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,86,_ctx) ) {
			case 1:
				_localctx = new AnyShortestPathContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(855);
				match(ANY);
				setState(856);
				match(SHORTEST);
				setState(858);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==PATH || _la==PATHS) {
					{
					setState(857);
					pathToken();
					}
				}

				}
				break;
			case 2:
				_localctx = new AllShortestPathContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(860);
				match(ALL);
				setState(861);
				match(SHORTEST);
				setState(863);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==PATH || _la==PATHS) {
					{
					setState(862);
					pathToken();
					}
				}

				}
				break;
			case 3:
				_localctx = new AnyPathContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(865);
				match(ANY);
				setState(867);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==UNSIGNED_DECIMAL_INTEGER) {
					{
					setState(866);
					match(UNSIGNED_DECIMAL_INTEGER);
					}
				}

				setState(870);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==PATH || _la==PATHS) {
					{
					setState(869);
					pathToken();
					}
				}

				}
				break;
			case 4:
				_localctx = new AllPathContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(872);
				match(ALL);
				setState(874);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==PATH || _la==PATHS) {
					{
					setState(873);
					pathToken();
					}
				}

				}
				break;
			case 5:
				_localctx = new ShortestGroupContext(_localctx);
				enterOuterAlt(_localctx, 5);
				{
				setState(876);
				match(SHORTEST);
				setState(878);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==UNSIGNED_DECIMAL_INTEGER) {
					{
					setState(877);
					match(UNSIGNED_DECIMAL_INTEGER);
					}
				}

				setState(881);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==PATH || _la==PATHS) {
					{
					setState(880);
					pathToken();
					}
				}

				setState(883);
				groupToken();
				}
				break;
			case 6:
				_localctx = new AnyShortestPathContext(_localctx);
				enterOuterAlt(_localctx, 6);
				{
				setState(884);
				match(SHORTEST);
				setState(885);
				match(UNSIGNED_DECIMAL_INTEGER);
				setState(887);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==PATH || _la==PATHS) {
					{
					setState(886);
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
			setState(891);
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
			setState(893);
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
			setState(895);
			nodePattern();
			setState(899); 
			_errHandler.sync(this);
			_alt = 1;
			do {
				switch (_alt) {
				case 1:
					{
					{
					setState(896);
					relationshipPattern();
					setState(897);
					nodePattern();
					}
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(901); 
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
			setState(903);
			match(LPAREN);
			setState(905);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,88,_ctx) ) {
			case 1:
				{
				setState(904);
				variable();
				}
				break;
			}
			setState(908);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==COLON || _la==IS) {
				{
				setState(907);
				labelExpression();
				}
			}

			setState(911);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==DOLLAR || _la==LCURLY) {
				{
				setState(910);
				properties();
				}
			}

			setState(915);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WHERE) {
				{
				setState(913);
				match(WHERE);
				setState(914);
				expression();
				}
			}

			setState(917);
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
			setState(919);
			match(LPAREN);
			setState(921);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,92,_ctx) ) {
			case 1:
				{
				setState(920);
				variable();
				}
				break;
			}
			setState(924);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==COLON || _la==IS) {
				{
				setState(923);
				insertNodeLabelExpression();
				}
			}

			setState(927);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LCURLY) {
				{
				setState(926);
				map();
				}
			}

			setState(929);
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
			setState(931);
			match(LPAREN);
			setState(932);
			pattern();
			setState(935);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WHERE) {
				{
				setState(933);
				match(WHERE);
				setState(934);
				expression();
				}
			}

			setState(937);
			match(RPAREN);
			setState(939);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LCURLY || _la==PLUS || _la==TIMES) {
				{
				setState(938);
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
			setState(943); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				setState(943);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,97,_ctx) ) {
				case 1:
					{
					setState(941);
					labelType();
					}
					break;
				case 2:
					{
					setState(942);
					dynamicLabelType();
					}
					break;
				}
				}
				setState(945); 
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
			setState(947);
			match(IS);
			setState(950);
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
				setState(948);
				symbolicNameString();
				}
				break;
			case DOLLAR:
				{
				setState(949);
				dynamicExpression();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(956);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COLON) {
				{
				setState(954);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,100,_ctx) ) {
				case 1:
					{
					setState(952);
					labelType();
					}
					break;
				case 2:
					{
					setState(953);
					dynamicLabelType();
					}
					break;
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
			setState(959);
			match(DOLLAR);
			setState(960);
			match(LPAREN);
			setState(961);
			expression();
			setState(962);
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
			setState(964);
			match(DOLLAR);
			setState(966);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==ALL || _la==ANY) {
				{
				setState(965);
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

			setState(968);
			match(LPAREN);
			setState(969);
			expression();
			setState(970);
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
			setState(972);
			match(COLON);
			setState(973);
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
			setState(975);
			match(COLON);
			setState(976);
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
			setState(978);
			match(COLON);
			setState(979);
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
			setState(981);
			match(COLON);
			setState(982);
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
			setState(986);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case LCURLY:
				enterOuterAlt(_localctx, 1);
				{
				setState(984);
				map();
				}
				break;
			case DOLLAR:
				enterOuterAlt(_localctx, 2);
				{
				setState(985);
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
			setState(989);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LT || _la==ARROW_LEFT_HEAD) {
				{
				setState(988);
				leftArrow();
				}
			}

			setState(991);
			arrowLine();
			setState(1010);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LBRACKET) {
				{
				setState(992);
				match(LBRACKET);
				setState(994);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,105,_ctx) ) {
				case 1:
					{
					setState(993);
					variable();
					}
					break;
				}
				setState(997);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==COLON || _la==IS) {
					{
					setState(996);
					labelExpression();
					}
				}

				setState(1000);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==TIMES) {
					{
					setState(999);
					pathLength();
					}
				}

				setState(1003);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==DOLLAR || _la==LCURLY) {
					{
					setState(1002);
					properties();
					}
				}

				setState(1007);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==WHERE) {
					{
					setState(1005);
					match(WHERE);
					setState(1006);
					expression();
					}
				}

				setState(1009);
				match(RBRACKET);
				}
			}

			setState(1012);
			arrowLine();
			setState(1014);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==GT || _la==ARROW_RIGHT_HEAD) {
				{
				setState(1013);
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
			setState(1017);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LT || _la==ARROW_LEFT_HEAD) {
				{
				setState(1016);
				leftArrow();
				}
			}

			setState(1019);
			arrowLine();
			setState(1020);
			match(LBRACKET);
			setState(1022);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,113,_ctx) ) {
			case 1:
				{
				setState(1021);
				variable();
				}
				break;
			}
			setState(1024);
			insertRelationshipLabelExpression();
			setState(1026);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LCURLY) {
				{
				setState(1025);
				map();
				}
			}

			setState(1028);
			match(RBRACKET);
			setState(1029);
			arrowLine();
			setState(1031);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==GT || _la==ARROW_RIGHT_HEAD) {
				{
				setState(1030);
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
			setState(1033);
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
			setState(1035);
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
			setState(1037);
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
			setState(1039);
			match(TIMES);
			setState(1048);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,118,_ctx) ) {
			case 1:
				{
				setState(1041);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==UNSIGNED_DECIMAL_INTEGER) {
					{
					setState(1040);
					((PathLengthContext)_localctx).from = match(UNSIGNED_DECIMAL_INTEGER);
					}
				}

				setState(1043);
				match(DOTDOT);
				setState(1045);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==UNSIGNED_DECIMAL_INTEGER) {
					{
					setState(1044);
					((PathLengthContext)_localctx).to = match(UNSIGNED_DECIMAL_INTEGER);
					}
				}

				}
				break;
			case 2:
				{
				setState(1047);
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
			setState(1054);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case COLON:
				enterOuterAlt(_localctx, 1);
				{
				setState(1050);
				match(COLON);
				setState(1051);
				labelExpression4();
				}
				break;
			case IS:
				enterOuterAlt(_localctx, 2);
				{
				setState(1052);
				match(IS);
				setState(1053);
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
			setState(1056);
			labelExpression3();
			setState(1064);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,121,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(1057);
					match(BAR);
					setState(1059);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==COLON) {
						{
						setState(1058);
						match(COLON);
						}
					}

					setState(1061);
					labelExpression3();
					}
					} 
				}
				setState(1066);
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
			setState(1067);
			labelExpression3Is();
			setState(1075);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,123,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(1068);
					match(BAR);
					setState(1070);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==COLON) {
						{
						setState(1069);
						match(COLON);
						}
					}

					setState(1072);
					labelExpression3Is();
					}
					} 
				}
				setState(1077);
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
			setState(1078);
			labelExpression2();
			setState(1083);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,124,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(1079);
					_la = _input.LA(1);
					if ( !(_la==COLON || _la==AMPERSAND) ) {
					_errHandler.recoverInline(this);
					}
					else {
						if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
						_errHandler.reportMatch(this);
						consume();
					}
					setState(1080);
					labelExpression2();
					}
					} 
				}
				setState(1085);
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
			setState(1086);
			labelExpression2Is();
			setState(1091);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,125,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(1087);
					_la = _input.LA(1);
					if ( !(_la==COLON || _la==AMPERSAND) ) {
					_errHandler.recoverInline(this);
					}
					else {
						if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
						_errHandler.reportMatch(this);
						consume();
					}
					setState(1088);
					labelExpression2Is();
					}
					} 
				}
				setState(1093);
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
			setState(1097);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==EXCLAMATION_MARK) {
				{
				{
				setState(1094);
				match(EXCLAMATION_MARK);
				}
				}
				setState(1099);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(1100);
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
			setState(1105);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==EXCLAMATION_MARK) {
				{
				{
				setState(1102);
				match(EXCLAMATION_MARK);
				}
				}
				setState(1107);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(1108);
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
			setState(1117);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case LPAREN:
				_localctx = new ParenthesizedLabelExpressionContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(1110);
				match(LPAREN);
				setState(1111);
				labelExpression4();
				setState(1112);
				match(RPAREN);
				}
				break;
			case PERCENT:
				_localctx = new AnyLabelContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(1114);
				match(PERCENT);
				}
				break;
			case DOLLAR:
				_localctx = new DynamicLabelContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(1115);
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
				setState(1116);
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
			setState(1126);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case LPAREN:
				_localctx = new ParenthesizedLabelExpressionIsContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(1119);
				match(LPAREN);
				setState(1120);
				labelExpression4Is();
				setState(1121);
				match(RPAREN);
				}
				break;
			case PERCENT:
				_localctx = new AnyLabelIsContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(1123);
				match(PERCENT);
				}
				break;
			case DOLLAR:
				_localctx = new DynamicLabelIsContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(1124);
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
				setState(1125);
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
			setState(1128);
			_la = _input.LA(1);
			if ( !(_la==COLON || _la==IS) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			setState(1129);
			symbolicNameString();
			setState(1134);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COLON || _la==AMPERSAND) {
				{
				{
				setState(1130);
				_la = _input.LA(1);
				if ( !(_la==COLON || _la==AMPERSAND) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(1131);
				symbolicNameString();
				}
				}
				setState(1136);
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
			setState(1137);
			_la = _input.LA(1);
			if ( !(_la==COLON || _la==IS) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			setState(1138);
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
			setState(1140);
			expression11();
			setState(1145);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==OR) {
				{
				{
				setState(1141);
				match(OR);
				setState(1142);
				expression11();
				}
				}
				setState(1147);
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
			setState(1148);
			expression10();
			setState(1153);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==XOR) {
				{
				{
				setState(1149);
				match(XOR);
				setState(1150);
				expression10();
				}
				}
				setState(1155);
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
			setState(1156);
			expression9();
			setState(1161);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AND) {
				{
				{
				setState(1157);
				match(AND);
				setState(1158);
				expression9();
				}
				}
				setState(1163);
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
			setState(1167);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,134,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(1164);
					match(NOT);
					}
					} 
				}
				setState(1169);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,134,_ctx);
			}
			setState(1170);
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
			setState(1172);
			expression7();
			setState(1177);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (((((_la - 100)) & ~0x3f) == 0 && ((1L << (_la - 100)) & -9151032967823097855L) != 0) || _la==NEQ) {
				{
				{
				setState(1173);
				_la = _input.LA(1);
				if ( !(((((_la - 100)) & ~0x3f) == 0 && ((1L << (_la - 100)) & -9151032967823097855L) != 0) || _la==NEQ) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(1174);
				expression7();
				}
				}
				setState(1179);
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
			setState(1180);
			expression6();
			setState(1182);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==COLONCOLON || _la==CONTAINS || ((((_la - 99)) & ~0x3f) == 0 && ((1L << (_la - 99)) & 1103806595073L) != 0) || _la==REGEQ || _la==STARTS) {
				{
				setState(1181);
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
			setState(1216);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,143,_ctx) ) {
			case 1:
				_localctx = new StringAndListComparisonContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(1191);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case REGEQ:
					{
					setState(1184);
					match(REGEQ);
					}
					break;
				case STARTS:
					{
					setState(1185);
					match(STARTS);
					setState(1186);
					match(WITH);
					}
					break;
				case ENDS:
					{
					setState(1187);
					match(ENDS);
					setState(1188);
					match(WITH);
					}
					break;
				case CONTAINS:
					{
					setState(1189);
					match(CONTAINS);
					}
					break;
				case IN:
					{
					setState(1190);
					match(IN);
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(1193);
				expression6();
				}
				break;
			case 2:
				_localctx = new NullComparisonContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(1194);
				match(IS);
				setState(1196);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==NOT) {
					{
					setState(1195);
					match(NOT);
					}
				}

				setState(1198);
				match(NULL);
				}
				break;
			case 3:
				_localctx = new TypeComparisonContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(1205);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case IS:
					{
					setState(1199);
					match(IS);
					setState(1201);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==NOT) {
						{
						setState(1200);
						match(NOT);
						}
					}

					setState(1203);
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
					setState(1204);
					match(COLONCOLON);
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(1207);
				type();
				}
				break;
			case 4:
				_localctx = new NormalFormComparisonContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(1208);
				match(IS);
				setState(1210);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==NOT) {
					{
					setState(1209);
					match(NOT);
					}
				}

				setState(1213);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (((((_la - 168)) & ~0x3f) == 0 && ((1L << (_la - 168)) & 15L) != 0)) {
					{
					setState(1212);
					normalForm();
					}
				}

				setState(1215);
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
			setState(1218);
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
			setState(1220);
			expression5();
			setState(1225);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==DOUBLEBAR || _la==MINUS || _la==PLUS) {
				{
				{
				setState(1221);
				_la = _input.LA(1);
				if ( !(_la==DOUBLEBAR || _la==MINUS || _la==PLUS) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(1222);
				expression5();
				}
				}
				setState(1227);
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
			setState(1228);
			expression4();
			setState(1233);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==DIVIDE || _la==PERCENT || _la==TIMES) {
				{
				{
				setState(1229);
				_la = _input.LA(1);
				if ( !(_la==DIVIDE || _la==PERCENT || _la==TIMES) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(1230);
				expression4();
				}
				}
				setState(1235);
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
			setState(1236);
			expression3();
			setState(1241);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==POW) {
				{
				{
				setState(1237);
				match(POW);
				setState(1238);
				expression3();
				}
				}
				setState(1243);
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
			setState(1247);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,147,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1244);
				expression2();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1245);
				_la = _input.LA(1);
				if ( !(_la==MINUS || _la==PLUS) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(1246);
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
			setState(1249);
			expression1();
			setState(1253);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,148,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(1250);
					postFix();
					}
					} 
				}
				setState(1255);
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
			setState(1271);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,151,_ctx) ) {
			case 1:
				_localctx = new PropertyPostfixContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(1256);
				property();
				}
				break;
			case 2:
				_localctx = new LabelPostfixContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(1257);
				labelExpression();
				}
				break;
			case 3:
				_localctx = new IndexPostfixContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(1258);
				match(LBRACKET);
				setState(1259);
				expression();
				setState(1260);
				match(RBRACKET);
				}
				break;
			case 4:
				_localctx = new RangePostfixContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(1262);
				match(LBRACKET);
				setState(1264);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & -985163492229136L) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & -1170935971839737857L) != 0) || ((((_la - 128)) & ~0x3f) == 0 && ((1L << (_la - 128)) & -120528764929L) != 0) || ((((_la - 192)) & ~0x3f) == 0 && ((1L << (_la - 192)) & -36169535635589377L) != 0) || ((((_la - 256)) & ~0x3f) == 0 && ((1L << (_la - 256)) & 18014398509449215L) != 0)) {
					{
					setState(1263);
					((RangePostfixContext)_localctx).fromExp = expression();
					}
				}

				setState(1266);
				match(DOTDOT);
				setState(1268);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & -985163492229136L) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & -1170935971839737857L) != 0) || ((((_la - 128)) & ~0x3f) == 0 && ((1L << (_la - 128)) & -120528764929L) != 0) || ((((_la - 192)) & ~0x3f) == 0 && ((1L << (_la - 192)) & -36169535635589377L) != 0) || ((((_la - 256)) & ~0x3f) == 0 && ((1L << (_la - 256)) & 18014398509449215L) != 0)) {
					{
					setState(1267);
					((RangePostfixContext)_localctx).toExp = expression();
					}
				}

				setState(1270);
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
			setState(1273);
			match(DOT);
			setState(1274);
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
			setState(1276);
			match(LBRACKET);
			setState(1277);
			expression();
			setState(1278);
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
			setState(1280);
			expression1();
			setState(1282); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(1281);
				property();
				}
				}
				setState(1284); 
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
			setState(1286);
			expression1();
			setState(1287);
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
			setState(1310);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,153,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1289);
				literal();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1290);
				parameter("ANY");
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(1291);
				caseExpression();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(1292);
				extendedCaseExpression();
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(1293);
				countStar();
				}
				break;
			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(1294);
				existsExpression();
				}
				break;
			case 7:
				enterOuterAlt(_localctx, 7);
				{
				setState(1295);
				countExpression();
				}
				break;
			case 8:
				enterOuterAlt(_localctx, 8);
				{
				setState(1296);
				collectExpression();
				}
				break;
			case 9:
				enterOuterAlt(_localctx, 9);
				{
				setState(1297);
				mapProjection();
				}
				break;
			case 10:
				enterOuterAlt(_localctx, 10);
				{
				setState(1298);
				listComprehension();
				}
				break;
			case 11:
				enterOuterAlt(_localctx, 11);
				{
				setState(1299);
				listLiteral();
				}
				break;
			case 12:
				enterOuterAlt(_localctx, 12);
				{
				setState(1300);
				patternComprehension();
				}
				break;
			case 13:
				enterOuterAlt(_localctx, 13);
				{
				setState(1301);
				reduceExpression();
				}
				break;
			case 14:
				enterOuterAlt(_localctx, 14);
				{
				setState(1302);
				listItemsPredicate();
				}
				break;
			case 15:
				enterOuterAlt(_localctx, 15);
				{
				setState(1303);
				normalizeFunction();
				}
				break;
			case 16:
				enterOuterAlt(_localctx, 16);
				{
				setState(1304);
				trimFunction();
				}
				break;
			case 17:
				enterOuterAlt(_localctx, 17);
				{
				setState(1305);
				patternExpression();
				}
				break;
			case 18:
				enterOuterAlt(_localctx, 18);
				{
				setState(1306);
				shortestPathExpression();
				}
				break;
			case 19:
				enterOuterAlt(_localctx, 19);
				{
				setState(1307);
				parenthesizedExpression();
				}
				break;
			case 20:
				enterOuterAlt(_localctx, 20);
				{
				setState(1308);
				functionInvocation();
				}
				break;
			case 21:
				enterOuterAlt(_localctx, 21);
				{
				setState(1309);
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
			setState(1321);
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
				setState(1312);
				numberLiteral();
				}
				break;
			case STRING_LITERAL1:
			case STRING_LITERAL2:
				_localctx = new StringsLiteralContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(1313);
				stringLiteral();
				}
				break;
			case LCURLY:
				_localctx = new OtherLiteralContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(1314);
				map();
				}
				break;
			case TRUE:
				_localctx = new BooleanLiteralContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(1315);
				match(TRUE);
				}
				break;
			case FALSE:
				_localctx = new BooleanLiteralContext(_localctx);
				enterOuterAlt(_localctx, 5);
				{
				setState(1316);
				match(FALSE);
				}
				break;
			case INF:
				_localctx = new KeywordLiteralContext(_localctx);
				enterOuterAlt(_localctx, 6);
				{
				setState(1317);
				match(INF);
				}
				break;
			case INFINITY:
				_localctx = new KeywordLiteralContext(_localctx);
				enterOuterAlt(_localctx, 7);
				{
				setState(1318);
				match(INFINITY);
				}
				break;
			case NAN:
				_localctx = new KeywordLiteralContext(_localctx);
				enterOuterAlt(_localctx, 8);
				{
				setState(1319);
				match(NAN);
				}
				break;
			case NULL:
				_localctx = new KeywordLiteralContext(_localctx);
				enterOuterAlt(_localctx, 9);
				{
				setState(1320);
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
			setState(1323);
			match(CASE);
			setState(1325); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(1324);
				caseAlternative();
				}
				}
				setState(1327); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==WHEN );
			setState(1331);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==ELSE) {
				{
				setState(1329);
				match(ELSE);
				setState(1330);
				expression();
				}
			}

			setState(1333);
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
			setState(1335);
			match(WHEN);
			setState(1336);
			expression();
			setState(1337);
			match(THEN);
			setState(1338);
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
			setState(1340);
			match(CASE);
			setState(1341);
			expression();
			setState(1343); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(1342);
				extendedCaseAlternative();
				}
				}
				setState(1345); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==WHEN );
			setState(1349);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==ELSE) {
				{
				setState(1347);
				match(ELSE);
				setState(1348);
				((ExtendedCaseExpressionContext)_localctx).elseExp = expression();
				}
			}

			setState(1351);
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
			setState(1353);
			match(WHEN);
			setState(1354);
			extendedWhen();
			setState(1359);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(1355);
				match(COMMA);
				setState(1356);
				extendedWhen();
				}
				}
				setState(1361);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(1362);
			match(THEN);
			setState(1363);
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
			setState(1398);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,166,_ctx) ) {
			case 1:
				_localctx = new WhenStringOrListContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(1370);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case REGEQ:
					{
					setState(1365);
					match(REGEQ);
					}
					break;
				case STARTS:
					{
					setState(1366);
					match(STARTS);
					setState(1367);
					match(WITH);
					}
					break;
				case ENDS:
					{
					setState(1368);
					match(ENDS);
					setState(1369);
					match(WITH);
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(1372);
				expression6();
				}
				break;
			case 2:
				_localctx = new WhenNullContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(1373);
				match(IS);
				setState(1375);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==NOT) {
					{
					setState(1374);
					match(NOT);
					}
				}

				setState(1377);
				match(NULL);
				}
				break;
			case 3:
				_localctx = new WhenTypeContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(1384);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case IS:
					{
					setState(1378);
					match(IS);
					setState(1380);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==NOT) {
						{
						setState(1379);
						match(NOT);
						}
					}

					setState(1382);
					match(TYPED);
					}
					break;
				case COLONCOLON:
					{
					setState(1383);
					match(COLONCOLON);
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(1386);
				type();
				}
				break;
			case 4:
				_localctx = new WhenFormContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(1387);
				match(IS);
				setState(1389);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==NOT) {
					{
					setState(1388);
					match(NOT);
					}
				}

				setState(1392);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (((((_la - 168)) & ~0x3f) == 0 && ((1L << (_la - 168)) & 15L) != 0)) {
					{
					setState(1391);
					normalForm();
					}
				}

				setState(1394);
				match(NORMALIZED);
				}
				break;
			case 5:
				_localctx = new WhenComparatorContext(_localctx);
				enterOuterAlt(_localctx, 5);
				{
				setState(1395);
				_la = _input.LA(1);
				if ( !(((((_la - 100)) & ~0x3f) == 0 && ((1L << (_la - 100)) & -9151032967823097855L) != 0) || _la==NEQ) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(1396);
				expression7();
				}
				break;
			case 6:
				_localctx = new WhenEqualsContext(_localctx);
				enterOuterAlt(_localctx, 6);
				{
				setState(1397);
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
			setState(1400);
			match(LBRACKET);
			setState(1401);
			variable();
			setState(1402);
			match(IN);
			setState(1403);
			expression();
			setState(1414);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,169,_ctx) ) {
			case 1:
				{
				setState(1406);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==WHERE) {
					{
					setState(1404);
					match(WHERE);
					setState(1405);
					((ListComprehensionContext)_localctx).whereExp = expression();
					}
				}

				setState(1408);
				match(BAR);
				setState(1409);
				((ListComprehensionContext)_localctx).barExp = expression();
				}
				break;
			case 2:
				{
				setState(1412);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==WHERE) {
					{
					setState(1410);
					match(WHERE);
					setState(1411);
					((ListComprehensionContext)_localctx).whereExp = expression();
					}
				}

				}
				break;
			}
			setState(1416);
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
			setState(1418);
			match(LBRACKET);
			setState(1422);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & -985163492230144L) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & -1170935971839803393L) != 0) || ((((_la - 128)) & ~0x3f) == 0 && ((1L << (_la - 128)) & -129253703681L) != 0) || ((((_la - 192)) & ~0x3f) == 0 && ((1L << (_la - 192)) & -36169535635589505L) != 0) || ((((_la - 256)) & ~0x3f) == 0 && ((1L << (_la - 256)) & 18014398509449215L) != 0)) {
				{
				setState(1419);
				variable();
				setState(1420);
				match(EQ);
				}
			}

			setState(1424);
			pathPatternNonEmpty();
			setState(1427);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WHERE) {
				{
				setState(1425);
				match(WHERE);
				setState(1426);
				((PatternComprehensionContext)_localctx).whereExp = expression();
				}
			}

			setState(1429);
			match(BAR);
			setState(1430);
			((PatternComprehensionContext)_localctx).barExp = expression();
			setState(1431);
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
			setState(1433);
			match(REDUCE);
			setState(1434);
			match(LPAREN);
			setState(1435);
			variable();
			setState(1436);
			match(EQ);
			setState(1437);
			expression();
			setState(1438);
			match(COMMA);
			setState(1439);
			variable();
			setState(1440);
			match(IN);
			setState(1441);
			expression();
			setState(1442);
			match(BAR);
			setState(1443);
			expression();
			setState(1444);
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
			setState(1446);
			_la = _input.LA(1);
			if ( !(_la==ALL || _la==ANY || _la==NONE || _la==SINGLE) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			setState(1447);
			match(LPAREN);
			setState(1448);
			variable();
			setState(1449);
			match(IN);
			setState(1450);
			((ListItemsPredicateContext)_localctx).inExp = expression();
			setState(1453);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WHERE) {
				{
				setState(1451);
				match(WHERE);
				setState(1452);
				((ListItemsPredicateContext)_localctx).whereExp = expression();
				}
			}

			setState(1455);
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
			setState(1457);
			match(NORMALIZE);
			setState(1458);
			match(LPAREN);
			setState(1459);
			expression();
			setState(1462);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==COMMA) {
				{
				setState(1460);
				match(COMMA);
				setState(1461);
				normalForm();
				}
			}

			setState(1464);
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
			setState(1466);
			match(TRIM);
			setState(1467);
			match(LPAREN);
			setState(1475);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,176,_ctx) ) {
			case 1:
				{
				setState(1469);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,174,_ctx) ) {
				case 1:
					{
					setState(1468);
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
				setState(1472);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,175,_ctx) ) {
				case 1:
					{
					setState(1471);
					((TrimFunctionContext)_localctx).trimCharacterString = expression();
					}
					break;
				}
				setState(1474);
				match(FROM);
				}
				break;
			}
			setState(1477);
			((TrimFunctionContext)_localctx).trimSource = expression();
			setState(1478);
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
			setState(1480);
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
			setState(1482);
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
			setState(1484);
			match(LPAREN);
			setState(1485);
			expression();
			setState(1486);
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
			setState(1488);
			variable();
			setState(1489);
			match(LCURLY);
			setState(1498);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & -985163492230144L) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & -1170935971839279105L) != 0) || ((((_la - 128)) & ~0x3f) == 0 && ((1L << (_la - 128)) & -129253703681L) != 0) || ((((_la - 192)) & ~0x3f) == 0 && ((1L << (_la - 192)) & -36169535635589505L) != 0) || ((((_la - 256)) & ~0x3f) == 0 && ((1L << (_la - 256)) & 18014398509449215L) != 0)) {
				{
				setState(1490);
				mapProjectionElement();
				setState(1495);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(1491);
					match(COMMA);
					setState(1492);
					mapProjectionElement();
					}
					}
					setState(1497);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
			}

			setState(1500);
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
			setState(1510);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,179,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1502);
				propertyKeyName();
				setState(1503);
				match(COLON);
				setState(1504);
				expression();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1506);
				property();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(1507);
				variable();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(1508);
				match(DOT);
				setState(1509);
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
			setState(1512);
			match(COUNT);
			setState(1513);
			match(LPAREN);
			setState(1514);
			match(TIMES);
			setState(1515);
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
			setState(1517);
			match(EXISTS);
			setState(1518);
			match(LCURLY);
			setState(1527);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,182,_ctx) ) {
			case 1:
				{
				setState(1519);
				regularQuery();
				}
				break;
			case 2:
				{
				setState(1521);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,180,_ctx) ) {
				case 1:
					{
					setState(1520);
					matchMode();
					}
					break;
				}
				setState(1523);
				patternList();
				setState(1525);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==WHERE) {
					{
					setState(1524);
					whereClause();
					}
				}

				}
				break;
			}
			setState(1529);
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
			setState(1531);
			match(COUNT);
			setState(1532);
			match(LCURLY);
			setState(1541);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,185,_ctx) ) {
			case 1:
				{
				setState(1533);
				regularQuery();
				}
				break;
			case 2:
				{
				setState(1535);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,183,_ctx) ) {
				case 1:
					{
					setState(1534);
					matchMode();
					}
					break;
				}
				setState(1537);
				patternList();
				setState(1539);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==WHERE) {
					{
					setState(1538);
					whereClause();
					}
				}

				}
				break;
			}
			setState(1543);
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
			setState(1545);
			match(COLLECT);
			setState(1546);
			match(LCURLY);
			setState(1547);
			regularQuery();
			setState(1548);
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
			setState(1551);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==MINUS) {
				{
				setState(1550);
				match(MINUS);
				}
			}

			setState(1553);
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
			setState(1556);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==MINUS) {
				{
				setState(1555);
				match(MINUS);
				}
			}

			setState(1558);
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
			setState(1560);
			match(LBRACKET);
			setState(1569);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & -985163492229136L) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & -1170935971839737857L) != 0) || ((((_la - 128)) & ~0x3f) == 0 && ((1L << (_la - 128)) & -120528764929L) != 0) || ((((_la - 192)) & ~0x3f) == 0 && ((1L << (_la - 192)) & -36169535635589377L) != 0) || ((((_la - 256)) & ~0x3f) == 0 && ((1L << (_la - 256)) & 18014398509449215L) != 0)) {
				{
				setState(1561);
				expression();
				setState(1566);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(1562);
					match(COMMA);
					setState(1563);
					expression();
					}
					}
					setState(1568);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
			}

			setState(1571);
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
			setState(1573);
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
			setState(1575);
			match(DOLLAR);
			setState(1576);
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
			setState(1580);
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
				setState(1578);
				symbolicNameString();
				}
				break;
			case UNSIGNED_DECIMAL_INTEGER:
				{
				setState(1579);
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
			setState(1582);
			functionName();
			setState(1583);
			match(LPAREN);
			setState(1585);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,191,_ctx) ) {
			case 1:
				{
				setState(1584);
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
			setState(1595);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & -985163492229136L) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & -1170935971839737857L) != 0) || ((((_la - 128)) & ~0x3f) == 0 && ((1L << (_la - 128)) & -120528764929L) != 0) || ((((_la - 192)) & ~0x3f) == 0 && ((1L << (_la - 192)) & -36169535635589377L) != 0) || ((((_la - 256)) & ~0x3f) == 0 && ((1L << (_la - 256)) & 18014398509449215L) != 0)) {
				{
				setState(1587);
				functionArgument();
				setState(1592);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(1588);
					match(COMMA);
					setState(1589);
					functionArgument();
					}
					}
					setState(1594);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
			}

			setState(1597);
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
			setState(1599);
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
			setState(1601);
			namespace();
			setState(1602);
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
			setState(1609);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,194,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(1604);
					symbolicNameString();
					setState(1605);
					match(DOT);
					}
					} 
				}
				setState(1611);
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
			setState(1612);
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
			setState(1614);
			symbolicNameString();
			setState(1619);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(1615);
				match(COMMA);
				setState(1616);
				symbolicNameString();
				}
				}
				setState(1621);
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
			setState(1622);
			typePart();
			setState(1627);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,196,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(1623);
					match(BAR);
					setState(1624);
					typePart();
					}
					} 
				}
				setState(1629);
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
			setState(1630);
			typeName();
			setState(1632);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==EXCLAMATION_MARK || _la==NOT) {
				{
				setState(1631);
				typeNullability();
				}
			}

			setState(1637);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==ARRAY || _la==LIST) {
				{
				{
				setState(1634);
				typeListSuffix();
				}
				}
				setState(1639);
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
			setState(1705);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case NOTHING:
				enterOuterAlt(_localctx, 1);
				{
				setState(1640);
				match(NOTHING);
				}
				break;
			case NULL:
				enterOuterAlt(_localctx, 2);
				{
				setState(1641);
				match(NULL);
				}
				break;
			case BOOL:
				enterOuterAlt(_localctx, 3);
				{
				setState(1642);
				match(BOOL);
				}
				break;
			case BOOLEAN:
				enterOuterAlt(_localctx, 4);
				{
				setState(1643);
				match(BOOLEAN);
				}
				break;
			case VARCHAR:
				enterOuterAlt(_localctx, 5);
				{
				setState(1644);
				match(VARCHAR);
				}
				break;
			case STRING:
				enterOuterAlt(_localctx, 6);
				{
				setState(1645);
				match(STRING);
				}
				break;
			case INT:
				enterOuterAlt(_localctx, 7);
				{
				setState(1646);
				match(INT);
				}
				break;
			case INTEGER:
			case SIGNED:
				enterOuterAlt(_localctx, 8);
				{
				setState(1648);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==SIGNED) {
					{
					setState(1647);
					match(SIGNED);
					}
				}

				setState(1650);
				match(INTEGER);
				}
				break;
			case FLOAT:
				enterOuterAlt(_localctx, 9);
				{
				setState(1651);
				match(FLOAT);
				}
				break;
			case DATE:
				enterOuterAlt(_localctx, 10);
				{
				setState(1652);
				match(DATE);
				}
				break;
			case LOCAL:
				enterOuterAlt(_localctx, 11);
				{
				setState(1653);
				match(LOCAL);
				setState(1654);
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
				setState(1655);
				match(ZONED);
				setState(1656);
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
				setState(1657);
				match(TIME);
				setState(1658);
				_la = _input.LA(1);
				if ( !(_la==WITH || _la==WITHOUT) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(1662);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case TIMEZONE:
					{
					setState(1659);
					match(TIMEZONE);
					}
					break;
				case TIME:
					{
					setState(1660);
					match(TIME);
					setState(1661);
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
				setState(1664);
				match(TIMESTAMP);
				setState(1665);
				_la = _input.LA(1);
				if ( !(_la==WITH || _la==WITHOUT) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(1669);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case TIMEZONE:
					{
					setState(1666);
					match(TIMEZONE);
					}
					break;
				case TIME:
					{
					setState(1667);
					match(TIME);
					setState(1668);
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
				setState(1671);
				match(DURATION);
				}
				break;
			case POINT:
				enterOuterAlt(_localctx, 16);
				{
				setState(1672);
				match(POINT);
				}
				break;
			case NODE:
				enterOuterAlt(_localctx, 17);
				{
				setState(1673);
				match(NODE);
				}
				break;
			case VERTEX:
				enterOuterAlt(_localctx, 18);
				{
				setState(1674);
				match(VERTEX);
				}
				break;
			case RELATIONSHIP:
				enterOuterAlt(_localctx, 19);
				{
				setState(1675);
				match(RELATIONSHIP);
				}
				break;
			case EDGE:
				enterOuterAlt(_localctx, 20);
				{
				setState(1676);
				match(EDGE);
				}
				break;
			case MAP:
				enterOuterAlt(_localctx, 21);
				{
				setState(1677);
				match(MAP);
				}
				break;
			case ARRAY:
			case LIST:
				enterOuterAlt(_localctx, 22);
				{
				setState(1678);
				_la = _input.LA(1);
				if ( !(_la==ARRAY || _la==LIST) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(1679);
				match(LT);
				setState(1680);
				type();
				setState(1681);
				match(GT);
				}
				break;
			case PATH:
				enterOuterAlt(_localctx, 23);
				{
				setState(1683);
				match(PATH);
				}
				break;
			case PATHS:
				enterOuterAlt(_localctx, 24);
				{
				setState(1684);
				match(PATHS);
				}
				break;
			case PROPERTY:
				enterOuterAlt(_localctx, 25);
				{
				setState(1685);
				match(PROPERTY);
				setState(1686);
				match(VALUE);
				}
				break;
			case ANY:
				enterOuterAlt(_localctx, 26);
				{
				setState(1687);
				match(ANY);
				setState(1703);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,203,_ctx) ) {
				case 1:
					{
					setState(1688);
					match(NODE);
					}
					break;
				case 2:
					{
					setState(1689);
					match(VERTEX);
					}
					break;
				case 3:
					{
					setState(1690);
					match(RELATIONSHIP);
					}
					break;
				case 4:
					{
					setState(1691);
					match(EDGE);
					}
					break;
				case 5:
					{
					setState(1692);
					match(MAP);
					}
					break;
				case 6:
					{
					setState(1693);
					match(PROPERTY);
					setState(1694);
					match(VALUE);
					}
					break;
				case 7:
					{
					setState(1696);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==VALUE) {
						{
						setState(1695);
						match(VALUE);
						}
					}

					setState(1698);
					match(LT);
					setState(1699);
					type();
					setState(1700);
					match(GT);
					}
					break;
				case 8:
					{
					setState(1702);
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
			setState(1710);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case NOT:
				enterOuterAlt(_localctx, 1);
				{
				setState(1707);
				match(NOT);
				setState(1708);
				match(NULL);
				}
				break;
			case EXCLAMATION_MARK:
				enterOuterAlt(_localctx, 2);
				{
				setState(1709);
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
			setState(1712);
			_la = _input.LA(1);
			if ( !(_la==ARRAY || _la==LIST) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			setState(1714);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==EXCLAMATION_MARK || _la==NOT) {
				{
				setState(1713);
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
		enterRule(_localctx, 290, RULE_symbolicNameOrStringParameter);
		try {
			setState(1718);
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
				setState(1716);
				symbolicNameString();
				}
				break;
			case DOLLAR:
				enterOuterAlt(_localctx, 2);
				{
				setState(1717);
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
		enterRule(_localctx, 292, RULE_commandNameExpression);
		try {
			setState(1722);
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
				setState(1720);
				symbolicNameString();
				}
				break;
			case DOLLAR:
				enterOuterAlt(_localctx, 2);
				{
				setState(1721);
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
		enterRule(_localctx, 294, RULE_symbolicNameOrStringParameterList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1724);
			commandNameExpression();
			setState(1729);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(1725);
				match(COMMA);
				setState(1726);
				commandNameExpression();
				}
				}
				setState(1731);
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
		enterRule(_localctx, 296, RULE_symbolicAliasNameList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1732);
			symbolicAliasNameOrParameter();
			setState(1737);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(1733);
				match(COMMA);
				setState(1734);
				symbolicAliasNameOrParameter();
				}
				}
				setState(1739);
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
		enterRule(_localctx, 298, RULE_symbolicAliasNameOrParameter);
		try {
			setState(1742);
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
				setState(1740);
				symbolicAliasName();
				}
				break;
			case DOLLAR:
				enterOuterAlt(_localctx, 2);
				{
				setState(1741);
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
		enterRule(_localctx, 300, RULE_symbolicAliasName);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1744);
			symbolicNameString();
			setState(1749);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==DOT) {
				{
				{
				setState(1745);
				match(DOT);
				setState(1746);
				symbolicNameString();
				}
				}
				setState(1751);
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
		enterRule(_localctx, 302, RULE_stringListLiteral);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1752);
			match(LBRACKET);
			setState(1761);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==STRING_LITERAL1 || _la==STRING_LITERAL2) {
				{
				setState(1753);
				stringLiteral();
				setState(1758);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(1754);
					match(COMMA);
					setState(1755);
					stringLiteral();
					}
					}
					setState(1760);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
			}

			setState(1763);
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
		enterRule(_localctx, 304, RULE_stringList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1765);
			stringLiteral();
			setState(1768); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(1766);
				match(COMMA);
				setState(1767);
				stringLiteral();
				}
				}
				setState(1770); 
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
		enterRule(_localctx, 306, RULE_stringLiteral);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1772);
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
		enterRule(_localctx, 308, RULE_stringOrParameterExpression);
		try {
			setState(1776);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case STRING_LITERAL1:
			case STRING_LITERAL2:
				enterOuterAlt(_localctx, 1);
				{
				setState(1774);
				stringLiteral();
				}
				break;
			case DOLLAR:
				enterOuterAlt(_localctx, 2);
				{
				setState(1775);
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
		enterRule(_localctx, 310, RULE_stringOrParameter);
		try {
			setState(1780);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case STRING_LITERAL1:
			case STRING_LITERAL2:
				enterOuterAlt(_localctx, 1);
				{
				setState(1778);
				stringLiteral();
				}
				break;
			case DOLLAR:
				enterOuterAlt(_localctx, 2);
				{
				setState(1779);
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
		enterRule(_localctx, 312, RULE_uIntOrIntParameter);
		try {
			setState(1784);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case UNSIGNED_DECIMAL_INTEGER:
				enterOuterAlt(_localctx, 1);
				{
				setState(1782);
				match(UNSIGNED_DECIMAL_INTEGER);
				}
				break;
			case DOLLAR:
				enterOuterAlt(_localctx, 2);
				{
				setState(1783);
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
		enterRule(_localctx, 314, RULE_mapOrParameter);
		try {
			setState(1788);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case LCURLY:
				enterOuterAlt(_localctx, 1);
				{
				setState(1786);
				map();
				}
				break;
			case DOLLAR:
				enterOuterAlt(_localctx, 2);
				{
				setState(1787);
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
		enterRule(_localctx, 316, RULE_map);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1790);
			match(LCURLY);
			setState(1804);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & -985163492230144L) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & -1170935971839803393L) != 0) || ((((_la - 128)) & ~0x3f) == 0 && ((1L << (_la - 128)) & -129253703681L) != 0) || ((((_la - 192)) & ~0x3f) == 0 && ((1L << (_la - 192)) & -36169535635589505L) != 0) || ((((_la - 256)) & ~0x3f) == 0 && ((1L << (_la - 256)) & 18014398509449215L) != 0)) {
				{
				setState(1791);
				propertyKeyName();
				setState(1792);
				match(COLON);
				setState(1793);
				expression();
				setState(1801);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(1794);
					match(COMMA);
					setState(1795);
					propertyKeyName();
					setState(1796);
					match(COLON);
					setState(1797);
					expression();
					}
					}
					setState(1803);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
			}

			setState(1806);
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
		enterRule(_localctx, 318, RULE_symbolicVariableNameString);
		try {
			setState(1810);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case ESCAPED_SYMBOLIC_NAME:
				enterOuterAlt(_localctx, 1);
				{
				setState(1808);
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
				setState(1809);
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
		enterRule(_localctx, 320, RULE_escapedSymbolicVariableNameString);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1812);
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
		enterRule(_localctx, 322, RULE_unescapedSymbolicVariableNameString);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1814);
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
		enterRule(_localctx, 324, RULE_symbolicNameString);
		try {
			setState(1818);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case ESCAPED_SYMBOLIC_NAME:
				enterOuterAlt(_localctx, 1);
				{
				setState(1816);
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
				setState(1817);
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
		enterRule(_localctx, 326, RULE_escapedSymbolicNameString);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1820);
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
		enterRule(_localctx, 328, RULE_unescapedSymbolicNameString);
		try {
			setState(1831);
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
				setState(1822);
				unescapedLabelSymbolicNameString();
				}
				break;
			case NOT:
				enterOuterAlt(_localctx, 2);
				{
				setState(1823);
				match(NOT);
				}
				break;
			case NULL:
				enterOuterAlt(_localctx, 3);
				{
				setState(1824);
				match(NULL);
				}
				break;
			case TYPED:
				enterOuterAlt(_localctx, 4);
				{
				setState(1825);
				match(TYPED);
				}
				break;
			case NORMALIZED:
				enterOuterAlt(_localctx, 5);
				{
				setState(1826);
				match(NORMALIZED);
				}
				break;
			case NFC:
				enterOuterAlt(_localctx, 6);
				{
				setState(1827);
				match(NFC);
				}
				break;
			case NFD:
				enterOuterAlt(_localctx, 7);
				{
				setState(1828);
				match(NFD);
				}
				break;
			case NFKC:
				enterOuterAlt(_localctx, 8);
				{
				setState(1829);
				match(NFKC);
				}
				break;
			case NFKD:
				enterOuterAlt(_localctx, 9);
				{
				setState(1830);
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
		enterRule(_localctx, 330, RULE_symbolicLabelNameString);
		try {
			setState(1835);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case ESCAPED_SYMBOLIC_NAME:
				enterOuterAlt(_localctx, 1);
				{
				setState(1833);
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
				setState(1834);
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
		enterRule(_localctx, 332, RULE_unescapedLabelSymbolicNameString);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1837);
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
		enterRule(_localctx, 334, RULE_unescapedLabelSymbolicNameString_);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1839);
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
		enterRule(_localctx, 336, RULE_endOfFile);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1841);
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

	public static final String _serializedATN =
		"\u0004\u0001\u0139\u0734\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001"+
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
		"\u00a7\u0002\u00a8\u0007\u00a8\u0001\u0000\u0001\u0000\u0001\u0000\u0005"+
		"\u0000\u0156\b\u0000\n\u0000\f\u0000\u0159\t\u0000\u0001\u0000\u0003\u0000"+
		"\u015c\b\u0000\u0001\u0000\u0001\u0000\u0001\u0001\u0003\u0001\u0161\b"+
		"\u0001\u0001\u0001\u0001\u0001\u0001\u0002\u0001\u0002\u0001\u0002\u0001"+
		"\u0002\u0003\u0002\u0169\b\u0002\u0001\u0003\u0001\u0003\u0001\u0003\u0003"+
		"\u0003\u016e\b\u0003\u0001\u0003\u0005\u0003\u0171\b\u0003\n\u0003\f\u0003"+
		"\u0174\t\u0003\u0001\u0004\u0004\u0004\u0177\b\u0004\u000b\u0004\f\u0004"+
		"\u0178\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0001"+
		"\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0001"+
		"\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0003"+
		"\u0005\u018c\b\u0005\u0001\u0006\u0001\u0006\u0003\u0006\u0190\b\u0006"+
		"\u0001\u0006\u0001\u0006\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007"+
		"\u0001\u0007\u0001\u0007\u0003\u0007\u019a\b\u0007\u0001\b\u0001\b\u0001"+
		"\t\u0001\t\u0001\t\u0001\n\u0003\n\u01a2\b\n\u0001\n\u0001\n\u0003\n\u01a6"+
		"\b\n\u0001\n\u0003\n\u01a9\b\n\u0001\n\u0003\n\u01ac\b\n\u0001\u000b\u0001"+
		"\u000b\u0001\u000b\u0003\u000b\u01b1\b\u000b\u0001\f\u0001\f\u0003\f\u01b5"+
		"\b\f\u0001\f\u0001\f\u0005\f\u01b9\b\f\n\f\f\f\u01bc\t\f\u0001\r\u0001"+
		"\r\u0001\r\u0003\r\u01c1\b\r\u0001\u000e\u0001\u000e\u0001\u000f\u0001"+
		"\u000f\u0001\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0005"+
		"\u0010\u01cc\b\u0010\n\u0010\f\u0010\u01cf\t\u0010\u0001\u0011\u0001\u0011"+
		"\u0001\u0011\u0001\u0012\u0001\u0012\u0001\u0012\u0001\u0013\u0001\u0013"+
		"\u0001\u0013\u0001\u0014\u0001\u0014\u0001\u0014\u0003\u0014\u01dd\b\u0014"+
		"\u0001\u0015\u0001\u0015\u0001\u0015\u0001\u0016\u0001\u0016\u0001\u0016"+
		"\u0001\u0017\u0001\u0017\u0001\u0017\u0001\u0017\u0005\u0017\u01e9\b\u0017"+
		"\n\u0017\f\u0017\u01ec\t\u0017\u0001\u0018\u0001\u0018\u0001\u0018\u0001"+
		"\u0018\u0001\u0018\u0001\u0018\u0001\u0018\u0001\u0018\u0001\u0018\u0001"+
		"\u0018\u0001\u0018\u0001\u0018\u0001\u0018\u0001\u0018\u0001\u0018\u0001"+
		"\u0018\u0001\u0018\u0001\u0018\u0001\u0018\u0001\u0018\u0001\u0018\u0001"+
		"\u0018\u0003\u0018\u0204\b\u0018\u0001\u0019\u0001\u0019\u0001\u0019\u0001"+
		"\u0019\u0005\u0019\u020a\b\u0019\n\u0019\f\u0019\u020d\t\u0019\u0001\u001a"+
		"\u0001\u001a\u0001\u001a\u0001\u001a\u0001\u001a\u0001\u001a\u0001\u001a"+
		"\u0001\u001a\u0003\u001a\u0217\b\u001a\u0001\u001b\u0003\u001b\u021a\b"+
		"\u001b\u0001\u001b\u0001\u001b\u0001\u001b\u0001\u001b\u0005\u001b\u0220"+
		"\b\u001b\n\u001b\f\u001b\u0223\t\u001b\u0001\u001c\u0003\u001c\u0226\b"+
		"\u001c\u0001\u001c\u0001\u001c\u0003\u001c\u022a\b\u001c\u0001\u001c\u0001"+
		"\u001c\u0005\u001c\u022e\b\u001c\n\u001c\f\u001c\u0231\t\u001c\u0001\u001c"+
		"\u0003\u001c\u0234\b\u001c\u0001\u001d\u0001\u001d\u0001\u001d\u0003\u001d"+
		"\u0239\b\u001d\u0001\u001d\u0003\u001d\u023c\b\u001d\u0001\u001d\u0001"+
		"\u001d\u0001\u001d\u0003\u001d\u0241\b\u001d\u0001\u001d\u0003\u001d\u0244"+
		"\b\u001d\u0003\u001d\u0246\b\u001d\u0001\u001e\u0001\u001e\u0001\u001e"+
		"\u0001\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0001\u001e"+
		"\u0001\u001e\u0003\u001e\u0252\b\u001e\u0001\u001e\u0003\u001e\u0255\b"+
		"\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0001"+
		"\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0001"+
		"\u001e\u0001\u001e\u0003\u001e\u0264\b\u001e\u0001\u001f\u0001\u001f\u0001"+
		"\u001f\u0005\u001f\u0269\b\u001f\n\u001f\f\u001f\u026c\t\u001f\u0001 "+
		"\u0001 \u0001 \u0001 \u0001!\u0001!\u0001!\u0001!\u0001!\u0001\"\u0003"+
		"\"\u0278\b\"\u0001\"\u0001\"\u0001\"\u0001\"\u0001\"\u0001\"\u0005\"\u0280"+
		"\b\"\n\"\f\"\u0283\t\"\u0003\"\u0285\b\"\u0001\"\u0003\"\u0288\b\"\u0001"+
		"\"\u0001\"\u0001\"\u0001\"\u0001\"\u0005\"\u028f\b\"\n\"\f\"\u0292\t\""+
		"\u0001\"\u0003\"\u0295\b\"\u0003\"\u0297\b\"\u0003\"\u0299\b\"\u0001#"+
		"\u0001#\u0001#\u0001$\u0001$\u0001%\u0001%\u0001%\u0003%\u02a3\b%\u0001"+
		"&\u0001&\u0001&\u0001&\u0003&\u02a9\b&\u0001&\u0001&\u0001&\u0001&\u0001"+
		"&\u0001&\u0003&\u02b1\b&\u0001\'\u0001\'\u0001\'\u0001\'\u0001\'\u0001"+
		"\'\u0001\'\u0004\'\u02ba\b\'\u000b\'\f\'\u02bb\u0001\'\u0001\'\u0001("+
		"\u0003(\u02c1\b(\u0001(\u0001(\u0003(\u02c5\b(\u0001(\u0001(\u0001(\u0001"+
		"(\u0003(\u02cb\b(\u0001)\u0001)\u0001)\u0001)\u0001)\u0005)\u02d2\b)\n"+
		")\f)\u02d5\t)\u0003)\u02d7\b)\u0001)\u0001)\u0001*\u0001*\u0003*\u02dd"+
		"\b*\u0001*\u0003*\u02e0\b*\u0001*\u0001*\u0001*\u0001*\u0005*\u02e6\b"+
		"*\n*\f*\u02e9\t*\u0001+\u0001+\u0001+\u0001+\u0001,\u0001,\u0001,\u0001"+
		",\u0001-\u0001-\u0001-\u0001-\u0001-\u0001.\u0001.\u0003.\u02fa\b.\u0001"+
		".\u0003.\u02fd\b.\u0001.\u0001.\u0003.\u0301\b.\u0001.\u0003.\u0304\b"+
		".\u0001/\u0001/\u0001/\u0005/\u0309\b/\n/\f/\u030c\t/\u00010\u00010\u0001"+
		"0\u00050\u0311\b0\n0\f0\u0314\t0\u00011\u00011\u00011\u00031\u0319\b1"+
		"\u00011\u00031\u031c\b1\u00011\u00011\u00012\u00012\u00012\u00032\u0323"+
		"\b2\u00012\u00012\u00012\u00012\u00052\u0329\b2\n2\f2\u032c\t2\u00013"+
		"\u00013\u00013\u00013\u00013\u00033\u0333\b3\u00013\u00013\u00033\u0337"+
		"\b3\u00013\u00013\u00013\u00033\u033c\b3\u00014\u00014\u00034\u0340\b"+
		"4\u00015\u00015\u00015\u00015\u00015\u00016\u00016\u00016\u00036\u034a"+
		"\b6\u00016\u00016\u00056\u034e\b6\n6\f6\u0351\t6\u00016\u00046\u0354\b"+
		"6\u000b6\f6\u0355\u00017\u00017\u00017\u00037\u035b\b7\u00017\u00017\u0001"+
		"7\u00037\u0360\b7\u00017\u00017\u00037\u0364\b7\u00017\u00037\u0367\b"+
		"7\u00017\u00017\u00037\u036b\b7\u00017\u00017\u00037\u036f\b7\u00017\u0003"+
		"7\u0372\b7\u00017\u00017\u00017\u00017\u00037\u0378\b7\u00037\u037a\b"+
		"7\u00018\u00018\u00019\u00019\u0001:\u0001:\u0001:\u0001:\u0004:\u0384"+
		"\b:\u000b:\f:\u0385\u0001;\u0001;\u0003;\u038a\b;\u0001;\u0003;\u038d"+
		"\b;\u0001;\u0003;\u0390\b;\u0001;\u0001;\u0003;\u0394\b;\u0001;\u0001"+
		";\u0001<\u0001<\u0003<\u039a\b<\u0001<\u0003<\u039d\b<\u0001<\u0003<\u03a0"+
		"\b<\u0001<\u0001<\u0001=\u0001=\u0001=\u0001=\u0003=\u03a8\b=\u0001=\u0001"+
		"=\u0003=\u03ac\b=\u0001>\u0001>\u0004>\u03b0\b>\u000b>\f>\u03b1\u0001"+
		"?\u0001?\u0001?\u0003?\u03b7\b?\u0001?\u0001?\u0005?\u03bb\b?\n?\f?\u03be"+
		"\t?\u0001@\u0001@\u0001@\u0001@\u0001@\u0001A\u0001A\u0003A\u03c7\bA\u0001"+
		"A\u0001A\u0001A\u0001A\u0001B\u0001B\u0001B\u0001C\u0001C\u0001C\u0001"+
		"D\u0001D\u0001D\u0001E\u0001E\u0001E\u0001F\u0001F\u0003F\u03db\bF\u0001"+
		"G\u0003G\u03de\bG\u0001G\u0001G\u0001G\u0003G\u03e3\bG\u0001G\u0003G\u03e6"+
		"\bG\u0001G\u0003G\u03e9\bG\u0001G\u0003G\u03ec\bG\u0001G\u0001G\u0003"+
		"G\u03f0\bG\u0001G\u0003G\u03f3\bG\u0001G\u0001G\u0003G\u03f7\bG\u0001"+
		"H\u0003H\u03fa\bH\u0001H\u0001H\u0001H\u0003H\u03ff\bH\u0001H\u0001H\u0003"+
		"H\u0403\bH\u0001H\u0001H\u0001H\u0003H\u0408\bH\u0001I\u0001I\u0001J\u0001"+
		"J\u0001K\u0001K\u0001L\u0001L\u0003L\u0412\bL\u0001L\u0001L\u0003L\u0416"+
		"\bL\u0001L\u0003L\u0419\bL\u0001M\u0001M\u0001M\u0001M\u0003M\u041f\b"+
		"M\u0001N\u0001N\u0001N\u0003N\u0424\bN\u0001N\u0005N\u0427\bN\nN\fN\u042a"+
		"\tN\u0001O\u0001O\u0001O\u0003O\u042f\bO\u0001O\u0005O\u0432\bO\nO\fO"+
		"\u0435\tO\u0001P\u0001P\u0001P\u0005P\u043a\bP\nP\fP\u043d\tP\u0001Q\u0001"+
		"Q\u0001Q\u0005Q\u0442\bQ\nQ\fQ\u0445\tQ\u0001R\u0005R\u0448\bR\nR\fR\u044b"+
		"\tR\u0001R\u0001R\u0001S\u0005S\u0450\bS\nS\fS\u0453\tS\u0001S\u0001S"+
		"\u0001T\u0001T\u0001T\u0001T\u0001T\u0001T\u0001T\u0003T\u045e\bT\u0001"+
		"U\u0001U\u0001U\u0001U\u0001U\u0001U\u0001U\u0003U\u0467\bU\u0001V\u0001"+
		"V\u0001V\u0001V\u0005V\u046d\bV\nV\fV\u0470\tV\u0001W\u0001W\u0001W\u0001"+
		"X\u0001X\u0001X\u0005X\u0478\bX\nX\fX\u047b\tX\u0001Y\u0001Y\u0001Y\u0005"+
		"Y\u0480\bY\nY\fY\u0483\tY\u0001Z\u0001Z\u0001Z\u0005Z\u0488\bZ\nZ\fZ\u048b"+
		"\tZ\u0001[\u0005[\u048e\b[\n[\f[\u0491\t[\u0001[\u0001[\u0001\\\u0001"+
		"\\\u0001\\\u0005\\\u0498\b\\\n\\\f\\\u049b\t\\\u0001]\u0001]\u0003]\u049f"+
		"\b]\u0001^\u0001^\u0001^\u0001^\u0001^\u0001^\u0001^\u0003^\u04a8\b^\u0001"+
		"^\u0001^\u0001^\u0003^\u04ad\b^\u0001^\u0001^\u0001^\u0003^\u04b2\b^\u0001"+
		"^\u0001^\u0003^\u04b6\b^\u0001^\u0001^\u0001^\u0003^\u04bb\b^\u0001^\u0003"+
		"^\u04be\b^\u0001^\u0003^\u04c1\b^\u0001_\u0001_\u0001`\u0001`\u0001`\u0005"+
		"`\u04c8\b`\n`\f`\u04cb\t`\u0001a\u0001a\u0001a\u0005a\u04d0\ba\na\fa\u04d3"+
		"\ta\u0001b\u0001b\u0001b\u0005b\u04d8\bb\nb\fb\u04db\tb\u0001c\u0001c"+
		"\u0001c\u0003c\u04e0\bc\u0001d\u0001d\u0005d\u04e4\bd\nd\fd\u04e7\td\u0001"+
		"e\u0001e\u0001e\u0001e\u0001e\u0001e\u0001e\u0001e\u0003e\u04f1\be\u0001"+
		"e\u0001e\u0003e\u04f5\be\u0001e\u0003e\u04f8\be\u0001f\u0001f\u0001f\u0001"+
		"g\u0001g\u0001g\u0001g\u0001h\u0001h\u0004h\u0503\bh\u000bh\fh\u0504\u0001"+
		"i\u0001i\u0001i\u0001j\u0001j\u0001j\u0001j\u0001j\u0001j\u0001j\u0001"+
		"j\u0001j\u0001j\u0001j\u0001j\u0001j\u0001j\u0001j\u0001j\u0001j\u0001"+
		"j\u0001j\u0001j\u0001j\u0003j\u051f\bj\u0001k\u0001k\u0001k\u0001k\u0001"+
		"k\u0001k\u0001k\u0001k\u0001k\u0003k\u052a\bk\u0001l\u0001l\u0004l\u052e"+
		"\bl\u000bl\fl\u052f\u0001l\u0001l\u0003l\u0534\bl\u0001l\u0001l\u0001"+
		"m\u0001m\u0001m\u0001m\u0001m\u0001n\u0001n\u0001n\u0004n\u0540\bn\u000b"+
		"n\fn\u0541\u0001n\u0001n\u0003n\u0546\bn\u0001n\u0001n\u0001o\u0001o\u0001"+
		"o\u0001o\u0005o\u054e\bo\no\fo\u0551\to\u0001o\u0001o\u0001o\u0001p\u0001"+
		"p\u0001p\u0001p\u0001p\u0003p\u055b\bp\u0001p\u0001p\u0001p\u0003p\u0560"+
		"\bp\u0001p\u0001p\u0001p\u0003p\u0565\bp\u0001p\u0001p\u0003p\u0569\b"+
		"p\u0001p\u0001p\u0001p\u0003p\u056e\bp\u0001p\u0003p\u0571\bp\u0001p\u0001"+
		"p\u0001p\u0001p\u0003p\u0577\bp\u0001q\u0001q\u0001q\u0001q\u0001q\u0001"+
		"q\u0003q\u057f\bq\u0001q\u0001q\u0001q\u0001q\u0003q\u0585\bq\u0003q\u0587"+
		"\bq\u0001q\u0001q\u0001r\u0001r\u0001r\u0001r\u0003r\u058f\br\u0001r\u0001"+
		"r\u0001r\u0003r\u0594\br\u0001r\u0001r\u0001r\u0001r\u0001s\u0001s\u0001"+
		"s\u0001s\u0001s\u0001s\u0001s\u0001s\u0001s\u0001s\u0001s\u0001s\u0001"+
		"s\u0001t\u0001t\u0001t\u0001t\u0001t\u0001t\u0001t\u0003t\u05ae\bt\u0001"+
		"t\u0001t\u0001u\u0001u\u0001u\u0001u\u0001u\u0003u\u05b7\bu\u0001u\u0001"+
		"u\u0001v\u0001v\u0001v\u0003v\u05be\bv\u0001v\u0003v\u05c1\bv\u0001v\u0003"+
		"v\u05c4\bv\u0001v\u0001v\u0001v\u0001w\u0001w\u0001x\u0001x\u0001y\u0001"+
		"y\u0001y\u0001y\u0001z\u0001z\u0001z\u0001z\u0001z\u0005z\u05d6\bz\nz"+
		"\fz\u05d9\tz\u0003z\u05db\bz\u0001z\u0001z\u0001{\u0001{\u0001{\u0001"+
		"{\u0001{\u0001{\u0001{\u0001{\u0003{\u05e7\b{\u0001|\u0001|\u0001|\u0001"+
		"|\u0001|\u0001}\u0001}\u0001}\u0001}\u0003}\u05f2\b}\u0001}\u0001}\u0003"+
		"}\u05f6\b}\u0003}\u05f8\b}\u0001}\u0001}\u0001~\u0001~\u0001~\u0001~\u0003"+
		"~\u0600\b~\u0001~\u0001~\u0003~\u0604\b~\u0003~\u0606\b~\u0001~\u0001"+
		"~\u0001\u007f\u0001\u007f\u0001\u007f\u0001\u007f\u0001\u007f\u0001\u0080"+
		"\u0003\u0080\u0610\b\u0080\u0001\u0080\u0001\u0080\u0001\u0081\u0003\u0081"+
		"\u0615\b\u0081\u0001\u0081\u0001\u0081\u0001\u0082\u0001\u0082\u0001\u0082"+
		"\u0001\u0082\u0005\u0082\u061d\b\u0082\n\u0082\f\u0082\u0620\t\u0082\u0003"+
		"\u0082\u0622\b\u0082\u0001\u0082\u0001\u0082\u0001\u0083\u0001\u0083\u0001"+
		"\u0084\u0001\u0084\u0001\u0084\u0001\u0085\u0001\u0085\u0003\u0085\u062d"+
		"\b\u0085\u0001\u0086\u0001\u0086\u0001\u0086\u0003\u0086\u0632\b\u0086"+
		"\u0001\u0086\u0001\u0086\u0001\u0086\u0005\u0086\u0637\b\u0086\n\u0086"+
		"\f\u0086\u063a\t\u0086\u0003\u0086\u063c\b\u0086\u0001\u0086\u0001\u0086"+
		"\u0001\u0087\u0001\u0087\u0001\u0088\u0001\u0088\u0001\u0088\u0001\u0089"+
		"\u0001\u0089\u0001\u0089\u0005\u0089\u0648\b\u0089\n\u0089\f\u0089\u064b"+
		"\t\u0089\u0001\u008a\u0001\u008a\u0001\u008b\u0001\u008b\u0001\u008b\u0005"+
		"\u008b\u0652\b\u008b\n\u008b\f\u008b\u0655\t\u008b\u0001\u008c\u0001\u008c"+
		"\u0001\u008c\u0005\u008c\u065a\b\u008c\n\u008c\f\u008c\u065d\t\u008c\u0001"+
		"\u008d\u0001\u008d\u0003\u008d\u0661\b\u008d\u0001\u008d\u0005\u008d\u0664"+
		"\b\u008d\n\u008d\f\u008d\u0667\t\u008d\u0001\u008e\u0001\u008e\u0001\u008e"+
		"\u0001\u008e\u0001\u008e\u0001\u008e\u0001\u008e\u0001\u008e\u0003\u008e"+
		"\u0671\b\u008e\u0001\u008e\u0001\u008e\u0001\u008e\u0001\u008e\u0001\u008e"+
		"\u0001\u008e\u0001\u008e\u0001\u008e\u0001\u008e\u0001\u008e\u0001\u008e"+
		"\u0001\u008e\u0003\u008e\u067f\b\u008e\u0001\u008e\u0001\u008e\u0001\u008e"+
		"\u0001\u008e\u0001\u008e\u0003\u008e\u0686\b\u008e\u0001\u008e\u0001\u008e"+
		"\u0001\u008e\u0001\u008e\u0001\u008e\u0001\u008e\u0001\u008e\u0001\u008e"+
		"\u0001\u008e\u0001\u008e\u0001\u008e\u0001\u008e\u0001\u008e\u0001\u008e"+
		"\u0001\u008e\u0001\u008e\u0001\u008e\u0001\u008e\u0001\u008e\u0001\u008e"+
		"\u0001\u008e\u0001\u008e\u0001\u008e\u0001\u008e\u0001\u008e\u0003\u008e"+
		"\u06a1\b\u008e\u0001\u008e\u0001\u008e\u0001\u008e\u0001\u008e\u0001\u008e"+
		"\u0003\u008e\u06a8\b\u008e\u0003\u008e\u06aa\b\u008e\u0001\u008f\u0001"+
		"\u008f\u0001\u008f\u0003\u008f\u06af\b\u008f\u0001\u0090\u0001\u0090\u0003"+
		"\u0090\u06b3\b\u0090\u0001\u0091\u0001\u0091\u0003\u0091\u06b7\b\u0091"+
		"\u0001\u0092\u0001\u0092\u0003\u0092\u06bb\b\u0092\u0001\u0093\u0001\u0093"+
		"\u0001\u0093\u0005\u0093\u06c0\b\u0093\n\u0093\f\u0093\u06c3\t\u0093\u0001"+
		"\u0094\u0001\u0094\u0001\u0094\u0005\u0094\u06c8\b\u0094\n\u0094\f\u0094"+
		"\u06cb\t\u0094\u0001\u0095\u0001\u0095\u0003\u0095\u06cf\b\u0095\u0001"+
		"\u0096\u0001\u0096\u0001\u0096\u0005\u0096\u06d4\b\u0096\n\u0096\f\u0096"+
		"\u06d7\t\u0096\u0001\u0097\u0001\u0097\u0001\u0097\u0001\u0097\u0005\u0097"+
		"\u06dd\b\u0097\n\u0097\f\u0097\u06e0\t\u0097\u0003\u0097\u06e2\b\u0097"+
		"\u0001\u0097\u0001\u0097\u0001\u0098\u0001\u0098\u0001\u0098\u0004\u0098"+
		"\u06e9\b\u0098\u000b\u0098\f\u0098\u06ea\u0001\u0099\u0001\u0099\u0001"+
		"\u009a\u0001\u009a\u0003\u009a\u06f1\b\u009a\u0001\u009b\u0001\u009b\u0003"+
		"\u009b\u06f5\b\u009b\u0001\u009c\u0001\u009c\u0003\u009c\u06f9\b\u009c"+
		"\u0001\u009d\u0001\u009d\u0003\u009d\u06fd\b\u009d\u0001\u009e\u0001\u009e"+
		"\u0001\u009e\u0001\u009e\u0001\u009e\u0001\u009e\u0001\u009e\u0001\u009e"+
		"\u0001\u009e\u0005\u009e\u0708\b\u009e\n\u009e\f\u009e\u070b\t\u009e\u0003"+
		"\u009e\u070d\b\u009e\u0001\u009e\u0001\u009e\u0001\u009f\u0001\u009f\u0003"+
		"\u009f\u0713\b\u009f\u0001\u00a0\u0001\u00a0\u0001\u00a1\u0001\u00a1\u0001"+
		"\u00a2\u0001\u00a2\u0003\u00a2\u071b\b\u00a2\u0001\u00a3\u0001\u00a3\u0001"+
		"\u00a4\u0001\u00a4\u0001\u00a4\u0001\u00a4\u0001\u00a4\u0001\u00a4\u0001"+
		"\u00a4\u0001\u00a4\u0001\u00a4\u0003\u00a4\u0728\b\u00a4\u0001\u00a5\u0001"+
		"\u00a5\u0003\u00a5\u072c\b\u00a5\u0001\u00a6\u0001\u00a6\u0001\u00a7\u0001"+
		"\u00a7\u0001\u00a8\u0001\u00a8\u0001\u00a8\u0000\u0000\u00a9\u0000\u0002"+
		"\u0004\u0006\b\n\f\u000e\u0010\u0012\u0014\u0016\u0018\u001a\u001c\u001e"+
		" \"$&(*,.02468:<>@BDFHJLNPRTVXZ\\^`bdfhjlnprtvxz|~\u0080\u0082\u0084\u0086"+
		"\u0088\u008a\u008c\u008e\u0090\u0092\u0094\u0096\u0098\u009a\u009c\u009e"+
		"\u00a0\u00a2\u00a4\u00a6\u00a8\u00aa\u00ac\u00ae\u00b0\u00b2\u00b4\u00b6"+
		"\u00b8\u00ba\u00bc\u00be\u00c0\u00c2\u00c4\u00c6\u00c8\u00ca\u00cc\u00ce"+
		"\u00d0\u00d2\u00d4\u00d6\u00d8\u00da\u00dc\u00de\u00e0\u00e2\u00e4\u00e6"+
		"\u00e8\u00ea\u00ec\u00ee\u00f0\u00f2\u00f4\u00f6\u00f8\u00fa\u00fc\u00fe"+
		"\u0100\u0102\u0104\u0106\u0108\u010a\u010c\u010e\u0110\u0112\u0114\u0116"+
		"\u0118\u011a\u011c\u011e\u0120\u0122\u0124\u0126\u0128\u012a\u012c\u012e"+
		"\u0130\u0132\u0134\u0136\u0138\u013a\u013c\u013e\u0140\u0142\u0144\u0146"+
		"\u0148\u014a\u014c\u014e\u0150\u0000\u001f\u0002\u0000\u0012\u0012QQ\u0001"+
		"\u0000\u0018\u0019\u0001\u0000KL\u0002\u0000\u00b8\u00b8\u0102\u0102\u0002"+
		"\u0000NN\u00ae\u00ae\u0002\u0000==\u009f\u009f\u0001\u0000\u00ed\u00ee"+
		"\u0003\u0000$$;;kk\u0002\u0000\u0011\u0011\u00fd\u00fd\u0001\u0000z{\u0001"+
		"\u0000\u00c3\u00c4\u0002\u0000\u0012\u0012\u0015\u0015\u0002\u0000\u009c"+
		"\u009c\u0137\u0137\u0002\u0000\u00a1\u00a1\u0136\u0136\u0002\u0000||\u0138"+
		"\u0138\u0002\u0000//\u0090\u0090\u0002\u0000//\u008b\u008b\u0006\u0000"+
		"ddvv||\u0094\u0094\u009c\u009c\u00a3\u00a4\u0002\u000000\u011b\u011b\u0001"+
		"\u0000\u00a8\u00ab\u0003\u0000UU\u00a1\u00a1\u00c7\u00c7\u0003\u0000R"+
		"R\u00a2\u00a2\u010f\u010f\u0002\u0000\u00a1\u00a1\u00c7\u00c7\u0004\u0000"+
		"\u0012\u0012\u0015\u0015\u00b0\u00b0\u0101\u0101\u0003\u0000##\u0095\u0095"+
		"\u0114\u0114\u0001\u0000\u0004\u0007\u0002\u0000DD\u010e\u010e\u0001\u0000"+
		"\u012e\u012f\u0002\u0000\u0016\u0016\u0097\u0097\u0001\u0000\b\t\u0017"+
		"\u0000\u000b\u001d\u001f.2OQQVceuw{}\u008f\u0095\u009a\u009d\u00a0\u00a5"+
		"\u00a7\u00ac\u00b1\u00b4\u00b5\u00b7\u00c6\u00c9\u00ca\u00cc\u00d5\u00d7"+
		"\u00d7\u00da\u00dd\u00df\u00ee\u00f0\u00f6\u00f8\u010e\u0110\u011a\u011c"+
		"\u0135\u07e3\u0000\u0152\u0001\u0000\u0000\u0000\u0002\u0160\u0001\u0000"+
		"\u0000\u0000\u0004\u0164\u0001\u0000\u0000\u0000\u0006\u016a\u0001\u0000"+
		"\u0000\u0000\b\u0176\u0001\u0000\u0000\u0000\n\u018b\u0001\u0000\u0000"+
		"\u0000\f\u018d\u0001\u0000\u0000\u0000\u000e\u0199\u0001\u0000\u0000\u0000"+
		"\u0010\u019b\u0001\u0000\u0000\u0000\u0012\u019d\u0001\u0000\u0000\u0000"+
		"\u0014\u01a1\u0001\u0000\u0000\u0000\u0016\u01ad\u0001\u0000\u0000\u0000"+
		"\u0018\u01b4\u0001\u0000\u0000\u0000\u001a\u01bd\u0001\u0000\u0000\u0000"+
		"\u001c\u01c2\u0001\u0000\u0000\u0000\u001e\u01c4\u0001\u0000\u0000\u0000"+
		" \u01c6\u0001\u0000\u0000\u0000\"\u01d0\u0001\u0000\u0000\u0000$\u01d3"+
		"\u0001\u0000\u0000\u0000&\u01d6\u0001\u0000\u0000\u0000(\u01d9\u0001\u0000"+
		"\u0000\u0000*\u01de\u0001\u0000\u0000\u0000,\u01e1\u0001\u0000\u0000\u0000"+
		".\u01e4\u0001\u0000\u0000\u00000\u0203\u0001\u0000\u0000\u00002\u0205"+
		"\u0001\u0000\u0000\u00004\u0216\u0001\u0000\u0000\u00006\u0219\u0001\u0000"+
		"\u0000\u00008\u0225\u0001\u0000\u0000\u0000:\u0245\u0001\u0000\u0000\u0000"+
		"<\u0247\u0001\u0000\u0000\u0000>\u0265\u0001\u0000\u0000\u0000@\u026d"+
		"\u0001\u0000\u0000\u0000B\u0271\u0001\u0000\u0000\u0000D\u0277\u0001\u0000"+
		"\u0000\u0000F\u029a\u0001\u0000\u0000\u0000H\u029d\u0001\u0000\u0000\u0000"+
		"J\u029f\u0001\u0000\u0000\u0000L\u02a4\u0001\u0000\u0000\u0000N\u02b2"+
		"\u0001\u0000\u0000\u0000P\u02c0\u0001\u0000\u0000\u0000R\u02cc\u0001\u0000"+
		"\u0000\u0000T\u02da\u0001\u0000\u0000\u0000V\u02ea\u0001\u0000\u0000\u0000"+
		"X\u02ee\u0001\u0000\u0000\u0000Z\u02f2\u0001\u0000\u0000\u0000\\\u0303"+
		"\u0001\u0000\u0000\u0000^\u0305\u0001\u0000\u0000\u0000`\u030d\u0001\u0000"+
		"\u0000\u0000b\u0318\u0001\u0000\u0000\u0000d\u0322\u0001\u0000\u0000\u0000"+
		"f\u033b\u0001\u0000\u0000\u0000h\u033f\u0001\u0000\u0000\u0000j\u0341"+
		"\u0001\u0000\u0000\u0000l\u0353\u0001\u0000\u0000\u0000n\u0379\u0001\u0000"+
		"\u0000\u0000p\u037b\u0001\u0000\u0000\u0000r\u037d\u0001\u0000\u0000\u0000"+
		"t\u037f\u0001\u0000\u0000\u0000v\u0387\u0001\u0000\u0000\u0000x\u0397"+
		"\u0001\u0000\u0000\u0000z\u03a3\u0001\u0000\u0000\u0000|\u03af\u0001\u0000"+
		"\u0000\u0000~\u03b3\u0001\u0000\u0000\u0000\u0080\u03bf\u0001\u0000\u0000"+
		"\u0000\u0082\u03c4\u0001\u0000\u0000\u0000\u0084\u03cc\u0001\u0000\u0000"+
		"\u0000\u0086\u03cf\u0001\u0000\u0000\u0000\u0088\u03d2\u0001\u0000\u0000"+
		"\u0000\u008a\u03d5\u0001\u0000\u0000\u0000\u008c\u03da\u0001\u0000\u0000"+
		"\u0000\u008e\u03dd\u0001\u0000\u0000\u0000\u0090\u03f9\u0001\u0000\u0000"+
		"\u0000\u0092\u0409\u0001\u0000\u0000\u0000\u0094\u040b\u0001\u0000\u0000"+
		"\u0000\u0096\u040d\u0001\u0000\u0000\u0000\u0098\u040f\u0001\u0000\u0000"+
		"\u0000\u009a\u041e\u0001\u0000\u0000\u0000\u009c\u0420\u0001\u0000\u0000"+
		"\u0000\u009e\u042b\u0001\u0000\u0000\u0000\u00a0\u0436\u0001\u0000\u0000"+
		"\u0000\u00a2\u043e\u0001\u0000\u0000\u0000\u00a4\u0449\u0001\u0000\u0000"+
		"\u0000\u00a6\u0451\u0001\u0000\u0000\u0000\u00a8\u045d\u0001\u0000\u0000"+
		"\u0000\u00aa\u0466\u0001\u0000\u0000\u0000\u00ac\u0468\u0001\u0000\u0000"+
		"\u0000\u00ae\u0471\u0001\u0000\u0000\u0000\u00b0\u0474\u0001\u0000\u0000"+
		"\u0000\u00b2\u047c\u0001\u0000\u0000\u0000\u00b4\u0484\u0001\u0000\u0000"+
		"\u0000\u00b6\u048f\u0001\u0000\u0000\u0000\u00b8\u0494\u0001\u0000\u0000"+
		"\u0000\u00ba\u049c\u0001\u0000\u0000\u0000\u00bc\u04c0\u0001\u0000\u0000"+
		"\u0000\u00be\u04c2\u0001\u0000\u0000\u0000\u00c0\u04c4\u0001\u0000\u0000"+
		"\u0000\u00c2\u04cc\u0001\u0000\u0000\u0000\u00c4\u04d4\u0001\u0000\u0000"+
		"\u0000\u00c6\u04df\u0001\u0000\u0000\u0000\u00c8\u04e1\u0001\u0000\u0000"+
		"\u0000\u00ca\u04f7\u0001\u0000\u0000\u0000\u00cc\u04f9\u0001\u0000\u0000"+
		"\u0000\u00ce\u04fc\u0001\u0000\u0000\u0000\u00d0\u0500\u0001\u0000\u0000"+
		"\u0000\u00d2\u0506\u0001\u0000\u0000\u0000\u00d4\u051e\u0001\u0000\u0000"+
		"\u0000\u00d6\u0529\u0001\u0000\u0000\u0000\u00d8\u052b\u0001\u0000\u0000"+
		"\u0000\u00da\u0537\u0001\u0000\u0000\u0000\u00dc\u053c\u0001\u0000\u0000"+
		"\u0000\u00de\u0549\u0001\u0000\u0000\u0000\u00e0\u0576\u0001\u0000\u0000"+
		"\u0000\u00e2\u0578\u0001\u0000\u0000\u0000\u00e4\u058a\u0001\u0000\u0000"+
		"\u0000\u00e6\u0599\u0001\u0000\u0000\u0000\u00e8\u05a6\u0001\u0000\u0000"+
		"\u0000\u00ea\u05b1\u0001\u0000\u0000\u0000\u00ec\u05ba\u0001\u0000\u0000"+
		"\u0000\u00ee\u05c8\u0001\u0000\u0000\u0000\u00f0\u05ca\u0001\u0000\u0000"+
		"\u0000\u00f2\u05cc\u0001\u0000\u0000\u0000\u00f4\u05d0\u0001\u0000\u0000"+
		"\u0000\u00f6\u05e6\u0001\u0000\u0000\u0000\u00f8\u05e8\u0001\u0000\u0000"+
		"\u0000\u00fa\u05ed\u0001\u0000\u0000\u0000\u00fc\u05fb\u0001\u0000\u0000"+
		"\u0000\u00fe\u0609\u0001\u0000\u0000\u0000\u0100\u060f\u0001\u0000\u0000"+
		"\u0000\u0102\u0614\u0001\u0000\u0000\u0000\u0104\u0618\u0001\u0000\u0000"+
		"\u0000\u0106\u0625\u0001\u0000\u0000\u0000\u0108\u0627\u0001\u0000\u0000"+
		"\u0000\u010a\u062c\u0001\u0000\u0000\u0000\u010c\u062e\u0001\u0000\u0000"+
		"\u0000\u010e\u063f\u0001\u0000\u0000\u0000\u0110\u0641\u0001\u0000\u0000"+
		"\u0000\u0112\u0649\u0001\u0000\u0000\u0000\u0114\u064c\u0001\u0000\u0000"+
		"\u0000\u0116\u064e\u0001\u0000\u0000\u0000\u0118\u0656\u0001\u0000\u0000"+
		"\u0000\u011a\u065e\u0001\u0000\u0000\u0000\u011c\u06a9\u0001\u0000\u0000"+
		"\u0000\u011e\u06ae\u0001\u0000\u0000\u0000\u0120\u06b0\u0001\u0000\u0000"+
		"\u0000\u0122\u06b6\u0001\u0000\u0000\u0000\u0124\u06ba\u0001\u0000\u0000"+
		"\u0000\u0126\u06bc\u0001\u0000\u0000\u0000\u0128\u06c4\u0001\u0000\u0000"+
		"\u0000\u012a\u06ce\u0001\u0000\u0000\u0000\u012c\u06d0\u0001\u0000\u0000"+
		"\u0000\u012e\u06d8\u0001\u0000\u0000\u0000\u0130\u06e5\u0001\u0000\u0000"+
		"\u0000\u0132\u06ec\u0001\u0000\u0000\u0000\u0134\u06f0\u0001\u0000\u0000"+
		"\u0000\u0136\u06f4\u0001\u0000\u0000\u0000\u0138\u06f8\u0001\u0000\u0000"+
		"\u0000\u013a\u06fc\u0001\u0000\u0000\u0000\u013c\u06fe\u0001\u0000\u0000"+
		"\u0000\u013e\u0712\u0001\u0000\u0000\u0000\u0140\u0714\u0001\u0000\u0000"+
		"\u0000\u0142\u0716\u0001\u0000\u0000\u0000\u0144\u071a\u0001\u0000\u0000"+
		"\u0000\u0146\u071c\u0001\u0000\u0000\u0000\u0148\u0727\u0001\u0000\u0000"+
		"\u0000\u014a\u072b\u0001\u0000\u0000\u0000\u014c\u072d\u0001\u0000\u0000"+
		"\u0000\u014e\u072f\u0001\u0000\u0000\u0000\u0150\u0731\u0001\u0000\u0000"+
		"\u0000\u0152\u0157\u0003\u0002\u0001\u0000\u0153\u0154\u0005\u00f7\u0000"+
		"\u0000\u0154\u0156\u0003\u0002\u0001\u0000\u0155\u0153\u0001\u0000\u0000"+
		"\u0000\u0156\u0159\u0001\u0000\u0000\u0000\u0157\u0155\u0001\u0000\u0000"+
		"\u0000\u0157\u0158\u0001\u0000\u0000\u0000\u0158\u015b\u0001\u0000\u0000"+
		"\u0000\u0159\u0157\u0001\u0000\u0000\u0000\u015a\u015c\u0005\u00f7\u0000"+
		"\u0000\u015b\u015a\u0001\u0000\u0000\u0000\u015b\u015c\u0001\u0000\u0000"+
		"\u0000\u015c\u015d\u0001\u0000\u0000\u0000\u015d\u015e\u0005\u0000\u0000"+
		"\u0001\u015e\u0001\u0001\u0000\u0000\u0000\u015f\u0161\u0003\u0004\u0002"+
		"\u0000\u0160\u015f\u0001\u0000\u0000\u0000\u0160\u0161\u0001\u0000\u0000"+
		"\u0000\u0161\u0162\u0001\u0000\u0000\u0000\u0162\u0163\u0003\u0006\u0003"+
		"\u0000\u0163\u0003\u0001\u0000\u0000\u0000\u0164\u0165\u0005\u0125\u0000"+
		"\u0000\u0165\u0166\u0005\u00c5\u0000\u0000\u0166\u0168\u00054\u0000\u0000"+
		"\u0167\u0169\u0005\u0005\u0000\u0000\u0168\u0167\u0001\u0000\u0000\u0000"+
		"\u0168\u0169\u0001\u0000\u0000\u0000\u0169\u0005\u0001\u0000\u0000\u0000"+
		"\u016a\u0172\u0003\b\u0004\u0000\u016b\u016d\u0005\u011d\u0000\u0000\u016c"+
		"\u016e\u0007\u0000\u0000\u0000\u016d\u016c\u0001\u0000\u0000\u0000\u016d"+
		"\u016e\u0001\u0000\u0000\u0000\u016e\u016f\u0001\u0000\u0000\u0000\u016f"+
		"\u0171\u0003\b\u0004\u0000\u0170\u016b\u0001\u0000\u0000\u0000\u0171\u0174"+
		"\u0001\u0000\u0000\u0000\u0172\u0170\u0001\u0000\u0000\u0000\u0172\u0173"+
		"\u0001\u0000\u0000\u0000\u0173\u0007\u0001\u0000\u0000\u0000\u0174\u0172"+
		"\u0001\u0000\u0000\u0000\u0175\u0177\u0003\n\u0005\u0000\u0176\u0175\u0001"+
		"\u0000\u0000\u0000\u0177\u0178\u0001\u0000\u0000\u0000\u0178\u0176\u0001"+
		"\u0000\u0000\u0000\u0178\u0179\u0001\u0000\u0000\u0000\u0179\t\u0001\u0000"+
		"\u0000\u0000\u017a\u018c\u0003\f\u0006\u0000\u017b\u018c\u0003\u0010\b"+
		"\u0000\u017c\u018c\u0003\u0012\t\u0000\u017d\u018c\u0003*\u0015\u0000"+
		"\u017e\u018c\u0003,\u0016\u0000\u017f\u018c\u00036\u001b\u0000\u0180\u018c"+
		"\u0003.\u0017\u0000\u0181\u018c\u00032\u0019\u0000\u0182\u018c\u00038"+
		"\u001c\u0000\u0183\u018c\u0003>\u001f\u0000\u0184\u018c\u0003(\u0014\u0000"+
		"\u0185\u018c\u0003B!\u0000\u0186\u018c\u0003D\"\u0000\u0187\u018c\u0003"+
		"P(\u0000\u0188\u018c\u0003L&\u0000\u0189\u018c\u0003N\'\u0000\u018a\u018c"+
		"\u0003\\.\u0000\u018b\u017a\u0001\u0000\u0000\u0000\u018b\u017b\u0001"+
		"\u0000\u0000\u0000\u018b\u017c\u0001\u0000\u0000\u0000\u018b\u017d\u0001"+
		"\u0000\u0000\u0000\u018b\u017e\u0001\u0000\u0000\u0000\u018b\u017f\u0001"+
		"\u0000\u0000\u0000\u018b\u0180\u0001\u0000\u0000\u0000\u018b\u0181\u0001"+
		"\u0000\u0000\u0000\u018b\u0182\u0001\u0000\u0000\u0000\u018b\u0183\u0001"+
		"\u0000\u0000\u0000\u018b\u0184\u0001\u0000\u0000\u0000\u018b\u0185\u0001"+
		"\u0000\u0000\u0000\u018b\u0186\u0001\u0000\u0000\u0000\u018b\u0187\u0001"+
		"\u0000\u0000\u0000\u018b\u0188\u0001\u0000\u0000\u0000\u018b\u0189\u0001"+
		"\u0000\u0000\u0000\u018b\u018a\u0001\u0000\u0000\u0000\u018c\u000b\u0001"+
		"\u0000\u0000\u0000\u018d\u018f\u0005\u0122\u0000\u0000\u018e\u0190\u0005"+
		"x\u0000\u0000\u018f\u018e\u0001\u0000\u0000\u0000\u018f\u0190\u0001\u0000"+
		"\u0000\u0000\u0190\u0191\u0001\u0000\u0000\u0000\u0191\u0192\u0003\u000e"+
		"\u0007\u0000\u0192\r\u0001\u0000\u0000\u0000\u0193\u0194\u0005\u009b\u0000"+
		"\u0000\u0194\u0195\u0003\u000e\u0007\u0000\u0195\u0196\u0005\u00ef\u0000"+
		"\u0000\u0196\u019a\u0001\u0000\u0000\u0000\u0197\u019a\u0003\u010c\u0086"+
		"\u0000\u0198\u019a\u0003\u012c\u0096\u0000\u0199\u0193\u0001\u0000\u0000"+
		"\u0000\u0199\u0197\u0001\u0000\u0000\u0000\u0199\u0198\u0001\u0000\u0000"+
		"\u0000\u019a\u000f\u0001\u0000\u0000\u0000\u019b\u019c\u0005n\u0000\u0000"+
		"\u019c\u0011\u0001\u0000\u0000\u0000\u019d\u019e\u0005\u00e9\u0000\u0000"+
		"\u019e\u019f\u0003\u0014\n\u0000\u019f\u0013\u0001\u0000\u0000\u0000\u01a0"+
		"\u01a2\u0005Q\u0000\u0000\u01a1\u01a0\u0001\u0000\u0000\u0000\u01a1\u01a2"+
		"\u0001\u0000\u0000\u0000\u01a2\u01a3\u0001\u0000\u0000\u0000\u01a3\u01a5"+
		"\u0003\u0018\f\u0000\u01a4\u01a6\u0003 \u0010\u0000\u01a5\u01a4\u0001"+
		"\u0000\u0000\u0000\u01a5\u01a6\u0001\u0000\u0000\u0000\u01a6\u01a8\u0001"+
		"\u0000\u0000\u0000\u01a7\u01a9\u0003\"\u0011\u0000\u01a8\u01a7\u0001\u0000"+
		"\u0000\u0000\u01a8\u01a9\u0001\u0000\u0000\u0000\u01a9\u01ab\u0001\u0000"+
		"\u0000\u0000\u01aa\u01ac\u0003$\u0012\u0000\u01ab\u01aa\u0001\u0000\u0000"+
		"\u0000\u01ab\u01ac\u0001\u0000\u0000\u0000\u01ac\u0015\u0001\u0000\u0000"+
		"\u0000\u01ad\u01b0\u0003\u00b0X\u0000\u01ae\u01af\u0005\u0017\u0000\u0000"+
		"\u01af\u01b1\u0003\u0114\u008a\u0000\u01b0\u01ae\u0001\u0000\u0000\u0000"+
		"\u01b0\u01b1\u0001\u0000\u0000\u0000\u01b1\u0017\u0001\u0000\u0000\u0000"+
		"\u01b2\u01b5\u0005\u010f\u0000\u0000\u01b3\u01b5\u0003\u0016\u000b\u0000"+
		"\u01b4\u01b2\u0001\u0000\u0000\u0000\u01b4\u01b3\u0001\u0000\u0000\u0000"+
		"\u01b5\u01ba\u0001\u0000\u0000\u0000\u01b6\u01b7\u00051\u0000\u0000\u01b7"+
		"\u01b9\u0003\u0016\u000b\u0000\u01b8\u01b6\u0001\u0000\u0000\u0000\u01b9"+
		"\u01bc\u0001\u0000\u0000\u0000\u01ba\u01b8\u0001\u0000\u0000\u0000\u01ba"+
		"\u01bb\u0001\u0000\u0000\u0000\u01bb\u0019\u0001\u0000\u0000\u0000\u01bc"+
		"\u01ba\u0001\u0000\u0000\u0000\u01bd\u01c0\u0003\u00b0X\u0000\u01be\u01c1"+
		"\u0003\u001c\u000e\u0000\u01bf\u01c1\u0003\u001e\u000f\u0000\u01c0\u01be"+
		"\u0001\u0000\u0000\u0000\u01c0\u01bf\u0001\u0000\u0000\u0000\u01c0\u01c1"+
		"\u0001\u0000\u0000\u0000\u01c1\u001b\u0001\u0000\u0000\u0000\u01c2\u01c3"+
		"\u0007\u0001\u0000\u0000\u01c3\u001d\u0001\u0000\u0000\u0000\u01c4\u01c5"+
		"\u0007\u0002\u0000\u0000\u01c5\u001f\u0001\u0000\u0000\u0000\u01c6\u01c7"+
		"\u0005\u00bf\u0000\u0000\u01c7\u01c8\u0005(\u0000\u0000\u01c8\u01cd\u0003"+
		"\u001a\r\u0000\u01c9\u01ca\u00051\u0000\u0000\u01ca\u01cc\u0003\u001a"+
		"\r\u0000\u01cb\u01c9\u0001\u0000\u0000\u0000\u01cc\u01cf\u0001\u0000\u0000"+
		"\u0000\u01cd\u01cb\u0001\u0000\u0000\u0000\u01cd\u01ce\u0001\u0000\u0000"+
		"\u0000\u01ce!\u0001\u0000\u0000\u0000\u01cf\u01cd\u0001\u0000\u0000\u0000"+
		"\u01d0\u01d1\u0007\u0003\u0000\u0000\u01d1\u01d2\u0003\u00b0X\u0000\u01d2"+
		"#\u0001\u0000\u0000\u0000\u01d3\u01d4\u0005\u0096\u0000\u0000\u01d4\u01d5"+
		"\u0003\u00b0X\u0000\u01d5%\u0001\u0000\u0000\u0000\u01d6\u01d7\u0005\u012d"+
		"\u0000\u0000\u01d7\u01d8\u0003\u00b0X\u0000\u01d8\'\u0001\u0000\u0000"+
		"\u0000\u01d9\u01da\u0005\u012e\u0000\u0000\u01da\u01dc\u0003\u0014\n\u0000"+
		"\u01db\u01dd\u0003&\u0013\u0000\u01dc\u01db\u0001\u0000\u0000\u0000\u01dc"+
		"\u01dd\u0001\u0000\u0000\u0000\u01dd)\u0001\u0000\u0000\u0000\u01de\u01df"+
		"\u0005=\u0000\u0000\u01df\u01e0\u0003^/\u0000\u01e0+\u0001\u0000\u0000"+
		"\u0000\u01e1\u01e2\u0005\u0088\u0000\u0000\u01e2\u01e3\u0003`0\u0000\u01e3"+
		"-\u0001\u0000\u0000\u0000\u01e4\u01e5\u0005\u00fa\u0000\u0000\u01e5\u01ea"+
		"\u00030\u0018\u0000\u01e6\u01e7\u00051\u0000\u0000\u01e7\u01e9\u00030"+
		"\u0018\u0000\u01e8\u01e6\u0001\u0000\u0000\u0000\u01e9\u01ec\u0001\u0000"+
		"\u0000\u0000\u01ea\u01e8\u0001\u0000\u0000\u0000\u01ea\u01eb\u0001\u0000"+
		"\u0000\u0000\u01eb/\u0001\u0000\u0000\u0000\u01ec\u01ea\u0001\u0000\u0000"+
		"\u0000\u01ed\u01ee\u0003\u00d0h\u0000\u01ee\u01ef\u0005d\u0000\u0000\u01ef"+
		"\u01f0\u0003\u00b0X\u0000\u01f0\u0204\u0001\u0000\u0000\u0000\u01f1\u01f2"+
		"\u0003\u00d2i\u0000\u01f2\u01f3\u0005d\u0000\u0000\u01f3\u01f4\u0003\u00b0"+
		"X\u0000\u01f4\u0204\u0001\u0000\u0000\u0000\u01f5\u01f6\u0003\u0114\u008a"+
		"\u0000\u01f6\u01f7\u0005d\u0000\u0000\u01f7\u01f8\u0003\u00b0X\u0000\u01f8"+
		"\u0204\u0001\u0000\u0000\u0000\u01f9\u01fa\u0003\u0114\u008a\u0000\u01fa"+
		"\u01fb\u0005\u00c8\u0000\u0000\u01fb\u01fc\u0003\u00b0X\u0000\u01fc\u0204"+
		"\u0001\u0000\u0000\u0000\u01fd\u01fe\u0003\u0114\u008a\u0000\u01fe\u01ff"+
		"\u0003|>\u0000\u01ff\u0204\u0001\u0000\u0000\u0000\u0200\u0201\u0003\u0114"+
		"\u008a\u0000\u0201\u0202\u0003~?\u0000\u0202\u0204\u0001\u0000\u0000\u0000"+
		"\u0203\u01ed\u0001\u0000\u0000\u0000\u0203\u01f1\u0001\u0000\u0000\u0000"+
		"\u0203\u01f5\u0001\u0000\u0000\u0000\u0203\u01f9\u0001\u0000\u0000\u0000"+
		"\u0203\u01fd\u0001\u0000\u0000\u0000\u0203\u0200\u0001\u0000\u0000\u0000"+
		"\u02041\u0001\u0000\u0000\u0000\u0205\u0206\u0005\u00e2\u0000\u0000\u0206"+
		"\u020b\u00034\u001a\u0000\u0207\u0208\u00051\u0000\u0000\u0208\u020a\u0003"+
		"4\u001a\u0000\u0209\u0207\u0001\u0000\u0000\u0000\u020a\u020d\u0001\u0000"+
		"\u0000\u0000\u020b\u0209\u0001\u0000\u0000\u0000\u020b\u020c\u0001\u0000"+
		"\u0000\u0000\u020c3\u0001\u0000\u0000\u0000\u020d\u020b\u0001\u0000\u0000"+
		"\u0000\u020e\u0217\u0003\u00d0h\u0000\u020f\u0217\u0003\u00d2i\u0000\u0210"+
		"\u0211\u0003\u0114\u008a\u0000\u0211\u0212\u0003|>\u0000\u0212\u0217\u0001"+
		"\u0000\u0000\u0000\u0213\u0214\u0003\u0114\u008a\u0000\u0214\u0215\u0003"+
		"~?\u0000\u0215\u0217\u0001\u0000\u0000\u0000\u0216\u020e\u0001\u0000\u0000"+
		"\u0000\u0216\u020f\u0001\u0000\u0000\u0000\u0216\u0210\u0001\u0000\u0000"+
		"\u0000\u0216\u0213\u0001\u0000\u0000\u0000\u02175\u0001\u0000\u0000\u0000"+
		"\u0218\u021a\u0007\u0004\u0000\u0000\u0219\u0218\u0001\u0000\u0000\u0000"+
		"\u0219\u021a\u0001\u0000\u0000\u0000\u021a\u021b\u0001\u0000\u0000\u0000"+
		"\u021b\u021c\u0005I\u0000\u0000\u021c\u0221\u0003\u00b0X\u0000\u021d\u021e"+
		"\u00051\u0000\u0000\u021e\u0220\u0003\u00b0X\u0000\u021f\u021d\u0001\u0000"+
		"\u0000\u0000\u0220\u0223\u0001\u0000\u0000\u0000\u0221\u021f\u0001\u0000"+
		"\u0000\u0000\u0221\u0222\u0001\u0000\u0000\u0000\u02227\u0001\u0000\u0000"+
		"\u0000\u0223\u0221\u0001\u0000\u0000\u0000\u0224\u0226\u0005\u00bb\u0000"+
		"\u0000\u0225\u0224\u0001\u0000\u0000\u0000\u0225\u0226\u0001\u0000\u0000"+
		"\u0000\u0226\u0227\u0001\u0000\u0000\u0000\u0227\u0229\u0005\u009f\u0000"+
		"\u0000\u0228\u022a\u0003:\u001d\u0000\u0229\u0228\u0001\u0000\u0000\u0000"+
		"\u0229\u022a\u0001\u0000\u0000\u0000\u022a\u022b\u0001\u0000\u0000\u0000"+
		"\u022b\u022f\u0003^/\u0000\u022c\u022e\u0003<\u001e\u0000\u022d\u022c"+
		"\u0001\u0000\u0000\u0000\u022e\u0231\u0001\u0000\u0000\u0000\u022f\u022d"+
		"\u0001\u0000\u0000\u0000\u022f\u0230\u0001\u0000\u0000\u0000\u0230\u0233"+
		"\u0001\u0000\u0000\u0000\u0231\u022f\u0001\u0000\u0000\u0000\u0232\u0234"+
		"\u0003&\u0013\u0000\u0233\u0232\u0001\u0000\u0000\u0000\u0233\u0234\u0001"+
		"\u0000\u0000\u0000\u02349\u0001\u0000\u0000\u0000\u0235\u023b\u0005\u00e3"+
		"\u0000\u0000\u0236\u0238\u0005^\u0000\u0000\u0237\u0239\u0005\u001f\u0000"+
		"\u0000\u0238\u0237\u0001\u0000\u0000\u0000\u0238\u0239\u0001\u0000\u0000"+
		"\u0000\u0239\u023c\u0001\u0000\u0000\u0000\u023a\u023c\u0005_\u0000\u0000"+
		"\u023b\u0236\u0001\u0000\u0000\u0000\u023b\u023a\u0001\u0000\u0000\u0000"+
		"\u023c\u0246\u0001\u0000\u0000\u0000\u023d\u0243\u0005O\u0000\u0000\u023e"+
		"\u0240\u0005\u00e0\u0000\u0000\u023f\u0241\u0005\u001f\u0000\u0000\u0240"+
		"\u023f\u0001\u0000\u0000\u0000\u0240\u0241\u0001\u0000\u0000\u0000\u0241"+
		"\u0244\u0001\u0000\u0000\u0000\u0242\u0244\u0005\u00e1\u0000\u0000\u0243"+
		"\u023e\u0001\u0000\u0000\u0000\u0243\u0242\u0001\u0000\u0000\u0000\u0244"+
		"\u0246\u0001\u0000\u0000\u0000\u0245\u0235\u0001\u0000\u0000\u0000\u0245"+
		"\u023d\u0001\u0000\u0000\u0000\u0246;\u0001\u0000\u0000\u0000\u0247\u0263"+
		"\u0005\u0125\u0000\u0000\u0248\u0252\u0005\u0084\u0000\u0000\u0249\u024a"+
		"\u0005&\u0000\u0000\u024a\u0252\u0005\u0084\u0000\u0000\u024b\u024c\u0005"+
		"\u010c\u0000\u0000\u024c\u0252\u0005\u0084\u0000\u0000\u024d\u024e\u0005"+
		"\u00d7\u0000\u0000\u024e\u0252\u0005\u0084\u0000\u0000\u024f\u0250\u0005"+
		"\u00c9\u0000\u0000\u0250\u0252\u0005\u0084\u0000\u0000\u0251\u0248\u0001"+
		"\u0000\u0000\u0000\u0251\u0249\u0001\u0000\u0000\u0000\u0251\u024b\u0001"+
		"\u0000\u0000\u0000\u0251\u024d\u0001\u0000\u0000\u0000\u0251\u024f\u0001"+
		"\u0000\u0000\u0000\u0252\u0254\u0001\u0000\u0000\u0000\u0253\u0255\u0005"+
		"\u00f6\u0000\u0000\u0254\u0253\u0001\u0000\u0000\u0000\u0254\u0255\u0001"+
		"\u0000\u0000\u0000\u0255\u0256\u0001\u0000\u0000\u0000\u0256\u0257\u0003"+
		"\u0114\u008a\u0000\u0257\u0258\u0003\u008aE\u0000\u0258\u0259\u0005\u009b"+
		"\u0000\u0000\u0259\u025a\u0003\u0116\u008b\u0000\u025a\u025b\u0005\u00ef"+
		"\u0000\u0000\u025b\u0264\u0001\u0000\u0000\u0000\u025c\u025d\u0005\u008c"+
		"\u0000\u0000\u025d\u025e\u0005\u00b9\u0000\u0000\u025e\u0264\u0003\u0116"+
		"\u008b\u0000\u025f\u0260\u0005\u00f0\u0000\u0000\u0260\u0261\u0003\u0114"+
		"\u008a\u0000\u0261\u0262\u0003\u008aE\u0000\u0262\u0264\u0001\u0000\u0000"+
		"\u0000\u0263\u0251\u0001\u0000\u0000\u0000\u0263\u025c\u0001\u0000\u0000"+
		"\u0000\u0263\u025f\u0001\u0000\u0000\u0000\u0264=\u0001\u0000\u0000\u0000"+
		"\u0265\u0266\u0005\u00a0\u0000\u0000\u0266\u026a\u0003b1\u0000\u0267\u0269"+
		"\u0003@ \u0000\u0268\u0267\u0001\u0000\u0000\u0000\u0269\u026c\u0001\u0000"+
		"\u0000\u0000\u026a\u0268\u0001\u0000\u0000\u0000\u026a\u026b\u0001\u0000"+
		"\u0000\u0000\u026b?\u0001\u0000\u0000\u0000\u026c\u026a\u0001\u0000\u0000"+
		"\u0000\u026d\u026e\u0005\u00b9\u0000\u0000\u026e\u026f\u0007\u0005\u0000"+
		"\u0000\u026f\u0270\u0003.\u0017\u0000\u0270A\u0001\u0000\u0000\u0000\u0271"+
		"\u0272\u0005\u0120\u0000\u0000\u0272\u0273\u0003\u00b0X\u0000\u0273\u0274"+
		"\u0005\u0017\u0000\u0000\u0274\u0275\u0003\u0114\u008a\u0000\u0275C\u0001"+
		"\u0000\u0000\u0000\u0276\u0278\u0005\u00bb\u0000\u0000\u0277\u0276\u0001"+
		"\u0000\u0000\u0000\u0277\u0278\u0001\u0000\u0000\u0000\u0278\u0279\u0001"+
		"\u0000\u0000\u0000\u0279\u027a\u0005)\u0000\u0000\u027a\u0287\u0003F#"+
		"\u0000\u027b\u0284\u0005\u009b\u0000\u0000\u027c\u0281\u0003H$\u0000\u027d"+
		"\u027e\u00051\u0000\u0000\u027e\u0280\u0003H$\u0000\u027f\u027d\u0001"+
		"\u0000\u0000\u0000\u0280\u0283\u0001\u0000\u0000\u0000\u0281\u027f\u0001"+
		"\u0000\u0000\u0000\u0281\u0282\u0001\u0000\u0000\u0000\u0282\u0285\u0001"+
		"\u0000\u0000\u0000\u0283\u0281\u0001\u0000\u0000\u0000\u0284\u027c\u0001"+
		"\u0000\u0000\u0000\u0284\u0285\u0001\u0000\u0000\u0000\u0285\u0286\u0001"+
		"\u0000\u0000\u0000\u0286\u0288\u0005\u00ef\u0000\u0000\u0287\u027b\u0001"+
		"\u0000\u0000\u0000\u0287\u0288\u0001\u0000\u0000\u0000\u0288\u0298\u0001"+
		"\u0000\u0000\u0000\u0289\u0296\u0005\u0132\u0000\u0000\u028a\u0297\u0005"+
		"\u010f\u0000\u0000\u028b\u0290\u0003J%\u0000\u028c\u028d\u00051\u0000"+
		"\u0000\u028d\u028f\u0003J%\u0000\u028e\u028c\u0001\u0000\u0000\u0000\u028f"+
		"\u0292\u0001\u0000\u0000\u0000\u0290\u028e\u0001\u0000\u0000\u0000\u0290"+
		"\u0291\u0001\u0000\u0000\u0000\u0291\u0294\u0001\u0000\u0000\u0000\u0292"+
		"\u0290\u0001\u0000\u0000\u0000\u0293\u0295\u0003&\u0013\u0000\u0294\u0293"+
		"\u0001\u0000\u0000\u0000\u0294\u0295\u0001\u0000\u0000\u0000\u0295\u0297"+
		"\u0001\u0000\u0000\u0000\u0296\u028a\u0001\u0000\u0000\u0000\u0296\u028b"+
		"\u0001\u0000\u0000\u0000\u0297\u0299\u0001\u0000\u0000\u0000\u0298\u0289"+
		"\u0001\u0000\u0000\u0000\u0298\u0299\u0001\u0000\u0000\u0000\u0299E\u0001"+
		"\u0000\u0000\u0000\u029a\u029b\u0003\u0112\u0089\u0000\u029b\u029c\u0003"+
		"\u0144\u00a2\u0000\u029cG\u0001\u0000\u0000\u0000\u029d\u029e\u0003\u00b0"+
		"X\u0000\u029eI\u0001\u0000\u0000\u0000\u029f\u02a2\u0003\u0144\u00a2\u0000"+
		"\u02a0\u02a1\u0005\u0017\u0000\u0000\u02a1\u02a3\u0003\u0114\u008a\u0000"+
		"\u02a2\u02a0\u0001\u0000\u0000\u0000\u02a2\u02a3\u0001\u0000\u0000\u0000"+
		"\u02a3K\u0001\u0000\u0000\u0000\u02a4\u02a5\u0005\u0098\u0000\u0000\u02a5"+
		"\u02a8\u0005>\u0000\u0000\u02a6\u02a7\u0005\u012e\u0000\u0000\u02a7\u02a9"+
		"\u0005}\u0000\u0000\u02a8\u02a6\u0001\u0000\u0000\u0000\u02a8\u02a9\u0001"+
		"\u0000\u0000\u0000\u02a9\u02aa\u0001\u0000\u0000\u0000\u02aa\u02ab\u0005"+
		"r\u0000\u0000\u02ab\u02ac\u0003\u00b0X\u0000\u02ac\u02ad\u0005\u0017\u0000"+
		"\u0000\u02ad\u02b0\u0003\u0114\u008a\u0000\u02ae\u02af\u0005m\u0000\u0000"+
		"\u02af\u02b1\u0003\u0132\u0099\u0000\u02b0\u02ae\u0001\u0000\u0000\u0000"+
		"\u02b0\u02b1\u0001\u0000\u0000\u0000\u02b1M\u0001\u0000\u0000\u0000\u02b2"+
		"\u02b3\u0005q\u0000\u0000\u02b3\u02b4\u0005\u009b\u0000\u0000\u02b4\u02b5"+
		"\u0003\u0114\u008a\u0000\u02b5\u02b6\u0005\u0083\u0000\u0000\u02b6\u02b7"+
		"\u0003\u00b0X\u0000\u02b7\u02b9\u0005\u001e\u0000\u0000\u02b8\u02ba\u0003"+
		"\n\u0005\u0000\u02b9\u02b8\u0001\u0000\u0000\u0000\u02ba\u02bb\u0001\u0000"+
		"\u0000\u0000\u02bb\u02b9\u0001\u0000\u0000\u0000\u02bb\u02bc\u0001\u0000"+
		"\u0000\u0000\u02bc\u02bd\u0001\u0000\u0000\u0000\u02bd\u02be\u0005\u00ef"+
		"\u0000\u0000\u02beO\u0001\u0000\u0000\u0000\u02bf\u02c1\u0005\u00bb\u0000"+
		"\u0000\u02c0\u02bf\u0001\u0000\u0000\u0000\u02c0\u02c1\u0001\u0000\u0000"+
		"\u0000\u02c1\u02c2\u0001\u0000\u0000\u0000\u02c2\u02c4\u0005)\u0000\u0000"+
		"\u02c3\u02c5\u0003R)\u0000\u02c4\u02c3\u0001\u0000\u0000\u0000\u02c4\u02c5"+
		"\u0001\u0000\u0000\u0000\u02c5\u02c6\u0001\u0000\u0000\u0000\u02c6\u02c7"+
		"\u0005\u0093\u0000\u0000\u02c7\u02c8\u0003\u0006\u0003\u0000\u02c8\u02ca"+
		"\u0005\u00d9\u0000\u0000\u02c9\u02cb\u0003T*\u0000\u02ca\u02c9\u0001\u0000"+
		"\u0000\u0000\u02ca\u02cb\u0001\u0000\u0000\u0000\u02cbQ\u0001\u0000\u0000"+
		"\u0000\u02cc\u02d6\u0005\u009b\u0000\u0000\u02cd\u02d7\u0005\u010f\u0000"+
		"\u0000\u02ce\u02d3\u0003\u0114\u008a\u0000\u02cf\u02d0\u00051\u0000\u0000"+
		"\u02d0\u02d2\u0003\u0114\u008a\u0000\u02d1\u02cf\u0001\u0000\u0000\u0000"+
		"\u02d2\u02d5\u0001\u0000\u0000\u0000\u02d3\u02d1\u0001\u0000\u0000\u0000"+
		"\u02d3\u02d4\u0001\u0000\u0000\u0000\u02d4\u02d7\u0001\u0000\u0000\u0000"+
		"\u02d5\u02d3\u0001\u0000\u0000\u0000\u02d6\u02cd\u0001\u0000\u0000\u0000"+
		"\u02d6\u02ce\u0001\u0000\u0000\u0000\u02d6\u02d7\u0001\u0000\u0000\u0000"+
		"\u02d7\u02d8\u0001\u0000\u0000\u0000\u02d8\u02d9\u0005\u00ef\u0000\u0000"+
		"\u02d9S\u0001\u0000\u0000\u0000\u02da\u02df\u0005\u0083\u0000\u0000\u02db"+
		"\u02dd\u0003\u00b0X\u0000\u02dc\u02db\u0001\u0000\u0000\u0000\u02dc\u02dd"+
		"\u0001\u0000\u0000\u0000\u02dd\u02de\u0001\u0000\u0000\u0000\u02de\u02e0"+
		"\u00056\u0000\u0000\u02df\u02dc\u0001\u0000\u0000\u0000\u02df\u02e0\u0001"+
		"\u0000\u0000\u0000\u02e0\u02e1\u0001\u0000\u0000\u0000\u02e1\u02e7\u0005"+
		"\u0116\u0000\u0000\u02e2\u02e6\u0003V+\u0000\u02e3\u02e6\u0003X,\u0000"+
		"\u02e4\u02e6\u0003Z-\u0000\u02e5\u02e2\u0001\u0000\u0000\u0000\u02e5\u02e3"+
		"\u0001\u0000\u0000\u0000\u02e5\u02e4\u0001\u0000\u0000\u0000\u02e6\u02e9"+
		"\u0001\u0000\u0000\u0000\u02e7\u02e5\u0001\u0000\u0000\u0000\u02e7\u02e8"+
		"\u0001\u0000\u0000\u0000\u02e8U\u0001\u0000\u0000\u0000\u02e9\u02e7\u0001"+
		"\u0000\u0000\u0000\u02ea\u02eb\u0005\u00b7\u0000\u0000\u02eb\u02ec\u0003"+
		"\u00b0X\u0000\u02ec\u02ed\u0007\u0006\u0000\u0000\u02edW\u0001\u0000\u0000"+
		"\u0000\u02ee\u02ef\u0005\u00b9\u0000\u0000\u02ef\u02f0\u0005j\u0000\u0000"+
		"\u02f0\u02f1\u0007\u0007\u0000\u0000\u02f1Y\u0001\u0000\u0000\u0000\u02f2"+
		"\u02f3\u0005\u00e5\u0000\u0000\u02f3\u02f4\u0005\u0105\u0000\u0000\u02f4"+
		"\u02f5\u0005\u0017\u0000\u0000\u02f5\u02f6\u0003\u0114\u008a\u0000\u02f6"+
		"[\u0001\u0000\u0000\u0000\u02f7\u02f9\u0003 \u0010\u0000\u02f8\u02fa\u0003"+
		"\"\u0011\u0000\u02f9\u02f8\u0001\u0000\u0000\u0000\u02f9\u02fa\u0001\u0000"+
		"\u0000\u0000\u02fa\u02fc\u0001\u0000\u0000\u0000\u02fb\u02fd\u0003$\u0012"+
		"\u0000\u02fc\u02fb\u0001\u0000\u0000\u0000\u02fc\u02fd\u0001\u0000\u0000"+
		"\u0000\u02fd\u0304\u0001\u0000\u0000\u0000\u02fe\u0300\u0003\"\u0011\u0000"+
		"\u02ff\u0301\u0003$\u0012\u0000\u0300\u02ff\u0001\u0000\u0000\u0000\u0300"+
		"\u0301\u0001\u0000\u0000\u0000\u0301\u0304\u0001\u0000\u0000\u0000\u0302"+
		"\u0304\u0003$\u0012\u0000\u0303\u02f7\u0001\u0000\u0000\u0000\u0303\u02fe"+
		"\u0001\u0000\u0000\u0000\u0303\u0302\u0001\u0000\u0000\u0000\u0304]\u0001"+
		"\u0000\u0000\u0000\u0305\u030a\u0003b1\u0000\u0306\u0307\u00051\u0000"+
		"\u0000\u0307\u0309\u0003b1\u0000\u0308\u0306\u0001\u0000\u0000\u0000\u0309"+
		"\u030c\u0001\u0000\u0000\u0000\u030a\u0308\u0001\u0000\u0000\u0000\u030a"+
		"\u030b\u0001\u0000\u0000\u0000\u030b_\u0001\u0000\u0000\u0000\u030c\u030a"+
		"\u0001\u0000\u0000\u0000\u030d\u0312\u0003d2\u0000\u030e\u030f\u00051"+
		"\u0000\u0000\u030f\u0311\u0003d2\u0000\u0310\u030e\u0001\u0000\u0000\u0000"+
		"\u0311\u0314\u0001\u0000\u0000\u0000\u0312\u0310\u0001\u0000\u0000\u0000"+
		"\u0312\u0313\u0001\u0000\u0000\u0000\u0313a\u0001\u0000\u0000\u0000\u0314"+
		"\u0312\u0001\u0000\u0000\u0000\u0315\u0316\u0003\u0114\u008a\u0000\u0316"+
		"\u0317\u0005d\u0000\u0000\u0317\u0319\u0001\u0000\u0000\u0000\u0318\u0315"+
		"\u0001\u0000\u0000\u0000\u0318\u0319\u0001\u0000\u0000\u0000\u0319\u031b"+
		"\u0001\u0000\u0000\u0000\u031a\u031c\u0003n7\u0000\u031b\u031a\u0001\u0000"+
		"\u0000\u0000\u031b\u031c\u0001\u0000\u0000\u0000\u031c\u031d\u0001\u0000"+
		"\u0000\u0000\u031d\u031e\u0003h4\u0000\u031ec\u0001\u0000\u0000\u0000"+
		"\u031f\u0320\u0003\u0144\u00a2\u0000\u0320\u0321\u0005d\u0000\u0000\u0321"+
		"\u0323\u0001\u0000\u0000\u0000\u0322\u031f\u0001\u0000\u0000\u0000\u0322"+
		"\u0323\u0001\u0000\u0000\u0000\u0323\u0324\u0001\u0000\u0000\u0000\u0324"+
		"\u032a\u0003x<\u0000\u0325\u0326\u0003\u0090H\u0000\u0326\u0327\u0003"+
		"x<\u0000\u0327\u0329\u0001\u0000\u0000\u0000\u0328\u0325\u0001\u0000\u0000"+
		"\u0000\u0329\u032c\u0001\u0000\u0000\u0000\u032a\u0328\u0001\u0000\u0000"+
		"\u0000\u032a\u032b\u0001\u0000\u0000\u0000\u032be\u0001\u0000\u0000\u0000"+
		"\u032c\u032a\u0001\u0000\u0000\u0000\u032d\u032e\u0005\u0093\u0000\u0000"+
		"\u032e\u032f\u0005\u0005\u0000\u0000\u032f\u033c\u0005\u00d9\u0000\u0000"+
		"\u0330\u0332\u0005\u0093\u0000\u0000\u0331\u0333\u0005\u0005\u0000\u0000"+
		"\u0332\u0331\u0001\u0000\u0000\u0000\u0332\u0333\u0001\u0000\u0000\u0000"+
		"\u0333\u0334\u0001\u0000\u0000\u0000\u0334\u0336\u00051\u0000\u0000\u0335"+
		"\u0337\u0005\u0005\u0000\u0000\u0336\u0335\u0001\u0000\u0000\u0000\u0336"+
		"\u0337\u0001\u0000\u0000\u0000\u0337\u0338\u0001\u0000\u0000\u0000\u0338"+
		"\u033c\u0005\u00d9\u0000\u0000\u0339\u033c\u0005\u00c7\u0000\u0000\u033a"+
		"\u033c\u0005\u010f\u0000\u0000\u033b\u032d\u0001\u0000\u0000\u0000\u033b"+
		"\u0330\u0001\u0000\u0000\u0000\u033b\u0339\u0001\u0000\u0000\u0000\u033b"+
		"\u033a\u0001\u0000\u0000\u0000\u033cg\u0001\u0000\u0000\u0000\u033d\u0340"+
		"\u0003j5\u0000\u033e\u0340\u0003l6\u0000\u033f\u033d\u0001\u0000\u0000"+
		"\u0000\u033f\u033e\u0001\u0000\u0000\u0000\u0340i\u0001\u0000\u0000\u0000"+
		"\u0341\u0342\u0007\b\u0000\u0000\u0342\u0343\u0005\u009b\u0000\u0000\u0343"+
		"\u0344\u0003l6\u0000\u0344\u0345\u0005\u00ef\u0000\u0000\u0345k\u0001"+
		"\u0000\u0000\u0000\u0346\u034f\u0003v;\u0000\u0347\u0349\u0003\u008eG"+
		"\u0000\u0348\u034a\u0003f3\u0000\u0349\u0348\u0001\u0000\u0000\u0000\u0349"+
		"\u034a\u0001\u0000\u0000\u0000\u034a\u034b\u0001\u0000\u0000\u0000\u034b"+
		"\u034c\u0003v;\u0000\u034c\u034e\u0001\u0000\u0000\u0000\u034d\u0347\u0001"+
		"\u0000\u0000\u0000\u034e\u0351\u0001\u0000\u0000\u0000\u034f\u034d\u0001"+
		"\u0000\u0000\u0000\u034f\u0350\u0001\u0000\u0000\u0000\u0350\u0354\u0001"+
		"\u0000\u0000\u0000\u0351\u034f\u0001\u0000\u0000\u0000\u0352\u0354\u0003"+
		"z=\u0000\u0353\u0346\u0001\u0000\u0000\u0000\u0353\u0352\u0001\u0000\u0000"+
		"\u0000\u0354\u0355\u0001\u0000\u0000\u0000\u0355\u0353\u0001\u0000\u0000"+
		"\u0000\u0355\u0356\u0001\u0000\u0000\u0000\u0356m\u0001\u0000\u0000\u0000"+
		"\u0357\u0358\u0005\u0015\u0000\u0000\u0358\u035a\u0005\u00fe\u0000\u0000"+
		"\u0359\u035b\u0003r9\u0000\u035a\u0359\u0001\u0000\u0000\u0000\u035a\u035b"+
		"\u0001\u0000\u0000\u0000\u035b\u037a\u0001\u0000\u0000\u0000\u035c\u035d"+
		"\u0005\u0012\u0000\u0000\u035d\u035f\u0005\u00fe\u0000\u0000\u035e\u0360"+
		"\u0003r9\u0000\u035f\u035e\u0001\u0000\u0000\u0000\u035f\u0360\u0001\u0000"+
		"\u0000\u0000\u0360\u037a\u0001\u0000\u0000\u0000\u0361\u0363\u0005\u0015"+
		"\u0000\u0000\u0362\u0364\u0005\u0005\u0000\u0000\u0363\u0362\u0001\u0000"+
		"\u0000\u0000\u0363\u0364\u0001\u0000\u0000\u0000\u0364\u0366\u0001\u0000"+
		"\u0000\u0000\u0365\u0367\u0003r9\u0000\u0366\u0365\u0001\u0000\u0000\u0000"+
		"\u0366\u0367\u0001\u0000\u0000\u0000\u0367\u037a\u0001\u0000\u0000\u0000"+
		"\u0368\u036a\u0005\u0012\u0000\u0000\u0369\u036b\u0003r9\u0000\u036a\u0369"+
		"\u0001\u0000\u0000\u0000\u036a\u036b\u0001\u0000\u0000\u0000\u036b\u037a"+
		"\u0001\u0000\u0000\u0000\u036c\u036e\u0005\u00fe\u0000\u0000\u036d\u036f"+
		"\u0005\u0005\u0000\u0000\u036e\u036d\u0001\u0000\u0000\u0000\u036e\u036f"+
		"\u0001\u0000\u0000\u0000\u036f\u0371\u0001\u0000\u0000\u0000\u0370\u0372"+
		"\u0003r9\u0000\u0371\u0370\u0001\u0000\u0000\u0000\u0371\u0372\u0001\u0000"+
		"\u0000\u0000\u0372\u0373\u0001\u0000\u0000\u0000\u0373\u037a\u0003p8\u0000"+
		"\u0374\u0375\u0005\u00fe\u0000\u0000\u0375\u0377\u0005\u0005\u0000\u0000"+
		"\u0376\u0378\u0003r9\u0000\u0377\u0376\u0001\u0000\u0000\u0000\u0377\u0378"+
		"\u0001\u0000\u0000\u0000\u0378\u037a\u0001\u0000\u0000\u0000\u0379\u0357"+
		"\u0001\u0000\u0000\u0000\u0379\u035c\u0001\u0000\u0000\u0000\u0379\u0361"+
		"\u0001\u0000\u0000\u0000\u0379\u0368\u0001\u0000\u0000\u0000\u0379\u036c"+
		"\u0001\u0000\u0000\u0000\u0379\u0374\u0001\u0000\u0000\u0000\u037ao\u0001"+
		"\u0000\u0000\u0000\u037b\u037c\u0007\t\u0000\u0000\u037cq\u0001\u0000"+
		"\u0000\u0000\u037d\u037e\u0007\n\u0000\u0000\u037es\u0001\u0000\u0000"+
		"\u0000\u037f\u0383\u0003v;\u0000\u0380\u0381\u0003\u008eG\u0000\u0381"+
		"\u0382\u0003v;\u0000\u0382\u0384\u0001\u0000\u0000\u0000\u0383\u0380\u0001"+
		"\u0000\u0000\u0000\u0384\u0385\u0001\u0000\u0000\u0000\u0385\u0383\u0001"+
		"\u0000\u0000\u0000\u0385\u0386\u0001\u0000\u0000\u0000\u0386u\u0001\u0000"+
		"\u0000\u0000\u0387\u0389\u0005\u009b\u0000\u0000\u0388\u038a\u0003\u0114"+
		"\u008a\u0000\u0389\u0388\u0001\u0000\u0000\u0000\u0389\u038a\u0001\u0000"+
		"\u0000\u0000\u038a\u038c\u0001\u0000\u0000\u0000\u038b\u038d\u0003\u009a"+
		"M\u0000\u038c\u038b\u0001\u0000\u0000\u0000\u038c\u038d\u0001\u0000\u0000"+
		"\u0000\u038d\u038f\u0001\u0000\u0000\u0000\u038e\u0390\u0003\u008cF\u0000"+
		"\u038f\u038e\u0001\u0000\u0000\u0000\u038f\u0390\u0001\u0000\u0000\u0000"+
		"\u0390\u0393\u0001\u0000\u0000\u0000\u0391\u0392\u0005\u012d\u0000\u0000"+
		"\u0392\u0394\u0003\u00b0X\u0000\u0393\u0391\u0001\u0000\u0000\u0000\u0393"+
		"\u0394\u0001\u0000\u0000\u0000\u0394\u0395\u0001\u0000\u0000\u0000\u0395"+
		"\u0396\u0005\u00ef\u0000\u0000\u0396w\u0001\u0000\u0000\u0000\u0397\u0399"+
		"\u0005\u009b\u0000\u0000\u0398\u039a\u0003\u0114\u008a\u0000\u0399\u0398"+
		"\u0001\u0000\u0000\u0000\u0399\u039a\u0001\u0000\u0000\u0000\u039a\u039c"+
		"\u0001\u0000\u0000\u0000\u039b\u039d\u0003\u00acV\u0000\u039c\u039b\u0001"+
		"\u0000\u0000\u0000\u039c\u039d\u0001\u0000\u0000\u0000\u039d\u039f\u0001"+
		"\u0000\u0000\u0000\u039e\u03a0\u0003\u013c\u009e\u0000\u039f\u039e\u0001"+
		"\u0000\u0000\u0000\u039f\u03a0\u0001\u0000\u0000\u0000\u03a0\u03a1\u0001"+
		"\u0000\u0000\u0000\u03a1\u03a2\u0005\u00ef\u0000\u0000\u03a2y\u0001\u0000"+
		"\u0000\u0000\u03a3\u03a4\u0005\u009b\u0000\u0000\u03a4\u03a7\u0003b1\u0000"+
		"\u03a5\u03a6\u0005\u012d\u0000\u0000\u03a6\u03a8\u0003\u00b0X\u0000\u03a7"+
		"\u03a5\u0001\u0000\u0000\u0000\u03a7\u03a8\u0001\u0000\u0000\u0000\u03a8"+
		"\u03a9\u0001\u0000\u0000\u0000\u03a9\u03ab\u0005\u00ef\u0000\u0000\u03aa"+
		"\u03ac\u0003f3\u0000\u03ab\u03aa\u0001\u0000\u0000\u0000\u03ab\u03ac\u0001"+
		"\u0000\u0000\u0000\u03ac{\u0001\u0000\u0000\u0000\u03ad\u03b0\u0003\u0086"+
		"C\u0000\u03ae\u03b0\u0003\u0084B\u0000\u03af\u03ad\u0001\u0000\u0000\u0000"+
		"\u03af\u03ae\u0001\u0000\u0000\u0000\u03b0\u03b1\u0001\u0000\u0000\u0000"+
		"\u03b1\u03af\u0001\u0000\u0000\u0000\u03b1\u03b2\u0001\u0000\u0000\u0000"+
		"\u03b2}\u0001\u0000\u0000\u0000\u03b3\u03b6\u0005\u008b\u0000\u0000\u03b4"+
		"\u03b7\u0003\u0144\u00a2\u0000\u03b5\u03b7\u0003\u0080@\u0000\u03b6\u03b4"+
		"\u0001\u0000\u0000\u0000\u03b6\u03b5\u0001\u0000\u0000\u0000\u03b7\u03bc"+
		"\u0001\u0000\u0000\u0000\u03b8\u03bb\u0003\u0086C\u0000\u03b9\u03bb\u0003"+
		"\u0084B\u0000\u03ba\u03b8\u0001\u0000\u0000\u0000\u03ba\u03b9\u0001\u0000"+
		"\u0000\u0000\u03bb\u03be\u0001\u0000\u0000\u0000\u03bc\u03ba\u0001\u0000"+
		"\u0000\u0000\u03bc\u03bd\u0001\u0000\u0000\u0000\u03bd\u007f\u0001\u0000"+
		"\u0000\u0000\u03be\u03bc\u0001\u0000\u0000\u0000\u03bf\u03c0\u0005P\u0000"+
		"\u0000\u03c0\u03c1\u0005\u009b\u0000\u0000\u03c1\u03c2\u0003\u00b0X\u0000"+
		"\u03c2\u03c3\u0005\u00ef\u0000\u0000\u03c3\u0081\u0001\u0000\u0000\u0000"+
		"\u03c4\u03c6\u0005P\u0000\u0000\u03c5\u03c7\u0007\u000b\u0000\u0000\u03c6"+
		"\u03c5\u0001\u0000\u0000\u0000\u03c6\u03c7\u0001\u0000\u0000\u0000\u03c7"+
		"\u03c8\u0001\u0000\u0000\u0000\u03c8\u03c9\u0005\u009b\u0000\u0000\u03c9"+
		"\u03ca\u0003\u00b0X\u0000\u03ca\u03cb\u0005\u00ef\u0000\u0000\u03cb\u0083"+
		"\u0001\u0000\u0000\u0000\u03cc\u03cd\u0005/\u0000\u0000\u03cd\u03ce\u0003"+
		"\u0080@\u0000\u03ce\u0085\u0001\u0000\u0000\u0000\u03cf\u03d0\u0005/\u0000"+
		"\u0000\u03d0\u03d1\u0003\u0144\u00a2\u0000\u03d1\u0087\u0001\u0000\u0000"+
		"\u0000\u03d2\u03d3\u0005/\u0000\u0000\u03d3\u03d4\u0003\u0144\u00a2\u0000"+
		"\u03d4\u0089\u0001\u0000\u0000\u0000\u03d5\u03d6\u0005/\u0000\u0000\u03d6"+
		"\u03d7\u0003\u0144\u00a2\u0000\u03d7\u008b\u0001\u0000\u0000\u0000\u03d8"+
		"\u03db\u0003\u013c\u009e\u0000\u03d9\u03db\u0003\u0108\u0084\u0000\u03da"+
		"\u03d8\u0001\u0000\u0000\u0000\u03da\u03d9\u0001\u0000\u0000\u0000\u03db"+
		"\u008d\u0001\u0000\u0000\u0000\u03dc\u03de\u0003\u0092I\u0000\u03dd\u03dc"+
		"\u0001\u0000\u0000\u0000\u03dd\u03de\u0001\u0000\u0000\u0000\u03de\u03df"+
		"\u0001\u0000\u0000\u0000\u03df\u03f2\u0003\u0094J\u0000\u03e0\u03e2\u0005"+
		"\u0092\u0000\u0000\u03e1\u03e3\u0003\u0114\u008a\u0000\u03e2\u03e1\u0001"+
		"\u0000\u0000\u0000\u03e2\u03e3\u0001\u0000\u0000\u0000\u03e3\u03e5\u0001"+
		"\u0000\u0000\u0000\u03e4\u03e6\u0003\u009aM\u0000\u03e5\u03e4\u0001\u0000"+
		"\u0000\u0000\u03e5\u03e6\u0001\u0000\u0000\u0000\u03e6\u03e8\u0001\u0000"+
		"\u0000\u0000\u03e7\u03e9\u0003\u0098L\u0000\u03e8\u03e7\u0001\u0000\u0000"+
		"\u0000\u03e8\u03e9\u0001\u0000\u0000\u0000\u03e9\u03eb\u0001\u0000\u0000"+
		"\u0000\u03ea\u03ec\u0003\u008cF\u0000\u03eb\u03ea\u0001\u0000\u0000\u0000"+
		"\u03eb\u03ec\u0001\u0000\u0000\u0000\u03ec\u03ef\u0001\u0000\u0000\u0000"+
		"\u03ed\u03ee\u0005\u012d\u0000\u0000\u03ee\u03f0\u0003\u00b0X\u0000\u03ef"+
		"\u03ed\u0001\u0000\u0000\u0000\u03ef\u03f0\u0001\u0000\u0000\u0000\u03f0"+
		"\u03f1\u0001\u0000\u0000\u0000\u03f1\u03f3\u0005\u00d8\u0000\u0000\u03f2"+
		"\u03e0\u0001\u0000\u0000\u0000\u03f2\u03f3\u0001\u0000\u0000\u0000\u03f3"+
		"\u03f4\u0001\u0000\u0000\u0000\u03f4\u03f6\u0003\u0094J\u0000\u03f5\u03f7"+
		"\u0003\u0096K\u0000\u03f6\u03f5\u0001\u0000\u0000\u0000\u03f6\u03f7\u0001"+
		"\u0000\u0000\u0000\u03f7\u008f\u0001\u0000\u0000\u0000\u03f8\u03fa\u0003"+
		"\u0092I\u0000\u03f9\u03f8\u0001\u0000\u0000\u0000\u03f9\u03fa\u0001\u0000"+
		"\u0000\u0000\u03fa\u03fb\u0001\u0000\u0000\u0000\u03fb\u03fc\u0003\u0094"+
		"J\u0000\u03fc\u03fe\u0005\u0092\u0000\u0000\u03fd\u03ff\u0003\u0114\u008a"+
		"\u0000\u03fe\u03fd\u0001\u0000\u0000\u0000\u03fe\u03ff\u0001\u0000\u0000"+
		"\u0000\u03ff\u0400\u0001\u0000\u0000\u0000\u0400\u0402\u0003\u00aeW\u0000"+
		"\u0401\u0403\u0003\u013c\u009e\u0000\u0402\u0401\u0001\u0000\u0000\u0000"+
		"\u0402\u0403\u0001\u0000\u0000\u0000\u0403\u0404\u0001\u0000\u0000\u0000"+
		"\u0404\u0405\u0005\u00d8\u0000\u0000\u0405\u0407\u0003\u0094J\u0000\u0406"+
		"\u0408\u0003\u0096K\u0000\u0407\u0406\u0001\u0000\u0000\u0000\u0407\u0408"+
		"\u0001\u0000\u0000\u0000\u0408\u0091\u0001\u0000\u0000\u0000\u0409\u040a"+
		"\u0007\f\u0000\u0000\u040a\u0093\u0001\u0000\u0000\u0000\u040b\u040c\u0007"+
		"\r\u0000\u0000\u040c\u0095\u0001\u0000\u0000\u0000\u040d\u040e\u0007\u000e"+
		"\u0000\u0000\u040e\u0097\u0001\u0000\u0000\u0000\u040f\u0418\u0005\u010f"+
		"\u0000\u0000\u0410\u0412\u0005\u0005\u0000\u0000\u0411\u0410\u0001\u0000"+
		"\u0000\u0000\u0411\u0412\u0001\u0000\u0000\u0000\u0412\u0413\u0001\u0000"+
		"\u0000\u0000\u0413\u0415\u0005T\u0000\u0000\u0414\u0416\u0005\u0005\u0000"+
		"\u0000\u0415\u0414\u0001\u0000\u0000\u0000\u0415\u0416\u0001\u0000\u0000"+
		"\u0000\u0416\u0419\u0001\u0000\u0000\u0000\u0417\u0419\u0005\u0005\u0000"+
		"\u0000\u0418\u0411\u0001\u0000\u0000\u0000\u0418\u0417\u0001\u0000\u0000"+
		"\u0000\u0418\u0419\u0001\u0000\u0000\u0000\u0419\u0099\u0001\u0000\u0000"+
		"\u0000\u041a\u041b\u0005/\u0000\u0000\u041b\u041f\u0003\u009cN\u0000\u041c"+
		"\u041d\u0005\u008b\u0000\u0000\u041d\u041f\u0003\u009eO\u0000\u041e\u041a"+
		"\u0001\u0000\u0000\u0000\u041e\u041c\u0001\u0000\u0000\u0000\u041f\u009b"+
		"\u0001\u0000\u0000\u0000\u0420\u0428\u0003\u00a0P\u0000\u0421\u0423\u0005"+
		"\u001e\u0000\u0000\u0422\u0424\u0005/\u0000\u0000\u0423\u0422\u0001\u0000"+
		"\u0000\u0000\u0423\u0424\u0001\u0000\u0000\u0000\u0424\u0425\u0001\u0000"+
		"\u0000\u0000\u0425\u0427\u0003\u00a0P\u0000\u0426\u0421\u0001\u0000\u0000"+
		"\u0000\u0427\u042a\u0001\u0000\u0000\u0000\u0428\u0426\u0001\u0000\u0000"+
		"\u0000\u0428\u0429\u0001\u0000\u0000\u0000\u0429\u009d\u0001\u0000\u0000"+
		"\u0000\u042a\u0428\u0001\u0000\u0000\u0000\u042b\u0433\u0003\u00a2Q\u0000"+
		"\u042c\u042e\u0005\u001e\u0000\u0000\u042d\u042f\u0005/\u0000\u0000\u042e"+
		"\u042d\u0001\u0000\u0000\u0000\u042e\u042f\u0001\u0000\u0000\u0000\u042f"+
		"\u0430\u0001\u0000\u0000\u0000\u0430\u0432\u0003\u00a2Q\u0000\u0431\u042c"+
		"\u0001\u0000\u0000\u0000\u0432\u0435\u0001\u0000\u0000\u0000\u0433\u0431"+
		"\u0001\u0000\u0000\u0000\u0433\u0434\u0001\u0000\u0000\u0000\u0434\u009f"+
		"\u0001\u0000\u0000\u0000\u0435\u0433\u0001\u0000\u0000\u0000\u0436\u043b"+
		"\u0003\u00a4R\u0000\u0437\u0438\u0007\u000f\u0000\u0000\u0438\u043a\u0003"+
		"\u00a4R\u0000\u0439\u0437\u0001\u0000\u0000\u0000\u043a\u043d\u0001\u0000"+
		"\u0000\u0000\u043b\u0439\u0001\u0000\u0000\u0000\u043b\u043c\u0001\u0000"+
		"\u0000\u0000\u043c\u00a1\u0001\u0000\u0000\u0000\u043d\u043b\u0001\u0000"+
		"\u0000\u0000\u043e\u0443\u0003\u00a6S\u0000\u043f\u0440\u0007\u000f\u0000"+
		"\u0000\u0440\u0442\u0003\u00a6S\u0000\u0441\u043f\u0001\u0000\u0000\u0000"+
		"\u0442\u0445\u0001\u0000\u0000\u0000\u0443\u0441\u0001\u0000\u0000\u0000"+
		"\u0443\u0444\u0001\u0000\u0000\u0000\u0444\u00a3\u0001\u0000\u0000\u0000"+
		"\u0445\u0443\u0001\u0000\u0000\u0000\u0446\u0448\u0005\u0091\u0000\u0000"+
		"\u0447\u0446\u0001\u0000\u0000\u0000\u0448\u044b\u0001\u0000\u0000\u0000"+
		"\u0449\u0447\u0001\u0000\u0000\u0000\u0449\u044a\u0001\u0000\u0000\u0000"+
		"\u044a\u044c\u0001\u0000\u0000\u0000\u044b\u0449\u0001\u0000\u0000\u0000"+
		"\u044c\u044d\u0003\u00a8T\u0000\u044d\u00a5\u0001\u0000\u0000\u0000\u044e"+
		"\u0450\u0005\u0091\u0000\u0000\u044f\u044e\u0001\u0000\u0000\u0000\u0450"+
		"\u0453\u0001\u0000\u0000\u0000\u0451\u044f\u0001\u0000\u0000\u0000\u0451"+
		"\u0452\u0001\u0000\u0000\u0000\u0452\u0454\u0001\u0000\u0000\u0000\u0453"+
		"\u0451\u0001\u0000\u0000\u0000\u0454\u0455\u0003\u00aaU\u0000\u0455\u00a7"+
		"\u0001\u0000\u0000\u0000\u0456\u0457\u0005\u009b\u0000\u0000\u0457\u0458"+
		"\u0003\u009cN\u0000\u0458\u0459\u0005\u00ef\u0000\u0000\u0459\u045e\u0001"+
		"\u0000\u0000\u0000\u045a\u045e\u0005\u00a2\u0000\u0000\u045b\u045e\u0003"+
		"\u0082A\u0000\u045c\u045e\u0003\u0144\u00a2\u0000\u045d\u0456\u0001\u0000"+
		"\u0000\u0000\u045d\u045a\u0001\u0000\u0000\u0000\u045d\u045b\u0001\u0000"+
		"\u0000\u0000\u045d\u045c\u0001\u0000\u0000\u0000\u045e\u00a9\u0001\u0000"+
		"\u0000\u0000\u045f\u0460\u0005\u009b\u0000\u0000\u0460\u0461\u0003\u009e"+
		"O\u0000\u0461\u0462\u0005\u00ef\u0000\u0000\u0462\u0467\u0001\u0000\u0000"+
		"\u0000\u0463\u0467\u0005\u00a2\u0000\u0000\u0464\u0467\u0003\u0082A\u0000"+
		"\u0465\u0467\u0003\u014a\u00a5\u0000\u0466\u045f\u0001\u0000\u0000\u0000"+
		"\u0466\u0463\u0001\u0000\u0000\u0000\u0466\u0464\u0001\u0000\u0000\u0000"+
		"\u0466\u0465\u0001\u0000\u0000\u0000\u0467\u00ab\u0001\u0000\u0000\u0000"+
		"\u0468\u0469\u0007\u0010\u0000\u0000\u0469\u046e\u0003\u0144\u00a2\u0000"+
		"\u046a\u046b\u0007\u000f\u0000\u0000\u046b\u046d\u0003\u0144\u00a2\u0000"+
		"\u046c\u046a\u0001\u0000\u0000\u0000\u046d\u0470\u0001\u0000\u0000\u0000"+
		"\u046e\u046c\u0001\u0000\u0000\u0000\u046e\u046f\u0001\u0000\u0000\u0000"+
		"\u046f\u00ad\u0001\u0000\u0000\u0000\u0470\u046e\u0001\u0000\u0000\u0000"+
		"\u0471\u0472\u0007\u0010\u0000\u0000\u0472\u0473\u0003\u0144\u00a2\u0000"+
		"\u0473\u00af\u0001\u0000\u0000\u0000\u0474\u0479\u0003\u00b2Y\u0000\u0475"+
		"\u0476\u0005\u00be\u0000\u0000\u0476\u0478\u0003\u00b2Y\u0000\u0477\u0475"+
		"\u0001\u0000\u0000\u0000\u0478\u047b\u0001\u0000\u0000\u0000\u0479\u0477"+
		"\u0001\u0000\u0000\u0000\u0479\u047a\u0001\u0000\u0000\u0000\u047a\u00b1"+
		"\u0001\u0000\u0000\u0000\u047b\u0479\u0001\u0000\u0000\u0000\u047c\u0481"+
		"\u0003\u00b4Z\u0000\u047d\u047e\u0005\u0131\u0000\u0000\u047e\u0480\u0003"+
		"\u00b4Z\u0000\u047f\u047d\u0001\u0000\u0000\u0000\u0480\u0483\u0001\u0000"+
		"\u0000\u0000\u0481\u047f\u0001\u0000\u0000\u0000\u0481\u0482\u0001\u0000"+
		"\u0000\u0000\u0482\u00b3\u0001\u0000\u0000\u0000\u0483\u0481\u0001\u0000"+
		"\u0000\u0000\u0484\u0489\u0003\u00b6[\u0000\u0485\u0486\u0005\u0014\u0000"+
		"\u0000\u0486\u0488\u0003\u00b6[\u0000\u0487\u0485\u0001\u0000\u0000\u0000"+
		"\u0488\u048b\u0001\u0000\u0000\u0000\u0489\u0487\u0001\u0000\u0000\u0000"+
		"\u0489\u048a\u0001\u0000\u0000\u0000\u048a\u00b5\u0001\u0000\u0000\u0000"+
		"\u048b\u0489\u0001\u0000\u0000\u0000\u048c\u048e\u0005\u00b3\u0000\u0000"+
		"\u048d\u048c\u0001\u0000\u0000\u0000\u048e\u0491\u0001\u0000\u0000\u0000"+
		"\u048f\u048d\u0001\u0000\u0000\u0000\u048f\u0490\u0001\u0000\u0000\u0000"+
		"\u0490\u0492\u0001\u0000\u0000\u0000\u0491\u048f\u0001\u0000\u0000\u0000"+
		"\u0492\u0493\u0003\u00b8\\\u0000\u0493\u00b7\u0001\u0000\u0000\u0000\u0494"+
		"\u0499\u0003\u00ba]\u0000\u0495\u0496\u0007\u0011\u0000\u0000\u0496\u0498"+
		"\u0003\u00ba]\u0000\u0497\u0495\u0001\u0000\u0000\u0000\u0498\u049b\u0001"+
		"\u0000\u0000\u0000\u0499\u0497\u0001\u0000\u0000\u0000\u0499\u049a\u0001"+
		"\u0000\u0000\u0000\u049a\u00b9\u0001\u0000\u0000\u0000\u049b\u0499\u0001"+
		"\u0000\u0000\u0000\u049c\u049e\u0003\u00c0`\u0000\u049d\u049f\u0003\u00bc"+
		"^\u0000\u049e\u049d\u0001\u0000\u0000\u0000\u049e\u049f\u0001\u0000\u0000"+
		"\u0000\u049f\u00bb\u0001\u0000\u0000\u0000\u04a0\u04a8\u0005\u00de\u0000"+
		"\u0000\u04a1\u04a2\u0005\u0104\u0000\u0000\u04a2\u04a8\u0005\u012e\u0000"+
		"\u0000\u04a3\u04a4\u0005c\u0000\u0000\u04a4\u04a8\u0005\u012e\u0000\u0000"+
		"\u04a5\u04a8\u00059\u0000\u0000\u04a6\u04a8\u0005\u0083\u0000\u0000\u04a7"+
		"\u04a0\u0001\u0000\u0000\u0000\u04a7\u04a1\u0001\u0000\u0000\u0000\u04a7"+
		"\u04a3\u0001\u0000\u0000\u0000\u04a7\u04a5\u0001\u0000\u0000\u0000\u04a7"+
		"\u04a6\u0001\u0000\u0000\u0000\u04a8\u04a9\u0001\u0000\u0000\u0000\u04a9"+
		"\u04c1\u0003\u00c0`\u0000\u04aa\u04ac\u0005\u008b\u0000\u0000\u04ab\u04ad"+
		"\u0005\u00b3\u0000\u0000\u04ac\u04ab\u0001\u0000\u0000\u0000\u04ac\u04ad"+
		"\u0001\u0000\u0000\u0000\u04ad\u04ae\u0001\u0000\u0000\u0000\u04ae\u04c1"+
		"\u0005\u00b6\u0000\u0000\u04af\u04b1\u0005\u008b\u0000\u0000\u04b0\u04b2"+
		"\u0005\u00b3\u0000\u0000\u04b1\u04b0\u0001\u0000\u0000\u0000\u04b1\u04b2"+
		"\u0001\u0000\u0000\u0000\u04b2\u04b3\u0001\u0000\u0000\u0000\u04b3\u04b6"+
		"\u0007\u0012\u0000\u0000\u04b4\u04b6\u00050\u0000\u0000\u04b5\u04af\u0001"+
		"\u0000\u0000\u0000\u04b5\u04b4\u0001\u0000\u0000\u0000\u04b6\u04b7\u0001"+
		"\u0000\u0000\u0000\u04b7\u04c1\u0003\u0118\u008c\u0000\u04b8\u04ba\u0005"+
		"\u008b\u0000\u0000\u04b9\u04bb\u0005\u00b3\u0000\u0000\u04ba\u04b9\u0001"+
		"\u0000\u0000\u0000\u04ba\u04bb\u0001\u0000\u0000\u0000\u04bb\u04bd\u0001"+
		"\u0000\u0000\u0000\u04bc\u04be\u0003\u00be_\u0000\u04bd\u04bc\u0001\u0000"+
		"\u0000\u0000\u04bd\u04be\u0001\u0000\u0000\u0000\u04be\u04bf\u0001\u0000"+
		"\u0000\u0000\u04bf\u04c1\u0005\u00b2\u0000\u0000\u04c0\u04a7\u0001\u0000"+
		"\u0000\u0000\u04c0\u04aa\u0001\u0000\u0000\u0000\u04c0\u04b5\u0001\u0000"+
		"\u0000\u0000\u04c0\u04b8\u0001\u0000\u0000\u0000\u04c1\u00bd\u0001\u0000"+
		"\u0000\u0000\u04c2\u04c3\u0007\u0013\u0000\u0000\u04c3\u00bf\u0001\u0000"+
		"\u0000\u0000\u04c4\u04c9\u0003\u00c2a\u0000\u04c5\u04c6\u0007\u0014\u0000"+
		"\u0000\u04c6\u04c8\u0003\u00c2a\u0000\u04c7\u04c5\u0001\u0000\u0000\u0000"+
		"\u04c8\u04cb\u0001\u0000\u0000\u0000\u04c9\u04c7\u0001\u0000\u0000\u0000"+
		"\u04c9\u04ca\u0001\u0000\u0000\u0000\u04ca\u00c1\u0001\u0000\u0000\u0000"+
		"\u04cb\u04c9\u0001\u0000\u0000\u0000\u04cc\u04d1\u0003\u00c4b\u0000\u04cd"+
		"\u04ce\u0007\u0015\u0000\u0000\u04ce\u04d0\u0003\u00c4b\u0000\u04cf\u04cd"+
		"\u0001\u0000\u0000\u0000\u04d0\u04d3\u0001\u0000\u0000\u0000\u04d1\u04cf"+
		"\u0001\u0000\u0000\u0000\u04d1\u04d2\u0001\u0000\u0000\u0000\u04d2\u00c3"+
		"\u0001\u0000\u0000\u0000\u04d3\u04d1\u0001\u0000\u0000\u0000\u04d4\u04d9"+
		"\u0003\u00c6c\u0000\u04d5\u04d6\u0005\u00cb\u0000\u0000\u04d6\u04d8\u0003"+
		"\u00c6c\u0000\u04d7\u04d5\u0001\u0000\u0000\u0000\u04d8\u04db\u0001\u0000"+
		"\u0000\u0000\u04d9\u04d7\u0001\u0000\u0000\u0000\u04d9\u04da\u0001\u0000"+
		"\u0000\u0000\u04da\u00c5\u0001\u0000\u0000\u0000\u04db\u04d9\u0001\u0000"+
		"\u0000\u0000\u04dc\u04e0\u0003\u00c8d\u0000\u04dd\u04de\u0007\u0016\u0000"+
		"\u0000\u04de\u04e0\u0003\u00c8d\u0000\u04df\u04dc\u0001\u0000\u0000\u0000"+
		"\u04df\u04dd\u0001\u0000\u0000\u0000\u04e0\u00c7\u0001\u0000\u0000\u0000"+
		"\u04e1\u04e5\u0003\u00d4j\u0000\u04e2\u04e4\u0003\u00cae\u0000\u04e3\u04e2"+
		"\u0001\u0000\u0000\u0000\u04e4\u04e7\u0001\u0000\u0000\u0000\u04e5\u04e3"+
		"\u0001\u0000\u0000\u0000\u04e5\u04e6\u0001\u0000\u0000\u0000\u04e6\u00c9"+
		"\u0001\u0000\u0000\u0000\u04e7\u04e5\u0001\u0000\u0000\u0000\u04e8\u04f8"+
		"\u0003\u00ccf\u0000\u04e9\u04f8\u0003\u009aM\u0000\u04ea\u04eb\u0005\u0092"+
		"\u0000\u0000\u04eb\u04ec\u0003\u00b0X\u0000\u04ec\u04ed\u0005\u00d8\u0000"+
		"\u0000\u04ed\u04f8\u0001\u0000\u0000\u0000\u04ee\u04f0\u0005\u0092\u0000"+
		"\u0000\u04ef\u04f1\u0003\u00b0X\u0000\u04f0\u04ef\u0001\u0000\u0000\u0000"+
		"\u04f0\u04f1\u0001\u0000\u0000\u0000\u04f1\u04f2\u0001\u0000\u0000\u0000"+
		"\u04f2\u04f4\u0005T\u0000\u0000\u04f3\u04f5\u0003\u00b0X\u0000\u04f4\u04f3"+
		"\u0001\u0000\u0000\u0000\u04f4\u04f5\u0001\u0000\u0000\u0000\u04f5\u04f6"+
		"\u0001\u0000\u0000\u0000\u04f6\u04f8\u0005\u00d8\u0000\u0000\u04f7\u04e8"+
		"\u0001\u0000\u0000\u0000\u04f7\u04e9\u0001\u0000\u0000\u0000\u04f7\u04ea"+
		"\u0001\u0000\u0000\u0000\u04f7\u04ee\u0001\u0000\u0000\u0000\u04f8\u00cb"+
		"\u0001\u0000\u0000\u0000\u04f9\u04fa\u0005S\u0000\u0000\u04fa\u04fb\u0003"+
		"\u0106\u0083\u0000\u04fb\u00cd\u0001\u0000\u0000\u0000\u04fc\u04fd\u0005"+
		"\u0092\u0000\u0000\u04fd\u04fe\u0003\u00b0X\u0000\u04fe\u04ff\u0005\u00d8"+
		"\u0000\u0000\u04ff\u00cf\u0001\u0000\u0000\u0000\u0500\u0502\u0003\u00d4"+
		"j\u0000\u0501\u0503\u0003\u00ccf\u0000\u0502\u0501\u0001\u0000\u0000\u0000"+
		"\u0503\u0504\u0001\u0000\u0000\u0000\u0504\u0502\u0001\u0000\u0000\u0000"+
		"\u0504\u0505\u0001\u0000\u0000\u0000\u0505\u00d1\u0001\u0000\u0000\u0000"+
		"\u0506\u0507\u0003\u00d4j\u0000\u0507\u0508\u0003\u00ceg\u0000\u0508\u00d3"+
		"\u0001\u0000\u0000\u0000\u0509\u051f\u0003\u00d6k\u0000\u050a\u051f\u0003"+
		"\u0108\u0084\u0000\u050b\u051f\u0003\u00d8l\u0000\u050c\u051f\u0003\u00dc"+
		"n\u0000\u050d\u051f\u0003\u00f8|\u0000\u050e\u051f\u0003\u00fa}\u0000"+
		"\u050f\u051f\u0003\u00fc~\u0000\u0510\u051f\u0003\u00fe\u007f\u0000\u0511"+
		"\u051f\u0003\u00f4z\u0000\u0512\u051f\u0003\u00e2q\u0000\u0513\u051f\u0003"+
		"\u0104\u0082\u0000\u0514\u051f\u0003\u00e4r\u0000\u0515\u051f\u0003\u00e6"+
		"s\u0000\u0516\u051f\u0003\u00e8t\u0000\u0517\u051f\u0003\u00eau\u0000"+
		"\u0518\u051f\u0003\u00ecv\u0000\u0519\u051f\u0003\u00eew\u0000\u051a\u051f"+
		"\u0003\u00f0x\u0000\u051b\u051f\u0003\u00f2y\u0000\u051c\u051f\u0003\u010c"+
		"\u0086\u0000\u051d\u051f\u0003\u0114\u008a\u0000\u051e\u0509\u0001\u0000"+
		"\u0000\u0000\u051e\u050a\u0001\u0000\u0000\u0000\u051e\u050b\u0001\u0000"+
		"\u0000\u0000\u051e\u050c\u0001\u0000\u0000\u0000\u051e\u050d\u0001\u0000"+
		"\u0000\u0000\u051e\u050e\u0001\u0000\u0000\u0000\u051e\u050f\u0001\u0000"+
		"\u0000\u0000\u051e\u0510\u0001\u0000\u0000\u0000\u051e\u0511\u0001\u0000"+
		"\u0000\u0000\u051e\u0512\u0001\u0000\u0000\u0000\u051e\u0513\u0001\u0000"+
		"\u0000\u0000\u051e\u0514\u0001\u0000\u0000\u0000\u051e\u0515\u0001\u0000"+
		"\u0000\u0000\u051e\u0516\u0001\u0000\u0000\u0000\u051e\u0517\u0001\u0000"+
		"\u0000\u0000\u051e\u0518\u0001\u0000\u0000\u0000\u051e\u0519\u0001\u0000"+
		"\u0000\u0000\u051e\u051a\u0001\u0000\u0000\u0000\u051e\u051b\u0001\u0000"+
		"\u0000\u0000\u051e\u051c\u0001\u0000\u0000\u0000\u051e\u051d\u0001\u0000"+
		"\u0000\u0000\u051f\u00d5\u0001\u0000\u0000\u0000\u0520\u052a\u0003\u0100"+
		"\u0080\u0000\u0521\u052a\u0003\u0132\u0099\u0000\u0522\u052a\u0003\u013c"+
		"\u009e\u0000\u0523\u052a\u0005\u0119\u0000\u0000\u0524\u052a\u0005l\u0000"+
		"\u0000\u0525\u052a\u0005\u0086\u0000\u0000\u0526\u052a\u0005\u0087\u0000"+
		"\u0000\u0527\u052a\u0005\u00a7\u0000\u0000\u0528\u052a\u0005\u00b6\u0000"+
		"\u0000\u0529\u0520\u0001\u0000\u0000\u0000\u0529\u0521\u0001\u0000\u0000"+
		"\u0000\u0529\u0522\u0001\u0000\u0000\u0000\u0529\u0523\u0001\u0000\u0000"+
		"\u0000\u0529\u0524\u0001\u0000\u0000\u0000\u0529\u0525\u0001\u0000\u0000"+
		"\u0000\u0529\u0526\u0001\u0000\u0000\u0000\u0529\u0527\u0001\u0000\u0000"+
		"\u0000\u0529\u0528\u0001\u0000\u0000\u0000\u052a\u00d7\u0001\u0000\u0000"+
		"\u0000\u052b\u052d\u0005+\u0000\u0000\u052c\u052e\u0003\u00dam\u0000\u052d"+
		"\u052c\u0001\u0000\u0000\u0000\u052e\u052f\u0001\u0000\u0000\u0000\u052f"+
		"\u052d\u0001\u0000\u0000\u0000\u052f\u0530\u0001\u0000\u0000\u0000\u0530"+
		"\u0533\u0001\u0000\u0000\u0000\u0531\u0532\u0005`\u0000\u0000\u0532\u0534"+
		"\u0003\u00b0X\u0000\u0533\u0531\u0001\u0000\u0000\u0000\u0533\u0534\u0001"+
		"\u0000\u0000\u0000\u0534\u0535\u0001\u0000\u0000\u0000\u0535\u0536\u0005"+
		"b\u0000\u0000\u0536\u00d9\u0001\u0000\u0000\u0000\u0537\u0538\u0005\u012c"+
		"\u0000\u0000\u0538\u0539\u0003\u00b0X\u0000\u0539\u053a\u0005\u010d\u0000"+
		"\u0000\u053a\u053b\u0003\u00b0X\u0000\u053b\u00db\u0001\u0000\u0000\u0000"+
		"\u053c\u053d\u0005+\u0000\u0000\u053d\u053f\u0003\u00b0X\u0000\u053e\u0540"+
		"\u0003\u00deo\u0000\u053f\u053e\u0001\u0000\u0000\u0000\u0540\u0541\u0001"+
		"\u0000\u0000\u0000\u0541\u053f\u0001\u0000\u0000\u0000\u0541\u0542\u0001"+
		"\u0000\u0000\u0000\u0542\u0545\u0001\u0000\u0000\u0000\u0543\u0544\u0005"+
		"`\u0000\u0000\u0544\u0546\u0003\u00b0X\u0000\u0545\u0543\u0001\u0000\u0000"+
		"\u0000\u0545\u0546\u0001\u0000\u0000\u0000\u0546\u0547\u0001\u0000\u0000"+
		"\u0000\u0547\u0548\u0005b\u0000\u0000\u0548\u00dd\u0001\u0000\u0000\u0000"+
		"\u0549\u054a\u0005\u012c\u0000\u0000\u054a\u054f\u0003\u00e0p\u0000\u054b"+
		"\u054c\u00051\u0000\u0000\u054c\u054e\u0003\u00e0p\u0000\u054d\u054b\u0001"+
		"\u0000\u0000\u0000\u054e\u0551\u0001\u0000\u0000\u0000\u054f\u054d\u0001"+
		"\u0000\u0000\u0000\u054f\u0550\u0001\u0000\u0000\u0000\u0550\u0552\u0001"+
		"\u0000\u0000\u0000\u0551\u054f\u0001\u0000\u0000\u0000\u0552\u0553\u0005"+
		"\u010d\u0000\u0000\u0553\u0554\u0003\u00b0X\u0000\u0554\u00df\u0001\u0000"+
		"\u0000\u0000\u0555\u055b\u0005\u00de\u0000\u0000\u0556\u0557\u0005\u0104"+
		"\u0000\u0000\u0557\u055b\u0005\u012e\u0000\u0000\u0558\u0559\u0005c\u0000"+
		"\u0000\u0559\u055b\u0005\u012e\u0000\u0000\u055a\u0555\u0001\u0000\u0000"+
		"\u0000\u055a\u0556\u0001\u0000\u0000\u0000\u055a\u0558\u0001\u0000\u0000"+
		"\u0000\u055b\u055c\u0001\u0000\u0000\u0000\u055c\u0577\u0003\u00c0`\u0000"+
		"\u055d\u055f\u0005\u008b\u0000\u0000\u055e\u0560\u0005\u00b3\u0000\u0000"+
		"\u055f\u055e\u0001\u0000\u0000\u0000\u055f\u0560\u0001\u0000\u0000\u0000"+
		"\u0560\u0561\u0001\u0000\u0000\u0000\u0561\u0577\u0005\u00b6\u0000\u0000"+
		"\u0562\u0564\u0005\u008b\u0000\u0000\u0563\u0565\u0005\u00b3\u0000\u0000"+
		"\u0564\u0563\u0001\u0000\u0000\u0000\u0564\u0565\u0001\u0000\u0000\u0000"+
		"\u0565\u0566\u0001\u0000\u0000\u0000\u0566\u0569\u0005\u011b\u0000\u0000"+
		"\u0567\u0569\u00050\u0000\u0000\u0568\u0562\u0001\u0000\u0000\u0000\u0568"+
		"\u0567\u0001\u0000\u0000\u0000\u0569\u056a\u0001\u0000\u0000\u0000\u056a"+
		"\u0577\u0003\u0118\u008c\u0000\u056b\u056d\u0005\u008b\u0000\u0000\u056c"+
		"\u056e\u0005\u00b3\u0000\u0000\u056d\u056c\u0001\u0000\u0000\u0000\u056d"+
		"\u056e\u0001\u0000\u0000\u0000\u056e\u0570\u0001\u0000\u0000\u0000\u056f"+
		"\u0571\u0003\u00be_\u0000\u0570\u056f\u0001\u0000\u0000\u0000\u0570\u0571"+
		"\u0001\u0000\u0000\u0000\u0571\u0572\u0001\u0000\u0000\u0000\u0572\u0577"+
		"\u0005\u00b2\u0000\u0000\u0573\u0574\u0007\u0011\u0000\u0000\u0574\u0577"+
		"\u0003\u00ba]\u0000\u0575\u0577\u0003\u00b0X\u0000\u0576\u055a\u0001\u0000"+
		"\u0000\u0000\u0576\u055d\u0001\u0000\u0000\u0000\u0576\u0568\u0001\u0000"+
		"\u0000\u0000\u0576\u056b\u0001\u0000\u0000\u0000\u0576\u0573\u0001\u0000"+
		"\u0000\u0000\u0576\u0575\u0001\u0000\u0000\u0000\u0577\u00e1\u0001\u0000"+
		"\u0000\u0000\u0578\u0579\u0005\u0092\u0000\u0000\u0579\u057a\u0003\u0114"+
		"\u008a\u0000\u057a\u057b\u0005\u0083\u0000\u0000\u057b\u0586\u0003\u00b0"+
		"X\u0000\u057c\u057d\u0005\u012d\u0000\u0000\u057d\u057f\u0003\u00b0X\u0000"+
		"\u057e\u057c\u0001\u0000\u0000\u0000\u057e\u057f\u0001\u0000\u0000\u0000"+
		"\u057f\u0580\u0001\u0000\u0000\u0000\u0580\u0581\u0005\u001e\u0000\u0000"+
		"\u0581\u0587\u0003\u00b0X\u0000\u0582\u0583\u0005\u012d\u0000\u0000\u0583"+
		"\u0585\u0003\u00b0X\u0000\u0584\u0582\u0001\u0000\u0000\u0000\u0584\u0585"+
		"\u0001\u0000\u0000\u0000\u0585\u0587\u0001\u0000\u0000\u0000\u0586\u057e"+
		"\u0001\u0000\u0000\u0000\u0586\u0584\u0001\u0000\u0000\u0000\u0587\u0588"+
		"\u0001\u0000\u0000\u0000\u0588\u0589\u0005\u00d8\u0000\u0000\u0589\u00e3"+
		"\u0001\u0000\u0000\u0000\u058a\u058e\u0005\u0092\u0000\u0000\u058b\u058c"+
		"\u0003\u0114\u008a\u0000\u058c\u058d\u0005d\u0000\u0000\u058d\u058f\u0001"+
		"\u0000\u0000\u0000\u058e\u058b\u0001\u0000\u0000\u0000\u058e\u058f\u0001"+
		"\u0000\u0000\u0000\u058f\u0590\u0001\u0000\u0000\u0000\u0590\u0593\u0003"+
		"t:\u0000\u0591\u0592\u0005\u012d\u0000\u0000\u0592\u0594\u0003\u00b0X"+
		"\u0000\u0593\u0591\u0001\u0000\u0000\u0000\u0593\u0594\u0001\u0000\u0000"+
		"\u0000\u0594\u0595\u0001\u0000\u0000\u0000\u0595\u0596\u0005\u001e\u0000"+
		"\u0000\u0596\u0597\u0003\u00b0X\u0000\u0597\u0598\u0005\u00d8\u0000\u0000"+
		"\u0598\u00e5\u0001\u0000\u0000\u0000\u0599\u059a\u0005\u00dc\u0000\u0000"+
		"\u059a\u059b\u0005\u009b\u0000\u0000\u059b\u059c\u0003\u0114\u008a\u0000"+
		"\u059c\u059d\u0005d\u0000\u0000\u059d\u059e\u0003\u00b0X\u0000\u059e\u059f"+
		"\u00051\u0000\u0000\u059f\u05a0\u0003\u0114\u008a\u0000\u05a0\u05a1\u0005"+
		"\u0083\u0000\u0000\u05a1\u05a2\u0003\u00b0X\u0000\u05a2\u05a3\u0005\u001e"+
		"\u0000\u0000\u05a3\u05a4\u0003\u00b0X\u0000\u05a4\u05a5\u0005\u00ef\u0000"+
		"\u0000\u05a5\u00e7\u0001\u0000\u0000\u0000\u05a6\u05a7\u0007\u0017\u0000"+
		"\u0000\u05a7\u05a8\u0005\u009b\u0000\u0000\u05a8\u05a9\u0003\u0114\u008a"+
		"\u0000\u05a9\u05aa\u0005\u0083\u0000\u0000\u05aa\u05ad\u0003\u00b0X\u0000"+
		"\u05ab\u05ac\u0005\u012d\u0000\u0000\u05ac\u05ae\u0003\u00b0X\u0000\u05ad"+
		"\u05ab\u0001\u0000\u0000\u0000\u05ad\u05ae\u0001\u0000\u0000\u0000\u05ae"+
		"\u05af\u0001\u0000\u0000\u0000\u05af\u05b0\u0005\u00ef\u0000\u0000\u05b0"+
		"\u00e9\u0001\u0000\u0000\u0000\u05b1\u05b2\u0005\u00b1\u0000\u0000\u05b2"+
		"\u05b3\u0005\u009b\u0000\u0000\u05b3\u05b6\u0003\u00b0X\u0000\u05b4\u05b5"+
		"\u00051\u0000\u0000\u05b5\u05b7\u0003\u00be_\u0000\u05b6\u05b4\u0001\u0000"+
		"\u0000\u0000\u05b6\u05b7\u0001\u0000\u0000\u0000\u05b7\u05b8\u0001\u0000"+
		"\u0000\u0000\u05b8\u05b9\u0005\u00ef\u0000\u0000\u05b9\u00eb\u0001\u0000"+
		"\u0000\u0000\u05ba\u05bb\u0005\u0118\u0000\u0000\u05bb\u05c3\u0005\u009b"+
		"\u0000\u0000\u05bc\u05be\u0007\u0018\u0000\u0000\u05bd\u05bc\u0001\u0000"+
		"\u0000\u0000\u05bd\u05be\u0001\u0000\u0000\u0000\u05be\u05c0\u0001\u0000"+
		"\u0000\u0000\u05bf\u05c1\u0003\u00b0X\u0000\u05c0\u05bf\u0001\u0000\u0000"+
		"\u0000\u05c0\u05c1\u0001\u0000\u0000\u0000\u05c1\u05c2\u0001\u0000\u0000"+
		"\u0000\u05c2\u05c4\u0005r\u0000\u0000\u05c3\u05bd\u0001\u0000\u0000\u0000"+
		"\u05c3\u05c4\u0001\u0000\u0000\u0000\u05c4\u05c5\u0001\u0000\u0000\u0000"+
		"\u05c5\u05c6\u0003\u00b0X\u0000\u05c6\u05c7\u0005\u00ef\u0000\u0000\u05c7"+
		"\u00ed\u0001\u0000\u0000\u0000\u05c8\u05c9\u0003t:\u0000\u05c9\u00ef\u0001"+
		"\u0000\u0000\u0000\u05ca\u05cb\u0003j5\u0000\u05cb\u00f1\u0001\u0000\u0000"+
		"\u0000\u05cc\u05cd\u0005\u009b\u0000\u0000\u05cd\u05ce\u0003\u00b0X\u0000"+
		"\u05ce\u05cf\u0005\u00ef\u0000\u0000\u05cf\u00f3\u0001\u0000\u0000\u0000"+
		"\u05d0\u05d1\u0003\u0114\u008a\u0000\u05d1\u05da\u0005\u0093\u0000\u0000"+
		"\u05d2\u05d7\u0003\u00f6{\u0000\u05d3\u05d4\u00051\u0000\u0000\u05d4\u05d6"+
		"\u0003\u00f6{\u0000\u05d5\u05d3\u0001\u0000\u0000\u0000\u05d6\u05d9\u0001"+
		"\u0000\u0000\u0000\u05d7\u05d5\u0001\u0000\u0000\u0000\u05d7\u05d8\u0001"+
		"\u0000\u0000\u0000\u05d8\u05db\u0001\u0000\u0000\u0000\u05d9\u05d7\u0001"+
		"\u0000\u0000\u0000\u05da\u05d2\u0001\u0000\u0000\u0000\u05da\u05db\u0001"+
		"\u0000\u0000\u0000\u05db\u05dc\u0001\u0000\u0000\u0000\u05dc\u05dd\u0005"+
		"\u00d9\u0000\u0000\u05dd\u00f5\u0001\u0000\u0000\u0000\u05de\u05df\u0003"+
		"\u0106\u0083\u0000\u05df\u05e0\u0005/\u0000\u0000\u05e0\u05e1\u0003\u00b0"+
		"X\u0000\u05e1\u05e7\u0001\u0000\u0000\u0000\u05e2\u05e7\u0003\u00ccf\u0000"+
		"\u05e3\u05e7\u0003\u0114\u008a\u0000\u05e4\u05e5\u0005S\u0000\u0000\u05e5"+
		"\u05e7\u0005\u010f\u0000\u0000\u05e6\u05de\u0001\u0000\u0000\u0000\u05e6"+
		"\u05e2\u0001\u0000\u0000\u0000\u05e6\u05e3\u0001\u0000\u0000\u0000\u05e6"+
		"\u05e4\u0001\u0000\u0000\u0000\u05e7\u00f7\u0001\u0000\u0000\u0000\u05e8"+
		"\u05e9\u0005<\u0000\u0000\u05e9\u05ea\u0005\u009b\u0000\u0000\u05ea\u05eb"+
		"\u0005\u010f\u0000\u0000\u05eb\u05ec\u0005\u00ef\u0000\u0000\u05ec\u00f9"+
		"\u0001\u0000\u0000\u0000\u05ed\u05ee\u0005i\u0000\u0000\u05ee\u05f7\u0005"+
		"\u0093\u0000\u0000\u05ef\u05f8\u0003\u0006\u0003\u0000\u05f0\u05f2\u0003"+
		":\u001d\u0000\u05f1\u05f0\u0001\u0000\u0000\u0000\u05f1\u05f2\u0001\u0000"+
		"\u0000\u0000\u05f2\u05f3\u0001\u0000\u0000\u0000\u05f3\u05f5\u0003^/\u0000"+
		"\u05f4\u05f6\u0003&\u0013\u0000\u05f5\u05f4\u0001\u0000\u0000\u0000\u05f5"+
		"\u05f6\u0001\u0000\u0000\u0000\u05f6\u05f8\u0001\u0000\u0000\u0000\u05f7"+
		"\u05ef\u0001\u0000\u0000\u0000\u05f7\u05f1\u0001\u0000\u0000\u0000\u05f8"+
		"\u05f9\u0001\u0000\u0000\u0000\u05f9\u05fa\u0005\u00d9\u0000\u0000\u05fa"+
		"\u00fb\u0001\u0000\u0000\u0000\u05fb\u05fc\u0005<\u0000\u0000\u05fc\u0605"+
		"\u0005\u0093\u0000\u0000\u05fd\u0606\u0003\u0006\u0003\u0000\u05fe\u0600"+
		"\u0003:\u001d\u0000\u05ff\u05fe\u0001\u0000\u0000\u0000\u05ff\u0600\u0001"+
		"\u0000\u0000\u0000\u0600\u0601\u0001\u0000\u0000\u0000\u0601\u0603\u0003"+
		"^/\u0000\u0602\u0604\u0003&\u0013\u0000\u0603\u0602\u0001\u0000\u0000"+
		"\u0000\u0603\u0604\u0001\u0000\u0000\u0000\u0604\u0606\u0001\u0000\u0000"+
		"\u0000\u0605\u05fd\u0001\u0000\u0000\u0000\u0605\u05ff\u0001\u0000\u0000"+
		"\u0000\u0606\u0607\u0001\u0000\u0000\u0000\u0607\u0608\u0005\u00d9\u0000"+
		"\u0000\u0608\u00fd\u0001\u0000\u0000\u0000\u0609\u060a\u0005.\u0000\u0000"+
		"\u060a\u060b\u0005\u0093\u0000\u0000\u060b\u060c\u0003\u0006\u0003\u0000"+
		"\u060c\u060d\u0005\u00d9\u0000\u0000\u060d\u00ff\u0001\u0000\u0000\u0000"+
		"\u060e\u0610\u0005\u00a1\u0000\u0000\u060f\u060e\u0001\u0000\u0000\u0000"+
		"\u060f\u0610\u0001\u0000\u0000\u0000\u0610\u0611\u0001\u0000\u0000\u0000"+
		"\u0611\u0612\u0007\u0019\u0000\u0000\u0612\u0101\u0001\u0000\u0000\u0000"+
		"\u0613\u0615\u0005\u00a1\u0000\u0000\u0614\u0613\u0001\u0000\u0000\u0000"+
		"\u0614\u0615\u0001\u0000\u0000\u0000\u0615\u0616\u0001\u0000\u0000\u0000"+
		"\u0616\u0617\u0005\u0005\u0000\u0000\u0617\u0103\u0001\u0000\u0000\u0000"+
		"\u0618\u0621\u0005\u0092\u0000\u0000\u0619\u061e\u0003\u00b0X\u0000\u061a"+
		"\u061b\u00051\u0000\u0000\u061b\u061d\u0003\u00b0X\u0000\u061c\u061a\u0001"+
		"\u0000\u0000\u0000\u061d\u0620\u0001\u0000\u0000\u0000\u061e\u061c\u0001"+
		"\u0000\u0000\u0000\u061e\u061f\u0001\u0000\u0000\u0000\u061f\u0622\u0001"+
		"\u0000\u0000\u0000\u0620\u061e\u0001\u0000\u0000\u0000\u0621\u0619\u0001"+
		"\u0000\u0000\u0000\u0621\u0622\u0001\u0000\u0000\u0000\u0622\u0623\u0001"+
		"\u0000\u0000\u0000\u0623\u0624\u0005\u00d8\u0000\u0000\u0624\u0105\u0001"+
		"\u0000\u0000\u0000\u0625\u0626\u0003\u0144\u00a2\u0000\u0626\u0107\u0001"+
		"\u0000\u0000\u0000\u0627\u0628\u0005P\u0000\u0000\u0628\u0629\u0003\u010a"+
		"\u0085\u0000\u0629\u0109\u0001\u0000\u0000\u0000\u062a\u062d\u0003\u0144"+
		"\u00a2\u0000\u062b\u062d\u0005\u0005\u0000\u0000\u062c\u062a\u0001\u0000"+
		"\u0000\u0000\u062c\u062b\u0001\u0000\u0000\u0000\u062d\u010b\u0001\u0000"+
		"\u0000\u0000\u062e\u062f\u0003\u0110\u0088\u0000\u062f\u0631\u0005\u009b"+
		"\u0000\u0000\u0630\u0632\u0007\u0000\u0000\u0000\u0631\u0630\u0001\u0000"+
		"\u0000\u0000\u0631\u0632\u0001\u0000\u0000\u0000\u0632\u063b\u0001\u0000"+
		"\u0000\u0000\u0633\u0638\u0003\u010e\u0087\u0000\u0634\u0635\u00051\u0000"+
		"\u0000\u0635\u0637\u0003\u010e\u0087\u0000\u0636\u0634\u0001\u0000\u0000"+
		"\u0000\u0637\u063a\u0001\u0000\u0000\u0000\u0638\u0636\u0001\u0000\u0000"+
		"\u0000\u0638\u0639\u0001\u0000\u0000\u0000\u0639\u063c\u0001\u0000\u0000"+
		"\u0000\u063a\u0638\u0001\u0000\u0000\u0000\u063b\u0633\u0001\u0000\u0000"+
		"\u0000\u063b\u063c\u0001\u0000\u0000\u0000\u063c\u063d\u0001\u0000\u0000"+
		"\u0000\u063d\u063e\u0005\u00ef\u0000\u0000\u063e\u010d\u0001\u0000\u0000"+
		"\u0000\u063f\u0640\u0003\u00b0X\u0000\u0640\u010f\u0001\u0000\u0000\u0000"+
		"\u0641\u0642\u0003\u0112\u0089\u0000\u0642\u0643\u0003\u0144\u00a2\u0000"+
		"\u0643\u0111\u0001\u0000\u0000\u0000\u0644\u0645\u0003\u0144\u00a2\u0000"+
		"\u0645\u0646\u0005S\u0000\u0000\u0646\u0648\u0001\u0000\u0000\u0000\u0647"+
		"\u0644\u0001\u0000\u0000\u0000\u0648\u064b\u0001\u0000\u0000\u0000\u0649"+
		"\u0647\u0001\u0000\u0000\u0000\u0649\u064a\u0001\u0000\u0000\u0000\u064a"+
		"\u0113\u0001\u0000\u0000\u0000\u064b\u0649\u0001\u0000\u0000\u0000\u064c"+
		"\u064d\u0003\u013e\u009f\u0000\u064d\u0115\u0001\u0000\u0000\u0000\u064e"+
		"\u0653\u0003\u0144\u00a2\u0000\u064f\u0650\u00051\u0000\u0000\u0650\u0652"+
		"\u0003\u0144\u00a2\u0000\u0651\u064f\u0001\u0000\u0000\u0000\u0652\u0655"+
		"\u0001\u0000\u0000\u0000\u0653\u0651\u0001\u0000\u0000\u0000\u0653\u0654"+
		"\u0001\u0000\u0000\u0000\u0654\u0117\u0001\u0000\u0000\u0000\u0655\u0653"+
		"\u0001\u0000\u0000\u0000\u0656\u065b\u0003\u011a\u008d\u0000\u0657\u0658"+
		"\u0005\u001e\u0000\u0000\u0658\u065a\u0003\u011a\u008d\u0000\u0659\u0657"+
		"\u0001\u0000\u0000\u0000\u065a\u065d\u0001\u0000\u0000\u0000\u065b\u0659"+
		"\u0001\u0000\u0000\u0000\u065b\u065c\u0001\u0000\u0000\u0000\u065c\u0119"+
		"\u0001\u0000\u0000\u0000\u065d\u065b\u0001\u0000\u0000\u0000\u065e\u0660"+
		"\u0003\u011c\u008e\u0000\u065f\u0661\u0003\u011e\u008f\u0000\u0660\u065f"+
		"\u0001\u0000\u0000\u0000\u0660\u0661\u0001\u0000\u0000\u0000\u0661\u0665"+
		"\u0001\u0000\u0000\u0000\u0662\u0664\u0003\u0120\u0090\u0000\u0663\u0662"+
		"\u0001\u0000\u0000\u0000\u0664\u0667\u0001\u0000\u0000\u0000\u0665\u0663"+
		"\u0001\u0000\u0000\u0000\u0665\u0666\u0001\u0000\u0000\u0000\u0666\u011b"+
		"\u0001\u0000\u0000\u0000\u0667\u0665\u0001\u0000\u0000\u0000\u0668\u06aa"+
		"\u0005\u00b4\u0000\u0000\u0669\u06aa\u0005\u00b6\u0000\u0000\u066a\u06aa"+
		"\u0005 \u0000\u0000\u066b\u06aa\u0005!\u0000\u0000\u066c\u06aa\u0005\u0127"+
		"\u0000\u0000\u066d\u06aa\u0005\u0107\u0000\u0000\u066e\u06aa\u0005\u0089"+
		"\u0000\u0000\u066f\u0671\u0005\u0100\u0000\u0000\u0670\u066f\u0001\u0000"+
		"\u0000\u0000\u0670\u0671\u0001\u0000\u0000\u0000\u0671\u0672\u0001\u0000"+
		"\u0000\u0000\u0672\u06aa\u0005\u008a\u0000\u0000\u0673\u06aa\u0005o\u0000"+
		"\u0000\u0674\u06aa\u0005C\u0000\u0000\u0675\u0676\u0005\u0099\u0000\u0000"+
		"\u0676\u06aa\u0007\u001a\u0000\u0000\u0677\u0678\u0005\u0134\u0000\u0000"+
		"\u0678\u06aa\u0007\u001a\u0000\u0000\u0679\u067a\u0005\u010e\u0000\u0000"+
		"\u067a\u067e\u0007\u001b\u0000\u0000\u067b\u067f\u0005\u0111\u0000\u0000"+
		"\u067c\u067d\u0005\u010e\u0000\u0000\u067d\u067f\u0005\u0133\u0000\u0000"+
		"\u067e\u067b\u0001\u0000\u0000\u0000\u067e\u067c\u0001\u0000\u0000\u0000"+
		"\u067f\u06aa\u0001\u0000\u0000\u0000\u0680\u0681\u0005\u0110\u0000\u0000"+
		"\u0681\u0685\u0007\u001b\u0000\u0000\u0682\u0686\u0005\u0111\u0000\u0000"+
		"\u0683\u0684\u0005\u010e\u0000\u0000\u0684\u0686\u0005\u0133\u0000\u0000"+
		"\u0685\u0682\u0001\u0000\u0000\u0000\u0685\u0683\u0001\u0000\u0000\u0000"+
		"\u0686\u06aa\u0001\u0000\u0000\u0000\u0687\u06aa\u0005Z\u0000\u0000\u0688"+
		"\u06aa\u0005\u00c9\u0000\u0000\u0689\u06aa\u0005\u00ad\u0000\u0000\u068a"+
		"\u06aa\u0005\u012a\u0000\u0000\u068b\u06aa\u0005\u00e0\u0000\u0000\u068c"+
		"\u06aa\u0005\\\u0000\u0000\u068d\u06aa\u0005\u009e\u0000\u0000\u068e\u068f"+
		"\u0007\u001c\u0000\u0000\u068f\u0690\u0005\u009c\u0000\u0000\u0690\u0691"+
		"\u0003\u0118\u008c\u0000\u0691\u0692\u0005|\u0000\u0000\u0692\u06aa\u0001"+
		"\u0000\u0000\u0000\u0693\u06aa\u0005\u00c3\u0000\u0000\u0694\u06aa\u0005"+
		"\u00c4\u0000\u0000\u0695\u0696\u0005\u00d3\u0000\u0000\u0696\u06aa\u0005"+
		"\u0126\u0000\u0000\u0697\u06a7\u0005\u0015\u0000\u0000\u0698\u06a8\u0005"+
		"\u00ad\u0000\u0000\u0699\u06a8\u0005\u012a\u0000\u0000\u069a\u06a8\u0005"+
		"\u00e0\u0000\u0000\u069b\u06a8\u0005\\\u0000\u0000\u069c\u06a8\u0005\u009e"+
		"\u0000\u0000\u069d\u069e\u0005\u00d3\u0000\u0000\u069e\u06a8\u0005\u0126"+
		"\u0000\u0000\u069f\u06a1\u0005\u0126\u0000\u0000\u06a0\u069f\u0001\u0000"+
		"\u0000\u0000\u06a0\u06a1\u0001\u0000\u0000\u0000\u06a1\u06a2\u0001\u0000"+
		"\u0000\u0000\u06a2\u06a3\u0005\u009c\u0000\u0000\u06a3\u06a4\u0003\u0118"+
		"\u008c\u0000\u06a4\u06a5\u0005|\u0000\u0000\u06a5\u06a8\u0001\u0000\u0000"+
		"\u0000\u06a6\u06a8\u0005\u0126\u0000\u0000\u06a7\u0698\u0001\u0000\u0000"+
		"\u0000\u06a7\u0699\u0001\u0000\u0000\u0000\u06a7\u069a\u0001\u0000\u0000"+
		"\u0000\u06a7\u069b\u0001\u0000\u0000\u0000\u06a7\u069c\u0001\u0000\u0000"+
		"\u0000\u06a7\u069d\u0001\u0000\u0000\u0000\u06a7\u06a0\u0001\u0000\u0000"+
		"\u0000\u06a7\u06a6\u0001\u0000\u0000\u0000\u06a7\u06a8\u0001\u0000\u0000"+
		"\u0000\u06a8\u06aa\u0001\u0000\u0000\u0000\u06a9\u0668\u0001\u0000\u0000"+
		"\u0000\u06a9\u0669\u0001\u0000\u0000\u0000\u06a9\u066a\u0001\u0000\u0000"+
		"\u0000\u06a9\u066b\u0001\u0000\u0000\u0000\u06a9\u066c\u0001\u0000\u0000"+
		"\u0000\u06a9\u066d\u0001\u0000\u0000\u0000\u06a9\u066e\u0001\u0000\u0000"+
		"\u0000\u06a9\u0670\u0001\u0000\u0000\u0000\u06a9\u0673\u0001\u0000\u0000"+
		"\u0000\u06a9\u0674\u0001\u0000\u0000\u0000\u06a9\u0675\u0001\u0000\u0000"+
		"\u0000\u06a9\u0677\u0001\u0000\u0000\u0000\u06a9\u0679\u0001\u0000\u0000"+
		"\u0000\u06a9\u0680\u0001\u0000\u0000\u0000\u06a9\u0687\u0001\u0000\u0000"+
		"\u0000\u06a9\u0688\u0001\u0000\u0000\u0000\u06a9\u0689\u0001\u0000\u0000"+
		"\u0000\u06a9\u068a\u0001\u0000\u0000\u0000\u06a9\u068b\u0001\u0000\u0000"+
		"\u0000\u06a9\u068c\u0001\u0000\u0000\u0000\u06a9\u068d\u0001\u0000\u0000"+
		"\u0000\u06a9\u068e\u0001\u0000\u0000\u0000\u06a9\u0693\u0001\u0000\u0000"+
		"\u0000\u06a9\u0694\u0001\u0000\u0000\u0000\u06a9\u0695\u0001\u0000\u0000"+
		"\u0000\u06a9\u0697\u0001\u0000\u0000\u0000\u06aa\u011d\u0001\u0000\u0000"+
		"\u0000\u06ab\u06ac\u0005\u00b3\u0000\u0000\u06ac\u06af\u0005\u00b6\u0000"+
		"\u0000\u06ad\u06af\u0005\u0091\u0000\u0000\u06ae\u06ab\u0001\u0000\u0000"+
		"\u0000\u06ae\u06ad\u0001\u0000\u0000\u0000\u06af\u011f\u0001\u0000\u0000"+
		"\u0000\u06b0\u06b2\u0007\u001c\u0000\u0000\u06b1\u06b3\u0003\u011e\u008f"+
		"\u0000\u06b2\u06b1\u0001\u0000\u0000\u0000\u06b2\u06b3\u0001\u0000\u0000"+
		"\u0000\u06b3\u0121\u0001\u0000\u0000\u0000\u06b4\u06b7\u0003\u0144\u00a2"+
		"\u0000\u06b5\u06b7\u0003\u0108\u0084\u0000\u06b6\u06b4\u0001\u0000\u0000"+
		"\u0000\u06b6\u06b5\u0001\u0000\u0000\u0000\u06b7\u0123\u0001\u0000\u0000"+
		"\u0000\u06b8\u06bb\u0003\u0144\u00a2\u0000\u06b9\u06bb\u0003\u0108\u0084"+
		"\u0000\u06ba\u06b8\u0001\u0000\u0000\u0000\u06ba\u06b9\u0001\u0000\u0000"+
		"\u0000\u06bb\u0125\u0001\u0000\u0000\u0000\u06bc\u06c1\u0003\u0124\u0092"+
		"\u0000\u06bd\u06be\u00051\u0000\u0000\u06be\u06c0\u0003\u0124\u0092\u0000"+
		"\u06bf\u06bd\u0001\u0000\u0000\u0000\u06c0\u06c3\u0001\u0000\u0000\u0000"+
		"\u06c1\u06bf\u0001\u0000\u0000\u0000\u06c1\u06c2\u0001\u0000\u0000\u0000"+
		"\u06c2\u0127\u0001\u0000\u0000\u0000\u06c3\u06c1\u0001\u0000\u0000\u0000"+
		"\u06c4\u06c9\u0003\u012a\u0095\u0000\u06c5\u06c6\u00051\u0000\u0000\u06c6"+
		"\u06c8\u0003\u012a\u0095\u0000\u06c7\u06c5\u0001\u0000\u0000\u0000\u06c8"+
		"\u06cb\u0001\u0000\u0000\u0000\u06c9\u06c7\u0001\u0000\u0000\u0000\u06c9"+
		"\u06ca\u0001\u0000\u0000\u0000\u06ca\u0129\u0001\u0000\u0000\u0000\u06cb"+
		"\u06c9\u0001\u0000\u0000\u0000\u06cc\u06cf\u0003\u012c\u0096\u0000\u06cd"+
		"\u06cf\u0003\u0108\u0084\u0000\u06ce\u06cc\u0001\u0000\u0000\u0000\u06ce"+
		"\u06cd\u0001\u0000\u0000\u0000\u06cf\u012b\u0001\u0000\u0000\u0000\u06d0"+
		"\u06d5\u0003\u0144\u00a2\u0000\u06d1\u06d2\u0005S\u0000\u0000\u06d2\u06d4"+
		"\u0003\u0144\u00a2\u0000\u06d3\u06d1\u0001\u0000\u0000\u0000\u06d4\u06d7"+
		"\u0001\u0000\u0000\u0000\u06d5\u06d3\u0001\u0000\u0000\u0000\u06d5\u06d6"+
		"\u0001\u0000\u0000\u0000\u06d6\u012d\u0001\u0000\u0000\u0000\u06d7\u06d5"+
		"\u0001\u0000\u0000\u0000\u06d8\u06e1\u0005\u0092\u0000\u0000\u06d9\u06de"+
		"\u0003\u0132\u0099\u0000\u06da\u06db\u00051\u0000\u0000\u06db\u06dd\u0003"+
		"\u0132\u0099\u0000\u06dc\u06da\u0001\u0000\u0000\u0000\u06dd\u06e0\u0001"+
		"\u0000\u0000\u0000\u06de\u06dc\u0001\u0000\u0000\u0000\u06de\u06df\u0001"+
		"\u0000\u0000\u0000\u06df\u06e2\u0001\u0000\u0000\u0000\u06e0\u06de\u0001"+
		"\u0000\u0000\u0000\u06e1\u06d9\u0001\u0000\u0000\u0000\u06e1\u06e2\u0001"+
		"\u0000\u0000\u0000\u06e2\u06e3\u0001\u0000\u0000\u0000\u06e3\u06e4\u0005"+
		"\u00d8\u0000\u0000\u06e4\u012f\u0001\u0000\u0000\u0000\u06e5\u06e8\u0003"+
		"\u0132\u0099\u0000\u06e6\u06e7\u00051\u0000\u0000\u06e7\u06e9\u0003\u0132"+
		"\u0099\u0000\u06e8\u06e6\u0001\u0000\u0000\u0000\u06e9\u06ea\u0001\u0000"+
		"\u0000\u0000\u06ea\u06e8\u0001\u0000\u0000\u0000\u06ea\u06eb\u0001\u0000"+
		"\u0000\u0000\u06eb\u0131\u0001\u0000\u0000\u0000\u06ec\u06ed\u0007\u001d"+
		"\u0000\u0000\u06ed\u0133\u0001\u0000\u0000\u0000\u06ee\u06f1\u0003\u0132"+
		"\u0099\u0000\u06ef\u06f1\u0003\u0108\u0084\u0000\u06f0\u06ee\u0001\u0000"+
		"\u0000\u0000\u06f0\u06ef\u0001\u0000\u0000\u0000\u06f1\u0135\u0001\u0000"+
		"\u0000\u0000\u06f2\u06f5\u0003\u0132\u0099\u0000\u06f3\u06f5\u0003\u0108"+
		"\u0084\u0000\u06f4\u06f2\u0001\u0000\u0000\u0000\u06f4\u06f3\u0001\u0000"+
		"\u0000\u0000\u06f5\u0137\u0001\u0000\u0000\u0000\u06f6\u06f9\u0005\u0005"+
		"\u0000\u0000\u06f7\u06f9\u0003\u0108\u0084\u0000\u06f8\u06f6\u0001\u0000"+
		"\u0000\u0000\u06f8\u06f7\u0001\u0000\u0000\u0000\u06f9\u0139\u0001\u0000"+
		"\u0000\u0000\u06fa\u06fd\u0003\u013c\u009e\u0000\u06fb\u06fd\u0003\u0108"+
		"\u0084\u0000\u06fc\u06fa\u0001\u0000\u0000\u0000\u06fc\u06fb\u0001\u0000"+
		"\u0000\u0000\u06fd\u013b\u0001\u0000\u0000\u0000\u06fe\u070c\u0005\u0093"+
		"\u0000\u0000\u06ff\u0700\u0003\u0106\u0083\u0000\u0700\u0701\u0005/\u0000"+
		"\u0000\u0701\u0709\u0003\u00b0X\u0000\u0702\u0703\u00051\u0000\u0000\u0703"+
		"\u0704\u0003\u0106\u0083\u0000\u0704\u0705\u0005/\u0000\u0000\u0705\u0706"+
		"\u0003\u00b0X\u0000\u0706\u0708\u0001\u0000\u0000\u0000\u0707\u0702\u0001"+
		"\u0000\u0000\u0000\u0708\u070b\u0001\u0000\u0000\u0000\u0709\u0707\u0001"+
		"\u0000\u0000\u0000\u0709\u070a\u0001\u0000\u0000\u0000\u070a\u070d\u0001"+
		"\u0000\u0000\u0000\u070b\u0709\u0001\u0000\u0000\u0000\u070c\u06ff\u0001"+
		"\u0000\u0000\u0000\u070c\u070d\u0001\u0000\u0000\u0000\u070d\u070e\u0001"+
		"\u0000\u0000\u0000\u070e\u070f\u0005\u00d9\u0000\u0000\u070f\u013d\u0001"+
		"\u0000\u0000\u0000\u0710\u0713\u0003\u0140\u00a0\u0000\u0711\u0713\u0003"+
		"\u0142\u00a1\u0000\u0712\u0710\u0001\u0000\u0000\u0000\u0712\u0711\u0001"+
		"\u0000\u0000\u0000\u0713\u013f\u0001\u0000\u0000\u0000\u0714\u0715\u0003"+
		"\u0146\u00a3\u0000\u0715\u0141\u0001\u0000\u0000\u0000\u0716\u0717\u0003"+
		"\u0148\u00a4\u0000\u0717\u0143\u0001\u0000\u0000\u0000\u0718\u071b\u0003"+
		"\u0146\u00a3\u0000\u0719\u071b\u0003\u0148\u00a4\u0000\u071a\u0718\u0001"+
		"\u0000\u0000\u0000\u071a\u0719\u0001\u0000\u0000\u0000\u071b\u0145\u0001"+
		"\u0000\u0000\u0000\u071c\u071d\u0005\n\u0000\u0000\u071d\u0147\u0001\u0000"+
		"\u0000\u0000\u071e\u0728\u0003\u014c\u00a6\u0000\u071f\u0728\u0005\u00b3"+
		"\u0000\u0000\u0720\u0728\u0005\u00b6\u0000\u0000\u0721\u0728\u0005\u011b"+
		"\u0000\u0000\u0722\u0728\u0005\u00b2\u0000\u0000\u0723\u0728\u0005\u00a8"+
		"\u0000\u0000\u0724\u0728\u0005\u00a9\u0000\u0000\u0725\u0728\u0005\u00aa"+
		"\u0000\u0000\u0726\u0728\u0005\u00ab\u0000\u0000\u0727\u071e\u0001\u0000"+
		"\u0000\u0000\u0727\u071f\u0001\u0000\u0000\u0000\u0727\u0720\u0001\u0000"+
		"\u0000\u0000\u0727\u0721\u0001\u0000\u0000\u0000\u0727\u0722\u0001\u0000"+
		"\u0000\u0000\u0727\u0723\u0001\u0000\u0000\u0000\u0727\u0724\u0001\u0000"+
		"\u0000\u0000\u0727\u0725\u0001\u0000\u0000\u0000\u0727\u0726\u0001\u0000"+
		"\u0000\u0000\u0728\u0149\u0001\u0000\u0000\u0000\u0729\u072c\u0003\u0146"+
		"\u00a3\u0000\u072a\u072c\u0003\u014c\u00a6\u0000\u072b\u0729\u0001\u0000"+
		"\u0000\u0000\u072b\u072a\u0001\u0000\u0000\u0000\u072c\u014b\u0001\u0000"+
		"\u0000\u0000\u072d\u072e\u0003\u014e\u00a7\u0000\u072e\u014d\u0001\u0000"+
		"\u0000\u0000\u072f\u0730\u0007\u001e\u0000\u0000\u0730\u014f\u0001\u0000"+
		"\u0000\u0000\u0731\u0732\u0005\u0000\u0000\u0001\u0732\u0151\u0001\u0000"+
		"\u0000\u0000\u00e2\u0157\u015b\u0160\u0168\u016d\u0172\u0178\u018b\u018f"+
		"\u0199\u01a1\u01a5\u01a8\u01ab\u01b0\u01b4\u01ba\u01c0\u01cd\u01dc\u01ea"+
		"\u0203\u020b\u0216\u0219\u0221\u0225\u0229\u022f\u0233\u0238\u023b\u0240"+
		"\u0243\u0245\u0251\u0254\u0263\u026a\u0277\u0281\u0284\u0287\u0290\u0294"+
		"\u0296\u0298\u02a2\u02a8\u02b0\u02bb\u02c0\u02c4\u02ca\u02d3\u02d6\u02dc"+
		"\u02df\u02e5\u02e7\u02f9\u02fc\u0300\u0303\u030a\u0312\u0318\u031b\u0322"+
		"\u032a\u0332\u0336\u033b\u033f\u0349\u034f\u0353\u0355\u035a\u035f\u0363"+
		"\u0366\u036a\u036e\u0371\u0377\u0379\u0385\u0389\u038c\u038f\u0393\u0399"+
		"\u039c\u039f\u03a7\u03ab\u03af\u03b1\u03b6\u03ba\u03bc\u03c6\u03da\u03dd"+
		"\u03e2\u03e5\u03e8\u03eb\u03ef\u03f2\u03f6\u03f9\u03fe\u0402\u0407\u0411"+
		"\u0415\u0418\u041e\u0423\u0428\u042e\u0433\u043b\u0443\u0449\u0451\u045d"+
		"\u0466\u046e\u0479\u0481\u0489\u048f\u0499\u049e\u04a7\u04ac\u04b1\u04b5"+
		"\u04ba\u04bd\u04c0\u04c9\u04d1\u04d9\u04df\u04e5\u04f0\u04f4\u04f7\u0504"+
		"\u051e\u0529\u052f\u0533\u0541\u0545\u054f\u055a\u055f\u0564\u0568\u056d"+
		"\u0570\u0576\u057e\u0584\u0586\u058e\u0593\u05ad\u05b6\u05bd\u05c0\u05c3"+
		"\u05d7\u05da\u05e6\u05f1\u05f5\u05f7\u05ff\u0603\u0605\u060f\u0614\u061e"+
		"\u0621\u062c\u0631\u0638\u063b\u0649\u0653\u065b\u0660\u0665\u0670\u067e"+
		"\u0685\u06a0\u06a7\u06a9\u06ae\u06b2\u06b6\u06ba\u06c1\u06c9\u06ce\u06d5"+
		"\u06de\u06e1\u06ea\u06f0\u06f4\u06f8\u06fc\u0709\u070c\u0712\u071a\u0727"+
		"\u072b";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}