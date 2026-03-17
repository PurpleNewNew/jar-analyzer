package org.benf.cfr.reader.bytecode;

import org.benf.cfr.reader.api.CfrDriver;
import org.benf.cfr.reader.api.OutputSinkFactory;
import org.benf.cfr.reader.api.SinkReturns;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

final class CfrDecompilerRegressionSupport {
    private CfrDecompilerRegressionSupport() {
    }

    static Path compileJava(Path tempDir, String className, String source, String... javacArgs) throws IOException {
        Path sourceFile = tempDir.resolve(className + ".java");
        Files.writeString(sourceFile, source, StandardCharsets.UTF_8);

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "JDK compiler is required for CFR regression tests");

        String[] args = new String[javacArgs.length + 5];
        System.arraycopy(javacArgs, 0, args, 0, javacArgs.length);
        args[javacArgs.length] = "-proc:none";
        args[javacArgs.length + 1] = "-g";
        args[javacArgs.length + 2] = "-d";
        args[javacArgs.length + 3] = tempDir.toString();
        args[javacArgs.length + 4] = sourceFile.toString();
        int compileExit = compiler.run(null, null, null, args);
        assertEquals(0, compileExit);
        return tempDir.resolve(className + ".class");
    }

    static String decompile(Path classFile) {
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
