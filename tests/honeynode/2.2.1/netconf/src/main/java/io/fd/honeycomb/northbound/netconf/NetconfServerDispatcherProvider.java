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
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.Timer;
import java.util.concurrent.TimeUnit;
import org.opendaylight.netconf.api.NetconfServerDispatcher;
import org.opendaylight.netconf.api.monitoring.NetconfMonitoringService;
import org.opendaylight.netconf.impl.NetconfServerDispatcherImpl;
import org.opendaylight.netconf.impl.SessionIdProvider;
import org.opendaylight.netconf.impl.osgi.AggregatedNetconfOperationServiceFactory;
import org.opendaylight.netconf.mapping.api.NetconfOperationServiceFactory;

public final class NetconfServerDispatcherProvider extends ProviderTrait<NetconfServerDispatcher> {
    private static final long CONNECTION_TIMEOUT_MILLIS = TimeUnit.SECONDS.toMillis(20);

    @Inject
    @Named(NetconfModule.HONEYCOMB_NETCONF_MAPPER_AGGR)
    private NetconfOperationServiceFactory aggregator;
    @Inject
    private NetconfMonitoringService monitoringService;
    @Inject
    private Timer timer;
    @Inject
    private NioEventLoopGroup nettyThreadgroup;

    @Override
    protected NetconfServerDispatcherImpl create() {
        AggregatedNetconfOperationServiceFactory netconfOperationProvider =
                new AggregatedNetconfOperationServiceFactory();
        netconfOperationProvider.onAddNetconfOperationServiceFactory(aggregator);

        NetconfServerSessionNegotiatorFactory serverNegotiatorFactory =
                new NetconfServerSessionNegotiatorFactory(timer, netconfOperationProvider, new SessionIdProvider(),
                        CONNECTION_TIMEOUT_MILLIS, monitoringService);
        NetconfServerDispatcherImpl.ServerChannelInitializer serverChannelInitializer =
                new NetconfServerDispatcherImpl.ServerChannelInitializer(serverNegotiatorFactory);

        return new NetconfServerDispatcherImpl(serverChannelInitializer, nettyThreadgroup, nettyThreadgroup);
    }

    private static final class NetconfServerSessionNegotiatorFactory extends
            org.opendaylight.netconf.impl.NetconfServerSessionNegotiatorFactory {

        NetconfServerSessionNegotiatorFactory(final Timer timer,
                                                     final AggregatedNetconfOperationServiceFactory netconfOperationProvider,
                                                     final SessionIdProvider sessionIdProvider,
                                                     final long connectionTimeoutMillis,
                                                     final NetconfMonitoringService monitoringService) {
            super(timer, netconfOperationProvider, sessionIdProvider, connectionTimeoutMillis, monitoringService,
                    org.opendaylight.netconf.impl.NetconfServerSessionNegotiatorFactory.DEFAULT_BASE_CAPABILITIES);
        }
    }
}
