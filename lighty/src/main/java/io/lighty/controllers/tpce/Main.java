/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.controllers.tpce;

import io.lighty.controllers.tpce.exception.TechnicalException;
import io.lighty.controllers.tpce.module.TransportPCE;
import io.lighty.controllers.tpce.module.TransportPCEImpl;
import io.lighty.controllers.tpce.utils.TPCEUtils;
import io.lighty.controllers.tpce.utils.TpceBanner;
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
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

    private static final String RESTCONF_OPTION_NAME = "restconf";
    private static final String NBINOTIFICATION_OPTION_NAME = "nbinotification";
    private static final String OLMTIMER1_OPTION_NAME = "olmtimer1";
    private static final String OLMTIMER2_OPTION_NAME = "olmtimer2";

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    private ShutdownHook shutdownHook;

    public void start() {
        start(null, false, null, null, false);
    }

    @SuppressWarnings("checkstyle:Illegalcatch")
    public void start(String restConfConfigurationFile, boolean activateNbiNotification,
                      String olmtimer1, String olmtimer2, boolean registerShutdownHook) {
        long startTime = System.nanoTime();
        TpceBanner.print();
        RestConfConfiguration restConfConfig = null;
        try {
            // 1. get controller configuration
            ControllerConfiguration singleNodeConfiguration = ControllerConfigUtils
                    .getDefaultSingleNodeConfiguration(TPCEUtils.getYangModels());
            // 2. get RESTCONF NBP configuration
            if (restConfConfigurationFile != null) {
                Path configPath = Paths.get(restConfConfigurationFile);
                LOG.info("Using restconf configuration from file {} ...", configPath);
                restConfConfig = RestConfConfigUtils.getRestConfConfiguration(Files.newInputStream(configPath));

            } else {
                LOG.info("Using default restconf configuration with http port 8181 ...");

                restConfConfig = RestConfConfigUtils.getDefaultRestConfConfiguration();
                restConfConfig.setHttpPort(8181);

            }
            restConfConfig.setJsonRestconfServiceType(JsonRestConfServiceType.DRAFT_02);
            // 3. NETCONF SBP configuration
            NetconfConfiguration netconfSBPConfig = NetconfConfigUtils.createDefaultNetconfConfiguration();
            startLighty(singleNodeConfiguration, restConfConfig, netconfSBPConfig, registerShutdownHook,
                    activateNbiNotification, olmtimer1, olmtimer2);
            float duration = (System.nanoTime() - startTime) / 1_000_000f;
            LOG.info("lighty.io and RESTCONF-NETCONF started in {}ms", duration);
        } catch (ConfigurationException | ExecutionException | IOException e) {
            LOG.error("An error occured while starting application: ", e);
            throw new TechnicalException("An error occured while starting application", e);
        } catch (InterruptedException e) {
            LOG.error("Application start interrupted : ", e);
            Thread.currentThread().interrupt();
            throw new TechnicalException("Application start interrupted", e);
          //CHECKSTYLE:OFF
        } catch (Exception e) {
          //CHECKSTYLE:ON
            LOG.error("Application start unmanaged exception : ", e);
            throw new TechnicalException("Application start unmanaged exception", e);

        }
    }

    /**
     * Build options for command line arguments.
     * @return
     */
    private static Options buildOptions() {
        Option restconfFileOption = Option.builder(RESTCONF_OPTION_NAME)
                .desc("Restconf configuration file")
                .argName(RESTCONF_OPTION_NAME)
                .hasArg(true)
                .required(false)
                .build();
        Option useNbiNotificationsOption = Option.builder(NBINOTIFICATION_OPTION_NAME)
                .desc("Activate NBI notifications feature")
                .argName(NBINOTIFICATION_OPTION_NAME)
                .hasArg(false)
                .required(false)
                .build();
        Option olmTimer1Option = Option.builder(OLMTIMER1_OPTION_NAME)
                .desc("OLM timer 1 value")
                .argName(OLMTIMER1_OPTION_NAME)
                .hasArg(true)
                .required(false)
                .build();
        Option olmTimer2Option = Option.builder(OLMTIMER2_OPTION_NAME)
                .desc("OLM timer 2 value")
                .argName(OLMTIMER1_OPTION_NAME)
                .hasArg(true)
                .required(false)
                .build();
        Options options = new Options();
        options.addOption(restconfFileOption);
        options.addOption(useNbiNotificationsOption);
        options.addOption(olmTimer1Option);
        options.addOption(olmTimer2Option);
        return options;
    }

    private void startLighty(ControllerConfiguration controllerConfiguration,
            RestConfConfiguration restConfConfiguration, NetconfConfiguration netconfSBPConfiguration,
            boolean registerShutdownHook, boolean activateNbiNotification, String olmtimer1, String olmtimer2)
                    throws ConfigurationException, ExecutionException, InterruptedException {

        // 1. initialize and start Lighty controller (MD-SAL, Controller, YangTools,
        // Akka)
        LightyControllerBuilder lightyControllerBuilder = new LightyControllerBuilder();
        LightyController lightyController = lightyControllerBuilder.from(controllerConfiguration).build();
        lightyController.start().get();

        // 2. start RestConf server
        LightyServerBuilder jettyServerBuilder = new LightyServerBuilder(
                new InetSocketAddress(restConfConfiguration.getInetAddress(), restConfConfiguration.getHttpPort()));
        CommunityRestConfBuilder communityRestConfBuilder = CommunityRestConfBuilder.from(
                RestConfConfigUtils.getRestConfConfiguration(restConfConfiguration, lightyController.getServices()));
        CommunityRestConf communityRestConf = communityRestConfBuilder.withLightyServer(jettyServerBuilder).build();
        communityRestConf.start().get();
        communityRestConf.startServer();

        // 3. start NetConf SBP
        NetconfSBPlugin netconfSouthboundPlugin;
        netconfSBPConfiguration = NetconfConfigUtils.injectServicesToTopologyConfig(netconfSBPConfiguration,
                lightyController.getServices());
        NetconfTopologyPluginBuilder netconfSBPBuilder = new NetconfTopologyPluginBuilder(
                lightyController.getServices(), netconfSBPConfiguration);
        netconfSouthboundPlugin = netconfSBPBuilder.from(netconfSBPConfiguration, lightyController.getServices())
                .build();
        netconfSouthboundPlugin.start().get();

        // 4. start TransportPCE beans
        TransportPCE transportPCE = new TransportPCEImpl(lightyController.getServices(), activateNbiNotification,
            olmtimer1, olmtimer2);
        transportPCE.start().get();

        // 5. Register shutdown hook for graceful shutdown.
        shutdownHook = new ShutdownHook(lightyController, communityRestConf, netconfSouthboundPlugin, transportPCE);
        if (registerShutdownHook) {
            Runtime.getRuntime().addShutdownHook(shutdownHook);
        }
    }

    public void shutdown() {
        shutdownHook.run();
    }

    public static void main(String[] args) {
        Options options = buildOptions();
        try {
            CommandLine commandLine = new DefaultParser().parse(options, args);
            String restConfConfigurationFile = commandLine.getOptionValue(RESTCONF_OPTION_NAME, null);
            boolean useNbiNotifications = commandLine.hasOption(NBINOTIFICATION_OPTION_NAME);
            String olmtimer1 = commandLine.getOptionValue(OLMTIMER1_OPTION_NAME, null);
            String olmtimer2 = commandLine.getOptionValue(OLMTIMER2_OPTION_NAME, null);
            Main app = new Main();
            app.start(restConfConfigurationFile, useNbiNotifications, olmtimer1, olmtimer2, true);
        } catch (ParseException e) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(
                    "java -ms<size> -mx<size> -XX:MaxMetaspaceSize=<size> -jar tpce.jar "
                    + "[-restconf <restconfConfigurationFile>] [-nbinotification]"
                    + " e.g. java -ms128m -mx512m -XX:MaxMetaspaceSize=128m -jar tpce.jar"
                    + "-restconf ../src/test/resources/config.json -nbinotification"
                    + "-olmtimer1 120000 -olmtimer2 20000",
                    options);
            System.exit(1);
        }
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
        @SuppressWarnings({"checkstyle:Illegalcatch", "checkstyle:VariableDeclarationUsageDistance"})
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
            float duration = (System.nanoTime() - startTime) / 1_000_000f;
            LOG.info("lighty.io and RESTCONF-NETCONF stopped in {}ms", duration);
        }

    }

}
