/*
 * Copyright © 2025 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.factory;

import java.util.Map;
import java.util.Set;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.NodeKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePointKey;

public interface OwnNodeEdgePoint {

    /**
     * Create a map of OwnedNodeEdgePoint from OpenROADM topology nodes.
     *
     * @param tapiTopologyNodes the TAPI topology nodes
     * @param openRoadmTopology the OpenROADM topology
     * @param nodeIdTerminationPointIdMap a map of node IDs to termination point IDs
     * @return a map where the key is the NodeKey and the value is a map of OwnedNodeEdgePointKey to OwnedNodeEdgePoint
     */
    Map<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology
            .rev221121.topology.NodeKey,
            Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint>> createUpdatedOwnNodeEdgePoints(
                    Map<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.NodeKey,
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node>
            tapiTopologyNodes,
            Set<Map.Entry<NodeKey, Node>> openRoadmTopology,
            Map<String, Set<String>> nodeIdTerminationPointIdMap);

}
