/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.controllers.tpce;

import io.lighty.controllers.tpce.module.TransportPCE;
import io.lighty.controllers.tpce.module.TransportPCEImpl;
import io.lighty.controllers.tpce.utils.TPCEUtils;
import io.lighty.core.controller.api.LightyController;
import io.lighty.core.controller.api.LightyModule;
import io.lighty.core.controller.impl.LightyControllerBuilder;
import io.lighty.core.controller.impl.config.ConfigurationException;
import io.lighty.core.controller.impl.config.ControllerConfiguration;
import io.lighty.core.controller.impl.util.ControllerConfigUtils;
import io.lighty.modules.northbound.restconf.community.impl.CommunityRestConf;
import io.lighty.modules.northbound.restconf.community.impl.CommunityRestConfBuilder;
import io.lighty.modules.northbound.restconf.community.impl.config.JsonRestConfServiceType;
import io.lighty.modules.northbound.restconf.community.impl.config.RestConfConfiguration;
import io.lighty.modules.northbound.restconf.community.impl.util.RestConfConfigUtils;
import io.lighty.modules.southbound.netconf.impl.NetconfSBPlugin;
import io.lighty.modules.southbound.netconf.impl.NetconfTopologyPluginBuilder;
import io.lighty.modules.southbound.netconf.impl.config.NetconfConfiguration;
import io.lighty.modules.southbound.netconf.impl.util.NetconfConfigUtils;
import io.lighty.server.LightyServerBuilder;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    private ShutdownHook shutdownHook;

    public void start() {
        start(new String[] {}, false);
    }

    public void start(String[] args, boolean registerShutdownHook) {
        long startTime = System.nanoTime();
        LOG.info(" ___________     ___________________ ___________");
        LOG.info(" \\__    ___/     \\______   \\_   ___ \\\\_   _____/");
        LOG.info("   |    |  ______ |     ___/    \\  \\/ |    __)_");
        LOG.info("   |    | /_____/ |    |   \\     \\____|        \\");
        LOG.info("   |____|         |____|    \\______  /_______  /");
        LOG.info("                                   \\/        \\/");
        LOG.info(".__  .__       .__     __              .__          ");
        LOG.info("|  | |__| ____ |  |___/  |_ ___.__.    |__| ____    ");
        LOG.info("|  | |  |/ ___\\|  |  \\   __<   |  |    |  |/  _ \\");
        LOG.info("|  |_|  / /_/  >   Y  \\  |  \\___  |    |  (  <_> )");
        LOG.info("|____/__\\___  /|___|  /__|  / ____| /\\ |__|\\____/");
        LOG.info("        /_____/     \\/      \\/      \\/           ");
        LOG.info("Starting lighty.io TransportPCE application ...");
        LOG.info("https://lighty.io/");
        LOG.info("https://github.com/PantheonTechnologies/lighty-core");
        try {
            LOG.info("using default configuration ...");
            //1. get controller configuration
            ControllerConfiguration defaultSingleNodeConfiguration =
                    ControllerConfigUtils.getDefaultSingleNodeConfiguration(TPCEUtils.yangModels);
            //2. get RESTCONF NBP configuration
            RestConfConfiguration restConfConfig =
                    RestConfConfigUtils.getDefaultRestConfConfiguration();
            restConfConfig.setJsonRestconfServiceType(JsonRestConfServiceType.DRAFT_02);
            restConfConfig.setHttpPort(8181);
            //3. NETCONF SBP configuration
            NetconfConfiguration netconfSBPConfig = NetconfConfigUtils.createDefaultNetconfConfiguration();
            startLighty(defaultSingleNodeConfiguration, restConfConfig, netconfSBPConfig, registerShutdownHook);
            float duration = (System.nanoTime() - startTime) / 1_000_000f;
            LOG.info("lighty.io and RESTCONF-NETCONF started in {}ms", duration);
        } catch (Exception e) {
            LOG.error("Main RESTCONF-NETCONF application exception: ", e);
        }
    }

    private void startLighty(ControllerConfiguration controllerConfiguration,
                             RestConfConfiguration restConfConfiguration,
                             NetconfConfiguration netconfSBPConfiguration, boolean registerShutdownHook)
            throws ConfigurationException, ExecutionException, InterruptedException {

        //1. initialize and start Lighty controller (MD-SAL, Controller, YangTools, Akka)
        LightyControllerBuilder lightyControllerBuilder = new LightyControllerBuilder();
        LightyController lightyController = lightyControllerBuilder.from(controllerConfiguration).build();
        lightyController.start().get();

        //2. start RestConf server
        CommunityRestConfBuilder communityRestConfBuilder = new CommunityRestConfBuilder();
        LightyServerBuilder jettyServerBuilder = new LightyServerBuilder(new InetSocketAddress(
                restConfConfiguration.getInetAddress(), restConfConfiguration.getHttpPort()));
        CommunityRestConf communityRestConf = communityRestConfBuilder.from(RestConfConfigUtils
                .getRestConfConfiguration(restConfConfiguration, lightyController.getServices()))
                .withLightyServer(jettyServerBuilder)
                .build();
        communityRestConf.start().get();
        communityRestConf.startServer();

        //3. start NetConf SBP
        NetconfSBPlugin netconfSouthboundPlugin;
        netconfSBPConfiguration = NetconfConfigUtils.injectServicesToTopologyConfig(
                netconfSBPConfiguration, lightyController.getServices());
        NetconfTopologyPluginBuilder netconfSBPBuilder = new NetconfTopologyPluginBuilder();
        netconfSouthboundPlugin = netconfSBPBuilder
                .from(netconfSBPConfiguration, lightyController.getServices())
                .build();
        netconfSouthboundPlugin.start().get();

        //4. start TransportPCE beans
        TransportPCE transportPCE = new TransportPCEImpl(lightyController.getServices());
        transportPCE.start().get();

        //5. Register shutdown hook for graceful shutdown.
        shutdownHook = new ShutdownHook(lightyController, communityRestConf, netconfSouthboundPlugin, transportPCE);
        if (registerShutdownHook) {
            Runtime.getRuntime().addShutdownHook(shutdownHook);
        }
    }

    public void shutdown() {
        shutdownHook.run();
    }

    public static void main(String[] args) {
        Main app = new Main();
        app.start(args, true);
    }

    private static class ShutdownHook extends Thread {

        private static final Logger LOG = LoggerFactory.getLogger(ShutdownHook.class);
        private final LightyController lightyController;
        private final CommunityRestConf communityRestConf;
        private final LightyModule netconfSouthboundPlugin;
        private final TransportPCE transportPCE;

        ShutdownHook(LightyController lightyController, CommunityRestConf communityRestConf,
                     LightyModule netconfSouthboundPlugin, TransportPCE transportPCE) {
            this.lightyController = lightyController;
            this.communityRestConf = communityRestConf;
            this.netconfSouthboundPlugin = netconfSouthboundPlugin;
            this.transportPCE = transportPCE;
        }

        @Override
        public void run() {
            LOG.info("lighty.io and RESTCONF-NETCONF shutting down ...");
            long startTime = System.nanoTime();
            try {
                transportPCE.shutdown().get();
            } catch (Exception e) {
                LOG.error("Exception while shutting down TransportPCE: ", e);
            }
            try {
                communityRestConf.shutdown().get();
            } catch (Exception e) {
                LOG.error("Exception while shutting down RESTCONF: ", e);
            }
            try {
                netconfSouthboundPlugin.shutdown().get();
            } catch (Exception e) {
                LOG.error("Exception while shutting down NETCONF: ", e);
            }
            try {
                lightyController.shutdown().get();
            } catch (Exception e) {
                LOG.error("Exception while shutting down lighty.io controller:", e);
            }
            float duration = (System.nanoTime() - startTime)/1_000_000f;
            LOG.info("lighty.io and RESTCONF-NETCONF stopped in {}ms", duration);
        }

    }

}
