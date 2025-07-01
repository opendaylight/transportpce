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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record TapiNodeEdgePoint(Map<NodeKey, Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint>> edgePoints)
        implements NodeEdgePoints {

    private static final Logger LOG = LoggerFactory.getLogger(TapiNodeEdgePoint.class);

    @Override
    public Map<NodeKey, Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint>> edgePoints() {
        return edgePoints;
    }

    @Override
    public boolean updateDataStore(TopologyKey topologyKey, TAPI tapiDataStore) {
        LOG.info("Updating {} node(s) in TAPI context {}...", edgePoints.size(), topologyKey.getUuid().getValue());

        boolean result = tapiDataStore.updateTopology(topologyKey, this);

        LOG.info("All nodes updated in TAPI context {}.", topologyKey.getUuid().getValue());

        return result;
    }
}
