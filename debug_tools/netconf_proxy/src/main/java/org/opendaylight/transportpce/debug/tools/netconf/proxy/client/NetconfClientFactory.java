/*
 * Copyright © 2026 1FINITY Inc.,  and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.debug.tools.netconf.proxy.client;

import org.opendaylight.transportpce.debug.tools.netconf.proxy.config.ProxyConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for creating NETCONF device clients.
 * Uses JSch-based NETCONF client implementation.
 */
public final class NetconfClientFactory {

    private static final Logger LOG = LoggerFactory.getLogger(NetconfClientFactory.class);

    private NetconfClientFactory() {
        // Prevent instantiation
    }

    /**
     * Create a NETCONF client.
     *
     * @param config the proxy configuration
     * @return a JSch-based NETCONF device client instance
     */
    public static INetconfDeviceClient createClient(ProxyConfig config) {
        LOG.info("Creating JSch-based NETCONF client");
        return new JnccNetconfDeviceClient(config);
    }

}
