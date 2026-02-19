/*
 * Copyright © 2026 1FINITY Inc.,  and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.debug.tools.netconf.proxy.client;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.opendaylight.transportpce.debug.tools.netconf.proxy.config.ProxyConfig;
import org.opendaylight.transportpce.debug.tools.netconf.proxy.notification.NotificationBroker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages a shared NETCONF device client connection.
 * The connection is established once at startup and shared across all client sessions.
 * Includes keep-alive mechanism to maintain the connection.
 */
public class SharedDeviceClientManager {

    private static final Logger LOG = LoggerFactory.getLogger(SharedDeviceClientManager.class);

    // Keep-alive settings
    private static final long KEEP_ALIVE_INTERVAL_MS = 300000; // 1 minute
    private static final String KEEP_ALIVE_RPC =
        "<nc:rpc xmlns:nc=\"urn:ietf:params:xml:ns:netconf:base:1.0\" nc:message-id=\"keep-alive\">\n" +
        "  <nc:get>\n" +
        "    <nc:filter nc:type=\"subtree\">\n" +
        "      <netconf-state xmlns=\"urn:ietf:params:xml:ns:yang:ietf-netconf-monitoring\">\n" +
        "        <sessions>\n" +
        "          <session>\n" +
        "            <session-id/>          </session>\n" +
        "        </sessions>\n" +
        "      </netconf-state>\n" +
        "    </nc:filter>\n" +
        "  </nc:get>\n" +
        "</nc:rpc>";

    private final ProxyConfig config;
    private final NotificationBroker notificationBroker;
    private INetconfDeviceClient deviceClient;
    private final Object lock = new Object();

    // Keep-alive tracking
    private final AtomicLong lastRpcTime = new AtomicLong(System.currentTimeMillis());
    private ScheduledExecutorService keepAliveExecutor;
    private volatile boolean keepAliveEnabled = false;

    public SharedDeviceClientManager(ProxyConfig config) {
        this.config = config;
        this.notificationBroker = new NotificationBroker();
    }

    /**
     * Initialize and connect to the device.
     * This should be called once at proxy startup.
     *
     * @throws Exception if connection fails
     */
    public void connect() throws Exception {
        synchronized (lock) {
            if (deviceClient != null && deviceClient.isConnected()) {
                LOG.warn("Device client already connected");
                return;
            }

            LOG.info("Establishing shared connection to NETCONF device...");
            deviceClient = NetconfClientFactory.createClient(config);

            // Set up notification broker for JSch client
            ((JnccNetconfDeviceClient) deviceClient).setNotificationBroker(notificationBroker);
            LOG.info("Notification broker configured for JSch device client");

            deviceClient.connect();

            LOG.info("Shared device connection established successfully");

            // Start keep-alive mechanism
            startKeepAlive();
        }
    }

    /**
     * Get the shared device client.
     * Ensures connection is active, reconnects if needed.
     *
     * @return the device client instance
     * @throws Exception if connection fails
     */
    public INetconfDeviceClient getDeviceClient() throws Exception {
        synchronized (lock) {
            if (deviceClient == null || !deviceClient.isConnected()) {
                LOG.warn("Device connection lost, reconnecting...");
                connect();
            }
            return deviceClient;
        }
    }

    /**
     * Check if device is connected.
     */
    public boolean isConnected() {
        synchronized (lock) {
            return deviceClient != null && deviceClient.isConnected();
        }
    }

    /**
     * Get the notification broker for registering listeners.
     *
     * @return the notification broker
     */
    public NotificationBroker getNotificationBroker() {
        return notificationBroker;
    }

    /**
     * Send an RPC through the device client and update last RPC time.
     * This wrapper ensures keep-alive tracking is updated.
     *
     * @param rpcXml the RPC XML to send
     * @return the RPC response XML
     * @throws Exception if RPC fails
     */
    public String sendRpc(String rpcXml) throws Exception {
        // Update last RPC time
        long currentTime = System.currentTimeMillis();
        lastRpcTime.set(currentTime);
        LOG.debug("Updated last RPC time to: {}", currentTime);

        // Forward to device client
        return getDeviceClient().sendRpc(rpcXml);
    }

    /**
     * Start the keep-alive mechanism.
     */
    private void startKeepAlive() {
        if (keepAliveEnabled) {
            LOG.warn("Keep-alive already started");
            return;
        }

        LOG.info("Starting keep-alive mechanism (interval: {} seconds)", KEEP_ALIVE_INTERVAL_MS / 1000);

        keepAliveExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "device-keep-alive");
            t.setDaemon(true);
            return t;
        });

        // Check every 10 seconds if keep-alive is needed
        keepAliveExecutor.scheduleAtFixedRate(this::checkAndSendKeepAlive, 10, 10, TimeUnit.SECONDS);
        keepAliveEnabled = true;

        LOG.info("Keep-alive mechanism started");
    }

    /**
     * Check if keep-alive is needed and send it.
     */
    private void checkAndSendKeepAlive() {
        try {
            long currentTime = System.currentTimeMillis();
            long lastRpc = lastRpcTime.get();
            long timeSinceLastRpc = currentTime - lastRpc;

            LOG.trace("Keep-alive check: idle for {} seconds", timeSinceLastRpc / 1000);

            if (timeSinceLastRpc >= KEEP_ALIVE_INTERVAL_MS) {
                LOG.info("Sending keep-alive RPC (idle for {} seconds)", timeSinceLastRpc / 1000);

                synchronized (lock) {
                    if (deviceClient != null && deviceClient.isConnected()) {
                        try {
                            String response = deviceClient.sendRpc(KEEP_ALIVE_RPC);
                            lastRpcTime.set(System.currentTimeMillis());
                            LOG.info("Keep-alive RPC successful. Response:\n{}", response);
                        } catch (Exception e) {
                            LOG.error("Keep-alive RPC failed", e);
                            // Don't update lastRpcTime on failure, will retry
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.error("Error in keep-alive check", e);
        }
    }

    /**
     * Stop the keep-alive mechanism.
     */
    private void stopKeepAlive() {
        if (keepAliveExecutor != null) {
            LOG.info("Stopping keep-alive mechanism");
            keepAliveExecutor.shutdown();
            try {
                if (!keepAliveExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    keepAliveExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                keepAliveExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
            keepAliveEnabled = false;
            LOG.info("Keep-alive mechanism stopped");
        }
    }

    /**
     * Disconnect from the device.
     * This should be called at proxy shutdown.
     */
    public void disconnect() {
        synchronized (lock) {
            // Stop keep-alive first
            stopKeepAlive();

            if (deviceClient != null) {
                LOG.info("Disconnecting shared device connection...");
                try {
                    deviceClient.disconnect();
                } catch (Exception e) {
                    LOG.error("Error disconnecting device client", e);
                }
                deviceClient = null;
                notificationBroker.clearListeners();
                LOG.info("Shared device connection closed");
            }
        }
    }
}
