import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TargetTypingConstructorSample {
    public Map<String, List<String>> createMap() {
        return new LinkedHashMap<>();
    }

    public Map<String, List<String>> indexByValue(List<String> values) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        for (String value : values) {
            result.computeIfAbsent(value, ignored -> new ArrayList<>()).add(value);
        }
        return result;
    }
}
