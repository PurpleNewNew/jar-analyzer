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
package org.neo4j.internal.counts;

import org.neo4j.io.layout.DatabaseLayout;
import org.neo4j.io.pagecache.PageCache;
import org.neo4j.io.pagecache.context.CursorContext;
import org.neo4j.io.pagecache.context.CursorContextFactory;
import org.neo4j.kernel.impl.store.NeoStores;
import org.neo4j.logging.InternalLog;
import org.neo4j.logging.InternalLogProvider;
import org.neo4j.memory.MemoryTracker;

/**
 * Embedded-lite build disables batch-import based degree reconstruction.
 */
public class DegreesRebuildFromStore implements DegreesRebuilder {
    private final NeoStores neoStores;
    private final InternalLog log;

    public DegreesRebuildFromStore(
            PageCache pageCache,
            NeoStores neoStores,
            DatabaseLayout databaseLayout,
            CursorContextFactory contextFactory,
            InternalLogProvider logProvider) {
        this.neoStores = neoStores;
        this.log = logProvider.getLog(DegreesRebuildFromStore.class);
    }

    @Override
    public long lastCommittedTxId() {
        return neoStores.getMetaDataStore().getLastCommittedTransactionId();
    }

    @Override
    public void rebuild(DegreeUpdater updater, CursorContext cursorContext, MemoryTracker memoryTracker) {
        if (!neoStores.getRelationshipGroupStore().isEmpty()) {
            log.warn("Relationship degrees rebuild is disabled in embedded-lite source-pruned runtime");
        }
    }
}
