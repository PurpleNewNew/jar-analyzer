/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.graph.query;

import java.util.Map;

public record QueryRequestDto(
        String query,
        Map<String, Object> params,
        Map<String, Object> options
) {
}
