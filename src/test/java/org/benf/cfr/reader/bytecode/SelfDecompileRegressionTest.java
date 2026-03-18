package org.benf.cfr.reader.bytecode;

import me.n1ar4.jar.analyzer.engine.CFRDecompileEngine;
import me.n1ar4.jar.analyzer.engine.ClassLookupService;
import me.n1ar4.jar.analyzer.utils.ClasspathRegistry;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SelfDecompileRegressionTest {
    @Test
    void shouldKeepJarAnalyzerCfrEngineStructured() throws IOException {
        String decompiled = CfrDecompilerRegressionSupport.decompile(
                CfrDecompilerRegressionSupport.locateClassFile(CFRDecompileEngine.class));

        assertStructured(decompiled);
        assertTrue(decompiled.contains("private static List<Path> collectNestedLibJars(Path resourcesRoot)"), decompiled);
        assertTrue(decompiled.contains("stream.forEach(path -> {"), decompiled);
        assertTrue(decompiled.contains("isNestedLibPath(rel = root.relativize"), decompiled);
        assertTrue(decompiled.contains("new BuildScopedLru<String, String>("), decompiled);
        assertTrue(decompiled.contains("new LinkedHashMap<Path, List<String>>()"), decompiled);
        assertTrue(decompiled.contains("new ArrayList<String>()"), decompiled);
        assertFalse(decompiled.contains("new BuildScopedLru<K, V>("), decompiled);
        assertFalse(decompiled.contains("new LinkedHashMap<Path, List>("), decompiled);
        assertFalse(decompiled.contains("new ArrayList<E>()"), decompiled);
    }

    @Test
    void shouldKeepJarAnalyzerClasspathRegistryStructured() throws IOException {
        String decompiled = CfrDecompilerRegressionSupport.decompile(
                CfrDecompilerRegressionSupport.locateClassFile(ClasspathRegistry.class));

        assertStructured(decompiled);
        assertTrue(decompiled.contains("private static boolean matchesAllowedHash(String dirName, Set<String> allowedHashes)"), decompiled);
        assertTrue(decompiled.contains("allowedHashes.isEmpty()"), decompiled);
        assertTrue(decompiled.contains("return allowedHashes.contains(suffix);"), decompiled);
    }

    @Test
    void shouldKeepJarAnalyzerClassLookupStructured() throws IOException {
        String decompiled = CfrDecompilerRegressionSupport.decompile(
                CfrDecompilerRegressionSupport.locateClassFile(ClassLookupService.class));

        assertStructured(decompiled);
        assertTrue(decompiled.contains("public static LookupResult find(String nameOrPath, Integer preferJarId)"), decompiled);
        assertTrue(decompiled.contains("raw.indexOf(33)) > 0 && ClassLookupService.looksLikePath(raw)"), decompiled);
        assertTrue(decompiled.contains("ClassLookupService.findClassInternal("), decompiled);
        assertTrue(decompiled.contains("return new LookupResult(data, path.toString(), null, path.toString());"), decompiled);
    }

    private static void assertStructured(String decompiled) {
        assertFalse(decompiled.contains("Unable to fully structure code"), decompiled);
        assertFalse(decompiled.contains("** GOTO"), decompiled);
    }
}
