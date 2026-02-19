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
 * Default transformer that performs no transformation.
 * Simply passes through requests and responses unchanged.
 */
public class NoOpTransformer implements RpcTransformer {

    @Override
    public Document transformRequest(Document rpcRequest) {
        // No transformation - return as-is
        return rpcRequest;
    }

    @Override
    public Document transformResponse(Document rpcResponse) {
        // No transformation - return as-is
        return rpcResponse;
    }
}
