import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class GenericPropagationSample {
    public <T> List<T> withoutNulls(List<T> input) {
        List<T> copy = new ArrayList<>(input);
        for (Iterator<T> iterator = copy.iterator(); iterator.hasNext(); ) {
            T value = iterator.next();
            if (value == null) {
                iterator.remove();
            }
        }
        return copy;
    }

    public <K, V> List<K> keys(Map<K, V> input) {
        List<K> keys = new ArrayList<>();
        for (Map.Entry<K, V> entry : input.entrySet()) {
            keys.add(entry.getKey());
        }
        return keys;
    }
}
