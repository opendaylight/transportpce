/*
 * Copyright © 2026 1FINITY Inc.,  and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.debug.tools.netconf.proxy;

import java.io.File;

import org.opendaylight.transportpce.debug.tools.netconf.proxy.client.SharedDeviceClientManager;
import org.opendaylight.transportpce.debug.tools.netconf.proxy.config.ProxyConfig;
import org.opendaylight.transportpce.debug.tools.netconf.proxy.server.NetconfSshServer;
import org.opendaylight.transportpce.debug.tools.netconf.proxy.transform.RpcPatternMatcher;
import org.opendaylight.transportpce.debug.tools.netconf.proxy.transform.RpcTransformer;
import org.opendaylight.transportpce.debug.tools.netconf.proxy.transform.TransformerFactory;
import org.opendaylight.transportpce.debug.tools.netconf.proxy.util.XsltTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entry point for NETCONF Proxy Server.
 *
 * This proxy sits between NETCONF clients and a real NETCONF device,
 * forwarding RPCs and optionally transforming them.
 *
 * Usage:
 *   java -jar netconf-proxy.jar [config-file]
 */
public class NetconfProxyServer {

    private static final Logger LOG = LoggerFactory.getLogger(NetconfProxyServer.class);

    private final ProxyConfig config;
    private final RpcTransformer transformer;
    private final SharedDeviceClientManager deviceManager;
    private final RpcPatternMatcher patternMatcher;
    private final XsltTransformer xsltTransformer;
    private final NetconfSshServer sshServer;

    /**
     * Create a new NETCONF Proxy Server.
     *
     * @param config the proxy configuration
     * @throws Exception if initialization fails
     */
    public NetconfProxyServer(ProxyConfig config) throws Exception {
        this.config = config;
        this.transformer = TransformerFactory.createTransformer(config);
        this.deviceManager = new SharedDeviceClientManager(config);

        // Initialize pattern matcher and XSLT transformer
        this.patternMatcher = new RpcPatternMatcher(config.getRpcPatternsFile());
        this.xsltTransformer = new XsltTransformer(config.getXsltFilesPath());

        this.sshServer = new NetconfSshServer(config, transformer, deviceManager, patternMatcher, xsltTransformer);
    }

    /**
     * Start the proxy server.
     *
     * @throws Exception if server startup fails
     */
    public void start() throws Exception {
        LOG.info("=================================================");
        LOG.info("  NETCONF Proxy Server");
        LOG.info("=================================================");
        LOG.info("Configuration: {}", config);
        LOG.info("Transformer: {}", transformer.getClass().getSimpleName());
        LOG.info("=================================================");

        // Create log directory if needed
        File logDir = new File(config.getLogDir());
        if (!logDir.exists()) {
            logDir.mkdirs();
        }

        // Connect to device BEFORE starting SSH server
        LOG.info("Connecting to NETCONF device...");
        deviceManager.connect();
        LOG.info("Device connection established");

        // Start SSH server
        sshServer.start();

        LOG.info("NETCONF Proxy Server is running");
        LOG.info("Press Ctrl+C to stop");

        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOG.info("Shutdown signal received");
            try {
                stop();
            } catch (Exception e) {
                LOG.error("Error during shutdown", e);
            }
        }));
    }

    /**
     * Stop the proxy server.
     *
     * @throws Exception if server shutdown fails
     */
    public void stop() throws Exception {
        LOG.info("Stopping NETCONF Proxy Server");
        sshServer.stop();
        deviceManager.disconnect();
        LOG.info("NETCONF Proxy Server stopped");
    }

    /**
     * Main method.
     */
    public static void main(String[] args) {
        try {
            // Load configuration
            ProxyConfig config;
            if (args.length > 0) {
                LOG.info("Loading configuration from: {}", args[0]);
                System.setProperty("config.file", args[0]);
                config = new ProxyConfig();
            } else {
                LOG.info("Loading default configuration from classpath");
                config = new ProxyConfig();
            }

            // Create and start server
            NetconfProxyServer server = new NetconfProxyServer(config);
            server.start();

            // Keep main thread alive
            Thread.currentThread().join();

        } catch (Exception e) {
            LOG.error("Fatal error starting NETCONF Proxy Server", e);
            System.exit(1);
        }
    }
}
