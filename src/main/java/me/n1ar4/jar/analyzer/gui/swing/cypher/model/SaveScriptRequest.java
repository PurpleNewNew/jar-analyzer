/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.gui.swing.cypher.model;

public record SaveScriptRequest(
        Long scriptId,
        String title,
        String body,
        String tags,
        boolean pinned
) {
}
