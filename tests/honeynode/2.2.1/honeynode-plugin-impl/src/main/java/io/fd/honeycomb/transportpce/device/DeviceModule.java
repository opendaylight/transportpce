/*
 * Copyright (c) 2016 Cisco and/or its affiliates.
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
package io.fd.honeycomb.transportpce.device;

import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.store.inmemory.InMemoryDOMDataStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.PrivateModule;
import com.google.inject.Singleton;
import com.google.inject.name.Names;

import io.fd.honeycomb.infra.distro.data.DataStoreProvider;
import io.fd.honeycomb.infra.distro.data.InmemoryDOMDataBrokerProvider;
import io.fd.honeycomb.transportpce.device.configuration.DeviceConfigurationModule;
import io.fd.honeycomb.transportpce.device.configuration.NetconfConfigurationModule;
import io.fd.honeycomb.transportpce.device.configuration.OcPlatformConfigurationModule;
import io.fd.honeycomb.transportpce.device.configuration.OcTerminalDeviceConfigurationModule;
import io.fd.honeycomb.transportpce.device.configuration.PmConfigurationModule;

/**
 * Module class instantiating device-plugin plugin components.
 */

public final class DeviceModule extends PrivateModule {

    private static final Logger LOG = LoggerFactory.getLogger(DeviceModule.class);
    public static final String DEVICE_DATABROKER = "device-databroker";
    public static final String DEVICE_DATABROKER_NONPERSIST = "device-databroker-nopersist";

    @Override
    protected void configure() {
        LOG.info("Initializing Honeynode Modules");
        // Create inmemory config data store for DEVICE
        bind(InMemoryDOMDataStore.class).annotatedWith(Names.named(InmemoryDOMDataBrokerProvider.CONFIG))
            .toProvider(new DataStoreProvider(InmemoryDOMDataBrokerProvider.CONFIG, org.opendaylight.mdsal.common.api.LogicalDatastoreType.CONFIGURATION))
            .in(Singleton.class);
        // Create inmemory operational data store for DEVICE
        bind(InMemoryDOMDataStore.class).annotatedWith(Names.named(InmemoryDOMDataBrokerProvider.OPERATIONAL))
            .toProvider(new DataStoreProvider(InmemoryDOMDataBrokerProvider.OPERATIONAL, org.opendaylight.mdsal.common.api.LogicalDatastoreType.OPERATIONAL))
            .in(Singleton.class);

        // Wrap datastores as DOMDataBroker
        // TODO make executor service configurable
        bind(DOMDataBroker.class).annotatedWith(Names.named(DEVICE_DATABROKER))
            .toProvider(InmemoryDOMDataBrokerProvider.class).in(Singleton.class);
        expose(DOMDataBroker.class).annotatedWith(Names.named(DEVICE_DATABROKER));

        // Wrap DOMDataBroker as BA data broker
        bind(DataBroker.class).annotatedWith(Names.named(DEVICE_DATABROKER)).toProvider(DeviceBindingDataBrokerProvider.class)
            .in(Singleton.class);
        expose(DataBroker.class).annotatedWith(Names.named(DEVICE_DATABROKER));

        //install device configuration module
        install(new DeviceConfigurationModule());

        //install pm configuration module
        install(new PmConfigurationModule());
        LOG.info("Device Module initialized !");

        // install oc-platform configuration module
        install(new OcPlatformConfigurationModule());
        LOG.info("oc-platform-configuration Module initialized !");

        // install oc-terminal-device configuration module
        install(new OcTerminalDeviceConfigurationModule());
        LOG.info("oc-terminal-device Module initialized !");

        //install netconf configuration module
        install(new NetconfConfigurationModule());
        LOG.info("Netconf Module initialized !");
    }

}

