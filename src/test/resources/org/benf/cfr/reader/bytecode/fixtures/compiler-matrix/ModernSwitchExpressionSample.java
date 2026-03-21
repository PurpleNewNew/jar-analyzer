public class ModernSwitchExpressionSample {
    static int map(String value) {
        return switch (value) {
            case "open" -> 1;
            case "closed" -> 2;
            default -> -1;
        };
    }
}
