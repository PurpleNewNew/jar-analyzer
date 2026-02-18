/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.graph.plan;

import me.n1ar4.jar.analyzer.graph.cypher.AstNormalizer;
import me.n1ar4.jar.analyzer.graph.cypher.CypherParserFacade;

public final class Planner {
    private final CypherParserFacade parser = new CypherParserFacade();

    public LogicalPlan plan(String query) {
        parser.validate(query);
        AstNormalizer.NormalizedCypher normalized = AstNormalizer.normalize(query);
        if (normalized.isProcedure()) {
            return new LogicalPlan(
                    LogicalPlan.Kind.PROCEDURE,
                    normalized.getRawQuery(),
                    normalized.getProcedureName(),
                    normalized.getProcedureArgs(),
                    "",
                    normalized.getLimit(),
                    normalized.getSkip(),
                    false,
                    false,
                    1
            );
        }
        return new LogicalPlan(
                LogicalPlan.Kind.MATCH,
                normalized.getRawQuery(),
                "",
                normalized.getProcedureArgs(),
                normalized.getLabelHint(),
                normalized.getLimit(),
                normalized.getSkip(),
                normalized.isRelationshipPattern(),
                normalized.isShortestPath(),
                normalized.getMaxHops()
        );
    }
}
