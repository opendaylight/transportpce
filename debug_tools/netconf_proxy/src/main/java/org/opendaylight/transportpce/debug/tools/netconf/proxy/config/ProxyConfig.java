/*
 * Copyright © 2026 1FINITY Inc.,  and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.debug.tools.netconf.proxy.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

/**
 * Configuration holder for NETCONF proxy settings.
 */
public class ProxyConfig {

    private final Config config;

    // Server (northbound) settings
    private final String serverHost;
    private final int serverPort;
    private final String serverUsername;
    private final String serverPassword;

    // Device (southbound) settings
    private final String deviceHost;
    private final int devicePort;
    private final String deviceUsername;
    private final String devicePassword;
    private final int connectionTimeoutMs;
    private final int keepAliveIntervalMs;

    // Transformation settings
    private final boolean transformationEnabled;
    private final String transformerClass;
    private final String rpcPatternsFile;
    private final String xsltFilesPath;

    // Logging settings
    private final boolean logMessages;
    private final String logDir;

    // Hello XML settings
    private final String helloXmlFile;

    public ProxyConfig() {
        this(ConfigFactory.load());
    }

    public ProxyConfig(Config config) {
        this.config = config;

        // Load server settings
        this.serverHost = config.getString("proxy.server.host");
        this.serverPort = config.getInt("proxy.server.port");
        this.serverUsername = config.getString("proxy.server.username");
        this.serverPassword = config.getString("proxy.server.password");

        // Load device settings
        this.deviceHost = config.getString("proxy.device.host");
        this.devicePort = config.getInt("proxy.device.port");
        this.deviceUsername = config.getString("proxy.device.username");
        this.devicePassword = config.getString("proxy.device.password");
        this.connectionTimeoutMs = config.getInt("proxy.device.connectionTimeoutMs");
        this.keepAliveIntervalMs = config.getInt("proxy.device.keepAliveIntervalMs");

        // Load transformation settings
        this.transformationEnabled = config.getBoolean("proxy.transformation.enabled");
        this.transformerClass = config.hasPath("proxy.transformation.transformerClass")
            ? config.getString("proxy.transformation.transformerClass")
            : null;
        this.rpcPatternsFile = config.hasPath("proxy.transformation.rpcPatternsFile")
            ? config.getString("proxy.transformation.rpcPatternsFile")
            : "./rpc-patterns.xml";
        this.xsltFilesPath = config.hasPath("proxy.transformation.xsltFilesPath")
            ? config.getString("proxy.transformation.xsltFilesPath")
            : "./xslt-files";

        // Load logging settings
        this.logMessages = config.getBoolean("proxy.logging.logMessages");
        this.logDir = config.getString("proxy.logging.logDir");

        // Load hello XML settings
        this.helloXmlFile = config.hasPath("proxy.server.helloXmlFile")
            ? config.getString("proxy.server.helloXmlFile")
            : null;
    }

    // Getters
    public String getServerHost() { return serverHost; }
    public int getServerPort() { return serverPort; }
    public String getServerUsername() { return serverUsername; }
    public String getServerPassword() { return serverPassword; }

    public String getDeviceHost() { return deviceHost; }
    public int getDevicePort() { return devicePort; }
    public String getDeviceUsername() { return deviceUsername; }
    public String getDevicePassword() { return devicePassword; }
    public int getConnectionTimeoutMs() { return connectionTimeoutMs; }
    public int getKeepAliveIntervalMs() { return keepAliveIntervalMs; }

    public boolean isTransformationEnabled() { return transformationEnabled; }
    public String getTransformerClass() { return transformerClass; }
    public String getRpcPatternsFile() { return rpcPatternsFile; }
    public String getXsltFilesPath() { return xsltFilesPath; }

    public boolean isLogMessages() { return logMessages; }
    public String getLogDir() { return logDir; }

    public String getHelloXmlFile() { return helloXmlFile; }

    @Override
    public String toString() {
        return String.format("ProxyConfig{server=%s:%d, device=%s:%d, transformation=%s}",
            serverHost, serverPort, deviceHost, devicePort, transformationEnabled);
    }
}
