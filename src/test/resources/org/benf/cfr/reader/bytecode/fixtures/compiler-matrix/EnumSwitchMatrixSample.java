public class EnumSwitchMatrixSample {
    enum Mode {
        OPEN,
        CLOSED,
        UNKNOWN
    }

    static int map(Mode mode) {
        switch (mode) {
            case OPEN:
                return 1;
            case CLOSED:
                return 2;
            default:
                return 3;
        }
    }
}
