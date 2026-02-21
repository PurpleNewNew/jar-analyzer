/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [https://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.neo4j.cypher.internal.runtime.core

import org.neo4j.cypher.internal.config.CUSTOM_MEMORY_TRACKING
import org.neo4j.cypher.internal.config.MEMORY_TRACKING
import org.neo4j.cypher.internal.config.MemoryTrackingController
import org.neo4j.cypher.internal.config.NO_TRACKING
import org.neo4j.cypher.internal.runtime.InputDataStream
import org.neo4j.cypher.internal.runtime.QueryContext
import org.neo4j.cypher.internal.runtime.QueryTransactionMode
import org.neo4j.cypher.internal.runtime.StartsConcurrentTransactions
import org.neo4j.cypher.internal.runtime.core.pipes.NullPipeDecorator
import org.neo4j.cypher.internal.runtime.core.pipes.Pipe
import org.neo4j.cypher.internal.runtime.core.pipes.PipeDecorator
import org.neo4j.cypher.internal.runtime.core.pipes.QueryState
import org.neo4j.cypher.internal.runtime.core.profiler.RuntimeProfileInformation
import org.neo4j.cypher.internal.runtime.memory.CustomTrackingQueryMemoryTracker
import org.neo4j.cypher.internal.runtime.memory.NoOpQueryMemoryTracker
import org.neo4j.cypher.internal.runtime.memory.ParallelTrackingQueryMemoryTracker
import org.neo4j.cypher.internal.runtime.memory.ProfilingParallelTrackingQueryMemoryTracker
import org.neo4j.cypher.internal.runtime.memory.QueryMemoryTracker
import org.neo4j.cypher.internal.runtime.memory.TrackingQueryMemoryTracker
import org.neo4j.cypher.internal.runtime.memory.TransactionWorkerThreadDelegatingMemoryTracker
import org.neo4j.cypher.result.RuntimeResult
import org.neo4j.internal.kernel.api.DefaultCloseListenable
import org.neo4j.kernel.impl.query.QuerySubscriber
import org.neo4j.scheduler.CallableExecutor
import org.neo4j.scheduler.Group
import org.neo4j.values.virtual.MapValue

abstract class BaseExecutionResultBuilderFactory(
  pipe: Pipe,
  columns: Seq[String],
  transactionMode: QueryTransactionMode
) extends ExecutionResultBuilderFactory {

  abstract class BaseExecutionResultBuilder() extends ExecutionResultBuilder {
    protected var pipeDecorator: PipeDecorator = NullPipeDecorator

    protected val transactionWorkerExecutor: Option[CallableExecutor] = transactionMode match {
      case StartsConcurrentTransactions =>
        Some(queryContext.jobScheduler.executor(Group.CYPHER_TRANSACTION_WORKER))
      case _ => None
    }

    protected def createQueryMemoryTracker(
      memoryTrackingController: MemoryTrackingController,
      profile: Boolean,
      queryContext: QueryContext
    ): QueryMemoryTracker = {
      (memoryTrackingController.memoryTracking, transactionMode) match {
        case (NO_TRACKING, _) => NoOpQueryMemoryTracker
        case (MEMORY_TRACKING, StartsConcurrentTransactions) =>
          val delegateFactory = () => {
            new TransactionWorkerThreadDelegatingMemoryTracker
          }
          val mainThreadMemoryTracker = queryContext.transactionalContext.createExecutionContextMemoryTracker()
          val mt = if (profile) {
            new ProfilingParallelTrackingQueryMemoryTracker(delegateFactory)
          } else {
            new ParallelTrackingQueryMemoryTracker(delegateFactory)
          }
          // mainThreadMemoryTracker should be closed together with the query context
          queryContext.resources.trace(DefaultCloseListenable.wrap(mainThreadMemoryTracker))
          mt.setInitializationMemoryTracker(mainThreadMemoryTracker)
          mt
        case (MEMORY_TRACKING, _)                   => new TrackingQueryMemoryTracker
        case (CUSTOM_MEMORY_TRACKING(decorator), _) => new CustomTrackingQueryMemoryTracker(decorator)
      }
    }

    protected def createQueryState(
      params: MapValue,
      prePopulateResults: Boolean,
      input: InputDataStream,
      subscriber: QuerySubscriber,
      doProfile: Boolean,
      profileInformation: RuntimeProfileInformation
    ): QueryState

    def queryContext: QueryContext

    def addProfileDecorator(profileDecorator: PipeDecorator): Unit = pipeDecorator = profileDecorator

    override def build(
      params: MapValue,
      queryProfile: RuntimeProfileInformation,
      prePopulateResults: Boolean,
      input: InputDataStream,
      subscriber: QuerySubscriber,
      doProfile: Boolean
    ): RuntimeResult = {
      val state = createQueryState(params, prePopulateResults, input, subscriber, doProfile, queryProfile)
      new PipeExecutionResult(
        pipe,
        columns.toArray,
        state,
        queryProfile,
        subscriber
      )
    }
  }

}
