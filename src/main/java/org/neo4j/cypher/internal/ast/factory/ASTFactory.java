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
import org.neo4j.cypher.internal.parser.common.ast.factory.CallInTxsOnErrorBehaviourType;
import org.neo4j.cypher.internal.parser.common.ast.factory.HintIndexType;
import org.neo4j.cypher.internal.parser.common.ast.factory.SimpleEither;

/**
 * Factory for constructing ASTs.
 * <p>
 * This interface is generic in many dimensions, in order to support type-safe construction of ASTs
 * without depending on the concrete AST type. This architecture allows code which creates/manipulates AST
 * to live independently of the AST, and thus makes sharing and reuse of these components much easier.
 * <p>
 * The factory contains methods for creating AST representing all of Cypher 9, as defined
 * at `https://github.com/opencypher/openCypher/`, and implemented in `https://github.com/opencypher/front-end`.
 * <p>
 * Schema commands like `CREATE/DROP INDEX` as not supported, nor system DSL used in Neo4j.
 *
 * @param <POS> type used to mark the input position of the created AST node.
 */
public interface ASTFactory<
                STATEMENTS,
                STATEMENT,
                QUERY extends STATEMENT,
                CLAUSE,
                FINISH_CLAUSE extends CLAUSE,
                RETURN_CLAUSE extends CLAUSE,
                RETURN_ITEM,
                RETURN_ITEMS,
                ORDER_ITEM,
                PATTERN,
                NODE_PATTERN extends PATTERN_ATOM,
                REL_PATTERN extends PATTERN_ATOM,
                PATH_LENGTH,
                SET_CLAUSE extends CLAUSE,
                SET_ITEM,
                REMOVE_ITEM,
                CALL_RESULT_ITEM,
                HINT,
                EXPRESSION,
                LABEL_EXPRESSION,
                FUNCTION_INVOCATION extends EXPRESSION,
                PARAMETER extends EXPRESSION,
                VARIABLE extends EXPRESSION,
                PROPERTY extends EXPRESSION,
                MAP_PROJECTION_ITEM,
                USE_GRAPH extends CLAUSE,
                STATEMENT_WITH_GRAPH extends STATEMENT,
                SCHEMA_COMMAND extends STATEMENT_WITH_GRAPH,
                YIELD extends CLAUSE,
                WHERE,
                SUBQUERY_IN_TRANSACTIONS_PARAMETERS,
                SUBQUERY_IN_TRANSACTIONS_BATCH_PARAMETERS,
                SUBQUERY_IN_TRANSACTIONS_CONCURRENCY_PARAMETERS,
                SUBQUERY_IN_TRANSACTIONS_ERROR_PARAMETERS,
                SUBQUERY_IN_TRANSACTIONS_REPORT_PARAMETERS,
                POS,
                ENTITY_TYPE,
                PATH_PATTERN_QUANTIFIER,
                PATTERN_ATOM,
                DATABASE_NAME,
                PATTERN_SELECTOR,
                MATCH_MODE,
                PATTERN_ELEMENT>
        extends ASTExpressionFactory<
                EXPRESSION,
                LABEL_EXPRESSION,
                PARAMETER,
                PATTERN,
                QUERY,
                WHERE,
                VARIABLE,
                PROPERTY,
                FUNCTION_INVOCATION,
                MAP_PROJECTION_ITEM,
                POS,
                ENTITY_TYPE,
                MATCH_MODE> {
    final class NULL {
        private NULL() {
            throw new IllegalStateException("This class should not be instantiated, use `null` instead.");
        }
    }

    class StringPos<POS> {
        public final String string;
        public final POS pos;
        public final POS endPos;

        public StringPos(String string, POS pos) {
            this.string = string;
            this.pos = pos;
            this.endPos = null;
        }

        public StringPos(String string, POS pos, POS endPos) {
            this.string = string;
            this.pos = pos;
            this.endPos = endPos;
        }
    }

    STATEMENTS statements(List<STATEMENT> statements);

    // QUERY

    QUERY newSingleQuery(POS p, List<CLAUSE> clauses);

    QUERY newSingleQuery(List<CLAUSE> clauses);

    QUERY newUnion(POS p, QUERY lhs, QUERY rhs, boolean all);

    USE_GRAPH directUseClause(POS p, DATABASE_NAME name);

    USE_GRAPH functionUseClause(POS p, FUNCTION_INVOCATION function);

    FINISH_CLAUSE newFinishClause(POS p);

    RETURN_CLAUSE newReturnClause(
            POS p,
            boolean distinct,
            RETURN_ITEMS returnItems,
            List<ORDER_ITEM> order,
            POS orderPos,
            EXPRESSION skip,
            POS skipPosition,
            EXPRESSION limit,
            POS limitPosition);

    RETURN_ITEMS newReturnItems(POS p, boolean returnAll, List<RETURN_ITEM> returnItems);

    RETURN_ITEM newReturnItem(POS p, EXPRESSION e, VARIABLE v);

    RETURN_ITEM newReturnItem(POS p, EXPRESSION e, int eStartOffset, int eEndOffset);

    ORDER_ITEM orderDesc(POS p, EXPRESSION e);

    ORDER_ITEM orderAsc(POS p, EXPRESSION e);

    WHERE whereClause(POS p, EXPRESSION e);

    CLAUSE withClause(POS p, RETURN_CLAUSE returnClause, WHERE where);

    CLAUSE matchClause(
            POS p,
            boolean optional,
            MATCH_MODE matchMode,
            List<PATTERN> patterns,
            POS patternPos,
            List<HINT> hints,
            WHERE where);

    HINT usingIndexHint(
            POS p,
            VARIABLE v,
            String labelOrRelType,
            List<String> properties,
            boolean seekOnly,
            HintIndexType indexType);

    HINT usingJoin(POS p, List<VARIABLE> joinVariables);

    HINT usingScan(POS p, VARIABLE v, String labelOrRelType);

    CLAUSE createClause(POS p, List<PATTERN> patterns);

    CLAUSE insertClause(POS p, List<PATTERN> patterns);

    SET_CLAUSE setClause(POS p, List<SET_ITEM> setItems);

    SET_ITEM setProperty(PROPERTY property, EXPRESSION value);

    SET_ITEM setDynamicProperty(EXPRESSION dynamicProperty, EXPRESSION value);

    SET_ITEM setVariable(VARIABLE variable, EXPRESSION value);

    SET_ITEM addAndSetVariable(VARIABLE variable, EXPRESSION value);

    SET_ITEM setLabels(
            VARIABLE variable, List<StringPos<POS>> labels, List<EXPRESSION> dynamicLabels, boolean containsIs);

    CLAUSE removeClause(POS p, List<REMOVE_ITEM> removeItems);

    REMOVE_ITEM removeProperty(PROPERTY property);

    REMOVE_ITEM removeDynamicProperty(EXPRESSION dynamicProperty);

    REMOVE_ITEM removeLabels(
            VARIABLE variable, List<StringPos<POS>> labels, List<EXPRESSION> dynamicLabels, boolean containsIs);

    CLAUSE deleteClause(POS p, boolean detach, List<EXPRESSION> expressions);

    CLAUSE unwindClause(POS p, EXPRESSION e, VARIABLE v);

    enum MergeActionType {
        OnCreate,
        OnMatch
    }

    CLAUSE mergeClause(
            POS p,
            PATTERN pattern,
            List<SET_CLAUSE> setClauses,
            List<MergeActionType> actionTypes,
            List<POS> positions);

    CLAUSE callClause(
            POS p,
            POS namespacePosition,
            POS procedureNamePosition,
            POS procedureResultPosition,
            List<String> namespace,
            String name,
            List<EXPRESSION> arguments,
            boolean yieldAll,
            List<CALL_RESULT_ITEM> resultItems,
            WHERE where,
            boolean optional);

    CALL_RESULT_ITEM callResultItem(POS p, String name, VARIABLE v);

    PATTERN patternWithSelector(PATTERN_SELECTOR selector, PATTERN patternPart);

    PATTERN namedPattern(VARIABLE v, PATTERN pattern);

    PATTERN shortestPathPattern(POS p, PATTERN_ELEMENT patternElement);

    PATTERN allShortestPathsPattern(POS p, PATTERN_ELEMENT patternElement);

    PATTERN pathPattern(PATTERN_ELEMENT patternElement);

    PATTERN insertPathPattern(List<PATTERN_ATOM> atoms);

    PATTERN_ELEMENT patternElement(List<PATTERN_ATOM> atoms);

    PATTERN_SELECTOR anyPathSelector(String count, POS countPosition, POS position);

    PATTERN_SELECTOR allPathSelector(POS position);

    PATTERN_SELECTOR anyShortestPathSelector(String count, POS countPosition, POS position);

    PATTERN_SELECTOR allShortestPathSelector(POS position);

    PATTERN_SELECTOR shortestGroupsSelector(String count, POS countPosition, POS position);

    NODE_PATTERN nodePattern(
            POS p, VARIABLE v, LABEL_EXPRESSION labelExpression, EXPRESSION properties, EXPRESSION predicate);

    REL_PATTERN relationshipPattern(
            POS p,
            boolean left,
            boolean right,
            VARIABLE v,
            LABEL_EXPRESSION labelExpression,
            PATH_LENGTH pathLength,
            EXPRESSION properties,
            EXPRESSION predicate);

    /**
     * Create a path-length object used to specify path lengths for variable length patterns.
     *
     * Note that paths will be reported in a quite specific manner:
     * Cypher       minLength   maxLength
     * ----------------------------------
     * [*]          null        null
     * [*2]         "2"         "2"
     * [*2..]       "2"         ""
     * [*..3]       ""          "3"
     * [*2..3]      "2"         "3"
     * [*..]        ""          ""      <- separate from [*] to allow specific error messages
     */
    PATH_LENGTH pathLength(POS p, POS pMin, POS pMax, String minLength, String maxLength);

    PATH_PATTERN_QUANTIFIER intervalPathQuantifier(
            POS p, POS posLowerBound, POS posUpperBound, String lowerBound, String upperBound);

    PATH_PATTERN_QUANTIFIER fixedPathQuantifier(POS p, POS valuePos, String value);

    PATH_PATTERN_QUANTIFIER plusPathQuantifier(POS p);

    PATH_PATTERN_QUANTIFIER starPathQuantifier(POS p);

    MATCH_MODE repeatableElements(POS p);

    MATCH_MODE differentRelationships(POS p);

    PATTERN_ATOM parenthesizedPathPattern(
            POS p, PATTERN internalPattern, EXPRESSION where, PATH_PATTERN_QUANTIFIER quantifier);

    PATTERN_ATOM quantifiedRelationship(REL_PATTERN rel, PATH_PATTERN_QUANTIFIER quantifier);

    CLAUSE foreachClause(POS p, VARIABLE v, EXPRESSION list, List<CLAUSE> clauses);

    CLAUSE subqueryClause(
            POS p,
            QUERY subquery,
            SUBQUERY_IN_TRANSACTIONS_PARAMETERS inTransactions,
            boolean scopeAll,
            boolean hasScope,
            List<VARIABLE> variables,
            boolean optional);

    SUBQUERY_IN_TRANSACTIONS_PARAMETERS subqueryInTransactionsParams(
            POS p,
            SUBQUERY_IN_TRANSACTIONS_BATCH_PARAMETERS batchParams,
            SUBQUERY_IN_TRANSACTIONS_CONCURRENCY_PARAMETERS concurrencyParams,
            SUBQUERY_IN_TRANSACTIONS_ERROR_PARAMETERS errorParams,
            SUBQUERY_IN_TRANSACTIONS_REPORT_PARAMETERS reportParams);

    SUBQUERY_IN_TRANSACTIONS_BATCH_PARAMETERS subqueryInTransactionsBatchParameters(POS p, EXPRESSION batchSize);

    SUBQUERY_IN_TRANSACTIONS_CONCURRENCY_PARAMETERS subqueryInTransactionsConcurrencyParameters(
            POS p, EXPRESSION concurrency);

    SUBQUERY_IN_TRANSACTIONS_ERROR_PARAMETERS subqueryInTransactionsErrorParameters(
            POS p, CallInTxsOnErrorBehaviourType onErrorBehaviour);

    SUBQUERY_IN_TRANSACTIONS_REPORT_PARAMETERS subqueryInTransactionsReportParameters(POS p, VARIABLE v);

    CLAUSE orderBySkipLimitClause(
            POS t, List<ORDER_ITEM> order, POS orderPos, EXPRESSION skip, POS skipPos, EXPRESSION limit, POS limitPos);
    // Commands
    STATEMENT_WITH_GRAPH useGraph(STATEMENT_WITH_GRAPH statement, USE_GRAPH useGraph);

    // Show Command Clauses

    YIELD yieldClause(
            POS p,
            boolean returnAll,
            List<RETURN_ITEM> returnItems,
            POS returnItemsPosition,
            List<ORDER_ITEM> orderBy,
            POS orderPos,
            EXPRESSION skip,
            POS skipPosition,
            EXPRESSION limit,
            POS limitPosition,
            WHERE where);

    // Administration command surface is intentionally removed from this lite AST interface.

    void addDeprecatedIdentifierUnicodeNotification(POS p, Character character, String identifier);
}
