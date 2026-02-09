/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */
package me.n1ar4.jar.analyzer.taint;

import me.n1ar4.jar.analyzer.core.BuildSeqUtil;
import me.n1ar4.jar.analyzer.engine.CoreEngine;
import me.n1ar4.jar.analyzer.engine.EngineContext;
import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.jar.analyzer.taint.summary.TypeHint;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class GenericSignatureResolver {
    private static final Logger logger = LogManager.getLogger();
    private static final Object EPOCH_LOCK = new Object();
    private static final AtomicLong LAST_BUILD_SEQ = new AtomicLong(-1);
    private static final Map<String, GenericSignatureInfo> FIELD_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, GenericSignatureInfo> METHOD_RETURN_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, List<GenericSignatureInfo>> METHOD_PARAM_CACHE = new ConcurrentHashMap<>();
    private static final Set<String> SCANNED = ConcurrentHashMap.newKeySet();
    private static final Set<String> MISS_FIELD = ConcurrentHashMap.newKeySet();
    private static final Set<String> MISS_METHOD = ConcurrentHashMap.newKeySet();

    private GenericSignatureResolver() {
    }

    public static GenericSignatureInfo resolveField(String owner, String name, String desc) {
        ensureFresh();
        if (owner == null || name == null || desc == null) {
            return null;
        }
        String key = fieldKey(owner, name, desc);
        GenericSignatureInfo cached = FIELD_CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        if (MISS_FIELD.contains(key)) {
            return null;
        }
        scanClass(owner);
        cached = FIELD_CACHE.get(key);
        if (cached == null) {
            MISS_FIELD.add(key);
        }
        return cached;
    }

    public static GenericSignatureInfo resolveMethodReturn(String owner, String name, String desc) {
        ensureFresh();
        if (owner == null || name == null || desc == null) {
            return null;
        }
        String key = methodKey(owner, name, desc);
        GenericSignatureInfo cached = METHOD_RETURN_CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        if (MISS_METHOD.contains(key)) {
            return null;
        }
        scanClass(owner);
        cached = METHOD_RETURN_CACHE.get(key);
        if (cached == null) {
            MISS_METHOD.add(key);
        }
        return cached;
    }

    public static List<GenericSignatureInfo> resolveMethodParams(String owner, String name, String desc) {
        ensureFresh();
        if (owner == null || name == null || desc == null) {
            return Collections.emptyList();
        }
        String key = methodKey(owner, name, desc);
        List<GenericSignatureInfo> cached = METHOD_PARAM_CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        if (MISS_METHOD.contains(key)) {
            return Collections.emptyList();
        }
        scanClass(owner);
        cached = METHOD_PARAM_CACHE.get(key);
        if (cached == null) {
            MISS_METHOD.add(key);
            return Collections.emptyList();
        }
        return cached;
    }

    private static void scanClass(String owner) {
        ensureFresh();
        if (owner == null || SCANNED.contains(owner)) {
            return;
        }
        CoreEngine engine = EngineContext.getEngine();
        if (engine == null) {
            SCANNED.add(owner);
            return;
        }
        String absPath = engine.getAbsPath(owner);
        if (absPath == null || absPath.trim().isEmpty()) {
            SCANNED.add(owner);
            return;
        }
        try {
            byte[] bytes = Files.readAllBytes(Paths.get(absPath));
            if (bytes.length == 0) {
                SCANNED.add(owner);
                return;
            }
            analyzeAll(bytes);
        } catch (Exception ex) {
            logger.debug("generic signature resolve failed: {}", ex.toString());
        }
        SCANNED.add(owner);
    }

    private static void ensureFresh() {
        BuildSeqUtil.ensureFresh(LAST_BUILD_SEQ, EPOCH_LOCK, () -> {
            FIELD_CACHE.clear();
            METHOD_RETURN_CACHE.clear();
            METHOD_PARAM_CACHE.clear();
            SCANNED.clear();
            MISS_FIELD.clear();
            MISS_METHOD.clear();
        });
    }

    private static void analyzeAll(byte[] bytes) {
        try {
            ClassReader cr = new ClassReader(bytes);
            ClassNode cn = new ClassNode();
            cr.accept(cn, Const.GlobalASMOptions);
            if (cn.fields != null) {
                for (FieldNode fn : cn.fields) {
                    if (fn == null) {
                        continue;
                    }
                    String key = fieldKey(cn.name, fn.name, fn.desc);
                    GenericSignatureInfo info = parseFieldSignature(fn.signature);
                    if (info != null) {
                        FIELD_CACHE.put(key, info);
                    }
                }
            }
            if (cn.methods != null) {
                for (MethodNode mn : cn.methods) {
                    if (mn == null) {
                        continue;
                    }
                    String key = methodKey(cn.name, mn.name, mn.desc);
                    MethodSignatureInfo info = parseMethodSignature(mn.signature);
                    if (info != null) {
                        if (info.returnInfo != null && info.returnInfo.hasAnyHint()) {
                            METHOD_RETURN_CACHE.put(key, info.returnInfo);
                        }
                        if (info.paramInfos != null && !info.paramInfos.isEmpty()) {
                            METHOD_PARAM_CACHE.put(key, info.paramInfos);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            logger.debug("generic signature analyze failed: {}", ex.toString());
        }
    }

    private static GenericSignatureInfo parseFieldSignature(String signature) {
        if (signature == null || signature.isEmpty()) {
            return null;
        }
        ParseResult result = parseType(signature, 0);
        if (result == null) {
            return null;
        }
        return buildInfo(result.rawType, result.typeArgs);
    }

    private static MethodSignatureInfo parseMethodSignature(String signature) {
        if (signature == null || signature.isEmpty()) {
            return null;
        }
        int idx = 0;
        if (signature.charAt(idx) == '<') {
            idx = skipTypeParameters(signature, idx);
        }
        if (idx < 0 || idx >= signature.length() || signature.charAt(idx) != '(') {
            return null;
        }
        idx++;
        List<GenericSignatureInfo> params = new ArrayList<>();
        while (idx < signature.length() && signature.charAt(idx) != ')') {
            ParseResult param = parseType(signature, idx);
            if (param == null) {
                break;
            }
            params.add(buildInfo(param.rawType, param.typeArgs));
            idx = param.nextIndex;
        }
        if (idx >= signature.length() || signature.charAt(idx) != ')') {
            return null;
        }
        idx++;
        ParseResult ret = parseType(signature, idx);
        GenericSignatureInfo returnInfo = ret == null ? null : buildInfo(ret.rawType, ret.typeArgs);
        return new MethodSignatureInfo(params, returnInfo);
    }

    private static int skipTypeParameters(String signature, int idx) {
        int depth = 0;
        int i = idx;
        while (i < signature.length()) {
            char ch = signature.charAt(i);
            if (ch == '<') {
                depth++;
            } else if (ch == '>') {
                depth--;
                if (depth == 0) {
                    return i + 1;
                }
            }
            i++;
        }
        return i;
    }

    private static ParseResult parseType(String signature, int idx) {
        if (signature == null || idx < 0 || idx >= signature.length()) {
            return null;
        }
        char ch = signature.charAt(idx);
        if (ch == 'L') {
            return parseObjectType(signature, idx);
        }
        if (ch == 'T') {
            int end = signature.indexOf(';', idx);
            if (end < 0) {
                return new ParseResult(null, Collections.emptyList(), signature.length());
            }
            return new ParseResult(null, Collections.emptyList(), end + 1);
        }
        if (ch == '[') {
            ParseResult element = parseType(signature, idx + 1);
            return new ParseResult(null, Collections.emptyList(), element == null ? idx + 1 : element.nextIndex);
        }
        // primitive or void
        return new ParseResult(null, Collections.emptyList(), idx + 1);
    }

    private static ParseResult parseObjectType(String signature, int idx) {
        int i = idx + 1;
        StringBuilder name = new StringBuilder();
        List<TypeHint> args = new ArrayList<>();
        while (i < signature.length()) {
            char ch = signature.charAt(i);
            if (ch == '<') {
                i++;
                while (i < signature.length() && signature.charAt(i) != '>') {
                    TypeArgResult arg = parseTypeArg(signature, i);
                    if (arg != null) {
                        args.add(arg.hint);
                        i = arg.nextIndex;
                    } else {
                        break;
                    }
                }
                if (i < signature.length() && signature.charAt(i) == '>') {
                    i++;
                }
                continue;
            }
            if (ch == ';') {
                i++;
                break;
            }
            name.append(ch);
            i++;
        }
        return new ParseResult(name.toString(), args, i);
    }

    private static TypeArgResult parseTypeArg(String signature, int idx) {
        if (signature == null || idx < 0 || idx >= signature.length()) {
            return null;
        }
        char ch = signature.charAt(idx);
        if (ch == '*') {
            return new TypeArgResult(TypeHint.unknown(), idx + 1);
        }
        if (ch == '+') {
            ParseResult nested = parseType(signature, idx + 1);
            TypeHint hint = toHint(nested == null ? null : nested.rawType, true);
            return new TypeArgResult(hint, nested == null ? idx + 1 : nested.nextIndex);
        }
        if (ch == '-') {
            ParseResult nested = parseType(signature, idx + 1);
            return new TypeArgResult(TypeHint.unknown(), nested == null ? idx + 1 : nested.nextIndex);
        }
        ParseResult nested = parseType(signature, idx);
        TypeHint hint = toHint(nested == null ? null : nested.rawType, false);
        return new TypeArgResult(hint, nested == null ? idx + 1 : nested.nextIndex);
    }

    private static TypeHint toHint(String rawType, boolean upperBound) {
        if (rawType == null || rawType.isEmpty()) {
            return TypeHint.unknown();
        }
        return upperBound ? TypeHint.upperBound(rawType) : TypeHint.exact(rawType);
    }

    private static GenericSignatureInfo buildInfo(String rawType, List<TypeHint> args) {
        if (rawType == null || rawType.isEmpty()) {
            return null;
        }
        if (args == null || args.isEmpty()) {
            return null;
        }
        if (isMapRaw(rawType)) {
            if (args.size() < 2) {
                return null;
            }
            GenericSignatureInfo info = new GenericSignatureInfo(null, args.get(0), args.get(1));
            return info.hasAnyHint() ? info : null;
        }
        if (isCollectionRaw(rawType) || isOptionalRaw(rawType) || isStreamRaw(rawType) || isIterableRaw(rawType)) {
            GenericSignatureInfo info = new GenericSignatureInfo(args.get(0), null, null);
            return info.hasAnyHint() ? info : null;
        }
        return null;
    }

    private static boolean isMapRaw(String raw) {
        return "java/util/Map".equals(raw) || "java/util/Map$Entry".equals(raw);
    }

    private static boolean isCollectionRaw(String raw) {
        return "java/util/List".equals(raw)
                || "java/util/Set".equals(raw)
                || "java/util/Collection".equals(raw)
                || "java/util/Queue".equals(raw)
                || "java/util/Deque".equals(raw);
    }

    private static boolean isIterableRaw(String raw) {
        return "java/lang/Iterable".equals(raw) || "java/util/Iterator".equals(raw);
    }

    private static boolean isOptionalRaw(String raw) {
        return "java/util/Optional".equals(raw);
    }

    private static boolean isStreamRaw(String raw) {
        return "java/util/stream/Stream".equals(raw);
    }

    private static String fieldKey(String owner, String name, String desc) {
        return owner + "#" + name + "#" + desc;
    }

    private static String methodKey(String owner, String name, String desc) {
        return owner + "#" + name + desc;
    }

    public static final class GenericSignatureInfo {
        private final TypeHint element;
        private final TypeHint key;
        private final TypeHint value;

        GenericSignatureInfo(TypeHint element, TypeHint key, TypeHint value) {
            this.element = element;
            this.key = key;
            this.value = value;
        }

        public TypeHint getElement() {
            return element;
        }

        public TypeHint getKey() {
            return key;
        }

        public TypeHint getValue() {
            return value;
        }

        public boolean hasAnyHint() {
            return isValid(element) || isValid(key) || isValid(value);
        }

        private boolean isValid(TypeHint hint) {
            return hint != null
                    && hint.getKind() != TypeHint.Kind.UNKNOWN
                    && hint.getType() != null;
        }
    }

    private static final class MethodSignatureInfo {
        private final List<GenericSignatureInfo> paramInfos;
        private final GenericSignatureInfo returnInfo;

        private MethodSignatureInfo(List<GenericSignatureInfo> paramInfos, GenericSignatureInfo returnInfo) {
            this.paramInfos = paramInfos == null ? Collections.emptyList() : paramInfos;
            this.returnInfo = returnInfo;
        }
    }

    private static final class ParseResult {
        private final String rawType;
        private final List<TypeHint> typeArgs;
        private final int nextIndex;

        private ParseResult(String rawType, List<TypeHint> typeArgs, int nextIndex) {
            this.rawType = rawType;
            this.typeArgs = typeArgs == null ? Collections.emptyList() : typeArgs;
            this.nextIndex = nextIndex;
        }
    }

    private static final class TypeArgResult {
        private final TypeHint hint;
        private final int nextIndex;

        private TypeArgResult(TypeHint hint, int nextIndex) {
            this.hint = hint == null ? TypeHint.unknown() : hint;
            this.nextIndex = nextIndex;
        }
    }
}
