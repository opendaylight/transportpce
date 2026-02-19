/*
 * Copyright © 2026 1FINITY Inc.,  and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.debug.tools.netconf.proxy.transform;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.opendaylight.transportpce.debug.tools.netconf.proxy.util.XmlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Matches incoming RPC messages against patterns defined in rpc-patterns.xml.
 * Performs structural XML comparison ignoring attribute values and leaf element values.
 */
public class RpcPatternMatcher {

    private static final Logger LOG = LoggerFactory.getLogger(RpcPatternMatcher.class);

    private final List<RpcPattern> patterns;

    /**
     * Create a new RPC pattern matcher.
     *
     * @param patternsFilePath path to the RPC patterns XML file
     * @throws Exception if pattern file cannot be loaded or parsed
     */
    public RpcPatternMatcher(String patternsFilePath) throws Exception {
        this.patterns = loadPatterns(patternsFilePath);
        LOG.info("Loaded {} RPC patterns from {}", patterns.size(), patternsFilePath);
    }

    /**
     * Load RPC patterns from the patterns file.
     */
    private List<RpcPattern> loadPatterns(String patternsFilePath) throws Exception {
        List<RpcPattern> patternList = new ArrayList<>();

        File patternsFile = new File(patternsFilePath);
        if (!patternsFile.exists()) {
            LOG.warn("RPC patterns file not found: {}", patternsFilePath);
            return patternList;
        }

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(patternsFile);

        NodeList patternNodes = doc.getElementsByTagName("pattern");
        for (int i = 0; i < patternNodes.getLength(); i++) {
            Element patternElement = (Element) patternNodes.item(i);

            // Extract the pattern RPC (first child element, should be <nc:rpc> or <rpc>)
            Element patternRpcElement = getFirstChildElement(patternElement);
            if (patternRpcElement == null) {
                LOG.warn("Pattern {} has no RPC element, skipping", i);
                continue;
            }

            // Create a document from the pattern RPC element
            Document patternDoc = builder.newDocument();
            Node importedNode = patternDoc.importNode(patternRpcElement, true);
            patternDoc.appendChild(importedNode);

            // Extract transformer XSLT files
            String rpcXsltFile = extractXsltFile(patternElement, "rpc-transformer");
            String replyXsltFile = extractXsltFile(patternElement, "reply-transformer");

            // Extract response file for interception
            String responseFile = extractResponseFile(patternElement);

            RpcPattern pattern = new RpcPattern(patternDoc, rpcXsltFile, replyXsltFile, responseFile);
            patternList.add(pattern);

            LOG.debug("Loaded pattern {}: rpcXslt={}, replyXslt={}, responseFile={}",
                i, rpcXsltFile, replyXsltFile, responseFile);
        }

        return patternList;
    }

    /**
     * Extract XSLT file path from transformer element.
     */
    private String extractXsltFile(Element patternElement, String transformerTagName) {
        NodeList transformerNodes = patternElement.getElementsByTagName(transformerTagName);
        if (transformerNodes.getLength() > 0) {
            Element transformerElement = (Element) transformerNodes.item(0);
            NodeList xsltFileNodes = transformerElement.getElementsByTagName("xslt-file");
            if (xsltFileNodes.getLength() > 0) {
                String xsltFile = xsltFileNodes.item(0).getTextContent();
                return (xsltFile != null && !xsltFile.trim().isEmpty()) ? xsltFile.trim() : null;
            }
        }
        return null;
    }

    /**
     * Extract response file path for RPC interception.
     */
    private String extractResponseFile(Element patternElement) {
        NodeList responseFileNodes = patternElement.getElementsByTagName("response-file");
        if (responseFileNodes.getLength() > 0) {
            String responseFile = responseFileNodes.item(0).getTextContent();
            return (responseFile != null && !responseFile.trim().isEmpty()) ? responseFile.trim() : null;
        }
        return null;
    }

    /**
     * Get the first child element of a node.
     */
    private Element getFirstChildElement(Node parent) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                return (Element) child;
            }
        }
        return null;
    }

    /**
     * Find a matching pattern for the given RPC document.
     * Returns null if no pattern matches.
     */
    public RpcPattern findMatchingPattern(Document rpcDoc) {
        for (int i = 0; i < patterns.size(); i++) {
            RpcPattern pattern = patterns.get(i);
            if (matchesPattern(rpcDoc, pattern.getPatternDocument())) {
                LOG.debug("RPC matched pattern {}", i);
                return pattern;
            }
        }
        LOG.debug("No matching pattern found for RPC");
        return null;
    }

    /**
     * Check if an RPC document matches a pattern document.
     * Performs structural comparison ignoring attribute values and leaf element text values.
     * Element order within a container does not matter.
     */
    private boolean matchesPattern(Document rpcDoc, Document patternDoc) {
        Element rpcRoot = rpcDoc.getDocumentElement();
        Element patternRoot = patternDoc.getDocumentElement();

        return matchesElement(rpcRoot, patternRoot);
    }

    /**
     * Recursively check if an element matches a pattern element.
     * Ignores attribute values and leaf element text values.
     * Element order does not matter.
     */
    private boolean matchesElement(Element element, Element pattern) {
        // Check element name (local name to ignore namespace prefixes)
        if (!element.getLocalName().equals(pattern.getLocalName())) {
            return false;
        }

        // Check namespace URI
        String elementNS = element.getNamespaceURI();
        String patternNS = pattern.getNamespaceURI();
        if (!namespaceEquals(elementNS, patternNS)) {
            return false;
        }

        // Get child elements (ignore text nodes, comments, etc.)
        List<Element> elementChildren = getChildElements(element);
        List<Element> patternChildren = getChildElements(pattern);

        // If pattern has no children, it's a leaf element - match succeeds
        // (we ignore the actual text value)
        if (patternChildren.isEmpty()) {
            return true;
        }

        // If pattern has children but element doesn't, no match
        if (elementChildren.isEmpty() && !patternChildren.isEmpty()) {
            return false;
        }

        // Match children (order-independent)
        // For each pattern child, find a matching element child
        for (Element patternChild : patternChildren) {
            boolean foundMatch = false;

            for (Element elementChild : elementChildren) {
                if (matchesElement(elementChild, patternChild)) {
                    foundMatch = true;
                    break;
                }
            }

            if (!foundMatch) {
                return false;
            }
        }

        return true;
    }

    /**
     * Get all child elements of a node (ignoring text nodes, comments, etc.).
     */
    private List<Element> getChildElements(Element parent) {
        List<Element> children = new ArrayList<>();
        NodeList nodeList = parent.getChildNodes();

        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                children.add((Element) node);
            }
        }

        return children;
    }

    /**
     * Compare namespace URIs, treating null and empty string as equivalent.
     */
    private boolean namespaceEquals(String ns1, String ns2) {
        if (ns1 == null || ns1.isEmpty()) {
            return ns2 == null || ns2.isEmpty();
        }
        return ns1.equals(ns2);
    }
}
