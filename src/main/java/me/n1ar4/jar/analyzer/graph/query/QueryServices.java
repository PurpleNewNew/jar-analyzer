/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.graph.query;

public final class QueryServices {
    private static final Neo4jQueryService CYPHER = new Neo4jQueryService();

    private QueryServices() {
    }

    public static Neo4jQueryService cypher() {
        return CYPHER;
    }
}
