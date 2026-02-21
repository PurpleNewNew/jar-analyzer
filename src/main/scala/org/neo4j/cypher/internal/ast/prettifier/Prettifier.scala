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
package org.neo4j.cypher.internal.ast.prettifier

import org.neo4j.cypher.internal.ast.AddedInRewrite
import org.neo4j.cypher.internal.ast.AdministrationCommand
import org.neo4j.cypher.internal.ast.AliasedReturnItem
import org.neo4j.cypher.internal.ast.AscSortItem
import org.neo4j.cypher.internal.ast.Clause
import org.neo4j.cypher.internal.ast.Create
import org.neo4j.cypher.internal.ast.DatabaseName
import org.neo4j.cypher.internal.ast.Delete
import org.neo4j.cypher.internal.ast.DescSortItem
import org.neo4j.cypher.internal.ast.Finish
import org.neo4j.cypher.internal.ast.Foreach
import org.neo4j.cypher.internal.ast.GraphDirectReference
import org.neo4j.cypher.internal.ast.GraphFunctionReference
import org.neo4j.cypher.internal.ast.Hint
import org.neo4j.cypher.internal.ast.ImportingWithSubqueryCall
import org.neo4j.cypher.internal.ast.Insert
import org.neo4j.cypher.internal.ast.Limit
import org.neo4j.cypher.internal.ast.Match
import org.neo4j.cypher.internal.ast.Merge
import org.neo4j.cypher.internal.ast.MergeAction
import org.neo4j.cypher.internal.ast.NamespacedName
import org.neo4j.cypher.internal.ast.OnCreate
import org.neo4j.cypher.internal.ast.OnMatch
import org.neo4j.cypher.internal.ast.OrderBy
import org.neo4j.cypher.internal.ast.ParameterName
import org.neo4j.cypher.internal.ast.ParsedAsYield
import org.neo4j.cypher.internal.ast.ProcedureResult
import org.neo4j.cypher.internal.ast.ProcedureResultItem
import org.neo4j.cypher.internal.ast.ProjectingUnionAll
import org.neo4j.cypher.internal.ast.ProjectingUnionDistinct
import org.neo4j.cypher.internal.ast.Query
import org.neo4j.cypher.internal.ast.Remove
import org.neo4j.cypher.internal.ast.RemoveDynamicPropertyItem
import org.neo4j.cypher.internal.ast.RemoveItem
import org.neo4j.cypher.internal.ast.RemoveLabelItem
import org.neo4j.cypher.internal.ast.RemovePropertyItem
import org.neo4j.cypher.internal.ast.Return
import org.neo4j.cypher.internal.ast.ReturnItem
import org.neo4j.cypher.internal.ast.ReturnItems
import org.neo4j.cypher.internal.ast.SchemaCommand
import org.neo4j.cypher.internal.ast.ScopeClauseSubqueryCall
import org.neo4j.cypher.internal.ast.SetClause
import org.neo4j.cypher.internal.ast.SetDynamicPropertyItem
import org.neo4j.cypher.internal.ast.SetExactPropertiesFromMapItem
import org.neo4j.cypher.internal.ast.SetIncludingPropertiesFromMapItem
import org.neo4j.cypher.internal.ast.SetItem
import org.neo4j.cypher.internal.ast.SetLabelItem
import org.neo4j.cypher.internal.ast.SetPropertyItem
import org.neo4j.cypher.internal.ast.SetPropertyItems
import org.neo4j.cypher.internal.ast.SingleQuery
import org.neo4j.cypher.internal.ast.Skip
import org.neo4j.cypher.internal.ast.Statement
import org.neo4j.cypher.internal.ast.SubqueryCall.InTransactionsConcurrencyParameters
import org.neo4j.cypher.internal.ast.SubqueryCall.InTransactionsOnErrorBehaviour.OnErrorBreak
import org.neo4j.cypher.internal.ast.SubqueryCall.InTransactionsOnErrorBehaviour.OnErrorContinue
import org.neo4j.cypher.internal.ast.SubqueryCall.InTransactionsOnErrorBehaviour.OnErrorFail
import org.neo4j.cypher.internal.ast.SubqueryCall.InTransactionsParameters
import org.neo4j.cypher.internal.ast.UnaliasedReturnItem
import org.neo4j.cypher.internal.ast.Union
import org.neo4j.cypher.internal.ast.UnionAll
import org.neo4j.cypher.internal.ast.UnionDistinct
import org.neo4j.cypher.internal.ast.UnresolvedCall
import org.neo4j.cypher.internal.ast.Unwind
import org.neo4j.cypher.internal.ast.UseGraph
import org.neo4j.cypher.internal.ast.UsingIndexHint
import org.neo4j.cypher.internal.ast.UsingIndexHint.SeekOnly
import org.neo4j.cypher.internal.ast.UsingIndexHint.UsingAnyIndexType
import org.neo4j.cypher.internal.ast.UsingIndexHint.UsingPointIndexType
import org.neo4j.cypher.internal.ast.UsingIndexHint.UsingRangeIndexType
import org.neo4j.cypher.internal.ast.UsingIndexHint.UsingTextIndexType
import org.neo4j.cypher.internal.ast.UsingJoinHint
import org.neo4j.cypher.internal.ast.UsingScanHint
import org.neo4j.cypher.internal.ast.UsingStatefulShortestPathAll
import org.neo4j.cypher.internal.ast.UsingStatefulShortestPathInto
import org.neo4j.cypher.internal.ast.Where
import org.neo4j.cypher.internal.ast.With
import org.neo4j.cypher.internal.ast.Yield
import org.neo4j.cypher.internal.ast.prettifier.Prettifier.escapeName
import org.neo4j.cypher.internal.expressions.CoerceTo
import org.neo4j.cypher.internal.expressions.Expression
import org.neo4j.cypher.internal.expressions.FunctionInvocation
import org.neo4j.cypher.internal.expressions.ImplicitProcedureArgument
import org.neo4j.cypher.internal.expressions.LabelName
import org.neo4j.cypher.internal.expressions.LogicalVariable
import org.neo4j.cypher.internal.expressions.Parameter
import org.neo4j.cypher.internal.expressions.StringLiteral

//noinspection DuplicatedCode
case class Prettifier(
  expr: ExpressionStringifier,
  extension: Prettifier.ClausePrettifier = Prettifier.EmptyExtension
) {

  val NL: String = System.lineSeparator()
  val BASE_INDENT: String = "  "

  private val base = IndentingQueryPrettifier()

  def asString(statement: Statement): String = statement match {
    case q: Query => base.query(q)
    case _: SchemaCommand | _: AdministrationCommand =>
      throw new UnsupportedOperationException("feature disabled: administration/schema commands")
    case _ => throw new IllegalStateException(s"Unknown statement: $statement")
  }

  def asString(hint: Hint): String = base.asString(hint)

  def backtick(s: String): String = expr.backtick(s)

  def prettifySetItems(setItems: Seq[SetItem]): String = {
    val items = setItems.map {
      case SetPropertyItem(prop, exp)        => s"${expr(prop)} = ${expr(exp)}"
      case SetDynamicPropertyItem(prop, exp) => s"${expr(prop)} = ${expr(exp)}"
      case SetPropertyItems(entity, items) =>
        items.map(i => s"${expr(entity)}.${i._1.name} = ${expr(i._2)}").mkString(", ")
      case SetLabelItem(variable, labels, dynamicLabels, false) => labelsString(variable, labels, dynamicLabels)
      case SetLabelItem(variable, labels, dynamicLabels, true)  => isLabelsString(variable, labels, dynamicLabels)
      case SetIncludingPropertiesFromMapItem(variable, exp, _)  => s"${expr(variable)} += ${expr(exp)}"
      case SetExactPropertiesFromMapItem(variable, exp, _)      => s"${expr(variable)} = ${expr(exp)}"
    }
    items.mkString(", ")
  }

  def prettifyRemoveItems(removeItems: Seq[RemoveItem]): String = {
    val items = removeItems.map {
      case RemovePropertyItem(prop)                                => s"${expr(prop)}"
      case RemoveDynamicPropertyItem(dynamicPropertyLookup)        => s"${expr(dynamicPropertyLookup)}"
      case RemoveLabelItem(variable, labels, dynamicLabels, false) => labelsString(variable, labels, dynamicLabels)
      case RemoveLabelItem(variable, labels, dynamicLabels, true)  => isLabelsString(variable, labels, dynamicLabels)
    }
    items.mkString(", ")
  }

  private def labelsString(
    variable: LogicalVariable,
    labels: Seq[LabelName],
    dynamicLabels: Seq[Expression]
  ): String = {
    expr(variable) + labelsOrderedSeq(labels, dynamicLabels).map(l => s":$l").mkString("")
  }

  private def isLabelsString(
    variable: LogicalVariable,
    labels: Seq[LabelName],
    dynamicLabels: Seq[Expression]
  ): String = {
    val labelsStrings: Seq[String] = labelsOrderedSeq(labels, dynamicLabels)
    expr(variable) + " IS " + labelsStrings.head + labelsStrings.tail.map(l => s":$l").mkString("")
  }

  private def labelsOrderedSeq(labels: Seq[LabelName], dynamicLabels: Seq[Expression]): Seq[String] = {
    (labels ++ dynamicLabels).map {
      case l: LabelName  => (s"${expr(l)}", l.position)
      case d: Expression => (s"$$(${expr(d)})", d.position)
      case _             => throw new IllegalStateException("Unreachable state.")
    }.sortBy(pos => (pos._2.line, pos._2.column)).map(_._1)
  }

  case class IndentingQueryPrettifier(indentLevel: Int = 0) extends Prettifier.QueryPrettifier {
    def indented(): IndentingQueryPrettifier = copy(indentLevel + 1)
    val INDENT: String = BASE_INDENT * indentLevel

    private def asNewLine(l: String): String = NL + l

    private def appendSpaceIfNonEmpty(s: String): String = if (s.nonEmpty) s"$s " else s

    def query(q: Query): String =
      q match {
        case SingleQuery(clauses) =>
          // Need to filter away empty strings as SHOW/TERMINATE commands might get an empty string from YIELD/WITH/RETURN clauses
          clauses.map(dispatch).filter(_.nonEmpty).mkString(NL)

        case union: Union =>
          val lhs = query(union.lhs)
          val rhs = query(union.rhs)
          val operation = union match {
            case _: UnionAll | _: ProjectingUnionAll           => s"${INDENT}UNION ALL"
            case _: UnionDistinct | _: ProjectingUnionDistinct => s"${INDENT}UNION"
          }
          Seq(lhs, operation, rhs).mkString(NL)
      }

    def asString(clause: Clause): String = dispatch(clause)

    def dispatch(clause: Clause): String = clause match {
      case u: UseGraph                    => asString(u)
      case e: Return                      => asString(e)
      case f: Finish                      => asString(f)
      case m: Match                       => asString(m)
      case c: ImportingWithSubqueryCall   => asString(c)
      case c: ScopeClauseSubqueryCall     => asString(c)
      case w: With                        => asString(w)
      case y: Yield                       => asString(y)
      case c: Create                      => asString(c)
      case i: Insert                      => asString(i)
      case u: Unwind                      => asString(u)
      case u: UnresolvedCall              => asString(u)
      case s: SetClause                   => asString(s)
      case r: Remove                      => asString(r)
      case d: Delete                      => asString(d)
      case m: Merge                       => asString(m)
      case f: Foreach                     => asString(f)
      case c =>
        val ext = extension.asString(this)
        ext.applyOrElse(c, fallback)
    }

    private def fallback(clause: Clause): String =
      clause.asCanonicalStringVal

    def asString(u: UseGraph): String = {
      u.graphReference match {
        case GraphDirectReference(catalogName) => s"${INDENT}USE ${catalogName.asCanonicalNameString}"
        case GraphFunctionReference(functionInvocation: FunctionInvocation) =>
          s"${INDENT}USE ${expr(functionInvocation)}"
      }
    }

    def asString(m: Match): String = {
      val o = if (m.optional) "OPTIONAL " else ""
      val mm = appendSpaceIfNonEmpty(m.matchMode.prettified)
      val p = expr.patterns.apply(m.pattern)
      val ind = indented()
      val w = m.where.map(ind.asString).map(asNewLine).getOrElse("")
      val h = m.hints.map(ind.asString).map(asNewLine).mkString
      s"$INDENT${o}MATCH $mm$p$h$w"
    }

    def asString(c: ImportingWithSubqueryCall): String = {
      val optional = if (c.optional) "OPTIONAL " else ""
      val inTxParams = c.inTransactionsParameters.map(asString).getOrElse("")
      s"""${INDENT}${optional}CALL {
         |${indented().query(c.innerQuery)}
         |$INDENT}$inTxParams""".stripMargin
    }

    def asString(c: ScopeClauseSubqueryCall): String = {
      val optional = if (c.optional) "OPTIONAL " else ""
      val inTxParams = c.inTransactionsParameters.map(asString).getOrElse("")
      s"""${INDENT}${optional}CALL (${if (c.isImportingAll) "*"
        else c.importedVariables.map(expr(_)).mkString("", ",", "")}) {
         |${indented().query(c.innerQuery)}
         |$INDENT}$inTxParams""".stripMargin
    }

    def asString(ip: InTransactionsParameters): String = {
      val ofRows = ip.batchParams.map(_.batchSize) match {
        case Some(size) => " OF " + expr(size) + " ROWS"
        case None       => ""
      }
      val concurrency = ip.concurrencyParams match {
        case Some(InTransactionsConcurrencyParameters(Some(explicit))) => " " + expr(explicit) + " CONCURRENT"
        case Some(InTransactionsConcurrencyParameters(None))           => " CONCURRENT"
        case None                                                      => ""
      }
      val onError = ip.errorParams.map(_.behaviour) match {
        case Some(OnErrorBreak)    => s" ON ERROR BREAK"
        case Some(OnErrorContinue) => s" ON ERROR CONTINUE"
        case Some(OnErrorFail)     => s" ON ERROR FAIL"
        case None                  => ""
      }
      val reportStatus = ip.reportParams.map(_.reportAs) match {
        case Some(statusVar) => s" REPORT STATUS AS ${ExpressionStringifier.backtick(statusVar.name)}"
        case None            => ""
      }
      s" IN$concurrency TRANSACTIONS$ofRows$onError$reportStatus"
    }

    def asString(w: Where): String =
      s"${INDENT}WHERE ${expr(w.expression)}"

    def asString(m: Hint): String = {
      m match {
        case UsingIndexHint(v, l, ps, s, t) => Seq(
            s"${INDENT}USING ",
            t match {
              case UsingAnyIndexType   => "INDEX "
              case UsingTextIndexType  => "TEXT INDEX "
              case UsingRangeIndexType => "RANGE INDEX "
              case UsingPointIndexType => "POINT INDEX "
            },
            if (s == SeekOnly) "SEEK " else "",
            expr(v),
            ":",
            expr(l),
            ps.map(expr(_)).mkString("(", ",", ")")
          ).mkString

        case UsingScanHint(v, l) => Seq(
            s"${INDENT}USING SCAN ",
            expr(v),
            ":",
            expr(l)
          ).mkString

        case UsingJoinHint(vs) => Seq(
            s"${INDENT}USING JOIN ON ",
            vs.map(expr(_)).toIterable.mkString(", ")
          ).mkString

        // Note: This hint cannot be written in Cypher.
        case UsingStatefulShortestPathAll(vs) => Seq(
            s"${INDENT}USING SSP_ALL ON ",
            vs.map(expr(_)).toIterable.mkString(", ")
          ).mkString

        // Note: This hint cannot be written in Cypher.
        case UsingStatefulShortestPathInto(vs) => Seq(
            s"${INDENT}USING SSP_INTO ON ",
            vs.map(expr(_)).toIterable.mkString(", ")
          ).mkString
      }
    }

    def asString(ma: MergeAction): String = ma match {
      case OnMatch(set)  => s"${INDENT}ON MATCH ${asString(set)}"
      case OnCreate(set) => s"${INDENT}ON CREATE ${asString(set)}"
    }

    def asString(m: Merge): String = {
      val p = expr.patterns.apply(m.pattern)
      val ind = indented()
      val a = m.actions.map(ind.asString).map(asNewLine).mkString
      s"${INDENT}MERGE $p$a"
    }

    def asString(o: Skip): String = s"${INDENT}SKIP ${expr(o.expression)}"
    def asString(o: Limit): String = s"${INDENT}LIMIT ${expr(o.expression)}"

    def asString(o: OrderBy): String = s"${INDENT}ORDER BY " + {
      o.sortItems.map {
        case AscSortItem(expression)  => expr(expression) + " ASCENDING"
        case DescSortItem(expression) => expr(expression) + " DESCENDING"
      }.mkString(", ")
    }

    def asString(r: ReturnItem): String = r match {
      case AliasedReturnItem(e, v)   => expr(e) + " AS " + expr(v)
      case UnaliasedReturnItem(e, _) => expr(e)
    }

    def asString(r: ReturnItems): String = {
      val as = if (r.includeExisting) Seq("*") else Seq()
      val is = r.items.map(asString)
      (as ++ is).mkString(", ")
    }

    def asString(r: Return): String =
      if (r.addedInRewrite) ""
      else {
        val d = if (r.distinct) " DISTINCT" else ""
        val i = asString(r.returnItems)
        val ind = indented()
        val o = r.orderBy.map(ind.asString).map(asNewLine).getOrElse("")
        val l = r.limit.map(ind.asString).map(asNewLine).getOrElse("")
        val s = r.skip.map(ind.asString).map(asNewLine).getOrElse("")
        s"${INDENT}RETURN$d $i$o$s$l"
      }

    def asString(f: Finish): String = s"${INDENT}FINISH"

    def asString(w: With): String = {
      val ind = indented()
      val rewrittenClauses = List(
        w.orderBy.map(ind.asString),
        w.skip.map(ind.asString),
        w.limit.map(ind.asString),
        w.where.map(ind.asString)
      ).flatten

      if (w.withType == ParsedAsYield || w.withType == AddedInRewrite) {
        // part of SHOW/TERMINATE TRANSACTION which prettifies the YIELD items part
        // but it no longer knows the subclauses, hence prettifying them here

        // only add newlines between subclauses and not in front of the first one
        if (rewrittenClauses.nonEmpty)
          s"$INDENT${rewrittenClauses.head}${rewrittenClauses.tail.map(asNewLine).mkString}"
        else ""
      } else {
        val d = if (w.distinct) " DISTINCT" else ""
        val i = asString(w.returnItems)

        s"${INDENT}WITH$d $i${rewrittenClauses.map(asNewLine).mkString}"
      }
    }

    def asString(y: Yield): String = {
      val i = asString(y.returnItems)
      val ind = indented()
      val o = y.orderBy.map(ind.asString).map(asNewLine).getOrElse("")
      val l = y.limit.map(ind.asString).map(asNewLine).getOrElse("")
      val s = y.skip.map(ind.asString).map(asNewLine).getOrElse("")
      val wh = y.where.map(ind.asString).map(asNewLine).getOrElse("")
      s"${INDENT}YIELD $i$o$s$l$wh"
    }

    def asString(c: Create): String = {
      val p = expr.patterns.apply(c.pattern)
      s"${INDENT}CREATE $p"
    }

    def asString(i: Insert): String = {
      val p = expr.patterns.apply(i.pattern)
      s"${INDENT}INSERT $p"
    }

    def asString(u: Unwind): String = {
      s"${INDENT}UNWIND ${expr(u.expression)} AS ${expr(u.variable)}"
    }

    def asString(u: UnresolvedCall): String = {
      val namespace = expr(u.procedureNamespace)
      val optional = if (u.optional) "OPTIONAL " else ""
      val prefix = if (namespace.isEmpty) "" else namespace + "."
      val args = u.declaredArguments.map(_.filter {
        case CoerceTo(_: ImplicitProcedureArgument, _) => false
        case _: ImplicitProcedureArgument              => false
        case _                                         => true
      })
      val arguments = args.map(list => list.map(expr(_)).mkString("(", ", ", ")")).getOrElse("")
      val ind = indented()
      val yields =
        if (u.yieldAll) asNewLine(s"${indented().INDENT}YIELD *")
        else u.declaredResult.filter(_.items.nonEmpty).map(ind.asString).map(asNewLine).getOrElse("")
      s"${INDENT}${optional}CALL $prefix${expr(u.procedureName)}$arguments$yields"
    }

    def asString(r: ProcedureResult): String = {
      def item(i: ProcedureResultItem) = i.output.map(expr(_) + " AS ").getOrElse("") + expr(i.variable)
      val items = r.items.map(item).mkString(", ")
      val ind = indented()
      val where = r.where.map(ind.asString).map(asNewLine).getOrElse("")
      s"${INDENT}YIELD $items$where"
    }

    def asString(s: SetClause): String = {
      s"${INDENT}SET ${prettifySetItems(s.items)}"
    }

    def asString(r: Remove): String = {
      s"${INDENT}REMOVE ${prettifyRemoveItems(r.items)}"
    }

    def asString(delete: Delete): String = {
      val detach = if (delete.forced) "DETACH " else ""
      s"$INDENT${detach}DELETE ${delete.expressions.map(expr(_)).mkString(", ")}"
    }

    def asString(foreach: Foreach): String = {
      val varName = expr(foreach.variable)
      val list = expr(foreach.expression)
      val updates = foreach.updates.map(dispatch).mkString(s"$NL  ", s"$NL  ", NL)
      s"${INDENT}FOREACH ( $varName IN $list |$updates)"
    }

  }
}

object Prettifier {

  trait QueryPrettifier {
    def INDENT: String
    def asString(clause: Clause): String
  }

  trait ClausePrettifier {
    def asString(ctx: QueryPrettifier): PartialFunction[Clause, String]
  }

  object EmptyExtension extends ClausePrettifier {
    def asString(ctx: QueryPrettifier): PartialFunction[Clause, String] = PartialFunction.empty
  }

  def escapeName(name: Either[String, Parameter]): String = name match {
    case Left(s)  => ExpressionStringifier.backtick(s)
    case Right(p) => s"$$${ExpressionStringifier.backtick(p.name)}"
  }

  def escapeName(name: DatabaseName)(implicit d: DummyImplicit): String = name match {
    case NamespacedName(names, Some(namespace)) =>
      ExpressionStringifier.backtick(namespace) + "." + ExpressionStringifier.backtick(names.mkString("."))
    case NamespacedName(names, None) => ExpressionStringifier.backtick(names.mkString("."))
    case ParameterName(p)            => "$" + ExpressionStringifier.backtick(p.name)
  }

  val escapeName: PartialFunction[Expression, String] = {
    case StringLiteral(s) => ExpressionStringifier.backtick(s)
    case p: Parameter     => s"$$${ExpressionStringifier.backtick(p.name)}"
  }

  def escapeNames(names: Seq[Expression]): String = names.map(escapeName).mkString(", ")

  def escapeNames(names: Seq[DatabaseName])(implicit d: DummyImplicit): String =
    names.map(databaseName => escapeName(databaseName)).mkString(", ")

  def maybeImmutable(immutable: Boolean): String = if (immutable) " IMMUTABLE" else ""

}
