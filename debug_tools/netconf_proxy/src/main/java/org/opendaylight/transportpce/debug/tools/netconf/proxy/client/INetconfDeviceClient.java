/*
 * Copyright © 2026 1FINITY Inc.,  and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.debug.tools.netconf.proxy.client;

import java.util.Set;

/**
 * Interface for NETCONF device clients.
 * Allows switching between different NETCONF client implementations (ODL, JNCC, etc.)
 */
public interface INetconfDeviceClient {

    /**
     * Connect to the NETCONF device.
     * @throws Exception if connection fails
     */
    void connect() throws Exception;

    /**
     * Send an RPC to the device and get the response.
     * @param rpcXml the RPC XML string to send
     * @return the response XML string
     * @throws Exception if RPC fails
     */
    String sendRpc(String rpcXml) throws Exception;

    /**
     * Get the device capabilities.
     * @return set of capability URIs
     */
    Set<String> getDeviceCapabilities();

    /**
     * Check if connected to device.
     * @return true if connected, false otherwise
     */
    boolean isConnected();

    /**
     * Disconnect from the device.
     */
    void disconnect();
}
