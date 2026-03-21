import java.io.StringWriter;

public class ModernTryResourcesMatrixSample {
    static String read() throws Exception {
        try (StringWriter writer = new StringWriter()) {
            writer.write("ok");
            return writer.toString();
        }
    }
}
