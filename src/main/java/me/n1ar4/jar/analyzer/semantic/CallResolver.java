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
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.InitializerDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.TypeExpr;
import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.engine.CoreEngine;
import me.n1ar4.jar.analyzer.entity.CallSiteEntity;
import me.n1ar4.jar.analyzer.entity.MethodResult;
import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.jar.analyzer.utils.RuntimeClassResolver;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class CallResolver {
    private static final String CACHE_TYPE_METHOD_DESC = "method-desc";

    private final CoreEngine engine;
    private final TypeSolver typeSolver;
    private LineMapper lineMapper;
    private volatile HierarchyResolver hierarchyResolver;
    private final ConcurrentHashMap<String, String> methodDescCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> classStampCache = new ConcurrentHashMap<>();
    private volatile String lastRootKey = "";

    public CallResolver(CoreEngine engine, TypeSolver typeSolver) {
        this.engine = engine;
        this.typeSolver = typeSolver;
    }

    void setLineMapper(LineMapper mapper) {
        this.lineMapper = mapper;
    }

    public String resolveScopeClassName(CompilationUnit cu, MethodCallExpr call, CallContext ctx) {
        if (call == null) {
            return null;
        }
        if (!call.getScope().isPresent()) {
            if (typeSolver != null) {
                String staticOwner = typeSolver.resolveStaticImportOwner(cu, call.getNameAsString());
                if (staticOwner != null) {
                    return staticOwner;
                }
                return typeSolver.resolveEnclosingClassName(cu, call);
            }
            return null;
        }
        Expression scope = unwrapEnclosed(call.getScope().get());
        if (typeSolver == null) {
            return null;
        }
        TypeRef resolved = typeSolver.resolveExpressionType(cu, scope, ctx);
        return resolved == null ? null : resolved.internalName;
    }

    public boolean isStaticScope(CompilationUnit cu, MethodCallExpr call, CallContext ctx) {
        if (call == null || !call.getScope().isPresent()) {
            if (typeSolver == null) {
                return false;
            }
            String staticOwner = typeSolver.resolveStaticImportOwner(cu, call == null ? null : call.getNameAsString());
            return staticOwner != null;
        }
        Expression scope = unwrapEnclosed(call.getScope().get());
        if (scope instanceof TypeExpr) {
            return true;
        }
        if (scope instanceof NameExpr && typeSolver != null) {
            String name = ((NameExpr) scope).getNameAsString();
            if (typeSolver.looksLikeClassName(name)) {
                String cls = typeSolver.resolveQualifiedClassName(cu, name);
                return cls != null;
            }
        }
        if (typeSolver == null) {
            return false;
        }
        TypeRef resolved = typeSolver.resolveExpressionType(cu, scope, ctx);
        return resolved != null && resolved.internalName != null
                && typeSolver.looksLikeClassName(
                resolved.internalName.substring(resolved.internalName.lastIndexOf('/') + 1));
    }

    public boolean preferStaticForMethodRef(MethodReferenceExpr ref,
                                            String ownerClass,
                                            int argCount) {
        if (ref == null || ownerClass == null || engine == null) {
            return false;
        }
        Expression scope = ref.getScope();
        if (!(scope instanceof TypeExpr)) {
            return false;
        }
        String name = ref.getIdentifier();
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        List<MethodResult> methods = engine.getMethod(ownerClass, name, null);
        if (methods == null || methods.isEmpty()) {
            return false;
        }
        boolean hasStatic = false;
        boolean hasInstance = false;
        for (MethodResult method : methods) {
            if (method == null || method.getMethodDesc() == null) {
                continue;
            }
            int count = argCountFromDesc(method.getMethodDesc());
            if (count != argCount) {
                continue;
            }
            if (method.getIsStaticInt() == 1) {
                hasStatic = true;
            } else {
                hasInstance = true;
            }
        }
        if (hasStatic && !hasInstance) {
            return true;
        }
        if (hasInstance && !hasStatic) {
            return false;
        }
        return false;
    }

    public String resolveMethodDescByArgCount(String className, String methodName, int argCount) {
        if (engine == null) {
            return null;
        }
        if (className == null || className.trim().isEmpty()) {
            return null;
        }
        if (methodName == null || methodName.trim().isEmpty()) {
            return null;
        }
        String normalized = normalizeClassName(className);
        String cacheKey = buildMethodDescCacheKeyByCount(normalized, methodName, argCount);
        String cached = getCachedMethodDesc(cacheKey);
        if (cached != null) {
            return cached.isEmpty() ? null : cached;
        }
        List<MethodResult> candidates = engine.getMethod(normalized, methodName, null);
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        List<MethodResult> filtered = filterByArgCount(candidates, argCount);
        List<MethodResult> pool = (filtered != null && !filtered.isEmpty())
                ? filtered
                : candidates;
        if (pool.size() == 1) {
            String desc = pool.get(0).getMethodDesc();
            cacheMethodDesc(cacheKey, desc);
            return desc;
        }
        cacheMethodDesc(cacheKey, null);
        return null;
    }

    public String resolveMethodDescByArgTypes(String className,
                                              String methodName,
                                              List<TypeRef> argTypes,
                                              TypeRef scopeType,
                                              boolean preferStatic) {
        if (engine == null) {
            return null;
        }
        if (className == null || className.trim().isEmpty()) {
            return null;
        }
        if (methodName == null || methodName.trim().isEmpty()) {
            return null;
        }
        String normalized = normalizeClassName(className);
        String cacheKey = buildMethodDescCacheKey(normalized, methodName, argTypes, scopeType, preferStatic);
        String cached = getCachedMethodDesc(cacheKey);
        if (cached != null) {
            return cached.isEmpty() ? null : cached;
        }
        List<MethodResult> candidates = engine.getMethod(normalized, methodName, null);
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        List<MethodCandidate> methodCandidates = buildMethodCandidates(
                normalized, candidates, scopeType, argTypes);
        if (methodCandidates.isEmpty()) {
            return null;
        }
        HierarchyResolver hierarchy = getHierarchyResolver();
        MethodCandidate best = OverloadResolver.pickBest(
                methodCandidates, argTypes, preferStatic, hierarchy);
        String desc = best == null ? null : best.getMethodDesc();
        cacheMethodDesc(cacheKey, desc);
        return desc;
    }

    public String resolveMethodDescByCall(CompilationUnit cu,
                                          String className,
                                          MethodCallExpr call,
                                          CallContext ctx,
                                          TypeRef scopeType,
                                          boolean preferStatic) {
        if (engine == null) {
            return null;
        }
        if (className == null || className.trim().isEmpty()) {
            return null;
        }
        if (call == null) {
            return null;
        }
        String methodName = call.getNameAsString();
        if (methodName == null || methodName.trim().isEmpty()) {
            return null;
        }
        String normalized = normalizeClassName(className);
        List<MethodResult> candidates = engine.getMethod(normalized, methodName, null);
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        List<CallArgument> args = buildCallArguments(cu, call.getArguments(), ctx);
        List<TypeRef> argTypes = extractArgumentTypes(args);
        String cacheKey = buildMethodDescCacheKey(normalized, methodName, argTypes, scopeType, preferStatic);
        String cached = getCachedMethodDesc(cacheKey);
        if (cached != null) {
            return cached.isEmpty() ? null : cached;
        }
        List<MethodCandidate> methodCandidates = buildMethodCandidates(
                normalized, candidates, scopeType, argTypes);
        if (methodCandidates.isEmpty()) {
            return null;
        }
        HierarchyResolver hierarchy = getHierarchyResolver();
        MethodCandidate best = OverloadResolver.pickBest(
                methodCandidates, args, preferStatic, hierarchy, typeSolver, cu, ctx);
        String desc = best == null ? null : best.getMethodDesc();
        cacheMethodDesc(cacheKey, desc);
        return desc;
    }

    public CallPosition resolveCallPosition(CompilationUnit cu, Position pos, CallContext ctx) {
        if (cu == null || pos == null) {
            return null;
        }
        MethodCallExpr call = findMethodCallAt(cu, pos);
        if (call == null) {
            return null;
        }
        return resolveCallPosition(cu, call, ctx);
    }

    public CallPosition resolveCallPosition(CompilationUnit cu, MethodCallExpr call, CallContext ctx) {
        if (cu == null || call == null) {
            return null;
        }
        Node scopeRoot = typeSolver == null ? null : typeSolver.findScopeRoot(call);
        if (scopeRoot == null) {
            return null;
        }
        boolean inLambda = scopeRoot instanceof LambdaExpr;
        List<MethodCallExpr> calls = collectMethodCalls(scopeRoot);
        int line = call.getBegin().map(p -> p.line).orElse(-1);
        int mappedLine = line;
        if (line > 0) {
            String className = ctx == null ? null : ctx.className;
            String methodName = ctx == null ? null : ctx.methodName;
            String methodDesc = ctx == null ? null : ctx.methodDesc;
            int argCount = ctx == null ? TypeSolver.ARG_COUNT_UNKNOWN : ctx.methodArgCount;
            if ((className == null || className.trim().isEmpty()) && typeSolver != null) {
                className = typeSolver.resolveEnclosingClassName(cu, call);
            }
            if ((methodName == null || methodName.trim().isEmpty()) && typeSolver != null) {
                EnclosingCallable enclosing = resolveEnclosingCallable(cu, call.getBegin().orElse(null));
                if (enclosing != null) {
                    if (className == null || className.trim().isEmpty()) {
                        className = enclosing.className;
                    }
                    methodName = enclosing.methodName;
                    if (argCount == TypeSolver.ARG_COUNT_UNKNOWN) {
                        argCount = enclosing.argCount;
                    }
                }
            }
            if (className != null && !className.trim().isEmpty()) {
                className = normalizeClassName(className);
            }
            if ((methodDesc == null || methodDesc.trim().isEmpty())
                    && className != null && methodName != null) {
                methodDesc = resolveMethodDescByArgCount(className, methodName, argCount);
            }
            if (lineMapper != null) {
                mappedLine = lineMapper.mapLineNumber(className, methodName, methodDesc, argCount, line);
            }
        }
        if (calls.isEmpty()) {
            return new CallPosition(call, -1, inLambda, mappedLine);
        }
        calls.sort((a, b) -> {
            Range ra = a.getRange().orElse(null);
            Range rb = b.getRange().orElse(null);
            long ka = ra == null ? Long.MAX_VALUE : rangeBeginKey(ra);
            long kb = rb == null ? Long.MAX_VALUE : rangeBeginKey(rb);
            return Long.compare(ka, kb);
        });
        int index = -1;
        for (int i = 0; i < calls.size(); i++) {
            MethodCallExpr item = calls.get(i);
            if (item == call) {
                index = i;
                break;
            }
            Range r1 = item.getRange().orElse(null);
            Range r2 = call.getRange().orElse(null);
            if (r1 != null && r2 != null && r1.equals(r2)) {
                index = i;
                break;
            }
        }
        return new CallPosition(call, index, inLambda, mappedLine);
    }

    public MethodCallExpr findMethodCallAt(CompilationUnit cu, Position pos) {
        if (cu == null || pos == null) {
            return null;
        }
        List<MethodCallExpr> calls = cu.findAll(MethodCallExpr.class);
        MethodCallExpr best = null;
        long bestSpan = Long.MAX_VALUE;
        for (MethodCallExpr call : calls) {
            if (!call.getName().getRange().isPresent()) {
                continue;
            }
            Range range = call.getName().getRange().get();
            if (!contains(range, pos)) {
                continue;
            }
            long span = rangeSpan(range);
            if (span < bestSpan) {
                best = call;
                bestSpan = span;
            }
        }
        return best;
    }

    public MethodReferenceExpr findMethodReferenceAt(CompilationUnit cu, Position pos) {
        if (cu == null || pos == null) {
            return null;
        }
        List<MethodReferenceExpr> refs = cu.findAll(MethodReferenceExpr.class);
        MethodReferenceExpr best = null;
        long bestSpan = Long.MAX_VALUE;
        for (MethodReferenceExpr ref : refs) {
            Range range = ref.getRange().orElse(null);
            if (range == null || !contains(range, pos)) {
                continue;
            }
            long span = rangeSpan(range);
            if (span < bestSpan) {
                best = ref;
                bestSpan = span;
            }
        }
        return best;
    }

    public String resolveScopeHint(CallContext ctx, SymbolRef target) {
        if (target != null && target.getOwner() != null && !target.getOwner().trim().isEmpty()) {
            return target.getOwner();
        }
        if (ctx == null || ctx.cu == null || ctx.position == null) {
            return null;
        }
        MethodCallExpr callExpr = findMethodCallAt(ctx.cu, ctx.position);
        if (callExpr == null) {
            return null;
        }
        String scopeHint = resolveScopeClassName(ctx.cu, callExpr, ctx);
        if (scopeHint != null && !scopeHint.trim().isEmpty()) {
            return scopeHint;
        }
        if (callExpr.getScope().isPresent() && typeSolver != null) {
            Expression scope = unwrapEnclosed(callExpr.getScope().get());
            TypeRef resolved = typeSolver.resolveExpressionType(ctx.cu, scope, ctx);
            if (resolved != null && resolved.internalName != null) {
                return resolved.internalName;
            }
        }
        return null;
    }

    public CallSiteSelection resolveCallSiteTarget(CallContext ctx,
                                                   String methodName,
                                                   int argCount,
                                                   String scopeHint,
                                                   CallPosition callPosition) {
        if (ctx == null || engine == null) {
            return null;
        }
        if (methodName == null || methodName.trim().isEmpty()) {
            return null;
        }
        String callerClass = ctx.className;
        if (callerClass == null || callerClass.trim().isEmpty()) {
            return null;
        }
        if (ctx.methodName == null || ctx.methodName.trim().isEmpty()) {
            return null;
        }
        callerClass = normalizeClassName(callerClass);
        if (callPosition != null && callPosition.inLambda) {
            return resolveLambdaCallSiteTarget(ctx, callerClass, methodName, argCount, scopeHint, callPosition);
        }
        List<CallSiteEntity> sites = engine
                .getCallSitesByCaller(callerClass, ctx.methodName, ctx.methodDesc);
        if ((sites == null || sites.isEmpty()) && ctx.methodDesc != null) {
            sites = engine.getCallSitesByCaller(callerClass, ctx.methodName, null);
        }
        if (sites == null || sites.isEmpty()) {
            return null;
        }
        int callIndex = callPosition == null ? -1 : callPosition.callIndex;
        int line = ctx.line;
        if (callPosition != null && callPosition.line > 0) {
            line = callPosition.line;
        }
        CallSiteEntity picked = pickBestCallSite(
                sites, methodName, argCount, line, callIndex, scopeHint);
        if (picked == null) {
            return null;
        }
        String className = chooseCallSiteClass(picked, scopeHint);
        if (className == null || className.trim().isEmpty()) {
            return null;
        }
        return new CallSiteSelection(className, picked.getCalleeMethodName(), picked.getCalleeMethodDesc());
    }

    private CallSiteSelection resolveLambdaCallSiteTarget(CallContext ctx,
                                                          String callerClass,
                                                          String methodName,
                                                          int argCount,
                                                          String scopeHint,
                                                          CallPosition callPosition) {
        if (ctx == null || engine == null) {
            return null;
        }
        if (callerClass == null || callerClass.trim().isEmpty()) {
            return null;
        }
        List<CallSiteEntity> sites = engine.getCallSitesByCaller(callerClass, null, null);
        if (sites == null || sites.isEmpty()) {
            return null;
        }
        List<CallSiteEntity> lambdaSites = new ArrayList<>();
        for (CallSiteEntity site : sites) {
            if (site == null || site.getCallerMethodName() == null) {
                continue;
            }
            if (site.getCallerMethodName().startsWith("lambda$")) {
                lambdaSites.add(site);
            }
        }
        if (lambdaSites.isEmpty()) {
            return null;
        }
        int callIndex = callPosition == null ? -1 : callPosition.callIndex;
        int line = ctx.line;
        if (callPosition != null && callPosition.line > 0) {
            line = callPosition.line;
        }
        CallSiteEntity picked = pickBestCallSite(
                lambdaSites, methodName, argCount, line, callIndex, scopeHint);
        if (picked == null) {
            return null;
        }
        String className = chooseCallSiteClass(picked, scopeHint);
        if (className == null || className.trim().isEmpty()) {
            return null;
        }
        return new CallSiteSelection(className, picked.getCalleeMethodName(), picked.getCalleeMethodDesc());
    }

    private CallSiteEntity pickBestCallSite(List<CallSiteEntity> sites,
                                            String methodName,
                                            int argCount,
                                            int line,
                                            int callIndex,
                                            String scopeHint) {
        if (sites == null || sites.isEmpty()) {
            return null;
        }
        String normalizedScope = scopeHint == null ? null : normalizeClassName(scopeHint);
        CallSiteEntity best = null;
        int bestScore = Integer.MIN_VALUE;
        for (CallSiteEntity site : sites) {
            if (site == null) {
                continue;
            }
            if (methodName != null && !methodName.equals(site.getCalleeMethodName())) {
                continue;
            }
            int score = 0;
            Integer lineNumber = site.getLineNumber();
            if (lineNumber != null && lineNumber > 0 && line > 0) {
                int diff = Math.abs(lineNumber - line);
                if (diff == 0) {
                    score += 6;
                } else if (diff == 1) {
                    score += 4;
                } else if (diff <= 3) {
                    score += 2;
                } else if (diff <= 5) {
                    score += 1;
                } else {
                    score -= 2;
                }
            }
            if (callIndex >= 0) {
                Integer siteIndex = site.getCallIndex();
                if (siteIndex != null && siteIndex >= 0) {
                    int diff = Math.abs(siteIndex - callIndex);
                    if (diff == 0) {
                        score += 6;
                    } else if (diff == 1) {
                        score += 4;
                    } else if (diff <= 2) {
                        score += 2;
                    } else if (diff <= 4) {
                        score += 1;
                    } else {
                        score -= 2;
                    }
                }
            }
            if (argCount != TypeSolver.ARG_COUNT_UNKNOWN) {
                int siteArgs = argCountFromDesc(site.getCalleeMethodDesc());
                if (siteArgs == argCount) {
                    score += 3;
                } else if (siteArgs >= 0) {
                    score -= 2;
                }
            }
            if (normalizedScope != null) {
                String owner = site.getCalleeOwner();
                String receiver = site.getReceiverType();
                if (normalizedScope.equals(owner)) {
                    score += 3;
                }
                if (receiver != null && normalizedScope.equals(receiver)) {
                    score += 5;
                }
            }
            if (score > bestScore) {
                best = site;
                bestScore = score;
            }
        }
        if (bestScore < 3) {
            return null;
        }
        return best;
    }

    private String chooseCallSiteClass(CallSiteEntity site, String scopeHint) {
        if (site == null) {
            return null;
        }
        String owner = site.getCalleeOwner();
        String receiver = site.getReceiverType();
        String normalizedScope = scopeHint == null ? null : normalizeClassName(scopeHint);
        if (normalizedScope != null) {
            if (receiver != null && normalizedScope.equals(receiver)) {
                return receiver;
            }
            if (owner != null && normalizedScope.equals(owner)) {
                return owner;
            }
        }
        if (receiver != null && owner != null && !receiver.equals(owner)
                && engine != null) {
            List<MethodResult> methods = engine
                    .getMethod(receiver, site.getCalleeMethodName(), site.getCalleeMethodDesc());
            if (methods != null && !methods.isEmpty()) {
                return receiver;
            }
        }
        return owner;
    }

    private List<MethodCandidate> buildMethodCandidates(String className,
                                                        List<MethodResult> candidates,
                                                        TypeRef scopeType,
                                                        List<TypeRef> argTypes) {
        List<MethodCandidate> out = new ArrayList<>();
        if (candidates == null || candidates.isEmpty()) {
            return out;
        }
        for (MethodResult candidate : candidates) {
            if (candidate == null || candidate.getMethodDesc() == null) {
                continue;
            }
            Type[] params;
            try {
                params = Type.getArgumentTypes(candidate.getMethodDesc());
            } catch (Exception ex) {
                continue;
            }
            boolean varargs = (candidate.getAccessInt() & Opcodes.ACC_VARARGS) != 0;
            List<TypeRef> paramTypes = resolveCandidateParamTypes(
                    className, candidate, scopeType, params, argTypes);
            out.add(new MethodCandidate(
                    className,
                    candidate.getMethodName(),
                    candidate.getMethodDesc(),
                    candidate.getIsStaticInt() == 1,
                    varargs,
                    paramTypes));
        }
        return out;
    }

    private List<CallArgument> buildCallArguments(CompilationUnit cu,
                                                  List<com.github.javaparser.ast.expr.Expression> args,
                                                  CallContext ctx) {
        List<CallArgument> out = new ArrayList<>();
        if (args == null || args.isEmpty()) {
            return out;
        }
        for (com.github.javaparser.ast.expr.Expression expr : args) {
            TypeRef type = typeSolver == null ? null : typeSolver.resolveExpressionType(cu, expr, ctx);
            out.add(CallArgument.from(expr, type));
        }
        return out;
    }

    private List<TypeRef> extractArgumentTypes(List<CallArgument> args) {
        List<TypeRef> out = new ArrayList<>();
        if (args == null || args.isEmpty()) {
            return out;
        }
        for (CallArgument arg : args) {
            out.add(arg == null ? null : arg.type);
        }
        return out;
    }

    private List<TypeRef> resolveCandidateParamTypes(String className,
                                                     MethodResult candidate,
                                                     TypeRef scopeType,
                                                     Type[] descParams,
                                                     List<TypeRef> argTypes) {
        List<TypeRef> out = new ArrayList<>();
        List<TypeRef> resolvedParams = null;
        if (typeSolver != null && candidate != null && candidate.getMethodDesc() != null) {
            TypeSolver.ClassSignatureCache cache = typeSolver.getClassSignatureCache(className);
            TypeRef bindingScope = scopeType;
            if (bindingScope != null && bindingScope.internalName != null
                    && !bindingScope.internalName.equals(className)) {
                bindingScope = null;
            }
            if (cache != null && cache.methodSignatures != null) {
                String sig = cache.methodSignatures.get(
                        candidate.getMethodName() + candidate.getMethodDesc());
                if (sig != null && !sig.trim().isEmpty()) {
                    List<TypeSolver.GenericType> params = typeSolver.parseMethodParamSignatures(sig);
                    if (params != null && !params.isEmpty()) {
                        Map<String, String> classBindings = typeSolver.buildGenericBindings(cache, bindingScope);
                        Map<String, String> methodBindings = typeSolver.buildMethodTypeBindings(sig, argTypes);
                        Map<String, String> bindings = typeSolver.mergeBindings(classBindings, methodBindings);
                        List<TypeRef> temp = new ArrayList<>();
                        for (TypeSolver.GenericType gt : params) {
                            temp.add(typeSolver.resolveGenericType(gt, bindings));
                        }
                        resolvedParams = temp;
                    }
                }
            }
        }
        for (int i = 0; i < descParams.length; i++) {
            TypeRef resolved = null;
            if (resolvedParams != null && i < resolvedParams.size()) {
                resolved = resolvedParams.get(i);
            }
            out.add(toTypeRef(resolved, descParams[i]));
        }
        return out;
    }

    private TypeRef toTypeRef(TypeRef resolved, Type fallback) {
        if (resolved != null) {
            return resolved;
        }
        if (fallback != null) {
            return TypeRef.fromAsmType(fallback);
        }
        return null;
    }

    private List<MethodResult> filterByArgCount(List<MethodResult> candidates, int argCount) {
        if (argCount == TypeSolver.ARG_COUNT_UNKNOWN) {
            return candidates;
        }
        if (candidates == null || candidates.isEmpty()) {
            return candidates;
        }
        List<MethodResult> filtered = new ArrayList<>();
        for (MethodResult result : candidates) {
            if (result == null) {
                continue;
            }
            String desc = result.getMethodDesc();
            if (desc == null || desc.trim().isEmpty()) {
                continue;
            }
            try {
                Type[] args = Type.getArgumentTypes(desc);
                if (args.length == argCount) {
                    filtered.add(result);
                }
            } catch (Exception ignored) {
            }
        }
        return filtered;
    }

    private List<MethodCallExpr> collectMethodCalls(Node scopeRoot) {
        List<MethodCallExpr> out = new ArrayList<>();
        collectMethodCalls(scopeRoot, scopeRoot, out);
        return out;
    }

    private void collectMethodCalls(Node node, Node scopeRoot, List<MethodCallExpr> out) {
        if (node == null) {
            return;
        }
        if (node != scopeRoot) {
            if (node instanceof LambdaExpr) {
                return;
            }
            if (node instanceof ClassOrInterfaceDeclaration
                    || node instanceof EnumDeclaration
                    || node instanceof RecordDeclaration
                    || node instanceof AnnotationDeclaration) {
                return;
            }
        }
        if (node instanceof MethodCallExpr) {
            out.add((MethodCallExpr) node);
        }
        for (Node child : node.getChildNodes()) {
            collectMethodCalls(child, scopeRoot, out);
        }
    }

    public EnclosingCallable resolveEnclosingCallable(CompilationUnit cu, Position pos) {
        if (cu == null || pos == null || typeSolver == null) {
            return null;
        }
        EnclosingCallable best = null;
        long bestSpan = Long.MAX_VALUE;
        List<MethodDeclaration> methods = cu.findAll(MethodDeclaration.class);
        for (MethodDeclaration decl : methods) {
            Range range = decl.getRange().orElse(null);
            if (range == null || !contains(range, pos)) {
                continue;
            }
            long span = rangeSpan(range);
            if (span < bestSpan) {
                String className = typeSolver.resolveEnclosingClassName(cu, decl);
                int argCount = decl.getParameters() == null
                        ? TypeSolver.ARG_COUNT_UNKNOWN
                        : decl.getParameters().size();
                best = new EnclosingCallable(className, decl.getNameAsString(), argCount);
                bestSpan = span;
            }
        }
        List<ConstructorDeclaration> constructors = cu.findAll(ConstructorDeclaration.class);
        for (ConstructorDeclaration decl : constructors) {
            Range range = decl.getRange().orElse(null);
            if (range == null || !contains(range, pos)) {
                continue;
            }
            long span = rangeSpan(range);
            if (span < bestSpan) {
                String className = typeSolver.resolveEnclosingClassName(cu, decl);
                int argCount = decl.getParameters() == null
                        ? TypeSolver.ARG_COUNT_UNKNOWN
                        : decl.getParameters().size();
                best = new EnclosingCallable(className, "<init>", argCount);
                bestSpan = span;
            }
        }
        List<InitializerDeclaration> inits = cu.findAll(InitializerDeclaration.class);
        for (InitializerDeclaration decl : inits) {
            Range range = decl.getRange().orElse(null);
            if (range == null || !contains(range, pos)) {
                continue;
            }
            long span = rangeSpan(range);
            if (span < bestSpan) {
                String className = typeSolver.resolveEnclosingClassName(cu, decl);
                String methodName = decl.isStatic() ? "<clinit>" : "<init>";
                best = new EnclosingCallable(className, methodName, TypeSolver.ARG_COUNT_UNKNOWN);
                bestSpan = span;
            }
        }
        return best;
    }

    private long rangeSpan(Range range) {
        if (range == null) {
            return Long.MAX_VALUE;
        }
        long lines = Math.max(0, range.end.line - range.begin.line);
        long cols = Math.max(0, range.end.column - range.begin.column);
        return lines * 10000L + cols;
    }

    private long rangeBeginKey(Range range) {
        if (range == null) {
            return Long.MAX_VALUE;
        }
        return positionKey(range.begin);
    }

    private long positionKey(Position pos) {
        if (pos == null) {
            return Long.MIN_VALUE;
        }
        return (((long) pos.line) << 32) | (pos.column & 0xffffffffL);
    }

    private boolean contains(Range range, Position pos) {
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

    private static Expression unwrapEnclosed(Expression expr) {
        Expression cur = expr;
        while (cur instanceof EnclosedExpr) {
            cur = ((EnclosedExpr) cur).getInner();
        }
        return cur;
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

    private int argCountFromDesc(String desc) {
        if (desc == null || desc.trim().isEmpty()) {
            return TypeSolver.ARG_COUNT_UNKNOWN;
        }
        try {
            return Type.getArgumentTypes(desc).length;
        } catch (Exception ignored) {
            return TypeSolver.ARG_COUNT_UNKNOWN;
        }
    }

    private String buildMethodDescCacheKey(String className,
                                           String methodName,
                                           List<TypeRef> argTypes,
                                           TypeRef scopeType,
                                           boolean preferStatic) {
        if (className == null || methodName == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(className).append('#').append(methodName);
        sb.append('|').append(preferStatic ? "S" : "I");
        sb.append('|').append(encodeTypeRef(scopeType));
        sb.append('|');
        if (argTypes == null || argTypes.isEmpty()) {
            sb.append('-');
        } else {
            for (TypeRef arg : argTypes) {
                sb.append(encodeTypeRef(arg)).append(',');
            }
        }
        String stamp = buildClassStamp(className);
        if (stamp != null && !stamp.isEmpty()) {
            sb.append('|').append(stamp);
        }
        return sb.toString();
    }

    private String buildMethodDescCacheKeyByCount(String className,
                                                  String methodName,
                                                  int argCount) {
        if (className == null || methodName == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(className).append('#').append(methodName).append("|count:").append(argCount);
        String stamp = buildClassStamp(className);
        if (stamp != null && !stamp.isEmpty()) {
            sb.append('|').append(stamp);
        }
        return sb.toString();
    }

    private String encodeTypeRef(TypeRef type) {
        if (type == null) {
            return "-";
        }
        Type asm = type.getAsmType();
        if (asm != null) {
            return asm.getDescriptor();
        }
        String name = type.getInternalName();
        if (name != null && !name.trim().isEmpty()) {
            return "L" + name + ";";
        }
        return "-";
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
            methodDescCache.clear();
            classStampCache.clear();
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
                } catch (Exception ignored) {
                }
            }
            if (jarName != null && !jarName.trim().isEmpty()) {
                return "jar:" + jarName;
            }
        }
        return "";
    }

    private String getCachedMethodDesc(String cacheKey) {
        ensureRootKey();
        if (cacheKey == null) {
            return null;
        }
        String cached = methodDescCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        String value = DatabaseManager.getSemanticCacheValue(cacheKey, CACHE_TYPE_METHOD_DESC);
        if (value != null) {
            methodDescCache.put(cacheKey, value);
        }
        return value;
    }

    private void cacheMethodDesc(String cacheKey, String desc) {
        if (cacheKey == null) {
            return;
        }
        String value = desc == null ? "" : desc;
        methodDescCache.put(cacheKey, value);
        DatabaseManager.putSemanticCacheValue(cacheKey, CACHE_TYPE_METHOD_DESC, value);
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
}
