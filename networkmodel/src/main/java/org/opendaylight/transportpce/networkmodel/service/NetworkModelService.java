/*
 * Copyright © 2016 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.networkmodel.service;

import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.mapping.Mapping;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210618.renderer.rpc.result.sp.Link;
import org.opendaylight.yang.gen.v1.http.transportpce.topology.rev210511.OtnLinkType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNodeConnectionStatus;
import org.opendaylight.yangtools.yang.common.Uint32;

/**
 * Service for data manipulation on OpenROADM topology models.
 */
public interface NetworkModelService {

    /**
     * Create new OpenROADM node in all OpenROADM topologies.
     *
     * @param nodeId
     *     unique node ID of new OpenROADM node
     * @param nodeVersion
     *     OpenROADM node version
     */
    void createOpenRoadmNode(String nodeId, String nodeVersion);

    /**
     * Delete OpenROADM node mapping and topologies.
     *
     * @param nodeId
     *     unique node ID of OpenROADM node.
     *
     */
    void deleteOpenRoadmnode(String nodeId);

    /**
     * Update termination point, and if need, be associated links, of
     * openroadm-topology and otn-topology after a change on a given mapping.
     *
     * @param nodeId
     *            unique node ID of OpenROADM node at the origin of the NETCONF
     *            notification change.
     * @param mapping
     *            updated mapping following the device notification change.
     */
    void updateOpenRoadmTopologies(String nodeId, Mapping mapping);

    /**
     * Set/update connection status of OpenROADM node.
     *
     * @param nodeId
     *     unique node ID of new OpenROADM node
     * @param connectionStatus
     *     connection status of the node
     */
    void setOpenRoadmNodeStatus(String nodeId, NetconfNodeConnectionStatus.ConnectionStatus connectionStatus);

    /**
     * create new otn link in otn-topology.
     *
     * @param nodeA
     *     OpenROADM node ID for link termination point A
     * @param tpA
     *     OpenROADM tp id on nodeA for link termination point A
     * @param nodeZ
     *     OpenROADM node ID for link termination point Z
     * @param tpZ
     *     OpenROADM tp id on nodeZ for link termination point Z
     * @param linkType
     *     OtnLinkType, as OTU4, ODTU, etc
     */
    void createOtnLinks(String nodeA, String tpA, String nodeZ, String tpZ, OtnLinkType linkType);

    /**
     * delete otn links from otn-topology.
     *
     * @param nodeA
     *     OpenROADM node ID for link termination point A
     * @param tpA
     *     OpenROADM tp id on nodeA for link termination point A
     * @param nodeZ
     *     OpenROADM node ID for link termination point Z
     * @param tpZ
     *     OpenROADM tp id on nodeZ for link termination point Z
     * @param linkType
     *     OtnLinkType, as OTU4, ODTU, etc
     */
    void deleteOtnLinks(String nodeA, String tpA, String nodeZ, String tpZ, OtnLinkType linkType);

    /**
     * update otn links from otn-topology.
     *
     * @param link
     *     link containing termination points to be updated
     * @param serviceRate
     *     Service rate may be 1G, 10G, 100G or 400G
     * @param tribPortNb
     *     Trib port number allocated by the service
     * @param minTribSoltNb
     *     First contiguous trib slot number allocated by the service
     * @param maxTribSoltNb
     *     Last contiguous trib slot number allocated by the service
     * @param isDeletion
     *       True indicates if the low-order otn service must be deleted
     */
    void updateOtnLinks(Link link, Uint32 serviceRate, Short tribPortNb, Short minTribSoltNb, Short maxTribSoltNb,
            boolean isDeletion);
}
