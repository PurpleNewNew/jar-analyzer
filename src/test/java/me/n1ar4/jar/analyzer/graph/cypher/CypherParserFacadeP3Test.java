/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.graph.cypher;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class CypherParserFacadeP3Test {
    private final CypherParserFacade parser = new CypherParserFacade();

    private void assertCommandRejected(String query) {
        IllegalArgumentException ex = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> parser.validate(query)
        );
        Assertions.assertTrue(ex.getMessage().contains("cypher_parse_error"));
    }

    @Test
    void shouldRejectShowAndTerminateAtParseBoundary() {
        assertCommandRejected("SHOW INDEXES");
        assertCommandRejected("TERMINATE TRANSACTIONS '1'");
        assertCommandRejected("CREATE CONSTRAINT c IF NOT EXISTS FOR (n:A) REQUIRE n.id IS UNIQUE");
        assertCommandRejected("DROP INDEX idx");
    }

    @Test
    void shouldKeepCoreCypherReadFeaturesAvailable() {
        parser.validate("UNWIND [1,2,3] AS x RETURN x");
        parser.validate("MATCH (n) WITH n RETURN n");
        parser.validate("MATCH (n) OPTIONAL MATCH (n)-[r]->(m) RETURN n, r, m");
        parser.validate("CALL { MATCH (n) RETURN n LIMIT 1 } RETURN 1");
        parser.validate("MATCH (n) RETURN count(n) AS c");
        parser.validate("MATCH p=(a)-[*1..3]->(b) RETURN p LIMIT 1");
    }
}
