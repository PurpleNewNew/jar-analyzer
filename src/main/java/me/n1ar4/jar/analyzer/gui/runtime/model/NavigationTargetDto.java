package me.n1ar4.jar.analyzer.gui.runtime.model;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public record NavigationTargetDto(
        NavigationTargetType type,
        String className,
        int jarId,
        int resourceId,
        String path,
        String message
) {
    private static final String SEP = "|";
    private static final NavigationTargetDto NONE = new NavigationTargetDto(
            NavigationTargetType.NONE, "", 0, 0, "", ""
    );

    public NavigationTargetDto {
        type = type == null ? NavigationTargetType.NONE : type;
        className = safe(className);
        jarId = Math.max(0, jarId);
        resourceId = Math.max(0, resourceId);
        path = safe(path);
        message = safe(message);
    }

    public static NavigationTargetDto none() {
        return NONE;
    }

    public static NavigationTargetDto classTarget(String className, Integer jarId) {
        return new NavigationTargetDto(NavigationTargetType.CLASS, className, normalizeInt(jarId), 0, "", "");
    }

    public static NavigationTargetDto resourceTarget(int resourceId) {
        return new NavigationTargetDto(NavigationTargetType.RESOURCE, "", 0, resourceId, "", "");
    }

    public static NavigationTargetDto jarPathTarget(Integer jarId) {
        return new NavigationTargetDto(NavigationTargetType.JAR_PATH, "", normalizeInt(jarId), 0, "", "");
    }

    public static NavigationTargetDto filePathTarget(String path) {
        return new NavigationTargetDto(NavigationTargetType.FILE_PATH, "", 0, 0, path, "");
    }

    public static NavigationTargetDto messageTarget(String message) {
        return new NavigationTargetDto(NavigationTargetType.MESSAGE, "", 0, 0, "", message);
    }

    public boolean present() {
        return type != NavigationTargetType.NONE;
    }

    public String encode() {
        return switch (type) {
            case CLASS -> "class" + SEP + encodePart(className) + SEP + jarId;
            case RESOURCE -> "resource" + SEP + resourceId;
            case JAR_PATH -> "jar-path" + SEP + jarId;
            case FILE_PATH -> "file-path" + SEP + encodePart(path);
            case MESSAGE -> "message" + SEP + encodePart(message);
            case NONE -> "";
        };
    }

    public static NavigationTargetDto decode(String raw) {
        String value = safe(raw).trim();
        if (value.isBlank()) {
            return none();
        }
        String[] parts = value.split("\\|", 3);
        if (parts.length == 0) {
            return none();
        }
        return switch (parts[0]) {
            case "class" -> classTarget(decodePart(part(parts, 1)), parseInt(part(parts, 2)));
            case "resource" -> resourceTarget(parseInt(part(parts, 1)));
            case "jar-path" -> jarPathTarget(parseInt(part(parts, 1)));
            case "file-path" -> filePathTarget(decodePart(part(parts, 1)));
            case "message" -> messageTarget(decodePart(part(parts, 1)));
            default -> none();
        };
    }

    private static String part(String[] parts, int index) {
        if (parts == null || index < 0 || index >= parts.length) {
            return "";
        }
        return safe(parts[index]);
    }

    private static int normalizeInt(Integer value) {
        return value == null ? 0 : Math.max(0, value);
    }

    private static String encodePart(String value) {
        if (safe(value).isBlank()) {
            return "";
        }
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(safe(value).getBytes(StandardCharsets.UTF_8));
    }

    private static String decodePart(String value) {
        if (safe(value).isBlank()) {
            return "";
        }
        try {
            return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            return "";
        }
    }

    private static Integer parseInt(String raw) {
        try {
            return Integer.parseInt(safe(raw).trim());
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
