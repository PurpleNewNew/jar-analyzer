/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */
package me.n1ar4.jar.analyzer.rules;

import me.n1ar4.jar.analyzer.core.DatabaseManager;
import me.n1ar4.jar.analyzer.core.facts.ResourceEntity;
import me.n1ar4.jar.analyzer.graph.store.GraphNode;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class FrameworkXmlSourceIndex {
    private static final Logger logger = LogManager.getLogger();
    private static final long MAX_XML_BYTES = 2L * 1024L * 1024L;

    private static volatile CachedIndex cachedIndex;

    private FrameworkXmlSourceIndex() {
    }

    public static Result currentProject() {
        List<ResourceEntity> resources = DatabaseManager.getResources();
        long buildSeq = DatabaseManager.getBuildSeq();
        String fingerprint = resourceFingerprint(resources);
        CachedIndex local = cachedIndex;
        if (local != null && local.buildSeq() == buildSeq && local.resourceFingerprint().equals(fingerprint)) {
            return local.result();
        }
        synchronized (FrameworkXmlSourceIndex.class) {
            local = cachedIndex;
            if (local != null && local.buildSeq() == buildSeq && local.resourceFingerprint().equals(fingerprint)) {
                return local.result();
            }
            Result result = fromResources(resources);
            cachedIndex = new CachedIndex(buildSeq, fingerprint, result);
            return result;
        }
    }

    public static Result fromResources(Collection<ResourceEntity> resources) {
        if (resources == null || resources.isEmpty()) {
            return Result.empty();
        }
        Set<String> servlets = new LinkedHashSet<>();
        Set<String> filters = new LinkedHashSet<>();
        Set<String> listeners = new LinkedHashSet<>();
        List<MethodPattern> methodPatterns = new ArrayList<>();
        for (ResourceEntity resource : resources) {
            if (resource == null) {
                continue;
            }
            String resourcePath = safe(resource.getResourcePath()).replace('\\', '/');
            String lower = resourcePath.toLowerCase(Locale.ROOT);
            if (!lower.endsWith(".xml")) {
                continue;
            }
            Path file = safePath(resource.getPathStr());
            if (file == null || Files.notExists(file)) {
                continue;
            }
            try {
                long size = resource.getFileSize();
                if (size > MAX_XML_BYTES) {
                    logger.debug("skip large xml resource: {} size={}", resourcePath, size);
                    continue;
                }
            } catch (Exception ignored) {
                logger.debug("resolve xml size failed: {}", resourcePath);
            }
            Document document = parse(file);
            if (document == null) {
                continue;
            }
            if (lower.endsWith("web.xml")) {
                addTagText(document, "servlet-class", servlets);
                addTagText(document, "filter-class", filters);
                addTagText(document, "listener-class", listeners);
            }
            if (isStrutsConfig(lower)) {
                collectStrutsActions(document, methodPatterns);
            }
        }
        if (servlets.isEmpty() && filters.isEmpty() && listeners.isEmpty() && methodPatterns.isEmpty()) {
            return Result.empty();
        }
        return new Result(
                servlets.isEmpty() ? Set.of() : Set.copyOf(servlets),
                filters.isEmpty() ? Set.of() : Set.copyOf(filters),
                listeners.isEmpty() ? Set.of() : Set.copyOf(listeners),
                methodPatterns.isEmpty() ? List.of() : List.copyOf(methodPatterns)
        );
    }

    private static Document parse(Path file) {
        try (InputStream in = Files.newInputStream(file)) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setXIncludeAware(false);
            factory.setNamespaceAware(false);
            factory.setExpandEntityReferences(false);
            return factory.newDocumentBuilder().parse(in);
        } catch (Exception ex) {
            logger.debug("parse framework xml failed: {} ({})", file, ex.toString());
            return null;
        }
    }

    private static boolean isStrutsConfig(String resourcePath) {
        if (resourcePath.isBlank()) {
            return false;
        }
        return resourcePath.endsWith("struts.xml")
                || resourcePath.endsWith("struts-config.xml")
                || resourcePath.contains("/struts-");
    }

    private static void addTagText(Document document, String tagName, Set<String> out) {
        if (document == null || tagName == null || out == null) {
            return;
        }
        NodeList nodes = document.getElementsByTagName(tagName);
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            String normalized = normalizeClassName(node == null ? null : node.getTextContent());
            if (!normalized.isBlank()) {
                out.add(normalized);
            }
        }
    }

    private static void collectStrutsActions(Document document, List<MethodPattern> out) {
        if (document == null || out == null) {
            return;
        }
        NodeList nodes = document.getElementsByTagName("action");
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (!(node instanceof Element element)) {
                continue;
            }
            String className = normalizeClassName(attr(element, "class"));
            if (className.isBlank()) {
                className = normalizeClassName(attr(element, "type"));
            }
            if (className.isBlank()) {
                continue;
            }
            String methodName = safe(attr(element, "method"));
            if (methodName.isBlank()) {
                methodName = "execute";
            }
            out.add(new MethodPattern(className, methodName, "*", GraphNode.SOURCE_FLAG_WEB));
        }
    }

    private static String attr(Element element, String name) {
        if (element == null || name == null || name.isBlank()) {
            return "";
        }
        return safe(element.getAttribute(name));
    }

    private static Path safePath(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Path.of(raw);
        } catch (Exception ex) {
            logger.debug("resolve xml path failed: {} ({})", raw, ex.toString());
            return null;
        }
    }

    private static String normalizeClassName(String raw) {
        String value = safe(raw);
        if (value.isBlank()) {
            return "";
        }
        if (value.startsWith("class ")) {
            value = value.substring("class ".length()).trim();
        }
        if (value.startsWith("L") && value.endsWith(";") && value.length() > 2) {
            value = value.substring(1, value.length() - 1);
        }
        value = value.replace('.', '/').replace('\\', '/');
        while (value.startsWith("/")) {
            value = value.substring(1);
        }
        if (value.endsWith(".class")) {
            value = value.substring(0, value.length() - ".class".length());
        }
        return value;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static String resourceFingerprint(Collection<ResourceEntity> resources) {
        if (resources == null || resources.isEmpty()) {
            return "empty";
        }
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (ResourceEntity resource : resources) {
            if (resource == null) {
                continue;
            }
            sb.append(safe(resource.getResourcePath()))
                    .append('@')
                    .append(resource.getJarId() == null ? -1 : resource.getJarId())
                    .append('#')
                    .append(resource.getFileSize())
                    .append(';');
            count++;
        }
        return count + ":" + Integer.toHexString(sb.toString().hashCode());
    }

    public record Result(Set<String> servletClasses,
                         Set<String> filterClasses,
                         Set<String> listenerClasses,
                         List<MethodPattern> methodPatterns) {
        public static Result empty() {
            return new Result(Set.of(), Set.of(), Set.of(), List.of());
        }

        public boolean isEmpty() {
            return servletClasses.isEmpty()
                    && filterClasses.isEmpty()
                    && listenerClasses.isEmpty()
                    && methodPatterns.isEmpty();
        }

        public int resolveFlags(String className, String methodName, String methodDesc) {
            String owner = normalizeClassName(className);
            String name = safe(methodName);
            String desc = safe(methodDesc);
            if (owner.isBlank() || name.isBlank() || desc.isBlank()) {
                return 0;
            }
            int flags = 0;
            for (MethodPattern pattern : methodPatterns) {
                if (pattern != null && pattern.matches(owner, name, desc)) {
                    flags |= pattern.flags();
                }
            }
            return flags;
        }
    }

    public record MethodPattern(String className, String methodName, String methodDesc, int flags) {
        public boolean matches(String owner, String name, String desc) {
            if (!normalizeClassName(className).equals(normalizeClassName(owner))) {
                return false;
            }
            if (!safe(methodName).equals(safe(name))) {
                return false;
            }
            String expectedDesc = safe(methodDesc);
            return "*".equals(expectedDesc) || expectedDesc.equals(safe(desc));
        }
    }

    private record CachedIndex(long buildSeq, String resourceFingerprint, Result result) {
    }
}
