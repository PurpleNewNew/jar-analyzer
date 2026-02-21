/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.graph.cypher;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class CypherParserFacadeP3Test {
    private final CypherParserFacade parser = new CypherParserFacade();

    @Test
    void shouldRejectShowAndTerminateAtParseBoundary() {
        IllegalArgumentException showEx = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> parser.validate("SHOW INDEXES")
        );
        Assertions.assertTrue(showEx.getMessage().contains("cypher_parse_error"));

        IllegalArgumentException terminateEx = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> parser.validate("TERMINATE TRANSACTIONS '1'")
        );
        Assertions.assertTrue(terminateEx.getMessage().contains("cypher_parse_error"));
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
