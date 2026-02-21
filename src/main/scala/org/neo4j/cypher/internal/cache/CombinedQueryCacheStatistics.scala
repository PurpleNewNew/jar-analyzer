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
package org.neo4j.cypher.internal.cache

class CombinedCacheTracer[T](a: CacheTracer[T], b: CacheTracer[T]) extends CacheTracer[T] {

  override def cacheHit(key: T, metaData: String): Unit = {
    a.cacheHit(key, metaData)
    b.cacheHit(key, metaData)
  }

  override def cacheMiss(key: T, metaData: String): Unit = {
    a.cacheMiss(key, metaData)
    b.cacheMiss(key, metaData)
  }

  override def compute(key: T, metaData: String): Unit = {
    a.compute(key, metaData)
    b.compute(key, metaData)
  }

  override def discard(key: T, metaData: String): Unit = {
    a.discard(key, metaData)
    b.discard(key, metaData)
  }

  override def computeWithExpressionCodeGen(key: T, metaData: String): Unit = {
    a.computeWithExpressionCodeGen(key, metaData)
    b.computeWithExpressionCodeGen(key, metaData)
  }

  override def cacheStale(key: T, secondsSinceCompute: Int, metaData: String, maybeReason: Option[String]): Unit = {
    a.cacheStale(key, secondsSinceCompute, metaData, maybeReason)
    b.cacheStale(key, secondsSinceCompute, metaData, maybeReason)
  }

  override def cacheFlush(sizeOfCacheBeforeFlush: Long): Unit = {
    a.cacheFlush(sizeOfCacheBeforeFlush)
    b.cacheFlush(sizeOfCacheBeforeFlush)
  }

  override def awaitOngoingComputation(key: T, metaData: String): Unit = {
    a.awaitOngoingComputation(key, metaData)
    b.awaitOngoingComputation(key, metaData)
  }
}
