/*
 * Copyright © 2026 1FINITY Inc.,  and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.debug.tools.netconf.proxy.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

/**
 * Utility class for applying XSLT transformations to XML documents.
 * Caches compiled XSLT templates for performance.
 */
public class XsltTransformer {

    private static final Logger LOG = LoggerFactory.getLogger(XsltTransformer.class);

    private final String xsltBasePath;
    private final TransformerFactory transformerFactory;
    private final Map<String, javax.xml.transform.Templates> templateCache;

    public XsltTransformer(String xsltBasePath) {
        this.xsltBasePath = xsltBasePath;
        this.transformerFactory = TransformerFactory.newInstance();
        this.templateCache = new ConcurrentHashMap<>();
    }

    /**
     * Transform an XML document using the specified XSLT file.
     *
     * @param inputDoc The input XML document
     * @param xsltFileName The XSLT file name (relative to xsltBasePath)
     * @return The transformed document
     * @throws Exception if transformation fails
     */
    public Document transform(Document inputDoc, String xsltFileName) throws Exception {
        if (xsltFileName == null || xsltFileName.trim().isEmpty()) {
            LOG.debug("No XSLT file specified, returning original document");
            return inputDoc;
        }

        // Get or compile the XSLT template
        javax.xml.transform.Templates templates = getTemplates(xsltFileName);

        // Create transformer and apply transformation
        Transformer transformer = templates.newTransformer();
        DOMSource source = new DOMSource(inputDoc);
        DOMResult result = new DOMResult();

        transformer.transform(source, result);

        LOG.debug("Applied XSLT transformation: {}", xsltFileName);
        return (Document) result.getNode();
    }

    /**
     * Get compiled XSLT templates from cache or compile if not cached.
     */
    private javax.xml.transform.Templates getTemplates(String xsltFileName) throws Exception {
        return templateCache.computeIfAbsent(xsltFileName, fileName -> {
            try {
                File xsltFile = new File(xsltBasePath, fileName);
                if (!xsltFile.exists()) {
                    throw new RuntimeException("XSLT file not found: " + xsltFile.getAbsolutePath());
                }

                LOG.info("Compiling XSLT template: {}", xsltFile.getAbsolutePath());
                try (InputStream is = new FileInputStream(xsltFile)) {
                    StreamSource xsltSource = new StreamSource(is);
                    xsltSource.setSystemId(xsltFile.toURI().toString());
                    return transformerFactory.newTemplates(xsltSource);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to compile XSLT template: " + fileName, e);
            }
        });
    }

    /**
     * Clear the template cache (useful for reloading XSLT files).
     */
    public void clearCache() {
        templateCache.clear();
        LOG.info("XSLT template cache cleared");
    }
}
