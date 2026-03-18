import java.util.List;

public class RawReceiverSample {
    public List<String> flattenAndTrim(List<List<String>> values) {
        return values.stream()
                .flatMap(List::stream)
                .map(String::trim)
                .toList();
    }
}
