package org.benf.cfr.reader.bytecode.fixtures.structure.failure;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public class LambdaGuardCollectorSample {
    public static List<Path> collectNestedLibJars(Path resourcesRoot) throws Exception {
        List<Path> jars = new ArrayList<>();
        Path root = resourcesRoot.toAbsolutePath().normalize();
        try (Stream<Path> stream = Files.walk(root)) {
            stream.forEach(path -> {
                if (path == null || !Files.isRegularFile(path)) {
                    return;
                }
                String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
                if (!name.endsWith(".jar")) {
                    return;
                }
                String rel = root.relativize(path).toString().replace("\\", "/");
                if (isNestedLibPath(rel)) {
                    jars.add(path);
                }
            });
        }
        return jars;
    }

    private static boolean isNestedLibPath(String relativePath) {
        if (relativePath == null) {
            return false;
        }
        String rel = relativePath.replace("\\", "/");
        return rel.startsWith("BOOT-INF/lib/")
                || rel.startsWith("WEB-INF/lib/")
                || rel.startsWith("lib/");
    }
}
