/*
 * Copyright © 2025 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm.tapi;

import java.util.Map;
import org.opendaylight.transportpce.tapi.openroadm.topology.datastore.TAPI;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.context.TopologyKey;

public interface NodeEdgePoints {

    /**
     * Get the edge points of the nodes.
     *
     * @return A map where the key is NodeKey and the value is a map of OwnedNodeEdgePointKey to OwnedNodeEdgePoint.
     */
    Map<NodeKey, Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint>> edgePoints();

    /**
     * Update the TAPI datastore with these edge points.
     *
     * @param topologyKey The key of the topology to update.
     * @param tapiDataStore The TAPI store instance to perform the update.
     * @return true if the update was successful, false otherwise.
     */
    boolean updateDataStore(TopologyKey topologyKey, TAPI tapiDataStore);

}
