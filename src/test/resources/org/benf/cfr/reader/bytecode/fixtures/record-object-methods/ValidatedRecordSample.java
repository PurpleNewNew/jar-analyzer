public record ValidatedRecordSample(String name, int age) {
    public ValidatedRecordSample {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name");
        }
    }
}
