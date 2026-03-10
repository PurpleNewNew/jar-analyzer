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
import me.n1ar4.jar.analyzer.entity.ResourceEntity;
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class MyBatisMapperXmlIndex {
    private static final Logger logger = LogManager.getLogger();
    private static final long MAX_XML_BYTES = 2L * 1024L * 1024L;
    private static final List<String> STATEMENT_TAGS = List.of("select", "insert", "update", "delete");

    private static volatile CachedIndex cachedIndex;

    private MyBatisMapperXmlIndex() {
    }

    public static Result currentProject() {
        List<ResourceEntity> resources = DatabaseManager.getResources();
        long buildSeq = DatabaseManager.getBuildSeq();
        String fingerprint = resourceFingerprint(resources);
        CachedIndex local = cachedIndex;
        if (local != null && local.buildSeq() == buildSeq && local.resourceFingerprint().equals(fingerprint)) {
            return local.result();
        }
        synchronized (MyBatisMapperXmlIndex.class) {
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
        List<SinkPattern> sinkPatterns = new ArrayList<>();
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
                    logger.debug("skip large mybatis xml resource: {} size={}", resourcePath, size);
                    continue;
                }
            } catch (Exception ignored) {
                logger.debug("resolve mybatis xml size failed: {}", resourcePath);
            }
            Document document = parse(file);
            if (document == null || !isMyBatisMapper(document, lower)) {
                continue;
            }
            collectMapperSinkPatterns(document, sinkPatterns);
        }
        return sinkPatterns.isEmpty() ? Result.empty() : new Result(List.copyOf(sinkPatterns));
    }

    private static void collectMapperSinkPatterns(Document document, List<SinkPattern> out) {
        Element root = document == null ? null : document.getDocumentElement();
        String className = normalizeClassName(root == null ? null : root.getAttribute("namespace"));
        if (className.isBlank()) {
            className = normalizeClassName(root == null ? null : root.getAttribute("class"));
        }
        if (className.isBlank()) {
            return;
        }
        Map<String, String> sqlFragments = collectSqlFragments(document);
        for (String tag : STATEMENT_TAGS) {
            NodeList nodes = document.getElementsByTagName(tag);
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                if (!(node instanceof Element element)) {
                    continue;
                }
                String methodName = safe(element.getAttribute("id"));
                if (methodName.isBlank()) {
                    continue;
                }
                String sql = renderNodes(element.getChildNodes(), sqlFragments).trim();
                if (sql.isBlank()) {
                    continue;
                }
                if (sql.contains("${") || sql.contains("<bind name=\"")) {
                    out.add(new SinkPattern(className, methodName, "*"));
                }
            }
        }
    }

    private static Map<String, String> collectSqlFragments(Document document) {
        Map<String, String> fragments = new HashMap<>();
        if (document == null) {
            return fragments;
        }
        NodeList nodes = document.getElementsByTagName("sql");
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (!(node instanceof Element element)) {
                continue;
            }
            String id = safe(element.getAttribute("id"));
            if (id.isBlank()) {
                continue;
            }
            fragments.put(id, renderNodes(element.getChildNodes(), fragments));
        }
        return fragments;
    }

    private static boolean isMyBatisMapper(Document document, String resourcePath) {
        if (document == null) {
            return false;
        }
        Element root = document.getDocumentElement();
        String rootName = root == null ? "" : safe(root.getTagName()).toLowerCase(Locale.ROOT);
        if ("mapper".equals(rootName) || rootName.endsWith(":mapper")) {
            return true;
        }
        return resourcePath.contains("/mapper/") || resourcePath.contains("/mappers/");
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
            logger.debug("parse mybatis xml failed: {} ({})", file, ex.toString());
            return null;
        }
    }

    private static String renderNodes(NodeList nodes, Map<String, String> sqlFragments) {
        if (nodes == null || nodes.getLength() == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < nodes.getLength(); i++) {
            sb.append(renderNode(nodes.item(i), sqlFragments));
        }
        return sb.toString();
    }

    private static String renderNode(Node node, Map<String, String> sqlFragments) {
        if (node == null) {
            return "";
        }
        short nodeType = node.getNodeType();
        if (nodeType == Node.TEXT_NODE || nodeType == Node.CDATA_SECTION_NODE) {
            return " " + safe(node.getTextContent());
        }
        if (nodeType != Node.ELEMENT_NODE) {
            return "";
        }
        Element element = (Element) node;
        String name = safe(element.getTagName()).toLowerCase(Locale.ROOT);
        if (name.endsWith(":")) {
            name = name.substring(name.indexOf(':') + 1);
        }
        if ("bind".equals(name)) {
            String bindName = safe(element.getAttribute("name"));
            String bindValue = safe(element.getAttribute("value"));
            return " <bind name=\"" + bindName + "\" value=\"" + bindValue + "\"/>";
        }
        if ("include".equals(name)) {
            String refId = safe(element.getAttribute("refid"));
            return " " + sqlFragments.getOrDefault(refId, "");
        }
        if ("trim".equals(name) || "where".equals(name) || "set".equals(name)
                || "if".equals(name) || "when".equals(name) || "otherwise".equals(name)
                || "choose".equals(name) || "foreach".equals(name)) {
            return " " + renderNodes(element.getChildNodes(), sqlFragments);
        }
        return " " + renderNodes(element.getChildNodes(), sqlFragments);
    }

    private static Path safePath(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Path.of(raw);
        } catch (Exception ex) {
            logger.debug("resolve mybatis xml path failed: {} ({})", raw, ex.toString());
            return null;
        }
    }

    private static String normalizeClassName(String raw) {
        String value = safe(raw);
        if (value.isBlank()) {
            return "";
        }
        if (value.startsWith("L") && value.endsWith(";") && value.length() > 2) {
            value = value.substring(1, value.length() - 1);
        }
        return value.replace('.', '/').replace('\\', '/');
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

    public record Result(List<SinkPattern> sinkPatterns) {
        public static Result empty() {
            return new Result(List.of());
        }

        public SinkPattern resolve(String className, String methodName, String methodDesc) {
            String owner = normalizeClassName(className);
            String name = safe(methodName);
            String desc = safe(methodDesc);
            if (owner.isBlank() || name.isBlank()) {
                return null;
            }
            for (SinkPattern pattern : sinkPatterns) {
                if (pattern != null && pattern.matches(owner, name, desc)) {
                    return pattern;
                }
            }
            return null;
        }
    }

    public record SinkPattern(String className, String methodName, String methodDesc) {
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
