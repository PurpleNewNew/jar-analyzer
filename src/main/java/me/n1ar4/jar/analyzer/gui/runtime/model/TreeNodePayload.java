/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.gui.runtime.model;

public record TreeNodePayload(
        String type,
        String className,
        String methodName,
        String methodDesc,
        Integer jarId,
        Integer resourceId,
        String path,
        String locator,
        String artifactRole,
        boolean indexed,
        boolean resolvable,
        String displayHint
) {
    public static final String TYPE_CATEGORY = "category";
    public static final String TYPE_CLASS = "class";
    public static final String TYPE_RESOURCE = "resource";
    public static final String TYPE_ARCHIVE = "archive";
    public static final String TYPE_PATH = "path";
    public static final String TYPE_INFO = "info";

    public TreeNodePayload {
        type = safe(type);
        className = safe(className);
        methodName = safe(methodName);
        methodDesc = safe(methodDesc);
        path = safe(path);
        locator = safe(locator);
        artifactRole = safe(artifactRole);
        displayHint = safe(displayHint);
    }

    public static TreeNodePayload category(String hint) {
        return new TreeNodePayload(TYPE_CATEGORY, "", "", "", null, null, "", "", "", false, false, hint);
    }

    public static TreeNodePayload classNode(String className,
                                            Integer jarId,
                                            String locator,
                                            String artifactRole,
                                            boolean indexed,
                                            boolean resolvable,
                                            String displayHint) {
        return new TreeNodePayload(
                TYPE_CLASS,
                className,
                "",
                "",
                jarId,
                null,
                "",
                locator,
                artifactRole,
                indexed,
                resolvable,
                displayHint
        );
    }

    public static TreeNodePayload resourceNode(Integer resourceId,
                                               Integer jarId,
                                               String path,
                                               String locator,
                                               String artifactRole,
                                               boolean indexed,
                                               boolean resolvable,
                                               String displayHint) {
        return new TreeNodePayload(
                TYPE_RESOURCE,
                "",
                "",
                "",
                jarId,
                resourceId,
                path,
                locator,
                artifactRole,
                indexed,
                resolvable,
                displayHint
        );
    }

    public static TreeNodePayload archiveNode(Integer jarId,
                                              String path,
                                              String locator,
                                              String artifactRole,
                                              boolean indexed,
                                              boolean resolvable,
                                              String displayHint) {
        return new TreeNodePayload(
                TYPE_ARCHIVE,
                "",
                "",
                "",
                jarId,
                null,
                path,
                locator,
                artifactRole,
                indexed,
                resolvable,
                displayHint
        );
    }

    public static TreeNodePayload pathNode(String path, String displayHint) {
        return new TreeNodePayload(TYPE_PATH, "", "", "", null, null, path, "", "", false, true, displayHint);
    }

    public static TreeNodePayload info(String displayHint) {
        return new TreeNodePayload(TYPE_INFO, "", "", "", null, null, "", "", "", false, false, displayHint);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
