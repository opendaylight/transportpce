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

package io.fd.honeycomb.northbound.restconf;

import static io.fd.honeycomb.northbound.restconf.RestconfModule.RESTCONF_HTTP;
import static io.fd.honeycomb.northbound.restconf.RestconfModule.RESTCONF_HTTPS;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.fd.honeycomb.binding.init.ProviderTrait;

import io.fd.honeycomb.data.init.ShutdownHandler;
import javax.annotation.Nullable;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.opendaylight.netconf.sal.rest.api.RestConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class JettyServerStarter extends ProviderTrait<JettyServerStarter.RestconfJettyServer> {

    private static final Logger LOG = LoggerFactory.getLogger(JettyServerStarter.class);

    @Inject
    private Server server;

    // injecting all connectors to make sure that server is started after they are added
    @Inject
    private RestConnector connector;

    // if HTTP is disabled, null will be injected
    @Nullable
    @Inject(optional = true)
    @Named(RESTCONF_HTTP)
    private ServerConnector httpConnectorInit;

    // if HTTPS is disabled, null will be injected
    @Nullable
    @Inject(optional = true)
    @Named(RESTCONF_HTTPS)
    private ServerConnector httpsConnectorInit;

    @Inject
    private ShutdownHandler shutdownHandler;

    @Override
    protected RestconfJettyServer create() {
        final RestconfJettyServer jettyServer = new RestconfJettyServer(server);
        shutdownHandler.register(RestconfJettyServer.class.getCanonicalName(), jettyServer);
        jettyServer.start();
        return jettyServer;
    }

    final class RestconfJettyServer implements AutoCloseable {
        private final Server server;

        private RestconfJettyServer(final Server server) {
            this.server = server;
        }

        private void start() {
            try {
                LOG.info("Starting RESTCONF Jetty server");
                server.start();
                LOG.info("RESTCONF Jetty server successfully started");
            } catch (Exception e) {
                LOG.error("Unable to start RESTCONF Jetty server", e);
                throw new IllegalStateException("Unable to start RESTCONF Jetty server", e);
            }
        }

        @Override
        public void close() throws Exception {
            LOG.info("Stopping RESTCONF Jetty server");
            server.stop();
            server.destroy();
            LOG.info("RESTCONF Jetty server successfully stopped");
        }
    }
}
