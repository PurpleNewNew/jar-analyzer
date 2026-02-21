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
package org.neo4j.cypher.internal.ast.factory.neo4j

import org.neo4j.cypher.internal.ast.AliasedReturnItem
import org.neo4j.cypher.internal.ast.AscSortItem
import org.neo4j.cypher.internal.ast.CatalogName
import org.neo4j.cypher.internal.ast.Clause
import org.neo4j.cypher.internal.ast.CollectExpression
import org.neo4j.cypher.internal.ast.CountExpression
import org.neo4j.cypher.internal.ast.Create
import org.neo4j.cypher.internal.ast.DatabaseName
import org.neo4j.cypher.internal.ast.Delete
import org.neo4j.cypher.internal.ast.DescSortItem
import org.neo4j.cypher.internal.ast.ExistsExpression
import org.neo4j.cypher.internal.ast.Finish
import org.neo4j.cypher.internal.ast.Foreach
import org.neo4j.cypher.internal.ast.GraphDirectReference
import org.neo4j.cypher.internal.ast.GraphFunctionReference
import org.neo4j.cypher.internal.ast.Hint
import org.neo4j.cypher.internal.ast.IfExistsDo
import org.neo4j.cypher.internal.ast.IfExistsDoNothing
import org.neo4j.cypher.internal.ast.IfExistsInvalidSyntax
import org.neo4j.cypher.internal.ast.IfExistsReplace
import org.neo4j.cypher.internal.ast.IfExistsThrowError
import org.neo4j.cypher.internal.ast.ImportingWithSubqueryCall
import org.neo4j.cypher.internal.ast.Insert
import org.neo4j.cypher.internal.ast.IsNormalized
import org.neo4j.cypher.internal.ast.IsNotNormalized
import org.neo4j.cypher.internal.ast.IsNotTyped
import org.neo4j.cypher.internal.ast.IsTyped
import org.neo4j.cypher.internal.ast.Limit
import org.neo4j.cypher.internal.ast.Match
import org.neo4j.cypher.internal.ast.Merge
import org.neo4j.cypher.internal.ast.NamespacedName
import org.neo4j.cypher.internal.ast.NoOptions
import org.neo4j.cypher.internal.ast.OnCreate
import org.neo4j.cypher.internal.ast.OnMatch
import org.neo4j.cypher.internal.ast.OptionsMap
import org.neo4j.cypher.internal.ast.OptionsParam
import org.neo4j.cypher.internal.ast.OrderBy
import org.neo4j.cypher.internal.ast.ParameterName
import org.neo4j.cypher.internal.ast.ProcedureResult
import org.neo4j.cypher.internal.ast.ProcedureResultItem
import org.neo4j.cypher.internal.ast.Query
import org.neo4j.cypher.internal.ast.Remove
import org.neo4j.cypher.internal.ast.RemoveDynamicPropertyItem
import org.neo4j.cypher.internal.ast.RemoveItem
import org.neo4j.cypher.internal.ast.RemoveLabelItem
import org.neo4j.cypher.internal.ast.RemovePropertyItem
import org.neo4j.cypher.internal.ast.Return
import org.neo4j.cypher.internal.ast.ReturnItem
import org.neo4j.cypher.internal.ast.ReturnItems
import org.neo4j.cypher.internal.ast.ScopeClauseSubqueryCall
import org.neo4j.cypher.internal.ast.SetClause
import org.neo4j.cypher.internal.ast.SetDynamicPropertyItem
import org.neo4j.cypher.internal.ast.SetExactPropertiesFromMapItem
import org.neo4j.cypher.internal.ast.SetIncludingPropertiesFromMapItem
import org.neo4j.cypher.internal.ast.SetItem
import org.neo4j.cypher.internal.ast.SetLabelItem
import org.neo4j.cypher.internal.ast.SetPropertyItem
import org.neo4j.cypher.internal.ast.SingleQuery
import org.neo4j.cypher.internal.ast.Skip
import org.neo4j.cypher.internal.ast.SortItem
import org.neo4j.cypher.internal.ast.Statement
import org.neo4j.cypher.internal.ast.StatementWithGraph
import org.neo4j.cypher.internal.ast.Statements
import org.neo4j.cypher.internal.ast.SubqueryCall
import org.neo4j.cypher.internal.ast.SubqueryCall.InTransactionsOnErrorBehaviour.OnErrorBreak
import org.neo4j.cypher.internal.ast.SubqueryCall.InTransactionsOnErrorBehaviour.OnErrorContinue
import org.neo4j.cypher.internal.ast.SubqueryCall.InTransactionsOnErrorBehaviour.OnErrorFail
import org.neo4j.cypher.internal.ast.UnaliasedReturnItem
import org.neo4j.cypher.internal.ast.UnionAll
import org.neo4j.cypher.internal.ast.UnionDistinct
import org.neo4j.cypher.internal.ast.UnresolvedCall
import org.neo4j.cypher.internal.ast.Unwind
import org.neo4j.cypher.internal.ast.UseGraph
import org.neo4j.cypher.internal.ast.UsingIndexHint
import org.neo4j.cypher.internal.ast.UsingIndexHint.SeekOnly
import org.neo4j.cypher.internal.ast.UsingIndexHint.SeekOrScan
import org.neo4j.cypher.internal.ast.UsingIndexHint.UsingAnyIndexType
import org.neo4j.cypher.internal.ast.UsingIndexHint.UsingIndexHintType
import org.neo4j.cypher.internal.ast.UsingIndexHint.UsingPointIndexType
import org.neo4j.cypher.internal.ast.UsingIndexHint.UsingRangeIndexType
import org.neo4j.cypher.internal.ast.UsingIndexHint.UsingTextIndexType
import org.neo4j.cypher.internal.ast.UsingJoinHint
import org.neo4j.cypher.internal.ast.UsingScanHint
import org.neo4j.cypher.internal.ast.Where
import org.neo4j.cypher.internal.ast.With
import org.neo4j.cypher.internal.ast.Yield
import org.neo4j.cypher.internal.ast.factory.ASTFactory
import org.neo4j.cypher.internal.ast.factory.ASTFactory.MergeActionType
import org.neo4j.cypher.internal.ast.factory.ASTFactory.StringPos
import org.neo4j.cypher.internal.expressions.Add
import org.neo4j.cypher.internal.expressions.AllIterablePredicate
import org.neo4j.cypher.internal.expressions.AllPropertiesSelector
import org.neo4j.cypher.internal.expressions.And
import org.neo4j.cypher.internal.expressions.Ands
import org.neo4j.cypher.internal.expressions.AnonymousPatternPart
import org.neo4j.cypher.internal.expressions.AnyIterablePredicate
import org.neo4j.cypher.internal.expressions.CaseExpression
import org.neo4j.cypher.internal.expressions.Concatenate
import org.neo4j.cypher.internal.expressions.ContainerIndex
import org.neo4j.cypher.internal.expressions.Contains
import org.neo4j.cypher.internal.expressions.CountStar
import org.neo4j.cypher.internal.expressions.DecimalDoubleLiteral
import org.neo4j.cypher.internal.expressions.Divide
import org.neo4j.cypher.internal.expressions.DynamicLabelExpression
import org.neo4j.cypher.internal.expressions.DynamicLabelOrRelTypeExpression
import org.neo4j.cypher.internal.expressions.DynamicRelTypeExpression
import org.neo4j.cypher.internal.expressions.EndsWith
import org.neo4j.cypher.internal.expressions.Equals
import org.neo4j.cypher.internal.expressions.ExplicitParameter
import org.neo4j.cypher.internal.expressions.Expression
import org.neo4j.cypher.internal.expressions.False
import org.neo4j.cypher.internal.expressions.FixedQuantifier
import org.neo4j.cypher.internal.expressions.FunctionInvocation
import org.neo4j.cypher.internal.expressions.FunctionName
import org.neo4j.cypher.internal.expressions.GraphPatternQuantifier
import org.neo4j.cypher.internal.expressions.GreaterThan
import org.neo4j.cypher.internal.expressions.GreaterThanOrEqual
import org.neo4j.cypher.internal.expressions.In
import org.neo4j.cypher.internal.expressions.Infinity
import org.neo4j.cypher.internal.expressions.IntervalQuantifier
import org.neo4j.cypher.internal.expressions.InvalidNotEquals
import org.neo4j.cypher.internal.expressions.IsNotNull
import org.neo4j.cypher.internal.expressions.IsNull
import org.neo4j.cypher.internal.expressions.LabelName
import org.neo4j.cypher.internal.expressions.LabelOrRelTypeName
import org.neo4j.cypher.internal.expressions.LessThan
import org.neo4j.cypher.internal.expressions.LessThanOrEqual
import org.neo4j.cypher.internal.expressions.ListComprehension
import org.neo4j.cypher.internal.expressions.ListLiteral
import org.neo4j.cypher.internal.expressions.ListSlice
import org.neo4j.cypher.internal.expressions.LiteralEntry
import org.neo4j.cypher.internal.expressions.MapExpression
import org.neo4j.cypher.internal.expressions.MapProjection
import org.neo4j.cypher.internal.expressions.MapProjectionElement
import org.neo4j.cypher.internal.expressions.MatchMode
import org.neo4j.cypher.internal.expressions.MatchMode.DifferentRelationships
import org.neo4j.cypher.internal.expressions.MatchMode.MatchMode
import org.neo4j.cypher.internal.expressions.MatchMode.RepeatableElements
import org.neo4j.cypher.internal.expressions.Modulo
import org.neo4j.cypher.internal.expressions.Multiply
import org.neo4j.cypher.internal.expressions.NFCNormalForm
import org.neo4j.cypher.internal.expressions.NFDNormalForm
import org.neo4j.cypher.internal.expressions.NFKCNormalForm
import org.neo4j.cypher.internal.expressions.NFKDNormalForm
import org.neo4j.cypher.internal.expressions.NaN
import org.neo4j.cypher.internal.expressions.NamedPatternPart
import org.neo4j.cypher.internal.expressions.Namespace
import org.neo4j.cypher.internal.expressions.NodePattern
import org.neo4j.cypher.internal.expressions.NonPrefixedPatternPart
import org.neo4j.cypher.internal.expressions.NoneIterablePredicate
import org.neo4j.cypher.internal.expressions.NormalForm
import org.neo4j.cypher.internal.expressions.Not
import org.neo4j.cypher.internal.expressions.NotEquals
import org.neo4j.cypher.internal.expressions.Null
import org.neo4j.cypher.internal.expressions.Or
import org.neo4j.cypher.internal.expressions.Parameter
import org.neo4j.cypher.internal.expressions.ParenthesizedPath
import org.neo4j.cypher.internal.expressions.PathConcatenation
import org.neo4j.cypher.internal.expressions.PathFactor
import org.neo4j.cypher.internal.expressions.PathPatternPart
import org.neo4j.cypher.internal.expressions.Pattern
import org.neo4j.cypher.internal.expressions.PatternAtom
import org.neo4j.cypher.internal.expressions.PatternComprehension
import org.neo4j.cypher.internal.expressions.PatternElement
import org.neo4j.cypher.internal.expressions.PatternExpression
import org.neo4j.cypher.internal.expressions.PatternPart
import org.neo4j.cypher.internal.expressions.PatternPartWithSelector
import org.neo4j.cypher.internal.expressions.PlusQuantifier
import org.neo4j.cypher.internal.expressions.Pow
import org.neo4j.cypher.internal.expressions.ProcedureName
import org.neo4j.cypher.internal.expressions.ProcedureOutput
import org.neo4j.cypher.internal.expressions.Property
import org.neo4j.cypher.internal.expressions.PropertyKeyName
import org.neo4j.cypher.internal.expressions.PropertySelector
import org.neo4j.cypher.internal.expressions.QuantifiedPath
import org.neo4j.cypher.internal.expressions.Range
import org.neo4j.cypher.internal.expressions.ReduceExpression
import org.neo4j.cypher.internal.expressions.RegexMatch
import org.neo4j.cypher.internal.expressions.RelTypeName
import org.neo4j.cypher.internal.expressions.RelationshipChain
import org.neo4j.cypher.internal.expressions.RelationshipPattern
import org.neo4j.cypher.internal.expressions.RelationshipsPattern
import org.neo4j.cypher.internal.expressions.SemanticDirection
import org.neo4j.cypher.internal.expressions.SensitiveParameter
import org.neo4j.cypher.internal.expressions.ShortestPathExpression
import org.neo4j.cypher.internal.expressions.ShortestPathsPatternPart
import org.neo4j.cypher.internal.expressions.SignedDecimalIntegerLiteral
import org.neo4j.cypher.internal.expressions.SignedHexIntegerLiteral
import org.neo4j.cypher.internal.expressions.SignedOctalIntegerLiteral
import org.neo4j.cypher.internal.expressions.SimplePattern
import org.neo4j.cypher.internal.expressions.SingleIterablePredicate
import org.neo4j.cypher.internal.expressions.StarQuantifier
import org.neo4j.cypher.internal.expressions.StartsWith
import org.neo4j.cypher.internal.expressions.StringLiteral
import org.neo4j.cypher.internal.expressions.Subtract
import org.neo4j.cypher.internal.expressions.True
import org.neo4j.cypher.internal.expressions.UnaryAdd
import org.neo4j.cypher.internal.expressions.UnarySubtract
import org.neo4j.cypher.internal.expressions.UnsignedDecimalIntegerLiteral
import org.neo4j.cypher.internal.expressions.Variable
import org.neo4j.cypher.internal.expressions.VariableSelector
import org.neo4j.cypher.internal.expressions.Xor
import org.neo4j.cypher.internal.expressions.functions.Normalize
import org.neo4j.cypher.internal.expressions.functions.Trim
import org.neo4j.cypher.internal.label_expressions.LabelExpression
import org.neo4j.cypher.internal.label_expressions.LabelExpression.DynamicLeaf
import org.neo4j.cypher.internal.label_expressions.LabelExpression.Leaf
import org.neo4j.cypher.internal.label_expressions.LabelExpressionPredicate
import org.neo4j.cypher.internal.parser.common.ast.factory.ASTExceptionFactory
import org.neo4j.cypher.internal.parser.common.ast.factory.AccessType
import org.neo4j.cypher.internal.parser.common.ast.factory.ActionType
import org.neo4j.cypher.internal.parser.common.ast.factory.CallInTxsOnErrorBehaviourType
import org.neo4j.cypher.internal.parser.common.ast.factory.HintIndexType
import org.neo4j.cypher.internal.parser.common.ast.factory.ParameterType
import org.neo4j.cypher.internal.parser.common.ast.factory.ParserCypherTypeName
import org.neo4j.cypher.internal.parser.common.ast.factory.ParserNormalForm
import org.neo4j.cypher.internal.parser.common.ast.factory.ParserTrimSpecification
import org.neo4j.cypher.internal.parser.common.ast.factory.ScopeType
import org.neo4j.cypher.internal.parser.common.ast.factory.SimpleEither
import org.neo4j.cypher.internal.ast.factory.neo4j.EntityType
import org.neo4j.cypher.internal.util.DeprecatedIdentifierUnicode
import org.neo4j.cypher.internal.util.DeprecatedIdentifierWhitespaceUnicode
import org.neo4j.cypher.internal.util.InputPosition
import org.neo4j.cypher.internal.util.InternalNotificationLogger
import org.neo4j.cypher.internal.util.symbols.AnyType
import org.neo4j.cypher.internal.util.symbols.BooleanType
import org.neo4j.cypher.internal.util.symbols.CTAny
import org.neo4j.cypher.internal.util.symbols.CTInteger
import org.neo4j.cypher.internal.util.symbols.CTMap
import org.neo4j.cypher.internal.util.symbols.CTString
import org.neo4j.cypher.internal.util.symbols.ClosedDynamicUnionType
import org.neo4j.cypher.internal.util.symbols.CypherType
import org.neo4j.cypher.internal.util.symbols.DateType
import org.neo4j.cypher.internal.util.symbols.DurationType
import org.neo4j.cypher.internal.util.symbols.FloatType
import org.neo4j.cypher.internal.util.symbols.IntegerType
import org.neo4j.cypher.internal.util.symbols.ListType
import org.neo4j.cypher.internal.util.symbols.LocalDateTimeType
import org.neo4j.cypher.internal.util.symbols.LocalTimeType
import org.neo4j.cypher.internal.util.symbols.MapType
import org.neo4j.cypher.internal.util.symbols.NodeType
import org.neo4j.cypher.internal.util.symbols.NothingType
import org.neo4j.cypher.internal.util.symbols.NullType
import org.neo4j.cypher.internal.util.symbols.PathType
import org.neo4j.cypher.internal.util.symbols.PointType
import org.neo4j.cypher.internal.util.symbols.PropertyValueType
import org.neo4j.cypher.internal.util.symbols.RelationshipType
import org.neo4j.cypher.internal.util.symbols.StringType
import org.neo4j.cypher.internal.util.symbols.ZonedDateTimeType
import org.neo4j.cypher.internal.util.symbols.ZonedTimeType

import java.lang
import java.nio.charset.StandardCharsets
import java.util
import java.util.stream.Collectors

import scala.jdk.CollectionConverters.IteratorHasAsScala
import scala.jdk.CollectionConverters.ListHasAsScala
import scala.jdk.CollectionConverters.MapHasAsScala
import scala.jdk.CollectionConverters.SetHasAsScala
import scala.language.implicitConversions

trait DecorateTuple {

  class AsScala[A](op: => A) {
    def asScala: A = op
  }

  implicit def asScalaEither[L, R](i: SimpleEither[L, R]): AsScala[Either[L, R]] = {
    new AsScala(if (i.getRight == null) Left[L, R](i.getLeft) else Right[L, R](i.getRight))
  }
}

object TupleConverter extends DecorateTuple

import org.neo4j.cypher.internal.ast.factory.neo4j.TupleConverter.asScalaEither

class Neo4jASTFactory(query: String, astExceptionFactory: ASTExceptionFactory, logger: InternalNotificationLogger)
    extends ASTFactory[
      Statements,
      Statement,
      Query,
      Clause,
      Finish,
      Return,
      ReturnItem,
      ReturnItems,
      SortItem,
      PatternPart,
      NodePattern,
      RelationshipPattern,
      Option[Range],
      SetClause,
      SetItem,
      RemoveItem,
      ProcedureResultItem,
      Hint,
      Expression,
      LabelExpression,
      FunctionInvocation,
      Parameter,
      Variable,
      Property,
      MapProjectionElement,
      UseGraph,
      StatementWithGraph,
      StatementWithGraph,
      Yield,
      Where,
      SubqueryCall.InTransactionsParameters,
      SubqueryCall.InTransactionsBatchParameters,
      SubqueryCall.InTransactionsConcurrencyParameters,
      SubqueryCall.InTransactionsErrorParameters,
      SubqueryCall.InTransactionsReportParameters,
      InputPosition,
      EntityType,
      GraphPatternQuantifier,
      PatternAtom,
      DatabaseName,
      PatternPart.Selector,
      MatchMode,
      PatternElement
    ] {

  override def statements(statements: util.List[Statement]): Statements = Statements(statements.asScala.toSeq)

  override def newSingleQuery(p: InputPosition, clauses: util.List[Clause]): Query = {
    if (clauses.isEmpty) {
      throw new Neo4jASTConstructionException("A valid Cypher query has to contain at least 1 clause")
    }
    SingleQuery(clauses.asScala.toList)(p)
  }

  override def newSingleQuery(clauses: util.List[Clause]): Query = {
    if (clauses.isEmpty) {
      throw new Neo4jASTConstructionException("A valid Cypher query has to contain at least 1 clause")
    }
    val pos = clauses.get(0).position
    SingleQuery(clauses.asScala.toList)(pos)
  }

  override def newUnion(p: InputPosition, lhs: Query, rhs: Query, all: Boolean): Query = {
    val rhsQuery =
      rhs match {
        case x: SingleQuery => x
        case other =>
          throw new Neo4jASTConstructionException(
            s"The Neo4j AST encodes Unions as a left-deep tree, so the rhs query must always be a SingleQuery. Got `$other`"
          )
      }

    if (all) UnionAll(lhs, rhsQuery)(p)
    else UnionDistinct(lhs, rhsQuery)(p)
  }

  override def directUseClause(p: InputPosition, name: DatabaseName): UseGraph = {
    name match {
      case NamespacedName(nameComponents, namespace) =>
        namespace match {
          case Some(pattern) => UseGraph(GraphDirectReference(CatalogName(pattern +: nameComponents))(name.position))(p)
          case None          => UseGraph(GraphDirectReference(CatalogName(nameComponents))(name.position))(p)
        }
      case ParameterName(_) => throw new Neo4jASTConstructionException("invalid graph reference")
    }
  }

  override def functionUseClause(p: InputPosition, function: FunctionInvocation): UseGraph = {
    UseGraph(GraphFunctionReference(function)(function.position))(p)
  }

  override def newFinishClause(p: InputPosition): Finish = {
    Finish()(p)
  }

  override def newReturnClause(
    p: InputPosition,
    distinct: Boolean,
    returnItems: ReturnItems,
    order: util.List[SortItem],
    orderPosition: InputPosition,
    skip: Expression,
    skipPosition: InputPosition,
    limit: Expression,
    limitPosition: InputPosition
  ): Return = {
    val orderList = order.asScala.toList
    Return(
      distinct,
      returnItems,
      if (order.isEmpty) None else Some(OrderBy(orderList)(orderPosition)),
      Option(skip).map(e => Skip(e)(skipPosition)),
      Option(limit).map(e => Limit(e)(limitPosition))
    )(p)
  }

  override def newReturnItems(p: InputPosition, returnAll: Boolean, returnItems: util.List[ReturnItem]): ReturnItems = {
    ReturnItems(returnAll, returnItems.asScala.toList)(p)
  }

  override def newReturnItem(p: InputPosition, e: Expression, v: Variable): ReturnItem = {
    AliasedReturnItem(e, v)(p)
  }

  override def newReturnItem(p: InputPosition, e: Expression, eStartOffset: Int, eEndOffset: Int): ReturnItem = {

    val name = query.substring(eStartOffset, eEndOffset + 1)
    UnaliasedReturnItem(e, name)(p)
  }

  override def orderDesc(p: InputPosition, e: Expression): SortItem = DescSortItem(e)(p)

  override def orderAsc(p: InputPosition, e: Expression): SortItem = AscSortItem(e)(p)

  override def createClause(p: InputPosition, patterns: util.List[PatternPart]): Clause = {
    val patternList: Seq[NonPrefixedPatternPart] = patterns.asScala.toList.map {
      case p: NonPrefixedPatternPart  => p
      case p: PatternPartWithSelector => throw pathSelectorCannotBeUsedInClauseException("CREATE", p.selector)
    }

    Create(Pattern.ForUpdate(patternList)(patterns.asScala.map(_.position).minBy(_.offset)))(p)
  }

  override def insertClause(p: InputPosition, patterns: util.List[PatternPart]): Clause = {
    val patternList: Seq[NonPrefixedPatternPart] = patterns.asScala.toList.asInstanceOf[List[NonPrefixedPatternPart]]
    Insert(Pattern.ForUpdate(patternList)(patterns.asScala.map(_.position).minBy(_.offset)))(p)
  }

  override def matchClause(
    p: InputPosition,
    optional: Boolean,
    matchMode: MatchMode,
    patterns: util.List[PatternPart],
    patternPos: InputPosition,
    hints: util.List[Hint],
    where: Where
  ): Clause = {
    val patternList: Seq[PatternPartWithSelector] = patterns.asScala.toList.map {
      case part: PatternPartWithSelector => part
      case part: NonPrefixedPatternPart  => PatternPartWithSelector(allPathSelector(part.position), part)
    }
    val finalMatchMode = if (matchMode == null) MatchMode.default(p) else matchMode
    Match(
      optional,
      finalMatchMode,
      Pattern.ForMatch(patternList)(patternPos),
      if (hints == null) Nil else hints.asScala.toList,
      Option(where)
    )(
      p
    )
  }

  override def usingIndexHint(
    p: InputPosition,
    v: Variable,
    labelOrRelType: String,
    properties: util.List[String],
    seekOnly: Boolean,
    indexType: HintIndexType
  ): Hint =
    UsingIndexHint(
      v,
      LabelOrRelTypeName(labelOrRelType)(p),
      properties.asScala.toList.map(PropertyKeyName(_)(p)),
      if (seekOnly) SeekOnly else SeekOrScan,
      usingIndexType(indexType)
    )(p)

  private def usingIndexType(indexType: HintIndexType): UsingIndexHintType = indexType match {
    case HintIndexType.ANY => UsingAnyIndexType
    case HintIndexType.BTREE =>
      throw new Neo4jASTConstructionException(ASTExceptionFactory.invalidHintIndexType(indexType))
    case HintIndexType.TEXT  => UsingTextIndexType
    case HintIndexType.RANGE => UsingRangeIndexType
    case HintIndexType.POINT => UsingPointIndexType
  }

  override def usingJoin(p: InputPosition, joinVariables: util.List[Variable]): Hint =
    UsingJoinHint(joinVariables.asScala.toList)(p)

  override def usingScan(p: InputPosition, v: Variable, labelOrRelType: String): Hint =
    UsingScanHint(v, LabelOrRelTypeName(labelOrRelType)(p))(p)

  override def withClause(p: InputPosition, r: Return, where: Where): Clause =
    With(r.distinct, r.returnItems, r.orderBy, r.skip, r.limit, Option(where))(p)

  override def whereClause(p: InputPosition, where: Expression): Where =
    Where(where)(p)

  override def setClause(p: InputPosition, setItems: util.List[SetItem]): SetClause =
    SetClause(setItems.asScala.toList)(p)

  override def setProperty(property: Property, value: Expression): SetItem =
    SetPropertyItem(property, value)(property.position)

  override def setDynamicProperty(dynamicProperty: Expression, value: Expression): SetItem = dynamicProperty match {
    case c: ContainerIndex => SetDynamicPropertyItem(c, value)(dynamicProperty.position)
    case _ => throw new IllegalArgumentException(
        s"Expected a container index, got: [${dynamicProperty.getClass.getSimpleName}]"
      )
  }

  override def setVariable(variable: Variable, value: Expression): SetItem =
    SetExactPropertiesFromMapItem(variable, value)(variable.position)

  override def addAndSetVariable(variable: Variable, value: Expression): SetItem =
    SetIncludingPropertiesFromMapItem(variable, value)(variable.position)

  override def setLabels(
    variable: Variable,
    labels: util.List[StringPos[InputPosition]],
    dynamicLabels: util.List[Expression],
    containsIs: Boolean
  ): SetItem =
    SetLabelItem(
      variable,
      labels.asScala.toList.map(sp => LabelName(sp.string)(sp.pos)),
      dynamicLabels.asScala.toList,
      containsIs
    )(variable.position)

  override def removeClause(p: InputPosition, removeItems: util.List[RemoveItem]): Clause =
    Remove(removeItems.asScala.toList)(p)

  override def removeProperty(property: Property): RemoveItem = RemovePropertyItem(property)

  override def removeDynamicProperty(expression: Expression): RemoveItem = expression match {
    case c: ContainerIndex => RemoveDynamicPropertyItem(c)
    case _ => throw new IllegalArgumentException(
        s"Expected a container index, got: [${expression.getClass.getSimpleName}]"
      )
  }

  override def removeLabels(
    variable: Variable,
    labels: util.List[StringPos[InputPosition]],
    dynamicLabels: util.List[Expression],
    containsIs: Boolean
  ): RemoveItem =
    RemoveLabelItem(
      variable,
      labels.asScala.toList.map(sp => LabelName(sp.string)(sp.pos)),
      dynamicLabels.asScala.toList,
      containsIs
    )(
      variable.position
    )

  override def deleteClause(p: InputPosition, detach: Boolean, expressions: util.List[Expression]): Clause =
    Delete(expressions.asScala.toList, detach)(p)

  override def unwindClause(p: InputPosition, e: Expression, v: Variable): Clause = Unwind(e, v)(p)

  override def mergeClause(
    p: InputPosition,
    pattern: PatternPart,
    setClauses: util.List[SetClause],
    actionTypes: util.List[MergeActionType],
    positions: util.List[InputPosition]
  ): Clause = {
    val patternForMerge: NonPrefixedPatternPart = pattern match {
      case nonPrefixedPatternPart: NonPrefixedPatternPart => nonPrefixedPatternPart
      case pp: PatternPartWithSelector => throw pathSelectorCannotBeUsedInClauseException("MERGE", pp.selector)
    }

    val clausesIter = setClauses.iterator()
    val positionItr = positions.iterator()
    val actions = actionTypes.asScala.toList.map {
      case MergeActionType.OnMatch =>
        OnMatch(clausesIter.next())(positionItr.next)
      case MergeActionType.OnCreate =>
        OnCreate(clausesIter.next())(positionItr.next)
    }

    Merge(patternForMerge, actions)(p)
  }

  override def callClause(
    p: InputPosition,
    namespacePosition: InputPosition,
    procedureNamePosition: InputPosition,
    procedureResultPosition: InputPosition,
    namespace: util.List[String],
    name: String,
    arguments: util.List[Expression],
    yieldAll: Boolean,
    resultItems: util.List[ProcedureResultItem],
    where: Where,
    optional: Boolean
  ): Clause =
    UnresolvedCall(
      Namespace(namespace.asScala.toList)(namespacePosition),
      ProcedureName(name)(procedureNamePosition),
      if (arguments == null) None else Some(arguments.asScala.toList),
      Option(resultItems).map(items =>
        ProcedureResult(items.asScala.toList.toIndexedSeq, Option(where))(procedureResultPosition)
      ),
      yieldAll,
      optional
    )(p)

  override def callResultItem(p: InputPosition, name: String, v: Variable): ProcedureResultItem =
    if (v == null) ProcedureResultItem(Variable(name)(p, Variable.isIsolatedDefault))(p)
    else ProcedureResultItem(ProcedureOutput(name)(v.position), v)(p)

  override def foreachClause(p: InputPosition, v: Variable, list: Expression, clauses: util.List[Clause]): Clause =
    Foreach(v, list, clauses.asScala.toList)(p)

  override def subqueryInTransactionsParams(
    p: InputPosition,
    batchParams: SubqueryCall.InTransactionsBatchParameters,
    concurrencyParams: SubqueryCall.InTransactionsConcurrencyParameters,
    errorParams: SubqueryCall.InTransactionsErrorParameters,
    reportParams: SubqueryCall.InTransactionsReportParameters
  ): SubqueryCall.InTransactionsParameters =
    SubqueryCall.InTransactionsParameters(
      Option(batchParams),
      Option(concurrencyParams),
      Option(errorParams),
      Option(reportParams)
    )(p)

  override def subqueryInTransactionsBatchParameters(
    p: InputPosition,
    batchSize: Expression
  ): SubqueryCall.InTransactionsBatchParameters =
    SubqueryCall.InTransactionsBatchParameters(batchSize)(p)

  override def subqueryInTransactionsConcurrencyParameters(
    p: InputPosition,
    concurrency: Expression
  ): SubqueryCall.InTransactionsConcurrencyParameters =
    concurrency match {
      case null => SubqueryCall.InTransactionsConcurrencyParameters(None)(p)
      case _    => SubqueryCall.InTransactionsConcurrencyParameters(Some(concurrency))(p)
    }

  override def subqueryInTransactionsErrorParameters(
    p: InputPosition,
    onErrorBehaviour: CallInTxsOnErrorBehaviourType
  ): SubqueryCall.InTransactionsErrorParameters = {
    onErrorBehaviour match {
      case CallInTxsOnErrorBehaviourType.ON_ERROR_CONTINUE =>
        SubqueryCall.InTransactionsErrorParameters(OnErrorContinue)(p)
      case CallInTxsOnErrorBehaviourType.ON_ERROR_BREAK =>
        SubqueryCall.InTransactionsErrorParameters(OnErrorBreak)(p)
      case CallInTxsOnErrorBehaviourType.ON_ERROR_FAIL =>
        SubqueryCall.InTransactionsErrorParameters(OnErrorFail)(p)
    }
  }

  override def subqueryInTransactionsReportParameters(
    p: InputPosition,
    v: Variable
  ): SubqueryCall.InTransactionsReportParameters =
    SubqueryCall.InTransactionsReportParameters(v)(p)

  override def subqueryClause(
    p: InputPosition,
    subquery: Query,
    inTransactions: SubqueryCall.InTransactionsParameters,
    scopeAll: Boolean,
    hasScope: Boolean,
    variables: util.List[Variable],
    optional: Boolean
  ): Clause = {
    if (hasScope) {
      ScopeClauseSubqueryCall(
        subquery,
        scopeAll,
        variables.asScala.toSeq,
        Option(inTransactions),
        optional
      )(p)
    } else {
      ImportingWithSubqueryCall(
        subquery,
        Option(inTransactions),
        optional
      )(p)
    }
  }

  override def orderBySkipLimitClause(
    p: InputPosition,
    order: util.List[SortItem],
    orderPosition: InputPosition,
    skip: Expression,
    skipPosition: InputPosition,
    limit: Expression,
    limitPosition: InputPosition
  ): With = {
    val orderList = order.asScala.toList
    With(
      distinct = false,
      ReturnItems(
        includeExisting = true,
        Seq.empty
      )(p),
      if (order.isEmpty) None else Some(OrderBy(orderList)(orderPosition)),
      Option(skip).map(e => Skip(e)(skipPosition)),
      Option(limit).map(e => Limit(e)(limitPosition)),
      None
    )(p)
  }

  // PATTERNS

  override def namedPattern(v: Variable, pattern: PatternPart): PatternPart =
    NamedPatternPart(v, pattern.asInstanceOf[AnonymousPatternPart])(v.position)

  override def shortestPathPattern(p: InputPosition, patternElement: PatternElement): PatternPart =
    ShortestPathsPatternPart(patternElement, single = true)(p)

  override def allShortestPathsPattern(p: InputPosition, patternElement: PatternElement): PatternPart =
    ShortestPathsPatternPart(patternElement, single = false)(p)

  override def pathPattern(patternElement: PatternElement): PatternPart =
    PathPatternPart(patternElement)

  override def insertPathPattern(atoms: util.List[PatternAtom]): PatternPart = {
    PathPatternPart(getPatternElement(atoms))
  }

  override def patternWithSelector(
    selector: PatternPart.Selector,
    patternPart: PatternPart
  ): PatternPartWithSelector = {
    val nonPrefixedPatternPart = patternPart match {
      case npp: NonPrefixedPatternPart => npp
      case pp: PatternPartWithSelector =>
        throw new IllegalArgumentException(
          s"Expected a pattern without a selector, got: [${pp.getClass.getSimpleName}]: $pp"
        )
    }

    PatternPartWithSelector(selector, nonPrefixedPatternPart)
  }

  override def patternElement(atoms: util.List[PatternAtom]): PatternElement = {
    getPatternElement(atoms)
  }

  private def getPatternElement(atoms: util.List[PatternAtom]): PatternElement = {
    val iterator = atoms.iterator().asScala.buffered

    var factors = Seq.empty[PathFactor]
    while (iterator.hasNext) {
      iterator.next() match {
        case n: NodePattern =>
          var patternElement: SimplePattern = n
          while (iterator.hasNext && iterator.head.isInstanceOf[RelationshipPattern]) {
            val relPattern = iterator.next()
            // we trust in the parser to alternate nodes and relationships
            val rightNodePattern = iterator.next()
            patternElement = RelationshipChain(
              patternElement,
              relPattern.asInstanceOf[RelationshipPattern],
              rightNodePattern.asInstanceOf[NodePattern]
            )(patternElement.position)
          }
          factors = factors :+ patternElement
        case element: QuantifiedPath    => factors = factors :+ element
        case element: ParenthesizedPath => factors = factors :+ element
        case _: RelationshipPattern     => throw new IllegalStateException("Abbreviated patterns are not supported yet")
      }
    }

    val pathElement: PatternElement = factors match {
      case Seq(element) => element
      case factors =>
        val position = factors.head.position
        PathConcatenation(factors)(position)
    }
    pathElement
  }

  override def anyPathSelector(
    count: String,
    countPosition: InputPosition,
    position: InputPosition
  ): PatternPart.Selector = {
    PatternPart.AnyPath(defaultCountValue(count, countPosition, position))(position)
  }

  override def allPathSelector(position: InputPosition): PatternPart.Selector =
    PatternPart.AllPaths()(position)

  override def anyShortestPathSelector(
    count: String,
    countPosition: InputPosition,
    position: InputPosition
  ): PatternPart.Selector =
    PatternPart.AnyShortestPath(defaultCountValue(count, countPosition, position))(position)

  override def allShortestPathSelector(position: InputPosition): PatternPart.Selector =
    PatternPart.AllShortestPaths()(position)

  override def shortestGroupsSelector(
    count: String,
    countPosition: InputPosition,
    position: InputPosition
  ): PatternPart.Selector =
    PatternPart.ShortestGroups(defaultCountValue(count, countPosition, position))(position)

  private def defaultCountValue(count: String, countPosition: InputPosition, position: InputPosition) =
    if (count != null) {
      UnsignedDecimalIntegerLiteral(count)(countPosition)
    } else {
      UnsignedDecimalIntegerLiteral("1")(position)
    }

  override def nodePattern(
    p: InputPosition,
    v: Variable,
    labelExpression: LabelExpression,
    properties: Expression,
    predicate: Expression
  ): NodePattern = {
    NodePattern(Option(v), Option(labelExpression), Option(properties), Option(predicate))(p)
  }

  override def relationshipPattern(
    p: InputPosition,
    left: Boolean,
    right: Boolean,
    v: Variable,
    labelExpression: LabelExpression,
    pathLength: Option[Range],
    properties: Expression,
    predicate: Expression
  ): RelationshipPattern = {
    val direction =
      if (left && !right) SemanticDirection.INCOMING
      else if (!left && right) SemanticDirection.OUTGOING
      else SemanticDirection.BOTH

    val range =
      pathLength match {
        case null    => None
        case None    => Some(None)
        case Some(r) => Some(Some(r))
      }

    RelationshipPattern(
      Option(v),
      Option(labelExpression),
      range,
      Option(properties),
      Option(predicate),
      direction
    )(p)
  }

  override def pathLength(
    p: InputPosition,
    pMin: InputPosition,
    pMax: InputPosition,
    minLength: String,
    maxLength: String
  ): Option[Range] = {
    if (minLength == null && maxLength == null) {
      None
    } else {
      val min = if (minLength == "") None else Some(UnsignedDecimalIntegerLiteral(minLength)(pMin))
      val max = if (maxLength == "") None else Some(UnsignedDecimalIntegerLiteral(maxLength)(pMax))
      Some(Range(min, max)(if (pMin != null) pMin else p))
    }
  }

  override def intervalPathQuantifier(
    position: InputPosition,
    positionLowerBound: InputPosition,
    positionUpperBound: InputPosition,
    lowerBoundText: String,
    upperBoundText: String
  ): GraphPatternQuantifier = {
    val lowerBound =
      if (lowerBoundText == null) None else Some(UnsignedDecimalIntegerLiteral(lowerBoundText)(positionLowerBound))
    val upperBound =
      if (upperBoundText == null) None else Some(UnsignedDecimalIntegerLiteral(upperBoundText)(positionUpperBound))
    IntervalQuantifier(lowerBound, upperBound)(position)
  }

  override def fixedPathQuantifier(
    p: InputPosition,
    valuePos: InputPosition,
    value: String
  ): GraphPatternQuantifier = {
    FixedQuantifier(UnsignedDecimalIntegerLiteral(value)(valuePos))(p)
  }

  override def plusPathQuantifier(
    p: InputPosition
  ): GraphPatternQuantifier = {
    PlusQuantifier()(p)
  }

  override def starPathQuantifier(
    p: InputPosition
  ): GraphPatternQuantifier = {
    StarQuantifier()(p)
  }

  override def repeatableElements(p: InputPosition): MatchMode = {
    RepeatableElements()(p)
  }

  override def differentRelationships(p: InputPosition): MatchMode = {
    DifferentRelationships()(p)
  }

  override def parenthesizedPathPattern(
    p: InputPosition,
    internalPattern: PatternPart,
    where: Expression,
    length: GraphPatternQuantifier
  ): PatternAtom = {
    val nonPrefixedPatternPart: NonPrefixedPatternPart = internalPattern match {
      case nonPrefixedPatternPart: NonPrefixedPatternPart => nonPrefixedPatternPart
      case pp: PatternPartWithSelector =>
        val pathPatternKind = if (length == null) "parenthesized" else "quantified"
        throw pathSelectorNotAllowedWithinPathPatternKindException(pathPatternKind, pp.selector)
    }

    if (length != null)
      QuantifiedPath(nonPrefixedPatternPart, length, Option(where))(p)
    else {
      ParenthesizedPath(nonPrefixedPatternPart, Option(where))(p)
    }
  }

  override def quantifiedRelationship(
    rel: RelationshipPattern,
    quantifier: GraphPatternQuantifier
  ): PatternAtom = {
    // represent -[rel]->+ as (()-[rel]->())+
    val pos = rel.position
    val pattern = PathPatternPart(
      RelationshipChain(
        NodePattern(None, None, None, None)(pos),
        rel,
        NodePattern(None, None, None, None)(pos)
      )(pos)
    )
    parenthesizedPathPattern(pos, pattern, where = null, quantifier)
  }

  private def pathSelectorCannotBeUsedInClauseException(
    clauseName: String,
    selector: PatternPart.Selector
  ): Exception = {
    val p = selector.position
    astExceptionFactory.syntaxException(
      new Neo4jASTConstructionException(
        s"Path selectors such as `${selector.prettified}` cannot be used in a $clauseName clause, but only in a MATCH clause."
      ),
      p.offset,
      p.line,
      p.column
    )
  }

  private def pathSelectorNotAllowedWithinPathPatternKindException(
    pathPatternKind: String,
    selector: PatternPart.Selector
  ): Exception = {
    val p = selector.position
    astExceptionFactory.syntaxException(
      new Neo4jASTConstructionException(
        s"Path selectors such as `${selector.prettified}` are not supported within $pathPatternKind path patterns."
      ),
      p.offset,
      p.line,
      p.column
    )
  }

  // EXPRESSIONS

  override def newVariable(p: InputPosition, name: String): Variable = Variable(name)(p, Variable.isIsolatedDefault)

  override def newParameter(p: InputPosition, v: Variable, t: ParameterType): Parameter = {
    ExplicitParameter(v.name, transformParameterType(t))(p)
  }

  override def newParameter(p: InputPosition, offset: String, t: ParameterType): Parameter = {
    ExplicitParameter(offset, transformParameterType(t))(p)
  }

  private def transformParameterType(t: ParameterType) = {
    t match {
      case ParameterType.ANY     => CTAny
      case ParameterType.STRING  => CTString
      case ParameterType.INTEGER => CTInteger
      case ParameterType.MAP     => CTMap
      case _                     => throw new IllegalArgumentException("unknown parameter type: " + t.toString)
    }
  }

  override def newSensitiveStringParameter(p: InputPosition, v: Variable): Parameter =
    new ExplicitParameter(v.name, CTString)(p) with SensitiveParameter

  override def newSensitiveStringParameter(p: InputPosition, offset: String): Parameter =
    new ExplicitParameter(offset, CTString)(p) with SensitiveParameter

  override def newDouble(p: InputPosition, image: String): Expression = DecimalDoubleLiteral(image)(p)

  override def newDecimalInteger(p: InputPosition, image: String, negated: Boolean): Expression =
    if (negated) SignedDecimalIntegerLiteral("-" + image)(p)
    else SignedDecimalIntegerLiteral(image)(p)

  override def newHexInteger(p: InputPosition, image: String, negated: Boolean): Expression =
    if (negated) SignedHexIntegerLiteral("-" + image)(p)
    else SignedHexIntegerLiteral(image)(p)

  override def newOctalInteger(p: InputPosition, image: String, negated: Boolean): Expression =
    if (negated) SignedOctalIntegerLiteral("-" + image)(p)
    else SignedOctalIntegerLiteral(image)(p)

  override def newString(s: InputPosition, e: InputPosition, image: String): Expression =
    StringLiteral(image)(s.withInputLength(e.offset - s.offset + 1))

  override def newTrueLiteral(p: InputPosition): Expression = True()(p)

  override def newFalseLiteral(p: InputPosition): Expression = False()(p)

  override def newInfinityLiteral(p: InputPosition): Expression = Infinity()(p)

  override def newNaNLiteral(p: InputPosition): Expression = NaN()(p)

  override def newNullLiteral(p: InputPosition): Expression = Null()(p)

  override def listLiteral(p: InputPosition, values: util.List[Expression]): Expression = {
    ListLiteral(values.asScala.toList)(p)
  }

  override def mapLiteral(
    p: InputPosition,
    keys: util.List[StringPos[InputPosition]],
    values: util.List[Expression]
  ): Expression = {

    if (keys.size() != values.size()) {
      throw new Neo4jASTConstructionException(
        s"Map have the same number of keys and values, but got keys `${pretty(keys)}` and values `${pretty(values)}`"
      )
    }

    var i = 0
    val pairs = new Array[(PropertyKeyName, Expression)](keys.size())

    while (i < keys.size()) {
      val key = keys.get(i)
      pairs(i) = PropertyKeyName(key.string)(key.pos) -> values.get(i)
      i += 1
    }

    MapExpression(pairs.toIndexedSeq)(p)
  }

  override def property(subject: Expression, propertyKeyName: StringPos[InputPosition]): Property =
    Property(subject, PropertyKeyName(propertyKeyName.string)(propertyKeyName.pos))(subject.position)

  override def or(p: InputPosition, lhs: Expression, rhs: Expression): Expression = Or(lhs, rhs)(p)

  override def xor(p: InputPosition, lhs: Expression, rhs: Expression): Expression = Xor(lhs, rhs)(p)

  override def and(p: InputPosition, lhs: Expression, rhs: Expression): Expression = And(lhs, rhs)(p)

  override def ands(exprs: util.List[Expression]): Expression = Ands(exprs.asScala)(exprs.get(0).position)

  override def not(p: InputPosition, e: Expression): Expression = Not(e)(p)

  override def plus(p: InputPosition, lhs: Expression, rhs: Expression): Expression = Add(lhs, rhs)(p)

  override def concatenate(p: InputPosition, lhs: Expression, rhs: Expression): Expression = Concatenate(lhs, rhs)(p)

  override def minus(p: InputPosition, lhs: Expression, rhs: Expression): Expression = Subtract(lhs, rhs)(p)

  override def multiply(p: InputPosition, lhs: Expression, rhs: Expression): Expression = Multiply(lhs, rhs)(p)

  override def divide(p: InputPosition, lhs: Expression, rhs: Expression): Expression = Divide(lhs, rhs)(p)

  override def modulo(p: InputPosition, lhs: Expression, rhs: Expression): Expression = Modulo(lhs, rhs)(p)

  override def pow(p: InputPosition, lhs: Expression, rhs: Expression): Expression = Pow(lhs, rhs)(p)

  override def unaryPlus(e: Expression): Expression = unaryPlus(e.position, e)

  override def unaryPlus(p: InputPosition, e: Expression): Expression = UnaryAdd(e)(p)

  override def unaryMinus(p: InputPosition, e: Expression): Expression = UnarySubtract(e)(p)

  override def eq(p: InputPosition, lhs: Expression, rhs: Expression): Expression = Equals(lhs, rhs)(p)

  override def neq(p: InputPosition, lhs: Expression, rhs: Expression): Expression = InvalidNotEquals(lhs, rhs)(p)

  override def neq2(p: InputPosition, lhs: Expression, rhs: Expression): Expression = NotEquals(lhs, rhs)(p)

  override def lte(p: InputPosition, lhs: Expression, rhs: Expression): Expression = LessThanOrEqual(lhs, rhs)(p)

  override def gte(p: InputPosition, lhs: Expression, rhs: Expression): Expression = GreaterThanOrEqual(lhs, rhs)(p)

  override def lt(p: InputPosition, lhs: Expression, rhs: Expression): Expression = LessThan(lhs, rhs)(p)

  override def gt(p: InputPosition, lhs: Expression, rhs: Expression): Expression = GreaterThan(lhs, rhs)(p)

  override def regeq(p: InputPosition, lhs: Expression, rhs: Expression): Expression = RegexMatch(lhs, rhs)(p)

  override def startsWith(p: InputPosition, lhs: Expression, rhs: Expression): Expression = StartsWith(lhs, rhs)(p)

  override def endsWith(p: InputPosition, lhs: Expression, rhs: Expression): Expression = EndsWith(lhs, rhs)(p)

  override def contains(p: InputPosition, lhs: Expression, rhs: Expression): Expression = Contains(lhs, rhs)(p)

  override def in(p: InputPosition, lhs: Expression, rhs: Expression): Expression = In(lhs, rhs)(p)

  override def isNull(p: InputPosition, e: Expression): Expression = IsNull(e)(p)

  override def isNotNull(p: InputPosition, e: Expression): Expression = IsNotNull(e)(p)

  override def isTyped(p: InputPosition, e: Expression, javaType: ParserCypherTypeName): Expression = {
    val scalaType = convertCypherType(javaType)
    IsTyped(e, scalaType)(p, IsTyped.withDoubleColonOnlyDefault)
  }

  override def isNotTyped(p: InputPosition, e: Expression, javaType: ParserCypherTypeName): Expression = {
    val scalaType = convertCypherType(javaType)
    IsNotTyped(e, scalaType)(p)
  }

  override def isNormalized(p: InputPosition, e: Expression, normalForm: ParserNormalForm): Expression = {
    IsNormalized(e, convertNormalForm(normalForm))(p)
  }

  override def isNotNormalized(p: InputPosition, e: Expression, normalForm: ParserNormalForm): Expression = {
    IsNotNormalized(e, convertNormalForm(normalForm))(p)
  }

  override def listLookup(list: Expression, index: Expression): Expression = ContainerIndex(list, index)(index.position)

  override def listSlice(p: InputPosition, list: Expression, start: Expression, end: Expression): Expression = {
    ListSlice(list, Option(start), Option(end))(p)
  }

  override def newCountStar(p: InputPosition): Expression = CountStar()(p)

  override def functionInvocation(
    p: InputPosition,
    functionNamePosition: InputPosition,
    namespace: util.List[String],
    name: String,
    distinct: Boolean,
    arguments: util.List[Expression],
    calledFromUseClause: Boolean
  ): FunctionInvocation = {
    FunctionInvocation(
      FunctionName(Namespace(namespace.asScala.toList)(p), name)(functionNamePosition),
      distinct,
      arguments.asScala.toIndexedSeq,
      calledFromUseClause = calledFromUseClause
    )(p)
  }

  override def listComprehension(
    p: InputPosition,
    v: Variable,
    list: Expression,
    where: Expression,
    projection: Expression
  ): Expression =
    ListComprehension(v, list, Option(where), Option(projection))(p)

  override def patternComprehension(
    p: InputPosition,
    relationshipPatternPosition: InputPosition,
    v: Variable,
    pattern: PatternPart,
    where: Expression,
    projection: Expression
  ): Expression =
    PatternComprehension(
      Option(v),
      RelationshipsPattern(pattern.element.asInstanceOf[RelationshipChain])(relationshipPatternPosition),
      Option(where),
      projection
    )(p, None, None)

  override def reduceExpression(
    p: InputPosition,
    acc: Variable,
    accExpr: Expression,
    v: Variable,
    list: Expression,
    innerExpr: Expression
  ): Expression =
    ReduceExpression(acc, accExpr, v, list, innerExpr)(p)

  override def allExpression(p: InputPosition, v: Variable, list: Expression, where: Expression): Expression =
    AllIterablePredicate(v, list, Option(where))(p)

  override def anyExpression(p: InputPosition, v: Variable, list: Expression, where: Expression): Expression =
    AnyIterablePredicate(v, list, Option(where))(p)

  override def noneExpression(p: InputPosition, v: Variable, list: Expression, where: Expression): Expression =
    NoneIterablePredicate(v, list, Option(where))(p)

  override def singleExpression(p: InputPosition, v: Variable, list: Expression, where: Expression): Expression =
    SingleIterablePredicate(v, list, Option(where))(p)

  override def normalizeExpression(p: InputPosition, i: Expression, normalForm: ParserNormalForm): Expression = {
    FunctionInvocation(
      FunctionName(Normalize.name)(p),
      distinct = false,
      IndexedSeq(i, StringLiteral(normalForm.description())(p.withInputLength(0)))
    )(p)
  }

  override def trimFunction(
    p: InputPosition,
    trimSpec: ParserTrimSpecification,
    trimCharacterString: Expression,
    trimSource: Expression
  ): Expression = {
    if (trimCharacterString == null) {
      FunctionInvocation(
        FunctionName(Trim.name)(p),
        distinct = false,
        IndexedSeq(
          StringLiteral(trimSpec.description())(p.withInputLength(0)),
          trimSource
        )
      )(p)
    } else {
      FunctionInvocation(
        FunctionName(Trim.name)(p),
        distinct = false,
        IndexedSeq(
          StringLiteral(trimSpec.description())(p.withInputLength(0)),
          trimCharacterString,
          trimSource
        )
      )(p)
    }
  }

  override def patternExpression(p: InputPosition, pattern: PatternPart): Expression =
    pattern match {
      case paths: ShortestPathsPatternPart =>
        ShortestPathExpression(paths)
      case _ =>
        PatternExpression(RelationshipsPattern(pattern.element.asInstanceOf[RelationshipChain])(p))(
          None,
          None
        )
    }

  /** Exists and Count allow for PatternList and Optional Where, convert here to give a unified Exists / Count
   * containing a semantically valid Query. */
  private def convertSubqueryExpressionToUnifiedExpression(
    matchMode: MatchMode,
    patterns: util.List[PatternPart],
    query: Query,
    where: Where
  ): Query = {
    if (query != null) {
      query
    } else {
      val patternParts = patterns.asScala.toList.map {
        case p: PatternPartWithSelector => p
        case p: NonPrefixedPatternPart  => PatternPartWithSelector(allPathSelector(p.position), p)
      }
      val patternPos = patternParts.head.position
      val finalMatchMode = if (matchMode == null) MatchMode.default(patternPos) else matchMode
      SingleQuery(
        Seq(
          Match(optional = false, finalMatchMode, Pattern.ForMatch(patternParts)(patternPos), Seq.empty, Option(where))(
            patternPos
          )
        )
      )(patternPos)
    }
  }

  override def existsExpression(
    p: InputPosition,
    matchMode: MatchMode,
    patterns: util.List[PatternPart],
    query: Query,
    where: Where
  ): Expression = {
    ExistsExpression(convertSubqueryExpressionToUnifiedExpression(matchMode, patterns, query, where))(
      p,
      None,
      None
    )
  }

  override def countExpression(
    p: InputPosition,
    matchMode: MatchMode,
    patterns: util.List[PatternPart],
    query: Query,
    where: Where
  ): Expression = {
    CountExpression(convertSubqueryExpressionToUnifiedExpression(matchMode, patterns, query, where))(p, None, None)
  }

  override def collectExpression(
    p: InputPosition,
    query: Query
  ): Expression = {
    CollectExpression(query)(
      p,
      None,
      None
    )
  }

  override def mapProjection(p: InputPosition, v: Variable, items: util.List[MapProjectionElement]): Expression =
    MapProjection(v, items.asScala.toList)(p)

  override def mapProjectionLiteralEntry(property: StringPos[InputPosition], value: Expression): MapProjectionElement =
    LiteralEntry(PropertyKeyName(property.string)(property.pos), value)(value.position)

  override def mapProjectionProperty(property: StringPos[InputPosition]): MapProjectionElement =
    PropertySelector(PropertyKeyName(property.string)(property.pos))(property.pos)

  override def mapProjectionVariable(v: Variable): MapProjectionElement =
    VariableSelector(v)(v.position)

  override def mapProjectionAll(p: InputPosition): MapProjectionElement =
    AllPropertiesSelector()(p)

  override def caseExpression(
    p: InputPosition,
    e: Expression,
    whens: util.List[Expression],
    thens: util.List[Expression],
    elze: Expression
  ): Expression = {

    if (whens.size() != thens.size()) {
      throw new Neo4jASTConstructionException(
        s"Case expressions have the same number of whens and thens, but got whens `${pretty(whens)}` and thens `${pretty(thens)}`"
      )
    }

    val alternatives = new Array[(Expression, Expression)](whens.size())
    var i = 0
    while (i < whens.size()) {
      alternatives(i) = whens.get(i) -> thens.get(i)
      i += 1
    }
    CaseExpression(Option(e), alternatives.toIndexedSeq, Option(elze))(p)
  }

  override def inputPosition(offset: Int, line: Int, column: Int): InputPosition = InputPosition(offset, line, column)

  // Commands

  override def useGraph(command: StatementWithGraph, graph: UseGraph): StatementWithGraph = {
    command.withGraph(Option(graph))
  }

  // Show Commands

  override def yieldClause(
    p: InputPosition,
    returnAll: Boolean,
    returnItemList: util.List[ReturnItem],
    returnItemsP: InputPosition,
    order: util.List[SortItem],
    orderPos: InputPosition,
    skip: Expression,
    skipPosition: InputPosition,
    limit: Expression,
    limitPosition: InputPosition,
    where: Where
  ): Yield = {

    val returnItems = ReturnItems(returnAll, returnItemList.asScala.toList)(returnItemsP)

    Yield(
      returnItems,
      Option(order.asScala.toList).filter(_.nonEmpty).map(o => OrderBy(o)(orderPos)),
      Option(skip).map(s => Skip(s)(skipPosition)),
      Option(limit).map(l => Limit(l)(limitPosition)),
      Option(where)
    )(p)
  }

  private def convertCypherType(javaType: ParserCypherTypeName): CypherType = {
    val pos = inputPosition(javaType.getOffset, javaType.getLine, javaType.getColumn)
    val cypherTypeName = javaType match {
      case ParserCypherTypeName.NOTHING =>
        NothingType()(pos)
      case ParserCypherTypeName.NULL =>
        NullType()(pos)
      case ParserCypherTypeName.BOOLEAN =>
        BooleanType(isNullable = true)(pos)
      case ParserCypherTypeName.BOOLEAN_NOT_NULL =>
        BooleanType(isNullable = false)(pos)
      case ParserCypherTypeName.STRING =>
        StringType(isNullable = true)(pos)
      case ParserCypherTypeName.STRING_NOT_NULL =>
        StringType(isNullable = false)(pos)
      case ParserCypherTypeName.INTEGER =>
        IntegerType(isNullable = true)(pos)
      case ParserCypherTypeName.INTEGER_NOT_NULL =>
        IntegerType(isNullable = false)(pos)
      case ParserCypherTypeName.FLOAT =>
        FloatType(isNullable = true)(pos)
      case ParserCypherTypeName.FLOAT_NOT_NULL =>
        FloatType(isNullable = false)(pos)
      case ParserCypherTypeName.DATE =>
        DateType(isNullable = true)(pos)
      case ParserCypherTypeName.DATE_NOT_NULL =>
        DateType(isNullable = false)(pos)
      case ParserCypherTypeName.LOCAL_TIME =>
        LocalTimeType(isNullable = true)(pos)
      case ParserCypherTypeName.LOCAL_TIME_NOT_NULL =>
        LocalTimeType(isNullable = false)(pos)
      case ParserCypherTypeName.ZONED_TIME =>
        ZonedTimeType(isNullable = true)(pos)
      case ParserCypherTypeName.ZONED_TIME_NOT_NULL =>
        ZonedTimeType(isNullable = false)(pos)
      case ParserCypherTypeName.LOCAL_DATETIME =>
        LocalDateTimeType(isNullable = true)(pos)
      case ParserCypherTypeName.LOCAL_DATETIME_NOT_NULL =>
        LocalDateTimeType(isNullable = false)(pos)
      case ParserCypherTypeName.ZONED_DATETIME =>
        ZonedDateTimeType(isNullable = true)(pos)
      case ParserCypherTypeName.ZONED_DATETIME_NOT_NULL =>
        ZonedDateTimeType(isNullable = false)(pos)
      case ParserCypherTypeName.DURATION =>
        DurationType(isNullable = true)(pos)
      case ParserCypherTypeName.DURATION_NOT_NULL =>
        DurationType(isNullable = false)(pos)
      case ParserCypherTypeName.POINT =>
        PointType(isNullable = true)(pos)
      case ParserCypherTypeName.POINT_NOT_NULL =>
        PointType(isNullable = false)(pos)
      case ParserCypherTypeName.NODE =>
        NodeType(isNullable = true)(pos)
      case ParserCypherTypeName.NODE_NOT_NULL =>
        NodeType(isNullable = false)(pos)
      case ParserCypherTypeName.RELATIONSHIP =>
        RelationshipType(isNullable = true)(pos)
      case ParserCypherTypeName.RELATIONSHIP_NOT_NULL =>
        RelationshipType(isNullable = false)(pos)
      case ParserCypherTypeName.MAP =>
        MapType(isNullable = true)(pos)
      case ParserCypherTypeName.MAP_NOT_NULL =>
        MapType(isNullable = false)(pos)
      case l: ParserCypherTypeName.ListParserCypherTypeName =>
        val inner = convertCypherType(l.getInnerType)
        ListType(inner, l.isNullable)(pos)
      case ParserCypherTypeName.PATH =>
        PathType(isNullable = true)(pos)
      case ParserCypherTypeName.PATH_NOT_NULL =>
        PathType(isNullable = false)(pos)
      case ParserCypherTypeName.PROPERTY_VALUE =>
        PropertyValueType(isNullable = true)(pos)
      case ParserCypherTypeName.PROPERTY_VALUE_NOT_NULL =>
        PropertyValueType(isNullable = false)(pos)
      case ParserCypherTypeName.ANY =>
        AnyType(isNullable = true)(pos)
      case ParserCypherTypeName.ANY_NOT_NULL =>
        AnyType(isNullable = false)(pos)
      case dynamicUnion: ParserCypherTypeName.ClosedDynamicUnionParserCypherTypeName =>
        val unionOfTypes: Set[CypherType] = dynamicUnion.getUnionTypes.stream().map[CypherType](unionType =>
          convertCypherType(unionType)
        ).toList.asScala.toSet
        ClosedDynamicUnionType(unionOfTypes)(pos)
      case ct =>
        throw new Neo4jASTConstructionException(s"Unknown Cypher type: $ct")
    }

    cypherTypeName.simplify
  }

  private def convertNormalForm(javaType: ParserNormalForm): NormalForm = {
    javaType match {
      case ParserNormalForm.NFC  => NFCNormalForm
      case ParserNormalForm.NFD  => NFDNormalForm
      case ParserNormalForm.NFKC => NFKCNormalForm
      case ParserNormalForm.NFKD => NFKDNormalForm
      case nf =>
        throw new Neo4jASTConstructionException(s"Unknown Normal Form: $nf")
    }
  }

  private def pretty[T <: AnyRef](ts: util.List[T]): String = {
    ts.stream().map[String](t => t.toString).collect(Collectors.joining(","))
  }

  private def stringLiteralOrParameterExpression(name: Either[StringPos[InputPosition], Parameter]): Expression =
    name match {
      case Left(literal) =>
        StringLiteral(literal.string)(literal.pos.withInputLength(literal.endPos.offset - literal.pos.offset + 1))
      case Right(param) => param
    }

  override def labelConjunction(
    p: InputPosition,
    lhs: LabelExpression,
    rhs: LabelExpression,
    containsIs: Boolean
  ): LabelExpression =
    LabelExpression.Conjunctions.flat(lhs, rhs, p, containsIs)

  override def labelDisjunction(
    p: InputPosition,
    lhs: LabelExpression,
    rhs: LabelExpression,
    containsIs: Boolean
  ): LabelExpression = {
    LabelExpression.Disjunctions.flat(lhs, rhs, p, containsIs)
  }

  override def labelNegation(p: InputPosition, e: LabelExpression, containsIs: Boolean): LabelExpression =
    LabelExpression.Negation(e, containsIs)(p)

  override def labelWildcard(p: InputPosition, containsIs: Boolean): LabelExpression =
    LabelExpression.Wildcard(containsIs)(p)

  override def labelLeaf(p: InputPosition, n: String, entityType: EntityType, containsIs: Boolean): LabelExpression =
    entityType match {
      case EntityType.NODE                 => Leaf(LabelName(n)(p), containsIs)
      case EntityType.NODE_OR_RELATIONSHIP => Leaf(LabelOrRelTypeName(n)(p), containsIs)
      case EntityType.RELATIONSHIP         => Leaf(RelTypeName(n)(p), containsIs)
    }

  override def dynamicLabelLeaf(
    p: InputPosition,
    n: Expression,
    entityType: EntityType,
    all: Boolean,
    containsIs: Boolean
  ): LabelExpression =
    entityType match {
      case EntityType.NODE                 => DynamicLeaf(DynamicLabelExpression(n, all)(p), containsIs)
      case EntityType.NODE_OR_RELATIONSHIP => DynamicLeaf(DynamicLabelOrRelTypeExpression(n, all)(p), containsIs)
      case EntityType.RELATIONSHIP         => DynamicLeaf(DynamicRelTypeExpression(n, all)(p), containsIs)
    }

  override def labelColonConjunction(
    p: InputPosition,
    lhs: LabelExpression,
    rhs: LabelExpression,
    containsIs: Boolean
  ): LabelExpression =
    LabelExpression.ColonConjunction(lhs, rhs, containsIs)(p)

  override def labelColonDisjunction(
    p: InputPosition,
    lhs: LabelExpression,
    rhs: LabelExpression,
    containsIs: Boolean
  ): LabelExpression =
    LabelExpression.ColonDisjunction(lhs, rhs, containsIs)(p)

  override def labelExpressionPredicate(subject: Expression, exp: LabelExpression): Expression =
    LabelExpressionPredicate(subject, exp)(
      subject.position,
      isParenthesized = LabelExpressionPredicate.isParenthesizedDefault
    )

  override def nodeType(): EntityType = EntityType.NODE

  override def relationshipType(): EntityType = EntityType.RELATIONSHIP

  override def nodeOrRelationshipType(): EntityType = EntityType.NODE_OR_RELATIONSHIP

  override def addDeprecatedIdentifierUnicodeNotification(
    p: InputPosition,
    char: Character,
    identifier: String
  ): Unit = {
    if (logger != null) {
      if (char == '\u0085') {
        logger.log(DeprecatedIdentifierWhitespaceUnicode(p, char, identifier))
      } else {
        logger.log(DeprecatedIdentifierUnicode(p, char, identifier))
      }
    }
  }
}
