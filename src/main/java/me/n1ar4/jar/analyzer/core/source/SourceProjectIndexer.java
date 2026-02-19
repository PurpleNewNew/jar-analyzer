/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.core.source;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.AnnotationMemberDeclaration;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.ArrayCreationExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.CharLiteralExpr;
import com.github.javaparser.ast.expr.DoubleLiteralExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.TextBlockLiteralExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.expr.TypeExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.VoidType;
import me.n1ar4.jar.analyzer.core.MethodCallKey;
import me.n1ar4.jar.analyzer.core.MethodCallMeta;
import me.n1ar4.jar.analyzer.core.reference.ClassReference;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.entity.ClassFileEntity;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Lightweight source-first indexing path used when no bytecode is available.
 */
public final class SourceProjectIndexer {
    private static final Logger logger = LogManager.getLogger();

    private SourceProjectIndexer() {
    }

    public static Result index(Path projectRoot, List<Path> sourceRoots, List<Path> sourceFiles) {
        List<Path> files = normalizeSourceFiles(sourceFiles);
        if (files.isEmpty()) {
            return Result.empty();
        }
        String jarName = resolveProjectName(projectRoot);
        IndexState state = new IndexState(jarName, -1, normalizeSourceRoots(sourceRoots));
        JavaParser parser = new JavaParser(new ParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.BLEEDING_EDGE));

        for (Path file : files) {
            try {
                ParseResult<CompilationUnit> parse = parser.parse(file);
                CompilationUnit cu = parse.getResult().orElse(null);
                if (cu == null) {
                    continue;
                }
                UnitContext unit = buildUnitContext(file, cu);
                for (TypeDeclaration<?> type : cu.getTypes()) {
                    scanType(type, unit, null, state);
                }
            } catch (Exception ex) {
                logger.debug("source parse failed: {}: {}", file, ex.toString());
            }
        }

        for (MethodTask task : state.methodTasks) {
            analyzeMethod(task, state);
        }

        state.rebuildIndexMaps();
        return state.toResult();
    }

    private static List<Path> normalizeSourceFiles(List<Path> sourceFiles) {
        if (sourceFiles == null || sourceFiles.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<Path> out = new LinkedHashSet<>();
        for (Path path : sourceFiles) {
            Path p = normalizePath(path);
            if (p == null || Files.isDirectory(p) || Files.notExists(p)) {
                continue;
            }
            String name = p.getFileName() == null ? "" : p.getFileName().toString().toLowerCase(Locale.ROOT);
            if (!name.endsWith(".java")) {
                continue;
            }
            out.add(p);
        }
        if (out.isEmpty()) {
            return List.of();
        }
        List<Path> sorted = new ArrayList<>(out);
        sorted.sort(Comparator.comparing(Path::toString));
        return List.copyOf(sorted);
    }

    private static List<Path> normalizeSourceRoots(List<Path> sourceRoots) {
        if (sourceRoots == null || sourceRoots.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<Path> out = new LinkedHashSet<>();
        for (Path root : sourceRoots) {
            Path p = normalizePath(root);
            if (p == null || Files.notExists(p) || !Files.isDirectory(p)) {
                continue;
            }
            out.add(p);
        }
        if (out.isEmpty()) {
            return List.of();
        }
        List<Path> sorted = new ArrayList<>(out);
        sorted.sort(Comparator.comparing(Path::toString));
        return List.copyOf(sorted);
    }

    private static UnitContext buildUnitContext(Path sourcePath, CompilationUnit cu) {
        String pkg = "";
        if (cu.getPackageDeclaration().isPresent()) {
            pkg = safe(cu.getPackageDeclaration().get().getNameAsString());
        }
        Map<String, String> imports = new HashMap<>();
        List<String> wildcardImports = new ArrayList<>();
        cu.getImports().forEach(imp -> {
            if (imp == null || imp.isStatic()) {
                return;
            }
            String name = safe(imp.getNameAsString());
            if (name.isEmpty()) {
                return;
            }
            if (imp.isAsterisk()) {
                wildcardImports.add(name);
                return;
            }
            int idx = name.lastIndexOf('.');
            if (idx <= 0 || idx >= name.length() - 1) {
                return;
            }
            String simple = name.substring(idx + 1);
            imports.putIfAbsent(simple, name);
        });
        return new UnitContext(sourcePath, pkg, imports, wildcardImports);
    }

    private static void scanType(TypeDeclaration<?> decl,
                                 UnitContext unit,
                                 String outerBinaryName,
                                 IndexState state) {
        if (decl == null || unit == null || state == null) {
            return;
        }
        String simple = safe(decl.getNameAsString());
        if (simple.isEmpty()) {
            return;
        }
        String binaryName = outerBinaryName == null || outerBinaryName.isEmpty()
                ? (unit.packageName.isEmpty() ? simple : unit.packageName + "." + simple)
                : outerBinaryName + "$" + simple;
        String internalName = toInternalName(binaryName);

        TypeIndexEntry typeEntry = new TypeIndexEntry(internalName, simple, unit.packageName);
        typeEntry.superClass = resolveSuperClass(decl, unit, state, typeEntry);
        typeEntry.isInterface = isInterfaceType(decl);

        List<String> interfaces = resolveInterfaces(decl, unit, state, typeEntry);
        int classAccess = modifierMask(decl);

        ClassReference classReference = new ClassReference(
                -1,
                classAccess,
                internalName,
                typeEntry.superClass,
                interfaces,
                typeEntry.isInterface,
                List.of(),
                Set.of(),
                state.jarName,
                state.jarId
        );
        state.classes.add(classReference);
        state.classMap.put(classReference.getHandle(), classReference);
        state.typeByInternal.put(internalName, typeEntry);
        state.typeBySimple.computeIfAbsent(simple, k -> new ArrayList<>()).add(typeEntry);

        ClassFileEntity classFile = new ClassFileEntity();
        classFile.setClassName(internalName + ".class");
        classFile.setPath(unit.sourcePath);
        classFile.setJarName(state.jarName);
        classFile.setJarId(state.jarId);
        state.classFiles.add(classFile);

        for (BodyDeclaration<?> member : decl.getMembers()) {
            if (member instanceof FieldDeclaration field) {
                recordFieldHints(field, unit, state, typeEntry);
                continue;
            }
            if (member instanceof MethodDeclaration methodDecl) {
                MethodReference methodReference = buildMethodReference(
                        typeEntry,
                        methodDecl,
                        methodDecl.getNameAsString(),
                        buildMethodDescriptor(methodDecl, unit, state, typeEntry),
                        unit,
                        state
                );
                state.methods.add(methodReference);
                state.methodMap.put(methodReference.getHandle(), methodReference);
                typeEntry.methodsByName
                        .computeIfAbsent(methodReference.getName(), k -> new ArrayList<>())
                        .add(new MethodSig(methodReference.getDesc(), methodDecl.getParameters().size(), methodReference.isStatic()));
                if (methodDecl.getBody().isPresent()) {
                    state.methodTasks.add(new MethodTask(typeEntry, methodReference, methodDecl, unit));
                }
                continue;
            }
            if (member instanceof ConstructorDeclaration ctorDecl) {
                MethodReference methodReference = buildMethodReference(
                        typeEntry,
                        ctorDecl,
                        "<init>",
                        buildConstructorDescriptor(ctorDecl, unit, state, typeEntry),
                        unit,
                        state
                );
                state.methods.add(methodReference);
                state.methodMap.put(methodReference.getHandle(), methodReference);
                typeEntry.methodsByName
                        .computeIfAbsent("<init>", k -> new ArrayList<>())
                        .add(new MethodSig(methodReference.getDesc(), ctorDecl.getParameters().size(), false));
                state.methodTasks.add(new MethodTask(typeEntry, methodReference, ctorDecl, unit));
                continue;
            }
            if (member instanceof AnnotationMemberDeclaration annotationMember) {
                String desc = "()" + descriptorOf(annotationMember.getType(), unit, state, typeEntry);
                MethodReference methodReference = new MethodReference(
                        new ClassReference.Handle(typeEntry.internalName, state.jarId),
                        annotationMember.getNameAsString(),
                        desc,
                        false,
                        Set.of(),
                        modifierMask(annotationMember),
                        lineOf(annotationMember),
                        state.jarName,
                        state.jarId
                );
                state.methods.add(methodReference);
                state.methodMap.put(methodReference.getHandle(), methodReference);
                typeEntry.methodsByName
                        .computeIfAbsent(methodReference.getName(), k -> new ArrayList<>())
                        .add(new MethodSig(desc, 0, false));
                continue;
            }
            if (member instanceof TypeDeclaration<?> nestedType) {
                scanType(nestedType, unit, binaryName, state);
            }
        }
    }

    private static void recordFieldHints(FieldDeclaration field,
                                         UnitContext unit,
                                         IndexState state,
                                         TypeIndexEntry owner) {
        if (field == null || owner == null) {
            return;
        }
        String desc = descriptorOf(field.getElementType(), unit, state, owner);
        String fieldType = internalFromDescriptor(desc);
        if (fieldType == null || fieldType.isBlank()) {
            return;
        }
        for (VariableDeclarator var : field.getVariables()) {
            if (var == null) {
                continue;
            }
            String name = safe(var.getNameAsString());
            if (name.isBlank()) {
                continue;
            }
            owner.fieldTypeHints.put(name, fieldType);
        }
    }

    private static MethodReference buildMethodReference(TypeIndexEntry owner,
                                                        Node node,
                                                        String methodName,
                                                        String methodDesc,
                                                        UnitContext unit,
                                                        IndexState state) {
        boolean isStatic = false;
        int access = 0;
        int line = lineOf(node);
        if (node instanceof MethodDeclaration methodDecl) {
            isStatic = methodDecl.isStatic();
            access = modifierMask(methodDecl);
        } else if (node instanceof ConstructorDeclaration ctorDecl) {
            isStatic = false;
            access = modifierMask(ctorDecl);
        } else if (node instanceof AnnotationMemberDeclaration annoDecl) {
            access = modifierMask(annoDecl);
        }
        return new MethodReference(
                new ClassReference.Handle(owner.internalName, state.jarId),
                safe(methodName),
                safe(methodDesc),
                isStatic,
                Set.of(),
                access,
                line,
                state.jarName,
                state.jarId
        );
    }

    private static void analyzeMethod(MethodTask task, IndexState state) {
        if (task == null || task.callable == null || task.methodReference == null) {
            return;
        }
        MethodReference.Handle caller = task.methodReference.getHandle();
        Map<String, String> typeHints = collectTypeHints(task, state);

        List<String> literals = collectStringLiterals(task.callable);
        if (!literals.isEmpty()) {
            state.strMap.put(caller, literals);
        }

        for (MethodCallExpr call : task.callable.findAll(MethodCallExpr.class)) {
            MethodReference.Handle callee = resolveMethodCall(task, call, typeHints, state);
            if (callee == null) {
                continue;
            }
            addEdge(caller, callee, state);
        }
        for (ObjectCreationExpr creation : task.callable.findAll(ObjectCreationExpr.class)) {
            MethodReference.Handle callee = resolveConstructorCall(task, creation, typeHints, state);
            if (callee == null) {
                continue;
            }
            addEdge(caller, callee, state);
        }
    }

    private static void addEdge(MethodReference.Handle caller,
                                MethodReference.Handle callee,
                                IndexState state) {
        if (caller == null || callee == null || state == null) {
            return;
        }
        state.methodCalls.computeIfAbsent(caller, k -> new HashSet<>()).add(callee);
        MethodCallKey key = MethodCallKey.of(caller, callee);
        MethodCallMeta.record(state.methodCallMeta,
                key,
                MethodCallMeta.TYPE_DIRECT,
                MethodCallMeta.CONF_MEDIUM,
                "source-index");
    }

    private static MethodReference.Handle resolveMethodCall(MethodTask task,
                                                            MethodCallExpr call,
                                                            Map<String, String> typeHints,
                                                            IndexState state) {
        if (task == null || call == null || state == null) {
            return null;
        }
        String owner = resolveCallOwner(task, call.getScope().orElse(null), typeHints, state);
        if (owner == null || owner.isBlank()) {
            owner = task.ownerType.internalName;
        }
        String methodName = safe(call.getNameAsString());
        if (methodName.isBlank()) {
            return null;
        }
        int argCount = call.getArguments() == null ? 0 : call.getArguments().size();
        String methodDesc = resolveMethodDesc(owner,
                methodName,
                argCount,
                call.getArguments(),
                task,
                typeHints,
                state,
                false);
        return new MethodReference.Handle(new ClassReference.Handle(owner, state.jarId), methodName, methodDesc);
    }

    private static MethodReference.Handle resolveConstructorCall(MethodTask task,
                                                                 ObjectCreationExpr creation,
                                                                 Map<String, String> typeHints,
                                                                 IndexState state) {
        if (task == null || creation == null || state == null) {
            return null;
        }
        String owner = resolveClassName(creation.getType().toString(), task.unit, state, task.ownerType);
        if (owner == null || owner.isBlank()) {
            return null;
        }
        int argCount = creation.getArguments() == null ? 0 : creation.getArguments().size();
        String desc = resolveMethodDesc(owner,
                "<init>",
                argCount,
                creation.getArguments(),
                task,
                typeHints,
                state,
                true);
        return new MethodReference.Handle(new ClassReference.Handle(owner, state.jarId), "<init>", desc);
    }

    private static String resolveCallOwner(MethodTask task,
                                           Expression scope,
                                           Map<String, String> typeHints,
                                           IndexState state) {
        if (task == null) {
            return null;
        }
        if (scope == null) {
            return task.ownerType.internalName;
        }
        if (scope instanceof ThisExpr) {
            return task.ownerType.internalName;
        }
        if (scope instanceof com.github.javaparser.ast.expr.SuperExpr) {
            return task.ownerType.superClass;
        }
        if (scope instanceof TypeExpr typeExpr) {
            return internalFromDescriptor(descriptorOf(typeExpr.getType(), task.unit, state, task.ownerType));
        }
        if (scope instanceof ObjectCreationExpr creationExpr) {
            return resolveClassName(creationExpr.getType().toString(), task.unit, state, task.ownerType);
        }
        if (scope instanceof CastExpr castExpr) {
            return internalFromDescriptor(descriptorOf(castExpr.getType(), task.unit, state, task.ownerType));
        }
        if (scope instanceof EnclosedExpr enclosedExpr) {
            return resolveCallOwner(task, enclosedExpr.getInner(), typeHints, state);
        }
        if (scope instanceof NameExpr nameExpr) {
            String name = safe(nameExpr.getNameAsString());
            if (name.isBlank()) {
                return null;
            }
            String hinted = typeHints.get(name);
            if (hinted != null && !hinted.isBlank()) {
                return hinted;
            }
            if (looksLikeTypeName(name)) {
                return resolveClassName(name, task.unit, state, task.ownerType);
            }
            return null;
        }
        if (scope instanceof FieldAccessExpr fieldAccessExpr) {
            String dotted = flattenFieldAccess(fieldAccessExpr);
            if (dotted != null && !dotted.isBlank() && looksLikePotentialTypePath(dotted, typeHints)) {
                String resolved = resolveClassName(dotted, task.unit, state, task.ownerType);
                if (resolved != null) {
                    return resolved;
                }
            }
            return resolveCallOwner(task, fieldAccessExpr.getScope(), typeHints, state);
        }
        return null;
    }

    private static String flattenFieldAccess(Expression expr) {
        if (expr == null) {
            return null;
        }
        if (expr instanceof NameExpr nameExpr) {
            return nameExpr.getNameAsString();
        }
        if (expr instanceof FieldAccessExpr fieldAccessExpr) {
            String left = flattenFieldAccess(fieldAccessExpr.getScope());
            if (left == null || left.isBlank()) {
                return fieldAccessExpr.getNameAsString();
            }
            return left + "." + fieldAccessExpr.getNameAsString();
        }
        if (expr instanceof TypeExpr typeExpr) {
            return typeExpr.getType().toString();
        }
        return expr.toString();
    }

    private static boolean looksLikePotentialTypePath(String dotted, Map<String, String> hints) {
        if (dotted == null || dotted.isBlank()) {
            return false;
        }
        String[] parts = dotted.split("\\.");
        if (parts.length == 0) {
            return false;
        }
        String first = parts[0];
        if (hints != null && hints.containsKey(first)) {
            return false;
        }
        for (String part : parts) {
            if (looksLikeTypeName(part)) {
                return true;
            }
        }
        return Character.isLowerCase(first.charAt(0));
    }

    private static Map<String, String> collectTypeHints(MethodTask task, IndexState state) {
        Map<String, String> hints = new HashMap<>();
        if (task == null || task.callable == null) {
            return hints;
        }
        hints.putAll(task.ownerType.fieldTypeHints);
        hints.put("this", task.ownerType.internalName);
        if (task.ownerType.superClass != null && !task.ownerType.superClass.isBlank()) {
            hints.put("super", task.ownerType.superClass);
        }

        for (Parameter parameter : task.callable.getParameters()) {
            String name = safe(parameter.getNameAsString());
            String type = internalFromDescriptor(descriptorOf(parameter.getType(), task.unit, state, task.ownerType));
            if (!name.isBlank() && type != null && !type.isBlank()) {
                hints.put(name, type);
            }
        }

        for (VariableDeclarator var : task.callable.findAll(VariableDeclarator.class)) {
            String name = safe(var.getNameAsString());
            if (name.isBlank()) {
                continue;
            }
            String type = internalFromDescriptor(descriptorOf(var.getType(), task.unit, state, task.ownerType));
            if (type != null && !type.isBlank()) {
                hints.put(name, type);
            }
            Optional<Expression> init = var.getInitializer();
            if (init.isPresent()) {
                String inferred = inferExpressionOwner(init.get(), hints, task, state);
                if (inferred != null && !inferred.isBlank()) {
                    hints.put(name, inferred);
                }
            }
        }

        for (AssignExpr assign : task.callable.findAll(AssignExpr.class)) {
            if (!(assign.getTarget() instanceof NameExpr target)) {
                continue;
            }
            String name = safe(target.getNameAsString());
            if (name.isBlank()) {
                continue;
            }
            String inferred = inferExpressionOwner(assign.getValue(), hints, task, state);
            if (inferred != null && !inferred.isBlank()) {
                hints.put(name, inferred);
            }
        }
        return hints;
    }

    private static String inferExpressionOwner(Expression expr,
                                               Map<String, String> hints,
                                               MethodTask task,
                                               IndexState state) {
        if (expr == null || task == null) {
            return null;
        }
        if (expr instanceof ObjectCreationExpr creationExpr) {
            return resolveClassName(creationExpr.getType().toString(), task.unit, state, task.ownerType);
        }
        if (expr instanceof CastExpr castExpr) {
            return internalFromDescriptor(descriptorOf(castExpr.getType(), task.unit, state, task.ownerType));
        }
        if (expr instanceof NameExpr nameExpr) {
            return hints == null ? null : hints.get(nameExpr.getNameAsString());
        }
        if (expr instanceof ThisExpr) {
            return task.ownerType.internalName;
        }
        if (expr instanceof FieldAccessExpr fieldAccessExpr) {
            String dotted = flattenFieldAccess(fieldAccessExpr);
            if (dotted != null && !dotted.isBlank()) {
                return resolveClassName(dotted, task.unit, state, task.ownerType);
            }
        }
        if (expr instanceof EnclosedExpr enclosedExpr) {
            return inferExpressionOwner(enclosedExpr.getInner(), hints, task, state);
        }
        if (expr instanceof MethodCallExpr callExpr) {
            MethodReference.Handle handle = resolveMethodCall(task, callExpr, hints, state);
            if (handle == null) {
                return null;
            }
            return internalFromDescriptor(returnDescriptor(handle.getDesc()));
        }
        return null;
    }

    private static List<String> collectStringLiterals(CallableDeclaration<?> callable) {
        if (callable == null) {
            return List.of();
        }
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (StringLiteralExpr expr : callable.findAll(StringLiteralExpr.class)) {
            String value = safe(expr.getValue());
            if (!value.isBlank()) {
                out.add(value);
            }
        }
        for (TextBlockLiteralExpr expr : callable.findAll(TextBlockLiteralExpr.class)) {
            String value = safe(expr.getValue());
            if (!value.isBlank()) {
                out.add(value);
            }
        }
        return out.isEmpty() ? List.of() : List.copyOf(out);
    }

    private static String resolveMethodDesc(String owner,
                                            String methodName,
                                            int argCount,
                                            List<Expression> args,
                                            MethodTask task,
                                            Map<String, String> typeHints,
                                            IndexState state,
                                            boolean constructor) {
        String resolved = resolveMethodDescFromIndex(owner, methodName, argCount, state, constructor);
        if (resolved != null) {
            return resolved;
        }
        StringBuilder desc = new StringBuilder();
        desc.append('(');
        if (args != null) {
            for (Expression arg : args) {
                desc.append(inferArgDescriptor(arg, typeHints, task, state));
            }
        } else {
            for (int i = 0; i < argCount; i++) {
                desc.append("Ljava/lang/Object;");
            }
        }
        desc.append(')');
        if (constructor) {
            desc.append('V');
        } else {
            desc.append("Ljava/lang/Object;");
        }
        return desc.toString();
    }

    private static String resolveMethodDescFromIndex(String owner,
                                                     String methodName,
                                                     int argCount,
                                                     IndexState state,
                                                     boolean constructor) {
        if (state == null || owner == null || methodName == null) {
            return null;
        }
        Set<String> visited = new HashSet<>();
        return resolveMethodDescFromIndexRecursive(owner, methodName, argCount, state, constructor, visited);
    }

    private static String resolveMethodDescFromIndexRecursive(String owner,
                                                              String methodName,
                                                              int argCount,
                                                              IndexState state,
                                                              boolean constructor,
                                                              Set<String> visited) {
        if (owner == null || owner.isBlank() || visited.contains(owner)) {
            return null;
        }
        visited.add(owner);
        TypeIndexEntry type = state.typeByInternal.get(owner);
        if (type == null) {
            return null;
        }
        List<MethodSig> sigs = type.methodsByName.get(methodName);
        if (sigs != null && !sigs.isEmpty()) {
            for (MethodSig sig : sigs) {
                if (sig.argCount == argCount) {
                    return sig.desc;
                }
            }
            if (constructor) {
                for (MethodSig sig : sigs) {
                    return sig.desc;
                }
            }
        }
        if (!constructor && type.superClass != null && !type.superClass.isBlank()) {
            return resolveMethodDescFromIndexRecursive(type.superClass, methodName, argCount, state, false, visited);
        }
        return null;
    }

    private static String inferArgDescriptor(Expression expr,
                                             Map<String, String> hints,
                                             MethodTask task,
                                             IndexState state) {
        if (expr == null) {
            return "Ljava/lang/Object;";
        }
        if (expr instanceof StringLiteralExpr || expr instanceof TextBlockLiteralExpr) {
            return "Ljava/lang/String;";
        }
        if (expr instanceof BooleanLiteralExpr) {
            return "Z";
        }
        if (expr instanceof IntegerLiteralExpr) {
            return "I";
        }
        if (expr instanceof LongLiteralExpr) {
            return "J";
        }
        if (expr instanceof DoubleLiteralExpr) {
            return "D";
        }
        if (expr instanceof CharLiteralExpr) {
            return "C";
        }
        if (expr instanceof NullLiteralExpr) {
            return "Ljava/lang/Object;";
        }
        if (expr instanceof UnaryExpr unaryExpr) {
            return inferArgDescriptor(unaryExpr.getExpression(), hints, task, state);
        }
        if (expr instanceof EnclosedExpr enclosedExpr) {
            return inferArgDescriptor(enclosedExpr.getInner(), hints, task, state);
        }
        if (expr instanceof CastExpr castExpr) {
            return descriptorOf(castExpr.getType(), task.unit, state, task.ownerType);
        }
        if (expr instanceof NameExpr nameExpr) {
            String hinted = hints == null ? null : hints.get(nameExpr.getNameAsString());
            if (hinted != null && !hinted.isBlank()) {
                return "L" + hinted + ";";
            }
            return "Ljava/lang/Object;";
        }
        if (expr instanceof ObjectCreationExpr creationExpr) {
            String owner = resolveClassName(creationExpr.getType().toString(), task.unit, state, task.ownerType);
            if (owner == null || owner.isBlank()) {
                return "Ljava/lang/Object;";
            }
            return "L" + owner + ";";
        }
        if (expr instanceof ArrayCreationExpr creationExpr) {
            String element = descriptorOf(creationExpr.getElementType(), task.unit, state, task.ownerType);
            int dims = creationExpr.getLevels() == null ? 1 : Math.max(1, creationExpr.getLevels().size());
            StringBuilder out = new StringBuilder();
            for (int i = 0; i < dims; i++) {
                out.append('[');
            }
            out.append(element);
            return out.toString();
        }
        if (expr instanceof ThisExpr) {
            return "L" + task.ownerType.internalName + ";";
        }
        if (expr instanceof FieldAccessExpr fieldAccessExpr) {
            String owner = inferExpressionOwner(fieldAccessExpr, hints, task, state);
            if (owner != null && !owner.isBlank()) {
                return "L" + owner + ";";
            }
            return "Ljava/lang/Object;";
        }
        if (expr instanceof MethodCallExpr callExpr) {
            MethodReference.Handle handle = resolveMethodCall(task, callExpr, hints, state);
            if (handle != null) {
                String ret = returnDescriptor(handle.getDesc());
                if (ret != null && !ret.isBlank()) {
                    return ret;
                }
            }
            return "Ljava/lang/Object;";
        }
        return "Ljava/lang/Object;";
    }

    private static String resolveSuperClass(TypeDeclaration<?> decl,
                                            UnitContext unit,
                                            IndexState state,
                                            TypeIndexEntry owner) {
        if (decl instanceof RecordDeclaration) {
            return "java/lang/Record";
        }
        if (decl instanceof EnumDeclaration) {
            return "java/lang/Enum";
        }
        if (decl instanceof AnnotationDeclaration) {
            return "java/lang/Object";
        }
        if (decl instanceof ClassOrInterfaceDeclaration classDecl) {
            if (classDecl.isInterface()) {
                return "java/lang/Object";
            }
            if (!classDecl.getExtendedTypes().isEmpty()) {
                return resolveClassName(classDecl.getExtendedTypes().get(0).toString(), unit, state, owner);
            }
            return "java/lang/Object";
        }
        return "java/lang/Object";
    }

    private static List<String> resolveInterfaces(TypeDeclaration<?> decl,
                                                  UnitContext unit,
                                                  IndexState state,
                                                  TypeIndexEntry owner) {
        List<String> out = new ArrayList<>();
        if (decl instanceof ClassOrInterfaceDeclaration classDecl) {
            List<ClassOrInterfaceType> list = classDecl.isInterface()
                    ? classDecl.getExtendedTypes()
                    : classDecl.getImplementedTypes();
            for (ClassOrInterfaceType type : list) {
                String name = resolveClassName(type.toString(), unit, state, owner);
                if (name != null && !name.isBlank()) {
                    out.add(name);
                }
            }
            return out;
        }
        if (decl instanceof EnumDeclaration enumDecl) {
            for (ClassOrInterfaceType type : enumDecl.getImplementedTypes()) {
                String name = resolveClassName(type.toString(), unit, state, owner);
                if (name != null && !name.isBlank()) {
                    out.add(name);
                }
            }
            return out;
        }
        if (decl instanceof RecordDeclaration recordDecl) {
            for (ClassOrInterfaceType type : recordDecl.getImplementedTypes()) {
                String name = resolveClassName(type.toString(), unit, state, owner);
                if (name != null && !name.isBlank()) {
                    out.add(name);
                }
            }
            return out;
        }
        return out;
    }

    private static boolean isInterfaceType(TypeDeclaration<?> decl) {
        if (decl instanceof ClassOrInterfaceDeclaration classDecl) {
            return classDecl.isInterface();
        }
        return decl instanceof AnnotationDeclaration;
    }

    private static String buildMethodDescriptor(MethodDeclaration method,
                                                UnitContext unit,
                                                IndexState state,
                                                TypeIndexEntry owner) {
        StringBuilder sb = new StringBuilder();
        sb.append('(');
        for (Parameter parameter : method.getParameters()) {
            sb.append(descriptorOf(parameter.getType(), unit, state, owner));
        }
        sb.append(')');
        sb.append(descriptorOf(method.getType(), unit, state, owner));
        return sb.toString();
    }

    private static String buildConstructorDescriptor(ConstructorDeclaration ctor,
                                                     UnitContext unit,
                                                     IndexState state,
                                                     TypeIndexEntry owner) {
        StringBuilder sb = new StringBuilder();
        sb.append('(');
        for (Parameter parameter : ctor.getParameters()) {
            sb.append(descriptorOf(parameter.getType(), unit, state, owner));
        }
        sb.append(')');
        sb.append('V');
        return sb.toString();
    }

    private static String descriptorOf(Type type,
                                       UnitContext unit,
                                       IndexState state,
                                       TypeIndexEntry owner) {
        if (type == null) {
            return "Ljava/lang/Object;";
        }
        if (type instanceof PrimitiveType primitiveType) {
            return switch (primitiveType.getType()) {
                case BOOLEAN -> "Z";
                case BYTE -> "B";
                case CHAR -> "C";
                case SHORT -> "S";
                case INT -> "I";
                case LONG -> "J";
                case FLOAT -> "F";
                case DOUBLE -> "D";
            };
        }
        if (type instanceof VoidType) {
            return "V";
        }
        if (type instanceof ArrayType arrayType) {
            return "[" + descriptorOf(arrayType.getComponentType(), unit, state, owner);
        }
        if (type instanceof ClassOrInterfaceType classOrInterfaceType) {
            String internal = resolveClassName(classOrInterfaceType.toString(), unit, state, owner);
            if (internal == null || internal.isBlank()) {
                return "Ljava/lang/Object;";
            }
            return "L" + internal + ";";
        }
        return "Ljava/lang/Object;";
    }

    private static String resolveClassName(String raw,
                                           UnitContext unit,
                                           IndexState state,
                                           TypeIndexEntry owner) {
        String text = sanitizeTypeName(raw);
        if (text.isBlank()) {
            return "java/lang/Object";
        }
        if (text.contains("/")) {
            return text;
        }
        if (isPrimitiveKeyword(text)) {
            return "java/lang/Object";
        }
        if ("void".equals(text)) {
            return "java/lang/Object";
        }

        if (text.contains(".")) {
            String first = firstSegment(text);
            if (first != null && !first.isBlank()) {
                String explicit = unit == null ? null : unit.explicitImports.get(first);
                if (explicit != null && !explicit.isBlank() && text.length() > first.length() + 1) {
                    String tail = text.substring(first.length() + 1);
                    return dottedToInternal(explicit + "." + tail);
                }
            }
            if (first != null && !first.isBlank() && Character.isLowerCase(first.charAt(0))) {
                return dottedToInternal(text);
            }
            if (unit != null && !unit.packageName.isBlank()) {
                return dottedToInternal(unit.packageName + "." + text);
            }
            return dottedToInternal(text);
        }

        if (owner != null && !owner.packageName.isBlank()) {
            String samePkg = dottedToInternal(owner.packageName + "." + text);
            if (state.typeByInternal.containsKey(samePkg)) {
                return samePkg;
            }
        }

        if (unit != null) {
            String imported = unit.explicitImports.get(text);
            if (imported != null && !imported.isBlank()) {
                return dottedToInternal(imported);
            }
        }

        List<TypeIndexEntry> sameSimple = state.typeBySimple.get(text);
        if (sameSimple != null && !sameSimple.isEmpty()) {
            if (owner != null && owner.packageName != null) {
                for (TypeIndexEntry candidate : sameSimple) {
                    if (Objects.equals(owner.packageName, candidate.packageName)) {
                        return candidate.internalName;
                    }
                }
            }
            return sameSimple.get(0).internalName;
        }

        if (unit != null && unit.wildcardImports != null) {
            for (String imp : unit.wildcardImports) {
                String candidate = dottedToInternal(imp + "." + text);
                if (state.typeByInternal.containsKey(candidate)) {
                    return candidate;
                }
            }
        }

        if (unit != null && !unit.packageName.isBlank()) {
            return dottedToInternal(unit.packageName + "." + text);
        }
        return dottedToInternal("java.lang." + text);
    }

    private static String dottedToInternal(String dotted) {
        String value = safe(dotted);
        if (value.isBlank()) {
            return "java/lang/Object";
        }
        value = sanitizeTypeName(value);
        String[] parts = value.split("\\.");
        if (parts.length == 0) {
            return "java/lang/Object";
        }
        int classStart = -1;
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (part == null || part.isBlank()) {
                continue;
            }
            char c = part.charAt(0);
            if (Character.isUpperCase(c) || c == '$') {
                classStart = i;
                break;
            }
        }
        if (classStart < 0) {
            classStart = Math.max(0, parts.length - 1);
        }
        StringBuilder pkg = new StringBuilder();
        for (int i = 0; i < classStart; i++) {
            if (parts[i] == null || parts[i].isBlank()) {
                continue;
            }
            if (pkg.length() > 0) {
                pkg.append('/');
            }
            pkg.append(parts[i]);
        }
        StringBuilder cls = new StringBuilder();
        for (int i = classStart; i < parts.length; i++) {
            if (parts[i] == null || parts[i].isBlank()) {
                continue;
            }
            if (cls.length() > 0) {
                cls.append('$');
            }
            cls.append(parts[i]);
        }
        if (cls.length() == 0) {
            return "java/lang/Object";
        }
        if (pkg.length() == 0) {
            return cls.toString();
        }
        return pkg + "/" + cls;
    }

    private static String sanitizeTypeName(String raw) {
        if (raw == null) {
            return "";
        }
        String value = raw.trim();
        if (value.isEmpty()) {
            return "";
        }
        int generic = value.indexOf('<');
        if (generic >= 0) {
            value = value.substring(0, generic);
        }
        value = value.replace("?", "").trim();
        while (value.endsWith("[]")) {
            value = value.substring(0, value.length() - 2).trim();
        }
        while (value.endsWith("...")) {
            value = value.substring(0, value.length() - 3).trim();
        }
        return value;
    }

    private static boolean isPrimitiveKeyword(String value) {
        return "boolean".equals(value)
                || "byte".equals(value)
                || "char".equals(value)
                || "short".equals(value)
                || "int".equals(value)
                || "long".equals(value)
                || "float".equals(value)
                || "double".equals(value);
    }

    private static String firstSegment(String value) {
        if (value == null) {
            return null;
        }
        int idx = value.indexOf('.');
        if (idx < 0) {
            return value;
        }
        if (idx == 0) {
            return "";
        }
        return value.substring(0, idx);
    }

    private static boolean looksLikeTypeName(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        char c = value.charAt(0);
        return Character.isUpperCase(c) || c == '$';
    }

    private static int lineOf(Node node) {
        if (node == null) {
            return -1;
        }
        return node.getBegin().map(pos -> pos.line).orElse(-1);
    }

    private static int modifierMask(Node node) {
        int out = 0;
        if (node instanceof com.github.javaparser.ast.nodeTypes.NodeWithModifiers<?> nodeWithModifiers) {
            for (com.github.javaparser.ast.Modifier modifier : nodeWithModifiers.getModifiers()) {
                if (modifier == null) {
                    continue;
                }
                switch (modifier.getKeyword()) {
                    case PUBLIC -> out |= 0x0001;
                    case PRIVATE -> out |= 0x0002;
                    case PROTECTED -> out |= 0x0004;
                    case STATIC -> out |= 0x0008;
                    case FINAL -> out |= 0x0010;
                    case ABSTRACT -> out |= 0x0400;
                    case SYNCHRONIZED -> out |= 0x0020;
                    case NATIVE -> out |= 0x0100;
                    case STRICTFP -> out |= 0x0800;
                    case TRANSIENT -> out |= 0x0080;
                    case VOLATILE -> out |= 0x0040;
                    default -> {
                    }
                }
            }
        }
        return out;
    }

    private static String returnDescriptor(String methodDesc) {
        if (methodDesc == null) {
            return null;
        }
        int idx = methodDesc.lastIndexOf(')');
        if (idx < 0 || idx >= methodDesc.length() - 1) {
            return null;
        }
        return methodDesc.substring(idx + 1);
    }

    private static String internalFromDescriptor(String desc) {
        if (desc == null || desc.isBlank()) {
            return null;
        }
        if (desc.startsWith("L") && desc.endsWith(";") && desc.length() > 2) {
            return desc.substring(1, desc.length() - 1);
        }
        if (desc.startsWith("[")) {
            String elem = desc;
            while (elem.startsWith("[")) {
                elem = elem.substring(1);
            }
            if (elem.startsWith("L") && elem.endsWith(";") && elem.length() > 2) {
                return elem.substring(1, elem.length() - 1);
            }
            return null;
        }
        return null;
    }

    private static String resolveProjectName(Path projectRoot) {
        if (projectRoot == null) {
            return "source-project";
        }
        Path fileName = projectRoot.getFileName();
        if (fileName == null) {
            return "source-project";
        }
        String name = safe(fileName.toString()).trim();
        return name.isEmpty() ? "source-project" : name;
    }

    private static String toInternalName(String dottedBinaryName) {
        if (dottedBinaryName == null || dottedBinaryName.isBlank()) {
            return "";
        }
        return dottedBinaryName.replace('.', '/');
    }

    private static Path normalizePath(Path path) {
        if (path == null) {
            return null;
        }
        try {
            return path.toAbsolutePath().normalize();
        } catch (Exception ex) {
            return path.normalize();
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    public static final class Result {
        private final Set<ClassFileEntity> classFiles;
        private final Set<ClassReference> classes;
        private final Set<MethodReference> methods;
        private final HashMap<MethodReference.Handle, HashSet<MethodReference.Handle>> methodCalls;
        private final Map<MethodCallKey, MethodCallMeta> methodCallMeta;
        private final Map<MethodReference.Handle, List<String>> strMap;
        private final Map<MethodReference.Handle, List<String>> stringAnnoMap;
        private final Map<ClassReference.Handle, ClassReference> classMap;
        private final Map<MethodReference.Handle, MethodReference> methodMap;

        private Result(Set<ClassFileEntity> classFiles,
                       Set<ClassReference> classes,
                       Set<MethodReference> methods,
                       HashMap<MethodReference.Handle, HashSet<MethodReference.Handle>> methodCalls,
                       Map<MethodCallKey, MethodCallMeta> methodCallMeta,
                       Map<MethodReference.Handle, List<String>> strMap,
                       Map<MethodReference.Handle, List<String>> stringAnnoMap,
                       Map<ClassReference.Handle, ClassReference> classMap,
                       Map<MethodReference.Handle, MethodReference> methodMap) {
            this.classFiles = classFiles;
            this.classes = classes;
            this.methods = methods;
            this.methodCalls = methodCalls;
            this.methodCallMeta = methodCallMeta;
            this.strMap = strMap;
            this.stringAnnoMap = stringAnnoMap;
            this.classMap = classMap;
            this.methodMap = methodMap;
        }

        public static Result empty() {
            return new Result(
                    Collections.emptySet(),
                    Collections.emptySet(),
                    Collections.emptySet(),
                    new HashMap<>(),
                    Collections.emptyMap(),
                    Collections.emptyMap(),
                    Collections.emptyMap(),
                    Collections.emptyMap(),
                    Collections.emptyMap()
            );
        }

        public Set<ClassFileEntity> classFiles() {
            return classFiles;
        }

        public Set<ClassReference> classes() {
            return classes;
        }

        public Set<MethodReference> methods() {
            return methods;
        }

        public HashMap<MethodReference.Handle, HashSet<MethodReference.Handle>> methodCalls() {
            return methodCalls;
        }

        public Map<MethodCallKey, MethodCallMeta> methodCallMeta() {
            return methodCallMeta;
        }

        public Map<MethodReference.Handle, List<String>> strMap() {
            return strMap;
        }

        public Map<MethodReference.Handle, List<String>> stringAnnoMap() {
            return stringAnnoMap;
        }

        public Map<ClassReference.Handle, ClassReference> classMap() {
            return classMap;
        }

        public Map<MethodReference.Handle, MethodReference> methodMap() {
            return methodMap;
        }
    }

    private static final class IndexState {
        private final String jarName;
        private final int jarId;
        @SuppressWarnings("unused")
        private final List<Path> sourceRoots;

        private final Set<ClassFileEntity> classFiles = new LinkedHashSet<>();
        private final Set<ClassReference> classes = new LinkedHashSet<>();
        private final Set<MethodReference> methods = new LinkedHashSet<>();
        private final HashMap<MethodReference.Handle, HashSet<MethodReference.Handle>> methodCalls = new HashMap<>();
        private final Map<MethodCallKey, MethodCallMeta> methodCallMeta = new LinkedHashMap<>();
        private final Map<MethodReference.Handle, List<String>> strMap = new LinkedHashMap<>();
        private final Map<MethodReference.Handle, List<String>> stringAnnoMap = new LinkedHashMap<>();

        private final Map<ClassReference.Handle, ClassReference> classMap = new LinkedHashMap<>();
        private final Map<MethodReference.Handle, MethodReference> methodMap = new LinkedHashMap<>();
        private final Map<String, TypeIndexEntry> typeByInternal = new LinkedHashMap<>();
        private final Map<String, List<TypeIndexEntry>> typeBySimple = new HashMap<>();
        private final List<MethodTask> methodTasks = new ArrayList<>();

        private IndexState(String jarName, int jarId, List<Path> sourceRoots) {
            this.jarName = safe(jarName);
            this.jarId = jarId;
            this.sourceRoots = sourceRoots == null ? List.of() : sourceRoots;
        }

        private void rebuildIndexMaps() {
            classMap.clear();
            for (ClassReference ref : classes) {
                classMap.put(ref.getHandle(), ref);
            }
            methodMap.clear();
            for (MethodReference ref : methods) {
                methodMap.put(ref.getHandle(), ref);
            }
        }

        private Result toResult() {
            return new Result(
                    classFiles.isEmpty() ? Collections.emptySet() : Collections.unmodifiableSet(classFiles),
                    classes.isEmpty() ? Collections.emptySet() : Collections.unmodifiableSet(classes),
                    methods.isEmpty() ? Collections.emptySet() : Collections.unmodifiableSet(methods),
                    methodCalls,
                    methodCallMeta.isEmpty() ? Collections.emptyMap() : Collections.unmodifiableMap(methodCallMeta),
                    strMap.isEmpty() ? Collections.emptyMap() : Collections.unmodifiableMap(strMap),
                    stringAnnoMap.isEmpty() ? Collections.emptyMap() : Collections.unmodifiableMap(stringAnnoMap),
                    classMap.isEmpty() ? Collections.emptyMap() : Collections.unmodifiableMap(classMap),
                    methodMap.isEmpty() ? Collections.emptyMap() : Collections.unmodifiableMap(methodMap)
            );
        }
    }

    private static final class UnitContext {
        private final Path sourcePath;
        private final String packageName;
        private final Map<String, String> explicitImports;
        private final List<String> wildcardImports;

        private UnitContext(Path sourcePath,
                            String packageName,
                            Map<String, String> explicitImports,
                            List<String> wildcardImports) {
            this.sourcePath = sourcePath;
            this.packageName = packageName == null ? "" : packageName;
            this.explicitImports = explicitImports == null ? Map.of() : explicitImports;
            this.wildcardImports = wildcardImports == null ? List.of() : wildcardImports;
        }
    }

    private static final class TypeIndexEntry {
        private final String internalName;
        private final String simpleName;
        private final String packageName;
        private String superClass = "java/lang/Object";
        private boolean isInterface;
        private final Map<String, List<MethodSig>> methodsByName = new HashMap<>();
        private final Map<String, String> fieldTypeHints = new HashMap<>();

        private TypeIndexEntry(String internalName, String simpleName, String packageName) {
            this.internalName = internalName;
            this.simpleName = simpleName;
            this.packageName = packageName == null ? "" : packageName;
        }
    }

    private static final class MethodSig {
        private final String desc;
        private final int argCount;
        @SuppressWarnings("unused")
        private final boolean isStatic;

        private MethodSig(String desc, int argCount, boolean isStatic) {
            this.desc = desc;
            this.argCount = argCount;
            this.isStatic = isStatic;
        }
    }

    private static final class MethodTask {
        private final TypeIndexEntry ownerType;
        private final MethodReference methodReference;
        private final CallableDeclaration<?> callable;
        private final UnitContext unit;

        private MethodTask(TypeIndexEntry ownerType,
                           MethodReference methodReference,
                           CallableDeclaration<?> callable,
                           UnitContext unit) {
            this.ownerType = ownerType;
            this.methodReference = methodReference;
            this.callable = callable;
            this.unit = unit;
        }
    }
}
