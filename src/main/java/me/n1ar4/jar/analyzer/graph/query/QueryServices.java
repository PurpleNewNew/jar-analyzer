/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.graph.query;

public final class QueryServices {
    private static final SqlQueryService SQL = new SqlQueryService();
    private static final CypherQueryService CYPHER = new CypherQueryService();

    private QueryServices() {
    }

    public static SqlQueryService sql() {
        return SQL;
    }

    public static CypherQueryService cypher() {
        return CYPHER;
    }
}
