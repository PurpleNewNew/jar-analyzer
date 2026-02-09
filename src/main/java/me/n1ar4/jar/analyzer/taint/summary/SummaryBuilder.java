/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */
package me.n1ar4.jar.analyzer.taint.summary;

import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.engine.CoreEngine;
import me.n1ar4.jar.analyzer.engine.EngineContext;
import me.n1ar4.jar.analyzer.starter.Const;
import me.n1ar4.jar.analyzer.taint.Sanitizer;
import me.n1ar4.jar.analyzer.taint.TaintAnalysisProfile;
import me.n1ar4.jar.analyzer.taint.TaintMethodAdapter;
import me.n1ar4.jar.analyzer.taint.TaintPropagationConfig;
import me.n1ar4.jar.analyzer.taint.TaintPropagationMode;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public final class SummaryBuilder {
    private static final Logger logger = LogManager.getLogger();

    public MethodSummary build(MethodReference.Handle handle) {
        MethodSummary summary = new MethodSummary();
        if (handle == null) {
            summary.setUnknown(true);
            return summary;
        }
        CoreEngine engine = EngineContext.getEngine();
        if (engine == null) {
            summary.setUnknown(true);
            return summary;
        }
        String absPath = engine.getAbsPath(handle.getClassReference().getName());
        if (absPath == null || absPath.trim().isEmpty()) {
            summary.setUnknown(true);
            return summary;
        }
        try {
            byte[] bytes = Files.readAllBytes(Paths.get(absPath));
            if (bytes.length == 0) {
                summary.setUnknown(true);
                return summary;
            }
            buildFromBytes(bytes, handle, summary);
        } catch (Exception ex) {
            logger.warn("summary build failed: {}", ex.toString());
            summary.setUnknown(true);
        }
        return summary;
    }

    private void buildFromBytes(byte[] bytes,
                                MethodReference.Handle target,
                                MethodSummary summary) throws Exception {
        ClassReader cr = new ClassReader(bytes);
        MethodMeta meta = MethodMeta.resolve(cr, target);
        if (meta == null) {
            summary.setUnknown(true);
            return;
        }
        TaintPropagationConfig config = TaintPropagationConfig.resolve();
        SummaryCollector collector = new SummaryCollectorImpl(summary, meta.isStatic, meta.paramCount);
        if (!meta.isStatic) {
            runSeed(cr, target, meta, Sanitizer.THIS_PARAM, collector, config);
        }
        for (int i = 0; i < meta.paramCount; i++) {
            runSeed(cr, target, meta, i, collector, config);
        }
    }

    private void runSeed(ClassReader cr,
                         MethodReference.Handle target,
                         MethodMeta meta,
                         int seedParam,
                         SummaryCollector collector,
                         TaintPropagationConfig config) {
        try {
            ClassVisitor cv = new SummaryClassVisitor(target, meta, seedParam, collector, config);
            cr.accept(cv, Const.GlobalASMOptions);
        } catch (Exception ex) {
            logger.debug("summary seed failed: {}", ex.toString());
        }
    }

    private static final class SummaryClassVisitor extends ClassVisitor {
        private final MethodReference.Handle target;
        private final MethodMeta meta;
        private final int seedParam;
        private final SummaryCollector collector;
        private final TaintPropagationConfig config;

        private SummaryClassVisitor(MethodReference.Handle target,
                                    MethodMeta meta,
                                    int seedParam,
                                    SummaryCollector collector,
                                    TaintPropagationConfig config) {
            super(Const.ASMVersion);
            this.target = target;
            this.meta = meta;
            this.seedParam = seedParam;
            this.collector = collector;
            this.config = config;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
            if (!name.equals(target.getName()) || !desc.equals(target.getDesc())) {
                return mv;
            }
            return createSummaryAdapter(mv, access, name, desc, seedParam, collector, config);
        }

        private MethodVisitor createSummaryAdapter(MethodVisitor mv,
                                                   int access, String name, String desc,
                                                   int seedParam,
                                                   SummaryCollector collector,
                                                   TaintPropagationConfig config) {
            AtomicReference<me.n1ar4.jar.analyzer.taint.TaintPass> pass =
                    new AtomicReference<>(me.n1ar4.jar.analyzer.taint.TaintPass.fail());
            AtomicBoolean lowConfidence = new AtomicBoolean(false);
            TaintAnalysisProfile profile = config.getProfile();
            TaintPropagationMode mode = config.getPropagationMode();
            MethodReference.Handle dummyNext = new MethodReference.Handle(
                    target.getClassReference(), "noop", "()V");
            return new TaintMethodAdapter(
                    api, mv,
                    target.getClassReference().getName(),
                    access, name, desc,
                    seedParam,
                    dummyNext,
                    pass,
                    config.getBarrierRule(),
                    config.getSummaryRule(),
                    config.getAdditionalRule(),
                    config.getGuardRules(),
                    profile,
                    mode,
                    new StringBuilder(),
                    false,
                    false,
                    false,
                    lowConfidence,
                    null,
                    collector,
                    true);
        }
    }

    private static final class MethodMeta {
        private final boolean isStatic;
        private final int paramCount;

        private MethodMeta(boolean isStatic, int paramCount) {
            this.isStatic = isStatic;
            this.paramCount = paramCount;
        }

        private static MethodMeta resolve(ClassReader cr, MethodReference.Handle target) {
            if (cr == null || target == null) {
                return null;
            }
            final MethodMetaHolder holder = new MethodMetaHolder();
            ClassVisitor cv = new ClassVisitor(Const.ASMVersion) {
                @Override
                public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                    if (!name.equals(target.getName()) || !desc.equals(target.getDesc())) {
                        return super.visitMethod(access, name, desc, signature, exceptions);
                    }
                    int paramCount = Type.getArgumentTypes(desc).length;
                    boolean isStatic = (access & Opcodes.ACC_STATIC) != 0;
                    holder.meta = new MethodMeta(isStatic, paramCount);
                    return super.visitMethod(access, name, desc, signature, exceptions);
                }
            };
            cr.accept(cv, Const.GlobalASMOptions);
            return holder.meta;
        }
    }

    private static final class MethodMetaHolder {
        private MethodMeta meta;
    }

    private static final class SummaryCollectorImpl implements SummaryCollector {
        private final MethodSummary summary;
        private final boolean isStatic;
        private final int paramCount;
        private final java.util.Map<Integer, AtomicBoolean> lowFlags = new java.util.concurrent.ConcurrentHashMap<>();

        private SummaryCollectorImpl(MethodSummary summary, boolean isStatic, int paramCount) {
            this.summary = summary;
            this.isStatic = isStatic;
            this.paramCount = paramCount;
        }

        @Override
        public void onReturnTaint(int seedParam, Set<String> markers) {
            FlowPort from = seedPort(seedParam);
            FlowPort to = FlowPort.ret();
            summary.addEdge(new FlowEdge(from, to, sanitizeMarkers(markers), confidence(seedParam)));
        }

        @Override
        public void onFieldTaint(int seedParam, String owner, String name, String desc, boolean isStaticField, Set<String> markers) {
            FlowPort from = seedPort(seedParam);
            FlowPort to = FlowPort.field(owner, name, desc, isStaticField);
            summary.addEdge(new FlowEdge(from, to, sanitizeMarkers(markers), confidence(seedParam)));
            summary.setHasSideEffect(true);
        }

        @Override
        public void onCallTaint(int seedParam, MethodReference.Handle callee, FlowPort to, Set<String> markers) {
            if (callee == null || to == null) {
                return;
            }
            FlowPort from = seedPort(seedParam);
            summary.addCallFlow(new CallFlow(from, callee, to, sanitizeMarkers(markers), confidence(seedParam)));
        }

        @Override
        public void onLowConfidence(int seedParam, String reason) {
            lowFlags.computeIfAbsent(seedParam, k -> new AtomicBoolean()).set(true);
        }

        private FlowPort seedPort(int seedParam) {
            if (seedParam == Sanitizer.THIS_PARAM) {
                return FlowPort.thisPort();
            }
            if (seedParam >= 0 && seedParam < paramCount) {
                return FlowPort.param(seedParam);
            }
            return FlowPort.thisPort();
        }

        private String confidence(int seedParam) {
            AtomicBoolean flag = lowFlags.get(seedParam);
            return flag != null && flag.get() ? "low" : "high";
        }

        private Set<String> sanitizeMarkers(Set<String> markers) {
            if (markers == null || markers.isEmpty()) {
                return markers;
            }
            Set<String> filtered = new HashSet<>();
            for (String marker : markers) {
                if (marker == null || marker.startsWith("ALIAS:")) {
                    continue;
                }
                filtered.add(marker);
            }
            return filtered;
        }
    }
}
