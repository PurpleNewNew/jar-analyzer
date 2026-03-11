package me.n1ar4.jar.analyzer.core.bytecode;

import me.n1ar4.jar.analyzer.core.DiscoveryRunner;
import me.n1ar4.jar.analyzer.core.InheritanceMap;
import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.entity.ClassFileEntity;
import me.n1ar4.support.FixtureJars;
import me.n1ar4.jar.analyzer.utils.CoreUtil;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.SourceValue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildBytecodeWorkspaceTest {
    @Test
    void workspaceShouldReuseParsedClassNodesAndFrameFacts() {
        Path jar = FixtureJars.callbackTestJar();
        List<ClassFileEntity> classFiles = CoreUtil.getAllClassesFromJars(
                List.of(jar.toString()),
                new HashMap<>(java.util.Map.of(jar.toString(), 1)),
                new ArrayList<>(),
                false
        );
        BuildBytecodeWorkspace workspace = BuildBytecodeWorkspace.parse(new HashSet<>(classFiles));

        assertTrue(workspace.parsedClasses().size() >= classFiles.size());

        BuildBytecodeWorkspace.ParsedClass parsedClass = workspace.findClass("me/n1ar4/cb/CallbackEntry", 1);
        assertNotNull(parsedClass);

        BuildBytecodeWorkspace.ParsedMethod parsedMethod = parsedClass.methods().stream()
                .filter(method -> method != null
                        && method.methodNode() != null
                        && "ptaFieldSensitiveDispatch".equals(method.methodNode().name)
                        && "()V".equals(method.methodNode().desc))
                .findFirst()
                .orElse(null);
        assertNotNull(parsedMethod);

        Frame<SourceValue>[] first = parsedMethod.sourceFrames();
        Frame<SourceValue>[] second = parsedMethod.sourceFrames();
        assertNotNull(first);
        assertSame(first, second);

        HashSet<ClassReference> discoveredClasses = new HashSet<>();
        HashSet<MethodReference> discoveredMethods = new HashSet<>();
        HashMap<ClassReference.Handle, ClassReference> classMap = new HashMap<>();
        HashMap<MethodReference.Handle, MethodReference> methodMap = new HashMap<>();
        DiscoveryRunner.start(workspace, discoveredClasses, discoveredMethods, classMap, methodMap, new HashMap<>());
        InheritanceMap inheritanceMap = InheritanceMap.fromClasses(classMap);

        assertTrue(workspace.collectInstantiatedClasses(inheritanceMap).stream().anyMatch(handle ->
                handle != null && "me/n1ar4/cb/FastTask".equals(handle.getName())));
    }

    @Test
    void workspaceShouldTolerateClassFilesWithNullJarId() {
        Path jar = FixtureJars.callbackTestJar();
        List<ClassFileEntity> classFiles = CoreUtil.getAllClassesFromJars(
                List.of(jar.toString()),
                new HashMap<>(java.util.Map.of(jar.toString(), 1)),
                new ArrayList<>(),
                false
        );
        assertTrue(!classFiles.isEmpty());
        classFiles.get(0).setJarId(null);

        BuildBytecodeWorkspace workspace = BuildBytecodeWorkspace.parse(new HashSet<>(classFiles));

        assertTrue(!workspace.parsedClasses().isEmpty());
        assertTrue(workspace.parsedClasses().stream().anyMatch(parsed ->
                parsed != null
                        && Integer.valueOf(-1).equals(parsed.jarId())));
    }
}
