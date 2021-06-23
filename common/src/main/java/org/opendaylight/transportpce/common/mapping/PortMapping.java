/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.mapping;

import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.mapping.Mapping;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.mc.capabilities.McCapabilities;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.network.Nodes;

public interface PortMapping {

    /**
     * This method creates logical to physical port mapping for a given device.
     * Instead of parsing all the circuit packs/ports in the device, this methods
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
     * This method removes mapping data from the datastore after disconnecting ODL
     * from a Netconf device.
     *
     * @param nodeId
     *            node ID
     */
    void deleteMappingData(String nodeId);

    /**
     * This method for a given node's termination point returns the Mapping object
     * based on portmapping.yang model stored in the MD-SAL data store which is
     * created when the node is connected for the first time. The mapping object
     * basically contains the following attributes of interest:
     *
     * <p>
     * 1. Supporting circuit pack
     *
     * <p>
     * 2. Supporting port
     *
     * <p>
     * 3. Supporting OTS/OMS interface (if port on ROADM)
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
     * This method allows retrieving a Mapping object from the mapping list stored in
     * the MD-SAL data store. The main interest is to retrieve the
     * logical-connection-point associated with a given port on a supporting
     * circuit-pack
     *
     * @param nodeId
     *            Unique Identifier for the node of interest.
     * @param circuitPackName
     *            Name of the supporting circuit-pack
     * @param portName
     *            Name of the supporting port
     *
     * @return Result Mapping object if success otherwise null.
     */
    Mapping getMapping(String nodeId, String circuitPackName, String portName);

    /**
     * This method, for a given node media channel-capabilities, returns the object
     * based on portmapping.yang model stored in the MD-SAL data store which is
     * created when the node is connected for the first time. The mapping object
     * basically contains the following attributes of interest:
     *
     * <p>
     * 1. slot width granularity
     *
     * <p>
     * 2. center frequency granularity
     *
     * <p>
     * 3. Supporting OMS interface (if port on ROADM)
     *
     * <p>
     * 4. Supporting OTS interface (if port on ROADM)
     *
     * @param nodeId
     *            Unique Identifier for the node of interest.
     * @param mcLcp
     *            Name of the MC-capability
     *
     * @return Result McCapabilities.
     */
    McCapabilities getMcCapbilities(String nodeId, String mcLcp);

    /**
     * This method for a given node, allows to update a specific mapping based on
     * portmapping.yang model already stored in the MD-SAL data store, following
     * some changes on the device port (creation of interface supported on this
     * port, change of port admin state, etc).
     *
    * @param nodeId
     *            Unique Identifier for the node of interest.
     * @param mapping
     *            Old mapping to be updated.
     *
     * @return Result true/false based on status of operation.
     */
    boolean updateMapping(String nodeId, Mapping mapping);

    /**
     * Returns all Mapping informations for a given ordm device. This method returns
     * all Mapping informations already stored in the MD-SAL data store for a given
     * openroadm device. Beyound the list of mappings, it gives access to general
     * node information as its version or its node type, etc.
     *
     * @param nodeId
     *            Unique Identifier for the node of interest.
     *
     * @return node data if success otherwise null.
     */
    Nodes getNode(String nodeId);
}
