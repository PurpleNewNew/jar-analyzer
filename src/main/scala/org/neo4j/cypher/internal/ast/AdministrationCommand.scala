/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [https://neo4j.com]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neo4j.cypher.internal.ast

import org.neo4j.cypher.internal.ast.semantics.SemanticAnalysisTooling
import org.neo4j.cypher.internal.ast.semantics.SemanticCheck
import org.neo4j.cypher.internal.expressions.LogicalVariable

/**
 * Administration commands are disabled in jar-analyzer neo4lite.
 * This file intentionally keeps only the minimal command marker type required by
 * parser/AST factory signatures.
 */
sealed trait AdministrationCommand extends StatementWithGraph with SemanticAnalysisTooling {
  override def useGraph: Option[GraphSelection] = None
  override def withGraph(useGraph: Option[UseGraph]): AdministrationCommand = this
  override def semanticCheck: SemanticCheck = SemanticCheck.success
  override def returnColumns: List[LogicalVariable] = List.empty
  override def containsUpdates: Boolean = false
}
