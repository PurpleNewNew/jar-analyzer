public class LegacySwitchSample {
    static int map(String value) {
        switch (value) {
            case "open":
                return 1;
            case "closed":
                return 2;
            default:
                return -1;
        }
    }
}
