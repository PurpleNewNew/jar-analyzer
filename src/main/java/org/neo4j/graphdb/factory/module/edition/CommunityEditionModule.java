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
package org.neo4j.graphdb.factory.module.edition;

import static org.neo4j.configuration.GraphDatabaseSettings.SYSTEM_DATABASE_NAME;
import static org.neo4j.configuration.GraphDatabaseSettings.initial_default_database;
import static org.neo4j.dbms.database.DatabaseContextProviderDelegate.delegate;
import static org.neo4j.dbms.systemgraph.TopologyGraphDbmsModel.DATABASE_DEFAULT_PROPERTY;
import static org.neo4j.dbms.systemgraph.TopologyGraphDbmsModel.DATABASE_LABEL;
import static org.neo4j.dbms.systemgraph.TopologyGraphDbmsModel.DATABASE_NAME_PROPERTY;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.neo4j.collection.Dependencies;
import org.neo4j.configuration.Config;
import org.neo4j.configuration.GraphDatabaseSettings;
import org.neo4j.dbms.CommunityDatabaseStateService;
import org.neo4j.dbms.CommunityKernelPanicListener;
import org.neo4j.dbms.DatabaseStateService;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.dbms.database.DatabaseContextProvider;
import org.neo4j.dbms.database.DatabaseLifecycles;
import org.neo4j.dbms.database.DatabaseOperationCounts;
import org.neo4j.dbms.database.DatabaseReferenceCacheClearingListener;
import org.neo4j.dbms.database.DatabaseRepository;
import org.neo4j.dbms.database.DatabaseStateMonitor;
import org.neo4j.dbms.database.DefaultDatabaseContextFactory;
import org.neo4j.dbms.database.DefaultDatabaseContextFactoryComponents;
import org.neo4j.dbms.database.DefaultDatabaseDetailsExtrasProvider;
import org.neo4j.dbms.database.DefaultSystemGraphComponent;
import org.neo4j.dbms.database.DefaultSystemGraphInitializer;
import org.neo4j.dbms.database.DefaultTopologyInfoService;
import org.neo4j.dbms.database.StandaloneDatabaseContext;
import org.neo4j.dbms.database.SystemGraphComponents;
import org.neo4j.dbms.database.SystemGraphInitializer;
import org.neo4j.dbms.database.TopologyInfoService;
import org.neo4j.dbms.database.readonly.ReadOnlyChangeListener;
import org.neo4j.dbms.database.readonly.ReadOnlyDatabases;
import org.neo4j.dbms.database.readonly.SystemGraphReadOnlyDatabaseLookupFactory;
import org.neo4j.dbms.database.readonly.SystemGraphReadOnlyListener;
import org.neo4j.dbms.identity.DefaultIdentityModule;
import org.neo4j.dbms.identity.ServerIdentity;
import org.neo4j.dbms.identity.ServerIdentityFactory;
import org.neo4j.dbms.systemgraph.CommunityTopologyGraphComponent;
import org.neo4j.dbms.systemgraph.SystemDatabaseProvider;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.event.TransactionData;
import org.neo4j.graphdb.event.TransactionEventListenerAdapter;
import org.neo4j.graphdb.factory.module.GlobalModule;
import org.neo4j.internal.kernel.api.security.CommunitySecurityLog;
import org.neo4j.io.device.DeviceMapper;
import org.neo4j.kernel.api.security.provider.NoAuthSecurityProvider;
import org.neo4j.kernel.api.security.provider.SecurityProvider;
import org.neo4j.kernel.database.DatabaseId;
import org.neo4j.kernel.database.DatabaseIdRepository;
import org.neo4j.kernel.database.DatabaseReferenceRepository;
import org.neo4j.kernel.database.DefaultDatabaseResolver;
import org.neo4j.kernel.database.MapCachingDatabaseIdRepository;
import org.neo4j.kernel.database.MapCachingDatabaseReferenceRepository;
import org.neo4j.kernel.database.SystemGraphDatabaseIdRepository;
import org.neo4j.kernel.database.SystemGraphDatabaseReferenceRepository;
import org.neo4j.kernel.impl.api.TransactionalProcessFactory;
import org.neo4j.kernel.impl.factory.DefaultTransactionalProcessFactory;
import org.neo4j.kernel.impl.pagecache.CommunityIOControllerService;
import org.neo4j.kernel.impl.security.URIAccessRules;
import org.neo4j.kernel.lifecycle.Lifecycle;
import org.neo4j.kernel.lifecycle.LifecycleAdapter;
import org.neo4j.procedure.impl.ProcedureConfig;

/**
 * This implementation of {@link AbstractEditionModule} creates the implementations of services
 * that are specific to the Community edition.
 */
public class CommunityEditionModule extends AbstractEditionModule implements DefaultDatabaseContextFactoryComponents {
    protected final GlobalModule globalModule;
    protected final ServerIdentity identityModule;
    private final MapCachingDatabaseReferenceRepository databaseReferenceRepo;
    private final DeviceMapper deviceMapper;

    protected DatabaseStateService databaseStateService;
    private Lifecycle defaultDatabaseInitializer = new LifecycleAdapter();
    private SystemGraphComponents systemGraphComponents;

    public CommunityEditionModule(GlobalModule globalModule) {
        Dependencies globalDependencies = globalModule.getGlobalDependencies();
        Config globalConfig = globalModule.getGlobalConfig();
        this.globalModule = globalModule;

        globalDependencies.satisfyDependency(new DatabaseOperationCounts.Counter()); // for global metrics
        globalDependencies.satisfyDependency(new DatabaseStateMonitor.Counter()); // for global metrics

        var securityLog = new CommunitySecurityLog(
                globalModule.getLogService().getInternalLogProvider().getLog(getClass()));
        globalDependencies.satisfyDependency(new URIAccessRules(securityLog, globalConfig));

        identityModule = tryResolveOrCreate(
                        ServerIdentityFactory.class,
                        globalModule.getExternalDependencyResolver(),
                        DefaultIdentityModule::fromGlobalModule)
                .create(globalModule);
        globalDependencies.satisfyDependency(identityModule);

        deviceMapper = DeviceMapper.UNKNOWN_MAPPER;
        globalDependencies.satisfyDependency(deviceMapper);

        connectionTracker = globalDependencies.satisfyDependency(createConnectionTracker());
        databaseReferenceRepo = globalDependencies.satisfyDependency(new MapCachingDatabaseReferenceRepository());
    }

    @Override
    public DatabaseContextProvider<?> createDatabaseContextProvider(GlobalModule globalModule) {
        var databaseContextFactory = new DefaultDatabaseContextFactory(
                globalModule,
                identityModule,
                getTransactionMonitorFactory(),
                getIndexMonitorFactory(),
                createIdContextFactory(globalModule),
                deviceMapper,
                new CommunityIOControllerService(),
                createCommitProcessFactory(),
                this);

        var databaseIdRepo = new MapCachingDatabaseIdRepository();
        var databaseRepository = new DatabaseRepository<StandaloneDatabaseContext>(databaseIdRepo);
        var rootDatabaseIdRepository = AbstractEditionModule.tryResolveOrCreate(
                DatabaseIdRepository.class,
                globalModule.getExternalDependencyResolver(),
                () -> new SystemGraphDatabaseIdRepository(
                        () -> databaseRepository.getDatabaseContext(DatabaseId.SYSTEM_DATABASE_ID),
                        globalModule.getLogService().getInternalLogProvider()));
        var rootDatabaseReferenceRepository = AbstractEditionModule.tryResolveOrCreate(
                DatabaseReferenceRepository.class,
                globalModule.getExternalDependencyResolver(),
                () -> new SystemGraphDatabaseReferenceRepository(databaseRepository::getSystemDatabaseContext));
        databaseIdRepo.setDelegate(rootDatabaseIdRepository);
        databaseReferenceRepo.setDelegate(rootDatabaseReferenceRepository);
        var databaseIdCacheCleaner = new DatabaseReferenceCacheClearingListener(databaseIdRepo, databaseReferenceRepo);

        var kernelPanicListener =
                new CommunityKernelPanicListener(globalModule.getDatabaseEventListeners(), databaseRepository);
        globalModule.getGlobalLife().add(kernelPanicListener);

        var databaseLifecycles = new DatabaseLifecycles(
                databaseRepository,
                globalModule.getGlobalConfig().get(initial_default_database),
                databaseContextFactory,
                globalModule.getLogService().getInternalLogProvider());
        databaseStateService = new CommunityDatabaseStateService(databaseRepository);

        globalModule.getGlobalLife().add(databaseLifecycles.systemDatabaseStarter());
        globalModule.getGlobalLife().add(databaseLifecycles.allDatabaseShutdown());
        globalModule.getGlobalDependencies().satisfyDependency(delegate(databaseRepository));
        globalModule.getGlobalDependencies().satisfyDependency(databaseStateService);

        globalModule
                .getTransactionEventListeners()
                .registerTransactionEventListener(SYSTEM_DATABASE_NAME, databaseIdCacheCleaner);

        defaultDatabaseInitializer = databaseLifecycles.defaultDatabaseStarter();

        globalModule
                .getGlobalDependencies()
                .satisfyDependency(SystemGraphComponents.UpgradeChecker.UPGRADE_ALWAYS_ALLOWED);

        return databaseRepository;
    }

    @Override
    public TopologyInfoService createTopologyInfoService(DatabaseContextProvider<?> databaseContextProvider) {
        var detailsExtrasProvider = new DefaultDatabaseDetailsExtrasProvider(databaseContextProvider);
        return new DefaultTopologyInfoService(
                identityModule.serverId(),
                globalModule.getGlobalConfig(),
                databaseStateService,
                globalReadOnlyChecker,
                detailsExtrasProvider);
    }

    @Override
    public ProcedureConfig getProcedureConfig(Config config) {
        return new ProcedureConfig(config, false);
    }

    @Override
    public void registerDatabaseInitializers(GlobalModule globalModule, SystemDatabaseProvider systemDatabaseProvider) {
        registerSystemGraphInitializer(globalModule, systemDatabaseProvider);
        registerDefaultDatabaseInitializer(globalModule);
    }

    private void registerSystemGraphInitializer(
            GlobalModule globalModule, SystemDatabaseProvider systemDatabaseProvider) {
        var initializer = AbstractEditionModule.tryResolveOrCreate(
                SystemGraphInitializer.class,
                globalModule.getExternalDependencyResolver(),
                () -> new DefaultSystemGraphInitializer(systemDatabaseProvider::database, systemGraphComponents));
        globalModule.getGlobalDependencies().satisfyDependency(initializer);
        globalModule.getGlobalLife().add(initializer);
    }

    protected void registerDefaultDatabaseInitializer(GlobalModule globalModule) {
        globalModule.getGlobalLife().add(defaultDatabaseInitializer);
    }

    @Override
    public void registerSystemGraphComponents(
            SystemGraphComponents.Builder systemGraphComponentsBuilder, GlobalModule globalModule) {
        var config = globalModule.getGlobalConfig();
        var log = globalModule.getLogService().getInternalLogProvider();
        var clock = globalModule.getGlobalClock();
        var systemGraphComponent = new DefaultSystemGraphComponent(config, clock);
        var communityTopologyGraphComponentComponent = new CommunityTopologyGraphComponent(config, log);
        systemGraphComponentsBuilder.register(systemGraphComponent);
        systemGraphComponentsBuilder.register(communityTopologyGraphComponentComponent);
        this.systemGraphComponents = systemGraphComponentsBuilder.build();
    }

    @Override
    public void createSecurityModule(GlobalModule globalModule) {
        setSecurityProvider(makeSecurityModule(globalModule));
    }

    @Override
    public DatabaseReferenceRepository getDatabaseReferenceRepo() {
        return databaseReferenceRepo;
    }

    private SecurityProvider makeSecurityModule(GlobalModule globalModule) {
        globalModule.getGlobalDependencies().satisfyDependency(CommunitySecurityLog.NULL_LOG);
        if (globalModule.getGlobalConfig().get(GraphDatabaseSettings.auth_enabled)) {
            globalModule
                    .getLogService()
                    .getInternalLog(getClass())
                    .warn("neo4lite embedded build forces no-auth mode; auth_enabled is ignored");
        }
        return NoAuthSecurityProvider.INSTANCE;
    }

    @Override
    public void createDefaultDatabaseResolver(SystemDatabaseProvider systemDatabaseProvider) {
        var defaultDatabaseResolver = new EmbeddedDefaultDatabaseResolver(
                () -> globalModule.getGlobalConfig().get(initial_default_database), systemDatabaseProvider);
        globalModule
                .getTransactionEventListeners()
                .registerTransactionEventListener(SYSTEM_DATABASE_NAME, defaultDatabaseResolver);
        this.defaultDatabaseResolver = defaultDatabaseResolver;
    }

    @Override
    public void createGlobalReadOnlyChecker(
            SystemDatabaseProvider systemDatabaseProvider,
            DatabaseIdRepository databaseIdRepository,
            GlobalModule globalModule) {
        globalReadOnlyChecker = createGlobalReadOnlyChecker(
                Set.of(SystemGraphReadOnlyDatabaseLookupFactory.DEFAULT_PROVIDER),
                systemDatabaseProvider,
                databaseIdRepository,
                ReadOnlyChangeListener.NO_OP,
                globalModule);
        globalModule
                .getGlobalLife()
                .add(new SystemGraphReadOnlyListener(
                        globalModule.getTransactionEventListeners(), globalReadOnlyChecker));
        globalModule.getGlobalDependencies().satisfyDependency(globalReadOnlyChecker);
    }

    protected TransactionalProcessFactory createCommitProcessFactory() {
        return new DefaultTransactionalProcessFactory();
    }

    @Override
    public void bootstrapQueryRouterServices(DatabaseManagementService databaseManagementService) {
        // neo4lite: router/fabric path removed for embedded-only runtime.
    }

    @Override
    public ReadOnlyDatabases readOnlyDatabases() {
        return globalReadOnlyChecker;
    }

    @Override
    public SystemGraphComponents getSystemGraphComponents() {
        return systemGraphComponents;
    }

    private static final class EmbeddedDefaultDatabaseResolver extends TransactionEventListenerAdapter<Object>
            implements DefaultDatabaseResolver {
        private final AtomicReference<String> cachedDefaultDatabase = new AtomicReference<>();
        private final Supplier<String> defaultDbSupplier;
        private final SystemDatabaseProvider systemDbProvider;

        private EmbeddedDefaultDatabaseResolver(
                Supplier<String> defaultDbSupplier, SystemDatabaseProvider systemDbProvider) {
            this.defaultDbSupplier = defaultDbSupplier;
            this.systemDbProvider = systemDbProvider;
        }

        @Override
        public String defaultDatabase(String username) {
            var defaultDatabase = cachedDefaultDatabase.get();
            if (defaultDatabase == null) {
                defaultDatabase = systemDbProvider
                        .query(EmbeddedDefaultDatabaseResolver::resolveDefaultDatabase)
                        .orElseGet(defaultDbSupplier);
                cachedDefaultDatabase.set(defaultDatabase);
            }
            return defaultDatabase;
        }

        @Override
        public void clearCache() {
            cachedDefaultDatabase.set(null);
        }

        @Override
        public void afterCommit(TransactionData data, Object state, GraphDatabaseService databaseService) {
            clearCache();
        }

        private static Optional<String> resolveDefaultDatabase(Transaction tx) {
            return Optional.ofNullable(tx.findNode(DATABASE_LABEL, DATABASE_DEFAULT_PROPERTY, true))
                    .flatMap(defaultDatabaseNode -> Optional.ofNullable(
                            (String) defaultDatabaseNode.getProperty(DATABASE_NAME_PROPERTY, null)));
        }
    }
}
