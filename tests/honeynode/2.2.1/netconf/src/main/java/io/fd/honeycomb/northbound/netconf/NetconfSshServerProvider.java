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

import com.google.common.net.InetAddresses;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import io.fd.honeycomb.binding.init.ProviderTrait;
import io.fd.honeycomb.infra.distro.InitializationException;
import io.fd.honeycomb.northbound.CredentialsConfiguration;
import io.fd.honeycomb.northbound.NetconfConfiguration;
import io.netty.channel.ChannelFuture;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.GlobalEventExecutor;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.apache.sshd.server.keyprovider.PEMGeneratorHostKeyProvider;
import org.opendaylight.netconf.api.NetconfServerDispatcher;
import org.opendaylight.netconf.auth.AuthProvider;
import org.opendaylight.netconf.ssh.SshProxyServer;
import org.opendaylight.netconf.ssh.SshProxyServerConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public final class NetconfSshServerProvider extends ProviderTrait<NetconfSshServerProvider.NetconfSshServer> {

    private static final Logger LOG = LoggerFactory.getLogger(NetconfSshServerProvider.class);

    // Use RSA for ssh server, see https://git.opendaylight.org/gerrit/#/c/60138/
    private static final String DEFAULT_PRIVATE_KEY_PATH = null; // disable private key serialization
    private static final String DEFAULT_ALGORITHM = "RSA";
    private static final int DEFAULT_KEY_SIZE = 4096;

    @Inject
    private NetconfServerDispatcher dispatcher;
    @Inject
    private NetconfConfiguration cfgAttributes;
    @Inject
    private NioEventLoopGroup nettyThreadgroup;
    @Inject
    private CredentialsConfiguration credentialsCfg;

    private ScheduledExecutorService pool =
            Executors.newScheduledThreadPool(1, new ThreadFactoryBuilder().setNameFormat("netconf-ssh-%d").build());

    @Override
    protected NetconfSshServer create() {
        if (!cfgAttributes.isNetconfSshEnabled()) {
            LOG.info("NETCONF SSH disabled, skipping initialization");
            return null;
        }
        LOG.info("Starting NETCONF SSH");

        // TODO(HONEYCOMB-414):  the logic below is very similar to
        // org.opendaylight.netconf.ssh.NetconfNorthboundSshServer (introduced in Carbon), so consider reusing it
        // (requires fixing hardcoded private key path).
        final InetAddress sshBindingAddress = InetAddresses.forString(cfgAttributes.netconfSshBindingAddress.get());
        final InetSocketAddress bindingAddress =
                new InetSocketAddress(sshBindingAddress, cfgAttributes.netconfSshBindingPort.get());

        LocalAddress localAddress = new LocalAddress(cfgAttributes.netconfSshBindingPort.toString());
        ChannelFuture localServer = dispatcher.createLocalServer(localAddress);

        final SshProxyServer sshProxyServer = new SshProxyServer(pool, nettyThreadgroup, GlobalEventExecutor.INSTANCE);

        final SshProxyServerConfigurationBuilder sshConfigBuilder = new SshProxyServerConfigurationBuilder();
        sshConfigBuilder.setBindingAddress(bindingAddress);
        sshConfigBuilder.setLocalAddress(localAddress);
        // Only simple authProvider checking ConfigAttributes, checking the config file
        sshConfigBuilder.setAuthenticator(new SimplelAuthProvider(credentialsCfg));
        sshConfigBuilder.setIdleTimeout(Integer.MAX_VALUE);
        sshConfigBuilder.setKeyPairProvider(new PEMGeneratorHostKeyProvider(DEFAULT_PRIVATE_KEY_PATH,
            DEFAULT_ALGORITHM, DEFAULT_KEY_SIZE));

        localServer.addListener(new SshServerBinder(sshProxyServer, sshConfigBuilder, bindingAddress));

        return new NetconfSshServer(localServer, sshProxyServer);
    }

    public static final class NetconfSshServer {
        private ChannelFuture localServer;
        private SshProxyServer sshProxyServer;

        NetconfSshServer(final ChannelFuture localServer,
                         final SshProxyServer sshProxyServer) {
            this.localServer = localServer;
            this.sshProxyServer = sshProxyServer;
        }

        public Object getLocalServer() {
            return localServer;
        }

        public Object getSshProxyServer() {
            return sshProxyServer;
        }
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

    private static final class SshServerBinder implements GenericFutureListener<ChannelFuture> {
        private final SshProxyServer sshProxyServer;
        private final SshProxyServerConfigurationBuilder sshConfigBuilder;
        private final InetSocketAddress bindingAddress;

        SshServerBinder(final SshProxyServer sshProxyServer,
                        final SshProxyServerConfigurationBuilder sshConfigBuilder,
                        final InetSocketAddress bindingAddress) {
            this.sshProxyServer = sshProxyServer;
            this.sshConfigBuilder = sshConfigBuilder;
            this.bindingAddress = bindingAddress;
        }

        @Override
        public void operationComplete(final ChannelFuture future) {
            if (future.isDone() && !future.isCancelled()) {
                try {
                    sshProxyServer.bind(sshConfigBuilder.createSshProxyServerConfiguration());
                    LOG.info("Netconf SSH endpoint started successfully at {}", bindingAddress);
                } catch (final IOException e) {
                    throw new InitializationException("Unable to start SSH netconf server", e);
                }

            } else {
                LOG.warn("Unable to start SSH netconf server at {}", bindingAddress, future.cause());
                throw new InitializationException("Unable to start SSH netconf server", future.cause());
            }

        }

    }
}
