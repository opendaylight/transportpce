/*
 * Copyright © 2019 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.renderer.provisiondevice.otn;

import java.util.List;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.odu.switching.pools.non.blocking.list.PortList;

public interface OtnDeviceOperations {

    /**
     * This method checks if the client port can be used or not.
     *
     * @param nodeID unique identifier of a device
     * @param circuitPackName circuit pack name of the client port
     * @param portName port name of the client port
     * @param capacity rate of the service needed
     * @return String which states whether the client port is valid or not
     */
    String validateClientPort(String nodeID, String circuitPackName, String portName, String capacity);

    /**
     * This method retrieves the possible network ports.
     *
     *<p>
     *     checks for the possible network ports in odu-switching-pool
     *</p>
     * @param circuitPackName Circuit pack name of the client port
     * @param portName port name of the client port
     * @return List of all possible network ports
     */
    List<PortList> getPossibleNetworkPorts(String circuitPackName, String portName);


}
