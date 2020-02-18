/*
 * Copyright (c) 2017 Cisco and/or its affiliates.
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
import io.fd.honeycomb.binding.init.ProviderTrait;
import io.fd.honeycomb.data.init.ShutdownHandler;
import io.fd.honeycomb.northbound.CredentialsConfiguration;
import io.fd.honeycomb.northbound.NetconfConfiguration;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.opendaylight.netconf.api.NetconfServerDispatcher;
import org.opendaylight.netconf.auth.AuthProvider;
import org.opendaylight.netconf.ssh.NetconfNorthboundSshServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NetconfSshServerProvider extends ProviderTrait<NetconfNorthboundSshServer> {

    private static final Logger LOG = LoggerFactory.getLogger(NetconfSshServerProvider.class);

    @Inject
    private NetconfServerDispatcher dispatcher;
    @Inject
    private NetconfConfiguration cfgAttributes;
    @Inject
    private NioEventLoopGroup nettyThreadgroup;
    @Inject
    private CredentialsConfiguration credentialsCfg;
    @Inject
    private ShutdownHandler shutdownHandler;

    @Override
    protected NetconfNorthboundSshServer create() {
        if (!cfgAttributes.isNetconfSshEnabled()) {
            LOG.info("NETCONF SSH disabled, skipping initialization");
            return null;
        }
        LOG.info("Starting NETCONF SSH");

        final NetconfNorthboundSshServer netconfServer = new NetconfNorthboundSshServer(dispatcher, nettyThreadgroup,
            GlobalEventExecutor.INSTANCE, cfgAttributes.netconfSshBindingAddress.get(),
            cfgAttributes.netconfSshBindingPort.get().toString(), new SimplelAuthProvider(credentialsCfg));
        shutdownHandler.register("netconf-northbound-ssh-server", netconfServer::close);
        return netconfServer;
    }

    private static final class SimplelAuthProvider implements AuthProvider {

        private final CredentialsConfiguration cfgAttributes;

        SimplelAuthProvider(final CredentialsConfiguration cfgAttributes) {
            this.cfgAttributes = cfgAttributes;
        }

        @Override
        public boolean authenticated(final String uname, final String passwd) {
            return cfgAttributes.username.equals(uname) && cfgAttributes.password.equals(passwd);
        }
    }
}
