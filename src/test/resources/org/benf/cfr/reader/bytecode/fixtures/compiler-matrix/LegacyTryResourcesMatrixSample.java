import java.io.ByteArrayOutputStream;

public class LegacyTryResourcesMatrixSample {
    static int size() throws Exception {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            output.write(1);
            return output.size();
        }
    }
}
