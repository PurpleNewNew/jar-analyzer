/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.core;

import me.n1ar4.jar.analyzer.core.bytecode.BuildBytecodeWorkspace;
import me.n1ar4.jar.analyzer.core.facts.CallSiteEntity;
import me.n1ar4.jar.analyzer.core.facts.ClassFileEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.Opcodes;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class BytecodeSymbolRunnerInvokeDynamicTest {
    @TempDir
    Path tempDir;

    @Test
    void shouldGenerateDistinctKeysForDuplicateInvokeDynamicSitesInSameMethod() throws Exception {
        Path classesDir = compileClass("""
                package demo;

                public class IndyDup {
                    private void target() {
                    }

                    public void build() {
                        Runnable first = this::target;
                        Runnable second = this::target;
                    }
                }
                """);
        Path classFile = classesDir.resolve("demo/IndyDup.class");
        ClassFileEntity entity = new ClassFileEntity();
        entity.setClassName("demo/IndyDup.class");
        entity.setPath(classFile);
        entity.setPathStr(classFile.toString());
        entity.setJarId(1);
        entity.setJarName("demo.jar");

        BuildBytecodeWorkspace workspace = BuildBytecodeWorkspace.parse(Set.of(entity));
        List<CallSiteEntity> sites = BytecodeSymbolRunner.collectCallSites(workspace).stream()
                .filter(site -> "build".equals(site.getCallerMethodName()))
                .filter(site -> site.getOpCode() != null && site.getOpCode() == Opcodes.INVOKEDYNAMIC)
                .toList();

        assertEquals(2, sites.size());
        assertEquals(2, sites.stream().map(CallSiteEntity::getCallSiteKey).distinct().count());
    }

    private Path compileClass(String source) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "JDK compiler is required");

        Path srcDir = tempDir.resolve("src");
        Path classesDir = tempDir.resolve("classes");
        Files.createDirectories(srcDir);
        Files.createDirectories(classesDir);
        Path sourceFile = srcDir.resolve("demo/IndyDup.java");
        Files.createDirectories(sourceFile.getParent());
        Files.writeString(sourceFile, source, StandardCharsets.UTF_8);

        int exit = compiler.run(null, null, null,
                "-d", classesDir.toString(),
                sourceFile.toString());
        assertEquals(0, exit);
        return classesDir;
    }
}
