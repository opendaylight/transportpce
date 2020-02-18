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

package io.fd.honeycomb.northbound.restconf;

import com.google.inject.Singleton;
import com.google.inject.name.Names;
import io.fd.honeycomb.northbound.NorthboundAbstractModule;
import io.fd.honeycomb.northbound.restconf.JettyServerStarter.RestconfJettyServer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.opendaylight.netconf.sal.rest.api.RestConnector;
import org.opendaylight.netconf.sal.rest.impl.RestconfApplication;
import org.opendaylight.netconf.sal.restconf.impl.BrokerFacade;
import org.opendaylight.netconf.sal.restconf.impl.ControllerContext;
import org.opendaylight.netconf.sal.restconf.impl.RestconfImpl;
import org.opendaylight.netconf.sal.restconf.impl.StatisticsRestconfServiceWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RestconfModule extends NorthboundAbstractModule<RestconfConfiguration> {

    private static final Logger LOG = LoggerFactory.getLogger(RestconfModule.class);

    public static final String HONEYCOMB_RESTCONF = "honeycomb-restconf";
    public static final String RESTCONF_HTTP = "restconf-http";
    public static final String RESTCONF_HTTPS = "restconf-https";

    public RestconfModule() {
        super(new RestconfConfigurationModule(), RestconfConfiguration.class);
    }

    @Override
    protected void configure() {
        if (!getConfiguration().isRestconfEnabled()) {
            LOG.info("Restconf is disabled, skipping configuration");
            return;
        }

        LOG.info("Starting RESTCONF Northbound");
        install(new RestconfConfigurationModule());
        bind(ControllerContext.class).toProvider(ControllerContextProvider.class).in(Singleton.class);
        bind(BrokerFacade.class).toProvider(BrokerFacadeProvider.class).in(Singleton.class);
        bind(RestconfImpl.class).toProvider(RestconfServiceProvider.class).in(Singleton.class);
        bind(StatisticsRestconfServiceWrapper.class)
                .toProvider(StatisticsRestconfServiceWrapperProvider.class).in(Singleton.class);
        bind(RestconfApplication.class).toProvider(RestconfApplicationProvider.class).in(Singleton.class);
        bind(Server.class).toProvider(JettyServerProvider.class).in(Singleton.class);
        bind(ServerConnector.class).annotatedWith(Names.named(RESTCONF_HTTP))
                .toProvider(HttpConnectorProvider.class)
                .in(Singleton.class);
        bind(ServerConnector.class).annotatedWith(Names.named(RESTCONF_HTTPS))
                .toProvider(HttpsConnectorProvider.class)
                .in(Singleton.class);
        bind(RestConnector.class).toProvider(RestconfProvider.class).in(Singleton.class);
        bind(RestconfJettyServer.class).toProvider(JettyServerStarter.class).asEagerSingleton();
    }
}
