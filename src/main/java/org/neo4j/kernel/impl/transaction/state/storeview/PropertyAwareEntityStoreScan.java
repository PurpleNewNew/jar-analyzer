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
package org.neo4j.kernel.impl.transaction.state.storeview;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.LongFunction;
import org.neo4j.collection.PrimitiveLongResourceCollections.AbstractPrimitiveLongBaseResourceIterator;
import org.neo4j.configuration.Config;
import org.neo4j.internal.kernel.api.PopulationProgress;
import org.neo4j.io.pagecache.context.CursorContext;
import org.neo4j.io.pagecache.context.CursorContextFactory;
import org.neo4j.kernel.impl.api.index.PhaseTracker;
import org.neo4j.kernel.impl.api.index.PropertyScanConsumer;
import org.neo4j.kernel.impl.api.index.StoreScan;
import org.neo4j.kernel.impl.api.index.TokenScanConsumer;
import org.neo4j.lock.Lock;
import org.neo4j.memory.MemoryTracker;
import org.neo4j.scheduler.JobScheduler;
import org.neo4j.storageengine.api.PropertySelection;
import org.neo4j.storageengine.api.StorageEntityScanCursor;
import org.neo4j.storageengine.api.StoragePropertyCursor;
import org.neo4j.storageengine.api.StorageReader;
import org.neo4j.storageengine.api.cursor.StoreCursors;
import org.neo4j.values.storable.Value;

/**
 * Scan store with the view given by iterator created by {@link #getEntityIdIterator(CursorContext, StoreCursors)}. This might be a full scan of the store
 * or a partial scan backed by the node label index.
 *
 * @param <CURSOR> the type of cursor used to read the records.
 */
public abstract class PropertyAwareEntityStoreScan<CURSOR extends StorageEntityScanCursor<?>> implements StoreScan {
    protected final StorageReader storageReader;
    private final Function<CursorContext, StoreCursors> storeCursorsFactory;
    protected final EntityScanCursorBehaviour<CURSOR> cursorBehaviour;
    private final AtomicBoolean continueScanning = new AtomicBoolean();
    private final long totalCount;
    private final MemoryTracker memoryTracker;
    private final boolean canDetermineExternalUpdatesCutOffPoint;
    protected final int[] entityTokenIdFilter;
    private final PropertySelection propertySelection;
    private final LongFunction<Lock> lockFunction;
    private PhaseTracker phaseTracker;
    protected final TokenScanConsumer tokenScanConsumer;
    protected final PropertyScanConsumer propertyScanConsumer;
    private volatile long completedEntities;
    private volatile boolean closed;

    protected PropertyAwareEntityStoreScan(
            Config config,
            StorageReader storageReader,
            Function<CursorContext, StoreCursors> storeCursorsFactory,
            long totalEntityCount,
            int[] entityTokenIdFilter,
            PropertySelection propertySelection,
            PropertyScanConsumer propertyScanConsumer,
            TokenScanConsumer tokenScanConsumer,
            LongFunction<Lock> lockFunction,
            EntityScanCursorBehaviour<CURSOR> cursorBehaviour,
            boolean parallelWrite,
            JobScheduler scheduler,
            CursorContextFactory contextFactory,
            MemoryTracker memoryTracker,
            boolean canDetermineExternalUpdatesCutOffPoint) {
        this.storageReader = storageReader;
        this.storeCursorsFactory = storeCursorsFactory;
        this.cursorBehaviour = cursorBehaviour;
        this.entityTokenIdFilter = entityTokenIdFilter;
        this.propertySelection = propertySelection;
        this.lockFunction = lockFunction;
        this.totalCount = totalEntityCount;
        this.memoryTracker = memoryTracker;
        this.canDetermineExternalUpdatesCutOffPoint = canDetermineExternalUpdatesCutOffPoint;
        this.phaseTracker = PhaseTracker.nullInstance;
        this.tokenScanConsumer = tokenScanConsumer;
        this.propertyScanConsumer = propertyScanConsumer;
        this.contextFactory = contextFactory;
    }

    protected final CursorContextFactory contextFactory;

    @Override
    public void run(ExternalUpdatesCheck externalUpdatesCheck) {
        continueScanning.set(true);
        completedEntities = 0;

        PropertyScanConsumer.Batch propertyBatch = propertyScanConsumer != null ? propertyScanConsumer.newBatch() : null;
        TokenScanConsumer.Batch tokenBatch = tokenScanConsumer != null ? tokenScanConsumer.newBatch() : null;
        long lastEntityId = -1;

        long scanStart = System.nanoTime();
        try (CursorContext cursorContext = contextFactory.create("indexPopulationScan");
                StoreCursors iteratorStoreCursors = storeCursorsFactory.apply(cursorContext);
                EntityIdIterator entityIdIterator = getEntityIdIterator(cursorContext, iteratorStoreCursors);
                StoreCursors readStoreCursors = storeCursorsFactory.apply(cursorContext);
                CURSOR entityCursor = cursorBehaviour.allocateEntityScanCursor(cursorContext, readStoreCursors, memoryTracker);
                StoragePropertyCursor propertyCursor =
                        storageReader.allocatePropertyCursor(cursorContext, readStoreCursors, memoryTracker)) {
            while (continueScanning.get() && entityIdIterator.hasNext()) {
                if (externalUpdatesCheck.needToApplyExternalUpdates()) {
                    externalUpdatesCheck.applyExternalUpdates(
                            canDetermineExternalUpdatesCutOffPoint && lastEntityId >= 0 ? lastEntityId : Long.MAX_VALUE);
                    entityIdIterator.invalidateCache();
                }

                long entityId = entityIdIterator.next();
                lastEntityId = entityId;

                try (Lock ignored = lockFunction.apply(entityId)) {
                    entityCursor.single(entityId);
                    while (entityCursor.next()) {
                        int[] tokens = propertyBatch != null
                                ? cursorBehaviour.readTokensAndProperties(entityCursor, propertyCursor, propertySelection)
                                : cursorBehaviour.readTokens(entityCursor);
                        if (tokens.length == 0) {
                            continue;
                        }

                        long entityRef = entityCursor.entityReference();
                        if (tokenBatch != null) {
                            tokenBatch.addRecord(entityRef, tokens);
                        }

                        if (propertyBatch != null && containsAnyEntityToken(entityTokenIdFilter, tokens)) {
                            Map<Integer, Value> relevantProperties = new HashMap<>();
                            while (propertyCursor.next()) {
                                relevantProperties.put(propertyCursor.propertyKey(), propertyCursor.propertyValue());
                            }
                            if (!relevantProperties.isEmpty()) {
                                propertyBatch.addRecord(entityRef, tokens, relevantProperties);
                            }
                        }
                    }
                }

                completedEntities++;
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to run index population store scan", e);
        }
        long scanNanos = System.nanoTime() - scanStart;

        long writeStart = System.nanoTime();
        if (propertyBatch != null) {
            propertyBatch.process();
        }
        if (tokenBatch != null) {
            tokenBatch.process();
        }
        long writeNanos = System.nanoTime() - writeStart;

        phaseTracker.registerTime(PhaseTracker.Phase.SCAN, TimeUnit.NANOSECONDS.toMillis(scanNanos));
        phaseTracker.registerTime(PhaseTracker.Phase.WRITE, TimeUnit.NANOSECONDS.toMillis(writeNanos));
    }

    @Override
    public void stop() {
        continueScanning.set(false);
    }

    @Override
    public void close() {
        storageReader.close();
        closed = true;
    }

    @Override
    public PopulationProgress getProgress() {
        if (closed) {
            return PopulationProgress.DONE;
        }
        if (totalCount > 0) {
            return PopulationProgress.single(completedEntities, totalCount);
        }
        return PopulationProgress.DONE;
    }

    @Override
    public void setPhaseTracker(PhaseTracker phaseTracker) {
        this.phaseTracker = phaseTracker;
    }

    public EntityIdIterator getEntityIdIterator(CursorContext cursorContext, StoreCursors storeCursors) {
        return new CursorEntityIdIterator<>(
                cursorBehaviour.allocateEntityScanCursor(cursorContext, storeCursors, memoryTracker));
    }

    private static boolean containsAnyEntityToken(int[] entityTokenFilter, int... entityTokens) {
        for (int candidate : entityTokens) {
            for (int token : entityTokenFilter) {
                if (token == candidate) {
                    return true;
                }
            }
        }
        return false;
    }

    static class CursorEntityIdIterator<CURSOR extends StorageEntityScanCursor<?>>
            extends AbstractPrimitiveLongBaseResourceIterator implements EntityIdIterator {
        private final CURSOR entityCursor;

        CursorEntityIdIterator(CURSOR entityCursor) {
            super(entityCursor::close);
            this.entityCursor = entityCursor;
            entityCursor.scan();
        }

        @Override
        public void invalidateCache() {
            // Nothing to invalidate, we're reading directly from the store
        }

        @Override
        protected boolean fetchNext() {
            return entityCursor.next() && next(entityCursor.entityReference());
        }
    }
}
