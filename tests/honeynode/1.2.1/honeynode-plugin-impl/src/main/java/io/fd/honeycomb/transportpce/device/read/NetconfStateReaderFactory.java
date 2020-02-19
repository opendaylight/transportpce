/*
 * Copyright (c) 2018 Orange and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fd.honeycomb.transportpce.device.read;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.netconf.api.capability.Capability;
import org.opendaylight.netconf.api.capability.YangModuleCapability;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.NetconfState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.NetconfStateBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.Yang;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.Schemas;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.SchemasBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.schemas.Schema;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.schemas.Schema.Location;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.schemas.Schema.Location.Enumeration;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.schemas.SchemaBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.repo.api.RevisionSourceIdentifier;
import org.opendaylight.yangtools.yang.model.repo.api.SchemaSourceFilter;
import org.opendaylight.yangtools.yang.model.repo.api.SchemaSourceRepresentation;
import org.opendaylight.yangtools.yang.model.repo.api.SourceIdentifier;
import org.opendaylight.yangtools.yang.model.repo.api.YangTextSchemaSource;
import org.opendaylight.yangtools.yang.model.repo.spi.PotentialSchemaSource;
import org.opendaylight.yangtools.yang.model.repo.spi.SchemaSourceListener;
import org.opendaylight.yangtools.yang.model.repo.util.FilesystemSchemaSourceCache;
import org.opendaylight.yangtools.yang.parser.repo.SharedSchemaRepository;
import org.opendaylight.yangtools.yang.parser.rfc7950.repo.TextToASTTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.fd.honeycomb.translate.read.ReaderFactory;
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder;

/**
 * @author Martial Coulibaly ( martial.coulibaly@gfi.com ) on behalf of Orange
 *
 */
public class NetconfStateReaderFactory implements ReaderFactory {
    private static final Logger LOG = LoggerFactory.getLogger(NetconfStateReaderFactory.class);
    public static final InstanceIdentifier<NetconfState> NETCONF_STATE_ID = InstanceIdentifier
            .create(NetconfState.class);
    private static final String YANG_MODELS = "yang";

    @Inject
    @Named("device-databroker")
    private DataBroker dataBroker;

    @Override
    public void init(ModifiableReaderRegistryBuilder registry) {
        writeNetconfState();
    }

    /**
     * Write {@link NetconfState} data to operational device datastore.
     *
     * @return result {@link Boolean}
     */
    public boolean writeNetconfState() {
        Boolean res = false;
        LOG.info("writting netconf state to oper datastore");
        final SharedSchemaRepository schemaRepo = new SharedSchemaRepository("honeynode-simulator");
        final Set<Capability> capabilities = parseSchemasToModuleCapabilities(schemaRepo);
        final Set<Capability> transformedCapabilities = Sets.newHashSet(capabilities);
        DummyMonitoringService monitor = new DummyMonitoringService(transformedCapabilities);
        List<Schema> schemaList = new ArrayList<Schema>();
        List<Location> locationList = new ArrayList<Location>();
        Location location = new Location(Enumeration.NETCONF);
        locationList.add(location);
        Schema schematobuild = null;
        for (final Schema schema : monitor.getSchemas().getSchema()) {
            schematobuild = new SchemaBuilder().setIdentifier(schema.getIdentifier())
                    .setNamespace(schema.getNamespace()).setVersion(schema.getVersion()).setFormat(Yang.class)
                    .setLocation(locationList).build();
            schemaList.add(schematobuild);
        }
        Schemas schemas = new SchemasBuilder().setSchema(schemaList).build();
        NetconfState netconfState = new NetconfStateBuilder().setSchemas(schemas).build();
        if (netconfState != null) {
            WriteTransaction writeTx = this.dataBroker.newWriteOnlyTransaction();
            if (writeTx != null) {
                LOG.info("WriteTransaction is ok, copy device info to oper datastore");
                writeTx.put(LogicalDatastoreType.OPERATIONAL, NETCONF_STATE_ID, netconfState);
                 @NonNull
                 FluentFuture<? extends @NonNull CommitInfo> future = writeTx.commit();
                try {
                    Futures.getChecked(future, ExecutionException.class);
                    LOG.info("netconf state writed to oper datastore");
                    res = true;
                } catch (ExecutionException e) {
                    LOG.error("Failed to write netconf state to oper datastore");
                }
            } else {
                LOG.error("WriteTransaction object is null");
            }
        } else {
            LOG.error("device data operation gets from xml file is null !");
        }
        return res;
    }

    private Set<Capability> parseSchemasToModuleCapabilities(final SharedSchemaRepository consumer) {
        final Set<SourceIdentifier> loadedSources = Sets.newHashSet();
        consumer.registerSchemaSourceListener(TextToASTTransformer.create(consumer, consumer));
        consumer.registerSchemaSourceListener(new SchemaSourceListener() {
            @Override
            public void schemaSourceEncountered(final SchemaSourceRepresentation schemaSourceRepresentation) {
            }

            @Override
            public void schemaSourceRegistered(final Iterable<PotentialSchemaSource<?>> potentialSchemaSources) {
                for (final PotentialSchemaSource<?> potentialSchemaSource : potentialSchemaSources) {
                    loadedSources.add(potentialSchemaSource.getSourceIdentifier());
                }
            }

            @Override
            public void schemaSourceUnregistered(final PotentialSchemaSource<?> potentialSchemaSource) {
            }
        });
        LOG.info("Loading models from directory.");
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        File models = new File(classLoader.getResource(YANG_MODELS).getFile());
        if (models.exists() && models.isDirectory()) {
            LOG.info("folder '{}' exists !", models.getAbsolutePath());
            final FilesystemSchemaSourceCache<YangTextSchemaSource> cache = new FilesystemSchemaSourceCache<>(consumer,
                    YangTextSchemaSource.class, models);
            consumer.registerSchemaSourceListener(cache);
        } else {
            LOG.warn("folder '{}' not exists !", models.getAbsolutePath());
            LOG.info("Custom module loading skipped.");
        }
        SchemaContext schemaContext;
        try {
            // necessary for creating mdsal data stores and operations
            schemaContext = consumer.createSchemaContextFactory(SchemaSourceFilter.ALWAYS_ACCEPT)
                    .createSchemaContext(loadedSources).get();
        } catch (final InterruptedException | ExecutionException e) {
            throw new RuntimeException("Cannot parse schema context", e);
        }

        final Set<Capability> capabilities = Sets.newHashSet();

        for (final Module module : schemaContext.getModules()) {
            for (final Module subModule : module.getSubmodules()) {
                addModuleCapability(consumer, capabilities, subModule);
            }
            addModuleCapability(consumer, capabilities, module);
        }
        return capabilities;
    }

    private static void addModuleCapability(final SharedSchemaRepository consumer, final Set<Capability> capabilities,
            final Module module) {
        final SourceIdentifier moduleSourceIdentifier = RevisionSourceIdentifier.create(module.getName(),
                module.getRevision());
        try {
            final String moduleContent = new String(
                    consumer.getSchemaSource(moduleSourceIdentifier, YangTextSchemaSource.class).get().read());
            capabilities.add(new YangModuleCapability(module, moduleContent));
            // IOException would be thrown in creating SchemaContext already
        } catch (ExecutionException | InterruptedException | IOException e) {
            LOG.warn("Cannot retrieve schema source for module {} from schema repository",
                    moduleSourceIdentifier.toString(), e);
        }
    }

}
