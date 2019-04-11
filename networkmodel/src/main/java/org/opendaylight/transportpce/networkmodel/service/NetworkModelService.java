/*
 * Copyright © 2016 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.networkmodel.service;

import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNodeConnectionStatus;

/**
 * Service for data manipulation on OpenROADM topology models.
 */
public interface NetworkModelService {


    /**
        * Create new OpenROADM node in all OpenROADM topologies.
     * @param nodeId
     *   unique node ID of new OpenROADM node
     * @param nodeVersion
     *   OpenROADM node version
     */
    void createOpenRoadmNode(String nodeId, String nodeVersion);

    /**
     * Delete OpenROADM node mapping and topologies.
     *
     * @param nodeId unique node ID of OpenROADM node.
     *
     */
    void deleteOpenRoadmnode(String nodeId);

    /**
     * Set/update connection status of OpenROADM node.
     *
     * @param nodeId
     *   unique node ID of new OpenROADM node
     * @param connectionStatus
     *   connection status of the node
     */
    void setOpenRoadmNodeStatus(String nodeId, NetconfNodeConnectionStatus.ConnectionStatus connectionStatus);

}
