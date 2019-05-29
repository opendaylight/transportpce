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

import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.opendaylight.controller.config.util.capability.Capability;
import org.opendaylight.controller.config.util.capability.YangModuleCapability;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.OrgOpenroadmDevice;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.OrgOpenroadmDeviceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netmod.notification.rev080714.Netconf;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netmod.notification.rev080714.netconf.Streams;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netmod.notification.rev080714.netconf.StreamsBuilder;
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
import org.opendaylight.yangtools.yang.parser.util.TextToASTTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fd.honeycomb.translate.read.ReaderFactory;
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder;
import io.fd.honeycomb.translate.util.read.BindingBrokerReader;
import io.fd.honeycomb.transportpce.device.configuration.DeviceConfiguration;
import io.fd.honeycomb.transportpce.device.configuration.NetconfConfiguration;

/**
 * @author Martial COULIBALY ( martial.coulibaly@gfi.com ) on behalf of Orange
 */
public final class DeviceReaderFactory implements ReaderFactory {

    private static final Logger LOG = LoggerFactory.getLogger(DeviceReaderFactory.class);
    public static final InstanceIdentifier<OrgOpenroadmDevice> DEVICE_CONTAINER_ID = InstanceIdentifier
            .create(OrgOpenroadmDevice.class);
    private static final String YANG_MODELS = "yang";

    @Inject
    @Named("device-databroker")
    private DataBroker dataBroker;

    @Inject
    private DeviceConfiguration deviceConfiguration;

    @Inject
    private NetconfConfiguration netconfConfiguration;

    @Override
    public void init(final ModifiableReaderRegistryBuilder registry) {
        registry.add(new BindingBrokerReader<>(DEVICE_CONTAINER_ID, dataBroker, LogicalDatastoreType.OPERATIONAL,
                OrgOpenroadmDeviceBuilder.class));
        if (writeXMLDataToOper()) {
            writeNetconfState();
            writeNetconfStream();
            loadConfigData();
        }
    }

    /**
     * Write xml data from {@link DeviceConfiguration} to operational data.
     *
     */
    public boolean writeXMLDataToOper() {
        Boolean res = false;
        LOG.info("writting xml file data to oper datastore");
        OrgOpenroadmDevice device = this.deviceConfiguration.getDataDevice();
        if (device != null) {
            String deviceId = device.getInfo().getNodeId();
            LOG.info("Getting device info from xml file for device '{}'", deviceId);
            OrgOpenroadmDeviceBuilder result = new OrgOpenroadmDeviceBuilder(device);
            InstanceIdentifier<OrgOpenroadmDevice> iid = InstanceIdentifier.create(OrgOpenroadmDevice.class);
            WriteTransaction writeTx = this.dataBroker.newWriteOnlyTransaction();
            if (writeTx != null) {
                LOG.info("WriteTransaction is ok, copy device info to oper datastore");
                writeTx.put(LogicalDatastoreType.OPERATIONAL, iid, result.build());
                Future<Void> future = writeTx.submit();
                try {
                    Futures.getChecked(future, ExecutionException.class);
                    LOG.info("device '{}' writed to oper datastore", deviceId);
                    res = true;
                } catch (ExecutionException e) {
                    LOG.error("Failed to write Element '{}' to oper datastore", deviceId);
                }
            } else {
                LOG.error("WriteTransaction object is null");
            }
        } else {
            LOG.error("device data operation gets from xml file is null !");
        }
        return res;
    }

    /**
     * Load data to config device datastore.
     *
     */
    public boolean loadConfigData() {
        Boolean res = false;
        LOG.info("loading device configuration info from xml file...");
        String xml = this.deviceConfiguration.getConfigDevice();
        LOG.info("device info gets from xml file !");
        if (xml != null) {
            OrgOpenroadmDevice result = this.deviceConfiguration.getDeviceFromXML(xml);
            if (result != null) {
                LOG.info("OrgOpenroadmDevice info gets : {}", result.getInfo().getNodeId());
                WriteTransaction writeTx = this.dataBroker.newWriteOnlyTransaction();
                if (writeTx != null) {
                    LOG.info("WriteTransaction is ok, copy device info to config datastore");
                    writeTx.put(LogicalDatastoreType.CONFIGURATION, DEVICE_CONTAINER_ID, result);
                    Future<Void> future = writeTx.submit();
                    try {
                        Futures.getChecked(future, ExecutionException.class);
                        LOG.info("device writed to config datastore");
                    } catch (ExecutionException e) {
                        LOG.error("Failed to write device to config datastore");
                    }
                } else {
                    LOG.error("WriteTransaction object is null");
                }
            } else {
                LOG.error("device gets from xml is null !!");
            }
        } else {
            LOG.error("device ID from input is not the same from xml file");
        }
        return res;
    }

    /**
     * write {@link Streams} data to operational device datastore.
     *
     * @return result {@link Boolean}
     */
    public boolean writeNetconfStream() {
        Boolean result = false;
        LOG.info("writting netconf stream to oper datastore");
        Streams streams = this.netconfConfiguration.getNetconfStreamsData();
        if (streams != null) {
            LOG.info("Netconf Data gets from xml file is present");
            InstanceIdentifier<Streams> iid = InstanceIdentifier.create(Netconf.class).child(Streams.class);
            Streams netconfStreams = new StreamsBuilder(streams).build();
            WriteTransaction writeTx = this.dataBroker.newWriteOnlyTransaction();
            if (writeTx != null) {
                LOG.info("WriteTransaction is ok");
                writeTx.put(LogicalDatastoreType.OPERATIONAL, iid, netconfStreams);
                Future<Void> future = writeTx.submit();
                try {
                    Futures.getChecked(future, ExecutionException.class);
                    LOG.info("netconf stream writed to oper datastore");
                    result = true;
                } catch (ExecutionException e) {
                    LOG.error("Failed to write netconf stream to oper datastore");
                }
            } else {
                LOG.error("WriteTransaction object is null");
            }
        } else {
            LOG.error("Netconf data gets from xml file is null !");
        }
        return result;
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
            InstanceIdentifier<NetconfState> iid = InstanceIdentifier.create(NetconfState.class);
            WriteTransaction writeTx = this.dataBroker.newWriteOnlyTransaction();
            if (writeTx != null) {
                LOG.info("WriteTransaction is ok, copy device info to oper datastore");
                writeTx.put(LogicalDatastoreType.OPERATIONAL, iid, netconfState);
                Future<Void> future = writeTx.submit();
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
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        // to convert Date to String, use format method of SimpleDateFormat class.
        String revision = dateFormat.format(module.getRevision());
        final SourceIdentifier moduleSourceIdentifier = RevisionSourceIdentifier.create(module.getName(), revision);
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
