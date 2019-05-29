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

import com.google.common.net.InetAddresses;
import com.google.inject.Inject;
import io.fd.honeycomb.binding.init.ProviderTrait;
import io.fd.honeycomb.infra.distro.InitializationException;
import io.fd.honeycomb.northbound.NetconfConfiguration;
import io.netty.channel.ChannelFuture;
import io.netty.util.concurrent.GenericFutureListener;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import org.opendaylight.netconf.api.NetconfServerDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NetconfTcpServerProvider extends ProviderTrait<NetconfTcpServerProvider.NetconfTcpServer> {

    private static final Logger LOG = LoggerFactory.getLogger(NetconfTcpServerProvider.class);

    @Inject
    private NetconfServerDispatcher dispatcher;
    @Inject
    private NetconfConfiguration cfgAttributes;

    @Override
    protected NetconfTcpServer create() {
        if (!cfgAttributes.isNetconfTcpEnabled()) {
            LOG.debug("NETCONF TCP disabled, skipping initalization");
            return null;
        }
        LOG.info("Starting NETCONF TCP");
        final InetAddress name = InetAddresses.forString(cfgAttributes.netconfTcpBindingAddress.get());
        final InetSocketAddress unresolved = new InetSocketAddress(name, cfgAttributes.netconfTcpBindingPort.get());

        ChannelFuture tcpServer = dispatcher.createServer(unresolved);
        tcpServer.addListener(new TcpLoggingListener(unresolved));
        return new NetconfTcpServer(tcpServer);
    }

    public static final class NetconfTcpServer {
        private Object tcpServer;

        NetconfTcpServer(final ChannelFuture tcpServer) {
            this.tcpServer = tcpServer;
        }

        public Object getTcpServer() {
            return tcpServer;
        }
    }

    private static final class TcpLoggingListener implements GenericFutureListener<ChannelFuture> {
        private final InetSocketAddress unresolved;

        TcpLoggingListener(final InetSocketAddress unresolved) {
            this.unresolved = unresolved;
        }

        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
            if (future.isDone() && future.isSuccess()) {
                LOG.info("Netconf TCP endpoint started successfully at {}", unresolved);
            } else {
                LOG.warn("Unable to start TCP netconf server at {}", unresolved, future.cause());
                throw new InitializationException("Unable to start TCP netconf server", future.cause());
            }
        }
    }
}
