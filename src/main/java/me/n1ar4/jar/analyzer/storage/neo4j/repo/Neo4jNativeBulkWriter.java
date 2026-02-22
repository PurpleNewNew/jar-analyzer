/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.storage.neo4j.repo;

import me.n1ar4.jar.analyzer.storage.neo4j.Neo4jStore;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Neo4jNativeBulkWriter {
    private static final Label LABEL_METHOD = Label.label("Method");
    private static final Label LABEL_METHOD_CALL = Label.label("MethodCall");
    private static final Label LABEL_CALL_SITE = Label.label("CallSite");
    private static final Label LABEL_GRAPH_NODE = Label.label("GraphNode");
    private static final Label LABEL_METHOD_NODE = Label.label("MethodNode");
    private static final Label LABEL_CALL_SITE_NODE = Label.label("CallSiteNode");

    private static final RelationshipType REL_CALLS = RelationshipType.withName("CALLS");
    private static final RelationshipType REL_GRAPH_EDGE = RelationshipType.withName("GRAPH_EDGE");

    private final Neo4jStore store;

    public Neo4jNativeBulkWriter(Neo4jStore store) {
        this.store = store;
    }

    public void upsertMethods(List<Map<String, Object>> rows, long timeoutMs) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        store.write(timeoutMs, tx -> {
            Map<String, Node> methodCache = new HashMap<>(rows.size() * 2);
            Map<Long, Node> graphNodeCache = new HashMap<>(rows.size() * 2);
            for (Map<String, Object> row : rows) {
                if (row == null) {
                    continue;
                }
                String className = safe(row.get("className"));
                String methodName = safe(row.get("methodName"));
                String methodDesc = safe(row.get("methodDesc"));
                int jarId = asInt(row.get("jarId"), -1);
                if (className.isEmpty() || methodName.isEmpty() || methodDesc.isEmpty()) {
                    continue;
                }
                String methodKey = safe(row.get("methodKey"));
                if (methodKey.isEmpty()) {
                    methodKey = methodKey(className, methodName, methodDesc, jarId);
                }

                Node method = methodCache.get(methodKey);
                if (method == null) {
                    method = tx.findNode(LABEL_METHOD, "methodKey", methodKey);
                    if (method == null) {
                        method = tx.createNode(LABEL_METHOD);
                        method.setProperty("methodKey", methodKey);
                    }
                    methodCache.put(methodKey, method);
                }

                long mid = asLong(row.get("mid"), -1L);
                long graphNodeId = asLong(row.get("graphNodeId"), -1L);
                if (mid > 0L) {
                    setIfMissingLong(method, "mid", mid);
                }
                if (graphNodeId > 0L) {
                    setIfMissingLong(method, "graphNodeId", graphNodeId);
                }
                method.setProperty("className", className);
                method.setProperty("methodName", methodName);
                method.setProperty("methodDesc", methodDesc);
                method.setProperty("jarId", jarId);
                method.setProperty("jarName", safe(row.get("jarName")));
                method.setProperty("isStaticInt", asInt(row.get("isStaticInt"), 0));
                method.setProperty("accessInt", asInt(row.get("accessInt"), 0));
                method.setProperty("lineNumber", asInt(row.get("lineNumber"), -1));

                if (graphNodeId > 0L) {
                    Node graphNode = graphNodeCache.get(graphNodeId);
                    if (graphNode == null) {
                        graphNode = ensureGraphNode(tx, graphNodeId);
                        graphNode.addLabel(LABEL_METHOD_NODE);
                        graphNodeCache.put(graphNodeId, graphNode);
                    }
                    graphNode.setProperty("kind", "method");
                    graphNode.setProperty("jarId", jarId);
                    graphNode.setProperty("className", className);
                    graphNode.setProperty("methodName", methodName);
                    graphNode.setProperty("methodDesc", methodDesc);
                    graphNode.setProperty("callSiteKey", "");
                    graphNode.setProperty("lineNumber", asInt(row.get("lineNumber"), -1));
                    graphNode.setProperty("callIndex", -1);
                    graphNode.setProperty("buildSeq", asLong(row.get("buildSeq"), 0L));
                }
            }
            return null;
        });
    }

    public void upsertMethodCalls(List<Map<String, Object>> rows, long timeoutMs) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        store.write(timeoutMs, tx -> {
            Map<String, Node> methodCache = new HashMap<>(rows.size() * 2);
            Map<Long, Node> graphNodeCache = new HashMap<>(rows.size() * 2);
            for (Map<String, Object> row : rows) {
                if (row == null) {
                    continue;
                }
                Node caller = ensureMethodNode(tx, methodCache, row, "caller");
                Node callee = ensureMethodNode(tx, methodCache, row, "callee");
                if (caller == null || callee == null) {
                    continue;
                }

                Relationship calls = caller.createRelationshipTo(callee, REL_CALLS);
                calls.setProperty("callSiteKey", safe(row.get("callSiteKey")));
                calls.setProperty("opCode", asInt(row.get("opCode"), -1));
                calls.setProperty("calleeClass", safe(row.get("calleeClass")));
                calls.setProperty("calleeMethod", safe(row.get("calleeMethod")));
                calls.setProperty("calleeDesc", safe(row.get("calleeDesc")));
                calls.setProperty("calleeJarId", asInt(row.get("calleeJarId"), -1));
                calls.setProperty("edgeType", safe(row.get("edgeType")));
                calls.setProperty("confidence", safe(row.get("confidence")));
                calls.setProperty("evidence", safe(row.get("evidence")));
                calls.setProperty("updatedAt", System.currentTimeMillis());

                long callId = asLong(row.get("callId"), -1L);
                if (callId > 0L) {
                    Node methodCall = tx.findNode(LABEL_METHOD_CALL, "callId", callId);
                    if (methodCall == null) {
                        methodCall = tx.createNode(LABEL_METHOD_CALL);
                        methodCall.setProperty("callId", callId);
                    }
                    methodCall.setProperty("callerClassName", safe(row.get("callerClass")));
                    methodCall.setProperty("callerMethodName", safe(row.get("callerMethod")));
                    methodCall.setProperty("callerMethodDesc", safe(row.get("callerDesc")));
                    methodCall.setProperty("callerJarId", asInt(row.get("callerJarId"), -1));
                    methodCall.setProperty("calleeClassName", safe(row.get("calleeClass")));
                    methodCall.setProperty("calleeMethodName", safe(row.get("calleeMethod")));
                    methodCall.setProperty("calleeMethodDesc", safe(row.get("calleeDesc")));
                    methodCall.setProperty("calleeJarId", asInt(row.get("calleeJarId"), -1));
                    methodCall.setProperty("opCode", asInt(row.get("opCode"), -1));
                    methodCall.setProperty("edgeType", safe(row.get("edgeType")));
                    methodCall.setProperty("edgeConfidence", safe(row.get("confidence")));
                    methodCall.setProperty("edgeEvidence", safe(row.get("evidence")));
                    methodCall.setProperty("callSiteKey", safe(row.get("callSiteKey")));
                }

                long callerGraphNodeId = asLong(row.get("callerGraphNodeId"), -1L);
                long calleeGraphNodeId = asLong(row.get("calleeGraphNodeId"), -1L);
                if (callerGraphNodeId <= 0L || calleeGraphNodeId <= 0L) {
                    continue;
                }
                Node g1 = graphNodeCache.computeIfAbsent(callerGraphNodeId, key -> ensureGraphNode(tx, key));
                Node g2 = graphNodeCache.computeIfAbsent(calleeGraphNodeId, key -> ensureGraphNode(tx, key));
                g1.addLabel(LABEL_METHOD_NODE);
                g2.addLabel(LABEL_METHOD_NODE);
                g1.setProperty("kind", "method");
                g1.setProperty("jarId", asInt(row.get("callerJarId"), -1));
                g1.setProperty("className", safe(row.get("callerClass")));
                g1.setProperty("methodName", safe(row.get("callerMethod")));
                g1.setProperty("methodDesc", safe(row.get("callerDesc")));
                g1.setProperty("callSiteKey", "");
                g1.setProperty("lineNumber", -1);
                g1.setProperty("callIndex", -1);
                g1.setProperty("buildSeq", asLong(row.get("buildSeq"), 0L));

                g2.setProperty("kind", "method");
                g2.setProperty("jarId", asInt(row.get("calleeJarId"), -1));
                g2.setProperty("className", safe(row.get("calleeClass")));
                g2.setProperty("methodName", safe(row.get("calleeMethod")));
                g2.setProperty("methodDesc", safe(row.get("calleeDesc")));
                g2.setProperty("callSiteKey", "");
                g2.setProperty("lineNumber", -1);
                g2.setProperty("callIndex", -1);
                g2.setProperty("buildSeq", asLong(row.get("buildSeq"), 0L));

                Relationship graphEdge = g1.createRelationshipTo(g2, REL_GRAPH_EDGE);
                graphEdge.setProperty("edgeId", asLong(row.get("edgeId"), -1L));
                graphEdge.setProperty("relType", safe(row.get("relType")));
                graphEdge.setProperty("confidence", safe(row.get("confidence")));
                graphEdge.setProperty("evidence", safe(row.get("evidence")));
                graphEdge.setProperty("opCode", asInt(row.get("opCode"), -1));
                graphEdge.setProperty("buildSeq", asLong(row.get("buildSeq"), 0L));
            }
            return null;
        });
    }

    public void upsertCallSites(List<Map<String, Object>> rows, long timeoutMs) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        store.write(timeoutMs, tx -> {
            Map<Long, Node> graphNodeCache = new HashMap<>(rows.size() * 2);
            for (Map<String, Object> row : rows) {
                if (row == null) {
                    continue;
                }
                String callSiteKey = safe(row.get("callSiteKey"));
                if (callSiteKey.isEmpty()) {
                    continue;
                }
                long csId = asLong(row.get("csId"), -1L);
                Node callSite = tx.findNode(LABEL_CALL_SITE, "callSiteKey", callSiteKey);
                if (callSite == null) {
                    callSite = tx.createNode(LABEL_CALL_SITE);
                    callSite.setProperty("callSiteKey", callSiteKey);
                }
                if (csId > 0L) {
                    setIfMissingLong(callSite, "csId", csId);
                }
                callSite.setProperty("callerClassName", safe(row.get("callerClassName")));
                callSite.setProperty("callerMethodName", safe(row.get("callerMethodName")));
                callSite.setProperty("callerMethodDesc", safe(row.get("callerMethodDesc")));
                callSite.setProperty("calleeOwner", safe(row.get("calleeOwner")));
                callSite.setProperty("calleeMethodName", safe(row.get("calleeMethodName")));
                callSite.setProperty("calleeMethodDesc", safe(row.get("calleeMethodDesc")));
                callSite.setProperty("opCode", asInt(row.get("opCode"), -1));
                callSite.setProperty("lineNumber", asInt(row.get("lineNumber"), -1));
                callSite.setProperty("callIndex", asInt(row.get("callIndex"), -1));
                callSite.setProperty("receiverType", safe(row.get("receiverType")));
                callSite.setProperty("jarId", asInt(row.get("jarId"), -1));

                long graphNodeId = asLong(row.get("graphNodeId"), -1L);
                if (graphNodeId <= 0L) {
                    continue;
                }
                Node graphNode = graphNodeCache.computeIfAbsent(graphNodeId, key -> ensureGraphNode(tx, key));
                graphNode.addLabel(LABEL_CALL_SITE_NODE);
                graphNode.setProperty("kind", "callsite");
                graphNode.setProperty("jarId", asInt(row.get("jarId"), -1));
                graphNode.setProperty("className", safe(row.get("callerClassName")));
                graphNode.setProperty("methodName", safe(row.get("callerMethodName")));
                graphNode.setProperty("methodDesc", safe(row.get("callerMethodDesc")));
                graphNode.setProperty("callSiteKey", callSiteKey);
                graphNode.setProperty("lineNumber", asInt(row.get("lineNumber"), -1));
                graphNode.setProperty("callIndex", asInt(row.get("callIndex"), -1));
                graphNode.setProperty("buildSeq", asLong(row.get("buildSeq"), 0L));
            }
            return null;
        });
    }

    public void upsertGraphEdges(List<Map<String, Object>> rows, long timeoutMs) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        store.write(timeoutMs, tx -> {
            Map<Long, Node> graphNodeCache = new HashMap<>(rows.size() * 2);
            for (Map<String, Object> row : rows) {
                if (row == null) {
                    continue;
                }
                long src = asLong(row.get("src"), -1L);
                long dst = asLong(row.get("dst"), -1L);
                if (src <= 0L || dst <= 0L) {
                    continue;
                }
                Node srcNode = graphNodeCache.computeIfAbsent(src, key -> ensureGraphNode(tx, key));
                Node dstNode = graphNodeCache.computeIfAbsent(dst, key -> ensureGraphNode(tx, key));
                Relationship rel = srcNode.createRelationshipTo(dstNode, REL_GRAPH_EDGE);
                rel.setProperty("edgeId", asLong(row.get("edgeId"), -1L));
                rel.setProperty("relType", safe(row.get("relType")));
                rel.setProperty("confidence", safe(row.get("confidence")));
                rel.setProperty("evidence", safe(row.get("evidence")));
                rel.setProperty("opCode", asInt(row.get("opCode"), -1));
                rel.setProperty("buildSeq", asLong(row.get("buildSeq"), 0L));
            }
            return null;
        });
    }

    private Node ensureMethodNode(Transaction tx,
                                  Map<String, Node> methodCache,
                                  Map<String, Object> row,
                                  String prefix) {
        String className = safe(row.get(prefix + "Class"));
        String methodName = safe(row.get(prefix + "Method"));
        String methodDesc = safe(row.get(prefix + "Desc"));
        int jarId = asInt(row.get(prefix + "JarId"), -1);
        if (className.isEmpty() || methodName.isEmpty() || methodDesc.isEmpty()) {
            return null;
        }
        String key = methodKey(className, methodName, methodDesc, jarId);
        Node method = methodCache.get(key);
        if (method != null) {
            return method;
        }
        method = tx.findNode(LABEL_METHOD, "methodKey", key);
        if (method == null) {
            method = tx.createNode(LABEL_METHOD);
            method.setProperty("methodKey", key);
        }
        long mid = asLong(row.get(prefix + "Mid"), -1L);
        long graphNodeId = asLong(row.get(prefix + "GraphNodeId"), -1L);
        if (mid > 0L) {
            setIfMissingLong(method, "mid", mid);
        }
        if (graphNodeId > 0L) {
            setIfMissingLong(method, "graphNodeId", graphNodeId);
        }
        method.setProperty("className", className);
        method.setProperty("methodName", methodName);
        method.setProperty("methodDesc", methodDesc);
        method.setProperty("jarId", jarId);
        method.setProperty("jarName", safe(row.get(prefix + "JarName")));
        if (!method.hasProperty("isStaticInt")) {
            method.setProperty("isStaticInt", 0);
        }
        if (!method.hasProperty("accessInt")) {
            method.setProperty("accessInt", 0);
        }
        if (!method.hasProperty("lineNumber")) {
            method.setProperty("lineNumber", -1);
        }
        methodCache.put(key, method);
        return method;
    }

    private static Node ensureGraphNode(Transaction tx, long nodeId) {
        Node node = tx.findNode(LABEL_GRAPH_NODE, "nodeId", nodeId);
        if (node == null) {
            node = tx.createNode(LABEL_GRAPH_NODE);
            node.setProperty("nodeId", nodeId);
        }
        return node;
    }

    private static void setIfMissingLong(Node node, String key, long value) {
        if (value <= 0L || node == null || key == null) {
            return;
        }
        if (!node.hasProperty(key)) {
            node.setProperty(key, value);
            return;
        }
        long current = asLong(node.getProperty(key), -1L);
        if (current <= 0L) {
            node.setProperty(key, value);
        }
    }

    private static String methodKey(String className, String methodName, String methodDesc, int jarId) {
        return className + "#" + methodName + "#" + methodDesc + "#" + jarId;
    }

    private static String safe(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static long asLong(Object value, long def) {
        if (value == null) {
            return def;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value).trim());
        } catch (Exception ignored) {
            return def;
        }
    }

    private static int asInt(Object value, int def) {
        if (value == null) {
            return def;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (Exception ignored) {
            return def;
        }
    }
}
