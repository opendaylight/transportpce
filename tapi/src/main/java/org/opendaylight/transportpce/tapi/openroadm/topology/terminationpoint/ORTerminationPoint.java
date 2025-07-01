/*
 * Copyright © 2025 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint;

import java.util.Map;
import java.util.Set;
import org.opendaylight.transportpce.tapi.utils.TapiContext;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.NodeKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePointKey;

public interface ORTerminationPoint {

    /**
     * Create TAPI Owned Node Edge points for the OpenROADM topology.
     *
     * <p>Searches the OpenROADM topology for the termination points anv converts them
     * to OwnedNodeEdgePoint.</p>
     *
     * @param openRoadmTopology The OpenROADM topology as a set of node entries.
     * @param nodeIdTerminationPointIdMap A map of node IDs to their termination point IDs.
     *                                    e.g. ROADM-A1-DEG1 -> [DEG1-CTP-TXRX, DEG1-TTP-TXRX]
     * @param tapiContext The TAPI context.
     * @return A map of owned node edge points keyed by their unique identifiers.
     */
    Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> ownedNodeEdgePoints(
            Set<Map.Entry<NodeKey, Node>> openRoadmTopology,
            Map<String, Set<String>> nodeIdTerminationPointIdMap,
            TapiContext tapiContext);

}
