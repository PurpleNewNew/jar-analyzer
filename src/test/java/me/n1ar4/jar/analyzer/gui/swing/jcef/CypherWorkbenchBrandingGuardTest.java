/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.gui.swing.jcef;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

class CypherWorkbenchBrandingGuardTest {
    @Test
    void shouldNotContainDeprecatedBrandingOrConnectFlowWords() throws Exception {
        List<Path> roots = List.of(
                Path.of("src/main/java/me/n1ar4/jar/analyzer/gui/swing/panel/CypherToolPanel.java"),
                Path.of("src/main/java/me/n1ar4/jar/analyzer/gui/swing/jcef"),
                Path.of("frontend/cypher-workbench/index.html"),
                Path.of("frontend/cypher-workbench/src")
        );
        List<String> hits = new ArrayList<>();
        for (Path root : roots) {
            if (!Files.exists(root)) {
                continue;
            }
            if (Files.isRegularFile(root)) {
                collectHits(root, hits);
                continue;
            }
            try (Stream<Path> stream = Files.walk(root)) {
                stream.filter(Files::isRegularFile).forEach(path -> collectHits(path, hits));
            }
        }
        Assertions.assertTrue(
                hits.isEmpty(),
                "forbidden text found in workbench sources: " + String.join("; ", hits)
        );
    }

    private static void collectHits(Path file, List<String> hits) {
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        if (!(name.endsWith(".java") || name.endsWith(".ts") || name.endsWith(".tsx")
                || name.endsWith(".css") || name.endsWith(".html"))) {
            return;
        }
        try {
            String content = Files.readString(file, StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
            if (content.contains("neo4j") || content.contains(":server connect")) {
                hits.add(file + " contains forbidden wording");
            }
        } catch (Exception ex) {
            hits.add(file + " read failed: " + ex.getMessage());
        }
    }
}
