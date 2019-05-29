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
import io.fd.honeycomb.translate.read.ReaderFactory;
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder;
import io.fd.honeycomb.translate.util.read.BindingBrokerReader;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.NetconfState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.NetconfStateBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;


public final class NetconfMonitoringReaderFactoryProvider extends ProviderTrait<ReaderFactory> {

    @Inject
    @Named(NetconfModule.HONEYCOMB_NETCONF)
    private DataBroker netconfDataBroker;

    @Override
    protected NetconfMonitoringReaderFactory create() {
        return new NetconfMonitoringReaderFactory(netconfDataBroker);
    }

    /**
     * {@link io.fd.honeycomb.translate.read.ReaderFactory} initiating reader into NETCONF's dedicated data store.
     * Making NETCONF operational data available over NETCONF/RESTCONF
     */
    private static final class NetconfMonitoringReaderFactory implements ReaderFactory {

        private final DataBroker netconfMonitoringBindingBrokerDependency;

        NetconfMonitoringReaderFactory(final DataBroker netconfMonitoringBindingBrokerDependency) {
            this.netconfMonitoringBindingBrokerDependency = netconfMonitoringBindingBrokerDependency;
        }

        @Override
        public void init(final ModifiableReaderRegistryBuilder registry) {
            registry.add(new BindingBrokerReader<>(InstanceIdentifier.create(NetconfState.class),
                    netconfMonitoringBindingBrokerDependency,
                    LogicalDatastoreType.OPERATIONAL, NetconfStateBuilder.class));
        }
    }
}
