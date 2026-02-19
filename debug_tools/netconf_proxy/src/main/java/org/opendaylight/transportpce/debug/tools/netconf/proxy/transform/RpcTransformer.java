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
 * Interface for transforming NETCONF RPC requests and responses.
 * Implementations can modify the XML content before forwarding to device
 * or before returning to client.
 */
public interface RpcTransformer {

    /**
     * Transform an RPC request before sending to the device.
     *
     * @param rpcRequest The original RPC request from client
     * @return The transformed RPC request to send to device (or original if no transformation)
     */
    Document transformRequest(Document rpcRequest);

    /**
     * Transform an RPC response before sending back to the client.
     *
     * @param rpcResponse The original RPC response from device
     * @return The transformed RPC response to send to client (or original if no transformation)
     */
    Document transformResponse(Document rpcResponse);

    /**
     * Check if this transformer should be applied to the given RPC.
     *
     * @param rpcName The name of the RPC operation
     * @return true if transformation should be applied, false otherwise
     */
    default boolean shouldTransform(String rpcName) {
        return true;
    }
}
