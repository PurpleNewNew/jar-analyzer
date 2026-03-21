package org.benf.cfr.reader.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DecompilerCommentsContractTest {
    @Test
    void shouldTreatPreviewOnlyCommentsAsClean() {
        DecompilerComments comments = new DecompilerComments();
        comments.addComment(DecompilerComment.PREVIEW_FEATURE);

        assertEquals(DecompilationQuality.CLEAN, comments.getQuality());
        assertFalse(comments.isDegraded());
        assertTrue(comments.getDegradingComments().isEmpty());
    }

    @Test
    void shouldTreatRecoveryWarningsAsDegraded() {
        DecompilerComments comments = new DecompilerComments();
        comments.addComment(DecompilerComment.LOOPING_EXCEPTIONS);

        assertEquals(DecompilationQuality.DEGRADED, comments.getQuality());
        assertTrue(comments.isDegraded());
        assertEquals(1, comments.getDegradingComments().size());
    }

    @Test
    void shouldPromoteHardFailuresAboveSoftDegradation() {
        DecompilerComments comments = new DecompilerComments();
        comments.addComment(DecompilerComment.MALFORMED_SWITCH);
        comments.addComment(DecompilerComment.UNABLE_TO_STRUCTURE);

        assertEquals(DecompilationQuality.FAILED, comments.getQuality());
        assertEquals(2, comments.getDegradingComments().size());
    }
}
