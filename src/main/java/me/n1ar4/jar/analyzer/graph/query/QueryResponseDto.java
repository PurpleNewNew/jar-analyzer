/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.graph.query;

import java.util.List;
import java.util.Map;

public record QueryResponseDto(
        List<String> columns,
        List<List<Object>> rows,
        Map<String, Object> meta,
        List<String> warnings
) {
}
