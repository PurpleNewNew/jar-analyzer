/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.semantic;

import com.github.javaparser.Position;
import com.github.javaparser.Range;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.TypeExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.TypeParameter;
import me.n1ar4.jar.analyzer.engine.CoreEngine;

import java.util.List;

public final class SemanticResolver {
    private final CoreEngine engine;
    private final TypeSolver typeSolver;
    private final CallResolver callResolver;

    public SemanticResolver(CoreEngine engine, TypeSolver typeSolver) {
        this.engine = engine;
        this.typeSolver = typeSolver;
        this.callResolver = typeSolver == null ? null : typeSolver.getCallResolver();
    }

    public SymbolRef resolveSemanticTarget(CompilationUnit cu, Position pos, String fallbackWord) {
        if (cu == null || pos == null) {
            return new SymbolRef(SymbolKind.UNKNOWN, fallbackWord, null);
        }
        SimpleName picked = selectNameAt(cu, pos);
        if (picked == null) {
            return new SymbolRef(SymbolKind.METHOD, fallbackWord, null);
        }
        SymbolRef target = classifyName(cu, picked);
        if (target == null || target.getKind() == SymbolKind.UNKNOWN) {
            return new SymbolRef(SymbolKind.METHOD, fallbackWord, null);
        }
        return target;
    }

    public SymbolRef classifyName(CompilationUnit cu, SimpleName name) {
        if (name == null) {
            return new SymbolRef(SymbolKind.UNKNOWN, null, null);
        }
        Node parent = name.getParentNode().orElse(null);
        PackageDeclaration pkgDecl = findAncestor(name, PackageDeclaration.class);
        if (pkgDecl != null) {
            return new SymbolRef(SymbolKind.PACKAGE, name.asString(), pkgDecl.getNameAsString());
        }
        ImportDeclaration impDecl = findAncestor(name, ImportDeclaration.class);
        if (impDecl != null && !impDecl.isStatic()) {
            if (impDecl.isAsterisk()) {
                return new SymbolRef(SymbolKind.PACKAGE, name.asString(), impDecl.getNameAsString());
            }
            String importName = impDecl.getNameAsString();
            if (importName != null) {
                String simple = name.asString();
                if (importName.equals(simple) || importName.endsWith("." + simple)) {
                    String cls = importName.replace('.', '/');
                    SymbolKind kind = typeSolver == null
                            ? SymbolKind.CLASS
                            : typeSolver.resolveClassKind(cls, SymbolKind.CLASS);
                    return new SymbolRef(kind, name.asString(), cls);
                }
            }
            return new SymbolRef(SymbolKind.PACKAGE, name.asString(), impDecl.getNameAsString());
        }
        if (parent instanceof MethodCallExpr) {
            MethodCallExpr call = (MethodCallExpr) parent;
            if (call.getName() == name) {
                String classHint = callResolver == null
                        ? null
                        : callResolver.resolveScopeClassName(cu, call, null);
                int argCount = call.getArguments() == null
                        ? TypeSolver.ARG_COUNT_UNKNOWN
                        : call.getArguments().size();
                return new SymbolRef(SymbolKind.METHOD, call.getNameAsString(),
                        classHint, null, null, argCount);
            }
        }
        if (parent instanceof MethodReferenceExpr) {
            MethodReferenceExpr ref = (MethodReferenceExpr) parent;
            String identifier = ref.getIdentifier();
            if (identifier != null && identifier.equals(name.asString())) {
                String classHint = null;
                Expression scope = ref.getScope();
                if (typeSolver != null) {
                    if (scope instanceof TypeExpr) {
                        TypeExpr te = (TypeExpr) scope;
                        classHint = typeSolver.resolveTypeToClassName(cu, te.getTypeAsString());
                    } else {
                        TypeRef resolved = typeSolver.resolveExpressionType(cu, scope, null);
                        classHint = resolved == null ? null : resolved.internalName;
                    }
                }
                if ("new".equals(identifier)) {
                    return new SymbolRef(SymbolKind.CONSTRUCTOR, identifier, classHint);
                }
                return new SymbolRef(SymbolKind.METHOD, identifier, classHint);
            }
        }
        if (parent instanceof MethodDeclaration) {
            MethodDeclaration decl = (MethodDeclaration) parent;
            int argCount = decl.getParameters() == null
                    ? TypeSolver.ARG_COUNT_UNKNOWN
                    : decl.getParameters().size();
            String owner = typeSolver == null ? null : typeSolver.resolveEnclosingClassName(cu, parent);
            return new SymbolRef(SymbolKind.METHOD, name.asString(), owner, null, null, argCount);
        }
        if (parent instanceof ConstructorDeclaration) {
            String cls = typeSolver == null ? null : typeSolver.resolveEnclosingClassName(cu, parent);
            ConstructorDeclaration decl = (ConstructorDeclaration) parent;
            int argCount = decl.getParameters() == null
                    ? TypeSolver.ARG_COUNT_UNKNOWN
                    : decl.getParameters().size();
            return new SymbolRef(SymbolKind.CONSTRUCTOR, name.asString(), cls, null, null, argCount);
        }
        if (parent instanceof ClassOrInterfaceDeclaration) {
            ClassOrInterfaceDeclaration decl = (ClassOrInterfaceDeclaration) parent;
            String cls = typeSolver == null ? null : typeSolver.resolveQualifiedClassName(cu, name.asString());
            SymbolKind kind = decl.isInterface() ? SymbolKind.INTERFACE : SymbolKind.CLASS;
            return new SymbolRef(kind, name.asString(), cls);
        }
        if (parent instanceof EnumDeclaration) {
            String cls = typeSolver == null ? null : typeSolver.resolveQualifiedClassName(cu, name.asString());
            return new SymbolRef(SymbolKind.ENUM, name.asString(), cls);
        }
        if (parent instanceof RecordDeclaration) {
            String cls = typeSolver == null ? null : typeSolver.resolveQualifiedClassName(cu, name.asString());
            return new SymbolRef(SymbolKind.RECORD, name.asString(), cls);
        }
        if (parent instanceof AnnotationDeclaration) {
            String cls = typeSolver == null ? null : typeSolver.resolveQualifiedClassName(cu, name.asString());
            return new SymbolRef(SymbolKind.ANNOTATION, name.asString(), cls);
        }
        if (parent instanceof TypeParameter) {
            Range range = name.getRange().orElse(null);
            return new SymbolRef(SymbolKind.TYPE_PARAM, name.asString(), null, range, null);
        }
        if (parent instanceof ClassOrInterfaceType) {
            String cls = typeSolver == null ? null : typeSolver.resolveQualifiedClassName(cu, name.asString());
            if (isConstructorContext(parent)) {
                int argCount = resolveConstructorArgCount(parent);
                return new SymbolRef(SymbolKind.CONSTRUCTOR, name.asString(), cls, null, null, argCount);
            }
            SymbolKind kind = typeSolver == null
                    ? SymbolKind.CLASS
                    : typeSolver.resolveClassKind(cls, SymbolKind.CLASS);
            return new SymbolRef(kind, name.asString(), cls);
        }
        SymbolRef decl = typeSolver == null ? null : typeSolver.resolveDeclaration(cu, name);
        if (decl != null) {
            return decl;
        }
        String staticOwner = typeSolver == null ? null : typeSolver.resolveStaticImportOwner(cu, name.asString());
        if (staticOwner != null) {
            if (parent instanceof MethodCallExpr) {
                MethodCallExpr call = (MethodCallExpr) parent;
                int argCount = call.getArguments() == null
                        ? TypeSolver.ARG_COUNT_UNKNOWN
                        : call.getArguments().size();
                return new SymbolRef(SymbolKind.METHOD, name.asString(), staticOwner, null, null, argCount);
            }
            return new SymbolRef(SymbolKind.FIELD, name.asString(), staticOwner);
        }
        return new SymbolRef(SymbolKind.UNKNOWN, name.asString(), null);
    }

    private static SimpleName selectNameAt(CompilationUnit cu, Position pos) {
        if (cu == null || pos == null) {
            return null;
        }
        List<SimpleName> names = cu.findAll(SimpleName.class);
        if (names == null || names.isEmpty()) {
            return null;
        }
        SimpleName best = null;
        int bestLen = Integer.MAX_VALUE;
        for (SimpleName name : names) {
            if (!name.getRange().isPresent()) {
                continue;
            }
            Range r = name.getRange().get();
            if (contains(r, pos)) {
                int len = rangeLength(r);
                if (len < bestLen) {
                    best = name;
                    bestLen = len;
                }
            }
        }
        return best;
    }

    private static int rangeLength(Range range) {
        if (range == null) {
            return Integer.MAX_VALUE;
        }
        if (range.begin.line == range.end.line) {
            return Math.max(0, range.end.column - range.begin.column);
        }
        return Math.max(0, (range.end.line - range.begin.line)) * 1000
                + Math.max(0, range.end.column - range.begin.column);
    }

    private static boolean contains(Range range, Position pos) {
        if (range == null || pos == null) {
            return false;
        }
        if (pos.line < range.begin.line || pos.line > range.end.line) {
            return false;
        }
        if (pos.line == range.begin.line && pos.column < range.begin.column) {
            return false;
        }
        if (pos.line == range.end.line && pos.column > range.end.column) {
            return false;
        }
        return true;
    }

    private static <T> T findAncestor(Node node, Class<T> cls) {
        Node cur = node;
        while (cur != null) {
            if (cls.isInstance(cur)) {
                return cls.cast(cur);
            }
            cur = cur.getParentNode().orElse(null);
        }
        return null;
    }

    private static boolean isConstructorContext(Node node) {
        Node cur = node;
        while (cur != null) {
            if (cur instanceof ObjectCreationExpr) {
                return true;
            }
            cur = cur.getParentNode().orElse(null);
        }
        return false;
    }

    private static int resolveConstructorArgCount(Node node) {
        Node cur = node;
        while (cur != null) {
            if (cur instanceof ObjectCreationExpr) {
                ObjectCreationExpr expr = (ObjectCreationExpr) cur;
                if (expr.getArguments() == null) {
                    return TypeSolver.ARG_COUNT_UNKNOWN;
                }
                return expr.getArguments().size();
            }
            cur = cur.getParentNode().orElse(null);
        }
        return TypeSolver.ARG_COUNT_UNKNOWN;
    }
}
