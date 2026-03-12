package me.n1ar4.jar.analyzer.core.bytecode;

import me.n1ar4.jar.analyzer.core.BytecodeSymbolRunner;
import me.n1ar4.jar.analyzer.core.ClassAnalysisRunner;
import me.n1ar4.jar.analyzer.core.DiscoveryRunner;
import me.n1ar4.jar.analyzer.core.InheritanceMap;
import me.n1ar4.jar.analyzer.analyze.spring.SpringController;
import me.n1ar4.jar.analyzer.analyze.spring.SpringMapping;
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
import java.util.stream.Collectors;
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
    void workspaceRunnersShouldRemainDeterministicAcrossRepeatedRuns() {
        Path jar = FixtureJars.springbootTestJar();
        List<ClassFileEntity> classFiles = CoreUtil.getAllClassesFromJars(
                List.of(jar.toString()),
                new HashMap<>(Map.of(jar.toString(), 1)),
                new ArrayList<>(),
                false
        );
        BuildBytecodeWorkspace workspace = BuildBytecodeWorkspace.parse(new HashSet<>(classFiles));

        DiscoverySnapshot firstDiscovery = runDiscovery(workspace);
        DiscoverySnapshot secondDiscovery = runDiscovery(workspace);
        assertEquals(firstDiscovery.classHandles, secondDiscovery.classHandles);
        assertEquals(firstDiscovery.methodHandles, secondDiscovery.methodHandles);
        assertEquals(firstDiscovery.stringAnnoMap, secondDiscovery.stringAnnoMap);

        ClassAnalysisSnapshot firstAnalysis =
                runClassAnalysis(workspace, firstDiscovery.classMap, firstDiscovery.methodMap);
        ClassAnalysisSnapshot secondAnalysis =
                runClassAnalysis(workspace, firstDiscovery.classMap, firstDiscovery.methodMap);
        assertEquals(firstAnalysis.strings, secondAnalysis.strings);
        assertEquals(firstAnalysis.controllers, secondAnalysis.controllers);
        assertEquals(firstAnalysis.interceptors, secondAnalysis.interceptors);
        assertEquals(firstAnalysis.servlets, secondAnalysis.servlets);
        assertEquals(firstAnalysis.filters, secondAnalysis.filters);
        assertEquals(firstAnalysis.listeners, secondAnalysis.listeners);

        SymbolSnapshot firstSymbol = runSymbol(workspace);
        SymbolSnapshot secondSymbol = runSymbol(workspace);
        assertEquals(firstSymbol.callSites, secondSymbol.callSites);
        assertEquals(firstSymbol.localVars, secondSymbol.localVars);
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
                normalizeControllers(controllers),
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

    private static TreeSet<String> normalizeControllers(List<SpringController> controllers) {
        TreeSet<String> out = new TreeSet<>();
        if (controllers == null) {
            return out;
        }
        for (SpringController controller : controllers) {
            if (controller == null) {
                continue;
            }
            String mappings = controller.getMappings() == null
                    ? ""
                    : controller.getMappings().stream()
                    .map(BuildBytecodeWorkspaceTest::normalizeMapping)
                    .sorted()
                    .collect(Collectors.joining(","));
            String className = controller.getClassName() == null ? "" : controller.getClassName().toString();
            out.add(className + "|" + controller.isRest() + "|" + safe(controller.getBasePath()) + "|" + mappings);
        }
        return out;
    }

    private static String normalizeMapping(SpringMapping mapping) {
        if (mapping == null) {
            return "";
        }
        String method = mapping.getMethodName() == null ? "" : mapping.getMethodName().toString();
        return method + "|" + mapping.isRest() + "|" + safe(mapping.getPath()) + "|"
                + safe(mapping.getPathRestful());
    }

    private static String safe(String value) {
        return value == null ? "" : value;
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
