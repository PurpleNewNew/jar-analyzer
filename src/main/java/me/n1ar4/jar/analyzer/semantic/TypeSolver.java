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
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.InitializerDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.ArrayCreationExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.CharLiteralExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.DoubleLiteralExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.LiteralExpr;
import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.SuperExpr;
import com.github.javaparser.ast.expr.TextBlockLiteralExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.expr.TypeExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.engine.CoreEngine;
import me.n1ar4.jar.analyzer.entity.ClassResult;
import me.n1ar4.jar.analyzer.entity.LocalVarEntity;
import me.n1ar4.jar.analyzer.entity.MemberEntity;
import me.n1ar4.jar.analyzer.entity.MethodResult;
import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.jar.analyzer.utils.InterruptUtil;
import me.n1ar4.jar.analyzer.utils.JarUtil;
import me.n1ar4.jar.analyzer.utils.RuntimeClassResolver;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TypeSolver {
    private static final Logger logger = LogManager.getLogger();

    public static final int ARG_COUNT_UNKNOWN = -1;
    private static final String CACHE_TYPE_METHOD_RETURN = "method-ret";

    private final CoreEngine engine;
    private final CallResolver callResolver;
    private LineMapper lineMapper;
    private final FlowTypeAnalyzer flowTypeAnalyzer;
    private final java.util.concurrent.ConcurrentHashMap<String, String> methodReturnCache = new java.util.concurrent.ConcurrentHashMap<>();
    private volatile HierarchyResolver hierarchyResolver;
    private final java.util.concurrent.ConcurrentHashMap<String, String> classStampCache = new java.util.concurrent.ConcurrentHashMap<>();
    private volatile String lastRootKey = "";

    private static final Map<String, ClassSignatureCache> CLASS_SIGNATURE_CACHE = new ConcurrentHashMap<>();

    public TypeSolver(CoreEngine engine) {
        this.engine = engine;
        this.callResolver = new CallResolver(engine, this);
        this.flowTypeAnalyzer = new FlowTypeAnalyzer(this);
    }

    private static void debugIgnored(String context, Throwable t) {
        if (t == null) {
            return;
        }
        InterruptUtil.restoreInterruptIfNeeded(t);
        logger.debug("{}: {}", context, t.toString());
    }

    public CallResolver getCallResolver() {
        return callResolver;
    }

    public void setLineMapper(LineMapper mapper) {
        this.lineMapper = mapper;
        this.callResolver.setLineMapper(mapper);
    }

    LineMapper getLineMapper() {
        return lineMapper;
    }

    CoreEngine getEngine() {
        return engine;
    }

    public TypeRef resolveExpressionType(CompilationUnit cu, Expression expr, CallContext ctx) {
        if (expr == null) {
            return null;
        }
        Expression scope = unwrapEnclosed(expr);
        if (scope instanceof ThisExpr) {
            String cls = ctx != null ? ctx.className : resolveEnclosingClassName(cu, scope);
            if (cls != null) {
                return TypeRef.fromInternalName(normalizeClassName(cls));
            }
            return null;
        }
        if (scope instanceof LiteralExpr) {
            if (scope instanceof StringLiteralExpr || scope instanceof TextBlockLiteralExpr) {
                return TypeRef.fromInternalName("java/lang/String");
            }
            if (scope instanceof IntegerLiteralExpr) {
                return TypeRef.fromAsmType(Type.INT_TYPE);
            }
            if (scope instanceof LongLiteralExpr) {
                return TypeRef.fromAsmType(Type.LONG_TYPE);
            }
            if (scope instanceof DoubleLiteralExpr) {
                return TypeRef.fromAsmType(Type.DOUBLE_TYPE);
            }
            if (scope instanceof BooleanLiteralExpr) {
                return TypeRef.fromAsmType(Type.BOOLEAN_TYPE);
            }
            if (scope instanceof CharLiteralExpr) {
                return TypeRef.fromAsmType(Type.CHAR_TYPE);
            }
            if (scope instanceof NullLiteralExpr) {
                return null;
            }
        }
        if (scope instanceof UnaryExpr) {
            UnaryExpr unary = (UnaryExpr) scope;
            return resolveExpressionType(cu, unary.getExpression(), ctx);
        }
        if (scope instanceof AssignExpr) {
            AssignExpr assign = (AssignExpr) scope;
            return resolveExpressionType(cu, assign.getValue(), ctx);
        }
        if (scope instanceof BinaryExpr) {
            BinaryExpr binary = (BinaryExpr) scope;
            TypeRef left = resolveExpressionType(cu, binary.getLeft(), ctx);
            TypeRef right = resolveExpressionType(cu, binary.getRight(), ctx);
            if (binary.getOperator() == BinaryExpr.Operator.PLUS) {
                if (isStringType(left) || isStringType(right)) {
                    return TypeRef.fromInternalName("java/lang/String");
                }
            }
            if (left != null && right != null && left.isPrimitive() && right.isPrimitive()) {
                Type wider = widerPrimitiveType(left.asmType, right.asmType);
                if (wider != null) {
                    return TypeRef.fromAsmType(wider);
                }
            }
        }
        if (scope instanceof ArrayCreationExpr) {
            ArrayCreationExpr array = (ArrayCreationExpr) scope;
            TypeRef elementType = resolveTypeFromAstType(cu, array.getElementType());
            int dims = array.getLevels() == null ? 1 : array.getLevels().size();
            if (elementType != null && elementType.asmType != null) {
                try {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < Math.max(1, dims); i++) {
                        sb.append('[');
                    }
                    sb.append(elementType.asmType.getDescriptor());
                    return TypeRef.fromAsmType(Type.getType(sb.toString()));
                } catch (Exception ex) {
                    debugIgnored("TypeSolver resolveExpressionType array creation (asm)", ex);
                }
            }
            if (elementType != null && elementType.internalName != null) {
                try {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < Math.max(1, dims); i++) {
                        sb.append('[');
                    }
                    sb.append('L').append(elementType.internalName).append(';');
                    return TypeRef.fromAsmType(Type.getType(sb.toString()));
                } catch (Exception ex) {
                    debugIgnored("TypeSolver resolveExpressionType array creation (internal)", ex);
                }
            }
            return null;
        }
        if (scope instanceof MethodReferenceExpr) {
            TypeRef fiType = resolveFunctionalInterfaceTypeFromContext(cu, scope, ctx);
            if (fiType != null) {
                return fiType;
            }
        }
        if (scope instanceof TypeExpr) {
            TypeExpr typeExpr = (TypeExpr) scope;
            return resolveTypeFromAstType(cu, typeExpr.getType());
        }
        if (scope instanceof SuperExpr) {
            String cls = ctx != null ? ctx.className : resolveEnclosingClassName(cu, scope);
            if (cls != null && engine != null) {
                ClassResult result = engine.getClassByClass(cls);
                if (result != null && result.getSuperClassName() != null) {
                    return TypeRef.fromInternalName(result.getSuperClassName());
                }
            }
            return null;
        }
        if (scope instanceof NameExpr) {
            String name = ((NameExpr) scope).getNameAsString();
            if (looksLikeQualifiedClassName(name) || looksLikeClassName(name)) {
                String cls = resolveQualifiedClassName(cu, name);
                return cls == null ? null : TypeRef.fromInternalName(cls);
            }
            TypeRef localType = resolveLocalDeclaredType(
                    cu, name, scope.getBegin().orElse(null), scope);
            if (localType != null) {
                return localType;
            }
            TypeRef flowSensitive = flowTypeAnalyzer == null ? null
                    : flowTypeAnalyzer.resolveLocalType(cu, name, scope.getBegin().orElse(null), scope, ctx);
            if (flowSensitive != null) {
                return flowSensitive;
            }
            TypeRef flowType = resolveLocalAssignedType(
                    cu, name, scope.getBegin().orElse(null), scope, ctx);
            if (flowType != null) {
                return flowType;
            }
            TypeRef narrowed = resolveNarrowedTypeFromConditions(cu, name, scope);
            if (narrowed != null) {
                return narrowed;
            }
            TypeRef lambdaParamType = resolveLambdaParamType(
                    cu, name, scope.getBegin().orElse(null), scope, ctx);
            if (lambdaParamType != null) {
                return lambdaParamType;
            }
            TypeRef fieldAstType = resolveFieldTypeInAst(cu, name, scope);
            if (fieldAstType != null) {
                return fieldAstType;
            }
            SymbolRef decl = resolveDeclaration(cu, ((NameExpr) scope).getName());
            if (decl != null && decl.getTypeRef() != null && decl.getTypeRef().internalName != null) {
                return TypeRef.fromInternalName(decl.getTypeRef().internalName);
            }
            if (ctx != null) {
                TypeRef bytecodeLocal = resolveLocalVarTypeFromDbType(
                        ctx.className, ctx.methodName, ctx.methodDesc, name, ctx.line);
                if (bytecodeLocal != null) {
                    return bytecodeLocal;
                }
                TypeRef fieldType = resolveFieldTypeFromDb(ctx.className, name);
                if (fieldType != null) {
                    return fieldType;
                }
            }
            String staticOwner = resolveStaticImportOwner(cu, name);
            if (staticOwner != null) {
                TypeRef fieldType = resolveFieldTypeFromDb(staticOwner, name);
                if (fieldType != null) {
                    return fieldType;
                }
                return TypeRef.fromInternalName(staticOwner);
            }
            return null;
        }
        if (scope instanceof FieldAccessExpr) {
            FieldAccessExpr fa = (FieldAccessExpr) scope;
            Expression faScope = unwrapEnclosed(fa.getScope());
            if (faScope instanceof ThisExpr || faScope instanceof SuperExpr) {
                TypeRef astField = resolveFieldTypeInAst(cu, fa.getNameAsString(), fa);
                if (astField != null) {
                    return astField;
                }
                SymbolRef field = resolveFieldDeclaration(cu, fa.getNameAsString(), fa);
                if (field != null && field.getTypeRef() != null) {
                    return field.getTypeRef();
                }
            }
            TypeRef scopeType = resolveExpressionType(cu, faScope, ctx);
            if (scopeType != null && scopeType.internalName != null) {
                TypeRef fieldType = resolveFieldTypeFromDb(scopeType.internalName, fa.getNameAsString());
                if (fieldType != null) {
                    return fieldType;
                }
            }
            String text = fa.toString();
            if (looksLikeQualifiedClassName(text)) {
                String cls = resolveQualifiedClassName(cu, text);
                return cls == null ? null : TypeRef.fromInternalName(cls);
            }
            if (faScope instanceof NameExpr) {
                String scopeName = ((NameExpr) faScope).getNameAsString();
                if (looksLikeClassName(scopeName)) {
                    String cls = resolveQualifiedClassName(cu, text);
                    return cls == null ? null : TypeRef.fromInternalName(cls);
                }
            }
            return null;
        }
        if (scope instanceof ObjectCreationExpr) {
            ObjectCreationExpr oce = (ObjectCreationExpr) scope;
            return resolveTypeFromAstType(cu, oce.getType());
        }
        if (scope instanceof CastExpr) {
            CastExpr cast = (CastExpr) scope;
            return resolveTypeFromAstType(cu, cast.getType());
        }
        if (scope instanceof ConditionalExpr) {
            ConditionalExpr cond = (ConditionalExpr) scope;
            TypeRef thenType = resolveExpressionType(cu, cond.getThenExpr(), ctx);
            TypeRef elseType = resolveExpressionType(cu, cond.getElseExpr(), ctx);
            if (thenType != null && elseType != null
                    && thenType.internalName != null
                    && thenType.internalName.equals(elseType.internalName)) {
                return thenType;
            }
            return thenType != null ? thenType : elseType;
        }
        if (scope instanceof LambdaExpr) {
            TypeRef functionalType = resolveFunctionalInterfaceTypeFromContext(cu, scope, ctx);
            if (functionalType != null) {
                return functionalType;
            }
            Node parent = scope.getParentNode().orElse(null);
            if (parent instanceof VariableDeclarator) {
                VariableDeclarator vd = (VariableDeclarator) parent;
                return resolveTypeFromAstType(cu, vd.getType());
            }
            if (parent instanceof Parameter) {
                Parameter param = (Parameter) parent;
                return resolveTypeFromAstType(cu, param.getType());
            }
        }
        if (scope instanceof MethodCallExpr) {
            return resolveMethodCallReturnType(cu, (MethodCallExpr) scope, ctx);
        }
        return null;
    }

    public TypeRef resolveTypeFromClassName(String className) {
        if (className == null || className.trim().isEmpty()) {
            return null;
        }
        String normalized = normalizeClassName(className);
        return TypeRef.fromInternalName(normalized);
    }

    public TypeRef resolveTypeFromAstType(CompilationUnit cu,
                                          com.github.javaparser.ast.type.Type type) {
        if (type == null) {
            return null;
        }
        if (type.isVarType()) {
            return null;
        }
        if (type.isTypeParameter()) {
            return null;
        }
        if (type.isWildcardType()) {
            com.github.javaparser.ast.type.WildcardType wt = type.asWildcardType();
            if (wt.getExtendedType().isPresent()) {
                return resolveTypeFromAstType(cu, wt.getExtendedType().get());
            }
            if (wt.getSuperType().isPresent()) {
                return resolveTypeFromAstType(cu, wt.getSuperType().get());
            }
            return TypeRef.fromInternalName("java/lang/Object");
        }
        if (type.isPrimitiveType()) {
            try {
                com.github.javaparser.ast.type.PrimitiveType prim = type.asPrimitiveType();
                switch (prim.getType()) {
                    case BOOLEAN:
                        return TypeRef.fromAsmType(Type.BOOLEAN_TYPE);
                    case BYTE:
                        return TypeRef.fromAsmType(Type.BYTE_TYPE);
                    case CHAR:
                        return TypeRef.fromAsmType(Type.CHAR_TYPE);
                    case SHORT:
                        return TypeRef.fromAsmType(Type.SHORT_TYPE);
                    case INT:
                        return TypeRef.fromAsmType(Type.INT_TYPE);
                    case LONG:
                        return TypeRef.fromAsmType(Type.LONG_TYPE);
                    case FLOAT:
                        return TypeRef.fromAsmType(Type.FLOAT_TYPE);
                    case DOUBLE:
                        return TypeRef.fromAsmType(Type.DOUBLE_TYPE);
                    default:
                        return null;
                }
            } catch (Exception ex) {
                debugIgnored("TypeSolver resolveTypeFromAstType primitive", ex);
                return null;
            }
        }
        if (type.isArrayType()) {
            com.github.javaparser.ast.type.Type element = type.getElementType();
            TypeRef elementType = resolveTypeFromAstType(cu, element);
            if (elementType != null && elementType.asmType != null) {
                try {
                    String desc = elementType.asmType.getDescriptor();
                    return TypeRef.fromAsmType(Type.getType("[" + desc));
                } catch (Exception ex) {
                    debugIgnored("TypeSolver resolveTypeFromAstType array (asm)", ex);
                }
            }
            if (elementType != null && elementType.internalName != null) {
                try {
                    return TypeRef.fromAsmType(Type.getType("[L" + elementType.internalName + ";"));
                } catch (Exception ex) {
                    debugIgnored("TypeSolver resolveTypeFromAstType array (internal)", ex);
                }
            }
            return null;
        }
        String name = type.asString();
        String raw = normalizeTypeName(name);
        String className = resolveQualifiedClassName(cu, raw == null ? name : raw);
        List<String> args = resolveAstTypeArguments(cu, type);
        return className == null ? null : TypeRef.fromInternalName(className, args);
    }

    private List<String> resolveAstTypeArguments(CompilationUnit cu,
                                                 com.github.javaparser.ast.type.Type type) {
        if (type == null || !type.isClassOrInterfaceType()) {
            return null;
        }
        ClassOrInterfaceType cit = type.asClassOrInterfaceType();
        if (!cit.getTypeArguments().isPresent()) {
            return null;
        }
        List<String> out = new ArrayList<>();
        for (com.github.javaparser.ast.type.Type arg : cit.getTypeArguments().get()) {
            TypeRef resolved = resolveTypeFromAstType(cu, arg);
            if (resolved != null) {
                out.add(resolved.internalName);
            }
        }
        return out.isEmpty() ? null : out;
    }

    public TypeRef resolveMethodCallReturnType(CompilationUnit cu,
                                               MethodCallExpr call,
                                               CallContext ctx) {
        if (call == null) {
            return null;
        }
        String scopeHint = null;
        TypeRef scopeType = null;
        if (call.getScope().isPresent()) {
            scopeType = resolveExpressionType(cu, call.getScope().get(), ctx);
            if (scopeType != null) {
                scopeHint = scopeType.internalName;
            }
        } else {
            scopeHint = resolveStaticImportOwner(cu, call.getNameAsString());
            if (scopeHint == null && ctx != null) {
                scopeHint = ctx.className;
            }
            if (scopeHint == null) {
                scopeHint = resolveEnclosingClassName(cu, call);
            }
        }
        List<TypeRef> argTypes = resolveArgumentTypes(cu, call.getArguments(), ctx);
        CallSiteSelection selection = null;
        if (ctx != null) {
            CallPosition callPos = callResolver.resolveCallPosition(cu, call, ctx);
            selection = callResolver.resolveCallSiteTarget(
                    ctx,
                    call.getNameAsString(),
                    call.getArguments() == null ? ARG_COUNT_UNKNOWN : call.getArguments().size(),
                    scopeHint,
                    callPos);
        }
        if (selection == null && scopeHint != null) {
            boolean preferStatic = callResolver.isStaticScope(cu, call, ctx);
            String methodDesc = callResolver.resolveMethodDescByCall(
                    cu,
                    scopeHint,
                    call,
                    ctx,
                    scopeType,
                    preferStatic);
            if (methodDesc == null) {
                methodDesc = callResolver.resolveMethodDescByArgTypes(
                        scopeHint,
                        call.getNameAsString(),
                        argTypes,
                        scopeType,
                        preferStatic);
            }
            if (methodDesc == null) {
                methodDesc = callResolver.resolveMethodDescByArgCount(
                        scopeHint,
                        call.getNameAsString(),
                        call.getArguments() == null ? ARG_COUNT_UNKNOWN : call.getArguments().size());
            }
            if (methodDesc != null) {
                selection = new CallSiteSelection(scopeHint, call.getNameAsString(), methodDesc);
            }
        }
        if (selection == null) {
            return null;
        }
        return resolveMethodReturnType(selection.className,
                selection.methodName, selection.methodDesc, scopeType, argTypes);
    }

    public int resolveMethodReferenceArgCount(CompilationUnit cu,
                                              MethodReferenceExpr ref,
                                              CallContext ctx) {
        if (ref == null) {
            return ARG_COUNT_UNKNOWN;
        }
        TypeRef fiType = resolveFunctionalInterfaceTypeFromContext(cu, ref, ctx);
        if (fiType == null || fiType.internalName == null) {
            return ARG_COUNT_UNKNOWN;
        }
        MethodResult sam = resolveSamMethod(fiType.internalName);
        if (sam == null) {
            return ARG_COUNT_UNKNOWN;
        }
        int samArgs = argCountFromDesc(sam.getMethodDesc());
        if (samArgs <= 0) {
            return samArgs;
        }
        String identifier = ref.getIdentifier();
        if ("new".equals(identifier)) {
            return samArgs;
        }
        Expression scope = ref.getScope();
        if (!(scope instanceof TypeExpr)) {
            return samArgs;
        }
        String classHint = null;
        TypeRef scopeType = resolveExpressionType(cu, scope, ctx);
        if (scopeType != null && scopeType.internalName != null) {
            classHint = scopeType.internalName;
        }
        if (classHint == null || engine == null) {
            return samArgs;
        }
        List<MethodResult> methods = engine.getMethod(classHint, identifier, null);
        if (methods == null || methods.isEmpty()) {
            return samArgs;
        }
        boolean hasStatic = false;
        boolean hasInstance = false;
        int instanceArgs = Math.max(0, samArgs - 1);
        for (MethodResult method : methods) {
            if (method == null || method.getMethodDesc() == null) {
                continue;
            }
            int count = argCountFromDesc(method.getMethodDesc());
            if (method.getIsStaticInt() == 1 && count == samArgs) {
                hasStatic = true;
            }
            if (method.getIsStaticInt() == 0 && count == instanceArgs) {
                hasInstance = true;
            }
        }
        if (hasStatic) {
            return samArgs;
        }
        if (hasInstance) {
            return instanceArgs;
        }
        return samArgs;
    }

    public TypeRef resolveLambdaParamType(CompilationUnit cu,
                                          String name,
                                          Position usagePos,
                                          Node context,
                                          CallContext ctx) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        Node scopeRoot = findScopeRoot(context);
        if (!(scopeRoot instanceof LambdaExpr)) {
            return null;
        }
        LambdaExpr lambda = (LambdaExpr) scopeRoot;
        int paramIndex = -1;
        if (lambda.getParameters() != null) {
            for (int i = 0; i < lambda.getParameters().size(); i++) {
                Parameter param = lambda.getParameters().get(i);
                if (param != null && name.equals(param.getNameAsString())) {
                    paramIndex = i;
                    break;
                }
            }
        }
        if (paramIndex < 0) {
            return null;
        }
        TypeRef fiType = resolveFunctionalInterfaceTypeFromContext(cu, lambda, ctx);
        if (fiType == null || fiType.internalName == null) {
            return null;
        }
        MethodResult sam = resolveSamMethod(fiType.internalName);
        if (sam == null || sam.getMethodDesc() == null) {
            return null;
        }
        TypeRef genericResolved = resolveSamParamType(fiType, sam, paramIndex);
        if (genericResolved != null) {
            return genericResolved;
        }
        try {
            Type[] args = Type.getArgumentTypes(sam.getMethodDesc());
            if (paramIndex < 0 || paramIndex >= args.length) {
                return null;
            }
            Type arg = args[paramIndex];
            if (arg.getSort() == Type.OBJECT) {
                return TypeRef.fromInternalName(arg.getInternalName());
            }
            if (arg.getSort() == Type.ARRAY) {
                Type element = arg.getElementType();
                if (element != null && element.getSort() == Type.OBJECT) {
                    return TypeRef.fromInternalName(element.getInternalName());
                }
            }
        } catch (Exception ex) {
            debugIgnored("TypeSolver resolveSamParamType", ex);
        }
        return null;
    }

    public TypeRef resolveFunctionalInterfaceTypeFromContext(CompilationUnit cu,
                                                             Node node,
                                                             CallContext ctx) {
        if (node == null) {
            return null;
        }
        Node parent = node.getParentNode().orElse(null);
        if (parent instanceof EnclosedExpr) {
            parent = parent.getParentNode().orElse(null);
        }
        if (parent instanceof CastExpr) {
            return resolveTypeFromAstType(cu, ((CastExpr) parent).getType());
        }
        if (parent instanceof VariableDeclarator) {
            VariableDeclarator vd = (VariableDeclarator) parent;
            return resolveTypeFromAstType(cu, vd.getType());
        }
        if (parent instanceof AssignExpr) {
            AssignExpr assign = (AssignExpr) parent;
            return resolveExpressionType(cu, assign.getTarget(), ctx);
        }
        if (parent instanceof MethodCallExpr) {
            MethodCallExpr call = (MethodCallExpr) parent;
            int argIndex = resolveArgumentIndex(call.getArguments(), node);
            if (argIndex >= 0) {
                return resolveFunctionalInterfaceTypeFromCallArg(cu, call, argIndex, ctx);
            }
        }
        if (parent instanceof ObjectCreationExpr) {
            ObjectCreationExpr call = (ObjectCreationExpr) parent;
            int argIndex = resolveArgumentIndex(call.getArguments(), node);
            if (argIndex >= 0) {
                return resolveFunctionalInterfaceTypeFromCtorArg(cu, call, argIndex);
            }
        }
        if (parent instanceof com.github.javaparser.ast.stmt.ReturnStmt) {
            if (ctx != null && ctx.className != null
                    && ctx.methodName != null && ctx.methodDesc != null) {
                return resolveMethodReturnType(ctx.className, ctx.methodName, ctx.methodDesc, null);
            }
        }
        return null;
    }

    private int resolveArgumentIndex(List<Expression> args, Node target) {
        if (args == null || args.isEmpty() || target == null) {
            return -1;
        }
        for (int i = 0; i < args.size(); i++) {
            Expression arg = args.get(i);
            if (arg == null) {
                continue;
            }
            if (arg == target) {
                return i;
            }
            Range r1 = arg.getRange().orElse(null);
            Range r2 = target.getRange().orElse(null);
            if (r1 != null && r2 != null && r1.equals(r2)) {
                return i;
            }
        }
        return -1;
    }

    private TypeRef resolveFunctionalInterfaceTypeFromCallArg(CompilationUnit cu,
                                                              MethodCallExpr call,
                                                              int argIndex,
                                                              CallContext ctx) {
        if (call == null) {
            return null;
        }
        String scopeHint = callResolver.resolveScopeClassName(cu, call, ctx);
        if (scopeHint == null) {
            return null;
        }
        int argCount = call.getArguments() == null ? ARG_COUNT_UNKNOWN : call.getArguments().size();
        CallSiteSelection selection = null;
        if (ctx != null) {
            CallPosition callPos = callResolver.resolveCallPosition(cu, call, ctx);
            selection = callResolver.resolveCallSiteTarget(ctx,
                    call.getNameAsString(),
                    argCount,
                    scopeHint,
                    callPos);
        }
        if (selection == null) {
            List<TypeRef> argTypes = new ArrayList<>();
            if (call.getArguments() != null) {
                for (int i = 0; i < call.getArguments().size(); i++) {
                    if (i == argIndex) {
                        argTypes.add(null);
                    } else {
                        argTypes.add(resolveExpressionType(cu, call.getArguments().get(i), ctx));
                    }
                }
            }
            TypeRef scopeType = call.getScope().isPresent()
                    ? resolveExpressionType(cu, call.getScope().get(), ctx)
                    : null;
            boolean preferStatic = callResolver.isStaticScope(cu, call, ctx);
            String methodDesc = callResolver.resolveMethodDescByArgTypes(
                    scopeHint, call.getNameAsString(), argTypes, scopeType, preferStatic);
            if (methodDesc == null) {
                methodDesc = callResolver.resolveMethodDescByArgCount(scopeHint, call.getNameAsString(), argCount);
            }
            if (methodDesc != null) {
                selection = new CallSiteSelection(scopeHint, call.getNameAsString(), methodDesc);
            }
        }
        if (selection == null || selection.methodDesc == null) {
            return null;
        }
        try {
            Type[] args = Type.getArgumentTypes(selection.methodDesc);
            if (argIndex < 0 || argIndex >= args.length) {
                return null;
            }
            Type arg = args[argIndex];
            if (arg.getSort() == Type.OBJECT) {
                return TypeRef.fromInternalName(arg.getInternalName());
            }
            if (arg.getSort() == Type.ARRAY) {
                Type element = arg.getElementType();
                if (element != null && element.getSort() == Type.OBJECT) {
                    return TypeRef.fromInternalName(element.getInternalName());
                }
            }
        } catch (Exception ex) {
            debugIgnored("TypeSolver resolveFunctionalInterfaceTypeFromCallArg", ex);
        }
        return null;
    }

    private TypeRef resolveFunctionalInterfaceTypeFromCtorArg(CompilationUnit cu,
                                                              ObjectCreationExpr call,
                                                              int argIndex) {
        if (call == null) {
            return null;
        }
        TypeRef owner = resolveTypeFromAstType(cu, call.getType());
        if (owner == null || owner.internalName == null) {
            return null;
        }
        int argCount = call.getArguments() == null ? ARG_COUNT_UNKNOWN : call.getArguments().size();
        String desc = callResolver.resolveMethodDescByArgCount(owner.internalName, "<init>", argCount);
        if (desc == null) {
            return null;
        }
        try {
            Type[] args = Type.getArgumentTypes(desc);
            if (argIndex < 0 || argIndex >= args.length) {
                return null;
            }
            Type arg = args[argIndex];
            if (arg.getSort() == Type.OBJECT) {
                return TypeRef.fromInternalName(arg.getInternalName());
            }
            if (arg.getSort() == Type.ARRAY) {
                Type element = arg.getElementType();
                if (element != null && element.getSort() == Type.OBJECT) {
                    return TypeRef.fromInternalName(element.getInternalName());
                }
            }
        } catch (Exception ex) {
            debugIgnored("TypeSolver resolveFunctionalInterfaceTypeFromCtorArg", ex);
        }
        return null;
    }

    private MethodResult resolveSamMethod(String interfaceName) {
        if (interfaceName == null || interfaceName.trim().isEmpty()) {
            return null;
        }
        if (engine == null) {
            return null;
        }
        List<MethodResult> methods = engine.getMethod(interfaceName, null, null);
        if (methods == null || methods.isEmpty()) {
            return null;
        }
        List<MethodResult> candidates = new ArrayList<>();
        for (MethodResult method : methods) {
            if (method == null || method.getMethodName() == null) {
                continue;
            }
            String name = method.getMethodName();
            if ("<init>".equals(name) || "<clinit>".equals(name)) {
                continue;
            }
            if (method.getIsStaticInt() == 1) {
                continue;
            }
            if ((method.getAccessInt() & Opcodes.ACC_ABSTRACT) == 0) {
                continue;
            }
            candidates.add(method);
        }
        if (candidates.size() == 1) {
            return candidates.get(0);
        }
        return null;
    }

    boolean isFunctionalInterface(String interfaceName) {
        return resolveSamMethod(interfaceName) != null;
    }

    MethodResult resolveSamMethodPublic(String interfaceName) {
        return resolveSamMethod(interfaceName);
    }

    boolean isMethodReferenceCompatible(MethodReferenceExpr ref,
                                        TypeRef targetType,
                                        CompilationUnit cu,
                                        CallContext ctx) {
        if (ref == null || targetType == null || targetType.internalName == null) {
            return false;
        }
        MethodResult sam = resolveSamMethod(targetType.internalName);
        if (sam == null || sam.getMethodDesc() == null) {
            return false;
        }
        Type samReturn;
        try {
            TypeRef samReturnType = resolveMethodReturnType(
                    targetType.internalName, sam.getMethodName(), sam.getMethodDesc(), targetType);
            samReturn = samReturnType == null ? Type.getReturnType(sam.getMethodDesc()) : toAsmType(samReturnType);
        } catch (Exception ex) {
            samReturn = null;
        }
        List<TypeRef> samParams = resolveSamParamTypes(targetType, sam);
        int samArgs = argCountFromDesc(sam.getMethodDesc());
        if (samArgs == ARG_COUNT_UNKNOWN) {
            samArgs = -1;
        }
        String identifier = ref.getIdentifier();
        if (identifier == null || identifier.trim().isEmpty()) {
            return false;
        }
        Expression scope = ref.getScope();
        if (scope == null) {
            return false;
        }
        String owner = resolveClassNameFromExpression(cu, scope, ref, ctx);
        if (owner == null || owner.trim().isEmpty()) {
            return false;
        }
        if (engine == null) {
            return true;
        }
        if ("new".equals(identifier)) {
            List<MethodResult> ctors = engine.getMethod(owner, "<init>", null);
            if (ctors == null || ctors.isEmpty()) {
                return false;
            }
            for (MethodResult ctor : ctors) {
                if (ctor == null || ctor.getMethodDesc() == null) {
                    continue;
                }
                if (samReturn != null) {
                    Type ctorType = Type.getObjectType(owner);
                    if (!isReturnTypeCompatible(ctorType, samReturn)) {
                        continue;
                    }
                }
                if (!isMethodRefParamsCompatible(samParams, ctor.getMethodDesc(), false, owner)) {
                    continue;
                }
                if (samArgs >= 0 && argCountFromDesc(ctor.getMethodDesc()) == samArgs) {
                    return true;
                }
            }
            return samArgs < 0;
        }
        List<MethodResult> methods = engine.getMethod(owner, identifier, null);
        if (methods == null || methods.isEmpty()) {
            return false;
        }
        boolean scopeIsType = scope instanceof TypeExpr;
        int instanceArgs = scopeIsType ? Math.max(0, samArgs - 1) : samArgs;
        for (MethodResult method : methods) {
            if (method == null || method.getMethodDesc() == null) {
                continue;
            }
            if (samReturn != null) {
                try {
                    Type ret = Type.getReturnType(method.getMethodDesc());
                    if (!isReturnTypeCompatible(ret, samReturn)) {
                        continue;
                    }
                } catch (Exception ex) {
                    debugIgnored("TypeSolver resolveFunctionalInterfaceTypeFromMethodRef return", ex);
                }
            }
            if (scopeIsType) {
                boolean staticOk = method.getIsStaticInt() == 1
                        && isMethodRefParamsCompatible(samParams, method.getMethodDesc(), false, owner);
                boolean instanceOk = method.getIsStaticInt() == 0
                        && isMethodRefParamsCompatible(samParams, method.getMethodDesc(), true, owner);
                if (!staticOk && !instanceOk) {
                    continue;
                }
            } else {
                if (!isMethodRefParamsCompatible(samParams, method.getMethodDesc(), false, owner)) {
                    continue;
                }
            }
            int count = argCountFromDesc(method.getMethodDesc());
            if (samArgs >= 0) {
                if (method.getIsStaticInt() == 1 && count == samArgs) {
                    return true;
                }
                if (method.getIsStaticInt() == 0 && count == instanceArgs) {
                    return true;
                }
            } else {
                return true;
            }
        }
        return false;
    }

    boolean isLambdaCompatible(LambdaExpr lambda,
                               TypeRef targetType,
                               CompilationUnit cu,
                               CallContext ctx) {
        if (lambda == null || targetType == null || targetType.internalName == null) {
            return false;
        }
        MethodResult sam = resolveSamMethod(targetType.internalName);
        if (sam == null || sam.getMethodDesc() == null) {
            return false;
        }
        List<TypeRef> samParams = resolveSamParamTypes(targetType, sam);
        if (lambda.getParameters() != null && samParams != null) {
            for (int i = 0; i < lambda.getParameters().size() && i < samParams.size(); i++) {
                Parameter param = lambda.getParameters().get(i);
                if (param == null) {
                    continue;
                }
                if (!(param.getType() instanceof com.github.javaparser.ast.type.UnknownType)) {
                    TypeRef declared = resolveTypeFromAstType(cu, param.getType());
                    if (declared != null && !isAssignable(declared, samParams.get(i))) {
                        return false;
                    }
                }
            }
        }
        Type samReturn;
        try {
            TypeRef samReturnType = resolveMethodReturnType(
                    targetType.internalName, sam.getMethodName(), sam.getMethodDesc(), targetType);
            samReturn = samReturnType == null ? Type.getReturnType(sam.getMethodDesc()) : toAsmType(samReturnType);
        } catch (Exception ex) {
            samReturn = null;
        }
        if (samReturn == null || samReturn.getSort() == Type.VOID) {
            return true;
        }
        Statement body = lambda.getBody();
        if (body instanceof ExpressionStmt) {
            Expression expr = ((ExpressionStmt) body).getExpression();
            TypeRef exprType = resolveExpressionType(cu, expr, ctx);
            if (exprType == null) {
                return true;
            }
            Type candidate = toAsmType(exprType);
            return candidate == null || isReturnTypeCompatible(candidate, samReturn);
        }
        if (body instanceof BlockStmt) {
            BlockStmt block = (BlockStmt) body;
            List<ReturnStmt> returns = block.findAll(ReturnStmt.class);
            if (returns.isEmpty()) {
                return false;
            }
            for (ReturnStmt ret : returns) {
                if (!ret.getExpression().isPresent()) {
                    return false;
                }
                Expression expr = ret.getExpression().get();
                TypeRef exprType = resolveExpressionType(cu, expr, ctx);
                if (exprType == null) {
                    continue;
                }
                Type candidate = toAsmType(exprType);
                if (candidate != null && !isReturnTypeCompatible(candidate, samReturn)) {
                    return false;
                }
            }
            return true;
        }
        return true;
    }

    private boolean isReturnTypeCompatible(Type candidate, Type target) {
        if (candidate == null || target == null) {
            return true;
        }
        if (target.getSort() == Type.VOID) {
            return candidate.getSort() == Type.VOID;
        }
        if (candidate.getSort() == Type.VOID) {
            return false;
        }
        int candSort = candidate.getSort();
        int targetSort = target.getSort();
        if (candSort >= Type.BOOLEAN && candSort <= Type.DOUBLE
                && targetSort >= Type.BOOLEAN && targetSort <= Type.DOUBLE) {
            return candSort == targetSort || isWideningPrimitive(candSort, targetSort);
        }
        if (candSort == Type.OBJECT && targetSort == Type.OBJECT) {
            String candName = candidate.getInternalName();
            String targetName = target.getInternalName();
            return isSubtypeOrUnknown(candName, targetName);
        }
        if (candSort == Type.ARRAY && targetSort == Type.OBJECT) {
            String targetName = target.getInternalName();
            return "java/lang/Object".equals(targetName)
                    || "java/lang/Cloneable".equals(targetName)
                    || "java/io/Serializable".equals(targetName);
        }
        if (candSort == Type.ARRAY && targetSort == Type.ARRAY) {
            return isArrayAssignable(candidate, target);
        }
        return false;
    }

    private boolean isArrayAssignable(Type fromType, Type toType) {
        if (fromType == null || toType == null) {
            return true;
        }
        if (fromType.getSort() != Type.ARRAY || toType.getSort() != Type.ARRAY) {
            return false;
        }
        Type fromElem = fromType.getElementType();
        Type toElem = toType.getElementType();
        int fromSort = fromElem.getSort();
        int toSort = toElem.getSort();
        boolean fromPrimitive = fromSort >= Type.BOOLEAN && fromSort <= Type.DOUBLE;
        boolean toPrimitive = toSort >= Type.BOOLEAN && toSort <= Type.DOUBLE;
        if (fromPrimitive || toPrimitive) {
            return fromSort == toSort;
        }
        if (fromSort == Type.ARRAY && toSort == Type.ARRAY) {
            return isArrayAssignable(fromElem, toElem);
        }
        if (fromSort == Type.ARRAY && toSort == Type.OBJECT) {
            String toName = toElem.getInternalName();
            return "java/lang/Object".equals(toName)
                    || "java/lang/Cloneable".equals(toName)
                    || "java/io/Serializable".equals(toName);
        }
        if (fromSort == Type.OBJECT && toSort == Type.ARRAY) {
            return false;
        }
        String fromName = fromElem.getInternalName();
        String toName = toElem.getInternalName();
        return isSubtypeOrUnknown(fromName, toName);
    }

    private boolean isSubtypeOrUnknown(String fromName, String toName) {
        if (fromName == null || toName == null) {
            return true;
        }
        if (fromName.equals(toName)) {
            return true;
        }
        HierarchyResolver hierarchy = getHierarchyResolver();
        if (hierarchy == null) {
            return true;
        }
        return hierarchy.isSubtype(fromName, toName);
    }

    private Type toAsmType(TypeRef type) {
        if (type == null) {
            return null;
        }
        if (type.asmType != null) {
            return type.asmType;
        }
        if (type.internalName != null && !type.internalName.trim().isEmpty()) {
            return Type.getObjectType(type.internalName);
        }
        return null;
    }

    private boolean isMethodRefParamsCompatible(List<TypeRef> samParams,
                                                String methodDesc,
                                                boolean instanceMethod,
                                                String owner) {
        if (samParams == null) {
            return true;
        }
        List<TypeRef> methodParams = resolveParamTypes(methodDesc);
        if (methodParams == null) {
            return true;
        }
        int samCount = samParams.size();
        int methodCount = methodParams.size();
        if (instanceMethod) {
            if (samCount == 0 || methodCount != samCount - 1) {
                return false;
            }
            if (owner != null) {
                TypeRef ownerRef = TypeRef.fromInternalName(owner);
                if (!isAssignable(samParams.get(0), ownerRef)) {
                    return false;
                }
            }
            for (int i = 0; i < methodCount; i++) {
                if (!isAssignable(samParams.get(i + 1), methodParams.get(i))) {
                    return false;
                }
            }
            return true;
        }
        if (samCount != methodCount) {
            return false;
        }
        for (int i = 0; i < methodCount; i++) {
            if (!isAssignable(samParams.get(i), methodParams.get(i))) {
                return false;
            }
        }
        return true;
    }

    private List<TypeRef> resolveSamParamTypes(TypeRef interfaceType, MethodResult sam) {
        if (sam == null || sam.getMethodDesc() == null) {
            return null;
        }
        List<TypeRef> params = new ArrayList<>();
        try {
            Type[] args = Type.getArgumentTypes(sam.getMethodDesc());
            for (Type arg : args) {
                params.add(TypeRef.fromAsmType(arg));
            }
        } catch (Exception ex) {
            debugIgnored("TypeSolver resolveSamParamTypes", ex);
        }
        if (interfaceType != null && interfaceType.internalName != null) {
            for (int i = 0; i < params.size(); i++) {
                TypeRef resolved = resolveSamParamType(interfaceType, sam, i);
                if (resolved != null) {
                    params.set(i, resolved);
                }
            }
        }
        return params;
    }

    private List<TypeRef> resolveParamTypes(String desc) {
        if (desc == null || desc.trim().isEmpty()) {
            return null;
        }
        List<TypeRef> params = new ArrayList<>();
        try {
            Type[] args = Type.getArgumentTypes(desc);
            for (Type arg : args) {
                params.add(TypeRef.fromAsmType(arg));
            }
        } catch (Exception ex) {
            debugIgnored("TypeSolver resolveParamTypes", ex);
            return null;
        }
        return params;
    }

    private boolean isAssignable(TypeRef from, TypeRef to) {
        if (to == null) {
            return true;
        }
        if (from == null) {
            return true;
        }
        Type fromType = toAsmType(from);
        Type toType = toAsmType(to);
        if (fromType == null || toType == null) {
            return true;
        }
        int fromSort = fromType.getSort();
        int toSort = toType.getSort();
        if (fromSort >= Type.BOOLEAN && fromSort <= Type.DOUBLE
                && toSort >= Type.BOOLEAN && toSort <= Type.DOUBLE) {
            return fromSort == toSort || isWideningPrimitive(fromSort, toSort);
        }
        if (fromSort >= Type.BOOLEAN && fromSort <= Type.DOUBLE && toSort == Type.OBJECT) {
            String boxed = wrapperInternalName(fromSort);
            String targetName = toType.getInternalName();
            if (boxed == null || targetName == null) {
                return true;
            }
            if (boxed.equals(targetName)) {
                return true;
            }
            return isSubtypeOrUnknown(boxed, targetName);
        }
        if (fromSort == Type.OBJECT && toSort >= Type.BOOLEAN && toSort <= Type.DOUBLE) {
            Integer primitive = primitiveForWrapper(fromType.getInternalName());
            return primitive != null && (primitive == toSort || isWideningPrimitive(primitive, toSort));
        }
        if (fromSort == Type.OBJECT && toSort == Type.OBJECT) {
            String fromName = fromType.getInternalName();
            String toName = toType.getInternalName();
            return isSubtypeOrUnknown(fromName, toName);
        }
        if (fromSort == Type.ARRAY && toSort == Type.OBJECT) {
            String toName = toType.getInternalName();
            return "java/lang/Object".equals(toName)
                    || "java/lang/Cloneable".equals(toName)
                    || "java/io/Serializable".equals(toName);
        }
        if (fromSort == Type.ARRAY && toSort == Type.ARRAY) {
            return isArrayAssignable(fromType, toType);
        }
        return fromType.getDescriptor().equals(toType.getDescriptor());
    }

    private boolean isWideningPrimitive(int from, int to) {
        if (from == to) {
            return true;
        }
        switch (from) {
            case Type.BYTE:
                return to == Type.SHORT || to == Type.INT || to == Type.LONG
                        || to == Type.FLOAT || to == Type.DOUBLE;
            case Type.SHORT:
                return to == Type.INT || to == Type.LONG || to == Type.FLOAT || to == Type.DOUBLE;
            case Type.CHAR:
                return to == Type.INT || to == Type.LONG || to == Type.FLOAT || to == Type.DOUBLE;
            case Type.INT:
                return to == Type.LONG || to == Type.FLOAT || to == Type.DOUBLE;
            case Type.LONG:
                return to == Type.FLOAT || to == Type.DOUBLE;
            case Type.FLOAT:
                return to == Type.DOUBLE;
            default:
                return false;
        }
    }

    private TypeRef resolveSamParamType(TypeRef interfaceType,
                                        MethodResult sam,
                                        int paramIndex) {
        if (interfaceType == null || sam == null) {
            return null;
        }
        if (interfaceType.internalName == null || interfaceType.internalName.trim().isEmpty()) {
            return null;
        }
        ClassSignatureCache cache = getClassSignatureCache(interfaceType.internalName);
        if (cache == null || cache.methodSignatures == null || cache.methodSignatures.isEmpty()) {
            return null;
        }
        String signature = cache.methodSignatures.get(sam.getMethodName() + sam.getMethodDesc());
        if (signature == null || signature.trim().isEmpty()) {
            return null;
        }
        List<GenericType> params = parseMethodParamSignatures(signature);
        if (params == null || paramIndex < 0 || paramIndex >= params.size()) {
            return null;
        }
        Map<String, String> bindings = buildGenericBindings(cache, interfaceType);
        return resolveGenericType(params.get(paramIndex), bindings);
    }

    public SymbolRef resolveDeclaration(CompilationUnit cu, SimpleName name) {
        if (name == null) {
            return null;
        }
        Node parent = name.getParentNode().orElse(null);
        if (parent instanceof Parameter) {
            Parameter p = (Parameter) parent;
            Range range = p.getName().getRange().orElse(null);
            String typeName = resolveTypeToClassName(cu, p.getTypeAsString());
            TypeRef type = typeName == null ? null : TypeRef.fromInternalName(typeName);
            return new SymbolRef(SymbolKind.VARIABLE, p.getNameAsString(), null, range, type);
        }
        if (parent instanceof VariableDeclarator) {
            VariableDeclarator vd = (VariableDeclarator) parent;
            Node varParent = vd.getParentNode().orElse(null);
            SymbolKind kind = varParent instanceof com.github.javaparser.ast.body.FieldDeclaration
                    ? SymbolKind.FIELD
                    : SymbolKind.VARIABLE;
            Range range = vd.getName().getRange().orElse(null);
            String typeName = resolveTypeToClassName(cu, vd.getType().asString());
            TypeRef type = typeName == null ? null : TypeRef.fromInternalName(typeName);
            return new SymbolRef(kind, vd.getNameAsString(), null, range, type);
        }
        Position usagePos = name.getBegin().orElse(null);
        SymbolRef local = resolveLocalDeclaration(cu, name.asString(), usagePos, name);
        if (local != null) {
            return local;
        }
        return resolveFieldDeclaration(cu, name.asString(), name);
    }

    SymbolRef resolveLocalDeclaration(CompilationUnit cu,
                                      String name,
                                      Position usagePos,
                                      Node context) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        Node scopeRoot = findScopeRoot(context);
        if (scopeRoot == null) {
            return null;
        }
        long usageKey = usagePos == null ? Long.MAX_VALUE : positionKey(usagePos);
        SymbolRef best = null;
        long bestKey = Long.MIN_VALUE;
        List<Parameter> params = scopeRoot.findAll(Parameter.class);
        for (Parameter param : params) {
            if (!name.equals(param.getNameAsString())) {
                continue;
            }
            if (isInsideExcludedScope(param, scopeRoot)) {
                continue;
            }
            Position pos = param.getBegin().orElse(null);
            if (pos == null) {
                continue;
            }
            long key = positionKey(pos);
            if (key > usageKey) {
                continue;
            }
            if (key > bestKey) {
                Range range = param.getName().getRange().orElse(null);
                String typeName = resolveTypeToClassName(cu, param.getTypeAsString());
                TypeRef type = typeName == null ? null : TypeRef.fromInternalName(typeName);
                best = new SymbolRef(SymbolKind.VARIABLE, name, null, range, type);
                bestKey = key;
            }
        }
        List<VariableDeclarator> vars = scopeRoot.findAll(VariableDeclarator.class);
        for (VariableDeclarator var : vars) {
            if (!name.equals(var.getNameAsString())) {
                continue;
            }
            if (isInsideExcludedScope(var, scopeRoot)) {
                continue;
            }
            if (!FlowAnalyzer.isInSameBranch(var, usagePos, scopeRoot)) {
                continue;
            }
            Node varParent = var.getParentNode().orElse(null);
            if (varParent instanceof com.github.javaparser.ast.body.FieldDeclaration) {
                continue;
            }
            Position pos = var.getBegin().orElse(null);
            if (pos == null) {
                continue;
            }
            long key = positionKey(pos);
            if (key > usageKey) {
                continue;
            }
            if (key > bestKey) {
                Range range = var.getName().getRange().orElse(null);
                String typeName = resolveTypeToClassName(cu, var.getType().asString());
                TypeRef type = typeName == null ? null : TypeRef.fromInternalName(typeName);
                best = new SymbolRef(SymbolKind.VARIABLE, name, null, range, type);
                bestKey = key;
            }
        }
        return best;
    }

    private TypeRef resolveLocalDeclaredType(CompilationUnit cu,
                                             String name,
                                             Position usagePos,
                                             Node context) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        Node scopeRoot = findScopeRoot(context);
        if (scopeRoot == null) {
            return null;
        }
        long usageKey = usagePos == null ? Long.MAX_VALUE : positionKey(usagePos);
        TypeRef best = null;
        long bestKey = Long.MIN_VALUE;
        List<Parameter> params = scopeRoot.findAll(Parameter.class);
        for (Parameter param : params) {
            if (!name.equals(param.getNameAsString())) {
                continue;
            }
            if (isInsideExcludedScope(param, scopeRoot)) {
                continue;
            }
            Position pos = param.getBegin().orElse(null);
            if (pos == null) {
                continue;
            }
            long key = positionKey(pos);
            if (key > usageKey) {
                continue;
            }
            if (key > bestKey) {
                TypeRef type = resolveTypeFromAstType(cu, param.getType());
                if (type != null) {
                    best = type;
                    bestKey = key;
                }
            }
        }
        List<VariableDeclarator> vars = scopeRoot.findAll(VariableDeclarator.class);
        for (VariableDeclarator var : vars) {
            if (!name.equals(var.getNameAsString())) {
                continue;
            }
            if (isInsideExcludedScope(var, scopeRoot)) {
                continue;
            }
            Node varParent = var.getParentNode().orElse(null);
            if (varParent instanceof com.github.javaparser.ast.body.FieldDeclaration) {
                continue;
            }
            Position pos = var.getBegin().orElse(null);
            if (pos == null) {
                continue;
            }
            long key = positionKey(pos);
            if (key > usageKey) {
                continue;
            }
            if (key > bestKey) {
                TypeRef type = resolveTypeFromAstType(cu, var.getType());
                if (type != null) {
                    best = type;
                    bestKey = key;
                }
            }
        }
        return best;
    }

    private TypeRef resolveLocalAssignedType(CompilationUnit cu,
                                             String name,
                                             Position usagePos,
                                             Node context,
                                             CallContext ctx) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        Node scopeRoot = findScopeRoot(context);
        if (scopeRoot == null) {
            return null;
        }
        long usageKey = usagePos == null ? Long.MAX_VALUE : positionKey(usagePos);
        TypeRef best = null;
        long bestKey = Long.MIN_VALUE;
        List<VariableDeclarator> vars = scopeRoot.findAll(VariableDeclarator.class);
        for (VariableDeclarator var : vars) {
            if (!name.equals(var.getNameAsString())) {
                continue;
            }
            if (isInsideExcludedScope(var, scopeRoot)) {
                continue;
            }
            Node varParent = var.getParentNode().orElse(null);
            if (varParent instanceof com.github.javaparser.ast.body.FieldDeclaration) {
                continue;
            }
            Position pos = var.getBegin().orElse(null);
            if (pos == null) {
                continue;
            }
            long key = positionKey(pos);
            if (key > usageKey) {
                continue;
            }
            TypeRef declared = resolveTypeFromAstType(cu, var.getType());
            if (declared != null) {
                continue;
            }
            if (!var.getInitializer().isPresent()) {
                continue;
            }
            Expression init = var.getInitializer().get();
            if (init instanceof NameExpr && name.equals(((NameExpr) init).getNameAsString())) {
                continue;
            }
            TypeRef inferred = resolveExpressionType(cu, init, ctx);
            if (inferred != null && key > bestKey) {
                best = inferred;
                bestKey = key;
            }
        }
        List<AssignExpr> assigns = scopeRoot.findAll(AssignExpr.class);
        for (AssignExpr assign : assigns) {
            if (assign == null) {
                continue;
            }
            if (isInsideExcludedScope(assign, scopeRoot)) {
                continue;
            }
            if (!FlowAnalyzer.isInSameBranch(assign, usagePos, scopeRoot)) {
                continue;
            }
            Expression target = unwrapEnclosed(assign.getTarget());
            if (!(target instanceof NameExpr)) {
                continue;
            }
            String varName = ((NameExpr) target).getNameAsString();
            if (!name.equals(varName)) {
                continue;
            }
            Position pos = assign.getBegin().orElse(null);
            if (pos == null) {
                continue;
            }
            long key = positionKey(pos);
            if (key > usageKey) {
                continue;
            }
            Expression value = assign.getValue();
            if (value instanceof NameExpr && name.equals(((NameExpr) value).getNameAsString())) {
                continue;
            }
            TypeRef inferred = resolveExpressionType(cu, value, ctx);
            if (inferred != null && key > bestKey) {
                best = inferred;
                bestKey = key;
            }
        }
        return best;
    }

    private TypeRef resolveNarrowedTypeFromConditions(CompilationUnit cu,
                                                      String name,
                                                      Node context) {
        if (name == null || name.trim().isEmpty() || context == null) {
            return null;
        }
        Node cur = context;
        while (cur != null) {
            Node parent = cur.getParentNode().orElse(null);
            if (parent instanceof com.github.javaparser.ast.stmt.IfStmt) {
                com.github.javaparser.ast.stmt.IfStmt ifs =
                        (com.github.javaparser.ast.stmt.IfStmt) parent;
                Node thenStmt = ifs.getThenStmt();
                Node elseStmt = ifs.getElseStmt().orElse(null);
                boolean inThen = FlowAnalyzer.isWithinNode(thenStmt, context);
                boolean inElse = elseStmt != null && FlowAnalyzer.isWithinNode(elseStmt, context);
                if (inThen) {
                    com.github.javaparser.ast.type.Type narrowedType =
                            FlowAnalyzer.findInstanceofType(ifs.getCondition(), name, true);
                    if (narrowedType != null) {
                        return resolveTypeFromAstType(cu, narrowedType);
                    }
                } else if (inElse) {
                    com.github.javaparser.ast.type.Type narrowedType =
                            FlowAnalyzer.findInstanceofType(ifs.getCondition(), name, false);
                    if (narrowedType != null) {
                        return resolveTypeFromAstType(cu, narrowedType);
                    }
                }
            }
            cur = parent;
        }
        return null;
    }

    private TypeRef resolveFieldTypeInAst(CompilationUnit cu,
                                          String name,
                                          Node context) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        Node typeNode = findEnclosingType(context);
        if (typeNode == null) {
            return null;
        }
        if (typeNode instanceof ClassOrInterfaceDeclaration) {
            ClassOrInterfaceDeclaration decl = (ClassOrInterfaceDeclaration) typeNode;
            return findFieldTypeInAst(decl.getFields(), cu, name);
        }
        if (typeNode instanceof EnumDeclaration) {
            EnumDeclaration decl = (EnumDeclaration) typeNode;
            return findFieldTypeInAst(decl.getFields(), cu, name);
        }
        if (typeNode instanceof RecordDeclaration) {
            RecordDeclaration decl = (RecordDeclaration) typeNode;
            return findFieldTypeInAst(decl.getFields(), cu, name);
        }
        return null;
    }

    SymbolRef resolveFieldDeclaration(CompilationUnit cu, String name, Node context) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        Node typeNode = findEnclosingType(context);
        if (typeNode instanceof ClassOrInterfaceDeclaration) {
            ClassOrInterfaceDeclaration decl = (ClassOrInterfaceDeclaration) typeNode;
            return findFieldIn(decl.getFields(), cu, name);
        }
        if (typeNode instanceof EnumDeclaration) {
            EnumDeclaration decl = (EnumDeclaration) typeNode;
            return findFieldIn(decl.getFields(), cu, name);
        }
        if (typeNode instanceof RecordDeclaration) {
            RecordDeclaration decl = (RecordDeclaration) typeNode;
            return findFieldIn(decl.getFields(), cu, name);
        }
        String className = resolveEnclosingClassName(cu, context);
        TypeRef dbType = resolveFieldTypeFromDb(className, name);
        if (dbType != null && dbType.internalName != null) {
            return new SymbolRef(SymbolKind.FIELD, name, null, null, TypeRef.fromInternalName(dbType.internalName));
        }
        return null;
    }

    private Node findEnclosingType(Node node) {
        Node cur = node;
        while (cur != null) {
            if (cur instanceof ClassOrInterfaceDeclaration
                    || cur instanceof EnumDeclaration
                    || cur instanceof RecordDeclaration
                    || cur instanceof AnnotationDeclaration) {
                return cur;
            }
            cur = cur.getParentNode().orElse(null);
        }
        return null;
    }

    private SymbolRef findFieldIn(List<com.github.javaparser.ast.body.FieldDeclaration> fields,
                                  CompilationUnit cu,
                                  String name) {
        if (fields == null || fields.isEmpty()) {
            return null;
        }
        for (com.github.javaparser.ast.body.FieldDeclaration field : fields) {
            for (VariableDeclarator var : field.getVariables()) {
                if (!name.equals(var.getNameAsString())) {
                    continue;
                }
                Range range = var.getName().getRange().orElse(null);
                String typeName = resolveTypeToClassName(cu, var.getType().asString());
                TypeRef type = typeName == null ? null : TypeRef.fromInternalName(typeName);
                return new SymbolRef(SymbolKind.FIELD, name, null, range, type);
            }
        }
        return null;
    }

    private TypeRef findFieldTypeInAst(List<com.github.javaparser.ast.body.FieldDeclaration> fields,
                                       CompilationUnit cu,
                                       String name) {
        if (fields == null || fields.isEmpty()) {
            return null;
        }
        for (com.github.javaparser.ast.body.FieldDeclaration field : fields) {
            for (VariableDeclarator var : field.getVariables()) {
                if (!name.equals(var.getNameAsString())) {
                    continue;
                }
                return resolveTypeFromAstType(cu, var.getType());
            }
        }
        return null;
    }

    String resolveTypeToClassName(CompilationUnit cu, String typeName) {
        String normalized = normalizeTypeName(typeName);
        if (normalized == null) {
            return null;
        }
        return resolveQualifiedClassName(cu, normalized);
    }

    private String normalizeTypeName(String typeName) {
        if (typeName == null) {
            return null;
        }
        String trimmed = typeName.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        int genericIndex = trimmed.indexOf('<');
        if (genericIndex >= 0) {
            trimmed = trimmed.substring(0, genericIndex);
        }
        while (trimmed.endsWith("[]")) {
            trimmed = trimmed.substring(0, trimmed.length() - 2);
        }
        trimmed = trimmed.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if ("var".equals(trimmed)) {
            return null;
        }
        if (isPrimitiveType(trimmed)) {
            return null;
        }
        return trimmed;
    }

    private boolean isPrimitiveType(String typeName) {
        if (typeName == null) {
            return false;
        }
        switch (typeName) {
            case "boolean":
            case "byte":
            case "short":
            case "int":
            case "long":
            case "char":
            case "float":
            case "double":
            case "void":
                return true;
            default:
                return false;
        }
    }

    private long positionKey(Position pos) {
        if (pos == null) {
            return Long.MIN_VALUE;
        }
        return (((long) pos.line) << 32) | (pos.column & 0xffffffffL);
    }

    Node findScopeRoot(Node node) {
        Node cur = node;
        while (cur != null) {
            if (cur instanceof MethodDeclaration
                    || cur instanceof ConstructorDeclaration
                    || cur instanceof LambdaExpr
                    || cur instanceof InitializerDeclaration) {
                return cur;
            }
            cur = cur.getParentNode().orElse(null);
        }
        return null;
    }

    private boolean isInsideExcludedScope(Node node, Node scopeRoot) {
        Node cur = node;
        while (cur != null && cur != scopeRoot) {
            if (cur instanceof LambdaExpr) {
                return true;
            }
            if (cur instanceof ClassOrInterfaceDeclaration
                    || cur instanceof EnumDeclaration
                    || cur instanceof RecordDeclaration
                    || cur instanceof AnnotationDeclaration) {
                return true;
            }
            cur = cur.getParentNode().orElse(null);
        }
        return false;
    }

    String resolveQualifiedClassName(CompilationUnit cu, String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        String trimmed = name.trim();
        if (trimmed.contains("<")) {
            trimmed = trimmed.substring(0, trimmed.indexOf('<'));
        }
        if (trimmed.contains("/")) {
            return resolveFromIndex(cu, trimmed, null);
        }
        if (trimmed.contains(".")) {
            String candidate = trimmed.replace('.', '/');
            return resolveFromIndex(cu, candidate, null);
        }
        if (cu != null) {
            String fromImport = resolveFromImports(cu, trimmed);
            if (fromImport != null) {
                return resolveFromIndex(cu, fromImport, trimmed);
            }
            if (cu.getPackageDeclaration().isPresent()) {
                String pkg = cu.getPackageDeclaration().get().getNameAsString();
                if (pkg != null && !pkg.trim().isEmpty()) {
                    String candidate = (pkg + "." + trimmed).replace('.', '/');
                    String resolved = resolveFromIndex(cu, candidate, trimmed);
                    if (resolved != null) {
                        return resolved;
                    }
                }
            }
        }
        String fallback = resolveFromIndex(cu, null, trimmed);
        return fallback == null ? trimmed : fallback;
    }

    SymbolKind resolveClassKind(String className, SymbolKind fallback) {
        if (className == null || className.trim().isEmpty()) {
            return fallback;
        }
        if (engine == null) {
            return fallback;
        }
        String normalized = normalizeClassName(className);
        ClassResult result = engine.getClassByClass(normalized);
        if (result != null && result.getIsInterfaceInt() == 1) {
            return SymbolKind.INTERFACE;
        }
        return fallback;
    }

    private String resolveFromIndex(CompilationUnit cu, String candidate, String simpleName) {
        if (candidate != null && existsInDatabase(candidate)) {
            return candidate;
        }
        if (simpleName == null || simpleName.trim().isEmpty()) {
            return candidate;
        }
        if (engine == null) {
            return candidate;
        }
        List<String> matches = engine.includeClassByClassName(simpleName, true);
        if (matches == null || matches.isEmpty()) {
            return candidate;
        }
        String pkg = null;
        if (cu != null && cu.getPackageDeclaration().isPresent()) {
            pkg = cu.getPackageDeclaration().get().getNameAsString();
        }
        if (pkg != null && !pkg.trim().isEmpty()) {
            String pkgPrefix = pkg.replace('.', '/') + "/" + simpleName;
            for (String match : matches) {
                if (match.equals(pkgPrefix) || match.startsWith(pkgPrefix + "$")) {
                    return match;
                }
            }
        }
        if (matches.size() == 1) {
            return matches.get(0);
        }
        String shortest = matches.get(0);
        for (String match : matches) {
            if (match != null && match.length() < shortest.length()) {
                shortest = match;
            }
        }
        return shortest != null ? shortest : candidate;
    }

    private boolean existsInDatabase(String className) {
        if (className == null || className.trim().isEmpty()) {
            return false;
        }
        if (engine == null) {
            return true;
        }
        return engine.getClassByClass(className) != null;
    }

    private String resolveFromImports(CompilationUnit cu, String simpleName) {
        if (cu == null || simpleName == null) {
            return null;
        }
        List<ImportDeclaration> imports = cu.getImports();
        if (imports == null || imports.isEmpty()) {
            return null;
        }
        for (ImportDeclaration imp : imports) {
            if (imp.isStatic()) {
                continue;
            }
            String q = imp.getNameAsString();
            if (imp.isAsterisk()) {
                if (q != null && !q.trim().isEmpty()) {
                    return (q + "." + simpleName).replace('.', '/');
                }
            } else {
                if (q != null && q.endsWith("." + simpleName)) {
                    return q.replace('.', '/');
                }
            }
        }
        return null;
    }

    public String resolveStaticImportOwner(CompilationUnit cu, String memberName) {
        if (cu == null || memberName == null || memberName.trim().isEmpty()) {
            return null;
        }
        List<ImportDeclaration> imports = cu.getImports();
        if (imports == null || imports.isEmpty()) {
            return null;
        }
        String fallback = null;
        for (ImportDeclaration imp : imports) {
            if (imp == null || !imp.isStatic()) {
                continue;
            }
            String q = imp.getNameAsString();
            if (q == null || q.trim().isEmpty()) {
                continue;
            }
            if (imp.isAsterisk()) {
                String owner = q.replace('.', '/');
                if (engine != null) {
                    try {
                        if (engine.getMemberByClassAndName(owner, memberName) != null) {
                            return owner;
                        }
                        List<MethodResult> methods = engine.getMethod(owner, memberName, null);
                        if (methods != null && !methods.isEmpty()) {
                            return owner;
                        }
                    } catch (Exception ex) {
                        debugIgnored("TypeSolver resolveStaticImportMember", ex);
                    }
                }
                if (fallback == null) {
                    fallback = owner;
                }
                continue;
            }
            int lastDot = q.lastIndexOf('.');
            if (lastDot <= 0) {
                continue;
            }
            String member = q.substring(lastDot + 1);
            if (!memberName.equals(member)) {
                continue;
            }
            String owner = q.substring(0, lastDot);
            return owner.replace('.', '/');
        }
        return fallback;
    }

    boolean looksLikeClassName(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        char c = name.charAt(0);
        return Character.isUpperCase(c);
    }

    boolean looksLikeQualifiedClassName(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        String[] parts = text.split("\\.");
        if (parts.length < 2) {
            return false;
        }
        return looksLikeClassName(parts[parts.length - 1]);
    }

    String resolveEnclosingClassName(CompilationUnit cu, Node node) {
        List<String> names = new ArrayList<>();
        Node cur = node;
        while (cur != null) {
            if (cur instanceof ClassOrInterfaceDeclaration) {
                names.add(0, ((ClassOrInterfaceDeclaration) cur).getNameAsString());
            } else if (cur instanceof EnumDeclaration) {
                names.add(0, ((EnumDeclaration) cur).getNameAsString());
            } else if (cur instanceof RecordDeclaration) {
                names.add(0, ((RecordDeclaration) cur).getNameAsString());
            } else if (cur instanceof AnnotationDeclaration) {
                names.add(0, ((AnnotationDeclaration) cur).getNameAsString());
            }
            cur = cur.getParentNode().orElse(null);
        }
        if (names.isEmpty()) {
            return inferClassNameFromAst(cu);
        }
        String pkg = null;
        if (cu != null && cu.getPackageDeclaration().isPresent()) {
            pkg = cu.getPackageDeclaration().get().getNameAsString();
        }
        StringBuilder sb = new StringBuilder();
        if (pkg != null && !pkg.trim().isEmpty()) {
            sb.append(pkg.replace('.', '/')).append("/");
        }
        sb.append(names.get(0));
        for (int i = 1; i < names.size(); i++) {
            sb.append("$").append(names.get(i));
        }
        return sb.toString();
    }

    private String inferClassNameFromAst(CompilationUnit cu) {
        if (cu == null) {
            return null;
        }
        return inferClassName(cu.toString());
    }

    String inferClassName(String code) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }
        String pkg = null;
        Matcher pkgMatcher = Pattern.compile("(?m)^\\s*package\\s+([\\w\\.]+)\\s*;").matcher(code);
        if (pkgMatcher.find()) {
            pkg = pkgMatcher.group(1);
        }
        Matcher clsMatcher = Pattern.compile(
                "(?m)^\\s*(?:public|protected|private|abstract|final|static|sealed|non-sealed|strictfp|\\s)*" +
                        "\\s*(?:class|interface|enum|record|@interface)\\s+([\\w$]+)")
                .matcher(code);
        if (clsMatcher.find()) {
            String name = clsMatcher.group(1);
            if (name == null || name.trim().isEmpty()) {
                return null;
            }
            if (pkg != null && !pkg.trim().isEmpty()) {
                return pkg.replace('.', '/') + "/" + name;
            }
            return name;
        }
        return null;
    }

    public List<TypeRef> resolveArgumentTypes(CompilationUnit cu,
                                              List<Expression> args,
                                              CallContext ctx) {
        if (args == null || args.isEmpty()) {
            return new ArrayList<>();
        }
        List<TypeRef> out = new ArrayList<>();
        for (Expression expr : args) {
            out.add(resolveExpressionType(cu, expr, ctx));
        }
        return out;
    }

    public List<TypeRef> resolveMethodReferenceParamTypes(CompilationUnit cu,
                                                          MethodReferenceExpr ref,
                                                          CallContext ctx) {
        TypeRef fiType = resolveFunctionalInterfaceTypeFromContext(cu, ref, ctx);
        MethodResult sam = null;
        if (fiType != null && fiType.internalName != null) {
            sam = resolveSamMethod(fiType.internalName);
        }
        List<TypeRef> params = new ArrayList<>();
        if (sam != null && sam.getMethodDesc() != null) {
            try {
                Type[] args = Type.getArgumentTypes(sam.getMethodDesc());
                for (Type t : args) {
                    if (t.getSort() == Type.OBJECT) {
                        params.add(TypeRef.fromInternalName(t.getInternalName()));
                    } else if (t.getSort() == Type.ARRAY) {
                        params.add(TypeRef.fromAsmType(t));
                    } else {
                        params.add(TypeRef.fromAsmType(t));
                    }
                }
            } catch (Exception ex) {
                debugIgnored("TypeSolver resolveMethodReferenceParamTypes", ex);
            }
        }
        if (fiType != null && sam != null && sam.getMethodDesc() != null) {
            List<GenericType> paramsSig = null;
            ClassSignatureCache cache = getClassSignatureCache(fiType.internalName);
            if (cache != null && cache.methodSignatures != null) {
                String signature = cache.methodSignatures.get(sam.getMethodName() + sam.getMethodDesc());
                if (signature != null) {
                    paramsSig = parseMethodParamSignatures(signature);
                }
            }
            if (paramsSig != null && !paramsSig.isEmpty()) {
                Map<String, String> bindings = buildGenericBindings(cache, fiType);
                for (int i = 0; i < paramsSig.size() && i < params.size(); i++) {
                    TypeRef resolved = resolveGenericType(paramsSig.get(i), bindings);
                    if (resolved != null) {
                        params.set(i, resolved);
                    }
                }
            }
        }
        if (ref != null && params != null && !params.isEmpty()) {
            String identifier = ref.getIdentifier();
            if (identifier != null && !"new".equals(identifier)) {
                Expression scope = ref.getScope();
                if (scope instanceof TypeExpr) {
                    String owner = resolveClassNameFromExpression(cu, scope, ref, ctx);
                    if (owner != null && engine != null) {
                        List<MethodResult> methods = engine.getMethod(owner, identifier, null);
                        if (methods != null && !methods.isEmpty() && !params.isEmpty()) {
                            boolean hasInstance = false;
                            boolean hasStaticExact = false;
                            int instanceArgCount = Math.max(0, params.size() - 1);
                            for (MethodResult method : methods) {
                                if (method == null || method.getMethodDesc() == null) {
                                    continue;
                                }
                                int count = argCountFromDesc(method.getMethodDesc());
                                if (method.getIsStaticInt() == 1 && count == params.size()) {
                                    hasStaticExact = true;
                                }
                                if (method.getIsStaticInt() == 0 && count == instanceArgCount) {
                                    hasInstance = true;
                                }
                            }
                            if (hasInstance && !hasStaticExact) {
                                params = new ArrayList<>(params.subList(1, params.size()));
                            }
                        }
                    }
                }
            }
        }
        return params;
    }

    TypeRef resolveTypeFromDescAndSignature(String desc, String signature) {
        if ((signature == null || signature.trim().isEmpty())
                && (desc == null || desc.trim().isEmpty())) {
            return null;
        }
        if (signature != null && !signature.trim().isEmpty()) {
            GenericType gt = parseTypeSignature(signature);
            if (gt != null) {
                return resolveGenericType(gt, null);
            }
        }
        if (desc != null && !desc.trim().isEmpty()) {
            try {
                Type type = Type.getType(desc);
                if (type.getSort() >= Type.BOOLEAN && type.getSort() <= Type.DOUBLE) {
                    return TypeRef.fromAsmType(type);
                }
                if (type.getSort() == Type.OBJECT) {
                    return TypeRef.fromInternalName(type.getInternalName());
                }
                if (type.getSort() == Type.ARRAY) {
                    return TypeRef.fromAsmType(type);
                }
            } catch (Exception ex) {
                debugIgnored("TypeSolver resolveTypeFromDescAndSignature", ex);
            }
        }
        return null;
    }

    TypeRef resolveLocalVarTypeFromDbType(String className,
                                          String methodName,
                                          String methodDesc,
                                          String varName,
                                          int line) {
        if (engine == null) {
            return null;
        }
        if (className == null || className.trim().isEmpty()) {
            return null;
        }
        className = normalizeClassName(className);
        if (varName == null || varName.trim().isEmpty()) {
            return null;
        }
        List<LocalVarEntity> vars = engine.getLocalVarsByMethod(
                className, methodName, methodDesc);
        if (vars == null || vars.isEmpty()) {
            return null;
        }
        LocalVarEntity best = null;
        int bestSpan = Integer.MAX_VALUE;
        for (LocalVarEntity var : vars) {
            if (var == null) {
                continue;
            }
            if (!varName.equals(var.getVarName())) {
                continue;
            }
            int start = var.getStartLine() == null ? -1 : var.getStartLine();
            int end = var.getEndLine() == null ? -1 : var.getEndLine();
            if (line > 0) {
                if (start > 0 && line < start) {
                    continue;
                }
                if (end > 0 && line > end) {
                    continue;
                }
            }
            int span = (start > 0 && end > 0) ? (end - start) : Integer.MAX_VALUE - 1;
            if (span < bestSpan) {
                best = var;
                bestSpan = span;
            }
        }
        if (best == null) {
            return null;
        }
        return resolveTypeFromDescAndSignature(best.getVarDesc(), best.getVarSignature());
    }

    public String resolveLocalVarTypeFromDb(String className,
                                            String methodName,
                                            String methodDesc,
                                            String varName,
                                            int line) {
        TypeRef type = resolveLocalVarTypeFromDbType(className, methodName, methodDesc, varName, line);
        return type == null ? null : type.internalName;
    }

    public TypeRef resolveFieldTypeFromDb(String className, String fieldName) {
        if (engine == null) {
            return null;
        }
        if (className == null || className.trim().isEmpty()) {
            return null;
        }
        if (fieldName == null || fieldName.trim().isEmpty()) {
            return null;
        }
        String normalized = normalizeClassName(className);
        MemberEntity member = null;
        String current = normalized;
        for (int i = 0; i < 6; i++) {
            member = engine.getMemberByClassAndName(current, fieldName);
            if (member != null) {
                break;
            }
            ClassResult result = engine.getClassByClass(current);
            if (result == null || result.getSuperClassName() == null
                    || result.getSuperClassName().trim().isEmpty()) {
                break;
            }
            current = result.getSuperClassName();
        }
        if (member == null) {
            return null;
        }
        if (member.getMethodDesc() != null) {
            TypeRef fromSig = resolveTypeFromDescAndSignature(
                    member.getMethodDesc(), member.getMethodSignature());
            if (fromSig != null) {
                return fromSig;
            }
        }
        if (member.getTypeClassName() != null && !member.getTypeClassName().trim().isEmpty()) {
            return TypeRef.fromInternalName(member.getTypeClassName());
        }
        return null;
    }

    public TypeRef resolveMethodReturnType(String owner,
                                           String methodName,
                                           String methodDesc,
                                           TypeRef scopeType) {
        return resolveMethodReturnType(owner, methodName, methodDesc, scopeType, null);
    }

    public TypeRef resolveMethodReturnType(String owner,
                                           String methodName,
                                           String methodDesc,
                                           TypeRef scopeType,
                                           List<TypeRef> argTypes) {
        if (owner == null || methodName == null || methodDesc == null) {
            return null;
        }
        String cacheKey = buildMethodReturnCacheKey(owner, methodName, methodDesc, scopeType, argTypes);
        TypeRef cached = getCachedMethodReturn(cacheKey);
        if (cached != null) {
            return cached;
        }
        String normalizedOwner = normalizeClassName(owner);
        ClassSignatureCache cache = getClassSignatureCache(normalizedOwner);
        String signature = null;
        if (cache != null && cache.methodSignatures != null) {
            signature = cache.methodSignatures.get(methodName + methodDesc);
        }
        TypeRef bindingScope = scopeType;
        if (bindingScope != null && bindingScope.internalName != null
                && !bindingScope.internalName.equals(normalizedOwner)) {
            bindingScope = null;
        }
        Map<String, String> classBindings = buildGenericBindings(cache, bindingScope);
        Map<String, String> methodBindings = signature == null
                ? null
                : buildMethodTypeBindings(signature, argTypes);
        Map<String, String> bindings = mergeBindings(classBindings, methodBindings);
        if (signature != null && !signature.trim().isEmpty()) {
            GenericType ret = parseMethodReturnSignature(signature);
            TypeRef resolved = resolveGenericType(ret, bindings);
            if (resolved != null) {
                cacheMethodReturn(cacheKey, resolved);
                return resolved;
            }
        }
        try {
            Type ret = Type.getReturnType(methodDesc);
            if (ret.getSort() >= Type.BOOLEAN && ret.getSort() <= Type.DOUBLE) {
                TypeRef resolved = TypeRef.fromAsmType(ret);
                cacheMethodReturn(cacheKey, resolved);
                return resolved;
            }
            if (ret.getSort() == Type.OBJECT) {
                TypeRef resolved = TypeRef.fromInternalName(ret.getInternalName());
                cacheMethodReturn(cacheKey, resolved);
                return resolved;
            }
            if (ret.getSort() == Type.ARRAY) {
                TypeRef resolved = TypeRef.fromAsmType(ret);
                cacheMethodReturn(cacheKey, resolved);
                return resolved;
            }
        } catch (Exception ex) {
            debugIgnored("TypeSolver resolveMethodReturnType", ex);
        }
        cacheMethodReturn(cacheKey, null);
        return null;
    }

    private String buildMethodReturnCacheKey(String owner,
                                             String methodName,
                                             String methodDesc,
                                             TypeRef scopeType,
                                             List<TypeRef> argTypes) {
        if (owner == null || methodName == null || methodDesc == null) {
            return null;
        }
        String normalized = normalizeClassName(owner);
        StringBuilder sb = new StringBuilder();
        sb.append(normalized).append('#').append(methodName).append(methodDesc);
        sb.append('|').append(encodeTypeRef(scopeType));
        sb.append('|');
        if (argTypes == null || argTypes.isEmpty()) {
            sb.append('-');
        } else {
            for (TypeRef arg : argTypes) {
                sb.append(encodeTypeRef(arg)).append(',');
            }
        }
        String stamp = buildClassStamp(normalized);
        if (stamp != null && !stamp.isEmpty()) {
            sb.append('|').append(stamp);
        }
        return sb.toString();
    }

    private TypeRef getCachedMethodReturn(String cacheKey) {
        ensureRootKey();
        if (cacheKey == null) {
            return null;
        }
        String cached = methodReturnCache.get(cacheKey);
        if (cached != null) {
            return decodeTypeRef(cached);
        }
        String value = DatabaseManager.getSemanticCacheValue(cacheKey, CACHE_TYPE_METHOD_RETURN);
        if (value != null) {
            methodReturnCache.put(cacheKey, value);
            return decodeTypeRef(value);
        }
        return null;
    }

    private void cacheMethodReturn(String cacheKey, TypeRef type) {
        if (cacheKey == null) {
            return;
        }
        String value = encodeTypeRef(type);
        methodReturnCache.put(cacheKey, value);
        DatabaseManager.putSemanticCacheValue(cacheKey, CACHE_TYPE_METHOD_RETURN, value);
    }

    private String encodeTypeRef(TypeRef type) {
        if (type == null) {
            return "";
        }
        Type asm = type.getAsmType();
        if (asm != null) {
            return asm.getDescriptor();
        }
        String name = type.getInternalName();
        if (name != null && !name.trim().isEmpty()) {
            return "L" + name + ";";
        }
        return "";
    }

    private TypeRef decodeTypeRef(String value) {
        if (value == null) {
            return null;
        }
        String text = value.trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            Type asm = Type.getType(text);
            return TypeRef.fromAsmType(asm);
        } catch (Exception ex) {
            debugIgnored("TypeSolver decodeTypeRef", ex);
        }
        if (text.contains(".")) {
            text = text.replace('.', '/');
        }
        if (text.contains("/")) {
            return TypeRef.fromInternalName(text);
        }
        return null;
    }

    ClassSignatureCache getClassSignatureCache(String className) {
        ensureRootKey();
        if (className == null || className.trim().isEmpty()) {
            return null;
        }
        String normalized = normalizeClassName(className);
        ClassSignatureCache cached = CLASS_SIGNATURE_CACHE.get(normalized);
        if (cached != null) {
            return cached;
        }
        ClassSignatureCache loaded = loadClassSignatureCache(normalized);
        if (loaded == null) {
            return null;
        }
        CLASS_SIGNATURE_CACHE.put(normalized, loaded);
        return loaded;
    }

    private ClassSignatureCache loadClassSignatureCache(String className) {
        String classPath = resolveClassPath(className);
        if (classPath == null || classPath.trim().isEmpty()) {
            return new ClassSignatureCache(null, null, new HashMap<>());
        }
        try {
            byte[] bytes = Files.readAllBytes(Paths.get(classPath));
            if (bytes == null || bytes.length == 0) {
                return new ClassSignatureCache(null, null, new HashMap<>());
            }
            ClassReader reader = new ClassReader(bytes);
            ClassNode node = new ClassNode();
            reader.accept(node, ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
            String signature = node.signature;
            List<String> typeParams = signature == null ? null : parseClassTypeParams(signature);
            Map<String, String> methodSigs = new HashMap<>();
            if (node.methods != null) {
                for (MethodNode mn : node.methods) {
                    if (mn == null || mn.signature == null) {
                        continue;
                    }
                    methodSigs.put(mn.name + mn.desc, mn.signature);
                }
            }
            return new ClassSignatureCache(signature, typeParams, methodSigs);
        } catch (Exception ex) {
            return new ClassSignatureCache(null, null, new HashMap<>());
        }
    }

    List<String> parseClassTypeParams(String signature) {
        if (signature == null || signature.trim().isEmpty()) {
            return null;
        }
        List<String> params = new ArrayList<>();
        try {
            SignatureReader reader = new SignatureReader(signature);
            reader.accept(new SignatureVisitor(Opcodes.ASM9) {
                @Override
                public void visitFormalTypeParameter(String name) {
                    if (name != null && !name.trim().isEmpty()) {
                        params.add(name);
                    }
                }
            });
        } catch (Exception ex) {
            debugIgnored("TypeSolver parseClassTypeParams", ex);
        }
        return params.isEmpty() ? null : params;
    }

    List<String> parseMethodTypeParams(String signature) {
        if (signature == null || signature.trim().isEmpty()) {
            return null;
        }
        List<String> params = new ArrayList<>();
        try {
            SignatureReader reader = new SignatureReader(signature);
            reader.accept(new SignatureVisitor(Opcodes.ASM9) {
                @Override
                public void visitFormalTypeParameter(String name) {
                    if (name != null && !name.trim().isEmpty()) {
                        params.add(name);
                    }
                }
            });
        } catch (Exception ex) {
            debugIgnored("TypeSolver parseMethodTypeParams", ex);
        }
        return params.isEmpty() ? null : params;
    }

    private GenericType parseTypeSignature(String signature) {
        if (signature == null || signature.trim().isEmpty()) {
            return null;
        }
        try {
            SignatureReader reader = new SignatureReader(signature);
            GenericType type = new GenericType();
            reader.acceptType(new TypeCapture(type));
            return type;
        } catch (Exception ex) {
            debugIgnored("TypeSolver parseTypeSignature", ex);
            return null;
        }
    }

    private GenericType parseMethodReturnSignature(String signature) {
        if (signature == null || signature.trim().isEmpty()) {
            return null;
        }
        try {
            GenericType returnType = new GenericType();
            SignatureReader reader = new SignatureReader(signature);
            reader.accept(new SignatureVisitor(Opcodes.ASM9) {
                @Override
                public SignatureVisitor visitReturnType() {
                    return new TypeCapture(returnType);
                }
            });
            return returnType;
        } catch (Exception ex) {
            debugIgnored("TypeSolver parseMethodReturnSignature", ex);
            return null;
        }
    }

    List<GenericType> parseMethodParamSignatures(String signature) {
        if (signature == null || signature.trim().isEmpty()) {
            return null;
        }
        List<GenericType> params = new ArrayList<>();
        try {
            SignatureReader reader = new SignatureReader(signature);
            reader.accept(new SignatureVisitor(Opcodes.ASM9) {
                @Override
                public SignatureVisitor visitParameterType() {
                    GenericType paramType = new GenericType();
                    params.add(paramType);
                    return new TypeCapture(paramType);
                }
            });
        } catch (Exception ex) {
            debugIgnored("TypeSolver parseMethodParamSignatures", ex);
            return null;
        }
        return params.isEmpty() ? null : params;
    }

    Map<String, String> buildMethodTypeBindings(String signature,
                                                List<TypeRef> argTypes) {
        if (signature == null || signature.trim().isEmpty()) {
            return null;
        }
        if (argTypes == null || argTypes.isEmpty()) {
            return null;
        }
        List<String> methodParams = parseMethodTypeParams(signature);
        if (methodParams == null || methodParams.isEmpty()) {
            return null;
        }
        List<GenericType> paramTypes = parseMethodParamSignatures(signature);
        if (paramTypes == null || paramTypes.isEmpty()) {
            return null;
        }
        int limit = Math.min(paramTypes.size(), argTypes.size());
        Map<String, String> bindings = new HashMap<>();
        for (int i = 0; i < limit; i++) {
            collectMethodTypeBindings(paramTypes.get(i), argTypes.get(i), methodParams, bindings);
        }
        return bindings.isEmpty() ? null : bindings;
    }

    Map<String, String> mergeBindings(Map<String, String> base,
                                      Map<String, String> extra) {
        if ((base == null || base.isEmpty()) && (extra == null || extra.isEmpty())) {
            return null;
        }
        Map<String, String> merged = new HashMap<>();
        if (base != null && !base.isEmpty()) {
            merged.putAll(base);
        }
        if (extra != null && !extra.isEmpty()) {
            merged.putAll(extra);
        }
        return merged.isEmpty() ? null : merged;
    }

    private void collectMethodTypeBindings(GenericType paramType,
                                           TypeRef argType,
                                           List<String> methodParams,
                                           Map<String, String> bindings) {
        if (paramType == null || argType == null || methodParams == null || methodParams.isEmpty()) {
            return;
        }
        if (paramType.typeVar != null) {
            if (!methodParams.contains(paramType.typeVar)) {
                return;
            }
            String bound = internalNameForBinding(argType);
            if (bound == null || bound.trim().isEmpty()) {
                return;
            }
            if (!bindings.containsKey(paramType.typeVar)) {
                bindings.put(paramType.typeVar, bound);
            }
            return;
        }
        if (paramType.typeArgs == null || paramType.typeArgs.isEmpty()) {
            return;
        }
        List<String> argTypeArgs = argType.typeArguments;
        if (argTypeArgs == null || argTypeArgs.isEmpty()) {
            return;
        }
        int limit = Math.min(paramType.typeArgs.size(), argTypeArgs.size());
        for (int i = 0; i < limit; i++) {
            String name = argTypeArgs.get(i);
            if (name == null || name.trim().isEmpty()) {
                continue;
            }
            collectMethodTypeBindings(paramType.typeArgs.get(i), TypeRef.fromInternalName(name),
                    methodParams, bindings);
        }
    }

    private String internalNameForBinding(TypeRef argType) {
        if (argType == null) {
            return null;
        }
        if (argType.internalName != null) {
            return argType.internalName;
        }
        if (argType.asmType == null) {
            return null;
        }
        int sort = argType.asmType.getSort();
        if (sort >= Type.BOOLEAN && sort <= Type.DOUBLE) {
            return wrapperInternalName(sort);
        }
        return null;
    }

    private String wrapperInternalName(int sort) {
        switch (sort) {
            case Type.BOOLEAN:
                return "java/lang/Boolean";
            case Type.BYTE:
                return "java/lang/Byte";
            case Type.CHAR:
                return "java/lang/Character";
            case Type.SHORT:
                return "java/lang/Short";
            case Type.INT:
                return "java/lang/Integer";
            case Type.LONG:
                return "java/lang/Long";
            case Type.FLOAT:
                return "java/lang/Float";
            case Type.DOUBLE:
                return "java/lang/Double";
            default:
                return null;
        }
    }

    private Integer primitiveForWrapper(String internalName) {
        if (internalName == null) {
            return null;
        }
        switch (internalName) {
            case "java/lang/Boolean":
                return Type.BOOLEAN;
            case "java/lang/Byte":
                return Type.BYTE;
            case "java/lang/Character":
                return Type.CHAR;
            case "java/lang/Short":
                return Type.SHORT;
            case "java/lang/Integer":
                return Type.INT;
            case "java/lang/Long":
                return Type.LONG;
            case "java/lang/Float":
                return Type.FLOAT;
            case "java/lang/Double":
                return Type.DOUBLE;
            default:
                return null;
        }
    }

    Map<String, String> buildGenericBindings(ClassSignatureCache cache,
                                             TypeRef scopeType) {
        if (cache == null || cache.typeParams == null || cache.typeParams.isEmpty()) {
            return null;
        }
        if (scopeType == null || scopeType.typeArguments == null || scopeType.typeArguments.isEmpty()) {
            return null;
        }
        Map<String, String> bindings = new HashMap<>();
        int limit = Math.min(cache.typeParams.size(), scopeType.typeArguments.size());
        for (int i = 0; i < limit; i++) {
            String param = cache.typeParams.get(i);
            String arg = scopeType.typeArguments.get(i);
            if (param != null && arg != null && !arg.trim().isEmpty()) {
                bindings.put(param, arg);
            }
        }
        return bindings.isEmpty() ? null : bindings;
    }

    TypeRef resolveGenericType(GenericType type, Map<String, String> bindings) {
        if (type == null) {
            return null;
        }
        if (type.wildcard == '*') {
            return TypeRef.fromInternalName("java/lang/Object");
        }
        if (type.typeVar != null) {
            if (bindings == null) {
                return null;
            }
            String bound = bindings.get(type.typeVar);
            if (bound == null || bound.trim().isEmpty()) {
                return null;
            }
            return TypeRef.fromInternalName(bound);
        }
        if (type.internalName == null || type.internalName.trim().isEmpty()) {
            return null;
        }
        List<String> args = null;
        if (type.typeArgs != null && !type.typeArgs.isEmpty()) {
            args = new ArrayList<>();
            for (GenericType arg : type.typeArgs) {
                TypeRef resolved = resolveGenericType(arg, bindings);
                if (resolved != null && resolved.internalName != null) {
                    args.add(resolved.internalName);
                }
            }
        }
        return TypeRef.fromInternalName(type.internalName, args == null || args.isEmpty() ? null : args);
    }

    int argCountFromDesc(String desc) {
        if (desc == null || desc.trim().isEmpty()) {
            return ARG_COUNT_UNKNOWN;
        }
        try {
            return Type.getArgumentTypes(desc).length;
        } catch (Exception ex) {
            debugIgnored("TypeSolver argCountFromDesc", ex);
            return ARG_COUNT_UNKNOWN;
        }
    }

    private boolean isStringType(TypeRef type) {
        if (type == null) {
            return false;
        }
        if (type.internalName != null) {
            return "java/lang/String".equals(type.internalName);
        }
        return false;
    }

    private HierarchyResolver getHierarchyResolver() {
        if (hierarchyResolver != null) {
            return hierarchyResolver;
        }
        if (engine == null) {
            return null;
        }
        hierarchyResolver = new HierarchyResolver(engine);
        return hierarchyResolver;
    }

    private String buildClassStamp(String className) {
        ensureRootKey();
        if (className == null) {
            return "";
        }
        String normalized = normalizeClassName(className);
        if (normalized == null) {
            return "";
        }
        String cached = classStampCache.get(normalized);
        if (cached != null) {
            return cached;
        }
        String stamp = computeClassStamp(normalized);
        classStampCache.put(normalized, stamp);
        return stamp;
    }

    private void ensureRootKey() {
        long rootSeq = RuntimeClassResolver.getRootSeq();
        String combined = rootSeq + "#" + DatabaseManager.getBuildSeq();
        if (!combined.equals(lastRootKey)) {
            methodReturnCache.clear();
            classStampCache.clear();
            CLASS_SIGNATURE_CACHE.clear();
            hierarchyResolver = null;
            lastRootKey = combined;
        }
    }

    private String computeClassStamp(String normalized) {
        if (engine != null) {
            Integer jarId = engine.getJarIdByClass(normalized);
            if (jarId != null) {
                return "jar:" + jarId;
            }
            String jarName = engine.getJarByClass(normalized);
            if (jarName != null && !jarName.trim().isEmpty()) {
                return "jar:" + jarName;
            }
        }
        RuntimeClassResolver.ResolvedClass resolved = RuntimeClassResolver.resolve(normalized);
        if (resolved != null) {
            String jarName = resolved.getJarName();
            Path classFile = resolved.getClassFile();
            if (classFile != null) {
                Path abs = classFile.toAbsolutePath().normalize();
                Path tempRoot = Paths.get(Const.tempDir).toAbsolutePath().normalize();
                if (jarName != null && !jarName.trim().isEmpty() && abs.startsWith(tempRoot)) {
                    return "jar:" + jarName;
                }
                try {
                    long ts = Files.getLastModifiedTime(classFile).toMillis();
                    return "file:" + ts;
                } catch (Exception ex) {
                    debugIgnored("TypeSolver buildClassStamp file ts", ex);
                }
            }
            if (jarName != null && !jarName.trim().isEmpty()) {
                return "jar:" + jarName;
            }
        }
        return "";
    }

    private Type widerPrimitiveType(Type left, Type right) {
        if (left == null || right == null) {
            return null;
        }
        int l = left.getSort();
        int r = right.getSort();
        int rank = Math.max(primitiveRank(l), primitiveRank(r));
        switch (rank) {
            case 6:
                return Type.DOUBLE_TYPE;
            case 5:
                return Type.FLOAT_TYPE;
            case 4:
                return Type.LONG_TYPE;
            case 3:
                return Type.INT_TYPE;
            case 2:
                return Type.CHAR_TYPE;
            case 1:
                return Type.SHORT_TYPE;
            case 0:
                return Type.BYTE_TYPE;
            default:
                return null;
        }
    }

    private int primitiveRank(int sort) {
        switch (sort) {
            case Type.DOUBLE:
                return 6;
            case Type.FLOAT:
                return 5;
            case Type.LONG:
                return 4;
            case Type.INT:
                return 3;
            case Type.CHAR:
                return 2;
            case Type.SHORT:
                return 1;
            case Type.BYTE:
                return 0;
            default:
                return -1;
        }
    }

    private static Expression unwrapEnclosed(Expression expr) {
        Expression cur = expr;
        while (cur instanceof EnclosedExpr) {
            cur = ((EnclosedExpr) cur).getInner();
        }
        return cur;
    }

    private String resolveClassNameFromExpression(CompilationUnit cu,
                                                  Expression expr,
                                                  Node context) {
        return resolveClassNameFromExpression(cu, expr, context, null);
    }

    private String resolveClassNameFromExpression(CompilationUnit cu,
                                                  Expression expr,
                                                  Node context,
                                                  CallContext ctx) {
        TypeRef resolved = resolveExpressionType(cu, expr, ctx);
        if (resolved != null && resolved.internalName != null) {
            return resolved.internalName;
        }
        return null;
    }

    private String normalizeClassName(String className) {
        if (className == null) {
            return null;
        }
        String normalized = className.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        if (normalized.endsWith(".class")) {
            normalized = normalized.substring(0, normalized.length() - 6);
        }
        if (normalized.contains(".")) {
            normalized = normalized.replace('.', '/');
        }
        return normalized;
    }

    private String resolveClassPath(String className) {
        if (className == null || className.trim().isEmpty()) {
            return null;
        }
        String normalized = normalizeClassName(className);
        if (engine != null) {
            String path = engine.getAbsPath(normalized);
            if (path != null && Files.exists(Paths.get(path))) {
                return path;
            }
        }
        String tempPath = resolveTempClassPath(normalized);
        if (tempPath != null) {
            return tempPath;
        }
        RuntimeClassResolver.ResolvedClass resolved = RuntimeClassResolver.resolve(normalized);
        if (resolved != null && resolved.getClassFile() != null) {
            return resolved.getClassFile().toAbsolutePath().toString();
        }
        return null;
    }

    private String resolveTempClassPath(String className) {
        if (className == null || className.trim().isEmpty()) {
            return null;
        }
        Path resolved = JarUtil.resolveClassFileInTemp(className);
        return resolved == null ? null : resolved.toString();
    }

    static final class GenericType {
        private String internalName;
        private String typeVar;
        private List<GenericType> typeArgs;
        private char wildcard;
    }

    private static final class TypeCapture extends SignatureVisitor {
        private final GenericType target;

        private TypeCapture(GenericType target) {
            super(Opcodes.ASM9);
            this.target = target;
        }

        @Override
        public void visitClassType(String name) {
            target.internalName = name;
        }

        @Override
        public void visitInnerClassType(String name) {
            if (target.internalName == null || target.internalName.trim().isEmpty()) {
                target.internalName = name;
            } else {
                target.internalName = target.internalName + "$" + name;
            }
        }

        @Override
        public void visitTypeVariable(String name) {
            target.typeVar = name;
        }

        @Override
        public SignatureVisitor visitTypeArgument(char wildcard) {
            if (target.typeArgs == null) {
                target.typeArgs = new ArrayList<>();
            }
            GenericType arg = new GenericType();
            arg.wildcard = wildcard;
            target.typeArgs.add(arg);
            return new TypeCapture(arg);
        }
    }

    static final class ClassSignatureCache {
        final String classSignature;
        final List<String> typeParams;
        final Map<String, String> methodSignatures;

        private ClassSignatureCache(String classSignature,
                                    List<String> typeParams,
                                    Map<String, String> methodSignatures) {
            this.classSignature = classSignature;
            this.typeParams = typeParams;
            this.methodSignatures = methodSignatures;
        }
    }
}
