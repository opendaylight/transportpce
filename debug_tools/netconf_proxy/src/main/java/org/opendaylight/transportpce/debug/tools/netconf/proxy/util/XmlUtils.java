/*
 * Copyright © 2026 1FINITY Inc.,  and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.debug.tools.netconf.proxy.util;

import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Utility methods for XML processing.
 */
public final class XmlUtils {

    private static final DocumentBuilderFactory DOC_BUILDER_FACTORY;
    private static final TransformerFactory TRANSFORMER_FACTORY;

    static {
        DOC_BUILDER_FACTORY = DocumentBuilderFactory.newInstance();
        DOC_BUILDER_FACTORY.setNamespaceAware(true);

        TRANSFORMER_FACTORY = TransformerFactory.newInstance();
    }

    private XmlUtils() {
        // Prevent instantiation
    }

    /**
     * Parse XML string to Document.
     *
     * @param xml the XML string to parse
     * @return the parsed Document
     * @throws Exception if parsing fails
     */
    public static Document parseXml(String xml) throws Exception {
        DocumentBuilder builder = DOC_BUILDER_FACTORY.newDocumentBuilder();
        return builder.parse(new InputSource(new StringReader(xml)));
    }

    /**
     * Convert Document to XML string.
     *
     * @param doc the Document to convert
     * @return the XML string representation
     * @throws Exception if conversion fails
     */
    public static String documentToString(Document doc) throws Exception {
        Transformer transformer = TRANSFORMER_FACTORY.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.INDENT, "no");

        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));
        return writer.toString();
    }

    /**
     * Pretty print XML string for logging.
     */
    public static String prettyPrint(String xml) {
        try {
            Document doc = parseXml(xml);
            Transformer transformer = TRANSFORMER_FACTORY.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(writer));
            return writer.toString();
        } catch (Exception e) {
            return xml; // Return original if pretty print fails
        }
    }

    /**
     * Extract message-id from NETCONF RPC.
     */
    public static String extractMessageId(String rpcXml) {
        try {
            Document doc = parseXml(rpcXml);
            String messageId = doc.getDocumentElement().getAttribute("message-id");
            if (messageId == null || messageId.isEmpty()) {
                messageId = doc.getDocumentElement().getAttributeNS("urn:ietf:params:xml:ns:netconf:base:1.0", "message-id");
            }
            return messageId != null && !messageId.isEmpty() ? messageId : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Set message-id attribute on NETCONF rpc-reply element.
     *
     * @param rpcReplyXml the RPC reply XML string
     * @param messageId the message ID to set
     * @return the modified XML string with message-id set
     * @throws Exception if XML processing fails
     */
    public static String setMessageId(String rpcReplyXml, String messageId) throws Exception {
        if (messageId == null || messageId.isEmpty()) {
            return rpcReplyXml;
        }

        Document doc = parseXml(rpcReplyXml);
        if (doc.getDocumentElement().hasAttribute("message-id")) {
            doc.getDocumentElement().setAttribute("message-id", messageId);
        } else {
            doc.getDocumentElement().setAttribute("message-id", messageId);
        }
        return documentToString(doc);
    }

    /**
     * Load capabilities from a hello XML file.
     * Expected format:
     * <hello xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
     *   <capabilities>
     *     <capability>urn:ietf:params:netconf:base:1.0</capability>
     *     ...
     *   </capabilities>
     * </hello>
     *
     * @param filePath the path to the hello XML file
     * @return set of capability URIs
     * @throws Exception if file reading or XML parsing fails
     */
    public static Set<String> loadCapabilitiesFromFile(String filePath) throws Exception {
        Set<String> capabilities = new HashSet<>();

        File file = new File(filePath);
        if (!file.exists()) {
            throw new Exception("Capabilities file not found: " + filePath);
        }

        String xmlContent = new String(Files.readAllBytes(file.toPath()));
        Document doc = parseXml(xmlContent);

        NodeList capabilityNodes = doc.getElementsByTagNameNS("urn:ietf:params:xml:ns:netconf:base:1.0", "capability");
        if (capabilityNodes.getLength() == 0) {
            // Try without namespace
            capabilityNodes = doc.getElementsByTagName("capability");
        }

        for (int i = 0; i < capabilityNodes.getLength(); i++) {
            Element capElement = (Element) capabilityNodes.item(i);
            String capability = capElement.getTextContent().trim();
            if (!capability.isEmpty()) {
                capabilities.add(capability);
            }
        }

        return capabilities;
    }
}
