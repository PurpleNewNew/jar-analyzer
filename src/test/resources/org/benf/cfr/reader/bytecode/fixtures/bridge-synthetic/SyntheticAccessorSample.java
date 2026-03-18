public class SyntheticAccessorSample {
    private int value;

    class Inner {
        int read() {
            return value;
        }

        void write(int next) {
            value = next;
        }
    }

    public int roundTrip(int next) {
        Inner inner = new Inner();
        inner.write(next);
        return inner.read();
    }
}
