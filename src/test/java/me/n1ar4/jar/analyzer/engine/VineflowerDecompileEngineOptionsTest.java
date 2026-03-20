package me.n1ar4.jar.analyzer.engine;

import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VineflowerDecompileEngineOptionsTest {
    @Test
    void shouldBuildCommonOptionsWithBytecodeMapping() {
        Map<String, String> options = VineflowerDecompileEngine.buildCommonOptions(true);

        assertEquals("1", options.get(IFernflowerPreferences.DECOMPILE_INNER));
        assertEquals("1", options.get(IFernflowerPreferences.DECOMPILE_GENERIC_SIGNATURES));
        assertEquals("1", options.get(IFernflowerPreferences.PATTERN_MATCHING));
        assertEquals("1", options.get(IFernflowerPreferences.BYTECODE_SOURCE_MAPPING));
        assertEquals("1", options.get(IFernflowerPreferences.DUMP_CODE_LINES));
        assertFalse(options.containsKey(IFernflowerPreferences.THREADS));
        assertFalse(options.containsKey("outputdir"));
    }

    @Test
    void shouldBuildBatchOptionsWithExplicitThreadcount() {
        Map<String, String> options = VineflowerDecompileEngine.buildBatchOptions(Path.of("out"));

        assertEquals("0", options.get(IFernflowerPreferences.THREADS));
        assertEquals("true", options.get("clobber"));
        assertEquals("UTF-8", options.get("outputencoding"));
        assertTrue(options.containsKey("outputdir"));
        assertFalse(options.containsKey(IFernflowerPreferences.BYTECODE_SOURCE_MAPPING));
    }
}
