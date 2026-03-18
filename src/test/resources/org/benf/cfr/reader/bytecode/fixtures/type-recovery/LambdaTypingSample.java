import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

public class LambdaTypingSample {
    public Function<String, List<String>> listFactory() {
        return value -> new ArrayList<>();
    }

    public Comparator<String> comparingLength() {
        return Comparator.comparingInt(value -> value.length());
    }
}
