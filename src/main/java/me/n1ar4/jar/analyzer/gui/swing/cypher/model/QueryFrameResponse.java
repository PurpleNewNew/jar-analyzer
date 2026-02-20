/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.gui.swing.cypher.model;

public record QueryFrameResponse(
        boolean ok,
        String code,
        String message,
        QueryFramePayload frame
) {
    public static QueryFrameResponse ok(QueryFramePayload frame) {
        return new QueryFrameResponse(true, "", "", frame);
    }

    public static QueryFrameResponse error(String code, String message) {
        return new QueryFrameResponse(false, safe(code), safe(message), null);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
