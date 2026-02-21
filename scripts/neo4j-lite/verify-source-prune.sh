#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
JAVA_SRC="${ROOT_DIR}/src/main/java/org/neo4j"
SCALA_SRC="${ROOT_DIR}/src/main/scala/org/neo4j"
JAVA_ROOT="${ROOT_DIR}/src/main/java"
FULLTEXT_SRC_DIR="${JAVA_SRC}/kernel/api/impl/fulltext"
VECTOR_SRC_DIR="${JAVA_SRC}/kernel/api/impl/schema/vector"
KNN_SERVICE_FILE="${ROOT_DIR}/src/main/resources/META-INF/services/org.apache.lucene.codecs.KnnVectorsFormat"
ANALYZER_SERVICE_FILE="${ROOT_DIR}/src/main/resources/META-INF/services/org.neo4j.graphdb.schema.AnalyzerProvider"
FACTORY_FILE="${JAVA_SRC}/graphdb/facade/DatabaseManagementServiceFactory.java"
ABSTRACT_EDITION_FILE="${JAVA_SRC}/graphdb/factory/module/edition/AbstractEditionModule.java"
COMMUNITY_EDITION_FILE="${JAVA_SRC}/graphdb/factory/module/edition/CommunityEditionModule.java"
DBMS_DIAG_FILE="${JAVA_SRC}/kernel/diagnostics/providers/DbmsDiagnosticsManager.java"
HEAP_DUMP_FILE="${JAVA_SRC}/internal/diagnostics/HeapDumpDiagnostics.java"
VALIDATORS_FILE="${JAVA_SRC}/kernel/impl/util/Validators.java"
REMOVED_BUILTIN_PROC_FILES=(
  BuiltInProcedures
  CapabilityResult
  ConfigResult
  ConnectionTerminationFailedResult
  ConnectionTerminationResult
  IndexProcedures
  ListComponentsProcedure
  ListConnectionResult
  NodePropertySchemaInfoResult
  ProceduresTimeFormatHelper
  QueryId
  RelationshipPropertySchemaInfoResult
  SchemaCalculator
  SchemaProcedure
  SortedLabels
  TokenProcedures
  TransactionMarkForTerminationFailedResult
  TransactionMarkForTerminationResult
)

for d in "${JAVA_SRC}" "${SCALA_SRC}"; do
  if [[ ! -d "${d}" ]]; then
    echo "[neo4j-lite] missing source tree: ${d}" >&2
    exit 1
  fi
done

if [[ -f "${JAVA_ROOT}/Cypher5Lexer.tokens" || -f "${JAVA_ROOT}/Cypher5Parser.tokens" || -f "${JAVA_ROOT}/Cypher25Lexer.tokens" || -f "${JAVA_ROOT}/Cypher25Parser.tokens" ]]; then
  echo "[neo4j-lite] generated parser token files must not be committed under src/main/java" >&2
  find "${JAVA_ROOT}" -maxdepth 1 -type f \( -name 'Cypher5Lexer.tokens' -o -name 'Cypher5Parser.tokens' -o -name 'Cypher25Lexer.tokens' -o -name 'Cypher25Parser.tokens' \) -print >&2
  exit 1
fi

for cls in "${REMOVED_BUILTIN_PROC_FILES[@]}"; do
  if [[ -f "${JAVA_SRC}/procedure/builtin/${cls}.java" ]]; then
    echo "[neo4j-lite] removed builtin procedure residue leaked back in: org/neo4j/procedure/builtin/${cls}.java" >&2
    exit 1
  fi
done

banned_find_expr=(
  -path "*/org/neo4j/server/*" -o
  -path "*/org/neo4j/bolt/*" -o
  -path "*/org/neo4j/fabric/*" -o
  -path "*/org/neo4j/router/*" -o
  -path "*/org/neo4j/importer/*" -o
  -path "*/org/neo4j/udc/*" -o
  -path "*/org/neo4j/cloud/*" -o
  -path "*/org/neo4j/cli/*" -o
  -path "*/org/neo4j/commandline/*" -o
  -path "*/org/neo4j/dbms/routing/*" -o
  -path "*/org/neo4j/dbms/admissioncontrol/*" -o
  -path "*/org/neo4j/dbms/archive/*" -o
  -path "*/org/neo4j/dbms/diagnostics/profile/*" -o
  -path "*/org/neo4j/graphdb/factory/module/edition/migration/*" -o
  -path "*/org/neo4j/logging/event/*" -o
  -path "*/org/neo4j/graphalgo/*" -o
  -path "*/org/neo4j/csv/*" -o
  -path "*/org/neo4j/batchimport/*" -o
  -path "*/org/neo4j/internal/batchimport/*" -o
  -path "*/org/neo4j/consistency/*" -o
  -path "*/org/neo4j/dbms/diagnostics/jmx/*" -o
  -path "*/org/neo4j/cypher/internal/profiling/*" -o
  -path "*/org/neo4j/cypher/internal/parser/javacc/*" -o
  -path "*/org/neo4j/cypher/internal/ir/converters/QuantifiedPathPatternConverters.*" -o
  -path "*/org/neo4j/cypher/internal/AdministrationShowCommandUtils.*" -o
  -path "*/org/neo4j/cypher/internal/planner/spi/AdministrationPlannerName.*" -o
  -path "*/org/neo4j/cypher/internal/ast/AdministrationCommand.*" -o
  -path "*/org/neo4j/cypher/internal/ast/factory/ASTAdministrationFactory.*" -o
  -path "*/org/neo4j/cypher/internal/ast/factory/neo4j/UnsupportedAdministrationAstSupport.*" -o
  -path "*/org/neo4j/cypher/internal/compiler/AdministrationCommandPlanBuilder.*" -o
  -path "*/org/neo4j/cypher/internal/compiler/SchemaCommandPlanBuilder.*" -o
  -path "*/org/neo4j/cypher/internal/logical/plans/AdministrationCommandLogicalPlan.*" -o
  -path "*/org/neo4j/cypher/internal/logical/plans/SystemProcedureCall.*" -o
  -path "*/org/neo4j/cypher/internal/logical/plans/PrivilegeCommandScope.*" -o
  -path "*/org/neo4j/cypher/internal/ast/AdministrationAction.*" -o
  -path "*/org/neo4j/cypher/internal/ast/ActionResource.*" -o
  -path "*/org/neo4j/cypher/internal/ast/PrivilegeQualifier.*" -o
  -path "*/org/neo4j/cypher/internal/ast/RevokeType.*" -o
  -path "*/org/neo4j/cypher/internal/ast/Scope.*" -o
  -path "*/org/neo4j/cypher/internal/ast/PrivilegeType.*" -o
  -path "*/org/neo4j/cypher/internal/ast/ShowConstraintTypes.*" -o
  -path "*/org/neo4j/cypher/internal/ast/ShowFunctionTypes.*" -o
  -path "*/org/neo4j/cypher/internal/ast/ShowIndexTypes.*" -o
  -path "*/org/neo4j/cypher/internal/ast/ShowExecutableBy.*" -o
  -path "*/org/neo4j/cypher/internal/ast/ShowColumn.*" -o
  -path "*/org/neo4j/cypher/internal/ast/SchemaCommand.*" -o
  -path "*/org/neo4j/cypher/internal/ast/CreateIndexTypes.*" -o
  -path "*/org/neo4j/cypher/internal/ast/CreateConstraintTypes.*" -o
  -path "*/org/neo4j/cypher/internal/ast/package.*" -o
  -path "*/org/neo4j/cypher/internal/AliasMapSettingsEvaluator.*" -o
  -path "*/org/neo4j/cypher/internal/evaluator/StaticEvaluation.*" -o
  -path "*/org/neo4j/cypher/internal/runtime/memory/HighWaterMarkScopedMemoryTracker.*" -o
  -path "*/org/neo4j/cypher/internal/parser/v5/ast/factory/DdlBuilder.*" -o
  -path "*/org/neo4j/cypher/internal/parser/v5/ast/factory/DdlCreateBuilder.*" -o
  -path "*/org/neo4j/cypher/internal/parser/v5/ast/factory/DdlPrivilegeBuilder.*" -o
  -path "*/org/neo4j/cypher/internal/parser/v5/ast/factory/DdlShowBuilder.*" -o
  -path "*/org/neo4j/cypher/internal/parser/common/ast/factory/ShowCommandFilterTypes.*" -o
  -path "*/org/neo4j/cypher/internal/rewriting/rewriters/rewriteShowQuery.*" -o
  -path "*/org/neo4j/cypher/internal/rewriting/rewriters/expandShowWhere.*" -o
  -path "*/org/neo4j/cypher/internal/runtime/interpreted/*" -o
  -path "*/org/neo4j/cypher/internal/SchemaCommandRuntime.*" -o
  -path "*/org/neo4j/cypher/internal/procs/*" -o
  -path "*/org/neo4j/cypher/operations/GraphFunctions.*" -o
  -path "*/org/neo4j/values/virtual/GraphReferenceValue.*" -o
  -path "*/org/neo4j/kernel/api/impl/fulltext/FulltextAdapter.*" -o
  -path "*/org/neo4j/kernel/api/impl/fulltext/DefaultFulltextAdapter.*" -o
  -path "*/org/neo4j/procedure/builtin/FulltextProcedures.*" -o
  -path "*/org/neo4j/procedure/builtin/VectorIndexProcedures.*" -o
  -path "*/org/neo4j/procedure/builtin/JmxQueryProcedure.*" -o
  -path "*/org/neo4j/kernel/impl/index/schema/FulltextIndexProviderFactory.*" -o
  -path "*/org/neo4j/kernel/impl/index/schema/VectorIndexProviderFactory.*" -o
  -path "*/org/neo4j/cypher/internal/optionsmap/CreateFulltextIndexOptionsConverter.*" -o
  -path "*/org/neo4j/cypher/internal/optionsmap/CreateVectorIndexOptionsConverter.*" -o
  -path "*/org/neo4j/kernel/impl/storemigration/SchemaStore44MigrationUtil.*" -o
  -path "*/org/neo4j/storageengine/migration/MigrationProgressMonitor.*" -o
  -path "*/org/neo4j/exceptions/DatabaseAdministrationException.*" -o
  -path "*/org/neo4j/exceptions/DatabaseAdministrationOnFollowerException.*" -o
  -path "*/org/neo4j/exceptions/InvalidTargetDatabaseException.*" -o
  -path "*/org/neo4j/exceptions/NotSystemDatabaseException.*" -o
  -path "*/org/neo4j/kernel/api/impl/schema/trigram/*" -o
  -path "*/org/neo4j/kernel/api/impl/index/backup/*" -o
  -path "*/org/neo4j/kernel/impl/traversal/*" -o
  -path "*/org/neo4j/cypher/internal/expressions/functions/File.*" -o
  -path "*/org/neo4j/cypher/internal/expressions/functions/Linenumber.*" -o
  -path "*/org/neo4j/cypher/internal/runtime/core/commands/expressions/LineFunction.*" -o
  -path "*/org/neo4j/cypher/internal/runtime/core/pipes/ExternalCSVResource.*" -o
  -path "*/org/neo4j/cypher/internal/logical/plans/SchemaLogicalPlan.*" -o
  -path "*/org/neo4j/cypher/internal/util/DummyPosition.*"
)

if find "${JAVA_SRC}" "${SCALA_SRC}" -type f \( "${banned_find_expr[@]}" \) | rg . >/dev/null; then
  echo "[neo4j-lite] banned source namespace remains in src/main/{java,scala}" >&2
  find "${JAVA_SRC}" "${SCALA_SRC}" -type f \( "${banned_find_expr[@]}" \) | head -n 120 >&2
  exit 1
fi

if [[ -d "${FULLTEXT_SRC_DIR}" ]]; then
  echo "[neo4j-lite] fulltext source package should be fully removed: ${FULLTEXT_SRC_DIR}" >&2
  find "${FULLTEXT_SRC_DIR}" -type f | head -n 120 >&2
  exit 1
fi

if [[ -d "${VECTOR_SRC_DIR}" ]]; then
  if find "${VECTOR_SRC_DIR}" -type f -name '*.java' \
    ! -name 'VectorSimilarity.java' \
    ! -name 'VectorSimilarityFunctions.java' | rg . >/dev/null; then
    echo "[neo4j-lite] unexpected vector source files remain (only VectorSimilarity* are allowed)" >&2
    find "${VECTOR_SRC_DIR}" -type f -name '*.java' \
      ! -name 'VectorSimilarity.java' \
      ! -name 'VectorSimilarityFunctions.java' | head -n 120 >&2
    exit 1
  fi
fi

if [[ -f "${KNN_SERVICE_FILE}" ]]; then
  echo "[neo4j-lite] vector codec service loader file must be removed: ${KNN_SERVICE_FILE}" >&2
  exit 1
fi

if [[ -f "${ANALYZER_SERVICE_FILE}" ]]; then
  echo "[neo4j-lite] fulltext analyzer service loader file must be removed: ${ANALYZER_SERVICE_FILE}" >&2
  exit 1
fi

if rg -n 'case class RunQueryAtProjection\b|case class RunQueryAt\b' "${SCALA_SRC}" >/dev/null; then
  echo "[neo4j-lite] RunQueryAt types reintroduced in source tree" >&2
  rg -n 'case class RunQueryAtProjection\b|case class RunQueryAt\b' "${SCALA_SRC}" >&2
  exit 1
fi

if rg -n 'org\.neo4j\.bolt|BoltServer|Netty4LoggerFactory|TransactionManagerImpl|CommunityNeoWebServer|CommunityQueryRouterBootstrap|org\.neo4j\.server\.security' \
  "${FACTORY_FILE}" "${ABSTRACT_EDITION_FILE}" "${COMMUNITY_EDITION_FILE}" >/dev/null; then
  echo "[neo4j-lite] banned runtime hooks remain in embedded source path" >&2
  rg -n 'org\.neo4j\.bolt|BoltServer|Netty4LoggerFactory|TransactionManagerImpl|CommunityNeoWebServer|CommunityQueryRouterBootstrap|org\.neo4j\.server\.security' \
    "${FACTORY_FILE}" "${ABSTRACT_EDITION_FILE}" "${COMMUNITY_EDITION_FILE}" >&2
  exit 1
fi

if rg -n 'getFulltextProvider|getVectorIndexProvider' \
  "${JAVA_SRC}/kernel/impl/api/index/IndexProviderMap.java" \
  "${JAVA_SRC}/kernel/impl/api/index/IndexingProvidersService.java" >/dev/null; then
  echo "[neo4j-lite] fulltext/vector provider API leaked back into index interfaces" >&2
  rg -n 'getFulltextProvider|getVectorIndexProvider' \
    "${JAVA_SRC}/kernel/impl/api/index/IndexProviderMap.java" \
    "${JAVA_SRC}/kernel/impl/api/index/IndexingProvidersService.java" >&2
  exit 1
fi

if rg -n 'CreateFulltextIndex|DoNothingIfExistsForFulltextIndex' \
  "${SCALA_SRC}/cypher/internal/plandescription/LogicalPlan2PlanDescription.scala" >/dev/null; then
  echo "[neo4j-lite] fulltext schema logical plan residue leaked back in" >&2
  rg -n 'CreateFulltextIndex|DoNothingIfExistsForFulltextIndex' \
    "${SCALA_SRC}/cypher/internal/plandescription/LogicalPlan2PlanDescription.scala" >&2
  exit 1
fi

if rg -n 'case class CommandProjection\(' \
  "${SCALA_SRC}/cypher/internal/ir/QueryHorizon.scala" >/dev/null; then
  echo "[neo4j-lite] command projection horizon leaked back in" >&2
  rg -n 'case class CommandProjection\(' \
    "${SCALA_SRC}/cypher/internal/ir/QueryHorizon.scala" >&2
  exit 1
fi

if rg -n 'case c: CommandClause =>|addCommandClauseToLogicalPlanInput\(' \
  "${SCALA_SRC}/cypher/internal/compiler/ast/convert/plannerQuery/ClauseConverters.scala" >/dev/null; then
  echo "[neo4j-lite] command clause conversion path leaked back in" >&2
  rg -n 'case c: CommandClause =>|addCommandClauseToLogicalPlanInput\(' \
    "${SCALA_SRC}/cypher/internal/compiler/ast/convert/plannerQuery/ClauseConverters.scala" >&2
  exit 1
fi

if rg -n 'planCommand\(' \
  "${SCALA_SRC}/cypher/internal/compiler/planner/logical/PlanEventHorizon.scala" \
  "${SCALA_SRC}/cypher/internal/compiler/planner/logical/steps/LogicalPlanProducer.scala" >/dev/null; then
  echo "[neo4j-lite] command planning entry leaked back in" >&2
  rg -n 'planCommand\(' \
    "${SCALA_SRC}/cypher/internal/compiler/planner/logical/PlanEventHorizon.scala" \
    "${SCALA_SRC}/cypher/internal/compiler/planner/logical/steps/LogicalPlanProducer.scala" >&2
  exit 1
fi

if ! rg -n 'isDisabledCommandRule|rejectDisabledCommand' \
  "${JAVA_SRC}/cypher/internal/parser/v5/AbstractCypher5AstBuilder.java" >/dev/null; then
  echo "[neo4j-lite] parser command fail-fast guard missing in AbstractCypher5AstBuilder" >&2
  exit 1
fi

if awk 'BEGIN{flag=0} /public final void exitEveryRule/{flag=1} /private static boolean isDisabledCommandRule/{flag=0} {if(flag) print}' \
  "${JAVA_SRC}/cypher/internal/parser/v5/AbstractCypher5AstBuilder.java" | \
  rg -n 'case Cypher5Parser\.RULE_(command|createCommand|dropCommand|showCommand|showCommandYield|showBriefAndYield|showIndexCommand|showIndexesAllowBrief|showIndexesNoBrief|showConstraintCommand|constraintAllowYieldType|constraintExistType|constraintBriefAndYieldType|showConstraintsAllowBriefAndYield|showConstraintsAllowBrief|showConstraintsAllowYield|showProcedures|showFunctions|showFunctionsType|showTransactions|terminateTransactions|showSettings|commandOptions|terminateCommand|composableCommandClauses|composableShowCommandClauses|namesAndClauses|stringsOrExpression|commandNodePattern|commandRelPattern|commandNameExpression|createConstraint|dropConstraint|createIndex|oldCreateIndex|createIndex_|createFulltextIndex|createLookupIndex|dropIndex|alterCommand|renameCommand|grantCommand|denyCommand|revokeCommand|enableServerCommand|alterServer|renameServer|dropServer|showServers|allocationCommand|deallocateDatabaseFromServers|reallocateDatabases|createRole|dropRole|renameRole|showRoles|grantRole|revokeRole|createUser|dropUser|renameUser|alterCurrentUser|alterUser|showUsers|showCurrentUser|showSupportedPrivileges|showPrivileges|showRolePrivileges|showUserPrivileges|createPrivilege|dropPrivilege|createDatabase|createCompositeDatabase|dropDatabase|alterDatabase|startDatabase|stopDatabase|showDatabase|createAlias|dropAlias|alterAlias|showAliases)\b' >/dev/null; then
  echo "[neo4j-lite] disabled command parser rules leaked back into AbstractCypher5AstBuilder switch dispatch" >&2
  awk 'BEGIN{flag=0} /public final void exitEveryRule/{flag=1} /private static boolean isDisabledCommandRule/{flag=0} {if(flag) print}' \
    "${JAVA_SRC}/cypher/internal/parser/v5/AbstractCypher5AstBuilder.java" | \
    rg -n 'case Cypher5Parser\.RULE_(command|createCommand|dropCommand|showCommand|showCommandYield|showBriefAndYield|showIndexCommand|showIndexesAllowBrief|showIndexesNoBrief|showConstraintCommand|constraintAllowYieldType|constraintExistType|constraintBriefAndYieldType|showConstraintsAllowBriefAndYield|showConstraintsAllowBrief|showConstraintsAllowYield|showProcedures|showFunctions|showFunctionsType|showTransactions|terminateTransactions|showSettings|commandOptions|terminateCommand|composableCommandClauses|composableShowCommandClauses|namesAndClauses|stringsOrExpression|commandNodePattern|commandRelPattern|commandNameExpression|createConstraint|dropConstraint|createIndex|oldCreateIndex|createIndex_|createFulltextIndex|createLookupIndex|dropIndex|alterCommand|renameCommand|grantCommand|denyCommand|revokeCommand|enableServerCommand|alterServer|renameServer|dropServer|showServers|allocationCommand|deallocateDatabaseFromServers|reallocateDatabases|createRole|dropRole|renameRole|showRoles|grantRole|revokeRole|createUser|dropUser|renameUser|alterCurrentUser|alterUser|showUsers|showCurrentUser|showSupportedPrivileges|showPrivileges|showRolePrivileges|showUserPrivileges|createPrivilege|dropPrivilege|createDatabase|createCompositeDatabase|dropDatabase|alterDatabase|startDatabase|stopDatabase|showDatabase|createAlias|dropAlias|alterAlias|showAliases)\b' >&2
  exit 1
fi

if rg -n -F 'override def exitCommand(ctx: Cypher5Parser.CommandContext): Unit = {}' \
  "${SCALA_SRC}/cypher/internal/parser/v5/ast/factory/Cypher5DisabledCommandNoOpBuilder.scala" >/dev/null || \
   rg -n -F 'override def exitComposableCommandClauses(ctx: Cypher5Parser.ComposableCommandClausesContext): Unit = {}' \
  "${SCALA_SRC}/cypher/internal/parser/v5/ast/factory/Cypher5DisabledCommandNoOpBuilder.scala" >/dev/null || \
   rg -n -F 'override def exitComposableShowCommandClauses(ctx: Cypher5Parser.ComposableShowCommandClausesContext): Unit = {}' \
  "${SCALA_SRC}/cypher/internal/parser/v5/ast/factory/Cypher5DisabledCommandNoOpBuilder.scala" >/dev/null || \
   rg -n -F 'override def exitCommandNameExpression(ctx: Cypher5Parser.CommandNameExpressionContext): Unit = {}' \
  "${SCALA_SRC}/cypher/internal/parser/v5/ast/factory/Cypher5DisabledCommandNoOpBuilder.scala" >/dev/null; then
  echo "[neo4j-lite] parser command clause handlers leaked back to no-op bodies" >&2
  rg -n 'exitCommand\\(|exitComposableCommandClauses\\(|exitComposableShowCommandClauses\\(|exitCommandNameExpression\\(' \
    "${SCALA_SRC}/cypher/internal/parser/v5/ast/factory/Cypher5DisabledCommandNoOpBuilder.scala" >&2
  exit 1
fi

if rg -n '\bCommandLogicalPlan\b' \
  "${SCALA_SRC}/cypher/internal/logical/plans/LogicalPlan.scala" \
  "${SCALA_SRC}/cypher/internal/compiler/phases/ValidateAvailableSymbols.scala" \
  "${SCALA_SRC}/cypher/internal/compiler/planner/logical/plans/rewriter/eager/ReadFinder.scala" \
  "${SCALA_SRC}/cypher/internal/physicalplanning/SlotAllocation.scala" >/dev/null; then
  echo "[neo4j-lite] command logical-plan marker leaked back in" >&2
  rg -n '\bCommandLogicalPlan\b' \
    "${SCALA_SRC}/cypher/internal/logical/plans/LogicalPlan.scala" \
    "${SCALA_SRC}/cypher/internal/compiler/phases/ValidateAvailableSymbols.scala" \
    "${SCALA_SRC}/cypher/internal/compiler/planner/logical/plans/rewriter/eager/ReadFinder.scala" \
    "${SCALA_SRC}/cypher/internal/physicalplanning/SlotAllocation.scala" >&2
  exit 1
fi

if rg -n 'turnYieldToWith\(' \
  "${JAVA_SRC}/cypher/internal/ast/factory/ASTFactory.java" \
  "${SCALA_SRC}/cypher/internal/ast/factory/neo4j/Neo4jASTFactory.scala" >/dev/null; then
  echo "[neo4j-lite] command yield-to-with adapter leaked back in" >&2
  rg -n 'turnYieldToWith\(' \
    "${JAVA_SRC}/cypher/internal/ast/factory/ASTFactory.java" \
    "${SCALA_SRC}/cypher/internal/ast/factory/neo4j/Neo4jASTFactory.scala" >&2
  exit 1
fi

if rg -n '\b(createConstraint|dropConstraint|createLookupIndex|createIndex|createFulltextIndex|dropIndex)\(' \
  "${JAVA_SRC}/cypher/internal/ast/factory/ASTFactory.java" \
  "${SCALA_SRC}/cypher/internal/ast/factory/neo4j/Neo4jASTFactory.scala" >/dev/null; then
  echo "[neo4j-lite] schema-command AST factory methods leaked back in" >&2
  rg -n '\b(createConstraint|dropConstraint|createLookupIndex|createIndex|createFulltextIndex|dropIndex)\(' \
    "${JAVA_SRC}/cypher/internal/ast/factory/ASTFactory.java" \
    "${SCALA_SRC}/cypher/internal/ast/factory/neo4j/Neo4jASTFactory.scala" >&2
  exit 1
fi

if rg -n 'internal\\.dbms\\.show_setting|internal\\.dbms\\.composable_commands|internal\\.cypher\\.pipelined_interpreted_pipes_fallback' \
  "${JAVA_SRC}/configuration/GraphDatabaseInternalSettings.java" \
  "${JAVA_SRC}/configuration/SettingMigrators.java" >/dev/null; then
  echo "[neo4j-lite] removed command/runtime fallback toggles leaked back into configuration sources" >&2
  rg -n 'internal\\.dbms\\.show_setting|internal\\.dbms\\.composable_commands|internal\\.cypher\\.pipelined_interpreted_pipes_fallback' \
    "${JAVA_SRC}/configuration/GraphDatabaseInternalSettings.java" \
    "${JAVA_SRC}/configuration/SettingMigrators.java" >&2
  exit 1
fi

if rg -n 'sealed trait CommandClause|sealed trait CommandClauseWithNames|sealed trait ClauseAllowedOnSystem|sealed trait CommandClauseAllowedOnSystem|case class CommandResultItem|case class ShowAndTerminateColumn' \
  "${SCALA_SRC}/cypher/internal/ast/Clause.scala" >/dev/null; then
  echo "[neo4j-lite] legacy command clause AST entities leaked back into Clause.scala" >&2
  rg -n 'sealed trait CommandClause|sealed trait CommandClauseWithNames|sealed trait ClauseAllowedOnSystem|sealed trait CommandClauseAllowedOnSystem|case class CommandResultItem|case class ShowAndTerminateColumn' \
    "${SCALA_SRC}/cypher/internal/ast/Clause.scala" >&2
  exit 1
fi

if rg -n 'CommandClause' \
  "${SCALA_SRC}/cypher/internal/ast/Query.scala" >/dev/null; then
  echo "[neo4j-lite] command semantic checks leaked back into Query.scala" >&2
  rg -n 'CommandClause' \
    "${SCALA_SRC}/cypher/internal/ast/Query.scala" >&2
  exit 1
fi

if rg -n 'unsupportedAdministration\(' \
  "${SCALA_SRC}/cypher/internal/ast/factory/neo4j/Neo4jASTFactory.scala" >/dev/null; then
  echo "[neo4j-lite] administration fail-fast implementation leaked back into Neo4jASTFactory.scala" >&2
  rg -n 'unsupportedAdministration\(' \
    "${SCALA_SRC}/cypher/internal/ast/factory/neo4j/Neo4jASTFactory.scala" >&2
  exit 1
fi

if [[ -f "${JAVA_SRC}/cypher/internal/ast/factory/ASTAdministrationFactory.java" ]]; then
  echo "[neo4j-lite] ASTAdministrationFactory.java should be removed" >&2
  exit 1
fi

if [[ -f "${SCALA_SRC}/cypher/internal/ast/factory/neo4j/UnsupportedAdministrationAstSupport.scala" ]]; then
  echo "[neo4j-lite] UnsupportedAdministrationAstSupport.scala should be removed" >&2
  exit 1
fi

if [[ -f "${SCALA_SRC}/cypher/internal/ast/AdministrationCommand.scala" ]]; then
  echo "[neo4j-lite] AdministrationCommand.scala should be removed" >&2
  exit 1
fi

if [[ -f "${SCALA_SRC}/cypher/internal/ast/SchemaCommand.scala" || -f "${SCALA_SRC}/cypher/internal/ast/CreateIndexTypes.scala" || -f "${SCALA_SRC}/cypher/internal/ast/CreateConstraintTypes.scala" ]]; then
  echo "[neo4j-lite] schema command/type AST sources should be removed in lite mode" >&2
  find "${SCALA_SRC}/cypher/internal/ast" -maxdepth 1 -type f \
    \( -name 'SchemaCommand.scala' -o -name 'CreateIndexTypes.scala' -o -name 'CreateConstraintTypes.scala' \) -print >&2
  exit 1
fi

if rg -n 'fulltextIndexReference\(' \
  "${SCALA_SRC}/cypher/internal/runtime/QueryContext.scala" \
  "${SCALA_SRC}/cypher/internal/runtime/core/DelegatingQueryContext.scala" \
  "${SCALA_SRC}/cypher/internal/runtime/core/TransactionBoundQueryContext.scala" \
  "${SCALA_SRC}/cypher/internal/planning/ExceptionTranslatingQueryContext.scala" >/dev/null; then
  echo "[neo4j-lite] fulltext query-context API leaked back in" >&2
  rg -n 'fulltextIndexReference\(' \
    "${SCALA_SRC}/cypher/internal/runtime/QueryContext.scala" \
    "${SCALA_SRC}/cypher/internal/runtime/core/DelegatingQueryContext.scala" \
    "${SCALA_SRC}/cypher/internal/runtime/core/TransactionBoundQueryContext.scala" \
    "${SCALA_SRC}/cypher/internal/planning/ExceptionTranslatingQueryContext.scala" >&2
  exit 1
fi

if rg -n 'NodeFulltext|RelationshipFulltext|NodeVector|RelationshipVector' \
  "${JAVA_SRC}/internal/schema/SchemaCommand.java" \
  "${JAVA_SRC}/internal/schema/SchemaTokens.java" >/dev/null; then
  echo "[neo4j-lite] internal schema command residue leaked back in (fulltext/vector command models)" >&2
  rg -n 'NodeFulltext|RelationshipFulltext|NodeVector|RelationshipVector' \
    "${JAVA_SRC}/internal/schema/SchemaCommand.java" \
    "${JAVA_SRC}/internal/schema/SchemaTokens.java" >&2
  exit 1
fi

if rg -n 'IndexType\\.FULLTEXT|IndexType\\.VECTOR' \
  "${JAVA_SRC}/internal/schema/AllIndexProviderDescriptors.java" >/dev/null; then
  echo "[neo4j-lite] fulltext/vector provider mapping leaked back into AllIndexProviderDescriptors" >&2
  rg -n 'IndexType\\.FULLTEXT|IndexType\\.VECTOR' \
    "${JAVA_SRC}/internal/schema/AllIndexProviderDescriptors.java" >&2
  exit 1
fi

if rg -n 'ShowPrivileges|ShowPrivilegeCommands|ShowSupportedPrivilegeCommand|ShowDatabase|ShowAliases|ShowCurrentUser|ShowServers|ShowUsers|ShowRoles' \
  "${SCALA_SRC}/cypher/internal/rewriting/rewriters/normalizeWithAndReturnClauses.scala" >/dev/null; then
  echo "[neo4j-lite] administration normalize branches leaked back into normalizeWithAndReturnClauses" >&2
  rg -n 'ShowPrivileges|ShowPrivilegeCommands|ShowSupportedPrivilegeCommand|ShowDatabase|ShowAliases|ShowCurrentUser|ShowServers|ShowUsers|ShowRoles' \
    "${SCALA_SRC}/cypher/internal/rewriting/rewriters/normalizeWithAndReturnClauses.scala" >&2
  exit 1
fi

if rg -n 'CreateDatabase\(|DeprecatedDatabaseNameNotification|NamespacedName\(.*, Some\(_\)\)' \
  "${SCALA_SRC}/cypher/internal/rewriting/Deprecation.scala" >/dev/null; then
  echo "[neo4j-lite] deprecated create-database namespace branch leaked back into Deprecation rewriter" >&2
  rg -n 'CreateDatabase\(|DeprecatedDatabaseNameNotification|NamespacedName\(.*, Some\(_\)\)' \
    "${SCALA_SRC}/cypher/internal/rewriting/Deprecation.scala" >&2
  exit 1
fi

if [[ -f "${SCALA_SRC}/cypher/internal/ast/AdministrationCommandSemanticAnalysis.scala" ]]; then
  echo "[neo4j-lite] AdministrationCommandSemanticAnalysis.scala should be removed" >&2
  exit 1
fi

if rg -n 'case class Topology|def extractTopology\(' \
  "${SCALA_SRC}/cypher/internal/ast/prettifier/Prettifier.scala" >/dev/null; then
  echo "[neo4j-lite] topology command formatting residue leaked back in" >&2
  rg -n 'case class Topology|def extractTopology\(' \
    "${SCALA_SRC}/cypher/internal/ast/prettifier/Prettifier.scala" >&2
  exit 1
fi

if rg -n '\bAuth\b|\bAuthAttribute\b|\bWaitUntilComplete\b|\bNoWait\b|\bIndefiniteWait\b|\bTimeoutAfter\b' \
  "${SCALA_SRC}/cypher/internal/ast/factory/neo4j/Neo4jASTFactory.scala" >/dev/null; then
  echo "[neo4j-lite] legacy auth/wait command types leaked back into administration surface" >&2
  rg -n '\bAuth\b|\bAuthAttribute\b|\bWaitUntilComplete\b|\bNoWait\b|\bIndefiniteWait\b|\bTimeoutAfter\b' \
    "${SCALA_SRC}/cypher/internal/ast/factory/neo4j/Neo4jASTFactory.scala" >&2
  exit 1
fi

if rg -n 'CoreExecutionResultBuilderFactory' \
  "${SCALA_SRC}/cypher/internal/runtime/core/CoreExecutionResultBuilderFactory.scala" \
  "${SCALA_SRC}/cypher/internal/runtime/slotted/SlottedExecutionResultBuilderFactory.scala" >/dev/null; then
  echo "[neo4j-lite] core runtime execution-result builder leaked back in" >&2
  rg -n 'CoreExecutionResultBuilderFactory' \
    "${SCALA_SRC}/cypher/internal/runtime/core/CoreExecutionResultBuilderFactory.scala" \
    "${SCALA_SRC}/cypher/internal/runtime/slotted/SlottedExecutionResultBuilderFactory.scala" >&2
  exit 1
fi

if rg -n 'ExternalCSVResource|hasLoadCSV' \
  "${SCALA_SRC}/cypher/internal/runtime/core" \
  "${SCALA_SRC}/cypher/internal/runtime/slotted" >/dev/null; then
  echo "[neo4j-lite] LOAD CSV runtime resource scaffolding leaked back in" >&2
  rg -n 'ExternalCSVResource|hasLoadCSV' \
    "${SCALA_SRC}/cypher/internal/runtime/core" \
    "${SCALA_SRC}/cypher/internal/runtime/slotted" >&2
  exit 1
fi

if rg -n 'LoadCSVProjection|planLoadCSV\(|addLoadCSVToLogicalPlanInput\(' \
  "${SCALA_SRC}/cypher/internal/ir/QueryHorizon.scala" \
  "${SCALA_SRC}/cypher/internal/compiler/ast/convert/plannerQuery/ClauseConverters.scala" \
  "${SCALA_SRC}/cypher/internal/compiler/planner/logical/PlanEventHorizon.scala" \
  "${SCALA_SRC}/cypher/internal/compiler/planner/logical/steps/LogicalPlanProducer.scala" \
  "${SCALA_SRC}/cypher/internal/compiler/planner/logical/StatisticsBackedCardinalityModel.scala" >/dev/null; then
  echo "[neo4j-lite] LOAD CSV planner-horizon residue leaked back in" >&2
  rg -n 'LoadCSVProjection|planLoadCSV\(|addLoadCSVToLogicalPlanInput\(' \
    "${SCALA_SRC}/cypher/internal/ir/QueryHorizon.scala" \
    "${SCALA_SRC}/cypher/internal/compiler/ast/convert/plannerQuery/ClauseConverters.scala" \
    "${SCALA_SRC}/cypher/internal/compiler/planner/logical/PlanEventHorizon.scala" \
    "${SCALA_SRC}/cypher/internal/compiler/planner/logical/steps/LogicalPlanProducer.scala" \
    "${SCALA_SRC}/cypher/internal/compiler/planner/logical/StatisticsBackedCardinalityModel.scala" >&2
  exit 1
fi

if rg -n 'case class LoadCSV\(' \
  "${SCALA_SRC}/cypher/internal/logical/plans/LogicalPlan.scala" >/dev/null; then
  echo "[neo4j-lite] LOAD CSV logical plan model leaked back in" >&2
  rg -n 'case class LoadCSV\(' \
    "${SCALA_SRC}/cypher/internal/logical/plans/LogicalPlan.scala" >&2
  exit 1
fi

if rg -n 'sealed trait CSVFormat|case object HasHeaders|case object NoHeaders' \
  "${SCALA_SRC}/cypher/internal/ir" >/dev/null; then
  echo "[neo4j-lite] LOAD CSV format model leaked back in" >&2
  rg -n 'sealed trait CSVFormat|case object HasHeaders|case object NoHeaders' \
    "${SCALA_SRC}/cypher/internal/ir" >&2
  exit 1
fi

if rg -n 'loadCsvClause\(' \
  "${JAVA_SRC}/cypher/internal/ast/factory/ASTFactory.java" \
  "${SCALA_SRC}/cypher/internal/ast/factory/neo4j/Neo4jASTFactory.scala" >/dev/null; then
  echo "[neo4j-lite] LOAD CSV AST factory method leaked back in" >&2
  rg -n 'loadCsvClause\(' \
    "${JAVA_SRC}/cypher/internal/ast/factory/ASTFactory.java" \
    "${SCALA_SRC}/cypher/internal/ast/factory/neo4j/Neo4jASTFactory.scala" >&2
  exit 1
fi

if rg -n 'case class CachedExpression\b|trait ExtendedExpression\b|abstract class NullSafeMathFunction\b' \
  "${SCALA_SRC}/cypher/internal/runtime/core/commands/expressions/Expression.scala" \
  "${SCALA_SRC}/cypher/internal/runtime/core/commands/expressions/MathFunction.scala" >/dev/null; then
  echo "[neo4j-lite] legacy runtime expression helper types leaked back in (cached/extended/null-safe-math)" >&2
  rg -n 'case class CachedExpression\b|trait ExtendedExpression\b|abstract class NullSafeMathFunction\b' \
    "${SCALA_SRC}/cypher/internal/runtime/core/commands/expressions/Expression.scala" \
    "${SCALA_SRC}/cypher/internal/runtime/core/commands/expressions/MathFunction.scala" >&2
  exit 1
fi

if rg -n 'AssignPrivilegeCommandHasNoEffectNotification|RevokePrivilegeCommandHasNoEffectNotification|GrantRoleCommandHasNoEffectNotification|RevokeRoleCommandHasNoEffectNotification|ImpossibleRevokeCommandWarning|ServerAlreadyEnabled|ServerAlreadyCordoned|NoDatabasesReallocated|CordonedServersExistedDuringAllocation|RequestedTopologyMatchedCurrentTopology' \
  "${SCALA_SRC}/cypher/internal/util/InternalNotification.scala" \
  "${SCALA_SRC}/notifications/NotificationWrapping.scala" >/dev/null; then
  echo "[neo4j-lite] legacy administration/cluster notification models leaked back in" >&2
  rg -n 'AssignPrivilegeCommandHasNoEffectNotification|RevokePrivilegeCommandHasNoEffectNotification|GrantRoleCommandHasNoEffectNotification|RevokeRoleCommandHasNoEffectNotification|ImpossibleRevokeCommandWarning|ServerAlreadyEnabled|ServerAlreadyCordoned|NoDatabasesReallocated|CordonedServersExistedDuringAllocation|RequestedTopologyMatchedCurrentTopology' \
    "${SCALA_SRC}/cypher/internal/util/InternalNotification.scala" \
    "${SCALA_SRC}/notifications/NotificationWrapping.scala" >&2
  exit 1
fi

if rg -n 'COMMAND_HAS_NO_EFFECT_ASSIGN_PRIVILEGE|COMMAND_HAS_NO_EFFECT_REVOKE_PRIVILEGE|COMMAND_HAS_NO_EFFECT_GRANT_ROLE|COMMAND_HAS_NO_EFFECT_REVOKE_ROLE|IMPOSSIBLE_REVOKE_COMMAND|SERVER_ALREADY_ENABLED|SERVER_ALREADY_CORDONED|NO_DATABASES_REALLOCATED|CORDONED_SERVERS_EXISTED_DURING_ALLOCATION|REQUESTED_TOPOLOGY_MATCHED_CURRENT_TOPOLOGY|commandHasNoEffectAssignPrivilege\(|commandHasNoEffectRevokePrivilege\(|commandHasNoEffectGrantRole\(|commandHasNoEffectRevokeRole\(|impossibleRevokeCommand\(|serverAlreadyEnabled\(|serverAlreadyCordoned\(|noDatabasesReallocated\(|cordonedServersExist\(|requestedTopologyMatchedCurrentTopology\(' \
  "${JAVA_SRC}/notifications/NotificationCodeWithDescription.java" >/dev/null; then
  echo "[neo4j-lite] legacy administration/cluster notification code mappings leaked back in" >&2
  rg -n 'COMMAND_HAS_NO_EFFECT_ASSIGN_PRIVILEGE|COMMAND_HAS_NO_EFFECT_REVOKE_PRIVILEGE|COMMAND_HAS_NO_EFFECT_GRANT_ROLE|COMMAND_HAS_NO_EFFECT_REVOKE_ROLE|IMPOSSIBLE_REVOKE_COMMAND|SERVER_ALREADY_ENABLED|SERVER_ALREADY_CORDONED|NO_DATABASES_REALLOCATED|CORDONED_SERVERS_EXISTED_DURING_ALLOCATION|REQUESTED_TOPOLOGY_MATCHED_CURRENT_TOPOLOGY|commandHasNoEffectAssignPrivilege\(|commandHasNoEffectRevokePrivilege\(|commandHasNoEffectGrantRole\(|commandHasNoEffectRevokeRole\(|impossibleRevokeCommand\(|serverAlreadyEnabled\(|serverAlreadyCordoned\(|noDatabasesReallocated\(|cordonedServersExist\(|requestedTopologyMatchedCurrentTopology\(' \
    "${JAVA_SRC}/notifications/NotificationCodeWithDescription.java" >&2
  exit 1
fi

if rg -n 'CommandHasNoEffect|ImpossibleRevokeCommand|ServerAlreadyEnabled|ServerAlreadyCordoned|NoDatabasesReallocated|CordonedServersExistedDuringAllocation|RequestedTopologyMatchedCurrentTopology' \
  "${JAVA_SRC}/kernel/api/exceptions/Status.java" >/dev/null; then
  echo "[neo4j-lite] legacy administration/cluster status codes leaked back in" >&2
  rg -n 'CommandHasNoEffect|ImpossibleRevokeCommand|ServerAlreadyEnabled|ServerAlreadyCordoned|NoDatabasesReallocated|CordonedServersExistedDuringAllocation|RequestedTopologyMatchedCurrentTopology' \
    "${JAVA_SRC}/kernel/api/exceptions/Status.java" >&2
  exit 1
fi

if rg -n 'STATUS_00N70|STATUS_00N71|STATUS_00N80|STATUS_00N81|STATUS_00N82|STATUS_00N83|STATUS_00N84|STATUS_01N70' \
  "${JAVA_SRC}/gqlstatus/GqlStatusInfoCodes.java" >/dev/null; then
  echo "[neo4j-lite] legacy administration/cluster GQL status codes leaked back in" >&2
  rg -n 'STATUS_00N70|STATUS_00N71|STATUS_00N80|STATUS_00N81|STATUS_00N82|STATUS_00N83|STATUS_00N84|STATUS_01N70' \
    "${JAVA_SRC}/gqlstatus/GqlStatusInfoCodes.java" >&2
  exit 1
fi

for f in "${DBMS_DIAG_FILE}" "${HEAP_DUMP_FILE}" "${VALIDATORS_FILE}"; do
  if [[ ! -f "${f}" ]]; then
    echo "[neo4j-lite] missing required patched file: ${f}" >&2
    exit 1
  fi
done

echo "[neo4j-lite] verify-source-prune passed"
