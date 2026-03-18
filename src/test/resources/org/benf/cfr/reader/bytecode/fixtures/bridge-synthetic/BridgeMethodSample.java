public class BridgeMethodSample {
    static abstract class Base<T> {
        public abstract T get();
    }

    static final class Impl extends Base<String> {
        @Override
        public String get() {
            return "value";
        }
    }

    public String read() {
        return new Impl().get();
    }
}
