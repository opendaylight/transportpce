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

package io.fd.honeycomb.northbound.netconf;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.fd.honeycomb.binding.init.ProviderTrait;
import org.opendaylight.controller.config.yang.netconf.mdsal.monitoring.MdsalMonitoringMapperFactory;
import org.opendaylight.controller.config.yang.netconf.mdsal.monitoring.MonitoringToMdsalWriter;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.netconf.api.monitoring.NetconfMonitoringService;
import org.opendaylight.netconf.mapping.api.NetconfOperationServiceFactory;
import org.opendaylight.netconf.mapping.api.NetconfOperationServiceFactoryListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NetconfMonitoringMapperProvider extends ProviderTrait<NetconfOperationServiceFactory> {

    private static final Logger LOG = LoggerFactory.getLogger(NetconfMonitoringMapperProvider.class);

    @Inject
    @Named(NetconfModule.HONEYCOMB_NETCONF)
    private DataBroker dataBroker;
    @Inject
    private NetconfOperationServiceFactoryListener aggregator;
    @Inject
    private NetconfMonitoringService monitoringService;

    @Override
    protected NetconfOperationServiceFactory create() {
        LOG.trace("Initializing MonitoringToMdsalWriter");
        final MonitoringToMdsalWriter writer = new MonitoringToMdsalWriter(monitoringService, dataBroker);
        writer.start();

        LOG.trace("Providing MdsalMonitoringMapperFactory");
        return new MdsalMonitoringMapperFactory(aggregator, monitoringService, writer);
    }
}
