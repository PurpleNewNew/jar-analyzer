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

import me.n1ar4.jar.analyzer.engine.CoreEngine;
import me.n1ar4.jar.analyzer.gui.MainForm;
import me.n1ar4.jar.analyzer.taint.summary.TypeHint;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class LocalVariableTypeResolver {
    private static final Logger logger = LogManager.getLogger();
    private static final Map<String, Map<Integer, LocalTypeHint>> CACHE = new ConcurrentHashMap<>();
    private static final Set<String> SCANNED = ConcurrentHashMap.newKeySet();
    private static final Set<String> MISS = ConcurrentHashMap.newKeySet();

    private LocalVariableTypeResolver() {
    }

    public static LocalTypeHint resolve(String owner, String name, String desc, int index) {
        if (owner == null || name == null || desc == null || index < 0) {
            return null;
        }
        String key = methodKey(owner, name, desc);
        Map<Integer, LocalTypeHint> cached = CACHE.get(key);
        if (cached != null) {
            return cached.get(index);
        }
        if (MISS.contains(key)) {
            return null;
        }
        scanClass(owner);
        cached = CACHE.get(key);
        if (cached == null) {
            MISS.add(key);
            return null;
        }
        LocalTypeHint hint = cached.get(index);
        if (hint == null) {
            return null;
        }
        return hint;
    }

    private static void scanClass(String owner) {
        if (owner == null || SCANNED.contains(owner)) {
            return;
        }
        CoreEngine engine = MainForm.getEngine();
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
            logger.debug("local variable type resolve failed: {}", ex.toString());
        }
        SCANNED.add(owner);
    }

    private static void analyzeAll(byte[] bytes) {
        try {
            ClassReader cr = new ClassReader(bytes);
            ClassNode cn = new ClassNode();
            cr.accept(cn, 0);
            if (cn.methods == null) {
                return;
            }
            for (MethodNode mn : cn.methods) {
                if (mn == null) {
                    continue;
                }
                if (mn.localVariables == null || mn.localVariables.isEmpty()) {
                    continue;
                }
                String key = methodKey(cn.name, mn.name, mn.desc);
                Map<Integer, LocalTypeHint> map = new ConcurrentHashMap<>();
                for (LocalVariableNode lv : mn.localVariables) {
                    if (lv == null) {
                        continue;
                    }
                    LocalTypeHint hint = parseLocalHint(lv.signature, lv.desc);
                    if (hint == null) {
                        continue;
                    }
                    map.putIfAbsent(lv.index, hint);
                }
                if (!map.isEmpty()) {
                    CACHE.put(key, map);
                }
            }
        } catch (Exception ex) {
            logger.debug("local variable analyze failed: {}", ex.toString());
        }
    }

    private static LocalTypeHint parseLocalHint(String signature, String desc) {
        if (signature == null || signature.isEmpty()) {
            return parseDescHint(desc);
        }
        ParseResult result = parseType(signature, 0);
        if (result == null || result.rawType == null || result.rawType.isEmpty()) {
            return parseDescHint(desc);
        }
        TypeHint type = TypeHint.exact(result.rawType);
        GenericSignatureResolver.GenericSignatureInfo generic = buildGenericInfo(result.rawType, result.typeArgs);
        return new LocalTypeHint(type, generic);
    }

    private static LocalTypeHint parseDescHint(String desc) {
        if (desc == null || desc.isEmpty()) {
            return null;
        }
        try {
            Type type = Type.getType(desc);
            if (type.getSort() != Type.OBJECT) {
                return null;
            }
            return new LocalTypeHint(TypeHint.exact(type.getInternalName()), null);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static GenericSignatureResolver.GenericSignatureInfo buildGenericInfo(String rawType, List<TypeHint> args) {
        if (rawType == null || rawType.isEmpty() || args == null || args.isEmpty()) {
            return null;
        }
        if (isMapRaw(rawType)) {
            if (args.size() < 2) {
                return null;
            }
            return new GenericSignatureResolver.GenericSignatureInfo(null, args.get(0), args.get(1));
        }
        if (isCollectionRaw(rawType) || isOptionalRaw(rawType) || isStreamRaw(rawType) || isIterableRaw(rawType)) {
            return new GenericSignatureResolver.GenericSignatureInfo(args.get(0), null, null);
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

    private static String methodKey(String owner, String name, String desc) {
        return owner + "#" + name + desc;
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

    public static final class LocalTypeHint {
        private final TypeHint type;
        private final GenericSignatureResolver.GenericSignatureInfo generic;

        private LocalTypeHint(TypeHint type, GenericSignatureResolver.GenericSignatureInfo generic) {
            this.type = type;
            this.generic = generic;
        }

        public TypeHint getType() {
            return type;
        }

        public GenericSignatureResolver.GenericSignatureInfo getGeneric() {
            return generic;
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
