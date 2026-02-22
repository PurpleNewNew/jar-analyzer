/*
 * GPLv3 License
 */

package me.n1ar4.jar.analyzer.storage.neo4j.repo;

import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.util.List;

public final class Neo4jSchemaInitializer {
    private static final Logger logger = LogManager.getLogger();

    private final Neo4jWriteRepository writeRepository;

    public Neo4jSchemaInitializer(Neo4jWriteRepository writeRepository) {
        this.writeRepository = writeRepository;
    }

    public void init() {
        List<String> ddl = List.of(
                "CREATE CONSTRAINT id_sequence_name_unique IF NOT EXISTS FOR (n:IdSequence) REQUIRE n.name IS UNIQUE",
                "CREATE CONSTRAINT build_meta_name_unique IF NOT EXISTS FOR (n:BuildMeta) REQUIRE n.name IS UNIQUE",
                "CREATE CONSTRAINT jar_id_unique IF NOT EXISTS FOR (n:Jar) REQUIRE n.jid IS UNIQUE",
                "CREATE CONSTRAINT jar_abs_path_unique IF NOT EXISTS FOR (n:Jar) REQUIRE n.jarAbsPath IS UNIQUE",
                "CREATE CONSTRAINT class_id_unique IF NOT EXISTS FOR (n:Class) REQUIRE n.cid IS UNIQUE",
                "CREATE CONSTRAINT method_id_unique IF NOT EXISTS FOR (n:Method) REQUIRE n.mid IS UNIQUE",
                "CREATE CONSTRAINT method_key_unique IF NOT EXISTS FOR (n:Method) REQUIRE n.methodKey IS UNIQUE",
                "CREATE CONSTRAINT class_file_id_unique IF NOT EXISTS FOR (n:ClassFile) REQUIRE n.cfid IS UNIQUE",
                "CREATE CONSTRAINT member_id_unique IF NOT EXISTS FOR (n:Member) REQUIRE n.memberId IS UNIQUE",
                "CREATE CONSTRAINT anno_id_unique IF NOT EXISTS FOR (n:Annotation) REQUIRE n.annoId IS UNIQUE",
                "CREATE CONSTRAINT interface_id_unique IF NOT EXISTS FOR (n:InterfaceDef) REQUIRE n.interfaceId IS UNIQUE",
                "CREATE CONSTRAINT string_id_unique IF NOT EXISTS FOR (n:StringLiteral) REQUIRE n.stringId IS UNIQUE",
                "CREATE CONSTRAINT resource_id_unique IF NOT EXISTS FOR (n:Resource) REQUIRE n.rid IS UNIQUE",
                "CREATE CONSTRAINT call_site_id_unique IF NOT EXISTS FOR (n:CallSite) REQUIRE n.csId IS UNIQUE",
                "CREATE CONSTRAINT call_site_key_unique IF NOT EXISTS FOR (n:CallSite) REQUIRE n.callSiteKey IS UNIQUE",
                "CREATE CONSTRAINT local_var_id_unique IF NOT EXISTS FOR (n:LocalVar) REQUIRE n.lvId IS UNIQUE",
                "CREATE CONSTRAINT line_mapping_id_unique IF NOT EXISTS FOR (n:LineMapping) REQUIRE n.mapId IS UNIQUE",
                "CREATE CONSTRAINT graph_node_id_unique IF NOT EXISTS FOR (n:GraphNode) REQUIRE n.nodeId IS UNIQUE",
                "CREATE CONSTRAINT cypher_script_id_unique IF NOT EXISTS FOR (n:SavedCypher) REQUIRE n.scriptId IS UNIQUE",
                "CREATE CONSTRAINT vul_report_id_unique IF NOT EXISTS FOR (n:VulReport) REQUIRE n.reportId IS UNIQUE",
                "CREATE CONSTRAINT dfs_job_id_unique IF NOT EXISTS FOR (n:DfsJob) REQUIRE n.id IS UNIQUE",
                "CREATE CONSTRAINT dfs_path_id_unique IF NOT EXISTS FOR (n:DfsPath) REQUIRE n.id IS UNIQUE",
                "CREATE INDEX class_signature_lookup IF NOT EXISTS FOR (n:Class) ON (n.className, n.jarId)",
                "CREATE INDEX method_signature_lookup IF NOT EXISTS FOR (n:Method) ON (n.className, n.methodName, n.methodDesc, n.jarId)",
                "CREATE INDEX method_class_lookup IF NOT EXISTS FOR (n:Method) ON (n.className, n.jarId)",
                "CREATE INDEX method_callsite_lookup IF NOT EXISTS FOR (n:MethodCall) ON (n.callSiteKey)",
                "CREATE INDEX annotation_lookup IF NOT EXISTS FOR (n:Annotation) ON (n.className, n.methodName, n.annoName, n.jarId)",
                "CREATE INDEX resource_lookup IF NOT EXISTS FOR (n:Resource) ON (n.resourcePath, n.jarId)",
                "CREATE INDEX project_entry_lookup IF NOT EXISTS FOR (n:ProjectEntry) ON (n.buildSeq, n.entryKind, n.originKind, n.entryPath)",
                "CREATE INDEX project_root_lookup IF NOT EXISTS FOR (n:ProjectRoot) ON (n.buildSeq, n.rootKind, n.originKind, n.rootPath)",
                "CREATE FULLTEXT INDEX string_literal_fulltext IF NOT EXISTS FOR (n:StringLiteral) ON EACH [n.value]"
        );
        for (String statement : ddl) {
            try {
                writeRepository.run(statement);
            } catch (Exception ex) {
                logger.debug("neo4j schema init statement failed: {} -> {}", statement, ex.toString());
            }
        }
    }
}
