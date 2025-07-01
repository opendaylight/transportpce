/*
 * Copyright © 2025 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm.topology.datastore;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.tapi.openroadm.tapi.NodeEdgePoints;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Context;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.NameAndValue;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.Context1;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.context.TopologyContext;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.context.Topology;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.context.TopologyKey;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TapiDataStore implements TAPI {

    private static final Logger LOG = LoggerFactory.getLogger(TapiDataStore.class);

    private final NetworkTransactionService networkTransactionService;

    public TapiDataStore(NetworkTransactionService networkTransactionService) {
        this.networkTransactionService = networkTransactionService;
    }

    @Override
    public boolean updateTopology(TopologyKey topologyKey, NodeKey nodeKey, OwnedNodeEdgePoint ownedNodeEdgePoint) {
        LOG.debug("Updating node {} in topology {} with OwnedNodeEdgePoint: {}",
                nodeKey.getUuid().getValue(), topologyKey.getUuid().getValue(), ownedNodeEdgePoint);

        DataObjectIdentifier.WithKey<OwnedNodeEdgePoint, OwnedNodeEdgePointKey> nodeId =
                topologyNodeId(topologyKey, nodeKey, ownedNodeEdgePoint);

        try {
            this.networkTransactionService.put(LogicalDatastoreType.OPERATIONAL, nodeId, ownedNodeEdgePoint);
            this.networkTransactionService.commit().get();

            logTopologyUpdate(true, topologyKey, nodeKey, ownedNodeEdgePoint);

            return true;
        } catch (InterruptedException | ExecutionException e) {
            logTopologyUpdate(false, topologyKey, nodeKey, ownedNodeEdgePoint);
            LOG.error("Exception while updating TAPI topology: ", e);
        }

        return false;
    }

    @Override
    public boolean updateTopology(
            TopologyKey topologyKey,
            NodeKey nodeKey, Map<OwnedNodeEdgePointKey,
            OwnedNodeEdgePoint> ownedNodeEdgePoints) {

        LOG.info("Updating TAPI node uuid: {} with {} updated OwnedNodeEdgePoints...",
                nodeKey.getUuid().getValue(), ownedNodeEdgePoints.entrySet());

        for (OwnedNodeEdgePoint ownedNodeEdgePoint : ownedNodeEdgePoints.values()) {
            if (!updateTopology(topologyKey, nodeKey, ownedNodeEdgePoint)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean updateTopology(
            TopologyKey topologyKey,
            NodeEdgePoints updatedOwnNodeEdgePoints) {

        for (Map.Entry<NodeKey, Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint>> node : updatedOwnNodeEdgePoints
                .edgePoints().entrySet()) {

            if (!updateTopology(topologyKey, node.getKey(), node.getValue())) {
                return false;
            }
        }

        return true;
    }

    private void logTopologyUpdate(boolean success,
            TopologyKey topologyKey,
            NodeKey nodeKey,
            OwnedNodeEdgePoint ownedNodeEdgePoint) {

        if (success) {
            LOG.info("OwnedNodeEdgePoint uuid: {} ({}) updated in TAPI topology {} for node {}",
                    ownedNodeEdgePoint.key().getUuid().getValue(),
                    ownedNodeEdgePoint.nonnullName().values().stream()
                            .map(NameAndValue::getValue)
                            .collect(Collectors.joining()),
                    topologyKey.getUuid().getValue(),
                    nodeKey.getUuid().getValue());
        } else {
            LOG.error("Failed adding/updating tapi topology {} with edge point {} : {}",
                    topologyKey.getUuid().getValue(),
                    ownedNodeEdgePoint.key().getUuid(),
                    Objects.requireNonNull(ownedNodeEdgePoint.getName())
                            .values().stream().map(NameAndValue::getValue).collect(Collectors.joining()));
        }
    }

    private DataObjectIdentifier.WithKey<OwnedNodeEdgePoint, OwnedNodeEdgePointKey> topologyNodeId(
            TopologyKey topologyKey,
            NodeKey nodeKey,
            OwnedNodeEdgePoint ownedNodeEdgePoint) {

        return DataObjectIdentifier
                .builder(Context.class)
                .augmentation(Context1.class)
                .child(TopologyContext.class)
                .child(Topology.class, topologyKey)
                .child(Node.class, nodeKey)
                .child(OwnedNodeEdgePoint.class, ownedNodeEdgePoint.key())
                .build();
    }

}
