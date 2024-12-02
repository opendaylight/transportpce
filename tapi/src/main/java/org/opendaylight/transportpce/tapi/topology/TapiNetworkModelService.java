/*
 * Copyright © 2021 Nokia, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.topology;

import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev240315.mapping.Mapping;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev240315.network.Nodes;

public interface TapiNetworkModelService {

    /**
     * Create new TAPI node in tapi topologies.
     *
     * @param orNodeId
     *     unique node ID of new OpenROADM node
     * @param orNodeVersion
     *     OpenROADM node version
     * @param node
     *     OpenRoadm node
     */
    void createTapiNode(String orNodeId, int orNodeVersion, Nodes node);

    /**
     * Delete TAPI node in topologies and update corresponding TAPI context objects.
     *
     * @param nodeId
     *     unique node ID of OpenROADM node.
     *
     */
    void deleteTapinode(String nodeId);

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
    void updateTapiTopology(String nodeId, Mapping mapping);
}
