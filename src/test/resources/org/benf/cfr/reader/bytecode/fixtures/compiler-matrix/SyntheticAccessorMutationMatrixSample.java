public class SyntheticAccessorMutationMatrixSample {
    private int counter;

    class Inner {
        void bump() {
            ++counter;
        }

        int read() {
            return counter;
        }
    }

    static int run() {
        SyntheticAccessorMutationMatrixSample outer = new SyntheticAccessorMutationMatrixSample();
        Inner inner = outer.new Inner();
        inner.bump();
        inner.bump();
        return inner.read();
    }
}
