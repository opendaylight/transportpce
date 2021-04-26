/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.mapping;

import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.network.Nodes;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.network.nodes.Mapping;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.network.nodes.McCapabilities;

public interface PortMapping {

    /**
     * This method creates logical to physical port mapping for a given device.
     * Instead of parsing all the circuit packs/ports in the device this methods
     * does a selective read operation on degree/srg subtree to get circuit
     * packs/ports that map to :
     *
     * <p>
     * 1. DEGn-TTP-TX, DEGn-TTP-RX, DEGn-TTP-TXRX
     *
     * <p>
     * 2. SRGn-PPp-TX, SRGn-PPp-RX, SRGn-PPp-TXRX
     *
     * <p>
     * 3. LINEn
     *
     * <p>
     * 4. CLNTn.
     *
     * <p>
     * If the port is Mw it also store the OMS, OTS interface provisioned on the
     * port. It skips the logical ports that are internal. If operation is
     * successful the mapping gets stored in datastore corresponding to
     * portmapping.yang data model.
     *
     * @param nodeId
     *            node ID
     * @param nodeVersion
     *            node version
     *
     * @return true/false based on status of operation
     */
    boolean createMappingData(String nodeId, String nodeVersion);

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

    /**
     * This method for a given node's media channel-capabilities returns the
     * object based on portmapping.yang model stored in the MD-SAL data store
     * which is created when the node is connected for the first time. The
     * mapping object basically contains the following attributes of interest:
     *
     * <p>
     * 1. slot width granularity
     *
     * <p>
     * 2. center frequency granularity
     *
     * <p>
     * 3. Supporting OMS interface (if port on ROADM) 4. Supporting OTS
     * interface (if port on ROADM)
     *
     * @param nodeId
     *            Unique Identifier for the node of interest.
     * @param mcLcp
     *            Name of the MC-capability
     *
     * @return Result McCapabilities.
     */
    McCapabilities getMcCapbilities(String nodeId, String mcLcp);

    boolean updateMapping(String nodeId, Mapping mapping);

    /**
     * Returns all Mapping informations for a given ordm device.
     * This method returns all Mapping informations already stored in the MD-SAL
     * data store for a given openroadm device. Beyound the list of mappings, it
     * gives access to general node information as its version or its node type,
     * etc.
     *
     * @param nodeId
     *            Unique Identifier for the node of interest.
     *
     * @return node data if success otherwise null.
     */
    Nodes getNode(String nodeId);
}
