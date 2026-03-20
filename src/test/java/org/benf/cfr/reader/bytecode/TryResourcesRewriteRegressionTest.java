package org.benf.cfr.reader.bytecode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TryResourcesRewriteRegressionTest {
    @Test
    void shouldResugarTryWithResourcesWithoutSyntheticCloseScaffolding(@TempDir Path tempDir) throws IOException {
        String source = """
                import java.io.StringWriter;

                public class TryResourcesRewriteSample {
                    static String read() throws Exception {
                        try (StringWriter writer = new StringWriter()) {
                            writer.write("ok");
                            return writer.toString();
                        }
                    }
                }
                """;

        Path classFile = CfrDecompilerRegressionSupport.compileJava(tempDir, "TryResourcesRewriteSample", source, "--release", "21");

        String decompiled = CfrDecompilerRegressionSupport.decompileJava(classFile);

        assertTrue(Pattern.compile("try \\(StringWriter \\w+ = new StringWriter\\(\\);?\\)").matcher(decompiled).find(), decompiled);
        assertFalse(decompiled.contains("addSuppressed"), decompiled);
        assertFalse(decompiled.contains(".close();"), decompiled);

        Path decompiledDir = Files.createDirectories(tempDir.resolve("decompiled-try-resources"));
        CfrDecompilerRegressionSupport.compileJava(decompiledDir, "TryResourcesRewriteSample", decompiled, "--release", "21");
    }
}
