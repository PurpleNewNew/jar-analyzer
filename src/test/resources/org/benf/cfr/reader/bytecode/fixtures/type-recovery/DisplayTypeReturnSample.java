import java.util.ArrayList;
import java.util.List;

public class DisplayTypeReturnSample {
    static final class Builder<T> {
        private final T value;

        Builder(T value) {
            this.value = value;
        }

        List<T> buildList() {
            ArrayList<T> out = new ArrayList<>();
            out.add(value);
            return out;
        }
    }

    static <T> List<T> wrap(T value) {
        ArrayList<T> out = new ArrayList<>();
        out.add(value);
        return out;
    }

    public List<String> collect() {
        List<String> fromStatic = wrap("a");
        Builder<String> builder = new Builder<>("b");
        List<String> fromMember = builder.buildList();
        return fromStatic.size() >= fromMember.size() ? fromStatic : fromMember;
    }
}
