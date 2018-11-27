/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.mapping;

import java.util.List;

import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev170228.network.nodes.CpToDegree;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev170228.network.nodes.Mapping;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.circuit.pack.Ports;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.Info;

public interface PortMapping {

    /**
     * When OpenRoadmTopology creates the logical to physical port mapping for a
     * given device, this methods has just to store the resulting mapping
     * operation to datastore.
     *
     * @param deviceInfo
     *            deviceInfo
     * @param portMapList
     *            The list of discovered mapping for the node
     * @param degreeCpList
     *            The list of circuit-packs supporting eth interface with lldp
     *            protocole
     * @return true if success to store mapping in datastore, false otherwise.
     */
    boolean createMappingData(Info deviceInfo, List<Mapping> portMapList, List<CpToDegree> degreeCpList);

    /**
     * This method removes mapping data from the datastore after disconnecting
     * ODL from a Netconf device.
     *
     * @param nodeId
     *            node ID
     */
    void deleteMappingData(String nodeId);

    /**
     * This method for a given node's termination point returns the Mapping
     * object based on portmapping.yang model stored in the MD-SAL data store
     * which is created when the node is connected for the first time. The
     * mapping object basically contains the following attributes of interest:
     *
     * <p>
     * 1. Supporting circuit pack
     *
     * <p>
     * 2. Supporting port
     *
     * <p>
     * 3. Supporting OMS interface (if port on ROADM) 4. Supporting OTS
     * interface (if port on ROADM)
     *
     * @param nodeId
     *            Unique Identifier for the node of interest.
     * @param logicalConnPoint
     *            Name of the logical point
     *
     * @return Result Mapping object if success otherwise null.
     */
    Mapping getMapping(String nodeId, String logicalConnPoint);

    boolean updateMapping(String nodeId, Mapping mapping);

    Mapping createMappingObject(String nodeId, Ports port, String circuitPackName,
        String logicalConnectionPoint);
}
