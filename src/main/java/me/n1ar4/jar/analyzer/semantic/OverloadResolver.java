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

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import me.n1ar4.jar.analyzer.entity.MethodResult;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class OverloadResolver {
    private static final int COST_INCOMPATIBLE = Integer.MAX_VALUE / 4;
    private static final int COST_UNKNOWN = 6;
    private static final int COST_UNKNOWN_PRIMITIVE = 8;
    private static final int COST_VARARGS_PENALTY = 2;

    private OverloadResolver() {
    }

    public static MethodCandidate pickBest(List<MethodCandidate> candidates,
                                           List<TypeRef> argTypes,
                                           boolean preferStatic,
                                           HierarchyResolver hierarchy) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        List<TypeRef> args = argTypes == null ? Collections.emptyList() : argTypes;
        List<CandidateScore> strict = scoreCandidates(candidates, args, Phase.STRICT, hierarchy);
        if (!strict.isEmpty()) {
            return chooseBest(strict, preferStatic, hierarchy);
        }
        List<CandidateScore> boxing = scoreCandidates(candidates, args, Phase.BOXING, hierarchy);
        if (!boxing.isEmpty()) {
            return chooseBest(boxing, preferStatic, hierarchy);
        }
        List<CandidateScore> varargs = scoreCandidates(candidates, args, Phase.VARARGS, hierarchy);
        if (varargs.isEmpty()) {
            return null;
        }
        return chooseBest(varargs, preferStatic, hierarchy);
    }

    public static MethodCandidate pickBest(List<MethodCandidate> candidates,
                                           List<CallArgument> args,
                                           boolean preferStatic,
                                           HierarchyResolver hierarchy,
                                           TypeSolver solver,
                                           CompilationUnit cu,
                                           CallContext ctx) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        List<CallArgument> safeArgs = args == null ? Collections.emptyList() : args;
        List<CandidateScore> strict = scoreCandidates(candidates, safeArgs, Phase.STRICT, hierarchy, solver, cu, ctx);
        if (!strict.isEmpty()) {
            return chooseBest(strict, preferStatic, hierarchy);
        }
        List<CandidateScore> boxing = scoreCandidates(candidates, safeArgs, Phase.BOXING, hierarchy, solver, cu, ctx);
        if (!boxing.isEmpty()) {
            return chooseBest(boxing, preferStatic, hierarchy);
        }
        List<CandidateScore> varargs = scoreCandidates(candidates, safeArgs, Phase.VARARGS, hierarchy, solver, cu, ctx);
        if (varargs.isEmpty()) {
            return null;
        }
        return chooseBest(varargs, preferStatic, hierarchy);
    }

    private static List<CandidateScore> scoreCandidates(List<MethodCandidate> candidates,
                                                        List<TypeRef> args,
                                                        Phase phase,
                                                        HierarchyResolver hierarchy) {
        List<CandidateScore> out = new ArrayList<>();
        for (MethodCandidate candidate : candidates) {
            CandidateScore score = scoreCandidate(candidate, args, phase, hierarchy);
            if (score != null) {
                out.add(score);
            }
        }
        return out;
    }

    private static List<CandidateScore> scoreCandidates(List<MethodCandidate> candidates,
                                                        List<CallArgument> args,
                                                        Phase phase,
                                                        HierarchyResolver hierarchy,
                                                        TypeSolver solver,
                                                        CompilationUnit cu,
                                                        CallContext ctx) {
        List<CandidateScore> out = new ArrayList<>();
        for (MethodCandidate candidate : candidates) {
            CandidateScore score = scoreCandidate(candidate, args, phase, hierarchy, solver, cu, ctx);
            if (score != null) {
                out.add(score);
            }
        }
        return out;
    }

    private static MethodCandidate chooseBest(List<CandidateScore> scored,
                                              boolean preferStatic,
                                              HierarchyResolver hierarchy) {
        CandidateScore best = null;
        for (CandidateScore score : scored) {
            if (best == null || compareCandidates(score, best, preferStatic, hierarchy) < 0) {
                best = score;
            }
        }
        return best == null ? null : best.candidate;
    }

    private static CandidateScore scoreCandidate(MethodCandidate candidate,
                                                 List<TypeRef> args,
                                                 Phase phase,
                                                 HierarchyResolver hierarchy) {
        if (candidate == null) {
            return null;
        }
        List<TypeRef> params = candidate.getParamTypes() == null
                ? Collections.emptyList()
                : candidate.getParamTypes();
        int argCount = args.size();
        int paramCount = params.size();
        boolean varargsMethod = candidate.isVarargs();
        if (!varargsMethod || phase != Phase.VARARGS) {
            if (argCount != paramCount) {
                return null;
            }
            return computeScore(candidate, args, params, phase, false, hierarchy);
        }
        if (argCount < Math.max(0, paramCount - 1)) {
            return null;
        }
        CandidateScore best = null;
        if (argCount == paramCount) {
            best = computeScore(candidate, args, params, phase, false, hierarchy);
        }
        TypeRef varParam = paramCount == 0 ? null : params.get(paramCount - 1);
        TypeRef element = toVarArgElement(varParam);
        if (element != null) {
            List<TypeRef> expanded = new ArrayList<>(argCount);
            int fixedCount = Math.max(0, paramCount - 1);
            for (int i = 0; i < argCount; i++) {
                if (i < fixedCount) {
                    expanded.add(params.get(i));
                } else {
                    expanded.add(element);
                }
            }
            CandidateScore expandedScore = computeScore(candidate, args, expanded, phase, true, hierarchy);
            best = chooseBetter(best, expandedScore, hierarchy);
        }
        return best;
    }

    private static CandidateScore scoreCandidate(MethodCandidate candidate,
                                                 List<CallArgument> args,
                                                 Phase phase,
                                                 HierarchyResolver hierarchy,
                                                 TypeSolver solver,
                                                 CompilationUnit cu,
                                                 CallContext ctx) {
        if (candidate == null) {
            return null;
        }
        List<TypeRef> params = candidate.getParamTypes() == null
                ? Collections.emptyList()
                : candidate.getParamTypes();
        int argCount = args.size();
        int paramCount = params.size();
        boolean varargsMethod = candidate.isVarargs();
        if (!varargsMethod || phase != Phase.VARARGS) {
            if (argCount != paramCount) {
                return null;
            }
            return computeScore(candidate, args, params, phase, false, hierarchy, solver, cu, ctx);
        }
        if (argCount < Math.max(0, paramCount - 1)) {
            return null;
        }
        CandidateScore best = null;
        if (argCount == paramCount) {
            best = computeScore(candidate, args, params, phase, false, hierarchy, solver, cu, ctx);
        }
        TypeRef varParam = paramCount == 0 ? null : params.get(paramCount - 1);
        TypeRef element = toVarArgElement(varParam);
        if (element != null) {
            List<TypeRef> expanded = new ArrayList<>(argCount);
            int fixedCount = Math.max(0, paramCount - 1);
            for (int i = 0; i < argCount; i++) {
                if (i < fixedCount) {
                    expanded.add(params.get(i));
                } else {
                    expanded.add(element);
                }
            }
            CandidateScore expandedScore = computeScore(
                    candidate, args, expanded, phase, true, hierarchy, solver, cu, ctx);
            best = chooseBetter(best, expandedScore, hierarchy);
        }
        return best;
    }

    private static CandidateScore chooseBetter(CandidateScore left,
                                               CandidateScore right,
                                               HierarchyResolver hierarchy) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        int cmp = compareCandidates(left, right, false, hierarchy);
        return cmp <= 0 ? left : right;
    }

    private static CandidateScore computeScore(MethodCandidate candidate,
                                               List<TypeRef> args,
                                               List<TypeRef> params,
                                               Phase phase,
                                               boolean varargsUsed,
                                               HierarchyResolver hierarchy) {
        if (args.size() != params.size()) {
            return null;
        }
        int cost = 0;
        int unknown = 0;
        int size = args.size();
        for (int i = 0; i < size; i++) {
            Conversion conv = conversionCost(args.get(i), params.get(i), phase, hierarchy);
            if (!conv.compatible) {
                return null;
            }
            cost += conv.cost;
            unknown += conv.unknown;
            if (cost >= COST_INCOMPATIBLE) {
                return null;
            }
        }
        if (varargsUsed) {
            cost += COST_VARARGS_PENALTY;
        }
        return new CandidateScore(candidate, cost, unknown, varargsUsed, params);
    }

    private static CandidateScore computeScore(MethodCandidate candidate,
                                               List<CallArgument> args,
                                               List<TypeRef> params,
                                               Phase phase,
                                               boolean varargsUsed,
                                               HierarchyResolver hierarchy,
                                               TypeSolver solver,
                                               CompilationUnit cu,
                                               CallContext ctx) {
        if (args.size() != params.size()) {
            return null;
        }
        int cost = 0;
        int unknown = 0;
        int size = args.size();
        for (int i = 0; i < size; i++) {
            Conversion conv = conversionCost(args.get(i), params.get(i), phase, hierarchy, solver, cu, ctx);
            if (!conv.compatible) {
                return null;
            }
            cost += conv.cost;
            unknown += conv.unknown;
            if (cost >= COST_INCOMPATIBLE) {
                return null;
            }
        }
        if (varargsUsed) {
            cost += COST_VARARGS_PENALTY;
        }
        return new CandidateScore(candidate, cost, unknown, varargsUsed, params);
    }

    private static int compareCandidates(CandidateScore left,
                                         CandidateScore right,
                                         boolean preferStatic,
                                         HierarchyResolver hierarchy) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return 1;
        }
        if (right == null) {
            return -1;
        }
        if (left.cost != right.cost) {
            return Integer.compare(left.cost, right.cost);
        }
        if (left.unknown != right.unknown) {
            return Integer.compare(left.unknown, right.unknown);
        }
        if (left.varargsUsed != right.varargsUsed) {
            return left.varargsUsed ? 1 : -1;
        }
        int specific = compareSpecificity(left, right, hierarchy);
        if (specific != 0) {
            return specific;
        }
        if (preferStatic && left.candidate.isStatic() != right.candidate.isStatic()) {
            return left.candidate.isStatic() ? -1 : 1;
        }
        if (!preferStatic && left.candidate.isStatic() != right.candidate.isStatic()) {
            return left.candidate.isStatic() ? 1 : -1;
        }
        return 0;
    }

    private static int compareSpecificity(CandidateScore left,
                                          CandidateScore right,
                                          HierarchyResolver hierarchy) {
        List<TypeRef> leftParams = left.paramTypes;
        List<TypeRef> rightParams = right.paramTypes;
        int size = Math.min(leftParams.size(), rightParams.size());
        boolean leftBetter = false;
        boolean rightBetter = false;
        for (int i = 0; i < size; i++) {
            int rel = compareParamSpecificity(leftParams.get(i), rightParams.get(i), hierarchy);
            if (rel < 0) {
                leftBetter = true;
            } else if (rel > 0) {
                rightBetter = true;
            }
        }
        if (leftBetter && !rightBetter) {
            return -1;
        }
        if (rightBetter && !leftBetter) {
            return 1;
        }
        return 0;
    }

    private static int compareParamSpecificity(TypeRef left,
                                               TypeRef right,
                                               HierarchyResolver hierarchy) {
        if (left == null || right == null || left.getAsmType() == null || right.getAsmType() == null) {
            return 0;
        }
        Type leftType = left.getAsmType();
        Type rightType = right.getAsmType();
        int leftSort = leftType.getSort();
        int rightSort = rightType.getSort();
        if (leftSort >= Type.BOOLEAN && leftSort <= Type.DOUBLE
                && rightSort >= Type.BOOLEAN && rightSort <= Type.DOUBLE) {
            if (leftSort == rightSort) {
                return 0;
            }
            if (isWideningPrimitive(leftSort, rightSort)) {
                return -1;
            }
            if (isWideningPrimitive(rightSort, leftSort)) {
                return 1;
            }
            return 0;
        }
        if (leftSort == Type.ARRAY && rightSort == Type.ARRAY) {
            if (isArraySubtype(leftType, rightType, hierarchy)) {
                return -1;
            }
            if (isArraySubtype(rightType, leftType, hierarchy)) {
                return 1;
            }
            return 0;
        }
        if (leftSort == Type.ARRAY && rightSort == Type.OBJECT) {
            String rightName = rightType.getInternalName();
            if (isArraySupertype(rightName)) {
                return -1;
            }
            return 0;
        }
        if (rightSort == Type.ARRAY && leftSort == Type.OBJECT) {
            String leftName = leftType.getInternalName();
            if (isArraySupertype(leftName)) {
                return 1;
            }
            return 0;
        }
        if (leftSort == Type.OBJECT && rightSort == Type.OBJECT) {
            String leftName = leftType.getInternalName();
            String rightName = rightType.getInternalName();
            if (leftName == null || rightName == null || leftName.equals(rightName)) {
                return 0;
            }
            if (hierarchy != null && hierarchy.isSubtype(leftName, rightName)) {
                return -1;
            }
            if (hierarchy != null && hierarchy.isSubtype(rightName, leftName)) {
                return 1;
            }
            return 0;
        }
        return 0;
    }

    private static Conversion conversionCost(TypeRef arg,
                                             TypeRef param,
                                             Phase phase,
                                             HierarchyResolver hierarchy) {
        if (param == null || param.getAsmType() == null) {
            return new Conversion(COST_UNKNOWN, 1, true);
        }
        Type paramType = param.getAsmType();
        if (arg == null || arg.getAsmType() == null && arg.getInternalName() == null) {
            if (paramType.getSort() >= Type.BOOLEAN && paramType.getSort() <= Type.DOUBLE) {
                return new Conversion(COST_UNKNOWN_PRIMITIVE, 1, true);
            }
            return new Conversion(COST_UNKNOWN, 1, true);
        }
        Type argType = arg.getAsmType();
        int argSort = argType.getSort();
        int paramSort = paramType.getSort();
        if (argSort >= Type.BOOLEAN && argSort <= Type.DOUBLE) {
            if (paramSort >= Type.BOOLEAN && paramSort <= Type.DOUBLE) {
                if (argSort == paramSort) {
                    return new Conversion(0, 0, true);
                }
                if (isWideningPrimitive(argSort, paramSort)) {
                    return new Conversion(1, 0, true);
                }
                return Conversion.incompatible();
            }
            if (!allowsBoxing(phase)) {
                return Conversion.incompatible();
            }
            String wrapper = wrapperForPrimitive(argSort);
            if (wrapper == null) {
                return Conversion.incompatible();
            }
            Conversion refConv = referenceConversionCost(Type.getObjectType(wrapper), paramType, hierarchy);
            if (!refConv.compatible) {
                return refConv;
            }
            return new Conversion(2 + refConv.cost, refConv.unknown, true);
        }
        if (paramSort >= Type.BOOLEAN && paramSort <= Type.DOUBLE) {
            if (!allowsBoxing(phase)) {
                return Conversion.incompatible();
            }
            String argName = arg.getInternalName();
            Integer primitive = primitiveForWrapper(argName);
            if (primitive == null) {
                return Conversion.incompatible();
            }
            if (primitive == paramSort) {
                return new Conversion(2, 0, true);
            }
            if (isWideningPrimitive(primitive, paramSort)) {
                return new Conversion(3, 0, true);
            }
            return Conversion.incompatible();
        }
        return referenceConversionCost(argType, paramType, hierarchy);
    }

    private static Conversion conversionCost(CallArgument arg,
                                             TypeRef param,
                                             Phase phase,
                                             HierarchyResolver hierarchy,
                                             TypeSolver solver,
                                             CompilationUnit cu,
                                             CallContext ctx) {
        if (arg == null) {
            return conversionCost((TypeRef) null, param, phase, hierarchy);
        }
        if (arg.kind == CallArgument.Kind.LAMBDA) {
            if (param == null || param.getInternalName() == null) {
                return Conversion.incompatible();
            }
            if (solver == null || !solver.isFunctionalInterface(param.getInternalName())) {
                return Conversion.incompatible();
            }
            MethodResult sam = solver.resolveSamMethodPublic(param.getInternalName());
            if (sam != null && sam.getMethodDesc() != null && arg.lambdaParamCount >= 0) {
                int samArgs = solver.argCountFromDesc(sam.getMethodDesc());
                if (samArgs != arg.lambdaParamCount) {
                    return Conversion.incompatible();
                }
            }
            if (solver != null && arg.expression instanceof com.github.javaparser.ast.expr.LambdaExpr) {
                if (!solver.isLambdaCompatible(
                        (com.github.javaparser.ast.expr.LambdaExpr) arg.expression, param, cu, ctx)) {
                    return Conversion.incompatible();
                }
            }
            return new Conversion(0, 0, true);
        }
        if (arg.kind == CallArgument.Kind.METHOD_REF) {
            if (param == null || param.getInternalName() == null) {
                return Conversion.incompatible();
            }
            if (solver == null || !solver.isFunctionalInterface(param.getInternalName())) {
                return Conversion.incompatible();
            }
            if (arg.expression instanceof MethodReferenceExpr) {
                if (!solver.isMethodReferenceCompatible(
                        (MethodReferenceExpr) arg.expression, param, cu, ctx)) {
                    return Conversion.incompatible();
                }
            }
            return new Conversion(0, 0, true);
        }
        return conversionCost(arg.type, param, phase, hierarchy);
    }

    private static Conversion referenceConversionCost(Type argType,
                                                      Type paramType,
                                                      HierarchyResolver hierarchy) {
        if (argType == null || paramType == null) {
            return new Conversion(COST_UNKNOWN, 1, true);
        }
        if (argType.equals(paramType)) {
            return new Conversion(0, 0, true);
        }
        int argSort = argType.getSort();
        int paramSort = paramType.getSort();
        if (argSort == Type.ARRAY && paramSort == Type.ARRAY) {
            if (isArraySubtype(argType, paramType, hierarchy)) {
                if (argType.equals(paramType)) {
                    return new Conversion(0, 0, true);
                }
                return new Conversion(2, 0, true);
            }
            Type argElem = argType.getElementType();
            Type paramElem = paramType.getElementType();
            if (argElem != null && paramElem != null) {
                int argElemSort = argElem.getSort();
                int paramElemSort = paramElem.getSort();
                boolean argPrimitive = argElemSort >= Type.BOOLEAN && argElemSort <= Type.DOUBLE;
                boolean paramPrimitive = paramElemSort >= Type.BOOLEAN && paramElemSort <= Type.DOUBLE;
                if (argPrimitive || paramPrimitive) {
                    return Conversion.incompatible();
                }
                if (hierarchy == null) {
                    return new Conversion(COST_UNKNOWN, 1, true);
                }
            }
            return Conversion.incompatible();
        }
        if (argSort == Type.ARRAY && paramSort == Type.OBJECT) {
            String paramName = paramType.getInternalName();
            if (isArraySupertype(paramName)) {
                return new Conversion(3, 0, true);
            }
            if (hierarchy == null) {
                return new Conversion(COST_UNKNOWN, 1, true);
            }
            return Conversion.incompatible();
        }
        if (argSort == Type.OBJECT && paramSort == Type.ARRAY) {
            return Conversion.incompatible();
        }
        if (argSort != Type.OBJECT || paramSort != Type.OBJECT) {
            return Conversion.incompatible();
        }
        String argName = argType.getInternalName();
        String paramName = paramType.getInternalName();
        if (argName == null || paramName == null) {
            return new Conversion(COST_UNKNOWN, 1, true);
        }
        if (argName.equals(paramName)) {
            return new Conversion(0, 0, true);
        }
        if ("java/lang/Object".equals(paramName)) {
            return new Conversion(4, 0, true);
        }
        if (hierarchy != null && hierarchy.isSubtype(argName, paramName)) {
            int dist = hierarchy.distance(argName, paramName);
            int extra = dist == Integer.MAX_VALUE ? 3 : Math.min(4, dist);
            return new Conversion(1 + extra, 0, true);
        }
        if (hierarchy == null) {
            return new Conversion(COST_UNKNOWN, 1, true);
        }
        return Conversion.incompatible();
    }

    private static boolean allowsBoxing(Phase phase) {
        return phase != Phase.STRICT;
    }

    private static TypeRef toVarArgElement(TypeRef varParam) {
        if (varParam == null || varParam.getAsmType() == null) {
            return varParam;
        }
        Type asm = varParam.getAsmType();
        if (asm.getSort() != Type.ARRAY) {
            return null;
        }
        Type element = asm.getElementType();
        return TypeRef.fromAsmType(element);
    }

    private static boolean isArraySubtype(Type child, Type parent, HierarchyResolver hierarchy) {
        if (child == null || parent == null) {
            return false;
        }
        if (child.getSort() != Type.ARRAY || parent.getSort() != Type.ARRAY) {
            return false;
        }
        Type childElem = child.getElementType();
        Type parentElem = parent.getElementType();
        if (childElem == null || parentElem == null) {
            return false;
        }
        int childSort = childElem.getSort();
        int parentSort = parentElem.getSort();
        if (childSort >= Type.BOOLEAN && childSort <= Type.DOUBLE
                || parentSort >= Type.BOOLEAN && parentSort <= Type.DOUBLE) {
            return childSort == parentSort;
        }
        if (childSort != Type.OBJECT || parentSort != Type.OBJECT) {
            return false;
        }
        String childName = childElem.getInternalName();
        String parentName = parentElem.getInternalName();
        if (childName == null || parentName == null) {
            return false;
        }
        if (childName.equals(parentName)) {
            return true;
        }
        return hierarchy != null && hierarchy.isSubtype(childName, parentName);
    }

    private static boolean isArraySupertype(String internalName) {
        if (internalName == null) {
            return false;
        }
        return "java/lang/Object".equals(internalName)
                || "java/lang/Cloneable".equals(internalName)
                || "java/io/Serializable".equals(internalName);
    }

    private static boolean isWideningPrimitive(int from, int to) {
        if (from == to) {
            return true;
        }
        switch (from) {
            case Type.BYTE:
                return to == Type.SHORT || to == Type.INT || to == Type.LONG || to == Type.FLOAT || to == Type.DOUBLE;
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

    private static String wrapperForPrimitive(int sort) {
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

    private static Integer primitiveForWrapper(String internalName) {
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

    private enum Phase {
        STRICT,
        BOXING,
        VARARGS
    }

    private static final class CandidateScore {
        private final MethodCandidate candidate;
        private final int cost;
        private final int unknown;
        private final boolean varargsUsed;
        private final List<TypeRef> paramTypes;

        private CandidateScore(MethodCandidate candidate,
                               int cost,
                               int unknown,
                               boolean varargsUsed,
                               List<TypeRef> paramTypes) {
            this.candidate = candidate;
            this.cost = cost;
            this.unknown = unknown;
            this.varargsUsed = varargsUsed;
            this.paramTypes = paramTypes;
        }
    }

    private static final class Conversion {
        private final int cost;
        private final int unknown;
        private final boolean compatible;

        private Conversion(int cost, int unknown, boolean compatible) {
            this.cost = cost;
            this.unknown = unknown;
            this.compatible = compatible;
        }

        private static Conversion incompatible() {
            return new Conversion(COST_INCOMPATIBLE, 0, false);
        }
    }
}
