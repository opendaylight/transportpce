/*
 * Copyright © 2026 1FINITY Inc.,  and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.debug.tools.netconf.proxy.server;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;

import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.command.Command;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.subsystem.SubsystemFactory;
import org.opendaylight.transportpce.debug.tools.netconf.proxy.client.SharedDeviceClientManager;
import org.opendaylight.transportpce.debug.tools.netconf.proxy.config.ProxyConfig;
import org.opendaylight.transportpce.debug.tools.netconf.proxy.transform.RpcPatternMatcher;
import org.opendaylight.transportpce.debug.tools.netconf.proxy.transform.RpcTransformer;
import org.opendaylight.transportpce.debug.tools.netconf.proxy.util.XsltTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SSH server that accepts NETCONF connections from clients (northbound).
 */
public class NetconfSshServer {

    private static final Logger LOG = LoggerFactory.getLogger(NetconfSshServer.class);

    private final ProxyConfig config;
    private final RpcTransformer transformer;
    private final SharedDeviceClientManager deviceManager;
    private final RpcPatternMatcher patternMatcher;
    private final XsltTransformer xsltTransformer;
    private final SshServer sshServer;

    public NetconfSshServer(ProxyConfig config, RpcTransformer transformer, SharedDeviceClientManager deviceManager,
                           RpcPatternMatcher patternMatcher, XsltTransformer xsltTransformer) {
        this.config = config;
        this.transformer = transformer;
        this.deviceManager = deviceManager;
        this.patternMatcher = patternMatcher;
        this.xsltTransformer = xsltTransformer;
        this.sshServer = SshServer.setUpDefaultServer();
    }

    /**
     * Start the SSH server.
     *
     * @throws IOException if server startup fails
     */
    public void start() throws IOException {
        LOG.info("Starting NETCONF SSH server on {}:{}", config.getServerHost(), config.getServerPort());

        // Configure SSH server
        sshServer.setHost(config.getServerHost());
        sshServer.setPort(config.getServerPort());

        // Set up host key - use RSA algorithm to avoid EC key warnings
        SimpleGeneratorHostKeyProvider keyProvider = new SimpleGeneratorHostKeyProvider(Paths.get("hostkey.ser"));
        keyProvider.setAlgorithm("RSA");
        keyProvider.setKeySize(2048);
        sshServer.setKeyPairProvider(keyProvider);

        // Configure password authentication
        sshServer.setPasswordAuthenticator(createPasswordAuthenticator());

        // Configure NETCONF subsystem
        sshServer.setSubsystemFactories(Collections.singletonList(createNetconfSubsystemFactory()));

        // Start server
        sshServer.start();

        LOG.info("NETCONF SSH server started successfully");
        LOG.info("Clients can connect using: ssh {}@{}:{} -s netconf",
            config.getServerUsername(), config.getServerHost(), config.getServerPort());
    }

    /**
     * Stop the SSH server.
     *
     * @throws IOException if server shutdown fails
     */
    public void stop() throws IOException {
        LOG.info("Stopping NETCONF SSH server");
        sshServer.stop();
        LOG.info("NETCONF SSH server stopped");
    }

    /**
     * Create password authenticator.
     */
    private PasswordAuthenticator createPasswordAuthenticator() {
        return (username, password, session) -> {
            boolean authenticated = config.getServerUsername().equals(username)
                && config.getServerPassword().equals(password);

            if (authenticated) {
                LOG.info("User {} authenticated successfully from {}", username, session.getClientAddress());
            } else {
                LOG.warn("Authentication failed for user {} from {}", username, session.getClientAddress());
            }

            return authenticated;
        };
    }

    /**
     * Create NETCONF subsystem factory.
     */
    private SubsystemFactory createNetconfSubsystemFactory() {
        return new SubsystemFactory() {
            @Override
            public String getName() {
                return "netconf";
            }

            @Override
            public Command createSubsystem(org.apache.sshd.server.channel.ChannelSession channel) {
                return new NetconfSubsystemCommand(config, transformer, deviceManager, patternMatcher, xsltTransformer);
            }
        };
    }
}
