package org.benf.cfr.reader.bytecode;

import org.benf.cfr.reader.api.CfrDriver;
import org.benf.cfr.reader.api.OutputSinkFactory;
import org.benf.cfr.reader.api.SinkReturns;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SwitchPatternRewriterTest {
    @Test
    void shouldRewriteJava21TypeSwitchIntoPatternCases(@TempDir Path tempDir) throws IOException {
        Path source = tempDir.resolve("SwitchPatternSample.java");
        Files.writeString(source, """
                public class SwitchPatternSample {
                    static String test(Object o) {
                        return switch (o) {
                            case String s -> s;
                            case Integer i when i > 10 -> "big" + i;
                            case Integer i -> "small" + i;
                            case Point(int x, int y) -> x + "," + y;
                            case null -> "nil";
                            default -> "other";
                        };
                    }
                    record Point(int x, int y) {}
                }
                """, StandardCharsets.UTF_8);

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "JDK compiler is required for CFR modern syntax regression tests");
        int compileExit = compiler.run(null, null, null,
                "--release", "21",
                "-proc:none",
                "-g",
                "-d", tempDir.toString(),
                source.toString());
        assertEquals(0, compileExit);

        String decompiled = decompile(tempDir.resolve("SwitchPatternSample.class"));

        assertFalse(decompiled.contains("SwitchBootstraps.typeSwitch"));
        assertFalse(decompiled.contains("while (true)"));
        assertFalse(decompiled.contains("continue block"));
        assertFalse(decompiled.contains("MatchException"));
        assertTrue(Pattern.compile("case String \\w+:").matcher(decompiled).find());
        assertTrue(Pattern.compile("case Integer (\\w+) when \\1 > 10:").matcher(decompiled).find());
        assertTrue(Pattern.compile("case Integer \\w+:").matcher(decompiled).find());
        assertTrue(Pattern.compile("case Point\\(int \\w+, int \\w+\\):").matcher(decompiled).find());
        assertFalse(Pattern.compile("case Point \\w+:").matcher(decompiled).find());
        assertTrue(decompiled.contains("case null:"));
        assertTrue(Pattern.compile("switch \\([^\\n]*\\)").matcher(decompiled).find());
    }

    private String decompile(Path classFile) {
        StringBuilder sinkOutput = new StringBuilder();
        OutputSinkFactory sinkFactory = new OutputSinkFactory() {
            @Override
            public List<SinkClass> getSupportedSinks(SinkType sinkType, Collection<SinkClass> available) {
                if (sinkType == SinkType.JAVA) {
                    return Arrays.asList(SinkClass.DECOMPILED, SinkClass.STRING);
                }
                return Arrays.asList(SinkClass.STRING, SinkClass.EXCEPTION_MESSAGE);
            }

            @Override
            public <T> Sink<T> getSink(SinkType sinkType, SinkClass sinkClass) {
                if (sinkType == SinkType.JAVA && sinkClass == SinkClass.DECOMPILED) {
                    return t -> sinkOutput.append(((SinkReturns.Decompiled) t).getJava());
                }
                return t -> {
                    if (t != null) {
                        sinkOutput.append(String.valueOf(t)).append('\n');
                    }
                };
            }
        };

        Map<String, String> options = new HashMap<String, String>();
        options.put("showversion", "false");
        options.put("silent", "true");

        CfrDriver driver = new CfrDriver.Builder()
                .withOptions(options)
                .withOutputSink(sinkFactory)
                .build();
        driver.analyse(Collections.singletonList(classFile.toString()));
        return sinkOutput.toString();
    }
}
