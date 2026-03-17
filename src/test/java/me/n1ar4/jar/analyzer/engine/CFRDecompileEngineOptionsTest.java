package me.n1ar4.jar.analyzer.engine;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CFRDecompileEngineOptionsTest {
    @Test
    void shouldBuildCommonOptionsWithoutBatchSideEffects() {
        Map<String, String> options = CFRDecompileEngine.buildCommonOptions(true);

        assertEquals("false", options.get("showversion"));
        assertEquals("false", options.get("hideutf"));
        assertEquals("true", options.get("innerclasses"));
        assertEquals("true", options.get("trackbytecodeloc"));
        assertFalse(options.containsKey("threadcount"));
        assertFalse(options.containsKey("outputdir"));
    }

    @Test
    void shouldBuildBatchOptionsWithExplicitThreadcount() {
        Map<String, String> options = CFRDecompileEngine.buildBatchOptions(Path.of("out"));

        assertEquals("0", options.get("threadcount"));
        assertEquals("true", options.get("clobber"));
        assertEquals("UTF-8", options.get("outputencoding"));
        assertTrue(options.containsKey("outputdir"));
        assertFalse(options.containsKey("trackbytecodeloc"));
    }
}
