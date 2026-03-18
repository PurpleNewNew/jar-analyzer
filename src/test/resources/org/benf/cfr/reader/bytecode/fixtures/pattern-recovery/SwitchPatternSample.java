public class SwitchPatternSample {
    static String test(Object o) {
        return switch (o) {
            case String s -> s;
            case Integer i when i > 10 -> "big" + i;
            case Integer i -> "small" + i;
            case Point(int x, int y) -> x + "," + y;
            case null -> "nil";
            default -> "other";
        };
    }

    record Point(int x, int y) {}
}
