/*
 * Copyright © 2026 1FINITY Inc.,  and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.debug.tools.netconf.proxy.server;

import java.io.InputStream;
import java.io.OutputStream;

import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.command.Command;
import org.opendaylight.transportpce.debug.tools.netconf.proxy.client.SharedDeviceClientManager;
import org.opendaylight.transportpce.debug.tools.netconf.proxy.config.ProxyConfig;
import org.opendaylight.transportpce.debug.tools.netconf.proxy.transform.RpcPatternMatcher;
import org.opendaylight.transportpce.debug.tools.netconf.proxy.transform.RpcTransformer;
import org.opendaylight.transportpce.debug.tools.netconf.proxy.util.XsltTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SSH subsystem command for NETCONF.
 * This is invoked when a client connects with "ssh ... -s netconf"
 */
public class NetconfSubsystemCommand implements Command {

    private static final Logger LOG = LoggerFactory.getLogger(NetconfSubsystemCommand.class);

    private final ProxyConfig config;
    private final RpcTransformer transformer;
    private final SharedDeviceClientManager deviceManager;
    private final RpcPatternMatcher patternMatcher;
    private final XsltTransformer xsltTransformer;

    private InputStream inputStream;
    private OutputStream outputStream;
    private OutputStream errorStream;
    private ExitCallback exitCallback;
    private Thread sessionThread;

    public NetconfSubsystemCommand(ProxyConfig config, RpcTransformer transformer, SharedDeviceClientManager deviceManager,
                                  RpcPatternMatcher patternMatcher, XsltTransformer xsltTransformer) {
        this.config = config;
        this.transformer = transformer;
        this.deviceManager = deviceManager;
        this.patternMatcher = patternMatcher;
        this.xsltTransformer = xsltTransformer;
    }

    @Override
    public void setInputStream(InputStream in) {
        this.inputStream = in;
    }

    @Override
    public void setOutputStream(OutputStream out) {
        this.outputStream = out;
    }

    @Override
    public void setErrorStream(OutputStream err) {
        this.errorStream = err;
    }

    @Override
    public void setExitCallback(ExitCallback callback) {
        this.exitCallback = callback;
    }

    @Override
    public void start(ChannelSession channel, Environment env) {
        LOG.info("NETCONF subsystem started - client authenticated");

        // Create and start session handler in a new thread
        // Pass the shared device manager instead of creating a new client
        NetconfSessionHandler sessionHandler = new NetconfSessionHandler(
            inputStream, outputStream, config, transformer, deviceManager, patternMatcher, xsltTransformer
        );

        sessionThread = new Thread(sessionHandler, "netconf-session");
        sessionThread.start();

        // Monitor thread and call exit callback when done
        new Thread(() -> {
            try {
                sessionThread.join();
                exitCallback.onExit(0);
            } catch (InterruptedException e) {
                LOG.error("Session thread interrupted", e);
                exitCallback.onExit(1);
            }
        }, "netconf-monitor").start();
    }

    /**
     * Destroy the subsystem command.
     *
     * @param channel the channel session
     * @throws Exception if cleanup fails
     */
    @Override
    public void destroy(ChannelSession channel) throws Exception {
        LOG.info("NETCONF subsystem destroyed");

        if (sessionThread != null && sessionThread.isAlive()) {
            sessionThread.interrupt();
        }
    }
}
