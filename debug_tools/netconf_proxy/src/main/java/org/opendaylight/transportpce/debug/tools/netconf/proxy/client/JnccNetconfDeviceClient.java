/*
 * Copyright © 2026 1FINITY Inc.,  and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.debug.tools.netconf.proxy.client;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.jcraft.jsch.ChannelSubsystem;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import org.opendaylight.transportpce.debug.tools.netconf.proxy.config.ProxyConfig;
import org.opendaylight.transportpce.debug.tools.netconf.proxy.notification.NotificationBroker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JSch-based NETCONF client for connecting to the real device (southbound).
 * This is a lightweight alternative implementation to the ODL-based NetconfDeviceClient.
 * Uses JSch for SSH transport and implements NETCONF protocol directly.
 * Supports receiving asynchronous NETCONF notifications from the device.
 */
public class JnccNetconfDeviceClient implements INetconfDeviceClient {

    private static final Logger LOG = LoggerFactory.getLogger(JnccNetconfDeviceClient.class);
    private static final String NETCONF_END_DELIMITER = "]]>]]>";
    private static final Pattern CAPABILITY_PATTERN = Pattern.compile("<capability>([^<]+)</capability>");
    private static final Pattern MESSAGE_ID_PATTERN = Pattern.compile("message-id=\"([^\"]+)\"");

    private final ProxyConfig config;
    private Session session;
    private ChannelSubsystem channel;
    private InputStream inputStream;
    private OutputStream outputStream;
    private Set<String> serverCapabilities;

    // Notification support
    private NotificationBroker notificationBroker;
    private Thread messageReaderThread;
    private volatile boolean running = false;
    private final BlockingQueue<String> responseQueue = new LinkedBlockingQueue<>();
    private final Object connectionLock = new Object();

    public JnccNetconfDeviceClient(ProxyConfig config) {
        this.config = config;

        // Enable JSch debug logging
        JSch.setLogger(new com.jcraft.jsch.Logger() {
            @Override
            public boolean isEnabled(int level) {
                return true;
            }

            @Override
            public void log(int level, String message) {
                switch (level) {
                    case com.jcraft.jsch.Logger.DEBUG:
                        LOG.debug("[JSch] {}", message);
                        break;
                    case com.jcraft.jsch.Logger.INFO:
                        LOG.info("[JSch] {}", message);
                        break;
                    case com.jcraft.jsch.Logger.WARN:
                        LOG.warn("[JSch] {}", message);
                        break;
                    case com.jcraft.jsch.Logger.ERROR:
                    case com.jcraft.jsch.Logger.FATAL:
                        LOG.error("[JSch] {}", message);
                        break;
                }
            }
        });
    }

    /**
     * Connect to the NETCONF device.
     *
     * @throws Exception if connection fails
     */
    @Override
    public void connect() throws Exception {
        synchronized (connectionLock) {
            LOG.info("Connecting to NETCONF device at {}:{} using JSch-based client",
                config.getDeviceHost(), config.getDevicePort());

            // Ensure any previous connection is fully cleaned up
            if (isConnected()) {
                LOG.warn("Already connected, disconnecting first");
                disconnect();
            }

            try {
            // Create SSH session
            JSch jsch = new JSch();
            session = jsch.getSession(
                config.getDeviceUsername(),
                config.getDeviceHost(),
                config.getDevicePort()
            );
            session.setPassword(config.getDevicePassword());

            // Disable strict host key checking
            // JSch 0.2.x supports modern SSH algorithms (rsa-sha2-256/512, modern KEX, etc.)
            java.util.Properties sessionConfig = new java.util.Properties();
            sessionConfig.put("StrictHostKeyChecking", "no");
            session.setConfig(sessionConfig);

            // Set timeout
            session.setTimeout(config.getConnectionTimeoutMs());

            // Connect SSH session
            LOG.debug("Establishing SSH connection...");
            session.connect();

            // Open NETCONF subsystem channel
            LOG.debug("Opening NETCONF subsystem channel...");
            channel = (ChannelSubsystem) session.openChannel("subsystem");
            channel.setSubsystem("netconf");

            // Get streams before connecting channel
            inputStream = channel.getInputStream();
            outputStream = channel.getOutputStream();

            // Connect channel
            channel.connect(config.getConnectionTimeoutMs());

            // Exchange hello messages (synchronous, before starting background thread)
            exchangeHello();

            LOG.info("Successfully connected to NETCONF device using JSch");
            LOG.info("Device capabilities: {}", serverCapabilities);

            // Start background message reader thread for notifications
            // IMPORTANT: Start AFTER hello exchange to avoid race conditions on input stream
            startMessageReader();

            } catch (Exception e) {
                LOG.error("Failed to connect to NETCONF device", e);
                cleanup();
                throw new Exception("Failed to establish NETCONF session: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Exchange NETCONF hello messages.
     */
    private void exchangeHello() throws Exception {
        // Send client hello
        String clientHello = buildClientHello();
        sendMessage(clientHello);
        LOG.debug("Sent client hello");

        // Read server hello
        String serverHello = readMessage();
        LOG.debug("Received server hello");

        // Parse server capabilities
        serverCapabilities = parseCapabilities(serverHello);
        LOG.debug("Parsed {} server capabilities", serverCapabilities.size());
    }

    /**
     * Build client hello message.
     */
    private String buildClientHello() {
        StringBuilder hello = new StringBuilder();
        hello.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        hello.append("<hello xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">\n");
        hello.append("  <capabilities>\n");
        hello.append("    <capability>urn:ietf:params:netconf:base:1.0</capability>\n");
        //hello.append("    <capability>urn:ietf:params:netconf:base:1.1</capability>\n");
        hello.append("  </capabilities>\n");
        hello.append("</hello>");
        return hello.toString();
    }

    /**
     * Parse capabilities from hello message.
     */
    private Set<String> parseCapabilities(String hello) {
        Set<String> capabilities = new HashSet<>();
        Matcher matcher = CAPABILITY_PATTERN.matcher(hello);
        while (matcher.find()) {
            capabilities.add(matcher.group(1));
        }
        return capabilities;
    }

    /**
     * Set the notification broker for broadcasting notifications to clients.
     * Must be called before connect().
     *
     * @param broker the notification broker
     */
    public void setNotificationBroker(NotificationBroker broker) {
        this.notificationBroker = broker;
        LOG.info("Notification broker configured");
    }

    /**
     * Send an RPC to the device and get the response.
     *
     * @param rpcXml the RPC XML to send
     * @return the RPC response XML
     * @throws Exception if RPC fails
     */
    @Override
    public String sendRpc(String rpcXml) throws Exception {
        if (!isConnected()) {
            LOG.warn("NETCONF session is not connected, attempting to reconnect...");
            try {
                disconnect();
                connect();
                LOG.info("Successfully reconnected to device");
            } catch (Exception e) {
                throw new IllegalStateException("NETCONF session is not active and reconnection failed: " + e.getMessage(), e);
            }
        }

        LOG.debug("Sending RPC to device via JSch");

        try {
            // Send RPC
            sendMessage(rpcXml);

            // Wait for response from queue (populated by background reader thread)
            String response = responseQueue.poll(config.getConnectionTimeoutMs(), TimeUnit.MILLISECONDS);

            if (response == null) {
                throw new Exception("Timeout waiting for device response");
            }

            LOG.debug("Received response from device via JSch");

            return response;

        } catch (Exception e) {
            LOG.error("Error sending RPC to device", e);
            throw new Exception("Failed to send RPC to device: " + e.getMessage(), e);
        }
    }

    /**
     * Send a NETCONF message.
     */
    private void sendMessage(String message) throws Exception {
        if (outputStream == null) {
            throw new IllegalStateException("Output stream is not available");
        }

        byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
        outputStream.write(messageBytes);
        outputStream.write(("\n" + NETCONF_END_DELIMITER).getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
    }

    /**
     * Start background thread to read messages from device.
     * This thread distinguishes between RPC responses and notifications.
     */
    private void startMessageReader() {
        if (messageReaderThread != null && messageReaderThread.isAlive()) {
            LOG.warn("Message reader thread already running");
            return;
        }

        running = true;
        messageReaderThread = new Thread(() -> {
            LOG.info("Message reader thread started");

            try {
                while (running && isConnected()) {
                    try {
                        // Read next message from device
                        String message = readMessageFromStream();

                        if (message == null || message.isEmpty()) {
                            continue;
                        }

                        // Determine if this is a notification or RPC response
                        if (isNotification(message)) {
                            // This is a notification - forward to broker
                            LOG.debug("Received notification from device");
                            if (notificationBroker != null) {
                                notificationBroker.broadcastNotification(message);
                            } else {
                                LOG.warn("Received notification but no broker configured");
                            }
                        } else {
                            // This is an RPC response - put in queue
                            LOG.debug("Received RPC response from device");
                            responseQueue.offer(message);
                        }

                    } catch (Exception e) {
                        if (running) {
                            LOG.error("Error reading message from device", e);
                            if (notificationBroker != null) {
                                notificationBroker.broadcastError(e);
                            }
                        }
                    }
                }
            } finally {
                LOG.info("Message reader thread stopped");
            }
        }, "netconf-message-reader");

        messageReaderThread.setDaemon(true);
        messageReaderThread.start();
        LOG.info("Message reader thread started successfully");
    }

    /**
     * Read a NETCONF message from the input stream.
     * Reads character-by-character until the delimiter is found.
     */
    private String readMessageFromStream() throws Exception {
        if (inputStream == null) {
            throw new IllegalStateException("Input stream is not available");
        }

        StringBuilder message = new StringBuilder();
        InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);

        // Read character by character until we find the delimiter
        int ch;
        while ((ch = reader.read()) != -1) {
            message.append((char) ch);

            // Check if we've received the complete delimiter
            String current = message.toString();
            if (current.endsWith(NETCONF_END_DELIMITER)) {
                break;
            }
        }

        String result = message.toString();
        // Remove delimiter
        return result.replace(NETCONF_END_DELIMITER, "").trim();
    }

    /**
     * Determine if a message is a notification or an RPC response.
     * Notifications contain <notification> tag (with or without namespace prefix).
     * RPC responses contain <rpc-reply> (with or without namespace prefix).
     *
     * Handles various formats:
     * Notifications:
     *   - <notification xmlns="...">
     *   - <nc:notification xmlns:nc="...">
     *   - <netconf:notification>
     *
     * RPC Replies:
     *   - <rpc-reply message-id="...">
     *   - <nc:rpc-reply xmlns:nc="..." message-id="...">
     *   - <netconf:rpc-reply>
     */
    private boolean isNotification(String message) {
        // Normalize message for easier matching (remove extra whitespace)
        String normalized = message.replaceAll("\\s+", " ");

        // Check for notification tag (with or without namespace prefix)
        // Pattern: < + optional_prefix + : + notification + (space or >)
        // Matches: <notification>, <nc:notification>, <netconf:notification>, etc.
        if (normalized.matches(".*<[a-zA-Z0-9]*:?notification[\\s>].*")) {
            LOG.debug("Detected notification message");
            return true;
        }

        // Check for rpc-reply tag (with or without namespace prefix)
        // Pattern: < + optional_prefix + : + rpc-reply + (space or >)
        // Matches: <rpc-reply>, <nc:rpc-reply>, <netconf:rpc-reply>, etc.
        if (normalized.matches(".*<[a-zA-Z0-9]*:?rpc-reply[\\s>].*")) {
            LOG.debug("Detected rpc-reply message");
            return false;
        }

        // Default: treat as RPC response (safer to not lose responses)
        LOG.warn("Unable to determine message type, treating as RPC response: {}",
            message.substring(0, Math.min(200, message.length())));
        return false;
    }

    /**
     * Read a NETCONF message synchronously (used during hello exchange).
     */
    private String readMessage() throws Exception {
        return readMessageFromStream();
    }

    /**
     * Get the device capabilities.
     */
    @Override
    public Set<String> getDeviceCapabilities() {
        if (serverCapabilities != null) {
            return new HashSet<>(serverCapabilities);
        }
        return new HashSet<>();
    }

    /**
     * Check if connected to device.
     */
    @Override
    public boolean isConnected() {
        return session != null && session.isConnected()
            && channel != null && channel.isConnected();
    }

    /**
     * Disconnect from the device.
     */
    @Override
    public void disconnect() {
        synchronized (connectionLock) {
            LOG.info("Disconnecting from NETCONF device");

            // Stop message reader thread
            running = false;
            if (messageReaderThread != null && messageReaderThread.isAlive()) {
                LOG.info("Stopping message reader thread");
                messageReaderThread.interrupt();
                try {
                    messageReaderThread.join(2000); // Wait up to 2 seconds
                } catch (InterruptedException e) {
                    LOG.warn("Interrupted while waiting for message reader to stop");
                    Thread.currentThread().interrupt();
                }
            }

            cleanup();
            LOG.info("Disconnected from NETCONF device");
        }
    }

    /**
     * Cleanup resources.
     */
    private void cleanup() {
        if (channel != null && channel.isConnected()) {
            try {
                channel.disconnect();
            } catch (Exception e) {
                LOG.error("Error disconnecting channel", e);
            }
        }

        if (session != null && session.isConnected()) {
            try {
                session.disconnect();
            } catch (Exception e) {
                LOG.error("Error disconnecting session", e);
            }
        }

        channel = null;
        session = null;
        inputStream = null;
        outputStream = null;
        responseQueue.clear();
    }
}
