/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.gui.swing.jcef;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

class CypherWorkbenchTemplateContractTest {
    @Test
    void taintTemplateShouldUseCurrentTrackSignature() throws Exception {
        Path scripts = Path.of("frontend/cypher-workbench/src/scripts.ts");
        String content = Files.readString(scripts, StandardCharsets.UTF_8);

        Assertions.assertTrue(
                content.contains(
                        "CALL ja.taint.track(\"\", \"\", \"\", \"app/Sink\", \"sink\", \"()V\", 8, 15000, 10, true, false, {{TRAVERSAL_MODE_LITERAL}}, \"backward\")"
                ),
                "taint template should match current ja.taint.track signature"
        );
        Assertions.assertFalse(
                content.contains(
                        "CALL ja.taint.track(\"\", \"\", \"\", \"app/Sink\", \"sink\", \"()V\", 8, 15000, 10, \"sink\", true, false"
                ),
                "taint template should not retain removed legacy position arguments"
        );
    }
}
