/*
 * Copyright © 2026 1FINITY Inc.,  and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.debug.tools.netconf.proxy.server;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.opendaylight.transportpce.debug.tools.netconf.proxy.client.INetconfDeviceClient;
import org.opendaylight.transportpce.debug.tools.netconf.proxy.client.SharedDeviceClientManager;
import org.opendaylight.transportpce.debug.tools.netconf.proxy.config.ProxyConfig;
import org.opendaylight.transportpce.debug.tools.netconf.proxy.notification.NotificationListener;
import org.opendaylight.transportpce.debug.tools.netconf.proxy.transform.RpcPattern;
import org.opendaylight.transportpce.debug.tools.netconf.proxy.transform.RpcPatternMatcher;
import org.opendaylight.transportpce.debug.tools.netconf.proxy.transform.RpcTransformer;
import org.opendaylight.transportpce.debug.tools.netconf.proxy.util.XmlUtils;
import org.opendaylight.transportpce.debug.tools.netconf.proxy.util.XsltTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

/**
 * Handles a NETCONF session with a client.
 * Manages the protocol flow: hello exchange, RPC forwarding, etc.
 */
public class NetconfSessionHandler implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(NetconfSessionHandler.class);

    private static final String NETCONF_1_0_END_DELIMITER = "]]>]]>";
    private static final String NETCONF_BASE_1_0 = "urn:ietf:params:netconf:base:1.0";
    private static final String NETCONF_BASE_1_1 = "urn:ietf:params:netconf:base:1.1";

    // Thread-safe counter for unique session IDs
    private static final AtomicInteger sessionIdCounter = new AtomicInteger(1);

    private final InputStream inputStream;
    private final OutputStream outputStream;
    private final ProxyConfig config;
    private final RpcTransformer transformer;
    private final SharedDeviceClientManager deviceManager;
    private final RpcPatternMatcher patternMatcher;
    private final XsltTransformer xsltTransformer;
    private INetconfDeviceClient deviceClient;
    private NotificationListener notificationListener;
    private BufferedReader bufferedReader;

    private boolean useChunkedFraming = false;
    private volatile boolean running = true;

    public NetconfSessionHandler(InputStream inputStream,
                                  OutputStream outputStream,
                                  ProxyConfig config,
                                  RpcTransformer transformer,
                                  SharedDeviceClientManager deviceManager,
                                  RpcPatternMatcher patternMatcher,
                                  XsltTransformer xsltTransformer) {
        this.inputStream = inputStream;
        this.outputStream = outputStream;
        this.config = config;
        this.transformer = transformer;
        this.deviceManager = deviceManager;
        this.patternMatcher = patternMatcher;
        this.xsltTransformer = xsltTransformer;
    }

    @Override
    public void run() {
        try {
            LOG.info("NETCONF session started - client authenticated");

            // Get shared device client (already connected at startup)
            deviceClient = deviceManager.getDeviceClient();
            LOG.info("Using shared device connection");

            // Register for notifications from the device
            registerForNotifications();

            // Send hello to client
            sendHelloToClient();

            // Read client hello
            readClientHello();

            // Process RPC messages
            processRpcMessages();

        } catch (Exception e) {
            LOG.error("Error in NETCONF session", e);
        } finally {
            cleanup();
        }
    }

    /**
     * Register this session to receive notifications from the device.
     */
    private void registerForNotifications() {
        notificationListener = new NotificationListener() {
            @Override
            public void onNotification(String notificationXml) {
                try {
                    LOG.debug("Received notification from device, forwarding to client");

                    // Forward the notification to the client
                    sendMessage(notificationXml);

                    if (config.isLogMessages()) {
                        LOG.info("Forwarded notification to client:\n{}", XmlUtils.prettyPrint(notificationXml));
                    }

                } catch (Exception e) {
                    LOG.error("Error forwarding notification to client", e);
                }
            }

            @Override
            public void onError(Exception error) {
                LOG.error("Notification error from device", error);
            }
        };

        // Register with the notification broker
        deviceManager.getNotificationBroker().registerListener(notificationListener);
        LOG.info("Registered for device notifications");
    }

    /**
     * Send hello message to client IMMEDIATELY after authentication.
     */
    private void sendHelloToClient() throws Exception {
        LOG.debug("Sending hello to client immediately");

        // Get capabilities - either from hello XML file or from device
        Set<String> capabilities;
        if (config.getHelloXmlFile() != null && !config.getHelloXmlFile().isEmpty()) {
            LOG.info("Loading capabilities from hello XML file: {}", config.getHelloXmlFile());
            try {
                capabilities = XmlUtils.loadCapabilitiesFromFile(config.getHelloXmlFile());
                LOG.info("Loaded {} capabilities from hello XML file", capabilities.size());
            } catch (Exception e) {
                LOG.error("Failed to load capabilities from hello XML file, falling back to device capabilities", e);
                capabilities = deviceClient.getDeviceCapabilities();
            }
        } else {
            // Get device capabilities from shared connection
            capabilities = deviceClient.getDeviceCapabilities();
        }

        // Build and send hello to client
        String serverHello = buildHelloMessage(capabilities);
        sendMessage(serverHello);

        if (config.isLogMessages()) {
            LOG.info("Sent server hello:\n{}", XmlUtils.prettyPrint(serverHello));
        }

        LOG.info("Hello sent to client immediately after authentication");
    }

    /**
     * Read client hello message.
     */
    private void readClientHello() throws Exception {
        LOG.debug("Reading client hello");

        // Read client hello
        String clientHello = readMessage();
        if (config.isLogMessages()) {
            LOG.info("Received client hello:\n{}", XmlUtils.prettyPrint(clientHello));
        }

        // Check if client supports base:1.1 (chunked framing)
      /**
        if (clientHello.contains(NETCONF_BASE_1_1)) {
            useChunkedFraming = true;
            LOG.info("Using NETCONF 1.1 chunked framing");
        } else {
            LOG.info("Using NETCONF 1.0 end-of-message framing");
        }*/

        LOG.debug("Client hello received and processed");
    }

    /**
     * Build hello message with capabilities.
     */
    private String buildHelloMessage(Set<String> deviceCapabilities) {
        StringBuilder hello = new StringBuilder();
        hello.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        hello.append("<hello xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">\n");
        hello.append("  <capabilities>\n");

        // Add base capabilities
        hello.append("    <capability>urn:ietf:params:netconf:base:1.0</capability>\n");
        // hello.append("    <capability>urn:ietf:params:netconf:base:1.1</capability>\n");

        // Add device capabilities
        for (String capability : deviceCapabilities) {
            if (!capability.contains("base:1.0") && !capability.contains("base:1.1")) {
                hello.append("    <capability>").append(escapeXml(capability)).append("</capability>\n");
            }
        }

        hello.append("  </capabilities>\n");
        hello.append("  <session-id>").append(sessionIdCounter.getAndIncrement()).append("</session-id>\n");
        hello.append("</hello>");

        return hello.toString();
    }

    /**
     * Escape XML special characters in a string.
     */
    private String escapeXml(String text) {
        if (text == null) {
            return null;
        }
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&apos;");
    }

    /**
     * Process incoming RPC messages from client.
     */
    private void processRpcMessages() throws Exception {
        LOG.debug("Starting RPC message processing");

        while (running) {
            String clientRpc = null;
            try {
                // Read RPC from client
                clientRpc = readMessage();

                if (clientRpc == null || clientRpc.trim().isEmpty()) {
                    LOG.debug("Empty message received, ending session");
                    break;
                }

                if (config.isLogMessages()) {
                    LOG.info("Received RPC from client:\n{}", XmlUtils.prettyPrint(clientRpc));
                }

                // Check for close-session
                if (clientRpc.contains("<close-session")) {
                    handleCloseSession(clientRpc);
                    break;
                }

                // Extract message-id from client RPC
                String messageId = XmlUtils.extractMessageId(clientRpc);

                // Parse the RPC
                Document requestDoc = XmlUtils.parseXml(clientRpc);

                // Find matching pattern only if transformation is enabled
                RpcPattern matchedPattern = config.isTransformationEnabled()
                    ? patternMatcher.findMatchingPattern(requestDoc)
                    : null;

                // Check if this RPC should be intercepted
                if (matchedPattern != null && matchedPattern.isIntercepted()) {
                    LOG.info("Intercepting RPC, returning response from file: {}", matchedPattern.getResponseFile());

                    // Read response from file
                    String interceptedResponse = readResponseFromFile(matchedPattern.getResponseFile());

                    // Set message-id on the response
                    if (messageId != null) {
                        interceptedResponse = XmlUtils.setMessageId(interceptedResponse, messageId);
                    }

                    // Send response to client
                    sendMessage(interceptedResponse);

                    if (config.isLogMessages()) {
                        LOG.info("Sent intercepted response:\n{}", XmlUtils.prettyPrint(interceptedResponse));
                    }

                    continue; // Skip forwarding to device
                }

                // Transform request if needed
                String transformedRequest = clientRpc;

                // First, apply pattern-based XSLT transformation if available
                if (config.isTransformationEnabled() && matchedPattern != null && matchedPattern.hasRpcTransformer()) {
                    LOG.debug("Applying RPC XSLT transformation: {}", matchedPattern.getRpcTransformerXsltFile());
                    Document xsltTransformedDoc = xsltTransformer.transform(requestDoc, matchedPattern.getRpcTransformerXsltFile());
                    transformedRequest = XmlUtils.documentToString(xsltTransformedDoc);

                    if (config.isLogMessages()) {
                        LOG.info("XSLT transformed request:\n{}", XmlUtils.prettyPrint(transformedRequest));
                    }

                    // Update requestDoc for potential legacy transformer
                    requestDoc = xsltTransformedDoc;
                }

                // Then, apply legacy transformer if enabled
                if (config.isTransformationEnabled() && transformer != null) {
                    Document transformedDoc = transformer.transformRequest(requestDoc);
                    transformedRequest = XmlUtils.documentToString(transformedDoc);

                    if (config.isLogMessages() && !transformedRequest.equals(clientRpc)) {
                        LOG.info("Legacy transformed request:\n{}", XmlUtils.prettyPrint(transformedRequest));
                    }
                }

                // Forward to device (using manager's sendRpc to track keep-alive)
                String deviceResponse = deviceManager.sendRpc(transformedRequest);

                if (config.isLogMessages()) {
                    LOG.info("Received response from device:\n{}", deviceResponse);
                }

                // Transform response if needed
                String transformedResponse = deviceResponse;
                Document responseDoc = XmlUtils.parseXml(deviceResponse);

                // First, apply legacy transformer if enabled
                if (config.isTransformationEnabled() && transformer != null) {
                    Document transformedDoc = transformer.transformResponse(responseDoc);
                    transformedResponse = XmlUtils.documentToString(transformedDoc);

                    if (config.isLogMessages() && !transformedResponse.equals(deviceResponse)) {
                        LOG.info("Legacy transformed response:\n{}", XmlUtils.prettyPrint(transformedResponse));
                    }

                    // Update responseDoc for potential XSLT transformer
                    responseDoc = transformedDoc;
                }

                // Then, apply pattern-based XSLT transformation if available
                if (config.isTransformationEnabled() && matchedPattern != null && matchedPattern.hasReplyTransformer()) {
                    LOG.debug("Applying reply XSLT transformation: {}", matchedPattern.getReplyTransformerXsltFile());
                    Document xsltTransformedDoc = xsltTransformer.transform(responseDoc, matchedPattern.getReplyTransformerXsltFile());
                    transformedResponse = XmlUtils.documentToString(xsltTransformedDoc);

                    if (config.isLogMessages()) {
                        LOG.info("XSLT transformed response:\n{}", XmlUtils.prettyPrint(transformedResponse));
                    }
                }

                // Set message-id on the response
                if (messageId != null) {
                    transformedResponse = XmlUtils.setMessageId(transformedResponse, messageId);
                }

                // Send response to client
                sendMessage(transformedResponse);

            } catch (Exception e) {
                LOG.error("Error processing RPC", e);
                String errorMessageId = null;
                try {
                    errorMessageId = XmlUtils.extractMessageId(clientRpc);
                } catch (Exception ex) {
                    // Ignore if we can't extract message-id
                }
                sendErrorResponse(e.getMessage(), errorMessageId);
            }
        }

        LOG.debug("RPC message processing ended");
    }

    /**
     * Handle close-session RPC.
     */
    private void handleCloseSession(String closeSessionRpc) throws Exception {
        String messageId = XmlUtils.extractMessageId(closeSessionRpc);

        String response = String.format(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<rpc-reply xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\" message-id=\"%s\">" +
            "<ok/>" +
            "</rpc-reply>",
            messageId != null ? messageId : "0"
        );

        sendMessage(response);
        running = false;
    }

    /**
     * Send error response to client.
     */
    private void sendErrorResponse(String errorMessage, String messageId) throws Exception {
        String messageIdAttr = (messageId != null) ? " message-id=\"" + messageId + "\"" : "";
        String response = String.format(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<rpc-reply xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\"%s>" +
            "<rpc-error>" +
            "<error-type>application</error-type>" +
            "<error-tag>operation-failed</error-tag>" +
            "<error-severity>error</error-severity>" +
            "<error-message>%s</error-message>" +
            "</rpc-error>" +
            "</rpc-reply>",
            messageIdAttr,
            errorMessage
        );

        sendMessage(response);
    }

    /**
     * Read a NETCONF message from the input stream.
     */
    private String readMessage() throws Exception {
        if (useChunkedFraming) {
            return readChunkedMessage();
        } else {
            return readDelimitedMessage();
        }
    }

    /**
     * Read message with NETCONF 1.0 end-of-message delimiter.
     * Reads character-by-character to handle messages without trailing newlines.
     */
    private String readDelimitedMessage() throws Exception {
        // Create BufferedReader once if not already created
        if (bufferedReader == null) {
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        }

        StringBuilder message = new StringBuilder();
        int ch;

        // Read character by character until we find the delimiter
        while ((ch = bufferedReader.read()) != -1) {
            message.append((char) ch);

            // Check if we've accumulated the delimiter at the end
            String current = message.toString();
            if (current.endsWith(NETCONF_1_0_END_DELIMITER)) {
                // Found delimiter, remove it and return
                String result = current.substring(0, current.length() - NETCONF_1_0_END_DELIMITER.length());
                return result.trim();
            }
        }

        // Stream ended without delimiter
        throw new Exception("Stream ended before NETCONF delimiter was found");
    }

    /**
     * Read message with NETCONF 1.1 chunked framing.
     * Format: \n#<size>\n<data>\n##\n
     */
    private String readChunkedMessage() throws Exception {
        StringBuilder message = new StringBuilder();

        while (true) {
            // Read chunk header: \n#<size>\n
            int ch = inputStream.read();
            if (ch == -1) {
                throw new Exception("Unexpected end of stream");
            }

            // Expect newline
            if (ch != '\n') {
                throw new Exception("Expected newline before chunk header, got: " + (char)ch);
            }

            // Expect '#'
            ch = inputStream.read();
            if (ch != '#') {
                throw new Exception("Expected '#' in chunk header, got: " + (char)ch);
            }

            // Read size
            StringBuilder sizeStr = new StringBuilder();
            while ((ch = inputStream.read()) != '\n') {
                if (ch == -1) {
                    throw new Exception("Unexpected end of stream while reading chunk size");
                }
                sizeStr.append((char)ch);
            }

            // Check for end-of-chunks marker: ##
            if (sizeStr.toString().equals("#")) {
                // End of message
                break;
            }

            // Parse chunk size
            int chunkSize;
            try {
                chunkSize = Integer.parseInt(sizeStr.toString());
            } catch (NumberFormatException e) {
                throw new Exception("Invalid chunk size: " + sizeStr.toString());
            }

            // Read chunk data
            byte[] chunkData = new byte[chunkSize];
            int totalRead = 0;
            while (totalRead < chunkSize) {
                int read = inputStream.read(chunkData, totalRead, chunkSize - totalRead);
                if (read == -1) {
                    throw new Exception("Unexpected end of stream while reading chunk data");
                }
                totalRead += read;
            }

            // Append chunk to message
            message.append(new String(chunkData, StandardCharsets.UTF_8));
        }

        String result = message.toString();

        // Clean up malformed messages: remove any XML declarations that appear after the first one
        // Some clients incorrectly send XML declarations in the middle or at the end of messages
        int firstXmlDecl = result.indexOf("<?xml");
        if (firstXmlDecl != -1) {
            int secondXmlDecl = result.indexOf("<?xml", firstXmlDecl + 5);
            if (secondXmlDecl != -1) {
                LOG.warn("Detected malformed message with multiple XML declarations, cleaning up");
                result = result.substring(0, secondXmlDecl).trim();
            }
        }

        return result;
    }

    /**
     * Send a NETCONF message to the output stream.
     */
    private void sendMessage(String message) throws Exception {
        if (useChunkedFraming) {
            sendChunkedMessage(message);
        } else {
            sendDelimitedMessage(message);
        }
        outputStream.flush();
    }

    /**
     * Send message with NETCONF 1.0 end-of-message delimiter.
     */
    private void sendDelimitedMessage(String message) throws Exception {
        outputStream.write(message.getBytes(StandardCharsets.UTF_8));
        outputStream.write(("\n" + NETCONF_1_0_END_DELIMITER).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Send message with NETCONF 1.1 chunked framing.
     */
    private void sendChunkedMessage(String message) throws Exception {
        byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
        String chunkHeader = "\n#" + messageBytes.length + "\n";
        outputStream.write(chunkHeader.getBytes(StandardCharsets.UTF_8));
        outputStream.write(messageBytes);
        outputStream.write("\n##\n".getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Cleanup resources.
     * Note: We don't disconnect the device client as it's shared across sessions.
     */
    private void cleanup() {
        LOG.info("Cleaning up NETCONF session");

        // Unregister from notifications
        if (notificationListener != null) {
            deviceManager.getNotificationBroker().unregisterListener(notificationListener);
            LOG.info("Unregistered from device notifications");
        }

        // Don't disconnect device client - it's shared and managed by SharedDeviceClientManager
        LOG.debug("Device connection is shared, not disconnecting");

        try {
            inputStream.close();
            outputStream.close();
        } catch (Exception e) {
            LOG.error("Error closing streams", e);
        }

        LOG.info("NETCONF session ended");
    }

    /**
     * Read response content from a file for intercepted RPCs.
     */
    private String readResponseFromFile(String filePath) throws Exception {
        try {
            java.nio.file.Path path = java.nio.file.Paths.get(filePath);
            byte[] bytes = java.nio.file.Files.readAllBytes(path);
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            LOG.error("Failed to read response file: {}", filePath, e);
            throw new Exception("Failed to read intercepted response from file: " + filePath, e);
        }
    }
}
