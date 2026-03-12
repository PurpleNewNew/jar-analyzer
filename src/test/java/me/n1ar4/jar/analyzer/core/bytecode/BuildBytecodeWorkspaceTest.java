package me.n1ar4.jar.analyzer.core.bytecode;

import me.n1ar4.jar.analyzer.core.BytecodeSymbolRunner;
import me.n1ar4.jar.analyzer.core.ClassAnalysisRunner;
import me.n1ar4.jar.analyzer.core.DiscoveryRunner;
import me.n1ar4.jar.analyzer.core.InheritanceMap;
import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.entity.ClassFileEntity;
import me.n1ar4.support.FixtureJars;
import me.n1ar4.jar.analyzer.utils.CoreUtil;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.SourceValue;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @Test
    void workspaceRunnersShouldMatchSequentialAndParallelResults() {
        Path jar = FixtureJars.callbackTestJar();
        List<ClassFileEntity> classFiles = CoreUtil.getAllClassesFromJars(
                List.of(jar.toString()),
                new HashMap<>(Map.of(jar.toString(), 1)),
                new ArrayList<>(),
                false
        );
        BuildBytecodeWorkspace workspace = BuildBytecodeWorkspace.parse(new HashSet<>(classFiles));

        DiscoverySnapshot sequentialDiscovery = withProperty("jar.analyzer.discovery.threads", "1",
                () -> runDiscovery(workspace));
        DiscoverySnapshot parallelDiscovery = withProperty("jar.analyzer.discovery.threads", "2",
                () -> runDiscovery(workspace));
        assertEquals(sequentialDiscovery.classHandles, parallelDiscovery.classHandles);
        assertEquals(sequentialDiscovery.methodHandles, parallelDiscovery.methodHandles);
        assertEquals(sequentialDiscovery.stringAnnoMap, parallelDiscovery.stringAnnoMap);

        ClassAnalysisSnapshot sequentialAnalysis = withProperty("jar.analyzer.class.analysis.threads", "1",
                () -> runClassAnalysis(workspace, sequentialDiscovery.classMap, sequentialDiscovery.methodMap));
        ClassAnalysisSnapshot parallelAnalysis = withProperty("jar.analyzer.class.analysis.threads", "2",
                () -> runClassAnalysis(workspace, sequentialDiscovery.classMap, sequentialDiscovery.methodMap));
        assertEquals(sequentialAnalysis.strings, parallelAnalysis.strings);
        assertEquals(sequentialAnalysis.controllers, parallelAnalysis.controllers);
        assertEquals(sequentialAnalysis.interceptors, parallelAnalysis.interceptors);
        assertEquals(sequentialAnalysis.servlets, parallelAnalysis.servlets);
        assertEquals(sequentialAnalysis.filters, parallelAnalysis.filters);
        assertEquals(sequentialAnalysis.listeners, parallelAnalysis.listeners);

        SymbolSnapshot sequentialSymbol = withProperty("jar.analyzer.symbol.threads", "1",
                () -> runSymbol(workspace));
        SymbolSnapshot parallelSymbol = withProperty("jar.analyzer.symbol.threads", "2",
                () -> runSymbol(workspace));
        assertEquals(sequentialSymbol.callSites, parallelSymbol.callSites);
        assertEquals(sequentialSymbol.localVars, parallelSymbol.localVars);
    }

    @Test
    void symbolWorkspaceShouldNotLoadFramesForMethodsWithoutVirtualDispatch() throws Exception {
        Path jar = FixtureJars.callbackTestJar();
        List<ClassFileEntity> classFiles = CoreUtil.getAllClassesFromJars(
                List.of(jar.toString()),
                new HashMap<>(Map.of(jar.toString(), 1)),
                new ArrayList<>(),
                false
        );
        BuildBytecodeWorkspace workspace = BuildBytecodeWorkspace.parse(new HashSet<>(classFiles));
        BuildBytecodeWorkspace.ParsedMethod candidate = workspace.parsedClasses().stream()
                .filter(parsedClass -> parsedClass != null && parsedClass.methods() != null)
                .flatMap(parsedClass -> parsedClass.methods().stream())
                .filter(method -> method != null
                        && method.methodNode() != null
                        && method.methodNode().instructions != null
                        && method.methodNode().instructions.size() > 0
                        && !containsVirtualInvoke(method.methodNode()))
                .findFirst()
                .orElse(null);
        assertNotNull(candidate);
        assertFalse(sourceFramesLoaded(candidate));

        BytecodeSymbolRunner.collectCallSites(workspace);

        assertFalse(sourceFramesLoaded(candidate));
    }

    @Test
    void workspaceShouldFailForInvalidClassBytes() {
        HashSet<ClassFileEntity> classFiles = new HashSet<>();
        classFiles.add(invalidClass("bad/A", 1));
        classFiles.add(invalidClass("bad/B", 2));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> BuildBytecodeWorkspace.parse(classFiles));
        assertTrue(ex.getMessage().contains("parse class node failed"));
    }

    private static DiscoverySnapshot runDiscovery(BuildBytecodeWorkspace workspace) {
        HashSet<ClassReference> discoveredClasses = new HashSet<>();
        HashSet<MethodReference> discoveredMethods = new HashSet<>();
        HashMap<ClassReference.Handle, ClassReference> classMap = new HashMap<>();
        HashMap<MethodReference.Handle, MethodReference> methodMap = new HashMap<>();
        HashMap<MethodReference.Handle, List<String>> stringAnnoMap = new HashMap<>();
        DiscoveryRunner.start(workspace, discoveredClasses, discoveredMethods, classMap, methodMap, stringAnnoMap);
        return new DiscoverySnapshot(
                classMap,
                methodMap,
                new TreeSet<>(classMap.keySet().stream().map(Object::toString).toList()),
                new TreeSet<>(methodMap.keySet().stream().map(Object::toString).toList()),
                normalizeStringMap(stringAnnoMap)
        );
    }

    private static ClassAnalysisSnapshot runClassAnalysis(BuildBytecodeWorkspace workspace,
                                                          Map<ClassReference.Handle, ClassReference> classMap,
                                                          Map<MethodReference.Handle, MethodReference> methodMap) {
        HashMap<MethodReference.Handle, List<String>> strMap = new HashMap<>();
        ArrayList<String> interceptors = new ArrayList<>();
        ArrayList<String> servlets = new ArrayList<>();
        ArrayList<String> filters = new ArrayList<>();
        ArrayList<String> listeners = new ArrayList<>();
        ArrayList<me.n1ar4.jar.analyzer.analyze.spring.SpringController> controllers = new ArrayList<>();
        ClassAnalysisRunner.start(
                workspace,
                methodMap,
                strMap,
                classMap,
                controllers,
                interceptors,
                servlets,
                filters,
                listeners,
                true,
                true,
                true
        );
        return new ClassAnalysisSnapshot(
                normalizeStringMap(strMap),
                new TreeSet<>(controllers.stream().map(Object::toString).toList()),
                new TreeSet<>(interceptors),
                new TreeSet<>(servlets),
                new TreeSet<>(filters),
                new TreeSet<>(listeners)
        );
    }

    private static SymbolSnapshot runSymbol(BuildBytecodeWorkspace workspace) {
        TreeSet<String> callSites = new TreeSet<>();
        BytecodeSymbolRunner.collectCallSites(workspace).forEach(site -> callSites.add(
                site.getCallerClassName() + "|" + site.getCallerMethodName() + "|" + site.getCallerMethodDesc()
                        + "|" + site.getCalleeOwner() + "|" + site.getCalleeMethodName() + "|"
                        + site.getCalleeMethodDesc() + "|" + site.getOpCode() + "|" + site.getCallSiteKey()
        ));
        TreeSet<String> localVars = new TreeSet<>();
        BytecodeSymbolRunner.collectLocalVars(workspace).forEach(localVar -> localVars.add(
                localVar.getClassName() + "|" + localVar.getMethodName() + "|" + localVar.getMethodDesc()
                        + "|" + localVar.getVarIndex() + "|" + localVar.getVarName() + "|" + localVar.getVarDesc()
        ));
        return new SymbolSnapshot(callSites, localVars);
    }

    private static TreeMap<String, List<String>> normalizeStringMap(Map<MethodReference.Handle, List<String>> source) {
        TreeMap<String, List<String>> out = new TreeMap<>();
        source.forEach((handle, values) -> {
            if (handle == null || values == null) {
                return;
            }
            ArrayList<String> copy = new ArrayList<>(values);
            copy.sort(String::compareTo);
            out.put(handle.toString(), List.copyOf(copy));
        });
        return out;
    }

    private static <T> T withProperty(String key, String value, java.util.concurrent.Callable<T> action) {
        String previous = System.getProperty(key);
        try {
            if (value == null) {
                System.clearProperty(key);
            } else {
                System.setProperty(key, value);
            }
            return action.call();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        } finally {
            if (previous == null) {
                System.clearProperty(key);
            } else {
                System.setProperty(key, previous);
            }
        }
    }

    private static boolean containsVirtualInvoke(org.objectweb.asm.tree.MethodNode methodNode) {
        if (methodNode == null || methodNode.instructions == null) {
            return false;
        }
        for (int i = 0; i < methodNode.instructions.size(); i++) {
            AbstractInsnNode insn = methodNode.instructions.get(i);
            if (!(insn instanceof MethodInsnNode methodInsn)) {
                continue;
            }
            int opcode = methodInsn.getOpcode();
            if (opcode == Opcodes.INVOKEVIRTUAL || opcode == Opcodes.INVOKEINTERFACE) {
                return true;
            }
        }
        return false;
    }

    private static boolean sourceFramesLoaded(BuildBytecodeWorkspace.ParsedMethod method) throws Exception {
        Field loaded = BuildBytecodeWorkspace.ParsedMethod.class.getDeclaredField("sourceFramesLoaded");
        loaded.setAccessible(true);
        return loaded.getBoolean(method);
    }

    private static ClassFileEntity invalidClass(String className, int jarId) {
        ClassFileEntity row = new ClassFileEntity();
        row.setClassName(className);
        row.setJarId(jarId);
        row.setPath(Path.of(className.replace('/', '_') + ".class"));
        row.setCachedBytes(new byte[]{1, 2, 3, 4});
        return row;
    }

    private record DiscoverySnapshot(Map<ClassReference.Handle, ClassReference> classMap,
                                     Map<MethodReference.Handle, MethodReference> methodMap,
                                     Set<String> classHandles,
                                     Set<String> methodHandles,
                                     Map<String, List<String>> stringAnnoMap) {
    }

    private record ClassAnalysisSnapshot(Map<String, List<String>> strings,
                                         Set<String> controllers,
                                         Set<String> interceptors,
                                         Set<String> servlets,
                                         Set<String> filters,
                                         Set<String> listeners) {
    }

    private record SymbolSnapshot(Set<String> callSites,
                                  Set<String> localVars) {
    }
}
