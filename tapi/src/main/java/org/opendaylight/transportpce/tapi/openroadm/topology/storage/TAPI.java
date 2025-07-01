/*
 * Copyright © 2025 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm.topology.storage;

import java.util.Map;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.context.TopologyKey;

public interface TAPI {

    /**
     * Update the TAPI topology with a single OwnedNodeEdgePoint.
     */
    boolean updateTopology(TopologyKey topologyKey, NodeKey nodeKey, OwnedNodeEdgePoint ownedNodeEdgePoint);

    /**
     * Update the TAPI topology with a map of OwnedNodeEdgePoints for a specific node.
     *
     * @param topologyKey The key of the topology to update.
     * @param nodeKey The key of the node to update.
     * @param ownedNodeEdgePoints A map of OwnedNodeEdgePointKey to OwnedNodeEdgePoint.
     * @return true if the update was successful, false otherwise.
     */
    boolean updateTopology(
            TopologyKey topologyKey,
            NodeKey nodeKey,
            Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> ownedNodeEdgePoints);

    /**
     * Update the TAPI topology with a map of OwnedNodeEdgePoints for a collection of nodes.
     *
     * @param topologyKey The key of the topology to update.
     * @param updatedOwnNodeEdgePoints A map where the key is NodeKey and the value is a map of
     *                                 OwnedNodeEdgePointKey to OwnedNodeEdgePoint.
     * @return true if the update was successful, false otherwise.
     */
    boolean updateTopology(
            TopologyKey topologyKey,
            Map<NodeKey, Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint>> updatedOwnNodeEdgePoints);

}

