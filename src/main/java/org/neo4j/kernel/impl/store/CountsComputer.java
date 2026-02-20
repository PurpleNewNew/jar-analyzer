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
package org.neo4j.kernel.impl.store;

import org.neo4j.counts.CountsUpdater;
import org.neo4j.internal.counts.CountsBuilder;
import org.neo4j.io.layout.DatabaseLayout;
import org.neo4j.io.pagecache.PageCache;
import org.neo4j.io.pagecache.context.CursorContext;
import org.neo4j.io.pagecache.context.CursorContextFactory;
import org.neo4j.logging.InternalLog;
import org.neo4j.memory.MemoryTracker;

/**
 * Embedded-lite build disables batch-import based counts reconstruction.
 */
public class CountsComputer implements CountsBuilder {
    private final NeoStores neoStores;
    private final InternalLog log;

    public CountsComputer(
            NeoStores stores,
            PageCache pageCache,
            CursorContextFactory contextFactory,
            DatabaseLayout databaseLayout,
            MemoryTracker memoryTracker,
            InternalLog log) {
        this(stores, stores.getMetaDataStore().getLastCommittedTransactionId(), log);
    }

    public CountsComputer(
            NeoStores stores,
            long lastCommittedTransactionId,
            PageCache pageCache,
            CursorContextFactory contextFactory,
            DatabaseLayout databaseLayout,
            MemoryTracker memoryTracker,
            InternalLog log) {
        this(stores, lastCommittedTransactionId, log);
    }

    private CountsComputer(NeoStores stores, long lastCommittedTransactionId, InternalLog log) {
        this.neoStores = stores;
        this.log = log;
        this.lastCommittedTransactionId = lastCommittedTransactionId;
    }

    private final long lastCommittedTransactionId;

    @Override
    public void initialize(CountsUpdater countsUpdater, CursorContext cursorContext, MemoryTracker memoryTracker) {
        if (neoStores.getNodeStore().getIdGenerator().getHighId() > 0
                || neoStores.getRelationshipStore().getIdGenerator().getHighId() > 0) {
            log.warn("Counts rebuild is disabled in embedded-lite source-pruned runtime");
        }
    }

    @Override
    public long lastCommittedTxId() {
        return lastCommittedTransactionId;
    }
}
