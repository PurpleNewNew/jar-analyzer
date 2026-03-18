package org.benf.cfr.reader.bytecode;

public final class StructuredPassEntry {
    private final String category;
    private final String stage;
    private final String inputRequirement;
    private final StructuredPassDescriptor descriptor;

    private StructuredPassEntry(String category,
                                String stage,
                                String inputRequirement,
                                StructuredPassDescriptor descriptor) {
        this.category = category;
        this.stage = stage;
        this.inputRequirement = inputRequirement;
        this.descriptor = descriptor;
    }

    public static StructuredPassEntry of(String category,
                                         String stage,
                                         String inputRequirement,
                                         StructuredPassDescriptor descriptor) {
        return new StructuredPassEntry(category, stage, inputRequirement, descriptor);
    }

    public String getCategory() {
        return category;
    }

    public String getStage() {
        return stage;
    }

    public String getInputRequirement() {
        return inputRequirement;
    }

    public StructuredPassDescriptor getDescriptor() {
        return descriptor;
    }
}
