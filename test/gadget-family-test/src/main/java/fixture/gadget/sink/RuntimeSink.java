package fixture.gadget.sink;

public final class RuntimeSink {
    private RuntimeSink() {
    }

    public static String exec(Object input) {
        return String.valueOf(input);
    }
}
