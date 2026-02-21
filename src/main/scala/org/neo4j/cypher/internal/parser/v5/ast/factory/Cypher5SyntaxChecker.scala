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

import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.Token
import org.antlr.v4.runtime.tree.ErrorNode
import org.antlr.v4.runtime.tree.TerminalNode
import org.neo4j.cypher.internal.expressions.Expression
import org.neo4j.cypher.internal.expressions.FunctionName
import org.neo4j.cypher.internal.parser.AstRuleCtx
import org.neo4j.cypher.internal.parser.ast.SyntaxChecker
import org.neo4j.cypher.internal.parser.ast.util.Util.astSeq
import org.neo4j.cypher.internal.parser.ast.util.Util.cast
import org.neo4j.cypher.internal.parser.ast.util.Util.ctxChild
import org.neo4j.cypher.internal.parser.ast.util.Util.nodeChild
import org.neo4j.cypher.internal.parser.ast.util.Util.pos
import org.neo4j.cypher.internal.parser.common.ast.factory.ASTExceptionFactory
import org.neo4j.cypher.internal.parser.common.ast.factory.ConstraintType
import org.neo4j.cypher.internal.parser.common.ast.factory.HintIndexType
import org.neo4j.cypher.internal.parser.v5.Cypher5Parser
import org.neo4j.cypher.internal.parser.v5.Cypher5Parser.ConstraintExistsContext
import org.neo4j.cypher.internal.parser.v5.Cypher5Parser.ConstraintIsNotNullContext
import org.neo4j.cypher.internal.parser.v5.Cypher5Parser.ConstraintIsUniqueContext
import org.neo4j.cypher.internal.parser.v5.Cypher5Parser.ConstraintKeyContext
import org.neo4j.cypher.internal.parser.v5.Cypher5Parser.ConstraintTypedContext
import org.neo4j.cypher.internal.parser.v5.Cypher5Parser.DropConstraintContext
import org.neo4j.cypher.internal.parser.v5.Cypher5Parser.GlobContext
import org.neo4j.cypher.internal.parser.v5.Cypher5Parser.GlobRecursiveContext
import org.neo4j.cypher.internal.parser.v5.Cypher5Parser.SymbolicAliasNameOrParameterContext
import org.neo4j.cypher.internal.parser.v5.ast.factory.Cypher5SyntaxChecker.MAX_ALIAS_NAME_COMPONENTS
import org.neo4j.cypher.internal.parser.v5.ast.factory.Cypher5SyntaxChecker.MAX_DATABASE_NAME_COMPONENTS
import org.neo4j.cypher.internal.util.CypherExceptionFactory
import org.neo4j.cypher.internal.util.InputPosition
import org.neo4j.cypher.internal.util.symbols.ClosedDynamicUnionType
import org.neo4j.gqlstatus.GqlHelper
import org.neo4j.internal.helpers.NameUtil

import scala.collection.mutable
import scala.jdk.CollectionConverters.ListHasAsScala

final class Cypher5SyntaxChecker(exceptionFactory: CypherExceptionFactory) extends SyntaxChecker {
  private[this] var _errors: Seq[Exception] = Seq.empty

  override def errors: Seq[Throwable] = _errors

  override def visitTerminal(node: TerminalNode): Unit = {}
  override def visitErrorNode(node: ErrorNode): Unit = {}
  override def enterEveryRule(ctx: ParserRuleContext): Unit = {}

  override def exitEveryRule(ctx: ParserRuleContext): Unit = {
    // Note, this has been shown to be significantly faster than using the generated listener.
    // Compiles into a lookupswitch (or possibly tableswitch)
    ctx.getRuleIndex match {
      case Cypher5Parser.RULE_periodicCommitQueryHintFailure   => checkPeriodicCommitQueryHintFailure(cast(ctx))
      case Cypher5Parser.RULE_subqueryInTransactionsParameters => checkSubqueryInTransactionsParameters(cast(ctx))
      case Cypher5Parser.RULE_command                          => checkDisabledFeature(cast(ctx), "administration command")
      case Cypher5Parser.RULE_loadCSVClause                    => checkDisabledFeature(cast(ctx), "LOAD CSV")
      case Cypher5Parser.RULE_globPart                         => checkGlobPart(cast(ctx))
      case Cypher5Parser.RULE_insertPattern                    => checkInsertPattern(cast(ctx))
      case Cypher5Parser.RULE_insertNodeLabelExpression        => checkInsertLabelConjunction(cast(ctx))
      case Cypher5Parser.RULE_functionInvocation               => checkFunctionInvocation(cast(ctx))
      case Cypher5Parser.RULE_typePart                         => checkTypePart(cast(ctx))
      case Cypher5Parser.RULE_hint                             => checkHint(cast(ctx))
      case Cypher5Parser.RULE_symbolicAliasNameOrParameter     => checkSymbolicAliasNameOrParameter(cast(ctx))
      case _                                                   =>
    }
  }

  override def check(ctx: ParserRuleContext): Boolean = {
    exitEveryRule(ctx)
    _errors.isEmpty
  }

  private def checkDisabledFeature(ctx: ParserRuleContext, feature: String): Unit = {
    _errors :+= exceptionFactory.syntaxException(
      s"feature disabled: $feature",
      inputPosition(ctx.getStart)
    )
  }

  private def inputPosition(symbol: Token): InputPosition = {
    InputPosition(symbol.getStartIndex, symbol.getLine, symbol.getCharPositionInLine + 1)
  }

  private def errorOnDuplicate(
    token: Token,
    description: String,
    isParam: Boolean
  ): Unit = {
    if (isParam) {
      _errors :+= exceptionFactory.syntaxException(
        s"Duplicated $description parameters",
        inputPosition(token)
      )
    } else {
      _errors :+= exceptionFactory.syntaxException(
        s"Duplicate $description clause",
        inputPosition(token)
      )

    }
  }

  private def errorOnDuplicateCtx[T <: AstRuleCtx](
    ctx: java.util.List[T],
    description: String,
    isParam: Boolean = false
  ): Unit = {
    if (ctx.size > 1) {
      errorOnDuplicate(nodeChild(ctx.get(1), 0).getSymbol, description, isParam)
    }
  }

  private def errorOnDuplicateRule[T <: ParserRuleContext](
    params: java.util.List[T],
    description: String,
    isParam: Boolean = false
  ): Unit = {
    if (params.size() > 1) {
      errorOnDuplicate(params.get(1).start, description, isParam)
    }
  }

  private def errorOnAliasNameContainingTooManyComponents(
    aliasesNames: Seq[SymbolicAliasNameOrParameterContext],
    maxComponents: Int,
    errorTemplate: String
  ): Unit = {
    if (aliasesNames.nonEmpty) {
      val literalAliasNames = aliasesNames.filter(_.symbolicAliasName() != null)
      for (aliasName <- literalAliasNames) {
        val nameComponents = aliasName.symbolicAliasName().symbolicNameString().asScala.toList
        val componentCount = nameComponents.sliding(2, 1).foldLeft(1) {
          case (count, a :: b :: Nil)
            if a.escapedSymbolicNameString() != null || b.escapedSymbolicNameString() != null => count + 1
          case (count, _) => count
        }
        if (componentCount > maxComponents) {
          val start = aliasName.symbolicAliasName().symbolicNameString().get(0).getStart
          _errors :+= exceptionFactory.syntaxException(
            errorTemplate.formatted(
              aliasName.symbolicAliasName().symbolicNameString().asScala.map {
                case context if context.unescapedSymbolicNameString() != null =>
                  context.unescapedSymbolicNameString().ast
                case context if context.escapedSymbolicNameString() != null =>
                  NameUtil.forceEscapeName(context.escapedSymbolicNameString().ast())
                case _ => ""
              }.mkString(".")
            ),
            inputPosition(start)
          )
        }
      }
    }
  }

  private def checkSubqueryInTransactionsParameters(ctx: Cypher5Parser.SubqueryInTransactionsParametersContext)
    : Unit = {
    errorOnDuplicateRule(ctx.subqueryInTransactionsBatchParameters(), "OF ROWS", isParam = true)
    errorOnDuplicateRule(ctx.subqueryInTransactionsErrorParameters(), "ON ERROR", isParam = true)
    errorOnDuplicateRule(ctx.subqueryInTransactionsReportParameters(), "REPORT STATUS", isParam = true)
  }

  private def checkSymbolicAliasNameOrParameter(ctx: Cypher5Parser.SymbolicAliasNameOrParameterContext): Unit = {
    ctx.getParent.getRuleIndex match {
      case Cypher5Parser.RULE_createDatabase =>
        // `a`.`b` disallowed
        errorOnAliasNameContainingTooManyComponents(
          Seq(ctx),
          MAX_DATABASE_NAME_COMPONENTS,
          "Invalid input `%s` for database name. Expected name to contain at most one component."
        )
      case Cypher5Parser.RULE_createCompositeDatabase =>
      // Handled in semantic checks
      case _ =>
        // `a`.`b` allowed, `a`.`b`.`c` disallowed
        errorOnAliasNameContainingTooManyComponents(
          Seq(ctx),
          MAX_ALIAS_NAME_COMPONENTS,
          "Invalid input `%s` for name. Expected name to contain at most two components separated by `.`."
        )
    }
  }

  private def checkGlobPart(ctx: Cypher5Parser.GlobPartContext): Unit = {
    if (ctx.DOT() == null) {
      ctx.parent.parent match {
        case r: GlobRecursiveContext if r.globPart().escapedSymbolicNameString() != null =>
          addError()

        case r: GlobContext if r.escapedSymbolicNameString() != null =>
          addError()

        case _ =>
      }

      def addError(): Unit = {
        _errors :+= exceptionFactory.syntaxException(
          "Each part of the glob (a block of text up until a dot) must either be fully escaped or not escaped at all.",
          inputPosition(ctx.start)
        )
      }
    }
  }

  private def checkPeriodicCommitQueryHintFailure(ctx: Cypher5Parser.PeriodicCommitQueryHintFailureContext): Unit = {
    val periodic = ctx.PERIODIC().getSymbol

    _errors :+= exceptionFactory.syntaxException(
      "The PERIODIC COMMIT query hint is no longer supported. Please use CALL { ... } IN TRANSACTIONS instead.",
      inputPosition(periodic)
    )
  }

  private def checkInsertPattern(ctx: Cypher5Parser.InsertPatternContext): Unit = {
    if (ctx.EQ() != null) {
      _errors :+= exceptionFactory.syntaxException(
        "Named patterns are not allowed in `INSERT`. Use `CREATE` instead or remove the name.",
        pos(ctxChild(ctx, 0))
      )
    }
  }

  private def checkInsertLabelConjunction(ctx: Cypher5Parser.InsertNodeLabelExpressionContext): Unit = {
    val colons = ctx.COLON()
    val firstIsColon = nodeChild(ctx, 0).getSymbol.getType == Cypher5Parser.COLON

    if (firstIsColon && colons.size > 1) {
      _errors :+= exceptionFactory.syntaxException(
        "Colon `:` conjunction is not allowed in INSERT. Use `CREATE` or conjunction with ampersand `&` instead.",
        inputPosition(colons.get(1).getSymbol)
      )
    } else if (!firstIsColon && colons.size() > 0) {
      _errors :+= exceptionFactory.syntaxException(
        "Colon `:` conjunction is not allowed in INSERT. Use `CREATE` or conjunction with ampersand `&` instead.",
        inputPosition(colons.get(0).getSymbol)
      )
    }
  }

  private def checkFunctionInvocation(ctx: Cypher5Parser.FunctionInvocationContext): Unit = {
    val functionName = ctx.functionName().ast[FunctionName]()
    if (
      functionName.name == "normalize" &&
      functionName.namespace.parts.isEmpty &&
      ctx.functionArgument().size == 2
    ) {
      _errors :+= exceptionFactory.syntaxException(
        "Invalid normal form, expected NFC, NFD, NFKC, NFKD",
        ctx.functionArgument(1).expression().ast[Expression]().position
      )
    }
  }

  private def checkTypePart(ctx: Cypher5Parser.TypePartContext): Unit = {
    val cypherType = ctx.typeName().ast
    if (cypherType.isInstanceOf[ClosedDynamicUnionType] && ctx.typeNullability() != null) {
      _errors :+= exceptionFactory.syntaxException(
        "Closed Dynamic Union Types can not be appended with `NOT NULL`, specify `NOT NULL` on all inner types instead.",
        pos(ctx.typeNullability())
      )
    }
  }

  private def checkHint(ctx: Cypher5Parser.HintContext): Unit = {
    nodeChild(ctx, 1).getSymbol.getType match {
      case Cypher5Parser.BTREE => _errors :+= exceptionFactory.syntaxException(
          ASTExceptionFactory.invalidHintIndexType(HintIndexType.BTREE),
          pos(nodeChild(ctx, 1))
        )
      case _ =>
    }
  }
}

object Cypher5SyntaxChecker {
  private val MAX_ALIAS_NAME_COMPONENTS: Int = 2
  private val MAX_DATABASE_NAME_COMPONENTS: Int = 1
}
