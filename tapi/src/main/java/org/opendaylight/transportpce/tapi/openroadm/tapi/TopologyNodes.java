/*
 * Copyright © 2025 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm.tapi;

import java.util.Map;
import java.util.Set;
import org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.ORTerminationPoint;
import org.opendaylight.transportpce.tapi.utils.TapiContext;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.NodeKey;

public interface TopologyNodes {

    boolean isEmpty();

    Map<NodeKey, Node> nodes();

    int size();

    Set<NodeKey> nodeKeys();

    /**
     * Turn (updated) OpenROADM termination points into corresponding edge points in the TAPI model.
     *
     * @param orTerminationPoint The updated OpenROADM termination points.
     * @param tapiContext The TAPI context.
     * @return A NodeEdgePoints object containing updated Edge Points in the TAPI model.
     */
    NodeEdgePoints updatedNodeEdgePoints(ORTerminationPoint orTerminationPoint, TapiContext tapiContext);
}
