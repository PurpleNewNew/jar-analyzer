import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class TernaryTypeMergeSample {
    public Collection<String> chooseCollection(boolean flag) {
        return flag ? new ArrayList<>() : new LinkedList<>();
    }

    public List<String> copyInput(boolean flag, List<String> input) {
        return flag ? new ArrayList<>(input) : new LinkedList<>(input);
    }
}
