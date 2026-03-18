public class UnifiedPatternSample {
    static String describe(Object o) {
        if (o instanceof String s && s.length() > 2) {
            return s.trim();
        }
        return switch (o) {
            case Integer i when i > 10 -> "big" + i;
            case Integer i -> "small" + i;
            case Point(int x, int y) -> x + ":" + y;
            default -> "other";
        };
    }

    record Point(int x, int y) {}
}
