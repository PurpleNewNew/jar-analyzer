/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.graph.query;

import me.n1ar4.jar.analyzer.storage.neo4j.Neo4jStore;

public final class QueryServices {
    private static final CypherQueryService CYPHER = new CypherQueryService(Neo4jStore.getInstance());

    private QueryServices() {
    }

    public static CypherQueryService cypher() {
        return CYPHER;
    }
}
