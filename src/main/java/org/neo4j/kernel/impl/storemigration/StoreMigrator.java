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
package org.neo4j.kernel.impl.storemigration;

import java.io.IOException;
import java.util.function.Supplier;
import org.neo4j.configuration.Config;
import org.neo4j.io.fs.FileSystemAbstraction;
import org.neo4j.io.layout.DatabaseLayout;
import org.neo4j.io.pagecache.PageCache;
import org.neo4j.kernel.database.DatabaseTracers;
import org.neo4j.kernel.impl.api.index.IndexProviderMap;
import org.neo4j.kernel.impl.transaction.log.LogTailMetadata;
import org.neo4j.logging.internal.LogService;
import org.neo4j.memory.MemoryTracker;
import org.neo4j.scheduler.JobScheduler;
import org.neo4j.storageengine.api.StorageEngineFactory;

/**
 * Embedded-lite runtime does not support store migration tooling.
 */
public class StoreMigrator {
    private final FileSystemAbstraction fs;
    private final DatabaseLayout databaseLayout;
    private final StorageEngineFactory storageEngineFactory;

    public StoreMigrator(
            FileSystemAbstraction fs,
            Config config,
            LogService logService,
            PageCache pageCache,
            DatabaseTracers databaseTracers,
            JobScheduler jobScheduler,
            DatabaseLayout databaseLayout,
            StorageEngineFactory storageEngineFactory,
            StorageEngineFactory targetStorageEngineFactory,
            IndexProviderMap indexProviderMap,
            MemoryTracker memoryTracker,
            Supplier<LogTailMetadata> logTailSupplier) {
        this.fs = fs;
        this.databaseLayout = databaseLayout;
        this.storageEngineFactory = storageEngineFactory;
    }

    public void migrateIfNeeded(String formatToMigrateTo, boolean forceBtreeIndexesToRange, boolean keepNodeIds)
            throws UnableToMigrateException, IOException {
        if (!storageEngineFactory.storageExists(fs, databaseLayout)) {
            throw new UnableToMigrateException("Database '" + databaseLayout.getDatabaseName()
                    + "' either does not exists or it has not been initialised");
        }
    }

    public void upgradeIfNeeded() throws UnableToMigrateException, IOException {
        // Migration/upgrade workflow is intentionally disabled in embedded-lite mode.
    }
}
