package me.n1ar4.jar.analyzer.graph.query;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CypherLogicalModelRewriterTest {
    @Test
    void shouldExpandLogicalCallRelationshipTypeInPattern() {
        String rewritten = CypherLogicalModelRewriter.rewrite(
                "MATCH (m:Method)-[r:CALL]->(n:Method) RETURN m, r, n");

        assertTrue(rewritten.contains("[r:CALLS_DIRECT|CALLS_DISPATCH|CALLS_REFLECTION|CALLS_CALLBACK|CALLS_OVERRIDE|CALLS_INDY|CALLS_METHOD_HANDLE|CALLS_FRAMEWORK|CALLS_PTA]"));
    }

    @Test
    void shouldExpandLogicalCallInVariableLengthMixedPattern() {
        String rewritten = CypherLogicalModelRewriter.rewrite(
                "MATCH p=(m:Method)-[:CALL|ALIAS*1..3]->(n:Method) RETURN p");

        assertTrue(rewritten.contains("[:CALLS_DIRECT|CALLS_DISPATCH|CALLS_REFLECTION|CALLS_CALLBACK|CALLS_OVERRIDE|CALLS_INDY|CALLS_METHOD_HANDLE|CALLS_FRAMEWORK|CALLS_PTA|ALIAS*1..3]"));
    }

    @Test
    void shouldLeaveStringsCommentsAndNonPatternListsUntouched() {
        String query = "RETURN 'MATCH ()-[:CALL]->()' AS text // [:CALL]\nWITH [1, 2, 3] AS xs RETURN xs";
        assertEquals(query, CypherLogicalModelRewriter.rewrite(query));
    }

    @Test
    void shouldKeepAliasPatternAsIs() {
        String query = "MATCH (m)-[:ALIAS]->(n) RETURN m, n";
        assertEquals(query, CypherLogicalModelRewriter.rewrite(query));
    }

    @Test
    void shouldExpandLogicalTypeEqualityPredicate() {
        String rewritten = CypherLogicalModelRewriter.rewrite(
                "MATCH ()-[r]->() WHERE type(r) = 'CALL' RETURN r");

        assertTrue(rewritten.contains("(type(r) = 'CALLS_DIRECT' OR type(r) = 'CALLS_DISPATCH'"));
    }

    @Test
    void shouldExpandLogicalTypeInPredicate() {
        String rewritten = CypherLogicalModelRewriter.rewrite(
                "MATCH ()-[r]->() WHERE type(r) IN ['CALL', 'ALIAS'] RETURN r");

        assertTrue(rewritten.contains("type(r) IN ['CALLS_DIRECT'"));
        assertTrue(rewritten.contains("'ALIAS']"));
    }

    @Test
    void shouldExpandReversedLogicalTypeEqualityPredicate() {
        String rewritten = CypherLogicalModelRewriter.rewrite(
                "MATCH ()-[r]->() WHERE 'CALL' = type(r) RETURN r");

        assertTrue(rewritten.contains("('CALLS_DIRECT' = type(r) OR 'CALLS_DISPATCH' = type(r)"));
    }

    @Test
    void shouldExpandReversedLogicalTypeInequalityPredicate() {
        String rewritten = CypherLogicalModelRewriter.rewrite(
                "MATCH ()-[r]->() WHERE 'CALL' <> type(r) RETURN r");

        assertTrue(rewritten.contains("NOT (type(r) IN ['CALLS_DIRECT'"));
    }

    @Test
    void shouldRewriteParameterizedTypePredicates() {
        String rewritten = CypherLogicalModelRewriter.rewrite(
                "MATCH ()-[r]->() WHERE type(r) = $relType OR type(r) IN $relTypes RETURN r");

        assertTrue(rewritten.contains("(type(r) = $relType OR ja.relGroup(type(r)) = $relType)"));
        assertTrue(rewritten.contains("(type(r) IN $relTypes OR ja.relGroup(type(r)) IN $relTypes)"));
    }

    @Test
    void shouldRewriteReversedParameterizedTypePredicate() {
        String rewritten = CypherLogicalModelRewriter.rewrite(
                "MATCH ()-[r]->() WHERE $relType = type(r) RETURN r");

        assertTrue(rewritten.contains("(type(r) = $relType OR ja.relGroup(type(r)) = $relType)"));
    }

    @Test
    void shouldLeavePhysicalTypeReturnUntouched() {
        String query = "MATCH ()-[r]->() RETURN type(r) AS relType";
        assertEquals(query, CypherLogicalModelRewriter.rewrite(query));
    }
}
