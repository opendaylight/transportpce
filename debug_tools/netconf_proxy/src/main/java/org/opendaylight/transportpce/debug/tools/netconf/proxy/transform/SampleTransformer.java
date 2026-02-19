/*
 * Copyright © 2026 1FINITY Inc.,  and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.debug.tools.netconf.proxy.transform;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Sample transformer demonstrating how to implement custom transformations.
 *
 * This example shows:
 * - Modifying XML elements
 * - Adding/removing attributes
 * - Filtering specific RPCs
 *
 * To use this transformer, set in application.conf:
 *   proxy.transformation.enabled = true
 *   proxy.transformation.transformerClass = "org.opendaylight.transportpce.debug.tools.netconf.proxy.transform.SampleTransformer"
 */
public class SampleTransformer implements RpcTransformer {

    private static final Logger LOG = LoggerFactory.getLogger(SampleTransformer.class);

    @Override
    public Document transformRequest(Document rpcRequest) {
        LOG.debug("Transforming RPC request");

        try {
            Element root = rpcRequest.getDocumentElement();
            String rpcName = getRpcName(root);

            LOG.debug("RPC operation: {}", rpcName);

            // Example transformation: Add a custom attribute to edit-config
            if ("edit-config".equals(rpcName)) {
                NodeList editConfigs = rpcRequest.getElementsByTagName("edit-config");
                if (editConfigs.getLength() > 0) {
                    Element editConfig = (Element) editConfigs.item(0);
                    // Add custom attribute or modify content
                    LOG.debug("Transforming edit-config request");
                }
            }

            // Example: Modify namespace or other elements as needed
            // transformNamespaces(rpcRequest);

        } catch (Exception e) {
            LOG.error("Error transforming request", e);
        }

        return rpcRequest;
    }

    @Override
    public Document transformResponse(Document rpcResponse) {
        LOG.debug("Transforming RPC response");

        try {
            Element root = rpcResponse.getDocumentElement();

            // Example: Transform data elements in response
            NodeList dataNodes = rpcResponse.getElementsByTagName("data");
            for (int i = 0; i < dataNodes.getLength(); i++) {
                Element dataElement = (Element) dataNodes.item(i);
                // Transform data content
                LOG.debug("Transforming data element in response");
            }

        } catch (Exception e) {
            LOG.error("Error transforming response", e);
        }

        return rpcResponse;
    }

    @Override
    public boolean shouldTransform(String rpcName) {
        // Example: Only transform specific operations
        switch (rpcName) {
            case "get":
            case "get-config":
            case "edit-config":
                return true;
            default:
                LOG.debug("Skipping transformation for RPC: {}", rpcName);
                return false;
        }
    }

    /**
     * Extract RPC operation name from the document.
     */
    private String getRpcName(Element rpcElement) {
        NodeList children = rpcElement.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                return child.getLocalName();
            }
        }
        return "unknown";
    }

    /**
     * Example: Transform namespaces in the document.
     */
    private void transformNamespaces(Document doc) {
        // Example implementation:
        // - Find elements with specific namespace
        // - Change namespace URI
        // - Update prefixes

        // This is just a placeholder for demonstration
        LOG.debug("Namespace transformation (placeholder)");
    }
}
