/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.gui.swing.cypher.model;

public record ScriptItem(
        long scriptId,
        String title,
        String body,
        String tags,
        boolean pinned,
        long createdAt,
        long updatedAt
) {
}
