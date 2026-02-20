/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.gui.swing.cypher.model;

import java.util.Map;

public record QueryFrameRequest(
        String query,
        Map<String, Object> params,
        Map<String, Object> options
) {
}
