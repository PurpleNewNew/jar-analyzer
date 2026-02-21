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

package org.neo4j.cypher.internal.parser.v5.ast.factory

import org.neo4j.cypher.internal.ast.AliasedReturnItem
import org.neo4j.cypher.internal.ast.Limit
import org.neo4j.cypher.internal.ast.OrderBy
import org.neo4j.cypher.internal.ast.ReturnItem
import org.neo4j.cypher.internal.ast.ReturnItems
import org.neo4j.cypher.internal.ast.Skip
import org.neo4j.cypher.internal.ast.SortItem
import org.neo4j.cypher.internal.ast.UnaliasedReturnItem
import org.neo4j.cypher.internal.ast.Where
import org.neo4j.cypher.internal.ast.Yield
import org.neo4j.cypher.internal.expressions.Expression
import org.neo4j.cypher.internal.expressions.LogicalVariable
import org.neo4j.cypher.internal.expressions.Variable
import org.neo4j.cypher.internal.parser.ast.util.Util.astOpt
import org.neo4j.cypher.internal.parser.ast.util.Util.astSeq
import org.neo4j.cypher.internal.parser.ast.util.Util.inputText
import org.neo4j.cypher.internal.parser.ast.util.Util.pos
import org.neo4j.cypher.internal.parser.v5.Cypher5Parser
import org.neo4j.cypher.internal.parser.v5.Cypher5ParserListener
import org.neo4j.cypher.internal.util.CypherExceptionFactory

trait DdlShowBuilder extends Cypher5ParserListener {
  protected def exceptionFactory: CypherExceptionFactory

  final override def exitShowCommand(
    ctx: Cypher5Parser.ShowCommandContext
  ): Unit = {
    throw exceptionFactory.syntaxException("feature disabled: administration commands", pos(ctx))
  }

  final override def exitTerminateCommand(
    ctx: Cypher5Parser.TerminateCommandContext
  ): Unit = {
    throw exceptionFactory.syntaxException("feature disabled: administration commands", pos(ctx))
  }

  final override def exitYieldItem(
    ctx: Cypher5Parser.YieldItemContext
  ): Unit = {
    val variables = ctx.variable()
    val returnItem = variables.get(0).ast[Variable]()
    ctx.ast = if (variables.size == 1) {
      UnaliasedReturnItem(returnItem, inputText(variables.get(0)))(pos(ctx))
    } else {
      AliasedReturnItem(returnItem, variables.get(1).ast[Variable]())(pos(ctx))
    }
  }

  final override def exitYieldSkip(ctx: Cypher5Parser.YieldSkipContext): Unit = {
    ctx.ast = Skip(ctx.signedIntegerLiteral().ast[Expression]())(pos(ctx))
  }

  final override def exitYieldLimit(ctx: Cypher5Parser.YieldLimitContext): Unit = {
    ctx.ast = Limit(ctx.signedIntegerLiteral().ast[Expression]())(pos(ctx))
  }

  final override def exitOrderBy(ctx: Cypher5Parser.OrderByContext): Unit = {
    ctx.ast = OrderBy(astSeq[SortItem](ctx.orderItem()))(pos(ctx.ORDER().getSymbol))
  }

  final override def exitYieldClause(
    ctx: Cypher5Parser.YieldClauseContext
  ): Unit = {
    val returnItems =
      if (ctx.TIMES() != null)
        ReturnItems(includeExisting = true, Seq.empty)(pos(ctx.YIELD().getSymbol))
      else {
        ReturnItems(includeExisting = false, astSeq[ReturnItem](ctx.yieldItem()))(pos(ctx.yieldItem().get(0)))
      }
    ctx.ast = Yield(
      returnItems,
      astOpt[OrderBy](ctx.orderBy()),
      astOpt[Skip](ctx.yieldSkip()),
      astOpt[Limit](ctx.yieldLimit()),
      astOpt[Where](ctx.whereClause())
    )(pos(ctx))
  }
}
