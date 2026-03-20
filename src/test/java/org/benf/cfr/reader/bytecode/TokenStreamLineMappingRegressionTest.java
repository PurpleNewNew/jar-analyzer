package org.benf.cfr.reader.bytecode;

import org.benf.cfr.reader.api.CfrDriver;
import org.benf.cfr.reader.api.OutputSinkFactory;
import org.benf.cfr.reader.api.SinkReturns;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenStreamLineMappingRegressionTest {
    @Test
    void tokenStreamShouldUseSameCurrentLineAsStringSink(@TempDir Path tempDir) throws IOException {
        String source = ""
                + "public class LineSample {\n"
                + "    static int test(int x) {\n"
                + "        int a = x + 1;\n"
                + "        int b = a * 2;\n"
                + "        return b;\n"
                + "    }\n"
                + "}\n";
        Path classFile = CfrDecompilerRegressionSupport.compileJava(
                tempDir,
                "LineSample",
                source,
                "--release", "21");

        NavigableMap<Integer, Integer> stringMappings = decompileAndCollectMappings(classFile, OutputSinkFactory.SinkClass.STRING);
        NavigableMap<Integer, Integer> tokenMappings = decompileAndCollectMappings(classFile, OutputSinkFactory.SinkClass.TOKEN_STREAM);

        assertNotNull(stringMappings);
        assertNotNull(tokenMappings);
        assertEquals(stringMappings.navigableKeySet(), tokenMappings.navigableKeySet());

        Integer lineDelta = null;
        for (Entry<Integer, Integer> entry : stringMappings.entrySet()) {
            Integer tokenLine = tokenMappings.get(entry.getKey());
            assertNotNull(tokenLine);
            int currentDelta = entry.getValue() - tokenLine;
            if (lineDelta == null) {
                lineDelta = currentDelta;
            } else {
                assertEquals(lineDelta.intValue(), currentDelta);
            }
        }
        assertTrue(lineDelta != null && lineDelta >= 0);
    }

    private static NavigableMap<Integer, Integer> decompileAndCollectMappings(Path classFile,
                                                                              OutputSinkFactory.SinkClass javaSink) {
        List<SinkReturns.LineNumberMapping> mappings = new ArrayList<SinkReturns.LineNumberMapping>();
        OutputSinkFactory sinkFactory = new OutputSinkFactory() {
            @Override
            public List<SinkClass> getSupportedSinks(SinkType sinkType, Collection<SinkClass> available) {
                if (sinkType == SinkType.JAVA) {
                    return Collections.singletonList(javaSink);
                }
                if (sinkType == SinkType.LINENUMBER) {
                    return Collections.singletonList(SinkClass.LINE_NUMBER_MAPPING);
                }
                return Collections.singletonList(SinkClass.STRING);
            }

            @Override
            public <T> Sink<T> getSink(SinkType sinkType, SinkClass sinkClass) {
                if (sinkType == SinkType.LINENUMBER && sinkClass == SinkClass.LINE_NUMBER_MAPPING) {
                    return t -> mappings.add((SinkReturns.LineNumberMapping) t);
                }
                return t -> {
                };
            }
        };

        CfrDriver driver = new CfrDriver.Builder()
                .withOptions(Map.of("trackbytecodeloc", "true", "silent", "true"))
                .withOutputSink(sinkFactory)
                .build();
        driver.analyse(Collections.singletonList(classFile.toString()));

        for (SinkReturns.LineNumberMapping mapping : mappings) {
            if (mapping != null && "test".equals(mapping.methodName())) {
                return mapping.getMappings();
            }
        }
        return null;
    }
}
