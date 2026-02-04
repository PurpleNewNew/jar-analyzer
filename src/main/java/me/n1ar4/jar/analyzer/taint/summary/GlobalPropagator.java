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

import me.n1ar4.jar.analyzer.chains.SinkModel;
import me.n1ar4.jar.analyzer.core.reference.MethodReference;
import me.n1ar4.jar.analyzer.engine.CallGraphCache;
import me.n1ar4.jar.analyzer.engine.CoreEngine;
import me.n1ar4.jar.analyzer.entity.ClassResult;
import me.n1ar4.jar.analyzer.entity.MethodResult;
import me.n1ar4.jar.analyzer.rules.ModelRegistry;
import me.n1ar4.jar.analyzer.rules.SourceModel;
import me.n1ar4.jar.analyzer.taint.Sanitizer;
import org.objectweb.asm.Type;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class GlobalPropagator {
    private final SummaryEngine summaryEngine = new SummaryEngine();

    public ReachabilityIndex propagate(CoreEngine engine, CallGraphCache cache) {
        return propagate(engine, cache, null);
    }

    public ReachabilityIndex propagate(CoreEngine engine, CallGraphCache cache,
                                       Set<MethodReference.Handle> extraSources) {
        return propagate(engine, cache, extraSources, null);
    }

    public ReachabilityIndex propagate(CoreEngine engine, CallGraphCache cache,
                                       Set<MethodReference.Handle> extraSources,
                                       Set<MethodReference.Handle> extraSinks) {
        if (engine == null || cache == null) {
            return new ReachabilityIndex(new HashSet<>(), new HashSet<>());
        }
        Set<MethodReference.Handle> sources = buildSourceHandles(engine);
        if (extraSources != null && !extraSources.isEmpty()) {
            sources.addAll(extraSources);
        }
        Set<MethodReference.Handle> sinks = buildSinkHandles(engine);
        if (extraSinks != null && !extraSinks.isEmpty()) {
            sinks.addAll(extraSinks);
        }
        Set<MethodReference.Handle> reachableFromSource = forwardReachableBySummary(cache, sources);
        Set<MethodReference.Handle> reachableToSink = backwardReachableBySummary(cache, sinks);
        return new ReachabilityIndex(reachableToSink, reachableFromSource);
    }

    private Set<MethodReference.Handle> forwardReachableBySummary(CallGraphCache cache,
                                                                  Set<MethodReference.Handle> sources) {
        Set<MethodReference.Handle> reachableMethods = new HashSet<>();
        Set<MethodPort> visited = new HashSet<>();
        Deque<MethodPort> queue = new ArrayDeque<>();
        for (MethodReference.Handle source : sources) {
            if (source == null) {
                continue;
            }
            for (MethodPort port : allInputPorts(source)) {
                if (visited.add(port)) {
                    queue.add(port);
                    reachableMethods.add(source);
                }
            }
        }
        while (!queue.isEmpty()) {
            MethodPort port = queue.poll();
            MethodSummary summary = summaryEngine.getSummary(port.method);
            if (summary == null || summary.isUnknown()) {
                for (MethodReference.Handle callee : resolveCallees(cache, port.method)) {
                    for (MethodPort next : allInputPorts(callee)) {
                        if (visited.add(next)) {
                            queue.add(next);
                            reachableMethods.add(callee);
                        }
                    }
                }
                continue;
            }
            for (CallFlow flow : summary.getCallFlows()) {
                if (!matchesFrom(port, flow.getFrom())) {
                    continue;
                }
                MethodPort next = toMethodPort(flow.getCallee(), flow.getTo());
                if (next == null) {
                    continue;
                }
                if (visited.add(next)) {
                    queue.add(next);
                    reachableMethods.add(next.method);
                }
            }
        }
        return reachableMethods;
    }

    private Set<MethodReference.Handle> backwardReachableBySummary(CallGraphCache cache,
                                                                   Set<MethodReference.Handle> sinks) {
        Set<MethodReference.Handle> reachableMethods = new HashSet<>();
        Set<MethodPort> visited = new HashSet<>();
        Deque<MethodPort> queue = new ArrayDeque<>();
        for (MethodReference.Handle sink : sinks) {
            if (sink == null) {
                continue;
            }
            for (MethodPort port : allInputPorts(sink)) {
                if (visited.add(port)) {
                    queue.add(port);
                    reachableMethods.add(sink);
                }
            }
        }
        while (!queue.isEmpty()) {
            MethodPort target = queue.poll();
            ArrayList<MethodReference.Handle> callers = resolveCallers(cache, target.method);
            for (MethodReference.Handle caller : callers) {
                if (caller == null) {
                    continue;
                }
                MethodSummary summary = summaryEngine.getSummary(caller);
                if (summary == null || summary.isUnknown()) {
                    for (MethodPort port : allInputPorts(caller)) {
                        if (visited.add(port)) {
                            queue.add(port);
                            reachableMethods.add(caller);
                        }
                    }
                    continue;
                }
                for (CallFlow flow : summary.getCallFlows()) {
                    if (!matchesTo(target, flow.getCallee(), flow.getTo())) {
                        continue;
                    }
                    MethodPort from = toMethodPort(caller, flow.getFrom());
                    if (from == null) {
                        continue;
                    }
                    if (visited.add(from)) {
                        queue.add(from);
                        reachableMethods.add(caller);
                    }
                }
            }
        }
        return reachableMethods;
    }

    private Set<MethodReference.Handle> buildSourceHandles(CoreEngine engine) {
        Set<MethodReference.Handle> out = new HashSet<>();
        ArrayList<ClassResult> springC = engine.getAllSpringC();
        for (ClassResult cr : springC) {
            ArrayList<MethodResult> methods = engine.getSpringM(cr.getClassName());
            for (MethodResult m : methods) {
                MethodReference.Handle handle = toHandle(m);
                if (handle != null) {
                    out.add(handle);
                }
            }
        }
        ArrayList<ClassResult> servlets = engine.getAllServlets();
        for (ClassResult cr : servlets) {
            for (MethodResult method : engine.getMethodsByClass(cr.getClassName())) {
                if (isServletEntry(method)) {
                    MethodReference.Handle handle = toHandle(method);
                    if (handle != null) {
                        out.add(handle);
                    }
                }
            }
        }
        List<String> annoSources = ModelRegistry.getSourceAnnotations();
        if (annoSources != null && !annoSources.isEmpty()) {
            for (MethodResult method : engine.getMethodsByAnnoNames(annoSources)) {
                MethodReference.Handle handle = toHandle(method);
                if (handle != null) {
                    out.add(handle);
                }
            }
        }
        List<SourceModel> sourceModels = ModelRegistry.getSourceModels();
        if (sourceModels != null && !sourceModels.isEmpty()) {
            for (SourceModel model : sourceModels) {
                out.addAll(resolveSourceModel(engine, model));
            }
        }
        return out;
    }

    private List<MethodPort> allInputPorts(MethodReference.Handle handle) {
        if (handle == null) {
            return new ArrayList<>();
        }
        List<MethodPort> ports = new ArrayList<>();
        ports.add(new MethodPort(handle, Sanitizer.THIS_PARAM));
        Type[] args = Type.getArgumentTypes(handle.getDesc());
        for (int i = 0; i < args.length; i++) {
            ports.add(new MethodPort(handle, i));
        }
        return ports;
    }

    private ArrayList<MethodReference.Handle> resolveCallees(CallGraphCache cache,
                                                             MethodReference.Handle caller) {
        if (cache == null || caller == null) {
            return new ArrayList<>();
        }
        ArrayList<MethodResult> callees = cache.getCallees(toMethodResult(caller));
        ArrayList<MethodReference.Handle> out = new ArrayList<>();
        for (MethodResult callee : callees) {
            MethodReference.Handle handle = toHandle(callee);
            if (handle != null) {
                out.add(handle);
            }
        }
        return out;
    }

    private ArrayList<MethodReference.Handle> resolveCallers(CallGraphCache cache,
                                                             MethodReference.Handle callee) {
        if (cache == null || callee == null) {
            return new ArrayList<>();
        }
        ArrayList<MethodResult> callers = cache.getCallers(toMethodResult(callee));
        ArrayList<MethodReference.Handle> out = new ArrayList<>();
        for (MethodResult caller : callers) {
            MethodReference.Handle handle = toHandle(caller);
            if (handle != null) {
                out.add(handle);
            }
        }
        return out;
    }

    private boolean matchesFrom(MethodPort port, FlowPort from) {
        if (port == null || from == null) {
            return false;
        }
        if (from.getKind() == FlowPort.Kind.THIS) {
            return port.paramIndex == Sanitizer.THIS_PARAM;
        }
        if (from.getKind() == FlowPort.Kind.PARAM) {
            return port.paramIndex == from.getIndex();
        }
        return false;
    }

    private boolean matchesTo(MethodPort target, MethodReference.Handle callee, FlowPort to) {
        if (target == null || callee == null || to == null) {
            return false;
        }
        if (!callee.equals(target.method)) {
            return false;
        }
        if (to.getKind() == FlowPort.Kind.THIS) {
            return target.paramIndex == Sanitizer.THIS_PARAM;
        }
        if (to.getKind() == FlowPort.Kind.PARAM) {
            return target.paramIndex == to.getIndex();
        }
        return false;
    }

    private MethodPort toMethodPort(MethodReference.Handle method, FlowPort port) {
        if (method == null || port == null) {
            return null;
        }
        if (port.getKind() == FlowPort.Kind.THIS) {
            return new MethodPort(method, Sanitizer.THIS_PARAM);
        }
        if (port.getKind() == FlowPort.Kind.PARAM) {
            return new MethodPort(method, port.getIndex());
        }
        return null;
    }

    private Set<MethodReference.Handle> resolveSourceModel(CoreEngine engine, SourceModel model) {
        if (model == null || engine == null) {
            return new HashSet<>();
        }
        String className = model.getClassName();
        String methodName = model.getMethodName();
        if (className == null || methodName == null) {
            return new HashSet<>();
        }
        String cls = className.replace('.', '/').trim();
        String desc = model.getMethodDesc();
        List<MethodResult> methods;
        if (desc == null || desc.trim().isEmpty() || "*".equals(desc.trim())) {
            methods = engine.getMethod(cls, methodName, "");
        } else {
            methods = engine.getMethod(cls, methodName, desc);
        }
        Set<MethodReference.Handle> out = new HashSet<>();
        for (MethodResult m : methods) {
            MethodReference.Handle handle = toHandle(m);
            if (handle != null) {
                out.add(handle);
            }
        }
        return out;
    }

    private Set<MethodReference.Handle> buildSinkHandles(CoreEngine engine) {
        Set<MethodReference.Handle> out = new HashSet<>();
        List<SinkModel> sinks = ModelRegistry.getSinkModels();
        for (SinkModel model : sinks) {
            if (model == null) {
                continue;
            }
            String cls = model.getClassName();
            String name = model.getMethodName();
            String desc = model.getMethodDesc();
            if (cls == null || name == null) {
                continue;
            }
            boolean anyDesc = isAnyDesc(desc);
            if (anyDesc && engine != null) {
                List<MethodResult> methods = engine.getMethod(cls, name, "");
                for (MethodResult method : methods) {
                    MethodReference.Handle handle = toHandle(method);
                    if (handle != null) {
                        out.add(handle);
                    }
                }
                if (!methods.isEmpty()) {
                    continue;
                }
            }
            String normalizedDesc = anyDesc ? "" : (desc == null ? "" : desc);
            MethodReference.Handle handle = new MethodReference.Handle(
                    new me.n1ar4.jar.analyzer.core.reference.ClassReference.Handle(cls),
                    name,
                    normalizedDesc);
            out.add(handle);
        }
        return out;
    }

    private MethodReference.Handle toHandle(MethodResult method) {
        if (method == null) {
            return null;
        }
        return new MethodReference.Handle(
                new me.n1ar4.jar.analyzer.core.reference.ClassReference.Handle(method.getClassName()),
                method.getMethodName(),
                method.getMethodDesc());
    }

    private MethodResult toMethodResult(MethodReference.Handle handle) {
        if (handle == null) {
            return null;
        }
        return new MethodResult(handle.getClassReference().getName(), handle.getName(), handle.getDesc());
    }

    private boolean isServletEntry(MethodResult method) {
        if (method == null) {
            return false;
        }
        String name = method.getMethodName();
        String desc = method.getMethodDesc();
        if (name == null || desc == null) {
            return false;
        }
        if (!("doGet".equals(name) || "doPost".equals(name) || "doPut".equals(name)
                || "doDelete".equals(name) || "doHead".equals(name)
                || "doOptions".equals(name) || "doTrace".equals(name)
                || "service".equals(name))) {
            return false;
        }
        return "(Ljavax/servlet/http/HttpServletRequest;Ljavax/servlet/http/HttpServletResponse;)V".equals(desc)
                || "(Ljakarta/servlet/http/HttpServletRequest;Ljakarta/servlet/http/HttpServletResponse;)V".equals(desc)
                || "(Ljavax/servlet/ServletRequest;Ljavax/servlet/ServletResponse;)V".equals(desc)
                || "(Ljakarta/servlet/ServletRequest;Ljakarta/servlet/ServletResponse;)V".equals(desc);
    }

    private boolean isAnyDesc(String desc) {
        if (desc == null) {
            return true;
        }
        String v = desc.trim();
        if (v.isEmpty()) {
            return true;
        }
        return "*".equals(v) || "null".equalsIgnoreCase(v);
    }

    private static final class MethodPort {
        private final MethodReference.Handle method;
        private final int paramIndex;

        private MethodPort(MethodReference.Handle method, int paramIndex) {
            this.method = method;
            this.paramIndex = paramIndex;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof MethodPort)) {
                return false;
            }
            MethodPort that = (MethodPort) o;
            return paramIndex == that.paramIndex
                    && Objects.equals(method, that.method);
        }

        @Override
        public int hashCode() {
            return Objects.hash(method, paramIndex);
        }
    }
}
