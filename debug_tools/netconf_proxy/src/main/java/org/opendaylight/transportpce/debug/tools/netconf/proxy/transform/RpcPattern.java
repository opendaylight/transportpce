/*
 * Copyright © 2026 1FINITY Inc.,  and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.debug.tools.netconf.proxy.transform;

import org.w3c.dom.Document;

/**
 * Represents an RPC pattern definition with optional XSLT transformations.
 */
public class RpcPattern {

    private final Document patternDocument;
    private final String rpcTransformerXsltFile;
    private final String replyTransformerXsltFile;
    private final String responseFile;

    public RpcPattern(Document patternDocument,
                      String rpcTransformerXsltFile,
                      String replyTransformerXsltFile,
                      String responseFile) {
        this.patternDocument = patternDocument;
        this.rpcTransformerXsltFile = rpcTransformerXsltFile;
        this.replyTransformerXsltFile = replyTransformerXsltFile;
        this.responseFile = responseFile;
    }

    /**
     * Get the pattern document to match against.
     */
    public Document getPatternDocument() {
        return patternDocument;
    }

    /**
     * Get the XSLT file path for RPC transformation (request).
     * Returns null if no transformation is specified.
     */
    public String getRpcTransformerXsltFile() {
        return rpcTransformerXsltFile;
    }

    /**
     * Get the XSLT file path for reply transformation (response).
     * Returns null if no transformation is specified.
     */
    public String getReplyTransformerXsltFile() {
        return replyTransformerXsltFile;
    }

    /**
     * Check if RPC transformation is specified.
     */
    public boolean hasRpcTransformer() {
        return rpcTransformerXsltFile != null && !rpcTransformerXsltFile.trim().isEmpty();
    }

    /**
     * Check if reply transformation is specified.
     */
    public boolean hasReplyTransformer() {
        return replyTransformerXsltFile != null && !replyTransformerXsltFile.trim().isEmpty();
    }

    /**
     * Get the response file path for interception.
     * Returns null if no response file is specified.
     */
    public String getResponseFile() {
        return responseFile;
    }

    /**
     * Check if this RPC should be intercepted (not forwarded to device).
     */
    public boolean isIntercepted() {
        return responseFile != null && !responseFile.trim().isEmpty();
    }
}
