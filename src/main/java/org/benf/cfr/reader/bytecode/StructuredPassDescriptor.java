package org.benf.cfr.reader.bytecode;

import java.util.List;

public final class StructuredPassDescriptor {
    private final String name;
    private final String outputPromise;
    private final boolean idempotent;
    private final boolean allowsStructuralChange;
    private final List<String> dependencies;

    private StructuredPassDescriptor(String name,
                                     String outputPromise,
                                     boolean idempotent,
                                     boolean allowsStructuralChange,
                                     List<String> dependencies) {
        this.name = name;
        this.outputPromise = outputPromise;
        this.idempotent = idempotent;
        this.allowsStructuralChange = allowsStructuralChange;
        this.dependencies = List.copyOf(dependencies);
    }

    public static StructuredPassDescriptor of(String name,
                                              String outputPromise,
                                              boolean idempotent,
                                              boolean allowsStructuralChange,
                                              String... dependencies) {
        return new StructuredPassDescriptor(
                name,
                outputPromise,
                idempotent,
                allowsStructuralChange,
                List.of(dependencies)
        );
    }

    public String getName() {
        return name;
    }

    public String getOutputPromise() {
        return outputPromise;
    }

    public boolean isIdempotent() {
        return idempotent;
    }

    public boolean allowsStructuralChange() {
        return allowsStructuralChange;
    }

    public List<String> getDependencies() {
        return dependencies;
    }
}
