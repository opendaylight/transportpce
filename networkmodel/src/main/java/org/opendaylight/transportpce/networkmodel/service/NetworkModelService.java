/*
 * Copyright © 2016 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.networkmodel.service;

import java.util.List;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210927.mapping.Mapping;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.renderer.rpc.result.sp.Link;
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
     * @param notifLink
     *     Expressed by the means of a link, specifies the
     *     termination points of the otn link to create.
     * @param suppLinks
     *     list of link-id supported the service (used when more than one supported link)
     * @param linkType
     *     OtnLinkType, as OTU4, ODTU, etc
     */
    void createOtnLinks(
        org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.renderer.rpc.result.sp.Link
        notifLink, List<String> suppLinks, OtnLinkType linkType);

    /**
     * delete otn links from otn-topology.
     *
     * @param notifLink
     *     Expressed by the means of a link, specifies the
     *     termination points of the otn link to create.
     * @param suppLinks
     *     list of link-id supported the service (used when more than one supported link)
     * @param linkType
     *     OtnLinkType, as OTU4, ODTU, etc
     */
    void deleteOtnLinks(
        org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.renderer.rpc.result.sp.Link
        notifLink, List<String> suppLinks, OtnLinkType linkType);

    /**
     * Update otn links from otn-topology.
     * For services using low-order odu, updates bandwidth parameters
     * for both the direct parent high-order odu link, and also its server
     * otu link.
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
     *     True indicates if the low-order otn service must be deleted
     */
    void updateOtnLinks(Link link, Uint32 serviceRate, Short tribPortNb, Short minTribSoltNb, Short maxTribSoltNb,
            boolean isDeletion);

    /**
     * Update otn links from otn-topology.
     * For services using directly a high-order odu, updates bandwidth parameters
     * of the direct parent otu link.
     *
     * @param supportedLinks
     *     list of link-id supported the service (used when more than one supported link)
     * @param isDeletion
     *     True indicates if the low-order otn service must be deleted
     */
    void updateOtnLinks(List<String> supportedLinks, boolean isDeletion);
}
