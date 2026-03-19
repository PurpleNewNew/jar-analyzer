/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.server.handler.base;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BaseHandlerMethodExtractionTest {
    private final TestHandler handler = new TestHandler();

    @Test
    void extractMethodCodeShouldSupportRecordCompactConstructor() {
        String code = ""
                + "public record ValidatedRecordSample(String name, int age) {\n"
                + "    public ValidatedRecordSample {\n"
                + "        if (name == null || name.isBlank()) {\n"
                + "            throw new IllegalArgumentException(\"name\");\n"
                + "        }\n"
                + "    }\n"
                + "}\n";

        String methodCode = handler.extract(code, "<init>", "(Ljava/lang/String;I)V");

        assertNotNull(methodCode);
        assertTrue(normalize(methodCode).startsWith("public ValidatedRecordSample {"));
        assertTrue(normalize(methodCode).contains("throw new IllegalArgumentException(\"name\");"));
        assertTrue(normalize(methodCode).endsWith("}"));
    }

    @Test
    void extractMethodCodeShouldIgnoreBracesInsideStringsAndCommentsWhenFallbackScanning() {
        String code = ""
                + "class 1 {\n"
                + "    public void run() {\n"
                + "        String json = \"{\\\"outer\\\": {\\\"inner\\\": 1}}\";\n"
                + "        // comment with }\n"
                + "        System.out.println(json);\n"
                + "    }\n"
                + "}\n";

        String methodCode = handler.extract(code, "run", "()V");

        assertNotNull(methodCode);
        assertTrue(normalize(methodCode).startsWith("public void run() {"));
        assertTrue(normalize(methodCode).contains("String json = \"{\\\"outer\\\": {\\\"inner\\\": 1}}\";"));
        assertTrue(normalize(methodCode).contains("// comment with }"));
        assertTrue(normalize(methodCode).contains("System.out.println(json);"));
        assertTrue(normalize(methodCode).endsWith("}"));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.replace("\r\n", "\n").trim();
    }

    private static final class TestHandler extends BaseHandler {
        private String extract(String classCode, String methodName, String methodDesc) {
            return extractMethodCode(classCode, methodName, methodDesc);
        }
    }
}
