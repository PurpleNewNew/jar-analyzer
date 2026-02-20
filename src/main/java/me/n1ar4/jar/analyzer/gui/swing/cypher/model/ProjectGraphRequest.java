/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.gui.swing.cypher.model;

import java.util.List;

public record ProjectGraphRequest(
        String query,
        List<String> columns,
        List<List<Object>> rows,
        List<String> warnings,
        boolean truncated
) {
}
