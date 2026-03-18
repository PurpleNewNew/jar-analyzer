import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;

public class LambdaTernaryTypingSample {
    public Supplier<List<String>> choose(boolean flag) {
        return () -> flag ? new ArrayList<>() : new LinkedList<>();
    }
}
